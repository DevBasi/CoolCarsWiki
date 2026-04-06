package dev.basi.cars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * @author basi
 */
public final class CarEntity {

    public enum DamagePart {
        FRONT,
        REAR,
        WHEEL_FL,
        WHEEL_FR,
        WHEEL_RL,
        WHEEL_RR,
    }

    private static final double TICK_DT = 1.0D / 20.0D;
    private static final Vector WORLD_UP = new Vector(0.0D, 1.0D, 0.0D);
    private static final double DEFAULT_PHYSICS_WHEEL_Y = 0.35D;
    private static final double SMALL_STEER_THRESHOLD = 0.35D;
    private static final double MAX_VISUAL_SUSPENSION_DROOP_GROUNDED = 0.22D;
    private static final double MAX_VISUAL_SUSPENSION_DROOP_AIR = 0.10D;
    private static final double MAX_VISUAL_SUSPENSION_COMPRESSION = 0.32D;
    private static final double VISUAL_SUSPENSION_RESPONSE = 0.38D;
    private static final double EFFECTS_VISIBILITY_RADIUS = 72.0D;
    private static final int EFFECTS_VISIBILITY_RECHECK_TICKS = 10;
    private static final Vector3f WHEEL_MODEL_CENTER_CORRECTION = new Vector3f(
        -0.1717F,
        -0.0033F,
        -0.1334F
    );

    private final UUID vehicleId;
    private final World world;
    private final CarSettings settings;

    private final ArmorStand root;
    private final ArmorStand fuelPointStand;
    private final ArmorStand trunkPointStand;
    private final ArmorStand frontDamageStand;
    private final ArmorStand rearDamageStand;
    private final Interaction interaction;
    private final ItemDisplay bodyDisplay;
    private final ItemDisplay steeringWheelDisplay;
    private final ItemDisplay[] wheelDisplays = new ItemDisplay[4];
    private final ArmorStand[] wheelDamageStands = new ArmorStand[4];
    private final ArmorStand[] seats;

    private final Vector bodyOffset;
    private final Vector steeringWheelOffset;
    private final Vector[] wheelOffsets;
    private final Vector[] seatOffsets;
    private final Vector[] headlightOffsets;
    private final Vector fuelPointOffset;
    private final Vector trunkPointOffset;
    private final Inventory trunkInventory;
    private final double physicsWheelYOffset;
    private final Set<BlockKey> placedHeadlightBlocks = new HashSet<>();
    private final BlockData headlightBlockData;

    private final WheelState[] wheels = new WheelState[] {
        new WheelState(),
        new WheelState(),
        new WheelState(),
        new WheelState(),
    };

    private final Vector velocity = new Vector();
    private double yawDegrees;
    private Location lastKnownLocation;
    private double steerInput;
    private double throttleInput;
    private boolean handBrake;

    private double steerVisualRadians;
    private double wheelSpinRadians;
    private final double[] wheelSpinRadiansByIndex = new double[] {
        0.0D,
        0.0D,
        0.0D,
        0.0D,
    };
    private double bodyRollRadians;
    private double bodyPitchRadians;
    private double terrainRollRadians;
    private double terrainPitchRadians;
    private double driverSeatAdaptiveLift;
    private int driverSeatStabilizeTicks;
    private double health;
    private double frontHealth;
    private double rearHealth;
    private final double[] wheelHealth = new double[] {
        0.0D,
        0.0D,
        0.0D,
        0.0D,
    };
    private double fuelLiters;
    private boolean engineRunning;
    private boolean headlightsOn;
    private boolean bodyMaterialHeadlightsApplied;
    private int ticksSinceDamage;
    private int lastEffectsVisibilityCheckTick =
        -EFFECTS_VISIBILITY_RECHECK_TICKS;
    private boolean hasNearbyPlayersForEffects = true;
    private boolean collidedThisTick;
    private boolean ramHitThisTick;
    private double ramHitSpeedThisTick;
    private boolean crashThisTick;
    private double crashDeltaSpeedThisTick;
    private double crashSpeedThisTick;
    private boolean hardLandingThisTick;
    private double landingImpactSpeedThisTick;
    private double landingContactImpactSpeedThisTick;
    private double lastDriveDirectionSign = 1.0D;
    private int lastGroundedWheels = 4;
    private int physicsTick;
    private final Map<UUID, Integer> ramVictimCooldownUntilTick =
        new HashMap<>();

    public CarEntity(
        Location spawnAt,
        ItemStack bodyModel,
        ItemStack wheelModel,
        CarSettings settings
    ) {
        this(
            UUID.randomUUID(),
            spawnAt,
            bodyModel,
            new ItemStack[] { wheelModel, wheelModel, wheelModel, wheelModel },
            settings,
            settings.maxHealth,
            settings.fuelInitialLiters,
            null,
            true,
            false
        );
    }

    public CarEntity(
        Location spawnAt,
        ItemStack bodyModel,
        ItemStack[] wheelModels,
        CarSettings settings
    ) {
        this(
            UUID.randomUUID(),
            spawnAt,
            bodyModel,
            wheelModels,
            settings,
            settings.maxHealth,
            settings.fuelInitialLiters,
            null,
            true,
            false
        );
    }

    public CarEntity(
        UUID vehicleId,
        Location spawnAt,
        ItemStack bodyModel,
        ItemStack wheelModel,
        CarSettings settings,
        double health,
        double fuelLiters,
        ItemStack[] trunkContents,
        boolean engineRunning,
        boolean headlightsOn
    ) {
        this(
            vehicleId,
            spawnAt,
            bodyModel,
            new ItemStack[] { wheelModel, wheelModel, wheelModel, wheelModel },
            settings,
            health,
            fuelLiters,
            trunkContents,
            engineRunning,
            headlightsOn
        );
    }

