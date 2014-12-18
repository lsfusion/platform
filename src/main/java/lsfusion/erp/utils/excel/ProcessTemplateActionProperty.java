package lsfusion.erp.utils.excel;

import lsfusion.server.data.SQLHandledException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.Compare;
import lsfusion.server.classes.ExcelClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

                Object fileObject = findProperty("fileTemplate").read(context, templateObject);
                if (fileObject != null) {

                    DataObject excelObject = new DataObject(findProperty("fileTemplate").read(context, templateObject), ExcelClass.get(false, false));
                    Map<String, String> templateEntriesMap = new HashMap<String, String>();

                    KeyExpr templateEntryExpr = new KeyExpr("TemplateEntry");
                    ImRevMap<Object, KeyExpr> templateEntryKeys = MapFact.singletonRev((Object) "TemplateEntry", templateEntryExpr);

                    QueryBuilder<Object, Object> templateEntryQuery = new QueryBuilder<Object, Object>(templateEntryKeys);
                    templateEntryQuery.addProperty("keyTemplateEntry", findProperty("keyTemplateEntry").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("valueTemplateEntry", findProperty("valueTemplateEntry").getExpr(context.getModifier(), templateEntryExpr));

                    templateEntryQuery.and(findProperty("templateTemplateEntry").getExpr(context.getModifier(), templateEntryQuery.getMapExprs().get("TemplateEntry")).compare(templateObject.getExpr(), Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> templateEntryResult = templateEntryQuery.execute(context);

                    for (ImMap<Object, Object> templateEntry : templateEntryResult.values()) {

                        String keyTemplateEntry = (String) templateEntry.get("keyTemplateEntry");
                        String valueTemplateEntry = (String) templateEntry.get("valueTemplateEntry");

                        if (keyTemplateEntry != null && valueTemplateEntry != null)
                            templateEntriesMap.put(keyTemplateEntry.trim(), valueTemplateEntry.trim());
                    }

                    ByteArrayInputStream inputStream = new ByteArrayInputStream((byte[]) excelObject.object);

                    Workbook wb = WorkbookFactory.create(inputStream);
                    for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                        Sheet sheet = wb.getSheetAt(i);
                        for (int j = sheet.getFirstRowNum(); j <= sheet.getLastRowNum(); j++) {
                            Row row = sheet.getRow(j);
                            if (row != null) {
                                for (int k = row.getFirstCellNum(); k <= row.getLastCellNum(); k++) {
                                    Cell cell = row.getCell(k);
                                    if (cell != null) {
                                        String cellContents = cell.getStringCellValue();
                                        for (Map.Entry<String, String> entry : templateEntriesMap.entrySet()) {
                                            cellContents = cellContents.replace(entry.getKey(), entry.getValue());
                                        }
                                        cell.setCellValue(cellContents);
                                    }
                                }
                            }
                        }
                    }

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    wb.write(outputStream);

                    findProperty("resultTemplate").change(outputStream.toByteArray(), context);
                }
            }

        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}