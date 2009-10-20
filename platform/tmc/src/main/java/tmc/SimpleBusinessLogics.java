package tmc;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import platform.interop.UserInfo;
import platform.interop.Compare;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.data.Union;
import platform.server.data.classes.*;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.properties.AggregateProperty;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.logics.properties.groups.AbstractGroup;
import platform.server.logics.linear.properties.*;
import platform.server.view.form.client.DefaultFormView;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.*;
import platform.base.BaseUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.registry.LocateRegistry;
import java.sql.SQLException;
import java.util.*;

public class SimpleBusinessLogics extends BusinessLogics<TmcBusinessLogics> {

    public SimpleBusinessLogics(DataAdapter iAdapter,int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(iAdapter,port);
    }

    public SimpleBusinessLogics(DataAdapter iAdapter,int testType,Integer seed,int iterations) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException {
        super(iAdapter,testType,seed,iterations);
    }

//    static Registry registry;
    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, FileNotFoundException, JRException, MalformedURLException {

        System.out.println("Server is starting...");
        DataAdapter adapter = new PostgreDataAdapter("testplat","server","postgres","sergtsop");
        SimpleBusinessLogics BL = new SimpleBusinessLogics(adapter,7652);

//        if(args.length>0 && args[0].equals("-F"))
//        BL.fillData();
        LocateRegistry.createRegistry(7652).rebind("BusinessLogics", BL);
//        Naming.rebind("rmi://127.0.0.1:1099/TmcBusinessLogics",new TmcBusinessLogics());
        System.out.println("Server has successfully started");
    }


    AbstractGroup documentGroup, fixedGroup, currentGroup, lastDocumentGroup;

    protected void initGroups() {

        documentGroup = new AbstractGroup("Параметры транзакции");
        fixedGroup = new AbstractGroup("Текущие Параметры транзакции");
        currentGroup = new AbstractGroup("Текущие параметры");
        lastDocumentGroup = new AbstractGroup("Последние параметры");
    }

    AbstractCustomClass document, quantityDocument, incomeDocument, outcomeDocument, extOutcomeDocument,
                        saleDocument, articleDocument, priceOutDocument;

    ConcreteCustomClass article, articleGroup, store, customer, supplier, extIncomeDocument, intraDocument, exchangeDocument, specification,
                        cashSaleDocument, clearingSaleDocument, invDocument, returnDocument, revalDocument, taxDocument, locTaxDocument, format, region;
    
    protected void initClasses() {

        article = addConcreteClass("Товар", namedObject);
        articleGroup = addConcreteClass("Группа товаров", namedObject);

        store = addConcreteClass("Склад", namedObject);

        supplier = addConcreteClass("Поставщик", namedObject);
        customer = addConcreteClass("Покупатель", namedObject);
        format = addConcreteClass("Формат", namedObject);
        region = addConcreteClass("Регион", namedObject);

        document = addAbstractClass("Документ", namedObject, transaction);
        priceOutDocument = addAbstractClass("Документ изм. цены", document);
        quantityDocument = addAbstractClass("Документ перемещения", document);
        incomeDocument = addAbstractClass("Приходный документ", quantityDocument, priceOutDocument);
        outcomeDocument = addAbstractClass("Расходный документ", quantityDocument);
        articleDocument = addAbstractClass("Перемещение товара", quantityDocument);

        extIncomeDocument = addConcreteClass("Внешний приход", incomeDocument, articleDocument);

        intraDocument = addConcreteClass("Внутреннее перемещение", incomeDocument, outcomeDocument, articleDocument);
        extOutcomeDocument = addAbstractClass("Внешний расход", outcomeDocument);
        exchangeDocument = addConcreteClass("Пересорт", outcomeDocument);

        saleDocument = addAbstractClass("Реализация", extOutcomeDocument, articleDocument);
        cashSaleDocument = addConcreteClass("Реализация по кассе", saleDocument);
        clearingSaleDocument = addConcreteClass("Реализация по б/н расчету", saleDocument);

        invDocument = addConcreteClass("Инвентаризация", extOutcomeDocument);

        returnDocument = addConcreteClass("Возврат поставщику", extOutcomeDocument, articleDocument);

        revalDocument = addConcreteClass("Переоценка", priceOutDocument);
        taxDocument = addConcreteClass("Изменение НДС", priceOutDocument);
        locTaxDocument = addConcreteClass("Изменение местн. нал.", priceOutDocument);

        specification = addConcreteClass("Спецификация",namedObject);
    }

    LDP artGroup, incStore, outStore, extIncSupplier, extIncPriceIn, extIncVATIn, invDBBalance,
            returnSupplier, clearingSaleCustomer, articleQuantity, extIncDetailDocument;

    LP quantity, roundm1, addPercent, docIncBalanceQuantity, docOutBalanceQuantity, revalFormat, storeFormat, locTaxRegion, storeRegion,
            invBalance, exchangeQuantity, exchIncQuantity, exchOutQuantity, priceOutChange, balanceStoreQuantity, revalAdd, taxVatOut, locTaxValue;
    LP currentIncDate, currentIncDoc, currentRevalDate, currentRevalDoc, currentTaxDate, currentTaxDoc, currentLocTaxDate, currentLocTaxDoc,
            currentExtIncDate, currentExtIncDoc, currentSupplier, docOutPriceOut;

    LP remainStoreArticleStartQuantity, remainStoreArticleEndQuantity, incBetweenDateQuantity, outBetweenDateQuantity,
        saleStoreArticleBetweenDateQuantity, saleArticleBetweenDateQuantity;

    LP currentAdd,currentVatOut,currentLocTax,currentPriceIn,currentPriceOut;

    LP initDateBalance(LP dateAnd, String caption) {
        LP dateQuantity = addJProp("Кол-во "+caption, and1, quantity, 1, 2, dateAnd, 1, 3);
        LP incDateQuantity = addSGProp("Прих. на скл. "+caption, dateQuantity, incStore, 1, 2, 3);
        LP outDateQuantity = addSGProp("Расх. со скл. "+caption, dateQuantity, outStore, 1, 2, 3);
        return addDUProp("Ост. на скл. "+caption, incDateQuantity, outDateQuantity);
    }

