package jasperapi;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.design.JRDesignField;

import java.util.ArrayList;
import java.util.List;

public class ReportDependentDataSource implements JRDataSource {
    private List<Object> parentKeyValues;
    private final List<ReportDependentDataSource> childSources;
    private final ClientReportData data;
    private String repeatCountFieldName = null;
    private int repeatCount = 0;

    public ReportDependentDataSource(ClientReportData data, List<ReportDependentDataSource> childSources, String repeatCountFieldName) {
        this(data, childSources);
        this.repeatCountFieldName = repeatCountFieldName;
    }

    public ReportDependentDataSource(ClientReportData data, List<ReportDependentDataSource> childSources) {
        this.data = data;
        this.childSources = childSources;
    }

    public Object getFieldValue(String fieldName) {
        JRDesignField field = new JRDesignField();
        field.setName(fieldName);
        return getFieldValue(field);
    }

    public Object getFieldValue(JRField jrField) {
        return data.getFieldValue(jrField);
    }

    public boolean next() {
        if (repeatCount > 0) {
            --repeatCount;
        } else {
            boolean hasNext = data.next();
            if (!hasNext) return false;
            if (!sameParentKeyValues()) {
                data.revert();
                return false;
            }

            setKeyValuesToChildren();
            setRepeatCountIfExists();
        } 
        return true;
    }

    private boolean sameParentKeyValues() {
        if (parentKeyValues != null) {
            for (int i = 0; i < parentKeyValues.size(); i++) {
                if (data.getKeyValueByIndex(i) == null && parentKeyValues.get(i) != null || !data.getKeyValueByIndex(i).equals(parentKeyValues.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private void setKeyValuesToChildren() {
        if (childSources != null) {
            List<Object> keyValues = new ArrayList<>();
            for (int i = 0; i < data.getObjectsCount(); i++) {
                keyValues.add(data.getKeyValueByIndex(i));
            }

            for (ReportDependentDataSource childSource : childSources) {
                childSource.setParentKeyValues(keyValues);
            }
        }
    }
    
    private void setParentKeyValues(List<Object> parentKeyValues) { 
        this.parentKeyValues = parentKeyValues; 
    }

    private void setRepeatCountIfExists() {
        if (repeatCountFieldName != null) {
            Object obj = getFieldValue(repeatCountFieldName);
            if (obj instanceof Integer && (Integer) obj > 0) {
                repeatCount = (Integer) obj - 1;
            }
        }
    }
}
