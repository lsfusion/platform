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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public class ResizeImageMaxSizeAction extends InternalAction {
    public ResizeImageMaxSizeAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            RawFileData inputFile = (RawFileData) getParam(0, context);
            Integer maxSize = (Integer) getParam(1, context);

            findProperty("resizedImage[]").change(getResizedImage(inputFile, maxSize), context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private RawFileData getResizedImage(RawFileData inputFile, Integer maxSize) throws IOException {
        RawFileData result = null;
        if (inputFile != null && maxSize != null) {
            BufferedImage image = ImageIO.read(inputFile.getInputStream());
            if (image != null) {
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();
                double scale = (double) maxSize / Math.max(imageWidth, imageHeight);
                if (scale != 0) {
                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        Thumbnails.of(inputFile.getInputStream()).scale(scale, scale).toOutputStream(os);
                        result = new RawFileData(os);
                    }
                }
            } else {
                throw new RuntimeException("Failed to read image");
            }
        }
        return result;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}