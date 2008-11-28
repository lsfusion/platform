package platformlocal;

import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.JRException;

import javax.swing.*;
import java.sql.SQLException;
import java.util.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;

public class TmcBusinessLogics extends BusinessLogics<TmcBusinessLogics>{

    public TmcBusinessLogics() {
        super();
    }

    public TmcBusinessLogics(int TestType) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(TestType);
    }

    Class article;
    Class articleGroup;

    Class store;
    Class supplier;
    Class customer;

    Class document;
    Class primaryDocument, secondaryDocument;
    Class fixedDocument, accountDocument;
    Class quantityDocument;
    Class incomeDocument;
    Class outcomeDocument;

    Class extIncomeDocument;
    Class extIncomeDetail;

    Class intraDocument;
    Class extOutcomeDocument;
    Class exchangeDocument;
    Class revalDocument;

    Class saleDocument;
    Class cashSaleDocument;
    Class clearingSaleDocument;
    Class invDocument;
    Class returnDocument;

    Class receipt;

    AbstractGroup artclGroup, artgrGroup, storeGroup, supplierGroup, customerGroup, quantGroup, balanceGroup;
    AbstractGroup incPrmsGroup, incPrmsGroupBefore, incPrmsGroupAfter, incSumsGroup, outSumsGroup, outPrmsGroup, outPrmsGroupBefore, outPrmsGroupAfter;
    AbstractGroup paramsGroup, accountGroup;

    void InitGroups() {

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

    void InitClasses() {

        article = addObjectClass("Товар", objectClass);
        articleGroup = addObjectClass("Группа товаров", objectClass);

        store = addObjectClass("Склад", objectClass);

        supplier = addObjectClass("Поставщик", objectClass);
        customer = addObjectClass("Покупатель", objectClass);

        document = addObjectClass("Документ", objectClass);
        primaryDocument = addObjectClass("Первичный документ", document);
        secondaryDocument = addObjectClass("Непервичный документ", document);
        quantityDocument = addObjectClass("Товарный документ", document);
        incomeDocument = addObjectClass("Приходный документ", quantityDocument);
        outcomeDocument = addObjectClass("Расходный документ", quantityDocument);
        fixedDocument = addObjectClass("Зафиксированный документ", document);
        accountDocument = addObjectClass("Бухгалтерский документ", document);

        extIncomeDocument = addObjectClass("Внешний приход", incomeDocument, primaryDocument);
        extIncomeDetail = addObjectClass("Внешний приход (строки)", objectClass);

        intraDocument = addObjectClass("Внутреннее перемещение", incomeDocument, outcomeDocument, primaryDocument, fixedDocument);
        extOutcomeDocument = addObjectClass("Внешний расход", outcomeDocument, secondaryDocument, accountDocument);
        exchangeDocument = addObjectClass("Пересорт", incomeDocument, outcomeDocument, secondaryDocument, fixedDocument);

        revalDocument = addObjectClass("Переоценка", primaryDocument);

        saleDocument = addObjectClass("Реализация", extOutcomeDocument);
        cashSaleDocument = addObjectClass("Реализация по кассе", saleDocument);
        clearingSaleDocument = addObjectClass("Реализация по б/н расчету", saleDocument, fixedDocument);

        invDocument = addObjectClass("Инвентаризация", extOutcomeDocument, fixedDocument);

        returnDocument = addObjectClass("Возврат поставщику", extOutcomeDocument, fixedDocument);

        receipt = addObjectClass("Чек", fixedDocument);
    }

    void InitProperties() {

        InitAbstractProperties();
        InitClassProperties();
        InitPrimaryProperties();
        InitAggregateProperties();
    }

    // ======================================================================================================= //
    // ==================================== Инициализация абстратных свойств ================================= //
    // ======================================================================================================= //

    LOFP object1;
    LSFP equals2, equals22;
    LSFP groeq2, greater2;
    LSFP notZero;
    LSFP percent, revPercent, addPercent;
    LSFP round, roundm1;
    LMFP multiplyBit2, multiplyDouble2, multiplyDate2;

    private void InitAbstractProperties() {

        equals2 = addWSFProp("((prm1)=(prm2))",2);
        object1 = addOFProp(1);
        equals22 = addWSFProp("((prm1)=(prm2)) AND ((prm3)=(prm4))",4);
        groeq2 = addWSFProp("((prm1)>=(prm2))",2);
        greater2 = addWSFProp("((prm1)>(prm2))",2);
        notZero = addWSFProp("((prm1)<>0)",1);
        percent = addSFProp("((prm1*prm2)/100)", Class.doubleClass, 2);
        revPercent = addSFProp("((prm1*prm2)/(100+prm2))", Class.doubleClass, 2);
        addPercent = addSFProp("((prm1*(100+prm2))/100)", Class.doubleClass, 2);
        round = addSFProp("round(CAST(prm1 as numeric),0)", Class.doubleClass, 1);
        roundm1 = addSFProp("round(CAST(prm1 as numeric),-1)", Class.doubleClass, 1);
        multiplyBit2 = addMFProp(Class.bit,2);
        multiplyDouble2 = addMFProp(Class.doubleClass,2);
        multiplyDate2 = addMFProp(Class.date,2);
   }

    // ======================================================================================================= //
    // ==================================== Инициализация классовых свойств ================================== //
    // ======================================================================================================= //

    LP dateDocument;
    LP datePrimDocument;
    LP storePrimDoc;
    LP bitExtInc;
    LP bitDocStore, doubleDocStore;
    LP doubleDocArticle;
    LP doubleDocStoreArticle;
    LP doubleIncDocArticle;
    LP doubleOutDocArticle;
    LP bitPrimDocArticle, doublePrimDocArticle;

    private void InitClassProperties() {

        dateDocument = addCProp("пустая дата", null, Class.date, document);
        datePrimDocument = addCProp("пустая дата", null, Class.date, primaryDocument);
        storePrimDoc = addCProp("пустой склад", null, store, primaryDocument);
        bitExtInc = addCProp("пустой бит", true, Class.bit, extIncomeDetail);
        bitDocStore = addCProp("пустой бит", null, Class.bit, document, store);
        doubleDocStore = addCProp("пустое число", null, Class.doubleClass, document, store);
        doubleDocArticle = addCProp("пустое кол-во", null, Class.doubleClass, document, article);
        doubleDocStoreArticle = addCProp("пустое число", null, Class.doubleClass, document, store, article);
        doubleIncDocArticle = addCProp("пустое кол-во", null, Class.doubleClass, incomeDocument, article);
        doubleOutDocArticle = addCProp("пустое кол-во", null, Class.doubleClass, outcomeDocument, article);
        bitPrimDocArticle = addCProp("пустой бит", null, Class.bit, primaryDocument, article);
        doublePrimDocArticle = addCProp("пустое число", null, Class.doubleClass, primaryDocument, article);

    }

    // ======================================================================================================= //
    // ==================================== Инициализация первичных свойств ================================== //
    // ======================================================================================================= //

    private void InitPrimaryProperties() {

        InitObjectProperties();
        InitArticleProperties();
        initCustomArticleLogics();
        InitExtIncProperties();
        InitClearingSaleProperties();
        InitReceiptProperties();
        InitReturnProperties();
        InitRevalProperties();
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Общие свойства -------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LDP name;

    private void InitObjectProperties() {

        name = addDProp(baseGroup, "Имя", Class.string, objectClass);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------------- Товар ----------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LDP artGroup;
    LJP artGroupName;

    private void InitArticleProperties() {

        artGroup = addDProp("Гр. тов.", articleGroup, article);
        artGroupName = addJProp(artgrGroup, "Имя гр. тов.", name, 1, artGroup, 1);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Внешний приход -------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LDP extIncSupplier;
    LJP extIncSupplierName;

    LDP extIncDetailDocument;
    LDP extIncDetailArticle;
    LJP extIncDetailArticleName;

    private void InitExtIncProperties() {

        extIncSupplier = addDProp("Поставщик", supplier, extIncomeDocument);
        extIncSupplierName = addJProp(supplierGroup, "extIncSupplierName", "Имя поставщика", name, 1, extIncSupplier, 1);

        extIncDetailDocument = addDProp("Документ", extIncomeDocument, extIncomeDetail);

        extIncDetailArticle = addDProp("Товар", article, extIncomeDetail);
        extIncDetailArticleName = addJProp(artclGroup, "extIncDetailArticleName", "Имя товара", name, 1, extIncDetailArticle, 1);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // -------------------------------------------------- Чеки ----------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LDP receiptSaleDocument;

    private void InitReceiptProperties() {

        receiptSaleDocument = addDProp("Документ продажи", cashSaleDocument, receipt);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ---------------------------------------- Реализация по б/н расчету ------------------------------------ //
    // ------------------------------------------------------------------------------------------------------- //

    LDP clearingSaleCustomer;
    LJP clearingSaleCustomerName;

    private void InitClearingSaleProperties() {

        clearingSaleCustomer = addDProp("Покупатель", customer, clearingSaleDocument);
        clearingSaleCustomerName = addJProp(customerGroup, "Имя покупателя", name, 1, clearingSaleCustomer, 1);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Возврат поставщику ---------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LDP returnSupplier;
    LJP returnSupplierName;

    private void InitReturnProperties() {

        returnSupplier = addDProp("Поставщик", supplier, returnDocument);
        returnSupplierName = addJProp(supplierGroup, "Имя поставщика", name, 1, returnSupplier, 1);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------------ Переоценка ------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void InitRevalProperties() {
    }

    // ======================================================================================================= //
    // ================================= Инициализация агрегированных свойств ================================ //
    // ======================================================================================================= //

    private void InitAggregateProperties() {

        InitDateProperties();
        InitStoreProperties();
        InitQuantityProperties();
        InitDocArtProperties();
        InitMaxProperties();
        InitParamsProperties();
        InitSumProperties();
        InitDocCurrentProperties();
    }

    // ------------------------------------------------------------------------------------------------------- //
    // --------------------------------------------------- Даты ---------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void InitDateProperties() {

        InitDatePrimaryProperties();
        InitDateOverrideProperties();
        InitDateDocProperties();
    }

    // ---------------------------------------- Первичные свойства ------------------------------------------- //

    LDP extIncDate;
    LDP intraDate;
    LDP extOutDate;
    LDP exchDate;
    LDP revalDate;

    private void InitDatePrimaryProperties() {

        extIncDate = addDProp(baseGroup, "extIncDate", "Дата", Class.date, extIncomeDocument);
        intraDate = addDProp(baseGroup, "Дата", Class.date, intraDocument);
        extOutDate = addDProp(baseGroup, "Дата", Class.date, extOutcomeDocument);
        exchDate = addDProp(baseGroup, "Дата", Class.date, exchangeDocument);
        revalDate = addDProp(baseGroup, "Дата", Class.date, revalDocument);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LUP primDocDate, secDocDate;
    LUP docDate;

    private void InitDateOverrideProperties() {

        primDocDate = addUProp(paramsGroup, "Дата", 2, 1, 1, datePrimDocument, 1, 1, extIncDate, 1, 1, intraDate, 1, 1, revalDate, 1);
        secDocDate = addUProp("Дата", 2, 1, 1, extOutDate, 1, 1, exchDate, 1);

        docDate = addUProp("Дата", 2, 1, 1, dateDocument, 1, 1, secDocDate, 1, 1, primDocDate, 1);
    }

    // ------------------------------------ Свойства по документам ------------------------------------------- //

    LJP groeqDocDate, greaterDocDate;

    private void InitDateDocProperties() {

        groeqDocDate = addJProp("Дата док.>=Дата", groeq2, 2, docDate, 1, 2);
        greaterDocDate = addJProp("Дата док.>Дата", greater2, 2, docDate, 1, 2);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // -------------------------------------------------- Склады --------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void InitStoreProperties() {

        InitStorePrimaryProperties();
        InitStoreOverrideProperties();
        InitDocStoreProperties();
    }

    // ---------------------------------------- Первичные свойства ------------------------------------------- //

    LDP extIncStore;
    LDP intraOutStore, intraIncStore;
    LJP intraStoreName;
    LDP extOutStore;
    LDP exchStore;
    LDP revalStore;

    LJP receiptStore;

    private void InitStorePrimaryProperties() {

        extIncStore = addDProp("Склад", store, extIncomeDocument);

        intraOutStore = addDProp("Склад отпр.", store, intraDocument);
        intraIncStore = addDProp("Склад назн.", store, intraDocument);
        intraStoreName = addJProp(storeGroup, "Имя склада (назн.)", name, 1, intraIncStore, 1);

        extOutStore = addDProp("Склад", store, extOutcomeDocument);
        exchStore = addDProp("Склад", store, exchangeDocument);
        revalStore = addDProp("Склад", store, revalDocument);

        receiptStore = addJProp(storeGroup, "Склад чека", extOutStore, 1, receiptSaleDocument, 1);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LUP incQStore;
    LUP incSStore;
    LUP outQStore;
    LUP outSStore;
    LUP primDocStore;
    LUP fixedStore;
    LUP docStore;

    LJP docStoreName;
    
    private void InitStoreOverrideProperties() {

        incQStore = addUProp("Склад прих.", 2, 1, 1, extIncStore, 1, 1, intraIncStore, 1, 1, exchStore, 1);
        outQStore = addUProp("Склад расх.", 2, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1);

        incSStore = addUProp("Склад прих. (сум)", 2, 1, 1, incQStore, 1, 1, revalStore, 1);
        outSStore = outQStore;

        primDocStore = addUProp(paramsGroup, "Склад (изм.)", 2, 1, 1, storePrimDoc, 1, 1, extIncStore, 1, 1, intraIncStore, 1, 1, revalStore, 1);
        fixedStore = addUProp("Склад (парам.)", 2, 1, 1, receiptStore, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1);

        docStore = addUProp("Склад", 2, 1, 1, extIncStore, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1, 1, revalStore, 1);

        docStoreName = addJProp(storeGroup, "docStoreName", "Имя склада", name, 1, docStore, 1);
    }

    // ------------------------------------ Свойства по документам ------------------------------------------- //

    LJP isDocIncQStore;
    LJP isDocOutQStore;
    LJP isDocRevalStore;
    LJP isDocIncSStore;
    LJP isDocOutSStore;
    LUP isDocStore;

    private void InitDocStoreProperties() {

        isDocIncQStore = addJProp("Склад=прих.кол-во", equals2, 2, incQStore, 1, 2);
        isDocOutQStore = addJProp("Склад=расх.кол-во", equals2, 2, outQStore, 1, 2);
        isDocRevalStore = addJProp("Склад=переоц.", equals2, 2, revalStore, 1, 2);

        isDocIncSStore = addJProp("Склад=прих.кол-во", equals2, 2, incSStore, 1, 2);
        isDocOutSStore = addJProp("Склад=расх.кол-во", equals2, 2, outSStore, 1, 2);

        isDocStore = addUProp("Склад=док.", 2, 2, 1, bitDocStore, 1, 2, 1, isDocIncQStore, 1, 2, 1, isDocOutQStore, 1, 2, 1, isDocRevalStore, 1, 2);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------------- Количество ------------------------------------------ //
    // ------------------------------------------------------------------------------------------------------- //

    private void InitQuantityProperties() {

        InitQuantityPrimaryProperties();
        InitQuantityOverrideProperties();
        InitQuantityDocStoreProperties();
        InitQuantityStoreProperties();
    }

    // ---------------------------------------- Первичные свойства ------------------------------------------- //

    LDP extIncDetailQuantity;
    LGP extIncDocumentQuantity;
    LGP extIncQuantity;

    LDP intraQuantity;

    LDP receiptQuantity;
    LGP cashSaleQuantity;
    LDP clearingSaleQuantity;
    LDP invQuantity;
    LDP invBalance;
    LDP returnQuantity;
    LUP extOutQuantity;

    LDP exchangeQuantity;
    LGP exchIncQuantity, exchOutQuantity;
    LUP exchDltQuantity;

    LDP revalBalanceQuantity;

    private void InitQuantityPrimaryProperties() {

        extIncDetailQuantity = addDProp(quantGroup, "extIncDetailQuantity", "Кол-во", Class.doubleClass, extIncomeDetail);
        extIncDocumentQuantity = addGProp(quantGroup, "extIncDocumentQuantity", "Кол-во (всего)", extIncDetailQuantity, true, extIncDetailDocument, 1);

        extIncQuantity = addGProp(quantGroup, "Кол-во прих.", extIncDetailQuantity, true, extIncDetailDocument, 1, extIncDetailArticle, 1);

        intraQuantity = addDProp(quantGroup, "Кол-во внутр.", Class.doubleClass, intraDocument, article);

        receiptQuantity = addDProp(quantGroup, "Кол-во в чеке", Class.doubleClass, receipt, article);
        cashSaleQuantity = addGProp(quantGroup, "Кол-во прод.", receiptQuantity, true, receiptSaleDocument, 1, 2);

        clearingSaleQuantity = addDProp(quantGroup, "Кол-во расх.", Class.doubleClass, clearingSaleDocument, article);

        invQuantity = addDProp(quantGroup, "Кол-во инв.", Class.doubleClass, invDocument, article);
        invBalance = addDProp(quantGroup, "Остаток инв.", Class.doubleClass, invDocument, article);
//        LP defInvQuantity = addUProp("Кол-во инв. (по умолч.)", 1, 2, 1, docOutBalanceQuantity, 1, 2, -1, invBalance, 1, 2);
//        setDefProp(invQuantity, defInvQuantity, true);

        returnQuantity = addDProp(quantGroup, "Кол-во возвр.", Class.doubleClass, returnDocument, article);

        extOutQuantity = addUProp("Кол-во расх.", 1, 2, 1, returnQuantity, 1, 2, 1, invQuantity, 1, 2, 1, clearingSaleQuantity, 1, 2, 1, cashSaleQuantity, 1, 2);

        exchangeQuantity = addDProp(quantGroup, "Кол-во перес.", Class.doubleClass, exchangeDocument, article, article);

        revalBalanceQuantity = addDProp(quantGroup, "Остаток", Class.doubleClass, revalDocument, article);

        exchIncQuantity = addGProp("Прих. перес.", exchangeQuantity, true, 1, 3);
        exchOutQuantity = addGProp("Расх. перес.", exchangeQuantity, true, 1, 2);
        exchDltQuantity = addUProp("Разн. перес.", 1, 2, 1, exchIncQuantity, 1, 2, -1, exchOutQuantity, 1, 2);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LUP incQuantity;
    LUP outQuantity;
    LUP quantity;

    private void InitQuantityOverrideProperties() {

        incQuantity = addUProp("Кол-во прих.", 1, 2, 1, doubleIncDocArticle, 1, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchIncQuantity, 1, 2);
        outQuantity = addUProp("Кол-во расх.", 1, 2, 1, doubleOutDocArticle, 1, 2, 1, extOutQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchOutQuantity, 1, 2);

        quantity = addUProp("Кол-во", 2, 2, 1, doubleDocArticle, 1, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2,
                                                                               1, extOutQuantity, 1, 2, 1, exchDltQuantity, 1, 2 );
    }

    // ---------------------------------- Свойства по документам/складам --------------------------------------- //

    LJP incDocStoreQuantity, outDocStoreQuantity;
    LUP dltDocStoreQuantity;

    private void InitQuantityDocStoreProperties() {

        incDocStoreQuantity = addJProp("Кол-во прих. по скл.", multiplyDouble2, 3, isDocIncQStore, 1, 2, incQuantity, 1, 3);
        outDocStoreQuantity = addJProp("Кол-во расх. по скл.", multiplyDouble2, 3, isDocOutQStore, 1, 2, outQuantity, 1, 3);
        dltDocStoreQuantity = addUProp("Кол-во (+-)", 1, 3, 1, doubleDocStoreArticle, 1, 2, 3, 1, incDocStoreQuantity, 1, 2, 3, -1, outDocStoreQuantity, 1, 2, 3);
    }

    // ----------------------------------------- Свойства по складам ------------------------------------------- //

    LGP incStoreQuantity, outStoreQuantity;
    LUP balanceStoreQuantity;

    private void InitQuantityStoreProperties() {

        incStoreQuantity = addGProp("Прих. на скл.", incQuantity, true, incQStore, 1, 2);
        outStoreQuantity = addGProp("Расх. со скл.", outQuantity, true, outQStore, 1, 2);

        balanceStoreQuantity = addUProp(balanceGroup, "Ост. на скл.", 1, 2, 1, incStoreQuantity, 1, 2, -1, outStoreQuantity, 1, 2);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // --------------------------------------------- Документы/товары ---------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LJP notZeroExtIncQuantity;

    LJP notZeroIncPrmsQuantity;
    LDP isRevalued;
    LUP isDocArtChangesParams;

    LJP notZeroQuantity;
    LUP isDocArtInclude;

    LJP isDocStoreArtInclude;

    private void InitDocArtProperties() {

        notZeroExtIncQuantity = addJProp("Есть во вн. прих.", notZero, 2, extIncQuantity, 1, 2);

        LP incPrmsQuantity = addUProp("Кол-во прих. (парам.)", 2, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2);
        notZeroIncPrmsQuantity = addJProp("Есть в перв. док.", notZero, 2, incPrmsQuantity, 1, 2);

        isRevalued = addDProp("Переоц.", Class.bit, revalDocument, article);

        isDocArtChangesParams = addUProp(paramsGroup, "Изм. парам.", 2, 2, 1, bitPrimDocArticle, 1, 2, 1, isRevalued, 1, 2, 1, notZeroIncPrmsQuantity, 1, 2);

        notZeroQuantity = addJProp("Есть в док.", notZero, 2, quantity, 1, 2);
        isDocArtInclude = addUProp("Есть в док.", 2, 2, 1, notZeroQuantity, 1, 2, 1, isRevalued, 1, 2);

        isDocStoreArtInclude = addJProp("В док. и скл.", multiplyBit2, 3, isDocStore, 1, 2, isDocArtInclude, 1, 3);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------ Последние документы ---------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LGP maxStoreExtIncDate;
    LGP maxStoreExtIncDoc;

    LGP maxChangesParamsDate;
    LGP maxChangesParamsDoc;

    private void InitMaxProperties() {

        // -------------------------- Последний приходный документ по товару ---------------------------- //

        LJP notZeroExtIncDate = addJProp("Дата посл. прих.", multiplyDate2, 2, notZeroExtIncQuantity, 1, 2, extIncDate, 1);

        maxStoreExtIncDate = addGProp(baseGroup, "Дата посл. прих.", notZeroExtIncDate, false, extIncStore, 1, 2);

        LJP extIncDocIsCor = addJProp("Прих. док. макс.", equals22, 3, extIncDate, 1, maxStoreExtIncDate, 2, 3, extIncStore, 1, 2);

        LJP extIncDocIsLast = addJProp("Посл.", multiplyBit2, 3, extIncDocIsCor, 1, 2, 3, notZeroExtIncQuantity, 1, 3);

        LJP extIncDocSelfLast = addJProp("Тов. док. макс.", object1, 3, 1, extIncDocIsLast, 1, 2, 3);
        maxStoreExtIncDoc = addGProp("Посл. док. прих.", extIncDocSelfLast, false, 2, 3);

        // -------------------------- Последний документ изм. цену ---------------------------- //

        LJP changesParamsDate = addJProp("Дата изм. пар.", multiplyDate2, 2, isDocArtChangesParams, 1, 2, primDocDate, 1);
        maxChangesParamsDate = addGProp(baseGroup, "Посл. дата изм. парам.", changesParamsDate, false, primDocStore, 1, 2);

        LJP primDocIsCor = addJProp("Док. макс.", equals22, 3, primDocDate, 1, maxChangesParamsDate, 2, 3, primDocStore, 1, 2);

        LJP primDocIsLast = addJProp("Посл.", multiplyBit2, 3, primDocIsCor, 1, 2, 3, isDocArtChangesParams, 1, 3);

        LJP primDocSelfLast = addJProp("Тов. док. макс.", object1, 3, 1, primDocIsLast, 1, 2, 3);
        maxChangesParamsDoc = addGProp("Посл. док. изм. парам.", primDocSelfLast, false, 2, 3);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Ценовые параметры ----------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void InitParamsProperties() {

        InitParamsPrimaryProperties();
        InitParamsOverrideProperties();
        InitParamsCurrentProperties();
    }

    // ---------------------------------------- Первичные свойства ------------------------------------------- //

    private void InitParamsPrimaryProperties() {

        InitParamsPrimaryExtIncProperties();
        InitParamsPrimaryFixedProperties();
        InitParamsPrimaryRevalProperties();
    }

    // Внешний приход

    LDP extIncDetailPriceIn, extIncDetailVATIn;

    LDP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
    LJP extIncDetailCalcPriceOut;
    LDP extIncDetailPriceOut;

    LGP extIncLastDetail;

    LJP extIncPriceIn, extIncVATIn;
    LJP extIncAdd, extIncVATOut, extIncLocTax;
    LJP extIncPriceOut;

    private void InitParamsPrimaryExtIncProperties() {

        extIncDetailPriceIn = addDProp(incPrmsGroup, "extIncDetailPriceIn", "Цена пост.", Class.doubleClass, extIncomeDetail);
        extIncDetailVATIn = addDProp(incPrmsGroup, "extIncDetailVATIn", "НДС пост.", Class.doubleClass, extIncomeDetail);

        // -------------------------- Выходные параметры ---------------------------- //

        extIncDetailAdd = addDProp(outPrmsGroup, "extIncDetailAdd", "Надбавка", Class.doubleClass, extIncomeDetail);
        extIncDetailVATOut = addDProp(outPrmsGroup, "extIncDetailVATOut", "НДС прод.", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailVATOut, extIncDetailVATIn, true);
        extIncDetailLocTax = addDProp(outPrmsGroup, "extIncDetailLocTax", "Местн. нал.", Class.doubleClass, extIncomeDetail);

        extIncDetailCalcPriceOut = addJProp("Цена розн. (расч.)", roundm1, 1,
                                   addJProp("Цена розн. (расч. - неокр.)", addPercent, 1,
                                   addJProp("Цена с НДС", addPercent, 1,
                                   addJProp("Цена с надбавкой", addPercent, 1,
                                           extIncDetailPriceIn, 1,
                                           extIncDetailAdd, 1), 1,
                                           extIncDetailVATOut, 1), 1,
                                           extIncDetailLocTax, 1), 1);

        extIncDetailPriceOut = addDProp(outPrmsGroup, "extIncDetailPriceOut", "Цена розн.", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailPriceOut, extIncDetailCalcPriceOut, true);

        // ------------------------- Последняя строка ------------------------------ //
        
        LJP propDetail = addJProp("Зн. строки", object1, 1, 1, bitExtInc, 1);
        extIncLastDetail = addGProp("Посл. строка", propDetail, false, extIncDetailDocument, 1, extIncDetailArticle, 1);

        extIncPriceIn = addJProp(incPrmsGroup, "Цена пост. (прих.)", extIncDetailPriceIn, 2, extIncLastDetail, 1, 2);
        extIncVATIn = addJProp(incPrmsGroup, "НДС пост. (прих.)", extIncDetailVATIn, 2, extIncLastDetail, 1, 2);
        extIncAdd = addJProp(outPrmsGroup, "Надбавка (прих.)", extIncDetailAdd, 2, extIncLastDetail, 1, 2);
        extIncVATOut = addJProp(outPrmsGroup, "НДС прод. (прих.)", extIncDetailVATOut, 2, extIncLastDetail, 1, 2);
        extIncLocTax = addJProp(outPrmsGroup, "Местн. нал. (прих.)", extIncDetailLocTax, 2, extIncLastDetail, 1, 2);
        extIncPriceOut = addJProp(outPrmsGroup, "Цена розн. (прих.)", extIncDetailPriceOut, 2, extIncLastDetail, 1, 2);
    }

    // Зафиксированные документы

    LDP fixedPriceIn, fixedVATIn;
    LDP fixedAdd, fixedVATOut, fixedLocTax;
    LDP fixedPriceOut;

    private void InitParamsPrimaryFixedProperties() {

        fixedPriceIn = addDProp("Цена пост.", Class.doubleClass, fixedDocument, article);
        fixedVATIn = addDProp("НДС пост.", Class.doubleClass, fixedDocument, article);
        fixedAdd = addDProp("Надбавка", Class.doubleClass, fixedDocument, article);
        fixedVATOut = addDProp("НДС прод.", Class.doubleClass, fixedDocument, article);
        fixedLocTax = addDProp("Местн. нал.", Class.doubleClass, fixedDocument, article);
        fixedPriceOut = addDProp("Цена розн.", Class.doubleClass, fixedDocument, article);
    }

    // Переоценка

    LJP revaluedBalanceQuantity;
    LDP revalPriceIn, revalVATIn;
    LDP revalAddBefore, revalVATOutBefore, revalLocTaxBefore;
    LDP revalPriceOutBefore;
    LDP revalAddAfter, revalVATOutAfter, revalLocTaxAfter;
    LDP revalPriceOutAfter;
    
    private void InitParamsPrimaryRevalProperties() {

        revaluedBalanceQuantity = addJProp("Остаток (переоц.)", multiplyDouble2, 2, revalBalanceQuantity, 1, 2, isRevalued, 1, 2);

        revalPriceIn = addDProp("Цена пост.", Class.doubleClass, revalDocument, article);
        revalVATIn = addDProp("НДС пост.", Class.doubleClass, revalDocument, article);
        revalAddBefore = addDProp("Надбавка (до)", Class.doubleClass, revalDocument, article);
        revalVATOutBefore = addDProp("НДС прод. (до)", Class.doubleClass, revalDocument, article);
        revalLocTaxBefore = addDProp("Местн. нал. (до)", Class.doubleClass, revalDocument, article);
        revalPriceOutBefore = addDProp("Цена розн. (до)", Class.doubleClass, revalDocument, article);
        revalAddAfter = addDProp(outPrmsGroupAfter, "Надбавка (после)", Class.doubleClass, revalDocument, article);
        revalVATOutAfter = addDProp(outPrmsGroupAfter, "НДС прод. (после)", Class.doubleClass, revalDocument, article);
        revalLocTaxAfter = addDProp(outPrmsGroupAfter, "Местн. нал. (после)", Class.doubleClass, revalDocument, article);
        revalPriceOutAfter = addDProp(outPrmsGroupAfter, "Цена розн. (после)", Class.doubleClass, revalDocument, article);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LUP primDocPriceIn;
    LUP primDocVATIn;
    LUP primDocAdd;
    LUP primDocVATOut;
    LUP primDocLocTax;
    LUP primDocPriceOut;

    private void InitParamsOverrideProperties() {

        primDocPriceIn = addUProp(paramsGroup, "Цена пост. (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedPriceIn, 1, 2, 1, extIncPriceIn, 1, 2, 1, revalPriceIn, 1, 2);
        primDocVATIn = addUProp(paramsGroup, "НДС пост. (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedVATIn, 1, 2, 1, extIncVATIn, 1, 2, 1, revalVATIn, 1, 2);
        primDocAdd = addUProp(paramsGroup, "Надбавка (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedAdd, 1, 2, 1, extIncAdd, 1, 2, 1, revalAddAfter, 1, 2);
        primDocVATOut = addUProp(paramsGroup, "НДС прод. (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedVATOut, 1, 2, 1, extIncVATOut, 1, 2, 1, revalVATOutAfter, 1, 2);
        primDocLocTax = addUProp(paramsGroup, "Местн. нал. (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedLocTax, 1, 2, 1, extIncLocTax, 1, 2, 1, revalLocTaxAfter, 1, 2);
        primDocPriceOut = addUProp(paramsGroup, "Цена розн. (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedPriceOut, 1, 2, 1, extIncPriceOut, 1, 2, 1, revalPriceOutAfter, 1, 2);
    }

    // -------------------------------------- Текущие свойства ---------------------------------------------- //

    LJP storeSupplier;

    LJP storePriceIn, storeVATIn;
    LJP storeAdd, storeVATOut, storeLocTax;
    LJP storePriceOut;

    private void InitParamsCurrentProperties() {

        storeSupplier = addJProp(supplierGroup, "Посл. пост.", extIncSupplier, 2, maxStoreExtIncDoc, 1, 2);

        storePriceIn = addJProp(incPrmsGroup, "Цена пост. (тек.)", primDocPriceIn, 2, maxChangesParamsDoc, 1, 2, 2);
        storeVATIn = addJProp(incPrmsGroup, "НДС пост. (тек.)", primDocVATIn, 2, maxChangesParamsDoc, 1, 2, 2);
        storeAdd = addJProp(outPrmsGroup, "Надбавка (тек.)", primDocAdd, 2, maxChangesParamsDoc, 1, 2, 2);
        storeVATOut = addJProp(outPrmsGroup, "НДС прод. (тек.)", primDocVATOut, 2, maxChangesParamsDoc, 1, 2, 2);
        storeLocTax = addJProp(outPrmsGroup, "Местн. нал. (тек.)", primDocLocTax, 2, maxChangesParamsDoc, 1, 2, 2);
        storePriceOut = addJProp(outPrmsGroup, "Цена розн. (тек.)", primDocPriceOut, 2, maxChangesParamsDoc, 1, 2, 2);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Суммовые свойства ----------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void InitSumProperties() {

        InitSumInProperties();
        InitSumOutProperties();
        InitSumAccountProperties();
    }

    // ---------------------------------------- Входные суммы --------------------------------------------- //

    LJP extIncDetailCalcSum;
    LJP extIncDetailCalcSumVATIn;
    LUP extIncDetailCalcSumPay;
    LDP extIncDetailSumVATIn, extIncDetailSumPay;
    LUP extIncDetailSumInc;
    LGP extIncDocumentSumInc, extIncDocumentSumVATIn, extIncDocumentSumPay;

    private void InitSumInProperties() {

        // -------------------------- Входные суммы ---------------------------- //

        extIncDetailCalcSum = addJProp("Сумма НДС (расч.)", round, 1,
                              addJProp("Сумма пост.", multiplyDouble2, 1, extIncDetailQuantity, 1, extIncDetailPriceIn, 1), 1);

        extIncDetailCalcSumVATIn = addJProp("Сумма НДС (расч.)", round, 1,
                                   addJProp("Сумма НДС (расч. - неокр.)", percent, 1, extIncDetailCalcSum, 1, extIncDetailVATIn, 1), 1);

        extIncDetailSumVATIn = addDProp(incSumsGroup, "extIncDetailSumVATIn", "Сумма НДС", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailSumVATIn, extIncDetailCalcSumVATIn, true);

        extIncDetailCalcSumPay = addUProp("Всего с НДС (расч.)", 1, 1, 1, extIncDetailCalcSum, 1, 1, extIncDetailSumVATIn, 1);

        extIncDetailSumPay = addDProp(incSumsGroup, "extIncDetailSumPay", "Всего с НДС", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailSumPay, extIncDetailCalcSumPay, true);

        extIncDetailSumInc = addUProp(incSumsGroup, "extIncDetailSumInc", "Сумма пост.", 1, 1, 1, extIncDetailSumPay, 1, -1, extIncDetailSumVATIn, 1);
        setPropOrder(extIncDetailSumInc.Property, extIncDetailSumVATIn.Property, true);

        extIncDocumentSumInc = addGProp(incSumsGroup, "extIncDocumentSumInc", "Сумма пост.", extIncDetailSumInc, true, extIncDetailDocument, 1);
        extIncDocumentSumVATIn = addGProp(incSumsGroup, "extIncDocumentSumVATIn", "Сумма НДС", extIncDetailSumVATIn, true, extIncDetailDocument, 1);
        extIncDocumentSumPay = addGProp(incSumsGroup, "extIncDocumentSumPay", "Всего с НДС", extIncDetailSumPay, true, extIncDetailDocument, 1);

    }

    // ---------------------------------------- Выходные суммы --------------------------------------------- //

    private void InitSumOutProperties() {

        InitSumPriceOutProperties();
        InitSumLocTaxProperties();
        InitSumVATOutProperties();
        InitSumAddProperties();
    }

    // Розничные суммы

    LJP extIncDetailSumPriceOut;
    LGP extIncDocumentSumPriceOut;

    LJP intraSumPriceOut;


    LDP receiptSumPriceOut;
    LGP receiptDocumentSumPriceOut;
    LGP cashSaleSumPriceOut;

    LUP extOutSumPriceOut;
    LGP extOutDocumentSumPriceOut;
    LJP exchIncSumPriceOut, exchOutSumPriceOut;
    LUP exchDltSumPriceOut;
    LJP revalSumPriceOutBefore, revalSumPriceOutAfter;
    LUP revalSumPriceOut;

    private void InitSumPriceOutProperties() {

        extIncDetailSumPriceOut = addJProp(outSumsGroup, "extIncDetailSumPriceOut", "Сумма розн.", round, 1,
                                  addJProp("Сумма розн. (неокр.)", multiplyDouble2, 1, extIncDetailQuantity, 1, extIncDetailPriceOut, 1), 1);
        extIncDocumentSumPriceOut = addGProp(outSumsGroup, "extIncDocumentSumPriceOut", "Сумма розн. (всего)", extIncDetailSumPriceOut, true, extIncDetailDocument, 1);

        intraSumPriceOut = addJProp("Сумма розн. (вн.)", multiplyDouble2, 2, intraQuantity, 1, 2, fixedPriceOut, 1, 2);

        receiptSumPriceOut = addDProp(outSumsGroup, "Сумма по чеку", Class.doubleClass, receipt, article);
        receiptDocumentSumPriceOut = addGProp(outSumsGroup, "Сумма чека", receiptSumPriceOut, true, 1);
        cashSaleSumPriceOut = addGProp(outSumsGroup, "Сумма прод.", receiptSumPriceOut, true, receiptSaleDocument, 1, 2);

        LP extOutParamsSumPriceOut = addJProp("Сумма розн. (расх. расч.)", multiplyDouble2, 2, extOutQuantity, 1, 2, fixedPriceOut, 1, 2);
        extOutSumPriceOut = addUProp("Сумма розн. (расх. расч.)", 1, 2, 1, extOutParamsSumPriceOut, 1, 2, 1, cashSaleSumPriceOut, 1, 2);
        extOutDocumentSumPriceOut = addGProp(outSumsGroup, "Сумма розн. (всего)", extOutSumPriceOut, true, 1);

        exchIncSumPriceOut = addJProp("Сумма розн. (перес. +)", multiplyDouble2, 2, exchIncQuantity, 1, 2, fixedPriceOut, 1, 2);
        exchOutSumPriceOut = addJProp("Сумма розн. (перес. -)", multiplyDouble2, 2, exchOutQuantity, 1, 2, fixedPriceOut, 1, 2);
        exchDltSumPriceOut = addUProp("Сумма розн. (перес.)", 1, 2, 1, exchIncSumPriceOut, 1, 2, -1, exchOutSumPriceOut, 1, 2);

        revalSumPriceOutBefore = addJProp("Сумма розн. (переоц. до)", multiplyDouble2, 2, revaluedBalanceQuantity, 1, 2, revalPriceOutBefore, 1, 2);
        revalSumPriceOutAfter = addJProp("Сумма розн. (переоц. после)", multiplyDouble2, 2, revaluedBalanceQuantity, 1, 2, revalPriceOutAfter, 1, 2);
        revalSumPriceOut = addUProp("Сумма розн. (переоц.)", 1, 2, 1, revalSumPriceOutAfter, 1, 2, -1, revalSumPriceOutBefore, 1, 2);
    }

    // Налог с продаж
    LJP extIncDetailSumLocTax;
    LGP extIncDocumentSumLocTax;
    LUP extIncDetailSumWVAT;

    private void InitSumLocTaxProperties() {

        extIncDetailSumLocTax = addJProp(outSumsGroup, "extIncDetailSumLocTax", "Сумма местн. нал.", round, 1,
                                addJProp("Сумма местн. нал. (неокр.)", revPercent, 1, extIncDetailSumPriceOut, 1, extIncDetailLocTax, 1), 1);
        setPropOrder(extIncDetailSumLocTax.Property, extIncDetailSumPriceOut.Property, true);
        extIncDocumentSumLocTax = addGProp(outSumsGroup, "extIncDocumentSumLocTax", "Сумма местн. нал. (всего)", extIncDetailSumLocTax, true, extIncDetailDocument, 1);

        extIncDetailSumWVAT = addUProp("Сумма с НДС (розн.)", 1, 1, 1, extIncDetailSumPriceOut, 1, -1, extIncDetailSumLocTax, 1);
    }

    // НДС розничный
    LJP extIncDetailSumVATOut;
    LGP extIncDocumentSumVATOut;
    LUP extIncDetailSumWAdd;

    private void InitSumVATOutProperties() {

        extIncDetailSumVATOut = addJProp(outSumsGroup, "extIncDetailSumVATOut", "Сумма НДС розн.", round, 1,
                                addJProp("Сумма НДС (розн. неокр.)", revPercent, 1, extIncDetailSumWVAT, 1, extIncDetailVATOut, 1), 1);
        setPropOrder(extIncDetailSumVATOut.Property, extIncDetailSumLocTax.Property, true);
        extIncDocumentSumVATOut = addGProp(outSumsGroup, "extIncDocumentSumVATOut", "Сумма НДС розн. (всего)", extIncDetailSumVATOut, true, extIncDetailDocument, 1);

        extIncDetailSumWAdd = addUProp("Сумма с торг. надб.", 1, 1, 1, extIncDetailSumWVAT, 1, -1, extIncDetailSumVATOut, 1);
    }

    // Торговая надбавка
    LUP extIncDetailSumAdd;
    LGP extIncDocumentSumAdd;

    private void InitSumAddProperties() {

        extIncDetailSumAdd = addUProp(outSumsGroup, "extIncDetailSumAdd", "Сумма торг. надб.", 1, 1, 1, extIncDetailSumWAdd, 1, -1, extIncDetailSumInc, 1);
        setPropOrder(extIncDetailSumAdd.Property, extIncDetailSumVATOut.Property, true);
        extIncDocumentSumAdd = addGProp(outSumsGroup, "extIncDocumentSumAdd", "Сумма торг. надб. (всего)", extIncDetailSumAdd, true, extIncDetailDocument, 1);
    }

    // ---------------------------------------- Учетные суммы ------------------------------------------------ //

    LDP accExcl;

    LJP extIncDetailSumAccount, intraSumAccount;
    LJP extOutSumAccountExcl;
    LUP extOutSumAccount;
    LGP extIncSumAccount;
    LJP exchIncSumAccount, exchOutSumAccount;
    LUP exchDltSumAccount;
    LUP revalSumAccount;

    LUP incSumAccount, outSumAccount;
    LGP incDocSumAccount, outDocSumAccount;
    LJP incDocStoreSumAccount, outDocStoreSumAccount;
    LUP dltDocStoreSumAccount;

    LJP incGroeqDateSumAccount, outGroeqDateSumAccount;
    LGP incStoreGroeqDateSumAccount, outStoreGroeqDateSumAccount;
    LUP dltStoreGroeqDateSumAccount;

    LJP incGreaterDateSumAccount, outGreaterDateSumAccount;
    LGP incStoreGreaterDateSumAccount, outStoreGreaterDateSumAccount;
    LUP dltStoreGreaterDateSumAccount;

    LGP incStoreSumAccount, outStoreSumAccount;
    LUP balanceDocStoreArticleSumAccount;
    LJP balanceStoreArticleSumAccount;
    LUP dltStoreArticleSumAccount;
    LGP balanceDocStoreSumAccount, balanceStoreSumAccount, dltStoreSumAccount;

    LUP balanceDocStoreDateMSumAccount;
    LUP balanceDocStoreDateESumAccount;

    private void InitSumAccountProperties() {

        accExcl = addDProp(accountGroup, "Искл.", Class.bit, accountDocument, article);

        extIncDetailSumAccount = extIncDetailSumPriceOut;
        extIncSumAccount = addGProp("Сумма учетн. (вх.)", extIncDetailSumAccount, true, extIncDetailDocument, 1, extIncDetailArticle, 1);

        intraSumAccount = intraSumPriceOut;
        extOutSumAccountExcl = addJProp("Сумма учетн. искл. (вых.)", multiplyDouble2, 2, extOutSumPriceOut, 1, 2, accExcl, 1, 2);
        extOutSumAccount = addUProp("Сумма учетн. (вых.)", 1, 2, 1, extOutSumPriceOut, 1, 2, -1, extOutSumAccountExcl, 1, 2);
        exchIncSumAccount = exchIncSumPriceOut;
        exchOutSumAccount = exchOutSumPriceOut;
        exchDltSumAccount = exchDltSumPriceOut;
        revalSumAccount = revalSumPriceOut;

        incSumAccount = addUProp("Сумма учетн. прих.", 1, 2, 1, extIncSumAccount, 1, 2, 1, intraSumAccount, 1, 2, 1, exchIncSumAccount, 1, 2, 1, revalSumAccount, 1, 2);
        outSumAccount = addUProp("Сумма учетн. расх.", 1, 2, 1, extOutSumAccount, 1, 2, 1, intraSumAccount, 1, 2, 1, exchOutSumAccount, 1, 2);

        incDocSumAccount = addGProp(accountGroup, "Сумма учетн. прих. на скл.", incSumAccount, true, 1);
        outDocSumAccount = addGProp(accountGroup, "Сумма учетн. расх. со скл.", outSumAccount, true, 1);

        incDocStoreSumAccount = addJProp("Сумма учетн. прих. по скл.", multiplyDouble2, 2, isDocIncSStore, 1, 2, incDocSumAccount, 1);
        outDocStoreSumAccount = addJProp("Сумма учетн. расх. по скл.", multiplyDouble2, 2, isDocOutSStore, 1, 2, outDocSumAccount, 1);
        dltDocStoreSumAccount = addUProp("Сумма учетн. товара (+-)", 1, 2, 1, doubleDocStore, 1, 2, 1, incDocStoreSumAccount, 1, 2, -1, outDocStoreSumAccount, 1, 2);


        incGroeqDateSumAccount = addJProp("Сумма учетн. прих. с даты", multiplyDouble2, 3, groeqDocDate, 1, 3, incSumAccount, 1, 2);
        outGroeqDateSumAccount = addJProp("Сумма учетн. расх. с даты", multiplyDouble2, 3, groeqDocDate, 1, 3, outSumAccount, 1, 2);

        incStoreGroeqDateSumAccount = addGProp("Сумма учетн. прих. на скл. с даты", incGroeqDateSumAccount, true, incSStore, 1, 3);
        outStoreGroeqDateSumAccount = addGProp("Сумма учетн. расх. со скл. с даты", outGroeqDateSumAccount, true, outSStore, 1, 3);
        dltStoreGroeqDateSumAccount = addUProp("Сумма учетн. на скл. с даты", 1, 2, 1, incStoreGroeqDateSumAccount, 1, 2, -1, outStoreGroeqDateSumAccount, 1, 2);

        incGreaterDateSumAccount = addJProp("Сумма учетн. прих. после даты", multiplyDouble2, 3, greaterDocDate, 1, 3, incSumAccount, 1, 2);
        outGreaterDateSumAccount = addJProp("Сумма учетн. расх. после даты", multiplyDouble2, 3, greaterDocDate, 1, 3, outSumAccount, 1, 2);

        incStoreGreaterDateSumAccount = addGProp("Сумма учетн. прих. на скл. после даты", incGreaterDateSumAccount, true, incSStore, 1, 3);
        outStoreGreaterDateSumAccount = addGProp("Сумма учетн. расх. со скл. после даты", outGreaterDateSumAccount, true, outSStore, 1, 3);
        dltStoreGreaterDateSumAccount = addUProp("Сумма учетн. на скл. после даты", 1, 2, 1, incStoreGreaterDateSumAccount, 1, 2, -1, outStoreGreaterDateSumAccount, 1, 2);

        incStoreSumAccount = addGProp("Сумма учетн. прих. на скл.", incSumAccount, true, incSStore, 1, 2);
        outStoreSumAccount = addGProp("Сумма учетн. расх. со скл.", outSumAccount, true, outSStore, 1, 2);
        balanceDocStoreArticleSumAccount = addUProp("Сумма товара на скл. (по док.)", 1, 2, 1, incStoreSumAccount, 1, 2, -1, outStoreSumAccount, 1, 2);

        balanceStoreArticleSumAccount = addJProp("Сумма товара на скл. (по ост.)", multiplyDouble2, 2, balanceStoreQuantity, 1, 2, storePriceOut, 1, 2);
        dltStoreArticleSumAccount = addUProp(accountGroup, "Отклонение суммы товара на скл.", 1, 2, 1, balanceDocStoreArticleSumAccount, 1, 2, -1, balanceStoreArticleSumAccount, 1, 2);

        balanceDocStoreSumAccount = addGProp(accountGroup, "Сумма на скл. (по док.)", balanceDocStoreArticleSumAccount, true, 1);
        balanceStoreSumAccount = addGProp(accountGroup, "Сумма на скл. (по ост.)", balanceStoreArticleSumAccount, true, 1);
        dltStoreSumAccount = addGProp(accountGroup, "Отклонение суммы на скл.", dltStoreArticleSumAccount, true, 1);

        balanceDocStoreDateMSumAccount = addUProp(accountGroup, "Сумма учетн. на начало", 1, 2, 1, addJProp("", balanceDocStoreSumAccount, 2, 1), 1, 2, -1, dltStoreGroeqDateSumAccount, 1, 2);
        balanceDocStoreDateESumAccount = addUProp(accountGroup, "Сумма учетн. на конец", 1, 2, 1, addJProp("", balanceDocStoreSumAccount, 2, 1), 1, 2, -1, dltStoreGreaterDateSumAccount, 1, 2);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------- Текущие параметры по документам --------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    private void InitDocCurrentProperties() {

        InitDocCurrentBalanceProperties();
        InitDocCurrentFixedProperties();
        InitDocCurrentRevalProperties();
    }

    // ------------------------------------------- Остатки -------------------------------------------------- //

    LJP docOutBalanceQuantity, docIncBalanceQuantity, docRevBalanceQuantity;

    private void InitDocCurrentBalanceProperties() {

        docOutBalanceQuantity = addJProp(balanceGroup, "Остаток (расх.)", balanceStoreQuantity, 2, outQStore, 1, 2);
        docIncBalanceQuantity = addJProp(balanceGroup, "Остаток (прих.)", balanceStoreQuantity, 2, incQStore, 1, 2);
        docRevBalanceQuantity = addJProp(balanceGroup, "Остаток (переоц.)", balanceStoreQuantity, 2, revalStore, 1, 2);
    }

    // ----------------------------------- Фискированные документы ------------------------------------------ //

    LJP docCurPriceIn, docCurVATIn;
    LJP docCurAdd, docCurVATOut, docCurLocTax;
    LJP docCurPriceOut;

    LUP docOverPriceIn;
    LUP docOverVATIn;
    LUP docOverAdd;
    LUP docOverVATOut;
    LUP docOverLocTax;
    LUP docOverPriceOut;

    private void InitDocCurrentFixedProperties() {

        docCurPriceIn = addJProp("Цена пост. (тек.)", storePriceIn, 2, fixedStore, 1, 2);
        docCurVATIn = addJProp("НДС пост. (тек.)", storeVATIn, 2, fixedStore, 1, 2);
        docCurAdd = addJProp("Надбавка (тек.)", storeAdd, 2, fixedStore, 1, 2);
        docCurVATOut = addJProp("НДС прод. (тек.)", storeVATOut, 2, fixedStore, 1, 2);
        docCurLocTax = addJProp("Местн. нал. (тек.)", storeLocTax, 2, fixedStore, 1, 2);
        docCurPriceOut = addJProp("Цена розн. (тек.)", storePriceOut, 2, fixedStore, 1, 2);

        docOverPriceIn = addUProp(incPrmsGroup, "Цена пост.", 2, 2, 1, docCurPriceIn, 1, 2, 1, fixedPriceIn, 1, 2);
        docOverVATIn = addUProp(incPrmsGroup, "НДС пост.", 2, 2, 1, docCurVATIn, 1, 2, 1, fixedVATIn, 1, 2);
        docOverAdd = addUProp(outPrmsGroup, "Надбавка", 2, 2, 1, docCurAdd, 1, 2, 1, fixedAdd, 1, 2);
        docOverVATOut = addUProp(outPrmsGroup, "НДС прод.", 2, 2, 1, docCurVATOut, 1, 2, 1, fixedVATOut, 1, 2);
        docOverLocTax = addUProp(outPrmsGroup, "Местн. нал.", 2, 2, 1, docCurLocTax, 1, 2, 1, fixedLocTax, 1, 2);
        docOverPriceOut = addUProp(outPrmsGroup, "Цена розн.", 2, 2, 1, docCurPriceOut, 1, 2, 1, fixedPriceOut, 1, 2);
    }

    // --------------------------------------- Переоценки -------------------------------------------- //

    LJP revalCurPriceIn, revalCurVATIn;
    LJP revalCurAdd, revalCurVATOut, revalCurLocTax;
    LJP revalCurPriceOut;

    LUP revalOverBalanceQuantity;
    LUP revalOverPriceIn;
    LUP revalOverVATIn;
    LUP revalOverAddBefore;
    LUP revalOverVATOutBefore;
    LUP revalOverLocTaxBefore;
    LUP revalOverPriceOutBefore;

    private void InitDocCurrentRevalProperties() {

        revalCurPriceIn = addJProp("Цена пост. (тек.)", storePriceIn, 2, revalStore, 1, 2);
        revalCurVATIn = addJProp("НДС пост. (тек.)", storeVATIn, 2, revalStore, 1, 2);
        revalCurAdd = addJProp("Надбавка (тек.)", storeAdd, 2, revalStore, 1, 2);
        revalCurVATOut = addJProp("НДС прод. (тек.)", storeVATOut, 2, revalStore, 1, 2);
        revalCurLocTax = addJProp("Местн. нал. (тек.)", storeLocTax, 2, revalStore, 1, 2);
        revalCurPriceOut = addJProp("Цена розн. (тек.)", storePriceOut, 2, revalStore, 1, 2);

        revalOverBalanceQuantity = addUProp(balanceGroup, "Остаток", 2, 2, 1, docRevBalanceQuantity, 1, 2, 1, revalBalanceQuantity, 1, 2);
        revalOverPriceIn = addUProp(incPrmsGroupBefore, "Цена пост.", 2, 2, 1, revalCurPriceIn, 1, 2, 1, revalPriceIn, 1, 2);
        revalOverVATIn = addUProp(incPrmsGroupBefore, "НДС пост.", 2, 2, 1, revalCurVATIn, 1, 2, 1, revalVATIn, 1, 2);
        revalOverAddBefore = addUProp(outPrmsGroupBefore, "Надбавка (до)", 2, 2, 1, revalCurAdd, 1, 2, 1, revalAddBefore, 1, 2);
        revalOverVATOutBefore = addUProp(outPrmsGroupBefore, "НДС прод. (до)", 2, 2, 1, revalCurVATOut, 1, 2, 1, revalVATOutBefore, 1, 2);
        revalOverLocTaxBefore = addUProp(outPrmsGroupBefore, "Местн. нал. (до)", 2, 2, 1, revalCurLocTax, 1, 2, 1, revalLocTaxBefore, 1, 2);
        revalOverPriceOutBefore = addUProp(outPrmsGroupBefore, "Цена розн. (до)", 2, 2, 1, revalCurPriceOut, 1, 2, 1, revalPriceOutBefore, 1, 2);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------- Конкретные классы товаров --------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    void initCustomArticleLogics() {

        // конкретные классы
        Class articleFood = addObjectClass("Продтовары", article);
        addDProp(baseGroup, "Срок годности", Class.string, articleFood);

        Class articleAlcohol = addObjectClass("Алкоголь", articleFood);
        addDProp(baseGroup, "Крепость", Class.integer, articleAlcohol);

        Class articleVodka = addObjectClass("Водка", articleAlcohol);
        addDProp(baseGroup, "Прейск.", Class.bit, articleVodka);

        Class articleBeer = addObjectClass("Пиво", articleAlcohol);
        addDProp(baseGroup, "Тип", Class.string, articleBeer);
        addDProp(baseGroup, "Упак.", Class.string, articleBeer);

        Class wineTaste = addObjectClass("Вкус вина", objectClass);
        Class articleWine = addObjectClass("Вино", articleAlcohol);
        addJProp(baseGroup, "Вкус", name, 1, addDProp("Код вкуса", wineTaste, articleWine), 1);

        Class articleMilkGroup = addObjectClass("Молочные продукты", articleFood);
        addDProp(baseGroup, "Жирн.", Class.doubleClass, articleMilkGroup);

        Class articleMilk = addObjectClass("Молоко", articleMilkGroup);
        addDProp(baseGroup, "Упак.", Class.string,  articleMilk);

        Class articleCheese = addObjectClass("Сыр", articleMilkGroup);
        addDProp(baseGroup, "Вес.", Class.bit, articleCheese);

        Class articleCurd = addObjectClass("Творог", articleMilkGroup);

        Class articleBreadGroup = addObjectClass("Хлебобулочные изделия", articleFood);
        addDProp(baseGroup, "Вес", Class.integer, articleBreadGroup);

        Class articleBread = addObjectClass("Хлеб", articleBreadGroup);
        addDProp(baseGroup, "Вес", Class.integer, articleBread);

        Class articleCookies = addObjectClass("Печенье", articleBreadGroup);

        Class articleJuice = addObjectClass("Соки", articleFood);
        addDProp(baseGroup, "Вкус", Class.string, articleJuice);
        addDProp(baseGroup, "Литраж", Class.integer, articleJuice);

        Class articleClothes = addObjectClass("Одежда", article);
        addDProp(baseGroup, "Модель", Class.string, articleClothes);

        Class shirtSize = addObjectClass("Размер майки", objectClass);
        Class articleTShirt = addObjectClass("Майки", articleClothes);
        addJProp(baseGroup, "Размер", name, 1, addDProp("Код размера", shirtSize, articleTShirt), 1);

        Class articleJeans = addObjectClass("Джинсы", articleClothes);
        addDProp(baseGroup, "Ширина", Class.integer, articleJeans);
        addDProp(baseGroup, "Длина", Class.integer, articleJeans);

        Class articleShooes = addObjectClass("Обувь", article);
        addDProp(baseGroup, "Цвет", Class.string, articleShooes);
    }

    void InitConstraints() {

//        Constraints.put(balanceStoreQuantity.Property,new PositiveConstraint());
    }

    void InitPersistents() {

        Persistents.add((AggregateProperty)docStore.Property);

        Persistents.add((AggregateProperty)extIncQuantity.Property);

        Persistents.add((AggregateProperty)incStoreQuantity.Property);
        Persistents.add((AggregateProperty)outStoreQuantity.Property);
        Persistents.add((AggregateProperty)maxChangesParamsDate.Property);
        Persistents.add((AggregateProperty)maxChangesParamsDoc.Property);

        Persistents.add((AggregateProperty)extIncLastDetail.Property);

    }

    void InitTables() {

        TableImplement Include;

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,article));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,store));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,articleGroup));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,article));
        Include.add(new DataPropertyInterface(0,document));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,article));
        Include.add(new DataPropertyInterface(0,store));
        TableFactory.IncludeIntoGraph(Include);

    }

    void InitIndexes() {
        List<Property> index;

        index = new ArrayList();
        index.add(extIncDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(intraDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(extOutDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(exchDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(revalDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(maxChangesParamsDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(docStore.Property);
        Indexes.add(index);
    }

    void InitNavigators() {

        createDefaultClassForms(objectClass, baseElement);

        NavigatorElement primaryData = new NavigatorElement(baseElement, 100, "Первичные данные");
            NavigatorForm extIncDetailForm = new ExtIncDetailNavigatorForm(primaryData, 110, "Внешний приход");
                NavigatorForm extIncForm = new ExtIncNavigatorForm(extIncDetailForm, 115, "Внешний приход по товарам");
                NavigatorForm extIncPrintForm = new ExtIncPrintNavigatorForm(extIncDetailForm, 117, "Реестр цен");
            NavigatorForm intraForm = new IntraNavigatorForm(primaryData, 120, "Внутреннее перемещение");
            NavigatorForm extOutForm = new ExtOutNavigatorForm(primaryData, 130, "Внешний расход");
                NavigatorForm cashSaleForm = new CashSaleNavigatorForm(extOutForm, 131, "Реализация по кассе");
                    NavigatorForm receiptForm = new ReceiptNavigatorForm(cashSaleForm, 1310, "Реализация по кассе (чеки)");
                NavigatorForm clearingSaleForm = new ClearingSaleNavigatorForm(extOutForm, 132, "Реализация по б/н расчету");
                NavigatorForm invForm = new InvNavigatorForm(extOutForm, 134, "Инвентаризация", false);
                    NavigatorForm invStoreForm = new InvNavigatorForm(invForm, 1341, "Инвентаризация по складам", true);
                NavigatorForm returnForm = new ReturnNavigatorForm(extOutForm, 136, "Возврат поставщику");
            NavigatorForm exchangeForm = new ExchangeNavigatorForm(primaryData, 140, "Пересорт");
                NavigatorForm exchangeMForm = new ExchangeMNavigatorForm(exchangeForm, 142, "Сводный пересорт");
            NavigatorForm revalueForm = new RevalueNavigatorForm(primaryData, 150, "Переоценка", false);
                NavigatorForm revalueStoreForm = new RevalueNavigatorForm(revalueForm, 155, "Переоценка по складам", true);

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

        NavigatorElement analyticsData = new NavigatorElement(baseElement, 300, "Аналитические данные");
            NavigatorElement dateIntervalForms = new NavigatorElement(analyticsData, 310, "За интервал дат");
                NavigatorForm mainAccountForm = new MainAccountNavigatorForm(dateIntervalForms, 311, "Товарный отчет");

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

        void addArticleRegularFilterGroup(PropertyObjectImplement documentProp, Object documentValue, PropertyObjectImplement... extraProps) {

            RegularFilterGroup filterGroup = new RegularFilterGroup(IDShift(1));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(documentProp, Filter.NOT_EQUALS, new UserValueLink(documentValue)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));

            int functionKey = KeyEvent.VK_F9;

            for (PropertyObjectImplement extraProp : extraProps) {
                filterGroup.addFilter(new RegularFilter(IDShift(1),
                                      new Filter(extraProp, Filter.NOT_EQUALS, new UserValueLink(0)),
                                      extraProp.Property.caption,
                                      KeyStroke.getKeyStroke(functionKey--, 0)));
            }
            addRegularFilterGroup(filterGroup);
        }
    }

    private class ExtIncDocumentNavigatorForm extends TmcNavigatorForm {

        protected ObjectImplement objDoc;

        ExtIncDocumentNavigatorForm(NavigatorElement parent, int iID, String caption, boolean isPrintForm) {
            super(parent, iID, caption, isPrintForm);

            objDoc = addSingleGroupObjectImplement(extIncomeDocument, "Документ", Properties,
                                                   baseGroup, storeGroup, supplierGroup, quantGroup, incSumsGroup);
        }
    }

    private class ExtIncDetailNavigatorForm extends ExtIncDocumentNavigatorForm {

        ObjectImplement objDetail;

        public ExtIncDetailNavigatorForm(NavigatorElement parent, int ID, String caption) {
            this(parent, ID, caption, false);
        }
        public ExtIncDetailNavigatorForm(NavigatorElement parent, int ID, String caption, boolean isPrintForm) {
            super(parent, ID, caption, isPrintForm);

            objDetail = addSingleGroupObjectImplement(extIncomeDetail, "Строка", Properties,
                                                                      artclGroup, quantGroup, incPrmsGroup, incSumsGroup, outPrmsGroup);

            PropertyObjectImplement detDocument = addPropertyObjectImplement(extIncDetailDocument, objDetail);
            addFixedFilter(new Filter(detDocument, Filter.EQUALS, new ObjectValueLink(objDoc)));
        }
    }

    private class ExtIncNavigatorForm extends ExtIncDocumentNavigatorForm {

        public ExtIncNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption, false);

            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArt, Properties,
                    balanceGroup, extIncQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyView(extIncQuantity.Property).View, 0);
        }
    }

    private class ExtIncPrintNavigatorForm extends ExtIncDetailNavigatorForm {

        public ExtIncPrintNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption, true);

            objDoc.GroupTo.gridClassView = false;
            objDoc.GroupTo.singleViewType = true;

            addPropertyView(objDoc, Properties, outSumsGroup);
            addPropertyView(objDetail, Properties, outSumsGroup);

            objDoc.sID = "objDoc";
            getPropertyView(name.Property, objDoc.GroupTo).sID = "docName";

            try {
                reportDesign = JRXmlLoader.load(new FileInputStream(new File("d:/java/application/platform/src/platformlocal/reports/extIncLog.jrxml")));
            } catch (JRException e) {
                reportDesign = new DefaultJasperDesign(this);
            } catch (FileNotFoundException e) {
                reportDesign = new DefaultJasperDesign(this);
            }

        }
    }

    private class IntraNavigatorForm extends TmcNavigatorForm {

        public IntraNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(intraDocument, "Документ", Properties,
                                                                        baseGroup, storeGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArt, Properties,
                    balanceGroup, intraQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyView(intraQuantity.Property).View, 0,
                                         getPropertyView(docOutBalanceQuantity.Property).View,
                                         getPropertyView(docIncBalanceQuantity.Property).View);

            addHintsNoUpdate(maxChangesParamsDoc.Property);
        }
    }

    private class ExtOutNavigatorForm extends TmcNavigatorForm {

        public ExtOutNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(intraDocument, "Документ", Properties,
                                                                        baseGroup, storeGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, Properties,
                    balanceGroup, extOutQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyView(extOutQuantity.Property).View, 0,
                                         getPropertyView(docOutBalanceQuantity.Property).View);
        }
    }

    private class CashSaleNavigatorForm extends TmcNavigatorForm {

        public CashSaleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(cashSaleDocument, "Документ", Properties,
                                                                        baseGroup, storeGroup, outSumsGroup, accountGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, Properties,
                        balanceGroup, cashSaleQuantity, outSumsGroup, accountGroup);

            addArticleRegularFilterGroup(getPropertyView(cashSaleQuantity.Property).View, 0,
                                         getPropertyView(docOutBalanceQuantity.Property).View);
        }
    }

    private class ReceiptNavigatorForm extends TmcNavigatorForm {

        public ReceiptNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(cashSaleDocument, "Документ", Properties,
                                                                        baseGroup, storeGroup, outSumsGroup, accountGroup);
            objDoc.GroupTo.gridClassView = false;
            objDoc.GroupTo.singleViewType = true;

            ObjectImplement objReceipt = addSingleGroupObjectImplement(receipt, "Чек", Properties,
                                                                        baseGroup, outSumsGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, Properties,
                        balanceGroup);

            addPropertyView(objReceipt, objArt, Properties,
                        receiptQuantity, incPrmsGroup, outPrmsGroup, outSumsGroup);

            addPropertyView(objDoc, objArt, Properties,
                        accountGroup);

            addFixedFilter(new Filter(addPropertyObjectImplement(receiptSaleDocument, objReceipt), Filter.EQUALS, new ObjectValueLink(objDoc)));

            addArticleRegularFilterGroup(getPropertyView(receiptQuantity.Property).View, 0,
                                         getPropertyView(docOutBalanceQuantity.Property).View);
        }
    }

    private class ClearingSaleNavigatorForm extends TmcNavigatorForm {

        public ClearingSaleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(clearingSaleDocument, "Документ", Properties,
                                                                        baseGroup, storeGroup, customerGroup, extOutDocumentSumPriceOut);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, Properties,
                        balanceGroup, clearingSaleQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyView(clearingSaleQuantity.Property).View, 0,
                                         getPropertyView(docOutBalanceQuantity.Property).View);
        }
    }

    private class InvNavigatorForm extends TmcNavigatorForm {

        public InvNavigatorForm(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectImplement objStore = null;
            if (groupStore) {
                objStore = addSingleGroupObjectImplement(store, "Склад", Properties,
                                                                            baseGroup, accountGroup);
                objStore.GroupTo.gridClassView = false;
                objStore.GroupTo.singleViewType = true;
            }

            ObjectImplement objDoc = addSingleGroupObjectImplement(invDocument, "Документ", Properties,
                                                                        baseGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, Properties,
                    balanceGroup, invBalance, invQuantity, incPrmsGroup, outPrmsGroup, accountGroup);

            addArticleRegularFilterGroup(getPropertyView(invQuantity.Property).View, 0,
                                         getPropertyView(docOutBalanceQuantity.Property).View);

            if (groupStore)
                addFixedFilter(new Filter(addPropertyObjectImplement(revalStore, objDoc), Filter.EQUALS, new ObjectValueLink(objStore)));
            else
                addPropertyView(Properties, storeGroup, false, objDoc);
        }
    }

    private class ReturnNavigatorForm extends TmcNavigatorForm {

        public ReturnNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(returnDocument, "Документ", Properties,
                                                                        baseGroup, storeGroup, supplierGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, Properties,
                    balanceGroup, returnQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyView(returnQuantity.Property).View, 0,
                                         getPropertyView(docOutBalanceQuantity.Property).View);
        }
    }

    private class ExchangeNavigatorForm extends TmcNavigatorForm {

        public ExchangeNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(exchangeDocument, "Документ", Properties,
                                                                        baseGroup, storeGroup);
            ObjectImplement objArtTo = addSingleGroupObjectImplement(article, "Товар (на)", Properties,
                                                                        baseGroup);
            ObjectImplement objArtFrom = addSingleGroupObjectImplement(article, "Товар (c)", Properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArtTo, Properties,
                    docOutBalanceQuantity, exchIncQuantity, exchOutQuantity, incPrmsGroup, outPrmsGroup);
            addPropertyView(docOutBalanceQuantity, objDoc, objArtFrom);
            addPropertyView(exchangeQuantity, objDoc, objArtFrom, objArtTo);
            addPropertyView(objDoc, objArtFrom, Properties, incPrmsGroup, outPrmsGroup);

            RegularFilterGroup filterGroup = new RegularFilterGroup(IDShift(1));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(exchIncQuantity.Property).View, Filter.NOT_EQUALS, new UserValueLink(0)),
                                  "Приход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(exchOutQuantity.Property).View, Filter.NOT_EQUALS, new UserValueLink(0)),
                                  "Расход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.Property, objArtTo.GroupTo).View, Filter.NOT_EQUALS, new UserValueLink(0)),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.Property, objArtTo.GroupTo).View, Filter.LESS, new UserValueLink(0)),
                                  "Отр. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroup);

            filterGroup = new RegularFilterGroup(IDShift(1));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(exchangeQuantity.Property).View, Filter.NOT_EQUALS, new UserValueLink(0)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.Property, objArtFrom.GroupTo).View, Filter.NOT_EQUALS, new UserValueLink(0)),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.Property, objArtFrom.GroupTo).View, Filter.GREATER, new UserValueLink(0)),
                                  "Пол. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOverPriceOut.Property, objArtFrom.GroupTo).View, Filter.EQUALS, new PropertyValueLink(getPropertyView(docOverPriceOut.Property, objArtTo.GroupTo).View)),
                                  "Одинаковая розн. цена",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);

        }
    }

    private class ExchangeMNavigatorForm extends TmcNavigatorForm {

        public ExchangeMNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(exchangeDocument, "Документ", Properties,
                                                                        baseGroup, storeGroup);

            GroupObjectImplement gobjArts = new GroupObjectImplement(IDShift(1));

            ObjectImplement objArtTo = new ObjectImplement(IDShift(1), article, "Товар (на)", gobjArts);
            ObjectImplement objArtFrom = new ObjectImplement(IDShift(1), article, "Товар (с)", gobjArts);

            addGroup(gobjArts);

            addPropertyView(Properties, baseGroup, false, objArtTo);
            addPropertyView(Properties, baseGroup, false, objArtFrom);
            addPropertyView(exchangeQuantity, objDoc, objArtFrom, objArtTo);

            addFixedFilter(new Filter(getPropertyView(exchangeQuantity.Property).View, Filter.NOT_EQUALS, new UserValueLink(0)));
        }
    }

    private class RevalueNavigatorForm extends TmcNavigatorForm {

        public RevalueNavigatorForm(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectImplement objStore = null;
            if (groupStore) {
                objStore = addSingleGroupObjectImplement(store, "Склад", Properties,
                                                                            baseGroup, accountGroup);
                objStore.GroupTo.gridClassView = false;
                objStore.GroupTo.singleViewType = true;
            }

            ObjectImplement objDoc = addSingleGroupObjectImplement(revalDocument, "Документ", Properties,
                                                                        baseGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArt, Properties,
                    revalOverBalanceQuantity, isRevalued, incPrmsGroupBefore, outPrmsGroupBefore, outPrmsGroupAfter);

            addArticleRegularFilterGroup(getPropertyView(isRevalued.Property).View, false,
                                         getPropertyView(revalOverBalanceQuantity.Property).View);

            if (groupStore)
                addFixedFilter(new Filter(addPropertyObjectImplement(revalStore, objDoc), Filter.EQUALS, new ObjectValueLink(objStore)));
            else
                addPropertyView(Properties, storeGroup, false, objDoc);
        }
    }

    private class StoreArticleNavigatorForm extends TmcNavigatorForm {

        ObjectImplement objStore, objArt;

        public StoreArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objStore = addSingleGroupObjectImplement(store, "Склад", Properties,
                                                                        baseGroup, accountGroup);
            objStore.GroupTo.gridClassView = false;
            objStore.GroupTo.singleViewType = true;

            objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup);

            addPropertyView(objStore, objArt, Properties,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);
        }
    }

    private class StoreArticlePrimDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticlePrimDocNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objPrimDoc = addSingleGroupObjectImplement(primaryDocument, "Документ", Properties,
                                                                                    baseGroup, paramsGroup);

            addPropertyView(objPrimDoc, objArt, Properties,
                    paramsGroup);

            addFixedFilter(new Filter(getPropertyView(isDocArtChangesParams.Property).View, Filter.NOT_EQUALS, new UserValueLink(false)));
            addFixedFilter(new Filter(getPropertyView(primDocStore.Property).View, Filter.EQUALS, new ObjectValueLink(objStore)));

            DefaultClientFormView formView = new DefaultClientFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyView(primDocDate.Property)), false);
            richDesign = formView;
        }
    }

    private class StoreArticleDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticleDocNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(document, "Документ", Properties,
                                                                                    baseGroup, docDate, storeGroup, true, supplierGroup, true, customerGroup, true);

            addPropertyView(dltDocStoreQuantity, objDoc, objStore, objArt);

            addFixedFilter(new Filter(addPropertyObjectImplement(isDocStoreArtInclude, objDoc, objStore, objArt), Filter.EQUALS, new UserValueLink(true)));

            DefaultClientFormView formView = new DefaultClientFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyView(docDate.Property)), false);
            richDesign = formView;
        }
    }

    private class ArticleStoreNavigatorForm extends TmcNavigatorForm {

        ObjectImplement objStore, objArt;

        public ArticleStoreNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup);

            objStore = addSingleGroupObjectImplement(store, "Склад", Properties,
                                                                        baseGroup);

            addPropertyView(objStore, objArt, Properties,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);

            addPropertyView(Properties, baseGroup, false, objArt.GroupTo, objStore, objArt);
            addPropertyView(Properties, balanceGroup, false, objArt.GroupTo, objStore, objArt);
            addPropertyView(Properties, incPrmsGroup, false, objArt.GroupTo, objStore, objArt);
            addPropertyView(Properties, outPrmsGroup, false, objArt.GroupTo, objStore, objArt);
        }

    }

    private class ArticleMStoreNavigatorForm extends TmcNavigatorForm {

        GroupObjectImplement gobjArtStore;
        ObjectImplement objStore, objArt;

        public ArticleMStoreNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            gobjArtStore = new GroupObjectImplement(IDShift(1));

            objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArtStore);
            objStore = new ObjectImplement(IDShift(1), store, "Склад", gobjArtStore);

            addGroup(gobjArtStore);

            addPropertyView(Properties, baseGroup, false, objArt);
            addPropertyView(Properties, baseGroup, false, objStore);

            addPropertyView(objStore, objArt, Properties,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);
        }

    }

    private class SupplierStoreArticleNavigatorForm extends TmcNavigatorForm {

        SupplierStoreArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objSupplier = addSingleGroupObjectImplement(supplier, "Поставщик", Properties,
                                                                                    baseGroup);
            objSupplier.GroupTo.gridClassView = false;
            objSupplier.GroupTo.singleViewType = true;

            ObjectImplement objStore = addSingleGroupObjectImplement(store, "Склад", Properties,
                                                                        baseGroup);
            objStore.GroupTo.gridClassView = false;

            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", Properties,
                                                                        baseGroup);

            addPropertyView(objStore, objArt, Properties,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);

            addFixedFilter(new Filter(addPropertyObjectImplement(storeSupplier, objStore, objArt), Filter.EQUALS, new ObjectValueLink(objSupplier)));
        }
    }



    private class DateIntervalNavigatorForm extends TmcNavigatorForm {

        GroupObjectImplement gobjInterval;
        ObjectImplement objDateFrom, objDateTo;

        public DateIntervalNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            gobjInterval = new GroupObjectImplement(IDShift(1));
            gobjInterval.gridClassView = false;
            gobjInterval.singleViewType = true;

            objDateFrom = new ObjectImplement(IDShift(1), Class.date, "С даты :", gobjInterval);
            objDateTo = new ObjectImplement(IDShift(1), Class.date, "По дату :", gobjInterval);

            addGroup(gobjInterval);
        }
    }

    private class MainAccountNavigatorForm extends DateIntervalNavigatorForm {

        public MainAccountNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objStore = addSingleGroupObjectImplement(store, "Склад", Properties,
                                                                        baseGroup);
            ObjectImplement objDoc = addSingleGroupObjectImplement(document, "Документ", Properties,
                                                                                    baseGroup, docDate);

            addPropertyView(balanceDocStoreDateMSumAccount, objStore, objDateFrom);
            addPropertyView(balanceDocStoreDateESumAccount, objStore, objDateTo);

            addPropertyView(dltDocStoreSumAccount, objDoc, objStore);

            addFixedFilter(new Filter(getPropertyView(dltDocStoreSumAccount.Property).View, Filter.NOT_EQUALS, new UserValueLink(0)));
            addFixedFilter(new Filter(getPropertyView(docDate.Property).View, Filter.GREATER_EQUALS, new ObjectValueLink(objDateFrom)));
            addFixedFilter(new Filter(getPropertyView(docDate.Property).View, Filter.LESS_EQUALS, new ObjectValueLink(objDateTo)));

            DefaultClientFormView formView = new DefaultClientFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyView(docDate.Property)), true);
            richDesign = formView;

        }
    }

    // ------------------------------------- Временные методы --------------------------- //

    void fillData(DataAdapter Adapter) throws SQLException {

        int Modifier = 10;
        int PropModifier = 1;

        Map<Class,Integer> ClassQuantity = new HashMap();
        List<Class> articleChilds = new ArrayList();
        article.fillChilds(articleChilds);
        for (Class articleClass : articleChilds)
            ClassQuantity.put(articleClass,Modifier*10/articleChilds.size());

        ClassQuantity.put(articleGroup,((Double)(Modifier*0.3)).intValue());
//        ClassQuantity.put(store,((Double)(Modifier*0.3)).intValue());
        ClassQuantity.put(store,3);
        ClassQuantity.put(supplier,5);
        ClassQuantity.put(customer,5);
        ClassQuantity.put(extIncomeDocument,Modifier*2);
        ClassQuantity.put(extIncomeDetail,Modifier*100*PropModifier);
        ClassQuantity.put(intraDocument,Modifier);
        ClassQuantity.put(cashSaleDocument,Modifier);
        ClassQuantity.put(receipt,Modifier*8);
        ClassQuantity.put(clearingSaleDocument,((Double)(Modifier*0.5)).intValue());
        ClassQuantity.put(invDocument,((Double)(Modifier*0.2)).intValue());
        ClassQuantity.put(returnDocument,((Double)(Modifier*0.3)).intValue());
        ClassQuantity.put(exchangeDocument,Modifier);
        ClassQuantity.put(revalDocument,((Double)(Modifier*0.5)).intValue());

        Map<DataProperty, Set<DataPropertyInterface>> PropNotNulls = new HashMap();
        name.putNotNulls(PropNotNulls,0);
        artGroup.putNotNulls(PropNotNulls,0);
        extIncDate.putNotNulls(PropNotNulls,0);
        intraDate.putNotNulls(PropNotNulls,0);
        extOutDate.putNotNulls(PropNotNulls,0);
        exchDate.putNotNulls(PropNotNulls,0);
        revalDate.putNotNulls(PropNotNulls,0);
        extIncStore.putNotNulls(PropNotNulls,0);
        intraIncStore.putNotNulls(PropNotNulls,0);
        intraOutStore.putNotNulls(PropNotNulls,0);
        extOutStore.putNotNulls(PropNotNulls,0);
        exchStore.putNotNulls(PropNotNulls,0);
        revalStore.putNotNulls(PropNotNulls,0);
        intraIncStore.putNotNulls(PropNotNulls,0);
        extIncSupplier.putNotNulls(PropNotNulls,0);
        extIncDetailDocument.putNotNulls(PropNotNulls,0);
        extIncDetailArticle.putNotNulls(PropNotNulls,0);
        extIncDetailQuantity.putNotNulls(PropNotNulls,0);
        extIncDetailPriceIn.putNotNulls(PropNotNulls,0);
        extIncDetailVATIn.putNotNulls(PropNotNulls,0);
        receiptSaleDocument.putNotNulls(PropNotNulls,0);
        clearingSaleCustomer.putNotNulls(PropNotNulls,0);
        returnSupplier.putNotNulls(PropNotNulls,0);

//        LDP extIncDetailSumVATIn, extIncDetailSumPay;
//        LDP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
//        LDP extIncDetailPriceOut;

        Map<DataProperty,Integer> PropQuantity = new HashMap();

//        PropQuantity.put((DataProperty)extIncQuantity.Property,10);
        PropQuantity.put((DataProperty)intraQuantity.Property,Modifier*PropModifier*2);
        PropQuantity.put((DataProperty)receiptQuantity.Property,Modifier*PropModifier*30);
        PropQuantity.put((DataProperty)receiptSumPriceOut.Property,Modifier*PropModifier*30);
        PropQuantity.put((DataProperty)clearingSaleQuantity.Property,Modifier*PropModifier*8);
        PropQuantity.put((DataProperty)invQuantity.Property,Modifier*PropModifier);
        PropQuantity.put((DataProperty)exchangeQuantity.Property,Modifier*PropModifier);
        PropQuantity.put((DataProperty)returnQuantity.Property,Modifier*PropModifier);
        PropQuantity.put((DataProperty)isRevalued.Property,Modifier*PropModifier);

        PropQuantity.putAll(autoQuantity(0, fixedPriceIn, fixedVATIn, fixedAdd, fixedVATOut, fixedLocTax,
            revalBalanceQuantity,revalPriceIn,revalVATIn,revalAddBefore,revalVATOutBefore,revalLocTaxBefore,
                revalAddAfter,revalVATOutAfter,revalLocTaxAfter));

        autoFillDB(Adapter,ClassQuantity,PropQuantity,PropNotNulls);
    }

}


