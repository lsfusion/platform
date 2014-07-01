package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.sql.SQLException;
import java.util.Iterator;

public class CancelActiveJavaThreadActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface integerInterface;

    public CancelActiveJavaThreadActionProperty(ScriptingLogicsModule LM) {
        super(LM, IntegerClass.instance);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            getActiveThreadsFromDatabase(context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }

    }


    private void getActiveThreadsFromDatabase(ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        DataObject currentObject = context.getDataKeyValue(integerInterface);
        Integer id = (Integer) getLCP("idActiveJavaThread").read(context, currentObject);
        ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(id);
        if(threadInfo != null) {
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (thread.getName().equals(threadInfo.getThreadName()))
                    thread.stop();
            }
        }
        getLAP("getActiveJavaThreadsAction").execute(context);
    }
}
                     