package lsfusion.interop.action;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;


public class OpenUriClientAction extends ExecuteClientAction {

    public URI uri;

    public OpenUriClientAction(URI uri) {
        this.uri = uri;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            if (uri.getScheme() != null && uri.getScheme().equals("file"))
                uri = Paths.get(uri).normalize().toUri();
            desktop.browse(uri);
        }
    }
}
