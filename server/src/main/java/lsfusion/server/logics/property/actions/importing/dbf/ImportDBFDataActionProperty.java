package lsfusion.server.logics.property.actions.importing.dbf;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.reader.DbfReader;
import org.jdom.JDOMException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportDBFDataActionProperty extends ImportDataActionProperty {
    public ImportDBFDataActionProperty(ValueClass valueClass, ValueClass wheresClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(wheresClass == null ? new ValueClass[] {valueClass} : new ValueClass[] {valueClass, wheresClass}, LM, ids, properties);
    }

    @Override
    public ImportIterator getIterator(byte[] file, String wheres) throws IOException, ParseException, JDOMException, ClassNotFoundException {
        DbfReader reader = new DbfReader(new ByteArrayInputStream(file));
        Map<String, Integer> fieldMapping = new HashMap<>();
        int i = 1;
        for (DbfField field : reader.getMetadata().getFields()) {
            fieldMapping.put(field.getName().toLowerCase(), i);
            i++;
        }
        List<Integer> sourceColumns = getSourceColumns(fieldMapping);
        return new ImportDBFIterator(reader, sourceColumns, wheres);
    }

    @Override
    protected int columnsNumberBase() {
        return 1;
    }

    @Override
    protected boolean ignoreIncorrectColumns() {
        return false;
    }
}
