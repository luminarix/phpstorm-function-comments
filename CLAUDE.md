# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PhpStorm Function Comments is an IntelliJ Platform plugin targeting PhpStorm. Built with Kotlin using the IntelliJ Platform Gradle Plugin (version 2).

## Build Commands

```bash
# Build the plugin (outputs ZIP to build/distributions/)
./gradlew buildPlugin

# Run plugin in development IDE sandbox
./gradlew runIde

# Run tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.github.luminarix.phpstormfunctioncomments.MyPluginTest"

# Run a specific test method
./gradlew test --tests "com.github.luminarix.phpstormfunctioncomments.MyPluginTest.testProjectService"

# Verify plugin compatibility with target IDEs
./gradlew verifyPlugin

# Clean build
./gradlew clean build
```

## Architecture

- **Platform**: IntelliJ Platform (PhpStorm), Kotlin, JVM 21
- **Plugin ID**: `com.github.luminarix.phpstormfunctioncomments`
- **Target**: PhpStorm 2025.2+ (sinceBuild: 252)

### Key Directories

- `src/main/kotlin/` - Plugin source code
- `src/main/resources/META-INF/plugin.xml` - Plugin configuration and extension points
- `src/main/resources/messages/` - i18n message bundles
- `src/test/kotlin/` - Tests using `BasePlatformTestCase`
- `src/test/testData/` - Test fixtures

### Extension Points (plugin.xml)

The plugin registers extensions in `plugin.xml`:
- `toolWindow` - Custom tool window factory
- `postStartupActivity` - Project startup hook

### Project Service Pattern

Services use `@Service(Service.Level.PROJECT)` annotation and are retrieved via:
```kotlin
project.service<MyProjectService>()
```

## Configuration

- `gradle.properties` - Plugin metadata, version, platform target
- `build.gradle.kts` - Build configuration, dependencies, signing/publishing setup
- Plugin description is extracted from `README.md` between `<!-- Plugin description -->` markers

## Testing

Tests extend `BasePlatformTestCase` from the IntelliJ test framework. Test data files go in `src/test/testData/`.
