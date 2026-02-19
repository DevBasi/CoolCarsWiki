# Placeholders

CoolCars integrates with the **PlaceholderAPI** plugin, allowing you to display real-time car information in other plugins, such as scoreboards, chat formats, or holographic displays.

The plugin also uses its own placeholder system internally for things like the action bar UI and item names.

## Internal Placeholders

These placeholders are used within the CoolCars configuration files, such as `config.yml` and `items.yml`.

### Action Bar: Car Display (`ui.action-bar.car.format`)
*   `{model}`: The car's model name.
*   `{model_key}`: The car's model key (folder name).
*   `{speed_label}`: Localized "Speed" text.
*   `{speed_kmh}`: The car's speed in kilometers per hour.
*   `{speed_mps}`: The car's speed in meters per second.
*   `{passengers_label}`: Localized "Passengers" text.
*   `{passengers}/{passengers_max}`: Current vs. max passengers.
*   `{occupied_seats}/{total_seats}`: Alternative for passenger count.
*   `{hp_label}`: Localized "HP" text.
*   `{hp}/{hp_max}`: Current vs. max health.
*   `{hp_percent}`: Health as a percentage.
*   `{hp_bar}`: A visual bar representing health.
*   `{fuel_label}`: Localized "Fuel" text.
*   `{fuel}/{fuel_max}`: Current vs. max fuel.
*   `{fuel_percent}`: Fuel as a percentage.
*   `{fuel_bar}`: A visual bar representing fuel level.
*   `{engine}`: Engine status (On/Off).
*   `{lights}`: Headlight status (On/Off).
*   `{damage_block}`: A symbol indicating damage.
*   `{seats_color}`, `{hp_color}`, etc.: Dynamic colors for different stats.

### Action Bar: Refuel & Repair
*   `{refuel_label}` / `{repair_label}`: Localized text for the action.
*   `{progress_bar}`: A visual bar showing the progress of the action.
*   `{progress_percent}`: The progress as a percentage.
*   `{tank}/{tank_max}`: Fuel tank level.
*   `{canister}/{canister_max}`: Fuel level in the canister.
*   `{part}`: The name of the part being repaired.
*   `{part_bar}` / `{part_percent}`: The health of the part being repaired.
*   `{kit}/{kit_max}`: The remaining units in the repair kit.
*   `{hold_shift}`: A message indicating to hold shift for an action.

### Car Key Item (`config.yml`)
*   `{model}`: The car's model name.
*   `{model_key}`: The car's model key.
*   `{car_uuid}`: The unique ID of the car.
*   `{owner_name}`: The name of the car's owner.
*   `{owner_uuid}`: The UUID of the car's owner.

### Fuel Canister & Repair Kit (`items.yml`)
*   `{liters}` / `{capacity}`: The current and max fuel in a canister.
*   `{units}` / `{capacity}`: The current and max units in a repair kit.

## PlaceholderAPI Placeholders

To use these, you need to have PlaceholderAPI installed. The format for CoolCars placeholders will likely follow the pattern `%coolcars_<car_uuid>_<value>%` or similar. The exact list of external placeholders would need to be confirmed from the plugin developer or by testing, but they would expose the same data as the internal placeholders.

**Example Usage (Conceptual):**

*   `%coolcars_player_speed_kmh%`: Shows the speed of the car the player is currently in.
*   `%coolcars_player_fuel_percent%`: Shows the fuel percentage of the player's current car.

You would need to consult the plugin's page or developer for a definitive list of placeholders available for PlaceholderAPI.
