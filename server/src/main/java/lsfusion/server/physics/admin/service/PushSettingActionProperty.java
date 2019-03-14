package lsfusion.server.physics.admin.service;

import com.google.common.base.Throwables;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Iterator;

public class PushSettingActionProperty extends ScriptingAction {
    private ClassPropertyInterface nameInterface;
    private ClassPropertyInterface valueInterface;

    public PushSettingActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        nameInterface = i.next();
        valueInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            String name = (String) context.getDataKeyValue(nameInterface).getValue();
            String value = (String) context.getDataKeyValue(valueInterface).getValue();

            ThreadLocalContext.pushSettings(name, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | CloneNotSupportedException e) {
            throw Throwables.propagate(e);
        }

    }
}