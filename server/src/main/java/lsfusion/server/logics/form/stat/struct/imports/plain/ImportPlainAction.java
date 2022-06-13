package lsfusion.server.logics.form.stat.struct.imports.plain;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.form.open.stat.ImportAction;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.struct.hierarchy.ImportData;
import lsfusion.server.logics.form.stat.struct.imports.FormImportData;
import lsfusion.server.logics.form.stat.struct.plain.PlainConstants;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class ImportPlainAction<I extends ImportPlainIterator> extends ImportAction {

    protected final ImRevMap<GroupObjectEntity, PropertyInterface> fileInterfaces;
    protected final PropertyInterface whereInterface;

    public ImportPlainAction(int paramsCount, ImOrderSet<GroupObjectEntity> groupFiles, FormEntity formEntity, String charset, boolean hasWhere) {
        super(paramsCount, formEntity, charset);

        this.fileInterfaces = groupFiles.mapSet(getOrderInterfaces());
        whereInterface = hasWhere ? getOrderInterfaces().get(groupFiles.size()) : null;
    }

    public abstract ImportPlainIterator getIterator(RawFileData file, ImOrderMap<String, Type> fieldTypes, String wheres, ExecutionContext<PropertyInterface> context) throws IOException;

    protected FormImportData getData(ExecutionContext<PropertyInterface> context) throws IOException, SQLException, SQLHandledException {
        Map<GroupObjectEntity, RawFileData> files = getFiles(context);

        StaticDataGenerator.Hierarchy hierarchy = formEntity.getImportHierarchy();
        FormImportData importData = new FormImportData(formEntity, context);
        importGroupData(hierarchy.getRoot(), SetFact.<GroupObjectEntity>EMPTY(), hierarchy, files, importData, context, null);

        return importData;
    }

    private void importGroupData(GroupObjectEntity currentGroup, ImSet<GroupObjectEntity> parentGroups, StaticDataGenerator.Hierarchy hierarchy, Map<GroupObjectEntity, RawFileData> files, ImportData data, ExecutionContext<PropertyInterface> context, ImOrderSet<ImMap<ObjectEntity, Object>> parentRows) throws IOException {
        
        ImOrderSet<PropertyDrawEntity> childProperties = hierarchy.getProperties(currentGroup);

        RawFileData file = files.get(currentGroup);
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
                    mapFields = ImportPlainAction.getFields(currentGroup);
                    fieldTypes = fieldTypes.addOrderExcl(ImportPlainAction.getFieldTypes(currentGroup, mapFields));
                }
            }

            ImRevMap<PropertyDrawEntity, String> propertyNames = childProperties.getSet().mapRevValues((Function<PropertyDrawEntity, String>) PropertyDrawEntity::getIntegrationSID);

            fieldTypes = fieldTypes.addOrderExcl(propertyNames.reverse().mapOrder(childProperties).mapOrderValues(new Function<PropertyDrawEntity, Type>() {
                public Type apply(PropertyDrawEntity object) {
                    return object.getType();
                }}));

            MOrderSet<ImMap<ObjectEntity, Object>> mAllRows = SetFact.mOrderSet(isIndex);

            String wheres = whereInterface != null ? (String)context.getKeyObject(whereInterface) : null;
            ImportPlainIterator iterator = getIterator(file, fieldTypes, wheres, context);
            try {
                ImMap<String, Object> row;
                while ((row = iterator.next()) != null) {
                    ImMap<ObjectEntity, Object> objectValues = MapFact.EMPTY();
                    if (!parentGroups.isEmpty()) 
                        objectValues = parentRows.get((Integer) row.get(PlainConstants.parentFieldName));
                    if (currentGroup != null) {
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
                    for (int i = 0, size = propertyValues.size(); i < size; i++)
                        data.addProperty(propertyValues.getKey(i), objectValues, propertyValues.getValue(i), isIndex);
                }
            } finally {
                iterator.release();
            }
            
            allRows = mAllRows.immutableOrder();
        }

        if(currentGroup != null)
            parentGroups = parentGroups.addExcl(currentGroup);
        for(GroupObjectEntity childGroup : hierarchy.getDependencies(currentGroup))
            importGroupData(childGroup, parentGroups, hierarchy, files, data, context, allRows);
    }

    public static ImRevMap<ObjectEntity, String> getFields(GroupObjectEntity currentGroup) {
        return currentGroup.getObjects().mapRevValues(ObjectEntity::getIntegrationSID);
    }
    public static ImOrderMap<String, Type> getFieldTypes(GroupObjectEntity currentGroup, final ImRevMap<ObjectEntity, String> fields) {
        return currentGroup.getOrderObjects().mapOrderValues(ObjectEntity::getType).map(fields);
    }

    private Map<GroupObjectEntity, RawFileData> getFiles(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        Map<GroupObjectEntity, RawFileData> files = new HashMap<>();
        for(int i=0,size=fileInterfaces.size();i<size;i++) {
            GroupObjectEntity fileObject = fileInterfaces.getKey(i);
            files.put(fileObject == GroupObjectEntity.NULL ? null : fileObject, readFile(context.getKeyValue(fileInterfaces.getValue(i)), charset));
        }
        return files;
    }
}