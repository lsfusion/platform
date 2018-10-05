package lsfusion.server.logics.property.actions.integration.importing.plain;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.stat.StaticDataGenerator;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.integration.hierarchy.ImportData;
import lsfusion.server.logics.property.actions.integration.importing.FormImportData;
import lsfusion.server.logics.property.actions.integration.importing.ImportActionProperty;
import lsfusion.server.logics.property.actions.integration.plain.PlainConstants;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class ImportPlainActionProperty<I extends ImportPlainIterator> extends ImportActionProperty {

    protected final LCP<?> fileProperty;
    protected final ImRevMap<GroupObjectEntity, PropertyInterface> fileInterfaces;

    public abstract ImportPlainIterator getIterator(byte[] file, ImOrderMap<String, Type> fieldTypes, ExecutionContext<PropertyInterface> context) throws IOException;

    public ImportPlainActionProperty(int paramsCount, LCP<?> fileProperty, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity) {
        super(paramsCount, formEntity);

        this.fileProperty = fileProperty;
        this.fileInterfaces = groupFiles.mapSet(getOrderInterfaces());
    }

    protected FormImportData getData(ExecutionContext<PropertyInterface> context) throws IOException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        Map<GroupObjectEntity, byte[]> files = getFiles(context);
        
        FormImportData importData = new FormImportData(formEntity, context);
        
        StaticDataGenerator.Hierarchy hierarchy = formEntity.getImportHierarchy();        
        importGroupData(hierarchy.getRoot(), SetFact.<GroupObjectEntity>EMPTY(), hierarchy, files, importData, context, null);

        return importData;
    }

    private void importGroupData(GroupObjectEntity currentGroup, ImSet<GroupObjectEntity> parentGroups, StaticDataGenerator.Hierarchy hierarchy, Map<GroupObjectEntity, byte[]> files, ImportData data, ExecutionContext<PropertyInterface> context, ImOrderSet<ImMap<ObjectEntity, Object>> parentRows) throws IOException {
        
        ImOrderSet<PropertyDrawEntity> childProperties = hierarchy.getProperties(currentGroup);

        byte[] file = files.get(currentGroup);
        ImOrderSet<ImMap<ObjectEntity, Object>> allRows = null;
        if(file != null) {
            ImOrderMap<String, Type> fieldTypes = MapFact.EMPTYORDER();
            if(!parentGroups.isEmpty())
                fieldTypes = MapFact.singletonOrder(PlainConstants.parentFieldName, (Type) IntegerClass.instance);

            // index or key object access
            ObjectEntity object = null;
            boolean isIndex = false;
            ImRevMap<ObjectEntity, String> mapFields = null;
            if(currentGroup != null) {
                isIndex = currentGroup.isIndex();

                if(isIndex)
                    object = currentGroup.getObjects().single();
                else {
                    mapFields = ImportPlainActionProperty.getFields(currentGroup);
                    fieldTypes = fieldTypes.addOrderExcl(ImportPlainActionProperty.getFieldTypes(currentGroup, mapFields));
                }
            }

            ImRevMap<PropertyDrawEntity, String> propertyNames = childProperties.getSet().mapRevValues(new GetValue<String, PropertyDrawEntity>() {
                public String getMapValue(PropertyDrawEntity property) {
                    return property.getIntegrationSID();
                }});

            fieldTypes = fieldTypes.addOrderExcl(propertyNames.reverse().mapOrder(childProperties).mapOrderValues(new GetValue<Type, PropertyDrawEntity>() {
                public Type getMapValue(PropertyDrawEntity object) {
                    return object.getType();
                }}));

            MOrderSet<ImMap<ObjectEntity, Object>> mAllRows = SetFact.mOrderSet(isIndex);

            ImportPlainIterator iterator = getIterator(file, fieldTypes, context);
            ImMap<String, Object> row;
            while ((row = iterator.next()) != null) {
                ImMap<ObjectEntity, Object> objectValues = MapFact.EMPTY();
                if(!parentGroups.isEmpty())
                    objectValues = parentRows.get((Integer)row.get(PlainConstants.parentFieldName));
                if(currentGroup != null) {
                    // getting object value
                    try {
                        if (isIndex)
                            objectValues = objectValues.addExcl(object, data.genObject(object));
                        else
                            objectValues = objectValues.addExcl(mapFields.join(row));
                    } catch (SQLException e) {
                        throw Throwables.propagate(e);
                    }
                }
                mAllRows.add(objectValues);

                data.addObject(currentGroup, objectValues, isIndex);

                ImMap<PropertyDrawEntity, Object> propertyValues = propertyNames.join(row);
                for(int i=0,size=propertyValues.size();i<size;i++)
                    data.addProperty(propertyValues.getKey(i), objectValues, propertyValues.getValue(i), isIndex);
            }
            
            allRows = mAllRows.immutableOrder();
        }

        if(currentGroup != null)
            parentGroups = parentGroups.addExcl(currentGroup);
        for(GroupObjectEntity childGroup : hierarchy.getDependencies(currentGroup))
            importGroupData(childGroup, parentGroups, hierarchy, files, data, context, allRows);
    }

    public static ImRevMap<ObjectEntity, String> getFields(GroupObjectEntity currentGroup) {
        return currentGroup.getObjects().mapRevValues(new GetValue<String, ObjectEntity>() {
            public String getMapValue(ObjectEntity value) {
                return value.getIntegrationSID();
            }
        });
    }
    public static ImOrderMap<String, Type> getFieldTypes(GroupObjectEntity currentGroup, final ImRevMap<ObjectEntity, String> fields) {
        return currentGroup.getOrderObjects().mapOrderValues(new GetValue<Type, ObjectEntity>() {
            public Type getMapValue(ObjectEntity value) {
                return value.getType();
            }
        }).map(fields);
    }

    private Map<GroupObjectEntity, byte[]> getFiles(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {
        Map<GroupObjectEntity, byte[]> files = new HashMap<>();

        if(fileProperty != null) {
            LCP<? extends PropertyInterface> property = fileProperty;

            KeyExpr stringExpr = new KeyExpr("string");
            ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "string", stringExpr);
            QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
            query.addProperty("importFiles", property.getExpr(context.getModifier(), stringExpr));
            query.and(property.getExpr(context.getModifier(), stringExpr).getWhere());
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(context);
            
            for (int i = 0; i < result.size(); i++) {
                String groupSID = ((String) result.getKey(i).get("string")).trim();
                
                GroupObjectEntity group; 
                if(groupSID.equals("root"))
                    group = null;
                else
                    group = formEntity.getGroupObjectIntegration(groupSID);
                
                files.put(group, readFile(property, (byte[]) result.getValue(i).get("importFiles")));                
            }
        } else {
            for(int i=0,size=fileInterfaces.size();i<size;i++)
                files.put(fileInterfaces.getKey(i), readFile(context.getKeyValue(fileInterfaces.getValue(i))));
        }
        return files;
    }
}