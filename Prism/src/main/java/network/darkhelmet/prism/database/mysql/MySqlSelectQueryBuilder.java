package network.darkhelmet.prism.database.mysql;

import network.darkhelmet.prism.database.PrismDataSource;
import network.darkhelmet.prism.database.sql.SqlSelectQueryBuilder;
import network.darkhelmet.prism.utils.TypeUtils;

public class MySqlSelectQueryBuilder extends SqlSelectQueryBuilder {

    public MySqlSelectQueryBuilder(PrismDataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected String select() {
        String query = "";

        query += "SELECT ";
        columns.add("id");
        columns.add("epoch");
        columns.add("action_id");
        columns.add("player");
        columns.add("world_id");

        if (shouldGroup) {
            columns.add("AVG(x)");
            columns.add("AVG(y)");
            columns.add("AVG(z)");
        } else {
            columns.add("x");
            columns.add("y");
            columns.add("z");
        }

        columns.add("block_id");
        columns.add("block_subid");
        columns.add("old_block_id");
        columns.add("old_block_subid");
        columns.add("data");
        columns.add("HEX(player_uuid)");
        columns.add("rollbacked");
        if (shouldGroup) {
            columns.add("COUNT(*) counted");
        }

        // Append all columns
        if (columns.size() > 0) {
            query += TypeUtils.join(columns, ", ");
        }

        // From
        query += " FROM " + tableNameData + " ";

        // Joins
        query += "INNER JOIN " + prefix + "players p ON p.player_id = " + tableNameData + ".player_id ";
        query += "LEFT JOIN " + tableNameDataExtra + " ex ON ex.data_id = " + tableNameData + ".id ";

        return query;

    }

    @Override
    protected String order() {
        if (parameters == null) {
            return " ";
        }
        final String sort_dir = parameters.getSortDirection();

        if (shouldGroup) {
            return " ORDER BY MAX(" + tableNameData + ".epoch) " + sort_dir
                    + ", AVG(x) ASC, AVG(z) ASC, AVG(y) ASC, id " + sort_dir;
        }

        return " ORDER BY " + tableNameData + ".epoch " + sort_dir + ", x ASC, z ASC, y ASC, id " + sort_dir;
    }
}