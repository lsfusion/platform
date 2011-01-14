package roman;

import net.sf.jasperreports.engine.JRException;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.*;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class RomanBusinessLogics extends BusinessLogics<RomanBusinessLogics> {
    private AbstractCustomClass article;
    private ConcreteCustomClass articleComposite;
    private ConcreteCustomClass articleSingle;
    private ConcreteCustomClass item;
    private AbstractCustomClass sku;
    private ConcreteCustomClass pallet;
    private LP sidArticle;
    private LP articleItem;
    private LP sidArticleItem;
    private ConcreteCustomClass order;
    private AbstractCustomClass invoice;
    private AbstractCustomClass shipment;
    private ConcreteCustomClass boxShipment;
    private ConcreteCustomClass simpleShipment;
    private ConcreteCustomClass freight;
    private ConcreteCustomClass route;
    private ConcreteCustomClass supplier;
    private AbstractCustomClass document;
    private AbstractCustomClass priceDocument;
    private LP supplierDocument;
    private LP nameSupplierDocument;
    private LP sidDocument;
    private LP sumDocument;
    private ConcreteCustomClass colorSupplier;
    private ConcreteCustomClass sizeSupplier;
    private LP supplierArticle;
    private LP nameSupplierArticle;
    private LP priceDocumentArticle;
    private LP sumDocumentArticle;
    private LP colorSupplierItem;
    private LP nameColorSupplierItem;
    private LP sizeSupplierItem;
    private LP nameSizeSupplierItem;
    private LP supplierColorSupplier;
    private LP nameSupplierColorSupplier;
    private LP supplierSizeSupplier;
    private LP nameSupplierSizeSupplier;
    private LP supplierItem;
    private LP nameSupplierItem;
    private ConcreteCustomClass currency;
    private ConcreteCustomClass store;
    private LP currencySupplier;
    private LP nameCurrencySupplier;
    private LP currencyDocument;
    private LP nameCurrencyDocument;
    private LP storeOrder;
    private LP nameStoreOrder;
    private LP quantityPalletShipment;
    private LP grossWeightShipment;
    private LP netWeightShipment;
    private LP sidColorSupplier;
    private LP sidColorSupplierItem;
    private LP quantityDocumentSku;
    private LP quantityDocumentArticleComposite;
    private LP quantityDocumentArticleSingle;
    private LP quantityDocumentArticle;
    private LP quantityDocumentArticleCompositeColor;
    private LP quantityDocumentArticleCompositeSize;
    private LP quantityDocumentArticleCompositeColorSize;
    private LP originalNameArticle;
    private ConcreteCustomClass country;
    private LP countryOfOriginArticle;
    private LP nameCountryOfOriginArticle;
    private LP articleSIDSupplier;
    private LP seekArticleSIDSupplier;
    private LP numberListArticle;
    private LP incrementNumberDocumentArticle;
    private LP articleSIDList;
    private LP incrementNumberListSID;
    private LP addArticleSingleSIDSupplier;
    private LP addNEArticleSingleSIDSupplier;
    private LP addArticleCompositeSIDSupplier;
    private LP addNEArticleCompositeSIDSupplier;
    private LP numberListSIDArticle;
    private LP inOrderInvoice;
    private LP inBoxDocumentArticle;
    private LP quantityOrderInvoiceSku;
    private LP orderedOrderInvoiceSku;
    private LP orderedInvoiceSku;
    private LP invoicedOrderSku;
    private LP quantityAggregateOrderInvoiceSku;
    private LP invoicedOrderArticleComposite;
    private LP invoicedOrderArticleSingle;
    private LP invoicedOrderArticle;
    private LP inListArticleSingle;
    private LP numberArticleListItem;
    private LP inListSku;
    private LP quantityListSku;
    private LP orderedOrderInvoiceArticle;
    private LP orderedInvoiceArticle;
    private LP priceOrderInvoiceArticle;
    private LP priceOrderedInvoiceArticle;
    private AbstractCustomClass boxDocument;
    private AbstractCustomClass simpleDocument;
    private ConcreteCustomClass supplierBox;
    private LP quantityBoxDocumentSupplierBoxSku;
    private LP quantityBoxDocumentSku;
    private ConcreteCustomClass boxInvoice;
    private ConcreteCustomClass simpleInvoice;
    private LP sidSupplierBox;
    private LP inListItem;
    private AbstractCustomClass list;
    private LP quantitySimpleDocumentSku;
    private LP quantityDataListSku;
    private LP boxDocumentSupplierBox;
    private LP sidBoxDocumentSupplierBox;
    private LP supplierSupplierBox;
    private LP supplierSimpleDocument;
    private LP orderedSupplierBoxSku;
    private LP quantityListArticleComposite;
    private LP quantityBoxDocumentArticleComposite;
    private LP quantitySimpleDocumentArticleComposite;
    private LP quantityListArticleSingle;
    private LP quantityListArticle;
    private LP orderedSimpleInvoiceSku;
    private LP priceDataDocumentItem;
    private LP priceArticleDocumentItem;
    private LP priceDocumentItem;
    private LP priceDocumentArticleSingle;
    private LP priceDocumentSku;
    private LP sumDocumentSku;
    private LP sumDocumentItem;
    private LP sumDocumentArticleSingle;
    private LP sumDocumentArticleComposite;
    private LP quantityListItem;
    private LP quantitySimpleDocumentItem;
    private LP quantityBoxDocumentItem;
    private LP quantityDocumentItem;
    private LP sumNumberBoxDocumentArticle;
    private LP inSimpleDocumentArticle;
    private LP inDocumentArticle;
    private LP inInvoiceShipment;
    private LP tonnageFreight;
    private LP quantityPalletFreight;
    private LP volumeFreight;
    private LP sumFreight;
    private LP routeFreight;
    private LP nameRouteFreight;
    private LP quantityPalletShipmentFreight;
    private LP freighedShipment;
    private LP shipmentedFreight;
    private ConcreteCustomClass stock;
    private ConcreteCustomClass freightBox;
    private LP countryOfOriginArticleItem;
    private LP countryOfOriginDataItem;
    private LP countryOfOriginItem;
    private LP countryOfOriginArticleSingle;
    private LP countryOfOriginSku;
    private LP nameCountryOfOriginSku;
    private LP sidArticleSingle;
    private LP sidArticleSku;
    private LP originalNameArticleSingle;
    private LP originalNameArticleItem;
    private LP originalNameArticleSku;
    private LP inSupplierBoxShipment;
    private LP invoicedSimpleShipmentSku;
    private LP quantitySupplierBoxBoxShipmentStockSku;
    private LP quantitySimpleShipmentStockSku;
    private LP barcodeAction3;
    private LP supplierBoxSIDSupplier;
    private LP seekSupplierBoxSIDSupplier;
    private LP quantityPalletShipmentBetweenDate;
    private LP quantityPalletFreightBetweenDate;
    private LP routeFreightBox;
    private LP nameRouteFreightBox;
    private LP quantityBoxShipmentStockSku;
    private LP quantityShipmentStockSku;
    private LP quantityShipmentRouteSku;
    private LP quantitySupplierBoxBoxShipmentSku;
    private LP quantitySimpleShipmentSku;
    private LP nameDataArticle;
    private LP nameArticle;
    private LP nameArticleSingle;
    private LP nameArticleItem;
    private LP nameArticleSku;
    private LP supplierPriceDocument;
    private LP percentShipmentRoute;
    private LP percentShipmentRouteSku;
    private LP invoicedBoxShipmentSku;
    private LP invoicedShipmentSku;
    private LP invoicedShipmentRouteSku;

    public RomanBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void initGroups() {
    }

    @Override
    protected void initClasses() {

        country = addConcreteClass("country", "Страна", baseClass.named);

        currency = addConcreteClass("currency", "Валюта", baseClass.named);

        store = addConcreteClass("store", "Магазин", baseClass.named);

        sku = addAbstractClass("sku", "SKU", barcodeObject);

        article = addAbstractClass("article", "Артикул", baseClass);
        articleComposite = addConcreteClass("articleComposite", "Артикул (составной)", article);
        articleSingle = addConcreteClass("articleSingle", "Артикул (простой)", sku, article);

        item = addConcreteClass("item", "Товар", sku);

        document = addAbstractClass("document", "Документ", transaction);
        list = addAbstractClass("list", "Список", baseClass);

        priceDocument = addAbstractClass("priceDocument", "Документ с ценами", document);

        boxDocument = addAbstractClass("boxDocument", "Документ по коробам", document);
        simpleDocument = addAbstractClass("simpleDocument", "Документ без коробов", document, list);

        supplierBox = addConcreteClass("supplierBox", "Короб поставщика", list, barcodeObject);

        order = addConcreteClass("order", "Заказ", priceDocument, simpleDocument);

        invoice = addAbstractClass("invoice", "Инвойс", priceDocument);
        boxInvoice = addConcreteClass("boxInvoice", "Инвойс по коробам", invoice, boxDocument);
        simpleInvoice = addConcreteClass("simpleInvoice", "Инвойс без коробов", invoice, simpleDocument);

        shipment = addAbstractClass("shipment", "Поставка", document);
        boxShipment = addConcreteClass("boxShipment", "Поставка по коробам", shipment);
        simpleShipment = addConcreteClass("simpleShipment", "Поставка без коробов", shipment);

        supplier = addConcreteClass("supplier", "Поставщик", baseClass.named);

        colorSupplier = addConcreteClass("colorSupplier", "Цвет поставщика", baseClass.named);
        sizeSupplier = addConcreteClass("sizeSupplier", "Размер поставщика", baseClass.named);

        stock = addConcreteClass("stock", "Место хранения", barcodeObject);

        freightBox = addConcreteClass("freightBox", "Короб для транспортировки", stock);

        freight = addConcreteClass("freight", "Фрахт", baseClass.named, transaction);

        pallet = addConcreteClass("pallet", "Паллета", barcodeObject);

        route = addStaticClass("route", "Маршрут", new String[]{"rb", "rf"}, new String[]{"РБ", "РФ"});
    }

    @Override
    protected void initProperties() {

        currencySupplier = addDProp(idGroup, "currencySupplier", "Валюта (ИД)", currency, supplier);
        nameCurrencySupplier = addJProp(baseGroup, "nameCurrencySupplier", "Валюта", name, currencySupplier, 1);

        sidColorSupplier = addDProp(baseGroup, "sidColorSupplier", "Код", StringClass.get(50), colorSupplier);

        supplierColorSupplier = addDProp(idGroup, "supplierColorSupplier", "Поставщик (ИД)", supplier, colorSupplier);
        nameSupplierColorSupplier = addJProp(baseGroup, "nameSupplierColorSupplier", "Поставщик", name, supplierColorSupplier, 1);

        supplierSizeSupplier = addDProp(idGroup, "supplierSizeSupplier", "Поставщик (ИД)", supplier, sizeSupplier);
        nameSupplierSizeSupplier = addJProp(baseGroup, "nameSupplierSizeSupplier", "Поставщик", name, supplierSizeSupplier, 1);

        supplierDocument = addDProp(idGroup, "supplierDocument", "Поставщик (ИД)", supplier, document);
        supplierPriceDocument = addJProp(idGroup, "supplierPricedDocument", "Поставщик(ИД)", and1, supplierDocument, 1, is(priceDocument), 1);
        nameSupplierDocument = addJProp(baseGroup, "nameSupplierDocument", "Поставщик", name, supplierDocument, 1);

        supplierSimpleDocument = addJProp("supplierSimpleDocument", "Поставщик (ИД)", and1, supplierDocument, 1, is(simpleDocument), 1);

        currencyDocument = addDCProp(idGroup, "currencyDocument", "Валюта (ИД)", currencySupplier, supplierPriceDocument, 1);
        //currencyDocument = addDProp(idGroup, "currencyDocument", "Валюта (ИД)", currency, order);
        nameCurrencyDocument = addJProp(baseGroup, "nameCurrencyDocument", "Валюта", name, currencyDocument, 1);
        
        // Order
        storeOrder = addDProp(idGroup, "storeOrder", "Магазин (ИД)", store, order);
        nameStoreOrder = addJProp(baseGroup, "nameStoreOrder", "Магазин", name, storeOrder, 1);

        // Shipment
        quantityPalletShipment = addDProp(baseGroup, "quantityPalletShipment", "Кол-во паллет", IntegerClass.instance, shipment);
        netWeightShipment = addDProp(baseGroup, "netWeightShipment", "Вес нетто", DoubleClass.instance, shipment);
        grossWeightShipment = addDProp(baseGroup, "grossWeightShipment", "Вес брутто", DoubleClass.instance, shipment);

        // Article
        sidArticle = addDProp(baseGroup, "sidArticle", "Код", StringClass.get(50), article);
        sidArticleSingle = addJProp(baseGroup, "sidArticleSingle", "Код", and1, sidArticle, 1, is(articleSingle), 1);

        originalNameArticle = addDProp(baseGroup, "originalNameArticle", "Имя производителя (ориг.)", StringClass.get(50), article);
        originalNameArticleSingle = addJProp(baseGroup, "originalNameArticleSingle", "Имя производителя (ориг.)", and1, originalNameArticle, 1, is(articleSingle), 1);

        nameDataArticle = addDProp(baseGroup, "nameDataArticle", "Имя производителя (перв.)", StringClass.get(50), article);
        nameArticle = addSUProp(baseGroup, "nameArticle", "Имя производителя", Union.OVERRIDE, originalNameArticle, nameDataArticle);
        nameArticleSingle = addJProp(baseGroup, "nameArticleSingle", "Имя производителя", and1, nameArticle, 1, is(articleSingle), 1);

        countryOfOriginArticle = addDProp(idGroup, "countryOfOriginArticle", "Страна происхождения (ИД)", country, article);
        nameCountryOfOriginArticle = addJProp(baseGroup, "nameCountryOfOriginArticle", "Страна происхождения", name, countryOfOriginArticle, 1);
        countryOfOriginArticleSingle = addJProp(idGroup, "countryOfOriginSingleArticle", "Страна происхождения (ИД)", and1, countryOfOriginArticle, 1, is(articleSingle), 1);

        supplierArticle = addDProp(idGroup, "supplierArticle", "Поставщик (ИД)", supplier, article);
        nameSupplierArticle = addJProp(baseGroup, "nameSupplierArticle", "Поставщик", name, supplierArticle, 1);

        articleSIDSupplier = addCGProp(idGroup, "articleSIDSupplier", "Артикул (ИД)", object(article), sidArticle, sidArticle, 1, supplierArticle, 1);

        seekArticleSIDSupplier = addJProp(true, "Поиск артикула", addSAProp(null), articleSIDSupplier, 1, 2);

        addArticleSingleSIDSupplier = addJProp(true, "Ввод простого артикула", addAAProp(articleSingle, sidArticle, supplierArticle), 1, 2);
        addNEArticleSingleSIDSupplier = addJProp(true, "Ввод простого артикула (НС)", andNot1, addArticleSingleSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);

        addArticleCompositeSIDSupplier = addJProp(true, "Ввод составного артикула", addAAProp(articleComposite, sidArticle, supplierArticle), 1, 2);
        addNEArticleCompositeSIDSupplier = addJProp(true, "Ввод составного артикула (НС)", andNot1, addArticleCompositeSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);

        // Item

        articleItem = addDProp(idGroup, "articleItem", "Артикул (ИД)", articleComposite, item);

        sidArticleItem = addJProp(baseGroup, "sidArticleItem", "Артикул", sidArticle, articleItem, 1);
        originalNameArticleItem = addJProp(baseGroup, "originalNameArticleItem", "Имя производителя (ориг.)", originalNameArticle, articleItem, 1);
        nameArticleItem = addJProp(baseGroup, "nameArticleItem", "Имя производителя", nameArticle, articleItem, 1);

        countryOfOriginArticleItem = addJProp(idGroup, "countryOfOriginArticleItem", "Страна происхождения (ИД) (артикул)", countryOfOriginArticle, articleItem, 1);
        countryOfOriginDataItem = addDProp(idGroup, "countryOfOriginDataItem", "Страна происхождения (ИД) (первичное)", country, item);
        countryOfOriginItem = addSUProp(idGroup, "countryOfOriginItem", "Страна происхождения (ИД)", Union.OVERRIDE, countryOfOriginArticleItem, countryOfOriginDataItem);

        supplierItem = addJProp(idGroup, "supplierItem", "Поставщик (ИД)", supplierArticle, articleItem, 1);
        nameSupplierItem = addJProp(baseGroup, "nameSupplierItem", "Поставщик", name, supplierItem, 1);

        colorSupplierItem = addDProp(idGroup, "colorSupplierItem", "Цвет поставщика (ИД)", colorSupplier, item);
        sidColorSupplierItem = addJProp(baseGroup, "sidColorSupplierItem", "Код цвета", sidColorSupplier, colorSupplierItem, 1);
        nameColorSupplierItem = addJProp(baseGroup, "nameColorSupplierItem", "Цвет поставщика", name, colorSupplierItem, 1);

        sizeSupplierItem = addDProp(idGroup, "sizeSupplierItem", "Размер поставщика (ИД)", sizeSupplier, item);
        nameSizeSupplierItem = addJProp(baseGroup, "nameSizeSupplierItem", "Размер поставщика", name, sizeSupplierItem, 1);

        addConstraint(addJProp("Поставщик товара должен соответствовать цвету поставщика", diff2,
                supplierItem, 1,
                addJProp(supplierColorSupplier, colorSupplierItem, 1), 1), true);

        addConstraint(addJProp("Поставщик товара должен соответствовать размеру поставщика", diff2,
                supplierItem, 1,
                addJProp(supplierSizeSupplier, sizeSupplierItem, 1), 1), true);

        // sku
        sidArticleSku = addCUProp(baseGroup, "sidArticleSku", "Код", sidArticleItem, sidArticleSingle);
        originalNameArticleSku = addCUProp(baseGroup, "originalNameArticleSku", "Имя производителя (ориг.)", originalNameArticleItem, originalNameArticleSingle);
        nameArticleSku = addCUProp(baseGroup, "nameArticleSku", "Имя производителя", nameArticleItem, nameArticleSingle);

        countryOfOriginSku = addCUProp(idGroup, "countryOfOriginSku", "Страна происхождения (ИД)", countryOfOriginItem, countryOfOriginArticleSingle);
        nameCountryOfOriginSku = addJProp(baseGroup, "nameCountryOfOriginSku", "Страна происхождения", name, countryOfOriginSku, 1);

        sidDocument = addDProp(baseGroup, "sidDocument", "Код", StringClass.get(50), document);

        // коробки
        sidSupplierBox = addDProp(baseGroup, "sidSupplierBox", "Код", StringClass.get(50), supplierBox);

        boxDocumentSupplierBox = addDProp(idGroup, "boxDocumentSupplierBox", "Документ по коробам (ИД)", boxDocument, supplierBox);
        sidBoxDocumentSupplierBox = addJProp(baseGroup, "nameBoxDocumentSupplierBox", "Документ по коробам", sidDocument, boxDocumentSupplierBox, 1);

        supplierSupplierBox = addJProp(idGroup, "supplierSupplierBox", "Поставщик (ИД)", supplierDocument, boxDocumentSupplierBox, 1);

        supplierBoxSIDSupplier = addCGProp(idGroup, "supplierBoxSIDSupplier", "Короб поставщика (ИД)", object(supplierBox), sidSupplierBox, sidSupplierBox, 1, supplierSupplierBox, 1);

        seekSupplierBoxSIDSupplier = addJProp(true, "Поиск короба поставщика", addSAProp(null), supplierBoxSIDSupplier, 1, 2);

        // заказ по артикулам

        articleSIDList = addJProp(idGroup, "articleSIDList", "Артикул (ИД)", articleSIDSupplier, 1,
                addCUProp(supplierSimpleDocument, supplierSupplierBox), 2);

        numberListArticle = addDProp(baseGroup, "numberListArticle", "Номер", IntegerClass.instance, list, article);
        numberListSIDArticle = addJProp(numberListArticle, 1, articleSIDList, 2, 1);

        inListArticleSingle = addJProp(baseGroup, "inListArticleSingle", "Вкл", and1, addCProp(LogicalClass.instance, true, list, articleSingle), 1, 2, numberListArticle, 1, 2);
        numberArticleListItem = addJProp("numberArticleListItem", "Номер артикула в списке", numberListArticle, 1, articleItem, 2);
        inListItem = addJProp(baseGroup, "inListItem", "Вкл", and1, addCProp(LogicalClass.instance, true, list, item), 1, 2, numberArticleListItem, 1, 2);
        inListSku = addCUProp(baseGroup, "inListSku", "Вкл", inListArticleSingle, numberArticleListItem);

        sumNumberBoxDocumentArticle = addSGProp(baseGroup, "sumBoxDocumentArticleNumber", numberListArticle, boxDocumentSupplierBox, 1, 2);
        inBoxDocumentArticle = addJProp(baseGroup, "inBoxDocumentArticle", "Вкл.", and1, addCProp(LogicalClass.instance, true, boxDocument, article), 1, 2, sumNumberBoxDocumentArticle, 1, 2);
        
        inSimpleDocumentArticle = addJProp(baseGroup, "inSimpleDocumentArticle", "Вкл", and1, addCProp(LogicalClass.instance, true, simpleDocument, article), 1, 2, numberListArticle, 1, 2);
        inDocumentArticle = addCUProp(baseGroup, "inDocumentArticle", "Вкл", inBoxDocumentArticle, inSimpleDocumentArticle);

        incrementNumberListSID = addJProp(true, "Добавить строку", andNot1,
                                                  addJProp(true, addIAProp(numberListArticle, 1),
                                                  1, articleSIDList, 2, 1), 1, 2,
                numberListSIDArticle, 1, 2); // если еще не было добавлено такой строки

        // кол-во заказа
        quantityDataListSku = addDProp("quantityDataListSku", "Кол-во (первичное)", DoubleClass.instance, list, sku);
        quantityListSku = addJProp(baseGroup, "quantityListSku", true, "Кол-во", and1, quantityDataListSku, 1, 2, inListSku, 1, 2);

        quantitySimpleDocumentSku = addJProp(baseGroup, "quantitySimpleDocumentSku", "Кол-во", and1, quantityListSku, 1, 2, is(simpleDocument), 1);
        quantityBoxDocumentSku = addSGProp(baseGroup, "quantityBoxDocumentSku", "Кол-во в инвойсе", quantityListSku, boxDocumentSupplierBox, 1, 2);

        quantityDocumentSku = addCUProp(baseGroup, "quantityDocumentSku", "Кол-во", quantitySimpleDocumentSku, quantityBoxDocumentSku);

        // связь инвойсов и заказов
        inOrderInvoice = addDProp(baseGroup, "inOrderInvoice", "Вкл", LogicalClass.instance, order, invoice);
        orderedOrderInvoiceSku = addJProp(and1, quantityDocumentSku, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceSku = addSGProp(baseGroup, "orderedInvoiceSku", "Кол-во заказано", orderedOrderInvoiceSku, 2, 3);
        orderedSimpleInvoiceSku = addJProp(baseGroup, "orderedSimpleInvoiceSku", "Кол-во заказано", and1, orderedInvoiceSku, 1, 2, is(simpleInvoice), 1);
        // здесь на самом деле есть ограничение, что supplierBox ссылается именно на invoice
        orderedSupplierBoxSku = addJProp("orderedSupplierBoxSku", "Кол-во заказано", orderedInvoiceSku, boxDocumentSupplierBox, 1, 2);


//        // todo : переделать на PGProp        // todo : здесь надо derive'ить, иначе могут быть проблемы с расписыванием
//        quantityOrderInvoiceSku = addPGProp(baseGroup, "quantityOrderInvoiceSku", true, 0, "Кол-во по заказу/инвойсу (расч.)",
//                orderedOrderInvoiceSku,
//                quantityDocumentSku, 2, 3);

        quantityOrderInvoiceSku = addDProp(baseGroup, "quantityOrderInvoiceSku", "Кол-во по заказу/инвойсу (расч.)", DoubleClass.instance,
                                           order, invoice, sku);

        invoicedOrderSku = addSGProp(baseGroup, "invoicedOrderSku", "Выставлено инвойсов", quantityOrderInvoiceSku, 1, 3);

        quantityListItem = addJProp(baseGroup, "quantityListItem", "Кол-во", and1, quantityListSku, 1, 2, is(item), 2);

        quantitySimpleDocumentItem = addJProp(baseGroup, "quantitySimpleDocumentItem", "Кол-во", and1, quantityListItem, 1, 2, is(simpleDocument), 1);
        quantityBoxDocumentItem = addSGProp(baseGroup, "quantityBoxDocumentItem", "Кол-во", quantityListItem, boxDocumentSupplierBox, 1, 2);

        quantityDocumentItem = addCUProp(baseGroup, "quantityDocumentItem", "Кол-во", quantityBoxDocumentItem, quantitySimpleDocumentItem);

        // для заказа при вводе этого количества все кидается на первую
        quantityListArticleComposite = addDGProp(baseGroup, "quantityListArticleComposite", "Кол-во",
                1, false, // кол-во объектов для порядка и ascending/descending
                quantityListSku, 1, articleItem, 2,
                addCUProp(addCProp(DoubleClass.instance, Double.MAX_VALUE, order, sku), orderedSimpleInvoiceSku, orderedSupplierBoxSku), 1, 2, // ограничение (максимально-возможное число)
                2);
        quantityListArticleSingle = addJProp("quantityListArticleSingle", "Кол-во", and1, quantityListSku, 1, 2, is(articleSingle), 2);
        quantityListArticle = addCUProp(baseGroup, "quantityListArticle", "Кол-во", quantityListArticleComposite, quantityListArticleSingle);

        quantityBoxDocumentArticleComposite = addSGProp("quantityBoxDocumentArticleComposite", "Кол-во",
                                                quantityListArticleComposite, boxDocumentSupplierBox, 1, 2);

        quantitySimpleDocumentArticleComposite = addJProp("quantitySimpleDocumentArticleComposite", "Кол-во",
                and1, quantityListArticleComposite, 1, 2, is(simpleDocument), 1);

        quantityDocumentArticleComposite = addCUProp(quantityBoxDocumentArticleComposite, quantitySimpleDocumentArticleComposite);

        quantityDocumentArticleSingle = addJProp(baseGroup, "quantityDocumentArticleSingle", "Кол-во", and1, quantityDocumentSku, 1, 2, is(articleSingle), 2);

        quantityDocumentArticle = addCUProp(baseGroup, "quantityDocumentArticle", "Кол-во", quantityDocumentArticleComposite, quantityDocumentArticleSingle);

        quantityDocumentArticleCompositeColor = addSGProp(baseGroup, "quantityDocumentArticleCompositeColor", "Кол-во", quantityDocumentSku, 1, articleItem, 2, colorSupplierItem, 2);
        quantityDocumentArticleCompositeSize = addSGProp(baseGroup, "quantityDocumentArticleCompositeSize", "Кол-во", quantityDocumentSku, 1, articleItem, 2, sizeSupplierItem, 2);

        quantityDocumentArticleCompositeColorSize = addDGProp(baseGroup, "quantityDocumentArticleCompositeColorSize", "Кол-во",
                1, false,
                quantityDocumentSku, 1, articleItem, 2, colorSupplierItem, 2, sizeSupplierItem, 2,
                addCProp(DoubleClass.instance, Double.MAX_VALUE, document, sku), 1, 2,
                2);
        quantityDocumentArticleCompositeColorSize.property.setFixedCharWidth(2);

        orderedOrderInvoiceArticle = addJProp(and1, quantityDocumentArticle, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceArticle = addSGProp(baseGroup, "orderedInvoiceArticle", "Кол-во заказано", orderedOrderInvoiceArticle, 2, 3);
        // todo : сделать, чтобы работало автоматическое проставление
//        quantityDocumentArticle.setDerivedChange(orderedInvoiceArticle, 1, 2, numberListArticle, 1, 2);

        invoicedOrderArticleComposite = addSGProp(baseGroup, "orderedInvoiceArticleComposite", "Выставлено инвойсов", invoicedOrderSku, 1, articleItem, 2);
        invoicedOrderArticleSingle = addJProp(baseGroup, "invoicedOrderArticleSingle", "Выставлено инвойсов", and1, invoicedOrderSku, 1, 2, is(articleSingle), 2);
        invoicedOrderArticle = addCUProp(baseGroup, "invoicedOrderArticle", "Выставлено инвойсов", invoicedOrderArticleComposite, invoicedOrderArticleSingle);

        // цены

        priceDocumentArticle = addDProp(baseGroup, "priceDocumentArticle", "Цена", DoubleClass.instance, priceDocument, article);
        priceDataDocumentItem = addDProp(baseGroup, "priceDataDocumentItem", "Цена по товару", DoubleClass.instance, priceDocument, item);
        priceArticleDocumentItem = addJProp(baseGroup, "priceArticleDocumentItem", "Цена по артикулу", priceDocumentArticle, 1, articleItem, 2);
        priceDocumentItem = addSUProp(baseGroup, "priceDocumentItem", "Цена", Union.OVERRIDE, priceArticleDocumentItem, priceDataDocumentItem);

        priceDocumentArticleSingle = addJProp(baseGroup, "priceDocumentArticleSingle", "Цена", and1, priceDocumentArticle, 1, 2, is(articleSingle), 2);
        priceDocumentSku = addCUProp(baseGroup, "priceDocumentSku", "Цена", priceDocumentItem, priceDocumentArticleSingle);

        priceOrderInvoiceArticle = addJProp(and1, priceDocumentArticle, 1, 3, inOrderInvoice, 1, 2);
        priceOrderedInvoiceArticle = addMGProp(baseGroup, "priceOrderedInvoiceArticle", "Цена в заказе", priceOrderInvoiceArticle, 2, 3);
        // todo : не работает
        priceDocumentArticle.setDerivedChange(priceOrderedInvoiceArticle, 1, 2, inDocumentArticle, 1, 2);

        sumDocumentItem = addJProp(baseGroup, "sumDocumentItem", "Сумма", multiplyDouble2, quantityDocumentItem, 1, 2, priceDocumentItem, 1, 2);
        sumDocumentArticleSingle = addJProp(baseGroup, "sumDocumentArticleSingle", "Сумма", multiplyDouble2, quantityDocumentArticleSingle, 1, 2, priceDocumentArticleSingle, 1, 2);
        sumDocumentArticleComposite = addSGProp(baseGroup, "sumDocumentArticleComposite", "Сумма", sumDocumentItem, 1, articleItem, 2);
        sumDocumentArticle = addCUProp(baseGroup, "sumDocumentArticle", "Сумма", sumDocumentArticleComposite, sumDocumentArticleSingle);

        sumDocumentSku = addJProp(baseGroup, "sumDocumentSku", "Сумма", multiplyDouble2, quantityDocumentSku, 1, 2, priceDocumentSku, 1, 2);
        sumDocument = addSGProp(baseGroup, "sumDocument", "Сумма документа", sumDocumentSku, 1);

        // route
        percentShipmentRoute = addDProp(baseGroup, "percentShipmentRoute", "Процент", DoubleClass.instance, shipment, route);

        percentShipmentRouteSku = addJProp(baseGroup, "percentShipmentRouteSku", "Процент", and1, percentShipmentRoute, 1, 2, is(sku), 3);

        // поставка на склад
        inInvoiceShipment = addDProp(baseGroup, "inInvoiceShipment", "Вкл", LogicalClass.instance, invoice, shipment);

        inSupplierBoxShipment = addJProp(baseGroup, "inSupplierBoxShipment", "Вкл", inInvoiceShipment, boxDocumentSupplierBox, 1, 2);

        invoicedShipmentSku = addSGProp(baseGroup, "invoicedShipmentSku", "Кол-во ожид.",
                                              addJProp(and1, quantityDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        quantitySupplierBoxBoxShipmentStockSku = addDProp(baseGroup, "quantitySupplierBoxBoxShipmentStockSku", "Кол-во оприход.", DoubleClass.instance,
                                                          supplierBox, boxShipment, stock, sku);

        quantitySupplierBoxBoxShipmentSku = addSGProp(baseGroup, "quantitySupplierBoxBoxShipmentSku", "Всего оприход.", quantitySupplierBoxBoxShipmentStockSku, 1, 2, 4);

        quantitySimpleShipmentStockSku = addDProp(baseGroup, "quantitySimpleShipmentStockSku", "Кол-во оприход.", DoubleClass.instance,
                                                          simpleShipment, stock, sku);

        quantitySimpleShipmentSku = addSGProp(baseGroup, "quantitySimpleShipmentSku", "Всего оприход.", quantitySimpleShipmentStockSku, 1, 3);

        quantityBoxShipmentStockSku = addSGProp(baseGroup, "quantityBoxShipmentStockSku", "Кол-во оприход.", quantitySupplierBoxBoxShipmentStockSku, 2, 3, 4);

        quantityShipmentStockSku = addCUProp(baseGroup, "quantityShipmentStockSku", "Кол-во оприход.", quantitySimpleShipmentStockSku, quantityBoxShipmentStockSku);

        // freightBox
        routeFreightBox = addDProp(idGroup, "routeFreightBox", "Маршрут (ИД)", route, freightBox);
        nameRouteFreightBox = addJProp(baseGroup, "nameRouteFreightBox", "Маршрут", name, routeFreightBox, 1);

        quantityShipmentRouteSku = addSGProp(baseGroup, "quantityShipmentRouteSku", "Кол-во оприход.", quantityShipmentStockSku, 1, routeFreightBox, 2, 3);
        invoicedShipmentRouteSku = addPGProp(baseGroup, "invoicedShipmentRouteSku", false, 0, "Кол-во ожид.",
                                             percentShipmentRouteSku,
                                             invoicedShipmentSku, 1, 3);

        // Freight
        tonnageFreight = addDProp(baseGroup, "tonnageFreight", "Тоннаж", DoubleClass.instance, freight);
        quantityPalletFreight = addDProp(baseGroup, "quantityPalletFreight", "Кол-во паллет", IntegerClass.instance, freight);
        volumeFreight = addDProp(baseGroup, "volumeFreight", "Объём", DoubleClass.instance, freight);
        sumFreight = addDProp(baseGroup, "sumFreight", "Стоимость", DoubleClass.instance, freight);

        routeFreight = addDProp(idGroup, "routeFreight", "Маршрут (ИД)", route, freight);
        nameRouteFreight = addJProp(baseGroup, "nameRouteFreight", "Маршрут", name, routeFreight, 1);

        quantityPalletShipmentBetweenDate = addSGProp(baseGroup, "quantityPalletShipmentBetweenDate", "Кол-во паллет по поставкам за интервал",
              addJProp(and1, quantityPalletShipment, 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);
        quantityPalletFreightBetweenDate = addSGProp(baseGroup, "quantityPalletFreightBetweenDate", "Кол-во паллет по фрахтам за интервал",
              addJProp(and1, quantityPalletFreight, 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);

        quantityPalletShipmentFreight = addDProp(baseGroup, "quantityPalletShipmentFreight", "Кол-во паллет из поставки", IntegerClass.instance, shipment, freight);
        freighedShipment = addSGProp(baseGroup, "freighedShipment", "Распределено", quantityPalletShipmentFreight, 1);
        shipmentedFreight = addSGProp(baseGroup, "shipmentedFreight", "Использовано", quantityPalletShipmentFreight, 2);

        routeItem = addDProp(baseGroup, "routeItem", "Маршрут товара", route, item);
        palletFreightBox = addDProp(baseGroup, "palletFreightBox", "Палета", pallet, freightBox);
        routePallet = addDProp(baseGroup, "routePallet", "Маршрут", route, pallet);
        freightPallet = addDProp(baseGroup, "freightPallet", "Фрахт", freight, pallet);
        nameRoutePallet = addJProp(baseGroup, "nameRoutePallet", "Маршрут", name, routePallet, 1);
        addConstraint(addJProp("Маршрут паллеты должен совпадать с маршрутом фрахта", diff2,
                routePallet, 1, addJProp(routeFreight, freightPallet, 1), 1), true);

        currentPalletRoute = addDProp("currentPalletRoute", "Тек. палета", pallet, route);
        currentFreightBoxRoute = addDProp("currentFreightBoxRoute", "Тек. короб", freightBox, route);

        isCurrentFreightBox = addJProp(equals2, addJProp(true, currentFreightBoxRoute, routeFreightBox, 1), 1, 1);
        isCurrentPallet = addJProp(equals2, addJProp(true, currentPalletRoute, routePallet, 1), 1, 1);
        isCurrentPalletFreightBox = addJProp(equals2, palletFreightBox, 1, addJProp(currentPalletRoute, routeFreightBox, 1), 1);

        barcodeAction1 = addJProp(true, "Ввод штрих-кода 1", addCUProp(isCurrentFreightBox, isCurrentPallet), barcodeToObject, 1);
        barcodeActionSetPallet = addJProp(true, "Установить паллету", isCurrentPalletFreightBox, barcodeToObject, 1);

        LP currentFreightBoxItem = addJProp(currentFreightBoxRoute, routeItem, 1);

        quantitySupplierBoxBoxShipmentRouteSku = addJProp(baseGroup, true,  "quantitySupplierBoxBoxShipmentRouteSku", "Кол-во оприход.",
                                                    quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4);
        quantitySimpleShipmentRouteSku = addJProp(baseGroup, true,  "quantitySimpleShipmentRouteSku", "Кол-во оприход.",
                                                    quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3);

        barcodeAction3 = addJProp(true, "Ввод штрих-кода 3",
                addCUProp(
                        addSCProp(addJProp(true, quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxItem, 3, 3))
                ), 1, 2, barcodeToObject, 3);
        barcodeAction2 = addJProp(true, "Ввод штрих-кода 2",
                addCUProp(
                        addSCProp(addJProp(true, quantitySimpleShipmentStockSku, 1, currentFreightBoxItem, 2, 2))
                ), 1, barcodeToObject, 2);
    }

    LP quantitySupplierBoxBoxShipmentRouteSku;
    LP quantitySimpleShipmentRouteSku;
    LP routePallet, freightPallet, nameRoutePallet, palletFreightBox;
    LP currentPalletRoute;
    LP currentFreightBoxRoute;
    LP isCurrentFreightBox, isCurrentPalletFreightBox;
    LP isCurrentPallet;
    LP barcodeAction1, barcodeActionSetPallet, barcodeAction2;
    LP routeItem;

    @Override
    protected void initTables() {
    }

    @Override
    protected void initIndexes() {
    }

    @Override
    protected void initNavigators() throws JRException, FileNotFoundException {
        addFormEntity(new OrderFormEntity(baseElement, 10, "Заказы"));
        addFormEntity(new InvoiceFormEntity(baseElement, 20, "Инвойсы по коробам", true));
        addFormEntity(new InvoiceFormEntity(baseElement, 25, "Инвойсы без коробов", false));
        addFormEntity(new ShipmentListFormEntity(baseElement, 30, "Поставки по коробам", true));
        addFormEntity(new ShipmentListFormEntity(baseElement, 40, "Поставки без коробов", false));
        addFormEntity(new ShipmentSpecFormEntity(baseElement, 50, "Прием товара по коробам", true));
        addFormEntity(new ShipmentSpecFormEntity(baseElement, 60, "Прием товара без коробов", false));
        addFormEntity(new FreightFormEntity(baseElement, 70, "Фрахт по поставкам"));
    }


    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
        admin.addSecurityPolicy(permitAllPolicy);
    }

    private class BarcodeFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objBarcode;

        protected Font getDefaultFont() {
            return null;
        }

        private BarcodeFormEntity(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseGroup, true);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

            objBarcode.resetOnApply = true;

            addPropertyDraw(reverseBarcode);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            if (getDefaultFont() != null)
                design.setFont(getDefaultFont());

            PropertyDrawView barcodeView = design.get(getPropertyDraw(objectValue, objBarcode));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(reverseBarcode)));
            design.getPanelContainer(design.get(objBarcode.groupTo)).constraints.maxVariables = 0;

            design.setBackground(barcodeObjectName, new Color(240, 240, 240));

            design.setEditKey(barcodeView, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
            design.setEditKey(reverseBarcode, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

            design.setFocusable(reverseBarcode, false);
            design.setFocusable(false, objBarcode.groupTo);

            return design;
        }
    }

    private class OrderFormEntity extends FormEntity<RomanBusinessLogics> {
        private ObjectEntity objSupplier;
        private ObjectEntity objOrder;
        private ObjectEntity objSIDArticleComposite;
        private ObjectEntity objSIDArticleSingle;
        private ObjectEntity objArticle;
        private ObjectEntity objItem;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objColorSupplier;

        private OrderFormEntity(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, nameCurrencySupplier);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objOrder = addSingleGroupObject(order, "Заказ", date, sidDocument, nameCurrencyDocument, sumDocument, nameStoreOrder);
            addObjectActions(this, objOrder);

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            addAutoAction(objSIDArticleComposite, addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
            addAutoAction(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, objOrder, objSIDArticleComposite));
            addAutoAction(objSIDArticleComposite, addPropertyObject(seekArticleSIDSupplier, objSIDArticleComposite, objSupplier));

            objSIDArticleSingle = addSingleGroupObject(StringClass.get(50), "Ввод простого артикула", objectValue);
            objSIDArticleSingle.groupTo.setSingleClassView(ClassViewType.PANEL);

            addAutoAction(objSIDArticleSingle, addPropertyObject(addNEArticleSingleSIDSupplier, objSIDArticleSingle, objSupplier));
            addAutoAction(objSIDArticleSingle, addPropertyObject(incrementNumberListSID, objOrder, objSIDArticleSingle));
            addAutoAction(objSIDArticleSingle, addPropertyObject(seekArticleSIDSupplier, objSIDArticleSingle, objSupplier));

            objArticle = addSingleGroupObject(article, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, objOrder, objArticle);
            addPropertyDraw(objArticle, sidArticle, originalNameArticle, nameCountryOfOriginArticle, barcode);
            addPropertyDraw(quantityDocumentArticle, objOrder, objArticle);
            addPropertyDraw(priceDocumentArticle, objOrder, objArticle);
            addPropertyDraw(sumDocumentArticle, objOrder, objArticle);
            addPropertyDraw(invoicedOrderArticle, objOrder, objArticle);
            addPropertyDraw(delete, objArticle);

            objItem = addSingleGroupObject(item, "Товар", barcode, sidColorSupplierItem, nameColorSupplierItem, nameSizeSupplierItem);
            addObjectActions(this, objItem);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", selection, name);
            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", selection, sidColorSupplier, name);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityDocumentArticleCompositeColorSize, objOrder, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.columnGroupObjects.add(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(name, objSizeSupplier);

            addPropertyDraw(quantityDocumentSku, objOrder, objItem);
            addPropertyDraw(priceDocumentItem, objOrder, objItem);
            addPropertyDraw(invoicedOrderSku, objOrder, objItem);
            addPropertyDraw(quantityDocumentArticleCompositeColor, objOrder, objArticle, objColorSupplier);
            addPropertyDraw(quantityDocumentArticleCompositeSize, objOrder, objArticle, objSizeSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleItem, objItem), Compare.EQUALS, objArticle));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(numberListArticle, objOrder, objArticle)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(quantityDocumentArticleCompositeColor, objOrder, objArticle, objColorSupplier)),
                                  "Заказано",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroup);

            setReadOnly(objSupplier, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.defaultOrders.put(design.get(getPropertyDraw(numberListArticle)), true);

            design.get(getPropertyDraw(objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(objectValue, objSIDArticleSingle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

            design.get(objOrder.groupTo).grid.constraints.fillVertical = 0.5;
            design.get(objArticle.groupTo).grid.constraints.fillHorizontal = 4;
            design.get(objItem.groupTo).grid.constraints.fillHorizontal = 3;

            design.addIntersection(design.getGroupObjectContainer(objSIDArticleComposite.groupTo),
                                   design.getGroupObjectContainer(objSIDArticleSingle.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objArticle.groupTo),
                                   design.getGroupObjectContainer(objSizeSupplier.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objItem.groupTo),
                                   design.getGroupObjectContainer(objSizeSupplier.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objItem.groupTo),
                                   design.getGroupObjectContainer(objColorSupplier.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class InvoiceFormEntity extends FormEntity<RomanBusinessLogics> {
        private ObjectEntity objSupplier;
        private ObjectEntity objOrder;
        private ObjectEntity objInvoice;
        private ObjectEntity objSupplierBox;
        private ObjectEntity objSIDArticleComposite;
        private ObjectEntity objSIDArticleSingle;
        private ObjectEntity objArticle;
        private ObjectEntity objItem;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objColorSupplier;

        private InvoiceFormEntity(NavigatorElement parent, int iID, String caption, boolean box) {
            super(parent, iID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, nameCurrencySupplier);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс", date, sidDocument, nameCurrencyDocument, sumDocument, nameStoreOrder, quantityPalletShipment, netWeightShipment, grossWeightShipment);
            addObjectActions(this, objInvoice);

            objOrder = addSingleGroupObject(order, "Заказ");
            objOrder.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inOrderInvoice, objOrder, objInvoice);
            addPropertyDraw(objOrder, date, sidDocument, nameCurrencyDocument, sumDocument, nameStoreOrder);

            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб", sidSupplierBox, barcode);
                objSupplierBox.groupTo.initClassView = ClassViewType.PANEL;
                addObjectActions(this, objSupplierBox);
            }

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            addAutoAction(objSIDArticleComposite, addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
            addAutoAction(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleComposite));
            addAutoAction(objSIDArticleComposite, addPropertyObject(seekArticleSIDSupplier, objSIDArticleComposite, objSupplier));

            objSIDArticleSingle = addSingleGroupObject(StringClass.get(50), "Ввод простого артикула", objectValue);
            objSIDArticleSingle.groupTo.setSingleClassView(ClassViewType.PANEL);

            addAutoAction(objSIDArticleSingle, addPropertyObject(addNEArticleSingleSIDSupplier, objSIDArticleSingle, objSupplier));
            addAutoAction(objSIDArticleSingle, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleSingle));
            addAutoAction(objSIDArticleSingle, addPropertyObject(seekArticleSIDSupplier, objSIDArticleSingle, objSupplier));

            objArticle = addSingleGroupObject(article, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(objArticle, sidArticle, originalNameArticle, nameCountryOfOriginArticle, barcode);
            addPropertyDraw(quantityListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(priceDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(sumDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(orderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(priceOrderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(inDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(delete, objArticle);

            objItem = addSingleGroupObject(item, "Товар", barcode, sidColorSupplierItem, nameColorSupplierItem, nameSizeSupplierItem);
            addObjectActions(this, objItem);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", selection, name);
            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", selection, sidColorSupplier, name);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityDocumentArticleCompositeColorSize, objInvoice, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.columnGroupObjects.add(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(name, objSizeSupplier);

            addPropertyDraw(quantityListSku, (box ? objSupplierBox : objInvoice), objItem);
            addPropertyDraw(priceDocumentItem, objInvoice, objItem);
            addPropertyDraw(orderedInvoiceSku, objInvoice, objItem);
            addPropertyDraw(quantityDocumentArticleCompositeColor, objInvoice, objArticle, objColorSupplier);
            addPropertyDraw(quantityDocumentArticleCompositeSize, objInvoice, objArticle, objSizeSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));
            if (box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(boxDocumentSupplierBox, objSupplierBox), Compare.EQUALS, objInvoice));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleItem, objItem), Compare.EQUALS, objArticle));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(quantityDocumentArticleCompositeColor, objInvoice, objArticle, objColorSupplier)),
                                  "В инвойсе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroup);

            setReadOnly(objSupplier, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.defaultOrders.put(design.get(getPropertyDraw(numberListArticle)), true);

            design.get(objOrder.groupTo).grid.constraints.fillVertical = 0.5;
            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 0.5;
            design.get(objInvoice.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objArticle.groupTo).grid.constraints.fillHorizontal = 4;
            design.get(objItem.groupTo).grid.constraints.fillHorizontal = 3;

            design.get(getPropertyDraw(objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(objectValue, objSIDArticleSingle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

            design.addIntersection(design.getGroupObjectContainer(objInvoice.groupTo),
                                   design.getGroupObjectContainer(objOrder.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSIDArticleComposite.groupTo),
                                   design.getGroupObjectContainer(objSIDArticleSingle.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objArticle.groupTo),
                                   design.getGroupObjectContainer(objSizeSupplier.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objItem.groupTo),
                                   design.getGroupObjectContainer(objSizeSupplier.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objItem.groupTo),
                                   design.getGroupObjectContainer(objColorSupplier.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class ShipmentListFormEntity extends FormEntity {
        private boolean box;

        private ObjectEntity objSupplier;
        private ObjectEntity objShipment;
        private ObjectEntity objInvoice;
        private ObjectEntity objRoute;

        private ShipmentListFormEntity(NavigatorElement parent, int iID, String caption, boolean box) {
            super(parent, iID, caption);

            this.box = box;

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", date, sidDocument);
            addObjectActions(this, objShipment);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс");
            objInvoice.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inInvoiceShipment, objInvoice, objShipment);
            addPropertyDraw(objInvoice, date, sidDocument, nameStoreOrder);

            objRoute = addSingleGroupObject(route, "Маршрут", name);
            addPropertyDraw(percentShipmentRoute, objShipment, objRoute);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objShipment), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));

            setReadOnly(objSupplier, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objRoute.groupTo).grid.constraints.fillHorizontal = 0.3;

            design.addIntersection(design.getGroupObjectContainer(objInvoice.groupTo),
                                   design.getGroupObjectContainer(objRoute.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }
    
    private class ShipmentSpecFormEntity extends BarcodeFormEntity {
        private boolean box;

        private ObjectEntity objSIDSupplierBox;
        private ObjectEntity objSupplier;
        private ObjectEntity objShipment;
        private ObjectEntity objInvoice;
        private ObjectEntity objSupplierBox;
        private ObjectEntity objFreightBoxRB;
        private ObjectEntity objFreightBoxRF;
        private ObjectEntity objStock;
        private ObjectEntity objSku;
        private ObjectEntity objRoute;

        private ShipmentSpecFormEntity(NavigatorElement parent, int iID, String caption, boolean box) {
            super(parent, iID, caption);

            this.box = box;

            if (box) {
                objSIDSupplierBox = addSingleGroupObject(StringClass.get(50), "Код короба", objectValue);
                objSIDSupplierBox.groupTo.setSingleClassView(ClassViewType.PANEL);
            }

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", date, sidDocument);
            objShipment.groupTo.initClassView = ClassViewType.PANEL;

            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб поставщика", sidSupplierBox, barcode);
                objSupplierBox.groupTo.initClassView = ClassViewType.PANEL;
            }

            objRoute = addSingleGroupObject(route, "Маршрут", name, currentPalletRoute, currentFreightBoxRoute);
            objRoute.groupTo.setSingleClassView(ClassViewType.GRID);

            objStock = addSingleGroupObject(stock, "Место хранения", barcode, objectClassName, nameRouteFreightBox);
            objStock.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSku = addSingleGroupObject(sku, "SKU", barcode, sidArticleSku, originalNameArticleSku, nameArticleSku, nameCountryOfOriginSku, sidColorSupplierItem, nameColorSupplierItem, nameSizeSupplierItem, routeItem);

            getPropertyDraw(sidColorSupplierItem).forceViewType = ClassViewType.GRID;
            getPropertyDraw(nameColorSupplierItem).forceViewType = ClassViewType.GRID;
            getPropertyDraw(nameSizeSupplierItem).forceViewType = ClassViewType.GRID;

            addPropertyDraw(invoicedShipmentSku, objShipment, objSku);

            if (box) {
                addPropertyDraw(quantityListSku, objSupplierBox, objSku);
                addPropertyDraw(quantitySupplierBoxBoxShipmentSku, objSupplierBox, objShipment, objSku);
                addPropertyDraw(quantitySupplierBoxBoxShipmentRouteSku, objSupplierBox, objShipment, objRoute, objSku);
            } else {
                addPropertyDraw(quantitySimpleShipmentSku, objShipment, objSku);
                addPropertyDraw(quantitySimpleShipmentRouteSku, objShipment, objRoute, objSku);
            }

            addPropertyDraw(percentShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);
            addPropertyDraw(invoicedShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);
            addPropertyDraw(quantityShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objShipment), Compare.EQUALS, objSupplier));

            if (box)
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(inSupplierBoxShipment, objSupplierBox, objShipment)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            if (box) {
                FilterEntity inSupplierBox = new NotNullFilterEntity(addPropertyObject(quantityListSku, objSupplierBox, objSku));
                FilterEntity inSupplierBoxShipmentStock = new NotNullFilterEntity(addPropertyObject(quantitySupplierBoxBoxShipmentRouteSku, objSupplierBox, objShipment, objRoute, objSku));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      new OrFilterEntity(inSupplierBox, inSupplierBoxShipmentStock),
                                      "В коробе поставщика или оприходовано",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      inSupplierBox,
                                      "В коробе поставщика"));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      inSupplierBoxShipmentStock,
                                      "Оприходовано",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            } else {
                FilterEntity inInvoice = new NotNullFilterEntity(addPropertyObject(invoicedShipmentSku, objShipment, objSku));
                FilterEntity inInvoiceShipmentStock = new NotNullFilterEntity(addPropertyObject(quantitySimpleShipmentRouteSku, objShipment, objRoute, objSku));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      new OrFilterEntity(inInvoice, inInvoiceShipmentStock),
                                      "В инвойсах или оприходовано",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      inInvoice,
                                      "В инвойсах"));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      inInvoice,
                                      "Оприходовано",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            }
            filterGroup.defaultFilter = 0;
            addRegularFilterGroup(filterGroup);

            addAutoAction(objBarcode, addPropertyObject(barcodeAction1, objBarcode));
            addAutoAction(objBarcode, addPropertyObject(barcodeActionSetPallet, objBarcode));
            addAutoAction(objBarcode, addPropertyObject(barcodeAction3, objSupplierBox, objShipment, objBarcode));
            addAutoAction(objBarcode, addPropertyObject(barcodeAction2, objShipment, objBarcode));
            addAutoAction(objBarcode, addPropertyObject(seekBarcodeAction, objBarcode));
            addAutoAction(objBarcode, addPropertyObject(barcodeNotFoundMessage, objBarcode));
            if (box)
                addAutoAction(objSIDSupplierBox, addPropertyObject(seekSupplierBoxSIDSupplier, objSIDSupplierBox, objSupplier));

            setReadOnly(objSupplier, true);
            setReadOnly(objShipment, true);
            setReadOnly(barcode, true, objStock.groupTo);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            if (box)
                design.setEditKey(design.get(getPropertyDraw(objectValue, objSIDSupplierBox)), KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));

            design.get(objRoute.groupTo).grid.constraints.fillVertical = 0.2;
            design.get(objRoute.groupTo).grid.showFilter = false;

            if (box)
                design.addIntersection(design.getGroupObjectContainer(objBarcode.groupTo),
                                       design.getGroupObjectContainer(objSIDSupplierBox.groupTo),
                                       DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                                   design.getGroupObjectContainer(objShipment.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            if (box)
                design.addIntersection(design.getGroupObjectContainer(objSupplierBox.groupTo),
                                       design.getGroupObjectContainer(objStock.groupTo),
                                       DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            if (box)
                design.addIntersection(design.getGroupObjectContainer(objSupplierBox.groupTo),
                                       design.getGroupObjectContainer(objRoute.groupTo),
                                       DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objStock.groupTo),
                                   design.getGroupObjectContainer(objRoute.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objRoute.groupTo),
                                   design.getGroupObjectContainer(objSku.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_BOTTOM);

            return design;
        }
    }
    private class FreightFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objShipment;
        private ObjectEntity objFreight;
        private ObjectEntity objPallet;
        private ObjectEntity objDateFrom;
        private ObjectEntity objDateTo;
        
        private FreightFormEntity(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);

            objDateFrom = addSingleGroupObject(DateClass.instance, "Дата с", objectValue);
            objDateTo = addSingleGroupObject(DateClass.instance, "Дата по", objectValue);
            objDateFrom.groupTo.setSingleClassView(ClassViewType.PANEL);
            objDateTo.groupTo.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(quantityPalletShipmentBetweenDate, objDateFrom, objDateTo);
            addPropertyDraw(quantityPalletFreightBetweenDate, objDateFrom, objDateTo);

            objShipment = addSingleGroupObject(shipment, "Поставка", date, nameSupplierDocument, sidDocument, sumDocument, nameCurrencyDocument, quantityPalletShipment, freighedShipment, netWeightShipment, grossWeightShipment);
            objFreight = addSingleGroupObject(freight, "Фрахт", date, nameRouteFreight, tonnageFreight, quantityPalletFreight, shipmentedFreight, volumeFreight, sumFreight);
            addObjectActions(this, objFreight);
            objPallet = addSingleGroupObject(pallet, "Паллета", barcode, nameRoutePallet, freightPallet);

            addPropertyDraw(quantityPalletShipmentFreight, objShipment, objFreight);
           
            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objShipment), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objShipment), Compare.LESS_EQUALS, objDateTo));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objShipment.groupTo).grid.constraints.fillVertical = 4;
            design.get(objFreight.groupTo).grid.constraints.fillVertical = 4;
            design.get(objPallet.groupTo).grid.constraints.fillVertical = 4;

            design.addIntersection(design.getGroupObjectContainer(objDateFrom.groupTo),
                                   design.getGroupObjectContainer(objDateTo.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            return design;
        }
    }
}
