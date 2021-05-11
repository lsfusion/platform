package lsfusion.server.logics.form.stat.struct.export.plain;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.file.RawFileData;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.open.stat.ExportAction;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.StaticExportData;
import lsfusion.server.logics.form.stat.struct.hierarchy.ExportData;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainAction;
import lsfusion.server.logics.form.stat.struct.plain.PlainConstants;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class ExportPlainAction<O extends ObjectSelector> extends ExportAction<O> {
    protected final ImMap<GroupObjectEntity, LP> exportFiles;

    private boolean useCaptionInsteadOfIntegrationSID;

    public ExportPlainAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                             ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<?, PropertyInterface, O>> contextFilters,
                             FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, Integer selectTop, String charset) {
        this(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, exportFiles, selectTop, charset, false);
    }

    public ExportPlainAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                             ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<?, PropertyInterface, O>> contextFilters,
                             FormIntegrationType staticType, ImMap<GroupObjectEntity, LP> exportFiles, Integer selectTop, String charset, boolean useCaptionInsteadOfIntegrationSID) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, selectTop, charset);
        this.exportFiles = exportFiles;
        this.useCaptionInsteadOfIntegrationSID = useCaptionInsteadOfIntegrationSID;
    }

    @Override
    protected void export(ExecutionContext<ClassPropertyInterface> context, StaticExportData exportData, StaticDataGenerator.Hierarchy hierarchy) throws IOException, SQLException, SQLHandledException {
        Map<GroupObjectEntity, RawFileData> files = new HashMap<>();

        exportGroupData(hierarchy.getRoot(), SetFact.EMPTY(), hierarchy, files, exportData, null);

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

    protected void exportGroupData(GroupObjectEntity currentGroup, ImSet<GroupObjectEntity> parentGroups, StaticDataGenerator.Hierarchy hierarchy, Map<GroupObjectEntity, RawFileData> files, final ExportData data, ImOrderSet<ImMap<ObjectEntity, Object>> parentRows) throws IOException {
        
        ImOrderSet<PropertyDrawEntity> childProperties = hierarchy.getProperties(currentGroup);

        ImOrderMap<String, Type> fieldTypes = MapFact.EMPTYORDER();
        ImSet<ObjectEntity> parentObjects = null;
        ImMap<ImMap<ObjectEntity, Object>, Integer> parentIndexes = null; // optimization, a lot faster then indexof
        if(!parentGroups.isEmpty()) {
            fieldTypes = MapFact.singletonOrder(PlainConstants.parentFieldName, IntegerClass.instance);
            parentObjects = GroupObjectEntity.getObjects(parentGroups);

            parentIndexes = parentRows.mapOrderValues((int i) -> i);
        }

        // index or key object access
        boolean isIndex = false;
        ImRevMap<ObjectEntity, String> mapFields = null;
        if(currentGroup != null) {
            isIndex = currentGroup.isIndex();

            if(!isIndex) {
                mapFields = ImportPlainAction.getFields(currentGroup);
                fieldTypes = fieldTypes.addOrderExcl(ImportPlainAction.getFieldTypes(currentGroup, mapFields));
            }
        }

        ImRevMap<String, PropertyDrawEntity> propertyNames = childProperties.getSet().mapRevKeys(useCaptionInsteadOfIntegrationSID ?
                ((Function<PropertyDrawEntity, String>) propertyDrawEntity -> ThreadLocalContext.localize(propertyDrawEntity.getCaption())) :
                (Function<PropertyDrawEntity, String>) PropertyDrawEntity::getIntegrationSID);
        fieldTypes = fieldTypes.addOrderExcl(propertyNames.mapOrder(childProperties).mapOrderValues(new Function<PropertyDrawEntity, Type>() {
            public Type apply(PropertyDrawEntity property) {
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

                ImMap<String, Object> fieldValues = propertyNames.mapValues(value -> data.getProperty(value, currentRow));

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
        return getChangeProps(exportFiles.values().mapColValues(value -> ((LP<?>)value).property));
    }    
}
