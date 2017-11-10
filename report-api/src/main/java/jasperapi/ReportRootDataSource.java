package jasperapi;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 * User: DAle
 * Date: 16.09.2010
 * Time: 15:15:04
 */

public class ReportRootDataSource implements JRDataSource {
    private int index = 0;
    public boolean next() throws JRException {
        ++index;
        return index == 1;
    }

    public Object getFieldValue(JRField jrField) throws JRException { return ""; }
}

