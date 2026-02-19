# Components (components.yml)

The `components.yml` file defines all the visual and physical components of your car. It controls how the car looks, where players sit, and how they interact with it.

---
> **Developer Note:** A great video for this page would be a showcase of the visual options. Start with a basic car body, then show how to add wheels and a steering wheel. Demonstrate changing the `CustomModelData` for the body and seeing the car's appearance change in-game. Also, show how adjusting the `body-offset` and `wheel-offsets` moves the parts around.

## `car.meta`

Basic information about the car model.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `display-name`| String | The name shown in commands like `/car list`. |

## `car.models`

This section defines the items used to build the car's visual model. The car is assembled from multiple `ItemDisplay` entities.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `body-material` | String | The Minecraft material for the main body of the car. |
| `body-custom-model-data` | Integer | The `CustomModelData` value for the body's texture. |
| `body-material-headlights` | String | (Optional) A different material to use when the headlights are on. |
| `body-material-headlights-custom-model-data` | Integer | (Optional) A different `CustomModelData` to use when headlights are on. |
| `wheel-material` | String | The default material for the wheels. |
| `wheel-custom-model-data` | Integer | The default `CustomModelData` for the wheels. |
| `steering-wheel-material` | String | The material for the steering wheel. |
| `steering-wheel-custom-model-data` | Integer | The `CustomModelData` for the steering wheel. |

### `wheels`

You can define different models for each of the four wheels individually.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `default` | String | Default wheel material (if specific wheels aren't set). |
| `default-custom-model-data` | Integer | Default wheel `CustomModelData`. |
| `front-left`, `front-right`, etc. | String | Material for a specific wheel. |
| `front-left-custom-model-data`, etc. | Integer | `CustomModelData` for a specific wheel. |

## `car.menu`

Configures the in-car UI menu that opens when a player is in a seat.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `trunk-button-enabled` | Boolean | If `true`, a button to open the trunk is shown in the menu. |
| `slots.*` | Integer | The inventory slot index (0-26) for the engine, trunk, and lights buttons. |
| `style.filler.material` | String | The material used for empty slots in the menu. |
| `style.filler.name` | String | The name for the filler item. |
| `style.items.*` | Map | Defines the icons for the buttons (engine on/off, lights on/off, trunk). |

## `car.interaction`

Defines the invisible hitbox used for right-clicking to enter the car.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `hitbox-width` | Double | The width (X-axis) of the interaction box. |
| `hitbox-height` | Double | The height (Y-axis) of the interaction box. |

## `car.seats`

Defines the position of each seat in the car.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `offsets` | List | A list of seat positions, defined by `x`, `y`, and `z` coordinates relative to the car's center. The first entry is the driver's seat. |

## `car.visual`

Controls the visual offsets and smoothing for the display entities.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `interpolation-duration` | Integer | The duration (in ticks) for smoothing movement. |
| `teleport-duration`| Integer | The duration for smoothing movement after a teleport. |
| `body-offset` | Map | The `x, y, z` offset of the main body model from the center. |
| `steering-wheel-offset` | Map | The `x, y, z` offset of the steering wheel model. |
| `wheel-offsets` | List | A list of `x, y, z` offsets for each of the four wheels. |

## `car.lights`

Configures the car's headlights.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `headlight-offsets` | List | A list of `x, y, z` offsets where the light sources will be created. |
| `range` | Double | The distance in blocks that the light will travel. |
| `level` | Integer | The brightness level of the light (1-15). |
| `update-ticks`| Integer | How often the light source's position is updated. |
| `visible-effects` | Boolean | If `true`, displays a visible particle effect for the headlights. |

## `car.exhaust`

Configures the exhaust smoke effect.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | Toggles the exhaust particle effect. |
| `offsets` | List | A list of `x, y, z` offsets where exhaust particles will be emitted. |
| `update-ticks`| Integer | How often particles are emitted. |
| `min-throttle` | Double | The minimum throttle required for exhaust to appear. |
| `speed-factor` | Double | A multiplier that increases particle count with speed. |
| `base-count` / `max-count` | Integer | The base and maximum number of particles to spawn. |

## `car.trunk`

Defines the car's storage.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `slots` | Integer | The size of the trunk inventory. Must be a multiple of 9. |
| `point-offset` | Map | The `x, y, z` offset for the interaction point to open the trunk from outside the car. |

## `car.fuel`

Defines the fuel refill point.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `point-offset` | Map | The `x, y, z` offset for the interaction point to refuel the car. |
