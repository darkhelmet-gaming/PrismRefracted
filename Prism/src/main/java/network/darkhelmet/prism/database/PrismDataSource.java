package network.darkhelmet.prism.database;

import network.darkhelmet.prism.actionlibs.ActionRegistry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public interface PrismDataSource {

    boolean isPaused();

    void setPaused(boolean paused);

    String getName();

    default String getPrefix() {
        return "prism_";
    }

    PrismDataSource createDataSource();

    void setFile();

    void setupDatabase(ActionRegistry actionRegistry);

    Connection getConnection();

    void rebuildDataSource();


    DataSource getDataSource();

    void handleDataSourceException(SQLException e);

    void cacheWorldPrimaryKeys(Map<String, Integer> prismWorlds);

    void addWorldName(String worldName);

    void addActionName(String actionName);

    void dispose();

    SelectQuery createSelectQuery();

    SelectIdQuery createSelectIdQuery();

    DeleteQuery createDeleteQuery();

    BlockReportQuery createBlockReportQuery();

    ActionReportQuery createActionReportQuery();

    SettingsQuery createSettingsQuery();

    SelectProcessActionQuery createProcessQuery();

    UpdateQuery createUpdateQuery();

    InsertQuery getDataInsertionQuery();
}
