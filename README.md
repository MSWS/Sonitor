# Sonitor

Sonitor is an application that combines the SteamAge program with a brand new Jailbreak Analyzer. Sonitor has all the
features and configurability of SteamAge but with Jailbreak Logs integrated into itself.

## Installation

_Note_: for Counter-Strike: Source Players, you will have to manually specify the game's directory.<br>
_Note_: the EXE file is recommended for novice users, the JAR version is supplied for compatability.

1. Download Sonitor.exe
2. (Optional) Move it to a preferred folder
3. Run Sonitor - First time will generate a `settings.txt` file.
4. The generated file should automatically open.
5. Open the link that is after `steamkey=`.
6. Register your Steam API Key.
7. Paste it back into `settings.txt`.
8. (Optional) Mess around with the `settings.txt` file, Sonitor is very configurable
9. Run CS.

## Features

- Parse and analyze Jailbreak Logs
- Summarizes the round in fewer words, with less spam
- Supports button aliases. If you run into an Unknown (#00000) format, you can set an alias and logs will reflect alias
- Prints out potentially suspect situations such as:
    - When a CT kills a prisoner after the warden has died
    - When a CT kills a prisoner less than three seconds after a warden has been selected
    - When a CT breaks vents before a prisoner has
    - When a CT drops a gun and a prisoner uses the same gun
    - When a player pushes a button and other players die by the world soon after
    - When a player throws a grenade and other players die by the world soon after
    - When a spectator deals damage to players

### Usage

Sonitor will watch over the console output of Counter-Strike and print out events when they trigger. For account ages,
this happens when you type `status` in the console. For Jailbreak Logs, these trigger when the round ends.

### Examples

![Image](https://i.imgur.com/XqrZMFu.png)