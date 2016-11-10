package lsfusion.server.logics.property.actions.importing.dbf;

import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.reader.DbfReader;
import org.jdom.JDOMException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportDBFDataActionProperty extends ImportDataActionProperty {
    private String wheres;
    public ImportDBFDataActionProperty(ValueClass valueClass, ValueClass wheresClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(wheresClass == null ? new ValueClass[] {valueClass} : new ValueClass[] {valueClass, wheresClass}, LM, ids, properties);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject wheresObject = null;
        if (context.getDataKeys().size() == 2) {
            wheresObject = context.getDataKeys().getValue(1);
            assert wheresObject.getType() instanceof StringClass;
        }
        wheres = wheresObject != null ? (String) wheresObject.object : null;
        super.executeCustom(context);
    }

    @Override
    public ImportIterator getIterator(byte[] file) throws IOException, ParseException, JDOMException, ClassNotFoundException {
        DbfReader reader = new DbfReader(new ByteArrayInputStream(file));
        Map<String, Integer> fieldMapping = new HashMap<>();
        int i = 1;
        for (DbfField field : reader.getMetadata().getFields()) {
            fieldMapping.put(field.getName().toLowerCase(), i);
            i++;
        }
        List<Integer> sourceColumns = getSourceColumns(fieldMapping);
        return new ImportDBFIterator(reader, sourceColumns, getWheresList());
    }

    private List<List<String>> getWheresList() {
        List<List<String>> wheresList = new ArrayList<>();
        if (wheres != null) { //spaces in value are not permitted
            Pattern wherePattern = Pattern.compile("(?:\\s(AND|OR)\\s)?(?:(NOT)\\s)?([^=<>\\s]+)(\\sIN\\s|=|<|>|<=|>=)([^=<>\\s]+)");
            Matcher whereMatcher = wherePattern.matcher(wheres);
            while (whereMatcher.find()) {
                String condition = whereMatcher.group(1);
                String not = whereMatcher.group(2);
                String field = whereMatcher.group(3);
                String sign = whereMatcher.group(4);
                String value = whereMatcher.group(5);
                wheresList.add(Arrays.asList(condition, not, field, sign, value));
            }
        }
        return wheresList;
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
