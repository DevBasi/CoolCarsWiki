# Performance (performance.yml)

The `performance.yml` file is the heart of your car's physics and handling. By tweaking these values, you can create anything from a slow, heavy truck to a nimble sports car.

---
> **Developer Note:** A video for this page is essential. Show two cars with drastically different `performance.yml` settings side-by-side. For example, a "sports car" with high `engine-force` and a "truck" with low `engine-force` but higher `mass`. Race them to show the difference in acceleration and top speed. Then, show them taking a corner to demonstrate how `max-steer-deg` and `lateral-grip-ground` affect handling.

## `car.suspension`

Controls how the car's wheels interact with the ground.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `rest-length` | Double | The natural length of the suspension spring. |
| `stiffness` | Double | How stiff the suspension spring is. Higher values mean less bounce. |
| `damping` | Double | How quickly the suspension stops oscillating. |
| `wheel-radius` | Double | The radius of the wheel used in the physics calculations. |
| `step-height` | Double | The maximum height of a block the car can drive up without jumping. |

## `car.drivetrain`

Manages engine power, acceleration, and speed.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `engine-force` | Double | The fundamental power of the engine. This is the main value that determines acceleration. |
| `acceleration-forward` | Double | A multiplier for the forward acceleration force. |
| `acceleration-reverse-multiplier` | Double | A multiplier for reverse acceleration. `0.5` means reversing is half as powerful as driving forward. |
| `acceleration-curve-exponent`| Double | The exponent for the throttle curve. `1.0` is linear. `>1.0` means more acceleration at higher throttle. |
| `brake-force`| Double | The force applied when the brakes are engaged. |
| `brake-multiplier` | Double | A global multiplier for brake force. |
| `max-forward-speed` | Double | The maximum forward speed in meters/second. |
| `max-reverse-speed` | Double | The maximum reverse speed in meters/second. |

## `car.steering`

Controls how the car handles turns and body roll.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `max-steer-deg` | Double | The maximum angle in degrees that the front wheels can turn. |
| `wheel-base`| Double | The distance between the front and rear axles. |
| `steer-response`| Double | How quickly the wheels turn to the desired angle. |
| `body-roll-limit-deg` | Double | The maximum angle the car's body will roll during turns. |
| `body-roll-response` | Double | How quickly the body rolls. |
| `body-roll-strength` | Double | The intensity of the body roll effect. |

## `car.physics`

Defines the core physical properties of the car.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `mass` | Double | The total mass of the vehicle in kilograms. Affects acceleration, inertia, and suspension. |
| `drag-coeff` | Double | The aerodynamic drag coefficient. Higher values mean more air resistance at high speeds. |
| `rolling-resistance` | Double | The force of friction from the wheels on the ground. |
| `vertical-damping` | Double | Damping force applied to vertical movement, helps stabilize the car in the air. |
| `lateral-grip-ground` | Double | How much grip the tires have on the ground during turns. Higher values prevent sliding. |
| `lateral-grip-air` | Double | How much control the player has over the car's orientation while airborne. |
| `max-tick-horizontal-delta-v`| Double | The maximum change in horizontal velocity per tick, used to prevent physics instability. |

## `car.fuel`

Configures the car's fuel tank and consumption.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `tank-capacity` | Double | The maximum amount of fuel the tank can hold in liters. |
| `initial-fuel`| Double | The amount of fuel the car has when it's first spawned. |
| `consumption.base-per-tick` | Double | The amount of fuel consumed every tick, even when idling. |
| `consumption.speed-factor` | Double | Extra fuel consumed based on the car's speed. |
| `consumption.throttle-factor`| Double | Extra fuel consumed based on how much throttle is being applied. |
