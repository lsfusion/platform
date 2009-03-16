package platform.client.logics;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.logics.classes.ClientClass;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.Format;
import java.util.Collection;
import java.rmi.RemoteException;

abstract public class ClientCellView extends ClientComponentView {

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    private ClientClass baseClass;

    public abstract int getID();
    public abstract ClientGroupObjectImplementView getGroupObject();

    private Dimension minimumSize;
    private Dimension maximumSize;
    private Dimension preferredSize;

    public String caption;

    ClientCellView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        caption = inStream.readUTF();

        baseClass = ClientClass.deserialize(inStream);

        minimumSize = (Dimension) new ObjectInputStream(inStream).readObject();
        maximumSize = (Dimension) new ObjectInputStream(inStream).readObject();
        preferredSize = (Dimension) new ObjectInputStream(inStream).readObject();
    }

    public int getMinimumWidth() {
        return baseClass.getMinimumWidth();
    }

    int getMinimumHeight() {
        return getPreferredHeight();
    }

    public Dimension getMinimumSize() {

        if (minimumSize != null) return minimumSize;
        return new Dimension(getMinimumWidth(), getMinimumHeight());
    }

    public int getPreferredWidth() {
        return baseClass.getPreferredWidth();
    }

    int getPreferredHeight() {
        return 15;
    }

    public Dimension getPreferredSize() {

        if (preferredSize != null) return preferredSize;
        return new Dimension(getPreferredWidth(), getPreferredHeight());
    }

    public int getMaximumWidth() {
        return baseClass.getMaximumWidth();
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

        if (renderer == null) renderer = baseClass.getRendererComponent(getFormat());

        return renderer;
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value, boolean isDataChanging, boolean externalID) throws IOException, ClassNotFoundException {

        ClientObjectValue objectValue;
        if (isDataChanging)
            objectValue = getEditorObjectValue(form, value, externalID);
        else
            objectValue = new ClientObjectValue(baseClass, value);

        if (objectValue == null) return null;

        return objectValue.cls.getEditorComponent(form, this, objectValue.object, getFormat());
    }

    ClientObjectValue getEditorObjectValue(ClientForm form, Object value, boolean externalID) throws IOException {
        if (externalID) return null;
        return new ClientObjectValue(baseClass, value);
    }

    private Format format;
    Format getFormat() {
        if (format == null) return baseClass.getDefaultFormat();
        return format;
    }

    public String toString() { return caption; }

}
