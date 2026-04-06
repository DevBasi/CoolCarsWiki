package dev.basi.cars;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.StringUtil;
import org.bukkit.util.Vector;

/**
 * @author basi
 */
public final class MainCars extends JavaPlugin implements Listener {

    private final Map<UUID, CarEntity> cars = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> driverToCar = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToCarCache = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> refuelTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> repairTasks = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> refuelCarByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> repairCarByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> repairHitboxPreviewTasks =
        new ConcurrentHashMap<>();
    private final Map<UUID, Long> hornCooldownUntilMs =
        new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> jumpHeldByPlayer =
        new ConcurrentHashMap<>();
    private final Map<UUID, UUID> carOwners = new ConcurrentHashMap<>();
    private final Map<UUID, String> carOwnerNames = new ConcurrentHashMap<>();
    private final Map<UUID, String> carModelById = new ConcurrentHashMap<>();
    private final Map<UUID, Long> carSpawnedAtMs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> carLastUsedAtMs = new ConcurrentHashMap<>();
    private final Map<UUID, String> carLastEventTypeById =
        new ConcurrentHashMap<>();
    private final Map<UUID, String> carLastEventDetailsById =
        new ConcurrentHashMap<>();
    private final Map<UUID, Long> carLastEventAtMs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> carLastEventFlushedAtMs =
        new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> carChunkTickets =
        new ConcurrentHashMap<>();
    private final Map<UUID, String> carChunkCenterTokenCache =
        new ConcurrentHashMap<>();
    private final Map<UUID, UUID> partEntityToCar = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> fuelEntityToCar = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> trunkEntityToCar = new ConcurrentHashMap<>();
    private final Map<String, CarSettings> modelSettings =
        new ConcurrentHashMap<>();
    private final Map<String, YamlConfiguration> modelConfigs =
        new ConcurrentHashMap<>();
    private final Map<String, String> modelDisplayNames =
        new ConcurrentHashMap<>();

    private BukkitTask physicsTask;
    private CarSettings carSettings;
    private boolean carActionBarEnabled;
    private int carActionBarUpdateTicks;
    private String carActionBarFormat;
    private String carActionBarFormatLangKey;
    private boolean refuelActionBarEnabled;
    private int refuelActionBarUpdateTicks;
    private String refuelActionBarFormat;
    private String refuelActionBarFormatLangKey;
    private boolean repairActionBarEnabled;
    private int repairActionBarUpdateTicks;
    private String repairActionBarFormat;
    private String repairActionBarFormatLangKey;
    private boolean soundsEnabled;
    private SoundCategory soundsCategory;
    private SoundProfile soundSeatOpen;
    private SoundProfile soundSeatClose;
    private SoundProfile soundEngineStart;
    private SoundProfile soundEngineStop;
    private SoundProfile soundEngineIdle;
    private SoundProfile soundHorn;
    private SoundProfile soundDriving;
    private SoundProfile soundGlovebox;
    private SoundProfile soundFilling;
    private SoundProfile soundRepair;
    private SoundProfile soundHit;
    private SoundProfile soundCrash;
    private SoundProfile soundLanding;
    private final Map<UUID, Integer> drivingSoundNextTick =
        new ConcurrentHashMap<>();
    private final Map<UUID, Integer> idleSoundNextTick =
        new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingIdleStartTick =
        new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingDriveStartTick =
        new ConcurrentHashMap<>();
    private int drivingToIdleDelayTicks;
    private int idleToDrivingDelayTicks;
    private double soundSpatialMinVolumeFactor;
    private double soundSpatialNearDistanceBlocks;
    private double soundSpatialFalloffCurvePower;
    private final Map<String, Long> soundCooldownUntilMs =
        new ConcurrentHashMap<>();
    private boolean projectileDamageEnabled;
    private boolean projectileStickArrows;
    private boolean projectileNotifyShooter;
    private boolean projectileNotifyDriver;
    private boolean projectileNotifyFriendlyBlocked;
    private boolean projectileIgnoreOccupants;
    private double projectileBaseDamage;
    private double projectileArrowDamageMultiplier;
    private double projectileTridentDamageMultiplier;
    private double projectileOtherDamageMultiplier;
    private double projectileSpeedDamageFactor;
    private boolean meleeDamageToCarEnabled;
    private double meleeDamageMultiplier;
    private boolean meleePlayHitSound;
    private boolean projectilePlayHitSound;
    private int menuSlotEngine;
    private int menuSlotLights;
    private int menuSlotTrunk;
    private int menuRows;
    private int menuSize;
    private boolean menuTrunkEnabled;
    private boolean menuFillEmptySlots;
    private Material menuFillerMaterial;
    private String menuFillerName;
    private int menuFillerCustomModelData;
    private Material menuEngineMaterialOn;
    private Material menuEngineMaterialOff;
    private int menuEngineCustomModelDataOn;
    private int menuEngineCustomModelDataOff;
    private Material menuLightsMaterialOn;
    private Material menuLightsMaterialOff;
    private int menuLightsCustomModelDataOn;
    private int menuLightsCustomModelDataOff;
    private Material menuTrunkMaterial;
    private int menuTrunkCustomModelData;
    private final Map<String, Map<String, String>> menuLocalizedTexts =
        new HashMap<>();
    private final Map<String, Map<String, List<String>>> menuLocalizedLores =
        new HashMap<>();

    private NamespacedKey canisterLitersKey;
    private NamespacedKey repairKitUnitsKey;
    private NamespacedKey canisterItemIdKey;
    private NamespacedKey repairKitItemIdKey;
    private NamespacedKey carKeyVehicleIdKey;
    private NamespacedKey carKeyOwnerIdKey;
    private NamespacedKey carKeyUniqueIdKey;
    private NamespacedKey carKeyItemMarkerKey;
    private double canisterMaxLiters;
    private Material canisterMaterial;
    private String canisterDisplayName;
    private List<String> canisterLoreLines;
    private final Map<String, String> canisterDisplayNamesByLang =
        new HashMap<>();
    private final Map<String, List<String>> canisterLoreByLang =
        new HashMap<>();
    private Integer canisterCustomModelData;
    private boolean canisterUnbreakable;
    private Map<NamespacedKey, String> canisterParsedTags;
    private Material repairKitMaterial;
    private String repairKitDisplayName;
    private List<String> repairKitLoreLines;
    private final Map<String, String> repairKitDisplayNamesByLang =
        new HashMap<>();
    private final Map<String, List<String>> repairKitLoreByLang =
        new HashMap<>();
    private Integer repairKitCustomModelData;
    private boolean repairKitUnbreakable;
    private Map<NamespacedKey, String> repairKitParsedTags;
    private boolean keysEnabled;
    private boolean keysRequireToStartEngine;
    private boolean keysRequireToStopEngine;
    private boolean keysAllowOwnerStartWithoutKey;
    private boolean keysAllowNonOwnerUseKey;
    private boolean keysCheckEntireInventory;
    private Material keyItemMaterial;
    private String keyItemDisplayName;
    private List<String> keyItemLoreLines;
    private final Map<String, String> keyDisplayNamesByLang = new HashMap<>();
    private final Map<String, List<String>> keyLoreByLang = new HashMap<>();
    private Integer keyItemCustomModelData;
    private boolean keyItemUnbreakable;
    private boolean healCommandEnabled;
    private boolean healCommandConsoleAllowed;
    private double repairKitMaxUnits;
    private double repairRateUnitsPerTick;
    private File carsFile;
    private I18n i18n;
    private ConfigDocManager configDocManager;
    private CoolCarsPlaceholderExpansion placeholderExpansion;
    private CarOwnershipDatabase ownershipDatabase;
    private final List<StoredCarRecord> pendingCarRestores = new ArrayList<>();
    private BukkitTask pendingRestoreTask;
    private BukkitTask autoSaveTask;
    private YamlConfiguration runtimeConfig;
    private String defaultModel = "volga";
    private boolean autoSaveEnabled;
    private int autoSaveIntervalTicks;

    @Override
    public void onEnable() {
        ensureSplitConfigStructure();
        this.i18n = new I18n(this);
        this.configDocManager = new ConfigDocManager(this);
        this.ownershipDatabase = new CarOwnershipDatabase(this);
        this.carsFile = new File(getDataFolder(), "cars.yml");
        reloadPluginConfig();
        getLogger().info(
            "Using config file: " +
                new File(getDataFolder(), "config.yml").getAbsolutePath()
        );

        this.canisterLitersKey = new NamespacedKey(this, "canister_liters");
        this.repairKitUnitsKey = new NamespacedKey(this, "repair_kit_units");
        this.canisterItemIdKey = new NamespacedKey(this, "canister_item_id");
        this.repairKitItemIdKey = new NamespacedKey(this, "repair_kit_item_id");
        this.carKeyVehicleIdKey = new NamespacedKey(this, "car_key_vehicle");
        this.carKeyOwnerIdKey = new NamespacedKey(this, "car_key_owner");
        this.carKeyUniqueIdKey = new NamespacedKey(this, "car_key_uid");
        this.carKeyItemMarkerKey = new NamespacedKey(this, "car_key_marker");
        logStartupBanner();

        getServer().getPluginManager().registerEvents(this, this);
        this.physicsTask = getServer()
            .getScheduler()
            .runTaskTimer(
                this,
                new Runnable() {
                    private int tick = 0;

                    @Override
                    public void run() {
                        tick++;
                        boolean updateActionBar =
                            carActionBarEnabled &&
                            carActionBarUpdateTicks > 0 &&
                            (tick % carActionBarUpdateTicks == 0);
                        List<UUID> invalidVehicleIds = null;

                        for (Map.Entry<
                            UUID,
                            CarEntity
                        > entry : cars.entrySet()) {
                            UUID vehicleId = entry.getKey();
                            CarEntity car = entry.getValue();
                            if (car == null) {
                                if (invalidVehicleIds == null) {
                                    invalidVehicleIds = new ArrayList<>();
                                }
                                invalidVehicleIds.add(vehicleId);
                                continue;
                            }
                            if (!car.isValid()) {
                                clearVehicleSoundState(vehicleId);
                                clearCarChunkTickets(vehicleId);
                                if (invalidVehicleIds == null) {
                                    invalidVehicleIds = new ArrayList<>();
                                }
                                invalidVehicleIds.add(vehicleId);
                                continue;
                            }
                            refreshCarChunkTickets(vehicleId, car);
                            if (!(car.getDriver() instanceof Player)) {
                                car.setDriverInput(0.0F, 0.0F, false);
                            }
                            car.tickPhysics();
                            car.updateVisuals();
                            touchCarActivity(car);
                            emitContinuousVehicleSounds(car, tick);

                            if (updateActionBar) {
                                pushActionBar(car);
                            }
                        }
                        if (invalidVehicleIds != null) {
                            for (UUID vehicleId : invalidVehicleIds) {
                                CarEntity removed = cars.remove(vehicleId);
                                if (removed != null) {
                                    noteCarEvent(
                                        removed,
                                        "invalid-entity",
                                        "auto-cleanup"
                                    );
                                    unregisterCarPartMappings(removed);
                                } else {
                                    unregisterCarPartMappings(vehicleId);
                                }
                                clearPlayerCacheForCar(vehicleId);
                                carSpawnedAtMs.remove(vehicleId);
                                carLastUsedAtMs.remove(vehicleId);
                                carLastEventTypeById.remove(vehicleId);
                                carLastEventDetailsById.remove(vehicleId);
                                carLastEventAtMs.remove(vehicleId);
                                carLastEventFlushedAtMs.remove(vehicleId);
                            }
                        }
                    }
                },
                1L,
                1L
            );

        loadCars();
        registerPlaceholderExpansionIfAvailable();
    }

