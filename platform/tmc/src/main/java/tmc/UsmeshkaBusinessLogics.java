package tmc;

import net.sf.jasperreports.engine.JRException;

import java.sql.SQLException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.rmi.registry.LocateRegistry;
import java.awt.event.KeyEvent;

import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.MSSQLDataAdapter;
import platform.server.data.Union;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.linear.LP;
import platform.server.logics.property.AggregateProperty;
import platform.server.classes.*;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.auth.User;
import platform.interop.UserInfo;

import javax.swing.*;


public class UsmeshkaBusinessLogics extends BusinessLogics<UsmeshkaBusinessLogics> {

    public UsmeshkaBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    //    static Registry registry;
    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, FileNotFoundException, JRException, MalformedURLException {

        System.out.println("Server is starting...");
//        DataAdapter adapter = new PostgreDataAdapter("usmeshka","localhost","postgres","11111");
        DataAdapter adapter = new MSSQLDataAdapter("usmeshka","ME2-ПК","sa","11111");
        UsmeshkaBusinessLogics BL = new UsmeshkaBusinessLogics(adapter,7652);

//        if(args.length>0 && args[0].equals("-F"))
//        BL.fillData();
        LocateRegistry.createRegistry(7652).rebind("BusinessLogics", BL);
//        Naming.rebind("rmi://127.0.0.1:1099/TmcBusinessLogics",new TmcBusinessLogics());
        System.out.println("Server has successfully started");
    }

    // конкретные классы
    // реализация по безналу (в опт)
    CustomClass orderSaleWhole, invoiceSaleWhole, commitSaleWhole;
    // реализация в розницу
    CustomClass orderSaleRetail, commitSaleRetail;
    // инвентаризация
    CustomClass balanceCheck;
    // закупка у местного поставщика
    CustomClass orderDeliveryLocal, commitDeliveryLocal;
    // закупка у импортного поставщика
    CustomClass orderDeliveryImport, commitDeliveryImport;
    // внутреннее перемещение
    CustomClass orderDistribute, invoiceDistribute, commitOutDistribute, commitIncDistribute;
    // возвраты
    // возврат местному поставщику
    CustomClass orderReturnDeliveryLocal, invoiceReturnDeliveryLocal, commitReturnDeliveryLocal;
    // возврат реализации по безналу
    CustomClass returnSaleWhole;
    // возврат реализации за наличный расчет
    CustomClass returnSaleRetail;

    CustomClass order, orderInc, orderOut;
    CustomClass invoiceDocument;
    CustomClass commitOut, commitInc;
    CustomClass orderExtInc, commitExtInc;

    CustomClass documentInner, orderOuter, commitOuter;

    CustomClass move, moveInner, returnInner, returnOuter, orderInner;

    CustomClass store, article, articleGroup, localSupplier, importSupplier, orderLocal, format;
    CustomClass customerWhole, customerRetail, orderWhole, orderRetail;

