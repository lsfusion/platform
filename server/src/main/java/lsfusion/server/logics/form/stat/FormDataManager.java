package lsfusion.server.logics.form.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.form.stat.print.PrintMessageData;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

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

    public PrintMessageData getPrintMessageData(int selectTop, boolean removeNullsAndDuplicates) throws SQLException, SQLHandledException {

        ExportResult sources = getExportData(selectTop);

        // filling message (root group)
        GroupObjectEntity root = sources.hierarchy.getRoot();
        Pair<List<String>, List<List<String>>> rootTable = getPrintTable(root, sources, removeNullsAndDuplicates);
        List<String> rootTitles = rootTable.first;
        List<String> rootRows = BaseUtils.single(rootTable.second);

        StringBuilder builder = new StringBuilder();
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
            Pair<List<String>, List<List<String>>> table = getPrintTable(dependencies.get(0), sources, removeNullsAndDuplicates);
            titles = table.first;
            rows = table.second;
        } else { // empty table
            titles = new ArrayList<>();
            rows = new ArrayList<>();
        }

        return new PrintMessageData(message, titles, rows);
    }

    public ExportResult getExportData(int selectTop) throws SQLException, SQLHandledException {
        ExportStaticDataGenerator sourceGenerator = new ExportStaticDataGenerator(dataInterface);
        Pair<Map<GroupObjectEntity, StaticKeyData>, StaticPropertyData<PropertyDrawEntity>> data = sourceGenerator.generate(selectTop);
        return new ExportResult(data.first, data.second, sourceGenerator.hierarchy);
    }

    private Pair<List<String>, List<List<String>>> getPrintTable(GroupObjectEntity group, final ExportResult sources, boolean removeNullsAndDuplicates) {
        final StaticKeyData tableData = sources.keys.get(group);

        ImOrderSet<PropertyDrawEntity> tableProperties = sources.hierarchy.getProperties(group);
        if(tableProperties == null)
            tableProperties = SetFact.EMPTYORDER();

        if(removeNullsAndDuplicates) // actually the more precise heuristics can be implemented in addPropertyDraw for group (calculating expr and putting expr itself (not its values)  in a set) 
            tableProperties = removeNullsAndDuplicates(sources, tableProperties);

        // filling titles
        List<String> titles = new ArrayList<>();
        for(PropertyDrawEntity<?> property : tableProperties)
            titles.add(ThreadLocalContext.localize(property.getCaption()));

        // filling data
        List<List<String>> rows = new ArrayList<>();
        for(ImMap<ObjectEntity, Object> row : tableData.data) {
            List<String> dataRow = new ArrayList<>();
            for(PropertyDrawEntity<?> property : tableProperties)
                dataRow.add(sources.properties.types.get(property).formatString(StaticPropertyData.getProperty(sources.properties, property, row), true));
            rows.add(dataRow);
        }

        return new Pair<>(titles, rows);
    }

    private ImOrderSet<PropertyDrawEntity> removeNullsAndDuplicates(ExportResult sources, ImOrderSet<PropertyDrawEntity> tableProperties) {
        MAddSet<Pair<LocalizedString, ImMap<ImMap<ObjectEntity, Object>, Object>>> existingColumns = SetFact.mAddSet();
        MOrderExclSet<PropertyDrawEntity> mFilteredProps = SetFact.mOrderExclSetMax(tableProperties.size());
        for(PropertyDrawEntity tableProperty : tableProperties) {
            ImMap<ImMap<ObjectEntity, Object>, Object> values = sources.properties.data.get(tableProperty);
            if(!SetFact.onlyNulls(values.valueIt()) && //  remove columns with nulls
                !existingColumns.add(new Pair<>(tableProperty.getCaption(), values))) // remove columns with the same name and data
                    mFilteredProps.exclAdd(tableProperty);
        }
        return mFilteredProps.immutableOrder();
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
