package lsfusion.server.logics.classes.data.utils.image;

import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public class MergeImagesAction extends InternalAction {
    public MergeImagesAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            RawFileData backgroundFile = (RawFileData) getParam(0, context);
            RawFileData frontFile = (RawFileData) getParam(1, context);

            findProperty("mergedImage[]").change(mergeImages(backgroundFile, frontFile), context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private RawFileData mergeImages(RawFileData backgroundFile, RawFileData frontFile) throws IOException {
        RawFileData result = null;
        if (backgroundFile != null && frontFile != null) {
            BufferedImage backgroundImage = ImageIO.read(backgroundFile.getInputStream());
            BufferedImage frontImage = ImageIO.read(frontFile.getInputStream());

            int backgroundWidth = backgroundImage.getWidth();
            int backgroundHeight = backgroundImage.getHeight();
            int frontWidth = frontImage.getWidth();
            int frontHeight = frontImage.getHeight();

            if (backgroundWidth >= frontWidth && backgroundHeight >= frontHeight) {

                BufferedImage mergedImage = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);

                Graphics g = mergedImage.getGraphics();
                g.drawImage(backgroundImage, 0, 0, null);
                //draw front image at the center of background image
                g.drawImage(frontImage, (backgroundWidth - frontWidth) / 2, (backgroundHeight - frontHeight) / 2, null);
                g.dispose();

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(mergedImage, "JPG", os);
                result = new RawFileData(os);
            } else {
                throw new RuntimeException("Background image must be bigger than front image");
            }
        }
        return result;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}