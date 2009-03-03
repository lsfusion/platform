package platform.server.logics.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.server.view.form.client.report.ReportDrawField;

import java.text.DateFormat;
import java.text.Format;

public class DateClass extends IntegralClass {
    DateClass(Integer iID, String caption) {super(iID, caption);}

    public int getPreferredWidth() { return 70; }

    public Format getDefaultFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    public Class getJavaClass() {
        return java.util.Date.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }

    public byte getTypeID() {
        return 5;
    }
}
