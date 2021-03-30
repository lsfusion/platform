package lsfusion.server.logics.classes.data.utils.image.opencv;

import com.google.common.base.Throwables;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class ConvertToBlackAndWhiteAction extends InternalAction {
    protected final ClassPropertyInterface imageInterface;
    protected final ClassPropertyInterface thresholdInterface;

    public ConvertToBlackAndWhiteAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        imageInterface = i.next();
        thresholdInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        FileData imageObject = (FileData) context.getKeyValue(imageInterface).getValue();
        Integer threshold = (Integer) context.getKeyValue(thresholdInterface).getValue();

        if (imageObject != null && threshold != null) {
            RawFileData imageFile = imageObject.getRawFile();
            String extension = imageObject.getExtension();

            try {
                BufferedImage coloredImage = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
                if (coloredImage != null) {
                    BufferedImage blackNWhite = thresholdImage(coloredImage, threshold);
                    findProperty("convertToBlackAndWhiteResult[]").change(getFileData(blackNWhite, extension), context);
                } else {
                    throw new RuntimeException("Failed to read image");
                }

            } catch (Throwable t) {
                throw Throwables.propagate(t);
            }
        } else {
            throw new RuntimeException("No image or threshold");
        }
    }

    private FileData getFileData(BufferedImage image, String extension) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, extension, os);
        os.flush();
        byte[] result = os.toByteArray();
        os.close();
        return new FileData(new RawFileData(result), extension);
    }

    public static BufferedImage thresholdImage(BufferedImage image, int threshold) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        result.getGraphics().drawImage(image, 0, 0, null);
        WritableRaster raster = result.getRaster();
        int[] pixels = new int[image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++) {
            raster.getPixels(0, y, image.getWidth(), 1, pixels);
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] < threshold) pixels[i] = 0;
                else pixels[i] = 255;
            }
            raster.setPixels(0, y, image.getWidth(), 1, pixels);
        }
        return result;
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}