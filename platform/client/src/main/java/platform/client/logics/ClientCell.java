package platform.client.logics;

import platform.client.SwingUtils;
import platform.client.form.ClientExternalScreen;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.CellView;
import platform.client.logics.classes.ClientType;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.screen.ExternalScreenConstraints;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.RemoteException;
import java.text.Format;
import java.text.ParseException;
import java.util.Collection;

abstract public class ClientCell extends ClientComponent {

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    ClientType baseType;

    public abstract int getID();

    public abstract int getShiftID(); // нужен для того, чтобы CellView мог генерировать уникальный ID

    public abstract ClientGroupObject getGroupObject();

    public String caption;

    public KeyStroke editKey;
    public boolean showEditKey;

    public Boolean focusable;
    public Boolean readOnly;

    public boolean panelLabelAbove;

    public ClientExternalScreen externalScreen;
    public ExternalScreenConstraints externalScreenConstraints;

    ClientCell(DataInputStream inStream, Collection<ClientContainer> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        caption = inStream.readUTF();

        baseType = ClientTypeSerializer.deserialize(inStream);

        minimumSize = (Dimension) new ObjectInputStream(inStream).readObject();
        maximumSize = (Dimension) new ObjectInputStream(inStream).readObject();
        preferredSize = (Dimension) new ObjectInputStream(inStream).readObject();

        editKey = (KeyStroke) new ObjectInputStream(inStream).readObject();
        showEditKey = inStream.readBoolean();

        format = (Format) new ObjectInputStream(inStream).readObject();

        focusable = (Boolean) new ObjectInputStream(inStream).readObject();
        readOnly = (Boolean) new ObjectInputStream(inStream).readObject();
        if (readOnly == null) {
            readOnly = true;
        }

        panelLabelAbove = inStream.readBoolean();

        if (inStream.readBoolean())
            externalScreen = ClientExternalScreen.getScreen(inStream.readInt());

        if (inStream.readBoolean())
            externalScreenConstraints = (ExternalScreenConstraints) new ObjectInputStream(inStream).readObject();
    }

    public int getMinimumWidth(JComponent comp) {
        if (minimumSize != null) {
            return minimumSize.width;
        }
        return baseType.getMinimumWidth(comp.getFontMetrics(design.getFont(comp)));
    }

    public int getMinimumHeight(JComponent comp) {
        if (minimumSize != null) {
            return minimumSize.height;
        }
        return getPreferredHeight(comp);
    }

    private Dimension minimumSize;

    public Dimension getMinimumSize(JComponent comp) {
        if (minimumSize != null)
            return minimumSize;
        return new Dimension(getMinimumWidth(comp), getMinimumHeight(comp));
    }

    public int getPreferredWidth(JComponent comp) {
        if (preferredSize != null) {
            return preferredSize.width;
        }
        return baseType.getPreferredWidth(comp.getFontMetrics(design.getFont(comp)));
    }

    public int getPreferredHeight(JComponent comp) {
        if (preferredSize != null) {
            return preferredSize.height;
        }
        return baseType.getPreferredHeight(comp.getFontMetrics(design.getFont(comp)));
    }

    private Dimension preferredSize;

    public Dimension getPreferredSize(JComponent comp) {
        if (preferredSize != null)
            return preferredSize;
        return new Dimension(getPreferredWidth(comp), getPreferredHeight(comp));
    }

    public int getMaximumWidth(JComponent comp) {
        if (maximumSize != null) {
            return maximumSize.width;
        }
        return baseType.getMaximumWidth(comp.getFontMetrics(design.getFont(comp)));
    }

    public int getMaximumHeight(JComponent comp) {
        if (maximumSize != null) {
            return maximumSize.height;
        }
        return getPreferredHeight(comp);
    }

    private Dimension maximumSize;

    public Dimension getMaximumSize(JComponent comp) {
        if (maximumSize != null)
            return maximumSize;
        return new Dimension(getMaximumWidth(comp), getMaximumHeight(comp));
    }

    private transient PropertyRendererComponent renderer;

    public PropertyRendererComponent getRendererComponent() {
        if (renderer == null) renderer = baseType.getRendererComponent(getFormat(), caption, design);
        return renderer;
    }

    public CellView getPanelComponent(ClientFormController form) {
        return baseType.getPanelComponent(this, form);
    }

    // диалог для получения возможных значений, используются только в нижних методах
    public abstract RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException;

    public abstract RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException;

    // на данный момент ClientFormController нужна для 2-х целей : как owner, создаваемых диалогов и как провайдер RemoteFormInterface, для получения того, что мы вообще редактируем
    public abstract PropertyEditorComponent getEditorComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException;

    public abstract PropertyEditorComponent getClassComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException;

    private Format format;

    Format getFormat() {
        if (format == null) return baseType.getDefaultFormat();
        return format;
    }

    public String toString() {
        return caption;
    }

    public String getFullCaption() {

        String fullCaption = caption;
        if (showEditKey && editKey != null)
            fullCaption += " (" + SwingUtils.getKeyStrokeCaption(editKey) + ")";
        return fullCaption;
    }

    public boolean checkEquals() {
        return true;
    }

    public Object parseString(ClientFormController form, String s) throws ParseException {
        return baseType.parseString(s);
    }
}
