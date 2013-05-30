package fdk.region.by.customs;

import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.IOUtils;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.classes.DateClass;
import platform.server.integration.*;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class ImportTNVEDCustomsRatesActionProperty extends ScriptingActionProperty {

    public ImportTNVEDCustomsRatesActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {
            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(false, false, "Файлы DBF", "DBF");
            ObjectValue objectValue = context.requestUserData(valueClass, null);

            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());
                for (byte[] file : fileList) {
                    importDuties(context, file);
                }
            }

        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void importDuties(ExecutionContext<ClassPropertyInterface> context, byte[] fileBytes) throws IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException, SQLException, ParseException {

        File tempFile = File.createTempFile("tempTnved", ".dbf");
        IOUtils.putFileBytes(tempFile, fileBytes);

        DBF file = new DBF(tempFile.getPath());

        Map<String, BigDecimal> registrationMap = new HashMap<String, BigDecimal>();
        List<List<Object>> data = new ArrayList<List<Object>>();

        int recordCount = file.getRecordCount();
        for (int i = 1; i <= recordCount; i++) {
            file.read();

            Integer type = Integer.parseInt(new String(file.getField("PP").getBytes(), "Cp866").trim());
            String groupID = new String(file.getField("KOD").getBytes(), "Cp866").trim();
            BigDecimal stav_a = new BigDecimal(new String(file.getField("STAV_A").getBytes(), "Cp866").trim());
            BigDecimal stav_s = new BigDecimal(new String(file.getField("STAV_S").getBytes(), "Cp866").trim());
            Date dateFrom = new Date(DateUtils.parseDate(new String(file.getField("DATE1").getBytes(), "Cp866").trim(), new String[]{"yyyyMMdd"}).getTime());
            Date dateTo = new Date(DateUtils.parseDate(new String(file.getField("DATE2").getBytes(), "Cp866").trim(), new String[]{"yyyyMMdd"}).getTime());

            switch (type) {
                case 1:
                    if (groupID.length() == 2)
                        registrationMap.put(groupID, stav_s);
                    break;
                case 2:
                    data.add(Arrays.asList((Object) groupID, registrationMap.get(groupID.substring(0, 2)), stav_s, stav_a, null, dateFrom, dateTo));
                    break;
                case 4:
                    data.add(Arrays.asList((Object) groupID, null, null, null, stav_a, dateFrom, dateTo));
                    break;
            }
        }

        ImportField groupIDField = new ImportField(LM.findLCPByCompoundName("codeCustomsGroup"));
        ImportField dateFromField = new ImportField(DateClass.instance);
        ImportField dateToField = new ImportField(DateClass.instance);
        ImportField registrationCustomsGroupDateField = new ImportField(LM.findLCPByCompoundName("dataRegistrationCustomsGroupDate"));
        ImportField percentDutyCustomsGroupDateField = new ImportField(LM.findLCPByCompoundName("dataPercentDutyCustomsGroupDate"));
        ImportField weightDutyCustomsGroupDateField = new ImportField(LM.findLCPByCompoundName("dataWeightDutyCustomsGroupDate"));
        ImportField vatField = new ImportField(LM.findLCPByCompoundName("dataValueSupplierVATCustomsGroupDate"));

        ImportKey<?> customsGroupKey = new ImportKey((CustomClass) LM.findClassByCompoundName("CustomsGroup"),
                LM.findLCPByCompoundName("customsGroupCode").getMapping(groupIDField));
        ImportKey<?> VATKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Range"),
                LM.findLCPByCompoundName("valueCurrentVATDefaultValue").getMapping(vatField));

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(registrationCustomsGroupDateField, LM.findLCPByCompoundName("dataRegistrationCustomsGroupDate").getMapping(customsGroupKey, dateFromField)));
        properties.add(new ImportProperty(percentDutyCustomsGroupDateField, LM.findLCPByCompoundName("dataPercentDutyCustomsGroupDate").getMapping(customsGroupKey, dateFromField)));
        properties.add(new ImportProperty(weightDutyCustomsGroupDateField, LM.findLCPByCompoundName("dataWeightDutyCustomsGroupDate").getMapping(customsGroupKey, dateFromField)));
        properties.add(new ImportProperty(vatField, LM.findLCPByCompoundName("dataSupplierVATCustomsGroupDate").getMapping(customsGroupKey, dateFromField),
                LM.object(LM.findClassByCompoundName("Range")).getMapping(VATKey)));
        properties.add(new ImportProperty(dateFromField, LM.findLCPByCompoundName("dateFromCustomsGroup").getMapping(customsGroupKey)));
        properties.add(new ImportProperty(dateToField, LM.findLCPByCompoundName("dateToCustomsGroup").getMapping(customsGroupKey)));

        ImportTable table = new ImportTable(Arrays.asList(groupIDField, registrationCustomsGroupDateField,
                weightDutyCustomsGroupDateField, percentDutyCustomsGroupDateField, vatField, dateFromField, dateToField), data);

        DataSession session = context.createSession();
        IntegrationService service = new IntegrationService(session, table,
                Arrays.asList(customsGroupKey, VATKey), properties);
        service.synchronize(true, false);
        session.apply(context.getBL());
        session.close();
    }
}