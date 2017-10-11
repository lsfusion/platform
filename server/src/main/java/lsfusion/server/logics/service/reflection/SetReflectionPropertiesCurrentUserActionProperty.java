package lsfusion.server.logics.service.reflection;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.Settings;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
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

public class SetReflectionPropertiesCurrentUserActionProperty extends ScriptingActionProperty {

    public SetReflectionPropertiesCurrentUserActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            Map<String, String> savedReflectionProperties = readSavedReflectionProperties(context);

            Settings settings = ThreadLocalContext.getUserSettings();
            Field[] attributes = settings.getClass().getDeclaredFields();

            for (Field field : attributes) {
                String name = field.getName();
                String savedValue = savedReflectionProperties.get(name);
                if(savedValue != null)
                    SaveReflectionPropertyActionProperty.setPropertyValue(settings, name, savedValue);
            }

        } catch (InvocationTargetException | NoSuchMethodException | ScriptingErrorLog.SemanticErrorException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> readSavedReflectionProperties(ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        KeyExpr reflectionPropertyExpr = new KeyExpr("reflectionProperty");
        ImRevMap<Object, KeyExpr> reflectionPropertyKeys = MapFact.singletonRev((Object) "reflectionProperty", reflectionPropertyExpr);

        QueryBuilder<Object, Object> reflectionPropertyQuery = new QueryBuilder<>(reflectionPropertyKeys);
        reflectionPropertyQuery.addProperty("name", findProperty("name[ReflectionProperty]").getExpr(reflectionPropertyExpr));
        reflectionPropertyQuery.addProperty("baseValue", findProperty("baseValueCurrentUser[ReflectionProperty]").getExpr(reflectionPropertyExpr));
        reflectionPropertyQuery.and(findProperty("name[ReflectionProperty]").getExpr(reflectionPropertyExpr).getWhere());
        reflectionPropertyQuery.and(findProperty("baseValueCurrentUser[ReflectionProperty]").getExpr(reflectionPropertyExpr).getWhere());

        Map<String, String> reflectionPropertiesMap = new HashMap<>();
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> receiptDetailResult = reflectionPropertyQuery.execute(context);
        for (ImMap<Object, Object> receiptDetailValues : receiptDetailResult.valueIt()) {
            String name = trimToNull((String) receiptDetailValues.get("name"));
            String baseValue = trimToNull((String) receiptDetailValues.get("baseValue"));
            reflectionPropertiesMap.put(name, baseValue);
        }
        return reflectionPropertiesMap;
    }
}