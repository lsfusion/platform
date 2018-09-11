package lsfusion.server.logics.property.actions.importing;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.IntegralClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.form.entity.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.actions.ExportActionProperty;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.*;

public abstract class ImportFormHierarchicalDataActionProperty<E> extends ImportFormDataActionProperty {

    private Map<String, List<List<String>>> objectGroups;
    private Map<String, List<List<String>>> propertyGroups;

    private static Map<String, List<List<String>>> calcObjectGroups(FormEntity formEntity) {
        Map<String, List<List<String>>> formObjectGroups = new HashMap<>();
        for(GroupObjectEntity formGroup : formEntity.getGroupsIt()) {
            List<String> groups = ExportActionProperty.getParentExportGroups(formGroup.propertyGroup);
            if(!groups.isEmpty()) {
                String topGroup = groups.get(groups.size() - 1);
                groups = new ArrayList<>(groups.subList(0, groups.size() - 1));
                groups.add(formGroup.getSID());
                List<List<String>> entry = formObjectGroups.get(topGroup);
                if(entry == null)
                    entry = new ArrayList<>();
                entry.add(groups);
                formObjectGroups.put(topGroup, entry);
            }
        }
        return formObjectGroups;
    }

    private static Map<String, List<List<String>>> calcPropertyGroups(FormEntity formEntity) {
        Map<String, List<List<String>>> formPropertyGroups = new HashMap<>();
        for(PropertyDrawEntity<?> formProperty : formEntity.getPropertyDrawsIt()) {
            List<String> groups = ExportActionProperty.getParentExportGroups(formProperty.group);
            if(!groups.isEmpty()) {
                String topGroup = groups.get(groups.size() - 1);
                groups = new ArrayList<>(groups.subList(0, groups.size() - 1));
                groups.add(formProperty.getShortSID());
                List<List<String>> entry = formPropertyGroups.get(topGroup);
                if(entry == null)
                    entry = new ArrayList<>();
                entry.add(groups);
                formPropertyGroups.put(topGroup, entry);
            }
        }
        return formPropertyGroups;
    }

    @ManualLazy
    protected Map<String, List<List<String>>> getObjectGroups() {
        if(objectGroups == null)
            objectGroups = calcObjectGroups(formEntity);
        return objectGroups;
    }
    @ManualLazy
    protected Map<String, List<List<String>>> getPropertyGroups() {
        if(propertyGroups == null)
            propertyGroups = calcPropertyGroups(formEntity);
        return propertyGroups;
    }

    private Map<String, Integer> tagsMap;
    private Set<String> formObjects;

    protected String root;

    public abstract E getRootElement(byte[] file);

    public abstract ImportFormIterator getIterator(Pair<String, Object> rootElement);

    public abstract String getChildValue(Object child);

    public abstract boolean isLeaf(Object child);

