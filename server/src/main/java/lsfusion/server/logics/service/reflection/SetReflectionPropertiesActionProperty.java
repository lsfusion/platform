package lsfusion.server.logics.service.reflection;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.Settings;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.trimToNull;

public class SetReflectionPropertiesActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface userRoleInterface;

    public SetReflectionPropertiesActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        userRoleInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        ObjectValue userRoleObject = context.getKeyValue(userRoleInterface);

        try {

            Map<String, String> savedReflectionProperties = readSavedReflectionProperties(context, userRoleObject);

            Settings settings = ThreadLocalContext.createRoleSettings((Long) userRoleObject.getValue());

            Field[] attributes = settings.getClass().getDeclaredFields();
            for (Field field : attributes) {
                String name = field.getName();
                String savedValue = savedReflectionProperties.get(name);
                if (savedValue != null)
                    SaveReflectionPropertyActionProperty.setPropertyValue(settings, name, savedValue);
            }

        } catch (InvocationTargetException | NoSuchMethodException | ScriptingErrorLog.SemanticErrorException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> readSavedReflectionProperties(ExecutionContext context, ObjectValue userRoleObject) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        KeyExpr reflectionPropertyExpr = new KeyExpr("reflectionProperty");
        ImRevMap<Object, KeyExpr> reflectionPropertyKeys = MapFact.singletonRev((Object) "reflectionProperty", reflectionPropertyExpr);

        QueryBuilder<Object, Object> reflectionPropertyQuery = new QueryBuilder<>(reflectionPropertyKeys);
        Expr nameExpr = findProperty("name[ReflectionProperty]").getExpr(reflectionPropertyExpr);
        Expr baseValueExpr = findProperty("overBaseValue[ReflectionProperty, UserRole]").getExpr(reflectionPropertyExpr, userRoleObject.getExpr());

        reflectionPropertyQuery.addProperty("name", nameExpr);
        reflectionPropertyQuery.addProperty("baseValue", baseValueExpr);
        reflectionPropertyQuery.and(nameExpr.getWhere());
        reflectionPropertyQuery.and(baseValueExpr.getWhere());

        Map<String, String> reflectionPropertiesMap = new HashMap<>();
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> receiptDetailResult = reflectionPropertyQuery.execute(context);
        for (ImMap<Object, Object> receiptDetailValues : receiptDetailResult.valueIt()) {
            String name = trimToNull((String) receiptDetailValues.get("name"));
            String baseValue = trimToNull((String) receiptDetailValues.get("baseValue"));
            reflectionPropertiesMap.put(name, baseValue);
        }
        return reflectionPropertiesMap;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}