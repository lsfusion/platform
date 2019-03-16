package lsfusion.server.physics.admin.service.action;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;

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