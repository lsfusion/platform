package platformlocal;

import javax.swing.*;
import java.sql.SQLException;
import java.util.*;
import java.awt.event.KeyEvent;

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

    Class document;
    Class primaryDocument, secondaryDocument;
    Class paramsDocument;
    Class quantityDocument;
    Class incomeDocument;
    Class outcomeDocument;

    Class extIncomeDocument;
    Class extIncomeDetail;

    Class intraDocument;
    Class extOutcomeDocument;
    Class exchangeDocument;

    Class revalDocument;

    AbstractGroup baseGroup, artclGroup, artgrGroup, storeGroup, quantGroup, balanceGroup;
    AbstractGroup incPrmsGroup, incPrmsGroupBefore, incPrmsGroupAfter, incSumsGroup, outPrmsGroup, outPrmsGroupBefore, outPrmsGroupAfter;
    AbstractGroup paramsGroup;
    
    void InitGroups() {

        baseGroup = new AbstractGroup("Атрибуты");
        artclGroup = new AbstractGroup("Товар");
        artgrGroup = new AbstractGroup("Группа товаров");
        storeGroup = new AbstractGroup("Склад");
        quantGroup = new AbstractGroup("Количество");
        balanceGroup = new AbstractGroup("Остаток");
        incPrmsGroup = new AbstractGroup("Входные параметры");
        incPrmsGroupBefore = new AbstractGroup("До");
        incPrmsGroup.add(incPrmsGroupBefore);
        incPrmsGroupAfter = new AbstractGroup("После");
        incPrmsGroup.add(incPrmsGroupAfter);
        incSumsGroup = new AbstractGroup("Входные суммы");
        outPrmsGroup = new AbstractGroup("Выходные параметры");
        outPrmsGroupBefore = new AbstractGroup("До");
        outPrmsGroup.add(outPrmsGroupBefore);
        outPrmsGroupAfter = new AbstractGroup("После");
        outPrmsGroup.add(outPrmsGroupAfter);
        paramsGroup = new AbstractGroup("Измененные параметры");
    }

    void InitClasses() {

        article = new ObjectClass(baseGroup, 4, "Товар", objectClass);
        articleGroup = new ObjectClass(baseGroup, 5, "Группа товаров", objectClass);

        store = new ObjectClass(baseGroup, 6, "Склад", objectClass);

        document = new ObjectClass(baseGroup, 7, "Документ", objectClass);
        primaryDocument = new ObjectClass(baseGroup, 8, "Первичный документ", document);
        secondaryDocument = new ObjectClass(baseGroup, 9, "Непервичный документ", document);
        quantityDocument = new ObjectClass(baseGroup, 10, "Товарный документ", document);
        incomeDocument = new ObjectClass(baseGroup, 11, "Приходный документ", quantityDocument);
        outcomeDocument = new ObjectClass(baseGroup, 12, "Расходный документ", quantityDocument);
        paramsDocument = new ObjectClass(baseGroup, 13, "Порожденный документ", document);

        extIncomeDocument = new ObjectClass(baseGroup, 14, "Внешний приход", incomeDocument, primaryDocument);
        extIncomeDetail = new ObjectClass(baseGroup, 15, "Внешний приход (строки)", objectClass);

        intraDocument = new ObjectClass(baseGroup, 16, "Внутреннее перемещение", incomeDocument, outcomeDocument, primaryDocument, paramsDocument);
        extOutcomeDocument = new ObjectClass(baseGroup, 17, "Внешний расход", outcomeDocument, secondaryDocument, paramsDocument);
        exchangeDocument = new ObjectClass(baseGroup, 18, "Пересорт", incomeDocument, outcomeDocument, secondaryDocument, paramsDocument);

        revalDocument = new ObjectClass(19, "Переоценка", primaryDocument);
    }

    LDP name;
    LDP artGroup;
    LDP primDocDate, secDocDate;

    LDP extIncStore;
    LDP intraOutStore, intraIncStore;
    LDP extOutStore;
    LDP exchStore;
    LDP revalStore;

    LJP incStore, outStore;
    LJP primDocStore, paramsStore;
    LJP docStore;

    LJP artGroupName;
    LJP docStoreName;
    LJP intraStoreName;
    LJP extIncDetailArticleName;

    LDP extIncDetailDocument, extIncDetailArticle, extIncDetailQuantity;
    LGP extIncQuantity;
    LDP intraQuantity;
    LDP extOutQuantity;
    LDP exchangeQuantity;
    LGP exchIncQuantity, exchOutQuantity;

    LJP incQuantity, outQuantity;
    LJP quantity, notZeroIncPrmsQuantity;
    LGP incStoreQuantity, outStoreQuantity;
    LJP balanceStoreQuantity;

    LJP docDate;

    LDP extIncDetailPriceIn, extIncDetailVATIn;
    LJP extIncDetailCalcSum;
    LJP extIncDetailCalcSumVATIn, extIncDetailCalcSumPay;
    LDP extIncDetailSumVATIn, extIncDetailSumPay;

    LGP extIncDocumentSumVATIn, extIncDocumentSumPay;

    LDP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
    LJP extIncDetailCalcPriceOut;
    LDP extIncDetailPriceOut;

    LDP isRevalued;
    LDP revalPriceIn, revalVATIn;
    LDP revalAddBefore, revalVATOutBefore, revalLocTaxBefore;
    LDP revalPriceOutBefore;
    LDP revalAddAfter, revalVATOutAfter, revalLocTaxAfter;
    LDP revalPriceOutAfter;

    LJP changesParams;
    LGP maxChangesParamsDate;
    LGP maxChangesParamsDoc;

    LDP paramsPriceIn, paramsVATIn;
    LDP paramsAdd, paramsVATOut, paramsLocTax;
    LDP paramsPriceOut;

    LGP extIncLastDetail;

    LJP extIncPriceIn, extIncVATIn;
    LJP extIncAdd, extIncVATOut, extIncLocTax;
    LJP extIncPriceOut;

    LJP primDocPriceIn, primDocVATIn;
    LJP primDocAdd, primDocVATOut, primDocLocTax;
    LJP primDocPriceOut;

    LJP storePriceIn, storeVATIn;
    LJP storeAdd, storeVATOut, storeLocTax;
    LJP storePriceOut;

    LJP docOutBalanceQuantity, docIncBalanceQuantity;

    LJP docCurPriceIn, docCurVATIn;
    LJP docCurAdd, docCurVATOut, docCurLocTax;
    LJP docCurPriceOut;

    LJP docOverPriceIn, docOverVATIn;
    LJP docOverAdd, docOverVATOut, docOverLocTax;
    LJP docOverPriceOut;

    LJP revalCurPriceIn, revalCurVATIn;
    LJP revalCurAdd, revalCurVATOut, revalCurLocTax;
    LJP revalCurPriceOut;

    LJP revalOverPriceIn, revalOverVATIn;
    LJP revalOverAddBefore, revalOverVATOutBefore, revalOverLocTaxBefore;
    LJP revalOverPriceOutBefore;

    void InitProperties() {

        // -------------------------- Data Properties ---------------------- //

        name = AddDProp(baseGroup, "Имя", Class.stringClass, objectClass);

        artGroup = AddDProp(artgrGroup, "Гр. тов.", articleGroup, article);

        // -------------------------- Склады ---------------------- //

        extIncStore = AddDProp(storeGroup, "Склад", store, extIncomeDocument);
        intraOutStore = AddDProp(storeGroup, "Склад отпр.", store, intraDocument);
        intraIncStore = AddDProp(storeGroup, "Склад назн.", store, intraDocument);
        extOutStore = AddDProp(storeGroup, "Склад", store, extOutcomeDocument);
        exchStore = AddDProp(storeGroup, "Склад", store, exchangeDocument);
        revalStore = AddDProp(storeGroup, "Склад", store, revalDocument);

        incStore = AddUProp("Склад прих.", 2, 1, 1, extIncStore, 1, 1, intraIncStore, 1, 1, exchStore, 1);
        outStore = AddUProp("Склад расх.", 2, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1);
        LP primDocStoreNull = AddCProp("абст. склад", null, store, primaryDocument);
        primDocStore = AddUProp("Склад (изм.)", 2, 1, 1, primDocStoreNull, 1, 1, extIncStore, 1, 1, intraIncStore, 1, 1, revalStore, 1);
        paramsStore = AddUProp("Склад (парам.)", 2, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1);
        docStore = AddUProp("Склад", 2, 1, 1, extIncStore, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1, 1, revalStore, 1);

        extIncDetailDocument = AddDProp(null, "Документ", extIncomeDocument, extIncomeDetail);
        extIncDetailArticle = AddDProp(artclGroup, "Товар", article, extIncomeDetail);

        // -------------------------- Relation Properties ------------------ //

        artGroupName = AddJProp(artgrGroup, "Имя гр. тов.", name, 1, artGroup, 1);
        docStoreName = AddJProp(storeGroup, "Имя склада", name, 1, docStore, 1);
        intraStoreName = AddJProp(storeGroup, "Имя склада (назн.)", name, 1, intraIncStore, 1);

        extIncDetailArticleName = AddJProp(artclGroup, "Имя товара", name, 1, extIncDetailArticle, 1);

        // -------------------------- Движение товара по количествам ---------------------- //

        extIncDetailQuantity = AddDProp(quantGroup, "Кол-во", Class.doubleClass, extIncomeDetail);

//        extIncQuantity = AddDProp(quantGroup, "Кол-во прих.", Class.doubleClass, extIncomeDocument, article);
        extIncQuantity = AddGProp(quantGroup, "Кол-во прих.", extIncDetailQuantity, true, extIncDetailDocument, 1, extIncDetailArticle, 1);

        intraQuantity = AddDProp(quantGroup, "Кол-во внутр.", Class.doubleClass, intraDocument, article);

        extOutQuantity = AddDProp(quantGroup, "Кол-во расх.", Class.doubleClass, extOutcomeDocument, article);

        exchangeQuantity = AddDProp(quantGroup, "Кол-во перес.", Class.doubleClass, exchangeDocument, article, article);

        exchIncQuantity = AddGProp("Прих. перес.", exchangeQuantity, true, 1, 3);
        exchOutQuantity = AddGProp("Расх. перес.", exchangeQuantity, true, 1, 2);

        LP docIncQuantity = AddCProp("абст. кол-во", null, Class.doubleClass, incomeDocument, article);
        incQuantity = AddUProp("Кол-во прих.", 2, 2, 1, docIncQuantity, 1, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchIncQuantity, 1, 2);
        LP docOutQuantity = AddCProp("абст. кол-во", null, Class.doubleClass, outcomeDocument, article);
        outQuantity = AddUProp("Кол-во расх.", 2, 2, 1, docOutQuantity, 1, 2, 1, extOutQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchOutQuantity, 1, 2);

        LP docQuantity = AddCProp("абст. кол-во", null, Class.doubleClass, document, article);
        quantity = AddUProp("Кол-во", 2, 2, 1, docQuantity, 1, 2, 1, incQuantity, 1, 2, 1, outQuantity, 1, 2);

        LP incPrmsQuantity = AddUProp("Кол-во прих. (парам.)", 2, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2);
        LSFP notZero = AddWSFProp("((prm1)<>0)",Class.integralClass);
        notZeroIncPrmsQuantity = AddJProp("Есть в док.", notZero, 2, incPrmsQuantity, 1, 2);

        incStoreQuantity = AddGProp(quantGroup, "Прих. на скл.", incQuantity, true, incStore, 1, 2);
        outStoreQuantity = AddGProp(quantGroup, "Расх. со скл.", outQuantity, true, outStore, 1, 2);

        balanceStoreQuantity = AddUProp(quantGroup, "Ост. на скл.", 1, 2, 1, incStoreQuantity, 1, 2, -1, outStoreQuantity, 1, 2);
//        OstArtStore = AddUProp("остаток по складу",1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);

        // -------------------------- Входные параметры ---------------------------- //

        primDocDate = AddDProp(baseGroup, "Дата", Class.dateClass, primaryDocument);
        secDocDate = AddDProp(baseGroup, "Дата", Class.dateClass, secondaryDocument);

        docDate = AddUProp("Дата", 2, 1, 1, secDocDate, 1, 1, primDocDate, 1);

        extIncDetailPriceIn = AddDProp(incPrmsGroup, "Цена пост.", Class.doubleClass, extIncomeDetail);
        extIncDetailVATIn = AddDProp(incPrmsGroup, "НДС пост.", Class.doubleClass, extIncomeDetail);

        // -------------------------- Входные суммы ---------------------------- //

        LMFP multiplyDoubleDouble = AddMFProp(Class.doubleClass, Class.doubleClass);
        extIncDetailCalcSum = AddJProp(incSumsGroup, "Сумма пост.", multiplyDoubleDouble, 1, extIncDetailQuantity, 1, extIncDetailPriceIn, 1);

        LSFP percent = AddSFProp("((prm1*prm2)/100)", Class.doubleClass, Class.doubleClass);
        LSFP round = AddSFProp("round(prm1)", Class.doubleClass);

        extIncDetailCalcSumVATIn = AddJProp("Сумма НДС (расч.)", round, 1,
                                   AddJProp("Сумма НДС (расч. - неокр.)", percent, 1, extIncDetailCalcSum, 1, extIncDetailVATIn, 1), 1);

        extIncDetailSumVATIn = AddDProp(incSumsGroup, "Сумма НДС", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailSumVATIn, extIncDetailCalcSumVATIn, true);

        extIncDetailCalcSumPay = AddUProp("Всего с НДС (расч.)", 1, 1, 1, extIncDetailCalcSum, 1, 1, extIncDetailSumVATIn, 1);

        extIncDetailSumPay = AddDProp(incSumsGroup, "Всего с НДС", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailSumPay, extIncDetailCalcSumPay, true);

        extIncDocumentSumVATIn = AddGProp(incSumsGroup, "Сумма НДС", extIncDetailSumVATIn, true, extIncDetailDocument, 1);
        extIncDocumentSumPay = AddGProp(incSumsGroup, "Всего с НДС", extIncDetailSumPay, true, extIncDetailDocument, 1);

        // -------------------------- Выходные параметры ---------------------------- //

        extIncDetailAdd = AddDProp(outPrmsGroup, "Надбавка", Class.doubleClass, extIncomeDetail);
        extIncDetailVATOut = AddDProp(outPrmsGroup, "НДС прод.", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailVATOut, extIncDetailVATIn, true);
        extIncDetailLocTax = AddDProp(outPrmsGroup, "Местн. нал.", Class.doubleClass, extIncomeDetail);

        LSFP addPercent = AddSFProp("((prm1*(100+prm2))/100)", Class.doubleClass, Class.doubleClass);
        extIncDetailCalcPriceOut = AddJProp("Цена розн. (расч.)", round, 1,
                                   AddJProp("Цена розн. (расч. - неокр.)", addPercent, 1,
                                   AddJProp("Цена с НДС", addPercent, 1,
                                   AddJProp("Цена с надбавкой", addPercent, 1,
                                           extIncDetailPriceIn, 1,
                                           extIncDetailAdd, 1), 1,
                                           extIncDetailVATOut, 1), 1,
                                           extIncDetailLocTax, 1), 1);

        extIncDetailPriceOut = AddDProp(outPrmsGroup, "Цена розн.", Class.doubleClass, extIncomeDetail);
        setDefProp(extIncDetailPriceOut, extIncDetailCalcPriceOut, true);

        // ------------------------- Фиксирующиеся параметры товара ------------------------- //

        paramsPriceIn = AddDProp("Цена пост.", Class.doubleClass, paramsDocument, article);
        paramsVATIn = AddDProp("НДС пост.", Class.doubleClass, paramsDocument, article);
        paramsAdd = AddDProp("Надбавка", Class.doubleClass, paramsDocument, article);
        paramsVATOut = AddDProp("НДС прод.", Class.doubleClass, paramsDocument, article);
        paramsLocTax = AddDProp("Местн. нал.", Class.doubleClass, paramsDocument, article);
        paramsPriceOut = AddDProp("Цена розн.", Class.doubleClass, paramsDocument, article);

        // ------------------------------ Переоценка -------------------------------- //

        isRevalued = AddDProp("Переоц.", Class.bitClass, revalDocument, article);

        revalPriceIn = AddDProp("Цена пост.", Class.doubleClass, revalDocument, article);
        revalVATIn = AddDProp("НДС пост.", Class.doubleClass, revalDocument, article);
        revalAddBefore = AddDProp("Надбавка (до)", Class.doubleClass, revalDocument, article);
        revalVATOutBefore = AddDProp("НДС прод. (до)", Class.doubleClass, revalDocument, article);
        revalLocTaxBefore = AddDProp("Местн. нал. (до)", Class.doubleClass, revalDocument, article);
        revalPriceOutBefore = AddDProp("Цена розн. (до)", Class.doubleClass, revalDocument, article);
        revalAddAfter = AddDProp(outPrmsGroupAfter, "Надбавка (после)", Class.doubleClass, revalDocument, article);
        revalVATOutAfter = AddDProp(outPrmsGroupAfter, "НДС прод. (после)", Class.doubleClass, revalDocument, article);
        revalLocTaxAfter = AddDProp(outPrmsGroupAfter, "Местн. нал. (после)", Class.doubleClass, revalDocument, article);
        revalPriceOutAfter = AddDProp(outPrmsGroupAfter, "Цена розн. (после)", Class.doubleClass, revalDocument, article);

        // -------------------------- Последний документ ---------------------------- //

        LP primDocArtBitNull = AddCProp("абст. бит", null, Class.bitClass, primaryDocument, article);
        changesParams = AddUProp("Изм. парам.", 2, 2, 1, primDocArtBitNull, 1, 2, 1, isRevalued, 1, 2, 1, notZeroIncPrmsQuantity, 1, 2);
        LMFP multiplyBitDate = AddMFProp(Class.bitClass, Class.dateClass);
        LJP changesParamsDate = AddJProp("Дата изм. пар.", multiplyBitDate, 2, changesParams, 1, 2, primDocDate, 1);
        maxChangesParamsDate = AddGProp(baseGroup, "Посл. дата изм. парам.", changesParamsDate, false, primDocStore, 1, 2);

        LSFP equalsDD = AddWSFProp("((prm1)=(prm2)) AND ((prm3)=(prm4))", Class.dateClass, Class.dateClass, store, store);
        LJP primDocIsCor = AddJProp("Док. макс.", equalsDD, 3, primDocDate, 1, maxChangesParamsDate, 2, 3, primDocStore, 1, 2);

        LMFP multiplyBitBit = AddMFProp(Class.bitClass, Class.bitClass);
        LJP primDocIsLast = AddJProp("Посл.", multiplyBitBit, 3, primDocIsCor, 1, 2, 3, changesParams, 1, 3);

        LMFP multiplyBitPrimDoc = AddMFProp(Class.bitClass, primaryDocument);
        LJP primDocSelfLast = AddJProp("Тов. док. максю", multiplyBitPrimDoc, 3, primDocIsLast, 1, 2, 3, 1);
        maxChangesParamsDoc = AddGProp(baseGroup, "Посл. док. изм. парам.", primDocSelfLast, false, 2, 3);

        // ------------------------- Параметры по приходу --------------------------- //

        LP bitExtInc = AddCProp("Бит", true, Class.bitClass, extIncomeDetail);
        LMFP multiplyBitDetail = AddMFProp(Class.bitClass, extIncomeDetail);
        LJP propDetail = AddJProp("", multiplyBitDetail, 1, bitExtInc, 1, 1);
        extIncLastDetail = AddGProp("Посл. строка", propDetail, false, extIncDetailDocument, 1, extIncDetailArticle, 1);

        extIncPriceIn = AddJProp(incPrmsGroup, "Цена пост. (прих.)", extIncDetailPriceIn, 2, extIncLastDetail, 1, 2);
        extIncVATIn = AddJProp(incPrmsGroup, "НДС пост. (прих.)", extIncDetailVATIn, 2, extIncLastDetail, 1, 2);
        extIncAdd = AddJProp(outPrmsGroup, "Надбавка (прих.)", extIncDetailAdd, 2, extIncLastDetail, 1, 2);
        extIncVATOut = AddJProp(outPrmsGroup, "НДС прод. (прих.)", extIncDetailVATOut, 2, extIncLastDetail, 1, 2);
        extIncLocTax = AddJProp(outPrmsGroup, "Местн. нал. (прих.)", extIncDetailLocTax, 2, extIncLastDetail, 1, 2);
        extIncPriceOut = AddJProp(outPrmsGroup, "Цена розн. (прих.)", extIncDetailPriceOut, 2, extIncLastDetail, 1, 2);

        // ------------------------- Перегруженные параметры ------------------------ //

        LP nullPrimDocArt = AddCProp("null", null, Class.doubleClass, primaryDocument, article);

        primDocPriceIn = AddUProp(paramsGroup, "Цена пост. (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsPriceIn, 1, 2, 1, extIncPriceIn, 1, 2, 1, revalPriceIn, 1, 2);
        primDocVATIn = AddUProp(paramsGroup, "НДС пост. (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsVATIn, 1, 2, 1, extIncVATIn, 1, 2, 1, revalVATIn, 1, 2);
        primDocAdd = AddUProp(paramsGroup, "Надбавка (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsAdd, 1, 2, 1, extIncAdd, 1, 2, 1, revalAddAfter, 1, 2);
        primDocVATOut = AddUProp(paramsGroup, "НДС прод. (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsVATOut, 1, 2, 1, extIncVATOut, 1, 2, 1, revalVATOutAfter, 1, 2);
        primDocLocTax = AddUProp(paramsGroup, "Местн. нал. (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsLocTax, 1, 2, 1, extIncLocTax, 1, 2, 1, revalLocTaxAfter, 1, 2);
        primDocPriceOut = AddUProp(paramsGroup, "Цена розн. (изм.)", 2, 2, 1, nullPrimDocArt, 1, 2, 1, paramsPriceOut, 1, 2, 1, extIncPriceOut, 1, 2, 1, revalPriceOutAfter, 1, 2);

        storePriceIn = AddJProp(incPrmsGroup, "Цена пост. (тек.)", primDocPriceIn, 2, maxChangesParamsDoc, 1, 2, 2);
        storeVATIn = AddJProp(incPrmsGroup, "НДС пост. (тек.)", primDocVATIn, 2, maxChangesParamsDoc, 1, 2, 2);
        storeAdd = AddJProp(outPrmsGroup, "Надбавка (тек.)", primDocAdd, 2, maxChangesParamsDoc, 1, 2, 2);
        storeVATOut = AddJProp(outPrmsGroup, "НДС прод. (тек.)", primDocVATOut, 2, maxChangesParamsDoc, 1, 2, 2);
        storeLocTax = AddJProp(outPrmsGroup, "Местн. нал. (тек.)", primDocLocTax, 2, maxChangesParamsDoc, 1, 2, 2);
        storePriceOut = AddJProp(outPrmsGroup, "Цена розн. (тек.)", primDocPriceOut, 2, maxChangesParamsDoc, 1, 2, 2);

        docOutBalanceQuantity = AddJProp(balanceGroup, "Остаток (расх.)", balanceStoreQuantity, 2, outStore, 1, 2);
        docIncBalanceQuantity = AddJProp(balanceGroup, "Остаток (прих.)", balanceStoreQuantity, 2, incStore, 1, 2);

        docCurPriceIn = AddJProp("Цена пост. (тек.)", storePriceIn, 2, paramsStore, 1, 2);
        docCurVATIn = AddJProp("НДС пост. (тек.)", storeVATIn, 2, paramsStore, 1, 2);
        docCurAdd = AddJProp("Надбавка (тек.)", storeAdd, 2, paramsStore, 1, 2);
        docCurVATOut = AddJProp("НДС прод. (тек.)", storeVATOut, 2, paramsStore, 1, 2);
        docCurLocTax = AddJProp("Местн. нал. (тек.)", storeLocTax, 2, paramsStore, 1, 2);
        docCurPriceOut = AddJProp("Цена розн. (тек.)", storePriceOut, 2, paramsStore, 1, 2);

        docOverPriceIn = AddUProp(incPrmsGroup, "Цена пост.", 2, 2, 1, docCurPriceIn, 1, 2, 1, paramsPriceIn, 1, 2);
        docOverVATIn = AddUProp(incPrmsGroup, "НДС пост.", 2, 2, 1, docCurVATIn, 1, 2, 1, paramsVATIn, 1, 2);
        docOverAdd = AddUProp(outPrmsGroup, "Надбавка", 2, 2, 1, docCurAdd, 1, 2, 1, paramsAdd, 1, 2);
        docOverVATOut = AddUProp(outPrmsGroup, "НДС прод.", 2, 2, 1, docCurVATOut, 1, 2, 1, paramsVATOut, 1, 2);
        docOverLocTax = AddUProp(outPrmsGroup, "Местн. нал.", 2, 2, 1, docCurLocTax, 1, 2, 1, paramsLocTax, 1, 2);
        docOverPriceOut = AddUProp(outPrmsGroup, "Цена розн.", 2, 2, 1, docCurPriceOut, 1, 2, 1, paramsPriceOut, 1, 2);

        revalCurPriceIn = AddJProp("Цена пост. (тек.)", storePriceIn, 2, revalStore, 1, 2);
        revalCurVATIn = AddJProp("НДС пост. (тек.)", storeVATIn, 2, revalStore, 1, 2);
        revalCurAdd = AddJProp("Надбавка (тек.)", storeAdd, 2, revalStore, 1, 2);
        revalCurVATOut = AddJProp("НДС прод. (тек.)", storeVATOut, 2, revalStore, 1, 2);
        revalCurLocTax = AddJProp("Местн. нал. (тек.)", storeLocTax, 2, revalStore, 1, 2);
        revalCurPriceOut = AddJProp("Цена розн. (тек.)", storePriceOut, 2, revalStore, 1, 2);

        revalOverPriceIn = AddUProp(incPrmsGroupBefore, "Цена пост.", 2, 2, 1, revalCurPriceIn, 1, 2, 1, revalPriceIn, 1, 2);
        revalOverVATIn = AddUProp(incPrmsGroupBefore, "НДС пост.", 2, 2, 1, revalCurVATIn, 1, 2, 1, revalVATIn, 1, 2);
        revalOverAddBefore = AddUProp(outPrmsGroupBefore, "Надбавка (до)", 2, 2, 1, revalCurAdd, 1, 2, 1, revalAddBefore, 1, 2);
        revalOverVATOutBefore = AddUProp(outPrmsGroupBefore, "НДС прод. (до)", 2, 2, 1, revalCurVATOut, 1, 2, 1, revalVATOutBefore, 1, 2);
        revalOverLocTaxBefore = AddUProp(outPrmsGroupBefore, "Местн. нал. (до)", 2, 2, 1, revalCurLocTax, 1, 2, 1, revalLocTaxBefore, 1, 2);
        revalOverPriceOutBefore = AddUProp(outPrmsGroupBefore, "Цена розн. (до)", 2, 2, 1, revalCurPriceOut, 1, 2, 1, revalPriceOutBefore, 1, 2);

        LMFP multiplyBitDouble = AddMFProp(Class.bitClass, Class.doubleClass);
        LJP docCurQPriceIn = AddJProp("", multiplyBitDouble, 2, notZeroIncPrmsQuantity, 1, 2, docCurPriceIn, 1, 2);
        LJP docCurQVATIn = AddJProp("", multiplyBitDouble, 2, notZeroIncPrmsQuantity, 1, 2, docCurVATIn, 1, 2);
        LJP docCurQAdd = AddJProp("", multiplyBitDouble, 2, notZeroIncPrmsQuantity, 1, 2, docCurAdd, 1, 2);
        LJP docCurQVATOut = AddJProp("", multiplyBitDouble, 2, notZeroIncPrmsQuantity, 1, 2, docCurVATOut, 1, 2);
        LJP docCurQLocTax = AddJProp("", multiplyBitDouble, 2, notZeroIncPrmsQuantity, 1, 2, docCurLocTax, 1, 2);
        LJP docCurQPriceOut = AddJProp("", multiplyBitDouble, 2, notZeroIncPrmsQuantity, 1, 2, docCurPriceOut, 1, 2);

/*        setDefProp(paramsPriceIn, docCurQPriceIn, true);
        setDefProp(paramsVATIn, docCurQVATIn, true);
        setDefProp(paramsAdd, docCurQAdd, true);
        setDefProp(paramsVATOut, docCurQVATOut, true);
        setDefProp(paramsLocTax, docCurQLocTax, true);
        setDefProp(paramsPriceOut, docCurQPriceOut, true); */
    }

    void InitConstraints() {
    }

    void InitPersistents() {

        Persistents.add((AggregateProperty)incStoreQuantity.Property);
        Persistents.add((AggregateProperty)outStoreQuantity.Property);
        Persistents.add((AggregateProperty)maxChangesParamsDate.Property);
        Persistents.add((AggregateProperty)maxChangesParamsDoc.Property);

        Persistents.add((AggregateProperty)extIncLastDetail.Property);

/*        Persistents.add((AggregateProperty)storePriceIn.Property);
        Persistents.add((AggregateProperty)storeVATIn.Property);
        Persistents.add((AggregateProperty)storeAdd.Property);
        Persistents.add((AggregateProperty)storeVATOut.Property);
        Persistents.add((AggregateProperty)storeLocTax.Property);
        Persistents.add((AggregateProperty)storePriceOut.Property); */
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

/*        index = new ArrayList();
        index.add(primDocDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(maxChangesParamsDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(docStore.Property);
        Indexes.add(index);*/
    }

    void InitNavigators() {

        createDefaultClassForms(objectClass, baseElement);

        NavigatorForm extIncDetailForm = new ExtIncDetailNavigatorForm(10, "Внешний приход");
        baseElement.addChild(extIncDetailForm);

        NavigatorForm extIncForm = new ExtIncNavigatorForm(15, "Внешний приход по товарам");
        extIncDetailForm.addChild(extIncForm);

        NavigatorForm intraForm = new IntraNavigatorForm(20, "Внутреннее перемещение");
        baseElement.addChild(intraForm);

        NavigatorForm extOutForm = new ExtOutNavigatorForm(30, "Внешний расход");
        baseElement.addChild(extOutForm);

        NavigatorForm exchangeForm = new ExchangeNavigatorForm(40, "Пересорт");
        baseElement.addChild(exchangeForm);

        NavigatorForm revalueForm = new RevalueNavigatorForm(45, "Переоценка");
        baseElement.addChild(revalueForm);

        NavigatorForm storeArticleForm = new StoreArticleNavigatorForm(50, "Товары по складам");
        baseElement.addChild(storeArticleForm);

        NavigatorForm storeArticlePrimDocForm = new StoreArticlePrimDocNavigatorForm(55, "Товары по складам (изм. цен)");
        storeArticleForm.addChild(storeArticlePrimDocForm);
    }

    private class ExtIncDetailNavigatorForm extends NavigatorForm {

        public ExtIncDetailNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjDetail = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extIncomeDocument, "Документ", gobjDoc);
            ObjectImplement objDetail = new ObjectImplement(IDShift(1), extIncomeDetail, "Строка", gobjDetail);

            addGroup(gobjDoc);
            addGroup(gobjDetail);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, incSumsGroup, objDoc);
            addPropertyView(this, artclGroup, objDetail);
            addPropertyView(this, quantGroup, objDetail);
            addPropertyView(this, incPrmsGroup, objDetail);
            addPropertyView(this, incSumsGroup, objDetail);
            addPropertyView(this, outPrmsGroup, objDetail);

            PropertyObjectImplement detDocument = addPropertyObjectImplement(extIncDetailDocument, objDetail);
            addFixedFilter(new Filter(detDocument, FieldExprCompareWhere.EQUALS, new ObjectValueLink(objDoc)));
        }
    }

    private class ExtIncNavigatorForm extends NavigatorForm {

        public ExtIncNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extIncomeDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
//            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, balanceGroup, objDoc, objArt);
            addPropertyView(this, extIncQuantity, objDoc, objArt);
            addPropertyView(this, incPrmsGroup, objDoc, objArt);
            addPropertyView(this, outPrmsGroup, objDoc, objArt);
        }
    }

    private class IntraNavigatorForm extends NavigatorForm {

        public IntraNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), intraDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
//            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, balanceGroup, objDoc, objArt);
            addPropertyView(this, intraQuantity, objDoc, objArt);
            addPropertyView(this, incPrmsGroup, objDoc, objArt);
            addPropertyView(this, outPrmsGroup, objDoc, objArt);

        }
    }

    private class ExtOutNavigatorForm extends NavigatorForm {

        public ExtOutNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extOutcomeDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
//            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, balanceGroup, objDoc, objArt);
            addPropertyView(this, extOutQuantity, objDoc, objArt);
            addPropertyView(this, incPrmsGroup, objDoc, objArt);
            addPropertyView(this, outPrmsGroup, objDoc, objArt);

            RegularFilterGroup filterGroup = new RegularFilterGroup(IDShift(1));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(this, docOutBalanceQuantity.Property).View, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(0)),
                                  "Остатки",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(this, extOutQuantity.Property).View, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(0)),
                                  "Расходуемые позиции",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class ExchangeNavigatorForm extends NavigatorForm {

        public ExchangeNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArtFrom = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArtTo = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), exchangeDocument, "Документ", gobjDoc);
            ObjectImplement objArtFrom = new ObjectImplement(IDShift(1), article, "Товар (с)", gobjArtFrom);
            ObjectImplement objArtTo = new ObjectImplement(IDShift(1), article, "Товар (на)", gobjArtTo);

            addGroup(gobjDoc);
            addGroup(gobjArtFrom);
            addGroup(gobjArtTo);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArtFrom);
