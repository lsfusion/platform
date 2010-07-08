package platform.server.view.form.client;

import platform.interop.ComponentDesign;
import platform.interop.form.screen.ExternalScreen;
import platform.server.data.type.Type;
import platform.server.data.type.TypeSerializer;
import platform.server.view.form.client.report.ReportDrawField;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;

abstract public class CellView extends ComponentView {

    public Dimension minimumSize;
    public Dimension maximumSize;
    public Dimension preferredSize;

    public KeyStroke editKey;
    public boolean showEditKey = true;

    public Format format;

    public Boolean focusable;

    public boolean panelLabelAbove = false;

    public CellView(int ID) {
        super(ID);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeUTF(getCaption());

        TypeSerializer.serialize(outStream,getType());

        new ObjectOutputStream(outStream).writeObject(minimumSize);
        new ObjectOutputStream(outStream).writeObject(maximumSize);
        new ObjectOutputStream(outStream).writeObject(preferredSize);

        new ObjectOutputStream(outStream).writeObject(editKey);
        outStream.writeBoolean(showEditKey);

        new ObjectOutputStream(outStream).writeObject(format);

        new ObjectOutputStream(outStream).writeObject(focusable);

        outStream.writeBoolean(panelLabelAbove);
    }

    abstract Type getType();

    abstract int getID();
    abstract String getSID();
    abstract String getDefaultCaption();

    public String caption;
    String getCaption() {
        if (caption != null)
            return caption;
        else
            return getDefaultCaption();
    };

    public void fillReportDrawField(ReportDrawField reportField) {

        reportField.sID = getSID();
        reportField.caption = getCaption();

        Type type = getType();

        reportField.minimumWidth = type.getMinimumWidth();
        reportField.preferredWidth = type.getPreferredWidth();

        Format format = type.getDefaultFormat();
        if (format instanceof DecimalFormat) {
            reportField.pattern = ((DecimalFormat)format).toPattern();
        }
        if (format instanceof SimpleDateFormat) {
            reportField.pattern = ((SimpleDateFormat)format).toPattern();
        }

        getType().fillReportDrawField(reportField);
    }
}
