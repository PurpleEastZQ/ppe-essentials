# PPE Essentials

[![Modrinth](https://img.shields.io/badge/Modrinth-PPE%20Essentials-00AF5C?style=flat&logo=modrinth&logoColor=white)](https://modrinth.com/mod/ppe-essentials) [![CurseForge](https://img.shields.io/badge/CurseForge-PPE%20Essentials-F16436?style=flat&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/ppe-essentials) [![GitHub](https://img.shields.io/badge/GitHub-PurpleEastZQ%2Fppe--essentials-181717?style=flat&logo=github&logoColor=white)](https://github.com/PurpleEastZQ/ppe-essentials)

PPE Essentials is a lightweight mod that adds useful commands (like /tpa /repeat) and tweaks, with highly configurable options.

![PPE Essentials command preview](https://raw.githubusercontent.com/PurpleEastZQ/ppe-essentials/master/artwork/ppe-essentials-help-preview.png)

<br>

## Features

- Server-side only mod (Can still be installed on your client for singleplayer use.)
- Configurable command toggles, permission levels and more.
- Configurable protection against creeper, enderman, and ravager block damage.
- No extra dependencies, ready out of the box.
- Polished messages with clickable buttons, titles, and sounds.
- Multilingual support with automatic client language detection and a configurable `fallbackLanguage`.
- Persistent player data for homes, warps, back locations, fly, god mode, and notice triggers.

<br>

## Supported Versions

- NeoForge 1.21.1, 1.21.11, 26.1.2
- Fabric 1.21.1, 1.21.11, 26.1.2

<br>

## Commands

### Teleport

| Command | Description |
| --- | --- |
| `/tpa <player>` | Request to teleport to another player. |
| `/tpaa` | Accept a pending TPA request. |
| `/tpad` | Deny a pending TPA request. |
| `/tpaauto` | Toggle automatic TPA acceptance. |
| `/tpahere <player>` | Request another player to teleport to you. |
| `/tpaherea` | Accept a pending TPAhere request. |
| `/tpahered` | Deny a pending TPAhere request. |
| `/rtp` | Randomly teleport nearby. |
| `/spawn` | Teleport to world spawn. |
| `/back` or `/dback` | Return to your last death location. |
| `/tback` | Return to your previous teleport location. |

<br>

### Homes And Warps

| Command | Description |
| --- | --- |
| `/sethome` | Set your home. |
| `/home` | Teleport home. |
| `/delhome` | Delete your home. |
| `/setwarp <name>` | Create a server warp. |
| `/warp <name>` | Teleport to a server warp. |
| `/delwarp <name>` | Delete a server warp. |

<br>

### Utility And Admin

| Command | Description |
| --- | --- |
| `/trash` | Open a temporary trash inventory that clears on close. |
| `/suicide` | Kill yourself. |
| `/heal [player]` | Heal yourself or another player. |
| `/fly [player]` | Toggle flight for yourself or another player. |
| `/god [player]` | Toggle god mode for yourself or another player. |
| `/repeat <times> [command]` | Repeat your last command or a specified command. |
| `/ppe-ess help` | Show the command list. Disabled or unavailable commands are shown in red. |
| `/ppe-ess reset all` | Clear all player data. |
| `/ppe-ess reset notice` | Clear notice trigger data. |

<br>

## Configuration

All configuration options are located in `config/ppe_essentials-common.toml`.
The `[PPE]` message prefix is disabled by default. Set `messagePrefixEnabled = true` to show it.

<br>

## Building & Installation

To build every supported NeoForge and Fabric jar:

```powershell
.\build-all-versions.ps1
```

The script discovers supported Minecraft versions from `gradle.properties` and `src/versions/`.
Before building, it removes previously generated PPE Essentials jars from `build/libs/`.
Intermediate Gradle outputs are isolated under `build/work/<loader>/<minecraft-version>/` to prevent targets from reusing each other's classes.
Use `-Clean` to additionally clean all build outputs once before the build matrix, or limit the run when needed:

```powershell
.\build-all-versions.ps1 -Clean
.\build-all-versions.ps1 -MinecraftVersion 1.21.11 -Loader fabric
```

For single-target builds, pass PowerShell project properties by quoting the whole `-P...` argument:

```powershell
.\gradlew.bat buildNeoForge --no-daemon
.\gradlew.bat buildFabric --no-daemon
.\gradlew.bat buildNeoForge "-Ptarget_minecraft_version=1.21.11" --no-daemon
.\gradlew.bat buildFabric "-Ptarget_minecraft_version=1.21.11" --no-daemon
.\gradlew.bat buildNeoForge "-Ptarget_minecraft_version=26.1.2" --no-daemon
.\gradlew.bat buildFabric "-Ptarget_minecraft_version=26.1.2" --no-daemon
```

Built jars are generated under `build/libs/`. Put the matching jar into your `mods` folder.

Fabric requires Fabric API.
