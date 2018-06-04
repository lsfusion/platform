package lsfusion.server.logics.property.actions.importing;

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
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.entity.filter.NotNullFilterEntity;
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
import java.sql.SQLException;
import java.util.*;

public abstract class ImportFormDataActionProperty extends SystemExplicitActionProperty {
    private FormEntity formEntity;

    public ImportFormDataActionProperty(ValueClass[] valueClasses, FormEntity formEntity) {
        super(valueClasses);
        this.formEntity = formEntity;
    }

    protected abstract Map<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> getData(Object files, Map<String, Pair<List<String>, CalcProperty>> propertyKeysMap, Map<String, List<String>> headersMap) throws IOException, ParseException;

    protected void importData(ExecutionContext context, Object files) throws IOException, ParseException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        Map<String, Pair<List<String>, CalcProperty>> propertyKeysMap = new HashMap<>();
        Map<String, List<String>> headersMap = new LinkedHashMap<>();
        Map<String, List<CalcProperty>> propertiesMap = new HashMap<>();
        Map<String, List<KeyField>> keyFieldsMap = new HashMap<>();
        Map<String, Pair<List<String>, CalcProperty>> filtersMap = new HashMap<>(); //предполагаем, что будет только 1 фильтр на каждое сочетание ключей

        //читаем свойства
        for (PropertyDrawEntity propertyDraw : formEntity.getPropertyDrawsList()) {
            GroupObjectEntity toDraw = propertyDraw.getToDraw(formEntity);
            if (toDraw != null && propertyDraw.propertyObject.property instanceof CalcProperty) {
                CalcProperty property = (CalcProperty) propertyDraw.propertyObject.property;
                
                List<String> keys = getNeededGroupsForColumnProp(propertyDraw);
                String keysId = getKeysId(keys);
                String escapedId = escapeTag(propertyDraw.getSID());
                propertyKeysMap.put(escapedId, Pair.create(keys, property));
                
                List<String> headersEntry = headersMap.get(toDraw.getSID());
                if(headersEntry == null)
                    headersEntry = new ArrayList<>();
                headersEntry.add(escapedId);
                headersMap.put(toDraw.getSID(), headersEntry);

                List<CalcProperty> propertiesEntry = propertiesMap.get(keysId);
                if (propertiesEntry == null)
                    propertiesEntry = new ArrayList();
                propertiesEntry.add(property);
                propertiesMap.put(keysId, propertiesEntry);

                List<KeyField> keyFieldsEntry = keyFieldsMap.get(keysId);
                if (keyFieldsEntry == null)
                    keyFieldsEntry = new ArrayList();
                for (String key : keys) {
                    KeyField keyField = new KeyField(key, ImportDataActionProperty.type);
                    if (!keyFieldsEntry.contains(keyField))
                        keyFieldsEntry.add(keyField);
                }
                keyFieldsMap.put(keysId, keyFieldsEntry);
            }
        }

        //добавляем фильтры
        for (FilterEntity filter : formEntity.getFixedFilters()) {
            if (filter instanceof NotNullFilterEntity) {
                CalcPropertyObjectEntity<?> property = ((NotNullFilterEntity) filter).property;
                if (property.property instanceof DataProperty) {
                    List<String> keys = new ArrayList<>();
                    for (Object objectInstance : property.getObjectInstances()) {
                        if (objectInstance instanceof ObjectEntity)
                            keys.add(((ObjectEntity) objectInstance).getSID());
                    }
                    context.getSession().dropChanges((DataProperty) property.property);
                    String keysId = getKeysId(keys);
                    filtersMap.put(keysId, Pair.<List<String>, CalcProperty>create(keys, property.property));
                    List<CalcProperty> propertiesEntry = propertiesMap.get(keysId);
                    if (propertiesEntry == null)
                        propertiesEntry = new ArrayList();
                    propertiesEntry.add(property.property);
                    propertiesMap.put(keysId, propertiesEntry);
                } else if (property.property instanceof JoinProperty) { // непонятно почему 
                    for (Object dProperty : ((JoinProperty) property.property).getChangeProps()) {
                        if (dProperty instanceof DataProperty) {
                            List<String> keys = new ArrayList<>();
                            for (Object objectInstance : property.getRemappedObjectInstances()) {
                                if (objectInstance instanceof ObjectEntity) {
                                    keys.add(((ObjectEntity) objectInstance).getSID());
                                }
                            }
                            String keysId = getKeysId(keys);
                            filtersMap.put(keysId, Pair.create(keys, (CalcProperty) dProperty));
                            List<CalcProperty> propertiesEntry = propertiesMap.get(keysId);
                            if (propertiesEntry == null)
                                propertiesEntry = new ArrayList();
                            propertiesEntry.add((CalcProperty) dProperty);
                            propertiesMap.put(keysId, propertiesEntry);
                        }
                    }
                }
            }
        }

