# PPE Essential

[![Modrinth](https://img.shields.io/badge/Modrinth-PPE%20Essential-00AF5C?style=flat&logo=modrinth&logoColor=white)](https://modrinth.com/mod/ppe-essential) [![CurseForge](https://img.shields.io/badge/CurseForge-PPE%20Essential-F16436?style=flat&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/ppe-essential) [![GitHub](https://img.shields.io/badge/GitHub-PurpleEastZQ%2FPPE--Essential-181717?style=flat&logo=github&logoColor=white)](https://github.com/PurpleEastZQ/PPE-Essential)

PPE Essential is a lightweight mod that adds useful commands (like /tpa /repeat) and tweaks, with highly configurable options.

![PPE Essential command preview](https://raw.githubusercontent.com/PurpleEastZQ/PPE-Essential/master/artwork/ppe-essential-help-preview.png)

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

- NeoForge 1.21.1
- Fabric 1.21.1

Additional Minecraft 1.21+ versions are planned.

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
| `/ppe-essential help` | Show the command list. Disabled or unavailable commands are shown in red. |
| `/ppe-essential reset all` | Clear all player data. |
| `/ppe-essential reset notice` | Clear notice trigger data. |

<br>

## Configuration

All configuration options are located in `config/ppe_essential-common.toml`.

<br>

## Building & Installation

Build NeoForge:

```powershell
.\gradlew.bat buildNeoForge --no-daemon
```

Build Fabric:

```powershell
.\gradlew.bat buildFabric --no-daemon
```

When passing PowerShell project properties, quote the whole `-P...` argument:

```powershell
.\gradlew.bat buildNeoForge "-Ptarget_minecraft_version=1.21.11" --no-daemon
.\gradlew.bat buildFabric "-Ptarget_minecraft_version=1.21.11" --no-daemon
```

Build environment requirements:

- JDK 25

Built jars are generated under `build/libs/`.

Put the matches jar into your `mods` folder. Fabric requires Fabric API.
