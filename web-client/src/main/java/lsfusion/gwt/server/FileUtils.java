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
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

public class FileUtils {
    // not that pretty with statics, in theory it's better to autowire LogicsHandlerProvider (or get it from servlet) and pass as a parameter here
    public static String STATIC_CSS_RESOURCE_PATH = "static/css/"; // all files has to be prefixed with logicsName
    public static String STATIC_GWT_IMAGE_RESOURCE_PATH = "main/static/images"; // getStaticImageURL

    // static app files
    public static String STATIC_IMAGE_FOLDER_PATH = "gwtimages"; // getStaticImageURL
    public static String APP_STATIC_IMAGE_FOLDER_PATH = "appimages"; // getAppStaticImageURL, all files has to be prefixed with logicsName
    public static String APP_STATIC_WEB_FOLDER_PATH = "appweb"; // getAppStaticFileURL, all files has to be prefixed with logicsName

    public static String APP_DOWNLOAD_FOLDER_PATH; // getAppDownloadURL, all files hasn't to be prefixed because their names are (or prefixed with) random strings
    public static String APP_UPLOAD_FOLDER_PATH; // getAppUploadURL, all files hasn't to be prefixed because their names are (or prefixed with) random strings
    public static String APP_CONTEXT_FOLDER_PATH;

    // should correspond from in urlrewrite.xml urls
    public static String STATIC_PATH = "static";
    public static String TEMP_PATH = "temp";
    public static String DEV_PATH = "dev";
    // should correspond url-pattern for fileDownloadHandler (in web.xml)
    public static String DOWNLOAD_HANDLER = "downloadFile";

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

    private static String getStaticPath(String path, ServerSettings settings) {
        return path + "/" + settings.logicsName;
    }

    public static ImageHolder createImageFile(ServletContext servletContext, ServerSettings settings, SerializableImageIconHolder imageHolder, boolean canBeDisabled) {
        if (imageHolder != null) {
            ImageHolder imageDescription = new ImageHolder();
                
            for (GColorTheme gColorTheme : GColorTheme.values()) {
                ColorTheme colorTheme = ColorTheme.get(gColorTheme.getSid());

                String imagePath = imageHolder.getImagePath(colorTheme);
                String imageFileType = imagePath.substring(imagePath.lastIndexOf(".") + 1);
                
                ImageIcon image = imageHolder.getImage(colorTheme); 
                if (image == null) {
                    image = ClientColorUtils.createFilteredImageIcon(imageHolder.getImage(ColorTheme.DEFAULT),
                            ServerColorUtils.getDefaultThemePanelBackground(servletContext),
                            ServerColorUtils.getPanelBackground(servletContext, colorTheme),
                            ServerColorUtils.getComponentForeground(servletContext, colorTheme));
                }

                String imagesFolderPath = getStaticPath(APP_STATIC_IMAGE_FOLDER_PATH, settings);
                String imageUrl = saveImageFile(getIconSaver(image, imageFileType, false), imagesFolderPath, imagePath, null);
                if (canBeDisabled) {
                    String imageFileName = imagePath.substring(0, imagePath.lastIndexOf("."));
                    saveImageFile(getIconSaver(image, imageFileType, true), imagesFolderPath, imageFileName + "_Disabled." + imageFileType, null);
                }
                
                imageDescription.addImage(gColorTheme, imageUrl, image.getIconWidth(), image.getIconHeight());
            }
            return imageDescription;
        }
        return null;
    }

    public static String getWebFile(String fileName, ServerSettings settings) {
        // assert that it was saved before
        return saveWebFile(fileName, null, settings);
    }

    public static String saveWebFile(String fileName, RawFileData fileData, ServerSettings settings) {
        String webFolder = "/web/";
        assert fileName.startsWith(webFolder);
        fileName = fileName.substring(webFolder.length());

        return saveDownloadFile(false, settings.inDevMode ? DownloadType.DEV : DownloadType.STATIC, getStaticPath(APP_STATIC_WEB_FOLDER_PATH, settings), fileName, null, null, fileData != null ? fileData::write : null);
    }

