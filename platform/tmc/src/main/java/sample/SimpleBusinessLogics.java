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
import platform.server.form.entity.filter.*;
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

public class SimpleBusinessLogics extends BusinessLogics<SimpleBusinessLogics> {

    public SimpleBusinessLogics(DataAdapter adapter,int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(adapter,port);
    }

    AbstractGroup formatGroup, regionGroup;
    AbstractGroup supplierGroup, contractGroup;
    AbstractGroup documentGroup, fixedGroup, currentGroup, lastDocumentGroup;

    protected void initGroups() {

        formatGroup = new AbstractGroup("Формат");
        regionGroup = new AbstractGroup("Регион");

        supplierGroup = new AbstractGroup("Поставщик");
        contractGroup = new AbstractGroup("Договор");

        documentGroup = new AbstractGroup("Параметры транзакции");
        fixedGroup = new AbstractGroup("Текущие Параметры транзакции");
        currentGroup = new AbstractGroup("Текущие параметры");
        lastDocumentGroup = new AbstractGroup("Последние параметры");
    }

    AbstractCustomClass document, quantityDocument, incomeDocument, outcomeDocument, extOutcomeDocument,
                        saleDocument, articleDocument, priceOutDocument;

    ConcreteCustomClass article, articleGroup, store, customer, supplier, extIncomeOrder, extIncomeDocument, intraDocument, exchangeDocument, specification, contract,
                        cashSaleDocument, clearingSaleDocument, invDocument, returnDocument, revalDocument, taxDocument, locTaxDocument, format, region, category;
    
    protected void initClasses() {

        article = addConcreteClass("Товар", baseClass.named);
        articleGroup = addConcreteClass("Группа товаров", baseClass.named);

        store = addConcreteClass("Склад", baseClass.named);

        supplier = addConcreteClass("Поставщик", baseClass.named);
        customer = addConcreteClass("Покупатель", baseClass.named);
        format = addConcreteClass("Формат", baseClass.named);
        region = addConcreteClass("Регион", baseClass.named);
        category = addConcreteClass("Категория", baseClass.named);

        document = addAbstractClass("Документ", baseClass.named, transaction);
        priceOutDocument = addAbstractClass("Документ изм. цены", document);
        quantityDocument = addAbstractClass("Товарный документ", document);
        incomeDocument = addAbstractClass("Приходный документ", quantityDocument, priceOutDocument);
        outcomeDocument = addAbstractClass("Расходный документ", quantityDocument);
        articleDocument = addAbstractClass("Перемещение товара", quantityDocument);

        extIncomeOrder = addConcreteClass("Заказ", document);
        extIncomeDocument = addConcreteClass("Внешний приход", incomeDocument, articleDocument);

        intraDocument = addConcreteClass("Внутреннее перемещение", incomeDocument, outcomeDocument, articleDocument);
        extOutcomeDocument = addAbstractClass("Внешний расход", outcomeDocument);
        exchangeDocument = addConcreteClass("Пересорт", outcomeDocument);

        saleDocument = addAbstractClass("Реализация", extOutcomeDocument, articleDocument);
        cashSaleDocument = addConcreteClass("Реализация по кассе", saleDocument);
        clearingSaleDocument = addConcreteClass("Реализация по б/н расчету", saleDocument);

        invDocument = addConcreteClass("Инвентаризация", extOutcomeDocument);

        returnDocument = addConcreteClass("Возврат поставщику", extOutcomeDocument);

        revalDocument = addConcreteClass("Переоценка", priceOutDocument);
        taxDocument = addConcreteClass("Изменение НДС", priceOutDocument);
        locTaxDocument = addConcreteClass("Изменение местн. нал.", priceOutDocument);

        contract = addConcreteClass("Договор", document);
        specification = addConcreteClass("Спецификация", document);
    }

    LP artGroup, outStore, extIncPriceIn, extIncVATIn, invDBBalance,
            returnSupplier, clearingSaleCustomer, articleQuantity, extIncDetailDocument;

    LP quantity, roundm1, addPercent, docIncBalanceQuantity, docOutBalanceQuantity, revalFormat, storeFormat, locTaxRegion, storeRegion, currentSupplier,
            invBalance, exchangeQuantity, exchIncQuantity, exchOutQuantity, priceOutChange, balanceStoreQuantity, revalAdd, taxVatOut, locTaxValue;
    LP currentIncDate, incStore, orderQuantity, orderInSpec, orderAllow;
    LP currentIncDoc;
    LP currentRevalDate;
    LP currentRevalDoc;
    LP currentTaxDate;
    LP currentTaxDoc;
    LP currentLocTaxDate;
    LP currentLocTaxDoc;
    LP currentExtIncDate;
    LP currentExtIncDoc;
    LP docOutPriceOut;
    LP saleFormatArticleBetweenDateQuantity, sumSaleFormatArticleBetweenDateQuantity, saleArticle2;
    LP sumsaleArticle2, saleFormatGroupBetweenDateQuantity;
    LP article1SaleBestArticle2;
    LP percentForABC;
    LP resultABCInsideGroupQuantity, resultABCInsideGroupSum;
    LP resultABC;
    LP resultABCSum;
    LP percentForABCSum, resultABCGroup;
    LP resultABCInsideGroupQ;
    LP saleFormatArticleBetweenDateSum;
    LP saleFormatGroupBetweenDateSum, wholesaleArticleBetweenDateSum, wholesaleFormatBetweenDateSum, saleGroupBetweenDateSum, saleBetweenDateSum;
    LP categoryMark;
    LP allsumSaleSameGroupArticle2;
        
    LP contractSupplier, specContract, extIncSupplier;
    LP storeSpecIncl, specArticleIncl;

    LP remainStoreArticleStartQuantity, remainStoreArticleEndQuantity, incBetweenDateQuantity, outBetweenDateQuantity,
        saleStoreArticleBetweenDateQuantity, saleArticleBetweenDateQuantity, returnQuantity;

    LP currentAdd,currentVatOut,currentLocTax,currentPriceIn,currentPriceOut,storeInRange,storeSupplArt,storeSupplIsCurrent,revalCurrentAdd,locCurrentTax;

    LP initDateBalance(LP dateAnd, String caption) {
        LP dateQuantity = addJProp("Кол-во "+caption, and1, quantity, 1, 2, dateAnd, 1, 3);
        LP incDateQuantity = addSGProp("Прих. на скл. "+caption, dateQuantity, incStore, 1, 2, 3);
        LP outDateQuantity = addSGProp("Расх. со скл. "+caption, dateQuantity, outStore, 1, 2, 3);
        return addDUProp("Ост. на скл. "+caption, incDateQuantity, outDateQuantity);
    }

