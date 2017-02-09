package lsfusion.server.logics.property.actions.importing;

import lsfusion.base.OrderedMap;
import lsfusion.base.Pair;
import lsfusion.base.ProgressBar;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.entity.filter.NotNullFilterEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyDrawInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.SessionTableUsage;
import lsfusion.server.stack.StackProgress;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public abstract class ImportFormDataActionProperty extends SystemExplicitActionProperty {
    private FormEntity<?> formEntity;

    public ImportFormDataActionProperty(ValueClass[] valueClasses, FormEntity formEntity) {
        super(valueClasses);
        this.formEntity = formEntity;
    }

    protected abstract Map<String, Map<ImMap<KeyField, DataObject>, Map<Property, Object>>> getData(Object files, Map<String, Pair<List<String>, Property>> propertyKeysMap, Map<String, List<String>> headersMap) throws IOException, ParseException;

    protected void importData(ExecutionContext context, Object files) throws IOException, ParseException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        Map<String, Pair<List<String>, Property>> propertyKeysMap = new HashMap<>();
        Map<String, List<String>> headersMap = new LinkedHashMap<>();
        Map<String, List<Property>> propertiesMap = new HashMap<>();
        Map<String, List<KeyField>> keyFieldsMap = new HashMap<>();
        Map<String, Pair<List<String>, Property>> filtersMap = new HashMap<>(); //предполагаем, что будет только 1 фильтр на каждое сочетание ключей

        FormInstance formInstance = context.createFormInstance(formEntity);

        //читаем свойства
        for (Object propertyDraw : formEntity.getPropertyDrawsList()) {
            PropertyDrawInstance instance = ((PropertyDrawEntity) propertyDraw).getInstance(formInstance.instanceFactory);
            if (instance.toDraw != null) {
                List<String> keys = getNeededGroupsForColumnProp((PropertyDrawEntity) propertyDraw);
                String keysId = getKeysId(keys);
                String escapedId = escapeTag(instance.getsID());
                propertyKeysMap.put(escapedId, Pair.create(keys, instance.propertyObject.property));

                List<String> headersEntry = headersMap.get(instance.toDraw.getSID());
                if(headersEntry == null)
                    headersEntry = new ArrayList<>();
                headersEntry.add(escapedId);
                headersMap.put(instance.toDraw.getSID(), headersEntry);

                List<Property> propertiesEntry = propertiesMap.get(keysId);
                if (propertiesEntry == null)
                    propertiesEntry = new ArrayList();
                propertiesEntry.add(instance.propertyObject.property);
                propertiesMap.put(keysId, propertiesEntry);

                List<KeyField> keyFieldsEntry = keyFieldsMap.get(keysId);
                if (keyFieldsEntry == null)
                    keyFieldsEntry = new ArrayList();
                for (String key : keys) {
                    KeyField keyField = new KeyField(key, IntegerClass.instance);
                    if (!keyFieldsEntry.contains(keyField))
                        keyFieldsEntry.add(keyField);
                }
                keyFieldsMap.put(keysId, keyFieldsEntry);
            }
        }

        //добавляем фильтры
        for (FilterEntity filter : formEntity.getFixedFilters()) {
            if (filter instanceof NotNullFilterEntity) {
                CalcPropertyObjectEntity property = ((NotNullFilterEntity) filter).property;
                if (property.property instanceof DataProperty) {
                    List<String> keys = new ArrayList<>();
                    for (Object objectInstance : property.getObjectInstances()) {
                        if (objectInstance instanceof ObjectEntity)
                            keys.add(((ObjectEntity) objectInstance).getSID());
                    }
                    context.getSession().dropChanges((DataProperty) property.property);
                    String keysId = getKeysId(keys);
                    filtersMap.put(keysId, Pair.create(keys, property.property));
                    List<Property> propertiesEntry = propertiesMap.get(keysId);
                    if (propertiesEntry == null)
                        propertiesEntry = new ArrayList();
                    propertiesEntry.add(property.property);
                    propertiesMap.put(keysId, propertiesEntry);
                } else if (property.property instanceof JoinProperty) {
                    for (Object dProperty : ((JoinProperty) property.property).getChangeProps()) {
                        if (dProperty instanceof DataProperty) {
                            List<String> keys = new ArrayList<>();
                            for (Object objectInstance : property.getRemappedObjectInstances()) {
                                if (objectInstance instanceof ObjectEntity) {
                                    keys.add(((ObjectEntity) objectInstance).getSID());
                                }
                            }
                            String keysId = getKeysId(keys);
                            filtersMap.put(keysId, Pair.create(keys, (Property) dProperty));
                            List<Property> propertiesEntry = propertiesMap.get(keysId);
                            if (propertiesEntry == null)
                                propertiesEntry = new ArrayList();
                            propertiesEntry.add((Property) dProperty);
                            propertiesMap.put(keysId, propertiesEntry);
                        }
                    }
                }
            }
        }

        //Отменяем все предыдущие изменения в сессии
        for (List<Property> properties : propertiesMap.values()) {
            for (Property property : properties) {
                if (property instanceof DataProperty)
                    context.getSession().dropChanges((DataProperty) property);
            }
        }

        //читаем данные
        Map<String, Map<ImMap<KeyField, DataObject>, Map<Property, Object>>> data = getData(files, propertyKeysMap, headersMap);

        //Дополняем null'ами незаполненные данные, чтобы далее получить из них NullValue.instance,
        // потому как writeRows требует именно NullValue.instance
        for (Map.Entry<String, Map<ImMap<KeyField, DataObject>, Map<Property, Object>>> dataEntry : data.entrySet()) {
            for (Map.Entry<ImMap<KeyField, DataObject>, Map<Property, Object>> entry : dataEntry.getValue().entrySet()) {
                for (Pair<List<String>, Property> propertyEntry : propertyKeysMap.values()) {
                    Property property = propertyEntry.second;
                    if (dataEntry.getKey().equals(getKeysId(propertyEntry.first)) && !entry.getValue().containsKey(property)) {
                        entry.getValue().put(property, null);
                    }
                }
                Pair<List<String>, Property> filterEntry = filtersMap.get(dataEntry.getKey());
                assert filterEntry != null;
                entry.getValue().put(filterEntry.second, new DataObject(true));

            }
        }

        //записываем данные
        for (Map.Entry<String, Map<ImMap<KeyField, DataObject>, Map<Property, Object>>> dataEntry : data.entrySet()) {
            String dataKey = dataEntry.getKey();
            writeData(context, keyFieldsMap.get(dataKey), propertiesMap.get(dataKey), dataEntry.getValue());
        }
    }

    private List<String> getNeededGroupsForColumnProp(PropertyDrawEntity propertyDraw) {
        List<String> result = new ArrayList();
        for(Object entry : propertyDraw.propertyObject.mapping.valueIt()) {
            result.add(((ObjectEntity) entry).getSID());
        }
        return result;
    }

    private void writeData(ExecutionContext context, List<KeyField> keys, List<Property> properties, Map<ImMap<KeyField, DataObject>, Map<Property, Object>> data) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        MMap<ImMap<KeyField, DataObject>, Map<Property, Object>> mPremap = newPremap();
        GetValue<ImMap<Property, ObjectValue>, Map<Property, Object>> mapProfileValue = new GetValue<ImMap<Property, ObjectValue>, Map<Property, Object>>() {
            @Override
            public ImMap<Property, ObjectValue> getMapValue(Map<Property, Object> profileValue) {
                ImMap<Property, ObjectValue> map = MapFact.EMPTY();
                for (Map.Entry<Property, Object> profileEntry : profileValue.entrySet()) {
                    map = map.addExcl(profileEntry.getKey(), profileEntry.getValue() == null ? NullValue.instance : (DataObject) profileEntry.getValue());
                }
                return map;
            }
        };

        int batchCounter = 0;
        int batchSize = data.size();
        int batchQuantity = (int) Math.ceil((double) data.size() / batchSize);
        int batchNumber = 1;

        for (Map.Entry<ImMap<KeyField, DataObject>, Map<Property, Object>> entry : data.entrySet()) {
            mPremap.add(entry.getKey(), entry.getValue());

            batchCounter++;

            if (batchCounter == batchSize) {
                writeBatch(keys, properties, mPremap.immutable().mapValues(mapProfileValue), context, new ProgressBar("ImportForm", batchNumber, batchQuantity));
                batchNumber++;
                mPremap = newPremap();
                batchCounter = 0;
            }
        }
        if (batchCounter > 0) {
            writeBatch(keys, properties, mPremap.immutable().mapValues(mapProfileValue), context, new ProgressBar("ImportForm", batchNumber, batchQuantity));
        }
    }

    @StackProgress
    private void writeBatch(final List<KeyField> keys, List<Property> props, ImMap<ImMap<KeyField, DataObject>, ImMap<Property, ObjectValue>> data, ExecutionContext context, @StackProgress ProgressBar progress) throws SQLException, SQLHandledException {
        ImOrderSet<KeyField> keySet = SetFact.fromJavaOrderSet(keys);
        SessionTableUsage<KeyField, Property> importTable =
                new SessionTableUsage(keySet, SetFact.fromJavaOrderSet(props), new Type.Getter<KeyField>() {
                    @Override
                    public Type getType(KeyField key) {
                        return key.type;
                    }
                }, new Type.Getter<Property>() {
                    @Override
                    public Type getType(Property key) {
                        return key.getType();
                    }
                });

        DataSession session = context.getSession();

        importTable.writeRows(session.sql, data, session.getOwner());

        final ImRevMap<KeyField, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<Property> importJoin = importTable.join(mapKeys);
        try {
            for (Property property : importTable.getValues()) {
                PropertyChange propChange = new PropertyChange(property.getFriendlyPropertyOrderInterfaces().mapSet(keySet).join(mapKeys), importJoin.getExpr(property), importJoin.getWhere());
                context.getEnv().change((CalcProperty) property, propChange);
            }
        } finally {
            importTable.drop(session.sql, session.getOwner());
        }
    }

    private MMap<ImMap<KeyField, DataObject>, Map<Property, Object>> newPremap() {
        return MapFact.mMap(new SymmAddValue<ImMap<KeyField, DataObject>, Map<Property, Object>>() {
            @Override
            public Map<Property, Object> addValue(ImMap<KeyField, DataObject> key, Map<Property, Object> prevValue, Map<Property, Object> newValue) {
                prevValue.putAll(newValue);
                return prevValue;
            }
        });
    }

    protected String getKeysId(List<String> keys) {
        String result = "";
        List<String> keysCopy = new ArrayList<>(keys);
        for (String key : keysCopy)
            result += (result.isEmpty() ? "" : "_") + key;
        return result;
    }

    private String escapeTag(String value) {
        return value.replace("_", "__").replace("()", "").replaceAll(",|\\(", "_").replace(")", "");
    }

    private void addToMap(Map<String, List<String>> headersMap, String escapedId, String keysId) {
        List<String> headersEntry = headersMap.get(keysId);
        if(headersEntry == null)
            headersEntry = new ArrayList<>();
        headersEntry.add(escapedId);
        headersMap.put(keysId, headersEntry);
    }

    protected boolean indexBased() {
        return false;
    }

    @Override
    protected boolean allowNulls() {
        return false;
    }
}
