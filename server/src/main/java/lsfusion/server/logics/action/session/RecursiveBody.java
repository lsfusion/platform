package lsfusion.server.logics.action.session;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;

import java.sql.SQLException;

// a body deferred into the apply recursion (see DataSession.addRecursion) : a deferred lsf NEW SESSION body (ActionValueImplement)
// or a java one (NewSessionBody, see ExecutionContext.newSession(body))
public interface RecursiveBody {

    Action<?> getSingleApplyAction(); // the action the single apply machinery works with (see DataSession.startPendingSingles), null for the java bodies

    void execute(ExecutionEnvironment env, ExecutionStack stack) throws SQLException, SQLHandledException;

    RecursiveBody updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException; // the keys have to be class-updated between the recursion rounds
}
