# CLI Helper powered by Mistral AI

A command-line tool built with Kotlin that leverages Mistral AI to translate natural language into executable terminal/command-prompt commands.

## Features

- **Cross-Platform**: Executes `cmd /c` on Windows and `bash -c` on Unix/Linux systems.
- **Context-Aware**: Remembers the conversation history to perform multi-step operations efficiently.
- **Safety**: Built-in blocked commands list (e.g., `rmdir`, `del`, `rm`) prevents execution of destructive operations.
- **Streaming Context**: Instructs the AI using `system.txt` for comprehensive prompt conditioning.

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
4. The application executes the command and prints the output to your console.
5. Type `exit` or `quit` to stop the application.

## Blocked Commands

For security reasons, the following commands are strictly prohibited and will be rejected:
- `rmdir`
- `del`
- `rm`
