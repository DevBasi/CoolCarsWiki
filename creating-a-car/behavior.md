# Behavior (behavior.yml)

The `behavior.yml` file configures how your car interacts with the world and with players. It controls things like the horn, collision physics, health, damage, and combat mechanics.

---
> **Developer Note:** A good video for this page would be a "crash test" demonstration. Show a car colliding with a wall and other entities. Demonstrate the part-based damage system by showing how a frontal impact damages the `front` part. Then, show the car's performance degrading as it takes more damage (`slowdown-start-percent`) and the smoke effects appearing (`smoke-threshold-percent`).

## `car.horn`

Configures the car's horn.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | If `true`, the car has a functional horn. |
| `require-moving`| Boolean | If `true`, the horn can only be used while the car is moving. |
| `cooldown-ticks`| Integer | The delay in ticks before the horn can be used again. |
| `sound` | String | The sound key to play for the horn. |
| `volume` | Double | The volume of the horn sound. |
| `pitch` | Double | The pitch of the horn sound. |

## `car.collision`

Defines the physical collision box and its behavior.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `half-width` / `half-length` | Double | The half-size of the car's rectangular collision box (from the center out). |
| `height` | Double | The total height of the collision box. |
| `base-y` | Double | The vertical offset of the collision box's base. |
| `slide-factor`| Double | How much the car slides along walls on impact. |
| `wall-damping`| Double | How much speed is lost when hitting a wall. |
| `unstuck-max-lift` | Double | The maximum vertical distance the car can lift itself to try and get unstuck. |

## `car.health`

Manages the car's overall health points (HP) and regeneration.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `max` | Double | The maximum HP of the car. |
| `engine-disabled-health-percent`| Double | The HP percentage (0.0 to 1.0) below which the engine will not start. |
| `destroy-on-zero`| Boolean | If `true`, the car is destroyed when its HP reaches 0. |
| `idle-damage-per-tick` | Double | Passive damage the car takes every tick. |
| `impact.min-delta-speed` | Double | The minimum speed change in a collision required to cause damage. |
| `impact.damage-scale` | Double | A multiplier for calculating damage from impacts. |
| `impact.wall-multiplier`| Double | An extra damage multiplier for impacts with walls. |
| `regen.enabled`| Boolean | If `true`, the car will slowly regenerate health. |
| `regen.per-tick`| Double | The amount of HP regenerated per tick. |
| `regen.delay-ticks`| Integer | The delay after taking damage before regeneration begins. |

## `car.damage`

Controls the modular, part-based damage system.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `enabled` | Boolean | Toggles the entire part-based damage system. |
| `parts.*.enabled`| Boolean | Toggles damage for specific parts (`front`, `rear`, `wheels`). |
| `parts.*.max-health` | Double | The maximum health for each individual part. |
| `impact.*-multiplier` | Double | Damage multipliers for impacts affecting different parts. |
| `performance.slowdown-start-percent` | Double | The overall HP percentage at which the car starts to lose performance. |
| `performance.min-factor-at-zero`| Double | The minimum performance factor (e.g., `0.35` = 35% power) when the car is at 0 HP. |
| `visual.smoke-threshold-percent`| Double | The HP percentage below which the car will start to emit smoke particles. |
| `hitbox.*-offset` | Map | The `x, y, z` offset for the damage hitboxes for the front and rear parts. |

## `car.combat`

Defines how the car deals damage to other entities.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `ram.enabled` | Boolean | If `true`, the car can damage entities by ramming them. |
| `ram.min-speed` | Double | The minimum speed required to deal ram damage. |
| `ram.base-damage`| Double | The base damage dealt on a successful ram. |
| `ram.speed-damage-factor` | Double | Additional damage that scales with the car's speed. |
| `ram.max-damage` | Double | The maximum possible damage from a single ram. |
| `ram.knockback-horizontal` | Double | The horizontal knockback applied to the rammed entity. |
| `ram.knockback-vertical`| Double | The vertical knockback applied. |
| `ram.hit-cooldown-ticks` | Integer | The cooldown in ticks before the same entity can be damaged again. |
| `ram.affect-players` | Boolean | If `true`, ramming will damage other players. |
| `ram.affect-mobs`| Boolean | If `true`, ramming will damage mobs. |
| `ram.self-speed-loss-factor` | Double | How much speed the car loses after ramming something. |

## `car.fuel`

Additional fuel-related behaviors.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `no-fuel-brake-factor` | Double | A braking force that is applied when the car runs out of fuel, causing it to slow to a stop. |
| `refuel-rate-liters-per-tick`| Double | The rate at which the car refuels when using a canister. |
| `refuel-sound-interval-ticks`| Integer | How often the `filling` sound is played during refueling. |
