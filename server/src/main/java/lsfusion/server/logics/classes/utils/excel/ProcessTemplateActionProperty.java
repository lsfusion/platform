package lsfusion.server.logics.classes.utils.excel;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static lsfusion.base.BaseUtils.trim;

public class ProcessTemplateActionProperty extends ScriptingAction {
    public final ClassPropertyInterface templateInterface;

    public ProcessTemplateActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
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

                    String[] names = new String[]{"key", "value", "isTable", "rowSeparator", "isNumeric", "format"};
                    LP[] properties = findProperties("key[TemplateEntry]", "value[TemplateEntry]", "isTable[TemplateEntry]",
                            "rowSeparator[TemplateEntry]", "isNumeric[TemplateEntry]", "format[TemplateEntry]");
                    for (int j = 0; j < properties.length; j++) {
                        templateEntryQuery.addProperty(names[j], properties[j].getExpr(context.getModifier(), templateEntryExpr));
                    }

                    templateEntryQuery.and(findProperty("template[TemplateEntry]").getExpr(context.getModifier(), templateEntryQuery.getMapExprs().get("TemplateEntry")).compare(templateObject.getExpr(), Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> templateEntryResult = templateEntryQuery.execute(context);

                    for (ImMap<Object, Object> templateEntry : templateEntryResult.values()) {

                        String key = trim((String) templateEntry.get("key"));
                        String value = trim((String) templateEntry.get("value"));
                        boolean isTable = templateEntry.get("isTable") != null;
                        String rowSeparator = (String) templateEntry.get("rowSeparator");
                        boolean isNumeric = templateEntry.get("isNumeric") != null;
                        String format = trim((String) templateEntry.get("format"));

                        if (key != null && value != null)
                            templateEntriesList.add(new TemplateEntry(key, value, isTable, rowSeparator, isNumeric, format));
                    }

                    InputStream inputStream = ((RawFileData) excelObject.object).getInputStream();

                    Workbook wb = WorkbookFactory.create(inputStream);

                    for (TemplateEntry templateEntry : templateEntriesList) {
                        replaceData(wb, templateEntry);
                    }

                    //после изменения данных пересчитаем все формулы
                    wb.getCreationHelper().createFormulaEvaluator().evaluateAll();

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    wb.write(outputStream);

                    findProperty("resultTemplate[]").change(new RawFileData(outputStream), context);
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
                            DataFormat dataFormat = wb.createDataFormat();
                            if (cellContents.contains(templateEntry.key)) {
                                if (templateEntry.isTable) {
                                    String[] rows = templateEntry.value.split(templateEntry.rowSeparator);
                                    for (int r = 0; r < rows.length; r++) {
                                        if (r == 0) {
                                            cellContents = cellContents.replace(templateEntry.key, rows[r]);
                                            setCellValue(cell, rows[r], templateEntry, dataFormat);
                                        } else {
                                            Row newRow = sheet.getRow(j + r);
                                            if (newRow == null)
                                                newRow = sheet.createRow(j + r);
                                            Cell newCell = newRow.getCell(k);
                                            if (newCell == null)
                                                newCell = newRow.createCell(k);
                                            setCellValue(newCell, rows[r], templateEntry, dataFormat);
                                        }
                                    }
                                } else {
                                    cellContents = cellContents.replace(templateEntry.key, templateEntry.value);
                                    setCellValue(cell, cellContents, templateEntry, dataFormat);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void setCellValue(Cell cell, String value, TemplateEntry templateEntry, DataFormat dataFormat) {

        if (templateEntry.isNumeric) {
            cell.setCellValue(Double.parseDouble(value));
            cell.setCellType(XSSFCell.CELL_TYPE_NUMERIC);
        } else {
            cell.setCellValue(value);
        }
        if (templateEntry.format != null)
            cell.getCellStyle().setDataFormat(dataFormat.getFormat(templateEntry.format));
    }

    private class TemplateEntry {
        String key;
        String value;
        boolean isTable;
        String rowSeparator;
        boolean isNumeric;
        String format;

        public TemplateEntry(String key, String value, boolean isTable, String rowSeparator, boolean isNumeric, String format) {
            this.key = key;
            this.value = value;
            this.isTable = isTable;
            this.rowSeparator = rowSeparator;
            this.isNumeric = isNumeric;
            this.format = format;
        }
    }
}