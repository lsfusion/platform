package lsfusion.server.form.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.SFunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.form.PrintMessageData;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.instance.FormDataInterface;
import lsfusion.server.form.instance.StaticKeyData;
import lsfusion.server.form.instance.StaticPropertyData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class FormDataManager {

    protected final FormDataInterface dataInterface; // for multiple inhertiance

    public FormDataManager(FormDataInterface dataInterface) {
        this.dataInterface = dataInterface;
    }

    public FormEntity getFormEntity() {
        return dataInterface.getFormEntity();
    }

    public PrintMessageData getPrintMessageData(int selectTop, boolean removeNulls) throws SQLException, SQLHandledException {

        FormEntity formEntity = getFormEntity();

        ExportResult sources = getExportData(selectTop);

        // filling message (root group)
        GroupObjectEntity root = sources.hierarchy.getRoot();
        Pair<List<String>, List<List<String>>> rootTable = getPrintTable(root, sources, removeNulls);
        List<String> rootTitles = rootTable.first;
        List<String> rootRows = BaseUtils.single(rootTable.second);

        StringBuilder builder = new StringBuilder(ThreadLocalContext.localize(formEntity.getCaption()));
        for (int i=0,size=rootTitles.size();i<size;i++) {
            if(builder.length() != 0)
                builder.append("\n");

            String title = rootTitles.get(i);
            if(!title.isEmpty())
                builder.append(title).append(" : ");
            builder.append(rootRows.get(i));
        }
        String message = builder.toString();

        // filling table (first child)
        List<String> titles;
        List<List<String>> rows;
        ImOrderSet<GroupObjectEntity> dependencies = sources.hierarchy.getDependencies(root);
        if(!dependencies.isEmpty()) {
            Pair<List<String>, List<List<String>>> table = getPrintTable(dependencies.get(0), sources, removeNulls);
            titles = table.first;
            rows = table.second;
        } else { // empty table
            titles = new ArrayList<>();
            rows = new ArrayList<>();
        }

        return new PrintMessageData(message, titles, rows);
    }

    public ExportResult getExportData(int selectTop) throws SQLException, SQLHandledException {
        StaticDataGenerator.Hierarchy hierarchy = dataInterface.getHierarchy(false);
        BaseStaticDataGenerator sourceGenerator = new BaseStaticDataGenerator(dataInterface, hierarchy, false);
        Pair<Map<GroupObjectEntity, StaticKeyData>, StaticPropertyData<PropertyDrawEntity>> data = sourceGenerator.generate(selectTop);
        return new ExportResult(data.first, data.second, hierarchy);
    }

    private Pair<List<String>, List<List<String>>> getPrintTable(GroupObjectEntity group, final ExportResult sources, boolean removeNulls) {
        final StaticKeyData tableData = sources.keys.get(group);

        ImOrderSet<PropertyDrawEntity> tableProperties = sources.hierarchy.getProperties(group);
        if(tableProperties == null)
            tableProperties = SetFact.EMPTYORDER();

        // remov'ing nulls 
        if(removeNulls)
            tableProperties = tableProperties.filterOrder(new SFunctionSet<PropertyDrawEntity>() {
                public boolean contains(PropertyDrawEntity property) {
                    for(ImMap<ObjectEntity, Object> row : tableData.data) {
                        Object value = StaticPropertyData.getProperty(sources.properties, property, row);
                        if(value != null)
                            return true;
                    }
                    return false;
                }
            });

        // filling titles
        List<String> titles = new ArrayList<>();
        for(PropertyDrawEntity<?> property : tableProperties)
            titles.add(ThreadLocalContext.localize(property.getCaption()));

        // filling data
        List<List<String>> rows = new ArrayList<>();
        for(ImMap<ObjectEntity, Object> row : tableData.data) {
            List<String> dataRow = new ArrayList<>();
            for(PropertyDrawEntity<?> property : tableProperties)
                dataRow.add(sources.properties.types.get(property).formatString(StaticPropertyData.getProperty(sources.properties, property, row)));
            rows.add(dataRow);
        }

        return new Pair<>(titles, rows);
    }

    public ExportResult getExportData() throws SQLException, SQLHandledException {
        return getExportData(0);
    }

    public static class ExportResult {
        public final Map<GroupObjectEntity, StaticKeyData> keys;
        public final StaticPropertyData<PropertyDrawEntity> properties;
        public final StaticDataGenerator.Hierarchy hierarchy;

        public ExportResult(Map<GroupObjectEntity, StaticKeyData> keys, StaticPropertyData<PropertyDrawEntity> properties, StaticDataGenerator.Hierarchy hierarchy) {
            this.keys = keys;
            this.properties = properties;
            this.hierarchy = hierarchy;
        }
    }
}
