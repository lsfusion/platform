package tmc;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.ClientAction;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexComponentDirections;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.view.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.session.DataSession;
import platform.server.form.navigator.*;
import platform.server.form.entity.filter.*;
import tmc.integration.PanelExternalScreen;
import tmc.integration.PanelExternalScreenParameters;
import tmc.integration.exp.CashRegController;
import tmc.integration.imp.CustomerCheckRetailImportActionProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;


public class VEDBusinessLogics extends BusinessLogics<VEDBusinessLogics> {

    CashRegController cashRegController = new CashRegController(this);

    public VEDBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    // конкретные классы
    // реализация по безналу (в опт)
    CustomClass orderSaleWhole;
    CustomClass invoiceSaleWhole;
    CustomClass commitSaleWhole;

    // реализация в розницу
    CustomClass orderSaleArticleRetail;
    CustomClass orderSaleInvoiceArticleRetail;
    CustomClass commitSaleInvoiceArticleRetail;
    CustomClass commitSaleCheckArticleRetail;
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
    CustomClass returnSaleInvoice;
    CustomClass returnSaleWhole;
    // возврат реализации за наличный расчет
    CustomClass returnSaleInvoiceRetail;
    CustomClass returnSaleCheckRetail;

    CustomClass order, orderInc, orderOut, orderStoreOut;
    CustomClass invoiceDocument;
    CustomClass commitOut, commitInc;
    CustomClass orderSale;

    CustomClass documentInner;
    CustomClass orderDelivery;
    CustomClass commitDelivery;

    CustomClass move, moveInner, returnInner, returnOuter, orderInner;

    CustomClass supplier;
    public ConcreteCustomClass article;
    CustomClass store, articleGroup, localSupplier, importSupplier, orderLocal, format;
    CustomClass customerWhole;
    CustomClass customerInvoiceRetail;
    public ConcreteCustomClass customerCheckRetail;
    CustomClass orderWhole;
    CustomClass orderInvoiceRetail;
    CustomClass checkRetail;
    CustomClass orderSaleCheckRetail;

    CustomClass documentRevalue;
    CustomClass commitWholeShopInc;
    CustomClass documentNDS;
    CustomClass subject;

    CustomClass shop, warehouse;

    CustomClass orderShopInc, orderShopOut;
    CustomClass orderWarehouseInc, orderWarehouseOut;

    CustomClass action;
    CustomClass saleAction;
    CustomClass articleAction;
    CustomClass groupArticleAction;

    private final boolean noArticleGroups = true;

    CustomClass obligation;
    CustomClass coupon;
    ConcreteCustomClass giftObligation;

    public LP checkRetailExported;

    protected void initClasses() {

        subject = addAbstractClass("Субъект", baseClass.named);

        action = addAbstractClass("Акция", baseClass);
        saleAction = addConcreteClass(1, "Распродажа", action);
        articleAction = addConcreteClass(2, "Акции по позициям", action);

        groupArticleAction = addConcreteClass(3, "Группа акций", baseClass.named);

        store = addAbstractClass("Склад", subject);
        shop = addConcreteClass(4, "Магазин", store);
        warehouse = addConcreteClass(5, "Распред. центр", store);
        article = addConcreteClass(6, "Товар", baseClass.named, barcodeObject);
        articleGroup = addConcreteClass(7, "Группа товаров", baseClass.named);
        supplier = addAbstractClass("Поставщик", subject);
        localSupplier = addConcreteClass(8, "Местный поставщик", supplier);
        importSupplier = addConcreteClass(9, "Импортный поставщик", supplier);
        customerWhole = addConcreteClass(10, "Оптовый покупатель", baseClass.named);
        customerInvoiceRetail = addConcreteClass(11, "Покупатель по накладным", baseClass.named);
        customerCheckRetail = addConcreteClass(12, "Розничный покупатель", baseClass.named, barcodeObject);

        format = addConcreteClass(13, "Формат", baseClass.named);

        documentShopPrice = addAbstractClass("Изменение цены в магазине", transaction);
        documentRevalue = addConcreteClass(14, "Переоценка в магазине", documentShopPrice);

        documentNDS = addConcreteClass(15, "Изменение НДС", transaction);

        // заявки на приход, расход
        order = addAbstractClass("Заявка", transaction);

        orderInc = addAbstractClass("Заявка прихода на склад", order);
        orderShopInc = addAbstractClass("Заявка прихода на магазин", orderInc);
        orderWarehouseInc = addAbstractClass("Заявка прихода на распред. центр", orderInc);

        documentInner = addAbstractClass("Внутренняя операция", order);
        returnInner = addAbstractClass("Возврат внутренней операции", order);
        orderInner = addAbstractClass("Заказ", documentInner);

        orderOut = addAbstractClass("Заявка расхода со склада", documentInner);
        orderStoreOut = addAbstractClass("Заявка расхода со склада", orderOut);
        orderShopOut = addAbstractClass("Заявка расхода с магазина", orderOut);
        orderWarehouseOut = addAbstractClass("Заявка расхода с распред. центра", orderOut);

        invoiceDocument = addAbstractClass("Заявка на перевозку", order);
        commitOut = addAbstractClass("Отгруженная заявка", order);
        commitInc = addAbstractClass("Принятая заявка", commitOut);

        commitWholeShopInc = addAbstractClass("Принятый оптовый приход на магазин", documentShopPrice, orderShopInc, commitInc);

        // внутр. и внешние операции
        orderDelivery = addAbstractClass("Закупка", orderInc); // всегда прих., создает партию - элементарную единицу учета
        commitDelivery = addAbstractClass("Приход от пост.", orderDelivery, commitInc, invoiceDocument);

        orderSale = addAbstractClass("Продажа", orderOut);

        orderWhole = addAbstractClass("Оптовая операция", order);
        orderInvoiceRetail = addAbstractClass("Розничная операция по накладной", order);

        checkRetail = addAbstractClass("Кассовые операции", baseClass);
        orderSaleCheckRetail = addAbstractClass("Реализация через кассу", order, checkRetail);

        orderSaleWhole = addConcreteClass(16, "Оптовый заказ", orderWarehouseOut, orderInner, orderWhole, orderSale);
        invoiceSaleWhole = addConcreteClass(17, "Выписанный оптовый заказ", orderSaleWhole, invoiceDocument);
        commitSaleWhole = addConcreteClass(18, "Отгруженный оптовый заказ", invoiceSaleWhole, commitOut);

        orderSaleArticleRetail = addAbstractClass("Розничный заказ товаров", orderShopOut, orderInner, orderSale);
        orderSaleInvoiceArticleRetail = addConcreteClass(19, "Розничный заказ товаров по накладной", orderSaleArticleRetail, orderInvoiceRetail);
        commitSaleInvoiceArticleRetail = addConcreteClass(20, "Отгруженный розничный заказ по накладной", commitOut,
                addConcreteClass(21, "Выписанный розничный заказ по накладной", orderSaleInvoiceArticleRetail, invoiceDocument));
        commitSaleCheckArticleRetail = addConcreteClass(22, "Реализация товаров через кассу", orderSaleArticleRetail, commitOut, orderSaleCheckRetail);

        saleCert = addConcreteClass(23, "Реализация сертификатов", order);
        saleInvoiceCert = addConcreteClass(24, "Реализация сертификатов по накладной", saleCert, orderInvoiceRetail, invoiceDocument);
        saleCheckCert = addConcreteClass(25, "Реализация сертификатов через кассу", saleCert, orderSaleCheckRetail);

        balanceCheck = addConcreteClass(26, "Инвентаризация", orderStoreOut, commitOut, documentInner);

        orderDistributeShop = addConcreteClass(27, "Заказ на внутреннее перемещение на магазин", orderWarehouseOut, orderShopInc, orderInner);
        addConcreteClass(28, "Принятое внутреннее перемещение на магазин", commitWholeShopInc,
                addConcreteClass(29, "Отгруженное внутреннее перемещение на магазин", commitOut,
                        addConcreteClass(30, "Выписанное внутреннее перемещение на магазин", orderDistributeShop, invoiceDocument)));
        orderDistributeWarehouse = addConcreteClass(31, "Заказ на внутреннее перемещение на распред. центр", orderStoreOut, orderWarehouseInc, orderInner);
        addConcreteClass(32, "Принятое внутреннее перемещение на распред. центр", commitInc,
                addConcreteClass(33, "Отгруженное внутреннее перемещение на распред. центр", commitOut,
                        addConcreteClass(34, "Выписанное внутреннее перемещение на распред. центр", orderDistributeWarehouse, invoiceDocument)));

        orderLocal = addAbstractClass("Операция с местным поставщиком", order);

        orderDeliveryLocal = addAbstractClass("Закупка у местного поставщика", orderDelivery, orderLocal);
        commitDeliveryLocal = addAbstractClass("Приход от местного поставщика", orderDeliveryLocal, commitDelivery);

        orderDeliveryShopLocal = addConcreteClass(35, "Закупка у местного поставщика на магазин", orderDeliveryLocal, orderShopInc);
        addConcreteClass(36, "Приход от местного поставщика на магазин", orderDeliveryShopLocal, commitDeliveryLocal, commitWholeShopInc);

        orderDeliveryWarehouseLocal = addConcreteClass(37, "Закупка у местного поставщика на распред. центр", orderDeliveryLocal, orderWarehouseInc);
        addConcreteClass(38, "Приход от местного поставщика на распред. центр", orderDeliveryWarehouseLocal, commitDeliveryLocal);

        orderDeliveryImport = addConcreteClass(39, "Закупка у импортного поставщика", orderDelivery, orderWarehouseInc);
        addConcreteClass(40, "Приход от импортного поставщика", orderDeliveryImport, commitDelivery);

        orderReturnDeliveryLocal = addConcreteClass(41, "Заявка на возврат местному поставщику", orderStoreOut, documentInner, orderLocal);
        invoiceReturnDeliveryLocal = addConcreteClass(42, "Выписанная заявка на возврат местному поставщику", orderReturnDeliveryLocal, invoiceDocument);
        commitReturnDeliveryLocal = addConcreteClass(43, "Возврат местному поставщику", invoiceReturnDeliveryLocal, commitOut);

        returnSaleInvoice = addConcreteClass(44, "Возврат по накладной", orderInc, returnInner, commitInc, invoiceDocument);
        returnSaleWhole = addConcreteClass(45, "Оптовый возврат", orderWarehouseInc, orderWhole, returnSaleInvoice);
        returnSaleInvoiceRetail = addConcreteClass(46, "Возврат розничного заказа по накладной", orderShopInc, orderInvoiceRetail, returnSaleInvoice);
        returnSaleCheckRetail = addConcreteClass(47, "Возврат реализации через кассу", orderShopInc, returnInner, commitInc, checkRetail);

        obligation = addAbstractClass("Сертификат", baseClass.named, barcodeObject);
        coupon = addConcreteClass(48, "Купон", obligation);
        giftObligation = addConcreteClass(49, "Подарочный сертификат", obligation);
    }

    CustomClass saleCert;
    CustomClass saleInvoiceCert;
    CustomClass saleCheckCert;
    CustomClass documentShopPrice;

    LP balanceSklFreeQuantity;
    LP articleFreeQuantity;
    LP certToIssued, obligationSumFrom;
    LP documentBarcodePrice, documentBarcodePriceOv;
    LP invoiceNumber, invoiceSeries;

