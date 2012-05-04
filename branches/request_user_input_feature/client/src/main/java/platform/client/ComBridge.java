package platform.client;

import com.jacob.com.LibraryLoader;
import platform.base.OSUtils;

import java.io.IOException;

public class ComBridge {

    public static void loadJacobLibraries() throws IOException {
        System.setProperty(LibraryLoader.JACOB_DLL_PATH, OSUtils.getLibraryPath("jacob-1.15-M3", "/platform/client/", ComBridge.class));
    }

    public static void loadJsscLibraries() throws IOException {
        OSUtils.loadLibrary("libjssc", "/platform/client/", ComBridge.class);
    }
}
