package tmc;

import jxl.Sheet;
import jxl.Workbook;
import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.*;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexComponentDirections;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.query.OrderType;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.filter.CompareFilterInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.*;
import platform.server.integration.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.session.DataSession;
import tmc.integration.PanelExternalScreen;
import tmc.integration.PanelExternalScreenParameters;
import tmc.integration.exp.AbstractSaleExportTask;
import tmc.integration.exp.CashRegController;
import tmc.integration.imp.CustomerCheckRetailImportActionProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.List;


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
    CustomClass commitDeliveryShopLocal;
    CustomClass orderDeliveryWarehouseLocal;
    CustomClass commitDeliveryWarehouseLocal;
    // закупка у импортного поставщика
    CustomClass orderDeliveryImport;
    CustomClass commitDeliveryImport;

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

    CustomClass move, moveInner, returnInner, returnOuter, orderInner, commitInner;

    CustomClass supplier;
    public ConcreteCustomClass article;
    public ConcreteCustomClass currency;
    public ConcreteCustomClass unitOfMeasure;
    CustomClass store, articleGroup, localSupplier, importSupplier, orderLocal, format, brend, line, gender;
    CustomClass customerWhole;
    CustomClass customerInvoiceRetail;
    public ConcreteCustomClass customerCheckRetail;
    //CustomClass customerCheckRetail;
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

        subject = addAbstractClass("subject", "Субъект", baseClass.named);

        action = addAbstractClass("action", "Акция", baseClass);
        saleAction = addConcreteClass("saleAction", "Распродажа", action);
        articleAction = addConcreteClass("articleAction", "Акции по позициям", action);

        groupArticleAction = addConcreteClass("groupArticleAction", "Группа акций", baseClass.named);

        store = addAbstractClass("store", "Склад", subject);
        shop = addConcreteClass("shop", "Магазин", store);
        warehouse = addConcreteClass("warehouse", "Распред. центр", store);
        article = addConcreteClass("article", "Товар", baseClass.named, barcodeObject);
        articleGroup = addConcreteClass("articleGroup", "Группа товаров", baseClass.named);

        currency = addConcreteClass("currency", "Валюта", baseClass.named);
        unitOfMeasure = addConcreteClass("unitOfMeasure", "Единица измерения", baseClass.named);

        // новый классы
        brend = addConcreteClass("brend", "Бренд", baseClass.named);
        line = addConcreteClass("line", "Линия", baseClass.named);
        gender = addConcreteClass("gender", "Пол", baseClass.named);

        supplier = addAbstractClass("supplier", "Поставщик", subject);
        localSupplier = addConcreteClass("localSupplier", "Местный поставщик", supplier);
        importSupplier = addConcreteClass("importSupplier", "Импортный поставщик", supplier);
        customerWhole = addConcreteClass("customerWhole", "Оптовый покупатель", baseClass.named);
        customerInvoiceRetail = addConcreteClass("customerInvoiceRetail", "Покупатель по накладным", baseClass.named);
        customerCheckRetail = addConcreteClass("customerCheckRetail", "Розничный покупатель", baseClass.named, barcodeObject);

        format = addConcreteClass("format", "Формат", baseClass.named);

        documentShopPrice = addAbstractClass("documentShopPrice", "Изменение цены в магазине", transaction);
        documentRevalue = addConcreteClass("documentRevalue", "Переоценка в магазине", documentShopPrice);

        documentNDS = addConcreteClass("documentNDS", "Изменение НДС", transaction);

        // заявки на приход, расход
        order = addAbstractClass("order", "Заявка", transaction);

        orderInc = addAbstractClass("orderInc", "Заявка прихода на склад", order);
        orderShopInc = addAbstractClass("orderShopInc", "Заявка прихода на магазин", orderInc);
        orderWarehouseInc = addAbstractClass("orderWarehouseInc", "Заявка прихода на распред. центр", orderInc);

        documentInner = addAbstractClass("documentInner", "Внутренняя операция", order);
        returnInner = addAbstractClass("returnInner", "Возврат внутренней операции", order);
        orderInner = addAbstractClass("orderInner", "Заказ", documentInner);

        orderOut = addAbstractClass("orderOut", "Заявка расхода со склада", documentInner);
        orderStoreOut = addAbstractClass("orderStoreOut", "Заявка расхода со склада", orderOut);
        orderShopOut = addAbstractClass("orderShopOut", "Заявка расхода с магазина", orderOut);
        orderWarehouseOut = addAbstractClass("orderWarehouseOut", "Заявка расхода с распред. центра", orderOut);

        invoiceDocument = addAbstractClass("invoiceDocument", "Заявка на перевозку", order);
        commitOut = addAbstractClass("commitOut", "Отгруженная заявка", order);
        commitInc = addAbstractClass("commitInc", "Принятая заявка", commitOut);

        commitInner = addAbstractClass("commitInner", "Отгруженный заказ", commitOut, orderInner);

        commitWholeShopInc = addAbstractClass("commitWholeShopInc", "Принятый оптовый приход на магазин", documentShopPrice, orderShopInc, commitInc);

        // внутр. и внешние операции
        orderDelivery = addAbstractClass("orderDelivery", "Закупка", orderInc); // всегда прих., создает партию - элементарную единицу учета
        commitDelivery = addAbstractClass("commitDelivery", "Приход от пост.", orderDelivery, commitInc, invoiceDocument);

        orderSale = addAbstractClass("orderSale", "Продажа", orderOut);

        orderWhole = addAbstractClass("orderWhole", "Оптовая операция", order);
        orderInvoiceRetail = addAbstractClass("orderInvoiceRetail", "Розничная операция по накладной", order);

        checkRetail = addAbstractClass("checkRetail", "Кассовые операции", baseClass);
        orderSaleCheckRetail = addAbstractClass("orderSaleCheckRetail", "Реализация через кассу", order, checkRetail);

        orderSaleWhole = addConcreteClass("orderSaleWhole", "Оптовый заказ", orderWarehouseOut, orderInner, orderWhole, orderSale);
        invoiceSaleWhole = addConcreteClass("invoiceSaleWhole", "Выписанный оптовый заказ", orderSaleWhole, invoiceDocument);
        commitSaleWhole = addConcreteClass("commitSaleWhole", "Отгруженный оптовый заказ", invoiceSaleWhole, commitInner);

        orderSaleArticleRetail = addAbstractClass("orderSaleArticleRetail", "Розничный заказ товаров", orderShopOut, orderInner, orderSale);
        orderSaleInvoiceArticleRetail = addConcreteClass("orderSaleInvoiceArticleRetail", "Розничный заказ товаров по накладной", orderSaleArticleRetail, orderInvoiceRetail);
        commitSaleInvoiceArticleRetail = addConcreteClass("commitSaleInvoiceArticleRetail", "Отгруженный розничный заказ по накладной", commitInner,
                addConcreteClass("writtenOutSaleInvoiceArticleRetail", "Выписанный розничный заказ по накладной", orderSaleInvoiceArticleRetail, invoiceDocument));
        commitSaleCheckArticleRetail = addConcreteClass("commitSaleCheckArticleRetail", "Реализация товаров через кассу", orderSaleArticleRetail, commitOut, orderSaleCheckRetail);

        saleCert = addConcreteClass("saleCert", "Реализация сертификатов", order);
        saleInvoiceCert = addConcreteClass("saleInvoiceCert", "Реализация сертификатов по накладной", saleCert, orderInvoiceRetail, invoiceDocument);
        saleCheckCert = addConcreteClass("saleCheckCert", "Реализация сертификатов через кассу", saleCert, orderSaleCheckRetail);

        balanceCheck = addConcreteClass("balanceCheck", "Инвентаризация", orderStoreOut, commitOut, documentInner);

        orderDistributeShop = addConcreteClass("orderDistributeShop", "Заказ на внутреннее перемещение на магазин", orderWarehouseOut, orderShopInc, orderInner);
        addConcreteClass("commitDistributeShop", "Принятое внутреннее перемещение на магазин", commitWholeShopInc,
                addConcreteClass("loadedDistributeShop", "Отгруженное внутреннее перемещение на магазин", commitInner,
                        addConcreteClass("writtenOutDistributeShop", "Выписанное внутреннее перемещение на магазин", orderDistributeShop, invoiceDocument)));
        orderDistributeWarehouse = addConcreteClass("orderDistributeWarehouse", "Заказ на внутреннее перемещение на распред. центр", orderStoreOut, orderWarehouseInc, orderInner);
        addConcreteClass("commitDistributeWarehouse", "Принятое внутреннее перемещение на распред. центр", commitInc,
                addConcreteClass("loadedDistributeWarehouse", "Отгруженное внутреннее перемещение на распред. центр", commitInner,
                        addConcreteClass("writtenOutDistributeWarehouse", "Выписанное внутреннее перемещение на распред. центр", orderDistributeWarehouse, invoiceDocument)));

        orderLocal = addAbstractClass("orderLocal", "Операция с местным поставщиком", order);

        orderDeliveryLocal = addAbstractClass("orderDeliveryLocal", "Закупка у местного поставщика", orderDelivery, orderLocal);
        commitDeliveryLocal = addAbstractClass("commitDeliveryLocal", "Приход от местного поставщика", orderDeliveryLocal, commitDelivery);

        orderDeliveryShopLocal = addConcreteClass("orderDeliveryShopLocal", "Закупка у местного поставщика на магазин", orderDeliveryLocal, orderShopInc);
        commitDeliveryShopLocal = addConcreteClass("commitDeliveryShopLocal", "Приход от местного поставщика на магазин", orderDeliveryShopLocal, commitDeliveryLocal, commitWholeShopInc);

        orderDeliveryWarehouseLocal = addConcreteClass("orderDeliveryWarehouseLocal", "Закупка у местного поставщика на распред. центр", orderDeliveryLocal, orderWarehouseInc);
        commitDeliveryWarehouseLocal = addConcreteClass("commitDeliveryWarehouseLocal", "Приход от местного поставщика на распред. центр", orderDeliveryWarehouseLocal, commitDeliveryLocal);

        orderDeliveryImport = addConcreteClass("orderDeliveryImport", "Закупка у импортного поставщика", orderDelivery, orderWarehouseInc);
        commitDeliveryImport = addConcreteClass("commitDeliveryImport", "Приход от импортного поставщика", orderDeliveryImport, commitDelivery);

        orderReturnDeliveryLocal = addConcreteClass("orderReturnDeliveryLocal", "Заявка на возврат местному поставщику", orderStoreOut, documentInner, orderLocal);
        invoiceReturnDeliveryLocal = addConcreteClass("invoiceReturnDeliveryLocal", "Выписанная заявка на возврат местному поставщику", orderReturnDeliveryLocal, invoiceDocument);
        commitReturnDeliveryLocal = addConcreteClass("commitReturnDeliveryLocal", "Возврат местному поставщику", invoiceReturnDeliveryLocal, commitOut);

        returnSaleInvoice = addConcreteClass("returnSaleInvoice", "Возврат по накладной", orderInc, returnInner, commitInc, invoiceDocument);
        returnSaleWhole = addConcreteClass("returnSaleWhole", "Оптовый возврат", orderWarehouseInc, orderWhole, returnSaleInvoice);
        returnSaleInvoiceRetail = addConcreteClass("returnSaleInvoiceRetail", "Возврат розничного заказа по накладной", orderShopInc, orderInvoiceRetail, returnSaleInvoice);
        returnSaleCheckRetail = addConcreteClass("returnSaleCheckRetail", "Возврат реализации через кассу", orderShopInc, returnInner, commitInc, checkRetail);

        obligation = addAbstractClass("obligation", "Сертификат", baseClass.named, barcodeObject);
        coupon = addConcreteClass("coupon", "Купон", obligation);
        giftObligation = addConcreteClass("giftObligation", "Подарочный сертификат", obligation);
    }

    CustomClass saleCert;
    CustomClass saleInvoiceCert;
    CustomClass saleCheckCert;
    CustomClass documentShopPrice;

    LP balanceSklFreeQuantity;
    LP articleFreeQuantity;
    LP obligationSumFrom;
    LP couponFromIssued;
    LP obligationToIssued;
    LP documentBarcodePrice, documentBarcodePriceOv;
    LP invoiceNumber, invoiceSeries;
    LP orderBirthDay;
    LP payWithCard;
    LP printOrderCheck;
    // выноски
    LP orderUser;
    //LP orderArticleSaleDiscountSum;
    LP orderHour;
    LP orderContragentBarcode;
    LP orderUserBarcode;
    LP orderComputer;
    LP saleExport;
    LP importOrder;
    LP articleSaleAction;

    LP articleFormatMin;
    LP articleFormatToSell;

    LP articleCountry;

    LP orderAllDeliveryPrice;
    LP sumNDSRetailOrderArticle;
    LP sumDeliveryOrderArticle;
    LP sumNDSDeliveryOrderArticle;

    LP sumWithNDSDeliveryOrderArticle;
    LP sumWithoutNDSRetailOrderArticle;
    LP addvOrderArticle;

    LP sumRetailOrder;
    LP sumNDSRetailOrder;
    LP sumDeliveryOrder;
    LP sumNDSDeliveryOrder;

    LP sumWithNDSDeliveryOrder;
    LP sumWithoutNDSRetailOrder;
    LP sumAddvOrder;
    LP sumPriceChangeOrder;
    LP addvOrder;


    protected void initProperties() {

        LP removePercent = addSFProp("((prm1*(100-prm2))/100)", DoubleClass.instance, 2);
        LP backPercent = addSFProp("prm1*prm2/(100+prm2)", DoubleClass.instance, 2);
        LP calcPercent = addSFProp("prm1*100/prm2", DoubleClass.instance, 2);
        LP diff = addSFProp("prm1-prm2", DoubleClass.instance, 2);

        LP round1 = addSFProp("(ROUND(CAST((prm1) as NUMERIC(15,3)),-1))", IntegerClass.instance, 1);
        LP round0 = addSFProp("(ROUND(CAST((prm1) as NUMERIC(15,3)),0))", IntegerClass.instance, 1);

        LP multiplyDouble2 = addMFProp(DoubleClass.instance, 2);

        LP positive = addJProp(greater2, 1, vzero);
        LP negative = addJProp(less2, 1, vzero);
        LP onlyPositive = addJProp(and1, 1, positive, 1);
        LP min = addSFProp("(prm1+prm2-ABS(prm1-prm2))/2", DoubleClass.instance, 2);
        LP abs = addSFProp("ABS(prm1)", DoubleClass.instance, 1);

        LP groupParent = addDProp("groupParent", "Родительская группа", articleGroup, articleGroup);
        LP groupParentName = addJProp(baseGroup, "Родительская группа", name, groupParent, 1);

        articleToGroup = addDProp("articleToGroup", "Группа товаров", articleGroup, article); // принадлежность товара группе
        articleToGroupName = addJProp(baseGroup, "Группа товаров", name, articleToGroup, 1);

        incStore = addCUProp("incStore", true, "Склад (прих.)", // generics
                addDProp("incShop", "Магазин (прих.)", shop, orderShopInc),
                addDProp("incWarehouse", "Распред. центр (прих.)", warehouse, orderWarehouseInc));
        incStoreName = addJProp(baseGroup, "Склад (прих.)", name, incStore, 1);
        outStore = addCUProp("outCStore", true, "Склад (расх.)", // generics
                addDProp("outStore", "Склад (расх.)", store, orderStoreOut),
                addDProp("outShop", "Магазин (расх.)", shop, orderShopOut),
                addDProp("outWarehouse", "Распред. центр (расх.)", warehouse, orderWarehouseOut),
                addDProp("certStore", "Магазин (серт.)", shop, saleCert));
        outStoreName = addJProp(baseGroup, "Склад (расх.)", name, outStore, 1);

        payWithCard = addAProp(new PayWithCardActionProperty());
        printOrderCheck = addAProp(new PrintOrderCheckActionProperty());
        saleExport = addAProp(new SaleExportActionProperty());
        importOrder = addAProp(new ImportOrderActionProperty());

        computerShop = addDProp("computerShop", "Магазин рабочего места", shop, computer);
        currentShop = addJProp("Текущий магазин", computerShop, currentComputer);

        panelScreenComPort = addDProp(baseGroup, "panelComPort", "COM-порт табло", IntegerClass.instance, computer);
        cashRegComPort = addDProp(baseGroup, "cashRegComPort", "COM-порт фискального регистратора", IntegerClass.instance, computer);
        addJProp(baseGroup, "Магазин рабочего места", name, computerShop, 1);

        // новые свойства поставщика
        LP supplierDogovor = addDProp(baseGroup, "supplierDogovor", "Номер договора", StringClass.get(20), supplier);
        LP supplierDogovorDate = addDProp(baseGroup, "supplierDogovorDate", "Дата начала договора", DateClass.instance, supplier);

        // новые свойства местного поставщика
        LP localSupplierUNN = addDProp(baseGroup, "localSupplierUNN", "УНН", StringClass.get(20), localSupplier);
        LP localSupplierAddress = addDProp(baseGroup, "localSupplierAddress", "Адрес", StringClass.get(100), localSupplier);
        LP localSupplierTel = addDProp(baseGroup, "localSupplierTel", "Контактный телефон", StringClass.get(20), localSupplier);
        LP localSupplierAddressBank = addDProp(baseGroup, "localSupplierAddressBank", "Адрес банка", StringClass.get(100), localSupplier);
        LP localSupplierBill = addDProp(baseGroup, "localSupplierBill", "Счёт", StringClass.get(30), localSupplier);

        orderSupplier = addCUProp("orderSupplier", true, "Поставщик", addDProp("localSupplier", "Местный поставщик", localSupplier, orderLocal),
                addDProp("importSupplier", "Импортный поставщик", importSupplier, orderDeliveryImport)); // совокупность местных и импортных поставщиков

        LP outSubject = addCUProp(addJProp(and1, orderSupplier, 1, is(orderDelivery), 1), outStore);

        customerCheckRetailPhone = addDProp(baseGroup, "checkRetailCustomerPhone", "Телефон", StringClass.get(20), customerCheckRetail);
        customerCheckRetailBorn = addDProp(baseGroup, "checkRetailCustomerBorn", "Дата рождения", DateClass.instance, customerCheckRetail);
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

        invoiceNumber = addDProp(baseGroup, "invoiceNumber", "Накладная", StringClass.get(15), invoiceDocument);
        invoiceSeries = addDProp(baseGroup, "invoiceSeries", "Серия", StringClass.get(4), invoiceDocument);

        outerOrderQuantity = addDProp(documentGroup, "extIncOrderQuantity", "Кол-во заяв.", DoubleClass.instance, orderDelivery, article);
        outerCommitedQuantity = addDProp(documentGroup, "extIncCommitedQuantity", "Кол-во принятое", DoubleClass.instance, commitDelivery, article);
//        outerCommitedQuantity.setDerivedChange(outerOrderQuantity, 1, 2, is(commitInc), 1); // пока не будем делать так как идет ручное штрих-кодирование
        LP expiryDate = addDProp(baseGroup, "expiryDate", "Срок годн.", DateClass.instance, commitDelivery, article);

        // для возвратных своего рода generics
        LP returnOuterQuantity = addDProp("returnDeliveryLocalQuantity", "Кол-во возврата", DoubleClass.instance, orderReturnDeliveryLocal, article, commitDeliveryLocal);

        returnInnerCommitQuantity = addCUProp(documentGroup, "returnInnerCommitQuantity", "Кол-во возврата", // generics
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
        returnInnerFreeQuantity = addJProp(documentGroup, "returnInnerFreeQuantity", "Дост. кол-во по возврату парт.", and(false, true), confirmedInnerQuantity, 4, 2, 3, returnSameClasses, 1, 4, diffContragent, 1, 4);
        returnInnerQuantity = addDGProp(documentGroup, "returnInnerQuantity", "Кол-во возврата", 2, false, returnInnerCommitQuantity, 1, 2, 4, returnInnerFreeQuantity, 1, 2, 3, 4, date, 3, 3);
        LP returnDocumentQuantity = addCUProp("Кол-во возврата", returnOuterQuantity, returnInnerQuantity); // возвратный документ\прямой документ
        addConstraint(addJProp("При возврате контрагент документа, по которому идет возврат, должен совпадать с контрагентом возврата", and1, diffContragent, 1, 3, returnDocumentQuantity, 1, 2, 3), false);

        // инвентаризация
        innerBalanceCheck = addDProp(documentGroup, "innerBalanceCheck", "Остаток инв.", DoubleClass.instance, balanceCheck, article, commitDelivery);
        innerBalanceCheckDB = addDProp("innerBalanceCheckDB", "Остаток (по учету)", DoubleClass.instance, balanceCheck, article, commitDelivery);

        innerQuantity = addCUProp(documentGroup, "innerQuantity", true, "Кол-во", returnOuterQuantity, orderInnerQuantity,
                addDGProp(privateGroup, "returnDistrCommitQuantity", true, "Возвр. кол-во", 2, false, returnInnerCommitQuantity, 1, 2, 3, returnInnerFreeQuantity, 1, 2, 3, 4, date, 4, 4),
                addDUProp("balanceCheckQuantity", "Кол-во инв.", innerBalanceCheckDB, innerBalanceCheck));

        LP incSklCommitedQuantity = addSGProp(moveGroup, "incSklCommitedQuantity", true, "Кол-во прихода парт. на скл.",
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
        articleInnerQuantity = addDGProp(documentGroup, "articleInnerQuantity", true, "Кол-во", 2, false, innerQuantity, 1, 2, documentInnerFreeQuantity, 1, 2, 3, date, 3, 3);
        addIndex(articleInnerQuantity);
        documentFreeQuantity = addSGProp(documentMoveGroup, "Доступ. кол-во", documentInnerFreeQuantity, 1, 2);

        LP saleCertGiftObligation = addDProp("saleCertGiftObligation", "Выдать", LogicalClass.instance, saleCert, giftObligation);

        articleQuantity = addCUProp("Кол-во", outerCommitedQuantity, articleInnerQuantity);
        articleOrderQuantity = addCUProp("Заяв. кол-во", outerOrderQuantity, articleInnerQuantity);
        LP articleDocQuantity = addCUProp("Кол-во док.", addSUProp(Union.OVERRIDE, outerCommitedQuantity, outerOrderQuantity), articleInnerQuantity);
        LP absQuantity = addSGProp("Всего тов.", addCUProp(addJProp(abs, articleDocQuantity, 1, 2), addJProp(and1, addCProp(DoubleClass.instance, 1), saleCertGiftObligation, 1, 2)), 1);
        addConstraint(addJProp("Нельзя создавать пустые документы", andNot1, is(order), 1, addJProp(greater2, absQuantity, 1, vzero), 1), false);

        // ожидаемый приход на склад
        articleFreeOrderQuantity = addSUProp("articleFreeOrderQuantity", true, "Ожидаемое своб. кол-во", Union.SUM, articleFreeQuantity, addSGProp(moveGroup, "Ожидается приход", addJProp(andNot1, articleOrderQuantity, 1, 2, is(commitInc), 1), incStore, 1, 2)); // сумма по еще не пришедшим

        articleBalanceCheck = addDGProp(documentGroup, "articleBalanceCheck", "Остаток инв.", 2, false, innerBalanceCheck, 1, 2, innerBalanceCheckDB, 1, 2, 3, date, 3, 3);

        LP articleBalanceSklCommitedQuantity = addSGProp(moveGroup, "articleBalanceSklCommitedQuantity", "Остаток тов. на скл.", balanceSklCommitedQuantity, 1, 2);
        addJProp(documentMoveGroup, "Остаток тов. прих.", articleBalanceSklCommitedQuantity, incStore, 1, 2);
        addJProp(documentMoveGroup, "Остаток тов. расх.", articleBalanceSklCommitedQuantity, outStore, 1, 2);

        // цены
        LP shopFormat = addDProp("shopFormat", "Формат", format, shop);
        addJProp(baseGroup, "Формат", name, shopFormat, 1);

        // новые свойства товара
        LP fullName = addDProp(artExtraGroup, "fullName", "Полное наименование", StringClass.get(100), article);
        LP gigiena = addDProp(artExtraGroup, "gigiena", "Гигиеническое разрешение", StringClass.get(50), article);
        LP articleStatus = addDProp(artExtraGroup, "articleStatus", "Собственный/несобственный", LogicalClass.instance, article);

        LP articleBrend = addDProp("articleBrend", "Бренд товара", brend, article);
        addJProp(artExtraGroup, "Бренд товара", name, articleBrend, 1);

        articleCountry = addDProp("articleCountry", "Страна товара", country, article);
        addJProp(artExtraGroup, "Страна товара", name, articleCountry, 1);

        LP articleLine = addDProp("articleLine", "Линия товара", line, article);
        addJProp(artExtraGroup, "Линия товара", name, articleLine, 1);

        LP articleGender = addDProp("articleGender", "Пол", gender, article);
        addJProp(artExtraGroup, "Пол", name, articleGender, 1);
        //**************************************************************************************************************
        currentRRP = addDProp(priceGroup, "currentRRP", "RRP", DoubleClass.instance, article);
        currencyArticle = addDProp(priceGroup, "currencyArticle", "Валюта", currency, article);
        unitOfMeasureArticle = addDProp("unitOfMeasureArticle", "Ед. изм.", unitOfMeasure, article);
        nameUnitOfMeasureArticle = addJProp(baseGroup, "nameUnitOfMeasureArticle", "Ед. изм.", name, unitOfMeasureArticle, 1);
        LP currentCurrencyRate = addDProp(baseGroup, "currentCurrencyRate", "Курс", DoubleClass.instance, currency);
        LP currentFormatDiscount = addDProp(priceGroup, "currentFormatDiscount", "Скидка на формат", DoubleClass.instance, format);
        LP currentWarehouseDiscount = addDProp(priceGroup, "currentWarehouseDiscount", "Опт. скидка", DoubleClass.instance);
        LP currentPrice = addJProp(priceGroup, "Необх. цена", multiplyDouble2, currentRRP, 1, addJProp(currentCurrencyRate, currencyArticle, 1), 1);

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
                addJProp(less2, currentDate, actionFrom, 1), 1,  // активация акции, если текущая дата в диапазоне акции
                addJProp(greater2, currentDate, actionTo, 1), 1);

        articleSaleAction = addCGProp(priceGroup, false, "articleAction", "Дейст. распродажа",
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
        LP articleActionWithCheck = addDProp(baseGroup, "articleActionWithCheck", "Нак. с тек. чеком", LogicalClass.instance, articleAction);

        // продажа облигаций
        //**************************************************************************************************************
        // новые свойства для подарочных сертификатов
        LP certToSaled = addDProp(baseGroup, "certToSaled", "Продан заранее", LogicalClass.instance, obligation);
        LP sverka = addJProp(equals2, 1, addJProp(and1, 1, certToSaled, 1), 2);
        LP issueCoupon = addDProp("orderSaleCoupon", "Выдать купон", LogicalClass.instance, commitSaleCheckArticleRetail, coupon);
        issueObligation = addCUProp(documentPriceGroup, "Выдать", sverka, saleCertGiftObligation, issueCoupon);

        LP obligationIssued = addCGProp(null, "obligationIssued", true, "Выд. документ", addJProp(and1, 1, issueObligation, 1, 2), issueObligation, 2);

        obligationSum = addDProp(baseGroup, "obligationSum", "Сумма", DoubleClass.instance, obligation);
        obligationSumFrom = addDProp(baseGroup, "obligationSumFrom", "Сумма покупки", DoubleClass.instance, obligation);

        LP couponMaxPercent = addDProp(baseGroup, "couponMaxPercent", "Макс. процент по купонам", DoubleClass.instance);

        LP currentStoreDiscount = addCUProp(priceGroup, "Скидка на складе",
                addJProp(and1, currentWarehouseDiscount, is(warehouse), 1),
                addJProp(currentFormatDiscount, shopFormat, 1));    // берётся скидка формата, если её нет - оптовая скидка

        LP actionPrice = addJProp(priceGroup, "Акц. цена", removePercent, currentPrice, 1, articleDiscount, 1);
        LP requiredStorePrice = addJProp(priceGroup, "requiredStorePrice", true, "Необх. цена", removePercent, actionPrice, 2, currentStoreDiscount, 1);

        balanceFormatFreeQuantity = addSGProp(moveGroup, "Своб. кол-во по форм.", articleFreeQuantity, shopFormat, 1, 2);

        LP revalueShop = addDProp("revalueShop", "Магазин", shop, documentRevalue);
        addJProp(baseGroup, "Магазин", name, revalueShop, 1);
        priceStore = addCUProp("priceStore", true, "Склад (цены)", incStore, revalueShop);

        documentRevalued = addDProp(documentGroup, "isRevalued", "Переоц.", LogicalClass.instance, documentRevalue, article);
        inDocumentPrice = addCUProp("inDocumentPrice", true, "Изм. цены", documentRevalued, addJProp(and1, is(commitWholeShopInc), 1, articleQuantity, 1, 2));

        LP[] maxShopPriceProps = addMGProp((AbstractGroup) null, true, new String[]{"currentShopPriceDate", "currentShopPriceDoc"}, new String[]{"Дата посл. цены в маг.", "Посл. док. цены в маг."}, 1,
                addJProp(and1, date, 1, inDocumentPrice, 1, 2), 1, priceStore, 1, 2);
        currentShopPriceDate = maxShopPriceProps[0];
        currentShopPriceDoc = maxShopPriceProps[1];
        addPersistent(currentShopPriceDate);
        addPersistent(currentShopPriceDoc);

        shopPrice = addDCProp(documentPriceGroup, "shopPrice", "Цена (док.)", true, requiredStorePrice, priceStore, 1, 2, inDocumentPrice, 1, 2);

        currentShopPrice = addJProp(priceGroup, "currentShopPrice", "Цена на складе (тек.)", shopPrice, currentShopPriceDoc, 1, 2, 2);

        LP outOfDatePrice = addJProp(and(false, false), vtrue, articleBalanceSklCommitedQuantity, 1, 2, addJProp(diff2, requiredStorePrice, 1, 2, currentShopPrice, 1, 2), 1, 2);
        documentRevalued.setDerivedChange(outOfDatePrice, priceStore, 1, 2);

        prevPrice = addDCProp(documentPriceGroup, "prevPrice", "Цена пред.", true, currentShopPrice, priceStore, 1, 2, inDocumentPrice, 1, 2);
        revalBalance = addDCProp(documentPriceGroup, "revalBalance", "Остаток переоц.", true, articleBalanceSklCommitedQuantity, priceStore, 1, 2, inDocumentPrice, 1, 2);

        isRevalued = addJProp(diff2, shopPrice, 1, 2, prevPrice, 1, 2); // для акта переоценки
        isNewPrice = addJProp(andNot1, inDocumentPrice, 1, 2, addJProp(equals2, shopPrice, 1, 2, prevPrice, 1, 2), 1, 2); // для ценников

        LP saleStorePrice = addCUProp(priceGroup, "Цена прод.", addJProp(and1, requiredStorePrice, 1, 2, is(warehouse), 1), currentShopPrice);

        NDS = addDProp(documentGroup, "NDS", "НДС", DoubleClass.instance, documentNDS, article);
        LP[] maxNDSProps = addMGProp((AbstractGroup) null, true, new String[]{"currentNDSDate", "currentNDSDoc"}, new String[]{"Дата посл. НДС", "Посл. док. НДС"}, 1,
                addJProp(and1, date, 1, NDS, 1, 2), 1, 2);
        currentNDSDate = maxNDSProps[0];
        currentNDSDoc = maxNDSProps[1];
        addPersistent(currentNDSDate);
        addPersistent(currentNDSDoc);
        currentNDS = addJProp("Тек. НДС", NDS, currentNDSDoc, 1, 1);

        // блок с логистикой\управленческими характеристиками

        // текущая схема
        articleSupplier = addDProp("articleSupplier", "Поставщик товара", supplier, article);
        addJProp(logisticsGroup, "Поставщик товара", name, articleSupplier, 1);
        LP shopWarehouse = addDProp("storeWarehouse", "Распред. центр", warehouse, shop); // магазин может числиться не более чем в одном распределяющем центре
        addJProp(logisticsGroup, "Распред. центр", name, shopWarehouse, 1);
        LP articleSupplierPrice = addDProp(logisticsGroup, "articleSupplierPrice", "Цена поставок", DoubleClass.instance, article);
        LP supplierCycle = addDProp(logisticsGroup, "supplierCycle", "Цикл поставок", DoubleClass.instance, supplier);
        LP shopCycle = addDProp(logisticsGroup, "shopCycle", "Цикл распределения", DoubleClass.instance, shop);

        LP supplierToWarehouse = addDProp(logisticsGroup, "supplierToWarehouse", "Пост. на распред. центр", LogicalClass.instance, supplier);

        // абстрактный товар \ склад - поставщик
        LP articleSuppliedOnWarehouse = addJProp(supplierToWarehouse, articleSupplier, 1);
        articleStoreSupplier = addSUProp("articleStoreSupplier", true, "Пост. товара на склад", Union.OVERRIDE, addJProp(and1, articleSupplier, 2, is(store), 1),
                addJProp(and1, shopWarehouse, 1, articleSuppliedOnWarehouse, 2));

        LP storeSupplierCycle = addCUProp(addJProp(and1, supplierCycle, 2, is(store), 1), addJProp(and1, shopCycle, 1, is(warehouse), 2));
        // цикл распределения, если от распределяющего центра или цикл поставок, если от поставщика

        articleStorePeriod = addJProp("articleStorePeriod", true, "Цикл поставок на склад", storeSupplierCycle, 1, articleStoreSupplier, 1, 2);

        articleFormatToSell = addDProp(logisticsGroup, "articleFormatToSell", "В ассортименте", LogicalClass.instance, format, article);
        articleFormatMin = addDProp(logisticsGroup, "articleFormatMin", "Страх. запас", DoubleClass.instance, format, article);

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
        orderHour = addDCProp(baseGroup, "orderHour", "Час", currentHour, is(orderSale), 1, orderSaleArticleRetail);

        changeQuantityTime = addTCProp(Time.EPOCH, "changeQuantityTime", "Время выбора", articleInnerQuantity, orderSaleArticleRetail);
        changeQuantityOrder = addOProp(documentGroup, "Номер", OrderType.SUM, addJProp(and1, addCProp(IntegerClass.instance, 1), articleInnerQuantity, 1, 2), true, true, 1, 1, changeQuantityTime, 1, 2);

        orderSaleDocPrice = addDCProp("orderSalePrice", "Цена прод.", true, saleStorePrice, outStore, 1, 2, articleQuantity, 1, 2, orderSale);
        orderSalePrice = addSUProp(documentPriceGroup, "Цена прод.", Union.OVERRIDE, addJProp(and1, addJProp(saleStorePrice, outStore, 1, 2), 1, 2, is(orderSale), 1), orderSaleDocPrice);
        documentBarcodePrice = addJProp("Цена", orderSalePrice, 1, barcodeToObject, 2);
        documentBarcodePriceOv = addSUProp("Цена", Union.OVERRIDE, documentBarcodePrice, addJProp(and1, addJProp(obligationSum, barcodeToObject, 1), 2, is(order), 1));

        ndsOrderArticle = addDCProp(baseGroup, "ndsOrderArticle", "НДС", currentNDS, 2, articleQuantity, 1, 2);

        LP monthDay = addSFProp("EXTRACT(MONTH FROM prm1) * 40 + EXTRACT(DAY FROM prm1)", IntegerClass.instance, 1);
        orderBirthDay = addDCProp("orderBirthDay", "День рожд.", addJProp(equals2, monthDay, 1, addJProp(monthDay, customerCheckRetailBorn, 1), 2), true, date, 1, orderContragent, 1);

        LP orderArticleSaleSum = addJProp(documentPriceGroup, "Сумма прод.", multiplyDouble2, articleQuantity, 1, 2, orderSaleDocPrice, 1, 2);

        LP orderActionClientSum = addSUProp(Union.SUM, addJProp(and1, orderClientSum, 1, is(articleAction), 2), addJProp(and1, addSGProp(orderArticleSaleSum, 1), 1, articleActionWithCheck, 2));
        LP articleActionActive = addJProp(and(false, false, false, false, true, true, true, true, true), articleQuantity, 1, 2, is(orderSaleArticleRetail), 1, is(articleAction), 3, inAction, 3, 2, isStarted, 3,
                addJProp(less2, articleQuantity, 1, 2, articleActionQuantity, 3), 1, 2, 3,
                addJProp(and(false, true), articleActionBirthDay, 2, is(orderSaleArticleRetail), 1, orderBirthDay, 1), 1, 3,
                addJProp(less2, orderActionClientSum, 1, 2, articleActionClientSum, 2), 1, 3,
                addJProp(less2, orderHour, 1, articleActionHourFrom, 2), 1, 3,
                addJProp(greater2, orderHour, 1, articleActionHourTo, 2), 1, 3);

        orderNoDiscount = addDProp(baseGroup, "orderNoDiscount", "Без. скидок", LogicalClass.instance, orderSaleArticleRetail);
        orderArticleSaleDiscount = addDCProp(baseGroup, "orderArticleSaleDiscount", "Скидка", true, addJProp(and(true, true),
                addSUProp(Union.MAX,
                        addSGProp(addMGProp(addJProp(and1, actionDiscount, 3, articleActionActive, 1, 2, 3), 1, 2, articleActionToGroup, 3), 1, 2),
                        addJProp(and1, addJProp(customerCheckRetailDiscount, orderContragent, 1), 1, is(article), 2)), 1, 2,
                addJProp(actionNoExtraDiscount, articleSaleAction, 1), 2, orderNoDiscount, 1),
                true, 1, 2, articleQuantity, 1, 2);

        orderArticleSaleDiscountSum = addJProp(documentPriceGroup, "Сумма скидки", round1, addJProp(percent, orderArticleSaleSum, 1, 2, orderArticleSaleDiscount, 1, 2), 1, 2);
        orderArticleSaleSumWithDiscount = addDUProp(documentPriceGroup, "Сумма к опл.", orderArticleSaleSum, orderArticleSaleDiscountSum);
        orderSaleDiscountSum = addSGProp(documentAggrPriceGroup, "Сумма скидки", orderArticleSaleDiscountSum, 1);
        LP orderSalePayGift = addSGProp(addJProp(and(false, false), obligationSum, 2, issueObligation, 1, 2, is(giftObligation), 2), 1);
        orderSalePay = addCUProp(documentAggrPriceGroup, "Сумма чека",
                orderSalePayGift, addSGProp(orderArticleSaleSumWithDiscount, 1));

        LP returnArticleSaleSum = addJProp(documentPriceGroup, "Сумма возвр.", multiplyDouble2, returnInnerQuantity, 1, 2, 3, orderSaleDocPrice, 3, 2);
        returnArticleSaleDiscount = addJProp(documentPriceGroup, "Сумма скидки возвр.", round1, addJProp(percent, returnArticleSaleSum, 1, 2, 3, orderArticleSaleDiscount, 3, 2), 1, 2, 3);
        returnArticleSalePay = addDUProp(documentPriceGroup, "Сумма к возвр.", returnArticleSaleSum, returnArticleSaleDiscount);
        returnSaleDiscount = addSGProp(documentAggrPriceGroup, "Сумма скидки возвр.", returnArticleSaleDiscount, 1);
        returnSalePay = addDUProp(documentAggrPriceGroup, "Сумма к возвр.", addSGProp("Сумма возвр.", returnArticleSaleSum, 1), returnSaleDiscount);

        LP orderDeliveryPrice = addDCProp("orderDeliveryPrice", "Цена закуп.", true, articleSupplierPrice, 2, articleQuantity, 1, 2, is(orderDelivery), 1);
        orderAllDeliveryPrice = addSUProp(documentPriceGroup, "orderAllDeliveryPrice", "Цена закуп.", Union.OVERRIDE, addJProp(and1, articleSupplierPrice, 2, is(orderDelivery), 1), orderDeliveryPrice);

        // с округлениями
        sumRetailOrderArticle = addJProp(documentPriceGroup, "sumRetailOrderArticle", "Сумма розн.", multiplyDouble2, shopPrice, 1, 2, articleQuantity, 1, 2);
        sumNDSRetailOrderArticle = addJProp(documentPriceGroup, "sumNDSRetailOrderArticle", "Сумма НДС (розн.)", round1, addJProp(backPercent, sumRetailOrderArticle, 1, 2, ndsOrderArticle, 1, 2), 1, 2);
        sumDeliveryOrderArticle = addJProp(documentPriceGroup, "sumDeliveryOrderArticle", "Сумма пост.", multiplyDouble2, orderDeliveryPrice, 1, 2, articleQuantity, 1, 2);
        sumNDSDeliveryOrderArticle = addJProp(documentPriceGroup, "sumNDSDeliveryOrderArticle", "Сумма НДС (пост.)", round1, addJProp(percent, sumDeliveryOrderArticle, 1, 2, ndsOrderArticle, 1, 2), 1, 2);

        // без округлений
        sumWithNDSDeliveryOrderArticle = addSUProp(documentPriceGroup, "sumWithNDSDeliveryOrderArticle", "Сумма пост. с НДС", Union.SUM, sumDeliveryOrderArticle, sumNDSDeliveryOrderArticle);
        sumWithoutNDSRetailOrderArticle = addDUProp(documentPriceGroup, "sumWithoutNDSRetailOrderArticle", "Сумма розн. без НДС", sumRetailOrderArticle, sumNDSRetailOrderArticle);
        sumAddvOrderArticle = addJProp(documentPriceGroup, "sumAddvOrderArticle", "Сумма нацен.", diff, sumWithoutNDSRetailOrderArticle, 1, 2, sumDeliveryOrderArticle, 1, 2);
        addvOrderArticle = addJProp(documentPriceGroup, "addvOrderArticle", "Наценка", round0, addJProp(calcPercent, sumAddvOrderArticle, 1, 2, sumDeliveryOrderArticle, 1, 2), 1, 2);

        // с округлениями
        sumRetailOrder = addSGProp(documentPriceGroup, "sumRetailOrder", "Сумма розн.", sumRetailOrderArticle, 1);
        sumNDSRetailOrder = addSGProp(documentPriceGroup, "sumNDSRetailOrder", "Сумма НДС (розн.)", sumNDSRetailOrderArticle, 1);
        sumDeliveryOrder = addSGProp(documentPriceGroup, "sumDeliveryOrder", "Сумма пост.", sumDeliveryOrderArticle, 1);
        sumNDSDeliveryOrder = addSGProp(documentPriceGroup, "sumNDSDeliveryOrder", "Сумма НДС (пост.)", sumNDSDeliveryOrderArticle, 1);

        // без округлений
        sumWithNDSDeliveryOrder = addSUProp(documentPriceGroup, "sumWithNDSDeliveryOrder", "Сумма пост. с НДС", Union.SUM, sumDeliveryOrder, sumNDSDeliveryOrder);
        sumWithoutNDSRetailOrder = addDUProp(documentPriceGroup, "sumWithoutNDSRetailOrder", "Сумма розн. без НДС", sumRetailOrder, sumNDSRetailOrder);
        sumAddvOrder = addJProp(documentPriceGroup, "sumAddvOrder", "Сумма нацен.", diff, sumWithoutNDSRetailOrder, 1, sumDeliveryOrder, 1);
        addvOrder = addJProp(documentPriceGroup, "addvOrder", "Наценка", round0, addJProp(calcPercent, sumAddvOrder, 1, sumDeliveryOrder, 1), 1);

        // переоценка
        revalChangeBalance = addJProp("revalChangeBalance", "Остаток (изм.)", and1, revalBalance, 1, 2, addJProp(diff2, shopPrice, 1, 2, prevPrice, 1, 2), 1, 2);
        sumNewPrevRetailOrderArticle = addJProp("sumNewPrevRetailOrderArticle", "Сумма розн. (нов.)", multiplyDouble2, shopPrice, 1, 2, revalChangeBalance, 1, 2);
        sumPrevRetailOrderArticle = addJProp("sumPrevRetailOrderArticle", "Сумма розн. (пред.)", multiplyDouble2, prevPrice, 1, 2, revalChangeBalance, 1, 2);
        sumPriceChangeOrderArticle = addJProp("sumPriceChangeOrderArticle", "Сумма переоц.", diff, sumNewPrevRetailOrderArticle, 1, 2, sumPrevRetailOrderArticle, 1, 2);
        sumRevalBalance = addSGProp("sumRevalBalance", "Кол-во переоц.", revalChangeBalance, 1);
        sumNewPrevRetailOrder = addSGProp("sumNewPrevRetailOrder", "Сумма розн. (нов.)", sumNewPrevRetailOrderArticle, 1);
        sumPrevRetailOrder = addSGProp("sumPrevRetailOrder", "Сумма розн. (пред.)", sumPrevRetailOrderArticle, 1);
        sumPriceChangeOrder = addJProp("sumPriceChangeOrder", "Сумма переоц.", diff, sumNewPrevRetailOrder, 1, sumPrevRetailOrder, 1);

        orderSaleUseObligation = addDProp(documentPriceGroup, "orderSaleUseObligation", "Использовать", LogicalClass.instance, commitSaleCheckArticleRetail, obligation);
        LP obligationUseSum = addJProp(and1, obligationSum, 2, orderSaleUseObligation, 1, 2);

        obligationDocument = addCGProp(null, "obligationDocument", "Исп. документ", addJProp(and1, 1, orderSaleUseObligation, 1, 2), orderSaleUseObligation, 2);

        LP addDays = addSFProp("prm1+prm2", DateClass.instance, 2);

        couponStart = addDProp(baseGroup, "couponStart", "Дата начала купонов", DateClass.instance);
        LP couponExpiry = addDProp(baseGroup, "couponExpiry", "Дата окончания купонов", DateClass.instance);
        LP certExpiry = addDProp(baseGroup, "certExpiry", "Срок действия серт.", IntegerClass.instance);

        LP dateIssued = addJProp("Дата выдачи", date, obligationIssued, 1);
        couponFromIssued = addDCProp(baseGroup, "couponFromIssued", "Дата начала", couponStart, dateIssued, 1, coupon);
        LP couponToIssued = addDCProp("couponToIssued", "Дата окончания", couponExpiry, obligationIssued, 1, coupon);
        LP certToIssued = addDCProp("certToIssued", "Дата окончания", addJProp(addDays, 1, certExpiry), dateIssued, 1, giftObligation);

        obligationToIssued = addCUProp(baseGroup, "obligationToIssued", "Дата окончания", couponToIssued, certToIssued);
        LP orderSaleObligationAllowed = addJProp(and(false, true, true, true), is(commitSaleCheckArticleRetail), 1, obligationIssued, 2,
                addJProp(less2, orderSalePay, 1, obligationSumFrom, 2), 1, 2,
                addJProp(greater2, date, 1, obligationToIssued, 2), 1, 2,
                addJProp(less2, date, 1, couponFromIssued, 2), 1, 2);
        addConstraint(addJProp("Нельзя использовать выбранный сертификат", andNot1, orderSaleUseObligation, 1, 2, orderSaleObligationAllowed, 1, 2), false);

        LP orderSaleObligationCanBeUsed = addJProp(andNot1, orderSaleObligationAllowed, 1, 2, obligationDocument, 2);
        orderSaleObligationCanNotBeUsed = addJProp(and(false, true), is(commitSaleCheckArticleRetail), 1, is(obligation), 2, orderSaleObligationCanBeUsed, 1, 2);

        LP orderMaxCoupon = addDCProp("orderMaxCoupon", "Макс. процент по купонам", couponMaxPercent, is(orderSaleArticleRetail), 1);

        LP orderSalePayGiftObligation = addSGProp(addJProp(and1, obligationUseSum, 1, 2, is(giftObligation), 2), 1);
        LP orderSalePayCoupon = addJProp(min, addSGProp(addJProp(and1, obligationUseSum, 1, 2, is(coupon), 2), 1), 1, addJProp(percent, orderSalePay, 1, orderMaxCoupon, 1), 1);
        orderSalePayObligation = addSUProp(documentAggrPriceGroup, "Сумма серт.", Union.SUM, orderSalePayGiftObligation, orderSalePayCoupon);

        orderSalePayNoObligation = addJProp(documentAggrPriceGroup, "Сумма к опл.", onlyPositive, addDUProp(orderSalePay, orderSalePayObligation), 1);
        orderArticleSaleSumCoeff = addPGProp(documentPriceGroup, "orderArticleSaleSumCoeff", false, -1, "Сумма со скидкой", orderArticleSaleSumWithDiscount, orderSalePayNoObligation, 1);

        LP clientSaleSum = addSGProp(orderSalePayNoObligation, orderContragent, 1);
        orderClientSaleSum.setDerivedChange(clientSaleSum, orderContragent, 1);
        clientSum = addSUProp(baseGroup, "clientSum", true, "Нак. сумма", Union.SUM, clientSaleSum, clientInitialSum);
        accumulatedClientSum = addJProp("Накопленная сумма", clientSum, orderContragent, 1);

        orderSalePayCash = addDProp(documentPriceGroup, "orderSalePayCash", "Наличными", DoubleClass.instance, orderSaleCheckRetail);
        orderSalePayCard = addDProp(documentPriceGroup, "orderSalePayCard", "Карточкой", DoubleClass.instance, orderSaleCheckRetail);

        impSumCard = addDProp(baseGroup, "inpSumCard", "Безнал. в кассе (ввод)", DoubleClass.instance, DateClass.instance, shop);
        LP curCard = addJProp(cashRegGroup, true, "Безнал. в кассе (ввод)", impSumCard, currentDate, currentShop);
        impSumCash = addDProp(baseGroup, "inpSumCash", "Наличных в кассе (ввод)", DoubleClass.instance, DateClass.instance, shop);
        LP curCash = addJProp(cashRegGroup, true, "Наличных в кассе (ввод)", impSumCash, currentDate, currentShop);
        impSumBank = addDProp(baseGroup, "inpSumBank", "Отправить в банк", DoubleClass.instance, DateClass.instance, shop);
        LP curBank = addJProp(cashRegGroup, true, "Отправить в банк (ввод)", impSumBank, currentDate, currentShop);

        LP allOrderSalePayCard = addSGProp(baseGroup, "Безнал. в кассе", orderSalePayCard, date, 1, outStore, 1);
        LP allOrderSalePayCash = addDUProp(cashRegGroup, "Наличных в кассе", addDUProp(addSGProp(orderSalePayNoObligation, date, 1, outStore, 1), addSGProp(returnSalePay, date, 1, incStore, 1)), allOrderSalePayCard);

        LP allOrderSalePayCardCur = addJProp(cashRegGroup, "allOrderSalePayCardCur", "Безнал. в кассе", allOrderSalePayCard, currentDate, currentShop);
        LP allOrderSalePayCashCur = addJProp(cashRegGroup, "Наличных в кассе", allOrderSalePayCash, currentDate, currentShop);

        // сдача/доплата
        LP orderSalePayAll = addSUProp(Union.SUM, orderSalePayCard, orderSalePayCash);
        LP orderSaleDiffSum = addJProp(and1, addDUProp(orderSalePayAll, orderSalePayNoObligation), 1, is(orderSaleCheckRetail), 1);
        LP notEnoughSum = addJProp(negative, orderSaleDiffSum, 1);
        orderSaleToDo = addJProp(documentAggrPriceGroup, "Необходимо", and1, addIfElseUProp(addCProp(StringClass.get(5), "Итого", orderSaleCheckRetail),
                addCProp(StringClass.get(5), "Сдача", orderSaleCheckRetail), notEnoughSum, 1), 1, orderSaleDiffSum, 1);
        orderSaleToDoSum = addJProp(documentAggrPriceGroup, "Сумма необх.", abs, orderSaleDiffSum, 1);

        addConstraint(addJProp("Сумма наличными меньше сдачи", greater2, orderSalePayCard, 1, orderSalePayNoObligation, 1), false);
        addConstraint(addJProp("Всё оплачено карточкой", and1, addJProp(equals2, orderSalePayCard, 1, orderSalePayNoObligation, 1), 1, orderSalePayCash, 1), false);
        addConstraint(addJProp("Введенной суммы не достаточно", and1, notEnoughSum, 1, orderSalePayAll, 1), false); // если ни карточки ни кэша не задали, значит заплатитли без сдачи

        barcodeAddClient = addSDProp("Доб. клиента", LogicalClass.instance);
        barcodeAddClientAction = addJProp(true, "", andNot1, addBAProp(customerCheckRetail, barcodeAddClient), 1, barcodeToObject, 1);

        barcodeAddCert = addSDProp("Доб. серт.", LogicalClass.instance);
        barcodeAddCertAction = addJProp(true, "", andNot1, addBAProp(giftObligation, barcodeAddCert), 1, barcodeToObject, 1);

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

        LP xorCouponArticleGroup = addDProp(couponGroup, "xorCouponArticleGroup", "Вкл.", LogicalClass.instance, articleGroup);
        xorCouponArticle = addDProp(couponGroup, "xorCouponArticle", "Вкл./искл.", LogicalClass.instance, article);
        inCoupon = addXorUProp(couponGroup, "inCoupon", true, "Выд. купон", xorCouponArticle, addJProp(xorCouponArticleGroup, articleToGroup, 1));

        couponIssueSum = addDProp(couponGroup, "couponIssueSum", "Сумма купона", DoubleClass.instance, DoubleClass.instance);

        LP couponDocToIssueSum = addDCProp("couponDocToIssueSum", "Сумма купона к выд.", addIfProp(addMGProp(addJProp(and1, couponIssueSum, 3, addJProp(greater2, orderSaleDocPrice, 1, 2, 3), 1, 2, 3), 1, 2), false, inCoupon, 2), true, 1, 2, articleQuantity, 1, 2, commitSaleCheckArticleRetail); // здесь конечно хорошо было бы orderSaleDocPrice вытащить за скобки, но будет висячий ключ поэтому приходится пока немого извращаться

        couponToIssueQuantity = addDUProp("К выдаче", addSGProp(articleQuantity, 1, couponDocToIssueSum, 1, 2),
                addSGProp(addJProp(and1, addCProp(DoubleClass.instance, 1), issueCoupon, 1, 2), 1, obligationSum, 2));
        couponToIssueConstraint = addJProp("Кол-во выданных купонов не соответствует требуемому", diff2, couponToIssueQuantity, 1, 2, vzero);
        addConstraint(couponToIssueConstraint, false);

        orderUser = addDCProp("orderUser", "Исп-ль заказа", currentUser, true, is(order), 1);
        orderUserName = addJProp("Исп-ль заказа", name, orderUser, 1);
        // вспомогательные свойства
        orderContragentBarcode = addJProp("Дисконт", barcode, orderContragent, 1);
        orderUserBarcode = addJProp("Кассир", barcode, orderUser, 1);

        orderComputer = addDCProp("orderComputer", "Компьютер заказа", currentComputer, true, is(order), 1);
        orderComputerName = addJProp("Компьютер заказа", hostname, orderComputer, 1);


        setNotNull(addJProp("Штрих-код покупателя", and1, barcode, 1, is(customerCheckRetail), 1));
        //setNotNull(addJProp("Штрих-код товара", and1, barcode, 1, is(article), 1));
        setNotNull(addJProp("Штрих-код сертификата", and1, barcode, 1, is(obligation), 1));
        //setNotNull(addJProp(andNot1, barcode, 1, is(user), 1));

        checkRetailExported = addDProp("checkRetailExported", "Экспортирован", LogicalClass.instance, checkRetail);

        cashRegController = new CashRegController(this); // бред конечно создавать его здесь, но влом создавать getCashRegController()
        cashRegController.addCashRegProperties();

        LP importCustomerCheckRetail = addProp(baseGroup, new CustomerCheckRetailImportActionProperty(this, genSID()));

        quantityCheckCommitInnerArticle = addSDProp("quantityCheckCommitInnerArticle", "Кол-во свер.", DoubleClass.instance, commitInner, article);
        barcodeActionCheck = addJProp(true, "Ввод штрих-кода (проверки)",
                addCUProp(
                        addSCProp(addIfElseUProp(quantityCheckCommitInnerArticle, articleOrderQuantity, is(commitInner), 1))
                ), 1, barcodeToObject, 2);

        quantityDiffCommitArticle = addDUProp(articleOrderQuantity, addCUProp("Кол-во свер.", outerCommitedQuantity, quantityCheckCommitInnerArticle));
    }

    LP barcodeActionCheck, quantityCheckCommitInnerArticle, quantityDiffCommitArticle;
    LP impSumCard;
    LP impSumCash;
    LP impSumBank;

    private LP addSupplierProperty(LP property) {
        return addSUProp(Union.SUM, property, addSGProp(property, articleStoreSupplier, 1, 2, 2));
    }

    LP orderUserName;
    LP orderComputerName;
    LP articleToGroup;
    LP couponToIssueConstraint;
    LP couponIssueSum;
    LP couponToIssueQuantity;
    LP xorCouponArticle;
    LP inCoupon;
    public LP issueObligation;
    public LP obligationSum;
    LP orderSaleCoupon;
    LP barcodeAddClient;
    LP barcodeAddClientAction;
    LP barcodeAddCert;
    LP barcodeAddCertAction;
    LP barcodeAction2;
    LP barcodeAction3;
    LP orderClientSum;
    public LP orderArticleSaleSumWithDiscount;
    public LP orderSaleDocPrice;
    public LP orderSalePrice;
    LP changeQuantityOrder;
    LP computerShop;
    LP currentShop;
    LP panelScreenComPort;
    public LP cashRegComPort;
    LP orderSalePayCash;
    LP orderSalePayCard;
    LP changeQuantityTime;
    LP confirmedInnerQuantity;
    LP couponStart;
    LP obligationDocument;
    LP orderSaleObligationCanNotBeUsed;
    LP orderSaleUseObligation;

    public LP xorActionArticle;
    LP inAction;
    LP orderSalePayObligation;
    LP orderSalePayNoObligation;
    public LP orderArticleSaleSumCoeff;
    public LP clientInitialSum;
    LP clientSum;
    LP accumulatedClientSum;
    public LP incStore;
    LP incStoreName;
    public LP outStore;
    LP outStoreName;

    LP orderContragent;
    LP orderSupplier;

    LP articleFreeOrderQuantity;

    LP articleToGroupName;

    LP articleSupplier;
    LP articleStoreSupplier;
    LP articleStorePeriod;
    LP articleStoreMin;
    LP articleFullStoreDemand;

    LP customerCheckRetailPhone, customerCheckRetailBorn, customerCheckRetailAddress, customerCheckRetailDiscount;

    LP nameContragent, phoneContragent, bornContragent, addressContragent, initialSumContragent;
    LP nameContragentImpl, phoneContragentImpl, bornContragentImpl, addressContragentImpl, initialSumContragentImpl;

//    LP logClientInitialSum;

    LP documentLogisticsSupplied, documentLogisticsRequired, documentLogisticsRecommended;
    LP currentNDSDate, currentNDSDoc, currentNDS, NDS;
    LP ndsOrderArticle;
    LP sumAddvOrderArticle;
    LP sumPriceChangeOrderArticle;
    LP sumRevalBalance;
    LP revalChangeBalance;
    LP sumNewPrevRetailOrderArticle;
    LP sumNewPrevRetailOrder;
    LP sumRetailOrderArticle;
    LP sumPrevRetailOrderArticle;
    LP sumPrevRetailOrder;
    public LP articleQuantity, prevPrice, revalBalance;
    LP articleOrderQuantity;
    LP orderSaleDiscountSum, orderSalePay, orderSaleDiff;
    LP orderSaleToDo, orderSaleToDoSum;
    public LP returnArticleSalePay;
    LP returnSaleDiscount, returnSalePay;
    public LP orderArticleSaleDiscount;
    public LP orderArticleSaleDiscountSum;
    LP returnArticleSaleDiscount;
    LP orderNoDiscount;
    public LP shopPrice;
    LP priceStore, inDocumentPrice;
    LP isRevalued, isNewPrice, documentRevalued;
    LP balanceFormatFreeQuantity;
    LP currentShopPriceDate;
    LP currentShopPriceDoc;
    LP currentShopPrice;
    LP currentRRP;
    LP currencyArticle;
    LP unitOfMeasureArticle;
    LP nameUnitOfMeasureArticle;
    LP returnInnerFreeQuantity;
    LP sumReturnedQuantity;
    LP sumReturnedQuantityFree;

    public LP documentFreeQuantity, documentInnerFreeQuantity, returnFreeQuantity, innerQuantity, returnInnerCommitQuantity, returnInnerQuantity;
    public LP outerOrderQuantity, outerCommitedQuantity, articleBalanceCheck, articleBalanceCheckDB, innerBalanceCheck, innerBalanceCheckDB, balanceSklCommitedQuantity;
    public LP articleInnerQuantity;

    AbstractGroup documentGroup;
    AbstractGroup priceGroup;
    AbstractGroup moveGroup;
    AbstractGroup logisticsGroup;
    AbstractGroup documentMoveGroup;
    AbstractGroup documentPriceGroup, documentAggrPriceGroup;
    AbstractGroup documentLogisticsGroup;
    public AbstractGroup cashRegGroup;
    public AbstractGroup cashRegOperGroup, cashRegAdminGroup;
    AbstractGroup couponGroup;
    AbstractGroup artExtraGroup;

    protected void initGroups() {
        documentGroup = new AbstractGroup("Параметры документа");
        publicGroup.add(documentGroup);

        moveGroup = new AbstractGroup("Движение товаров");
        publicGroup.add(moveGroup);

        documentMoveGroup = new AbstractGroup("Текущие параметры документа");
        documentGroup.add(documentMoveGroup);

        priceGroup = new AbstractGroup("Ценовые параметры");
        publicGroup.add(priceGroup);

        documentPriceGroup = new AbstractGroup("Ценовые параметры документа");
        documentPriceGroup.createContainer = false;
        documentGroup.add(documentPriceGroup);

        documentAggrPriceGroup = new AbstractGroup("Агрегированные ценовые параметры документа");
        documentAggrPriceGroup.createContainer = false;
        documentPriceGroup.add(documentAggrPriceGroup);

        logisticsGroup = new AbstractGroup("Логистические параметры");
        publicGroup.add(logisticsGroup);

        documentLogisticsGroup = new AbstractGroup("Логистические параметры документа");
        documentGroup.add(documentLogisticsGroup);

        cashRegGroup = new AbstractGroup("Операции с ФР");
        cashRegGroup.createContainer = false;
        baseGroup.add(cashRegGroup);

        cashRegOperGroup = new AbstractGroup("Оперативные операции с ФР");
        cashRegGroup.add(cashRegOperGroup);

        cashRegAdminGroup = new AbstractGroup("Административные операции с ФР");
        cashRegGroup.add(cashRegAdminGroup);

        couponGroup = new AbstractGroup("Параметры купона");
        publicGroup.add(couponGroup);

        artExtraGroup = new AbstractGroup("Доп. атрибуты товара");
        publicGroup.add(artExtraGroup);
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
        tableFactory.include("articleaction", article, action);
        tableFactory.include("rates", DateClass.instance);
        tableFactory.include("intervals", DoubleClass.instance);
        tableFactory.include("shoprates", DateClass.instance, shop);
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
            DataSession session = createSession();

            Integer result = (Integer) panelScreenComPort.read(session, new DataObject(compId, computer));

            session.close();

            return result == null ? -1 : result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Integer getCashRegComPort(int compId) {
        try {
            DataSession session = createSession();

            Integer result = (Integer) cashRegComPort.read(session, new DataObject(compId, computer));

            session.close();

            return result == null ? -1 : result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        if (panelScreen.getID() == screenID) {
            return new PanelExternalScreenParameters(getPanelComPort(computerId), getCashRegComPort(computerId));
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

        NavigatorElement print = new NavigatorElement(baseElement, "print", "Печатные формы");
        FormEntity incomePrice = addFormEntity(new IncomePriceFormEntity(print, "incomePrice"));
        FormEntity revalueAct = addFormEntity(new RevalueActFormEntity(print, "revalueAct"));
        FormEntity pricers = addFormEntity(new PricersFormEntity(print, "pricers"));

        NavigatorElement delivery = new NavigatorElement(baseElement, "delivery", "Управление закупками");
            addFormEntity(new SupplierArticleFormEntity(delivery, "supplierArticleForm"));
            NavigatorElement deliveryLocal = new NavigatorElement(delivery, "deliveryLocal", "Закупки у местных поставщиков");
                NavigatorElement deliveryShopLocal = new NavigatorElement(deliveryLocal, "deliveryShopLocal", "Закупки на магазин");
                    FormEntity deliveryCommitShopLocal = addFormEntity(new DeliveryShopLocalFormEntity(deliveryShopLocal, true, "deliveryCommitShopLocal", 1));
                    deliveryCommitShopLocal.caption = "Ввод прихода";
                    FormEntity deliveryOrderShopLocal = addFormEntity(new DeliveryShopLocalFormEntity(deliveryShopLocal, true, "deliveryOrderShopLocal", 0));
                    deliveryOrderShopLocal.caption = "Ввод заявки";
                    FormEntity deliveryShopLocalBrowse = addFormEntity(new DeliveryShopLocalFormEntity(deliveryShopLocal, false, "deliveryShopLocalBrowse", 0));
                    deliveryShopLocalBrowse.caption = "Список документов";
                NavigatorElement deliveryWarehouseLocal = new NavigatorElement(deliveryLocal, "deliveryWarehouseLocal", "Закупки на распред. центр");
                    FormEntity deliveryWarehouseShopLocal = addFormEntity(new DeliveryWarehouseLocalFormEntity(deliveryWarehouseLocal, true, "deliveryWarehouseShopLocal", 1));
                    deliveryWarehouseShopLocal.caption = "Ввод прихода";
                    FormEntity deliveryOrderWarehouseLocal = addFormEntity(new DeliveryWarehouseLocalFormEntity(deliveryWarehouseLocal, true, "deliveryOrderWarehouseLocal", 0));
                    deliveryOrderWarehouseLocal.caption = "Ввод заявки";
                    FormEntity deliveryCommitWarehouseLocal = addFormEntity(new DeliveryWarehouseLocalFormEntity(deliveryWarehouseLocal, false, "deliveryCommitWarehouseLocal", 0));
                    deliveryCommitWarehouseLocal.caption = "Список документов";
                NavigatorElement returnDeliveryLocal = new NavigatorElement(deliveryLocal, "returnDeliveryLocal", "Возвраты поставщику");
                    FormEntity returnCommitDeliveryLocal = addFormEntity(new ReturnDeliveryLocalFormEntity(returnDeliveryLocal, true, "returnCommitDeliveryLocal", 1));
                    returnCommitDeliveryLocal.caption = "Ввод отгрузки";
                    FormEntity returnOrderDeliveryLocal = addFormEntity(new ReturnDeliveryLocalFormEntity(returnDeliveryLocal, true, "returnOrderDeliveryLocal", 0));
                    returnOrderDeliveryLocal.caption = "Ввод заявки";
                    FormEntity returnDeliveryLocalBrowse = addFormEntity(new ReturnDeliveryLocalFormEntity(returnDeliveryLocal, false, "returnDeliveryLocalBrowse", 0));
                    returnDeliveryLocalBrowse.caption = "Список документов";
            NavigatorElement deliveryImport = new NavigatorElement(delivery, "deliveryImport", "Закупки у импортных поставщиков");
                FormEntity deliveryCommitImport = addFormEntity(new DeliveryImportFormEntity(deliveryImport, true, "deliveryCommitImport", 1));
                deliveryCommitImport.caption = "Ввод прихода";
                FormEntity deliveryOrderImport = addFormEntity(new DeliveryImportFormEntity(deliveryImport, true, "deliveryOrderImport", 0));
                deliveryOrderImport.caption = "Ввод заявки";
                FormEntity deliveryImportBrowse = addFormEntity(new DeliveryImportFormEntity(deliveryImport, false, "deliveryImportBrowse", 0));
                deliveryImportBrowse.caption = "Список документов";

        NavigatorElement sale = new NavigatorElement(baseElement, "sale", "Управление продажами");
            NavigatorElement saleRetailElement = new NavigatorElement(sale, "saleRetailElement", "Управление розничными продажами");
                saleRetailCashRegisterElement = new NavigatorElement(saleRetailElement, "saleRetailCashRegisterElement", "Касса");
                    commitSaleForm = addFormEntity(new CommitSaleCheckRetailFormEntity(saleRetailCashRegisterElement, "commitSaleForm", true, false));
                        addFormEntity(new CommitSaleCheckRetailFormEntity(commitSaleForm, "commitSaleCheckRetailForm", false, false));
                        commitSaleBrowseForm = addFormEntity(new CommitSaleCheckRetailFormEntity(commitSaleForm, "commitSaleBrowseForm", false, true));
                        addFormEntity(new CommitSaleCheckRetailExcelFormEntity(commitSaleForm, "commitSaleCheckRetailExcelForm", "Выгрузка в Excel"));
                    saleCheckCertForm = addFormEntity(new SaleCheckCertFormEntity(saleRetailCashRegisterElement, "saleCheckCertForm", true, false));
                        addFormEntity(new SaleCheckCertFormEntity(saleCheckCertForm, "saleCheckCertForm2", false, false));
                        saleCheckCertBrowseForm = addFormEntity(new SaleCheckCertFormEntity(saleCheckCertForm, "saleCheckCertBrowseForm", false, true));
                    returnSaleCheckRetailArticleForm = addFormEntity(new ReturnSaleCheckRetailFormEntity(saleRetailCashRegisterElement, true, "returnSaleCheckRetailArticleForm", false));
                        addFormEntity(new ReturnSaleCheckRetailFormEntity(returnSaleCheckRetailArticleForm, false, "returnSaleCheckRetailArticleForm2", false));
                        returnSaleCheckRetailBrowse = addFormEntity(new ReturnSaleCheckRetailFormEntity(returnSaleCheckRetailArticleForm, false, "returnSaleCheckRetailBrowse", true));
                    cachRegManagementForm = addFormEntity(cashRegController.createCashRegManagementFormEntity(saleRetailCashRegisterElement, "cachRegManagementForm"));
                    addFormEntity(new ShopMoneyFormEntity(saleRetailCashRegisterElement, "shopMoneyForm", "Данные из касс"));
                    addFormEntity(new ClientFormEntity(saleRetailCashRegisterElement, "clientForm", "Редактирование клиентов"));
                NavigatorElement saleRetailInvoice = new NavigatorElement(saleRetailElement, "saleRetailInvoice", "Безналичный расчет");
                    FormEntity saleRetailInvoiceForm = addFormEntity(new OrderSaleInvoiceRetailFormEntity(saleRetailInvoice, "saleRetailInvoiceForm", true, false));
                        addFormEntity(new OrderSaleInvoiceRetailFormEntity(saleRetailInvoiceForm, "orderSaleInvoiceRetailForm", false, false));
                    FormEntity saleInvoiceCert = addFormEntity(new SaleInvoiceCertFormEntity(saleRetailInvoice, "saleInvoiceCert", true, false));
                        addFormEntity(new SaleInvoiceCertFormEntity(saleInvoiceCert, "saleInvoiceCert2", false, false));
                    FormEntity returnSaleInvoiceRetailArticle = addFormEntity(new ReturnSaleInvoiceRetailFormEntity(saleRetailInvoice, true, "returnSaleInvoiceRetailArticle", false));
                        addFormEntity(new ReturnSaleInvoiceRetailFormEntity(returnSaleInvoiceRetailArticle, false, "returnSaleInvoiceRetailArticle2", false));
                        addFormEntity(new ReturnSaleInvoiceRetailFormEntity(returnSaleInvoiceRetailArticle, false, "returnSaleInvoiceRetailArticle3", true));
            NavigatorElement saleWhole = new NavigatorElement(sale, "saleWhole", "Управление оптовыми продажами");
                FormEntity saleWholeForm = addFormEntity(new SaleWholeFormEntity(saleWhole, "saleWholeForm", true));
                    addFormEntity(new SaleWholeFormEntity(saleWholeForm, "saleWholeForm2", false));
                FormEntity returnSaleWholeArticle = addFormEntity(new ReturnSaleWholeFormEntity(saleWhole, "returnSaleWholeArticle", true));
                    addFormEntity(new ReturnSaleWholeFormEntity(returnSaleWholeArticle, "returnSaleWholeArticle2", false));

        NavigatorElement distribute = new NavigatorElement(baseElement, "distribute", "Управление распределением");
            FormEntity distributeShopForm = addFormEntity(new DistributeShopFormEntity(distribute, "distributeShopForm", true));
                FormEntity distributeShopBrowseForm = addFormEntity(new DistributeShopFormEntity(distributeShopForm, "distributeShopBrowseForm", false));
            FormEntity distributeWarehouseForm = addFormEntity(new DistributeWarehouseFormEntity(distribute, "distributeWarehouseForm", true));
                FormEntity distributeWarehouseBrowseForm = addFormEntity(new DistributeWarehouseFormEntity(distributeWarehouseForm, "distributeWarehouseBrowseForm", false));

        NavigatorElement price = new NavigatorElement(baseElement, "price", "Управление ценообразованием");
            FormEntity documentRevalue = addFormEntity(new DocumentRevalueFormEntity(price, true, "documentRevalue"));
                addFormEntity(new DocumentRevalueFormEntity(documentRevalue, false, "documentRevalue2"));

        NavigatorElement toSell = new NavigatorElement(baseElement, "toSell", "Управление ассортиментом");
            addFormEntity(new ArticleFormatFormEntity(toSell, "articleFormatForm", true));
            addFormEntity(new ArticleFormatFormEntity(toSell, "articleFormatForm2", false));

        NavigatorElement tax = new NavigatorElement(baseElement, "tax", "Управление налогами");
            FormEntity nds = addFormEntity(new DocumentNDSFormEntity(tax, true, "nds"));
                addFormEntity(new DocumentNDSFormEntity(nds, false, "nds2"));

        NavigatorElement actions = new NavigatorElement(baseElement, "actions", "Управление акциями");
            FormEntity saleAction = addFormEntity(new ActionFormEntity(actions, "saleAction"));
            FormEntity couponInterval = addFormEntity(new CouponIntervalFormEntity(actions, "couponInterval"));
            FormEntity couponArticle = addFormEntity(new CouponArticleFormEntity(actions, "couponArticle"));

        NavigatorElement balance = new NavigatorElement(baseElement, "balance", "Управление хранением");
            FormEntity balanceCheck = addFormEntity(new BalanceCheckFormEntity(balance, "balanceCheck", true));
                addFormEntity(new BalanceCheckFormEntity(balanceCheck, "balanceCheck2", false));

        NavigatorElement store = new NavigatorElement(baseElement, "store", "Сводная информация");
            addFormEntity(new StoreArticleFormEntity(store, "storeArticleForm"));

        addFormEntity(new GlobalFormEntity(baseElement, "globalForm"));

//        FormEntity logClient = addFormEntity(new LogClientFormEntity(actions, "logClientForm"));

        commitWholeShopInc.addRelevant(incomePrice);
        documentShopPrice.addRelevant(revalueAct);
        documentShopPrice.addRelevant(pricers);
    }

    public static Font FONT_SMALL_BOLD = new Font("Tahoma", Font.BOLD, 12);
    public static Font FONT_SMALL_PLAIN = new Font("Tahoma", Font.PLAIN, 12);
    public static Font FONT_MEDIUM_BOLD = new Font("Tahoma", Font.BOLD, 14);
    public static Font FONT_MEDIUM_PLAIN = new Font("Tahoma", Font.PLAIN, 14);
    public static Font FONT_LARGE_BOLD = new Font("Tahoma", Font.BOLD, 18);
    public static Font FONT_HUGE_BOLD = new Font("Tahoma", Font.BOLD, 23);

    private class GlobalFormEntity extends FormEntity {
        protected GlobalFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Глобальные параметры");
            addPropertyDraw(publicGroup, true);

        }
    }

    private class ClientFormEntity extends FormEntity {

        private ClientFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            GroupObjectEntity gobjFindClient = new GroupObjectEntity(genID());
            ObjectEntity barcodeClient = new ObjectEntity(genID(), StringClass.get(13), "Введите штрих код");
            ObjectEntity nameClient = new ObjectEntity(genID(), StringClass.get(100), "Введите ФИО");

            gobjFindClient.add(barcodeClient);
            gobjFindClient.add(nameClient);

            addGroup(gobjFindClient);

            gobjFindClient.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
            gobjFindClient.initClassView = ClassViewType.PANEL;

            addPropertyDraw(barcodeClient, objectValue);
            addPropertyDraw(nameClient, objectValue);

            ObjectEntity objClient = addSingleGroupObject(customerCheckRetail, "Клиент");
            ObjectEntity objDoc = addSingleGroupObject(commitSaleCheckArticleRetail, "Чек");

            addPropertyDraw(objClient, barcode, name, customerCheckRetailPhone, customerCheckRetailBorn, customerCheckRetailAddress, customerCheckRetailDiscount, clientInitialSum, clientSum);
            addPropertyDraw(objDoc, objectValue, date, orderHour, outStore, orderSalePayNoObligation);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(barcode, objClient), Compare.EQUALS, addPropertyObject(orderContragentBarcode, objDoc)));

            //addFixedFilter(new NotNullFilterEntity(addPropertyObject(barcode, objClient)));

            addFixedFilter(new OrFilterEntity(
                    new CompareFilterEntity(addPropertyObject(barcode, objClient), Compare.EQUALS, barcodeClient),
                    new CompareFilterEntity(addPropertyObject(name, objClient), Compare.EQUALS, nameClient)));
        }


        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }
    }

    private class BarcodeFormEntity extends FormEntity<VEDBusinessLogics> {

        ObjectEntity objBarcode;

        protected Font getDefaultFont() {
            return null;
        }

        private BarcodeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseGroup, true);
            objBarcode.groupTo.initClassView = ClassViewType.PANEL;
            objBarcode.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));

            objBarcode.resetOnApply = true;

            addPropertyDraw(reverseBarcode);