    private LP calcPriceOut(AbstractGroup group, String caption, LP priceIn, LP add, LP vatOut, Object[] priceParams, Object[] addParams) {
        int intNum = getIntNum(BaseUtils.add(priceParams,addParams));
        Object[] interfaces = new Object[intNum];
        for(int i=0;i<intNum;i++)
            interfaces[i] = i+1;
        LJP priceAdd = addJProp("Цена с надбавкой", addPercent, BaseUtils.add(
                BaseUtils.add(new Object[]{priceIn}, priceParams), BaseUtils.add(new Object[]{add}, addParams)));
        LJP priceVatOut = addJProp("Цена с НДС", addPercent, BaseUtils.add(
                BaseUtils.add(new Object[]{priceAdd}, interfaces), BaseUtils.add(new Object[]{vatOut}, addParams)));
        return addJProp(group, caption, roundm1, BaseUtils.add(new Object[]{priceVatOut}, interfaces));
    }

    protected void initProperties() {

        // абстрактные св-ва объектов
        LP percent = addSFProp("((prm1*prm2)/100)", DoubleClass.instance, 2);
        LP revPercent = addSFProp("((prm1*prm2)/(100+prm2))", DoubleClass.instance, 2);
        addPercent = addSFProp("((prm1*(100+prm2))/100)", DoubleClass.instance, 2);
        LP round = addSFProp("round(CAST(prm1 as numeric),0)", DoubleClass.instance, 1);
        roundm1 = addSFProp("round(CAST(prm1 as numeric),-1)", DoubleClass.instance, 1);
        LP multiplyDouble2 = addMFProp(DoubleClass.instance,2);

        // свойства товара
        artGroup = addDProp("artGroup", "Гр. тов.", articleGroup, article);
        LP artGroupName = addJProp(baseGroup, "Имя гр. тов.", name, artGroup, 1);

        LP artBarCode = addDProp(baseGroup, "Штрих-код", NumericClass.get(13, 0), article);
        LP artWeight = addDProp(baseGroup, "Вес (кг.)", NumericClass.get(6, 3), article);
        LP artPackCount = addDProp(baseGroup, "Кол-во в уп.", IntegerClass.instance, article);

        article.externalID = (DataProperty)artBarCode.property;

        extIncSupplier = addDProp("Поставщик", supplier, extIncomeDocument); // внешний приход
        LP extIncSupplierName = addJProp(baseGroup, "extIncSupplierName", "Имя поставщика", name, extIncSupplier, 1);

        clearingSaleCustomer = addDProp("clearingSaleCustomer","Покупатель", customer, clearingSaleDocument); // реализация по б\н расчету
        LP clearingSaleCustomerName = addJProp(baseGroup, "Имя покупателя", name, clearingSaleCustomer, 1);

        returnSupplier = addDProp("returnSupplier","Поставщик", supplier, returnDocument); // возврат поставщику
        LP returnSupplierName = addJProp(baseGroup, "Имя поставщика", name, returnSupplier, 1);

        revalFormat = addDProp(baseGroup, "revalFormat","Формат", format, revalDocument);
        storeFormat = addDProp(baseGroup, "storeFormat","Формат склада", format, store);

        locTaxRegion = addDProp(baseGroup, "locTaxRegion","Регион", region, locTaxDocument);
        storeRegion = addDProp(baseGroup, "storeRegion","Регион склада", region, store);

        LDP specSupplier = addDProp(baseGroup, "specSupplier", "Спец. пост." , supplier, specification);
        LDP storeSpec = addDProp(baseGroup, "storeSpec", "Спец. скл. пост.", specification, store, supplier);
        LP storeSpecAggr = addJProp(baseGroup, "storeSpecAggr", "В спец. пост.", diff2, 2,
                                addJProp("",specSupplier,storeSpec, 1, 2), 1, 2);
        storeSpecAggr.property.isFalse = true;
        addMCProp(baseGroup,storeSpecAggr,storeSpec);

        // сравнение дат
        LP groeqDocDate = addJProp("Дата док.>=Дата", groeq2, date, 1, object(DateClass.instance), 2);
        LP greaterDocDate = addJProp("Дата док.>Дата", greater2, date, 1, object(DateClass.instance), 2);
        LP betweenDocDate = addJProp("Дата док. между Дата", between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3);
        LP equalDocDate = addJProp("Дата док.=Дата", equals2, date, 1, object(DateClass.instance), 2);

        incStore = addDProp("incStore", "Склад (прих.)", store, incomeDocument);
        LP incStoreName = addJProp(baseGroup, "Имя склада (прих.)", name, incStore, 1);
        outStore = addDProp("outStore", "Склад (расх.)", store, outcomeDocument);
        LP outStoreName = addJProp(baseGroup, "Имя склада (расх.)", name, outStore, 1);

        // количества
        articleQuantity = addDProp("Кол-во товара", DoubleClass.instance, articleDocument, article);

        exchangeQuantity = addDProp(baseGroup, "exchangeQuantity", "Кол-во перес.", DoubleClass.instance, exchangeDocument, article, article);
        exchIncQuantity = addSGProp(baseGroup, "Прих. перес.", exchangeQuantity, 1, 3);
        exchOutQuantity = addSGProp(baseGroup, "Расх. перес.", exchangeQuantity, 1, 2);
        LP exchDltQuantity = addDUProp("Кол-во перес. (тов.)", exchOutQuantity, exchIncQuantity);

        invBalance = addDProp(baseGroup, "invBalance", "Остаток инв.", DoubleClass.instance, invDocument, article);
        invDBBalance = addDProp(baseGroup, "invDBBalance", "Остаток (по учету)", DoubleClass.instance, invDocument, article);

        LP invQuantity = addDUProp("invQuantity","Кол-во инв.", invDBBalance, invBalance);

        quantity = addCUProp(baseGroup, "quantity", "Кол-во", articleQuantity, exchDltQuantity, invQuantity);

        // кол-во по документу
        LP documentQuantity = addSGProp(baseGroup, "documentQuantity", "Кол-во (всего)", quantity, 1);

        // остатки
        LP incStoreQuantity = addSGProp("incStoreQuantity", "Прих. на скл.", quantity, incStore, 1, 2);
        LP outStoreQuantity = addSGProp("outStoreQuantity", "Расх. со скл.", quantity, outStore, 1, 2);
        balanceStoreQuantity = addDUProp(baseGroup, "balanceStoreQuantity", "Ост. на скл.", incStoreQuantity, outStoreQuantity);

        // остатки для документов
        docOutBalanceQuantity = addJProp(baseGroup, "Остаток (расх.)", balanceStoreQuantity, outStore, 1, 2);
        docIncBalanceQuantity = addJProp(baseGroup, "Остаток (прих.)", balanceStoreQuantity, incStore, 1, 2);

        invDBBalance.setDefProp(balanceStoreQuantity, outStore, 1, 2);

        // для отчетов св-ва за период
        LP dltStoreArticleGroeqDateQuantity = initDateBalance(groeqDocDate, "с даты");
        LP dltStoreArticleGreaterDateQuantity = initDateBalance(greaterDocDate, "после даты");
        remainStoreArticleStartQuantity = addDUProp(baseGroup, "Кол-во на начало", addJProp("", and1, balanceStoreQuantity, 1, 2, is(DateClass.instance), 3), dltStoreArticleGroeqDateQuantity);
        remainStoreArticleEndQuantity = addDUProp(baseGroup, "Кол-во на конец", addJProp("", and1, balanceStoreQuantity, 1, 2, is(DateClass.instance), 3), dltStoreArticleGreaterDateQuantity);

        LP betweenDateQuantity = addJProp("Кол-во за интервал", and1, quantity, 1, 2, betweenDocDate, 1, 3, 4);
        incBetweenDateQuantity = addSGProp(baseGroup, "Кол-во прих. на скл. за интервал", betweenDateQuantity, incStore, 1, 2, 3, 4);
        outBetweenDateQuantity = addSGProp(baseGroup, "Кол-во расх. со скл. за интервал", betweenDateQuantity, outStore, 1, 2, 3, 4);

        LP saleBetweenDateQuantity = addJProp("Кол-во реал. за интервал", and1, betweenDateQuantity, 1, 2, 3, 4, is(saleDocument), 1);
        saleStoreArticleBetweenDateQuantity = addSGProp(baseGroup, "Кол-во реал. на скл. за интервал", saleBetweenDateQuantity, outStore, 1, 2, 3, 4);
        saleArticleBetweenDateQuantity = addSGProp(baseGroup, "Реал. кол-во (по товару)", saleStoreArticleBetweenDateQuantity, 2, 3, 4);

        // посл. документ прихода от вн. пост.
        LP[] maxExtIncProps = addMGProp(lastDocumentGroup, new String[]{"currentExtIncDate","currentExtIncDoc"}, new String[]{"Дата посл. вн. прих.","Посл. вн. прих."}, 1,
                addJProp("Дата прих. (кол-во)",and1, addJProp("Дата (кол-во)", and1, date, 1, quantity, 1, 2), 1, 2, extIncSupplier, 1), 1, 2);
        currentExtIncDate = maxExtIncProps[0]; currentExtIncDoc = maxExtIncProps[1];
        currentSupplier = addJProp(currentGroup, "currentSupplier", "Тек. пост.", extIncSupplier, currentExtIncDoc, 1);
        
        // Надбавка
        revalAdd = addDProp(baseGroup, "revalAdd", "Надбавка", DoubleClass.instance, revalDocument, article);
        LP[] maxRevalProps = addMGProp(lastDocumentGroup, new String[]{"currentRevalDate","currentRevalDoc"}, new String[]{"Дата посл. переоц.","Посл. док. переоц."}, 1,
                addJProp("Дата переоц.", and1, date, 1, revalAdd, 1, 2), 1, 2, revalFormat, 1);
        currentRevalDate = maxRevalProps[0]; currentRevalDoc = maxRevalProps[1];
        currentAdd = addJProp(currentGroup, "currentAdd", "Надбавка (тек. форм.)", revalAdd, currentRevalDoc, 1, 2, 1);
        LP currentStoreAdd = addJProp(currentGroup, "Надбавка (тек. скл.)",currentAdd, 2, storeFormat, 1);
        addJProp(baseGroup, "Надбавка (тек. док.)", currentAdd, 2, revalFormat, 1);

        // НДС
        taxVatOut = addDProp(baseGroup, "taxVatOut", "НДС розн.", DoubleClass.instance, taxDocument, article);
        LP[] maxTaxProps = addMGProp(lastDocumentGroup, new String[]{"currentTaxDate","currentTexDoc"}, new String[]{"Дата посл. НДС","Посл. док. НДС"}, 1,
                addJProp("Дата НДС", and1, date, 1, taxVatOut, 1, 2), 1, 2);
        currentTaxDate = maxTaxProps[0]; currentTaxDoc = maxTaxProps[1];
        currentVatOut = addJProp(currentGroup, "НДС (тек.)", taxVatOut, currentTaxDoc, 1, 1);

        // Местный налог
        locTaxValue = addDProp(baseGroup, "locTaxValue", "Местн. нал.", DoubleClass.instance, locTaxDocument, article);
        LP[] maxLocTaxProps = addMGProp(lastDocumentGroup, new String[]{"currentLocTaxDate","currentLocTaxDoc"}, new String[]{"Дата посл. местн. нал.","Посл. док. местн. нал."}, 1,
                addJProp("Дата переоц.", and1, date, 1, locTaxValue, 1, 2), 1, 2, locTaxRegion, 1);
        currentLocTaxDate = maxLocTaxProps[0]; currentLocTaxDoc = maxLocTaxProps[1];
        currentLocTax = addJProp(currentGroup, "currentLocTax", "Местн. нал. (тек. рег.)", locTaxValue, currentLocTaxDoc, 1, 2, 1);
        LP currentStoreLocTax = addJProp(currentGroup, "Местн. нал. (тек. скл.)",currentLocTax, 2, storeRegion, 1);
        addJProp(baseGroup, "Местн. нал. (тек. док.)", currentLocTax, 2, locTaxRegion, 1);

        // цены поставщика
        extIncPriceIn = addDProp(baseGroup, "extIncPriceIn", "Цена пост.", DoubleClass.instance, extIncomeDocument, article);
        extIncVATIn = addDProp(baseGroup, "extIncVATIn", "НДС (пост.)", DoubleClass.instance, extIncomeDocument, article);
        extIncVATIn.setDefProp(currentVatOut, 2, articleQuantity, 1, 2);

        LDP outPriceIn = addDProp("outPriceIn", "Цена пост. (расх.)", DoubleClass.instance, outcomeDocument, article);
        LP priceIn = addCUProp("priceIn", "Цена пост. (фикс.)", extIncPriceIn, outPriceIn);

        LP[] maxIncProps = addMGProp(lastDocumentGroup, new String[]{"currentIncDate","currentIncDoc"}, new String[]{"Дата посл. прих. по скл.","Посл. прих. по скл."}, 1,
                addJProp("Дата прих. (кол-во)", and1, date, 1, quantity, 1, 2), 1, incStore, 1, 2);
        currentIncDate = maxIncProps[0]; currentIncDoc = maxIncProps[1];
        currentPriceIn = addJProp(currentGroup, "currentPriceIn", "Цена пост. (тек.)", priceIn, currentIncDoc, 1, 2, 2);

        outPriceIn.setDefProp(currentPriceIn, outStore, 1, 2, quantity, 1, 2); // подставляем тек. цену со склада расх
        addSUProp(baseGroup, "Цена пост. (док. расх.)",Union.OVERRIDE, addJProp("Цена пост. (док. расх. тек.)", currentPriceIn, outStore, 1, 2), outPriceIn);

        extIncPriceIn.setDefProp(currentPriceIn, incStore, 1, 2, quantity, 1, 2);

        // розничная цена
        currentPriceOut = addJProp(currentGroup, "currentPriceOut", "Цена розн. (тек.)", roundm1,
                addJProp("Цена розн. (тек. - неокр.)", addPercent,
                        addJProp("Цена с НДС", addPercent,
                                addJProp("Цена с надбавкой", addPercent,
                                        currentPriceIn, 1, 2,
                                           currentStoreAdd, 1, 2), 1, 2,
                                           currentVatOut, 2), 1, 2,
                                           currentStoreLocTax, 1, 2), 1, 2);

        // прих. суммы по позициям
        LP extIncDetailSumIn = addJProp(baseGroup, "Сумма пост.", round,
                addJProp("", multiplyDouble2, quantity, 1, 2, extIncPriceIn, 1, 2), 1, 2);
        LP extIncDetailSumVATIn = addJProp(baseGroup, "Сумма НДС", round,
                addJProp("", percent, extIncDetailSumIn, 1, 2, extIncVATIn, 1, 2), 1, 2);
        LP extIncDetailSumPay = addSUProp(baseGroup, "Всего с НДС", Union.SUM, extIncDetailSumIn, extIncDetailSumVATIn);

        // прих. суммы по документам
        LP extIncDocumentSumIn = addSGProp(baseGroup, "Сумма пост.", extIncDetailSumIn, 1);
        LP extIncDocumentSumVATIn = addSGProp(baseGroup, "Сумма НДС (пост.)", extIncDetailSumVATIn, 1);
        LP extIncDocumentSumPay = addSUProp(baseGroup, "Всего с НДС", Union.SUM, extIncDocumentSumIn, extIncDocumentSumVATIn);

        // расх. суммы по позициям
        LDP documentVatOut = addDProp("documentVatOut", "НДС (фикс.)", DoubleClass.instance, outcomeDocument, article);
        documentVatOut.setDefProp(currentVatOut, 2, quantity, 1, 2);
        addSUProp(baseGroup, "НДС (док.)", Union.OVERRIDE, addJProp("",and1,currentVatOut,2,is(outcomeDocument),1), documentVatOut);
        LDP outPriceOut = addDProp("outPriceOut", "Цена розн. (расх.)", DoubleClass.instance, outcomeDocument, article);
        outPriceOut.setDefProp(currentPriceOut, outStore, 1, 2, quantity, 1, 2);
        docOutPriceOut = addSUProp(baseGroup, "Цена розн. (док. расх.)",Union.OVERRIDE, addJProp("Цена розн. (док. расх. тек.)", currentPriceOut, outStore, 1, 2), outPriceOut);
        
        LP detailSumOut = addJProp(baseGroup, "detailSumOut", "Сумма розн. (расх.)", round,
                addJProp("", multiplyDouble2, quantity, 1, 2, outPriceOut, 1, 2), 1, 2);
        LP detailSumVatOut = addJProp(baseGroup, "Сумма НДС (розн. расх.)", round,
                addJProp("", percent, detailSumOut, 1, 2, documentVatOut, 1, 2), 1, 2);

        // расх. суммы по документам
        LP documentSumOut = addSGProp(baseGroup, "documentSumOut", "Сумма розн.", detailSumOut, 1);
        LP documentSaleVatOut = addSGProp(baseGroup, "documentSaleVatOut", "Сумма НДС (розн.)", detailSumVatOut, 1);

        // изменение цен
        LDP incPriceOutChange = addDProp("incPriceOutChange", "Цена розн. (прих.)", DoubleClass.instance, incomeDocument, article);
        incPriceOutChange.setDefProp(currentPriceOut, true, incStore, 1, 2, quantity, 1, 2);

        LDP revalPriceOutChange = addDProp(baseGroup, "revalPriceOutChange", "Цена розн. (переоц.)", DoubleClass.instance, revalDocument, store, article);
        revalPriceOutChange.setDefProp(currentPriceOut, true, 2, 3, revalAdd, 1, 3, addJProp("Склад документа (форм.)", equals2, revalFormat, 1, storeFormat, 2), 1, 2);

        LDP taxPriceOutChange = addDProp(baseGroup, "taxPriceOutChange", "Цена розн. (НДС)", DoubleClass.instance, taxDocument, store, article);
        taxPriceOutChange.setDefProp(currentPriceOut, true, 2, 3, taxVatOut, 1, 3);

        LDP locTaxPriceOutChange = addDProp(baseGroup, "locTaxPriceOutChange", "Цена розн. (местн. нал.)", DoubleClass.instance, locTaxDocument, store, article);
        locTaxPriceOutChange.setDefProp(currentPriceOut, true, 2, 3, locTaxValue, 1, 3, addJProp("Склад документа (рег.)", equals2, locTaxRegion, 1, storeRegion, 2), 1, 2);

        // цена для документа
        priceOutChange = addCUProp(baseGroup,"Цена розн. по док.", revalPriceOutChange, taxPriceOutChange, locTaxPriceOutChange,
                                        addJProp("Цена розн. по скл. (прих.)", and1,  // цена для прихода склада - вытащим склад в интерфейс, для объединения с reval
                                                incPriceOutChange, 1, 3,
                                            addJProp("Склад документа", equals2, incStore, 1, 2), 1, 2));
    }

