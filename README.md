# File Sharing Server

A simple Java application to easily share files via HTTP server.

## Features

- **HTTP server on port 8181**
- **Multi-language support** (German/English based on system locale)
- **Simple GUI** with server control
- **File size display**

## Requirements

- Java Runtime Environment (JRE) 7 or higher
- Windows operating system

## Usage

### Direct Execution
1. Double-click `FileShare.exe`
2. Select file in the dialog
3. Server starts

## How it Works

The application creates a local HTTP server that serves the selected file at:
- **Info page:** `http://localhost:8181`
- **Direct download:** `http://localhost:8181/download`

Other devices on the same network can access the file using your computer's IP address instead of localhost.

<img width="498" height="354" alt="grafik" src="https://github.com/user-attachments/assets/1bb7636f-88c8-4d3e-9773-5045359f7c05" />
<img width="431" height="238" alt="grafik" src="https://github.com/user-attachments/assets/f5a45d38-387e-41f9-99fa-50ae6dc62f6b" />

## Supported File Types

basically any file extension

## License

This project is licensed under the MIT License.
