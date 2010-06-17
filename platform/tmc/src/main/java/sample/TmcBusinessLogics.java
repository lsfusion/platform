package sample;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import platform.interop.Compare;
import platform.interop.ClassViewType;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.data.Union;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.AggregateProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.view.form.client.DefaultFormView;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.*;
import platform.server.classes.*;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class TmcBusinessLogics extends BusinessLogics<TmcBusinessLogics> {

    public TmcBusinessLogics(DataAdapter iAdapter,int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(iAdapter,port);
    }

    ConcreteCustomClass article;
    ConcreteCustomClass articleGroup;

    ConcreteCustomClass store;
    ConcreteCustomClass supplier;
    ConcreteCustomClass customer;

    AbstractCustomClass document;
    AbstractCustomClass primaryDocument, secondaryDocument;
    AbstractCustomClass fixedDocument, accountDocument;
    AbstractCustomClass quantityDocument;
    AbstractCustomClass incomeDocument;
    AbstractCustomClass outcomeDocument;

    ConcreteCustomClass extIncomeDocument;
    ConcreteCustomClass extIncomeDetail;

    ConcreteCustomClass intraDocument;
    AbstractCustomClass extOutcomeDocument;
    ConcreteCustomClass exchangeDocument;
    ConcreteCustomClass revalDocument;

    AbstractCustomClass saleDocument;
    ConcreteCustomClass cashSaleDocument;
    ConcreteCustomClass clearingSaleDocument;
    ConcreteCustomClass invDocument;
    ConcreteCustomClass returnDocument;

    ConcreteCustomClass receipt;

    AbstractGroup artclGroup, artgrGroup, storeGroup, supplierGroup, customerGroup, quantGroup, balanceGroup;
    AbstractGroup incPrmsGroup, incPrmsGroupBefore, incPrmsGroupAfter, incSumsGroup, outSumsGroup, outPrmsGroup, outPrmsGroupBefore, outPrmsGroupAfter;
    AbstractGroup paramsGroup, accountGroup;

    protected void initGroups() {

        artclGroup = new AbstractGroup("Товар");
        artgrGroup = new AbstractGroup("Группа товаров");
        storeGroup = new AbstractGroup("Склад");
        supplierGroup = new AbstractGroup("Поставщик");
        customerGroup = new AbstractGroup("Поставщик");
        quantGroup = new AbstractGroup("Количество");
        balanceGroup = new AbstractGroup("Остаток");
        incPrmsGroup = new AbstractGroup("Входные параметры");
        incPrmsGroupBefore = new AbstractGroup("До");
        incPrmsGroup.add(incPrmsGroupBefore);
        incPrmsGroupAfter = new AbstractGroup("После");
        incPrmsGroup.add(incPrmsGroupAfter);
        incSumsGroup = new AbstractGroup("Входные суммы");
        outSumsGroup = new AbstractGroup("Выходные суммы");
        outPrmsGroup = new AbstractGroup("Выходные параметры");
        outPrmsGroupBefore = new AbstractGroup("До");
        outPrmsGroup.add(outPrmsGroupBefore);
        outPrmsGroupAfter = new AbstractGroup("После");
        outPrmsGroup.add(outPrmsGroupAfter);
        paramsGroup = new AbstractGroup("Измененные параметры");
        accountGroup = new AbstractGroup("Бухг. параметры");
    }

    protected void initClasses() {

        article = addConcreteClass("Товар", namedObject);
        articleGroup = addConcreteClass("Группа товаров", namedObject);

        store = addConcreteClass("Склад", namedObject);

        supplier = addConcreteClass("Поставщик", namedObject);
        customer = addConcreteClass("Покупатель", namedObject);

        document = addAbstractClass("Документ", namedObject);
        primaryDocument = addAbstractClass("Первичный документ", document);
        secondaryDocument = addAbstractClass("Непервичный документ", document);
        quantityDocument = addAbstractClass("Товарный документ", document);
        incomeDocument = addAbstractClass("Приходный документ", quantityDocument);
        outcomeDocument = addAbstractClass("Расходный документ", quantityDocument);
        fixedDocument = addAbstractClass("Зафиксированный документ", document);
        accountDocument = addAbstractClass("Бухгалтерский документ", document);

        extIncomeDocument = addConcreteClass("Внешний приход", incomeDocument, primaryDocument);
        extIncomeDetail = addConcreteClass("Внешний приход (строки)", baseClass);

        intraDocument = addConcreteClass("Внутреннее перемещение", incomeDocument, outcomeDocument, primaryDocument, fixedDocument);
        extOutcomeDocument = addAbstractClass("Внешний расход", outcomeDocument, secondaryDocument, accountDocument);
        exchangeDocument = addConcreteClass("Пересорт", incomeDocument, outcomeDocument, secondaryDocument, fixedDocument);

        revalDocument = addConcreteClass("Переоценка", primaryDocument);

        saleDocument = addAbstractClass("Реализация", extOutcomeDocument);
        cashSaleDocument = addConcreteClass("Реализация по кассе", saleDocument);
        clearingSaleDocument = addConcreteClass("Реализация по б/н расчету", saleDocument, fixedDocument);

        invDocument = addConcreteClass("Инвентаризация", extOutcomeDocument, fixedDocument);

        returnDocument = addConcreteClass("Возврат поставщику", extOutcomeDocument, fixedDocument);

        receipt = addConcreteClass("Чек", fixedDocument);
    }

    protected void initProperties() {

        initAbstractProperties();
        initPrimaryProperties();
        initAggregateProperties();
    }

    // ======================================================================================================= //
    // ==================================== Инициализация абстратных свойств ================================= //
    // ======================================================================================================= //

    LP percent, revPercent, addPercent;
    LP round, roundm1;
    LP multiplyDouble2;
    LP equals22;

    private void initAbstractProperties() {

        equals22 = addJProp("И", and1, equals2,1,2,equals2,3,4);
        percent = addSFProp("((prm1*prm2)/100)", DoubleClass.instance, 2);
        revPercent = addSFProp("((prm1*prm2)/(100+prm2))", DoubleClass.instance, 2);
        addPercent = addSFProp("((prm1*(100+prm2))/100)", DoubleClass.instance, 2);
        round = addSFProp("round(CAST(prm1 as numeric),0)", DoubleClass.instance, 1);
        roundm1 = addSFProp("round(CAST(prm1 as numeric),-1)", DoubleClass.instance, 1);
        multiplyDouble2 = addMFProp(DoubleClass.instance,2);
    }

    // ======================================================================================================= //
    // ==================================== Инициализация первичных свойств ================================== //
    // ======================================================================================================= //

    private void initPrimaryProperties() {

        initArticleProperties();
        initCustomArticleLogics();
        initExtIncProperties();
        initClearingSaleProperties();
        initReceiptProperties();
        initReturnProperties();
        initRevalProperties();
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------------- Товар ----------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LP artGroup;
    LP artGroupName;

    LP artBarCode;
    LP artWeight;
    LP artPackCount;

    private void initArticleProperties() {

        artGroup = addDProp("Гр. тов.", articleGroup, article);
        artGroupName = addJProp(artgrGroup, "Имя гр. тов.", name, artGroup, 1);

        artBarCode = addDProp(baseGroup, "Штрих-код", NumericClass.get(13, 0), article);
        artWeight = addDProp(baseGroup, "Вес (кг.)", NumericClass.get(6, 3), article);
        artPackCount = addDProp(baseGroup, "Кол-во в уп.", IntegerClass.instance, article);

    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Внешний приход -------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LP extIncSupplier;
    LP extIncSupplierName;

    LP extIncDetailDocument;
    LP extIncDetailArticle;
    LP extIncDetailArticleName;

    private void initExtIncProperties() {

        extIncSupplier = addDProp("Поставщик", supplier, extIncomeDocument);
        extIncSupplierName = addJProp(supplierGroup, "extIncSupplierName", "Имя поставщика", name, extIncSupplier, 1);

        extIncDetailDocument = addDProp("extIncDetailDocument", "Документ", extIncomeDocument, extIncomeDetail);

        extIncDetailArticle = addDProp("Товар", article, extIncomeDetail);
        extIncDetailArticleName = addJProp(artclGroup, "extIncDetailArticleName", "Имя товара", name, extIncDetailArticle, 1);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // -------------------------------------------------- Чеки ----------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LP receiptSaleDocument;

    private void initReceiptProperties() {

        receiptSaleDocument = addDProp("receiptSaleDocument","Документ продажи", cashSaleDocument, receipt);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ---------------------------------------- Реализация по б/н расчету ------------------------------------ //
    // ------------------------------------------------------------------------------------------------------- //

    LP clearingSaleCustomer;
    LP clearingSaleCustomerName;

    private void initClearingSaleProperties() {

        clearingSaleCustomer = addDProp("clearingSaleCustomer","Покупатель", customer, clearingSaleDocument);
        clearingSaleCustomerName = addJProp(customerGroup, "Имя покупателя", name, clearingSaleCustomer, 1);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Возврат поставщику ---------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LP returnSupplier;
    LP returnSupplierName;

    private void initReturnProperties() {

        returnSupplier = addDProp("returnSupplier","Поставщик", supplier, returnDocument);
        returnSupplierName = addJProp(supplierGroup, "Имя поставщика", name, returnSupplier, 1);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------------ Переоценка ------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void initRevalProperties() {
    }

    // ======================================================================================================= //
    // ================================= Инициализация агрегированных свойств ================================ //
    // ======================================================================================================= //

    private void initAggregateProperties() {

        initDateProperties();
        initStoreProperties();
        initQuantityProperties();
        initDocArtProperties();
        initMaxProperties();
        initParamsProperties();
        initSumProperties();
        initDocCurrentProperties();
    }

    // ------------------------------------------------------------------------------------------------------- //
    // --------------------------------------------------- Даты ---------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void initDateProperties() {

        initDatePrimaryProperties();
        initDateOverrideProperties();
        initDateDocProperties();
    }

    // ---------------------------------------- Первичные свойства ------------------------------------------- //

    LP extIncDate;
    LP intraDate;
    LP extOutDate;
    LP exchDate;
    LP revalDate;

    private void initDatePrimaryProperties() {

        extIncDate = addDProp(baseGroup, "extIncDate", "Дата", DateClass.instance, extIncomeDocument);
        intraDate = addDProp(baseGroup, "intraDate", "Дата", DateClass.instance, intraDocument);
        extOutDate = addDProp(baseGroup, "extOutDate", "Дата", DateClass.instance, extOutcomeDocument);
        exchDate = addDProp(baseGroup, "exchDate", "Дата", DateClass.instance, exchangeDocument);
        revalDate = addDProp(baseGroup, "revalDate", "Дата", DateClass.instance, revalDocument);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LP primDocDate, secDocDate;
    LP docDate;

    private void initDateOverrideProperties() {

        primDocDate = addCUProp(paramsGroup, "Дата", extIncDate, intraDate, revalDate);
        secDocDate = addCUProp("Дата", extOutDate, exchDate);

        docDate = addCUProp("docDate", "Дата", secDocDate, primDocDate);
    }

    // ------------------------------------ Свойства по документам ------------------------------------------- //

    LP groeqDocDate, greaterDocDate, betweenDocDate;

    private void initDateDocProperties() {

        groeqDocDate = addJProp("Дата док.>=Дата", groeq2, docDate, 1, object(DateClass.instance), 2);
        greaterDocDate = addJProp("Дата док.>Дата", greater2, docDate, 1, object(DateClass.instance), 2);
        betweenDocDate = addJProp("Дата док. между Дата", between, docDate, 1, object(DateClass.instance), 2, object(DateClass.instance), 3);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // -------------------------------------------------- Склады --------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void initStoreProperties() {

        initStorePrimaryProperties();
        initStoreOverrideProperties();
        initDocStoreProperties();
    }

    // ---------------------------------------- Первичные свойства ------------------------------------------- //

    LP extIncStore;
    LP intraOutStore, intraIncStore;
    LP intraStoreName;
    LP extOutStore;
    LP exchStore;
    LP revalStore;

    LP receiptStore;

    private void initStorePrimaryProperties() {

        extIncStore = addDProp("extIncStore", "Склад", store, extIncomeDocument);

        intraOutStore = addDProp("intraOutStore", "Склад отпр.", store, intraDocument);
        intraIncStore = addDProp("intraIncStore", "Склад назн.", store, intraDocument);
        intraStoreName = addJProp(storeGroup, "Имя склада (назн.)", name, intraIncStore, 1);

        extOutStore = addDProp("extOutStore", "Склад", store, extOutcomeDocument);
        exchStore = addDProp("exchStore", "Склад", store, exchangeDocument);
        revalStore = addDProp("revalStore", "Склад", store, revalDocument);

        receiptStore = addJProp(storeGroup, "Склад чека", extOutStore, receiptSaleDocument, 1);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LP incQStore;
    LP incSStore;
    LP outQStore;
    LP outSStore;
    LP primDocStore;
    LP fixedStore;
    LP docStore;

    LP docStoreName;
    
    private void initStoreOverrideProperties() {

        incQStore = addCUProp("incQStore", "Склад прих.", extIncStore, intraIncStore, exchStore);
        outQStore = addCUProp("outQStore", "Склад расх.", intraOutStore, extOutStore, exchStore);

        incSStore = addCUProp("Склад прих. (сум)", incQStore, revalStore);
        outSStore = outQStore;

        primDocStore = addCUProp(paramsGroup, "Склад (изм.)", extIncStore, intraIncStore, revalStore);
        fixedStore = addCUProp("Склад (парам.)", receiptStore, intraOutStore, extOutStore, exchStore);

        docStore = addCUProp("docStore", "Склад", extIncStore, intraOutStore, extOutStore, exchStore, revalStore);

        docStoreName = addJProp(storeGroup, "docStoreName", "Имя склада", name, docStore, 1);
    }

    // ------------------------------------ Свойства по документам ------------------------------------------- //

    LP isDocIncQStore;
    LP isDocOutQStore;
    LP isDocRevalStore;
    LP isDocIncSStore;
    LP isDocOutSStore;
    LP isDocStore;

    private void initDocStoreProperties() {

        isDocIncQStore = addJProp("Склад=прих.кол-во", equals2, incQStore, 1, 2);
        isDocOutQStore = addJProp("Склад=расх.кол-во", equals2, outQStore, 1, 2);
        isDocRevalStore = addJProp("Склад=переоц.", equals2, revalStore, 1, 2);

        isDocIncSStore = addJProp("Склад=прих.кол-во", equals2, incSStore, 1, 2);
        isDocOutSStore = addJProp("Склад=расх.кол-во", equals2, outSStore, 1, 2);

        isDocStore = addSUProp("Склад=док.", Union.OVERRIDE, isDocIncQStore, isDocOutQStore, isDocRevalStore); // 1-й и 2-й пересекаются по внутреннему перемещению
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------------- Количество ------------------------------------------ //
    // ------------------------------------------------------------------------------------------------------- //

    private void initQuantityProperties() {

        initQuantityPrimaryProperties();
        initQuantityOverrideProperties();
        initQuantityDocStoreProperties();
        initQuantityStoreProperties();
        initQuantityDateProperties();
        initQuantitySaleProperties();
    }

    // ---------------------------------------- Первичные свойства ------------------------------------------- //

    LP extIncDetailQuantity;
    LP extIncDocumentQuantity;
    LP extIncQuantity;

    LP intraQuantity;

    LP receiptQuantity;
    LP cashSaleQuantity;
    LP clearingSaleQuantity;
    LP saleQuantity;
    LP invQuantity;
    LP invBalance;
    LP returnQuantity;
    LP extOutQuantity;

    LP exchangeQuantity;
    LP exchIncQuantity, exchOutQuantity;
    LP exchDltQuantity;

    LP revalBalanceQuantity;

    private void initQuantityPrimaryProperties() {

        extIncDetailQuantity = addDProp(quantGroup, "extIncDetailQuantity", "Кол-во", DoubleClass.instance, extIncomeDetail);
        extIncDocumentQuantity = addSGProp(quantGroup, "extIncDocumentQuantity", "Кол-во (всего)", extIncDetailQuantity, extIncDetailDocument, 1);

        extIncQuantity = addSGProp(quantGroup, "extIncQuantity" , "Кол-во прих.", extIncDetailQuantity, extIncDetailDocument, 1, extIncDetailArticle, 1);

        intraQuantity = addDProp(quantGroup, "Кол-во внутр.", DoubleClass.instance, intraDocument, article);

        receiptQuantity = addDProp(quantGroup, "receiptQuantity", "Кол-во в чеке", DoubleClass.instance, receipt, article);
        cashSaleQuantity = addSGProp(quantGroup, "cashSaleQuantity", "Кол-во прод.", receiptQuantity, receiptSaleDocument, 1, 2);

        clearingSaleQuantity = addDProp(quantGroup, "clearingSaleQuantity", "Кол-во расх.", DoubleClass.instance, clearingSaleDocument, article);

        saleQuantity = addCUProp("Кол-во реал.", clearingSaleQuantity, cashSaleQuantity);

        invQuantity = addDProp(quantGroup, "invQuantity", "Кол-во инв.", DoubleClass.instance, invDocument, article);
        invBalance = addDProp(quantGroup, "invBalance", "Остаток инв.", DoubleClass.instance, invDocument, article);
//        LF defInvQuantity = addUProp("Кол-во инв. (по умолч.)", 1, 2, 1, docOutBalanceQuantity, 1, 2, -1, invBalance, 1, 2);
//        setDefProp(invQuantity, defInvQuantity, true);

        returnQuantity = addDProp(quantGroup, "returnQuantity", "Кол-во возвр.", DoubleClass.instance, returnDocument, article);

        extOutQuantity = addCUProp("Кол-во расх.", returnQuantity, invQuantity, clearingSaleQuantity, cashSaleQuantity);

        exchangeQuantity = addDProp(quantGroup, "exchangeQuantity", "Кол-во перес.", DoubleClass.instance, exchangeDocument, article, article);

        revalBalanceQuantity = addDProp(quantGroup, "revalBalanceQuantity", "Остаток", DoubleClass.instance, revalDocument, article);

        exchIncQuantity = addSGProp("Прих. перес.", exchangeQuantity, 1, 3);
        exchOutQuantity = addSGProp("Расх. перес.", exchangeQuantity, 1, 2);
        exchDltQuantity = addDUProp("Разн. перес.", exchIncQuantity, exchOutQuantity);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LP incQuantity;
    LP outQuantity;
    LP quantity;

    private void initQuantityOverrideProperties() {

        incQuantity = addCUProp("Кол-во прих.", extIncQuantity, intraQuantity, exchIncQuantity);
        outQuantity = addCUProp("Кол-во расх.", extOutQuantity, intraQuantity, exchOutQuantity);

        quantity = addCUProp("Кол-во", extIncQuantity, intraQuantity, extOutQuantity, exchDltQuantity);
    }

    // ---------------------------------- Свойства по документам/складам --------------------------------------- //

    LP incDocStoreQuantity, outDocStoreQuantity;
    LP dltDocStoreQuantity;

    private void initQuantityDocStoreProperties() {

        incDocStoreQuantity = addJProp("Кол-во прих. по скл.", and1, incQuantity, 1, 3, isDocIncQStore, 1, 2);
        outDocStoreQuantity = addJProp("Кол-во расх. по скл.", and1, outQuantity, 1, 3, isDocOutQStore, 1, 2);
        dltDocStoreQuantity = addDUProp("Кол-во (+-)", incDocStoreQuantity, outDocStoreQuantity);
    }

    // ----------------------------------------- Свойства по складам ------------------------------------------- //

    LP incStoreQuantity, outStoreQuantity;
    LP balanceStoreQuantity;

    private void initQuantityStoreProperties() {

        incStoreQuantity = addSGProp("incStoreQuantity", "Прих. на скл.", incQuantity, incQStore, 1, 2);
        outStoreQuantity = addSGProp("outStoreQuantity", "Расх. со скл.", outQuantity, outQStore, 1, 2);

        balanceStoreQuantity = addDUProp(balanceGroup, "Ост. на скл.", incStoreQuantity, outStoreQuantity);
    }

    // ----------------------------------------- Свойства по датам ------------------------------------------------ //


    LP incGroeqDateQuantity, outGroeqDateQuantity;
    LP incStoreArticleGroeqDateQuantity, outStoreArticleGroeqDateQuantity;
    LP dltStoreArticleGroeqDateQuantity;

    LP incGreaterDateQuantity, outGreaterDateQuantity;
    LP incStoreArticleGreaterDateQuantity, outStoreArticleGreaterDateQuantity;
    LP dltStoreArticleGreaterDateQuantity;

    LP balanceStoreDateMQuantity;
    LP balanceStoreDateEQuantity;

    LP incBetweenDateQuantity, outBetweenDateQuantity;
    LP incStoreArticleBetweenDateQuantity, outStoreArticleBetweenDateQuantity;

    private void initQuantityDateProperties() {

        incGroeqDateQuantity = addJProp("Кол-во прих. с даты", and1, incQuantity, 1, 2, groeqDocDate, 1, 3);
        outGroeqDateQuantity = addJProp("Кол-во расх. с даты", and1, outQuantity, 1, 2, groeqDocDate, 1, 3);

        incStoreArticleGroeqDateQuantity = addSGProp("Кол-во прих. на скл. с даты", incGroeqDateQuantity, incQStore, 1, 2, 3);
        outStoreArticleGroeqDateQuantity = addSGProp("Кол-во расх. со скл. с даты", outGroeqDateQuantity, outQStore, 1, 2, 3);
        dltStoreArticleGroeqDateQuantity = addDUProp("Кол-во на скл. с даты", incStoreArticleGroeqDateQuantity, outStoreArticleGroeqDateQuantity);

        incGreaterDateQuantity = addJProp("Кол-во прих. после даты", and1, incQuantity, 1, 2, greaterDocDate, 1, 3);
        outGreaterDateQuantity = addJProp("Кол-во расх. после даты", and1, outQuantity, 1, 2, greaterDocDate, 1, 3);

        incStoreArticleGreaterDateQuantity = addSGProp("Кол-во прих. на скл. после даты", incGreaterDateQuantity, incQStore, 1, 2, 3);
        outStoreArticleGreaterDateQuantity = addSGProp("Кол-во расх. со скл. после даты", outGreaterDateQuantity, outQStore, 1, 2, 3);
        dltStoreArticleGreaterDateQuantity = addDUProp("Кол-во на скл. после даты", incStoreArticleGreaterDateQuantity, outStoreArticleGreaterDateQuantity);

        balanceStoreDateMQuantity = addDUProp(quantGroup, "Кол-во на начало", addJProp("", and1, balanceStoreQuantity, 1, 2, is(DateClass.instance), 3), dltStoreArticleGroeqDateQuantity);
        balanceStoreDateEQuantity = addDUProp(quantGroup, "Кол-во на конец", addJProp("", and1, balanceStoreQuantity, 1, 2, is(DateClass.instance), 3), dltStoreArticleGreaterDateQuantity);

        incBetweenDateQuantity = addJProp("Кол-во прих. за интервал", and1, incQuantity, 1, 2, betweenDocDate, 1, 3, 4);
        outBetweenDateQuantity = addJProp("Кол-во расх. за интервал", and1, outQuantity, 1, 2, betweenDocDate, 1, 3, 4);

        incStoreArticleBetweenDateQuantity = addSGProp(quantGroup, "Кол-во прих. на скл. за интервал", incBetweenDateQuantity, incQStore, 1, 2, 3, 4);
        outStoreArticleBetweenDateQuantity = addSGProp(quantGroup, "Кол-во расх. со скл. за интервал", outBetweenDateQuantity, outQStore, 1, 2, 3, 4);
    }

    // ----------------------------------------- Свойства по реализации ------------------------------------------- //

    LP saleBetweenDateQuantity;
    LP saleStoreArticleBetweenDateQuantity;
    LP saleArticleBetweenDateQuantity;

    private void initQuantitySaleProperties() {

        saleBetweenDateQuantity = addJProp("Кол-во реал. за интервал", and1, saleQuantity, 1, 2, betweenDocDate, 1, 3, 4);
        saleStoreArticleBetweenDateQuantity = addSGProp(quantGroup, "Кол-во реал. на скл. за интервал", saleBetweenDateQuantity, extOutStore, 1, 2, 3, 4);

        saleArticleBetweenDateQuantity = addSGProp(quantGroup, "Реал. кол-во (по товару)", saleStoreArticleBetweenDateQuantity, 2, 3, 4);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // --------------------------------------------- Документы/товары ---------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LP isRevalued;
    LP isDocArtChangesParams;

    LP isDocArtInclude;

    LP isDocStoreArtInclude;

    private void initDocArtProperties() {

        isRevalued = addDProp("isRevalued", "Переоц.", LogicalClass.instance, revalDocument, article);

        isDocArtChangesParams = addCUProp(paramsGroup, "Изм. парам.", isRevalued,
                addJProp("Есть в перв. док.", and1, vtrue,
                        addCUProp("Кол-во прих. (парам.)", extIncQuantity, intraQuantity), 1, 2));

        isDocArtInclude = addCUProp("Есть в док.", addJProp("Есть в док.", and1, vtrue, quantity, 1, 2), isRevalued);

        isDocStoreArtInclude = addJProp("В док. и скл.", and1, isDocStore, 1, 2, isDocArtInclude, 1, 3);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------ Последние документы ---------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LP maxStoreExtIncDate;
    LP maxStoreExtIncDoc;

    LP maxChangesParamsDate;
    LP maxChangesParamsDoc;

    LP changesParamsDate;
    private void initMaxProperties() {

        // -------------------------- Последний приходный документ по товару ---------------------------- //

        LP notZeroExtIncDate = addJProp("Дата посл. прих.", and1, extIncDate, 1, extIncQuantity, 1, 2);
        LP[] maxIncProps = addMGProp(baseGroup, new String[]{"maxStoreExtIncDate","maxStoreExtIncDoc"},
                new String[]{"Дата посл. прих.","Посл. док. прих."}, 1, notZeroExtIncDate, 1, extIncStore, 1, 2);
        maxStoreExtIncDate = maxIncProps[0];
        maxStoreExtIncDoc = maxIncProps[1]; 

        // -------------------------- Последний документ изм. цену ---------------------------- //
        changesParamsDate = addJProp("Дата изм. пар.", and1, primDocDate, 1, isDocArtChangesParams, 1, 2);
        LP[] maxChangesProps = addMGProp(baseGroup, new String[]{"maxChangesParamsDate","maxChangesParamsDoc"},
                new String[]{"Посл. дата изм. парам.","Посл. док. изм. парам."}, 1, changesParamsDate, 1, primDocStore, 1, 2);
        maxChangesParamsDate = maxChangesProps[0];
        maxChangesParamsDoc = maxChangesProps[1];
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Ценовые параметры ----------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void initParamsProperties() {

        initParamsPrimaryProperties();
        initParamsOverrideProperties();
        initParamsCurrentProperties();
    }

    // ---------------------------------------- Первичные свойства ------------------------------------------- //

    private void initParamsPrimaryProperties() {

        initParamsPrimaryExtIncProperties();
        initParamsPrimaryFixedProperties();
        initParamsPrimaryRevalProperties();
    }

    // Внешний приход

    LP extIncDetailPriceIn, extIncDetailVATIn;

    LP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
    LP extIncDetailCalcPriceOut;
    LP extIncDetailPriceOut;

    LP extIncLastDetail;

    LP extIncPriceIn, extIncVATIn;
    LP extIncAdd, extIncVATOut, extIncLocTax;
    LP extIncPriceOut;

    private void initParamsPrimaryExtIncProperties() {

        extIncDetailPriceIn = addDProp(incPrmsGroup, "extIncDetailPriceIn", "Цена пост.", DoubleClass.instance, extIncomeDetail);
        extIncDetailVATIn = addDProp(incPrmsGroup, "extIncDetailVATIn", "НДС пост.", DoubleClass.instance, extIncomeDetail);

        // -------------------------- Выходные параметры ---------------------------- //

        extIncDetailAdd = addDProp(outPrmsGroup, "extIncDetailAdd", "Надбавка", DoubleClass.instance, extIncomeDetail);
        extIncDetailVATOut = addDProp(outPrmsGroup, "extIncDetailVATOut", "НДС прод.", DoubleClass.instance, extIncomeDetail);
        extIncDetailVATOut.setDerivedChange(extIncDetailVATIn);
        extIncDetailLocTax = addDProp(outPrmsGroup, "extIncDetailLocTax", "Местн. нал.", DoubleClass.instance, extIncomeDetail);

        extIncDetailCalcPriceOut = addJProp("Цена розн. (расч.)", roundm1,
                addJProp("Цена розн. (расч. - неокр.)", addPercent,
                        addJProp("Цена с НДС", addPercent,
                                addJProp("Цена с надбавкой", addPercent,
                                        extIncDetailPriceIn, 1,
                                           extIncDetailAdd, 1), 1,
                                           extIncDetailVATOut, 1), 1,
                                           extIncDetailLocTax, 1), 1);

        extIncDetailPriceOut = addDProp(outPrmsGroup, "extIncDetailPriceOut", "Цена розн.", DoubleClass.instance, extIncomeDetail);
        extIncDetailPriceOut.setDerivedChange(extIncDetailCalcPriceOut);

        // ------------------------- Последняя строка ------------------------------ //
        
        extIncLastDetail = addMGProp(baseGroup,"extIncLastDetail", "Посл. строка", object(extIncomeDetail), extIncDetailDocument, 1, extIncDetailArticle, 1);

        extIncPriceIn = addJProp(incPrmsGroup, "Цена пост. (прих.)", extIncDetailPriceIn, extIncLastDetail, 1, 2);
        extIncVATIn = addJProp(incPrmsGroup, "НДС пост. (прих.)", extIncDetailVATIn, extIncLastDetail, 1, 2);
        extIncAdd = addJProp(outPrmsGroup, "Надбавка (прих.)", extIncDetailAdd, extIncLastDetail, 1, 2);
        extIncVATOut = addJProp(outPrmsGroup, "НДС прод. (прих.)", extIncDetailVATOut, extIncLastDetail, 1, 2);
        extIncLocTax = addJProp(outPrmsGroup, "Местн. нал. (прих.)", extIncDetailLocTax, extIncLastDetail, 1, 2);
        extIncPriceOut = addJProp(outPrmsGroup, "Цена розн. (прих.)", extIncDetailPriceOut, extIncLastDetail, 1, 2);
    }

    // Зафиксированные документы

    LP fixedPriceIn, fixedVATIn;
    LP fixedAdd, fixedVATOut, fixedLocTax;
    LP fixedPriceOut;

    private void initParamsPrimaryFixedProperties() {

        fixedPriceIn = addDProp("fixedPriceIn", "Цена пост.", DoubleClass.instance, fixedDocument, article);
        fixedVATIn = addDProp("fixedVATIn", "НДС пост.", DoubleClass.instance, fixedDocument, article);
        fixedAdd = addDProp("fixedAdd", "Надбавка", DoubleClass.instance, fixedDocument, article);
        fixedVATOut = addDProp("fixedVATOut", "НДС прод.", DoubleClass.instance, fixedDocument, article);
        fixedLocTax = addDProp("fixedLocTax", "Местн. нал.", DoubleClass.instance, fixedDocument, article);
        fixedPriceOut = addDProp("fixedPriceOut", "Цена розн.", DoubleClass.instance, fixedDocument, article);
    }

    // Переоценка

    LP revaluedBalanceQuantity;
    LP revalPriceIn, revalVATIn;
    LP revalAddBefore, revalVATOutBefore, revalLocTaxBefore;
    LP revalPriceOutBefore;
    LP revalAddAfter, revalVATOutAfter, revalLocTaxAfter;
    LP revalPriceOutAfter;
    
    private void initParamsPrimaryRevalProperties() {

        revaluedBalanceQuantity = addJProp("Остаток (переоц.)", and1, revalBalanceQuantity, 1, 2, isRevalued, 1, 2);

        revalPriceIn = addDProp("revalPriceIn", "Цена пост.", DoubleClass.instance, revalDocument, article);
        revalVATIn = addDProp("revalVATIn", "НДС пост.", DoubleClass.instance, revalDocument, article);
        revalAddBefore = addDProp("revalAddBefore", "Надбавка (до)", DoubleClass.instance, revalDocument, article);
        revalVATOutBefore = addDProp("revalVATOutBefore", "НДС прод. (до)", DoubleClass.instance, revalDocument, article);
        revalLocTaxBefore = addDProp("revalLocTaxBefore", "Местн. нал. (до)", DoubleClass.instance, revalDocument, article);
        revalPriceOutBefore = addDProp("revalPriceOutBefore", "Цена розн. (до)", DoubleClass.instance, revalDocument, article);
        revalAddAfter = addDProp(outPrmsGroupAfter, "revalAddAfter", "Надбавка (после)", DoubleClass.instance, revalDocument, article);
        revalVATOutAfter = addDProp(outPrmsGroupAfter, "revalVATOutAfter", "НДС прод. (после)", DoubleClass.instance, revalDocument, article);
        revalLocTaxAfter = addDProp(outPrmsGroupAfter, "revalLocTaxAfter", "Местн. нал. (после)", DoubleClass.instance, revalDocument, article);
        revalPriceOutAfter = addDProp(outPrmsGroupAfter, "revalPriceOutAfter", "Цена розн. (после)", DoubleClass.instance, revalDocument, article);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LP primDocPriceIn;
    LP primDocVATIn;
    LP primDocAdd;
    LP primDocVATOut;
    LP primDocLocTax;
    LP primDocPriceOut;

    private void initParamsOverrideProperties() {

        primDocPriceIn = addCUProp(paramsGroup, "Цена пост. (изм.)", fixedPriceIn, extIncPriceIn, revalPriceIn);
        primDocVATIn = addCUProp(paramsGroup, "НДС пост. (изм.)", fixedVATIn, extIncVATIn, revalVATIn);
        primDocAdd = addCUProp(paramsGroup, "Надбавка (изм.)", fixedAdd, extIncAdd, revalAddAfter);
        primDocVATOut = addCUProp(paramsGroup, "НДС прод. (изм.)", fixedVATOut, extIncVATOut, revalVATOutAfter);
        primDocLocTax = addCUProp(paramsGroup, "Местн. нал. (изм.)", fixedLocTax, extIncLocTax, revalLocTaxAfter);
        primDocPriceOut = addCUProp(paramsGroup, "Цена розн. (изм.)", fixedPriceOut, extIncPriceOut, revalPriceOutAfter);
    }

    // -------------------------------------- Текущие свойства ---------------------------------------------- //

    LP storeSupplier;

    LP storePriceIn, storeVATIn;
    LP storeAdd, storeVATOut, storeLocTax;
    LP storePriceOut;

    private void initParamsCurrentProperties() {

        storeSupplier = addJProp(supplierGroup, "Посл. пост.", extIncSupplier, maxStoreExtIncDoc, 1, 2);

        storePriceIn = addJProp(incPrmsGroup, "Цена пост. (тек.)", primDocPriceIn, maxChangesParamsDoc, 1, 2, 2);
        storeVATIn = addJProp(incPrmsGroup, "НДС пост. (тек.)", primDocVATIn, maxChangesParamsDoc, 1, 2, 2);
        storeAdd = addJProp(outPrmsGroup, "Надбавка (тек.)", primDocAdd, maxChangesParamsDoc, 1, 2, 2);
        storeVATOut = addJProp(outPrmsGroup, "НДС прод. (тек.)", primDocVATOut, maxChangesParamsDoc, 1, 2, 2);
        storeLocTax = addJProp(outPrmsGroup, "Местн. нал. (тек.)", primDocLocTax, maxChangesParamsDoc, 1, 2, 2);
        storePriceOut = addJProp(outPrmsGroup, "Цена розн. (тек.)", primDocPriceOut, maxChangesParamsDoc, 1, 2, 2);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Суммовые свойства ----------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void initSumProperties() {

        initSumInProperties();
        initSumOutProperties();
        initSumAccountProperties();
    }

    // ---------------------------------------- Входные суммы --------------------------------------------- //

    LP extIncDetailCalcSum;
    LP extIncDetailCalcSumVATIn;
    LP extIncDetailCalcSumPay;
    LP extIncDetailSumVATIn, extIncDetailSumPay;
    LP extIncDetailSumInc;
    LP extIncDocumentSumInc, extIncDocumentSumVATIn, extIncDocumentSumPay;

    private void initSumInProperties() {

        // -------------------------- Входные суммы ---------------------------- //

        extIncDetailCalcSum = addJProp("Сумма НДС (расч.)", round,
                addJProp("Сумма пост.", multiplyDouble2, extIncDetailQuantity, 1, extIncDetailPriceIn, 1), 1);

        extIncDetailCalcSumVATIn = addJProp("Сумма НДС (расч.)", round,
                addJProp("Сумма НДС (расч. - неокр.)", percent, extIncDetailCalcSum, 1, extIncDetailVATIn, 1), 1);

        extIncDetailSumVATIn = addDProp(incSumsGroup, "extIncDetailSumVATIn", "Сумма НДС", DoubleClass.instance, extIncomeDetail);
        extIncDetailSumVATIn.setDerivedChange(extIncDetailCalcSumVATIn);

        extIncDetailCalcSumPay = addSUProp("Всего с НДС (расч.)", Union.SUM, extIncDetailCalcSum, extIncDetailSumVATIn);

        extIncDetailSumPay = addDProp(incSumsGroup, "extIncDetailSumPay", "Всего с НДС", DoubleClass.instance, extIncomeDetail);
        extIncDetailSumPay.setDerivedChange(extIncDetailCalcSumPay);

        extIncDetailSumInc = addUProp(incSumsGroup, "extIncDetailSumInc", "Сумма пост.", Union.SUM, 1, extIncDetailSumPay, 1, -1, extIncDetailSumVATIn, 1);
        setPropOrder(extIncDetailSumInc, extIncDetailSumVATIn, true);

        extIncDocumentSumInc = addSGProp(incSumsGroup, "extIncDocumentSumInc", "Сумма пост.", extIncDetailSumInc, extIncDetailDocument, 1);
        extIncDocumentSumVATIn = addSGProp(incSumsGroup, "extIncDocumentSumVATIn", "Сумма НДС", extIncDetailSumVATIn, extIncDetailDocument, 1);
        extIncDocumentSumPay = addSGProp(incSumsGroup, "extIncDocumentSumPay", "Всего с НДС", extIncDetailSumPay, extIncDetailDocument, 1);

    }

    // ---------------------------------------- Выходные суммы --------------------------------------------- //

    private void initSumOutProperties() {

        initSumPriceOutProperties();
        initSumLocTaxProperties();
        initSumVATOutProperties();
        initSumAddProperties();
    }

    // Розничные суммы

    LP extIncDetailSumPriceOut;
    LP extIncDocumentSumPriceOut;

    LP intraSumPriceOut;


    LP receiptSumPriceOut;
    LP receiptDocumentSumPriceOut;
    LP cashSaleSumPriceOut;

    LP extOutSumPriceOut;
    LP extOutDocumentSumPriceOut;
    LP exchIncSumPriceOut, exchOutSumPriceOut;
    LP exchDltSumPriceOut;
    LP revalSumPriceOutBefore, revalSumPriceOutAfter;
    LP revalSumPriceOut;

    private void initSumPriceOutProperties() {

        extIncDetailSumPriceOut = addJProp(outSumsGroup, "extIncDetailSumPriceOut", "Сумма розн.", round,
                addJProp("Сумма розн. (неокр.)", multiplyDouble2, extIncDetailQuantity, 1, extIncDetailPriceOut, 1), 1);
        extIncDocumentSumPriceOut = addSGProp(outSumsGroup, "extIncDocumentSumPriceOut", "Сумма розн. (всего)", extIncDetailSumPriceOut, extIncDetailDocument, 1);

        intraSumPriceOut = addJProp("Сумма розн. (вн.)", multiplyDouble2, intraQuantity, 1, 2, fixedPriceOut, 1, 2);

        receiptSumPriceOut = addDProp(outSumsGroup, "receiptSumPriceOut", "Сумма по чеку", DoubleClass.instance, receipt, article);
        receiptDocumentSumPriceOut = addSGProp(outSumsGroup, "Сумма чека", receiptSumPriceOut, 1);
        cashSaleSumPriceOut = addSGProp(outSumsGroup, "Сумма прод.", receiptSumPriceOut, receiptSaleDocument, 1, 2);

        LP extOutParamsSumPriceOut = addJProp("Сумма розн. (расх. расч.)", multiplyDouble2, extOutQuantity, 1, 2, fixedPriceOut, 1, 2);
        extOutSumPriceOut = addCUProp("Сумма розн. (расх. расч.)", extOutParamsSumPriceOut, cashSaleSumPriceOut);
        extOutDocumentSumPriceOut = addSGProp(outSumsGroup, "Сумма розн. (всего)", extOutSumPriceOut, 1);

        exchIncSumPriceOut = addJProp("Сумма розн. (перес. +)", multiplyDouble2, exchIncQuantity, 1, 2, fixedPriceOut, 1, 2);
        exchOutSumPriceOut = addJProp("Сумма розн. (перес. -)", multiplyDouble2, exchOutQuantity, 1, 2, fixedPriceOut, 1, 2);
        exchDltSumPriceOut = addDUProp("Сумма розн. (перес.)", exchIncSumPriceOut, exchOutSumPriceOut);

        revalSumPriceOutBefore = addJProp("Сумма розн. (переоц. до)", multiplyDouble2, revaluedBalanceQuantity, 1, 2, revalPriceOutBefore, 1, 2);
        revalSumPriceOutAfter = addJProp("Сумма розн. (переоц. после)", multiplyDouble2, revaluedBalanceQuantity, 1, 2, revalPriceOutAfter, 1, 2);
        revalSumPriceOut = addDUProp("Сумма розн. (переоц.)", revalSumPriceOutAfter, revalSumPriceOutBefore);
    }

    // Налог с продаж
    LP extIncDetailSumLocTax;
    LP extIncDocumentSumLocTax;
    LP extIncDetailSumWVAT;

    private void initSumLocTaxProperties() {

        extIncDetailSumLocTax = addJProp(outSumsGroup, "extIncDetailSumLocTax", "Сумма местн. нал.", round,
                addJProp("Сумма местн. нал. (неокр.)", revPercent, extIncDetailSumPriceOut, 1, extIncDetailLocTax, 1), 1);
        setPropOrder(extIncDetailSumLocTax, extIncDetailSumPriceOut, true);
        extIncDocumentSumLocTax = addSGProp(outSumsGroup, "extIncDocumentSumLocTax", "Сумма местн. нал. (всего)", extIncDetailSumLocTax, extIncDetailDocument, 1);

        extIncDetailSumWVAT = addDUProp("Сумма с НДС (розн.)", extIncDetailSumPriceOut, extIncDetailSumLocTax);
    }

    // НДС розничный
    LP extIncDetailSumVATOut;
    LP extIncDocumentSumVATOut;
    LP extIncDetailSumWAdd;

    private void initSumVATOutProperties() {

        extIncDetailSumVATOut = addJProp(outSumsGroup, "extIncDetailSumVATOut", "Сумма НДС розн.", round,
                addJProp("Сумма НДС (розн. неокр.)", revPercent, extIncDetailSumWVAT, 1, extIncDetailVATOut, 1), 1);
        setPropOrder(extIncDetailSumVATOut, extIncDetailSumLocTax, true);
        extIncDocumentSumVATOut = addSGProp(outSumsGroup, "extIncDocumentSumVATOut", "Сумма НДС розн. (всего)", extIncDetailSumVATOut, extIncDetailDocument, 1);

        extIncDetailSumWAdd = addDUProp("Сумма с торг. надб.", extIncDetailSumWVAT, extIncDetailSumVATOut);
    }

    // Торговая надбавка
    LP extIncDetailSumAdd;
    LP extIncDocumentSumAdd;

    private void initSumAddProperties() {

        extIncDetailSumAdd = addUProp(outSumsGroup, "extIncDetailSumAdd", "Сумма торг. надб.", Union.SUM, 1, extIncDetailSumWAdd, 1, -1, extIncDetailSumInc, 1);
        setPropOrder(extIncDetailSumAdd, extIncDetailSumVATOut, true);
        extIncDocumentSumAdd = addSGProp(outSumsGroup, "extIncDocumentSumAdd", "Сумма торг. надб. (всего)", extIncDetailSumAdd, extIncDetailDocument, 1);
    }

    // ---------------------------------------- Учетные суммы ------------------------------------------------ //

    LP accExcl;

    LP extIncDetailSumAccount, intraSumAccount;
    LP extOutSumAccountExcl;
    LP extOutSumAccount;
    LP extIncSumAccount;
    LP exchIncSumAccount, exchOutSumAccount;
    LP exchDltSumAccount;
    LP revalSumAccount;

    LP incSumAccount, outSumAccount;
    LP incDocSumAccount, outDocSumAccount;
    LP incDocStoreSumAccount, outDocStoreSumAccount;
    LP dltDocStoreSumAccount;

    LP incGroeqDateSumAccount, outGroeqDateSumAccount;
    LP incStoreGroeqDateSumAccount, outStoreGroeqDateSumAccount;
    LP dltStoreGroeqDateSumAccount;

    LP incGreaterDateSumAccount, outGreaterDateSumAccount;
    LP incStoreGreaterDateSumAccount, outStoreGreaterDateSumAccount;
    LP dltStoreGreaterDateSumAccount;

    LP incStoreSumAccount, outStoreSumAccount;
    LP balanceDocStoreArticleSumAccount;
    LP balanceStoreArticleSumAccount;
    LP dltStoreArticleSumAccount;
    LP balanceDocStoreSumAccount, balanceStoreSumAccount, dltStoreSumAccount;

    LP balanceDocStoreDateMSumAccount;
    LP balanceDocStoreDateESumAccount;

    private void initSumAccountProperties() {

        accExcl = addDProp(accountGroup, "accExcl", "Искл.", LogicalClass.instance, accountDocument, article);

        extIncDetailSumAccount = extIncDetailSumPriceOut;
        extIncSumAccount = addSGProp("Сумма учетн. (вх.)", extIncDetailSumAccount, extIncDetailDocument, 1, extIncDetailArticle, 1);

        intraSumAccount = intraSumPriceOut;
        extOutSumAccountExcl = addJProp("Сумма учетн. искл. (вых.)", and1, extOutSumPriceOut, 1, 2, accExcl, 1, 2);
        extOutSumAccount = addDUProp("Сумма учетн. (вых.)", extOutSumPriceOut, extOutSumAccountExcl);
        exchIncSumAccount = exchIncSumPriceOut;
        exchOutSumAccount = exchOutSumPriceOut;
        exchDltSumAccount = exchDltSumPriceOut;
        revalSumAccount = revalSumPriceOut;

        incSumAccount = addCUProp("Сумма учетн. прих.", extIncSumAccount, intraSumAccount, exchIncSumAccount, revalSumAccount);
        outSumAccount = addCUProp("Сумма учетн. расх.", extOutSumAccount, intraSumAccount, exchOutSumAccount);

        incDocSumAccount = addSGProp(accountGroup, "Сумма учетн. прих. на скл.", incSumAccount, 1);
        outDocSumAccount = addSGProp(accountGroup, "Сумма учетн. расх. со скл.", outSumAccount, 1);

        incDocStoreSumAccount = addJProp("Сумма учетн. прих. по скл.", and1, incDocSumAccount, 1, isDocIncSStore, 1, 2);
        outDocStoreSumAccount = addJProp("Сумма учетн. расх. по скл.", and1, outDocSumAccount, 1, isDocOutSStore, 1, 2);
        dltDocStoreSumAccount = addDUProp("Сумма учетн. товара (+-)", incDocStoreSumAccount, outDocStoreSumAccount);


        incGroeqDateSumAccount = addJProp("Сумма учетн. прих. с даты", and1, incSumAccount, 1, 2, groeqDocDate, 1, 3);
        outGroeqDateSumAccount = addJProp("Сумма учетн. расх. с даты", and1, outSumAccount, 1, 2, groeqDocDate, 1, 3);

        incStoreGroeqDateSumAccount = addSGProp("Сумма учетн. прих. на скл. с даты", incGroeqDateSumAccount, incSStore, 1, 3);
        outStoreGroeqDateSumAccount = addSGProp("Сумма учетн. расх. со скл. с даты", outGroeqDateSumAccount, outSStore, 1, 3);
        dltStoreGroeqDateSumAccount = addDUProp("Сумма учетн. на скл. с даты", incStoreGroeqDateSumAccount, outStoreGroeqDateSumAccount);

        incGreaterDateSumAccount = addJProp("Сумма учетн. прих. после даты", and1, incSumAccount, 1, 2, greaterDocDate, 1, 3);
        outGreaterDateSumAccount = addJProp("Сумма учетн. расх. после даты", and1, outSumAccount, 1, 2, greaterDocDate, 1, 3);

        incStoreGreaterDateSumAccount = addSGProp("Сумма учетн. прих. на скл. после даты", incGreaterDateSumAccount, true, incSStore, 1, 3);
        outStoreGreaterDateSumAccount = addSGProp("Сумма учетн. расх. со скл. после даты", outGreaterDateSumAccount, true, outSStore, 1, 3);
        dltStoreGreaterDateSumAccount = addDUProp("Сумма учетн. на скл. после даты", incStoreGreaterDateSumAccount, outStoreGreaterDateSumAccount);

        incStoreSumAccount = addSGProp("Сумма учетн. прих. на скл.", incSumAccount, incSStore, 1, 2);
        outStoreSumAccount = addSGProp("Сумма учетн. расх. со скл.", outSumAccount, outSStore, 1, 2);
        balanceDocStoreArticleSumAccount = addDUProp("Сумма товара на скл. (по док.)", incStoreSumAccount, outStoreSumAccount);

        balanceStoreArticleSumAccount = addJProp("Сумма товара на скл. (по ост.)", multiplyDouble2, balanceStoreQuantity, 1, 2, storePriceOut, 1, 2);
        dltStoreArticleSumAccount = addDUProp(accountGroup, "Отклонение суммы товара на скл.", balanceDocStoreArticleSumAccount, balanceStoreArticleSumAccount);

        balanceDocStoreSumAccount = addSGProp(accountGroup, "Сумма на скл. (по док.)", balanceDocStoreArticleSumAccount, 1);
        balanceStoreSumAccount = addSGProp(accountGroup, "Сумма на скл. (по ост.)", balanceStoreArticleSumAccount, 1);
        dltStoreSumAccount = addSGProp(accountGroup, "Отклонение суммы на скл.", dltStoreArticleSumAccount, 1);

        balanceDocStoreDateMSumAccount = addDUProp(accountGroup, "Сумма учетн. на начало", addJProp("", and1, balanceDocStoreSumAccount, 1, is(DateClass.instance), 2), dltStoreGroeqDateSumAccount);
        balanceDocStoreDateESumAccount = addDUProp(accountGroup, "Сумма учетн. на конец", addJProp("", and1, balanceDocStoreSumAccount , 1, is(DateClass.instance), 2), dltStoreGreaterDateSumAccount);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------- Текущие параметры по документам --------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void initDocCurrentProperties() {

        initDocCurrentBalanceProperties();
        initDocCurrentFixedProperties();
        initDocCurrentRevalProperties();
    }

    // ------------------------------------------- Остатки -------------------------------------------------- //

    LP docOutBalanceQuantity, docIncBalanceQuantity, docRevBalanceQuantity;

    private void initDocCurrentBalanceProperties() {

        docOutBalanceQuantity = addJProp(balanceGroup, "Остаток (расх.)", balanceStoreQuantity, outQStore, 1, 2);
        docIncBalanceQuantity = addJProp(balanceGroup, "Остаток (прих.)", balanceStoreQuantity, incQStore, 1, 2);
        docRevBalanceQuantity = addJProp(balanceGroup, "Остаток (переоц.)", balanceStoreQuantity, revalStore, 1, 2);
    }

    // ----------------------------------- Фискированные документы ------------------------------------------ //

    LP docCurPriceIn, docCurVATIn;
    LP docCurAdd, docCurVATOut, docCurLocTax;
    LP docCurPriceOut;

    LP docOverPriceIn;
    LP docOverVATIn;
    LP docOverAdd;
    LP docOverVATOut;
    LP docOverLocTax;
    LP docOverPriceOut;

    private void initDocCurrentFixedProperties() {

        docCurPriceIn = addJProp("Цена пост. (тек.)", storePriceIn, fixedStore, 1, 2);
        docCurVATIn = addJProp("НДС пост. (тек.)", storeVATIn, fixedStore, 1, 2);
        docCurAdd = addJProp("Надбавка (тек.)", storeAdd, fixedStore, 1, 2);
        docCurVATOut = addJProp("НДС прод. (тек.)", storeVATOut, fixedStore, 1, 2);
        docCurLocTax = addJProp("Местн. нал. (тек.)", storeLocTax, fixedStore, 1, 2);
        docCurPriceOut = addJProp("Цена розн. (тек.)", storePriceOut, fixedStore, 1, 2);

        docOverPriceIn = addSUProp(incPrmsGroup, "Цена пост.", Union.OVERRIDE, docCurPriceIn, fixedPriceIn);
        docOverVATIn = addSUProp(incPrmsGroup, "НДС пост.", Union.OVERRIDE, docCurVATIn, fixedVATIn);
        docOverAdd = addSUProp(outPrmsGroup, "Надбавка", Union.OVERRIDE, docCurAdd, fixedAdd);
        docOverVATOut = addSUProp(outPrmsGroup, "НДС прод.", Union.OVERRIDE, docCurVATOut, fixedVATOut);
        docOverLocTax = addSUProp(outPrmsGroup, "Местн. нал.", Union.OVERRIDE, docCurLocTax, fixedLocTax);
        docOverPriceOut = addSUProp(outPrmsGroup, "Цена розн.", Union.OVERRIDE, docCurPriceOut, fixedPriceOut);
    }

    // --------------------------------------- Переоценки -------------------------------------------- //

    LP revalCurPriceIn, revalCurVATIn;
    LP revalCurAdd, revalCurVATOut, revalCurLocTax;
    LP revalCurPriceOut;

    LP revalOverBalanceQuantity;
    LP revalOverPriceIn;
    LP revalOverVATIn;
    LP revalOverAddBefore;
    LP revalOverVATOutBefore;
    LP revalOverLocTaxBefore;
    LP revalOverPriceOutBefore;

    private void initDocCurrentRevalProperties() {

        revalCurPriceIn = addJProp("Цена пост. (тек.)", storePriceIn, revalStore, 1, 2);
        revalCurVATIn = addJProp("НДС пост. (тек.)", storeVATIn, revalStore, 1, 2);
        revalCurAdd = addJProp("Надбавка (тек.)", storeAdd, revalStore, 1, 2);
        revalCurVATOut = addJProp("НДС прод. (тек.)", storeVATOut, revalStore, 1, 2);
        revalCurLocTax = addJProp("Местн. нал. (тек.)", storeLocTax, revalStore, 1, 2);
        revalCurPriceOut = addJProp("Цена розн. (тек.)", storePriceOut, revalStore, 1, 2);

        revalOverBalanceQuantity = addSUProp(balanceGroup, "Остаток", Union.OVERRIDE, docRevBalanceQuantity, revalBalanceQuantity);
        revalOverPriceIn = addSUProp(incPrmsGroupBefore, "Цена пост.", Union.OVERRIDE, revalCurPriceIn, revalPriceIn);
        revalOverVATIn = addSUProp(incPrmsGroupBefore, "НДС пост.", Union.OVERRIDE, revalCurVATIn, revalVATIn);
        revalOverAddBefore = addSUProp(outPrmsGroupBefore, "Надбавка (до)", Union.OVERRIDE, revalCurAdd, revalAddBefore);
        revalOverVATOutBefore = addSUProp(outPrmsGroupBefore, "НДС прод. (до)", Union.OVERRIDE, revalCurVATOut, revalVATOutBefore);
        revalOverLocTaxBefore = addSUProp(outPrmsGroupBefore, "Местн. нал. (до)", Union.OVERRIDE, revalCurLocTax, revalLocTaxBefore);
        revalOverPriceOutBefore = addSUProp(outPrmsGroupBefore, "Цена розн. (до)", Union.OVERRIDE, revalCurPriceOut, revalPriceOutBefore);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------- Конкретные классы товаров --------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    void initCustomArticleLogics() {

        // конкретные классы
        CustomClass articleFood = addConcreteClass("Продтовары", article);
        addDProp(baseGroup, "Срок годности", StringClass.get(10), articleFood);

        CustomClass articleAlcohol = addConcreteClass("Алкоголь", articleFood);
        addDProp(baseGroup, "Крепость", IntegerClass.instance, articleAlcohol);

        CustomClass articleVodka = addConcreteClass("Водка", articleAlcohol);
        addDProp(baseGroup, "Прейск.", LogicalClass.instance, articleVodka);

        CustomClass articleBeer = addConcreteClass("Пиво", articleAlcohol);
        addDProp(baseGroup, "Тип", StringClass.get(10), articleBeer);
        addDProp(baseGroup, "Упак.", StringClass.get(10), articleBeer);

        CustomClass wineTaste = addConcreteClass("Вкус вина", namedObject);
        CustomClass articleWine = addConcreteClass("Вино", articleAlcohol);
        addJProp(baseGroup, "Вкус", name, addDProp("Код вкуса", wineTaste, articleWine), 1);

        CustomClass articleMilkGroup = addConcreteClass("Молочные продукты", articleFood);
        addDProp(baseGroup, "Жирн.", DoubleClass.instance, articleMilkGroup);

        CustomClass articleMilk = addConcreteClass("Молоко", articleMilkGroup);
        addDProp(baseGroup, "Упак.", StringClass.get(10),  articleMilk);

        CustomClass articleCheese = addConcreteClass("Сыр", articleMilkGroup);
        addDProp(baseGroup, "Вес.", LogicalClass.instance, articleCheese);

        CustomClass articleCurd = addConcreteClass("Творог", articleMilkGroup);

        CustomClass articleBreadGroup = addConcreteClass("Хлебобулочные изделия", articleFood);
        addDProp(baseGroup, "Вес", IntegerClass.instance, articleBreadGroup);

        CustomClass articleBread = addConcreteClass("Хлеб", articleBreadGroup);
        addDProp(baseGroup, "Вес", IntegerClass.instance, articleBread);

        CustomClass articleCookies = addConcreteClass("Печенье", articleBreadGroup);

        CustomClass articleJuice = addConcreteClass("Соки", articleFood);
        addDProp(baseGroup, "Вкус", StringClass.get(10), articleJuice);
        addDProp(baseGroup, "Литраж", IntegerClass.instance, articleJuice);

        CustomClass articleClothes = addConcreteClass("Одежда", article);
        addDProp(baseGroup, "Модель", StringClass.get(10), articleClothes);

        CustomClass shirtSize = addConcreteClass("Размер майки", namedObject);
        CustomClass articleTShirt = addConcreteClass("Майки", articleClothes);
        addJProp(baseGroup, "Размер", name, addDProp("Код размера", shirtSize, articleTShirt), 1);

        CustomClass articleJeans = addConcreteClass("Джинсы", articleClothes);
        addDProp(baseGroup, "Ширина", IntegerClass.instance, articleJeans);
        addDProp(baseGroup, "Длина", IntegerClass.instance, articleJeans);

        CustomClass articleShooes = addConcreteClass("Обувь", article);
        addDProp(baseGroup, "Цвет", StringClass.get(10), articleShooes);
    }

    protected void initConstraints() {

//        Constraints.put(balanceStoreQuantity.Property,new PositiveConstraint());
    }

    protected void initPersistents() {

        persistents.add((AggregateProperty)docStore.property);

        persistents.add((AggregateProperty)docDate.property);

        persistents.add((AggregateProperty)extIncQuantity.property);

        persistents.add((AggregateProperty)incStoreQuantity.property);
        persistents.add((AggregateProperty)outStoreQuantity.property);
        persistents.add((AggregateProperty)maxChangesParamsDate.property);
        persistents.add((AggregateProperty)maxChangesParamsDoc.property);

        persistents.add((AggregateProperty)extIncLastDetail.property);

//        persistents.add((AggregateProperty)outQStore.property);
//        persistents.add((AggregateProperty)incQStore.property);
    }

    protected void initTables() {

        tableFactory.include("article",article);
        tableFactory.include("document",document);
        tableFactory.include("store",store);
        tableFactory.include("articlegroup",articleGroup);
        tableFactory.include("articledocument",article,document);
        tableFactory.include("articlestore",article,store);
    }

    protected void initIndexes() {
        addIndex(extIncDate);
        addIndex(intraDate);
        addIndex(extOutDate);
        addIndex(exchDate);
        addIndex(revalDate);
        addIndex(maxChangesParamsDate);
        addIndex(docStore);
    }

    NavigatorElement primaryData;
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

        primaryData = new NavigatorElement(baseElement, 100, "Первичные данные");
            extIncDetailForm = new ExtIncDetailNavigatorForm(primaryData, 110, "Внешний приход");
                extIncForm = new ExtIncNavigatorForm(extIncDetailForm, 115, "Внешний приход по товарам");
                extIncPrintForm = new ExtIncPrintNavigatorForm(extIncDetailForm, 117, "Реестр цен");
            intraForm = new IntraNavigatorForm(primaryData, 120, "Внутреннее перемещение");
            extOutForm = new ExtOutNavigatorForm(primaryData, 130, "Внешний расход");
                cashSaleForm = new CashSaleNavigatorForm(extOutForm, 131, "Реализация по кассе");
                    receiptForm = new ReceiptNavigatorForm(cashSaleForm, 1310, "Реализация по кассе (чеки)");
                clearingSaleForm = new ClearingSaleNavigatorForm(extOutForm, 132, "Реализация по б/н расчету");
                invForm = new InvNavigatorForm(extOutForm, 134, "Инвентаризация", false);
                    invStoreForm = new InvNavigatorForm(invForm, 1341, "Инвентаризация по складам", true);
                returnForm = new ReturnNavigatorForm(extOutForm, 136, "Возврат поставщику");
            exchangeForm = new ExchangeNavigatorForm(primaryData, 140, "Пересорт");
                exchangeMForm = new ExchangeMNavigatorForm(exchangeForm, 142, "Сводный пересорт");
            revalueForm = new RevalueNavigatorForm(primaryData, 150, "Переоценка", false);
                revalueStoreForm = new RevalueNavigatorForm(revalueForm, 155, "Переоценка по складам", true);

        aggregateData = new NavigatorElement(baseElement, 200, "Сводная информация");
            aggrStoreData = new NavigatorElement(aggregateData, 210, "Склады");
                storeArticleForm = new StoreArticleNavigatorForm(aggrStoreData, 211, "Товары по складам");
                    storeArticlePrimDocForm = new StoreArticlePrimDocNavigatorForm(storeArticleForm, 2111, "Товары по складам (изм. цен)");
                    storeArticleDocForm = new StoreArticleDocNavigatorForm(storeArticleForm, 2112, "Товары по складам (док.)");
            aggrArticleData = new NavigatorElement(aggregateData, 220, "Товары");
                articleStoreForm = new ArticleStoreNavigatorForm(aggrArticleData, 221, "Склады по товарам");
                articleMStoreForm = new ArticleMStoreNavigatorForm(aggrArticleData, 222, "Товары*Склады");
            aggrSupplierData = new NavigatorElement(aggregateData, 230, "Поставщики");
                supplierStoreArticleForm = new SupplierStoreArticleNavigatorForm(aggrSupplierData, 231, "Остатки по складам");

        analyticsData = new NavigatorElement(baseElement, 300, "Аналитические данные");
            dateIntervalForms = new NavigatorElement(analyticsData, 310, "За интервал дат");
                mainAccountForm = new MainAccountNavigatorForm(dateIntervalForms, 311, "Товарный отчет");
                salesArticleStoreForm = new SalesArticleStoreNavigatorForm(dateIntervalForms, 313, "Реализация товара по складам");

        extIncomeDocument.addRelevant(extIncDetailForm);
        intraDocument.addRelevant(intraForm);
        extOutcomeDocument.addRelevant(extOutForm);
        clearingSaleDocument.addRelevant(clearingSaleForm);
        invDocument.addRelevant(invForm);
        exchangeDocument.addRelevant(exchangeForm);
        revalDocument.addRelevant(revalueForm);

        extIncDetailForm.relevantElements.add(extIncPrintForm);
    }

    private class TmcNavigatorForm extends NavigatorForm {

        TmcNavigatorForm(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);
        }

        TmcNavigatorForm(NavigatorElement parent, int iID, String caption, boolean isPrintForm) {
            super(parent, iID, caption, isPrintForm);
        }

        void addArticleRegularFilterGroup(PropertyObjectNavigator documentProp, Object documentValue, PropertyObjectNavigator... extraProps) {

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
/*            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));*/
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(documentProp, Compare.NOT_EQUALS, documentValue),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));

            int functionKey = KeyEvent.VK_F9;

            for (PropertyObjectNavigator extraProp : extraProps) {
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      new CompareFilterNavigator(extraProp, Compare.NOT_EQUALS, 0),
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

            objDoc = addSingleGroupObjectImplement(extIncomeDocument, "Документ", properties,
                                                   baseGroup, storeGroup, supplierGroup, quantGroup, incSumsGroup);
        }
    }

    private class ExtIncDetailNavigatorForm extends ExtIncDocumentNavigatorForm {

        ObjectNavigator objDetail;

        public ExtIncDetailNavigatorForm(NavigatorElement parent, int ID, String caption) {
            this(parent, ID, caption, false);
        }
        public ExtIncDetailNavigatorForm(NavigatorElement parent, int ID, String caption, boolean isPrintForm) {
            super(parent, ID, caption, isPrintForm);

            objDetail = addSingleGroupObjectImplement(extIncomeDetail, "Строка", properties,
                                                                      artclGroup, quantGroup, incPrmsGroup, incSumsGroup, outPrmsGroup);

            PropertyObjectNavigator detDocument = addPropertyObjectImplement(extIncDetailDocument, objDetail);
            addFixedFilter(new CompareFilterNavigator(detDocument, Compare.EQUALS, objDoc));
        }
    }

    private class ExtIncNavigatorForm extends ExtIncDocumentNavigatorForm {

        public ExtIncNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption, false);

            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArt, properties,
                    balanceGroup, extIncQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyImplement(extIncQuantity), 0);
        }
    }

    private class ExtIncPrintNavigatorForm extends ExtIncDetailNavigatorForm {

        public ExtIncPrintNavigatorForm(NavigatorElement parent, int ID, String caption) throws JRException, FileNotFoundException {
            super(parent, ID, caption, true);

            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            objDoc.groupTo.banClassView = ClassViewType.GRID;

            addPropertyView(objDoc, properties, outSumsGroup);
            addPropertyView(objDetail, properties, outSumsGroup);

            objDoc.sID = "objDoc";
            getPropertyView(name.property, objDoc.groupTo).sID = "docName";

            //
            reportDesign = JRXmlLoader.load(getClass().getResourceAsStream("/tmc/reports/extIncLog.jrxml"));
        }
    }

    private class IntraNavigatorForm extends TmcNavigatorForm {

        public IntraNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(intraDocument, "Документ", properties,
                                                                        baseGroup, storeGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArt, properties,
                    balanceGroup, intraQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyImplement(intraQuantity), 0,
                                         getPropertyImplement(docOutBalanceQuantity),
                                         getPropertyImplement(docIncBalanceQuantity));

            addHintsNoUpdate(maxChangesParamsDoc);
        }
    }

    private class ExtOutNavigatorForm extends TmcNavigatorForm {

        public ExtOutNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(extOutcomeDocument, "Документ", properties,
                                                                        baseGroup, storeGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                    balanceGroup, extOutQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyImplement(extOutQuantity), 0,
                                         getPropertyImplement(docOutBalanceQuantity));
        }
    }

    private class CashSaleNavigatorForm extends TmcNavigatorForm {

        public CashSaleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(cashSaleDocument, "Документ", properties,
                                                                        baseGroup, storeGroup, outSumsGroup, accountGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                        balanceGroup, cashSaleQuantity, outSumsGroup, accountGroup);

            addArticleRegularFilterGroup(getPropertyImplement(cashSaleQuantity), 0,
                                         getPropertyImplement(docOutBalanceQuantity));

//            addPropertyView(objDoc, objArt, Properties, quantity, notZeroQuantity);
        }
    }

    private class ReceiptNavigatorForm extends TmcNavigatorForm {

        public ReceiptNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(cashSaleDocument, "Документ", properties,
                                                                        baseGroup, storeGroup, outSumsGroup, accountGroup);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            objDoc.groupTo.banClassView = ClassViewType.GRID;

            ObjectNavigator objReceipt = addSingleGroupObjectImplement(receipt, "Чек", properties,
                                                                        baseGroup, outSumsGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                        balanceGroup);

            addPropertyView(objReceipt, objArt, properties,
                        receiptQuantity, incPrmsGroup, outPrmsGroup, outSumsGroup);

            addPropertyView(objDoc, objArt, properties,
                        accountGroup);

            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(receiptSaleDocument, objReceipt), Compare.EQUALS, objDoc));

            addArticleRegularFilterGroup(getPropertyImplement(receiptQuantity), 0,
                                         getPropertyImplement(docOutBalanceQuantity));
        }
    }

    private class ClearingSaleNavigatorForm extends TmcNavigatorForm {

        public ClearingSaleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(clearingSaleDocument, "Документ", properties,
                                                                        baseGroup, storeGroup, customerGroup, extOutDocumentSumPriceOut);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                        balanceGroup, clearingSaleQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyImplement(clearingSaleQuantity), 0,
                                         getPropertyImplement(docOutBalanceQuantity));
        }
    }

    private class InvNavigatorForm extends TmcNavigatorForm {

        public InvNavigatorForm(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectNavigator objStore = null;
            if (groupStore) {
                objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                            baseGroup, accountGroup);
                objStore.groupTo.initClassView = ClassViewType.PANEL;
                objStore.groupTo.banClassView = ClassViewType.GRID;
            }

            ObjectNavigator objDoc = addSingleGroupObjectImplement(invDocument, "Документ", properties,
                                                                        baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                    balanceGroup, invBalance, invQuantity, incPrmsGroup, outPrmsGroup, accountGroup);

            addArticleRegularFilterGroup(getPropertyImplement(invQuantity), 0,
                                         getPropertyImplement(docOutBalanceQuantity));

            if (groupStore)
                addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(revalStore, objDoc), Compare.EQUALS, objStore));
            else
                addPropertyView(properties, storeGroup, false, objDoc);
        }
    }

    private class ReturnNavigatorForm extends TmcNavigatorForm {

        public ReturnNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(returnDocument, "Документ", properties,
                                                                        baseGroup, storeGroup, supplierGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                    balanceGroup, returnQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyImplement(returnQuantity), 0,
                                         getPropertyImplement(docOutBalanceQuantity));
        }
    }

    private class ExchangeNavigatorForm extends TmcNavigatorForm {

        public ExchangeNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(exchangeDocument, "Документ", properties,
                                                                        baseGroup, storeGroup);
            ObjectNavigator objArtTo = addSingleGroupObjectImplement(article, "Товар (на)", properties,
                                                                        baseGroup);
            ObjectNavigator objArtFrom = addSingleGroupObjectImplement(article, "Товар (c)", properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArtTo, properties,
                    docOutBalanceQuantity, exchIncQuantity, exchOutQuantity, incPrmsGroup, outPrmsGroup);
            addPropertyView(docOutBalanceQuantity, objDoc, objArtFrom);
            addPropertyView(exchangeQuantity, objDoc, objArtFrom, objArtTo);
            addPropertyView(objDoc, objArtFrom, properties, incPrmsGroup, outPrmsGroup);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
/*            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));*/
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyImplement(exchIncQuantity)),
                                  "Приход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyImplement(exchOutQuantity)),
                                  "Расход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyImplement(docOutBalanceQuantity, objArtTo), Compare.NOT_EQUALS, 0),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyImplement(docOutBalanceQuantity, objArtTo), Compare.LESS, 0),
                                  "Отр. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroup);

            filterGroup = new RegularFilterGroupNavigator(IDShift(1));
