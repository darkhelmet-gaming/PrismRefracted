package network.darkhelmet.prism.actions;

import com.google.gson.annotations.SerializedName;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.api.ChangeResult;
import network.darkhelmet.prism.api.ChangeResultType;
import network.darkhelmet.prism.api.PrismParameters;
import network.darkhelmet.prism.appliers.ChangeResultImpl;
import network.darkhelmet.prism.utils.EntityUtils;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.entity.minecart.StorageMinecart;

public class VehicleAction extends GenericAction {

    private static byte serverMajorVersion = Prism.getInstance().getServerMajorVersion();

    private VehicleActionData actionData;

    /**
     * Set the vehicle.
     * @param vehicle Entity
     */
    public void setVehicle(Entity vehicle) {

        actionData = new VehicleActionData();
        if (vehicle instanceof PoweredMinecart) {
            actionData.vehicleName = "powered minecart";
        } else if (vehicle instanceof HopperMinecart) {
            actionData.vehicleName = "minecart hopper";
        } else if (vehicle instanceof SpawnerMinecart) {
            actionData.vehicleName = "spawner minecart";
        } else if (vehicle instanceof ExplosiveMinecart) {
            actionData.vehicleName = "tnt minecart";
        } else if (vehicle instanceof StorageMinecart) {
            actionData.vehicleName = "storage minecart";
        } else if (serverMajorVersion >= 19 && vehicle instanceof ChestBoat) {
            actionData.vehicleName = "chest boat";
        } else {
            actionData.vehicleName = vehicle.getType().name().toLowerCase();
        }

        if (vehicle instanceof Boat) {
            actionData.woodType = EntityUtils.treeSpeciesToName(((Boat) vehicle).getWoodType());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNiceName() {
        return (actionData.woodType != null ? actionData.woodType + " " : "") + actionData.vehicleName;
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
        if (data.startsWith("{")) {
            actionData = gson().fromJson(data, VehicleActionData.class);
        } else {
            // Old version support
            actionData = new VehicleActionData();
            actionData.vehicleName = data;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeResult applyRollback(Player player, PrismParameters parameters, boolean isPreview) {
        if (isPreview) {
            // TODO: just returning PLANNED, not previewed right now.
            return new ChangeResultImpl(ChangeResultType.PLANNED, null);
        }
        Entity vehicle = null;
        switch (actionData.vehicleName) {
            case "command block minecart":
                vehicle = getWorld().spawn(getLoc(), CommandMinecart.class);
                break;
            case "powered minecart":
                vehicle = getWorld().spawn(getLoc(), PoweredMinecart.class);
                break;
            case "storage minecart":
                vehicle = getWorld().spawn(getLoc(), StorageMinecart.class);
                break;
            case "tnt minecart":
                vehicle = getWorld().spawn(getLoc(), ExplosiveMinecart.class);
                break;
            case "spawner minecart":
                vehicle = getWorld().spawn(getLoc(), SpawnerMinecart.class);
                break;
            case "minecart hopper":
                vehicle = getWorld().spawn(getLoc(), HopperMinecart.class);
                break;
            case "minecart":
                vehicle = getWorld().spawn(getLoc(), Minecart.class);
                break;
            case "boat":
                vehicle = getWorld().spawn(getLoc(), Boat.class);
                break;
            case "chest boat":
                vehicle = getWorld().spawn(getLoc(), ChestBoat.class);
                break;
            default:
                //null
        }
        if (vehicle == null) {
            return new ChangeResultImpl(ChangeResultType.SKIPPED, null);
        }

        if (vehicle instanceof Boat && actionData.woodType != null) {
            ((Boat) vehicle).setWoodType(EntityUtils.nameToTreeSpecies(actionData.woodType));
        }

        return new ChangeResultImpl(ChangeResultType.APPLIED, null);
    }

    public static class VehicleActionData {
        @SerializedName(value = "vn", alternate = {"vehicleName"})
        String vehicleName;
        @SerializedName(value = "wt", alternate = {"woodType"})
        String woodType;
    }
}
