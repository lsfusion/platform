package lsfusion.server.logics.property.actions.importing.xlsx;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.property.actions.importing.xls.ImportXLSDataActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jdom.JDOMException;
import org.xBaseJ.xBaseJException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class ImportXLSXDataActionProperty extends ImportDataActionProperty {
    public ImportXLSXDataActionProperty(ValueClass[] valueClasses, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(valueClasses, LM, ids, properties);
    }

    @Override
    public ImportIterator getIterator(byte[] file, Integer sheetIndex) throws IOException, ParseException, xBaseJException, JDOMException, ClassNotFoundException {
        return new ImportXLSXIterator(file, getSourceColumns(ImportXLSDataActionProperty.XLSColumnsMapping), properties, sheetIndex);
    }
}
