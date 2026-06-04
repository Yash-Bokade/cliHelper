# CLI Helper powered by Mistral AI

A command-line tool built with Kotlin that leverages Mistral AI to translate natural language into executable terminal/command-prompt commands.
[Installation Process](https://yash-bokade.github.io/cliHelper/pages)
## Features

- **Cross-Platform**: Executes `cmd /c` on Windows and `bash -c` on Unix/Linux systems.
- **Context-Aware**: Remembers the conversation history to perform multi-step operations efficiently. (currently working on that)
- **Safety**: Built-in blocked commands list (e.g., `rmdir`, `del`, `rm`) prevents execution of destructive operations, and prompts for user confirmation before executing any AI-generated command.(prefered to read command before executing)
- **Streaming Context**: Instructs the AI using `system.txt` for comprehensive prompt conditioning. (Modify the file to suit your needs)
- **Colorized Output**: Distinguishes between user input, AI commands, and terminal output using ANSI colors.
- **Built-in Commands**: Includes `help` and `clear` commands executed locally for convenience. (Trying to add more commands)

## Prerequisites

- Java 21 or higher.
- A valid Mistral API key.

## Installation & Setup

1. Download the `cliHelper.jar` from the [releases](https://github.com/joseph-kang/cliHelper/releases) page.
2. Ensure you have the required Java version installed.

## Usage

You must provide your Mistral API key While running the jar file.

```bash
java -jar .\cliHelper.jar "mistral-small-latest" "API_KEY"
```

### Passing a Custom Model

By default, the application uses the `mistral-small-latest` model. You can specify a different model by passing it as an argument:

## How It Works

1. Run the application. You'll see a prompt `> `.
2. Type a natural language command, e.g., "list all files in the current directory".
3. The AI processes your request and translates it into a shell command.
4. You will be prompted to confirm execution: `Execute this command? [Y/n]`.
5. The application executes the command and prints the output to your console.
6. Type `help` to see available local commands, `clear` to clear the terminal, or `exit`/`quit` to stop the application.

## Blocked Commands

For security reasons, the following commands are strictly prohibited and will be rejected:
- `rmdir`
- `del`
- `rm`
