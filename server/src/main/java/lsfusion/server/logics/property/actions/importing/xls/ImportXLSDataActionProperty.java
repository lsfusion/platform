package lsfusion.server.logics.property.actions.importing.xls;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportXLSDataActionProperty extends ImportDataActionProperty {
    public final static Map<String, Integer> XLSColumnsMapping = new HashMap<String, Integer>() {{
        put("A", 0); put("B", 1); put("C", 2); put("D", 3); put("E", 4); put("F", 5); put("G", 6); put("H", 7); put("I", 8);
        put("J", 9); put("K", 10); put("L", 11); put("M", 12); put("N", 13); put("O", 14); put("P", 15); put("Q", 16);
        put("R", 17); put("S", 18); put("T", 19); put("U", 20); put("V", 21); put("W", 22); put("X", 23); put("Y", 24);
        put("Z", 25); put("AA", 26); put("AB", 27); put("AC", 28); put("AD", 29); put("AE", 30); put("AF", 31); put("AG", 32);
        put("AH", 33); put("AI", 34); put("AJ", 35); put("AK", 36); put("AL", 37); put("AM", 38); put("AN", 39); put("AO", 40);
        put("AP", 41); put("AQ", 42); put("AR", 43); put("AS", 44); put("AT", 45); put("BA", 52); put("BB", 53); put("BC", 54);
    }};
    
    public ImportXLSDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(valueClass, LM, ids, properties);
    }

    @Override
    public ImportIterator getIterator(byte[] file) throws IOException {
        return new ImportXLSIterator(file, getSourceColumns(XLSColumnsMapping), properties);
    }
}
