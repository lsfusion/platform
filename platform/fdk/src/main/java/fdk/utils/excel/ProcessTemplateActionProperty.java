package fdk.utils.excel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.server.classes.ExcelClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ProcessTemplateActionProperty extends ScriptingActionProperty {
    public final ClassPropertyInterface templateInterface;

    public ProcessTemplateActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.getClassByName("Template")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        templateInterface = i.next();

    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            DataObject templateObject = context.getKeyValue(templateInterface);

            if (templateObject != null) {

                Object fileObject = LM.findLCPByCompoundName("fileTemplate").read(context, templateObject);
                if (fileObject != null) {

                    DataObject excelObject = new DataObject(LM.findLCPByCompoundName("fileTemplate").read(context, templateObject), ExcelClass.get(false, false));
                    Map<String, String> templateEntriesMap = new HashMap<String, String>();

                    KeyExpr templateEntryExpr = new KeyExpr("TemplateEntry");
                    ImRevMap<Object, KeyExpr> templateEntryKeys = MapFact.singletonRev((Object) "TemplateEntry", templateEntryExpr);

                    QueryBuilder<Object, Object> templateEntryQuery = new QueryBuilder<Object, Object>(templateEntryKeys);
                    templateEntryQuery.addProperty("keyTemplateEntry", getLCP("keyTemplateEntry").getExpr(context.getModifier(), templateEntryExpr));
                    templateEntryQuery.addProperty("valueTemplateEntry", getLCP("valueTemplateEntry").getExpr(context.getModifier(), templateEntryExpr));

                    templateEntryQuery.and(getLCP("templateTemplateEntry").getExpr(context.getModifier(), templateEntryQuery.getMapExprs().get("TemplateEntry")).compare(templateObject.getExpr(), Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> templateEntryResult = templateEntryQuery.execute(context.getSession().sql);

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

                    LM.findLCPByCompoundName("resultTemplate").change(outputStream.toByteArray(), context);
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