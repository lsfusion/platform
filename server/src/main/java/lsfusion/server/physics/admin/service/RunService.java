package lsfusion.server.physics.admin.service;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;

import java.sql.SQLException;

public interface RunService {
    void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException;        
}
