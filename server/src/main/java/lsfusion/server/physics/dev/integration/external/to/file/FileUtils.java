package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.base.file.*;
import lsfusion.server.logics.classes.data.utils.image.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.SQLException;
import java.util.List;

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

    @Deprecated
    public static RawFileData createThumbnails(RawFileData inputFile, BufferedImage image, double scale) throws IOException {
        return ImageUtils.createThumbnails(inputFile, image, scale);
    }

    @Deprecated
    public static RawFileData createThumbnails(RawFileData inputFile, BufferedImage image, double scaleWidth, double scaleHeight) throws IOException {
        return ImageUtils.createThumbnails(inputFile, image, scaleWidth, scaleHeight);
    }
}