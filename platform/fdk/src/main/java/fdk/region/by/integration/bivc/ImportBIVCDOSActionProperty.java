package fdk.region.by.integration.bivc;

import fdk.integration.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.xBaseJException;
import platform.server.classes.ConcreteCustomClass;
import platform.server.integration.*;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.*;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportBIVCDOSActionProperty extends ScriptingActionProperty {

    public ImportBIVCDOSActionProperty(ScriptingLogicsModule LM) {
        super(LM);

        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            String path = (String) getLCP("importBIVCDOSDirectory").read(context);
            Integer numberOfItems = (Integer) getLCP("importBIVCDOSNumberItems").read(context);
            if (path != null && !path.isEmpty()) {
                path = path.trim();
                ImportData importData = new ImportData();

                importData.setNumberOfItemsAtATime((Integer) getLCP("importBIVCDOSNumberItemsAtATime").read(context));

                importData.setItemGroupsList((getLCP("importBIVCDOSGroupItems").read(context) != null) ?
                        importItemGroups(path + "//stg", false) : null);

                importData.setParentGroupsList((getLCP("importBIVCDOSGroupItems").read(context) != null) ?
                        importItemGroups(path + "//stg", true) : null);

                importData.setBanksList((getLCP("importBIVCDOSBanks").read(context) != null) ?
                        importBanks(path + "//swtp") : null);

                importData.setLegalEntitiesList((getLCP("importBIVCDOSLegalEntities").read(context) != null) ?
                        importLegalEntities(path + "//swtp") : null);

                importData.setWarehouseGroupsList((getLCP("importBIVCDOSWarehouses").read(context) != null) ?
                        importWarehouseGroups() : null);

                importData.setWarehousesList((getLCP("importBIVCDOSWarehouses").read(context) != null) ?
                        importWarehouses(path + "//smol", path + "//swtp", path + "//ost") : null);

                importData.setContractsList((getLCP("importBIVCDOSContracts").read(context) != null) ?
                        importContracts(path + "//swtp") : null);

                importData.setItemsList((getLCP("importBIVCDOSItems").read(context) != null) ?
                        importItems(path + "//ost", path + "//sedi", path + "//prc", numberOfItems) : null);

                importData.setUserInvoicesList((getLCP("importBIVCDOSUserInvoices").read(context) != null) ?
                        importUserInvoices(path + "//ost", numberOfItems) : null);

                importData.setImportUserInvoicesPosted(getLCP("importBIVCDOSUserInvoicesPosted").read(context) != null);

                new ImportActionProperty(LM, importData, context).makeImport();

                if (getLCP("importBIVCDOSMag2").read(context) != null)
                    importLegalEntityStock(path + "//magp");

            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


    private List<ItemGroup> importItemGroups(String path, Boolean parents) throws IOException, xBaseJException {

        List<ItemGroup> itemGroupsList = new ArrayList<ItemGroup>();
        List<String> subParentsList = new ArrayList<String>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "cp866"));
        String line;
        String rootID = "ВСЕ";
        itemGroupsList.add(new ItemGroup(rootID, rootID, null));
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^STG")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                if (splittedLine.length > 1) {
                    String parentID = splittedLine.length == 2 ? rootID : splittedLine[1];
                    String groupID = splittedLine.length == 2 ? splittedLine[1] : (parentID + ":" + splittedLine[2]);
                    String subParentID = groupID.substring(0, groupID.length() - 2) + "00";
                    Boolean subParent = splittedLine.length == 3 && !groupID.endsWith("00");

                    String name = reader.readLine().trim();
                    if (!"".equals(name)) {
                        if (!parents) {
                            //sid - name - parentSID(null)
                            itemGroupsList.add(new ItemGroup(groupID, name, null));
                            if (subParent && !subParentsList.contains(subParentID))
                                itemGroupsList.add(new ItemGroup(subParentID, null, null));
                            if (!subParentsList.contains(subParent ? subParentID : parentID))
                                subParentsList.add(subParent ? subParentID : parentID);
                        } else {
                            //sid - name(null) - parentSID
                            itemGroupsList.add(new ItemGroup(groupID, null, subParent ? subParentID : parentID));
                            if (subParent)
                                itemGroupsList.add(new ItemGroup(subParentID, null, parentID));
                        }
                    }
                }
            }
        }
        reader.close();
        return itemGroupsList;
    }

    private List<Bank> importBanks(String path) throws IOException, xBaseJException {

        List<Bank> banksList = new ArrayList<Bank>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "cp866"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = reader.readLine().split(":");
                String mfo = splittedLine.length > 2 ? (splittedLine[2].length() > 3 ? splittedLine[2].substring(splittedLine[2].length() - 3) : splittedLine[2].trim()) : "";
                String bankName = splittedLine.length > 6 ? splittedLine[6].trim() : "";
                String bankAddress = splittedLine.length > 7 ? splittedLine[7].trim() : "";
                String bankID = mfo + bankName;
                if (!bankName.isEmpty() && !bankID.isEmpty())
                    banksList.add(new Bank(bankID, bankName, bankAddress.isEmpty() ? null : bankAddress,
                            null, mfo.isEmpty() ? null : mfo, null));
            }
        }
        reader.close();
        return banksList;
    }

    private List<Item> importItems(String ostPath, String sediPath, String prcPath, Integer numberOfItems) throws IOException, xBaseJException {

        List<Item> itemsList = new ArrayList<Item>();
        String[] patterns = new String[]{
                "\\d:(.*?)(?:\\s|\\.)([\\d\\.]+)\\s?(КГ|Г|ГР|Л|МЛ)?\\.?",
                "\\d:(.*?)" //ловим всё остальное без массы
        };

        Map<String, UOM> uomMap = new HashMap<String, UOM>();
        BufferedReader uomReader = new BufferedReader(new InputStreamReader(new FileInputStream(sediPath), "cp866"));
        String line;
        while ((line = uomReader.readLine()) != null) {
            if (line.startsWith("^SEDI")) {
                String[] splittedLine = line.split("\\(|\\)|,|\"");
                if (splittedLine.length == 2) {
                    String uomLine = uomReader.readLine().trim().replace(",", ".");
                    for (String p : patterns) {
                        Pattern r = Pattern.compile(p);
                        Matcher m = r.matcher(uomLine);
                        if (m.matches()) {
                            String uomName = m.group(1).trim();
                            Integer coefficient = (m.groupCount() >= 3 && m.group(3) != null) ? ((m.group(3).equals("Г")||m.group(3).equals("ГР")||m.group(3).equals("МЛ")) ? 1000 : 1) : 1;
                            Double weight = m.groupCount() < 2 ? null : (Double.parseDouble(m.group(2)) / coefficient);
                            uomMap.put(splittedLine[1], new UOM(uomName,
                                    uomName.length() <= 5 ? uomName : uomName.substring(0, 5), weight, weight));
                            break;
                        }
                    }
                }
            }
        }

        Map<String, Double> retailMarkups = new HashMap<String, Double>();
        uomReader = new BufferedReader(new InputStreamReader(new FileInputStream(prcPath), "cp866"));
        while ((line = uomReader.readLine()) != null) {
            if (line.startsWith("^PRC")) {
                String[] splittedLine = line.split("\\(|\\)");
                if (splittedLine.length == 2) {
                    String[] markupString = uomReader.readLine().split(":");
                    if (markupString.length > 0 && !markupString[0].trim().isEmpty()) {
                        Double markup = Double.parseDouble(markupString[0]);
                        retailMarkups.put(splittedLine[1].replace("\"", ""), markup);
                    }
                }
            }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ostPath), "cp866"));

        String pnt13;
        String pnt48;
        String markupID = null;
        Date date = null;
        String name = null;
        Double baseMarkup = null;
        Double retailVAT = null;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && itemsList.size() >= numberOfItems)
                break;
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                pnt13 = splittedLine.length > 2 ? splittedLine[2] : null;
                pnt48 = splittedLine.length > 3 ? splittedLine[3] : null;
                String extra = splittedLine.length > 4 ? splittedLine[4] : null;
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    markupID = splittedLine.length > 0 ? splittedLine[0].substring(0, 1) : null;
                    String dateField = splittedLine.length > 0 ? splittedLine[0].substring(24, 30) : null;
                    date = dateField == null ? null : new Date(Integer.parseInt(dateField.substring(4, 6)) + 100, Integer.parseInt(dateField.substring(2, 4)), Integer.parseInt(dateField.substring(0, 2)));
                    name = splittedLine.length > 3 ? splittedLine[3] : null;
                    try {
                        baseMarkup = splittedLine.length > 15 ? Double.parseDouble(splittedLine[15].endsWith(".") ?
                                splittedLine[15].substring(0, splittedLine[15].length() - 1) : splittedLine[15]) : null;
                    } catch (NumberFormatException e) {
                        baseMarkup = null;
                    }

                    retailVAT = splittedLine.length > 18 ? Double.parseDouble(splittedLine[18]) : null;
                } else if ("9".equals(extra)) {
                    String groupID = reader.readLine();
                    UOM uom = uomMap.get(groupID.split(":")[0]);
                    itemsList.add(new Item(groupID + ":" + name/*pnt13 + pnt48*/, groupID, name,
                            uom==null ? null : uom.uomName, uom==null ? null : uom.uomShortName,
                            uom==null ? null : uom.uomName,null, null, null, null, date, null,
                            uom==null ? null : uom.netWeight, uom==null ? null : uom.grossWeight, null,
                            retailVAT, null, null, null, null, baseMarkup, retailMarkups.get(markupID)));
                }
            }
        }
        reader.close();
        return itemsList;
    }

    private List<UserInvoiceDetail> importUserInvoices(String path, Integer numberOfItems) throws IOException, ParseException {

        List<UserInvoiceDetail> userInvoiceDetailsList = new ArrayList<UserInvoiceDetail>();

        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "cp866"));

        String dateField = null;
        Date date = null;
        String name = null;
        Double quantity = null;
        Double price = null;
        Double chargePrice = null;
        String numberCompliance = null;
        Timestamp toDateTimeCompliance = null;
        String textCompliance = null;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && userInvoiceDetailsList.size() >= numberOfItems)
                break;
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String warehouse = splittedLine.length > 1 ? splittedLine[1].replace("\"", "") : null;
                String pnt13 = splittedLine.length > 2 ? splittedLine[2].replace("\"", "") : null;
                String pnt48 = splittedLine.length > 3 ? splittedLine[3].replace("\"", "") : null;
                String extra = splittedLine.length > 4 ? splittedLine[4] : null;
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    dateField = splittedLine.length > 0 ? splittedLine[0].substring(24, 30) : null;
                    date = dateField == null ? null : new Date(DateUtils.parseDate(dateField, new String[]{"ddmmyy"}).getTime());
                    name = splittedLine.length > 3 ? splittedLine[3] : null;
                    quantity = splittedLine.length > 7 ? Double.parseDouble(splittedLine[7]) : null;
                    Double sumPrice = Double.parseDouble(splittedLine.length > 2 ? splittedLine[2] : null);
                    String chargePricePercent = splittedLine.length > 19 ? (splittedLine[19].endsWith(".00") ?
                            splittedLine[19].substring(0, splittedLine[19].length() - 3) : splittedLine[19]) : "";
                    chargePrice = chargePricePercent.trim().isEmpty() ? null : sumPrice * Double.parseDouble(chargePricePercent) / 100;
                    price = chargePricePercent.trim().isEmpty() ? sumPrice : (sumPrice - chargePrice);
                } else if ("1".equals(extra)) {
                    textCompliance = reader.readLine();
                    String[] compliance = textCompliance.split(" ДО | ПО ");
                    numberCompliance = compliance.length > 0 ? (compliance[0].trim().length() > 17 ? compliance[0].substring(0, 17) : compliance[0].trim()) : null;
                    if (compliance.length > 1) {
                        compliance[1] = compliance[1].replace("+", "");
                        while (compliance[1].matches(".*\\D"))
                            compliance[1] = compliance[1].substring(0, compliance[1].length() - 1);
                    }
                    try {
                        toDateTimeCompliance = compliance.length > 1 ?
                                new Timestamp(DateUtils.parseDate(compliance[1], new String[]{"dd.mm.yy", "dd.mm.yyyy", "d.mm.yy", "ddmm-yy", "dd\\mm-yy", "ddmm.yy"}).getTime()) : null;
                    } catch (ParseException e) {
                        toDateTimeCompliance = null;
                    }
                } else if ("9".equals(extra)) {
                    String groupID = reader.readLine();
                    if (quantity != null && quantity != 0)
                        userInvoiceDetailsList.add(new UserInvoiceDetail(warehouse + "/" + dateField,
                                "AA", null, true, pnt13 + pnt48, date, groupID + ":" + name/*pnt13 + pnt48*/,
                                quantity, "70020", warehouse, "S70020", price, chargePrice, null, null, numberCompliance,
                                new Timestamp(date.getTime()), toDateTimeCompliance, textCompliance));
                }
            }
        }
        reader.close();
        return userInvoiceDetailsList;
    }

    private List<WarehouseGroup> importWarehouseGroups() {

        List<WarehouseGroup> warehouseGroupsList = new ArrayList<WarehouseGroup>();

        warehouseGroupsList.add(new WarehouseGroup("own", "Собственные склады"));
        warehouseGroupsList.add(new WarehouseGroup("contractor", "Склады контрагентов"));
        return warehouseGroupsList;
    }

    private List<Warehouse> importWarehouses(String smolPath, String swtpPath, String ostPath) throws IOException {

        List<Warehouse> warehousesList = new ArrayList<Warehouse>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ostPath), "cp866"));
        List<String> warehouses = new ArrayList<String>();
        String smol;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                smol = splittedLine.length > 1 ? splittedLine[1].trim().replace("\"", "") : null;
                if (smol != null && !warehouses.contains(smol))
                    warehouses.add(smol);
            }
        }
        reader.close();

        String defaultLegalEntitySID = "sle";
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(smolPath), "cp866"));
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SMOL")) {
                String warehouseID = line.split("\\(|\\)|,")[1].replace("\"", "");
                String[] dataWarehouse = reader.readLine().split(":");
                String name = dataWarehouse.length > 0 ? dataWarehouse[0].trim() : null;
                if (name != null && !"".equals(name) && warehouses.contains(warehouseID)) {
                    warehousesList.add(new Warehouse(defaultLegalEntitySID, "own", warehouseID, name, null));
                }
            }
        }
        reader.close();

        reader = new BufferedReader(new InputStreamReader(new FileInputStream(swtpPath), "cp866"));
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String legalEntityID = splittedLine[1].replace("\"", "");
                String sid = (splittedLine.length == 2 ? splittedLine[1] : splittedLine[2]).replace("\"", "");
                String[] dataLegalEntity = reader.readLine().split(":");
                String name = dataLegalEntity.length > 0 ? dataLegalEntity[0].trim() : null;
                String warehouseAddress1 = dataLegalEntity.length > 9 ? dataLegalEntity[9].trim() : "";
                String warehouseAddress2 = dataLegalEntity.length > 10 ? dataLegalEntity[10].trim() : "";
                String warehouseAddress = (warehouseAddress1 + " " + warehouseAddress2).trim();
                if (name != null && !"".equals(name))
                    warehousesList.add(new Warehouse(legalEntityID, "contractor", "S" + sid, name, warehouseAddress.isEmpty() ? null : warehouseAddress));
            }
        }
        reader.close();

        return warehousesList;
    }

    private List<LegalEntity> importLegalEntities(String path) throws IOException {

        List<LegalEntity> legalEntitiesList = new ArrayList<LegalEntity>();
        String nameCountry = "БЕЛАРУСЬ";

        legalEntitiesList.add(new LegalEntity("sle", "Стандартная Организация", null, null, null,
                null, null, null, null, null, null, null, null, nameCountry, null, true, null));

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "cp866"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                if (splittedLine.length == 2) {
                    String legalEntityID = splittedLine[1].replace("\"", "");
                    splittedLine = reader.readLine().split(":");
                    String name = splittedLine.length > 0 ? splittedLine[0].trim() : "";
                    String mfo = splittedLine.length > 2 ? (splittedLine[2].length() > 3 ? splittedLine[2].substring(splittedLine[2].length() - 3) : splittedLine[2].trim()) : "";
                    String account = splittedLine.length > 3 ? splittedLine[3] : "";
                    String okpo = splittedLine.length > 4 ? splittedLine[4].trim() : "";
                    String unp = splittedLine.length > 5 ? splittedLine[5].trim() : "";
                    String bankName = splittedLine.length > 6 ? splittedLine[6].trim() : "";
                    String address1 = splittedLine.length > 9 ? splittedLine[9].trim() : "";
                    String address2 = splittedLine.length > 10 ? splittedLine[10].trim() : "";
                    String address = (address1 + " " + address2).trim();
                    String bankID = mfo + bankName;
                    if (!name.isEmpty())
                        legalEntitiesList.add(new LegalEntity(legalEntityID, name,
                                address.isEmpty() ? null : address, unp.isEmpty() ? null : unp,
                                okpo.isEmpty() ? null : okpo, null, null, null, null,
                                account.isEmpty() ? null : account, null, null,
                                bankID.isEmpty() ? null : bankID, nameCountry, true, null, true));
                }
            }
        }
        reader.close();
        return legalEntitiesList;
    }

    private List<Contract> importContracts(String path) throws IOException {

        List<Contract> contractsList = new ArrayList<Contract>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "cp866"));
        String line;
        String[] patterns = new String[]{"(.*)ОТ(\\d{2}\\.\\d{2}\\.\\d{2})ДО(\\d{2}\\.\\d{2}\\.\\d{2}).*",
                "()()ДО\\s(\\d{2}-\\d{2}-\\d{2}).*",
                "(.*)\\s?ОТ\\s?(\\d{1,2}\\.?\\d{1,2}\\.?\\d{1,4})\\w?$",
                "()(\\d{1,2}\\/\\d{1,2}\\-\\d{2,4})",
                "(.*?)\\s?(\\d{1,2}\\.?\\/?\\d{1,2}\\.?\\d{2,4})\\w?$",
                "(ЛИЦ.*)\\s(\\d{6})",
                "((?:ДОГ|КОНТР).*)", /*ловим договоры вообще без дат*/
                "(.*)/?\\s?(?:ИП|СПК|ОП)"
        };
        String[] datePatterns = new String[]{"dd-mm-yy", "dd.mm.yy", "dd.mm.yyГ", "ddmmyy", "dd/mm-yyyy", "dd.mm.y", "dd.mmyy"};
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                if (splittedLine.length == 2) {
                    String legalEntity1ID = splittedLine[1].replace("\"", "");
                    String legalEntity2ID = "sle";
                    String contractID1 = legalEntity1ID + "/" + legalEntity2ID;
                    String contractID2 = legalEntity2ID + "/" + legalEntity1ID;
                    splittedLine = reader.readLine().split(":");
                    String contractString = splittedLine.length > 11 ? splittedLine[11].trim() : "";
                    if (!contractString.trim().isEmpty()) {
                        String number = "";
                        Date dateFrom = null;
                        Date dateTo = null;
                        Boolean found = false;
                        for (String p : patterns) {
                            Pattern r = Pattern.compile(p);
                            Matcher m = r.matcher(contractString);
                            if (m.find()) {
                                number = m.group(1).trim();
                                try {
                                    dateFrom = (m.groupCount() >= 2 && !m.group(2).isEmpty()) ? new Date(DateUtils.parseDate(m.group(2), datePatterns).getTime()) : null;
                                    dateTo = (m.groupCount() >= 3 && !m.group(3).isEmpty()) ? new Date(DateUtils.parseDate(m.group(3), datePatterns).getTime()) : null;
                                } catch (ParseException e) {
                                    if (dateFrom == null && m.groupCount() >= 2)
                                        number = m.group(0).trim();
                                    else if (dateTo == null && m.groupCount() >= 3)
                                        number = m.group(0).trim();
                                }
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            contractsList.add(new Contract(contractID1, legalEntity1ID, legalEntity2ID,
                                    number, dateFrom, dateTo));
                            contractsList.add(new Contract(contractID2, legalEntity2ID, legalEntity1ID,
                                    number, dateFrom, null));
                        }
                    }
                }
            }
        }
        reader.close();

        return contractsList;
    }

    private void importLegalEntityStock(String path) throws SQLException, ScriptingErrorLog.SemanticErrorException, IOException {

        List<List<Object>> data = importLegalEntityStockFromFile(path);

        if (data != null) {

            ImportField legalEntityIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
            ImportField warehouseIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
            ImportField dataField = new ImportField(LM.findLCPByCompoundName("mag2LegalEntityStock"));

            ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("legalEntity"),
                    LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityIDField));

            ImportKey<?> warehouseKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("warehouse"),
                    LM.findLCPByCompoundName("externalizableSID").getMapping(warehouseIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(legalEntityKey)));
            props.add(new ImportProperty(warehouseIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(warehouseKey)));
            props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("legalEntityStock").getMapping(warehouseKey),
                    LM.object(LM.findClassByCompoundName("legalEntity")).getMapping(legalEntityKey)));
            props.add(new ImportProperty(dataField, LM.findLCPByCompoundName("mag2LegalEntityStock").getMapping(legalEntityKey, warehouseKey)));

            ImportTable table = new ImportTable(Arrays.asList(legalEntityIDField, warehouseIDField,
                    dataField), data);

            DataSession session = LM.getBL().createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(
                    legalEntityKey, warehouseKey), props);
            service.synchronize(true, false);
            session.apply(LM.getBL());
            session.close();
        }
    }

    private List<List<Object>> importLegalEntityStockFromFile(String path) throws IOException {
        List<List<Object>> data = new ArrayList<List<Object>>();


        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "cp866"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^MAGP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                if (splittedLine.length == 3) {
                    String legalEntityID = splittedLine[1].replace("\"", "");
                    String warehouseID = splittedLine[2].replace("\"", "");
                    String mag2LegalEntityStock = reader.readLine();

                    data.add(Arrays.asList((Object) ("L" + legalEntityID), "WHS" + warehouseID, mag2LegalEntityStock));
                }
            }
        }
        reader.close();
        return data;
    }
}