package network.darkhelmet.prism.actions;

import org.bukkit.block.Block;

public class BonemealUseAction extends GenericAction {

    public void setBlock(Block block) {
        setLoc(block.getLocation());
        setMaterial(block.getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNiceName() {
        return getMaterial().name().toLowerCase().replace("_", " ");
    }

    @Override
    public boolean hasExtraData() {
        return false;
    }

    @Override
    public String serialize() {
        return null;
    }

    @Override
    public void deserialize(String data) {
    }

}
