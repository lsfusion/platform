package tmc;

import net.sf.jasperreports.engine.JRException;

import java.sql.SQLException;
import java.io.*;
import java.awt.event.KeyEvent;

import platform.server.data.sql.DataAdapter;
import platform.server.data.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.AggregateProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.classes.*;
import platform.server.view.form.client.ContainerView;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.*;
import platform.server.view.form.client.DefaultFormView;
import platform.server.view.form.*;
import platform.server.auth.User;
import platform.interop.Compare;
import platform.interop.ClassViewType;
import platform.interop.action.*;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexComponentDirections;
import platform.base.BaseUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import tmc.integration.imp.CustomerCheckRetailImportActionProperty;
import tmc.integration.exp.CashRegController;


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

        subject = addAbstractClass("Субъект", namedObject);

        action = addAbstractClass("Акция", baseClass);
        saleAction = addConcreteClass(1, "Распродажа", action);
        articleAction = addConcreteClass(2, "Акции по позициям", action);

        groupArticleAction = addConcreteClass(3, "Группа акций", namedObject);

        store = addAbstractClass("Склад", subject);
        shop = addConcreteClass(4, "Магазин", store);
        warehouse = addConcreteClass(5, "Распред. центр", store);
        article = addConcreteClass(6, "Товар", namedObject, barcodeObject);
        articleGroup = addConcreteClass(7, "Группа товаров", namedObject);
        supplier = addAbstractClass("Поставщик", subject);
        localSupplier = addConcreteClass(8, "Местный поставщик", supplier);
        importSupplier = addConcreteClass(9, "Импортный поставщик", supplier);
        customerWhole = addConcreteClass(10, "Оптовый покупатель", namedObject);
        customerInvoiceRetail = addConcreteClass(11, "Покупатель по накладным", namedObject);
        customerCheckRetail = addConcreteClass(12, "Розничный покупатель", namedObject, barcodeObject);

        format = addConcreteClass(13, "Формат", namedObject);

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
        invoiceReturnDeliveryLocal = addConcreteClass(42, "Выписанная заявка на возврат местному поставщику", orderReturnDeliveryLocal,invoiceDocument);
        commitReturnDeliveryLocal = addConcreteClass(43, "Возврат местному поставщику", invoiceReturnDeliveryLocal, commitOut);

        returnSaleInvoice = addConcreteClass(44, "Возврат по накладной", orderInc, returnInner, commitInc, invoiceDocument);
        returnSaleWhole = addConcreteClass(45, "Оптовый возврат", orderWarehouseInc, orderWhole, returnSaleInvoice);
        returnSaleInvoiceRetail = addConcreteClass(46, "Возврат розничного заказа по накладной", orderShopInc, orderInvoiceRetail, returnSaleInvoice);
        returnSaleCheckRetail = addConcreteClass(47, "Возврат реализации через кассу", orderShopInc, returnInner, commitInc, checkRetail);

        obligation = addAbstractClass("Сертификат", namedObject, barcodeObject);
        coupon = addConcreteClass(48, "Купон", obligation);
        giftObligation = addConcreteClass(49, "Подарочный сертификат", obligation);
    }

    CustomClass saleCert;
    CustomClass saleInvoiceCert;
    CustomClass saleCheckCert;
    CustomClass documentShopPrice;

    LP balanceSklFreeQuantity;
    LP articleFreeQuantity;

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

        incStore = addCUProp("incStore", "Склад (прих.)", // generics
                addDProp("incShop", "Магазин (прих.)", shop, orderShopInc),
                addDProp("incWarehouse", "Распред. центр (прих.)", warehouse, orderWarehouseInc));
        incStoreName = addJProp(baseGroup, "Склад (прих.)", name, incStore, 1);
        outStore = addCUProp("outCStore", "Склад (расх.)", // generics
                addDProp("outStore", "Склад (расх.)", store, orderStoreOut),
                addDProp("outShop", "Магазин (расх.)", shop, orderShopOut),
                addDProp("outWarehouse", "Распред. центр (расх.)", warehouse, orderWarehouseOut));
        addJProp(baseGroup, "Склад (расх.)", name, outStore, 1);

        computerShop = addDProp("computerShop", "Магазин рабочего места", shop, computer);
        addJProp(baseGroup, "Магазин рабочего места", name, computerShop, 1);

        orderSupplier = addCUProp("orderSupplier", "Поставщик", addDProp("localSupplier", "Местный поставщик", localSupplier, orderLocal),
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

        nameContragentImpl = addJProp(true, "Контрагент", name, orderContragent, 1);
        phoneContragentImpl = addJProp(true, "Телефон", customerCheckRetailPhone, orderContragent, 1);
        bornContragentImpl = addJProp(true, "Дата рождения", customerCheckRetailBorn, orderContragent, 1);
        addressContragentImpl = addJProp(true, "Адрес", customerCheckRetailAddress, orderContragent, 1);
        initialSumContragentImpl = addJProp(true, "Начальная сумма", clientInitialSum, orderContragent, 1);

        LP sameContragent = addJProp(equals2, orderContragent, 1, orderContragent, 2);
        LP diffContragent = addJProp(diff2, orderContragent, 1, orderContragent, 2);

        LP invoiceNumber = addDProp(baseGroup, "invoiceNumber", "Накладная", StringClass.get(20), invoiceDocument);

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
        confirmedInnerQuantity = addDUProp("Кол-во подтв. парт.", addJProp(and1, orderInnerQuantity, 1, 2, 3, is(commitOut), 1) , returnedInnerQuantity);
        addConstraint(addJProp("Кол-во возврата должно быть не меньше кол-ва самой операции", greater2, vzero, confirmedInnerQuantity, 1, 2, 3), false);

        // для док. \ товара \ парт. \ док. прод.   - кол-во подтв. парт. если совпадают контрагенты
        returnInnerFreeQuantity = addJProp(documentGroup, "Дост. кол-во по возврату парт.", and(false, true), confirmedInnerQuantity, 4, 2, 3, returnSameClasses, 1, 4, diffContragent, 1, 4);
        returnInnerQuantity = addDGProp(documentGroup, "Кол-во возврата", 2, false, returnInnerCommitQuantity, 1, 2, 4, returnInnerFreeQuantity, 1, 2, 3, 4, date, 3, 3);
        LP returnDocumentQuantity = addCUProp("Кол-во возврата", returnOuterQuantity, returnInnerQuantity); // возвратный документ\прямой документ
        addConstraint(addJProp("При возврате контрагент документа, по которому идет возврат, должен совпадать с контрагентом возврата", and1, diffContragent, 1, 3, returnDocumentQuantity, 1, 2, 3), false);

        // инвентаризация
        innerBalanceCheck = addDProp(documentGroup, "innerBalanceCheck", "Остаток инв.", DoubleClass.instance, balanceCheck, article, commitDelivery);
        innerBalanceCheckDB = addDProp("innerBalanceCheckDB", "Остаток (по учету)", DoubleClass.instance, balanceCheck, article, commitDelivery);

        innerQuantity = addCUProp(documentGroup, "innerQuantity", "Кол-во", returnOuterQuantity, orderInnerQuantity,
                                addDGProp(2, false, returnInnerCommitQuantity, 1, 2, 3, returnInnerFreeQuantity, 1, 2, 3, 4, date, 4, 4),
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

        // ожидаемый приход на склад
        articleFreeOrderQuantity = addSUProp("articleFreeOrderQuantity" , "Ожидаемое своб. кол-во", Union.SUM, articleFreeQuantity, addSGProp(moveGroup, "Ожидается приход", addJProp(andNot1, articleOrderQuantity, 1, 2, is(commitInc), 1), incStore, 1, 2)); // сумма по еще не пришедшим

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
        inAction = addXorUProp(baseGroup, "inAction", "В акции", xorActionArticle, addXorUProp(
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
        obligationIssued = addCGProp(null, "obligationIssued", "Выд. документ", addJProp(and1, 1, issueObligation, 1, 2), issueObligation, 2);

        obligationSum = addDProp(baseGroup, "obligationSum", "Сумма", DoubleClass.instance, obligation);
        LP obligationSumFrom = addDProp(baseGroup, "obligationSumFrom", "Сумма покупки", DoubleClass.instance, obligation);

        LP couponMaxPercent = addDProp(baseGroup, "couponMaxPercent", "Макс. процент по купонам", DoubleClass.instance);

        LP currentStoreDiscount = addCUProp(priceGroup, "Скидка на складе",
                addJProp(and1, currentWarehouseDiscount, is(warehouse), 1),
                addJProp(currentFormatDiscount, shopFormat, 1));

        LP actionPrice = addJProp(priceGroup, "Акц. цена", removePercent, currentPrice, 1, articleDiscount, 1);
        LP requiredStorePrice = addJProp(priceGroup, "Необх. цена", removePercent, actionPrice, 2, currentStoreDiscount, 1);

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

        currentShopPrice = addJProp(priceGroup, "currentShopPrice", "Цена на складе (тек.)", shopPrice, currentShopPriceDoc, 1, 2, 2);

        LP outOfDatePrice = addJProp(and(false,false), vtrue, articleBalanceSklCommitedQuantity, 1, 2, addJProp(diff2, requiredStorePrice, 1, 2, currentShopPrice, 1, 2), 1, 2);
        documentRevalued.setDerivedChange(outOfDatePrice, priceStore, 1, 2);

        prevPrice = addDProp(documentPriceGroup, "prevPrice", "Цена пред.", DoubleClass.instance, documentShopPrice, article);
        prevPrice.setDerivedChange(currentShopPrice, priceStore, 1, 2, inDocumentPrice, 1, 2);
        revalBalance = addDProp(documentPriceGroup, "revalBalance", "Остаток переоц.", DoubleClass.instance, documentShopPrice, article);
        revalBalance.setDerivedChange(articleBalanceSklCommitedQuantity, priceStore, 1, 2, inDocumentPrice, 1, 2);

        isRevalued = addJProp(diff2, shopPrice, 1, 2, prevPrice, 1, 2); // для акта переоценки
        isNewPrice = addJProp(andNot1, inDocumentPrice, 1, 2, addJProp(equals2, shopPrice, 1, 2, prevPrice, 1, 2), 1, 2); // для ценников

        LP saleStorePrice = addCUProp(priceGroup, "Цена прод.", addJProp(and1, requiredStorePrice, 1, 2, is(warehouse), 1), currentShopPrice);

        NDS = addDProp(documentGroup, "NDS", "НДС", DoubleClass.instance, documentNDS, article);
        LP[] maxNDSProps = addMGProp((AbstractGroup)null, true, new String[]{"currentNDSDate","currentNDSDoc"}, new String[]{"Дата посл. НДС","Посл. док. НДС"}, 1,
                addJProp(and1, date, 1, NDS, 1, 2), 1, 2);
        currentNDSDate = maxNDSProps[0]; currentNDSDoc = maxNDSProps[1];
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
        documentLogisticsSupplied = addJProp(documentLogisticsGroup, "Поставляется", equals2, outSubject, 1, addJProp(articleStoreSupplier, incStore, 1, 2), 1, 2);
        documentLogisticsRecommended = addJProp(documentLogisticsGroup, "Рекомендовано", min, documentLogisticsRequired, 1, 2, documentFreeQuantity, 1, 2);

        LP orderClientSaleSum = addDProp("orderClientSaleSum", "Нак. сумма", DoubleClass.instance, orderSaleArticleRetail);
        LP orderClientInitialSum = addDProp("orderClientInitialSum", "Нак. сумма", DoubleClass.instance, orderSaleArticleRetail);
        orderClientSum = addSUProp(baseGroup, "Нак. сумма", Union.SUM, addCProp(DoubleClass.instance, 0, orderSaleArticleRetail), orderClientSaleSum, orderClientInitialSum);
        LP orderHour = addDProp(baseGroup, "orderHour", "Час", DoubleClass.instance, orderSaleArticleRetail);
        orderHour.setDerivedChange(currentHour, is(orderSale), 1);

        changeQuantityTime = addTCProp(Time.EPOCH, "changeQuantityTime", "Время выбора", articleInnerQuantity, orderSaleArticleRetail, article);
        changeQuantityOrder = addOProp(documentGroup, "Номер", addJProp(and1, addCProp(IntegerClass.instance, 1), articleInnerQuantity, 1, 2), false, true, true, 1, 1, changeQuantityTime, 1, 2);

        LP orderSaleDocPrice = addDProp("orderSalePrice", "Цена прод.", DoubleClass.instance, orderSale, article);
        orderSaleDocPrice.setDerivedChange(saleStorePrice, outStore, 1, 2, articleQuantity, 1, 2);
        orderSalePrice = addSUProp(documentPriceGroup, "Цена прод.", Union.OVERRIDE, addJProp(and1, addJProp(saleStorePrice, outStore, 1, 2), 1, 2, is(orderSale), 1), orderSaleDocPrice);

        LP monthDay = addSFProp("EXTRACT(DOY FROM prm1)",IntegerClass.instance,1);
        LP isOrderContragentBirthDay = addJProp(equals2, addJProp(monthDay, date, 1), 1, addJProp(monthDay, addJProp(customerCheckRetailBorn, orderContragent, 1), 1), 1);
        LP articleActionActive = addJProp(and(false, false, false, false, true, true, true, true, true, true), articleQuantity, 1, 2, is(orderSaleArticleRetail), 1, is(articleAction), 3, inAction, 3, 2, isStarted, 3,
                                        addJProp(less2, articleQuantity, 1, 2, articleActionQuantity, 3), 1, 2, 3,
                                        addJProp(and(false, true), articleActionBirthDay, 2, is(orderSaleArticleRetail), 1, isOrderContragentBirthDay, 1), 1, 3,
                                        addJProp(actionNoExtraDiscount, articleSaleAction, 1), 2,
                                        addJProp(less2, orderClientSum, 1, articleActionClientSum, 2), 1, 3,
                                        addJProp(less2, orderHour, 1, articleActionHourFrom, 2), 1, 3,
                                        addJProp(greater2, orderHour, 1, articleActionHourTo, 2), 1, 3);
        orderArticleSaleDiscount = addDProp(baseGroup, "orderArticleSaleDiscount", "Скидка", DoubleClass.instance, orderSaleArticleRetail, article);
        orderArticleSaleDiscount.setDerivedChange(addSUProp(Union.MAX,addSGProp(addMGProp(addJProp(and1, actionDiscount, 3, articleActionActive, 1, 2, 3), 1, 2, articleActionToGroup, 3), 1, 2), addJProp(and1, addJProp(customerCheckRetailDiscount, orderContragent, 1), 1, is(article), 2)), true, 1, 2, is(orderSaleArticleRetail), 1);

        LP orderArticleSaleSum = addJProp(documentPriceGroup, "Сумма прод.", multiplyDouble2, articleQuantity, 1, 2, orderSaleDocPrice, 1, 2);
        LP orderArticleSaleDiscountSum = addJProp(documentPriceGroup, "Сумма скидки", percent, orderArticleSaleSum, 1, 2, orderArticleSaleDiscount, 1, 2);
        orderArticleSaleSumWithDiscount = addDUProp(documentPriceGroup, "Сумма к оплате", orderArticleSaleSum, orderArticleSaleDiscountSum);
        orderSaleDiscountSum = addSGProp(documentAggrPriceGroup, "Сумма скидки", orderArticleSaleDiscountSum, 1);
        orderSalePay = addCUProp(documentAggrPriceGroup, "Сумма чека",
                                    addSGProp(addJProp(and(false, false), obligationSum, 2, issueObligation, 1, 2, is(giftObligation), 2), 1),
                                    addSGProp(orderArticleSaleSumWithDiscount, 1));

        LP returnArticleSaleSum = addJProp(documentPriceGroup, "Сумма возвр.", multiplyDouble2, returnInnerQuantity, 1, 2, 3, orderSaleDocPrice, 3, 2);
        LP returnArticleSaleDiscount = addJProp(documentPriceGroup, "Сумма скидки возвр.", percent, returnArticleSaleSum, 1, 2, 3, orderArticleSaleDiscount, 3, 2);
        returnArticleSalePay = addDUProp(documentPriceGroup, "Сумма к возвр.", returnArticleSaleSum, returnArticleSaleDiscount);
        returnSaleDiscount = addSGProp(documentAggrPriceGroup, "Сумма скидки возвр.", returnArticleSaleDiscount, 1);
        returnSalePay = addDUProp(documentAggrPriceGroup, "Сумма к возвр.", addSGProp("Сумма возвр.", returnArticleSaleSum, 1), returnSaleDiscount);

        LP orderDeliveryPrice = addDProp("orderDeliveryPrice", "Цена закуп.", DoubleClass.instance, orderDelivery, article);
        orderDeliveryPrice.setDerivedChange(articleSupplierPrice, 2, articleQuantity, 1, 2);
        addSUProp(documentPriceGroup, "Цена закуп.", Union.OVERRIDE, addJProp(and1, articleSupplierPrice, 2, is(orderDelivery), 1), orderDeliveryPrice);

        orderSaleUseObligation = addDProp(documentPriceGroup, "orderSaleUseObligation", "Использовать", LogicalClass.instance, commitSaleCheckArticleRetail, obligation);
        LP obligationUseSum = addJProp(and1, obligationSum, 2, orderSaleUseObligation, 1, 2);
        obligationDocument = addCGProp(null, "obligationDocument", "Исп. документ", addJProp(and1, 1, orderSaleUseObligation, 1, 2), orderSaleUseObligation, 2);

        LP addDays = addSFProp("prm1+prm2", DateClass.instance, 2);

        LP couponStart = addDProp(baseGroup, "couponStart", "Дата начала купонов", DateClass.instance);
        LP couponExpiry = addDProp(baseGroup, "couponExpiry", "Дата окончания купонов", DateClass.instance);
        LP certExpiry = addDProp(baseGroup, "certExpiry", "Срок действия серт.", IntegerClass.instance);

        LP dateIssued = addJProp("Дата выдачи", date, obligationIssued, 1);
        LP couponFromIssued = addDProp(baseGroup, "couponFromIssued", "Дата начала", DateClass.instance, coupon);
        couponFromIssued.setDerivedChange(couponStart, dateIssued, 1);
        LP couponToIssued = addDProp("couponToIssued", "Дата окончания", DateClass.instance, coupon);
        couponToIssued.setDerivedChange(couponExpiry, obligationIssued, 1);
        LP certToIssued = addDProp("certToIssued", "Дата окончания", DateClass.instance, giftObligation);
        certToIssued.setDerivedChange(addJProp(addDays, 1, certExpiry), dateIssued, 1);
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
        orderClientInitialSum.setDerivedChange(clientInitialSum, true, orderContragent, 1);
        clientSum = addSUProp(baseGroup, "clientSum", "Нак. сумма", Union.SUM, clientSaleSum, clientInitialSum);

        orderSalePayCash = addDProp(documentPriceGroup, "orderSalePayCash", "Наличными", DoubleClass.instance, orderSaleCheckRetail);
        orderSalePayCard = addDProp(documentPriceGroup, "orderSalePayCard", "Карточкой", DoubleClass.instance, orderSaleCheckRetail);

        // сдача/доплата
        LP orderSaleDiffSum = addDUProp(orderSalePayNoObligation, addSUProp(Union.SUM, orderSalePayCard, orderSalePayCash));
//        orderSaleDiff = addIfElseUProp(documentAggrPriceGroup, "Необходимо", addJProp(string2, addCProp(StringClass.get(6), "Допл:"), orderSaleDiffSum, 1),
//                addJProp(string2, addCProp(StringClass.get(6), "Сдача:"), addNUProp(orderSaleDiffSum), 1), addJProp(positive, orderSaleDiffSum, 1), 1);
        orderSaleToDo = addJProp(documentAggrPriceGroup, "Необходимо", and1, addIfElseUProp(addCProp(StringClass.get(5), "Допл.", orderSaleCheckRetail),
                addCProp(StringClass.get(5), "Сдача", orderSaleCheckRetail), addJProp(positive, orderSaleDiffSum, 1), 1), 1, orderSaleDiffSum, 1);
        orderSaleToDoSum = addJProp(documentAggrPriceGroup, "Сумма необх.", abs, orderSaleDiffSum, 1);

        LP couponCanBeUsed = addJProp(greater2, addJProp(date, obligationIssued, 1), 2, date, 1);

        barcodeAction2 = addJProp(true, "Ввод штрих-кода 2", addCUProp(addSCProp(addIfElseUProp(articleQuantity, articleOrderQuantity, is(commitInc), 1)),
                addIfElseUProp(orderSaleUseObligation, issueObligation, addJProp(diff2, 1, obligationIssued, 2), 1, 2),addJProp(equals2, orderContragent, 1, 2),
                xorActionArticle, articleFormatToSell, NDS, documentRevalued, addJProp(and1, changeUser, 2, is(baseClass), 1)), 1, barcodeToObject, 2);
        barcodeAction3 = addJProp(true, "Ввод штрих-кода 3", addCUProp(addJProp(and(false, false), changeUser, 2, is(baseClass), 1, is(baseClass), 3), addSCProp(returnInnerQuantity)), 1, barcodeToObject, 3, 2);
        seekAction = addJProp(true, "Поиск штрих-кода", addSAProp(null), barcodeToObject, 1);

        LP xorCouponArticleGroup = addDProp(couponGroup, "xorCouponArticleGroup", "Вкл.", LogicalClass.instance, articleGroup);
        LP xorCouponArticle = addDProp(couponGroup, "xorCouponArticle", "Вкл./искл.", LogicalClass.instance, article);
        inCoupon = addXorUProp(couponGroup, "inCoupon", "Выд. купон", xorCouponArticle, addJProp(xorCouponArticleGroup, articleToGroup, 1));

        couponIssueSum = addDProp(couponGroup, "couponIssueSum", "Сумма купона", DoubleClass.instance, DoubleClass.instance);

        LP couponDocToIssueSum = addDProp("couponDocToIssueSum", "Сумма купона к выд.", DoubleClass.instance, commitSaleCheckArticleRetail, article); // здесь конечно хорошо было бы orderSaleDocPrice вытащить за скобки, но будет висячий ключ поэтому приходится пока немого извращаться
        couponDocToIssueSum.setDerivedChange(addIfProp(addMGProp(addJProp(and1, couponIssueSum, 3, addJProp(greater2, orderSaleDocPrice, 1, 2, 3), 1, 2, 3), 1, 2), false, inCoupon, 2), true, 1, 2, orderSaleDocPrice, 1, 2);

        couponToIssueQuantity = addDUProp("К выдаче", addSGProp(articleQuantity, 1, couponDocToIssueSum, 1, 2),
                                          addSGProp(addJProp(and1, addCProp(DoubleClass.instance, 1), addIfProp(issueObligation, false, is(coupon), 2), 1, 2), 1, obligationSum, 2));
        couponToIssueConstraint = addJProp("Кол-во выданных купонов не соответствует требуемому", diff2, couponToIssueQuantity, 1, 2, vzero);
        addConstraint(couponToIssueConstraint, false);

        LP orderUser = addDProp("orderUser", "Исп-ль заказа", user, order);
        orderUser.setDerivedChange(currentUser, true, is(order), 1);
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
    LP orderClientSum;
    public LP orderArticleSaleSumWithDiscount;
    public LP orderSalePrice;
    LP changeQuantityOrder;
    LP computerShop;
    LP orderSalePayCash;
    LP orderSalePayCard;
    LP changeQuantityTime;
    LP confirmedInnerQuantity;
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

        persistents.add((AggregateProperty) clientSum.property);

        persistents.add((AggregateProperty) inAction.property);
        persistents.add((AggregateProperty) inCoupon.property);

        persistents.add((AggregateProperty) obligationIssued.property);

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
        tableFactory.include("customerretail", customerCheckRetail);
        tableFactory.include("articlestore",article,store);
        tableFactory.include("articleorder",article,order);
        tableFactory.include("rates",DateClass.instance);
        tableFactory.include("intervals",DoubleClass.instance);
    }

    protected void initIndexes() {
    }

    public CommitSaleCheckRetailNavigatorForm commitSaleBrowse;
    public SaleCheckCertNavigatorForm saleCheckCertBrowse;
    public ReturnSaleCheckRetailNavigatorForm returnSaleCheckRetailBrowse;

    protected void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement print = new NavigatorElement(baseElement, 4000, "Печатные формы");
            NavigatorForm incomePrice = addNavigatorForm(new IncomePriceNavigatorForm(print, 4100));
            NavigatorForm revalueAct = addNavigatorForm(new RevalueActNavigatorForm(print, 4200));
            NavigatorForm pricers = addNavigatorForm(new PricersNavigatorForm(print, 4300));

        NavigatorElement delivery = new NavigatorElement(baseElement, 1000, "Управление закупками");
            addNavigatorForm(new SupplierArticleNavigatorForm(delivery, 1050));
            NavigatorForm deliveryShopLocal = addNavigatorForm(new DeliveryShopLocalNavigatorForm(delivery, true, 1100));
                NavigatorForm deliveryShopLocalBrowse = addNavigatorForm(new DeliveryShopLocalNavigatorForm(deliveryShopLocal, false, 1125));
            NavigatorForm deliveryWarehouseLocal = addNavigatorForm(new DeliveryWarehouseLocalNavigatorForm(delivery, true, 1130));
                NavigatorForm deliveryWarehouseLocalBrowse = addNavigatorForm(new DeliveryWarehouseLocalNavigatorForm(deliveryWarehouseLocal, false, 1135));
            NavigatorForm deliveryImport = addNavigatorForm(new DeliveryImportNavigatorForm(delivery, true, 1150));
                NavigatorForm deliveryImportBrowse = addNavigatorForm(new DeliveryImportNavigatorForm(deliveryImport, false, 1175));
            NavigatorForm returnDelivery = addNavigatorForm(new ReturnDeliveryLocalNavigatorForm(delivery, 1190, true));
                addNavigatorForm(new ReturnDeliveryLocalNavigatorForm(returnDelivery, 1195, false));

        NavigatorElement sale = new NavigatorElement(baseElement, 1200, "Управление продажами");
            NavigatorElement saleRetail = new NavigatorElement(sale, 1250, "Управление розничными продажами");
                NavigatorElement saleRetailCashRegister = new NavigatorElement(saleRetail, 1300, "Касса");
                    NavigatorForm commitSale = addNavigatorForm(new CommitSaleCheckRetailNavigatorForm(saleRetailCashRegister, 1310, true));
                        commitSaleBrowse = addNavigatorForm(new CommitSaleCheckRetailNavigatorForm(commitSale, 1320, false));
                    NavigatorForm saleCheckCert = addNavigatorForm(new SaleCheckCertNavigatorForm(saleRetailCashRegister, 1325, true));
                        saleCheckCertBrowse = addNavigatorForm(new SaleCheckCertNavigatorForm(saleCheckCert, 1335, false));
                    NavigatorForm returnSaleCheckRetailArticle = addNavigatorForm(new ReturnSaleCheckRetailNavigatorForm(saleRetailCashRegister, true, 1345));
                        returnSaleCheckRetailBrowse = addNavigatorForm(new ReturnSaleCheckRetailNavigatorForm(returnSaleCheckRetailArticle, false, 1355));
                    addNavigatorForm(cashRegController.createCashRegManagementNavigatorForm(saleRetailCashRegister, 1365));
                NavigatorElement saleRetailInvoice = new NavigatorElement(saleRetail, 1400, "Безналичный расчет");
                    NavigatorForm saleRetailInvoiceForm = addNavigatorForm(new OrderSaleInvoiceRetailNavigatorForm(saleRetailInvoice, 1410, true));
                        addNavigatorForm(new OrderSaleInvoiceRetailNavigatorForm(saleRetailInvoiceForm, 1420, false));
                    NavigatorForm saleInvoiceCert = addNavigatorForm(new SaleInvoiceCertNavigatorForm(saleRetailInvoice, 1440, true));
                        addNavigatorForm(new SaleInvoiceCertNavigatorForm(saleInvoiceCert, 1445, false));
                    NavigatorForm returnSaleInvoiceRetailArticle = addNavigatorForm(new ReturnSaleInvoiceRetailNavigatorForm(saleRetailInvoice, true, 1477));
                        addNavigatorForm(new ReturnSaleInvoiceRetailNavigatorForm(returnSaleInvoiceRetailArticle, false, 1487));
            NavigatorElement saleWhole = new NavigatorElement(sale, 1500, "Управление оптовыми продажами");
                NavigatorForm saleWholeForm = addNavigatorForm(new SaleWholeNavigatorForm(saleWhole, 1520, true));
                    addNavigatorForm(new SaleWholeNavigatorForm(saleWholeForm, 1540, false));
                NavigatorForm returnSaleWholeArticle = addNavigatorForm(new ReturnSaleWholeNavigatorForm(saleWhole, 1560, true));
                    addNavigatorForm(new ReturnSaleWholeNavigatorForm(returnSaleWholeArticle, 1580, false));

        NavigatorElement distribute = new NavigatorElement(baseElement, 3000, "Управление распределением");
            NavigatorForm distributeShopForm = addNavigatorForm(new DistributeShopNavigatorForm(distribute, 3100, true));
                NavigatorForm distributeShopBrowseForm = addNavigatorForm(new DistributeShopNavigatorForm(distributeShopForm, 3200, false));
            NavigatorForm distributeWarehouseForm = addNavigatorForm(new DistributeWarehouseNavigatorForm(distribute, 3110, true));
                NavigatorForm distributeWarehouseBrowseForm = addNavigatorForm(new DistributeWarehouseNavigatorForm(distributeWarehouseForm, 3210, false));

        NavigatorElement price = new NavigatorElement(baseElement, 2400, "Управление ценообразованием");
            NavigatorForm documentRevalue = addNavigatorForm(new DocumentRevalueNavigatorForm(price, true, 2650));
                addNavigatorForm(new DocumentRevalueNavigatorForm(documentRevalue, false, 2750));
            addNavigatorForm(new FormatArticleNavigatorForm(price, 2200));
            addNavigatorForm(new GlobalNavigatorForm(price, 5200));

        NavigatorElement tax = new NavigatorElement(baseElement, 5400, "Управление налогами");
            NavigatorForm nds = addNavigatorForm(new DocumentNDSNavigatorForm(tax, true, 5800));
                addNavigatorForm(new DocumentNDSNavigatorForm(nds, false, 5850));

        NavigatorElement actions = new NavigatorElement(baseElement, 7400, "Управление акциями");
            NavigatorForm saleAction = addNavigatorForm(new ActionNavigatorForm(actions, 7800));
            NavigatorForm couponInterval = addNavigatorForm(new CouponIntervalNavigatorForm(actions, 7825));
            NavigatorForm couponArticle = addNavigatorForm(new CouponArticleNavigatorForm(actions, 7850));

        NavigatorElement balance = new NavigatorElement(baseElement, 6500, "Управление хранением");
            NavigatorForm balanceCheck = addNavigatorForm(new BalanceCheckNavigatorForm(balance, 6350, true));
                addNavigatorForm(new BalanceCheckNavigatorForm(balanceCheck, 6375, false));

        NavigatorElement store = new NavigatorElement(baseElement, 2000, "Сводная информация");
            addNavigatorForm(new StoreArticleNavigatorForm(store, 2100));

        commitWholeShopInc.addRelevant(incomePrice);
        documentShopPrice.addRelevant(revalueAct);
        documentShopPrice.addRelevant(pricers);
    }

    public static Font FONT_SMALL_BOLD = new Font("Tahoma", Font.BOLD, 12);
    public static Font FONT_SMALL_PLAIN = new Font("Tahoma", Font.PLAIN, 12);
    public static Font FONT_MEDIUM_BOLD = new Font("Tahoma", Font.BOLD, 14);
    public static Font FONT_LARGE_BOLD = new Font("Tahoma", Font.BOLD, 24);
    public static Font FONT_HUGE_BOLD = new Font("Tahoma", Font.BOLD, 28);

    private class GlobalNavigatorForm extends NavigatorForm {
        protected GlobalNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Глобальные параметры");
            addPropertyView(properties, allGroup, true);
        }
    }

    private class BarcodeNavigatorForm extends NavigatorForm {

        ObjectNavigator objBarcode;

        protected boolean isBarcodeFocusable() { return true; }
        protected Font getDefaultFont() { return null; }

        private BarcodeNavigatorForm(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);

            addPropertyView(reverseBarcode);

            objBarcode = addSingleGroupObjectImplement(StringClass.get(13), "Штрих-код", properties, baseGroup, true);
            objBarcode.groupTo.initClassView = ClassViewType.PANEL;
            objBarcode.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;

//            addAutoAction(objBarcode, addPropertyObjectImplement(barcodeAction, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            if (getDefaultFont() != null)
                design.setFont(getDefaultFont(), true);

            design.get(getPropertyView(reverseBarcode)).setContainer(design.getPanelContainer(design.get(objBarcode.groupTo)));
            design.addIntersection(design.get(objBarcode).objectCellView, design.get(getPropertyView(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.get(getPropertyView(reverseBarcode)), design.get(getPropertyView(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.setFont(design.get(objBarcode).objectCellView, FONT_SMALL_BOLD);
            design.setFont(reverseBarcode, FONT_SMALL_BOLD);
            design.setFont(barcodeObjectName, FONT_LARGE_BOLD);
            design.setBackground(barcodeObjectName, new Color(240,240,240));

            design.setEditKey(design.get(objBarcode).objectCellView, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
            design.setEditKey(reverseBarcode, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

            if (!isBarcodeFocusable()) {
                design.setFocusable(reverseBarcode, false);
                design.setFocusable(false, objBarcode.groupTo);
                design.setFocusable(design.get(objBarcode).objectCellView, false);
            }

            return design;
        }
    }

    private class DocumentNavigatorForm extends BarcodeNavigatorForm {

        public final ObjectNavigator objDoc;

        protected boolean toAdd = false;

        protected static final boolean fixFilters = false;
        protected static final boolean noOuters = true;

        protected Object[] getDocumentProps() {
            return new Object[] {baseGroup, true, documentGroup, true};
        }

        protected boolean isDocumentFocusable() { return true; }

        protected DocumentNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, (toAdd?documentClass.caption:"Документы"));

            this.toAdd = toAdd;

            objDoc = addSingleGroupObjectImplement(documentClass, "Документ", properties, getDocumentProps());
            if(toAdd) {
                objDoc.groupTo.initClassView = ClassViewType.PANEL;
                objDoc.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
                objDoc.show = false;
                objDoc.addOnTransaction = true;
            } else if (!isReadOnly())
                addObjectActions(this, objDoc);

            addAutoAction(objBarcode, addPropertyObjectImplement(barcodeAction2, objDoc, objBarcode));
            addAutoAction(objBarcode, addPropertyObjectImplement(seekAction, objBarcode));

            if(!toAdd)
                addPropertyView(orderUserName, objDoc);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            if (toAdd) {

                // так конечно делать неправильно, но DocumentNavigatorForm - это первый общий класс у продажи сертификатов и кассы
                PropertyViewNavigator payView = getPropertyView(orderSalePay);

                if (payView != null) {
                    // делаем, чтобы суммы были внизу и как можно правее
                    ContainerView docSumsContainer = design.get(payView).getContainer();
                    docSumsContainer.setContainer(design.getMainContainer());
//                    docSumsContainer.constraints.directions = new SimplexComponentDirections(0.1,-0.1,0,0.1);
                    docSumsContainer.constraints.order = 6;

                    PropertyViewNavigator payCash = getPropertyView(orderSalePayCash);
                    PropertyViewNavigator payCard = getPropertyView(orderSalePayCard);
                    PropertyViewNavigator toDo = getPropertyView(orderSaleToDo);
                    PropertyViewNavigator toDoSum = getPropertyView(orderSaleToDoSum);

                    if (payCash != null || payCard != null || toDo != null || toDoSum != null) {

                        ContainerView payContainer = design.addContainer("Платежные средства");
                        payContainer.setContainer(design.getMainContainer());
                        payContainer.constraints.directions = new SimplexComponentDirections(0.1,-0.1,0,0.1);
                        payContainer.constraints.order = 6;

                        if (payCash != null) design.get(payCash).setContainer(payContainer);
                        if (payCard != null) design.get(payCard).setContainer(payContainer);
                        if (toDo != null) design.get(toDo).setContainer(payContainer);
                        if (toDoSum != null) design.get(toDoSum).setContainer(payContainer); 

                        design.addIntersection(docSumsContainer, payContainer, DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                    }
                }

                design.setFont(FONT_MEDIUM_BOLD, objDoc.groupTo);

                // устанавливаем дизайн
                design.setFont(documentPriceGroup, FONT_HUGE_BOLD, objDoc.groupTo);
                design.setBackground(documentAggrPriceGroup, new Color(240,240,240), objDoc.groupTo);

                // ставим Label сверху
                design.setPanelLabelAbove(documentPriceGroup, true, objDoc.groupTo);

                // привязываем функциональные кнопки
                design.setEditKey(nameContragent, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), objDoc.groupTo);
                design.setEditKey(orderSalePayCard, KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
                design.setEditKey(orderSalePayCash, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));

                if (!isDocumentFocusable())
                    design.setFocusable(false, objDoc.groupTo);
                else
                    design.setFocusable(documentAggrPriceGroup, false, objDoc.groupTo);

            }

            return design;
        }
    }

    private abstract class ArticleNavigatorForm extends DocumentNavigatorForm {
        public final ObjectNavigator objArt;

        protected abstract PropertyObjectNavigator getCommitedQuantity();

        protected Object[] getArticleProps() {
            return new Object[] {baseGroup, true};
        }

        protected Object[] getDocumentArticleProps() {
            return new Object[] {baseGroup, true, documentGroup, true};
        }

        protected ArticleNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, ID, documentClass, toAdd);

            objArt = addSingleGroupObjectImplement(article, "Товар", properties, getArticleProps());
            addPropertyView(objDoc, objArt, properties, getDocumentArticleProps());

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
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
            design.get(objArt.groupTo).gridView.constraints.fillVertical = 3;
            return design;
        }

        protected ArticleNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            this(parent, ID, documentClass, toAdd, false);
        }

        protected abstract FilterNavigator getDocumentArticleFilter();

        protected void fillExtraFilters(RegularFilterGroupNavigator filterGroup, boolean toAdd) {
        }

        // такое дебильное множественное наследование
        public void fillExtraLogisticsFilters(RegularFilterGroupNavigator filterGroup, boolean toAdd) {
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyImplement(documentLogisticsSupplied)),
                                  "Поставляется",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(getPropertyImplement(documentLogisticsRequired)),
                                  "Необходимо",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
        }
    }

    // для те которые не различают заказано и выполнено
    private abstract class ArticleNoCheckNavigatorForm extends ArticleNavigatorForm {

        protected FilterNavigator getDocumentArticleFilter() {
            return new NotNullFilterNavigator(getCommitedQuantity());
        }

        protected ArticleNoCheckNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, documentClass, toAdd);
        }

        protected ArticleNoCheckNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, ID, documentClass, toAdd, filled);
        }
    }

    private abstract class InnerNavigatorForm extends ArticleNoCheckNavigatorForm {

        protected PropertyObjectNavigator getCommitedQuantity() {
            return addPropertyObjectImplement(articleInnerQuantity, objDoc, objArt);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupNavigator filterGroup, boolean toAdd) {
            if(!fixFilters)
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(addPropertyObjectImplement(documentFreeQuantity, objDoc, objArt)),
                                  "Дост. кол-во",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
        }

        protected InnerNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, ID, documentClass, toAdd, filled);

            if(fixFilters)
                addFixedFilter(new OrFilterNavigator(getDocumentArticleFilter(),new NotNullFilterNavigator(addPropertyObjectImplement(documentFreeQuantity, objDoc, objArt))));
        }
    }

    private abstract class OuterNavigatorForm extends ArticleNavigatorForm {

        protected PropertyObjectNavigator getCommitedQuantity() {
            return getPropertyImplement(outerCommitedQuantity);
        }

        protected PropertyObjectNavigator getOrderQuantity() {
            return getPropertyImplement(outerOrderQuantity);
        }

        protected FilterNavigator getDocumentArticleFilter() {
            return new OrFilterNavigator(new NotNullFilterNavigator(getOrderQuantity()), new NotNullFilterNavigator(getCommitedQuantity()));
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
        ObjectNavigator objOuter;

        protected ArticleOuterNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd, CustomClass commitClass, boolean filled) {
            super(parent, ID, documentClass, toAdd, filled);

            if(!noOuters) {
                objOuter = addSingleGroupObjectImplement(commitClass, "Партия", properties, baseGroup, true);
                addPropertyView(objOuter, objDoc, properties, baseGroup, true, documentGroup, true);
                addPropertyView(objOuter, objDoc, objArt, properties, baseGroup, true, documentGroup, true);
                addPropertyView(objOuter, objArt, properties, baseGroup, true);

                NotNullFilterNavigator documentFilter = new NotNullFilterNavigator(getPropertyImplement(innerQuantity));
                NotNullFilterNavigator documentFreeFilter = new NotNullFilterNavigator(getPropertyImplement(documentInnerFreeQuantity));
                if(fixFilters)
                        addFixedFilter(new OrFilterNavigator(documentFilter, documentFreeFilter));
                RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      documentFilter,
                                      "Документ",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)),!toAdd || filled);
                if(!fixFilters)
                   filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      documentFreeFilter,
                                      "Дост. кол-во",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)),toAdd && !filled);
                addRegularFilterGroup(filterGroup);
            }
        }
    }

    private class ReturnDeliveryLocalNavigatorForm extends ArticleOuterNavigatorForm {
        public ReturnDeliveryLocalNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, orderReturnDeliveryLocal, toAdd, commitDeliveryLocal, false);
        }
    }

    private class ArticleInnerNavigatorForm extends ArticleOuterNavigatorForm {

        protected ArticleInnerNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, boolean filled) {
            super(parent, ID, documentClass, toAdd, commitDelivery, filled);
        }
    }

    private class DocumentInnerNavigatorForm extends ArticleInnerNavigatorForm {

        protected DocumentInnerNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, boolean filled) {
            super(parent, ID, toAdd, documentClass, filled);
        }
    }

    private class SaleWholeNavigatorForm extends DocumentInnerNavigatorForm {
        public SaleWholeNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, orderSaleWhole, false);
        }
    }

    private abstract class SaleRetailNavigatorForm extends DocumentInnerNavigatorForm {

        public SaleRetailNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            this(parent, ID, orderSaleArticleRetail, toAdd);
        }

        @Override
        protected Object[] getDocumentProps() {
            return new Object[] {nameContragentImpl, phoneContragentImpl, bornContragentImpl, addressContragentImpl, initialSumContragentImpl, orderClientSum,
                                 orderSalePay, orderSaleDiscountSum, orderSalePayNoObligation, orderSalePayCash, orderSalePayCard, orderSaleToDo, orderSaleToDoSum};
        }

        @Override
        protected Object[] getArticleProps() {
            return new Object[] {};
        }

        @Override
        protected Object[] getDocumentArticleProps() {
            return new Object[] {};
        }

        @Override
        protected boolean isBarcodeFocusable() {
            return false;
        }

        @Override
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        protected SaleRetailNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, toAdd, documentClass, true);

            if (!toAdd)
                addPropertyView(date, objDoc);

            // чтобы в порядке нужном
            addPropertyView(changeQuantityOrder, objDoc, objArt);
            addPropertyView(barcode, objArt);
            addPropertyView(name, objArt);
            addPropertyView(articleQuantity, objDoc, objArt); // для timeChanges иначе можно было бы articleQuantity
            addPropertyView(documentFreeQuantity, objDoc, objArt);
            addPropertyView(orderSalePrice, objDoc, objArt);
            addPropertyView(orderArticleSaleDiscount, objDoc, objArt);
            addPropertyView(orderArticleSaleSumWithDiscount, objDoc, objArt);
            if (!toAdd) {
                addPropertyView(orderArticleSaleSumCoeff, objDoc, objArt);
            }

            objArt.show = false; objArt.showClass = false; objArt.showTree = false; objArt.groupTo.banClassView |= ClassViewType.HIDE | ClassViewType.PANEL;

