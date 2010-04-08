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
import platform.server.logics.property.group.AbstractGroup;
import platform.server.classes.*;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.view.navigator.filter.OrFilterNavigator;
import platform.server.view.navigator.filter.NotFilterNavigator;
import platform.server.view.navigator.filter.CompareFilterNavigator;
import platform.server.auth.User;
import platform.interop.UserInfo;
import platform.interop.Compare;

import javax.swing.*;


public class UsmeshkaBusinessLogics extends BusinessLogics<UsmeshkaBusinessLogics> {

    public UsmeshkaBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    //    static Registry registry;
    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, FileNotFoundException, JRException, MalformedURLException {

        System.out.println("Server is starting...");
//        DataAdapter adapter = new PostgreDataAdapter("usmeshka","localhost","postgres","11111");
        DataAdapter adapter = new MSSQLDataAdapter("usmeshka2","ME2-ПК","sa","11111");
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

    CustomClass documentRevalue, commitOrderInc;

    protected void initClasses() {

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

        documentStorePrice = addAbstractClass("Изменение цены в магазине", transaction);
        documentRevalue = addConcreteClass("Переоценка в магазине", documentStorePrice);
        
        // заявки на приход, расход
        order = addAbstractClass("Заявка", transaction);
        orderInc = addAbstractClass("Заявка прихода на склад", order);
        orderOut = addAbstractClass("Заявка расхода со склада", order);

        invoiceDocument = addAbstractClass("Заявка на перевозку", order);
        commitOut = addAbstractClass("Отгруженная заявка", order);
        commitInc = addAbstractClass("Принятая заявка", commitOut);

        commitOrderInc = addAbstractClass("Принятый приход", documentStorePrice, orderInc, commitInc);

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
        commitIncDistribute = addConcreteClass("Принятое внутреннее перемещение", commitOutDistribute, commitOrderInc);

        orderLocal = addAbstractClass("Операция с местным поставщиком", order);

        orderDeliveryLocal = addConcreteClass("Закупка у местного поставщика", orderExtInc, orderLocal);
        commitDeliveryLocal = addConcreteClass("Приход от местного поставщика", orderDeliveryLocal, commitExtInc, commitOrderInc);

        orderDeliveryImport = addConcreteClass("Закупка у импортного поставщика", orderExtInc);
        commitDeliveryImport = addConcreteClass("Приход от импортного поставщика", orderDeliveryImport, commitExtInc, commitOrderInc);

        orderReturnDeliveryLocal = addConcreteClass("Заявка на возврат местному поставщику", orderOut, orderLocal);
        invoiceReturnDeliveryLocal = addConcreteClass("Выписанная заявка на возврат местному поставщику", orderReturnDeliveryLocal,invoiceDocument);
        commitReturnDeliveryLocal = addConcreteClass("Возврат местному поставщику", invoiceReturnDeliveryLocal, commitOut);

        returnSaleWhole = addConcreteClass("Возврат реализации по безналу", orderInc, returnInner, commitInc, orderWhole, invoiceDocument);
        returnSaleRetail = addConcreteClass("Возврат реализации за наличный расчет", orderInc, returnInner, commitInc, orderRetail);
    }

    CustomClass documentPrice;
    CustomClass documentStorePrice;
    CustomClass documentRate;
    CustomClass documentRRP;

    LP balanceSklFreeQuantity, orderContragent, rate;

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

        articleQuantity = addCUProp("Кол-во операции", outerCommitedQuantity, articleInnerQuantity);

        articleBalanceCheck = addDGProp(baseGroup, "articleBalanceCheck", "Остаток инв.", 2, false, innerBalanceCheck, 1, 2, innerBalanceCheckDB, 1, 2, 3, date, 3, 3);

        LP articleBalanceSklCommitedQuantity = addSGProp(baseGroup, "articleBalanceSklCommitedQuantity", "Остаток тов. на скл.", balanceSklCommitedQuantity, 1, 2);
        addJProp(baseGroup, "Остаток тов. прих.", articleBalanceSklCommitedQuantity, incStore, 1, 2);
        addJProp(baseGroup, "Остаток тов. расх.", articleBalanceSklCommitedQuantity, outStore, 1, 2);

        // цены
        LP storeFormat = addDProp(baseGroup, "storeFormat", "Формат", format, store);

        LP documentFormat = addDProp(baseGroup, "documentFormat", "Формат", format, documentPrice);
        rate = addDProp(baseGroup, "rate", "Курс", DoubleClass.instance, DateClass.instance);

        priceRRP = addDProp(baseGroup, "priceRRP", "RRP", DoubleClass.instance, documentRRP, article);
        LP[] maxRRPProps = addMGProp(null, true, new String[]{"currentRRPDate","currentRRPDoc"}, new String[]{"Дата посл. RRP","Посл. док. RRP"}, 1,
                addJProp(and1, date, 1, priceRRP, 1, 2), 1, documentFormat, 1, 2);
        currentRRPDate = maxRRPProps[0]; currentRRPDoc = maxRRPProps[1]; baseGroup.add(currentRRPDoc.property);
        currentRRP = addJProp(baseGroup, "currentVatOut", "RRP (тек.)", priceRRP, currentRRPDoc, 1, 2, 2);

        isRateChanged = addDProp(baseGroup, "isRateChanged", "Изм. курс", LogicalClass.instance, documentRate, article);
        LP currentRateDate = addMGProp(null, "currentRateDate", "Дата изм. курса", addJProp(and1, date, 1, isRateChanged, 1, 2), documentFormat, 1, 2);

        LP currentPriceDate = addSUProp(baseGroup, "currentPriceDate", "Дата курса", Union.MAX, currentRRPDate, currentRateDate);
        LP currentPriceRate = addJProp(baseGroup, "Курс", rate, currentPriceDate, 1, 2);

        LP currentPrice = addJProp(baseGroup, "Необх. цена", multiplyDouble2, currentRRP, 1, 2, currentPriceRate, 1, 2);
        LP currentFormatPrice = addJProp(baseGroup, "Необх. цена", currentPrice, storeFormat, 1, 2);

        balanceFormatFreeQuantity = addSGProp(baseGroup, "Своб. кол-во по форм.", balanceSklFreeQuantity, storeFormat, 1, 2);

        documentCurrentRRP = addJProp(baseGroup, "Тек. RRP", currentRRP, documentFormat, 1, 2);

        documentFormatFreeQuantity = addJProp(baseGroup, "Тек. остаток", balanceFormatFreeQuantity, documentFormat, 1, 2);
        addJProp(baseGroup, "Тек. рубл. цена", currentPrice, documentFormat, 1, 2);
        addJProp(baseGroup, "Тек. курс", currentPriceRate, documentFormat, 1, 2);

        LP revalueStore = addDProp(baseGroup, "documentStore", "Склад", store, documentRevalue);
        priceStore = addCUProp("Склад (цены)", incStore, revalueStore);

        documentRevalued = addDProp(baseGroup, "isRevalued", "Переоц.", LogicalClass.instance, documentRevalue, article);
        inDocumentPrice = addCUProp("Изм. цены", documentRevalued, addJProp(and1, is(commitOrderInc), 1, articleQuantity, 1, 2));

        LP[] maxStorePriceProps = addMGProp(baseGroup, true, new String[]{"currentStorePriceDate","currentStorePriceDoc"}, new String[]{"Дата посл. цены в маг.","Посл. док. цены в маг."}, 1,
                addJProp(and1, date, 1, inDocumentPrice, 1, 2), 1, priceStore, 1, 2);
        currentStorePriceDate = maxStorePriceProps[0]; currentStorePriceDoc = maxStorePriceProps[1];

        storePrice = addDProp(baseGroup, "storePrice", "Цена (док.)", DoubleClass.instance, documentStorePrice, article);
        storePrice.setDerivedChange(currentFormatPrice, priceStore, 1, 2, inDocumentPrice, 1, 2);

        currentStorePrice = addJProp(baseGroup, "currentStorePrice", "Цена на складе (тек.)", storePrice, currentStorePriceDoc, 1, 2, 2);

        LP outOfDatePrice = addJProp(and1, articleBalanceSklCommitedQuantity, 1, 2, addJProp(diff2, currentFormatPrice, 1, 2, currentStorePrice, 1, 2), 1, 2);
        documentRevalued.setDerivedChange(outOfDatePrice, priceStore, 1, 2);

        prevPrice = addDProp(baseGroup, "prevPrice", "Цена пред.", DoubleClass.instance, documentStorePrice, article);
        prevPrice.setDerivedChange(currentStorePrice, priceStore, 1, 2, inDocumentPrice, 1, 2);
        revalBalance = addDProp(baseGroup, "revalBalance", "Остаток переоц.", DoubleClass.instance, documentStorePrice, article);
        revalBalance.setDerivedChange(articleBalanceSklCommitedQuantity, priceStore, 1, 2, inDocumentPrice, 1, 2);

        isRevalued = addJProp(diff2, storePrice, 1, 2, prevPrice, 1, 2); // для акта переоценки
        isNewPrice = addJProp(andNot1, inDocumentPrice, 1, 2, addJProp(equals2, storePrice, 1, 2, prevPrice, 1, 2), 1, 2); // для ценников
    }

