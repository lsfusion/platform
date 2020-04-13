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

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ImportPlainIterator {
    
    protected ImOrderMap<String, Type> fieldTypes; // required, order is needed only in finalizeInit to build mapping    
    protected ImMap<String, String> mapping; // required - actual
    protected static final String EQ = "=";
    protected static final String GE = ">=";
    protected static final String GT = ">";
    protected static final String LE = "<=";
    protected static final String LT = "<";
    protected static final String IN = " IN ";
    protected List<Where> wheresList;

    public ImportPlainIterator(ImOrderMap<String, Type> fieldTypes, String wheres) {
        this.fieldTypes = fieldTypes;
        this.wheresList = getWheresList(wheres);
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
                } else
                    actualField = actualFields.get(f);
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

    private List<Where> getWheresList(String wheres) {
        List<Where> wheresList = new ArrayList<>();
        if (wheres != null) { //spaces in value are not permitted
            Pattern wherePattern = Pattern.compile("(?:\\s(AND|OR)\\s)?(?:(NOT)\\s)?([^=<>\\s]+)(\\sIN\\s|=|<|>|<=|>=)([^=<>\\s]+)");
            Matcher whereMatcher = wherePattern.matcher(wheres);
            while (whereMatcher.find()) {
                String condition = whereMatcher.group(1);
                String not = whereMatcher.group(2);
                String field = whereMatcher.group(3);
                String sign = whereMatcher.group(4);
                String value = whereMatcher.group(5);
                wheresList.add(new Where(condition, not != null, field, sign, value));
            }
        }
        return wheresList;
    }

    private static int compare(Object field1, Object field2) {
        if (field1 instanceof LocalDate) {
            return ((LocalDate) field1).compareTo((LocalDate) field2);
        } else if (field1 instanceof LocalTime) {
            return ((LocalTime) field1).compareTo((LocalTime) field2);
        } else if (field1 instanceof LocalDateTime) {
            return ((LocalDateTime) field1).compareTo((LocalDateTime) field2);
        } else if (field1 instanceof BigDecimal) {
            return ((BigDecimal) field1).compareTo((BigDecimal) field2);
        } else if (field1 instanceof String) {
            return ((String) field1).compareTo((String) field2);
        } else return 0;
    }

    private boolean ignoreRowIndexCondition(boolean not, Integer fieldValue, String sign, String value) {
        boolean ignoreRow = false;
        if (sign.equals(IN)) {
            List<String> stringValues = splitIn(value);
            ignoreRow = !stringValues.contains(String.valueOf(fieldValue));
        } else {
            Integer intValue = Integer.parseInt(value);
            switch (sign) {
                case EQ:
                    if (!fieldValue.equals(intValue))
                        ignoreRow = true;
                    break;
                case GE:
                    if (fieldValue.compareTo(intValue) < 0)
                        ignoreRow = true;
                    break;
                case GT:
                    if (fieldValue.compareTo(intValue) <= 0)
                        ignoreRow = true;
                    break;
                case LE:
                    if (fieldValue.compareTo(intValue) > 0)
                        ignoreRow = true;
                    break;
                case LT:
                    if (fieldValue.compareTo(intValue) >= 0)
                        ignoreRow = true;
                    break;
            }
        }
        return not != ignoreRow;
    }

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

    private boolean ignoreRowCondition(boolean not, Object fieldValue, String sign, Object value) {
        boolean ignoreRow = false;
        if (sign.equals(IN)) {
            if(fieldValue instanceof String && value instanceof String) {
                List<String> stringValues = splitIn((String) value);
                ignoreRow = !stringValues.contains(fieldValue);
            } else {
                throw new UnsupportedOperationException("IMPORT WHERE IN is not supported for numeric / date fields");
            }
        } else {
            int compareResult = compare(fieldValue, value);
            switch (sign) {
                case EQ:
                    ignoreRow = compareResult != 0;
                    break;
                case GE:
                    ignoreRow = compareResult < 0;
                    break;
                case GT:
                    ignoreRow = compareResult <= 0;
                    break;
                case LE:
                    ignoreRow = compareResult > 0;
                    break;
                case LT:
                    ignoreRow = compareResult >= 0;
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

    private static class Where {
        String condition;
        boolean not;
        String field;
        String sign;
        String value;

        public Where(String condition, boolean not, String field, String sign, String value) {
            this.condition = condition;
            this.not = not;
            this.field = field;
            this.sign = sign;
            this.value = value;
        }

        public boolean isAnd() {
            return condition != null && condition.equals("AND");
        }

        public boolean isOr() {
            return condition != null && condition.equals("OR");
        }

        public boolean isRow() {
            return field != null && field.toUpperCase().equals("@ROW");
        }
    }

}