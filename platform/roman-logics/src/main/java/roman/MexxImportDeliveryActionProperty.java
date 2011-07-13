package roman;

import jxl.read.biff.BiffException;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class MexxImportDeliveryActionProperty extends BaseImportActionProperty {

    public MexxImportDeliveryActionProperty(RomanLogicsModule LM) {
        super(LM, "Импортировать инвойс", LM.mexxSupplier, "zip");
    }


    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        try {

            DataObject supplier = BaseUtils.singleValue(keys);

            List<byte[]> fileList = valueClass.getFiles(value.getValue());
            for (byte[] file : fileList) {

                ByteArrayInputStream stream = new ByteArrayInputStream(file);

                ZipInputStream zin = new ZipInputStream(stream);
                ZipEntry entry = zin.getNextEntry();
                byte[][] outputListInOrder = new byte[4][];

                while ((entry = zin.getNextEntry()) != null) {
                    String name = entry.getName();
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    int data = 0;
                    while ((data = zin.read()) != -1) {
                        output.write(data);
                    }

                    byte[] outputList = BaseUtils.bytesToBytes(output.toByteArray());
                    switch (name.charAt(0)) {
                        case 'W':
                            outputListInOrder[0] = outputList;
                            break;
                        case 'I':
                            outputListInOrder[1] = outputList;
                            break;
                        case 'G':
                            outputListInOrder[2] = outputList;
                            break;
                        case 'K':
                            outputListInOrder[3] = outputList;
                            break;
                    }

                }

                LM.mexxImportInvoice.execute(outputListInOrder[0], session, supplier);
                LM.mexxImportArticleInfoInvoice.execute(outputListInOrder[1], session, supplier);
                LM.mexxImportColorInvoice.execute(outputListInOrder[2], session, supplier);
                LM.mexxImportPricesInvoice.execute(outputListInOrder[3], session, supplier);
            }
            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception
                e) {
            throw new RuntimeException(e);
        }


    }
}
