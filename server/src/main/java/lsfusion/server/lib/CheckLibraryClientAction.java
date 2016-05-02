package lsfusion.server.lib;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class CheckLibraryClientAction implements ClientAction {
    String path;
    String filename;

    public CheckLibraryClientAction(String path, String filename) {
        this.path = path;
        this.filename = filename;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        String hash = null;
        try {
            File dll = new File(path + filename);
            setJNALibraryPath(dll.getParentFile().getAbsolutePath());
            if (dll.exists()) {
                hash = new String(Hex.encodeHex(MessageDigest.getInstance("MD5").digest(FileUtils.readFileToByteArray(dll))));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }

    protected String setJNALibraryPath(String path) throws IOException {
        String javaLibraryPath = System.getProperty("jna.library.path");
        if (javaLibraryPath == null) {
            System.setProperty("jna.library.path", path);
        } else if (!javaLibraryPath.contains(path)) {
            System.setProperty("jna.library.path", path + ";" + javaLibraryPath);
        }
        return path;
    }
}