    protected void initProperties() {

        LP removePercent = addSFProp("((prm1*(100-prm2))/100)", DoubleClass.instance, 2);
        LP percent = addSFProp("(prm1*prm2/100)", DoubleClass.instance, 2);

        LP multiplyDouble2 = addMFProp(DoubleClass.instance, 2);

        LP positive = addJProp(greater2, 1, vzero);
        LP onlyPositive = addJProp(and1, 1, positive, 1);
        LP min = addSFProp("(prm1+prm2-ABS(prm1-prm2))/2", DoubleClass.instance, 2);
        LP abs = addSFProp("ABS(prm1)", DoubleClass.instance, 1);

        articleToGroup = addDProp("articleToGroup", "Группа товаров", articleGroup, article);
        articleToGroupName = addJProp(baseGroup, "Группа товаров", name, articleToGroup, 1);

        incStore = addCUProp("incStore", true, "Склад (прих.)", // generics
                addDProp("incShop", "Магазин (прих.)", shop, orderShopInc),
                addDProp("incWarehouse", "Распред. центр (прих.)", warehouse, orderWarehouseInc));
        incStoreName = addJProp(baseGroup, "Склад (прих.)", name, incStore, 1);
        outStore = addCUProp("outCStore", true, "Склад (расх.)", // generics
                addDProp("outStore", "Склад (расх.)", store, orderStoreOut),
                addDProp("outShop", "Магазин (расх.)", shop, orderShopOut),
                addDProp("outWarehouse", "Распред. центр (расх.)", warehouse, orderWarehouseOut));
        addJProp(baseGroup, "Склад (расх.)", name, outStore, 1);

        computerShop = addDProp("computerShop", "Магазин рабочего места", shop, computer);
        panelScreenComPort = addDProp(baseGroup, "panelComPort", "COM-порт табло", IntegerClass.instance, computer);
        addJProp(baseGroup, "Магазин рабочего места", name, computerShop, 1);

        orderSupplier = addCUProp("orderSupplier", true, "Поставщик", addDProp("localSupplier", "Местный поставщик", localSupplier, orderLocal),
                addDProp("importSupplier", "Импортный поставщик", importSupplier, orderDeliveryImport));

        LP outSubject = addCUProp(addJProp(and1, orderSupplier, 1, is(orderDelivery), 1), outStore);

        customerCheckRetailPhone = addDProp(baseGroup, "checkRetailCustomerPhone", "Телефон", StringClass.get(20), customerCheckRetail);
        customerCheckRetailBorn = addDProp(baseGroup, "checkRetailCustomerBorn", "Дата роджения", DateClass.instance, customerCheckRetail);
        customerCheckRetailAddress = addDProp(baseGroup, "checkRetailCustomerAddress", "Адрес", StringClass.get(40), customerCheckRetail);
        clientInitialSum = addDProp(baseGroup, "clientInitialSum", "Начальная сумма", DoubleClass.instance, customerCheckRetail);

        orderContragent = addCUProp("Контрагент", // generics
                orderSupplier,
                addDProp("wholeCustomer", "Оптовый покупатель", customerWhole, orderWhole),
                addDProp("invoiceRetailCustomer", "Розничный покупатель", customerInvoiceRetail, orderInvoiceRetail),
                addDProp("checkRetailCustomer", "Розничный покупатель", customerCheckRetail, orderSaleCheckRetail));
        nameContragent = addJProp(baseGroup, "Контрагент", name, orderContragent, 1);
        phoneContragent = addJProp(baseGroup, "Телефон", customerCheckRetailPhone, orderContragent, 1);
        bornContragent = addJProp(baseGroup, "Дата рождения", customerCheckRetailBorn, orderContragent, 1);
        addressContragent = addJProp(baseGroup, "Адрес", customerCheckRetailAddress, orderContragent, 1);
        initialSumContragent = addJProp(baseGroup, "Начальная сумма", clientInitialSum, orderContragent, 1);

//        logClientInitialSum = addLProp(clientInitialSum);

        nameContragentImpl = addJProp(true, "Контрагент", name, orderContragent, 1);
        phoneContragentImpl = addJProp(true, "Телефон", customerCheckRetailPhone, orderContragent, 1);
        bornContragentImpl = addJProp(true, "Дата рождения", customerCheckRetailBorn, orderContragent, 1);
        addressContragentImpl = addJProp(true, "Адрес", customerCheckRetailAddress, orderContragent, 1);
        initialSumContragentImpl = addJProp(true, "Начальная сумма", clientInitialSum, orderContragent, 1);

        LP sameContragent = addJProp(equals2, orderContragent, 1, orderContragent, 2);
        LP diffContragent = addJProp(diff2, orderContragent, 1, orderContragent, 2);

        invoiceNumber = addDProp(baseGroup, "invoiceNumber", "Накладная", StringClass.get(7), invoiceDocument);
        invoiceSeries = addDProp(baseGroup, "invoiceSeries", "Серия", StringClass.get(2), invoiceDocument);

        outerOrderQuantity = addDProp(documentGroup, "extIncOrderQuantity", "Кол-во заяв.", DoubleClass.instance, orderDelivery, article);
        outerCommitedQuantity = addDProp(documentGroup, "extIncCommitedQuantity", "Кол-во принятое", DoubleClass.instance, commitDelivery, article);
//        outerCommitedQuantity.setDerivedChange(outerOrderQuantity, 1, 2, is(commitInc), 1); // пока не будем делать так как идет ручное штрих-кодирование
        LP expiryDate = addDProp(baseGroup, "expiryDate", "Срок годн.", DateClass.instance, commitDelivery, article);

        // для возвратных своего рода generics
        LP returnOuterQuantity = addDProp("returnDeliveryLocalQuantity", "Кол-во возврата", DoubleClass.instance, orderReturnDeliveryLocal, article, commitDeliveryLocal);

        returnInnerCommitQuantity = addCUProp(documentGroup, "Кол-во возврата", // generics
                addDProp("returnSaleWholeQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleWhole, article, commitDelivery, commitSaleWhole),
                addDProp("returnSaleInvoiceRetailQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleInvoiceRetail, article, commitDelivery, commitSaleInvoiceArticleRetail),
                addDProp("returnSaleCheckRetailQuantity", "Кол-во возврата", DoubleClass.instance, returnSaleCheckRetail, article, commitDelivery, commitSaleCheckArticleRetail));
        LP returnSameClasses = addCUProp( // generics - для ограничения что возвращать те же классы операций, повторяет верхнее свойство
                addCProp(LogicalClass.instance, true, returnSaleWhole, commitSaleWhole),
                addCProp(LogicalClass.instance, true, returnSaleInvoiceRetail, commitSaleInvoiceArticleRetail),
                addCProp(LogicalClass.instance, true, returnSaleCheckRetail, commitSaleCheckArticleRetail));

        LP orderInnerQuantity = addDProp("outOrderQuantity", "Кол-во", DoubleClass.instance, orderInner, article, commitDelivery);

        LP returnedInnerQuantity = addSGProp("Кол-во возвр. парт.", returnInnerCommitQuantity, 4, 2, 3);
        confirmedInnerQuantity = addDUProp("Кол-во подтв. парт.", addJProp(and1, orderInnerQuantity, 1, 2, 3, is(commitOut), 1), returnedInnerQuantity);
        addConstraint(addJProp("Кол-во возврата должно быть не меньше кол-ва самой операции", greater2, vzero, confirmedInnerQuantity, 1, 2, 3), false);

        // для док. \ товара \ парт. \ док. прод.   - кол-во подтв. парт. если совпадают контрагенты
        returnInnerFreeQuantity = addJProp(documentGroup, "Дост. кол-во по возврату парт.", and(false, true), confirmedInnerQuantity, 4, 2, 3, returnSameClasses, 1, 4, diffContragent, 1, 4);
        returnInnerQuantity = addDGProp(documentGroup, "Кол-во возврата", 2, false, returnInnerCommitQuantity, 1, 2, 4, returnInnerFreeQuantity, 1, 2, 3, 4, date, 3, 3);
        LP returnDocumentQuantity = addCUProp("Кол-во возврата", returnOuterQuantity, returnInnerQuantity); // возвратный документ\прямой документ
        addConstraint(addJProp("При возврате контрагент документа, по которому идет возврат, должен совпадать с контрагентом возврата", and1, diffContragent, 1, 3, returnDocumentQuantity, 1, 2, 3), false);

        // инвентаризация
        innerBalanceCheck = addDProp(documentGroup, "innerBalanceCheck", "Остаток инв.", DoubleClass.instance, balanceCheck, article, commitDelivery);
        innerBalanceCheckDB = addDProp("innerBalanceCheckDB", "Остаток (по учету)", DoubleClass.instance, balanceCheck, article, commitDelivery);

        innerQuantity = addCUProp(documentGroup, "innerQuantity", true, "Кол-во", returnOuterQuantity, orderInnerQuantity,
                addDGProp(2, false, returnInnerCommitQuantity, 1, 2, 3, returnInnerFreeQuantity, 1, 2, 3, 4, date, 4, 4),
                addDUProp("balanceCheckQuantity", "Кол-во инв.", innerBalanceCheckDB, innerBalanceCheck));

        LP incSklCommitedQuantity = addSGProp(moveGroup, "Кол-во прихода парт. на скл.",
                addCUProp(addJProp(and1, outerCommitedQuantity, 1, 2, equals2, 1, 3),
                        addJProp(and1, innerQuantity, 1, 2, 3, is(commitInc), 1)), incStore, 1, 2, 3);

        LP outSklCommitedQuantity = addSGProp(moveGroup, "Кол-во отгр. парт. на скл.", addJProp("Кол-во отгр. парт.", and1, innerQuantity, 1, 2, 3, is(commitOut), 1), outStore, 1, 2, 3);
        LP outSklQuantity = addSGProp(moveGroup, "Кол-во заяв. парт. на скл.", innerQuantity, outStore, 1, 2, 3);

        balanceSklCommitedQuantity = addDUProp(moveGroup, "balanceSklCommitedQuantity", true, "Остаток парт. на скл.", incSklCommitedQuantity, outSklCommitedQuantity);
        balanceSklFreeQuantity = addDUProp(moveGroup, "balanceSklFreeQuantity", true, "Свободное кол-во на скл.", incSklCommitedQuantity, outSklQuantity);
        addConstraint(addJProp("Кол-во резерва должно быть не меньше нуля", greater2, vzero, balanceSklFreeQuantity, 1, 2, 3), false);

        articleFreeQuantity = addSGProp(moveGroup, "articleFreeQuantity", true, "Свободное кол-во на скл.", balanceSklFreeQuantity, 1, 2);

        innerBalanceCheckDB.setDerivedChange(balanceSklCommitedQuantity, outStore, 1, 2, 3);

        addJProp(moveGroup, "Остаток парт. прих.", balanceSklCommitedQuantity, incStore, 1, 2, 3);
        addJProp(moveGroup, "Остаток парт. расх.", balanceSklCommitedQuantity, outStore, 1, 2, 3);

        returnFreeQuantity = addSGProp(documentGroup, "Дост. кол-во к возврату", returnInnerFreeQuantity, 1, 2, 4);

        LP documentOutSklFreeQuantity = addJProp("Дост. парт. расх.", balanceSklFreeQuantity, outStore, 1, 2, 3);
        // создаем свойства ограничения для расчета себестоимости (являются следствием addConstraint)
        documentInnerFreeQuantity = addCUProp(documentMoveGroup, "Дост. кол-во по парт.",
                addJProp(and1, documentOutSklFreeQuantity, 1, 2, 3, sameContragent, 1, 3), // возврата поставщику - ограничение что кол-во своб. (всегда меньше кол-во подтв.) + условие что партии этого поставщика
                addJProp(and1, documentOutSklFreeQuantity, 1, 2, 3, is(orderInner), 1), // прямого расхода - кол-во свободного для этого склада
                innerBalanceCheckDB, // для инвентаризации - не больше зафиксированного количества по учету
                addSGProp(returnInnerFreeQuantity, 1, 2, 3)); // возврата расхода  - кол-во подтв. этого контрагента

        sumReturnedQuantity = addSGProp(documentGroup, "Кол-во возврата", returnInnerQuantity, 1, 3);
        sumReturnedQuantityFree = addSGProp(documentGroup, "Дост. кол-во к возврату", returnFreeQuantity, 1, 3);

        // добавляем свойства по товарам
        articleInnerQuantity = addDGProp(documentGroup, "articleInnerQuantity", "Кол-во", 2, false, innerQuantity, 1, 2, documentInnerFreeQuantity, 1, 2, 3, date, 3, 3);
        documentFreeQuantity = addSGProp(documentMoveGroup, "Доступ. кол-во", documentInnerFreeQuantity, 1, 2);

        articleQuantity = addCUProp("Кол-во", outerCommitedQuantity, articleInnerQuantity);
        articleOrderQuantity = addCUProp("Заяв. кол-во", outerOrderQuantity, articleInnerQuantity);
        LP absQuantity = addSGProp("Всего тов.", addJProp(abs, articleQuantity, 1, 2), 1);
        addConstraint(addJProp("Нельзя создавать пустые документы", addJProp(and(true, true), is(order), 1, is(saleCert), 1, addJProp(greater2, absQuantity, 1, vzero), 1), 1), false);

        // ожидаемый приход на склад
        articleFreeOrderQuantity = addSUProp("articleFreeOrderQuantity", true, "Ожидаемое своб. кол-во", Union.SUM, articleFreeQuantity, addSGProp(moveGroup, "Ожидается приход", addJProp(andNot1, articleOrderQuantity, 1, 2, is(commitInc), 1), incStore, 1, 2)); // сумма по еще не пришедшим

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

        // простые акции
        LP actionFrom = addDProp(baseGroup, "actionFrom", "От", DateClass.instance, action);
        LP actionTo = addDProp(baseGroup, "actionTo", "До", DateClass.instance, action);
        LP actionDiscount = addDProp(baseGroup, "actionDiscount", "Скидка", DoubleClass.instance, action);

        LP customerCheckRetailDiscount = addDProp(baseGroup, "customerCheckRetailDiscount", "Мин. скидка", DoubleClass.instance, customerCheckRetail);

        LP xorActionAll = addDProp(baseGroup, "xorActionAll", "Вкл./искл.", LogicalClass.instance, action);
        LP xorActionArticleGroup = addDProp(baseGroup, "xorActionArticleGroup", "Вкл./искл.", LogicalClass.instance, action, articleGroup);
        xorActionArticle = addDProp(baseGroup, "xorArticle", "Вкл./искл.", LogicalClass.instance, action, article);
        inAction = addXorUProp(baseGroup, "inAction", true, "В акции", xorActionArticle, addXorUProp(
                addJProp(and1, xorActionAll, 1, is(article), 2), addJProp(xorActionArticleGroup, 1, articleToGroup, 2)));

        LP isStarted = addJProp(baseGroup, "Началась", and(true, true), is(action), 1,
                addJProp(less2, currentDate, actionFrom, 1), 1,
                addJProp(greater2, currentDate, actionTo, 1), 1);
        LP articleSaleAction = addCGProp(priceGroup, false, "articleAction", "Дейст. распродажа",
                addJProp(and1, 1, addJProp(and1, inAction, 1, 2, addJProp(and1, isStarted, 1, is(saleAction), 1), 1), 1, 2), inAction, 2);
        LP articleDiscount = addSUProp(Union.OVERRIDE, addCProp(DoubleClass.instance, 0, article), addJProp(priceGroup, "Тек. скидка", actionDiscount, articleSaleAction, 1));
        LP actionNoExtraDiscount = addDProp(baseGroup, "actionNoExtraDiscount", "Без доп. скидок", LogicalClass.instance, saleAction);

        LP articleActionToGroup = addDProp("articleActionToGroup", "Группа акций", groupArticleAction, articleAction);
        addJProp(baseGroup, "Группа акций", name, articleActionToGroup, 1);

        LP articleActionHourFrom = addDProp(baseGroup, "articleActionHourFrom", "Час от", DoubleClass.instance, articleAction);
        LP articleActionHourTo = addDProp(baseGroup, "articleActionHourTo", "Час до", DoubleClass.instance, articleAction);
        LP articleActionClientSum = addDProp(baseGroup, "articleActionClientSum", "Нак. сумма от", DoubleClass.instance, articleAction);
        LP articleActionQuantity = addDProp(baseGroup, "articleActionQuantity", "Кол-во от", DoubleClass.instance, articleAction);
        LP articleActionBirthDay = addDProp(baseGroup, "articleActionBirthDay", "День рожд.", LogicalClass.instance, articleAction);

        // продажа облигаций
        issueObligation = addCUProp(documentPriceGroup, "Выдать", addDProp("saleCertGiftObligation", "Выдать", LogicalClass.instance, saleCert, giftObligation),
                addDProp("orderSaleCoupon", "Выдать", LogicalClass.instance, commitSaleCheckArticleRetail, coupon));
        obligationIssued = addCGProp(null, "obligationIssued", true, "Выд. документ", addJProp(and1, 1, issueObligation, 1, 2), issueObligation, 2);

        obligationSum = addDProp(baseGroup, "obligationSum", "Сумма", DoubleClass.instance, obligation);
        obligationSumFrom = addDProp(baseGroup, "obligationSumFrom", "Сумма покупки", DoubleClass.instance, obligation);

        LP couponMaxPercent = addDProp(baseGroup, "couponMaxPercent", "Макс. процент по купонам", DoubleClass.instance);

        LP currentStoreDiscount = addCUProp(priceGroup, "Скидка на складе",
                addJProp(and1, currentWarehouseDiscount, is(warehouse), 1),
                addJProp(currentFormatDiscount, shopFormat, 1));

        LP actionPrice = addJProp(priceGroup, "Акц. цена", removePercent, currentPrice, 1, articleDiscount, 1);
        LP requiredStorePrice = addJProp(priceGroup, "Необх. цена", removePercent, actionPrice, 2, currentStoreDiscount, 1);

        balanceFormatFreeQuantity = addSGProp(moveGroup, "Своб. кол-во по форм.", articleFreeQuantity, shopFormat, 1, 2);

        LP revalueShop = addDProp("revalueShop", "Магазин", shop, documentRevalue);
        addJProp(baseGroup, "Магазин", name, revalueShop, 1);
        priceStore = addCUProp("priceStore", true, "Склад (цены)", incStore, revalueShop);

        documentRevalued = addDProp(documentGroup, "isRevalued", "Переоц.", LogicalClass.instance, documentRevalue, article);
        inDocumentPrice = addCUProp("inDocumentPrice", true, "Изм. цены", documentRevalued, addJProp(and1, is(commitWholeShopInc), 1, articleQuantity, 1, 2));

        LP[] maxShopPriceProps = addMGProp((AbstractGroup) null, true, new String[]{"currentShopPriceDate", "currentShopPriceDoc"}, new String[]{"Дата посл. цены в маг.", "Посл. док. цены в маг."}, 1,
                addJProp(and1, date, 1, inDocumentPrice, 1, 2), 1, priceStore, 1, 2);
        currentShopPriceDate = maxShopPriceProps[0]; currentShopPriceDoc = maxShopPriceProps[1];
        addPersistent(currentShopPriceDate); addPersistent(currentShopPriceDoc);

        shopPrice = addDCProp(documentPriceGroup, "shopPrice", "Цена (док.)", requiredStorePrice, priceStore, 1, 2, inDocumentPrice, 1, 2);

        currentShopPrice = addJProp(priceGroup, "currentShopPrice", "Цена на складе (тек.)", shopPrice, currentShopPriceDoc, 1, 2, 2);

        LP outOfDatePrice = addJProp(and(false, false), vtrue, articleBalanceSklCommitedQuantity, 1, 2, addJProp(diff2, requiredStorePrice, 1, 2, currentShopPrice, 1, 2), 1, 2);
        documentRevalued.setDerivedChange(outOfDatePrice, priceStore, 1, 2);

        prevPrice = addDCProp(documentPriceGroup, "prevPrice", "Цена пред.", currentShopPrice, priceStore, 1, 2, inDocumentPrice, 1, 2);
        revalBalance = addDCProp(documentPriceGroup, "revalBalance", "Остаток переоц.", articleBalanceSklCommitedQuantity, priceStore, 1, 2, inDocumentPrice, 1, 2);

        isRevalued = addJProp(diff2, shopPrice, 1, 2, prevPrice, 1, 2); // для акта переоценки
        isNewPrice = addJProp(andNot1, inDocumentPrice, 1, 2, addJProp(equals2, shopPrice, 1, 2, prevPrice, 1, 2), 1, 2); // для ценников

        LP saleStorePrice = addCUProp(priceGroup, "Цена прод.", addJProp(and1, requiredStorePrice, 1, 2, is(warehouse), 1), currentShopPrice);

        NDS = addDProp(documentGroup, "NDS", "НДС", DoubleClass.instance, documentNDS, article);
        LP[] maxNDSProps = addMGProp((AbstractGroup) null, true, new String[]{"currentNDSDate", "currentNDSDoc"}, new String[]{"Дата посл. НДС", "Посл. док. НДС"}, 1,
                addJProp(and1, date, 1, NDS, 1, 2), 1, 2);
        currentNDSDate = maxNDSProps[0]; currentNDSDoc = maxNDSProps[1];
        addPersistent(currentNDSDate); addPersistent(currentNDSDoc);
        currentNDS = addJProp(baseGroup, "Тек. НДС", NDS, currentNDSDoc, 1, 1);

        // блок с логистикой\управленческими характеристиками

        // текущая схема
        articleSupplier = addDProp("articleSupplier", "Поставщик товара", supplier, article);
        addJProp(logisticsGroup, "Поставщик товара", name, articleSupplier, 1);
        LP shopWarehouse = addDProp("storeWarehouse", "Распред. центр", warehouse, shop);
        addJProp(logisticsGroup, "Распред. центр", name, shopWarehouse, 1);
        LP articleSupplierPrice = addDProp(logisticsGroup, "articleSupplierPrice", "Цена поставок", DoubleClass.instance, article);
        LP supplierCycle = addDProp(logisticsGroup, "supplierCycle", "Цикл поставок", DoubleClass.instance, supplier);
        LP shopCycle = addDProp(logisticsGroup, "shopCycle", "Цикл распределения", DoubleClass.instance, shop);

        LP supplierToWarehouse = addDProp(logisticsGroup, "supplierToWarehouse", "Пост. на распред. центр", LogicalClass.instance, supplier);

        // абстрактный товар \ склад - поставщик
        articleStoreSupplier = addSUProp("articleStoreSupplier", true, "Пост. товара на склад", Union.OVERRIDE, addJProp(and1, articleSupplier, 2, is(store), 1),
                addJProp(and1, shopWarehouse, 1, addJProp(supplierToWarehouse, articleSupplier, 1), 2));
        LP storeSupplierCycle = addCUProp(addJProp(and1, supplierCycle, 2, is(store), 1), addJProp(and1, shopCycle, 1, is(warehouse), 2));

        articleStorePeriod = addJProp("articleStorePeriod", true, "Цикл поставок на склад", storeSupplierCycle, 1, articleStoreSupplier, 1, 2);

        LP articleFormatToSell = addDProp(logisticsGroup, "articleFormatToSell", "В ассортименте", LogicalClass.instance, format, article);
        LP articleFormatMin = addDProp(logisticsGroup, "articleFormatMin", "Страх. запас", DoubleClass.instance, format, article);

        LP articleStoreToSell = addCUProp(logisticsGroup, "articleStoreToSell", "В ассортименте", addJProp(articleFormatToSell, shopFormat, 1, 2),
                addDProp("articleWarehouseToSell", "В ассортименте", LogicalClass.instance, warehouse, article));
        articleStoreMin = addJProp("articleStoreMin", true, "Страх. запас", and1, addCUProp(logisticsGroup, "Страх. запас", addJProp(articleFormatMin, shopFormat, 1, 2),
                addDProp("articleWarehouseMin", "Страх. запас", DoubleClass.instance, warehouse, article)), 1, 2, articleStoreToSell, 1, 2);
        LP articleStoreForecast = addJProp(and1, addDProp(logisticsGroup, "articleStoreForecast", "Прогноз прод. (в день)", DoubleClass.instance, store, article), 1, 2, articleStoreToSell, 1, 2);

        // MAX((страховой запас+прогноз расхода до следующего цикла поставки)-остаток,0) (по внутренним складам)
        articleFullStoreDemand = addSUProp("articleFullStoreDemand", true, "Общ. необходимость", Union.SUM, addJProp(multiplyDouble2, addSupplierProperty(articleStoreForecast), 1, 2, articleStorePeriod, 1, 2), addSupplierProperty(articleStoreMin));
        LP articleStoreRequired = addJProp(onlyPositive, addDUProp(articleFullStoreDemand, addSupplierProperty(articleFreeOrderQuantity)), 1, 2);

        documentLogisticsRequired = addJProp(documentLogisticsGroup, "Необходимо", articleStoreRequired, incStore, 1, 2);
        documentLogisticsSupplied = addJProp(documentLogisticsGroup, "Поставляется", equals2, outSubject, 1, addJProp(articleStoreSupplier, incStore, 1, 2), 1, 2);
        documentLogisticsRecommended = addJProp(documentLogisticsGroup, "Рекомендовано", min, documentLogisticsRequired, 1, 2, documentFreeQuantity, 1, 2);

        LP orderClientSaleSum = addDProp("orderClientSaleSum", "Нак. сумма", DoubleClass.instance, orderSaleArticleRetail);
        LP orderClientInitialSum = addDCProp("orderClientInitialSum", "Нак. сумма", clientInitialSum, true, orderContragent, 1, orderSaleArticleRetail);
        orderClientSum = addSUProp(baseGroup, "Нак. сумма", Union.SUM, addCProp(DoubleClass.instance, 0, orderSaleArticleRetail), orderClientSaleSum, orderClientInitialSum);
        LP orderHour = addDCProp(baseGroup, "orderHour", "Час", currentHour, is(orderSale), 1, orderSaleArticleRetail);

        changeQuantityTime = addTCProp(Time.EPOCH, "changeQuantityTime", "Время выбора", articleInnerQuantity, orderSaleArticleRetail);
        changeQuantityOrder = addOProp(documentGroup, "Номер", addJProp(and1, addCProp(IntegerClass.instance, 1), articleInnerQuantity, 1, 2), false, true, true, 1, 1, changeQuantityTime, 1, 2);

        orderSaleDocPrice = addDCProp("orderSalePrice", "Цена прод.", saleStorePrice, outStore, 1, 2, articleQuantity, 1, 2, orderSale);
        orderSalePrice = addSUProp(documentPriceGroup, "Цена прод.", Union.OVERRIDE, addJProp(and1, addJProp(saleStorePrice, outStore, 1, 2), 1, 2, is(orderSale), 1), orderSaleDocPrice);
        documentBarcodePrice = addJProp("Цена", orderSalePrice, 1, barcodeToObject, 2);
        documentBarcodePriceOv = addSUProp("Цена", Union.OVERRIDE, documentBarcodePrice, addJProp(and1, addJProp(obligationSum, barcodeToObject, 1), 2, is(order), 1));

        LP monthDay = addSFProp("EXTRACT(DOY FROM prm1)", IntegerClass.instance, 1);
        LP isOrderContragentBirthDay = addJProp(equals2, addJProp(monthDay, date, 1), 1, addJProp(monthDay, addJProp(customerCheckRetailBorn, orderContragent, 1), 1), 1);
        LP articleActionActive = addJProp(and(false, false, false, false, true, true, true, true, true), articleQuantity, 1, 2, is(orderSaleArticleRetail), 1, is(articleAction), 3, inAction, 3, 2, isStarted, 3,
                addJProp(less2, articleQuantity, 1, 2, articleActionQuantity, 3), 1, 2, 3,
                addJProp(and(false, true), articleActionBirthDay, 2, is(orderSaleArticleRetail), 1, isOrderContragentBirthDay, 1), 1, 3,
                addJProp(less2, orderClientSum, 1, articleActionClientSum, 2), 1, 3,
                addJProp(less2, orderHour, 1, articleActionHourFrom, 2), 1, 3,
                addJProp(greater2, orderHour, 1, articleActionHourTo, 2), 1, 3);
        
        orderArticleSaleDiscount = addDCProp(baseGroup, "orderArticleSaleDiscount", "Скидка", addJProp(andNot1,
                addSUProp(Union.MAX,
                    addSGProp(addMGProp(addJProp(and1, actionDiscount, 3, articleActionActive, 1, 2, 3), 1, 2, articleActionToGroup, 3), 1, 2),
                    addJProp(and1, addJProp(customerCheckRetailDiscount, orderContragent, 1), 1, is(article), 2)), 1, 2,
                addJProp(actionNoExtraDiscount, articleSaleAction, 1), 2),
                true, 1, 2, is(orderSaleArticleRetail), 1);

        LP orderArticleSaleSum = addJProp(documentPriceGroup, "Сумма прод.", multiplyDouble2, articleQuantity, 1, 2, orderSaleDocPrice, 1, 2);
        LP round1 = addSFProp("(ROUND(CAST((prm1) as NUMERIC(15,3)),-1))", IntegerClass.instance, 1);
        LP orderArticleSaleDiscountSum = addJProp(documentPriceGroup, "Сумма скидки", round1, addJProp(percent, orderArticleSaleSum, 1, 2, orderArticleSaleDiscount, 1, 2), 1, 2);
        orderArticleSaleSumWithDiscount = addDUProp(documentPriceGroup, "Сумма к оплате", orderArticleSaleSum, orderArticleSaleDiscountSum);
        orderSaleDiscountSum = addSGProp(documentAggrPriceGroup, "Сумма скидки", orderArticleSaleDiscountSum, 1);
        orderSalePay = addCUProp(documentAggrPriceGroup, "Сумма чека",
                addSGProp(addJProp(and(false, false), obligationSum, 2, issueObligation, 1, 2, is(giftObligation), 2), 1),
                addSGProp(orderArticleSaleSumWithDiscount, 1));

        LP returnArticleSaleSum = addJProp(documentPriceGroup, "Сумма возвр.", multiplyDouble2, returnInnerQuantity, 1, 2, 3, orderSaleDocPrice, 3, 2);
        LP returnArticleSaleDiscount = addJProp(documentPriceGroup, "Сумма скидки возвр.",round1, addJProp(percent, returnArticleSaleSum, 1, 2, 3, orderArticleSaleDiscount, 3, 2), 1, 2, 3);
        returnArticleSalePay = addDUProp(documentPriceGroup, "Сумма к возвр.", returnArticleSaleSum, returnArticleSaleDiscount);
        returnSaleDiscount = addSGProp(documentAggrPriceGroup, "Сумма скидки возвр.", returnArticleSaleDiscount, 1);
        returnSalePay = addDUProp(documentAggrPriceGroup, "Сумма к возвр.", addSGProp("Сумма возвр.", returnArticleSaleSum, 1), returnSaleDiscount);

        LP orderDeliveryPrice = addDCProp("orderDeliveryPrice", "Цена закуп.", articleSupplierPrice, 2, articleQuantity, 1, 2, orderDelivery);
        addSUProp(documentPriceGroup, "Цена закуп.", Union.OVERRIDE, addJProp(and1, articleSupplierPrice, 2, is(orderDelivery), 1), orderDeliveryPrice);

        orderSaleUseObligation = addDProp(documentPriceGroup, "orderSaleUseObligation", "Использовать", LogicalClass.instance, commitSaleCheckArticleRetail, obligation);
        LP obligationUseSum = addJProp(and1, obligationSum, 2, orderSaleUseObligation, 1, 2);
        obligationDocument = addCGProp(null, "obligationDocument", "Исп. документ", addJProp(and1, 1, orderSaleUseObligation, 1, 2), orderSaleUseObligation, 2);

        LP addDays = addSFProp("prm1+prm2", DateClass.instance, 2);

        couponStart = addDProp(baseGroup, "couponStart", "Дата начала купонов", DateClass.instance);
        LP couponExpiry = addDProp(baseGroup, "couponExpiry", "Дата окончания купонов", DateClass.instance);
        LP certExpiry = addDProp(baseGroup, "certExpiry", "Срок действия серт.", IntegerClass.instance);

        LP dateIssued = addJProp("Дата выдачи", date, obligationIssued, 1);
        LP couponFromIssued = addDCProp(baseGroup, "couponFromIssued", "Дата начала", couponStart, dateIssued, 1, coupon);
        LP couponToIssued = addDCProp("couponToIssued", "Дата окончания", couponExpiry, obligationIssued, 1, coupon);
        certToIssued = addDCProp("certToIssued", "Дата окончания", addJProp(addDays, 1, certExpiry), dateIssued, 1, giftObligation);
        LP obligationToIssued = addCUProp(baseGroup, "obligationToIssued", "Дата окончания", couponToIssued, certToIssued);
        orderSaleObligationCanBeUsed = addJProp(and(false, true, true, true), is(commitSaleCheckArticleRetail), 1, obligationIssued, 2,
                addJProp(less2, orderSalePay, 1, obligationSumFrom, 2), 1, 2,
                addJProp(greater2, date, 1, obligationToIssued, 2), 1, 2,
                addJProp(less2, date, 1, couponFromIssued, 2), 1, 2);
        addConstraint(addJProp("Нельзя использовать выбранный сертификат", andNot1, orderSaleUseObligation, 1, 2, orderSaleObligationCanBeUsed, 1, 2), false);
        LP orderSalePayGiftObligation = addSGProp(addJProp(and1, obligationUseSum, 1, 2, is(giftObligation), 2), 1);
        LP orderSalePayCoupon = addJProp(min, addSGProp(addJProp(and1, obligationUseSum, 1, 2, is(coupon), 2), 1), 1, addJProp(percent, orderSalePay, 1, couponMaxPercent), 1);

        orderSalePayNoObligation = addJProp(documentAggrPriceGroup, "Сумма к оплате", onlyPositive, addDUProp(orderSalePay, addSUProp(Union.SUM, orderSalePayGiftObligation, orderSalePayCoupon)), 1);
        LP orderSalePayCoeff = addJProp("Коэфф. скидки", divideDouble, orderSalePayNoObligation, 1, orderSalePay, 1);
        orderArticleSaleSumCoeff = addJProp(documentPriceGroup, "Сумма со скидкой", addMFProp(DoubleClass.instance, 2), orderArticleSaleSumWithDiscount, 1, 2, orderSalePayCoeff, 1);

        LP clientSaleSum = addSGProp(orderSalePayNoObligation, orderContragent, 1);
        orderClientSaleSum.setDerivedChange(clientSaleSum, orderContragent, 1);
        clientSum = addSUProp(baseGroup, "clientSum", true, "Нак. сумма", Union.SUM, clientSaleSum, clientInitialSum);

        orderSalePayCash = addDProp(documentPriceGroup, "orderSalePayCash", "Наличными", DoubleClass.instance, orderSaleCheckRetail);
        orderSalePayCard = addDProp(documentPriceGroup, "orderSalePayCard", "Карточкой", DoubleClass.instance, orderSaleCheckRetail);

        // сдача/доплата
        LP orderSaleDiffSum = addJProp(and1, addDUProp(orderSalePayNoObligation, addSUProp(Union.SUM, orderSalePayCard, orderSalePayCash)), 1, is(orderSaleCheckRetail), 1);
//        orderSaleDiff = addIfElseUProp(documentAggrPriceGroup, "Необходимо", addJProp(string2, addCProp(StringClass.get(6), "Допл:"), orderSaleDiffSum, 1),
//                addJProp(string2, addCProp(StringClass.get(6), "Сдача:"), addNUProp(orderSaleDiffSum), 1), addJProp(positive, orderSaleDiffSum, 1), 1);
        orderSaleToDo = addJProp(documentAggrPriceGroup, "Необходимо", and1, addIfElseUProp(addCProp(StringClass.get(5), "Итого", orderSaleCheckRetail),
                addCProp(StringClass.get(5), "Сдача", orderSaleCheckRetail), addJProp(positive, orderSaleDiffSum, 1), 1), 1, orderSaleDiffSum, 1);
        orderSaleToDoSum = addJProp(documentAggrPriceGroup, "Сумма необх.", abs, orderSaleDiffSum, 1);

        LP couponCanBeUsed = addJProp(greater2, addJProp(date, obligationIssued, 1), 2, date, 1);

        barcodeAction2 = addJProp(true, "Ввод штрих-кода 2",
                addCUProp(
                        addSCProp(addIfElseUProp(articleQuantity, articleOrderQuantity, is(commitInc), 1)),
                        addIfElseUProp(orderSaleUseObligation, issueObligation, addJProp(diff2, 1, obligationIssued, 2), 1, 2),
                        addJProp(equals2, orderContragent, 1, 2),
                        xorActionArticle, articleFormatToSell, NDS, documentRevalued,
                        addJProp(and1, changeUser, 2, is(baseClass), 1)
                ), 1, barcodeToObject, 2);
        barcodeAction3 = addJProp(true, "Ввод штрих-кода 3",
                addCUProp(
                        addJProp(and(false, false), changeUser, 2, is(baseClass), 1, is(baseClass), 3),
                        addSCProp(returnInnerQuantity)
                ), 1, barcodeToObject, 3, 2);
        seekAction = addJProp(true, "Поиск штрих-кода", addSAProp(null), barcodeToObject, 1);

        barcodeNotFoundMessage = addJProp(true, "", and(false, true), addMAProp("Штрих-код не найден!", "Ошибка"), is(StringClass.get(13)), 1, barcodeToObject, 1);

        LP xorCouponArticleGroup = addDProp(couponGroup, "xorCouponArticleGroup", "Вкл.", LogicalClass.instance, articleGroup);
        LP xorCouponArticle = addDProp(couponGroup, "xorCouponArticle", "Вкл./искл.", LogicalClass.instance, article);
        inCoupon = addXorUProp(couponGroup, "inCoupon", true, "Выд. купон", xorCouponArticle, addJProp(xorCouponArticleGroup, articleToGroup, 1));

        couponIssueSum = addDProp(couponGroup, "couponIssueSum", "Сумма купона", DoubleClass.instance, DoubleClass.instance);

        LP couponDocToIssueSum = addDCProp("couponDocToIssueSum", "Сумма купона к выд.", addIfProp(addMGProp(addJProp(and1, couponIssueSum, 3, addJProp(greater2, orderSaleDocPrice, 1, 2, 3), 1, 2, 3), 1, 2), false, inCoupon, 2), true, 1, 2, articleQuantity, 1, 2, commitSaleCheckArticleRetail); // здесь конечно хорошо было бы orderSaleDocPrice вытащить за скобки, но будет висячий ключ поэтому приходится пока немого извращаться

        couponToIssueQuantity = addDUProp("К выдаче", addSGProp(articleQuantity, 1, couponDocToIssueSum, 1, 2),
                addSGProp(addJProp(and1, addCProp(DoubleClass.instance, 1), addIfProp(issueObligation, false, is(coupon), 2), 1, 2), 1, obligationSum, 2));
        couponToIssueConstraint = addJProp("Кол-во выданных купонов не соответствует требуемому", diff2, couponToIssueQuantity, 1, 2, vzero);
        addConstraint(couponToIssueConstraint, false);

        LP orderUser = addDCProp("orderUser", "Исп-ль заказа", currentUser, true, is(order), 1);
        orderUserName = addJProp("Исп-ль заказа", name, orderUser, 1);

        checkRetailExported = addDProp("checkRetailExported", "Экспортирован", LogicalClass.instance, checkRetail);

        cashRegController = new CashRegController(this); // бред конечно создавать его здесь, но влом создавать getCashRegController()
        cashRegController.addCashRegProperties();

        LP importCustomerCheckRetail = addProp(baseGroup, new CustomerCheckRetailImportActionProperty(this, genSID()));

    }

    private LP addSupplierProperty(LP property) {
        return addSUProp(Union.SUM, property, addSGProp(property, articleStoreSupplier, 1, 2, 2));
    }

    LP orderUserName;
    LP articleToGroup;
    LP couponToIssueConstraint;
    LP couponIssueSum;
    LP couponToIssueQuantity;
    LP inCoupon;
    public LP issueObligation;
    LP obligationIssued;
    public LP obligationSum;
    LP orderSaleCoupon;
    LP barcodeAction2;
    LP barcodeAction3;
    LP seekAction;
    LP barcodeNotFoundMessage;
    LP orderClientSum;
    public LP orderArticleSaleSumWithDiscount;
    public LP orderSaleDocPrice;
    public LP orderSalePrice;
    LP changeQuantityOrder;
    LP computerShop;
    LP panelScreenComPort;
    LP orderSalePayCash;
    LP orderSalePayCard;
    LP changeQuantityTime;
    LP confirmedInnerQuantity;
    LP couponStart;
    LP obligationDocument;
    LP orderSaleObligationCanBeUsed;
    LP orderSaleUseObligation;

    public LP xorActionArticle;
    LP inAction;
    LP orderSalePayNoObligation;
    public LP orderArticleSaleSumCoeff;
    public LP clientInitialSum;
    LP clientSum;
    LP incStore;
    LP incStoreName;
    LP outStore;

    LP orderContragent;
    LP orderSupplier;

    LP articleFreeOrderQuantity;

    LP articleToGroupName;

    LP articleSupplier;
    LP articleStoreSupplier;
    LP articleStorePeriod;
    LP articleStoreMin;
    LP articleFullStoreDemand;

    LP customerCheckRetailPhone, customerCheckRetailBorn, customerCheckRetailAddress;

    LP nameContragent, phoneContragent, bornContragent, addressContragent, initialSumContragent;
    LP nameContragentImpl, phoneContragentImpl, bornContragentImpl, addressContragentImpl, initialSumContragentImpl;

//    LP logClientInitialSum;

    LP documentLogisticsSupplied, documentLogisticsRequired, documentLogisticsRecommended;
    LP currentNDSDate, currentNDSDoc, currentNDS, NDS;
    public LP articleQuantity, prevPrice, revalBalance;
    LP articleOrderQuantity;
    LP orderSaleDiscountSum, orderSalePay, orderSaleDiff;
    LP orderSaleToDo, orderSaleToDoSum;
    public LP returnArticleSalePay;
    LP returnSaleDiscount, returnSalePay;
    public LP orderArticleSaleDiscount;
    public LP shopPrice;
    LP priceStore, inDocumentPrice;
    LP isRevalued, isNewPrice, documentRevalued;
    LP balanceFormatFreeQuantity;
    LP currentShopPriceDate;
    LP currentShopPriceDoc;
    LP currentShopPrice;
    LP currentRRP;
    LP returnInnerFreeQuantity;
    LP sumReturnedQuantity;
    LP sumReturnedQuantityFree;

    public LP documentFreeQuantity, documentInnerFreeQuantity, returnFreeQuantity, innerQuantity, returnInnerCommitQuantity, returnInnerQuantity;
    public LP outerOrderQuantity, outerCommitedQuantity, articleBalanceCheck, articleBalanceCheckDB, innerBalanceCheck, innerBalanceCheckDB, balanceSklCommitedQuantity;
    public LP articleInnerQuantity;

    AbstractGroup documentGroup;
    AbstractGroup priceGroup;
    AbstractGroup moveGroup;
    AbstractGroup allGroup;
    AbstractGroup logisticsGroup;
    AbstractGroup documentMoveGroup;
    AbstractGroup documentPriceGroup, documentAggrPriceGroup;
    AbstractGroup documentLogisticsGroup;
    public AbstractGroup cashRegGroup;
    public AbstractGroup cashRegOperGroup, cashRegAdminGroup;
    AbstractGroup couponGroup;

    protected void initGroups() {
        allGroup = new AbstractGroup("Все");
        allGroup.createContainer = false;
        allGroup.add(baseGroup);

        documentGroup = new AbstractGroup("Параметры документа");
        allGroup.add(documentGroup);

        moveGroup = new AbstractGroup("Движение товаров");
        allGroup.add(moveGroup);

        documentMoveGroup = new AbstractGroup("Текущие параметры документа");
        documentGroup.add(documentMoveGroup);

        priceGroup = new AbstractGroup("Ценовые параметры");
        allGroup.add(priceGroup);

        documentPriceGroup = new AbstractGroup("Ценовые параметры документа");
        documentPriceGroup.createContainer = false;
        documentGroup.add(documentPriceGroup);

        documentAggrPriceGroup = new AbstractGroup("Агрегированные ценовые параметры документа");
        documentAggrPriceGroup.createContainer = false;
        documentPriceGroup.add(documentAggrPriceGroup);

        logisticsGroup = new AbstractGroup("Логистические параметры");
        allGroup.add(logisticsGroup);

        documentLogisticsGroup = new AbstractGroup("Логистические параметры документа");
        documentGroup.add(documentLogisticsGroup);

        cashRegGroup = new AbstractGroup("Операции с ФР");
        cashRegGroup.createContainer = false;
        baseGroup.add(cashRegGroup);

        cashRegOperGroup = new AbstractGroup("Оперативные операции с ФР");
        cashRegGroup.add(cashRegOperGroup);

        cashRegAdminGroup = new AbstractGroup("Административные операции с ФР");
        cashRegGroup.add(cashRegAdminGroup);

        couponGroup = new AbstractGroup("Параметры документа");
    }

    protected void initTables() {
        tableFactory.include("article", article);
        tableFactory.include("orders", order);
        tableFactory.include("store", store);
        tableFactory.include("localsupplier", localSupplier);
        tableFactory.include("importsupplier", importSupplier);
        tableFactory.include("customerwhole", customerWhole);
        tableFactory.include("customerretail", customerCheckRetail);
        tableFactory.include("articlestore", article, store);
        tableFactory.include("articleorder", article, order);
        tableFactory.include("rates", DateClass.instance);
        tableFactory.include("intervals", DoubleClass.instance);
    }

    protected void initIndexes() {
    }

    PanelExternalScreen panelScreen;

    @Override
    protected void initExternalScreens() {
        panelScreen = new PanelExternalScreen();
        addExternalScreen(panelScreen);
    }

    private Integer getPanelComPort(int compId) {
        try {
            Integer result = null;
            DataSession session = createSession();

            Query<String, Object> q = new Query<String, Object>(Collections.singleton("key"));
            q.properties.put("comport", panelScreenComPort.getExpr(session.modifier, q.mapKeys.get("key")));
            q.and(q.mapKeys.get("key").compare(new DataObject(compId, computer), Compare.EQUALS));

            Collection<Map<Object, Object>> values = q.execute(session).values();
            if (values.size() != 0) {
                result = (Integer) values.iterator().next().get("comport");
            }

            session.close();

            return result == null ? -1 : result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        if (panelScreen.getID() == screenID) {
            return new PanelExternalScreenParameters(getPanelComPort(computerId));
        }
        return null;
    }

    private NavigatorElement saleRetailCashRegisterElement;
    private FormEntity commitSaleForm;
    private FormEntity saleCheckCertForm;
    private FormEntity cachRegManagementForm;
    private FormEntity returnSaleCheckRetailArticleForm;
    public CommitSaleCheckRetailFormEntity commitSaleBrowseForm;
    public SaleCheckCertFormEntity saleCheckCertBrowseForm;
    public ReturnSaleCheckRetailFormEntity returnSaleCheckRetailBrowse;

    protected void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement print = new NavigatorElement(baseElement, 4000, "Печатные формы");
        FormEntity incomePrice = addFormEntity(new IncomePriceFormEntity(print, 4100));
        FormEntity revalueAct = addFormEntity(new RevalueActFormEntity(print, 4200));
        FormEntity pricers = addFormEntity(new PricersFormEntity(print, 4300));

        NavigatorElement delivery = new NavigatorElement(baseElement, 1000, "Управление закупками");
        addFormEntity(new SupplierArticleFormEntity(delivery, 1050));
        FormEntity deliveryShopLocal = addFormEntity(new DeliveryShopLocalFormEntity(delivery, true, 1100));
        FormEntity deliveryShopLocalBrowse = addFormEntity(new DeliveryShopLocalFormEntity(deliveryShopLocal, false, 1125));
        FormEntity deliveryWarehouseLocal = addFormEntity(new DeliveryWarehouseLocalFormEntity(delivery, true, 1130));
        FormEntity deliveryWarehouseLocalBrowse = addFormEntity(new DeliveryWarehouseLocalFormEntity(deliveryWarehouseLocal, false, 1135));
        FormEntity deliveryImport = addFormEntity(new DeliveryImportFormEntity(delivery, true, 1150));
        FormEntity deliveryImportBrowse = addFormEntity(new DeliveryImportFormEntity(deliveryImport, false, 1175));
        FormEntity returnDelivery = addFormEntity(new ReturnDeliveryLocalFormEntity(delivery, 1190, true));
        addFormEntity(new ReturnDeliveryLocalFormEntity(returnDelivery, 1195, false));

        NavigatorElement sale = new NavigatorElement(baseElement, 1200, "Управление продажами");
        NavigatorElement saleRetailElement = new NavigatorElement(sale, 1250, "Управление розничными продажами");
        saleRetailCashRegisterElement = new NavigatorElement(saleRetailElement, 1300, "Касса");
        commitSaleForm = addFormEntity(new CommitSaleCheckRetailFormEntity(saleRetailCashRegisterElement, 1310, true));
        commitSaleBrowseForm = addFormEntity(new CommitSaleCheckRetailFormEntity(commitSaleForm, 1320, false));
        saleCheckCertForm = addFormEntity(new SaleCheckCertFormEntity(saleRetailCashRegisterElement, 1325, true));
        saleCheckCertBrowseForm = addFormEntity(new SaleCheckCertFormEntity(saleCheckCertForm, 1335, false));
        returnSaleCheckRetailArticleForm = addFormEntity(new ReturnSaleCheckRetailFormEntity(saleRetailCashRegisterElement, true, 1345));
        returnSaleCheckRetailBrowse = addFormEntity(new ReturnSaleCheckRetailFormEntity(returnSaleCheckRetailArticleForm, false, 1355));
        cachRegManagementForm = addFormEntity(cashRegController.createCashRegManagementFormEntity(saleRetailCashRegisterElement, 1365));
        NavigatorElement saleRetailInvoice = new NavigatorElement(saleRetailElement, 1400, "Безналичный расчет");
        FormEntity saleRetailInvoiceForm = addFormEntity(new OrderSaleInvoiceRetailFormEntity(saleRetailInvoice, 1410, true));
        addFormEntity(new OrderSaleInvoiceRetailFormEntity(saleRetailInvoiceForm, 1420, false));
        FormEntity saleInvoiceCert = addFormEntity(new SaleInvoiceCertFormEntity(saleRetailInvoice, 1440, true));
        addFormEntity(new SaleInvoiceCertFormEntity(saleInvoiceCert, 1445, false));
        FormEntity returnSaleInvoiceRetailArticle = addFormEntity(new ReturnSaleInvoiceRetailFormEntity(saleRetailInvoice, true, 1477));
        addFormEntity(new ReturnSaleInvoiceRetailFormEntity(returnSaleInvoiceRetailArticle, false, 1487));
        NavigatorElement saleWhole = new NavigatorElement(sale, 1500, "Управление оптовыми продажами");
        FormEntity saleWholeForm = addFormEntity(new SaleWholeFormEntity(saleWhole, 1520, true));
        addFormEntity(new SaleWholeFormEntity(saleWholeForm, 1540, false));
        FormEntity returnSaleWholeArticle = addFormEntity(new ReturnSaleWholeFormEntity(saleWhole, 1560, true));
        addFormEntity(new ReturnSaleWholeFormEntity(returnSaleWholeArticle, 1580, false));

        NavigatorElement distribute = new NavigatorElement(baseElement, 3000, "Управление распределением");
        FormEntity distributeShopForm = addFormEntity(new DistributeShopFormEntity(distribute, 3100, true));
        FormEntity distributeShopBrowseForm = addFormEntity(new DistributeShopFormEntity(distributeShopForm, 3200, false));
        FormEntity distributeWarehouseForm = addFormEntity(new DistributeWarehouseFormEntity(distribute, 3110, true));
        FormEntity distributeWarehouseBrowseForm = addFormEntity(new DistributeWarehouseFormEntity(distributeWarehouseForm, 3210, false));

        NavigatorElement price = new NavigatorElement(baseElement, 2400, "Управление ценообразованием");
        FormEntity documentRevalue = addFormEntity(new DocumentRevalueFormEntity(price, true, 2650));
        addFormEntity(new DocumentRevalueFormEntity(documentRevalue, false, 2750));
        addFormEntity(new FormatArticleFormEntity(price, 2200));
        addFormEntity(new GlobalFormEntity(price, 5200));

        NavigatorElement tax = new NavigatorElement(baseElement, 5400, "Управление налогами");
        FormEntity nds = addFormEntity(new DocumentNDSFormEntity(tax, true, 5800));
        addFormEntity(new DocumentNDSFormEntity(nds, false, 5850));

        NavigatorElement actions = new NavigatorElement(baseElement, 7400, "Управление акциями");
        FormEntity saleAction = addFormEntity(new ActionFormEntity(actions, 7800));
        FormEntity couponInterval = addFormEntity(new CouponIntervalFormEntity(actions, 7825));
        FormEntity couponArticle = addFormEntity(new CouponArticleFormEntity(actions, 7850));

        NavigatorElement balance = new NavigatorElement(baseElement, 6500, "Управление хранением");
        FormEntity balanceCheck = addFormEntity(new BalanceCheckFormEntity(balance, 6350, true));
        addFormEntity(new BalanceCheckFormEntity(balanceCheck, 6375, false));

        NavigatorElement store = new NavigatorElement(baseElement, 2000, "Сводная информация");
        addFormEntity(new StoreArticleFormEntity(store, 2100));

//        FormEntity logClient = addFormEntity(new LogClientFormEntity(actions, 9850));

        commitWholeShopInc.addRelevant(incomePrice);
        documentShopPrice.addRelevant(revalueAct);
        documentShopPrice.addRelevant(pricers);
    }

    public static Font FONT_SMALL_BOLD = new Font("Tahoma", Font.BOLD, 12);
    public static Font FONT_SMALL_PLAIN = new Font("Tahoma", Font.PLAIN, 12);
    public static Font FONT_MEDIUM_BOLD = new Font("Tahoma", Font.BOLD, 14);
    public static Font FONT_LARGE_BOLD = new Font("Tahoma", Font.BOLD, 24);
    public static Font FONT_HUGE_BOLD = new Font("Tahoma", Font.BOLD, 28);

    private class GlobalFormEntity extends FormEntity {
        protected GlobalFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Глобальные параметры");
            addPropertyDraw(allGroup, true);
        }
    }

    private class BarcodeFormEntity extends FormEntity<VEDBusinessLogics> {

        ObjectEntity objBarcode;

        protected boolean isBarcodeFocusable() {
            return true;
        }

        protected Font getDefaultFont() {
            return null;
        }

        private BarcodeFormEntity(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseGroup, true);
            objBarcode.groupTo.initClassView = ClassViewType.PANEL;
            objBarcode.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;

            objBarcode.resetOnApply = true;

            addPropertyDraw(reverseBarcode);

//            addAutoAction(objBarcode, addPropertyObject(barcodeAction, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            if (getDefaultFont() != null)
                design.setFont(getDefaultFont());

            PropertyDrawView barcodeView = design.get(getPropertyDraw(objectValue, objBarcode));
            
            design.get(getPropertyDraw(reverseBarcode)).setContainer(design.getPanelContainer(design.get(objBarcode.groupTo)));
            design.addIntersection(barcodeView, design.get(getPropertyDraw(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.get(getPropertyDraw(reverseBarcode)), design.get(getPropertyDraw(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            if (getPropertyDraw(documentBarcodePriceOv) != null) {
                design.addIntersection(design.get(getPropertyDraw(barcodeObjectName)), design.get(getPropertyDraw(documentBarcodePriceOv)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }

            design.setFont(barcodeView, FONT_SMALL_BOLD);
            design.setFont(reverseBarcode, FONT_SMALL_BOLD);
            design.setFont(barcodeObjectName, FONT_LARGE_BOLD);
            design.setFont(documentBarcodePriceOv, FONT_LARGE_BOLD);
            design.setBackground(barcodeObjectName, new Color(240, 240, 240));

            design.setEditKey(barcodeView, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
            design.setEditKey(reverseBarcode, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

            if (!isBarcodeFocusable()) {
                design.setFocusable(reverseBarcode, false);
                design.setFocusable(false, objBarcode.groupTo);
                design.setFocusable(barcodeView, false);
            }

            return design;
        }
    }

    private class DocumentFormEntity extends BarcodeFormEntity {

        public final ObjectEntity objDoc;

        protected boolean toAdd = false;

        protected static final boolean fixFilters = false;
        protected static final boolean noOuters = true;

        protected Object[] getDocumentProps() {
            return new Object[]{baseGroup, true, documentGroup, true};
        }

        protected boolean isDocumentFocusable() {
            return true;
        }

        protected DocumentFormEntity(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, (toAdd ? documentClass.caption : "Документы"));

            this.toAdd = toAdd;

            objDoc = addSingleGroupObject(documentClass, "Документ", getDocumentProps());


            if (toAdd) {
                objDoc.groupTo.initClassView = ClassViewType.PANEL;
                objDoc.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
                objDoc.addOnTransaction = true;
            } else {
                addPropertyDraw(orderUserName, objDoc);
                if (!isReadOnly())
                    addObjectActions(this, objDoc);
            }

            addAutoAction(objBarcode, addPropertyObject(barcodeAction2, objDoc, objBarcode));
            addAutoAction(objBarcode, addPropertyObject(seekAction, objBarcode));
            addAutoAction(objBarcode, addPropertyObject(barcodeNotFoundMessage, objBarcode));

            if (hasExternalScreen()) {
                addPropertyDraw(documentBarcodePriceOv, objDoc, objBarcode).setToDraw(objBarcode.groupTo);
                //getPropertyDraw(documentBarcodePriceOv).setToDraw(objBarcode.groupTo);
            }
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            if (toAdd) {
                design.setFont(FONT_MEDIUM_BOLD, objDoc.groupTo);

                // устанавливаем дизайн
                design.setFont(documentPriceGroup, FONT_HUGE_BOLD, objDoc.groupTo);
                design.setBackground(documentAggrPriceGroup, new Color(240, 240, 240), objDoc.groupTo);

                // ставим Label сверху
                design.setPanelLabelAbove(documentPriceGroup, true, objDoc.groupTo);

                // привязываем функциональные кнопки
                design.setEditKey(nameContragent, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), objDoc.groupTo);
                design.setEditKey(orderSalePayCard, KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
                design.setEditKey(orderSalePayCash, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));


                if (!isDocumentFocusable()) {
                    design.setEnabled(false, objDoc.groupTo);
                } else {
                    design.setEnabled(documentAggrPriceGroup, false, objDoc.groupTo);
                }

                // так конечно делать неправильно, но DocumentFormEntity - это первый общий класс у продажи сертификатов и кассы
                PropertyDrawEntity payView = getPropertyDraw(orderSalePay);

                if (payView != null) {
                    // делаем, чтобы суммы были внизу и как можно правее
                    ContainerView docSumsContainer = design.get(payView).getContainer();
                    docSumsContainer.setContainer(design.getMainContainer());
//                    docSumsContainer.constraints.directions = new SimplexComponentDirections(0.1,-0.1,0,0.1);
                    docSumsContainer.constraints.order = 6;

                    PropertyDrawEntity payCash = getPropertyDraw(orderSalePayCash);
                    PropertyDrawEntity payCard = getPropertyDraw(orderSalePayCard);
                    PropertyDrawEntity toDo = getPropertyDraw(orderSaleToDo);
                    PropertyDrawEntity toDoSum = getPropertyDraw(orderSaleToDoSum);

                    if (payCash != null || payCard != null || toDo != null || toDoSum != null) {

                        ContainerView payContainer = design.addContainer("Платежные средства");
                        payContainer.setContainer(design.getMainContainer());
                        payContainer.constraints.directions = new SimplexComponentDirections(0.1, -0.1, 0, 0.1);
                        payContainer.constraints.order = 6;

                        if (payCash != null) design.get(payCash).setContainer(payContainer);
                        if (payCard != null) design.get(payCard).setContainer(payContainer);
                        if (toDo != null) {
                            design.get(toDo).setContainer(payContainer);
                            design.get(toDo).design.background = Color.yellow;
                        }
                        if (toDoSum != null) {
                            design.get(toDoSum).setContainer(payContainer);
                            design.get(toDoSum).design.background = Color.yellow;
                        }

                        design.addIntersection(docSumsContainer, payContainer, DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                    }
                }

                if (hasExternalScreen()) {
                    PropertyDrawEntity barcodeNavigator = getPropertyDraw(barcodeObjectName);
                    if (barcodeNavigator != null) {
                        design.get(barcodeNavigator).externalScreen = panelScreen;
                        design.get(barcodeNavigator).externalScreenConstraints.order = 1;
                    }

                    PropertyDrawEntity orderSaleNavigator = getPropertyDraw(orderSaleToDo);
                    if (orderSaleNavigator != null) {
                        design.get(orderSaleNavigator).externalScreen = panelScreen;
                        design.get(orderSaleNavigator).externalScreenConstraints.order = 3;
                    }

                    PropertyDrawEntity orderSaleSumNavigator = getPropertyDraw(orderSaleToDoSum);
                    if (orderSaleSumNavigator != null) {
                        design.get(orderSaleSumNavigator).externalScreen = panelScreen;
                        design.get(orderSaleSumNavigator).externalScreenConstraints.order = 4;
                    }

                    PropertyDrawEntity priceNavigator = getPropertyDraw(documentBarcodePriceOv);
                    if (priceNavigator != null) {
                        design.get(priceNavigator).externalScreen = panelScreen;
                        design.get(priceNavigator).externalScreenConstraints.order = 2;
                    }
                }
            }

            return design;
        }

        protected boolean hasExternalScreen() {
            return false;
        }
    }

    private abstract class ArticleFormEntity extends DocumentFormEntity {
        public final ObjectEntity objArt;

        protected abstract PropertyObjectEntity getCommitedQuantity();

        protected Object[] getArticleProps() {
            return new Object[]{baseGroup, true};
        }

        protected Object[] getDocumentArticleProps() {
            return new Object[]{baseGroup, true, documentGroup, true};
        }

        protected ArticleFormEntity(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, ID, documentClass, toAdd);

            objArt = addSingleGroupObject(article, "Товар", getArticleProps());
            addPropertyDraw(objDoc, objArt, getDocumentArticleProps());

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    getDocumentArticleFilter(),
                    "Документ",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), !toAdd || filled);
            fillExtraFilters(filterGroup, toAdd && !filled);
            addRegularFilterGroup(filterGroup);

//            addHintsNoUpdate(properties, moveGroup);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();
            design.get(objArt.groupTo).grid.constraints.fillVertical = 3;

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F2, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }

        protected ArticleFormEntity(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            this(parent, ID, documentClass, toAdd, false);
        }

        protected abstract FilterEntity getDocumentArticleFilter();

        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
        }

        // такое дебильное множественное наследование
        public void fillExtraLogisticsFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(getPropertyObject(documentLogisticsSupplied)),
                    "Поставляется",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(getPropertyObject(documentLogisticsRequired)),
                    "Необходимо",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
        }
    }

    // для те которые не различают заказано и выполнено
    private abstract class ArticleNoCheckFormEntity extends ArticleFormEntity {

        protected FilterEntity getDocumentArticleFilter() {
            return new NotNullFilterEntity(getCommitedQuantity());
        }

        protected ArticleNoCheckFormEntity(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, documentClass, toAdd);
        }

        protected ArticleNoCheckFormEntity(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, ID, documentClass, toAdd, filled);
        }
    }

    private abstract class InnerFormEntity extends ArticleNoCheckFormEntity {

        protected PropertyObjectEntity getCommitedQuantity() {
            return addPropertyObject(articleInnerQuantity, objDoc, objArt);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            if (!fixFilters)
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        new NotNullFilterEntity(addPropertyObject(documentFreeQuantity, objDoc, objArt)),
                        "Дост. кол-во",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), toAdd);
        }

