package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.ReadUtils;
import lsfusion.server.logics.property.actions.WriteActionProperty;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MoveFileActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface destinationInterface;
    private final ClassPropertyInterface isClientInterface;

    public MoveFileActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        sourceInterface = i.next();
        destinationInterface = i.next();
        isClientInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String sourcePath = (String) context.getKeyValue(sourceInterface).getValue();
            String destinationPath = (String) context.getKeyValue(destinationInterface).getValue();
            boolean isClient = context.getKeyValue(isClientInterface).getValue() != null;
            if (sourcePath != null && destinationPath != null) {

                if(isClient) {

                    Pattern p = Pattern.compile("(?:file:(?://)?)?(.*)");
                    Matcher m = p.matcher(sourcePath);
                    if (m.matches()) {
                        boolean result = (boolean) context.requestUserInteraction(new FileClientAction(2, sourcePath.replace("file://", ""), destinationPath.replace("file://", "")));
                        if (!result)
                            throw Throwables.propagate(new RuntimeException(String.format("Failed to move file from %s to %s", sourcePath, destinationPath)));
                    } else {
                        throw Throwables.propagate(new RuntimeException("Unsupported move source: " + sourcePath + ". Please use format: file://path"));
                    }

                } else {

                    Pattern p = Pattern.compile("(?:(file|ftp|sftp):(?://)?)?(.*)");
                    Matcher m = p.matcher(sourcePath);
                    if (m.matches()) {
                        String type = m.group(1) == null ? "file" : m.group(1).toLowerCase();

                        ReadUtils.ReadResult readResult = ReadUtils.readFile(sourcePath, false, false, false);
                        if (readResult.errorCode == 0) {
                            File sourceFile = new File(readResult.filePath);
                            try {
                                if (destinationPath.startsWith("ftp://")) {
                                    WriteActionProperty.storeFileToFTP(destinationPath, sourceFile);
                                } else if (destinationPath.startsWith("sftp://")) {
                                    WriteActionProperty.storeFileToSFTP(destinationPath, sourceFile);
                                } else {
                                    FileCopyUtils.copy(sourceFile, new File(destinationPath.replace("file://", "")));
                                }
                            } finally {
                                ReadUtils.deleteFile(type, sourcePath);
                            }

                        } else if (readResult.error != null) {
                            throw Throwables.propagate(new RuntimeException(readResult.error));
                        }
                    } else {
                        throw Throwables.propagate(new RuntimeException("Unsupported move source: " + sourcePath + ". Please use format: file://path or ftp|sftp://username:password;charset@host:port/path"));
                    }
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}