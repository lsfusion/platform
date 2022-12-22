package lsfusion.client.navigator;

import lsfusion.base.file.IOUtils;
import lsfusion.base.file.AppImage;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.property.async.ClientAsyncExec;
import lsfusion.client.form.property.async.ClientAsyncSerializer;
import lsfusion.client.navigator.window.ClientNavigatorWindow;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import javax.swing.*;
import java.awt.*;
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
    public String path;
    public String caption;
    
    public List<ClientNavigatorElement> parents = new ArrayList<>();
    public List<ClientNavigatorElement> children = new ArrayList<>();
    public AppImage appImage;
    public Image fileImage = null;

    public ImageIcon getImage() {
        return fileImage != null ? new ImageIcon(fileImage) : ClientImages.getImage(appImage);
    }

    public ClientAsyncExec asyncExec;

    public boolean isDesktopAsync() {
        return asyncExec != null && asyncExec.isDesktopEnabled(true);
    }

    protected boolean hasChildren = false;
    public ClientNavigatorWindow window;
    public boolean parentWindow;

    public ClientNavigatorElement(DataInputStream inStream) throws IOException {
        canonicalName = SerializationUtil.readString(inStream);
        creationPath = SerializationUtil.readString(inStream);
        path = SerializationUtil.readString(inStream);

        caption = inStream.readUTF();
        hasChildren = inStream.readBoolean();
        window = ClientNavigatorWindow.deserialize(inStream);
        if(window != null) {
            parentWindow = inStream.readBoolean();
        }

        appImage = IOUtils.readImageIcon(inStream);

        asyncExec = (ClientAsyncExec) ClientAsyncSerializer.deserializeEventExec(inStream);
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

    public ClientNavigatorElement findChild(ClientNavigatorElement element) {
        if (element == null) {
            return null;
        }
        if (element == this) {
            return this;
        }

        for (ClientNavigatorElement child : children) {
            ClientNavigatorElement found = child.findChild(element);
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