//            addFixedFilter(new OrFilterNavigator(new CompareFilterNavigator(addPropertyObjectImplement(outStore, objDoc), Compare.EQUALS, shopImplement),
//                                                    new NotFilterNavigator(new NotNullFilterNavigator(shopImplement))));

            PropertyObjectNavigator shopImplement = addPropertyObjectImplement(computerShop, CurrentComputerNavigator.instance);
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outStore, objDoc), Compare.EQUALS, shopImplement));

            addFixedOrder(addPropertyObjectImplement(changeQuantityTime, objDoc, objArt), false);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();
            design.setFocusable(orderClientSum, false);

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";

            return design;
        }
    }

    public class CommitSaleCheckRetailNavigatorForm extends SaleRetailNavigatorForm {

        private ObjectNavigator objObligation;
        private ObjectNavigator objCoupon;
        private ObjectNavigator objIssue;

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        private CommitSaleCheckRetailNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, commitSaleCheckArticleRetail, toAdd);

            objObligation = addSingleGroupObjectImplement(obligation, "Оплачено купонами/ сертификатами", properties, baseGroup, true);
            addPropertyView(objDoc, objObligation, properties, baseGroup, true, orderSaleUseObligation);
            objObligation.show = false; objObligation.showClass = false; objObligation.showTree = false; objObligation.groupTo.banClassView |= ClassViewType.HIDE | ClassViewType.PANEL;
