package lsfusion.gwt.server;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.SystemUtils;
import lsfusion.base.col.MapFact;
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
import javax.servlet.ServletContext;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.Iterator;
import java.util.Map;

public class FileUtils {
    // not that pretty with statics, in theory it's better to autowire LogicsHandlerProvider (or get it from servlet) and pass as a parameter here
    public static String APP_CSS_FOLDER_PATH = "static/css/"; // all files has to be prefixed with logicsName
    // static files (platform / app)
    public static String STATIC_IMAGE_FOLDER_PATH = "main/static/images"; // getStaticImageURL
    public static String APP_STATIC_IMAGE_FOLDER_PATH = "images"; // getAppStaticImageURL, all files has to be prefixed with logicsName
    public static String APP_STATIC_FILE_FOLDER_PATH = "web"; // getAppStaticFileURL

    public static String APP_DOWNLOAD_FOLDER_PATH; // getAppDownloadURL, all files hasn't to be prefixed because their names are (or prefixed with) random strings
    public static String APP_UPLOAD_FOLDER_PATH; // getAppUploadURL, all files hasn't to be prefixed because their names are (or prefixed with) random strings
    public static String APP_CONTEXT_FOLDER_PATH;

    public static File createFile(String relativePath, String fileName) {
        return new File(relativePath, fileName);
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

    private static String getStaticPath(String path, ServerSettings settings, String subFolder) {
        return path + "/" + settings.logicsName + (subFolder.isEmpty() ? "" : "/" + subFolder);
    }

    public static ImageHolder createImageFile(ServletContext servletContext, ServerSettings settings, SerializableImageIconHolder imageHolder, String imagesFolderName, boolean canBeDisabled) {
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
                            ServerColorUtils.getDefaultThemePanelBackground(servletContext),
                            ServerColorUtils.getPanelBackground(servletContext, colorTheme),
                            ServerColorUtils.getComponentForeground(servletContext, colorTheme));
                }

                String imagesFolderPath = getStaticPath(APP_STATIC_IMAGE_FOLDER_PATH, settings, imagesFolderName);
                String imageUrl = saveImageFile(image.getImage(), imagesFolderPath, imageFileName, imageFileType, false);
                if (canBeDisabled) {
                    saveImageFile(image.getImage(), imagesFolderPath, imageFileName + "_Disabled", imageFileType, true);
                }
                
                imageDescription.addImage(gColorTheme, imageUrl, image.getIconWidth(), image.getIconHeight());
            }
            return imageDescription;
        }
        return null;
    }

    private static final Map<String, String> webFiles = MapFact.getGlobalConcurrentHashMap();///

    public static String getWebFile(String fileName, ServerSettings settings) {
        return webFiles.get(fileName);
    }

    public static void saveWebFile(String fileName, RawFileData fileData, ServerSettings settings) {
        webFiles.put(fileName, saveFile(false, settings.inDevMode, getStaticPath(APP_STATIC_FILE_FOLDER_PATH, settings, ""), fileName, null, fileData::write));
    }

    private static String saveImageFile(Image image, String imagesFolder, String imageFileName, String iconFileType, boolean gray) {
        return saveFile(false, true, imagesFolder, imageFileName + "." + iconFileType, null, fos -> {
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
    
    public static void createThemedClientImages(ServletContext servletContext) {
        File imagesFolder = new File(APP_CONTEXT_FOLDER_PATH + "/" + STATIC_IMAGE_FOLDER_PATH);
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
                                        ServerColorUtils.getDefaultThemePanelBackground(servletContext),
                                        ServerColorUtils.getPanelBackground(servletContext, colorTheme),
                                        ServerColorUtils.getComponentForeground(servletContext, colorTheme));
    
                                String newImagePath = gColorTheme.getImagePath(imagePath);
                                String imageFileName = newImagePath.substring(0, newImagePath.lastIndexOf("."));
                                String imageFileType = newImagePath.substring(newImagePath.lastIndexOf(".") + 1);

                                saa
                                saveImageFile(themeImage.getImage(), STATIC_IMAGE_FOLDER_PATH, imageFileName, imageFileType, false);
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

    private static String saveFile(boolean useDownload, boolean isStatic, String innerPath, String fileName, Result<Runnable> rCloser, EConsumer<OutputStream, IOException> consumer) {
        String relativePath;
        String url;
        if(useDownload) { // we'll put it in the temp folder, and add static to final url
            relativePath = APP_DOWNLOAD_FOLDER_PATH;
            url = "downloadFile/";
        } else {
            relativePath = APP_CONTEXT_FOLDER_PATH;
            url = "";

            if(isStatic)
                relativePath += "static/";
        }

        Runnable closer = writeFile(relativePath, fileName, consumer);
        if(rCloser != null)
            rCloser.set(closer);

        if(isStatic)
            url += "static/";

        return url + fileName;
    }

    public static String saveApplicationFile(RawFileData fileData) { // for login page, logo and icon images
        String fileName = SystemUtils.generateID(fileData.getBytes());
        return saveDownloadFile(fileName, true, null, fileData);
    }

    public static String saveActionFile(RawFileData fileData) { // with single usage (action scoped), so will be deleted just right after downloaded
        if(fileData != null) {
            String fileName = BaseUtils.randomString(15);            ;
            return saveDownloadFile(fileName, false, null, fileData);
        }
        return null;
    }

    public static String saveFormFile(RawFileData fileData, FormSessionObject<?> sessionObject) { // multiple usages (form scoped), so should be deleted just right after form is closed
        String fileName = SystemUtils.generateID(fileData.getBytes());
        Pair<String, Runnable> savedFile = sessionObject.savedTempFiles.get(fileName);
        if (savedFile == null) {
            Result<Runnable> rCloser = new Result<>();
            savedFile = new Pair<>(saveDownloadFile(fileName, true, rCloser, fileData), rCloser.result)
            sessionObject.savedTempFiles.put(fileName, savedFile);
        }
        return savedFile.first;
    }

    private static String saveDownloadFile(String fileName, boolean isStatic, Result<Runnable> rCloser, RawFileData fileData) {
        return saveFile(true, isStatic, "", fileName, rCloser, fileData::write); // assert !exists
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
