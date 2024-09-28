package network.darkhelmet.prism.database.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import network.darkhelmet.prism.Prism;
import network.darkhelmet.prism.database.PrismDataSource;
import network.darkhelmet.prism.database.sql.SqlPrismDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;

public class PrismHikariDataSource extends SqlPrismDataSource {

    private static final File propFile = new File(Prism.getInstance().getDataFolder(),
            "hikari.properties");
    private static final HikariConfig dbConfig;

    static {
        if (propFile.exists()) {
            Prism.log("Configuring Hikari from " + propFile.getName());
            dbConfig = new HikariConfig(propFile.getPath());
        } else {
            Prism.log("You may need to adjust these settings for your setup.");
            Prism.log("To change the table prefix you will need to edit datasource.properties.prefix entry in config.yml");
            String jdbcUrl = "jdbc:mysql://localhost:3306/prism?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
            Prism.log("Default jdbcUrl: " + jdbcUrl);
            Prism.log("Default Username: username");
            Prism.log("Default Password: password");
            Prism.log("You may need to provide the required jar libraries(driver) that support your database.");
            dbConfig = new HikariConfig();
            dbConfig.setJdbcUrl(jdbcUrl);
            dbConfig.setUsername("username");
            dbConfig.setPassword("password");
            dbConfig.setMinimumIdle(2);
            dbConfig.setMaximumPoolSize(10);
            HikariHelper.createPropertiesFile(propFile, dbConfig, false);
        }
    }

    /**
     * Create a dataSource.
     *
     * @param section Config
     */
    public PrismHikariDataSource(ConfigurationSection section) {
        super(section);
        name = "hikari";
    }

    @Override
    public PrismDataSource createDataSource() {
        try {
            database = new HikariDataSource(dbConfig);
            createSettingsQuery();
            return this;
        } catch (HikariPool.PoolInitializationException e) {
            Prism.warn("Hikari Pool did not Initialize: " + e.getMessage());
            database = null;
        } catch (IllegalArgumentException e) {
            Prism.warn("Hikari Pool did not Initialize: " + e);
            database = null;
        }
        return this;

    }

    @Override
    public void setFile() {

    }
}
