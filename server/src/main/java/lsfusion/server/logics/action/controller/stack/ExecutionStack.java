package lsfusion.server.logics.action.controller.stack;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.Processor;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;

import java.sql.SQLException;
import java.util.List;

public interface ExecutionStack {

    ImMap<String, String> getAllParamsWithClassesInStack();

    ImMap<String, ObjectValue> getAllParamsWithValuesInStack();

    ImSet<Pair<LP, List<ResolveClassSet>>> getAllLocalsInStack();

    Processor<ImMap<String, ObjectValue>> getWatcher();

    boolean hasNewDebugStack();

    void updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException;

    boolean sameSession(UpdateCurrentClassesSession session);
}
