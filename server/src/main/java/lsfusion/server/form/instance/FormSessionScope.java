package lsfusion.server.form.instance;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public enum FormSessionScope {
    OLDSESSION, NEWSESSION, NESTEDSESSION;

    public boolean isNewSession() {
        return this == NEWSESSION || this == NESTEDSESSION;
    }
    public boolean isNestedSession() {
        return this == NESTEDSESSION;
    }
}
