package network.darkhelmet.prism.database;

import network.darkhelmet.prism.api.actions.Handler;

public interface UpdateQuery {

    void updateRollbacked(Handler... handlers);

}