    protected void initConstraints() {

//        Constraints.put(balanceStoreQuantity.Property,new PositiveConstraint());
    }

    protected void initPersistents() {

//        persistents.add((AggregateProperty)quantity.property);
        persistents.add((AggregateProperty)balanceStoreQuantity.property);

        persistents.add((AggregateProperty)currentExtIncDate.property);
        persistents.add((AggregateProperty)currentExtIncDoc.property);
        persistents.add((AggregateProperty)currentIncDate.property);
        persistents.add((AggregateProperty)currentIncDoc.property);
        persistents.add((AggregateProperty)currentRevalDate.property);
        persistents.add((AggregateProperty)currentRevalDoc.property);
        persistents.add((AggregateProperty)currentTaxDate.property);
        persistents.add((AggregateProperty)currentTaxDoc.property);
        persistents.add((AggregateProperty)currentLocTaxDate.property);
        persistents.add((AggregateProperty)currentLocTaxDoc.property);

        persistents.add((AggregateProperty)currentAdd.property);
        persistents.add((AggregateProperty)currentVatOut.property);
        persistents.add((AggregateProperty)currentLocTax.property);
        persistents.add((AggregateProperty)currentPriceIn.property);
        
        persistents.add((AggregateProperty)currentPriceOut.property);
    }

