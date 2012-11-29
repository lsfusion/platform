package platform.fdk.actions.geo;

import java.net.URI;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.awt.*;
import java.io.IOException;
import java.net.URI;


public class OpenUriClientAction implements ClientAction {

    URI uri;

    public OpenUriClientAction(URI uri) {
        this.uri = uri;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE))
            desktop.browse(uri);
        return true;
    }
}
