# Installation

Installing the CoolCars plugin is a simple process.

## Requirements

*   A Minecraft server running **Spigot**, **Paper**, or a compatible fork.
*   The server version must be **1.21** or higher, as specified in the plugin's configuration.
*   (Optional) **PlaceholderAPI** for using CoolCars placeholders in other plugins.

## Steps

1.  **Download the Plugin:**
    Download the latest version of `CoolCars-X.X.X.jar` from the official source where you purchased the plugin.

2.  **Place the JAR File:**
    Stop your server. Place the downloaded `.jar` file into your server's `plugins` directory.

3.  **Start the Server:**
    Start your server. The first time it runs, the CoolCars plugin will generate its default configuration files and folders. You will see a new `CoolCars` folder inside your `plugins` directory.

4.  **Configuration:**
    Stop the server again. Navigate to `plugins/CoolCars/` and review the configuration files, especially `config.yml`. You can adjust settings to your liking. The plugin comes with a pre-configured `Volga` car model, so it's ready to use out of the box.

5.  **Restart and Verify:**
    Start your server one more time. To verify that the plugin is working correctly, you can try spawning the default car:
    ```
    /car spawn volga
    ```
    If a car appears, the installation was successful!

---
> **Developer Note:** A helpful mini-video for this page would show the process of dragging the JAR file into the `plugins` folder, starting the server, and then running the `/car spawn volga` command in-game to show the immediate result. This visually confirms a successful installation.