        protected InnerFormEntity(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, ID, documentClass, toAdd, filled);

            if (fixFilters)
                addFixedFilter(new OrFilterEntity(getDocumentArticleFilter(), new NotNullFilterEntity(addPropertyObject(documentFreeQuantity, objDoc, objArt))));
        }
    }

    private abstract class OuterFormEntity extends ArticleFormEntity {

        protected PropertyObjectEntity getCommitedQuantity() {
            return getPropertyObject(outerCommitedQuantity);
        }

        protected PropertyObjectEntity getOrderQuantity() {
            return getPropertyObject(outerOrderQuantity);
        }

        protected FilterEntity getDocumentArticleFilter() {
            return new OrFilterEntity(new NotNullFilterEntity(getOrderQuantity()), new NotNullFilterEntity(getCommitedQuantity()));
        }

        protected OuterFormEntity(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass) {
            super(parent, ID, documentClass, toAdd);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            fillExtraLogisticsFilters(filterGroup, toAdd);
        }
    }

    private class DeliveryShopLocalFormEntity extends OuterFormEntity {
        public DeliveryShopLocalFormEntity(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, orderDeliveryShopLocal);
        }
    }

    private class DeliveryWarehouseLocalFormEntity extends OuterFormEntity {
        public DeliveryWarehouseLocalFormEntity(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, orderDeliveryWarehouseLocal);
        }
    }

    private class DeliveryImportFormEntity extends OuterFormEntity {
        public DeliveryImportFormEntity(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, orderDeliveryImport);
        }
    }

    private class ArticleOuterFormEntity extends InnerFormEntity {
        ObjectEntity objOuter;

        protected ArticleOuterFormEntity(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, CustomClass commitClass, boolean filled) {
            super(parent, ID, documentClass, toAdd, filled);

            if (!noOuters) {
                objOuter = addSingleGroupObject(commitClass, "Партия", baseGroup, true);
                addPropertyDraw(objOuter, objDoc, baseGroup, true, documentGroup, true);
                addPropertyDraw(objOuter, objDoc, objArt, baseGroup, true, documentGroup, true);
                addPropertyDraw(objOuter, objArt, baseGroup, true);

                NotNullFilterEntity documentFilter = new NotNullFilterEntity(getPropertyObject(innerQuantity));
                NotNullFilterEntity documentFreeFilter = new NotNullFilterEntity(getPropertyObject(documentInnerFreeQuantity));
                if (fixFilters)
                    addFixedFilter(new OrFilterEntity(documentFilter, documentFreeFilter));
                RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        documentFilter,
                        "Документ",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), !toAdd || filled);
                if (!fixFilters)
                    filterGroup.addFilter(new RegularFilterEntity(genID(),
                            documentFreeFilter,
                            "Дост. кол-во",
                            KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), toAdd && !filled);
                addRegularFilterGroup(filterGroup);
            }
        }
    }

    private class ReturnDeliveryLocalFormEntity extends ArticleOuterFormEntity {
        public ReturnDeliveryLocalFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, orderReturnDeliveryLocal, toAdd, commitDeliveryLocal, false);
        }
    }

    private class ArticleInnerFormEntity extends ArticleOuterFormEntity {

        protected ArticleInnerFormEntity(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, boolean filled) {
            super(parent, ID, documentClass, toAdd, commitDelivery, filled);
        }
    }

    private class DocumentInnerFormEntity extends ArticleInnerFormEntity {

        protected DocumentInnerFormEntity(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, boolean filled) {
            super(parent, ID, toAdd, documentClass, filled);
        }
    }

    private class SaleWholeFormEntity extends DocumentInnerFormEntity {
        public SaleWholeFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, orderSaleWhole, false);
        }
    }

    private abstract class SaleRetailFormEntity extends DocumentInnerFormEntity {

        public SaleRetailFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            this(parent, ID, orderSaleArticleRetail, toAdd);
        }

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{nameContragentImpl, phoneContragentImpl, bornContragentImpl, addressContragentImpl, initialSumContragentImpl, orderClientSum,
                    orderSalePay, orderSaleDiscountSum, orderSalePayNoObligation, orderSalePayCash, orderSalePayCard, orderSaleToDo, orderSaleToDoSum};
        }

        @Override
        protected Object[] getArticleProps() {
            return new Object[]{};
        }

        @Override
        protected Object[] getDocumentArticleProps() {
            return new Object[]{};
        }

        @Override
        protected boolean isBarcodeFocusable() {
            return false;
        }

        @Override
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        protected SaleRetailFormEntity(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, toAdd, documentClass, true);

            if (!toAdd)
                addPropertyDraw(date, objDoc);

            // чтобы в порядке нужном
            addPropertyDraw(changeQuantityOrder, objDoc, objArt);
            addPropertyDraw(barcode, objArt);
            addPropertyDraw(name, objArt);
            addPropertyDraw(articleQuantity, objDoc, objArt); // для timeChanges иначе можно было бы articleQuantity
            addPropertyDraw(documentFreeQuantity, objDoc, objArt);
            addPropertyDraw(orderSalePrice, objDoc, objArt);
            addPropertyDraw(orderArticleSaleDiscount, objDoc, objArt);
            addPropertyDraw(orderArticleSaleSumWithDiscount, objDoc, objArt);
            //addPropertyDraw(documentBarcodePriceOv, objDoc, objBarcode);
            if (!toAdd) {
                addPropertyDraw(orderArticleSaleSumCoeff, objDoc, objArt);
            }
            //getPropertyDraw(documentBarcodePriceOv).setToDraw(objBarcode.groupTo);

            objArt.groupTo.banClassView |= ClassViewType.HIDE | ClassViewType.PANEL;

