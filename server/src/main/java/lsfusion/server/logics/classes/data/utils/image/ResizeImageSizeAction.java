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

public class ResizeImageSizeAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface widthInterface;
    private final ClassPropertyInterface heightInterface;

    public ResizeImageSizeAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
        widthInterface = i.next();
        heightInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            RawFileData inputFile = (RawFileData) context.getKeyValue(fileInterface).getValue();
            Integer width = (Integer) context.getKeyValue(widthInterface).getValue();
            Integer height = (Integer) context.getKeyValue(heightInterface).getValue();

            BufferedImage image = ImageIO.read(inputFile.getInputStream());
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            double scaleWidth = (double) width / imageWidth;
            double scaleHeight = (double) height / imageHeight;

            File outputFile = null;
            try {
                outputFile = File.createTempFile("resized", ".jpg");
                if(scaleWidth != 0 && scaleHeight != 0) {
                    Thumbnails.of(inputFile.getInputStream()).scale(scaleWidth, scaleHeight).toFile(outputFile);
                    findProperty("resizedImage[]").change(new RawFileData(outputFile), context);
                }
            } finally {
                if (outputFile != null && !outputFile.delete())
                    outputFile.deleteOnExit();
            }

        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}