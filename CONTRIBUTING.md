# Contributing to EpicDuels

Thank you for your interest in contributing to EpicDuels! This document outlines guidelines for contributing to the project.

## How to Contribute

### Reporting Bugs

- Open an issue on GitHub with a clear title and description
- Include your server version (Paper build number), Java version, and EpicDuels version
- Describe the steps to reproduce the bug
- Include any relevant error messages or stack traces from the console

### Suggesting Features

- Open an issue with the label `enhancement`
- Describe the feature and why it would be useful
- If possible, sketch out how you imagine it working

### Submitting Pull Requests

1. **Fork** the repository and create a new branch from `main`
2. **Name your branch** descriptively, e.g. `feat/spectator-mode` or `fix/arena-cleanup`
3. **Make your changes** — keep commits focused and atomic
4. **Test your changes** on a real Paper 1.21.1 server before submitting
5. **Open a Pull Request** against `main` with a clear description of what changed and why

## Code Style

- Java 21, Paper 1.21.1 API
- 4-space indentation
- Follow the existing package structure (`manager/`, `model/`, `listener/`, `command/`, `world/`)
- Use the Adventure API for all text components — no legacy `ChatColor`
- All managers are instantiated in `EpicDuels#onEnable()` and accessed via plugin getters
- Avoid adding features or abstractions beyond what the task requires

## Project Structure

```
src/main/java/dev/epicduels/
├── EpicDuels.java          # Main plugin class, manager init, architecture docs
├── command/
│   ├── DuelCommand.java    # All /duel subcommands
│   └── DuelTabCompleter.java
├── listener/
│   ├── GUIListener.java    # Inventory click routing
│   ├── PlayerListener.java # Join, quit, death, freeze
│   └── WorldProtectionListener.java
├── manager/
│   ├── ArenaManager.java   # Arena CRUD + world copy/delete
│   ├── DuelManager.java    # Duel lifecycle
│   ├── GUIManager.java     # All chest GUIs
│   ├── KitManager.java     # Kit CRUD
│   ├── QueueManager.java   # Matchmaking queue
│   └── StatsManager.java   # Win/loss tracking
├── model/
│   ├── Arena.java
│   ├── DuelInstance.java
│   ├── DuelRequest.java
│   ├── Kit.java
│   └── PlayerStats.java
└── world/
    └── VoidWorldGenerator.java
```

## Building

```bash
# Gradle (recommended)
gradle clean build

# Maven
mvn clean package
```

Output: `build/libs/EpicDuels-<version>.jar`

## License

By contributing you agree that your contributions will be licensed under the same **CC BY-NC-SA 4.0** license as the rest of the project.