//            addFixedFilter(new NotFilterNavigator(new NotNullFilterNavigator(addPropertyObjectImplement(obligationDocument, objObligation))));
//            addFixedFilter(new NotNullFilterNavigator(addPropertyObjectImplement(orderSaleObligationCanNotBeUsed, objDoc, objObligation)));
            addFixedFilter(new NotNullFilterNavigator(addPropertyObjectImplement(orderSaleUseObligation, objDoc, objObligation)));

            objCoupon = addSingleGroupObjectImplement(coupon, "Выдано купонов", properties, baseGroup, true);
            addPropertyView(objDoc, objCoupon, properties, baseGroup, true, issueObligation);
            objCoupon.show = false; objCoupon.showClass = false; objCoupon.showTree = false; objCoupon.groupTo.banClassView |= ClassViewType.HIDE | ClassViewType.PANEL;
//            addFixedFilter(new NotFilterNavigator(new NotNullFilterNavigator(addPropertyObjectImplement(obligationDocument, objObligation))));
//            addFixedFilter(new NotNullFilterNavigator(addPropertyObjectImplement(orderSaleObligationCanNotBeUsed, objDoc, objObligation)));
            addFixedFilter(new NotNullFilterNavigator(addPropertyObjectImplement(issueObligation, objDoc, objCoupon)));

            if(toAdd) {
                objIssue = addSingleGroupObjectImplement(DoubleClass.instance, "Выдать купоны", properties);
                addPropertyView(couponToIssueQuantity, objDoc, objIssue);
                objIssue.groupTo.banClassView |= ClassViewType.HIDE | ClassViewType.PANEL;
                addFixedFilter(new NotNullFilterNavigator(addPropertyObjectImplement(couponToIssueConstraint, objDoc, objIssue)));

                addPropertyView(properties, cashRegOperGroup, true);
            } else {
                addPropertyView(checkRetailExported, objDoc);
            }
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            design.get(objArt.groupTo).gridView.minRowCount = 6;

            if(toAdd) {
                design.get(objIssue.groupTo).gridView.constraints.fillHorizontal /= 3;
                design.get(objIssue.groupTo).gridView.minRowCount = 2;
                design.get(objIssue.groupTo).gridView.showFilter = false;
                design.addIntersection(design.getGroupObjectContainer(objIssue.groupTo), design.getGroupObjectContainer(objCoupon.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objIssue.groupTo), design.getGroupObjectContainer(objObligation.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }

            design.get(objCoupon.groupTo).gridView.minRowCount = 2;
            design.get(objObligation.groupTo).gridView.minRowCount = 2;
            design.get(objCoupon.groupTo).gridView.showFilter = false;
            design.get(objObligation.groupTo).gridView.showFilter = false;
            design.addIntersection(design.getGroupObjectContainer(objCoupon.groupTo), design.getGroupObjectContainer(objObligation.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }

        @Override
        public List<? extends ClientAction> getApplyActions(RemoteForm remoteForm) {
            if (toAdd) {

                ObjectImplement doc = remoteForm.mapper.mapObject(objDoc);
                ObjectImplement art = remoteForm.mapper.mapObject(objArt);

                return cashRegController.getCashRegApplyActions(remoteForm, 1,
                    BaseUtils.toSetElements(doc.groupTo, art.groupTo), BaseUtils.toSetElements(art.groupTo),
                    getPropertyView(orderSalePrice, objArt), getPropertyView(articleQuantity, objArt),
                    getPropertyView(name, objArt), getPropertyView(orderArticleSaleSumWithDiscount, objArt),
                    getPropertyView(orderSalePayNoObligation, objDoc),
                    getPropertyView(orderSalePayCard, objDoc), getPropertyView(orderSalePayCash, objDoc));
            } else
                return super.getApplyActions(remoteForm);
        }

        @Override
        public String checkApplyActions(int actionID, ClientActionResult result) {

            String check = cashRegController.checkCashRegApplyActions(actionID, result);
            if (check != null) return check;

            return super.checkApplyActions(actionID, result);
        }
    }

    private class OrderSaleInvoiceRetailNavigatorForm extends SaleRetailNavigatorForm {

        private OrderSaleInvoiceRetailNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, orderSaleInvoiceArticleRetail, toAdd);
        }
    }

    private class DistributeNavigatorForm extends DocumentInnerNavigatorForm {
        public DistributeNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass) {
            super(parent, ID, toAdd, documentClass, false);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupNavigator filterGroup, boolean toAdd) {
            fillExtraLogisticsFilters(filterGroup, toAdd);
        }
    }

    private class DistributeShopNavigatorForm extends DocumentInnerNavigatorForm {
        public DistributeShopNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, orderDistributeShop, false);
        }
    }

    private class DistributeWarehouseNavigatorForm extends DocumentInnerNavigatorForm {
        public DistributeWarehouseNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, orderDistributeWarehouse, false);
        }
    }

    private class BalanceCheckNavigatorForm extends DocumentInnerNavigatorForm {
        public BalanceCheckNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, balanceCheck, false);
        }
    }

    private class ReturnSaleNavigatorForm extends DocumentNavigatorForm {

        @Override
        protected Object[] getDocumentProps() {
            return new Object[] {returnSaleDiscount, returnSalePay};
        }

        @Override
        protected boolean isDocumentFocusable() {
            return false;
        }

        @Override
        protected boolean isBarcodeFocusable() {
            return false;
        }

        @Override
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        public final ObjectNavigator objInner;
        public final ObjectNavigator objArt;
        ObjectNavigator objOuter;

        protected ReturnSaleNavigatorForm(NavigatorElement parent, int ID, boolean toAdd, CustomClass documentClass, CustomClass commitClass) {
            super(parent, ID, documentClass, toAdd);

            objInner = addSingleGroupObjectImplement(commitClass, "Документ к возврату", properties, baseGroup, true);

            addPropertyView(objInner, objDoc, properties, baseGroup, true, documentGroup, true);

            NotNullFilterNavigator documentFilter = new NotNullFilterNavigator(getPropertyImplement(sumReturnedQuantity));
            NotNullFilterNavigator documentFreeFilter = new NotNullFilterNavigator(getPropertyImplement(sumReturnedQuantityFree));
            if(fixFilters)
                addFixedFilter(new OrFilterNavigator(documentFilter, documentFreeFilter));
            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  documentFilter,
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), !toAdd);
            if(!fixFilters)
                filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  documentFreeFilter,
                                  "Дост. кол-во",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
            addRegularFilterGroup(filterGroup);

            objArt = addSingleGroupObjectImplement(article, "Товар", properties);

            addPropertyView(changeQuantityOrder, objInner, objArt);
            addPropertyView(barcode, objArt);
            addPropertyView(name, objArt);
            addPropertyView(objInner, objDoc, objArt, properties, baseGroup, true, documentGroup, true);
            addPropertyView(orderSalePrice, objInner, objArt);
            addPropertyView(orderArticleSaleDiscount, objInner, objArt);
            addPropertyView(orderArticleSaleSumWithDiscount, objInner, objArt);

            PropertyObjectNavigator returnInnerImplement = getPropertyImplement(returnInnerQuantity);

            NotNullFilterNavigator articleFilter = new NotNullFilterNavigator(returnInnerImplement);
            NotNullFilterNavigator articleFreeFilter = new NotNullFilterNavigator(getPropertyImplement(returnFreeQuantity));
            if(fixFilters)
                addFixedFilter(new OrFilterNavigator(articleFilter, articleFreeFilter));
            RegularFilterGroupNavigator articleFilterGroup = new RegularFilterGroupNavigator(IDShift(1));
            articleFilterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  articleFilter,
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), !toAdd);
            if(!fixFilters)
                articleFilterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  articleFreeFilter,
                                  "Дост. кол-во",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
            addRegularFilterGroup(articleFilterGroup);

