package lsfusion.server.physics.admin.service;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;

import java.sql.SQLException;

public interface RunService {
    void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException;        
}
