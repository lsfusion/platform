package lsfusion.server.logics.form.open.stat;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.ProgressBar;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.session.*;
import lsfusion.server.logics.action.session.classes.change.ClassChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.action.session.table.SingleKeyPropertyUsage;
import lsfusion.server.logics.classes.ConcreteCustomClass;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.logics.classes.StaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.NullValue;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.form.stat.integration.importing.FormImportData;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.CalcPropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.action.SystemActionProperty;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.session.*;
import lsfusion.server.base.stack.StackProgress;

import java.io.IOException;
import java.sql.SQLException;

public abstract class ImportActionProperty extends SystemActionProperty {
    protected FormEntity formEntity;
    protected String charset;

    public ImportActionProperty(int paramsCount, FormEntity formEntity, String charset) {
        super(LocalizedString.create("Import"), SetFact.toOrderExclSet(paramsCount, new GetIndex<PropertyInterface>() {
            @Override
            public PropertyInterface getMapValue(int i) {
                return new PropertyInterface();
            }
        }));
        
        this.formEntity = formEntity;
        this.charset = charset;
    }

    protected static RawFileData readFile(ObjectValue value) throws SQLException, SQLHandledException {
        if(value instanceof DataObject)
            return readFile(((DataObject) value).objectClass.getType(), ((DataObject) value).object);
        return null;
    }
    private static RawFileData readFile(Type type, Object singleFile) throws SQLException, SQLHandledException {
        if (type instanceof StaticFormatFileClass) {
            return (RawFileData) singleFile;
        } else {
            if(singleFile == null)
                return null;
            return ((FileData)singleFile).getRawFile();
        }
    }

    protected abstract FormImportData getData(ExecutionContext<PropertyInterface> context) throws IOException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException;

