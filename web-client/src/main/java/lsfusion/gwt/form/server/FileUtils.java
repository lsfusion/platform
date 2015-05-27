package lsfusion.gwt.form.server;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.form.shared.view.ImageDescription;
import lsfusion.gwt.form.shared.view.changes.dto.GFilesDTO;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.SerializableImageIconHolder;
import lsfusion.interop.form.ReportGenerationData;
import net.sf.jasperreports.engine.JasperExportManager;

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
    public static String APP_FOLDER_URL;
    public static String APP_IMAGES_FOLDER_URL;
    public static String APP_TEMP_FOLDER_URL;

    public static ImageDescription createImage(SerializableImageIconHolder iconHolder, String iconPath, String imagesFolderName, boolean canBeDisabled) {
        if (iconHolder != null) {
            File imagesFolder = new File(APP_IMAGES_FOLDER_URL, imagesFolderName);
            imagesFolder.mkdir();

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

    public static String createPropertyImage(byte[] imageBytes, String imageFilePrefix) {
        if (imageBytes != null) {
            String newFileName = imageFilePrefix + "_" + BaseUtils.randomString(15);
            File imageFile = new File(APP_TEMP_FOLDER_URL, newFileName);
            try {
                FileOutputStream fos = new FileOutputStream(imageFile);
                fos.write(imageBytes);
                fos.close();
            } catch (Exception e) {
                Throwables.propagate(e);
            }

            return newFileName ;
        }
        return null;
    }

    public static byte[] readFilesAndDelete(GFilesDTO filesObj) {
        File[] files = new File[filesObj.filePaths.size()];
        for (int i = 0; i < filesObj.filePaths.size(); i++) {
            files[i] = new File(APP_TEMP_FOLDER_URL, filesObj.filePaths.get(i));
        }
        try {
            byte[] bytes = BaseUtils.filesToBytes(filesObj.multiple, filesObj.storeName, filesObj.custom, files);
            for (File file : files) {
                file.delete();
            }
            return bytes;

        } catch (IOException e) {
            return null;
        }
    }

    public static String saveFile(byte[] fileBytes, String extension) {
        return saveFile(BaseUtils.randomString(15) + "." + extension, fileBytes);
    }

    public static String saveFile(String fileName, byte[] fileBytes) {
        try {
            if (fileBytes != null) {
                File file = new File(APP_TEMP_FOLDER_URL, fileName);
                FileOutputStream f = new FileOutputStream(file);
                f.write(fileBytes);
                f.close();
                return fileName;
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public static String exportReport(FormPrintType type, ReportGenerationData reportData) {
        try {
            ReportGenerator generator = new ReportGenerator(reportData);
            byte[] report;
            if (type != null && type.isExcel()) {
                report = ReportGenerator.exportToExcelByteArray(reportData, type);
            } else {
                report = JasperExportManager.exportReportToPdf(generator.createReport(false, null));
            }
            String fileName = "lsfReport" + BaseUtils.randomString(15) + "." + (type != null ? type.getFileExtension() : "pdf");
            File file = new File(APP_TEMP_FOLDER_URL, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(report);

            fos.close();
            return fileName;
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return null;
    }
}
