package lsfusion.server.data.table;

import lsfusion.server.data.sql.SQLSession;

public interface RegisterChange {

    RegisterChange VOID = new RegisterChange() {
        public void register(SQLSession sql, int result) {

        }
    };

    void register(SQLSession sql, int result);
}
