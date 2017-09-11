package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.Settings;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.Iterator;

public class ChangeProfilingSettingsActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface keyInterface;
    private final ClassPropertyInterface valueInterface;

    public ChangeProfilingSettingsActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        keyInterface = i.next();
        valueInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        String key = (String) context.getKeyValue(keyInterface).getValue();
        Object value = context.getKeyValue(valueInterface).getValue();

        try {
            ObjectValue reflectionProperty = findProperty("reflectionProperty[VARISTRING[100]]").readClasses(context, new DataObject(key));

            if(reflectionProperty instanceof DataObject) {
                try(DataSession session = context.createSession()) {
                    findProperty("baseValue[ReflectionProperty]").change(value == null ? null : String.valueOf(value), session, (DataObject) reflectionProperty);
                    session.apply(context);
                }
            }

            if(key != null && key.equals("explainNoAnalyze")) {
                Settings.get().setExplainNoAnalyze(value != null);
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