    private static String saveImageFile(EConsumer<OutputStream, IOException> file, String imagesFolder, String imagePath, Result<String> rUrl) {
        return saveDownloadFile(false, DownloadType.STATIC, imagesFolder, imagePath, null, rUrl, file);
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
    
    public static String createThemedClientImages(ServletContext servletContext) throws IOException {
        Result<String> rUrl = new Result<>();
        String imageResourcePath = "/" + STATIC_GWT_IMAGE_RESOURCE_PATH + "/";
        Set<String> imagePaths = servletContext.getResourcePaths(imageResourcePath);
        for(String fullImagePath : imagePaths) {
            String imagePath = fullImagePath;

            assert imagePath.startsWith(imageResourcePath);
            imagePath = imagePath.substring(imageResourcePath.length());
            String imageType = BaseUtils.getFileExtension(imagePath);

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
                ImageIcon icon = null;
                for (GColorTheme gColorTheme : GColorTheme.values()) {
                    if (!gColorTheme.isDefault()) {
                        String themedImagePath = gColorTheme.getImagePath(imagePath);
                        if (!imagePaths.contains(imageResourcePath + themedImagePath)) {
                            if(icon == null)
                                icon = new ImageIcon(IOUtils.toByteArray(servletContext.getResource(fullImagePath)));
                            ColorTheme colorTheme = ColorTheme.get(gColorTheme.getSid());
                            saveImageFile(getIconSaver(ClientColorUtils.createFilteredImageIcon(icon,
                                    ServerColorUtils.getDefaultThemePanelBackground(servletContext),
                                    ServerColorUtils.getPanelBackground(servletContext, colorTheme),
                                    ServerColorUtils.getComponentForeground(servletContext, colorTheme)),
                                    imageType, false), themedImagePath, rUrl);
                        }
                    }
                }
            }

            // have no idea why but gif is not properly saved when using ImageIcon and ImageIO
            try(InputStream imageFile = servletContext.getResourceAsStream(fullImagePath)) {
                saveImageFile(fos -> IOUtils.copy(imageFile, fos), imagePath, rUrl);
            }
        }
        return rUrl.result;
    }

    public static EConsumer<OutputStream, IOException> getIconSaver(ImageIcon icon, String iconFileType, boolean gray) {
        Image image = icon.getImage();
        return fos -> ImageIO.write((RenderedImage) (image instanceof RenderedImage ? image : getBufferedImage(image, gray)), iconFileType, fos);
    }

    public static void saveImageFile(EConsumer<OutputStream, IOException> file, String imagePath, Result<String> rUrl) {
        saveImageFile(file, STATIC_IMAGE_FOLDER_PATH, imagePath, rUrl);
    }

    public static Object readUploadFileAndDelete(GFilesDTO filesObj) {
        Result<Object> result = new Result<>();
        readFile(APP_UPLOAD_FOLDER_PATH, filesObj.filePath, true, inStream ->
                result.set(BaseUtils.filesToBytes(false, filesObj.storeName, filesObj.custom, filesObj.named, new String[]{filesObj.fileName}, new String[]{filesObj.filePath}, new InputStream[]{inStream})));
        return result.result;
    }

    private enum DownloadType {
        STATIC, // need to be cached for a long time and stored
        TEMP, // need to be cached for a long time and deleted after usage
        DEV // need not to be cached but stored
    }

    private static String saveDownloadFile(boolean useDownload, DownloadType type, String innerPath, String fileName, Result<Runnable> rCloser, Result<String> rUrl, EConsumer<OutputStream, IOException> consumer) {
        String filePath;
        String url;
        String extraPath = null;
        switch (type) {
            case STATIC:
                extraPath = STATIC_PATH;
                break;
            case TEMP:
                extraPath = TEMP_PATH;
                break;
            case DEV:
                extraPath = DEV_PATH;
                break;
        }
        if(useDownload) { // we'll put it in the temp folder, and add static to final url
            filePath = APP_DOWNLOAD_FOLDER_PATH;
            url = DOWNLOAD_HANDLER + "/";
        } else {
            filePath = APP_CONTEXT_FOLDER_PATH;
            url = "";

            filePath += "/" + extraPath;
        }
        url += extraPath + "/";

        if(!innerPath.isEmpty()) {
            filePath += "/" + innerPath;
            url += innerPath + "/";
        }

        Runnable closer = writeFile(filePath, fileName, consumer);
        if(rCloser != null)
            rCloser.set(closer);

        if(rUrl != null)
            rUrl.set(url);

        return url + fileName;
    }

    public static String saveApplicationFile(RawFileData fileData) { // for login page, logo and icon images
        String fileName = SystemUtils.generateID(fileData.getBytes());
        return saveDataFile(fileName, true, null, fileData);
    }

    public static String saveActionFile(RawFileData fileData) { // with single usage (action scoped), so will be deleted just right after downloaded
        if(fileData != null) {
            String fileName = BaseUtils.randomString(15);            ;
            return saveDataFile(fileName, false, null, fileData);
        }
        return null;
    }

    public static String saveFormFile(RawFileData fileData, FormSessionObject<?> sessionObject) { // multiple usages (form scoped), so should be deleted just right after form is closed
        String fileName = SystemUtils.generateID(fileData.getBytes());
        Pair<String, Runnable> savedFile = sessionObject.savedTempFiles.get(fileName);
        if (savedFile == null) {
            Result<Runnable> rCloser = new Result<>();
            savedFile = new Pair<>(saveDataFile(fileName, true, rCloser, fileData), rCloser.result);
            sessionObject.savedTempFiles.put(fileName, savedFile);
        }
        return savedFile.first;
    }

    private static String saveDataFile(String fileName, boolean isStatic, Result<Runnable> rCloser, RawFileData fileData) {
        return saveDownloadFile(true, isStatic ? DownloadType.STATIC : DownloadType.TEMP, "", fileName, rCloser, null, fileData::write); // assert !exists
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
