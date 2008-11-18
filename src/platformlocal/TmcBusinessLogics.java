package platformlocal;

import javax.swing.*;
import java.sql.SQLException;
import java.util.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

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
    AbstractGroup paramsGroup, accountGroup;
    
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
        accountGroup = new AbstractGroup("Бухг. параметры");
    }

    void InitClasses() {

        article = new ObjectClass(baseGroup, IDGet(1), "Товар", objectClass);
        articleGroup = new ObjectClass(baseGroup, IDGet(2), "Группа товаров", objectClass);

        store = new ObjectClass(baseGroup, IDGet(3), "Склад", objectClass);

        document = new ObjectClass(baseGroup, IDGet(4), "Документ", objectClass);
        primaryDocument = new ObjectClass(baseGroup, IDGet(5), "Первичный документ", document);
        secondaryDocument = new ObjectClass(baseGroup, IDGet(6), "Непервичный документ", document);
        quantityDocument = new ObjectClass(baseGroup, IDGet(7), "Товарный документ", document);
        incomeDocument = new ObjectClass(baseGroup, IDGet(8), "Приходный документ", quantityDocument);
        outcomeDocument = new ObjectClass(baseGroup, IDGet(9), "Расходный документ", quantityDocument);
        paramsDocument = new ObjectClass(baseGroup, IDGet(10), "Порожденный документ", document);

        extIncomeDocument = new ObjectClass(baseGroup, IDGet(11), "Внешний приход", incomeDocument, primaryDocument);
        extIncomeDetail = new ObjectClass(baseGroup, IDGet(12), "Внешний приход (строки)", objectClass);

        intraDocument = new ObjectClass(baseGroup, IDGet(13), "Внутреннее перемещение", incomeDocument, outcomeDocument, primaryDocument, paramsDocument);
        extOutcomeDocument = new ObjectClass(baseGroup, IDGet(14), "Внешний расход", outcomeDocument, secondaryDocument, paramsDocument);
        exchangeDocument = new ObjectClass(baseGroup, IDGet(15), "Пересорт", incomeDocument, outcomeDocument, secondaryDocument, paramsDocument);

        revalDocument = new ObjectClass(IDGet(16), "Переоценка", primaryDocument);

        IDShift(100);
    }

    LDP name;
    LDP artGroup;
    LDP primDocDate, secDocDate;

    LDP extIncStore;
    LDP intraOutStore, intraIncStore;
    LDP extOutStore;
    LDP exchStore;
    LDP revalStore;

    LUP incQStore;
    LUP incSStore;
    LUP outQStore;
    LUP outSStore;
    LUP primDocStore;
    LUP paramsStore;
    LUP docStore;

    LJP isDocIncStore;
    LJP isDocOutStore;
    LJP isDocRevalStore;
    LUP isDocStore;
    LUP dltDocStore;

    LJP artGroupName;
    LJP docStoreName;
    LJP intraStoreName;
    LJP extIncDetailArticleName;

    LDP extIncDetailDocument, extIncDetailArticle, extIncDetailQuantity;
    LGP extIncQuantity;
    LDP intraQuantity;
    LDP extOutQuantity;
    LDP exchangeQuantity;
    LDP revalBalanceQuantity;
    LGP exchIncQuantity, exchOutQuantity;
    LUP exchDltQuantity;

    LUP incQuantity;
    LUP outQuantity;
    LUP quantity;
    LUP paramsQuantity;
    LJP dltStoreQuantity;
    LJP notZeroQuantity;
    LJP notZeroIncPrmsQuantity;
    LGP incStoreQuantity, outStoreQuantity;
    LUP balanceStoreQuantity;

    LUP docDate;
    LJP groeqDocDate;

    LDP extIncDetailPriceIn, extIncDetailVATIn;
    LJP extIncDetailCalcSum;
    LJP extIncDetailCalcSumVATIn;
    LUP extIncDetailCalcSumPay;
    LDP extIncDetailSumVATIn, extIncDetailSumPay;

    LGP extIncDocumentSumVATIn, extIncDocumentSumPay;

    LDP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
    LJP extIncDetailCalcPriceOut;
    LDP extIncDetailPriceOut;

    LDP isRevalued;
    LJP revaluedBalanceQuantity;
    LDP revalPriceIn, revalVATIn;
    LDP revalAddBefore, revalVATOutBefore, revalLocTaxBefore;
    LDP revalPriceOutBefore;
    LDP revalAddAfter, revalVATOutAfter, revalLocTaxAfter;
    LDP revalPriceOutAfter;

    LUP changesParams;
    LGP maxChangesParamsDate;
    LGP maxChangesParamsDoc;

    LDP paramsPriceIn, paramsVATIn;
    LDP paramsAdd, paramsVATOut, paramsLocTax;
    LDP paramsPriceOut;

    LGP extIncLastDetail;

    LJP extIncPriceIn, extIncVATIn;
    LJP extIncAdd, extIncVATOut, extIncLocTax;
    LJP extIncPriceOut;

    LUP primDocPriceIn;
    LUP primDocVATIn;
    LUP primDocAdd;
    LUP primDocVATOut;
    LUP primDocLocTax;
    LUP primDocPriceOut;

    LJP storePriceIn, storeVATIn;
    LJP storeAdd, storeVATOut, storeLocTax;
    LJP storePriceOut;

    LJP docOutBalanceQuantity, docIncBalanceQuantity, docRevBalanceQuantity;

    LJP extIncDetailSumPriceOut, intraSumPriceOut, extOutSumPriceOut;
    LJP exchIncSumPriceOut, exchOutSumPriceOut;
    LUP exchDltSumPriceOut;
    LJP revalSumPriceOutBefore, revalSumPriceOutAfter;
    LUP revalSumPriceOut;

    LJP extIncDetailSumAccount, intraSumAccount, extOutSumAccount;
    LGP extIncSumAccount;
    LJP exchIncSumAccount, exchOutSumAccount;
    LUP exchDltSumAccount;
    LUP revalSumAccount;

    LUP incSumAccount, outSumAccount, docArticleSumAccount;
    LJP dltDocStoreArticleSumAccount;

    LGP dltDocStoreSumAccount
            ;
    LUP dltClassDocStoreSumAccount;

    LJP dltDocStoreGroeqDateSumAccount;
    LGP dltStoreGroeqDateSumAccount;

    LGP incStoreSumAccount, outStoreSumAccount;
    LUP balanceDocStoreArticleSumAccount;
    LJP balanceStoreArticleSumAccount;
    LUP dltStoreArticleSumAccount;
    LGP balanceDocStoreSumAccount, balanceStoreSumAccount, dltStoreSumAccount;

    LUP balanceDocStoreDateOSumAccount;

    LJP docCurPriceIn, docCurVATIn;
    LJP docCurAdd, docCurVATOut, docCurLocTax;
    LJP docCurPriceOut;

    LUP docOverPriceIn;
    LUP docOverVATIn;
    LUP docOverAdd;
    LUP docOverVATOut;
    LUP docOverLocTax;
    LUP docOverPriceOut;

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

    LUP isDocArtInclude;
    LJP isDocStoreArtInclude;

    void InitProperties() {

        LSFP equals2 = AddWSFProp("((prm1)=(prm2))",2);
        LSFP equals22 = AddWSFProp("((prm1)=(prm2)) AND ((prm3)=(prm4))",4);
        LSFP groeq2 = AddWSFProp("((prm1)>=(prm2))",2);
        LSFP notZero = AddWSFProp("((prm1)<>0)",1);
        LSFP percent = AddSFProp("((prm1*prm2)/100)", Class.doubleClass, 2);
        LSFP addPercent = AddSFProp("((prm1*(100+prm2))/100)", Class.doubleClass, 2);
        LSFP round = AddSFProp("round(prm1)", Class.doubleClass, 1);
        LMFP multiplyBit2 = AddMFProp(Class.bit,2);
        LMFP multiplyDouble2 = AddMFProp(Class.doubleClass,2);

        LP dateDocument = AddCProp("абст. дата", null, Class.date, document);
        LP storePrimDoc = AddCProp("абст. склад", null, store, primaryDocument);
        LP bitExtInc = AddCProp("Бит", true, Class.bit, extIncomeDetail);
        LP bitDocStore = AddCProp("абст. бит", null, Class.bit, document, store);
        LP doubleDocStore = AddCProp("абст. число", null, Class.doubleClass, document, store);
        LP doubleDocArticle = AddCProp("абст. кол-во", null, Class.doubleClass, document, article);
        LP doubleIncDocArticle = AddCProp("абст. кол-во", null, Class.doubleClass, incomeDocument, article);
        LP doubleOutDocArticle = AddCProp("абст. кол-во", null, Class.doubleClass, outcomeDocument, article);
        LP bitPrimDocArticle = AddCProp("абст. бит", null, Class.bit, primaryDocument, article);
        LP doublePrimDocArticle = AddCProp("null", null, Class.doubleClass, primaryDocument, article);

        // -------------------------- Data propertyViews ---------------------- //

        name = AddDProp(baseGroup, "Имя", Class.string, objectClass);

        artGroup = AddDProp("Гр. тов.", articleGroup, article);

        // -------------------------- Склады ---------------------- //

        extIncStore = AddDProp("Склад", store, extIncomeDocument);
        intraOutStore = AddDProp("Склад отпр.", store, intraDocument);
        intraIncStore = AddDProp("Склад назн.", store, intraDocument);
        extOutStore = AddDProp("Склад", store, extOutcomeDocument);
        exchStore = AddDProp("Склад", store, exchangeDocument);
        revalStore = AddDProp("Склад", store, revalDocument);

        incQStore = AddUProp("Склад прих.", 2, 1, 1, extIncStore, 1, 1, intraIncStore, 1, 1, exchStore, 1);
        outQStore = AddUProp("Склад расх.", 2, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1);
        incSStore = AddUProp("Склад прих. (сум)", 2, 1, 1, incQStore, 1, 1, revalStore, 1);
        outSStore = outQStore;
        primDocStore = AddUProp(paramsGroup, "Склад (изм.)", 2, 1, 1, storePrimDoc, 1, 1, extIncStore, 1, 1, intraIncStore, 1, 1, revalStore, 1);
        paramsStore = AddUProp("Склад (парам.)", 2, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1);
        docStore = AddUProp("Склад", 2, 1, 1, extIncStore, 1, 1, intraOutStore, 1, 1, extOutStore, 1, 1, exchStore, 1, 1, revalStore, 1);

        // здесь свойства по складам/документам
        isDocIncStore = AddJProp("Склад=прих.", equals2, 2, incQStore, 1, 2);
        isDocOutStore = AddJProp("Склад=расх.", equals2, 2, outQStore, 1, 2);
        isDocRevalStore = AddJProp("Склад=переоц.", equals2, 2, revalStore, 1, 2);
        isDocStore = AddUProp("Склад=док.", 2, 2, 1, bitDocStore, 1, 2, 1, isDocIncStore, 1, 2, 1, isDocOutStore, 1, 2, 1, isDocRevalStore, 1, 2);

        dltDocStore = AddUProp("Склад=док.", 2, 2, 1, bitDocStore, 1, 2, -1, isDocOutStore, 1, 2, 1, isDocIncStore, 1, 2, 1, isDocRevalStore, 1, 2);

        extIncDetailDocument = AddDProp("Документ", extIncomeDocument, extIncomeDetail);
        extIncDetailArticle = AddDProp("Товар", article, extIncomeDetail);

        // -------------------------- Relation propertyViews ------------------ //

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

        revalBalanceQuantity = AddDProp(quantGroup, "Остаток", Class.doubleClass, revalDocument, article);

        exchIncQuantity = AddGProp("Прих. перес.", exchangeQuantity, true, 1, 3);
        exchOutQuantity = AddGProp("Расх. перес.", exchangeQuantity, true, 1, 2);
        exchDltQuantity = AddUProp("Разн. перес.", 1, 2, 1, exchIncQuantity, 1, 2, -1, exchOutQuantity, 1, 2);

        incQuantity = AddUProp("Кол-во прих.", 1, 2, 1, doubleIncDocArticle, 1, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchIncQuantity, 1, 2);
        outQuantity = AddUProp("Кол-во расх.", 1, 2, 1, doubleOutDocArticle, 1, 2, 1, extOutQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchOutQuantity, 1, 2);

        quantity = AddUProp("Кол-во", 2, 2, 1, doubleDocArticle, 1, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2,
                                                                               1, extOutQuantity, 1, 2, 1, exchDltQuantity, 1, 2 );

        paramsQuantity = AddUProp(paramsGroup, "Кол-во пар.", 2, 2, 1, quantity, 1, 2, 1, revalBalanceQuantity, 1, 2);

        dltStoreQuantity = AddJProp("Кол-во (+-)", multiplyDouble2, 3, dltDocStore, 1, 2, quantity, 1, 3);

        notZeroQuantity = AddJProp("Есть в док.", notZero, 2, quantity, 1, 2);

        LP incPrmsQuantity = AddUProp("Кол-во прих. (парам.)", 2, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2);
        notZeroIncPrmsQuantity = AddJProp("Есть в перв. док.", notZero, 2, incPrmsQuantity, 1, 2);

        incStoreQuantity = AddGProp("Прих. на скл.", incQuantity, true, incQStore, 1, 2);
        outStoreQuantity = AddGProp("Расх. со скл.", outQuantity, true, outQStore, 1, 2);

        balanceStoreQuantity = AddUProp(balanceGroup, "Ост. на скл.", 1, 2, 1, incStoreQuantity, 1, 2, -1, outStoreQuantity, 1, 2);
//        OstArtStore = AddUProp("остаток по складу",1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);

        // -------------------------- Входные параметры ---------------------------- //

        primDocDate = AddDProp(baseGroup, "Дата", Class.date, primaryDocument);
        secDocDate = AddDProp(baseGroup, "Дата", Class.date, secondaryDocument);

        docDate = AddUProp("Дата", 2, 1, 1, dateDocument, 1, 1, secDocDate, 1, 1, primDocDate, 1);
        groeqDocDate = AddJProp("Дата>=док.", groeq2, 2, docDate, 1, 2);

        extIncDetailPriceIn = AddDProp(incPrmsGroup, "Цена пост.", Class.doubleClass, extIncomeDetail);
        extIncDetailVATIn = AddDProp(incPrmsGroup, "НДС пост.", Class.doubleClass, extIncomeDetail);

        // -------------------------- Входные суммы ---------------------------- //

        extIncDetailCalcSum = AddJProp(incSumsGroup, "Сумма пост.", multiplyDouble2, 1, extIncDetailQuantity, 1, extIncDetailPriceIn, 1);

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

        isRevalued = AddDProp("Переоц.", Class.bit, revalDocument, article);
        revaluedBalanceQuantity = AddJProp("Остаток (переоц.)", multiplyDouble2, 2, revalBalanceQuantity, 1, 2, isRevalued, 1, 2);

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

        changesParams = AddUProp(paramsGroup, "Изм. парам.", 2, 2, 1, bitPrimDocArticle, 1, 2, 1, isRevalued, 1, 2, 1, notZeroIncPrmsQuantity, 1, 2);
        LMFP multiplyBitDate = AddMFProp(Class.date,2);
        LJP changesParamsDate = AddJProp("Дата изм. пар.", multiplyBitDate, 2, changesParams, 1, 2, primDocDate, 1);
        maxChangesParamsDate = AddGProp(baseGroup, "Посл. дата изм. парам.", changesParamsDate, false, primDocStore, 1, 2);

        LJP primDocIsCor = AddJProp("Док. макс.", equals22, 3, primDocDate, 1, maxChangesParamsDate, 2, 3, primDocStore, 1, 2);

        LJP primDocIsLast = AddJProp("Посл.", multiplyBit2, 3, primDocIsCor, 1, 2, 3, changesParams, 1, 3);

        LMFP multiplyBitPrimDoc = AddMFProp(primaryDocument,2);
        LJP primDocSelfLast = AddJProp("Тов. док. макс.", multiplyBitPrimDoc, 3, primDocIsLast, 1, 2, 3, 1);
        maxChangesParamsDoc = AddGProp(baseGroup, "Посл. док. изм. парам.", primDocSelfLast, false, 2, 3);

        // ------------------------- Параметры по приходу --------------------------- //

        LMFP multiplyBitDetail = AddMFProp(extIncomeDetail,2);
        LJP propDetail = AddJProp("Зн. строки", multiplyBitDetail, 1, bitExtInc, 1, 1);
        extIncLastDetail = AddGProp("Посл. строка", propDetail, false, extIncDetailDocument, 1, extIncDetailArticle, 1);

        extIncPriceIn = AddJProp(incPrmsGroup, "Цена пост. (прих.)", extIncDetailPriceIn, 2, extIncLastDetail, 1, 2);
        extIncVATIn = AddJProp(incPrmsGroup, "НДС пост. (прих.)", extIncDetailVATIn, 2, extIncLastDetail, 1, 2);
        extIncAdd = AddJProp(outPrmsGroup, "Надбавка (прих.)", extIncDetailAdd, 2, extIncLastDetail, 1, 2);
        extIncVATOut = AddJProp(outPrmsGroup, "НДС прод. (прих.)", extIncDetailVATOut, 2, extIncLastDetail, 1, 2);
        extIncLocTax = AddJProp(outPrmsGroup, "Местн. нал. (прих.)", extIncDetailLocTax, 2, extIncLastDetail, 1, 2);
        extIncPriceOut = AddJProp(outPrmsGroup, "Цена розн. (прих.)", extIncDetailPriceOut, 2, extIncLastDetail, 1, 2);

        // ------------------------- Текущие параметры ------------------------ //

        primDocPriceIn = AddUProp(paramsGroup, "Цена пост. (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, paramsPriceIn, 1, 2, 1, extIncPriceIn, 1, 2, 1, revalPriceIn, 1, 2);
        primDocVATIn = AddUProp(paramsGroup, "НДС пост. (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, paramsVATIn, 1, 2, 1, extIncVATIn, 1, 2, 1, revalVATIn, 1, 2);
        primDocAdd = AddUProp(paramsGroup, "Надбавка (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, paramsAdd, 1, 2, 1, extIncAdd, 1, 2, 1, revalAddAfter, 1, 2);
        primDocVATOut = AddUProp(paramsGroup, "НДС прод. (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, paramsVATOut, 1, 2, 1, extIncVATOut, 1, 2, 1, revalVATOutAfter, 1, 2);
        primDocLocTax = AddUProp(paramsGroup, "Местн. нал. (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, paramsLocTax, 1, 2, 1, extIncLocTax, 1, 2, 1, revalLocTaxAfter, 1, 2);
        primDocPriceOut = AddUProp(paramsGroup, "Цена розн. (изм.)", 2, 2, 1, doublePrimDocArticle, 1, 2, 1, paramsPriceOut, 1, 2, 1, extIncPriceOut, 1, 2, 1, revalPriceOutAfter, 1, 2);

        storePriceIn = AddJProp(incPrmsGroup, "Цена пост. (тек.)", primDocPriceIn, 2, maxChangesParamsDoc, 1, 2, 2);
        storeVATIn = AddJProp(incPrmsGroup, "НДС пост. (тек.)", primDocVATIn, 2, maxChangesParamsDoc, 1, 2, 2);
        storeAdd = AddJProp(outPrmsGroup, "Надбавка (тек.)", primDocAdd, 2, maxChangesParamsDoc, 1, 2, 2);
        storeVATOut = AddJProp(outPrmsGroup, "НДС прод. (тек.)", primDocVATOut, 2, maxChangesParamsDoc, 1, 2, 2);
        storeLocTax = AddJProp(outPrmsGroup, "Местн. нал. (тек.)", primDocLocTax, 2, maxChangesParamsDoc, 1, 2, 2);
        storePriceOut = AddJProp(outPrmsGroup, "Цена розн. (тек.)", primDocPriceOut, 2, maxChangesParamsDoc, 1, 2, 2);

        // ------------------------- Суммы выходные ----------------------------------------- //

        extIncDetailSumPriceOut = AddJProp("Сумма розн. (вх.)", multiplyDouble2, 1, extIncDetailQuantity, 1, extIncDetailPriceOut, 1);
        intraSumPriceOut = AddJProp("Сумма розн. (вн.)", multiplyDouble2, 2, intraQuantity, 1, 2, paramsPriceOut, 1, 2);
        extOutSumPriceOut = AddJProp("Сумма розн. (расх.)", multiplyDouble2, 2, extOutQuantity, 1, 2, paramsPriceOut, 1, 2);

        exchIncSumPriceOut = AddJProp("Сумма розн. (перес. +)", multiplyDouble2, 2, exchIncQuantity, 1, 2, paramsPriceOut, 1, 2);
        exchOutSumPriceOut = AddJProp("Сумма розн. (перес. -)", multiplyDouble2, 2, exchOutQuantity, 1, 2, paramsPriceOut, 1, 2);
        exchDltSumPriceOut = AddUProp("Сумма розн. (перес.)", 1, 2, 1, exchIncSumPriceOut, 1, 2, -1, exchOutSumPriceOut, 1, 2);

        revalSumPriceOutBefore = AddJProp("Сумма розн. (переоц. до)", multiplyDouble2, 2, revaluedBalanceQuantity, 1, 2, revalPriceOutBefore, 1, 2);
        revalSumPriceOutAfter = AddJProp("Сумма розн. (переоц. после)", multiplyDouble2, 2, revaluedBalanceQuantity, 1, 2, revalPriceOutAfter, 1, 2);
        revalSumPriceOut = AddUProp("Сумма розн. (переоц.)", 1, 2, 1, revalSumPriceOutAfter, 1, 2, -1, revalSumPriceOutBefore, 1, 2);

        // ------------------------- Суммы учетные по док. ----------------------------------------- //

        extIncDetailSumAccount = extIncDetailSumPriceOut;
        extIncSumAccount = AddGProp("Сумма учетн. (вх.)", extIncDetailSumAccount, true, extIncDetailDocument, 1, extIncDetailArticle, 1);

        intraSumAccount = intraSumPriceOut;
        extOutSumAccount = extOutSumPriceOut;
        exchIncSumAccount = exchIncSumPriceOut;
        exchOutSumAccount = exchOutSumPriceOut;
        exchDltSumAccount = exchDltSumPriceOut;
        revalSumAccount = revalSumPriceOut;

        incSumAccount = AddUProp("Сумма учетн. прих.", 1, 2, 1, extIncSumAccount, 1, 2, 1, intraSumAccount, 1, 2, 1, exchIncSumAccount, 1, 2, 1, revalSumAccount, 1, 2);
        outSumAccount = AddUProp("Сумма учетн. расх.", 1, 2, 1, extOutSumAccount, 1, 2, 1, intraSumAccount, 1, 2, 1, exchOutSumAccount, 1, 2);

        docArticleSumAccount = AddUProp("Сумма учетн. товара по док.", 1, 2, 1, extIncSumAccount, 1, 2, 1, intraSumAccount, 1, 2, 1, extOutSumAccount, 1, 2,
                                                               1, exchDltSumAccount, 1, 2, 1, revalSumAccount, 1, 2);

        dltDocStoreArticleSumAccount = AddJProp("Сумма учетн. товара (+-)", multiplyDouble2, 3, dltDocStore, 1, 2, docArticleSumAccount, 1, 3);

        dltDocStoreSumAccount = AddGProp("Сумма учетн. груп. (+-)", dltDocStoreArticleSumAccount, true, 1, 2);
        dltClassDocStoreSumAccount = AddUProp("Сумма учетн. (+-)", 2, 2, 1, doubleDocStore, 1, 2, 1, dltDocStoreSumAccount, 1, 2);

        dltDocStoreGroeqDateSumAccount = AddJProp("Сумма учетн. с даты (+-)", multiplyDouble2, 3, groeqDocDate, 1, 3, dltDocStoreSumAccount, 1, 2);
        dltStoreGroeqDateSumAccount = AddGProp("Сумма учетн. с даты по складу", dltDocStoreGroeqDateSumAccount, true, 2, 3);


//        dltDocStoreSumAccount = AddUProp("Сумма учетн. (+-)", 2, 2, 1, doubleDocStore, 1, 2, 1, AddGProp("Сумма учетн. груп. (+-)", dltDocStoreArticleSumAccount, true, 1, 2), 1, 2);

        incStoreSumAccount = AddGProp("Сумма учетн. прих. на скл.", incSumAccount, true, incSStore, 1, 2);
        outStoreSumAccount = AddGProp("Сумма учетн. расх. со скл.", outSumAccount, true, outSStore, 1, 2);

        balanceDocStoreArticleSumAccount = AddUProp("Сумма товара на скл. (по док.)", 1, 2, 1, incStoreSumAccount, 1, 2, -1, outStoreSumAccount, 1, 2);
        balanceStoreArticleSumAccount = AddJProp("Сумма товара на скл. (по ост.)", multiplyDouble2, 2, balanceStoreQuantity, 1, 2, storePriceOut, 1, 2);
        dltStoreArticleSumAccount = AddUProp(accountGroup, "Отклонение суммы товара на скл.", 1, 2, 1, balanceDocStoreArticleSumAccount, 1, 2, -1, balanceStoreArticleSumAccount, 1, 2);

        balanceDocStoreSumAccount = AddGProp("Сумма на скл. (по док.)", balanceDocStoreArticleSumAccount, true, 1);
        balanceStoreSumAccount = AddGProp("Сумма на скл. (по ост.)", balanceStoreArticleSumAccount, true, 1);
        dltStoreSumAccount = AddGProp(accountGroup, "Отклонение суммы на скл.", dltStoreArticleSumAccount, true, 1);

        balanceDocStoreDateOSumAccount = AddUProp("Сумма учетн. на начало", 1, 2, 1, AddJProp("", balanceDocStoreSumAccount, 2, 1), 1, 2, -1, dltStoreGroeqDateSumAccount, 1, 2);

        // ------------------------- Перегруженные параметры ------------------------ //

        docOutBalanceQuantity = AddJProp(balanceGroup, "Остаток (расх.)", balanceStoreQuantity, 2, outQStore, 1, 2);
        docIncBalanceQuantity = AddJProp(balanceGroup, "Остаток (прих.)", balanceStoreQuantity, 2, incQStore, 1, 2);
        docRevBalanceQuantity = AddJProp(balanceGroup, "Остаток (переоц.)", balanceStoreQuantity, 2, revalStore, 1, 2);

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

        revalOverBalanceQuantity = AddUProp(balanceGroup, "Остаток", 2, 2, 1, docRevBalanceQuantity, 1, 2, 1, revalBalanceQuantity, 1, 2);
        revalOverPriceIn = AddUProp(incPrmsGroupBefore, "Цена пост.", 2, 2, 1, revalCurPriceIn, 1, 2, 1, revalPriceIn, 1, 2);
        revalOverVATIn = AddUProp(incPrmsGroupBefore, "НДС пост.", 2, 2, 1, revalCurVATIn, 1, 2, 1, revalVATIn, 1, 2);
        revalOverAddBefore = AddUProp(outPrmsGroupBefore, "Надбавка (до)", 2, 2, 1, revalCurAdd, 1, 2, 1, revalAddBefore, 1, 2);
        revalOverVATOutBefore = AddUProp(outPrmsGroupBefore, "НДС прод. (до)", 2, 2, 1, revalCurVATOut, 1, 2, 1, revalVATOutBefore, 1, 2);
        revalOverLocTaxBefore = AddUProp(outPrmsGroupBefore, "Местн. нал. (до)", 2, 2, 1, revalCurLocTax, 1, 2, 1, revalLocTaxBefore, 1, 2);
        revalOverPriceOutBefore = AddUProp(outPrmsGroupBefore, "Цена розн. (до)", 2, 2, 1, revalCurPriceOut, 1, 2, 1, revalPriceOutBefore, 1, 2);

        LJP docCurQPriceIn = AddJProp("", multiplyDouble2, 2, notZeroIncPrmsQuantity, 1, 2, docCurPriceIn, 1, 2);
        LJP docCurQVATIn = AddJProp("", multiplyDouble2, 2, notZeroIncPrmsQuantity, 1, 2, docCurVATIn, 1, 2);
        LJP docCurQAdd = AddJProp("", multiplyDouble2, 2, notZeroIncPrmsQuantity, 1, 2, docCurAdd, 1, 2);
        LJP docCurQVATOut = AddJProp("", multiplyDouble2, 2, notZeroIncPrmsQuantity, 1, 2, docCurVATOut, 1, 2);
        LJP docCurQLocTax = AddJProp("", multiplyDouble2, 2, notZeroIncPrmsQuantity, 1, 2, docCurLocTax, 1, 2);
        LJP docCurQPriceOut = AddJProp("", multiplyDouble2, 2, notZeroIncPrmsQuantity, 1, 2, docCurPriceOut, 1, 2);

        // ------------------------- Вхождение в документ ------------------------ //

        isDocArtInclude = AddUProp("Есть в док.", 2, 2, 1, notZeroQuantity, 1, 2, 1, isRevalued, 1, 2);
        isDocStoreArtInclude = AddJProp("В док. и скл.", multiplyBit2, 3, isDocStore, 1, 2, isDocArtInclude, 1, 3);

/*        setDefProp(paramsPriceIn, docCurQPriceIn, true);
        setDefProp(paramsVATIn, docCurQVATIn, true);
        setDefProp(paramsAdd, docCurQAdd, true);
        setDefProp(paramsVATOut, docCurQVATOut, true);
        setDefProp(paramsLocTax, docCurQLocTax, true);
        setDefProp(paramsPriceOut, docCurQPriceOut, true); */

        initCustomLogics();
    }

    void initCustomLogics() {

        // конкретные классы
        Class articleFood = new ObjectClass(IDGet(1), "Продтовары", article);
        AddDProp(artclGroup, "Срок годности", Class.string, articleFood);

        Class articleAlcohol = new ObjectClass(IDGet(2), "Алкоголь", articleFood);
        AddDProp(artclGroup, "Крепость", Class.integer, articleAlcohol);

        Class articleVodka = new ObjectClass(IDGet(3), "Водка", articleAlcohol);
        AddDProp(artclGroup, "Прейск.", Class.bit, articleVodka);

        Class articleBeer = new ObjectClass(IDGet(4), "Пиво", articleAlcohol);
        AddDProp(artclGroup, "Тип", Class.string, articleBeer);
        AddDProp(artclGroup, "Упак.", Class.string, articleBeer);

        Class wineTaste = new ObjectClass(IDGet(5), "Вкус вина", objectClass);
        Class articleWine = new ObjectClass(IDGet(6), "Вино", articleAlcohol);
        AddJProp(artclGroup, "Вкус", name, 1, AddDProp("Код вкуса", wineTaste, articleWine), 1);

        Class articleMilkGroup = new ObjectClass(IDGet(7), "Молочные продукты", articleFood);
        AddDProp(artclGroup, "Жирн.", Class.doubleClass, articleMilkGroup);

        Class articleMilk = new ObjectClass(IDGet(8), "Молоко", articleMilkGroup);
        AddDProp(artclGroup, "Упак.", Class.string,  articleMilk);

        Class articleCheese = new ObjectClass(IDGet(9), "Сыр", articleMilkGroup);
        AddDProp(artclGroup, "Вес.", Class.bit, articleCheese);

        Class articleCurd = new ObjectClass(IDGet(10), "Творог", articleMilkGroup);

        Class articleBreadGroup = new ObjectClass(IDGet(11), "Хлебобулочные изделия", articleFood);
        AddDProp(artclGroup, "Вес", Class.integer, articleBreadGroup);

        Class articleBread = new ObjectClass(IDGet(12), "Хлеб", articleBreadGroup);
        AddDProp(artclGroup, "Вес", Class.integer, articleBread);

        Class articleCookies = new ObjectClass(IDGet(13), "Печенье", articleBreadGroup);

        Class articleJuice = new ObjectClass(IDGet(14), "Соки", articleFood);
        AddDProp(artclGroup, "Вкус", Class.string, articleJuice);
        AddDProp(artclGroup, "Литраж", Class.integer, articleJuice);

        Class articleClothes = new ObjectClass(IDGet(15), "Одежда", article);
        AddDProp(artclGroup, "Модель", Class.string, articleClothes);

        Class articleTShirt = new ObjectClass(IDGet(16), "Майки", articleClothes);
        AddDProp(artclGroup, "Размер", Class.string, articleTShirt);

        Class articleJeans = new ObjectClass(IDGet(17), "Джинсы", articleClothes);
        AddDProp(artclGroup, "Ширина", Class.integer, articleJeans);
        AddDProp(artclGroup, "Длина", Class.integer, articleJeans);

        Class articleShooes = new ObjectClass(IDGet(18), "Обувь", article);
        AddDProp(artclGroup, "Цвет", Class.string, articleShooes);

        IDShift(1000);
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

        index = new ArrayList();
        index.add(primDocDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(maxChangesParamsDate.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(docStore.Property);
        Indexes.add(index);

        index = new ArrayList();
        index.add(extOutQuantity.Property);
        Indexes.add(index);
    }

    void InitNavigators() {

        createDefaultClassForms(objectClass, baseElement);

        NavigatorElement primaryData = new NavigatorElement(100, "Первичные данные");
        baseElement.addChild(primaryData);

        NavigatorForm extIncDetailForm = new ExtIncDetailNavigatorForm(110, "Внешний приход");
        primaryData.addChild(extIncDetailForm);

        NavigatorForm extIncForm = new ExtIncNavigatorForm(115, "Внешний приход по товарам");
        extIncDetailForm.addChild(extIncForm);

        NavigatorForm intraForm = new IntraNavigatorForm(120, "Внутреннее перемещение");
        primaryData.addChild(intraForm);

        NavigatorForm extOutForm = new ExtOutNavigatorForm(130, "Внешний расход");
        primaryData.addChild(extOutForm);

        NavigatorForm exchangeForm = new ExchangeNavigatorForm(140, "Пересорт");
        primaryData.addChild(exchangeForm);

        NavigatorForm revalueForm = new RevalueNavigatorForm(150, "Переоценка", false);
        primaryData.addChild(revalueForm);

        NavigatorForm revalueStoreForm = new RevalueNavigatorForm(155, "Переоценка по складам", true);
        revalueForm.addChild(revalueStoreForm);

        NavigatorElement aggregateData = new NavigatorElement(200, "Сводная информация");
        baseElement.addChild(aggregateData);

        NavigatorElement aggrStoreData = new NavigatorElement(210, "Склады");
        aggregateData.addChild(aggrStoreData);

        NavigatorForm storeArticleForm = new StoreArticleNavigatorForm(211, "Товары по складам");
        aggrStoreData.addChild(storeArticleForm);

        NavigatorForm storeArticlePrimDocForm = new StoreArticlePrimDocNavigatorForm(2111, "Товары по складам (изм. цен)");
        storeArticleForm.addChild(storeArticlePrimDocForm);

        NavigatorForm storeArticleDocForm = new StoreArticleDocNavigatorForm(2112, "Товары по складам (док.)");
        storeArticleForm.addChild(storeArticleDocForm);

        NavigatorElement aggrArticleData = new NavigatorElement(220, "Товары");
        aggregateData.addChild(aggrArticleData);

        NavigatorForm articleStoreForm = new ArticleStoreNavigatorForm(221, "Склады по товарам");
        aggrArticleData.addChild(articleStoreForm);

        NavigatorForm articleMStoreForm = new ArticleMStoreNavigatorForm(222, "Товары*Склады");
        aggrArticleData.addChild(articleMStoreForm);

        NavigatorElement analyticsData = new NavigatorElement(300, "Аналитические данные");
        baseElement.addChild(analyticsData);

        NavigatorElement dateIntervalForms = new NavigatorElement(310, "За интервал дат");
        analyticsData.addChild(dateIntervalForms);

        NavigatorForm mainAccountForm = new MainAccountNavigatorForm(311, "Товарный отчет");
        dateIntervalForms.addChild(mainAccountForm);

    }

    private class TmcNavigatorForm extends NavigatorForm {

        TmcNavigatorForm(int iID, String caption) {
            super(iID, caption);
        }

        void addArticleRegularFilterGroup(PropertyObjectImplement documentProp, Object documentValue, PropertyObjectImplement... extraProps) {

            RegularFilterGroup filterGroup = new RegularFilterGroup(IDShift(1));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(documentProp, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(documentValue)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));

            int functionKey = KeyEvent.VK_F9;

            for (PropertyObjectImplement extraProp : extraProps) {
                filterGroup.addFilter(new RegularFilter(IDShift(1),
                                      new Filter(extraProp, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(0)),
                                      extraProp.Property.caption,
                                      KeyStroke.getKeyStroke(functionKey--, 0)));
            }
            addRegularFilterGroup(filterGroup);
        }
    }

    private class ExtIncDetailNavigatorForm extends TmcNavigatorForm {

        public ExtIncDetailNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjDetail = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extIncomeDocument, "Документ", gobjDoc);
            ObjectImplement objDetail = new ObjectImplement(IDShift(1), extIncomeDetail, "Строка", gobjDetail);

            addGroup(gobjDoc);
            addGroup(gobjDetail);

            addPropertyView(Properties, baseGroup, false, objDoc);
            addPropertyView(Properties, storeGroup, false, objDoc);
            addPropertyView(Properties, incSumsGroup, false, objDoc);
            addPropertyView(Properties, artclGroup, false, objDetail);
            addPropertyView(Properties, quantGroup, false, objDetail);
            addPropertyView(Properties, incPrmsGroup, false, objDetail);
            addPropertyView(Properties, incSumsGroup, false, objDetail);
            addPropertyView(Properties, outPrmsGroup, false, objDetail);

            PropertyObjectImplement detDocument = addPropertyObjectImplement(extIncDetailDocument, objDetail);
            addFixedFilter(new Filter(detDocument, FieldExprCompareWhere.EQUALS, new ObjectValueLink(objDoc)));
        }
    }

    private class ExtIncNavigatorForm extends TmcNavigatorForm {

        public ExtIncNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extIncomeDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(Properties, baseGroup, false, objDoc);
            addPropertyView(Properties, storeGroup, false, objDoc);
            addPropertyView(Properties, baseGroup, false, objArt);
//            addPropertyView(Properties, artgrGroup, objArt);
            addPropertyView(Properties, balanceGroup, false, objDoc, objArt);
            addPropertyView(extIncQuantity, objDoc, objArt);
            addPropertyView(Properties, incPrmsGroup, false, objDoc, objArt);
            addPropertyView(Properties, outPrmsGroup, false, objDoc, objArt);

            addArticleRegularFilterGroup(getPropertyView(extIncQuantity.Property).View, 0);
        }
    }

    private class IntraNavigatorForm extends TmcNavigatorForm {

        public IntraNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), intraDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(Properties, baseGroup, false, objDoc);
            addPropertyView(Properties, storeGroup, false, objDoc);
            addPropertyView(Properties, baseGroup, false, objArt);
//            addPropertyView(Properties, artgrGroup, objArt);
            addPropertyView(Properties, balanceGroup, false, objDoc, objArt);
            addPropertyView(intraQuantity, objDoc, objArt);
            addPropertyView(Properties, incPrmsGroup, false, objDoc, objArt);
            addPropertyView(Properties, outPrmsGroup, false, objDoc, objArt);

            addArticleRegularFilterGroup(getPropertyView(intraQuantity.Property).View, 0,
                                         getPropertyView(docOutBalanceQuantity.Property).View,
                                         getPropertyView(docIncBalanceQuantity.Property).View);

            addHintsNoUpdate(maxChangesParamsDoc.Property);
        }
    }

    private class ExtOutNavigatorForm extends TmcNavigatorForm {

        public ExtOutNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extOutcomeDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(Properties, baseGroup, false, objDoc);
            addPropertyView(Properties, storeGroup, false, objDoc);
            addPropertyView(Properties, baseGroup, true, objArt);
            addPropertyView(Properties, artclGroup, true, objArt);
//            addPropertyView(Properties, artgrGroup, objArt);
            addPropertyView(Properties, balanceGroup, false, objDoc, objArt);
            addPropertyView(extOutQuantity, objDoc, objArt);
            addPropertyView(Properties, incPrmsGroup, false, objDoc, objArt);
            addPropertyView(Properties, outPrmsGroup, false, objDoc, objArt);

            addArticleRegularFilterGroup(getPropertyView(extOutQuantity.Property).View, 0,
                                         getPropertyView(docOutBalanceQuantity.Property).View);
        }
    }

    private class ExchangeNavigatorForm extends TmcNavigatorForm {

        public ExchangeNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArtTo = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArtFrom = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), exchangeDocument, "Документ", gobjDoc);
            ObjectImplement objArtTo = new ObjectImplement(IDShift(1), article, "Товар (на)", gobjArtTo);
            ObjectImplement objArtFrom = new ObjectImplement(IDShift(1), article, "Товар (с)", gobjArtFrom);

            addGroup(gobjDoc);
            addGroup(gobjArtTo);
            addGroup(gobjArtFrom);

            addPropertyView(Properties, baseGroup, false, objDoc);
            addPropertyView(Properties, storeGroup, false, objDoc);
            addPropertyView(Properties, baseGroup, false, objArtFrom);
