package platform.interop;

import java.text.Format;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import platform.server.view.form.report.ReportDrawField;
import platform.client.form.*;
import net.sf.jasperreports.engine.JRAlignment;

public class ClientDateClass extends ClientClass {

    public int getPreferredWidth() { return 70; }

    Format getDefaultFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new DatePropertyRenderer(format); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new DatePropertyEditor(value, (SimpleDateFormat) format); }

    public Class getJavaClass() {
        return java.util.Date.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }
}
