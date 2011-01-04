package platform.server.form.reportstmp;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.util.ArrayList;
import java.util.List;

/**
 * User: DAle
 * Date: 03.01.11
 * Time: 16:52
 */

public class ReportDependentDataSource_tmp implements JRDataSource {
    private List<Object> values;
    private final List<ReportDependentDataSource_tmp> childSources;
    private final ClientReportData_tmp data;

    public ReportDependentDataSource_tmp(ClientReportData_tmp data, List<ReportDependentDataSource_tmp> childSources) {
        this.data = data;
        this.childSources = childSources;
    }

    public Object getFieldValue(JRField jrField) throws JRException {
        return data.getFieldValue(jrField);
    }

    public boolean next() throws JRException {
        boolean hasNext = data.next();
        if (hasNext) {
            if (values != null) {
                for (int i = 0; i < values.size(); i++) {
                    if (!data.getKeyValueByIndex(i).equals(values.get(i))) {
                        hasNext = false;
                        data.revert();
                        break;
                    }
                }
            }

            if (hasNext && childSources != null) {
                List<Object> keyValues = new ArrayList<Object>();
                for (int i = 0; i < data.objectNames.size(); i++) {
                    keyValues.add(data.getKeyValueByIndex(i));
                }

                for (ReportDependentDataSource_tmp childSource : childSources) {
                    childSource.setValues(keyValues);
                }
            }
        }
        return hasNext;
    }

    private void setValues(List<Object> values) { this.values = values; }
}
