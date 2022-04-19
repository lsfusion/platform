package lsfusion.server.logics.form.stat.struct.imports;

import com.google.common.base.Throwables;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ImportIterator {

    private static final String EQ = "=";
    private static final String GE = ">=";
    private static final String GT = ">";
    private static final String LE = "<=";
    private static final String LT = "<";
    private static final String IN = " IN ";
    protected List<Where> wheresList;

    public ImportIterator(String wheres) {
        this.wheresList = getWheresList(wheres);
    }

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

    protected boolean ignoreRowIndexCondition(boolean not, Integer fieldValue, String sign, String value) {
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

    protected boolean ignoreRowCondition(boolean not, Object fieldValue, String sign, Object value) {
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

    protected static class Where {
        public String condition;
        public boolean not;
        public String field;
        public String sign;
        public String value;

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
            return field != null && field.equalsIgnoreCase("@ROW");
        }
    }
}
