package lsfusion.server.logics.property.actions.importing;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ImportFormHierarchicalDataActionProperty<E> extends ImportFormDataActionProperty {

    private Map<String, Integer> tagsMap;

    public abstract E getRootElement(byte[] file);

    public abstract ImportFormIterator getIterator(Pair<String, Object> rootElement);

    public abstract String getChildValue(Object child);

    public abstract boolean isLeaf(Object child);

    public ImportFormHierarchicalDataActionProperty(ValueClass[] valueClasses, FormEntity formEntity) {
        super(valueClasses, formEntity);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            byte[] file = (byte[]) context.getBL().LM.findProperty("System.formImportFile[]").read(context);
            if (file != null) {
                file = BaseUtils.getFile(file);
                importData(context, file);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }

    }

    @Override
    protected Map<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> getData(Object file, Map<String, Pair<List<String>, CalcProperty>> propertyKeysMap, Map<String, List<String>> headersMap) throws IOException, ParseException {
        E rootElement = getRootElement((byte[]) file);
        tagsMap = new HashMap<>();
        return getData(Pair.create((String) null, (Object) rootElement), propertyKeysMap);
    }

    private Map<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> getData(
            Pair<String, Object> rootElement, Map<String, Pair<List<String>, CalcProperty>> propertyKeysMap) {
        Map<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> dataMap = new HashMap<>();
        ImportFormIterator iterator = getIterator(rootElement);
        while (iterator.hasNext()) {
            Pair<String, Object> child = iterator.next();

            Integer count = tagsMap.get(child.first);
            tagsMap.put(child.first, count == null ? 0 : ++count);

            Pair<List<String>, CalcProperty> entry = propertyKeysMap.get(child.first);
            if (entry != null && (!getKeysId(entry.first).equals(child.first) || child.second instanceof JSONObject)) {
                String keyId = getKeysId(entry.first);
                Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> dataEntry = dataMap.get(keyId);
                if (dataEntry == null)
                    dataEntry = new HashMap<>();
                ImMap<KeyField, DataObject> key = getKeys(entry.first);
                Map<Property, ObjectValue> properties = dataEntry.get(key);
                if (properties == null)
                    properties = new HashMap<>();
                properties.put(entry.second, isLeaf(child.second) ? new DataObject(getChildValue(child.second), (ConcreteClass) entry.second.getType()) : NullValue.instance);
                dataEntry.put(key, properties);
                dataMap.put(keyId, dataEntry);
            }

            for (Map.Entry<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> childDataEntry : getData(child, propertyKeysMap).entrySet()) {
                Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> data = dataMap.get(childDataEntry.getKey());
                if (data == null)
                    data = new HashMap<>();
                data.putAll(childDataEntry.getValue());
                dataMap.put(childDataEntry.getKey(), data);
            }

        }
        return dataMap;
    }

    private ImMap<KeyField, DataObject> getKeys(List<String> keys) {
        ImMap<KeyField, DataObject> keyObjects = MapFact.EMPTY();
        int i = 0;
        for (String key : keys) {
            keyObjects = keyObjects.addExcl(new KeyField(key, IntegerClass.instance), new DataObject(tagsMap.get(key)));
            i++;
        }
        return keyObjects;
    }
}