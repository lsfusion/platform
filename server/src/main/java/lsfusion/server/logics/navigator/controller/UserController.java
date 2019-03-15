package lsfusion.server.logics.navigator.controller;

import lsfusion.server.logics.action.stack.ExecutionStack;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;

import java.sql.SQLException;

public interface UserController {

    boolean changeCurrentUser(DataObject user, ExecutionStack stack) throws SQLException, SQLHandledException;
    Long getCurrentUserRole();
}
