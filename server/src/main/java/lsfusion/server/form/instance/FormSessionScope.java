package lsfusion.server.form.instance;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public enum FormSessionScope {
    OLDSESSION, NEWSESSION, NESTEDSESSION, MANAGESESSION;

    public boolean isNewSession() {
        return this == NEWSESSION || this == NESTEDSESSION;
    }

    public boolean isManageSession() {
        return this != OLDSESSION;
    }
    
    public DataSession createSession(DataSession oldSession) throws SQLException, SQLHandledException {
        switch (this) {
            case MANAGESESSION:
            case OLDSESSION:
                return oldSession;
            
            case NEWSESSION:
                return oldSession.createSession();
            
            case NESTEDSESSION:
                DataSession newSession = oldSession.createSession();
                newSession.setParentSession(oldSession);
                return newSession;
        }
        throw new IllegalStateException("shouldn't happen");
    }
}
