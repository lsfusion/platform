package lsfusion.server.logics.form.stat.struct.imports.plain;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.data.time.DateClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;
import lsfusion.server.logics.classes.data.time.TimeClass;
import lsfusion.server.logics.form.stat.struct.imports.ImportIterator;

import java.io.IOException;
import java.text.ParseException;

public abstract class ImportPlainIterator extends ImportIterator {

    protected ImOrderMap<String, Type> fieldTypes; // required, order is needed only in finalizeInit to build mapping
    protected ImMap<String, String> mapping; // required - actual

    public ImportPlainIterator(ImOrderMap<String, Type> fieldTypes, String wheres) {
        super(wheres);
        this.fieldTypes = fieldTypes;
    }
    
    private static Pair<String, Integer> findActualField(String field, ImOrderSet<String> fileFields, boolean isCI) {
        String actualField = null;
        Integer actualIndex = null;
        if(isCI) {
            String lowerField = field.toLowerCase();
            for(int i=0,size=fileFields.size();i<size;i++) {
                String fieldName = fileFields.get(i);
                if(lowerField.equals(fieldName.toLowerCase())) {
                    actualField = fieldName;
                    actualIndex = i;
                    break;
                }                    
            }
        } else {  // optimization
            if (fileFields.contains(field)) {
                actualField = field;
                actualIndex = fileFields.indexOf(field);
            }
        }
        if(actualField != null)
            return new Pair<>(actualField, actualIndex);
        return null;
    }
    
    protected boolean isFieldCI() {
        return false;
    }
    
    protected void finalizeInit() throws IOException {
        ImOrderSet<String> fileFields = readFields();
        boolean fieldCI = isFieldCI();

        mapping = getRequiredActualMap(fileFields, fieldTypes, fieldCI);
    }

    public static ImMap<String, String> getRequiredActualMap(ImOrderSet<String> actualFields, ImOrderMap<String, Type> requiredFieldTypes, boolean fieldCI) {
        int f = 0;
        ImOrderSet<String> keys = requiredFieldTypes.keyOrderSet();
        ImOrderValueMap<String, String> mMapping = keys.mapItOrderValues();
        for(int i = 0, size = keys.size(); i<size; i++) {
            String field = keys.get(i);
            String actualField;
            if(actualFields == null) {
                actualField = field;
            } else {
                Pair<String, Integer> actual = findActualField(field, actualFields, fieldCI);
                if (actual != null) {
                    actualField = actual.first;
                    f = actual.second;
                } else if (f < actualFields.size()) {
                    actualField = actualFields.get(f);
                } else {
                    throw new RuntimeException("IMPORT error: field " + field + " doesn't exist in file");
                }
                f++;
            }
            mMapping.mapValue(i, actualField);
        }
        return mMapping.immutableValueOrder().getMap();
    }

    protected abstract ImOrderSet<String> readFields() throws IOException;

    protected abstract boolean nextRow(boolean checkWhere) throws IOException;
    
    protected abstract Object getPropValue(String name, Type type) throws lsfusion.server.logics.classes.data.ParseException, ParseException, IOException;

    protected abstract Integer getRowIndex();
    
    public ImMap<String, Object> next() {
        try {
            if(!nextRow(true))
                return null;
            return mapping.mapValues((key, value) -> {
                try {
                    return getPropValue(value, fieldTypes.get(key));
                } catch (lsfusion.server.logics.classes.data.ParseException | ParseException | IOException e) {
                    throw Throwables.propagate(e);
                }
            });
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    } 
    public abstract void release() throws IOException;

    protected boolean ignoreRow() {
        boolean ignoreRow = false;
        try {
            for (Where where : wheresList) {

                boolean conditionResult;
                if(where.isRow()) {
                    conditionResult = ignoreRowIndexCondition(where.not, getRowIndex(), where.sign, where.value);
                } else {
                    Type fieldType = fieldTypes.get(where.field);
                    if (fieldType == null || !mapping.containsKey(where.field)) {
                        throw Throwables.propagate(new RuntimeException(String.format("Incorrect WHERE in IMPORT: no such column '%s'", where.field)));
                    }

                    Object fieldValue = getPropValue(mapping.get(where.field), fieldType);
                    if(fieldValue != null) {
                        if (fieldType == DateClass.instance || fieldType == TimeClass.instance || fieldType == DateTimeClass.instance || fieldType instanceof NumericClass) {
                            conditionResult = ignoreRowCondition(where.not, fieldValue, where.sign, fieldType.parseString(where.value));
                        } else {
                            conditionResult = ignoreRowCondition(where.not, String.valueOf(fieldValue), where.sign, where.value);
                        }
                    } else {
                        conditionResult = true;
                    }
                }

                ignoreRow = where.isAnd() ? (ignoreRow | conditionResult) : where.isOr() ? (ignoreRow & conditionResult) : conditionResult;
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return ignoreRow;
    }
}