# Creating a Car

One of the most powerful features of the CoolCars plugin is the ability to create your own unique cars from scratch. The entire definition of a car is contained within its own folder, making it easy to manage, share, and create new vehicles.

## The Car Folder

To create a new car, you simply create a new folder inside the `plugins/CoolCars/cars/` directory. For example, to create a car called "MyCar", you would create the folder `plugins/CoolCars/cars/MyCar/`.

The name of this folder becomes the car's unique **model key**. You will use this key in commands like `/car spawn MyCar`.

## Car Definition Files

Inside your car's folder, you will create four YAML files that define every aspect of your vehicle:

*   **`components.yml`**: Defines the visual and physical parts of the car. This includes the 3D models (via `CustomModelData`), seat positions, hitbox size, and UI layout.
*   **`performance.yml`**: Controls the car's physics and handling. Here you set the engine power, brake force, steering angle, suspension, and fuel consumption.
*   **`behavior.yml`**: Governs the car's interactive features and game mechanics. This includes health, damage, collision behavior, and special abilities like the horn.
*   **`localization.yml`**: Contains all the text related to the car, such as its name in the menu, trunk title, etc. This allows for easy translation.

## The Process

1.  **Create your folder:** e.g., `cars/MyCar`.
2.  **Create the four YAML files** inside it.
3.  **Configure each file:** Use the following pages in this documentation as a reference for every available parameter.
4.  **Reload the plugin:** Use `/car reload` to load your new car into the game.
5.  **Spawn and test:** Use `/car spawn MyCar` to see your creation!

> **Developer Note:** This is the perfect place for a comprehensive "Let's Make a Car" video tutorial. The video should walk through the entire process: creating the folder, creating the four blank YAML files, and then going through each file, adding a few key parameters at a time, and showing the result in-game after each `/car reload`. For example, start with just the car body model, then add wheels, then configure the seats, then tweak the speed, etc. This would be the most valuable video in the entire documentation.
