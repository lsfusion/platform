package lsfusion.gwt.server;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.SystemUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.SerializableImageIconHolder;
import lsfusion.base.lambda.EConsumer;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.gwt.client.base.ImageHolder;
import lsfusion.gwt.client.form.property.cell.classes.GFilesDTO;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import lsfusion.interop.logics.ServerSettings;
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
import java.util.function.Consumer;

public class FileUtils {
    // not that pretty with statics, in theory it's better to autowire LogicsHandlerProvider (or get it from servlet) and pass as a parameter here
    public static String APP_CSS_FOLDER_PATH = "static/css/"; // all files has to be prefixed with logicsName
    // static files (platform / app)
    public static String STATIC_IMAGE_FOLDER_PATH = "main/static/images"; // getStaticImageURL
    public static String APP_STATIC_IMAGE_FOLDER_PATH = "static/images"; // getAppStaticImageURL, all files has to be prefixed with logicsName
    public static String APP_STATIC_FILE_FOLDER_PATH(boolean inDevMode) { // getAppStaticFileURL
        return inDevMode ? "dev/web" : "static/web";
    }
    public static String APP_DOWNLOAD_FOLDER_PATH = "WEB-INF/temp"; // getAppDownloadURL, all files hasn't to be prefixed because their names are (or prefixed with) random strings
    public static String APP_UPLOAD_FOLDER_PATH = "WEB-INF/temp"; // getAppUploadURL, all files hasn't to be prefixed because their names are (or prefixed with) random strings

    public static String APP_PATH;

