package lsfusion.gwt.server;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.SerializableImageIconHolder;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.form.property.cell.classes.GFilesDTO;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
    // not that pretty with statics, in theory it's better to autowire LogicsHandlerProvider (or get it from servlet) and pass as a parameter here
    public static String APP_IMAGES_FOLDER_URL; // all files has to be prefixed with logicsName
    public static String APP_TEMP_FOLDER_URL; // all files hasn't to be prefixed because their names are (or prefixed with) random strings

    public static ImageDescription createImage(String logicsName, SerializableImageIconHolder iconHolder, String iconPath, String imagesFolderName, boolean canBeDisabled) {
        if (iconHolder != null) {
            String fullPath = logicsName + "/" + imagesFolderName;
            File imagesFolder = new File(APP_IMAGES_FOLDER_URL, fullPath);
            imagesFolder.mkdirs(); // not mkdir because we have complex path (logics/navigator)

            String iconFileName = iconPath.substring(0, iconPath.lastIndexOf("."));
            String iconFileType = iconPath.substring(iconPath.lastIndexOf(".") + 1);

            createImageFile(iconHolder.getImage().getImage(), imagesFolder, iconFileName, iconFileType, false);
            if (canBeDisabled) {
                createImageFile(iconHolder.getImage().getImage(), imagesFolder, iconFileName + "_Disabled", iconFileType, canBeDisabled);
            }
            return new ImageDescription("static/images/" + fullPath + "/" + iconPath, iconHolder.getImage().getIconWidth(), iconHolder.getImage().getIconHeight());
        }
        return null;
    }

    private static void createImageFile(Image image, File imagesFolder, String iconFileName, String iconFileType, boolean gray) {
        File imageFile = new File(imagesFolder, iconFileName + "." + iconFileType);
        if (!imageFile.exists()) {
            ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
            try {
                ImageIO.write((RenderedImage) (
                        image instanceof RenderedImage ? image : getBufferedImage(image, gray)),
                        iconFileType,
                        byteOS
                );

                FileOutputStream fos = new FileOutputStream(imageFile);
                fos.write(byteOS.toByteArray());
                fos.close();
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }
    }

    private static BufferedImage getBufferedImage(Image img, boolean gray) {
        BufferedImage bufferedImage;
        if (gray) {
            bufferedImage = new BufferedImage(img.getWidth(null),
                    img.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR_PRE);

            bufferedImage.createGraphics().drawImage(img, 0, 0, null);

            ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                    bufferedImage.getColorModel().getColorSpace(),  null);
            op.filter(bufferedImage, bufferedImage);

            Graphics g = bufferedImage.createGraphics();
            g.drawImage(bufferedImage, 0, 0, null);
            g.dispose();
        } else {
            bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null), Transparency.TRANSLUCENT);
            Graphics g = bufferedImage.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
        }
        return bufferedImage;
    }

    public static Object readFilesAndDelete(GFilesDTO filesObj) {
        File[] files = new File[filesObj.filePaths.size()];
        for (int i = 0; i < filesObj.filePaths.size(); i++) {
            files[i] = new File(APP_TEMP_FOLDER_URL, filesObj.filePaths.get(i));
        }
        try {
            Object bytes = BaseUtils.filesToBytes(filesObj.multiple, filesObj.storeName, filesObj.custom, files);
            for (File file : files) {
                file.delete();
            }
            return bytes;

        } catch (IOException e) {
            return null;
        }
    }

    public static String saveApplicationFile(RawFileData fileData) { // for login page, logo and icon images
        String fileName = BaseUtils.randomString(15);
        if(saveFile(fileName, fileData) != null)
            return fileName;
        return null;
    }

    public static String saveActionFile(RawFileData fileData) { // with single usage (action scoped), so will be deleted just right after downloaded
        String fileName = BaseUtils.randomString(15);
        if(saveFile(fileName, fileData) != null)
            return fileName;
        return null;
    }

    public static String saveFormFile(RawFileData fileData, FormSessionObject<?> sessionObject) { // multiple usages (form scoped), so should be deleted just right after form is closed
        String fileName = BaseUtils.randomString(15);
        File file = saveFile(fileName, fileData);
        if(file != null) {
            sessionObject.savedTempFiles.add(file);
            return fileName;
        }
        return null;
    }

    private static File saveFile(String fileName, RawFileData fileData) {
        try {
            if (fileData != null) {
                File file = new File(APP_TEMP_FOLDER_URL, fileName);
                fileData.write(file);
                return file;
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }
    
    public static void deleteFile(File file) {
        try { // maybe its better to do it with some delay, but now there's no request retry for action files (except maybe beep), and form should be already closed
            file.delete();
        } catch (Throwable t) { // this files are in temp dir anyway, so no big deal                
        }
    }

    public static Pair<String, String> exportReport(FormPrintType type, ReportGenerationData reportData) {
        try {
            RawFileData report = ReportGenerator.exportToFileByteArray(reportData, type);
            return new Pair<>(FileUtils.saveActionFile(report), type.getExtension());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
