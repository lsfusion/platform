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
        DataAdapter adapter = new PostgreDataAdapter("simpleplat","localhost");
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

    ConcreteCustomClass article, articleGroup, store, customer, supplier, extIncomeDocument, extIncomeDetail, intraDocument, exchangeDocument,
                        cashSaleDocument, clearingSaleDocument, invDocument, returnDocument, revalDocument;
    
    protected void initClasses() {

        article = addConcreteClass("Товар", namedObject);
        articleGroup = addConcreteClass("Группа товаров", namedObject);

        store = addConcreteClass("Склад", namedObject);

        supplier = addConcreteClass("Поставщик", namedObject);
        customer = addConcreteClass("Покупатель", namedObject);

        document = addAbstractClass("Документ", namedObject, transaction);
        priceOutDocument = addAbstractClass("Документ изм. цены", document);
        quantityDocument = addAbstractClass("Документ перемещения", document);
        incomeDocument = addAbstractClass("Приходный документ", quantityDocument, priceOutDocument);
        outcomeDocument = addAbstractClass("Расходный документ", quantityDocument);
        articleDocument = addAbstractClass("Перемещение товара", quantityDocument);

        extIncomeDocument = addConcreteClass("Внешний приход", incomeDocument);
        extIncomeDetail = addConcreteClass("Внешний приход (строки)", baseClass);

        intraDocument = addConcreteClass("Внутреннее перемещение", incomeDocument, outcomeDocument, articleDocument);
        extOutcomeDocument = addAbstractClass("Внешний расход", outcomeDocument);
        exchangeDocument = addConcreteClass("Пересорт", outcomeDocument);

        saleDocument = addAbstractClass("Реализация", extOutcomeDocument, articleDocument);
        cashSaleDocument = addConcreteClass("Реализация по кассе", saleDocument);
        clearingSaleDocument = addConcreteClass("Реализация по б/н расчету", saleDocument);

        invDocument = addConcreteClass("Инвентаризация", extOutcomeDocument);

        returnDocument = addConcreteClass("Возврат поставщику", extOutcomeDocument, articleDocument);

        revalDocument = addConcreteClass("Переоценка", priceOutDocument);
    }

    LDP artGroup, incStore, outStore, extIncSupplier, extIncDetailPriceIn, extIncDetailVATIn, invDBBalance,
            returnSupplier, clearingSaleCustomer, articleQuantity, extIncDetailDocument, extIncDetailQuantity, extIncDetailArticle;

    LP quantity, roundm1, addPercent, docIncBalanceQuantity, docOutBalanceQuantity,
            invBalance, exchangeQuantity, exchIncQuantity, exchOutQuantity, isRevalued, priceOutChange, balanceStoreQuantity;
    LP currentIncDate, currentIncDoc, currentRevalDate, currentRevalDoc, currentPriceOutDate, currentPriceOutDoc,
            currentExtIncDate, currentExtIncDoc, currentSupplier, priceOut;

    LP remainStoreArticleStartQuantity, remainStoreArticleEndQuantity, incBetweenDateQuantity, outBetweenDateQuantity,
        saleStoreArticleBetweenDateQuantity, saleArticleBetweenDateQuantity;

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

        // внешний приход
        extIncSupplier = addDProp("Поставщик", supplier, extIncomeDocument);
        LP extIncSupplierName = addJProp(baseGroup, "extIncSupplierName", "Имя поставщика", name, extIncSupplier, 1);

        extIncDetailDocument = addDProp("extIncDetailDocument", "Документ", extIncomeDocument, extIncomeDetail);

        extIncDetailArticle = addDProp("Товар", article, extIncomeDetail);
        LP extIncDetailArticleName = addJProp(baseGroup, "extIncDetailArticleName", "Имя товара", name, extIncDetailArticle, 1);
        
        // реализация по б\н расчету
        clearingSaleCustomer = addDProp("clearingSaleCustomer","Покупатель", customer, clearingSaleDocument);
        LP clearingSaleCustomerName = addJProp(baseGroup, "Имя покупателя", name, clearingSaleCustomer, 1);

        // возврат поставщику
        returnSupplier = addDProp("returnSupplier","Поставщик", supplier, returnDocument);
        LP returnSupplierName = addJProp(baseGroup, "Имя поставщика", name, returnSupplier, 1);

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
        articleQuantity = addDProp(baseGroup, "Кол-во товара", DoubleClass.instance, articleDocument, article);

        extIncDetailQuantity = addDProp(baseGroup, "extIncDetailQuantity", "Кол-во", DoubleClass.instance, extIncomeDetail);
        LP extIncQuantity = addSGProp(baseGroup, "extIncQuantity" , "Кол-во прих.", extIncDetailQuantity, extIncDetailDocument, 1, extIncDetailArticle, 1);

        exchangeQuantity = addDProp(baseGroup, "exchangeQuantity", "Кол-во перес.", DoubleClass.instance, exchangeDocument, article, article);
        exchIncQuantity = addSGProp(baseGroup, "Прих. перес.", exchangeQuantity, 1, 3);
        exchOutQuantity = addSGProp(baseGroup, "Расх. перес.", exchangeQuantity, 1, 2);
        LP exchDltQuantity = addDUProp("Кол-во перес.", exchOutQuantity, exchIncQuantity);

        invBalance = addDProp(baseGroup, "invBalance", "Остаток инв.", DoubleClass.instance, invDocument, article);
        invDBBalance = addDProp(baseGroup, "invDBBalance", "Остаток (по учету)", DoubleClass.instance, invDocument, article);

        LP invQuantity = addDUProp("invQuantity","Кол-во инв.", invDBBalance, invBalance);

        quantity = addCUProp(baseGroup, "quantity", "Кол-во", articleQuantity, exchDltQuantity, extIncQuantity, invQuantity);

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

        // переоценка
        isRevalued = addDProp(baseGroup, "isRevalued", "Переоц.", LogicalClass.instance, revalDocument, article);
        
        LDP revalAdd = addDProp(baseGroup, "revalAdd", "Надбавка", DoubleClass.instance, revalDocument, article);
        LDP revalVatOut = addDProp(baseGroup, "revalVatOut", "НДС розн.", DoubleClass.instance, revalDocument, article);

        // посл. (текущие) документы

        LP[] maxRevalProps = addMGProp(lastDocumentGroup, new String[]{"maxRevalDate","maxRevalDoc"}, new String[]{"Дата посл. переоц.","Посл. док. переоц."}, 1,
                addJProp("Дата посл. переоц.", and1, date, 1, isRevalued, 1, 2), 1, 2);
        currentRevalDate = maxRevalProps[0];
        currentRevalDoc = maxRevalProps[1];

        // текущие надбавки
        LP currentAdd = addJProp(currentGroup, "Надбавка (тек.)", revalAdd, currentRevalDoc, 1, 1);
        LP currentVatOut = addJProp(currentGroup, "НДС (тек.)", revalVatOut, currentRevalDoc, 1, 1);

        revalAdd.setDefProp(currentAdd, 2, isRevalued, 1, 2);
        revalVatOut.setDefProp(currentVatOut, 2, isRevalued, 1, 2);

        // посл. документ прихода от вн. пост.
        LP[] maxExtIncProps = addMGProp(lastDocumentGroup, new String[]{"maxExtIncDate","maxExtIncDoc"}, new String[]{"Дата посл. вн. прих.","Посл. вн. прих."}, 1,
                addJProp("Дата прих. (кол-во)", and1, date, 1, extIncQuantity, 1, 2), 1, 2);
        currentExtIncDate = maxExtIncProps[0];
        currentExtIncDoc = maxExtIncProps[1];

        currentSupplier = addJProp(currentGroup, "currentSupplier", "Тек. пост.", extIncSupplier, currentExtIncDoc, 1);

        // посл. документ прихода на склад
        LP[] maxIncProps = addMGProp(lastDocumentGroup, new String[]{"maxStoreIncDate","maxStoreIncDoc"}, new String[]{"Дата посл. прих. по скл.","Посл. прих. по скл."}, 1,
                addJProp("Дата прих. (кол-во)", and1, date, 1, quantity, 1, 2), 1, incStore, 1, 2);
        currentIncDate = maxIncProps[0];
        currentIncDoc = maxIncProps[1];

        LP[] maxPriceOutProps = addMUProp(lastDocumentGroup, new String[]{"maxRetailDate","maxRetailDoc"}, new String[]{"Дата посл. цены по скл.","Посл. док. цены по скл."}, 1,
                currentIncDate, addJProp("",and1, currentRevalDate,2,is(store),1), currentIncDoc, addJProp("",and1, currentRevalDoc,2,is(store),1)); // переоценка идет для всех складов
        currentPriceOutDate = maxPriceOutProps[0];
        currentPriceOutDoc = maxPriceOutProps[1];

        // цены поставщика
        extIncDetailPriceIn = addDProp(baseGroup, "extIncDetailPriceIn", "Цена пост.", DoubleClass.instance, extIncomeDetail);
        extIncDetailVATIn = addDProp(baseGroup, "extIncDetailVATIn", "НДС пост.", DoubleClass.instance, extIncomeDetail);

        LP extIncLastDetail = addMGProp(lastDocumentGroup, "extIncLastDetail", "Посл. строка", object(extIncomeDetail), extIncDetailDocument, 1, extIncDetailArticle, 1);
        LP extIncPriceIn = addJProp("Цена пост. (прих.)", extIncDetailPriceIn, extIncLastDetail, 1, 2);

        LDP outPriceIn = addDProp("intraPriceIn", "Цена пост. (расх.)", DoubleClass.instance, outcomeDocument, article);

        LP priceIn = addCUProp(baseGroup, "incPriceIn", "Цена пост.", extIncPriceIn, outPriceIn);

        // текущие цены 
        LP currentPriceIn = addJProp(currentGroup, "Цена пост. (тек.)", priceIn, currentIncDoc, 1, 2, 2);

        addJProp(baseGroup, "Цена пост.(док.)", currentPriceIn, outStore, 1, 2);

        outPriceIn.setDefProp(currentPriceIn, outStore, 1, 2, quantity, 1, 2); // подставляем тек. цену со склада расх

        // НДС по док.
        LDP documentVatOut = addDProp(baseGroup, "documentVatOut", "НДС (док.)", DoubleClass.instance, quantityDocument, article);
        documentVatOut.setDefProp(currentVatOut, 2, quantity, 1, 2);

        // Надбавака по док.
        LDP documentAdd = addDProp(baseGroup, "documentAdd", "Надбавка (док.)", DoubleClass.instance, quantityDocument, article);
        documentAdd.setDefProp(currentAdd, 2, quantity, 1, 2);

        // цена для вн. прих.
        LP extIncPriceOut = calcPriceOut(baseGroup, "Цена розн. (вн. прих.)", extIncPriceIn, documentAdd, documentVatOut, new Object[]{1,2}, new Object[]{1,2});

        // цена для расх.
        LDP outPriceOut = addDProp(baseGroup, "outPriceOut", "Цена розн. (расх.)", DoubleClass.instance, outcomeDocument, article);

        // цена для прих.
        priceOut = addCUProp(baseGroup, "priceOut", "Цена розн. (док.)", extIncPriceOut, outPriceOut);

        // цена прих. переоценки
        LDP revalPriceIn = addDProp(baseGroup, "revalPriceOut", "Цена прих. (переоц.)", DoubleClass.instance, revalDocument, store, article);
        revalPriceIn.setDefProp(currentPriceIn, 2, 3, isRevalued, 1, 3);

        // цена розн. переоценки
        LP revalPriceOut = calcPriceOut(baseGroup,"Цена розн. (переоц.)",revalPriceIn, revalAdd, revalVatOut, new Object[]{1,2,3}, new Object[]{1,3});

        // цена для документа
        priceOutChange = addCUProp(baseGroup,"Цена розн. по док.",revalPriceOut,
                                        addJProp("Цена розн. по скл. (прих.)", and1,  // цена для прихода склада - вытащим склад в интерфейс, для объединения с reval
                                                priceOut, 1, 3,
                                            addJProp("Склад документа", equals2, incStore, 1, 2), 1, 2));

        // текущая розничная цена
        LP currentPriceOut = addJProp(currentGroup, "Цена розн. (тек.)", priceOutChange, currentPriceOutDoc, 1, 2, 1, 2);

        // заполним defProp'ы для цены розн.
        outPriceOut.setDefProp(currentPriceOut, outStore, 1, 2, quantity, 1, 2);

        // прих. суммы по позициям
        LP extIncDetailSumIn = addJProp(baseGroup, "Сумма пост.", round,
                addJProp("", multiplyDouble2, extIncDetailQuantity, 1, extIncDetailPriceIn, 1), 1);
        LP extIncDetailSumVATIn = addJProp(baseGroup, "Сумма НДС", round,
                addJProp("", percent, extIncDetailSumIn, 1, extIncDetailVATIn, 1), 1);
        LP extIncDetailSumPay = addSUProp(baseGroup, "Всего с НДС", Union.SUM, extIncDetailSumIn, extIncDetailSumVATIn);

        // прих. суммы по документам
        LP extIncDocumentSumIn = addSGProp(baseGroup, "extIncDocumentSumInc", "Сумма пост.", extIncDetailSumIn, extIncDetailDocument, 1);
        LP extIncDocumentSumVATIn = addSGProp(baseGroup, "extIncDocumentSumVATIn", "Сумма НДС", extIncDetailSumVATIn, extIncDetailDocument, 1);
        LP extIncDocumentSumPay = addSUProp(baseGroup, "Всего с НДС", Union.SUM, extIncDocumentSumIn, extIncDocumentSumVATIn);

        // расх. суммы по позициям
        LP detailSumOut = addJProp(baseGroup, "detailSumOut", "Сумма розн.", round,
                addJProp("", multiplyDouble2, quantity, 1, 2, priceOut, 1, 2), 1, 2);
        LP detailSumVatOut = addJProp(baseGroup, "Сумма НДС (розн.)", round,
                addJProp("", percent, detailSumOut, 1, 2, documentVatOut, 1, 2), 1, 2);

        // расх. суммы по документам
        LP documentSumOut = addSGProp(baseGroup, "documentSumOut", "Сумма розн.", detailSumOut, 1);
        LP documentSaleVatOut = addSGProp(baseGroup, "documentSaleVatOut", "Сумма НДС (розн.)", detailSumVatOut, 1);
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
        persistents.add((AggregateProperty)currentPriceOutDate.property);
        persistents.add((AggregateProperty)currentPriceOutDoc.property);
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

    NavigatorForm extIncDetailForm, extIncForm, extIncPrintForm;
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
            NavigatorForm extIncDetailForm = new ExtIncDetailNavigatorForm(primaryData, 110, "Внешний приход");
                NavigatorForm extIncForm = new ExtIncNavigatorForm(extIncDetailForm, 115, "Внешний приход по товарам");
                NavigatorForm extIncPrintForm = new ExtIncPrintNavigatorForm(extIncDetailForm, 117, "Реестр цен");
            NavigatorForm intraForm = new IntraNavigatorForm(primaryData, 120, "Внутреннее перемещение");
            NavigatorForm extOutForm = new ExtOutNavigatorForm(primaryData, 130, "Внешний расход");
                NavigatorForm cashSaleForm = new CashSaleNavigatorForm(extOutForm, 131, "Реализация по кассе");
                NavigatorForm clearingSaleForm = new ClearingSaleNavigatorForm(extOutForm, 132, "Реализация по б/н расчету");
                NavigatorForm invForm = new InvNavigatorForm(extOutForm, 134, "Инвентаризация", false);
                    NavigatorForm invStoreForm = new InvNavigatorForm(invForm, 1341, "Инвентаризация по складам", true);
                NavigatorForm returnForm = new ReturnNavigatorForm(extOutForm, 136, "Возврат поставщику");
            NavigatorForm exchangeForm = new ExchangeNavigatorForm(primaryData, 140, "Пересорт");
                NavigatorForm exchangeMForm = new ExchangeMNavigatorForm(exchangeForm, 142, "Сводный пересорт");
            NavigatorForm revalueForm = new RevalueNavigatorForm(primaryData, 150, "Переоценка", false);

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

        extIncomeDocument.relevantElements.set(0, extIncDetailForm);
        intraDocument.relevantElements.set(0, intraForm);
        extOutcomeDocument.relevantElements.set(0, extOutForm);
        clearingSaleDocument.relevantElements.set(0, clearingSaleForm);
        invDocument.relevantElements.set(0, invForm);
        exchangeDocument.relevantElements.set(0, exchangeForm);
        revalDocument.relevantElements.set(0, revalueForm);

        extIncDetailForm.addRelevantElement(extIncPrintForm);
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

    private class ExtIncDetailNavigatorForm extends ExtIncDocumentNavigatorForm {

        ObjectNavigator objDetail;

        public ExtIncDetailNavigatorForm(NavigatorElement parent, int ID, String caption) {
            this(parent, ID, caption, false);
        }
        public ExtIncDetailNavigatorForm(NavigatorElement parent, int ID, String caption, boolean isPrintForm) {
            super(parent, ID, caption, isPrintForm);

            objDetail = addSingleGroupObjectImplement(extIncomeDetail, "Строка", properties, baseGroup);

            PropertyObjectNavigator detDocument = addPropertyObjectImplement(extIncDetailDocument, objDetail);
            addFixedFilter(new CompareFilterNavigator(detDocument, Compare.EQUALS, objDoc));
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

    private class ExtIncPrintNavigatorForm extends ExtIncDetailNavigatorForm {

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

    private class IntraNavigatorForm extends TmcNavigatorForm {

        public IntraNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(intraDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup);

            addPropertyView(objDoc, objArt, properties, baseGroup, quantity);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view,
                                         getPropertyView(docIncBalanceQuantity.property).view);

            addHintsNoUpdate(currentIncDate.property);
        }
    }

    private class ExtOutNavigatorForm extends TmcNavigatorForm {

        public ExtOutNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(extOutcomeDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup, quantity);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class CashSaleNavigatorForm extends TmcNavigatorForm {

        public CashSaleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(cashSaleDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup, quantity);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class ClearingSaleNavigatorForm extends TmcNavigatorForm {

        public ClearingSaleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(clearingSaleDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, currentGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup, quantity);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class InvNavigatorForm extends TmcNavigatorForm {

        public InvNavigatorForm(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectNavigator objStore = null;
            if (groupStore) {
                objStore = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup);
                objStore.groupTo.gridClassView = false;
                objStore.groupTo.singleViewType = true;
            }

            ObjectNavigator objDoc = addSingleGroupObjectImplement(invDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup, invBalance, quantity);

            addArticleRegularFilterGroup(getPropertyView(quantity.property).view,
                    getPropertyView(docOutBalanceQuantity.property).view);

            if (groupStore)
                addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outStore, objDoc), Compare.EQUALS, objStore));
        }
    }

    private class ReturnNavigatorForm extends TmcNavigatorForm {

        public ReturnNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(returnDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup, quantity);

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

            addPropertyView(objDoc, objArtTo, properties, docOutBalanceQuantity, exchIncQuantity, exchOutQuantity, baseGroup);
            addPropertyView(docOutBalanceQuantity, objDoc, objArtFrom);
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
                                  new CompareFilterNavigator(getPropertyView(priceOut.property, objArtFrom.groupTo).view, Compare.EQUALS, getPropertyView(priceOut.property, objArtTo.groupTo).view),
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

        public RevalueNavigatorForm(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(revalDocument, "Документ", properties, baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup);

            addPropertyView(objDoc, objArt, properties, baseGroup, isRevalued);

            addArticleRegularFilterGroup(getPropertyView(isRevalued.property).view);
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
        securityPolicy.property.change.deny(extIncDetailArticle.property);
        securityPolicy.property.change.deny(extIncDetailQuantity.property);

        securityPolicy.navigator.deny(analyticsData.getChildren(true));
        securityPolicy.navigator.deny(extIncPrintForm);

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
        classQuantity.put(extIncomeDetail, modifier *100*propModifier);
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
        extIncDetailArticle.putNotNulls(propNotNulls,0);
        extIncDetailQuantity.putNotNulls(propNotNulls,0);
        extIncDetailPriceIn.putNotNulls(propNotNulls,0);
        extIncDetailVATIn.putNotNulls(propNotNulls,0);
        clearingSaleCustomer.putNotNulls(propNotNulls,0);
        returnSupplier.putNotNulls(propNotNulls,0);

//        LDP extIncDetailSumVATIn, extIncDetailSumPay;
//        LDP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
//        LDP extIncDetailPriceOut;

        Map<DataProperty,Integer> propQuantity = new HashMap<DataProperty, Integer>();

//        PropQuantity.put((DataProperty)extIncQuantity.Property,10);
        propQuantity.put((DataProperty)articleQuantity.property, modifier *propModifier*8);
        propQuantity.put((DataProperty)exchangeQuantity.property, modifier *propModifier);
        propQuantity.put((DataProperty)isRevalued.property, modifier *propModifier);

//        propQuantity.putAll(autoQuantity(0, fixedPriceIn, fixedVATIn, fixedAdd, fixedVATOut, fixedLocTax,
//            revalBalanceQuantity,revalPriceIn,revalVATIn,revalAddBefore,revalVATOutBefore,revalLocTaxBefore,
//                revalAddAfter,revalVATOutAfter,revalLocTaxAfter));

        autoFillDB(classQuantity, propQuantity,propNotNulls);
    }

}
