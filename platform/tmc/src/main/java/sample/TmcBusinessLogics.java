package sample;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.group.AbstractGroup;

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

        article = addConcreteClass("article", "Товар", baseClass.named);
        articleGroup = addConcreteClass("articleGroup", "Группа товаров", baseClass.named);

        store = addConcreteClass("store", "Склад", baseClass.named);

        supplier = addConcreteClass("supplier", "Поставщик", baseClass.named);
        customer = addConcreteClass("customer", "Покупатель", baseClass.named);

        document = addAbstractClass("document", "Документ", baseClass.named);
        primaryDocument = addAbstractClass("primaryDocument", "Первичный документ", document);
        secondaryDocument = addAbstractClass("secondaryDocument", "Непервичный документ", document);
        quantityDocument = addAbstractClass("quantityDocument", "Товарный документ", document);
        incomeDocument = addAbstractClass("incomeDocument", "Приходный документ", quantityDocument);
        outcomeDocument = addAbstractClass("outcomeDocument", "Расходный документ", quantityDocument);
        fixedDocument = addAbstractClass("fixedDocument", "Зафиксированный документ", document);
        accountDocument = addAbstractClass("accountDocument", "Бухгалтерский документ", document);

        extIncomeDocument = addConcreteClass("extIncomeDocument", "Внешний приход", incomeDocument, primaryDocument);
        extIncomeDetail = addConcreteClass("extIncomeDetail", "Внешний приход (строки)", baseClass);

        intraDocument = addConcreteClass("intraDocument", "Внутреннее перемещение", incomeDocument, outcomeDocument, primaryDocument, fixedDocument);
        extOutcomeDocument = addAbstractClass("extOutcomeDocument", "Внешний расход", outcomeDocument, secondaryDocument, accountDocument);
        exchangeDocument = addConcreteClass("exchangeDocument", "Пересорт", incomeDocument, outcomeDocument, secondaryDocument, fixedDocument);

        revalDocument = addConcreteClass("revalDocument", "Переоценка", primaryDocument);

        saleDocument = addAbstractClass("saleDocument", "Реализация", extOutcomeDocument);
        cashSaleDocument = addConcreteClass("cashSaleDocument", "Реализация по кассе", saleDocument);
        clearingSaleDocument = addConcreteClass("clearingSaleDocument", "Реализация по б/н расчету", saleDocument, fixedDocument);

        invDocument = addConcreteClass("invDocument", "Инвентаризация", extOutcomeDocument, fixedDocument);

        returnDocument = addConcreteClass("returnDocument", "Возврат поставщику", extOutcomeDocument, fixedDocument);

        receipt = addConcreteClass("receipt", "Чек", fixedDocument);
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
    LP equals22;

    private void initAbstractProperties() {

        equals22 = addJProp("И", and1, equals2,1,2,equals2,3,4);
        percent = addSFProp("((prm1*prm2)/100)", DoubleClass.instance, 2);
        revPercent = addSFProp("((prm1*prm2)/(100+prm2))", DoubleClass.instance, 2);
        addPercent = addSFProp("((prm1*(100+prm2))/100)", DoubleClass.instance, 2);
        round = addSFProp("round(CAST(prm1 as numeric),0)", DoubleClass.instance, 1);
        roundm1 = addSFProp("round(CAST(prm1 as numeric),-1)", DoubleClass.instance, 1);
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

        artGroup = addDProp("articlegroup", "Гр. тов.", articleGroup, article);
        artGroupName = addJProp(artgrGroup, "Имя гр. тов.", name, artGroup, 1);

        artBarCode = addDProp(baseGroup, "barCode", "Штрих-код", NumericClass.get(13, 0), article);
        artWeight = addDProp(baseGroup, "weight", "Вес (кг.)", NumericClass.get(6, 3), article);
        artPackCount = addDProp(baseGroup, "count", "Кол-во в уп.", IntegerClass.instance, article);

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

        extIncSupplier = addDProp("supplier", "Поставщик", supplier, extIncomeDocument);
        extIncSupplierName = addJProp(supplierGroup, "extIncSupplierName", "Имя поставщика", name, extIncSupplier, 1);

        extIncDetailDocument = addDProp("extIncDetailDocument", "Документ", extIncomeDocument, extIncomeDetail);

        extIncDetailArticle = addDProp("article", "Товар", article, extIncomeDetail);
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

        docDate = addCUProp("docDate", true, "Дата", secDocDate, primDocDate);
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

        docStore = addCUProp("docStore", true, "Склад", extIncStore, intraOutStore, extOutStore, exchStore, revalStore);

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

        extIncQuantity = addSGProp(quantGroup, "extIncQuantity" , true, "Кол-во прих.", extIncDetailQuantity, extIncDetailDocument, 1, extIncDetailArticle, 1);

        intraQuantity = addDProp(quantGroup, "innerCount","Кол-во внутр.", DoubleClass.instance, intraDocument, article);

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

        incStoreQuantity = addSGProp("incStoreQuantity", true, "Прих. на скл.", incQuantity, incQStore, 1, 2);
        outStoreQuantity = addSGProp("outStoreQuantity", true, "Расх. со скл.", outQuantity, outQStore, 1, 2);

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
        maxChangesParamsDate = maxChangesProps[0]; maxChangesParamsDoc = maxChangesProps[1];
        addPersistent(maxChangesParamsDate); addPersistent(maxChangesParamsDoc);

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
        addPersistent(extIncLastDetail);

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
        extIncDocumentSumVATOut = addSGProp(outSumsGroup, "extIncDocumentSumVATOut", "Сумма НДС розн. (всего)", extIncDetailSumVATOut, extIncDetailDocument, 1);

        extIncDetailSumWAdd = addDUProp("Сумма с торг. надб.", extIncDetailSumWVAT, extIncDetailSumVATOut);
    }

    // Торговая надбавка
    LP extIncDetailSumAdd;
    LP extIncDocumentSumAdd;

    private void initSumAddProperties() {

        extIncDetailSumAdd = addUProp(outSumsGroup, "extIncDetailSumAdd", "Сумма торг. надб.", Union.SUM, 1, extIncDetailSumWAdd, 1, -1, extIncDetailSumInc, 1);
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
        CustomClass articleFood = addConcreteClass("articleFood", "Продтовары", article);
        addDProp(baseGroup, "expTime", "Срок годности", StringClass.get(10), articleFood);

        CustomClass articleAlcohol = addConcreteClass("articleAlcohol", "Алкоголь", articleFood);
        addDProp(baseGroup, "alchohol", "Крепость", IntegerClass.instance, articleAlcohol);

        CustomClass articleVodka = addConcreteClass("articleVodka", "Водка", articleAlcohol);
        addDProp(baseGroup, "vodka","Прейск.", LogicalClass.instance, articleVodka);

        CustomClass articleBeer = addConcreteClass("articleBeer", "Пиво", articleAlcohol);
        addDProp(baseGroup, "beerType", "Тип", StringClass.get(10), articleBeer);
        addDProp(baseGroup, "beerPack", "Упак.", StringClass.get(10), articleBeer);

        CustomClass wineTaste = addConcreteClass("wineTaste", "Вкус вина", baseClass.named);
        CustomClass articleWine = addConcreteClass("articleWine", "Вино", articleAlcohol);
        addJProp(baseGroup, "Вкус", name, addDProp("vineCode", "Код вкуса", wineTaste, articleWine), 1);

        CustomClass articleMilkGroup = addConcreteClass("articleMilkGroup", "Молочные продукты", articleFood);
        addDProp(baseGroup, "milkProd", "Жирн.", DoubleClass.instance, articleMilkGroup);

        CustomClass articleMilk = addConcreteClass("articleMilk", "Молоко", articleMilkGroup);
        addDProp(baseGroup, "milkPack", "Упак.", StringClass.get(10),  articleMilk);

        CustomClass articleCheese = addConcreteClass("articleCheese", "Сыр", articleMilkGroup);
        addDProp(baseGroup, "cheeseWeight", "Вес.", LogicalClass.instance, articleCheese);

        CustomClass articleCurd = addConcreteClass("articleCurd", "Творог", articleMilkGroup);

        CustomClass articleBreadGroup = addConcreteClass("articleBreadGroup", "Хлебобулочные изделия", articleFood);
        addDProp(baseGroup, "bunWeight", "Вес", IntegerClass.instance, articleBreadGroup);

        CustomClass articleBread = addConcreteClass("articleBread", "Хлеб", articleBreadGroup);
        addDProp(baseGroup, "breadWight", "Вес", IntegerClass.instance, articleBread);

        CustomClass articleCookies = addConcreteClass("articleCookies", "Печенье", articleBreadGroup);

        CustomClass articleJuice = addConcreteClass("articleJuice", "Соки", articleFood);
        addDProp(baseGroup, "juiceTaste", "Вкус", StringClass.get(10), articleJuice);
        addDProp(baseGroup, "juiceSize", "Литраж", IntegerClass.instance, articleJuice);

        CustomClass articleClothes = addConcreteClass("articleClothes", "Одежда", article);
        addDProp(baseGroup, "wearModel", "Модель", StringClass.get(10), articleClothes);

        CustomClass shirtSize = addConcreteClass("shirtSize", "Размер майки", baseClass.named);
        CustomClass articleTShirt = addConcreteClass("articleTShirt", "Майки", articleClothes);
        addJProp(baseGroup, "Размер", name, addDProp("shirtSize", "Код размера", shirtSize, articleTShirt), 1);

        CustomClass articleJeans = addConcreteClass("articleJeans", "Джинсы", articleClothes);
        addDProp(baseGroup, "jeansWidth", "Ширина", IntegerClass.instance, articleJeans);
        addDProp(baseGroup, "jeansLength", "Длина", IntegerClass.instance, articleJeans);

        CustomClass articleShoes = addConcreteClass("articleShoes", "Обувь", article);
        addDProp(baseGroup, "shoesColor", "Цвет", StringClass.get(10), articleShoes);
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
    FormEntity extIncDetailForm, extIncForm, extIncPrintForm;
    FormEntity intraForm;
    FormEntity extOutForm, cashSaleForm, receiptForm, clearingSaleForm, invForm, invStoreForm, returnForm;
    FormEntity exchangeForm, exchangeMForm;
    FormEntity revalueForm, revalueStoreForm;
    NavigatorElement aggregateData;
    NavigatorElement aggrStoreData;
    FormEntity storeArticleForm, storeArticlePrimDocForm, storeArticleDocForm;
    NavigatorElement aggrArticleData;
    FormEntity articleStoreForm, articleMStoreForm;
    NavigatorElement aggrSupplierData;
    FormEntity supplierStoreArticleForm;
    NavigatorElement analyticsData;
    NavigatorElement dateIntervalForms;
    FormEntity mainAccountForm, salesArticleStoreForm;

    protected void initNavigators() throws JRException, FileNotFoundException {

        primaryData = new NavigatorElement(baseElement, 100, "Первичные данные");
            extIncDetailForm = new ExtIncDetailFormEntity(primaryData, 110, "Внешний приход");
                extIncForm = new ExtIncFormEntity(extIncDetailForm, 115, "Внешний приход по товарам");
                extIncPrintForm = new ExtIncPrintFormEntity(extIncDetailForm, 117, "Реестр цен");
            intraForm = new IntraFormEntity(primaryData, 120, "Внутреннее перемещение");
            extOutForm = new ExtOutFormEntity(primaryData, 130, "Внешний расход");
                cashSaleForm = new CashSaleFormEntity(extOutForm, 131, "Реализация по кассе");
                    receiptForm = new ReceiptFormEntity(cashSaleForm, 1310, "Реализация по кассе (чеки)");
                clearingSaleForm = new ClearingSaleFormEntity(extOutForm, 132, "Реализация по б/н расчету");
                invForm = new InvFormEntity(extOutForm, 134, "Инвентаризация", false);
                    invStoreForm = new InvFormEntity(invForm, 1341, "Инвентаризация по складам", true);
                returnForm = new ReturnFormEntity(extOutForm, 136, "Возврат поставщику");
            exchangeForm = new ExchangeFormEntity(primaryData, 140, "Пересорт");
                exchangeMForm = new ExchangeMFormEntity(exchangeForm, 142, "Сводный пересорт");
            revalueForm = new RevalueFormEntity(primaryData, 150, "Переоценка", false);
                revalueStoreForm = new RevalueFormEntity(revalueForm, 155, "Переоценка по складам", true);

        aggregateData = new NavigatorElement(baseElement, 200, "Сводная информация");
            aggrStoreData = new NavigatorElement(aggregateData, 210, "Склады");
                storeArticleForm = new StoreArticleFormEntity(aggrStoreData, 211, "Товары по складам");
                    storeArticlePrimDocForm = new StoreArticlePrimDocFormEntity(storeArticleForm, 2111, "Товары по складам (изм. цен)");
                    storeArticleDocForm = new StoreArticleDocFormEntity(storeArticleForm, 2112, "Товары по складам (док.)");
            aggrArticleData = new NavigatorElement(aggregateData, 220, "Товары");
                articleStoreForm = new ArticleStoreFormEntity(aggrArticleData, 221, "Склады по товарам");
                articleMStoreForm = new ArticleMStoreFormEntity(aggrArticleData, 222, "Товары*Склады");
            aggrSupplierData = new NavigatorElement(aggregateData, 230, "Поставщики");
                supplierStoreArticleForm = new SupplierStoreArticleFormEntity(aggrSupplierData, 231, "Остатки по складам");

        analyticsData = new NavigatorElement(baseElement, 300, "Аналитические данные");
            dateIntervalForms = new NavigatorElement(analyticsData, 310, "За интервал дат");
                mainAccountForm = new MainAccountFormEntity(dateIntervalForms, 311, "Товарный отчет");
                salesArticleStoreForm = new SalesArticleStoreFormEntity(dateIntervalForms, 313, "Реализация товара по складам");

        extIncomeDocument.addRelevant(extIncDetailForm);
        intraDocument.addRelevant(intraForm);
        extOutcomeDocument.addRelevant(extOutForm);
        clearingSaleDocument.addRelevant(clearingSaleForm);
        invDocument.addRelevant(invForm);
        exchangeDocument.addRelevant(exchangeForm);
        revalDocument.addRelevant(revalueForm);

        extIncDetailForm.relevantElements.add(extIncPrintForm);
    }

    private class TmcFormEntity extends FormEntity {

        TmcFormEntity(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);
        }

        TmcFormEntity(NavigatorElement parent, int iID, String caption, boolean isPrintForm) {
            super(parent, iID, caption, isPrintForm);
        }

        void addArticleRegularFilterGroup(PropertyObjectEntity documentProp, Object documentValue, PropertyObjectEntity... extraProps) {

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
/*            filterGroup.addFilter(new RegularFilterEntity(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));*/
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(documentProp, Compare.NOT_EQUALS, documentValue),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));

            int functionKey = KeyEvent.VK_F9;

            for (PropertyObjectEntity extraProp : extraProps) {
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      new CompareFilterEntity(extraProp, Compare.NOT_EQUALS, 0),
                                      extraProp.property.caption,
                                      KeyStroke.getKeyStroke(functionKey--, 0)));
            }
            addRegularFilterGroup(filterGroup);
        }
    }

    private class ExtIncDocumentFormEntity extends TmcFormEntity {

        protected ObjectEntity objDoc;

        ExtIncDocumentFormEntity(NavigatorElement parent, int iID, String caption, boolean isPrintForm) {
            super(parent, iID, caption, isPrintForm);

            objDoc = addSingleGroupObject(extIncomeDocument, "Документ",
                    baseGroup, storeGroup, supplierGroup, quantGroup, incSumsGroup);
        }
    }

    private class ExtIncDetailFormEntity extends ExtIncDocumentFormEntity {

        ObjectEntity objDetail;

        public ExtIncDetailFormEntity(NavigatorElement parent, int ID, String caption) {
            this(parent, ID, caption, false);
        }
        public ExtIncDetailFormEntity(NavigatorElement parent, int ID, String caption, boolean isPrintForm) {
            super(parent, ID, caption, isPrintForm);

            objDetail = addSingleGroupObject(extIncomeDetail, "Строка",
                    artclGroup, quantGroup, incPrmsGroup, incSumsGroup, outPrmsGroup);

            PropertyObjectEntity detDocument = addPropertyObject(extIncDetailDocument, objDetail);
            addFixedFilter(new CompareFilterEntity(detDocument, Compare.EQUALS, objDoc));
        }
    }

    private class ExtIncFormEntity extends ExtIncDocumentFormEntity {

        public ExtIncFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption, false);

            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup);

            addPropertyDraw(objDoc, objArt,
                    balanceGroup, extIncQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyObject(extIncQuantity), 0);
        }
    }

    private class ExtIncPrintFormEntity extends ExtIncDetailFormEntity {

        public ExtIncPrintFormEntity(NavigatorElement parent, int ID, String caption) throws JRException, FileNotFoundException {
            super(parent, ID, caption, true);

            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            objDoc.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID));

            addPropertyDraw(objDoc, outSumsGroup);
            addPropertyDraw(objDetail, outSumsGroup);

            objDoc.setSID("objDoc");
            getPropertyDraw(name.property, objDoc.groupTo).setSID("docName");
        }
    }

    private class IntraFormEntity extends TmcFormEntity {

        public IntraFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(intraDocument, "Документ",
                    baseGroup, storeGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup);

            addPropertyDraw(objDoc, objArt,
                    balanceGroup, intraQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyObject(intraQuantity), 0,
                                         getPropertyObject(docOutBalanceQuantity),
                                         getPropertyObject(docIncBalanceQuantity));

            addHintsNoUpdate(maxChangesParamsDoc);
        }
    }

    private class ExtOutFormEntity extends TmcFormEntity {

        public ExtOutFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(extOutcomeDocument, "Документ",
                    baseGroup, storeGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup, true);

            addPropertyDraw(objDoc, objArt,
                    balanceGroup, extOutQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyObject(extOutQuantity), 0,
                                         getPropertyObject(docOutBalanceQuantity));
        }
    }

    private class CashSaleFormEntity extends TmcFormEntity {

        public CashSaleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(cashSaleDocument, "Документ",
                    baseGroup, storeGroup, outSumsGroup, accountGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup, true);

            addPropertyDraw(objDoc, objArt,
                    balanceGroup, cashSaleQuantity, outSumsGroup, accountGroup);

            addArticleRegularFilterGroup(getPropertyObject(cashSaleQuantity), 0,
                                         getPropertyObject(docOutBalanceQuantity));

//            addPropertyDraw(objDoc, objArt, Properties, quantity, notZeroQuantity);
        }
    }

    private class ReceiptFormEntity extends TmcFormEntity {

        public ReceiptFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(cashSaleDocument, "Документ",
                    baseGroup, storeGroup, outSumsGroup, accountGroup);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            objDoc.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID));

            ObjectEntity objReceipt = addSingleGroupObject(receipt, "Чек",
                    baseGroup, outSumsGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup, true);

            addPropertyDraw(objDoc, objArt,
                    balanceGroup);

            addPropertyDraw(objReceipt, objArt,
                    receiptQuantity, incPrmsGroup, outPrmsGroup, outSumsGroup);

            addPropertyDraw(objDoc, objArt,
                    accountGroup);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(receiptSaleDocument, objReceipt), Compare.EQUALS, objDoc));

            addArticleRegularFilterGroup(getPropertyObject(receiptQuantity), 0,
                                         getPropertyObject(docOutBalanceQuantity));
        }
    }

    private class ClearingSaleFormEntity extends TmcFormEntity {

        public ClearingSaleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(clearingSaleDocument, "Документ",
                    baseGroup, storeGroup, customerGroup, extOutDocumentSumPriceOut);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup, true);

            addPropertyDraw(objDoc, objArt,
                    balanceGroup, clearingSaleQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyObject(clearingSaleQuantity), 0,
                                         getPropertyObject(docOutBalanceQuantity));
        }
    }

    private class InvFormEntity extends TmcFormEntity {

        public InvFormEntity(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectEntity objStore = null;
            if (groupStore) {
                objStore = addSingleGroupObject(store, "Склад",
                        baseGroup, accountGroup);
                objStore.groupTo.initClassView = ClassViewType.PANEL;
                objStore.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID));
            }

            ObjectEntity objDoc = addSingleGroupObject(invDocument, "Документ",
                    baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup, true);

            addPropertyDraw(objDoc, objArt,
                    balanceGroup, invBalance, invQuantity, incPrmsGroup, outPrmsGroup, accountGroup);

            addArticleRegularFilterGroup(getPropertyObject(invQuantity), 0,
                                         getPropertyObject(docOutBalanceQuantity));

            if (groupStore)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(revalStore, objDoc), Compare.EQUALS, objStore));
            else
                addPropertyDraw(storeGroup, false, objDoc);
        }
    }

    private class ReturnFormEntity extends TmcFormEntity {

        public ReturnFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(returnDocument, "Документ",
                    baseGroup, storeGroup, supplierGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup, true);

            addPropertyDraw(objDoc, objArt,
                    balanceGroup, returnQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyObject(returnQuantity), 0,
                                         getPropertyObject(docOutBalanceQuantity));
        }
    }

    private class ExchangeFormEntity extends TmcFormEntity {

        public ExchangeFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(exchangeDocument, "Документ",
                    baseGroup, storeGroup);
            ObjectEntity objArtTo = addSingleGroupObject(article, "Товар (на)",
                    baseGroup);
            ObjectEntity objArtFrom = addSingleGroupObject(article, "Товар (c)",
                    baseGroup);

            addPropertyDraw(objDoc, objArtTo,
                    docOutBalanceQuantity, exchIncQuantity, exchOutQuantity, incPrmsGroup, outPrmsGroup);
            addPropertyDraw(docOutBalanceQuantity, objDoc, objArtFrom);
            addPropertyDraw(exchangeQuantity, objDoc, objArtFrom, objArtTo);
            addPropertyDraw(objDoc, objArtFrom, incPrmsGroup, outPrmsGroup);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
/*            filterGroup.addFilter(new RegularFilterEntity(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));*/
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(getPropertyObject(exchIncQuantity)),
                                  "Приход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(getPropertyObject(exchOutQuantity)),
                                  "Расход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(getPropertyObject(docOutBalanceQuantity, objArtTo), Compare.NOT_EQUALS, 0),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(getPropertyObject(docOutBalanceQuantity, objArtTo), Compare.LESS, 0),
                                  "Отр. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroup);

            filterGroup = new RegularFilterGroupEntity(genID());
/*            filterGroup.addFilter(new RegularFilterEntity(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.SHIFT_DOWN_MASK)));*/
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(getPropertyObject(exchangeQuantity)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(getPropertyObject(docOutBalanceQuantity, objArtFrom), Compare.NOT_EQUALS, 0),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(getPropertyObject(docOutBalanceQuantity, objArtFrom), Compare.GREATER, 0),
                                  "Пол. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(getPropertyObject(docOverPriceOut, objArtFrom), Compare.EQUALS, getPropertyObject(docOverPriceOut, objArtTo)),
                                  "Одинаковая розн. цена",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);

        }
    }

    private class ExchangeMFormEntity extends TmcFormEntity {

        public ExchangeMFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(exchangeDocument, "Документ",
                    baseGroup, storeGroup);

            GroupObjectEntity gobjArts = new GroupObjectEntity(genID());

            ObjectEntity objArtTo = new ObjectEntity(genID(), article, "Товар (на)");
            ObjectEntity objArtFrom = new ObjectEntity(genID(), article, "Товар (с)");

            gobjArts.add(objArtTo);
            gobjArts.add(objArtFrom);
            addGroup(gobjArts);

            addPropertyDraw(baseGroup, false, objArtTo);
            addPropertyDraw(baseGroup, false, objArtFrom);
            addPropertyDraw(exchangeQuantity, objDoc, objArtFrom, objArtTo);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(exchangeQuantity)));
        }
    }

    private class RevalueFormEntity extends TmcFormEntity {

        public RevalueFormEntity(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectEntity objStore = null;
            if (groupStore) {
                objStore = addSingleGroupObject(store, "Склад",
                        baseGroup, accountGroup);
                objStore.groupTo.initClassView = ClassViewType.PANEL;
                objStore.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID));
            }

            ObjectEntity objDoc = addSingleGroupObject(revalDocument, "Документ",
                    baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup);

            addPropertyDraw(objDoc, objArt,
                    revalOverBalanceQuantity, isRevalued, incPrmsGroupBefore, outPrmsGroupBefore, outPrmsGroupAfter);

            addArticleRegularFilterGroup(getPropertyObject(isRevalued), false,
                                         getPropertyObject(revalOverBalanceQuantity));

            if (groupStore)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(revalStore, objDoc), Compare.EQUALS, objStore));
            else
                addPropertyDraw(storeGroup, false, objDoc);
        }
    }

    private class StoreArticleFormEntity extends TmcFormEntity {

        ObjectEntity objStore, objArt;

        public StoreArticleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objStore = addSingleGroupObject(store, "Склад",
                    baseGroup, accountGroup);
            objStore.groupTo.initClassView = ClassViewType.PANEL;
            objStore.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID));

            objArt = addSingleGroupObject(article, "Товар",
                    baseGroup);

            addPropertyDraw(objStore, objArt,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);
        }
    }

    private class StoreArticlePrimDocFormEntity extends StoreArticleFormEntity {

        public StoreArticlePrimDocFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objPrimDoc = addSingleGroupObject(primaryDocument, "Документ",
                    baseGroup, paramsGroup);

            addPropertyDraw(objPrimDoc, objArt,
                    paramsGroup);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(isDocArtChangesParams)));
            addFixedFilter(new CompareFilterEntity(getPropertyObject(primDocStore), Compare.EQUALS, objStore));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyDraw(primDocDate)), false);
            richDesign = formView;
        }
    }

    private class StoreArticleDocFormEntity extends StoreArticleFormEntity {

        public StoreArticleDocFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(quantityDocument, "Товарный документ",
                    baseGroup, docDate, storeGroup, true, supplierGroup, true, customerGroup, true);

            addPropertyDraw(dltDocStoreQuantity, objDoc, objStore, objArt);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(isDocStoreArtInclude, objDoc, objStore, objArt)));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyDraw(docDate)), false);
            richDesign = formView;
        }
    }

    private class ArticleStoreFormEntity extends TmcFormEntity {

        ObjectEntity objStore, objArt;

        public ArticleStoreFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objArt = addSingleGroupObject(article, "Товар",
                    baseGroup);

            objStore = addSingleGroupObject(store, "Склад",
                    baseGroup);

            addPropertyDraw(objStore, objArt,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);

            addPropertyDraw(baseGroup, false, objArt.groupTo, false, objStore, objArt);
            addPropertyDraw(balanceGroup, false, objArt.groupTo, false, objStore, objArt);
            addPropertyDraw(incPrmsGroup, false, objArt.groupTo, false, objStore, objArt);
            addPropertyDraw(outPrmsGroup, false, objArt.groupTo, false, objStore, objArt);
        }

    }

    private class ArticleMStoreFormEntity extends TmcFormEntity {

        public ArticleMStoreFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            GroupObjectEntity gobjArtStore = new GroupObjectEntity(genID());

            ObjectEntity objArt = new ObjectEntity(genID(), article, "Товар");
            ObjectEntity objStore = new ObjectEntity(genID(), store, "Склад");

            gobjArtStore.add(objArt);
            gobjArtStore.add(objStore);
            addGroup(gobjArtStore);

            // добавить свойства по товару
            addPropertyDraw(baseGroup, false, objArt);
            // добавить свойства по складу
            addPropertyDraw(baseGroup, false, objStore);

            // добавить множественные свойства по товару и складу
            addPropertyDraw(objStore, objArt,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);
        }
    }

    private class SupplierStoreArticleFormEntity extends TmcFormEntity {

        SupplierStoreArticleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            // создать блок "Поставщик"
            ObjectEntity objSupplier = addSingleGroupObject(supplier, "Поставщик",
                    baseGroup);
            objSupplier.groupTo.initClassView = ClassViewType.PANEL;
            objSupplier.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID));

            // создать блок "Склад"
            ObjectEntity objStore = addSingleGroupObject(store, "Склад",
                    baseGroup);
            objStore.groupTo.initClassView = ClassViewType.PANEL;

            // создать блок "Товар"
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup, true);

            // добавить множественные свойства
            addPropertyDraw(objStore, objArt,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);

            // установить фильтр по умолчанию на поставщик товара = поставщик
            addFixedFilter(new CompareFilterEntity(addPropertyObject(storeSupplier, objStore, objArt), Compare.EQUALS, objSupplier));

            // добавить стандартные фильтры
            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