    protected void initClasses() {
        // заявки на приход, расход
        order = addAbstractClass("Заявка", transaction);
        orderInc = addAbstractClass("Заявка прихода на склад", order);
        orderOut = addAbstractClass("Заявка расхода со склада", order);

        invoiceDocument = addAbstractClass("Заявка на перевозку", order);
        commitOut = addAbstractClass("Отгруженная заявка", order);
        commitInc = addAbstractClass("Принятая заявка", commitOut);

        // внутр. и внешние операции
        orderOuter = addAbstractClass("Заявка на внешнюю операцию", orderInc); // всегда прих., создает партию - элементарную единицу учета
        commitOuter = addAbstractClass("Внешняя операция", orderOuter, commitInc);

        documentInner = addAbstractClass("Внутренняя операция", order);
        returnInner = addAbstractClass("Возврат внутренней операции", order);
        orderInner = addAbstractClass("Заказ", documentInner);

        orderExtInc = addAbstractClass("Закупка", orderOuter);
        commitExtInc = addAbstractClass("Приход от пост.", commitOuter, orderExtInc, invoiceDocument);

        orderWhole = addAbstractClass("Операция по безналу", order);
        orderRetail = addAbstractClass("Операция за наличный расчет", order);

        orderSaleWhole = addConcreteClass("Заказ по безналу", orderOut, orderInner, orderWhole);
        invoiceSaleWhole = addConcreteClass("Выписанный заказ по безналу", orderSaleWhole, invoiceDocument);
        commitSaleWhole = addConcreteClass("Отгруженный заказ по безналу", invoiceSaleWhole, commitOut);

        orderSaleRetail = addConcreteClass("Заказ за наличный расчет", orderOut, orderInner, orderRetail);
        commitSaleRetail = addConcreteClass("Реализация за наличный расчет", orderSaleRetail, commitOut);

        balanceCheck = addConcreteClass("Инвентаризация", orderOut, commitOut, documentInner);

        orderDistribute = addConcreteClass("Заказ на внутреннее перемещение", orderOut, orderInc, orderInner);
        invoiceDistribute = addConcreteClass("Выписанное внутреннее перемещение", orderDistribute, invoiceDocument);
        commitOutDistribute = addConcreteClass("Отгруженное внутреннее перемещение", invoiceDistribute, commitOut);
        commitIncDistribute = addConcreteClass("Принятое внутреннее перемещение", commitOutDistribute, commitInc);

        orderLocal = addAbstractClass("Операция с местным поставщиком", order);

        orderDeliveryLocal = addConcreteClass("Закупка у местного поставщика", orderExtInc, orderLocal);
        commitDeliveryLocal = addConcreteClass("Приход от местного поставщика", orderDeliveryLocal, commitExtInc);

        orderDeliveryImport = addConcreteClass("Закупка у импортного поставщика", orderExtInc);
        commitDeliveryImport = addConcreteClass("Приход от импортного поставщика", orderDeliveryImport, commitExtInc);

        orderReturnDeliveryLocal = addConcreteClass("Заявка на возврат местному поставщику", orderOut, orderLocal);
        invoiceReturnDeliveryLocal = addConcreteClass("Выписанная заявка на возврат местному поставщику", orderReturnDeliveryLocal,invoiceDocument);
        commitReturnDeliveryLocal = addConcreteClass("Возврат местному поставщику", invoiceReturnDeliveryLocal, commitOut);

        returnSaleWhole = addConcreteClass("Возврат реализации по безналу", orderInc, returnInner, commitInc, orderWhole, invoiceDocument);
        returnSaleRetail = addConcreteClass("Возврат реализации за наличный расчет", orderInc, returnInner, commitInc, orderRetail);

        store = addConcreteClass("Склад", namedObject);
        article = addConcreteClass("Товар", namedObject);
        articleGroup = addConcreteClass("Группа товаров", namedObject);        
        localSupplier = addConcreteClass("Местный поставщик", namedObject);
        importSupplier = addConcreteClass("Импортный поставщик", namedObject);
        customerWhole = addConcreteClass("Оптовый покупатель", namedObject);
        customerRetail = addConcreteClass("Розничный покупатель", namedObject);

        format = addConcreteClass("Формат", namedObject);

        documentPrice = addAbstractClass("Изменение цены", transaction);
        documentRate = addConcreteClass("Изменение курса", documentPrice);
        documentRRP = addConcreteClass("Изменение RRP", documentPrice);

        documentStorePrice = addConcreteClass("Изменение цены в магазине", transaction);
    }

    CustomClass documentPrice;
    CustomClass documentStorePrice;
    CustomClass documentRate;
    CustomClass documentRRP;

    LP balanceSklFreeQuantity, orderContragent;

