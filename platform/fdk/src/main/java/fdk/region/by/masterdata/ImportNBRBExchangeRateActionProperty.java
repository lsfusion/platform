package fdk.region.by.masterdata;


import org.apache.commons.lang.time.DateUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
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
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ImportNBRBExchangeRateActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface currencyInterface;

    public ImportNBRBExchangeRateActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, new ValueClass[]{LM.findClassByCompoundName("Currency")});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        currencyInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            DataObject currencyObject = context.getKeyValue(currencyInterface);

            String shortNameCurrency = (String) LM.findLCPByCompoundName("shortNameCurrency").read(context, currencyObject);
            Date nbrbDateFrom = (Date) LM.findLCPByCompoundName("importNBRBExchangeRateDateFrom").read(context);
            Date nbrbDateTo = (Date) LM.findLCPByCompoundName("importNBRBExchangeRateDateTo").read(context);

            if (nbrbDateFrom != null && nbrbDateTo != null && shortNameCurrency != null)
                importExchanges(nbrbDateFrom, nbrbDateTo, shortNameCurrency, context);

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

    private void importExchanges(Date dateFrom, Date dateTo, String shortNameCurrency, ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, IOException, JDOMException, SQLException, ParseException {


        List<Exchange> exchangesList = importExchangesFromXML(dateFrom, dateTo, shortNameCurrency);

        if (exchangesList != null) {

            ImportField typeExchangeBYRField = new ImportField(LM.findLCPByCompoundName("name"));
            ImportField typeExchangeForeignField = new ImportField(LM.findLCPByCompoundName("name"));
            ImportField currencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));
            ImportField homeCurrencyField = new ImportField(LM.findLCPByCompoundName("shortNameCurrency"));
            ImportField rateField = new ImportField(LM.findLCPByCompoundName("rateExchange"));
            ImportField foreignRateField = new ImportField(LM.findLCPByCompoundName("rateExchange"));
            ImportField dateField = new ImportField(DateClass.instance);

            ImportKey<?> typeExchangeBYRKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("TypeExchange"),
                    LM.findLCPByCompoundName("typeExchangeName").getMapping(typeExchangeBYRField));

            ImportKey<?> typeExchangeForeignKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("TypeExchange"),
                    LM.findLCPByCompoundName("typeExchangeName").getMapping(typeExchangeForeignField));

            ImportKey<?> currencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Сurrency"),
                    LM.findLCPByCompoundName("currencyShortName").getMapping(currencyField));

            ImportKey<?> homeCurrencyKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("Сurrency"),
                    LM.findLCPByCompoundName("currencyShortName").getMapping(homeCurrencyField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(typeExchangeBYRField, LM.findLCPByCompoundName("name").getMapping(typeExchangeBYRKey)));
            props.add(new ImportProperty(homeCurrencyField, LM.findLCPByCompoundName("currencyTypeExchange").getMapping(typeExchangeBYRKey),
                    LM.object(LM.findClassByCompoundName("Currency")).getMapping(homeCurrencyKey)));
            props.add(new ImportProperty(rateField, LM.findLCPByCompoundName("rateExchange").getMapping(typeExchangeBYRKey, currencyKey, dateField)));

            props.add(new ImportProperty(typeExchangeForeignField, LM.findLCPByCompoundName("name").getMapping(typeExchangeForeignKey)));
            props.add(new ImportProperty(currencyField, LM.findLCPByCompoundName("currencyTypeExchange").getMapping(typeExchangeForeignKey),
                    LM.object(LM.findClassByCompoundName("Currency")).getMapping(currencyKey)));
            props.add(new ImportProperty(foreignRateField, LM.findLCPByCompoundName("rateExchange").getMapping(typeExchangeForeignKey, homeCurrencyKey, dateField)));

            List<List<Object>> data = new ArrayList<List<Object>>();
            for (Exchange e : exchangesList) {
                data.add(Arrays.asList((Object) "НБРБ (BYR)", "НБРБ (" + e.currencyID + ")", e.currencyID, e.homeCurrencyID, e.exchangeRate, 1/e.exchangeRate, e.date));
            }
            ImportTable table = new ImportTable(Arrays.asList(typeExchangeBYRField, typeExchangeForeignField, currencyField,
                    homeCurrencyField, rateField, foreignRateField, dateField), data);

            DataSession session = context.getSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(typeExchangeBYRKey,
                    typeExchangeForeignKey, currencyKey, homeCurrencyKey), props);
            service.synchronize(true, false);
            //session.apply(LM.getBL());
            //session.close();
        }
    }

    private List<Exchange> importExchangesFromXML(Date dateFrom, Date dateTo, String shortNameCurrency) throws IOException, JDOMException, ParseException {
        SAXBuilder builder = new SAXBuilder();

        List<Exchange> exchangesList = new ArrayList<Exchange>();

        Document document = builder.build(new URL("http://www.nbrb.by/Services/XmlExRatesRef.aspx").openStream());
        Element rootNode = document.getRootElement();
        List list = rootNode.getChildren("Currency");

        for (int i = 0; i < list.size(); i++) {

            Element node = (Element) list.get(i);

            String charCode = node.getChildText("CharCode");
            String id = node.getAttributeValue("Id");

            if (shortNameCurrency.equals(charCode)) {
                Document exchangeDocument = builder.build(new URL("http://www.nbrb.by/Services/XmlExRatesDyn.aspx?curId=" + id
                        + "&fromDate=" + new SimpleDateFormat("MM/dd/yyyy").format(dateFrom)
                        + "&toDate=" + new SimpleDateFormat("MM/dd/yyyy").format(dateTo)).openStream());
                Element exchangeRootNode = exchangeDocument.getRootElement();
                List exchangeList = exchangeRootNode.getChildren("Record");

                for (int j = 0; j < exchangeList.size(); j++) {

                    Element exchangeNode = (Element) exchangeList.get(j);

                    exchangesList.add(new Exchange(charCode, "BLR", new Date(DateUtils.parseDate(exchangeNode.getAttributeValue("Date"), new String[]{"MM/dd/yyyy"}).getTime()),
                            Double.valueOf(exchangeNode.getChildText("Rate"))));
                }
                if (exchangesList.size() > 0)
                    return exchangesList;
            }
        }
        return exchangesList;
    }


}