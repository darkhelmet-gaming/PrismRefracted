package network.darkhelmet.prism.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionFactory;
import network.darkhelmet.prism.actionlibs.RecordingQueue;
import network.darkhelmet.prism.utils.MaterialTag;
import network.darkhelmet.prism.utils.block.Utilities;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PrismExplodeEvents implements Listener {

    private final Prism plugin;

    /**
     * Constructor.
     * @param plugin Prism
     */
    public PrismExplodeEvents(Prism plugin) {
        this.plugin = plugin;
    }

    private final Cache<Object, List<Cause>> weakCache = CacheBuilder
            .newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();


    /*
        =============================
        ##      Cache Handler      ##
        =============================
    */

    public void addCache(Object obj, String action, String actor) {
        addCache(obj, null, action, actor, null);
    }

    public void addCache(Object obj, Object old, String action, String actor) {
        addCache(obj, old, action, actor, null);
    }

    public void addCache(Object obj, Object old, String action, String actor, Material blockType) {
        ArrayList<Cause> causes;
        if (old != null) {
            List<Cause> oldCache = getCache(old);
            if (oldCache != null) {
                causes = new ArrayList<>(oldCache);
            } else {
                causes = new ArrayList<>();
            }
        } else {
            causes = new ArrayList<>();
        }

        if (action != null) {
            causes.add(0, new Cause(action, actor, blockType));
        }
        weakCache.put(obj, causes);
    }

    public List<Cause> getCache(Object obj) {
        return weakCache.getIfPresent(obj);
    }


    /*
        ============================
        ##  Explode Reason Track  ##
        ============================
    */

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractCreeper(PlayerInteractEntityEvent e) {
        Entity clicked = e.getRightClicked();
        if (clicked instanceof Creeper) {
            ItemStack mainHand = e.getPlayer().getInventory().getItemInMainHand();
            ItemStack offHand = e.getPlayer().getInventory().getItemInOffHand();
            if ((mainHand != null && mainHand.getType() == Material.FLINT_AND_STEEL)
                    || (offHand != null && offHand.getType() == Material.FLINT_AND_STEEL)) {
                addCache(clicked, "igniting", e.getPlayer().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clickedBlock = e.getClickedBlock();
        Location location = clickedBlock.getLocation();
        Material material = clickedBlock.getBlockData().getMaterial();
        if (clickedBlock.getBlockData() instanceof Bed) {
            Bed bed = (Bed) clickedBlock.getBlockData();
            Location subtract = location.clone().subtract(bed.getFacing().getDirection());
            if (bed.getPart() == Bed.Part.FOOT) {
                location.add(bed.getFacing().getDirection());
            }
            addCache(location, null, "clicking", e.getPlayer().getName(), material);
            addCache(subtract, null, "clicking", e.getPlayer().getName(), material);
        }
        if (clickedBlock.getBlockData() instanceof RespawnAnchor) {
            addCache(clickedBlock.getLocation(), null, "clicking", e.getPlayer().getName(), material);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        Projectile entity = e.getEntity();
        ProjectileSource shooter = entity.getShooter();
        String projName = entity.getName().toLowerCase(Locale.ROOT).replace("_", " ");
        if (shooter == null) {
            // Nullable
            addCache(entity, "shooting " + projName, null);
            return;
        }

        if (shooter instanceof Entity) {
            if (shooter instanceof Mob && ((Mob) shooter).getTarget() != null) {
                // Ghast or something, attacking player or others.
                addCache(shooter, "combat " + ((Mob) shooter).getName().toLowerCase(Locale.ROOT) + " shooting " + projName, ((Mob) shooter).getTarget().getName());
                addCache(entity, "combat " + ((Mob) shooter).getName().toLowerCase(Locale.ROOT) + " shooting " + projName, ((Mob) shooter).getTarget().getName());
                return;
            }
            // Mostly players.
            addCache(shooter, "shooting " + projName, ((Entity) shooter).getName());
            addCache(entity, "shooting " + projName, ((Entity) shooter).getName());

        } else if (shooter instanceof BlockProjectileSource) {
            // Like dispenser with fireball.....
            addCache(shooter, "shooting " + projName, ((BlockProjectileSource) shooter).getBlock().getType().name());
            addCache(entity, "shooting " + projName, ((BlockProjectileSource) shooter).getBlock().getType().name());
        } else {
            // If not listed here...
            addCache(shooter, "shooting " + projName, shooter.getClass().getName());
            addCache(entity, "shooting " + projName, shooter.getClass().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrimedTNTSpawn(EntitySpawnEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof TNTPrimed))
            return;
        TNTPrimed tntPrimed = (TNTPrimed) entity;
        Entity source = tntPrimed.getSource();
        // TODO: Null if being shot by dispenser with fireball, how to deal with?
        if (source != null) {
            if (getCache(source) != null) {
                addCache(entity, source, null, null);
                return;
            }
            if (source.getType() == EntityType.PLAYER) {
                // Don't return because it's possible caused by other exploded blocks by the player.
                addCache(entity, null, "igniting", source.getName());
            }
        }

        Location blockCorner = entity.getLocation().clone().subtract(0.5, 0, 0.5);
        Location nearby = null;
        Location self = null;
        for (Object key : weakCache.asMap().keySet()) {
            if (key instanceof Location) {
                Location loc = (Location) key;
                if (loc.getWorld().equals(blockCorner.getWorld())) {
                    if (loc.distance(blockCorner) < 0.5) {
                        // Self is mostly tnt, useless.
                        self = loc;
                    } else if (loc.distance(blockCorner) < 1.5) {
                        // Can be redstone or something nearby
                        nearby = loc;
                    }
                }
            }
        }
        if (nearby != null) {
            addCache(entity, nearby, null, null);
            return;
        }
        if (self != null) {
            addCache(entity, self, null, null);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        addCache(block.getLocation(),
                "placing " + block.getType().name().toLowerCase(Locale.ROOT).replace("_", " "),
                event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHitEndCrystal(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof EnderCrystal))
            return;
        if (e.getDamager() instanceof Player) {
            addCache(e.getEntity(), "hiting", e.getDamager().getName());
        } else {
            if (getCache(e.getDamager()) != null) {
                addCache(e.getEntity(), e.getDamager(), null, null);
            } else if (e.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) e.getDamager();
                if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) {
                    addCache(e.getEntity(), "shooting", ((Player) projectile.getShooter()).getName());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockIgnite(BlockIgniteEvent e) {
        Location location = e.getBlock().getLocation();

        Entity entity = e.getIgnitingEntity();
        if (entity != null) {
            if (entity.getType() == EntityType.PLAYER) {
                addCache(location, "igniting", e.getPlayer().getName());

            } else if (getCache(entity) != null) {
                addCache(location, entity, null, null);

            } else if (entity instanceof Projectile) {
                if (((Projectile) entity).getShooter() != null) {
                    ProjectileSource shooter = ((Projectile) entity).getShooter();
                    if (shooter instanceof Player) {
                        addCache(location, "shooting", ((Player) shooter).getName());
                    }
                }
            }

        } else if (e.getIgnitingBlock() != null) {
            if (getCache(e.getIgnitingBlock().getLocation()) != null) {
                addCache(location, e.getIgnitingBlock().getLocation(), null, null);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getHitEntity() instanceof ExplosiveMinecart || e.getEntityType() == EntityType.ENDER_CRYSTAL) {
            if (e.getEntity().getShooter() != null && e.getEntity().getShooter() instanceof Player) {
                if (getCache(e.getEntity()) != null) {
                    addCache(e.getHitEntity(), e.getEntity(), null, null);
                } else {
                    if (e.getEntity().getShooter() != null && e.getEntity().getShooter() instanceof Player) {
                        addCache(e.getHitEntity(), null, ((Player) e.getEntity().getShooter()).getName());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockRedstone(BlockRedstoneEvent e) {
        // TODO  waste ram and need to detect the player.
        addCache(e.getBlock().getLocation(), "redstone", null);
    }


    /*
        ============================
        ##  Explode Events Track  ##
        ============================
    */

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent e) {
        Block block = e.getBlock();
        Location location = block.getLocation();
        boolean blockTrack = true;
        List<Cause> causes = getCache(block);
        if (causes == null) {
            blockTrack = false;
            causes = getCache(location);
        }
        final String action;

        final Material blockType;
        if (causes == null) {
            if (!Prism.getIgnore().event("block-explode", block)) {
                return;
            }
            blockType = null;
            action = "block-explode";
        } else {
            blockType = causes.get(0).blockType;
            if (MaterialTag.BEDS.isTagged(blockType)) {
                if (!Prism.getIgnore().event("bed-explode", block)) {
                    return;
                }
                action = "bed-explode";
            } else if (blockType == Material.RESPAWN_ANCHOR) {
                if (!Prism.getIgnore().event("respawnanchor-explode", block)) {
                    return;
                }
                action = "respawnanchor-explode";
            } else {
                if (!Prism.getIgnore().event("block-explode", block)) {
                    return;
                }
                action = "block-explode";
            }
        }

        final String blockName;
        final String niceName;
        if (blockType == null) {
            blockName = null;
            niceName = getNiceFullName(null, causes);
        } else {
            blockName = blockType.name().toLowerCase().replace("_", " ");
            niceName = getNiceFullName(blockName, causes);
        }
        contructExplodeEvent(action, niceName, e.blockList(), blockTrack ? block : location, blockName);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent e) {
        List<Block> blockList = e.blockList();
        if (blockList.isEmpty()) {
            return;
        }
        Entity entity = e.getEntity();
        List<Cause> causes = getCache(entity);

        String action = "entity-explode";
        String entityName = null;
        String overrideActor = null;

        if (entity == null) {
            if (!Prism.getIgnore().event("entity-explode", e.getLocation().getWorld())) {
                return;
            }
        } else if (entity instanceof TNTPrimed) {
            if (!Prism.getIgnore().event("tnt-explode", entity.getWorld())) {
                return;
            }
            action = "tnt-explode";
            entityName = "tnt";
        } else if (entity instanceof Creeper) {
            if (!Prism.getIgnore().event("creeper-explode", entity.getWorld())) {
                return;
            }
            if (causes == null) {
                overrideActor = "creeper < combat (" + ((Creeper) entity).getTarget().getName() + ")";
            }
            action = "creeper-explode";
        } else if (entity instanceof EnderDragon) {
            if (!Prism.getIgnore().event("dragon-eat", entity.getWorld())) {
                return;
            }
            action = "dragon-eat";
            entityName = "enderdragon";
        } else if (entity instanceof EnderCrystal) {
            // TODO if we need a action for this
            if (!Prism.getIgnore().event("entity-explode", e.getEntity().getWorld())) {
                return;
            }
        } else if (entity instanceof ExplosiveMinecart) {
            if (!Prism.getIgnore().event("entity-explode", e.getEntity().getWorld())) {
                return;
            }
            Location blockCorner = entity.getLocation().clone().subtract(0.5, 0, 0.5);
            Location nearby = null;
            Location rail = null;
            for (Object key : weakCache.asMap().keySet()) {
                if (key instanceof Location) {
                    Location loc = (Location) key;
                    if (loc.getWorld().equals(blockCorner.getWorld())) {
                        if (loc.distance(blockCorner) < 0.5) {
                            if (MaterialTag.RAILS.isTagged(loc.getBlock().getType())) {
                                // Rail is useless for getting who make it explode if using redstone.
                                rail = loc;
                                continue;
                            }
                            contructExplodeEvent(action, getNiceFullName("tntminecart", getCache(key)), e.blockList(), entity, "tntminecart");
                            return;
                        } else if (loc.distance(blockCorner) < 1.5) {
                            // Can be redstone or something nearby
                            nearby = loc;
                        }
                    }
                }
            }
            if (nearby != null) {
                contructExplodeEvent(action, getNiceFullName("tntminecart", getCache(nearby)), e.blockList(), entity, "tntminecart");
                return;
            } else if (rail != null) {
                contructExplodeEvent(action, getNiceFullName("tntminecart", getCache(rail)), e.blockList(), entity, "tntminecart");
                return;
            }

            entityName = "tntminecart";
        } else {
            if (!Prism.getIgnore().event("entity-explode", e.getLocation().getWorld())) {
                return;
            }
        }

        if (entityName == null) {
            if (entity == null) {
                entityName = "magic";
            } else {
                entityName = entity.getName().toLowerCase(Locale.ROOT);
            }
        }

        // Last damage is the cause
        if (causes == null) {
            EntityDamageEvent lastDamageCause = entity.getLastDamageCause();
            if (lastDamageCause instanceof EntityDamageByEntityEvent) {
                addCache(entity, "combat", ((EntityDamageByEntityEvent) lastDamageCause).getDamager().getName());
                causes = getCache(entity);
            }
        }

        String fullActor;
        if (overrideActor == null) {
            fullActor = getNiceFullName(entityName, causes);
        } else {
            fullActor = overrideActor;
        }

        contructExplodeEvent(action, fullActor, e.blockList(), entity, entityName);
        weakCache.invalidate(entity);
    }

    private String getNiceFullName(String head, List<Cause> causes) {
        if (causes == null) {
            return "unknown";
        }
        StringBuilder builder = new StringBuilder();
        String lastAction = head;
        if (head != null) {
            builder.append(" < ").append(head);
        }
        byte count = 1;
        for (Cause cause : causes) {
            if (cause.action.equals(lastAction)) {
                count++;
            } else {
                if (count > 1) {
                    builder.append(" x").append(count);
                }
                builder.append(" < ");
                count = 1;
                lastAction = cause.action;
                builder.append(cause.action);
                if (cause.actor != null) {
                    builder.append(" (").append(cause.actor).append(')');
                }
            }
        }
        if (count > 1) {
            builder.append(" x").append(count);
        }
        return builder.substring(3, builder.length());
    }

    protected void contructExplodeEvent(final String parentAction, final String cause, final List<Block> blockList, final Object causeObj, final String action) {
        final PrismBlockEvents be = new PrismBlockEvents(plugin); //todo is this necessary?
        for (Block block : blockList) {
            if (causeObj != null) {
                addCache(block, causeObj, action, null);
                addCache(block.getLocation(), causeObj, action, null);
            }
            // don't bother record upper doors.
            if (MaterialTag.DOORS.isTagged(block.getType())
                    && ((Door) block.getState().getBlockData()).getHalf() == Bisected.Half.TOP) {
                continue;
            }

            // Change handling a bit if it's a long block
            final Block sibling = Utilities.getSiblingForDoubleLengthBlock(block);
            if (sibling != null && !block.getType().equals(Material.CHEST)
                    && !block.getType().equals(Material.TRAPPED_CHEST)) {
                block = sibling;
            }

            // log items removed from container
            // note: done before the container so a "rewind" for rollback will
            // work properly
            final Block b2 = block;
            be.forEachItem(block, (i, s) -> RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove",
                    i, i.getAmount(), 0, null, b2.getLocation(), cause)));
            // be.logItemRemoveFromDestroyedContainer( name, block );
            RecordingQueue.addToQueue(ActionFactory.createBlock(parentAction, block, cause));
            // look for relationships
            be.logBlockRelationshipsForBlock(cause, block);

        }
    }

    public static class Cause {

        @NotNull final String action;
        @Nullable final String actor;
        @Nullable final Material blockType;

        Cause(String action, String actor, Material blockType) {
            this.action = action;
            this.actor = actor;
            this.blockType = blockType;
        }

    }

}
