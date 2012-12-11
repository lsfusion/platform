package platform.interop.action;

import java.awt.*;
import java.io.IOException;
import java.net.URI;


public class OpenUriClientAction extends ExecuteClientAction {

    public URI uri;

    public OpenUriClientAction(URI uri) {
        this.uri = uri;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE))
            desktop.browse(uri);
    }
}