//            addPropertyView(Properties, artgrGroup, objArtFrom);
            addPropertyView(Properties, baseGroup, false, objArtTo);
//            addPropertyView(Properties, artgrGroup, objArtTo);
            addPropertyView(docOutBalanceQuantity, objDoc, objArtTo);
            addPropertyView(exchIncQuantity, objDoc, objArtTo);
            addPropertyView(exchOutQuantity, objDoc, objArtTo);
            addPropertyView(Properties, incPrmsGroup, false, objDoc, objArtTo);
            addPropertyView(Properties, outPrmsGroup, false, objDoc, objArtTo);
            addPropertyView(docOutBalanceQuantity, objDoc, objArtFrom);
            addPropertyView(exchangeQuantity, objDoc, objArtFrom, objArtTo);
            addPropertyView(Properties, incPrmsGroup, false, objDoc, objArtFrom);
            addPropertyView(Properties, outPrmsGroup, false, objDoc, objArtFrom);

            RegularFilterGroup filterGroup = new RegularFilterGroup(IDShift(1));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(exchIncQuantity.Property).View, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(0)),
                                  "Приход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(exchOutQuantity.Property).View, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(0)),
                                  "Расход",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.Property, gobjArtTo).View, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(0)),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.Property, gobjArtTo).View, FieldExprCompareWhere.LESS, new UserValueLink(0)),
                                  "Отр. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroup);

            filterGroup = new RegularFilterGroup(IDShift(1));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  null,
                                  "Все",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(exchangeQuantity.Property).View, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(0)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.Property, gobjArtFrom).View, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(0)),
                                  "Остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOutBalanceQuantity.Property, gobjArtFrom).View, FieldExprCompareWhere.GREATER, new UserValueLink(0)),
                                  "Пол. остаток",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK)));
            filterGroup.addFilter(new RegularFilter(IDShift(1),
                                  new Filter(getPropertyView(docOverPriceOut.Property, gobjArtFrom).View, FieldExprCompareWhere.EQUALS, new PropertyValueLink(getPropertyView(docOverPriceOut.Property, gobjArtTo).View)),
                                  "Одинаковая розн. цена",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);

        }
    }

    private class RevalueNavigatorForm extends TmcNavigatorForm {

        public RevalueNavigatorForm(int ID, String caption, boolean groupStore) {
            super(ID, caption);

            GroupObjectImplement gobjStore = null;
            ObjectImplement objStore = null;
            if (groupStore) {

                gobjStore = new GroupObjectImplement(IDShift(1));
                gobjStore.gridClassView = false;
                gobjStore.singleViewType = true;

                objStore = new ObjectImplement(IDShift(1), store, "Склад", gobjStore);

                addGroup(gobjStore);
            }

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), revalDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(Properties, baseGroup, false, objDoc);
            addPropertyView(Properties, baseGroup, false, objArt);
//            addPropertyView(Properties, artgrGroup, objArt);
            addPropertyView(revalOverBalanceQuantity, objDoc, objArt);
            addPropertyView(isRevalued, objDoc, objArt);
            addPropertyView(Properties, incPrmsGroupBefore, false, objDoc, objArt);
            addPropertyView(Properties, outPrmsGroupBefore, false, objDoc, objArt);
            addPropertyView(Properties, outPrmsGroupAfter, false, objDoc, objArt);

            addArticleRegularFilterGroup(getPropertyView(isRevalued.Property).View, false,
                                         getPropertyView(revalOverBalanceQuantity.Property).View);

            if (groupStore) {

                addPropertyView(Properties, baseGroup, false, objStore);
                addPropertyView(Properties, accountGroup, false, objStore);
                addFixedFilter(new Filter(addPropertyObjectImplement(revalStore, objDoc), FieldExprCompareWhere.EQUALS, new ObjectValueLink(objStore)));
            } else {
                addPropertyView(Properties, storeGroup, false, objDoc);
            }


        }
    }

    private class StoreArticleNavigatorForm extends TmcNavigatorForm {

        GroupObjectImplement gobjStore, gobjArt;
        ObjectImplement objStore, objArt;

        public StoreArticleNavigatorForm(int ID, String caption) {
            super(ID, caption);

            gobjStore = new GroupObjectImplement(IDShift(1));
            gobjStore.gridClassView = false;
            gobjStore.singleViewType = true;

            gobjArt = new GroupObjectImplement(IDShift(1));

            objStore = new ObjectImplement(IDShift(1), store, "Склад", gobjStore);
            objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjStore);
            addGroup(gobjArt);

            addPropertyView(Properties, baseGroup, false, objStore);
            addPropertyView(Properties, accountGroup, false, objStore);
            addPropertyView(Properties, baseGroup, false, objArt);
            addPropertyView(Properties, baseGroup, false, objStore, objArt);
            addPropertyView(Properties, balanceGroup, false, objStore, objArt);
            addPropertyView(Properties, incPrmsGroup, false, objStore, objArt);
            addPropertyView(Properties, outPrmsGroup, false, objStore, objArt);
        }
    }

    private class StoreArticlePrimDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticlePrimDocNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjPrimDoc = new GroupObjectImplement(IDShift(1));

            ObjectImplement objPrimDoc = new ObjectImplement(IDShift(1), primaryDocument, "Документ", gobjPrimDoc);

            addGroup(gobjPrimDoc);

            addPropertyView(Properties, baseGroup, false, objPrimDoc);
