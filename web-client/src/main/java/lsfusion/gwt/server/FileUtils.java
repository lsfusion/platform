package lsfusion.gwt.server;

import com.google.common.base.Throwables;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.nio.AnimatedGif;
import com.sksamuel.scrimage.nio.AnimatedGifReader;
import com.sksamuel.scrimage.nio.ImageSource;
import com.sksamuel.scrimage.nio.StreamingGifWriter;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.AppImage;
import lsfusion.base.lambda.EConsumer;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.gwt.client.base.AppStaticImage;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.form.property.cell.classes.GFilesDTO;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.logics.ServerSettings;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class FileUtils {
    // not that pretty with statics, in theory it's better to autowire LogicsHandlerProvider (or get it from servlet) and pass as a parameter here
    public static String STATIC_CSS_RESOURCE_PATH = "static/css/"; // all files has to be prefixed with logicsName
    public static String THEME_CSS_FOLDER_PATH = STATIC_CSS_RESOURCE_PATH + "theme/"; // all files has to be prefixed with logicsName
    public static String STATIC_GWT_IMAGE_RESOURCE_PATH = "main/static/images"; // getStaticImageURL

    // static app files
    public static String STATIC_IMAGE_SUBFOLDER_SUBPATH = "gwtimages"; // getStaticImageURL
    public static String APP_STATIC_FOLDER_SUBPATH = "app"; // getAppStaticImageURL, all files has to be prefixed with logicsName

    public static String NOAUTH_FOLDER_PREFIX = "noauth"; // in web.xml security should be dropped for such pathes (static/noauth, download/static/noauth)

    public static String APP_DOWNLOAD_FOLDER_PATH; // getAppDownloadURL, all files hasn't to be prefixed because their names are (or prefixed with) random strings
    public static String APP_UPLOAD_FOLDER_PATH; // getAppUploadURL, all files hasn't to be prefixed because their names are (or prefixed with) random strings
    public static String APP_CONTEXT_FOLDER_PATH;

    // should correspond from in urlrewrite.xml urls
    public static String STATIC_PATH = "static";
    public static String TEMP_PATH = "temp";
    public static String DEV_PATH = "dev";
    // should correspond url-pattern for fileDownloadHandler, application context handler (in web.xml)
    public static String DOWNLOAD_HANDLER = "file";

    private static final ScheduledExecutorService closeExecutor = Executors.newScheduledThreadPool(5);
    private static void scheduleCloseFile(File file, long delay) {
        closeExecutor.schedule(() -> {
            try {
                deleteFile(file);
            } catch (Throwable e) { // we need to suppress to not stop scheduler
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public static void readFile(String relativePath, String fileName, boolean close, boolean scheduleClose, EConsumer<InputStream, IOException> consumer) {
        File file = createFile(relativePath, fileName);

        try(FileInputStream inStream = new FileInputStream(file)) {
            consumer.accept(inStream);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        if(close) {
            if(scheduleClose)
                scheduleCloseFile(file, 5000);
            else
                deleteFile(file);
        }
    }
    public static Runnable writeFile(String relativePath, boolean override, String fileName, EConsumer<OutputStream, IOException> consumer) {
        File file = createFile(relativePath, fileName);

        if(override || !file.exists()) {
            try(FileOutputStream outStream = org.apache.commons.io.FileUtils.openOutputStream(file)) {
                consumer.accept(outStream);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        return () -> deleteFile(file);
    }
    public static File createFile(String relativePath, String fileName) {
        return new File(relativePath, fileName);
    }
    private static void deleteFile(File file) {
        try { // maybe its better to do it with some delay, but now there's no request retry for action files (except maybe beep), and form should be already closed
            GLoggers.invocationLogger.info("deleting file: " + file.getName());
            if (!file.delete()) {
                GLoggers.invocationLogger.error("file not deleted: " + file.getName());
                file.deleteOnExit();
            }
        } catch (Throwable t) { // this files are in temp dir anyway, so no big deal
            GLoggers.invocationLogger.error("file delete failed: " + file.getName());
        }
    }

    private static String getStaticPath(ServerSettings settings) {
        return APP_STATIC_FOLDER_SUBPATH + "/" + settings.logicsName;
    }

    public static AppStaticImage createImageFile(ServletContext servletContext, ServerSettings settings, AppImage imageHolder, boolean canBeDisabled) throws IOException {
        if (imageHolder != null) {
            HashMap<GColorTheme, ImageDescription> images = null;
            RawFileData defaultImageFile = imageHolder.getImage(ColorTheme.DEFAULT);
            if(defaultImageFile != null) {
                images = new HashMap<>();
                String defaultImagePath = imageHolder.getImagePath();
                String extension = BaseUtils.getFileExtension(defaultImagePath);
                for (GColorTheme gColorTheme : GColorTheme.values()) {
                    ColorTheme colorTheme = ColorTheme.get(gColorTheme.getSid());
                    RawFileData imageFile = imageHolder.getImage(colorTheme);

                    RawFileData rawFile;
                    ImageIcon defaultImageIcon;
                    Supplier<ImageIcon> unanimatedImageIcon;
                    String ID;
                    if (imageFile == null) {
                        rawFile = defaultImageFile;
                        defaultImageIcon = defaultImageFile.getImageIcon();
                        ID = colorTheme.getID(defaultImageFile.getID());
                        unanimatedImageIcon = () -> ClientColorUtils.createFilteredImageIcon(defaultImageIcon, ServerColorUtils.getDefaultThemePanelBackground(servletContext), ServerColorUtils.getPanelBackground(servletContext, colorTheme), ServerColorUtils.getComponentForeground(servletContext, colorTheme));
                    } else {
                        rawFile = imageFile;
                        defaultImageIcon = imageFile.getImageIcon();
                        unanimatedImageIcon = () -> defaultImageIcon;
                        ID = imageFile.getID();
                    }

                    int width;
                    int height;
                    boolean nonThemed = AppImage.isNonThemed(extension);
                    if(nonThemed) {
                        width = 16;
                        height = 16;
                    } else {
                        width = defaultImageIcon.getIconWidth();
                        height = defaultImageIcon.getIconHeight();
                        if(!(width > 0 && height > 0)) // ignore broken images
                            nonThemed = true;
                    }

                    String disabledImageUrl = null;
                    EConsumer<OutputStream, IOException> iconSaver;
                    String imagePath = colorTheme.getImagePath(defaultImagePath);
                    if (nonThemed)
                        iconSaver = fos -> IOUtils.copy(rawFile.getInputStream(), fos);
                    else if (AppImage.isGif(extension))
                        iconSaver = getGifIconSaver(rawFile.getBytes(), imageFile == null, servletContext, colorTheme);
                    else {
                        iconSaver = getIconSaver(unanimatedImageIcon, BaseUtils.getFileExtension(imagePath), false);
                        if(canBeDisabled)
                            disabledImageUrl = saveImageFile(imagePath, getIconSaver(unanimatedImageIcon, BaseUtils.getFileExtension(imagePath), true), ID, settings, true);
                    }
                    String imageUrl = saveImageFile(imagePath, iconSaver, ID, settings, false);
                    if(canBeDisabled && disabledImageUrl == null)
                        disabledImageUrl = imageUrl;

                    images.put(gColorTheme, new ImageDescription(imageUrl, disabledImageUrl, width, height));
                }
            }
            return new AppStaticImage(imageHolder.fontClasses, images);
        }
        return null;
    }

    public static String saveImageFile(String imagePath, EConsumer<OutputStream, IOException> iconSaver, String ID, ServerSettings settings, boolean disabled) {
        if(disabled) {
            String disabledSuffix = "_Disabled";
            ID = ColorTheme.addIDSuffix(ID, disabledSuffix);
            imagePath = BaseUtils.addSuffix(imagePath, disabledSuffix);
        }
        return saveImageFile(iconSaver, ID, false, getStaticPath(settings), imagePath, null);
    }

    private final static boolean useDownloadForAppResources = true;
    // static image (app) with the fullpath (with the extension inside)
    public static String saveWebFile(String fullPath, RawFileData fileData, ServerSettings settings, boolean noAuth) {
        // we don't know if it was the first start, so we don't want to override, since it is called too often (however this may lead to the "incorrect server cache")
        // UPD: server settings is cached, so we want to always override file (which can change after app server update)
        return saveDownloadFile(useDownloadForAppResources, settings.inDevMode ? DownloadStoreType.DEV : DownloadStoreType.STATIC,
                (noAuth ? NOAUTH_FOLDER_PREFIX + "/" : "") + getStaticPath(settings), fullPath, null, fileData, fileData.getID());
    }

    // static image (app or web-client) with the fullpath (with the extension inside)
    private static String saveImageFile(EConsumer<OutputStream, IOException> file, String ID, boolean override, String imagesFolder, String fullPath, Result<String> rUrl) {
        return saveDownloadFile(useDownloadForAppResources, override, DownloadStoreType.STATIC, imagesFolder, fullPath, null, rUrl, file, ID);
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
                String fileName = BaseUtils.getFileName(imagePath);
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
                            ColorTheme colorTheme = ColorTheme.get(gColorTheme.getSid());
                            byte[] imageData = IOUtils.toByteArray(servletContext.getResource(fullImagePath));
                            
                            if ("gif".equalsIgnoreCase(imageType)) {
                                saveImageFile(getGifIconSaver(imageData, true, servletContext, colorTheme), themedImagePath, rUrl);
                            } else {
                                if(icon == null) {
                                    icon = new ImageIcon(imageData);
                                }
                                ImageIcon filteredImageIcon = ClientColorUtils.createFilteredImageIcon(icon,
                                        ServerColorUtils.getDefaultThemePanelBackground(servletContext),
                                        ServerColorUtils.getPanelBackground(servletContext, colorTheme),
                                        ServerColorUtils.getComponentForeground(servletContext, colorTheme));
                                saveImageFile(getIconSaver(() -> filteredImageIcon, imageType, false), themedImagePath, rUrl);
                            }
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

    public static EConsumer<OutputStream, IOException> getIconSaver(Supplier<ImageIcon> icon, String iconFileType, boolean gray) {
        return fos -> {
            Image image = icon.get().getImage();
            ImageIO.write((RenderedImage) (image instanceof RenderedImage ? image : getBufferedImage(image, gray)), iconFileType, fos);
        };
    }

    private static EConsumer<OutputStream, IOException> getGifIconSaver(byte[] imageBytes, boolean filter, ServletContext servletContext, ColorTheme colorTheme) {
        return outputStream -> {
            AnimatedGif gif = AnimatedGifReader.read(ImageSource.of(imageBytes));
            StreamingGifWriter writer = new StreamingGifWriter(gif.getDelay(0), gif.getLoopCount() == 0, false);
            StreamingGifWriter.GifStream gifStream = writer.prepareStream(outputStream, gif.getFrame(0).getType());
            for (int i = 0; i < gif.getFrameCount(); i++) {
                ImmutableImage frame = gif.getFrame(i);
                if (filter && !colorTheme.isDefault()) {
                    frame = frame.filter(image -> {
                        for (int x = 0; x < image.width; x++) {
                            for (int y = 0; y < image.height; y++) {
                                image.awt().setRGB(x, y, ClientColorUtils.filterColor(image.color(x, y).toARGBInt(),
                                        ServerColorUtils.getDefaultThemePanelBackground(servletContext),
                                        ServerColorUtils.getPanelBackground(servletContext, colorTheme),
                                        ServerColorUtils.getComponentForeground(servletContext, colorTheme)));
                            }
                        }
                    });
                }
                gifStream.writeFrame(frame, gif.getDisposeMethod(i));
            }
            try {
                gifStream.close();
            } catch (Exception ignored) {}
        };
    }

    public static void saveImageFile(EConsumer<OutputStream, IOException> file, String imagePath, Result<String> rUrl) {
        // we can override since it is called only once the container is started
        saveImageFile(file, null, true, STATIC_IMAGE_SUBFOLDER_SUBPATH, imagePath, rUrl);
    }

    public static Object readUploadFileAndDelete(GFilesDTO filesObj) {
        Result<Object> result = new Result<>();
        readFile(APP_UPLOAD_FOLDER_PATH, filesObj.filePath, true, false, inStream ->
                result.set(BaseUtils.filesToBytes(false, filesObj.storeName, filesObj.custom, filesObj.named, new String[]{filesObj.fileName}, new String[]{filesObj.filePath}, new InputStream[]{inStream})));
        return result.result;
    }

    private enum DownloadStoreType {
        STATIC, // need to be cached for a long time and stored
        TEMP, // need to be cached for a long time and deleted after usage
        DEV // need not to be cached but stored
    }

    private static String saveDownloadFile(boolean useDownload, DownloadStoreType storeType, String innerPath, String fullPath, Result<Runnable> rCloser, RawFileData fileData, String ID) {
        return saveDownloadFile(useDownload, false, storeType, innerPath, fullPath, rCloser, null, fileData::write, ID);
    }

    private static String saveDownloadFile(boolean useDownload, boolean override, DownloadStoreType storeType, String innerPath, String fileName, Result<Runnable> rCloser, Result<String> rUrl, EConsumer<OutputStream, IOException> consumer, String ID) {
        String filePath;
        String url;
        String extraPath = null;
        switch (storeType) {
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

        assert !fileName.startsWith("/");
        String innerFileName = fileName;
        if(useDownload && ID != null)
            innerFileName = BaseUtils.replaceFileNameAndExtension(fileName, ID);

        if(!innerPath.isEmpty()) {
            filePath += "/" + innerPath;
            url += innerPath + "/";
        }

        Runnable closer = writeFile(filePath, override, innerFileName, consumer);
        if(rCloser != null)
            rCloser.set(closer);

        if(rUrl != null)
            rUrl.set(url);

        try {
            url += URIUtil.encodePath(fileName);
        } catch (URIException e) {
            throw Throwables.propagate(e);
        }

        String urlParams = "";
        if(useDownload && ID != null)
            urlParams = "version=" + ID;

        if(urlParams.isEmpty()) // we assume that url should have params, because getDownloadParams assumes that
            urlParams = "dumb=0";

        return url + "?" + urlParams;
    }

    public static String saveApplicationFile(FileData fileData) { // for login page, logo and icon images
        return saveStaticDataFile(fileData, null, true, null);
    }

    public static String saveActionFile(RawFileData fileData, String extension, String displayName) { // with single usage (action scoped), so will be deleted just right after downloaded
        return saveDataFile(false, false, null, new FileData(fileData, extension), displayName);
    }

    private static String saveStaticDataFile(FileData fileData, String displayName, boolean noAuth, Map<String, Pair<String, Runnable>> cacheMap) {
        return saveDataFile(true, noAuth, cacheMap, fileData, displayName);
    }

    public static String saveFormFile(FileData fileData, String displayName, Map<String, Pair<String, Runnable>> cacheMap) { // multiple usages (form scoped), so should be deleted just right after form is closed
        return saveStaticDataFile(fileData, displayName, false, cacheMap);
    }

    private static String saveDataFile(boolean isStatic, boolean noAuth, Map<String, Pair<String, Runnable>> cacheMap, FileData fileData, String displayName) {
        // we don't want to override file, since it's name is based on base64Hash or random
        assert !(noAuth && !isStatic); // noAuth => isStatic

        RawFileData rawFile = fileData.getRawFile();
        String fileID = isStatic ? rawFile.getID() : BaseUtils.randomString(15);
        if(displayName != null)
            fileID = fileID + "/" + displayName;
        String fileName = BaseUtils.addExtension(fileID, fileData.getExtension());

        Function<Result<Runnable>, String> saveData = (rCloser) ->  saveDownloadFile(true, isStatic ? DownloadStoreType.STATIC : DownloadStoreType.TEMP, noAuth ? NOAUTH_FOLDER_PREFIX : "", fileName, rCloser, rawFile, null);

        if(cacheMap != null) {
            Pair<String, Runnable> savedFile = cacheMap.get(fileName);
            if (savedFile == null) {
                Result<Runnable> rCloser = new Result<>();
                savedFile = new Pair<>(saveData.apply(rCloser), rCloser.result);
                cacheMap.put(fileName, savedFile);
            }
            return savedFile.first;
        }

        return saveData.apply(null);
    }

    public static String exportFile(FileData file) {
        try {
            return FileUtils.saveActionFile(file.getRawFile(), file.getExtension(), "lsfReport");
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
