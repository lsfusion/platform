package lsfusion.server.logics.form.stat.integration.exporting.plain;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.IntegerClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.open.stat.ExportActionProperty;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.integration.FormIntegrationType;
import lsfusion.server.logics.form.stat.integration.exporting.StaticExportData;
import lsfusion.server.logics.form.stat.integration.hierarchy.ExportData;
import lsfusion.server.logics.form.stat.integration.importing.plain.ImportPlainActionProperty;
import lsfusion.server.logics.form.stat.integration.plain.PlainConstants;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class ExportPlainActionProperty<O extends ObjectSelector> extends ExportActionProperty<O> {
    protected final ImMap<GroupObjectEntity, LP> exportFiles;
    
    public ExportPlainActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, String charset) {
        super(caption, form, objectsToSet, nulls, staticType, charset);
        this.exportFiles = exportFiles;
    }

    @Override
    protected void export(ExecutionContext<ClassPropertyInterface> context, StaticExportData exportData, StaticDataGenerator.Hierarchy hierarchy) throws IOException, SQLException, SQLHandledException {
        Map<GroupObjectEntity, RawFileData> files = new HashMap<>();

        exportGroupData(hierarchy.getRoot(), SetFact.<GroupObjectEntity>EMPTY(), hierarchy, files, exportData, null);

        writeResult(context, files);
    }

    protected void writeResult(ExecutionContext<ClassPropertyInterface> context, Map<GroupObjectEntity, RawFileData> files) throws SQLException, SQLHandledException {
        for (Map.Entry<GroupObjectEntity, RawFileData> entry : files.entrySet()) {
            LP exportFile = exportFiles.get(entry.getKey() == null ? GroupObjectEntity.NULL : entry.getKey());
            if(exportFile != null)
                writeResult(exportFile, staticType, context, entry.getValue());
        }
    }

    protected abstract ExportPlainWriter getWriter(ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException;

    private void exportGroupData(GroupObjectEntity currentGroup, ImSet<GroupObjectEntity> parentGroups, StaticDataGenerator.Hierarchy hierarchy, Map<GroupObjectEntity, RawFileData> files, final ExportData data, ImOrderSet<ImMap<ObjectEntity, Object>> parentRows) throws IOException {
        
        ImOrderSet<PropertyDrawEntity> childProperties = hierarchy.getProperties(currentGroup);

        ImOrderMap<String, Type> fieldTypes = MapFact.EMPTYORDER();
        ImSet<ObjectEntity> parentObjects = null;
        ImMap<ImMap<ObjectEntity, Object>, Integer> parentIndexes = null; // optimization, a lot faster then indexof
        if(!parentGroups.isEmpty()) {
            fieldTypes = MapFact.singletonOrder(PlainConstants.parentFieldName, (Type) IntegerClass.instance);
            parentObjects = GroupObjectEntity.getObjects(parentGroups);

            parentIndexes = parentRows.mapOrderValues(new GetIndex<Integer>() {
                public Integer getMapValue(int i) {
                    return i;
                }
            });
        }

        // index or key object access
        boolean isIndex = false;
        ImRevMap<ObjectEntity, String> mapFields = null;
        if(currentGroup != null) {
            isIndex = currentGroup.isIndex();

            if(!isIndex) {
                mapFields = ImportPlainActionProperty.getFields(currentGroup);
                fieldTypes = fieldTypes.addOrderExcl(ImportPlainActionProperty.getFieldTypes(currentGroup, mapFields));
            }
        }

        ImRevMap<String, PropertyDrawEntity> propertyNames = childProperties.getSet().mapRevKeys(new GetValue<String, PropertyDrawEntity>() {
            public String getMapValue(PropertyDrawEntity property) {
                return property.getIntegrationSID();
            }
        });
        fieldTypes = fieldTypes.addOrderExcl(propertyNames.mapOrder(childProperties).mapOrderValues(new GetValue<Type, PropertyDrawEntity>() {
            public Type getMapValue(PropertyDrawEntity property) {
                return data.getType(property);
            }
        }));

        ImOrderSet<ImMap<ObjectEntity, Object>> allRows = data.getRows(currentGroup);

        RawFileData resultFile;
        ExportPlainWriter exporter = getWriter(fieldTypes, currentGroup == null);
        try {
            exporter.writeCount(allRows.size());
            for (int i = 0, size = allRows.size(); i < size; i++) {
                final ImMap<ObjectEntity, Object> currentRow = allRows.get(i);

                ImMap<String, Object> fieldValues = propertyNames.mapValues(new GetValue<Object, PropertyDrawEntity>() {
                    public Object getMapValue(PropertyDrawEntity value) {
                        return data.getProperty(value, currentRow);
                    }
                });

                if (!parentGroups.isEmpty()) fieldValues = fieldValues.addExcl(PlainConstants.parentFieldName, parentIndexes.get(currentRow.filterIncl(parentObjects)));

                if (currentGroup != null) if (!isIndex) fieldValues = fieldValues.addExcl(mapFields.crossJoin(currentRow));

                exporter.writeLine(fieldValues);
            }
        } finally {
            resultFile = exporter.release();
        }

        files.put(currentGroup, resultFile);

        if(currentGroup != null)
            parentGroups = parentGroups.addExcl(currentGroup);
        for(GroupObjectEntity childGroup : hierarchy.getDependencies(currentGroup))
            exportGroupData(childGroup, parentGroups, hierarchy, files, data, allRows);

    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getChangeProps(exportFiles.values().mapColValues(new GetValue<Property, LP>() {
            public Property getMapValue(LP value) {
                return ((LP<?>)value).property;
            }
        }));
    }    
}