    // Функция АВС-анализа расчитанного процента(<анализируемый процент>,<подпись>)
    LP initABCAnalyze(LP analizedPercent, String caption) {
        Object[] extendedProperty = directLI(analizedPercent); // Свойство и его интерфейсы
        int intNum = analizedPercent.listInterfaces.size(); // Количество интерфейсов свойства
        Object[] a = new Object [intNum];
        for(int i=0; i < intNum; i++)
            a[i] = i + 1;
        LP numberGreaterCategory = addJProp ("Число больше категории", greater2, BaseUtils.add(extendedProperty,new Object[]{categoryMark, intNum+1}));
        LP categoryMarkLess = addJProp("Порог категории (если меньше числа)", and1, BaseUtils.add(new Object[]{categoryMark, intNum+1, numberGreaterCategory}, BaseUtils.add(a,new Object[]{intNum+1})));
        LP[] storeABCCategory = addMGProp(null,new String[]{"a1","a2"},new String[]{"Max порог","Max категория"}, 1, categoryMarkLess, BaseUtils.add(new Object[]{intNum+1},a));
        return addJProp(baseGroup, "Категория "+caption, name, directLI(storeABCCategory[1]));
    }

    protected void initProperties() {

        // абстрактные св-ва объектов
        LP percent = addSFProp("((prm1*prm2)/100)", DoubleClass.instance, 2);
        LP revPercent = addSFProp("((prm1*prm2)/(100+prm2))", DoubleClass.instance, 2);
        addPercent = addSFProp("((prm1*(100+prm2))/100)", DoubleClass.instance, 2);
        LP round = addSFProp("round(CAST(prm1 as numeric),0)", DoubleClass.instance, 1);
        roundm1 = addSFProp("round(CAST(prm1 as numeric),-1)", DoubleClass.instance, 1);
        LP multiplyDouble2 = addMFProp(DoubleClass.instance,2);
        LP percentABC = addSFProp ("(prm1*100)/prm2", DoubleClass.instance, 2);

        // свойства товара
        artGroup = addDProp("artGroup", "Гр. тов.", articleGroup, article);
        LP artGroupName = addJProp(baseGroup, "Имя гр. тов.", name, artGroup, 1);

        LP artBarCode = addDProp(baseGroup, "artBarCode", "Штрих-код", NumericClass.get(13, 0), article);
        LP artWeight = addDProp(baseGroup, "artWeight", "Вес (кг.)", NumericClass.get(6, 3), article);
        LP artPackCount = addDProp(baseGroup, "artPackCount", "Кол-во в уп.", IntegerClass.instance, article);

        clearingSaleCustomer = addDProp("clearingSaleCustomer","Покупатель", customer, clearingSaleDocument); // реализация по б\н расчету
        LP clearingSaleCustomerName = addJProp(baseGroup, "Имя покупателя", name, clearingSaleCustomer, 1);

        returnSupplier = addDProp("returnSupplier","Поставщик", supplier, returnDocument); // возврат поставщику
        LP returnSupplierName = addJProp(baseGroup, "Имя поставщика", name, returnSupplier, 1);

        revalFormat = addDProp("revalFormat","Формат", format, revalDocument);
        addJProp(baseGroup, "Название формата", name, revalFormat, 1);
        storeFormat = addDProp("storeFormat","Формат", format, store);
        addJProp(baseGroup, "Название формата", name, storeFormat, 1);

        locTaxRegion = addDProp("locTaxRegion","Регион", region, locTaxDocument);
        addJProp(baseGroup, "Название региона", name, locTaxRegion, 1);
        storeRegion = addDProp("storeRegion","Регион", region, store);
        addJProp(baseGroup, "Название региона", name, storeRegion, 1);

        categoryMark = addDProp(baseGroup, "categoryMark", "Ранговое значение", DoubleClass.instance, category);  // Категория

        // Управление поставками

        LP contractDateEnd = addDProp(baseGroup, "contractDateEnd", "Действует до", DateClass.instance, contract);
        LP contractPayTerms = addDProp(baseGroup, "contractPayTerms", "По реализации", LogicalClass.instance, contract);
        LP contractPayDelay = addDProp(baseGroup, "contractPayDelay", "Отсрочка", IntegerClass.instance, contract);

        contractSupplier = addDProp(baseGroup, "contractSupplier", "Поставщик", supplier, contract);
        LP contractSupplierName = addJProp(baseGroup, "contractSupplierName", "Название поставщика", name, contractSupplier, 1);

        LP specDateEnd = addDProp(baseGroup, "specDateEnd", "Действует до", DateClass.instance, specification);
        LP specPriceVolatility = addDProp(baseGroup, "specPriceVolatility", "Отклонение цены", DoubleClass.instance, specification);

        specContract = addDProp(baseGroup, "specContract", "Договор" , contract, specification);
        LP specContractName = addJProp(baseGroup, "specContractName", "Название договора", name, specContract, 1);
        LP specSupplier = addJProp("Поставщик спец.", contractSupplier, specContract, 1);

        LP storeSuppSpec = addDProp(baseGroup, "storeSuppSpec", "Спец. скл. пост.", specification, store, supplier);

        // ограничение на то, что спецификация поставщика соответствует спецификации по складу/поставщику
        addConstraint(addJProp("Выбранная спецификация должна быть поставщика для которого задается свойство", diff2, 2,
                addJProp("", specSupplier, storeSuppSpec, 1, 2), 1, 2), true);

        storeSpecIncl = addJProp(baseGroup, "storeSpecIncl", "Вкл. спец.", equals2, 2,
                                addJProp(true, storeSuppSpec, 1, specSupplier, 2), 1, 2);

        specArticleIncl = addDProp(baseGroup, "specArticleIncl", "Вкл. спец.", LogicalClass.instance, specification, article);
        LP specArticlePrice = addDProp(baseGroup, "specArticlePrice", "Цена по спец.", DoubleClass.instance, specification, article);

        LP inRange = addDProp(baseGroup, "inRange", "В ассорт.", LogicalClass.instance, format, article);
        currentSupplier = addDProp("currentSupplier", "Тек. пост.", supplier, store, article);
        addJProp(supplierGroup, "Имя тек. пост.", name, currentSupplier, 1, 2);

        storeInRange = addJProp(baseGroup, "В ассорт.", inRange, storeFormat, 1, 2);

        // ограничение что текущий поставщик есть в спецификации
        storeSupplArt = addJProp("Вкл. в спец.", specArticleIncl, storeSuppSpec, 1, 2, 3);
        LP storeSupplPrice = addJProp(supplierGroup, "Цена по спец.", specArticlePrice, storeSuppSpec, 1, 2, 3);
        LP storeArtIncl = addJProp("Вкл. в спец.", storeSupplArt, 1, currentSupplier, 1, 2, 2);

        storeSupplIsCurrent = addJProp("Тек.", equals2, currentSupplier, 1, 3, 2);

        addConstraint(addJProp("Текущий поставщик товара должен содержать его в одной из своих спецификаций", andNot1, currentSupplier, 1, 2, storeArtIncl, 1, 2),true);
        
        LP extIncOrderSupplier = addDProp("extIncOrderSupplier", "Поставщик", supplier, extIncomeOrder); // внешний приход
        addJProp(baseGroup, "Имя поставщика", name, extIncOrderSupplier, 1);
        LP extIncOrderStore = addDProp("extIncOrderStore", "Склад (прих.)", store, extIncomeOrder); // внешний приход
        addJProp(baseGroup, "Имя склада", name, extIncOrderStore, 1);

        orderQuantity = addDProp(baseGroup, "orderQuantity", "Кол-во заказа", DoubleClass.instance, extIncomeOrder, article);
        LP orderPrice = addDProp("orderPrice", "Цена заказа (док. фикс.)", DoubleClass.instance, extIncomeOrder, article);
        orderPrice.setDerivedChange(storeSupplPrice, extIncOrderStore, 1, extIncOrderSupplier, 1, 2, orderQuantity, 1, 2);
        addSUProp(baseGroup, "Цена заказа (док.)", Union.OVERRIDE, addJProp("Цена заказа (тек.)", storeSupplPrice, extIncOrderStore, 1, extIncOrderSupplier, 1, 2), orderPrice);
        LP orderVolatility = addDProp(baseGroup,"orderVolatility", "Откл. заказа", DoubleClass.instance, extIncomeOrder);
        orderVolatility.setDerivedChange(addJProp("Откл. скл. пост.", specPriceVolatility, storeSuppSpec, 1, 2), extIncOrderStore, 1, extIncOrderSupplier, 1);
        LP orderMaxPrice = addJProp(baseGroup, "Макс. цена", addPercent, orderPrice, 1, 2, orderVolatility, 1); // ограничение что не больше заданной цены

        orderAllow = addJProp("Актив.", equals2, extIncOrderSupplier, 1, addJProp("Тек. пост.", currentSupplier, extIncOrderStore, 1, 2), 1, 2); 
        orderInSpec = addJProp("В спец. и ассорт.", and1, addJProp("В спец.", storeSupplArt, extIncOrderStore, 1, extIncOrderSupplier, 1, 2), 1, 2,
                            addJProp("В ассорт.", storeInRange, extIncOrderStore, 1, 2), 1, 2);

        LP orderSpec = addDProp(baseGroup, "orderSpec", "Спец. заказа", specification, extIncomeOrder);
        orderSpec.setDerivedChange(storeSuppSpec, extIncOrderStore, 1, extIncOrderSupplier, 1);
        addConstraint(addJProp("Выбранный склад должен быть в спецификации поставщика", andNot1, addJProp(and1, extIncOrderStore, 1, extIncOrderSupplier, 1), 1, orderSpec, 1),true);

        LP extIncDocumentOrder = addDProp("extIncDocumentOrder", "Заказ", extIncomeOrder, extIncomeDocument); // внешний приход

        extIncSupplier = addJProp("Поставщик", extIncOrderSupplier, extIncDocumentOrder, 1); // внешний приход
        addJProp(baseGroup, "Имя поставщика", name, extIncSupplier, 1);
        LP extIncStore = addJProp("Склад (прих.)", extIncOrderStore, extIncDocumentOrder, 1); // внешний приход

        addJProp(baseGroup, "Кол-во заказа", orderQuantity, extIncDocumentOrder, 1, 2);
        addJProp(baseGroup, "Цена заказа", orderPrice, extIncDocumentOrder, 1, 2);

        // сравнение дат
        LP groeqDocDate = addJProp("Дата док.>=Дата", groeq2, date, 1, object(DateClass.instance), 2);
        LP greaterDocDate = addJProp("Дата док.>Дата", greater2, date, 1, object(DateClass.instance), 2);
        LP betweenDocDate = addJProp("Дата док. между Дата", between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3);
        LP equalDocDate = addJProp("Дата док.=Дата", equals2, date, 1, object(DateClass.instance), 2);

        LP intraIncStore = addDProp("intraIncStore", "Склад (прих.)", store, intraDocument); 

        incStore = addCUProp("incStore", true, "Склад (прих.)", extIncStore, intraIncStore);
        LP incStoreName = addJProp(baseGroup, "Имя склада (прих.)", name, incStore, 1);
        outStore = addDProp("outStore", "Склад (расх.)", store, outcomeDocument);
        LP outStoreName = addJProp(baseGroup, "Имя склада (расх.)", name, outStore, 1);

        // количества
        articleQuantity = addDProp("articleQuantity", "Кол-во товара", DoubleClass.instance, articleDocument, article);
        articleQuantity.setDerivedChange(orderQuantity, extIncDocumentOrder, 1, 2);

        exchangeQuantity = addDProp(baseGroup, "exchangeQuantity", "Кол-во перес.", DoubleClass.instance, exchangeDocument, article, article);
        exchIncQuantity = addSGProp(baseGroup, "Прих. перес.", exchangeQuantity, 1, 3);
        exchOutQuantity = addSGProp(baseGroup, "Расх. перес.", exchangeQuantity, 1, 2);
        LP exchDltQuantity = addDUProp("Кол-во перес. (тов.)", exchOutQuantity, exchIncQuantity);

        invBalance = addDProp(baseGroup, "invBalance", "Остаток инв.", DoubleClass.instance, invDocument, article);
        invDBBalance = addDProp(baseGroup, "invDBBalance", "Остаток (по учету)", DoubleClass.instance, invDocument, article);

        // количества по возврату
        returnQuantity = addDProp(baseGroup, "returnQuantity", "Кол-во к возвр.", DoubleClass.instance, returnDocument, extIncomeDocument, article);
        LP returnDocQuantity = addSGProp("Возвр. кол-во (расх.)", returnQuantity, 1, 3);
        LP returnedQuantity = addSGProp("Возвр. кол-во (прих.)", returnQuantity, 2, 3);

        LP invQuantity = addDUProp("invQuantity","Кол-во инв.", invDBBalance, invBalance);

        quantity = addCUProp(baseGroup, "quantity", true, "Кол-во", articleQuantity, exchDltQuantity, invQuantity, returnDocQuantity);

        // кол-во по документу
        LP documentQuantity = addSGProp(baseGroup, "documentQuantity", "Кол-во (всего)", quantity, 1);

        // остатки
        LP incStoreQuantity = addSGProp("incStoreQuantity", "Прих. на скл.", quantity, incStore, 1, 2);
        LP outStoreQuantity = addSGProp("outStoreQuantity", "Расх. со скл.", quantity, outStore, 1, 2);
        balanceStoreQuantity = addDUProp(baseGroup, "balanceStoreQuantity", true, "Ост. на скл.", incStoreQuantity, outStoreQuantity);

        LP balanceQuantity = addSGProp(baseGroup, "balanceQuantity", "Ост. тов.", balanceStoreQuantity, 2);

        // остатки для документов
        docOutBalanceQuantity = addJProp(baseGroup, "Остаток (расх.)", balanceStoreQuantity, outStore, 1, 2);
        docIncBalanceQuantity = addJProp(baseGroup, "Остаток (прих.)", balanceStoreQuantity, incStore, 1, 2);

        LP extIncQuantity = addJProp("Кол-во прих. от пост.", and1, quantity, 1, 2, is(extIncomeDocument), 1);
        LP extIncRemainQuantity = addDUProp("Кол-во прих. (ост.)", extIncQuantity, returnedQuantity);

        addJProp(baseGroup, "Остаток (прих.)", balanceStoreQuantity, extIncOrderStore, 1, 2);
        
//        LF remainDocQuantity = addUGProp(baseGroup, "Остаток по док.", false, extIncRemainQuantity, balanceQuantity, 2, date, 1, 1);

        invDBBalance.setDerivedChange(balanceStoreQuantity, outStore, 1, 2);

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


        // Надбавка
        revalAdd = addDProp(baseGroup, "revalAdd", "Надбавка", DoubleClass.instance, revalDocument, article);
        LP[] maxRevalProps = addMGProp(lastDocumentGroup, new String[]{"currentRevalDate","currentRevalDoc"}, new String[]{"Дата посл. переоц.","Посл. док. переоц."}, 1,
                addJProp("Дата переоц.", and1, date, 1, revalAdd, 1, 2), 1, 2, revalFormat, 1);
        currentRevalDate = maxRevalProps[0]; currentRevalDoc = maxRevalProps[1];
        addPersistent(currentRevalDate); addPersistent(currentRevalDoc);

        currentAdd = addJProp(currentGroup, "currentAdd", true, "Надбавка (тек. форм.)", revalAdd, currentRevalDoc, 1, 2, 1);
        LP currentStoreAdd = addJProp(currentGroup, "Надбавка (тек. скл.)",currentAdd, 2, storeFormat, 1);
        revalCurrentAdd = addJProp(baseGroup, "Надбавка (тек. док.)", currentAdd, 2, revalFormat, 1);

        // НДС
        taxVatOut = addDProp(baseGroup, "taxVatOut", "НДС розн.", DoubleClass.instance, taxDocument, article);
        LP[] maxTaxProps = addMGProp(lastDocumentGroup, new String[]{"currentTaxDate","currentTaxDoc"}, new String[]{"Дата посл. НДС","Посл. док. НДС"}, 1,
                addJProp("Дата НДС", and1, date, 1, taxVatOut, 1, 2), 1, 2);
        currentTaxDate = maxTaxProps[0]; currentTaxDoc = maxTaxProps[1];
        addPersistent(currentTaxDate); addPersistent(currentTaxDoc);
        currentVatOut = addJProp(currentGroup, "currentVatOut", true, "НДС (тек.)", taxVatOut, currentTaxDoc, 1, 1);

        // Местный налог
        locTaxValue = addDProp(baseGroup, "locTaxValue", "Местн. нал.", DoubleClass.instance, locTaxDocument, article);
        LP[] maxLocTaxProps = addMGProp(lastDocumentGroup, new String[]{"currentLocTaxDate","currentLocTaxDoc"}, new String[]{"Дата посл. местн. нал.","Посл. док. местн. нал."}, 1,
                addJProp("Дата переоц.", and1, date, 1, locTaxValue, 1, 2), 1, 2, locTaxRegion, 1);
        currentLocTaxDate = maxLocTaxProps[0]; currentLocTaxDoc = maxLocTaxProps[1];
        addPersistent(currentLocTaxDate); addPersistent(currentLocTaxDoc);
        currentLocTax = addJProp(currentGroup, "currentLocTax", true, "Местн. нал. (тек. рег.)", locTaxValue, currentLocTaxDoc, 1, 2, 1);
        LP currentStoreLocTax = addJProp(currentGroup, "Местн. нал. (тек. скл.)",currentLocTax, 2, storeRegion, 1);
        locCurrentTax = addJProp(baseGroup, "Местн. нал. (тек. док.)", currentLocTax, 2, locTaxRegion, 1);

        // цены поставщика
        extIncPriceIn = addDProp(baseGroup, "extIncPriceIn", "Цена пост.", DoubleClass.instance, extIncomeDocument, article);
        extIncPriceIn.setDerivedChange(orderPrice, extIncDocumentOrder, 1, 2, quantity, 1, 2);
        addConstraint(addJProp("Отклонение от цены заказа превышает допустимое значение", greater2, extIncPriceIn, 1, 2, addJProp("Макс. цена док.", orderMaxPrice, extIncDocumentOrder, 1, 2), 1, 2),true);
        extIncVATIn = addDProp(baseGroup, "extIncVATIn", "НДС (пост.)", DoubleClass.instance, extIncomeDocument, article);
        extIncVATIn.setDerivedChange(currentVatOut, 2, articleQuantity, 1, 2);

        LP outPriceIn = addDProp("outPriceIn", "Цена пост. (расх.)", DoubleClass.instance, outcomeDocument, article);
        LP priceIn = addCUProp("priceIn", "Цена пост. (фикс.)", extIncPriceIn, outPriceIn);

        LP[] maxIncProps = addMGProp(lastDocumentGroup, new String[]{"currentIncDate","currentIncDoc"}, new String[]{"Дата посл. прих. по скл.","Посл. прих. по скл."}, 1,
                addJProp("Дата прих. (кол-во)", and1, date, 1, quantity, 1, 2), 1, incStore, 1, 2);
        currentIncDate = maxIncProps[0]; currentIncDoc = maxIncProps[1];
        addPersistent(currentIncDate); addPersistent(currentIncDoc);
        currentPriceIn = addJProp(currentGroup, "currentPriceIn", true, "Цена пост. (тек.)", priceIn, currentIncDoc, 1, 2, 2);

        outPriceIn.setDerivedChange(currentPriceIn, outStore, 1, 2, quantity, 1, 2); // подставляем тек. цену со склада расх
        addSUProp(baseGroup, "Цена пост. (док. расх.)",Union.OVERRIDE, addJProp("Цена пост. (док. расх. тек.)", currentPriceIn, outStore, 1, 2), outPriceIn);

        // розничная цена
        currentPriceOut = addJProp(currentGroup, "currentPriceOut", true, "Цена розн. (тек.)", roundm1,
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
        LP documentVatOut = addDProp("documentVatOut", "НДС (фикс.)", DoubleClass.instance, outcomeDocument, article);
        documentVatOut.setDerivedChange(currentVatOut, 2, quantity, 1, 2);
        addSUProp(baseGroup, "НДС (док.)", Union.OVERRIDE, addJProp("",and1,currentVatOut,2,is(outcomeDocument),1), documentVatOut);
        LP outPriceOut = addDProp("outPriceOut", "Цена розн. (расх.)", DoubleClass.instance, outcomeDocument, article);
        outPriceOut.setDerivedChange(currentPriceOut, outStore, 1, 2, quantity, 1, 2);
        docOutPriceOut = addSUProp(baseGroup, "Цена розн. (док. расх.)",Union.OVERRIDE, addJProp("Цена розн. (док. расх. тек.)", currentPriceOut, outStore, 1, 2), outPriceOut);

        LP detailSumOut = addJProp(baseGroup, "detailSumOut", "Сумма розн. (расх.)", round,
                addJProp("", multiplyDouble2, quantity, 1, 2, outPriceOut, 1, 2), 1, 2);
        LP detailSumVatOut = addJProp(baseGroup, "Сумма НДС (розн. расх.)", round,
                addJProp("", percent, detailSumOut, 1, 2, documentVatOut, 1, 2), 1, 2);

        // расх. суммы по документам
        LP documentSumOut = addSGProp(baseGroup, "documentSumOut", "Сумма розн.", detailSumOut, 1);
        LP documentSaleVatOut = addSGProp(baseGroup, "documentSaleVatOut", "Сумма НДС (розн.)", detailSumVatOut, 1);

        // свойства по суммам розничным за период
        saleBetweenDateSum = addJProp("Сумма розн. за период", and1, detailSumOut, 1, 2, betweenDocDate, 1, 3, 4);
        LP saleStoreBetweenDateSum = addSGProp("Реализация по складу за период", saleBetweenDateSum, outStore, 1, 2, 3, 4);
        saleFormatArticleBetweenDateSum = addSGProp("Сумма реализ. за период", saleStoreBetweenDateSum, storeFormat, 1, 2, 3, 4);
        wholesaleArticleBetweenDateSum = addSGProp(baseGroup, "Сумма реализации за период", saleFormatArticleBetweenDateSum, 1, 3, 4);
        wholesaleFormatBetweenDateSum = addSGProp(baseGroup, "Сумма реализации за период", saleFormatArticleBetweenDateSum, 2, 3, 4);

        saleGroupBetweenDateSum = addSGProp("Реализ. по группе за период", saleBetweenDateSum, 1, artGroup, 2, 3, 4);

        saleFormatArticleBetweenDateQuantity = addSGProp(baseGroup, "Кол-во реал. по товару за период", saleStoreArticleBetweenDateQuantity, storeFormat, 1, 2, 3, 4);

       // свойства групп товаров по реализации по формату
       // реализация по группе-формату
       saleFormatGroupBetweenDateSum = addSGProp("Сумма реализ. за период", saleFormatArticleBetweenDateSum, 1, artGroup, 2, 3, 4);
        
       // АВС-категория группы в формате
       resultABCGroup = initABCAnalyze(addOProp(null,"Процент ABC группы", saleFormatGroupBetweenDateSum, true, false, true, 3, 1, 3, 4, saleFormatGroupBetweenDateSum, 1, 2, 3, 4),"группы");

        // ABC-категория по товарам в группе - кол-во
       resultABCInsideGroupQuantity = initABCAnalyze(addOProp(null, "Процент ABC товара в группе по кол-ву", saleFormatArticleBetweenDateQuantity, true, false, true, 4, 1, artGroup, 2, 3, 4, saleFormatArticleBetweenDateQuantity, 1, 2, 3, 4),"товара по кол-ву в группе");

        // ABC-категория по товарам в группе - сумма
        resultABCInsideGroupSum = initABCAnalyze(addOProp(null, "Процент ABC товара в группе по сумме", saleFormatArticleBetweenDateSum, true, false, true, 4, 1, artGroup, 2, 3, 4, saleFormatArticleBetweenDateSum, 1, 2, 3, 4),"товара по сумме в группе");

       // АВС-категория товара по формату
        resultABCSum = initABCAnalyze(addOProp(null, "Процент ABC товара", saleFormatArticleBetweenDateSum, true, false, true, 3, 1, 3, 4, saleFormatArticleBetweenDateSum, 1, 2, 3, 4),"товара");

        // изменение цен
        LP incPriceOutChange = addDProp("incPriceOutChange", "Цена розн. (прих.)", DoubleClass.instance, incomeDocument, article);
        incPriceOutChange.setDerivedChange(currentPriceOut, true, incStore, 1, 2, quantity, 1, 2);

        LP revalPriceOutChange = addDProp(baseGroup, "revalPriceOutChange", "Цена розн. (переоц.)", DoubleClass.instance, revalDocument, store, article);
        revalPriceOutChange.setDerivedChange(currentPriceOut, true, 2, 3, revalAdd, 1, 3, addJProp("Склад документа (форм.)", equals2, revalFormat, 1, storeFormat, 2), 1, 2);

        LP taxPriceOutChange = addDProp(baseGroup, "taxPriceOutChange", "Цена розн. (НДС)", DoubleClass.instance, taxDocument, store, article);
        taxPriceOutChange.setDerivedChange(currentPriceOut, true, 2, 3, taxVatOut, 1, 3);

        LP locTaxPriceOutChange = addDProp(baseGroup, "locTaxPriceOutChange", "Цена розн. (местн. нал.)", DoubleClass.instance, locTaxDocument, store, article);
        locTaxPriceOutChange.setDerivedChange(currentPriceOut, true, 2, 3, locTaxValue, 1, 3, addJProp("Склад документа (рег.)", equals2, locTaxRegion, 1, storeRegion, 2), 1, 2);

        // цена для документа
        priceOutChange = addCUProp(baseGroup,"Цена розн. по док.", revalPriceOutChange, taxPriceOutChange, locTaxPriceOutChange,
                                        addJProp("Цена розн. по скл. (прих.)", and1,  // цена для прихода склада - вытащим склад в интерфейс, для объединения с reval
                                                incPriceOutChange, 1, 3,
                                            addJProp("Склад документа", equals2, incStore, 1, 2), 1, 2));
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

    FormEntity extIncDetailForm, extIncForm;
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

        NavigatorElement supplyManagement = new NavigatorElement(baseElement, 1000, "Управление поставками");
            NavigatorElement suppliers = new StoreSupplierSpecFormEntity(supplyManagement, 1100, "Поставщики");
            FormEntity extIncOrderForm = new ExtIncOrderFormEntity(supplyManagement, 1200, "Заказы поставщику");
            FormEntity extIncForm = new ExtIncFormEntity(supplyManagement, 1300, "Приходы от поставщика");
            FormEntity returnForm = new ReturnFormEntity(supplyManagement, 1400, "Возврат поставщику");
            FormEntity supplierStoreArticleForm = new SupplierStoreArticleFormEntity(supplyManagement, 1500, "Товары поставщика");
            FormEntity supplyRangeForm = new SupplyRangeFormEntity(supplyManagement, 1600, "Обеспечение ассортимента");

        NavigatorElement rangeManagement = new NavigatorElement(baseElement, 2000, "Управление ассортиментом");
            FormEntity rangeGroupForm = new RangeGroupFormEntity(rangeManagement, 2100, "Ассортимент по группам");
            FormEntity rangeFormatForm = new RangeFormEntity(rangeManagement, 2200, "Ассортимент по форматам", true);
            FormEntity rangeArticleForm = new RangeFormEntity(rangeManagement, 2300, "Ассортимент по товарам", false);
            FormEntity remainRangeForm = new RemainRangeFormEntity(rangeManagement, 2400, "Активные товары на складах");

        NavigatorElement distrManagement = new NavigatorElement(baseElement, 3000, "Управление распределением");
            FormEntity intraForm = new IntraFormEntity(distrManagement, 3100, "Внутреннее перемещение");
            FormEntity storeArticleForm = new StoreArticleFormEntity(distrManagement, 3200, "Товары по складам");

        NavigatorElement storeManagement = new NavigatorElement(baseElement, 4000, "Управление хранением");
            FormEntity invForm = new InvFormEntity(storeManagement, 4100, "Инвентаризация", false);
            FormEntity exchangeForm = new ExchangeFormEntity(storeManagement, 4200, "Пересорт");
                FormEntity exchangeMForm = new ExchangeMFormEntity(exchangeForm, 4210, "Сводный пересорт");

        NavigatorElement saleManagement = new NavigatorElement(baseElement, 5000, "Управление продажами");
            FormEntity cashSaleForm = new CashSaleFormEntity(saleManagement, 5100, "Реализация по кассе");
            FormEntity clearingSaleForm = new ClearingSaleFormEntity(saleManagement, 5200, "Реализация по б/н расчету");
            FormEntity salesArticleStoreForm = new SalesArticleStoreFormEntity(saleManagement, 5300, "Реализация товара по складам");

        NavigatorElement valueManagement = new NavigatorElement(baseElement, 6000, "Управление ценообразованием");
            FormEntity revalueForm = new RevalueFormEntity(valueManagement, 6100, "Изменение наценок");
            FormEntity storeArticlePrimDocForm = new StoreArticlePrimDocFormEntity(valueManagement, 6200, "Изменение цен по товарам");

        NavigatorElement taxManagement = new NavigatorElement(baseElement, 7000, "Управление налогами");
            FormEntity taxForm = new TaxFormEntity(taxManagement, 7100, "Изменение НДС");
            FormEntity locTaxForm = new LocTaxFormEntity(taxManagement, 7200, "Изменение местн. нал.");

        NavigatorElement aggregateData = new NavigatorElement(baseElement, 8000, "Сводная информация");
            FormEntity storeArticleDocForm = new StoreArticleDocFormEntity(aggregateData, 8100, "Движение товара");
            FormEntity articleMStoreForm = new ArticleMStoreFormEntity(aggregateData, 8200, "Сводные остатки");
        FormEntity articleStoreForm = new ArticleStoreFormEntity(aggrArticleData, 8300, "Склады по товарам");

        intraDocument.addRelevant(intraForm);
        clearingSaleDocument.addRelevant(clearingSaleForm);
        invDocument.addRelevant(invForm);
        exchangeDocument.addRelevant(exchangeForm);
        revalDocument.addRelevant(revalueForm);

//        extIncDetailForm.addRelevantElement(extIncPrintForm);
    }

    private class TmcFormEntity extends FormEntity {

        TmcFormEntity(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);
        }

        TmcFormEntity(NavigatorElement parent, int iID, String caption, boolean isPrintForm) {
            super(parent, iID, caption, isPrintForm);
        }

        void addArticleRegularFilterGroup(PropertyObjectEntity documentProp, PropertyObjectEntity... extraProps) {

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(documentProp),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));

