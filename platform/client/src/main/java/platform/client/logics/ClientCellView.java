package platform.client.logics;

import platform.client.SwingUtils;
import platform.client.form.ClientExternalScreen;
import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.CellView;
import platform.client.logics.classes.ClientType;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.interop.ComponentDesign;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenConstraints;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.RemoteException;
import java.text.Format;
import java.util.Collection;

abstract public class ClientCellView extends ClientComponentView {

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    ClientType baseType;

    public abstract int getID();
    public abstract int getShiftID(); // нужен для того, чтобы CellView мог генерировать уникальный ID
    public abstract ClientGroupObjectImplementView getGroupObject();

    public String caption;

    public KeyStroke editKey;
    public boolean showEditKey;

    public Boolean focusable;

    public boolean panelLabelAbove;

    public ClientExternalScreen externalScreen;
    public ExternalScreenConstraints externalScreenConstraints;

    ClientCellView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
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

        panelLabelAbove = inStream.readBoolean();

        if (inStream.readBoolean())
            externalScreen = ClientExternalScreen.getScreen(inStream.readInt());

        if (inStream.readBoolean())
            externalScreenConstraints = (ExternalScreenConstraints) new ObjectInputStream(inStream).readObject();
    }

    public int getMinimumWidth(JComponent comp) {
        return baseType.getMinimumWidth(comp.getFontMetrics(design.getFont(comp)));
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
        return baseType.getPreferredWidth(comp.getFontMetrics(design.getFont(comp)));
    }

    public int getPreferredHeight(JComponent comp) {
        return comp.getFontMetrics(design.getFont(comp)).getHeight() + 1;
    }

    private Dimension preferredSize;
    public Dimension getPreferredSize(JComponent comp) {
        if (preferredSize != null) return preferredSize;
        return new Dimension(getPreferredWidth(comp), getPreferredHeight(comp));
    }

    public int getMaximumWidth(JComponent comp) {
        return baseType.getMaximumWidth(comp.getFontMetrics(design.getFont(comp)));
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
        if (renderer == null) renderer = baseType.getRendererComponent(getFormat(), caption, design);
        return renderer;
    }

    public CellView getPanelComponent(ClientForm form) {
        return baseType.getPanelComponent(this, form);
    }

    // диалог для получения возможных значений, используются только в нижних методах
    public abstract RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException;
    public abstract RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException;

    // на данный момент ClientForm нужна для 2-х целей : как owner, создаваемых диалогов и как провайдер RemoteFormInterface, для получения того, что мы вообще редактируем
    public abstract PropertyEditorComponent getEditorComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException;
    public abstract PropertyEditorComponent getClassComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException;

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

    public boolean checkEquals() {
        return true;
    }
}
