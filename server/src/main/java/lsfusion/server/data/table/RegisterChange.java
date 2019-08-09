package lsfusion.server.data.table;

import lsfusion.server.data.sql.SQLSession;

public interface RegisterChange {

    RegisterChange VOID = (sql, result) -> {

    };

    void register(SQLSession sql, int result);
}
