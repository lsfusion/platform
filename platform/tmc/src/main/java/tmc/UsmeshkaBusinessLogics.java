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
import platform.server.view.navigator.filter.*;
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
        DataAdapter adapter = new MSSQLDataAdapter("usmeshka2","169.254.1.25","sa","12345");
        UsmeshkaBusinessLogics BL = new UsmeshkaBusinessLogics(adapter,7652);

//        if(args.length>0 && args[0].equals("-F"))
//        BL.fillData();
        LocateRegistry.createRegistry(7652).rebind("BusinessLogics", BL);
//        Naming.rebind("rmi://127.0.0.1:1099/TmcBusinessLogics",new TmcBusinessLogics());
        System.out.println("Server has successfully started");
    }

    // конкретные классы
    // реализация по безналу (в опт)
    CustomClass orderSaleWhole;
    CustomClass invoiceSaleWhole;
    CustomClass commitSaleWhole;

    // реализация в розницу
    CustomClass orderSaleRetail;
    CustomClass invoiceSaleRetail;
    CustomClass commitSaleInvoiceRetail;
    CustomClass commitSaleCheckRetail;
    // инвентаризация
    CustomClass balanceCheck;
    // закупка у местного поставщика
    CustomClass orderDeliveryLocal;
    CustomClass commitDeliveryLocal;
    CustomClass orderDeliveryShopLocal;
    CustomClass orderDeliveryWarehouseLocal;
    // закупка у импортного поставщика
    CustomClass orderDeliveryImport;

    // внутреннее перемещение
    CustomClass orderDistributeShop;
    CustomClass orderDistributeWarehouse;
    // возвраты
    // возврат местному поставщику
    CustomClass orderReturnDeliveryLocal, invoiceReturnDeliveryLocal, commitReturnDeliveryLocal;
    // возврат реализации по безналу
    CustomClass returnSaleWhole;
    // возврат реализации за наличный расчет
    CustomClass returnSaleInvoiceRetail;
    CustomClass returnSaleCheckRetail;

    CustomClass order, orderInc, orderOut, orderStoreOut;
    CustomClass invoiceDocument;
    CustomClass commitOut, commitInc;
    CustomClass orderExtInc, commitExtInc;

    CustomClass documentInner, orderOuter, commitOuter;

    CustomClass move, moveInner, returnInner, returnOuter, orderInner;

    CustomClass supplier;
    CustomClass store, article, articleGroup, localSupplier, importSupplier, orderLocal, format;
    CustomClass customerWhole, customerRetail, orderWhole, orderRetail;

    CustomClass documentRevalue;
    CustomClass commitWholeShopInc;
    CustomClass documentNDS;
    CustomClass subject;

    CustomClass shop, warehouse;

    CustomClass orderShopInc, orderShopOut;
    CustomClass orderWarehouseInc, orderWarehouseOut;

    CustomClass action;

    protected void initClasses() {

        subject = addAbstractClass("Субъект", namedObject);

        action = addConcreteClass("Акция", baseClass);

        store = addAbstractClass("Склад", subject);
        shop = addConcreteClass("Магазин", store);
        warehouse = addConcreteClass("Распред. центр", store);
        article = addConcreteClass("Товар", namedObject);
        articleGroup = addConcreteClass("Группа товаров", namedObject);
        supplier = addConcreteClass("Поставщик", subject);
        localSupplier = addConcreteClass("Местный поставщик", supplier);
        importSupplier = addConcreteClass("Импортный поставщик", supplier);
        customerWhole = addConcreteClass("Оптовый покупатель", namedObject);
        customerRetail = addConcreteClass("Розничный покупатель", namedObject);

        format = addConcreteClass("Формат", namedObject);

        documentShopPrice = addAbstractClass("Изменение цены в магазине", transaction);
        documentRevalue = addConcreteClass("Переоценка в магазине", documentShopPrice);

        documentNDS = addConcreteClass("Изменение НДС", transaction);

        // заявки на приход, расход
        order = addAbstractClass("Заявка", transaction);

        orderInc = addAbstractClass("Заявка прихода на склад", order);
        orderShopInc = addAbstractClass("Заявка прихода на магазин", orderInc);
        orderWarehouseInc = addAbstractClass("Заявка прихода на распред. центр", orderInc);

        orderOut = addAbstractClass("Заявка расхода со склада", order);
        orderStoreOut = addAbstractClass("Заявка расхода со склада", orderOut);
        orderShopOut = addAbstractClass("Заявка расхода с магазина", orderOut);
        orderWarehouseOut = addAbstractClass("Заявка расхода с распред. центра", orderOut);

        invoiceDocument = addAbstractClass("Заявка на перевозку", order);
        commitOut = addAbstractClass("Отгруженная заявка", order);
        commitInc = addAbstractClass("Принятая заявка", commitOut);

        commitWholeShopInc = addAbstractClass("Принятый оптовый приход на магазин", documentShopPrice, orderShopInc, commitInc);

        // внутр. и внешние операции
        orderOuter = addAbstractClass("Заявка на внешнюю операцию", orderInc); // всегда прих., создает партию - элементарную единицу учета
        commitOuter = addAbstractClass("Внешняя операция", orderOuter, commitInc);

        documentInner = addAbstractClass("Внутренняя операция", order);
        returnInner = addAbstractClass("Возврат внутренней операции", order);
        orderInner = addAbstractClass("Заказ", documentInner);

        orderExtInc = addAbstractClass("Закупка", orderOuter);
        commitExtInc = addAbstractClass("Приход от пост.", commitOuter, orderExtInc, invoiceDocument);

        orderWhole = addAbstractClass("Оптовая операция", order);
        orderRetail = addAbstractClass("Розничная операция", order);

        orderSaleWhole = addConcreteClass("Оптовый заказ", orderWarehouseOut, orderInner, orderWhole);
        invoiceSaleWhole = addConcreteClass("Выписанный оптовый заказ", orderSaleWhole, invoiceDocument);
        commitSaleWhole = addConcreteClass("Отгруженный оптовый заказ", invoiceSaleWhole, commitOut);

        orderSaleRetail = addConcreteClass("Розничный заказ", orderShopOut, orderInner, orderRetail);
        invoiceSaleRetail = addConcreteClass("Выписанная реализация по накладной", orderSaleRetail, invoiceDocument);
        commitSaleInvoiceRetail = addConcreteClass("Отгруженная реализация по накладной", invoiceSaleRetail, commitOut);
        commitSaleCheckRetail = addConcreteClass("Реализация за наличный расчет", orderSaleRetail, commitOut);

        balanceCheck = addConcreteClass("Инвентаризация", orderStoreOut, commitOut, documentInner);

        orderDistributeShop = addConcreteClass("Заказ на внутреннее перемещение на магазин", orderStoreOut, orderShopInc, orderInner);
        addConcreteClass("Принятое внутреннее перемещение на магазин", commitWholeShopInc,
                addConcreteClass("Отгруженное внутреннее перемещение на магазин", commitOut,
                        addConcreteClass("Выписанное внутреннее перемещение на магазин", orderDistributeShop, invoiceDocument)));
        orderDistributeWarehouse = addConcreteClass("Заказ на внутреннее перемещение на распред. центр", orderStoreOut, orderWarehouseInc, orderInner);
        addConcreteClass("Принятое внутреннее перемещение на распред. центр", commitInc,
                addConcreteClass("Отгруженное внутреннее перемещение на распред. центр", commitOut,
                        addConcreteClass("Выписанное внутреннее перемещение на распред. центр", orderDistributeWarehouse, invoiceDocument)));

        orderLocal = addAbstractClass("Операция с местным поставщиком", order);

        orderDeliveryLocal = addAbstractClass("Закупка у местного поставщика", orderExtInc, orderLocal);
        commitDeliveryLocal = addAbstractClass("Приход от местного поставщика", orderDeliveryLocal, commitExtInc);

        orderDeliveryShopLocal = addConcreteClass("Закупка у местного поставщика на магазин", orderDeliveryLocal, orderShopInc);
        addConcreteClass("Приход от местного поставщика на магазин", orderDeliveryShopLocal, commitDeliveryLocal, commitWholeShopInc);

        orderDeliveryWarehouseLocal = addConcreteClass("Закупка у местного поставщика на распред. центр", orderDeliveryLocal, orderWarehouseInc);
        addConcreteClass("Приход от местного поставщика на распред. центр", orderDeliveryWarehouseLocal, commitDeliveryLocal);

        orderDeliveryImport = addConcreteClass("Закупка у импортного поставщика", orderExtInc, orderWarehouseInc);
        addConcreteClass("Приход от импортного поставщика", orderDeliveryImport, commitExtInc);

        orderReturnDeliveryLocal = addConcreteClass("Заявка на возврат местному поставщику", orderStoreOut, orderLocal);
        invoiceReturnDeliveryLocal = addConcreteClass("Выписанная заявка на возврат местному поставщику", orderReturnDeliveryLocal,invoiceDocument);
        commitReturnDeliveryLocal = addConcreteClass("Возврат местному поставщику", invoiceReturnDeliveryLocal, commitOut);

        returnSaleWhole = addConcreteClass("Оптовый возврат", orderWarehouseInc, returnInner, commitInc, orderWhole, invoiceDocument);
        returnSaleInvoiceRetail = addConcreteClass("Возврат реализации по безналу", orderShopInc, returnInner, commitInc, orderRetail);
        returnSaleCheckRetail = addConcreteClass("Возврат реализации за наличный расчет", orderShopInc, returnInner, commitInc, orderRetail);
    }

    CustomClass documentShopPrice;

    LP balanceSklFreeQuantity;
    LP articleFreeQuantity;

    protected void initProperties() {

        LP removePercent = addSFProp("((prm1*(100-prm2))/100)", DoubleClass.instance, 2);

        LP multiplyDouble2 = addMFProp(DoubleClass.instance, 2);

        LP positive = addJProp(greater2, 1, vzero);
        LP onlyPositive = addJProp(and1, 1, positive, 1);
        LP min = addSFProp("(prm1+prm2-ABS(prm1-prm2))/2", DoubleClass.instance, 2);

        LP articleToGroup = addDProp("articleToGroup", "Группа товаров", articleGroup, article); addJProp(baseGroup, "Группа товаров", name, articleToGroup, 1);

        incStore = addCUProp("incStore", "Склад (прих.)", // generics
                addDProp("incShop", "Магазин (прих.)", shop, orderShopInc),
                addDProp("incWarehouse", "Распред. центр (прих.)", warehouse, orderWarehouseInc));
        addJProp(baseGroup, "Склад (прих.)", name, incStore, 1);
        outStore = addCUProp("outCStore", "Склад (расх.)", // generics
                addDProp("outStore", "Склад (расх.)", store, orderStoreOut),
                addDProp("outShop", "Магазин (расх.)", shop, orderShopOut),
                addDProp("outWarehouse", "Распред. центр (расх.)", warehouse, orderWarehouseOut));
        addJProp(baseGroup, "Склад (расх.)", name, outStore, 1);

        orderSupplier = addCUProp("orderSupplier", "Поставщик", addDProp("localSupplier", "Местный поставщик", localSupplier, orderLocal),
                addDProp("importSupplier", "Импортный поставщик", importSupplier, orderDeliveryImport));

        LP outSubject = addCUProp(addJProp(and1, orderSupplier, 1, is(orderExtInc), 1), outStore);

        LP orderContragent = addCUProp("Контрагент", // generics
                orderSupplier,
                addDProp("wholeCustomer", "Оптовый покупатель", customerWhole, orderWhole),
                addDProp("retailCustomer", "Розничный покупатель", customerRetail, orderRetail));
        addJProp(baseGroup, "Контрагент", name, orderContragent, 1);

        LP invoiceNumber = addDProp(baseGroup, "invoiceNumber", "Накладная", StringClass.get(20), invoiceDocument);

        outerOrderQuantity = addDProp(documentGroup, "extIncOrderQuantity", "Кол-во заяв.", DoubleClass.instance, orderOuter, article);
        outerCommitedQuantity = addDProp(documentGroup, "extIncCommitedQuantity", "Кол-во принятое", DoubleClass.instance, commitOuter, article);
        outerCommitedQuantity.setDerivedChange(outerOrderQuantity, 1, 2, is(commitInc), 1);
        LP expiryDate = addDProp(baseGroup, "expiryDate", "Срок годн.", DateClass.instance, commitOuter, article);

        // для возвратных своего рода generics
        LP returnOuterQuantity = addDProp("returnDeliveryLocalQuantity", "Кол-во возврата", DoubleClass.instance, orderReturnDeliveryLocal, article, commitDeliveryLocal);

        returnInnerCommitQuantity = addCUProp(documentGroup, "Кол-во возврата", // generics
                         addDProp("returnSaleWholeQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleWhole, article, commitOuter, commitSaleWhole),
                         addDProp("returnSaleInvoiceRetailQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleInvoiceRetail, article, commitOuter, commitSaleInvoiceRetail),
                         addDProp("returnSaleCheckRetailQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleCheckRetail, article, commitOuter, commitSaleCheckRetail));

        returnInnerQuantity = addSGProp(documentGroup, "Кол-во возврата", returnInnerCommitQuantity, 1, 2, 4);
        LP returnDocumentQuantity = addCUProp("Кол-во возврата", returnOuterQuantity, returnInnerQuantity); // возвратный документ\прямой документ
        addConstraint(addJProp("При возврате контрагент документа, по которому идет возврат, должен совпадать с контрагентом возврата", and1, addJProp(diff2, orderContragent, 1, orderContragent, 2), 1, 3, returnDocumentQuantity, 1, 2, 3), false);

        LP orderInnerQuantity = addDProp("outOrderQuantity", "Кол-во операции", DoubleClass.instance, orderInner, article, commitOuter);

        // инвентаризация
        innerBalanceCheck = addDProp(documentGroup, "innerBalanceCheck", "Остаток инв.", DoubleClass.instance, balanceCheck, article, commitOuter);
        innerBalanceCheckDB = addDProp("innerBalanceCheckDB", "Остаток (по учету)", DoubleClass.instance, balanceCheck, article, commitOuter);

        innerQuantity = addCUProp(documentGroup, "innerQuantity", "Кол-во операции", returnOuterQuantity, orderInnerQuantity,
                                addSGProp("Кол-во операции", returnInnerCommitQuantity, 1, 2, 3),
                                addDUProp("balanceCheckQuantity","Кол-во инв.", innerBalanceCheckDB, innerBalanceCheck));

        LP incSklCommitedQuantity = addSGProp(moveGroup, "Кол-во прихода парт. на скл.",
                                    addCUProp(addJProp(and1, outerCommitedQuantity, 1, 2, equals2, 1, 3),
                                    addJProp(and1, innerQuantity, 1, 2, 3, is(commitInc), 1)), incStore, 1, 2, 3);

        LP outSklCommitedQuantity = addSGProp(moveGroup, "Кол-во отгр. парт. на скл.", addJProp("Кол-во отгр. парт.", and1, innerQuantity, 1, 2, 3, is(commitOut), 1), outStore, 1, 2, 3);
        LP outSklQuantity = addSGProp(moveGroup, "Кол-во заяв. парт. на скл.", innerQuantity, outStore, 1, 2, 3);

        balanceSklCommitedQuantity = addDUProp(moveGroup, "balanceSklCommitedQuantity", "Остаток парт. на скл.", incSklCommitedQuantity, outSklCommitedQuantity);
        balanceSklFreeQuantity = addDUProp(moveGroup, "balanceSklFreeQuantity", "Свободное кол-во на скл.", incSklCommitedQuantity, outSklQuantity);
        addConstraint(addJProp("Кол-во резерва должно быть не меньше нуля", greater2, vzero, balanceSklFreeQuantity, 1, 2, 3), false);

        articleFreeQuantity = addSGProp(moveGroup, "articleFreeQuantity", "Свободное кол-во на скл.", balanceSklFreeQuantity, 1, 2);

        innerBalanceCheckDB.setDerivedChange(balanceSklCommitedQuantity, outStore, 1, 2, 3);

        addJProp(moveGroup, "Остаток парт. прих.", balanceSklCommitedQuantity, incStore, 1, 2, 3);
        addJProp(moveGroup, "Остаток парт. расх.", balanceSklCommitedQuantity, outStore, 1, 2, 3);

        LP returnedInnerQuantity = addSGProp("Кол-во возвр. парт.", returnInnerCommitQuantity, 4, 2, 3);
        LP confirmedInnerQuantity = addDUProp("Кол-во подтв. парт.", addJProp(and1, orderInnerQuantity, 1, 2, 3, is(commitOut), 1) , returnedInnerQuantity);
        addConstraint(addJProp("Кол-во возврата должно быть не меньше кол-ва самой операции", greater2, vzero, confirmedInnerQuantity, 1, 2, 3), false);

        LP sameContragent = addJProp(equals2, orderContragent, 1, orderContragent, 2);

        // для док. \ товара \ парт. \ док. прод.   - кол-во подтв. парт. если совпадают контрагенты
        returnInnerFreeQuantity = addJProp(documentGroup, "Макс. кол-во по возврату парт.", and1, addJProp(and1, confirmedInnerQuantity, 4, 2, 3, sameContragent, 1, 4), 1, 2, 3, 4, is(returnInner), 1) ;
        returnFreeQuantity = addSGProp(documentGroup, "Макс. кол-во по возврату", returnInnerFreeQuantity, 1, 2, 4);

        LP documentOutSklFreeQuantity = addJProp("Свободно парт. расх.", balanceSklFreeQuantity, outStore, 1, 2, 3);
        // создаем свойства ограничения для расчета себестоимости (являются следствием addConstraint)
        documentInnerFreeQuantity = addCUProp(documentMoveGroup, "Макс. кол-во по парт.",
                            addJProp(and1, documentOutSklFreeQuantity, 1, 2, 3, sameContragent, 1, 3), // возврата поставщику - ограничение что кол-во своб. (всегда меньше кол-во подтв.) + условие что партии этого поставщика
                            addJProp(and1, documentOutSklFreeQuantity, 1, 2, 3, is(orderInner), 1), // прямого расхода - кол-во свободного для этого склада
                            innerBalanceCheckDB, // для инвентаризации - не больше зафиксированного количества по учету
                            addSGProp(returnInnerFreeQuantity, 1, 2, 3)); // возврата расхода  - кол-во подтв. этого контрагента

        // добавляем свойства по товарам
        articleInnerQuantity = addDGProp(documentGroup, "articleInnerQuantity", "Кол-во операции", 2, false, innerQuantity, 1, 2, documentInnerFreeQuantity, 1, 2, 3, date, 3, 3);
        documentFreeQuantity = addSGProp(documentMoveGroup, "Макс. кол-во по товару", documentInnerFreeQuantity, 1, 2);

        articleQuantity = addCUProp("Кол-во операции", outerCommitedQuantity, articleInnerQuantity);

        // ожидаемый приход на склад
        articleFreeOrderQuantity = addSUProp("articleFreeOrderQuantity" , "Ожидаемое своб. кол-во", Union.SUM, articleFreeQuantity, addSGProp(moveGroup, "Ожидается приход", addJProp(andNot1, articleQuantity, 1, 2, is(commitInc), 1), incStore, 1, 2)); // сумма по еще не пришедшим

        articleBalanceCheck = addDGProp(documentGroup, "articleBalanceCheck", "Остаток инв.", 2, false, innerBalanceCheck, 1, 2, innerBalanceCheckDB, 1, 2, 3, date, 3, 3);

        LP articleBalanceSklCommitedQuantity = addSGProp(moveGroup, "articleBalanceSklCommitedQuantity", "Остаток тов. на скл.", balanceSklCommitedQuantity, 1, 2);
        addJProp(documentMoveGroup, "Остаток тов. прих.", articleBalanceSklCommitedQuantity, incStore, 1, 2);
        addJProp(documentMoveGroup, "Остаток тов. расх.", articleBalanceSklCommitedQuantity, outStore, 1, 2);

        // цены
        LP shopFormat = addDProp("shopFormat", "Формат", format, shop);
        addJProp(baseGroup, "Формат", name, shopFormat, 1);

        currentRRP = addDProp(priceGroup, "currentRRP", "RRP", DoubleClass.instance, article);
        LP currentPriceRate = addDProp(priceGroup, "currentPriceRate", "Курс", DoubleClass.instance);
        LP currentFormatDiscount = addDProp(priceGroup, "currentFormatDiscount", "Скидка на формат", DoubleClass.instance, format);
        LP currentWarehouseDiscount = addDProp(priceGroup, "currentWarehouseDiscount", "Опт. скидка", DoubleClass.instance);
        LP currentPrice = addJProp(priceGroup, "Необх. цена", multiplyDouble2, currentRRP, 1, currentPriceRate);

        LP currentDate = addDProp(baseGroup, "currentDate", "Тек. дата", DateClass.instance);

        // простые акции
        LP actionFrom = addDProp(baseGroup, "actionFrom", "От", DateClass.instance, action);
        LP actionTo = addDProp(baseGroup, "actionTo", "До", DateClass.instance, action);
        LP actionDiscount = addDProp(baseGroup, "actionDiscount", "Скидка", DoubleClass.instance, action);
        LP inAction = addDProp(baseGroup, "inAction", "В акции", LogicalClass.instance, action, article);

        LP isActive = addJProp(baseGroup, "Акт. акция", between, currentDate, actionFrom, 1, actionTo, 1);
        LP articleAction = addCGProp(priceGroup, "articleAction", "Дейст. акция",
                addJProp(and1, 1, addJProp(and1, inAction, 1, 2, isActive, 1), 1, 2), inAction, 2);
        LP articleDiscount = addSUProp(Union.OVERRIDE, addCProp("0", DoubleClass.instance, 0, article), addJProp(priceGroup, "Тек. скидка", actionDiscount, articleAction, 1));

        LP currentShopDiscount = addCUProp(priceGroup, "Скидка на складе",
                addJProp(and1, currentWarehouseDiscount, is(warehouse), 1),
                addJProp(currentFormatDiscount, shopFormat, 1));

        LP requiredStorePrice = addJProp(priceGroup, "Необх. цена", removePercent,
                addJProp(removePercent, currentPrice, 1, articleDiscount, 1), 2, currentShopDiscount, 1);

        balanceFormatFreeQuantity = addSGProp(moveGroup, "Своб. кол-во по форм.", articleFreeQuantity, shopFormat, 1, 2);

        LP revalueShop = addDProp("revalueShop", "Магазин", shop, documentRevalue);
        addJProp(baseGroup, "Магазин", name, revalueShop, 1);
        priceStore = addCUProp("priceStore", "Склад (цены)", incStore, revalueShop);

        documentRevalued = addDProp(documentGroup, "isRevalued", "Переоц.", LogicalClass.instance, documentRevalue, article);
        inDocumentPrice = addCUProp("inDocumentPrice", "Изм. цены", documentRevalued, addJProp(and1, is(commitWholeShopInc), 1, articleQuantity, 1, 2));

        LP[] maxShopPriceProps = addMGProp((AbstractGroup)null, true, new String[]{"currentShopPriceDate","currentShopPriceDoc"}, new String[]{"Дата посл. цены в маг.","Посл. док. цены в маг."}, 1,
                addJProp(and1, date, 1, inDocumentPrice, 1, 2), 1, priceStore, 1, 2);
        currentShopPriceDate = maxShopPriceProps[0]; currentShopPriceDoc = maxShopPriceProps[1];

        shopPrice = addDProp(documentPriceGroup, "shopPrice", "Цена (док.)", DoubleClass.instance, documentShopPrice, article);
        shopPrice.setDerivedChange(requiredStorePrice, priceStore, 1, 2, inDocumentPrice, 1, 2);

        currentShopPrice = addJProp(priceGroup, "currentStorePrice", "Цена на складе (тек.)", shopPrice, currentShopPriceDoc, 1, 2, 2);
        addJProp(documentPriceGroup, "Цена (расх.)", currentShopPrice, outStore, 1, 2);

        LP outOfDatePrice = addJProp(and1, articleBalanceSklCommitedQuantity, 1, 2, addJProp(diff2, requiredStorePrice, 1, 2, currentShopPrice, 1, 2), 1, 2);
        documentRevalued.setDerivedChange(outOfDatePrice, priceStore, 1, 2);

        prevPrice = addDProp(documentPriceGroup, "prevPrice", "Цена пред.", DoubleClass.instance, documentShopPrice, article);
        prevPrice.setDerivedChange(currentShopPrice, priceStore, 1, 2, inDocumentPrice, 1, 2);
        revalBalance = addDProp(documentPriceGroup, "revalBalance", "Остаток переоц.", DoubleClass.instance, documentShopPrice, article);
        revalBalance.setDerivedChange(articleBalanceSklCommitedQuantity, priceStore, 1, 2, inDocumentPrice, 1, 2);

        isRevalued = addJProp(diff2, shopPrice, 1, 2, prevPrice, 1, 2); // для акта переоценки
        isNewPrice = addJProp(andNot1, inDocumentPrice, 1, 2, addJProp(equals2, shopPrice, 1, 2, prevPrice, 1, 2), 1, 2); // для ценников

        NDS = addDProp(documentGroup, "NDS", "НДС", DoubleClass.instance, documentNDS, article);
        LP[] maxNDSProps = addMGProp((AbstractGroup)null, true, new String[]{"currentNDSDate","currentNDSDoc"}, new String[]{"Дата посл. НДС","Посл. док. НДС"}, 1,
                addJProp(and1, date, 1, NDS, 1, 2), 1, 2);
        currentNDSDate = maxNDSProps[0]; currentNDSDoc = maxNDSProps[1];
        currentNDS = addJProp(baseGroup, "Тек. НДС", NDS, currentNDSDoc, 1, 1);

        // блок с логистикой\управленческими характеристиками

        // текущая схема
        LP articleSupplier = addDProp(logisticsGroup, "articleSupplier", "Поставщик товара", supplier, article);
        LP shopWarehouse = addDProp(logisticsGroup, "storeWarehouse", "Распред. центр", warehouse, shop);
        LP articleSupplierPrice = addDProp(logisticsGroup, "articleSupplierPrice", "Цена поставок", DoubleClass.instance, article);
        LP supplierCycle = addDProp(logisticsGroup, "supplierCycle", "Цикл поставок", DoubleClass.instance, supplier);
        LP shopCycle = addDProp(logisticsGroup, "shopCycle", "Цикл распределения", DoubleClass.instance, shop);

        LP supplierToWarehouse = addDProp(logisticsGroup, "supplierToWarehouse", "Пост. на распред. центр", LogicalClass.instance, supplier);

        // абстрактный товар \ склад - поставщик
        articleStoreSupplier = addSUProp("articleStoreSupplier", "Пост. товара на склад", Union.OVERRIDE, addJProp(and1, articleSupplier, 2, is(store), 1),
                        addJProp(and1, shopWarehouse, 1, addJProp(supplierToWarehouse, articleSupplier, 1), 2));
        LP storeSupplierCycle = addCUProp(addJProp(and1, supplierCycle, 2, is(store), 1), addJProp(and1, shopCycle, 1, is(warehouse), 2));

        articleStorePeriod = addJProp("articleStorePeriod", "Цикл поставок на склад", storeSupplierCycle, 1, articleStoreSupplier, 1, 2);

        LP articleFormatToSell = addDProp(logisticsGroup, "articleFormatToSell", "В ассортименте", LogicalClass.instance, format, article);
        LP articleFormatMin = addDProp(logisticsGroup, "articleFormatMin", "Страх. запас", DoubleClass.instance, format, article);

        LP articleStoreToSell = addCUProp(logisticsGroup, "articleStoreToSell", "В ассортименте",addJProp(articleFormatToSell, shopFormat, 1, 2),
                                    addDProp("articleWarehouseToSell", "В ассортименте", LogicalClass.instance, warehouse, article));
        articleStoreMin = addJProp("articleStoreMin", "Страх. запас", and1, addCUProp(logisticsGroup, "Страх. запас", addJProp(articleFormatMin, shopFormat, 1, 2),
                                    addDProp("articleWarehouseMin", "Страх. запас", DoubleClass.instance, warehouse, article)), 1, 2, articleStoreToSell, 1, 2);
        LP articleStoreForecast = addJProp(and1, addDProp(logisticsGroup, "articleStoreForecast", "Прогноз прод. (в день)", DoubleClass.instance, store, article), 1, 2, articleStoreToSell, 1, 2);

        // MAX((страховой запас+прогноз расхода до следующего цикла поставки)-остаток,0) (по внутренним складам)
        articleFullStoreDemand = addSUProp("articleFullStoreDemand", "Общ. необходимость", Union.SUM, addJProp(multiplyDouble2, addSupplierProperty(articleStoreForecast), 1, 2, articleStorePeriod, 1, 2), addSupplierProperty(articleStoreMin));
        LP articleStoreRequired = addJProp(onlyPositive, addDUProp(articleFullStoreDemand, addSupplierProperty(articleFreeOrderQuantity)), 1, 2);

        documentLogisticsRequired = addJProp(documentLogisticsGroup, "Необходимо", articleStoreRequired, incStore, 1, 2);
        // является тек. пост. для товара
        documentLogisticsSupplied = addJProp(documentLogisticsGroup, "Поставляется", equals2, outSubject, 1, addJProp(articleStoreSupplier, incStore, 1, 2), 1, 2);
        // MIN(макс. Кол-во, необходимо) и поставляется - если приход то просто необходимо и разрешено
        documentLogisticsRecommended = addJProp(documentLogisticsGroup, "Рекомендовано", and1, addCUProp(addJProp(min, documentLogisticsRequired, 1, 2, documentFreeQuantity, 1, 2),addJProp(and1, documentLogisticsRequired, 1, 2, is(orderExtInc), 1)), 1, 2, documentLogisticsSupplied, 1, 2);
    }

    private LP addSupplierProperty(LP property) {
        return addSUProp(Union.SUM, property, addSGProp(property, articleStoreSupplier, 1, 2, 2));
    }

    LP incStore;
    LP outStore;
    LP orderSupplier;

    LP articleFreeOrderQuantity;

    LP articleStoreSupplier;
    LP articleStorePeriod;
    LP articleStoreMin;
    LP articleFullStoreDemand;

    LP documentLogisticsSupplied, documentLogisticsRequired, documentLogisticsRecommended;
    LP currentNDSDate, currentNDSDoc, currentNDS, NDS;
    LP articleQuantity, prevPrice, revalBalance;
    LP shopPrice;
    LP priceStore, inDocumentPrice;
    LP isRevalued, isNewPrice, documentRevalued;
    LP balanceFormatFreeQuantity;
    LP currentShopPriceDate;
    LP currentShopPriceDoc;
    LP currentShopPrice;
    LP currentRRP;

    LP documentFreeQuantity, documentInnerFreeQuantity, returnInnerFreeQuantity, returnFreeQuantity, innerQuantity, returnInnerCommitQuantity, returnInnerQuantity;
    LP outerOrderQuantity, outerCommitedQuantity, articleBalanceCheck, articleBalanceCheckDB, innerBalanceCheck, innerBalanceCheckDB, balanceSklCommitedQuantity;
    LP articleInnerQuantity;

    AbstractGroup documentGroup;
    AbstractGroup priceGroup;
    AbstractGroup moveGroup;
    AbstractGroup logisticsGroup;
    AbstractGroup documentMoveGroup;
    AbstractGroup documentPriceGroup;
    AbstractGroup documentLogisticsGroup;

    protected void initGroups() {
        documentGroup = new AbstractGroup("Параметры документа");

        moveGroup = new AbstractGroup("Движение товаров");

        documentMoveGroup = new AbstractGroup("Текущие параметры документа");
        documentGroup.add(documentMoveGroup);

        priceGroup = new AbstractGroup("Ценовые параметры");
        
        documentPriceGroup = new AbstractGroup("Ценовые параметры документа");
        documentGroup.add(documentPriceGroup);

        logisticsGroup = new AbstractGroup("Логистические параметры");

        documentLogisticsGroup = new AbstractGroup("Логистические параметры документа");
        documentGroup.add(documentLogisticsGroup);
    }

    protected void initConstraints() {
    }

    protected void initPersistents() {
        persistents.add((AggregateProperty) balanceSklCommitedQuantity.property);
        persistents.add((AggregateProperty) balanceSklFreeQuantity.property);

        persistents.add((AggregateProperty) articleFreeQuantity.property);
        persistents.add((AggregateProperty) articleFreeOrderQuantity.property);

        persistents.add((AggregateProperty) incStore.property);
        persistents.add((AggregateProperty) outStore.property);
        persistents.add((AggregateProperty) orderSupplier.property);

        persistents.add((AggregateProperty) priceStore.property);
        persistents.add((AggregateProperty) inDocumentPrice.property);

        persistents.add((AggregateProperty) currentShopPriceDate.property);
        persistents.add((AggregateProperty) currentShopPriceDoc.property);

        persistents.add((AggregateProperty) currentNDSDate.property);
        persistents.add((AggregateProperty) currentNDSDoc.property);

        persistents.add((AggregateProperty) innerQuantity.property);

        // все связанное с ассортиментами чтобы веселее работало
        persistents.add((AggregateProperty) articleStoreSupplier.property);
        persistents.add((AggregateProperty) articleStorePeriod.property);
        persistents.add((AggregateProperty) articleStoreMin.property);
        persistents.add((AggregateProperty) articleFullStoreDemand.property);
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
            NavigatorForm deliveryShopLocal = new DeliveryShopLocalNavigatorForm(delivery, true, 1100);
                NavigatorForm deliveryShopLocalBrowse = new DeliveryShopLocalNavigatorForm(deliveryShopLocal, false, 1125);
            NavigatorForm deliveryWarehouseLocal = new DeliveryWarehouseLocalNavigatorForm(delivery, true, 1130);
                NavigatorForm deliveryWarehouseLocalBrowse = new DeliveryWarehouseLocalNavigatorForm(deliveryWarehouseLocal, false, 1135);
            NavigatorForm deliveryImport = new DeliveryImportNavigatorForm(delivery, true, 1150);
                NavigatorForm deliveryImportBrowse = new DeliveryImportNavigatorForm(deliveryImport, false, 1175);
            NavigatorForm returnDelivery = new ReturnDeliveryLocalNavigatorForm(delivery, 1400, true);
                new ReturnDeliveryLocalNavigatorForm(returnDelivery, 1410, false);

        NavigatorElement sale = new NavigatorElement(baseElement, 1200, "Управление продажами");
            NavigatorForm saleWhole = new SaleWholeNavigatorForm(sale, 1250, true);
                new SaleWholeNavigatorForm(saleWhole, 1275, false);
            NavigatorForm saleRetail = new SaleRetailNavigatorForm(sale, 1300, true);
                new SaleRetailNavigatorForm(saleRetail, 1310, false);
            NavigatorForm returnSaleWholeArticle = new ReturnSaleWholeArticleNavigatorForm(sale, true, 1450);
                new ReturnSaleWholeArticleNavigatorForm(returnSaleWholeArticle, false, 1460);
            NavigatorForm returnSaleCheckRetailArticle = new ReturnSaleCheckRetailArticleNavigatorForm(sale, true, 1475);
                new ReturnSaleCheckRetailArticleNavigatorForm(returnSaleCheckRetailArticle, false, 1485);
            NavigatorForm returnSaleInvoiceRetailArticle = new ReturnSaleInvoiceRetailArticleNavigatorForm(sale, true, 1477);
                new ReturnSaleInvoiceRetailArticleNavigatorForm(returnSaleInvoiceRetailArticle, false, 1487);

        NavigatorElement distribute = new NavigatorElement(baseElement, 3000, "Управление распределением");
            NavigatorForm distributeShopForm = new DistributeShopNavigatorForm(distribute, 3100, true);
                NavigatorForm distributeShopBrowseForm = new DistributeShopNavigatorForm(distributeShopForm, 3200, false);
            NavigatorForm distributeWarehouseForm = new DistributeWarehouseNavigatorForm(distribute, 3110, true);
                NavigatorForm distributeWarehouseBrowseForm = new DistributeWarehouseNavigatorForm(distributeWarehouseForm, 3210, false);

        NavigatorElement price = new NavigatorElement(baseElement, 2400, "Управление ценообразованием");
            NavigatorForm documentRevalue = new DocumentRevalueNavigatorForm(price, true, 2650);
                new DocumentRevalueNavigatorForm(documentRevalue, false, 2750);
            new FormatArticleNavigatorForm(price, 2200);
            new GlobalNavigatorForm(price, 5200);

        NavigatorElement tax = new NavigatorElement(baseElement, 5400, "Управление налогами");
            NavigatorForm nds = new DocumentNDSNavigatorForm(tax, true, 5800);
                new DocumentNDSNavigatorForm(nds, false, 5850);

        NavigatorElement balance = new NavigatorElement(baseElement, 1500, "Управление хранением");
            NavigatorForm balanceCheck = new BalanceCheckNavigatorForm(balance, 1350, true);
                new BalanceCheckNavigatorForm(balanceCheck, 1375, false);

        NavigatorElement store = new NavigatorElement(baseElement, 2000, "Сводная информация");
            new StoreArticleNavigatorForm(store, 2100);

        NavigatorElement print = new NavigatorElement(baseElement, 4000, "Печатные формы");
            NavigatorForm incomePrice = new IncomePriceNavigatorForm(print, 4100);
            NavigatorForm revalueAct = new RevalueActNavigatorForm(print, 4200);
            NavigatorForm pricers = new PricersNavigatorForm(print, 4300);

        commitWholeShopInc.addRelevant(incomePrice);
        documentShopPrice.addRelevant(revalueAct);
        documentShopPrice.addRelevant(pricers);
    }

    private class GlobalNavigatorForm extends NavigatorForm {
        protected GlobalNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Глобальные параметры");
            addPropertyView(properties, baseGroup, true);
        }
    }

    private class DocumentNavigatorForm extends NavigatorForm {
        final ObjectNavigator objDoc;

        protected DocumentNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, (toAdd?documentClass.caption:"Документы"));

            objDoc = addSingleGroupObjectImplement(documentClass, "Документ", properties, baseGroup, true, documentGroup, true);
            if(toAdd) {
                objDoc.groupTo.gridClassView = false;
                objDoc.groupTo.fixedClassView = true;
                objDoc.show = false;
                objDoc.addOnTransaction = true;
            }
        }
    }

    private abstract class ArticleNavigatorForm extends DocumentNavigatorForm {
        final ObjectNavigator objArt;

        protected ArticleNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, documentClass, toAdd);

            objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);
            addPropertyView(objDoc, objArt, properties, baseGroup, true, documentGroup, true);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  getDocumentArticleFilter(),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), !toAdd);
            fillExtraFilters(filterGroup, toAdd);
            addRegularFilterGroup(filterGroup);
        }

        protected abstract FilterNavigator getDocumentArticleFilter();

        protected void fillExtraFilters(RegularFilterGroupNavigator filterGroup, boolean toAdd) {
        }

        // такое дебильное множественное наследование
        public void fillExtraLogisticsFilters(RegularFilterGroupNavigator filterGroup, boolean toAdd) {
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(documentLogisticsSupplied.property).view),
                                  "Поставляется",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(documentLogisticsRequired.property).view),
                                  "Необходимо",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyView(documentLogisticsRecommended.property).view),
                                  "Рекомендовано",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)), toAdd);
        }
    }

    private abstract class InnerNavigatorForm extends ArticleNavigatorForm {

        protected FilterNavigator getDocumentArticleFilter() {
            return new NotNullFilterNavigator(getPropertyView(articleInnerQuantity.property).view);
        }

        protected InnerNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, documentClass, toAdd);

            addFixedFilter(new OrFilterNavigator(getDocumentArticleFilter(),new NotNullFilterNavigator(getPropertyView(documentFreeQuantity.property).view)));
        }
    }

    private abstract class OuterNavigatorForm extends ArticleNavigatorForm {

        protected FilterNavigator getDocumentArticleFilter() {
            return new OrFilterNavigator(new NotNullFilterNavigator(getPropertyView(outerOrderQuantity.property).view),
                                    new NotNullFilterNavigator(getPropertyView(outerCommitedQuantity.property).view));
        }

        protected OuterNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass) {
            super(parent, ID, documentClass, toAdd);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupNavigator filterGroup, boolean toAdd) {
            fillExtraLogisticsFilters(filterGroup, toAdd);
        }
    }

    private class DeliveryShopLocalNavigatorForm extends OuterNavigatorForm {
        public DeliveryShopLocalNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, orderDeliveryShopLocal);
        }
    }

    private class DeliveryWarehouseLocalNavigatorForm extends OuterNavigatorForm {
        public DeliveryWarehouseLocalNavigatorForm (NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, orderDeliveryWarehouseLocal);
        }
    }

    private class DeliveryImportNavigatorForm extends OuterNavigatorForm {
        public DeliveryImportNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, orderDeliveryImport);
        }
    }

    private class ArticleOuterNavigatorForm extends InnerNavigatorForm {
        final ObjectNavigator objOuter;

        protected ArticleOuterNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, CustomClass commitClass) {
            super(parent, ID, documentClass, toAdd);

            objOuter = addSingleGroupObjectImplement(commitClass, "Партия", properties, baseGroup, true);

            addPropertyView(objOuter, objDoc, properties, baseGroup, true, documentGroup, true);
            addPropertyView(objOuter, objDoc, objArt, properties, baseGroup, true, documentGroup, true);
            addPropertyView(objOuter, objArt, properties, baseGroup, true);

            NotNullFilterNavigator documentFilter = new NotNullFilterNavigator(getPropertyView(innerQuantity.property).view);
            addFixedFilter(new OrFilterNavigator(documentFilter, new NotNullFilterNavigator(getPropertyView(documentInnerFreeQuantity.property).view)));
            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  documentFilter,
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)),!toAdd);
            addRegularFilterGroup(filterGroup);
        }
    }

    private class ReturnDeliveryLocalNavigatorForm extends ArticleOuterNavigatorForm {
        public ReturnDeliveryLocalNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, orderReturnDeliveryLocal, toAdd, commitDeliveryLocal);
        }
    }

    private class ArticleInnerNavigatorForm extends ArticleOuterNavigatorForm {

        protected ArticleInnerNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass) {
            super(parent, ID, documentClass, toAdd, commitOuter);
        }
    }

    private class DocumentInnerNavigatorForm extends ArticleInnerNavigatorForm {

        protected DocumentInnerNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass) {
            super(parent, ID, toAdd, documentClass);
        }
    }

    private class SaleWholeNavigatorForm extends DocumentInnerNavigatorForm {
        public SaleWholeNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, orderSaleWhole);
        }
    }

    private class SaleRetailNavigatorForm extends DocumentInnerNavigatorForm {
        public SaleRetailNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, orderSaleRetail);
        }
    }

    private class DistributeNavigatorForm extends DocumentInnerNavigatorForm {
        public DistributeNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass) {
            super(parent, ID, toAdd, documentClass);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupNavigator filterGroup, boolean toAdd) {
            fillExtraLogisticsFilters(filterGroup, toAdd);
        }
    }

    private class DistributeShopNavigatorForm extends DocumentInnerNavigatorForm {
        public DistributeShopNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, orderDistributeShop);
        }
    }

    private class DistributeWarehouseNavigatorForm extends DocumentInnerNavigatorForm {
        public DistributeWarehouseNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, orderDistributeWarehouse);
        }
    }

    private class BalanceCheckNavigatorForm extends DocumentInnerNavigatorForm {
        public BalanceCheckNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, balanceCheck);
        }
    }

    private class ReturnArticleNavigatorForm extends InnerNavigatorForm {
        final ObjectNavigator objInner;
        final ObjectNavigator objOuter;

        protected ReturnArticleNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, CustomClass commitClass) {
            super(parent, ID, documentClass, toAdd);

            objInner = addSingleGroupObjectImplement(commitClass, "Документ к возврату", properties, baseGroup, true);

            addPropertyView(objInner, objDoc, properties, baseGroup, true, documentGroup, true);
            addPropertyView(objInner, objDoc, objArt, properties, baseGroup, true, documentGroup, true);
            addPropertyView(objInner, objArt, properties, baseGroup, true);

            NotNullFilterNavigator documentFilter = new NotNullFilterNavigator(getPropertyView(returnInnerQuantity.property).view);
            addFixedFilter(new OrFilterNavigator(documentFilter, new NotNullFilterNavigator(getPropertyView(returnFreeQuantity.property).view)));
            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  documentFilter,
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)), !toAdd);
            addRegularFilterGroup(filterGroup);

            objOuter = addSingleGroupObjectImplement(commitOuter, "Партия", properties, baseGroup, true);

            addPropertyView(objInner, objOuter, objDoc, properties, baseGroup, true, documentGroup, true);
            addPropertyView(objInner, objOuter, objDoc, objArt, properties, baseGroup, true, documentGroup, true);
            addPropertyView(objInner, objOuter, properties, baseGroup, true);
            addPropertyView(objInner, objOuter, objArt, properties, baseGroup, true);

            NotNullFilterNavigator documentCommitFilter = new NotNullFilterNavigator(getPropertyView(returnInnerCommitQuantity.property).view);
            addFixedFilter(new OrFilterNavigator(documentCommitFilter,new NotNullFilterNavigator(getPropertyView(returnInnerFreeQuantity.property).view)));
            RegularFilterGroupNavigator filterOutGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterOutGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  documentCommitFilter,
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !toAdd);
            addRegularFilterGroup(filterOutGroup);
        }
    }

    private class ReturnSaleWholeArticleNavigatorForm extends ReturnArticleNavigatorForm {
        private ReturnSaleWholeArticleNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, returnSaleWhole, commitSaleWhole);
        }
    }

    private class ReturnSaleInvoiceRetailArticleNavigatorForm extends ReturnArticleNavigatorForm {
        private ReturnSaleInvoiceRetailArticleNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, returnSaleInvoiceRetail, commitSaleInvoiceRetail);
        }
    }

    private class ReturnSaleCheckRetailArticleNavigatorForm extends ReturnArticleNavigatorForm {
        private ReturnSaleCheckRetailArticleNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, returnSaleCheckRetail, commitSaleCheckRetail);
        }
    }

    private class SupplierArticleNavigatorForm extends NavigatorForm {
        protected SupplierArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Ассортимент поставщиков");

            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
            ObjectNavigator objOuter = addSingleGroupObjectImplement(commitOuter, "Партия", properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);

            addPropertyView(objStore, objArt, properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
            addPropertyView(objStore, objOuter, properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
            addPropertyView(objOuter, objArt, properties, baseGroup, true);
            addPropertyView(objStore, objOuter, objArt, properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
        }
    }

    private class StoreArticleNavigatorForm extends NavigatorForm {
        protected StoreArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по складу");

            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
            ObjectNavigator objOuter = addSingleGroupObjectImplement(commitOuter, "Партия", properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);

            addPropertyView(objStore, objArt, properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
            addPropertyView(objStore, objOuter, properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
            addPropertyView(objOuter, objArt, properties, baseGroup, true);
            addPropertyView(objStore, objOuter, objArt, properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
        }
    }

    private class FormatArticleNavigatorForm extends NavigatorForm {
        protected FormatArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по форматам");

            ObjectNavigator objFormat = addSingleGroupObjectImplement(format, "Формат", properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);

            addPropertyView(objFormat, objArt, properties, baseGroup, true, moveGroup, true, priceGroup, true, logisticsGroup, true);
        }
    }

    private class DocumentRevalueNavigatorForm extends ArticleNavigatorForm {

        protected FilterNavigator getDocumentArticleFilter() {
            return new NotNullFilterNavigator(getPropertyView(documentRevalued.property).view);
        }

        protected DocumentRevalueNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, documentRevalue, toAdd);
        }
    }

    private class DocumentNDSNavigatorForm extends ArticleNavigatorForm {

        protected FilterNavigator getDocumentArticleFilter() {
            return new NotNullFilterNavigator(getPropertyView(NDS.property).view);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupNavigator filterGroup, boolean toAdd) {
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotFilterNavigator(new NotNullFilterNavigator(getPropertyView(currentNDS.property).view)),
                                  "Без НДС",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
        }

        protected DocumentNDSNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, documentNDS, toAdd);

            addHintsNoUpdate(currentNDSDoc.property);
            addHintsNoUpdate(currentNDSDate.property);
        }
    }

    private class IncomePriceNavigatorForm extends NavigatorForm {

        protected IncomePriceNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Реестр цен", true);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(commitWholeShopInc, "Документ", properties, baseGroup, true);
            objDoc.groupTo.gridClassView = false;
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, articleQuantity, shopPrice);

            addFixedFilter(new NotNullFilterNavigator(getPropertyView(shopPrice.property).view));
        }
    }

    private class RevalueActNavigatorForm extends NavigatorForm {

        protected RevalueActNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Акт переоценки", true);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(documentShopPrice, "Документ", properties, baseGroup, true);
            objDoc.groupTo.gridClassView = false;
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, articleQuantity, shopPrice, prevPrice, revalBalance);

            addFixedFilter(new CompareFilterNavigator(getPropertyView(shopPrice.property).view, Compare.NOT_EQUALS, getPropertyView(prevPrice.property).view));
        }
    }

    private class PricersNavigatorForm extends NavigatorForm {

        protected PricersNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Ценники", true);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(documentShopPrice, "Документ", properties, baseGroup, true);
            objDoc.groupTo.gridClassView = false;
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, shopPrice);

            addFixedFilter(new NotNullFilterNavigator(getPropertyView(shopPrice.property).view));
            addFixedFilter(new NotFilterNavigator(new CompareFilterNavigator(getPropertyView(shopPrice.property).view, Compare.EQUALS, addPropertyObjectImplement(prevPrice,objDoc,objArt))));
        }
    }

    protected void initAuthentication() {
        User user1 = authPolicy.addUser("user1", "user1", new UserInfo("Петр", "Петров"));
    }
}
