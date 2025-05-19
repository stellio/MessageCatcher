# MessageCatcher

MessageCatcher is an Android application that captures and displays messages from various messaging applications like Viber and Telegram. It provides a clean interface to view message history and manage filtered senders.

## Features

- **Message Capture**: Automatically captures messages from supported messaging apps (Viber, Telegram)
- **Message History**: Displays captured messages in a chronological order
- **Filtered Senders**: Ability to filter out messages from specific senders
- **Message Management**: 
  - Copy message content
  - Add/remove senders to/from filter list
  - Clear message history
- **Notification Listener**: Requires notification access permission to capture messages

## Supported Messaging Apps

- Viber
- Telegram

## Requirements

- Android 7.0 (API level 24) or higher
- Notification access permission
- Storage permission (for Android 9 and below)

## Setup

1. Install the application from the release APK or build from source
2. Grant notification access permission:
   - Open system settings
   - Go to Apps > Special app access > Notification access
   - Enable access for MessageCatcher
3. For Android 9 and below, grant storage permission when prompted

## Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/MessageCatcher.git
   ```

2. Open the project in Android Studio

3. Build the project:
   ```bash
   ./gradlew build
   ```

## Dependencies

- AndroidX Core KTX
- AndroidX AppCompat
- Material Design Components
- Room Database
- Kotlin Coroutines
- AndroidX Lifecycle Components

## Architecture

The application follows a clean architecture approach with the following components:

- **Data Layer**:
  - Room Database for local storage
  - DAOs for data access
  - Entities for data models

- **UI Layer**:
  - Activities for user interface
  - Custom views for message display
  - Material Design components

- **Service Layer**:
  - Notification Listener Service for message capture

## Testing

The project includes unit tests for the data layer:

- MessageDao tests
- FilteredSenderDao tests

To run tests:
```bash
./gradlew test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- AndroidX team for the Room database library
- Material Design team for the UI components
- Kotlin team for the coroutines library 