/*            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.SHIFT_DOWN_MASK)));*/
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyImplement(exchangeQuantity)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyImplement(docOutBalanceQuantity, objArtFrom), Compare.NOT_EQUALS, 0),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyImplement(docOutBalanceQuantity, objArtFrom), Compare.GREATER, 0),
                                  "Пол. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyImplement(docOverPriceOut, objArtFrom), Compare.EQUALS, getPropertyImplement(docOverPriceOut, objArtTo)),
                                  "Одинаковая розн. цена",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);

        }
    }

    private class ExchangeMNavigatorForm extends TmcNavigatorForm {

        public ExchangeMNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(exchangeDocument, "Документ", properties,
                                                                        baseGroup, storeGroup);

            GroupObjectNavigator gobjArts = new GroupObjectNavigator(IDShift(1));

            ObjectNavigator objArtTo = new ObjectNavigator(IDShift(1), article, "Товар (на)");
            ObjectNavigator objArtFrom = new ObjectNavigator(IDShift(1), article, "Товар (с)");

            gobjArts.add(objArtTo);
            gobjArts.add(objArtFrom);
            addGroup(gobjArts);

            addPropertyView(properties, baseGroup, false, objArtTo);
            addPropertyView(properties, baseGroup, false, objArtFrom);
            addPropertyView(exchangeQuantity, objDoc, objArtFrom, objArtTo);

            addFixedFilter(new NotNullFilterNavigator(getPropertyImplement(exchangeQuantity)));
        }
    }

    private class RevalueNavigatorForm extends TmcNavigatorForm {

        public RevalueNavigatorForm(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectNavigator objStore = null;
            if (groupStore) {
                objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                            baseGroup, accountGroup);
                objStore.groupTo.initClassView = ClassViewType.PANEL;
                objStore.groupTo.banClassView = ClassViewType.GRID;
            }

            ObjectNavigator objDoc = addSingleGroupObjectImplement(revalDocument, "Документ", properties,
                                                                        baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArt, properties,
                    revalOverBalanceQuantity, isRevalued, incPrmsGroupBefore, outPrmsGroupBefore, outPrmsGroupAfter);

            addArticleRegularFilterGroup(getPropertyImplement(isRevalued), false,
                                         getPropertyImplement(revalOverBalanceQuantity));

            if (groupStore)
                addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(revalStore, objDoc), Compare.EQUALS, objStore));
            else
                addPropertyView(properties, storeGroup, false, objDoc);
        }
    }

    private class StoreArticleNavigatorForm extends TmcNavigatorForm {

        ObjectNavigator objStore, objArt;

        public StoreArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                        baseGroup, accountGroup);
            objStore.groupTo.initClassView = ClassViewType.PANEL;
            objStore.groupTo.banClassView = ClassViewType.GRID;

            objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);

            addPropertyView(objStore, objArt, properties,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);
        }
    }

    private class StoreArticlePrimDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticlePrimDocNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objPrimDoc = addSingleGroupObjectImplement(primaryDocument, "Документ", properties,
                                                                                    baseGroup, paramsGroup);

            addPropertyView(objPrimDoc, objArt, properties,
                    paramsGroup);

            addFixedFilter(new NotNullFilterNavigator(getPropertyImplement(isDocArtChangesParams)));
            addFixedFilter(new CompareFilterNavigator(getPropertyImplement(primDocStore), Compare.EQUALS, objStore));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(getPropertyView(primDocDate), false);
            richDesign = formView;
        }
    }

    private class StoreArticleDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticleDocNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(quantityDocument, "Товарный документ", properties,
                                                                                    baseGroup, docDate, storeGroup, true, supplierGroup, true, customerGroup, true);

            addPropertyView(dltDocStoreQuantity, objDoc, objStore, objArt);

            addFixedFilter(new NotNullFilterNavigator(addPropertyObjectImplement(isDocStoreArtInclude, objDoc, objStore, objArt)));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(getPropertyView(docDate), false);
            richDesign = formView;
        }
    }

    private class ArticleStoreNavigatorForm extends TmcNavigatorForm {

        ObjectNavigator objStore, objArt;

        public ArticleStoreNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);

            objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                        baseGroup);

            addPropertyView(objStore, objArt, properties,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);

            addPropertyView(properties, baseGroup, false, objArt.groupTo, objStore, objArt);
            addPropertyView(properties, balanceGroup, false, objArt.groupTo, objStore, objArt);
            addPropertyView(properties, incPrmsGroup, false, objArt.groupTo, objStore, objArt);
            addPropertyView(properties, outPrmsGroup, false, objArt.groupTo, objStore, objArt);
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

            // добавить свойства по товару
            addPropertyView(properties, baseGroup, false, objArt);
            // добавить свойства по складу
            addPropertyView(properties, baseGroup, false, objStore);

            // добавить множественные свойства по товару и складу
            addPropertyView(objStore, objArt, properties,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);
        }
    }

    private class SupplierStoreArticleNavigatorForm extends TmcNavigatorForm {

        SupplierStoreArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            // создать блок "Поставщик"
            ObjectNavigator objSupplier = addSingleGroupObjectImplement(supplier, "Поставщик", properties,
                                                                                    baseGroup);
            objSupplier.groupTo.initClassView = ClassViewType.PANEL;
            objSupplier.groupTo.banClassView = ClassViewType.GRID;

            // создать блок "Склад"
            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                        baseGroup);
            objStore.groupTo.initClassView = ClassViewType.PANEL;

            // создать блок "Товар"
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            // добавить множественные свойства
            addPropertyView(objStore, objArt, properties,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);

            // установить фильтр по умолчанию на поставщик товара = поставщик
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(storeSupplier, objStore, objArt), Compare.EQUALS, objSupplier));

            // добавить стандартные фильтры
            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
