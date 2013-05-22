package fdk.region.by.integration.vetraz;

import fdk.integration.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportVetrazActionProperty extends ScriptingActionProperty {

    public ImportVetrazActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {

            Integer numberOfItems = (Integer) getLCP("importNumberItems").read(context);
            Integer numberOfUserInvoices = (Integer) getLCP("importNumberUserInvoices").read(context);

            Object pathObject = getLCP("importVetrazDirectory").read(context);
            String path = pathObject == null ? "" : ((String) pathObject).trim();
            if (!path.isEmpty()) {

                ImportData importData = new ImportData();

                importData.setLegalEntitiesList((getLCP("importLegalEntities").read(context) != null) ?
                        importLegalEntitiesFromDBF(path + "//sprana.dbf") : null);

                importData.setWarehousesList((getLCP("importWarehouses").read(context) != null) ?
                        importWarehousesFromDBF(path + "//sprana.dbf") : null);

                importData.setItemGroupsList((getLCP("importItems").read(context) != null) ?
                        importItemGroupsFromDBF(false) : null);

                importData.setParentGroupsList((getLCP("importItems").read(context) != null) ?
                        importItemGroupsFromDBF(true) : null);

                importData.setItemsList((getLCP("importItems").read(context) != null) ?
                        importItemsFromDBF(path + "//sprmat.dbf", numberOfItems) : null);

                importData.setUserInvoicesList((getLCP("importUserInvoices").read(context) != null) ?
                        importUserInvoicesFromDBF(path + "//sprmat.dbf", path + "//ostt.dbf", numberOfUserInvoices) : null);

                new ImportActionProperty(LM, importData, context).makeImport();
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private List<ItemGroup> importItemGroupsFromDBF(Boolean parents) throws IOException, xBaseJException {
        List<ItemGroup> data = new ArrayList<ItemGroup>();
        String groupTop = "ВСЕ";
        data.add(new ItemGroup(groupTop, parents ? null : groupTop, null));
        return data;
    }

    List<Double> allowedVAT = Arrays.asList(0.0, 9.09, 16.67, 10.0, 20.0, 24.0);

    private List<Item> importItemsFromDBF(String itemsPath, Integer numberOfItems) throws IOException, xBaseJException, ParseException {

        if (!(new File(itemsPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + itemsPath + " не найден");

        List<Item> data = new ArrayList<Item>();

        DBF itemsImportFile = new DBF(itemsPath);
        int totalRecordCount = itemsImportFile.getRecordCount();
        int recordCount = (numberOfItems != null && numberOfItems != 0 && numberOfItems < totalRecordCount) ? numberOfItems : totalRecordCount;

        for (int i = 0; i < recordCount; i++) {
            itemsImportFile.read();

            String k_group = getFieldValue(itemsImportFile, "K_GRUP", "Cp866", null);
            String name = getFieldValue(itemsImportFile, "POL_NAIM", "Cp866", null);
            String idItem = k_group + name;

            String UOM = getFieldValue(itemsImportFile, "K_IZM", "Cp866", null);
            BigDecimal retailVAT = getBigDecimalFieldValue(itemsImportFile, "NDSR", "Cp866", null);
            String manufacturer = getFieldValue(itemsImportFile, "DOPPRIM", "Cp866", null);
            String country = "БЕЛАРУСЬ";
            String tnved = getFieldValue(itemsImportFile, "DPRM1", "Cp866", null);
            Date date = getDateFieldValue(itemsImportFile, "DATPR1", "Cp866", null);
            BigDecimal amountInPack = getBigDecimalFieldValue(itemsImportFile, "N_PER2", "Cp866", null);
            BigDecimal weight = getBigDecimalFieldValue(itemsImportFile, "N_PER3", "Cp866", null);

            if (!idItem.trim().isEmpty())
                data.add(new Item(idItem, "ВСЕ", name, UOM, UOM, UOM, null, null, country, k_group,
                        k_group, date, null, weight, weight, null, allowedVAT.contains(retailVAT.doubleValue()) ? retailVAT : null,
                        null, null, null, null, null, null, idItem, amountInPack, manufacturer, manufacturer, tnved, country));
        }
        return data;
    }

    private List<LegalEntity> importLegalEntitiesFromDBF(String path) throws
            IOException, xBaseJException {

        if (!(new File(path).exists()))
            throw new RuntimeException("Запрашиваемый файл " + path + " не найден");

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        List<LegalEntity> data = new ArrayList<LegalEntity>();

        String nameCountry = "БЕЛАРУСЬ";

        data.add(new LegalEntity("sle", "Стандартная Организация", null, null, null,
                null, null, null, null, null, null, null, null, nameCountry, null, true, null));

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String idLegalEntity = getFieldValue(importFile, "K_ANA", "Cp866", null);
            String name = getFieldValue(importFile, "POL_NAIM", "Cp866", "");
            String address = getFieldValue(importFile, "DPRA1", "Cp866", null);
            String unp = getFieldValue(importFile, "PRIM", "Cp866", null);
            String okpo = getFieldValue(importFile, "DPRIM", "Cp866", null);
            String account = getFieldValue(importFile, "DPRA4", "Cp866", null);
            String[] ownership = getAndTrimOwnershipFromName(name);
            String type = getFieldValue(importFile, "K_VAN", "Cp866", null);
            Boolean isSupplier = "ПС".equals(type);
            if (isSupplier && !name.isEmpty())
                data.add(new LegalEntity(idLegalEntity, ownership[2], address, unp, okpo, null, null, ownership[1],
                        ownership[0], account, null, null, null, nameCountry, isSupplier ? true : null, null, null));
        }
        return data;
    }

    private List<Warehouse> importWarehousesFromDBF(String spranaPath) throws
            IOException, xBaseJException {

        if (!(new File(spranaPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + spranaPath + " не найден");

        DBF importFile = new DBF(spranaPath);
        int recordCount = importFile.getRecordCount();

        List<Warehouse> data = new ArrayList<Warehouse>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String id = getFieldValue(importFile, "K_ANA", "Cp866", null);
            String name = getFieldValue(importFile, "POL_NAIM", "Cp866", "");
            String address = getFieldValue(importFile, "DPRA1", "Cp866", null);
            String type = getFieldValue(importFile, "K_VAN", "Cp866", null);
            Boolean isWarehouse = "СК".equals(type);
            Boolean isSupplier = "ПС".equals(type);

            if (!name.isEmpty()) {
                if (isWarehouse)
                    data.add(new Warehouse("sle", null, id, name, address));
                if (isSupplier)
                    data.add(new Warehouse(id, null, id + "WH", name, address));
            }
        }
        return data;
    }

    private List<UserInvoiceDetail> importUserInvoicesFromDBF(String sprmatPath, String osttPath, Integer numberOfUserInvoices) throws IOException, xBaseJException, ParseException {

        if (!(new File(sprmatPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + sprmatPath + " не найден");

        if (!(new File(osttPath).exists()))
            throw new RuntimeException("Запрашиваемый файл " + osttPath + " не найден");

        DBF importFile = new DBF(sprmatPath);
        int totalRecordCount = importFile.getRecordCount();

        Map<String, Object[]> sprmatMap = new HashMap<String, Object[]>();

        for (int i = 0; i < totalRecordCount; i++) {
            importFile.read();

            String k_mat = getFieldValue(importFile, "K_MAT", "Cp866", "");
            String k_group = getFieldValue(importFile, "K_GRUP", "Cp866", null);
            String name = getFieldValue(importFile, "POL_NAIM", "Cp866", null);
            String idItem = k_group + name;
            String idDocument = getFieldValue(importFile, "POST_DOK", "Cp866", "");
            String idSupplier = getFieldValue(importFile, "K_POST", "Cp866", null);
            Date date = getDateFieldValue(importFile, "D_PRIH", "Cp866", null);
            String descriptionDeclaration = getFieldValue(importFile, "DPRM4", "Cp866", null);
            String certificateText = getFieldValue(importFile, "DPRM6", "Cp866", null);
            String descriptionCompliance = getFieldValue(importFile, "DPRM7", "Cp866", null);
            Date expiryDate = getDateFieldValue(importFile, "D_GODN", "Cp866", null);
            String bin = getFieldValue(importFile, "DPRM9", "Cp866", null);
            if (!k_mat.isEmpty())
                sprmatMap.put(k_mat, new Object[]{idItem, idDocument, idSupplier, date, descriptionDeclaration,
                        certificateText,descriptionCompliance, expiryDate, bin});

        }

        List<UserInvoiceDetail> data = new ArrayList<UserInvoiceDetail>();

        importFile = new DBF(osttPath);
        totalRecordCount = importFile.getRecordCount();
        int recordCount = (numberOfUserInvoices != null && numberOfUserInvoices != 0 && numberOfUserInvoices < totalRecordCount) ? numberOfUserInvoices : totalRecordCount;

        for (int i = 0; i < totalRecordCount; i++) {

            if (data.size() >= recordCount)
                break;

            importFile.read();

            String k_mat = getFieldValue(importFile, "K_MAT", "Cp866", null);
            BigDecimal quantity = getBigDecimalFieldValue(importFile, "N_MAT", "Cp866", null);
            String idWarehouse = getFieldValue(importFile, "K_SKL", "Cp866", "");
            idWarehouse = idWarehouse.isEmpty() ? null : ("СК" + idWarehouse);
            Object[] sprmat = sprmatMap.get(k_mat);

            if (sprmat != null) {

                String idItem = (String) sprmat[0];
                String numberUserInvoice = (String) sprmat[1];
                String seriesUserInvoice = "AA";
                String idSupplier = (String) sprmat[2];
                Date date = (Date) sprmat[3];
                String descriptionDeclaration = (String) sprmat[4];
                String certificateText = (String) sprmat[5];
                String descriptionCompliance = (String) sprmat[6];
                Date expiryDate = (Date) sprmat[7];
                String bin = (String) sprmat[8];

                String numberDeclaration = null;
                Date dateDeclaration = null;
                if (!descriptionDeclaration.isEmpty()) {
                    for (String p : declarationPatterns) {
                        Pattern r = Pattern.compile(p);
                        Matcher m = r.matcher(descriptionDeclaration);
                        if (m.find()) {
                            numberDeclaration = m.group(1).trim();
                            try {
                                dateDeclaration = new Date(DateUtils.parseDate(m.group(2), datePatterns).getTime());
                            } catch (ParseException ignored) {
                            }
                            break;
                        }
                    }
                }

                String numberCompliance = null;
                Date fromDateCompliance = null;
                Date toDateCompliance = null;
                if (!descriptionCompliance.isEmpty()) {
                    for (String p : compliancePatterns) {
                        Pattern r = Pattern.compile(p);
                        Matcher m = r.matcher(descriptionCompliance);
                        if (m.find()) {
                            numberCompliance = m.group(1).trim();
                            try {
                                fromDateCompliance = (m.groupCount()>=2 && !m.group(2).isEmpty()) ? new Date(DateUtils.parseDate(m.group(2), datePatterns).getTime()) : null;
                                toDateCompliance = (m.groupCount() >=3 && !m.group(3).isEmpty()) ? new Date(DateUtils.parseDate(m.group(3), datePatterns).getTime()) : null;
                            } catch (ParseException ignored) {
                            }
                            break;
                        }
                    }
                }


                if (!numberUserInvoice.isEmpty())
                    data.add(new UserInvoiceDetail(seriesUserInvoice + numberUserInvoice + String.valueOf(date),
                            seriesUserInvoice, numberUserInvoice, null, true, k_mat, date, idItem, null, quantity, idSupplier,
                            idWarehouse, idSupplier + "WH", null, null, null, null, null, null, null, certificateText, null,
                            numberDeclaration, dateDeclaration, numberCompliance, fromDateCompliance, toDateCompliance,
                            expiryDate, bin));
            }

        }
        return data;
    }

    String[] declarationPatterns = new String[]{"№?((?:\\d|\\/)*)от(\\d{2}\\.\\d{2}\\.\\d{2,4})"};
    String[] compliancePatterns = new String[]{"(сертификат)","(#)",
                                               "№?((?:\\p{L}|\\d|\\.)*)от(\\d{2}\\.\\d{2}\\.\\d{2})",
                                               "№?\\s?((?:\\p{L}|-|\\d|\\.)*)()\\s?(?:до|по)\\s?(\\d{2}\\.\\d{2}\\.\\d{2,4})",
                                                "№?((?:\\p{L}|\\d|\\.)*)(\\d{2}\\.\\d{2}\\.\\d{2})",
                                               "(\\p{L}{2}-?\\s?(?:\\d|\\.)*)", "№?((?:\\p{L}|-|\\s|\\d|\\.)*)"};
    String[] datePatterns = new String[]{"dd.MM.yy", "dd.MM.yyyy"};


    String[][] ownershipsList = new String[][]{
            {"ОАОТ", "Открытое акционерное общество торговое"},
            {"ОАО", "Открытое акционерное общество"},
            {"СООО", "Совместное общество с ограниченной ответственностью"},
            {"ООО", "Общество с ограниченной ответственностью"},
            {"ОДО", "Общество с дополнительной ответственностью"},
            {"ЗАО", "Закрытое акционерное общество"},
            {"ЧТУП", "Частное торговое унитарное предприятие"},
            {"ЧУТП", "Частное унитарное торговое предприятие"},
            {"ТЧУП", "Торговое частное унитарное предприятие"},
            {"ЧУП", "Частное унитарное предприятие"},
            {"РУП", "Республиканское унитарное предприятие"},
            {"РДУП", "Республиканское дочернее унитарное предприятие"},
            {"УП", "Унитарное предприятие"},
            {"ИП", "Индивидуальный предприниматель"},
            {"СПК", "Сельскохозяйственный производственный кооператив"},
            {"СП", "Совместное предприятие"}};

    private String[] getAndTrimOwnershipFromName(String name) {
        name = name == null ? "" : name;
        String ownershipName = "";
        String ownershipShortName = "";
        for (String[] ownership : ownershipsList) {
            if (name.contains(ownership[0] + " ") || name.contains(" " + ownership[0])) {
                ownershipName = ownership[1];
                ownershipShortName = ownership[0];
                name = name.replace(ownership[0], "");
            }
        }
        return new String[]{ownershipShortName, ownershipName, name};
    }

    private BigDecimal getBigDecimalFieldValue(DBF importFile, String fieldName, String charset, String defaultValue) throws UnsupportedEncodingException {
        return BigDecimal.valueOf(Double.valueOf(getFieldValue(importFile, fieldName, charset, defaultValue)));
    }

    private Date getDateFieldValue(DBF importFile, String fieldName, String charset, Date defaultValue) throws UnsupportedEncodingException, ParseException {
        String dateString = getFieldValue(importFile, fieldName, charset, "");
        return dateString.isEmpty() ? defaultValue : new java.sql.Date(DateUtils.parseDate(dateString, new String[]{"yyyyMMdd"}).getTime());

    }

    private String getFieldValue(DBF importFile, String fieldName, String charset, String defaultValue) throws UnsupportedEncodingException {
        try {
            return new String(importFile.getField(fieldName).getBytes(), charset).trim();
        } catch (xBaseJException e) {
            return defaultValue;
        }
    }

}