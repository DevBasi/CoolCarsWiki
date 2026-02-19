# Global Configuration (config.yml)

The `config.yml` file is the main configuration file for the CoolCars plugin. It controls global settings like the UI, database, language, and core mechanics.

## `cars`

This section defines default car settings.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `default-model` | String | The key of the default car model to be used if another model is not specified. In this case, it's `volga`. |

## `ui.action-bar`

This section controls the dynamic information displayed on the player's action bar while interacting with a car.

> **Developer Note:** A great video here would show a player getting into a car, and the action bar appearing. You could then show refueling and repairing, highlighting how the action bar display changes contextually.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | If `true`, the action bar display is enabled globally. |
| `update-ticks` | Integer | The rate in ticks at which the action bar updates. `20 ticks = 1 second`. |

### `car`

Controls the main driving display.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | Toggles the driving-specific action bar. |
| `update-ticks`| Integer | How often the car's action bar updates. |
| `format` | String | The format of the text using placeholders. See the [Placeholders](placeholders.md) page for a full list. |
| `format-lang-key` | String | A key from the language file to use for the format, allowing for translations. |

### `refuel` & `repair`

Controls the display when refueling or repairing. The parameters are the same as for the `car` section.

## `language`

Manages the plugin's localization.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `global` | Boolean | If `true`, the `default` language is used for all players. |
| `default` | String | The default language to use (e.g., `en` for English). |
| `use-player-locale` | Boolean | If `true`, the plugin will try to use the language of the player's Minecraft client. |

## `storage`

Controls data saving.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `autosave.enabled`| Boolean | If `true`, the plugin will automatically save car data periodically. |
| `autosave.interval-ticks` | Integer | The interval in ticks between automatic saves. |

## `database`

Configures the database for storing persistent car data.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | If `true`, car data is saved to a database. |
| `type` | String | The type of database. Can be `sqlite` or `mysql`. |
| `connection-timeout-seconds` | Integer | How long the plugin will wait to connect to the database. |

### `sqlite`

Settings for the SQLite database (a simple file-based DB).

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `file` | String | The name of the SQLite database file. |

### `mysql`

Settings for a MySQL database server.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `host` | String | The IP address of the MySQL server. |
| `port` | Integer | The port of the MySQL server. |
| `database` | String | The name of the database to use. |
| `username` | String | The username for the database connection. |
| `password` | String | The password for the database connection. |
| `params` | String | Additional connection parameters. |

### `telemetry`

Controls detailed logging of car events to the database.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | If `true`, extended per-car state snapshots are saved. |
| `events.enabled`| Boolean | If `true`, a timeline of events (crash, engine start, etc.) is saved. |

## `commands.car-heal`

Controls the `/cars heal` admin command.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | Toggles the `/cars heal` command. |
| `allow-console`| Boolean | If `true`, the command can be run from the server console. |

## `keys`

Manages the car key system.

> **Developer Note:** Record a short video demonstrating the key system. Show a player trying to start a car without a key (and failing), then crafting/receiving a key, and successfully starting the car. Also show the `allow-owner-start-without-key` feature.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | Master switch for the entire key system. |
| `require-key-to-start-engine`| Boolean | If `true`, the player needs a key to start the car's engine. |
| `require-key-to-stop-engine` | Boolean | If `true`, a key is also required to stop the engine. |
| `allow-owner-start-without-key`| Boolean | If `true`, the owner of the car can start it without needing a key in their inventory. |
| `allow-non-owner-use-key` | Boolean | If `true`, a player who is not the owner can use a key to start the car. |
| `check-entire-inventory` | Boolean | If `true`, the plugin checks the player's full inventory for a key. If `false`, it only checks their main hand and off-hand. |

### `keys.item`

Defines the key item itself.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `material` | String | The Minecraft material for the key item. |
| `name` | String | The display name of the key item. Supports placeholders. |
| `lore` | List | The lore (description) of the key item. Supports placeholders. |
| `localized` | Map | Allows for different names and lores based on player language. |
| `custom-model-data` | Integer| The custom model data for the item. Use `null` to disable. |
| `unbreakable`| Boolean | If `true`, the key item will be unbreakable. |
