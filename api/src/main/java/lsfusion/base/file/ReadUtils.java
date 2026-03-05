package lsfusion.base.file;

import com.google.common.base.Throwables;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lsfusion.base.BaseUtils;
import lsfusion.base.MIMETypeUtils;
import lsfusion.base.Pair;
import lsfusion.base.SystemUtils;
import lsfusion.interop.session.ExternalUtils;
import org.apache.commons.httpclient.util.URIUtil;
import org.jfree.ui.ExtensionFileFilter;

import javax.mail.internet.ParseException;
import javax.swing.*;
import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ReadUtils {

    public static ReadResult readFile(String sourcePath, boolean isBlockingFileRead, boolean isDialog, ExtraReadInterface extraReadProcessor) throws IOException, SQLException {
        String dialogPath = null;
        if (isDialog) {
            sourcePath = showReadFileDialog(sourcePath);
            if(sourcePath == null) {
                return null;
            }
            dialogPath = sourcePath;
        }

        Path filePath = Path.parsePath(sourcePath, true);

        File localFile = null;
        if(!filePath.type.equals("file"))
            localFile = File.createTempFile("downloaded", ".tmp");

        try {
            File file = localFile;
            Pair<String, String> fileNameAndExtension;
            switch (filePath.type) {
                case "file":
                    file = new File(filePath.path);
                    fileNameAndExtension = getFileNameAndExtension(filePath.path);
                    break;
                case "http":
                case "https":
                    fileNameAndExtension = copyHTTPToFile(filePath, localFile);
                    break;
                case "ftp":
                    fileNameAndExtension = copyFTPToFile(filePath.path, localFile);
                    break;
                case "sftp":
                    fileNameAndExtension = copySFTPToFile(filePath.path, localFile);
                    break;
                default:
                    if(extraReadProcessor != null) {
                        fileNameAndExtension = new Pair<>("file", extraReadProcessor.copyToFile(filePath.type, sourcePath, localFile));
                    } else {
                        throw new RuntimeException(String.format("READ %s is not supported", filePath.type));
                    }
            }
            RawFileData rawFileData;
            if (file.exists()) {
                if (isBlockingFileRead) {
                    try (FileChannel channel = new RandomAccessFile(file, "rw").getChannel()) {
                        try (java.nio.channels.FileLock lock = channel.lock()) {
                            rawFileData = readBytesFromChannel(channel);
                        }
                    }
                } else {
                    rawFileData = new RawFileData(file);
                }
            } else {
                throw new RuntimeException("Read Error. File not found: " + sourcePath);
            }
            return new ReadResult(new NamedFileData(new FileData(rawFileData, fileNameAndExtension.second), fileNameAndExtension.first), filePath.type, dialogPath);
        } finally {
            BaseUtils.safeDelete(localFile);
        }
    }

    private static Pair<String, String> readFileExtension(URLConnection connection, URL url) {
        String contentDispositionHeader = connection.getHeaderField("Content-Disposition");
        Pair<String, String> fileAndNameExtension = null;
        if (contentDispositionHeader != null) {
            try {
                fileAndNameExtension = getFileNameAndExtension(ExternalUtils.getContentDispositionFileName(contentDispositionHeader).second);
            } catch (ParseException e) {
                //do nothing
            }
        }

        if (fileAndNameExtension == null)
            fileAndNameExtension = getFileNameAndExtension(url.getPath());

        String contentType = connection.getHeaderField("Content-Type");
        if (fileAndNameExtension == null && contentType != null)
            fileAndNameExtension = new Pair<>("file", MIMETypeUtils.fileExtensionForMIMEType(contentType));

        return fileAndNameExtension;
    }

    private static String getFileExtension(String filename) {
        int endIndex = filename.indexOf("?");
        String result = filename.substring(0, endIndex == -1 ? filename.length() : endIndex);
        int beginIndex = result.lastIndexOf(".");
        return beginIndex == -1 ? "" : result.substring(beginIndex + 1);
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

    private static Pair<String, String> copyHTTPToFile(Path path, File file) throws IOException {
        final List<String> properties = parseHTTPPath(path.path);
        String urlSpec;
        if (properties != null) {
            final String username = properties.get(1);
            final String password = properties.get(2);
            String pathToFile = properties.get(3);
            Authenticator.setDefault(new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(username, password.toCharArray()));
                }
            });
            urlSpec = URIUtil.encodeQuery(path.type + "://" + pathToFile);
        } else {
            urlSpec = path.type + "://" + path.path;
        }

        URL url = new URL(urlSpec);
        URLConnection connection = url.openConnection();
        connection.connect();

        org.apache.commons.io.FileUtils.copyInputStreamToFile(connection.getInputStream(), file);
        return readFileExtension(connection, url);
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

    private static Pair<String, String> copyFTPToFile(String path, File file) {
        return IOUtils.ftpAction(path, (ftpPath, ftpClient) -> {
            try {
                try(OutputStream outputStream = new FileOutputStream(file)) {
                    if (!ftpClient.retrieveFile(ftpPath.remoteFile, outputStream)) {
                        throw Throwables.propagate(new RuntimeException("Failed to copy '" + path + "'"));
                    }
                    return getFileNameAndExtension(ftpPath.remoteFile);
                }
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        });
    }

    private static Pair<String, String> copySFTPToFile(String path, File file) {
        return IOUtils.sftpAction(path, (ftpPath, channelSftp) -> {
            try {
                channelSftp.get(ftpPath.remoteFile, file.getAbsolutePath());
            } catch (SftpException e) {
                if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)
                    throw new RuntimeException(String.format("Path '%s' not found for %s", ftpPath.remoteFile, path), e);
                else
                    throw Throwables.propagate(e);
            }
            return getFileNameAndExtension(ftpPath.remoteFile);
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
        public final NamedFileData fileData;
        public final String type;
        public final String dialogPath;

        public ReadResult(NamedFileData fileData, String type, String dialogPath) {
            this.fileData = fileData;
            this.type = type;
            this.dialogPath = dialogPath;
        }
    }

    public static Pair<String, String> getFileNameAndExtension(String path) {
        return new Pair<>(BaseUtils.getFileName(path), BaseUtils.getFileExtension(path));
    }
}