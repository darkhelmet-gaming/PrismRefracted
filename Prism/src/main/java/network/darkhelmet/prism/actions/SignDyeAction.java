package network.darkhelmet.prism.actions;

import com.google.gson.annotations.SerializedName;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.api.BlockStateChange;
import network.darkhelmet.prism.api.ChangeResult;
import network.darkhelmet.prism.api.ChangeResultType;
import network.darkhelmet.prism.api.PrismParameters;
import network.darkhelmet.prism.appliers.ChangeResultImpl;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SignDyeAction extends GenericAction {

    private static final boolean POST_20 = Prism.getInstance().getServerMajorVersion() >= 20;
    protected SignDyeActionData actionData;

    public void setBlock(Block block, DyeColor color, boolean isFront) {

        // Build an object for the specific details of this action
        actionData = new SignDyeActionData();

        if (block != null) {
            setMaterial(block.getType());
            setLoc(block.getLocation());
        }
        actionData.newColor = color;

        actionData.frontSide = isFront;
        Sign blockState = (Sign) block.getState();
        if (POST_20) {
            actionData.oldColor = blockState.getSide(isFront ? Side.FRONT : Side.BACK).getColor();
        } else {
            actionData.oldColor = blockState.getColor();
        }
    }

    @Override
    public ChangeResult applyRollback(Player player, PrismParameters parameters, boolean isPreview) {
        ChangeResult changeResult = setSignColor(actionData.oldColor, isPreview);
        if (changeResult.getType() == ChangeResultType.APPLIED && !isPreview) {
            placeDyeItem(true);
        }
        return changeResult;
    }

    @Override
    public ChangeResult applyRestore(Player player, PrismParameters parameters, boolean isPreview) {
        ChangeResult changeResult = setSignColor(actionData.newColor, isPreview);
        if (changeResult.getType() == ChangeResultType.APPLIED && !isPreview) {
            placeDyeItem(false);
        }
        return changeResult;
    }

    private ChangeResult setSignColor(DyeColor color, boolean isPreview) {
        final Block block = getWorld().getBlockAt(getLoc());

        // Ensure a sign exists there (and no other block)
        if (Tag.SIGNS.isTagged(block.getType())
                || (Prism.getInstance().getServerMajorVersion() >= 20 && Tag.ALL_SIGNS.isTagged(block.getType()))) {

            // Set the content
            if (block.getState() instanceof Sign) {
                if (isPreview) {
                    // TODO: just returning PLANNED, not previewed right now.
                    return new ChangeResultImpl(ChangeResultType.PLANNED, null);
                }

                // Set sign data
                final Sign sign = (Sign) block.getState();
                if (POST_20) {
                    SignSide side = sign.getSide(actionData.frontSide ? Side.FRONT : Side.BACK);
                    side.setColor(color);
                } else {
                    sign.setColor(color);
                }
                sign.update(true, false);
                return new ChangeResultImpl(ChangeResultType.APPLIED, null);
            }
        }
        return new ChangeResultImpl(ChangeResultType.SKIPPED, null);
    }

    private void placeDyeItem(boolean rollback) {
        Player player = Bukkit.getPlayer(getUuid());
        if (player == null) {
            return;
        }
        ItemStack dye = new ItemStack(Material.valueOf(actionData.newColor.name() + "_DYE"));
        if (rollback) {
            player.getInventory().addItem(dye);
        } else {
            player.getInventory().removeItem(dye);
        }
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
        actionData = gson().fromJson(data, SignDyeActionData.class);
    }

    @Override
    public String getNiceName() {
        return getMaterial().name().toLowerCase().replace('_', ' ')
                + " from " + actionData.oldColor.name().toLowerCase().replace('_', ' ')
                + " to "  + actionData.newColor.name().toLowerCase().replace('_', ' ')
                + " at " + (actionData.frontSide ? "front" : "back");
    }

    public class SignDyeActionData {
        @SerializedName(value = "fs", alternate = {"frontSide"})
        public boolean frontSide;
        @SerializedName(value = "oc", alternate = {"oldColor"})
        public DyeColor oldColor;
        @SerializedName(value = "nc", alternate = {"newColor"})
        public DyeColor newColor;
    }

}
