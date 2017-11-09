package lsfusion.erp.utils.excel;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static lsfusion.base.BaseUtils.trim;

public class ProcessTemplateActionProperty extends ScriptingActionProperty {
    public final ClassPropertyInterface templateInterface;

    public ProcessTemplateActionProperty(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        templateInterface = i.next();

    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataObject templateObject = context.getDataKeyValue(templateInterface);

            if (templateObject != null) {

                Object fileObject = findProperty("file[Template]").read(context, templateObject);
                if (fileObject != null) {

                    DataObject excelObject = (DataObject) findProperty("file[Template]").readClasses(context, templateObject);
                    List<TemplateEntry> templateEntriesList = new ArrayList<>();

                    KeyExpr templateEntryExpr = new KeyExpr("TemplateEntry");
                    ImRevMap<Object, KeyExpr> templateEntryKeys = MapFact.singletonRev((Object) "TemplateEntry", templateEntryExpr);

                    QueryBuilder<Object, Object> templateEntryQuery = new QueryBuilder<>(templateEntryKeys);
                    templateEntryQuery.addProperty("key", findProperty("key[TemplateEntry]").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("value", findProperty("value[TemplateEntry]").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("isTable", findProperty("isTable[TemplateEntry]").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("rowSeparator", findProperty("rowSeparator[TemplateEntry]").getExpr(context.getModifier(), templateEntryExpr));

                    templateEntryQuery.and(findProperty("template[TemplateEntry]").getExpr(context.getModifier(), templateEntryQuery.getMapExprs().get("TemplateEntry")).compare(templateObject.getExpr(), Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> templateEntryResult = templateEntryQuery.execute(context);

                    for (ImMap<Object, Object> templateEntry : templateEntryResult.values()) {

                        String key = trim((String) templateEntry.get("key"));
                        String value = trim((String) templateEntry.get("value"));
                        boolean isTable = templateEntry.get("isTable") != null;
                        String rowSeparator = (String) templateEntry.get("rowSeparator");

                        if (key != null && value != null)
                            templateEntriesList.add(new TemplateEntry(key, value, isTable, rowSeparator));
                    }

                    ByteArrayInputStream inputStream = new ByteArrayInputStream((byte[]) excelObject.object);

                    Workbook wb = WorkbookFactory.create(inputStream);

                    for (TemplateEntry templateEntry : templateEntriesList) {
                        replaceData(wb, templateEntry);
                    }

                    //после изменения данных пересчитаем все формулы
                    wb.getCreationHelper().createFormulaEvaluator().evaluateAll();

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    wb.write(outputStream);

                    findProperty("resultTemplate[]").change(outputStream.toByteArray(), context);
                }
            }

        } catch (ScriptingErrorLog.SemanticErrorException | InvalidFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void replaceData(Workbook wb, TemplateEntry templateEntry) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);
            for (int j = sheet.getFirstRowNum(); j <= sheet.getLastRowNum(); j++) {
                Row row = sheet.getRow(j);
                if (row != null) {
                    for (int k = row.getFirstCellNum(); k <= row.getLastCellNum(); k++) {
                        Cell cell = row.getCell(k);
                        //если вдруг понадобится заменять ячейки не строкового типа, будем думать, но пока это представляется крайне маловероятным
                        if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
                            String cellContents = cell.getStringCellValue();
                            if (templateEntry.isTable) {
                                if (cellContents.contains(templateEntry.key)) {
                                    String[] rows = templateEntry.value.split(templateEntry.rowSeparator);
                                    for (int r = 0; r < rows.length; r++) {
                                        if (r == 0) {
                                            cellContents = cellContents.replace(templateEntry.key, rows[r]);
                                            cell.setCellValue(cellContents);
                                        } else {
                                            Row newRow = sheet.getRow(j + r);
                                            if (newRow == null)
                                                newRow = sheet.createRow(j + r);
                                            Cell newCell = newRow.getCell(k);
                                            if (newCell == null)
                                                newCell = newRow.createCell(k);
                                            newCell.setCellValue(rows[r]);
                                        }
                                    }
                                }
                            } else {
                                cellContents = cellContents.replace(templateEntry.key, templateEntry.value);
                                cell.setCellValue(cellContents);
                            }
                        }
                    }
                }
            }
        }
    }

    private class TemplateEntry {
        String key;
        String value;
        boolean isTable;
        String rowSeparator;

        public TemplateEntry(String key, String value, boolean isTable, String rowSeparator) {
            this.key = key;
            this.value = value;
            this.isTable = isTable;
            this.rowSeparator = rowSeparator;
        }
    }
}