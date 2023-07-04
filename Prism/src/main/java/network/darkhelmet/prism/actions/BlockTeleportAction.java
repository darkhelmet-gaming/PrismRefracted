package network.darkhelmet.prism.actions;

import network.darkhelmet.prism.Prism;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class BlockTeleportAction extends BlockChangeAction {

    private TeleportActionData actionData = new TeleportActionData();

    public void setOtherLoc(Location other) {
        actionData.x = other.getBlock().getX();
        actionData.y = other.getBlock().getY();
        actionData.z = other.getBlock().getZ();
    }

    public void setFrom(boolean from) {
        actionData.from = from;
    }

    public void setBlock(Block from, Block to) {
        if (isFrom()) {
            setOtherLoc(to.getLocation());
            setOldMaterial(from.getType());
            setOldBlockData(from.getBlockData());

            setLoc(from.getLocation());
            setMaterial(Material.AIR);
        } else {
            setOtherLoc(from.getLocation());
            setOldMaterial(to.getType());
            setOldBlockData(to.getBlockData());

            setLoc(to.getLocation());
            setMaterial(from.getType());
            setBlockData(from.getBlockData());
        }
    }

    public boolean isFrom() {
        return actionData.from;
    }

    @Override
    public boolean hasExtraData() {
        return true;
    }

    @Override
    public String serialize() {
        return gson().toJson(actionData);
    }

    @Override
    public void deserialize(String data) {
        if (data != null && data.startsWith("{")) {
            actionData = gson().fromJson(data, TeleportActionData.class);
        }
    }

    @Override
    public String getNiceName() {
        String extraInfo = " unknown";
        if (actionData != null) {
            if (actionData.from) {
                extraInfo = " to " + actionData.x + " " + actionData.y + " " + actionData.z;
            } else {
                extraInfo = " from " + actionData.x + " " + actionData.y + " " + actionData.z;
            }
        }

        return (isFrom() ? Prism.getItems().getAlias(getOldMaterial(), getOldBlockData()) : Prism.getItems().getAlias(getMaterial(), getBlockData())) + extraInfo;
    }

    public static class TeleportActionData {
        boolean from;
        int x;
        int y;
        int z;
    }

}
