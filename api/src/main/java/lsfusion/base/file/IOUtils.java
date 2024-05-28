package lsfusion.base.file;

import com.google.common.base.Throwables;
import com.jcraft.jsch.*;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.hc.core5.http.HttpEntity;

import java.io.*;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IOUtils {
    public static final int BUFFER_SIZE = 16384;
    public static final String lineSeparator = System.getProperty("line.separator", "\n");

    public static byte[] readBytesFromHttpEntity(HttpEntity entity) throws IOException {
        //there is restriction in MultipartFormEntity: max contentLength = 25 * 1024 bytes
        String contentType = entity.getContentType();
        if(contentType != null && contentType.startsWith("multipart/")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            entity.writeTo(out);
            return out.toByteArray();
        } else {
            return readBytesFromStream(entity.getContent());
        }
    }

    public static byte[] readBytesFromStream(InputStream in) throws IOException {
        return readBytesFromStream(in, Integer.MAX_VALUE);
    }

    public static byte[] readBytesFromStream(InputStream in, int maxLength) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte buffer[] = new byte[BUFFER_SIZE];

        int left = maxLength;
        int readCount;
        while (left > 0 && (readCount = in.read(buffer, 0, Math.min(buffer.length, left))) != -1) {
            out.write(buffer, 0, readCount);
            left -= readCount;
        }

        return out.toByteArray();
    }

    public static byte[] getFileBytes(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return readBytesFromStream(in);
        }
    }

    public static byte[] getFileBytes(String filePath) throws IOException {
        try (InputStream in = new FileInputStream(filePath)) {
            return readBytesFromStream(in);
        }
    }

    public static void putFileBytes(File file, byte[] array) throws IOException {
        putFileBytes(file, array, 0, array.length);
    }

    public static void putFileBytes(File file, byte[] array, int off, int len) throws IOException {
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        try (OutputStream out = new FileOutputStream(file)) {
            out.write(array, off, len);
        }
    }

    public static String readFileToString(String fileName) throws IOException {
        return readFileToString(fileName, null);
    }

    public static String readFileToString(String fileName, String charsetName) throws IOException {
        return readStreamToString(new FileInputStream(fileName), charsetName);
    }

    public static String readStreamToString(InputStream inStream) throws IOException {
        return readStreamToString(inStream, null);
    }

    public static String readStreamToString(InputStream inStream, String charsetName) throws IOException {
        ByteArrayOutputStream tmpByteStream = new ByteArrayOutputStream();
        byte last = getLastByteAndCopyStream(inStream, tmpByteStream);
        boolean endsWithNewLine = last == '\n';
        
        InputStream newInStream = new ByteArrayInputStream(tmpByteStream.toByteArray());
        String result;
        try (BufferedReader reader = new BufferedReader(charsetName == null
                                                            ? new InputStreamReader(newInStream)
                                                            : new InputStreamReader(newInStream, charsetName))) {
            result = reader.lines().collect(Collectors.joining(lineSeparator));
        }
        
        if (endsWithNewLine) {
            result += lineSeparator;
        }
        
        return result;
    }

    private static byte getLastByteAndCopyStream(InputStream inStream, ByteArrayOutputStream outStream) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            byte lastByte = 0;
            int bytesRead;
            while ((bytesRead = inStream.read(buffer, 0, buffer.length)) != -1) {
                outStream.write(buffer, 0, bytesRead);
                lastByte = buffer[bytesRead - 1];
            }
            return lastByte;
        } finally {
            inStream.close();
        }
    }

    private static void writeImage(DataOutputStream outStream, Object imageHolder) throws IOException {
        outStream.writeBoolean(imageHolder != null);
        if (imageHolder != null)
            new ObjectOutputStream(outStream).writeObject(imageHolder);
    }

    public static void writeAppImage(DataOutputStream outStream, AppImage imageHolder) throws IOException {
        writeImage(outStream, imageHolder);
    }

    public static void writeAppFileDataImage(DataOutputStream outStream, AppFileDataImage imageHolder) throws IOException {
        writeImage(outStream, imageHolder);
    }

    private static <T> T readImage(DataInputStream inStream, Function<Object, T> imageType) throws IOException {
        if (inStream.readBoolean()) {
            ObjectInputStream in = new ObjectInputStream(inStream);
            try {
                return imageType.apply(in.readObject());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    public static AppImage readAppImage(DataInputStream inStream) throws IOException {
        return readImage(inStream, object -> (AppImage) object);
    }

    public static AppFileDataImage readAppFileDataImage(DataInputStream inStream) throws IOException {
        return readImage(inStream, object -> (AppFileDataImage)object);
    }

    public static File createTempDirectory(String prefix) throws IOException {
        final File tempFile = File.createTempFile(prefix, Long.toString(System.nanoTime()));

        if (!tempFile.delete()) {
            throw new IOException("Could not delete temp file: " + tempFile.getAbsolutePath());
        }

        if (!tempFile.mkdir()) {
            throw new IOException("Could not create temp directory: " + tempFile.getAbsolutePath());
        }

        return tempFile;
    }

    public static <R> R ftpAction(String path, BiFunction<FTPPath, FTPClient, R> function) {
        FTPPath ftpPath = FTPPath.parseFTPPath(path);
        FTPClient ftpClient = new FTPClient();
        ftpClient.setDataTimeout(ftpPath.dataTimeout);
        ftpClient.setConnectTimeout(ftpPath.connectTimeout);

        if (ftpPath.charset != null) {
            ftpClient.setControlEncoding(ftpPath.charset);
        } else {
            ftpClient.setAutodetectUTF8(true);
        }

        try {
            ftpClient.connect(ftpPath.server, ftpPath.port);
            boolean login = ftpClient.login(ftpPath.username, ftpPath.password);
            if (login) {
                if (ftpPath.passiveMode) {
                    ftpClient.enterLocalPassiveMode();
                }
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                if (ftpPath.binaryTransferMode) {
                    ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
                }
                //large buffer size increases download speed
                //https://stackoverflow.com/questions/30847433/very-slow-ftp-download
                ftpClient.setBufferSize(1024 * 1024);

                return function.apply(ftpPath, ftpClient);
            } else {
                throw new RuntimeException("Incorrect login or password '" + path + "'");
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.setSoTimeout(10000);
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static <R> R sftpAction(String path, BiFunction<FTPPath, ChannelSftp, R> function) {
        FTPPath ftpPath = FTPPath.parseSFTPPath(path);
        ftpPath.remoteFile = ftpPath.remoteFile.replace("\\", "/");
        ftpPath.remoteFile = (!ftpPath.remoteFile.startsWith("/") ? "/" : "") + ftpPath.remoteFile;

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(ftpPath.username, ftpPath.server, ftpPath.port);
            session.setPassword(ftpPath.password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            if (ftpPath.charset != null)
                channelSftp.setFilenameEncoding(ftpPath.charset);

            return function.apply(ftpPath, channelSftp);
        } catch (JSchException | SftpException e) {
            throw Throwables.propagate(e);
        } finally {
            if (channelSftp != null)
                channelSftp.exit();
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();
        }
    }

    public static String serializeAppImage(AppImage image) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        writeAppImage(new DataOutputStream(outStream), image);
        return outStream.toString();
    }

    public static AppImage deserializeAppImage(String image) throws IOException {
        return readAppImage(new DataInputStream(new ByteArrayInputStream(image.getBytes())));
    }
}
