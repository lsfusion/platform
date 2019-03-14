package lsfusion.server.physics.admin.service;

import com.google.common.base.Throwables;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.service.*;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WriteDefaultSettingsActionProperty extends ScriptingAction {

    public WriteDefaultSettingsActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            Settings settings = Settings.get();
            Field[] attributes = settings.getClass().getDeclaredFields();

            List<List<Object>> data = new ArrayList<>();
            for (Field field : attributes) {
                String name = field.getName();
                String value = String.valueOf(PropertyUtils.getSimpleProperty(settings, field.getName()));
                data.add(Arrays.asList((Object) name, value));
            }

            List<ImportProperty<?>> props = new ArrayList<>();
            List<ImportField> fields = new ArrayList<>();
            List<ImportKey<?>> keys = new ArrayList<>();

            ImportField nameSettingField = new ImportField(findProperty("name[Setting]"));
            ImportKey<?> settingKey = new ImportKey((CustomClass) findClass("Setting"),
                    findProperty("setting[VARSTRING[100]]").getMapping(nameSettingField));
            keys.add(settingKey);
            props.add(new ImportProperty(nameSettingField, findProperty("name[Setting]").getMapping(settingKey)));
            fields.add(nameSettingField);

            ImportField defaultValueSettingField = new ImportField(findProperty("defaultValue[Setting]"));
            props.add(new ImportProperty(defaultValueSettingField, findProperty("defaultValue[Setting]").getMapping(settingKey)));
            fields.add(defaultValueSettingField);

            ImportTable table = new ImportTable(fields, data);

            try (ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {
                IntegrationService service = new IntegrationService(newContext, table, keys, props);
                service.synchronize(true, false);
                newContext.apply();
            }

        } catch (InvocationTargetException | NoSuchMethodException | ScriptingErrorLog.SemanticErrorException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
}
