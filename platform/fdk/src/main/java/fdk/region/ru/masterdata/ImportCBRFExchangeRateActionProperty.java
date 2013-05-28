package fdk.region.ru.masterdata;

import org.apache.commons.lang.time.DateUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.DateClass;
import platform.server.classes.ValueClass;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ImportCBRFExchangeRateActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface currencyInterface;

    public ImportCBRFExchangeRateActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("Currency")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        currencyInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            DataObject currencyObject = context.getDataKeyValue(currencyInterface);

            String extraSIDCurrency = (String) LM.findLCPByCompoundName("extraSIDCurrency").read(context, currencyObject);
            Date cbrfDateFrom = (Date) LM.findLCPByCompoundName("importCBRFExchangeRateDateFrom").read(context);
            Date cbrfDateTo = (Date) LM.findLCPByCompoundName("importCBRFExchangeRateDateTo").read(context);

            if (cbrfDateFrom != null && cbrfDateTo != null && extraSIDCurrency != null)
                importExchanges(cbrfDateFrom, cbrfDateTo, extraSIDCurrency, context);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JDOMException e) {
            throw new RuntimeException(e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }

    private void importExchanges(Date dateFrom, Date dateTo, String extraSIDCurrency, ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, IOException, JDOMException, SQLException, ParseException {


        List<Exchange> exchangesList = importExchangesFromXML(dateFrom, dateTo, extraSIDCurrency, context);

        if (exchangesList != null) {

            ImportField typeExchangeRUField = new ImportField(LM.findLCPByCompoundName("nameTypeExchange"));
            ImportField typeExchangeForeignField = new ImportField(LM.findLCPByCompoundName("nameTypeExchange"));
            ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));
            ImportField homeCurrencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));
            ImportField rateField = new ImportField(LM.findLCPByCompoundName("rateExchange"));
            ImportField foreignRateField = new ImportField(LM.findLCPByCompoundName("rateExchange"));
            ImportField dateField = new ImportField(DateClass.instance);

            ImportKey<?> typeExchangeRUKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("TypeExchange"),
                    LM.findLCPByCompoundName("typeExchangeName").getMapping(typeExchangeRUField));

            ImportKey<?> typeExchangeForeignKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("TypeExchange"),
                    LM.findLCPByCompoundName("typeExchangeName").getMapping(typeExchangeForeignField));

            ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                    LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

            ImportKey<?> homeCurrencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Currency"),
                    LM.findLCPByCompoundName("currencyShortName").getMapping(homeCurrencyField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(typeExchangeRUField, LM.findLCPByCompoundName("nameTypeExchange").getMapping(typeExchangeRUKey)));
            props.add(new ImportProperty(homeCurrencyField, LM.findLCPByCompoundName("currencyTypeExchange").getMapping(typeExchangeRUKey),
                    LM.object(LM.findClassByCompoundName("Currency")).getMapping(homeCurrencyKey)));
            props.add(new ImportProperty(rateField, LM.findLCPByCompoundName("rateExchange").getMapping(typeExchangeRUKey, currencyKey, dateField)));

            props.add(new ImportProperty(typeExchangeForeignField, LM.findLCPByCompoundName("nameTypeExchange").getMapping(typeExchangeForeignKey)));
            props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("currencyTypeExchange").getMapping(typeExchangeForeignKey),
                    LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));
            props.add(new ImportProperty(foreignRateField, LM.findLCPByCompoundName("rateExchange").getMapping(typeExchangeForeignKey, homeCurrencyKey, dateField)));

            List<List<Object>> data = new ArrayList<List<Object>>();
            for (Exchange e : exchangesList) {
                data.add(Arrays.asList((Object) "ЦБРФ (RUB)", "ЦБРФ (" + e.currencyID + ")", e.currencyID, e.homeCurrencyID, e.exchangeRate, new BigDecimal(1 / e.exchangeRate.doubleValue()), e.date));
            }
            ImportTable table = new ImportTable(Arrays.asList(typeExchangeRUField, typeExchangeForeignField, currencyField,
                    homeCurrencyField, rateField, foreignRateField, dateField), data);

            DataSession session = context.getSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(typeExchangeRUKey,
                    typeExchangeForeignKey, currencyKey, homeCurrencyKey), props);
            service.synchronize(true, false);
        }
    }

    private List<Exchange> importExchangesFromXML(Date dateFrom, Date dateTo, String extraSIDCurrency, ExecutionContext context) throws IOException, JDOMException, ParseException, ScriptingErrorLog.SemanticErrorException, SQLException {
        SAXBuilder builder = new SAXBuilder();

        List<Exchange> exchangesList = new ArrayList<Exchange>();

        Document document = builder.build(new URL("http://www.cbr.ru/scripts/XML_val.asp?d=0").openStream());
        Element rootNode = document.getRootElement();
        List list = rootNode.getChildren("Item");

        for (int i = 0; i < list.size(); i++) {

            Element node = (Element) list.get(i);

            String id = node.getAttributeValue("ID");

            if (extraSIDCurrency.equals(id)) {
                Document exchangeDocument = builder.build(new URL("http://www.cbr.ru/scripts/XML_dynamic.asp?date_req1="
                        + new SimpleDateFormat("dd/MM/yyyy").format(dateFrom)
                        + "&date_req2=" + new SimpleDateFormat("dd/MM/yyyy").format(dateTo)
                        + "&VAL_NM_RQ=" + id).openStream());
                Element exchangeRootNode = exchangeDocument.getRootElement();
                List exchangeList = exchangeRootNode.getChildren("Record");

                String shortNameCurrency = (String) LM.findLCPByCompoundName("shortNameCurrency").read(context, new DataObject(LM.findLCPByCompoundName("currencyExtraSID").read(context, new DataObject(extraSIDCurrency)), (ConcreteClass) LM.findClassByCompoundName("Currency")));

                for (int j = 0; j < exchangeList.size(); j++) {

                    Element exchangeNode = (Element) exchangeList.get(j);

                    BigDecimal value = new BigDecimal(Double.valueOf(exchangeNode.getChildText("Value").replace(",", ".")) / Double.valueOf(exchangeNode.getChildText("Nominal")));

                    exchangesList.add(new Exchange(shortNameCurrency, "RUB",
                            new Date(DateUtils.parseDate(exchangeNode.getAttributeValue("Date"), new String[]{"dd.MM.yyyy"}).getTime()),
                            value));
                }
                if (exchangesList.size() > 0)
                    return exchangesList;
            }
        }
        return exchangesList;
    }


}