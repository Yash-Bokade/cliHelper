# CLI Helper powered by Mistral AI

A command-line tool built with Kotlin that leverages Mistral AI to translate natural language into executable terminal/command-prompt commands.

## Features

- **Cross-Platform**: Executes `cmd /c` on Windows and `bash -c` on Unix/Linux systems.
- **Context-Aware**: Remembers the conversation history to perform multi-step operations efficiently. (currently working on that)
- **Safety**: Built-in blocked commands list (e.g., `rmdir`, `del`, `rm`) prevents execution of destructive operations, and prompts for user confirmation before executing any AI-generated command.(prefered to read command before executing)
- **Streaming Context**: Instructs the AI using `system.txt` for comprehensive prompt conditioning. (Modify the file to suit your needs)
- **Colorized Output**: Distinguishes between user input, AI commands, and terminal output using ANSI colors.
- **Built-in Commands**: Includes `help`, `clear`, `history`, `sysinfo`, `model`, and `export` commands executed locally for convenience.

## Prerequisites

- Java 21 or higher.
- A valid Mistral API key.

## Installation & Setup

1. Download the `cliHelper.jar` from the [releases](https://github.com/joseph-kang/cliHelper/releases) page.
2. Ensure you have the required Java version installed.

## Usage

You must provide your Mistral API key either by setting the `MISTRAL_API_KEY` environment variable or passing it as the second argument when running the jar file.

**Using Environment Variable:**
```bash
export MISTRAL_API_KEY="your_api_key_here"
java -jar cliHelper.jar
```

**Using Arguments:**
```bash
java -jar cliHelper.jar "mistral-small-latest" "your_api_key_here"
```

### Built-in Local Commands

- `help` - Show the help message
- `clear` - Clear the terminal screen
- `history` - Show the command history from the current session and previous sessions (persistent history is saved in `~/.clihelper_history`)
- `sysinfo` - Show system information (OS, Java version, and current model)
- `export <filename>` - Export the conversation history with the AI to a file for saving complex workflows (defaults to `conversation.txt` if no filename is given)
- `model <name>` - Change the AI model during runtime
- `exit` / `quit` - Stop the application

## How It Works

1. Run the application. You'll see a prompt `> `.
2. Type a natural language command, e.g., "list all files in the current directory".
3. The AI processes your request and translates it into a shell command.
4. You will be prompted to confirm execution: `Execute this command? [Y/n]`.
5. The application executes the command and prints the output to your console.
6. Type `help` to see available local commands.

## Blocked Commands

For security reasons, the following commands are strictly prohibited and will be rejected:
- `rmdir`
- `del`
- `rm`
