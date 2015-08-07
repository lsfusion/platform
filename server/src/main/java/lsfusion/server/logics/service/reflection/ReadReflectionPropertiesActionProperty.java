package lsfusion.server.logics.service.reflection;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.Settings;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.integration.*;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;

import static org.apache.commons.lang.StringUtils.trimToNull;

public class ReadReflectionPropertiesActionProperty extends ScriptingActionProperty {

    public ReadReflectionPropertiesActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            Map<String, String> savedReflectionProperties = readSavedReflectionProperties(context);

            Settings settings = Settings.get();
            Field[] attributes = settings.getClass().getDeclaredFields();

            List<List<Object>> data = new ArrayList<>();
            for (Field field : attributes) {
                String name = field.getName();
                String value = String.valueOf(PropertyUtils.getSimpleProperty(settings, field.getName()));
                data.add(Arrays.asList((Object) name, value));
                String savedValue = savedReflectionProperties.get(name);
                if(savedValue != null)
                    SaveReflectionPropertyActionProperty.setPropertyValue(name, savedValue);
            }

            List<ImportProperty<?>> props = new ArrayList<>();
            List<ImportField> fields = new ArrayList<>();
            List<ImportKey<?>> keys = new ArrayList<>();

            ImportField nameReflectionPropertyField = new ImportField(findProperty("nameReflectionProperty"));
            ImportKey<?> reflectionPropertyKey = new ImportKey((CustomClass) findClass("ReflectionProperty"),
                    findProperty("reflectionPropertyName").getMapping(nameReflectionPropertyField));
            keys.add(reflectionPropertyKey);
            props.add(new ImportProperty(nameReflectionPropertyField, findProperty("nameReflectionProperty").getMapping(reflectionPropertyKey)));
            fields.add(nameReflectionPropertyField);

            ImportField defaultValueReflectionPropertyField = new ImportField(findProperty("defaultValueReflectionProperty"));
            props.add(new ImportProperty(defaultValueReflectionPropertyField, findProperty("defaultValueReflectionProperty").getMapping(reflectionPropertyKey)));
            fields.add(defaultValueReflectionPropertyField);

            ImportTable table = new ImportTable(fields, data);

            try (DataSession session = context.createSession()) {
                session.pushVolatileStats("RP_R");
                IntegrationService service = new IntegrationService(session, table, keys, props);
                service.synchronize(true, false);
                session.apply(context);
                session.popVolatileStats();
            }

        } catch (InvocationTargetException | NoSuchMethodException | ScriptingErrorLog.SemanticErrorException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }

    private Map<String, String> readSavedReflectionProperties(ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        KeyExpr reflectionPropertyExpr = new KeyExpr("reflectionProperty");
        ImRevMap<Object, KeyExpr> reflectionPropertyKeys = MapFact.singletonRev((Object) "reflectionProperty", reflectionPropertyExpr);

        QueryBuilder<Object, Object> reflectionPropertyQuery = new QueryBuilder<Object, Object>(reflectionPropertyKeys);
        reflectionPropertyQuery.addProperty("nameReflectionProperty", findProperty("nameReflectionProperty").getExpr(reflectionPropertyExpr));
        reflectionPropertyQuery.addProperty("baseValueReflectionProperty", findProperty("baseValueReflectionProperty").getExpr(reflectionPropertyExpr));
        reflectionPropertyQuery.and(findProperty("nameReflectionProperty").getExpr(reflectionPropertyExpr).getWhere());
        reflectionPropertyQuery.and(findProperty("baseValueReflectionProperty").getExpr(reflectionPropertyExpr).getWhere());

        Map<String, String> reflectionPropertiesMap = new HashMap<>();
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> receiptDetailResult = reflectionPropertyQuery.execute(context);
        for (ImMap<Object, Object> receiptDetailValues : receiptDetailResult.valueIt()) {
            String nameReflectionProperty = trimToNull((String) receiptDetailValues.get("nameReflectionProperty"));
            String baseValueReflectionProperty = trimToNull((String) receiptDetailValues.get("baseValueReflectionProperty"));
            reflectionPropertiesMap.put(nameReflectionProperty, baseValueReflectionProperty);
        }
        return reflectionPropertiesMap;
    }
}
