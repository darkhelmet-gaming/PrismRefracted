package network.darkhelmet.prism.actions;

import com.google.gson.annotations.SerializedName;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.api.actions.PrismProcessType;

public class PrismProcessAction extends GenericAction {

    /**
     * The extra data.
     */
    private PrismProcessActionData actionData;

    /**
     * Process.
     * @param processType PrismProcessType
     * @param parameters String
     */
    public void setProcessData(PrismProcessType processType, String parameters) {

        actionData = new PrismProcessActionData();

        if (processType != null) {
            actionData.params = parameters;
            actionData.processType = processType.name().toLowerCase();
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

    @Override
    public void deserialize(String data) {
        if (data != null && !data.isEmpty()) {
            actionData = gson().fromJson(data, PrismProcessActionData.class);
        }
    }

    /**
     * Get Type.
     * @return String
     */
    public String getProcessChildActionType() {
        return Prism.getActionRegistry().getAction("prism-" + actionData.processType).getName();
    }

    /**
     * Get nice name.
     */
    @Override
    public String getNiceName() {
        return actionData.processType + " (" + actionData.params + ")";
    }

    public static class PrismProcessActionData {
        @SerializedName(value = "p", alternate = {"params"})
        public String params = "";
        @SerializedName(value = "pt", alternate = {"processType"})
        public String processType;
    }
}