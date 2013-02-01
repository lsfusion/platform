package platform.gwt.form.server.convert;

import com.google.common.base.Throwables;
import platform.base.BaseUtils;
import platform.gwt.form.shared.view.ImageDescription;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageHandler {
    public static String APP_FOLDER_URL;
    public static String APP_IMAGES_FOLDER_URL;
    public static String APP_TEMP_FOLDER_URL;

    public static void initializeAppFolder(String path) {
        if (APP_FOLDER_URL == null) {
            APP_FOLDER_URL = path;
            APP_IMAGES_FOLDER_URL = path + "/images/";
            APP_TEMP_FOLDER_URL = path + "/WEB-INF/" + "temp";
        }
    }

    public static ImageDescription createImage(ImageIcon icon, String iconPath, String imagesFolderName, boolean canBeDisabled) {
        if (APP_IMAGES_FOLDER_URL != null && icon != null) {
            File imagesFolder = new File(APP_IMAGES_FOLDER_URL, imagesFolderName);
            imagesFolder.mkdir();

            String iconFileName = iconPath.substring(0, iconPath.lastIndexOf("."));
            String iconFileType = iconPath.substring(iconPath.lastIndexOf(".") + 1);

            createImageFile(icon.getImage(), imagesFolder, iconFileName, iconFileType, false);
            if (canBeDisabled) {
                createImageFile(icon.getImage(), imagesFolder, iconFileName + "_Disabled", iconFileType, canBeDisabled);
            }
            return new ImageDescription("images/" + imagesFolderName + "/" + iconPath, icon.getIconWidth(), icon.getIconHeight());
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
}
