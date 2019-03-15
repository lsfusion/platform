package lsfusion.server.logics.navigator.controller.env;

import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.sql.exception.SQLHandledException;

import java.sql.SQLException;

public interface UserController {

    boolean changeCurrentUser(DataObject user, ExecutionStack stack) throws SQLException, SQLHandledException;
    Long getCurrentUserRole();
}
