package lsfusion.server.logics.property.actions.integration.exporting.plain;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.stat.StaticDataGenerator;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.integration.FormIntegrationType;
import lsfusion.server.logics.property.actions.integration.exporting.ExportActionProperty;
import lsfusion.server.logics.property.actions.integration.exporting.StaticExportData;
import lsfusion.server.logics.property.actions.integration.hierarchy.ExportData;
import lsfusion.server.logics.property.actions.integration.plain.PlainConstants;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class ExportPlainActionProperty<O extends ObjectSelector> extends ExportActionProperty<O> {

    protected final String charset;

    protected final LCP<?> singleExportFile; // nullable, temporary will be removed
    protected final ImMap<GroupObjectEntity, LCP> exportFiles;
    
    public ExportPlainActionProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, FormIntegrationType staticType, LCP singleExportFile, ImMap<GroupObjectEntity, LCP> exportFiles, String charset) {
        super(caption, form, objectsToSet, nulls, staticType);
        
        this.charset = charset;
        
        this.singleExportFile = singleExportFile;
        this.exportFiles = exportFiles;
    }

    @Override
    protected void export(ExecutionContext<ClassPropertyInterface> context, StaticExportData exportData, StaticDataGenerator.Hierarchy hierarchy) throws IOException, SQLException, SQLHandledException {
        Map<GroupObjectEntity, byte[]> files = new HashMap<>();

        exportGroupData(hierarchy.getRoot(), SetFact.<GroupObjectEntity>EMPTY(), hierarchy, files, exportData, null);

        writeResult(context, files);
    }

    protected void writeResult(ExecutionContext<ClassPropertyInterface> context, Map<GroupObjectEntity, byte[]> files) throws SQLException, SQLHandledException {
        for (Map.Entry<GroupObjectEntity, byte[]> entry : files.entrySet()) {
            if(singleExportFile != null) {
                writeResult(singleExportFile, staticType, context, entry.getValue(), new DataObject(entry.getKey() == null ? "root" : entry.getKey().getSID()));
            } else {
                LCP exportFile = exportFiles.get(entry.getKey() == null ? GroupObjectEntity.NULL : entry.getKey());
                if(exportFile != null)
                    writeResult(exportFile, staticType, context, entry.getValue());
            }
        }
    }

    protected abstract ExportPlainWriter getWriter(ImOrderMap<String, Type> fieldTypes, boolean singleRow) throws IOException;

    private void exportGroupData(GroupObjectEntity currentGroup, ImSet<GroupObjectEntity> parentGroups, StaticDataGenerator.Hierarchy hierarchy, Map<GroupObjectEntity, byte[]> files, final ExportData data, ImOrderSet<ImMap<ObjectEntity, Object>> parentRows) throws IOException {
        
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
        ObjectEntity object = null;
        String objectSID = null;
        boolean isIndex = false;
        if(currentGroup != null) {
            isIndex = currentGroup.isIndex();

            if(!isIndex) {
                object = currentGroup.getObjects().single();
                objectSID = currentGroup.getIntegrationSID();
                fieldTypes = fieldTypes.addOrderExcl(MapFact.singletonOrder(objectSID, (Type)object.baseClass));
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

        ExportPlainWriter exporter = getWriter(fieldTypes, currentGroup == null);
        ImOrderSet<ImMap<ObjectEntity, Object>> allRows = data.getRows(currentGroup);
        exporter.writeCount(allRows.size());
        for(int i=0,size=allRows.size();i<size;i++) {
            final ImMap<ObjectEntity, Object> currentRow = allRows.get(i);

            ImMap<String, Object> fieldValues = propertyNames.mapValues(new GetValue<Object, PropertyDrawEntity>() {
                public Object getMapValue(PropertyDrawEntity value) {
                    return data.getProperty(value, currentRow);
                }});
            
            if(!parentGroups.isEmpty())
                fieldValues = fieldValues.addExcl(PlainConstants.parentFieldName, parentIndexes.get(currentRow.filterIncl(parentObjects)));

            if(currentGroup != null)
                fieldValues = fieldValues.addExcl(objectSID, currentRow.get(object));
            
            exporter.writeLine(fieldValues);
        }

        files.put(currentGroup, exporter.release());

        if(currentGroup != null)
            parentGroups = parentGroups.addExcl(currentGroup);
        for(GroupObjectEntity childGroup : hierarchy.getDependencies(currentGroup))
            exportGroupData(childGroup, parentGroups, hierarchy, files, data, allRows);

    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        if(singleExportFile != null)
            return getChangeProps(singleExportFile.property);
        return getChangeProps(exportFiles.values().mapColValues(new GetValue<CalcProperty, LCP>() {
            public CalcProperty getMapValue(LCP value) {
                return ((LCP<?>)value).property;
            }
        }));
    }    
}
