# Localization (localization.yml)

The `localization.yml` file handles all the text specific to your car model. This allows you to define different names and messages for different languages, making your server more accessible to a wider audience.

## `car.menu`

This section contains the text displayed in the car's interactive menu.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `localization.en.title` | String | The title of the menu in English. |
| `localization.en.engine-on` | String | The text for the engine button when the engine is on (English). |
| `localization.en.engine-off`| String | The text for the engine button when the engine is off (English). |
| `localization.en.lights-on`| String | The text for the lights button when they are on (English). |
| `localization.en.lights-off`| String | The text for the lights button when they are off (English). |
| `localization.en.trunk` | String | The text for the trunk button (English). |
| `localization.en.toggle-hint`| String | The hint text shown on toggleable buttons (English). |
| `localization.en.trunk-hint`| String | The hint text shown on the trunk button (English). |

You can add other languages by creating a new block, for example `localization.ru.*` for Russian, as seen in the default Volga.

### Example

```yaml
car:
  menu:
    localization:
      en:
        title: "&8Car Menu"
        engine-on: "&aEngine: ON"
        engine-off: "&cEngine: OFF"
        # ... and so on
      ru:
        title: "&8Меню машины"
        engine-on: "&aДвигатель: ВКЛ"
        engine-off: "&cДвигатель: ВЫКЛ"
        # ... and so on
```

## `car.trunk`

This section controls the title of the car's trunk inventory.

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `use-lang-title` | Boolean | If `true`, the plugin will use a key from the global language file (`lang/en.yml`, etc.) for the title. |
| `title-lang-key`| String | The key from the global language file to use (e.g., `trunk.title`). |
| `title` | String | A fallback title to use if the language key is not found. |
| `localization.en.title` | String | A specific title for this car model in English, overriding the global language file. |
| `localization.ru.title` | String | The same, but for Russian. |

By using a combination of global language files and this car-specific `localization.yml`, you can manage all plugin text effectively.
