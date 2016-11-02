package lsfusion.server.logics.property.actions.importing.dbf;

import com.google.common.io.Files;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.reader.DbfReader;
import org.jdom.JDOMException;

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
    public ImportIterator getIterator(byte[] file) throws IOException, ParseException, JDOMException, ClassNotFoundException {

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("importDBF", ".dbf");
            Files.write(file, tmpFile);
            DbfReader reader = new DbfReader(tmpFile);

            Map<String, Integer> fieldMapping = new HashMap<>();
            int i = 1;
            for(DbfField field : reader.getMetadata().getFields()) {
                fieldMapping.put(field.getName().toLowerCase(), i);
                i++;
            }
            List<Integer> sourceColumns = getSourceColumns(fieldMapping);

            return new ImportDBFIterator(reader, sourceColumns);

        } finally {
            if (tmpFile != null && !tmpFile.delete())
                tmpFile.deleteOnExit();
        }
    }

    @Override
    protected int columnsNumberBase() {
        return 1;
    }
}
