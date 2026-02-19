# Commands

This page lists all the available commands in the CoolCars plugin. The main command is `/car` (or its alias `/cars`), with other utility commands available for fuel and repairs.

> **Developer Note:** A video here could quickly demonstrate each of the key commands. Show a player spawning a car (`/car spawn`), getting information about it (`/car info`), teleporting it (`/car tp`), and finally removing it (`/car remove`). This gives a fast overview of the admin workflow.

## Main Command: `/car` or `/cars`

### `/car help`
**Description:** Displays a list of all available car commands.

### `/car spawn <model>`
**Description:** Spawns a new car of the specified model.
**Arguments:**
*   `<model>`: The model key (the folder name) of the car you want to spawn.
**Permission:** `coolcars.command.spawn` (example, needs verification)

### `/car info <uuid>`
**Description:** Displays detailed information about a specific car.
**Arguments:**
*   `<uuid>`: The unique ID of the car. You can get this from `/car list`.

### `/car list`
**Description:** Lists all active cars on the server, showing their model, owner, and UUID.

### `/car remove <uuid>`
**Description:** Removes a specific car from the world.
**Arguments:**
*   `<uuid>`: The UUID of the car to remove.
**Permission:** `coolcars.command.remove` (example)

### `/car tp <uuid> <player>`
**Description:** Teleports a car to a player's location.
**Arguments:**
*   `<uuid>`: The UUID of the car.
*   `<player>`: The name of the player to teleport the car to.
**Permission:** `coolcars.command.tp` (example)

### `/car key give <player> <uuid>`
**Description:** Gives a player a key for a specific car.
**Arguments:**
*   `<player>`: The name of the player to give the key to.
*   `<uuid>`: The UUID of the car the key belongs to.
**Permission:** `coolcars.command.key` (example)

### `/car reload`
**Description:** Reloads all plugin configurations, including car models.
**Permission:** `coolcars.command.reload` (example)

### `/car repairhitbox <on|off>`
**Description:** Toggles the visibility of repair hitboxes for administrators, which can help with configuration.
**Permission:** `coolcars.command.repairhitbox` (example)

### `/car heal <uuid> <target> <amount|full>`
**Description:** Heals a car or its specific parts.
**Arguments:**
*   `<uuid>`: The UUID of the car.
*   `<target>`: The part to heal (`car`, `front`, `rear`, `wheels`).
*   `<amount|full>`: The amount of health to restore, or `full` to completely heal.
**Permission:** `coolcars.command.heal` (example)

## Fuel & Repair Commands

### `/fuel give [liters]`
**Description:** Gives you a fuel canister.
**Arguments:**
*   `[liters]`: (Optional) The amount of fuel in the canister.

### `/fuel set <liters>`
**Description:** Sets the fuel level of the canister you are holding.
**Arguments:**
*   `<liters>`: The amount of fuel to set.

### `/repair give [units]`
**Description:** Gives you a repair kit.
**Arguments:**
*   `[units]`: (Optional) The number of repair units in the kit.

### `/repair set <units>`
**Description:** Sets the repair units of the kit you are holding.
**Arguments:**
*   `<units>`: The number of units to set.

## Language Command

### `/lang set <en|ru>` / `/lang auto`
**Description:** Sets your personal language for the plugin UI, or sets it to automatically detect your client language.

### `/lang server <en|ru>`
**Description:** Sets the default language for the entire server.
**Permission:** `coolcars.command.lang.server` (example)