//            addFixedFilter(new OrFilterEntity(new CompareFilterEntity(addPropertyObject(outStore, objDoc), Compare.EQUALS, shopImplement),
//                                                    new NotFilterEntity(new NotNullFilterEntity(shopImplement))));

            PropertyObjectEntity shopImplement = addPropertyObject(computerShop, CurrentComputerEntity.instance);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outStore, objDoc), Compare.EQUALS, shopImplement));

            addFixedOrder(addPropertyObject(changeQuantityTime, objDoc, objArt), false);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();
            design.setEnabled(orderClientSum, false);

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);

            design.setEnabled(allGroup, false, objArt.groupTo);
            design.setEnabled(articleQuantity, true, objArt.groupTo);

            design.get(objArt.groupTo).grid.defaultComponent = true;

            ObjectView objArtView = design.get(objArt);
            objArtView.classChooser.show = false;

            return design;
        }
    }

    public class CommitSaleCheckRetailFormEntity extends SaleRetailFormEntity {

        private ObjectEntity objObligation;
        private ObjectEntity objCoupon;
        private ObjectEntity objIssue;

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        private CommitSaleCheckRetailFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, commitSaleCheckArticleRetail, toAdd);

            objDoc.caption = "Чек";

            objObligation = addSingleGroupObject(obligation, "Оплачено купонами/ сертификатами", baseGroup, true);
            addPropertyDraw(objDoc, objObligation, baseGroup, true, orderSaleUseObligation);
            objObligation.groupTo.banClassView |= ClassViewType.HIDE | ClassViewType.PANEL;
