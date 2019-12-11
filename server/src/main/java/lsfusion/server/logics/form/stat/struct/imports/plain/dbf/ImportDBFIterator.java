package lsfusion.server.logics.form.stat.struct.imports.plain.dbf;

import com.google.common.base.Throwables;
import lsfusion.base.DateConverter;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.stat.struct.imports.plain.ImportPlainIterator;
import net.iryndin.jdbf.core.DbfField;
import net.iryndin.jdbf.core.DbfFieldTypeEnum;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class ImportDBFIterator extends ImportPlainIterator {
    private static final String EQ = "=";
    private static final String GE = ">=";
    private static final String GT = ">";
    private static final String LE = "<=";
    private static final String LT = "<";
    private static final String IN = " IN ";

    private CustomDbfReader reader;
    private String charset;
    private List<List<String>> wheresList;

    @Override
    protected boolean isFieldCI() {
        return true;
    }

    public ImportDBFIterator(ImOrderMap<String, Type> fieldTypes, RawFileData file, String charset, RawFileData memo, List<List<String>> wheresList) throws IOException {
        super(fieldTypes);

        this.reader = new CustomDbfReader(file, memo);
        this.charset = charset;
        this.wheresList = wheresList;
        
        finalizeInit();
    }

    @Override
    protected ImOrderSet<String> readFields() {
        Collection<DbfField> fields = reader.getMetadata().getFields();
        MOrderExclSet<String> mResult = SetFact.mOrderExclSet(fields.size());         
        for(DbfField field : fields)
            mResult.exclAdd(field.getName());
        return mResult.immutableOrder();
    }
        
    private CustomDbfRecord record;
    @Override
    protected boolean nextRow() throws IOException {
        do {
            record = reader.read();
            if (record == null) return false;
        } while (record.isDeleted() || ignoreRow(record, wheresList));
        return true;        
    }

    @Override
    protected Object getPropValue(String name, Type type) throws ParseException, java.text.ParseException, IOException {
        return type.parseDBF(record, name, charset);
    }

    private boolean ignoreRow(CustomDbfRecord record, List<List<String>> wheresList) {
        boolean ignoreRow = false;
        for (List<String> where : wheresList) {
            String condition = where.get(0);
            boolean and = condition != null && condition.equals("AND");
            boolean or = condition != null && condition.equals("OR");
            boolean not = where.get(1) != null;
            String field = where.get(2) != null ? where.get(2).toUpperCase() : null;
            String sign = where.get(3);
            String value = where.get(4);

            DbfField dbfField = record.getField(field);
            if (dbfField == null) {
                throw Throwables.propagate(new RuntimeException(String.format("Incorrect WHERE in IMPORT DBF: no such column '%s'", field)));
            }
            DbfFieldTypeEnum fieldType = dbfField.getType();
            if(fieldType == DbfFieldTypeEnum.Date || fieldType == DbfFieldTypeEnum.DateTime) {
                Date fieldValue = getDateFieldValue(record, field);
                boolean conditionResult = fieldValue == null || ignoreRowDateCondition(not, fieldValue, sign, DateConverter.smartParse(value));
                ignoreRow = and ? (ignoreRow | conditionResult) : or ? (ignoreRow & conditionResult) : conditionResult;
            } else {
                String fieldValue = record.getString(field, charset);
                boolean conditionResult = fieldValue == null || ignoreRowStringCondition(not, fieldValue, sign, value);
                ignoreRow = and ? (ignoreRow | conditionResult) : or ? (ignoreRow & conditionResult) : conditionResult;
            }
        }
        return ignoreRow;
    }

    private boolean ignoreRowStringCondition(boolean not, String fieldValue, String sign, String value) {
        boolean ignoreRow = false;
        if (sign.equals(IN)) {
            List<String> stringValues = splitIn(value);
            ignoreRow = !stringValues.contains(fieldValue);
        } else {
            String stringFieldValue = String.valueOf(fieldValue);
            switch (sign) {
                case EQ:
                    if (!stringFieldValue.equals(value))
                        ignoreRow = true;
                    break;
                case GE:
                    if (stringFieldValue.compareTo(value) < 0)
                        ignoreRow = true;
                    break;
                case GT:
                    if (stringFieldValue.compareTo(value) <= 0)
                        ignoreRow = true;
                    break;
                case LE:
                    if (stringFieldValue.compareTo(value) > 0)
                        ignoreRow = true;
                    break;
                case LT:
                    if (stringFieldValue.compareTo(value) >= 0)
                        ignoreRow = true;
                    break;
            }
        }
        return not != ignoreRow;
    }

    private Date getDateFieldValue(CustomDbfRecord record, String field) {
        try {
            return record.getDate(field);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    private boolean ignoreRowDateCondition(boolean not, Date fieldValue, String sign, Date value) {
        boolean ignoreRow = false;
        if (sign.equals(IN)) {
            throw new UnsupportedOperationException("IMPORT DBF WHERE IN is not supported for date fields");
        } else {
            switch (sign) {
                case EQ:
                    ignoreRow = fieldValue.compareTo(value) != 0;
                    break;
                case GE:
                    ignoreRow = fieldValue.compareTo(value) < 0;
                    break;
                case GT:
                    ignoreRow = fieldValue.compareTo(value) <= 0;;
                    break;
                case LE:
                    ignoreRow = fieldValue.compareTo(value) >= 0;;
                    break;
                case LT:
                    ignoreRow = fieldValue.compareTo(value) > 0;
                    break;
            }
        }
        return not != ignoreRow;
    }

    private List<String> splitIn(String value) {
        List<String> values = null;
        if (value.matches("\\(.*\\)")) {
            try {
                values = Arrays.asList(value.substring(1, value.length() - 1).split(","));
            } catch (Exception ignored) {
            }
            if (values == null)
                throw Throwables.propagate(new RuntimeException("Incorrect WHERE in IMPORT. Invalid \"IN\" condition"));
        }
        return values;
    }

    @Override
    public void release() {
        try {
            if (reader != null)
                reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}