package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.util.List;

public class ImportCSVDataActionProperty extends ImportDataActionProperty {
    private String separator;
    private boolean noHeader;
    private String charset;

    public ImportCSVDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties, 
                                       String separator, boolean noHeader, String charset) {
        super(new ValueClass[] {valueClass}, LM, ids, properties);
        this.separator = separator == null ? "|" : separator;
        this.noHeader = noHeader;
        this.charset = charset;
    }

    @Override
    public ImportIterator getIterator(byte[] file, Integer sheetIndex) {
        return new ImportCSVIterator(file, charset, separator, noHeader, properties.size());
    }
}
