package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.base.file.*;
import net.coobird.thumbnailator.Thumbnails;

import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.SQLException;
import java.util.List;

import static java.awt.image.BufferedImage.TYPE_BYTE_INDEXED;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

//todo: Replace all usages to lsfusion.base.FileUtils (available since 6.1)
//deprecated since 6.3, will be removed in 8.0
@Deprecated
public class FileUtils {

    @Deprecated
    public static void moveFile(String sourcePath, String destinationPath) throws SQLException, IOException {
        lsfusion.base.FileUtils.moveFile(sourcePath, destinationPath);
    }

    @Deprecated
    public static void copyFile(String sourcePath, String destinationPath) throws SQLException, IOException {
        lsfusion.base.FileUtils.copyFile(sourcePath, destinationPath);
    }

    @Deprecated
    public static void renameFTP(Path srcPath, Path destPath) {
        lsfusion.base.FileUtils.renameFTP(srcPath, destPath);
    }

    @Deprecated
    public static void delete(String sourcePath) {
        lsfusion.base.FileUtils.delete(sourcePath);
    }

    @Deprecated
    public static void delete(Path path) {
        lsfusion.base.FileUtils.delete(path);
    }

    @Deprecated
    public static void mkdir(String directory) {
        lsfusion.base.FileUtils.mkdir(directory);
    }

    @Deprecated
    public static boolean checkFileExists(String sourcePath) {
        return lsfusion.base.FileUtils.checkFileExists(sourcePath);
    }

    @Deprecated
    public static List<Object> listFiles(String sourcePath, boolean recursive) throws IOException {
        return lsfusion.base.FileUtils.listFiles(sourcePath, recursive);
    }

    @Deprecated
    public static String ping(String host) throws IOException {
        return lsfusion.base.FileUtils.ping(host);
    }

    public static RawFileData createThumbnails(RawFileData inputFile, BufferedImage image, double scale) throws IOException {
        return createThumbnails(inputFile, image, scale, scale);
    }

    public static RawFileData createThumbnails(RawFileData inputFile, BufferedImage image, double scaleWidth, double scaleHeight) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(inputFile.getInputStream()).scale(scaleWidth, scaleHeight);
            if (image.getType() == TYPE_BYTE_INDEXED) {
                builder.imageType(TYPE_INT_ARGB);
            }
            builder.toOutputStream(os);
            return new RawFileData(os);
        }
    }
}