    protected void initTables() {

        tableFactory.include("article",article);
        tableFactory.include("document", document);
        tableFactory.include("store",store);
        tableFactory.include("articlegroup",articleGroup);
        tableFactory.include("articledocument",article, document);
        tableFactory.include("articlestore",article,store);
    }

    protected void initIndexes() {
    }

    NavigatorForm extIncDetailForm, extIncForm;
    NavigatorForm intraForm;
    NavigatorForm extOutForm, cashSaleForm, receiptForm, clearingSaleForm, invForm, invStoreForm, returnForm;
    NavigatorForm exchangeForm, exchangeMForm;
    NavigatorForm revalueForm, revalueStoreForm;
    NavigatorElement aggregateData;
    NavigatorElement aggrStoreData;
    NavigatorForm storeArticleForm, storeArticlePrimDocForm, storeArticleDocForm;
    NavigatorElement aggrArticleData;
    NavigatorForm articleStoreForm, articleMStoreForm;
    NavigatorElement aggrSupplierData;
    NavigatorForm supplierStoreArticleForm;
    NavigatorElement analyticsData;
    NavigatorElement dateIntervalForms;
    NavigatorForm mainAccountForm, salesArticleStoreForm;

    protected void initNavigators() throws JRException, FileNotFoundException {

        createDefaultClassForms(baseClass, baseElement);

        NavigatorElement primaryData = new NavigatorElement(baseElement, 100, "Первичные данные");
            NavigatorForm extIncDetailForm = new ExtIncNavigatorForm(primaryData, 110, "Внешний приход");
//                NavigatorForm extIncPrintForm = new ExtIncPrintNavigatorForm(extIncDetailForm, 117, "Реестр цен");
            NavigatorForm intraForm = new IntraNavigatorForm(primaryData, 120, "Внутреннее перемещение");
            NavigatorForm extOutForm = new ExtOutNavigatorForm(primaryData, 130, "Внешний расход");
                NavigatorForm cashSaleForm = new CashSaleNavigatorForm(extOutForm, 131, "Реализация по кассе");
                NavigatorForm clearingSaleForm = new ClearingSaleNavigatorForm(extOutForm, 132, "Реализация по б/н расчету");
                NavigatorForm invForm = new InvNavigatorForm(extOutForm, 134, "Инвентаризация", false);
                NavigatorForm returnForm = new ReturnNavigatorForm(extOutForm, 136, "Возврат поставщику");
            NavigatorForm exchangeForm = new ExchangeNavigatorForm(primaryData, 140, "Пересорт");
                NavigatorForm exchangeMForm = new ExchangeMNavigatorForm(exchangeForm, 142, "Сводный пересорт");
            NavigatorForm revalueForm = new RevalueNavigatorForm(primaryData, 150, "Переоценка");
            NavigatorForm taxForm = new TaxNavigatorForm(primaryData, 160, "Изменение НДС");
            NavigatorForm locTaxForm = new LocTaxNavigatorForm(primaryData, 170, "Изменение местн. нал.");


        NavigatorElement aggregateData = new NavigatorElement(baseElement, 200, "Сводная информация");
            NavigatorElement aggrStoreData = new NavigatorElement(aggregateData, 210, "Склады");
                NavigatorForm storeArticleForm = new StoreArticleNavigatorForm(aggrStoreData, 211, "Товары по складам");
                    NavigatorForm storeArticlePrimDocForm = new StoreArticlePrimDocNavigatorForm(storeArticleForm, 2111, "Товары по складам (изм. цен)");
                    NavigatorForm storeArticleDocForm = new StoreArticleDocNavigatorForm(storeArticleForm, 2112, "Товары по складам (док.)");
            NavigatorElement aggrArticleData = new NavigatorElement(aggregateData, 220, "Товары");
                NavigatorForm articleStoreForm = new ArticleStoreNavigatorForm(aggrArticleData, 221, "Склады по товарам");
                NavigatorForm articleMStoreForm = new ArticleMStoreNavigatorForm(aggrArticleData, 222, "Товары*Склады");
            NavigatorElement aggrSupplierData = new NavigatorElement(aggregateData, 230, "Поставщики");
                NavigatorForm supplierStoreArticleForm = new SupplierStoreArticleNavigatorForm(aggrSupplierData, 231, "Остатки по складам");

        analyticsData = new NavigatorElement(baseElement, 300, "Аналитические данные");
            NavigatorElement dateIntervalForms = new NavigatorElement(analyticsData, 310, "За интервал дат");
                NavigatorForm salesArticleStoreForm = new SalesArticleStoreNavigatorForm(dateIntervalForms, 313, "Реализация товара по складам");

        NavigatorForm storeSupplierSpec = new StoreSupplierSpecNavigatorForm(baseElement, 400, "Тест");

        extIncomeDocument.relevantElements.set(0, extIncDetailForm);
        intraDocument.relevantElements.set(0, intraForm);
        extOutcomeDocument.relevantElements.set(0, extOutForm);
        clearingSaleDocument.relevantElements.set(0, clearingSaleForm);
        invDocument.relevantElements.set(0, invForm);
        exchangeDocument.relevantElements.set(0, exchangeForm);
        revalDocument.relevantElements.set(0, revalueForm);

//        extIncDetailForm.addRelevantElement(extIncPrintForm);
    }