//            addPropertyView(Properties, primDocStore, objPrimDoc);
//            addPropertyView(Properties, changesParams, objPrimDoc, objArt);
//            addPropertyView(Properties, quantity, objPrimDoc, objArt);
            addPropertyView(Properties, paramsGroup, false, objPrimDoc);
            addPropertyView(Properties, paramsGroup, false, objPrimDoc, objArt);

            addFixedFilter(new Filter(getPropertyView(changesParams.Property).View, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(false)));
            addFixedFilter(new Filter(getPropertyView(primDocStore.Property).View, FieldExprCompareWhere.EQUALS, new ObjectValueLink(objStore)));

            DefaultClientFormView formView = new DefaultClientFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyView(primDocDate.Property)), false);
            richDesign = formView;
        }
    }

    private class StoreArticleDocNavigatorForm extends StoreArticleNavigatorForm {

        public StoreArticleDocNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), document, "Документ", gobjDoc);

            addGroup(gobjDoc);

            addPropertyView(Properties, baseGroup, false, objDoc);
            addPropertyView(docDate, objDoc);
            addPropertyView(Properties, storeGroup, true, objDoc);
//            addPropertyView(Properties, primDocStore, objDoc);
//            addPropertyView(Properties, changesParams, objDoc, objArt);
            addPropertyView(dltStoreQuantity, objDoc, objStore, objArt);
