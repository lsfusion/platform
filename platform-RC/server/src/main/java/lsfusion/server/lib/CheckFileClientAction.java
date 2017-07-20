package lsfusion.server.lib;

import lsfusion.base.SystemUtils;
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

    public CheckFileClientAction(String path) {
        this.path = path;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        String hash = null;
        try {
            File file = SystemUtils.getUserFile(path);
            if (file.exists()) {
                hash = new String(Hex.encodeHex(MessageDigest.getInstance("MD5").digest(FileUtils.readFileToByteArray(file))));
            }
        } catch (NoSuchAlgorithmException e) {
            ClientActionLogger.logger.error("CheckFile Error: ", e);
        }
        return hash;
    }
}