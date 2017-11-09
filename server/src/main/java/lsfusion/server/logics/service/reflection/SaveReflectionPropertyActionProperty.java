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
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Iterator;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;
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

            if(nameReflectionProperty != null && valueReflectionProperty != null) {

                Long userRole = (Long) userRoleObject.getValue();
                Settings settings = ThreadLocalContext.getRoleSettings(userRole);
                if(settings != null) {

                    String oldValue = BeanUtils.getProperty(settings, nameReflectionProperty);

                    if (!oldValue.equals(valueReflectionProperty)) {
                        setPropertyValue(settings, nameReflectionProperty, valueReflectionProperty);
                    }
                }
            }
            

        } catch (ScriptingErrorLog.SemanticErrorException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | CloneNotSupportedException e) {
            throw Throwables.propagate(e);
        }

    }

    public static void setPropertyValue(Settings settings, String nameProperty, String valueProperty) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class type = PropertyUtils.getPropertyType(settings, nameProperty);
        if(type == Boolean.TYPE)
            BeanUtils.setProperty(settings, nameProperty, valueProperty.equals("true"));
        else if(type == Integer.TYPE)
            BeanUtils.setProperty(settings, nameProperty, Integer.valueOf(valueProperty));
        else if(type == Double.TYPE)
            BeanUtils.setProperty(settings, nameProperty, Double.valueOf(valueProperty));
        else if(type == Long.TYPE)
            BeanUtils.setProperty(settings, nameProperty, Long.valueOf(valueProperty));
        else
            BeanUtils.setProperty(settings, nameProperty, trimToEmpty(valueProperty));
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
