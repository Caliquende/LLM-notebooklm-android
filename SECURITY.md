# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| latest  | :white_check_mark: |

## Reporting a Vulnerability

1. **DO NOT** open a public GitHub issue for security vulnerabilities.
2. Report via [GitHub Security Advisory](https://github.com/Caliquende/LLM-notebooklm-android/security/advisories/new).
3. Include a detailed description, steps to reproduce, and any potential impact.
4. We will acknowledge your report within 48 hours.

## Security Considerations

- **API Keys:** Never hardcode API keys or tokens in the source code. Use Android's `local.properties` or environment variables.
- **Network Security:** All network communication should use HTTPS/TLS.
- **Data Storage:** Sensitive data should be stored using Android's EncryptedSharedPreferences.
- **Dependencies:** Monitored via Dependabot for Gradle and pip vulnerabilities.
