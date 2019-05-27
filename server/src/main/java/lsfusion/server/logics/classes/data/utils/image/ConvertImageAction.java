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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class ConvertImageAction extends InternalAction {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface convertExtensionInterface;

    public ConvertImageAction(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
        convertExtensionInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            RawFileData inputFile = (RawFileData) context.getKeyValue(fileInterface).getValue();
            String extension = (String) context.getKeyValue(convertExtensionInterface).getValue();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            BufferedImage bi = ImageIO.read(inputFile.getInputStream());
            ImageIO.write(bi, extension, os);
            findProperty("convertedImage[]").change(new RawFileData(os), context);

        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}