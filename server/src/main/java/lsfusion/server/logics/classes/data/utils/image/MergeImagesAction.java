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
import java.util.Iterator;

public class MergeImagesAction extends InternalAction {

    private final ClassPropertyInterface backgroundImageInterface;
    private final ClassPropertyInterface frontImageInterface;

    public MergeImagesAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        backgroundImageInterface = i.next();
        frontImageInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            BufferedImage backgroundImage = ImageIO.read(((RawFileData) context.getKeyValue(backgroundImageInterface).getValue()).getInputStream());
            BufferedImage frontImage = ImageIO.read(((RawFileData) context.getKeyValue(frontImageInterface).getValue()).getInputStream());

            int backgroundWidth = backgroundImage.getWidth();
            int backgroundHeight = backgroundImage.getHeight();
            int frontWidth = frontImage.getWidth();
            int frontHeight = frontImage.getHeight();

            if(backgroundWidth >= frontWidth && backgroundHeight >= frontHeight) {

                BufferedImage mergedImage = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);

                Graphics g = mergedImage.getGraphics();
                g.drawImage(backgroundImage, 0, 0, null);
                //draw front image at the center of background image
                g.drawImage(frontImage, (backgroundWidth - frontWidth) / 2, (backgroundHeight - frontHeight) / 2, null);
                g.dispose();

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(mergedImage, "JPG", os);
                findProperty("mergedImage[]").change(new RawFileData(os), context);
            } else {
                throw new RuntimeException("Background image must be bigger than front image");
            }



        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }
}