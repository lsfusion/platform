package roman;

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import platform.base.BaseUtils;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ByteArrayClass;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MexxImportDeliveryActionProperty extends BaseImportActionProperty {

    public MexxImportDeliveryActionProperty(RomanLogicsModule LM) {
        super(LM, "Импортировать инвойс", LM.mexxSupplier, "zip 7z");
    }


    @Override
    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
        try {

            DataObject supplier = context.getSingleKeyValue();

            List<byte[]> fileList = valueClass.getFiles(userValue);
            for (byte[] file : fileList) {

                ByteArrayInputStream stream = new ByteArrayInputStream(file);
                final byte[][] outputListInOrder = new byte[4][];
                byte[] buff = new byte[2];
                if (stream.read(buff) == 2) {
                    String type = new String(buff);
                    if ("7z".equals(type)) {
                        File tmp = new File("tmp");
                        FileOutputStream fileOuputStream = new FileOutputStream(tmp);
                        fileOuputStream.write(file);
                        fileOuputStream.close();
                        RandomAccessFile randomAccessFile = new RandomAccessFile(tmp, "r");
                        ISevenZipInArchive inArchive = SevenZip.openInArchive(null,
                                new RandomAccessFileInStream(randomAccessFile));
                        ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();

                        for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                            if (!item.isFolder()) {
                                ExtractOperationResult result;
                                result = item.extractSlow(new ISequentialOutStream() {
                                    public int write(byte[] data) throws SevenZipException {
                                        String name = item.getPath();
                                        switch (name.charAt(0)) {
                                            case 'W':
                                                outputListInOrder[0] = BaseUtils.bytesToBytes(data);
                                                break;
                                            case 'I':
                                                outputListInOrder[1] = data;
                                                break;
                                            case 'G':
                                                outputListInOrder[2] = data;
                                                break;
                                            case 'K':
                                                outputListInOrder[3] = data;
                                                break;
                                        }
                                        return data.length;
                                    }
                                });
                                if (result != ExtractOperationResult.OK) {
                                    System.err.println("Error extracting item: " + result);
                                }
                            }
                        }
                        inArchive.close();
                    } else {
                        stream.reset();
                        ZipInputStream zin = new ZipInputStream(stream);
                        ZipEntry entry;

                        while ((entry = zin.getNextEntry()) != null) {
                            String name = entry.getName();
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            int readCount;
                            byte[] buffer = new byte[(int) entry.getSize()];
                            while ((readCount = zin.read(buffer, 0, buffer.length)) != -1) {
                                output.write(buffer, 0, readCount);
                            }
                            switch (name.charAt(0)) {
                                case 'W':
                                    outputListInOrder[0] = BaseUtils.bytesToBytes(output.toByteArray());
                                    break;
                                case 'I':
                                    outputListInOrder[1] = output.toByteArray();
                                    break;
                                case 'G':
                                    outputListInOrder[2] = output.toByteArray();
                                    break;
                                case 'K':
                                    outputListInOrder[3] = output.toByteArray();
                                    break;
                            }
                        }
                    }

                    RomanLM.mexxImportInvoice.execute(context.pushUserInput(new DataObject(outputListInOrder[0], ByteArrayClass.instance)), supplier);
                    RomanLM.mexxImportArticleInfoInvoice.execute(context.pushUserInput(new DataObject(outputListInOrder[1], ByteArrayClass.instance)), supplier);
                    RomanLM.mexxImportColorInvoice.execute(context.pushUserInput(new DataObject(outputListInOrder[2], ByteArrayClass.instance)), supplier);
                    RomanLM.mexxImportPricesInvoice.execute(context.pushUserInput(new DataObject(outputListInOrder[3], ByteArrayClass.instance)),supplier);
                }
            }
            context.delayUserInterfaction(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (IOException
                e) {
            e.printStackTrace();
        } catch (Exception
                e) {
            throw new RuntimeException(e);
        }
    }
}
