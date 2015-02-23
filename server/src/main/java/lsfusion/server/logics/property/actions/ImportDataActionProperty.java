package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.ImportSourceFormat;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdom.input.SAXBuilder;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ImportDataActionProperty extends ScriptingActionProperty {
    private final ImportSourceFormat format;
    private final List<LCP> properties;

    public ImportDataActionProperty(ValueClass valueClass, ImportSourceFormat format, ScriptingLogicsModule LM, List<LCP> properties) {
        super(LM, valueClass);
        this.format = format;
        this.properties = properties;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject value = context.getDataKeys().getValue(0);
        assert value.getType() instanceof FileClass;
        
        Object file = value.object;
        if (file instanceof byte[]) {
            try {
                List<List<String>> table = null;
                if (format == ImportSourceFormat.XLS) {
                    table = getXLSTable((byte[]) file);
                } else if (format == ImportSourceFormat.XLSX) {
                    table = getXLSXTable((byte[]) file);
                } else if (format == ImportSourceFormat.DBF) {
                    table = getDBFTable((byte[]) file);
                } else if (format == ImportSourceFormat.CSV) {
                    table = getCSVTable((byte[]) file);
                } else if (format == ImportSourceFormat.XML) {
                    table = getXMLTable((byte[]) file);
                }
                
                if (table != null) {
                    for (List<String> row : table) {
                        DataObject rowKey = new DataObject(table.indexOf(row), IntegerClass.instance);
                        LM.baseLM.imported.change(true, context, rowKey);
                        for (int i = 0; i < Math.min(properties.size(), row.size()); i++) {
                            LCP property = properties.get(i);
                            Type type = property.property.getType();
                            property.change(type.parseString(row.get(i)), context, rowKey);
                        }
                    }
                }
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }   
    }
    
    private List<List<String>> getXLSTable(byte[] file) throws IOException, ParseException {

        HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(file));
        HSSFSheet sheet = wb.getSheetAt(0);
        
        List<List<String>> result = new ArrayList<List<String>>();
        
        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            HSSFRow row = sheet.getRow(i);
            if (row != null) {
                List<String> listRow = new ArrayList<String>();
                for (int j = 0; j < Math.min(properties.size(), row.getLastCellNum()); j++) {
                    listRow.add(getXLSFieldValue(sheet, i, j, null));
                }
                result.add(listRow);
            }
        }
        
        return result;
    }

    private List<List<String>> getXLSXTable(byte[] file) throws IOException, ParseException {
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(file));
        XSSFSheet sheet = wb.getSheetAt(0);

        List<List<String>> result = new ArrayList<List<String>>();

        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);
            if (row != null) {
                List<String> listRow = new ArrayList<String>();
                for (int j = 0; j < Math.min(properties.size(), row.getLastCellNum()); j++) {
                    listRow.add(getXLSXFieldValue(sheet, i, j, null));
                }
                result.add(listRow);
            }
        }

        return result;
    }

    private List<List<String>> getDBFTable(byte[] file) throws IOException, xBaseJException {

        File tmpFile = null;
        DBF importFile = null;
        try {
            tmpFile = File.createTempFile("importDBF", ".dbf");
            Files.write(file, tmpFile);
            importFile = new DBF(tmpFile.getAbsolutePath());
            int recordCount = importFile.getRecordCount();

            List<List<String>> result = new ArrayList<List<String>>();

            for (int i = 0; i < recordCount; i++) {
                importFile.read();
                List<String> listRow = new ArrayList<String>();
                for (int j = 1; j <= importFile.getFieldCount(); j++) {
                    listRow.add(new String(importFile.getField(j).getBytes()));
                }
                result.add(listRow);
            }

            importFile.close();

            return result;
        } finally {
            if (tmpFile != null)
                tmpFile.delete();
            if (importFile != null)
                importFile.close();
        }
    }

    private List<List<String>> getCSVTable(byte[] file) {

        List<List<String>> result = new ArrayList<List<String>>();

        Scanner scanner = new Scanner(new ByteArrayInputStream(file));
        scanner.nextLine();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] splittedLine = line.split("\\|");
            List<String> listRow = new ArrayList<String>();
            for (String field : splittedLine) {
                listRow.add(field);
            }
            result.add(listRow);
        }
        return result;
    }

    private List<List<String>> getXMLTable(byte[] file) throws JDOMException, IOException {

        List<List<String>> result = new ArrayList<List<String>>();

        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new ByteArrayInputStream(file));
        Element rootNode = document.getRootElement();
        List childrenList = rootNode.getChildren();
        for (Object childNode : childrenList) {
            List attributes = ((Element)childNode).getAttributes();
            List<String> listRow = new ArrayList<String>();
            for(Object attribute : attributes) {
                String attributeValue = ((Attribute) attribute).getValue();
                listRow.add(attributeValue);
            }
            result.add(listRow);
            
        }
        return result;
    }

    protected String getXLSFieldValue(HSSFSheet sheet, int row, int cell, String defaultValue) throws ParseException {
        HSSFRow hssfRow = sheet.getRow(row);
        if (hssfRow == null) return defaultValue;
        HSSFCell hssfCell = hssfRow.getCell(cell);
        if (hssfCell == null) return defaultValue;
        switch (hssfCell.getCellType()) {
            case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC:
            case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA:
                String result;
                try {
                    result = new DecimalFormat("#.#####").format(hssfCell.getNumericCellValue());
                } catch (Exception e) {
                    result = hssfCell.getStringCellValue().isEmpty() ? defaultValue : hssfCell.getStringCellValue();
                }
                return result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
            case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING:
            default:
                return (hssfCell.getStringCellValue().isEmpty()) ? defaultValue : hssfCell.getStringCellValue();
        }
    }

    protected String getXLSXFieldValue(XSSFSheet sheet, Integer row, Integer cell, String defaultValue) throws ParseException {
        if (cell == null) return defaultValue;
        XSSFRow xssfRow = sheet.getRow(row);
        if (xssfRow == null) return defaultValue;
        XSSFCell xssfCell = xssfRow.getCell(cell);
        if (xssfCell == null) return defaultValue;
        String result;
        switch (xssfCell.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC:
                result = new DecimalFormat("#.#####").format(xssfCell.getNumericCellValue());
                result = result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
                break;
            case Cell.CELL_TYPE_FORMULA:
                result = xssfCell.getCellFormula();
                break;
            case Cell.CELL_TYPE_STRING:
            default:
                result = (xssfCell.getStringCellValue().isEmpty()) ? defaultValue : xssfCell.getStringCellValue().trim();
                break;
        }
        return result;
    }
}