//            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(obligationDocument, objObligation))));
//            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleUseObligation, objDoc, objObligation)));

            objCoupon = addSingleGroupObject(coupon, "Выдано купонов", baseGroup, true);
            addPropertyDraw(objDoc, objCoupon, baseGroup, true, issueObligation);
            objCoupon.groupTo.banClassView |= ClassViewType.HIDE | ClassViewType.PANEL;
//            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(obligationDocument, objObligation))));
//            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(issueObligation, objDoc, objCoupon)));

            if (toAdd) {
                objIssue = addSingleGroupObject(DoubleClass.instance, "Выдать купоны");
                addPropertyDraw(couponToIssueQuantity, objDoc, objIssue);
                objIssue.groupTo.banClassView |= ClassViewType.HIDE | ClassViewType.PANEL;
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(couponToIssueConstraint, objDoc, objIssue)));

                addPropertyDraw(cashRegOperGroup, true);
            } else {
                addPropertyDraw(checkRetailExported, objDoc);
            }
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            design.get(objArt.groupTo).grid.minRowCount = 6;

            if (toAdd) {
                design.get(objIssue.groupTo).grid.constraints.fillHorizontal /= 3;
                design.get(objIssue.groupTo).grid.minRowCount = 2;
                design.get(objIssue.groupTo).grid.showFilter = false;
                design.get(objIssue.groupTo).grid.autoHide = true;
                design.addIntersection(design.getGroupObjectContainer(objIssue.groupTo), design.getGroupObjectContainer(objCoupon.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objIssue.groupTo), design.getGroupObjectContainer(objObligation.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }

            design.get(objCoupon.groupTo).grid.minRowCount = 2;
            design.get(objObligation.groupTo).grid.minRowCount = 2;
            design.get(objCoupon.groupTo).grid.showFilter = false;
            design.get(objObligation.groupTo).grid.showFilter = false;
            design.get(objCoupon.groupTo).grid.autoHide = true;
            design.addIntersection(design.getGroupObjectContainer(objCoupon.groupTo), design.getGroupObjectContainer(objObligation.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ObjectView objCouponView = design.get(objCoupon);
            objCouponView.classChooser.show = false;

            ObjectView objObligationView = design.get(objObligation);
            objObligationView.classChooser.show = false;
            
            return design;
        }

        @Override
        public boolean hasClientApply() {
            return toAdd;
        }

        @Override
        public ClientAction getClientApply(FormInstance<VEDBusinessLogics> formInstance) {
            if (toAdd) {

                ObjectInstance doc = formInstance.instanceFactory.getInstance(objDoc);
                ObjectInstance art = formInstance.instanceFactory.getInstance(objArt);

                return cashRegController.getCashRegApplyActions(formInstance, 1,
                        BaseUtils.toSetElements(doc.groupTo, art.groupTo), BaseUtils.toSetElements(art.groupTo),
                        getPropertyDraw(orderSalePrice, objArt), getPropertyDraw(articleQuantity, objArt),
                        getPropertyDraw(name, objArt), getPropertyDraw(orderArticleSaleSumWithDiscount, objArt),
                        getPropertyDraw(orderSalePayNoObligation, objDoc),
                        getPropertyDraw(orderSalePayCard, objDoc), getPropertyDraw(orderSalePayCash, objDoc));
            } else
                return super.getClientApply(formInstance);
        }

        @Override
        public String checkClientApply(Object result) {

            String check = cashRegController.checkCashRegApplyActions(result);
            if (check != null) return check;

            return super.checkClientApply(result);
        }

        @Override
        protected boolean hasExternalScreen() {
            return true;
        }
    }

    private class OrderSaleInvoiceRetailFormEntity extends SaleRetailFormEntity {

        private OrderSaleInvoiceRetailFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, orderSaleInvoiceArticleRetail, toAdd);
        }

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{nameContragent, phoneContragent, bornContragent, addressContragent, initialSumContragent, orderClientSum,
                    orderSalePay, orderSaleDiscountSum, orderSalePayNoObligation, orderSalePayCash, orderSalePayCard, orderSaleToDo, invoiceNumber, invoiceSeries};
        }
    }

    private class DistributeFormEntity extends DocumentInnerFormEntity {
        public DistributeFormEntity(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass) {
            super(parent, ID, toAdd, documentClass, false);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            fillExtraLogisticsFilters(filterGroup, toAdd);
        }
    }

    private class DistributeShopFormEntity extends DocumentInnerFormEntity {
        public DistributeShopFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, orderDistributeShop, false);
        }
    }

    private class DistributeWarehouseFormEntity extends DocumentInnerFormEntity {
        public DistributeWarehouseFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, orderDistributeWarehouse, false);
        }
    }

    private class BalanceCheckFormEntity extends DocumentInnerFormEntity {
        public BalanceCheckFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, balanceCheck, false);
        }
    }

    private class ReturnSaleFormEntity extends DocumentFormEntity {

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{returnSaleDiscount, returnSalePay};
        }

        @Override
        protected boolean isBarcodeFocusable() {
            return false;
        }

        @Override
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        public final ObjectEntity objInner;
        public final ObjectEntity objArt;
        ObjectEntity objOuter;

        protected String getReturnCaption() {
            return "Документ к возврату";
        }

        protected ReturnSaleFormEntity(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, CustomClass commitClass) {
            super(parent, ID, documentClass, toAdd);

            objInner = addSingleGroupObject(commitClass, getReturnCaption(), baseGroup, true);

            addPropertyDraw(objInner, objDoc, baseGroup, true, documentGroup, true);

            NotNullFilterEntity documentFilter = new NotNullFilterEntity(getPropertyObject(sumReturnedQuantity));
            NotNullFilterEntity documentFreeFilter = new NotNullFilterEntity(getPropertyObject(sumReturnedQuantityFree));
            if (fixFilters)
                addFixedFilter(new OrFilterEntity(documentFilter, documentFreeFilter));
            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    documentFilter,
                    "Документ",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, InputEvent.SHIFT_DOWN_MASK)), !toAdd);
            if (!fixFilters)
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        documentFreeFilter,
                        "Дост. кол-во",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_DOWN_MASK)), toAdd);
            addRegularFilterGroup(filterGroup);

            objArt = addSingleGroupObject(article, "Товар");

            addPropertyDraw(changeQuantityOrder, objInner, objArt);
            addPropertyDraw(barcode, objArt);
            addPropertyDraw(name, objArt);
            addPropertyDraw(objInner, objDoc, objArt, baseGroup, true, documentGroup, true);
            addPropertyDraw(orderSalePrice, objInner, objArt);
            addPropertyDraw(orderArticleSaleDiscount, objInner, objArt);
            addPropertyDraw(orderArticleSaleSumWithDiscount, objInner, objArt);

            PropertyObjectEntity returnInnerImplement = getPropertyObject(returnInnerQuantity);

            NotNullFilterEntity articleFilter = new NotNullFilterEntity(returnInnerImplement);
            NotNullFilterEntity articleFreeFilter = new NotNullFilterEntity(getPropertyObject(returnFreeQuantity));
            if (fixFilters)
                addFixedFilter(new OrFilterEntity(articleFilter, articleFreeFilter));
            RegularFilterGroupEntity articleFilterGroup = new RegularFilterGroupEntity(genID());
            articleFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    articleFilter,
                    "Документ",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), !toAdd);
            if (!fixFilters)
                articleFilterGroup.addFilter(new RegularFilterEntity(genID(),
                        articleFreeFilter,
                        "Дост. кол-во",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), toAdd);
            addRegularFilterGroup(articleFilterGroup);

