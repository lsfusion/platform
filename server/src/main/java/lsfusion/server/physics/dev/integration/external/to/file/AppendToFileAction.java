package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.IOUtils;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;

public class AppendToFileAction extends InternalAction {
    private final ClassPropertyInterface pathInterface;
    private final ClassPropertyInterface textInterface;
    private final ClassPropertyInterface charsetInterface;

    public AppendToFileAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        pathInterface = i.next();
        textInterface = i.next();
        charsetInterface = i.next();
    }

    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {

        String filePath = (String) context.getDataKeyValue(pathInterface).getValue();
        String text = (String) context.getDataKeyValue(textInterface).getValue();
        String charset = (String) context.getDataKeyValue(charsetInterface).getValue();

        try {
            if (new File(filePath).exists()) {
                Files.write(Paths.get(filePath), text.getBytes(charset), StandardOpenOption.APPEND);
            } else {
                IOUtils.putFileBytes(new File(filePath), text.getBytes(charset));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}