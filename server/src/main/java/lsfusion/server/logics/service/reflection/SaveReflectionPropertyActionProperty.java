package lsfusion.server.logics.service.reflection;

import com.google.common.base.Throwables;
import lsfusion.server.Settings;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Iterator;

import static org.apache.commons.lang3.StringUtils.trimToNull;

public class SaveReflectionPropertyActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface reflectionPropertyInterface;
    private final ClassPropertyInterface userRoleInterface;

    public SaveReflectionPropertyActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        reflectionPropertyInterface = i.next();
        userRoleInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            ObjectValue reflectionPropertyObject = context.getKeyValue(reflectionPropertyInterface);
            ObjectValue userRoleObject = context.getKeyValue(userRoleInterface);

            String nameReflectionProperty = trimToNull((String) findProperty("name[ReflectionProperty]").read(context, reflectionPropertyObject));
            String valueReflectionProperty = trimToNull((String) findProperty("value[ReflectionProperty, UserRole]").read(context, reflectionPropertyObject, userRoleObject));

            Settings settings = ThreadLocalContext.getRoleSettings((Long) userRoleObject.getValue());
            ThreadLocalContext.setPropertyValue(settings, nameReflectionProperty, valueReflectionProperty);

        } catch (ScriptingErrorLog.SemanticErrorException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | CloneNotSupportedException e) {
            throw Throwables.propagate(e);
        }

    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
