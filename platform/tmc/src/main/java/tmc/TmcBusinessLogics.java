package tmc;

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
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.net.MalformedURLException;

import platform.server.logics.BusinessLogics;
import platform.server.logics.session.DataSession;
import platform.server.logics.auth.SecurityPolicy;
import platform.interop.UserInfo;
import platform.interop.Compare;
import platform.server.logics.auth.User;
import platform.server.logics.data.TableImplement;
import platform.server.logics.properties.groups.AbstractGroup;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.AggregateProperty;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.logics.properties.linear.*;
import platform.server.logics.classes.RemoteClass;
import platform.server.data.Union;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.view.navigator.NavigatorForm;
import platform.server.view.navigator.NavigatorElement;
import platform.server.view.form.client.*;
import platform.server.view.form.*;

public class TmcBusinessLogics extends BusinessLogics<TmcBusinessLogics> {

    protected DataAdapter newAdapter() throws ClassNotFoundException {
        return new PostgreDataAdapter("testplat","localhost");
    }

    public TmcBusinessLogics() throws RemoteException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super();
    }

    public TmcBusinessLogics(int testType,Integer seed,int iterations) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, RemoteException {
        super(testType,seed,iterations);
    }

//    static Registry registry;
    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, RemoteException, FileNotFoundException, JRException, MalformedURLException {

        LocateRegistry.createRegistry(7653).rebind("TmcBusinessLogics", new TmcBusinessLogics());
//        Naming.rebind("rmi://127.0.0.1:1099/TmcBusinessLogics",new TmcBusinessLogics());
    }

    RemoteClass article;
    RemoteClass articleGroup;

    RemoteClass store;
    RemoteClass supplier;
    RemoteClass customer;

    RemoteClass document;
    RemoteClass primaryDocument, secondaryDocument;
    RemoteClass fixedDocument, accountDocument;
    RemoteClass quantityDocument;
    RemoteClass incomeDocument;
    RemoteClass outcomeDocument;

    RemoteClass extIncomeDocument;
    RemoteClass extIncomeDetail;

    RemoteClass intraDocument;
    RemoteClass extOutcomeDocument;
    RemoteClass exchangeDocument;
    RemoteClass revalDocument;

    RemoteClass saleDocument;
    RemoteClass cashSaleDocument;
    RemoteClass clearingSaleDocument;
    RemoteClass invDocument;
    RemoteClass returnDocument;

    RemoteClass receipt;

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

    protected void initProperties() {

        initAbstractProperties();
        initClassProperties();
        initPrimaryProperties();
        initAggregateProperties();
    }

    // ======================================================================================================= //
    // ==================================== Инициализация абстратных свойств ================================= //
    // ======================================================================================================= //

    LOFP object1;
    LCFP equals2;
    LJP equals22;
    LCFP groeq2;
    LCFP greater2;
    LJP between;
    LNFP notZero;
    LSFP percent, revPercent, addPercent;
    LSFP round, roundm1;
    LMFP multiplyBit2, multiplyDouble2, multiplyDate2;

    private void initAbstractProperties() {

        equals2 = addCFProp(Compare.EQUALS);
        object1 = addOFProp(1);
        multiplyBit2 = addMFProp(RemoteClass.bit,2);
        equals22 = addJProp("И",multiplyBit2,4,equals2,1,2,equals2,3,4);
        groeq2 = addCFProp(Compare.GREATER_EQUALS);
        greater2 = addCFProp(Compare.GREATER);
        between = addJProp("Между",multiplyBit2,3,groeq2,1,2,groeq2,3,1);
        notZero = addNFProp();
        percent = addSFProp("((prm1*prm2)/100)", RemoteClass.doubleClass, 2);
        revPercent = addSFProp("((prm1*prm2)/(100+prm2))", RemoteClass.doubleClass, 2);
        addPercent = addSFProp("((prm1*(100+prm2))/100)", RemoteClass.doubleClass, 2);
        round = addSFProp("round(CAST(prm1 as numeric),0)", RemoteClass.doubleClass, 1);
        roundm1 = addSFProp("round(CAST(prm1 as numeric),-1)", RemoteClass.doubleClass, 1);
        multiplyDouble2 = addMFProp(RemoteClass.doubleClass,2);
        multiplyDate2 = addMFProp(RemoteClass.date,2);
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
    LP doubleExtOutDocArticle;

    private void initClassProperties() {

        dateDocument = addCProp("пустая дата", null, RemoteClass.date, document);
        datePrimDocument = addCProp("пустая дата", null, RemoteClass.date, primaryDocument);
        storePrimDoc = addCProp("пустой склад", null, store, primaryDocument);
        bitExtInc = addCProp("пустой бит", true, RemoteClass.bit, extIncomeDetail);
        bitDocStore = addCProp("пустой бит", null, RemoteClass.bit, document, store);
        doubleDocStore = addCProp("пустое число", null, RemoteClass.doubleClass, document, store);
        doubleDocArticle = addCProp("пустое кол-во", null, RemoteClass.doubleClass, document, article);
        doubleDocStoreArticle = addCProp("пустое число", null, RemoteClass.doubleClass, document, store, article);
        doubleIncDocArticle = addCProp("пустое кол-во", null, RemoteClass.doubleClass, incomeDocument, article);
        doubleOutDocArticle = addCProp("пустое кол-во", null, RemoteClass.doubleClass, outcomeDocument, article);
        bitPrimDocArticle = addCProp("пустой бит", null, RemoteClass.bit, primaryDocument, article);
        doublePrimDocArticle = addCProp("пустое число", null, RemoteClass.doubleClass, primaryDocument, article);
        doubleExtOutDocArticle = addCProp("пустое число", null, RemoteClass.doubleClass, extOutcomeDocument, article);

    }

    // ======================================================================================================= //
    // ==================================== Инициализация первичных свойств ================================== //
    // ======================================================================================================= //

    private void initPrimaryProperties() {

        initObjectProperties();
        initArticleProperties();
        initCustomArticleLogics();
        initExtIncProperties();
        initClearingSaleProperties();
        initReceiptProperties();
        initReturnProperties();
        initRevalProperties();
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Общие свойства -------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LDP name;

    private void initObjectProperties() {

        name = addDProp(baseGroup, "name", "Имя", RemoteClass.string(50), objectClass);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------------- Товар ----------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LDP artGroup;
    LJP artGroupName;

    LDP artBarCode;

    private void initArticleProperties() {

        artGroup = addDProp("Гр. тов.", articleGroup, article);
        artGroupName = addJProp(artgrGroup, "Имя гр. тов.", name, 1, artGroup, 1);

        artBarCode = addDProp(baseGroup, "Штрих-код", RemoteClass.numeric(15,2),  article);
        article.externalID = (DataProperty)artBarCode.property;
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Внешний приход -------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LDP extIncSupplier;
    LJP extIncSupplierName;

    LDP extIncDetailDocument;
    LDP extIncDetailArticle;
    LJP extIncDetailArticleName;

    private void initExtIncProperties() {

        extIncSupplier = addDProp("Поставщик", supplier, extIncomeDocument);
        extIncSupplierName = addJProp(supplierGroup, "extIncSupplierName", "Имя поставщика", name, 1, extIncSupplier, 1);

        extIncDetailDocument = addDProp("extIncDetailDocument", "Документ", extIncomeDocument, extIncomeDetail);

        extIncDetailArticle = addDProp("Товар", article, extIncomeDetail);
        extIncDetailArticleName = addJProp(artclGroup, "extIncDetailArticleName", "Имя товара", name, 1, extIncDetailArticle, 1);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // -------------------------------------------------- Чеки ----------------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LDP receiptSaleDocument;

    private void initReceiptProperties() {

        receiptSaleDocument = addDProp("receiptSaleDocument","Документ продажи", cashSaleDocument, receipt);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ---------------------------------------- Реализация по б/н расчету ------------------------------------ //
    // ------------------------------------------------------------------------------------------------------- //

    LDP clearingSaleCustomer;
    LJP clearingSaleCustomerName;

    private void initClearingSaleProperties() {

        clearingSaleCustomer = addDProp("clearingSaleCustomer","Покупатель", customer, clearingSaleDocument);
        clearingSaleCustomerName = addJProp(customerGroup, "Имя покупателя", name, 1, clearingSaleCustomer, 1);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------- Возврат поставщику ---------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LDP returnSupplier;
    LJP returnSupplierName;

    private void initReturnProperties() {

        returnSupplier = addDProp("returnSupplier","Поставщик", supplier, returnDocument);
        returnSupplierName = addJProp(supplierGroup, "Имя поставщика", name, 1, returnSupplier, 1);
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

    LDP extIncDate;
    LDP intraDate;
    LDP extOutDate;
    LDP exchDate;
    LDP revalDate;

    private void initDatePrimaryProperties() {

        extIncDate = addDProp(baseGroup, "extIncDate", "Дата", RemoteClass.date, extIncomeDocument);
        intraDate = addDProp(baseGroup, "intraDate", "Дата", RemoteClass.date, intraDocument);
        extOutDate = addDProp(baseGroup, "extOutDate", "Дата", RemoteClass.date, extOutcomeDocument);
        exchDate = addDProp(baseGroup, "exchDate", "Дата", RemoteClass.date, exchangeDocument);
        revalDate = addDProp(baseGroup, "revalDate", "Дата", RemoteClass.date, revalDocument);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LUP primDocDate, secDocDate;
    LUP docDate;

    private void initDateOverrideProperties() {

        primDocDate = addUProp(paramsGroup, "Дата", Union.OVERRIDE, 1, 1, datePrimDocument, 1, 1, extIncDate, 1, 1, intraDate, 1, 1, revalDate, 1);
        secDocDate = addUProp("Дата", Union.OVERRIDE, 1, 1, extOutDate, 1, 1, exchDate, 1);

        docDate = addUProp("docDate", "Дата", Union.OVERRIDE, 1, 1, dateDocument, 1, 1, secDocDate, 1, 1, primDocDate, 1);
    }

    // ------------------------------------ Свойства по документам ------------------------------------------- //

    LJP groeqDocDate, greaterDocDate, betweenDocDate;

    private void initDateDocProperties() {

        groeqDocDate = addJProp("Дата док.>=Дата", groeq2, 2, docDate, 1, 2);
        greaterDocDate = addJProp("Дата док.>Дата", greater2, 2, docDate, 1, 2);
        betweenDocDate = addJProp("Дата док. между Дата", between, 3, docDate, 1, 2, 3);
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

    LDP extIncStore;
    LDP intraOutStore, intraIncStore;
    LJP intraStoreName;
    LDP extOutStore;
    LDP exchStore;
    LDP revalStore;

    LJP receiptStore;

    private void initStorePrimaryProperties() {

        extIncStore = addDProp("extIncStore", "Склад", store, extIncomeDocument);

        intraOutStore = addDProp("intraOutStore", "Склад отпр.", store, intraDocument);
        intraIncStore = addDProp("intraIncStore", "Склад назн.", store, intraDocument);
        intraStoreName = addJProp(storeGroup, "Имя склада (назн.)", name, 1, intraIncStore, 1);

        extOutStore = addDProp("extOutStore", "Склад", store, extOutcomeDocument);
        exchStore = addDProp("exchStore", "Склад", store, exchangeDocument);
        revalStore = addDProp("revalStore", "Склад", store, revalDocument);

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
    
    private void initStoreOverrideProperties() {

        incQStore = addUProp("Склад прих.", Union.OVERRIDE, 1, 1, extIncStore, 1, 1, intraIncStore, 1, 1, exchStore, 1);
        outQStore = addUProp("Склад расх.", Union.OVERRIDE, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1);

        incSStore = addUProp("Склад прих. (сум)", Union.OVERRIDE, 1, 1, incQStore, 1, 1, revalStore, 1);
        outSStore = outQStore;

        primDocStore = addUProp(paramsGroup, "Склад (изм.)", Union.OVERRIDE, 1, 1, storePrimDoc, 1, 1, extIncStore, 1, 1, intraIncStore, 1, 1, revalStore, 1);
        fixedStore = addUProp("Склад (парам.)", Union.OVERRIDE, 1, 1, receiptStore, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1);

        docStore = addUProp("docStore", "Склад", Union.OVERRIDE, 1, 1, extIncStore, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1, 1, revalStore, 1);

        docStoreName = addJProp(storeGroup, "docStoreName", "Имя склада", name, 1, docStore, 1);
    }

    // ------------------------------------ Свойства по документам ------------------------------------------- //

    LJP isDocIncQStore;
    LJP isDocOutQStore;
    LJP isDocRevalStore;
    LJP isDocIncSStore;
    LJP isDocOutSStore;
    LUP isDocStore;

    private void initDocStoreProperties() {

        isDocIncQStore = addJProp("Склад=прих.кол-во", equals2, 2, incQStore, 1, 2);
        isDocOutQStore = addJProp("Склад=расх.кол-во", equals2, 2, outQStore, 1, 2);
        isDocRevalStore = addJProp("Склад=переоц.", equals2, 2, revalStore, 1, 2);

        isDocIncSStore = addJProp("Склад=прих.кол-во", equals2, 2, incSStore, 1, 2);
        isDocOutSStore = addJProp("Склад=расх.кол-во", equals2, 2, outSStore, 1, 2);

        isDocStore = addUProp("Склад=док.", Union.OVERRIDE, 2, 1, bitDocStore, 1, 2, 1, isDocIncQStore, 1, 2, 1, isDocOutQStore, 1, 2, 1, isDocRevalStore, 1, 2);
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

    LDP extIncDetailQuantity;
    LGP extIncDocumentQuantity;
    LGP extIncQuantity;

    LDP intraQuantity;

    LDP receiptQuantity;
    LGP cashSaleQuantity;
    LDP clearingSaleQuantity;
    LUP saleQuantity;
    LDP invQuantity;
    LDP invBalance;
    LDP returnQuantity;
    LUP extOutQuantity;

    LDP exchangeQuantity;
    LGP exchIncQuantity, exchOutQuantity;
    LUP exchDltQuantity;

    LDP revalBalanceQuantity;

    private void initQuantityPrimaryProperties() {

        extIncDetailQuantity = addDProp(quantGroup, "extIncDetailQuantity", "Кол-во", RemoteClass.doubleClass, extIncomeDetail);
        extIncDocumentQuantity = addGProp(quantGroup, "extIncDocumentQuantity", "Кол-во (всего)", extIncDetailQuantity, true, extIncDetailDocument, 1);

        extIncQuantity = addGProp(quantGroup, "Кол-во прих.", extIncDetailQuantity, true, extIncDetailDocument, 1, extIncDetailArticle, 1);

        intraQuantity = addDProp(quantGroup, "Кол-во внутр.", RemoteClass.doubleClass, intraDocument, article);

        receiptQuantity = addDProp(quantGroup, "receiptQuantity", "Кол-во в чеке", RemoteClass.doubleClass, receipt, article);
        cashSaleQuantity = addGProp(quantGroup, "cashSaleQuantity", "Кол-во прод.", receiptQuantity, true, receiptSaleDocument, 1, 2);

        clearingSaleQuantity = addDProp(quantGroup, "clearingSaleQuantity", "Кол-во расх.", RemoteClass.doubleClass, clearingSaleDocument, article);

        saleQuantity = addUProp("Кол-во реал.", Union.SUM, 2, 1, clearingSaleQuantity, 1, 2, 1, cashSaleQuantity, 1, 2);

        invQuantity = addDProp(quantGroup, "invQuantity", "Кол-во инв.", RemoteClass.doubleClass, invDocument, article);
        invBalance = addDProp(quantGroup, "invBalance", "Остаток инв.", RemoteClass.doubleClass, invDocument, article);
//        LP defInvQuantity = addUProp("Кол-во инв. (по умолч.)", 1, 2, 1, docOutBalanceQuantity, 1, 2, -1, invBalance, 1, 2);
//        setDefProp(invQuantity, defInvQuantity, true);

        returnQuantity = addDProp(quantGroup, "returnQuantity", "Кол-во возвр.", RemoteClass.doubleClass, returnDocument, article);

        extOutQuantity = addUProp("Кол-во расх.", Union.SUM, 2, 1, doubleExtOutDocArticle, 1, 2, 1, returnQuantity, 1, 2, 1, invQuantity, 1, 2, 1, clearingSaleQuantity, 1, 2, 1, cashSaleQuantity, 1, 2);

        exchangeQuantity = addDProp(quantGroup, "exchangeQuantity", "Кол-во перес.", RemoteClass.doubleClass, exchangeDocument, article, article);

        revalBalanceQuantity = addDProp(quantGroup, "revalBalanceQuantity", "Остаток", RemoteClass.doubleClass, revalDocument, article);

        exchIncQuantity = addGProp("Прих. перес.", exchangeQuantity, true, 1, 3);
        exchOutQuantity = addGProp("Расх. перес.", exchangeQuantity, true, 1, 2);
        exchDltQuantity = addUProp("Разн. перес.", Union.SUM, 2, 1, exchIncQuantity, 1, 2, -1, exchOutQuantity, 1, 2);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LUP incQuantity;
    LUP outQuantity;
    LUP quantity;

    private void initQuantityOverrideProperties() {

        incQuantity = addUProp("Кол-во прих.", Union.SUM, 2, 1, doubleIncDocArticle, 1, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchIncQuantity, 1, 2);
        outQuantity = addUProp("Кол-во расх.", Union.SUM, 2, 1, doubleOutDocArticle, 1, 2, 1, extOutQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchOutQuantity, 1, 2);

        quantity = addUProp("Кол-во", Union.OVERRIDE, 2, 1, doubleDocArticle, 1, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2,
                                                                               1, extOutQuantity, 1, 2, 1, exchDltQuantity, 1, 2 );
    }

    // ---------------------------------- Свойства по документам/складам --------------------------------------- //

    LJP incDocStoreQuantity, outDocStoreQuantity;
    LUP dltDocStoreQuantity;

    private void initQuantityDocStoreProperties() {

        incDocStoreQuantity = addJProp("Кол-во прих. по скл.", multiplyDouble2, 3, isDocIncQStore, 1, 2, incQuantity, 1, 3);
        outDocStoreQuantity = addJProp("Кол-во расх. по скл.", multiplyDouble2, 3, isDocOutQStore, 1, 2, outQuantity, 1, 3);
        dltDocStoreQuantity = addUProp("Кол-во (+-)", Union.SUM, 3, 1, doubleDocStoreArticle, 1, 2, 3, 1, incDocStoreQuantity, 1, 2, 3, -1, outDocStoreQuantity, 1, 2, 3);
    }

    // ----------------------------------------- Свойства по складам ------------------------------------------- //

    LGP incStoreQuantity, outStoreQuantity;
    LUP balanceStoreQuantity;

    private void initQuantityStoreProperties() {

        incStoreQuantity = addGProp("Прих. на скл.", incQuantity, true, incQStore, 1, 2);
        outStoreQuantity = addGProp("Расх. со скл.", outQuantity, true, outQStore, 1, 2);

        balanceStoreQuantity = addUProp(balanceGroup, "Ост. на скл.", Union.SUM, 2, 1, incStoreQuantity, 1, 2, -1, outStoreQuantity, 1, 2);
    }

    // ----------------------------------------- Свойства по датам ------------------------------------------------ //


    LJP incGroeqDateQuantity, outGroeqDateQuantity;
    LGP incStoreArticleGroeqDateQuantity, outStoreArticleGroeqDateQuantity;
    LUP dltStoreArticleGroeqDateQuantity;

    LJP incGreaterDateQuantity, outGreaterDateQuantity;
    LGP incStoreArticleGreaterDateQuantity, outStoreArticleGreaterDateQuantity;
    LUP dltStoreArticleGreaterDateQuantity;

    LUP balanceStoreDateMQuantity;
    LUP balanceStoreDateEQuantity;

    LJP incBetweenDateQuantity, outBetweenDateQuantity;
    LGP incStoreArticleBetweenDateQuantity, outStoreArticleBetweenDateQuantity;

    private void initQuantityDateProperties() {

        incGroeqDateQuantity = addJProp("Кол-во прих. с даты", multiplyDouble2, 3, groeqDocDate, 1, 3, incQuantity, 1, 2);
        outGroeqDateQuantity = addJProp("Кол-во расх. с даты", multiplyDouble2, 3, groeqDocDate, 1, 3, outQuantity, 1, 2);

        incStoreArticleGroeqDateQuantity = addGProp("Кол-во прих. на скл. с даты", incGroeqDateQuantity, true, incQStore, 1, 2, 3);
        outStoreArticleGroeqDateQuantity = addGProp("Кол-во расх. со скл. с даты", outGroeqDateQuantity, true, outQStore, 1, 2, 3);
        dltStoreArticleGroeqDateQuantity = addUProp("Кол-во на скл. с даты", Union.SUM, 3, 1, incStoreArticleGroeqDateQuantity, 1, 2, 3, -1, outStoreArticleGroeqDateQuantity, 1, 2, 3);

        incGreaterDateQuantity = addJProp("Кол-во прих. после даты", multiplyDouble2, 3, greaterDocDate, 1, 3, incQuantity, 1, 2);
        outGreaterDateQuantity = addJProp("Кол-во расх. после даты", multiplyDouble2, 3, greaterDocDate, 1, 3, outQuantity, 1, 2);

        incStoreArticleGreaterDateQuantity = addGProp("Кол-во прих. на скл. после даты", incGreaterDateQuantity, true, incQStore, 1, 2, 3);
        outStoreArticleGreaterDateQuantity = addGProp("Кол-во расх. со скл. после даты", outGreaterDateQuantity, true, outQStore, 1, 2, 3);
        dltStoreArticleGreaterDateQuantity = addUProp("Кол-во на скл. после даты", Union.SUM, 3, 1, incStoreArticleGreaterDateQuantity, 1, 2, 3, -1, outStoreArticleGreaterDateQuantity, 1, 2, 3);

        balanceStoreDateMQuantity = addUProp(quantGroup, "Кол-во на начало", Union.SUM, 3, 1, addJProp("", balanceStoreQuantity, 3, 1, 2), 1, 2, 3, -1, dltStoreArticleGroeqDateQuantity, 1, 2, 3);
        balanceStoreDateEQuantity = addUProp(quantGroup, "Кол-во на конец", Union.SUM, 3, 1, addJProp("", balanceStoreQuantity, 3, 1, 2), 1, 2, 3, -1, dltStoreArticleGreaterDateQuantity, 1, 2, 3);

        incBetweenDateQuantity = addJProp("Кол-во прих. за интервал", multiplyDouble2, 4, betweenDocDate, 1, 3, 4, incQuantity, 1, 2);
        outBetweenDateQuantity = addJProp("Кол-во расх. за интервал", multiplyDouble2, 4, betweenDocDate, 1, 3, 4, outQuantity, 1, 2);

        incStoreArticleBetweenDateQuantity = addGProp(quantGroup, "Кол-во прих. на скл. за интервал", incBetweenDateQuantity, true, incQStore, 1, 2, 3, 4);
        outStoreArticleBetweenDateQuantity = addGProp(quantGroup, "Кол-во расх. со скл. за интервал", outBetweenDateQuantity, true, outQStore, 1, 2, 3, 4);
    }

    // ----------------------------------------- Свойства по реализации ------------------------------------------- //

    LJP saleBetweenDateQuantity;
    LGP saleStoreArticleBetweenDateQuantity;
    LGP saleArticleBetweenDateQuantity;

    private void initQuantitySaleProperties() {

        saleBetweenDateQuantity = addJProp("Кол-во реал. за интервал", multiplyDouble2, 4, betweenDocDate, 1, 3, 4, saleQuantity, 1, 2);
        saleStoreArticleBetweenDateQuantity = addGProp(quantGroup, "Кол-во реал. на скл. за интервал", saleBetweenDateQuantity, true, extOutStore, 1, 2, 3, 4);

        saleArticleBetweenDateQuantity = addGProp(quantGroup, "Реал. кол-во (по товару)", saleStoreArticleBetweenDateQuantity, true, 2, 3, 4);
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

    private void initDocArtProperties() {

        notZeroExtIncQuantity = addJProp("Есть во вн. прих.", notZero, 2, extIncQuantity, 1, 2);

        LP incPrmsQuantity = addUProp("Кол-во прих. (парам.)", Union.OVERRIDE, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2);
        notZeroIncPrmsQuantity = addJProp("Есть в перв. док.", notZero, 2, incPrmsQuantity, 1, 2);

        isRevalued = addDProp("isRevalued", "Переоц.", RemoteClass.bit, revalDocument, article);

        isDocArtChangesParams = addUProp(paramsGroup, "Изм. парам.", Union.OVERRIDE, 2, 1, bitPrimDocArticle, 1, 2, 1, isRevalued, 1, 2, 1, notZeroIncPrmsQuantity, 1, 2);

        notZeroQuantity = addJProp("Есть в док.", notZero, 2, quantity, 1, 2);
        isDocArtInclude = addUProp("Есть в док.", Union.OVERRIDE, 2, 1, notZeroQuantity, 1, 2, 1, isRevalued, 1, 2);

        isDocStoreArtInclude = addJProp("В док. и скл.", multiplyBit2, 3, isDocStore, 1, 2, isDocArtInclude, 1, 3);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------------ Последние документы ---------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    LGP maxStoreExtIncDate;
    LGP maxStoreExtIncDoc;

    LGP maxChangesParamsDate;
    LGP maxChangesParamsDoc;

    private void initMaxProperties() {

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

    LDP extIncDetailPriceIn, extIncDetailVATIn;

    LDP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
    LJP extIncDetailCalcPriceOut;
    LDP extIncDetailPriceOut;

    LGP extIncLastDetail;

    LJP extIncPriceIn, extIncVATIn;
    LJP extIncAdd, extIncVATOut, extIncLocTax;
    LJP extIncPriceOut;

    private void initParamsPrimaryExtIncProperties() {

        extIncDetailPriceIn = addDProp(incPrmsGroup, "extIncDetailPriceIn", "Цена пост.", RemoteClass.doubleClass, extIncomeDetail);
        extIncDetailVATIn = addDProp(incPrmsGroup, "extIncDetailVATIn", "НДС пост.", RemoteClass.doubleClass, extIncomeDetail);

        // -------------------------- Выходные параметры ---------------------------- //

        extIncDetailAdd = addDProp(outPrmsGroup, "extIncDetailAdd", "Надбавка", RemoteClass.doubleClass, extIncomeDetail);
        extIncDetailVATOut = addDProp(outPrmsGroup, "extIncDetailVATOut", "НДС прод.", RemoteClass.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailVATOut, extIncDetailVATIn, true);
        extIncDetailLocTax = addDProp(outPrmsGroup, "extIncDetailLocTax", "Местн. нал.", RemoteClass.doubleClass, extIncomeDetail);

        extIncDetailCalcPriceOut = addJProp("Цена розн. (расч.)", roundm1, 1,
                                   addJProp("Цена розн. (расч. - неокр.)", addPercent, 1,
                                   addJProp("Цена с НДС", addPercent, 1,
                                   addJProp("Цена с надбавкой", addPercent, 1,
                                           extIncDetailPriceIn, 1,
                                           extIncDetailAdd, 1), 1,
                                           extIncDetailVATOut, 1), 1,
                                           extIncDetailLocTax, 1), 1);

        extIncDetailPriceOut = addDProp(outPrmsGroup, "extIncDetailPriceOut", "Цена розн.", RemoteClass.doubleClass, extIncomeDetail);
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

    private void initParamsPrimaryFixedProperties() {

        fixedPriceIn = addDProp("fixedPriceIn", "Цена пост.", RemoteClass.doubleClass, fixedDocument, article);
        fixedVATIn = addDProp("fixedVATIn", "НДС пост.", RemoteClass.doubleClass, fixedDocument, article);
        fixedAdd = addDProp("fixedAdd", "Надбавка", RemoteClass.doubleClass, fixedDocument, article);
        fixedVATOut = addDProp("fixedVATOut", "НДС прод.", RemoteClass.doubleClass, fixedDocument, article);
        fixedLocTax = addDProp("fixedLocTax", "Местн. нал.", RemoteClass.doubleClass, fixedDocument, article);
        fixedPriceOut = addDProp("fixedPriceOut", "Цена розн.", RemoteClass.doubleClass, fixedDocument, article);
    }

    // Переоценка

    LJP revaluedBalanceQuantity;
    LDP revalPriceIn, revalVATIn;
    LDP revalAddBefore, revalVATOutBefore, revalLocTaxBefore;
    LDP revalPriceOutBefore;
    LDP revalAddAfter, revalVATOutAfter, revalLocTaxAfter;
    LDP revalPriceOutAfter;
    
    private void initParamsPrimaryRevalProperties() {

        revaluedBalanceQuantity = addJProp("Остаток (переоц.)", multiplyDouble2, 2, revalBalanceQuantity, 1, 2, isRevalued, 1, 2);

        revalPriceIn = addDProp("revalPriceIn", "Цена пост.", RemoteClass.doubleClass, revalDocument, article);
        revalVATIn = addDProp("revalVATIn", "НДС пост.", RemoteClass.doubleClass, revalDocument, article);
        revalAddBefore = addDProp("revalAddBefore", "Надбавка (до)", RemoteClass.doubleClass, revalDocument, article);
        revalVATOutBefore = addDProp("revalVATOutBefore", "НДС прод. (до)", RemoteClass.doubleClass, revalDocument, article);
        revalLocTaxBefore = addDProp("revalLocTaxBefore", "Местн. нал. (до)", RemoteClass.doubleClass, revalDocument, article);
        revalPriceOutBefore = addDProp("revalPriceOutBefore", "Цена розн. (до)", RemoteClass.doubleClass, revalDocument, article);
        revalAddAfter = addDProp(outPrmsGroupAfter, "revalAddAfter", "Надбавка (после)", RemoteClass.doubleClass, revalDocument, article);
        revalVATOutAfter = addDProp(outPrmsGroupAfter, "revalVATOutAfter", "НДС прод. (после)", RemoteClass.doubleClass, revalDocument, article);
        revalLocTaxAfter = addDProp(outPrmsGroupAfter, "revalLocTaxAfter", "Местн. нал. (после)", RemoteClass.doubleClass, revalDocument, article);
        revalPriceOutAfter = addDProp(outPrmsGroupAfter, "revalPriceOutAfter", "Цена розн. (после)", RemoteClass.doubleClass, revalDocument, article);
    }

    // ------------------------------------ Перегруженные свойства ------------------------------------------- //

    LUP primDocPriceIn;
    LUP primDocVATIn;
    LUP primDocAdd;
    LUP primDocVATOut;
    LUP primDocLocTax;
    LUP primDocPriceOut;

    private void initParamsOverrideProperties() {

        primDocPriceIn = addUProp(paramsGroup, "Цена пост. (изм.)", Union.OVERRIDE, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedPriceIn, 1, 2, 1, extIncPriceIn, 1, 2, 1, revalPriceIn, 1, 2);
        primDocVATIn = addUProp(paramsGroup, "НДС пост. (изм.)", Union.OVERRIDE, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedVATIn, 1, 2, 1, extIncVATIn, 1, 2, 1, revalVATIn, 1, 2);
        primDocAdd = addUProp(paramsGroup, "Надбавка (изм.)", Union.OVERRIDE, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedAdd, 1, 2, 1, extIncAdd, 1, 2, 1, revalAddAfter, 1, 2);
        primDocVATOut = addUProp(paramsGroup, "НДС прод. (изм.)", Union.OVERRIDE, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedVATOut, 1, 2, 1, extIncVATOut, 1, 2, 1, revalVATOutAfter, 1, 2);
        primDocLocTax = addUProp(paramsGroup, "Местн. нал. (изм.)", Union.OVERRIDE, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedLocTax, 1, 2, 1, extIncLocTax, 1, 2, 1, revalLocTaxAfter, 1, 2);
        primDocPriceOut = addUProp(paramsGroup, "Цена розн. (изм.)", Union.OVERRIDE, 2, 1, doublePrimDocArticle, 1, 2, 1, fixedPriceOut, 1, 2, 1, extIncPriceOut, 1, 2, 1, revalPriceOutAfter, 1, 2);
    }

    // -------------------------------------- Текущие свойства ---------------------------------------------- //

    LJP storeSupplier;

    LJP storePriceIn, storeVATIn;
    LJP storeAdd, storeVATOut, storeLocTax;
    LJP storePriceOut;

    private void initParamsCurrentProperties() {

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

    private void initSumProperties() {

        initSumInProperties();
        initSumOutProperties();
        initSumAccountProperties();
    }

    // ---------------------------------------- Входные суммы --------------------------------------------- //

    LJP extIncDetailCalcSum;
    LJP extIncDetailCalcSumVATIn;
    LUP extIncDetailCalcSumPay;
    LDP extIncDetailSumVATIn, extIncDetailSumPay;
    LUP extIncDetailSumInc;
    LGP extIncDocumentSumInc, extIncDocumentSumVATIn, extIncDocumentSumPay;

    private void initSumInProperties() {

        // -------------------------- Входные суммы ---------------------------- //

        extIncDetailCalcSum = addJProp("Сумма НДС (расч.)", round, 1,
                              addJProp("Сумма пост.", multiplyDouble2, 1, extIncDetailQuantity, 1, extIncDetailPriceIn, 1), 1);

        extIncDetailCalcSumVATIn = addJProp("Сумма НДС (расч.)", round, 1,
                                   addJProp("Сумма НДС (расч. - неокр.)", percent, 1, extIncDetailCalcSum, 1, extIncDetailVATIn, 1), 1);

        extIncDetailSumVATIn = addDProp(incSumsGroup, "extIncDetailSumVATIn", "Сумма НДС", RemoteClass.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailSumVATIn, extIncDetailCalcSumVATIn, true);

        extIncDetailCalcSumPay = addUProp("Всего с НДС (расч.)", Union.SUM, 1, 1, extIncDetailCalcSum, 1, 1, extIncDetailSumVATIn, 1);

        extIncDetailSumPay = addDProp(incSumsGroup, "extIncDetailSumPay", "Всего с НДС", RemoteClass.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailSumPay, extIncDetailCalcSumPay, true);

        extIncDetailSumInc = addUProp(incSumsGroup, "extIncDetailSumInc", "Сумма пост.", Union.SUM, 1, 1, extIncDetailSumPay, 1, -1, extIncDetailSumVATIn, 1);
        setPropOrder(extIncDetailSumInc.property, extIncDetailSumVATIn.property, true);

        extIncDocumentSumInc = addGProp(incSumsGroup, "extIncDocumentSumInc", "Сумма пост.", extIncDetailSumInc, true, extIncDetailDocument, 1);
        extIncDocumentSumVATIn = addGProp(incSumsGroup, "extIncDocumentSumVATIn", "Сумма НДС", extIncDetailSumVATIn, true, extIncDetailDocument, 1);
        extIncDocumentSumPay = addGProp(incSumsGroup, "extIncDocumentSumPay", "Всего с НДС", extIncDetailSumPay, true, extIncDetailDocument, 1);

    }

    // ---------------------------------------- Выходные суммы --------------------------------------------- //

    private void initSumOutProperties() {

        initSumPriceOutProperties();
        initSumLocTaxProperties();
        initSumVATOutProperties();
        initSumAddProperties();
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

    private void initSumPriceOutProperties() {

        extIncDetailSumPriceOut = addJProp(outSumsGroup, "extIncDetailSumPriceOut", "Сумма розн.", round, 1,
                                  addJProp("Сумма розн. (неокр.)", multiplyDouble2, 1, extIncDetailQuantity, 1, extIncDetailPriceOut, 1), 1);
        extIncDocumentSumPriceOut = addGProp(outSumsGroup, "extIncDocumentSumPriceOut", "Сумма розн. (всего)", extIncDetailSumPriceOut, true, extIncDetailDocument, 1);

        intraSumPriceOut = addJProp("Сумма розн. (вн.)", multiplyDouble2, 2, intraQuantity, 1, 2, fixedPriceOut, 1, 2);

        receiptSumPriceOut = addDProp(outSumsGroup, "receiptSumPriceOut", "Сумма по чеку", RemoteClass.doubleClass, receipt, article);
        receiptDocumentSumPriceOut = addGProp(outSumsGroup, "Сумма чека", receiptSumPriceOut, true, 1);
        cashSaleSumPriceOut = addGProp(outSumsGroup, "Сумма прод.", receiptSumPriceOut, true, receiptSaleDocument, 1, 2);

        LP extOutParamsSumPriceOut = addJProp("Сумма розн. (расх. расч.)", multiplyDouble2, 2, extOutQuantity, 1, 2, fixedPriceOut, 1, 2);
        extOutSumPriceOut = addUProp("Сумма розн. (расх. расч.)", Union.SUM, 2, 1, extOutParamsSumPriceOut, 1, 2, 1, cashSaleSumPriceOut, 1, 2);
        extOutDocumentSumPriceOut = addGProp(outSumsGroup, "Сумма розн. (всего)", extOutSumPriceOut, true, 1);

        exchIncSumPriceOut = addJProp("Сумма розн. (перес. +)", multiplyDouble2, 2, exchIncQuantity, 1, 2, fixedPriceOut, 1, 2);
        exchOutSumPriceOut = addJProp("Сумма розн. (перес. -)", multiplyDouble2, 2, exchOutQuantity, 1, 2, fixedPriceOut, 1, 2);
        exchDltSumPriceOut = addUProp("Сумма розн. (перес.)", Union.SUM, 2, 1, exchIncSumPriceOut, 1, 2, -1, exchOutSumPriceOut, 1, 2);

        revalSumPriceOutBefore = addJProp("Сумма розн. (переоц. до)", multiplyDouble2, 2, revaluedBalanceQuantity, 1, 2, revalPriceOutBefore, 1, 2);
        revalSumPriceOutAfter = addJProp("Сумма розн. (переоц. после)", multiplyDouble2, 2, revaluedBalanceQuantity, 1, 2, revalPriceOutAfter, 1, 2);
        revalSumPriceOut = addUProp("Сумма розн. (переоц.)", Union.SUM, 2, 1, revalSumPriceOutAfter, 1, 2, -1, revalSumPriceOutBefore, 1, 2);
    }

    // Налог с продаж
    LJP extIncDetailSumLocTax;
    LGP extIncDocumentSumLocTax;
    LUP extIncDetailSumWVAT;

    private void initSumLocTaxProperties() {

        extIncDetailSumLocTax = addJProp(outSumsGroup, "extIncDetailSumLocTax", "Сумма местн. нал.", round, 1,
                                addJProp("Сумма местн. нал. (неокр.)", revPercent, 1, extIncDetailSumPriceOut, 1, extIncDetailLocTax, 1), 1);
        setPropOrder(extIncDetailSumLocTax.property, extIncDetailSumPriceOut.property, true);
        extIncDocumentSumLocTax = addGProp(outSumsGroup, "extIncDocumentSumLocTax", "Сумма местн. нал. (всего)", extIncDetailSumLocTax, true, extIncDetailDocument, 1);

        extIncDetailSumWVAT = addUProp("Сумма с НДС (розн.)", Union.SUM, 1, 1, extIncDetailSumPriceOut, 1, -1, extIncDetailSumLocTax, 1);
    }

    // НДС розничный
    LJP extIncDetailSumVATOut;
    LGP extIncDocumentSumVATOut;
    LUP extIncDetailSumWAdd;

    private void initSumVATOutProperties() {

        extIncDetailSumVATOut = addJProp(outSumsGroup, "extIncDetailSumVATOut", "Сумма НДС розн.", round, 1,
                                addJProp("Сумма НДС (розн. неокр.)", revPercent, 1, extIncDetailSumWVAT, 1, extIncDetailVATOut, 1), 1);
        setPropOrder(extIncDetailSumVATOut.property, extIncDetailSumLocTax.property, true);
        extIncDocumentSumVATOut = addGProp(outSumsGroup, "extIncDocumentSumVATOut", "Сумма НДС розн. (всего)", extIncDetailSumVATOut, true, extIncDetailDocument, 1);

        extIncDetailSumWAdd = addUProp("Сумма с торг. надб.", Union.SUM, 1, 1, extIncDetailSumWVAT, 1, -1, extIncDetailSumVATOut, 1);
    }

    // Торговая надбавка
    LUP extIncDetailSumAdd;
    LGP extIncDocumentSumAdd;

    private void initSumAddProperties() {

        extIncDetailSumAdd = addUProp(outSumsGroup, "extIncDetailSumAdd", "Сумма торг. надб.", Union.SUM, 1, 1, extIncDetailSumWAdd, 1, -1, extIncDetailSumInc, 1);
        setPropOrder(extIncDetailSumAdd.property, extIncDetailSumVATOut.property, true);
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

    private void initSumAccountProperties() {

        accExcl = addDProp(accountGroup, "accExcl", "Искл.", RemoteClass.bit, accountDocument, article);

        extIncDetailSumAccount = extIncDetailSumPriceOut;
        extIncSumAccount = addGProp("Сумма учетн. (вх.)", extIncDetailSumAccount, true, extIncDetailDocument, 1, extIncDetailArticle, 1);

        intraSumAccount = intraSumPriceOut;
        extOutSumAccountExcl = addJProp("Сумма учетн. искл. (вых.)", multiplyDouble2, 2, extOutSumPriceOut, 1, 2, accExcl, 1, 2);
        extOutSumAccount = addUProp("Сумма учетн. (вых.)", Union.SUM, 2, 1, extOutSumPriceOut, 1, 2, -1, extOutSumAccountExcl, 1, 2);
        exchIncSumAccount = exchIncSumPriceOut;
        exchOutSumAccount = exchOutSumPriceOut;
        exchDltSumAccount = exchDltSumPriceOut;
        revalSumAccount = revalSumPriceOut;

        incSumAccount = addUProp("Сумма учетн. прих.", Union.SUM, 2, 1, extIncSumAccount, 1, 2, 1, intraSumAccount, 1, 2, 1, exchIncSumAccount, 1, 2, 1, revalSumAccount, 1, 2);
        outSumAccount = addUProp("Сумма учетн. расх.", Union.SUM, 2, 1, extOutSumAccount, 1, 2, 1, intraSumAccount, 1, 2, 1, exchOutSumAccount, 1, 2);

        incDocSumAccount = addGProp(accountGroup, "Сумма учетн. прих. на скл.", incSumAccount, true, 1);
        outDocSumAccount = addGProp(accountGroup, "Сумма учетн. расх. со скл.", outSumAccount, true, 1);

        incDocStoreSumAccount = addJProp("Сумма учетн. прих. по скл.", multiplyDouble2, 2, isDocIncSStore, 1, 2, incDocSumAccount, 1);
        outDocStoreSumAccount = addJProp("Сумма учетн. расх. по скл.", multiplyDouble2, 2, isDocOutSStore, 1, 2, outDocSumAccount, 1);
        dltDocStoreSumAccount = addUProp("Сумма учетн. товара (+-)", Union.SUM, 2, 1, doubleDocStore, 1, 2, 1, incDocStoreSumAccount, 1, 2, -1, outDocStoreSumAccount, 1, 2);


        incGroeqDateSumAccount = addJProp("Сумма учетн. прих. с даты", multiplyDouble2, 3, groeqDocDate, 1, 3, incSumAccount, 1, 2);
        outGroeqDateSumAccount = addJProp("Сумма учетн. расх. с даты", multiplyDouble2, 3, groeqDocDate, 1, 3, outSumAccount, 1, 2);

        incStoreGroeqDateSumAccount = addGProp("Сумма учетн. прих. на скл. с даты", incGroeqDateSumAccount, true, incSStore, 1, 3);
        outStoreGroeqDateSumAccount = addGProp("Сумма учетн. расх. со скл. с даты", outGroeqDateSumAccount, true, outSStore, 1, 3);
        dltStoreGroeqDateSumAccount = addUProp("Сумма учетн. на скл. с даты", Union.SUM, 2, 1, incStoreGroeqDateSumAccount, 1, 2, -1, outStoreGroeqDateSumAccount, 1, 2);

        incGreaterDateSumAccount = addJProp("Сумма учетн. прих. после даты", multiplyDouble2, 3, greaterDocDate, 1, 3, incSumAccount, 1, 2);
        outGreaterDateSumAccount = addJProp("Сумма учетн. расх. после даты", multiplyDouble2, 3, greaterDocDate, 1, 3, outSumAccount, 1, 2);

        incStoreGreaterDateSumAccount = addGProp("Сумма учетн. прих. на скл. после даты", incGreaterDateSumAccount, true, incSStore, 1, 3);
        outStoreGreaterDateSumAccount = addGProp("Сумма учетн. расх. со скл. после даты", outGreaterDateSumAccount, true, outSStore, 1, 3);
        dltStoreGreaterDateSumAccount = addUProp("Сумма учетн. на скл. после даты", Union.SUM, 2, 1, incStoreGreaterDateSumAccount, 1, 2, -1, outStoreGreaterDateSumAccount, 1, 2);

        incStoreSumAccount = addGProp("Сумма учетн. прих. на скл.", incSumAccount, true, incSStore, 1, 2);
        outStoreSumAccount = addGProp("Сумма учетн. расх. со скл.", outSumAccount, true, outSStore, 1, 2);
        balanceDocStoreArticleSumAccount = addUProp("Сумма товара на скл. (по док.)", Union.SUM, 2, 1, incStoreSumAccount, 1, 2, -1, outStoreSumAccount, 1, 2);

        balanceStoreArticleSumAccount = addJProp("Сумма товара на скл. (по ост.)", multiplyDouble2, 2, balanceStoreQuantity, 1, 2, storePriceOut, 1, 2);
        dltStoreArticleSumAccount = addUProp(accountGroup, "Отклонение суммы товара на скл.", Union.SUM, 2, 1, balanceDocStoreArticleSumAccount, 1, 2, -1, balanceStoreArticleSumAccount, 1, 2);

        balanceDocStoreSumAccount = addGProp(accountGroup, "Сумма на скл. (по док.)", balanceDocStoreArticleSumAccount, true, 1);
        balanceStoreSumAccount = addGProp(accountGroup, "Сумма на скл. (по ост.)", balanceStoreArticleSumAccount, true, 1);
        dltStoreSumAccount = addGProp(accountGroup, "Отклонение суммы на скл.", dltStoreArticleSumAccount, true, 1);

        balanceDocStoreDateMSumAccount = addUProp(accountGroup, "Сумма учетн. на начало", Union.SUM, 2, 1, addJProp("", balanceDocStoreSumAccount, 2, 1), 1, 2, -1, dltStoreGroeqDateSumAccount, 1, 2);
        balanceDocStoreDateESumAccount = addUProp(accountGroup, "Сумма учетн. на конец", Union.SUM, 2, 1, addJProp("", balanceDocStoreSumAccount, 2, 1), 1, 2, -1, dltStoreGreaterDateSumAccount, 1, 2);
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

    LJP docOutBalanceQuantity, docIncBalanceQuantity, docRevBalanceQuantity;

    private void initDocCurrentBalanceProperties() {

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

    private void initDocCurrentFixedProperties() {

        docCurPriceIn = addJProp("Цена пост. (тек.)", storePriceIn, 2, fixedStore, 1, 2);
        docCurVATIn = addJProp("НДС пост. (тек.)", storeVATIn, 2, fixedStore, 1, 2);
        docCurAdd = addJProp("Надбавка (тек.)", storeAdd, 2, fixedStore, 1, 2);
        docCurVATOut = addJProp("НДС прод. (тек.)", storeVATOut, 2, fixedStore, 1, 2);
        docCurLocTax = addJProp("Местн. нал. (тек.)", storeLocTax, 2, fixedStore, 1, 2);
        docCurPriceOut = addJProp("Цена розн. (тек.)", storePriceOut, 2, fixedStore, 1, 2);

        docOverPriceIn = addUProp(incPrmsGroup, "Цена пост.", Union.OVERRIDE, 2, 1, docCurPriceIn, 1, 2, 1, fixedPriceIn, 1, 2);
        docOverVATIn = addUProp(incPrmsGroup, "НДС пост.", Union.OVERRIDE, 2, 1, docCurVATIn, 1, 2, 1, fixedVATIn, 1, 2);
        docOverAdd = addUProp(outPrmsGroup, "Надбавка", Union.OVERRIDE, 2, 1, docCurAdd, 1, 2, 1, fixedAdd, 1, 2);
        docOverVATOut = addUProp(outPrmsGroup, "НДС прод.", Union.OVERRIDE, 2, 1, docCurVATOut, 1, 2, 1, fixedVATOut, 1, 2);
        docOverLocTax = addUProp(outPrmsGroup, "Местн. нал.", Union.OVERRIDE, 2, 1, docCurLocTax, 1, 2, 1, fixedLocTax, 1, 2);
        docOverPriceOut = addUProp(outPrmsGroup, "Цена розн.", Union.OVERRIDE, 2, 1, docCurPriceOut, 1, 2, 1, fixedPriceOut, 1, 2);
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

    private void initDocCurrentRevalProperties() {

        revalCurPriceIn = addJProp("Цена пост. (тек.)", storePriceIn, 2, revalStore, 1, 2);
        revalCurVATIn = addJProp("НДС пост. (тек.)", storeVATIn, 2, revalStore, 1, 2);
        revalCurAdd = addJProp("Надбавка (тек.)", storeAdd, 2, revalStore, 1, 2);
        revalCurVATOut = addJProp("НДС прод. (тек.)", storeVATOut, 2, revalStore, 1, 2);
        revalCurLocTax = addJProp("Местн. нал. (тек.)", storeLocTax, 2, revalStore, 1, 2);
        revalCurPriceOut = addJProp("Цена розн. (тек.)", storePriceOut, 2, revalStore, 1, 2);

        revalOverBalanceQuantity = addUProp(balanceGroup, "Остаток", Union.OVERRIDE, 2, 1, docRevBalanceQuantity, 1, 2, 1, revalBalanceQuantity, 1, 2);
        revalOverPriceIn = addUProp(incPrmsGroupBefore, "Цена пост.", Union.OVERRIDE, 2, 1, revalCurPriceIn, 1, 2, 1, revalPriceIn, 1, 2);
        revalOverVATIn = addUProp(incPrmsGroupBefore, "НДС пост.", Union.OVERRIDE, 2, 1, revalCurVATIn, 1, 2, 1, revalVATIn, 1, 2);
        revalOverAddBefore = addUProp(outPrmsGroupBefore, "Надбавка (до)", Union.OVERRIDE, 2, 1, revalCurAdd, 1, 2, 1, revalAddBefore, 1, 2);
        revalOverVATOutBefore = addUProp(outPrmsGroupBefore, "НДС прод. (до)", Union.OVERRIDE, 2, 1, revalCurVATOut, 1, 2, 1, revalVATOutBefore, 1, 2);
        revalOverLocTaxBefore = addUProp(outPrmsGroupBefore, "Местн. нал. (до)", Union.OVERRIDE, 2, 1, revalCurLocTax, 1, 2, 1, revalLocTaxBefore, 1, 2);
        revalOverPriceOutBefore = addUProp(outPrmsGroupBefore, "Цена розн. (до)", Union.OVERRIDE, 2, 1, revalCurPriceOut, 1, 2, 1, revalPriceOutBefore, 1, 2);
    }

    // ------------------------------------------------------------------------------------------------------- //
    // ------------------------------------- Конкретные классы товаров --------------------------------------- //
    // ------------------------------------------------------------------------------------------------------- //

    void initCustomArticleLogics() {

        // конкретные классы
        RemoteClass articleFood = addObjectClass("Продтовары", article);
        addDProp(baseGroup, "Срок годности", RemoteClass.string(10), articleFood);

        RemoteClass articleAlcohol = addObjectClass("Алкоголь", articleFood);
        addDProp(baseGroup, "Крепость", RemoteClass.integer, articleAlcohol);

        RemoteClass articleVodka = addObjectClass("Водка", articleAlcohol);
        addDProp(baseGroup, "Прейск.", RemoteClass.bit, articleVodka);

        RemoteClass articleBeer = addObjectClass("Пиво", articleAlcohol);
        addDProp(baseGroup, "Тип", RemoteClass.string(10), articleBeer);
        addDProp(baseGroup, "Упак.", RemoteClass.string(10), articleBeer);

        RemoteClass wineTaste = addObjectClass("Вкус вина", objectClass);
        RemoteClass articleWine = addObjectClass("Вино", articleAlcohol);
        addJProp(baseGroup, "Вкус", name, 1, addDProp("Код вкуса", wineTaste, articleWine), 1);

        RemoteClass articleMilkGroup = addObjectClass("Молочные продукты", articleFood);
        addDProp(baseGroup, "Жирн.", RemoteClass.doubleClass, articleMilkGroup);

        RemoteClass articleMilk = addObjectClass("Молоко", articleMilkGroup);
        addDProp(baseGroup, "Упак.", RemoteClass.string(10),  articleMilk);

        RemoteClass articleCheese = addObjectClass("Сыр", articleMilkGroup);
        addDProp(baseGroup, "Вес.", RemoteClass.bit, articleCheese);

        RemoteClass articleCurd = addObjectClass("Творог", articleMilkGroup);

        RemoteClass articleBreadGroup = addObjectClass("Хлебобулочные изделия", articleFood);
        addDProp(baseGroup, "Вес", RemoteClass.integer, articleBreadGroup);

        RemoteClass articleBread = addObjectClass("Хлеб", articleBreadGroup);
        addDProp(baseGroup, "Вес", RemoteClass.integer, articleBread);

        RemoteClass articleCookies = addObjectClass("Печенье", articleBreadGroup);

        RemoteClass articleJuice = addObjectClass("Соки", articleFood);
        addDProp(baseGroup, "Вкус", RemoteClass.string(10), articleJuice);
        addDProp(baseGroup, "Литраж", RemoteClass.integer, articleJuice);

        RemoteClass articleClothes = addObjectClass("Одежда", article);
        addDProp(baseGroup, "Модель", RemoteClass.string(10), articleClothes);

        RemoteClass shirtSize = addObjectClass("Размер майки", objectClass);
        RemoteClass articleTShirt = addObjectClass("Майки", articleClothes);
        addJProp(baseGroup, "Размер", name, 1, addDProp("Код размера", shirtSize, articleTShirt), 1);

        RemoteClass articleJeans = addObjectClass("Джинсы", articleClothes);
        addDProp(baseGroup, "Ширина", RemoteClass.integer, articleJeans);
        addDProp(baseGroup, "Длина", RemoteClass.integer, articleJeans);

        RemoteClass articleShooes = addObjectClass("Обувь", article);
        addDProp(baseGroup, "Цвет", RemoteClass.string(10), articleShooes);
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

//        persistents.add((AggregateProperty)docOutBalanceQuantity.property);
//        persistents.add((AggregateProperty)docIncBalanceQuantity.property);

        persistents.add((AggregateProperty)outQStore.property);
        persistents.add((AggregateProperty)incQStore.property);
    }

    protected void initTables() {

        TableImplement include;

        include = new TableImplement();
        include.add(new DataPropertyInterface(0,article));
        tableFactory.includeIntoGraph(include);

        include = new TableImplement();
        include.add(new DataPropertyInterface(0,store));
        tableFactory.includeIntoGraph(include);

        include = new TableImplement();
        include.add(new DataPropertyInterface(0,articleGroup));
        tableFactory.includeIntoGraph(include);

        include = new TableImplement();
        include.add(new DataPropertyInterface(0,article));
        include.add(new DataPropertyInterface(0,document));
        tableFactory.includeIntoGraph(include);

        include = new TableImplement();
        include.add(new DataPropertyInterface(0,article));
        include.add(new DataPropertyInterface(0,store));
        tableFactory.includeIntoGraph(include);

    }

    protected void initIndexes() {
        indexes.add(Collections.singletonList(extIncDate.property));
        indexes.add(Collections.singletonList(intraDate.property));
        indexes.add(Collections.singletonList(extOutDate.property));
        indexes.add(Collections.singletonList(exchDate.property));
        indexes.add(Collections.singletonList(revalDate.property));
        indexes.add(Collections.singletonList(maxChangesParamsDate.property));
        indexes.add(Collections.singletonList(docStore.property));
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

        createDefaultClassForms(objectClass, baseElement);

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
                                      extraProp.property.caption,
                                      KeyStroke.getKeyStroke(functionKey--, 0)));
            }
            addRegularFilterGroup(filterGroup);
        }
    }

    private class ExtIncDocumentNavigatorForm extends TmcNavigatorForm {

        protected ObjectImplement objDoc;

        ExtIncDocumentNavigatorForm(NavigatorElement parent, int iID, String caption, boolean isPrintForm) {
            super(parent, iID, caption, isPrintForm);

            objDoc = addSingleGroupObjectImplement(extIncomeDocument, "Документ", properties,
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

            objDetail = addSingleGroupObjectImplement(extIncomeDetail, "Строка", properties,
                                                                      artclGroup, quantGroup, incPrmsGroup, incSumsGroup, outPrmsGroup);

            PropertyObjectImplement detDocument = addPropertyObjectImplement(extIncDetailDocument, objDetail);
            addFixedFilter(new Filter(detDocument, Filter.EQUALS, new ObjectValueLink(objDoc)));
        }
    }

    private class ExtIncNavigatorForm extends ExtIncDocumentNavigatorForm {

        public ExtIncNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption, false);

            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArt, properties,
                    balanceGroup, extIncQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyView(extIncQuantity.property).view, 0);
        }
    }

    private class ExtIncPrintNavigatorForm extends ExtIncDetailNavigatorForm {

        public ExtIncPrintNavigatorForm(NavigatorElement parent, int ID, String caption) throws JRException, FileNotFoundException {
            super(parent, ID, caption, true);

            objDoc.groupTo.gridClassView = false;
            objDoc.groupTo.singleViewType = true;

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

            ObjectImplement objDoc = addSingleGroupObjectImplement(intraDocument, "Документ", properties,
                                                                        baseGroup, storeGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArt, properties,
                    balanceGroup, intraQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyView(intraQuantity.property).view, 0,
                                         getPropertyView(docOutBalanceQuantity.property).view,
                                         getPropertyView(docIncBalanceQuantity.property).view);

            addHintsNoUpdate(maxChangesParamsDoc.property);
        }
    }

    private class ExtOutNavigatorForm extends TmcNavigatorForm {

        public ExtOutNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(extOutcomeDocument, "Документ", properties,
                                                                        baseGroup, storeGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                    balanceGroup, extOutQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyView(extOutQuantity.property).view, 0,
                                         getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class CashSaleNavigatorForm extends TmcNavigatorForm {

        public CashSaleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(cashSaleDocument, "Документ", properties,
                                                                        baseGroup, storeGroup, outSumsGroup, accountGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                        balanceGroup, cashSaleQuantity, outSumsGroup, accountGroup);

            addArticleRegularFilterGroup(getPropertyView(cashSaleQuantity.property).view, 0,
                                         getPropertyView(docOutBalanceQuantity.property).view);

//            addPropertyView(objDoc, objArt, Properties, quantity, notZeroQuantity);
        }
    }

    private class ReceiptNavigatorForm extends TmcNavigatorForm {

        public ReceiptNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(cashSaleDocument, "Документ", properties,
                                                                        baseGroup, storeGroup, outSumsGroup, accountGroup);
            objDoc.groupTo.gridClassView = false;
            objDoc.groupTo.singleViewType = true;

            ObjectImplement objReceipt = addSingleGroupObjectImplement(receipt, "Чек", properties,
                                                                        baseGroup, outSumsGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                        balanceGroup);

            addPropertyView(objReceipt, objArt, properties,
                        receiptQuantity, incPrmsGroup, outPrmsGroup, outSumsGroup);

            addPropertyView(objDoc, objArt, properties,
                        accountGroup);

            addFixedFilter(new Filter(addPropertyObjectImplement(receiptSaleDocument, objReceipt), Filter.EQUALS, new ObjectValueLink(objDoc)));

            addArticleRegularFilterGroup(getPropertyView(receiptQuantity.property).view, 0,
                                         getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class ClearingSaleNavigatorForm extends TmcNavigatorForm {

        public ClearingSaleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(clearingSaleDocument, "Документ", properties,
                                                                        baseGroup, storeGroup, customerGroup, extOutDocumentSumPriceOut);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                        balanceGroup, clearingSaleQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyView(clearingSaleQuantity.property).view, 0,
                                         getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class InvNavigatorForm extends TmcNavigatorForm {

        public InvNavigatorForm(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectImplement objStore = null;
            if (groupStore) {
                objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                            baseGroup, accountGroup);
                objStore.groupTo.gridClassView = false;
                objStore.groupTo.singleViewType = true;
            }

            ObjectImplement objDoc = addSingleGroupObjectImplement(invDocument, "Документ", properties,
                                                                        baseGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                    balanceGroup, invBalance, invQuantity, incPrmsGroup, outPrmsGroup, accountGroup);

            addArticleRegularFilterGroup(getPropertyView(invQuantity.property).view, 0,
                                         getPropertyView(docOutBalanceQuantity.property).view);

            if (groupStore)
                addFixedFilter(new Filter(addPropertyObjectImplement(revalStore, objDoc), Filter.EQUALS, new ObjectValueLink(objStore)));
            else
                addPropertyView(properties, storeGroup, false, objDoc);
        }
    }

    private class ReturnNavigatorForm extends TmcNavigatorForm {

        public ReturnNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(returnDocument, "Документ", properties,
                                                                        baseGroup, storeGroup, supplierGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties,
                    balanceGroup, returnQuantity, incPrmsGroup, outPrmsGroup);

            addArticleRegularFilterGroup(getPropertyView(returnQuantity.property).view, 0,
                                         getPropertyView(docOutBalanceQuantity.property).view);
        }
    }

    private class ExchangeNavigatorForm extends TmcNavigatorForm {

        public ExchangeNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(exchangeDocument, "Документ", properties,
                                                                        baseGroup, storeGroup);
            ObjectImplement objArtTo = addSingleGroupObjectImplement(article, "Товар (на)", properties,
                                                                        baseGroup);
            ObjectImplement objArtFrom = addSingleGroupObjectImplement(article, "Товар (c)", properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArtTo, properties,
                    docOutBalanceQuantity, exchIncQuantity, exchOutQuantity, incPrmsGroup, outPrmsGroup);
            addPropertyView(docOutBalanceQuantity, objDoc, objArtFrom);
            addPropertyView(exchangeQuantity, objDoc, objArtFrom, objArtTo);
            addPropertyView(objDoc, objArtFrom, properties, incPrmsGroup, outPrmsGroup);

            RegularFilterGroup filterGroup = new RegularFilterGroup(IDShift(1));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(exchIncQuantity.property).view, Filter.NOT_EQUALS, new UserValueLink(0)),
                                  "Приход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(exchOutQuantity.property).view, Filter.NOT_EQUALS, new UserValueLink(0)),
                                  "Расход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.property, objArtTo.groupTo).view, Filter.NOT_EQUALS, new UserValueLink(0)),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.property, objArtTo.groupTo).view, Filter.LESS, new UserValueLink(0)),
                                  "Отр. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroup);

            filterGroup = new RegularFilterGroup(IDShift(1));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(exchangeQuantity.property).view, Filter.NOT_EQUALS, new UserValueLink(0)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.property, objArtFrom.groupTo).view, Filter.NOT_EQUALS, new UserValueLink(0)),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.property, objArtFrom.groupTo).view, Filter.GREATER, new UserValueLink(0)),
                                  "Пол. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOverPriceOut.property, objArtFrom.groupTo).view, Filter.EQUALS, new PropertyValueLink(getPropertyView(docOverPriceOut.property, objArtTo.groupTo).view)),
                                  "Одинаковая розн. цена",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);

        }
    }

    private class ExchangeMNavigatorForm extends TmcNavigatorForm {

        public ExchangeMNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(exchangeDocument, "Документ", properties,
                                                                        baseGroup, storeGroup);

            GroupObjectImplement gobjArts = new GroupObjectImplement(IDShift(1));

            ObjectImplement objArtTo = new ObjectImplement(IDShift(1), article, "Товар (на)", gobjArts);
            ObjectImplement objArtFrom = new ObjectImplement(IDShift(1), article, "Товар (с)", gobjArts);

            addGroup(gobjArts);

            addPropertyView(properties, baseGroup, false, objArtTo);
            addPropertyView(properties, baseGroup, false, objArtFrom);
            addPropertyView(exchangeQuantity, objDoc, objArtFrom, objArtTo);

            addFixedFilter(new Filter(getPropertyView(exchangeQuantity.property).view, Filter.NOT_EQUALS, new UserValueLink(0)));
        }
    }

    private class RevalueNavigatorForm extends TmcNavigatorForm {

        public RevalueNavigatorForm(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectImplement objStore = null;
            if (groupStore) {
                objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                            baseGroup, accountGroup);
                objStore.groupTo.gridClassView = false;
                objStore.groupTo.singleViewType = true;
            }

            ObjectImplement objDoc = addSingleGroupObjectImplement(revalDocument, "Документ", properties,
                                                                        baseGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);

            addPropertyView(objDoc, objArt, properties,
                    revalOverBalanceQuantity, isRevalued, incPrmsGroupBefore, outPrmsGroupBefore, outPrmsGroupAfter);

            addArticleRegularFilterGroup(getPropertyView(isRevalued.property).view, false,
                                         getPropertyView(revalOverBalanceQuantity.property).view);

            if (groupStore)
                addFixedFilter(new Filter(addPropertyObjectImplement(revalStore, objDoc), Filter.EQUALS, new ObjectValueLink(objStore)));
            else
                addPropertyView(properties, storeGroup, false, objDoc);
        }
    }

    private class StoreArticleNavigatorForm extends TmcNavigatorForm {

        ObjectImplement objStore, objArt;

        public StoreArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                        baseGroup, accountGroup);
            objStore.groupTo.gridClassView = false;
            objStore.groupTo.singleViewType = true;

            objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);

            addPropertyView(objStore, objArt, properties,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);
        }
    }

    private class StoreArticlePrimDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticlePrimDocNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objPrimDoc = addSingleGroupObjectImplement(primaryDocument, "Документ", properties,
                                                                                    baseGroup, paramsGroup);

            addPropertyView(objPrimDoc, objArt, properties,
                    paramsGroup);

            addFixedFilter(new Filter(getPropertyView(isDocArtChangesParams.property).view, Filter.NOT_EQUALS, new UserValueLink(false)));
            addFixedFilter(new Filter(getPropertyView(primDocStore.property).view, Filter.EQUALS, new ObjectValueLink(objStore)));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(getPropertyView(primDocDate.property), false);
            richDesign = formView;
        }
    }

    private class StoreArticleDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticleDocNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(document, "Документ", properties,
                                                                                    baseGroup, docDate, storeGroup, true, supplierGroup, true, customerGroup, true);

            addPropertyView(dltDocStoreQuantity, objDoc, objStore, objArt);

            addFixedFilter(new Filter(addPropertyObjectImplement(isDocStoreArtInclude, objDoc, objStore, objArt), Filter.EQUALS, new UserValueLink(true)));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(getPropertyView(docDate.property), false);
            richDesign = formView;
        }
    }

    private class ArticleStoreNavigatorForm extends TmcNavigatorForm {

        ObjectImplement objStore, objArt;

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

            GroupObjectImplement gobjArtStore = new GroupObjectImplement(IDShift(1));

            // добавить объект "Товар"
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArtStore);
            // добавить объект "Склад"
            ObjectImplement objStore = new ObjectImplement(IDShift(1), store, "Склад", gobjArtStore);

            // добавить блок Товар*Склад
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
            ObjectImplement objSupplier = addSingleGroupObjectImplement(supplier, "Поставщик", properties,
                                                                                    baseGroup);
            objSupplier.groupTo.gridClassView = false;
            objSupplier.groupTo.singleViewType = true;

            // создать блок "Склад"
            ObjectImplement objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                        baseGroup);
            objStore.groupTo.gridClassView = false;

            // создать блок "Товар"
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            // добавить множественные свойства
            addPropertyView(objStore, objArt, properties,
                    baseGroup, balanceGroup, incPrmsGroup, outPrmsGroup);

            // установить фильтр по умолчанию на поставщик товара = поставщик
            addFixedFilter(new Filter(addPropertyObjectImplement(storeSupplier, objStore, objArt), Filter.EQUALS, new ObjectValueLink(objSupplier)));

            // добавить стандартные фильтры
            RegularFilterGroup filterGroup = new RegularFilterGroup(IDShift(1));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(balanceStoreQuantity.property).view, Filter.GREATER, new UserValueLink(0)),
                                  "Есть на складе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(balanceStoreQuantity.property).view, Filter.LESS_EQUALS, new UserValueLink(0)),
                                  "Нет на складе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroup);
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

            objDateFrom = new ObjectImplement(IDShift(1), RemoteClass.date, "С даты :", gobjInterval);
            objDateTo = new ObjectImplement(IDShift(1), RemoteClass.date, "По дату :", gobjInterval);

            addGroup(gobjInterval);
        }
    }

    private class MainAccountNavigatorForm extends DateIntervalNavigatorForm {

        public MainAccountNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                        baseGroup);
            ObjectImplement objDoc = addSingleGroupObjectImplement(document, "Документ", properties,
                                                                                    baseGroup, docDate);

            addPropertyView(balanceDocStoreDateMSumAccount, objStore, objDateFrom);
            addPropertyView(balanceDocStoreDateESumAccount, objStore, objDateTo);

            addPropertyView(dltDocStoreSumAccount, objDoc, objStore);

            addFixedFilter(new Filter(getPropertyView(dltDocStoreSumAccount.property).view, Filter.NOT_EQUALS, new UserValueLink(0)));
            addFixedFilter(new Filter(getPropertyView(docDate.property).view, Filter.GREATER_EQUALS, new ObjectValueLink(objDateFrom)));
            addFixedFilter(new Filter(getPropertyView(docDate.property).view, Filter.LESS_EQUALS, new ObjectValueLink(objDateTo)));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(getPropertyView(docDate.property), true);
            richDesign = formView;

        }
    }


    private class SalesArticleStoreNavigatorForm extends DateIntervalNavigatorForm {

        public SalesArticleStoreNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objArticle = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup);
            ObjectImplement objStore = addSingleGroupObjectImplement(store, "Склад", properties,
                                                                        baseGroup);

            addPropertyView(saleArticleBetweenDateQuantity, objArticle, objDateFrom, objDateTo);

            addPropertyView(balanceStoreDateMQuantity, objStore, objArticle, objDateFrom);
            addPropertyView(incStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
            addPropertyView(outStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
            addPropertyView(balanceStoreDateEQuantity, objStore, objArticle, objDateTo);
            addPropertyView(saleStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
        }
    }

    protected void initAuthentication() {

        SecurityPolicy securityPolicy;

        User user1 = authPolicy.addUser("user1", "user1", new UserInfo("Петр", "Петров"));

        User user2 = authPolicy.addUser("user2", "user2", new UserInfo("Иван", "Иванов"));

        securityPolicy = new SecurityPolicy();

        securityPolicy.property.view.deny(extIncDocumentSumPay.property);
        securityPolicy.property.view.deny(incSumsGroup.getProperties());
        securityPolicy.property.change.deny(extIncDetailArticle.property);
        securityPolicy.property.change.deny(extIncDetailQuantity.property);

        securityPolicy.navigator.deny(analyticsData.getChildren(true));
        securityPolicy.navigator.deny(extIncPrintForm);

        securityPolicy.cls.edit.add.deny(document.getChildren(true));
        securityPolicy.cls.edit.remove.deny(baseGroup.getClasses());

        user2.addSecurityPolicy(securityPolicy);

    }

    // ------------------------------------- Временные методы --------------------------- //

    public void fillData(DataAdapter adapter) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        int modifier = 10;
        int propModifier = 1;

        Map<RemoteClass,Integer> classQuantity = new HashMap<RemoteClass, Integer>();
        List<RemoteClass> articleChilds = new ArrayList<RemoteClass>();
        article.fillChilds(articleChilds);
        for (RemoteClass articleClass : articleChilds)
            classQuantity.put(articleClass, modifier *10/articleChilds.size());

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

        Map<DataProperty, Set<DataPropertyInterface>> propNotNulls = new HashMap<DataProperty, Set<DataPropertyInterface>>();
        name.putNotNulls(propNotNulls,0);
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

        autoFillDB(adapter, classQuantity, propQuantity,propNotNulls);
    }

}
