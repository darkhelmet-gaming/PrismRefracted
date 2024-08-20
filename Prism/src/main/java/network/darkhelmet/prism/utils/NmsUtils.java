package network.darkhelmet.prism.utils;

import network.darkhelmet.prism.Prism;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class NmsUtils {

    private static final String NMS_PACKAGE_16 = getMc16NmsPackage();

    private static final EnumMap<InventoryType, WrappedSlot[]> itemInventoryAccepts = new EnumMap<>(InventoryType.class);
    private static final EnumMap<InventoryType, Method> inventoryCanPlaceItem = new EnumMap<>(InventoryType.class);
    private static final EnumMap<InventoryType, Method> getSlotsForFace = new EnumMap<>(InventoryType.class);

    private NmsUtils() {
        // private
    }

    /**
     * @param inventoryView
     * @param itemStack
     * @param index
     * @return If the inventory accepts the itemstack added by players by shift-click.
     */
    public static boolean canAcceptPlaceQuick(InventoryView inventoryView, ItemStack itemStack, int index) {
        if (!itemInventoryAccepts.containsKey(inventoryView.getType())) {
            itemInventoryAccepts.put(inventoryView.getType(), cacheInventoryAccepts(inventoryView));
        }
        WrappedSlot[] slots = itemInventoryAccepts.get(inventoryView.getType());
        if (slots == null) {
            // Failed when cached, don't check.
            return true;
        }
        return slots[index].acceptsPlace(itemStack);
    }

    /**
     * @param inventory
     * @param itemStack
     * @param index
     * @return If the inventory accepts the itemstack placement.
     */
    public static boolean canAcceptPlace(Inventory inventory, ItemStack itemStack, int index) {
        InventoryType type = inventory.getType();
        if (!inventoryCanPlaceItem.containsKey(type)) {
            inventoryCanPlaceItem.put(type, cacheInventoryCanPlace(inventory));
        }
        Method canPlaceItem = inventoryCanPlaceItem.get(type);
        if (canPlaceItem == null) {
            return true;
        }
        try {
            return (boolean) canPlaceItem.invoke(getNmsHandle(inventory), index, getNmsHandle(itemStack));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param hopper
     * @param container
     * @return The slots that container accepts placement from hopper
     */
    public static int[] getSlotsForFace(Inventory hopper, Inventory container) {
        InventoryType type = container.getType();
        if (!getSlotsForFace.containsKey(type)) {
            getSlotsForFace.put(type, cacheGetSlotsForFace(container));
        }
        Method slotsForFace = getSlotsForFace.get(type);
        if (slotsForFace == null) {
            // We failed. Just return all slots.
            int[] fail = new int[container.getContents().length];
            for (int i = 0; i < container.getContents().length; i++) {
                fail[i] = i;
            }
            return fail;
        }

        BlockFace facing = ((Container) container.getHolder()).getBlock().getFace(((Hopper) hopper.getHolder()).getBlock());
        Object nmsDirection;
        Class<?> nmsFacing = getNmsDirection();
        if (nmsFacing == null) {
            int[] fail = new int[container.getContents().length];
            for (int i = 0; i < container.getContents().length; i++) {
                fail[i] = i;
            }
            return fail;
        }
        switch (facing) {
            case DOWN:
                nmsDirection = nmsFacing.getEnumConstants()[0];
                break;
            case UP:
                nmsDirection = nmsFacing.getEnumConstants()[1];
                break;
            case NORTH:
                nmsDirection = nmsFacing.getEnumConstants()[2];
                break;
            case SOUTH:
                nmsDirection = nmsFacing.getEnumConstants()[3];
                break;
            case WEST:
                nmsDirection = nmsFacing.getEnumConstants()[4];
                break;
            case EAST:
                nmsDirection = nmsFacing.getEnumConstants()[5];
                break;
            default:
                throw new AssertionError();
        }
        try {
            return (int[]) slotsForFace.invoke(getNmsHandle(container), nmsDirection);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static WrappedSlot[] cacheInventoryAccepts(InventoryView inventoryView) {
        Object containerMenu = getNmsHandle(inventoryView);
        if (containerMenu == null) {
            // Failed
            return null;
        }
        List<Object> slots;
        try {
            Class<?> abstractContainer = containerMenu.getClass();
            while (true) {
                Class<?> temp = abstractContainer.getSuperclass();
                if (temp == Object.class) {
                    break;
                }
                abstractContainer = temp;
            }
            Field fieldSlots = Arrays.stream(abstractContainer.getDeclaredFields())
                    .filter(it -> List.class.isAssignableFrom(it.getType())).collect(Collectors.toList()).get(1);
            fieldSlots.setAccessible(true);
            //noinspection unchecked
            slots = (List<Object>) fieldSlots.get(containerMenu);
        } catch (IllegalAccessException e) {
            Prism.getInstance().getLogger().log(Level.WARNING, "Cannot get NMS container menu slots.", e);
            return null;
        }
        Object nmsItemStack = getNmsHandle(new ItemStack(Material.AIR));
        if (nmsItemStack == null) {
            return null;
        }
        List<WrappedSlot> res = new ArrayList<>();
        try {
            for (Object slot : slots) {
                Class<?> base = slot.getClass();
                while (true) {
                    Class<?> temp = base.getSuperclass();
                    if (temp == Object.class) {
                        break;
                    }
                    base = temp;
                }

                Field fieldSlotIndex = Arrays.stream(base.getDeclaredFields())
                        .filter(it -> it.getType() == int.class).findFirst().get();
                fieldSlotIndex.setAccessible(true);
                int slotIndex = (int) fieldSlotIndex.get(slot);

                Class<?> canPlaceClass = slot.getClass();
                Optional<Method> optional;
                while (!(optional = Arrays.stream(canPlaceClass.getDeclaredMethods())
                        .filter(it -> it.getReturnType() == boolean.class && it.getParameterCount() == 1
                                && it.getParameterTypes()[0] == nmsItemStack.getClass()
                                && !it.getName().endsWith("_")) // 1.16.5 has two methods in SlotFurnaceFuel, filter it
                        .findFirst()).isPresent()) {
                    // The method may exist in super class.
                    canPlaceClass = canPlaceClass.getSuperclass();
                    if (canPlaceClass == Object.class) {
                        // Break, and throw NoSuchElementException later.
                        break;
                    }
                }
                Method method = optional.get();
                method.setAccessible(true);
                res.add(new WrappedSlot(slotIndex, method, slot));
            }
        } catch (NoSuchElementException | IllegalAccessException e) {
            Prism.getInstance().getLogger().log(Level.WARNING, "Cannot wrap NMS Slot handles.", e);
            return null;
        }
        return res.toArray(new WrappedSlot[0]);
    }

    private static Method cacheInventoryCanPlace(Inventory inventory) {
        Object container = getNmsHandle(inventory);
        if (container == null) {
            // Failed
            return null;
        }
        Class<?> canPlaceClass = container.getClass();
        Object nmsItemStack = getNmsHandle(new ItemStack(Material.AIR));
        if (nmsItemStack == null) {
            return null;
        }
        Optional<Method> optional;
        while (!(optional = Arrays.stream(canPlaceClass.getDeclaredMethods())
                .filter(it -> it.getReturnType() == boolean.class && it.getParameterCount() == 2
                        && it.getParameterTypes()[0] == int.class && it.getParameterTypes()[1] == nmsItemStack.getClass())
                .findFirst()).isPresent()) {
            Method searchInterfaces = searchInterfaces(canPlaceClass);
            if (searchInterfaces != null) {
                return searchInterfaces;
            }
            canPlaceClass = canPlaceClass.getSuperclass();
            if (canPlaceClass == Object.class) {
                return null;
            }
        }
        Method method = optional.get();
        method.setAccessible(true);
        return method;
    }

    private static Method cacheGetSlotsForFace(Inventory inventory) {
        Object container = getNmsHandle(inventory);
        if (container == null) {
            // Failed
            return null;
        }
        Class<?> clazz = container.getClass();
        Optional<Method> optional;
        while (!(optional = Arrays.stream(clazz.getDeclaredMethods())
                .filter(it -> it.getReturnType() == int[].class && it.getParameterCount() == 1
                        && it.getParameterTypes()[0] == getNmsDirection())
                .findFirst()).isPresent()) {
            clazz = clazz.getSuperclass();
            if (clazz == Object.class) {
                return null;
            }
        }
        Method method = optional.get();
        method.setAccessible(true);
        return method;
    }

    private static Method searchInterfaces(Class<?> clazz) {
        Object nmsItemStack = getNmsHandle(new ItemStack(Material.AIR));
        for (Class<?> clazzInterface : clazz.getInterfaces()) {
            Optional<Method> optional = Arrays.stream(clazzInterface.getDeclaredMethods())
                    .filter(it -> it.getReturnType() == boolean.class && it.getParameterCount() == 2
                            && it.getParameterTypes()[0] == int.class && it.getParameterTypes()[1] == nmsItemStack.getClass())
                    .findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        return null;
    }

    private static Object getNmsHandle(InventoryView inventoryView) {
        try {
            Method getNms = inventoryView.getClass().getDeclaredMethod("getHandle");
            getNms.setAccessible(true);
            return getNms.invoke(inventoryView); // NMS ContainerMenu instance
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Prism.getInstance().getLogger().log(Level.WARNING, "Cannot get NMS container menu from Bukkit inventoryView.", e);
            return null;
        }
    }

    private static Object getNmsHandle(Inventory inventory) {
        try {
            Class<?> clazz = inventory.getClass();
            while (true) {
                Class<?> temp = clazz.getSuperclass();
                if (temp == Object.class) {
                    break;
                }
                clazz = temp;
            }
            Method getNms = clazz.getDeclaredMethod("getInventory");
            getNms.setAccessible(true);
            return getNms.invoke(inventory); // NMS Container instance
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Prism.getInstance().getLogger().log(Level.WARNING, "Cannot get NMS container from Bukkit inventory.", e);
            return null;
        }
    }

    private static Object getNmsHandle(ItemStack itemStack) {
        try {
            Class<?> craftClass = getCraftItemStack();
            if (craftClass == null) {
                return null;
            }
            if (craftClass.isInstance(itemStack)) {
                Field handle = craftClass.getDeclaredField("handle");
                handle.setAccessible(true);
                return handle.get(itemStack);
            } else {
                Method asNMSCopy = craftClass.getDeclaredMethod("asNMSCopy", ItemStack.class);
                asNMSCopy.setAccessible(true);
                return asNMSCopy.invoke(null, itemStack);
            }
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
            Prism.getInstance().getLogger().log(Level.WARNING, "Cannot get NMS itemstack from Bukkit itemstack.", e);
            return null;
        }
    }

    private static Class<?> getCraftItemStack() {
        String name = Bukkit.getServer().getClass().getCanonicalName();
        try {
            return Class.forName(name.replace(".CraftServer", ".inventory.CraftItemStack"));
        } catch (ClassNotFoundException e) {
            Prism.getInstance().getLogger().log(Level.WARNING, "Cannot get CraftItemStack class.", e);
            return null;
        }
    }

    private static String getMc16NmsPackage() {
        try {
            return Arrays.stream(Package.getPackages())
                    .filter(it -> it.getName().startsWith("net.minecraft.server.v")).findAny().get().getName();
        } catch (Throwable t) {
            // Maybe since 1.17?
            return null;
        }
    }

    private static Class<?> getNmsDirection() {
        try {
            if (Prism.getInstance().getServerMajorVersion() >= 17) {
                return Class.forName("net.minecraft.core.EnumDirection");
            } else {
                return Class.forName(NMS_PACKAGE_16 + ".EnumDirection");
            }
        } catch (ClassNotFoundException e) {
            Prism.getInstance().getLogger().log(Level.WARNING, "Cannot get NMS Direction class.", e);
            return null;
        }
    }

    public static class WrappedSlot {

        /**
         * Slot index
         */
        private final int slot;
        /**
         * public boolean mayPlace(net.minecraft.world.item.ItemStack var0)
         */
        private final Method acceptsItemPlace;
        /**
         * NMS Slot handle
         */
        private final Object handle;

        public WrappedSlot(int slot, Method acceptsItemPlace, Object handle) {
            this.slot = slot;
            this.acceptsItemPlace = acceptsItemPlace;
            this.handle = handle;
        }

        public int slot() {
            return slot;
        }

        public boolean acceptsPlace(ItemStack itemStack) {
            Object nms = getNmsHandle(itemStack);
            if (nms == null) {
                // Failed to get NMS handle, don't check.
                return true;
            }
            try {
                return (boolean) acceptsItemPlace.invoke(this.handle, nms);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
