package lsfusion.base.file;

import com.jcraft.jsch.*;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.Properties;

public class WriteUtils {

    public static void write(RawFileData fileData, String path, String extension, boolean client, boolean append) throws IOException, SftpException, JSchException {
        Path filePath = Path.parsePath(path);

        switch (filePath.type) {
            case "file": {
                writeFile(filePath.path, extension, fileData, client, append);
                break;
            }
            case "ftp": {
                if(append)
                    throw new RuntimeException("APPEND is not supported in WRITE to FTP");
                storeFileToFTP(filePath.path, fileData, extension);
                break;
            }
            case "sftp": {
                if(append)
                    throw new RuntimeException("APPEND is not supported in WRITE to SFTP");
                storeFileToSFTP(filePath.path, fileData, extension);
                break;
            }
        }
    }

    private static void writeFile(String path, String extension, RawFileData fileData, boolean client, boolean append) throws IOException {
        String url = appendExtension(path, extension);
        File file = createFile(client ? System.getProperty("user.home") + "/Downloads/" : null, url);
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            throw new RuntimeException(String.format("Path is incorrect or not found: '%s' (resolved to '%s')",
                    path, file.getAbsolutePath()));
        } else if (append && file.exists()) {
            switch (extension) {
                case "csv":
                    fileData.append(file.getAbsolutePath());
                    break;
                case "xls": {
                    try (HSSFWorkbook sourceWB = new HSSFWorkbook(fileData.getInputStream());
                         HSSFWorkbook destinationWB = new HSSFWorkbook(new FileInputStream(file));
                         FileOutputStream fos = new FileOutputStream(file)) {
                            CopyExcelUtil.copyHSSFSheets(sourceWB, destinationWB);
                            destinationWB.write(fos);
                    }
                    break;
                }
                case "xlsx":
                    try (XSSFWorkbook sourceWB = new XSSFWorkbook(fileData.getInputStream());
                         XSSFWorkbook destinationWB = new XSSFWorkbook(new FileInputStream(file));
                         FileOutputStream fos = new FileOutputStream(file)) {
                            CopyExcelUtil.copyXSSFSheets(sourceWB, destinationWB);
                            destinationWB.write(fos);
                    }
                    break;
                default:
                    throw new RuntimeException("APPEND is supported only for csv, xls, xlsx files");
            }
        } else {
            fileData.write(file);
        }
    }

    private static File createFile(String parent, String filePath) {
        File file = new File(filePath);
        if(file.isAbsolute() || filePath.matches("(?i:CON|PRN|AUX|NUL|COM\\d|LPT\\d)"))
            return file;
        return new File(parent, filePath);
    }

    public static void storeFileToFTP(String path, RawFileData file, String extension) throws IOException {
        FTPPath properties = FTPPath.parseFTPPath(path);
        String remoteFile = appendExtension(properties.remoteFile, extension);
        FTPClient ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(3600000); //1 hour = 3600 sec
        if (properties.charset != null)
            ftpClient.setControlEncoding(properties.charset);
        try {

            ftpClient.connect(properties.server, properties.port);
            if (ftpClient.login(properties.username, properties.password)) {
                if(properties.passiveMode) {
                    ftpClient.enterLocalPassiveMode();
                }
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                if(properties.binaryTransferMode) {
                    ftpClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
                }

                InputStream inputStream = file.getInputStream();
                boolean done = ftpClient.storeFile(remoteFile, inputStream);
                inputStream.close();
                if (!done) {
                    throw new RuntimeException("Error occurred while writing file to ftp : " + ftpClient.getReplyCode());
                }
            } else {
                throw new RuntimeException("Incorrect login or password. Writing file from ftp failed");
            }
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void storeFileToSFTP(String path, RawFileData file, String extension) throws JSchException, SftpException {
        FTPPath properties = FTPPath.parseSFTPPath(path);
        String remoteFilePath = appendExtension(properties.remoteFile, extension);
        File remoteFile = new File((!remoteFilePath.startsWith("/") ? "/" : "") + remoteFilePath);

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(properties.username, properties.server, properties.port);
            session.setPassword(properties.password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            if (properties.charset != null)
                channelSftp.setFilenameEncoding(properties.charset);
            channelSftp.cd(remoteFile.getParent().replace("\\", "/"));
            channelSftp.put(file.getInputStream(), remoteFile.getName());
        } finally {
            if (channelSftp != null)
                channelSftp.exit();
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();
        }
    }

    public static String appendExtension(String path, String extension) {
        //надо учесть, что путь может быть с точкой
        return extension != null && !extension.isEmpty() ? (path + "." + extension) : path;
    }
}