package platform.server.form.reportstmp;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 * User: DAle
 * Date: 03.01.11
 * Time: 16:52
 */

public class ReportRootDataSource_tmp implements JRDataSource {
    private int index = 0;
    public boolean next() throws JRException {
        ++index;
        return index == 1;
    }

    public Object getFieldValue(JRField jrField) throws JRException { return ""; }
}

