package network.darkhelmet.prism.actions;

import com.google.gson.annotations.SerializedName;
import network.darkhelmet.prism.Prism;
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

public class SignGlowAction extends GenericAction {

    private static final boolean POST_20 = Prism.getInstance().getServerMajorVersion() >= 20;
    protected SignGlowActionData actionData;

    public void setBlock(Block block, boolean makeGlow, boolean isFront) {

        // Build an object for the specific details of this action
        actionData = new SignGlowActionData();

        if (block != null) {
            setMaterial(block.getType());
            setLoc(block.getLocation());
        }
        actionData.makeGlow = makeGlow;
        actionData.frontSide = isFront;
    }

    @Override
    public ChangeResult applyRollback(Player player, PrismParameters parameters, boolean isPreview) {
        ChangeResult changeResult = setSignGlow(!actionData.makeGlow, isPreview);
        if (changeResult.getType() == ChangeResultType.APPLIED && !isPreview) {
            placeInkItem(true);
        }
        return changeResult;
    }

    @Override
    public ChangeResult applyRestore(Player player, PrismParameters parameters, boolean isPreview) {
        ChangeResult changeResult = setSignGlow(actionData.makeGlow, isPreview);
        if (changeResult.getType() == ChangeResultType.APPLIED) {
            placeInkItem(false);
        }
        return changeResult;
    }

    private ChangeResult setSignGlow(boolean glow, boolean isPreview) {
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
                    side.setGlowingText(glow);
                } else {
                    sign.setGlowingText(glow);
                }
                sign.update(true, false);
                return new ChangeResultImpl(ChangeResultType.APPLIED, null);
            }
        }
        return new ChangeResultImpl(ChangeResultType.SKIPPED, null);
    }

    private void placeInkItem(boolean rollback) {
        Player player = Bukkit.getPlayer(getUuid());
        if (player == null) {
            return;
        }
        ItemStack item = new ItemStack(actionData.makeGlow ? Material.GLOW_INK_SAC : Material.INK_SAC);
        if (rollback) {
            player.getInventory().addItem(item);
        } else {
            player.getInventory().removeItem(item);
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
        actionData = gson().fromJson(data, SignGlowActionData.class);
    }

    @Override
    public String getNiceName() {
        return getMaterial().name().toLowerCase().replace('_', ' ')
                + (actionData.makeGlow ? "" : " (cancelled)")
                + " at " + (actionData.frontSide ? "front" : "back");
    }

    public class SignGlowActionData {
        @SerializedName(value = "fs", alternate = {"frontSide"})
        public boolean frontSide;
        @SerializedName(value = "mg", alternate = {"makeGlow"})
        public boolean makeGlow;
    }

}
