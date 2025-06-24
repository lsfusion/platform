package lsfusion.base.file;

import com.google.common.base.Throwables;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lsfusion.base.BaseUtils;
import lsfusion.base.MIMETypeUtils;
import lsfusion.base.SystemUtils;
import org.apache.commons.httpclient.util.URIUtil;
import org.jfree.ui.ExtensionFileFilter;

import javax.mail.internet.ContentDisposition;
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
                    extension = copyHTTPToFile(filePath, localFile);
                    break;
                case "ftp":
                    copyFTPToFile(filePath.path, localFile);
                    extension = getFileExtension(filePath.path);
                    break;
                case "sftp":
                    copySFTPToFile(filePath.path, localFile);
                    extension = getFileExtension(filePath.path);
                    break;
                default:
                    if(extraReadProcessor != null) {
                        extraReadProcessor.copyToFile(filePath.type, sourcePath, localFile);
                        extension = filePath.type;
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
            return new ReadResult(new FileData(rawFileData, extension), filePath.type);
        } finally {
            BaseUtils.safeDelete(localFile);
        }
    }

    private static String readFileExtension(URLConnection connection, String urlString) {
        String contentDispositionHeader = connection.getHeaderField("Content-Disposition");
        if (contentDispositionHeader != null) {
            ContentDisposition contentDisposition = null;
            try {
                contentDisposition = new ContentDisposition(contentDispositionHeader);
            } catch (ParseException e) {
                //do nothing
            }

            String fileExtension = contentDisposition != null ? getFileExtension(contentDisposition.getParameter("filename")) : "";
            if (!fileExtension.isEmpty())
                return fileExtension;
        }

        String fileExtension = null;
        String contentType = connection.getHeaderField("Content-Type");
        if (contentType != null)
            fileExtension = MIMETypeUtils.fileExtensionForMIMEType(contentType);

        return fileExtension != null ? fileExtension : getFileExtension(urlString);
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

    private static String copyHTTPToFile(Path path, File file) throws IOException {
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

        URLConnection connection = new URL(urlSpec).openConnection();
        connection.connect();

        String extension = readFileExtension(connection, urlSpec);
        org.apache.commons.io.FileUtils.copyInputStreamToFile(connection.getInputStream(), file);
        return extension;
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
        public final FileData fileData;
        public final String type;

        public ReadResult(FileData fileData, String type) {
            this.fileData = fileData;
            this.type = type;
        }
    }
}