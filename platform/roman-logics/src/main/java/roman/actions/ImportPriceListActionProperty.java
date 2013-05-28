package roman.actions;

import org.apache.commons.lang.time.DateUtils;
import platform.interop.action.MessageClientAction;
import platform.server.classes.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.*;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;

public class ImportPriceListActionProperty extends ScriptingActionProperty {

    public ImportPriceListActionProperty(ScriptingLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) {

        try {
            DataSession session = context.getSession();
            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(false, false, "Файлы данных", "dat");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());
                for (byte[] file : fileList) {
                    File f = File.createTempFile("temp", ".dat");
                    FileOutputStream fileOutputStream = new FileOutputStream(f);
                    fileOutputStream.write(file);
                    fileOutputStream.close();

                    List<String> fileData = new ArrayList<String>();

                    Scanner in = new Scanner(f);
                    while (in.hasNext())
                        fileData.add(in.nextLine());

                    Map<String, DataObject> priceSetDocumentMap = new HashMap<String, DataObject>();
                    String barcodeChanged = "";
                    Integer barcodeCount = 0;
                    Integer maxBarcodeCount = 180;
                    String pricesChanged = "";
                    Integer pricesCount = 0;
                    for (int i = 1; i < fileData.size(); i++) {
                        String[] row = fileData.get(i).split(";");
                        String series = row[0];
                        String currency = row[1];
                        String number = row[2];
                        String barcode = (String) LM.findLCPByCompoundName("completeBarcode").read(session, new DataObject(row[3], StringClass.get(13)));
                        Timestamp startDate = new Timestamp(DateUtils.parseDate(row[4], new String[]{"yyyy-MM-dd"}).getTime());
                        Timestamp endDate = new Timestamp(DateUtils.parseDate(row[5], new String[]{"yyyy-MM-dd"}).getTime() + 3600 * 24 * 1000);
                        Double price = new Double(row[6]);
                        String status = row[7];

                        String key = series + "_" + currency + "_" + number;
                        DataObject priceSetDocumentObject = priceSetDocumentMap.get(key);

                        if (priceSetDocumentObject == null) {
                            ObjectValue priceSetDocumentObjectValue = LM.findLCPByCompoundName("priceSetDocumentSeriesNumberShortNameCurrency").readClasses(session,
                                    new DataObject(series, StringClass.get(2)), new DataObject(number, StringClass.get(18)),
                                    new DataObject(currency, StringClass.get(5)));

                            if (priceSetDocumentObjectValue.isNull()) {
                                priceSetDocumentObject = context.addObject((ConcreteCustomClass) LM.findClassByCompoundName("BasePriceSetDocument"));
                                LM.findLCPByCompoundName("seriesObject").change(series, session, priceSetDocumentObject);
                                LM.findLCPByCompoundName("numberObject").change(number, session, priceSetDocumentObject);
                                LM.findLCPByCompoundName("currencyPriceSetDocument").change(LM.findLCPByCompoundName("currencyShortName").read(session, new DataObject(currency, StringClass.get(5))), session, (DataObject) priceSetDocumentObject);
                                LM.findLCPByCompoundName("datePriceSetDocument").change(new java.sql.Date(startDate.getTime()), session, priceSetDocumentObject);
                                LM.findLCPByCompoundName("timePriceSetDocument").change(new Time(startDate.getTime()), session, priceSetDocumentObject);
                                if (!row[5].startsWith("9999"))
                                    LM.findLCPByCompoundName("dateTimeToPriceSetDocument").change(endDate, session, priceSetDocumentObject);
                            } else {
                                priceSetDocumentObject = (DataObject) priceSetDocumentObjectValue;
                            }
                        }
                        priceSetDocumentMap.put(key, priceSetDocumentObject);

                        ObjectValue skuObject = LM.findLCPByCompoundName("barcodeToObject").readClasses(session, new DataObject(barcode, StringClass.get(14)));
                        if (!skuObject.isNull()) {
                            DataObject skuDataObject = (DataObject) skuObject;
                            Boolean isDelete = status.equals("D") ? null : true;
                            LM.findLCPByCompoundName("inPriceSetDocumentSku").change(isDelete, session, priceSetDocumentObject, skuDataObject);
                            LM.findLCPByCompoundName("priceBasePriceSetDocumentSku").change(price, session, priceSetDocumentObject, skuDataObject);
                            LM.findLCPByCompoundName("userDateTimePriceSetDocumentSku").change(startDate, session, priceSetDocumentObject, skuDataObject);
                            LM.findLCPByCompoundName("userDateTimeToPriceSetDocumentSku").change(endDate, session, priceSetDocumentObject, skuDataObject);
                        } else {
                            barcodeCount++;
                            if (barcodeCount < maxBarcodeCount) {
                                barcodeChanged += barcode + ", ";
                                if (barcodeCount % 6 == 0)
                                    barcodeChanged += "\n";
                            }
                        }
                    }
//                    session.apply(LM.getBL());
                    for (Map.Entry<String, DataObject> entry : priceSetDocumentMap.entrySet()) {
                        pricesCount++;
                        pricesChanged += entry.getKey() + ", ";
                        if (pricesCount % 6 == 0)
                            pricesChanged += "\n";
                    }
                    String message = "";
                    if (!pricesChanged.equals(""))
                        message += "Изменённые прайсы:\n" + pricesChanged.substring(0, pricesChanged.length() - 2) + "\n";
                    else
                        message = "Ни одного прайса не было изменено";
                    if (!barcodeChanged.equals(""))
                        message += "следующие штрих-коды не были найдены:\n" + barcodeChanged.substring(0, barcodeChanged.length() - 2);
                    if (barcodeCount > maxBarcodeCount) message += " ...";
                    context.delayUserInterfaction(new MessageClientAction(message, "Импорт завершён"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
