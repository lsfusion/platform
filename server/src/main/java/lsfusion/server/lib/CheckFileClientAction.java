package lsfusion.server.lib;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class CheckFileClientAction implements ClientAction {
    String path;
    String filename;

    public CheckFileClientAction(String path, String filename) {
        this.path = path;
        this.filename = filename;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        String hash = null;
        try {
            File dll = new File(path + filename);
            if (dll.exists()) {
                hash = new String(Hex.encodeHex(MessageDigest.getInstance("MD5").digest(FileUtils.readFileToByteArray(dll))));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }
}