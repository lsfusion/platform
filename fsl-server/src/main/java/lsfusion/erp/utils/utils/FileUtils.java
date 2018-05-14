package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.ReadUtils;
import lsfusion.server.logics.property.actions.WriteActionProperty;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

    public static void moveFile(ExecutionContext context, String sourcePath, String destinationPath, boolean isClient) throws SQLException, JSchException, SftpException, IOException {
        Path srcPath = parsePath(sourcePath, isClient);
        Path destPath = parsePath(destinationPath, isClient);

        if(isClient) {
            boolean result = (boolean) context.requestUserInteraction(new FileClientAction(2, srcPath.path, destPath.path));
            if (!result)
                throw new RuntimeException(String.format("Failed to move file from %s to %s", sourcePath, destinationPath));
        } else {
            ReadUtils.ReadResult readResult = ReadUtils.readFile(sourcePath, false, false, false);
            if (readResult.errorCode == 0) {
                File sourceFile = new File(readResult.filePath);
                try {
                    switch (destPath.type) {
                        case "ftp":
                            WriteActionProperty.storeFileToFTP(destinationPath, sourceFile);
                            break;
                        case "sftp":
                            WriteActionProperty.storeFileToSFTP(destinationPath, sourceFile);
                            break;
                        default:
                            ServerLoggers.importLogger.info(String.format("Writing file to %s", destinationPath));
                            FileCopyUtils.copy(sourceFile, new File(destPath.path));
                            break;
                    }
                } finally {
                    deleteFile(srcPath, sourcePath);
                }

            } else if (readResult.error != null) {
                throw new RuntimeException(readResult.error);
            }
        }
    }

    public static void deleteFile(ExecutionContext context, String sourcePath, boolean isClient) throws SftpException, JSchException, IOException {
        Path path = parsePath(sourcePath, isClient);
        if (isClient) {
            boolean result = (boolean) context.requestUserInteraction(new FileClientAction(1, path.path));
            if (!result)
                throw new RuntimeException(String.format("Failed to delete file '%s'", sourcePath));
        } else {
            deleteFile(path, sourcePath);
        }
    }

    private static void deleteFile(Path path, String sourcePath) throws IOException, SftpException, JSchException {
        File sourceFile = new File(path.path);
        if (!sourceFile.delete()) {
            throw new RuntimeException(String.format("Failed to delete file '%s'", sourceFile));
        }
        if (path.type.equals("ftp")) {
            //todo: parseFTPPath может принимать путь без ftp://
            ReadUtils.deleteFTPFile(sourcePath);
        } else if (path.type.equals("sftp")) {
            //todo: parseFTPPath может принимать путь без ftp://
            ReadUtils.deleteSFTPFile(sourcePath);
        }
    }

    private static Path parsePath(String sourcePath,  boolean isClient) {
        String pattern = isClient ? "(file):(?://)?(.*)" : "(file|ftp|sftp):(?://)?(.*)";
        String[] types = isClient ? new String[]{"file:"} : new String[]{"file:", "ftp:", "sftp:"};

        if (!StringUtils.startsWithAny(sourcePath, types)) {
            sourcePath = "file:" + sourcePath;
        }
        Matcher m = Pattern.compile(pattern).matcher(sourcePath);
        if (m.matches()) {
            return new Path(m.group(1).toLowerCase(), m.group(2));
        } else {
            throw Throwables.propagate(new RuntimeException("Unsupported path: " + sourcePath));
        }
    }

    public static class Path {
        public String type;
        public String path;

        public Path(String type, String path) {
            this.type = type;
            this.path = path;
        }
    }
}