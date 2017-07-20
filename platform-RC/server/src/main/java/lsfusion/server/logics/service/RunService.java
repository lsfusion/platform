package lsfusion.server.logics.service;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.BusinessLogics;

import java.sql.SQLException;

public interface RunService {
    void run(SQLSession session, boolean isolatedTransaction) throws SQLException, SQLHandledException;        
}
