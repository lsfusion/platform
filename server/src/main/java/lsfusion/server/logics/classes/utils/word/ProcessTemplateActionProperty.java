package lsfusion.server.logics.classes.utils.word;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class ProcessTemplateActionProperty extends ScriptingActionProperty {
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

                ObjectValue fileObjectValue = findProperty("file[Template]").readClasses(context, templateObject);
                if (fileObjectValue instanceof DataObject) {

                    DataObject wordObject = (DataObject)fileObjectValue;
                    List<TemplateEntry> listTemplateEntriesList = new ArrayList<>();
                    List<TemplateEntry> templateEntriesList = new ArrayList<>();

                    KeyExpr templateEntryExpr = new KeyExpr("TemplateEntry");
                    ImRevMap<Object, KeyExpr> templateEntryKeys = MapFact.singletonRev((Object) "TemplateEntry", templateEntryExpr);

                    QueryBuilder<Object, Object> templateEntryQuery = new QueryBuilder<>(templateEntryKeys);
                    String[] templateEntryNames = new String[]{"objValue", "key", "value", "type", "firstRow", "columnSeparator", "rowSeparator"};
                    LCP[] templateEntryProperties = findProperties("objValue[TemplateEntry]", "key[TemplateEntry]", "value[TemplateEntry]", "idType[TemplateEntry]",
                            "firstRow[TemplateEntry]", "columnSeparator[TemplateEntry]", "rowSeparator[TemplateEntry]");
                    for (int i = 0; i < templateEntryProperties.length; i++) {
                        templateEntryQuery.addProperty(templateEntryNames[i], templateEntryProperties[i].getExpr(context.getModifier(), templateEntryExpr));
                    }

                    templateEntryQuery.and(findProperty("template[TemplateEntry]").getExpr(context.getModifier(), templateEntryQuery.getMapExprs().get("TemplateEntry")).compare(templateObject.getExpr(), Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> templateEntryResult = templateEntryQuery.execute(context, MapFact.singletonOrder((Object) "objValue", false));

                    for (ImMap<Object, Object> templateEntry : templateEntryResult.values()) {

                        String key = (String) templateEntry.get("key");
                        String value = (String) templateEntry.get("value");
                        String type = (String) templateEntry.get("type");
                        Integer firstRow = (Integer) templateEntry.get("firstRow");
                        String columnSeparator = (String) templateEntry.get("columnSeparator");
                        String rowSeparator = (String) templateEntry.get("rowSeparator");

                        if (key != null && value != null) {
                            TemplateEntry entry = new TemplateEntry(key, value.replace('\n', '\r'), type, firstRow, columnSeparator, rowSeparator);
                            if(entry.isList()) {
                                listTemplateEntriesList.add(entry);
                            } else {
                                templateEntriesList.add(entry);
                            }
                        }
                    }

                    RawFileData fileObject = (RawFileData) fileObjectValue.getValue();
                    byte[] bytes = fileObject.getBytes();
                    boolean isDocx = bytes.length > 2 && bytes[0] == 80 && bytes[1] == 75;

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                    if (isDocx) {
                        XWPFDocument document = new XWPFDocument(((RawFileData) wordObject.object).getInputStream());
                        for (TemplateEntry entry : listTemplateEntriesList) {
                            replaceListDataDocx(document, entry);
                            try(ByteArrayOutputStream os = new ByteArrayOutputStream()) { //save and reopen
                                document.write(os);
                                document = new XWPFDocument(new ByteArrayInputStream(os.toByteArray()));
                            }
                        }

                        for (TemplateEntry entry : templateEntriesList) {
                            for (XWPFTable tbl : document.getTables()) {
                                replaceTableDataDocx(tbl, entry);
                            }
                            replaceInParagraphs(document, entry.key, entry.value);
                        }
                        document.write(outputStream);
                    } else {
                        HWPFDocument document = new HWPFDocument(new POIFSFileSystem(((RawFileData) wordObject.object).getInputStream()));
                        Range range = document.getRange();
                        listTemplateEntriesList.addAll(templateEntriesList);
                        for (TemplateEntry entry : listTemplateEntriesList) {
                            range.replaceText(entry.key, entry.value);
                        }
                        document.write(outputStream);
                    }

                    findProperty("resultTemplate[]").change(new RawFileData(outputStream), context);
                }
            }
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private void replaceTableDataDocx(XWPFTable tbl, TemplateEntry entry) {
        if(entry.isTable()) {
            XWPFTableRow row = tbl.getRow(entry.firstRow);
            if (row == null) return;
            XWPFTableCell cell = row.getCell(0);
            String text = cell.getText();
            if (text != null && text.contains(entry.key)) {
                String[] tableRows = entry.value.split(entry.rowSeparator);
                int i = entry.firstRow;
                for (String tableRow : tableRows) {
                    if (i == entry.firstRow) {
                        XWPFTableRow newRow = tbl.getRow(i);
                        int j = 0;
                        for (String tableCell : tableRow.split(entry.columnSeparator)) {
                            XWPFTableCell newCell = newRow.getTableICells().size() > j ? newRow.getCell(j) : newRow.createCell();
                            if (newCell.getText().isEmpty())
                                newCell.setText(tableCell);
                            else {
                                newCell.getParagraphs().get(0).getRuns().get(0).setText(tableCell, 0);
                            }
                            j++;
                        }
                    } else {
                        XWPFTableRow newRow = tbl.createRow();
                        int j = 0;
                        for (String tableCell : tableRow.split(entry.columnSeparator)) {
                            XWPFTableCell newCell = newRow.getTableICells().size() > j ? newRow.getCell(j) : newRow.createCell();
                            newCell.setText(tableCell);
                            j++;
                        }
                    }
                    i++;
                }
            }
        } else {
            for (XWPFTableRow row : tbl.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        for (XWPFRun r : p.getRuns()) {
                            String text = r.getText(0);
                            if (text != null && text.contains(entry.key)) {
                                text = text.replace(entry.key, entry.value);
                                r.setText(text, 0);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void replaceInParagraphs(XWPFDocument document, String find, String repl) {

        Set<XWPFParagraph> toDelete = new HashSet<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();

            TextSegement found = paragraph.searchText(find, new PositionInParagraph());
            if (found != null) {
                if (found.getBeginRun() == found.getEndRun()) {
                    // whole search string is in one Run
                    XWPFRun run = runs.get(found.getBeginRun());
                    String runText = run.getText(run.getTextPosition());
                    String replaced = runText.replace(find, repl);
                    setText(run, replaced);
                } else {
                    // The search string spans over more than one Run
                    // Put the Strings together
                    StringBuilder b = new StringBuilder();
                    for (int runPos = found.getBeginRun(); runPos <= found.getEndRun(); runPos++) {
                        XWPFRun run = runs.get(runPos);
                        b.append(run.getText(run.getTextPosition()));
                    }
                    String connectedRuns = b.toString();
                    String replaced = connectedRuns.replace(find, repl);

                    // The first Run receives the replaced String of all connected Runs
                    XWPFRun partOne = runs.get(found.getBeginRun());
                    setText(partOne, replaced);
                    // Removing the text in the other Runs.
                    for (int runPos = found.getBeginRun() + 1; runPos <= found.getEndRun(); runPos++) {
                        XWPFRun partNext = runs.get(runPos);
                        partNext.setText("", 0);
                    }
                }
                if (paragraph.getText().isEmpty())
                    toDelete.add(paragraph);
            }
        }
        for (XWPFParagraph paragraph : toDelete) {
            document.removeBodyElement(document.getPosOfParagraph(paragraph));
        }
    }

    private void replaceListDataDocx(XWPFDocument document, TemplateEntry entry) {
        List<XWPFParagraph> docParagraphs = new ArrayList<>(document.getParagraphs()); //copy of mutable list
        for (XWPFParagraph p : docParagraphs) {
            if (p.getNumID() != null) { //part of numerator
                String pText = p.getText();
                if (pText != null && pText.equals(entry.key)) {
                    if(entry.value.isEmpty()) {
                        document.removeBodyElement(document.getPosOfParagraph(p));
                    } else {
                        XmlCursor cursor = p.getCTP().newCursor();
                        for (String row : entry.value.split(entry.rowSeparator)) {
                            XWPFParagraph newParagraph = document.createParagraph();
                            newParagraph.getCTP().setPPr(p.getCTP().getPPr());
                            XWPFRun newRun = newParagraph.createRun();
                            newRun.getCTR().setRPr(p.getRuns().get(0).getCTR().getRPr());
                            newRun.setText(row, 0);
                            XmlCursor newCursor = newParagraph.getCTP().newCursor();
                            newCursor.moveXml(cursor);
                            newCursor.dispose();
                        }
                        cursor.removeXml(); // Removes replacement text paragraph
                        cursor.dispose();
                    }
                }
            }
        }
    }
    
    private void setText(XWPFRun run, String newText) {
        List<String> splitted = BaseUtils.split(newText,"\r");
        for (int j = 0; j < splitted.size(); j++) {
            if (j > 0) {
                run.addBreak();
                run.setText(splitted.get(j));
            } else
                run.setText(splitted.get(j), 0);
        }
    }

    private class TemplateEntry {
        public String key;
        public String value;
        public String type;
        public Integer firstRow;
        public String columnSeparator;
        public String rowSeparator;

        public TemplateEntry(String key, String value, String type, Integer firstRow, String columnSeparator, String rowSeparator) {
            this.key = key;
            this.value = value;
            this.type = type;
            this.firstRow = firstRow;
            this.columnSeparator = columnSeparator;
            this.rowSeparator = rowSeparator;
        }

        public boolean isTable() {
          return type != null && type.endsWith("table");
        }

        public boolean isList() {
           return type != null && type.endsWith("list");
        }
    }
}