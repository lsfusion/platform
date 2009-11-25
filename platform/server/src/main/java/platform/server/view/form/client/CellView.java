package platform.server.view.form.client;

import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.view.form.client.report.ReportDrawField;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;

abstract public class CellView extends ComponentView {

    Dimension minimumSize;
    Dimension maximumSize;
    Dimension preferredSize;

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeUTF(getCaption());

        TypeSerializer.serialize(outStream,getType());

        new ObjectOutputStream(outStream).writeObject(minimumSize);
        new ObjectOutputStream(outStream).writeObject(maximumSize);
        new ObjectOutputStream(outStream).writeObject(preferredSize);
    }

    abstract Type getType();

    abstract int getID();
    abstract String getSID();
    abstract String getCaption();

    public Format getFormat() {
        return getType().getDefaultFormat();
    }

    public int getMinimumWidth() {
        return getType().getMinimumWidth();
    }

    public int getMinimumHeight() {
        return getPreferredHeight();
    }

    public Dimension getMinimumSize() {

        if (minimumSize != null) return minimumSize;
        return new Dimension(getMinimumWidth(), getMinimumHeight());
    }

    public int getPreferredWidth() {
        return getType().getPreferredWidth();
    }

    public int getPreferredHeight() {
        return 15;
    }

    public Dimension getPreferredSize() {

        if (preferredSize != null) return preferredSize;
        return new Dimension(getPreferredWidth(), getPreferredHeight());
    }

    public int getMaximumWidth() {
        return getType().getMaximumWidth();
    }

    public int getMaximumHeight() {
        return getPreferredHeight();
    }

    public Dimension getMaximumSize() {

        if (maximumSize != null) return maximumSize;
        return new Dimension(getMaximumWidth(), getMaximumHeight());
    }

    
    public void fillReportDrawField(ReportDrawField reportField) {

        reportField.sID = getSID();
        reportField.caption = getCaption();

        reportField.minimumWidth = getMinimumWidth();
        reportField.preferredWidth = getPreferredWidth();

        Format format = getFormat();
        if (format instanceof DecimalFormat) {
            reportField.pattern = ((DecimalFormat)format).toPattern();
        }
        if (format instanceof SimpleDateFormat) {
            reportField.pattern = ((SimpleDateFormat)format).toPattern();
        }

        getType().fillReportDrawField(reportField);
    }
}
