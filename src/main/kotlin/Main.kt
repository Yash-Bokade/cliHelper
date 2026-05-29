package me.yashbokade

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileReader

fun main(args: Array<String>) = runBlocking {
    val apiKey = "1f8Ibg4c7UovSyeK3VHfnM2WNyLI4beF"
    val a = File("D:\\Cursed\\cliHelper\\src\\main\\resources\\system.txt")
//    print(a.readText())

    if (apiKey == "YOUR_API_KEY") {
        println("Please set the MISTRAL_API_KEY environment variable.")
        return@runBlocking
    }

    val restClient = MistralRestClient(apiKey)
    val promptMessages = listOf(
        Message(role = "system", content = a.readText()) ,
        Message(role = "user", content = args[1]+"\n only give the cmd output and no explanation of what is it doing or what will happen or in code block")
    )

    val req = ChatRequest(
        model = args[0],
        messages = promptMessages,
        temperature = 0.4,

    )

    try {
        val response = restClient.complete(req)
        val textOutput = response.choices.firstOrNull()?.message?.content
//        println("Response ID: ${response.id}")
//        println("Tokens Used: ${response.usage.totalTokens}")
        println(textOutput)
        Runtime.getRuntime().exec("cmd /c $textOutput")
    } catch (e: Exception) {
        println("Synchronous execution error: ${e.message}\n")
    }
}

