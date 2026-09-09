# VapeVlite

Minecraft Forge 1.8.9 client project using Forge 11.15.1.2318 and Java 8.

## Modules

- AutoClicker — CPS 1-20, uses left mouse button.
- Aim Assist — Range and Speed settings.
- Jump Reset — configurable hit-delay before jumping.
- Backtrack — client-side entity position history with configurable delay.
- Reach — extended entity attack ray up to 6 blocks.

## GUI

- Press **RSHIFT** to open/close.
- Click a module to select it.
- Toggle modules and adjust numeric settings.
- Use **Key** to assign a keyboard key to a module.
- Settings are stored in `config/vapevlite.json` and autosaved.

## Build

Use JDK 8. The GitHub Actions workflow installs Gradle 4.10.3 and builds the Forge 1.8.9 project.

This is an independent implementation. It does not include proprietary source code copied from another client.
