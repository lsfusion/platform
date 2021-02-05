package lsfusion.client.navigator;

import lsfusion.base.file.IOUtils;
import lsfusion.base.file.SerializableImageIconHolder;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.property.async.ClientAsyncOpenForm;
import lsfusion.client.navigator.window.ClientNavigatorWindow;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class ClientNavigatorElement {

    private String canonicalName;

    public String creationPath;
    public String caption;
    
    public List<ClientNavigatorElement> parents = new ArrayList<>();
    public List<ClientNavigatorElement> children = new ArrayList<>();
    public SerializableImageIconHolder imageHolder;

    public ClientAsyncOpenForm asyncOpenForm;

    protected boolean hasChildren = false;
    public ClientNavigatorWindow window;

    public ClientNavigatorElement(DataInputStream inStream) throws IOException {
        canonicalName = SerializationUtil.readString(inStream);
        creationPath = SerializationUtil.readString(inStream);
        
        caption = inStream.readUTF();
        hasChildren = inStream.readBoolean();
        window = ClientNavigatorWindow.deserialize(inStream);

        imageHolder = IOUtils.readImageIcon(inStream);

        if(inStream.readBoolean()) {
            asyncOpenForm = new ClientAsyncOpenForm(SerializationUtil.readString(inStream), inStream.readBoolean());
        }
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public boolean hasChildren() {
        return hasChildren;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public int hashCode() {
        return canonicalName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClientNavigatorElement && ((ClientNavigatorElement) obj).canonicalName.equals(canonicalName);
    }

    public String toString() {
        return caption;
    }

    public ClientNavigatorElement findElementByCanonicalName(String canonicalName) {
        if (canonicalName == null) {
            return null;
        }
        if (canonicalName.equals(this.canonicalName)) {
            return this;
        }
        
        for (ClientNavigatorElement child : children) {
            ClientNavigatorElement found = child.findElementByCanonicalName(canonicalName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static ClientNavigatorElement deserialize(DataInputStream inStream, Map<String, ClientNavigatorWindow> windows) throws IOException {
        byte type = inStream.readByte();

        ClientNavigatorElement element;

        switch (type) {
            case 1: element = new ClientNavigatorFolder(inStream); break;
            case 2: element = new ClientNavigatorAction(inStream); break;
            default:
                throw new IOException("Incorrect navigator element type");
        }

        // todo [dale]: Это не помешало бы отрефакторить 
        // Так как окна десериализуются при десериализации каждого элемента навигатора, то необходимо замещать
        // окна с неуникальным каноническим именем, потому что такое окно уже было создано.
        if (element.window != null) {
            String windowCanonicalName = element.window.canonicalName;
            if (windows.containsKey(windowCanonicalName)) {
                element.window = windows.get(windowCanonicalName);
            } else {
                windows.put(windowCanonicalName, element.window);
            }
        }

        return element;
    }

    //содержатся ли родители текущей вершины в заданном множестве
    public boolean containsParent(Set<ClientNavigatorElement> set) {
        for (ClientNavigatorElement parent : parents) {
            if (set.contains(parent)) {
                return true;
            }
        }
        return false;
    }

    public String getTooltip() {
        return MainController.showDetailedInfo && creationPath != null ?
                String.format("<html><body>" +
                        "<b>%s</b><br/><hr>" +
                        "<b>sID:</b> %s<br/>" +
                        "<b>" + getString("logics.scriptpath") + ":</b> %s<br/>" +
                        "</body></html>", caption, canonicalName, creationPath) : caption;
    }
}
