package lsfusion.server.data;

import java.sql.Connection;
import java.sql.SQLException;

public class ExConnection {
    public Connection sql;
    public SQLTemporaryPool temporary;

    public ExConnection(Connection sql, SQLTemporaryPool temporary) {
        this.sql = sql;
        this.temporary = temporary;
    }
    
    public void close() throws SQLException {
        sql.close();
    }
}
