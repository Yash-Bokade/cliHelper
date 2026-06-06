package me.yashbokade

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import kotlinx.serialization.json.*
import java.io.File
import java.io.InputStreamReader
import java.util.Locale

var configFile = "Config.json"
var config: ConfigFile? = null
val AllowedCommands = listOf("ls", "dir", "cd", "mkdir", "echo", "clear", "exit")
val BlockedCommands = listOf("rmdir", "del", "rm")

// ANSI Color Codes
const val ANSI_RESET = "\u001B[0m"
const val ANSI_BLACK = "\u001B[30m"
const val ANSI_RED = "\u001B[31m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_YELLOW = "\u001B[33m"
const val ANSI_BLUE = "\u001B[34m"
const val ANSI_PURPLE = "\u001B[35m"
const val ANSI_CYAN = "\u001B[36m"
const val ANSI_WHITE = "\u001B[37m"

fun isWindows(): Boolean {
    return System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")
}

fun setupConfig() {
    val a = File(configFile)
    if (!a.exists()){
        a.createNewFile()
        a.writeText(Json.encodeToString(
            ConfigFile.serializer(),
            ConfigFile(model = "mistral-small-latest", apiKey = "MISTRAL_API_KEY")))
    }
    else{
        println(Json.decodeFromString<ConfigFile>(a.readText()))
        config = Json.decodeFromString<ConfigFile>(a.readText())
    }
}

fun main(args: Array<String>) = runBlocking {
    setupConfig()
    println("${ANSI_BLUE}=====================================================${ANSI_RESET}")
    println("${ANSI_CYAN}|        CLI Helper powered by Mistral AI           |${ANSI_RESET}")
    println("${ANSI_CYAN}=====================================================${ANSI_RESET}")


    var model = args.getOrNull(0) ?: config?.model ?: "mistral-small-latest"
    var apiKey = args.getOrNull(1) ?: System.getenv("MISTRAL_API_KEY") ?: config?.apiKey

    if (apiKey.isNullOrEmpty() || apiKey == "MISTRAL_API_KEY") {
        println("${ANSI_RED}Error: Please provide a Mistral API key via CLI arguments, MISTRAL_API_KEY environment variable, or Config.json.${ANSI_RESET}")
        return@runBlocking
    }
    println("${ANSI_YELLOW}Using model: $model${ANSI_RESET}")
    println("${ANSI_YELLOW}Type 'help' for list of commands.${ANSI_RESET}")
    println("${ANSI_RED}Type 'exit' or 'quit' to close.${ANSI_RESET}")
    println("${ANSI_CYAN}=====================================================${ANSI_RESET}")

    val rawSystemPrompt = File(config?.systemPrompt ?: "system.txt").readText()

//    val rawSystemPrompt = object {}.javaClass.getResourceAsStream("/")
//        ?.bufferedReader()
//        ?.use { it.readText() }
//        ?: throw Exception("Could not find system.txt in resources")

    val systemPrompt = "$rawSystemPrompt\n\nIMPORTANT: The current operating system is ${System.getProperty("os.name")}. Ensure all generated commands are compatible with this OS."

    val restClient = MistralRestClient(apiKey)

    val promptMessages = mutableListOf(
        Message(role = "system", content = systemPrompt)
    )
    val commandHistory = mutableListOf<String>()
    val historyFile = File(System.getProperty("user.home"), ".clihelper_history")
    if (historyFile.exists()) {
        commandHistory.addAll(historyFile.readLines())
    }

    while (true) {
        print("${ANSI_GREEN}> ${ANSI_RESET}")
        val rawInput = readlnOrNull()
        if (rawInput == null) {
            println("\n${ANSI_YELLOW}Exiting...${ANSI_RESET}")
            break
        }

        val input = rawInput.trim()
        if (input.isEmpty()) continue

        if (input.equals("exit", ignoreCase = true) || input.equals("quit", ignoreCase = true)) {
            println("${ANSI_YELLOW}Exiting...${ANSI_RESET}")
            break
        }

//        COMMAND ---
//
//        All commands here With custom Actions.

        // shows the help msg
        if (input.equals("help", ignoreCase = true)) {
            println("${ANSI_CYAN}Commands:${ANSI_RESET}")
            println("  ${ANSI_CYAN}help${ANSI_RESET}  - Show this help message")
            println("  ${ANSI_CYAN}clear${ANSI_RESET} - Clear the terminal screen")
            println("  ${ANSI_CYAN}history${ANSI_RESET} - Show command history")
            println("  ${ANSI_CYAN}config${ANSI_RESET} - Show configuration")
            println("  ${ANSI_CYAN}model <model_name>${ANSI_RESET} - Change model")
//            println("  ${ANSI_CYAN}reload${ANSI_RESET} - Reload configuration")
            println("  ${ANSI_CYAN}sysinfo${ANSI_RESET} - Show system information")
            println("  ${ANSI_CYAN}exit${ANSI_RESET}  - Exit the application")
            println("  ${ANSI_CYAN}quit${ANSI_RESET}  - Exit the application")
            println("Any other input will be processed by the AI.")
            continue
        }

        // clear terminal
        if (input.equals("clear", ignoreCase = true)) {
            print("\u001b[H\u001b[2J")
            System.out.flush()
            continue
        }

        // List of all old prompts
        if (input.equals("history", ignoreCase = true)) {
            println("${ANSI_CYAN}Command History:${ANSI_RESET}")
            commandHistory.forEachIndexed { index, cmd ->
                println("  ${index + 1}: $cmd")
            }
            continue
        }

        // your current system information
        if (input.equals("sysinfo", ignoreCase = true)) {
            println("${ANSI_CYAN}System Information:${ANSI_RESET}")
            println("  OS: ${System.getProperty("os.name")}")
            println("  Java Version: ${System.getProperty("java.version")}")
            println("  Model: $model")
            continue
        }

        // Shows loaded Config file
        if (input.equals("config", ignoreCase = true)) {
            println("${ANSI_CYAN}Configuration:${ANSI_RESET}")
            println("  Model: $model")
            println("  API Key: ${apiKey?.take(4)}...${apiKey?.takeLast(4)}")
            println("  System Prompt: ${config?.systemPrompt ?: "Not set"}")
            println("  Allowed Commands: ${AllowedCommands.joinToString(", ")}")
            println("  Blocked Commands: ${BlockedCommands.joinToString(", ")}")
            continue
        }

        // Reload command
        if (input.equals("reload", ignoreCase = true)) {
            setupConfig()
            model = config?.model ?: "mistral-small-latest"
            apiKey = config?.apiKey ?: apiKey
            val updatedSystemPrompt = File(config?.systemPrompt ?: "system.txt").readText() + "\n\nIMPORTANT: The current operating system is ${System.getProperty("os.name")}. Ensure all generated commands are compatible with this OS."
            promptMessages[0] = Message(role = "system", content = updatedSystemPrompt)
            println("${ANSI_GREEN}Configuration reloaded successfully.${ANSI_RESET}")
            continue
        }

        // allows update of model
        if (input.equals("model", ignoreCase = true)) {
            println("Current Model = ${model}")
            println("Enter New Model Name")
            val d = readln().trim()


            // TODO Make this better by checking if the model is valid --- [BEFORE] v4
            if(d.split("-").size<3) print("model name eg - [mistral-small-latest]")
            if(d!=model) model = d

            System.out.flush()
            println("Model changed to $model")
            continue
        }
        // another version for inline model updating
        if (input.split(" ")[0].equals("model" , ignoreCase = true)) {
            model = input.split(" ")[1]
            System.out.flush()
            continue
        }

        commandHistory.add(input)
        historyFile.appendText("$input${System.lineSeparator()}")

    // TODO Implement Different answers Like
        //  - "I don't know how to do that"
        //  - "I need Mode Data"


        val userMessageText = "$input\nonly give the cmd output and no explanation of what is it doing or what will happen or in code block | just plain text"
        promptMessages.add(Message(role = "user", content = userMessageText))

        val req = ChatRequest(
            model = model,
            messages = promptMessages,
            temperature = config?.temperature ?: 0.7,
        )

        try {
            val fullResponse = java.lang.StringBuilder()
            print("${ANSI_PURPLE}Generating command: ")
            restClient.stream(req).collect { chunk ->
                print(chunk)
                fullResponse.append(chunk)
            }
            println(ANSI_RESET)
            var textOutput = fullResponse.toString().trim()

            if (textOutput.isEmpty()) {
                println("${ANSI_YELLOW}No output generated.${ANSI_RESET}")
                continue
            }

            // TODO Optimize this checking of the command
            // Clean up markdown code blocks if the AI includes them
            if (textOutput.startsWith("```")) {
                val lines = textOutput.lines().toMutableList()
                if (lines.first().startsWith("```")) {
                    lines.removeFirst()
                }
                if (lines.last().startsWith("```")) {
                    lines.removeLast()
                }
                textOutput = lines.joinToString(System.lineSeparator()).trim()
            }

            val cmdToken = textOutput.split(" ")

            if (cmdToken.isNotEmpty()) {
                val command = cmdToken[0].lowercase()
                if (BlockedCommands.contains(command)) {
                    println("${ANSI_RED}Error: Blocked Command detected ($textOutput)${ANSI_RESET}")
                    promptMessages.removeLast() // remove the last user message to not pollute history
                    continue
                }
            }

    // TODO fix theallowed commands checking

            // Ask for confirmation
            print("${ANSI_YELLOW}Execute this command? [Y/n]: ${ANSI_RESET}")
            val confirm = readlnOrNull()?.trim()?.lowercase()
            if (confirm == "n" || confirm == "no" || confirm?.startsWith("n") == true) {
                println("${ANSI_YELLOW}Command execution cancelled.${ANSI_RESET}")
                // Remove the generated text from history since it wasn't executed
                promptMessages.removeLast()
                continue
            }
            println("${ANSI_PURPLE}Executing: $textOutput${ANSI_RESET}")
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
                println("${ANSI_WHITE}$line${ANSI_RESET}")
                outputBuilder.append(line).append("\n")
            }

            process.waitFor()

            // Add Assistant response to the history so model has context
            promptMessages.add(Message(role = "assistant", content = textOutput))

            promptMessages.add(Message(role = "user", content = "Command Output:\n${outputBuilder.toString()}"))

        } catch (e: Exception) {
            println("${ANSI_RED}Synchronous execution error: ${e.message}\n${ANSI_RESET}")
            promptMessages.removeLast() // rollback on error
        }
    }
}
