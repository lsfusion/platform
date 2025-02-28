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

public class ResizeImageScaleAction extends InternalAction {
    public ResizeImageScaleAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            RawFileData inputFile = (RawFileData) getParam(0, context);
            Number scale = (Number) getParam(1, context);

            findProperty("resizedImage[]").change(getResizedImage(inputFile, scale), context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private RawFileData getResizedImage(RawFileData inputFile, Number scale) throws IOException {
        RawFileData result = null;
        if (inputFile != null && scale != null) {
            double doubleScale = scale.doubleValue();
            if(doubleScale != 0) {
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    Thumbnails.of(inputFile.getInputStream()).scale((double) 1 / doubleScale).toOutputStream(os);
                    result = new RawFileData(os);
                }
            }
        }
        return result;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}