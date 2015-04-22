package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ImportCSVIterator extends ImportIterator {
    private final String separator;
    private final int propertiesCount;
    private Scanner scanner;

    public ImportCSVIterator(byte[] file, String charset, String separator, boolean noHeader, int propertiesCount) {
        this.separator = separator;
        this.propertiesCount = propertiesCount;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(file);
        scanner = charset == null ? new Scanner(inputStream) : new Scanner(inputStream, charset);
        
        if(!noHeader) {
            scanner.nextLine();
        }
    }

    @Override
    public List<String> nextRow() {
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] splittedLine = line.split(String.format("\\%s|;", separator));
            List<String> listRow = new ArrayList<String>();
            for (int i = 0; i < Math.min(splittedLine.length, propertiesCount); i++) {
                listRow.add(splittedLine[i]);
            }
            return listRow;
        }
        return null;
    }

    @Override
    protected void release() {
        scanner.close();
    }
}
