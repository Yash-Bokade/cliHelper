package me.yashbokade

import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

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

fun main(args: Array<String>) = runBlocking {
    println("${ANSI_CYAN}=====================================================${ANSI_RESET}")
    println("${ANSI_CYAN}         CLI Helper powered by Mistral AI            ${ANSI_RESET}")
    println("${ANSI_CYAN}=====================================================${ANSI_RESET}")

    val apiKey = System.getenv("MISTRAL_API_KEY")
    if (apiKey.isNullOrEmpty()) {
        println("${ANSI_RED}Error: Please set the MISTRAL_API_KEY environment variable.${ANSI_RESET}")
        println("${ANSI_YELLOW}Usage: MISTRAL_API_KEY=your_key ./gradlew run${ANSI_RESET}")
        return@runBlocking
    }

    val model = args.firstOrNull() ?: "mistral-small-latest"
    println("${ANSI_YELLOW}Using model: $model${ANSI_RESET}")
    println("${ANSI_YELLOW}Type 'exit' or 'quit' to close.${ANSI_RESET}")
    println("${ANSI_CYAN}=====================================================${ANSI_RESET}")

    val rawSystemPrompt = object {}.javaClass.getResourceAsStream("/system.txt")?.bufferedReader()?.use { it.readText() }
        ?: throw Exception("Could not find system.txt in resources")
    val systemPrompt = "$rawSystemPrompt\n\nIMPORTANT: The current operating system is ${System.getProperty("os.name")}. Ensure all generated commands are compatible with this OS."

    val restClient = MistralRestClient(apiKey)

    val promptMessages = mutableListOf(
        Message(role = "system", content = systemPrompt)
    )
    val commandHistory = mutableListOf<String>()

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

        if (input.equals("help", ignoreCase = true)) {
            println("${ANSI_CYAN}Commands:${ANSI_RESET}")
            println("  ${ANSI_CYAN}help${ANSI_RESET}  - Show this help message")
            println("  ${ANSI_CYAN}clear${ANSI_RESET} - Clear the terminal screen")
            println("  ${ANSI_CYAN}history${ANSI_RESET} - Show command history")
            println("  ${ANSI_CYAN}sysinfo${ANSI_RESET} - Show system information")
            println("  ${ANSI_CYAN}exit${ANSI_RESET}  - Exit the application")
            println("  ${ANSI_CYAN}quit${ANSI_RESET}  - Exit the application")
            println("Any other input will be processed by the AI.")
            continue
        }

        if (input.equals("clear", ignoreCase = true)) {
            print("\u001b[H\u001b[2J")
            System.out.flush()
            continue
        }

        if (input.equals("history", ignoreCase = true)) {
            println("${ANSI_CYAN}Command History:${ANSI_RESET}")
            commandHistory.forEachIndexed { index, cmd ->
                println("  ${index + 1}: $cmd")
            }
            continue
        }

        if (input.equals("sysinfo", ignoreCase = true)) {
            println("${ANSI_CYAN}System Information:${ANSI_RESET}")
            println("  OS: ${System.getProperty("os.name")}")
            println("  Java Version: ${System.getProperty("java.version")}")
            println("  Model: $model")
            continue
        }

        commandHistory.add(input)

        val userMessageText = "$input\nonly give the cmd output and no explanation of what is it doing or what will happen or in code block | just plain text"
        promptMessages.add(Message(role = "user", content = userMessageText))

        val req = ChatRequest(
            model = model,
            messages = promptMessages,
            temperature = 0.4
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

            // Clean up markdown code blocks if the AI includes them
            if (textOutput.startsWith("```")) {
                val lines = textOutput.lines().toMutableList()
                if (lines.first().startsWith("```")) {
                    lines.removeFirst()
                }
                if (lines.last().startsWith("```")) {
                    lines.removeLast()
                }
                textOutput = lines.joinToString("\n").trim()
            }

            println("${ANSI_PURPLE}Executing: $textOutput${ANSI_RESET}")

            val cmdToken = textOutput.split(" ")

            if (cmdToken.isNotEmpty()) {
                val command = cmdToken[0].lowercase()
                if (BlockedCommands.contains(command)) {
                    println("${ANSI_RED}Error: Blocked Command detected ($textOutput)${ANSI_RESET}")
                    promptMessages.removeLast() // remove the last user message to not pollute history
                    continue
                }
            }

            // Ask for confirmation
            print("${ANSI_YELLOW}Execute this command? [Y/n]: ${ANSI_RESET}")
            val confirm = readlnOrNull()?.trim()?.lowercase()
            if (confirm == "n" || confirm == "no") {
                println("${ANSI_YELLOW}Command execution cancelled.${ANSI_RESET}")
                // Remove the generated text from history since it wasn't executed
                promptMessages.removeLast()
                continue
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
                println("${ANSI_WHITE}$line${ANSI_RESET}")
                outputBuilder.append(line).append("\n")
            }

            process.waitFor()

            // Add Assistant response to the history so model has context
            promptMessages.add(Message(role = "assistant", content = textOutput))

            // We can optionally add the command output back to history if we wanted,
            // but for now let's just let the user see it.
            // promptMessages.add(Message(role = "user", content = "Command Output:\n${outputBuilder.toString()}"))

        } catch (e: Exception) {
            println("${ANSI_RED}Synchronous execution error: ${e.message}\n${ANSI_RESET}")
            promptMessages.removeLast() // rollback on error
        }
    }
}