    LP articleQuantity, storePrice, prevPrice, revalBalance;
    LP priceStore, inDocumentPrice;
    LP isRevalued, isNewPrice, documentRevalued;
    LP documentCurrentRRP, priceRRP, balanceFormatFreeQuantity, documentFormatFreeQuantity, isRateChanged;
    LP currentStorePriceDate, currentStorePriceDoc, currentStorePrice;
    LP currentRRPDate, currentRRPDoc, currentRRP;

    LP documentFreeQuantity, documentInnerFreeQuantity, returnInnerFreeQuantity, returnFreeQuantity, innerQuantity, returnInnerCommitQuantity, returnInnerQuantity;
    LP outerOrderQuantity, outerCommitedQuantity, articleBalanceCheck, articleBalanceCheckDB, innerBalanceCheck, innerBalanceCheckDB, balanceSklCommitedQuantity;
    LP articleInnerQuantity;

    AbstractGroup documentGroup;
    protected void initGroups() {
        documentGroup = new AbstractGroup("Параметры документа");
    }

    protected void initConstraints() {
    }

    protected void initPersistents() {
        persistents.add((AggregateProperty) balanceSklCommitedQuantity.property);
        persistents.add((AggregateProperty) balanceSklFreeQuantity.property);

        persistents.add((AggregateProperty) priceStore.property);
        persistents.add((AggregateProperty) inDocumentPrice.property);

        persistents.add((AggregateProperty) currentRRPDate.property);
        persistents.add((AggregateProperty) currentRRPDoc.property);

        persistents.add((AggregateProperty) currentStorePriceDate.property);
        persistents.add((AggregateProperty) currentStorePriceDoc.property);

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

        NavigatorElement delivery = new NavigatorElement(baseElement, 1000, "Управление закупками");
            NavigatorForm deliveryLocal = new DeliveryLocalNavigatorForm(delivery, true, 1100);
                NavigatorForm deliveryLocalBrowse = new DeliveryLocalNavigatorForm(deliveryLocal, false, 1125);
            NavigatorForm deliveryImport = new DeliveryImportNavigatorForm(delivery, true, 1150);
                NavigatorForm deliveryImportBrowse = new DeliveryImportNavigatorForm(deliveryImport, false, 1175);
            NavigatorForm returnDelivery = new ReturnDeliveryLocalNavigatorForm(delivery, 1400, true, true);
                new ReturnDeliveryLocalNavigatorForm(returnDelivery, 1410, false, true);

        NavigatorElement sale = new NavigatorElement(baseElement, 1200, "Управление продажами");
            NavigatorForm saleWhole = new SaleWholeNavigatorForm(sale, 1250, true, true);
                new SaleWholeNavigatorForm(saleWhole, 1275, false, true);
            NavigatorForm saleRetail = new SaleRetailNavigatorForm(sale, 1300, true, true);
                new SaleRetailNavigatorForm(saleRetail, 1310, false, true);
            NavigatorForm returnSaleWholeArticle = new ReturnSaleWholeArticleNavigatorForm(sale, true, 1450, true);
                new ReturnSaleWholeArticleNavigatorForm(returnSaleWholeArticle, false, 1460, true);
            NavigatorForm returnSaleRetailArticle = new ReturnSaleRetailArticleNavigatorForm(sale, true, 1475, true);
                new ReturnSaleRetailArticleNavigatorForm(returnSaleRetailArticle, false, 1485, true);

        NavigatorElement distribute = new NavigatorElement(baseElement, 3000, "Управление распределением");
            NavigatorForm distributeForm = new DistributeNavigatorForm(distribute, 3100, true, true);
                NavigatorForm distributeBrowseForm = new DistributeNavigatorForm(distributeForm, 3200, false, true);

        NavigatorElement price = new NavigatorElement(baseElement, 2400, "Управление ценообразованием");
            NavigatorForm documentRRP = new DocumentRRPNavigatorForm(price, true, 2450);
                new DocumentRRPNavigatorForm(documentRRP, false, 2500);
            NavigatorForm documentRate = new DocumentRateNavigatorForm(price, true, 2550);
                new DocumentRateNavigatorForm(documentRate, false, 2600);
            NavigatorForm documentRevalue = new DocumentRevalueNavigatorForm(price, true, 2650);
                new DocumentRevalueNavigatorForm(documentRevalue, false, 2750);
            NavigatorForm rates = new RatesNavigatorForm(price, true, 2800);
                new RatesNavigatorForm(rates, false, 2850);

        NavigatorElement balance = new NavigatorElement(baseElement, 1500, "Управление хранением");
            NavigatorForm balanceCheck = new BalanceCheckNavigatorForm(balance, 1350, true, true);
                new BalanceCheckNavigatorForm(balanceCheck, 1375, false, true);

        NavigatorElement store = new NavigatorElement(baseElement, 2000, "Сводная информация");
            new StoreArticleNavigatorForm(store, 2100);
            new FormatArticleNavigatorForm(store, 2200);

        NavigatorElement print = new NavigatorElement(baseElement, 4000, "Печатные формы");
            NavigatorForm incomePrice = new IncomePriceNavigatorForm(print, 4100);
            NavigatorForm revalueAct = new RevalueActNavigatorForm(print, 4200);
            NavigatorForm pricers = new PricersNavigatorForm(print, 4300);

        commitDeliveryLocal.addRelevant(incomePrice);
        documentStorePrice.addRelevant(revalueAct);
        documentStorePrice.addRelevant(pricers);
    }