    private class TmcNavigatorForm extends NavigatorForm {

        TmcNavigatorForm(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);
        }

        TmcNavigatorForm(NavigatorElement parent, int iID, String caption, boolean isPrintForm) {
            super(parent, iID, caption, isPrintForm);
        }

        void addArticleRegularFilterGroup(PropertyObjectNavigator documentProp, PropertyObjectNavigator... extraProps) {

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
/*            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));*/
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(documentProp),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));

            int functionKey = KeyEvent.VK_F9;

            for (PropertyObjectNavigator extraProp : extraProps) {
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      new NotNullFilterNavigator(extraProp),
                                      extraProp.property.caption,
                                      KeyStroke.getKeyStroke(functionKey--, 0)));
            }
            addRegularFilterGroup(filterGroup);
        }
    }

    private class ExtIncDocumentNavigatorForm extends TmcNavigatorForm {

        protected ObjectNavigator objDoc;

        ExtIncDocumentNavigatorForm(NavigatorElement parent, int iID, String caption, boolean isPrintForm) {
            super(parent, iID, caption, isPrintForm);

            objDoc = addSingleGroupObjectImplement(extIncomeDocument, "Документ", properties, baseGroup);
        }
    }

    private class ExtIncNavigatorForm extends ExtIncDocumentNavigatorForm {

        public ExtIncNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption, false);

            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view);
        }
    }
