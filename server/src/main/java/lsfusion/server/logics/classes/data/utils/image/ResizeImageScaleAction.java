package lsfusion.server.logics.classes.data.utils.image;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class ResizeImageScaleAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface scaleInterface;

    public ResizeImageScaleAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
        scaleInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            RawFileData inputFile = (RawFileData) context.getKeyValue(fileInterface).getValue();
            Double scale = (Double) context.getKeyValue(scaleInterface).getValue();

            if (scale != 0) {
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    Thumbnails.of(inputFile.getInputStream()).scale((double) 1 / scale).toOutputStream(os);
                    findProperty("resizedImage[]").change(new RawFileData(os), context);
                }
            }
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}