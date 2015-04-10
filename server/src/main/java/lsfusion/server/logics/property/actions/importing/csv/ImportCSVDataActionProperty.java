package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jdom.JDOMException;
import org.xBaseJ.xBaseJException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ImportCSVDataActionProperty extends ImportDataActionProperty {
    private String separator;
    private boolean noHeader;
    private String charset;

    public ImportCSVDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties, 
                                       String separator, boolean noHeader, String charset) {
        super(valueClass, LM, ids, properties);
        this.separator = separator == null ? "|" : separator;
        this.noHeader = noHeader;
        this.charset = charset;
    }

    @Override
    public ImportIterator getIterator(byte[] file) throws IOException, ParseException, xBaseJException, JDOMException, ClassNotFoundException {
        return new ImportCSVIterator(getTable(file));
    }
    
    private List<List<String>> getTable(byte[] file) throws IOException, ParseException, xBaseJException {
        List<List<String>> result = new ArrayList<List<String>>();

        Scanner scanner = charset == null ? new Scanner(new ByteArrayInputStream(file)) : new Scanner(new ByteArrayInputStream(file), charset);
        if(!noHeader)
            scanner.nextLine();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] splittedLine = line.split(String.format("\\%s|;", separator));
            List<String> listRow = new ArrayList<String>();
            for (int i = 0; i < Math.min(splittedLine.length, properties.size()); i++) {
                listRow.add(splittedLine[i]);
            }
            result.add(listRow);
        }
        return result;
    }
}