//            addHintsNoUpdate(properties, moveGroup);
            addFixedOrder(addPropertyObjectImplement(changeQuantityTime, objInner, objArt), false);

            PropertyObjectNavigator shopImplement = addPropertyObjectImplement(computerShop, CurrentComputerNavigator.instance);
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(incStore, objDoc), Compare.EQUALS, shopImplement));

            if(!noOuters) {
                objOuter = addSingleGroupObjectImplement(commitDelivery, "Партия", properties, baseGroup, true);

                addPropertyView(objInner, objOuter, objDoc, properties, baseGroup, true, documentGroup, true);
                addPropertyView(objInner, objOuter, objDoc, objArt, properties, baseGroup, true, documentGroup, true);
                addPropertyView(objInner, objOuter, properties, baseGroup, true);
                addPropertyView(objInner, objOuter, objArt, properties, baseGroup, true);

                NotNullFilterNavigator documentCommitFilter = new NotNullFilterNavigator(getPropertyImplement(returnInnerCommitQuantity));
                NotNullFilterNavigator documentCommitFreeFilter = new NotNullFilterNavigator(getPropertyImplement(returnInnerFreeQuantity));
                if(fixFilters)
                    addFixedFilter(new OrFilterNavigator(documentCommitFilter, documentCommitFreeFilter));
                RegularFilterGroupNavigator filterOutGroup = new RegularFilterGroupNavigator(IDShift(1));
                filterOutGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      documentCommitFilter,
                                      "Документ",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !toAdd);
                if(!fixFilters)
                    filterOutGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                      documentCommitFreeFilter,
                                      "Макс. кол-во",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)), toAdd);
                addRegularFilterGroup(filterOutGroup);
            }

            addAutoAction(objBarcode, true, addPropertyObjectImplement(barcodeAction3, objDoc, objInner, objBarcode), addPropertyObjectImplement(seekAction, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            if (toAdd) {

                // делаем, чтобы суммы были внизу и как можно правее
                design.get(getPropertyView(returnSalePay)).getContainer().setContainer(design.getMainContainer());
//                design.get(getPropertyView(returnSalePay)).getContainer().constraints.directions = new SimplexComponentDirections(0.1,-0.1,0,0.1);
                design.get(getPropertyView(returnSalePay)).getContainer().constraints.order = 3;
            }

            if(!noOuters)
                design.addIntersection(design.getGroupObjectContainer(objInner.groupTo), design.getGroupObjectContainer(objOuter.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class ReturnSaleWholeNavigatorForm extends ReturnSaleNavigatorForm {
        private ReturnSaleWholeNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, toAdd, returnSaleWhole, commitSaleWhole);
        }
    }

    private class ReturnSaleInvoiceRetailNavigatorForm extends ReturnSaleNavigatorForm {
        private ReturnSaleInvoiceRetailNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, returnSaleInvoiceRetail, commitSaleInvoiceArticleRetail);
        }
    }

    public class ReturnSaleCheckRetailNavigatorForm extends ReturnSaleNavigatorForm {

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        private ReturnSaleCheckRetailNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, toAdd, returnSaleCheckRetail, commitSaleCheckArticleRetail);

            if(toAdd) {
                addPropertyView(properties, cashRegOperGroup, true);
            } else {
                addPropertyView(checkRetailExported, objDoc);
            }
        }

        @Override
        public List<ClientAction> getApplyActions(RemoteForm remoteForm) {
            if (toAdd) {

                ObjectImplement doc = remoteForm.mapper.mapObject(objDoc);
                ObjectImplement inner = remoteForm.mapper.mapObject(objInner);
                ObjectImplement art = remoteForm.mapper.mapObject(objArt);

                return cashRegController.getCashRegApplyActions(remoteForm, 2,
                        BaseUtils.toSetElements(doc.groupTo, inner.groupTo, art.groupTo), BaseUtils.toSetElements(inner.groupTo, art.groupTo),
                        getPropertyView(orderSalePrice, objArt), getPropertyView(returnInnerQuantity, objArt),
                        getPropertyView(name, objArt), getPropertyView(returnArticleSalePay, objArt),
                        getPropertyView(returnSalePay, objDoc),
                        null, null);
            } else
                return super.getApplyActions(remoteForm);
        }

        @Override
        public String checkApplyActions(int actionID, ClientActionResult result) {

            String check = cashRegController.checkCashRegApplyActions(actionID, result);
            if (check != null) return check;

            return super.checkApplyActions(actionID, result);
        }
    }

    private class SupplierArticleNavigatorForm extends NavigatorForm {
        protected SupplierArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Ассортимент поставщиков");

            ObjectNavigator objSupplier = addSingleGroupObjectImplement(supplier, "Поставщик", properties, allGroup, true);
            addObjectActions(this, objSupplier);

            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, allGroup, true);
            addObjectActions(this, objArt);

            addPropertyView(objSupplier, objArt, properties, allGroup, true);

            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(articleSupplier,objArt),Compare.EQUALS,objSupplier));
        }
    }

    private class StoreArticleNavigatorForm extends BarcodeNavigatorForm {
        private ObjectNavigator objArt;

        protected StoreArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по складу");

            ObjectNavigator objStore = addSingleGroupObjectImplement(store, "Склад", properties, allGroup, true);
            objArt = addSingleGroupObjectImplement(article, "Товар", properties, allGroup, true);
            ObjectNavigator objOuter = addSingleGroupObjectImplement(commitDelivery, "Партия", properties, allGroup, true);

            addPropertyView(objStore, objArt, properties, allGroup, true);
            addPropertyView(objStore, objOuter, properties, allGroup, true);
            addPropertyView(objOuter, objArt, properties, baseGroup, true);
            addPropertyView(objStore, objOuter, objArt, properties, allGroup, true);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView form = super.createDefaultRichDesign();
            form.get(objArt.groupTo).gridView.constraints.fillVertical = 3;
            return form;
        }
    }

    private class FormatArticleNavigatorForm extends BarcodeNavigatorForm {
        protected FormatArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Остатки по форматам");

            ObjectNavigator objFormat = addSingleGroupObjectImplement(format, "Формат", properties, allGroup, true);
            addObjectActions(this, objFormat);

            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, allGroup, true);

            addPropertyView(objFormat, objArt, properties, allGroup, true);

            addAutoAction(objBarcode, addPropertyObjectImplement(barcodeAction2, objFormat, objBarcode));
        }
    }

    private class DocumentRevalueNavigatorForm extends ArticleNoCheckNavigatorForm {

        protected PropertyObjectNavigator getCommitedQuantity() {
            return getPropertyImplement(documentRevalued);
        }

        protected DocumentRevalueNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, documentRevalue, toAdd, true);
        }
    }

    private class DocumentNDSNavigatorForm extends ArticleNoCheckNavigatorForm {

        protected PropertyObjectNavigator getCommitedQuantity() {
            return getPropertyImplement(NDS);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupNavigator filterGroup, boolean toAdd) {
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotFilterNavigator(new NotNullFilterNavigator(getPropertyImplement(currentNDS))),
                                  "Без НДС",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
        }

        protected DocumentNDSNavigatorForm(NavigatorElement parent, boolean toAdd, int ID) {
            super(parent, ID, documentNDS, toAdd);

            addHintsNoUpdate(currentNDSDoc);
            addHintsNoUpdate(currentNDSDate);
        }
    }

    private class IncomePriceNavigatorForm extends NavigatorForm {

        protected IncomePriceNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Реестр цен", true);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(commitWholeShopInc, "Документ", properties, baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, articleQuantity, shopPrice);

            addFixedFilter(new NotNullFilterNavigator(getPropertyImplement(shopPrice)));

            addFAProp(documentPriceGroup, "Реестр цен", this, objDoc);
        }
    }

    private class RevalueActNavigatorForm extends NavigatorForm {

        protected RevalueActNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Акт переоценки", true);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(documentShopPrice, "Документ", properties, baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, articleQuantity, shopPrice, prevPrice, revalBalance);

            addFixedFilter(new CompareFilterNavigator(getPropertyImplement(shopPrice), Compare.NOT_EQUALS, getPropertyImplement(prevPrice)));

            addFAProp(documentPriceGroup, "Акт переоценки", this, objDoc);
        }
    }

    private class ActionNavigatorForm extends BarcodeNavigatorForm {
        protected ActionNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Акции");

            ObjectNavigator objAction = addSingleGroupObjectImplement(action, "Акция", properties, allGroup, true);
            addObjectActions(this, objAction);

            ObjectNavigator objArtGroup = addSingleGroupObjectImplement(articleGroup, "Группа товаров", properties, allGroup, true);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, allGroup, true);

            if(noArticleGroups)
                objArtGroup.groupTo.initClassView = ClassViewType.HIDE;

            addPropertyView(objAction, objArtGroup, properties, allGroup, true);
            addPropertyView(objAction, objArt, properties, allGroup, true);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(addPropertyObjectImplement(articleToGroup, objArt), Compare.EQUALS, objArtGroup),
                                  "В группе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !noArticleGroups);
            addRegularFilterGroup(filterGroup);

            PropertyObjectNavigator inActionImpl = addPropertyObjectImplement(inAction, objAction, objArt);
            RegularFilterGroupNavigator inActionGroup = new RegularFilterGroupNavigator(IDShift(1));
            inActionGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(inActionImpl),
                                  "В акции",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
            inActionGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotFilterNavigator(new NotNullFilterNavigator(inActionImpl)),
                                  "Не в акции",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0)));
            addRegularFilterGroup(inActionGroup);

            addAutoAction(objBarcode, addPropertyObjectImplement(barcodeAction2, objAction, objBarcode));
        }
    }

    private class PricersNavigatorForm extends NavigatorForm {

        protected PricersNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Ценники", true);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(documentShopPrice, "Документ", properties, baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true);

            addPropertyView(objDoc, objArt, properties, shopPrice);

            addFixedFilter(new NotNullFilterNavigator(getPropertyImplement(shopPrice)));
            addFixedFilter(new NotFilterNavigator(new CompareFilterNavigator(getPropertyImplement(shopPrice), Compare.EQUALS, addPropertyObjectImplement(prevPrice,objDoc,objArt))));

            addFAProp(documentPriceGroup, "Ценники", this, objDoc);
        }
    }

    private class SaleCertNavigatorForm extends DocumentNavigatorForm {

        public ObjectNavigator objObligation;

        @Override
        protected Object[] getDocumentProps() {
            return new Object[] {nameContragentImpl, phoneContragentImpl, bornContragentImpl, addressContragentImpl, initialSumContragentImpl,
                                 orderSalePay, orderSalePayCash, orderSalePayCard, orderSaleToDo, orderSaleToDoSum};
        }

        @Override
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        protected SaleCertNavigatorForm(NavigatorElement parent, int ID, CustomClass documentClass, boolean toAdd) {
            super(parent, ID, documentClass, toAdd);

            if (!toAdd)
                addPropertyView(date, objDoc);

            objObligation = addSingleGroupObjectImplement(giftObligation, "Подарочный сертификат", properties, allGroup, true);

            addPropertyView(objDoc, objObligation, properties, allGroup, true);

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(addPropertyObjectImplement(issueObligation,objDoc,objObligation)),
                                  "Документ",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), true);
            addRegularFilterGroup(filterGroup);

            if (!toAdd) {
                addPropertyView(checkRetailExported, objDoc);
            }
        }
    }

    public class SaleCheckCertNavigatorForm extends SaleCertNavigatorForm {

        @Override
        protected boolean isBarcodeFocusable() {
            return false;
        }

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        protected SaleCheckCertNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, saleCheckCert, toAdd);

            if(toAdd) {
                addPropertyView(properties, cashRegOperGroup, true);
            } else {
                addPropertyView(checkRetailExported, objDoc);
            }
        }

        @Override
        public List<ClientAction> getApplyActions(RemoteForm remoteForm) {
            if (toAdd) {

                ObjectImplement doc = remoteForm.mapper.mapObject(objDoc);
                ObjectImplement obligation = remoteForm.mapper.mapObject(objObligation);

                return cashRegController.getCashRegApplyActions(remoteForm, 1,
                        BaseUtils.toSetElements(doc.groupTo, obligation.groupTo), BaseUtils.toSetElements(obligation.groupTo),
                        getPropertyView(obligationSum, objObligation), getPropertyView(issueObligation, objObligation),
                        getPropertyView(name, objObligation), getPropertyView(obligationSum, objObligation),
                        getPropertyView(orderSalePay, objDoc),
                        getPropertyView(orderSalePayCard, objDoc), getPropertyView(orderSalePayCash, objDoc));

            } else
                return super.getApplyActions(remoteForm);
        }

        @Override
        public String checkApplyActions(int actionID, ClientActionResult result) {

            String check = cashRegController.checkCashRegApplyActions(actionID, result);
            if (check != null) return check;

            return super.checkApplyActions(actionID, result);
        }
    }

    private class SaleInvoiceCertNavigatorForm extends SaleCertNavigatorForm {
        protected SaleInvoiceCertNavigatorForm(NavigatorElement parent, int ID, boolean toAdd) {
            super(parent, ID, saleInvoiceCert, toAdd);
        }
    }

    private class CouponIntervalNavigatorForm extends NavigatorForm {
        protected CouponIntervalNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Интервалы цен по купонам");

            ObjectNavigator objIntervalAdd = addSingleGroupObjectImplement(DoubleClass.instance, "Цена товара от", properties, couponGroup, true);
            objIntervalAdd.groupTo.initClassView = ClassViewType.PANEL;
            objIntervalAdd.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;

            ObjectNavigator objInterval = addSingleGroupObjectImplement(DoubleClass.instance, "Цена товара", properties, couponGroup, true);
            objInterval.groupTo.banClassView = ClassViewType.PANEL | ClassViewType.HIDE;
            addFixedFilter(new NotNullFilterNavigator(addPropertyObjectImplement(couponIssueSum, objInterval)));
        }
    }

    private class CouponArticleNavigatorForm extends NavigatorForm {
        protected CouponArticleNavigatorForm(NavigatorElement parent, int ID) {
            super(parent, ID, "Товары по купонам");

            ObjectNavigator objArtGroup = addSingleGroupObjectImplement(articleGroup, "Группа товаров", properties, baseGroup, true, couponGroup, true);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties, baseGroup, true, couponGroup, true);

            if(noArticleGroups)
                objArtGroup.groupTo.initClassView = ClassViewType.HIDE;

            RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
            filterGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new CompareFilterNavigator(addPropertyObjectImplement(articleToGroup, objArt), Compare.EQUALS, objArtGroup),
                                  "В группе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)), !noArticleGroups);
            addRegularFilterGroup(filterGroup);

            PropertyObjectNavigator inCouponImpl = addPropertyObjectImplement(inCoupon, objArt);
            RegularFilterGroupNavigator inCouponGroup = new RegularFilterGroupNavigator(IDShift(1));
            inCouponGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotNullFilterNavigator(inCouponImpl),
                                  "В акции",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)));
            inCouponGroup.addFilter(new RegularFilterNavigator(IDShift(1),
                                  new NotFilterNavigator(new NotNullFilterNavigator(inCouponImpl)),
                                  "Не в акции",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0)));
            addRegularFilterGroup(inCouponGroup);
        }
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
    }
}
