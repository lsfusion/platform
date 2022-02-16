package lsfusion.gwt.server;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.SystemUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.SerializableImageIconHolder;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.gwt.client.base.ImageHolder;
import lsfusion.gwt.client.form.property.cell.classes.GFilesDTO;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.Iterator;

public class FileUtils {
    // not that pretty with statics, in theory it's better to autowire LogicsHandlerProvider (or get it from servlet) and pass as a parameter here
    public static String APP_IMAGES_FOLDER_URL; // all files has to be prefixed with logicsName
    public static String APP_CSS_FOLDER_URL; // all files has to be prefixed with logicsName
    public static String APP_CLIENT_IMAGES_FOLDER_URL;
    public static String APP_TEMP_FOLDER_URL; // all files hasn't to be prefixed because their names are (or prefixed with) random strings
    public static String APP_PATH;

    public static ImageHolder createImage(String logicsName, SerializableImageIconHolder imageHolder, String imagesFolderName, boolean canBeDisabled) {
        if (imageHolder != null) {
            ImageHolder imageDescription = new ImageHolder();
                
            for (GColorTheme gColorTheme : GColorTheme.values()) {
                ColorTheme colorTheme = ColorTheme.get(gColorTheme.getSid());

                String fullPath = logicsName + "/" + imagesFolderName;
                String imagePath = imageHolder.getImagePath(colorTheme);
                File imagesFolder = new File(APP_IMAGES_FOLDER_URL, fullPath);

                imagesFolder.mkdirs(); // not mkdir because we have complex path (logics/navigator)
                String imageFileName = imagePath.substring(0, imagePath.lastIndexOf("."));
                String imageFileType = imagePath.substring(imagePath.lastIndexOf(".") + 1);
                
                ImageIcon image = imageHolder.getImage(colorTheme); 
                if (image == null) {
                    image = ClientColorUtils.createFilteredImageIcon(imageHolder.getImage(ColorTheme.DEFAULT),
                            ServerColorUtils.getDefaultThemePanelBackground(),
                            ServerColorUtils.getPanelBackground(colorTheme),
                            ServerColorUtils.getComponentForeground(colorTheme));
                }
                
                createImageFile(image.getImage(), imagesFolder, imageFileName, imageFileType, false);
                if (canBeDisabled) {
                    createImageFile(image.getImage(), imagesFolder, imageFileName + "_Disabled", imageFileType, true);
                }
                
                imageDescription.addImage(gColorTheme, "static/images/" + fullPath + "/" + imagePath, image.getIconWidth(), image.getIconHeight());
            }
            return imageDescription;
        }
        return null;
    }

    private static void createImageFile(Image image, File imagesFolder, String imageFileName, String iconFileType, boolean gray) {
        File imageFile = new File(imagesFolder, imageFileName + "." + iconFileType);
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
    
    public static void createThemedClientImages() {
        File imagesFolder = new File(APP_CLIENT_IMAGES_FOLDER_URL);
        Iterator it = org.apache.commons.io.FileUtils.iterateFiles(imagesFolder, null, false);
        while (it.hasNext()) {
            File imageFile = (File) it.next();
                String imagePath = imageFile.getName();
                
                boolean alreadyThemed = false;
                if (imagePath.contains("_")) {
                    String fileName = imagePath.substring(0, imagePath.lastIndexOf("."));
                    String possibleThemeSid = fileName.substring(fileName.lastIndexOf("_") + 1);
                    for (GColorTheme theme : GColorTheme.values()) {
                        if (!theme.isDefault() && theme.getSid().equals(possibleThemeSid)) {
                            alreadyThemed = true;
                            break;
                        }
                    }
                }
                
                if (!alreadyThemed) {
                    byte[] bytesArray = new byte[(int) imageFile.length()];
                    try {
                        FileInputStream fileInputStream = new FileInputStream(imageFile);
                        fileInputStream.read(bytesArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (bytesArray.length > 0) {
                        ImageIcon baseImage = new ImageIcon(bytesArray);

                        for (GColorTheme gColorTheme : GColorTheme.values()) {
                            if (!gColorTheme.isDefault()) {
                                ColorTheme colorTheme = ColorTheme.get(gColorTheme.getSid());
    
                                ImageIcon themeImage = ClientColorUtils.createFilteredImageIcon(baseImage,
                                        ServerColorUtils.getDefaultThemePanelBackground(),
                                        ServerColorUtils.getPanelBackground(colorTheme),
                                        ServerColorUtils.getComponentForeground(colorTheme));
    
                                String newImagePath = gColorTheme.getImagePath(imagePath);
                                String imageFileName = newImagePath.substring(0, newImagePath.lastIndexOf("."));
                                String imageFileType = newImagePath.substring(newImagePath.lastIndexOf(".") + 1);
    
                                createImageFile(themeImage.getImage(), imagesFolder, imageFileName, imageFileType, false);
                            }
                        }
                    }
                }
        }
    }

    public static Object readFilesAndDelete(GFilesDTO filesObj) {
        File file = new File(APP_TEMP_FOLDER_URL, filesObj.filePath);
        try {
            return BaseUtils.filesToBytes(false, filesObj.storeName, filesObj.custom, filesObj.named, filesObj.fileName, file);
        } finally {
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    public static String saveApplicationFile(RawFileData fileData) { // for login page, logo and icon images
        String fileName = SystemUtils.generateID(fileData.getBytes());
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
        String fileName = SystemUtils.generateID(fileData.getBytes());
        if (!sessionObject.savedTempFiles.containsKey(fileName)) {
            File file = saveFile(fileName, fileData);
            if (file != null) {
                sessionObject.savedTempFiles.put(fileName, file);
            } else {
                return null;
            }
        }
        return fileName;
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

    public static Pair<String, String> exportReport(FormPrintType type, ReportGenerationData reportData, RemoteLogicsInterface remoteLogics) {
        try {
            RawFileData report = ReportGenerator.exportToFileByteArray(reportData, type, remoteLogics);
            return new Pair<>(FileUtils.saveActionFile(report), type.getExtension());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static Pair<String, String> exportFile(RawFileData file) {
        try {
            return new Pair<>(FileUtils.saveActionFile(file), "csv");
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
