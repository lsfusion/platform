package lsfusion.server.logics.form.open.stat;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.ProgressBar;
import lsfusion.server.base.controller.stack.StackProgress;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.classes.change.ClassChanges;
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.action.session.table.SingleKeyPropertyUsage;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.file.StaticFormatFileClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.stat.struct.imports.FormImportData;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ImportAction extends SystemAction {
    protected FormEntity formEntity;
    protected String charset;

    public ImportAction(int paramsCount, FormEntity formEntity, String charset) {
        super(LocalizedString.create("Import"), SetFact.toOrderExclSet(paramsCount, i -> new PropertyInterface()));
        
        this.formEntity = formEntity;
        this.charset = charset;
    }

    protected static RawFileData readFile(ObjectValue value) throws SQLException, SQLHandledException {
        if(value instanceof DataObject)
            return readFile(((DataObject) value).objectClass.getType(), ((DataObject) value).object);
        return null;
    }
    private static RawFileData readFile(Type type, Object singleFile) {
        if (type instanceof StaticFormatFileClass) {
            return (RawFileData) singleFile;
        } else {
            if(singleFile == null)
                return null;
            return ((FileData)singleFile).getRawFile();
        }
    }

    protected abstract FormImportData getData(ExecutionContext<PropertyInterface> context) throws IOException, SQLException, SQLHandledException;

    @Override
    protected FlowResult aspectExecute(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        Map<PropertyObjectEntity, MMap<ImMap<ObjectEntity, Object>, Object>> result;
        ImMap<ObjectEntity, ImSet<Long>> addedObjects;
        try {
            FormImportData data = getData(context);
            result = data.result();
            addedObjects = data.resultAddedObjects();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        // dropping previous changes
        for (PropertyObjectEntity<?> property : result.keySet()) {
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
        ImMap<ImSet<ObjectEntity>, ImSet<PropertyObjectEntity>> groupedProps = SetFact.fromJavaSet(result.keySet()).group(new BaseUtils.Group<ImSet<ObjectEntity>, PropertyObjectEntity>() {
            public ImSet<ObjectEntity> group(PropertyObjectEntity key) {
                return ((PropertyObjectEntity<?>)key).getObjectInstances();
            }
        });

        for(int i=0,size=groupedProps.size();i<size;i++) {            
            // group by rows, convert to DataObject / ObjectValue, fill map with null values (needed for writeRows)
            ImSet<PropertyObjectEntity> props = groupedProps.getValue(i);

            ImMap<PropertyObjectEntity, NullValue> nullValues = props.toMap(NullValue.instance);

            // group by rows
            MExclMap<ImMap<ObjectEntity, DataObject>, MMap<PropertyObjectEntity, ObjectValue>> mRows = MapFact.mExclMap();
            for(PropertyObjectEntity prop : props) {
                ImMap<ImMap<ObjectEntity, Object>, Object> propValues = result.get(prop).immutable();
                for(int j=0,sizeJ=propValues.size();j<sizeJ;j++) {
                    // convert to DataObject / ObjectValue
                    ImMap<ObjectEntity, DataObject> keys = propValues.getKey(j).mapValues((key, value) -> new DataObject(value, key.baseClass instanceof ConcreteCustomClass ? context.getSession().baseClass.unknown : (DataClass) key.baseClass));
                    ObjectValue value = ObjectValue.getValue(propValues.getValue(j), (DataClass)prop.getType());

                    MMap<PropertyObjectEntity, ObjectValue> mProps = mRows.get(keys);
                    if(mProps == null) {
                        // fill map with null values (needed for writeRows)
                        mProps = MapFact.mMap(nullValues, MapFact.override());
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
        writeData(data, (data1, progress) -> writeClassBatch(data1, context, progress));
    }

    private void writeData(final ExecutionContext context, final ImSet<ObjectEntity> keySet, final ImSet<PropertyObjectEntity> properties, ImMap<ImMap<ObjectEntity, DataObject>, ImMap<PropertyObjectEntity, ObjectValue>> data) throws SQLException, SQLHandledException {
        writeData(data, (data1, progress) -> ImportAction.this.writeBatch(keySet, properties, data1, context, progress));
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
    private <T extends PropertyInterface> void writeBatch(ImSet<ObjectEntity> keySet, ImSet<PropertyObjectEntity> props, ImMap<ImMap<ObjectEntity, DataObject>, ImMap<PropertyObjectEntity, ObjectValue>> data, ExecutionContext context, @StackProgress ProgressBar progress) throws SQLException, SQLHandledException {
        SessionTableUsage<ObjectEntity, PropertyObjectEntity> importTable =
                new SessionTableUsage<>("impformdata", keySet.toOrderSet(), props.toOrderSet(), ObjectEntity::getType, PropertyObjectEntity::getType);

        DataSession session = context.getSession();
        importTable.writeRows(session.sql, data, session.getOwner());

        final ImRevMap<ObjectEntity, KeyExpr> mapKeys = importTable.getMapKeys();
        Join<PropertyObjectEntity> importJoin = importTable.join(mapKeys);
        try {
            for (PropertyObjectEntity<T> property : props)
                context.getEnv().change(property.property, new PropertyChange<>(property.mapping.join(mapKeys), importJoin.getExpr(property), importJoin.getWhere()));
        } finally {
            importTable.drop(session.sql, session.getOwner());
        }
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        List<Property> properties = new ArrayList<>();
        for(PropertyDrawEntity propertyDraw : formEntity.getStaticPropertyDrawsList())
            properties.add((Property) propertyDraw.getImportProperty().property);
        return getChangeProps(properties.toArray(new Property[0]));
    }
}
