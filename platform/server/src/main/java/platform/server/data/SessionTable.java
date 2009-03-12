package platform.server.data;

import platform.server.data.sql.SQLSyntax;

// временная таблица на момент сессии
public class SessionTable extends Table {

    protected SessionTable(String iName) {
        super(iName);
    }

    public String getName(SQLSyntax Syntax) {
        return Syntax.getSessionTableName(name);
    }

}
