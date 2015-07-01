package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
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
            //cut BOM
            if(!line.isEmpty() && line.charAt(0) == '\uFEFF')
                line = line.substring(1);
            String[] splittedLine = line.split(String.format("\\%s|;", separator));
            return Arrays.asList(splittedLine).subList(0, Math.min(splittedLine.length, propertiesCount));
        }
        return null;
    }

    @Override
    protected void release() {
        scanner.close();
    }
}
