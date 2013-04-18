package integration;

import fdk.integration.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.xBaseJException;
import platform.interop.action.MessageClientAction;
import platform.server.ServerLoggers;
import platform.server.classes.ConcreteCustomClass;
import platform.server.integration.*;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportBIVCDOSActionProperty extends ScriptingActionProperty {

    Map<String, String[]> suppliersFastMap;

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
            Integer numberOfUserInvoices = (Integer) getLCP("importBIVCDOSNumberUserInvoices").read(context);
            Date startDate = (Date) getLCP("importBIVCDOSStartDate").read(context);
            if (path != null && !path.isEmpty()) {
                path = path.trim();
                ImportData importData = new ImportData();

                importData.setNumberOfItemsAtATime((Integer) getLCP("importBIVCDOSNumberItemsAtATime").read(context));

                importData.setNumberOfUserInvoicesAtATime((Integer) getLCP("importBIVCDOSNumberUserInvoicesAtATime").read(context));

                importData.setItemGroupsList((getLCP("importBIVCDOSGroupItems").read(context) != null) ?
                        importItemGroups(path + "//STG", false) : null);

                importData.setParentGroupsList((getLCP("importBIVCDOSGroupItems").read(context) != null) ?
                        importItemGroups(path + "//STG", true) : null);

                importData.setBanksList((getLCP("importBIVCDOSBanks").read(context) != null) ?
                        importBanks(path + "//SWTP") : null);

                importData.setLegalEntitiesList((getLCP("importBIVCDOSLegalEntities").read(context) != null) ?
                        importLegalEntities(path + "//SWTP") : null);

                importData.setEmployeesList((getLCP("importBIVCDOSEmployees").read(context) != null) ?
                        importEmployees(path + "//SWTP") : null);

                importData.setWarehouseGroupsList((getLCP("importBIVCDOSWarehouses").read(context) != null) ?
                        importWarehouseGroups() : null);

                importData.setWarehousesList((getLCP("importBIVCDOSWarehouses").read(context) != null) ?
                        importWarehouses(path + "//SMOL", path + "//SWTP", path + "//OST", startDate) : null);

                importData.setContractsList((getLCP("importBIVCDOSContracts").read(context) != null) ?
                        importContracts(path + "//SWTP") : null);

                importData.setWaresList((getLCP("importBIVCDOSWares").read(context) != null) ?
                        importWares(path + "//OST", path + "//SEDI") : null);

                importData.setItemsList((getLCP("importBIVCDOSItems").read(context) != null) ?
                        importItems(path + "//OST", path + "//SEDI", path + "//PRC", numberOfItems, context) : null);

                importData.setUserInvoicesList((getLCP("importBIVCDOSUserInvoices").read(context) != null) ?
                        importUserInvoices(path + "//SWTP", path + "//OST", path + "//SEDI", startDate, numberOfUserInvoices, context) : null);

                importData.setImportUserInvoicesPosted(getLCP("importBIVCDOSUserInvoicesPosted").read(context) != null);

                ImportActionProperty imp = new ImportActionProperty(LM, importData, context);
                imp.showManufacturingPrice = true;
                imp.showWholesalePrice = true;
                imp.makeImport();

                if (getLCP("importBIVCDOSMag2").read(context) != null)
                    importLegalEntityStock(context, path + "//SWTP", path + "//MAGP");

                if ((getLCP("importBIVCDOSItems").read(context) != null))
                    importSotUOM(context, path + "//SEDI", path + "//OST", numberOfItems);

                if ((getLCP("importBIVCDOSUserInvoices").read(context) != null))
                    importSOTUserInvoices(context, path + "//SEDI", path + "//OST", startDate, numberOfUserInvoices);

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
                    if (parentID.length() == 2)
                        parentID = "0" + parentID;
                    String groupID = splittedLine.length == 2 ? splittedLine[1] : (parentID + ":" + splittedLine[2]);
                    if (groupID.length() == 2)
                        groupID = "0" + groupID;
                    String subParentID = groupID.substring(0, groupID.length() - 2) + "00";
                    Boolean subParent = splittedLine.length == 3 && !groupID.endsWith("00");

                    String name = reader.readLine().trim();
                    //if (!"".equals(name)) {
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
                    //}
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

    private List<Ware> importWares(String ostPath, String sediPath) throws IOException, xBaseJException {

        List<Ware> waresList = new ArrayList<Ware>();
        Map<String, UOM> uomMap = getUOMMap(sediPath);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ostPath), "cp866"));

        String pnt13 = null;
        String pnt48 = null;
        String uomID = null;
        Double price = null;
        String name = null;
        String wareID;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                pnt13 = splittedLine.length > 2 ? splittedLine[2].trim().replace("\"", "") : null;
                pnt48 = splittedLine.length > 3 ? splittedLine[3].trim().replace("\"", "") : null;
                String extra = splittedLine.length > 4 ? splittedLine[4] : null;
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    price = Double.parseDouble(splittedLine.length > 2 ? splittedLine[2] : null);
                    name = splittedLine.length > 3 ? splittedLine[3] : null;
                } else if ("9".equals(extra)) {
                    String groupID = reader.readLine();
                    if (groupID.startsWith("929")) {
                        UOM uom = uomMap.get(uomID);
                        String uomFullName = uom == null ? "" : uom.uomFullName;
                        wareID = groupID + ":" + name + uomFullName;
                        waresList.add(new Ware(pnt13 + pnt48, name, price));
                    }
                }
            }
        }
        reader.close();
        return waresList;
    }

    private Map<String, UOM> getUOMMap(String sediPath) throws IOException {
        Map<String, UOM> uomMap = new HashMap<String, UOM>();
        String[] patterns = new String[]{
                "\\d:(.*?)(?:\\s|\\.)([\\d\\.]+)\\s?(КГ|Г|ГР|Л|МЛ)?\\.?",
                "\\d:(.*?)" //ловим всё остальное без массы
        };

        BufferedReader uomReader = new BufferedReader(new InputStreamReader(new FileInputStream(sediPath), "cp866"));
        String line;
        while ((line = uomReader.readLine()) != null) {
            if (line.startsWith("^SEDI")) {
                String[] splittedLine = line.replace("\"", "").split("\\(|\\)|,");
                if (splittedLine.length == 2) {
                    String uomLine = uomReader.readLine().trim();
                    String uomOriginalName = uomLine.replace(",", ".");
                    for (String p : patterns) {
                        Pattern r = Pattern.compile(p);
                        Matcher m = r.matcher(uomOriginalName);
                        if (m.matches()) {
                            String uomName = m.group(1).trim();
                            Integer coefficient = (m.groupCount() >= 3 && m.group(3) != null) ? ((m.group(3).equals("Г") || m.group(3).equals("ГР") || m.group(3).equals("МЛ")) ? 1000 : 1) : 1;
                            Double weight = m.groupCount() < 2 ? null : (Double.parseDouble(m.group(2)) / coefficient);
                            uomMap.put(splittedLine[1], new UOM(uomLine, uomName,
                                    uomName.length() <= 5 ? uomName : uomName.substring(0, 5), weight, weight));
                            break;
                        }
                    }
                }
            }
        }
        return uomMap;
    }

    private List<Item> importItems(String ostPath, String sediPath, String prcPath, Integer numberOfItems, ExecutionContext context) throws IOException, xBaseJException, ParseException {

        List<Item> itemsList = new ArrayList<Item>();
        Map<String, UOM> uomMap = getUOMMap(sediPath);
        Map<String, Double> retailMarkups = new HashMap<String, Double>();

        BufferedReader prcReader = new BufferedReader(new InputStreamReader(new FileInputStream(prcPath), "cp866"));
        String line;
        while ((line = prcReader.readLine()) != null) {
            if (line.startsWith("^PRC")) {
                String[] splittedLine = line.split("\\(|\\)");
                if (splittedLine.length == 2) {
                    String[] markupString = prcReader.readLine().split(":");
                    if (markupString.length > 0 && !markupString[0].trim().isEmpty()) {
                        Double markup = Double.parseDouble(markupString[0]);
                        retailMarkups.put(splittedLine[1].replace("\"", ""), markup);
                    }
                }
            }
        }

        String warePattern = "(\\d{8})\\s?(.*)";

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ostPath), "cp866"));

        Set<String> itemIDs = new HashSet<String>();

        String pnt13;
        String pnt48;
        String markupID = null;
        String uomID = null;
        Date date = null;
        String name = null;
        String nameID = null;
        String wareID = null;
        Double baseMarkup = null;
        Double retailVAT = null;
        Double packAmount = null;
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
                    uomID = splittedLine.length > 0 ? splittedLine[0].substring(15, 18) : null;
                    String dateField = splittedLine.length > 0 ? splittedLine[0].substring(24, 30) : null;
                    date = dateField == null ? null : new Date(DateUtils.parseDate(dateField, new String[]{"ddmmyy"}).getTime());
                    name = splittedLine.length > 3 ? splittedLine[3] : null;
                    nameID = name;
                    packAmount = null;
                    Pattern rPack = Pattern.compile(".*(?:\\\\|\\/)(\\d+)(?:\\\\|\\/)?");
                    Matcher mPack = rPack.matcher(name);
                    if (mPack.matches()) {
                        Double value = Double.parseDouble(mPack.group(1));
                        packAmount = value <= 1000 ? value : null;
                    }
                    wareID = null;
                    if (name != null) {
                        Pattern r = Pattern.compile(warePattern);
                        Matcher m = r.matcher(name);
                        if (m.matches()) {
                            wareID = m.group(1).trim();
                            name = m.group(2).trim();
                        }
                    }

                    try {
                        baseMarkup = splittedLine.length > 15 ? Double.parseDouble(splittedLine[15].endsWith(".") ?
                                splittedLine[15].substring(0, splittedLine[15].length() - 1) : splittedLine[15]) : null;
                    } catch (NumberFormatException e) {
                        baseMarkup = null;
                    }

                    retailVAT = splittedLine.length > 18 ? Double.parseDouble(splittedLine[18]) : 20;
                    //Так как ещё остались товары со старым НДС 18%
                    retailVAT = (retailVAT == 18) ? 20 : retailVAT;
                } else if ("9".equals(extra)) {
                    String groupID = reader.readLine();
                    if (groupID.split(":")[0].length() == 2)
                        groupID = "0" + groupID;
                    UOM uom = uomMap.get(uomID);
                    String uomFullName = uom == null ? "" : uom.uomFullName;
                    String itemID = /*pnt13 + pnt48*/groupID + ":" + nameID + uomFullName;
                    if (!groupID.startsWith("929") && !itemIDs.contains(itemID)) {
                        itemIDs.add(itemID);
                        itemsList.add(new Item(itemID, groupID, name,
                                uom == null ? null : uom.uomName, uom == null ? null : uom.uomShortName,
                                uom == null ? null : uom.uomName, null, null, null, null, itemID, date, null,
                                uom == null ? null : uom.netWeight, uom == null ? null : uom.grossWeight, null,
                                retailVAT, wareID, null, null, null, baseMarkup, retailMarkups.get(markupID),
                                itemID, packAmount));
                    }
                }
            }
        }
        reader.close();

        context.requestUserInteraction(new MessageClientAction("Кол-во товаров :" + itemIDs.size(), "Импорт товаров"));
        return itemsList;
    }

    private boolean isCorrectUserInvoiceDetail(Double quantity, Date startDate, Date date, String groupID) throws ParseException {
        Boolean correctDate = startDate != null && (date != null && startDate.before(date));
        Boolean correctQuantity = quantity != null && quantity != 0;
        Boolean correctGroupID = groupID == null || !groupID.startsWith("929");
        return (correctDate || correctQuantity) || correctGroupID;
    }

    List<String> ownershipsList = Arrays.asList("НАУЧНО ПРОИЗ.ОБЩЕСТВО С ДОПОЛ ОТВЕТ.",
            "ОАОТ", "ОАО", "СООО", "ООО", "ОДО", "ЗАО", "ЧУТПП", "ЧТУП", "ЧУТП", "ТЧУП", "ЧУП", "РУП", "РДУП", "УП", "ИП", "СПК", "СП");
    List<String> patterns = Arrays.asList("КАЧ(\\.)?(УД\\.)?(N|№)?(Б\\/Н)?\\s?((\\d|\\/|-|\\.)+\\s)?(12\\s)?(ОТ|ЛТ)?\\s?(\\d|\\.)+Г?",
            "((К|У|R)(\\/|\\s)?(У|К|У|E))(\\s)?(\\p{L}{2})?(\\s)?(N|№)?(((\\d|\\/|\\\\)+\\s)|(Б\\/Н\\s))?(СМ|А)?(ОТ)?(\\s)?(\\d|\\.)+Г?");

    private String trimWarehouseSID(String warehouseSID) {
        while (warehouseSID.startsWith("0"))
            warehouseSID = warehouseSID.substring(1);
        return warehouseSID;
    }

    private List<UserInvoiceDetail> importUserInvoices(String swtpPath, String ostPath, String sediPath, Date startDate, Integer numberOfItems,
                                                       ExecutionContext context) throws IOException, ParseException {

        Set<String> suppliers = new HashSet<String>();
        Map<String, String[]> suppliersMap = new HashMap<String, String[]>();
        suppliersFastMap = new HashMap<String, String[]>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(swtpPath), "cp866"));
        Map<String, String> warehouseLegalEntityMap = new HashMap<String, String>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                if (splittedLine.length >= 2) {
                    String legalEntityID = splittedLine[1].replaceAll("\"", "");
                    String warehouseID = trimWarehouseSID((splittedLine.length == 2 ? splittedLine[1] : splittedLine[2]).replace("\"", ""));
                    if (warehouseLegalEntityMap.containsKey(warehouseID))
                        legalEntityID = warehouseLegalEntityMap.get(warehouseID);
                    else
                        warehouseLegalEntityMap.put(warehouseID, legalEntityID);
                    splittedLine = reader.readLine().split(":");
                    String name = splittedLine.length > 0 ? splittedLine[0].trim().replace("\u007F", "") : "";
                    name = name.replaceAll("\"|\'", "").replaceAll("-|\\*", " ");
                    Boolean filtered = false;
                    for (String filter : employeeFilters) {
                        if (name.startsWith(filter)) {
                            filtered = true;
                            break;
                        }
                    }
                    if (!name.isEmpty() && !filtered) {
                        for (String ownership : ownershipsList) {
                            if (name.contains(ownership))
                                name = name.replace(ownership, "");
                        }
                        if (!suppliers.contains(warehouseID)) {
                            suppliers.add(warehouseID);
                            suppliersMap.put(name, new String[]{legalEntityID, "S" + warehouseID});
                        }
                    }
                }
            }
        }
        reader.close();

        Map<String, Double> totalSumWarehouse = new HashMap<String, Double>();

        List<UserInvoiceDetail> userInvoiceDetailsList = new ArrayList<UserInvoiceDetail>();
        Map<String, UOM> uomMap = getUOMMap(sediPath);

        reader = new BufferedReader(new InputStreamReader(new FileInputStream(ostPath), "cp866"));

        String uomID = null;
        Date date = null;
        String dateField = null;
        String name = null;
        String supplierID = null;
        String supplierWarehouseID = null;
        Double quantity = null;
        Double price = null;
        Double chargePrice = null;
        Double manufacturingPrice = null;
        Double wholesalePrice = null;
        Double wholesaleMarkup = null;
        String textCompliance = null;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && userInvoiceDetailsList.size() >= numberOfItems)
                break;
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String customerWarehouseID = splittedLine.length > 1 ? trimWarehouseSID(splittedLine[1].replace("\"", "")) : null;
                String pnt13 = splittedLine.length > 2 ? splittedLine[2].replace("\"", "") : null;
                String pnt48 = splittedLine.length > 3 ? splittedLine[3].replace("\"", "") : null;
                String extra = splittedLine.length > 4 ? splittedLine[4] : null;
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    uomID = splittedLine.length > 0 ? splittedLine[0].substring(15, 18) : null;
                    String date1Field = splittedLine.length > 0 ? splittedLine[0].substring(18, 24) : null;
                    Date date1 = (date1Field == null || date1Field.equals("000000")) ? null : new Date(DateUtils.parseDate(date1Field, new String[]{"ddMMyy"}).getTime());
                    String date2Field = splittedLine.length > 0 ? splittedLine[0].substring(24, 30) : null;
                    Date date2 = (date2Field == null || date2Field.equals("000000")) ? null : new Date(DateUtils.parseDate(date2Field, new String[]{"ddMMyy"}).getTime());
                    Date currentDate = new Date(System.currentTimeMillis());
                    String currentDateField = new SimpleDateFormat("dd/MM/yy").format(currentDate);
                    dateField = date1 == null ? (date2 == null ? currentDateField : date2Field) : (date2 == null ? date1Field : (date1.after(date2) ? date1Field : date2Field));
                    date = date1 == null ? (date2 == null ? currentDate : date2) : (date2 == null ? date1 : date1.after(date2) ? date1 : date2);
                    name = splittedLine.length > 3 ? splittedLine[3] : null;

                    String supplierString = splittedLine.length > 5 ? splittedLine[5] : null;
                    if (supplierString != null && !supplierString.isEmpty()) {
                        textCompliance = supplierString;
                        supplierID = null;
                        supplierWarehouseID = null;
                        if (suppliersFastMap.containsKey(supplierString)) {
                            String[] value = suppliersFastMap.get(supplierString);
                            supplierID = warehouseLegalEntityMap.containsKey(value[1]) ? warehouseLegalEntityMap.get(value[1]) : value[0];
                            supplierWarehouseID = value[1];
                        } else {
                            String supplierName = supplierString.replaceAll("\"|\'", "").replaceAll("-|\\*", " ");
                            for (String pattern : patterns)
                                supplierName = supplierName.replaceAll(pattern, "");
                            for (String ownership : ownershipsList) {
                                if (supplierName.contains(ownership))
                                    supplierName = supplierName.replace(ownership, "");
                            }
                            int matchesMax = 0;
                            for (Map.Entry<String, String[]> entry : suppliersMap.entrySet()) {
                                int matchesCount = 0;
                                for (String entryPart : entry.getKey().split("\\s|\\.")) {
                                    if (entryPart.length() > 3 && supplierName.contains(entryPart))
                                        matchesCount += entryPart.length();
                                }
                                if (matchesCount > matchesMax) {
                                    matchesMax = matchesCount;
                                    supplierID = warehouseLegalEntityMap.containsKey(entry.getValue()[1]) ? warehouseLegalEntityMap.get(entry.getValue()[1]) : entry.getValue()[0];
                                    supplierWarehouseID = entry.getValue()[1];
                                }
                            }
                            if (matchesMax == 0 || ((matchesMax > 0) && (matchesMax < 6) && (matchesMax < supplierName.length() / 4))) {
                                //ServerLoggers.systemLogger.info(name + ":   " + supplierName);
                                supplierID = "ds";
                                supplierWarehouseID = "dsw";
                            } else
                                suppliersFastMap.put(supplierString, new String[]{supplierID, supplierWarehouseID});
                        }
                    } else {
                        supplierID = "ds";
                        supplierWarehouseID = "dsw";
                    }
                    quantity = splittedLine.length > 7 ? Double.parseDouble(splittedLine[7]) : null;
                    Double sumPrice = Double.parseDouble(splittedLine.length > 2 ? splittedLine[2] : null);
                    sumPrice = sumPrice == null ? null : ((double) (Math.round(sumPrice * 100))) / 100;

                    Double chargePricePercent = splittedLine.length > 19 ? readPercent(splittedLine[19]) : 0.0;
                    try {
                        chargePrice = (double) Math.round((sumPrice * chargePricePercent) / (100 + chargePricePercent) * 100) / 100;
                    } catch (NumberFormatException e) {
                        chargePrice = 0.0;
                    }
                    manufacturingPrice = sumPrice - chargePrice;

                    Double manufacturingPercent = splittedLine.length > 17 ? readPercent(splittedLine[17]) : 0.0;
                    price = manufacturingPrice * (100 - manufacturingPercent) / 100;

                    wholesaleMarkup = splittedLine.length > 15 ? readPercent(splittedLine[15]) : 0.0;
                    wholesalePrice = sumPrice * (100 + wholesaleMarkup) / 100;
                } else if ("1".equals(extra)) {
                    String extraCompliance = reader.readLine();
                    if (extraCompliance != null && !extraCompliance.isEmpty()) {
                        if (textCompliance == null || textCompliance.isEmpty())
                            textCompliance = extraCompliance;
                        else
                            textCompliance += " " + extraCompliance;
                    }
                } else if ("9".equals(extra)) {
                    String groupID = reader.readLine();
                    if (groupID.split(":")[0].length() == 2)
                        groupID = "0" + groupID;
                    UOM uom = uomMap.get(uomID);
                    String uomFullName = uom == null ? "" : uom.uomFullName;
                    Boolean isWare = groupID.startsWith("929");
                    String itemID = isWare ? (pnt13 + pnt48) : (groupID + ":" + name + uomFullName);
                    if (isCorrectUserInvoiceDetail(quantity, startDate, date, groupID)) {
                        userInvoiceDetailsList.add(new UserInvoiceDetail(supplierWarehouseID + "/" + customerWarehouseID + "/" + dateField,
                                "AA", null, true, supplierWarehouseID + "/" + customerWarehouseID + "/" + dateField + "/" + pnt13 + pnt48,
                                date, itemID, isWare, quantity, supplierID, customerWarehouseID, supplierWarehouseID, price, chargePrice,
                                manufacturingPrice, wholesalePrice, wholesaleMarkup, null, null, textCompliance, null));
                        Double sum = ((double) (Math.round(((price + (chargePrice == null ? 0 : chargePrice)) * quantity) * 100))) / 100;
                        Double subtotal = totalSumWarehouse.get(customerWarehouseID);
                        if (subtotal == null)
                            totalSumWarehouse.put(customerWarehouseID, sum);
                        else
                            totalSumWarehouse.put(customerWarehouseID, sum + subtotal);
                    }
                }
            }
        }
        reader.close();
        String message = "Кол-во позиций :" + userInvoiceDetailsList.size() + "\r\n";
        for (Map.Entry<String, Double> entry : totalSumWarehouse.entrySet())
            message += entry.getKey() + ": " + NumberFormat.getNumberInstance().format(entry.getValue()) + "\r\n";
        context.requestUserInteraction(new MessageClientAction(message, "Общая сумма"));
        return userInvoiceDetailsList;
    }

    private Double readPercent(String percentString) {
        if (percentString == null) return null;
        if (percentString.endsWith(".00"))
            percentString = percentString.substring(0, percentString.length() - 3);
        Double result = 0.0;
        try {
            result = Double.parseDouble(percentString);
        } catch (Exception e) {
        }
        return result;
    }

    private List<WarehouseGroup> importWarehouseGroups() {

        List<WarehouseGroup> warehouseGroupsList = new ArrayList<WarehouseGroup>();

        warehouseGroupsList.add(new WarehouseGroup("own", "Собственные склады"));
        warehouseGroupsList.add(new WarehouseGroup("contractor", "Склады контрагентов"));
        return warehouseGroupsList;
    }

    List<String> employeeFilters = Arrays.asList("БУХГ.", "ВЕД.ЮРИСТ", "ВОДИТЕЛЬ",
            "ВОД.Л/АВ", "ВОД.ЭЛ/КАРЫ", "ВОД.", "ВОДИТ.АВТ.", "ГР.", "ГРУЗЧ.", "ГРУЗЧИК",
            "ДИСПЕТЧЕР", "ЗАВ.СКЛ.", "ЗАМ.ДИРЕКТОРА", "КЛ.", "КЛАД.", "КЛАДОВ.", "КЛАДОВЩИК",
            "КОЧЕГАР", "МЕХАНИК", "ОПЕРАТОР", "ПРОД.", "ПРОДАВ.", "ПРОДАВЕЦ", "СТР.", "СТ.КЛАД.", "ТОВАРОВЕД",
            "УБОРЩ.", "УБОРЩИЦА", "ЭКОН.-ТОВАР.", "ЭКОН.", "ЭКОНОМИСТ", "ЭКСП.", "ЭКСПЕД.",
            "ЭКСПЕДИТОР", "ЭЛЕКТР.", "ЭЛЕКТРИК");

    private List<Warehouse> importWarehouses(String smolPath, String swtpPath, String ostPath, Date startDate) throws IOException, ParseException {

        List<Warehouse> warehousesList = new ArrayList<Warehouse>();

        warehousesList.add(new Warehouse("ds", "own", "dsw", "Стандартный склад", null));

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ostPath), "cp866"));
        Set<String> warehousesOST = new HashSet<String>();
        String smol;
        Date date = null;
        Double quantity = 0.0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                smol = splittedLine.length > 1 ? trimWarehouseSID(splittedLine[1].trim().replace("\"", "")) : null;
                String extra = splittedLine.length > 4 ? splittedLine[4] : null;
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    String date1Field = splittedLine.length > 0 ? splittedLine[0].substring(18, 24) : null;
                    Date date1 = date1Field == null ? null : new Date(DateUtils.parseDate(date1Field, new String[]{"ddMMyy"}).getTime());
                    String date2Field = splittedLine.length > 0 ? splittedLine[0].substring(24, 30) : null;
                    Date date2 = date2Field == null ? null : new Date(DateUtils.parseDate(date2Field, new String[]{"ddMMyy"}).getTime());
                    date = date1 == null ? date2 : (date2 == null ? date1 : date1.after(date2) ? date1 : date2);
                    quantity = splittedLine.length > 7 ? Double.parseDouble(splittedLine[7]) : 0;
                }
                if (smol != null && isCorrectUserInvoiceDetail(quantity, startDate, date, null))
                    warehousesOST.add(smol);
            }
        }
        reader.close();

        String defaultLegalEntitySID = "sle";
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(smolPath), "cp866"));
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SMOL")) {
                String warehouseID = trimWarehouseSID(line.split("\\(|\\)|,")[1].replace("\"", ""));
                String[] dataWarehouse = reader.readLine().split(":");
                String name = dataWarehouse.length > 0 ? dataWarehouse[0].trim() : null;
                if (name != null && !"".equals(name) && warehousesOST.contains(warehouseID)) {
                    warehousesList.add(new Warehouse(defaultLegalEntitySID, "own", warehouseID, name, null));
                }
            }
        }
        reader.close();

        reader = new BufferedReader(new InputStreamReader(new FileInputStream(swtpPath), "cp866"));
        Set<String> warehousesSWTP = new HashSet<String>();
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String legalEntityID = splittedLine[1].replace("\"", "");
                String warehouseID = trimWarehouseSID((splittedLine.length == 2 ? splittedLine[1] : splittedLine[2]).replace("\"", ""));
                String[] dataLegalEntity = reader.readLine().split(":");
                String name = dataLegalEntity.length > 0 ? dataLegalEntity[0].trim() : "";
                String warehouseAddress1 = dataLegalEntity.length > 9 ? dataLegalEntity[9].trim() : "";
                String warehouseAddress2 = dataLegalEntity.length > 10 ? dataLegalEntity[10].trim() : "";
                String warehouseAddress = (warehouseAddress1 + " " + warehouseAddress2).trim();
                Boolean filtered = false;
                for (String filter : employeeFilters) {
                    if (name.startsWith(filter)) {
                        filtered = true;
                        break;
                    }
                }
                if (!name.trim().isEmpty() && !filtered && !warehousesSWTP.contains(warehouseID)) {
                    warehousesList.add(new Warehouse(legalEntityID, "contractor", "S" + warehouseID, name, warehouseAddress.isEmpty() ? null : warehouseAddress));
                    warehousesSWTP.add(warehouseID);
                }
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
        legalEntitiesList.add(new LegalEntity("ds", "Стандартный поставщик", null, null, null,
                null, null, null, null, null, null, null, null, nameCountry, true, null, null));

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "cp866"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                if (splittedLine.length == 2) {
                    String legalEntityID = splittedLine[1].replace("\"", "");
                    splittedLine = reader.readLine().split(":");
                    String name = splittedLine.length > 0 ? splittedLine[0].trim().replace("\u007F", "") : "";
                    String mfo = splittedLine.length > 2 ? (splittedLine[2].length() > 3 ? splittedLine[2].substring(splittedLine[2].length() - 3) : splittedLine[2].trim()) : "";
                    String account = splittedLine.length > 3 ? splittedLine[3] : "";
                    String okpo = splittedLine.length > 4 ? splittedLine[4].trim() : "";
                    String unp = splittedLine.length > 5 ? splittedLine[5].trim() : "";
                    String bankName = splittedLine.length > 6 ? splittedLine[6].trim() : "";
                    String address1 = splittedLine.length > 9 ? splittedLine[9].trim() : "";
                    String address2 = splittedLine.length > 10 ? splittedLine[10].trim() : "";
                    String address = (address1 + " " + address2).trim();
                    String bankID = mfo + bankName;
                    Boolean filtered = false;
                    for (String filter : employeeFilters) {
                        if (name.startsWith(filter)) {
                            filtered = true;
                            break;
                        }
                    }
                    if (!name.isEmpty() && !filtered)
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

    private List<Employee> importEmployees(String path) throws IOException {

        List<Employee> employeesList = new ArrayList<Employee>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "cp866"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                if (splittedLine.length == 2) {
                    String legalEntityID = splittedLine[1].replace("\"", "");
                    splittedLine = reader.readLine().split(":");
                    String name = splittedLine.length > 0 ? splittedLine[0].trim() : "";
                    for (String filter : employeeFilters) {
                        if (name.startsWith(filter)) {
                            String[] fullName = name.replaceFirst(filter, "").trim().split(" ");
                            String firstName = "";
                            for (int i = 1; i < fullName.length; i++)
                                firstName += fullName[i] + " ";
                            employeesList.add(new Employee(legalEntityID, firstName.trim(), fullName[0], filter));
                            break;
                        }
                    }
                }
            }
        }
        reader.close();
        return employeesList;
    }


    private List<Contract> importContracts(String path) throws IOException {

        String shortNameCurrency = "BLR";

        List<Contract> contractsList = new ArrayList<Contract>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "cp866"));
        String line;
        String[] patterns = new String[]{"(.*)ОТ(\\d{2}\\.\\d{2}\\.\\d{2})ДО(\\d{2}\\.\\d{2}\\.\\d{2}).*",
                "()()ДО\\s(\\d{2}-\\d{2}-\\d{2}).*",
                "(.*)\\s?ОТ\\s?(\\d{1,2}\\.?\\d{1,2}\\.?\\d{1,4})(?:Г|Н)?$",
                "()(\\d{1,2}\\/\\d{1,2}\\-\\d{2,4})",
                "(.*?)\\s?(\\d{1,2}\\.?\\/?\\d{1,2}\\.?\\d{2,4})Г?$",
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
                                    number, dateFrom, dateTo, shortNameCurrency));
                            contractsList.add(new Contract(contractID2, legalEntity2ID, legalEntity1ID,
                                    number, dateFrom, null, shortNameCurrency));
                        }
                    }
                }
            }
        }
        reader.close();

        return contractsList;
    }

    private void importLegalEntityStock(ExecutionContext context, String swtpPath, String magpPath) throws SQLException, ScriptingErrorLog.SemanticErrorException, IOException {

        List<List<Object>> data = importLegalEntityStockFromFile(swtpPath, magpPath);

        if (data != null) {

            ImportField legalEntityIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
            ImportField warehouseIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
            ImportField dataField = new ImportField(LM.findLCPByCompoundName("mag2LegalEntityStock"));

            ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                    LM.findLCPByCompoundName("externalizableSID").getMapping(legalEntityIDField));

            ImportKey<?> warehouseKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Warehouse"),
                    LM.findLCPByCompoundName("externalizableSID").getMapping(warehouseIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(legalEntityIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(legalEntityKey)));
            props.add(new ImportProperty(warehouseIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(warehouseKey)));
            props.add(new ImportProperty(dataField, LM.findLCPByCompoundName("mag2LegalEntityStock").getMapping(legalEntityKey, warehouseKey)));

            ImportTable table = new ImportTable(Arrays.asList(legalEntityIDField, warehouseIDField,
                    dataField), data);

            DataSession session = context.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(
                    legalEntityKey, warehouseKey), props);
            service.synchronize(true, false);
            session.apply(context.getBL());
            session.close();
        }
    }

    private List<List<Object>> importLegalEntityStockFromFile(String swtpPath, String magpPath) throws IOException {
        Set<String> warehousesSWTP = new HashSet<String>();
        List<List<Object>> data = new ArrayList<List<Object>>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(swtpPath), "cp866"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String sid = (splittedLine.length == 2 ? splittedLine[1] : splittedLine[2]).replace("\"", "");
                String[] dataLegalEntity = reader.readLine().split(":");
                String name = dataLegalEntity.length > 0 ? dataLegalEntity[0].trim() : "";
                Boolean filtered = false;
                for (String filter : employeeFilters) {
                    if (name.startsWith(filter)) {
                        filtered = true;
                        break;
                    }
                }
                if (!name.trim().isEmpty() && !filtered && !warehousesSWTP.contains(sid)) {
                    warehousesSWTP.add(sid);
                }
            }
        }
        reader.close();

        reader = new BufferedReader(new InputStreamReader(new FileInputStream(magpPath), "cp866"));
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^MAGP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                if (splittedLine.length == 3) {
                    String legalEntityID = splittedLine[1].replace("\"", "");
                    String warehouseID = trimWarehouseSID(splittedLine[2].replace("\"", ""));
                    String mag2LegalEntityStock = reader.readLine();

                    if (warehousesSWTP.contains(warehouseID))
                        data.add(Arrays.asList((Object) ("L" + legalEntityID), "WHS" + warehouseID, mag2LegalEntityStock));
                }
            }
        }
        reader.close();
        return data;
    }

    private void importSotUOM(ExecutionContext context, String sediPath, String ostPath, Integer numberOfItems) throws ScriptingErrorLog.SemanticErrorException, SQLException, IOException {

        List<List<Object>> data = importSotUOMItemsFromFile(sediPath, ostPath, numberOfItems);

        if (data != null) {
            ImportField UOMField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
            ImportField sotUOMIDField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
            ImportField netWeightField = new ImportField(LM.findLCPByCompoundName("netWeightSotUOM"));
            ImportField nameField = new ImportField(LM.findLCPByCompoundName("nameSotUOM"));
            ImportField itemField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                    LM.findLCPByCompoundName("externalizableSID").getMapping(itemField));

            ImportKey<?> UOMKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UOM"),
                    LM.findLCPByCompoundName("externalizableSID").getMapping(UOMField));

            ImportKey<?> sotUOMKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("SotUOM"),
                    LM.findLCPByCompoundName("externalizableSID").getMapping(sotUOMIDField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(sotUOMIDField, LM.findLCPByCompoundName("sidExternalizable").getMapping(sotUOMKey)));
            props.add(new ImportProperty(netWeightField, LM.findLCPByCompoundName("netWeightSotUOM").getMapping(sotUOMKey)));
            props.add(new ImportProperty(nameField, LM.findLCPByCompoundName("nameSotUOM").getMapping(sotUOMKey)));
            props.add(new ImportProperty(UOMField, LM.findLCPByCompoundName("UOMSotUOM").getMapping(sotUOMKey),
                    LM.object(LM.findClassByCompoundName("UOM")).getMapping(UOMKey)));
            props.add(new ImportProperty(sotUOMIDField, LM.findLCPByCompoundName("sotUOMItem").getMapping(itemKey),
                    LM.object(LM.findClassByCompoundName("SotUOM")).getMapping(sotUOMKey)));

            ImportTable table = new ImportTable(Arrays.asList(sotUOMIDField, UOMField, netWeightField, nameField, itemField), data);

            DataSession session = context.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(UOMKey, sotUOMKey, itemKey), props);
            service.synchronize(true, false);
            session.apply(context.getBL());
            session.close();
        }
    }

    private List<List<Object>> importSotUOMItemsFromFile(String sediPath, String ostPath, Integer numberOfItems) throws IOException {

        List<List<Object>> data = new ArrayList<List<Object>>();
        Map<String, UOM> uomMap = getUOMMap(sediPath);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ostPath), "cp866"));

        String uomID = null;
        String name = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && data.size() >= numberOfItems)
                break;
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String extra = splittedLine.length > 4 ? splittedLine[4] : null;
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    uomID = splittedLine.length > 0 ? splittedLine[0].substring(15, 18) : null;
                    name = splittedLine.length > 3 ? splittedLine[3] : null;
                } else if ("9".equals(extra)) {
                    String groupID = reader.readLine();
                    if (groupID.split(":")[0].length() == 2)
                        groupID = "0" + groupID;
                    UOM uomObject = uomMap.get(uomID);
                    String uomName = uomObject == null ? null : uomObject.uomName;
                    String uomFullName = uomObject == null ? "" : uomObject.uomFullName;
                    String itemID = /*pnt13 + pnt48*/groupID + ":" + name + uomFullName;
                    data.add(Arrays.asList((Object) (uomID == null ? null : "UOMS" + uomID),
                            uomName == null ? null : "UOM" + uomName,
                            uomObject == null ? null : uomObject.netWeight,
                            uomObject == null ? null : uomObject.uomFullName, "I" + itemID));
                }
            }
        }
        return data;
    }

    private void importSOTUserInvoices(ExecutionContext context, String sediPath, String ostPath, Date startDate, Integer numberOfItems) throws SQLException, ScriptingErrorLog.SemanticErrorException, IOException, ParseException {

        try {
            List<List<Object>> data = importSOTUserInvoicesFromFile(sediPath, ostPath, startDate, numberOfItems);
            if (data != null) {

                ImportField itemField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField userInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("sidExternalizable"));
                ImportField sotSIDField = new ImportField(LM.findLCPByCompoundName("sotSIDItem"));
                ImportField priceListTextUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("priceListTextUserInvoiceDetail"));

                ImportKey<?> userInvoiceDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Purchase.UserInvoiceDetail"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(userInvoiceDetailField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                        LM.findLCPByCompoundName("externalizableSID").getMapping(itemField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(sotSIDField, LM.findLCPByCompoundName("sotSIDItem").getMapping(itemKey)));
                props.add(new ImportProperty(sotSIDField, LM.findLCPByCompoundName("sotSIDUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(priceListTextUserInvoiceDetailField, LM.findLCPByCompoundName("priceListTextUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                ImportTable table = new ImportTable(Arrays.asList(itemField, userInvoiceDetailField,
                        sotSIDField, priceListTextUserInvoiceDetailField), data);

                DataSession session = context.createSession();
                IntegrationService service = new IntegrationService(session, table, Arrays.asList(userInvoiceDetailKey,
                        itemKey), props);
                service.synchronize(true, false);
                session.apply(context.getBL());
                session.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<List<Object>> importSOTUserInvoicesFromFile(String sediPath, String ostPath, Date startDate, Integer numberOfItems) throws IOException, ParseException {

        List<List<Object>> SOTUserInvoicesList = new ArrayList<List<Object>>();
        Map<String, UOM> uomMap = getUOMMap(sediPath);

        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ostPath), "cp866"));

        String uomID = null;
        String dateField = null;
        Date date = null;
        String name = null;
        String priceListText = null;
        String supplierWarehouseID = null;
        Double quantity = null;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && SOTUserInvoicesList.size() >= numberOfItems)
                break;
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String customerWarehouseID = splittedLine.length > 1 ? trimWarehouseSID(splittedLine[1].replace("\"", "")) : null;
                String pnt13 = splittedLine.length > 2 ? splittedLine[2].replace("\"", "") : null;
                String pnt48 = splittedLine.length > 3 ? splittedLine[3].replace("\"", "") : null;
                String extra = splittedLine.length > 4 ? splittedLine[4] : null;
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    uomID = splittedLine.length > 0 ? splittedLine[0].substring(15, 18) : null;
                    String date1Field = splittedLine.length > 0 ? splittedLine[0].substring(18, 24) : null;
                    Date date1 = (date1Field == null || date1Field.equals("000000")) ? null : new Date(DateUtils.parseDate(date1Field, new String[]{"ddMMyy"}).getTime());
                    String date2Field = splittedLine.length > 0 ? splittedLine[0].substring(24, 30) : null;
                    Date date2 = (date2Field == null || date2Field.equals("000000")) ? null : new Date(DateUtils.parseDate(date2Field, new String[]{"ddMMyy"}).getTime());
                    Date currentDate = new Date(System.currentTimeMillis());
                    String currentDateField = new SimpleDateFormat("dd/MM/yy").format(currentDate);
                    dateField = date1 == null ? (date2 == null ? currentDateField : date2Field) : (date2 == null ? date1Field : (date1.after(date2) ? date1Field : date2Field));
                    date = date1 == null ? (date2 == null ? currentDate : date2) : (date2 == null ? date1 : date1.after(date2) ? date1 : date2);
                    name = splittedLine.length > 3 ? splittedLine[3] : null;
                    priceListText = splittedLine.length > 4 ? splittedLine[4] : null;
                    String supplierString = splittedLine.length > 5 ? splittedLine[5] : null;
                    if (supplierString != null && !supplierString.isEmpty() && suppliersFastMap.containsKey(supplierString))
                        supplierWarehouseID = suppliersFastMap.get(supplierString)[1];
                    else
                        supplierWarehouseID = "dsw";
                    quantity = splittedLine.length > 7 ? Double.parseDouble(splittedLine[7]) : null;
                } else if ("9".equals(extra)) {
                    String groupID = reader.readLine();
                    if (groupID.split(":")[0].length() == 2)
                        groupID = "0" + groupID;
                    UOM uom = uomMap.get(uomID);
                    String uomFullName = uom == null ? "" : uom.uomFullName;
                    String itemID = /*pnt13 + pnt48*/groupID + ":" + name + uomFullName;

                    if (isCorrectUserInvoiceDetail(quantity, startDate, date, groupID))
                        SOTUserInvoicesList.add(Arrays.asList((Object) ("I" + itemID), "UID" + supplierWarehouseID + "/" + trimWarehouseSID(customerWarehouseID) + "/" + dateField + "/" + pnt13 + pnt48,
                                pnt13 + pnt48, priceListText));
                }
            }
        }
        reader.close();
        return SOTUserInvoicesList;
    }
}