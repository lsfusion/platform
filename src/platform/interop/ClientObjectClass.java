package platform.interop;

import java.text.Format;
import java.text.NumberFormat;

import platform.server.view.form.report.ReportDrawField;
import platform.client.form.*;
import net.sf.jasperreports.engine.JRAlignment;

public class ClientObjectClass extends ClientClass {

    public int getPreferredWidth() { return 45; }
    public int getMaximumWidth() { return getPreferredWidth(); }

    Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new IntegerPropertyRenderer(format); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new ObjectPropertyEditor(form, property, this, value); }

    public Class getJavaClass() {
        return Integer.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }
}
