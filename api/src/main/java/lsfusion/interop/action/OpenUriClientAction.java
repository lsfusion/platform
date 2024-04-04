package lsfusion.interop.action;

import com.google.common.base.Throwables;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;


public class OpenUriClientAction extends ExecuteClientAction {

    public URI uri = null;
    public URL url = null;
    private URISyntaxException uriSyntaxException = null;

    public OpenUriClientAction(String path) {
        this(path, false);
    }

    public OpenUriClientAction(String path, boolean decode) {
        try {
            try {
                uri = new URI((decode ? URIUtil.decode(path) : path).trim()); // trim is needed because space characters in str cause URISyntaxException
            } catch (URISyntaxException uriSyntaxException) {
                this.uriSyntaxException = uriSyntaxException;

                // LINK property allows to save a valid URL that will contain characters that are NOT VALID for the URI.
                // Try to create a URL object ONLY for the web-client, because URI object is important ONLY in desktop-client
                url = new URL(URIUtil.decode(path).trim());
            }
        } catch (URIException | MalformedURLException e) {
            throw Throwables.propagate(e);
        }

    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        if (uriSyntaxException != null)
            throw Throwables.propagate(uriSyntaxException);

        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            if (uri.getScheme() != null && uri.getScheme().equals("file"))
                uri = Paths.get(uri).normalize().toUri();
            desktop.browse(uri);
        }
    }

    public String getString() {
        return uri != null ? uri.toString() : url.toString();
    }
}
