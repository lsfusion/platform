package lsfusion.gwt.server;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.RawFileData;
import lsfusion.gwt.shared.view.ImageDescription;
import lsfusion.gwt.shared.view.changes.dto.GFilesDTO;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.SerializableImageIconHolder;
import lsfusion.interop.form.ReportGenerationData;

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

    public static ImageDescription createImage(SerializableImageIconHolder iconHolder, String iconPath, String imagesFolderName, boolean canBeDisabled) {
        if (iconHolder != null) {
            File imagesFolder = new File(APP_IMAGES_FOLDER_URL, imagesFolderName);
            imagesFolder.mkdirs(); // not mkdir because we have complex path (logics/navigator)

            String iconFileName = iconPath.substring(0, iconPath.lastIndexOf("."));
            String iconFileType = iconPath.substring(iconPath.lastIndexOf(".") + 1);

            createImageFile(iconHolder.getImage().getImage(), imagesFolder, iconFileName, iconFileType, false);
            if (canBeDisabled) {
                createImageFile(iconHolder.getImage().getImage(), imagesFolder, iconFileName + "_Disabled", iconFileType, canBeDisabled);
            }
            return new ImageDescription("images/" + imagesFolderName + "/" + iconPath, iconHolder.getImage().getIconWidth(), iconHolder.getImage().getIconHeight());
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

    public static String saveFile(RawFileData fileData) {
        return saveFile(BaseUtils.randomString(15), fileData);
    }

    @Deprecated
    public static String saveFile(String fileName, RawFileData fileData) {
        try {
            if (fileData != null) {
                File file = new File(APP_TEMP_FOLDER_URL, fileName);
                fileData.write(file);
                return fileName;
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public static Pair<String, String> exportReport(FormPrintType type, ReportGenerationData reportData) {
        try {
            RawFileData report = ReportGenerator.exportToFileByteArray(reportData, type);;
            return new Pair<>(FileUtils.saveFile(report), type.getExtension());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
