package lsfusion.base.file;

import com.google.common.base.Throwables;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lsfusion.base.BaseUtils;
import lsfusion.base.MIMETypeUtils;
import lsfusion.base.SystemUtils;
import org.apache.commons.httpclient.util.URIUtil;
import org.jfree.ui.ExtensionFileFilter;

import javax.swing.*;
import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ReadUtils {

    public static ReadResult readFile(String sourcePath, boolean isDynamicFormatFileClass, boolean isBlockingFileRead, boolean isDialog, ExtraReadInterface extraReadProcessor) throws IOException, SQLException {
        if (isDialog) {
            sourcePath = showReadFileDialog(sourcePath);
            if(sourcePath == null) {
                return null;
            }
        }

        Path filePath = Path.parsePath(sourcePath, true);

        File localFile = null;
        if(!filePath.type.equals("file"))
            localFile = File.createTempFile("downloaded", ".tmp");

        try {
            File file = localFile;
            String extension;
            switch (filePath.type) {
                case "file":
                    file = new File(filePath.path);
                    extension = BaseUtils.getFileExtension(filePath.path);
                    break;
                case "http":
                case "https":
                    copyHTTPToFile(filePath, localFile);
                    extension = readFileExtension(filePath.type + "://" + filePath.path);
                    break;
                case "ftp":
                    copyFTPToFile(filePath.path, localFile);
                    extension = BaseUtils.getFileExtension(filePath.path);
                    break;
                case "sftp":
                    copySFTPToFile(filePath.path, localFile);
                    extension = BaseUtils.getFileExtension(filePath.path);
                    break;
                default:
                    if(extraReadProcessor != null) {
                        extraReadProcessor.copyToFile(filePath.type, sourcePath, localFile);
                        extension = filePath.type;
                    } else {
                        throw new RuntimeException(String.format("READ %s is not supported", filePath.type));
                    }
            }
            Object fileBytes; // RawFileData or FileData
            if (file.exists()) {
                if (isBlockingFileRead) {
                    try (FileChannel channel = new RandomAccessFile(file, "rw").getChannel()) {
                        try (java.nio.channels.FileLock lock = channel.lock()) {
                            if (isDynamicFormatFileClass) {
                                fileBytes = new FileData(readBytesFromChannel(channel), extension);
                            } else {
                                fileBytes = readBytesFromChannel(channel);
                            }
                        }
                    }
                } else {
                    if (isDynamicFormatFileClass) {
                        fileBytes = new FileData(new RawFileData(file), extension);
                    } else {
                        fileBytes = new RawFileData(file);
                    }
                }
            } else {
                throw new RuntimeException("Read Error. File not found: " + sourcePath);
            }
            return new ReadResult(fileBytes, filePath.type);
        } finally {
            if (localFile != null && !localFile.delete())
                localFile.deleteOnExit();
        }
    }

    private static String readFileExtension(String urlString) throws IOException {
        String fileExtension = null;
        String contentType = new URL(urlString).openConnection().getHeaderField("Content-Type");
        if(contentType != null) {
            fileExtension = MIMETypeUtils.fileExtensionForMIMEType(contentType);
        }
        return fileExtension != null ? fileExtension : BaseUtils.getFileExtension(urlString);
    }

    public static String showReadFileDialog(String path) {
        String result = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);

        File parentDir = null;
        if (path != null) {
            File file = new File(path.startsWith("file://") ? path.substring(7) : path);
            fileChooser.setSelectedFile(file);
            parentDir = file.getParentFile();
            String extension = BaseUtils.getFileExtension(file);
            if (!BaseUtils.isRedundantString(extension)) {
                ExtensionFileFilter filter = new ExtensionFileFilter("." + extension, extension);
                fileChooser.addChoosableFileFilter(filter);
            }
        }
        fileChooser.setCurrentDirectory(parentDir != null ? parentDir : SystemUtils.loadCurrentDirectory());

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            result = fileChooser.getSelectedFile().getAbsolutePath();
            SystemUtils.saveCurrentDirectory(new File(result).getParentFile());
        }
        return result;
    }

    private static void copyHTTPToFile(Path path, File file) throws IOException {
        final List<String> properties = parseHTTPPath(path.path);
        if (properties != null) {
            final String username = properties.get(1);
            final String password = properties.get(2);
            String pathToFile = properties.get(3);
            Authenticator.setDefault(new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(username, password.toCharArray()));
                }
            });
            URL httpUrl = new URL(URIUtil.encodeQuery(path.type + "://" + pathToFile));
            org.apache.commons.io.FileUtils.copyInputStreamToFile(httpUrl.openConnection().getInputStream(), file);
        } else {
            org.apache.commons.io.FileUtils.copyURLToFile(new URL(path.type + "://" + path.path), file);
        }
    }

    private static List<String> parseHTTPPath(String path) {
        /*http|https://username:password@path_to_file*/
        Pattern connectionStringPattern = Pattern.compile("(.*):(.*)@(.*)");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String type = connectionStringMatcher.group(1);
            String username = connectionStringMatcher.group(2);
            String password = connectionStringMatcher.group(3);
            String pathToFile = connectionStringMatcher.group(4);
            return Arrays.asList(type, username, password, pathToFile);
        } else return null;
    }

    private static void copyFTPToFile(String path, File file) {
        IOUtils.ftpAction(path, (ftpPath, ftpClient) -> {
            try {
                try(OutputStream outputStream = new FileOutputStream(file)) {
                    if (!ftpClient.retrieveFile(ftpPath.remoteFile, outputStream)) {
                        throw Throwables.propagate(new RuntimeException("Failed to copy '" + path + "'"));
                    }
                    return null;
                }
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        });
    }

    private static void copySFTPToFile(String path, File file) {
        IOUtils.sftpAction(path, (ftpPath, channelSftp) -> {
            try {
                channelSftp.get(ftpPath.remoteFile, file.getAbsolutePath());
            } catch (SftpException e) {
                if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)
                    throw new RuntimeException(String.format("Path '%s' not found for %s", ftpPath.remoteFile, path), e);
                else
                    throw Throwables.propagate(e);
            }
            return null;
        });
    }

    private static RawFileData readBytesFromChannel(FileChannel channel) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(IOUtils.BUFFER_SIZE);
        int left = Integer.MAX_VALUE;
        int readCount;
        while (left > 0 && (readCount = channel.read(buffer)) > 0) {
            out.write(buffer.array(), 0, readCount);
            left -= readCount;
        }
        return new RawFileData(out);
    }

    public static class ReadResult implements Serializable {
        public final Object fileBytes; // RawFileData or FileData
        String type;

        public ReadResult(Object fileBytes, String type) {
            this.fileBytes = fileBytes;
            this.type = type;
        }
    }
}