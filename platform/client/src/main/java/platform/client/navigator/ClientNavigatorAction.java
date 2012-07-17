package platform.client.navigator;

import platform.gwt.view2.GNavigatorElement;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorAction extends ClientNavigatorElement {
    public ClientNavigatorAction() {
    }

    public ClientNavigatorAction(int ID, String sID, String caption) {
        super(ID, sID, caption, false);
    }

    public ClientNavigatorAction(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    private GNavigatorElement gwtNavigatorElement;
    public GNavigatorElement getGwtElement() {
        //todo:
        if (gwtNavigatorElement == null) {
            gwtNavigatorElement = super.getGwtElement();
            gwtNavigatorElement.icon = "form.png";
            gwtNavigatorElement.isForm = true;
        }
        return gwtNavigatorElement;
    }
}
