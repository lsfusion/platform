package lsfusion.interop.form.print;

import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRewindableDataSource;

import java.io.InputStream;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static lsfusion.base.DateConverter.*;
import static lsfusion.base.TimeConverter.localTimeToSqlTime;

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

    public Object getFieldValue(JRField jrField) {

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
        if(value != null) {
            if (Date.class.getName().equals(jrField.getValueClassName())) {
                if (value instanceof LocalDateTime)
                    value = stampToDate(localDateTimeToSqlTimestamp((LocalDateTime) value));
                else
                    value = localDateToSqlDate((LocalDate) value);
            } else if (Time.class.getName().equals(jrField.getValueClassName())) {
                value = localTimeToSqlTime((LocalTime) value);
            } else if (Timestamp.class.getName().equals(jrField.getValueClassName())) {
                if(value instanceof Instant) {
                   value = instantToSqlTimestamp((Instant) value);
                } else {
                    value = localDateTimeToSqlTimestamp((LocalDateTime) value);
                }
            } else if (InputStream.class.getName().equals(jrField.getValueClassName())) { //IMAGEFILE
                value = ((RawFileData) value).getInputStream();
            }
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

    public boolean next() {
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
