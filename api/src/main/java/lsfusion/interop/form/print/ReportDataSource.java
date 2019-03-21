package lsfusion.interop.form.print;

import lsfusion.base.BaseUtils;
import lsfusion.base.DateConverter;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRewindableDataSource;

import java.util.*;

public class ReportDataSource implements JRRewindableDataSource {
    private final List<ReportDataSource> childSources = new ArrayList<>();
    
    private final ClientKeyData keyData;
    private final ClientPropertyData propData;
    
    private String repeatCountFieldName;
    private int repeatCount = 0;

    public ListIterator<Map<Integer, Object>> iterator;
    private Map<Integer, Object> upCurrentKeyRow;
    public Map<Integer, Object> currentKeyRow;

    public ReportDataSource(ClientKeyData keyData, ClientPropertyData propData, String repeatCountFieldName) {
        this.keyData = keyData;
        this.propData = propData;
        this.repeatCountFieldName = repeatCountFieldName;

        moveFirst();
    }

    @Override
    public void moveFirst() {
        upCurrentKeyRow = new HashMap<>();
        iterator = keyData.listIterator();

        // not sure if it is needed, but just in case
        currentKeyRow = null;
        for (ReportDataSource childSource : childSources)
            childSource.upCurrentKeyRow = null;
    }

    public Object getFieldValue(JRField jrField) throws JRException {

        String fieldName = jrField.getName();
        if(fieldName == null) // just in case
            return fieldName;

        Object value = getCurrentKeyFieldValue(fieldName);
        if(value == null)
            value = getCurrentPropFieldValue(fieldName);        

        return transformValue(jrField, value);
    }

    private Object getCurrentKeyFieldValue(String fieldName) {
        return keyData.getFieldValue(currentKeyRow, fieldName);
    }

    private Object getCurrentPropFieldValue(String fieldName) {
        return propData.getFieldValue(currentKeyRow, fieldName);
    }

    private Object transformValue(JRField jrField, Object value) {
        if (Date.class.getName().equals(jrField.getValueClassName()) && value != null) {
            if (value instanceof java.sql.Timestamp)
                value = DateConverter.stampToDate((java.sql.Timestamp) value);
            else
                value = DateConverter.sqlToDate((java.sql.Date) value);
        }

        if (value instanceof String) {
            value = BaseUtils.rtrim((String) value);
        }

        if(jrField.getDescription()!=null && Number.class.isAssignableFrom(jrField.getValueClass()) && jrField.getDescription().contains("@Z") && value == null) {
            value = jrField.getValueClass().cast(0);
        }

        return value;
    }

    private static boolean match(Map<Integer, Object> upKeyRow, Map<Integer, Object> keyRow) {
        for(Map.Entry<Integer, Object> entry : upKeyRow.entrySet())
            if(!entry.getValue().equals(keyRow.get(entry.getKey())))
                return false;
        return true;
    }

    public boolean next() throws JRException {
        if (repeatCount == 0) {
            if (!iterator.hasNext())
                return false;
            
            currentKeyRow = iterator.next();
            
            if (!match(upCurrentKeyRow, currentKeyRow)) {
                iterator.previous();
                return false;
            }

            for (ReportDataSource childSource : childSources)
                childSource.upCurrentKeyRow = currentKeyRow;

            if (repeatCountFieldName != null) {
                Object obj = getCurrentPropFieldValue(repeatCountFieldName);
                if (obj instanceof Integer && (Integer)obj > 0) {
                    repeatCount = (Integer)obj - 1;
                }
            }
            return true;
        } else {
            --repeatCount;
            return true;
        }
    }

    public void addSubReportSource(ReportDataSource childSource) {
        childSources.add(childSource);
    }
}
