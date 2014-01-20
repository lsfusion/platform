package lsfusion.server.logics.service;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.SessionCreator;

import java.sql.SQLException;

public interface RunServiceData {
    void run(SessionCreator session, boolean isolatedTransaction) throws SQLException, SQLHandledException;
}
