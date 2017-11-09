package lsfusion.server.logics.service.reflection;

import com.google.common.base.Throwables;
import lsfusion.server.Settings;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Map;

public class SetReflectionPropertiesActionProperty extends ScriptingActionProperty {

    public SetReflectionPropertiesActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            Map<String, String> savedReflectionProperties = ThreadLocalContext.readSavedReflectionProperties(context.getSession(), NullValue.instance);

            Settings settings = Settings.get();

            Field[] attributes = settings.getClass().getDeclaredFields();
            for (Field field : attributes) {
                String name = field.getName();
                String savedValue = savedReflectionProperties.get(name);
                if (savedValue != null)
                    ThreadLocalContext.setPropertyValue(settings, name, savedValue);
            }

        } catch (InvocationTargetException | NoSuchMethodException | ScriptingErrorLog.SemanticErrorException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
}