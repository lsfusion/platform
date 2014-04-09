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

public class ReadValueReflectionSettingActionProperty extends ScriptingActionProperty {

    public ReadValueReflectionSettingActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            String nameReflectionSetting = (String) getLCP("nameReflectionSetting").read(context);

            if (nameReflectionSetting != null) {
                String valueReflectionSetting = BeanUtils.getProperty(Settings.get(), nameReflectionSetting);

                if (valueReflectionSetting != null) {
                    getLCP("valueReflectionSetting").change(String.valueOf(valueReflectionSetting), context);
                }

            }


        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        } catch (NoSuchMethodException e) {
            try {
                getLCP("valueReflectionSetting").change((Object) null, context);
            } catch (ScriptingErrorLog.SemanticErrorException ignore) {
            }
        } catch (InvocationTargetException e) {
            throw Throwables.propagate(e);
        }


    }
}
