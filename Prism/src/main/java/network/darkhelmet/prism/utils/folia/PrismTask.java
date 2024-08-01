package network.darkhelmet.prism.utils.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import network.darkhelmet.prism.Prism;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class PrismTask {

    private static final boolean FOLIA = Prism.isFolia;

    private final Object task;
    private final int taskId;

    public PrismTask(ScheduledTask task) {
        this.task = task;
        this.taskId = -1;
    }

    public PrismTask(BukkitTask task) {
        this.task = task;
        this.taskId = task.getTaskId();
    }

    public PrismTask(int taskId) {
        this.task = null;
        this.taskId = taskId;
    }

    public void cancel() {
        if (FOLIA) {
            ((ScheduledTask) task).cancel();
        } else {
            if (task != null) {
                ((BukkitTask) task).cancel();
            } else {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }
    }

    public boolean isCancelled() {
        if (task == null) {
            throw new IllegalStateException("Task is created by id");
        }
        if (FOLIA) {
            return ((ScheduledTask) task).isCancelled();
        } else {
            return ((BukkitTask) task).isCancelled();
        }
    }

    public boolean isActive() {
        if (task == null) {
            throw new IllegalStateException("Task is created by id");
        }
        if (FOLIA) {
            final ScheduledTask.ExecutionState state = ((ScheduledTask) task).getExecutionState();
            return state != ScheduledTask.ExecutionState.FINISHED && state != ScheduledTask.ExecutionState.CANCELLED;
        } else {
            final int taskId = ((BukkitTask) task).getTaskId();
            final BukkitScheduler scheduler = Bukkit.getScheduler();
            return scheduler.isCurrentlyRunning(taskId) || scheduler.isQueued(taskId);
        }
    }

}
