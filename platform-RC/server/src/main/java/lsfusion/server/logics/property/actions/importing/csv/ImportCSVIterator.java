package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ImportCSVIterator extends ImportIterator {
    private final List<Integer> columns;
    private final String separator;
    private Scanner scanner;

    public ImportCSVIterator(byte[] file, List<Integer> columns, String charset, String separator, boolean noHeader) {
        this.columns = columns;
        this.separator = separator;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(file);
        scanner = charset == null ? new Scanner(inputStream, "UTF-8") : new Scanner(inputStream, charset);
        
        if(!noHeader) {
            scanner.nextLine();
        }
    }

    @Override
    public List<String> nextRow() {
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            //cut BOM
            if(!line.isEmpty() && line.charAt(0) == '\uFEFF')
                line = line.substring(1);
            String[] splittedLine = line.split(String.format("\\%s|;", separator));
            List<String> result = new ArrayList<>();
            for(Integer column : columns) {
                result.add(splittedLine.length > column ? splittedLine[column] : "");
            }
            return result;
        }
        return null;
    }

    @Override
    protected void release() {
        scanner.close();
    }
}