package lsfusion.server.data;

import java.sql.SQLException;

public abstract class FillTemporaryTable {

    public abstract Integer fill(String name) throws SQLException, SQLHandledException;

    public boolean canBeNotEmptyIfFailed() {
        return false;
    }
}
