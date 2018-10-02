package lsfusion.server.logics.property.actions.integration.importing;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ProgressBar;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.interop.FormStaticType;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.SystemActionProperty;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.SessionTableUsage;
import lsfusion.server.stack.StackProgress;

import java.io.IOException;
import java.sql.SQLException;

public abstract class ImportActionProperty extends SystemActionProperty {
    protected FormEntity formEntity;

    public ImportActionProperty(int paramsCount, FormEntity formEntity) {
        super(LocalizedString.create("Import"), SetFact.toOrderExclSet(paramsCount, new GetIndex<PropertyInterface>() {
            @Override
            public PropertyInterface getMapValue(int i) {
                return new PropertyInterface();
            }
        }));
        
        this.formEntity = formEntity;
    }

    protected static byte[] readFile(LCP file, byte[] singleFile) throws SQLException, SQLHandledException {
        return readFile(file.property.getType(), singleFile);
    }
    protected static byte[] readFile(ObjectValue value) throws SQLException, SQLHandledException {
        if(value instanceof DataObject)
            return readFile(((DataObject) value).objectClass.getType(), (byte[])((DataObject) value).object);
        return null;
    }
    private static byte[] readFile(Type type, byte[] singleFile) throws SQLException, SQLHandledException {
        if (type instanceof StaticFormatFileClass) {
            return singleFile;
        } else {
            if(singleFile == null)
                return null;
            return BaseUtils.getFile(singleFile);
        }
    }

    protected abstract FormImportData getData(ExecutionContext<PropertyInterface> context) throws IOException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException;

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<CalcPropertyObjectEntity, ImMap<ImMap<ObjectEntity, Object>, Object>> result;
        try {
            FormImportData data = getData(context);
            result = data.result();
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

        // dropping previous changes
        for (CalcPropertyObjectEntity<?> property : result.keys()) {
            for(DataProperty changeProp : property.property.getChangeProps())
               context.getSession().dropChanges(changeProp);
        }

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
                            return new DataObject(value, (ConcreteClass) key.baseClass);
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

    private void writeData(ExecutionContext context, ImSet<ObjectEntity> keySet, ImSet<CalcPropertyObjectEntity> properties, ImMap<ImMap<ObjectEntity, DataObject>, ImMap<CalcPropertyObjectEntity, ObjectValue>> data) throws SQLException, SQLHandledException {
        int batchSize = data.size();
        
        ImFilterValueMap<ImMap<ObjectEntity, DataObject>, ImMap<CalcPropertyObjectEntity, ObjectValue>> mPremap = data.mapFilterValues();
        
        int batchCounter = 0;
        int batchQuantity = (int) Math.ceil((double) data.size() / batchSize);
        int batchNumber = 1;

        for (int i=0,size=data.size();i<size;i++) {
            mPremap.mapValue(i, data.getValue(i));

            batchCounter++;

            if (batchCounter == batchSize) {
                writeBatch(keySet, properties, mPremap.immutableValue(), context, new ProgressBar("ImportForm", batchNumber, batchQuantity));
                batchNumber++;
                mPremap = data.mapFilterValues();
                batchCounter = 0;
            }
        }
        if (batchCounter > 0) {
            writeBatch(keySet, properties, mPremap.immutableValue(), context, new ProgressBar("ImportForm", batchNumber, batchQuantity));
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
        for(ImOrderSet<PropertyDrawEntity> propertyDraws : formEntity.getGroupProperties(SetFact.<GroupObjectEntity>EMPTY()).valueIt())
            for(PropertyDrawEntity propertyDraw : propertyDraws)
                mProps.add((CalcProperty) propertyDraw.getImportProperty().property);
        return mProps.immutable().toMap(false);
    }
}
