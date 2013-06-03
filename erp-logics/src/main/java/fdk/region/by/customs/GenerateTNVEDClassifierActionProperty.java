package fdk.region.by.customs;

import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import lsfusion.base.IOUtils;
import lsfusion.interop.action.ExportFileClientAction;
import lsfusion.server.classes.CustomStaticFormatFileClass;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GenerateTNVEDClassifierActionProperty extends ScriptingActionProperty {

    public GenerateTNVEDClassifierActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            Map<String, byte[]> files = new HashMap<String, byte[]>();

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(true, true, "Файлы DBF", "DBF");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                Map<String, byte[]> fileList = valueClass.getNamedFiles(objectValue.getValue());

                byte[] fileDuties = fileList.get("TNVED_ST.DBF");
                byte[] fileClassifier = fileList.get("TNVED4.DBF");

                if (fileDuties == null)
                    throw new RuntimeException("Запрашиваемый файл TNVED_ST.DBF не найден");
                if (fileClassifier == null)
                    throw new RuntimeException("Запрашиваемый файл TNVED4.DBF не найден");

                File inputFile = File.createTempFile("inputTNVED", "dbf");
                IOUtils.putFileBytes(inputFile, fileClassifier);

                File outputFile = File.createTempFile("GenerateTNVED", ".lsf");
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF8"));

                writeTop(writer);

                DBF dbfFile = new DBF(inputFile.getAbsolutePath());

                List<String> groupIDsList = new ArrayList<String>();

                Map<String, Object[]> dutiesMap = getDuties(context, fileDuties);

                Set<String> codes = new HashSet<String>();

                int recordCount = dbfFile.getRecordCount();
                for (int i = 1; i <= recordCount; i++) {
                    dbfFile.read();

                    String nameCustomsZone = "ТАМОЖЕННЫЙ СОЮЗ";

                    String code = new String(dbfFile.getField("KOD").getBytes(), "Cp866").trim();
                    String name = new String(dbfFile.getField("NAIM").getBytes(), "Cp866").trim();
                    String extraName = new String(dbfFile.getField("KR_NAIM").getBytes(), "Cp866").trim();
                    String parent = null;

                    BigDecimal registration = null;
                    BigDecimal weightDuty = null;
                    BigDecimal percentDuty = null;
                    BigDecimal vat = null;
                    Date dateFrom = null;
                    Date dateTo = null;
                    Object[] dutiesEntry = dutiesMap.get(code);
                    if (dutiesEntry != null) {
                        registration = (BigDecimal) dutiesEntry[0];
                        weightDuty = (BigDecimal) dutiesEntry[1];
                        percentDuty = (BigDecimal) dutiesEntry[2];
                        vat = (BigDecimal) dutiesEntry[3];
                        dateFrom = (Date) dutiesEntry[4];
                        dateTo = (Date) dutiesEntry[5];
                    }
                    if (!code.equals("··········"))
                        for (int j = code.length() - 1; j > 0; j--) {
                            if (groupIDsList.contains(code.substring(0, j))) {
                                parent = code.substring(0, j);
                                break;
                            }
                        }

                    Boolean hasCode = true;
                    if (code.equals("··········")) {
                        code = "-" + i;
                        hasCode = null;
                    }

                    groupIDsList.add(code);

                    if (!codes.contains(code)) {
                        codes.add(code);
                        writer.println(String.format("EXEC loadDefaultCustomsGroup('%s', %s, '%s', %d, '%s', %s, %s, %s, %s, %s, %s, %s);",
                                code, parent == null ? "NULL" : ("\'" + parent + "\'"), (name + extraName).replace("'", "\\'"),
                                i, nameCustomsZone, hasCode == null ? "NULL" : "TRUE",
                                registration == null ? "NULL" : String.valueOf(registration),
                                weightDuty == null ? "NULL" : String.valueOf(weightDuty),
                                percentDuty == null ? "NULL" : String.valueOf(percentDuty),
                                vat == null ? "NULL" : String.valueOf(vat),
                                dateFrom == null ? "NULL" : new SimpleDateFormat("yyyy_MM_dd").format(dateFrom),
                                dateTo == null ? "NULL" : new SimpleDateFormat("yyyy_MM_dd").format(dateTo)));
                    }
                }

                writeBottom(writer);

                writer.close();

                files.put("GenerateTNVED.lsf", IOUtils.getFileBytes(outputFile));
                context.delayUserInterfaction(new ExportFileClientAction(files));
            }


        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        }

    }

    private Map<String, Object[]> getDuties(ExecutionContext<ClassPropertyInterface> context, byte[] fileBytes) throws IOException, xBaseJException, ParseException {

        Map<String, Object[]> result = new HashMap<String, Object[]>();

        File tempFile = File.createTempFile("dutiesTNVED", ".dbf");
        IOUtils.putFileBytes(tempFile, fileBytes);

        DBF file = new DBF(tempFile.getPath());

        Map<String, BigDecimal> registrationMap = new HashMap<String, BigDecimal>();

        int recordCount = file.getRecordCount();
        for (int i = 1; i <= recordCount; i++) {
            file.read();

            Integer type = Integer.parseInt(new String(file.getField("PP").getBytes(), "Cp866").trim());
            String code = new String(file.getField("KOD").getBytes(), "Cp866").trim();
            BigDecimal stav_a = new BigDecimal(new String(file.getField("STAV_A").getBytes(), "Cp866").trim());
            BigDecimal stav_s = new BigDecimal(new String(file.getField("STAV_S").getBytes(), "Cp866").trim());
            Date dateFrom = new Date(DateUtils.parseDate(new String(file.getField("DATE1").getBytes(), "Cp866").trim(), new String[]{"yyyyMMdd"}).getTime());
            Date dateTo = new Date(DateUtils.parseDate(new String(file.getField("DATE2").getBytes(), "Cp866").trim(), new String[]{"yyyyMMdd"}).getTime());

            switch (type) {
                case 1:
                    if (code.length() == 2)
                        registrationMap.put(code, stav_s);
                    break;
                case 2:
                    result.put(code, new Object[]{registrationMap.get(code.substring(0, 2)), stav_s, stav_a, null, dateFrom, dateTo});
                    break;
                case 4:
                    result.put(code, new Object[]{null, null, null, stav_a, dateFrom, dateTo});
                    break;
            }
        }
        return result;
    }

    private void writeTop(PrintWriter writer) {
        writer.println("MODULE GenerateTNVED;\n" +
                "\n" +
                "REQUIRE System, CustomsGroup, ImportTNVED;\n" +
                "\n" +
                "loadDefaultCustomsZone 'Добавить таможенную зону' = ACTION (name) {\n" +
                "    ADDOBJ CustomsZone;\n" +
                "    FOR c == addedObject() DO {\n" +
                "        SET nameCustomsZone(c) <- name AS STRING[100];\n" +
                "        }\n" +
                "}\n" +
                "\n" +
                "loadDefaultCustomsGroup 'Добавить позицию ТН ВЭД' = ACTION (code, parent, nameCustomsGroup, number, nameCustomsZone, hasCode, registration, weight, percent, vat, dateFrom, dateTo) {\n" +
                "    ADDOBJ CustomsGroup;\n" +
                "    FOR c == addedObject() DO {\n" +
                "       SET codeCustomsGroup(c) <- code AS STRING[10];\n" +
                "       SET parentCustomsGroup(c) <- customsGroupCode(parent AS STRING[10]);\n" +
                "       SET nameCustomsGroup(c) <- nameCustomsGroup AS VARISTRING[1000];\n" +
                "       SET numberCustomsGroup(c) <- number AS INTEGER;\n" +
                "       SET customsZoneCustomsGroup(c) <- customsZoneName(nameCustomsZone AS STRING[100]);\n" +
                "       SET hasCodeCustomsGroup(c) <- hasCode AS BOOLEAN;\n" +
                "       SET dataRegistrationCustomsGroupDate(c, dateFrom) <- registration AS NUMERIC[10,5];\n" +
                "       SET dataWeightDutyCustomsGroupDate(c, dateFrom) <- weight AS NUMERIC[10,5];\n" +
                "       SET dataPercentDutyCustomsGroupDate(c, dateFrom) <- percent AS NUMERIC[10,5];\n" +
                "       SET dataSupplierVATCustomsGroupDate(c, d) <- valueCurrentVATDefaultValue(vat AS NUMERIC[10,5]) WHERE d == 2001_01_01;\n" +
                "       SET dateFromCustomsGroup(c) <- dateFrom AS DATE;\n" +
                "       SET dateToCustomsGroup(c) <- dateTo AS DATE;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "loadDefaultCustomsGroups 'Загрузить стандартный справочник ТН ВЭД' = ACTION () {\n" +
                "        EXEC loadDefaultCustomsZone('ТАМОЖЕННЫЙ СОЮЗ');\n");
    }

    private void writeBottom(PrintWriter writer) {
        writer.println("} IN loadDefaultGroup;\n" +
                "\n" +
                "EXTEND FORM defaultData\n" +
                "    PROPERTIES() loadDefaultCustomsGroups\n" +
                ";\n" +
                "\n" +
                "EXTEND DESIGN defaultData {\n" +
                "    pane {\n" +
                "        customs {\n" +
                "            ADD PROPERTY(loadDefaultCustomsGroups);\n" +
                "        }\n" +
                "    }\n" +
                "}");
    }
}