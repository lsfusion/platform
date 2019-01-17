package lsfusion.utils.utils;

import com.google.common.base.Throwables;
import lsfusion.base.RawFileData;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Iterator;

public class ImageToStringOpenCVActionProperty extends ScriptingActionProperty {
    protected final ClassPropertyInterface imageInterface;
    protected final ClassPropertyInterface tessDataPathInterface;
    protected final ClassPropertyInterface oemInterface;
    protected final ClassPropertyInterface psmInterface;

    public ImageToStringOpenCVActionProperty(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        imageInterface = i.next();
        tessDataPathInterface = i.next();
        oemInterface = i.next();
        psmInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) {
        ObjectValue imageObject = context.getKeyValue(imageInterface);
        String tessDataPath = (String) context.getKeyValue(tessDataPathInterface).getValue();

        if (imageObject instanceof DataObject && tessDataPath != null) {
            RawFileData imageFile = (RawFileData) imageObject.getValue();
            String extension = ((StaticFormatFileClass) ((DataObject) imageObject).getType()).getOpenExtension(imageFile);

            Integer oem = (Integer) context.getKeyValue(oemInterface).getValue(); //1
            Integer psm = (Integer) context.getKeyValue(psmInterface).getValue(); //3

            File tmpFile = null;
            try {
                tmpFile = File.createTempFile("opencv", "." + extension);

                FileUtils.writeByteArrayToFile(tmpFile, imageFile.getBytes());

                String result = imageToString(tmpFile, tessDataPath, oem, psm);

                findProperty("imageToStringResult[]").change(result, context);

            } catch (Throwable t) {
                throw Throwables.propagate(t);
            } finally {
                if (tmpFile != null && !tmpFile.delete()) {
                    tmpFile.deleteOnExit();
                }
            }
        } else {
            throw new RuntimeException("No image or dataPath");
        }
    }

    private String imageToString(File file, String tessDataPath, Integer oem, Integer psm) throws TesseractException {

        File imageFile = new File(file.getAbsolutePath());
        Tesseract instance = new Tesseract();
        instance.setDatapath(tessDataPath);
        instance.setLanguage("rus");

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