    public ImportFormHierarchicalDataActionProperty(ValueClass[] valueClasses, LCP<?> fileProperty, FormEntity formEntity) {
        super(valueClasses, fileProperty, formEntity);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {
            root = context.getKeys().isEmpty() ? null : (String) context.getSingleKeyValue().getValue();
            byte[] file = (byte[]) (fileProperty != null ? fileProperty : context.getBL().LM.findProperty("System.importFile[]")).read(context);
            if (file != null) {
                file = BaseUtils.getFile(file);
                importData(context, file);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        }

    }

    @Override
    protected Map<ImportFormKeys, ImportFormData> getData(Object file, Map<String, Pair<ImportFormKeys, CalcPropertyObjectEntity>> propertyKeysMap,
                                                                                                             Set<Pair<ImportFormKeys, CalcPropertyObjectEntity>> filters, Map<String, List<String>> headersMap) {
        E rootElement = getRootElement((byte[]) file);
        tagsMap = new HashMap<>();
        formObjects = getFormObjects(propertyKeysMap.values());
        return getData(Pair.create((String) null, (Object) rootElement), propertyKeysMap, filters);
    }

    private Map<ImportFormKeys, ImportFormData> getData(
            Pair<String, Object> rootElement, Map<String, Pair<ImportFormKeys, CalcPropertyObjectEntity>> propertyKeysMap,
            Set<Pair<ImportFormKeys, CalcPropertyObjectEntity>> filters) {
        Map<ImportFormKeys, ImportFormData> dataMap = new HashMap<>();
        ImportFormIterator iterator = getIterator(rootElement);
        while (iterator.hasNext()) {
            Pair<String, Object> child = iterator.next();

            Integer count = tagsMap.get(child.first);
            tagsMap.put(child.first, count == null ? 0 : ++count);

            Pair<ImportFormKeys, CalcPropertyObjectEntity> entry = propertyKeysMap.get(child.first);
            //property found OR it's root OR it's form object
            if(entry != null || rootElement.first == null || formObjects.contains(child.first)) {
                if (entry != null && (!entry.first.getKeysId().equals(child.first) || child.second instanceof JSONObject)) {
                    ImportFormData dataEntry = dataMap.get(entry.first);
                    if (dataEntry == null)
                        dataEntry = new ImportFormData();
                    ImMap<KeyField, DataObject> key = getKeys(entry.first);
                    if (!key.isEmpty()) {
                        Map<Property, ObjectValue> properties = dataEntry.get(key);
                        if (properties == null)
                            properties = new HashMap<>();
                        properties.put(entry.second.property, getObjectValue(child.second, (ConcreteClass) entry.second.getType()));
                        dataEntry.put(key, properties);
                        dataMap.put(entry.first, dataEntry);
                    }
                }

                for (Map.Entry<ImportFormKeys, ImportFormData> childDataEntry : getData(child, propertyKeysMap, filters).entrySet()) {
                    ImportFormData data = dataMap.get(childDataEntry.getKey());
                    dataMap.put(childDataEntry.getKey(), merge(data, childDataEntry.getValue()));
                }

                if (!isLeaf(child.second)) {
                    Map<ImportFormKeys, ImportFormData> filterValues = getFilterValues(filters, child.first);
                    for (Map.Entry<ImportFormKeys, ImportFormData> filterEntry : filterValues.entrySet()) {
                        ImportFormKeys filterKey = filterEntry.getKey();
                        ImportFormData filterValue = filterEntry.getValue();
                        ImportFormData dataEntry = dataMap.get(filterKey);
                        if (dataEntry == null)
                            dataEntry = new ImportFormData();
                        for (Map.Entry<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> filterValueEntry : filterValue.getData().entrySet()) {
                            Map<Property, ObjectValue> innerFilterEntry = dataEntry.get(filterValueEntry.getKey());
                            if (innerFilterEntry == null)
                                innerFilterEntry = new HashMap<>();
                            innerFilterEntry.putAll(filterValueEntry.getValue());
                            dataEntry.put(filterValueEntry.getKey(), innerFilterEntry);
                        }
                        dataMap.put(filterKey, dataEntry);
                    }
                }
            }

        }
        return dataMap;
    }

    private Set<String> getFormObjects(Collection<Pair<ImportFormKeys, CalcPropertyObjectEntity>> properties) {
        Set<String> result = new HashSet<>();
        for(Pair<ImportFormKeys, CalcPropertyObjectEntity> property : properties) {
            for(ObjectEntity objectEntity : property.first.getData()) {
                result.add(objectEntity.getSID());
            }
        }
        return result;
    }

    private ImMap<KeyField, DataObject> getKeys(ImportFormKeys keys) {
        ImMap<KeyField, DataObject> keyObjects = MapFact.EMPTY();
        for (ObjectEntity key : keys.getData()) {
            if(tagsMap.containsKey(key.getSID())) {
                keyObjects = keyObjects.addExcl(new KeyField(key.getSID(), ImportDataActionProperty.type), new DataObject(tagsMap.get(key.getSID())));
            }
        }
        return keyObjects;
    }

    //todo: переосмыслить и упростить
    private Map<ImportFormKeys, ImportFormData> getFilterValues(
            Set<Pair<ImportFormKeys, CalcPropertyObjectEntity>> filters, String currentKey) {
        Map<ImportFormKeys, ImportFormData> filterValuesMap = new HashMap<>();
        for (Pair<ImportFormKeys, CalcPropertyObjectEntity> filterEntry : filters) {
            ImportFormData dataEntry = new ImportFormData();
            ImMap<KeyField, DataObject> key = getFilterKeys(filterEntry.first, currentKey);
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

    private ImMap<KeyField, DataObject> getFilterKeys(ImportFormKeys keys, String currentKey) {
        ImMap<KeyField, DataObject> keyObjects = MapFact.EMPTY();
        boolean skip = true;
        for (ObjectEntity key : keys.getData()) {
            if(tagsMap.containsKey(key.getSID())) {
                if(currentKey.equals(key.getSID())) {
                    skip = false;
                }
                keyObjects = keyObjects.addExcl(new KeyField(key.getSID(), ImportDataActionProperty.type), new DataObject(tagsMap.get(key.getSID())));
            } else return MapFact.EMPTY();
        }
        return skip ? MapFact.<KeyField, DataObject>EMPTY() : keyObjects;
    }

    private ObjectValue getObjectValue(Object value, ConcreteClass type) {
        try {
            if (isLeaf(value)) {
                value = type instanceof IntegralClass ? getChildValue(value).trim() : getChildValue(value);
                return new DataObject(type.getType().parseString((String) value), type);
            } else {
                return NullValue.instance;
            }
        } catch (ParseException e) {
            return NullValue.instance;
        }
    }

    private ImportFormData merge(ImportFormData data1, ImportFormData data2) {
        if (data1 != null) {
            data1.merge(data2);
            return data1;
        } else return data2;
    }
}