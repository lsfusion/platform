package roman;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.server.form.instance.FormInstance;
import platform.server.integration.*;
import platform.server.logics.ObjectValue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TNVEDDutiesImporter extends TNVEDImporter {
    private ImportField dutyTypeField = new ImportField(LM.sidTypeDuty);
    private ImportField category10Field = new ImportField(LM.sidCustomCategory10);
    private ImportField dutyPercentField = new ImportField(LM.dutyPercentCustomCategory10TypeDuty);
    private ImportField dutyEuroField = new ImportField(LM.dutySumCustomCategory10TypeDuty);

    public TNVEDDutiesImporter(FormInstance executeForm, ObjectValue value, RomanLogicsModule LM) {
        super(executeForm, value, LM);
    }

    public void doImport() throws IOException, xBaseJException, SQLException {
        category10sids = getFullCategory10();
        ImportKey category10Key = new ImportKey(LM.customCategory10, LM.sidToCustomCategory10.getMapping(category10Field));
        ImportKey dutyTypeKey = new ImportKey(LM.typeDuty, LM.sidToTypeDuty.getMapping(dutyTypeField));

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        properties.add(new ImportProperty(dutyTypeField, LM.sidTypeDuty.getMapping(dutyTypeKey)));
        properties.add(new ImportProperty(dutyPercentField, LM.dutyPercentCustomCategory10TypeDuty.getMapping(category10Key, dutyTypeKey)));
        properties.add(new ImportProperty(dutyEuroField, LM.dutySumCustomCategory10TypeDuty.getMapping(category10Key, dutyTypeKey)));

        ImportKey<?>[] keysArray = {category10Key, dutyTypeKey};
        new IntegrationService(session, getImportTable(), Arrays.asList(keysArray), properties).synchronize();
    }

    private ImportTable getImportTable() throws IOException, xBaseJException, SQLException {
        File tempFile = File.createTempFile("tempTnved", ".dbf");
        byte buf[] = (byte[]) value.getValue();
        IOUtils.putFileBytes(tempFile, buf);

        DBF file = new DBF(tempFile.getPath());

        List<List<Object>> data = new ArrayList<List<Object>>();
        int recordCount = file.getRecordCount();
        for (int i = 0; i < recordCount; i++) {
            file.read();
            String dutyType = new String(file.getField("PP").getBytes(), "Cp866");
            String code = new String(file.getField("KOD").getBytes(), "Cp866");
            Double dutyPercent = Double.valueOf(new String(file.getField("STAV_A").getBytes(), "Cp866"));
            Double dutyEuro = Double.valueOf(new String(file.getField("STAV_S").getBytes(), "Cp866"));
            for (String code10 : getCategory10(code.trim())) {
                List<Object> row = new ArrayList<Object>();
                row.add(dutyType);
                row.add(code10);
                row.add(dutyPercent);
                row.add(dutyEuro);
                data.add(row);
            }
        }
        List<ImportField> fields = BaseUtils.toList(dutyTypeField, category10Field, dutyPercentField, dutyEuroField);
        file.close();
        tempFile.delete();
        return new ImportTable(fields, data);
    }
}
