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
    public abstract ClientGroupObjectImplementView getGroupObject();

    // диалог для получения возможных значений
    public abstract RemoteFormInterface createForm(RemoteNavigatorInterface navigator) throws RemoteException;
    public abstract RemoteFormInterface createClassForm(RemoteNavigatorInterface navigator, Integer value) throws RemoteException;

    private Dimension minimumSize;
    private Dimension maximumSize;
    private Dimension preferredSize;

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

    public Dimension getMaximumSize() {

        if (maximumSize != null) return maximumSize;
        return new Dimension(getMaximumWidth(), getMaximumHeight());
    }

    private transient PropertyRendererComponent renderer;
    public PropertyRendererComponent getRendererComponent() {

        if (renderer == null) renderer = baseType.getRendererComponent(getFormat());

        return renderer;
    }

    public abstract PropertyEditorComponent getEditorComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException;
    public abstract PropertyEditorComponent getClassComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException;

    private Format format;
    Format getFormat() {
        if (format == null) return baseType.getDefaultFormat();
        return format;
    }

    public String toString() { return caption; }

}
