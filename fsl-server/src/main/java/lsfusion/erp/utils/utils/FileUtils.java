package lsfusion.erp.utils.utils;

import com.google.common.base.Throwables;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.ReadUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

    public static void deleteFile(ExecutionContext context, String sourcePath, boolean isClient) throws SftpException, JSchException, IOException {
        Path path = parsePath(sourcePath, isClient);
        if (isClient) {
            boolean result = (boolean) context.requestUserInteraction(new FileClientAction(1, path.path));
            if (!result)
                throw new RuntimeException(String.format("Failed to delete file '%s'", sourcePath));
        } else {
            switch (path.type) {
                case "ftp":
                    ReadUtils.deleteFTPFile(path.path);
                    break;
                case "sftp":
                    ReadUtils.deleteSFTPFile(path.path);
                    break;
                default:
                    File sourceFile = new File(path.path);
                    if (!sourceFile.delete()) {
                        throw new RuntimeException(String.format("Failed to delete file '%s'", sourceFile));
                    }
                    break;
            }
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