    protected void initProperties() {

        LP multiplyDouble2 = addMFProp(DoubleClass.instance, 2);

        LP articleToGroup = addDProp("articleToGroup", "Группа товаров", articleGroup, article); addJProp(baseGroup, "Группа товаров", name, articleToGroup, 1);

        LP incStore = addDProp("incStore", "Склад (прих.)", store, orderInc); addJProp(baseGroup, "Склад (прих.)", name, incStore, 1);
        LP outStore = addDProp("outStore", "Склад (расх.)", store, orderOut); addJProp(baseGroup, "Склад (расх.)", name, outStore, 1); 

        orderContragent = addCUProp("Контрагент", // generics
                addDProp("localSupplier", "Поставщик", localSupplier, orderLocal),
                addDProp("importSupplier", "Поставщик", importSupplier, orderDeliveryImport),
                addDProp("wholeCustomer", "Покупатель", customerWhole, orderWhole),
                addDProp("retailCustomer", "Покупатель", customerRetail, orderRetail));
        addJProp(baseGroup, "Контрагент", name, orderContragent, 1);

        LP invoiceNumber = addDProp(baseGroup, "Накладная", StringClass.get(20), invoiceDocument);

        outerOrderQuantity = addDProp(baseGroup, "extIncOrderQuantity", "Кол-во заяв.", DoubleClass.instance, orderOuter, article);
        outerCommitedQuantity = addDProp(baseGroup, "extIncCommitedQuantity", "Кол-во принятое", DoubleClass.instance, commitOuter, article);
        outerCommitedQuantity.setDerivedChange(outerOrderQuantity, 1, 2, is(commitInc), 1);
        LP expiryDate = addDProp(baseGroup, "expiryDate", "Срок годн.", DateClass.instance, commitOuter, article);        

        // для возвратных своего рода generics
        LP returnOuterQuantity = addDProp("returnDeliveryLocalQuantity", "Кол-во возврата", DoubleClass.instance, orderReturnDeliveryLocal, article, commitDeliveryLocal);

        returnInnerCommitQuantity = addCUProp(baseGroup, "Кол-во возврата", // generics
                         addDProp("returnSaleWholeQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleWhole, article, commitOuter, commitSaleWhole),
                         addDProp("returnSaleRetailQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleRetail, article, commitOuter, commitSaleRetail));

        returnInnerQuantity = addSGProp(baseGroup, "Кол-во возврата", returnInnerCommitQuantity, 1, 2, 4); 
        LP returnDocumentQuantity = addCUProp("Кол-во возврата", returnOuterQuantity, returnInnerQuantity); // возвратный документ\прямой документ
        addConstraint(addJProp("При возврате контрагент документа, по которому идет возврат, должен совпадать с контрагентом возврата", and1, addJProp(diff2, orderContragent, 1, orderContragent, 2), 1, 3, returnDocumentQuantity, 1, 2, 3), false);

        LP orderInnerQuantity = addDProp("outOrderQuantity", "Кол-во операции", DoubleClass.instance, orderInner, article, commitOuter);

        // инвентаризация
        innerBalanceCheck = addDProp(baseGroup, "innerBalanceCheck", "Остаток инв.", DoubleClass.instance, balanceCheck, article, commitOuter);
        innerBalanceCheckDB = addDProp("innerBalanceCheckDB", "Остаток (по учету)", DoubleClass.instance, balanceCheck, article, commitOuter);

        innerQuantity = addCUProp(baseGroup, "innerQuantity", "Кол-во операции", returnOuterQuantity, orderInnerQuantity,
                                addSGProp("Кол-во операции", returnInnerCommitQuantity, 1, 2, 3),
                                addDUProp("balanceCheckQuantity","Кол-во инв.", innerBalanceCheckDB, innerBalanceCheck));

        LP incCommitedQuantity = addCUProp(baseGroup, "Кол-во прихода парт.",
                        addJProp(and1, outerCommitedQuantity, 1, 2, equals2, 1, 3), // избыточно так как не может сама класс определить
                        addJProp(and1, innerQuantity, 1, 2, 3, is(commitInc), 1));
        LP incSklCommitedQuantity = addSGProp(baseGroup, "Кол-во прихода парт. на скл.", incCommitedQuantity, incStore, 1, 2, 3);

        LP outCommitedQuantity = addJProp("Кол-во отгр. парт.", and1, innerQuantity, 1, 2, 3, is(commitOut), 1);
        LP outSklCommitedQuantity = addSGProp(baseGroup, "Кол-во отгр. парт. на скл.", outCommitedQuantity, outStore, 1, 2, 3);
        LP outSklQuantity = addSGProp(baseGroup, "Кол-во заяв. парт. на скл.", innerQuantity, outStore, 1, 2, 3);

        balanceSklCommitedQuantity = addDUProp(baseGroup, "balanceSklCommitedQuantity", "Остаток парт. на скл.", incSklCommitedQuantity, outSklCommitedQuantity);
        balanceSklFreeQuantity = addDUProp(baseGroup, "balanceSklFreeQuantity", "Свободное кол-во на скл.", incSklCommitedQuantity, outSklQuantity);
        addConstraint(addJProp("Кол-во резерва должно быть не меньше нуля", greater2, vzero, balanceSklFreeQuantity, 1, 2, 3), false);

        innerBalanceCheckDB.setDerivedChange(balanceSklCommitedQuantity, outStore, 1, 2, 3);

        addJProp(baseGroup, "Остаток парт. прих.", balanceSklCommitedQuantity, incStore, 1, 2, 3);
        addJProp(baseGroup, "Остаток парт. расх.", balanceSklCommitedQuantity, outStore, 1, 2, 3);
        LP documentOutSklFreeQuantity = addJProp("Свободно парт. расх.", balanceSklFreeQuantity, outStore, 1, 2, 3);

        LP returnedInnerQuantity = addSGProp("Кол-во возвр. парт.", returnInnerCommitQuantity, 4, 2, 3);
        LP confirmedInnerQuantity = addDUProp("Кол-во подтв. парт.", addJProp(and1, orderInnerQuantity, 1, 2, 3, is(commitOut), 1) , returnedInnerQuantity);
        addConstraint(addJProp("Кол-во возврата должно быть не меньше кол-ва самой операции", greater2, vzero, confirmedInnerQuantity, 1, 2, 3), false);

        LP sameContragent = addJProp(equals2, orderContragent, 1, orderContragent, 2);
        
        // для док. \ товара \ парт. \ док. прод.   - кол-во подтв. парт. если совпадают контрагенты
        returnInnerFreeQuantity = addJProp(baseGroup, "Макс. кол-во по возврату парт.", and1, addJProp(and1, confirmedInnerQuantity, 4, 2, 3, sameContragent, 1, 4), 1, 2, 3, 4, is(returnInner), 1) ;
        returnFreeQuantity = addSGProp(baseGroup, "Макс. кол-во по возврату", returnInnerFreeQuantity, 1, 2, 4);

        // создаем свойства ограничения для расчета себестоимости (являются следствием addConstraint)
        documentInnerFreeQuantity = addCUProp(baseGroup, "Макс. кол-во по парт.",
                            addJProp(and1, documentOutSklFreeQuantity, 1, 2, 3, sameContragent, 1, 3), // возврата поставщику - ограничение что кол-во своб. (всегда меньше кол-во подтв.) + условие что партии этого поставщика
                            addJProp(and1, documentOutSklFreeQuantity, 1, 2, 3, is(orderInner), 1), // прямого расхода - кол-во свободного для этого склада
                            innerBalanceCheckDB, // для инвентаризации - не больше зафиксированного количества по учету
                            addSGProp(returnInnerFreeQuantity, 1, 2, 3)); // возврата расхода  - кол-во подтв. этого контрагента

        // добавляем свойства по товарам
        articleInnerQuantity = addDGProp(baseGroup, "articleInnerQuantity", "Кол-во операции", 2, false, innerQuantity, 1, 2, documentInnerFreeQuantity, 1, 2, 3, date, 3, 3);
        documentFreeQuantity = addSGProp(baseGroup, "Макс. кол-во по товару", documentInnerFreeQuantity, 1, 2);

        articleBalanceCheck = addDGProp(baseGroup, "articleBalanceCheck", "Остаток инв.", 2, false, innerBalanceCheck, 1, 2, innerBalanceCheckDB, 1, 2, 3, date, 3, 3);

        LP articleBalanceSklCommitedQuantity = addSGProp(baseGroup, "articleBalanceSklCommitedQuantity", "Остаток тов. на скл.", balanceSklCommitedQuantity, 1, 2);
        addJProp(baseGroup, "Остаток тов. прих.", articleBalanceSklCommitedQuantity, incStore, 1, 2);
        addJProp(baseGroup, "Остаток тов. расх.", articleBalanceSklCommitedQuantity, outStore, 1, 2);

        // цены
        LP storeFormat = addDProp(baseGroup, "storeFormat", "Формат", format, store);

        LP documentFormat = addDProp(baseGroup, "documentFormat", "Формат", format, documentPrice);
        LP rate = addDProp(baseGroup, "rate", "Курс", DoubleClass.instance, DateClass.instance);

        LP priceRRP = addDProp(baseGroup, "priceRRP", "RRP", DoubleClass.instance, documentRRP, article);
        LP[] maxRRPProps = addMGProp(null, new String[]{"currentRRPDate","currentRRPDoc"}, new String[]{"Дата посл. RRP","Посл. док. RRP"}, 1,
                addJProp(and1, date, 1, priceRRP, 1, 2), 1, documentFormat, 1, 2);
        currentRRPDate = maxRRPProps[0]; currentRRPDoc = maxRRPProps[1]; baseGroup.add(currentRRPDoc.property);
        currentRRP = addJProp(baseGroup, "currentVatOut", "RRP (тек.)", priceRRP, currentRRPDoc, 1, 2, 2);

        LP isRateChanged = addDProp(baseGroup, "isRateChanged", "Изм. курс", LogicalClass.instance, documentRate, article);
        LP currentRateDate = addMGProp(null, "currentRateDate", "Дата изм. курса", addJProp(and1, date, 1, isRateChanged, 1, 2), documentFormat, 1, 2);

        LP currentPriceDate = addSUProp(baseGroup, "currentPriceDate", "Дата курса", Union.MAX, currentRRPDate, currentRateDate);
        LP currentPriceRate = addJProp(baseGroup, "Курс", rate, currentPriceDate, 1, 2);

        LP currentPrice = addJProp(baseGroup, "Необх. цена", multiplyDouble2, currentRRP, 1, 2, currentPriceRate, 1, 2);

        LP documentStore = addDProp(baseGroup, "documentStore", "Склад", store, documentStorePrice);
        LP storePrice = addDProp(baseGroup, "storePrice", "Цена на складе", DoubleClass.instance, documentStorePrice, article);
        LP[] maxStorePriceProps = addMGProp(baseGroup, new String[]{"currentStorePriceDate","currentStorePriceDoc"}, new String[]{"Дата посл. цены в маг.","Посл. док. цены в маг."}, 1,
                addJProp(and1, date, 1, storePrice, 1, 2), 1, documentStore, 1, 2);
        currentStorePriceDate = maxStorePriceProps[0]; currentStorePriceDoc = maxStorePriceProps[1];
        currentStorePrice = addJProp(baseGroup, "currentStorePrice", "Цена на складе (тек.)", storePrice, currentStorePriceDoc, 1, 2, 2);

        LP currentFormatPrice = addJProp(baseGroup, "Необх. цена", currentPrice, storeFormat, 1, 2);
        LP outOfDatePrice = addJProp(andNot1, articleBalanceSklCommitedQuantity, 1, 2,
                addJProp(equals2, currentFormatPrice, 1, 2, currentStorePrice, 1, 2), 1, 2);
        storePrice.setDerivedChange(addJProp(and1, currentFormatPrice, 1, 2, outOfDatePrice, 1, 2), documentStore, 1, 2);
        LP prevPrice = addDProp(baseGroup, "prevPrice", "Цена пред.", DoubleClass.instance, documentStorePrice, article);
        prevPrice.setDerivedChange(addJProp(and1, currentStorePrice, 1, 2, outOfDatePrice, 1, 2), documentStore, 1, 2);
        LP revalBalance = addDProp(baseGroup, "revalBalance", "Остаток переоц.", DoubleClass.instance, documentStorePrice, article);
        revalBalance.setDerivedChange(addJProp(and1, articleBalanceSklCommitedQuantity, 1, 2, outOfDatePrice, 1, 2), documentStore, 1, 2);
    }

    LP currentStorePriceDate, currentStorePriceDoc, currentStorePrice;
    LP currentRRPDate, currentRRPDoc, currentRRP;

    LP documentFreeQuantity, documentInnerFreeQuantity, returnInnerFreeQuantity, returnFreeQuantity, innerQuantity, returnInnerCommitQuantity, returnInnerQuantity;
    LP outerOrderQuantity, outerCommitedQuantity, articleBalanceCheck, articleBalanceCheckDB, innerBalanceCheck, innerBalanceCheckDB, balanceSklCommitedQuantity;
    LP articleInnerQuantity;

    protected void initGroups() {
    }

    protected void initConstraints() {
    }

    protected void initPersistents() {
        persistents.add((AggregateProperty) balanceSklCommitedQuantity.property);
        persistents.add((AggregateProperty) balanceSklFreeQuantity.property);

        persistents.add((AggregateProperty) innerQuantity.property);
    }

    protected void initTables() {
        tableFactory.include("article",article);
        tableFactory.include("orders", order);
        tableFactory.include("store",store);
        tableFactory.include("localsupplier",localSupplier);
        tableFactory.include("importsupplier",importSupplier);
        tableFactory.include("customerwhole",customerWhole);
        tableFactory.include("customerretail",customerRetail);
        tableFactory.include("articlestore",article,store);
        tableFactory.include("articleorder",article,order);
        tableFactory.include("rates",DateClass.instance);
    }

    protected void initIndexes() {
    }

    protected void initNavigators() throws JRException, FileNotFoundException {

        createDefaultClassForms(baseClass, baseElement);

        NavigatorElement documents = new NavigatorElement(baseElement, 1000, "Документы");
            new LocalNavigatorForm(documents, 1100);
            new DeliveryImportNavigatorForm(documents, 1150);
            NavigatorElement innerSplit = new NavigatorElement(documents, 1200, "Внутренние документы по товарам");
                new WholeNavigatorForm(innerSplit, 1250, true);
                new RetailNavigatorForm(innerSplit, 1300, true);
                new DistributeNavigatorForm(innerSplit, 1320, true);
                new BalanceCheckNavigatorForm(innerSplit, 1350, true);
                new ReturnDeliveryLocalNavigatorForm(innerSplit, 1400, true);
                NavigatorElement innerSplitReturnSplit = new NavigatorElement(innerSplit, 1425, "Возвраты по товарам");
                    new ReturnSaleWholeArticleNavigatorForm(innerSplitReturnSplit, 1450, true);
                    new ReturnSaleRetailArticleNavigatorForm(innerSplitReturnSplit, 1475, true);
                NavigatorElement innerSplitReturn = new NavigatorElement(innerSplit, 1500, "Возвраты по документам");
                    new ReturnSaleWholeArticleNavigatorForm(innerSplitReturn, 1525, false);
                    new ReturnSaleRetailArticleNavigatorForm(innerSplitReturn, 1550, false);
            NavigatorElement inner = new NavigatorElement(documents, 1600, "Внутренние документы по партиям");
                new WholeNavigatorForm(inner, 1650, false);
                new RetailNavigatorForm(inner, 1700, false);
                new DistributeNavigatorForm(inner, 1720, false);
                new BalanceCheckNavigatorForm(inner, 1750, false);
                new ReturnDeliveryLocalNavigatorForm(inner, 1800, false);
                NavigatorElement innerReturnSplit = new NavigatorElement(inner, 1825, "Возвраты по партиям");
                    new ReturnSaleWholeNavigatorForm(innerReturnSplit, 1850, true);
                    new ReturnSaleRetailNavigatorForm(innerReturnSplit, 1875, true);
                NavigatorElement innerReturn = new NavigatorElement(inner, 1900, "Возвраты по документам");
                    new ReturnSaleWholeNavigatorForm(innerReturn, 1925, false);
                    new ReturnSaleRetailNavigatorForm(innerReturn, 1950, false);
        NavigatorElement store = new NavigatorElement(baseElement, 2000, "Сводная информация");
            new StoreArticleNavigatorForm(store, 2100);
            new FormatArticleNavigatorForm(store, 2200);
            new RatesNavigatorForm(store, 2300);
            new DocumentRRPNavigatorForm(store, 2400);
            new DocumentRateNavigatorForm(store, 2500);
            new DocumentStorePriceNavigatorForm(store, 2600);
    }

    private class DocumentNavigatorForm extends NavigatorForm {
        final ObjectNavigator objDoc;

        protected DocumentNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass) {
            super(parent, ID, documentClass.caption);

            objDoc = addSingleGroupObjectImplement(documentClass, "Документ", properties, baseGroup, true);
        }
    }

    private class ArticleNavigatorForm extends DocumentNavigatorForm {
        final ObjectNavigator objArt;

        protected ArticleNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean splitArticle) {
            super(parent, ID, documentClass);

            objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            if(splitArticle)
                addPropertyView(objDoc, objArt, properties, baseGroup, true);
        }
    }

    private class InnerNavigatorForm extends ArticleNavigatorForm {

        protected InnerNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean splitArticle) {
            super(parent, ID, documentClass, splitArticle);

            if(splitArticle) {
                RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      new NotNullFilterNavigator(getPropertyView(articleInnerQuantity.property).view),
                                      "Документ",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      new NotNullFilterNavigator(getPropertyView(documentFreeQuantity.property).view),
                                      "Макс. кол-во",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
                addRegularFilterGroup(filterGroup);
            }
        }
    }

