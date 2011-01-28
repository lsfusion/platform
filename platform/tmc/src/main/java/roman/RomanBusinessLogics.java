package roman;

import net.sf.jasperreports.engine.JRException;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.group.AbstractGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class RomanBusinessLogics extends BusinessLogics<RomanBusinessLogics> {

    private static boolean USE_SHIPMENT_DETAIL = true;

    private AbstractCustomClass article;
    private ConcreteCustomClass articleComposite;
    private ConcreteCustomClass articleSingle;
    private ConcreteCustomClass item;
    private AbstractCustomClass sku;
    private ConcreteCustomClass pallet;
    private LP sidArticle;
    private LP articleCompositeItem;
    private LP articleSku;
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
    private ConcreteCustomClass brandSupplier;
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
    private LP supplierBrandSupplier;
    private LP nameSupplierBrandSupplier;
    private LP brandSupplierSupplier;
    private LP nameBrandSupplierSupplier;
    private LP brandSupplierArticle;
    private LP nameBrandSupplierArticle;
    private LP supplierBrandSupplierArticle;
    private LP brandSupplierDataArticle;
    private LP brandSupplierSupplierArticle;
    private LP brandSupplierArticleSku;
    private LP nameBrandSupplierArticleSku;
    private LP nameBrandSupplierArticleSkuShipmentDetail;

    private LP supplierSku;
    private LP nameSupplierSku;
    private ConcreteCustomClass currency;
    private ConcreteCustomClass store;
    private ConcreteCustomClass unitOfMeasure;
    private LP unitOfMeasureArticle;
    private LP nameUnitOfMeasureArticle;
    private LP unitOfMeasureArticleSku;
    private LP nameUnitOfMeasureArticleSku;
    private LP currencySupplier;
    private LP nameCurrencySupplier;
    private LP currencyDocument;
    private LP nameCurrencyDocument;
    private LP destinationDestinationDocument;
    private LP nameDestinationDestinationDocument;
    private LP quantityPalletShipment;
    private LP grossWeightShipment;
    private LP netWeightShipment;
    private LP sidColorSupplier;
    private LP sidColorSupplierItem;
    private LP quantityDocumentSku;
    private LP quantityDocumentArticleCompositeColor;
    private LP quantityDocumentArticleCompositeSize;
    private LP quantityDocumentArticleCompositeColorSize;
    private LP originalNameArticle;
    private LP netWeightDataArticle;
    private LP grossWeightDataArticle;
    private LP netWeightOriginArticle;
    private LP grossWeightOriginArticle;
    private LP netWeightOriginArticleSku;
    private LP grossWeightOriginArticleSku;
    private LP netWeightArticleSku;
    private LP netWeightDataSku;
    private LP netWeightSku;
    private LP grossWeightArticleSku;
    private LP netWeightArticle;
    private LP netWeightSkuShipmentDetail;
    private LP grossWeightArticle;
    private LP mainCompositionDataSku;
    private LP additionalCompositionDataSku;
    private LP mainCompositionSku;
    private LP additionalCompositionSku;
    private ConcreteCustomClass category;
    private ConcreteCustomClass customCategory2;
    private ConcreteCustomClass customCategory4;
    private ConcreteCustomClass customCategory6;
    private ConcreteCustomClass customCategory9;
    private ConcreteCustomClass customCategory10;
    private ConcreteCustomClass customCategoryOrigin;
    private LP categoryArticle;
    private LP nameCategoryArticle;
    private LP categoryArticleSku;
    private LP nameCategoryArticleSku;
    private LP customCategory10ArticleSku;
    private LP sidCustomCategory2;
    private LP sidCustomCategory4;
    private LP sidCustomCategory6;
    private LP sidCustomCategory9;
    private LP sidCustomCategory10;
    private LP sidCustomCategoryOrigin;
    private LP customCategory2CustomCategory4;
    private LP customCategory4CustomCategory6;
    private LP customCategory6CustomCategory9;
    private LP customCategory9CustomCategory10;
    private LP customCategory6CustomCategory10;
    private LP customCategory6CustomCategoryOrigin;
    private LP customCategory10CustomCategoryOrigin;
    private LP sidCustomCategory10CustomCategoryOrigin;
    private LP sidCustomCategory2CustomCategory4;
    private LP sidCustomCategory4CustomCategory6;
    private LP sidCustomCategory6CustomCategory9;
    private LP sidCustomCategory9CustomCategory10;
    private LP sidCustomCategory6CustomCategoryOrigin;
    private LP nameCustomCategory2CustomCategory4;
    private LP nameCustomCategory4CustomCategory6;
    private LP nameCustomCategory6CustomCategory9;
    private LP nameCustomCategory9CustomCategory10;
    private LP nameCustomCategory6CustomCategoryOrigin;
    private LP relationCustomCategory10CustomCategoryOrigin;
    private LP relationCustomCategory10;
    private LP relationCustomCategoryOrigin;
    private LP customCategory10DataArticle;
    private LP customCategoryOriginDataArticle;
    private LP sidCustomCategory10Article;
    private LP customCategory10OriginArticle;
    private LP sidCustomCategory10OriginArticle;
    private LP sidCustomCategory10ArticleSku;
    private LP sidCustomCategory10OriginArticleSku;
    private LP customCategory10OriginArticleSku;
    private LP customCategory10Article;
    private LP mainCompositionArticle;
    private LP additionalCompositionArticle;
    private LP mainCompositionArticleSku;
    private LP additionalCompositionArticleSku;
    private LP countryOfOriginArticleSku;
    private LP countryOfOriginDataSku;
    private LP countryOfOriginSku;
    private LP nameCountryOfOriginSku;
    private LP countryOfOriginArticle;
    private LP nameCountryOfOriginArticle;
    private LP nameCountryOfOriginDataSku;
    private LP nameCountryOfOriginArticleSku;
    private LP articleSIDSupplier;
    private LP seekArticleSIDSupplier;
    private LP numberListArticle;
    private LP articleSIDList;
    private LP incrementNumberListSID;
    private LP addArticleSingleSIDSupplier;
    private LP addNEArticleSingleSIDSupplier;
    private LP addArticleCompositeSIDSupplier;
    private LP addNEArticleCompositeSIDSupplier;
    private LP numberListSIDArticle;
    private LP inOrderInvoice;
    private LP quantityOrderInvoiceSku;
    private LP orderedOrderInvoiceSku;
    private LP orderedInvoiceSku;
    private LP invoicedOrderSku;
    private LP invoicedOrderArticle;
    private LP inListSku;
    private LP quantityListSku;
    private LP orderedOrderInvoiceArticle;
    private LP orderedInvoiceArticle;
    private LP priceOrderInvoiceArticle;
    private LP priceOrderedInvoiceArticle;
    private ConcreteCustomClass supplierBox;
    private ConcreteCustomClass boxInvoice;
    private ConcreteCustomClass simpleInvoice;
    private LP sidSupplierBox;
    private AbstractCustomClass list;
    private LP quantityDataListSku;
    private LP boxInvoiceSupplierBox;
    private LP sidBoxInvoiceSupplierBox;
    private LP supplierSupplierBox;
    private LP supplierList;
    private LP orderedSupplierBoxSku;
    private LP quantityListArticle;
    private LP orderedSimpleInvoiceSku;
    private LP priceDataDocumentItem;
    private LP priceArticleDocumentSku;
    private LP priceDocumentSku;
    private LP sumDocumentSku;
    private LP inInvoiceShipment;
    private LP tonnageFreight;
    private LP palletCountFreight;
    private LP volumeFreight;
    private LP currencyFreight;
    private LP nameCurrencyFreight;
    private LP sumFreight;
    private LP routeFreight;
    private LP nameRouteFreight;
    private ConcreteCustomClass stock;
    private ConcreteCustomClass freightBox;
    private LP sidArticleSku;
    private LP originalNameArticleSku;
    private LP inSupplierBoxShipment;
    private LP quantitySupplierBoxBoxShipmentStockSku;
    private LP quantitySimpleShipmentStockSku;
    private LP barcodeAction4;
    private LP supplierBoxSIDSupplier;
    private LP seekSupplierBoxSIDSupplier;
    private LP quantityPalletShipmentBetweenDate;
    private LP quantityPalletFreightBetweenDate;
    private LP routeFreightBox;
    private LP nameRouteFreightBox;
    private LP quantityShipmentStockSku;
    private LP quantityShipmentRouteSku;
    private LP quantityShipDimensionShipmentSku;
    private LP supplierPriceDocument;
    private LP percentShipmentRoute;
    private LP percentShipmentRouteSku;
    private LP invoicedShipmentSku;
    private LP invoicedShipmentRouteSku;
    private LP documentList;
    private LP numberDocumentArticle;
    private ConcreteCustomClass shipDimension;
    private LP quantityShipDimensionShipmentStockSku;
    private LP quantityShipmentSku;
    private LP barcodeCurrentPalletRoute;
    private LP barcodeCurrentFreightBoxRoute;
    private LP equalsPalletFreight;
    private ConcreteCustomClass freightType;
    private ConcreteCustomClass creationFreightBox;
    private ConcreteCustomClass creationPallet;
    private LP quantityCreationPallet;
    private LP routeCreationPallet;
    private LP nameRouteCreationPallet;
    private LP creationPalletPallet;
    private LP routeCreationPalletPallet;
    private LP nameRouteCreationPalletPallet;
    private LP quantityCreationFreightBox;
    private LP routeCreationFreightBox;
    private LP nameRouteCreationFreightBox;
    private LP creationFreightBoxFreightBox;
    private LP routeCreationFreightBoxFreightBox;
    private LP nameRouteCreationFreightBoxFreightBox;
    private LP tonnageFreightType;
    private LP palletCountFreightType;
    private LP volumeFreightType;
    private LP freightTypeFreight;
    private LP nameFreightTypeFreight;
    private LP palletNumberFreight;
    private LP barcodePalletFreightBox;
    private LP freightBoxNumberPallet;
    private LP notFilledShipmentRouteSku;
    private LP routeToFillShipmentSku;
    private LP seekRouteToFillShipmentBarcode;
    private LP quantityShipmentArticle;
    private LP oneShipmentArticle;
    private LP oneShipmentArticleSku;
    private LP oneShipmentSku;
    private LP quantityBoxShipment;
    private LP quantityShipmentStock;
    private LP quantityShipmentPallet;
    private LP quantityShipmentFreight;
    private LP balanceStockSku;
    private LP quantityStockSku;
    private LP quantityRouteSku;   
    private AbstractCustomClass destinationDocument;
    private LP destinationSupplierBox;
    private LP destinationFreightBox;
    private LP nameDestinationFreightBox;
    private LP nameDestinationSupplierBox;
    private LP destinationCurrentFreightBoxRoute;
    private LP nameDestinationCurrentFreightBoxRoute;
    private LP quantityShipDimensionStock;
    private LP isStoreFreightBoxSupplierBox;
    private LP barcodeActionSetStore;
    private AbstractCustomClass destination;
    private AbstractCustomClass shipmentDetail;
    private ConcreteCustomClass boxShipmentDetail;
    private ConcreteCustomClass simpleShipmentDetail;
    private LP skuShipmentDetail;
    private LP boxShipmentBoxShipmentDetail;
    private LP simpleShipmentSimpleShipmentDetail;
    private LP quantityShipmentDetail;
    private LP stockShipmentDetail;
    private LP supplierBoxShipmentDetail;
    private LP barcodeSkuShipmentDetail;
    private LP shipmentShipmentDetail;
    private LP addBoxShipmentDetailSupplierBoxStockBarcode;
    private LP addBoxShipmentDetailSupplierBoxRouteBarcode;
    private LP articleShipmentDetail;
    private LP sidArticleShipmentDetail;
    private LP barcodeStockShipmentDetail;
    private LP barcodeSupplierBoxShipmentDetail;
    private LP sidSupplierBoxShipmentDetail;
    private LP routeFreightBoxShipmentDetail;
    private LP nameRouteFreightBoxShipmentDetail;
    private LP addSimpleShipmentDetailStockBarcode;
    private LP addSimpleShipmentDetailRouteBarcode;
    private AbstractGroup skuAttributeGroup;
    private AbstractGroup supplierAttributeGroup;
    private AbstractGroup intraAttributeGroup;
    private LP colorSupplierItemShipmentDetail;
    private LP sidColorSupplierItemShipmentDetail;
    private LP nameColorSupplierItemShipmentDetail;
    private LP sizeSupplierItemShipmentDetail;
    private LP nameSizeSupplierItemShipmentDetail;
    private AbstractGroup itemAttributeGroup;
    private LP originalNameArticleSkuShipmentDetail;
    private LP categoryArticleSkuShipmentDetail;
    private LP nameCategoryArticleSkuShipmentDetail;
    private LP customCategory10OriginArticleSkuShipmentDetail;
    private LP sidCustomCategory10OriginArticleSkuShipmentDetail;
    private LP customCategory10ArticleSkuShipmentDetail;
    private LP sidCustomCategory10ArticleSkuShipmentDetail;
    private LP netWeightOriginArticleSkuShipmentDetail;
    private LP grossWeightOriginArticleSkuShipmentDetail;
    private LP netWeightArticleSkuShipmentDetail;
    private LP grossWeightArticleSkuShipmentDetail;
    private LP countryOfOriginArticleSkuShipmentDetail;
    private LP nameCountryOfOriginArticleSkuShipmentDetail;
    private LP countryOfOriginSkuShipmentDetail;
    private LP nameCountryOfOriginSkuShipmentDetail;
    private LP mainCompositionArticleSkuShipmentDetail;
    private LP mainCompositionSkuShipmentDetail;
    private LP additionalCompositionSkuShipmentDetail;
    private LP unitOfMeasureArticleSkuShipmentDetail;
    private LP nameUnitOfMeasureArticleSkuShipmentDetail;
    private LP netWeightShipmentDetail;
    private LP userShipmentDetail;
    private LP nameUserShipmentDetail;
    private LP timeShipmentDetail;
    private LP createFreightBox;
    private LP createPallet;
    private ConcreteCustomClass transfer;
    private LP stockFromTransfer;
    private LP barcodeStockFromTransfer;
    private LP stockToTransfer;
    private LP barcodeStockToTransfer;
    private LP balanceStockFromTransferSku;
    private LP balanceStockToTransferSku;
    private LP quantityTransferSku;
    private LP outcomeTransferStockSku;
    private LP incomeTransferStockSku;
    private LP incomeStockSku;
    private LP outcomeStockSku;
    private AbstractCustomClass customCategory;
    private LP nameCustomCategory;
    private LP customCategoryOriginArticle;
    private LP customCategoryOriginArticleSku;
    private LP sidCustomCategoryOriginArticle;
    private LP sidCustomCategoryOriginArticleSku;
    private LP customCategoryOriginCustomCategory10;
    private LP customCategoryOriginOriginArticle;
    private LP sidCustomCategoryOriginCustomCategory10;
    private LP quantityBoxInvoiceBoxShipmentStockSku;
    private LP quantityInvoiceShipmentStockSku;
    private LP invoicedSimpleInvoiceSimpleShipmentStockSku;
    private LP quanitySimpleInvoiceSimpleShipmentStockSku;
    private LP quantityInvoiceStockSku;
    private ConcreteCustomClass importer;
    private LP importerInvoice;
    private LP nameImporterInvoice;
    private LP freightFreightBox;
    private LP quantityImporterStockSku;
    private LP quantityImporterFreightSku;
    private LP priceInInvoiceStockSku;
    private LP priceInImporterFreightSku;
    private LP netWeightImporterFreightSku;
    private LP priceFreightImporterFreightSku;

    public RomanBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void initGroups() {

        skuAttributeGroup = new AbstractGroup("Атрибуты SKU");
        baseGroup.add(skuAttributeGroup);

        itemAttributeGroup = new AbstractGroup("Атрибуты товара");
        baseGroup.add(itemAttributeGroup);

        supplierAttributeGroup = new AbstractGroup ("Атрибуты поставщика");
        publicGroup.add(supplierAttributeGroup);

        intraAttributeGroup = new AbstractGroup("Внутренние атрибуты");
        publicGroup.add(intraAttributeGroup);
    }

    @Override
    protected void initClasses() {

        currency = addConcreteClass("currency", "Валюта", baseClass.named);

        destination = addAbstractClass("destination", "Пункт назначения", baseClass);

        store = addConcreteClass("store", "Магазин", destination, baseClass.named);

        sku = addAbstractClass("sku", "SKU", barcodeObject);

        article = addAbstractClass("article", "Артикул", baseClass);
        articleComposite = addConcreteClass("articleComposite", "Артикул (составной)", article);
        articleSingle = addConcreteClass("articleSingle", "Артикул (простой)", sku, article);

        item = addConcreteClass("item", "Товар", sku);

        document = addAbstractClass("document", "Документ", transaction);
        list = addAbstractClass("list", "Список", baseClass);

        priceDocument = addAbstractClass("priceDocument", "Документ с ценами", document);
        destinationDocument = addAbstractClass("destinationDocument", "Документ в пункт назначения", document);

        order = addConcreteClass("order", "Заказ", priceDocument, destinationDocument, list);

        invoice = addAbstractClass("invoice", "Инвойс", priceDocument, destinationDocument);
        boxInvoice = addConcreteClass("boxInvoice", "Инвойс по коробам", invoice);
        simpleInvoice = addConcreteClass("simpleInvoice", "Инвойс без коробов", invoice, list);

        shipDimension = addConcreteClass("shipDimension", "Разрез поставки", baseClass);

        supplierBox = addConcreteClass("supplierBox", "Короб поставщика", list, shipDimension, barcodeObject);

        shipment = addAbstractClass("shipment", "Поставка", document);
        boxShipment = addConcreteClass("boxShipment", "Поставка по коробам", shipment);
        simpleShipment = addConcreteClass("simpleShipment", "Поставка без коробов", shipment, shipDimension);

        if (USE_SHIPMENT_DETAIL) {
            shipmentDetail = addAbstractClass("shipmentDetail", "Строка поставки", baseClass);
            boxShipmentDetail = addConcreteClass("boxShipmentDetail", "Строка поставки по коробам", shipmentDetail);
            simpleShipmentDetail = addConcreteClass("simpleShipmentDetail", "Строка поставки без коробов", shipmentDetail);
        }

        supplier = addConcreteClass("supplier", "Поставщик", baseClass.named);

        importer = addConcreteClass("importer", "Импортер", baseClass.named);

        colorSupplier = addConcreteClass("colorSupplier", "Цвет поставщика", baseClass.named);
        sizeSupplier = addConcreteClass("sizeSupplier", "Размер поставщика", baseClass.named);

        stock = addConcreteClass("stock", "Место хранения", barcodeObject);

        freightBox = addConcreteClass("freightBox", "Короб для транспортировки", stock);

        freight = addConcreteClass("freight", "Фрахт", baseClass.named, transaction);

        freightType = addConcreteClass("freightType", "Тип машины", baseClass.named);

        pallet = addConcreteClass("pallet", "Паллета", barcodeObject);

        category = addConcreteClass("category", "Категория", baseClass.named);

        customCategory = addAbstractClass("customCategory", "Уровень ТН ВЭД", baseClass);
        
        customCategory4 = addConcreteClass("customCategory4", "Первый уровень", customCategory);
        customCategory6 = addConcreteClass("customCategory6", "Второй уровень", customCategory);
        customCategory9 = addConcreteClass("customCategory9", "Третий уровень", customCategory);
        customCategory10 = addConcreteClass("customCategory10", "Четвёртый уровень", customCategory);

        customCategoryOrigin = addConcreteClass("customCategoryOrigin", "ЕС уровень", customCategory);

        creationFreightBox = addConcreteClass("creationFreightBox", "Операция создания коробов", transaction);
        creationPallet = addConcreteClass("creationPallet", "Операция создания паллет", transaction);

        transfer = addConcreteClass("transfer", "Внутреннее перемещение", baseClass);

        unitOfMeasure = addConcreteClass("unitOfMeasure", "Единица измерения", baseClass.named);

        brandSupplier = addConcreteClass("brandSupplier", "Бренд поставщика", baseClass.named);

        route = addStaticClass("route", "Маршрут", new String[]{"rb", "rf"}, new String[]{"РБ", "РФ"});
    }

    @Override
    protected void initProperties() {

        // CustomCategory
        nameCustomCategory = addDProp(baseGroup, "nameCustomCategory", "Наименование", StringClass.get(500), customCategory);

        sidCustomCategory4 = addDProp(baseGroup, "sidCustomCategory4", "Код(4)", StringClass.get(4), customCategory4);
        sidCustomCategory4.setFixedCharWidth(4);

        sidCustomCategory6 = addDProp(baseGroup, "sidCustomCategory6", "Код(6)", StringClass.get(6), customCategory6);
        sidCustomCategory6.setFixedCharWidth(6);

        sidCustomCategory9 = addDProp(baseGroup, "sidCustomCategory9", "Код(9)", StringClass.get(9), customCategory9);
        sidCustomCategory9.setFixedCharWidth(9);

        sidCustomCategory10 = addDProp(baseGroup, "sidCustomCategory10", "Код(10)", StringClass.get(10), customCategory10);
        sidCustomCategory10.setFixedCharWidth(10);

        sidCustomCategoryOrigin = addDProp(baseGroup, "sidCustomCategoryOrigin", "Код ЕС(10)", StringClass.get(10), customCategoryOrigin);
        sidCustomCategoryOrigin.setFixedCharWidth(10);

        customCategory4CustomCategory6 = addDProp(idGroup, "customCategory4CustomCategory6", "Код(4)", customCategory4, customCategory6);
        customCategory6CustomCategory9 = addDProp(idGroup, "customCategory6CustomCategory9", "Код(6)", customCategory6, customCategory9);
        customCategory9CustomCategory10 = addDProp(idGroup, "customCategory9CustomCategory10", "Код(9)", customCategory9, customCategory10);
        customCategory6CustomCategory10 = addJProp(idGroup, "customCategory6CustomCategory10", "Код(6)", customCategory6CustomCategory9, customCategory9CustomCategory10, 1);
        customCategory6CustomCategoryOrigin = addDProp(idGroup, "customCategory6CustomCategoryOrigin", "Код(6)", customCategory6, customCategoryOrigin);

        customCategory10CustomCategoryOrigin = addDProp(idGroup, "customCategory10CustomCategoryOrigin", "Код по умолчанию(ИД)", customCategory10, customCategoryOrigin);
        sidCustomCategory10CustomCategoryOrigin = addJProp(baseGroup, "sidCustomCategory10CustomCategoryOrigin", "Код по умолчанию", sidCustomCategory10, customCategory10CustomCategoryOrigin, 1);

        sidCustomCategory4CustomCategory6 = addJProp(baseGroup, "sidCustomCategory4CustomCategory6", "Код(4)", sidCustomCategory4, customCategory4CustomCategory6, 1);
        sidCustomCategory6CustomCategory9 = addJProp(baseGroup, "sidCustomCategory6CustomCategory9", "Код(6)", sidCustomCategory6, customCategory6CustomCategory9, 1);
        sidCustomCategory9CustomCategory10 = addJProp(baseGroup, "sidCustomCategory9CustomCategory10", "Код(9)", sidCustomCategory9, customCategory9CustomCategory10, 1);
        sidCustomCategory6CustomCategoryOrigin = addJProp(baseGroup, "sidCustomCategory6CustomCategoryOrigin", "Код(6)", sidCustomCategory6, customCategory6CustomCategoryOrigin, 1);

        nameCustomCategory4CustomCategory6 = addJProp(baseGroup, "nameCustomCategory4CustomCategory6", "Наименование(4)", nameCustomCategory, customCategory4CustomCategory6, 1);
        nameCustomCategory6CustomCategory9 = addJProp(baseGroup, "nameCustomCategory6CustomCategory9", "Наименование(6)", nameCustomCategory, customCategory6CustomCategory9, 1);
        nameCustomCategory9CustomCategory10 = addJProp(baseGroup, "nameCustomCategory9CustomCategory10", "Наименование(9)", nameCustomCategory, customCategory9CustomCategory10, 1);
        nameCustomCategory6CustomCategoryOrigin = addJProp(baseGroup, "nameCustomCategory6CustomCategoryOrigin", "Наименование(6)", nameCustomCategory, customCategory6CustomCategoryOrigin, 1);

        relationCustomCategory10CustomCategoryOrigin = addDProp(baseGroup, "relationCustomCategory10CustomCategoryOrigin", "Связь ТН ВЭД", LogicalClass.instance, customCategory10, customCategoryOrigin);

        //relationCustomCategory10 = addJProp(idGroup, "relationCustomCategory10", "Связь ТН ВЭД", and1, 1, relationCustomCategory10CustomCategoryOrigin, 1, 2);
        //relationCustomCategoryOrigin = addJProp(idGroup, "relationCustomCategoryOrigin", "Связь ТН ВЭД", and1, 2, relationCustomCategory10CustomCategoryOrigin, 1, 2);

        /*addConstraint(addJProp("Третий уровень для ЕС должен соответствовать третьему уровню для Бел", diff2,
                addJProp(customCategory6CustomCategoryOrigin, relationCustomCategoryOrigin, 1, 2),
                addJProp(customCategory6CustomCategory10, relationCustomCategory10, 1, 2), 1, 2), true);*/

        // Supplier
        currencySupplier = addDProp(idGroup, "currencySupplier", "Валюта (ИД)", currency, supplier);
        nameCurrencySupplier = addJProp(baseGroup, "nameCurrencySupplier", "Валюта", name, currencySupplier, 1);

        sidColorSupplier = addDProp(baseGroup, "sidColorSupplier", "Код", StringClass.get(50), colorSupplier);

        supplierColorSupplier = addDProp(idGroup, "supplierColorSupplier", "Поставщик (ИД)", supplier, colorSupplier);
        nameSupplierColorSupplier = addJProp(baseGroup, "nameSupplierColorSupplier", "Поставщик", name, supplierColorSupplier, 1);

        supplierSizeSupplier = addDProp(idGroup, "supplierSizeSupplier", "Поставщик (ИД)", supplier, sizeSupplier);
        nameSupplierSizeSupplier = addJProp(baseGroup, "nameSupplierSizeSupplier", "Поставщик", name, supplierSizeSupplier, 1);

        supplierBrandSupplier = addDProp(idGroup, "supplierBrandSupplier", "Поставщик (ИД)", supplier, brandSupplier);
        nameSupplierBrandSupplier = addJProp(baseGroup, "nameSupplierBrandSupplier", "Поставщик", name, supplierBrandSupplier, 1);

        brandSupplierSupplier = addDProp(idGroup, "brandSupplierSupplier", "Бренд (ИД)", brandSupplier, supplier);
        nameBrandSupplierSupplier = addJProp(baseGroup, "nameBrandSupplierSupplier", "Бренд по умолчанию", name, brandSupplierSupplier, 1);

        addConstraint(addJProp("Бренд по умолчанию для поставщика должен соответствовать брендам поставщика", diff2, 1, addJProp(supplierBrandSupplier, brandSupplierSupplier, 1), 1), true);

        supplierDocument = addDProp(idGroup, "supplierDocument", "Поставщик (ИД)", supplier, document);
        supplierPriceDocument = addJProp(idGroup, "supplierPricedDocument", "Поставщик(ИД)", and1, supplierDocument, 1, is(priceDocument), 1);
        nameSupplierDocument = addJProp(baseGroup, "nameSupplierDocument", "Поставщик", name, supplierDocument, 1);

        currencyDocument = addDCProp(idGroup, "currencyDocument", "Валюта (ИД)", currencySupplier, supplierPriceDocument, 1);
        //currencyDocument = addDProp(idGroup, "currencyDocument", "Валюта (ИД)", currency, order);
        nameCurrencyDocument = addJProp(baseGroup, "nameCurrencyDocument", "Валюта", name, currencyDocument, 1);
        
        // Order
        destinationDestinationDocument = addDProp(idGroup, "destinationDestinationDocument", "Пункт назначения (ИД)", destination, destinationDocument);
        nameDestinationDestinationDocument = addJProp(baseGroup, "nameDestinationDestinationDocument", "Пункт назначения", name, destinationDestinationDocument, 1);

        // Invoice
        importerInvoice = addDProp(idGroup, "importerDocument", "Импортер (ИД)", importer, invoice);
        nameImporterInvoice = addJProp(baseGroup, "nameImporterInvoice", "Импортер", name, importerInvoice, 1);

        // Shipment
        quantityPalletShipment = addDProp(baseGroup, "quantityPalletShipment", "Кол-во паллет", IntegerClass.instance, shipment);
        netWeightShipment = addDProp(baseGroup, "netWeightShipment", "Вес нетто", DoubleClass.instance, shipment);
        grossWeightShipment = addDProp(baseGroup, "grossWeightShipment", "Вес брутто", DoubleClass.instance, shipment);
        quantityBoxShipment = addDProp(baseGroup, "quantityBoxShipment", "Кол-во коробов", DoubleClass.instance, shipment);

        // Item
        articleCompositeItem = addDProp(idGroup, "articleCompositeItem", "Артикул (ИД)", articleComposite, item);
        articleSku = addCUProp(idGroup, "articleSku", "Артикул (ИД)", object(articleSingle), articleCompositeItem);

        // Article
        sidArticle = addDProp(baseGroup, "sidArticle", "Код", StringClass.get(50), article);
        sidArticleSku = addJProp(baseGroup, "sidArticleSku", "Код", sidArticle, articleSku, 1);

        originalNameArticle = addDProp(supplierAttributeGroup, "originalNameArticle", "Имя производителя (ориг.)", StringClass.get(50), article);
        originalNameArticleSku = addJProp(supplierAttributeGroup, "originalNameArticleSku", "Имя производителя (ориг.)", originalNameArticle, articleSku, 1);

        unitOfMeasureArticle = addDProp(idGroup, "unitOfMeasureArticle", "Единица измерения (ИД)", unitOfMeasure, article);
        nameUnitOfMeasureArticle = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticle", "Единица измерения", name, unitOfMeasureArticle, 1);
        unitOfMeasureArticleSku = addJProp(idGroup, true, "unitOfMeasureArticleSku", "Ед. изм. товара (ИД)", unitOfMeasureArticle, articleSku, 1);
        nameUnitOfMeasureArticleSku = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticleSku", "Ед. изм. товара", name, unitOfMeasureArticleSku, 1);

        //Category
        categoryArticle = addDProp(idGroup, "categoryArticle", "Категория товара (ИД)", category, article);
        nameCategoryArticle = addJProp(intraAttributeGroup, "nameCategoryArticle", "Категория товара", name, categoryArticle, 1);
        categoryArticleSku = addJProp(idGroup, true, "categoryArticleSku", "Категория товара (ИД)", categoryArticle, articleSku, 1);
        nameCategoryArticleSku = addJProp(intraAttributeGroup, "nameCategoryArticleSku", "Категория товара", name, categoryArticleSku, 1);

        customCategoryOriginArticle = addDProp(idGroup, "customCategoryOriginArticle", "ТН ВЭД (ориг.) (ИД)", customCategoryOrigin, article);
        sidCustomCategoryOriginArticle = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticle", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategoryOriginArticleSku = addJProp(idGroup, "customCategoryOriginArticleSku", "ТН ВЭД (ориг.) (ИД)", customCategoryOriginArticle, articleSku, 1);
        sidCustomCategoryOriginArticleSku = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticleSku", "Код ТН ВЭД (ориг.)", sidCustomCategoryOriginArticle, articleSku, 1);

        customCategory10DataArticle = addDProp(idGroup, "customCategory10DataArticle", "ТН ВЭД (ИД)", customCategory10, article);
        //customCategory10OriginArticle = addJProp(idGroup, "customCategoryAdditionalOriginArticle", "Код ТН ВЭД (ИД)", customCategoryAdditionalCustomCategory10, customCategory10OriginArticle, 1);
        //customCategory10Article = addSUProp(idGroup, "customCategoryAdditionalArticle", "ТН ВЭД (ИД)", Union.OVERRIDE, customCategoryAdditionalOriginArticle, customCategoryAdditionalDataArticle);

        customCategory10ArticleSku = addJProp(idGroup, true, "customCategoryAdditionalArticleSku", "ТН ВЭД (ИД)", customCategory10DataArticle, articleSku, 1);
        sidCustomCategory10ArticleSku = addJProp(intraAttributeGroup, "sidCustomCategoryAdditionalArticleSku", "Код ТН ВЭД", sidCustomCategory10, customCategory10ArticleSku, 1);

        // Weight
        netWeightArticle = addDProp(supplierAttributeGroup, "netWeightOriginArticle", "Вес нетто (ориг.)", DoubleClass.instance, article);
        netWeightDataSku = addDProp(intraAttributeGroup, "netWeightDataArticle", "Вес нетто", DoubleClass.instance, sku);
        netWeightArticleSku = addJProp(supplierAttributeGroup, "netWeightArticleSku", "Вес нетто", netWeightArticle, articleSku, 1);
        netWeightSku = addSUProp(intraAttributeGroup, "netWeightArticle", true, "Вес нетто", Union.OVERRIDE, netWeightArticleSku, netWeightDataSku);

        //grossWeightOriginArticle = addDProp(supplierAttributeGroup, "grossWeightOriginArticle", "Вес брутто (ориг.)", DoubleClass.instance, article);
        //grossWeightDataArticle = addDProp(intraAttributeGroup, "grossWeightDataArticle", "Вес брутто", DoubleClass.instance, article);
        //grossWeightArticle = addSUProp(intraAttributeGroup, "grossWeightArticle", "Вес брутто", Union.OVERRIDE, grossWeightOriginArticle, grossWeightDataArticle);
        //grossWeightOriginArticleSku = addJProp(supplierAttributeGroup, "grossWeightOriginArticleSku", "Вес брутто (ориг.)", grossWeightOriginArticle, articleSku, 1);
        //grossWeightArticleSku = addJProp(intraAttributeGroup, true, "grossWeightArticleSku", "Вес брутто", grossWeightArticle, articleSku, 1);

        // Composition
        mainCompositionArticle = addDProp(supplierAttributeGroup, "mainCompositionArticle", "Состав", StringClass.get(100), article);
        additionalCompositionArticle = addDProp(supplierAttributeGroup, "additionalCompositionArticle", "Доп. состав", StringClass.get(100), article);

        mainCompositionArticleSku = addJProp(supplierAttributeGroup, "mainCompositionArticleSku", "Состав", mainCompositionArticle, articleSku, 1);
        additionalCompositionArticleSku = addJProp(supplierAttributeGroup, "additionalCompositionArticleSku", "Доп. состав", additionalCompositionArticle, articleSku, 1);

        mainCompositionDataSku = addDProp(intraAttributeGroup, "mainCompositionDataSku", "Состав", StringClass.get(100), sku);
        additionalCompositionDataSku = addDProp(intraAttributeGroup, "additionalCompositionDataSku", "Доп. состав", StringClass.get(100), sku);

        mainCompositionSku = addSUProp(intraAttributeGroup, "mainCompositionSku", "Состав", Union.OVERRIDE, mainCompositionArticleSku, mainCompositionDataSku);
        additionalCompositionSku = addSUProp(intraAttributeGroup, "additionalCompositionSku", "Доп. состав", Union.OVERRIDE, additionalCompositionArticleSku, additionalCompositionDataSku);

        // Country
        countryOfOriginArticle = addDProp(idGroup, "countryOfOriginArticle", "Страна происхождения (ИД)", country, article);
        nameCountryOfOriginArticle = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticle", "Страна происхождения (ориг.)", name, countryOfOriginArticle, 1);

        countryOfOriginArticleSku = addJProp(idGroup, "countryOfOriginArticleSku", "Страна происхождения (ИД)", countryOfOriginArticle, articleSku, 1);
        nameCountryOfOriginArticleSku = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSku", "Страна происхождения", name, countryOfOriginArticleSku, 1);

        countryOfOriginDataSku = addDProp(idGroup, "countryOfOriginDataSku", "Страна происхождения (ИД) (первичное)", country, sku);

        countryOfOriginSku = addSUProp(idGroup, "countryOfOriginSku", "Страна происхождения (ИД)", Union.OVERRIDE, countryOfOriginArticleSku, countryOfOriginDataSku);
        nameCountryOfOriginSku = addJProp(intraAttributeGroup, "nameCountryOfOriginSku", "Страна происхождения", name, countryOfOriginSku, 1);

        // Supplier
        supplierArticle = addDProp(idGroup, "supplierArticle", "Поставщик (ИД)", supplier, article);
        nameSupplierArticle = addJProp(baseGroup, "nameSupplierArticle", "Поставщик", name, supplierArticle, 1);

        brandSupplierDataArticle = addDProp(idGroup, "brandSupplierDataArticle", "Бренд (ИД)", brandSupplier, article);
        brandSupplierSupplierArticle = addJProp(idGroup, "brandSupplierSupplierArticle", "Бренд (ИД)", brandSupplierSupplier, supplierArticle, 1);
        brandSupplierArticle = addSUProp(idGroup, "brandSupplierArticle", "Бренд (ИД)", Union.OVERRIDE, brandSupplierSupplierArticle, brandSupplierDataArticle);
        nameBrandSupplierArticle = addJProp(supplierAttributeGroup, "nameBrandSupplierArticle", "Бренд", name, brandSupplierArticle, 1);

        supplierBrandSupplierArticle = addJProp(idGroup, "supplierBrandSupplierArticle", "Поставщик", supplierBrandSupplier , brandSupplierArticle, 1);
        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику бренда артикула", diff2,
                supplierArticle, 1, addJProp(supplierBrandSupplier, brandSupplierArticle, 1), 1), true);

        brandSupplierArticleSku = addJProp(idGroup, "brandSupplierArticleSku", "Бренд (ИД)", brandSupplierArticle, articleSku, 1);
        nameBrandSupplierArticleSku = addJProp(supplierAttributeGroup, "nameBrandSupplierArticleSku", "Бренд", name, brandSupplierArticleSku, 1);

        articleSIDSupplier = addCGProp(idGroup, "articleSIDSupplier", "Артикул (ИД)", object(article), sidArticle, sidArticle, 1, supplierArticle, 1);

        seekArticleSIDSupplier = addJProp(true, "Поиск артикула", addSAProp(null), articleSIDSupplier, 1, 2);

        addArticleSingleSIDSupplier = addJProp(true, "Ввод простого артикула", addAAProp(articleSingle, sidArticle, supplierArticle), 1, 2);
        addNEArticleSingleSIDSupplier = addJProp(true, "Ввод простого артикула (НС)", andNot1, addArticleSingleSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);

        addArticleCompositeSIDSupplier = addJProp(true, "Ввод составного артикула", addAAProp(articleComposite, sidArticle, supplierArticle), 1, 2);
        addNEArticleCompositeSIDSupplier = addJProp(true, "Ввод составного артикула (НС)", andNot1, addArticleCompositeSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);
        
        supplierSku = addJProp(idGroup, "supplierSku", "Поставщик (ИД)", supplierArticle, articleSku, 1);
        nameSupplierSku = addJProp(baseGroup, "nameSupplierSku", "Поставщик", name, supplierSku, 1);

        colorSupplierItem = addDProp(idGroup, "colorSupplierItem", "Цвет поставщика (ИД)", colorSupplier, item);
        sidColorSupplierItem = addJProp(itemAttributeGroup, "sidColorSupplierItem", "Код цвета", sidColorSupplier, colorSupplierItem, 1);
        nameColorSupplierItem = addJProp(itemAttributeGroup, "nameColorSupplierItem", "Цвет поставщика", name, colorSupplierItem, 1);

        sizeSupplierItem = addDProp(itemAttributeGroup, "sizeSupplierItem", "Размер поставщика (ИД)", sizeSupplier, item);
        nameSizeSupplierItem = addJProp(itemAttributeGroup, "nameSizeSupplierItem", "Размер поставщика", name, sizeSupplierItem, 1);

        addConstraint(addJProp("Поставщик товара должен соответствовать цвету поставщика", diff2,
                supplierSku, 1,
                addJProp(supplierColorSupplier, colorSupplierItem, 1), 1), true);

        addConstraint(addJProp("Поставщик товара должен соответствовать размеру поставщика", diff2,
                supplierSku, 1,
                addJProp(supplierSizeSupplier, sizeSupplierItem, 1), 1), true);

        sidDocument = addDProp(baseGroup, "sidDocument", "Код", StringClass.get(50), document);

        // коробки
        sidSupplierBox = addDProp(baseGroup, "sidSupplierBox", "Код", StringClass.get(50), supplierBox);

        boxInvoiceSupplierBox = addDProp(idGroup, "boxInvoiceSupplierBox", "Документ по коробам (ИД)", boxInvoice, supplierBox);
        sidBoxInvoiceSupplierBox = addJProp(baseGroup, "sidBoxInvoiceSupplierBox", "Документ по коробам", sidDocument, boxInvoiceSupplierBox, 1);

        destinationSupplierBox = addJProp(idGroup, "destinationSupplierBox", "Пункт назначения (ИД)", destinationDestinationDocument, boxInvoiceSupplierBox, 1);
        nameDestinationSupplierBox = addJProp(baseGroup, "nameDestinationSupplierBox", "Пункт назначения", name, destinationSupplierBox, 1);

        supplierSupplierBox = addJProp(idGroup, "supplierSupplierBox", "Поставщик (ИД)", supplierDocument, boxInvoiceSupplierBox, 1);

        supplierBoxSIDSupplier = addCGProp(idGroup, "supplierBoxSIDSupplier", "Короб поставщика (ИД)", object(supplierBox), sidSupplierBox, sidSupplierBox, 1, supplierSupplierBox, 1);

        seekSupplierBoxSIDSupplier = addJProp(true, "Поиск короба поставщика", addSAProp(null), supplierBoxSIDSupplier, 1, 2);

        // заказ по артикулам

        documentList = addCUProp(idGroup, "documentList", "Документ (ИД)", object(order), object(simpleInvoice), boxInvoiceSupplierBox);
        supplierList = addJProp(idGroup, "supplierList", "Поставщик (ИД)", supplierDocument, documentList, 1);

        articleSIDList = addJProp(idGroup, "articleSIDList", "Артикул (ИД)", articleSIDSupplier, 1,
                supplierList, 2);

        numberListArticle = addDProp(baseGroup, "numberListArticle", "Номер", IntegerClass.instance, list, article);
        numberListSIDArticle = addJProp(numberListArticle, 1, articleSIDList, 2, 1);

        inListSku = addJProp("inListSku", "Вкл", numberListArticle, 1, articleSku, 2);

        numberDocumentArticle = addSGProp(baseGroup, "inDocumentArticle", numberListArticle, documentList, 1, 2);

        incrementNumberListSID = addJProp(true, "Добавить строку", andNot1,
                                                  addJProp(true, addIAProp(numberListArticle, 1),
                                                  1, articleSIDList, 2, 1), 1, 2,
                numberListSIDArticle, 1, 2); // если еще не было добавлено такой строки

        // кол-во заказа
        quantityDataListSku = addDProp("quantityDataListSku", "Кол-во (первичное)", DoubleClass.instance, list, sku);
        quantityListSku = addJProp(baseGroup, "quantityListSku", true, "Кол-во", and1, quantityDataListSku, 1, 2, inListSku, 1, 2);

        quantityDocumentSku = addSGProp(baseGroup, "quantityDocumentSku", "Кол-во в документе", quantityListSku, documentList, 1, 2);

        // связь инвойсов и заказов
        inOrderInvoice = addDProp(baseGroup, "inOrderInvoice", "Вкл", LogicalClass.instance, order, invoice);

        addConstraint(addJProp("Магазин инвойса должен совпадать с магазином заказа", and1,
                      addJProp(diff2, destinationDestinationDocument, 1, destinationDestinationDocument, 2), 1, 2,
                      inOrderInvoice, 1, 2), true);

        orderedOrderInvoiceSku = addJProp(and1, quantityDocumentSku, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceSku = addSGProp(baseGroup, "orderedInvoiceSku", "Кол-во заказано", orderedOrderInvoiceSku, 2, 3);
        orderedSimpleInvoiceSku = addJProp(baseGroup, "orderedSimpleInvoiceSku", "Кол-во заказано", and1, orderedInvoiceSku, 1, 2, is(simpleInvoice), 1);
        // здесь на самом деле есть ограничение, что supplierBox ссылается именно на invoice
        orderedSupplierBoxSku = addJProp("orderedSupplierBoxSku", "Кол-во заказано", orderedInvoiceSku, boxInvoiceSupplierBox, 1, 2);


        // todo : переделать на PGProp, здесь надо derive'ить, иначе могут быть проблемы с расписыванием
        // если включаешь, то начинает тормозить изменение количества в заказах
//        quantityOrderInvoiceSku = addPGProp(baseGroup, "quantityOrderInvoiceSku", true, 0, "Кол-во по заказу/инвойсу (расч.)",
//                orderedOrderInvoiceSku,
//                quantityDocumentSku, 2, 3);

        quantityOrderInvoiceSku = addDProp(baseGroup, "quantityOrderInvoiceSku", "Кол-во по заказу/инвойсу (расч.)", DoubleClass.instance,
                                           order, invoice, sku);

        invoicedOrderSku = addSGProp(baseGroup, "invoicedOrderSku", "Выставлено инвойсов", quantityOrderInvoiceSku, 1, 3);

        // todo : не работает на инвойсе/простом товаре
        quantityListArticle = addDGProp(baseGroup, "quantityListArticle", "Кол-во",
                1, false, // кол-во объектов для порядка и ascending/descending
                quantityListSku, 1, articleSku, 2,
                addCUProp(addCProp(DoubleClass.instance, Double.MAX_VALUE, list, articleSingle),
                                addCProp(DoubleClass.instance, Double.MAX_VALUE, order, item),
                                addJProp(and1, orderedSimpleInvoiceSku, 1, 2, is(item), 2), // если не артикул (простой), то пропорционально заказано
                                addJProp(and1, orderedSupplierBoxSku, 1, 2, is(item), 2)), 1, 2, // ограничение (максимально-возможное число)
                2);

        quantityDocumentArticleCompositeColor = addSGProp(baseGroup, "quantityDocumentArticleCompositeColor", "Кол-во", quantityDocumentSku, 1, articleCompositeItem, 2, colorSupplierItem, 2);
        quantityDocumentArticleCompositeSize = addSGProp(baseGroup, "quantityDocumentArticleCompositeSize", "Кол-во", quantityDocumentSku, 1, articleCompositeItem, 2, sizeSupplierItem, 2);

        quantityDocumentArticleCompositeColorSize = addDGProp(baseGroup, "quantityDocumentArticleCompositeColorSize", "Кол-во",
                1, false,
                quantityDocumentSku, 1, articleCompositeItem, 2, colorSupplierItem, 2, sizeSupplierItem, 2,
                addCProp(DoubleClass.instance, Double.MAX_VALUE, document, sku), 1, 2,
                2);
        quantityDocumentArticleCompositeColorSize.property.setFixedCharWidth(2);

        orderedOrderInvoiceArticle = addJProp(and1, quantityListArticle, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceArticle = addSGProp(baseGroup, "orderedInvoiceArticle", "Кол-во заказано", orderedOrderInvoiceArticle, 2, 3);
        // todo : сделать, чтобы работало автоматическое проставление
//        quantityDocumentArticle.setDerivedChange(orderedInvoiceArticle, 1, 2, numberListArticle, 1, 2);

        invoicedOrderArticle = addSGProp(baseGroup, "invoicedOrderArticle", "Выставлено инвойсов", invoicedOrderSku, 1, articleSku, 2);

        // цены
        priceDocumentArticle = addDProp(baseGroup, "priceDocumentArticle", "Цена", DoubleClass.instance, priceDocument, article);
        priceDataDocumentItem = addDProp(baseGroup, "priceDataDocumentItem", "Цена по товару", DoubleClass.instance, priceDocument, item);
        priceArticleDocumentSku = addJProp(baseGroup, "priceArticleDocumentItem", "Цена по артикулу", priceDocumentArticle, 1, articleSku, 2);
        priceDocumentSku = addSUProp(baseGroup, "priceDocumentSku", "Цена", Union.OVERRIDE, priceArticleDocumentSku, priceDataDocumentItem);

        priceOrderInvoiceArticle = addJProp(and1, priceDocumentArticle, 1, 3, inOrderInvoice, 1, 2);
        priceOrderedInvoiceArticle = addMGProp(baseGroup, "priceOrderedInvoiceArticle", "Цена в заказе", priceOrderInvoiceArticle, 2, 3);
        // todo : не работает
        priceDocumentArticle.setDerivedChange(priceOrderedInvoiceArticle, 1, 2, numberDocumentArticle, 1, 2);

        sumDocumentSku = addJProp(baseGroup, "sumDocumentSku", "Сумма", multiplyDouble2, quantityDocumentSku, 1, 2, priceDocumentSku, 1, 2);

        sumDocumentArticle = addSGProp(baseGroup, "sumDocumentArticle", "Сумма", sumDocumentSku, 1, articleSku, 2);
        sumDocument = addSGProp(baseGroup, "sumDocument", "Сумма документа", sumDocumentSku, 1);

        // route
        percentShipmentRoute = addDProp(baseGroup, "percentShipmentRoute", "Процент", DoubleClass.instance, shipment, route);

        percentShipmentRouteSku = addJProp(baseGroup, "percentShipmentRouteSku", "Процент", and1, percentShipmentRoute, 1, 2, is(sku), 3);

        // creation
        quantityCreationFreightBox = addDProp(baseGroup, "quantityCreationFreightBox", "Количество", IntegerClass.instance, creationFreightBox);
        routeCreationFreightBox = addDProp(idGroup, "routeCreationFreightBox", "Маршрут (ИД)", route, creationFreightBox);
        nameRouteCreationFreightBox = addJProp(baseGroup, "nameRouteCreationFreightBox", "Маршрут", name, routeCreationFreightBox, 1);

        quantityCreationPallet = addDProp(baseGroup, "quantityCreationPallet", "Количество", IntegerClass.instance, creationPallet);
        routeCreationPallet = addDProp(idGroup, "routeCreationPallet", "Маршрут (ИД)", route, creationPallet);
        nameRouteCreationPallet = addJProp(baseGroup, "nameRouteCreationPallet", "Маршрут", name, routeCreationPallet, 1);

        // паллеты
        creationPalletPallet = addDProp(idGroup, "creationPalletPallet", "Операция (ИД)",  creationPallet, pallet);
        routeCreationPalletPallet = addJProp(idGroup, "routeCreationPalletPallet", true, "Маршрут (ИД)", routeCreationPallet, creationPalletPallet, 1);
        nameRouteCreationPalletPallet = addJProp(baseGroup, "nameRouteCreationPalletPallet", true, "Маршрут", name, routeCreationPalletPallet, 1);

        freightPallet = addDProp(baseGroup, "freightPallet", "Фрахт (ИД)", freight, pallet);
        equalsPalletFreight = addJProp(baseGroup, "equalsPalletFreight", "Вкл.", equals2, freightPallet, 1, 2);

        // freight box
        creationFreightBoxFreightBox = addDProp(idGroup, "creationFreightBoxFreightBox", "Операция (ИД)",  creationFreightBox, freightBox);

        palletFreightBox = addDProp(idGroup, "palletFreightBox", "Паллета (ИД)", pallet, freightBox);
        barcodePalletFreightBox = addJProp(baseGroup, "barcodePalletFreightBox", "Паллета (штрих-код)", barcode, palletFreightBox, 1);

        routeCreationFreightBoxFreightBox = addJProp(idGroup, "routeCreationFreightBoxFreightBox", true, "Маршрут (ИД)", routeCreationFreightBox, creationFreightBoxFreightBox, 1);
        nameRouteCreationFreightBoxFreightBox = addJProp(baseGroup, "nameRouteCreationFreightBoxFreightBox", true, "Маршрут", name, routeCreationFreightBoxFreightBox, 1);

        freightFreightBox = addJProp(idGroup, "freightFreightBox", "Фрахт короба транспортировки", freightPallet, palletFreightBox, 1);

        //routeFreightBox = addDProp(idGroup, "routeFreightBox", "Маршрут (ИД)", route, freightBox);
        //nameRouteFreightBox = addJProp(baseGroup, "nameRouteFreightBox", "Маршрут", name, routeFreightBox, 1);

        destinationFreightBox = addDProp(idGroup, "destinationFreightBox", "Пункт назначения (ИД)", destination, freightBox);
        nameDestinationFreightBox = addJProp(baseGroup, "nameDestinationFreightBox", "Пункт назначения", name, destinationFreightBox, 1);

        // поставка на склад
        inInvoiceShipment = addDProp(baseGroup, "inInvoiceShipment", "Вкл", LogicalClass.instance, invoice, shipment);

        inSupplierBoxShipment = addJProp(baseGroup, "inSupplierBoxShipment", "Вкл", inInvoiceShipment, boxInvoiceSupplierBox, 1, 2);

        invoicedShipmentSku = addSGProp(baseGroup, "invoicedShipmentSku", "Ожид. (пост.)",
                addJProp(and1, quantityDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        if (USE_SHIPMENT_DETAIL) {

            //sku shipment detail
            skuShipmentDetail = addDProp(idGroup, "skuShipmentDetail", "SKU (ИД)", sku, shipmentDetail);
            barcodeSkuShipmentDetail = addJProp(baseGroup, "barcodeSkuShipmentDetail", "Штрих-код SKU", barcode, skuShipmentDetail, 1);

            articleShipmentDetail = addJProp(idGroup, "articleShipmentDetail", "Артикул (ИД)", articleSku, skuShipmentDetail, 1);
            sidArticleShipmentDetail = addJProp(baseGroup, "sidArticleShipmentDetail", "Артикул", sidArticle, articleShipmentDetail, 1);

            colorSupplierItemShipmentDetail = addJProp(idGroup, "colorSupplierItemShipmentDetail", "Цвет поставщика (ИД)", colorSupplierItem, skuShipmentDetail, 1);
            sidColorSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "sidColorSupplierItemShipmentDetail", "Код цвета", sidColorSupplier, colorSupplierItemShipmentDetail, 1);
            nameColorSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "nameColorSupplierItemShipmentDetail", "Цвет поставщика", name, colorSupplierItemShipmentDetail, 1);

            sizeSupplierItemShipmentDetail = addJProp(idGroup, "sizeSupplierItemShipmentDetail", "Размер поставщика (ИД)", sizeSupplierItem, skuShipmentDetail, 1);
            nameSizeSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "nameSizeSupplierItemShipmentDetail", "Размер поставщика", name, sizeSupplierItemShipmentDetail, 1);

            nameBrandSupplierArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "nameBrandSupplierArticleSkuShipmentDetail", "Бренд", nameBrandSupplierArticleSku, skuShipmentDetail, 1);
            originalNameArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "originalNameArticleSkuShipmentDetail", "Имя производителя (ориг.)", originalNameArticleSku, skuShipmentDetail, 1);

            categoryArticleSkuShipmentDetail = addJProp(idGroup, true, "categoryArticleSkuShipmentDetail", "Категория товара (ИД)", categoryArticleSku, skuShipmentDetail, 1);
            nameCategoryArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameCategoryArticleSkuShipmentDetail", "Категория товара", name, categoryArticleSkuShipmentDetail, 1);

            //customCategory10OriginArticleSkuShipmentDetail = addJProp(idGroup, true, "customCategory10OriginArticleSkuShipmentDetail", "ТН ВЭД (ориг.) (ИД)", customCategory10OriginArticleSku, skuShipmentDetail, 1);
            //sidCustomCategory10OriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "sidCustomCategory10OriginArticleSkuShipmentDetail", "Код ТН ВЭД (ориг.)", sidCustomCategory10, customCategory10OriginArticleSkuShipmentDetail, 1);

            customCategory10ArticleSkuShipmentDetail = addJProp(idGroup, true, "customCategory10ArticleSkuShipmentDetail", "ТН ВЭД (ИД)", customCategory10ArticleSku, skuShipmentDetail, 1);
            sidCustomCategory10ArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "sidCustomCategory10ArticleSkuShipmentDetail", "Код ТН ВЭД", sidCustomCategory10, customCategory10ArticleSkuShipmentDetail, 1);

            //grossWeightOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "grossWeightOriginArticleSkuShipmentDetail", "Вес брутто (ориг.)", grossWeightOriginArticleSku, skuShipmentDetail, 1);
            //grossWeightArticleSkuShipmentDetail = addJProp(intraAttributeGroup, true, "grossWeightArticleSkuShipmentDetail", "Вес брутто", grossWeightArticleSku, skuShipmentDetail, 1);
            netWeightArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "netWeightArticleSkuShipmentDetail", "Вес нетто (ориг.)", netWeightArticleSku, skuShipmentDetail, 1);
            netWeightSkuShipmentDetail = addJProp(intraAttributeGroup, true, "netWeightSkuShipmentDetail", "Вес нетто", netWeightSku, skuShipmentDetail, 1);

            countryOfOriginArticleSkuShipmentDetail = addJProp(idGroup, true, "countryOfOriginArticleSkuShipmentDetail", "Страна происхождения (ориг.) (ИД)", countryOfOriginArticleSku, skuShipmentDetail, 1);
            nameCountryOfOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSkuShipmentDetail", "Страна происхождения", name, countryOfOriginArticleSkuShipmentDetail, 1);

            countryOfOriginSkuShipmentDetail = addJProp(idGroup, true, "countryOfOriginSkuShipmentDetail", "Страна происхождения (ИД)", countryOfOriginSku, skuShipmentDetail, 1);
            nameCountryOfOriginSkuShipmentDetail = addJProp(intraAttributeGroup, "nameCountryOfOriginSkuShipmentDetail", "Страна происхождения", name, countryOfOriginSkuShipmentDetail, 1);

            mainCompositionArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "mainCompositionArticleSkuShipmentDetail", "Состав (ориг.)", mainCompositionArticleSku, skuShipmentDetail, 1);
            mainCompositionSkuShipmentDetail = addJProp(intraAttributeGroup, true, "mainCompositionSkuShipmentDetail", "Состав", mainCompositionSku, skuShipmentDetail, 1);

            additionalCompositionSkuShipmentDetail = addJProp(intraAttributeGroup, true, "additionalCompositionSkuShipmentDetail", "Дополнительный состав", additionalCompositionSku, skuShipmentDetail, 1);

            unitOfMeasureArticleSkuShipmentDetail = addJProp(idGroup, true, "unitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара (ИД)", unitOfMeasureArticleSku, skuShipmentDetail, 1);
            nameUnitOfMeasureArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара", name, unitOfMeasureArticleSkuShipmentDetail, 1);

            // stock shipment detail
            stockShipmentDetail = addDProp(idGroup, "stockShipmentDetail", "Место хранения (ИД)", stock, shipmentDetail);
            barcodeStockShipmentDetail = addJProp(baseGroup, "barcodeStockShipmentDetail", "Штрих-код МХ", barcode, stockShipmentDetail, 1);

            routeFreightBoxShipmentDetail = addJProp(idGroup, "routeFreightBoxShipmentDetail", "Маршрут (ИД)", routeCreationFreightBoxFreightBox, stockShipmentDetail, 1);
            nameRouteFreightBoxShipmentDetail = addJProp(baseGroup, "nameRouteFreightBoxShipmentDetail", "Маршрут", name, routeFreightBoxShipmentDetail, 1);

            boxShipmentBoxShipmentDetail = addDProp(idGroup, "boxShipmentBoxShipmentDetail", "Поставка (ИД)", boxShipment, boxShipmentDetail);
            simpleShipmentSimpleShipmentDetail = addDProp(idGroup, "simpleShipmentSimpleShipmentDetail", "Поставка (ИД)", simpleShipment, simpleShipmentDetail);
            shipmentShipmentDetail = addCUProp(idGroup, "shipmentShipmentDetail", "Поставка (ИД)", boxShipmentBoxShipmentDetail, simpleShipmentSimpleShipmentDetail);

            // supplier box shipmentDetail
            supplierBoxShipmentDetail = addDProp(idGroup, "supplierBoxShipmentDetail", "Короб поставщика (ИД)", supplierBox, boxShipmentDetail);
            sidSupplierBoxShipmentDetail = addJProp(baseGroup, "sidSupplierBoxShipmentDetail", "Код короба поставщика", sidSupplierBox, supplierBoxShipmentDetail, 1);
            barcodeSupplierBoxShipmentDetail = addJProp(baseGroup, "barcodeSupplierBoxShipmentDetail", "Штрих-код короба поставщика", barcode, supplierBoxShipmentDetail, 1);

            quantityShipmentDetail = addDProp(baseGroup, "quantityShipmentDetail", "Кол-во", DoubleClass.instance, shipmentDetail);
            netWeightShipmentDetail = addDCProp(baseGroup, "netWeightShipmentDetail", "Вес нетто", netWeightArticleSku, true, skuShipmentDetail, 1);

            userShipmentDetail = addDCProp(idGroup, "userShipmentDetail", "Пользователь (ИД)", currentUser, true, is(shipmentDetail), 1);
            nameUserShipmentDetail = addJProp(baseGroup, "nameUserShipmentDetail", "Пользователь", name, userShipmentDetail, 1);

            timeShipmentDetail = addDCProp(baseGroup, "timeShipmentDetail", "Время ввода", currentDateTime, true, is(shipmentDetail), 1);

            addBoxShipmentDetailSupplierBoxStockBarcode = addJProp(true, "Добавить строку поставки",
                    addAAProp(boxShipmentDetail, supplierBoxShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail), 1, 2, barcodeToObject, 3, addCProp(DoubleClass.instance, 1));

            addSimpleShipmentDetailStockBarcode = addJProp(true, "Добавить строку поставки",
                    addAAProp(simpleShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail), 1, barcodeToObject, 2, addCProp(DoubleClass.instance, 1));

            quantitySupplierBoxBoxShipmentStockSku = addSGProp(baseGroup, "quantitySupplierBoxBoxShipmentStockSku", "Кол-во оприход.", quantityShipmentDetail,
                    supplierBoxShipmentDetail, 1, boxShipmentBoxShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

            quantitySimpleShipmentStockSku = addSGProp(baseGroup, "quantitySimpleShipmentStockSku", "Кол-во оприход.", quantityShipmentDetail,
                    simpleShipmentSimpleShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);
        } else {

            quantitySupplierBoxBoxShipmentStockSku = addDProp(baseGroup, "quantitySupplierBoxBoxShipmentStockSku", "Кол-во оприход.", DoubleClass.instance,
                    supplierBox, boxShipment, stock, sku);

            quantitySimpleShipmentStockSku = addDProp(baseGroup, "quantitySimpleShipmentStockSku", "Кол-во оприход.", DoubleClass.instance,
                    simpleShipment, stock, sku);
        }

        quantityShipDimensionShipmentStockSku = addCUProp(baseGroup, "quantityShipDimensionShipmentStockSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                addJProp(and1, quantitySimpleShipmentStockSku, 2, 3, 4, equals2, 1, 2));

        quantityBoxInvoiceBoxShipmentStockSku = addSGProp(baseGroup, "quantityBoxInvoiceBoxShipmentStockSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                boxInvoiceSupplierBox, 1, 2, 3, 4);

        invoicedSimpleInvoiceSimpleShipmentStockSku = addJProp(and(false, false, false, false), quantityDocumentSku, 1, 4, inInvoiceShipment, 1, 2, is(simpleInvoice), 1, is(simpleShipment), 2, is(stock), 3);

        quanitySimpleInvoiceSimpleShipmentStockSku = addPGProp(baseGroup, "quanitySimpleInvoiceSimpleShipmentStockSku", true, 0, "Кол-во оприход.",
                invoicedSimpleInvoiceSimpleShipmentStockSku,
                quantitySimpleShipmentStockSku, 2, 3, 4);

        quantityInvoiceShipmentStockSku = addCUProp(baseGroup, "quantityInvoiceShipmentStockSku", "Кол-во оприход.",
                quantityBoxInvoiceBoxShipmentStockSku, quanitySimpleInvoiceSimpleShipmentStockSku);

        quantityInvoiceStockSku = addSGProp(baseGroup, "quantityInvoiceStockSku", true, "Кол-во оприход.", quantityInvoiceShipmentStockSku, 1, 3, 4);

        priceInInvoiceStockSku = addJProp(baseGroup, "priceInInvoiceStockSku", true, "Цена входная", and1,
                priceDocumentSku, 1, 3, quantityInvoiceStockSku, 1, 2, 3);

        quantityShipDimensionStock = addSGProp(baseGroup, "quantityShipDimensionStock", "Всего оприход.", quantityShipDimensionShipmentStockSku, 1, 3);

        quantityShipDimensionShipmentSku = addSGProp(baseGroup, "quantityShipDimensionShipmentSku", "Оприход. (короб)", quantityShipDimensionShipmentStockSku, 1, 2, 4);

        quantityShipmentStockSku = addSGProp(baseGroup, "quantityShipmentStockSku", "Кол-во оприход.", quantityShipDimensionShipmentStockSku, 2, 3, 4);

        quantityShipmentStock = addSGProp(baseGroup, "quantityShipmentStock", "Всего оприход.", quantityShipmentStockSku, 1, 2);

        quantityShipmentSku = addSGProp(baseGroup, "quantityShipmentSku", "Оприход. (пост.)", quantityShipmentStockSku, 1, 3);

        quantityStockSku = addSGProp(baseGroup, "quantityStockSku", "Оприход. (МХ)", quantityShipmentStockSku, 2, 3);

        quantityShipmentPallet = addSGProp(baseGroup, "quantityShipmentPallet", "Всего оприход. (паллета)", quantityShipmentStock, 1, palletFreightBox, 2);

        quantityShipmentFreight = addSGProp(baseGroup, "quantityShipmentFreight", "Всего оприход. (фрахт)", quantityShipmentPallet, 1, freightPallet, 2);

        quantityShipmentArticle = addSGProp(baseGroup, "quantityShipmentArticle", "Всего оприход. (артикул)", quantityShipmentSku, 1, articleSku, 2);

        oneShipmentArticle = addJProp(baseGroup, "oneShipmentArticle", "Первый артикул", equals2, quantityShipmentArticle, 1, 2, addCProp(DoubleClass.instance, 1));
        oneShipmentArticleSku = addJProp(baseGroup, "oneShipmentArticleSku", "Первый артикул", oneShipmentArticle, 1, articleSku, 2);
        oneShipmentSku = addJProp(baseGroup, "oneShipmentSku", "Первый SKU", equals2, quantityShipmentSku, 1, 2, addCProp(DoubleClass.instance, 1));

        // Transfer
        stockFromTransfer = addDProp(idGroup, "stockFromTransfer", "Место хранения (с) (ИД)", stock, transfer);
        barcodeStockFromTransfer = addJProp(baseGroup, "barcodeStockFromTransfer", "Штрих-код МХ (с)", barcode, stockFromTransfer, 1);

        stockToTransfer = addDProp(idGroup, "stockToTransfer", "Место хранения (на) (ИД)", stock, transfer);
        barcodeStockToTransfer = addJProp(baseGroup, "barcodeStockToTransfer", "Штрих-код МХ (на)", barcode, stockToTransfer, 1);

        quantityTransferSku = addDProp(baseGroup, "quantityTransferStockSku", "Кол-во перемещения", DoubleClass.instance, transfer, sku);

        outcomeTransferStockSku = addSGProp(baseGroup, "outcomeTransferStockSku", "Расход по ВП", quantityTransferSku, stockFromTransfer, 1, 2);
        incomeTransferStockSku = addSGProp(baseGroup, "incomeTransferStockSku", "Приход по ВП", quantityTransferSku, stockToTransfer, 1, 2);

        incomeStockSku = addSUProp(baseGroup, "incomeStockSku", "Приход", Union.SUM, quantityStockSku, incomeTransferStockSku);
        outcomeStockSku = outcomeTransferStockSku;

        balanceStockSku = addDUProp(baseGroup, "balanceStockSku", "Тек. остаток", incomeStockSku, outcomeStockSku);

        balanceStockFromTransferSku = addJProp(baseGroup, "balanceStockFromTransferSku", "Тек. остаток на МХ (с)", balanceStockSku, stockFromTransfer, 1, 2);
        balanceStockToTransferSku = addJProp(baseGroup, "balanceStockToTransferSku", "Тек. остаток на МХ (на)", balanceStockSku, stockToTransfer, 1, 2);

        // Расписывание по route'ам количеств в инвойсе
        quantityShipmentRouteSku = addSGProp(baseGroup, "quantityShipmentRouteSku", "Кол-во оприход.", quantityShipmentStockSku, 1, routeCreationFreightBoxFreightBox, 2, 3);
        invoicedShipmentRouteSku = addPGProp(baseGroup, "invoicedShipmentRouteSku", false, 0, "Кол-во ожид.",
                percentShipmentRouteSku,
                invoicedShipmentSku, 1, 3);

        notFilledShipmentRouteSku = addJProp(baseGroup, "notFilledShipmentRouteSku", "Не заполнен", greater2, invoicedShipmentRouteSku, 1, 2, 3,
                addSUProp(Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipment, route, sku), quantityShipmentRouteSku), 1, 2, 3);

        routeToFillShipmentSku = addMGProp(idGroup, "routeToFillShipmentSku", "Маршрут (ИД)",
                addJProp(and1, object(route), 2, notFilledShipmentRouteSku, 1, 2, 3), 1, 3);

        LP routeToFillShipmentBarcode = addJProp(routeToFillShipmentSku, 1, barcodeToObject, 2);
        seekRouteToFillShipmentBarcode = addJProp(actionGroup, true, "seekRouteToFillShipmentSku", "Поиск маршрута", addSAProp(null),
                routeToFillShipmentBarcode, 1, 2);

        addConstraint(addJProp("Магазин короба для транспортировки должен совпадать с магазином короба поставщика", and1,
                      addJProp(diff2, destinationSupplierBox, 1, destinationFreightBox, 2), 1, 2,
                      quantityShipDimensionStock, 1, 2), true);

        // Freight
        tonnageFreightType = addDProp(baseGroup, "tonnageFreightType", "Тоннаж", DoubleClass.instance, freightType);
        palletCountFreightType = addDProp(baseGroup, "palletCountFreightType", "Кол-во паллет", IntegerClass.instance, freightType);
        volumeFreightType = addDProp(baseGroup, "volumeFreightType", "Объем", DoubleClass.instance, freightType);

        freightTypeFreight = addDProp(idGroup, "freightTypeFreight", "Тип машины (ИД)", freightType, freight);
        nameFreightTypeFreight = addJProp(baseGroup, "nameFreightTypeFreight", "Тип машины", name, freightTypeFreight, 1);

        tonnageFreight = addJProp(baseGroup, "tonnageFreight", "Тоннаж", tonnageFreightType, freightTypeFreight, 1);
        palletCountFreight = addJProp(baseGroup, "palletCountFreight", "Кол-во паллет", palletCountFreightType, freightTypeFreight, 1);
        volumeFreight = addJProp(baseGroup, "volumeFreight", "Объём", volumeFreightType, freightTypeFreight, 1);

        currencyFreight = addDProp(idGroup, "currencyFreight", "Валюта (ИД)", currency, freight);
        nameCurrencyFreight = addJProp(baseGroup, "nameCurrencyFreight", "Валюта", name, currencyFreight, 1);
        sumFreight = addDProp(baseGroup, "sumFreight", "Стоимость", DoubleClass.instance, freight);

        routeFreight = addDProp(idGroup, "routeFreight", "Маршрут (ИД)", route, freight);
        nameRouteFreight = addJProp(baseGroup, "nameRouteFreight", "Маршрут", name, routeFreight, 1);

        quantityPalletShipmentBetweenDate = addSGProp(baseGroup, "quantityPalletShipmentBetweenDate", "Кол-во паллет по поставкам за интервал",
                addJProp(and1, quantityPalletShipment, 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);
        quantityPalletFreightBetweenDate = addSGProp(baseGroup, "quantityPalletFreightBetweenDate", "Кол-во паллет по фрахтам за интервал",
                addJProp(and1, palletCountFreight, 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);

        freightBoxNumberPallet = addSGProp(baseGroup, "freightBoxNumberPallet", "Кол-во коробов", addCProp(IntegerClass.instance, 1, freightBox), palletFreightBox, 1);

        //routePallet = addSDProp(idGroup, "routePallet", "Маршрут (ИД)", route, pallet);
        //nameRoutePallet = addJProp(baseGroup, "nameRoutePallet", "Маршрут", name, routePallet, 1);

        addConstraint(addJProp("Маршрут паллеты должен совпадать с маршрутом фрахта", diff2,
                routeCreationPalletPallet, 1, addJProp(routeFreight, freightPallet, 1), 1), true);

        palletNumberFreight = addSGProp(baseGroup, "palletNumberFreight", "Кол-во паллет", addCProp(IntegerClass.instance, 1, pallet), freightPallet, 1);

        // Кол-во импортеров

        quantityImporterStockSku = addSGProp(baseGroup, "quantityImporterStockSku", "Кол-во оприход.", quantityInvoiceStockSku,
                importerInvoice, 1, 2, 3);

        quantityImporterFreightSku = addSGProp(baseGroup, "quantityImporterFreightSku", true, "Кол-во", quantityImporterStockSku, 1, freightFreightBox, 2, 3);
        netWeightImporterFreightSku = addJProp(baseGroup, "netWeightImporterFreightSku", "Вес нетто", multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, netWeightSku, 3);

        priceInImporterFreightSku = addMGProp(baseGroup, "priceInImporterFreightSku", "Цена входная", priceInInvoiceStockSku, importerInvoice, 1, freightFreightBox, 2, 3);

        priceFreightImporterFreightSku = addPGProp(baseGroup, "priceFreightImporterFreightSku", true, 2, "Цена за фрахт",
                netWeightImporterFreightSku,
                sumFreight, 2);

        // Текущие палеты/коробки для приема
        currentPalletRoute = addDProp("currentPalletRoute", "Тек. паллета (ИД)", pallet, route);
        barcodeCurrentPalletRoute = addJProp("barcodeCurrentPalletRoute", "Тек. паллета (штрих-код)", barcode, currentPalletRoute, 1);

        currentFreightBoxRoute = addDProp("currentFreightBoxRoute", "Тек. короб (ИД)", freightBox, route);
        barcodeCurrentFreightBoxRoute = addJProp("barcodeCurrentFreightBoxRoute", "Тек. короб (штрих-код)", barcode, currentFreightBoxRoute, 1);

        destinationCurrentFreightBoxRoute = addJProp(true, "destinationCurrentFreightBoxRoute", "Пункт назначения тек. короба (ИД)", destinationFreightBox, currentFreightBoxRoute, 1);
        nameDestinationCurrentFreightBoxRoute = addJProp("nameDestinationCurrentFreightBoxRoute", "Пункт назначения тек. короба", name, destinationCurrentFreightBoxRoute, 1);

        isCurrentFreightBox = addJProp(equals2, addJProp(true, currentFreightBoxRoute, routeCreationFreightBoxFreightBox, 1), 1, 1);
        isCurrentPallet = addJProp(equals2, addJProp(true, currentPalletRoute, routeCreationPalletPallet, 1), 1, 1);
        isCurrentPalletFreightBox = addJProp(equals2, palletFreightBox, 1, addJProp(currentPalletRoute, routeCreationFreightBoxFreightBox, 1), 1);
        isStoreFreightBoxSupplierBox = addJProp(equals2, destinationFreightBox, 1, destinationSupplierBox, 2);

        barcodeAction1 = addJProp(true, "Ввод штрих-кода 1", addCUProp(isCurrentFreightBox, isCurrentPallet), barcodeToObject, 1);
        barcodeActionSetPallet = addJProp(true, "Установить паллету", isCurrentPalletFreightBox, barcodeToObject, 1);
        barcodeActionSetStore = addJProp(true, "Установить магазин", isStoreFreightBoxSupplierBox, barcodeToObject, 1, 2);

        if (USE_SHIPMENT_DETAIL) {
            addBoxShipmentDetailSupplierBoxRouteBarcode = addJProp(true, "Добавить строку поставки",
                    addBoxShipmentDetailSupplierBoxStockBarcode, 1, currentFreightBoxRoute, 2, 3);

            addSimpleShipmentDetailRouteBarcode = addJProp(true, "Добавить строку поставки",
                    addSimpleShipmentDetailStockBarcode, currentFreightBoxRoute, 1, 2);
        }

        quantityRouteSku = addJProp(baseGroup, "quantityRouteSku", "Оприход. (МХ)", quantityStockSku, currentFreightBoxRoute, 1, 2);

        quantitySupplierBoxBoxShipmentRouteSku = addJProp(baseGroup, true,  "quantitySupplierBoxBoxShipmentRouteSku", "Кол-во оприход.",
                                                    quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4);
        quantitySimpleShipmentRouteSku = addJProp(baseGroup, true,  "quantitySimpleShipmentRouteSku", "Кол-во оприход.",
                                                    quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3);

        createFreightBox = addJProp(true, "Сгенерировать короба", addAAProp(freightBox, barcode, barcodePrefix, true), quantityCreationFreightBox, 1);
        createPallet = addJProp(true, "Сгенерировать паллеты", addAAProp(pallet, barcode, barcodePrefix, true), quantityCreationPallet, 1);

        barcodeAction4 = addJProp(true, "Ввод штрих-кода 4",
                addCUProp(
                        addSCProp(addJProp(true, quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4))
                ), 1, 2, 3, barcodeToObject, 4);
        barcodeAction3 = addJProp(true, "Ввод штрих-кода 3",
                addCUProp(
                        addSCProp(addJProp(true, quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3))
                ), 1, 2, barcodeToObject, 3);
    }

    LP quantitySupplierBoxBoxShipmentRouteSku;
    LP quantitySimpleShipmentRouteSku;
    LP routePallet, freightPallet, nameRoutePallet, palletFreightBox;
    LP currentPalletRoute;
    LP currentFreightBoxRoute;
    LP isCurrentFreightBox, isCurrentPalletFreightBox;
    LP isCurrentPallet;
    LP barcodeAction1, barcodeActionSetPallet, barcodeAction3;

    @Override
    protected void initTables() {
    }

    @Override
    protected void initIndexes() {
    }

    @Override
    protected void initNavigators() throws JRException, FileNotFoundException {
        NavigatorElement classifier = new NavigatorElement(baseElement, "classifier", "Справочники");
        addFormEntity(new CustomCategoryFormEntity(classifier, "customCategoryForm", "ТН ВЭД (изменения)", false));
        addFormEntity(new CustomCategoryFormEntity(classifier, "customCategoryForm2", "ТН ВЭД (дерево)", true));
        addFormEntity(new NewCustomCategoryFormEntity(classifier, "customCategoryForm3", "ТН ВЭД"));
        classifier.add(category.getClassForm(this));
        classifier.add(currency.getClassForm(this));
        classifier.add(store.getClassForm(this));
        classifier.add(country.getClassForm(this));
        classifier.add(unitOfMeasure.getClassForm(this));
        classifier.add(freightType.getClassForm(this));
        addFormEntity(new ColorSizeSupplierFormEntity(classifier, "сolorSizeSupplierForm", "Цвета, размеры и бренды поставщика"));

        NavigatorElement purchase = new NavigatorElement(baseElement, "purchase", "Управление закупками");
        addFormEntity(new OrderFormEntity(purchase, "orderForm", "Заказы"));
        addFormEntity(new InvoiceFormEntity(purchase, "boxInvoiceForm", "Инвойсы по коробам", true));
        addFormEntity(new InvoiceFormEntity(purchase, "simpleInvoiceForm", "Инвойсы без коробов", false));

        NavigatorElement shipment = new NavigatorElement(baseElement, "shipment", "Управление поставками");
        addFormEntity(new ShipmentListFormEntity(shipment, "boxShipmentListForm", "Поставки по коробам", true));
        addFormEntity(new ShipmentListFormEntity(shipment, "simpleShipmentListForm", "Поставки без коробов", false));
        addFormEntity(new FreightShipmentFormEntity(shipment, "freightShipmentForm", "Фрахт по поставкам"));
        addFormEntity(new FreightInvoiceFormEntity(shipment, "freightInvoiceForm", "Фрахт по инвойсам"));

        NavigatorElement distribution = new NavigatorElement(baseElement, "distribution", "Управление складом");
        addFormEntity(new CreatePalletFormEntity(distribution, "createPalletForm", "Генерация паллет"));
        addFormEntity(new CreateFreightBoxFormEntity(distribution, "createFreightBoxForm", "Генерация коробов"));
        addFormEntity(new ShipmentSpecFormEntity(distribution, "boxShipmentSpecForm", "Прием товара по коробам", true));
        addFormEntity(new ShipmentSpecFormEntity(distribution, "simpleShipmentSpecForm", "Прием товара без коробов", false));
        // пока не поддерживается из-за того, что пока нет расчета себестоимости для внутреннего перемещения
//        addFormEntity(new StockTransferFormEntity(distribution, "stockTransferForm", "Внутреннее перемещение"));
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

        private BarcodeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

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
//            design.getPanelContainer(design.get(objBarcode.groupTo)).constraints.maxVariables = 0;

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

        private OrderFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, nameCurrencySupplier);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objOrder = addSingleGroupObject(order, "Заказ", date, sidDocument, nameCurrencyDocument, sumDocument, nameDestinationDestinationDocument);
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
            addPropertyDraw(objArticle, sidArticle, nameBrandSupplierArticle, originalNameArticle, nameCountryOfOriginArticle, barcode);
            addPropertyDraw(quantityListArticle, objOrder, objArticle);
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

            addPropertyDraw(quantityListSku, objOrder, objItem);
            addPropertyDraw(priceDocumentSku, objOrder, objItem);
            addPropertyDraw(invoicedOrderSku, objOrder, objItem);
            addPropertyDraw(quantityDocumentArticleCompositeColor, objOrder, objArticle, objColorSupplier);
            addPropertyDraw(quantityDocumentArticleCompositeSize, objOrder, objArticle, objSizeSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, objSupplier));
            //addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierBrandSupplierArticle, objArticle), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleCompositeItem, objItem), Compare.EQUALS, objArticle));
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

        private InvoiceFormEntity(NavigatorElement parent, String sID, String caption, boolean box) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, nameCurrencySupplier);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс", date, sidDocument, nameCurrencyDocument, sumDocument, nameImporterInvoice, nameDestinationDestinationDocument);
            addObjectActions(this, objInvoice);

            objOrder = addSingleGroupObject(order, "Заказ");
            objOrder.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inOrderInvoice, objOrder, objInvoice);
            addPropertyDraw(objOrder, date, sidDocument, nameCurrencyDocument, sumDocument, nameDestinationDestinationDocument);

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
            addPropertyDraw(objArticle, sidArticle, nameBrandSupplierArticle, originalNameArticle, sidCustomCategoryOriginArticle, nameCountryOfOriginArticle, sidCustomCategory10OriginArticle, netWeightArticle, mainCompositionArticle, additionalCompositionArticle, barcode);
            addPropertyDraw(quantityListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(priceDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(sumDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(orderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(priceOrderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(delete, objArticle);

            objItem = addSingleGroupObject(item, "Товар", barcode, sidColorSupplierItem, nameColorSupplierItem, nameSizeSupplierItem);
            addObjectActions(this, objItem);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", selection, name);
            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", selection, sidColorSupplier, name);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityDocumentArticleCompositeColorSize, objInvoice, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.columnGroupObjects.add(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(name, objSizeSupplier);

            addPropertyDraw(quantityListSku, (box ? objSupplierBox : objInvoice), objItem);
            addPropertyDraw(priceDocumentSku, objInvoice, objItem);
            addPropertyDraw(orderedInvoiceSku, objInvoice, objItem);
            addPropertyDraw(quantityDocumentArticleCompositeColor, objInvoice, objArticle, objColorSupplier);
            addPropertyDraw(quantityDocumentArticleCompositeSize, objInvoice, objArticle, objSizeSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));
            if (box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(boxInvoiceSupplierBox, objSupplierBox), Compare.EQUALS, objInvoice));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, objSupplier));
            //addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierBrandSupplierArticle, objArticle), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleCompositeItem, objItem), Compare.EQUALS, objArticle));
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

        private ShipmentListFormEntity(NavigatorElement parent, String sID, String caption, boolean box) {
            super(parent, sID, caption);

            this.box = box;

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", date, sidDocument, quantityPalletShipment, netWeightShipment, grossWeightShipment, quantityBoxShipment);
            addObjectActions(this, objShipment);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс");
            objInvoice.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inInvoiceShipment, objInvoice, objShipment);
            addPropertyDraw(objInvoice, date, sidDocument, nameDestinationDestinationDocument);

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
        private ObjectEntity objSupplierBox;
        private ObjectEntity objSku;
        private ObjectEntity objRoute;
        private ObjectEntity objShipmentDetail;

        private PropertyDrawEntity nameRoute;

        private ShipmentSpecFormEntity(NavigatorElement parent, String sID, String caption, boolean box) {
            super(parent, sID, caption);

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
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб поставщика", sidSupplierBox, barcode, nameDestinationSupplierBox);
                objSupplierBox.groupTo.initClassView = ClassViewType.PANEL;
            }

            objRoute = addSingleGroupObject(route, "Маршрут", name, barcodeCurrentPalletRoute, barcodeCurrentFreightBoxRoute, nameDestinationCurrentFreightBoxRoute);
            objRoute.groupTo.setSingleClassView(ClassViewType.GRID);

            nameRoute = addPropertyDraw(name, objRoute);
            nameRoute.forceViewType = ClassViewType.PANEL;
                        
            objSku = addSingleGroupObject(sku, "SKU", barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, nameSizeSupplierItem,
                    nameBrandSupplierArticleSku, originalNameArticleSku, 
                    netWeightArticleSku, nameCountryOfOriginArticleSku, mainCompositionArticleSku,
                    nameCategoryArticleSku, netWeightSku,
                    nameCountryOfOriginSku, mainCompositionSku,
                    additionalCompositionSku, nameUnitOfMeasureArticleSku);
            objSku.groupTo.setSingleClassView(ClassViewType.GRID);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            setForceViewType(intraAttributeGroup, ClassViewType.PANEL, objSku.groupTo);

            setReadOnly(supplierAttributeGroup, true, objSku.groupTo);

            addPropertyDraw(invoicedShipmentSku, objShipment, objSku);
            addPropertyDraw(quantityShipmentSku, objShipment, objSku);

//            addPropertyDraw(oneShipmentArticleSku, objShipment, objSku);
//            addPropertyDraw(oneShipmentSku, objShipment, objSku);
//
//            getPropertyDraw(oneShipmentArticleSku).forceViewType = ClassViewType.PANEL;
//            getPropertyDraw(oneShipmentSku).forceViewType = ClassViewType.PANEL;

            PropertyDrawEntity quantityColumn;
            if (box) {
                addPropertyDraw(quantityListSku, objSupplierBox, objSku);
                addPropertyDraw(quantityShipDimensionShipmentSku, objSupplierBox, objShipment, objSku);
                quantityColumn = addPropertyDraw(quantitySupplierBoxBoxShipmentRouteSku, objSupplierBox, objShipment, objRoute, objSku);
            } else {
                quantityColumn = addPropertyDraw(quantitySimpleShipmentRouteSku, objShipment, objRoute, objSku);
            }

            quantityColumn.columnGroupObjects.add(objRoute.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(name, objRoute);

            addPropertyDraw(quantityRouteSku, objRoute, objSku);

            addPropertyDraw(percentShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);
            addPropertyDraw(invoicedShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);
            addPropertyDraw(quantityShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);

            if (USE_SHIPMENT_DETAIL) {
                objShipmentDetail = addSingleGroupObject((box ? boxShipmentDetail : simpleShipmentDetail),
                        selection, barcodeSkuShipmentDetail, sidArticleShipmentDetail, sidColorSupplierItemShipmentDetail, nameColorSupplierItemShipmentDetail, nameSizeSupplierItemShipmentDetail,
                        nameBrandSupplierArticleSkuShipmentDetail, originalNameArticleSkuShipmentDetail, 
                        netWeightArticleSkuShipmentDetail,
                        nameCountryOfOriginArticleSkuShipmentDetail, mainCompositionArticleSkuShipmentDetail,
                        nameCategoryArticleSkuShipmentDetail, netWeightSkuShipmentDetail,
                        nameCountryOfOriginSkuShipmentDetail, mainCompositionSkuShipmentDetail,
                        additionalCompositionSkuShipmentDetail, nameUnitOfMeasureArticleSkuShipmentDetail,
                        sidSupplierBoxShipmentDetail, barcodeSupplierBoxShipmentDetail,
                        barcodeStockShipmentDetail, nameRouteFreightBoxShipmentDetail,
                        quantityShipmentDetail, netWeightShipmentDetail, nameUserShipmentDetail, timeShipmentDetail);

                objShipmentDetail.groupTo.setSingleClassView(ClassViewType.GRID);

                addObjectActions(this, objShipmentDetail);

                setForceViewType(itemAttributeGroup, ClassViewType.GRID, objShipmentDetail.groupTo);
                setForceViewType(supplierAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);
                setForceViewType(intraAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);

                addFixedFilter(new CompareFilterEntity(addPropertyObject(shipmentShipmentDetail, objShipmentDetail), Compare.EQUALS, objShipment));
            }

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


            if (box) {
                RegularFilterGroupEntity filterGroup2 = new RegularFilterGroupEntity(genID());
                filterGroup2.addFilter(new RegularFilterEntity(genID(),
                                      new CompareFilterEntity(addPropertyObject(supplierBoxShipmentDetail, objShipmentDetail), Compare.EQUALS, objSupplierBox),
                                      "В текущем коробе поставщика",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
                addRegularFilterGroup(filterGroup2);
            }

            if (box) {
                RegularFilterGroupEntity filterGroup3 = new RegularFilterGroupEntity(genID());
                filterGroup3.addFilter(new RegularFilterEntity(genID(),
                                      new CompareFilterEntity(addPropertyObject(userShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(currentUser)),
                                      "Текущего пользователя",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
                addRegularFilterGroup(filterGroup3);
            }

            if (box) {
                RegularFilterGroupEntity filterGroup4 = new RegularFilterGroupEntity(genID());
                filterGroup4.addFilter(new RegularFilterEntity(genID(),
                                      new CompareFilterEntity(addPropertyObject(stockShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(currentFreightBoxRoute, objRoute)),
                                      "В текущем коробе для транспортировки",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
                addRegularFilterGroup(filterGroup4);
            }

            addAutoAction(objBarcode, addPropertyObject(barcodeAction1, objBarcode));
            addAutoAction(objBarcode, addPropertyObject(barcodeActionSetPallet, objBarcode));
            if (box)
                addAutoAction(objBarcode, addPropertyObject(barcodeActionSetStore, objBarcode, objSupplierBox));
            addAutoAction(objBarcode, addPropertyObject(seekRouteToFillShipmentBarcode, objShipment, objBarcode));
            if (box)
                addAutoAction(objBarcode, addPropertyObject(barcodeAction4, objSupplierBox, objShipment, objRoute, objBarcode));
            else
                addAutoAction(objBarcode, addPropertyObject(barcodeAction3, objShipment, objRoute, objBarcode));
            addAutoAction(objBarcode, addPropertyObject(seekBarcodeAction, objBarcode));

            if (USE_SHIPMENT_DETAIL) {
                if (box)
                    addAutoAction(objBarcode, addPropertyObject(addBoxShipmentDetailSupplierBoxRouteBarcode, objSupplierBox, objRoute, objBarcode));
                else
                    addAutoAction(objBarcode, addPropertyObject(addSimpleShipmentDetailRouteBarcode, objRoute, objBarcode));
            }

            addAutoAction(objBarcode, addPropertyObject(barcodeNotFoundMessage, objBarcode));
            if (box)
                addAutoAction(objSIDSupplierBox, addPropertyObject(seekSupplierBoxSIDSupplier, objSIDSupplierBox, objSupplier));

            setReadOnly(objSupplier, true);
            setReadOnly(objShipment, true);
            if (box)
                setReadOnly(objSupplierBox, true);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            if (box)
                design.setEditKey(design.get(getPropertyDraw(objectValue, objSIDSupplierBox)), KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));

            design.get(objRoute.groupTo).grid.constraints.fillVertical = 0.4;
            design.get(objRoute.groupTo).grid.showFilter = false;

            if (box)
                design.get(getPropertyDraw(quantityListSku, objSku)).caption = "Ожид. (короб)";

            if (box)
                design.addIntersection(design.getGroupObjectContainer(objBarcode.groupTo),
                                       design.getGroupObjectContainer(objSIDSupplierBox.groupTo),
                                       DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                                   design.getGroupObjectContainer(objShipment.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            if (box)
                design.addIntersection(design.getGroupObjectContainer(objShipment.groupTo),
                                       design.getGroupObjectContainer(objSupplierBox.groupTo),
                                       DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            if (USE_SHIPMENT_DETAIL) {
                ContainerView specContainer = design.createContainer();
                design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objShipmentDetail.groupTo));
                specContainer.add(design.getGroupObjectContainer(objShipmentDetail.groupTo));
                specContainer.add(design.getGroupObjectContainer(objSku.groupTo));
                specContainer.tabbedPane = true;

                design.get(nameRoute).minimumCharWidth = 4;
                design.get(nameRoute).panelLabelAbove = true;
                design.get(nameRoute).design.font = new Font("Tahoma", Font.BOLD, 64);
                design.getGroupObjectContainer(objRoute.groupTo).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            }

            return design;
        }
    }

    private class FreightShipmentFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objShipment;
        private ObjectEntity objFreight;
        private ObjectEntity objPallet;
        private ObjectEntity objDateFrom;
        private ObjectEntity objDateTo;

        private FreightShipmentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
            objDateFrom = new ObjectEntity(genID(), DateClass.instance, "Дата (с)");
            objDateTo = new ObjectEntity(genID(), DateClass.instance, "Дата (по)");
            gobjDates.add(objDateFrom);
            gobjDates.add(objDateTo);

            addGroup(gobjDates);
            gobjDates.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(objDateFrom, objectValue);
            addPropertyDraw(objDateTo, objectValue);
            addPropertyDraw(quantityPalletShipmentBetweenDate, objDateFrom, objDateTo);
            addPropertyDraw(quantityPalletFreightBetweenDate, objDateFrom, objDateTo);

            objShipment = addSingleGroupObject(shipment, "Поставка", date, nameSupplierDocument, sidDocument, sumDocument, nameCurrencyDocument, quantityPalletShipment, netWeightShipment, grossWeightShipment, quantityBoxShipment);
            objFreight = addSingleGroupObject(freight, "Фрахт", date, nameRouteFreight, nameFreightTypeFreight, tonnageFreight, palletCountFreight, volumeFreight, sumFreight, nameCurrencyFreight, palletNumberFreight);
            addObjectActions(this, objFreight);
            objPallet = addSingleGroupObject(pallet, "Паллета", barcode, freightBoxNumberPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(equalsPalletFreight, objPallet, objFreight);

            addPropertyDraw(quantityShipmentFreight, objShipment, objFreight).setToDraw(objShipment.groupTo);
            addPropertyDraw(quantityShipmentPallet, objShipment, objPallet).setToDraw(objShipment.groupTo);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objShipment), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(date, objShipment), Compare.LESS_EQUALS, objDateTo));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(routeCreationPalletPallet, objPallet), Compare.EQUALS, addPropertyObject(routeFreight, objFreight)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new OrFilterEntity(new CompareFilterEntity(addPropertyObject(freightPallet, objPallet), Compare.EQUALS, objFreight),
                                                     new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(freightPallet, objPallet)))),
                                  "Не расписанные паллеты или в текущем фрахте",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(freightPallet, objPallet), Compare.EQUALS, objFreight),
                                  "В текущем фрахте",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.defaultFilter = 0;
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objPallet.groupTo).grid.constraints.fillHorizontal = 0.4;

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo),
                    design.getGroupObjectContainer(objPallet.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class CreateFreightBoxFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objFreightBox;

        private CreateFreightBoxFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objCreate = addSingleGroupObject(creationFreightBox, "Операция создания", objectValue, nameRouteCreationFreightBox, quantityCreationFreightBox);
            addObjectActions(this, objCreate);
            objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            objFreightBox = addSingleGroupObject(freightBox, "Короба для транспортировки", barcode);
            setReadOnly(objFreightBox, true);

            addPropertyDraw(createFreightBox, objCreate);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationFreightBoxFreightBox, objFreightBox), Compare.EQUALS, objCreate));
        }
    }

    private class CreatePalletFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objPallet;

        private CreatePalletFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objCreate = addSingleGroupObject(creationPallet, "Операция создания", objectValue, nameRouteCreationPallet, quantityCreationPallet);
            addObjectActions(this, objCreate);
            objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            objPallet = addSingleGroupObject(pallet, "Паллеты для транспортировки", barcode);
            setReadOnly(objPallet, true);

            addPropertyDraw(createPallet, objCreate);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationPalletPallet, objPallet), Compare.EQUALS, objCreate));
        }
    }

    private class CustomCategoryFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean tree = false;
                
        private ObjectEntity objCustomCategory4;
        private ObjectEntity objCustomCategory6;
        private ObjectEntity objCustomCategory9;
        private ObjectEntity objCustomCategory10;

        private TreeGroupEntity treeCustomCategory;

        private ObjectEntity objCustomCategoryAdditional;

        private CustomCategoryFormEntity(NavigatorElement parent, String sID, String caption, boolean tree) {
            super(parent, sID, caption);

            this.tree = tree;

            objCustomCategory4 = addSingleGroupObject(customCategory4, "Первый уровень", sidCustomCategory4, nameCustomCategory);
            if (!tree)
                addObjectActions(this, objCustomCategory4);

            objCustomCategory6 = addSingleGroupObject(customCategory6, "Второй уровень", sidCustomCategory6, nameCustomCategory);
            if (!tree)
                addObjectActions(this, objCustomCategory6);

            objCustomCategory9 = addSingleGroupObject(customCategory9, "Третий уровень", sidCustomCategory9, nameCustomCategory);
            if (!tree)
                addObjectActions(this, objCustomCategory9);

            objCustomCategory10 = addSingleGroupObject(customCategory10, "Четвёртый уровень", sidCustomCategory10, nameCustomCategory);
            if (!tree)
                addObjectActions(this, objCustomCategory10);

            if (tree)
                 treeCustomCategory = addTreeGroupObject(objCustomCategory4.groupTo, objCustomCategory6.groupTo, objCustomCategory9.groupTo, objCustomCategory10.groupTo);

            objCustomCategoryAdditional = addSingleGroupObject(customCategoryOrigin, "Дополнительный уровень", sidCustomCategoryOrigin, nameCustomCategory);
            addObjectActions(this, objCustomCategoryAdditional);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory4CustomCategory6, objCustomCategory6), Compare.EQUALS, objCustomCategory4));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory6CustomCategory9, objCustomCategory9), Compare.EQUALS, objCustomCategory6));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory9CustomCategory10, objCustomCategory10), Compare.EQUALS, objCustomCategory9));
            //addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory10CustomCategoryAdditional, objCustomCategoryAdditional), Compare.EQUALS, objCustomCategory10));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            if (tree)
                design.addIntersection(design.getTreeContainer(treeCustomCategory), design.getGroupObjectContainer(objCustomCategoryAdditional.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            else {

                design.addIntersection(design.getGroupObjectContainer(objCustomCategory4.groupTo), design.getGroupObjectContainer(objCustomCategory6.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objCustomCategory6.groupTo), design.getGroupObjectContainer(objCustomCategory9.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objCustomCategory9.groupTo), design.getGroupObjectContainer(objCustomCategory10.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objCustomCategory10.groupTo), design.getGroupObjectContainer(objCustomCategoryAdditional.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }

            return design;
        }
    }

    private class NewCustomCategoryFormEntity extends FormEntity<RomanBusinessLogics> {


        private ObjectEntity objCustomCategory4;
        private ObjectEntity objCustomCategory6;
        private ObjectEntity objCustomCategory9;
        private ObjectEntity objCustomCategory10;
        private ObjectEntity objCustomCategoryOrigin;

        private NewCustomCategoryFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objCustomCategory4 = addSingleGroupObject(customCategory4, "Первый уровень", sidCustomCategory4, nameCustomCategory);
            addObjectActions(this, objCustomCategory4);

            objCustomCategory6 = addSingleGroupObject(customCategory6, "Второй уровень", sidCustomCategory6, nameCustomCategory);
            addObjectActions(this, objCustomCategory6);

            objCustomCategory9 = addSingleGroupObject(customCategory9, "Третий уровень", sidCustomCategory9, nameCustomCategory);
            addObjectActions(this, objCustomCategory9);

            objCustomCategory10 = addSingleGroupObject(customCategory10, "Четвёртый уровень", sidCustomCategory10, nameCustomCategory);
            addObjectActions(this, objCustomCategory10);

            objCustomCategoryOrigin = addSingleGroupObject(customCategoryOrigin, "ЕС уровень", sidCustomCategoryOrigin, nameCustomCategory, sidCustomCategory10CustomCategoryOrigin);
            addObjectActions(this, objCustomCategoryOrigin);
            //, nameCustomCategory10CustomCategoryOrigin

            addPropertyDraw(relationCustomCategory10CustomCategoryOrigin, objCustomCategory10, objCustomCategoryOrigin);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory4CustomCategory6, objCustomCategory6), Compare.EQUALS, objCustomCategory4));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory6CustomCategory9, objCustomCategory9), Compare.EQUALS, objCustomCategory6));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory9CustomCategory10, objCustomCategory10), Compare.EQUALS, objCustomCategory9));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory6CustomCategoryOrigin, objCustomCategoryOrigin), Compare.EQUALS, objCustomCategory6));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objCustomCategory4.groupTo).grid.constraints.fillVertical = 1;
            design.get(objCustomCategory4.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objCustomCategory6.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objCustomCategory9.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objCustomCategory10.groupTo).grid.constraints.fillVertical = 1;
            design.get(objCustomCategory10.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objCustomCategoryOrigin.groupTo).grid.constraints.fillHorizontal = 2;

            design.addIntersection(design.getGroupObjectContainer(objCustomCategory4.groupTo), design.getGroupObjectContainer(objCustomCategory6.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objCustomCategory6.groupTo), design.getGroupObjectContainer(objCustomCategory9.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            //design.addIntersection(design.getGroupObjectContainer(objCustomCategory9.groupTo), design.getGroupObjectContainer(objCustomCategory10.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objCustomCategory10.groupTo), design.getGroupObjectContainer(objCustomCategoryOrigin.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class ColorSizeSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objColor;
        private ObjectEntity objSize;
        private ObjectEntity objBrand;

        private ColorSizeSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, nameBrandSupplierSupplier, nameCurrencySupplier);
            addObjectActions(this, objSupplier);

            objColor = addSingleGroupObject(colorSupplier, "Цвет", sidColorSupplier, name);
            addObjectActions(this, objColor);

            objSize = addSingleGroupObject(sizeSupplier, "Размер", name);
            addObjectActions(this, objSize);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд", name);
            addObjectActions(this, objBrand);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColor), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSize), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierBrandSupplier, objBrand), Compare.EQUALS, objSupplier));
        }
        
        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 0.5;
            design.get(objColor.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objSize.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objBrand.groupTo).grid.constraints.fillHorizontal = 2;

            design.addIntersection(design.getGroupObjectContainer(objColor.groupTo),
                                   design.getGroupObjectContainer(objSize.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            
            design.addIntersection(design.getGroupObjectContainer(objSize.groupTo),
                                   design.getGroupObjectContainer(objBrand.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            return design;
        }
    }

    private class StockTransferFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objTransfer;
        private ObjectEntity objSku;

        private StockTransferFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objTransfer = addSingleGroupObject(transfer, "Внутреннее перемещение", objectValue, barcodeStockFromTransfer, barcodeStockToTransfer);
            addObjectActions(this, objTransfer);

            objSku = addSingleGroupObject(sku, "SKU", barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, nameSizeSupplierItem,
                    nameCategoryArticleSku, sidCustomCategory10ArticleSku,
                    nameCountryOfOriginSku, netWeightSku, mainCompositionSku,
                    additionalCompositionSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            addPropertyDraw(balanceStockFromTransferSku, objTransfer, objSku);
            addPropertyDraw(quantityTransferSku, objTransfer, objSku);
            addPropertyDraw(balanceStockToTransferSku, objTransfer, objSku);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(balanceStockFromTransferSku, objTransfer, objSku)),
                                  "Есть на остатке",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(quantityTransferSku, objTransfer, objSku)),
                                  "В документе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.defaultFilter = 0;
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objTransfer.groupTo).grid.constraints.fillVertical = 0.4;

            return design;
        }
    }

    private class FreightInvoiceFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objSku;

        private FreightInvoiceFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", date, nameRouteFreight, nameFreightTypeFreight, tonnageFreight, palletCountFreight, volumeFreight, sumFreight, nameCurrencyFreight, palletNumberFreight);

            objImporter = addSingleGroupObject(importer, "Импортер", name);

            objSku = addSingleGroupObject(sku, "SKU", barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, nameSizeSupplierItem,
                    nameBrandSupplierArticleSku, nameCategoryArticleSku, sidCustomCategory10ArticleSku,
                    nameCountryOfOriginSku, netWeightSku, mainCompositionSku,
                    additionalCompositionSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            addPropertyDraw(quantityImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceFreightImporterFreightSku, objImporter, objFreight, objSku);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSku)));
        }
    }
}
