# Sounds (sounds.yml)

The `sounds.yml` file gives you full control over all audible effects for cars. You can customize the sound, volume, pitch, and the conditions under which each sound is played.

> **Developer Note:** A video could showcase the auditory experience. Drive a car, start and stop the engine, use the horn, and have a minor crash to demonstrate the different sound events in action. This helps users understand what each event corresponds to in-game.

## Global Sound Settings

These parameters at the top of the file control the overall sound environment.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | Master switch for all car sounds. |
| `category` | String | The sound category to play sounds under (e.g., `PLAYERS`, `RECORDS`). This affects which volume slider in the player's client controls these sounds. |
| `transition.*` | Integer | Delay in ticks when transitioning between engine idle and driving sounds. |

### `spatial`

These settings control how sounds fade with distance (attenuation).

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `min-volume-factor` | Double | The lowest volume a sound can be at its maximum distance (0.0 to 0.95). |
| `near-distance-blocks`| Double | The radius around the car where sounds are at full volume before they start to fade. |
| `falloff-curve-power` | Double | The power of the fade curve. `>1` means a faster fade with distance. |

## `events`

This is a list of all the different sound events you can configure. Each event has a common set of parameters.

### Common Event Parameters

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | Toggles this specific sound event. |
| `key` | String | The sound key to play. This can be a vanilla sound (e.g., `minecraft:entity.horse.ambient`) or a custom sound from a resource pack (e.g., `coolcars.start`). |
| `volume` | Double | The loudness of the sound (1.0 is default). |
| `pitch` | Double | The pitch of the sound (1.0 is default). |
| `radius` | Double | The maximum distance in blocks the sound can be heard from. |
| `interval-ticks` | Integer | For continuous sounds (like `driving`), how often the sound should be played. |
| `cooldown-ticks`| Integer | How long (in ticks) to wait before this sound can be triggered again. Prevents spam. |
| `require-moving` | Boolean | If `true`, the car must be moving for this sound to play. |
| `prevent-overlap` | Boolean | If `true`, the plugin tries to prevent this sound from playing on top of itself. |
| `min-speed` | Double | The minimum speed (in m/s) the car must be traveling at for the sound to play. |

### List of Events

Here are the default sound events you can configure:

*   `seat-open`: Played when a player enters a car seat.
*   `seat-close`: Played when a player leaves a car seat.
*   `engine-start`: The sound of the engine turning on.
*   `engine-stop`: The sound of the engine turning off.
*   `engine-idle`: The continuous sound of the engine idling.
*   `horn`: The car's horn.
*   `driving`: The continuous sound of the car driving.
*   `glovebox`: Sound for opening the car's menu/glovebox.
*   `filling`: Sound played while refueling.
*   `repair`: Sound played while repairing.
*   `hit`: A minor impact sound.
*   `crash`: A major impact or crash sound.
*   `landing`: Sound played when the car lands after a fall.
