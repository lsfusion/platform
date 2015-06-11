package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.sql.SQLException;
import java.util.Iterator;

public class CancelJavaProcessActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface integerInterface;

    public CancelJavaProcessActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        integerInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            DataObject currentObject = context.getDataKeyValue(integerInterface);
            Integer id = Integer.parseInt((String) findProperty("idThreadProcess").read(context, currentObject));
            ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(id);
            if (threadInfo != null) {
                for (Thread thread : Thread.getAllStackTraces().keySet()) {
                    if (thread.getName().equals(threadInfo.getThreadName()))
                        thread.interrupt();
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
                     