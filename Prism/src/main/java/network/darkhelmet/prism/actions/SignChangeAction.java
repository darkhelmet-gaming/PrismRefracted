package network.darkhelmet.prism.actions;

import com.google.gson.annotations.SerializedName;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.api.ChangeResult;
import network.darkhelmet.prism.api.ChangeResultType;
import network.darkhelmet.prism.api.PrismParameters;
import network.darkhelmet.prism.appliers.ChangeResultImpl;
import network.darkhelmet.prism.utils.TypeUtils;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;

import java.util.Objects;

public class SignChangeAction extends GenericAction {

    private static final boolean POST_20 = Prism.getInstance().getServerMajorVersion() >= 20;

    protected SignChangeActionData actionData;

    /**
     * Set the block.
     *
     * @param block Block
     * @param lines String[]
     */
    public void setBlock(Block block, String[] lines, boolean isFront) {

        // Build an object for the specific details of this action
        actionData = new SignChangeActionData();

        if (block != null) {
            actionData.signType = block.getType().name();

            if (block.getBlockData() instanceof Directional) {
                actionData.facing = ((Directional) block.getBlockData()).getFacing();
            }

            setMaterial(block.getType());
            setLoc(block.getLocation());
        }
        if (lines != null) {
            actionData.lines = lines;
        }

        actionData.frontSide = isFront;
        Sign blockState = (Sign) block.getState();
        if (POST_20) {
            actionData.oldLines = blockState.getSide(isFront ? Side.FRONT : Side.BACK).getLines();
        } else {
            actionData.oldLines = blockState.getLines();
        }
    }

    @Override
    public void deserialize(String data) {
        if (data != null && !data.isEmpty()) {
            actionData = gson().fromJson(data, SignChangeActionData.class);
        }
    }

    @Override
    public boolean hasExtraData() {
        return actionData != null;
    }

    @Override
    public String serialize() {
        return gson().toJson(actionData);
    }

    /**
     * Get Lines.
     * @return String[]
     */
    public String[] getLines() {
        return actionData.lines;
    }

    /**
     * Get the sign type.
     * @return Material
     */
    public Material getSignType() {
        if (actionData.signType != null) {
            final Material m = Material.matchMaterial(actionData.signType);
            if (m != null) {
                return m;
            }
        }

        // Could be legacy (x - 1.13) wall sign
        if (Objects.equals(actionData.signType, "WALL_SIGN")) {
            return Material.OAK_WALL_SIGN;
        }

        // Either was legacy standing sign or unknown/invalid. Default standing sign.
        return Material.OAK_SIGN;
    }

    /**
     * Get the blockface.
     * @return BlockFace.
     */
    public BlockFace getFacing() {
        return actionData.facing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNiceName() {
        String name = getMaterial().name().toLowerCase().replace('_', ' ') + " (";
        if (actionData.lines != null) {
            String join = TypeUtils.join(actionData.lines, ", ");
            if (join.isEmpty()) {
                name += "no text";
            } else {
                name += join;
            }
        }
        name += ") at " + (actionData.frontSide ? "front" : "back");
        return name;
    }

    @Override
    public ChangeResult applyRollback(Player player, PrismParameters parameters, boolean isPreview) {
        if (actionData.oldLines == null) {
            return new ChangeResultImpl(ChangeResultType.SKIPPED, null);
        }
        return setSignLines(actionData.oldLines, isPreview);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeResult applyRestore(Player player, PrismParameters parameters, boolean isPreview) {
        return setSignLines(getLines(), isPreview);
    }

    private ChangeResult setSignLines(String[] lines, boolean isPreview) {

        final Block block = getWorld().getBlockAt(getLoc());

        // Ensure a sign exists there (and no other block)
        if (block.getType().equals(Material.AIR) || Tag.SIGNS.isTagged(block.getType())
                || (Prism.getInstance().getServerMajorVersion() >= 20 && Tag.ALL_SIGNS.isTagged(block.getType()))) {
            if (isPreview) {
                // TODO: just returning PLANNED, not previewed right now.
                return new ChangeResultImpl(ChangeResultType.PLANNED, null);
            }

            if (block.getType().equals(Material.AIR)) {
                block.setType(getSignType());
            }

            // Set the facing direction
            if (block.getBlockData() instanceof Directional) {
                ((Directional) block.getBlockData()).setFacing(getFacing());
            }

            // Set the content
            if (block.getState() instanceof Sign) {

                // Set sign data
                final Sign sign = (Sign) block.getState();
                if (POST_20) {
                    SignSide side = sign.getSide(actionData.frontSide ? Side.FRONT : Side.BACK);
                    if (lines != null) {
                        for (int i = 0; i < 4; i++) {
                            side.setLine(i, lines[i]);
                        }
                    }
                } else {
                    if (lines != null) {
                        for (int i = 0; i < 4; i++) {
                            sign.setLine(i, lines[i]);
                        }
                    }
                }
                sign.update(true, false);
                return new ChangeResultImpl(ChangeResultType.APPLIED, null);
            }
        }
        return new ChangeResultImpl(ChangeResultType.SKIPPED, null);
    }

    public static class SignChangeActionData {
        @SerializedName(value = "fs", alternate = {"frontSide"})
        public boolean frontSide = true;
        @SerializedName(value = "ol", alternate = {"oldLines"})
        public String[] oldLines;
        @SerializedName(value = "l", alternate = {"lines"})
        public String[] lines;
        @SerializedName(value = "st", alternate = {"signType"})
        public String signType;
        @SerializedName(value = "f", alternate = {"facing"})
        public BlockFace facing;
    }
}