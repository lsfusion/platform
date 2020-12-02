package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.IOUtils;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.util.Iterator;

public class FileToStringAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface charsetInterface;

    public FileToStringAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        fileInterface = i.next();
        charsetInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        FileData fileData = (FileData) context.getKeyValue(fileInterface).getValue();
        String charset = (String) context.getKeyValue(charsetInterface).getValue();
        try {
            String fileString = fileData != null ? IOUtils.readStreamToString(fileData.getRawFile().getInputStream(), charset) : null;
            findProperty("resultString[]").change(fileString, context);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}