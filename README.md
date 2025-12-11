# Function Comments

![Build](https://github.com/luminarix/phpstorm-function-comments/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/29338.svg)](https://plugins.jetbrains.com/plugin/29338)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/29338.svg)](https://plugins.jetbrains.com/plugin/29338)

<!-- Plugin description -->
A PhpStorm plugin that lets you quickly comment or uncomment entire PHP functions with a single keyboard shortcut.

**Features:**
- Comment/uncomment complete functions including PHP 8+ attributes
- Single-line (`//`) and multi-line (`/* */`) comment styles
- Toggle behavior: automatically detects if function is commented and performs the opposite action
- Smart handling of nested block comments (falls back to single-line style when necessary)
- Available via keyboard shortcuts and editor context menu

**Known Limitations:**
- No proper support for anonymous functions or closures

**Roadmap:**
- Support for anonymous functions and closures


## Usage

Place your cursor anywhere inside a PHP function and use:

| Action | Shortcut | Description |
|--------|----------|-------------|
| Comment Function (Single-Line) | <kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>/</kbd> | Comments each line with `//` |
| Comment Function (Multi-Line) | <kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>Shift</kbd>+<kbd>/</kbd> | Wraps function in `/* */` |


Both actions are also available via right-click menu under **Function Comments**.

To uncomment, use the same shortcut on an already-commented function.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Function Comments"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Download the [latest release](https://github.com/luminarix/phpstorm-function-comments/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
