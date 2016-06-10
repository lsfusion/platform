package lsfusion.server.session;

import lsfusion.server.data.SQLHandledException;

import java.sql.SQLException;

public class CompoundUpdateCurrentClasses implements UpdateCurrentClasses {
    private final UpdateCurrentClasses[] updates;

    public CompoundUpdateCurrentClasses(UpdateCurrentClasses... updates) {
        this.updates = updates;
    }

    public void update(DataSession session) throws SQLException, SQLHandledException {
        for (UpdateCurrentClasses update : updates) {
            if (update != null) {
                update.update(session);
            }
        }
    }
    
    public static UpdateCurrentClasses merge(UpdateCurrentClasses... updates) {
        boolean compound = false;
        UpdateCurrentClasses theOnlyNotNull = null;
        for (UpdateCurrentClasses update : updates) {
            if (update != null) {
                if (theOnlyNotNull != null) {
                    compound = true;
                    break;
                }
                theOnlyNotNull = update;
            }
        }
        
        if (compound) {
            return new CompoundUpdateCurrentClasses(updates);
        }
        
        return theOnlyNotNull;
    }
}
