package network.darkhelmet.prism.commands;

import network.darkhelmet.prism.Il8nHelper;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.QueryParameters;
import network.darkhelmet.prism.commandlibs.SubHandler;
import org.bukkit.command.CommandSender;

import java.util.List;

public abstract class AbstractCommand implements SubHandler {

    final StringBuilder checkIfDefaultUsed(QueryParameters parameters) {
        final List<String> defaultsUsed = parameters.getDefaultsUsed();
        StringBuilder defaultsReminder = new StringBuilder();
        if (!defaultsUsed.isEmpty()) {
            defaultsReminder.append(" using defaults:");
            for (final String d : defaultsUsed) {
                defaultsReminder.append(" ").append(d);
            }
        }
        return defaultsReminder;
    }

    @SuppressWarnings("WeakerAccess")
    protected static boolean checkRecorderActive(Prism plugin) {
        boolean recorderActive = false;
        if (plugin.recordingTask != null && plugin.recordingTask.isActive()) {
            recorderActive = true;
        }
        return recorderActive;
    }

    protected static boolean checkNoPermissions(CommandSender sender, String... permissions) {
        for (String perm : permissions) {
            if (!sender.hasPermission(perm)) {
                Prism.messenger.sendMessage(sender,
                        Prism.messenger.playerError(Il8nHelper.getMessage("no-permission")));
                return true;
            }
        }
        return false;
    }
}
