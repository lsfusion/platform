package lsfusion.server.logics.form.stat.print.tree;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.RawFileData;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.stat.FormDataManager;
import lsfusion.server.logics.form.stat.StaticKeyData;
import lsfusion.server.logics.form.stat.StaticPropertyData;
import lsfusion.server.logics.form.stat.struct.export.plain.xls.ExportXLSWriter;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TreeReportGenerator {

    public static RawFileData generate(TreeGroupEntity treeGroup, FormDataManager.ExportResult sources, FormView formView, FormInstance form) throws IOException, SQLException, SQLHandledException {
        ImOrderSet<GroupObjectEntity> groups = treeGroup.getGroups();
        if (groups.isEmpty())
            return emptyWorkbook();

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet();
        sheet.setRowSumsBelow(false);
        ExportXLSWriter.Styles styles = new ExportXLSWriter.Styles(workbook);

        List<PropertyDrawEntity> columns = new ArrayList<>();
        Map<GroupObjectEntity, Integer> groupColumnStart = new HashMap<>();
        for (GroupObjectEntity group : groups) {
            groupColumnStart.put(group, columns.size());
            ImOrderSet<PropertyDrawEntity> groupProps = sources.hierarchy.getProperties(group);
            for (PropertyDrawEntity prop : groupProps)
                columns.add(prop);
        }

        XSSFRow header = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        headerStyle.setFont(boldFont);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(ThreadLocalContext.localize(formView.get(columns.get(i)).getCaption()));
            cell.setCellStyle(headerStyle);
        }

        int[] rowCounter = new int[]{1};

        GroupObjectEntity firstGroup = groups.get(0);
        if (groups.size() == 1 && firstGroup.isParent != null && firstGroup.getOrderObjects().size() == 1) {
            // Recursive tree: TREE neTree ne PARENT parent(ne)
            writeRecursiveTreeRows(sheet, styles, firstGroup, groupColumnStart.get(firstGroup),
                    sources.hierarchy.getProperties(firstGroup).size(), columns, sources, form, rowCounter);
        } else {
            // Multi-level tree: each group is a separate level
            ImSet<ObjectEntity> emptyObjects = SetFact.EMPTY();
            ImMap<ObjectEntity, Object> emptyRow = MapFact.EMPTY();
            writeGroupRows(sheet, styles, groups, 0, groupColumnStart, columns, sources, emptyRow, emptyObjects, rowCounter);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return new RawFileData(out.toByteArray());
    }

    private static void writeRecursiveTreeRows(XSSFSheet sheet, ExportXLSWriter.Styles styles,
                                               GroupObjectEntity group, int columnStart, int columnCount,
                                               List<PropertyDrawEntity> columns,
                                               FormDataManager.ExportResult sources,
                                               FormInstance form,
                                               int[] rowCounter) throws SQLException, SQLHandledException {
        StaticKeyData keyData = sources.keys.get(group);
        if (keyData == null || keyData.data.isEmpty())
            return;

        ObjectEntity objectEntity = group.getOrderObjects().single();
        InstanceFactory factory = form.instanceFactory;
        ObjectInstance objectInstance = factory.getInstance(objectEntity);
        PropertyObjectEntity<?> parentPropEntity = group.isParent.get(objectEntity);
        PropertyObjectInstance<?> parentPropInstance = factory.getInstance(parentPropEntity);

        ValueClass baseValueClass = objectEntity.baseClass;
        ConcreteCustomClass concreteClass = resolveConcreteClass(baseValueClass);

        // Preserve rows in their original (sorted) order.
        // childToParent: object value -> parent value (or null).
        // childrenByParent: parent value -> ordered list of its child rows.
        Map<Object, Object> childToParent = new LinkedHashMap<>();
        Map<Object, ImMap<ObjectEntity, Object>> rowsByKey = new LinkedHashMap<>();
        Map<Object, List<Object>> childrenByParent = new LinkedHashMap<>();
        childrenByParent.put(null, new ArrayList<>());

        for (ImMap<ObjectEntity, Object> row : keyData.data) {
            Object childValue = row.get(objectEntity);
            rowsByKey.put(childValue, row);

            DataObject childKey = buildDataObject(childValue, concreteClass);
            ObjectValue parentValue;
            if (childKey == null) {
                parentValue = lsfusion.server.data.value.NullValue.instance;
            } else {
                PropertyObjectInstance<?> remapped = parentPropInstance.getRemappedPropertyObject(
                        MapFact.singleton(objectInstance, childKey), true);
                Object raw = remapped.read(form);
                parentValue = raw == null ? lsfusion.server.data.value.NullValue.instance : new DataObject(raw, concreteClass);
            }
            Object parentKey = parentValue instanceof DataObject ? ((DataObject) parentValue).object : null;
            childToParent.put(childValue, parentKey);
        }

        // Group rows by parent. If parent references a node outside the data set, treat it as a root.
        for (Map.Entry<Object, Object> entry : childToParent.entrySet()) {
            Object child = entry.getKey();
            Object parent = entry.getValue();
            Object effectiveParent = (parent != null && rowsByKey.containsKey(parent)) ? parent : null;
            childrenByParent.computeIfAbsent(effectiveParent, k -> new ArrayList<>()).add(child);
        }

        // DFS
        List<Object> roots = childrenByParent.get(null);
        if (roots != null) {
            for (Object root : roots)
                writeRecursiveNode(sheet, styles, root, 0, rowsByKey, childrenByParent,
                        objectEntity, columnStart, columnCount, columns, sources, rowCounter);
        }
    }

    private static void writeRecursiveNode(XSSFSheet sheet, ExportXLSWriter.Styles styles,
                                           Object nodeKey, int level,
                                           Map<Object, ImMap<ObjectEntity, Object>> rowsByKey,
                                           Map<Object, List<Object>> childrenByParent,
                                           ObjectEntity objectEntity,
                                           int columnStart, int columnCount,
                                           List<PropertyDrawEntity> columns,
                                           FormDataManager.ExportResult sources,
                                           int[] rowCounter) {
        ImMap<ObjectEntity, Object> row = rowsByKey.get(nodeKey);
        XSSFRow xRow = sheet.createRow(rowCounter[0]++);
        if (level > 0)
            xRow.getCTRow().setOutlineLevel((short) Math.min(level, 7));

        for (int i = 0; i < columnCount; i++) {
            PropertyDrawEntity prop = columns.get(columnStart + i);
            Object value = StaticPropertyData.getProperty(sources.properties, prop, row);
            Cell cell = xRow.createCell(columnStart + i);
            Type type = sources.properties.types.get(prop);
            if (value != null && type != null)
                type.formatXLS(value, cell, styles);
        }

        List<Object> children = childrenByParent.get(nodeKey);
        if (children != null) {
            for (Object child : children)
                writeRecursiveNode(sheet, styles, child, level + 1, rowsByKey, childrenByParent,
                        objectEntity, columnStart, columnCount, columns, sources, rowCounter);
        }
    }

    private static ConcreteCustomClass resolveConcreteClass(ValueClass baseValueClass) {
        if (baseValueClass instanceof ConcreteCustomClass)
            return (ConcreteCustomClass) baseValueClass;
        if (baseValueClass instanceof CustomClass) {
            lsfusion.base.col.interfaces.mutable.MSet<ConcreteCustomClass> mSet = SetFact.mSet();
            ((CustomClass) baseValueClass).fillConcreteChilds(mSet);
            ImSet<ConcreteCustomClass> set = mSet.immutable();
            if (!set.isEmpty())
                return set.iterator().next();
        }
        return null;
    }

    private static DataObject buildDataObject(Object value, ConcreteCustomClass concreteClass) {
        if (value == null)
            return null;
        if (concreteClass != null)
            return new DataObject(value, concreteClass);
        if (value instanceof Long)
            return new DataObject((Long) value);
        if (value instanceof Integer)
            return new DataObject((Integer) value);
        return null;
    }

    private static void writeGroupRows(XSSFSheet sheet, ExportXLSWriter.Styles styles,
                                       ImOrderSet<GroupObjectEntity> groups, int level,
                                       Map<GroupObjectEntity, Integer> groupColumnStart,
                                       List<PropertyDrawEntity> columns,
                                       FormDataManager.ExportResult sources,
                                       ImMap<ObjectEntity, Object> parentKey,
                                       ImSet<ObjectEntity> parentObjects,
                                       int[] rowCounter) {
        GroupObjectEntity group = groups.get(level);
        StaticKeyData keyData = sources.keys.get(group);
        if (keyData == null)
            return;

        int columnStart = groupColumnStart.get(group);
        int columnCount = sources.hierarchy.getProperties(group).size();

        ImSet<ObjectEntity> nextParentObjects = parentObjects.merge(keyData.objects.getSet());

        for (ImMap<ObjectEntity, Object> row : keyData.data) {
            if (!parentObjects.isEmpty()) {
                ImMap<ObjectEntity, Object> rowParentKey = row.filterIncl(parentObjects);
                if (!rowParentKey.equals(parentKey))
                    continue;
            }

            XSSFRow xRow = sheet.createRow(rowCounter[0]++);
            if (level > 0)
                xRow.getCTRow().setOutlineLevel((short) Math.min(level, 7));

            for (int i = 0; i < columnCount; i++) {
                PropertyDrawEntity prop = columns.get(columnStart + i);
                Object value = StaticPropertyData.getProperty(sources.properties, prop, row);
                Cell cell = xRow.createCell(columnStart + i);
                Type type = sources.properties.types.get(prop);
                if (value != null && type != null)
                    type.formatXLS(value, cell, styles);
            }

            if (level + 1 < groups.size())
                writeGroupRows(sheet, styles, groups, level + 1, groupColumnStart, columns, sources,
                        row.filterIncl(nextParentObjects), nextParentObjects, rowCounter);
        }
    }

    private static RawFileData emptyWorkbook() throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        workbook.createSheet();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return new RawFileData(out.toByteArray());
    }
}
