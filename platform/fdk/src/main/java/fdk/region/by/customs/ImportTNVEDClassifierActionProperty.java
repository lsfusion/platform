package fdk.region.by.customs;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.IOUtils;
import platform.server.classes.*;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImportTNVEDClassifierActionProperty extends ScriptingActionProperty {

    public ImportTNVEDClassifierActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            Object countryBelarus = LM.findLCPByCompoundName("countrySID").read(context.getSession(), new DataObject("112", StringClass.get(3)));
            LM.findLCPByCompoundName("defaultCountry").change(countryBelarus, context.getSession());
            context.getSession().apply(context.getBL());

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.getDefinedInstance(false, "Файлы DBF", "DBF");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

                for (byte[] file : fileList) {
                    importGroups(context, file);
                    importParents(context, file);
                }
            }
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private void importGroups(ExecutionContext<ClassPropertyInterface> context, byte[] fileBytes) throws IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException, SQLException {

        File tempFile = File.createTempFile("tempTnved", ".dbf");
        IOUtils.putFileBytes(tempFile, fileBytes);

        DBF file = new DBF(tempFile.getPath());

        List<List<Object>> data = new ArrayList<List<Object>>();

        Double defaultVAT = 20.0;
        Date defaultDate = new Date(2001 - 1900, 0, 1);

        int recordCount = file.getRecordCount();
        for (int i = 1; i <= recordCount; i++) {
            file.read();

            String groupID = new String(file.getField("KOD").getBytes(), "Cp866").trim();
            String name = new String(file.getField("NAIM").getBytes(), "Cp866").trim();
            String extraName = new String(file.getField("KR_NAIM").getBytes(), "Cp866").trim();

            Boolean hasCode = true;
            if (groupID.equals("··········")) {
                groupID = "-" + i;
                hasCode = null;
            }
            data.add(Arrays.asList((Object) groupID, name + extraName, i, "Таможенный союз", hasCode, defaultVAT, defaultDate));
        }
        ImportField codeCustomsGroupField = new ImportField(LM.findLCPByCompoundName("codeCustomsGroup"));
        ImportField nameCustomsGroupField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField numberCustomsGroupField = new ImportField(LM.findLCPByCompoundName("numberCustomsGroup"));
        ImportField nameCustomsZoneField = new ImportField(LM.findLCPByCompoundName("name"));
        ImportField hasCodeCustomsGroupField = new ImportField(LM.findLCPByCompoundName("hasCodeCustomsGroup"));
        ImportField vatField = new ImportField(LM.findLCPByCompoundName("dataValueSupplierVATCustomsGroupDate"));
        ImportField dateField = new ImportField(DateClass.instance);

        ImportKey<?> customsGroupKey = new ImportKey((CustomClass) LM.findClassByCompoundName("customsGroup"), LM.findLCPByCompoundName("customsGroupCode").getMapping(codeCustomsGroupField));
        ImportKey<?> customsZoneKey = new ImportKey((CustomClass) LM.findClassByCompoundName("customsZone"), LM.findLCPByCompoundName("customsZoneName").getMapping(nameCustomsZoneField));
        ImportKey<?> VATKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("range"), LM.findLCPByCompoundName("valueCurrentVATDefaultValue").getMapping(vatField));

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(codeCustomsGroupField, LM.findLCPByCompoundName("codeCustomsGroup").getMapping(customsGroupKey)));
        properties.add(new ImportProperty(nameCustomsGroupField, LM.findLCPByCompoundName("name").getMapping(customsGroupKey)));
        properties.add(new ImportProperty(numberCustomsGroupField, LM.findLCPByCompoundName("numberCustomsGroup").getMapping(customsGroupKey)));
        properties.add(new ImportProperty(nameCustomsZoneField, LM.findLCPByCompoundName("name").getMapping(customsZoneKey)));
        properties.add(new ImportProperty(nameCustomsZoneField, LM.findLCPByCompoundName("customsZoneCustomsGroup").getMapping(customsGroupKey),
                LM.object(LM.findClassByCompoundName("customsZone")).getMapping(customsZoneKey)));
        properties.add(new ImportProperty(hasCodeCustomsGroupField, LM.findLCPByCompoundName("hasCodeCustomsGroup").getMapping(customsGroupKey)));
        properties.add(new ImportProperty(vatField, LM.findLCPByCompoundName("dataSupplierVATCustomsGroupDate").getMapping(customsGroupKey, dateField),
                LM.object(LM.findClassByCompoundName("range")).getMapping(VATKey)));

        ImportTable table = new ImportTable(Arrays.asList(codeCustomsGroupField, nameCustomsGroupField,
                numberCustomsGroupField, nameCustomsZoneField, hasCodeCustomsGroupField, vatField, dateField), data);

        DataSession session = context.createSession();
        IntegrationService service = new IntegrationService(session, table,
                Arrays.asList(customsGroupKey, customsZoneKey, VATKey), properties);
        service.synchronize(true, false);
        session.apply(context.getBL());
        session.close();
    }

    private void importParents(ExecutionContext<ClassPropertyInterface> context, byte[] fileBytes) throws IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException, SQLException {

        File tempFile = File.createTempFile("tempTnved", ".dbf");
        IOUtils.putFileBytes(tempFile, fileBytes);

        DBF file = new DBF(tempFile.getPath());

        List<List<Object>> data = new ArrayList<List<Object>>();
        List<String> groupIDsList = new ArrayList<String>();
        int recordCount = file.getRecordCount();
        for (int i = 1; i <= recordCount; i++) {
            file.read();

            String groupID = new String(file.getField("KOD").getBytes(), "Cp866").trim();
            String parentID = null;
            if (!groupID.equals("··········"))
                for (int j = groupID.length() - 1; j > 0; j--) {
                    if (groupIDsList.contains(groupID.substring(0, j))) {
                        parentID = groupID.substring(0, j);
                        break;
                    }
                }

            if (groupID.equals("··········")) {
                groupID = "-" + i;
            }

            data.add(Arrays.asList((Object) groupID, parentID));
            groupIDsList.add(groupID);
        }
        ImportField groupIDField = new ImportField(LM.findLCPByCompoundName("codeCustomsGroup"));
        ImportField parentIDField = new ImportField(LM.findLCPByCompoundName("codeCustomsGroup"));

        ImportKey<?> customsGroupKey = new ImportKey((CustomClass) LM.findClassByCompoundName("customsGroup"), LM.findLCPByCompoundName("customsGroupCode").getMapping(groupIDField));
        ImportKey<?> parentCustomsGroupKey = new ImportKey((CustomClass) LM.findClassByCompoundName("customsGroup"), LM.findLCPByCompoundName("customsGroupCode").getMapping(parentIDField));

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(parentIDField, LM.findLCPByCompoundName("parentCustomsGroup").getMapping(customsGroupKey),
                LM.object(LM.findClassByCompoundName("customsGroup")).getMapping(parentCustomsGroupKey)));

        ImportTable table = new ImportTable(Arrays.asList(groupIDField, parentIDField), data);

        DataSession session = context.createSession();
        IntegrationService service = new IntegrationService(session, table,
                Arrays.asList(customsGroupKey, parentCustomsGroupKey), properties);
        service.synchronize(true, false);
        session.apply(context.getBL());
        session.close();
    }
}