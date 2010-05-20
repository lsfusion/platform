package platform.client.logics;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.logics.classes.ClientType;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.SwingUtils;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.RemoteDialogInterface;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.Format;
import java.util.Collection;
import java.rmi.RemoteException;

abstract public class ClientCellView extends ClientComponentView {

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    ClientType baseType;

    public abstract int getID();
    public abstract int getShiftID(); // нужен для того, чтобы CellView мог генерировать уникальный ID
    public abstract ClientGroupObjectImplementView getGroupObject();

    public String caption;

    public KeyStroke editKey;
    public boolean showEditKey;

    public boolean enabled;

    ClientCellView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        caption = inStream.readUTF();

        baseType = ClientTypeSerializer.deserialize(inStream);

        minimumSize = (Dimension) new ObjectInputStream(inStream).readObject();
        maximumSize = (Dimension) new ObjectInputStream(inStream).readObject();
        preferredSize = (Dimension) new ObjectInputStream(inStream).readObject();

        editKey = (KeyStroke) new ObjectInputStream(inStream).readObject();
        showEditKey = inStream.readBoolean();
        font = (Font) new ObjectInputStream(inStream).readObject();

        enabled = inStream.readBoolean();
    }

    public int getMinimumWidth(JComponent comp) {
        return baseType.getMinimumWidth(comp.getFontMetrics(getFont(comp)));
    }

    int getMinimumHeight(JComponent comp) {
        return getPreferredHeight(comp);
    }

    private Dimension minimumSize;
    public Dimension getMinimumSize(JComponent comp) {
        if (minimumSize != null) return minimumSize;
        return new Dimension(getMinimumWidth(comp), getMinimumHeight(comp));
    }

    public int getPreferredWidth(JComponent comp) {
        return baseType.getPreferredWidth(comp.getFontMetrics(getFont(comp)));
    }

    int getPreferredHeight(JComponent comp) {
        return comp.getFontMetrics(getFont(comp)).getHeight() + 1;
    }

    private Dimension preferredSize;
    public Dimension getPreferredSize(JComponent comp) {
        if (preferredSize != null) return preferredSize;
        return new Dimension(getPreferredWidth(comp), getPreferredHeight(comp));
    }

    public int getMaximumWidth(JComponent comp) {
        return baseType.getMaximumWidth(comp.getFontMetrics(getFont(comp)));
    }

    int getMaximumHeight(JComponent comp) {
        return getPreferredHeight(comp);
    }

    private Dimension maximumSize;
    public Dimension getMaximumSize(JComponent comp) {
        if (maximumSize != null) return maximumSize;
        return new Dimension(getMaximumWidth(comp), getMaximumHeight(comp));
    }

    private transient PropertyRendererComponent renderer;
    public PropertyRendererComponent getRendererComponent() {
        if (renderer == null) renderer = baseType.getRendererComponent(getFormat(), caption, getFont());
        return renderer;
    }

    // диалог для получения возможных значений, используются только в нижних методах
    public abstract RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException;
    public abstract RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException;

    public abstract PropertyEditorComponent getEditorComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException;
    public abstract PropertyEditorComponent getClassComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException;

    private Font font;
    public Font getFont() {
        return font;
    }
    public Font getFont(JComponent comp) {
        return (font == null ? comp.getFont() : font);
    }

    private Format format;
    Format getFormat() {
        if (format == null) return baseType.getDefaultFormat();
        return format;
    }

    public String toString() { return caption; }

    public String getFullCaption() {

        String fullCaption = caption;
        if (showEditKey && editKey != null)
            fullCaption += " (" + SwingUtils.getKeyStrokeCaption(editKey) + ")";
        return fullCaption;
    }
}