        //Отменяем все предыдущие изменения в сессии
        for (List<CalcProperty> properties : propertiesMap.values()) {
            for (Property property : properties) {
                if (property instanceof DataProperty)
                    context.getSession().dropChanges((DataProperty) property);
            }
        }

        //читаем данные
        Map<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> data = getData(files, propertyKeysMap, headersMap);

        // потому как writeRows требует именно NullValue.instance
        for (Map.Entry<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> dataEntry : data.entrySet()) {
            for (Map.Entry<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> entry : dataEntry.getValue().entrySet()) {
                // дополняем NullValue.instance там где не все Property (вообще актуально)
                for (Pair<List<String>, CalcProperty> propertyEntry : propertyKeysMap.values()) {
                    Property property = propertyEntry.second;
                    if (dataEntry.getKey().equals(getKeysId(propertyEntry.first)) && !entry.getValue().containsKey(property)) {
                        entry.getValue().put(property, NullValue.instance);
                    }
                }
                // в filters записываем true
                Pair<List<String>, CalcProperty> filterEntry = filtersMap.get(dataEntry.getKey());
                assert filterEntry != null;
                DataObject defaultDataObject = filterEntry.second.getDefaultDataObject();
                if(defaultDataObject != null)
                    entry.getValue().put(filterEntry.second, defaultDataObject);
            }
        }

        //записываем данные
        for (Map.Entry<String, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> dataEntry : data.entrySet()) {
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

    private void writeData(ExecutionContext context, List<KeyField> keys, List<CalcProperty> properties, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> data) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        MMap<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> mPremap = newPremap();
        GetValue<ImMap<Property, ObjectValue>, Map<Property, ObjectValue>> mapProfileValue = new GetValue<ImMap<Property, ObjectValue>, Map<Property, ObjectValue>>() {
            @Override
            public ImMap<Property, ObjectValue> getMapValue(Map<Property, ObjectValue> profileValue) {
                return MapFact.fromJavaMap(profileValue);
            }
        };

        int batchCounter = 0;
        int batchSize = data.size();
        int batchQuantity = (int) Math.ceil((double) data.size() / batchSize);
        int batchNumber = 1;

        for (Map.Entry<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> entry : data.entrySet()) {
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
    private void writeBatch(final List<KeyField> keys, List<CalcProperty> props, ImMap<ImMap<KeyField, DataObject>, ImMap<Property, ObjectValue>> data, ExecutionContext context, @StackProgress ProgressBar progress) throws SQLException, SQLHandledException {
        ImOrderSet<KeyField> keySet = SetFact.fromJavaOrderSet(keys);
        SessionTableUsage<KeyField, Property> importTable =
                new SessionTableUsage("impformdata", keySet, SetFact.fromJavaOrderSet(props), new Type.Getter<KeyField>() {
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
                PropertyChange propChange = new PropertyChange(property.getFriendlyOrderInterfaces().mapSet(keySet).join(mapKeys), importJoin.getExpr(property), importJoin.getWhere());
                context.getEnv().change((CalcProperty) property, propChange);
            }
        } finally {
            importTable.drop(session.sql, session.getOwner());
        }
    }

    private MMap<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> newPremap() {
        return MapFact.mMap(new SymmAddValue<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>() {
            @Override
            public Map<Property, ObjectValue> addValue(ImMap<KeyField, DataObject> key, Map<Property, ObjectValue> prevValue, Map<Property, ObjectValue> newValue) {
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
    protected boolean isSync() {
        return true; // тут сложно посчитать что изменяется, поэтому пока просто считаем синхронным, чтобы не компилировался FOR
    }

    @Override
    protected boolean allowNulls() {
        return false;
    }
}
