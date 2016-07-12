package jasperapi;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.design.JRDesignField;

import java.util.ArrayList;
import java.util.List;

/**
 * User: DAle
 * Date: 16.09.2010
 * Time: 15:12:53
 */

public class ReportDependentDataSource implements JRDataSource {
    private List<Object> values;
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

    public Object getFieldValue(String fieldName) throws JRException {
        JRDesignField field = new JRDesignField();
        field.setName(fieldName);
        return getFieldValue(field);
    }

    public Object getFieldValue(JRField jrField) throws JRException {
        return data.getFieldValue(jrField);
    }

    public boolean next() throws JRException {
        if (repeatCount == 0) {
            boolean hasNext = data.next();
            if (hasNext && values != null) {
                for (int i = 0; i < values.size(); i++) {
                    if (data.getKeyValueByIndex(i) == null && values.get(i) != null || !data.getKeyValueByIndex(i).equals(values.get(i))) {
                        hasNext = false;
                        data.revert();
                        break;
                    }
                }
            }

            if (hasNext && childSources != null) {
                List<Object> keyValues = new ArrayList<Object>();
                for (int i = 0; i < data.getObjectsCount(); i++) {
                    keyValues.add(data.getKeyValueByIndex(i));
                }

                for (ReportDependentDataSource childSource : childSources) {
                    childSource.setValues(keyValues);
                }
            }

            if (hasNext && repeatCountFieldName != null) {
                Object obj = getFieldValue(repeatCountFieldName);
                if (obj instanceof Integer && (Integer)obj > 0) {
                    repeatCount = (Integer)obj - 1;
                }
            }
            return hasNext;
        } else {
            --repeatCount;
            return true;
        }
    }

    private void setValues(List<Object> values) { this.values = values; }
}
