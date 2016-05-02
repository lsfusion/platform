package lsfusion.server.lib;

import com.google.common.base.Throwables;
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

public class PrepareLibraryActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface pathInterface;
    private final ClassPropertyInterface filenameInterface;

    public PrepareLibraryActionProperty(BaseLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        pathInterface = i.next();
        filenameInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String path = (String) context.getDataKeyValue(pathInterface).getValue();
            String filename = (String) context.getDataKeyValue(filenameInterface).getValue();
            if (path != null && filename != null) {
                InputStream serverLib = this.getClass().getResourceAsStream("/" + path + filename);
                if (serverLib != null) {
                    byte[] serverBytes = IOUtils.toByteArray((serverLib));
                    String serverHash = new String(Hex.encodeHex(MessageDigest.getInstance("MD5").digest(serverBytes)));

                    Object clientHash = context.requestUserInteraction(new CheckLibraryClientAction(path, filename));

                    if (clientHash == null || !clientHash.equals(serverHash))
                        context.requestUserInteraction(new PrepareLibraryClientAction(path, filename, serverBytes));
                }
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            throw Throwables.propagate(e);
        }
    }
}