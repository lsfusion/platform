package lsfusion.server.logics.property.actions.importing;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.*;

public abstract class ImportFormHierarchicalDataActionProperty<E> extends ImportFormDataActionProperty {

    private Map<String, Integer> tagsMap;

    protected String root;

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
            root = context.getKeys().isEmpty() ? null : (String) context.getSingleKeyValue().getValue();
            byte[] file = (byte[]) context.getBL().LM.findProperty("System.importFile[]").read(context);
            if (file != null) {
                file = BaseUtils.getFile(file);
                importData(context, file);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }

    }

    @Override
    protected Map<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> getData(Object file, Map<String, Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity>> propertyKeysMap,
                                                                                                             Set<Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity>> filters, Map<String, List<String>> headersMap) {
        E rootElement = getRootElement((byte[]) file);
        tagsMap = new HashMap<>();
        return getData(Pair.create((String) null, (Object) rootElement), propertyKeysMap, filters);
    }

    private Map<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> getData(
            Pair<String, Object> rootElement, Map<String, Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity>> propertyKeysMap,
            Set<Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity>> filters) {
        Map<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> dataMap = new HashMap<>();
        ImportFormIterator iterator = getIterator(rootElement);
        while (iterator.hasNext()) {
            Pair<String, Object> child = iterator.next();

            Integer count = tagsMap.get(child.first);
            tagsMap.put(child.first, count == null ? 0 : ++count);

            Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity> entry = propertyKeysMap.get(child.first);
            if (entry != null && (!getKeysId(entry.first).equals(child.first) || child.second instanceof JSONObject)) {
                Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> dataEntry = dataMap.get(entry.first);
                if (dataEntry == null)
                    dataEntry = new HashMap<>();
                ImMap<KeyField, DataObject> key = getKeys(entry.first);
                if(!key.isEmpty()) {
                    Map<Property, ObjectValue> properties = dataEntry.get(key);
                    if (properties == null)
                        properties = new HashMap<>();
                    properties.put(entry.second.property, getObjectValue(child.second, (ConcreteClass) entry.second.getType()));
                    dataEntry.put(key, properties);
                    dataMap.put(entry.first, dataEntry);
                }
            }

            for (Map.Entry<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> childDataEntry : getData(child, propertyKeysMap, filters).entrySet()) {
                Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> data = dataMap.get(childDataEntry.getKey());
                if (data == null)
                    data = new HashMap<>();
                data.putAll(childDataEntry.getValue());
                dataMap.put(childDataEntry.getKey(), data);
            }

            if(!isLeaf(child.second)) {
                Map<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> filterValues = getFilterValues(filters, Collections.singletonList(child.first));
                for(Map.Entry<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> filterEntry : filterValues.entrySet()) {
                    ImSet<ObjectEntity> filterKey = filterEntry.getKey();
                    Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> filterValue = filterEntry.getValue();
                    Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> dataEntry = dataMap.get(filterKey);
                    if(dataEntry == null)
                        dataEntry = new HashMap<>();
                    for(Map.Entry<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> filterValueEntry : filterValue.entrySet()) {
                        Map<Property, ObjectValue> innerFilterEntry = dataEntry.get(filterValueEntry.getKey());
                        if(innerFilterEntry == null)
                            innerFilterEntry = new HashMap<>();
                        innerFilterEntry.putAll(filterValueEntry.getValue());
                        dataEntry.put(filterValueEntry.getKey(), innerFilterEntry);
                    }
                    dataMap.put(filterKey, dataEntry);
                }
            }

        }
        return dataMap;
    }

    private ImMap<KeyField, DataObject> getKeys(ImSet<ObjectEntity> keys) {
        ImMap<KeyField, DataObject> keyObjects = MapFact.EMPTY();
        for (ObjectEntity key : keys) {
            if(tagsMap.containsKey(key.getSID())) {
                keyObjects = keyObjects.addExcl(new KeyField(key.getSID(), ImportDataActionProperty.type), new DataObject(tagsMap.get(key.getSID())));
            }
        }
        return keyObjects;
    }

    //todo: переосмыслить и упростить
    private Map<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> getFilterValues(
            Set<Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity>> filters, List<String> allowedKeys) {
        Map<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> filterValuesMap = new HashMap<>();
        for (Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity> filterEntry : filters) {
            Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> dataEntry = new HashMap<>();
            ImMap<KeyField, DataObject> key = getFilterKeys(filterEntry.first, allowedKeys);
            if (!key.isEmpty()) {
                Map<Property, ObjectValue> properties = dataEntry.get(key);
                if (properties == null)
                    properties = new HashMap<>();

                properties.put(filterEntry.second.property, ((CalcProperty) filterEntry.second.property).getDefaultDataObject());
                dataEntry.put(key, properties);
                filterValuesMap.put(filterEntry.first, dataEntry);
            }
        }
        return filterValuesMap;
    }

    private ImMap<KeyField, DataObject> getFilterKeys(ImSet<ObjectEntity> keys, List<String> allowedKeys) {
        ImMap<KeyField, DataObject> keyObjects = MapFact.EMPTY();
        boolean skip = true;
        for (ObjectEntity key : keys) {
            if(tagsMap.containsKey(key.getSID())) {
                if(allowedKeys.contains(key.getSID())) {
                    skip = false;
                }
                keyObjects = keyObjects.addExcl(new KeyField(key.getSID(), ImportDataActionProperty.type), new DataObject(tagsMap.get(key.getSID())));
            } else return MapFact.EMPTY();
        }
        return skip  || keys.size() < allowedKeys.size() ? MapFact.<KeyField, DataObject>EMPTY() : keyObjects;
    }

    private ObjectValue getObjectValue(Object value, ConcreteClass type) {
        try {
            return isLeaf(value) ? new DataObject(type.getType().parseString(getChildValue(value)), type) : NullValue.instance;
        } catch (ParseException e) {
            return NullValue.instance;
        }
    }
}