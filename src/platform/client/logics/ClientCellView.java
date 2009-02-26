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

abstract public class ClientCellView extends ClientComponentView {

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public ClientClass baseClass;

    public abstract int getID();
    public abstract ClientGroupObjectImplementView getGroupObject();

    Dimension minimumSize;
    Dimension maximumSize;
    Dimension preferredSize;

    public String caption;

    protected ClientCellView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {
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

    public int getMinimumHeight() {
        return getPreferredHeight();
    }

    public Dimension getMinimumSize() {

        if (minimumSize != null) return minimumSize;
        return new Dimension(getMinimumWidth(), getMinimumHeight());
    }

    public int getPreferredWidth() {
        return baseClass.getPreferredWidth();
    }

    public int getPreferredHeight() {
        return 15;
    }

    public Dimension getPreferredSize() {

        if (preferredSize != null) return preferredSize;
        return new Dimension(getPreferredWidth(), getPreferredHeight());
    }

    public int getMaximumWidth() {
        return baseClass.getMaximumWidth();
    }

    public int getMaximumHeight() {
        return getPreferredHeight();
    }

    public Dimension getMaximumSize() {

        if (maximumSize != null) return maximumSize;
        return new Dimension(getMaximumWidth(), getMaximumHeight());
    }

    transient protected PropertyRendererComponent renderer;
    public PropertyRendererComponent getRendererComponent(ClientForm form) {

        if (renderer == null) renderer = baseClass.getRendererComponent(getFormat());

        return renderer;
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value, boolean isDataChanging, boolean externalID) {

        ClientObjectValue objectValue;
        if (isDataChanging)
            objectValue = getEditorObjectValue(form, value, externalID);
        else
            objectValue = new ClientObjectValue(baseClass, value);

        if (objectValue == null) return null;

        return objectValue.cls.getEditorComponent(form, this, objectValue.object, getFormat());
    }

    protected ClientObjectValue getEditorObjectValue(ClientForm form, Object value, boolean externalID) {
        if (externalID) return null;
        return new ClientObjectValue(baseClass, value);
    }

    Format format;
    public Format getFormat() {
        if (format == null) return baseClass.getDefaultFormat();
        return format;
    }

    public String toString() { return caption; }

}
