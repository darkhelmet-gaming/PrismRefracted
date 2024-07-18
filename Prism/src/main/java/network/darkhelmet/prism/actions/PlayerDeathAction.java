package network.darkhelmet.prism.actions;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class PlayerDeathAction extends GenericAction {
    @SerializedName(value = "c", alternate = {"cause"})
    private String cause;
    @SerializedName(value = "a", alternate = {"attacker"})

    private String attacker;

    public void setCause(String cause) {
        this.cause = cause;
    }

    public void setAttacker(String attacker) {
        this.attacker = attacker;
    }

    @Override
    public void deserialize(String data) {
        if (data != null) {
            final String[] dataArr = data.split(":");
            cause = dataArr[0];
            if (dataArr.length > 1) {
                attacker = dataArr[1];
            }
        }
    }

    @Override
    public boolean hasExtraData() {
        return cause != null;
    }

    @Override
    public String serialize() {
        if (cause != null) {
            if (attacker != null) {
                return cause + ":" + attacker;
            } else {
                return cause;
            }
        }

        return "";
    }

    @Override
    public String getNiceName() {
        String name = "";
        if (attacker != null && !attacker.isEmpty()) {
            name += attacker;
        }
        if (!Objects.equals(attacker, cause) && cause != null && !cause.isEmpty()) {
            name += "(" + cause + ")";
        }
        return name;
    }
}