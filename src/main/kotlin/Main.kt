package me.yashbokade

import kotlinx.coroutines.runBlocking
import java.io.File

val AllowedCommands = listOf("ls", "dir", "cd", "mkdir", "echo", "clear", "exit")
val BlockedCommands = listOf("rmdir","del")
fun main(args: Array<String>) = runBlocking {
    val apiKey = "1f8Ibg4c7UovSyeK3VHfnM2WNyLI4beF"
    val a = File("D:\\Cursed\\cliHelper\\src\\main\\resources\\system.txt")
//    print(a.readText())
    var input: String? = ""

    val restClient = MistralRestClient(apiKey)
    if (apiKey == "YOUR_API_KEY") {
        println("Please set the MISTRAL_API_KEY environment variable.")
        return@runBlocking
    }
    while (input != "exit"){

        input = readln()
        val promptMessages = listOf(
            Message(role = "system", content = a.readText()),
            Message(role = "user", content = input+"\n only give the cmd output and no explanation of what is it doing or what will happen or in code block | just plain text")
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
            println(response)
            val cmdToken: List<String>? = textOutput?.split(" ")

            while (cmdToken?.size!=0){
                if (BlockedCommands.contains(cmdToken?.get(0))){
                    throw Exception("Blocked Command $textOutput")
                }

            }


            Runtime.getRuntime().exec("cmd /c $textOutput")
            promptMessages.plus(textOutput)

        } catch (e: Exception) {
            println("Synchronous execution error: ${e.message}\n")
        }
    }
}

