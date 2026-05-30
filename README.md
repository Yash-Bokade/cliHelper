# CLI Helper powered by Mistral AI

A command-line tool built with Kotlin that leverages Mistral AI to translate natural language into executable terminal/command-prompt commands.

## Features

- **Cross-Platform**: Executes `cmd /c` on Windows and `bash -c` on Unix/Linux systems.
- **Context-Aware**: Remembers the conversation history to perform multi-step operations efficiently.
- **Safety**: Built-in blocked commands list (e.g., `rmdir`, `del`, `rm`) prevents execution of destructive operations, and prompts for user confirmation before executing any AI-generated command.
- **Streaming Context**: Instructs the AI using `system.txt` for comprehensive prompt conditioning.
- **Colorized Output**: Distinguishes between user input, AI commands, and terminal output using ANSI colors.
- **Built-in Commands**: Includes `help` and `clear` commands executed locally for convenience.

## Prerequisites

- Java 21 or higher.
- A valid Mistral API key.

## Installation & Setup

1. Clone this repository.
2. Ensure you have the required Java version installed.

## Usage

You must provide your Mistral API key via the `MISTRAL_API_KEY` environment variable.

### Running with Gradle (Unix/Linux/macOS)

```bash
MISTRAL_API_KEY="your_mistral_api_key" ./gradlew run
```

### Running with Gradle (Windows)

```cmd
set MISTRAL_API_KEY=your_mistral_api_key
gradlew.bat run
```

### Passing a Custom Model

By default, the application uses the `mistral-small-latest` model. You can specify a different model by passing it as an argument:

```bash
MISTRAL_API_KEY="your_api_key" ./gradlew run --args="mistral-large-latest"
```

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
