package lsfusion.server.logics.property.actions.importing;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ImportFormPlainDataActionProperty<I> extends ImportFormDataActionProperty {

    private Map<Object, Integer> tagsMap;
    private Map<Object, Integer> keyTagsMap;

    public abstract List<Pair<String, I>> getRootElements(Map<String, byte[]> files, Map<String, List<String>> headersMap) throws IOException;

    public abstract ImportFormIterator getIterator(List<Pair<String, I>> rootElements);

    public abstract String getChildValue(Object child);

    public ImportFormPlainDataActionProperty(ValueClass[] valueClasses, FormEntity formEntity) {
        super(valueClasses, formEntity);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            Map<String, byte[]> files = getFiles(context);

            if (files != null) {
                importData(context, files);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    @Override
    protected Map<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> getData(Object files, Map<String, Pair<List<String>, CalcProperty>> propertyKeysMap, Map<String, List<String>> headersMap) throws IOException, ParseException {
        List<Pair<String, I>> rootElements = getRootElements((Map<String, byte[]>) files, headersMap);
        tagsMap = new HashMap<>();
        keyTagsMap = new HashMap<>();
        return getData(rootElements, propertyKeysMap);
    }

    private Map<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> getData(
            List<Pair<String, I>> rootElements, Map<String, Pair<List<String>, CalcProperty>> propertyKeysMap) throws ParseException {
        Map<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> dataMap = new HashMap<>();
        ImportFormIterator iterator = getIterator(rootElements);
        Pair<String, Object> child;
        while ((child = iterator.next()) != null) {

            if(child.second instanceof String) {
                Integer value = parseInt((String) child.second);
                if(value != null)
                    keyTagsMap.put(child.first, value);
            }
            Integer count = tagsMap.get(child.first);
            tagsMap.put(child.first, count == null ? 0 : ++count);

            Pair<List<String>, CalcProperty> entry = propertyKeysMap.get(child.first);
            if (entry != null && (!getKeysId(entry.first).equals(child.first) || child.second instanceof ImportIterator)) {
                String keyId = getKeysId(entry.first);
                Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> dataEntry = dataMap.get(keyId);
                if (dataEntry == null)
                    dataEntry = new HashMap<>();
                ImMap<KeyField, DataObject> key = getKeys(entry.first);
                Map<Property, ObjectValue> properties = dataEntry.get(key);
                if (properties == null)
                    properties = new HashMap<>();
                String childValue = getChildValue(child.second);
                properties.put(entry.second, childValue == null ? NullValue.instance : new DataObject(entry.second.getType().parseString(childValue), (ConcreteClass) entry.second.getType()));
                Map<Property, ObjectValue> propertiesEntry = dataEntry.get(key);
                if(propertiesEntry == null)
                    propertiesEntry = new HashMap<>();
                propertiesEntry.putAll(properties);
                dataEntry.put(key, propertiesEntry);
                Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> dataMapEntry = dataMap.get(keyId);
                if(dataMapEntry == null)
                    dataMapEntry = new HashMap<>();
                dataMapEntry.putAll(dataEntry);
                dataMap.put(keyId, dataMapEntry);
            }
        }
        return dataMap;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private ImMap<KeyField, DataObject> getKeys(List<String> keys) {
        ImMap<KeyField, DataObject> keyObjects = MapFact.EMPTY();
        int i = 0;
        for (String key : keys) {
            Integer value = keyTagsMap.containsKey(key) ? keyTagsMap.get(key) : tagsMap.get(key);
            keyObjects = keyObjects.addExcl(new KeyField(key, ImportDataActionProperty.type), new DataObject(value));
            i++;
        }
        return keyObjects;
    }

    private Map<String, byte[]> getFiles(ExecutionContext context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        Map<String, byte[]> files = new HashMap<>();

        KeyExpr stringExpr = new KeyExpr("string");
        ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "string", stringExpr);
        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
        query.addProperty("importFiles", context.getBL().LM.findProperty("importFiles[VARSTRING[100]]").getExpr(context.getModifier(), stringExpr));
        query.and(context.getBL().LM.findProperty("importFiles[VARSTRING[100]]").getExpr(context.getModifier(), stringExpr).getWhere());
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(context);
        for (int i = 0; i < result.size(); i++) {
            String fileKey = (String) result.getKey(i).get("string");
            byte[] file = BaseUtils.getFile((byte[]) result.getValue(i).get("importFiles"));
            files.put(fileKey, file);

        }
        return files;
    }
}