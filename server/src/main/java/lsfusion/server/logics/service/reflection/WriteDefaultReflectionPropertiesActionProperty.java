package lsfusion.server.logics.service.reflection;

import com.google.common.base.Throwables;
import lsfusion.server.Settings;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.integration.*;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingModuleErrorLog;
import lsfusion.server.session.DataSession;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WriteDefaultReflectionPropertiesActionProperty extends ScriptingActionProperty {

    public WriteDefaultReflectionPropertiesActionProperty(ServiceLogicsModule LM) {
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

            ImportField nameReflectionPropertyField = new ImportField(findProperty("name[ReflectionProperty]"));
            ImportKey<?> reflectionPropertyKey = new ImportKey((CustomClass) findClass("ReflectionProperty"),
                    findProperty("reflectionProperty[VARSTRING[100]]").getMapping(nameReflectionPropertyField));
            keys.add(reflectionPropertyKey);
            props.add(new ImportProperty(nameReflectionPropertyField, findProperty("name[ReflectionProperty]").getMapping(reflectionPropertyKey)));
            fields.add(nameReflectionPropertyField);

            ImportField defaultValueReflectionPropertyField = new ImportField(findProperty("defaultValue[ReflectionProperty]"));
            props.add(new ImportProperty(defaultValueReflectionPropertyField, findProperty("defaultValue[ReflectionProperty]").getMapping(reflectionPropertyKey)));
            fields.add(defaultValueReflectionPropertyField);

            ImportTable table = new ImportTable(fields, data);

            try (DataSession session = context.createSession()) {
                session.pushVolatileStats("RP_R");
                IntegrationService service = new IntegrationService(session, table, keys, props);
                service.synchronize(true, false);
                session.apply(context);
                session.popVolatileStats();
            }

        } catch (InvocationTargetException | NoSuchMethodException | ScriptingModuleErrorLog.SemanticError | IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
}
