package network.darkhelmet.prism.commands;

import network.darkhelmet.prism.Il8nHelper;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.actionlibs.ActionsQuery;
import network.darkhelmet.prism.actionlibs.QueryParameters;
import network.darkhelmet.prism.actionlibs.RecordingQueue;
import network.darkhelmet.prism.api.actions.PrismProcessType;
import network.darkhelmet.prism.commandlibs.CallInfo;
import network.darkhelmet.prism.commandlibs.PreprocessArgs;
import network.darkhelmet.prism.purge.PurgeTask;
import network.darkhelmet.prism.purge.SenderPurgeCallback;
import net.kyori.adventure.text.Component;
import network.darkhelmet.prism.utils.folia.PrismScheduler;
import network.darkhelmet.prism.utils.folia.PrismTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class DeleteCommand extends AbstractCommand {

    private final Prism plugin;
    private PrismTask deleteTask;

    /**
     * Constructor.
     *
     * @param plugin Prism
     */
    public DeleteCommand(Prism plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(final CallInfo call) {

        // Allow for canceling tasks
        if (call.getArgs().length > 1 && call.getArg(1).equals("cancel")) {
            if (plugin.getPurgeManager().deleteTask != null) {
                plugin.getPurgeManager().deleteTask.cancel();
                Prism.messenger.sendMessage(call.getSender(),
                        Prism.messenger.playerMsg(Il8nHelper.getMessage("cancel-purge")));
            } else {
                Prism.messenger.sendMessage(call.getSender(),
                        Prism.messenger.playerError(Il8nHelper.getMessage("no-purge-running")));
            }
            return;
        }

        // Allow for wiping live queue
        if (call.getArgs().length > 1 && call.getArg(1).equals("queue")) {
            if (RecordingQueue.getQueue().size() > 0) {
                Prism.log("User " + call.getSender().getName()
                        + " wiped the live queue before it could be written to the database. "
                        + RecordingQueue.getQueue().size() + " events lost.");
                RecordingQueue.getQueue().clear();
                Prism.messenger.sendMessage(call.getSender(),
                        Prism.messenger.playerSuccess(Il8nHelper.getMessage("clear-queue")));
            } else {
                Prism.messenger.sendMessage(call.getSender(),
                        Prism.messenger.playerError(
                                Il8nHelper.getMessage("event-queue-clear")));
            }
            return;
        }

        // Process and validate all of the arguments
        final QueryParameters parameters = PreprocessArgs.process(plugin, call.getSender(), call.getArgs(),
                PrismProcessType.DELETE, 1, !plugin.getConfig().getBoolean("prism.queries.never-use-defaults"));
        if (parameters == null) {
            return;
        }
        parameters.setStringFromRawArgs(call.getArgs(), 1);

        StringBuilder defaultsReminder = checkIfDefaultUsed(parameters);
        if (parameters.getFoundArgs().size() > 0) {

            Prism.messenger.sendMessage(call.getSender(),
                    Prism.messenger.playerSubduedHeaderMsg(Il8nHelper.getMessage("purge-data")
                            .replaceFirstText(Pattern.compile("<defaults>"), builder ->
                                    Component.text()
                                            .content(defaultsReminder.toString()))));
            Prism.messenger.sendMessage(call.getSender(), Prism.messenger
                    .playerHeaderMsg(Il8nHelper.getMessage("start-purge")));
            PrismScheduler.runTaskAsynchronously(() -> {
                int purgeTickDelay = plugin.getConfig().getInt("prism.purge.batch-tick-delay");
                if (purgeTickDelay < 0) {
                    purgeTickDelay = 20;
                }

                // build callback
                final SenderPurgeCallback callback = new SenderPurgeCallback();
                callback.setSender(call.getSender());

                // add to an arraylist so we're consistent
                final CopyOnWriteArrayList<QueryParameters> paramList = new CopyOnWriteArrayList<>();
                paramList.add(parameters);

                final ActionsQuery aq = new ActionsQuery(plugin);
                final long[] extents = aq.getQueryExtents(parameters);
                final long minId = extents[0];
                final long maxId = extents[1];
                Prism.log(
                        "Beginning prism database purge cycle. Will be performed in batches so "
                                + "we don't tie up the db...");
                deleteTask = PrismScheduler.runTaskAsynchronously(
                        new PurgeTask(plugin, paramList, purgeTickDelay, minId, maxId, callback));
            });
        } else {
            Prism.messenger.sendMessage(call.getSender(),
                    Prism.messenger.playerError(Il8nHelper.getMessage("no-parameter")));
        }
    }

    @Override
    public List<String> handleComplete(CallInfo call) {
        return PreprocessArgs.complete(call.getSender(), call.getArgs());
    }

    @Override
    public String[] getHelp() {
        return new String[]{Il8nHelper.getRawMessage("help-delete")};
    }

    @Override
    public String getRef() {
        return null;
    }
}