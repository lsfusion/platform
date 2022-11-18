package lsfusion.server.logics.classes.data.utils.word;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.nvl;

public class ProcessTemplateAction extends InternalAction {
    public final ClassPropertyInterface templateInterface;

    public ProcessTemplateAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        templateInterface = i.next();

    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            DataObject templateObject = context.getDataKeyValue(templateInterface);

            if (templateObject != null) {

                ObjectValue fileObjectValue = findProperty("file[Template]").readClasses(context, templateObject);
                if (fileObjectValue instanceof DataObject) {

                    DataObject wordObject = (DataObject)fileObjectValue;
                    List<TemplateEntry> listTemplateEntriesList = new ArrayList<>();
                    List<TemplateEntry> templateEntriesList = new ArrayList<>();

                    KeyExpr templateEntryExpr = new KeyExpr("TemplateEntry");
                    ImRevMap<Object, KeyExpr> templateEntryKeys = MapFact.singletonRev("TemplateEntry", templateEntryExpr);

                    QueryBuilder<Object, Object> templateEntryQuery = new QueryBuilder<>(templateEntryKeys);
                    String[] templateEntryNames = new String[]{"objValue", "key", "value", "type", "columnSeparator", "rowSeparator"};
                    LP[] templateEntryProperties = findProperties("objValue[TemplateEntry]", "key[TemplateEntry]", "value[TemplateEntry]", "idType[TemplateEntry]",
                            "columnSeparator[TemplateEntry]", "rowSeparator[TemplateEntry]");
                    for (int i = 0; i < templateEntryProperties.length; i++) {
                        templateEntryQuery.addProperty(templateEntryNames[i], templateEntryProperties[i].getExpr(context.getModifier(), templateEntryExpr));
                    }

                    templateEntryQuery.and(findProperty("template[TemplateEntry]").getExpr(context.getModifier(), templateEntryQuery.getMapExprs().get("TemplateEntry")).compare(templateObject.getExpr(), Compare.EQUALS));

                    ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> templateEntryResult = templateEntryQuery.execute(context, MapFact.singletonOrder("objValue", false));

                    for (ImMap<Object, Object> templateEntry : templateEntryResult.values()) {

                        String key = (String) templateEntry.get("key");
                        String value = (String) templateEntry.get("value");

                        String hyperLink = null;
                        if(value != null) {
                            //url|text
                            Pattern pattern = Pattern.compile("((?:http|https):.*)\\|(.*)");
                            Matcher m = pattern.matcher(value);
                            if(m.matches()) {
                                hyperLink = m.group(1);
                                value = m.group(2);
                            }
                        }

                        String type = (String) templateEntry.get("type");
                        String columnSeparator = (String) templateEntry.get("columnSeparator");
                        String rowSeparator = (String) templateEntry.get("rowSeparator");

                        if (key != null && value != null) {
                            TemplateEntry entry = new TemplateEntry(key, value.replace('\n', '\r'), hyperLink, type, columnSeparator, rowSeparator);
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
                            replaceInParagraphs(document, entry.key, entry.value, entry.link);
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
            int numberOfRows = tbl.getNumberOfRows();
            for(int rowIndex = 0; rowIndex < numberOfRows; rowIndex++) {
                XWPFTableRow row = tbl.getRow(rowIndex);
                if (row == null) return;
                XWPFTableCell cell = row.getCell(0);
                String text = cell.getText();
                if (text != null && text.contains(entry.key)) {
                    String[] tableRows = entry.value.split(entry.rowSeparator);
                    int i = rowIndex;
                    boolean firstRow = true;

                    String fontFamily = null;
                    Integer fontSize = null;
                    String color = null;
                    boolean bold = false;
                    boolean italic = false;
                    XWPFParagraph templateParagraph = row.getCell(0).getParagraphArray(0);
                    if(templateParagraph != null) {
                        XWPFRun templateRun = templateParagraph.getRuns().get(0);
                        if(templateRun != null) {
                            fontFamily = templateRun.getFontFamily();
                            int fontSizeValue = templateRun.getFontSize();
                            if(fontSizeValue > 0)
                                fontSize = fontSizeValue;
                            color = templateRun.getColor();
                            bold = templateRun.isBold();
                            italic = templateRun.isItalic();
                        }
                    }

                    for (String tableRow : tableRows) {
                        XWPFTableRow newRow = firstRow ? tbl.getRow(i) : tbl.insertNewTableRow(i);
                        int j = 0;
                        for (CellValue cellValue : parseTableRow(tableRow, entry.columnSeparator, row, fontFamily, fontSize, color, bold, italic)) {
                            XWPFTableCell newCell = newRow.getTableICells().size() > j ? newRow.getCell(j) : newRow.createCell();
                            XWPFParagraph paragraph = newCell.getParagraphs().get(0);
                            if(cellValue.alignment != null) paragraph.setAlignment(cellValue.alignment);
                            XWPFRun run = newCell.getText().isEmpty() ? paragraph.createRun() : paragraph.getRuns().get(0);
                            if (cellValue.fontFamily != null) run.setFontFamily(cellValue.fontFamily);
                            if (cellValue.fontSize != null) run.setFontSize(cellValue.fontSize);
                            if (cellValue.color != null) run.setColor(cellValue.color);
                            run.setBold(cellValue.bold);
                            run.setItalic(cellValue.italic);
                            setText(run, cellValue.text);
                            j++;
                        }
                        i++;
                        firstRow = false;
                    }
                    break;
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
                                setText(r, text);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void replaceInParagraphs(XWPFDocument document, String find, String repl, String link) {

        Set<XWPFParagraph> toDelete = new HashSet<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();

            if(link != null) {
                //poi-ooxml 4.1.2 can't replace hyperlink, only to remove and create new one.
                //key must be in separate run and be not hyperlink (can't remove hyperLink run)
                //new run will be inserted to the end of paragraph
                for(int i = 0; i < runs.size(); i++){
                    XWPFRun run = runs.get(i);
                    String runText = run.getText(run.getTextPosition());

                    if(runText != null && runText.contains(find)) {
                        String replaced = runText.replace(find, repl);

                        String fontFamily = run.getFontFamily();
                        int fontSize = run.getFontSize();
                        String color = nvl(run.getColor(), "0000FF"); //default blue
                        boolean bold = run.isBold();
                        boolean italic = run.isItalic();

                        paragraph.removeRun(i);
                        XWPFHyperlinkRun newRun = paragraph.createHyperlinkRun(link);

                        newRun.setFontFamily(fontFamily);
                        newRun.setFontSize(fontSize);
                        newRun.setBold(bold);
                        newRun.setItalic(italic);

                        newRun.setColor(color);
                        newRun.setUnderline(UnderlinePatterns.SINGLE);
                        newRun.setUnderlineColor(color);

                        setText(newRun, replaced);
                    }
                }
            } else {
                TextSegment found = paragraph.searchText(find, new PositionInParagraph());
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
                    if (paragraph.getText().isEmpty()) {
                        toDelete.add(paragraph);
                    }
                }
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
                            setText(newRun, row);
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
        public String link;
        public String type;
        public String columnSeparator;
        public String rowSeparator;

        public TemplateEntry(String key, String value, String link, String type, String columnSeparator, String rowSeparator) {
            this.key = key;
            this.value = value;
            this.link = link;
            this.type = type;
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

    private List<CellValue> parseTableRow(String tableRow, String columnSeparator, XWPFTableRow templateRow, String fontFamily, Integer fontSize, String color, boolean bold, boolean italic) {
        List<CellValue> result = new ArrayList<>();
        int cellIndex = 0;
        for (String tableCell : tableRow.split(columnSeparator)) {
            ParagraphAlignment alignment = templateRow.getCell(cellIndex).getParagraphArray(0).getAlignment();
            result.add(parseCellValue(alignment, tableCell, fontFamily, fontSize, color, bold, italic));
            cellIndex++;
        }
        return result;
    }

    private CellValue parseCellValue(ParagraphAlignment alignment, String value, String fontFamily, Integer fontSize, String color, boolean bold, boolean italic) {
        boolean matches = value != null && value.startsWith("<b>") && value.endsWith("</b>");
        if (matches)
            value = value.substring(3, value.length() - 4);
        return new CellValue(alignment, fontFamily, fontSize, color, bold || matches, italic, value);
    }

    private class CellValue {
        public ParagraphAlignment alignment;
        public String fontFamily;
        public Integer fontSize;
        public String color;
        public boolean bold;
        public boolean italic;
        public String text;

        public CellValue(ParagraphAlignment alignment, String fontFamily, Integer fontSize, String color, boolean bold, boolean italic, String text) {
            this.alignment = alignment;
            this.fontFamily = fontFamily;
            this.fontSize = fontSize;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.text = text;
        }
    }
}