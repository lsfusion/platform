package lsfusion.client.navigator;

import lsfusion.base.BaseUtils;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.AppImage;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.controller.MainController;
import lsfusion.client.form.property.async.ClientAsyncExec;
import lsfusion.client.form.property.async.ClientAsyncSerializer;
import lsfusion.client.navigator.window.ClientNavigatorWindow;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.remote.serialization.SerializationUtil;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.base.SwingUtils.getEventCaption;

public abstract class ClientNavigatorElement {

    private String canonicalName;

    public String creationPath;
    public String path;
    public String caption;
    public boolean hide;

    public KeyInputEvent changeKey;
    public Integer changeKeyPriority;
    public boolean showChangeKey;
    public MouseInputEvent changeMouse;
    public Integer changeMousePriority;
    public boolean showChangeMouse;

    public String elementClass;

    public ClientNavigatorElement parent;
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

    protected boolean hasChildren;
    public ClientNavigatorWindow window;
    public boolean parentWindow;

    public ClientNavigatorElement(DataInputStream inStream, Map<String, ClientNavigatorWindow> windows) throws IOException {
        canonicalName = SerializationUtil.readString(inStream);
        creationPath = SerializationUtil.readString(inStream);
        path = SerializationUtil.readString(inStream);

        caption = inStream.readUTF();
        elementClass = SerializationUtil.readString(inStream);
        hasChildren = inStream.readBoolean();
        String windowCanonicalName = SerializationUtil.readString(inStream);
        if(windowCanonicalName != null) {
            window = windows.get(windowCanonicalName);
            parentWindow = inStream.readBoolean();
        }

        changeKey = BaseUtils.readObject(inStream);
        changeKeyPriority = SerializationUtil.readInt(inStream);
        showChangeKey = inStream.readBoolean();
        changeMouse = BaseUtils.readObject(inStream);
        changeMousePriority = SerializationUtil.readInt(inStream);
        showChangeMouse = inStream.readBoolean();

        appImage = IOUtils.readAppImage(inStream);

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
        return getCaption();
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
            case 1: element = new ClientNavigatorFolder(inStream, windows); break;
            case 2: element = new ClientNavigatorAction(inStream, windows); break;
            default:
                throw new IOException("Incorrect navigator element type");
        }

        return element;
    }

    public String getTooltip() {
        return MainController.showDetailedInfo && creationPath != null ?
                String.format("<html>%s" +
                        "<b>sID:</b> %s<br/>" +
                        "<b>" + getString("logics.scriptpath") + ":</b> %s<br/>" +
                        "</html>", caption != null ? ("<b>" + getCaption() + "</b><br/><hr>") : "",
                        canonicalName, creationPath) : getCaption();
    }

    private String getCaption() {
        String eventCaption = getEventCaption(changeKey, showChangeKey, changeMouse, showChangeMouse);
        return caption != null ? (caption + (eventCaption != null ? (" (" + eventCaption + ")") : "")) : null;
    }
}