/*            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));*/
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyImplement(balanceStoreQuantity), Compare.GREATER, 0),
                                  "Есть на складе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(getPropertyImplement(balanceStoreQuantity), Compare.LESS_EQUALS, 0),
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
            gobjInterval.initClassView = ClassViewType.PANEL;
            gobjInterval.banClassView = ClassViewType.GRID;

            objDateFrom = new ObjectNavigator(IDShift(1), DateClass.instance, "С даты :");
            objDateTo = new ObjectNavigator(IDShift(1), DateClass.instance, "По дату :");

            gobjInterval.add(objDateFrom);
            gobjInterval.add(objDateTo);
            addGroup(gobjInterval);
        }
    }

    private class MainAccountNavigatorForm extends DateIntervalNavigatorForm {

        public MainAccountNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                        baseGroup);
            ObjectNavigator objDoc = addSingleGroupObjectImplement(document, "Документ", properties,
                                                                                    baseGroup, docDate);

//            addPropertyView(balanceDocStoreDateMSumAccount, objStore, objDateFrom);
//            addPropertyView(balanceDocStoreDateESumAccount, objStore, objDateTo);

            addPropertyView(dltDocStoreSumAccount, objDoc, objStore);

            addFixedFilter(new CompareFilterNavigator(getPropertyImplement(dltDocStoreSumAccount), Compare.NOT_EQUALS, 0));
            addFixedFilter(new CompareFilterNavigator(getPropertyImplement(docDate), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterNavigator(getPropertyImplement(docDate), Compare.LESS_EQUALS, objDateTo));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(getPropertyView(docDate), true);
            richDesign = formView;

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

            addPropertyView(balanceStoreDateMQuantity, objStore, objArticle, objDateFrom);
            addPropertyView(incStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
            addPropertyView(outStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
            addPropertyView(balanceStoreDateEQuantity, objStore, objArticle, objDateTo);
            addPropertyView(saleStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
        }
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {

        SecurityPolicy securityPolicy;

        User user1 = addUser("user1");
        User user2 = addUser("user2");

        securityPolicy = new SecurityPolicy();

        securityPolicy.property.view.deny(extIncDocumentSumPay);
        securityPolicy.property.view.deny(incSumsGroup.getProperties());
        securityPolicy.property.change.deny(extIncDetailArticle);
        securityPolicy.property.change.deny(extIncDetailQuantity);

        securityPolicy.navigator.deny(analyticsData.getChildren(true));
        securityPolicy.navigator.deny(extIncPrintForm);

        securityPolicy.cls.edit.add.deny(document.getConcreteChildren());
        securityPolicy.cls.edit.remove.deny(baseGroup.getClasses());

        user2.addSecurityPolicy(securityPolicy);

    }

    // ------------------------------------- Временные методы --------------------------- //
/*
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
        classQuantity.put(receipt, modifier *8);
        classQuantity.put(clearingSaleDocument,((Double)(modifier *0.5)).intValue());
        classQuantity.put(invDocument,((Double)(modifier *0.2)).intValue());
        classQuantity.put(returnDocument,((Double)(modifier *0.3)).intValue());
        classQuantity.put(exchangeDocument, modifier);
        classQuantity.put(revalDocument,((Double)(modifier *0.5)).intValue());

        Map<DataProperty, Set<ClassPropertyInterface>> propNotNulls = new HashMap<DataProperty, Set<ClassPropertyInterface>>();
        artGroup.putNotNulls(propNotNulls,0);
        extIncDate.putNotNulls(propNotNulls,0);
        intraDate.putNotNulls(propNotNulls,0);
        extOutDate.putNotNulls(propNotNulls,0);
        exchDate.putNotNulls(propNotNulls,0);
        revalDate.putNotNulls(propNotNulls,0);
        extIncStore.putNotNulls(propNotNulls,0);
        intraIncStore.putNotNulls(propNotNulls,0);
        intraOutStore.putNotNulls(propNotNulls,0);
        extOutStore.putNotNulls(propNotNulls,0);
        exchStore.putNotNulls(propNotNulls,0);
        revalStore.putNotNulls(propNotNulls,0);
        intraIncStore.putNotNulls(propNotNulls,0);
        extIncSupplier.putNotNulls(propNotNulls,0);
        extIncDetailDocument.putNotNulls(propNotNulls,0);
        extIncDetailArticle.putNotNulls(propNotNulls,0);
        extIncDetailQuantity.putNotNulls(propNotNulls,0);
        extIncDetailPriceIn.putNotNulls(propNotNulls,0);
        extIncDetailVATIn.putNotNulls(propNotNulls,0);
        receiptSaleDocument.putNotNulls(propNotNulls,0);
        clearingSaleCustomer.putNotNulls(propNotNulls,0);
        returnSupplier.putNotNulls(propNotNulls,0);

//        LDP extIncDetailSumVATIn, extIncDetailSumPay;
//        LDP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
//        LDP extIncDetailPriceOut;

        Map<DataProperty,Integer> propQuantity = new HashMap<DataProperty, Integer>();

//        PropQuantity.put((DataProperty)extIncQuantity.Property,10);
        propQuantity.put((DataProperty)intraQuantity.property, modifier *propModifier*2);
        propQuantity.put((DataProperty)receiptQuantity.property, modifier *propModifier*30);
        propQuantity.put((DataProperty)receiptSumPriceOut.property, modifier *propModifier*30);
        propQuantity.put((DataProperty)clearingSaleQuantity.property, modifier *propModifier*8);
        propQuantity.put((DataProperty)invQuantity.property, modifier *propModifier);
        propQuantity.put((DataProperty)exchangeQuantity.property, modifier *propModifier);
        propQuantity.put((DataProperty)returnQuantity.property, modifier *propModifier);
        propQuantity.put((DataProperty)isRevalued.property, modifier *propModifier);

        propQuantity.putAll(autoQuantity(0, fixedPriceIn, fixedVATIn, fixedAdd, fixedVATOut, fixedLocTax,
            revalBalanceQuantity,revalPriceIn,revalVATIn,revalAddBefore,revalVATOutBefore,revalLocTaxBefore,
                revalAddAfter,revalVATOutAfter,revalLocTaxAfter));

        autoFillDB(classQuantity, propQuantity,propNotNulls);
    }
  */
}
