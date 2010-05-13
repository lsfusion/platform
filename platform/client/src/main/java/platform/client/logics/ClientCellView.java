package platform.client.logics;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.logics.classes.ClientType;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

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

    ClientCellView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        caption = inStream.readUTF();

        baseType = ClientTypeSerializer.deserialize(inStream);

        minimumSize = (Dimension) new ObjectInputStream(inStream).readObject();
        maximumSize = (Dimension) new ObjectInputStream(inStream).readObject();
        preferredSize = (Dimension) new ObjectInputStream(inStream).readObject();
    }

    public int getMinimumWidth() {
        return baseType.getMinimumWidth();
    }

    int getMinimumHeight() {
        return getPreferredHeight();
    }

    private Dimension minimumSize;
    public Dimension getMinimumSize() {
        if (minimumSize != null) return minimumSize;
        return new Dimension(getMinimumWidth(), getMinimumHeight());
    }

    public int getPreferredWidth() {
        return baseType.getPreferredWidth();
    }

    int getPreferredHeight() {
        return 15;
    }

    private Dimension preferredSize;
    public Dimension getPreferredSize() {
        if (preferredSize != null) return preferredSize;
        return new Dimension(getPreferredWidth(), getPreferredHeight());
    }

    public int getMaximumWidth() {
        return baseType.getMaximumWidth();
    }

    int getMaximumHeight() {
        return getPreferredHeight();
    }

    private Dimension maximumSize;
    public Dimension getMaximumSize() {
        if (maximumSize != null) return maximumSize;
        return new Dimension(getMaximumWidth(), getMaximumHeight());
    }

    private transient PropertyRendererComponent renderer;
    public PropertyRendererComponent getRendererComponent() {
        if (renderer == null) renderer = baseType.getRendererComponent(getFormat(), caption);
        return renderer;
    }

    // диалог для получения возможных значений, используются только в нижних методах
    public abstract RemoteFormInterface createEditorForm(RemoteNavigatorInterface navigator, int callerID) throws RemoteException;
    public abstract RemoteFormInterface createClassForm(RemoteNavigatorInterface navigator, int callerID, Integer value) throws RemoteException;

    public abstract PropertyEditorComponent getEditorComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException;
    public abstract PropertyEditorComponent getClassComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException;

    private Format format;
    Format getFormat() {
        if (format == null) return baseType.getDefaultFormat();
        return format;
    }

    public String toString() { return caption; }
}
