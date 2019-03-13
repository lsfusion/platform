package lsfusion.server.physics.admin.service;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.session.SessionCreator;

import java.sql.SQLException;

public interface RunServiceData {
    void run(SessionCreator session, boolean isolatedTransaction) throws SQLException, SQLHandledException;
}
