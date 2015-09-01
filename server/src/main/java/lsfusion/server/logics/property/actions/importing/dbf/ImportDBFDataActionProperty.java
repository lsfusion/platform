package lsfusion.server.logics.property.actions.importing.dbf;

import com.google.common.io.Files;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.jdom.JDOMException;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportDBFDataActionProperty extends ImportDataActionProperty {
    public ImportDBFDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(new ValueClass[] {valueClass}, LM, ids, properties);
    }

    @Override
    public ImportIterator getIterator(byte[] file) throws IOException, ParseException, xBaseJException, JDOMException, ClassNotFoundException {

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("importDBF", ".dbf");
            Files.write(file, tmpFile);
            DBF dbf = new DBF(tmpFile.getAbsolutePath());

            Map<String, Integer> fieldMapping = new HashMap<>();
            for (int i = 1; i <= dbf.getFieldCount(); i++) {
                fieldMapping.put(dbf.getField(i).getName().toLowerCase(), i);
            }
            List<Integer> sourceColumns = getSourceColumns(fieldMapping);

            return new ImportDBFIterator(dbf, sourceColumns, properties);

        } finally {
            if (tmpFile != null)
                tmpFile.delete();
        }
    }

    @Override
    protected int columnsNumberBase() {
        return 1;
    }
}
