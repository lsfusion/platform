package lsfusion.server.logics.service.reflection;

import com.google.common.base.Throwables;
import lsfusion.server.Settings;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
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
    private ClassPropertyInterface reflectionPropertyInterface;

    public SaveReflectionPropertyActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        reflectionPropertyInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataObject reflectionPropertyObject = context.getDataKeyValue(reflectionPropertyInterface);

            String nameReflectionProperty = trimToNull((String) findProperty("name[ReflectionProperty]").read(context, reflectionPropertyObject));
            String valueReflectionProperty = trimToNull((String) findProperty("value[ReflectionProperty]").read(context, reflectionPropertyObject));

            if(nameReflectionProperty != null && valueReflectionProperty != null) {
                
                String oldValue = BeanUtils.getProperty(Settings.get(), nameReflectionProperty);

                if(!oldValue.equals(valueReflectionProperty)) {
                    setPropertyValue(nameReflectionProperty, valueReflectionProperty);
                }
            }
            

        } catch (ScriptingErrorLog.SemanticErrorException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }

    }

    public static void setPropertyValue(String nameProperty, String valueProperty) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class type = PropertyUtils.getPropertyType(Settings.get(), nameProperty);
        if(type == Boolean.TYPE)
            BeanUtils.setProperty(Settings.get(), nameProperty, valueProperty.equals("true"));
        else if(type == Integer.TYPE)
            BeanUtils.setProperty(Settings.get(), nameProperty, Integer.valueOf(valueProperty));
        else if(type == Double.TYPE)
            BeanUtils.setProperty(Settings.get(), nameProperty, Double.valueOf(valueProperty));
        else if(type == Long.TYPE)
            BeanUtils.setProperty(Settings.get(), nameProperty, Long.valueOf(valueProperty));
        else
            BeanUtils.setProperty(Settings.get(), nameProperty, trimToEmpty(valueProperty));
    }
}