            int functionKey = KeyEvent.VK_F9;

            for (PropertyObjectEntity extraProp : extraProps) {
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      new NotNullFilterEntity(extraProp),
                                      extraProp.property.caption,
                                      KeyStroke.getKeyStroke(functionKey--, 0)));
            }
            addRegularFilterGroup(filterGroup);
        }
    }

    private class ExtIncOrderFormEntity extends TmcFormEntity {

        public ExtIncOrderFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption, false);

            ObjectEntity objDoc = addSingleGroupObject(extIncomeOrder, "Заказ", baseGroup);

            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup);

            addPropertyDraw(objDoc, objArt, baseGroup);

            PropertyObjectEntity orderImp = addPropertyObject(orderQuantity, objDoc, objArt);
            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(orderImp),
                    new NotNullFilterEntity(addPropertyObject(orderAllow, objDoc, objArt))));
            addArticleRegularFilterGroup(orderImp);
        }
    }

    private class ExtIncFormEntity extends TmcFormEntity {

        public ExtIncFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption, false);

            ObjectEntity objDoc = addSingleGroupObject(extIncomeDocument, "Документ", baseGroup);

            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup);

            addPropertyDraw(objDoc, objArt, baseGroup);

            addArticleRegularFilterGroup(getPropertyObject(quantity));
        }
    }
