package lsfusion.server.lib;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class LoadDownloadedFontClientAction implements ClientAction {
    String path;
    String filename;

    public LoadDownloadedFontClientAction(String path, String filename) {
        this.path = path;
        this.filename = filename;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        File font = new File(path + filename);
        if(font.exists()) {
            try {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, font));
            } catch (IOException|FontFormatException e) {
                return e.getMessage();
            }
        }
        return null;
    }
}