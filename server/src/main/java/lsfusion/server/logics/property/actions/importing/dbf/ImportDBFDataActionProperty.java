package lsfusion.server.logics.property.actions.importing.dbf;

import lsfusion.base.BaseUtils;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import net.iryndin.jdbf.core.DbfField;
import org.apache.commons.io.FileUtils;
import org.jdom.JDOMException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportDBFDataActionProperty extends ImportDataActionProperty {
    private boolean hasWheres;
    private String wheres;
    private boolean hasMemo;
    private byte[] memo;
    private File tempMemoFile;
    private String charset;
    public ImportDBFDataActionProperty(int paramsCount, boolean hasWheres, boolean hasMemo, List<String> ids, List<LCP> properties, String charset, BaseLogicsModule baseLM) {
        super(paramsCount, ids, properties, baseLM);
        this.hasWheres = hasWheres;
        this.hasMemo = hasMemo;
        this.charset = charset == null ? "cp1251" : charset;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject wheresObject = null;
        if (hasWheres) {
            wheresObject = context.getDataKeys().getValue(1);
            assert wheresObject.getType() instanceof StringClass;
        }
        wheres = wheresObject != null ? (String) wheresObject.object : null;
        DataObject memoObject = null;
        if (hasMemo) {
            memoObject = context.getDataKeys().getValue(hasWheres ? 2 : 1);
        }
        memo = memoObject != null ? BaseUtils.getFile((byte[]) memoObject.object) : null;
        return super.aspectExecute(context);
    }

    @Override
    public ImportIterator getIterator(byte[] file, String extension) throws IOException {
        if(memo != null) {
            tempMemoFile = File.createTempFile("tempMemoFile", ".FPT");
            FileUtils.writeByteArrayToFile(tempMemoFile, memo);
        }
        CustomDbfReader reader = new CustomDbfReader(new ByteArrayInputStream(file), tempMemoFile);
        return new ImportDBFIterator(reader, getSourceColumns(getFieldMapping(reader)), getWheresList(), properties, tempMemoFile, charset);
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

    public static Map<String, Integer> getFieldMapping(CustomDbfReader reader) {
        Map<String, Integer> fieldMapping = new HashMap<>();
        int i = 1;
        for (DbfField field : reader.getMetadata().getFields()) {
            fieldMapping.put(field.getName().toLowerCase(), i);
            i++;
        }
        return fieldMapping;
    }
}
