package network.darkhelmet.prism.actionlibs;

import network.darkhelmet.prism.Prism;

import java.sql.Connection;
import java.sql.SQLException;

public class InternalAffairs implements Runnable {

    private final Prism plugin;

    public InternalAffairs(Prism plugin) {
        Prism.debug("[InternalAffairs] Keeping watch over the recorder.");
        this.plugin = plugin;
    }

    @Override
    public void run() {

        if (plugin.recordingTask != null && plugin.recordingTask.isActive()) {
            Prism.debug("[InternalAffairs] Recorder is currently active. All is good.");
            return;
        }

        Prism.log("[InternalAffairs] Recorder is NOT active. checking database...");

        // is db connection valid?
        try (Connection conn = Prism.getPrismDataSource().getConnection()) {
            if (conn == null) {
                Prism.log("[InternalAffairs] Pool returned NULL instead of a valid connection.");
            } else if (conn.isClosed()) {
                Prism.log("[InternalAffairs] Pool returned an already closed connection.");
            } else if (conn.isValid(5)) {
                Prism.log("[InternalAffairs] Pool returned valid connection!");
                Prism.log("[InternalAffairs] Restarting scheduled recorder tasks.");
                plugin.actionRecorderTask();
            }
        } catch (final SQLException e) {
            Prism.debug("[InternalAffairs] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}