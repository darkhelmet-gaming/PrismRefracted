package network.darkhelmet.prism.bridge;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitBlockCommandSender;
import com.sk89q.worldedit.bukkit.BukkitCommandSender;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionFactory;
import network.darkhelmet.prism.actionlibs.RecordingQueue;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class PrismWorldEditLogger extends AbstractDelegateExtent {
    private final Actor actor;
    private final World world;

    /**
     * Constructor.
     *
     * @param actor Actor
     * @param extent Extent
     * @param world  World
     */
    public PrismWorldEditLogger(Actor actor, Extent extent, World world) {
        super(extent);
        this.actor = actor;
        this.world = world;
    }

    @Override
    public boolean setBlock(BlockVector3 pt, BlockStateHolder newBlock) throws WorldEditException {
        if (Prism.config.getBoolean("prism.tracking.world-edit")) {
            Location loc = BukkitAdapter.adapt(world, pt);
            Block oldBlock = loc.getBlock();
            Material newMaterial = BukkitAdapter.adapt(newBlock.getBlockType());
            BlockData newData = BukkitAdapter.adapt(newBlock);
            if (actor.isPlayer()) {
                RecordingQueue.addToQueue(ActionFactory.createBlockChange("world-edit", loc, oldBlock.getType(),
                        oldBlock.getBlockData(), newMaterial, newData, Bukkit.getPlayer(actor.getUniqueId())));
            } else {
                String nonPlayer;
                if (actor instanceof BukkitCommandSender) {
                    nonPlayer = "Console";
                } else if (actor instanceof BukkitBlockCommandSender) {
                    com.sk89q.worldedit.util.Location location = ((BukkitBlockCommandSender) actor).getBlockLocation();
                    nonPlayer = "Command Block(" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")";
                } else {
                    nonPlayer = actor.getName();
                }
                RecordingQueue.addToQueue(ActionFactory.createBlockChange("world-edit", loc, oldBlock.getType(),
                        oldBlock.getBlockData(), newMaterial, newData, nonPlayer));
            }
        }
        return super.setBlock(pt, newBlock);
    }
}