    public static File createFile(String relativePath, String fileName) {
        return new File(APP_PATH + "/" + relativePath, fileName);
    }
    public static void readFile(String relativePath, String fileName, boolean close, EConsumer<InputStream, IOException> consumer) {
        File file = createFile(relativePath, fileName);

        try(FileInputStream inStream = new FileInputStream(file)) {
            consumer.accept(inStream);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        if(close)
            deleteFile(file);
    }
    public static Runnable writeFile(String relativePath, String fileName, EConsumer<OutputStream, IOException> consumer) {
        File file = createFile(relativePath, fileName);
        if(!file.exists()) {
            try(FileOutputStream outStream = org.apache.commons.io.FileUtils.openOutputStream(file)) {
                consumer.accept(outStream);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        return () -> deleteFile(file);
    }
    private static void deleteFile(File file) {
        try { // maybe its better to do it with some delay, but now there's no request retry for action files (except maybe beep), and form should be already closed
            if (!file.delete())
                file.deleteOnExit();
        } catch (Throwable t) { // this files are in temp dir anyway, so no big deal
        }
    }

    public static File createDir(String path) {
        return new File(APP_PATH + "/" + path);
    }

    private static String getStaticPath(String path, ServerSettings settings, String subFolder) {
        return path + "/" + settings.logicsName + (subFolder.isEmpty() ? "" : "/" + subFolder);
    }

    public static ImageHolder createImage(ServerSettings settings, SerializableImageIconHolder imageHolder, String imagesFolderName, boolean canBeDisabled) {
        if (imageHolder != null) {
            ImageHolder imageDescription = new ImageHolder();
                
            for (GColorTheme gColorTheme : GColorTheme.values()) {
                ColorTheme colorTheme = ColorTheme.get(gColorTheme.getSid());

                String imagePath = imageHolder.getImagePath(colorTheme);
                String imageFileName = imagePath.substring(0, imagePath.lastIndexOf("."));
                String imageFileType = imagePath.substring(imagePath.lastIndexOf(".") + 1);
                
                ImageIcon image = imageHolder.getImage(colorTheme); 
                if (image == null) {
                    image = ClientColorUtils.createFilteredImageIcon(imageHolder.getImage(ColorTheme.DEFAULT),
                            ServerColorUtils.getDefaultThemePanelBackground(),
                            ServerColorUtils.getPanelBackground(colorTheme),
                            ServerColorUtils.getComponentForeground(colorTheme));
                }

                String imagesFolderPath = getStaticPath(APP_STATIC_IMAGE_FOLDER_PATH, settings, imagesFolderName);
                createImageFile(image.getImage(), imagesFolderPath, imageFileName, imageFileType, false);
                if (canBeDisabled) {
                    createImageFile(image.getImage(), imagesFolderPath, imageFileName + "_Disabled", imageFileType, true);
                }
                
                imageDescription.addImage(gColorTheme, imagesFolderPath + "/" + imagePath, image.getIconWidth(), image.getIconHeight());
            }
            return imageDescription;
        }
        return null;
    }

    public static String getWeb(String fileName, ServerSettings settings) {
        return getStaticPath(APP_STATIC_FILE_FOLDER_PATH(settings.inDevMode), settings, "") + "/" + fileName;
    }

    public static void saveWeb(String fileName, RawFileData fileData, ServerSettings settings) {
        String externalResourcesAbsolutePath = getStaticPath(APP_STATIC_FILE_FOLDER_PATH(settings.inDevMode), settings, ""))
        writeFile(externalResourcesAbsolutePath, fileName, fileData::write);
    }

    private static void createImageFile(Image image, String imagesFolder, String imageFileName, String iconFileType, boolean gray) {
        writeFile(imagesFolder, imageFileName + "." + iconFileType, fos -> {
            ImageIO.write((RenderedImage) (
                    image instanceof RenderedImage ? image : getBufferedImage(image, gray)),
                    iconFileType,
                    fos
            );
        });
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
        File imagesFolder = createDir(STATIC_IMAGE_FOLDER_PATH);
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
    
                                createImageFile(themeImage.getImage(), STATIC_IMAGE_FOLDER_PATH, imageFileName, imageFileType, false);
                            }
                        }
                    }
                }
        }
    }

    public static Object readUploadFileAndDelete(GFilesDTO filesObj) {
        Result<Object> result = new Result<>();
        readFile(APP_UPLOAD_FOLDER_PATH, filesObj.filePath, true, inStream -> {
            result.set(BaseUtils.filesToBytes(false, filesObj.storeName, filesObj.custom, filesObj.named, new String[]{filesObj.fileName}, new String[]{filesObj.filePath}, new InputStream[]{inStream}));
        });
        return result.result;
    }

    public static String saveApplicationFile(RawFileData fileData) { // for login page, logo and icon images
        String fileName = SystemUtils.generateID(fileData.getBytes());
        saveDownloadFile(fileName, fileData);
        return "downloadFile/static/" + fileName;
    }

    public static String saveActionFile(RawFileData fileData) { // with single usage (action scoped), so will be deleted just right after downloaded
        if(fileData != null) {
            String fileName = BaseUtils.randomString(15);
            saveDownloadFile(fileName, fileData);
            return "downloadFile/" + fileName;
        }
        return null;
    }

    public static String saveFormFile(RawFileData fileData, FormSessionObject<?> sessionObject) { // multiple usages (form scoped), so should be deleted just right after form is closed
        String fileName = SystemUtils.generateID(fileData.getBytes());
        if (!sessionObject.savedTempFiles.containsKey(fileName)) {
            Runnable closer = saveDownloadFile(fileName, fileData);
            sessionObject.savedTempFiles.put(fileName, closer);
        }
        return "downloadFile/static/" + fileName;
    }

    private static Runnable saveDownloadFile(String fileName, RawFileData fileData) {
        return writeFile(APP_DOWNLOAD_FOLDER_PATH, fileName, fileData::write); // assert !exists
    }

    public static Pair<String, String> exportReport(FormPrintType type, RawFileData report) {
        try {
            return new Pair<>(FileUtils.saveActionFile(report), type.getExtension());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static Pair<String, String> exportReport(FormPrintType type, ReportGenerationData reportData, RemoteLogicsInterface remoteLogics) {
        return exportReport(type, ReportGenerator.exportToFileByteArray(reportData, type, remoteLogics));
    }

    public static Pair<String, String> exportFile(RawFileData file) {
        try {
            return new Pair<>(FileUtils.saveActionFile(file), "csv");
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
