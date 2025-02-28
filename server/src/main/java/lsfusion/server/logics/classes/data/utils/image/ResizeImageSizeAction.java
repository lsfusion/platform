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

public class ResizeImageSizeAction extends InternalAction {
    public ResizeImageSizeAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            RawFileData inputFile = (RawFileData) getParam(0, context);
            Integer width = (Integer) getParam(1, context);
            Integer height = (Integer) getParam(2, context);

            findProperty("resizedImage[]").change(getResizedImage(inputFile, width, height), context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private RawFileData getResizedImage(RawFileData inputFile, Integer width, Integer height) throws IOException {
        RawFileData result = null;
        if (inputFile != null) {
            if (width != null || height != null) {

                BufferedImage image = ImageIO.read(inputFile.getInputStream());
                if (image != null) {
                    int imageWidth = image.getWidth();
                    int imageHeight = image.getHeight();

                    double scaleWidth = width != null ? ((double) width / imageWidth) : (double) height / imageHeight;
                    double scaleHeight = height != null ? (double) height / imageHeight : ((double) width / imageWidth);

                    if (scaleWidth != 0 && scaleHeight != 0) {
                        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                            Thumbnails.of(inputFile.getInputStream()).scale(scaleWidth, scaleHeight).toOutputStream(os);
                            result = new RawFileData(os);
                        }
                    }
                } else {
                    throw new RuntimeException("Failed to read image");
                }
            } else {
                throw new RuntimeException("No width nor height found");
            }
        }
        return result;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}