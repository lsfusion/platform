package lsfusion.server.physics.admin.service;

import com.google.common.base.Throwables;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Iterator;

public class PopSettingActionProperty extends ScriptingAction {
    private ClassPropertyInterface nameInterface;

    public PopSettingActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        nameInterface = i.next();
    }
    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            String name = (String) context.getDataKeyValue(nameInterface).getValue();
            ThreadLocalContext.popSettings(name);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }

    }
}