    public CarEntity(
        UUID vehicleId,
        Location spawnAt,
        ItemStack bodyModel,
        ItemStack[] wheelModels,
        CarSettings settings,
        double health,
        double fuelLiters,
        ItemStack[] trunkContents,
        boolean engineRunning,
        boolean headlightsOn
    ) {
        this.vehicleId = Objects.requireNonNull(
            vehicleId,
            "vehicleId"
        );
        this.world = Objects.requireNonNull(
            spawnAt.getWorld(),
            "spawn world"
        );
        this.settings = Objects.requireNonNull(
            settings,
            "settings"
        );
        this.yawDegrees = spawnAt.getYaw();
        this.lastKnownLocation = spawnAt.clone();
        this.fuelLiters = clamp(fuelLiters, 0.0D, settings.fuelTankCapacity);
        this.ticksSinceDamage = settings.regenDelayTicks;
        initializeDamageState(health);

        this.bodyOffset = settings.bodyOffset.clone();
        this.steeringWheelOffset = settings.steeringWheelOffset.clone();
        this.wheelOffsets = cloneVectors(settings.wheelOffsets);
        this.physicsWheelYOffset =
            averageWheelOffsetY(this.wheelOffsets) - DEFAULT_PHYSICS_WHEEL_Y;
        this.seatOffsets = cloneVectors(settings.seatOffsets);
        this.headlightOffsets = cloneVectors(settings.headlightOffsets);
        this.fuelPointOffset = settings.fuelPointOffset.clone();
        this.trunkPointOffset = settings.trunkPointOffset.clone();
        this.engineRunning = engineRunning;
        this.headlightsOn = headlightsOn;
        this.bodyMaterialHeadlightsApplied = false;
        this.headlightBlockData = createHeadlightBlockData(
            settings.headlightLevel
        );
        this.seats = new ArmorStand[this.seatOffsets.length];
        this.trunkInventory = Bukkit.createInventory(
            null,
            settings.trunkSlots,
            settings.trunkTitle
        );
        if (trunkContents != null) {
            this.trunkInventory.setContents(trunkContents);
        }

        ItemStack[] normalizedWheelModels = normalizeWheelModels(wheelModels);

        this.root = spawnRoot(spawnAt.clone());
        this.fuelPointStand = spawnFuelPoint(
            spawnAt.clone().add(this.fuelPointOffset)
        );
        this.trunkPointStand = spawnTrunkPoint(
            spawnAt.clone().add(this.trunkPointOffset)
        );
        this.frontDamageStand = settings.damage.enabled
            ? spawnDamagePoint(
                  spawnAt.clone().add(settings.damage.frontHitboxOffset)
              )
            : null;
        this.rearDamageStand = settings.damage.enabled
            ? spawnDamagePoint(
                  spawnAt.clone().add(settings.damage.rearHitboxOffset)
              )
            : null;
        this.interaction = spawnInteraction(spawnAt.clone(), settings);
        this.bodyDisplay = spawnDisplay(
            spawnAt.clone().add(this.bodyOffset),
            bodyModel
        );
        this.steeringWheelDisplay = spawnDisplay(
            spawnAt.clone().add(this.steeringWheelOffset),
            createDisplayModelStack(
                settings.steeringWheelMaterial,
                settings.steeringWheelCustomModelData
            )
        );

        for (int i = 0; i < wheelDisplays.length; i++) {
            this.wheelDisplays[i] = spawnDisplay(
                spawnAt.clone().add(this.wheelOffsets[i]),
                normalizedWheelModels[i]
            );
            this.wheelDisplays[i].setItemDisplayTransform(
                ItemDisplay.ItemDisplayTransform.FIXED
            );
            this.wheels[i].suspensionLength = settings.suspensionRest;
            this.wheels[i].lastSuspensionLength = settings.suspensionRest;
            this.wheels[i].visualSuspensionTravel = 0.0D;
            this.wheelDamageStands[i] = settings.damage.enabled
                ? spawnDamagePoint(spawnAt.clone().add(this.wheelOffsets[i]))
                : null;
        }

        for (int i = 0; i < seats.length; i++) {
            this.seats[i] = spawnSeat(spawnAt.clone().add(this.seatOffsets[i]));
        }

        configureInterpolation(this.bodyDisplay, settings);
        configureInterpolation(this.steeringWheelDisplay, settings);
        for (ItemDisplay wheelDisplay : wheelDisplays) {
            configureInterpolation(wheelDisplay, settings);
        }
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public Entity getRootEntity() {
        return root;
    }

    public boolean isValid() {
        if (!root.isValid() || !bodyDisplay.isValid()) {
            return false;
        }
        if (steeringWheelDisplay != null && !steeringWheelDisplay.isValid()) {
            return false;
        }
        for (ItemDisplay wheelDisplay : wheelDisplays) {
            if (!wheelDisplay.isValid()) {
                return false;
            }
        }
        for (ArmorStand seat : seats) {
            if (!seat.isValid()) {
                return false;
            }
        }
        return true;
    }

    public String validationState() {
        StringBuilder sb = new StringBuilder(128);
        sb
            .append("root=")
            .append(root != null && root.isValid());
        sb
            .append(", body=")
            .append(bodyDisplay != null && bodyDisplay.isValid());
        sb
            .append(", steering=")
            .append(
                steeringWheelDisplay != null && steeringWheelDisplay.isValid()
            );
        int invalidWheels = 0;
        for (ItemDisplay wheelDisplay : wheelDisplays) {
            if (wheelDisplay == null || !wheelDisplay.isValid()) {
                invalidWheels++;
            }
        }
        int invalidSeats = 0;
        for (ArmorStand seat : seats) {
            if (seat == null || !seat.isValid()) {
                invalidSeats++;
            }
        }
        sb
            .append(", invalidWheels=")
            .append(invalidWheels);
        sb
            .append(", invalidSeats=")
            .append(invalidSeats);
        return sb.toString();
    }

    public Location getLocation() {
        if (root != null && root.isValid()) {
            Location live = root.getLocation().clone();
            lastKnownLocation = live.clone();
            return live;
        }
        return lastKnownLocation == null ? null : lastKnownLocation.clone();
    }

    public Location getSafeLocation() {
        Location location = getLocation();
        if (location != null) {
            return location;
        }
        if (lastKnownLocation != null) {
            return lastKnownLocation.clone();
        }
        return null;
    }

    public Location getFuelPointLocation() {
        return fuelPointStand.getLocation().clone();
    }

    public Location getTrunkPointLocation() {
        return trunkPointStand.getLocation().clone();
    }

    public boolean teleportVehicle(Location target, boolean resetMotion) {
        if (
            target == null ||
            target.getWorld() == null ||
            !target.getWorld().getUID().equals(world.getUID()) ||
            !isValid()
        ) {
            return false;
        }

        yawDegrees = target.getYaw();
        if (resetMotion) {
            velocity.zero();
            steerInput = 0.0D;
            throttleInput = 0.0D;
            handBrake = false;
        }
        for (WheelState wheel : wheels) {
            wheel.grounded = false;
            wheel.suspensionLength = settings.suspensionRest;
            wheel.contactPoint = null;
        }
        lastGroundedWheels = 0;
        collidedThisTick = false;
        crashThisTick = false;
        crashDeltaSpeedThisTick = 0.0D;
        crashSpeedThisTick = 0.0D;
        ramHitThisTick = false;
        ramHitSpeedThisTick = 0.0D;
        hardLandingThisTick = false;
        landingImpactSpeedThisTick = 0.0D;
        landingContactImpactSpeedThisTick = 0.0D;

        Location postRoot = new Location(
            world,
            target.getX(),
            target.getY(),
            target.getZ(),
            (float) yawDegrees,
            0.0F
        );
        root.teleport(postRoot);
        lastKnownLocation = postRoot.clone();
        fuelPointStand.teleport(
            postRoot.clone().add(rotateYaw(fuelPointOffset, yawDegrees))
        );
        trunkPointStand.teleport(
            postRoot.clone().add(rotateYaw(trunkPointOffset, yawDegrees))
        );
        if (frontDamageStand != null) {
            frontDamageStand.teleport(
                postRoot
                    .clone()
                    .add(
                        rotateYaw(settings.damage.frontHitboxOffset, yawDegrees)
                    )
            );
        }
        if (rearDamageStand != null) {
            rearDamageStand.teleport(
                postRoot
                    .clone()
                    .add(
                        rotateYaw(settings.damage.rearHitboxOffset, yawDegrees)
                    )
            );
        }
        for (int i = 0; i < wheelDamageStands.length; i++) {
            if (wheelDamageStands[i] != null) {
                wheelDamageStands[i].teleport(
                    postRoot.clone().add(rotateYaw(wheelOffsets[i], yawDegrees))
                );
            }
        }
        interaction.teleport(postRoot);
        updateVisuals();
        return true;
    }

    public boolean isPart(Entity entity) {
        UUID id = entity.getUniqueId();
        if (
            root.getUniqueId().equals(id) ||
            fuelPointStand.getUniqueId().equals(id) ||
            trunkPointStand.getUniqueId().equals(id) ||
            interaction.getUniqueId().equals(id) ||
            bodyDisplay.getUniqueId().equals(id) ||
            steeringWheelDisplay.getUniqueId().equals(id)
        ) {
            return true;
        }
        for (ItemDisplay wheelDisplay : wheelDisplays) {
            if (wheelDisplay.getUniqueId().equals(id)) {
                return true;
            }
        }
        for (ArmorStand seat : seats) {
            if (seat.getUniqueId().equals(id)) {
                return true;
            }
        }
        if (settings.damage.enabled) {
            if (
                (frontDamageStand != null &&
                    frontDamageStand.getUniqueId().equals(id)) ||
                (rearDamageStand != null &&
                    rearDamageStand.getUniqueId().equals(id))
            ) {
                return true;
            }
            for (ArmorStand point : wheelDamageStands) {
                if (point != null && point.getUniqueId().equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isFuelPoint(Entity entity) {
        return fuelPointStand.getUniqueId().equals(entity.getUniqueId());
    }

    public boolean isTrunkPoint(Entity entity) {
        return trunkPointStand.getUniqueId().equals(entity.getUniqueId());
    }

    public UUID getFuelPointEntityId() {
        return fuelPointStand.getUniqueId();
    }

    public UUID getTrunkPointEntityId() {
        return trunkPointStand.getUniqueId();
    }

    public Set<UUID> getPartEntityIds() {
        Set<UUID> ids = new HashSet<>();
        ids.add(root.getUniqueId());
        ids.add(fuelPointStand.getUniqueId());
        ids.add(trunkPointStand.getUniqueId());
        ids.add(interaction.getUniqueId());
        ids.add(bodyDisplay.getUniqueId());
        ids.add(steeringWheelDisplay.getUniqueId());
        for (ItemDisplay wheelDisplay : wheelDisplays) {
            ids.add(wheelDisplay.getUniqueId());
        }
        for (ArmorStand seat : seats) {
            ids.add(seat.getUniqueId());
        }
        if (frontDamageStand != null) {
            ids.add(frontDamageStand.getUniqueId());
        }
        if (rearDamageStand != null) {
            ids.add(rearDamageStand.getUniqueId());
        }
        for (ArmorStand point : wheelDamageStands) {
            if (point != null) {
                ids.add(point.getUniqueId());
            }
        }
        return ids;
    }

    public boolean isDamagePointEntity(Entity entity) {
        if (entity == null || !settings.damage.enabled) {
            return false;
        }
        UUID id = entity.getUniqueId();
        if (
            (frontDamageStand != null &&
                frontDamageStand.getUniqueId().equals(id)) ||
            (rearDamageStand != null &&
                rearDamageStand.getUniqueId().equals(id))
        ) {
            return true;
        }
        for (ArmorStand point : wheelDamageStands) {
            if (point != null && point.getUniqueId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public DamagePart resolveDamagePart(Entity entity, Location actorLocation) {
        if (entity != null) {
            UUID id = entity.getUniqueId();
            if (settings.damage.enabled) {
                if (
                    frontDamageStand != null &&
                    frontDamageStand.getUniqueId().equals(id)
                ) {
                    return DamagePart.FRONT;
                }
                if (
                    rearDamageStand != null &&
                    rearDamageStand.getUniqueId().equals(id)
                ) {
                    return DamagePart.REAR;
                }
                for (int i = 0; i < wheelDamageStands.length; i++) {
                    ArmorStand point = wheelDamageStands[i];
                    if (point != null && point.getUniqueId().equals(id)) {
                        return wheelPartByIndex(i);
                    }
                }
            }
            for (int i = 0; i < wheelDisplays.length; i++) {
                if (wheelDisplays[i].getUniqueId().equals(id)) {
                    return wheelPartByIndex(i);
                }
            }
            if (actorLocation != null) {
                if (
                    bodyDisplay.getUniqueId().equals(id) ||
                    interaction.getUniqueId().equals(id) ||
                    root.getUniqueId().equals(id) ||
                    fuelPointStand.getUniqueId().equals(id) ||
                    trunkPointStand.getUniqueId().equals(id)
                ) {
                    return resolvePartByActorLocation(actorLocation);
                }
                for (ArmorStand seat : seats) {
                    if (seat.getUniqueId().equals(id)) {
                        return resolvePartByActorLocation(actorLocation);
                    }
                }
            } else if (
                bodyDisplay.getUniqueId().equals(id) ||
                interaction.getUniqueId().equals(id)
            ) {
                return resolveFrontOrRearByLocalZ(
                    entity.getLocation().toVector()
                );
            }
        }
        if (actorLocation != null) {
            return resolvePartByActorLocation(actorLocation);
        }
        return DamagePart.FRONT;
    }

    public DamagePart resolveClosestDamagePart(Location actorLocation) {
        if (actorLocation == null) {
            return DamagePart.FRONT;
        }
        Vector actor = actorLocation.toVector();
        DamagePart bestPart = DamagePart.FRONT;
        double bestDistance = Double.MAX_VALUE;
        for (DamagePart part : DamagePart.values()) {
            Location partLoc = getDamagePartLocation(part);
            if (partLoc == null) {
                continue;
            }
            double distSq = partLoc.toVector().distanceSquared(actor);
            if (distSq < bestDistance) {
                bestDistance = distSq;
                bestPart = part;
            }
        }
        return bestPart;
    }

    public Location getDamagePartLocation(DamagePart part) {
        if (part == null) {
            return root.getLocation().clone();
        }
        return switch (part) {
            case FRONT -> frontDamageStand != null
                ? frontDamageStand.getLocation().clone()
                : root
                      .getLocation()
                      .clone()
                      .add(
                          rotateYaw(
                              settings.damage.frontHitboxOffset,
                              yawDegrees
                          )
                      );
            case REAR -> rearDamageStand != null
                ? rearDamageStand.getLocation().clone()
                : root
                      .getLocation()
                      .clone()
                      .add(
                          rotateYaw(
                              settings.damage.rearHitboxOffset,
                              yawDegrees
                          )
                      );
            case WHEEL_FL, WHEEL_FR, WHEEL_RL, WHEEL_RR -> {
                int idx =
                    part == DamagePart.WHEEL_FL
                        ? 0
                        : part == DamagePart.WHEEL_FR
                            ? 1
                            : part == DamagePart.WHEEL_RL
                                ? 2
                                : 3;
                ArmorStand point = wheelDamageStands[idx];
                if (point != null) {
                    yield point.getLocation().clone();
                }
                yield root
                    .getLocation()
                    .clone()
                    .add(rotateYaw(wheelOffsets[idx], yawDegrees));
            }
        };
    }

    public double getPartHealth(DamagePart part) {
        if (!settings.damage.enabled || part == null) {
            return getHealth();
        }
        return switch (part) {
            case FRONT -> frontHealth;
            case REAR -> rearHealth;
            case WHEEL_FL -> wheelHealth[0];
            case WHEEL_FR -> wheelHealth[1];
            case WHEEL_RL -> wheelHealth[2];
            case WHEEL_RR -> wheelHealth[3];
        };
    }

    public double getPartMaxHealth(DamagePart part) {
        if (!settings.damage.enabled || part == null) {
            return getMaxHealth();
        }
        return switch (part) {
            case FRONT -> settings.damage.frontMaxHealth;
            case REAR -> settings.damage.rearMaxHealth;
            case
                WHEEL_FL,
                WHEEL_FR,
                WHEEL_RL,
                WHEEL_RR -> settings.damage.wheelMaxHealth;
        };
    }

    public double getPartHealthPercent(DamagePart part) {
        double max = getPartMaxHealth(part);
        if (max <= 0.0D) {
            return 0.0D;
        }
        return clamp(getPartHealth(part) / max, 0.0D, 1.0D);
    }

    public boolean repairPart(DamagePart part, double amount) {
        if (amount <= 0.0D) {
            return false;
        }
        if (!settings.damage.enabled) {
            double before = health;
            health = Math.min(settings.maxHealth, health + amount);
            return health > before;
        }
        boolean changed = false;
        switch (part) {
            case FRONT -> {
                if (settings.damage.frontEnabled) {
                    double before = frontHealth;
                    frontHealth = Math.min(
                        settings.damage.frontMaxHealth,
                        frontHealth + amount
                    );
                    changed = frontHealth > before;
                }
            }
            case REAR -> {
                if (settings.damage.rearEnabled) {
                    double before = rearHealth;
                    rearHealth = Math.min(
                        settings.damage.rearMaxHealth,
                        rearHealth + amount
                    );
                    changed = rearHealth > before;
                }
            }
            case WHEEL_FL, WHEEL_FR, WHEEL_RL, WHEEL_RR -> {
                if (settings.damage.wheelEnabled) {
                    int idx =
                        part == DamagePart.WHEEL_FL
                            ? 0
                            : part == DamagePart.WHEEL_FR
                                ? 1
                                : part == DamagePart.WHEEL_RL
                                    ? 2
                                    : 3;
                    double before = wheelHealth[idx];
                    wheelHealth[idx] = Math.min(
                        settings.damage.wheelMaxHealth,
                        wheelHealth[idx] + amount
                    );
                    changed = wheelHealth[idx] > before;
                }
            }
        }
        syncLegacyHealthFromParts();
        return changed;
    }

    public boolean repairCarHealth(double amount) {
        if (amount <= 0.0D) {
            return false;
        }
        double before = health;
        health = Math.min(settings.maxHealth, health + amount);
        return health > before;
    }

    public boolean repairAllHealth(double amount) {
        if (amount <= 0.0D) {
            return false;
        }
        boolean changed = repairCarHealth(amount);
        for (DamagePart part : DamagePart.values()) {
            changed = repairPart(part, amount) || changed;
        }
        return changed;
    }

    public void applyProjectileImpact(
        DamagePart part,
        String projectileType,
        double baseDamage
    ) {
        double resolvedDamage = Math.max(0.0D, baseDamage);
        if (resolvedDamage <= 0.0D) {
            return;
        }
        double typeMultiplier = switch (
            projectileType == null
                ? "other"
                : projectileType.toLowerCase(java.util.Locale.ROOT)
        ) {
            case "arrow" -> 1.0D;
            case "trident" -> 1.45D;
            default -> 0.85D;
        };
        resolvedDamage *= typeMultiplier;

        if (
            settings.damage.enabled &&
            settings.damage.wheelEnabled &&
            (part == DamagePart.WHEEL_FL ||
                part == DamagePart.WHEEL_FR ||
                part == DamagePart.WHEEL_RL ||
                part == DamagePart.WHEEL_RR)
        ) {
            resolvedDamage *= 1.20D;
        }

        if (!settings.damage.enabled) {
            damage(resolvedDamage);
            return;
        }
        double partDamage = resolvedDamage * getPartsDamageFactorForMode();
        if (partDamage > 0.0D) {
            applyPartDamage(part == null ? DamagePart.FRONT : part, partDamage);
        }
        applyCoreHealthDamageByMode(resolvedDamage);
        syncLegacyHealthFromParts();
        if (settings.destroyOnZeroHealth && getHealth() <= 0.0D) {
            destroy();
        }
    }

    public void setDriverInput(float sideways, float forward, boolean jump) {
        double steer = clamp(sideways, -1.0D, 1.0D);
        if (Math.abs(steer) < settings.steeringInputDeadzone) {
            steer = 0.0D;
        }
        this.steerInput = steer;
        this.throttleInput = clamp(forward, -1.0D, 1.0D);
        this.handBrake = jump;
    }

    public boolean mountDriver(LivingEntity entity) {
        purgeInvalidSeatPassengers();
        if (seats.length == 0 || !seats[0].getPassengers().isEmpty()) {
            return false;
        }
        entity.leaveVehicle();
        boolean mounted = seats[0].addPassenger(entity);
        if (mounted) {
            driverSeatAdaptiveLift = 0.0D;
            driverSeatStabilizeTicks = 40;
        }
        return mounted;
    }

    public Entity getDriver() {
        purgeInvalidSeatPassengers();
        if (seats.length == 0 || seats[0].getPassengers().isEmpty()) {
            return null;
        }
        return seats[0].getPassengers().get(0);
    }

    public int firstFreeSeatIndex() {
        purgeInvalidSeatPassengers();
        for (int i = 0; i < seats.length; i++) {
            if (seats[i].getPassengers().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public int findNearestFreeSeatIndex(Location playerLocation) {
        purgeInvalidSeatPassengers();
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        Vector rootPos = root.getLocation().toVector();
        for (int i = 0; i < seats.length; i++) {
            if (!seats[i].getPassengers().isEmpty()) {
                continue;
            }
            Vector seatWorld = rootPos
                .clone()
                .add(rotateYaw(seatOffsets[i], yawDegrees));
            double distSq = seatWorld.distanceSquared(
                playerLocation.toVector()
            );
            if (distSq < bestDistance) {
                bestDistance = distSq;
                best = i;
            }
        }
        return best;
    }

    public boolean mountSeat(LivingEntity entity, int seatIndex) {
        purgeInvalidSeatPassengers();
        if (seatIndex < 0 || seatIndex >= seats.length) {
            return false;
        }
        if (!seats[seatIndex].getPassengers().isEmpty()) {
            return false;
        }
        entity.leaveVehicle();
        boolean mounted = seats[seatIndex].addPassenger(entity);
        if (mounted && seatIndex == 0) {
            driverSeatAdaptiveLift = 0.0D;
            driverSeatStabilizeTicks = 40;
        }
        return mounted;
    }

    public int getSeatCount() {
        return seats.length;
    }

    public int getOccupiedSeatCount() {
        purgeInvalidSeatPassengers();
        int occupied = 0;
        for (ArmorStand seat : seats) {
            if (!seat.getPassengers().isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    public double getSpeedMetersPerSecond() {
        return velocity.clone().setY(0.0D).length();
    }

    public double getDriftAmount() {
        Vector flatVelocity = velocity.clone().setY(0.0D);
        double speed = flatVelocity.length();
        if (speed <= 0.0001D) {
            return 0.0D;
        }
        Vector forward = yawToForward(yawDegrees).setY(0.0D).normalize();
        double forwardSpeed = flatVelocity.dot(forward);
        Vector lateral = flatVelocity.subtract(forward.multiply(forwardSpeed));
        return lateral.length() / speed;
    }

    public double getLateralSlipSpeed() {
        Vector flatVelocity = velocity.clone().setY(0.0D);
        if (flatVelocity.lengthSquared() <= 0.0001D) {
            return 0.0D;
        }
        Vector forward = yawToForward(yawDegrees).setY(0.0D).normalize();
        double forwardSpeed = flatVelocity.dot(forward);
        Vector lateral = flatVelocity.subtract(forward.multiply(forwardSpeed));
        return lateral.length();
    }

    public double getSteerIntensity() {
        double maxSteer = Math.max(0.0001D, settings.maxSteerRad);
        return clamp(Math.abs(steerVisualRadians) / maxSteer, 0.0D, 1.0D);
    }

    public double getHealth() {
        if (!settings.damage.enabled) {
            return health;
        }
        return switch (settings.healthMode) {
            case CAR_ONLY -> health;
            case PARTS_ONLY -> getTotalDamageHealth();
            case CAR_AND_PARTS -> health + getTotalDamageHealth();
        };
    }

    public double getMaxHealth() {
        if (!settings.damage.enabled) {
            return settings.maxHealth;
        }
        return switch (settings.healthMode) {
            case CAR_ONLY -> settings.maxHealth;
            case PARTS_ONLY -> getTotalDamageMaxHealth();
            case CAR_AND_PARTS -> settings.maxHealth +
            getTotalDamageMaxHealth();
        };
    }

    public double getHealthPercent() {
        double max = getMaxHealth();
        if (max <= 0.0D) {
            return 0.0D;
        }
        return clamp(getHealth() / max, 0.0D, 1.0D);
    }

    public boolean isAdvancedDamageEnabled() {
        return settings.damage.enabled;
    }

    public boolean isRamHitThisTick() {
        return ramHitThisTick;
    }

    public double getRamHitSpeedThisTick() {
        return ramHitSpeedThisTick;
    }

    public boolean isCrashThisTick() {
        return crashThisTick;
    }

    public double getCrashDeltaSpeedThisTick() {
        return crashDeltaSpeedThisTick;
    }

    public double getCrashSpeedThisTick() {
        return crashSpeedThisTick;
    }

    public boolean isHardLandingThisTick() {
        return hardLandingThisTick;
    }

    public double getLandingImpactSpeedThisTick() {
        return landingImpactSpeedThisTick;
    }

    public double getFrontHealth() {
        return settings.damage.enabled ? frontHealth : getHealth();
    }

    public double getRearHealth() {
        return settings.damage.enabled ? rearHealth : getHealth();
    }

    public double getWheelHealth(int index) {
        if (
            !settings.damage.enabled || index < 0 || index >= wheelHealth.length
        ) {
            return getHealth();
        }
        return wheelHealth[index];
    }

    public double getFrontHealthPercent() {
        if (!settings.damage.enabled || !settings.damage.frontEnabled) {
            return getHealthPercent();
        }
        return clamp(frontHealth / settings.damage.frontMaxHealth, 0.0D, 1.0D);
    }

    public double getRearHealthPercent() {
        if (!settings.damage.enabled || !settings.damage.rearEnabled) {
            return getHealthPercent();
        }
        return clamp(rearHealth / settings.damage.rearMaxHealth, 0.0D, 1.0D);
    }

    public double getWheelAverageHealthPercent() {
        if (!settings.damage.enabled || !settings.damage.wheelEnabled) {
            return getHealthPercent();
        }
        double sum = 0.0D;
        for (double value : wheelHealth) {
            sum += clamp(value / settings.damage.wheelMaxHealth, 0.0D, 1.0D);
        }
        return sum / wheelHealth.length;
    }

    public double getFuelLiters() {
        return fuelLiters;
    }

    public double getFuelTankCapacity() {
        return settings.fuelTankCapacity;
    }

    public double getFuelPercent() {
        if (settings.fuelTankCapacity <= 0.0D) {
            return 0.0D;
        }
        return clamp(fuelLiters / settings.fuelTankCapacity, 0.0D, 1.0D);
    }

    public boolean isEngineRunning() {
        return engineRunning;
    }

    public void setEngineRunning(boolean running) {
        this.engineRunning = running;
        if (!running) {
            throttleInput = 0.0D;
            handBrake = true;
        }
    }

    public boolean toggleEngineRunning() {
        setEngineRunning(!engineRunning);
        return engineRunning;
    }

    public boolean isHeadlightsOn() {
        return headlightsOn;
    }

    public void setHeadlightsOn(boolean on) {
        this.headlightsOn = on;
        if (!on) {
            clearHeadlightBlocks();
        }
    }

    public boolean toggleHeadlights() {
        setHeadlightsOn(!headlightsOn);
        return headlightsOn;
    }

    public boolean hasFuel() {
        return fuelLiters > 0.0001D;
    }

    public double addFuel(double liters) {
        if (liters <= 0.0D || settings.fuelTankCapacity <= 0.0D) {
            return 0.0D;
        }
        double before = fuelLiters;
        fuelLiters = Math.min(settings.fuelTankCapacity, fuelLiters + liters);
        return fuelLiters - before;
    }

    public double drainFuel(double liters) {
        if (liters <= 0.0D) {
            return 0.0D;
        }
        double before = fuelLiters;
        fuelLiters = Math.max(0.0D, fuelLiters - liters);
        return before - fuelLiters;
    }

    public int getRefuelSoundIntervalTicks() {
        return Math.max(1, settings.fuelRefuelSoundIntervalTicks);
    }

    public double getRefuelRateLitersPerTick() {
        return Math.max(0.001D, settings.fuelRefuelRateLitersPerTick);
    }

    public Inventory getTrunkInventory() {
        return trunkInventory;
    }

    public ItemStack[] getTrunkContentsCopy() {
        return trunkInventory.getContents().clone();
    }

    public Map<String, Double> getDamageHealthSnapshot() {
        Map<String, Double> out = new HashMap<>();
        if (!settings.damage.enabled) {
            return out;
        }
        out.put("front", frontHealth);
        out.put("rear", rearHealth);
        out.put("wheel-fl", wheelHealth[0]);
        out.put("wheel-fr", wheelHealth[1]);
        out.put("wheel-rl", wheelHealth[2]);
        out.put("wheel-rr", wheelHealth[3]);
        return out;
    }

    public void applyDamageHealthSnapshot(Map<String, Double> snapshot) {
        if (!settings.damage.enabled || snapshot == null) {
            return;
        }
        frontHealth = clamp(
            snapshot.getOrDefault(
                "front",
                frontHealth
            ),
            0.0D,
            settings.damage.frontMaxHealth
        );
        rearHealth = clamp(
            snapshot.getOrDefault(
                "rear",
                rearHealth
            ),
            0.0D,
            settings.damage.rearMaxHealth
        );
        wheelHealth[0] = clamp(
            snapshot.getOrDefault(
                "wheel-fl",
                wheelHealth[0]
            ),
            0.0D,
            settings.damage.wheelMaxHealth
        );
        wheelHealth[1] = clamp(
            snapshot.getOrDefault(
                "wheel-fr",
                wheelHealth[1]
            ),
            0.0D,
            settings.damage.wheelMaxHealth
        );
        wheelHealth[2] = clamp(
            snapshot.getOrDefault(
                "wheel-rl",
                wheelHealth[2]
            ),
            0.0D,
            settings.damage.wheelMaxHealth
        );
        wheelHealth[3] = clamp(
            snapshot.getOrDefault(
                "wheel-rr",
                wheelHealth[3]
            ),
            0.0D,
            settings.damage.wheelMaxHealth
        );
        syncLegacyHealthFromParts();
    }

    public List<Player> getMountedPlayers() {
        purgeInvalidSeatPassengers();
        List<Player> out = new ArrayList<>();
        for (ArmorStand seat : seats) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof Player player) {
                    out.add(player);
                }
            }
        }
        return out;
    }

    public Map<UUID, Integer> getMountedPlayerSeatIndices() {
        purgeInvalidSeatPassengers();
        Map<UUID, Integer> out = new HashMap<>();
        for (int seatIndex = 0; seatIndex < seats.length; seatIndex++) {
            ArmorStand seat = seats[seatIndex];
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof Player player) {
                    out.put(player.getUniqueId(), seatIndex);
                }
            }
        }
        return out;
    }

    public boolean hasPlayer(UUID playerId) {
        purgeInvalidSeatPassengers();
        if (playerId == null) {
            return false;
        }
        for (ArmorStand seat : seats) {
            for (Entity passenger : seat.getPassengers()) {
                if (
                    passenger instanceof Player player &&
                    player.getUniqueId().equals(playerId)
                ) {
                    return true;
                }
            }
        }
        return false;
    }

    public void destroy() {
        clearHeadlightBlocks();
        purgeInvalidSeatPassengers();
        List<Entity> all = new ArrayList<>();
        all.add(root);
        all.add(fuelPointStand);
        all.add(trunkPointStand);
        if (frontDamageStand != null) {
            all.add(frontDamageStand);
        }
        if (rearDamageStand != null) {
            all.add(rearDamageStand);
        }
        all.add(interaction);
        all.add(bodyDisplay);
        all.add(steeringWheelDisplay);
        for (ItemDisplay wheelDisplay : wheelDisplays) {
            all.add(wheelDisplay);
        }
        for (ArmorStand point : wheelDamageStands) {
            if (point != null) {
                all.add(point);
            }
        }
        for (ArmorStand seat : seats) {
            all.add(seat);
        }
        all.forEach(Entity::remove);
    }

    public void tickPhysics() {
        if (!isValid()) {
            return;
        }
        purgeInvalidSeatPassengers();
        if (settings.destroyOnZeroHealth && health <= 0.0D) {
            destroy();
            return;
        }
        collidedThisTick = false;
        ramHitThisTick = false;
        ramHitSpeedThisTick = 0.0D;
        crashThisTick = false;
        crashDeltaSpeedThisTick = 0.0D;
        crashSpeedThisTick = 0.0D;
        hardLandingThisTick = false;
        landingImpactSpeedThisTick = 0.0D;
        landingContactImpactSpeedThisTick = 0.0D;
        physicsTick++;
        ticksSinceDamage++;

        if (settings.idleDamagePerTick > 0.0D) {
            damage(settings.idleDamagePerTick);
        }

        final Vector currentPos = root.getLocation().toVector();
        final Vector forward = yawToForward(yawDegrees);
        final int previouslyGroundedWheels = lastGroundedWheels;
        double totalSuspensionForce = 0.0D;
        int groundedWheels = 0;
        double combinedRollRadians = bodyRollRadians + terrainRollRadians;

        for (int i = 0; i < wheels.length; i++) {
            WheelState wheel = wheels[i];
            Vector wheelPhysicsOffset = new Vector(
                wheelOffsets[i].getX(),
                wheelOffsets[i].getY() - physicsWheelYOffset,
                wheelOffsets[i].getZ()
            );
            Vector wheelRootOffset = rotateYaw(
                wheelPhysicsOffset,
                yawDegrees
            ).add(
                new Vector(
                    0.0D,
                    combinedRollRadians * (i % 2 == 0 ? -0.25D : 0.25D),
                    0.0D
                )
            );
            Vector wheelAnchor = currentPos.clone().add(wheelRootOffset);

            Vector rayOrigin = wheelAnchor
                .clone()
                .add(WORLD_UP.clone().multiply(settings.suspensionRest));
            double rayLen =
                settings.suspensionRest +
                settings.wheelRadius +
                settings.stepHeight;

            RayTraceResult hit = world.rayTraceBlocks(
                rayOrigin.toLocation(world),
                WORLD_UP.clone().multiply(-1.0D),
                rayLen,
                FluidCollisionMode.NEVER,
                true
            );

            wheel.grounded = false;
            wheel.contactPoint = null;

            if (hit != null && hit.getHitPosition() != null) {
                Vector hitPos = hit.getHitPosition().clone();
                double lengthToGround =
                    rayOrigin.distance(hitPos) - settings.wheelRadius;
                lengthToGround = clamp(
                    lengthToGround,
                    0.0D,
                    settings.suspensionRest + settings.stepHeight
                );

                double compression = settings.suspensionRest - lengthToGround;
                double springVelocity =
                    (wheel.suspensionLength - lengthToGround) / TICK_DT;
                double springForce =
                    compression * settings.suspensionStiffness -
                    springVelocity * settings.suspensionDamping;
                springForce = clamp(springForce, 0.0D, settings.mass * 20.0D);

                if (springForce > 0.0D) {
                    totalSuspensionForce += springForce;
                    wheel.grounded = true;
                    wheel.contactPoint = hitPos;
                    groundedWheels++;
                }

                wheel.lastSuspensionLength = wheel.suspensionLength;
                wheel.suspensionLength = lengthToGround;
            } else {
                wheel.lastSuspensionLength = wheel.suspensionLength;
                wheel.suspensionLength =
                    settings.suspensionRest + settings.stepHeight;
            }
        }
        double prePhysicsSignedSpeed = velocity.clone().setY(0.0D).dot(forward);
        if (Math.abs(prePhysicsSignedSpeed) > 0.12D) {
            lastDriveDirectionSign = Math.signum(prePhysicsSignedSpeed);
        } else if (Math.abs(throttleInput) > 0.15D) {
            lastDriveDirectionSign = Math.signum(throttleInput);
        }
        updateTerrainTiltFromSuspension(
            currentPos,
            groundedWheels,
            prePhysicsSignedSpeed
        );

        Vector acceleration = new Vector(0.0D, -9.81D, 0.0D);
        if (groundedWheels > 0) {
            acceleration.add(
                new Vector(0.0D, totalSuspensionForce / settings.mass, 0.0D)
            );
            acceleration.add(
                new Vector(
                    0.0D,
                    -velocity.getY() * settings.verticalDamping,
                    0.0D
                )
            );
        }

        Vector horizontalVel = velocity.clone().setY(0.0D);
        double signedSpeed = horizontalVel.dot(forward);
        Vector right = new Vector(
            forward.getZ(),
            0.0D,
            -forward.getX()
        ).normalize();

        double tractionForce =
            throttleInput >= 0.0D
                ? Math.pow(
                      Math.abs(throttleInput),
                      settings.accelerationCurveExponent
                  ) *
                  settings.engineForce *
                  settings.accelerationForward
                : -Math.pow(
                      Math.abs(throttleInput),
                      settings.accelerationCurveExponent
                  ) *
                  settings.engineForce *
                  settings.accelerationReverseMultiplier;
        double performanceFactor = computeDamagePerformanceFactor();
        tractionForce *= performanceFactor;

        if (handBrake || (throttleInput < 0.0D && signedSpeed > 0.2D)) {
            tractionForce -=
                Math.signum(signedSpeed) *
                settings.brakeForce *
                settings.brakeMultiplier;
        }

        double maxForwardSpeed = settings.maxForwardSpeed * performanceFactor;
        double maxReverseSpeed = settings.maxReverseSpeed * performanceFactor;
        if (
            (signedSpeed > maxForwardSpeed && tractionForce > 0.0D) ||
            (signedSpeed < -maxReverseSpeed && tractionForce < 0.0D)
        ) {
            tractionForce = 0.0D;
        }
        if (!hasFuel()) {
            tractionForce = 0.0D;
        }
        if (!engineRunning) {
            tractionForce = 0.0D;
        }
        if (getHealthPercent() <= settings.engineDisabledHealthPercent) {
            tractionForce = 0.0D;
        }
        if (isImmobilizedByHealth()) {
            tractionForce = 0.0D;
        }

        if (groundedWheels == 0) {
            tractionForce = 0.0D;
        }
        Vector tractionAcc = forward
            .clone()
            .multiply(tractionForce / settings.mass);

        Vector dragAcc = horizontalVel
            .clone()
            .multiply(-settings.dragCoeff * horizontalVel.length());
        Vector rollingAcc =
            horizontalVel.lengthSquared() > 0.00001D
                ? horizontalVel
                      .clone()
                      .normalize()
                      .multiply(-settings.rollingResistance / settings.mass)
                : new Vector();
        double lateralSpeed = horizontalVel.dot(right);
        Vector lateralGripAcc = right.multiply(
            -lateralSpeed *
                (groundedWheels > 0
                    ? settings.lateralGripGround
                    : settings.lateralGripAir)
        );

        acceleration
            .add(tractionAcc)
            .add(dragAcc)
            .add(rollingAcc)
            .add(lateralGripAcc);

        Vector preTickVelocity = velocity.clone();
        velocity.add(acceleration.multiply(TICK_DT));
        double downwardImpactSpeed = Math.max(
            0.0D,
            Math.max(-preTickVelocity.getY(), -velocity.getY())
        );
        Vector preHorizontal = preTickVelocity.clone().setY(0.0D);
        Vector newHorizontal = velocity.clone().setY(0.0D);
        Vector horizontalDelta = newHorizontal.clone().subtract(preHorizontal);
        if (horizontalDelta.length() > settings.maxTickHorizontalDeltaV) {
            Vector clampedHorizontal = preHorizontal.add(
                horizontalDelta
                    .normalize()
                    .multiply(settings.maxTickHorizontalDeltaV)
            );
            velocity.setX(clampedHorizontal.getX());
            velocity.setZ(clampedHorizontal.getZ());
        }
        if (groundedWheels > 0 && velocity.getY() < -0.1D) {
            velocity.setY(velocity.getY() * 0.25D);
        }
        if (groundedWheels > 0 && velocity.getY() > 0.6D) {
            velocity.setY(0.6D);
        }
        if (!hasFuel()) {
            velocity.setX(velocity.getX() * settings.fuelNoFuelBrakeFactor);
            velocity.setZ(velocity.getZ() * settings.fuelNoFuelBrakeFactor);
        }
        if (!engineRunning) {
            velocity.setX(velocity.getX() * 0.94D);
            velocity.setZ(velocity.getZ() * 0.94D);
        }
        consumeFuelForTick(signedSpeed);
        lastGroundedWheels = groundedWheels;

        double steerTarget = steerInput * settings.maxSteerRad;
        if (
            Math.abs(steerInput) > 0.0D &&
            Math.abs(steerInput) < SMALL_STEER_THRESHOLD
        ) {
            steerTarget *= settings.steeringLowInputSteerBoost;
        }
        steerVisualRadians = lerp(
            steerVisualRadians,
            steerTarget,
            Math.min(
                0.9D,
                settings.steerResponse +
                    (Math.abs(steerInput) <= SMALL_STEER_THRESHOLD
                        ? settings.steeringLowInputResponseBonus
                        : 0.0D)
            )
        );

        double speedAbs = Math.abs(signedSpeed);
        if (speedAbs > 0.1D) {
            double steerForYaw = steerVisualRadians;
            if (
                Math.abs(steerInput) > 0.0D &&
                Math.abs(steerInput) < SMALL_STEER_THRESHOLD
            ) {
                steerForYaw *= settings.steeringLowInputYawBoost;
            }
            double highSpeedWindow = Math.max(
                0.1D,
                settings.steeringHighSpeedReductionEndSpeed -
                    settings.steeringHighSpeedReductionStartSpeed
            );
            double highSpeedSteerFactor =
                1.0D -
                clamp(
                    (speedAbs - settings.steeringHighSpeedReductionStartSpeed) /
                        highSpeedWindow,
                    0.0D,
                    settings.steeringHighSpeedMaxReduction
                );
            steerForYaw *= highSpeedSteerFactor;
            double yawRateRad =
                (signedSpeed / settings.wheelBase) * Math.tan(steerForYaw);
            yawDegrees += Math.toDegrees(yawRateRad * TICK_DT);
        }

        double lateralAccel =
            ((speedAbs * speedAbs) / Math.max(0.25D, settings.wheelBase)) *
            Math.sin(steerVisualRadians) *
            0.034D *
            settings.bodyRollStrength;
        bodyRollRadians = lerp(
            bodyRollRadians,
            clamp(
                -lateralAccel,
                -settings.bodyRollLimitRad,
                settings.bodyRollLimitRad
            ),
            settings.bodyRollResponse
        );
        double longitudinalAccel =
            (signedSpeed - prePhysicsSignedSpeed) / TICK_DT;
        double pitchTarget = clamp(
            -longitudinalAccel * 0.0018D,
            -Math.toRadians(7.0D),
            Math.toRadians(7.0D)
        );
        bodyPitchRadians = lerp(bodyPitchRadians, pitchTarget, 0.16D);

        Vector physicsPos = currentPos.clone();
        if (isCarColliding(physicsPos)) {
            Vector unstuck = tryUnstuckNear(physicsPos, velocity.clone());
            if (unstuck != null) {
                physicsPos = unstuck;
                if (velocity.getY() > 0.0D) {
                    velocity.setY(0.0D);
                }
            } else {
                velocity.multiply(settings.stuckVelocityDamping);
                if (velocity.getY() > 0.0D) {
                    velocity.setY(0.0D);
                }
            }
        }

        Vector displacement = velocity.clone().multiply(TICK_DT);
        Vector horizontalDisplacement = displacement.clone().setY(0.0D);
        if (horizontalDisplacement.length() > 1.0D) {
            horizontalDisplacement.normalize().multiply(1.0D);
            displacement.setX(horizontalDisplacement.getX());
            displacement.setZ(horizontalDisplacement.getZ());
        }
        Vector moved = moveWithCollisionSubsteps(physicsPos, displacement);
        if (
            (previouslyGroundedWheels == 0 && groundedWheels > 0) ||
            landingContactImpactSpeedThisTick > 0.001D
        ) {
            applyLandingImpact(
                Math.max(downwardImpactSpeed, landingContactImpactSpeedThisTick)
            );
        }
        applyRamDamage(moved, velocity.clone().setY(0.0D).length());

        Vector postHorizontal = velocity.clone().setY(0.0D);
        double impactDeltaSpeed = preHorizontal.distance(postHorizontal);
        if (
            collidedThisTick && impactDeltaSpeed >= settings.impactMinDeltaSpeed
        ) {
            double damage =
                (impactDeltaSpeed - settings.impactMinDeltaSpeed) *
                settings.impactDamageScale *
                settings.impactWallMultiplier;
            applyImpactDamage(damage, signedSpeed);
            crashThisTick = true;
            crashDeltaSpeedThisTick = impactDeltaSpeed;
            crashSpeedThisTick = Math.abs(signedSpeed);
        }

        if (
            settings.regenEnabled &&
            ticksSinceDamage > settings.regenDelayTicks &&
            getHealth() < getMaxHealth()
        ) {
            if (settings.damage.enabled) {
                regenParts(settings.regenPerTick);
            } else {
                health = Math.min(
                    settings.maxHealth,
                    health + settings.regenPerTick
                );
            }
        }

        Location postRoot = new Location(
            world,
            moved.getX(),
            moved.getY(),
            moved.getZ(),
            (float) yawDegrees,
            0.0F
        );
        root.teleport(postRoot, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        lastKnownLocation = postRoot.clone();
        
        // Ensure all utility points move smoothly with the root in the same tick.
        fuelPointStand.teleport(
            postRoot.clone().add(rotateYaw(fuelPointOffset, yawDegrees)),
            TeleportFlag.EntityState.RETAIN_PASSENGERS
        );
        trunkPointStand.teleport(
            postRoot.clone().add(rotateYaw(trunkPointOffset, yawDegrees)),
            TeleportFlag.EntityState.RETAIN_PASSENGERS
        );
        if (frontDamageStand != null) {
            frontDamageStand.teleport(
                postRoot
                    .clone()
                    .add(
                        rotateYaw(settings.damage.frontHitboxOffset, yawDegrees)
                    ),
                TeleportFlag.EntityState.RETAIN_PASSENGERS
            );
        }
        if (rearDamageStand != null) {
            rearDamageStand.teleport(
                postRoot
                    .clone()
                    .add(
                        rotateYaw(settings.damage.rearHitboxOffset, yawDegrees)
                    ),
                TeleportFlag.EntityState.RETAIN_PASSENGERS
            );
        }
        for (int i = 0; i < wheelDamageStands.length; i++) {
            if (wheelDamageStands[i] != null) {
                wheelDamageStands[i].teleport(
                    postRoot.clone().add(rotateYaw(wheelOffsets[i], yawDegrees)),
                    TeleportFlag.EntityState.RETAIN_PASSENGERS
                );
            }
        }
        interaction.teleport(postRoot, TeleportFlag.EntityState.RETAIN_PASSENGERS);
    }

    public void updateVisuals() {
        if (!root.isValid()) {
            return;
        }

        final Vector rootPos = root.getLocation().toVector();
        syncBodyMaterialForHeadlights();
        double combinedRollRadians = bodyRollRadians + terrainRollRadians;
        double combinedPitchRadians = terrainPitchRadians + bodyPitchRadians;
        final Quaternionf bodyRot = new Quaternionf()
            .rotateY((float) Math.toRadians(-yawDegrees))
            .rotateX((float) combinedPitchRadians)
            .rotateZ((float) combinedRollRadians);

        Location bodyLoc = rootPos
            .clone()
            .add(rotateYaw(bodyOffset, yawDegrees))
            .toLocation(world);
        bodyDisplay.teleport(bodyLoc, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        bodyDisplay.setTransformation(
            new Transformation(
                new Vector3f(0.0F, 0.0F, 0.0F),
                bodyRot,
                new Vector3f(2.5F, 2.5F, 2.5F),
                new Quaternionf()
            )
        );

        Location wheelHandleLoc = rootPos
            .clone()
            .add(rotateYaw(steeringWheelOffset, yawDegrees))
            .toLocation(world);
        steeringWheelDisplay.teleport(
            wheelHandleLoc,
            TeleportFlag.EntityState.RETAIN_PASSENGERS
        );
        Quaternionf steeringWheelRot = new Quaternionf()
            .rotateY((float) Math.toRadians(-yawDegrees))
            .rotateX((float) combinedPitchRadians)
            .rotateZ((float) combinedRollRadians)
            .rotateX((float) Math.toRadians(24.0D))
            .rotateZ((float) (steerVisualRadians * 2.25D));
        steeringWheelDisplay.setTransformation(
            new Transformation(
                new Vector3f(0.0F, 0.0F, 0.0F),
                steeringWheelRot,
                new Vector3f(0.65F, 0.65F, 0.65F),
                new Quaternionf()
            )
        );

        Vector flatVel = velocity.clone().setY(0.0D);
        double signedSpeed = flatVel.dot(yawToForward(yawDegrees));
        double slipRatio = clamp(getDriftAmount(), 0.0D, 1.0D);
        wheelSpinRadians += (signedSpeed / settings.wheelRadius) * TICK_DT;

        for (int i = 0; i < wheelDisplays.length; i++) {
            WheelState wheel = wheels[i];
            double rawTravel = settings.suspensionRest - wheel.suspensionLength;
            double maxDroop = wheel.grounded
                ? MAX_VISUAL_SUSPENSION_DROOP_GROUNDED
                : MAX_VISUAL_SUSPENSION_DROOP_AIR;
            double targetTravel = clamp(
                rawTravel,
                -maxDroop,
                MAX_VISUAL_SUSPENSION_COMPRESSION
            );
            wheel.visualSuspensionTravel = lerp(
                wheel.visualSuspensionTravel,
                targetTravel,
                VISUAL_SUSPENSION_RESPONSE
            );
            Vector wheelOffsetWithSuspension = wheelOffsets[i].clone().add(
                new Vector(0.0D, wheel.visualSuspensionTravel, 0.0D)
            );
            Vector wheelOffsetRotated = rotateYaw(
                wheelOffsetWithSuspension,
                yawDegrees
            );
            Location wheelLoc = rootPos
                .clone()
                .add(wheelOffsetRotated)
                .toLocation(world);
            wheelDisplays[i].teleport(
                wheelLoc,
                TeleportFlag.EntityState.RETAIN_PASSENGERS
            );

            boolean frontAxle = i < 2;
            float steerYaw = 0.0F;
            if (frontAxle) {
                double ackermann = 1.0D;
                if (Math.abs(steerVisualRadians) > 0.001D) {
                    boolean turningLeft = steerVisualRadians > 0.0D;
                    boolean isLeftWheel = i == 0;
                    boolean inner =
                        (turningLeft && isLeftWheel) ||
                        (!turningLeft && !isLeftWheel);
                    ackermann = inner ? 1.10D : 0.92D;
                }
                steerYaw = (float) (steerVisualRadians * ackermann);
            }
            double baseSpinDelta =
                (signedSpeed / settings.wheelRadius) * TICK_DT;
            double gripFactor = wheel.grounded ? 1.0D : 0.65D;
            double slipFactor =
                1.0D - (frontAxle ? slipRatio * 0.30D : slipRatio * 0.18D);
            double axleBrakeFactor =
                handBrake && !frontAxle && wheel.grounded ? 0.72D : 1.0D;
            wheelSpinRadiansByIndex[i] +=
                baseSpinDelta * gripFactor * slipFactor * axleBrakeFactor;

            Quaternionf wheelRot = new Quaternionf()
                .rotateY((float) Math.toRadians(-yawDegrees))
                .rotateX((float) combinedPitchRadians)
                .rotateZ((float) combinedRollRadians)
                .rotateY(steerYaw)
                .rotateX((float) -wheelSpinRadiansByIndex[i]);
            Vector3f pivotCompensation = new Vector3f(
                WHEEL_MODEL_CENTER_CORRECTION
            );
            wheelRot.transform(pivotCompensation);
            pivotCompensation.negate();

            wheelDisplays[i].setTransformation(
                new Transformation(
                    pivotCompensation,
                    wheelRot,
                    new Vector3f(1.0F, 1.0F, 1.0F),
                    new Quaternionf()
                )
            );
        }

        for (int i = 0; i < seats.length; i++) {
            Vector seatOffset = rotateYaw(seatOffsets[i], yawDegrees);
            if (i == 0) {
                seatOffset = seatOffset
                    .clone()
                    .add(
                        new Vector(
                            0.0D,
                            resolveDriverSeatAdaptiveLift(rootPos),
                            0.0D
                        )
                    );
            }
            Location seatLoc = rootPos
                .clone()
                .add(seatOffset)
                .toLocation(world);
            
            if (seats[i].getPassengers().isEmpty()) {
                seatLoc.setYaw((float) yawDegrees);
                seatLoc.setPitch(0.0F);
                seats[i].teleport(seatLoc, TeleportFlag.EntityState.RETAIN_PASSENGERS);
            } else {
                // If occupied, use a mix of hard teleport and relative rotation to avoid camera jitter
                // On 1.21.1 clients, hard-setting yaw/pitch causes the camera to "snap" back if not handled correctly.
                // However, moving only the location (Relative flags) can cause desync during turns.
                // We use TeleportFlag.EntityState.RETAIN_PASSENGERS for the location sync.
                seats[i].teleport(
                    seatLoc, 
                    TeleportFlag.EntityState.RETAIN_PASSENGERS,
                    TeleportFlag.Relative.YAW,
                    TeleportFlag.Relative.PITCH
                );
            }
        }

        updateHeadlights(rootPos);
        updateExhaust(rootPos, signedSpeed);
        updateDamageSmoke(rootPos);
    }

    private void syncBodyMaterialForHeadlights() {
        if (headlightsOn == bodyMaterialHeadlightsApplied) {
            return;
        }
        Material target = headlightsOn
            ? settings.bodyMaterialHeadlights
            : settings.bodyMaterial;
        int cmd = headlightsOn
            ? settings.bodyMaterialHeadlightsCustomModelData
            : settings.bodyCustomModelData;
        bodyDisplay.setItemStack(createDisplayModelStack(target, cmd));
        bodyMaterialHeadlightsApplied = headlightsOn;
    }

    private void updateHeadlights(Vector rootPos) {
        if (!headlightsOn) {
            clearHeadlightBlocks();
            return;
        }
        if (physicsTick % Math.max(1, settings.headlightUpdateTicks) != 0) {
            return;
        }

        clearHeadlightBlocks();
        Vector forward = yawToForward(yawDegrees);
        boolean emitParticles =
            settings.headlightParticlesVisible &&
            shouldRenderEffectsForPlayers(rootPos);
        for (Vector lampOffset : headlightOffsets) {
            Vector lampPos = rootPos
                .clone()
                .add(rotateYaw(lampOffset, yawDegrees));
            placeHeadlightBeam(lampPos, forward, emitParticles);
        }
    }

    private double resolveDriverSeatAdaptiveLift(Vector rootPos) {
        if (seats.length == 0) {
            driverSeatAdaptiveLift = 0.0D;
            driverSeatStabilizeTicks = 0;
            return 0.0D;
        }

        Entity driver = getDriver();
        if (driver == null || !driver.isValid()) {
            driverSeatAdaptiveLift = lerp(driverSeatAdaptiveLift, 0.0D, 0.20D);
            driverSeatStabilizeTicks = 0;
            return clamp(driverSeatAdaptiveLift, 0.0D, 0.45D);
        }

        double targetLift = 0.0D;
        if (driverSeatStabilizeTicks > 0) {
            Vector seatBasePos = rootPos
                .clone()
                .add(rotateYaw(seatOffsets[0], yawDegrees));
            double seatY = seatBasePos.getY();
            double riderMinY = driver.getBoundingBox().getMinY();
            double sinking = (seatY - 0.08D) - riderMinY;
            if (sinking > 0.0D) {
                targetLift = clamp(sinking * 1.15D, 0.0D, 0.45D);
            }
            driverSeatStabilizeTicks--;
            targetLift = Math.max(targetLift, 0.16D);
        }

        driverSeatAdaptiveLift = lerp(
            driverSeatAdaptiveLift,
            targetLift,
            driverSeatStabilizeTicks > 0 ? 0.24D : 0.18D
        );
        return clamp(driverSeatAdaptiveLift, 0.0D, 0.45D);
    }

    private void placeHeadlightBeam(
        Vector start,
        Vector forward,
        boolean emitParticles
    ) {
        int segments = Math.max(
            3,
            (int) Math.round(settings.headlightRange / 1.4D)
        );
        Vector dir = forward.clone().normalize();

        for (int i = 1; i <= segments; i++) {
            double dist = (settings.headlightRange * i) / segments;
            Vector sample = start.clone().add(dir.clone().multiply(dist));
            RayTraceResult hit = world.rayTraceBlocks(
                start.toLocation(world),
                dir,
                dist,
                FluidCollisionMode.NEVER,
                true
            );
            if (hit != null && hit.getHitPosition() != null) {
                sample = hit.getHitPosition().clone();
            }
            placeHeadlightLightBlock(sample);
            if (emitParticles) {
                world.spawnParticle(
                    Particle.END_ROD,
                    sample.getX(),
                    sample.getY(),
                    sample.getZ(),
                    1,
                    0.02D,
                    0.02D,
                    0.02D,
                    0.0D
                );
            }
            if (hit != null) {
                break;
            }
        }
    }

    private void updateExhaust(Vector rootPos, double signedSpeed) {
        if (!settings.exhaustEnabled || !engineRunning) {
            return;
        }
        if (!shouldRenderEffectsForPlayers(rootPos)) {
            return;
        }
        if (physicsTick % Math.max(1, settings.exhaustUpdateTicks) != 0) {
            return;
        }
        double throttleAbs = Math.abs(throttleInput);
        if (throttleAbs < settings.exhaustMinThrottle) {
            return;
        }

        int count =
            settings.exhaustBaseCount +
            (int) Math.round(
                throttleAbs * 2.0D +
                    Math.abs(signedSpeed) * settings.exhaustSpeedFactor
            );
        count = (int) clamp(count, 0.0D, settings.exhaustMaxCount);
        if (count <= 0) {
            return;
        }

        Vector right = new Vector(
            Math.cos(Math.toRadians(yawDegrees)),
            0.0D,
            Math.sin(Math.toRadians(yawDegrees))
        );
        Vector backward = yawToForward(yawDegrees).multiply(-1.0D);

        for (Vector offset : settings.exhaustOffsets) {
            Vector pos = rootPos.clone().add(rotateYaw(offset, yawDegrees));
            Vector drift = backward
                .clone()
                .multiply(0.02D + throttleAbs * 0.05D)
                .add(right.clone().multiply((Math.random() - 0.5D) * 0.02D));
            world.spawnParticle(
                Particle.SMOKE,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                count,
                0.05D,
                0.04D,
                0.05D,
                0.008D
            );
            world.spawnParticle(
                Particle.CAMPFIRE_COSY_SMOKE,
                pos.getX(),
                pos.getY() + 0.04D,
                pos.getZ(),
                1,
                0.01D,
                0.01D,
                0.01D,
                0.006D
            );
            world.spawnParticle(
                Particle.LARGE_SMOKE,
                pos.getX() + drift.getX(),
                pos.getY() + 0.02D,
                pos.getZ() + drift.getZ(),
                1,
                0.01D,
                0.01D,
                0.01D,
                0.004D
            );
        }
    }

    private void placeHeadlightLightBlock(Vector pos) {
        Block block = world.getBlockAt(
            (int) Math.floor(pos.getX()),
            (int) Math.floor(pos.getY()),
            (int) Math.floor(pos.getZ())
        );
        Material type = block.getType();
        boolean existingLight = type == Material.LIGHT;
        if (
            !(type.isAir() ||
                existingLight ||
                type == Material.CAVE_AIR ||
                type == Material.VOID_AIR)
        ) {
            return;
        }

        block.setBlockData(headlightBlockData, false);
        if (!existingLight) {
            placedHeadlightBlocks.add(
                new BlockKey(block.getX(), block.getY(), block.getZ())
            );
        }
    }

    private void clearHeadlightBlocks() {
        if (placedHeadlightBlocks.isEmpty()) {
            return;
        }
        for (BlockKey key : placedHeadlightBlocks) {
            Block block = world.getBlockAt(key.x, key.y, key.z);
            if (block.getType() == Material.LIGHT) {
                block.setType(Material.AIR, false);
            }
        }
        placedHeadlightBlocks.clear();
    }

    private static BlockData createHeadlightBlockData(int level) {
        BlockData data = Material.LIGHT.createBlockData();
        if (data instanceof org.bukkit.block.data.type.Light light) {
            light.setLevel(Math.max(1, Math.min(15, level)));
        }
        return data;
    }

    private Vector resolveHorizontalCollision(Vector from, Vector desiredMove) {
        Vector horizontalMove = desiredMove.clone().setY(0.0D);
        double verticalMove = desiredMove.getY();
        double blockedVerticalMove = Math.min(0.0D, verticalMove);
        double distance = horizontalMove.length();
        if (distance < 0.0001D) {
            return from.clone().add(new Vector(0.0D, verticalMove, 0.0D));
        }

        Vector candidate = from.clone().add(horizontalMove);
        if (!isCarColliding(candidate)) {
            candidate.setY(candidate.getY() + verticalMove);
            return candidate;
        }

        RayTraceResult hit = world.rayTraceBlocks(
            from.toLocation(world),
            horizontalMove.clone().normalize(),
            distance + 0.45D,
            FluidCollisionMode.NEVER,
            true
        );
        Block hitBlock = hit == null ? null : hit.getHitBlock();
        boolean narrowBarrier =
            (hitBlock != null && isNarrowCollisionBlock(hitBlock)) ||
            isNearNarrowObstacle(candidate, 0.70D);

        double speedAbs = velocity.clone().setY(0.0D).length();
        boolean allowStepUp =
            !narrowBarrier &&
            (Math.abs(throttleInput) > 0.05D || speedAbs > 0.22D);
        if (allowStepUp) {
            double adaptiveStepHeight = Math.min(
                settings.stepHeight + 0.14D,
                settings.stepHeight + speedAbs * 0.03D
            );
            int requiredProbes = speedAbs > 2.2D ? 1 : 2;
            double stepIncrement = speedAbs > 1.6D ? 0.05D : 0.08D;
            for (
                double step = stepIncrement;
                step <= adaptiveStepHeight;
                step += stepIncrement
            ) {
                Vector stepped = candidate
                    .clone()
                    .add(new Vector(0.0D, step, 0.0D));
                if (
                    !isCarColliding(stepped) &&
                    hasStableGroundSupport(
                        stepped,
                        Math.max(
                            0.45D,
                            Math.min(settings.stepHeight + 0.25D, 1.20D)
                        ),
                        requiredProbes
                    )
                ) {
                    stepped.setY(stepped.getY() + verticalMove);
                    return stepped;
                }
            }
        }

        Vector out = from.clone();
        if (hit == null || hit.getHitBlockFace() == null) {
            if (isCarColliding(out)) {
                collidedThisTick = true;
                Vector unstuck = tryUnstuckNear(out, horizontalMove);
                if (unstuck != null) {
                    unstuck.setY(unstuck.getY() + blockedVerticalMove);
                    return unstuck;
                }
                velocity.multiply(settings.stuckVelocityDamping);
                return out;
            }
            out.add(horizontalMove);
            out.setY(out.getY() + blockedVerticalMove);
            return out;
        }

        BlockFace face = hit.getHitBlockFace();
        Vector normal = new Vector(
            face.getModX(),
            face.getModY(),
            face.getModZ()
        );
        normal.setY(0.0D);
        if (normal.lengthSquared() < 0.0001D) {
            normal = horizontalMove.clone().normalize().multiply(-1.0D);
        } else {
            normal.normalize();
        }
        double velDot = velocity.clone().setY(0.0D).dot(normal);
        if (velDot < 0.0D) {
            collidedThisTick = true;
            velocity.subtract(
                normal.clone().multiply(velDot * settings.wallDamping)
            );
        }

        Vector tangentMove = horizontalMove
            .clone()
            .subtract(normal.multiply(horizontalMove.dot(normal)))
            .multiply(settings.slideFactor);
        out.add(tangentMove);
        if (isCarColliding(out)) {
            collidedThisTick = true;
            Vector unstuck = tryUnstuckNear(from, horizontalMove);
            out = unstuck != null ? unstuck : from.clone();
            velocity.multiply(settings.stuckVelocityDamping);
        }
        out.setY(out.getY() + blockedVerticalMove);
        if (collidedThisTick && velocity.getY() > 0.12D) {
            velocity.setY(0.12D);
        }

        Block block = hit.getHitBlock();
        if (block != null) {
            world.spawnParticle(
                Particle.BLOCK,
                out.toLocation(world),
                4,
                0.08D,
                0.04D,
                0.08D,
                block.getBlockData()
            );
        }

        return out;
    }

    private Vector moveWithCollisionSubsteps(Vector from, Vector displacement) {
        if (from == null || displacement == null) {
            return from;
        }
        double horizontalDistance = displacement.clone().setY(0.0D).length();
        double verticalDistance = Math.abs(displacement.getY());
        int steps = Math.max(
            1,
            (int) Math.ceil(
                Math.max(horizontalDistance / 0.22D, verticalDistance / 0.18D)
            )
        );
        steps = Math.min(8, steps);
        Vector stepMove = displacement.clone().multiply(1.0D / steps);
        Vector pos = from.clone();
        for (int i = 0; i < steps; i++) {
            Vector before = pos.clone();
            Vector afterHorizontal = resolveHorizontalCollision(
                before,
                stepMove
            );
            pos = resolveVerticalGroundCollision(before, afterHorizontal);
            if (isCarColliding(pos)) {
                Vector unstuck = tryUnstuckNear(before, stepMove);
                if (unstuck != null) {
                    pos = unstuck;
                } else {
                    pos = before;
                    velocity.setX(
                        velocity.getX() * settings.stuckVelocityDamping
                    );
                    velocity.setZ(
                        velocity.getZ() * settings.stuckVelocityDamping
                    );
                    if (velocity.getY() > 0.0D) {
                        velocity.setY(0.0D);
                    }
                }
                break;
            }
        }
        return pos;
    }

    private Vector resolveVerticalGroundCollision(Vector from, Vector to) {
        if (from == null || to == null) {
            return to;
        }
        if (to.getY() >= from.getY() - 0.0001D) {
            return to;
        }
        double descentDistance = Math.max(0.0D, from.getY() - to.getY());

        Vector corrected = to.clone();
        if (isCarColliding(corrected)) {
            double maxLift = Math.min(
                2.25D,
                Math.abs(from.getY() - to.getY()) + settings.stepHeight + 0.60D
            );
            for (double lift = 0.05D; lift <= maxLift; lift += 0.05D) {
                Vector raised = to.clone().add(new Vector(0.0D, lift, 0.0D));
                if (!isCarColliding(raised)) {
                    corrected = raised;
                    if (velocity.getY() < 0.0D) {
                        recordLandingContactImpact(descentDistance);
                        velocity.setY(0.0D);
                    }
                    break;
                }
            }
        }

        double snappedY = findGroundYUnderCar(from, corrected);
        if (Double.isFinite(snappedY) && snappedY > corrected.getY()) {
            double maxUpSnap = isNearNarrowObstacle(corrected, 0.75D)
                ? 0.02D
                : 0.08D;
            snappedY = Math.min(snappedY, from.getY() + maxUpSnap);
            if (snappedY <= corrected.getY()) {
                return corrected;
            }
            corrected.setY(snappedY);
            if (velocity.getY() < -0.02D) {
                recordLandingContactImpact(descentDistance);
                velocity.setY(0.0D);
            }
        }
        return corrected;
    }

    private void recordLandingContactImpact(double descentDistance) {
        double fallSpeedByDistance = descentDistance / TICK_DT;
        double impactSpeed = Math.max(
            Math.max(0.0D, -velocity.getY()),
            Math.max(0.0D, fallSpeedByDistance)
        );
        landingContactImpactSpeedThisTick = Math.max(
            landingContactImpactSpeedThisTick,
            impactSpeed
        );
    }

    private double findGroundYUnderCar(Vector from, Vector to) {
        double startBaseY =
            from.getY() +
            settings.collisionBaseY +
            Math.max(0.05D, settings.wheelRadius * 0.15D);
        double endBaseY = to.getY() + settings.collisionBaseY - 0.28D;
        if (startBaseY <= endBaseY) {
            return Double.NEGATIVE_INFINITY;
        }

        Vector[] probes = new Vector[] {
            new Vector(-settings.carHalfWidth, 0.0D, -settings.carHalfLength),
            new Vector(settings.carHalfWidth, 0.0D, -settings.carHalfLength),
            new Vector(-settings.carHalfWidth, 0.0D, settings.carHalfLength),
            new Vector(settings.carHalfWidth, 0.0D, settings.carHalfLength),
            new Vector(0.0D, 0.0D, 0.0D),
        };
        double rayLength =
            (startBaseY - endBaseY) +
            Math.max(0.35D, settings.stepHeight + 0.15D);
        double highestSafeCarY = Double.NEGATIVE_INFINITY;
        double secondSafeCarY = Double.NEGATIVE_INFINITY;
        int supportHits = 0;
        for (Vector local : probes) {
            Vector worldOffset = rotateYaw(local, yawDegrees);
            double probeX = to.getX() + worldOffset.getX();
            double probeZ = to.getZ() + worldOffset.getZ();
            Location origin = new Location(world, probeX, startBaseY, probeZ);
            RayTraceResult downHit = world.rayTraceBlocks(
                origin,
                WORLD_UP.clone().multiply(-1.0D),
                rayLength,
                FluidCollisionMode.NEVER,
                true
            );
            if (downHit == null || downHit.getHitPosition() == null) {
                continue;
            }
            Block hitBlock = downHit.getHitBlock();
            if (
                hitBlock != null && isNarrowObstacleMaterial(hitBlock.getType())
            ) {
                continue;
            }
            double topY = downHit.getHitPosition().getY();
            if (topY < endBaseY - 0.02D) {
                continue;
            }
            double safeCarY = topY - settings.collisionBaseY + 0.02D;
            supportHits++;
            if (safeCarY > highestSafeCarY) {
                secondSafeCarY = highestSafeCarY;
                highestSafeCarY = safeCarY;
            } else if (safeCarY > secondSafeCarY) {
                secondSafeCarY = safeCarY;
            }
        }
        if (supportHits >= 2 && Double.isFinite(secondSafeCarY)) {
            return secondSafeCarY;
        }
        return Double.NEGATIVE_INFINITY;
    }

    private Vector tryUnstuckNear(Vector origin, Vector moveHint) {
        Vector moveDir = moveHint.clone().setY(0.0D);
        if (moveDir.lengthSquared() < 0.00001D) {
            moveDir = yawToForward(yawDegrees).setY(0.0D);
        }
        if (moveDir.lengthSquared() < 0.00001D) {
            moveDir = new Vector(1.0D, 0.0D, 0.0D);
        } else {
            moveDir.normalize();
        }
        Vector sideDir = new Vector(-moveDir.getZ(), 0.0D, moveDir.getX());
        if (sideDir.lengthSquared() < 0.00001D) {
            sideDir = new Vector(0.0D, 0.0D, 1.0D);
        } else {
            sideDir.normalize();
        }
        int attempts = settings.unstuckHorizontalAttempts;
        if (isNearNarrowObstacle(origin, 0.90D)) {
            attempts = Math.max(
                attempts,
                settings.unstuckHorizontalAttempts + 3
            );
        }
        for (int step = 1; step <= attempts; step++) {
            double nudge = settings.unstuckHorizontalNudge * step;
            List<Vector> candidates = new ArrayList<>();
            candidates.add(origin.clone().add(sideDir.clone().multiply(nudge)));
            candidates.add(
                origin.clone().add(sideDir.clone().multiply(-nudge))
            );
            candidates.add(
                origin.clone().add(moveDir.clone().multiply(-nudge * 0.75D))
            );
            candidates.add(
                origin
                    .clone()
                    .add(sideDir.clone().multiply(nudge * 0.75D))
                    .add(moveDir.clone().multiply(-nudge * 0.55D))
            );
            candidates.add(
                origin
                    .clone()
                    .add(sideDir.clone().multiply(-nudge * 0.75D))
                    .add(moveDir.clone().multiply(-nudge * 0.55D))
            );
            for (Vector candidate : candidates) {
                if (!isCarColliding(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isNearNarrowObstacle(Vector origin, double radius) {
        if (origin == null) {
            return false;
        }
        int minX = (int) Math.floor(origin.getX() - radius);
        int maxX = (int) Math.floor(origin.getX() + radius);
        int minY = (int) Math.floor(
            origin.getY() + settings.collisionBaseY - 0.2D
        );
        int maxY = (int) Math.floor(
            origin.getY() + settings.collisionBaseY + settings.carHeight
        );
        int minZ = (int) Math.floor(origin.getZ() - radius);
        int maxZ = (int) Math.floor(origin.getZ() + radius);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Material type = world.getBlockAt(x, y, z).getType();
                    if (isNarrowObstacleMaterial(type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isCarColliding(Vector carPos) {
        Vector[] probes = collisionProbes();
        for (Vector local : probes) {
            Vector worldOffset = rotateYaw(local, yawDegrees);
            Vector feet = carPos
                .clone()
                .add(worldOffset)
                .add(new Vector(0.0D, settings.collisionBaseY, 0.0D));
            if (isSolidAt(feet.getX(), feet.getY(), feet.getZ())) {
                return true;
            }
            if (
                isSolidAt(
                    feet.getX(),
                    feet.getY() + (settings.carHeight * 0.5D),
                    feet.getZ()
                )
            ) {
                return true;
            }
            if (
                isSolidAt(
                    feet.getX(),
                    feet.getY() + settings.carHeight,
                    feet.getZ()
                )
            ) {
                return true;
            }
        }
        if (isCollidingWithNarrowObstaclesAabb(carPos)) {
            return true;
        }
        return false;
    }

    private boolean isCollidingWithNarrowObstaclesAabb(Vector carPos) {
        if (carPos == null) {
            return false;
        }

        double yawRad = Math.toRadians(yawDegrees);
        double cos = Math.abs(Math.cos(yawRad));
        double sin = Math.abs(Math.sin(yawRad));
        double extentX =
            (cos * settings.carHalfWidth) + (sin * settings.carHalfLength);
        double extentZ =
            (sin * settings.carHalfWidth) + (cos * settings.carHalfLength);
        double minX = carPos.getX() - extentX - 0.04D;
        double maxX = carPos.getX() + extentX + 0.04D;
        double minY = carPos.getY() + settings.collisionBaseY;
        double maxY = minY + settings.carHeight;
        double minZ = carPos.getZ() - extentZ - 0.04D;
        double maxZ = carPos.getZ() + extentZ + 0.04D;

        int bxMin = (int) Math.floor(minX);
        int bxMax = (int) Math.floor(maxX);
        int byMin = (int) Math.floor(minY);
        int byMax = (int) Math.floor(maxY);
        int bzMin = (int) Math.floor(minZ);
        int bzMax = (int) Math.floor(maxZ);

        double epsilon = 0.0001D;
        for (int bx = bxMin; bx <= bxMax; bx++) {
            for (int by = byMin; by <= byMax; by++) {
                for (int bz = bzMin; bz <= bzMax; bz++) {
                    Block block = world.getBlockAt(bx, by, bz);
                    if (!isNarrowCollisionBlock(block)) {
                        continue;
                    }

                    java.util.Collection<BoundingBox> boxes = block
                        .getCollisionShape()
                        .getBoundingBoxes();
                    if (boxes.isEmpty()) {
                        continue;
                    }

                    for (BoundingBox box : boxes) {
                        if (box == null) {
                            continue;
                        }
                        double bMinX = box.getMinX();
                        double bMinY = box.getMinY();
                        double bMinZ = box.getMinZ();
                        double bMaxX = box.getMaxX();
                        double bMaxY = box.getMaxY();
                        double bMaxZ = box.getMaxZ();

                        boolean localShape =
                            bMinX >= -epsilon &&
                            bMinY >= -epsilon &&
                            bMinZ >= -epsilon &&
                            bMaxX <= 1.0D + epsilon &&
                            bMaxY <= 1.0D + epsilon &&
                            bMaxZ <= 1.0D + epsilon;
                        if (localShape) {
                            bMinX += bx;
                            bMinY += by;
                            bMinZ += bz;
                            bMaxX += bx;
                            bMaxY += by;
                            bMaxZ += bz;
                        }

                        boolean intersects =
                            maxX >= bMinX - epsilon &&
                            minX <= bMaxX + epsilon &&
                            maxY >= bMinY - epsilon &&
                            minY <= bMaxY + epsilon &&
                            maxZ >= bMinZ - epsilon &&
                            minZ <= bMaxZ + epsilon;
                        if (intersects) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean hasStableGroundSupport(
        Vector carPos,
        double maxDrop,
        int requiredSupportedProbes
    ) {
        int supported = 0;
        Vector[] probes = collisionProbes();
        double probeDrop = Math.max(0.08D, maxDrop);
        for (Vector local : probes) {
            Vector worldOffset = rotateYaw(local, yawDegrees);
            Vector feet = carPos
                .clone()
                .add(worldOffset)
                .add(new Vector(0.0D, settings.collisionBaseY, 0.0D));
            boolean foundSupport = false;
            for (double drop = 0.05D; drop <= probeDrop; drop += 0.10D) {
                double sampleY = feet.getY() - drop;
                Block supportBlock = blockAt(feet.getX(), sampleY, feet.getZ());
                if (
                    supportBlock != null &&
                    !isNarrowObstacleMaterial(supportBlock.getType()) &&
                    isSolidAt(feet.getX(), sampleY, feet.getZ())
                ) {
                    foundSupport = true;
                    break;
                }
            }
            if (foundSupport) {
                supported++;
            }
        }
        return supported >= Math.max(1, requiredSupportedProbes);
    }

    private Vector[] collisionProbes() {
        return new Vector[] {
            new Vector(-settings.carHalfWidth, 0.0D, -settings.carHalfLength),
            new Vector(settings.carHalfWidth, 0.0D, -settings.carHalfLength),
            new Vector(-settings.carHalfWidth, 0.0D, settings.carHalfLength),
            new Vector(settings.carHalfWidth, 0.0D, settings.carHalfLength),
            new Vector(0.0D, 0.0D, -settings.carHalfLength),
            new Vector(0.0D, 0.0D, settings.carHalfLength),
            new Vector(-settings.carHalfWidth, 0.0D, 0.0D),
            new Vector(settings.carHalfWidth, 0.0D, 0.0D),
            new Vector(0.0D, 0.0D, 0.0D),
        };
    }

    private Block blockAt(double x, double y, double z) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        return world.getBlockAt(bx, by, bz);
    }

    private boolean isNarrowObstacleMaterial(Material type) {
        if (type == null) {
            return false;
        }
        return (
            Tag.FENCES.isTagged(type) ||
            Tag.WALLS.isTagged(type) ||
            Tag.FENCE_GATES.isTagged(type) ||
            Tag.TRAPDOORS.isTagged(type) ||
            type == Material.IRON_BARS ||
            type.name().endsWith("_PANE")
        );
    }

    private boolean isNarrowCollisionBlock(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (isNarrowObstacleMaterial(type)) {
            return true;
        }
        java.util.Collection<BoundingBox> boxes = block
            .getCollisionShape()
            .getBoundingBoxes();
        if (boxes.isEmpty()) {
            return false;
        }
        double epsilon = 0.0001D;
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
                minX >= -epsilon &&
                minY >= -epsilon &&
                minZ >= -epsilon &&
                maxX <= 1.0D + epsilon &&
                maxY <= 1.0D + epsilon &&
                maxZ <= 1.0D + epsilon;
            if (!localShape) {
                minX -= block.getX();
                minZ -= block.getZ();
                maxX -= block.getX();
                maxZ -= block.getZ();
            }
            double widthX = Math.max(0.0D, maxX - minX);
            double widthZ = Math.max(0.0D, maxZ - minZ);
            if (widthX < 0.84D || widthZ < 0.84D) {
                return true;
            }
        }
        return false;
    }

    private boolean isSolidAt(double x, double y, double z) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        Block block = world.getBlockAt(bx, by, bz);
        Material type = block.getType();
        boolean narrowObstacle = isNarrowObstacleMaterial(type);
        boolean passable = block.isPassable();

        java.util.Collection<BoundingBox> boxes = block
            .getCollisionShape()
            .getBoundingBoxes();
        if (boxes.isEmpty()) {
            if (narrowObstacle) {
                return true;
            }
            return !passable;
        }

        double epsilon = 0.0001D;
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
                minX >= -epsilon &&
                minY >= -epsilon &&
                minZ >= -epsilon &&
                maxX <= 1.0D + epsilon &&
                maxY <= 1.0D + epsilon &&
                maxZ <= 1.0D + epsilon;
            if (localShape) {
                minX += bx;
                minY += by;
                minZ += bz;
                maxX += bx;
                maxY += by;
                maxZ += bz;
            }
            if (
                x >= minX - epsilon &&
                x <= maxX + epsilon &&
                y >= minY - epsilon &&
                y <= maxY + epsilon &&
                z >= minZ - epsilon &&
                z <= maxZ + epsilon
            ) {
                return true;
            }
        }
        return false;
    }

    private static ArmorStand spawnRoot(Location at) {
        ArmorStand stand = (ArmorStand) at
            .getWorld()
            .spawnEntity(at, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setCollidable(false);
        stand.setPersistent(true);
        return stand;
    }

    private static Interaction spawnInteraction(
        Location at,
        CarSettings settings
    ) {
        Interaction interaction = (Interaction) at
            .getWorld()
            .spawnEntity(at, EntityType.INTERACTION);
        interaction.setInteractionWidth(settings.interactionWidth);
        interaction.setInteractionHeight(settings.interactionHeight);
        interaction.setPersistent(true);
        return interaction;
    }

    private static ArmorStand spawnSeat(Location at) {
        ArmorStand seat = (ArmorStand) at
            .getWorld()
            .spawnEntity(at, EntityType.ARMOR_STAND);
        seat.setVisible(false);
        seat.setGravity(false);
        seat.setMarker(false);
        seat.setSmall(true);
        seat.setBasePlate(false);
        seat.setInvulnerable(true);
        seat.setSilent(true);
        seat.setCollidable(false);
        seat.setPersistent(true);
        return seat;
    }

    private static ArmorStand spawnFuelPoint(Location at) {
        ArmorStand stand = (ArmorStand) at
            .getWorld()
            .spawnEntity(at, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(false);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setCollidable(false);
        stand.setPersistent(true);
        return stand;
    }

    private static ArmorStand spawnTrunkPoint(Location at) {
        ArmorStand stand = (ArmorStand) at
            .getWorld()
            .spawnEntity(at, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(false);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setCollidable(false);
        stand.setPersistent(true);
        return stand;
    }

    private static ArmorStand spawnDamagePoint(Location at) {
        ArmorStand stand = (ArmorStand) at
            .getWorld()
            .spawnEntity(at, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(false);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setCollidable(false);
        stand.setPersistent(true);
        return stand;
    }

    private static ItemDisplay spawnDisplay(Location at, ItemStack model) {
        ItemDisplay display = (ItemDisplay) at
            .getWorld()
            .spawnEntity(at, EntityType.ITEM_DISPLAY);
        display.setItemStack(
            model == null ? new ItemStack(Material.IRON_INGOT) : model
        );
        display.setBrightness(new Display.Brightness(15, 15));
        display.setViewRange(64.0F);
        display.setShadowRadius(0.0F);
        display.setPersistent(true);
        return display;
    }

    private static void configureInterpolation(
        ItemDisplay display,
        CarSettings settings
    ) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(
            Math.max(3, settings.interpolationDuration)
        );
        display.setTeleportDuration(Math.max(3, settings.teleportDuration));
    }

    private static Vector rotateYaw(Vector local, double yawDegrees) {
        double rad = Math.toRadians(yawDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = local.getX() * cos - local.getZ() * sin;
        double z = local.getX() * sin + local.getZ() * cos;
        return new Vector(x, local.getY(), z);
    }

    private static Vector yawToForward(double yawDegrees) {
        double rad = Math.toRadians(yawDegrees);
        return new Vector(-Math.sin(rad), 0.0D, Math.cos(rad)).normalize();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double lerp(double from, double to, double alpha) {
        return from + (to - from) * clamp(alpha, 0.0D, 1.0D);
    }

    private static double averageWheelOffsetY(Vector[] offsets) {
        if (offsets == null || offsets.length == 0) {
            return DEFAULT_PHYSICS_WHEEL_Y;
        }
        double sum = 0.0D;
        for (Vector offset : offsets) {
            sum += offset == null ? DEFAULT_PHYSICS_WHEEL_Y : offset.getY();
        }
        return sum / offsets.length;
    }

    private void updateTerrainTiltFromSuspension(
        Vector rootPos,
        int groundedWheels,
        double signedSpeed
    ) {
        if (groundedWheels <= 0) {
            double horizontalSpeed = Math.abs(signedSpeed);
            double driveSign;
            if (horizontalSpeed > 0.20D) {
                driveSign = Math.signum(signedSpeed);
            } else if (Math.abs(throttleInput) > 0.10D) {
                driveSign = Math.signum(throttleInput);
            } else {
                driveSign = lastDriveDirectionSign;
            }
            if (Math.abs(driveSign) < 0.001D) {
                driveSign = 1.0D;
            }

            double fallAngle = Math.atan2(
                -velocity.getY(),
                Math.max(0.75D, horizontalSpeed + 0.35D)
            );
            double airPitchTarget =
                fallAngle * settings.airPitchVelocityFactor * driveSign;
            double maxAirPitch = Math.toRadians(18.0D);
            double airResponse = lerp(
                settings.airPitchResponse * 0.65D,
                settings.airPitchResponse,
                clamp(horizontalSpeed / 10.0D, 0.0D, 1.0D)
            );
            terrainPitchRadians = lerp(
                terrainPitchRadians,
                clamp(airPitchTarget, -maxAirPitch, maxAirPitch),
                airResponse
            );
            terrainRollRadians = lerp(
                terrainRollRadians,
                0.0D,
                settings.airRollStabilizeResponse
            );
            return;
        }
        final double frontAvg = averageWheelGroundHeight(rootPos, 0, 1);
        final double rearAvg = averageWheelGroundHeight(rootPos, 2, 3);
        final double leftAvg = averageWheelGroundHeight(rootPos, 0, 2);
        final double rightAvg = averageWheelGroundHeight(rootPos, 1, 3);
        final int frontGrounded = groundedWheelCount(0, 1);
        final int rearGrounded = groundedWheelCount(2, 3);
        final int leftGrounded = groundedWheelCount(0, 2);
        final int rightGrounded = groundedWheelCount(1, 3);

        double frontZ =
            (wheelOffsets[0].getZ() + wheelOffsets[1].getZ()) * 0.5D;
        double rearZ = (wheelOffsets[2].getZ() + wheelOffsets[3].getZ()) * 0.5D;
        double rightX =
            (wheelOffsets[1].getX() + wheelOffsets[3].getX()) * 0.5D;
        double leftX = (wheelOffsets[0].getX() + wheelOffsets[2].getX()) * 0.5D;

        double longitudinalSpan = Math.max(0.25D, Math.abs(frontZ - rearZ));
        double lateralSpan = Math.max(0.25D, Math.abs(rightX - leftX));

        double pitchTarget = Math.atan2(rearAvg - frontAvg, longitudinalSpan);
        double rollTarget = Math.atan2(rightAvg - leftAvg, lateralSpan);

        double maxTerrainPitch = Math.toRadians(16.0D);
        double maxTerrainRoll = Math.toRadians(18.0D);
        double pitchConfidence = Math.min(frontGrounded, rearGrounded) / 2.0D;
        double rollConfidence = Math.min(leftGrounded, rightGrounded) / 2.0D;
        double pitchResponse = lerp(0.06D, 0.20D, pitchConfidence);
        double rollResponse = lerp(0.06D, 0.18D, rollConfidence);
        double maxPitchStep = Math.toRadians(1.6D);
        double maxRollStep = Math.toRadians(1.8D);

        double pitchTargetClamped = clamp(
            pitchTarget,
            -maxTerrainPitch,
            maxTerrainPitch
        );
        double rollTargetClamped = clamp(
            rollTarget,
            -maxTerrainRoll,
            maxTerrainRoll
        );
        double pitchNext = lerp(
            terrainPitchRadians,
            pitchTargetClamped,
            pitchResponse
        );
        double rollNext = lerp(
            terrainRollRadians,
            rollTargetClamped,
            rollResponse
        );
        terrainPitchRadians =
            terrainPitchRadians +
            clamp(pitchNext - terrainPitchRadians, -maxPitchStep, maxPitchStep);
        terrainRollRadians =
            terrainRollRadians +
            clamp(rollNext - terrainRollRadians, -maxRollStep, maxRollStep);
    }

    private double averageWheelGroundHeight(Vector rootPos, int a, int b) {
        return (
            (wheelGroundHeight(rootPos, a) + wheelGroundHeight(rootPos, b)) *
            0.5D
        );
    }

    private double wheelGroundHeight(Vector rootPos, int index) {
        WheelState wheel = wheels[index];
        if (wheel.grounded && wheel.contactPoint != null) {
            return wheel.contactPoint.getY() - rootPos.getY();
        }
        Vector wheelPhysicsOffset = new Vector(
            wheelOffsets[index].getX(),
            wheelOffsets[index].getY() - physicsWheelYOffset,
            wheelOffsets[index].getZ()
        );
        return (
            wheelPhysicsOffset.getY() +
            settings.suspensionRest -
            wheel.suspensionLength -
            settings.wheelRadius
        );
    }

    private int groundedWheelCount(int a, int b) {
        int count = 0;
        if (wheels[a].grounded) {
            count++;
        }
        if (wheels[b].grounded) {
            count++;
        }
        return count;
    }

    private static Vector[] cloneVectors(Vector[] source) {
        Vector[] out = new Vector[source.length];
        for (int i = 0; i < source.length; i++) {
            out[i] = source[i].clone();
        }
        return out;
    }

    private static ItemStack[] normalizeWheelModels(ItemStack[] wheelModels) {
        ItemStack fallback = new ItemStack(Material.IRON_INGOT);
        ItemStack[] out = new ItemStack[] {
            fallback.clone(),
            fallback.clone(),
            fallback.clone(),
            fallback.clone(),
        };
        if (wheelModels == null || wheelModels.length == 0) {
            return out;
        }
        for (int i = 0; i < out.length; i++) {
            ItemStack value =
                i < wheelModels.length ? wheelModels[i] : wheelModels[0];
            out[i] = (value == null ? fallback : value).clone();
        }
        return out;
    }

    private static ItemStack createDisplayModelStack(
        Material material,
        int cmd
    ) {
        ItemStack item = new ItemStack(material);
        if (cmd >= 0) {
            item.editMeta(meta -> meta.setCustomModelData(cmd));
        }
        return item;
    }

    private static final class BlockKey {

        private final int x;
        private final int y;
        private final int z;

        private BlockKey(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BlockKey other)) {
                return false;
            }
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(x);
            result = 31 * result + Integer.hashCode(y);
            result = 31 * result + Integer.hashCode(z);
            return result;
        }
    }

    private static final class WheelState {

        private boolean grounded;
        private double suspensionLength;
        private double lastSuspensionLength;
        private double visualSuspensionTravel;
        private Vector contactPoint;
    }

    private void initializeDamageState(double initialHealth) {
        if (!settings.damage.enabled) {
            this.health = clamp(initialHealth, 0.0D, settings.maxHealth);
            this.frontHealth = 0.0D;
            this.rearHealth = 0.0D;
            for (int i = 0; i < wheelHealth.length; i++) {
                wheelHealth[i] = 0.0D;
            }
            return;
        }
        this.health = clamp(initialHealth, 0.0D, settings.maxHealth);
        double sourceMax = Math.max(1.0D, settings.maxHealth);
        double ratio = clamp(initialHealth / sourceMax, 0.0D, 1.0D);
        frontHealth = settings.damage.frontMaxHealth * ratio;
        rearHealth = settings.damage.rearMaxHealth * ratio;
        for (int i = 0; i < wheelHealth.length; i++) {
            wheelHealth[i] = settings.damage.wheelMaxHealth * ratio;
        }
        syncLegacyHealthFromParts();
    }

    private DamagePart wheelPartByIndex(int index) {
        return switch (index) {
            case 0 -> DamagePart.WHEEL_FL;
            case 1 -> DamagePart.WHEEL_FR;
            case 2 -> DamagePart.WHEEL_RL;
            default -> DamagePart.WHEEL_RR;
        };
    }

    private DamagePart resolveFrontOrRearByLocalZ(Vector worldPos) {
        Vector rel = worldPos.clone().subtract(root.getLocation().toVector());
        Vector local = rotateYaw(rel, -yawDegrees);
        return local.getZ() >= 0.0D ? DamagePart.FRONT : DamagePart.REAR;
    }

    private DamagePart resolvePartByActorLocation(Location actorLocation) {
        Vector rel = actorLocation
            .toVector()
            .subtract(root.getLocation().toVector());
        Vector local = rotateYaw(rel, -yawDegrees);
        if (settings.damage.wheelEnabled) {
            int best = -1;
            double bestDist = Double.MAX_VALUE;
            for (int i = 0; i < wheelOffsets.length; i++) {
                double distSq = wheelOffsets[i].distanceSquared(local);
                if (distSq < bestDist) {
                    bestDist = distSq;
                    best = i;
                }
            }
            if (best >= 0 && bestDist <= 1.80D) {
                return wheelPartByIndex(best);
            }
        }
        return local.getZ() >= 0.0D ? DamagePart.FRONT : DamagePart.REAR;
    }

    private double getTotalDamageHealth() {
        if (!settings.damage.enabled) {
            return health;
        }
        return Math.max(
            0.0D,
            frontHealth +
                rearHealth +
                wheelHealth[0] +
                wheelHealth[1] +
                wheelHealth[2] +
                wheelHealth[3]
        );
    }

    private double getTotalDamageMaxHealth() {
        if (!settings.damage.enabled) {
            return settings.maxHealth;
        }
        return (
            settings.damage.frontMaxHealth +
            settings.damage.rearMaxHealth +
            settings.damage.wheelMaxHealth * 4.0D
        );
    }

    private void syncLegacyHealthFromParts() {
        if (!settings.damage.enabled) {
            return;
        }
        if (settings.healthMode == CarSettings.HealthMode.PARTS_ONLY) {
            health = getTotalDamageHealth();
            return;
        }
        health = clamp(health, 0.0D, settings.maxHealth);
    }

    private double computeDamagePerformanceFactor() {
        if (!settings.damage.enabled) {
            return 1.0D;
        }
        double front = settings.damage.frontEnabled
            ? getFrontHealthPercent()
            : 1.0D;
        double rear = settings.damage.rearEnabled
            ? getRearHealthPercent()
            : 1.0D;
        double wheels = settings.damage.wheelEnabled
            ? getWheelAverageHealthPercent()
            : 1.0D;
        double partIntegrity = Math.min(front, Math.min(rear, wheels));
        double coreIntegrity = clamp(
            settings.maxHealth <= 0.0D ? 1.0D : health / settings.maxHealth,
            0.0D,
            1.0D
        );
        double integrity = switch (settings.healthMode) {
            case CAR_ONLY -> coreIntegrity;
            case PARTS_ONLY -> partIntegrity;
            case CAR_AND_PARTS -> Math.min(coreIntegrity, partIntegrity);
        };
        double start = settings.damage.slowdownStartPercent;
        if (integrity >= start) {
            return 1.0D;
        }
        double normalized =
            start <= 0.0001D ? integrity : clamp(integrity / start, 0.0D, 1.0D);
        return lerp(settings.damage.slowdownMinFactor, 1.0D, normalized);
    }

    private void applyImpactDamage(double baseDamage, double signedSpeed) {
        if (baseDamage <= 0.0D) {
            return;
        }
        if (!settings.damage.enabled) {
            damage(baseDamage);
            return;
        }
        double partFactor = getPartsDamageFactorForMode();
        if (signedSpeed >= 0.0D) {
            if (settings.damage.frontEnabled) {
                applyPartDamage(
                    DamagePart.FRONT,
                    baseDamage *
                        settings.damage.frontImpactMultiplier *
                        partFactor
                );
            }
            if (settings.damage.wheelEnabled) {
                double wheelDmg =
                    baseDamage *
                    settings.damage.wheelImpactMultiplier *
                    0.5D *
                    partFactor;
                applyPartDamage(DamagePart.WHEEL_FL, wheelDmg);
                applyPartDamage(DamagePart.WHEEL_FR, wheelDmg);
            }
        } else {
            if (settings.damage.rearEnabled) {
                applyPartDamage(
                    DamagePart.REAR,
                    baseDamage *
                        settings.damage.rearImpactMultiplier *
                        partFactor
                );
            }
            if (settings.damage.wheelEnabled) {
                double wheelDmg =
                    baseDamage *
                    settings.damage.wheelImpactMultiplier *
                    0.5D *
                    partFactor;
                applyPartDamage(DamagePart.WHEEL_RL, wheelDmg);
                applyPartDamage(DamagePart.WHEEL_RR, wheelDmg);
            }
        }
        applyCoreHealthDamageByMode(baseDamage);
        syncLegacyHealthFromParts();
        if (settings.destroyOnZeroHealth && getHealth() <= 0.0D) {
            destroy();
        }
    }

    private void applyLandingImpact(double impactSpeed) {
        if (impactSpeed <= 0.0D) {
            return;
        }

        hardLandingThisTick = true;
        landingImpactSpeedThisTick = Math.max(
            landingImpactSpeedThisTick,
            impactSpeed
        );

        if (
            !settings.landingEnabled ||
            impactSpeed < settings.landingMinImpactSpeed
        ) {
            return;
        }
        double fallHeight = estimateFallHeightBlocks(impactSpeed);
        CarSettings.LandingDamageRange range = settings.findLandingDamageRange(
            fallHeight
        );
        boolean useRangeProfile =
            settings.landingUseHeightRanges && range != null;
        if (
            settings.landingUseHeightRanges &&
            range == null &&
            settings.landingRequireRangeMatch
        ) {
            return;
        }

        if (!settings.damage.enabled) {
            double baseDamage = computeLandingBaseDamage(impactSpeed);
            if (baseDamage > 0.0D) {
                damage(baseDamage);
            }
            return;
        }

        double partFactor = getPartsDamageFactorForMode();
        if (useRangeProfile) {
            double frontDamage =
                (range.frontDamage +
                    ((settings.damage.frontMaxHealth * range.frontPercent) /
                        100.0D)) *
                partFactor;
            double rearDamage =
                (range.rearDamage +
                    ((settings.damage.rearMaxHealth * range.rearPercent) /
                        100.0D)) *
                partFactor;
            double wheelDamage =
                (range.wheelDamage +
                    ((settings.damage.wheelMaxHealth * range.wheelPercent) /
                        100.0D)) *
                partFactor;
            double coreDamage =
                range.coreDamage +
                ((settings.maxHealth * range.corePercent) / 100.0D);

            applyPartDamage(DamagePart.FRONT, frontDamage);
            applyPartDamage(DamagePart.REAR, rearDamage);
            applyPartDamage(DamagePart.WHEEL_FL, wheelDamage);
            applyPartDamage(DamagePart.WHEEL_FR, wheelDamage);
            applyPartDamage(DamagePart.WHEEL_RL, wheelDamage);
            applyPartDamage(DamagePart.WHEEL_RR, wheelDamage);
            applyCoreHealthDamageByMode(coreDamage);
        } else {
            double baseDamage = computeLandingBaseDamage(impactSpeed);
            if (baseDamage <= 0.0D) {
                return;
            }
            double bodyDamage =
                baseDamage * settings.landingBodyDamageMultiplier;
            if (bodyDamage > 0.0D) {
                double splitBodyDamage = bodyDamage * 0.5D * partFactor;
                applyPartDamage(DamagePart.FRONT, splitBodyDamage);
                applyPartDamage(DamagePart.REAR, splitBodyDamage);
            }
            if (settings.damage.wheelEnabled) {
                double wheelDamage =
                    ((baseDamage * settings.landingWheelDamageMultiplier) /
                        4.0D) *
                    partFactor;
                applyPartDamage(DamagePart.WHEEL_FL, wheelDamage);
                applyPartDamage(DamagePart.WHEEL_FR, wheelDamage);
                applyPartDamage(DamagePart.WHEEL_RL, wheelDamage);
                applyPartDamage(DamagePart.WHEEL_RR, wheelDamage);
            }
            applyCoreHealthDamageByMode(baseDamage);
        }
        syncLegacyHealthFromParts();
        if (settings.destroyOnZeroHealth && getHealth() <= 0.0D) {
            destroy();
        }
    }

    private double computeLandingBaseDamage(double impactSpeed) {
        double baseDamage =
            (impactSpeed - settings.landingMinImpactSpeed) *
            settings.landingDamageScale;
        if (settings.landingMaxTotalDamage > 0.0D) {
            baseDamage = Math.min(baseDamage, settings.landingMaxTotalDamage);
        }
        return Math.max(0.0D, baseDamage);
    }

    private static double estimateFallHeightBlocks(double impactSpeed) {
        if (impactSpeed <= 0.0D) {
            return 0.0D;
        }
        return (impactSpeed * impactSpeed) / (2.0D * 9.81D);
    }

    private void applyPartDamage(DamagePart part, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        switch (part) {
            case FRONT -> {
                if (settings.damage.frontEnabled) {
                    frontHealth = Math.max(0.0D, frontHealth - amount);
                }
            }
            case REAR -> {
                if (settings.damage.rearEnabled) {
                    rearHealth = Math.max(0.0D, rearHealth - amount);
                }
            }
            case WHEEL_FL -> {
                if (settings.damage.wheelEnabled) {
                    wheelHealth[0] = Math.max(0.0D, wheelHealth[0] - amount);
                }
            }
            case WHEEL_FR -> {
                if (settings.damage.wheelEnabled) {
                    wheelHealth[1] = Math.max(0.0D, wheelHealth[1] - amount);
                }
            }
            case WHEEL_RL -> {
                if (settings.damage.wheelEnabled) {
                    wheelHealth[2] = Math.max(0.0D, wheelHealth[2] - amount);
                }
            }
            case WHEEL_RR -> {
                if (settings.damage.wheelEnabled) {
                    wheelHealth[3] = Math.max(0.0D, wheelHealth[3] - amount);
                }
            }
        }
        ticksSinceDamage = 0;
    }

    private void regenParts(double amount) {
        if (amount <= 0.0D || !settings.damage.enabled) {
            return;
        }
        if (settings.damage.frontEnabled) {
            frontHealth = Math.min(
                settings.damage.frontMaxHealth,
                frontHealth + amount * 0.30D
            );
        }
        if (settings.damage.rearEnabled) {
            rearHealth = Math.min(
                settings.damage.rearMaxHealth,
                rearHealth + amount * 0.30D
            );
        }
        if (settings.damage.wheelEnabled) {
            for (int i = 0; i < wheelHealth.length; i++) {
                wheelHealth[i] = Math.min(
                    settings.damage.wheelMaxHealth,
                    wheelHealth[i] + amount * 0.10D
                );
            }
        }
        syncLegacyHealthFromParts();
    }

    private void updateDamageSmoke(Vector rootPos) {
        if (!settings.damage.enabled || physicsTick % 6 != 0) {
            return;
        }
        boolean forceImmobilizedSmoke =
            settings.smokeWhenImmobilized && isImmobilizedByHealth();
        if (!engineRunning && !forceImmobilizedSmoke) {
            return;
        }
        if (!shouldRenderEffectsForPlayers(rootPos)) {
            return;
        }
        double threshold = settings.damage.smokeThresholdPercent;
        if (forceImmobilizedSmoke || getFrontHealthPercent() <= threshold) {
            spawnDamageSmokeAt(
                rootPos
                    .clone()
                    .add(
                        rotateYaw(settings.damage.frontHitboxOffset, yawDegrees)
                    )
            );
        }
        if (forceImmobilizedSmoke || getRearHealthPercent() <= threshold) {
            spawnDamageSmokeAt(
                rootPos
                    .clone()
                    .add(
                        rotateYaw(settings.damage.rearHitboxOffset, yawDegrees)
                    )
            );
        }
        if (settings.damage.wheelEnabled) {
            for (int i = 0; i < wheelHealth.length; i++) {
                double pct = clamp(
                    wheelHealth[i] / settings.damage.wheelMaxHealth,
                    0.0D,
                    1.0D
                );
                if (forceImmobilizedSmoke || pct <= threshold) {
                    spawnDamageSmokeAt(
                        rootPos
                            .clone()
                            .add(rotateYaw(wheelOffsets[i], yawDegrees))
                    );
                }
            }
        }
    }

    private void spawnDamageSmokeAt(Vector pos) {
        world.spawnParticle(
            Particle.SMOKE,
            pos.getX(),
            pos.getY() + 0.2D,
            pos.getZ(),
            5,
            0.12D,
            0.06D,
            0.12D,
            0.015D
        );
    }

    private boolean shouldRenderEffectsForPlayers(Vector rootPos) {
        if (rootPos == null) {
            return true;
        }
        if (
            physicsTick - lastEffectsVisibilityCheckTick >=
            EFFECTS_VISIBILITY_RECHECK_TICKS
        ) {
            hasNearbyPlayersForEffects = !world
                .getNearbyPlayers(
                    rootPos.toLocation(world),
                    EFFECTS_VISIBILITY_RADIUS
                )
                .isEmpty();
            lastEffectsVisibilityCheckTick = physicsTick;
        }
        return hasNearbyPlayersForEffects;
    }

    private void damage(double amount) {
        if (amount <= 0.0D) {
            return;
        }
        if (settings.damage.enabled) {
            double split = (amount * getPartsDamageFactorForMode()) / 6.0D;
            applyPartDamage(DamagePart.FRONT, split);
            applyPartDamage(DamagePart.REAR, split);
            applyPartDamage(DamagePart.WHEEL_FL, split);
            applyPartDamage(DamagePart.WHEEL_FR, split);
            applyPartDamage(DamagePart.WHEEL_RL, split);
            applyPartDamage(DamagePart.WHEEL_RR, split);
            applyCoreHealthDamageByMode(amount);
            syncLegacyHealthFromParts();
            if (settings.destroyOnZeroHealth && getHealth() <= 0.0D) {
                destroy();
            }
            return;
        }
        health = Math.max(0.0D, health - amount);
        ticksSinceDamage = 0;
        if (settings.destroyOnZeroHealth && health <= 0.0D) {
            destroy();
        }
    }

    private double getPartsDamageFactorForMode() {
        return switch (settings.healthMode) {
            case CAR_ONLY -> 0.0D;
            case PARTS_ONLY -> 1.0D;
            case CAR_AND_PARTS -> Math.max(
                0.0D,
                1.0D - settings.combinedCoreDamageShare
            );
        };
    }

    private double getCoreDamageFactorForMode() {
        return switch (settings.healthMode) {
            case CAR_ONLY -> 1.0D;
            case PARTS_ONLY -> 0.0D;
            case CAR_AND_PARTS -> clamp(
                settings.combinedCoreDamageShare,
                0.0D,
                1.0D
            );
        };
    }

    private void applyCoreHealthDamageByMode(double baseDamage) {
        if (baseDamage <= 0.0D) {
            return;
        }
        double coreFactor = getCoreDamageFactorForMode();
        if (coreFactor <= 0.0D) {
            return;
        }
        health = Math.max(0.0D, health - baseDamage * coreFactor);
        ticksSinceDamage = 0;
    }

    private boolean isAnyEnabledPartZero() {
        if (!settings.damage.enabled) {
            return false;
        }
        if (settings.damage.frontEnabled && frontHealth <= 0.0D) {
            return true;
        }
        if (settings.damage.rearEnabled && rearHealth <= 0.0D) {
            return true;
        }
        if (settings.damage.wheelEnabled) {
            for (double wheelHp : wheelHealth) {
                if (wheelHp <= 0.0D) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isImmobilizedByHealth() {
        if (!settings.damage.enabled) {
            return settings.stopOnCarHealthZero && health <= 0.0D;
        }
        if (settings.stopOnCarHealthZero && health <= 0.0D) {
            return true;
        }
        return settings.stopOnAnyPartZero && isAnyEnabledPartZero();
    }

    private void consumeFuelForTick(double signedSpeed) {
        if (
            !engineRunning ||
            settings.fuelTankCapacity <= 0.0D ||
            fuelLiters <= 0.0D
        ) {
            return;
        }
        double speedPart =
            Math.abs(signedSpeed) * settings.fuelSpeedConsumptionFactor;
        double throttlePart =
            Math.max(0.0D, throttleInput) *
            settings.fuelThrottleConsumptionFactor;
        double consumption =
            settings.fuelBaseConsumptionPerTick + speedPart + throttlePart;
        drainFuel(consumption);
    }

    private void applyRamDamage(Vector carCenter, double speed) {
        if (!settings.ramDamageEnabled || speed < settings.ramDamageMinSpeed) {
            return;
        }
        if (!settings.ramAffectPlayers && !settings.ramAffectMobs) {
            return;
        }

        double radius =
            Math.max(settings.carHalfLength, settings.carHalfWidth) + 0.85D;
        double yRange = (settings.carHeight * 0.5D) + 0.9D;
        Location centerLoc = carCenter.toLocation(world);
        Vector pushDir = velocity.clone().setY(0.0D);
        if (pushDir.lengthSquared() < 0.0001D) {
            pushDir = yawToForward(yawDegrees);
        } else {
            pushDir.normalize();
        }

        Entity attacker = getDriver() instanceof Entity driver ? driver : root;
        boolean hitAny = false;
        for (Entity entity : world.getNearbyEntities(
            centerLoc,
            radius,
            yRange,
            radius
        )) {
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }
            if (
                !target.isValid() ||
                target.isDead() ||
                target.getType() == EntityType.ARMOR_STAND
            ) {
                continue;
            }
            if (isPart(entity) || isPassengerEntity(entity)) {
                continue;
            }
            if (entity instanceof Player && !settings.ramAffectPlayers) {
                continue;
            }
            if (!(entity instanceof Player) && !settings.ramAffectMobs) {
                continue;
            }
            Integer cooldownUntil = ramVictimCooldownUntilTick.get(
                entity.getUniqueId()
            );
            if (cooldownUntil != null && cooldownUntil > physicsTick) {
                continue;
            }

            Vector rel = entity.getLocation().toVector().subtract(carCenter);
            rel.setY(0.0D);
            if (rel.lengthSquared() > radius * radius) {
                continue;
            }

            double damage =
                settings.ramDamageBase +
                Math.max(0.0D, speed - settings.ramDamageMinSpeed) *
                settings.ramDamageSpeedFactor;
            damage = Math.min(damage, settings.ramDamageMax);
            if (damage <= 0.0D) {
                continue;
            }

            target.damage(damage, attacker);
            Vector kb = target
                .getVelocity()
                .add(pushDir.clone().multiply(settings.ramKnockbackHorizontal));
            kb.setY(Math.max(kb.getY(), settings.ramKnockbackVertical));
            target.setVelocity(kb);

            ramVictimCooldownUntilTick.put(
                entity.getUniqueId(),
                physicsTick + settings.ramHitCooldownTicks
            );
            hitAny = true;
        }

        if (hitAny) {
            velocity.setX(velocity.getX() * settings.ramSelfSpeedLossFactor);
            velocity.setZ(velocity.getZ() * settings.ramSelfSpeedLossFactor);
            ramHitThisTick = true;
            ramHitSpeedThisTick = speed;
        }
    }

    private boolean isPassengerEntity(Entity entity) {
        for (ArmorStand seat : seats) {
            for (Entity passenger : seat.getPassengers()) {
                if (passenger.getUniqueId().equals(entity.getUniqueId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean unmountPlayer(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        boolean removed = false;
        for (ArmorStand seat : seats) {
            if (seat == null || !seat.isValid()) {
                continue;
            }
            List<Entity> passengers = new ArrayList<>(seat.getPassengers());
            for (Entity passenger : passengers) {
                if (passenger == null) {
                    continue;
                }
                if (!playerId.equals(passenger.getUniqueId())) {
                    continue;
                }
                try {
                    seat.removePassenger(passenger);
                } catch (Throwable ignored) {}
                try {
                    passenger.leaveVehicle();
                } catch (Throwable ignored) {}
                removed = true;
            }
        }
        return removed;
    }

    private void purgeInvalidSeatPassengers() {
        for (ArmorStand seat : seats) {
            if (seat == null || !seat.isValid()) {
                continue;
            }
            List<Entity> passengers = new ArrayList<>(seat.getPassengers());
            for (Entity passenger : passengers) {
                if (passenger == null) {
                    continue;
                }
                boolean invalid = !passenger.isValid();
                boolean offlinePlayer =
                    passenger instanceof Player player && !player.isOnline();
                boolean nonLiving = !(passenger instanceof LivingEntity);
                if (!invalid && !offlinePlayer && !nonLiving) {
                    continue;
                }
                try {
                    seat.removePassenger(passenger);
                } catch (Throwable ignored) {}
                if (invalid || nonLiving) {
                    try {
                        passenger.remove();
                    } catch (Throwable ignored) {}
                }
            }
        }
    }
}
