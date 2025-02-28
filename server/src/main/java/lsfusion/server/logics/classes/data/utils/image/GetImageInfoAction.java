package lsfusion.server.logics.classes.data.utils.image;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.file.RawFileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;

public class GetImageInfoAction extends InternalAction {
    public GetImageInfoAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            RawFileData inputFile = (RawFileData) getParam(0, context);

            Pair<Integer, Integer> imageInfo = getImageInfo(inputFile);
            findProperty("widthImageInfo[]").change(imageInfo.first, context);
            findProperty("heightImageInfo[]").change(imageInfo.second, context);
        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }

    private Pair<Integer, Integer> getImageInfo(RawFileData inputFile) throws IOException {
        Integer width = null, height = null;
        if (inputFile != null) {
            BufferedImage image = ImageIO.read(inputFile.getInputStream());
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            } else {
                throw new RuntimeException("Failed to read image");
            }
        }
        return Pair.create(width, height);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}