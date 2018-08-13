package lsfusion.server.logics.property.actions.importing;

import lsfusion.base.Pair;
import lsfusion.base.ProgressBar;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
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
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;
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

    protected abstract Map<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> getData(Object files, Map<String, Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity>> propertyKeysMap, Set<Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity>> filters, Map<String, List<String>> headersMap) throws IOException, ParseException;

    protected void importData(ExecutionContext context, Object files) throws IOException, ParseException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        Map<String, Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity>> propertyKeysMap = new HashMap<>();
        Set<Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity>> filters = new HashSet<>();
        Map<String, List<String>> headersMap = new LinkedHashMap<>();
        Map<ImSet<ObjectEntity>, List<CalcPropertyObjectEntity>> propertiesMap = new HashMap<>();

        //читаем свойства
        for (PropertyDrawEntity propertyDraw : formEntity.getPropertyDrawsList()) {
            GroupObjectEntity toDraw = propertyDraw.getToDraw(formEntity);
            if (toDraw != null && propertyDraw.propertyObject.property instanceof CalcProperty) {
                CalcPropertyObjectEntity property = (CalcPropertyObjectEntity) propertyDraw.propertyObject;

                ImSet<ObjectEntity> keys = getNeededGroupsForColumnProp(property);
                String escapedId = escapeTag(propertyDraw.getSID());
                propertyKeysMap.put(escapedId, Pair.create(keys, property));

                List<String> headersEntry = headersMap.get(toDraw.getSID());
                if (headersEntry == null)
                    headersEntry = new ArrayList<>();
                headersEntry.add(escapedId);
                headersMap.put(toDraw.getSID(), headersEntry);

                List<CalcPropertyObjectEntity> propertiesEntry = propertiesMap.get(keys);
                if (propertiesEntry == null)
                    propertiesEntry = new ArrayList<>();
                propertiesEntry.add(property);
                propertiesMap.put(keys, propertiesEntry);
            }
        }

        //добавляем фильтры
        for (FilterEntity filter : formEntity.getFixedFilters()) {
            if (filter instanceof NotNullFilterEntity) {
                CalcPropertyObjectEntity<?> property = ((NotNullFilterEntity) filter).property;
                    ImSet<ObjectEntity> keys = SetFact.fromJavaSet(new HashSet<>((property.getObjectInstances())));
                    List<CalcPropertyObjectEntity> propertiesEntry = propertiesMap.get(keys);
                    if (propertiesEntry == null)
                        propertiesEntry = new ArrayList<>();
                    propertiesEntry.add(property);
                    propertiesMap.put(keys, propertiesEntry);
                    filters.add(Pair.<ImSet<ObjectEntity>, CalcPropertyObjectEntity>create(keys, property));
                }
            }

            //Отменяем все предыдущие изменения в сессии
            for (List<CalcPropertyObjectEntity> properties : propertiesMap.values()) {
                for (CalcPropertyObjectEntity property : properties) {
                    if (property.property instanceof DataProperty)
                        context.getSession().dropChanges((DataProperty) property.property);
                }
            }

            //читаем данные
            Map<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> data = getData(files, propertyKeysMap, filters, headersMap);

            // потому как writeRows требует именно NullValue.instance
            for (Map.Entry<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> dataEntry : data.entrySet()) {
                for (Map.Entry<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> entry : dataEntry.getValue().entrySet()) {
                    // дополняем NullValue.instance там где не все Property (вообще актуально)
                    for (Pair<ImSet<ObjectEntity>, CalcPropertyObjectEntity> propertyEntry : propertyKeysMap.values()) {
                        Property property = propertyEntry.second.property;
                        if (dataEntry.getKey().equals(propertyEntry.first) && !entry.getValue().containsKey(property)) {
                            entry.getValue().put(property, NullValue.instance);
                        }
                    }
                }
            }

        //записываем данные
        for (Map.Entry<ImSet<ObjectEntity>, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>>> dataEntry : data.entrySet()) {
            ImSet<ObjectEntity> dataKey = dataEntry.getKey();
            List<CalcPropertyObjectEntity> properties = propertiesMap.get(dataKey);
            writeData(context, properties, dataEntry.getValue());
        }
    }

    private ImSet<ObjectEntity> getNeededGroupsForColumnProp(CalcPropertyObjectEntity property) {
        ImSet<ObjectEntity> result = SetFact.EMPTY();
        for(Object entry : property.mapping.valueIt()) {
            result = result.addExcl(((ObjectEntity) entry));
        }
        return result;
    }

    private void writeData(ExecutionContext context, List<CalcPropertyObjectEntity> properties, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> data) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        //группируем свойства по наборам ключей (может отличаться порядок)
        Map<ImOrderSet<KeyField>, List<CalcPropertyObjectEntity>> keyPropMap = new HashMap<>();
        for(CalcPropertyObjectEntity property : properties) {
            ImOrderSet<KeyField> keySet = SetFact.EMPTYORDER();
            for (Object key : property.mapping.values()) {
                KeyField keyField = new KeyField(((ObjectEntity) key).getSID(), ImportDataActionProperty.type);
                keySet = keySet.addOrderExcl(keyField);
            }
            List<CalcPropertyObjectEntity> props = keyPropMap.get(keySet);
            if(props == null) {
                props = new ArrayList<>();
            }
            props.add(property);
            keyPropMap.put(keySet, props);
        }

        for(Map.Entry<ImOrderSet<KeyField>, List<CalcPropertyObjectEntity>> keyPropEntry : keyPropMap.entrySet()) {
            writeData(context, keyPropEntry.getKey(), keyPropEntry.getValue(), data);
        }
    }

    private void writeData(ExecutionContext context, ImOrderSet<KeyField> keySet, List<CalcPropertyObjectEntity> properties, Map<ImMap<KeyField, DataObject>, Map<Property, ObjectValue>> data) throws SQLException, SQLHandledException {
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
                writeBatch(keySet, properties, mPremap.immutable().mapValues(mapProfileValue), context, new ProgressBar("ImportForm", batchNumber, batchQuantity));
                batchNumber++;
                mPremap = newPremap();
                batchCounter = 0;
            }
        }
        if (batchCounter > 0) {
            writeBatch(keySet, properties, mPremap.immutable().mapValues(mapProfileValue), context, new ProgressBar("ImportForm", batchNumber, batchQuantity));
        }
    }

    @StackProgress
    private void writeBatch(ImOrderSet<KeyField> keySet, List<CalcPropertyObjectEntity> props, ImMap<ImMap<KeyField, DataObject>, ImMap<Property, ObjectValue>> data, ExecutionContext context, @StackProgress ProgressBar progress) throws SQLException, SQLHandledException {
        ImOrderSet<Property> properties = SetFact.EMPTYORDER();
        for(CalcPropertyObjectEntity property : props) {
            properties = properties.addOrderExcl(property.property);
        }

        SessionTableUsage<KeyField, Property> importTable =
                new SessionTableUsage("impformdata", keySet, properties, new Type.Getter<KeyField>() {
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

    protected String getKeysId(ImSet<ObjectEntity> keys) {
        String result = "";
        for (ObjectEntity key : keys)
            result += (result.isEmpty() ? "" : "_") + key.getSID();
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