//            addPropertyView(Properties, paramsGroup, objDoc);
//            addPropertyView(Properties, paramsGroup, objDoc, objArt);
//            addPropertyView(docArticleSumAccount, objDoc, objArt);
//            addPropertyView(dltDocStoreArticleSumAccount, objDoc, objStore, objArt);

            addFixedFilter(new Filter(addPropertyObjectImplement(isDocStoreArtInclude, objDoc, objStore, objArt), FieldExprCompareWhere.EQUALS, new UserValueLink(true)));

            DefaultClientFormView formView = new DefaultClientFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyView(docDate.Property)), false);
            richDesign = formView;
        }
    }

    private class ArticleStoreNavigatorForm extends TmcNavigatorForm {

        GroupObjectImplement gobjStore, gobjArt;
        ObjectImplement objStore, objArt;

        public ArticleStoreNavigatorForm(int ID, String caption) {
            super(ID, caption);

            gobjArt = new GroupObjectImplement(IDShift(1));
            gobjStore = new GroupObjectImplement(IDShift(1));

            objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);
            objStore = new ObjectImplement(IDShift(1), store, "Склад", gobjStore);

            addGroup(gobjArt);
            addGroup(gobjStore);

            addPropertyView(Properties, baseGroup, false, objArt);
            addPropertyView(Properties, baseGroup, false, objStore);

            addPropertyView(Properties, baseGroup, false, objStore, objArt);
            addPropertyView(Properties, balanceGroup, false, objStore, objArt);
            addPropertyView(Properties, incPrmsGroup, false, objStore, objArt);
            addPropertyView(Properties, outPrmsGroup, false, objStore, objArt);

            addPropertyView(Properties, baseGroup, false, gobjArt, objStore, objArt);
            addPropertyView(Properties, balanceGroup, false, gobjArt, objStore, objArt);
            addPropertyView(Properties, incPrmsGroup, false, gobjArt, objStore, objArt);
            addPropertyView(Properties, outPrmsGroup, false, gobjArt, objStore, objArt);

        }

    }

    private class ArticleMStoreNavigatorForm extends TmcNavigatorForm {

        GroupObjectImplement gobjArtStore;
        ObjectImplement objStore, objArt;

        public ArticleMStoreNavigatorForm(int ID, String caption) {
            super(ID, caption);

            gobjArtStore = new GroupObjectImplement(IDShift(1));

            objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArtStore);
            objStore = new ObjectImplement(IDShift(1), store, "Склад", gobjArtStore);

            addGroup(gobjArtStore);

            addPropertyView(Properties, baseGroup, false, objArt);
            addPropertyView(Properties, baseGroup, false, objStore);

            addPropertyView(Properties, baseGroup, false, objStore, objArt);
            addPropertyView(Properties, balanceGroup, false, objStore, objArt);
            addPropertyView(Properties, incPrmsGroup, false, objStore, objArt);
            addPropertyView(Properties, outPrmsGroup, false, objStore, objArt);
        }

    }


    private class DateIntervalNavigatorForm extends TmcNavigatorForm {

        GroupObjectImplement gobjInterval;
        ObjectImplement objDateFrom, objDateTo;

        public DateIntervalNavigatorForm(int ID, String caption) {
            super(ID, caption);

            gobjInterval = new GroupObjectImplement(IDShift(1));
            gobjInterval.gridClassView = false;
            gobjInterval.singleViewType = true;

            objDateFrom = new ObjectImplement(IDShift(1), Class.date, "С даты :", gobjInterval);
            objDateTo = new ObjectImplement(IDShift(1), Class.date, "По дату :", gobjInterval);

            addGroup(gobjInterval);
        }
    }

    private class MainAccountNavigatorForm extends DateIntervalNavigatorForm {

        public MainAccountNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjStore = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));

            ObjectImplement objStore = new ObjectImplement(IDShift(1), store, "Склад", gobjStore);
            ObjectImplement objDoc = new ObjectImplement(IDShift(1), document, "Документ", gobjDoc);

            addGroup(gobjStore);
            addGroup(gobjDoc);

            addPropertyView(balanceDocStoreDateOSumAccount, objStore, objDateFrom);

