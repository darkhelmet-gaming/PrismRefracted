package network.darkhelmet.prism.commands;

import network.darkhelmet.prism.Il8nHelper;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionsQuery;
import network.darkhelmet.prism.actionlibs.QueryParameters;
import network.darkhelmet.prism.actionlibs.QueryResult;
import network.darkhelmet.prism.api.actions.PrismProcessType;
import network.darkhelmet.prism.appliers.PrismApplierCallback;
import network.darkhelmet.prism.appliers.Rollback;
import network.darkhelmet.prism.commandlibs.CallInfo;
import network.darkhelmet.prism.commandlibs.PreprocessArgs;
import network.darkhelmet.prism.text.ReplaceableTextComponent;
import network.darkhelmet.prism.utils.folia.PrismScheduler;

import java.util.List;

public class RollbackCommand extends AbstractCommand {

    private final Prism plugin;

    public RollbackCommand(Prism plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(final CallInfo call) {

        final QueryParameters parameters = PreprocessArgs.process(plugin, call.getSender(), call.getArgs(),
                PrismProcessType.ROLLBACK, 1, !plugin.getConfig().getBoolean("prism.queries.never-use-defaults"));
        if (parameters == null) {
            return;
        }
        parameters.setProcessType(PrismProcessType.ROLLBACK);
        parameters.setStringFromRawArgs(call.getArgs(), 1);
        StringBuilder defaultsReminder = checkIfDefaultUsed(parameters);
        Prism.messenger.sendMessage(call.getSender(),
                Prism.messenger.playerSubduedHeaderMsg(ReplaceableTextComponent.builder("rollback-prepare")
                        .replace("<defaults>", defaultsReminder)
                        .build()));
        PrismScheduler.runTaskAsynchronously(() -> {

            final ActionsQuery aq = new ActionsQuery(plugin);
            final QueryResult results = aq.lookup(parameters, call.getSender());
            if (!results.getActionResults().isEmpty()) {

                Prism.messenger.sendMessage(call.getSender(),
                        Prism.messenger.playerHeaderMsg(Il8nHelper.getMessage("rollback-start")));

                // Perform rollback on the main thread
                // TODO: FOLIA TEST
                PrismScheduler.runTask(() -> {
                    final Rollback rb = new Rollback(plugin, call.getSender(), results.getActionResults(),
                            parameters, new PrismApplierCallback());
                    rb.apply();
                });

            } else {
                Prism.messenger.sendMessage(call.getSender(),
                        Prism.messenger.playerError(Il8nHelper.getMessage("rollback-error")));
            }
        });
    }

    @Override
    public List<String> handleComplete(CallInfo call) {
        return PreprocessArgs.complete(call.getSender(), call.getArgs());
    }

    @Override
    public String[] getHelp() {
        return new String[]{Il8nHelper.getRawMessage("help-rollback")};
    }

    @Override
    public String getRef() {
        return "/rollbacks.html";
    }
}