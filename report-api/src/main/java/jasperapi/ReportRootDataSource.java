package jasperapi;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

public class ReportRootDataSource implements JRDataSource {
    private int index = 0;
    public boolean next() throws JRException {
        ++index;
        return index == 1;
    }

    public Object getFieldValue(JRField jrField) throws JRException { return ""; }
}

