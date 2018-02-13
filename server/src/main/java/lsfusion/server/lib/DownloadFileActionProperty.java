package lsfusion.server.lib;

import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import org.apache.commons.codec.binary.Hex;
import org.apache.poi.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Iterator;

public class DownloadFileActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;

    public DownloadFileActionProperty(BaseLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String path = (String) context.getDataKeyValue(pathInterface).getValue();
            if (path != null) {
                InputStream serverFile = this.getClass().getResourceAsStream("/" + path);
                if (serverFile != null) {
                    byte[] serverBytes = IOUtils.toByteArray((serverFile));
                    String serverHash = new String(Hex.encodeHex(MessageDigest.getInstance("MD5").digest(serverBytes)));

                    Object clientHash = context.requestUserInteraction(new CheckFileClientAction(path));

                    if (clientHash == null || !clientHash.equals(serverHash))
                        context.requestUserInteraction(new DownloadFileClientAction(path, serverBytes));
                }
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            ServerLoggers.systemLogger.error("DownloadFile Error", e);
        }
    }
}