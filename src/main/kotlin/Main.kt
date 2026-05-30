package me.yashbokade

import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

val AllowedCommands = listOf("ls", "dir", "cd", "mkdir", "echo", "clear", "exit")
val BlockedCommands = listOf("rmdir", "del", "rm")

fun isWindows(): Boolean {
    return System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")
}

fun main(args: Array<String>) = runBlocking {
    println("=====================================================")
    println("         CLI Helper powered by Mistral AI            ")
    println("=====================================================")

    val apiKey = System.getenv("MISTRAL_API_KEY")
    if (apiKey.isNullOrEmpty()) {
        println("Error: Please set the MISTRAL_API_KEY environment variable.")
        println("Usage: MISTRAL_API_KEY=your_key ./gradlew run")
        return@runBlocking
    }

    val model = args.firstOrNull() ?: "mistral-small-latest"
    println("Using model: $model")
    println("Type 'exit' or 'quit' to close.")
    println("=====================================================")

    val systemPrompt = object {}.javaClass.getResourceAsStream("/system.txt")?.bufferedReader()?.use { it.readText() }
        ?: throw Exception("Could not find system.txt in resources")

    val restClient = MistralRestClient(apiKey)

    val promptMessages = mutableListOf(
        Message(role = "system", content = systemPrompt)
    )

    while (true) {
        print("> ")
        val rawInput = readlnOrNull()
        if (rawInput == null) {
            println("\nExiting...")
            break
        }

        val input = rawInput.trim()
        if (input.isEmpty()) continue

        if (input.equals("exit", ignoreCase = true) || input.equals("quit", ignoreCase = true)) {
            println("Exiting...")
            break
        }

        val userMessageText = "$input\nonly give the cmd output and no explanation of what is it doing or what will happen or in code block | just plain text"
        promptMessages.add(Message(role = "user", content = userMessageText))

        val req = ChatRequest(
            model = model,
            messages = promptMessages,
            temperature = 0.4
        )

        try {
            val response = restClient.complete(req)
            val textOutput = response.choices.firstOrNull()?.message?.content?.trim()

            if (textOutput.isNullOrEmpty()) {
                println("No output generated.")
                continue
            }

            println("Executing: $textOutput")

            val cmdToken = textOutput.split(" ")

            if (cmdToken.isNotEmpty()) {
                val command = cmdToken[0].lowercase()
                if (BlockedCommands.contains(command)) {
                    println("Error: Blocked Command detected ($textOutput)")
                    promptMessages.removeLast() // remove the last user message to not pollute history
                    continue
                }
            }

            // Execute the command
            val processBuilder = ProcessBuilder()
            if (isWindows()) {
                processBuilder.command("cmd", "/c", textOutput)
            } else {
                processBuilder.command("bash", "-c", textOutput)
            }

            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val outputBuilder = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                println(line)
                outputBuilder.append(line).append("\n")
            }

            process.waitFor()

            // Add Assistant response to the history so model has context
            promptMessages.add(Message(role = "assistant", content = textOutput))

            // We can optionally add the command output back to history if we wanted,
            // but for now let's just let the user see it.
            // promptMessages.add(Message(role = "user", content = "Command Output:\n${outputBuilder.toString()}"))

        } catch (e: Exception) {
            println("Synchronous execution error: ${e.message}\n")
            promptMessages.removeLast() // rollback on error
        }
    }
}
