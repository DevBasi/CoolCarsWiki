# Items (items.yml)

The `items.yml` file defines the properties of special items used by the CoolCars plugin, such as fuel canisters and repair kits.

## `fuel.canister`

This section configures the item used to refuel cars.

> **Developer Note:** A video for this section could show a player receiving a fuel canister with `/fuel give`, inspecting its lore, and then using it at a fuel point on a car. The action bar changing during refueling would be important to show.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `material` | String | The Minecraft material for the fuel canister item. |
| `capacity-liters` | Double | The maximum amount of fuel the canister can hold. |
| `custom-model-data` | Integer | The custom model data for the item's texture. |
| `display-name`| String | The name of the item. Supports `{liters}` and `{capacity}` placeholders. |
| `lore` | List | The item's description. Supports the same placeholders. |
| `localization`| Map | Allows providing different names and lores for different languages. |
| `unbreakable` | Boolean | If `true`, the canister cannot be broken or take durability damage. |
| `nbt-tags` | Map | A map of custom NBT tags to apply to the item. These are used internally by the plugin to identify the item. |

### Example Canister

```yaml
fuel:
  canister:
    material: HONEY_BOTTLE
    capacity-liters: 10.0
    custom-model-data: 1
    display-name: "&6Fuel Canister &7({liters}/{capacity}L)"
    lore:
      - "&7Fuel: &f{liters}&7/&f{capacity}&7 L"
      - "&8SHIFT + RMB on fuel point"
    unbreakable: false
    nbt-tags:
      "cars:canister_type": standard
      "cars:refuel_item": true
```

## `repair.kit`

This section configures the item used to repair damaged car parts.

> **Developer Note:** Show a player damaging a car part (e.g., by crashing), then using a repair kit on the damaged area. The action bar display for repairs and the visual change (like smoke disappearing) would be good to capture.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `rate-units-per-tick` | Double | How many "units" of repair are applied per tick when using the kit. |
| `kit.material`| String | The Minecraft material for the repair kit item. |
| `kit.max-units` | Double | The total repair capacity of a single kit. |
| `kit.custom-model-data` | Integer | The custom model data for the item's texture. |
| `kit.display-name`| String | The name of the item. Supports `{units}` and `{capacity}` placeholders. |
| `kit.lore` | List | The item's description. Supports the same placeholders. |
| `kit.localization`| Map | Allows providing different names and lores for different languages. |
| `kit.unbreakable` | Boolean | If `true`, the kit cannot be broken or take durability damage. |
| `kit.nbt-tags`| Map | Custom NBT tags to identify the item as a repair kit. |

### Example Repair Kit

```yaml
repair:
  rate-units-per-tick: 0.08
  kit:
    material: RABBIT_HIDE
    max-units: 10.0
    custom-model-data: 1
    display-name: "&bRepair Kit &7({units}/{capacity})"
    lore:
      - "&7Kit units: &f{units}&7/&f{capacity}"
      - "&8SHIFT + RMB on damaged part"
    unbreakable: false
    nbt-tags:
      "cars:repair_item": true
```