//            addAutoAction(objBarcode, addPropertyObject(barcodeAction, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            if (getDefaultFont() != null)
                design.setFont(getDefaultFont());

            PropertyDrawView barcodeView = design.get(getPropertyDraw(objectValue, objBarcode));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(reverseBarcode)));
            design.getPanelContainer(design.get(objBarcode.groupTo)).constraints.maxVariables = 0;
            design.addIntersection(barcodeView, design.get(getPropertyDraw(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(barcodeView, design.get(getPropertyDraw(reverseBarcode)), DoNotIntersectSimplexConstraint.TOTHE_BOTTOM);
            design.addIntersection(design.get(getPropertyDraw(reverseBarcode)), design.get(getPropertyDraw(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            if (getPropertyDraw(documentBarcodePriceOv) != null) {
                design.addIntersection(design.get(getPropertyDraw(barcodeObjectName)), design.get(getPropertyDraw(documentBarcodePriceOv)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }

            design.setFont(barcodeView, FONT_LARGE_BOLD);
            design.setFont(reverseBarcode, FONT_SMALL_BOLD);
            design.setFont(barcodeAddClient, FONT_SMALL_BOLD);
            design.setFont(barcodeAddCert, FONT_SMALL_BOLD);
            design.setFont(barcodeObjectName, FONT_LARGE_BOLD);
            design.setFont(documentBarcodePriceOv, FONT_LARGE_BOLD);
            design.setBackground(barcodeObjectName, new Color(240, 240, 240));

            design.setEditKey(barcodeView, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
            design.setEditKey(reverseBarcode, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

            design.setFocusable(reverseBarcode, false);
            design.setFocusable(false, objBarcode.groupTo);

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

        protected DocumentFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd) {
            super(parent, sID, (toAdd ? documentClass.caption : "Документы"));

            this.toAdd = toAdd;

            objDoc = addSingleGroupObject(documentClass, getDocumentProps());


            if (toAdd) {
                objDoc.groupTo.initClassView = ClassViewType.PANEL;
                objDoc.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
                objDoc.addOnTransaction = true;
            } else {
                addPropertyDraw(orderUserName, objDoc);
                if (!isReadOnly())
                    addObjectActions(this, objDoc);
            }

            addAutoAction(objBarcode, addPropertyObject(barcodeAction2, objDoc, objBarcode));
            addAutoAction(objBarcode, addPropertyObject(seekBarcodeAction, objBarcode));
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
                    docSumsContainer.getContainer().setSID(null); // сделано, чтобы при визуальной настройке она сразу не перетаскивала все свойства в другой контейнер при помощи ContainerMover
                    design.mainContainer.addBack(2, docSumsContainer);

                    PropertyDrawEntity payCash = getPropertyDraw(orderSalePayCash);
                    PropertyDrawEntity payCard = getPropertyDraw(orderSalePayCard);
                    PropertyDrawEntity toDo = getPropertyDraw(orderSaleToDo);
                    PropertyDrawEntity toDoSum = getPropertyDraw(orderSaleToDoSum);

                    if (payCash != null || payCard != null || toDo != null || toDoSum != null) {

                        ContainerView payContainer = design.createContainer("Платежные средства");
                        design.mainContainer.addBack(2, payContainer);
                        payContainer.constraints.directions = new SimplexComponentDirections(0.1, -0.1, 0, 0.1);

                        if (payCash != null) payContainer.add(design.get(payCash));
                        if (payCard != null) payContainer.add(design.get(payCard));
                        if (toDo != null) {
                            payContainer.add(design.get(toDo));
                            design.get(toDo).design.background = Color.yellow;
                        }
                        if (toDoSum != null) {
                            payContainer.add(design.get(toDoSum));
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
        public RegularFilterGroupEntity articleFilterGroup;
        public RegularFilterEntity documentFilter;

        protected abstract PropertyObjectEntity getCommitedQuantity();

        protected Object[] getArticleProps() {
            return new Object[]{baseGroup, true};
        }

        protected Object[] getDocumentArticleProps() {
            return new Object[]{baseGroup, true, documentGroup, true};
        }

        protected ArticleFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, sID, documentClass, toAdd);

            objArt = addSingleGroupObject(article, getArticleProps());

            addPropertyDraw(objDoc, objArt, getDocumentArticleProps());

            articleFilterGroup = new RegularFilterGroupEntity(genID());
            documentFilter = new RegularFilterEntity(genID(),
                    getDocumentArticleFilter(),
                    "Документ",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));
            articleFilterGroup.addFilter(documentFilter, !toAdd || filled);
            fillExtraFilters(articleFilterGroup, toAdd && !filled);
            addRegularFilterGroup(articleFilterGroup);

//            addHintsNoUpdate(properties, moveGroup);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();
            design.get(objArt.groupTo).grid.constraints.fillVertical = 3;

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F2, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }

        protected ArticleFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd) {
            this(parent, sID, documentClass, toAdd, false);
        }

        protected abstract FilterEntity getDocumentArticleFilter();

        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
        }

        // такое дебильное множественное наследование
        public void fillExtraLogisticsFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(documentLogisticsSupplied, objDoc, objArt)),
                    "Поставляется",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), toAdd);
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(documentLogisticsRequired, objDoc, objArt)),
                    "Необходимо",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
        }

        protected void addCheckFilter(RegularFilterGroupEntity filterGroup, boolean setDefault) {
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(quantityDiffCommitArticle, objDoc, objArt), Compare.NOT_EQUALS, 0.0),
                    "Отличается от заказа",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)), setDefault);
        }

        protected void fillExtraCheckFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            if(!toAdd)
                addCheckFilter(filterGroup, false);
        }

    }

    // для те которые не различают заказано и выполнено
    private abstract class ArticleNoCheckFormEntity extends ArticleFormEntity {

        protected FilterEntity getDocumentArticleFilter() {
            return new NotNullFilterEntity(getCommitedQuantity());
        }

        protected ArticleNoCheckFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd) {
            super(parent, sID, documentClass, toAdd);
        }

        protected ArticleNoCheckFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, sID, documentClass, toAdd, filled);
        }
    }

    private abstract class InnerFormEntity extends ArticleNoCheckFormEntity {

        public RegularFilterEntity availableFilter;

        protected PropertyObjectEntity getCommitedQuantity() {
            return addPropertyObject(articleInnerQuantity, objDoc, objArt);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            if (!fixFilters) {
                availableFilter = new RegularFilterEntity(genID(),
                        new NotNullFilterEntity(addPropertyObject(documentFreeQuantity, objDoc, objArt)),
                        "Дост. кол-во",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
                filterGroup.addFilter(availableFilter, toAdd);
            }
        }

        protected InnerFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, boolean filled) {
            super(parent, sID, documentClass, toAdd, filled);

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

        protected OuterFormEntity(NavigatorElement parent, String sID, boolean toAdd, int concrete, CustomClass orderClass, CustomClass commitClass) {
            super(parent, sID, concrete==0?orderClass:commitClass, toAdd);

            if(toAdd && concrete!=0)
                addCheckFilter(articleFilterGroup, true);
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            fillExtraLogisticsFilters(filterGroup, toAdd);

            fillExtraCheckFilters(filterGroup, toAdd);
        }

        @Override
        protected Object[] getDocumentProps() {
            return BaseUtils.add(super.getDocumentProps(), importOrder);
        }

        @Override
        protected Object[] getDocumentArticleProps() {
            return new Object[]{outerOrderQuantity, outerCommitedQuantity, orderAllDeliveryPrice, shopPrice, prevPrice, revalBalance, sumDeliveryOrderArticle, ndsOrderArticle, sumNDSDeliveryOrderArticle, sumWithNDSDeliveryOrderArticle, addvOrderArticle, sumAddvOrderArticle, sumWithoutNDSRetailOrderArticle, sumNDSRetailOrderArticle, sumRetailOrderArticle, documentLogisticsGroup, true};
        }
    }

    private class DeliveryShopLocalFormEntity extends OuterFormEntity {
        public DeliveryShopLocalFormEntity(NavigatorElement parent, boolean toAdd, String sID, int concrete) {
            super(parent, sID, toAdd, concrete, orderDeliveryShopLocal, commitDeliveryShopLocal);
        }
    }

    private class DeliveryWarehouseLocalFormEntity extends OuterFormEntity {
        public DeliveryWarehouseLocalFormEntity(NavigatorElement parent, boolean toAdd, String sID, int concrete) {
            super(parent, sID, toAdd, concrete, orderDeliveryWarehouseLocal, commitDeliveryWarehouseLocal);
        }
    }

    private class DeliveryImportFormEntity extends OuterFormEntity {
        public DeliveryImportFormEntity(NavigatorElement parent, boolean toAdd, String sID, int concrete) {
            super(parent, sID, toAdd, concrete, orderDeliveryImport, commitDeliveryImport);
        }
    }

    private class ArticleOuterFormEntity extends InnerFormEntity {
        ObjectEntity objOuter;

        protected ArticleOuterFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, CustomClass commitClass, boolean filled) {
            super(parent, sID, documentClass, toAdd, filled);

            if (!noOuters) {
                objOuter = addSingleGroupObject(commitClass, baseGroup, true);
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
        public ReturnDeliveryLocalFormEntity(NavigatorElement parent, boolean toAdd, String sID, int concrete) {
            super(parent, sID, concrete==0?orderReturnDeliveryLocal:commitReturnDeliveryLocal, toAdd, commitDeliveryLocal, false);
        }
    }

    private class ArticleInnerFormEntity extends ArticleOuterFormEntity {

        protected ArticleInnerFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass, boolean filled) {
            super(parent, sID, documentClass, toAdd, commitDelivery, filled);
        }
    }

    private class DocumentInnerFormEntity extends ArticleInnerFormEntity {

        protected DocumentInnerFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass, boolean filled) {
            super(parent, sID, toAdd, documentClass, filled);
        }
    }

    private class SaleWholeFormEntity extends DocumentInnerFormEntity {
        public SaleWholeFormEntity(NavigatorElement parent, String sID, boolean toAdd) {
            super(parent, sID, toAdd, orderSaleWhole, false);
        }
    }

    public class ShopMoneyFormEntity extends FormEntity {
        public ShopMoneyFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objShop = addSingleGroupObject(shop, baseGroup);

            GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
            ObjectEntity objDateFrom = new ObjectEntity(genID(), DateClass.instance, "Выгрузить от");
            ObjectEntity objDateTo = new ObjectEntity(genID(), DateClass.instance, "Выгрузить до");
            gobjDates.add(objDateFrom);
            gobjDates.add(objDateTo);
            addGroup(gobjDates);

            gobjDates.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
            gobjDates.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objDateFrom, objectValue);
            addPropertyDraw(objDateTo, objectValue);
            addPropertyDraw(saleExport, objShop, objDateFrom, objDateTo);
        }
    }

    private abstract class SaleRetailFormEntity extends DocumentInnerFormEntity {

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{nameContragentImpl, phoneContragentImpl, bornContragentImpl, addressContragentImpl, initialSumContragentImpl, orderClientSum,
                    orderSalePay, orderSaleDiscountSum, orderSalePayObligation, orderSalePayNoObligation, orderSalePayCash, orderSalePayCard, orderSaleToDo, orderSaleToDoSum, orderBirthDay, orderNoDiscount, payWithCard, printOrderCheck};
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
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        protected SaleRetailFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, boolean allStores) {
            super(parent, sID, toAdd, documentClass, true);

            if (!toAdd)
                addPropertyDraw(date, objDoc);

            if (allStores) {
                addPropertyDraw(objDoc, outStore, outStoreName);
                caption = caption + " (все склады)";
            } else {
                PropertyObjectEntity shopImplement = addPropertyObject(currentShop);
                addFixedFilter(new CompareFilterEntity(addPropertyObject(outStore, objDoc), Compare.EQUALS, shopImplement));
            }

            // чтобы в порядке нужном
            addPropertyDraw(changeQuantityOrder, objDoc, objArt);
            addPropertyDraw(barcode, objArt);

            objArt.groupTo.filterProperty = addPropertyDraw(name, objArt);

            addPropertyDraw(articleQuantity, objDoc, objArt); // для timeChanges иначе можно было бы articleQuantity
            addPropertyDraw(documentFreeQuantity, objDoc, objArt);
            addPropertyDraw(orderSalePrice, objDoc, objArt);
            addPropertyDraw(orderArticleSaleDiscount, objDoc, objArt);
            addPropertyDraw(orderArticleSaleDiscountSum, objDoc, objArt);
            addPropertyDraw(orderArticleSaleSumWithDiscount, objDoc, objArt);
            //addPropertyDraw(documentBarcodePriceOv, objDoc, objBarcode);
            if (!toAdd) {
                addPropertyDraw(orderArticleSaleSumCoeff, objDoc, objArt);
            }
            //getPropertyDraw(documentBarcodePriceOv).setToDraw(objBarcode.groupTo);

            objArt.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));

            addFixedOrder(addPropertyObject(changeQuantityTime, objDoc, objArt), false);

            addPropertyDraw(barcodeAddClient);
            addAutoAction(objBarcode, true,
                    addPropertyObject(barcodeAddClientAction, objBarcode),
                    addPropertyObject(barcodeAction2, objDoc, objBarcode),
                    addPropertyObject(seekBarcodeAction, objBarcode),
                    addPropertyObject(barcodeNotFoundMessage, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();
            design.setEnabled(orderClientSum, false);

            design.get(objDoc.groupTo).grid.constraints.fillVertical = 2;

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);

            design.setEnabled(publicGroup, false, objArt.groupTo);
            design.setEnabled(articleQuantity, true, objArt.groupTo);

            design.get(objArt.groupTo).grid.defaultComponent = true;

            ObjectView objArtView = design.get(objArt);
            objArtView.classChooser.show = false;

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(barcodeAddClient)));
            design.addIntersection(design.get(getPropertyDraw(barcodeAddClient)), design.get(getPropertyDraw(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.setEditKey(barcodeAddClient, KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK));

            design.get(getPropertyDraw(changeQuantityOrder, objArt)).minimumSize = new Dimension(20, 20);
            design.get(getPropertyDraw(articleQuantity, objArt)).minimumSize = new Dimension(20, 20);
            design.get(getPropertyDraw(documentFreeQuantity, objArt)).minimumSize = new Dimension(90, 20);
            design.get(getPropertyDraw(orderArticleSaleDiscount, objArt)).minimumSize = new Dimension(20, 20);
            getPropertyDraw(orderArticleSaleDiscountSum, objArt).forceViewType = ClassViewType.HIDE;
            design.get(getPropertyDraw(barcode, objArt)).minimumSize = new Dimension(200, 100);

            return design;
        }
    }

    public class CommitSaleCheckRetailFormEntity extends SaleRetailFormEntity {

        public ObjectEntity objObligation;
        private ObjectEntity objCoupon;
        private ObjectEntity objIssue;

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        private CommitSaleCheckRetailFormEntity(NavigatorElement parent, String sID, boolean toAdd, boolean allStores) {
            super(parent, sID, commitSaleCheckArticleRetail, toAdd, allStores);

            objDoc.caption = "Чек";

            objObligation = addSingleGroupObject(obligation, "Оплачено купонами/ сертификатами");
            addPropertyDraw(barcode, objObligation);
            addPropertyDraw(objectClassName, objObligation);
            addPropertyDraw(obligationSum, objObligation);
            addPropertyDraw(obligationSumFrom, objObligation);
            addPropertyDraw(couponFromIssued, objObligation).forceViewType = ClassViewType.GRID;
            addPropertyDraw(obligationToIssued, objObligation);
            addPropertyDraw(orderSaleUseObligation, objDoc, objObligation);

            if (toAdd) {
                objArt.groupTo.propertyHighlight = addPropertyObject(articleSaleAction, objArt);

                objObligation.groupTo.propertyHighlight = addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation);
                addHintsNoUpdate(obligationDocument);
            }

            objObligation.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.HIDE, ClassViewType.PANEL));