    private class OuterNavigatorForm extends ArticleNavigatorForm {

        protected OuterNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass) {
            super(parent, ID, documentClass, true);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(outerOrderQuantity.property).view),
                                  "Заявлено",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(outerCommitedQuantity.property).view),
                                  "Выполнено",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class LocalNavigatorForm extends OuterNavigatorForm {
        public LocalNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, orderLocal);
        }
    }

    private class DeliveryImportNavigatorForm extends OuterNavigatorForm {
        public DeliveryImportNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, orderDeliveryImport);
        }
    }

    private class ArticleOuterNavigatorForm extends InnerNavigatorForm {
        final ObjectNavigator objOuter;

        protected ArticleOuterNavigatorForm (NavigatorElement parent, int ID, CustomClass documentClass, CustomClass commitClass, boolean splitArticle, boolean splitOuter) {
            super(parent, ID, documentClass, splitArticle);

            if(splitArticle) {
                objOuter = addSingleGroupObjectImplement(commitClass, "Партия", properties, baseGroup, true);
            } else {
                objOuter = new ObjectNavigator(IDShift(1), commitClass, "Партия");
                objArt.groupTo.add(objOuter);
                addPropertyView(objOuter, properties, baseGroup, true);
            }

//            addPropertyView(objOuter, objArt, properties, baseGroup, true);
            if(splitOuter) {
                addPropertyView(objOuter, objDoc, properties, baseGroup, true);
                addPropertyView(objOuter, objDoc, objArt, properties, baseGroup, true);
                
                RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      new NotNullFilterNavigator(getPropertyView(innerQuantity.property).view),
                                      "Документ",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      new NotNullFilterNavigator(getPropertyView(documentInnerFreeQuantity.property).view),
                                      "Макс. кол-во",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
                addRegularFilterGroup(filterGroup);
            }
        }
    }

    private class ReturnDeliveryLocalNavigatorForm extends ArticleOuterNavigatorForm {
        public ReturnDeliveryLocalNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, orderReturnDeliveryLocal, commitDeliveryLocal, split, true);
        }
    }
    
    private class ArticleInnerNavigatorForm extends ArticleOuterNavigatorForm {

        protected ArticleInnerNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean splitArticle, boolean splitOuter) {
            super(parent, ID, documentClass, commitOuter, splitArticle, splitOuter);
        }
    }

    private class DocumentInnerNavigatorForm extends ArticleInnerNavigatorForm {

        protected DocumentInnerNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean split) {
            super(parent, ID, documentClass, split, true);
        }
    }

    private class WholeNavigatorForm extends DocumentInnerNavigatorForm {
        public WholeNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, orderWhole, split);
        }
    }

    private class RetailNavigatorForm extends DocumentInnerNavigatorForm {
        public RetailNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, orderRetail, split);
        }
    }

    private class DistributeNavigatorForm extends DocumentInnerNavigatorForm {
        public DistributeNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, orderDistribute, split);
        }
    }

    private class BalanceCheckNavigatorForm extends DocumentInnerNavigatorForm {
        public BalanceCheckNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, balanceCheck, split);
        }
    }

    private class ReturnInnerNavigatorForm extends ArticleInnerNavigatorForm {
        final ObjectNavigator objInner;

        protected ReturnInnerNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, CustomClass commitClass, boolean splitOuter) {
            super(parent, ID, documentClass, false, splitOuter);

            if(splitOuter)
                objInner = addSingleGroupObjectImplement(commitClass, "Документ к возврату", properties, baseGroup, true);
            else {
                objInner = new ObjectNavigator(IDShift(1), commitClass, "Документ к возврату");
                objOuter.groupTo.add(objInner);
                addPropertyView(objInner, properties, baseGroup, true);
            }

//            addPropertyView(objInner, objArt, properties, baseGroup, true);
//            addPropertyView(objInner, objDoc, properties, baseGroup, true);
//            addPropertyView(objInner, objDoc, objArt, properties, baseGroup, true);
//            addPropertyView(objInner, objOuter, objArt, properties, baseGroup, true);
            addPropertyView(objInner, objOuter, objDoc, properties, baseGroup, true);
            addPropertyView(objInner, objOuter, objDoc, objArt, properties, baseGroup, true);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(returnInnerCommitQuantity.property).view),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(returnInnerFreeQuantity.property).view),
                                  "Макс. кол-во",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
            addRegularFilterGroup(filterGroup);
        }
    }

    private class ReturnSaleWholeNavigatorForm extends ReturnInnerNavigatorForm {
        private ReturnSaleWholeNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, returnSaleWhole, commitSaleWhole, split);
        }
    }

    private class ReturnSaleRetailNavigatorForm extends ReturnInnerNavigatorForm {
        private ReturnSaleRetailNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, returnSaleRetail, commitSaleRetail, split);
        }
    }

    private class ReturnArticleNavigatorForm extends InnerNavigatorForm {
        final ObjectNavigator objInner;
        final ObjectNavigator objOuter;

        protected ReturnArticleNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, CustomClass commitClass, boolean splitArticle) {
            super(parent, ID, documentClass, splitArticle);

            if(splitArticle)
                objInner = addSingleGroupObjectImplement(commitClass, "Документ к возврату", properties, baseGroup, true);
            else {
                objInner = new ObjectNavigator(IDShift(1), commitClass, "Документ к возврату");
                objArt.groupTo.add(objInner);
                addPropertyView(objInner, properties, baseGroup, true);
            }

//            addPropertyView(objInner, objArt, properties, baseGroup, true);
            addPropertyView(objInner, objDoc, properties, baseGroup, true);
            addPropertyView(objInner, objDoc, objArt, properties, baseGroup, true);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(returnInnerQuantity.property).view),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(returnFreeQuantity.property).view),
                                  "Макс. кол-во",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroup);

            objOuter = addSingleGroupObjectImplement(commitOuter, "Партия", properties, baseGroup, true);
            