/*
    private class ExtIncPrintNavigatorForm extends ExtIncNavigatorForm {

        public ExtIncPrintNavigatorForm(NavigatorElement parent, int ID, String caption) throws JRException, FileNotFoundException {
            super(parent, ID, caption, true);

            objDoc.groupTo.gridClassView = false;
            objDoc.groupTo.singleViewType = true;

            addPropertyView(objDoc, properties, baseGroup);
            addPropertyView(objDetail, properties, baseGroup);

            objDoc.sID = "objDoc";
            getPropertyView(name.property, objDoc.groupTo).sID = "docName";

            //
            reportDesign = JRXmlLoader.load(getClass().getResourceAsStream("/tmc/reports/extIncLog.jrxml"));
        }
    }
  */
    private class IntraNavigatorForm extends TmcNavigatorForm {

        public IntraNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(intraDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view,
                                         getPropertyView(docIncBalanceQuantity.property).view);

            addHintsNoUpdate(currentIncDate.property);
            addHintsNoUpdate(currentIncDoc.property);
        }
    }

    private class ExtOutNavigatorForm extends TmcNavigatorForm {

        public ExtOutNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(extOutcomeDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class CashSaleNavigatorForm extends TmcNavigatorForm {

        public CashSaleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(cashSaleDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class ClearingSaleNavigatorForm extends TmcNavigatorForm {

        public ClearingSaleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(clearingSaleDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class InvNavigatorForm extends TmcNavigatorForm {

        public InvNavigatorForm(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(invDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class ReturnNavigatorForm extends TmcNavigatorForm {

        public ReturnNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(returnDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class ExchangeNavigatorForm extends TmcNavigatorForm {

        public ExchangeNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(exchangeDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArtTo = addSingleGroupObjectImplement(article, "Товар (на)", properties,
                                                                        baseGroup);
            ObjectNavigator objArtFrom = addSingleGroupObjectImplement(article, "Товар (c)", properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArtTo, properties, baseGroup);
            addPropertyView(exchangeQuantity, objDoc, objArtFrom, objArtTo);
            addPropertyView(objDoc, objArtFrom, properties, baseGroup);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
/*            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));*/
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(exchIncQuantity.property).view),
                                  "Приход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(exchOutQuantity.property).view),
                                  "Расход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyView(docOutBalanceQuantity.property, objArtTo.groupTo).view, Compare.NOT_EQUALS, 0),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyView(docOutBalanceQuantity.property, objArtTo.groupTo).view, Compare.LESS, 0),
                                  "Отр. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroup);

            filterGroup = new RegularFilterGroupNavigator(IDShift(1));
/*            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.SHIFT_DOWN_MASK)));*/
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(exchangeQuantity.property).view),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyView(docOutBalanceQuantity.property, objArtFrom.groupTo).view, Compare.NOT_EQUALS, 0),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyView(docOutBalanceQuantity.property, objArtFrom.groupTo).view, Compare.GREATER, 0),
                                  "Пол. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyView(docOutPriceOut.property, objArtFrom.groupTo).view, Compare.EQUALS, getPropertyView(docOutPriceOut.property, objArtTo.groupTo).view),
                                  "Одинаковая розн. цена",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);

        }
    }

    private class ExchangeMNavigatorForm extends TmcNavigatorForm {

        public ExchangeMNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(exchangeDocument, "Документ", properties, baseGroup);

            GroupObjectNavigator gobjArts = new GroupObjectNavigator(IDShift(1));

            ObjectNavigator objArtTo = new ObjectNavigator(IDShift(1), article, "Товар (на)");
            ObjectNavigator objArtFrom = new ObjectNavigator(IDShift(1), article, "Товар (с)");

            gobjArts.add(objArtTo);
            gobjArts.add(objArtFrom);
            addGroup(gobjArts);

            addPropertyView(properties, baseGroup, false, objArtTo);
            addPropertyView(properties, baseGroup, false, objArtFrom);
            addPropertyView(exchangeQuantity, objDoc, objArtFrom, objArtTo);

            addFixedFilter(new NotNullFilterNavigator(getPropertyView(exchangeQuantity.property).view));
        }
    }

    private class RevalueNavigatorForm extends TmcNavigatorForm {

        public RevalueNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(revalDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            addArticleRegularFilterGroup(getPropertyView(revalAdd.property).view);

            addHintsNoUpdate(currentRevalDate.property);
            addHintsNoUpdate(currentRevalDoc.property);
        }
    }

    private class TaxNavigatorForm extends TmcNavigatorForm {

        public TaxNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(taxDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, currentVatOut);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            addArticleRegularFilterGroup(getPropertyView(taxVatOut.property).view);

            addHintsNoUpdate(currentTaxDate.property);
            addHintsNoUpdate(currentTaxDoc.property);
        }
    }

    private class LocTaxNavigatorForm extends TmcNavigatorForm {

        public LocTaxNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(locTaxDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup);

            addPropertyView(objDoc, objArt, properties, baseGroup);

            addArticleRegularFilterGroup(getPropertyView(locTaxValue.property).view);

            addHintsNoUpdate(currentLocTaxDate.property);
            addHintsNoUpdate(currentLocTaxDoc.property);
        }
    }

    private class StoreArticleNavigatorForm extends TmcNavigatorForm {

        ObjectNavigator objStore, objArt;

        public StoreArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objStore = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup);
            objStore.groupTo.gridClassView = false;
            objStore.groupTo.singleViewType = true;

            objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup);

            addPropertyView(objStore, objArt, properties, baseGroup);
        }
    }

    private class StoreArticlePrimDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticlePrimDocNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objPrimDoc = addSingleGroupObjectImplement(priceOutDocument, "Документ", properties, baseGroup);

            addPropertyView(objPrimDoc, objArt, properties, baseGroup);

            addPropertyView(objPrimDoc, objStore, objArt, properties, priceOutChange);

            addPropertyView(objArt, properties, lastDocumentGroup, currentGroup);

            addPropertyView(objStore, objArt, properties, lastDocumentGroup, currentGroup);

            addFixedFilter(new NotNullFilterNavigator(getPropertyView(priceOutChange.property).view));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(getPropertyView(date.property), false);
            formView.defaultOrders.put(objPrimDoc, false);
            richDesign = formView;
        }
    }

    private class StoreArticleDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticleDocNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(quantityDocument, "Товарный документ", properties, baseGroup, date, incStore, outStore);

            addPropertyView(objDoc, objArt, properties, baseGroup);