    private class DocumentNavigatorForm extends NavigatorForm {
        final ObjectNavigator objDoc;

        protected DocumentNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, (toAdd?documentClass.caption:"Документы"));

            objDoc = addSingleGroupObjectImplement(documentClass, "Документ", properties, baseGroup, true);
            if(toAdd) {
                objDoc.groupTo.gridClassView = false;
                objDoc.groupTo.fixedClassView = true;
                objDoc.show = false;
                objDoc.addOnTransaction = true;
            }
        }
    }

    private class ArticleNavigatorForm extends DocumentNavigatorForm {
        final ObjectNavigator objArt;

        protected ArticleNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, boolean splitArticle) {
            super(parent, ID, documentClass, toAdd);

            objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            if(splitArticle)
                addPropertyView(objDoc, objArt, properties, baseGroup, true);
        }
    }

    private class InnerNavigatorForm extends ArticleNavigatorForm {

        protected InnerNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, boolean splitArticle) {
            super(parent, ID, documentClass, toAdd, splitArticle);

            if(splitArticle) {
                RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      new NotNullFilterNavigator(getPropertyView(articleInnerQuantity.property).view),
                                      "Документ",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), !toAdd);
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      new NotNullFilterNavigator(getPropertyView(documentFreeQuantity.property).view),
                                      "Макс. кол-во",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), toAdd);
                addRegularFilterGroup(filterGroup);
            }
        }
    }

    private class OuterNavigatorForm extends ArticleNavigatorForm {

        protected OuterNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass) {
            super(parent, ID, documentClass, toAdd, true);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new OrFilterNavigator(new NotNullFilterNavigator(getPropertyView(outerOrderQuantity.property).view),
                                    new NotNullFilterNavigator(getPropertyView(outerCommitedQuantity.property).view)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)),!toAdd);
            addRegularFilterGroup(filterGroup);
        }
    }

    private class DeliveryLocalNavigatorForm extends OuterNavigatorForm {
        public DeliveryLocalNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, orderDeliveryLocal);
        }
    }

    private class DeliveryImportNavigatorForm extends OuterNavigatorForm {
        public DeliveryImportNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, orderDeliveryImport);
        }
    }

    private class ArticleOuterNavigatorForm extends InnerNavigatorForm {
        final ObjectNavigator objOuter;

        protected ArticleOuterNavigatorForm (NavigatorElement parent,  int ID, CustomClass documentClass, boolean toAdd, CustomClass commitClass, boolean splitArticle, boolean splitOuter) {
            super(parent, ID, documentClass, toAdd, splitArticle);

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
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)),!toAdd);
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      new NotNullFilterNavigator(getPropertyView(documentInnerFreeQuantity.property).view),
                                      "Макс. кол-во",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)),toAdd);
                addRegularFilterGroup(filterGroup);
            }
        }
    }

    private class ReturnDeliveryLocalNavigatorForm extends ArticleOuterNavigatorForm {
        public ReturnDeliveryLocalNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, boolean split) {
            super(parent, ID, orderReturnDeliveryLocal, toAdd, commitDeliveryLocal, split, true);
        }
    }
    
    private class ArticleInnerNavigatorForm extends ArticleOuterNavigatorForm {

        protected ArticleInnerNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, boolean splitArticle, boolean splitOuter) {
            super(parent, ID, documentClass, toAdd, commitOuter, splitArticle, splitOuter);
        }
    }

    private class DocumentInnerNavigatorForm extends ArticleInnerNavigatorForm {

        protected DocumentInnerNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, boolean split) {
            super(parent, ID, toAdd, documentClass, split, true);
        }
    }

    private class SaleWholeNavigatorForm extends DocumentInnerNavigatorForm {
        public SaleWholeNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, boolean split) {
            super(parent, ID, toAdd, orderSaleWhole, split);
        }
    }

    private class SaleRetailNavigatorForm extends DocumentInnerNavigatorForm {
        public SaleRetailNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, boolean split) {
            super(parent, ID, toAdd, orderSaleRetail, split);
        }
    }

    private class DistributeNavigatorForm extends DocumentInnerNavigatorForm {
        public DistributeNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, boolean split) {
            super(parent, ID, toAdd, orderDistribute, split);
        }
    }

    private class BalanceCheckNavigatorForm extends DocumentInnerNavigatorForm {
        public BalanceCheckNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, boolean split) {
            super(parent, ID, toAdd, balanceCheck, split);
        }
    }

    private class ReturnInnerNavigatorForm extends ArticleInnerNavigatorForm {
        final ObjectNavigator objInner;

        protected ReturnInnerNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, CustomClass commitClass, boolean splitOuter) {
            super(parent, ID, toAdd, documentClass, false, splitOuter);

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
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !toAdd);
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(returnInnerFreeQuantity.property).view),
                                  "Макс. кол-во",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)), toAdd);
            addRegularFilterGroup(filterGroup);
        }
    }

    private class ReturnSaleWholeNavigatorForm extends ReturnInnerNavigatorForm {
        private ReturnSaleWholeNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, boolean split) {
            super(parent, ID, toAdd, returnSaleWhole, commitSaleWhole, split);
        }
    }

    private class ReturnSaleRetailNavigatorForm extends ReturnInnerNavigatorForm {
        private ReturnSaleRetailNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, boolean split) {
            super(parent, ID, toAdd, returnSaleRetail, commitSaleRetail, split);
        }
    }

    private class ReturnArticleNavigatorForm extends InnerNavigatorForm {
        final ObjectNavigator objInner;
        final ObjectNavigator objOuter;

        protected ReturnArticleNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, CustomClass commitClass, boolean splitArticle) {
            super(parent, ID, documentClass, toAdd, splitArticle);

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
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)), !toAdd);
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(returnFreeQuantity.property).view),
                                  "Макс. кол-во",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
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
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !toAdd);
            filterOutGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(returnInnerFreeQuantity.property).view),
                                  "Макс. кол-во",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)), toAdd);
            addRegularFilterGroup(filterOutGroup);
        }
    }

    private class ReturnSaleWholeArticleNavigatorForm extends ReturnArticleNavigatorForm {
        private ReturnSaleWholeArticleNavigatorForm(NavigatorElement parent, boolean toAdd, int ID, boolean split) {
            super(parent, ID, toAdd, returnSaleWhole, commitSaleWhole, split);
        }
    }

    private class ReturnSaleRetailArticleNavigatorForm extends ReturnArticleNavigatorForm {
        private ReturnSaleRetailArticleNavigatorForm(NavigatorElement parent, boolean toAdd, int ID, boolean split) {
            super(parent, ID, toAdd, returnSaleRetail, commitSaleRetail, split);
        }
    }

    private class StoreArticleNavigatorForm extends NavigatorForm {
        protected StoreArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по складу");

            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup, true);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);
            ObjectNavigator objOuter = addSingleGroupObjectImplement(commitOuter, "Партия", properties, baseGroup, true);

            addPropertyView(objStore, objArt, properties, baseGroup, true);
            addPropertyView(objStore, objOuter, properties, baseGroup, true);
