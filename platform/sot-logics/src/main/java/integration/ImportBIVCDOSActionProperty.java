package integration;

import fdk.integration.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.xBaseJException;
import platform.interop.action.MessageClientAction;
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
import java.math.BigDecimal;
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
        String idRoot = "ВСЕ";
        itemGroupsList.add(new ItemGroup(idRoot, idRoot, null));
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^STG")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                if (splittedLine.length > 1) {
                    String idParent = splittedLine.length == 2 ? idRoot : splittedLine[1];
                    if (idParent.length() == 2)
                        idParent = "0" + idParent;
                    String idGroup = splittedLine.length == 2 ? splittedLine[1] : (idParent + ":" + splittedLine[2]);
                    if (idGroup.length() == 2)
                        idGroup = "0" + idGroup;
                    String idSubParent = idGroup.substring(0, idGroup.length() - 2) + "00";
                    Boolean subParent = splittedLine.length == 3 && !idGroup.endsWith("00");

                    String name = reader.readLine().trim();
                    //if (!"".equals(name)) {
                    if (!parents) {
                        //id - name - idParent(null)
                        itemGroupsList.add(new ItemGroup(idGroup, name, null));
                        if (subParent && !subParentsList.contains(idSubParent))
                            itemGroupsList.add(new ItemGroup(idSubParent, null, null));
                        if (!subParentsList.contains(subParent ? idSubParent : idParent))
                            subParentsList.add(subParent ? idSubParent : idParent);
                    } else {
                        //id - name(null) - idParent
                        itemGroupsList.add(new ItemGroup(idGroup, null, subParent ? idSubParent : idParent));
                        if (subParent)
                            itemGroupsList.add(new ItemGroup(idSubParent, null, idParent));
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
                String bankName = getSplittedValue(splittedLine, 6, "");
                String bankAddress = getSplittedValue(splittedLine, 7, "");
                String idBank = mfo + bankName;
                if (!bankName.isEmpty() && !idBank.isEmpty())
                    banksList.add(new Bank(idBank, bankName, bankAddress.isEmpty() ? null : bankAddress,
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
        String idUOM = null;
        BigDecimal price = null;
        String name = null;
        String idWare;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                pnt13 = getSplittedValue(splittedLine, 2, "\"", null);
                pnt48 = getSplittedValue(splittedLine, 3, "\"", null);
                String extra = getSplittedValue(splittedLine, 4, null);
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    price = parseBigDecimal(getSplittedValue(splittedLine, 2, null));
                    name = getSplittedValue(splittedLine, 3, null);
                } else if ("9".equals(extra)) {
                    String idGroup = reader.readLine();
                    if (idGroup.startsWith("929")) {
                        UOM uom = uomMap.get(idUOM);
                        String uomFullName = uom == null ? "" : uom.uomFullName;
                        idWare = idGroup + ":" + name + uomFullName;
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
                            BigDecimal weight = m.groupCount() < 2 ? null : new BigDecimal(parseBigDecimal(m.group(2)).doubleValue() / coefficient);
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
        Map<String, BigDecimal> retailMarkups = new HashMap<String, BigDecimal>();

        BufferedReader prcReader = new BufferedReader(new InputStreamReader(new FileInputStream(prcPath), "cp866"));
        String line;
        while ((line = prcReader.readLine()) != null) {
            if (line.startsWith("^PRC")) {
                String[] splittedLine = line.split("\\(|\\)");
                if (splittedLine.length == 2) {
                    String[] markupString = prcReader.readLine().split(":");
                    if (markupString.length > 0 && !markupString[0].trim().isEmpty()) {
                        BigDecimal markup = parseBigDecimal(markupString[0]);
                        retailMarkups.put(splittedLine[1].replace("\"", ""), markup);
                    }
                }
            }
        }

        String warePattern = "(\\d{8})\\s?(.*)";

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ostPath), "cp866"));

        Set<String> idItems = new HashSet<String>();

        String pnt13;
        String pnt48;
        String idMarkup = null;
        String idUOM = null;
        Date date = null;
        String name = null;
        String idName = null;
        String idWare = null;
        BigDecimal baseMarkup = null;
        BigDecimal retailVAT = null;
        BigDecimal packAmount = null;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && itemsList.size() >= numberOfItems)
                break;
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                pnt13 = getSplittedValue(splittedLine, 2, null);
                pnt48 = getSplittedValue(splittedLine, 3, null);
                String extra = getSplittedValue(splittedLine, 4, null);
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    idMarkup = getSplittedValue(splittedLine, 0, 0, 1, null);
                    idUOM = getSplittedValue(splittedLine, 0, 15, 18, null);
                    String dateField = getSplittedValue(splittedLine, 0, 24, 30, null);
                    date = dateField == null ? null : new Date(DateUtils.parseDate(dateField, new String[]{"ddmmyy"}).getTime());
                    name = getSplittedValue(splittedLine, 3, null);
                    idName = name;
                    packAmount = null;
                    Pattern rPack = Pattern.compile(".*(?:\\\\|\\/)(\\d+)(?:\\\\|\\/)?");
                    Matcher mPack = rPack.matcher(name);
                    if (mPack.matches()) {
                        BigDecimal value = parseBigDecimal(mPack.group(1));
                        packAmount = value.compareTo(new BigDecimal(1000))<0 ? value : null;
                    }
                    idWare = null;
                    if (name != null) {
                        Pattern r = Pattern.compile(warePattern);
                        Matcher m = r.matcher(name);
                        if (m.matches()) {
                            idWare = m.group(1).trim();
                            name = m.group(2).trim();
                        }
                    }

                    try {
                        baseMarkup = splittedLine.length > 15 ? parseBigDecimal(splittedLine[15].endsWith(".") ?
                                splittedLine[15].substring(0, splittedLine[15].length() - 1) : splittedLine[15]) : null;
                    } catch (NumberFormatException e) {
                        baseMarkup = null;
                    }

                    retailVAT = getBigDecimalSplittedValue(splittedLine, 18, "20");
                    //Так как ещё остались товары со старым НДС 18%
                    retailVAT = retailVAT.equals(new BigDecimal(18)) ? new BigDecimal(20) : retailVAT;
                } else if ("9".equals(extra)) {
                    String idGroup = reader.readLine();
                    if (idGroup.split(":")[0].length() == 2)
                        idGroup = "0" + idGroup;
                    UOM uom = uomMap.get(idUOM);
                    String uomFullName = uom == null ? "" : uom.uomFullName;
                    String idItem = /*pnt13 + pnt48*/idGroup + ":" + idName + uomFullName;
                    if (!idGroup.startsWith("929") && !idItems.contains(idItem)) {
                        idItems.add(idItem);
                        itemsList.add(new Item(idItem, idGroup, name,
                                uom == null ? null : uom.uomName, uom == null ? null : uom.uomShortName,
                                uom == null ? null : uom.uomName, null, null, null, null, idItem, date, null,
                                uom == null ? null : uom.netWeight, uom == null ? null : uom.grossWeight, null,
                                retailVAT, idWare, null, null, null, baseMarkup, retailMarkups.get(idMarkup),
                                idItem, packAmount, null, null, null, null));
                    }
                }
            }
        }
        reader.close();

        context.requestUserInteraction(new MessageClientAction("Кол-во товаров :" + idItems.size(), "Импорт товаров"));
        return itemsList;
    }

    private boolean isCorrectUserInvoiceDetail(BigDecimal quantity, Date startDate, Date date, String idGroup) throws ParseException {
        Boolean correctDate = startDate != null && (date != null && startDate.before(date));
        Boolean correctQuantity = quantity != null && !quantity.equals(BigDecimal.ZERO);
        Boolean correctIdGroup = idGroup == null || !idGroup.startsWith("929");
        return (correctDate || correctQuantity) || correctIdGroup;
    }

    List<String> ownershipsList = Arrays.asList("НАУЧНО ПРОИЗ.ОБЩЕСТВО С ДОПОЛ ОТВЕТ.",
            "ОАОТ", "ОАО", "СООО", "ООО", "ОДО", "ЗАО", "ЧУТПП", "ЧТУП", "ЧУТП", "ТЧУП", "ЧУП", "РУП", "РДУП", "УП", "ИП", "СПК", "СП");
    List<String> patterns = Arrays.asList("КАЧ(\\.)?(УД\\.)?(N|№)?(Б\\/Н)?\\s?((\\d|\\/|-|\\.)+\\s)?(12\\s)?(ОТ|ЛТ)?\\s?(\\d|\\.)+Г?",
            "((К|У|R)(\\/|\\s)?(У|К|У|E))(\\s)?(\\p{L}{2})?(\\s)?(N|№)?(((\\d|\\/|\\\\)+\\s)|(Б\\/Н\\s))?(СМ|А)?(ОТ)?(\\s)?(\\d|\\.)+Г?");

    private String trimIdWarehouse(String idWarehouse) {
        if(idWarehouse==null)
            return null;
        while (idWarehouse.startsWith("0"))
            idWarehouse = idWarehouse.substring(1);
        return idWarehouse;
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
                    String idLegalEntity = splittedLine[1].replaceAll("\"", "");
                    String idWarehouse = trimIdWarehouse((splittedLine.length == 2 ? splittedLine[1] : splittedLine[2]).replace("\"", ""));
                    if (warehouseLegalEntityMap.containsKey(idWarehouse))
                        idLegalEntity = warehouseLegalEntityMap.get(idWarehouse);
                    else
                        warehouseLegalEntityMap.put(idWarehouse, idLegalEntity);
                    splittedLine = reader.readLine().split(":");
                    String name = getSplittedValue(splittedLine, 0, "\u007F", "");
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
                        if (!suppliers.contains(idWarehouse)) {
                            suppliers.add(idWarehouse);
                            suppliersMap.put(name, new String[]{idLegalEntity, "S" + idWarehouse});
                        }
                    }
                }
            }
        }
        reader.close();

        Map<String, BigDecimal> totalSumWarehouse = new HashMap<String, BigDecimal>();

        List<UserInvoiceDetail> userInvoiceDetailsList = new ArrayList<UserInvoiceDetail>();
        Map<String, UOM> uomMap = getUOMMap(sediPath);

        reader = new BufferedReader(new InputStreamReader(new FileInputStream(ostPath), "cp866"));

        String idUOM = null;
        Date date = null;
        String dateField = null;
        String name = null;
        String idSupplier = null;
        String idSupplierWarehouse = null;
        BigDecimal quantity = null;
        BigDecimal price = null;
        BigDecimal chargePrice = null;
        BigDecimal manufacturingPrice = null;
        BigDecimal wholesalePrice = null;
        BigDecimal wholesaleMarkup = null;
        String textCompliance = null;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && userInvoiceDetailsList.size() >= numberOfItems)
                break;
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String idCustomerWarehouse = trimIdWarehouse(getSplittedValue(splittedLine, 1, "\"", null));
                String pnt13 = getSplittedValue(splittedLine, 2, "\"", null);
                String pnt48 = getSplittedValue(splittedLine, 3, "\"", null);
                String extra = getSplittedValue(splittedLine, 4, null);
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    idUOM = getSplittedValue(splittedLine, 0, 15, 18, null);
                    String date1Field = getSplittedValue(splittedLine, 0, 18, 24, null);
                    Date date1 = (date1Field == null || date1Field.equals("000000")) ? null : new Date(DateUtils.parseDate(date1Field, new String[]{"ddMMyy"}).getTime());
                    String date2Field = getSplittedValue(splittedLine, 0, 24, 30, null);
                    Date date2 = (date2Field == null || date2Field.equals("000000")) ? null : new Date(DateUtils.parseDate(date2Field, new String[]{"ddMMyy"}).getTime());
                    Date currentDate = new Date(System.currentTimeMillis());
                    String currentDateField = new SimpleDateFormat("dd/MM/yy").format(currentDate);
                    dateField = date1 == null ? (date2 == null ? currentDateField : date2Field) : (date2 == null ? date1Field : (date1.after(date2) ? date1Field : date2Field));
                    date = date1 == null ? (date2 == null ? currentDate : date2) : (date2 == null ? date1 : date1.after(date2) ? date1 : date2);
                    name = getSplittedValue(splittedLine, 3, null);

                    String supplierString = getSplittedValue(splittedLine, 5, null);
                    if (supplierString != null && !supplierString.isEmpty()) {
                        textCompliance = supplierString;
                        idSupplier = null;
                        idSupplierWarehouse = null;
                        if (suppliersFastMap.containsKey(supplierString)) {
                            String[] value = suppliersFastMap.get(supplierString);
                            idSupplier = warehouseLegalEntityMap.containsKey(value[1]) ? warehouseLegalEntityMap.get(value[1]) : value[0];
                            idSupplierWarehouse = value[1];
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
                                    idSupplier = warehouseLegalEntityMap.containsKey(entry.getValue()[1]) ? warehouseLegalEntityMap.get(entry.getValue()[1]) : entry.getValue()[0];
                                    idSupplierWarehouse = entry.getValue()[1];
                                }
                            }
                            if (matchesMax == 0 || ((matchesMax > 0) && (matchesMax < 6) && (matchesMax < supplierName.length() / 4))) {
                                //ServerLoggers.systemLogger.info(name + ":   " + supplierName);
                                idSupplier = "ds";
                                idSupplierWarehouse = "dsw";
                            } else
                                suppliersFastMap.put(supplierString, new String[]{idSupplier, idSupplierWarehouse});
                        }
                    } else {
                        idSupplier = "ds";
                        idSupplierWarehouse = "dsw";
                    }
                    quantity = getBigDecimalSplittedValue(splittedLine, 7, null);
                    BigDecimal sumPrice = getBigDecimalSplittedValue(splittedLine, 2, null);
                    sumPrice = sumPrice == null ? null : new BigDecimal((Math.round(sumPrice.doubleValue() * 100)) / 100);

                    BigDecimal chargePricePercent = readPercent(getSplittedValue(splittedLine, 19, "0"));
                    try {
                        chargePrice = new BigDecimal(Math.round((sumPrice.doubleValue() * chargePricePercent.doubleValue()) / (100 + chargePricePercent.doubleValue()) * 100) / 100);
                    } catch (NumberFormatException e) {
                        chargePrice = BigDecimal.ZERO;
                    }
                    manufacturingPrice = new BigDecimal(sumPrice.doubleValue() - chargePrice.doubleValue());

                    BigDecimal manufacturingPercent = readPercent(getSplittedValue(splittedLine, 17, "0"));
                    price = new BigDecimal(manufacturingPrice.doubleValue() * (100 - manufacturingPercent.doubleValue()) / 100);

                    wholesaleMarkup = readPercent(getSplittedValue(splittedLine, 15, "0"));
                    wholesalePrice = new BigDecimal(sumPrice.doubleValue() * (100 + wholesaleMarkup.doubleValue()) / 100);
                } else if ("1".equals(extra)) {
                    String extraCompliance = reader.readLine();
                    if (extraCompliance != null && !extraCompliance.isEmpty()) {
                        if (textCompliance == null || textCompliance.isEmpty())
                            textCompliance = extraCompliance;
                        else
                            textCompliance += " " + extraCompliance;
                    }
                } else if ("9".equals(extra)) {
                    String idGroup = reader.readLine();
                    if (idGroup.split(":")[0].length() == 2)
                        idGroup = "0" + idGroup;
                    UOM uom = uomMap.get(idUOM);
                    String uomFullName = uom == null ? "" : uom.uomFullName;
                    Boolean isWare = idGroup.startsWith("929");
                    String idItem = isWare ? (pnt13 + pnt48) : (idGroup + ":" + name + uomFullName);
                    if (isCorrectUserInvoiceDetail(quantity, startDate, date, idGroup)) {
                        String series = "AA";
                        String number = idSupplierWarehouse + "/" + idCustomerWarehouse + "/" + dateField;
                        userInvoiceDetailsList.add(new UserInvoiceDetail(series + number, series, number,
                                null, true, idSupplierWarehouse + "/" + idCustomerWarehouse + "/" + dateField + "/" + pnt13 + pnt48,
                                date, idItem, isWare, quantity, idSupplier, idCustomerWarehouse, idSupplierWarehouse, price, chargePrice,
                                manufacturingPrice, wholesalePrice, wholesaleMarkup, null, null, textCompliance, null, null,
                                null, null, null, null, null, null));
                        BigDecimal sum = new BigDecimal((Math.round(((price.doubleValue() + (chargePrice == null ? 0 : chargePrice.doubleValue())) * quantity.doubleValue()) * 100)) / 100);
                        BigDecimal subtotal = totalSumWarehouse.get(idCustomerWarehouse);
                        if (subtotal == null)
                            totalSumWarehouse.put(idCustomerWarehouse, sum);
                        else
                            totalSumWarehouse.put(idCustomerWarehouse, sum.add(subtotal));
                    }
                }
            }
        }
        reader.close();
        String message = "Кол-во позиций :" + userInvoiceDetailsList.size() + "\r\n";
        for (Map.Entry<String, BigDecimal> entry : totalSumWarehouse.entrySet())
            message += entry.getKey() + ": " + NumberFormat.getNumberInstance().format(entry.getValue()) + "\r\n";
        context.requestUserInteraction(new MessageClientAction(message, "Общая сумма"));
        return userInvoiceDetailsList;
    }

    private BigDecimal readPercent(String percentString) {
        if (percentString == null) return null;
        if (percentString.endsWith(".00"))
            percentString = percentString.substring(0, percentString.length() - 3);
        BigDecimal result = BigDecimal.ZERO;
        try {
            result = parseBigDecimal(percentString);
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
        BigDecimal quantity = new BigDecimal(0.0);
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                smol = trimIdWarehouse(getSplittedValue(splittedLine, 1, "\"", null));
                String extra = getSplittedValue(splittedLine, 4, null);
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    String date1Field = getSplittedValue(splittedLine, 0, 18, 24, null);
                    Date date1 = date1Field == null ? null : new Date(DateUtils.parseDate(date1Field, new String[]{"ddMMyy"}).getTime());
                    String date2Field = getSplittedValue(splittedLine, 0, 24, 30, null);
                    Date date2 = date2Field == null ? null : new Date(DateUtils.parseDate(date2Field, new String[]{"ddMMyy"}).getTime());
                    date = date1 == null ? date2 : (date2 == null ? date1 : date1.after(date2) ? date1 : date2);
                    quantity = getBigDecimalSplittedValue(splittedLine, 7, "0");
                }
                if (smol != null && isCorrectUserInvoiceDetail(quantity, startDate, date, null))
                    warehousesOST.add(smol);
            }
        }
        reader.close();

        String idDefaultLegalEntity = "sle";
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(smolPath), "cp866"));
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SMOL")) {
                String idWarehouse = trimIdWarehouse(line.split("\\(|\\)|,")[1].replace("\"", ""));
                String[] dataWarehouse = reader.readLine().split(":");
                String name = getSplittedValue(dataWarehouse, 0, null);
                if (name != null && !"".equals(name) && warehousesOST.contains(idWarehouse)) {
                    warehousesList.add(new Warehouse(idDefaultLegalEntity, "own", idWarehouse, name, null));
                }
            }
        }
        reader.close();

        reader = new BufferedReader(new InputStreamReader(new FileInputStream(swtpPath), "cp866"));
        Set<String> warehousesSWTP = new HashSet<String>();
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("^SWTP")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String idLegalEntity = splittedLine[1].replace("\"", "");
                String idWarehouse = trimIdWarehouse((splittedLine.length == 2 ? splittedLine[1] : splittedLine[2]).replace("\"", ""));
                String[] dataLegalEntity = reader.readLine().split(":");
                String name = getSplittedValue(dataLegalEntity, 0, "");
                if(splittedLine.length==3 && !getSplittedValue(dataLegalEntity, 3, "").isEmpty()) {
                    String index = getSplittedValue(dataLegalEntity, 1, "");
                    name += index.equals("0") ? "" : (" " + index);
                }
                String warehouseAddress1 = getSplittedValue(dataLegalEntity, 9, "");
                String warehouseAddress2 = getSplittedValue(dataLegalEntity, 10, "");
                String warehouseAddress = (warehouseAddress1 + " " + warehouseAddress2).trim();
                Boolean filtered = false;
                for (String filter : employeeFilters) {
                    if (name.startsWith(filter)) {
                        filtered = true;
                        break;
                    }
                }
                if (!name.trim().isEmpty() && !filtered && !warehousesSWTP.contains(idWarehouse)) {
                    warehousesList.add(new Warehouse(idLegalEntity, "contractor", "S" + idWarehouse, name, warehouseAddress.isEmpty() ? null : warehouseAddress));
                    warehousesSWTP.add(idWarehouse);
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
                    String idLegalEntity = splittedLine[1].replace("\"", "");
                    splittedLine = reader.readLine().split(":");
                    String name = getSplittedValue(splittedLine, 0, "\u007F", "");
                    String mfo = splittedLine.length > 2 ? (splittedLine[2].length() > 3 ? splittedLine[2].substring(splittedLine[2].length() - 3) : splittedLine[2].trim()) : "";
                    String account = getSplittedValue(splittedLine, 3, "");
                    String okpo = getSplittedValue(splittedLine, 4, "");
                    String unp = getSplittedValue(splittedLine, 5, "");
                    String bankName = getSplittedValue(splittedLine, 6, "");
                    String address1 = getSplittedValue(splittedLine, 9, "");
                    String address2 = getSplittedValue(splittedLine, 10, "");
                    String address = (address1 + " " + address2).trim();
                    String idBank = mfo + bankName;
                    Boolean filtered = false;
                    for (String filter : employeeFilters) {
                        if (name.startsWith(filter)) {
                            filtered = true;
                            break;
                        }
                    }
                    if (!name.isEmpty() && !filtered)
                        legalEntitiesList.add(new LegalEntity(idLegalEntity, name,
                                address.isEmpty() ? null : address, unp.isEmpty() ? null : unp,
                                okpo.isEmpty() ? null : okpo, null, null, null, null,
                                account.isEmpty() ? null : account, null, null,
                                idBank.isEmpty() ? null : idBank, nameCountry, true, null, true));
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
                    String idLegalEntity = splittedLine[1].replace("\"", "");
                    splittedLine = reader.readLine().split(":");
                    String name = getSplittedValue(splittedLine, 0, "");
                    for (String filter : employeeFilters) {
                        if (name.startsWith(filter)) {
                            String[] fullName = name.replaceFirst(filter, "").trim().split(" ");
                            String firstName = "";
                            for (int i = 1; i < fullName.length; i++)
                                firstName += fullName[i] + " ";
                            employeesList.add(new Employee(idLegalEntity, firstName.trim(), fullName[0], filter));
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
                    String idLegalEntity1 = splittedLine[1].replace("\"", "");
                    String idLegalEntity2 = "sle";
                    String idContract1 = idLegalEntity1 + "/" + idLegalEntity2;
                    String idContract2 = idLegalEntity2 + "/" + idLegalEntity1;
                    splittedLine = reader.readLine().split(":");
                    String contractString = getSplittedValue(splittedLine, 11, "");
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
                            contractsList.add(new Contract(idContract1, idLegalEntity1, idLegalEntity2,
                                    number, dateFrom, dateTo, shortNameCurrency));
                            contractsList.add(new Contract(idContract2, idLegalEntity2, idLegalEntity1,
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

            ImportField idLegalEntityField = new ImportField(LM.findLCPByCompoundName("idLegalEntity"));
            ImportField idWarehouseField = new ImportField(LM.findLCPByCompoundName("idWarehouse"));
            ImportField dataField = new ImportField(LM.findLCPByCompoundName("mag2LegalEntityStock"));

            ImportKey<?> legalEntityKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("LegalEntity"),
                    LM.findLCPByCompoundName("legalEntityId").getMapping(idLegalEntityField));

            ImportKey<?> warehouseKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Warehouse"),
                    LM.findLCPByCompoundName("warehouseId").getMapping(idWarehouseField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(idLegalEntityField, LM.findLCPByCompoundName("idLegalEntity").getMapping(legalEntityKey)));
            props.add(new ImportProperty(idWarehouseField, LM.findLCPByCompoundName("idWarehouse").getMapping(warehouseKey)));
            props.add(new ImportProperty(dataField, LM.findLCPByCompoundName("mag2LegalEntityStock").getMapping(legalEntityKey, warehouseKey)));

            ImportTable table = new ImportTable(Arrays.asList(idLegalEntityField, idWarehouseField,
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
                String name = getSplittedValue(dataLegalEntity, 0, "");
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
                    String idLegalEntity = splittedLine[1].replace("\"", "");
                    String idWarehouse = trimIdWarehouse(splittedLine[2].replace("\"", ""));
                    String mag2LegalEntityStock = reader.readLine();

                    if (warehousesSWTP.contains(idWarehouse))
                        data.add(Arrays.asList((Object) idLegalEntity, "S" + idWarehouse, mag2LegalEntityStock));
                }
            }
        }
        reader.close();
        return data;
    }

    private void importSotUOM(ExecutionContext context, String sediPath, String ostPath, Integer numberOfItems) throws ScriptingErrorLog.SemanticErrorException, SQLException, IOException {

        List<List<Object>> data = importSotUOMItemsFromFile(sediPath, ostPath, numberOfItems);

        if (data != null) {
            ImportField idUOMField = new ImportField(LM.findLCPByCompoundName("idUOM"));
            ImportField idSotUOMField = new ImportField(LM.findLCPByCompoundName("idSotUOM"));
            ImportField netWeightSotUOMField = new ImportField(LM.findLCPByCompoundName("netWeightSotUOM"));
            ImportField nameSotUOMField = new ImportField(LM.findLCPByCompoundName("nameSotUOM"));
            ImportField idItemField = new ImportField(LM.findLCPByCompoundName("idItem"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                    LM.findLCPByCompoundName("itemId").getMapping(idItemField));

            ImportKey<?> UOMKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("UOM"),
                    LM.findLCPByCompoundName("UOMId").getMapping(idUOMField));

            ImportKey<?> sotUOMKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("SotUOM"),
                    LM.findLCPByCompoundName("sotUOMId").getMapping(idSotUOMField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(idSotUOMField, LM.findLCPByCompoundName("idSotUOM").getMapping(sotUOMKey)));
            props.add(new ImportProperty(netWeightSotUOMField, LM.findLCPByCompoundName("netWeightSotUOM").getMapping(sotUOMKey)));
            props.add(new ImportProperty(nameSotUOMField, LM.findLCPByCompoundName("nameSotUOM").getMapping(sotUOMKey)));
            props.add(new ImportProperty(idUOMField, LM.findLCPByCompoundName("UOMSotUOM").getMapping(sotUOMKey),
                    LM.object(LM.findClassByCompoundName("UOM")).getMapping(UOMKey)));
            props.add(new ImportProperty(idSotUOMField, LM.findLCPByCompoundName("sotUOMItem").getMapping(itemKey),
                    LM.object(LM.findClassByCompoundName("SotUOM")).getMapping(sotUOMKey)));

            ImportTable table = new ImportTable(Arrays.asList(idSotUOMField, idUOMField, netWeightSotUOMField, nameSotUOMField, idItemField), data);

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

        String idUOM = null;
        String name = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && data.size() >= numberOfItems)
                break;
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String extra = getSplittedValue(splittedLine, 4, null);
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    idUOM = getSplittedValue(splittedLine, 0, 15, 18, null);
                    name = getSplittedValue(splittedLine, 3, null);
                } else if ("9".equals(extra)) {
                    String idGroup = reader.readLine();
                    if (idGroup.split(":")[0].length() == 2)
                        idGroup = "0" + idGroup;
                    UOM uomObject = uomMap.get(idUOM);
                    String uomName = uomObject == null ? null : uomObject.uomName;
                    String uomFullName = uomObject == null ? "" : uomObject.uomFullName;
                    String idItem = /*pnt13 + pnt48*/idGroup + ":" + name + uomFullName;
                    data.add(Arrays.asList((Object) (idUOM == null ? null : "S" + idUOM), uomName,
                            uomObject == null ? null : uomObject.netWeight,
                            uomObject == null ? null : uomObject.uomFullName, idItem));
                }
            }
        }
        return data;
    }

    private void importSOTUserInvoices(ExecutionContext context, String sediPath, String ostPath, Date startDate, Integer numberOfItems) throws SQLException, ScriptingErrorLog.SemanticErrorException, IOException, ParseException {

        try {
            List<List<Object>> data = importSOTUserInvoicesFromFile(sediPath, ostPath, startDate, numberOfItems);
            if (data != null) {

                ImportField idItemField = new ImportField(LM.findLCPByCompoundName("idItem"));
                ImportField userInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("idUserInvoiceDetail"));
                ImportField sotSIDItemField = new ImportField(LM.findLCPByCompoundName("sotSIDItem"));
                ImportField priceListTextUserInvoiceDetailField = new ImportField(LM.findLCPByCompoundName("priceListTextUserInvoiceDetail"));

                ImportKey<?> userInvoiceDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Purchase.UserInvoiceDetail"),
                        LM.findLCPByCompoundName("userInvoiceDetailId").getMapping(userInvoiceDetailField));

                ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Item"),
                        LM.findLCPByCompoundName("itemId").getMapping(idItemField));

                List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

                props.add(new ImportProperty(sotSIDItemField, LM.findLCPByCompoundName("sotSIDItem").getMapping(itemKey)));
                props.add(new ImportProperty(sotSIDItemField, LM.findLCPByCompoundName("sotSIDUserInvoiceDetail").getMapping(userInvoiceDetailKey)));
                props.add(new ImportProperty(priceListTextUserInvoiceDetailField, LM.findLCPByCompoundName("priceListTextUserInvoiceDetail").getMapping(userInvoiceDetailKey)));

                ImportTable table = new ImportTable(Arrays.asList(idItemField, userInvoiceDetailField,
                        sotSIDItemField, priceListTextUserInvoiceDetailField), data);

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

        String idUOM = null;
        String dateField = null;
        Date date = null;
        String name = null;
        String priceListText = null;
        String idSupplierWarehouse = null;
        BigDecimal quantity = null;
        while ((line = reader.readLine()) != null) {
            if (numberOfItems != null && SOTUserInvoicesList.size() >= numberOfItems)
                break;
            if (line.startsWith("^OST")) {
                String[] splittedLine = line.split("\\(|\\)|,");
                String idCustomerWarehouse = splittedLine.length > 1 ? trimIdWarehouse(splittedLine[1].replace("\"", "")) : null;
                String pnt13 = getSplittedValue(splittedLine, 2, "\"", null);
                String pnt48 = getSplittedValue(splittedLine, 3, "\"", null);
                String extra = getSplittedValue(splittedLine, 4, null);
                if (extra == null && splittedLine.length > 3) {
                    splittedLine = reader.readLine().split(":");
                    idUOM = getSplittedValue(splittedLine, 0, 15, 18, null);
                    String date1Field = getSplittedValue(splittedLine, 0, 18, 24, null);
                    Date date1 = (date1Field == null || date1Field.equals("000000")) ? null : new Date(DateUtils.parseDate(date1Field, new String[]{"ddMMyy"}).getTime());
                    String date2Field = getSplittedValue(splittedLine, 0, 24, 30, null);
                    Date date2 = (date2Field == null || date2Field.equals("000000")) ? null : new Date(DateUtils.parseDate(date2Field, new String[]{"ddMMyy"}).getTime());
                    Date currentDate = new Date(System.currentTimeMillis());
                    String currentDateField = new SimpleDateFormat("dd/MM/yy").format(currentDate);
                    dateField = date1 == null ? (date2 == null ? currentDateField : date2Field) : (date2 == null ? date1Field : (date1.after(date2) ? date1Field : date2Field));
                    date = date1 == null ? (date2 == null ? currentDate : date2) : (date2 == null ? date1 : date1.after(date2) ? date1 : date2);
                    name = getSplittedValue(splittedLine, 3, null);
                    priceListText = getSplittedValue(splittedLine, 4, null);
                    String supplierString = getSplittedValue(splittedLine, 5, null);
                    if (supplierString != null && !supplierString.isEmpty() && suppliersFastMap.containsKey(supplierString))
                        idSupplierWarehouse = suppliersFastMap.get(supplierString)[1];
                    else
                        idSupplierWarehouse = "dsw";
                    quantity = getBigDecimalSplittedValue(splittedLine, 7, null);
                } else if ("9".equals(extra)) {
                    String idGroup = reader.readLine();
                    if (idGroup.split(":")[0].length() == 2)
                        idGroup = "0" + idGroup;
                    UOM uom = uomMap.get(idUOM);
                    String uomFullName = uom == null ? "" : uom.uomFullName;
                    String idItem = /*pnt13 + pnt48*/idGroup + ":" + name + uomFullName;

                    if (isCorrectUserInvoiceDetail(quantity, startDate, date, idGroup))
                        SOTUserInvoicesList.add(Arrays.asList((Object) idItem, idSupplierWarehouse + "/" + trimIdWarehouse(idCustomerWarehouse) + "/" + dateField + "/" + pnt13 + pnt48,
                                pnt13 + pnt48, priceListText));
                }
            }
        }
        reader.close();
        return SOTUserInvoicesList;
    }

    private BigDecimal parseBigDecimal(String value) {
        return BigDecimal.valueOf(Double.parseDouble(value));
    }

    private BigDecimal getBigDecimalSplittedValue(String[] splittedLine, int index, String defaultValue) {
        return parseBigDecimal(getSplittedValue(splittedLine, index, defaultValue));
    }

    private String getSplittedValue(String[] splittedLine, int index, String defaultValue) {
        return splittedLine.length > index ? splittedLine[index].trim() : defaultValue;
    }

    private String getSplittedValue(String[] splittedLine, int index, String remove, String defaultValue) {
        String result = getSplittedValue(splittedLine, index, defaultValue);
        return result==null ? null : result.replace(remove, "");
    }

    private String getSplittedValue(String[] splittedLine, int index, int from, int to, String defaultValue) {
        String result = getSplittedValue(splittedLine, index, defaultValue);
        return result==null ? null : result.substring(from, to);
    }
}