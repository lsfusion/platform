package lsfusion.server.logics.service;

import com.google.common.base.Throwables;
import lsfusion.server.Settings;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class SaveValueReflectionSettingActionProperty extends ScriptingActionProperty {

    public SaveValueReflectionSettingActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            String nameReflectionSetting = (String) findProperty("nameReflectionSetting").read(context);
            String newValue = (String) findProperty("valueReflectionSetting").read(context);

            if(nameReflectionSetting != null && newValue != null) {
                
                String oldValue = BeanUtils.getProperty(Settings.get(), nameReflectionSetting);

                if(oldValue != null && !oldValue.equals(newValue)) {
                    BeanUtils.setProperty(Settings.get(), nameReflectionSetting, newValue);
                }
            }
            

        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException e) {
            throw Throwables.propagate(e);
        }


    }
}
