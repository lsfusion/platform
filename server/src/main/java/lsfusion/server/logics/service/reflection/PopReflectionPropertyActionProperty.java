package lsfusion.server.logics.service.reflection;

import com.google.common.base.Throwables;
import lsfusion.server.WrapperSettings;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class PopReflectionPropertyActionProperty extends ScriptingActionProperty {

    public PopReflectionPropertyActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }
    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            WrapperSettings.popSettings();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }

    }
}