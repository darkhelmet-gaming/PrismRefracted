package network.darkhelmet.prism.listeners;

import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionFactory;
import network.darkhelmet.prism.actionlibs.RecordingQueue;
import network.darkhelmet.prism.api.actions.Handler;
import network.darkhelmet.prism.utils.NmsUtils;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.ChiseledBookshelfInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class PrismInventoryEvents implements Listener {

    private boolean trackingInsert;
    private boolean trackingRemove;
    private boolean trackingBreaks;
    private static final String INSERT = "item-insert";
    private static final String REMOVE = "item-remove";
    private static final String BREAK = "item-break";
    private final Prism plugin;

    private final boolean supportGetSlot;

    /**
     * Constructor.
     * @param plugin Prism
     */
    public PrismInventoryEvents(Prism plugin) {
        this.plugin = plugin;
        this.trackingInsert = Prism.getIgnore().event(INSERT);
        this.trackingRemove = Prism.getIgnore().event(REMOVE);
        this.trackingBreaks = Prism.getIgnore().event(BREAK);

        boolean supportSlot = true;
        if (Prism.getInstance().getServerMajorVersion() >= 20) {
            try {
                ChiseledBookshelf.class.getMethod("getSlot", Vector.class);
            } catch (NoSuchMethodException e) {
                Prism.warn("Your server doesn't implement the methods we need, Please update to the latest build!");
                supportSlot = false;
            }
        }
        this.supportGetSlot = supportSlot;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTakeLecternBook(final PlayerTakeLecternBookEvent event) {
        if (trackingRemove) {
            RecordingQueue.addToQueue(ActionFactory.createItemStack(REMOVE, event.getBook(), 1,
                    0, null, event.getLectern().getLocation(), event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (notTrackingInsertAndRemove()) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        Player player = event.getPlayer();
        if (Prism.getInstance().getServerMajorVersion() >= 20 && clickedBlock.getType() == Material.CHISELED_BOOKSHELF) {
            if (event.getBlockFace() != ((Directional) clickedBlock.getBlockData()).getFacing()) {
                // The player is not clicking the intractable face
                return;
            }
            ChiseledBookshelf state = (ChiseledBookshelf) clickedBlock.getState();
            // Get the slot the player interacted
            final int slot = supportGetSlot
                    ? state.getSlot(event.getClickedPosition())
                    : getInteractedBookshelfSlot(player, clickedBlock, event.getBlockFace());
            if (slot == -1) {
                return;
            }

            // Process the inventory
            ChiseledBookshelfInventory inventory = state.getInventory();
            ItemStack item = inventory.getItem(slot);

            ItemStack hand = event.getItem();
            if (player.isSneaking() && isActuallyHoldingBook(player)) {
                // Not exchanging the items
                return;
            }
            if (item == null) {
                if (hand != null && Tag.ITEMS_BOOKSHELF_BOOKS.isTagged(hand.getType())) {
                    RecordingQueue.addToQueue(ActionFactory.createItemStack(INSERT, hand, 1,
                            slot, null, clickedBlock.getLocation(), player));
                }
            } else {
                RecordingQueue.addToQueue(ActionFactory.createItemStack(REMOVE, item, 1,
                        slot, null, clickedBlock.getLocation(), player));
            }
        } else if (clickedBlock.getType() == Material.LECTERN) {
            Inventory inventory = ((Lectern) clickedBlock.getState()).getInventory();
            if (inventory.isEmpty()) {
                // Ensure there's no book on it.
                ItemStack hand = event.getItem();
                if (hand != null && (hand.getType() == Material.WRITABLE_BOOK || hand.getType() == Material.WRITTEN_BOOK)) {
                    RecordingQueue.addToQueue(ActionFactory.createItemStack(INSERT, hand, 1,
                            0, null, clickedBlock.getLocation(), player));
                }
            }
        } else if (Tag.FLOWER_POTS.isTagged(clickedBlock.getType())) {
            // Only main hand. Offhand doesn't work here.
            ItemStack hand = player.getInventory().getItemInMainHand();

            // If null, the flower in hand can't fill the pot.
            Material changeTo = Material.matchMaterial("POTTED_" + hand.getType().name());
            if (clickedBlock.getType() == Material.FLOWER_POT) {
                if (changeTo == null) {
                    // The player is not going to fill the pot.
                    return;
                }
            } else {
                if (changeTo != null) {
                    // The player is holding a flower, can't take the flower in the pot.
                    return;
                }
                changeTo = Material.FLOWER_POT;
            }
            RecordingQueue.addToQueue(ActionFactory.createFlowerPotChange(clickedBlock, changeTo, player));
        } else if (Tag.SIGNS.isTagged(clickedBlock.getType())
                || Prism.getInstance().getServerMajorVersion() >= 20 && Tag.ALL_SIGNS.isTagged(clickedBlock.getType())) {
            Sign sign = (Sign) clickedBlock.getState();
            if (player.isSneaking()) {
                return;
            }

            // Only main hand. Offhand doesn't work here.
            ItemStack hand = player.getInventory().getItemInMainHand();
            String handMat = hand.getType().name();
            // Get the player clicked side
            boolean front = true;
            if (Prism.getInstance().getServerMajorVersion() >= 20) {
                BlockData blockData = clickedBlock.getBlockData();
                BlockFace facing;
                if (blockData instanceof Directional) {
                    facing = ((Directional) blockData).getFacing();
                } else {
                    facing = ((Rotatable) blockData).getRotation();
                }
                Vector signCenter = clickedBlock.getLocation().toVector();
                if (Tag.WALL_SIGNS.isTagged(sign.getType())) {
                    switch (facing) {
                        case EAST:
                            signCenter.setX(signCenter.getX() + 0.0625);
                            break;
                        case SOUTH:
                            signCenter.setZ(signCenter.getZ() + 0.0625);
                            break;
                        case WEST:
                            signCenter.setX(signCenter.getX() + 1 - 0.0625);
                            break;
                        case NORTH:
                            signCenter.setZ(signCenter.getZ() + 1 - 0.0625);
                            break;
                        default:
                            throw new AssertionError();
                    }
                } else {
                    signCenter.setX(signCenter.getX() + 0.5);
                    signCenter.setY(signCenter.getY() + 0.5);
                    signCenter.setZ(signCenter.getZ() + 0.5);
                }

                Vector playerDirection = new Vector(player.getLocation().getX() - signCenter.getX(), 0, player.getLocation().getZ() - signCenter.getZ());
                float angle = facing.getDirection().angle(playerDirection);
                front = angle <= 1.5707963267948966;
            }
            // Get side end
            if (handMat.endsWith("_DYE")) {
                if (!Prism.getIgnore().event("sign-dye", event.getPlayer())) {
                    return;
                }
                DyeColor dyeColor;
                try {
                    dyeColor = DyeColor.valueOf(handMat.substring(0, handMat.length() - 4));
                } catch (IllegalArgumentException ignored) {
                    // The player is not holding a dye...
                    return;
                }
                DyeColor signColor;
                if (Prism.getInstance().getServerMajorVersion() >= 20) {
                    signColor = sign.getSide(front ? Side.FRONT : Side.BACK).getColor();
                } else {
                    signColor = sign.getColor();
                }
                if (dyeColor != signColor) {
                    RecordingQueue.addToQueue(
                            ActionFactory.createSignDye(clickedBlock, dyeColor, front, event.getPlayer()));
                }
            } else if (Prism.getInstance().getServerMajorVersion() >= 17 && handMat.endsWith("INK_SAC")) {
                if (!Prism.getIgnore().event("sign-glow", event.getPlayer())) {
                    return;
                }
                boolean makeGlow = hand.getType() == Material.GLOW_INK_SAC;
                boolean signGlow;
                if (Prism.getInstance().getServerMajorVersion() >= 20) {
                    signGlow = sign.getSide(front ? Side.FRONT : Side.BACK).isGlowingText();
                } else {
                    signGlow = sign.isGlowingText();
                }
                if (makeGlow != signGlow) {
                    RecordingQueue.addToQueue(
                            ActionFactory.createSignGlow(clickedBlock, makeGlow, front, event.getPlayer()));
                }
            }
        }
    }

    /**
     * This is used for old 1.20(.1) builds which has not been added API for getting slot.
     * May not accurate if the player is changing the direction quickly while interacting,
     * because of the latency.
     *
     * @return slot index or -1
     */
    private int getInteractedBookshelfSlot(Player player, Block clickedBlock, BlockFace blockFace) {
        Vector eye = player.getEyeLocation().toVector();
        Vector direction = player.getEyeLocation().getDirection();
        Vector block = clickedBlock.getLocation().toVector();
        if (blockFace == BlockFace.EAST || blockFace == BlockFace.SOUTH) {
            block.add(blockFace.getDirection());
        }
        double distance;
        switch (blockFace) {
            case EAST:
            case WEST:
                distance = (block.getX() - eye.getX()) / direction.getX();
                break;
            case NORTH:
            case SOUTH:
                distance = (block.getZ() - eye.getZ()) / direction.getZ();
                break;
            default:
                // Not possible for inserting/removing
                return -1;
        }
        Vector clickedLoc = eye.add(direction.normalize().multiply(distance));
        double pos = -1;
        switch (blockFace) {
            case EAST:
                pos = Math.abs(clickedLoc.getZ() % 1);
                if (clickedLoc.getZ() > 0)
                    pos = 1 - pos;
                break;
            case WEST:
                pos = 1 - Math.abs(clickedLoc.getZ() % 1);
                if (clickedLoc.getZ() > 0)
                    pos = 1 - pos;
                break;
            case NORTH:
                pos = Math.abs(clickedLoc.getX() % 1);
                if (clickedLoc.getX() > 0)
                    pos = 1 - pos;
                break;
            case SOUTH:
                pos = 1 - Math.abs(clickedLoc.getX() % 1);
                if (clickedLoc.getX() > 0)
                    pos = 1 - pos;
                break;
        }
        int slot;
        if (pos < 0.375F)
            slot = 0;
        else if (pos < 0.6875F)
            slot = 1;
        else
            slot = 2;

        pos = clickedLoc.getY() % 1;
        if (pos < 0.5F)
            slot += 3;
        return slot;
    }

    private boolean isActuallyHoldingBook(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (Tag.ITEMS_BOOKSHELF_BOOKS.isTagged(item.getType())) {
            return true;
        }
        item = player.getInventory().getItemInOffHand();
        return Tag.ITEMS_BOOKSHELF_BOOKS.isTagged(item.getType());
    }

    /**
     * InventoryPickupItemEvent.
     * @param event InventoryPickupItemEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryPickupItem(final InventoryPickupItemEvent event) {

        if (!plugin.getConfig().getBoolean("prism.track-hopper-item-events")) {
            return;
        }

        if (!Prism.getIgnore().event("item-pickup")) {
            return;
        }

        // If hopper
        if (event.getInventory().getType().equals(InventoryType.HOPPER)) {
            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-pickup", event.getItem().getItemStack(),
                    event.getItem().getItemStack().getAmount(), -1, null, event.getItem().getLocation(), "hopper"));
        }
    }

    /**
     * Handle inventory transfers.
     *
     * @param event InventoryDragEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (notTrackingInsertAndRemove()) {
            return;
        }
        // Get container
        final InventoryHolder ih = event.getInventory().getHolder();

        // Store some info
        final Player player = (Player) event.getWhoClicked();

        // Ignore all item move events where players modify their own inventory
        if (ih instanceof Player) {
            Player other = (Player) event.getInventory().getHolder();

            if (event.getWhoClicked().equals(other)) {
                return;
            }
        }

        Location containerLoc = event.getInventory().getLocation();

        if (containerLoc == null) {
            return;
        }

        final Map<Integer, ItemStack> newItems = event.getNewItems();
        for (final Entry<Integer, ItemStack> entry : newItems.entrySet()) {

            int rawSlot = entry.getKey();

            // Top inventory
            if (rawSlot < event.getInventory().getSize()) {
                ItemStack stack = event.getView().getItem(rawSlot);
                int slotViewAmount = (stack == null)
                        ? 0 : stack.getAmount();
                int amount = entry.getValue().getAmount() - slotViewAmount;

                RecordingQueue.addToQueue(ActionFactory.createItemStack(INSERT, entry.getValue(), amount,
                        rawSlot, null, containerLoc, player));
            }
        }
    }

    /**
     * EnchantItemEvent.
     *
     * @param event EnchantItemEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(final EnchantItemEvent event) {
        if (!Prism.getIgnore().event("enchant-item", event.getEnchanter())) {
            return;
        }
        final Player player = event.getEnchanter();
        RecordingQueue.addToQueue(ActionFactory.createItemStack("enchant-item", event.getItem(),
                event.getEnchantsToAdd(), event.getEnchantBlock().getLocation(), player));
    }

    /**
     * Handle Crafting.
     * @param prepareItemCraftEvent event.
     */
    @EventHandler(priority = EventPriority.MONITOR,ignoreCancelled = true)
    public void onPrepareCraftItem(PrepareItemCraftEvent prepareItemCraftEvent) {
        if (Prism.getIgnore().event("craft-item")) {
            return;
        }
        List<HumanEntity> recordable = prepareItemCraftEvent.getViewers().stream().filter(humanEntity -> {
            if (humanEntity instanceof Player) {
                return Prism.getIgnore().event("craft-item", (Player) humanEntity);
            }
            return false;
        }).collect(Collectors.toList());
        if (recordable.size() > 0) {
            //todo
            Prism.debug("PrepareCraftEvent: " + prepareItemCraftEvent.toString());
        }

    }

    /**
     * CraftItemEvent.
     *
     * @param event CraftItemEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(final CraftItemEvent event) {
        final Player player = (Player) event.getWhoClicked();
        if (!Prism.getIgnore().event("craft-item", player)) {
            return;
        }
        final ItemStack item = event.getRecipe().getResult();
        RecordingQueue.addToQueue(
                ActionFactory.createItemStack("craft-item", item, 1, -1, null, player.getLocation(), player));
    }

    /**
     * Handle inventory transfers.
     *
     * @param event InventoryClickEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        int slot = event.getRawSlot(); //this is the unique slot number for the view.
        // Specifically slot -999, or out of the window
        if (slot < 0) {
            return;
        }

        // Store some info
        final Player player = (Player) event.getWhoClicked();
        // Check if Smithing Inventory
        if (Prism.getInstance().getServerMajorVersion() >= 20 && event.getInventory() instanceof SmithingInventory) {
            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                if (!Prism.getIgnore().event("upgrade-gear", player)) {
                    return;
                }
                final ItemStack item = event.getCurrentItem();
                if (item.getType() == Material.AIR) {
                    // Not upgraded
                    return;
                }
                if (event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                    // Forbidden action
                    return;
                }
                RecordingQueue.addToQueue(
                        ActionFactory.createItemStack("upgrade-gear", item, 1, -1, null, player.getLocation(), player));
            }
            return;
        }

        Location containerLoc = event.getInventory().getLocation(); //this is the top Inventory
        // Virtual inventory or something (enderchest?)
        if (containerLoc == null) {
            return;
        }

        if (notTrackingInsertAndRemove()) {
            return;
        }

        // Ignore all item move events where players modify their own inventory
        if (event.getInventory().getHolder() instanceof Player) {
            Player other = (Player) event.getInventory().getHolder();

            if (other.equals(player)) {
                return;
            }
        }
        boolean isTopInv = slot < event.getInventory().getSize();

        ItemStack heldItem = event.getCursor();
        ItemStack slotItem = event.getCurrentItem();

        // This happens when opening someone else's inventory, so don't bother tracking it
        if (slotItem == null) {
            return;
        }
        Prism.debug("HELD:" + ((heldItem != null) ? heldItem.toString() : "NULL"));
        Prism.debug("SLOT:" +  slotItem.toString());

        switch (event.getClick()) {
            // IGNORE BOTTOM
            case LEFT:
                if (isTopInv) {
                    if (heldItem == null || heldItem.getType() == Material.AIR) {
                        if (slotItem.getType() != Material.AIR) {
                            RecordingQueue.addToQueue(ActionFactory.createItemStack(REMOVE, slotItem,
                                    slotItem.getAmount(), slot, null, containerLoc, player));
                            Prism.debug("ACTION: " + event.getAction().name());
                        }
                    } else {
                        int amount = 0;
                        int maxStack = heldItem.getMaxStackSize();
                        if (slotItem.getType() == Material.AIR && heldItem.getAmount() <= maxStack) {
                            amount = heldItem.getAmount();
                        }
                        if (slotItem.getType().equals(heldItem.getType())) {
                            int slotQty = slotItem.getAmount();
                            amount = Math.min(maxStack - slotQty,heldItem.getAmount());
                        }
                        if (amount > 0) {
                            RecordingQueue.addToQueue(ActionFactory.createItemStack(INSERT, heldItem, amount, slot,
                                    null, containerLoc, player));
                            Prism.debug("ACTION: " + event.getAction().name());

                        }
                        if (slotItem.getType() != Material.AIR && !slotItem.getType().equals(heldItem.getType())) {
                            // its a switch.
                            RecordingQueue.addToQueue(ActionFactory.createItemStack(INSERT,heldItem,
                                    heldItem.getAmount(),slot,null,containerLoc,player));
                            Prism.debug("ACTION: " + event.getAction().name());
                            RecordingQueue.addToQueue(ActionFactory.createItemStack(REMOVE,slotItem,
                                    slotItem.getAmount(),slot,null,containerLoc,player));
                        }
                    }
                }
                break;

            case RIGHT:
                if (isTopInv) {
                    if (heldItem == null || heldItem.getType() == Material.AIR) {
                        if (slotItem.getType() != Material.AIR) {
                            RecordingQueue.addToQueue(ActionFactory.createItemStack(REMOVE, slotItem,
                                    (slotItem.getAmount() + 1) / 2, slot, null, containerLoc, player));
                            Prism.debug("ACTION: " + event.getAction().name());

                        }
                    } else {
                        if ((slotItem.getType() == Material.AIR || slotItem.equals(heldItem))
                                && slotItem.getAmount() < slotItem.getType().getMaxStackSize()) {
                            RecordingQueue.addToQueue(ActionFactory.createItemStack(INSERT, slotItem, 1, slot, null,
                                    containerLoc, player));
                            Prism.debug("ACTION: " + event.getAction().name());

                        }
                    }
                }
                break;

            case NUMBER_KEY:
                if (isTopInv) {
                    ItemStack swapItem = player.getInventory().getItem(event.getHotbarButton());

                    if (slotItem.getType() != Material.AIR) {
                        RecordingQueue.addToQueue(ActionFactory.createItemStack(REMOVE, slotItem, slotItem.getAmount(),
                                slot, null, containerLoc, player));
                        Prism.debug("ACTION: " + event.getAction().name());

                    }

                    if (swapItem != null && swapItem.getType() != Material.AIR) {
                        RecordingQueue.addToQueue(ActionFactory.createItemStack(INSERT, swapItem, swapItem.getAmount(),
                                slot, null, containerLoc, player));
                        Prism.debug("ACTION: " + event.getAction().name());

                    }
                }
                break;

            // HALF 'N HALF
            case DOUBLE_CLICK: {
                int amount = (heldItem == null) ? 0 :
                        heldItem.getType().getMaxStackSize() - heldItem.getAmount();

                ItemStack[] contents = event.getInventory().getStorageContents();
                int length = contents.length;

                for (int i = 0; i < length; ++i) {
                    ItemStack is = contents[i];

                    int size = 0;
                    if (is != null && (is.getType() != Material.AIR || is.equals(heldItem))) {
                        size += is.getAmount();
                    }
                    amount = recordDeductTransfer(REMOVE,size,amount,heldItem,containerLoc,i,player,event);
                    if (amount <= 0) {
                        break;
                    }
                }
                break;
            }

            // CROSS INVENTORY EVENTS
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                if (isTopInv) {
                    if (slotItem.getType() != Material.AIR) {
                        int stackSize = slotItem.getType().getMaxStackSize();
                        int remaining = slotItem.getAmount();

                        for (ItemStack is : event.getView().getBottomInventory().getStorageContents()) {
                            //noinspection ConstantConditions  Until intellij sorts it checks
                            if (is == null || is.getType() == Material.AIR) {
                                remaining -= stackSize;
                            } else if (is.isSimilar(slotItem)) {
                                remaining -= (stackSize - Math.min(is.getAmount(), stackSize));
                            }

                            if (remaining <= 0) {
                                remaining = 0;
                                break;
                            }
                        }

                        RecordingQueue.addToQueue(ActionFactory.createItemStack(REMOVE, slotItem,
                                slotItem.getAmount() - remaining, slot, null, containerLoc, player));
                        Prism.debug("ACTION: " + event.getAction().name());

                    }
                } else {
                    int stackSize = slotItem.getType().getMaxStackSize();
                    int amount = slotItem.getAmount();

                    ItemStack[] contents = event.getInventory().getStorageContents();
                    int length = contents.length;

                    // Fill item stacks first
                    for (int i = 0; i < length; ++i) {
                        ItemStack is = contents[i];

                        if (slotItem.isSimilar(is) && NmsUtils.canAcceptPlaceQuick(event.getView(), slotItem, i)) {
                            amount = recordDeductTransfer(INSERT,stackSize - is.getAmount(),amount,slotItem,
                                    containerLoc,i,player,event);
                            if (amount <= 0) {
                                break;
                            }
                        }
                    }

                    // Fill empty slots
                    if (amount > 0) {
                        for (int i = 0; i < length; ++i) {
                            ItemStack is = contents[i];

                            if ((is == null || is.getType() == Material.AIR) && NmsUtils.canAcceptPlaceQuick(event.getView(), slotItem, i)) {
                                amount = recordDeductTransfer(INSERT,stackSize,amount,slotItem,
                                        containerLoc,i,player,event);
                                if (amount <= 0) {
                                    break;
                                }
                            }
                        }
                    }
                }
                break;

            // DROPS
            case DROP:
                if (slotItem.getType() != Material.AIR && slotItem.getAmount() > 0) {
                    RecordingQueue.addToQueue(
                            ActionFactory.createItemStack(REMOVE, slotItem, 1, slot, null, containerLoc, player));
                    Prism.debug("ACTION: " + event.getAction().name());

                }
                break;

            case CONTROL_DROP:
                if (slotItem.getType() != Material.AIR && slotItem.getAmount() > 0) {
                    RecordingQueue.addToQueue(ActionFactory.createItemStack(REMOVE, slotItem, slotItem.getAmount(),
                            slot, null, containerLoc, player));
                    Prism.debug("ACTION: " + event.getAction().name());

                }
                break;

            case WINDOW_BORDER_LEFT:
                // Drop stack on cursor
            case WINDOW_BORDER_RIGHT:
                // Drop 1 on cursor

            default:
                // What the hell did you do
        }
    }

    private int recordDeductTransfer(String act, int size, int amount, ItemStack heldItem, Location containerLoc,
                                     int slotLocation, Player player, InventoryClickEvent event) {
        int transferred = Math.min(size, amount);
        int newAmount = amount - transferred;
        if (transferred > 0) {
            RecordingQueue.addToQueue(ActionFactory.createItemStack(act, heldItem, transferred, slotLocation, null,
                    containerLoc, player));
            Prism.debug("ACTION: " + event.getAction().name());

        }
        return newAmount;
    }

    /**
     * Tracks item breakage. Cant be rolled back.  At this point the item damage is not 0 however it will be set 0 after
     * event completes - Reported item durability will be the durability before the event.
     * @param event PlayerItemBreakEvent.
     */
    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    public void onItemBreak(PlayerItemBreakEvent event) {
        if (!trackingBreaks) {
            return;
        }
        ItemStack item = event.getBrokenItem();
        Handler h = ActionFactory.createItemStack(BREAK,item,null, event.getPlayer().getLocation(),event.getPlayer());
        RecordingQueue.addToQueue(h);
    }

    private boolean notTrackingInsertAndRemove() {
        this.trackingInsert = Prism.getIgnore().event(INSERT);
        this.trackingRemove = Prism.getIgnore().event(REMOVE);
        this.trackingBreaks = Prism.getIgnore().event(BREAK);

        return !trackingInsert && !trackingRemove;
    }

}
