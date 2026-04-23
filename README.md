# ResearchFlow Android

ResearchFlow is a local-first, AI-powered research assistant that bridges your Android device with a powerful research pipeline (NotebookLM CLI). It allows you to perform deep web research, collect sources, and generate research artifacts like reports, audio summaries, and flashcards directly from your phone.

## 🚀 Architecture

The project consists of two main components:
1.  **Android App**: A modern Jetpack Compose application with MVVM architecture, Room persistence, and WorkManager integration.
2.  **Bridge Server**: A lightweight FastAPI (Python) server that acts as an intermediary between the mobile app, LLM providers (OpenAI, Gemini, Groq, OpenRouter), and the local NotebookLM infrastructure.

## ✨ Key Features

- **Multi-Provider Research**: Search the web using OpenAI, Google Gemini, Groq, or OpenRouter.
- **NotebookLM Integration**: Automated handoff of found sources to a local NotebookLM instance.
- **Artifact Generation**: Generate reports, audio summaries, quizzes, and mind maps.
- **Local-First Design**: All research data is stored locally on your device (Room DB) and your local bridge server.
- **Premium UI**: Sleek Glassmorphism design with dark mode support.

## ⚠️ Important Warnings

- **Local Bridge Required**: This app is designed to work with a local `bridge-server` running on the same network or accessible via a tunnel.
- **API Keys**: You must provide your own API keys for the LLM providers in the App Settings.
- **Security**: 
    - Cleartext HTTP traffic is **only permitted for localhost/emulator host (10.0.2.2)** in **debug builds**.
    - Release builds strictly enforce HTTPS and have sensitive data backup disabled.
    - API keys are stored using `EncryptedSharedPreferences`.
- **NotebookLM Setup**: Ensure the `codex-notebooklm` CLI is correctly installed and authenticated on the machine running the bridge server.

## 🛠️ Setup Instructions

### Bridge Server (Python)
1. Navigate to `bridge-server/`.
2. Install dependencies: `pip install -r requirements.txt`.
3. Set the environment variable `CODEX_NOTEBOOKLM_DIR` to your local `codex-notebooklm` path.
4. Run the server: `python server.py`.

### Android App
1. Open the project in Android Studio.
2. Build and run the `app` module on an emulator or physical device.
3. In the app settings, configure the **Bridge Server URL** (e.g., `http://10.0.2.2:8080` for emulator).
4. Add at least one **API Key** (Groq and OpenRouter are recommended for free-tier research).

## 📄 License

This project is for educational and personal research purposes.