//            addHintsNoUpdate(properties, moveGroup);
            addFixedOrder(addPropertyObject(changeQuantityTime, objInner, objArt), false);

            PropertyObjectEntity shopImplement = addPropertyObject(computerShop, CurrentComputerEntity.instance);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(incStore, objDoc), Compare.EQUALS, shopImplement));

            if (!noOuters) {
                objOuter = addSingleGroupObject(commitDelivery, "Партия", baseGroup, true);

                addPropertyDraw(objInner, objOuter, objDoc, baseGroup, true, documentGroup, true);
                addPropertyDraw(objInner, objOuter, objDoc, objArt, baseGroup, true, documentGroup, true);
                addPropertyDraw(objInner, objOuter, baseGroup, true);
                addPropertyDraw(objInner, objOuter, objArt, baseGroup, true);

                NotNullFilterEntity documentCommitFilter = new NotNullFilterEntity(getPropertyObject(returnInnerCommitQuantity));
                NotNullFilterEntity documentCommitFreeFilter = new NotNullFilterEntity(getPropertyObject(returnInnerFreeQuantity));
                if (fixFilters)
                    addFixedFilter(new OrFilterEntity(documentCommitFilter, documentCommitFreeFilter));
                RegularFilterGroupEntity filterOutGroup = new RegularFilterGroupEntity(genID());
                filterOutGroup.addFilter(new RegularFilterEntity(genID(),
                        documentCommitFilter,
                        "Документ",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !toAdd);
                if (!fixFilters)
                    filterOutGroup.addFilter(new RegularFilterEntity(genID(),
                            documentCommitFreeFilter,
                            "Макс. кол-во",
                            KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)), toAdd);
                addRegularFilterGroup(filterOutGroup);
            }

            addAutoAction(objBarcode, true,
                    addPropertyObject(barcodeAction3, objDoc, objInner, objBarcode),
                    addPropertyObject(seekAction, objBarcode),
                    addPropertyObject(barcodeNotFoundMessage, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            if (toAdd) {

                // делаем, чтобы суммы были внизу и как можно правее
                design.get(getPropertyDraw(returnSalePay)).getContainer().setContainer(design.getMainContainer());
//                design.get(getPropertyDraw(returnSalePay)).getContainer().constraints.directions = new SimplexComponentDirections(0.1,-0.1,0,0.1);
                design.get(getPropertyDraw(returnSalePay)).getContainer().constraints.order = 3;
            }

            if (!noOuters)
                design.addIntersection(design.getGroupObjectContainer(objInner.groupTo), design.getGroupObjectContainer(objOuter.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.setEnabled(objArt, false);
            design.setEnabled(returnInnerQuantity, true, objArt.groupTo);
            design.get(objArt.groupTo).grid.defaultComponent = true;


            return design;
        }
    }

    private class ReturnSaleWholeFormEntity extends ReturnSaleFormEntity {
        private ReturnSaleWholeFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, returnSaleWhole, commitSaleWhole);
        }
    }

    private class ReturnSaleInvoiceRetailFormEntity extends ReturnSaleFormEntity {
        private ReturnSaleInvoiceRetailFormEntity(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, returnSaleInvoiceRetail, commitSaleInvoiceArticleRetail);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);

            design.setReadOnly(true, objInner.groupTo);
            return design;
        }


        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{returnSaleDiscount, returnSalePay, nameContragent, invoiceNumber, invoiceSeries};
        }

    }

    public class ReturnSaleCheckRetailFormEntity extends ReturnSaleFormEntity {

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        @Override
        protected String getReturnCaption() {
            return "Номер чека";
        }

        private ReturnSaleCheckRetailFormEntity(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, returnSaleCheckRetail, commitSaleCheckArticleRetail);

            objDoc.caption = "Возвратный чек";

            if (toAdd) {
                addPropertyDraw(cashRegOperGroup, true);
            } else {
                addPropertyDraw(checkRetailExported, objDoc);
            }

            addPropertyDraw(documentBarcodePriceOv, objInner, objBarcode).setToDraw(objBarcode.groupTo);
            //addPropertyDraw(returnArticleSalePay, objArt);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            design.getGroupObjectContainer(objInner.groupTo).title = "Список чеков";
            design.get(getPropertyDraw(objectValue, objInner)).caption = "Номер чека";
            design.getGroupObjectContainer(objArt.groupTo).title = "Товарные позиции";

            PropertyDrawEntity barcodeNavigator = getPropertyDraw(barcodeObjectName);
            if (barcodeNavigator != null) {
                design.get(barcodeNavigator).externalScreen = panelScreen;
                design.get(barcodeNavigator).externalScreenConstraints.order = 1;
            }

            PropertyDrawEntity priceNavigator = getPropertyDraw(documentBarcodePriceOv);
            if (priceNavigator != null) {
                design.get(priceNavigator).externalScreen = panelScreen;
                design.get(priceNavigator).externalScreenConstraints.order = 2;
            }

            PropertyDrawEntity returnSaleSumNavigator = getPropertyDraw(returnSalePay);
            if (returnSaleSumNavigator != null) {
                design.get(returnSaleSumNavigator).externalScreen = panelScreen;
                design.get(returnSaleSumNavigator).externalScreenConstraints.order = 4;
            }

            design.setReadOnly(true, objInner.groupTo);

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }

        @Override
        public boolean hasClientApply() {
            return toAdd;
        }

        @Override
        public ClientAction getClientApply(FormInstance formInstance) {
            if (toAdd) {

                ObjectInstance doc = formInstance.instanceFactory.getInstance(objDoc);
                ObjectInstance inner = formInstance.instanceFactory.getInstance(objInner);
                ObjectInstance art = formInstance.instanceFactory.getInstance(objArt);

                return cashRegController.getCashRegApplyActions(formInstance, 2,
                        BaseUtils.toSetElements(doc.groupTo, inner.groupTo, art.groupTo), BaseUtils.toSetElements(inner.groupTo, art.groupTo),
                        getPropertyDraw(orderSalePrice, objArt), getPropertyDraw(returnInnerQuantity, objArt),
                        getPropertyDraw(name, objArt), getPropertyDraw(returnArticleSalePay, objArt),
                        getPropertyDraw(returnSalePay, objDoc),
                        null, null);
            } else
                return super.getClientApply(formInstance);
        }

        @Override
        public String checkClientApply(Object result) {

            String check = cashRegController.checkCashRegApplyActions(result);
            if (check != null) return check;

            return super.checkClientApply(result);
        }
    }

    private class SupplierArticleFormEntity extends FormEntity {
        protected SupplierArticleFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Ассортимент поставщиков");

            ObjectEntity objSupplier = addSingleGroupObject(supplier, "Поставщик", allGroup, true);
            addObjectActions(this, objSupplier);

            ObjectEntity objArt = addSingleGroupObject(article, "Товар", allGroup, true);
            addObjectActions(this, objArt);

            addPropertyDraw(objSupplier, objArt, allGroup, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleSupplier, objArt), Compare.EQUALS, objSupplier));
        }
    }

    private class StoreArticleFormEntity extends BarcodeFormEntity {
        private ObjectEntity objArt;

        protected StoreArticleFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по складу");

            ObjectEntity objStore = addSingleGroupObject(store, "Склад", allGroup, true);
            objArt = addSingleGroupObject(article, "Товар", allGroup, true);
            ObjectEntity objOuter = addSingleGroupObject(commitDelivery, "Партия", allGroup, true);

            addPropertyDraw(objStore, objArt, allGroup, true);
            addPropertyDraw(objStore, objOuter, allGroup, true);
            addPropertyDraw(objOuter, objArt, baseGroup, true);
            addPropertyDraw(objStore, objOuter, objArt, allGroup, true);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView form = super.createDefaultRichDesign();
            form.get(objArt.groupTo).grid.constraints.fillVertical = 3;
            return form;
        }
    }

    private class FormatArticleFormEntity extends BarcodeFormEntity {
        protected FormatArticleFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по форматам");

            ObjectEntity objFormat = addSingleGroupObject(format, "Формат", allGroup, true);
            addObjectActions(this, objFormat);

            ObjectEntity objArt = addSingleGroupObject(article, "Товар", allGroup, true);

            addPropertyDraw(objFormat, objArt, allGroup, true);

            addAutoAction(objBarcode, addPropertyObject(barcodeAction2, objFormat, objBarcode));
        }
    }

    private class DocumentRevalueFormEntity extends ArticleNoCheckFormEntity {

        protected PropertyObjectEntity getCommitedQuantity() {
            return getPropertyObject(documentRevalued);
        }

        protected DocumentRevalueFormEntity(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, documentRevalue, toAdd, true);
        }
    }

    private class DocumentNDSFormEntity extends ArticleNoCheckFormEntity {

        protected PropertyObjectEntity getCommitedQuantity() {
            return getPropertyObject(NDS);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(getPropertyObject(currentNDS))),
                    "Без НДС",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
        }

        protected DocumentNDSFormEntity(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, documentNDS, toAdd);

            addHintsNoUpdate(currentNDSDoc);
            addHintsNoUpdate(currentNDSDate);
        }
    }

    private class IncomePriceFormEntity extends FormEntity {

        protected IncomePriceFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Реестр цен", true);

            ObjectEntity objDoc = addSingleGroupObject(commitWholeShopInc, "Документ", baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup, true);

            addPropertyDraw(objDoc, objArt, articleQuantity, shopPrice);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(shopPrice)));

            addFAProp(documentPriceGroup, "Реестр цен", this, objDoc);
        }
    }

    private class RevalueActFormEntity extends FormEntity {

        protected RevalueActFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Акт переоценки", true);

            ObjectEntity objDoc = addSingleGroupObject(documentShopPrice, "Документ", baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup, true);

            addPropertyDraw(objDoc, objArt, articleQuantity, shopPrice, prevPrice, revalBalance);

            addFixedFilter(new CompareFilterEntity(getPropertyObject(shopPrice), Compare.NOT_EQUALS, getPropertyObject(prevPrice)));

            addFAProp(documentPriceGroup, "Акт переоценки", this, objDoc);
        }
    }

    private class ActionFormEntity extends BarcodeFormEntity {
        protected ActionFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Акции");

            ObjectEntity objAction = addSingleGroupObject(action, "Акция", allGroup, true);
            addObjectActions(this, objAction);

            ObjectEntity objArtGroup = addSingleGroupObject(articleGroup, "Группа товаров", allGroup, true);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", allGroup, true);

            if (noArticleGroups)
                objArtGroup.groupTo.initClassView = ClassViewType.HIDE;

            addPropertyDraw(objAction, objArtGroup, allGroup, true);
            addPropertyDraw(objAction, objArt, allGroup, true);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(articleToGroup, objArt), Compare.EQUALS, objArtGroup),
                    "В группе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !noArticleGroups);
            addRegularFilterGroup(filterGroup);

            PropertyObjectEntity inActionImpl = addPropertyObject(inAction, objAction, objArt);
            RegularFilterGroupEntity inActionGroup = new RegularFilterGroupEntity(genID());
            inActionGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(inActionImpl),
                    "В акции",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
            inActionGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(inActionImpl)),
                    "Не в акции",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0)));
            addRegularFilterGroup(inActionGroup);

            addAutoAction(objBarcode, addPropertyObject(barcodeAction2, objAction, objBarcode));
        }
    }

    private class PricersFormEntity extends FormEntity {

        protected PricersFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Ценники", true);

            ObjectEntity objDoc = addSingleGroupObject(documentShopPrice, "Документ", baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup, true);

            addPropertyDraw(objDoc, objArt, shopPrice);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(shopPrice)));
            addFixedFilter(new NotFilterEntity(new CompareFilterEntity(getPropertyObject(shopPrice), Compare.EQUALS, addPropertyObject(prevPrice, objDoc, objArt))));

            addFAProp(documentPriceGroup, "Ценники", this, objDoc);
        }
    }

    private class SaleCertFormEntity extends DocumentFormEntity {

        public ObjectEntity objObligation;

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{nameContragentImpl, phoneContragentImpl, bornContragentImpl, addressContragentImpl, initialSumContragentImpl,
                    orderSalePay, orderSalePayCash, orderSalePayCard, orderSaleToDo, orderSaleToDoSum};
        }

        @Override
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        protected SaleCertFormEntity(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, documentClass, toAdd);

            if (!toAdd)
                addPropertyDraw(date, objDoc);
            objObligation = addSingleGroupObject(giftObligation, "Подарочный сертификат", allGroup, true);
            addPropertyDraw(objDoc, objObligation, allGroup, true);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(issueObligation, objDoc, objObligation)),
                    "Документ",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), true);
            addRegularFilterGroup(filterGroup);

            if (!toAdd) {
                addPropertyDraw(checkRetailExported, objDoc);
            }


        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);
            design.setEnabled(allGroup, false, objObligation.groupTo);

            design.get(objObligation.groupTo).grid.defaultComponent = true;

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }


    }

    public class SaleCheckCertFormEntity extends SaleCertFormEntity {

        @Override
        protected boolean isBarcodeFocusable() {
            return false;
        }

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        protected SaleCheckCertFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, saleCheckCert, toAdd);

            objDoc.caption = "Чек";

            if (toAdd) {
                addPropertyDraw(cashRegOperGroup, true);
            }
        }

        @Override
        public boolean hasClientApply() {
            return toAdd;
        }

        @Override
        public ClientAction getClientApply(FormInstance<VEDBusinessLogics> formInstance) {
            if (toAdd) {

                ObjectInstance doc = formInstance.instanceFactory.getInstance(objDoc);
                ObjectInstance obligation = formInstance.instanceFactory.getInstance(objObligation);

                return cashRegController.getCashRegApplyActions(formInstance, 1,
                        BaseUtils.toSetElements(doc.groupTo, obligation.groupTo), BaseUtils.toSetElements(obligation.groupTo),
                        getPropertyDraw(obligationSum, objObligation), getPropertyDraw(issueObligation, objObligation),
                        getPropertyDraw(name, objObligation), getPropertyDraw(obligationSum, objObligation),
                        getPropertyDraw(orderSalePay, objDoc),
                        getPropertyDraw(orderSalePayCard, objDoc), getPropertyDraw(orderSalePayCash, objDoc));

            } else
                return super.getClientApply(formInstance);
        }

        @Override
        public String checkClientApply(Object result) {

            String check = cashRegController.checkCashRegApplyActions(result);
            if (check != null) return check;

            return super.checkClientApply(result);
        }

        @Override
        protected boolean hasExternalScreen() {
            return true;
        }
    }

    private class SaleInvoiceCertFormEntity extends SaleCertFormEntity {
        protected SaleInvoiceCertFormEntity(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, saleInvoiceCert, toAdd);
        }

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{nameContragent, phoneContragent, bornContragent, addressContragent, initialSumContragent,
                    orderSalePay, orderSalePayCash, orderSalePayCard, orderSaleToDo, invoiceNumber, invoiceSeries};
        }
    }

    private class CouponIntervalFormEntity extends FormEntity {
        protected CouponIntervalFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Интервалы цен по купонам");

            ObjectEntity objIntervalAdd = addSingleGroupObject(DoubleClass.instance, "Цена товара от", couponGroup, true);
            objIntervalAdd.groupTo.initClassView = ClassViewType.PANEL;
            objIntervalAdd.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;

            ObjectEntity objInterval = addSingleGroupObject(DoubleClass.instance, "Цена товара", couponGroup, true);
            objInterval.groupTo.banClassView = ClassViewType.PANEL | ClassViewType.HIDE;
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(couponIssueSum, objInterval)));
        }
    }

    private class CouponArticleFormEntity extends FormEntity {
        protected CouponArticleFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Товары по купонам");

            ObjectEntity objArtGroup = addSingleGroupObject(articleGroup, "Группа товаров", baseGroup, true, couponGroup, true);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", baseGroup, true, couponGroup, true);

            if (noArticleGroups)
                objArtGroup.groupTo.initClassView = ClassViewType.HIDE;

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(articleToGroup, objArt), Compare.EQUALS, objArtGroup),
                    "В группе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !noArticleGroups);
            addRegularFilterGroup(filterGroup);

            PropertyObjectEntity inCouponImpl = addPropertyObject(inCoupon, objArt);
            RegularFilterGroupEntity inCouponGroup = new RegularFilterGroupEntity(genID());
            inCouponGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(inCouponImpl),
                    "В акции",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
            inCouponGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(inCouponImpl)),
                    "Не в акции",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0)));
            addRegularFilterGroup(inCouponGroup);
        }
    }