/*
    private class ExtIncPrintNavigatorForm extends ExtIncFormEntity {

        public ExtIncPrintNavigatorForm(NavigatorElement parent, int ID, String title) throws JRException, FileNotFoundException {
            super(parent, ID, title, true);

            objDoc.groupTo.initClassView = false;
            objDoc.groupTo.singleViewType = true;

            addPropertyDraw(objDoc, property, baseGroup);
            addPropertyDraw(objDetail, property, baseGroup);

            objDoc.sID = "objDoc";
            getPropertyDraw(name.property, objDoc.groupTo).sID = "docName";

            //
            reportDesign = JRXmlLoader.load(getClass().getResourceAsStream("/tmc/reports/extIncLog.jrxml"));
        }
    }
  */
    private class IntraFormEntity extends TmcFormEntity {

        public IntraFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(intraDocument, "Документ", baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup);

            addPropertyDraw(objDoc, objArt, baseGroup);

            addArticleRegularFilterGroup(getPropertyObject(quantity), getPropertyObject(docOutBalanceQuantity), getPropertyObject(docIncBalanceQuantity));

            addHintsNoUpdate(currentIncDate);
            addHintsNoUpdate(currentIncDoc);
        }
    }

    private class CashSaleFormEntity extends TmcFormEntity {

        public CashSaleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(cashSaleDocument, "Документ", baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup, true);

            addPropertyDraw(objDoc, objArt, baseGroup);

            addArticleRegularFilterGroup(getPropertyObject(quantity), getPropertyObject(docOutBalanceQuantity));
        }
    }

    private class ClearingSaleFormEntity extends TmcFormEntity {

        public ClearingSaleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(clearingSaleDocument, "Документ", baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup, true);

            addPropertyDraw(objDoc, objArt, baseGroup);

            addArticleRegularFilterGroup(getPropertyObject(quantity), getPropertyObject(docOutBalanceQuantity));
        }
    }

    private class InvFormEntity extends TmcFormEntity {

        public InvFormEntity(NavigatorElement parent, int ID, String caption, boolean groupStore) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(invDocument, "Документ", baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup, true);

            addPropertyDraw(objDoc, objArt, baseGroup);

            addArticleRegularFilterGroup(getPropertyObject(quantity), getPropertyObject(docOutBalanceQuantity));
        }
    }

    private class ReturnFormEntity extends TmcFormEntity {

        public ReturnFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(returnDocument, "Документ", baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup, true);
            ObjectEntity objExtInc = addSingleGroupObject(extIncomeDocument, "Приход", baseGroup, true);

            addPropertyDraw(objDoc, objArt, baseGroup);
            addPropertyDraw(objDoc, objExtInc, objArt, baseGroup);
            addPropertyDraw(objExtInc, objArt, baseGroup);

            addArticleRegularFilterGroup(addPropertyObject(quantity, objDoc, objArt),
                    addPropertyObject(docOutBalanceQuantity, objDoc, objArt));

            addArticleRegularFilterGroup(addPropertyObject(returnQuantity, objDoc, objExtInc, objArt));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantity, objExtInc, objArt)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(extIncSupplier,objExtInc), Compare.EQUALS, addPropertyObject(returnSupplier,objDoc)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(incStore,objExtInc), Compare.EQUALS, addPropertyObject(outStore,objDoc)));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyDraw(date,objExtInc)), false);
            formView.defaultOrders.put(formView.get(getPropertyDraw(objectValue,objExtInc)), false);
            richDesign = formView;
        }
    }

    private class ExchangeFormEntity extends TmcFormEntity {

        public ExchangeFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(exchangeDocument, "Документ", baseGroup);
            ObjectEntity objArtTo = addSingleGroupObject(article, "Товар (на)",
                    baseGroup);
            ObjectEntity objArtFrom = addSingleGroupObject(article, "Товар (c)",
                    baseGroup);

            addPropertyDraw(objDoc, objArtTo, baseGroup);
            addPropertyDraw(exchangeQuantity, objDoc, objArtFrom, objArtTo);
            addPropertyDraw(objDoc, objArtFrom, baseGroup);

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
                                  new CompareFilterEntity(getPropertyObject(docOutPriceOut, objArtFrom), Compare.EQUALS, getPropertyObject(docOutPriceOut, objArtTo)),
                                  "Одинаковая розн. цена",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK)));
            addRegularFilterGroup(filterGroup);

        }
    }

    private class ExchangeMFormEntity extends TmcFormEntity {

        public ExchangeMFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(exchangeDocument, "Документ", baseGroup);

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

        public RevalueFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(revalDocument, "Документ", baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup);

            addPropertyDraw(objDoc, objArt, baseGroup);

            addHintsNoUpdate(currentRevalDate);
            addHintsNoUpdate(currentRevalDoc);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(revalAdd, objDoc, objArt)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(revalCurrentAdd, objDoc, objArt))),
                                  "Не заданы значения",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class TaxFormEntity extends TmcFormEntity {

        public TaxFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(taxDocument, "Документ", baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup, currentVatOut);

            addPropertyDraw(objDoc, objArt, baseGroup);

            addHintsNoUpdate(currentTaxDate);
            addHintsNoUpdate(currentTaxDoc);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(taxVatOut, objDoc, objArt)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(currentVatOut, objArt))),
                                  "Не заданы значения",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class LocTaxFormEntity extends TmcFormEntity {

        public LocTaxFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(locTaxDocument, "Документ", baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup);

            addPropertyDraw(objDoc, objArt, baseGroup);

            addHintsNoUpdate(currentLocTaxDate);
            addHintsNoUpdate(currentLocTaxDoc);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(locTaxValue, objDoc, objArt)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(locCurrentTax, objDoc, objArt))),
                                  "Не заданы значения",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class StoreArticleFormEntity extends TmcFormEntity {

        ObjectEntity objStore, objArt;

        public StoreArticleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objStore = addSingleGroupObject(store, "Склад", baseGroup);
            objStore.groupTo.initClassView = ClassViewType.PANEL;
            objStore.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID));

            objArt = addSingleGroupObject(article, "Товар", baseGroup);

            addPropertyDraw(objStore, objArt, baseGroup);
        }
    }

    private class StoreArticlePrimDocFormEntity extends StoreArticleFormEntity {

        public StoreArticlePrimDocFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objPrimDoc = addSingleGroupObject(priceOutDocument, "Документ", baseGroup);

            addPropertyDraw(objPrimDoc, objArt, baseGroup);

            addPropertyDraw(objPrimDoc, objStore, objArt, priceOutChange);

            addPropertyDraw(objArt, lastDocumentGroup, currentGroup);

            addPropertyDraw(objStore, objArt, lastDocumentGroup, currentGroup);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(priceOutChange)));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyDraw(date)), false);
            formView.defaultOrders.put(formView.get(getPropertyDraw(objectValue, objPrimDoc)), false);
            richDesign = formView;
        }
    }

    private class StoreArticleDocFormEntity extends StoreArticleFormEntity {

        public StoreArticleDocFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(quantityDocument, "Товарный документ", baseGroup, date, incStore, outStore);

            addPropertyDraw(objDoc, objArt, baseGroup);
//            addPropertyDraw(property, baseGroup, true, objDoc);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(quantity)));
            addFixedFilter(new OrFilterEntity(
                                new CompareFilterEntity(getPropertyObject(incStore),Compare.EQUALS,objStore),
                                new CompareFilterEntity(getPropertyObject(outStore),Compare.EQUALS,objStore)));

            DefaultFormView formView = new DefaultFormView(this);
            formView.defaultOrders.put(formView.get(getPropertyDraw(date)), false);
            richDesign = formView;
        }
    }

    private class ArticleStoreFormEntity extends TmcFormEntity {

        ObjectEntity objStore, objArt;

        public ArticleStoreFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            objArt = addSingleGroupObject(article, "Товар", baseGroup);
            objStore = addSingleGroupObject(store, "Склад", baseGroup);

            addPropertyDraw(objStore, objArt, baseGroup);
//            addPropertyDraw(property, baseGroup, false, objArt.groupTo, objStore, objArt);
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

            addPropertyDraw(objArt, baseGroup);
            addPropertyDraw(objStore, baseGroup);

            addPropertyDraw(objStore, objArt, baseGroup);
        }
    }

    private class SupplierStoreArticleFormEntity extends TmcFormEntity {

        SupplierStoreArticleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objSupplier = addSingleGroupObject(supplier, "Поставщик", baseGroup);
            objSupplier.groupTo.initClassView = ClassViewType.PANEL;
            objSupplier.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID));

            GroupObjectEntity gobjArtStore = new GroupObjectEntity(genID());

            ObjectEntity objArt = new ObjectEntity(genID(), article, "Товар");
            ObjectEntity objStore = new ObjectEntity(genID(), store, "Склад");

            gobjArtStore.add(objArt);
            gobjArtStore.add(objStore);
            addGroup(gobjArtStore);

            addPropertyDraw(objStore, baseGroup);
            addPropertyDraw(objArt, baseGroup);
            addPropertyDraw(objStore, objArt, baseGroup, supplierGroup);
            addPropertyDraw(objStore, objSupplier, objArt, storeSupplIsCurrent);

            // установить фильтр по умолчанию на поставщик товара = поставщик
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(storeSupplArt, objStore, objSupplier, objArt)));

            // добавить стандартные фильтры
            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(storeSupplIsCurrent, objStore, objSupplier, objArt)),
                                  "Текущий этого пост.",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
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

    private class SupplyRangeFormEntity extends FormEntity {

        public SupplyRangeFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objStore = addSingleGroupObject(store, "Склад", baseGroup, currentGroup);
            ObjectEntity objArticle = addSingleGroupObject(article, "Товар", baseGroup, currentGroup);
            ObjectEntity objSupplier = addSingleGroupObject(supplier, "Поставщик", baseGroup, currentGroup);

            addPropertyDraw(objStore, objArticle, baseGroup, currentGroup, supplierGroup);
            addPropertyDraw(objStore, objSupplier, objArticle, baseGroup, currentGroup, supplierGroup, storeSupplIsCurrent);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(storeInRange, objStore, objArticle)));
            addSingleRegularFilterGroup(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(currentSupplier, objStore, objArticle))),
                                        "Без поставщиков",
                                        KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(storeSupplArt, objStore, objSupplier, objArticle)));

        }
    }

    private class RemainRangeFormEntity extends FormEntity {

        public RemainRangeFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            GroupObjectEntity gobjArtStore = new GroupObjectEntity(genID());

            ObjectEntity objArt = new ObjectEntity(genID(), article, "Товар");
            ObjectEntity objStore = new ObjectEntity(genID(), store, "Склад");

            gobjArtStore.add(objArt);
            gobjArtStore.add(objStore);
            addGroup(gobjArtStore);

            addPropertyDraw(objArt, baseGroup, currentGroup, supplierGroup);
            addPropertyDraw(objStore, baseGroup, currentGroup, supplierGroup);
            addPropertyDraw(objStore, objArt, baseGroup, currentGroup, supplierGroup);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(storeInRange, objStore, objArt)));
            addSingleRegularFilterGroup(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(balanceStoreQuantity, objStore, objArt))),
                    "Нет на складе",KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
        }
    }
  // Benderamania
    private class RangeFormEntity extends DateIntervalFormEntity {

        public RangeFormEntity(NavigatorElement parent, int ID, String caption, boolean upFormat) {
            super(parent, ID, caption);

            ObjectEntity objFormat, objArticle;
            if(upFormat) {
                objFormat = addSingleGroupObject(format, "Формат", baseGroup, currentGroup);
                objFormat.groupTo.initClassView = ClassViewType.PANEL;
                objArticle = addSingleGroupObject(article, "Товар", baseGroup, currentGroup);
            } else {
                objArticle = addSingleGroupObject(article, "Товар", baseGroup, currentGroup);
                objFormat = addSingleGroupObject(format, "Формат", baseGroup, currentGroup);
            }

            ObjectEntity objStore = addSingleGroupObject(store, "Склад", baseGroup, currentGroup);

            addPropertyDraw(objFormat, objArticle, baseGroup, currentGroup);
            addPropertyDraw(objArticle, objStore, baseGroup, supplierGroup, currentGroup);
            addPropertyDraw(objFormat, objArticle, objDateFrom, objDateTo, saleFormatArticleBetweenDateSum,  resultABCSum);

            PropertyObjectEntity storeFormatImplement = addPropertyObject(storeFormat, objStore);
            addFixedFilter(new CompareFilterEntity(storeFormatImplement, Compare.EQUALS, objFormat));
        }
    }

    private class RangeGroupFormEntity extends DateIntervalFormEntity {

        public RangeGroupFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objFormat, objArticle, objGroup;
            objFormat = addSingleGroupObject(format, "Формат", baseGroup, currentGroup);
            objGroup = addSingleGroupObject(articleGroup, "Группа", baseGroup, currentGroup);
            objArticle = addSingleGroupObject(article, "Товар", baseGroup, currentGroup);
            ObjectEntity objStore = addSingleGroupObject(store, "Склад", baseGroup, currentGroup);

            addPropertyDraw(objArticle, objGroup, baseGroup, currentGroup);
            addPropertyDraw(objFormat, objArticle, baseGroup, currentGroup);
            addPropertyDraw(objArticle, objStore, baseGroup, supplierGroup, currentGroup);
            addPropertyDraw(objFormat, objArticle, objDateFrom, objDateTo, saleFormatArticleBetweenDateQuantity, resultABCInsideGroupQuantity, saleFormatArticleBetweenDateSum, resultABCInsideGroupSum);
            addPropertyDraw(objFormat, objGroup, objDateFrom, objDateTo, saleFormatGroupBetweenDateSum, resultABCGroup);

            PropertyObjectEntity artGroupImplement = addPropertyObject(artGroup, objArticle);
            addFixedFilter(new CompareFilterEntity(artGroupImplement, Compare.EQUALS, objGroup));

            PropertyObjectEntity storeFormatImplement = addPropertyObject(storeFormat, objStore);
            addFixedFilter(new CompareFilterEntity(storeFormatImplement, Compare.EQUALS, objFormat));
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

            addPropertyDraw(remainStoreArticleStartQuantity, objStore, objArticle, objDateFrom);
            addPropertyDraw(incBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
            addPropertyDraw(outBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
            addPropertyDraw(remainStoreArticleEndQuantity, objStore, objArticle, objDateTo);
            addPropertyDraw(saleStoreArticleBetweenDateQuantity, objStore, objArticle, objDateFrom, objDateTo);
        }
    }

    private class StoreSupplierSpecFormEntity extends FormEntity {

        public StoreSupplierSpecFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objSupplier = addSingleGroupObject(supplier, "Поставщик", baseGroup);
            ObjectEntity objContract = addSingleGroupObject(contract, "Договор", baseGroup, supplierGroup);
            ObjectEntity objSpec = addSingleGroupObject(specification, "Спецификация", baseGroup, contractGroup);
            ObjectEntity objStore = addSingleGroupObject(store, "Склад", baseGroup, formatGroup, regionGroup);
            ObjectEntity objArticle = addSingleGroupObject(article, "Товар", baseGroup);

            // связываем объекты друг с другом
            PropertyObjectEntity contractSupplierImplement = addPropertyObject(contractSupplier, objContract);
            addFixedFilter(new CompareFilterEntity(contractSupplierImplement, Compare.EQUALS, objSupplier));

            PropertyObjectEntity specContractImplement = addPropertyObject(specContract, objSpec);
            addFixedFilter(new CompareFilterEntity(specContractImplement, Compare.EQUALS, objContract));

            // добавляем фильтров по умолчанию
            addSingleRegularFilterGroup(new NotNullFilterEntity(addPropertyObject(storeSpecIncl, objStore, objSpec)),
                                        "В спецификации",
                                        KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));

            addSingleRegularFilterGroup(new NotNullFilterEntity(addPropertyObject(specArticleIncl, objSpec, objArticle)),
                                        "В спецификации",
                                        KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));

            // добавляем множественные свойства
            addPropertyDraw(objStore, objSpec, baseGroup);
            addPropertyDraw(objSpec, objArticle, baseGroup);

            // указываем, что верхние три товара будут идти в панель
            objSupplier.groupTo.initClassView = ClassViewType.PANEL;
            objSupplier.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID));

            objContract.groupTo.initClassView = ClassViewType.PANEL;

            objSpec.groupTo.initClassView = ClassViewType.PANEL;

        }
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {

        SecurityPolicy securityPolicy = addPolicy("Базовая политка", "Запрещает использование чего-то тут...");

        securityPolicy.property.view.deny(currentGroup.getProperties());
//        securityPolicy.property.change.deny(extIncDetailArticle.property);
//        securityPolicy.property.change.deny(extIncDetailQuantity.property);

//        securityPolicy.navigator.deny(analyticsData.getChildren(true));
//        securityPolicy.navigator.deny(extIncPrintForm);

        securityPolicy.cls.edit.add.deny(document.getConcreteChildren());
        securityPolicy.cls.edit.remove.deny(baseGroup.getClasses());

        User admin = addUser("admin", "fusion");
        User user = addUser("user", "");
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
//        classQuantity.put(extIncomeDetail, modifier *100*propModifier);
        classQuantity.put(intraDocument, modifier);
        classQuantity.put(cashSaleDocument, modifier);
        classQuantity.put(clearingSaleDocument,((Double)(modifier *0.5)).intValue());
        classQuantity.put(invDocument,((Double)(modifier *0.2)).intValue());
        classQuantity.put(returnDocument,((Double)(modifier *0.3)).intValue());
        classQuantity.put(exchangeDocument, modifier);
        classQuantity.put(revalDocument,((Double)(modifier *0.5)).intValue());

        Map<DataProperty, Set<ClassPropertyInterface>> propNotNulls = new HashMap<DataProperty, Set<ClassPropertyInterface>>();
        artGroup.putNotNulls(propNotNulls,0);
        outStore.putNotNulls(propNotNulls,0);
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
  */
}