//            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(obligationDocument, objObligation))));
//            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleUseObligation, objDoc, objObligation)));

            objCoupon = addSingleGroupObject(coupon, "Выдано купонов", baseGroup, true);
            addPropertyDraw(objDoc, objCoupon, baseGroup, true, issueObligation);
            objCoupon.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.HIDE, ClassViewType.PANEL));
//            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(obligationDocument, objObligation))));
//            addFixedFilter(new NotNullFilterEntity(addPropertyObject(orderSaleObligationCanNotBeUsed, objDoc, objObligation)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(issueObligation, objDoc, objCoupon)));

            if (toAdd) {
                objIssue = addSingleGroupObject(DoubleClass.instance, "Выдать купоны", baseGroup, true);
                addPropertyDraw(couponToIssueQuantity, objDoc, objIssue);
                objIssue.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.HIDE, ClassViewType.PANEL));
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(couponToIssueConstraint, objDoc, objIssue)));

                addPropertyDraw(cashRegOperGroup, true);
            } else {
                addPropertyDraw(checkRetailExported, objDoc);
            }
            addPropertyDraw(orderUserName, objDoc);
            getPropertyDraw(orderUserName).forceViewType = ClassViewType.HIDE;
            addPropertyDraw(accumulatedClientSum, objDoc);
            getPropertyDraw(accumulatedClientSum).forceViewType = ClassViewType.HIDE;
        }

        @Override
        protected Font getDefaultFont() {
            if (toAdd)
                return FONT_MEDIUM_BOLD;
            else
                return super.getDefaultFont();
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

                design.getGroupPropertyContainer(null, cashRegOperGroup).add(0, design.get(getPropertyDraw(payWithCard)));
                design.getGroupPropertyContainer(null, cashRegOperGroup).add(1, design.get(getPropertyDraw(printOrderCheck)));

                design.get(getPropertyDraw(printOrderCheck)).constraints.insetsSibling.right = 100;

                design.get(objObligation.groupTo).highlightColor = new Color(255, 0, 0);
            }

            design.get(objCoupon.groupTo).grid.minRowCount = 2;
            design.get(objObligation.groupTo).grid.minRowCount = 2;
            design.get(objCoupon.groupTo).grid.showFilter = false;
            design.get(objObligation.groupTo).grid.showFilter = false;
            design.get(objCoupon.groupTo).grid.autoHide = true;
            design.addIntersection(design.getGroupObjectContainer(objCoupon.groupTo), design.getGroupObjectContainer(objObligation.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            if (toAdd) {
                design.setHeaderFont(FONT_MEDIUM_PLAIN);
                design.setFont(FONT_LARGE_BOLD, objArt.groupTo);
            }

            ObjectView objCouponView = design.get(objCoupon);
            objCouponView.classChooser.show = false;

            ObjectView objObligationView = design.get(objObligation);
            objObligationView.classChooser.show = false;

            design.get(getPropertyDraw(objectClassName, objObligation)).maximumSize = new Dimension(50, 50);

            if (documentFilter != null) {
                RegularFilterView docFilterView = design.get(articleFilterGroup).get(documentFilter);
                RegularFilterView availableFilterView = design.get(articleFilterGroup).get(availableFilter);

                //todo: проставить нужные порядки
//                docFilterView.orders.put(design.getProperty(getPropertyDraw(articleQuantity)), true);
//                availableFilterView.orders.put(design.getProperty(getPropertyDraw(documentFreeQuantity)), true);
//                design.get(articleFilterGroup).nullOrders.put(design.getProperty(getPropertyDraw(documentFreeQuantity)), false);
            }

            return design;
        }

        @Override
        public boolean hasClientApply() {
            return toAdd;
        }

        @Override
        public ClientResultAction getClientApply(FormInstance<VEDBusinessLogics> formInstance) {
            if (toAdd) {

                ObjectInstance art = formInstance.instanceFactory.getInstance(objArt);
                ObjectInstance obligation = formInstance.instanceFactory.getInstance(objObligation);

                return cashRegController.getCashRegApplyActions(formInstance, 1,
                        BaseUtils.toSet(art.groupTo),
                        getPropertyDraw(orderSalePrice, objArt), getPropertyDraw(articleQuantity, objArt),
                        getPropertyDraw(name, objArt), getPropertyDraw(orderArticleSaleSumWithDiscount, objArt),
                        getPropertyDraw(orderSalePayNoObligation, objDoc), getPropertyDraw(barcode, objArt),
                        getPropertyDraw(orderSalePayCard, objDoc), getPropertyDraw(orderSalePayCash, objDoc),
                        getPropertyDraw(orderArticleSaleDiscount), getPropertyDraw(orderArticleSaleDiscountSum), getPropertyDraw(orderUserName),
                        getPropertyDraw(nameContragentImpl), getPropertyDraw(accumulatedClientSum), getPropertyDraw(orderSaleDiscountSum, objDoc),
                        BaseUtils.toSet(obligation.groupTo),getPropertyDraw(objectClassName, objObligation), getPropertyDraw(obligationSum, objObligation), getPropertyDraw(barcode, objObligation));
            } else
                return super.getClientApply(formInstance);
        }

        public ClientAction getPrintOrderAction(FormInstance<VEDBusinessLogics> formInstance) {
            if (toAdd) {

                ObjectInstance art = formInstance.instanceFactory.getInstance(objArt);

                return cashRegController.getPrintOrderAction(formInstance,
                        BaseUtils.toSet(art.groupTo),
                        getPropertyDraw(orderSalePrice, objArt), getPropertyDraw(articleQuantity, objArt),
                        getPropertyDraw(name, objArt), getPropertyDraw(orderArticleSaleSumWithDiscount, objArt),
                        getPropertyDraw(orderSalePayNoObligation, objDoc), getPropertyDraw(barcode, objArt));
            } else
                return new ListClientAction(new ArrayList<ClientAction>());
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


    public class CommitSaleCheckRetailExcelFormEntity extends FormEntity {

        public CommitSaleCheckRetailExcelFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            // форма по проданным товарам (цена, стоимость, скидки, данные по продаже)
            GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
            ObjectEntity dateFrom = new ObjectEntity(genID(), DateClass.instance, "Дата (с)");
            ObjectEntity dateTo = new ObjectEntity(genID(), DateClass.instance, "Дата (по)");
            gobjDates.add(dateFrom);
            gobjDates.add(dateTo);

            addGroup(gobjDates);
            gobjDates.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));
            gobjDates.initClassView = ClassViewType.PANEL;

            addPropertyDraw(dateFrom, objectValue);
            addPropertyDraw(dateTo, objectValue);

            GroupObjectEntity gobjDocArt = new GroupObjectEntity(genID());

            ObjectEntity objDoc = new ObjectEntity(genID(), commitSaleCheckArticleRetail, "Чек");
            ObjectEntity objArt = new ObjectEntity(genID(), article, "Товар");

            gobjDocArt.add(objDoc);
            gobjDocArt.add(objArt);
            addGroup(gobjDocArt);

            addPropertyDraw(objArt, barcode, name);
            addPropertyDraw(objDoc, objArt, orderSalePrice, articleQuantity);
            addPropertyDraw(objDoc, objectValue, date, orderHour, orderUserBarcode, orderComputerName, outStore, orderContragentBarcode);
            addPropertyDraw(objDoc, objArt, orderArticleSaleDiscountSum, orderArticleSaleDiscount, orderArticleSaleSumWithDiscount, orderArticleSaleSumCoeff);


            removePropertyDraw(documentMoveGroup); // нужно, чтобы убрать Доступ. кол-во, которое не может нормально выполнить PostgreSQL

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(articleQuantity, objDoc, objArt)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objDoc), Compare.GREATER_EQUALS, dateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objDoc), Compare.LESS_EQUALS, dateTo));
        }


        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.overridePageWidth = 3000;
            return design;
        }
    }

    private class OrderSaleInvoiceRetailFormEntity extends SaleRetailFormEntity {

        private OrderSaleInvoiceRetailFormEntity(NavigatorElement parent, String sID, boolean toAdd, boolean allStores) {
            super(parent, sID, orderSaleInvoiceArticleRetail, toAdd, allStores);
        }

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{nameContragent, phoneContragent, bornContragent, addressContragent, initialSumContragent, orderClientSum,
                    orderSalePay, orderSaleDiscountSum, orderSalePayNoObligation, orderSalePayCash, orderSalePayCard, orderSaleToDo, invoiceNumber, invoiceSeries};
        }
    }

    private class DistributeFormEntity extends DocumentInnerFormEntity {
        public DistributeFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass) {
            super(parent, sID, toAdd, documentClass, false);

            addPropertyDraw(objDoc, objArt, quantityCheckCommitInnerArticle);

            if (!toAdd)
                addAutoAction(objBarcode, true,
                        addPropertyObject(barcodeActionCheck, objDoc, objBarcode),
                        addPropertyObject(seekBarcodeAction, objBarcode),
                        addPropertyObject(barcodeNotFoundMessage, objBarcode));
        }

        @Override
        protected void fillExtraFilters(RegularFilterGroupEntity filterGroup, boolean toAdd) {
            fillExtraLogisticsFilters(filterGroup, toAdd);

            fillExtraCheckFilters(filterGroup, toAdd);
        }
    }

    private class DistributeShopFormEntity extends DistributeFormEntity {
        public DistributeShopFormEntity(NavigatorElement parent, String sID, boolean toAdd) {
            super(parent, sID, toAdd, orderDistributeShop);
        }
    }

    private class DistributeWarehouseFormEntity extends DistributeFormEntity {
        public DistributeWarehouseFormEntity(NavigatorElement parent, String sID, boolean toAdd) {
            super(parent, sID, toAdd, orderDistributeWarehouse);
        }
    }

    private class BalanceCheckFormEntity extends DocumentInnerFormEntity {
        public BalanceCheckFormEntity(NavigatorElement parent, String sID, boolean toAdd) {
            super(parent, sID, toAdd, balanceCheck, false);
        }
    }

    private class ReturnSaleFormEntity extends DocumentFormEntity {

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{returnSaleDiscount, returnSalePay};
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

        protected ReturnSaleFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass, CustomClass commitClass) {
            super(parent, sID, documentClass, toAdd);

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

            objArt = addSingleGroupObject(article);

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

            if (!noOuters) {
                objOuter = addSingleGroupObject(commitDelivery, baseGroup, true);

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
                    addPropertyObject(seekBarcodeAction, objBarcode),
                    addPropertyObject(barcodeNotFoundMessage, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            if (toAdd) {

                // делаем, чтобы суммы были внизу и как можно правее
                design.mainContainer.addBack(2, design.get(getPropertyDraw(returnSalePay)).getContainer());
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
        private ReturnSaleWholeFormEntity(NavigatorElement parent, String sID, boolean toAdd) {
            super(parent, sID, toAdd, returnSaleWhole, commitSaleWhole);
        }
    }

    public class ReturnSaleRetailFormEntity extends ReturnSaleFormEntity {

        public ReturnSaleRetailFormEntity(NavigatorElement parent, String sID, boolean toAdd, CustomClass documentClass, CustomClass commitClass, boolean allStores) {
            super(parent, sID, toAdd, documentClass, commitClass);

            if (allStores) {
                addPropertyDraw(objDoc, incStore, incStoreName);
                caption = caption + " (все склады)";
            } else {
                PropertyObjectEntity shopImplement = addPropertyObject(currentShop);
                addFixedFilter(new CompareFilterEntity(addPropertyObject(incStore, objDoc), Compare.EQUALS, shopImplement));
            }
        }
    }

    private class ReturnSaleInvoiceRetailFormEntity extends ReturnSaleRetailFormEntity {
        private ReturnSaleInvoiceRetailFormEntity(NavigatorElement parent, boolean toAdd, String sID, boolean allStores) {
            super(parent, sID, toAdd, returnSaleInvoiceRetail, commitSaleInvoiceArticleRetail, allStores);

            setReadOnly(true, objInner.groupTo);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);

            return design;
        }


        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{returnSaleDiscount, returnSalePay, nameContragent, invoiceNumber, invoiceSeries};
        }
    }

    public class ReturnSaleCheckRetailFormEntity extends ReturnSaleRetailFormEntity {

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        @Override
        protected String getReturnCaption() {
            return "Номер чека";
        }

        private ReturnSaleCheckRetailFormEntity(NavigatorElement parent, boolean toAdd, String sID, boolean allStores) {
            super(parent, sID, toAdd, returnSaleCheckRetail, commitSaleCheckArticleRetail, allStores);

            objDoc.caption = "Возвратный чек";

            if (toAdd) {
                addPropertyDraw(cashRegOperGroup, true);
            } else {
                addPropertyDraw(date, objDoc);
                addPropertyDraw(checkRetailExported, objDoc);
            }

            addPropertyDraw(documentBarcodePriceOv, objInner, objBarcode).setToDraw(objBarcode.groupTo);
            addPropertyDraw(orderUserName, objDoc);
            getPropertyDraw(orderUserName).forceViewType = ClassViewType.HIDE;
            //addPropertyDraw(returnArticleSalePay, objArt);

            setReadOnly(true, objInner.groupTo);

            PropertyObjectEntity shopImplement = addPropertyObject(currentShop);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outStore, objInner), Compare.EQUALS, shopImplement));
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

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            return design;
        }

        @Override
        public boolean hasClientApply() {
            return toAdd;
        }

        @Override
        public ClientResultAction getClientApply(FormInstance formInstance) {
            if (toAdd) {

                ObjectInstance doc = formInstance.instanceFactory.getInstance(objDoc);
                ObjectInstance inner = formInstance.instanceFactory.getInstance(objInner);
                ObjectInstance art = formInstance.instanceFactory.getInstance(objArt);

                return cashRegController.getCashRegApplyActions(formInstance, 2,
                        BaseUtils.toSet(inner.groupTo, art.groupTo),
                        getPropertyDraw(orderSalePrice, objArt), getPropertyDraw(returnInnerQuantity, objArt),
                        getPropertyDraw(name, objArt), getPropertyDraw(returnArticleSalePay, objArt),
                        getPropertyDraw(returnSalePay, objDoc), getPropertyDraw(barcode, objArt), null, null,
                        getPropertyDraw(orderArticleSaleDiscount), getPropertyDraw(returnArticleSaleDiscount),
                        getPropertyDraw(orderUserName), null, null, null, null, null, null, null);
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
        protected SupplierArticleFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Ассортимент поставщиков");

            ObjectEntity objSupplier = addSingleGroupObject(supplier, publicGroup, true);
            addObjectActions(this, objSupplier);

            ObjectEntity objArt = addSingleGroupObject(article, baseGroup, true, priceGroup, logisticsGroup);
            addObjectActions(this, objArt);

            addPropertyDraw(objSupplier, objArt, publicGroup, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleSupplier, objArt), Compare.EQUALS, objSupplier));
        }
    }

    private class StoreArticleFormEntity extends BarcodeFormEntity {
        private ObjectEntity objArt;

        protected StoreArticleFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Остатки по складу");

            ObjectEntity objStore = addSingleGroupObject(store, publicGroup, true);
            objArt = addSingleGroupObject(article, publicGroup, true);
            ObjectEntity objOuter = addSingleGroupObject(commitDelivery, publicGroup, true);

            addPropertyDraw(objStore, objArt, publicGroup, true);
            addPropertyDraw(objStore, objOuter, publicGroup, true);
            addPropertyDraw(objOuter, objArt, baseGroup, true);
            addPropertyDraw(objStore, objOuter, objArt, publicGroup, true);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView form = super.createDefaultRichDesign();
            form.get(objArt.groupTo).grid.constraints.fillVertical = 3;
            return form;
        }
    }

    private class ArticleFormatFormEntity extends BarcodeFormEntity {
        protected ArticleFormatFormEntity(NavigatorElement parent, String sID, boolean splitGroup) {
            super(parent, sID, "Определение ассортимента" + (splitGroup?"":" (в таблице)"));

            ObjectEntity objFormat, objArt;
            if (splitGroup) {
                objArt = addSingleGroupObject(article);
                objFormat = addSingleGroupObject(format);
            } else {
                GroupObjectEntity gobjFormatArt = new GroupObjectEntity(genID());
                objFormat = new ObjectEntity(genID(), format, "Формат");
                objArt = new ObjectEntity(genID(), article, "Товар");
                gobjFormatArt.add(objFormat);
                gobjFormatArt.add(objArt);
                addGroup(gobjFormatArt);
            }

            addObjectActions(this, objArt);
            addPropertyDraw(objFormat, publicGroup, true);
            addPropertyDraw(objArt, baseGroup, true, currentRRP);
            addPropertyDraw(objFormat, objArt, publicGroup, true);

            addAutoAction(objBarcode, addPropertyObject(barcodeAction2, objFormat, objBarcode));

            RegularFilterGroupEntity filterBalanceGroup = new RegularFilterGroupEntity(genID());
            filterBalanceGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(balanceFormatFreeQuantity, objFormat, objArt)),
                    "Есть своб. кол.",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
            addRegularFilterGroup(filterBalanceGroup);

            RegularFilterGroupEntity filterToSellGroup = new RegularFilterGroupEntity(genID());
            filterToSellGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(articleFormatToSell, objFormat, objArt)),
                    "В ассорт.",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)), !splitGroup);
            addRegularFilterGroup(filterToSellGroup);

            RegularFilterGroupEntity filterMinGroup = new RegularFilterGroupEntity(genID());
            filterMinGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(articleFormatMin, objFormat, objArt))),
                    "Без страх. запаса",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)), !splitGroup);
            addRegularFilterGroup(filterMinGroup);
        }
    }

    private class DocumentRevalueFormEntity extends ArticleNoCheckFormEntity {

        protected PropertyObjectEntity getCommitedQuantity() {
            return getPropertyObject(documentRevalued);
        }

        protected DocumentRevalueFormEntity(NavigatorElement parent, boolean toAdd, String sID) {
            super(parent, sID, documentRevalue, toAdd, true);
        }
    }

    private class DocumentNDSFormEntity extends ArticleNoCheckFormEntity {

        @Override
        protected Object[] getArticleProps() {
            return new Object[]{baseGroup, true, currentNDS};
        }

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

        protected DocumentNDSFormEntity(NavigatorElement parent, boolean toAdd, String sID) {
            super(parent, sID, documentNDS, toAdd);

            addHintsNoUpdate(currentNDSDoc);
            addHintsNoUpdate(currentNDSDate);
        }
    }

    private class IncomePriceFormEntity extends FormEntity {

        protected IncomePriceFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр цен", true);

            ObjectEntity objDoc = addSingleGroupObject(commitWholeShopInc, "Документ", date, nameContragent, invoiceNumber, invoiceSeries, sumNDSDeliveryOrder, sumDeliveryOrder, sumWithNDSDeliveryOrder, sumNDSRetailOrder, sumAddvOrder, sumRetailOrder);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, name, objectValue, barcode, nameUnitOfMeasureArticle);

            addPropertyDraw(objDoc, objArt, articleQuantity, orderAllDeliveryPrice, addvOrderArticle, ndsOrderArticle, shopPrice, sumRetailOrderArticle);

            addFixedFilter(new NotNullFilterEntity(getPropertyObject(shopPrice)));

            addFAProp(documentPriceGroup, "Реестр цен", this, objDoc);
        }
    }

    private class RevalueActFormEntity extends FormEntity {

        protected RevalueActFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Акт переоценки", true);

            ObjectEntity objDoc = addSingleGroupObject(documentShopPrice, "Документ", date, sumRevalBalance, sumPrevRetailOrder, sumNewPrevRetailOrder, sumPriceChangeOrder);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, name, objectValue, barcode);

            addPropertyDraw(objDoc, objArt, revalChangeBalance, prevPrice, sumPrevRetailOrderArticle, shopPrice, sumNewPrevRetailOrderArticle, sumPriceChangeOrderArticle);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(revalChangeBalance, objDoc, objArt)));

            addFAProp(documentPriceGroup, "Акт переоценки", this, objDoc);
        }
    }

    private class ActionFormEntity extends BarcodeFormEntity {
        protected ActionFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Акции");

            ObjectEntity objAction = addSingleGroupObject(action, publicGroup, true);
            addObjectActions(this, objAction);

            ObjectEntity objArtGroup = addSingleGroupObject(articleGroup, publicGroup, true);
            ObjectEntity objArt = addSingleGroupObject(article, baseGroup, true, priceGroup);

            if (noArticleGroups)
                objArtGroup.groupTo.initClassView = ClassViewType.HIDE;

            addPropertyDraw(objAction, objArtGroup, publicGroup, true);
            addPropertyDraw(objAction, objArt, publicGroup, true);

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

        protected PricersFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Ценники", true);

            ObjectEntity objDoc = addSingleGroupObject(documentShopPrice, "Документ", baseGroup, true);
            objDoc.groupTo.initClassView = ClassViewType.PANEL;
            ObjectEntity objArt = addSingleGroupObject(article, baseGroup, true);

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
                    orderSalePay, orderSalePayCash, orderSalePayCard, orderSaleToDo, orderSaleToDoSum, payWithCard};
        }

        @Override
        protected Font getDefaultFont() {
            return FONT_SMALL_PLAIN;
        }

        protected SaleCertFormEntity(NavigatorElement parent, String sID, CustomClass documentClass, boolean toAdd, boolean allStores) {
            super(parent, sID, documentClass, toAdd);

            if (!toAdd)
                addPropertyDraw(date, objDoc);

            if (allStores) {
                addPropertyDraw(objDoc, outStore, outStoreName);
                caption = caption + " (все склады)";
            } else {
                PropertyObjectEntity shopImplement = addPropertyObject(currentShop);
                addFixedFilter(new CompareFilterEntity(addPropertyObject(outStore, objDoc), Compare.EQUALS, shopImplement));
            }

            objObligation = addSingleGroupObject(giftObligation);
            addPropertyDraw(barcode, objObligation);
            addPropertyDraw(name, objObligation);
            addPropertyDraw(obligationSum, objObligation);
            addPropertyDraw(obligationSumFrom, objObligation);
            addPropertyDraw(obligationToIssued, objObligation);
            addPropertyDraw(issueObligation, objDoc, objObligation);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(issueObligation, objDoc, objObligation)),
                    "Документ",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), true);
            addRegularFilterGroup(filterGroup);

            if (!toAdd) {
                addPropertyDraw(checkRetailExported, objDoc);
            }

            addPropertyDraw(barcodeAddClient);
            addPropertyDraw(barcodeAddCert);
            addAutoAction(objBarcode, true,
                    addPropertyObject(barcodeAddCertAction, objBarcode),
                    addPropertyObject(barcodeAddClientAction, objBarcode),
                    addPropertyObject(barcodeAction2, objDoc, objBarcode),
                    addPropertyObject(seekBarcodeAction, objBarcode),
                    addPropertyObject(barcodeNotFoundMessage, objBarcode));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            design.getGroupObjectContainer(objDoc.groupTo).title = "Клиент";
            design.getGroupObjectContainer(objDoc.groupTo).design.background = new Color(192, 192, 192);
            design.setEnabled(publicGroup, false, objObligation.groupTo);
            design.setEnabled(obligationSum, true);

            design.get(objObligation.groupTo).grid.defaultComponent = true;

            design.setKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(barcodeAddClient)));
            design.addIntersection(design.get(getPropertyDraw(barcodeAddClient)), design.get(getPropertyDraw(barcodeObjectName)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.setEditKey(barcodeAddClient, KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(barcodeAddCert)));
            design.addIntersection(design.get(getPropertyDraw(barcodeAddCert)), design.get(getPropertyDraw(barcodeAddClient)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.setEditKey(barcodeAddCert, KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.CTRL_DOWN_MASK));

            return design;
        }


    }

    public class SaleCheckCertFormEntity extends SaleCertFormEntity {

        @Override
        public boolean isReadOnly() {
            return false;
//            return !toAdd;
        }

        protected SaleCheckCertFormEntity(NavigatorElement parent, String sID, boolean toAdd, boolean allStores) {
            super(parent, sID, saleCheckCert, toAdd, allStores);

            objDoc.caption = "Чек";

            if (toAdd) {
                addPropertyDraw(cashRegOperGroup, true);
            }
            addPropertyDraw(orderUserName, objDoc);
            getPropertyDraw(orderUserName).forceViewType = ClassViewType.HIDE;
            addPropertyDraw(accumulatedClientSum, objDoc);
        }

        @Override
        public boolean hasClientApply() {
            return toAdd;
        }

        @Override
        protected Font getDefaultFont() {
            if (toAdd)
                return FONT_MEDIUM_BOLD;
            else
                return super.getDefaultFont();
        }

        @Override
        public ClientResultAction getClientApply(FormInstance<VEDBusinessLogics> formInstance) {
            if (toAdd) {

                ObjectInstance doc = formInstance.instanceFactory.getInstance(objDoc);
                ObjectInstance obligation = formInstance.instanceFactory.getInstance(objObligation);

                return cashRegController.getCashRegApplyActions(formInstance, 1,
                        BaseUtils.toSet(obligation.groupTo),
                        getPropertyDraw(obligationSum, objObligation), getPropertyDraw(issueObligation, objObligation),
                        getPropertyDraw(name, objObligation), getPropertyDraw(obligationSum, objObligation),
                        getPropertyDraw(orderSalePay, objDoc), getPropertyDraw(barcode, objObligation),
                        getPropertyDraw(orderSalePayCard, objDoc), getPropertyDraw(orderSalePayCash, objDoc), null, null,
                        getPropertyDraw(orderUserName), getPropertyDraw(nameContragentImpl), getPropertyDraw(accumulatedClientSum),
                        null, null, null, null, null);

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

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            if (toAdd) {
                design.getGroupPropertyContainer(null, cashRegOperGroup).add(design.get(getPropertyDraw(payWithCard)));
                design.get(getPropertyDraw(payWithCard)).constraints.directions = new SimplexComponentDirections(0.1, -0.1, 0, 0.1);

                design.setHeaderFont(FONT_MEDIUM_PLAIN);
                design.setFont(FONT_LARGE_BOLD, objObligation.groupTo);
            }

            return design;
        }
    }

    private class SaleInvoiceCertFormEntity extends SaleCertFormEntity {
        protected SaleInvoiceCertFormEntity(NavigatorElement parent, String sID, boolean toAdd, boolean allStores) {
            super(parent, sID, saleInvoiceCert, toAdd, allStores);
        }

        @Override
        protected Object[] getDocumentProps() {
            return new Object[]{nameContragent, phoneContragent, bornContragent, addressContragent, initialSumContragent,
                    orderSalePay, orderSalePayCash, orderSalePayCard, orderSaleToDo, invoiceNumber, invoiceSeries};
        }
    }

    private class CouponIntervalFormEntity extends FormEntity {
        protected CouponIntervalFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Интервалы цен по купонам");

            ObjectEntity objIntervalAdd = addSingleGroupObject(DoubleClass.instance, "Цена товара от", objectValue, couponGroup, true);
            objIntervalAdd.groupTo.initClassView = ClassViewType.PANEL;
            objIntervalAdd.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.GRID, ClassViewType.HIDE));

            ObjectEntity objInterval = addSingleGroupObject(DoubleClass.instance, "Цена товара", objectValue, couponGroup, true);
            objInterval.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.HIDE));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(couponIssueSum, objInterval)));
        }
    }

    private class CouponArticleFormEntity extends FormEntity {
        private ObjectEntity objArt;

        protected CouponArticleFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Товары по купонам");

            ObjectEntity objArtGroup = addSingleGroupObject(articleGroup, baseGroup, true, couponGroup, true);
            objArt = addSingleGroupObject(article, baseGroup, true, couponGroup, true);

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

            setReadOnly(objArt, true);
            setReadOnly(xorCouponArticle, false);
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

    private class PayWithCardActionProperty extends ActionProperty {

        private PayWithCardActionProperty() {
            super(genSID(), "Опл. карт.", new ValueClass[]{orderSaleCheckRetail});

            askConfirm = true;
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            //To change body of implemented methods use File | Settings | File Templates.
            FormInstance<?> remoteForm = executeForm.form;
            DataSession session = remoteForm.session;

            DataObject document = BaseUtils.singleValue(keys);
            orderSalePayCash.execute(null, session, remoteForm, document);
            orderSalePayCard.execute(orderSalePayNoObligation.read(session.sql, remoteForm, session.env, document), session, remoteForm, document);

            actions.add(new ApplyClientAction());
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK);
        }
    }

    private class PrintOrderCheckActionProperty extends ActionProperty {

        private PrintOrderCheckActionProperty() {
            super(genSID(), "Печать", new ValueClass[]{orderSaleCheckRetail});
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            //To change body of implemented methods use File | Settings | File Templates.
            FormInstance<VEDBusinessLogics> remoteForm = executeForm.form;
            actions.add(((CommitSaleCheckRetailFormEntity) remoteForm.entity).getPrintOrderAction(remoteForm));
        }

        @Override
        public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
            super.proceedDefaultDesign(view, entity);
            view.get(entity).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_DOWN_MASK);
        }
    }

    public class SaleExportActionProperty extends ActionProperty {

        private final ClassPropertyInterface shopInterface;
        private final ClassPropertyInterface dateFrom;
        private final ClassPropertyInterface dateTo;

        public SaleExportActionProperty() {
            super(genSID(), "Экспорт реализации", new ValueClass[]{shop, DateClass.instance, DateClass.instance});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            shopInterface = i.next();
            dateFrom = i.next();
            dateTo = i.next();
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            Integer shopID = (Integer) keys.get(shopInterface).object;
            try {
//                ((SaleExportTask) scheduler.getTask("saleExport")).getPath(shopID)
                new AbstractSaleExportTask(VEDBusinessLogics.this, "exp2", shopID) {
                    protected String getDbfName() {
                        return "datadat.dbf";
                    }

                    protected void setRemoteFormFilter(FormInstance formInstance) throws ParseException {
                        PropertyDrawInstance<?> dateDraw = formInstance.getPropertyDraw(date);
                        dateDraw.toDraw.addTempFilter(new CompareFilterInstance(dateDraw.propertyObject, Compare.GREATER_EQUALS, keys.get(dateFrom)));
                        dateDraw.toDraw.addTempFilter(new CompareFilterInstance(dateDraw.propertyObject, Compare.LESS_EQUALS, keys.get(dateTo)));
                    }

                    protected void updateRemoteFormProperties(FormInstance formInstance) throws SQLException {
                    }
                }.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            actions.add(new MessageClientAction("Данные были успешно выгружены", "Экспорт"));
        }
    }

    public class ImportOrderActionProperty extends ActionProperty {

        private final ClassPropertyInterface documentInterface;

        public ImportOrderActionProperty() {
            super(genSID(), "Импортировать заявку", new ValueClass[]{orderDelivery});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            documentInterface = i.next();
        }

        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            DataObject document = keys.get(documentInterface);
            FormInstance remoteForm = executeForm.form;
            DataSession session = remoteForm.session;

            Sheet sh;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                sh = Workbook.getWorkbook(inFile).getSheet(0);
            } catch (Exception e) {
                logger.fatal("Не могу прочитать .xsl файл.");
                return;
            }

            List<ImportField> fields = new ArrayList<ImportField>();
            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

            ImportField barcodeField = new ImportField(StringClass.get(13)); fields.add(barcodeField);
            ImportKey<?> articleKey = new ImportKey(article, barcodeToObject.getMapping(barcodeField));
            properties.add(new ImportProperty(barcodeField, barcode.getMapping(articleKey)));

            ImportField nameField = new ImportField(StringClass.get(100)); fields.add(nameField);
            properties.add(new ImportProperty(nameField, name.getMapping(articleKey)));
            ImportField priceField = new ImportField(DoubleClass.instance); fields.add(priceField);
            properties.add(new ImportProperty(priceField, orderAllDeliveryPrice.getMapping(document, articleKey)));
            ImportField quantityField = new ImportField(DoubleClass.instance); fields.add(quantityField);
            properties.add(new ImportProperty(quantityField, articleOrderQuantity.getMapping(document, articleKey)));
            ImportField ndsField = new ImportField(DoubleClass.instance); fields.add(ndsField);
            properties.add(new ImportProperty(ndsField, ndsOrderArticle.getMapping(document, articleKey)));

            ImportField countryField = new ImportField(InsensitiveStringClass.get(60)); fields.add(countryField);
            ImportKey<?> countryKey = new ImportKey(country, nameToCountry.getMapping(countryField));
            properties.add(new ImportProperty(countryField, articleCountry.getMapping(articleKey), object(country).getMapping(countryKey)));

            List<List<Object>> rows = new ArrayList<List<Object>>();

            for (int i = 0; i < sh.getRows(); ++i) {
                List<Object> row = new ArrayList<Object>();

                for (int j = 0; j < fields.size(); j++) {
                    try {
                        row.add(fields.get(j).getFieldClass().parseString(sh.getCell(j, i).getContents()));
                    } catch (platform.server.data.type.ParseException e) {
                        logger.warn("Не конвертировано значение совйства", e);
                    }
                }

                rows.add(row);
            }


            new IntegrationService(session, new ImportTable(fields, rows), Arrays.asList(articleKey, countryKey), properties).synchronize(true, true, false);


            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        }

        @Override
        protected DataClass getValueClass() {
            return FileActionClass.getInstance("Файлы таблиц", "xls");
        }

        @Override
        public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity form) {
            super.proceedDefaultDraw(entity, form);
            entity.shouldBeLast = true;
            entity.forceViewType = ClassViewType.PANEL;
        }
    }
}