    @Override
    protected FlowResult aspectExecute(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<CalcPropertyObjectEntity, ImMap<ImMap<ObjectEntity, Object>, Object>> result;
        ImMap<ObjectEntity, ImSet<Long>> addedObjects;
        try {
            FormImportData data = getData(context);
            result = data.result();
            addedObjects = data.resultAddedObjects();
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

        // dropping previous changes
        for (CalcPropertyObjectEntity<?> property : result.keys()) {
            for(DataProperty changeProp : property.property.getChangeProps())
               context.getSession().dropChanges(changeProp);
        }

        MExclMap<DataObject, ObjectValue> mAddedObjects = MapFact.mExclMap();
        for(int i=0,size=addedObjects.size();i<size;i++) {
            // convert to DataObject / ObjectValue
            ConcreteCustomClass customClass = (ConcreteCustomClass)addedObjects.getKey(i).baseClass;
            DataObject classObject = customClass.getClassObject();
            for(Long object : addedObjects.getValue(i))
                mAddedObjects.exclAdd(new DataObject(object, context.getSession().baseClass.unknown), classObject);
        }
        writeClassData(context, mAddedObjects.immutable());

        // group by used objects
        ImMap<ImSet<ObjectEntity>, ImSet<CalcPropertyObjectEntity>> groupedProps = result.keys().group(new BaseUtils.Group<ImSet<ObjectEntity>, CalcPropertyObjectEntity>() {
            public ImSet<ObjectEntity> group(CalcPropertyObjectEntity key) {
                return ((CalcPropertyObjectEntity<?>)key).getObjectInstances();
            }
        });

        for(int i=0,size=groupedProps.size();i<size;i++) {            
            // group by rows, convert to DataObject / ObjectValue, fill map with null values (needed for writeRows)
            ImSet<CalcPropertyObjectEntity> props = groupedProps.getValue(i);

            ImMap<CalcPropertyObjectEntity, NullValue> nullValues = props.toMap(NullValue.instance);

            // group by rows
            MExclMap<ImMap<ObjectEntity, DataObject>, MMap<CalcPropertyObjectEntity, ObjectValue>> mRows = MapFact.mExclMap();
            for(CalcPropertyObjectEntity prop : props) {
                ImMap<ImMap<ObjectEntity, Object>, Object> propValues = result.get(prop);
                for(int j=0,sizeJ=propValues.size();j<sizeJ;j++) {
                    // convert to DataObject / ObjectValue
                    ImMap<ObjectEntity, DataObject> keys = propValues.getKey(j).mapValues(new GetKeyValue<DataObject, ObjectEntity, Object>() {
                        public DataObject getMapValue(ObjectEntity key, Object value) {
                            return new DataObject(value, key.baseClass instanceof ConcreteCustomClass ? context.getSession().baseClass.unknown : (DataClass) key.baseClass);
                        }
                    });
                    ObjectValue value = ObjectValue.getValue(propValues.getValue(j), (DataClass)prop.getType());

                    MMap<CalcPropertyObjectEntity, ObjectValue> mProps = mRows.get(keys);
                    if(mProps == null) {
                        // fill map with null values (needed for writeRows)
                        mProps = MapFact.mMap(nullValues, MapFact.<CalcPropertyObjectEntity, ObjectValue>override());
                        mRows.exclAdd(keys, mProps);
                    }                    
                    mProps.add(prop, value);
                }
            }

            writeData(context, groupedProps.getKey(i), props, MapFact.immutableMapMap(mRows));
        }
        return FlowResult.FINISH;
    }

    private interface DataWriter<K, V> {

        void writeBatch(ImMap<K, V> data, ProgressBar progress) throws SQLException, SQLHandledException;
    }

    private <K, V> void writeData(ImMap<K, V> data, DataWriter<K, V> writer) throws SQLException, SQLHandledException {
        int batchSize = data.size();

        ImFilterValueMap<K, V> mPremap = data.mapFilterValues();

        int batchCounter = 0;
        int batchQuantity = (int) Math.ceil((double) data.size() / batchSize);
        int batchNumber = 1;

        for (int i=0,size=data.size();i<size;i++) {
            mPremap.mapValue(i, data.getValue(i));

            batchCounter++;

            if (batchCounter == batchSize) {
                writer.writeBatch(mPremap.immutableValue(), new ProgressBar("ImportForm", batchNumber, batchQuantity));
                batchNumber++;
                mPremap = data.mapFilterValues();
                batchCounter = 0;
            }
        }
        if (batchCounter > 0) {
            writer.writeBatch(mPremap.immutableValue(), new ProgressBar("ImportForm", batchNumber, batchQuantity));
        }

    }

    private void writeClassData(final ExecutionContext context, ImMap<DataObject, ObjectValue> data) throws SQLException, SQLHandledException {
        writeData(data, new DataWriter<DataObject, ObjectValue>() {
            @Override
            public void writeBatch(ImMap<DataObject, ObjectValue> data, ProgressBar progress) throws SQLException, SQLHandledException {
                writeClassBatch(data, context, progress);
            }
        });
    }

    private void writeData(final ExecutionContext context, final ImSet<ObjectEntity> keySet, final ImSet<CalcPropertyObjectEntity> properties, ImMap<ImMap<ObjectEntity, DataObject>, ImMap<CalcPropertyObjectEntity, ObjectValue>> data) throws SQLException, SQLHandledException {
        writeData(data, new DataWriter<ImMap<ObjectEntity, DataObject>, ImMap<CalcPropertyObjectEntity, ObjectValue>>() {
            public void writeBatch(ImMap<ImMap<ObjectEntity, DataObject>, ImMap<CalcPropertyObjectEntity, ObjectValue>> data, ProgressBar progress) throws SQLException, SQLHandledException {
                ImportActionProperty.this.writeBatch(keySet, properties, data, context, progress);
            }
        });
    }

    @StackProgress
    private <T extends PropertyInterface> void writeClassBatch(ImMap<DataObject, ObjectValue> data, ExecutionContext context, @StackProgress ProgressBar progress) throws SQLException, SQLHandledException {
        SingleKeyPropertyUsage importTable = ClassChanges.createChangeTable("impformclassdata");

        DataSession session = context.getSession();
        importTable.writeRows(session.sql, session.getOwner(), data);

        try {
            context.changeClass(importTable.getChange());
        } finally {
            importTable.drop(session.sql, session.getOwner());
        }
    }
    @StackProgress
    private <T extends PropertyInterface> void writeBatch(ImSet<ObjectEntity> keySet, ImSet<CalcPropertyObjectEntity> props, ImMap<ImMap<ObjectEntity, DataObject>, ImMap<CalcPropertyObjectEntity, ObjectValue>> data, ExecutionContext context, @StackProgress ProgressBar progress) throws SQLException, SQLHandledException {
        SessionTableUsage<ObjectEntity, CalcPropertyObjectEntity> importTable =
                new SessionTableUsage<>("impformdata", keySet.toOrderSet(), props.toOrderSet(), new Type.Getter<ObjectEntity>() {
                    public Type getType(ObjectEntity key) {
                        return key.getType();
                    }
                }, new Type.Getter<CalcPropertyObjectEntity>() {
                    public Type getType(CalcPropertyObjectEntity key) {
                        return key.getType();
                    }
                });

        DataSession session = context.getSession();
        importTable.writeRows(session.sql, data, session.getOwner());

        final ImRevMap<ObjectEntity, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<CalcPropertyObjectEntity> importJoin = importTable.join(mapKeys);
        try {
            for (CalcPropertyObjectEntity<T> property : props)
                context.getEnv().change(property.property, new PropertyChange<T>(property.mapping.join(mapKeys), importJoin.getExpr(property), importJoin.getWhere()));
        } finally {
            importTable.drop(session.sql, session.getOwner());
        }
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        MSet<CalcProperty> mProps = SetFact.mSet();
        for(PropertyDrawEntity propertyDraw : formEntity.getStaticPropertyDrawsList())
            mProps.add((CalcProperty) propertyDraw.getImportProperty().property);
        return mProps.immutable().toMap(false);
    }
}
