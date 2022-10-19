package lsfusion.server.logics.classes.data.utils.image.opencv;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Iterator;

public class ImageToStringAction extends InternalAction {
    protected final ClassPropertyInterface imageInterface;
    protected final ClassPropertyInterface languageInterface;
    protected final ClassPropertyInterface oemInterface;
    protected final ClassPropertyInterface psmInterface;

    public ImageToStringAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        imageInterface = i.next();
        languageInterface = i.next();
        oemInterface = i.next();
        psmInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        FileData imageObject = (FileData) context.getKeyValue(imageInterface).getValue();

        if (imageObject != null) {
            RawFileData imageFile = imageObject.getRawFile();
            String extension = imageObject.getExtension();

            String language = (String) context.getKeyValue(languageInterface).getValue(); //rus
            Integer oem = (Integer) context.getKeyValue(oemInterface).getValue(); //1
            Integer psm = (Integer) context.getKeyValue(psmInterface).getValue(); //3

            File tmpFile = null;
            try {

                String tessDataPath = (String) findProperty("tessDatPath[]").read(context);
                if (tessDataPath != null) {

                    tmpFile = File.createTempFile("opencv", "." + extension);

                    FileUtils.writeByteArrayToFile(tmpFile, imageFile.getBytes());

                    String result = imageToString(tmpFile, tessDataPath, language, oem, psm);

                    findProperty("imageToStringResult[]").change(result, context);
                } else {
                    throw new RuntimeException("Tess DataPath not defined");
                }

            } catch (Throwable t) {
                throw Throwables.propagate(t);
            } finally {
                BaseUtils.safeDelete(tmpFile);
            }
        } else {
            throw new RuntimeException("No image");
        }
    }

    private String imageToString(File file, String tessDataPath, String language, Integer oem, Integer psm) throws TesseractException {

        File imageFile = new File(file.getAbsolutePath());
        Tesseract instance = new Tesseract();
        instance.setDatapath(tessDataPath);
        instance.setLanguage(language);

        if (oem != null) {
            instance.setOcrEngineMode(oem);
        }

        if (psm != null) {
            instance.setPageSegMode(psm);
        }

        return instance.doOCR(imageFile);
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