/*            filterGroup.addFilter(new RegularFilterEntity(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));*/
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(getPropertyObject(balanceStoreQuantity), Compare.GREATER, 0),
                                  "Есть на складе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(getPropertyObject(balanceStoreQuantity), Compare.LESS_EQUALS, 0),
                                  "Нет на складе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }



    private class DateIntervalFormEntity extends TmcFormEntity {

        GroupObjectEntity gobjInterval;
        ObjectEntity objDateFrom, objDateTo;

        public DateIntervalFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            gobjInterval = new GroupObjectEntity(genID());
            gobjInterval.initClassView = ClassViewType.PANEL;
            gobjInterval.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID));

            objDateFrom = new ObjectEntity(genID(), DateClass.instance, "С даты :");
            objDateTo = new ObjectEntity(genID(), DateClass.instance, "По дату :");

            gobjInterval.add(objDateFrom);
            gobjInterval.add(objDateTo);
            addGroup(gobjInterval);
        }
    }

    private class MainAccountFormEntity extends DateIntervalFormEntity {

        public MainAccountFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objStore = addSingleGroupObject(store, "Склад",
                    baseGroup);
            ObjectEntity objDoc = addSingleGroupObject(document, "Документ",
                    baseGroup, docDate);

//            addPropertyDraw(balanceDocStoreDateMSumAccount, objStore, objDateFrom);
//            addPropertyDraw(balanceDocStoreDateESumAccount, objStore, objDateTo);

            addPropertyDraw(dltDocStoreSumAccount, objDoc, objStore);

            addFixedFilter(new CompareFilterEntity(getPropertyObject(dltDocStoreSumAccount), Compare.NOT_EQUALS, 0));
            addFixedFilter(new CompareFilterEntity(getPropertyObject(docDate), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(getPropertyObject(docDate), Compare.LESS_EQUALS, objDateTo));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyDraw(docDate)), true);
            richDesign = formView;

        }
    }


    private class SalesArticleStoreFormEntity extends DateIntervalFormEntity {

        public SalesArticleStoreFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objArticle = addSingleGroupObject(article, "Товар",
                    baseGroup);
            ObjectEntity objStore = addSingleGroupObject(store, "Склад",
                    baseGroup);

            addPropertyDraw(saleArticleBetweenDateQuantity, objArticle, objDateFrom, objDateTo);

            addPropertyDraw(balanceStoreDateMQuantity, objStore, objArticle, objDateFrom);
            addPropertyDraw(incStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
            addPropertyDraw(outStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
            addPropertyDraw(balanceStoreDateEQuantity, objStore, objArticle, objDateTo);
            addPropertyDraw(saleStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
        }
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        SecurityPolicy securityPolicy = addPolicy("Базовая полтика", "Запрещает редактирование некоторых свойств.");

        securityPolicy.property.view.deny(extIncDocumentSumPay);
        securityPolicy.property.view.deny(incSumsGroup.getProperties());
        securityPolicy.property.change.deny(extIncDetailArticle);
        securityPolicy.property.change.deny(extIncDetailQuantity);

        securityPolicy.navigator.deny(analyticsData.getChildren(true));
        securityPolicy.navigator.deny(extIncPrintForm);

        securityPolicy.cls.edit.add.deny(document.getConcreteChildren());
        securityPolicy.cls.edit.remove.deny(baseGroup.getClasses());

        User user1 = addUser("user1", "");
        User user2 = addUser("user2", "");
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
