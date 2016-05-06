package lsfusion.server.lib;

import lsfusion.base.SystemUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.server.ServerLoggers;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class LoadDownloadedFontClientAction implements ClientAction {
    String path;

    public LoadDownloadedFontClientAction(String path) {
        this.path = path;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        File font = SystemUtils.getUserFile(path);
        if(font.exists()) {
            try {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, font));
            } catch (IOException|FontFormatException e) {
                ServerLoggers.systemLogger.error("LoadDownloadedFont Error: ", e);
            }
        }
        return null;
    }
}