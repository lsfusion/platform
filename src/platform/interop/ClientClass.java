package platform.interop;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;

import java.io.Serializable;
import java.text.Format;

import platform.server.view.form.report.ReportDrawField;
import net.sf.jasperreports.engine.JRAlignment;

abstract public class ClientClass implements Serializable {

    public int ID;
    public String caption;

    public boolean hasChilds;

    public String toString() { return caption; }

    public int getMinimumWidth() {
        return getPreferredWidth();
    }
    public int getPreferredWidth() {
        return 50;
    }
    public int getMaximumWidth() {
        return Integer.MAX_VALUE;
    }

    abstract Format getDefaultFormat();

    abstract public PropertyRendererComponent getRendererComponent(Format format);
    abstract public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format);

    abstract public Class getJavaClass() ;

    public void fillReportDrawField(ReportDrawField reportField) {
        reportField.valueClass = getJavaClass();
        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_LEFT;
    };

}
