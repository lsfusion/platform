package lsfusion.server.data.table;

import lsfusion.server.data.sql.exception.SQLHandledException;

import java.sql.SQLException;

public abstract class FillTemporaryTable {

    public abstract Long fill(String name) throws SQLException, SQLHandledException;

    public boolean canBeNotEmptyIfFailed() {
        return false;
    }
}