//            addPropertyView(objOuter, objArt, properties, baseGroup, true);
//            addPropertyView(objOuter, objDoc, properties, baseGroup, true);
//            addPropertyView(objOuter, objDoc, objArt, properties, baseGroup, true);
//            addPropertyView(objInner, objOuter, objArt, properties, baseGroup, true);
            addPropertyView(objInner, objOuter, objDoc, properties, baseGroup, true);
            addPropertyView(objInner, objOuter, objDoc, objArt, properties, baseGroup, true);

            // тоже самое что в ReturnInnerNavigatorForm
            RegularFilterGroupNavigator filterOutGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterOutGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(returnInnerCommitQuantity.property).view),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
            filterOutGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(returnInnerFreeQuantity.property).view),
                                  "Макс. кол-во",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
            addRegularFilterGroup(filterOutGroup);
        }
    }

    private class ReturnSaleWholeArticleNavigatorForm extends ReturnArticleNavigatorForm {
        private ReturnSaleWholeArticleNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, returnSaleWhole, commitSaleWhole, split);
        }
    }

    private class ReturnSaleRetailArticleNavigatorForm extends ReturnArticleNavigatorForm {
        private ReturnSaleRetailArticleNavigatorForm(NavigatorElement parent, int ID, boolean split) {
            super(parent, ID, returnSaleRetail, commitSaleRetail, split);
        }
    }

    private class StoreArticleNavigatorForm extends NavigatorForm {
        protected StoreArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по складу");

            ObjectNavigator objDoc = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup, true);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);
            ObjectNavigator objOuter = addSingleGroupObjectImplement(commitOuter, "Партия", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup, true);
            addPropertyView(objDoc, objOuter, properties, baseGroup, true);
            addPropertyView(objOuter, objArt, properties, baseGroup, true);
            addPropertyView(objDoc, objOuter, objArt, properties, baseGroup, true);
        }
    }

    private class FormatArticleNavigatorForm extends NavigatorForm {
        protected FormatArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по форматам");

            ObjectNavigator objDoc = addSingleGroupObjectImplement(format, "Формат", properties, baseGroup, true);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup, true);
        }
    }

    private class RatesNavigatorForm extends NavigatorForm {
        protected RatesNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Курсы");

            ObjectNavigator objDoc = addSingleGroupObjectImplement(DateClass.instance, "Дата", properties, baseGroup, true);
            objDoc.groupTo.gridClassView = false;
            objDoc.groupTo.singleViewType = true;
        }
    }

    private class DocumentRRPNavigatorForm extends ArticleNavigatorForm {

        protected DocumentRRPNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, documentRRP, true);
        }
    }

    private class DocumentRateNavigatorForm extends ArticleNavigatorForm {

        protected DocumentRateNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, documentRate, true);
        }
    }

    private class DocumentStorePriceNavigatorForm extends ArticleNavigatorForm {

        protected DocumentStorePriceNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, documentStorePrice, true);
        }
    }

    protected void initAuthentication() {
        User user1 = authPolicy.addUser("user1", "user1", new UserInfo("Петр", "Петров"));
    }
}
