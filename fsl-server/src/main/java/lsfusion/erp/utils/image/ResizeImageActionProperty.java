package lsfusion.erp.utils.image;

import com.google.common.base.Throwables;
import lsfusion.base.IOUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.logics.scripted.ScriptingModuleErrorLog;
import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class ResizeImageActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface scaleInterface;

    public ResizeImageActionProperty(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
        scaleInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            byte[] inputFile = (byte[]) context.getKeyValue(fileInterface).getValue();
            Double scale = (Double) context.getKeyValue(scaleInterface).getValue();

            File outputFile = null;
            try {
                outputFile = File.createTempFile("resized", ".jpg");
                if(scale != 0) {
                    Thumbnails.of(new ByteArrayInputStream(inputFile)).scale((double) 1 / scale).toFile(outputFile);
                    findProperty("resizedImage[]").change(IOUtils.getFileBytes(outputFile), context);
                }
            } finally {
                if (outputFile != null && !outputFile.delete())
                    outputFile.deleteOnExit();
            }

        } catch (IOException | ScriptingModuleErrorLog.SemanticError e) {
            throw Throwables.propagate(e);
        }
    }
}
