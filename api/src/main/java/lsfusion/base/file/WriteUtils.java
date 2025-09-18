package lsfusion.base.file;

import com.google.common.base.Throwables;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lsfusion.base.BaseUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.nio.file.Files;

public class WriteUtils {

    public static void write(NamedFileData fileData, String path, boolean client, boolean append) throws IOException {
        Path filePath = Path.parsePath(path);

        switch (filePath.type) {
            case "file": {
                writeFile(filePath.path, fileData, client, append);
                break;
            }
            case "ftp": {
                if(append)
                    throw new RuntimeException("APPEND is not supported in WRITE to FTP");
                storeFileToFTP(filePath.path, fileData);
                break;
            }
            case "sftp": {
                if(append)
                    throw new RuntimeException("APPEND is not supported in WRITE to SFTP");
                storeFileToSFTP(filePath.path, fileData);
                break;
            }
        }
    }

    private static void writeFile(String path, NamedFileData namedFileData, boolean client, boolean append) throws IOException {
        String url = appendExtension(path, namedFileData);
        RawFileData fileData = namedFileData.getRawFile();
        File file = createFile(client ? System.getProperty("user.home") + "/Downloads/" : null, url);
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            throw new RuntimeException(String.format("Path is incorrect or not found: '%s' (resolved to '%s')",
                    path, file.getAbsolutePath()));
        } else if (append && file.exists()) {
            String extension = namedFileData.getExtension();
            switch (extension) {
                case "csv":
                case "txt": {
                    fileData.append(file.getAbsolutePath());
                    break;
                }
                case "xls": {
                    try (HSSFWorkbook sourceWB = new HSSFWorkbook(fileData.getInputStream());
                         HSSFWorkbook destinationWB = new HSSFWorkbook(Files.newInputStream(file.toPath()));
                         FileOutputStream fos = new FileOutputStream(file)) {
                        CopyExcelUtil.copyHSSFSheets(sourceWB, destinationWB);
                        destinationWB.write(fos);
                    }
                    break;
                }
                case "xlsx": {
                    try (XSSFWorkbook sourceWB = new XSSFWorkbook(fileData.getInputStream());
                         XSSFWorkbook destinationWB = new XSSFWorkbook(Files.newInputStream(file.toPath()));
                         FileOutputStream fos = new FileOutputStream(file)) {
                        CopyExcelUtil.copyXSSFSheets(sourceWB, destinationWB);
                        destinationWB.write(fos);
                    }
                    break;
                }
                case "docx": {
                    try (XWPFDocument sourceDoc = new XWPFDocument(fileData.getInputStream());
                         XWPFDocument destinationDoc = new XWPFDocument(Files.newInputStream(file.toPath()));
                         FileOutputStream fos = new FileOutputStream(file)) {
                        CopyWordUtil.copyXWPFDocument(sourceDoc, destinationDoc);
                        destinationDoc.write(fos);
                    }
                    break;
                }
                case "pdf": {
                    appendPDF(file, fileData);
                    break;
                }

                default:
                    throw new RuntimeException("APPEND is supported only for csv, txt, xls, xlsx, docx, pdf files");
            }
        } else {
            fileData.write(file);
        }
    }

    private static void appendPDF(File file, RawFileData fileData) throws IOException {
        PDFMergerUtility ut = new PDFMergerUtility();
        ut.addSource(Files.newInputStream(file.toPath()));
        ut.addSource(fileData.getInputStream());
        ByteArrayOutputStream destStream = new ByteArrayOutputStream();
        ut.setDestinationStream(destStream);
        ut.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        destStream.writeTo(Files.newOutputStream(file.toPath()));
    }

    private static File createFile(String parent, String filePath) {
        File file = new File(filePath);
        if(file.isAbsolute() || filePath.matches("(?i:CON|PRN|AUX|NUL|COM\\d|LPT\\d)"))
            return file;
        return new File(parent, filePath);
    }

    public static void storeFileToFTP(String path, NamedFileData file) {
        IOUtils.ftpAction(path, (ftpPath, ftpClient) -> {
            try {
                String remoteFile = appendExtension(ftpPath.remoteFile, file);
                try(InputStream inputStream = file.getRawFile().getInputStream()) {
                    if (!ftpClient.storeFile(remoteFile, inputStream)) {
                        throw new RuntimeException("Failed to write ftp file: " + ftpClient.getReplyString());
                    }
                }
                return null;
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        });
    }

    public static void storeFileToSFTP(String path, NamedFileData file) {
        IOUtils.sftpAction(path, (ftpPath, channelSftp) -> {
            try {
                File f = new File(appendExtension(ftpPath.remoteFile, file));
                channelSftp.cd(f.getParent().replace("\\", "/"));
                channelSftp.put(file.getRawFile().getInputStream(), f.getName());
            } catch (SftpException e) {
                if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)
                    throw new RuntimeException(String.format("Path '%s' not found for %s", ftpPath.remoteFile, path), e);
                else
                    throw Throwables.propagate(e);
            }
            return null;
        });
    }

    public static String appendExtension(String path, NamedFileData file) {
        String extension = file.getExtension();
        String fileName = file.getName();

        // If path ends with "/", it's a directory → add filename + extension
        if (path.endsWith("/")) {
            return path + fileName + "." + extension;
        }

        String pathFileName = BaseUtils.getFileNameAndExtension(path);

        // If no ".", it's a file without extension → add extension
        if (!pathFileName.contains(".")) {
            return path + "." + extension;
        }

        // Already has extension → leave unchanged
        return path;
    }
}