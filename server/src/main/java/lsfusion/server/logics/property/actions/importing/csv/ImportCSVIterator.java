package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.base.ExternalUtils;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ImportCSVIterator extends ImportIterator {
    private final List<Integer> columns;
    private final List<LCP> properties;
    private final String charset;
    private final String separator;
    private Scanner scanner;
    private int row;
    private final int lastRow;

    public ImportCSVIterator(byte[] file, List<Integer> columns, List<LCP> properties, String charset, String separator, boolean noHeader) {
        this.columns = columns;
        this.properties = properties;
        this.charset = charset == null ? ExternalUtils.defaultCSVCharset : charset;
        this.separator = separator == null ? ";" : separator;

        this.row = 0;
        this.lastRow = getLastRow(file, noHeader);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(file);
        scanner = new Scanner(inputStream, this.charset);

        if (!noHeader && lastRow > 0) {
            scanner.nextLine();
        }
    }

    @Override
    public List<String> nextRow() {
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            row++;
            if(row > lastRow)
                return null;
            else {
                //cut BOM
                if (!line.isEmpty() && line.charAt(0) == '\uFEFF')
                    line = line.substring(1);
                String[] splittedLine = line.split(String.format("\\%s|;", separator));
                List<String> result = new ArrayList<>();
                for (Integer column : columns) {
                    result.add(formatValue(properties, columns, column, splittedLine.length > column ? splittedLine[column] : ""));
                }
                return result;
            }
        }
        return null;
    }

    @Override
    protected void release() {
        scanner.close();
    }

    private int getLastRow(byte[] file, boolean noHeader) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(file);
        scanner = new Scanner(inputStream, charset);

        int lastNonEmptyRow = 0;
        if(scanner.hasNextLine()) { //защита от пустых файлов
            if (!noHeader) {
                scanner.nextLine();
            }

            int row = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                row++;
                if (!line.isEmpty())
                    lastNonEmptyRow = row;
            }
        }
        return lastNonEmptyRow;
    }

    private String formatValue(List<LCP> properties, List<Integer> columns, Integer column, String value) {
        DateFormat dateFormat = getDateFormat(properties, columns, column);
        if (dateFormat != null && value != null) {
            value = parseFormatDate(dateFormat, value);
        }
        return value;
    }
}