//            addPropertyView(properties, baseGroup, true, objDoc);

            addFixedFilter(new NotNullFilterNavigator(getPropertyView(quantity.property).view));
            addFixedFilter(new OrFilterNavigator(
                                new CompareFilterNavigator(getPropertyView(incStore.property).view,Compare.EQUALS,objStore),
                                new CompareFilterNavigator(getPropertyView(outStore.property).view,Compare.EQUALS,objStore)));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(getPropertyView(date.property), false);
            richDesign = formView;
        }
    }

    private class ArticleStoreNavigatorForm extends TmcNavigatorForm {

        ObjectNavigator objStore, objArt;

        public ArticleStoreNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup);
            objStore = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup);

            addPropertyView(objStore, objArt, properties, baseGroup);
//            addPropertyView(properties, baseGroup, false, objArt.groupTo, objStore, objArt);
        }
    }

    private class ArticleMStoreNavigatorForm extends TmcNavigatorForm {

        public ArticleMStoreNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            GroupObjectNavigator gobjArtStore = new GroupObjectNavigator(IDShift(1));

            ObjectNavigator objArt = new ObjectNavigator(IDShift(1), article, "Товар");
            ObjectNavigator objStore = new ObjectNavigator(IDShift(1), store, "Склад");

            gobjArtStore.add(objArt);
            gobjArtStore.add(objStore);
            addGroup(gobjArtStore);

            addPropertyView(objArt, properties, baseGroup);
            addPropertyView(objStore, properties, baseGroup);

            addPropertyView(objStore, objArt, properties, baseGroup);
        }
    }

    private class SupplierStoreArticleNavigatorForm extends TmcNavigatorForm {

        SupplierStoreArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objSupplier = addSingleGroupObjectImplement(supplier, "Поставщик", properties, baseGroup);
            objSupplier.groupTo.gridClassView = false;
            objSupplier.groupTo.singleViewType = true;

            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup);
            objStore.groupTo.gridClassView = false;

            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objStore, objArt, properties, baseGroup);

            // установить фильтр по умолчанию на поставщик товара = поставщик
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(currentSupplier, objArt), Compare.EQUALS, objSupplier));

            // добавить стандартные фильтры
            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyView(balanceStoreQuantity.property).view, Compare.GREATER, 0),
                                  "Есть на складе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyView(balanceStoreQuantity.property).view, Compare.LESS_EQUALS, 0),
                                  "Нет на складе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }



    private class DateIntervalNavigatorForm extends TmcNavigatorForm {

        GroupObjectNavigator gobjInterval;
        ObjectNavigator objDateFrom, objDateTo;

        public DateIntervalNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            gobjInterval = new GroupObjectNavigator(IDShift(1));
            gobjInterval.gridClassView = false;
            gobjInterval.singleViewType = true;

            objDateFrom = new ObjectNavigator(IDShift(1), DateClass.instance, "С даты :");
            objDateTo = new ObjectNavigator(IDShift(1), DateClass.instance, "По дату :");

            gobjInterval.add(objDateFrom);
            gobjInterval.add(objDateTo);
            addGroup(gobjInterval);
        }
    }

    private class SalesArticleStoreNavigatorForm extends DateIntervalNavigatorForm {

        public SalesArticleStoreNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objArticle = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);
            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                        baseGroup);

            addPropertyView(saleArticleBetweenDateQuantity, objArticle, objDateFrom, objDateTo);

            addPropertyView(remainStoreArticleStartQuantity, objStore, objArticle, objDateFrom);
            addPropertyView(incBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
            addPropertyView(outBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
            addPropertyView(remainStoreArticleEndQuantity, objStore, objArticle, objDateTo);
            addPropertyView(saleStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
        }
    }

    protected void initAuthentication() {

        SecurityPolicy securityPolicy;

        User user1 = authPolicy.addUser("user1", "user1", new UserInfo("Петр", "Петров"));

        User user2 = authPolicy.addUser("user2", "user2", new UserInfo("Иван", "Иванов"));

        securityPolicy = new SecurityPolicy();

        securityPolicy.property.view.deny(currentGroup.getProperties());
//        securityPolicy.property.change.deny(extIncDetailArticle.property);
//        securityPolicy.property.change.deny(extIncDetailQuantity.property);

        securityPolicy.navigator.deny(analyticsData.getChildren(true));
//        securityPolicy.navigator.deny(extIncPrintForm);

        securityPolicy.cls.edit.add.deny(document.getConcreteChildren());
        securityPolicy.cls.edit.remove.deny(baseGroup.getClasses());

        user2.addSecurityPolicy(securityPolicy);

    }

    // ------------------------------------- Временные методы --------------------------- //

    public void fillData() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        int modifier = 10;
        int propModifier = 1;

        Map<ConcreteCustomClass,Integer> classQuantity = new HashMap<ConcreteCustomClass, Integer>();
        Collection<ConcreteCustomClass> articleChildren = new ArrayList<ConcreteCustomClass>();
        article.fillConcreteChilds(articleChildren);
        for (ConcreteCustomClass articleClass : articleChildren)
            classQuantity.put(articleClass, modifier *10/ articleChildren.size());

        classQuantity.put(articleGroup,((Double)(modifier *0.3)).intValue());
//        ClassQuantity.put(store,((Double)(Modifier*0.3)).intValue());
        classQuantity.put(store,3);
        classQuantity.put(supplier,5);
        classQuantity.put(customer,5);
        classQuantity.put(extIncomeDocument, modifier *2);
//        classQuantity.put(extIncomeDetail, modifier *100*propModifier);
        classQuantity.put(intraDocument, modifier);
        classQuantity.put(cashSaleDocument, modifier);
        classQuantity.put(clearingSaleDocument,((Double)(modifier *0.5)).intValue());
        classQuantity.put(invDocument,((Double)(modifier *0.2)).intValue());
        classQuantity.put(returnDocument,((Double)(modifier *0.3)).intValue());
        classQuantity.put(exchangeDocument, modifier);
        classQuantity.put(revalDocument,((Double)(modifier *0.5)).intValue());

        Map<DataProperty, Set<DataPropertyInterface>> propNotNulls = new HashMap<DataProperty, Set<DataPropertyInterface>>();
        artGroup.putNotNulls(propNotNulls,0);
        incStore.putNotNulls(propNotNulls,0);
        outStore.putNotNulls(propNotNulls,0);
        extIncSupplier.putNotNulls(propNotNulls,0);
        extIncDetailDocument.putNotNulls(propNotNulls,0);
//        extIncDetailArticle.putNotNulls(propNotNulls,0);
//        extIncDetailQuantity.putNotNulls(propNotNulls,0);
//        extIncDetailPriceIn.putNotNulls(propNotNulls,0);
//        extIncDetailVATIn.putNotNulls(propNotNulls,0);
        clearingSaleCustomer.putNotNulls(propNotNulls,0);
        returnSupplier.putNotNulls(propNotNulls,0);

//        LDP extIncDetailSumVATIn, extIncDetailSumPay;
//        LDP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
//        LDP extIncDetailPriceOut;

        Map<DataProperty,Integer> propQuantity = new HashMap<DataProperty, Integer>();

//        PropQuantity.put((DataProperty)extIncQuantity.Property,10);
        propQuantity.put((DataProperty)articleQuantity.property, modifier *propModifier*8);
        propQuantity.put((DataProperty)exchangeQuantity.property, modifier *propModifier);
//        propQuantity.put((DataProperty)isRevalued.property, modifier *propModifier);

//        propQuantity.putAll(autoQuantity(0, fixedPriceIn, fixedVATIn, fixedAdd, fixedVATOut, fixedLocTax,
//            revalBalanceQuantity,revalPriceIn,revalVATIn,revalAddBefore,revalVATOutBefore,revalLocTaxBefore,
//                revalAddAfter,revalVATOutAfter,revalLocTaxAfter));

        autoFillDB(classQuantity, propQuantity,propNotNulls);
    }

    private class StoreSupplierSpecNavigatorForm extends NavigatorForm {

        public StoreSupplierSpecNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup);
            ObjectNavigator objSupplier = addSingleGroupObjectImplement(supplier, "Поставщик", properties, baseGroup);
            ObjectNavigator objSpec = addSingleGroupObjectImplement(specification, "Спецификация", properties, baseGroup);

            addPropertyView(objStore, objSupplier, properties, baseGroup);
            addPropertyView(objStore, objSpec, properties, baseGroup);
            addPropertyView(objStore, objSupplier, objSpec, properties, baseGroup);
        }
    }
}
