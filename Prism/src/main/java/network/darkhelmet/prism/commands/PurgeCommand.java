package network.darkhelmet.prism.commands;

import network.darkhelmet.prism.Il8nHelper;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.commandlibs.CallInfo;
import network.darkhelmet.prism.commandlibs.SubHandler;
import network.darkhelmet.prism.text.ReplaceableTextComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import network.darkhelmet.prism.utils.folia.PrismScheduler;

import java.util.List;
import java.util.Objects;

public class PurgeCommand implements SubHandler {

    private final Prism plugin;

    public PurgeCommand(Prism plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CallInfo call) {
        if (call.getArgs().length <= 1) {
            Prism.messenger.sendMessage(call.getSender(),
                    Prism.messenger.playerHeaderMsg(Component.text("Prism")
                            .append(Component.text(" v" + plugin.getPrismVersion()).color(NamedTextColor.GRAY))));
            Prism.messenger.sendMessage(call.getSender(),
                  Prism.messenger.playerSubduedHeaderMsg(ReplaceableTextComponent.builder("purge-report")
                    .replace("<taskCount>", plugin.getSchedulePool().getTaskCount())
                    .replace("<purgesComplete>", plugin.getSchedulePool().getCompletedTaskCount())
                    .replace("<poolString>", plugin.getSchedulePool().toString())
                    .build()));
        } else if (call.getArgs().length > 1) {
            if (Objects.equals(call.getArgs()[1], "execute")) {
                Prism.messenger.sendMessage(call.getSender(),
                        Prism.messenger.playerHeaderMsg(Il8nHelper.getMessage("purge-execute")));
                PrismScheduler.runTaskAsynchronously(plugin.getPurgeManager());
            }
        } else {
            Prism.messenger.sendMessage(call.getSender(),Il8nHelper.getMessage("invalid-command"));
        }

    }

    @Override
    public List<String> handleComplete(CallInfo call) {
        return null;
    }

    @Override
    public String[] getHelp() {
        return new String[]{Il8nHelper.getRawMessage("help-purge")};
    }

    @Override
    public String getRef() {
        return "/purge.html";
    }
}
