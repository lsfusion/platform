package lsfusion.server.logics.classes.data.utils.image.opencv;

import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class ImageToStringAction extends InternalAction {
    public ImageToStringAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException{
        try {
            FileData imageObject = (FileData) getParam(0, context);
            String language = (String) getParam(1, context); //rus
            Integer oem = (Integer) getParam(2, context); //1
            Integer psm = (Integer) getParam(3, context); //3
            String tessDataPath = (String) findProperty("tessDatPath[]").read(context);

            findProperty("imageToStringResult[]").change(imageToString(tessDataPath, imageObject, language, oem, psm), context);
        } catch (ScriptingErrorLog.SemanticErrorException | TesseractException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String imageToString(String tessDataPath, FileData imageObject, String language, Integer oem, Integer psm) throws IOException, TesseractException {
        String result = null;
        if (imageObject != null) {
            File tmpFile = null;
            try {
                if (tessDataPath != null) {
                    tmpFile = File.createTempFile("opencv", "." + imageObject.getExtension());
                    FileUtils.writeByteArrayToFile(tmpFile, imageObject.getRawFile().getBytes());

                    Tesseract instance = new Tesseract();
                    instance.setDatapath(tessDataPath);
                    instance.setLanguage(language);
                    if (oem != null) {
                        instance.setOcrEngineMode(oem);
                    }
                    if (psm != null) {
                        instance.setPageSegMode(psm);
                    }
                    result = instance.doOCR(tmpFile);
                } else {
                    throw new RuntimeException("Tess DataPath not defined");
                }
            }  finally {
                BaseUtils.safeDelete(tmpFile);
            }
        }
        return result;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