//            addPropertyView(objOuter, objArt, properties, baseGroup, true);
            addPropertyView(objStore, objOuter, objArt, properties, baseGroup, true);
        }
    }

    private class FormatArticleNavigatorForm extends NavigatorForm {
        protected FormatArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по форматам");

            ObjectNavigator objFormat = addSingleGroupObjectImplement(format, "Формат", properties, baseGroup, true);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objFormat, objArt, properties, baseGroup, true);
        }
    }

    private class RatesNavigatorForm extends NavigatorForm {
        protected RatesNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd?"Ввод":"Курсы");

            ObjectNavigator objDoc = addSingleGroupObjectImplement(DateClass.instance, "Дата", properties, baseGroup, true);
            if(toAdd) {
                objDoc.groupTo.gridClassView = false;
                objDoc.groupTo.fixedClassView = true;
            } else
                addFixedFilter(new NotNullFilterNavigator(getPropertyView(rate.property).view));
        }
    }

    private class DocumentRRPNavigatorForm extends ArticleNavigatorForm {

        protected DocumentRRPNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, documentRRP, toAdd, true);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(priceRRP.property).view),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)), !toAdd);
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotFilterNavigator(new NotNullFilterNavigator(getPropertyView(documentCurrentRRP.property).view)),
                                  "Без цены",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(documentFormatFreeQuantity.property).view),
                                  "На остатке",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
            addRegularFilterGroup(filterGroup);

            addHintsNoUpdate(currentRRP.property);
            addHintsNoUpdate(currentRRPDate.property);
        }
    }

    private class DocumentRateNavigatorForm extends ArticleNavigatorForm {

        protected DocumentRateNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, documentRate, toAdd, true);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(isRateChanged.property).view),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)), !toAdd);
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotFilterNavigator(new NotNullFilterNavigator(getPropertyView(documentFormatFreeQuantity.property).view)),
                                  "На остатке",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
            addRegularFilterGroup(filterGroup);
        }
    }

    private class DocumentRevalueNavigatorForm extends ArticleNavigatorForm {

        protected DocumentRevalueNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, documentRevalue, toAdd, true);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(documentRevalued.property).view),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)), true);
            addRegularFilterGroup(filterGroup);
        }
    }

    private class IncomePriceNavigatorForm extends NavigatorForm {

        protected IncomePriceNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Реестр цен", true);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(commitOrderInc, "Документ", properties, baseGroup, true);
            objDoc.groupTo.gridClassView = false;
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, articleQuantity, storePrice);

            addFixedFilter(new NotNullFilterNavigator(getPropertyView(storePrice.property).view));
        }
    }

    private class RevalueActNavigatorForm extends NavigatorForm {

        protected RevalueActNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Акт переоценки", true);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(documentStorePrice, "Документ", properties, baseGroup, true);
            objDoc.groupTo.gridClassView = false;
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, articleQuantity, storePrice, prevPrice, revalBalance);

            addFixedFilter(new CompareFilterNavigator(getPropertyView(storePrice.property).view, Compare.NOT_EQUALS, getPropertyView(prevPrice.property).view));
        }
    }

    private class PricersNavigatorForm extends NavigatorForm {

        protected PricersNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Ценники", true);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(documentStorePrice, "Документ", properties, baseGroup, true);
            objDoc.groupTo.gridClassView = false;
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, storePrice);

            addFixedFilter(new NotNullFilterNavigator(getPropertyView(storePrice.property).view));
            addFixedFilter(new NotFilterNavigator(new CompareFilterNavigator(getPropertyView(storePrice.property).view, Compare.EQUALS, addPropertyObjectImplement(prevPrice,objDoc,objArt))));
        }
    }

    protected void initAuthentication() {
        User user1 = authPolicy.addUser("user1", "user1", new UserInfo("Петр", "Петров"));
    }
}
