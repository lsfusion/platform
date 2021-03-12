package lsfusion.client.base.equ;

import com.jacob.com.LibraryLoader;
import lsfusion.base.SystemUtils;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ComBridge {

    public static void loadJacobLibraries() throws IOException {
        try {
            System.setProperty(LibraryLoader.JACOB_DLL_PATH, SystemUtils.getLibraryPath("jacob-1.20", "/lsfusion/client/", ComBridge.class));
        } catch (FileNotFoundException ignored) {
        }
    }

    public static void loadJsscLibraries() throws IOException {
        try {
            SystemUtils.loadLibrary("libjssc", "/lsfusion/client/", ComBridge.class);
        } catch (FileNotFoundException | UnsatisfiedLinkError ignored) {
        }

    }
}
