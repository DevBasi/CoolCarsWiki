<div align="center">
  <img src="https://mintcdn.com/coolcars/Nf4BiqjFcs6SEFCk/images/coolcars/logo-full.png?w=1100&fit=max&auto=format&n=Nf4BiqjFcs6SEFCk&q=85&s=519a7b4c36beba6e8c42eca5b5685fd6" width="800" alt="CoolCars Logo">
  <h1>CoolCars — Система транспорта для Minecraft</h1>
  
  <p align="center">
    <b>Продвинутая физика, модульная кастомизация и высокая производительность.</b>
  </p>

  [![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21+-62B06F?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
  [![Java Version](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
  [![Wiki](https://img.shields.io/badge/Wiki-Documentation-95A5A6?style=for-the-badge&logo=gitbook&logoColor=white)](https://coolcars.mintlify.app/ru/README)

  <br />

  [🇷🇺 **Русский**] | [🇺🇸 **English**](README_EN.md)
</div>

---

## ⚙️ Системные требования

* **Версия ядра:** Minecraft **1.21** и выше (Paper, Purpur).
* **Среда выполнения:** **Java 21** или выше.
* **Зависимости:** [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) (Обязательно).

---

## 📥 Установка

1. Установите последнюю версию **ProtocolLib**.
2. Поместите файл `CoolCars.jar` в папку `/plugins/`.
3. Перезапустите сервер для генерации файлов конфигурации.
4. Убедитесь, что ваш ресурспак активен для отображения 3D-моделей.

---

## 🚀 Основной функционал

### 🏎 Физика и движение
* **Симуляция инерции:** Расчет разгона, торможения и наката.
* **Обработка поверхностей:** Плавное передвижение по полублокам (Slabs), ступеням и склонам.
* **Столкновения:** Система детекции коллизий с блоками и сущностями.
* **Подвеска:** Визуальная анимация наклона кузова при маневрах.

### 🎨 Кастомизация (YAML)
* **Поддержка 3D-моделей:** Привязка `CustomModelData` из вашего ресурспака.
* **Настройка характеристик:** Индивидуальные параметры макс. скорости, ускорения и объема бака для каждой модели.
* **Звуковые эффекты:** Поддержка кастомных звуков для двигателя, езды и сигналов.

### 🎮 Игровые механики
* **Система топлива:** Настраиваемый расход и возможность заправки.
* **Управление доступом:** Система физических ключей (Key Items) для защиты от угона.
* **Хранилище:** Встроенный багажник (инвентарь) в каждом автомобиле.
* **Динамический HUD:** Спидометр и уровень топлива в Action Bar в реальном времени.

---

## ⌨️ Командный интерфейс

| Команда | Описание | Право (Permission) |
| :--- | :--- | :--- |
| `/car spawn <model>` | Призвать автомобиль выбранной модели | `coolcars.admin.spawn` |
| `/car givekey <player> <model>` | Выдать ключ от автомобиля игроку | `coolcars.admin.givekey` |
| `/car list` | Показать список доступных моделей | `coolcars.player.list` |
| `/car reload` | Перезагрузить конфигурационные файлы | `coolcars.admin.reload` |

---

<div align="center">
  <sub>Разработано <b>PenguinTeam & DevBasi</b> для современных Minecraft серверов.</sub>
</div>
