package platform.interop;

import java.text.Format;

import platform.server.view.form.report.ReportDrawField;
import platform.client.form.*;
import net.sf.jasperreports.engine.JRAlignment;

public class ClientBitClass extends ClientClass {

    public int getPreferredWidth() { return 35; }

    Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new BitPropertyRenderer(); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new BitPropertyEditor(value); }

    public Class getJavaClass() {
        return Boolean.class;
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_CENTER;
    }
}