//            addPropertyView(this, artgrGroup, objArtFrom);
            addPropertyView(this, baseGroup, objArtTo);
//            addPropertyView(this, artgrGroup, objArtTo);
            addPropertyView(this, docOutBalanceQuantity, objDoc, objArtFrom);
            addPropertyView(this, exchIncQuantity, objDoc, objArtFrom);
            addPropertyView(this, exchOutQuantity, objDoc, objArtFrom);
            addPropertyView(this, incPrmsGroup, objDoc, objArtFrom);
            addPropertyView(this, outPrmsGroup, objDoc, objArtFrom);
            addPropertyView(this, docOutBalanceQuantity, objDoc, objArtTo);
            addPropertyView(this, exchangeQuantity, objDoc, objArtFrom, objArtTo);
            addPropertyView(this, incPrmsGroup, objDoc, objArtTo);
            addPropertyView(this, outPrmsGroup, objDoc, objArtTo);
        }
    }

    private class RevalueNavigatorForm extends NavigatorForm {

        public RevalueNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), revalDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
//            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, isRevalued, objDoc, objArt);
            addPropertyView(this, incPrmsGroupBefore, objDoc, objArt);
            addPropertyView(this, outPrmsGroupBefore, objDoc, objArt);
            addPropertyView(this, outPrmsGroupAfter, objDoc, objArt);
        }
    }

    private class StoreArticleNavigatorForm extends NavigatorForm {

        GroupObjectImplement gobjStore, gobjArt;
        ObjectImplement objStore, objArt;

        public StoreArticleNavigatorForm(int ID, String caption) {
            super(ID, caption);

            gobjStore = new GroupObjectImplement(IDShift(1));
            gobjArt = new GroupObjectImplement(IDShift(1));

            objStore = new ObjectImplement(IDShift(1), store, "Склад", gobjStore);
            objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjStore);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objStore);
            addPropertyView(this, baseGroup, objArt);