/*    private class LogClientFormEntity extends FormEntity {
        protected LogClientFormEntity(NavigatorElement parent, int ID) {
            super(parent, ID, "Изменения клиентов");

            ObjectEntity objClient = addSingleGroupObject(customerCheckRetail, "Клиент", properties, baseGroup, true);
            ObjectEntity objSession = addSingleGroupObject(session, "Транзакция", properties, baseGroup, true);
            PropertyDrawEntity drawEntity = addPropertyDraw(logClientInitialSum, objClient, objSession);
            addFixedFilter(new NotNullFilterEntity(drawEntity.propertyObject));
        }
    }*/

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        SecurityPolicy permitCachRegister = addPolicy("Разрешить только кассовые формы", "Политика разрешает открытие только форма для работы за кассой.");
        permitCachRegister.navigator.defaultPermission = false;
        permitCachRegister.navigator.permit(commitSaleForm);
        permitCachRegister.navigator.permit(saleCheckCertForm);
        permitCachRegister.navigator.permit(returnSaleCheckRetailArticleForm);
        permitCachRegister.navigator.permit(cachRegManagementForm);

        User admin = addUser("admin", "fusion");
        //админ игнорит настройки в базе, ему разрешено всё
        admin.addSecurityPolicy(permitAllPolicy);
    }
}
