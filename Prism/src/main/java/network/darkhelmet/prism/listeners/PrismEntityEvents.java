package network.darkhelmet.prism.listeners;

import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionFactory;
import network.darkhelmet.prism.actionlibs.RecordingQueue;
import network.darkhelmet.prism.utils.DeathUtils;
import network.darkhelmet.prism.utils.InventoryUtils;
import network.darkhelmet.prism.utils.MaterialTag;
import network.darkhelmet.prism.utils.MiscUtils;
import network.darkhelmet.prism.utils.WandUtils;
import io.github.rothes.prismcn.PrismLocalization;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class PrismEntityEvents implements Listener {

    private final Prism plugin;

    private final PrismLocalization prismLocalization;

    /**
     * Constructor.
     * @param plugin Prism
     */
    public PrismEntityEvents(Prism plugin) {
        this.plugin = plugin;
        prismLocalization = plugin.getPrismLocalization();
    }

    /**
     * EntityDamageByEntityEvent.
     * @param event EntityDamageByEntityEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEvent(final EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        final Entity entity = event.getEntity();
        final Player player = (Player) event.getDamager();

        // Cancel the event if a wand is in use
        if (WandUtils.playerUsesWandOnClick(player, entity.getLocation())) {
            event.setCancelled(true);
            return;
        }

        if (entity instanceof ItemFrame) {
            final ItemFrame frame = (ItemFrame) event.getEntity();
            // Frame is empty but an item is held
            if (!frame.getItem().getType().equals(Material.AIR)) {
                if (Prism.getIgnore().event("item-remove", player)) {
                    RecordingQueue.addToQueue(
                            ActionFactory.createItemFrame("item-remove", frame.getItem(), 1,
                                    frame.getAttachedFace(), null, entity.getLocation(), player));
                }
            }
        }
    }

    private boolean checkNotNullorAir(ItemStack stack) {
        return !(stack == null || stack.getType().equals(Material.AIR));
    }

    /**
     * EntityDeathEvent.
     * @param event EntityDeathEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(final EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        // Mob Death
        if (!(entity instanceof Player)) {
            // Log item drops
            if (Prism.getIgnore().event("item-drop", entity.getWorld())) {
                String name = prismLocalization.hasEntityLocale(entity.getType().name()) ?
                        prismLocalization.getEntityLocale(entity.getType().name()) : entity.getType().name().toLowerCase();

                // Inventory
                if (entity instanceof InventoryHolder) {
                    final InventoryHolder holder = (InventoryHolder) entity;

                    for (final ItemStack i : holder.getInventory().getContents()) {
                        if (checkNotNullorAir(i)) {
                            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-drop", i, i.getAmount(), -1,
                                    null, entity.getLocation(), name));
                        }
                    }
                }

                // Equipment
                EntityEquipment equipment = entity.getEquipment();
                if (equipment != null) {
                    for (final ItemStack i : equipment.getArmorContents()) {
                        if (checkNotNullorAir(i)) {
                            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-drop", i, i.getAmount(), -1,
                                    null, entity.getLocation(), name));
                        }
                    }
                }
                // Hand items not stored in "getArmorContents"
                ItemStack main = entity.getEquipment().getItemInMainHand();
                ItemStack off = entity.getEquipment().getItemInOffHand();

                if (checkNotNullorAir(main)) {
                    RecordingQueue.addToQueue(ActionFactory.createItemStack("item-drop", main, main.getAmount(), -1,
                            null, entity.getLocation(), name));
                }

                if (checkNotNullorAir(off)) {
                    RecordingQueue.addToQueue(ActionFactory.createItemStack("item-drop", off, off.getAmount(), -1,
                            null, entity.getLocation(), name));
                }

            }

            EntityDamageEvent damageEvent = entity.getLastDamageCause();

            Entity entitySource = null;
            Block blockSource = null;

            // Resolve source
            if (damageEvent != null && !damageEvent.isCancelled()) {
                if (damageEvent instanceof EntityDamageByEntityEvent) {
                    entitySource = ((EntityDamageByEntityEvent) damageEvent).getDamager();

                    if (entitySource instanceof Projectile) {
                        ProjectileSource ps = ((Projectile) entitySource).getShooter();

                        if (ps instanceof BlockProjectileSource) {
                            entitySource = null;
                            blockSource = ((BlockProjectileSource) ps).getBlock();
                        } else {
                            entitySource = (Entity) ps;
                        }
                    }
                } else if (damageEvent instanceof EntityDamageByBlockEvent) {
                    blockSource = ((EntityDamageByBlockEvent) damageEvent).getDamager();
                }
            }

            // Create handlers
            if (entitySource instanceof Player) {
                Player player = (Player) entitySource;

                if (!Prism.getIgnore().event("player-kill", player)) {
                    return;
                }
                RecordingQueue.addToQueue(ActionFactory.createEntity("player-kill", entity, player));
            } else if (entitySource != null) {
                if (!Prism.getIgnore().event("entity-kill", entity.getWorld())) {
                    return;
                }
                String name = entitySource.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
                RecordingQueue.addToQueue(ActionFactory.createEntity("entity-kill", entity, name));
            } else if (blockSource != null) {
                if (!Prism.getIgnore().event("entity-kill", entity.getWorld())) {
                    return;
                }
                String name = "block:" + blockSource.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
                RecordingQueue.addToQueue(ActionFactory.createEntity("entity-kill", entity, name));
            } else {
                if (!Prism.getIgnore().event("entity-kill", entity.getWorld())) {
                    return;
                }

                String name = "未知";

                if (damageEvent != null && !damageEvent.isCancelled()) {
                    switch (damageEvent.getCause()) {
                        case LIGHTNING:
                            name = "闪电";
                            break;
                        case CUSTOM:
                            name = "自定义";
                            break;
                        case FIRE:
                            name = "火焰";
                            break;
                        case FIRE_TICK:
                            name = "火焰刻";
                            break;
                        case FALL:
                            name = "摔落";
                            break;
                        case LAVA:
                            name = "熔岩";
                            break;
                        case VOID:
                            name = "虚空";
                            break;
                        case MAGIC:
                            name = "魔法";
                            break;
                        case DRYOUT:
                            name = "缺水窒息";
                            break;
                        case POISON:
                            name = "药水";
                            break;
                        case THORNS:
                            name = "荆棘附魔";
                            break;
                        case WITHER:
                            name = "凋零效果";
                            break;
                        case CONTACT:
                            name = "接触方块";
                            break;
                        case MELTING:
                            name = "融化";
                            break;
                        case SUICIDE:
                            name = "自杀";
                            break;
                        case CRAMMING:
                            name = "过于拥挤";
                            break;
                        case DROWNING:
                            name = "溺水窒息";
                            break;
                        case HOT_FLOOR:
                            name = "踩岩浆块";
                            break;
                        case PROJECTILE:
                            name = "弹射物";
                            break;
                        case STARVATION:
                            name = "饥饿度";
                            break;
                        case SUFFOCATION:
                            name = "方块窒息";
                            break;
                        case DRAGON_BREATH:
                            name = "龙息";
                            break;
                        case ENTITY_ATTACK:
                            name = "实体攻击";
                            break;
                        case FALLING_BLOCK:
                            name = "坠落的方块";
                            break;
                        case FLY_INTO_WALL:
                            name = "飞进墙里";
                            break;
                        case BLOCK_EXPLOSION:
                            name = "方块爆炸";
                            break;
                        case ENTITY_EXPLOSION:
                            name = "实体爆炸";
                            break;
                        case ENTITY_SWEEP_ATTACK:
                            name = "横扫攻击";
                            break;
                        default:
                            name = damageEvent.getCause().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
                    }
                }

                RecordingQueue.addToQueue(ActionFactory.createEntity("entity-kill", entity, name));
            }

            /*
             * if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent) { final
             * EntityDamageByEntityEvent entityDamageByEntityEvent =
             * (EntityDamageByEntityEvent) entity .getLastDamageCause();
             *
             * // Mob killed by player if (entityDamageByEntityEvent.getDamager() instanceof
             * Player) { final Player player = (Player)
             * entityDamageByEntityEvent.getDamager(); if
             * (!Prism.getIgnore().event("player-kill", player)) return;
             * RecordingQueue.addToQueue(ActionFactory.createEntity("player-kill", entity,
             * player));
             *
             * } // Mob shot by an arrow from a player else if
             * (entityDamageByEntityEvent.getDamager() instanceof Arrow) { final Arrow arrow
             * = (Arrow) entityDamageByEntityEvent.getDamager();
             *
             * if (arrow.getShooter() instanceof Player) { final Player player = (Player)
             * arrow.getShooter(); if (!Prism.getIgnore().event("player-kill", player))
             * return; RecordingQueue.addToQueue(ActionFactory.createEntity("player-kill",
             * entity, player));
             *
             * } else if (arrow.getShooter() instanceof LivingEntity) { final Entity damager
             * = (Entity) arrow.getShooter(); String name = "unknown"; if (damager != null)
             * { name = damager.getType().name().toLowerCase(); }
             *
             * if (!Prism.getIgnore().event("entity-kill", entity.getWorld())) return;
             * RecordingQueue.addToQueue(ActionFactory.createEntity("entity-kill", entity,
             * name)); } else if (arrow.getShooter() instanceof BlockProjectileSource) {
             *
             * final Block damager = (Block)
             * ((BlockProjectileSource)arrow.getShooter()).getBlock();
             *
             * if (!Prism.getIgnore().event("entity-kill", entity.getWorld())) return;
             *
             * String name = "block:" + damager.getType().name().toLowerCase();
             *
             * RecordingQueue.addToQueue(ActionFactory.createEntity("entity-kill", entity,
             * name)); } } else { // Mob died by another mob final Entity damager =
             * entityDamageByEntityEvent.getDamager(); String name = "unknown"; if (damager
             * != null) { name = damager.getType().name().toLowerCase(); }
             *
             * if (!Prism.getIgnore().event("entity-kill", entity.getWorld())) return;
             * RecordingQueue.addToQueue(ActionFactory.createEntity("entity-kill", entity,
             * name)); } } else {
             *
             * if (!Prism.getIgnore().event("entity-kill", entity.getWorld())) return;
             *
             * String killer = "unknown"; final EntityDamageEvent damage =
             * entity.getLastDamageCause(); if (damage != null) { final DamageCause cause =
             * damage.getCause(); if (cause != null) { killer = cause.name().toLowerCase();
             * } }
             *
             * // Record the death as natural
             * RecordingQueue.addToQueue(ActionFactory.createEntity("entity-kill", entity,
             * killer));
             *
             * }
             */
        } else {

            // Determine who died and what the exact cause was
            final Player p = (Player) event.getEntity();
            if (Prism.getIgnore().event("player-death", p)) {
                final String cause = DeathUtils.getCauseNiceName(p);
                String attacker = DeathUtils.getAttackerName(p);
                if (attacker.equals("PVP狼")) {
                    final String owner = DeathUtils.getTameWolfOwner(event);
                    attacker = owner + "的狼";
                }
                RecordingQueue.addToQueue(ActionFactory.createPlayerDeath("player-death", p, cause, attacker));
            }

            // Log item drops
            if (Prism.getIgnore().event("item-drop", p)) {
                if (!event.getDrops().isEmpty()) {
                    for (final ItemStack i : event.getDrops()) {
                        RecordingQueue.addToQueue(ActionFactory.createItemStack("item-drop", i, i.getAmount(), -1, null,
                                p.getLocation(), p));
                    }
                }
            }
        }
    }

    /**
     * CreatureSpawnEvent.
     * @param event CreatureSpawnEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(final CreatureSpawnEvent event) {
        if (!Prism.getIgnore().event("entity-spawn", event.getEntity().getWorld())) {
            return;
        }
        final String reason;
        switch (event.getSpawnReason()) {
            case ENDER_PEARL:
                reason = "末影珍珠";
                break;
            case COMMAND:
                reason = "指令";
                break;
            case NETHER_PORTAL:
                reason = "下界传送门";
                break;
            case EGG:
                reason = "鸡蛋";
                break;
            case RAID:
                reason = "突袭";
                break;
            case TRAP:
                reason = "触发";
                break;
            case CURED:
                reason = "治愈村民";
                break;
            case MOUNT:
                reason = "坐骑";
                break;
            case CUSTOM:
                reason = "自定义";
                break;
            case JOCKEY:
                reason = "骑士";
                break;
            case PATROL:
                reason = "灾厄巡逻队";
                break;
            case BEEHIVE:
                reason = "蜂箱";
                break;
            case DEFAULT:
                reason = "默认值";
                break;
            case DROWNED:
                reason = "溺死";
                break;
            case NATURAL:
                reason = "自然";
                break;
            case SHEARED:
                reason = "剪哞菇";
                break;
            case SPAWNER:
                reason = "刷怪笼";
                break;
            case BREEDING:
                reason = "喂养";
                break;
            case EXPLOSION:
                reason = "爆炸";
                break;
            case INFECTION:
                reason = "感染";
                break;
            case LIGHTNING:
                reason = "闪电";
                break;
            case OCELOT_BABY:
                reason = "海龟宝宝";
                break;
            case SLIME_SPLIT:
                reason = "史莱姆分裂";
                break;
            case SPAWNER_EGG:
                reason = "刷怪蛋";
                break;
            case BUILD_WITHER:
                reason = "摆放凋灵";
                break;
            case DISPENSE_EGG:
                reason = "发射器发射鸡蛋";
                break;
            case BUILD_SNOWMAN:
                reason = "摆放雪傀儡";
                break;
            case REINFORCEMENTS:
                reason = "增援";
                break;
            case BUILD_IRONGOLEM:
                reason = "摆放铁傀儡";
                break;
            case SHOULDER_ENTITY:
                reason = "肩膀实体";
                break;
            case VILLAGE_DEFENSE:
                reason = "村庄防御";
                break;
            case PIGLIN_ZOMBIFIED:
                reason = "猪灵转化";
                break;
            case SILVERFISH_BLOCK:
                reason = "蠹虫刷怪石";
                break;
            case VILLAGE_INVASION:
                reason = "僵尸围城";
                break;
            case CHUNK_GEN:
                reason = "区块生成";
                break;
            default:
                reason = event.getSpawnReason().name().toLowerCase().replace("_", " ");
        }
        if (reason.equals("自然")) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createEntity("entity-spawn", event.getEntity(), reason));
    }

    /**
     * EntityTargetEvent.
     * @param event EntityTargetEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTargetEvent(final EntityTargetEvent event) {
        if (!Prism.getIgnore().event("entity-follow", event.getEntity().getWorld())) {
            return;
        }
        if (event.getTarget() instanceof Player) {
            if (event.getEntity().getType().equals(EntityType.CREEPER)) {
                final Player player = (Player) event.getTarget();
                RecordingQueue.addToQueue(ActionFactory.createEntity("entity-follow", event.getEntity(), player));
            }
        }
    }

    /**
     * PlayerShearEntityEvent.
     * @param event PlayerShearEntityEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerShearEntity(final PlayerShearEntityEvent event) {
        if (!Prism.getIgnore().event("entity-shear", event.getPlayer())) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createEntity("entity-shear", event.getEntity(), event.getPlayer()));
    }

    /**
     * PlayerInteractAtEntityEvent.
     * @param event PlayerInteractAtEntityEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void interactAtVariant(final PlayerInteractAtEntityEvent event) {

        final Player p = event.getPlayer();
        final Entity e = event.getRightClicked();
        final ItemStack hand = p.getInventory().getItemInMainHand();
        // @todo right clicks should technically follow blockface
        // Cancel the event if a wand is in use
        if (WandUtils.playerUsesWandOnClick(p, e.getLocation())) {
            event.setCancelled(true);
            return;
        }

        if (e instanceof ArmorStand) {
            Vector at = event.getClickedPosition();
            ArmorStand stand = (ArmorStand) e;

            if (hand.getType() != Material.AIR) {
                EquipmentSlot target = InventoryUtils.getTargetArmorSlot(hand.getType());

                if (stand.hasArms() || target != EquipmentSlot.HAND) {
                    ItemStack atSlot = InventoryUtils.getEquipment(stand.getEquipment(), target);

                    if (atSlot.getType() != Material.AIR) {
                        RecordingQueue.addToQueue(
                                ActionFactory.createItemStack("item-remove", atSlot, 1, target,
                                        null, e.getLocation(), p));
                    }
                    RecordingQueue.addToQueue(
                            ActionFactory.createItemStack("item-insert", hand, 1, target,
                                    null, e.getLocation(), p));
                }
            } else {
                double elevation = at.getY();

                EquipmentSlot slot;
                boolean hasChestPlate;
                {
                    ItemStack chestPlate = Objects.requireNonNull(stand.getEquipment()).getChestplate();
                    hasChestPlate = chestPlate != null && chestPlate.getType() != Material.AIR;
                }

                if (elevation >= 1.6) {
                    slot = EquipmentSlot.HEAD;
                } else if (hasChestPlate && elevation >= 0.9) {
                    slot = EquipmentSlot.CHEST;
                } else if (!hasChestPlate && elevation >= 1.2) {
                    slot = EquipmentSlot.HAND;
                } else if (elevation >= 0.55) {
                    slot = EquipmentSlot.LEGS;
                } else {
                    slot = EquipmentSlot.FEET;
                }

                ItemStack atSlot = InventoryUtils.getEquipment(stand.getEquipment(), slot);

                if (atSlot.getType() != Material.AIR) {
                    RecordingQueue.addToQueue(
                            ActionFactory.createItemStack("item-remove", atSlot, 1, slot, null, e.getLocation(), p));
                }
            }
        }
    }

    /**
     * PlayerInteractEntityEvent.
     * @param event PlayerInteractEntityEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntityEvent(final PlayerInteractEntityEvent event) {

        final Player p = event.getPlayer();
        final Entity e = event.getRightClicked();
        final ItemStack hand = p.getInventory().getItemInMainHand();
        // @todo right clicks should technically follow blockface
        // Cancel the event if a wand is in use
        if (WandUtils.playerUsesWandOnClick(p, e.getLocation())) {
            event.setCancelled(true);
            return;
        }

        if (e instanceof ItemFrame) {
            final ItemFrame frame = (ItemFrame) e;

            // If held item doesn't equal existing item frame object type
            if (!frame.getItem().getType().equals(Material.AIR)) {
                RecordingQueue.addToQueue(ActionFactory.createPlayer("item-rotate", event.getPlayer(),
                        frame.getRotation().name().toLowerCase()));
            }

            // Frame is empty but an item is held
            if (frame.getItem().getType().equals(Material.AIR) && hand != null) {
                if (Prism.getIgnore().event("item-insert", p)) {
                    RecordingQueue.addToQueue(
                            ActionFactory.createItemFrame("item-insert", hand, 1, frame.getAttachedFace(),
                                    null, e.getLocation(), p));
                }
            }
        }

        if (hand != null) {
            // if they're holding coal (or charcoal, a subitem) and they click a
            // powered minecart
            if (hand.getType() == Material.COAL && e instanceof PoweredMinecart) {
                if (!Prism.getIgnore().event("item-insert", p)) {
                    return;
                }
                RecordingQueue
                        .addToQueue(ActionFactory.createItemStack("item-insert", hand, 1, 0,
                                null, e.getLocation(), p));
            }

            if (!Prism.getIgnore().event("entity-dye", p)) {
                return;
            }
            // Only track the event on sheep, when player holds dye
            if (MaterialTag.DYES.isTagged(hand.getType()) && e.getType() == EntityType.SHEEP) {
                final String newColor = Prism.getItems().getAlias(hand.getType(), null);
                RecordingQueue.addToQueue(
                        ActionFactory.createEntity("entity-dye", event.getRightClicked(), event.getPlayer(),
                                newColor));
            }
        }
    }

    /**
     * EntityBreakDoorEvent.
     * @param event EntityBreakDoorEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreakDoor(final EntityBreakDoorEvent event) {
        if (!Prism.getIgnore().event("entity-break", event.getEntity().getWorld())) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createBlock("entity-break", event.getBlock(),
                prismLocalization.hasEntityLocale(event.getEntityType().name()) ?
                        prismLocalization.getEntityLocale(event.getEntityType().name()) : event.getEntityType().name().toLowerCase()));
    }

    /**
     * PlayerLeashEntityEvent.
     * @param event PlayerLeashEntityEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerEntityLeash(final PlayerLeashEntityEvent event) {
        if (!Prism.getIgnore().event("entity-leash", event.getPlayer())) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createEntity("entity-leash", event.getEntity(), event.getPlayer()));
    }

    /**
     * PlayerUnleashEntityEvent.
     * @param event PlayerUnleashEntityEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerEntityUnleash(final PlayerUnleashEntityEvent event) {
        if (!Prism.getIgnore().event("entity-unleash", event.getPlayer())) {
            return;
        }
        RecordingQueue.addToQueue(ActionFactory.createEntity("entity-unleash", event.getEntity(), event.getPlayer()));
    }

    /**
     * EntityUnleashEvent.
     * @param event EntityUnleashEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityUnleash(final EntityUnleashEvent event) {
        if (!Prism.getIgnore().event("entity-unleash")) {
            return;
        }
        final String reason;
        switch (event.getReason()) {
            case UNKNOWN:
                reason = "未知";
                break;
            case DISTANCE:
                reason = "距离";
                break;
            case HOLDER_GONE:
                reason = "生物消失";
                break;
            case PLAYER_UNLEASH:
                reason = "解栓";
                break;
            default:
                reason = event.getReason().toString().toLowerCase();
        }
        RecordingQueue.addToQueue(ActionFactory.createEntity("entity-unleash", event.getEntity(),
                reason));
    }

    /**
     * PotionSplashEvent.
     * @param event PotionSplashEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionSplashEvent(final PotionSplashEvent event) {

        final ProjectileSource source = event.getPotion().getShooter();

        // Ignore from non-players for the time being
        if (!(source instanceof Player)) {
            return;
        }

        final Player player = (Player) source;

        if (!Prism.getIgnore().event("potion-splash", player)) {
            return;
        }

        // What type?
        // Right now this won't support anything with multiple effects
        final Collection<PotionEffect> potion = event.getPotion().getEffects();
        String name = "";
        for (final PotionEffect eff : potion) {
            name = prismLocalization.hasEffectLocale(eff.getType().getName()) ?
                    prismLocalization.getEffectLocale(eff.getType().getName()) : eff.getType().getName().toLowerCase();
        }

        RecordingQueue.addToQueue(ActionFactory.createPlayer("potion-splash", player, name));

    }

    /**
     * HangingPlaceEvent.
     * @param event HangingPlaceEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingPlaceEvent(final HangingPlaceEvent event) {
        // Cancel the event if a wand is in use
        if (event.getPlayer() != null) {
            if (WandUtils.playerUsesWandOnClick(event.getPlayer(), event.getEntity().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
        if (!Prism.getIgnore().event("hangingitem-place", event.getPlayer())) {
            return;
        }
        RecordingQueue
                .addToQueue(ActionFactory.createHangingItem("hangingitem-place", event.getEntity(),
                        event.getPlayer()));
    }

    /**
     * Hanging items broken by a player fall under the HangingBreakByEntityEvent
     * events. This is merely here to capture cause = physics for when they detach
     * from a block.
     *
     * @param event HangingBreakEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingBreakEvent(final HangingBreakEvent event) {

        // Ignore other causes. Entity cause already handled.
        if (!event.getCause().equals(RemoveCause.PHYSICS)) {
            return;
        }

        if (!Prism.getIgnore().event("hangingitem-break", event.getEntity().getWorld())) {
            return;
        }

        final Hanging e = event.getEntity();

        // Check for planned hanging item breaks
        final String coord_key = e.getLocation().getBlockX() + ":" + e.getLocation().getBlockY() + ":"
                + e.getLocation().getBlockZ();

        String value = plugin.preplannedBlockFalls.remove(coord_key);

        if (value == null) {
            value = "未知";
        }

        Player player = null;
        try {
            player = Bukkit.getPlayer(UUID.fromString(value));
        } catch (Exception ignored) {
            //ignored.
        }

        // Track the hanging item break
        if (player != null) {
            RecordingQueue.addToQueue(ActionFactory.createHangingItem("hangingitem-break", e, player));
        } else {
            RecordingQueue.addToQueue(ActionFactory.createHangingItem("hangingitem-break", e, value));
        }

        plugin.preplannedBlockFalls.remove(coord_key);

        if (!Prism.getIgnore().event("item-remove", event.getEntity().getWorld())) {
            return;
        }

        // If an item frame, track it's contents
        if (e instanceof ItemFrame) {
            final ItemFrame frame = (ItemFrame) e;
            if (!checkNotNullorAir(frame.getItem())) {
                if (player != null) {
                    RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", frame.getItem(),
                            frame.getItem().getAmount(), -1, null, e.getLocation(), player));
                } else {
                    RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", frame.getItem(),
                            frame.getItem().getAmount(), -1, null, e.getLocation(), value));
                }
            }
        }
    }

    /**
     * HangingBreakByEntityEvent.
     * @param event HangingBreakByEntityEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHangingBreakByEntityEvent(final HangingBreakByEntityEvent event) {

        final Entity entity = event.getEntity();
        final Entity remover = event.getRemover();
        Player player = null;
        if (remover instanceof Player) {
            player = (Player) remover;
        }
        // Cancel the event if a wand is in use
        if (player != null && WandUtils.playerUsesWandOnClick(player, event.getEntity().getLocation())) {
            event.setCancelled(true);
            return;
        }

        if (!Prism.getIgnore().event("hangingitem-break", event.getEntity().getWorld())) {
            return;
        }
        String breakingName = (remover == null) ? "NULL" : prismLocalization.hasEntityLocale(remover.getType().name()) ?
                prismLocalization.getEntityLocale(remover.getType().name()) : remover.getType().name().toLowerCase();
        if (player != null) {
            RecordingQueue.addToQueue(ActionFactory.createHangingItem("hangingitem-break", event.getEntity(), player));
        } else {
            RecordingQueue
                    .addToQueue(ActionFactory.createHangingItem("hangingitem-break", event.getEntity(), breakingName));
        }
        if (!Prism.getIgnore().event("item-remove", event.getEntity().getWorld())) {
            return;
        }
        // If an item frame, track it's contents
        if (event.getEntity() instanceof ItemFrame) {
            final ItemFrame frame = (ItemFrame) event.getEntity();
            if (!checkNotNullorAir(frame.getItem())) {
                RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", frame.getItem(),
                        frame.getItem().getAmount(), -1, null, entity.getLocation(), breakingName));
            }
        }
    }

    /**
     * EntityChangeBlockEvent.
     * @param event EntityChangeBlockEvent
     */

    // TODO: This is a mess. Please, for the love of god, revisit and fix.
    @Deprecated
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        final String entity = MiscUtils.getEntityName(event.getEntity());

        // Technically I think that I really should name it "entity-eat" for
        // better consistency and
        // in case other mobs ever are made to eat. But that's not as fun
        Material to = event.getTo();
        Material from = event.getBlock().getType();
        if (from == Material.GRASS && to == Material.DIRT) {
            if (event.getEntityType() != EntityType.SHEEP) {
                return;
            }
            if (!Prism.getIgnore().event("sheep-eat", event.getBlock())) {
                return;
            }
            RecordingQueue.addToQueue(ActionFactory.createBlock("sheep-eat", event.getBlock(), entity));
        } else if (to == Material.AIR ^ from == Material.AIR && event.getEntity() instanceof Enderman) {
            if (from == Material.AIR) {
                if (!Prism.getIgnore().event("enderman-place", event.getBlock())) {
                    return;
                }
                BlockState state = event.getBlock().getState();
                state.setType(to);
                RecordingQueue.addToQueue(ActionFactory.createBlock("enderman-place", state, entity));
            } else {
                if (!Prism.getIgnore().event("enderman-pickup", event.getBlock())) {
                    return;
                }

                BlockState state = event.getBlock().getState();
                state.setBlockData(event.getBlockData());
                RecordingQueue.addToQueue(ActionFactory.createBlock("enderman-pickup", state, entity));
            }
        } else if (to == Material.AIR && event.getEntity() instanceof Wither) {
            if (!Prism.getIgnore().event("entity-break", event.getBlock())) {
                return;
            }
            RecordingQueue.addToQueue(ActionFactory.createBlock("block-break", event.getBlock(),
                    event.getEntityType().name().toLowerCase()));
        }
    }

    /**
     * EntityBlockFormEvent.
     * @param event EntityBlockFormEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlockForm(final EntityBlockFormEvent event) {
        if (!Prism.getIgnore().event("entity-form", event.getBlock())) {
            return;
        }
        final Block block = event.getBlock();
        final Location loc = block.getLocation();
        final BlockState newState = event.getNewState();

        if (event.getEntity() instanceof Player) {
            final Player player = (Player)event.getEntity();
            RecordingQueue.addToQueue(ActionFactory.createBlockChange("entity-form", block.getType(),
                    block.getBlockData(), newState, player));
        } else {
            final String entity = prismLocalization.hasEntityLocale(event.getEntity().getType().name()) ?
                    prismLocalization.getEntityLocale(event.getEntity().getType().name()) : event.getEntity().getType().name().toLowerCase();
            RecordingQueue.addToQueue(ActionFactory.createBlockChange("entity-form", block.getType(),
                    block.getBlockData(), newState, entity));
        }
    }
}
