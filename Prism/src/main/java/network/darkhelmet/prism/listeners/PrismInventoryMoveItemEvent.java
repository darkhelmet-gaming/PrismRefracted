package network.darkhelmet.prism.listeners;

import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionFactory;
import network.darkhelmet.prism.actionlibs.RecordingQueue;
import network.darkhelmet.prism.utils.NmsUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class PrismInventoryMoveItemEvent implements Listener {

    /**
     * InventoryMoveEvent.
     *
     * @param event InventoryMoveEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveItem(final InventoryMoveItemEvent event) {
        // Get container
        final InventoryHolder destHolder = event.getDestination().getHolder();
        Location destLoc = null;
        if (destHolder instanceof BlockState) {
            final BlockState eventChest = (BlockState) destHolder;
            destLoc = eventChest.getLocation();
        }
        final InventoryHolder sourHolder = event.getSource().getHolder();
        Location sourLoc = null;
        if (sourHolder instanceof BlockState) {
            final BlockState eventChest = (BlockState) sourHolder;
            sourLoc = eventChest.getLocation();
        }

        if (destLoc == null || sourLoc == null) {
            return;
        }

        String source = event.getSource().getType().name().toLowerCase();
        String destination = event.getDestination().getType().name().toLowerCase();

        ItemStack item = event.getItem();
        int amount = item.getAmount();
        int stackSize = item.getType().getMaxStackSize();
        ItemStack[] contents = destHolder.getInventory().getStorageContents();
        int[] slotsAccept = NmsUtils.getSlotsForFace(event.getSource(), event.getDestination());
        // Fill item stacks first
        for (int i : slotsAccept) {
            ItemStack is = contents[i];

            if (item.isSimilar(is) && NmsUtils.canAcceptPlace(event.getDestination(), item, i)) {
                amount = recordTransfer(stackSize - is.getAmount(), amount, item,
                        sourLoc, destLoc, i, source, destination);
                if (amount <= 0) {
                    break;
                }
            }
        }
        // Fill empty slots
        if (amount > 0) {
            for (int i : slotsAccept) {
                ItemStack is = contents[i];

                if ((is == null || is.getType() == Material.AIR) && NmsUtils.canAcceptPlace(event.getDestination(), item, i)) {
                    amount = recordTransfer(stackSize, amount, item,
                            sourLoc, destLoc, i, source, destination);
                    if (amount <= 0) {
                        break;
                    }
                }
            }
        }
    }


    private int recordTransfer(int size, int amount, ItemStack item, Location sourceLoc, Location destinationLoc,
                               int slotLocation, String sourceInvName, String destinationInvName) {
        int transferred = Math.min(size, amount);
        int newAmount = amount - transferred;
        if (transferred > 0) {
            if (Prism.getIgnore().event("item-insert")) {
                RecordingQueue.addToQueue(ActionFactory.createItemStack("item-insert", item, transferred, slotLocation, null,
                        destinationLoc, sourceInvName));
            }
            if (Prism.getIgnore().event("item-remove")) {
                RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", item, transferred, slotLocation, null,
                        sourceLoc, destinationInvName));
            }
        }
        return newAmount;
    }
}