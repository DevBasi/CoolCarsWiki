# Permissions

This page provides a reference for all the permissions used by the CoolCars plugin. Granting these permissions to players or groups will allow them to use specific commands and features.

**Note:** The permission nodes listed here are based on common plugin conventions. The exact nodes may vary. You should always check the plugin's official documentation or source if you have trouble.

## Player Permissions

These permissions are generally safe to grant to regular players.

| Permission Node | Description |
| :--- | :--- |
| `coolcars.player.enter` | Allows a player to enter and drive a car. (This is a likely default permission) |
| `coolcars.player.use` | Allows the use of basic car features like the horn and menu. (Likely default) |

## Command Permissions

These permissions control access to the plugin's commands. They are typically granted to administrators or staff members.

| Permission Node | Related Command | Description |
| :--- | :--- | :--- |
| `coolcars.command.spawn` | `/car spawn` | Allows a player to spawn new cars. |
| `coolcars.command.remove` | `/car remove` | Allows a player to remove cars. |
| `coolcars.command.list` | `/car list` | Allows viewing the list of all cars. |
| `coolcars.command.info` | `/car info` | Allows viewing detailed info about a car. |
| `coolcars.command.tp` | `/car tp` | Allows teleporting a car. |
| `coolcars.command.key` | `/car key give` | Allows giving car keys to players. |
| `coolcars.command.heal` | `/car heal` | Allows healing cars and their parts. |
| `coolcars.command.fuel` | `/fuel` | Allows use of the fuel commands. |
| `coolcars.command.repair`| `/repair` | Allows use of the repair kit commands. |

## Administrative Permissions

These permissions are for top-level server management and should only be given to trusted administrators.

| Permission Node | Related Command | Description |
| :--- | :--- | :--- |
| `coolcars.admin` | (All commands) | A potential wildcard permission that grants access to all CoolCars commands. |
| `coolcars.command.reload` | `/car reload` | Allows reloading the entire plugin configuration. |
| `coolcars.command.repairhitbox`| `/car repairhitbox`| Allows toggling the visibility of repair hitboxes. |
| `coolcars.command.lang.server`| `/lang server` | Allows changing the default language for the whole server. |