    private void logStartupBanner() {
        String prefix = colorize("&8[&1Cool&9Cars&8] ");
        Bukkit.getConsoleSender().sendMessage(prefix + colorize("&7=========================================="));
        Bukkit.getConsoleSender().sendMessage(prefix + colorize("&fVersion: &c" + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(prefix + colorize("&fAuthor: &c" + String.join(", ", getDescription().getAuthors())));
        Bukkit.getConsoleSender().sendMessage(prefix + colorize("&7=========================================="));
    }

    @Override
    public void onDisable() {
        if (physicsTask != null) {
            physicsTask.cancel();
        }
        for (BukkitTask task : refuelTasks.values()) {
            task.cancel();
        }
        refuelTasks.clear();
        refuelCarByPlayer.clear();
        for (BukkitTask task : repairTasks.values()) {
            task.cancel();
        }
        repairTasks.clear();
        repairCarByPlayer.clear();
        for (BukkitTask task : repairHitboxPreviewTasks.values()) {
            task.cancel();
        }
        repairHitboxPreviewTasks.clear();
        if (pendingRestoreTask != null) {
            pendingRestoreTask.cancel();
            pendingRestoreTask = null;
        }
        stopAutoSaveTask();
        saveCarsOnShutdown();
        clearAllCarChunkTickets();

        for (CarEntity car : cars.values()) {
            unregisterCarPartMappings(car);
            car.destroy();
        }
        cars.clear();
        carChunkTickets.clear();
        carChunkCenterTokenCache.clear();
        partEntityToCar.clear();
        fuelEntityToCar.clear();
        trunkEntityToCar.clear();
        driverToCar.clear();
        playerToCarCache.clear();
        hornCooldownUntilMs.clear();
        jumpHeldByPlayer.clear();
        drivingSoundNextTick.clear();
        idleSoundNextTick.clear();
        pendingIdleStartTick.clear();
        pendingDriveStartTick.clear();
        soundCooldownUntilMs.clear();
        carOwners.clear();
        carOwnerNames.clear();
        carModelById.clear();
        carSpawnedAtMs.clear();
        carLastUsedAtMs.clear();
        carLastEventTypeById.clear();
        carLastEventDetailsById.clear();
        carLastEventAtMs.clear();
        carLastEventFlushedAtMs.clear();

        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        if (ownershipDatabase != null) {
            ownershipDatabase = null;
        }
    }

    private void saveCarsOnShutdown() {
        int count = cars.size();
        List<String> sortedUuids = cars
            .keySet()
            .stream()
            .map(UUID::toString)
            .sorted()
            .collect(Collectors.toList());

        getLogger().info(
            "Shutdown save started. Active cars: " +
                count +
                (sortedUuids.isEmpty()
                    ? ""
                    : " | UUIDs: " + String.join(", ", sortedUuids))
        );

        saveCars(false);
        int persistedYaml =
            carsFile != null && carsFile.exists() ? cars.size() : 0;
        getLogger().info(
            "Shutdown YAML persistence target count: " + persistedYaml
        );

        if (ownershipDatabase != null && ownershipDatabase.isEnabled()) {
            int savedRows = 0;
            for (CarEntity car : cars.values()) {
                boolean saved = upsertOwnershipForCar(car);
                Location l = car.getSafeLocation();
                String pos =
                    l == null || l.getWorld() == null
                        ? "unknown"
                        : l.getWorld().getName() +
                          " " +
                          format1(l.getX()) +
                          " " +
                          format1(l.getY()) +
                          " " +
                          format1(l.getZ());
                getLogger().info(
                    "Shutdown car snapshot | uuid=" +
                        car.getVehicleId() +
                        " | pos=" +
                        pos +
                        " | hp=" +
                        format1(car.getHealth()) +
                        " | fuel=" +
                        format1(car.getFuelLiters()) +
                        " | db=" +
                        (saved ? "OK" : "FAIL")
                );
                if (saved) {
                    savedRows++;
                }
            }
            getLogger().info(
                "Shutdown DB sync completed. Saved rows: " +
                    savedRows +
                    "/" +
                    count
            );
        } else {
            getLogger().info("Shutdown DB sync skipped: database is disabled.");
        }

        getLogger().info(
            "Shutdown save completed. cars.yml: " +
                (carsFile == null
                    ? "not-initialized"
                    : carsFile.getAbsolutePath())
        );
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (
            name.equals("car") &&
            args.length > 0 &&
            args[0].equalsIgnoreCase("reload")
        ) {
            if (!sender.isOp()) {
                msg(sender, "command.no-permission");
                return true;
            }
            int reloaded = reloadCarsFromConfig();
            msg(sender, "car.reload.done", reloaded);
            return true;
        }
        if (
            name.equals("car") &&
            args.length > 0 &&
            args[0].equalsIgnoreCase("key")
        ) {
            return handleCarKeyCommand(sender, args);
        }
        if (
            name.equals("car") &&
            args.length > 0 &&
            args[0].equalsIgnoreCase("heal")
        ) {
            return handleCarHealCommand(sender, args);
        }

        if (!(sender instanceof Player player)) {
            msg(sender, "command.only-player");
            return true;
        }

        if (name.equals("lang")) {
            return handleLangCommand(player, args);
        }
        if (name.equals("car")) {
            return handleCarCommand(player, args);
        }
        if (name.equals("fuel")) {
            return handleFuelCommand(player, args);
        }
        if (name.equals("repair")) {
            return handleRepairCommand(player, args);
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(
        CommandSender sender,
        Command command,
        String alias,
        String[] args
    ) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("fuel")) {
            if (args.length == 1) {
                return complete(args[0], List.of("give", "set"));
            }
            if (
                args.length == 2 &&
                (args[0].equalsIgnoreCase("give") ||
                    args[0].equalsIgnoreCase("set"))
            ) {
                return complete(args[1], List.of("1", "5", "10"));
            }
        }
        if (name.equals("repair")) {
            if (args.length == 1) {
                return complete(args[0], List.of("give", "set"));
            }
            if (
                args.length == 2 &&
                (args[0].equalsIgnoreCase("give") ||
                    args[0].equalsIgnoreCase("set"))
            ) {
                return complete(args[1], List.of("1", "5", "10"));
            }
        }
        if (name.equals("car")) {
            if (args.length == 1) {
                return complete(
                    args[0],
                    List.of(
                        "help",
                        "spawn",
                        "info",
                        "list",
                        "remove",
                        "key",
                        "heal",
                        "tp",
                        "reload",
                        "repairhitbox"
                    )
                );
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("key")) {
                return complete(args[1], List.of("give"));
            }
            if (
                args.length == 3 &&
                args[0].equalsIgnoreCase("key") &&
                args[1].equalsIgnoreCase("give")
            ) {
                List<String> names = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName() != null) {
                        names.add(online.getName());
                    }
                }
                return complete(args[2], names);
            }
            if (
                args.length == 4 &&
                args[0].equalsIgnoreCase("key") &&
                args[1].equalsIgnoreCase("give")
            ) {
                List<String> ids = new ArrayList<>();
                for (UUID id : cars.keySet()) {
                    ids.add(id.toString());
                }
                return complete(args[3], ids);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("heal")) {
                List<String> ids = new ArrayList<>();
                for (UUID id : cars.keySet()) {
                    ids.add(id.toString());
                }
                return complete(args[1], ids);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("heal")) {
                return complete(
                    args[2],
                    List.of(
                        "car",
                        "front",
                        "rear",
                        "wheel_fl",
                        "wheel_fr",
                        "wheel_rl",
                        "wheel_rr",
                        "wheels",
                        "all"
                    )
                );
            }
            if (args.length == 4 && args[0].equalsIgnoreCase("heal")) {
                return complete(args[3], List.of("full", "25", "50", "100"));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
                return complete(
                    args[1],
                    new ArrayList<>(modelSettings.keySet())
                );
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("repairhitbox")) {
                return complete(args[1], List.of("on", "off"));
            }
            if (
                args.length == 2 &&
                (args[0].equalsIgnoreCase("remove") ||
                    args[0].equalsIgnoreCase("info"))
            ) {
                List<String> ids = new ArrayList<>();
                for (UUID id : cars.keySet()) {
                    ids.add(id.toString());
                }
                return complete(args[1], ids);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
                List<String> ids = new ArrayList<>();
                for (UUID id : cars.keySet()) {
                    ids.add(id.toString());
                }
                return complete(args[1], ids);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("tp")) {
                List<String> names = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName() != null) {
                        names.add(online.getName());
                    }
                }
                return complete(args[2], names);
            }
        }
        if (name.equals("lang")) {
            if (args.length == 1) {
                return complete(args[0], List.of("set", "auto", "server"));
            }
            if (
                args.length == 2 &&
                (args[0].equalsIgnoreCase("set") ||
                    args[0].equalsIgnoreCase("server"))
            ) {
                return complete(args[1], List.of("en", "ru"));
            }
        }
        return Collections.emptyList();
    }

    @EventHandler
    public void onPlayerInput(PlayerInputEvent event) {
        UUID carId = driverToCar.get(event.getPlayer().getUniqueId());
        if (carId == null) {
            return;
        }

        CarEntity car = cars.get(carId);
        if (car == null || !car.isValid()) {
            UUID id = event.getPlayer().getUniqueId();
            driverToCar.remove(id);
            playerToCarCache.remove(id);
            jumpHeldByPlayer.remove(id);
            return;
        }

        Entity driver = car.getDriver();
        if (
            !(driver instanceof Player player) ||
            !player.getUniqueId().equals(event.getPlayer().getUniqueId())
        ) {
            UUID id = event.getPlayer().getUniqueId();
            driverToCar.remove(id);
            playerToCarCache.remove(id);
            jumpHeldByPlayer.remove(id);
            return;
        }

        float steer =
            (event.getInput().isRight() ? 1.0F : 0.0F) -
            (event.getInput().isLeft() ? 1.0F : 0.0F);
        float forward =
            (event.getInput().isForward() ? 1.0F : 0.0F) -
            (event.getInput().isBackward() ? 1.0F : 0.0F);
        boolean jump = event.getInput().isJump();
        boolean brake = jump || event.getInput().isSneak();
        car.setDriverInput(steer, forward, brake);

        UUID playerId = event.getPlayer().getUniqueId();
        boolean wasJumpHeld = jumpHeldByPlayer.getOrDefault(playerId, false);
        if (jump && !wasJumpHeld) {
            tryPlayHorn(event.getPlayer(), car);
        }
        jumpHeldByPlayer.put(playerId, jump);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID carId = driverToCar.get(player.getUniqueId());
        if (carId == null) {
            return;
        }
        CarEntity car = cars.get(carId);
        if (
            car == null ||
            !car.isValid() ||
            !(car.getDriver() instanceof Player driver) ||
            !driver.getUniqueId().equals(player.getUniqueId())
        ) {
            return;
        }

        event.setCancelled(true);
        openCarMenu(player, car);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTrunkClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CarTrunkHolder holder)) {
            return;
        }
        // No cancellation here, we want players to be able to move items in the trunk.
        // We sync the contents on close.
    }

    @EventHandler(ignoreCancelled = true)
    public void onCarMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CarMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() != top) {
            return;
        }

        CarEntity car = cars.get(holder.carId);
        if (
            car == null ||
            !car.isValid() ||
            !(car.getDriver() instanceof Player driver) ||
            !driver.getUniqueId().equals(player.getUniqueId())
        ) {
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();
        if (slot == menuSlotEngine) {
            if (!car.isEngineRunning()) {
                String denyReason = canStartEngine(player, car);
                if (denyReason != null) {
                    msg(player, denyReason);
                    openCarMenu(player, car);
                    return;
                }
            } else {
                String denyReason = canStopEngine(player, car);
                if (denyReason != null) {
                    msg(player, denyReason);
                    openCarMenu(player, car);
                    return;
                }
            }
            boolean running = car.toggleEngineRunning();
            carLastUsedAtMs.put(car.getVehicleId(), System.currentTimeMillis());
            noteCarEvent(
                car,
                running ? "engine_started" : "engine_stopped",
                "menu-toggle"
            );
            playSoundAtCar(car, running ? soundEngineStart : soundEngineStop);
            if (!running) {
                stopContinuousCarSounds(car);
            }
            msg(player, running ? "car.engine.on" : "car.engine.off");
            openCarMenu(player, car);
            return;
        }
        if (slot == menuSlotLights) {
            boolean on = car.toggleHeadlights();
            carLastUsedAtMs.put(car.getVehicleId(), System.currentTimeMillis());
            noteCarEvent(car, on ? "lights_on" : "lights_off", "menu-toggle");
            msg(player, on ? "car.lights.on" : "car.lights.off");
            openCarMenu(player, car);
            return;
        }
        if (menuTrunkEnabled && slot == menuSlotTrunk) {
            player.closeInventory();
            String trunkTitle = colorize(menuText(player, "trunk", "menu.car.trunk"));
            Inventory trunkInv = Bukkit.createInventory(
                new CarTrunkHolder(car.getVehicleId(), car.getTrunkInventory()),
                car.getTrunkInventory().getSize(),
                trunkTitle
            );
            trunkInv.setContents(car.getTrunkInventory().getContents());
            player.openInventory(trunkInv);
            playSoundAtCar(car, soundGlovebox);
        }
    }

    @EventHandler
    public void onTrunkClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CarTrunkHolder holder)) {
            return;
        }
        holder.inventory.setContents(top.getContents());
        player.updateInventory();
    }

    @EventHandler
    public void onCarMenuClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CarMenuHolder)) {
            return;
        }
        player.updateInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isRepairKit(hand)) {
            return;
        }
        CarEntity partCar = findCarByPart(event.getRightClicked());
        if (partCar == null || !partCar.isValid()) {
            return;
        }
        event.setCancelled(true);
        if (!partCar.isDamagePointEntity(event.getRightClicked())) {
            msg(player, "repair.hitbox-only");
            return;
        }
        CarEntity.DamagePart part = partCar.resolveDamagePart(
            event.getRightClicked(),
            null
        );
        startRepair(player, partCar, part);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        CarEntity fuelCar = findCarByFuelPoint(event.getRightClicked());
        CarEntity partCar = findCarByPart(event.getRightClicked());
        CarEntity trunkCar = findCarByTrunkPoint(event.getRightClicked());
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (
            partCar != null &&
            partCar.isValid() &&
            player.isSneaking() &&
            isFuelCanister(hand) &&
            player
                .getLocation()
                .distanceSquared(partCar.getFuelPointLocation()) <=
            9.0D
        ) {
            event.setCancelled(true);
            startRefuel(player, partCar);
            return;
        }

        if (fuelCar != null && fuelCar.isValid()) {
            event.setCancelled(true);
            if (!player.isSneaking()) {
                msg(player, "fuel.hold-shift");
                return;
            }
            startRefuel(player, fuelCar);
            return;
        }

        if (trunkCar != null && trunkCar.isValid()) {
            event.setCancelled(true);
            if (player.isSneaking()) {
                return;
            }
            if (
                player
                    .getLocation()
                    .distanceSquared(trunkCar.getTrunkPointLocation()) >
                16.0D
            ) {
                msg(player, "trunk.too-far");
                return;
            }
            String trunkTitle = colorize(menuText(player, "trunk", "menu.car.trunk"));
            Inventory trunkInv = Bukkit.createInventory(
                new CarTrunkHolder(trunkCar.getVehicleId(), trunkCar.getTrunkInventory()),
                trunkCar.getTrunkInventory().getSize(),
                trunkTitle
            );
            trunkInv.setContents(trunkCar.getTrunkInventory().getContents());
            player.openInventory(trunkInv);
            playSoundAtCar(trunkCar, soundGlovebox);
            return;
        }

        CarEntity car = partCar;
        if (car == null || !car.isValid()) {
            return;
        }
        event.setCancelled(true);
        if (player.isSneaking()) {
            return;
        }

        int seatIndex = car.findNearestFreeSeatIndex(player.getLocation());
        if (seatIndex < 0) {
            msg(player, "seat.no-free");
            return;
        }
        if (!car.mountSeat(player, seatIndex)) {
            msg(player, "seat.enter-failed");
            return;
        }
        playSoundAtCar(car, soundSeatOpen);

        UUID playerId = player.getUniqueId();
        driverToCar.remove(playerId);
        cachePlayerCar(playerId, car.getVehicleId());
        if (seatIndex == 0) {
            driverToCar.put(playerId, car.getVehicleId());
            msg(player, "seat.driver");
        } else {
            msg(player, "seat.passenger");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!projectileDamageEnabled) {
            return;
        }
        Entity hit = event.getHitEntity();
        if (hit == null) {
            return;
        }
        CarEntity car = findCarByPart(hit);
        if (car == null || !car.isValid()) {
            return;
        }

        Projectile projectile = event.getEntity();
        Entity shooterEntity =
            projectile.getShooter() instanceof Entity entity ? entity : null;
        if (
            projectileIgnoreOccupants &&
            shooterEntity != null &&
            car.hasPlayer(shooterEntity.getUniqueId())
        ) {
            if (
                projectileNotifyFriendlyBlocked &&
                shooterEntity instanceof Player shooter
            ) {
                msg(shooter, "combat.projectile.blocked-self-car");
            }
            if (projectile instanceof AbstractArrow arrow) {
                arrow.remove();
            }
            return;
        }
        String projectileType = "other";
        double typeMultiplier = projectileOtherDamageMultiplier;
        if (projectile instanceof org.bukkit.entity.Trident) {
            projectileType = "trident";
            typeMultiplier = projectileTridentDamageMultiplier;
        } else if (projectile instanceof AbstractArrow) {
            projectileType = "arrow";
            typeMultiplier = projectileArrowDamageMultiplier;
        }

        double speed = projectile.getVelocity().length();
        double baseDamage =
            projectileBaseDamage * typeMultiplier +
            Math.max(0.0D, speed) * projectileSpeedDamageFactor;
        CarEntity.DamagePart part = car.resolveDamagePart(
            hit,
            projectile.getLocation()
        );
        car.applyProjectileImpact(part, projectileType, baseDamage);
        if (projectilePlayHitSound) {
            playSoundAtCar(car, soundHit);
        }

        if (
            projectileStickArrows && projectile instanceof AbstractArrow arrow
        ) {
            stickArrowToCarPart(arrow, hit);
        }

        if (
            projectileNotifyShooter &&
            projectile.getShooter() instanceof Player shooter
        ) {
            msg(
                shooter,
                "combat.projectile.hit",
                damagePartLabel(shooter, part),
                format1(baseDamage)
            );
        }
        if (
            projectileNotifyDriver && car.getDriver() instanceof Player driver
        ) {
            msg(
                driver,
                "combat.projectile.taken",
                damagePartLabel(driver, part),
                format1(baseDamage)
            );
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        clearPlayerControlState(playerId);
        getServer()
            .getScheduler()
            .runTaskLater(
                this,
                () -> unmountPlayerFromCars(playerId, false),
                1L
            );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        boolean wasInCar = findCarByPlayer(playerId) != null;
        clearPlayerControlState(playerId);
        unmountPlayerFromCars(playerId, true);
        if (wasInCar) {
            saveCars(false);
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            UUID id = player.getUniqueId();
            driverToCar.remove(id);
            playerToCarCache.remove(id);
            jumpHeldByPlayer.remove(id);
            stopRepairTask(id);
            CarEntity car = findCarByPart(event.getDismounted());
            if (car != null && car.isValid()) {
                stopDrivingSoundState(car);
                playSoundAtCar(car, soundSeatClose);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();
        if (victim == null || damager == null) {
            return;
        }

        if (victim instanceof Player playerVictim) {
            CarEntity car = findCarByPlayer(playerVictim.getUniqueId());
            if (car == null || !car.isValid()) {
                return;
            }
            if (damager instanceof Projectile) {
                return;
            }

            event.setCancelled(true);
            if (
                damager instanceof Player attacker &&
                car.hasPlayer(attacker.getUniqueId())
            ) {
                msg(attacker, "combat.melee.blocked-self-car");
                return;
            }
            if (!meleeDamageToCarEnabled) {
                return;
            }
            CarEntity.DamagePart part = car.resolveClosestDamagePart(
                damager.getLocation()
            );
            double damage =
                Math.max(0.0D, event.getFinalDamage()) * meleeDamageMultiplier;
            car.applyProjectileImpact(part, "other", damage);
            if (meleePlayHitSound) {
                playSoundAtCar(car, soundHit);
            }
            return;
        }

        CarEntity car = findCarByPart(victim);
        if (car == null || !car.isValid()) {
            return;
        }
        event.setCancelled(true);
        if (damager instanceof Projectile) {
            return;
        }
        if (
            damager instanceof Player attacker &&
            car.hasPlayer(attacker.getUniqueId())
        ) {
            msg(attacker, "combat.melee.blocked-self-car");
            return;
        }
        if (!meleeDamageToCarEnabled) {
            return;
        }
        CarEntity.DamagePart part = car.resolveDamagePart(
            victim,
            damager.getLocation()
        );
        double damage =
            Math.max(0.0D, event.getFinalDamage()) * meleeDamageMultiplier;
        car.applyProjectileImpact(part, "other", damage);
        if (meleePlayHitSound) {
            playSoundAtCar(car, soundHit);
        }
    }

    private void reloadPluginConfig() {
        ensureSplitConfigStructure();
        reloadConfigWithSanitization();
        this.runtimeConfig = buildRuntimeConfig();
        i18n.reload();
        String defaultLang = i18n.normalizeLang(
            cfg().getString("language.default", "en")
        );
        resolveMainConfigTrunkTitle(defaultLang);
        loadCarModels(runtimeConfig, defaultLang);
        String configuredDefaultModel = normalizeModelKey(
            runtimeConfig.getString("cars.default-model", "volga")
        );
        if (!modelSettings.containsKey(configuredDefaultModel)) {
            configuredDefaultModel = firstModelKey();
        }
        this.defaultModel = configuredDefaultModel;
        this.carSettings = modelSettings.get(defaultModel);
        if (carSettings == null) {
            this.carSettings = CarSettings.fromConfig(runtimeConfig);
        }

        this.carActionBarEnabled = cfg().getBoolean(
            "ui.action-bar.car.enabled",
            cfg().getBoolean("ui.action-bar.enabled", true)
        );
        this.carActionBarUpdateTicks = Math.max(
            1,
            cfg().getInt(
                "ui.action-bar.car.update-ticks",
                cfg().getInt("ui.action-bar.update-ticks", 2)
            )
        );
        this.carActionBarFormat = cfg().getString(
            "ui.action-bar.car.format",
            ""
        );
        this.carActionBarFormatLangKey = cfg().getString(
            "ui.action-bar.car.format-lang-key",
            "actionbar.car"
        );
        this.refuelActionBarEnabled = cfg().getBoolean(
            "ui.action-bar.refuel.enabled",
            cfg().getBoolean("ui.action-bar.enabled", true)
        );
        this.refuelActionBarUpdateTicks = Math.max(
            1,
            cfg().getInt(
                "ui.action-bar.refuel.update-ticks",
                cfg().getInt("ui.action-bar.update-ticks", 2)
            )
        );
        this.refuelActionBarFormat = cfg().getString(
            "ui.action-bar.refuel.format",
            ""
        );
        this.refuelActionBarFormatLangKey = cfg().getString(
            "ui.action-bar.refuel.format-lang-key",
            "actionbar.refuel"
        );
        this.repairActionBarEnabled = cfg().getBoolean(
            "ui.action-bar.repair.enabled",
            cfg().getBoolean("ui.action-bar.enabled", true)
        );
        this.repairActionBarUpdateTicks = Math.max(
            1,
            cfg().getInt(
                "ui.action-bar.repair.update-ticks",
                cfg().getInt("ui.action-bar.update-ticks", 2)
            )
        );
        this.repairActionBarFormat = cfg().getString(
            "ui.action-bar.repair.format",
            ""
        );
        this.repairActionBarFormatLangKey = cfg().getString(
            "ui.action-bar.repair.format-lang-key",
            "actionbar.repair"
        );
        this.autoSaveEnabled = cfg().getBoolean(
            "storage.autosave.enabled",
            true
        );
        this.autoSaveIntervalTicks = Math.max(
            100,
            cfg().getInt("storage.autosave.interval-ticks", 20 * 30)
        );
        startAutoSaveTask();
        loadSoundsConfig();
        this.menuRows = Math.max(
            1,
            Math.min(6, cfg().getInt("car.menu.rows", 3))
        );
        this.menuSize = menuRows * 9;
        this.menuSlotEngine = clampMenuSlot(
            cfg().getInt("car.menu.slots.engine", 11),
            11,
            menuSize - 1
        );
        this.menuSlotLights = clampMenuSlot(
            cfg().getInt("car.menu.slots.lights", 15),
            15,
            menuSize - 1
        );
        this.menuSlotTrunk = clampMenuSlot(
            cfg().getInt("car.menu.slots.trunk", 13),
            13,
            menuSize - 1
        );
        this.menuTrunkEnabled = cfg().getBoolean(
            "car.menu.trunk-button-enabled",
            true
        );
        this.menuFillEmptySlots = cfg().getBoolean(
            "car.menu.style.fill-empty-slots",
            true
        );
        normalizeMenuLayout();
        this.menuFillerMaterial = parseMaterial(
            cfg().getString(
                "car.menu.style.filler.material",
                "BLACK_STAINED_GLASS_PANE"
            ),
            Material.BLACK_STAINED_GLASS_PANE
        );
        this.menuFillerName = cfg().getString(
            "car.menu.style.filler.name",
            "&8 "
        );
        this.menuFillerCustomModelData = cfg().getInt(
            "car.menu.style.filler.custom-model-data",
            0
        );
        this.menuEngineMaterialOn = parseMaterial(
            cfg().getString(
                "car.menu.style.items.engine.material-on",
                "LIME_DYE"
            ),
            Material.LIME_DYE
        );
        this.menuEngineMaterialOff = parseMaterial(
            cfg().getString(
                "car.menu.style.items.engine.material-off",
                "RED_DYE"
            ),
            Material.RED_DYE
        );
        this.menuEngineCustomModelDataOn = cfg().getInt(
            "car.menu.style.items.engine.custom-model-data-on",
            0
        );
        this.menuEngineCustomModelDataOff = cfg().getInt(
            "car.menu.style.items.engine.custom-model-data-off",
            0
        );
        this.menuLightsMaterialOn = parseMaterial(
            cfg().getString(
                "car.menu.style.items.lights.material-on",
                "GLOWSTONE_DUST"
            ),
            Material.GLOWSTONE_DUST
        );
        this.menuLightsMaterialOff = parseMaterial(
            cfg().getString("car.menu.style.items.lights.material-off", "COAL"),
            Material.COAL
        );
        this.menuLightsCustomModelDataOn = cfg().getInt(
            "car.menu.style.items.lights.custom-model-data-on",
            0
        );
        this.menuLightsCustomModelDataOff = cfg().getInt(
            "car.menu.style.items.lights.custom-model-data-off",
            0
        );
        this.menuTrunkMaterial = parseMaterial(
            cfg().getString("car.menu.style.items.trunk.material", "CHEST"),
            Material.CHEST
        );
        this.menuTrunkCustomModelData = cfg().getInt(
            "car.menu.style.items.trunk.custom-model-data",
            0
        );
        loadMenuLocalization();

        this.canisterMaxLiters = Math.max(
            0.1D,
            cfg().getDouble("fuel.canister.capacity-liters", 10.0D)
        );
        String canisterMaterialName = cfg().getString(
            "fuel.canister.material",
            "HONEY_BOTTLE"
        );
        Material parsed = Material.matchMaterial(
            canisterMaterialName == null ? "" : canisterMaterialName
        );
        this.canisterMaterial = parsed == null ? Material.HONEY_BOTTLE : parsed;
        this.canisterDisplayName = cfg().getString(
            "fuel.canister.display-name",
            "&6Fuel Canister &7({liters}/{capacity}L)"
        );
        this.canisterLoreLines = cfg().getStringList("fuel.canister.lore");
        if (
            this.canisterLoreLines == null || this.canisterLoreLines.isEmpty()
        ) {
            this.canisterLoreLines = List.of(
                "&7Fuel: &f{liters}&7/&f{capacity}&7 L",
                "&8Use SHIFT + RMB on car fuel point"
            );
        }
        loadCanisterLocalization();
        this.canisterCustomModelData = cfg().isSet(
            "fuel.canister.custom-model-data"
        )
            ? cfg().getInt("fuel.canister.custom-model-data")
            : null;
        this.canisterUnbreakable = cfg().getBoolean(
            "fuel.canister.unbreakable",
            false
        );
        this.canisterParsedTags = new HashMap<>();
        ConfigurationSection nbtSection = cfg().getConfigurationSection(
            "fuel.canister.nbt-tags"
        );
        if (nbtSection != null) {
            for (String key : nbtSection.getKeys(false)) {
                Object value = nbtSection.get(key);
                if (value != null) {
                    String text = String.valueOf(value);
                    NamespacedKey parsedKey = parseTagKey(key);
                    if (parsedKey != null) {
                        canisterParsedTags.put(parsedKey, text);
                    }
                }
            }
        }
        this.repairKitMaxUnits = Math.max(
            0.1D,
            cfg().getDouble("repair.kit.max-units", 10.0D)
        );
        this.repairRateUnitsPerTick = Math.max(
            0.001D,
            cfg().getDouble("repair.rate-units-per-tick", 0.08D)
        );
        this.repairKitMaterial = parseMaterial(
            cfg().getString("repair.kit.material", "RABBIT_HIDE"),
            Material.RABBIT_HIDE
        );
        this.repairKitDisplayName = cfg().getString(
            "repair.kit.display-name",
            "&bRepair Kit &7({units}/{capacity})"
        );
        this.repairKitLoreLines = cfg().getStringList("repair.kit.lore");
        if (repairKitLoreLines == null || repairKitLoreLines.isEmpty()) {
            this.repairKitLoreLines = List.of(
                "&7Units: &f{units}&7/&f{capacity}",
                "&8SHIFT + RMB on damaged part"
            );
        }
        this.repairKitCustomModelData = cfg().isSet(
            "repair.kit.custom-model-data"
        )
            ? cfg().getInt("repair.kit.custom-model-data")
            : null;
        this.repairKitUnbreakable = cfg().getBoolean(
            "repair.kit.unbreakable",
            false
        );
        this.repairKitParsedTags = new HashMap<>();
        ConfigurationSection repairTagsSection = cfg().getConfigurationSection(
            "repair.kit.nbt-tags"
        );
        if (repairTagsSection != null) {
            for (String key : repairTagsSection.getKeys(false)) {
                Object value = repairTagsSection.get(key);
                if (value == null) {
                    continue;
                }
                NamespacedKey parsedKey = parseTagKey(key);
                if (parsedKey != null) {
                    repairKitParsedTags.put(parsedKey, String.valueOf(value));
                }
            }
        }
        loadRepairKitLocalization();
        this.keysEnabled = cfg().getBoolean("keys.enabled", false);
        this.keysRequireToStartEngine = cfg().getBoolean(
            "keys.require-key-to-start-engine",
            true
        );
        this.keysRequireToStopEngine = cfg().getBoolean(
            "keys.require-key-to-stop-engine",
            true
        );
        this.keysAllowOwnerStartWithoutKey = cfg().getBoolean(
            "keys.allow-owner-start-without-key",
            true
        );
        this.keysAllowNonOwnerUseKey = cfg().getBoolean(
            "keys.allow-non-owner-use-key",
            false
        );
        this.keysCheckEntireInventory = cfg().getBoolean(
            "keys.check-entire-inventory",
            true
        );
        this.keyItemMaterial = parseMaterial(
            cfg().getString("keys.item.material", "TRIPWIRE_HOOK"),
            Material.TRIPWIRE_HOOK
        );
        this.keyItemDisplayName = cfg().getString(
            "keys.item.name",
            "&6Key for {model}"
        );
        this.keyItemLoreLines = cfg().getStringList("keys.item.lore");
        if (keyItemLoreLines == null || keyItemLoreLines.isEmpty()) {
            this.keyItemLoreLines = List.of(
                "&7Vehicle: &f{model}",
                "&7UUID: &f{car_uuid}",
                "&7Owner: &f{owner_name}"
            );
        }
        loadKeyLocalization();
        this.keyItemCustomModelData = cfg().isSet("keys.item.custom-model-data")
            ? cfg().getInt("keys.item.custom-model-data")
            : null;
        this.keyItemUnbreakable = cfg().getBoolean(
            "keys.item.unbreakable",
            true
        );
        this.healCommandEnabled = cfg().getBoolean(
            "commands.car-heal.enabled",
            true
        );
        this.healCommandConsoleAllowed = cfg().getBoolean(
            "commands.car-heal.allow-console",
            true
        );

        if (configDocManager != null) {
            configDocManager.sync(defaultLang);
        }
        if (ownershipDatabase != null) {
            ownershipDatabase.reloadFromConfig(cfg());
        }
    }

    private void reloadConfigWithSanitization() {
        try {
            reloadConfig();
            return;
        } catch (Exception primary) {
            File configFile = new File(getDataFolder(), "config.yml");
            int cleaned = sanitizeInvalidYamlControlBytes(configFile);
            if (cleaned <= 0) {
                throw primary;
            }
            getLogger().warning(
                "Detected invalid control bytes in config.yml (" +
                    cleaned +
                    " byte(s)). Auto-sanitized file and retrying load."
            );
            reloadConfig();
        }
    }

    private int sanitizeInvalidYamlControlBytes(File file) {
        if (file == null || !file.isFile()) {
            return 0;
        }
        byte[] input;
        try {
            input = Files.readAllBytes(file.toPath());
        } catch (IOException readError) {
            getLogger().warning(
                "Failed to read config.yml for sanitization: " +
                    readError.getMessage()
            );
            return 0;
        }
        int replaced = 0;
        for (int i = 0; i < input.length; i++) {
            int value = input[i] & 0xFF;
            boolean isAllowedWhitespace =
                value == '\t' || value == '\n' || value == '\r';
            boolean isControl =
                (value <= 0x1F && !isAllowedWhitespace) ||
                (value >= 0x7F && value <= 0x9F);
            if (isControl) {
                input[i] = (byte) ' ';
                replaced++;
            }
        }
        if (replaced <= 0) {
            return 0;
        }
        try {
            Files.write(file.toPath(), input);
            return replaced;
        } catch (IOException writeError) {
            getLogger().warning(
                "Failed to write sanitized config.yml: " +
                    writeError.getMessage()
            );
            return 0;
        }
    }

    private void ensureSplitConfigStructure() {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            ensureResourceIfMissing("config.yml");
            ensureResourceIfMissing("items.yml");
            ensureResourceIfMissing("sounds.yml");
            ensureResourceIfMissing("cars/Volga/components.yml");
            ensureResourceIfMissing("cars/Volga/performance.yml");
            ensureResourceIfMissing("cars/Volga/behavior.yml");
            ensureResourceIfMissing("cars/Volga/localization.yml");
            ensureResourceIfMissing("lang/en/core.yml");
            ensureResourceIfMissing("lang/en/commands.yml");
            ensureResourceIfMissing("lang/en/ui.yml");
            ensureResourceIfMissing("lang/en/menu.yml");
            ensureResourceIfMissing("lang/en/help.yml");
            ensureResourceIfMissing("lang/ru/core.yml");
            ensureResourceIfMissing("lang/ru/commands.yml");
            ensureResourceIfMissing("lang/ru/ui.yml");
            ensureResourceIfMissing("lang/ru/menu.yml");
            ensureResourceIfMissing("lang/ru/help.yml");
        } catch (Exception ex) {
            getLogger().warning(
                "Failed to initialize split config structure: " +
                    ex.getMessage()
            );
        }
    }

    private void ensureResourceIfMissing(String path) {
        File target = new File(getDataFolder(), path);
        if (target.exists()) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        saveResource(path, false);
    }

    private YamlConfiguration buildRuntimeConfig() {
        YamlConfiguration merged = new YamlConfiguration();
        mergeConfigInto(merged, getConfig());
        mergeConfigInto(
            merged,
            loadYaml(new File(getDataFolder(), "items.yml"))
        );
        mergeConfigInto(
            merged,
            loadYaml(new File(getDataFolder(), "sounds.yml"))
        );
        return merged;
    }

    private void loadCarModels(
        YamlConfiguration globalConfig,
        String defaultLang
    ) {
        modelSettings.clear();
        modelConfigs.clear();
        modelDisplayNames.clear();
        File folder = new File(getDataFolder(), "cars");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File[] modelDirs = folder.listFiles(File::isDirectory);
        if (modelDirs == null) {
            return;
        }
        for (File modelDir : modelDirs) {
            String modelKey = normalizeModelKey(modelDir.getName());
            YamlConfiguration merged = new YamlConfiguration();
            mergeConfigInto(merged, globalConfig);
            mergeConfigInto(
                merged,
                loadYaml(new File(modelDir, "components.yml"))
            );
            mergeConfigInto(
                merged,
                loadYaml(new File(modelDir, "performance.yml"))
            );
            mergeConfigInto(
                merged,
                loadYaml(new File(modelDir, "behavior.yml"))
            );
            mergeConfigInto(
                merged,
                loadYaml(new File(modelDir, "localization.yml"))
            );
            resolveTrunkTitleInMergedConfig(merged, defaultLang);
            modelConfigs.put(modelKey, merged);
            modelSettings.put(modelKey, CarSettings.fromConfig(merged));
            String modelName = merged.getString("car.meta.display-name");
            if (modelName == null || modelName.isBlank()) {
                modelName = modelDir.getName();
            }
            modelDisplayNames.put(modelKey, modelName);
        }
        if (modelSettings.isEmpty()) {
            modelSettings.put("volga", CarSettings.fromConfig(globalConfig));
            modelConfigs.put("volga", globalConfig);
            modelDisplayNames.put("volga", "Volga");
        }
    }

    private void resolveMainConfigTrunkTitle(String defaultLang) {
        if (cfg().getBoolean("car.trunk.use-lang-title", true)) {
            String titleKey = cfg().getString(
                "car.trunk.title-lang-key",
                "trunk.title"
            );
            if (titleKey != null && !titleKey.isBlank()) {
                String trimmedKey = titleKey.trim();
                String translated = i18n.trByLang(defaultLang, trimmedKey);
                if (
                    translated.equals(trimmedKey) &&
                    "trunk.title".equals(trimmedKey)
                ) {
                    String fromMenu = i18n.trByLang(
                        defaultLang,
                        "menu.trunk.title"
                    );
                    if (!fromMenu.equals("menu.trunk.title")) {
                        translated = fromMenu;
                    }
                }
                if (!translated.equals(trimmedKey)) {
                    cfg().set("car.trunk.title", translated);
                } else {
                    cfg().set(
                        "car.trunk.title",
                        i18n.trByLang(defaultLang, "trunk.title")
                    );
                }
            }
        }
        String trunkTitleByConfigLang = getConfigStringStrict(
            "car.trunk.localization." + defaultLang + ".title"
        );
        if (
            trunkTitleByConfigLang != null && !trunkTitleByConfigLang.isBlank()
        ) {
            cfg().set("car.trunk.title", trunkTitleByConfigLang);
        }
        String effectiveTrunkTitle = cfg().getString("car.trunk.title", "");
        if (
            effectiveTrunkTitle != null &&
            !effectiveTrunkTitle.isBlank() &&
            effectiveTrunkTitle.equals(
                effectiveTrunkTitle.toLowerCase(Locale.ROOT)
            ) &&
            effectiveTrunkTitle.matches("[a-z0-9_.-]+")
        ) {
            String recovered = i18n.trByLang(defaultLang, effectiveTrunkTitle);
            if (!recovered.equals(effectiveTrunkTitle)) {
                cfg().set("car.trunk.title", recovered);
            } else {
                cfg().set(
                    "car.trunk.title",
                    i18n.trByLang(defaultLang, "trunk.title")
                );
            }
        }
        String trunkTitle = cfg().getString("car.trunk.title", "");
        if (trunkTitle != null && !trunkTitle.isBlank()) {
            String plain = trunkTitle
                .replace('\u00A7', '&')
                .replaceAll("(?i)&[0-9a-fk-or]", "")
                .trim()
                .toLowerCase(Locale.ROOT);
            if (
                plain.equals("truck") ||
                plain.equals("trunk") ||
                plain.equals("menu.open") ||
                plain.equals("menu.car.trunk")
            ) {
                cfg().set(
                    "car.trunk.title",
                    i18n.trByLang(defaultLang, "trunk.title")
                );
            }
        }
    }

    private void resolveTrunkTitleInMergedConfig(
        YamlConfiguration merged,
        String defaultLang
    ) {
        boolean useLangTitle = merged.getBoolean(
            "car.trunk.use-lang-title",
            true
        );
        String titleLangKey = merged.getString(
            "car.trunk.title-lang-key",
            "menu.trunk.title"
        );
        String fallbackTitle = merged.getString("car.trunk.title", "Trunk");
        String locTitle = merged.getString(
            "car.trunk.localization." + defaultLang + ".title"
        );
        String resolved;
        if (useLangTitle && titleLangKey != null && !titleLangKey.isBlank()) {
            String key = titleLangKey.trim();
            resolved = i18n.trByLang(defaultLang, key);
            if (resolved.equals(key)) {
                String altKey = "menu.trunk.title".equals(key)
                    ? "trunk.title"
                    : "menu.trunk.title";
                String alt = i18n.trByLang(defaultLang, altKey);
                resolved = !alt.equals(altKey)
                    ? alt
                    : (locTitle != null && !locTitle.isBlank()
                          ? locTitle
                          : fallbackTitle);
            }
        } else if (locTitle != null && !locTitle.isBlank()) {
            resolved = locTitle;
        } else {
            resolved =
                fallbackTitle != null && !fallbackTitle.isBlank()
                    ? fallbackTitle
                    : "Trunk";
        }
        String path = "car.trunk.title";
        merged.set(path, resolved);
    }

    private YamlConfiguration loadYaml(File file) {
        if (file == null || !file.isFile()) {
            return new YamlConfiguration();
        }
        try (
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(
                fis,
                StandardCharsets.UTF_8
            )
        ) {
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.load(reader);
            return cfg;
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning(
                "Could not load YAML " + file.getName() + ": " + e.getMessage()
            );
            return new YamlConfiguration();
        }
    }

    private void mergeConfigInto(
        YamlConfiguration target,
        ConfigurationSection source
    ) {
        if (target == null || source == null) {
            return;
        }
        for (String key : source.getKeys(true)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection) {
                continue;
            }
            target.set(key, value);
        }
    }

    private YamlConfiguration cfg() {
        return runtimeConfig == null ? new YamlConfiguration() : runtimeConfig;
    }

    private String normalizeModelKey(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return "volga";
        }
        return value.replaceAll("[^a-z0-9_-]", "");
    }

    private String firstModelKey() {
        if (modelSettings.isEmpty()) {
            return "volga";
        }
        return modelSettings
            .keySet()
            .stream()
            .sorted()
            .findFirst()
            .orElse("volga");
    }

    private CarSettings resolveModelSettings(String modelKey) {
        String normalized = normalizeModelKey(modelKey);
        CarSettings settings = modelSettings.get(normalized);
        if (settings != null) {
            return settings;
        }
        settings = modelSettings.get(defaultModel);
        if (settings != null) {
            return settings;
        }
        return carSettings == null
            ? CarSettings.fromConfig(cfg())
            : carSettings;
    }

    private void saveLanguageDefaultRaw(String lang) {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.isFile()) {
            saveConfig();
            return;
        }
        try {
            List<String> lines = Files.readAllLines(
                configFile.toPath(),
                StandardCharsets.UTF_8
            );
            boolean inLanguageSection = false;
            boolean replaced = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String trimmed = line.trim();
                if (trimmed.equals("language:")) {
                    inLanguageSection = true;
                    continue;
                }
                if (!inLanguageSection) {
                    continue;
                }
                int indent = line.length() - line.stripLeading().length();
                if (indent == 0 && !trimmed.isEmpty()) {
                    break;
                }
                if (trimmed.startsWith("default:")) {
                    String prefix = line.substring(0, line.indexOf("default:"));
                    lines.set(i, prefix + "default: " + lang);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                saveConfig();
                return;
            }
            Files.write(configFile.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            getLogger().warning(
                "Failed to persist language.default in config.yml: " +
                    ex.getMessage()
            );
            saveConfig();
        }
    }

    private void pushActionBar(CarEntity car) {
        double speedKmh = car.getSpeedMetersPerSecond() * 3.6D;
        double speedMps = car.getSpeedMetersPerSecond();
        int occupied = car.getOccupiedSeatCount();
        int total = car.getSeatCount();
        int passengerTotal = Math.max(0, total - 1);
        int passengerOccupied = Math.max(
            0,
            occupied - (car.getDriver() == null ? 0 : 1)
        );
        double hp = car.getHealth();
        double hpMax = car.getMaxHealth();
        double hpPercent = car.getHealthPercent();
        double fuel = car.getFuelLiters();
        double fuelMax = car.getFuelTankCapacity();
        double fuelPercent = car.getFuelPercent();
        double frontPct = car.getFrontHealthPercent();
        double rearPct = car.getRearHealthPercent();
        double wheelFlPct = car.getPartHealthPercent(
            CarEntity.DamagePart.WHEEL_FL
        );
        double wheelFrPct = car.getPartHealthPercent(
            CarEntity.DamagePart.WHEEL_FR
        );
        double wheelRlPct = car.getPartHealthPercent(
            CarEntity.DamagePart.WHEEL_RL
        );
        double wheelRrPct = car.getPartHealthPercent(
            CarEntity.DamagePart.WHEEL_RR
        );

        String hpColor =
            hpPercent > 0.6D ? "&a" : hpPercent > 0.3D ? "&e" : "&c";
        String fuelColor =
            fuelPercent > 0.45D ? "&a" : fuelPercent > 0.20D ? "&6" : "&c";
        String seatsColor = passengerOccupied >= passengerTotal ? "&c" : "&a";
        String damageColor =
            minDamagePercent(
                frontPct,
                rearPct,
                wheelFlPct,
                wheelFrPct,
                wheelRlPct,
                wheelRrPct
            ) >
            0.5D
                ? "&a"
                : minDamagePercent(
                      frontPct,
                      rearPct,
                      wheelFlPct,
                      wheelFrPct,
                      wheelRlPct,
                      wheelRrPct
                  ) >
                  0.25D
                    ? "&e"
                    : "&c";

        for (Player mounted : car.getMountedPlayers()) {
            String modelName = getCarModelName(car);
            String damageBlock = "";
            if (car.isAdvancedDamageEnabled()) {
                damageBlock =
                    "&8| &7" +
                    i18n.tr(mounted, "ui.damage-short") +
                    " " +
                    damageColor +
                    "F" +
                    toPercentInt(frontPct) +
                    " R" +
                    toPercentInt(rearPct) +
                    " W[" +
                    toPercentInt(wheelFlPct) +
                    "/" +
                    toPercentInt(wheelFrPct) +
                    "/" +
                    toPercentInt(wheelRlPct) +
                    "/" +
                    toPercentInt(wheelRrPct) +
                    "]";
            }

            Map<String, String> values = new HashMap<>();
            values.put("model", modelName);
            values.put("model_key", getCarModelKey(car));
            values.put("speed_label", i18n.tr(mounted, "ui.speed"));
            values.put("passengers_label", i18n.tr(mounted, "ui.passengers"));
            values.put("hp_label", i18n.tr(mounted, "ui.hp"));
            values.put("fuel_label", i18n.tr(mounted, "ui.fuel"));
            values.put("damage_label", i18n.tr(mounted, "ui.damage-short"));
            values.put("speed_kmh", format1(speedKmh));
            values.put("speed_mps", format2(speedMps));
            values.put("passengers", Integer.toString(passengerOccupied));
            values.put("passengers_max", Integer.toString(passengerTotal));
            values.put("occupied_seats", Integer.toString(occupied));
            values.put("total_seats", Integer.toString(total));
            values.put("hp", format0(hp));
            values.put("hp_max", format0(hpMax));
            values.put("hp_percent", Integer.toString(toPercentInt(hpPercent)));
            values.put("hp_bar", buildBar(hpPercent));
            values.put("fuel", format1(fuel));
            values.put("fuel_max", format1(fuelMax));
            values.put(
                "fuel_percent",
                Integer.toString(toPercentInt(fuelPercent))
            );
            values.put("fuel_bar", buildBar(fuelPercent));
            values.put("seats_color", seatsColor);
            values.put("hp_color", hpColor);
            values.put("fuel_color", fuelColor);
            values.put("damage_color", damageColor);
            values.put("damage_block", damageBlock);
            values.put(
                "front_percent",
                Integer.toString(toPercentInt(frontPct))
            );
            values.put("rear_percent", Integer.toString(toPercentInt(rearPct)));
            values.put(
                "wheel_fl_percent",
                Integer.toString(toPercentInt(wheelFlPct))
            );
            values.put(
                "wheel_fr_percent",
                Integer.toString(toPercentInt(wheelFrPct))
            );
            values.put(
                "wheel_rl_percent",
                Integer.toString(toPercentInt(wheelRlPct))
            );
            values.put(
                "wheel_rr_percent",
                Integer.toString(toPercentInt(wheelRrPct))
            );
            values.put("engine", car.isEngineRunning() ? "on" : "off");
            values.put("lights", car.isHeadlightsOn() ? "on" : "off");

            sendFormattedActionBar(
                mounted,
                carActionBarFormat,
                carActionBarFormatLangKey,
                "actionbar.car",
                values
            );
        }
    }

    private boolean handleFuelCommand(Player player, String[] args) {
        if (args.length == 0) {
            msg(player, "command.usage.fuel");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("give")) {
            String lang = i18n.resolvePlayerLang(player);
            double liters =
                args.length >= 2
                    ? parseDouble(args[1], canisterMaxLiters)
                    : canisterMaxLiters;
            liters = clamp(liters, 0.0D, canisterMaxLiters);
            ItemStack canister = createCanister(liters, lang);
            player.getInventory().addItem(canister);
            msg(player, "fuel.canister.given", format1(liters));
            return true;
        }

        if (sub.equals("set")) {
            String lang = i18n.resolvePlayerLang(player);
            if (args.length < 2) {
                msg(player, "command.usage.fuel");
                return true;
            }
            double liters = clamp(
                parseDouble(args[1], 0.0D),
                0.0D,
                canisterMaxLiters
            );
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (!isFuelCanister(inHand)) {
                inHand = createCanister(0.0D, lang);
                player.getInventory().setItemInMainHand(inHand);
            }
            setCanisterLiters(inHand, liters, lang);
            msg(player, "fuel.canister.set", format1(liters));
            return true;
        }

        msg(player, "command.usage.fuel");
        return true;
    }

    private boolean handleRepairCommand(Player player, String[] args) {
        if (args.length == 0) {
            msg(player, "command.usage.repair");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("give")) {
            String lang = i18n.resolvePlayerLang(player);
            double units =
                args.length >= 2
                    ? parseDouble(args[1], repairKitMaxUnits)
                    : repairKitMaxUnits;
            units = clamp(units, 0.0D, repairKitMaxUnits);
            ItemStack kit = createRepairKit(units, lang);
            player.getInventory().addItem(kit);
            msg(player, "repair.kit.given", format1(units));
            return true;
        }
        if (sub.equals("set")) {
            if (args.length < 2) {
                msg(player, "command.usage.repair");
                return true;
            }
            String lang = i18n.resolvePlayerLang(player);
            double units = clamp(
                parseDouble(args[1], 0.0D),
                0.0D,
                repairKitMaxUnits
            );
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (!isRepairKit(inHand)) {
                inHand = createRepairKit(0.0D, lang);
                player.getInventory().setItemInMainHand(inHand);
            }
            setRepairKitUnits(inHand, units, lang);
            msg(player, "repair.kit.set", format1(units));
            return true;
        }

        msg(player, "command.usage.repair");
        return true;
    }

    private boolean handleCarCommand(Player player, String[] args) {
        if (args.length == 0) {
            sendCarsHelp(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("help")) {
            sendCarsHelp(player);
            return true;
        }
        if (sub.equals("spawn")) {
            if (args.length < 2) {
                msg(player, "command.usage.car");
                return true;
            }
            return spawnCarByModel(player, args[1]);
        }
        if (sub.equals("tp")) {
            return handleCarTeleportCommand(player, args);
        }
        if (sub.equals("repairhitbox")) {
            if (!player.isOp()) {
                msg(player, "command.no-permission");
                return true;
            }
            if (args.length < 2) {
                toggleRepairHitboxPreview(player);
                return true;
            }
            String mode = args[1].toLowerCase(Locale.ROOT);
            if (mode.equals("on") || mode.equals("enable")) {
                enableRepairHitboxPreview(player);
                return true;
            }
            if (mode.equals("off") || mode.equals("disable")) {
                disableRepairHitboxPreview(player, true);
                return true;
            }
            msg(player, "command.usage.car");
            return true;
        }
        if (sub.equals("list")) {
            List<CarEntity> visibleCars = cars
                .values()
                .stream()
                .filter(car -> car != null && car.isValid())
                .collect(Collectors.toList());
            if (visibleCars.isEmpty()) {
                msg(player, "car.none");
                return true;
            }
            msg(player, "car.list.header", visibleCars.size());
            for (CarEntity car : visibleCars) {
                Location l = car.getSafeLocation();
                if (l == null) {
                    msg(
                        player,
                        "car.list.entry",
                        car.getVehicleId(),
                        modelDisplayNames.getOrDefault(
                            carModelById.getOrDefault(
                                car.getVehicleId(),
                                defaultModel
                            ),
                            carModelById.getOrDefault(
                                car.getVehicleId(),
                                defaultModel
                            )
                        ),
                        "unknown",
                        "?",
                        "?",
                        "?",
                        format0(car.getHealth()),
                        format0(car.getMaxHealth()),
                        format1(car.getFuelLiters()),
                        format1(car.getFuelTankCapacity())
                    );
                    continue;
                }
                msg(
                    player,
                    "car.list.entry",
                    car.getVehicleId(),
                    modelDisplayNames.getOrDefault(
                        carModelById.getOrDefault(
                            car.getVehicleId(),
                            defaultModel
                        ),
                        carModelById.getOrDefault(
                            car.getVehicleId(),
                            defaultModel
                        )
                    ),
                    l.getWorld() == null ? "unknown" : l.getWorld().getName(),
                    format1(l.getX()),
                    format1(l.getY()),
                    format1(l.getZ()),
                    format0(car.getHealth()),
                    format0(car.getMaxHealth()),
                    format1(car.getFuelLiters()),
                    format1(car.getFuelTankCapacity())
                );
            }
            return true;
        }
        if (sub.equals("info")) {
            if (!player.isOp()) {
                msg(player, "command.no-permission");
                return true;
            }
            if (args.length < 2) {
                msg(player, "command.usage.car");
                return true;
            }
            UUID id;
            try {
                id = UUID.fromString(args[1]);
            } catch (IllegalArgumentException ex) {
                msg(player, "car.remove.invalid-uuid");
                return true;
            }
            CarEntity car = cars.get(id);
            if (car == null || !car.isValid()) {
                msg(player, "car.info.not-found");
                return true;
            }
            sendCarInfo(player, car);
            return true;
        }
        if (sub.equals("remove")) {
            if (args.length < 2) {
                msg(player, "command.usage.car");
                return true;
            }
            UUID id;
            try {
                id = UUID.fromString(args[1]);
            } catch (IllegalArgumentException ex) {
                msg(player, "car.remove.invalid-uuid");
                return true;
            }
            CarEntity car = cars.remove(id);
            if (car == null) {
                msg(player, "car.remove.not-found");
                return true;
            }
            for (Player mounted : car.getMountedPlayers()) {
                UUID mountedId = mounted.getUniqueId();
                driverToCar.remove(mountedId);
                playerToCarCache.remove(mountedId);
                stopRefuelTask(mountedId);
                stopRepairTask(mountedId);
            }
            unregisterCarPartMappings(car);
            noteCarEvent(car, "removed", "admin-command");
            upsertTelemetryForCar(
                car,
                carOwners.get(id),
                carOwnerNames.get(id)
            );
            car.destroy();
            carOwners.remove(id);
            carOwnerNames.remove(id);
            carModelById.remove(id);
            carSpawnedAtMs.remove(id);
            carLastUsedAtMs.remove(id);
            carLastEventTypeById.remove(id);
            carLastEventDetailsById.remove(id);
            carLastEventAtMs.remove(id);
            carLastEventFlushedAtMs.remove(id);
            clearPlayerCacheForCar(id);
            clearCarChunkTickets(id);
            clearVehicleSoundState(id);
            if (ownershipDatabase != null) {
                ownershipDatabase.deleteOwnership(id);
            }
            msg(player, "car.remove.done", id);
            return true;
        }
        msg(player, "command.usage.car");
        return true;
    }

    private boolean handleCarKeyCommand(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            msg(sender, "car.key.no-permission");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("give")) {
            msg(sender, "car.key.usage");
            return true;
        }
        if (args.length < 4) {
            msg(sender, "car.key.usage");
            return true;
        }

        Player target = findOnlinePlayerByToken(args[2]);
        if (target == null) {
            msg(sender, "car.key.player-not-found", args[2]);
            return true;
        }

        UUID vehicleId;
        try {
            vehicleId = UUID.fromString(args[3]);
        } catch (IllegalArgumentException ex) {
            msg(sender, "car.key.invalid-uuid", args[3]);
            return true;
        }
        CarEntity car = cars.get(vehicleId);
        if (car == null || !car.isValid()) {
            msg(sender, "car.key.car-not-found", vehicleId);
            return true;
        }

        UUID ownerId = getCarOwnerId(car);
        String ownerName = getCarOwnerName(car);
        String modelName = getCarModelName(car);
        ItemStack key = createCarKey(
            car,
            ownerId,
            ownerName,
            modelName,
            i18n.resolvePlayerLang(target)
        );
        Map<Integer, ItemStack> notAdded = target.getInventory().addItem(key);
        if (!notAdded.isEmpty()) {
            for (ItemStack overflow : notAdded.values()) {
                target
                    .getWorld()
                    .dropItemNaturally(target.getLocation(), overflow);
            }
        }

        msg(sender, "car.key.give.done", target.getName(), vehicleId);
        msg(target, "car.key.received", modelName, vehicleId);
        if (!keysEnabled) {
            msg(sender, "car.key.system-disabled");
        }
        return true;
    }

    private boolean handleCarHealCommand(CommandSender sender, String[] args) {
        if (!healCommandEnabled) {
            msg(sender, "car.heal.disabled");
            return true;
        }
        if (!sender.isOp()) {
            msg(sender, "car.heal.no-permission");
            return true;
        }
        if (!healCommandConsoleAllowed && !(sender instanceof Player)) {
            msg(sender, "car.heal.console-disabled");
            return true;
        }
        if (args.length < 4) {
            msg(sender, "car.heal.usage");
            return true;
        }

        UUID vehicleId;
        try {
            vehicleId = UUID.fromString(args[1]);
        } catch (IllegalArgumentException ex) {
            msg(sender, "car.heal.invalid-uuid", args[1]);
            return true;
        }
        CarEntity car = cars.get(vehicleId);
        if (car == null || !car.isValid()) {
            msg(sender, "car.heal.car-not-found", vehicleId);
            return true;
        }

        String target = args[2].toLowerCase(Locale.ROOT);
        String amountInput = args[3];
        boolean healFull =
            amountInput.equalsIgnoreCase("full") ||
            amountInput.equalsIgnoreCase("max");
        double amount = 0.0D;
        if (!healFull) {
            try {
                amount = Double.parseDouble(amountInput);
            } catch (NumberFormatException ex) {
                msg(sender, "car.heal.invalid-amount", amountInput);
                return true;
            }
            if (amount <= 0.0D || !Double.isFinite(amount)) {
                msg(sender, "car.heal.invalid-amount", amountInput);
                return true;
            }
        }
        double effectiveAmount = healFull ? Double.MAX_VALUE : amount;

        boolean changed;
        switch (target) {
            case "car" -> changed = car.repairCarHealth(effectiveAmount);
            case "front" -> changed = car.repairPart(
                CarEntity.DamagePart.FRONT,
                effectiveAmount
            );
            case "rear" -> changed = car.repairPart(
                CarEntity.DamagePart.REAR,
                effectiveAmount
            );
            case "wheel_fl" -> changed = car.repairPart(
                CarEntity.DamagePart.WHEEL_FL,
                effectiveAmount
            );
            case "wheel_fr" -> changed = car.repairPart(
                CarEntity.DamagePart.WHEEL_FR,
                effectiveAmount
            );
            case "wheel_rl" -> changed = car.repairPart(
                CarEntity.DamagePart.WHEEL_RL,
                effectiveAmount
            );
            case "wheel_rr" -> changed = car.repairPart(
                CarEntity.DamagePart.WHEEL_RR,
                effectiveAmount
            );
            case "wheels" -> changed =
                car.repairPart(
                    CarEntity.DamagePart.WHEEL_FL,
                    effectiveAmount
                ) ||
                car.repairPart(
                    CarEntity.DamagePart.WHEEL_FR,
                    effectiveAmount
                ) ||
                car.repairPart(
                    CarEntity.DamagePart.WHEEL_RL,
                    effectiveAmount
                ) ||
                car.repairPart(CarEntity.DamagePart.WHEEL_RR, effectiveAmount);
            case "all" -> changed = car.repairAllHealth(effectiveAmount);
            default -> {
                msg(sender, "car.heal.bad-target", target);
                return true;
            }
        }

        if (!changed) {
            msg(sender, "car.heal.no-change", vehicleId, target);
            return true;
        }
        String amountLabel = healFull ? "full" : format1(amount);
        msg(sender, "car.heal.done", vehicleId, target, amountLabel);
        return true;
    }

    private boolean spawnCarByModel(Player player, String modelInput) {
        String modelKey = normalizeModelKey(modelInput);
        CarSettings selectedSettings = modelSettings.get(modelKey);
        if (selectedSettings == null) {
            msg(player, "spawn.unknown-model", modelInput);
            msg(
                player,
                "spawn.available-models",
                String.join(", ", modelSettings.keySet())
            );
            return true;
        }
        Location playerLoc = player.getLocation();
        Location spawnAt = playerLoc
            .clone()
            .add(forwardFromYaw(playerLoc.getYaw()).multiply(2.5D))
            .add(0.0D, 0.25D, 0.0D);
        spawnAt.setYaw(playerLoc.getYaw());
        spawnAt.setPitch(0.0F);
        Location safeSpawn = resolveSafeSpawnLocation(
            spawnAt,
            selectedSettings
        );
        if (safeSpawn == null) {
            msg(player, "car.spawn.no-space");
            return true;
        }

        CarEntity car = new CarEntity(
            safeSpawn,
            createBodyModel(selectedSettings),
            createWheelModels(selectedSettings),
            selectedSettings
        );
        car.setEngineRunning(false);
        if (!car.mountDriver(player)) {
            car.destroy();
            msg(player, "spawn.mount-failed");
            return true;
        }
        cars.put(car.getVehicleId(), car);
        registerCarPartMappings(car);
        carModelById.put(car.getVehicleId(), modelKey);
        carSpawnedAtMs.putIfAbsent(
            car.getVehicleId(),
            System.currentTimeMillis()
        );
        carLastUsedAtMs.put(car.getVehicleId(), System.currentTimeMillis());
        noteCarEvent(car, "spawned", "by-command");
        driverToCar.put(player.getUniqueId(), car.getVehicleId());
        cachePlayerCar(player.getUniqueId(), car.getVehicleId());
        setCarOwner(car.getVehicleId(), player.getUniqueId(), player.getName());
        upsertOwnershipForCar(car);
        playSoundAtCar(car, soundSeatOpen);

        msg(
            player,
            "spawn.success",
            modelDisplayNames.getOrDefault(modelKey, modelKey)
        );
        msg(player, "car.engine.off");
        return true;
    }

    private boolean handleCarTeleportCommand(Player player, String[] args) {
        if (!player.isOp()) {
            msg(player, "command.no-permission");
            return true;
        }
        if (args.length < 3) {
            msg(player, "car.tp.usage");
            return true;
        }

        UUID vehicleId;
        try {
            vehicleId = UUID.fromString(args[1]);
        } catch (IllegalArgumentException ex) {
            msg(player, "car.remove.invalid-uuid");
            return true;
        }

        CarEntity car = cars.get(vehicleId);
        if (car == null || !car.isValid()) {
            msg(player, "car.info.not-found");
            return true;
        }

        Player targetPlayer = findOnlinePlayerByToken(args[2]);
        if (targetPlayer == null) {
            msg(player, "car.tp.player-not-found", args[2]);
            return true;
        }

        Location targetLoc = targetPlayer.getLocation().clone();
        if (targetLoc.getWorld() == null) {
            msg(player, "car.tp.target-world-invalid", targetPlayer.getName());
            return true;
        }
        Location carTarget = targetLoc
            .clone()
            .add(forwardFromYaw(targetLoc.getYaw()).multiply(2.75D))
            .add(0.0D, 0.25D, 0.0D);
        carTarget.setYaw(targetLoc.getYaw());
        carTarget.setPitch(0.0F);
        String modelKeyForTarget = carModelById.getOrDefault(
            vehicleId,
            defaultModel
        );
        CarSettings targetSettings = resolveModelSettings(modelKeyForTarget);
        Location safeTarget = resolveSafeSpawnLocation(
            carTarget,
            targetSettings
        );
        if (safeTarget == null) {
            msg(player, "car.tp.failed", vehicleId);
            return true;
        }
        carTarget = safeTarget;

        Map<UUID, Integer> mountedSeatSnapshot =
            car.getMountedPlayerSeatIndices();
        for (UUID passengerId : mountedSeatSnapshot.keySet()) {
            car.unmountPlayer(passengerId);
            clearPlayerControlState(passengerId);
        }

        CarEntity teleportedCar = car;
        Location currentLoc = car.getSafeLocation();
        boolean crossWorld =
            currentLoc != null &&
            currentLoc.getWorld() != null &&
            !currentLoc
                .getWorld()
                .getUID()
                .equals(carTarget.getWorld().getUID());

        if (crossWorld) {
            String modelKey = modelKeyForTarget;
            CarSettings settings = targetSettings;
            double health = car.getHealth();
            double fuel = car.getFuelLiters();
            boolean engineRunning = car.isEngineRunning();
            boolean headlightsOn = car.isHeadlightsOn();
            ItemStack[] trunkContents = car.getTrunkContentsCopy();
            Map<String, Double> damageSnapshot = car.getDamageHealthSnapshot();

            unregisterCarPartMappings(car);
            clearVehicleSoundState(vehicleId);
            clearCarChunkTickets(vehicleId);
            car.destroy();

            teleportedCar = new CarEntity(
                vehicleId,
                carTarget.clone(),
                createBodyModel(settings),
                createWheelModels(settings),
                settings,
                health,
                fuel,
                trunkContents,
                engineRunning,
                headlightsOn
            );
            teleportedCar.applyDamageHealthSnapshot(damageSnapshot);
            cars.put(vehicleId, teleportedCar);
            registerCarPartMappings(teleportedCar);
            carModelById.put(vehicleId, modelKey);
        } else {
            if (!car.teleportVehicle(carTarget, true)) {
                msg(player, "car.tp.failed", vehicleId);
                return true;
            }
        }

        int remounted = 0;
        List<Map.Entry<UUID, Integer>> passengers = new ArrayList<>(
            mountedSeatSnapshot.entrySet()
        );
        passengers.sort(Comparator.comparingInt(Map.Entry::getValue));
        for (Map.Entry<UUID, Integer> entry : passengers) {
            Player mounted = Bukkit.getPlayer(entry.getKey());
            if (mounted == null || !mounted.isOnline()) {
                continue;
            }
            mounted.teleport(carTarget.clone().add(0.0D, 0.8D, 0.0D));
            boolean mountedBack = teleportedCar.mountSeat(
                mounted,
                entry.getValue()
            );
            if (!mountedBack) {
                int freeSeat = teleportedCar.firstFreeSeatIndex();
                mountedBack =
                    freeSeat >= 0 && teleportedCar.mountSeat(mounted, freeSeat);
            }
            if (mountedBack) {
                driverToCar.put(mounted.getUniqueId(), vehicleId);
                cachePlayerCar(mounted.getUniqueId(), vehicleId);
                remounted++;
            } else {
                clearPlayerControlState(mounted.getUniqueId());
            }
        }

        msg(
            player,
            "car.tp.done",
            vehicleId,
            targetPlayer.getName(),
            remounted,
            mountedSeatSnapshot.size()
        );
        return true;
    }

    private Location resolveSafeSpawnLocation(
        Location desired,
        CarSettings settings
    ) {
        if (desired == null || desired.getWorld() == null || settings == null) {
            return null;
        }
        Location base = desired.clone();
        base.setPitch(0.0F);
        Vector forward = forwardFromYaw(base.getYaw());
        Vector right = new Vector(
            forward.getZ(),
            0.0D,
            -forward.getX()
        ).normalize();

        double[] radial = new double[] { 0.0D, 0.8D, 1.6D, 2.4D };
        int[] vertical = new int[] { 0, 1, 2, 3, -1 };
        Vector[] directions = new Vector[] {
            new Vector(0.0D, 0.0D, 0.0D),
            forward.clone(),
            forward.clone().multiply(-1.0D),
            right.clone(),
            right.clone().multiply(-1.0D),
            forward.clone().add(right).normalize(),
            forward.clone().add(right.clone().multiply(-1.0D)).normalize(),
            forward.clone().multiply(-1.0D).add(right).normalize(),
            forward
                .clone()
                .multiply(-1.0D)
                .add(right.clone().multiply(-1.0D))
                .normalize(),
        };

        for (int dy : vertical) {
            for (double r : radial) {
                for (Vector dir : directions) {
                    Location candidate = base
                        .clone()
                        .add(dir.clone().multiply(r))
                        .add(0.0D, dy * 0.5D, 0.0D);
                    candidate.setYaw(base.getYaw());
                    candidate.setPitch(0.0F);
                    if (isSpawnAreaClear(candidate, settings)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private boolean isSpawnAreaClear(Location at, CarSettings settings) {
        if (at == null || at.getWorld() == null || settings == null) {
            return false;
        }
        World world = at.getWorld();
        double x = at.getX();
        double y = at.getY();
        double z = at.getZ();
        double baseY = y + settings.collisionBaseY;
        double halfW = Math.max(0.35D, settings.carHalfWidth - 0.03D);
        double halfL = Math.max(0.35D, settings.carHalfLength - 0.03D);
        double[] probeX = new double[] { -halfW, 0.0D, halfW };
        double[] probeZ = new double[] { -halfL, 0.0D, halfL };
        double[] yLevels = new double[] {
            baseY + 0.02D,
            baseY + settings.carHeight * 0.50D,
            baseY + settings.carHeight - 0.02D,
        };
        for (double ox : probeX) {
            for (double oz : probeZ) {
                Vector rotated = rotateYaw(
                    new Vector(ox, 0.0D, oz),
                    at.getYaw()
                );
                double px = x + rotated.getX();
                double pz = z + rotated.getZ();
                for (double py : yLevels) {
                    if (isSolidCollisionAt(world, px, py, pz)) {
                        return false;
                    }
                }
            }
        }
        return hasSpawnSupport(world, at, settings);
    }

    private boolean hasSpawnSupport(
        World world,
        Location at,
        CarSettings settings
    ) {
        double baseY = at.getY() + settings.collisionBaseY;
        double halfW = Math.max(0.35D, settings.carHalfWidth - 0.03D);
        double halfL = Math.max(0.35D, settings.carHalfLength - 0.03D);
        Vector[] probes = new Vector[] {
            new Vector(-halfW, 0.0D, -halfL),
            new Vector(halfW, 0.0D, -halfL),
            new Vector(-halfW, 0.0D, halfL),
            new Vector(halfW, 0.0D, halfL),
            new Vector(0.0D, 0.0D, 0.0D),
        };
        int supported = 0;
        for (Vector local : probes) {
            Vector rotated = rotateYaw(local, at.getYaw());
            double px = at.getX() + rotated.getX();
            double pz = at.getZ() + rotated.getZ();
            boolean hitSupport = false;
            for (double d = 0.10D; d <= 1.40D; d += 0.10D) {
                if (isSolidCollisionAt(world, px, baseY - d, pz)) {
                    hitSupport = true;
                    break;
                }
            }
            if (hitSupport) {
                supported++;
            }
        }
        return supported >= 2;
    }

    private boolean isSolidCollisionAt(
        World world,
        double x,
        double y,
        double z
    ) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        org.bukkit.block.Block block = world.getBlockAt(bx, by, bz);
        if (block.isPassable()) {
            return false;
        }
        Collection<BoundingBox> boxes = block
            .getCollisionShape()
            .getBoundingBoxes();
        if (boxes.isEmpty()) {
            return true;
        }
        double eps = 0.0001D;
        for (BoundingBox box : boxes) {
            if (box == null) {
                continue;
            }
            double minX = box.getMinX();
            double minY = box.getMinY();
            double minZ = box.getMinZ();
            double maxX = box.getMaxX();
            double maxY = box.getMaxY();
            double maxZ = box.getMaxZ();
            boolean localShape =
                minX >= -eps &&
                minY >= -eps &&
                minZ >= -eps &&
                maxX <= 1.0D + eps &&
                maxY <= 1.0D + eps &&
                maxZ <= 1.0D + eps;
            if (localShape) {
                minX += bx;
                minY += by;
                minZ += bz;
                maxX += bx;
                maxY += by;
                maxZ += bz;
            }
            if (
                x >= minX - eps &&
                x <= maxX + eps &&
                y >= minY - eps &&
                y <= maxY + eps &&
                z >= minZ - eps &&
                z <= maxZ + eps
            ) {
                return true;
            }
        }
        return false;
    }

    private static Vector rotateYaw(Vector local, float yawDegrees) {
        if (local == null) {
            return new Vector();
        }
        double rad = Math.toRadians(yawDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = local.getX();
        double z = local.getZ();
        return new Vector(
            (x * cos) - (z * sin),
            local.getY(),
            (x * sin) + (z * cos)
        );
    }

    private void sendCarInfo(Player player, CarEntity car) {
        UUID id = car.getVehicleId();
        String modelKey = getCarModelKey(car);
        String modelName = getCarModelName(car);
        UUID ownerId = getCarOwnerId(car);
        String ownerName = getCarOwnerName(car);
        Location loc = car.getSafeLocation();
        String world =
            loc == null || loc.getWorld() == null
                ? "unknown"
                : loc.getWorld().getName();
        String xyz =
            loc == null
                ? "?, ?, ?"
                : format1(loc.getX()) +
                  ", " +
                  format1(loc.getY()) +
                  ", " +
                  format1(loc.getZ());

        msg(player, "car.info.header");
        msg(player, "car.info.id", id);
        msg(player, "car.info.model", modelName, modelKey);
        msg(
            player,
            "car.info.owner",
            ownerName == null || ownerName.isBlank() ? "none" : ownerName,
            ownerId == null ? "none" : ownerId.toString()
        );
        msg(player, "car.info.location", world, xyz);
        msg(
            player,
            "car.info.state",
            format0(car.getHealth()),
            format0(car.getMaxHealth()),
            format1(car.getFuelLiters()),
            format1(car.getFuelTankCapacity()),
            car.isEngineRunning() ? "on" : "off",
            car.isHeadlightsOn() ? "on" : "off"
        );
    }

    private void toggleRepairHitboxPreview(Player player) {
        if (repairHitboxPreviewTasks.containsKey(player.getUniqueId())) {
            disableRepairHitboxPreview(player, true);
            return;
        }
        enableRepairHitboxPreview(player);
    }

    private void enableRepairHitboxPreview(Player player) {
        UUID playerId = player.getUniqueId();
        if (repairHitboxPreviewTasks.containsKey(playerId)) {
            msg(player, "car.repair-hitbox.already-on");
            return;
        }
        BukkitTask task = getServer()
            .getScheduler()
            .runTaskTimer(
                this,
                () -> {
                    Player online = Bukkit.getPlayer(playerId);
                    if (online == null || !online.isOnline()) {
                        stopRepairHitboxPreview(playerId);
                        return;
                    }
                    renderRepairHitboxPreview(online);
                },
                1L,
                8L
            );
        repairHitboxPreviewTasks.put(playerId, task);
        msg(player, "car.repair-hitbox.enabled");
    }

    private void disableRepairHitboxPreview(Player player, boolean notify) {
        if (stopRepairHitboxPreview(player.getUniqueId()) && notify) {
            msg(player, "car.repair-hitbox.disabled");
            return;
        }
        if (notify) {
            msg(player, "car.repair-hitbox.already-off");
        }
    }

    private boolean stopRepairHitboxPreview(UUID playerId) {
        BukkitTask task = repairHitboxPreviewTasks.remove(playerId);
        if (task == null) {
            return false;
        }
        task.cancel();
        return true;
    }

    private void sendCarsHelp(Player player) {
        String prefix = i18n.tr(player, "prefix");
        player.sendMessage(i18n.tr(player, "help.cars.title", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.section.core", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.core.spawn", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.core.menu", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.core.list", prefix));

        player.sendMessage(i18n.tr(player, "help.cars.section.fuel", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.fuel.give", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.fuel.set", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.fuel.use", prefix));

        player.sendMessage(i18n.tr(player, "help.cars.section.repair", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.repair.give", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.repair.set", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.repair.use", prefix));

        player.sendMessage(i18n.tr(player, "help.cars.section.lang", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.lang.self", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.lang.auto", prefix));
        player.sendMessage(i18n.tr(player, "help.cars.lang.server", prefix));

        if (player.isOp()) {
            player.sendMessage(
                i18n.tr(player, "help.cars.section.admin", prefix)
            );
            player.sendMessage(
                i18n.tr(player, "help.cars.admin.remove", prefix)
            );
            player.sendMessage(i18n.tr(player, "help.cars.admin.info", prefix));
            player.sendMessage(
                i18n.tr(player, "help.cars.admin.reload", prefix)
            );
            player.sendMessage(
                i18n.tr(player, "help.cars.admin.repair-hitbox", prefix)
            );
            player.sendMessage(i18n.tr(player, "car.help.admin.tp", prefix));
            player.sendMessage(i18n.tr(player, "car.help.admin.key", prefix));
            player.sendMessage(i18n.tr(player, "car.help.admin.heal", prefix));
        }
        player.sendMessage(i18n.tr(player, "help.cars.footer", prefix));
    }

    private boolean handleLangCommand(Player player, String[] args) {
        if (args.length == 0) {
            msg(player, "lang.current", i18n.resolvePlayerLang(player));
            msg(player, "command.usage.lang");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("auto")) {
            if (i18n.isGlobalMode()) {
                msg(player, "lang.global.locked");
                return true;
            }
            i18n.clearPlayerLanguage(player);
            msg(player, "lang.set.auto");
            return true;
        }
        if (sub.equals("set")) {
            if (args.length < 2) {
                msg(player, "command.usage.lang");
                return true;
            }
            if (i18n.isGlobalMode()) {
                msg(player, "lang.global.locked");
                return true;
            }
            String lang = i18n.normalizeLang(args[1]);
            i18n.setPlayerLanguage(player, lang);
            msg(player, "lang.set.done", lang);
            return true;
        }
        if (sub.equals("server")) {
            if (!player.isOp()) {
                msg(player, "command.no-permission");
                return true;
            }
            if (args.length < 2) {
                msg(player, "command.usage.lang");
                return true;
            }
            String lang = i18n.normalizeLang(args[1]);
            getConfig().set("language.default", lang);
            saveLanguageDefaultRaw(lang);
            reloadPluginConfig();
            msg(player, "lang.server.set", lang);
            return true;
        }
        msg(player, "command.usage.lang");
        return true;
    }

    private void startRefuel(Player player, CarEntity car) {
        if (refuelTasks.containsKey(player.getUniqueId())) {
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isFuelCanister(hand)) {
            msg(player, "fuel.need-canister");
            return;
        }

        double canisterLiters = getCanisterLiters(hand);
        if (canisterLiters <= 0.0001D) {
            msg(player, "fuel.canister-empty");
            return;
        }
        if (car.getFuelLiters() >= car.getFuelTankCapacity() - 0.0001D) {
            msg(player, "fuel.tank-full");
            return;
        }

        final double startCarFuel = car.getFuelLiters();
        final double startCanisterFuel = canisterLiters;
        final double maxTransferable = Math.min(
            startCanisterFuel,
            Math.max(0.0D, car.getFuelTankCapacity() - startCarFuel)
        );

        msg(player, "fuel.started");
        refuelCarByPlayer.put(player.getUniqueId(), car.getVehicleId());
        playSoundAtCar(car, soundFilling);
        BukkitTask task = getServer()
            .getScheduler()
            .runTaskTimer(
                this,
                new Runnable() {
                    private int t = 0;

                    @Override
                    public void run() {
                        if (!player.isOnline() || !car.isValid()) {
                            stopRefuelTask(player.getUniqueId());
                            return;
                        }
                        if (!player.isSneaking()) {
                            msg(player, "fuel.stopped-release");
                            stopRefuelTask(player.getUniqueId());
                            return;
                        }
                        if (
                            player
                                .getLocation()
                                .distanceSquared(car.getFuelPointLocation()) >
                            9.0D
                        ) {
                            msg(player, "fuel.too-far");
                            stopRefuelTask(player.getUniqueId());
                            return;
                        }

                        ItemStack current = player
                            .getInventory()
                            .getItemInMainHand();
                        if (!isFuelCanister(current)) {
                            msg(player, "fuel.stopped-no-can");
                            stopRefuelTask(player.getUniqueId());
                            return;
                        }

                        double litersInCanister = getCanisterLiters(current);
                        double freeTank = Math.max(
                            0.0D,
                            car.getFuelTankCapacity() - car.getFuelLiters()
                        );
                        if (
                            litersInCanister <= 0.0001D || freeTank <= 0.0001D
                        ) {
                            msg(player, "fuel.complete");
                            stopRefuelTask(player.getUniqueId());
                            return;
                        }

                        double transfer = Math.min(
                            car.getRefuelRateLitersPerTick(),
                            Math.min(litersInCanister, freeTank)
                        );
                        double added = car.addFuel(transfer);
                        setCanisterLiters(
                            current,
                            litersInCanister - added,
                            i18n.resolvePlayerLang(player)
                        );

                        double transferred = Math.max(
                            0.0D,
                            car.getFuelLiters() - startCarFuel
                        );
                        double sessionProgress =
                            maxTransferable <= 0.0001D
                                ? 1.0D
                                : transferred / maxTransferable;
                        double canLeft = Math.max(
                            0.0D,
                            startCanisterFuel - transferred
                        );

                        t++;
                        if (
                            refuelActionBarEnabled &&
                            (t % Math.max(1, refuelActionBarUpdateTicks) == 0)
                        ) {
                            Map<String, String> values = new HashMap<>();
                            values.put("model", getCarModelName(car));
                            values.put("model_key", getCarModelKey(car));
                            values.put(
                                "refuel_label",
                                i18n.tr(player, "ui.refuel")
                            );
                            values.put(
                                "tank_label",
                                i18n.tr(player, "ui.fuel")
                            );
                            values.put(
                                "can_label",
                                i18n.tr(player, "ui.can-short")
                            );
                            values.put(
                                "hold_shift",
                                i18n.tr(player, "ui.hold-shift")
                            );
                            values.put(
                                "progress_bar",
                                buildBar(sessionProgress)
                            );
                            values.put(
                                "progress_percent",
                                Integer.toString(toPercentInt(sessionProgress))
                            );
                            values.put("tank", format1(car.getFuelLiters()));
                            values.put(
                                "tank_max",
                                format1(car.getFuelTankCapacity())
                            );
                            values.put("canister", format1(canLeft));
                            values.put(
                                "canister_max",
                                format1(canisterMaxLiters)
                            );
                            sendFormattedActionBar(
                                player,
                                refuelActionBarFormat,
                                refuelActionBarFormatLangKey,
                                "actionbar.refuel",
                                values
                            );
                        }

                        int particleInterval = Math.max(
                            1,
                            car.getRefuelSoundIntervalTicks()
                        );
                        if (t % particleInterval == 0) {
                            player
                                .getWorld()
                                .spawnParticle(
                                    Particle.SMOKE,
                                    car
                                        .getFuelPointLocation()
                                        .add(0.0D, 0.25D, 0.0D),
                                    4,
                                    0.06D,
                                    0.04D,
                                    0.06D,
                                    0.01D
                                );
                        }
                    }
                },
                1L,
                1L
            );

        refuelTasks.put(player.getUniqueId(), task);
    }

    private void stopRefuelTask(UUID playerId) {
        BukkitTask task = refuelTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        UUID vehicleId = refuelCarByPlayer.remove(playerId);
        CarEntity car = vehicleId == null ? null : cars.get(vehicleId);
        stopConfiguredSoundAtCar(car, soundFilling);
        stopConfiguredSoundForPlayer(playerId, soundFilling);
    }

    private void startRepair(
        Player player,
        CarEntity car,
        CarEntity.DamagePart part
    ) {
        if (repairTasks.containsKey(player.getUniqueId())) {
            return;
        }
        if (car == null || !car.isValid() || !car.isAdvancedDamageEnabled()) {
            msg(player, "repair.disabled");
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isRepairKit(hand)) {
            msg(player, "repair.need-kit");
            return;
        }
        double units = getRepairKitUnits(hand);
        if (units <= 0.0001D) {
            msg(player, "repair.kit-empty");
            return;
        }
        if (car.getPartHealthPercent(part) >= 0.999D) {
            msg(player, "repair.part-full", damagePartLabel(player, part));
            return;
        }

        msg(player, "repair.started", damagePartLabel(player, part));
        repairCarByPlayer.put(player.getUniqueId(), car.getVehicleId());
        playSoundAtCar(car, soundRepair);
        BukkitTask task = getServer()
            .getScheduler()
            .runTaskTimer(
                this,
                new Runnable() {
                    private int t = 0;

                    @Override
                    public void run() {
                        if (!player.isOnline() || !car.isValid()) {
                            stopRepairTask(player.getUniqueId());
                            return;
                        }
                        if (!player.isSneaking()) {
                            msg(player, "repair.stopped-release");
                            stopRepairTask(player.getUniqueId());
                            return;
                        }
                        Location partLoc = car.getDamagePartLocation(part);
                        if (
                            partLoc == null ||
                            player.getLocation().distanceSquared(partLoc) > 9.0D
                        ) {
                            msg(player, "repair.too-far");
                            stopRepairTask(player.getUniqueId());
                            return;
                        }
                        ItemStack current = player
                            .getInventory()
                            .getItemInMainHand();
                        if (!isRepairKit(current)) {
                            msg(player, "repair.stopped-no-kit");
                            stopRepairTask(player.getUniqueId());
                            return;
                        }

                        double currentUnits = getRepairKitUnits(current);
                        if (currentUnits <= 0.0001D) {
                            msg(player, "repair.kit-empty");
                            stopRepairTask(player.getUniqueId());
                            return;
                        }
                        double maxPart = car.getPartMaxHealth(part);
                        double curPart = car.getPartHealth(part);
                        double missing = Math.max(0.0D, maxPart - curPart);
                        if (missing <= 0.0001D) {
                            msg(
                                player,
                                "repair.complete",
                                damagePartLabel(player, part)
                            );
                            stopRepairTask(player.getUniqueId());
                            return;
                        }
                        double transfer = Math.min(
                            repairRateUnitsPerTick,
                            Math.min(currentUnits, missing)
                        );
                        boolean repaired = car.repairPart(part, transfer);
                        if (!repaired) {
                            msg(
                                player,
                                "repair.complete",
                                damagePartLabel(player, part)
                            );
                            stopRepairTask(player.getUniqueId());
                            return;
                        }
                        setRepairKitUnits(
                            current,
                            currentUnits - transfer,
                            i18n.resolvePlayerLang(player)
                        );
                        t++;
                        if (
                            repairActionBarEnabled &&
                            t % Math.max(1, repairActionBarUpdateTicks) == 0
                        ) {
                            double partPercent = car.getPartHealthPercent(part);
                            Map<String, String> values = new HashMap<>();
                            values.put("model", getCarModelName(car));
                            values.put("model_key", getCarModelKey(car));
                            values.put(
                                "repair_label",
                                i18n.tr(player, "ui.repair")
                            );
                            values.put("part", damagePartLabel(player, part));
                            values.put("part_bar", buildBar(partPercent));
                            values.put(
                                "part_percent",
                                Integer.toString(toPercentInt(partPercent))
                            );
                            values.put(
                                "kit_label",
                                i18n.tr(player, "ui.kit-short")
                            );
                            values.put(
                                "kit",
                                format1(getRepairKitUnits(current))
                            );
                            values.put("kit_max", format1(repairKitMaxUnits));
                            values.put(
                                "hold_shift",
                                i18n.tr(player, "ui.hold-shift")
                            );
                            sendFormattedActionBar(
                                player,
                                repairActionBarFormat,
                                repairActionBarFormatLangKey,
                                "actionbar.repair",
                                values
                            );
                        }
                    }
                },
                1L,
                1L
            );
        repairTasks.put(player.getUniqueId(), task);
    }

    private void stopRepairTask(UUID playerId) {
        BukkitTask task = repairTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        UUID vehicleId = repairCarByPlayer.remove(playerId);
        CarEntity car = vehicleId == null ? null : cars.get(vehicleId);
        stopConfiguredSoundAtCar(car, soundRepair);
        stopConfiguredSoundForPlayer(playerId, soundRepair);
    }

    private void renderRepairHitboxPreview(Player viewer) {
        Location viewerLoc = viewer.getLocation();
        for (CarEntity car : cars.values()) {
            if (
                car == null || !car.isValid() || !car.isAdvancedDamageEnabled()
            ) {
                continue;
            }
            Location carLoc = car.getSafeLocation();
            if (carLoc == null || carLoc.getWorld() == null) {
                continue;
            }
            if (
                viewerLoc.getWorld() == null ||
                !viewerLoc.getWorld().equals(carLoc.getWorld())
            ) {
                continue;
            }
            if (viewerLoc.distanceSquared(carLoc) > 96.0D * 96.0D) {
                continue;
            }
            drawRepairPartHitbox(viewer, car, CarEntity.DamagePart.FRONT);
            drawRepairPartHitbox(viewer, car, CarEntity.DamagePart.REAR);
            drawRepairPartHitbox(viewer, car, CarEntity.DamagePart.WHEEL_FL);
            drawRepairPartHitbox(viewer, car, CarEntity.DamagePart.WHEEL_FR);
            drawRepairPartHitbox(viewer, car, CarEntity.DamagePart.WHEEL_RL);
            drawRepairPartHitbox(viewer, car, CarEntity.DamagePart.WHEEL_RR);
        }
    }

    private void drawRepairPartHitbox(
        Player viewer,
        CarEntity car,
        CarEntity.DamagePart part
    ) {
        Location center = car.getDamagePartLocation(part);
        if (center == null || center.getWorld() == null) {
            return;
        }
        Particle.DustOptions color = switch (part) {
            case FRONT -> new Particle.DustOptions(
                Color.fromRGB(255, 80, 80),
                1.2F
            );
            case REAR -> new Particle.DustOptions(
                Color.fromRGB(80, 160, 255),
                1.2F
            );
            default -> new Particle.DustOptions(
                Color.fromRGB(255, 210, 80),
                1.0F
            );
        };
        double halfX = (part == CarEntity.DamagePart.FRONT ||
            part == CarEntity.DamagePart.REAR)
            ? 0.55D
            : 0.32D;
        double halfY = (part == CarEntity.DamagePart.FRONT ||
            part == CarEntity.DamagePart.REAR)
            ? 0.45D
            : 0.30D;
        double halfZ = (part == CarEntity.DamagePart.FRONT ||
            part == CarEntity.DamagePart.REAR)
            ? 0.55D
            : 0.32D;

        viewer.spawnParticle(
            Particle.DUST,
            center,
            2,
            0.02D,
            0.02D,
            0.02D,
            0.0D,
            color
        );
        drawWireCube(viewer, center, halfX, halfY, halfZ, color);
    }

    private void drawWireCube(
        Player viewer,
        Location center,
        double halfX,
        double halfY,
        double halfZ,
        Particle.DustOptions color
    ) {
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            double x = -halfX + (halfX * 2.0D * t);
            double y = -halfY + (halfY * 2.0D * t);
            double z = -halfZ + (halfZ * 2.0D * t);

            spawnDust(viewer, center, x, -halfY, -halfZ, color);
            spawnDust(viewer, center, x, -halfY, halfZ, color);
            spawnDust(viewer, center, x, halfY, -halfZ, color);
            spawnDust(viewer, center, x, halfY, halfZ, color);

            spawnDust(viewer, center, -halfX, y, -halfZ, color);
            spawnDust(viewer, center, -halfX, y, halfZ, color);
            spawnDust(viewer, center, halfX, y, -halfZ, color);
            spawnDust(viewer, center, halfX, y, halfZ, color);

            spawnDust(viewer, center, -halfX, -halfY, z, color);
            spawnDust(viewer, center, -halfX, halfY, z, color);
            spawnDust(viewer, center, halfX, -halfY, z, color);
            spawnDust(viewer, center, halfX, halfY, z, color);
        }
    }

    private void spawnDust(
        Player viewer,
        Location center,
        double dx,
        double dy,
        double dz,
        Particle.DustOptions color
    ) {
        viewer.spawnParticle(
            Particle.DUST,
            center.getX() + dx,
            center.getY() + dy,
            center.getZ() + dz,
            1,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            color
        );
    }

    private ItemStack createCanister(double liters, String lang) {
        ItemStack item = new ItemStack(canisterMaterial);
        setCanisterLiters(item, liters, lang);
        return item;
    }

    private ItemStack createRepairKit(double units, String lang) {
        ItemStack item = new ItemStack(repairKitMaterial);
        setRepairKitUnits(item, units, lang);
        return item;
    }

    private ItemStack createCarKey(
        CarEntity car,
        UUID ownerId,
        String ownerName,
        String modelName,
        String lang
    ) {
        ItemStack item = new ItemStack(keyItemMaterial);
        if (car == null) {
            return item;
        }
        String ownerNameSafe = (ownerName == null || ownerName.isBlank())
            ? "none"
            : ownerName;
        String ownerUuidSafe = ownerId == null ? "none" : ownerId.toString();
        String modelSafe =
            modelName == null || modelName.isBlank() ? "car" : modelName;
        String modelKey = getCarModelKey(car);
        UUID carId = car.getVehicleId();
        String normalizedLang = i18n == null ? "en" : i18n.normalizeLang(lang);
        String localizedDisplayName = keyDisplayNamesByLang.getOrDefault(
            normalizedLang,
            keyItemDisplayName
        );
        List<String> localizedLore = keyLoreByLang.getOrDefault(
            normalizedLang,
            keyItemLoreLines
        );
        item.editMeta(meta -> {
            meta
                .getPersistentDataContainer()
                .set(carKeyItemMarkerKey, PersistentDataType.STRING, "1");
            meta
                .getPersistentDataContainer()
                .set(
                    carKeyVehicleIdKey,
                    PersistentDataType.STRING,
                    carId.toString()
                );
            meta
                .getPersistentDataContainer()
                .set(
                    carKeyOwnerIdKey,
                    PersistentDataType.STRING,
                    ownerUuidSafe
                );
            ensureUniqueItemId(meta, carKeyUniqueIdKey);

            Map<String, String> values = new HashMap<>();
            values.put("model", modelSafe);
            values.put("model_key", modelKey);
            values.put("car_uuid", carId.toString());
            values.put("owner_name", ownerNameSafe);
            values.put("owner_uuid", ownerUuidSafe);

            String display = PlaceholderLibrary.apply(
                localizedDisplayName,
                values
            );
            meta.setDisplayName(colorize(display));

            List<String> lore = new ArrayList<>();
            for (String line : localizedLore) {
                lore.add(colorize(PlaceholderLibrary.apply(line, values)));
            }
            meta.setLore(lore.isEmpty() ? null : lore);
            meta.setCustomModelData(keyItemCustomModelData);
            meta.setUnbreakable(keyItemUnbreakable);
        });
        return item;
    }

    private boolean isCarKey(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return (
            meta
                .getPersistentDataContainer()
                .has(carKeyVehicleIdKey, PersistentDataType.STRING) &&
            meta
                .getPersistentDataContainer()
                .has(carKeyItemMarkerKey, PersistentDataType.STRING)
        );
    }

    private boolean keyMatchesVehicle(ItemStack item, UUID vehicleId) {
        if (vehicleId == null || !isCarKey(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        String stored = meta
            .getPersistentDataContainer()
            .get(carKeyVehicleIdKey, PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) {
            return false;
        }
        return vehicleId.toString().equalsIgnoreCase(stored.trim());
    }

    private boolean playerHasKeyForVehicle(Player player, UUID vehicleId) {
        if (player == null || vehicleId == null) {
            return false;
        }
        if (
            keyMatchesVehicle(
                player.getInventory().getItemInMainHand(),
                vehicleId
            )
        ) {
            return true;
        }
        if (
            keyMatchesVehicle(
                player.getInventory().getItemInOffHand(),
                vehicleId
            )
        ) {
            return true;
        }
        if (!keysCheckEntireInventory) {
            return false;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (keyMatchesVehicle(item, vehicleId)) {
                return true;
            }
        }
        return false;
    }

    private String canStartEngine(Player player, CarEntity car) {
        if (player == null || car == null || !car.isValid()) {
            return "car.key.start.unavailable";
        }
        if (!keysEnabled || !keysRequireToStartEngine) {
            return null;
        }
        UUID ownerId = getCarOwnerId(car);
        UUID playerId = player.getUniqueId();
        boolean isOwner = ownerId != null && ownerId.equals(playerId);
        if (isOwner && keysAllowOwnerStartWithoutKey) {
            return null;
        }
        if (!isOwner && !keysAllowNonOwnerUseKey) {
            return "car.key.start.owner-only";
        }
        boolean hasKey = playerHasKeyForVehicle(player, car.getVehicleId());
        if (!hasKey) {
            return "car.key.start.need-key";
        }
        return null;
    }

    private String canStopEngine(Player player, CarEntity car) {
        if (player == null || car == null || !car.isValid()) {
            return "car.key.stop.unavailable";
        }
        if (!keysEnabled || !keysRequireToStopEngine) {
            return null;
        }
        boolean hasKey = playerHasKeyForVehicle(player, car.getVehicleId());
        if (!hasKey) {
            return "car.key.stop.need-key";
        }
        return null;
    }

    private boolean isFuelCanister(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta
            .getPersistentDataContainer()
            .has(canisterLitersKey, PersistentDataType.DOUBLE);
    }

    private double getCanisterLiters(ItemStack item) {
        if (!isFuelCanister(item)) {
            return 0.0D;
        }
        ItemMeta meta = item.getItemMeta();
        Double value = meta
            .getPersistentDataContainer()
            .get(canisterLitersKey, PersistentDataType.DOUBLE);
        return value == null ? 0.0D : clamp(value, 0.0D, canisterMaxLiters);
    }

    private void setCanisterLiters(ItemStack item, double liters, String lang) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        double clamped = clamp(liters, 0.0D, canisterMaxLiters);
        item.editMeta(meta -> {
            meta
                .getPersistentDataContainer()
                .set(canisterLitersKey, PersistentDataType.DOUBLE, clamped);
            ensureUniqueItemId(meta, canisterItemIdKey);
            applyCanisterMeta(meta, clamped, lang);
        });
    }

    private boolean isRepairKit(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta
            .getPersistentDataContainer()
            .has(repairKitUnitsKey, PersistentDataType.DOUBLE);
    }

    private double getRepairKitUnits(ItemStack item) {
        if (!isRepairKit(item)) {
            return 0.0D;
        }
        ItemMeta meta = item.getItemMeta();
        Double value = meta
            .getPersistentDataContainer()
            .get(repairKitUnitsKey, PersistentDataType.DOUBLE);
        return value == null ? 0.0D : clamp(value, 0.0D, repairKitMaxUnits);
    }

    private void setRepairKitUnits(ItemStack item, double units, String lang) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        double clamped = clamp(units, 0.0D, repairKitMaxUnits);
        item.editMeta(meta -> {
            meta
                .getPersistentDataContainer()
                .set(repairKitUnitsKey, PersistentDataType.DOUBLE, clamped);
            ensureUniqueItemId(meta, repairKitItemIdKey);
            applyRepairKitMeta(meta, clamped, lang);
        });
    }

    private void ensureUniqueItemId(ItemMeta meta, NamespacedKey key) {
        if (meta == null || key == null) {
            return;
        }
        if (
            meta
                .getPersistentDataContainer()
                .has(key, PersistentDataType.STRING)
        ) {
            return;
        }
        meta
            .getPersistentDataContainer()
            .set(key, PersistentDataType.STRING, UUID.randomUUID().toString());
    }

    private void applyCanisterMeta(ItemMeta meta, double liters, String lang) {
        String normalizedLang = i18n == null ? "en" : i18n.normalizeLang(lang);
        String localizedDisplayName = canisterDisplayNamesByLang.getOrDefault(
            normalizedLang,
            canisterDisplayName
        );
        String resolvedName = replaceCanisterPlaceholders(
            localizedDisplayName,
            liters
        );
        if (!resolvedName.isBlank()) {
            meta.setDisplayName(colorize(resolvedName));
        }

        List<String> localizedLore = canisterLoreByLang.getOrDefault(
            normalizedLang,
            canisterLoreLines
        );
        List<String> lore = new ArrayList<>();
        for (String raw : localizedLore) {
            lore.add(colorize(replaceCanisterPlaceholders(raw, liters)));
        }
        meta.setLore(lore.isEmpty() ? null : lore);

        meta.setCustomModelData(canisterCustomModelData);
        meta.setUnbreakable(canisterUnbreakable);

        for (Map.Entry<
            NamespacedKey,
            String
        > entry : canisterParsedTags.entrySet()) {
            applyTypedTag(meta, entry.getKey(), entry.getValue());
        }
    }

    private void applyRepairKitMeta(ItemMeta meta, double units, String lang) {
        String normalizedLang = i18n == null ? "en" : i18n.normalizeLang(lang);
        String localizedDisplayName = repairKitDisplayNamesByLang.getOrDefault(
            normalizedLang,
            repairKitDisplayName
        );
        String resolvedName = replaceRepairKitPlaceholders(
            localizedDisplayName,
            units
        );
        if (!resolvedName.isBlank()) {
            meta.setDisplayName(colorize(resolvedName));
        }
        List<String> localizedLore = repairKitLoreByLang.getOrDefault(
            normalizedLang,
            repairKitLoreLines
        );
        List<String> lore = new ArrayList<>();
        for (String raw : localizedLore) {
            lore.add(colorize(replaceRepairKitPlaceholders(raw, units)));
        }
        meta.setLore(lore.isEmpty() ? null : lore);
        meta.setCustomModelData(repairKitCustomModelData);
        meta.setUnbreakable(repairKitUnbreakable);
        if (repairKitParsedTags == null) {
            return;
        }
        for (Map.Entry<
            NamespacedKey,
            String
        > entry : repairKitParsedTags.entrySet()) {
            applyTypedTag(meta, entry.getKey(), entry.getValue());
        }
    }

    private String replaceCanisterPlaceholders(String text, double liters) {
        Map<String, String> values = Map.of(
            "liters",
            format1(liters),
            "litres",
            format1(liters),
            "capacity",
            format1(canisterMaxLiters)
        );
        return PlaceholderLibrary.apply(text, values);
    }

    private String replaceRepairKitPlaceholders(String text, double units) {
        Map<String, String> values = Map.of(
            "units",
            format1(units),
            "capacity",
            format1(repairKitMaxUnits)
        );
        return PlaceholderLibrary.apply(text, values);
    }

    private NamespacedKey parseTagKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        NamespacedKey parsed = NamespacedKey.fromString(raw);
        if (parsed != null) {
            return parsed;
        }
        String sanitized = raw
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9._-]", "_");
        if (sanitized.isBlank()) {
            return null;
        }
        return new NamespacedKey(this, sanitized);
    }

    private static void applyTypedTag(
        ItemMeta meta,
        NamespacedKey key,
        String rawValue
    ) {
        if (rawValue == null) {
            return;
        }
        String value = rawValue.trim();
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            meta
                .getPersistentDataContainer()
                .set(
                    key,
                    PersistentDataType.BYTE,
                    (byte) (Boolean.parseBoolean(value) ? 1 : 0)
                );
            return;
        }
        try {
            int intValue = Integer.parseInt(value);
            meta
                .getPersistentDataContainer()
                .set(key, PersistentDataType.INTEGER, intValue);
            return;
        } catch (NumberFormatException ignored) {}
        try {
            double doubleValue = Double.parseDouble(value);
            meta
                .getPersistentDataContainer()
                .set(key, PersistentDataType.DOUBLE, doubleValue);
            return;
        } catch (NumberFormatException ignored) {}
        meta
            .getPersistentDataContainer()
            .set(key, PersistentDataType.STRING, value);
    }

    private ItemStack[] createWheelModels() {
        return createWheelModels(carSettings);
    }

    private ItemStack[] createWheelModels(CarSettings settings) {
        ItemStack[] out = new ItemStack[4];
        for (int i = 0; i < out.length; i++) {
            out[i] = createDisplayModel(
                settings.wheelMaterials[i],
                settings.wheelCustomModelDataByIndex[i]
            );
        }
        return out;
    }

    private ItemStack createBodyModel() {
        return createBodyModel(carSettings);
    }

    private ItemStack createBodyModel(CarSettings settings) {
        return createDisplayModel(
            settings.bodyMaterial,
            settings.bodyCustomModelData
        );
    }

    private static ItemStack createDisplayModel(Material material, int cmd) {
        ItemStack item = new ItemStack(material);
        if (cmd >= 0) {
            item.editMeta(meta -> meta.setCustomModelData(cmd));
        }
        return item;
    }

    private static String colorize(String text) {
        return text == null
            ? ""
            : ChatColor.translateAlternateColorCodes('&', text);
    }

    private void startAutoSaveTask() {
        stopAutoSaveTask();
        if (!autoSaveEnabled) {
            return;
        }
        autoSaveTask = getServer()
            .getScheduler()
            .runTaskTimer(
                this,
                () -> {
                    try {
                        saveCars(false);
                    } catch (Exception ex) {
                        getLogger().warning(
                            "Auto-save failed: " + ex.getMessage()
                        );
                    }
                },
                autoSaveIntervalTicks,
                autoSaveIntervalTicks
            );
    }

    private void stopAutoSaveTask() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    private void saveCars() {
        saveCars(true);
    }

    private void saveCars(boolean syncDbSnapshot) {
        if (carsFile == null) {
            return;
        }
        YamlConfiguration cfg = new YamlConfiguration();
        ConfigurationSection root = cfg.createSection("cars");
        List<String> uuidList = new ArrayList<>();
        Set<UUID> savedVehicleIds = new HashSet<>();
        Set<UUID> skippedVehicleIds = new HashSet<>();
        int skipped = 0;
        for (CarEntity car : cars.values()) {
            Location loc = car.getSafeLocation();
            if (loc == null || loc.getWorld() == null) {
                skipped++;
                skippedVehicleIds.add(car.getVehicleId());
                getLogger().warning(
                    "Skipping car during save (no valid location): " +
                        car.getVehicleId()
                );
                continue;
            }
            String key = car.getVehicleId().toString();
            uuidList.add(key);
            savedVehicleIds.add(car.getVehicleId());
            ConfigurationSection sec = root.createSection(key);
            sec.set(
                "model",
                carModelById.getOrDefault(car.getVehicleId(), defaultModel)
            );
            sec.set("world", loc.getWorld().getName());
            sec.set("x", loc.getX());
            sec.set("y", loc.getY());
            sec.set("z", loc.getZ());
            sec.set("yaw", loc.getYaw());
            sec.set("health", car.getHealth());
            sec.set("fuel", car.getFuelLiters());
            sec.set("engine-running", car.isEngineRunning());
            sec.set("headlights-on", car.isHeadlightsOn());
            sec.set("trunk", car.getTrunkContentsCopy());
            Map<String, Double> damageSnapshot = car.getDamageHealthSnapshot();
            if (!damageSnapshot.isEmpty()) {
                sec.set("damage", damageSnapshot);
            }
            UUID ownerId = carOwners.get(car.getVehicleId());
            if (ownerId != null) {
                sec.set("owner", ownerId.toString());
                String ownerName = carOwnerNames.get(car.getVehicleId());
                if (ownerName != null && !ownerName.isBlank()) {
                    sec.set("owner-name", ownerName);
                }
            }
        }

        for (StoredCarRecord pending : pendingCarRestores) {
            if (pending == null || pending.vehicleId == null) {
                continue;
            }
            if (savedVehicleIds.contains(pending.vehicleId)) {
                continue;
            }
            if (pending.worldName == null || pending.worldName.isBlank()) {
                skipped++;
                continue;
            }
            String key = pending.vehicleId.toString();
            uuidList.add(key);
            savedVehicleIds.add(pending.vehicleId);
            ConfigurationSection sec = root.createSection(key);
            sec.set("model", pending.modelKey);
            sec.set("world", pending.worldName);
            sec.set("x", pending.x);
            sec.set("y", pending.y);
            sec.set("z", pending.z);
            sec.set("yaw", pending.yaw);
            sec.set("health", pending.health);
            sec.set("fuel", pending.fuel);
            sec.set("engine-running", pending.engineRunning);
            sec.set("headlights-on", pending.headlightsOn);
            sec.set("trunk", pending.trunkContents);
            if (
                pending.damageSnapshot != null &&
                !pending.damageSnapshot.isEmpty()
            ) {
                sec.set("damage", pending.damageSnapshot);
            }
            if (pending.ownerId != null) {
                sec.set("owner", pending.ownerId.toString());
                if (pending.ownerName != null && !pending.ownerName.isBlank()) {
                    sec.set("owner-name", pending.ownerName);
                }
            }
        }

        int preservedSkipped = preserveSkippedCarsFromPreviousFile(
            root,
            savedVehicleIds,
            skippedVehicleIds
        );
        if (preservedSkipped > 0) {
            for (String key : root.getKeys(false)) {
                UUID id = parseUuid(key);
                if (id != null) {
                    savedVehicleIds.add(id);
                }
            }
        }

        uuidList.clear();
        for (UUID id : savedVehicleIds) {
            uuidList.add(id.toString());
        }
        Collections.sort(uuidList);
        cfg.set("car-uuids", uuidList);
        cfg.set("car-count", uuidList.size());
        File tmpFile = new File(carsFile.getParentFile(), "cars.yml.tmp");
        try {
            cfg.save(tmpFile);
            try {
                Files.move(
                    tmpFile.toPath(),
                    carsFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException ignored) {
                Files.move(
                    tmpFile.toPath(),
                    carsFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (Exception ex) {
            getLogger().warning("Failed to save cars.yml: " + ex.getMessage());
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
        if (syncDbSnapshot) {
            syncOwnershipDatabaseSnapshot();
        }
    }

    private int preserveSkippedCarsFromPreviousFile(
        ConfigurationSection targetRoot,
        Set<UUID> alreadySaved,
        Set<UUID> skippedNow
    ) {
        if (
            targetRoot == null ||
            alreadySaved == null ||
            skippedNow == null ||
            skippedNow.isEmpty() ||
            carsFile == null ||
            !carsFile.exists()
        ) {
            return 0;
        }
        YamlConfiguration previous = YamlConfiguration.loadConfiguration(
            carsFile
        );
        ConfigurationSection previousRoot = previous.getConfigurationSection(
            "cars"
        );
        if (previousRoot == null) {
            return 0;
        }

        int restored = 0;
        for (UUID id : skippedNow) {
            if (id == null || alreadySaved.contains(id)) {
                continue;
            }
            String key = id.toString();
            ConfigurationSection oldSection =
                previousRoot.getConfigurationSection(key);
            if (oldSection == null) {
                continue;
            }
            ConfigurationSection copied = targetRoot.createSection(key);
            copySectionRecursive(oldSection, copied);
            alreadySaved.add(id);
            restored++;
        }
        return restored;
    }

    private static void copySectionRecursive(
        ConfigurationSection source,
        ConfigurationSection target
    ) {
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection sourceChild) {
                ConfigurationSection child = target.createSection(key);
                copySectionRecursive(sourceChild, child);
                continue;
            }
            target.set(key, value);
        }
    }

    private void loadCars() {
        if (carsFile == null) {
            loadOwnershipFromDatabase();
            return;
        }
        YamlConfiguration cfg = loadCarsStorageConfig();
        ConfigurationSection root = cfg.getConfigurationSection("cars");
        if (root == null) {
            loadOwnershipFromDatabase();
            return;
        }

        pendingCarRestores.clear();
        carOwners.clear();
        carOwnerNames.clear();
        carModelById.clear();
        carSpawnedAtMs.clear();
        carLastUsedAtMs.clear();
        carLastEventTypeById.clear();
        carLastEventDetailsById.clear();
        carLastEventAtMs.clear();
        carLastEventFlushedAtMs.clear();
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                ConfigurationSection sec = root.getConfigurationSection(key);
                if (sec == null) {
                    continue;
                }
                StoredCarRecord record = StoredCarRecord.fromSection(
                    id,
                    sec,
                    defaultModel,
                    this::resolveModelSettings,
                    this::normalizeModelKey
                );
                if (!attemptSpawnStoredCar(record)) {
                    pendingCarRestores.add(record);
                }
            } catch (Exception ex) {
                getLogger().warning(
                    "Failed to load car entry " + key + ": " + ex.getMessage()
                );
            }
        }
        schedulePendingCarRestoreTask();
        loadOwnershipFromDatabase();
        loadTelemetryFromDatabase();
        syncOwnershipDatabaseSnapshot();
        List<String> loadedIds = cars
            .entrySet()
            .stream()
            .filter(
                entry -> entry.getValue() != null && entry.getValue().isValid()
            )
            .map(entry -> entry.getKey().toString())
            .sorted()
            .collect(Collectors.toList());
        getLogger().info(
            "Loaded valid cars from storage: " +
                loadedIds.size() +
                (loadedIds.isEmpty()
                    ? ""
                    : " | UUIDs: " + String.join(", ", loadedIds))
        );
    }

    private YamlConfiguration loadCarsStorageConfig() {
        if (carsFile == null) {
            return new YamlConfiguration();
        }
        File tmpFile = new File(carsFile.getParentFile(), "cars.yml.tmp");
        if (!carsFile.exists() && tmpFile.exists()) {
            try {
                Files.move(
                    tmpFile.toPath(),
                    carsFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                );
                getLogger().warning(
                    "Recovered cars.yml from leftover cars.yml.tmp"
                );
            } catch (IOException ex) {
                getLogger().warning(
                    "Failed to recover cars.yml from tmp: " + ex.getMessage()
                );
            }
        }

        YamlConfiguration primary = loadYamlStrict(carsFile);
        ConfigurationSection root = primary.getConfigurationSection("cars");
        if (root != null || !tmpFile.exists()) {
            return primary;
        }

        YamlConfiguration fallback = loadYamlStrict(tmpFile);
        if (fallback.getConfigurationSection("cars") != null) {
            File broken = new File(
                carsFile.getParentFile(),
                "cars.yml.broken-" + System.currentTimeMillis()
            );
            try {
                if (carsFile.exists()) {
                    Files.move(
                        carsFile.toPath(),
                        broken.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                }
                Files.move(
                    tmpFile.toPath(),
                    carsFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                );
                getLogger().warning(
                    "cars.yml was invalid; restored from cars.yml.tmp"
                );
            } catch (IOException ex) {
                getLogger().warning(
                    "Failed to replace invalid cars.yml from tmp: " +
                        ex.getMessage()
                );
            }
            return fallback;
        }
        return primary;
    }

    private YamlConfiguration loadYamlStrict(File file) {
        YamlConfiguration cfg = new YamlConfiguration();
        if (file == null || !file.exists()) {
            return cfg;
        }
        try {
            String raw = Files.readString(
                file.toPath(),
                StandardCharsets.UTF_8
            );
            cfg.loadFromString(raw.replace("\\u0000", ""));
        } catch (Exception ex) {
            getLogger().warning(
                "Failed to parse " + file.getName() + ": " + ex.getMessage()
            );
        }
        return cfg;
    }

    private boolean spawnStoredCar(StoredCarRecord record) {
        if (
            record == null ||
            record.worldName == null ||
            record.worldName.isBlank()
        ) {
            return false;
        }
        World world = getServer().getWorld(record.worldName);
        if (
            world == null &&
            record.restoreAttempts >= 10 &&
            !getServer().getWorlds().isEmpty()
        ) {
            world = getServer().getWorlds().get(0);
            getLogger().warning(
                "World '" +
                    record.worldName +
                    "' unavailable for car " +
                    record.vehicleId +
                    ". Restoring in fallback world '" +
                    world.getName() +
                    "' after " +
                    record.restoreAttempts +
                    " attempts."
            );
        }
        if (world == null) {
            return false;
        }
        try {
            Location loc = new Location(
                world,
                record.x,
                record.y,
                record.z,
                record.yaw,
                0.0F
            );
            Chunk chunk = world.getChunkAt(loc);
            if (!chunk.isLoaded()) {
                chunk.load();
            }
            cleanupLegacyCarEntitiesNear(loc);
            CarEntity car = createStoredCarAt(record, loc);
            if (!car.isValid()) {
                String invalidState = car.validationState();
                car.destroy();
                Location fallback = world
                    .getSpawnLocation()
                    .clone()
                    .add(0.0D, 1.0D, 0.0D);
                fallback.setYaw(record.yaw);
                cleanupLegacyCarEntitiesNear(fallback);
                CarEntity fallbackCar = createStoredCarAt(record, fallback);
                if (!fallbackCar.isValid()) {
                    String fallbackState = fallbackCar.validationState();
                    fallbackCar.destroy();
                    getLogger().warning(
                        "Car spawned invalid, scheduling retry: " +
                            record.vehicleId +
                            " | attempt=" +
                            record.restoreAttempts +
                            " | state=" +
                            invalidState +
                            " | fallbackState=" +
                            fallbackState
                    );
                    return false;
                }
                car = fallbackCar;
                getLogger().warning(
                    "Car restored at fallback location for " +
                        record.vehicleId +
                        " | attempt=" +
                        record.restoreAttempts +
                        " (original state invalid: " +
                        invalidState +
                        ")"
                );
            }
            car.applyDamageHealthSnapshot(record.damageSnapshot);
            cars.put(car.getVehicleId(), car);
            registerCarPartMappings(car);
            carModelById.put(car.getVehicleId(), record.modelKey);
            carSpawnedAtMs.putIfAbsent(
                car.getVehicleId(),
                System.currentTimeMillis()
            );
            noteCarEvent(car, "restored", "from-storage");
            if (record.ownerId != null) {
                carOwners.put(record.vehicleId, record.ownerId);
                if (record.ownerName != null && !record.ownerName.isBlank()) {
                    carOwnerNames.put(record.vehicleId, record.ownerName);
                }
                upsertOwnershipForCar(car);
            }
            return true;
        } catch (Exception ex) {
            getLogger().warning(
                "Car spawn failed for " +
                    record.vehicleId +
                    ": " +
                    ex.getMessage()
            );
            return false;
        }
    }

    private CarEntity createStoredCarAt(StoredCarRecord record, Location loc) {
        CarSettings settings = resolveModelSettings(record.modelKey);
        Location spawnLoc = loc;
        if (!isSpawnAreaClear(spawnLoc, settings)) {
            for (int i = 1; i <= 4; i++) {
                Location lifted = loc.clone().add(0.0D, i * 0.50D, 0.0D);
                if (isSpawnAreaClear(lifted, settings)) {
                    spawnLoc = lifted;
                    break;
                }
            }
        }
        ItemStack[] trunk = normalizeTrunkContents(
            record.trunkContents,
            settings.trunkSlots
        );
        return new CarEntity(
            record.vehicleId,
            spawnLoc,
            createBodyModel(settings),
            createWheelModels(settings),
            settings,
            record.health,
            record.fuel,
            trunk,
            record.engineRunning,
            record.headlightsOn
        );
    }

    private boolean attemptSpawnStoredCar(StoredCarRecord record) {
        if (record == null) {
            return false;
        }
        record.restoreAttempts++;
        return spawnStoredCar(record);
    }

    private void cleanupLegacyCarEntitiesNear(Location center) {
        if (
            center == null || center.getWorld() == null || carSettings == null
        ) {
            return;
        }
        int removed = 0;
        for (Entity entity : center
            .getWorld()
            .getNearbyEntities(center, 4.0D, 3.0D, 4.0D)) {
            if (!isLikelyLegacyCarPart(entity)) {
                continue;
            }
            entity.remove();
            removed++;
        }
        if (removed > 0) {
            getLogger().info(
                "Removed legacy car entities near restore point: " + removed
            );
        }
    }

    private boolean isLikelyLegacyCarPart(Entity entity) {
        if (entity == null || !entity.isValid()) {
            return false;
        }
        if (entity instanceof ItemDisplay display) {
            if (!display.isPersistent()) {
                return false;
            }
            ItemStack stack = display.getItemStack();
            if (stack == null) {
                return false;
            }
            Material type = stack.getType();
            if (type == carSettings.bodyMaterial) {
                return true;
            }
            if (type == carSettings.steeringWheelMaterial) {
                return true;
            }
            for (Material wheelMaterial : carSettings.wheelMaterials) {
                if (type == wheelMaterial) {
                    return true;
                }
            }
            return false;
        }
        if (entity instanceof ArmorStand stand) {
            return (
                stand.isPersistent() &&
                !stand.isVisible() &&
                !stand.hasGravity() &&
                stand.isInvulnerable()
            );
        }
        if (entity instanceof Interaction interaction) {
            if (!interaction.isPersistent()) {
                return false;
            }
            float width = interaction.getInteractionWidth();
            float height = interaction.getInteractionHeight();
            return (
                Math.abs(width - (float) carSettings.interactionWidth) <=
                    0.15F &&
                Math.abs(height - (float) carSettings.interactionHeight) <=
                0.15F
            );
        }
        return false;
    }

    private void schedulePendingCarRestoreTask() {
        if (pendingCarRestores.isEmpty()) {
            return;
        }
        if (pendingRestoreTask != null) {
            pendingRestoreTask.cancel();
            pendingRestoreTask = null;
        }
        getLogger().info(
            "Cars pending restore (world/chunk unavailable): " +
                pendingCarRestores.size()
        );
        pendingRestoreTask = getServer()
            .getScheduler()
            .runTaskTimer(
                this,
                () -> {
                    if (pendingCarRestores.isEmpty()) {
                        if (pendingRestoreTask != null) {
                            pendingRestoreTask.cancel();
                            pendingRestoreTask = null;
                        }
                        return;
                    }
                    List<StoredCarRecord> unresolved = new ArrayList<>();
                    int restoredNow = 0;
                    for (StoredCarRecord record : pendingCarRestores) {
                        if (attemptSpawnStoredCar(record)) {
                            restoredNow++;
                        } else {
                            unresolved.add(record);
                        }
                    }
                    pendingCarRestores.clear();
                    pendingCarRestores.addAll(unresolved);
                    if (restoredNow > 0) {
                        getLogger().info(
                            "Deferred restore: restored " +
                                restoredNow +
                                ", remaining " +
                                pendingCarRestores.size()
                        );
                    }
                },
                40L,
                40L
            );
    }

    private ItemStack[] deserializeItemArray(List<?> raw, int size) {
        ItemStack[] out = new ItemStack[size];
        if (raw == null) {
            return out;
        }
        for (int i = 0; i < size && i < raw.size(); i++) {
            Object element = raw.get(i);
            if (element instanceof ItemStack item) {
                out[i] = item;
            }
        }
        return out;
    }

    private int reloadCarsFromConfig() {
        saveCars();
        Map<UUID, UUID> ownerSnapshot = new HashMap<>(carOwners);
        Map<UUID, String> ownerNameSnapshot = new HashMap<>(carOwnerNames);
        Map<UUID, String> modelSnapshot = new HashMap<>(carModelById);
        Map<UUID, Long> spawnedAtSnapshot = new HashMap<>(carSpawnedAtMs);
        Map<UUID, Long> lastUsedSnapshot = new HashMap<>(carLastUsedAtMs);
        Map<UUID, String> lastEventTypeSnapshot = new HashMap<>(
            carLastEventTypeById
        );
        Map<UUID, String> lastEventDetailsSnapshot = new HashMap<>(
            carLastEventDetailsById
        );
        Map<UUID, Long> lastEventAtSnapshot = new HashMap<>(carLastEventAtMs);

        List<CarReloadSnapshot> snapshots = new ArrayList<>();
        for (CarEntity car : cars.values()) {
            Location snapshotLoc = car.getSafeLocation();
            if (snapshotLoc == null || snapshotLoc.getWorld() == null) {
                getLogger().warning(
                    "Skipping car reload snapshot (no valid location): " +
                        car.getVehicleId()
                );
                continue;
            }
            List<UUID> mountedPlayers = new ArrayList<>();
            for (Player mounted : car.getMountedPlayers()) {
                UUID playerId = mounted.getUniqueId();
                mountedPlayers.add(playerId);
                driverToCar.remove(playerId);
                stopRefuelTask(playerId);
                stopRepairTask(playerId);
            }
            snapshots.add(
                new CarReloadSnapshot(
                    car.getVehicleId(),
                    modelSnapshot.getOrDefault(
                        car.getVehicleId(),
                        defaultModel
                    ),
                    snapshotLoc,
                    car.getHealth(),
                    car.getFuelLiters(),
                    car.isEngineRunning(),
                    car.isHeadlightsOn(),
                    car.getTrunkContentsCopy(),
                    car.getDamageHealthSnapshot(),
                    mountedPlayers,
                    spawnedAtSnapshot.getOrDefault(
                        car.getVehicleId(),
                        System.currentTimeMillis()
                    ),
                    lastUsedSnapshot.getOrDefault(car.getVehicleId(), 0L),
                    lastEventTypeSnapshot.getOrDefault(car.getVehicleId(), ""),
                    lastEventDetailsSnapshot.getOrDefault(
                        car.getVehicleId(),
                        ""
                    ),
                    lastEventAtSnapshot.getOrDefault(car.getVehicleId(), 0L)
                )
            );
            unregisterCarPartMappings(car);
            car.destroy();
            clearVehicleSoundState(car.getVehicleId());
            clearCarChunkTickets(car.getVehicleId());
        }

        cars.clear();
        carChunkTickets.clear();
        carChunkCenterTokenCache.clear();
        partEntityToCar.clear();
        fuelEntityToCar.clear();
        trunkEntityToCar.clear();
        driverToCar.clear();
        playerToCarCache.clear();
        carOwners.clear();
        carOwnerNames.clear();
        carModelById.clear();
        carSpawnedAtMs.clear();
        carLastUsedAtMs.clear();
        carLastEventTypeById.clear();
        carLastEventDetailsById.clear();
        carLastEventAtMs.clear();
        carLastEventFlushedAtMs.clear();
        drivingSoundNextTick.clear();
        idleSoundNextTick.clear();
        pendingIdleStartTick.clear();
        pendingDriveStartTick.clear();
        refuelCarByPlayer.clear();
        repairCarByPlayer.clear();

        reloadPluginConfig();

        int restored = 0;
        for (CarReloadSnapshot snapshot : snapshots) {
            try {
                CarSettings settings = resolveModelSettings(snapshot.modelKey);
                ItemStack[] trunk = normalizeTrunkContents(
                    snapshot.trunkContents,
                    settings.trunkSlots
                );
                Location safeSnapshotLoc = snapshot.location.clone();
                if (!isSpawnAreaClear(safeSnapshotLoc, settings)) {
                    Location resolved = resolveSafeSpawnLocation(
                        snapshot.location.clone(),
                        settings
                    );
                    if (resolved != null) {
                        safeSnapshotLoc = resolved;
                    }
                }
                CarEntity car = new CarEntity(
                    snapshot.vehicleId,
                    safeSnapshotLoc,
                    createBodyModel(settings),
                    createWheelModels(settings),
                    settings,
                    snapshot.health,
                    snapshot.fuelLiters,
                    trunk,
                    snapshot.engineRunning,
                    snapshot.headlightsOn
                );
                car.applyDamageHealthSnapshot(snapshot.damageHealth);
                cars.put(car.getVehicleId(), car);
                registerCarPartMappings(car);
                carModelById.put(car.getVehicleId(), snapshot.modelKey);
                if (snapshot.spawnedAtMs > 0L) {
                    carSpawnedAtMs.put(
                        car.getVehicleId(),
                        snapshot.spawnedAtMs
                    );
                }
                if (snapshot.lastUsedAtMs > 0L) {
                    carLastUsedAtMs.put(
                        car.getVehicleId(),
                        snapshot.lastUsedAtMs
                    );
                }
                if (snapshot.lastEventAtMs > 0L) {
                    carLastEventAtMs.put(
                        car.getVehicleId(),
                        snapshot.lastEventAtMs
                    );
                    carLastEventFlushedAtMs.put(
                        car.getVehicleId(),
                        snapshot.lastEventAtMs
                    );
                }
                if (
                    snapshot.lastEventType != null &&
                    !snapshot.lastEventType.isBlank()
                ) {
                    carLastEventTypeById.put(
                        car.getVehicleId(),
                        snapshot.lastEventType
                    );
                }
                if (
                    snapshot.lastEventDetails != null &&
                    !snapshot.lastEventDetails.isBlank()
                ) {
                    carLastEventDetailsById.put(
                        car.getVehicleId(),
                        snapshot.lastEventDetails
                    );
                }
                UUID ownerId = ownerSnapshot.get(snapshot.vehicleId);
                if (ownerId != null) {
                    carOwners.put(snapshot.vehicleId, ownerId);
                    String ownerName = ownerNameSnapshot.get(
                        snapshot.vehicleId
                    );
                    if (ownerName != null && !ownerName.isBlank()) {
                        carOwnerNames.put(snapshot.vehicleId, ownerName);
                    }
                }
                restoreMountedPlayers(car, snapshot.mountedPlayers);
                restored++;
            } catch (Exception ex) {
                getLogger().warning(
                    "Failed to rebuild car " +
                        snapshot.vehicleId +
                        " during reload: " +
                        ex.getMessage()
                );
            }
        }

        saveCars();
        return restored;
    }

    private void restoreMountedPlayers(
        CarEntity car,
        List<UUID> mountedPlayers
    ) {
        for (int i = 0; i < mountedPlayers.size(); i++) {
            Player player = Bukkit.getPlayer(mountedPlayers.get(i));
            if (player == null || !player.isOnline()) {
                continue;
            }

            boolean mounted;
            if (i == 0) {
                mounted = car.mountDriver(player);
            } else {
                int seatIndex = car.firstFreeSeatIndex();
                mounted = seatIndex >= 0 && car.mountSeat(player, seatIndex);
            }

            if (mounted && i == 0) {
                driverToCar.put(player.getUniqueId(), car.getVehicleId());
            }
            if (mounted) {
                cachePlayerCar(player.getUniqueId(), car.getVehicleId());
            }
        }
    }

    private ItemStack[] normalizeTrunkContents(ItemStack[] src, int size) {
        ItemStack[] out = new ItemStack[Math.max(0, size)];
        if (src == null) {
            return out;
        }
        for (int i = 0; i < out.length && i < src.length; i++) {
            out[i] = src[i];
        }
        return out;
    }

    private void setCarOwner(UUID vehicleId, UUID ownerId, String ownerName) {
        if (vehicleId == null || ownerId == null) {
            return;
        }
        carOwners.put(vehicleId, ownerId);
        if (ownerName != null && !ownerName.isBlank()) {
            carOwnerNames.put(vehicleId, ownerName);
        } else {
            carOwnerNames.remove(vehicleId);
        }
    }

    private void loadOwnershipFromDatabase() {
        if (ownershipDatabase == null || !ownershipDatabase.isEnabled()) {
            return;
        }
        Map<UUID, CarOwnershipDatabase.OwnershipRecord> records =
            ownershipDatabase.loadOwnershipRecords();
        for (Map.Entry<
            UUID,
            CarOwnershipDatabase.OwnershipRecord
        > entry : records.entrySet()) {
            UUID vehicleId = entry.getKey();
            if (!cars.containsKey(vehicleId)) {
                continue;
            }
            CarOwnershipDatabase.OwnershipRecord record = entry.getValue();
            setCarOwner(vehicleId, record.ownerId, record.ownerName);
        }
    }

    private void syncOwnershipDatabaseSnapshot() {
        if (ownershipDatabase == null || !ownershipDatabase.isEnabled()) {
            return;
        }
        Set<UUID> activeVehicleIds = new HashSet<>(cars.keySet());
        for (StoredCarRecord pending : pendingCarRestores) {
            if (pending != null && pending.vehicleId != null) {
                activeVehicleIds.add(pending.vehicleId);
            }
        }
        Map<UUID, CarOwnershipDatabase.OwnershipRecord> existing =
            ownershipDatabase.loadOwnershipRecords();
        for (UUID vehicleId : existing.keySet()) {
            if (!activeVehicleIds.contains(vehicleId)) {
                ownershipDatabase.deleteOwnership(vehicleId);
            }
        }
        for (CarEntity car : cars.values()) {
            upsertOwnershipForCar(car);
        }
    }

    private void loadTelemetryFromDatabase() {
        if (ownershipDatabase == null || !ownershipDatabase.isEnabled()) {
            return;
        }
        Map<UUID, CarOwnershipDatabase.TelemetryStateRecord> records =
            ownershipDatabase.loadTelemetryStateRecords();
        for (Map.Entry<
            UUID,
            CarOwnershipDatabase.TelemetryStateRecord
        > entry : records.entrySet()) {
            UUID vehicleId = entry.getKey();
            if (!cars.containsKey(vehicleId)) {
                continue;
            }
            CarOwnershipDatabase.TelemetryStateRecord record = entry.getValue();
            if (record.firstSeenAt > 0L) {
                carSpawnedAtMs.put(vehicleId, record.firstSeenAt);
            }
            if (record.spawnedAt > 0L) {
                carSpawnedAtMs.put(vehicleId, record.spawnedAt);
            }
            if (record.lastUsedAt > 0L) {
                carLastUsedAtMs.put(vehicleId, record.lastUsedAt);
            }
            if (record.lastEventAt > 0L) {
                carLastEventAtMs.put(vehicleId, record.lastEventAt);
                carLastEventFlushedAtMs.put(vehicleId, record.lastEventAt);
            }
            if (record.lastEvent != null && !record.lastEvent.isBlank()) {
                carLastEventTypeById.put(vehicleId, record.lastEvent);
            }
            if (
                record.lastEventDetails != null &&
                !record.lastEventDetails.isBlank()
            ) {
                carLastEventDetailsById.put(vehicleId, record.lastEventDetails);
            }
        }
    }

    private void touchCarActivity(CarEntity car) {
        if (car == null || !car.isValid()) {
            return;
        }
        UUID vehicleId = car.getVehicleId();
        long now = System.currentTimeMillis();
        carSpawnedAtMs.putIfAbsent(vehicleId, now);
        if (car.getOccupiedSeatCount() > 0) {
            carLastUsedAtMs.put(vehicleId, now);
        }
        if (car.isCrashThisTick()) {
            noteCarEvent(
                car,
                "crash",
                "impact=" + format1(car.getCrashDeltaSpeedThisTick())
            );
        } else if (car.isHardLandingThisTick()) {
            noteCarEvent(
                car,
                "hard_landing",
                "impact=" + format1(car.getLandingImpactSpeedThisTick())
            );
        } else if (car.isRamHitThisTick()) {
            noteCarEvent(
                car,
                "ram_hit",
                "speed=" + format1(car.getRamHitSpeedThisTick())
            );
        }
    }

    private void noteCarEvent(CarEntity car, String type, String details) {
        if (car == null) {
            return;
        }
        UUID vehicleId = car.getVehicleId();
        long now = System.currentTimeMillis();
        String safeType = type == null ? "event" : type.trim().toLowerCase();
        String safeDetails = details == null ? "" : details.trim();
        carLastEventTypeById.put(vehicleId, safeType);
        if (!safeDetails.isBlank()) {
            carLastEventDetailsById.put(vehicleId, safeDetails);
        }
        carLastEventAtMs.put(vehicleId, now);
    }

    private void noteCarEventById(UUID vehicleId, String type, String details) {
        if (vehicleId == null) {
            return;
        }
        CarEntity car = cars.get(vehicleId);
        if (car != null) {
            noteCarEvent(car, type, details);
            return;
        }
        long now = System.currentTimeMillis();
        String safeType = type == null ? "event" : type.trim().toLowerCase();
        carLastEventTypeById.put(vehicleId, safeType);
        if (details != null && !details.isBlank()) {
            carLastEventDetailsById.put(vehicleId, details.trim());
        }
        carLastEventAtMs.put(vehicleId, now);
    }

    private void upsertTelemetryForCar(
        CarEntity car,
        UUID ownerId,
        String ownerName
    ) {
        if (
            ownershipDatabase == null ||
            !ownershipDatabase.isEnabled() ||
            car == null
        ) {
            return;
        }

        UUID vehicleId = car.getVehicleId();
        long now = System.currentTimeMillis();
        long firstSeenAt = carSpawnedAtMs.getOrDefault(vehicleId, now);
        long lastUsedAt = carLastUsedAtMs.getOrDefault(vehicleId, firstSeenAt);
        long lastEventAt = carLastEventAtMs.getOrDefault(vehicleId, 0L);
        String lastEvent = carLastEventTypeById.getOrDefault(vehicleId, "");
        String lastEventDetails = carLastEventDetailsById.getOrDefault(
            vehicleId,
            ""
        );
        String modelKey = carModelById.getOrDefault(vehicleId, defaultModel);
        String modelName = modelDisplayNames.getOrDefault(modelKey, modelKey);
        Location loc = car.getSafeLocation();

        CarOwnershipDatabase.TelemetrySnapshot snapshot =
            new CarOwnershipDatabase.TelemetrySnapshot();
        snapshot.vehicleId = vehicleId;
        snapshot.modelKey = modelKey;
        snapshot.modelName = modelName;
        snapshot.ownerId = ownerId;
        snapshot.ownerName = ownerName;
        snapshot.firstSeenAt = firstSeenAt;
        snapshot.spawnedAt = firstSeenAt;
        snapshot.lastUsedAt = lastUsedAt;
        snapshot.lastSeenAt = now;
        snapshot.lastEventAt = lastEventAt;
        snapshot.lastEvent = lastEvent;
        snapshot.lastEventDetails = lastEventDetails;
        snapshot.worldName =
            loc != null && loc.getWorld() != null
                ? loc.getWorld().getName()
                : null;
        snapshot.x = loc != null ? loc.getX() : 0.0D;
        snapshot.y = loc != null ? loc.getY() : 0.0D;
        snapshot.z = loc != null ? loc.getZ() : 0.0D;
        snapshot.yaw = loc != null ? loc.getYaw() : 0.0F;
        snapshot.pitch = loc != null ? loc.getPitch() : 0.0F;
        snapshot.health = car.getHealth();
        snapshot.healthMax = car.getMaxHealth();
        snapshot.healthPercent = car.getHealthPercent();
        snapshot.fuel = car.getFuelLiters();
        snapshot.fuelMax = car.getFuelTankCapacity();
        snapshot.fuelPercent = car.getFuelPercent();
        snapshot.engineRunning = car.isEngineRunning();
        snapshot.headlightsOn = car.isHeadlightsOn();
        snapshot.speedMps = car.getSpeedMetersPerSecond();
        snapshot.occupiedSeats = car.getOccupiedSeatCount();
        snapshot.totalSeats = car.getSeatCount();
        snapshot.damageFront = car.getFrontHealth();
        snapshot.damageRear = car.getRearHealth();
        snapshot.damageWheelFl = car.getWheelHealth(0);
        snapshot.damageWheelFr = car.getWheelHealth(1);
        snapshot.damageWheelRl = car.getWheelHealth(2);
        snapshot.damageWheelRr = car.getWheelHealth(3);
        snapshot.damageSnapshot = encodeDamageSnapshot(
            car.getDamageHealthSnapshot()
        );

        ownershipDatabase.upsertTelemetryState(snapshot);
        long lastFlushed = carLastEventFlushedAtMs.getOrDefault(vehicleId, 0L);
        if (lastEventAt > lastFlushed && !lastEvent.isBlank()) {
            ownershipDatabase.appendTelemetryEvent(
                vehicleId,
                lastEvent,
                lastEventDetails,
                loc,
                lastEventAt
            );
            carLastEventFlushedAtMs.put(vehicleId, lastEventAt);
        }
    }

    private static String encodeDamageSnapshot(Map<String, Double> damageMap) {
        if (damageMap == null || damageMap.isEmpty()) {
            return "";
        }
        return damageMap
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + formatFinite(e.getValue()))
            .collect(Collectors.joining(";"));
    }

    private static String formatFinite(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return "0";
        }
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private boolean upsertOwnershipForCar(CarEntity car) {
        if (
            ownershipDatabase == null ||
            !ownershipDatabase.isEnabled() ||
            car == null
        ) {
            return false;
        }
        UUID vehicleId = car.getVehicleId();
        UUID ownerId = carOwners.get(vehicleId);
        if (ownerId == null) {
            upsertTelemetryForCar(car, null, null);
            return false;
        }
        String ownerName = carOwnerNames.get(vehicleId);
        if (ownerName == null || ownerName.isBlank()) {
            OfflinePlayer offlineOwner = Bukkit.getOfflinePlayer(ownerId);
            ownerName = offlineOwner.getName();
        }
        boolean ownershipSaved = ownershipDatabase.upsertOwnership(
            vehicleId,
            ownerId,
            ownerName,
            car.getSafeLocation(),
            car.getHealth(),
            car.getFuelLiters(),
            car.isEngineRunning(),
            car.isHeadlightsOn()
        );
        upsertTelemetryForCar(car, ownerId, ownerName);
        return ownershipSaved;
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static final class StoredCarRecord {

        private final UUID vehicleId;
        private final String modelKey;
        private final String worldName;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final double health;
        private final double fuel;
        private final boolean engineRunning;
        private final boolean headlightsOn;
        private final ItemStack[] trunkContents;
        private final Map<String, Double> damageSnapshot;
        private final UUID ownerId;
        private final String ownerName;
        private int restoreAttempts;

        private StoredCarRecord(
            UUID vehicleId,
            String modelKey,
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            double health,
            double fuel,
            boolean engineRunning,
            boolean headlightsOn,
            ItemStack[] trunkContents,
            Map<String, Double> damageSnapshot,
            UUID ownerId,
            String ownerName
        ) {
            this.vehicleId = vehicleId;
            this.modelKey = modelKey;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.health = health;
            this.fuel = fuel;
            this.engineRunning = engineRunning;
            this.headlightsOn = headlightsOn;
            this.trunkContents = trunkContents;
            this.damageSnapshot = damageSnapshot;
            this.ownerId = ownerId;
            this.ownerName = ownerName;
            this.restoreAttempts = 0;
        }

        private static StoredCarRecord fromSection(
            UUID id,
            ConfigurationSection sec,
            String defaultModel,
            java.util.function.Function<String, CarSettings> modelResolver,
            java.util.function.Function<String, String> modelKeyNormalizer
        ) {
            String modelKey = modelKeyNormalizer.apply(sec.getString("model"));
            if (modelKey == null || modelKey.isBlank()) {
                modelKey = defaultModel;
            }
            CarSettings settings = modelResolver.apply(modelKey);
            int trunkSlots = settings == null ? 18 : settings.trunkSlots;
            ItemStack[] trunk = new ItemStack[Math.max(0, trunkSlots)];
            List<?> rawTrunk = sec.getList("trunk");
            if (rawTrunk != null) {
                for (int i = 0; i < trunk.length && i < rawTrunk.size(); i++) {
                    Object element = rawTrunk.get(i);
                    if (element instanceof ItemStack item) {
                        trunk[i] = item;
                    }
                }
            }
            Map<String, Double> damageSnapshot = new HashMap<>();
            ConfigurationSection damageSec = sec.getConfigurationSection(
                "damage"
            );
            if (damageSec != null) {
                for (String dk : damageSec.getKeys(false)) {
                    damageSnapshot.put(dk, damageSec.getDouble(dk));
                }
            }
            UUID ownerId = parseUuid(sec.getString("owner"));
            String ownerName = sec.getString("owner-name", "");
            if (ownerName != null) {
                ownerName = ownerName.trim();
            }
            String worldName = sec.getString("world", "");
            double x = sanitizeFinite(sec.getDouble("x"), 0.0D);
            double y = sanitizeFinite(sec.getDouble("y"), 64.0D);
            double z = sanitizeFinite(sec.getDouble("z"), 0.0D);
            float yaw = (float) sanitizeFinite(
                sec.getDouble("yaw", 0.0D),
                0.0D
            );
            if (!Float.isFinite(yaw)) {
                yaw = 0.0F;
            }
            while (yaw < -180.0F) {
                yaw += 360.0F;
            }
            while (yaw > 180.0F) {
                yaw -= 360.0F;
            }
            double maxHealth =
                settings == null
                    ? Math.max(1.0D, sec.getDouble("health", 1000.0D))
                    : Math.max(1.0D, settings.maxHealth);
            double health = sanitizeFinite(
                sec.getDouble("health", maxHealth),
                maxHealth
            );
            health = Math.max(0.0D, Math.min(maxHealth, health));
            double maxFuel =
                settings == null
                    ? Math.max(0.0D, sec.getDouble("fuel", 35.0D))
                    : Math.max(0.0D, settings.fuelTankCapacity);
            double fuel = sanitizeFinite(
                sec.getDouble("fuel", maxFuel),
                maxFuel
            );
            fuel = Math.max(0.0D, Math.min(maxFuel, fuel));
            return new StoredCarRecord(
                id,
                modelKey,
                worldName,
                x,
                y,
                z,
                yaw,
                health,
                fuel,
                sec.getBoolean("engine-running", true),
                sec.getBoolean("headlights-on", false),
                trunk,
                damageSnapshot,
                ownerId,
                ownerName
            );
        }

        private static double sanitizeFinite(double value, double fallback) {
            return Double.isFinite(value) ? value : fallback;
        }
    }

    private static final class CarReloadSnapshot {

        private final UUID vehicleId;
        private final String modelKey;
        private final Location location;
        private final double health;
        private final double fuelLiters;
        private final boolean engineRunning;
        private final boolean headlightsOn;
        private final ItemStack[] trunkContents;
        private final Map<String, Double> damageHealth;
        private final List<UUID> mountedPlayers;
        private final long spawnedAtMs;
        private final long lastUsedAtMs;
        private final String lastEventType;
        private final String lastEventDetails;
        private final long lastEventAtMs;

        private CarReloadSnapshot(
            UUID vehicleId,
            String modelKey,
            Location location,
            double health,
            double fuelLiters,
            boolean engineRunning,
            boolean headlightsOn,
            ItemStack[] trunkContents,
            Map<String, Double> damageHealth,
            List<UUID> mountedPlayers,
            long spawnedAtMs,
            long lastUsedAtMs,
            String lastEventType,
            String lastEventDetails,
            long lastEventAtMs
        ) {
            this.vehicleId = vehicleId;
            this.modelKey = modelKey;
            this.location = location;
            this.health = health;
            this.fuelLiters = fuelLiters;
            this.engineRunning = engineRunning;
            this.headlightsOn = headlightsOn;
            this.trunkContents = trunkContents;
            this.damageHealth =
                damageHealth == null
                    ? new HashMap<>()
                    : new HashMap<>(damageHealth);
            this.mountedPlayers = mountedPlayers;
            this.spawnedAtMs = spawnedAtMs;
            this.lastUsedAtMs = lastUsedAtMs;
            this.lastEventType = lastEventType;
            this.lastEventDetails = lastEventDetails;
            this.lastEventAtMs = lastEventAtMs;
        }
    }

    private CarEntity findCarByPart(Entity clickedEntity) {
        if (clickedEntity == null) {
            return null;
        }
        UUID partId = clickedEntity.getUniqueId();
        UUID indexedVehicleId = partEntityToCar.get(partId);
        if (indexedVehicleId != null) {
            CarEntity indexedCar = cars.get(indexedVehicleId);
            if (
                indexedCar != null &&
                indexedCar.isValid() &&
                indexedCar.isPart(clickedEntity)
            ) {
                return indexedCar;
            }
            partEntityToCar.remove(partId, indexedVehicleId);
        }
        for (CarEntity car : cars.values()) {
            if (car.isPart(clickedEntity)) {
                partEntityToCar.put(partId, car.getVehicleId());
                return car;
            }
        }
        return null;
    }

    private CarEntity findCarByTrunkPoint(Entity clickedEntity) {
        if (clickedEntity == null) {
            return null;
        }
        UUID partId = clickedEntity.getUniqueId();
        UUID indexedVehicleId = trunkEntityToCar.get(partId);
        if (indexedVehicleId != null) {
            CarEntity indexedCar = cars.get(indexedVehicleId);
            if (
                indexedCar != null &&
                indexedCar.isValid() &&
                indexedCar.isTrunkPoint(clickedEntity)
            ) {
                return indexedCar;
            }
            trunkEntityToCar.remove(partId, indexedVehicleId);
        }
        for (CarEntity car : cars.values()) {
            if (car.isTrunkPoint(clickedEntity)) {
                trunkEntityToCar.put(partId, car.getVehicleId());
                return car;
            }
        }
        return null;
    }

    private CarEntity findCarByFuelPoint(Entity clickedEntity) {
        if (clickedEntity == null) {
            return null;
        }
        UUID partId = clickedEntity.getUniqueId();
        UUID indexedVehicleId = fuelEntityToCar.get(partId);
        if (indexedVehicleId != null) {
            CarEntity indexedCar = cars.get(indexedVehicleId);
            if (
                indexedCar != null &&
                indexedCar.isValid() &&
                indexedCar.isFuelPoint(clickedEntity)
            ) {
                return indexedCar;
            }
            fuelEntityToCar.remove(partId, indexedVehicleId);
        }
        for (CarEntity car : cars.values()) {
            if (car.isFuelPoint(clickedEntity)) {
                fuelEntityToCar.put(partId, car.getVehicleId());
                return car;
            }
        }
        return null;
    }

    private void msg(CommandSender sender, String key, Object... args) {
        Object[] withPrefix = new Object[args.length + 1];
        withPrefix[0] = i18n.tr(sender, "prefix");
        System.arraycopy(args, 0, withPrefix, 1, args.length);
        sender.sendMessage(i18n.tr(sender, key, withPrefix));
    }

    private static Vector forwardFromYaw(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vector(-Math.sin(rad), 0.0D, Math.cos(rad));
    }

    private static String buildBar(double percent) {
        int totalBars = 12;
        int filled = (int) Math.round(
            Math.max(0.0D, Math.min(1.0D, percent)) * totalBars
        );
        StringBuilder sb = new StringBuilder(totalBars);
        for (int i = 0; i < totalBars; i++) {
            sb.append(i < filled ? '|' : '.');
        }
        return sb.toString();
    }

    private void sendFormattedActionBar(
        Player player,
        String configuredFormat,
        String configuredLangKey,
        String fallbackLangKey,
        Map<String, String> values
    ) {
        if (player == null || !player.isOnline()) {
            return;
        }
        String template = configuredFormat;
        if (template == null || template.isBlank()) {
            String langKey =
                configuredLangKey == null || configuredLangKey.isBlank()
                    ? fallbackLangKey
                    : configuredLangKey;
            template = i18n.tr(player, langKey);
            if (
                template == null ||
                template.isBlank() ||
                template.equals(langKey)
            ) {
                template = i18n.tr(player, fallbackLangKey);
            }
        }
        String rendered = PlaceholderLibrary.apply(template, values);
        if (rendered == null || rendered.isBlank()) {
            return;
        }
        player.sendActionBar(colorize(rendered));
    }

    private static int toPercentInt(double percent) {
        return (int) Math.round(
            Math.max(0.0D, Math.min(1.0D, percent)) * 100.0D
        );
    }

    private static double minDamagePercent(double... values) {
        if (values == null || values.length == 0) {
            return 0.0D;
        }
        double min = 1.0D;
        for (double value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private static double parseDouble(String raw, double fallback) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String format1(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static String format2(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String format0(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    private Material parseMaterial(String raw, Material fallback) {
        Material parsed = Material.matchMaterial(raw == null ? "" : raw.trim());
        return parsed == null ? fallback : parsed;
    }

    private String getConfigStringStrict(String path) {
        Object value = cfg().get(path);
        if (value instanceof String text) {
            return text;
        }
        return null;
    }

    private void loadCanisterLocalization() {
        canisterDisplayNamesByLang.clear();
        canisterLoreByLang.clear();
        for (String lang : List.of("en", "ru")) {
            String normalized = i18n.normalizeLang(lang);
            String displayPath =
                "fuel.canister.localization." + normalized + ".display-name";
            String lorePath =
                "fuel.canister.localization." + normalized + ".lore";

            String displayName = getConfigStringStrict(displayPath);
            if (displayName == null || displayName.isBlank()) {
                displayName = canisterDisplayName;
            }

            List<String> lore = cfg().getStringList(lorePath);
            if (lore == null || lore.isEmpty()) {
                lore = canisterLoreLines;
            }

            canisterDisplayNamesByLang.put(normalized, displayName);
            canisterLoreByLang.put(normalized, new ArrayList<>(lore));
        }
    }

    private void loadRepairKitLocalization() {
        repairKitDisplayNamesByLang.clear();
        repairKitLoreByLang.clear();
        for (String lang : List.of("en", "ru")) {
            String normalized = i18n.normalizeLang(lang);
            String displayPath =
                "repair.kit.localization." + normalized + ".display-name";
            String lorePath = "repair.kit.localization." + normalized + ".lore";
            String displayName = getConfigStringStrict(displayPath);
            if (displayName == null || displayName.isBlank()) {
                displayName = repairKitDisplayName;
            }
            List<String> lore = cfg().getStringList(lorePath);
            if (lore == null || lore.isEmpty()) {
                lore = repairKitLoreLines;
            }
            repairKitDisplayNamesByLang.put(normalized, displayName);
            repairKitLoreByLang.put(normalized, new ArrayList<>(lore));
        }
    }

    private void loadKeyLocalization() {
        keyDisplayNamesByLang.clear();
        keyLoreByLang.clear();
        for (String lang : List.of("en", "ru")) {
            String normalized = i18n.normalizeLang(lang);
            String displayPath = "keys.item.localized." + normalized + ".name";
            String lorePath = "keys.item.localized." + normalized + ".lore";
            String displayName = getConfigStringStrict(displayPath);
            if (displayName == null || displayName.isBlank()) {
                displayName = keyItemDisplayName;
            }
            List<String> lore = cfg().getStringList(lorePath);
            if (lore == null || lore.isEmpty()) {
                lore = keyItemLoreLines;
            }
            keyDisplayNamesByLang.put(normalized, displayName);
            keyLoreByLang.put(normalized, new ArrayList<>(lore));
        }
    }

    private String damagePartLabel(Player player, CarEntity.DamagePart part) {
        if (part == null) {
            return i18n.tr(player, "damage.part.front");
        }
        return switch (part) {
            case FRONT -> i18n.tr(player, "damage.part.front");
            case REAR -> i18n.tr(player, "damage.part.rear");
            case WHEEL_FL -> i18n.tr(player, "damage.part.wheel-fl");
            case WHEEL_FR -> i18n.tr(player, "damage.part.wheel-fr");
            case WHEEL_RL -> i18n.tr(player, "damage.part.wheel-rl");
            case WHEEL_RR -> i18n.tr(player, "damage.part.wheel-rr");
        };
    }

    private void loadMenuLocalization() {
        menuLocalizedTexts.clear();
        menuLocalizedLores.clear();
        for (String lang : List.of("en", "ru")) {
            String normalized = i18n.normalizeLang(lang);
            Map<String, String> texts = new HashMap<>();
            Map<String, List<String>> lores = new HashMap<>();
            for (String key : List.of(
                "title",
                "engine-on",
                "engine-off",
                "lights-on",
                "lights-off",
                "trunk",
                "toggle-hint",
                "trunk-hint"
            )) {
                String textPath = "car.menu.localization." + normalized + "." + key;
                String text = getConfigStringStrict(textPath);
                if (text != null && !text.isBlank()) {
                    texts.put(key, text);
                }

                String lorePath = "car.menu.localization." + normalized + "." + key + "-lore";
                List<String> lore = cfg().getStringList(lorePath);
                if (lore != null && !lore.isEmpty()) {
                    lores.put(key, lore);
                }
            }
            menuLocalizedTexts.put(normalized, texts);
            menuLocalizedLores.put(normalized, lores);
        }
    }

    private List<String> menuLore(Player player, String key, List<String> fallback) {
        String lang = i18n.resolvePlayerLang(player);
        String normalizedLang = i18n.normalizeLang(lang);
        Map<String, List<String>> localized = menuLocalizedLores.get(normalizedLang);
        if (localized != null) {
            List<String> value = localized.get(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        Map<String, List<String>> en = menuLocalizedLores.get("en");
        if (en != null) {
            List<String> value = en.get(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return fallback;
    }

    private String menuText(Player player, String key, String i18nFallbackKey) {
        String lang = i18n.resolvePlayerLang(player);
        String normalizedLang = i18n.normalizeLang(lang);
        // Trunk name/hint: always use bundle to avoid mojibake (never use config or encrypted default)
        String explicitI18nKey = menuKeyToExplicitI18nKey(key);
        if (explicitI18nKey == null && i18nFallbackKey != null) {
            String f = i18nFallbackKey.trim().toLowerCase(Locale.ROOT);
            if (f.contains("trunk")) {
                explicitI18nKey = f.contains("hint")
                    ? "menu.car.trunk.hint"
                    : "menu.car.trunk";
            }
        }
        if (explicitI18nKey != null) {
            String fromBundle = i18n.trByLang(normalizedLang, explicitI18nKey);
            if (
                !fromBundle.equals(explicitI18nKey) &&
                !looksLikeLangKey(fromBundle)
            ) {
                return fromBundle;
            }
            String hardUtf8 = trunkMenuFallbackUtf8(
                normalizedLang,
                explicitI18nKey
            );
            if (hardUtf8 != null) {
                return hardUtf8;
            }
        }
        Map<String, String> localized = menuLocalizedTexts.get(normalizedLang);
        if (localized != null) {
            String value = localized.get(key);
            if (value != null && !value.isBlank()) {
                if (looksLikeLangKey(value)) {
                    String indirect = i18n.trByLang(normalizedLang, value);
                    if (!indirect.equals(value)) {
                        return indirect;
                    }
                } else if (looksLikeMojibake(value)) {
                    String clean = i18n.trByLang(
                        normalizedLang,
                        i18nFallbackKey
                    );
                    if (!clean.equals(i18nFallbackKey)) {
                        return clean;
                    }
                } else {
                    String trunkBundleKey = menuKeyToExplicitI18nKey(key);
                    if (trunkBundleKey != null) {
                        String fromBundle = i18n.trByLang(
                            normalizedLang,
                            trunkBundleKey
                        );
                        if (
                            !fromBundle.equals(trunkBundleKey) &&
                            !looksLikeLangKey(fromBundle)
                        ) {
                            return fromBundle;
                        }
                    }
                    return normalizeKnownMenuTypos(normalizedLang, key, value);
                }
            }
        }
        Map<String, String> en = menuLocalizedTexts.get("en");
        if (en != null) {
            String value = en.get(key);
            if (value != null && !value.isBlank()) {
                if (looksLikeLangKey(value)) {
                    String indirect = i18n.trByLang(normalizedLang, value);
                    if (!indirect.equals(value)) {
                        return indirect;
                    }
                } else if (looksLikeMojibake(value)) {
                    String clean = i18n.trByLang(
                        normalizedLang,
                        i18nFallbackKey
                    );
                    if (!clean.equals(i18nFallbackKey)) {
                        return clean;
                    }
                } else {
                    String trunkBundleKey = menuKeyToExplicitI18nKey(key);
                    if (trunkBundleKey != null) {
                        String fromBundle = i18n.trByLang(
                            normalizedLang,
                            trunkBundleKey
                        );
                        if (
                            !fromBundle.equals(trunkBundleKey) &&
                            !looksLikeLangKey(fromBundle)
                        ) {
                            return fromBundle;
                        }
                    }
                    return normalizeKnownMenuTypos(normalizedLang, key, value);
                }
            }
        }
        String translated = i18n.tr(player, i18nFallbackKey);
        if (
            !translated.equals(i18nFallbackKey) && !looksLikeLangKey(translated)
        ) {
            return translated;
        }
        String directByLang = i18n.trByLang(normalizedLang, i18nFallbackKey);
        if (!directByLang.equals(i18nFallbackKey)) {
            return directByLang;
        }
        return safeDefaultMenuText(normalizedLang, key);
    }

    private static String menuKeyToExplicitI18nKey(String menuKey) {
        if (menuKey == null) {
            return null;
        }
        String k = menuKey.trim().toLowerCase(Locale.ROOT);
        if (k.equals("trunk")) {
            return "menu.car.trunk";
        }
        if (k.equals("trunk-hint")) {
            return "menu.car.trunk.hint";
        }
        if (k.contains("trunk")) {
            return k.contains("hint")
                ? "menu.car.trunk.hint"
                : "menu.car.trunk";
        }
        return null;
    }

    /** Fallback when bundle has no translation for trunk menu (avoids mojibake from encrypted default). */
    private static String trunkMenuFallbackUtf8(String lang, String key) {
        boolean ru = "ru".equalsIgnoreCase(lang);
        if ("menu.car.trunk".equals(key)) {
            return ru
                ? "&6\u0411\u0430\u0433\u0430\u0436\u043d\u0438\u043a"
                : "&6Open trunk";
        }
        if ("menu.car.trunk.hint".equals(key)) {
            return ru
                ? "&7\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0431\u0430\u0433\u0430\u0436\u043d\u0438\u043a"
                : "&7Open trunk";
        }
        return null;
    }

    private static boolean looksLikeLangKey(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        return trimmed.matches("[a-z0-9_-]+(\\\\.[a-z0-9_-]+){1,5}");
    }

    private static boolean looksLikeMojibake(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        int suspicious = 0;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            if (
                ch == 'Р' ||
                ch == 'С' ||
                ch == 'Ð' ||
                ch == 'Ñ' ||
                ch == '\uFFFD'
            ) {
                suspicious++;
            }
        }
        if (suspicious < 4) {
            return false;
        }
        double ratio = (double) suspicious / (double) Math.max(1, len);
        return ratio >= 0.18D;
    }

    private static String safeDefaultMenuText(String lang, String key) {
        boolean ru = "ru".equalsIgnoreCase(lang);
        return switch (key) {
            case "title" -> ru
                ? "&8РњРµРЅСЋ Р°РІС‚РѕРјРѕР±РёР»СЏ"
                : "&8Car Menu";
            case "engine-on" -> ru
                ? "&aР”РІРёРіР°С‚РµР»СЊ: РІРєР»СЋС‡РµРЅ"
                : "&aEngine: ON";
            case "engine-off" -> ru
                ? "&cР”РІРёРіР°С‚РµР»СЊ: РІС‹РєР»СЋС‡РµРЅ"
                : "&cEngine: OFF";
            case "lights-on" -> ru
                ? "&eР¤Р°СЂС‹: РІРєР»СЋС‡РµРЅС‹"
                : "&eHeadlights: ON";
            case "lights-off" -> ru
                ? "&7Р¤Р°СЂС‹: РІС‹РєР»СЋС‡РµРЅС‹"
                : "&7Headlights: OFF";
            case "trunk" -> ru ? "&6Р‘Р°РіР°Р¶РЅРёРє" : "&6Open trunk";
            case "toggle-hint" -> ru
                ? "&7РќР°Р¶РјРёС‚Рµ, С‡С‚РѕР±С‹ РїРµСЂРµРєР»СЋС‡РёС‚СЊ"
                : "&7Click to toggle";
            case "trunk-hint" -> ru
                ? "&7РћС‚РєСЂС‹С‚СЊ Р±Р°РіР°Р¶РЅРёРє"
                : "&7Open trunk";
            default -> ru ? "&7РњРµРЅСЋ" : "&7Menu";
        };
    }

    private String normalizeKnownMenuTypos(
        String lang,
        String key,
        String value
    ) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String normalized = value
            .replace('\u00A7', '&')
            .replaceAll("(?i)&[0-9a-fk-or]", "")
            .trim()
            .toLowerCase(Locale.ROOT);
        if (
            key.equals("trunk") &&
            (normalized.equals("truck") ||
                normalized.equals("trunk") ||
                normalized.equals("open trunk") ||
                normalized.equals("opet truck"))
        ) {
            return i18n.trByLang(lang, "menu.car.trunk");
        }
        if (
            key.equals("trunk-hint") &&
            (normalized.equals("truck") ||
                normalized.equals("trunk") ||
                normalized.equals("open trunk") ||
                normalized.equals("opet truck"))
        ) {
            return i18n.trByLang(lang, "menu.car.trunk.hint");
        }
        return value;
    }

    private static String defaultMenuText(String lang, String key) {
        boolean ru = "ru".equalsIgnoreCase(lang);
        return switch (key) {
            case "title" -> ru
                ? "&8РњРµРЅСЋ Р°РІС‚РѕРјРѕР±РёР»СЏ"
                : "&8Car Menu";
            case "engine-on" -> ru
                ? "&aР”РІРёРіР°С‚РµР»СЊ: РІРєР»СЋС‡РµРЅ"
                : "&aEngine: ON";
            case "engine-off" -> ru
                ? "&cР”РІРёРіР°С‚РµР»СЊ: РІС‹РєР»СЋС‡РµРЅ"
                : "&cEngine: OFF";
            case "lights-on" -> ru
                ? "&eР¤Р°СЂС‹: РІРєР»СЋС‡РµРЅС‹"
                : "&eHeadlights: ON";
            case "lights-off" -> ru
                ? "&7Р¤Р°СЂС‹: РІС‹РєР»СЋС‡РµРЅС‹"
                : "&7Headlights: OFF";
            case "trunk" -> ru ? "&6Р‘Р°РіР°Р¶РЅРёРє" : "&6Open trunk";
            case "toggle-hint" -> ru
                ? "&7РќР°Р¶РјРёС‚Рµ, С‡С‚РѕР±С‹ РїРµСЂРµРєР»СЋС‡РёС‚СЊ"
                : "&7Click to toggle";
            case "trunk-hint" -> ru
                ? "&7РћС‚РєСЂС‹С‚СЊ Р±Р°РіР°Р¶РЅРёРє"
                : "&7Open trunk";
            default -> ru ? "&7РњРµРЅСЋ" : "&7Menu";
        };
    }

    private int clampMenuSlot(int slot, int fallback, int maxSlot) {
        if (slot < 0 || slot > maxSlot) {
            return fallback;
        }
        return slot;
    }

    private void normalizeMenuLayout() {
        int maxSlot = Math.max(0, menuSize - 1);
        menuSlotEngine = clampMenuSlot(menuSlotEngine, 11, maxSlot);
        menuSlotLights = clampMenuSlot(menuSlotLights, 15, maxSlot);
        menuSlotTrunk = clampMenuSlot(menuSlotTrunk, 13, maxSlot);

        Set<Integer> used = new HashSet<>();
        used.add(menuSlotEngine);

        if (used.contains(menuSlotLights)) {
            menuSlotLights = firstFreeMenuSlot(used, 15, maxSlot);
        }
        used.add(menuSlotLights);

        if (menuTrunkEnabled) {
            if (used.contains(menuSlotTrunk)) {
                menuSlotTrunk = firstFreeMenuSlot(used, 13, maxSlot);
            }
            used.add(menuSlotTrunk);
        }
    }

    private static int firstFreeMenuSlot(
        Set<Integer> used,
        int preferred,
        int maxSlot
    ) {
        int clampedPreferred = Math.max(0, Math.min(maxSlot, preferred));
        if (!used.contains(clampedPreferred)) {
            return clampedPreferred;
        }
        for (int i = 0; i <= maxSlot; i++) {
            if (!used.contains(i)) {
                return i;
            }
        }
        return clampedPreferred;
    }

    private void loadSoundsConfig() {
        this.soundsEnabled = cfg().getBoolean("sounds.enabled", true);
        this.soundsCategory = parseSoundCategory(
            cfg().getString("sounds.category", "PLAYERS"),
            SoundCategory.PLAYERS
        );

        this.soundSeatOpen = loadSoundProfile(
            "sounds.events.seat-open",
            "coolcars.open",
            1.0F,
            1.0F,
            18.0D,
            6,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0,
            false,
            true
        );
        this.soundSeatClose = loadSoundProfile(
            "sounds.events.seat-close",
            "coolcars.close",
            0.95F,
            1.0F,
            18.0D,
            6,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0,
            false,
            true
        );
        this.soundEngineStart = loadSoundProfile(
            "sounds.events.engine-start",
            "coolcars.start",
            1.05F,
            1.0F,
            20.0D,
            1,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0,
            false,
            true
        );
        this.soundEngineStop = loadSoundProfile(
            "sounds.events.engine-stop",
            "coolcars.stop",
            1.0F,
            1.0F,
            18.0D,
            1,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0,
            false,
            true
        );
        this.soundEngineIdle = loadSoundProfile(
            "sounds.events.engine-idle",
            "coolcars.idle",
            0.72F,
            0.93F,
            24.0D,
            180,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0,
            false,
            true
        );
        this.soundHorn = loadSoundProfile(
            "sounds.events.horn",
            cfg().getString("car.horn.sound", "coolcars.horn"),
            (float) cfg().getDouble("car.horn.volume", 1.25D),
            (float) cfg().getDouble("car.horn.pitch", 1.0D),
            36.0D,
            1,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            Math.max(1, cfg().getInt("car.horn.cooldown-ticks", 24)),
            cfg().getBoolean("car.horn.require-moving", false),
            true
        );
        this.soundDriving = loadSoundProfile(
            "sounds.events.driving",
            "coolcars.driving",
            0.9F,
            1.0F,
            32.0D,
            36,
            1.0D,
            0.0D,
            0.0D,
            0.0D,
            0,
            false,
            true
        );
        this.soundGlovebox = loadSoundProfile(
            "sounds.events.glovebox",
            "coolcars.glovebox",
            0.9F,
            1.0F,
            14.0D,
            6,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0,
            false,
            true
        );
        this.soundFilling = loadSoundProfile(
            "sounds.events.filling",
            "coolcars.filling",
            0.85F,
            1.0F,
            18.0D,
            8,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0,
            false,
            true
        );
        this.soundFilling = enforceLoopingSoundGuard(this.soundFilling, 8);
        this.soundRepair = loadSoundProfile(
            "sounds.events.repair",
            "coolcars.repair",
            0.8F,
            1.0F,
            18.0D,
            8,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0,
            false,
            true
        );
        this.soundRepair = enforceLoopingSoundGuard(this.soundRepair, 8);
        this.soundHit = loadSoundProfile(
            "sounds.events.hit",
            "coolcars.hit",
            1.1F,
            1.0F,
            28.0D,
            6,
            4.0D,
            0.0D,
            0.0D,
            0.0D,
            8,
            false,
            true
        );
        this.soundCrash = loadSoundProfile(
            "sounds.events.crash",
            "coolcars.crash",
            1.25F,
            1.0F,
            34.0D,
            12,
            10.0D,
            0.0D,
            0.0D,
            0.0D,
            18,
            false,
            true
        );
        this.soundLanding = loadSoundProfile(
            "sounds.events.landing",
            "coolcars.landing",
            1.15F,
            0.95F,
            26.0D,
            8,
            4.0D,
            0.0D,
            0.0D,
            0.0D,
            8,
            false,
            true
        );
        this.drivingToIdleDelayTicks = Math.max(
            0,
            cfg().getInt("sounds.transition.driving-to-idle-delay-ticks", 3)
        );
        this.idleToDrivingDelayTicks = Math.max(
            0,
            cfg().getInt("sounds.transition.idle-to-driving-delay-ticks", 2)
        );
        this.soundSpatialMinVolumeFactor = clamp(
            cfg().getDouble("sounds.spatial.min-volume-factor", 0.16D),
            0.0D,
            0.95D
        );
        this.soundSpatialNearDistanceBlocks = Math.max(
            0.0D,
            cfg().getDouble("sounds.spatial.near-distance-blocks", 2.0D)
        );
        this.soundSpatialFalloffCurvePower = Math.max(
            0.35D,
            cfg().getDouble("sounds.spatial.falloff-curve-power", 1.25D)
        );
        this.meleeDamageToCarEnabled = cfg().getBoolean(
            "car.combat.vehicle-damage.melee.enabled",
            true
        );
        this.meleeDamageMultiplier = Math.max(
            0.0D,
            cfg().getDouble("car.combat.vehicle-damage.melee.multiplier", 1.0D)
        );
        this.meleePlayHitSound = cfg().getBoolean(
            "car.combat.vehicle-damage.melee.play-hit-sound",
            true
        );
        this.projectileDamageEnabled = cfg().getBoolean(
            "car.combat.vehicle-damage.projectiles.enabled",
            true
        );
        this.projectileStickArrows = cfg().getBoolean(
            "car.combat.vehicle-damage.projectiles.stick-arrows",
            true
        );
        this.projectileNotifyShooter = cfg().getBoolean(
            "car.combat.vehicle-damage.projectiles.notify-shooter",
            true
        );
        this.projectileNotifyDriver = cfg().getBoolean(
            "car.combat.vehicle-damage.projectiles.notify-driver",
            true
        );
        this.projectileNotifyFriendlyBlocked = cfg().getBoolean(
            "car.combat.vehicle-damage.projectiles.notify-friendly-blocked",
            true
        );
        this.projectileIgnoreOccupants = cfg().getBoolean(
            "car.combat.vehicle-damage.projectiles.ignore-car-occupants",
            true
        );
        this.projectilePlayHitSound = cfg().getBoolean(
            "car.combat.vehicle-damage.projectiles.play-hit-sound",
            true
        );
        this.projectileBaseDamage = Math.max(
            0.0D,
            cfg().getDouble(
                "car.combat.vehicle-damage.projectiles.base-damage",
                5.0D
            )
        );
        this.projectileArrowDamageMultiplier = Math.max(
            0.0D,
            cfg().getDouble(
                "car.combat.vehicle-damage.projectiles.arrow-multiplier",
                1.0D
            )
        );
        this.projectileTridentDamageMultiplier = Math.max(
            0.0D,
            cfg().getDouble(
                "car.combat.vehicle-damage.projectiles.trident-multiplier",
                1.4D
            )
        );
        this.projectileOtherDamageMultiplier = Math.max(
            0.0D,
            cfg().getDouble(
                "car.combat.vehicle-damage.projectiles.other-multiplier",
                0.85D
            )
        );
        this.projectileSpeedDamageFactor = Math.max(
            0.0D,
            cfg().getDouble(
                "car.combat.vehicle-damage.projectiles.speed-damage-factor",
                0.55D
            )
        );
    }

    private SoundProfile loadSoundProfile(
        String path,
        String key,
        float volume,
        float pitch,
        double radius,
        int intervalTicks,
        double minSpeed,
        double driftThreshold,
        double minSlipSpeed,
        double minSteer,
        int cooldownTicks,
        boolean requireMoving,
        boolean preventOverlap
    ) {
        return new SoundProfile(
            cfg().getBoolean(path + ".enabled", true),
            cfg().getString(path + ".key", key),
            (float) cfg().getDouble(path + ".volume", volume),
            (float) cfg().getDouble(path + ".pitch", pitch),
            Math.max(1.0D, cfg().getDouble(path + ".radius", radius)),
            Math.max(1, cfg().getInt(path + ".interval-ticks", intervalTicks)),
            Math.max(0.0D, cfg().getDouble(path + ".min-speed", minSpeed)),
            Math.max(
                0.0D,
                cfg().getDouble(path + ".drift-threshold", driftThreshold)
            ),
            Math.max(
                0.0D,
                cfg().getDouble(path + ".min-slip-speed", minSlipSpeed)
            ),
            Math.max(0.0D, cfg().getDouble(path + ".min-steer", minSteer)),
            Math.max(0, cfg().getInt(path + ".cooldown-ticks", cooldownTicks)),
            cfg().getBoolean(path + ".require-moving", requireMoving),
            cfg().getBoolean(path + ".prevent-overlap", preventOverlap)
        );
    }

    private SoundProfile enforceLoopingSoundGuard(
        SoundProfile profile,
        int minIntervalTicks
    ) {
        if (profile == null) {
            return null;
        }
        int interval = Math.max(
            Math.max(1, minIntervalTicks),
            profile.intervalTicks
        );
        int cooldown = Math.max(
            profile.cooldownTicks,
            Math.max(2, interval / 2)
        );
        return new SoundProfile(
            profile.enabled,
            profile.key,
            profile.volume,
            profile.pitch,
            profile.radius,
            interval,
            profile.minSpeed,
            profile.driftThreshold,
            profile.minSlipSpeed,
            profile.minSteer,
            cooldown,
            profile.requireMoving,
            true
        );
    }

    private SoundCategory parseSoundCategory(
        String raw,
        SoundCategory fallback
    ) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return SoundCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private void emitContinuousVehicleSounds(CarEntity car, int tick) {
        if (!soundsEnabled || car == null || !car.isValid()) {
            return;
        }
        UUID vehicleId = car.getVehicleId();
        Entity driver = car.getDriver();
        boolean hasDriver = driver != null;
        double speed = car.getSpeedMetersPerSecond();
        boolean engineRunning = car.isEngineRunning();
        boolean canIdle =
            engineRunning && soundEngineIdle != null && soundEngineIdle.enabled;
        boolean canPlayDriving =
            canIdle &&
            soundDriving != null &&
            soundDriving.enabled &&
            speed >= soundDriving.minSpeed;
        boolean hadDrivingScheduled = drivingSoundNextTick.containsKey(
            vehicleId
        );

        if (!canIdle) {
            boolean hadContinuousState =
                drivingSoundNextTick.containsKey(vehicleId) ||
                idleSoundNextTick.containsKey(vehicleId) ||
                pendingIdleStartTick.containsKey(vehicleId) ||
                pendingDriveStartTick.containsKey(vehicleId);
            if (hadContinuousState) {
                stopContinuousCarSounds(car);
            } else {
                clearVehicleSoundState(vehicleId);
            }
        } else if (canPlayDriving) {
            pendingIdleStartTick.remove(vehicleId);
            if (idleSoundNextTick.containsKey(vehicleId)) {
                stopConfiguredSoundAtCar(car, soundEngineIdle);
                idleSoundNextTick.remove(vehicleId);
                if (idleToDrivingDelayTicks > 0) {
                    pendingDriveStartTick.put(
                        vehicleId,
                        tick + idleToDrivingDelayTicks
                    );
                }
            }
            int allowDriveAt = pendingDriveStartTick.getOrDefault(
                vehicleId,
                tick
            );
            if (tick >= allowDriveAt) {
                pendingDriveStartTick.remove(vehicleId);
                int next = drivingSoundNextTick.getOrDefault(vehicleId, 0);
                if (tick >= next) {
                    float speedPitch = (float) clamp(
                        soundDriving.pitch +
                            clamp(speed / 28.0D, 0.0D, 1.0D) * 0.16D,
                        0.5D,
                        2.0D
                    );
                    playSoundAtCar(car, soundDriving, speedPitch);
                    drivingSoundNextTick.put(
                        vehicleId,
                        tick + Math.max(1, soundDriving.intervalTicks)
                    );
                }
            }
        } else {
            pendingDriveStartTick.remove(vehicleId);
            if (hadDrivingScheduled) {
                stopConfiguredSoundAtCar(car, soundDriving);
                drivingSoundNextTick.remove(vehicleId);
                if (drivingToIdleDelayTicks > 0) {
                    pendingIdleStartTick.put(
                        vehicleId,
                        tick + drivingToIdleDelayTicks
                    );
                }
            }
            int allowIdleAt = pendingIdleStartTick.getOrDefault(
                vehicleId,
                tick
            );
            if (tick >= allowIdleAt) {
                pendingIdleStartTick.remove(vehicleId);
                int nextIdle = idleSoundNextTick.getOrDefault(vehicleId, 0);
                if (tick >= nextIdle) {
                    playSoundAtCar(car, soundEngineIdle);
                    idleSoundNextTick.put(
                        vehicleId,
                        tick + Math.max(1, soundEngineIdle.intervalTicks)
                    );
                }
            }
        }

        if (
            soundHit != null &&
            soundHit.enabled &&
            car.isRamHitThisTick() &&
            car.getRamHitSpeedThisTick() >= soundHit.minSpeed
        ) {
            playSoundAtCar(car, soundHit);
        }
        if (
            soundCrash != null &&
            soundCrash.enabled &&
            car.isCrashThisTick() &&
            Math.max(
                car.getCrashSpeedThisTick(),
                car.getCrashDeltaSpeedThisTick()
            ) >=
            soundCrash.minSpeed * 0.85D
        ) {
            float crashPitch = (float) clamp(
                soundCrash.pitch +
                    clamp(car.getCrashDeltaSpeedThisTick() / 8.0D, 0.0D, 1.0D) *
                    0.10D,
                0.5D,
                2.0D
            );
            playSoundAtCar(car, soundCrash, crashPitch);
        }
        if (car.isHardLandingThisTick()) {
            double impactSpeed = car.getLandingImpactSpeedThisTick();
            SoundProfile landingProfile =
                soundLanding != null && soundLanding.enabled
                    ? soundLanding
                    : soundCrash;
            if (
                landingProfile != null &&
                landingProfile.enabled &&
                impactSpeed >= landingProfile.minSpeed
            ) {
                float landingPitch = (float) clamp(
                    landingProfile.pitch +
                        clamp(impactSpeed / 16.0D, 0.0D, 1.0D) *
                        (landingProfile == soundCrash ? 0.10D : 0.08D),
                    0.5D,
                    2.0D
                );
                playSoundAtCar(car, landingProfile, landingPitch);
            }
        }
    }

    private void playSoundAtCar(CarEntity car, SoundProfile profile) {
        if (car == null || profile == null) {
            return;
        }
        if (!shouldPlayCarSound(car, profile)) {
            return;
        }
        playConfiguredSound(car.getLocation(), profile, profile.pitch);
    }

    private void playSoundAtCar(
        CarEntity car,
        SoundProfile profile,
        float pitchOverride
    ) {
        if (car == null || profile == null) {
            return;
        }
        if (!shouldPlayCarSound(car, profile)) {
            return;
        }
        playConfiguredSound(car.getLocation(), profile, pitchOverride);
    }

    private boolean shouldPlayCarSound(CarEntity car, SoundProfile profile) {
        if (car == null || profile == null || profile.key == null) {
            return false;
        }
        int gateTicks = Math.max(
            profile.cooldownTicks,
            profile.preventOverlap ? profile.intervalTicks : 0
        );
        if (gateTicks <= 0) {
            return true;
        }
        String gateKey = car.getVehicleId() + "|" + profile.key;
        long now = System.currentTimeMillis();
        long until = soundCooldownUntilMs.getOrDefault(gateKey, 0L);
        if (now < until) {
            return false;
        }
        soundCooldownUntilMs.put(gateKey, now + gateTicks * 50L);
        return true;
    }

    private void clearVehicleSoundState(UUID vehicleId) {
        if (vehicleId == null) {
            return;
        }
        drivingSoundNextTick.remove(vehicleId);
        idleSoundNextTick.remove(vehicleId);
        pendingIdleStartTick.remove(vehicleId);
        pendingDriveStartTick.remove(vehicleId);
        String prefix = vehicleId + "|";
        soundCooldownUntilMs.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private void stickArrowToCarPart(AbstractArrow arrow, Entity hitPart) {
        if (
            arrow == null ||
            hitPart == null ||
            !arrow.isValid() ||
            !hitPart.isValid()
        ) {
            return;
        }
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setDamage(0.0D);
        arrow.setCritical(false);
        arrow.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        arrow.setGravity(false);
        if (!hitPart.getPassengers().contains(arrow)) {
            hitPart.addPassenger(arrow);
        }
    }

    private void refreshCarChunkTickets(UUID vehicleId, CarEntity car) {
        if (vehicleId == null || car == null) {
            return;
        }
        Location location = car.getSafeLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }
        World world = location.getWorld();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        String centerToken = chunkTicketToken(world, chunkX, chunkZ);
        String previousCenter = carChunkCenterTokenCache.put(
            vehicleId,
            centerToken
        );
        if (centerToken.equals(previousCenter)) {
            return;
        }

        Set<String> active = carChunkTickets.computeIfAbsent(vehicleId, key ->
            new HashSet<>()
        );
        Set<String> required = new HashSet<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                String token = chunkTicketToken(world, x, z);
                required.add(token);
                if (active.contains(token)) {
                    continue;
                }
                Chunk chunk = world.getChunkAt(x, z);
                if (!chunk.isLoaded()) {
                    chunk.load();
                }
                chunk.addPluginChunkTicket(this);
                active.add(token);
            }
        }

        if (active.isEmpty()) {
            return;
        }
        Set<String> stale = new HashSet<>(active);
        stale.removeAll(required);
        for (String token : stale) {
            removeChunkTicket(token);
            active.remove(token);
        }
        if (active.isEmpty()) {
            carChunkTickets.remove(vehicleId);
        }
    }

    private void clearCarChunkTickets(UUID vehicleId) {
        if (vehicleId == null) {
            return;
        }
        carChunkCenterTokenCache.remove(vehicleId);
        Set<String> tickets = carChunkTickets.remove(vehicleId);
        if (tickets == null || tickets.isEmpty()) {
            return;
        }
        for (String token : tickets) {
            removeChunkTicket(token);
        }
    }

    private void clearAllCarChunkTickets() {
        if (carChunkTickets.isEmpty()) {
            carChunkCenterTokenCache.clear();
            return;
        }
        for (Set<String> tickets : carChunkTickets.values()) {
            if (tickets == null || tickets.isEmpty()) {
                continue;
            }
            for (String token : tickets) {
                removeChunkTicket(token);
            }
        }
        carChunkTickets.clear();
        carChunkCenterTokenCache.clear();
    }

    private static String chunkTicketToken(
        World world,
        int chunkX,
        int chunkZ
    ) {
        return (world.getUID() + ":" + chunkX + ":" + chunkZ);
    }

    private void removeChunkTicket(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String[] parts = token.split(":", 3);
        if (parts.length != 3) {
            return;
        }
        UUID worldId = parseUuid(parts[0]);
        if (worldId == null) {
            return;
        }
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            Chunk chunk = world.getChunkAt(x, z);
            chunk.removePluginChunkTicket(this);
        } catch (NumberFormatException ignored) {}
    }

    private void playConfiguredSound(
        Location origin,
        SoundProfile profile,
        float pitchValue
    ) {
        if (
            !soundsEnabled ||
            profile == null ||
            !profile.enabled ||
            profile.key == null ||
            profile.key.isBlank() ||
            origin == null ||
            origin.getWorld() == null
        ) {
            return;
        }
        World world = origin.getWorld();
        double radius = Math.max(1.0D, profile.radius);
        Collection<? extends Player> nearbyPlayers = world.getNearbyPlayers(
            origin,
            radius
        );
        if (nearbyPlayers.isEmpty()) {
            return;
        }
        double nearDistance = clamp(
            soundSpatialNearDistanceBlocks,
            0.0D,
            radius
        );
        double maxDistSq = radius * radius;
        double nearDistSq = nearDistance * nearDistance;
        double effectiveRange = Math.max(0.0001D, radius - nearDistance);
        double minFactor = clamp(soundSpatialMinVolumeFactor, 0.0D, 0.95D);
        double curvePower = Math.max(0.35D, soundSpatialFalloffCurvePower);
        double ox = origin.getX();
        double oy = origin.getY();
        double oz = origin.getZ();
        for (Player listener : nearbyPlayers) {
            Location listenerLoc = listener.getLocation();
            double dx = listenerLoc.getX() - ox;
            double dy = listenerLoc.getY() - oy;
            double dz = listenerLoc.getZ() - oz;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > maxDistSq) {
                continue;
            }
            double factor;
            if (distSq <= nearDistSq) {
                factor = 1.0D;
            } else {
                double dist = Math.sqrt(distSq);
                double normalized =
                    1.0D - ((dist - nearDistance) / effectiveRange);
                factor = Math.pow(clamp(normalized, 0.0D, 1.0D), curvePower);
            }
            if (factor <= 0.0D) {
                continue;
            }
            float scaledVolume = (float) (profile.volume *
                (minFactor + (1.0D - minFactor) * factor));
            listener.playSound(
                origin,
                profile.key,
                soundsCategory,
                scaledVolume,
                pitchValue
            );
        }
    }

    private void stopConfiguredSoundAtCar(CarEntity car, SoundProfile profile) {
        if (
            car == null ||
            profile == null ||
            profile.key == null ||
            profile.key.isBlank()
        ) {
            return;
        }
        Location origin = car.getLocation();
        if (origin == null || origin.getWorld() == null) {
            return;
        }
        World world = origin.getWorld();
        double radius = Math.max(1.0D, profile.radius);
        for (Player listener : world.getNearbyPlayers(origin, radius)) {
            listener.stopSound(profile.key, soundsCategory);
        }
        soundCooldownUntilMs.remove(car.getVehicleId() + "|" + profile.key);
    }

    private void stopConfiguredSoundForPlayer(
        UUID playerId,
        SoundProfile profile
    ) {
        if (playerId == null || profile == null || profile.key == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.stopSound(profile.key, soundsCategory);
        }
    }

    private void stopContinuousCarSounds(CarEntity car) {
        if (car == null) {
            return;
        }
        stopConfiguredSoundAtCar(car, soundDriving);
        stopConfiguredSoundAtCar(car, soundEngineIdle);
        clearVehicleSoundState(car.getVehicleId());
    }

    private void stopDrivingSoundState(CarEntity car) {
        if (car == null) {
            return;
        }
        stopConfiguredSoundAtCar(car, soundDriving);
        UUID vehicleId = car.getVehicleId();
        drivingSoundNextTick.remove(vehicleId);
        pendingDriveStartTick.remove(vehicleId);
    }

    private void registerCarPartMappings(CarEntity car) {
        if (car == null) {
            return;
        }
        UUID carId = car.getVehicleId();
        for (UUID partId : car.getPartEntityIds()) {
            if (partId != null) {
                partEntityToCar.put(partId, carId);
            }
        }
        UUID fuelId = car.getFuelPointEntityId();
        if (fuelId != null) {
            fuelEntityToCar.put(fuelId, carId);
        }
        UUID trunkId = car.getTrunkPointEntityId();
        if (trunkId != null) {
            trunkEntityToCar.put(trunkId, carId);
        }
    }

    private void unregisterCarPartMappings(CarEntity car) {
        if (car == null) {
            return;
        }
        UUID carId = car.getVehicleId();
        for (UUID partId : car.getPartEntityIds()) {
            if (partId != null) {
                partEntityToCar.remove(partId, carId);
            }
        }
        UUID fuelId = car.getFuelPointEntityId();
        if (fuelId != null) {
            fuelEntityToCar.remove(fuelId, carId);
        }
        UUID trunkId = car.getTrunkPointEntityId();
        if (trunkId != null) {
            trunkEntityToCar.remove(trunkId, carId);
        }
    }

    private void unregisterCarPartMappings(UUID vehicleId) {
        if (vehicleId == null) {
            return;
        }
        partEntityToCar.values().removeIf(vehicleId::equals);
        fuelEntityToCar.values().removeIf(vehicleId::equals);
        trunkEntityToCar.values().removeIf(vehicleId::equals);
    }

    private void clearPlayerControlState(UUID playerId) {
        if (playerId == null) {
            return;
        }
        driverToCar.remove(playerId);
        playerToCarCache.remove(playerId);
        stopRefuelTask(playerId);
        stopRepairTask(playerId);
        hornCooldownUntilMs.remove(playerId);
        jumpHeldByPlayer.remove(playerId);
        stopRepairHitboxPreview(playerId);
    }

    private void unmountPlayerFromCars(
        UUID playerId,
        boolean playSeatCloseSound
    ) {
        if (playerId == null || cars.isEmpty()) {
            return;
        }
        CarEntity direct = findCarByPlayer(playerId);
        if (
            direct != null && direct.isValid() && direct.unmountPlayer(playerId)
        ) {
            playerToCarCache.remove(playerId);
            stopDrivingSoundState(direct);
            if (playSeatCloseSound) {
                playSoundAtCar(direct, soundSeatClose);
            }
            return;
        }
        for (CarEntity car : cars.values()) {
            if (car == null || !car.isValid() || !car.unmountPlayer(playerId)) {
                continue;
            }
            playerToCarCache.remove(playerId);
            stopDrivingSoundState(car);
            if (playSeatCloseSound) {
                playSoundAtCar(car, soundSeatClose);
            }
            return;
        }
    }

    private void tryPlayHorn(Player player, CarEntity car) {
        if (player == null || car == null || !car.isValid()) {
            return;
        }
        if (!soundsEnabled || soundHorn == null || !soundHorn.enabled) {
            return;
        }
        if (
            soundHorn.requireMoving &&
            car.getSpeedMetersPerSecond() < soundHorn.minSpeed
        ) {
            return;
        }
        long now = System.currentTimeMillis();
        long until = hornCooldownUntilMs.getOrDefault(player.getUniqueId(), 0L);
        if (now < until) {
            return;
        }
        int cooldownTicks = Math.max(1, soundHorn.cooldownTicks);
        hornCooldownUntilMs.put(
            player.getUniqueId(),
            now + (long) cooldownTicks * 50L
        );
        playSoundAtCar(car, soundHorn);
    }

    private void openCarMenu(Player player, CarEntity car) {
        CarMenuHolder holder = new CarMenuHolder(car.getVehicleId());
        Inventory inv = Bukkit.createInventory(
            holder,
            menuSize,
            colorize(menuText(player, "title", "menu.car.title"))
        );
        holder.setInventory(inv);

        if (menuFillEmptySlots) {
            ItemStack filler = makeMenuItem(
                menuFillerMaterial,
                menuFillerName,
                Collections.emptyList(),
                menuFillerCustomModelData
            );
            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, filler);
            }
        }

        inv.setItem(
            menuSlotEngine,
            makeMenuItem(
                car.isEngineRunning()
                    ? menuEngineMaterialOn
                    : menuEngineMaterialOff,
                menuText(
                    player,
                    car.isEngineRunning() ? "engine-on" : "engine-off",
                    car.isEngineRunning()
                        ? "menu.car.engine.on"
                        : "menu.car.engine.off"
                ),
                menuLore(
                    player,
                    car.isEngineRunning() ? "engine-on" : "engine-off",
                    List.of(
                        menuText(player, "toggle-hint", "menu.car.toggle.hint")
                    )
                ),
                car.isEngineRunning()
                    ? menuEngineCustomModelDataOn
                    : menuEngineCustomModelDataOff
            )
        );
        inv.setItem(
            menuSlotLights,
            makeMenuItem(
                car.isHeadlightsOn()
                    ? menuLightsMaterialOn
                    : menuLightsMaterialOff,
                menuText(
                    player,
                    car.isHeadlightsOn() ? "lights-on" : "lights-off",
                    car.isHeadlightsOn()
                        ? "menu.car.lights.on"
                        : "menu.car.lights.off"
                ),
                menuLore(
                    player,
                    car.isHeadlightsOn() ? "lights-on" : "lights-off",
                    List.of(
                        menuText(player, "toggle-hint", "menu.car.toggle.hint")
                    )
                ),
                car.isHeadlightsOn()
                    ? menuLightsCustomModelDataOn
                    : menuLightsCustomModelDataOff
            )
        );
        if (menuTrunkEnabled) {
            inv.setItem(
                menuSlotTrunk,
                makeMenuItem(
                    menuTrunkMaterial,
                    menuText(player, "trunk", "menu.car.trunk"),
                    menuLore(
                        player,
                        "trunk",
                        List.of(
                            menuText(player, "trunk-hint", "menu.car.trunk.hint")
                        )
                    ),
                    menuTrunkCustomModelData
                )
            );
        }

        player.openInventory(inv);
    }

    private ItemStack makeMenuItem(
        Material material,
        String name,
        List<String> lore,
        int customModelData
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(colorize(name));
        List<String> outLore = new ArrayList<>();
        for (String line : lore) {
            outLore.add(colorize(line));
        }
        meta.setLore(outLore);
        if (customModelData != 0) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Player findOnlinePlayerByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(token);
        if (exact != null && exact.isOnline()) {
            return exact;
        }
        String needle = token.toLowerCase(Locale.ROOT);
        Player prefixMatch = null;
        for (Player online : Bukkit.getOnlinePlayers()) {
            String name = online.getName();
            if (name == null) {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.equals(needle)) {
                return online;
            }
            if (prefixMatch == null && lower.startsWith(needle)) {
                prefixMatch = online;
            }
        }
        return prefixMatch;
    }

    private static List<String> complete(String token, List<String> options) {
        List<String> out = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, out);
        Collections.sort(out);
        return out;
    }

    private static final class SoundProfile {

        private final boolean enabled;
        private final String key;
        private final float volume;
        private final float pitch;
        private final double radius;
        private final int intervalTicks;
        private final double minSpeed;
        private final double driftThreshold;
        private final double minSlipSpeed;
        private final double minSteer;
        private final int cooldownTicks;
        private final boolean requireMoving;
        private final boolean preventOverlap;

        private SoundProfile(
            boolean enabled,
            String key,
            float volume,
            float pitch,
            double radius,
            int intervalTicks,
            double minSpeed,
            double driftThreshold,
            double minSlipSpeed,
            double minSteer,
            int cooldownTicks,
            boolean requireMoving,
            boolean preventOverlap
        ) {
            this.enabled = enabled;
            this.key = key;
            this.volume = volume;
            this.pitch = pitch;
            this.radius = radius;
            this.intervalTicks = intervalTicks;
            this.minSpeed = minSpeed;
            this.driftThreshold = driftThreshold;
            this.minSlipSpeed = minSlipSpeed;
            this.minSteer = minSteer;
            this.cooldownTicks = cooldownTicks;
            this.requireMoving = requireMoving;
            this.preventOverlap = preventOverlap;
        }
    }

    private static final class CarTrunkHolder implements InventoryHolder {
        private final UUID carId;
        private final Inventory inventory;

        private CarTrunkHolder(UUID carId, Inventory inventory) {
            this.carId = carId;
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class CarMenuHolder implements InventoryHolder {

        private final UUID carId;
        private Inventory inventory;

        private CarMenuHolder(UUID carId) {
            this.carId = carId;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public int getActiveCarCount() {
        return cars.size();
    }

    public CarEntity findCarByPlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        UUID byCache = playerToCarCache.get(playerId);
        if (byCache != null) {
            CarEntity car = cars.get(byCache);
            if (car != null && car.isValid() && car.hasPlayer(playerId)) {
                return car;
            }
            playerToCarCache.remove(playerId);
        }

        UUID byDriverMap = driverToCar.get(playerId);
        if (byDriverMap != null) {
            CarEntity car = cars.get(byDriverMap);
            if (car != null && car.isValid() && car.hasPlayer(playerId)) {
                playerToCarCache.put(playerId, byDriverMap);
                return car;
            }
            driverToCar.remove(playerId);
        }

        for (CarEntity car : cars.values()) {
            if (car.isValid() && car.hasPlayer(playerId)) {
                playerToCarCache.put(playerId, car.getVehicleId());
                return car;
            }
        }
        playerToCarCache.remove(playerId);
        return null;
    }

    public CarEntity findCarForPlaceholder(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            return null;
        }
        UUID playerId = player.getUniqueId();

        CarEntity mounted = findCarByPlayer(playerId);
        if (mounted != null) {
            return mounted;
        }

        UUID cachedVehicleId = playerToCarCache.get(playerId);
        if (cachedVehicleId != null) {
            CarEntity cached = cars.get(cachedVehicleId);
            if (isOwnedByPlayer(cached, playerId)) {
                return cached;
            }
            playerToCarCache.remove(playerId);
        }

        CarEntity best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        Location playerLocation = null;
        if (player.isOnline()) {
            Player online = player.getPlayer();
            if (online != null) {
                playerLocation = online.getLocation();
            }
        }
        UUID fallbackVehicleId = null;

        for (Map.Entry<UUID, UUID> entry : carOwners.entrySet()) {
            if (!playerId.equals(entry.getValue())) {
                continue;
            }
            UUID vehicleId = entry.getKey();
            CarEntity candidate = cars.get(vehicleId);
            if (candidate == null || !candidate.isValid()) {
                continue;
            }
            if (
                fallbackVehicleId == null ||
                vehicleId.compareTo(fallbackVehicleId) < 0
            ) {
                fallbackVehicleId = vehicleId;
            }
            if (playerLocation == null) {
                continue;
            }
            Location candidateLocation = candidate.getSafeLocation();
            if (
                candidateLocation == null ||
                candidateLocation.getWorld() == null ||
                playerLocation.getWorld() == null ||
                !candidateLocation.getWorld().equals(playerLocation.getWorld())
            ) {
                continue;
            }
            double distanceSq = candidateLocation.distanceSquared(
                playerLocation
            );
            if (best == null || distanceSq < bestDistanceSq) {
                best = candidate;
                bestDistanceSq = distanceSq;
            }
        }

        if (best == null && fallbackVehicleId != null) {
            best = cars.get(fallbackVehicleId);
        }
        if (best != null) {
            cachePlayerCar(playerId, best.getVehicleId());
        }
        return best;
    }

    private boolean isOwnedByPlayer(CarEntity car, UUID playerId) {
        if (car == null || !car.isValid() || playerId == null) {
            return false;
        }
        UUID ownerId = carOwners.get(car.getVehicleId());
        return playerId.equals(ownerId);
    }

    private void cachePlayerCar(UUID playerId, UUID vehicleId) {
        if (playerId == null || vehicleId == null) {
            return;
        }
        playerToCarCache.put(playerId, vehicleId);
    }

    private void clearPlayerCacheForCar(UUID vehicleId) {
        if (vehicleId == null || playerToCarCache.isEmpty()) {
            return;
        }
        playerToCarCache
            .entrySet()
            .removeIf(entry -> vehicleId.equals(entry.getValue()));
    }

    public String getCarModelKey(CarEntity car) {
        if (car == null) {
            return defaultModel;
        }
        return carModelById.getOrDefault(car.getVehicleId(), defaultModel);
    }

    public String getCarModelName(CarEntity car) {
        String modelKey = getCarModelKey(car);
        return modelDisplayNames.getOrDefault(modelKey, modelKey);
    }

    public UUID getCarOwnerId(CarEntity car) {
        if (car == null) {
            return null;
        }
        return carOwners.get(car.getVehicleId());
    }

    public String getCarOwnerName(CarEntity car) {
        if (car == null) {
            return null;
        }
        UUID vehicleId = car.getVehicleId();
        String ownerName = carOwnerNames.get(vehicleId);
        if (ownerName != null && !ownerName.isBlank()) {
            return ownerName;
        }
        UUID ownerId = carOwners.get(vehicleId);
        if (ownerId == null) {
            return null;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(ownerId);
        String resolved = offline == null ? null : offline.getName();
        if (resolved != null && !resolved.isBlank()) {
            carOwnerNames.put(vehicleId, resolved);
        }
        return resolved;
    }

    private void registerPlaceholderExpansionIfAvailable() {
        Plugin placeholderApi = getServer()
            .getPluginManager()
            .getPlugin("PlaceholderAPI");
        if (placeholderApi == null || !placeholderApi.isEnabled()) {
            getLogger().info(
                "PlaceholderAPI not found: %coolcars_*% disabled."
            );
            return;
        }

        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.unregister();
            this.placeholderExpansion = null;
        }
        this.placeholderExpansion = new CoolCarsPlaceholderExpansion(this);
        if (this.placeholderExpansion.register()) {
            getLogger().info(
                "Registered PlaceholderAPI expansion: %coolcars_*%"
            );
        } else {
            getLogger().warning(
                "Failed to register PlaceholderAPI expansion: %coolcars_*%"
            );
        }
    }
}