/*            addPropertyView(Properties, baseGroup, false, objStore);
            addPropertyView(Properties, baseGroup, false, objDoc);
            addPropertyView(docDate, objDoc);
            addPropertyView(dltClassDocStoreSumAccount, objDoc, objStore);

            addFixedFilter(new Filter(getPropertyView(dltClassDocStoreSumAccount.Property).View, FieldExprCompareWhere.NOT_EQUALS, new UserValueLink(0)));
            addFixedFilter(new Filter(getPropertyView(docDate.Property).View, FieldExprCompareWhere.GREATER_EQUALS, new ObjectValueLink(objDateFrom)));
            addFixedFilter(new Filter(getPropertyView(docDate.Property).View, FieldExprCompareWhere.LESS_EQUALS, new ObjectValueLink(objDateTo)));

            DefaultClientFormView formView = new DefaultClientFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyView(docDate.Property)), true);
            richDesign = formView; */
            
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
            ClassQuantity.put(articleClass,Modifier*2/articleChilds.size());

        ClassQuantity.put(articleGroup,((Double)(Modifier*0.3)).intValue());
//        ClassQuantity.put(store,((Double)(Modifier*0.3)).intValue());
        ClassQuantity.put(store,4);
        ClassQuantity.put(extIncomeDocument,Modifier*2);
        ClassQuantity.put(extIncomeDetail,Modifier*PropModifier);
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

