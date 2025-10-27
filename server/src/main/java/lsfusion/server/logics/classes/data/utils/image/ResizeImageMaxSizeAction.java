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
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;

import static java.awt.image.BufferedImage.TYPE_BYTE_INDEXED;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class ResizeImageMaxSizeAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface maxSizeInterface;

    public ResizeImageMaxSizeAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
        maxSizeInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            RawFileData inputFile = (RawFileData) context.getKeyValue(fileInterface).getValue();
            Integer maxSize = (Integer) context.getKeyValue(maxSizeInterface).getValue();

            BufferedImage image = ImageIO.read(inputFile.getInputStream());
            if (image != null) {
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                double scale = (double) maxSize / Math.max(imageWidth, imageHeight);
                if (scale != 0) {
                    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                        Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(inputFile.getInputStream()).scale(scale, scale);
                        if (image.getType() == TYPE_BYTE_INDEXED) {
                            builder.imageType(TYPE_INT_ARGB);
                        }
                        builder.toOutputStream(os);
                        findProperty("resizedImage[]").change(new RawFileData(os), context);
                    }
                }
            } else {
                throw new RuntimeException("Failed to read image");
            }
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}