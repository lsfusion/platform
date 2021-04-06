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
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

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
            if(image != null) {
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();
    
                double scale = (double) maxSize / Math.max(imageWidth, imageHeight);
    
                File outputFile = null;
                try {
                    outputFile = File.createTempFile("resized", ".jpg");
                    if(scale != 0) {
                        Thumbnails.of(inputFile.getInputStream()).scale(scale, scale).toFile(outputFile);
                        findProperty("resizedImage[]").change(new RawFileData(outputFile), context);
                    }
                } finally {
                    if (outputFile != null && !outputFile.delete())
                        outputFile.deleteOnExit();
                }
            } else {
                throw new RuntimeException("Failed to read image");
            }



        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}