//        LDP extIncDetailSumVATIn, extIncDetailSumPay;
//        LDP extIncDetailAdd, extIncDetailVATOut, extIncDetailLocTax;
//        LDP extIncDetailPriceOut;

        Map<DataProperty,Integer> PropQuantity = new HashMap();

//        PropQuantity.put((DataProperty)extIncQuantity.Property,10);
        PropQuantity.put((DataProperty)intraQuantity.Property,Modifier*PropModifier*2);
        PropQuantity.put((DataProperty)extOutQuantity.Property,Modifier*PropModifier*8);
        PropQuantity.put((DataProperty)exchangeQuantity.Property,Modifier*PropModifier);
        PropQuantity.put((DataProperty)isRevalued.Property,Modifier*PropModifier);

        PropQuantity.putAll(autoQuantity(0,paramsPriceIn,paramsVATIn,paramsAdd,paramsVATOut,paramsLocTax,paramsPriceOut,
            revalBalanceQuantity,revalPriceIn,revalVATIn,revalAddBefore,revalVATOutBefore,revalLocTaxBefore,revalPriceOutBefore,
                revalAddAfter,revalVATOutAfter,revalLocTaxAfter,revalPriceOutAfter));

        autoFillDB(Adapter,ClassQuantity,PropQuantity,PropNotNulls);
    }

}


