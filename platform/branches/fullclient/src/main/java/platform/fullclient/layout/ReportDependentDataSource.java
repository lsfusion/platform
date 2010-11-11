package platform.fullclient.layout;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

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

    public ReportDependentDataSource(ClientReportData data, List<ReportDependentDataSource> childSources) {
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

                for (ReportDependentDataSource childSource : childSources) {
                    childSource.setValues(keyValues);
                }
            }
        }
        return hasNext;
    }

    private void setValues(List<Object> values) { this.values = values; }
}
