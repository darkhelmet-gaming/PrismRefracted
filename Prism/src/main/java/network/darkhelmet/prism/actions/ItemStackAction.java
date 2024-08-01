package network.darkhelmet.prism.actions;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actions.data.ItemStackActionData;
import network.darkhelmet.prism.api.ChangeResult;
import network.darkhelmet.prism.api.ChangeResultType;
import network.darkhelmet.prism.api.PrismParameters;
import network.darkhelmet.prism.api.actions.PrismProcessType;
import network.darkhelmet.prism.api.objects.MaterialState;
import network.darkhelmet.prism.appliers.ChangeResultImpl;
import network.darkhelmet.prism.utils.InventoryUtils;
import network.darkhelmet.prism.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Jukebox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ItemStackAction extends GenericAction {

    private static final Cache<Long, Item> dropCache = CacheBuilder
            .newBuilder()
            .softValues()
            .expireAfterWrite(3, TimeUnit.DAYS) // LONGGGGGGGGGGGGGGGGGGGGGG
            .build();

    public static void addCache(Long dataId, Item item) {
        dropCache.put(dataId, item);
    }

    public static boolean removeFromCache(Long dataId) {
        Item item = dropCache.getIfPresent(dataId);
        if (item != null) {
            dropCache.invalidate(dataId);
            item.remove();
            return true;
        }
        return false;
    }

    protected ItemStack item;
    private ItemStackActionData actionData;
    private Item dropEntity;

    /**
     * Holds durability if no actionData yet exists.
     */
    private short tempDurability = -1;

    public Item getItemEntity() {
        return dropEntity;
    }

    public void setItemEntity(Item dropEntity) {
        this.dropEntity = dropEntity;
    }

    @Override
    public boolean hasExtraData() {
        return actionData != null;
    }

    @Override
    public short getDurability() {
        if (actionData != null) {
            return actionData.durability;
        }
        return 0;
    }

    @Override
    public void setDurability(short durability) {
        if (actionData == null) {
            tempDurability = durability;
        } else {
            actionData.durability = durability;
        }
    }

    /**
     * Set the item.
     * @param item ItemStack
     * @param quantity int
     * @param enchantments Map of enchants.
     */
    public void setItem(ItemStack item, int quantity, Map<Enchantment, Integer> enchantments) {
        Map<Enchantment, Integer> tempEnchantments = new HashMap<>();
        if (enchantments != null) {
            tempEnchantments = enchantments;
        }

        if (item == null || item.getAmount() <= 0) {
            this.setCanceled(true);
            return;
        }

        this.item = item;
        if (enchantments == null) {
            tempEnchantments = item.getEnchantments();
        }

        // Set basics
        actionData = ItemStackActionData.createData(item, quantity,
                tempDurability >= 0 ? tempDurability : (short) ItemUtils.getItemDamage(item), tempEnchantments);
        setMaterial(item.getType());
    }

    public void setSlot(String slot) {
        actionData.slot = slot;
    }

    @Override
    public String serialize() {
        return gson().toJson(actionData);
    }

    @Override
    public void deserialize(String data) {
        deserialize(null, data);
    }

    public void deserialize(MaterialState materialState, String data) {
        if (data == null || !data.startsWith("{")) {
            return;
        }
        actionData = gson().fromJson(data, ItemStackActionData.class);

        // Old extra data doesn't include the material so
        // this bridges the gap between old and new
        if (materialState != null && actionData.material == null) {
            actionData.material = materialState.material;
        }

        item = actionData.toItem();
    }

    public ItemStackActionData getActionData() {
        return this.actionData;
    }

    /**
     * ItemStack.
     * @return ItemStack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Nice name.
     * @return String
     */
    @Override
    public String getNiceName() {
        String name = "";
        if (item != null) {
            final String fullItemName = ItemUtils.getItemFullNiceName(item);
            name = actionData.amt + " " + fullItemName;
        }
        return name;
    }

    @Override
    public ChangeResult applyRollback(Player player, PrismParameters parameters, boolean isPreview) {
        return placeItems(player, parameters, isPreview);
    }

    @Override
    public ChangeResult applyRestore(Player player, PrismParameters parameters, boolean isPreview) {
        return placeItems(player, parameters, isPreview);
    }

    protected ChangeResult placeItems(Player player, PrismParameters parameters, boolean isPreview) {

        if (actionData == null) {
            return new ChangeResultImpl(ChangeResultType.SKIPPED, null);
        }

        ChangeResultType result = ChangeResultType.SKIPPED;

        if (isPreview) {
            return new ChangeResultImpl(ChangeResultType.PLANNED, null);
        }

        if (Prism.config.getBoolean("prism.appliers.allow-rollback-items-removed-from-container")) {

            final Block block = getWorld().getBlockAt(getLoc());
            Inventory inventory = null;

            // Entity death drops. Just remove the drops.
            if (getUuid() == null || getActionType().getName().equals("item-drop")) {
                removeItemEntity();
                return new ChangeResultImpl(ChangeResultType.APPLIED, null);
            }

            // Item drop/pickup from player inventories
            if (getActionType().getName().equals("item-drop") || getActionType().getName().equals("item-pickup")) {

                // Is player online?
                final Player onlinePlayer = Bukkit.getServer().getPlayer(getUuid());
                if (onlinePlayer != null) {
                    inventory = onlinePlayer.getInventory();
                } else {
                    // Skip if the player isn't online
                    Prism.debug("Skipping inventory process because player is offline");
                    return new ChangeResultImpl(ChangeResultType.SKIPPED, null);
                }
            } else {
                if (block.getType().equals(Material.JUKEBOX)) {
                    final Jukebox jukebox = (Jukebox) block.getState();
                    jukebox.setPlaying(item.getType());
                    jukebox.update();
                } else if (block.getState() instanceof InventoryHolder) {
                    final InventoryHolder ih = (InventoryHolder) block.getState();
                    inventory = ih.getInventory();
                } else {
                    String slot = getActionData().slot.toUpperCase(Locale.ENGLISH);
                    EquipmentSlot eSlot = null;
                    // Prism.log("Slot found: " + slot);
                    try {
                        eSlot = EquipmentSlot.valueOf(slot);
                    } catch (IllegalArgumentException ignored) {
                        //ignored
                    }
                    // Prism.log("   eSlot: " + eSlot);

                    BlockFace fSlot = null;
                    try {
                        fSlot = BlockFace.valueOf(slot);
                    } catch (IllegalArgumentException ignored) {
                        //ignored
                    }
                    // Prism.log("   fSlot: " + fSlot);

                    Entity[] foundEntities = block.getChunk().getEntities();

                    for (Entity e : foundEntities) {
                        // Get the block location for better comparisons
                        Location loc = e.getLocation();
                        loc.setX(loc.getBlockX());
                        loc.setY(loc.getBlockY());
                        loc.setZ(loc.getBlockZ());

                        Prism.debug(block.getLocation());
                        Prism.debug(loc);

                        if (!block.getWorld().equals(e.getWorld())) {
                            continue;
                        }

                        if (block.getLocation().distanceSquared(loc) < 0.25) {
                            if (e instanceof ItemFrame) {
                                final ItemFrame frame = (ItemFrame) e;

                                // if we have a pseudo-slot try to use that
                                if (fSlot != null && fSlot != frame.getAttachedFace()) {
                                    // Prism.log("Skipping frame: " + frame.getFacing());
                                    continue;
                                }
                                // Prism.log("Using frame: " + frame.getFacing());

                                if ((getActionType().getName().equals("item-remove")
                                        && parameters.getProcessType().equals(PrismProcessType.ROLLBACK))
                                        || (getActionType().getName().equals("item-insert")
                                        && parameters.getProcessType().equals(PrismProcessType.RESTORE))) {
                                    if (frame.getItem().getType() == Material.AIR) {
                                        frame.setItem(item);
                                        result = ChangeResultType.APPLIED;
                                        break;
                                    }
                                } else if (frame.getItem().getType() != Material.AIR) {
                                    frame.setItem(null);
                                    result = ChangeResultType.APPLIED;
                                    break;
                                }
                            } else if (e instanceof ArmorStand) {
                                final LivingEntity stand = (ArmorStand) e;

                                EquipmentSlot actualSlot = eSlot;

                                if (actualSlot == null) {
                                    actualSlot = InventoryUtils.getTargetArmorSlot(item.getType());
                                }

                                ItemStack atPoint = InventoryUtils.getEquipment(stand.getEquipment(), eSlot);

                                if ((getActionType().getName().equals("item-remove")
                                        && parameters.getProcessType().equals(PrismProcessType.ROLLBACK))
                                        || (getActionType().getName().equals("item-insert")
                                        && parameters.getProcessType().equals(PrismProcessType.RESTORE))) {
                                    if (atPoint.getType() == Material.AIR) {
                                        InventoryUtils.setEquipment(stand.getEquipment(), actualSlot, item);
                                        result = ChangeResultType.APPLIED;
                                        break;
                                    }
                                } else if (atPoint.getType() != Material.AIR) {
                                    InventoryUtils.setEquipment(stand.getEquipment(), actualSlot, null);
                                    result = ChangeResultType.APPLIED;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (inventory != null) {

                final PrismProcessType pt = parameters.getProcessType();
                final String n = getActionType().getName();

                int iSlot = -1;

                try {
                    iSlot = Integer.parseInt(getActionData().slot);
                } catch (IllegalArgumentException ignored) {
                    //ignored
                }

                // Rolling back a:remove or a:drop should place the item into
                // the inventory
                // Restoring a:insert or a:pickup should place the item into the
                // inventory
                if ((pt.equals(PrismProcessType.ROLLBACK) && (n.equals("item-remove") || n.equals("item-drop")))
                        || (pt.equals(PrismProcessType.RESTORE)
                        && (n.equals("item-insert") || n.equals("item-pickup")))) {

                    boolean added = false;

                    // We'll attempt to put it back in the same slot
                    if (iSlot >= 0) {
                        // Ensure slot exists in this inventory
                        // I'm not sure why this happens but sometimes
                        // a slot larger than the contents size is recorded
                        // and triggers ArrayIndexOutOfBounds
                        // https://snowy-evening.com/botsko/prism/450/
                        if (iSlot < inventory.getSize()) {
                            final ItemStack currentSlotItem = inventory.getItem(iSlot);
                            int amount = 0;
                            ItemStack item = getItem().clone();
                            int max = item.getType().getMaxStackSize();

                            if (currentSlotItem == null || currentSlotItem.getType() == Material.AIR) {
                                amount = item.getAmount();
                            } else if (currentSlotItem.isSimilar(item)) {
                                amount = Math.min(currentSlotItem.getAmount() + item.getAmount(), max);
                            }

                            if (amount > 0) {
                                result = ChangeResultType.APPLIED;
                                item.setAmount(amount);
                                added = true;
                                inventory.setItem(iSlot, item);
                            }
                        }
                        if (added && (n.equals("item-insert") || n.equals("item-remove"))) {
                            final Player onlinePlayer = Bukkit.getServer().getPlayer(getUuid());
                            if (onlinePlayer != null) {
                                Inventory playerInventory = onlinePlayer.getInventory();
                                final HashMap<Integer, ItemStack> leftovers = InventoryUtils.removeItemFromInventory(playerInventory,
                                        getItem());
                                if (leftovers.size() > 0) {
                                    Prism.debug("There are leftovers when roll-backing player inventory, action " + n);
                                }
                            }
                        }
                    }
                    // If that failed we'll attempt to put it anywhere
                    if (!added) {
                        // TODO: Skip is actually "partially applied"
                        final HashMap<Integer, ItemStack> leftovers = InventoryUtils.addItemToInventory(inventory,
                                getItem());
                        if (leftovers.size() > 0) {
                            Prism.debug("Skipping adding items because there are leftovers");
                            result = ChangeResultType.SKIPPED;
                        } else {
                            result = ChangeResultType.APPLIED;
                            added = true;
                        }
                    }

                    // Item was added to the inv, we need to remove the entity
                    if (added && (n.equals("item-drop") || n.equals("item-pickup"))) {
                        removeItemEntity();
                    }
                }

                // Rolling back a:insert or a:pickup should remove the item from
                // the inventory
                // Restoring a:remove or a:drop should remove the item from the
                // inventory
                if ((pt.equals(PrismProcessType.ROLLBACK) && (n.equals("item-insert") || n.equals("item-pickup")))
                        || (pt.equals(PrismProcessType.RESTORE)
                        && (n.equals("item-remove") || n.equals("item-drop")))) {

                    // does inventory have item?
                    boolean removed = false;

                    // We'll attempt to take it from the same slot
                    if (iSlot >= 0) {

                        if (iSlot >= inventory.getContents().length) {
                            inventory.removeItem(getItem());
                        } else {
                            final ItemStack currentSlotItem = inventory.getItem(iSlot);
                            ItemStack item = getItem().clone();

                            if (item.isSimilar(currentSlotItem)) {
                                int amount = 0;
                                if (currentSlotItem != null) {
                                    amount = Math.max(currentSlotItem.getAmount() - item.getAmount(), 0);
                                }
                                item.setAmount(amount);
                                result = ChangeResultType.APPLIED;
                                removed = true;
                                inventory.setItem(iSlot, amount > 0 ? item : null);
                            }
                        }
                        if (removed && (n.equals("item-insert") || n.equals("item-remove"))) {
                            final Player onlinePlayer = Bukkit.getServer().getPlayer(getUuid());
                            if (onlinePlayer != null) {
                                Inventory playerInventory = onlinePlayer.getInventory();
                                final HashMap<Integer, ItemStack> leftovers = InventoryUtils.addItemToInventory(playerInventory,
                                        getItem());
                                if (leftovers.size() > 0) {
                                    Prism.debug("There are leftovers when roll-backing player inventory, action " + n);
                                }
                            }
                        }
                    }

                    // If that failed we'll attempt to remove it anywhere
                    if (!removed) {
                        // TODO: Skip is actually "partially applied"
                        final HashMap<Integer, ItemStack> leftovers = InventoryUtils.removeItemFromInventory(inventory,
                                getItem());
                        if (leftovers.size() > 0) {
                            Prism.debug("Skipping removing items because there are leftovers");
                            result = ChangeResultType.SKIPPED;
                        } else {
                            result = ChangeResultType.APPLIED;
                            removed = true;
                        }
                    }

                    if (removed && (n.equals("item-drop") || n.equals("item-pickup"))) {
                        ItemUtils.dropItem(getLoc(), getItem());
                    }
                }
            }
        }
        return new ChangeResultImpl(result, null);
    }

    private void removeItemEntity() {
        if (!removeFromCache(getId())) {
            // Not cached, search nearby for it.
            for (final Entity entity : getLoc().getWorld().getNearbyEntities(getLoc(), 10, 10, 10)) {
                if (entity instanceof Item) {
                    final ItemStack stack = ((Item) entity).getItemStack();
                    if (stack.isSimilar(getItem())) {
                        // Remove the event's number of items from
                        // the stack
                        stack.setAmount(stack.getAmount() - getItem().getAmount());
                        if (stack.getAmount() == 0) {
                            entity.remove();
                        }
                        break;
                    }
                }
            }
        }
    }
}
