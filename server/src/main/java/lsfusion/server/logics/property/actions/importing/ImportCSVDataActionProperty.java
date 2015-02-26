package lsfusion.server.logics.property.actions.importing;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.xBaseJ.xBaseJException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ImportCSVDataActionProperty extends ImportDataActionProperty {
    public ImportCSVDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(valueClass, LM, ids, properties);
    }

    @Override
    public List<List<String>> getTable(byte[] file) throws IOException, ParseException, xBaseJException {
        List<List<String>> result = new ArrayList<List<String>>();

        Scanner scanner = new Scanner(new ByteArrayInputStream(file));
        scanner.nextLine();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] splittedLine = line.split("\\||;");
            List<String> listRow = new ArrayList<String>();
            for (int i = 0; i < Math.min(splittedLine.length, properties.size()); i++) {
                listRow.add(splittedLine[i]);
            }
            result.add(listRow);
        }
        return result;
    }
}
