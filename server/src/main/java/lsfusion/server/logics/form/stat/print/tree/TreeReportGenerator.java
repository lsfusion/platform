package lsfusion.server.logics.form.stat.print.tree;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.file.RawFileData;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
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

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet();
        sheet.setRowSumsBelow(false);
        ExportXLSWriter.Styles styles = new ExportXLSWriter.Styles(workbook);

        List<PropertyDrawEntity> columns = new ArrayList<>();
        Map<GroupObjectEntity, Integer> groupColumnStart = new HashMap<>();
        for (GroupObjectEntity group : groups) {
            groupColumnStart.put(group, columns.size());
            for (PropertyDrawEntity prop : sources.hierarchy.getProperties(group))
                columns.add(prop);
        }

        writeHeader(sheet, workbook, formView, columns);

        WriteContext ctx = new WriteContext(sheet, styles, sources, columns);

        GroupObjectEntity firstGroup = groups.get(0);
        if (groups.size() == 1 && firstGroup.isParent != null && firstGroup.getOrderObjects().size() == 1) {
            // Recursive tree: TREE neTree ne PARENT parent(ne)
            writeRecursiveTreeRows(ctx, firstGroup, groupColumnStart.get(firstGroup), form);
        } else {
            // Multi-level tree: each group is a separate level
            writeGroupRows(ctx, groups, 0, groupColumnStart, MapFact.EMPTY(), SetFact.EMPTY());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return new RawFileData(out.toByteArray());
    }

    private static void writeHeader(XSSFSheet sheet, XSSFWorkbook workbook, FormView formView, List<PropertyDrawEntity> columns) {
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
    }

    private static void writeRecursiveTreeRows(WriteContext ctx, GroupObjectEntity group, int columnStart, FormInstance form) throws SQLException, SQLHandledException {
        StaticKeyData keyData = ctx.sources.keys.get(group);
        if (keyData == null || keyData.data.isEmpty())
            return;

        int columnCount = ctx.sources.hierarchy.getProperties(group).size();

        ObjectEntity objectEntity = group.getOrderObjects().single();
        InstanceFactory factory = form.instanceFactory;
        ObjectInstance objectInstance = factory.getInstance(objectEntity);
        PropertyObjectEntity<?> parentPropEntity = group.isParent.get(objectEntity);
        PropertyObjectInstance<?> parentPropInstance = factory.getInstance(parentPropEntity);
        ConcreteCustomClass concreteClass = resolveConcreteClass(objectEntity.baseClass);

        // First pass: read all parent values once and remember rows in their original (sorted) order.
        Map<Object, ImMap<ObjectEntity, Object>> rowsByKey = new LinkedHashMap<>();
        Map<Object, Object> childToParent = new LinkedHashMap<>();
        for (ImMap<ObjectEntity, Object> row : keyData.data) {
            Object childValue = row.get(objectEntity);
            rowsByKey.put(childValue, row);
            childToParent.put(childValue, readParent(parentPropInstance, objectInstance, childValue, concreteClass, form));
        }

        // Second pass: bucket children by parent. A parent that lies outside the data set makes the child a root.
        Map<Object, List<Object>> childrenByParent = new LinkedHashMap<>();
        childrenByParent.put(null, new ArrayList<>());
        for (Map.Entry<Object, Object> entry : childToParent.entrySet()) {
            Object parent = entry.getValue();
            Object effectiveParent = parent != null && rowsByKey.containsKey(parent) ? parent : null;
            childrenByParent.computeIfAbsent(effectiveParent, k -> new ArrayList<>()).add(entry.getKey());
        }

        for (Object root : childrenByParent.get(null))
            writeRecursiveNode(ctx, root, 0, columnStart, columnCount, rowsByKey, childrenByParent);
    }

    private static Object readParent(PropertyObjectInstance<?> parentPropInstance, ObjectInstance objectInstance,
                                     Object childValue, ConcreteCustomClass concreteClass, FormInstance form) throws SQLException, SQLHandledException {
        DataObject childKey = buildDataObject(childValue, concreteClass);
        if (childKey == null)
            return null;
        return parentPropInstance.getRemappedPropertyObject(MapFact.singleton(objectInstance, childKey), true).read(form);
    }

    private static void writeRecursiveNode(WriteContext ctx, Object nodeKey, int level, int columnStart, int columnCount,
                                           Map<Object, ImMap<ObjectEntity, Object>> rowsByKey,
                                           Map<Object, List<Object>> childrenByParent) {
        writeRow(ctx, rowsByKey.get(nodeKey), level, columnStart, columnCount);

        List<Object> children = childrenByParent.get(nodeKey);
        if (children != null) {
            for (Object child : children)
                writeRecursiveNode(ctx, child, level + 1, columnStart, columnCount, rowsByKey, childrenByParent);
        }
    }

    private static void writeGroupRows(WriteContext ctx, ImOrderSet<GroupObjectEntity> groups, int level,
                                       Map<GroupObjectEntity, Integer> groupColumnStart,
                                       ImMap<ObjectEntity, Object> parentKey, ImSet<ObjectEntity> parentObjects) {
        GroupObjectEntity group = groups.get(level);
        StaticKeyData keyData = ctx.sources.keys.get(group);
        if (keyData == null)
            return;

        int columnStart = groupColumnStart.get(group);
        int columnCount = ctx.sources.hierarchy.getProperties(group).size();
        ImSet<ObjectEntity> nextParentObjects = parentObjects.merge(keyData.objects.getSet());

        for (ImMap<ObjectEntity, Object> row : keyData.data) {
            if (!parentObjects.isEmpty() && !row.filterIncl(parentObjects).equals(parentKey))
                continue;

            writeRow(ctx, row, level, columnStart, columnCount);

            if (level + 1 < groups.size())
                writeGroupRows(ctx, groups, level + 1, groupColumnStart, row.filterIncl(nextParentObjects), nextParentObjects);
        }
    }

    private static void writeRow(WriteContext ctx, ImMap<ObjectEntity, Object> row, int level, int columnStart, int columnCount) {
        XSSFRow xRow = ctx.sheet.createRow(ctx.rowCounter++);
        if (level > 0)
            xRow.getCTRow().setOutlineLevel((short) Math.min(level, 7)); // XLSX caps outline at 7; deeper levels collapse onto level 7

        for (int i = 0; i < columnCount; i++) {
            PropertyDrawEntity prop = ctx.columns.get(columnStart + i);
            Object value = StaticPropertyData.getProperty(ctx.sources.properties, prop, row);
            Cell cell = xRow.createCell(columnStart + i);
            Type type = ctx.sources.properties.types.get(prop);
            if (value != null && type != null)
                type.formatXLS(value, cell, ctx.styles);
        }
    }

    private static ConcreteCustomClass resolveConcreteClass(ValueClass baseValueClass) {
        if (baseValueClass instanceof ConcreteCustomClass)
            return (ConcreteCustomClass) baseValueClass;
        if (baseValueClass instanceof CustomClass) {
            MSet<ConcreteCustomClass> mSet = SetFact.mSet();
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

    private static class WriteContext {
        final XSSFSheet sheet;
        final ExportXLSWriter.Styles styles;
        final FormDataManager.ExportResult sources;
        final List<PropertyDrawEntity> columns;
        int rowCounter = 1; // row 0 is header

        WriteContext(XSSFSheet sheet, ExportXLSWriter.Styles styles, FormDataManager.ExportResult sources, List<PropertyDrawEntity> columns) {
            this.sheet = sheet;
            this.styles = styles;
            this.sources = sources;
            this.columns = columns;
        }
    }
}
