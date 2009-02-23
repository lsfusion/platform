package platform.interop;

import java.text.Format;
import java.text.NumberFormat;

import platform.server.view.form.report.ReportDrawField;
import platform.client.form.*;
import net.sf.jasperreports.engine.JRAlignment;

abstract public class ClientIntegralClass extends ClientClass {

    public int getMinimumWidth() { return 45; }
    public int getPreferredWidth() { return 80; }

    Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new IntegerPropertyRenderer(format); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new IntegerPropertyEditor(value, (NumberFormat)format, getJavaClass()); }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }

}
