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

val prettyJson = Json { prettyPrint = true }

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
        a.writeText(prettyJson.encodeToString(
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
    val apiKey = args.getOrNull(1) ?: System.getenv("MISTRAL_API_KEY") ?: config?.apiKey

    if (apiKey.isNullOrEmpty()) {
        println("${ANSI_RED}Error: Please set the MISTRAL_API_KEY environment variable, provide it as a CLI argument, or set it in Config.json.${ANSI_RESET}")
        return@runBlocking
    }
    println("${ANSI_YELLOW}Using model: $model${ANSI_RESET}")
    println("${ANSI_YELLOW}Type 'help' for list of commands.${ANSI_RESET}")
    println("${ANSI_RED}Type 'exit' or 'quit' to close.${ANSI_RESET}")
    println("${ANSI_CYAN}=====================================================${ANSI_RESET}")

    val promptFileName = config?.systemPrompt ?: "system.txt"
    val promptFile = File(promptFileName)

    val rawSystemPrompt = if (promptFile.exists()) {
        promptFile.readText()
    } else {
        object {}.javaClass.getResourceAsStream("/$promptFileName")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: object {}.javaClass.getResourceAsStream("/system.txt")
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: throw Exception("Could not find $promptFileName or system.txt on the filesystem or in resources")
    }

    val systemPrompt = "$rawSystemPrompt" + System.lineSeparator() + System.lineSeparator() + "IMPORTANT: The current operating system is ${System.getProperty("os.name")}. Ensure all generated commands are compatible with this OS."

    val restClient = MistralRestClient(apiKey)

    val promptMessages = mutableListOf(
        Message(role = "system", content = systemPrompt)
    )

    val userHome = System.getProperty("user.home")
    val historyFile = File(userHome, ".clihelper_history")
    val commandHistory = mutableListOf<String>()

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
            println("  ${ANSI_CYAN}reload${ANSI_RESET} - Reload configuration")
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

        // Export history to a file
        if (input.startsWith("export ", ignoreCase = true)) {
            val parts = input.split(" ", limit = 2)
            if (parts.size > 1 && parts[1].isNotBlank()) {
                val fileName = parts[1]
                try {
                    val exportFile = File(fileName)
                    exportFile.writeText(commandHistory.joinToString(System.lineSeparator()) + System.lineSeparator())
                    println("${ANSI_GREEN}Command history exported to $fileName${ANSI_RESET}")
                } catch (e: Exception) {
                    println("${ANSI_RED}Failed to export history: ${e.message}${ANSI_RESET}")
                }
            } else {
                println("${ANSI_YELLOW}Please provide a filename, e.g., 'export history.txt'${ANSI_RESET}")
            }
            continue
        }

        // Shows loaded Config file
        if (input.equals("config", ignoreCase = true)) {
            println("${ANSI_CYAN}Configuration:${ANSI_RESET}")
            println("  Model: $model")
            println("  API Key: ${apiKey.take(4)}...${apiKey.takeLast(4)}")
            println("  System Prompt: ${config?.systemPrompt ?: "Not set"}")
            println("  Allowed Commands: ${AllowedCommands.joinToString(", ")}")
            println("  Blocked Commands: ${BlockedCommands.joinToString(", ")}")
            continue
        }

        // Reload command
        if (input.equals("reload", ignoreCase = true)) {
            setupConfig()
            println("${ANSI_GREEN}Configuration reloaded.${ANSI_RESET}")
            continue
        }

        // allows update of model
        if (input.equals("model", ignoreCase = true)) {
            println("Current Model = ${model}")
            println("Enter New Model Name")
            val d = readln().trim()

            if (d.isEmpty()) {
                println("${ANSI_YELLOW}Model name cannot be empty. Model unchanged.${ANSI_RESET}")
                continue
            }
            if (!d.contains("-")) {
                println("${ANSI_YELLOW}Warning: Valid Mistral models typically look like 'mistral-small-latest'.${ANSI_RESET}")
            }

            if (d != model) {
                model = d
                println("Model changed to $model")
            } else {
                println("Model is already $model")
            }
            System.out.flush()
            continue
        }

        // another version for inline model updating
        if (input.split(" ")[0].equals("model" , ignoreCase = true)) {
            val parts = input.split(" ")
            if (parts.size > 1 && parts[1].isNotBlank()) {
                model = parts[1]
                println("Model changed to $model")
            } else {
                println("${ANSI_YELLOW}Please provide a model name, e.g., 'model mistral-small-latest'${ANSI_RESET}")
            }
            System.out.flush()
            continue
        }

        commandHistory.add(input)
        try {
            historyFile.appendText(input + System.lineSeparator())
        } catch (e: Exception) {
            // Silently ignore if history cannot be written
        }

    // TODO Implement Different answers Like
        //  - "I don't know how to do that"
        //  - "I need Mode Data"


        val userMessageText = "$input" + System.lineSeparator() + "only give the cmd output and no explanation of what is it doing or what will happen or in code block | just plain text"
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
            textOutput = textOutput
                .replace(Regex("^```[a-zA-Z]*\\r?\\n", RegexOption.MULTILINE), "")
                .replace(Regex("\\r?\\n```$", RegexOption.MULTILINE), "")
                .replace(Regex("^```$", RegexOption.MULTILINE), "")
                .trim()

            val cmdToken = textOutput.split(" ")

            if (cmdToken.isNotEmpty()) {
                val command = cmdToken[0].substringAfterLast("/").substringAfterLast("\\").lowercase()
                if (BlockedCommands.contains(command)) {
                    println("${ANSI_RED}Error: Blocked Command detected ($textOutput)${ANSI_RESET}")
                    promptMessages.removeLast() // remove the last user message to not pollute history
                    continue
                }
            }

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
                outputBuilder.append(line).append(System.lineSeparator())
            }

            process.waitFor()

            // Add Assistant response to the history so model has context
            promptMessages.add(Message(role = "assistant", content = textOutput))

            promptMessages.add(Message(role = "user", content = "Command Output:" + System.lineSeparator() + "${outputBuilder.toString()}"))

        } catch (e: Exception) {
            println("${ANSI_RED}Synchronous execution error: ${e.message}" + System.lineSeparator() + "${ANSI_RESET}")
            promptMessages.removeLast() // rollback on error
        }
    }
}