//            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, objStore, objArt);
        }
    }

    private class StoreArticlePrimDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticlePrimDocNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjPrimDoc = new GroupObjectImplement(IDShift(1));

            ObjectImplement objPrimDoc = new ObjectImplement(IDShift(1), primaryDocument, "Документ", gobjPrimDoc);

            addGroup(gobjPrimDoc);

            addPropertyView(this, baseGroup, objPrimDoc);
            addPropertyView(this, primDocStore, objPrimDoc);
            addPropertyView(this, changesParams, objPrimDoc, objArt);
            addPropertyView(this, quantity, objPrimDoc, objArt);
            addPropertyView(this, paramsGroup, objPrimDoc, objArt);

        }
    }

    // ------------------------------------- Временные методы --------------------------- //

    void fillData(DataAdapter Adapter) throws SQLException {

        int Modifier = 10;

        Map<Class,Integer> ClassQuantity = new HashMap();
        ClassQuantity.put(article,2*Modifier);
        ClassQuantity.put(articleGroup,((Double)(Modifier*0.3)).intValue());
        ClassQuantity.put(store,((Double)(Modifier*0.3)).intValue());
        ClassQuantity.put(extIncomeDocument,Modifier*2);
        ClassQuantity.put(extIncomeDetail,Modifier*10);
        ClassQuantity.put(intraDocument,Modifier);
        ClassQuantity.put(extOutcomeDocument,Modifier*5);
        ClassQuantity.put(exchangeDocument,Modifier);
        ClassQuantity.put(revalDocument,((Double)(Modifier*0.5)).intValue());

        Map<DataProperty, Set<DataPropertyInterface>> PropNotNulls = new HashMap();
        name.putNotNulls(PropNotNulls,0);
        artGroup.putNotNulls(PropNotNulls,0);
        primDocDate.putNotNulls(PropNotNulls,0);
        secDocDate.putNotNulls(PropNotNulls,0);
        extIncStore.putNotNulls(PropNotNulls,0);
        intraIncStore.putNotNulls(PropNotNulls,0);
        intraOutStore.putNotNulls(PropNotNulls,0);
        extOutStore.putNotNulls(PropNotNulls,0);
        exchStore.putNotNulls(PropNotNulls,0);
        revalStore.putNotNulls(PropNotNulls,0);
        intraIncStore.putNotNulls(PropNotNulls,0);
        extIncDetailDocument.putNotNulls(PropNotNulls,0);
        extIncDetailArticle.putNotNulls(PropNotNulls,0);
        extIncDetailQuantity.putNotNulls(PropNotNulls,0);
        extIncDetailPriceIn.putNotNulls(PropNotNulls,0);
        extIncDetailVATIn.putNotNulls(PropNotNulls,0);

        Map<DataProperty,Integer> PropQuantity = new HashMap();

//        PropQuantity.put((DataProperty)extIncQuantity.Property,10);
        PropQuantity.put((DataProperty)intraQuantity.Property,Modifier*2);
        PropQuantity.put((DataProperty)extOutQuantity.Property,Modifier);
        PropQuantity.put((DataProperty)exchangeQuantity.Property,Modifier*2);

        autoFillDB(Adapter,ClassQuantity,PropQuantity,PropNotNulls);
    }

}


