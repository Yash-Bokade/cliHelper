package me.yashbokade

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// ==========================================
// API Request & Response Schemas
// ==========================================

@Serializable
data class Message(
    val role: String, // "system", "user", or "assistant"
    val content: String,
    val prefix: Boolean? = null // Forces prefix responses [2]
)

@Serializable
data class ResponseFormat(
    val type: String // "text" or "json_object"
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
    @SerialName("safe_prompt") val safePrompt: Boolean = false
)

@Serializable
data class ChatResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: UsageInfo
)

@Serializable
data class Choice(
    val index: Int,
    val message: Message,
    @SerialName("finish_reason") val finishReason: String?
)

@Serializable
data class UsageInfo(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)

@Serializable
data class ChatStreamChunk(
    val id: String? = null,
    val choices: List<ChunkChoice>
)

@Serializable
data class ChunkChoice(
    val index: Int,
    val delta: ChunkDelta,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChunkDelta(
    val content: String? = null
)

// ==========================================
// Pure JDK HTTP Client Wrapper
// ==========================================

class MistralRestClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.mistral.ai/v1"
) {
    // Configures native Java HTTP Client
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = false
    }

    /**
     * Executes a synchronous post request and parses the full JSON response.
     */
    fun complete(request: ChatRequest): ChatResponse {
        val requestBodyJson = jsonSerializer.encodeToString(ChatRequest.serializer(), request)

        // Builds HTTP request using Java’s native Builder pattern
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/chat/completions"))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
            .build()

        // Blocks and reads the entire response into a String
        val httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())

        if (httpResponse.statusCode()!= 200) {
            throw IllegalStateException("Mistral API error (${httpResponse.statusCode()}): ${httpResponse.body()}")
        }

        return jsonSerializer.decodeFromString(ChatResponse.serializer(), httpResponse.body())
    }

    /**
     * Executes an incremental, streamed request over Server-Sent Events (SSE).
     * Avoids heap allocation issues by converting the InputStream line-by-line into a Flow.
     */
    fun stream(request: ChatRequest): Flow<String> = flow {
        val streamingPayload = request.copy(stream = true)
        val requestBodyJson = jsonSerializer.encodeToString(ChatRequest.serializer(), streamingPayload)

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/chat/completions"))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
            .build()

        // Obtains response as a raw input stream rather than holding it in memory
        val httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())

        if (httpResponse.statusCode()!= 200) {
            throw IllegalStateException("API connection failed with status code: ${httpResponse.statusCode()}")
        }

        // Process stream line-by-line
        httpResponse.body().use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                while (true) {
                    val line = reader.readLine()?: break // Stop when stream is closed

                    if (line.isBlank() ||!line.startsWith("data:")) continue

                    val cleanSegment = line.removePrefix("data:").trim()

                    // Mistral signals end of transmission with data:
                    if (cleanSegment == "") {
                        break
                    }

                    try {
                        val parsedChunk = jsonSerializer.decodeFromString<ChatStreamChunk>(cleanSegment)
                        val deltaText = parsedChunk.choices.firstOrNull()?.delta?.content
                        if (!deltaText.isNullOrEmpty()) {
                            emit(deltaText) // Safely emits tokens as they arrive
                        }
                    } catch (e: Exception) {
                        // Suppresses parsing errors on incomplete framing arrays
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO) // Directs low-level Network I/O onto background IO threads
}