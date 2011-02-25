package roman;

import jxl.Sheet;
import jxl.Workbook;
import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.Settings;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.integration.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.session.DataSession;
import tmc.integration.exp.DeclarationExportActionProperty;
import tmc.integration.imp.ClassifierTNVEDImportActionProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class RomanBusinessLogics extends BusinessLogics<RomanBusinessLogics> {

    private static StringClass COMPOSITION_CLASS = StringClass.get(200);

    private AbstractCustomClass article;
    private ConcreteCustomClass articleComposite;
    private ConcreteCustomClass articleSingle;
    private ConcreteCustomClass item;
    protected AbstractCustomClass sku;
    private ConcreteCustomClass pallet;
    public LP sidArticle;
    private LP articleCompositeItem;
    private LP articleSku;
    private ConcreteCustomClass order;
    private AbstractCustomClass invoice;
    protected AbstractCustomClass shipment;
    private ConcreteCustomClass boxShipment;
    private ConcreteCustomClass simpleShipment;
    private ConcreteCustomClass freight;
    private StaticCustomClass route;
    private ConcreteCustomClass supplier;
    private AbstractCustomClass document;
    private AbstractCustomClass priceDocument;
    private AbstractCustomClass subject;
    public LP addressSubject;
    private LP supplierDocument;
    private LP nameSupplierDocument;
    private LP sidDocument;
    private LP documentSIDSupplier;
    private LP sumDocument;
    private ConcreteCustomClass colorSupplier;
    private ConcreteCustomClass sizeSupplier;
    private ConcreteCustomClass brandSupplier;
    private ConcreteCustomClass countrySupplier;
    private LP supplierArticle;
    private LP nameSupplierArticle;
    private LP priceDocumentArticle;
    private LP sumDocumentArticle;
    private LP colorSupplierItem;
    private LP nameColorSupplierItem;
    private LP sizeSupplierItem;
    private LP sidSizeSupplierItem;
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
    private LP sidDestination;
    private LP destinationSID;
    private LP unitOfMeasureCategory;
    private LP nameUnitOfMeasureCategory;
    private LP unitOfMeasureArticle;
    private LP nameOriginUnitOfMeasureArticle;
    public LP nameUnitOfMeasureArticle;
    private LP unitOfMeasureArticleSku;
    private LP nameUnitOfMeasureArticleSku;
    private LP supplierCountrySupplier;
    private LP nameSupplierCountrySupplier;
    private LP countryCountrySupplier;
    private LP nameCountryCountrySupplier;
    private LP currencySupplier;
    private LP nameCurrencySupplier;
    private LP currencyDocument;
    private LP nameCurrencyDocument;
    private LP destinationDestinationDocument;
    private LP nameDestinationDestinationDocument;
    private LP sidDestinationDestinationDocument;
    private LP quantityPalletShipment;
    private LP grossWeightPallet;
    private LP grossWeightCurrentPalletRoute;
    private LP grossWeightFreight;
    private LP sumGrossWeightFreightSku;
    private LP grossWeightFreightSkuAggr;
    private LP netWeightShipment;
    private LP grossWeightShipment;
    private LP sidColorSupplier;
    private LP sidColorSupplierItem;
    private LP quantityDocumentSku;
    private LP quantityDocumentArticleCompositeColor;
    private LP quantityDocumentArticleCompositeSize;
    private LP quantityDocumentArticleCompositeColorSize;
    private LP originalNameArticle;
    private LP nameArticle;
    private LP netWeightArticleSku;
    private LP netWeightDataSku;
    private LP netWeightSku;
    private LP sumNetWeightFreightSku;
    private LP netWeightArticle;
    private LP netWeightSkuShipmentDetail;
    private LP mainCompositionOriginDataSku;
    private LP additionalCompositionOriginDataSku;
    private LP mainCompositionOriginSku;
    private LP additionalCompositionOriginSku;
    AbstractCustomClass secondNameClass;
    private LP nameOrigin;
    private ConcreteCustomClass category;
    public ConcreteCustomClass customCategory4;
    public ConcreteCustomClass customCategory6;
    public ConcreteCustomClass customCategory9;
    public ConcreteCustomClass customCategory10;
    public ConcreteCustomClass customCategoryOrigin;
    private LP categoryArticle;
    private LP unitOfMeasureDataArticle;
    private LP unitOfMeasureCategoryArticle;
    private LP nameOriginUnitOfMeasureArticleSku;
    private LP nameOriginCategoryArticle;
    private LP nameCategoryArticle;
    private LP categoryArticleSku;
    private LP nameCategoryArticleSku;
    private LP nameOriginCategoryArticleSku;
    public LP sidCustomCategory4;
    public LP sidCustomCategory6;
    public LP sidCustomCategory9;
    public LP sidCustomCategory10;
    public LP sidCustomCategoryOrigin;

    public LP sidToCustomCategory4;
    public LP sidToCustomCategory6;
    public LP sidToCustomCategory9;
    public LP sidToCustomCategory10;
    public LP sidToCustomCategoryOrigin;
    private LP importBelTnved;
    private LP importEuTnved;

    public LP customCategory4CustomCategory6;
    public LP customCategory6CustomCategory9;
    public LP customCategory9CustomCategory10;
    public LP customCategory6CustomCategory10;
    public LP customCategory4CustomCategory10;
    public LP customCategory6CustomCategoryOrigin;
    public LP customCategory4CustomCategoryOrigin;
    public LP customCategory10CustomCategoryOrigin;
    public LP sidCustomCategory10CustomCategoryOrigin;
    private LP sidCustomCategory4CustomCategory6;
    private LP sidCustomCategory6CustomCategory9;
    private LP sidCustomCategory9CustomCategory10;
    private LP sidCustomCategory6CustomCategoryOrigin;
    private LP nameCustomCategory4CustomCategory6;
    private LP nameCustomCategory6CustomCategory9;
    private LP nameCustomCategory9CustomCategory10;
    private LP nameCustomCategory6CustomCategory10;
    private LP nameCustomCategory4CustomCategory10;
    private LP nameCustomCategory6CustomCategoryOrigin;
    private LP nameCustomCategory4CustomCategoryOrigin;
    public LP relationCustomCategory10CustomCategoryOrigin;
    private LP customCategory10DataSku;
    private LP customCategory10Sku;
    private LP sidCustomCategory10Sku;
    private LP customCategory10CustomCategoryOriginArticle;
    private LP customCategory10CustomCategoryOriginArticleSku;
    private LP mainCompositionArticle;
    private LP additionalCompositionArticle;
    private LP mainCompositionOriginArticle;
    private LP additionalCompositionOriginArticle;
    private LP mainCompositionOriginArticleSku;
    private LP additionalCompositionOriginArticleSku;
    private LP mainCompositionSku;
    private LP additionalCompositionSku;
    private LP countrySupplierOfOriginArticle;
    private LP countryOfOriginArticle;
    private LP nameCountryOfOriginArticle;
    private LP countryOfOriginArticleSku;
    private LP countryOfOriginDataSku;
    private LP countryOfOriginSku;
    private LP nameCountryOfOriginSku;
    private LP nameCountrySupplierOfOriginArticle;
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
    private LP numberListSku;
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
    private LP priceImporterFreightSku;
    private LP priceMaxImporterFreightSku;
    private LP priceDocumentSku;
    private LP sumDocumentSku;
    private LP inInvoiceShipment;
    private LP tonnageFreight;
    private LP palletCountFreight;
    private LP volumeFreight;
    private LP currencyFreight;
    private LP nameCurrencyFreight;
    private LP sumFreightFreight;
    private LP routeFreight;
    private LP nameRouteFreight;
    private LP exporterFreight;
    public LP nameExporterFreight;
    public LP addressExporterFreight;
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
     private LP quantityStockArticle;
    private LP quantityPalletSku;
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
    private LP addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode;
    private LP addBoxShipmentDetailBoxShipmentSupplierBoxRouteBarcode;
    private LP articleShipmentDetail;
    private LP sidArticleShipmentDetail;
    private LP barcodeStockShipmentDetail;
    private LP barcodeSupplierBoxShipmentDetail;
    private LP sidSupplierBoxShipmentDetail;
    private LP routeFreightBoxShipmentDetail;
    private LP nameRouteFreightBoxShipmentDetail;
    private LP addSimpleShipmentSimpleShipmentDetailStockBarcode;
    private LP addSimpleShipmentDetailSimpleShipmentRouteBarcode;
    private AbstractGroup skuAttributeGroup;
    private AbstractGroup supplierAttributeGroup;
    private AbstractGroup intraAttributeGroup;
    private LP colorSupplierItemShipmentDetail;
    private LP sidColorSupplierItemShipmentDetail;
    private LP nameColorSupplierItemShipmentDetail;
    private LP sizeSupplierItemShipmentDetail;
    private LP sidSizeSupplierItemShipmentDetail;
    private AbstractGroup itemAttributeGroup;
    private LP originalNameArticleSkuShipmentDetail;
    private LP categoryArticleSkuShipmentDetail;
    private LP nameOriginCategoryArticleSkuShipmentDetail;
    private LP customCategoryOriginArticleSkuShipmentDetail;
    private LP sidCustomCategoryOriginArticleSkuShipmentDetail;
    private LP netWeightArticleSkuShipmentDetail;
    private LP countryOfOriginArticleSkuShipmentDetail;
    private LP nameCountryOfOriginArticleSkuShipmentDetail;
    private LP countryOfOriginSkuShipmentDetail;
    private LP nameCountryOfOriginSkuShipmentDetail;
    private LP mainCompositionOriginArticleSkuShipmentDetail;
    private LP mainCompositionOriginSkuShipmentDetail;
    private LP additionalCompositionOriginSkuShipmentDetail;
    private LP unitOfMeasureArticleSkuShipmentDetail;
    private LP nameOriginUnitOfMeasureArticleSkuShipmentDetail;
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
    public LP nameCustomCategory;
    private LP customCategoryOriginArticle;
    private LP customCategoryOriginArticleSku;
    private LP sidCustomCategoryOriginArticle;
    private LP sidCustomCategoryOriginArticleSku;
    private LP quantityBoxInvoiceBoxShipmentStockSku;
    private LP quantityInvoiceShipmentStockSku;
    private LP invoicedSimpleInvoiceSimpleShipmentStockSku;
    private LP quantitySimpleInvoiceSimpleShipmentStockSku;
    private LP quantityInvoiceStockSku;
    private ConcreteCustomClass importer;
    private ConcreteCustomClass exporter;
    private LP contractImporter;
    private LP importerInvoice;
    private LP nameImporterInvoice;
    private LP freightFreightBox;
    private LP quantityImporterFreightUnitSku;
    private LP quantityImporterFreightUnitArticle;
    private LP quantityImporterFreightSku;
    private LP netWeightStockSku;
    private LP netWeightStock;
    private LP netWeightImporterFreightUnitSku;
    private LP netWeightImporterFreightUnit;
    private LP grossWeightImporterFreightUnitSku;
    private LP grossWeightImporterFreightUnit;
    private LP priceInInvoiceStockSku;
    private LP priceInImporterFreightSku;
    private LP netWeightImporterFreightSku;
    private LP grossWeightImporterFreightSku;
    private LP netWeightImporterFreight;
    private LP grossWeightImporterFreight;
    private LP priceFreightImporterFreightSku;
    private LP oneArticleSkuShipmentDetail;
    private LP oneSkuShipmentDetail;
    private LP quantityImporterFreight;
    private LP quantityImporterFreightBrandSupplier;
    private LP markupPercentImporterFreightBrandSupplier;
    private LP markupInImporterFreightSku;
    private LP priceExpenseImporterFreightSku;
    private LP markupPercentImporterFreightSku;
    private LP markupImporterFreightSku;
    private LP priceMarkupInImporterFreightSku;
    private LP priceOutImporterFreightSku;
    private LP priceInOutImporterFreightSku;
    private LP sumInImporterFreightSku;
    private LP sumMarkupImporterFreightSku;
    private LP sumOutImporterFreightSku;
    private LP sumInImporterFreight;
    private LP sumMarkupImporterFreight;
    private LP sumMarkupInImporterFreight;
    private LP sumMarkupInImporterFreightSku;
    private LP sumInOutImporterFreightSku;
    private LP sumImporterFreightUnitSku;
    private LP sumMarkupInFreight;
    private LP sumImporterFreight;
    private LP sumOutImporterFreight;
    private LP sumInOutImporterFreight;
    private LP sumInOutFreight;
    private LP sumInFreight;
    private LP sumMarkupFreight;
    private LP sumOutFreight;
    private LP sumFreightImporterFreightSku;
    private LP quantityFreightArticle;
    private LP quantityFreightSku;
    public LP quantityImporterFreightArticleCompositionCountryCategory;
    public LP netWeightImporterFreightArticleCompositionCountryCategory;
    public LP grossWeightImporterFreightArticleCompositionCountryCategory;
    public LP priceImporterFreightArticleCompositionCountryCategory;
    public LP sumImporterFreightArticleCompositionCountryCategory;
    private ConcreteCustomClass freightComplete;
    private ConcreteCustomClass freightPriced;
    private ConcreteCustomClass freightChanged;
    private ConcreteCustomClass freightShipped;
    private LP dictionaryComposition;
    private LP nameDictionaryComposition;
    private LP translationMainCompositionSku;
    private LP translationAdditionalCompositionSku;
    private LP sidShipmentShipmentDetail;
    private LP colorSIDSupplier;
    private LP sidSizeSupplier;
    private LP sizeSIDSupplier;
    private LP countryNameSupplier;
    private LP numberDataListSku;
    private LP numberArticleListSku;
    private LP grossWeightFreightSku;
    private LP netWeightFreightSku;
    private LP customCategory10FreightSku;
    private LP sidCustomCategory10FreightSku;
    private LP mainCompositionOriginFreightSku;
    private LP mainCompositionFreightSku;
    private LP additionalCompositionOriginFreightSku;
    private LP additionalCompositionFreightSku;
    private LP countryOfOriginFreightSku;
    private LP sidCountryOfOriginFreightSku;
    private LP nameCountryOfOriginFreightSku;
    private LP equalsItemArticleComposite;
    private LP executeArticleCompositeItemSIDSupplier;
    private CreateItemFormEntity createItemForm;
    private LP addItemBarcode;
    private LP barcodeActionSeekFreightBox;
    private LP currentPalletFreightBox;
    private LP barcodeActionCheckPallet;
    private LP barcodeActionCheckFreightBox;
    private LP packingListFormFreightBox;
    private LP packingListFormRoute;
    LP quantitySupplierBoxBoxShipmentRouteSku;
    LP quantitySimpleShipmentRouteSku;
    LP routePallet, freightPallet, nameRoutePallet, palletFreightBox;
    LP currentPalletRoute;
    LP currentFreightBoxRoute;
    LP isCurrentFreightBox, isCurrentPalletFreightBox;
    LP isCurrentPallet;
    LP barcodeActionSeekPallet, barcodeActionSetPallet, barcodeAction3;
    private LP invoiceFormImporterFreight;
    private LP packingListFormImporterFreight;
    private LP countrySupplierOfOriginArticleSku;
    private LP nameCountrySupplierOfOriginArticleSku;
    private AbstractCustomClass directInvoice;
    private ConcreteCustomClass directBoxInvoice;
    private LP freightDirectInvoice;
    private LP equalsDirectInvoiceFreight;
    private LP grossWeightDirectInvoice;
    private LP nameOriginCountry;
    private LP nameCountrySku;
    private LP sumInCurrentYear;
    private LP sumInOutCurrentYear;
    private LP balanceSumCurrentYear;
    private AbstractCustomClass freightUnit;
    private LP quantityInvoiceFreightUnitSku;
    private LP freightSupplierBox;
    private LP freightFreightUnit;
    private LP priceInInvoiceFreightUnitSku;
    private ConcreteCustomClass jennyferSupplier;
    private ConcreteCustomClass tallyWeijlSupplier;
    private LP jennyferImportInvoice;
    private LP tallyWeijlImportInvoice;
    private AbstractGroup importInvoiceActionGroup;

    public RomanBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void initGroups() {

        Settings.instance.setDisableSumGroupNotZero(true);

        skuAttributeGroup = new AbstractGroup("Атрибуты SKU");
        baseGroup.add(skuAttributeGroup);

        itemAttributeGroup = new AbstractGroup("Атрибуты товара");
        baseGroup.add(itemAttributeGroup);

        supplierAttributeGroup = new AbstractGroup ("Атрибуты поставщика");
        publicGroup.add(supplierAttributeGroup);

        intraAttributeGroup = new AbstractGroup("Внутренние атрибуты");
        publicGroup.add(intraAttributeGroup);

        importInvoiceActionGroup = new AbstractGroup("Импорт инвойсов");
        importInvoiceActionGroup.createContainer = false;
        actionGroup.add(importInvoiceActionGroup);
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

        directInvoice = addAbstractClass("directInvoice", "Инвойс (напрямую)", invoice);
        directBoxInvoice = addConcreteClass("directBoxInvoice", "Инвойс по коробам (напрямую)", boxInvoice, directInvoice);

        simpleInvoice = addConcreteClass("simpleInvoice", "Инвойс без коробов", invoice, list);

        shipDimension = addConcreteClass("shipDimension", "Разрез поставки", baseClass);

        stock = addConcreteClass("stock", "Место хранения", barcodeObject);

        freightUnit = addAbstractClass("freightUnit", "Машиноместо", baseClass);

        supplierBox = addConcreteClass("supplierBox", "Короб поставщика", list, shipDimension, barcodeObject, freightUnit);

        shipment = addAbstractClass("shipment", "Поставка", document);
        boxShipment = addConcreteClass("boxShipment", "Поставка по коробам", shipment);
        simpleShipment = addConcreteClass("simpleShipment", "Поставка без коробов", shipment, shipDimension);

        shipmentDetail = addAbstractClass("shipmentDetail", "Строка поставки", baseClass);
        boxShipmentDetail = addConcreteClass("boxShipmentDetail", "Строка поставки по коробам", shipmentDetail);
        simpleShipmentDetail = addConcreteClass("simpleShipmentDetail", "Строка поставки без коробов", shipmentDetail);

        supplier = addConcreteClass("supplier", "Поставщик", baseClass.named);

        jennyferSupplier = addConcreteClass("jennyferSupplier", "Jennyfer", supplier);
        tallyWeijlSupplier = addConcreteClass("tallyWeijlSupplier", "Tally Weijl", supplier);

        subject = addAbstractClass("subject", "Субъект", baseClass.named);
        importer = addConcreteClass("importer", "Импортер", subject);
        exporter = addConcreteClass("exporter", "Экспортер", subject);

        colorSupplier = addConcreteClass("colorSupplier", "Цвет поставщика", baseClass.named);
        sizeSupplier = addConcreteClass("sizeSupplier", "Размер поставщика", baseClass);

        freightBox = addConcreteClass("freightBox", "Короб для транспортировки", stock, freightUnit);

        freight = addConcreteClass("freight", "Фрахт", baseClass.named, transaction);
        freightComplete = addConcreteClass("freightComplete", "Скомплектованный фрахт", freight);
        freightPriced = addConcreteClass("freightPriced", "Расцененный фрахт", freightComplete);
        freightChanged = addConcreteClass("freightChanged", "Обработанный фрахт", freightPriced);
        freightShipped = addConcreteClass("freightShipped", "Отгруженный фрахт", freightChanged);

        freightType = addConcreteClass("freightType", "Тип машины", baseClass.named);

        pallet = addConcreteClass("pallet", "Паллета", barcodeObject);

        secondNameClass = addAbstractClass("secondNameClass", "Класс со вторым именем", baseClass);

        category = addConcreteClass("category", "Категория", secondNameClass, baseClass.named);

        customCategory = addAbstractClass("customCategory", "Уровень ТН ВЭД", baseClass);

        customCategory4 = addConcreteClass("customCategory4", "Первый уровень", customCategory);
        customCategory6 = addConcreteClass("customCategory6", "Второй уровень", customCategory);
        customCategory9 = addConcreteClass("customCategory9", "Третий уровень", customCategory);
        customCategory10 = addConcreteClass("customCategory10", "Четвёртый уровень", customCategory);

        customCategoryOrigin = addConcreteClass("customCategoryOrigin", "ЕС уровень", customCategory);

        creationFreightBox = addConcreteClass("creationFreightBox", "Операция создания коробов", transaction);
        creationPallet = addConcreteClass("creationPallet", "Операция создания паллет", transaction);

        transfer = addConcreteClass("transfer", "Внутреннее перемещение", baseClass);

        unitOfMeasure = addConcreteClass("unitOfMeasure", "Единица измерения", secondNameClass, baseClass.named);

        brandSupplier = addConcreteClass("brandSupplier", "Бренд поставщика", baseClass.named);

        countrySupplier = addConcreteClass("countrySupplier", "Страна поставщика", baseClass.named);

        route = addStaticClass("route", "Маршрут", new String[]{"rb", "rf"}, new String[]{"РБ", "РФ"});
    }

    @Override
    protected void initProperties() {

        nameOrigin = addDProp(baseGroup, "nameOrigin", "Наименование (ориг.)", StringClass.get(50), secondNameClass);
        nameOriginCountry = addDProp(baseGroup, "nameOriginCountry", "Наименование (ориг.)", StringClass.get(50), country); 

        dictionaryComposition = addDProp(idGroup, "dictionaryComposition", "Словарь для составов (ИД)", dictionary);
        nameDictionaryComposition = addJProp(baseGroup, "nameDictionaryComposition", "Словарь для составов", name, dictionaryComposition);

        sidDestination = addDProp(baseGroup, "sidDestination", "Код", StringClass.get(50), destination);

        destinationSID = addCGProp(idGroup, "destinationSID", "Магазин (ИД)", object(destination), sidDestination, sidDestination, 1);

        // Subject
        addressSubject = addDProp(baseGroup, "addressSubject", "Адрес", StringClass.get(200), subject);
        contractImporter = addDProp(baseGroup, "contractImporter", "Номер договора", StringClass.get(50), importer);

        // CustomCategory
        nameCustomCategory = addDProp(baseGroup, "nameCustomCategory", "Наименование", StringClass.get(500), customCategory);
        nameCustomCategory.property.preferredCharWidth = 50;
        nameCustomCategory.property.minimumCharWidth = 20;

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

        sidToCustomCategory4 = addCGProp(null, "sidToCustomCategory4", "Код(4)", object(customCategory4), sidCustomCategory4, sidCustomCategory4, 1);
        sidToCustomCategory6 = addCGProp(null, "sidToCustomCategory6", "Код(6)", object(customCategory6), sidCustomCategory6, sidCustomCategory6, 1);
        sidToCustomCategory9 = addCGProp(null, "sidToCustomCategory9", "Код(9)", object(customCategory9), sidCustomCategory9, sidCustomCategory9, 1);
        sidToCustomCategory10 = addCGProp(null, "sidToCustomCategory10", "Код(10)", object(customCategory10), sidCustomCategory10, sidCustomCategory10, 1);
        sidToCustomCategoryOrigin = addCGProp(null, "sidToCustomCategoryOrigin", "Код ЕС (10)", object(customCategoryOrigin), sidCustomCategoryOrigin, sidCustomCategoryOrigin, 1);

        importBelTnved = addAProp(new ClassifierTNVEDImportActionProperty(genSID(), "Импортировать (РБ)", this, "belarusian"));
        importEuTnved = addAProp(new ClassifierTNVEDImportActionProperty(genSID(), "Импортировать (ЕС)", this, "origin"));
        jennyferImportInvoice = addAProp(importInvoiceActionGroup, new JennyferImportInvoiceActionProperty());
        tallyWeijlImportInvoice = addAProp(importInvoiceActionGroup, new TallyWeijlImportInvoiceActionProperty());

        customCategory4CustomCategory6 = addDProp(idGroup, "customCategory4CustomCategory6", "Код(4)", customCategory4, customCategory6);
        customCategory6CustomCategory9 = addDProp(idGroup, "customCategory6CustomCategory9", "Код(6)", customCategory6, customCategory9);
        customCategory9CustomCategory10 = addDProp(idGroup, "customCategory9CustomCategory10", "Код(9)", customCategory9, customCategory10);
        customCategory6CustomCategory10 = addJProp(idGroup, "customCategory6CustomCategory10", "Код(6)", customCategory6CustomCategory9, customCategory9CustomCategory10, 1);
        customCategory4CustomCategory10 = addJProp(idGroup, "customCategory4CustomCategory10", "Код(4)", customCategory4CustomCategory6, customCategory6CustomCategory10, 1);

        customCategory6CustomCategoryOrigin = addDProp(idGroup, "customCategory6CustomCategoryOrigin", "Код(6)", customCategory6, customCategoryOrigin);
        customCategory4CustomCategoryOrigin = addJProp(idGroup, "customCategory4CustomCategoryOrigin", "Код(4)", customCategory4CustomCategory6, customCategory6CustomCategoryOrigin, 1);

        customCategory10CustomCategoryOrigin = addDProp(idGroup, "customCategory10CustomCategoryOrigin", "Код по умолчанию(ИД)", customCategory10, customCategoryOrigin);
        sidCustomCategory10CustomCategoryOrigin = addJProp(baseGroup, "sidCustomCategory10CustomCategoryOrigin", "Код по умолчанию", sidCustomCategory10, customCategory10CustomCategoryOrigin, 1);
        sidCustomCategory10CustomCategoryOrigin.property.preferredCharWidth = 10;
        sidCustomCategory10CustomCategoryOrigin.property.minimumCharWidth = 10;

        sidCustomCategory4CustomCategory6 = addJProp(baseGroup, "sidCustomCategory4CustomCategory6", "Код(4)", sidCustomCategory4, customCategory4CustomCategory6, 1);
        sidCustomCategory6CustomCategory9 = addJProp(baseGroup, "sidCustomCategory6CustomCategory9", "Код(6)", sidCustomCategory6, customCategory6CustomCategory9, 1);
        sidCustomCategory9CustomCategory10 = addJProp(idGroup, "sidCustomCategory9CustomCategory10", "Код(9)", sidCustomCategory9, customCategory9CustomCategory10, 1);
        sidCustomCategory6CustomCategoryOrigin = addJProp(idGroup, "sidCustomCategory6CustomCategoryOrigin", "Код(6)", sidCustomCategory6, customCategory6CustomCategoryOrigin, 1);

        nameCustomCategory4CustomCategory6 = addJProp(baseGroup, "nameCustomCategory4CustomCategory6", "Наименование(4)", nameCustomCategory, customCategory4CustomCategory6, 1);
        nameCustomCategory6CustomCategory9 = addJProp(baseGroup, "nameCustomCategory6CustomCategory9", "Наименование(6)", nameCustomCategory, customCategory6CustomCategory9, 1);
        nameCustomCategory9CustomCategory10 = addJProp(baseGroup, "nameCustomCategory9CustomCategory10", "Наименование(9)", nameCustomCategory, customCategory9CustomCategory10, 1);
        nameCustomCategory6CustomCategory10 = addJProp(baseGroup, "nameCustomCategory6CustomCategory10", "Наименование(6)", nameCustomCategory, customCategory6CustomCategory10, 1);
        nameCustomCategory4CustomCategory10 = addJProp(baseGroup, "nameCustomCategory4CustomCategory10", "Наименование(4)", nameCustomCategory, customCategory4CustomCategory10, 1);

        nameCustomCategory6CustomCategoryOrigin = addJProp(baseGroup, "nameCustomCategory6CustomCategoryOrigin", "Наименование(6)", nameCustomCategory, customCategory6CustomCategoryOrigin, 1);
        nameCustomCategory4CustomCategoryOrigin = addJProp(baseGroup, "nameCustomCategory4CustomCategoryOrigin", "Наименование(4)", nameCustomCategory, customCategory4CustomCategoryOrigin, 1);

        relationCustomCategory10CustomCategoryOrigin = addDProp(baseGroup, "relationCustomCategory10CustomCategoryOrigin", "Связь ТН ВЭД", LogicalClass.instance, customCategory10, customCategoryOrigin);

//        addConstraint(addJProp("По умолчанию должен быть среди связанных", and(true, false),
//                addCProp(LogicalClass.instance, true, customCategoryOrigin), 1,
//                addJProp(relationCustomCategory10CustomCategoryOrigin, customCategory10CustomCategoryOrigin, 1, 1), 1,
//                addJProp(is(customCategory10), customCategory10CustomCategoryOrigin, 1), 1), true);

        // Supplier
        currencySupplier = addDProp(idGroup, "currencySupplier", "Валюта (ИД)", currency, supplier);
        nameCurrencySupplier = addJProp(baseGroup, "nameCurrencySupplier", "Валюта", name, currencySupplier, 1);

        sidColorSupplier = addDProp(baseGroup, "sidColorSupplier", "Код", StringClass.get(50), colorSupplier);

        supplierColorSupplier = addDProp(idGroup, "supplierColorSupplier", "Поставщик (ИД)", supplier, colorSupplier);
        nameSupplierColorSupplier = addJProp(baseGroup, "nameSupplierColorSupplier", "Поставщик", name, supplierColorSupplier, 1);

        colorSIDSupplier = addCGProp(idGroup, "colorSIDSupplier", "Цвет поставщика (ИД)", object(colorSupplier), sidColorSupplier, sidColorSupplier, 1, supplierColorSupplier, 1);

        sidSizeSupplier = addDProp(baseGroup, "sidSizeSupplier", "Код", StringClass.get(50), sizeSupplier);

        supplierSizeSupplier = addDProp(idGroup, "supplierSizeSupplier", "Поставщик (ИД)", supplier, sizeSupplier);
        nameSupplierSizeSupplier = addJProp(baseGroup, "nameSupplierSizeSupplier", "Поставщик", name, supplierSizeSupplier, 1);

        sizeSIDSupplier = addCGProp(idGroup, "sizeSIDSupplier", "Размер поставщика (ИД)", object(sizeSupplier), sidSizeSupplier, sidSizeSupplier, 1, supplierSizeSupplier, 1);

        supplierCountrySupplier = addDProp(idGroup, "supplierCountrySupplier", "Поставщик (ИД)", supplier, countrySupplier);
        nameSupplierCountrySupplier = addJProp(baseGroup, "nameSupplierCountrySupplier", "Поставщик", name, supplierCountrySupplier, 1);

        countryCountrySupplier = addDProp(idGroup, "countryCountrySupplier", "Страна (ИД)", country, countrySupplier);
        nameCountryCountrySupplier = addJProp(baseGroup, "nameCountryCountrySupplier", "Страна", name, countryCountrySupplier, 1);

        countryNameSupplier = addCGProp(idGroup, "countryNameSupplier", "Страна поставщика", object(countrySupplier), name, name, 1, supplierCountrySupplier, 1);

        supplierBrandSupplier = addDProp(idGroup, "supplierBrandSupplier", "Поставщик (ИД)", supplier, brandSupplier);
        nameSupplierBrandSupplier = addJProp(baseGroup, "nameSupplierBrandSupplier", "Поставщик", name, supplierBrandSupplier, 1);

        brandSupplierSupplier = addDProp(idGroup, "brandSupplierSupplier", "Бренд (ИД)", brandSupplier, supplier);
        nameBrandSupplierSupplier = addJProp(baseGroup, "nameBrandSupplierSupplier", "Бренд по умолчанию", name, brandSupplierSupplier, 1);

        addConstraint(addJProp("Бренд по умолчанию для поставщика должен соответствовать брендам поставщика", diff2, 1, addJProp(supplierBrandSupplier, brandSupplierSupplier, 1), 1), true);

        supplierDocument = addDProp(idGroup, "supplierDocument", "Поставщик (ИД)", supplier, document);
        supplierPriceDocument = addJProp(idGroup, "supplierPricedDocument", "Поставщик(ИД)", and1, supplierDocument, 1, is(priceDocument), 1);
        nameSupplierDocument = addJProp(baseGroup, "nameSupplierDocument", "Поставщик", name, supplierDocument, 1);

        currencyDocument = addDCProp(idGroup, "currencyDocument", "Валюта (ИД)", currencySupplier, supplierPriceDocument, 1);
        nameCurrencyDocument = addJProp(baseGroup, "nameCurrencyDocument", "Валюта", name, currencyDocument, 1);

        // Order
        destinationDestinationDocument = addDProp(idGroup, "destinationDestinationDocument", "Пункт назначения (ИД)", destination, destinationDocument);
        nameDestinationDestinationDocument = addJProp(baseGroup, "nameDestinationDestinationDocument", "Пункт назначения (наим.)", name, destinationDestinationDocument, 1);
        sidDestinationDestinationDocument = addJProp(baseGroup, "sidDestinationDestinationDocument", "Пункт назначения", sidDestination, destinationDestinationDocument, 1);

        // Invoice
        importerInvoice = addDProp(idGroup, "importerDocument", "Импортер (ИД)", importer, invoice);
        nameImporterInvoice = addJProp(baseGroup, "nameImporterInvoice", "Импортер", name, importerInvoice, 1);

        // Shipment
        quantityPalletShipment = addDProp(baseGroup, "quantityPalletShipment", "Кол-во паллет", IntegerClass.instance, shipment);
        netWeightShipment = addDProp(baseGroup, "netWeightShipment", "Вес нетто", DoubleClass.instance, shipment);
        grossWeightShipment = addDProp(baseGroup, "grossWeightShipment", "Вес брутто", DoubleClass.instance, shipment);

        grossWeightPallet = addDProp(baseGroup, "grossWeightPallet", "Вес брутто", DoubleClass.instance, pallet);
        quantityBoxShipment = addDProp(baseGroup, "quantityBoxShipment", "Кол-во коробов", DoubleClass.instance, shipment);

        // Item
        articleCompositeItem = addDProp(idGroup, "articleCompositeItem", "Артикул (ИД)", articleComposite, item);
        equalsItemArticleComposite = addJProp(baseGroup, "equalsItemArticleComposite", "Вкл.", equals2, articleCompositeItem, 1, 2);

        articleSku = addCUProp(idGroup, "articleSku", "Артикул (ИД)", object(articleSingle), articleCompositeItem);

        addItemBarcode = addJProp(true, "Ввод товара по штрих-коду", addAAProp(item, barcode), 1);

        // Article
        sidArticle = addDProp(baseGroup, "sidArticle", "Код", StringClass.get(50), article);
        sidArticleSku = addJProp(baseGroup, "sidArticleSku", "Код", sidArticle, articleSku, 1);

        originalNameArticle = addDProp(supplierAttributeGroup, "originalNameArticle", "Имя производителя (ориг.)", StringClass.get(50), article);
        originalNameArticleSku = addJProp(supplierAttributeGroup, "originalNameArticleSku", "Имя производителя (ориг.)", originalNameArticle, articleSku, 1);

        //Category
        categoryArticle = addDProp(idGroup, "categoryArticle", "Категория товара (ИД)", category, article);
        nameOriginCategoryArticle = addJProp(intraAttributeGroup, "nameOriginCategoryArticle", "Категория товара (ориг.)", nameOrigin, categoryArticle, 1);
        nameCategoryArticle = addJProp(intraAttributeGroup, "nameCategoryArticle", "Категория товара", name, categoryArticle, 1);
        categoryArticleSku = addJProp(idGroup, true, "categoryArticleSku", "Категория товара (ИД)", categoryArticle, articleSku, 1);
        nameCategoryArticleSku = addJProp(intraAttributeGroup, "nameCategoryArticleSku", "Категория товара", name, categoryArticleSku, 1);
        nameOriginCategoryArticleSku = addJProp(intraAttributeGroup, "nameOriginCategoryArticleSku", "Категория товара", nameOrigin, categoryArticleSku, 1);

        nameArticle = addSUProp(baseGroup, "nameArticle", "Наименование", Union.OVERRIDE, originalNameArticle, nameCategoryArticle);

        customCategoryOriginArticle = addDProp(idGroup, "customCategoryOriginArticle", "ТН ВЭД (ориг.) (ИД)", customCategoryOrigin, article);
        sidCustomCategoryOriginArticle = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticle", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategoryOriginArticleSku = addJProp(idGroup, true, "customCategoryOriginArticleSku", "ТН ВЭД (ориг.) (ИД)", customCategoryOriginArticle, articleSku, 1);
        sidCustomCategoryOriginArticleSku = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticleSku", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticleSku, 1);

        customCategory10DataSku = addDProp(idGroup, "customCategory10DataSku", "ТН ВЭД (ИД)", customCategory10, sku);
        customCategory10CustomCategoryOriginArticle = addJProp(idGroup, "customCategory10CustomCategoryOriginArticle", "ТН ВЭД (ИД)", customCategory10CustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategory10CustomCategoryOriginArticleSku = addJProp(idGroup, "customCategory10CustomCategoryOriginArticleSku", "ТН ВЭД (ИД)", customCategory10CustomCategoryOriginArticle, articleSku, 1);
        customCategory10Sku = addSUProp(idGroup, "customCategory10Sku", "ТН ВЭД (ИД)", Union.OVERRIDE, customCategory10CustomCategoryOriginArticleSku, customCategory10DataSku);
        sidCustomCategory10Sku = addJProp(baseGroup, "sidCustomCategory10Sku", "ТН ВЭД", sidCustomCategory10, customCategory10Sku, 1);
        /*addConstraint(addJProp("Выбранный должен быть среди связанных кодов", andNot1, addCProp(LogicalClass.instance, true, article), 1,
                   addJProp(relationCustomCategory10CustomCategoryOrigin, customCategory10Article, 1, customCategoryOriginArticle, 1), 1), true);*/

        // unitOfMeasure
        unitOfMeasureCategory = addDProp(idGroup, "unitOfMeasureCategory", "Единица измерения (ИД)", unitOfMeasure, category);
        nameUnitOfMeasureCategory = addJProp(baseGroup, "nameUnitOfMeasureCategory", "Единица измерения", name, unitOfMeasureCategory, 1);
        unitOfMeasureCategoryArticle = addJProp(idGroup, "unitOfMeasureCategoryArticle", "Единица измерения (ИД)", unitOfMeasureCategory, categoryArticle, 1);
        unitOfMeasureDataArticle = addDProp(idGroup, "unitOfMeasureDataArticle", "Единица измерения (ИД)", unitOfMeasure, article);
        unitOfMeasureArticle = addSUProp(idGroup, "unitOfMeasureArticle", "Единица измерения", Union.OVERRIDE, unitOfMeasureCategoryArticle, unitOfMeasureDataArticle);

        nameOriginUnitOfMeasureArticle = addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticle", "Единица измерения (ориг.)", nameOrigin, unitOfMeasureArticle, 1);
        nameUnitOfMeasureArticle = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticle", "Единица измерения", name, unitOfMeasureArticle, 1);
        unitOfMeasureArticleSku = addJProp(idGroup, true, "unitOfMeasureArticleSku", "Ед. изм. товара (ИД)", unitOfMeasureArticle, articleSku, 1);
        nameUnitOfMeasureArticleSku = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticleSku", "Ед. изм. товара", name, unitOfMeasureArticleSku, 1);
        nameOriginUnitOfMeasureArticleSku = addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticleSku", "Ед. изм. товара", nameOrigin, unitOfMeasureArticleSku, 1);

        // Weight
        netWeightArticle = addDProp(supplierAttributeGroup, "netWeightArticle", "Вес нетто (ориг.)", DoubleClass.instance, article);
        netWeightDataSku = addDProp(intraAttributeGroup, "netWeightDataSku", "Вес нетто", DoubleClass.instance, sku);
        netWeightArticleSku = addJProp(supplierAttributeGroup, "netWeightArticleSku", "Вес нетто", netWeightArticle, articleSku, 1);
        netWeightSku = addSUProp(intraAttributeGroup, "netWeightSku", true, "Вес нетто", Union.OVERRIDE, netWeightArticleSku, netWeightDataSku);

        // Composition
        mainCompositionOriginArticle = addDProp(supplierAttributeGroup, "mainCompositionOriginArticle", "Состав", COMPOSITION_CLASS, article);
        additionalCompositionOriginArticle = addDProp(supplierAttributeGroup, "additionalCompositionOriginArticle", "Доп. состав", COMPOSITION_CLASS, article);

        mainCompositionOriginArticleSku = addJProp(supplierAttributeGroup, "mainCompositionOriginArticleSku", "Состав", mainCompositionOriginArticle, articleSku, 1);
        additionalCompositionOriginArticleSku = addJProp(supplierAttributeGroup, "additionalCompositionOriginArticleSku", "Доп. состав", additionalCompositionOriginArticle, articleSku, 1);

        mainCompositionOriginDataSku = addDProp(intraAttributeGroup, "mainCompositionOriginDataSku", "Состав", COMPOSITION_CLASS, sku);
        additionalCompositionOriginDataSku = addDProp(intraAttributeGroup, "additionalCompositionOriginDataSku", "Доп. состав", COMPOSITION_CLASS, sku);

        mainCompositionOriginSku = addSUProp(intraAttributeGroup, "mainCompositionOriginSku", "Состав", Union.OVERRIDE, mainCompositionOriginArticleSku, mainCompositionOriginDataSku);
        additionalCompositionOriginSku = addSUProp(intraAttributeGroup, "additionalCompositionOriginSku", "Доп. состав", Union.OVERRIDE, additionalCompositionOriginArticleSku, additionalCompositionOriginDataSku);

        mainCompositionArticle = addDProp(intraAttributeGroup, "mainCompositionArticle", "Состав (перевод)", COMPOSITION_CLASS, article);
        additionalCompositionArticle = addDProp(intraAttributeGroup, "additionalCompositionArticle", "Доп. состав (перевод)", COMPOSITION_CLASS, article);

        mainCompositionSku = addDProp(intraAttributeGroup, "mainCompositionSku", "Состав (перевод)", COMPOSITION_CLASS, sku);
        additionalCompositionSku = addDProp(intraAttributeGroup, "additionalCompositionSku", "Доп. состав (перевод)", COMPOSITION_CLASS, sku);

        // Country
        countrySupplierOfOriginArticle = addDProp(idGroup, "countrySupplierOfOriginArticle", "Страна происхождения (ИД)", countrySupplier, article);
        nameCountrySupplierOfOriginArticle = addJProp(supplierAttributeGroup, "nameCountrySupplierOfOriginArticle", "Страна происхождения (ориг.)", name, countrySupplierOfOriginArticle, 1);

        countrySupplierOfOriginArticleSku = addJProp(idGroup, "countrySupplierOfOriginArticleSku", "Страна происхождения (ИД)", countrySupplierOfOriginArticle, articleSku, 1);
        nameCountrySupplierOfOriginArticleSku = addJProp(supplierAttributeGroup, "nameCountrySupplierOfOriginArticleSku", "Страна происхождения (ориг.)", name, countrySupplierOfOriginArticleSku, 1);

        countryOfOriginArticle = addJProp(idGroup, "countryOfOriginArticle", "Страна происхождения (ИД)", countryCountrySupplier, countrySupplierOfOriginArticle, 1);
        nameCountryOfOriginArticle = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticle", "Страна происхождения", nameOriginCountry, countryOfOriginArticle, 1);

        countryOfOriginArticleSku = addJProp(idGroup, "countryOfOriginArticleSku", "Страна происхождения (ИД)", countryOfOriginArticle, articleSku, 1);
        nameCountryOfOriginArticleSku = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSku", "Страна происхождения", nameOriginCountry, countryOfOriginArticleSku, 1);

        countryOfOriginDataSku = addDProp(idGroup, "countryOfOriginDataSku", "Страна происхождения (ИД) (первичное)", country, sku);

        countryOfOriginSku = addSUProp(idGroup, "countryOfOriginSku", "Страна происхождения (ИД)", Union.OVERRIDE, countryOfOriginArticleSku, countryOfOriginDataSku);
        nameCountryOfOriginSku = addJProp(intraAttributeGroup, "nameCountryOfOriginSku", "Страна происхождения", nameOriginCountry, countryOfOriginSku, 1);
        nameCountrySku = addJProp(intraAttributeGroup, "nameCountrySku", "Страна происхождения", name, countryOfOriginSku, 1);

        // Supplier
        supplierArticle = addDProp(idGroup, "supplierArticle", "Поставщик (ИД)", supplier, article);
        nameSupplierArticle = addJProp(baseGroup, "nameSupplierArticle", "Поставщик", name, supplierArticle, 1);

        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику страны артикула", diff2,
                supplierArticle, 1, addJProp(supplierCountrySupplier, countrySupplierOfOriginArticle, 1), 1), true);

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

        executeArticleCompositeItemSIDSupplier = addJProp(true, "Замена артикула", addEPAProp(articleCompositeItem), articleSIDSupplier, 2, 3, 1);

        supplierSku = addJProp(idGroup, "supplierSku", "Поставщик (ИД)", supplierArticle, articleSku, 1);
        nameSupplierSku = addJProp(baseGroup, "nameSupplierSku", "Поставщик", name, supplierSku, 1);

        colorSupplierItem = addDProp(idGroup, "colorSupplierItem", "Цвет поставщика (ИД)", colorSupplier, item);
        sidColorSupplierItem = addJProp(itemAttributeGroup, "sidColorSupplierItem", "Код цвета", sidColorSupplier, colorSupplierItem, 1);
        nameColorSupplierItem = addJProp(itemAttributeGroup, "nameColorSupplierItem", "Цвет поставщика", name, colorSupplierItem, 1);

        sizeSupplierItem = addDProp(itemAttributeGroup, "sizeSupplierItem", "Размер поставщика (ИД)", sizeSupplier, item);
        sidSizeSupplierItem = addJProp(itemAttributeGroup, "sidSizeSupplierItem", "Размер поставщика", sidSizeSupplier, sizeSupplierItem, 1);

        addConstraint(addJProp("Поставщик товара должен соответствовать цвету поставщика", diff2,
                supplierSku, 1,
                addJProp(supplierColorSupplier, colorSupplierItem, 1), 1), true);

        addConstraint(addJProp("Поставщик товара должен соответствовать размеру поставщика", diff2,
                supplierSku, 1,
                addJProp(supplierSizeSupplier, sizeSupplierItem, 1), 1), true);

        sidDocument = addDProp(baseGroup, "sidDocument", "Код", StringClass.get(50), document);
        documentSIDSupplier = addCGProp(idGroup, "documentSIDSupplier", "Документ поставщика (ИД)", object(document), sidDocument, sidDocument, 1, supplierDocument, 1);

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

        numberDataListSku = addDProp(baseGroup, "numberDataListSku", "Номер", IntegerClass.instance, list, sku);
        numberArticleListSku = addJProp(baseGroup, "numberArticleListSku", "Номер (артикула)", numberListArticle, 1, articleSku, 2);

        numberListSku = addSUProp("numberListSku", "Номер", Union.OVERRIDE, numberArticleListSku, numberDataListSku);

        numberDocumentArticle = addSGProp(baseGroup, "inDocumentArticle", numberListArticle, documentList, 1, 2);

        incrementNumberListSID = addJProp(true, "Добавить строку", andNot1,
                addJProp(true, addIAProp(numberListArticle, 1),
                        1, articleSIDList, 2, 1), 1, 2,
                numberListSIDArticle, 1, 2); // если еще не было добавлено такой строки

        // кол-во заказа
        quantityDataListSku = addDProp("quantityDataListSku", "Кол-во (первичное)", DoubleClass.instance, list, sku);
        quantityListSku = quantityDataListSku; //addJProp(baseGroup, "quantityListSku", true, "Кол-во", and1, quantityDataListSku, 1, 2, numberListSku, 1, 2);

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
//        quantityListArticle.setDerivedForcedChange(orderedInvoiceArticle, 1, 2, numberListArticle, 1, 2);

        invoicedOrderArticle = addSGProp(baseGroup, "invoicedOrderArticle", "Выставлено инвойсов", invoicedOrderSku, 1, articleSku, 2);

        // цены
        priceDocumentArticle = addDProp(baseGroup, "priceDocumentArticle", "Цена", DoubleClass.instance, priceDocument, article);
        priceDataDocumentItem = addDProp(baseGroup, "priceDataDocumentItem", "Цена по товару", DoubleClass.instance, priceDocument, item);
        priceArticleDocumentSku = addJProp(baseGroup, "priceArticleDocumentItem", "Цена по артикулу", priceDocumentArticle, 1, articleSku, 2);
        priceDocumentSku = addSUProp(baseGroup, "priceDocumentSku", "Цена", Union.OVERRIDE, priceArticleDocumentSku, priceDataDocumentItem);

        priceOrderInvoiceArticle = addJProp(and1, priceDocumentArticle, 1, 3, inOrderInvoice, 1, 2);
        priceOrderedInvoiceArticle = addMGProp(baseGroup, "priceOrderedInvoiceArticle", "Цена в заказе", priceOrderInvoiceArticle, 2, 3);
        // todo : не работает
        priceDocumentArticle.setDerivedForcedChange(priceOrderedInvoiceArticle, 1, 2, numberDocumentArticle, 1, 2);

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
        creationPalletPallet = addDProp(idGroup, "creationPalletPallet", "Операция (ИД)", creationPallet, pallet);
        routeCreationPalletPallet = addJProp(idGroup, "routeCreationPalletPallet", true, "Маршрут (ИД)", routeCreationPallet, creationPalletPallet, 1);
        nameRouteCreationPalletPallet = addJProp(baseGroup, "nameRouteCreationPalletPallet", true, "Маршрут", name, routeCreationPalletPallet, 1);

        freightPallet = addDProp(baseGroup, "freightPallet", "Фрахт (ИД)", freight, pallet);
        equalsPalletFreight = addJProp(baseGroup, "equalsPalletFreight", "Вкл.", equals2, freightPallet, 1, 2);

        // инвойсы напрямую во фрахты

        freightDirectInvoice = addDProp(baseGroup, "freightDirectInvoice", "Фрахт (ИД)", freight, directInvoice);
        equalsDirectInvoiceFreight = addJProp(baseGroup, "equalsDirectInvoiceFreight", "Вкл.", equals2, freightDirectInvoice, 1, 2);

        grossWeightDirectInvoice = addDProp(baseGroup, "grossWeightDirectInvoice", "Вес брутто", DoubleClass.instance, directInvoice);

        // freight box
        creationFreightBoxFreightBox = addDProp(idGroup, "creationFreightBoxFreightBox", "Операция (ИД)", creationFreightBox, freightBox);

        palletFreightBox = addDProp(idGroup, "palletFreightBox", "Паллета (ИД)", pallet, freightBox);
        barcodePalletFreightBox = addJProp(baseGroup, "barcodePalletFreightBox", "Паллета (штрих-код)", barcode, palletFreightBox, 1);

        routeCreationFreightBoxFreightBox = addJProp(idGroup, "routeCreationFreightBoxFreightBox", true, "Маршрут (ИД)", routeCreationFreightBox, creationFreightBoxFreightBox, 1);
        nameRouteCreationFreightBoxFreightBox = addJProp(baseGroup, "nameRouteCreationFreightBoxFreightBox", true, "Маршрут", name, routeCreationFreightBoxFreightBox, 1);

        freightFreightBox = addJProp(idGroup, "freightFreightBox", "Фрахт короба транспортировки", freightPallet, palletFreightBox, 1);

        destinationFreightBox = addDProp(idGroup, "destinationFreightBox", "Пункт назначения (ИД)", destination, freightBox);
        nameDestinationFreightBox = addJProp(baseGroup, "nameDestinationFreightBox", "Пункт назначения", name, destinationFreightBox, 1);

        // поставка на склад
        inInvoiceShipment = addDProp(baseGroup, "inInvoiceShipment", "Вкл", LogicalClass.instance, invoice, shipment);

        inSupplierBoxShipment = addJProp(baseGroup, "inSupplierBoxShipment", "Вкл", inInvoiceShipment, boxInvoiceSupplierBox, 1, 2);

        invoicedShipmentSku = addSGProp(baseGroup, "invoicedShipmentSku", true, "Ожид. (пост.)",
                addJProp(and1, quantityDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        //sku shipment detail
        skuShipmentDetail = addDProp(idGroup, "skuShipmentDetail", "SKU (ИД)", sku, shipmentDetail);
        barcodeSkuShipmentDetail = addJProp(baseGroup, "barcodeSkuShipmentDetail", "Штрих-код SKU", barcode, skuShipmentDetail, 1);

        articleShipmentDetail = addJProp(idGroup, "articleShipmentDetail", "Артикул (ИД)", articleSku, skuShipmentDetail, 1);
        sidArticleShipmentDetail = addJProp(baseGroup, "sidArticleShipmentDetail", "Артикул", sidArticle, articleShipmentDetail, 1);

        colorSupplierItemShipmentDetail = addJProp(idGroup, "colorSupplierItemShipmentDetail", "Цвет поставщика (ИД)", colorSupplierItem, skuShipmentDetail, 1);
        sidColorSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "sidColorSupplierItemShipmentDetail", "Код цвета", sidColorSupplier, colorSupplierItemShipmentDetail, 1);
        nameColorSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "nameColorSupplierItemShipmentDetail", "Цвет поставщика", name, colorSupplierItemShipmentDetail, 1);

        sizeSupplierItemShipmentDetail = addJProp(idGroup, "sizeSupplierItemShipmentDetail", "Размер поставщика (ИД)", sizeSupplierItem, skuShipmentDetail, 1);
        sidSizeSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "sidSizeSupplierItemShipmentDetail", "Размер поставщика", sidSizeSupplier, sizeSupplierItemShipmentDetail, 1);

        nameBrandSupplierArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "nameBrandSupplierArticleSkuShipmentDetail", "Бренд", nameBrandSupplierArticleSku, skuShipmentDetail, 1);
        originalNameArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "originalNameArticleSkuShipmentDetail", "Имя производителя (ориг.)", originalNameArticleSku, skuShipmentDetail, 1);

        categoryArticleSkuShipmentDetail = addJProp(idGroup, true, "categoryArticleSkuShipmentDetail", "Категория товара (ИД)", categoryArticleSku, skuShipmentDetail, 1);
        nameOriginCategoryArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameOriginCategoryArticleSkuShipmentDetail", "Категория товара", nameOrigin, categoryArticleSkuShipmentDetail, 1);

        customCategoryOriginArticleSkuShipmentDetail = addJProp(idGroup, true, "customCategoryOriginArticleSkuShipmentDetail", "ТН ВЭД (ИД)", customCategoryOriginArticleSku, skuShipmentDetail, 1);
        sidCustomCategoryOriginArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "sidCustomCategoryOriginArticleSkuShipmentDetail", "Код ТН ВЭД", sidCustomCategoryOrigin, customCategoryOriginArticleSkuShipmentDetail, 1);

        netWeightArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "netWeightArticleSkuShipmentDetail", "Вес нетто (ориг.)", netWeightArticleSku, skuShipmentDetail, 1);
        netWeightSkuShipmentDetail = addJProp(intraAttributeGroup, true, "netWeightSkuShipmentDetail", "Вес нетто", netWeightSku, skuShipmentDetail, 1);

        countryOfOriginArticleSkuShipmentDetail = addJProp(idGroup, true, "countryOfOriginArticleSkuShipmentDetail", "Страна происхождения (ориг.) (ИД)", countryOfOriginArticleSku, skuShipmentDetail, 1);
        nameCountryOfOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSkuShipmentDetail", "Страна происхождения", nameOriginCountry, countryOfOriginArticleSkuShipmentDetail, 1);

        countryOfOriginSkuShipmentDetail = addJProp(idGroup, true, "countryOfOriginSkuShipmentDetail", "Страна происхождения (ИД)", countryOfOriginSku, skuShipmentDetail, 1);
        nameCountryOfOriginSkuShipmentDetail = addJProp(intraAttributeGroup, "nameCountryOfOriginSkuShipmentDetail", "Страна происхождения", nameOriginCountry, countryOfOriginSkuShipmentDetail, 1);

        mainCompositionOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "mainCompositionOriginArticleSkuShipmentDetail", "Состав", mainCompositionOriginArticleSku, skuShipmentDetail, 1);
        mainCompositionOriginSkuShipmentDetail = addJProp(intraAttributeGroup, true, "mainCompositionOriginSkuShipmentDetail", "Состав", mainCompositionOriginSku, skuShipmentDetail, 1);

        additionalCompositionOriginSkuShipmentDetail = addJProp(intraAttributeGroup, true, "additionalCompositionOriginSkuShipmentDetail", "Дополнительный состав", additionalCompositionOriginSku, skuShipmentDetail, 1);

        unitOfMeasureArticleSkuShipmentDetail = addJProp(idGroup, true, "unitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара (ИД)", unitOfMeasureArticleSku, skuShipmentDetail, 1);
        nameOriginUnitOfMeasureArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара", nameOrigin, unitOfMeasureArticleSkuShipmentDetail, 1);

        // stock shipment detail
        stockShipmentDetail = addDProp(idGroup, "stockShipmentDetail", "Место хранения (ИД)", stock, shipmentDetail);
        barcodeStockShipmentDetail = addJProp(baseGroup, "barcodeStockShipmentDetail", "Штрих-код МХ", barcode, stockShipmentDetail, 1);

        routeFreightBoxShipmentDetail = addJProp(idGroup, "routeFreightBoxShipmentDetail", "Маршрут (ИД)", routeCreationFreightBoxFreightBox, stockShipmentDetail, 1);
        nameRouteFreightBoxShipmentDetail = addJProp(baseGroup, "nameRouteFreightBoxShipmentDetail", "Маршрут", name, routeFreightBoxShipmentDetail, 1);

        boxShipmentBoxShipmentDetail = addDProp(idGroup, "boxShipmentBoxShipmentDetail", "Поставка (ИД)", boxShipment, boxShipmentDetail);
        simpleShipmentSimpleShipmentDetail = addDProp(idGroup, "simpleShipmentSimpleShipmentDetail", "Поставка (ИД)", simpleShipment, simpleShipmentDetail);
        shipmentShipmentDetail = addCUProp(idGroup, "shipmentShipmentDetail", "Поставка (ИД)", boxShipmentBoxShipmentDetail, simpleShipmentSimpleShipmentDetail);
        sidShipmentShipmentDetail = addJProp(baseGroup, "sidShipmentShipmentDetail", "Поставка", sidDocument, shipmentShipmentDetail, 1);

        // supplier box shipmentDetail
        supplierBoxShipmentDetail = addDProp(idGroup, "supplierBoxShipmentDetail", "Короб поставщика (ИД)", supplierBox, boxShipmentDetail);
        sidSupplierBoxShipmentDetail = addJProp(baseGroup, "sidSupplierBoxShipmentDetail", "Код короба поставщика", sidSupplierBox, supplierBoxShipmentDetail, 1);
        barcodeSupplierBoxShipmentDetail = addJProp(baseGroup, "barcodeSupplierBoxShipmentDetail", "Штрих-код короба поставщика", barcode, supplierBoxShipmentDetail, 1);

        quantityShipmentDetail = addDProp(baseGroup, "quantityShipmentDetail", "Кол-во", DoubleClass.instance, shipmentDetail);

        userShipmentDetail = addDCProp(idGroup, "userShipmentDetail", "Пользователь (ИД)", currentUser, true, is(shipmentDetail), 1);
        nameUserShipmentDetail = addJProp(baseGroup, "nameUserShipmentDetail", "Пользователь", name, userShipmentDetail, 1);

        timeShipmentDetail = addDCProp(baseGroup, "timeShipmentDetail", "Время ввода", currentDateTime, true, is(shipmentDetail), 1);

        addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode = addJProp(true, "Добавить строку поставки",
                addAAProp(boxShipmentDetail, boxShipmentBoxShipmentDetail, supplierBoxShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail),
                1, 2, 3, barcodeToObject, 4, addCProp(DoubleClass.instance, 1));

        addSimpleShipmentSimpleShipmentDetailStockBarcode = addJProp(true, "Добавить строку поставки",
                addAAProp(simpleShipmentDetail, simpleShipmentSimpleShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail),
                1, 2, barcodeToObject, 3, addCProp(DoubleClass.instance, 1));

        quantitySupplierBoxBoxShipmentStockSku = addSGProp(baseGroup, "quantitySupplierBoxBoxShipmentStockSku", "Кол-во оприход.", quantityShipmentDetail,
                supplierBoxShipmentDetail, 1, boxShipmentBoxShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        quantitySimpleShipmentStockSku = addSGProp(baseGroup, "quantitySimpleShipmentStockSku", "Кол-во оприход.", quantityShipmentDetail,
                simpleShipmentSimpleShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        quantityShipDimensionShipmentStockSku = addCUProp(baseGroup, "quantityShipDimensionShipmentStockSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                addJProp(and1, quantitySimpleShipmentStockSku, 2, 3, 4, equals2, 1, 2));

        quantityBoxInvoiceBoxShipmentStockSku = addSGProp(baseGroup, "quantityBoxInvoiceBoxShipmentStockSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                boxInvoiceSupplierBox, 1, 2, 3, 4);

        invoicedSimpleInvoiceSimpleShipmentStockSku = addJProp(and(false, false, false, false), quantityDocumentSku, 1, 4, inInvoiceShipment, 1, 2, is(simpleInvoice), 1, is(simpleShipment), 2, is(stock), 3);

        quantitySimpleInvoiceSimpleShipmentStockSku = addPGProp(baseGroup, "quantitySimpleInvoiceSimpleShipmentStockSku", true, 0, true, "Кол-во оприход.",
                invoicedSimpleInvoiceSimpleShipmentStockSku,
                quantitySimpleShipmentStockSku, 2, 3, 4);

        quantityInvoiceShipmentStockSku = addCUProp(baseGroup, "quantityInvoiceShipmentStockSku", "Кол-во оприход.",
                quantityBoxInvoiceBoxShipmentStockSku, quantitySimpleInvoiceSimpleShipmentStockSku);

        quantityInvoiceStockSku = addSGProp(baseGroup, "quantityInvoiceStockSku", true, "Кол-во оприход.", quantityInvoiceShipmentStockSku, 1, 3, 4);

        priceInInvoiceStockSku = addJProp(baseGroup, "priceInInvoiceStockSku", "Цена входная", and1,
                priceDocumentSku, 1, 3, quantityInvoiceStockSku, 1, 2, 3);

        quantityShipDimensionStock = addSGProp(baseGroup, "quantityShipDimensionStock", "Всего оприход.", quantityShipDimensionShipmentStockSku, 1, 3);

        quantityShipDimensionShipmentSku = addSGProp(baseGroup, "quantityShipDimensionShipmentSku", "Оприход. (короб)", quantityShipDimensionShipmentStockSku, 1, 2, 4);

        quantityShipmentStockSku = addSGProp(baseGroup, "quantityShipmentStockSku", true, "Кол-во оприход.", quantityShipDimensionShipmentStockSku, 2, 3, 4);

        quantityShipmentStock = addSGProp(baseGroup, "quantityShipmentStock", "Всего оприход.", quantityShipmentStockSku, 1, 2);

        quantityShipmentSku = addSGProp(baseGroup, "quantityShipmentSku", "Оприход. (пост.)", quantityShipmentStockSku, 1, 3);

        quantityStockSku = addSGProp(baseGroup, "quantityStockSku", true, "Оприход. (МХ)", quantityShipmentStockSku, 2, 3);

        quantityStockArticle = addSGProp(baseGroup, "quantityStockArticle", "Кол-во по артикулу", quantityStockSku, 1, articleSku, 2);

        quantityPalletSku = addSGProp(baseGroup, "quantityPalletSku", "Оприход. (пал.)", quantityStockSku, palletFreightBox, 1, 2);

        quantityShipmentPallet = addSGProp(baseGroup, "quantityShipmentPallet", "Всего оприход. (паллета)", quantityShipmentStock, 1, palletFreightBox, 2);

        quantityShipmentFreight = addSGProp(baseGroup, "quantityShipmentFreight", "Всего оприход. (фрахт)", quantityShipmentPallet, 1, freightPallet, 2);

        quantityShipmentArticle = addSGProp(baseGroup, "quantityShipmentArticle", "Всего оприход. (артикул)", quantityShipmentSku, 1, articleSku, 2);

        oneShipmentArticle = addJProp(baseGroup, "oneShipmentArticle", "Первый артикул", equals2, quantityShipmentArticle, 1, 2, addCProp(DoubleClass.instance, 1));
        oneShipmentArticleSku = addJProp(baseGroup, "oneShipmentArticleSku", "Первый артикул", oneShipmentArticle, 1, articleSku, 2);
        oneShipmentSku = addJProp(baseGroup, "oneShipmentSku", "Первый SKU", equals2, quantityShipmentSku, 1, 2, addCProp(DoubleClass.instance, 1));

        oneArticleSkuShipmentDetail = addJProp(baseGroup, "oneArticleSkuShipmentDetail", "Первый артикул", oneShipmentArticleSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);
        oneSkuShipmentDetail = addJProp(baseGroup, "oneSkuShipmentDetail", "Первый SKU", oneShipmentSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);

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
        invoicedShipmentRouteSku = addPGProp(baseGroup, "invoicedShipmentRouteSku", false, 0, true, "Кол-во ожид.",
                percentShipmentRouteSku,
                invoicedShipmentSku, 1, 3);

//        notFilledShipmentRouteSku = addJProp(baseGroup, "notFilledShipmentRouteSku", "Не заполнен", greater2, invoicedShipmentRouteSku, 1, 2, 3,
//                addSUProp(Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipment, route, sku), quantityShipmentRouteSku), 1, 2, 3);
//
//        routeToFillShipmentSku = addMGProp(idGroup, "routeToFillShipmentSku", "Маршрут (ИД)",
//                addJProp(and1, object(route), 2, notFilledShipmentRouteSku, 1, 2, 3), 1, 3);
//
//        LP routeToFillShipmentBarcode = addJProp(routeToFillShipmentSku, 1, barcodeToObject, 2);
//        seekRouteToFillShipmentBarcode = addJProp(actionGroup, true, "seekRouteToFillShipmentSku", "Поиск маршрута", addSAProp(null),
//                routeToFillShipmentBarcode, 1, 2);

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
        sumFreightFreight = addDProp(baseGroup, "sumFreightFreight", "Стоимость", DoubleClass.instance, freight);

        routeFreight = addDProp(idGroup, "routeFreight", "Маршрут (ИД)", route, freight);
        nameRouteFreight = addJProp(baseGroup, "nameRouteFreight", "Маршрут", name, routeFreight, 1);

        exporterFreight = addDProp(idGroup, "exporterFreight", "Экспортер (ИД)", exporter, freight);
        nameExporterFreight = addJProp(baseGroup, "nameExporterFreight", "Экспортер", name, exporterFreight, 1);
        addressExporterFreight = addJProp(baseGroup, "addressExporterFreight", "Адрес", addressSubject, exporterFreight, 1);

        quantityPalletShipmentBetweenDate = addSGProp(baseGroup, "quantityPalletShipmentBetweenDate", "Кол-во паллет по поставкам за интервал",
                addJProp(and1, quantityPalletShipment, 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);
        quantityPalletFreightBetweenDate = addSGProp(baseGroup, "quantityPalletFreightBetweenDate", "Кол-во паллет по фрахтам за интервал",
                addJProp(and1, palletCountFreight, 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);

        freightBoxNumberPallet = addSGProp(baseGroup, "freightBoxNumberPallet", "Кол-во коробов", addCProp(IntegerClass.instance, 1, freightBox), palletFreightBox, 1);

        addConstraint(addJProp("Маршрут паллеты должен совпадать с маршрутом фрахта", diff2,
                routeCreationPalletPallet, 1, addJProp(routeFreight, freightPallet, 1), 1), true);

        palletNumberFreight = addSGProp(baseGroup, "palletNumberFreight", "Кол-во паллет", addCProp(IntegerClass.instance, 1, pallet), freightPallet, 1);

        // freight для supplierBox

        freightSupplierBox = addJProp(baseGroup, "freightSupplierBox", "Фрахт (ИД)", freightDirectInvoice, boxInvoiceSupplierBox, 1);
        freightFreightUnit = addCUProp(idGroup, "freightFreightUnit", "Фрахт (ИД)", freightFreightBox, freightSupplierBox);

        // Кол-во импортеров

        // здесь не соблюдается policy, что входы совпадают с именем
        quantityInvoiceFreightUnitSku = addCUProp(baseGroup, "quantityInvoiceFreightUnitSku", "Кол-во",
                quantityInvoiceStockSku,
                addJProp(and1, quantityListSku, 2, 3, addJProp(equals2, 1, boxInvoiceSupplierBox, 2), 1, 2));

        priceInInvoiceFreightUnitSku = addCUProp(baseGroup, "priceInInvoiceFreightUnitSku", true, "Цена входная",
                priceInInvoiceStockSku,
                addJProp(and1, priceDocumentSku, 1, 3, addJProp(equals2, 1, boxInvoiceSupplierBox, 2), 1, 2));

        quantityImporterFreightUnitSku = addSGProp(baseGroup, "quantityImporterFreightUnitSku", "Кол-во", quantityInvoiceFreightUnitSku,
                importerInvoice, 1, 2, 3);

        quantityImporterFreightUnitArticle = addSGProp(baseGroup, "quantityImporterFreightUnitArticle", "Кол-во", quantityImporterFreightUnitSku, 1, 2, articleSku, 3);

        quantityImporterFreightSku = addSGProp(baseGroup, "quantityImporterFreightSku", true, true, "Кол-во", quantityImporterFreightUnitSku, 1, freightFreightUnit, 2, 3);

        quantityFreightArticle = addSGProp(baseGroup, "quantityFreightArticle", "Кол-во", quantityImporterFreightSku, 2, articleSku, 3);
        quantityFreightSku = addSGProp(baseGroup, "quantityFreightSku", true, true, "Кол-во", quantityImporterFreightSku, 2, 3);

        customCategory10FreightSku = addDProp(idGroup, "customCategory10FreightSku", "ТН ВЭД (ИД)", customCategory10, freight, sku);
        customCategory10FreightSku.setDerivedForcedChange(addJProp(and1, customCategory10Sku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        sidCustomCategory10FreightSku = addJProp(baseGroup, "sidCustomCategory10FreightSku", "ТН ВЭД", sidCustomCategory10, customCategory10FreightSku, 1, 2);

        mainCompositionOriginFreightSku = addDProp(baseGroup, "mainCompositionOriginFreightSku", "Состав", COMPOSITION_CLASS, freight, sku);
        mainCompositionOriginFreightSku.setDerivedForcedChange(addJProp(and1, mainCompositionOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        additionalCompositionOriginFreightSku = addDProp(baseGroup, "additionalCompositionOriginFreightSku", "Доп. состав", COMPOSITION_CLASS, freight, sku);
        additionalCompositionOriginFreightSku.setDerivedForcedChange(addJProp(and1, additionalCompositionOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        translationMainCompositionSku = addJProp(actionGroup, true, "translationMainCompositionSku", "Перевод состава", addTAProp(mainCompositionOriginSku, mainCompositionSku), dictionaryComposition, 1);
        translationAdditionalCompositionSku = addJProp(actionGroup, true, "translationAdditionalCompositionSku", "Перевод доп. состава", addTAProp(additionalCompositionOriginSku, additionalCompositionSku), dictionaryComposition, 1);

        mainCompositionFreightSku = addDProp(baseGroup, "mainCompositionFreightSku", "Состав (перевод)", COMPOSITION_CLASS, freight, sku);
        mainCompositionFreightSku.setDerivedForcedChange(addJProp(and1, mainCompositionSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        additionalCompositionFreightSku = addDProp(baseGroup, "additionalCompositionFreightSku", "Доп. состав (перевод)", COMPOSITION_CLASS, freight, sku);
        additionalCompositionFreightSku.setDerivedForcedChange(addJProp(and1, additionalCompositionSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        countryOfOriginFreightSku = addDProp(idGroup, "countryOfOriginFreightSku", "Страна (ИД)", country, freight, sku);
        countryOfOriginFreightSku.setDerivedForcedChange(addJProp(and1, countryOfOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        sidCountryOfOriginFreightSku = addJProp(baseGroup, "sidCountryOfOriginFreightSku", "Код страны", sidCountry, countryOfOriginFreightSku, 1, 2);
        nameCountryOfOriginFreightSku = addJProp(baseGroup, "nameCountryOfOriginFreightSku", "Страна", name, countryOfOriginFreightSku, 1, 2);
        
        quantityImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "quantityImporterFreightArticleCompositionCountryCategory", "Кол-во",
                quantityImporterFreightSku, 1, 2, articleSku, 3, mainCompositionFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        netWeightStockSku = addJProp(baseGroup, "netWeightStockSku", "Вес нетто", multiplyDouble2, quantityStockSku, 1, 2, netWeightSku, 2);
        netWeightStock = addSGProp(baseGroup, "netWeightStock", "Вес нетто", netWeightStockSku, 1);

        netWeightFreightSku = addDProp(baseGroup, "netWeightFreightSku", "Вес нетто", DoubleClass.instance, freight, sku);
        netWeightFreightSku.setDerivedForcedChange(addJProp(and1, netWeightSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        netWeightImporterFreightUnitSku = addJProp(baseGroup, "netWeightImporterFreightUnitSku", "Вес нетто", multiplyDouble2, quantityImporterFreightUnitSku, 1, 2, 3, addJProp(netWeightFreightSku, freightFreightUnit, 1, 2), 2, 3);
        netWeightImporterFreightUnit = addSGProp(baseGroup, "netWeightImporterFreightUnit", "Вес нетто", netWeightImporterFreightUnitSku, 1, 2);

        netWeightImporterFreightSku = addJProp(baseGroup, "netWeightImporterFreightSku", "Вес нетто", multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);
        netWeightImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "netWeightImporterFreightArticleCompositionCountryCategory", "Вес нетто",
                netWeightImporterFreightSku, 1, 2, articleSku, 3, mainCompositionFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        netWeightImporterFreight = addSGProp(baseGroup, "netWeightImporterFreight", "Вес нетто", netWeightImporterFreightSku, 1, 2);
        //netWeightImporterFreightBox = addSGProp(baseGroup, "netWeightImporterFreight", "Вес нетто", netWeightImporterFreightSku, 1, 2);
        
        priceImporterFreightSku = addDProp(baseGroup, "priceImporterFreightSku", "Цена входная", DoubleClass.instance, importer, freight, sku);
        priceMaxImporterFreightSku = addMGProp(baseGroup, "priceMaxImporterFreightSku", true, "Цена входная", priceInInvoiceFreightUnitSku, importerInvoice, 1, freightFreightUnit, 2, 3);
        priceInImporterFreightSku = addSUProp(baseGroup, "priceInImporterFreightSku", "Цена входная", Union.OVERRIDE, priceMaxImporterFreightSku, priceImporterFreightSku);

        sumInImporterFreightSku = addJProp(baseGroup, "sumInImporterFreightSku", "Сумма входная", multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceInImporterFreightSku, 1, 2, 3);

        sumFreightImporterFreightSku = addPGProp(baseGroup, "sumFreightImporterFreightSku", false, 2, false, "Сумма фрахта",
                netWeightImporterFreightSku,
                sumFreightFreight, 2);

        LP priceAggrFreightImporterFreightSku = addJProp(divideDouble2, sumFreightImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);

//        priceFreightImporterFreightSku = addJProp(baseGroup, "priceFreightImporterFreightSku", "Цена за фрахт", divideDouble2, sumFreightImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        priceFreightImporterFreightSku = addDProp(baseGroup, "priceFreightImporterFreightSku", "Цена за фрахт", DoubleClass.instance, importer, freight, sku);
        priceFreightImporterFreightSku.setDerivedForcedChange(priceAggrFreightImporterFreightSku, 1, 2, 3, is(freightPriced), 2, sumFreightFreight, 2);

        priceExpenseImporterFreightSku = addJProp(baseGroup, "priceExpenseImporterFreightSku", "Цена затр.", sumDouble2, priceInImporterFreightSku, 1, 2, 3, priceFreightImporterFreightSku, 1, 2, 3);

        markupPercentImporterFreightBrandSupplier = addDProp(baseGroup, "markupPercentImporterFreightBrandSupplier", "Надбавка (%)", DoubleClass.instance, importer, freight, brandSupplier);
        markupPercentImporterFreightSku = addJProp(baseGroup, "markupPercentImporterFreightSku", true, "Надбавка (%)", markupPercentImporterFreightBrandSupplier, 1, 2, brandSupplierArticleSku, 3);

        // надбавка на цену без учёта стоимости фрахта
        markupInImporterFreightSku = addJProp(baseGroup, "markupInImporterFreightSku", "Надбавка", percent2, priceInImporterFreightSku, 1, 2, 3, markupPercentImporterFreightSku, 1, 2, 3);

        priceMarkupInImporterFreightSku = addJProp(baseGroup, "priceMarkupInImporterFreightSku", "Цена выходная", sumDouble2, priceInImporterFreightSku, 1, 2, 3, markupInImporterFreightSku, 1, 2, 3);

        priceInOutImporterFreightSku = addDProp(baseGroup, "priceInOutImporterFreightSku", "Цена выходная", DoubleClass.instance, importer, freight, sku);
        priceInOutImporterFreightSku.setDerivedForcedChange(addJProp(and1, priceMarkupInImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3), 1, 2, 3, is(freightPriced), 2);

        priceImporterFreightArticleCompositionCountryCategory = addMGProp(baseGroup, "priceImporterFreightArticleCompositionCountryCategory", "Цена",
                priceInOutImporterFreightSku, 1, 2, articleSku, 3, mainCompositionFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        sumImporterFreightUnitSku = addJProp(baseGroup, "sumImporterFreightUnitSku", "Сумма", multiplyDouble2, quantityImporterFreightUnitSku, 1, 2, 3, addJProp(priceInOutImporterFreightSku, 1, freightFreightUnit, 2, 3), 1, 2, 3);

        sumImporterFreightArticleCompositionCountryCategory = addJProp(baseGroup, "sumImporterFreightArticleCompositionCountryCategory", "Сумма", multiplyDouble2,
                quantityImporterFreightArticleCompositionCountryCategory, 1, 2, 3, 4, 5, 6,
                priceImporterFreightArticleCompositionCountryCategory, 1, 2, 3, 4, 5, 6);

        sumImporterFreight = addSGProp(baseGroup, "sumImporterFreight", "Сумма выходная", sumImporterFreightArticleCompositionCountryCategory, 1, 2);

        sumMarkupInImporterFreightSku = addJProp(baseGroup, "sumMarkupInImporterFreightSku", "Сумма надбавки", multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, markupInImporterFreightSku, 1, 2, 3);
        sumInOutImporterFreightSku = addJProp(baseGroup, "sumInOutImporterFreightSku", "Сумма выходная", multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);

        sumMarkupInImporterFreight = addSGProp(baseGroup, "sumMarkupInImporterFreight", "Сумма надбавки", sumMarkupInImporterFreightSku, 1, 2);
        sumInOutImporterFreight = addSGProp(baseGroup, "sumInOutImporterFreight", "Сумма выходная", sumInOutImporterFreightSku, 1, 2);

        sumMarkupInFreight = addSGProp(baseGroup, "sumMarkupInFreight", "Сумма надбавки", sumMarkupInImporterFreight, 2);
        sumInOutFreight = addSGProp(baseGroup, "sumInOutFreight", "Сумма выходная", sumInOutImporterFreight, 2);

        // надбавка на цену с учётом стоимости фрахта
        markupImporterFreightSku = addJProp(baseGroup, "markupImporterFreightSku", "Надбавка", percent2, priceExpenseImporterFreightSku, 1, 2, 3, markupPercentImporterFreightSku, 1, 2, 3);
        sumMarkupImporterFreightSku = addJProp(baseGroup, "sumMarkupImporterFreightSku", "Сумма надбавки", multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, markupImporterFreightSku, 1, 2, 3);

        priceOutImporterFreightSku = addJProp(baseGroup, "priceOutImporterFreightSku", "Цена выходная", sumDouble2, priceExpenseImporterFreightSku, 1, 2, 3, markupImporterFreightSku, 1, 2, 3);
        sumOutImporterFreightSku = addJProp(baseGroup, "sumOutImporterFreightSku", "Сумма выходная", multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceOutImporterFreightSku, 1, 2, 3);

        sumInImporterFreight = addSGProp(baseGroup, "sumInImporterFreight", "Сумма входная", sumInImporterFreightSku, 1, 2);
        sumMarkupImporterFreight = addSGProp(baseGroup, "sumMarkupImporterFreight", "Сумма надбавки", sumMarkupImporterFreightSku, 1, 2);

        sumOutImporterFreight = addSGProp(baseGroup, "sumOutImporterFreight", "Сумма выходная", sumOutImporterFreightSku, 1, 2);

        sumInFreight = addSGProp(baseGroup, "sumInFreight", "Сумма входная", sumInImporterFreight, 2);
        sumMarkupFreight = addSGProp(baseGroup, "sumMarkupFreight", "Сумма надбавки", sumMarkupImporterFreight, 2);
        sumOutFreight = addSGProp(baseGroup, "sumOutFreight", "Сумма выходная", sumOutImporterFreight, 2);

        // итоги с начала года
        sumInCurrentYear = addSGProp(baseGroup, "sumInCurrentYear", "Итого вход", addJProp(and1, sumInFreight, 1, addJProp(equals2, addJProp(yearInDate, currentDate), addJProp(yearInDate, date, 1), 1), 1));
        sumInOutCurrentYear = addSGProp(baseGroup, "sumInOutCurrentYear", "Итого выход", addJProp(and1, sumInOutFreight, 1, addJProp(equals2, addJProp(yearInDate, currentDate), addJProp(yearInDate, date, 1), 1), 1));
        balanceSumCurrentYear = addDUProp(baseGroup, "balanceSumCurrentYear", "Сальдо", sumInOutCurrentYear, sumInCurrentYear);

        quantityImporterFreightBrandSupplier = addSGProp(baseGroup, "quantityImporterFreightBrandSupplier", "Кол-во позиций", quantityImporterFreightSku, 1, 2, brandSupplierArticleSku, 3);

        quantityImporterFreight = addSGProp(baseGroup, "quantityImporterFreight", "Кол-во позиций", quantityImporterFreightSku, 1, 2);

        // Текущие палеты/коробки для приема
        currentPalletRoute = addDProp("currentPalletRoute", "Тек. паллета (ИД)", pallet, route);
        barcodeCurrentPalletRoute = addJProp("barcodeCurrentPalletRoute", "Тек. паллета (штрих-код)", barcode, currentPalletRoute, 1);

        sumNetWeightFreightSku = addJProp(baseGroup, "sumNetWeightFreightSku", "Вес нетто (всего)", multiplyDouble2, quantityFreightSku, 1, 2, netWeightSku, 2);

        grossWeightCurrentPalletRoute = addJProp(true, "grossWeightCurrentPalletRoute", "Вес брутто", grossWeightPallet, currentPalletRoute, 1);
        grossWeightFreight = addSUProp(baseGroup, "freightGrossWeight", "Вес брутто (фрахт)", Union.SUM,
                                       addSGProp(grossWeightPallet, freightPallet, 1),
                                       addSGProp(grossWeightDirectInvoice, freightDirectInvoice, 1));

        sumGrossWeightFreightSku = addPGProp(baseGroup, "sumGrossWeightFreightSku", false, 1, false, "Вес брутто",
            sumNetWeightFreightSku,
            grossWeightFreight, 1);
        
        grossWeightFreightSkuAggr = addJProp(baseGroup, "grossWeightFreightSkuAggr", "Вес брутто", divideDouble2, sumGrossWeightFreightSku, 1, 2, quantityFreightSku, 1, 2);
        grossWeightFreightSku = addDProp(baseGroup, "grossWeightFreightSku", "Вес брутто", DoubleClass.instance, freight, sku);
        grossWeightFreightSku.setDerivedForcedChange(addJProp(and1, grossWeightFreightSkuAggr, 1, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        grossWeightImporterFreightSku = addJProp(baseGroup, "grossWeightImporterFreightSku", "Вес брутто", multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, grossWeightFreightSku, 2, 3);
        grossWeightImporterFreight = addSGProp(baseGroup, "grossWeightImporterFreight", "Вес брутто", grossWeightImporterFreightSku, 1, 2);
        grossWeightImporterFreightUnitSku = addJProp(baseGroup, "grossWeightImporterFreightUnitSku", "Вес брутто", multiplyDouble2, quantityImporterFreightUnitSku, 1, 2, 3, addJProp(grossWeightFreightSku, freightFreightUnit, 2, 3), 1, 2, 3);
        grossWeightImporterFreightUnit = addSGProp(baseGroup, "grossWeightImporterFreightUnit", "Вес брутто", grossWeightImporterFreightUnitSku, 1, 2);

        grossWeightImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "grossWeightImporterFreightArticleCompositionCountryCategory", "Вес брутто",
                grossWeightImporterFreightSku, 1, 2, articleSku, 3, mainCompositionFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);
        
        currentFreightBoxRoute = addDProp("currentFreightBoxRoute", "Тек. короб (ИД)", freightBox, route);
        barcodeCurrentFreightBoxRoute = addJProp("barcodeCurrentFreightBoxRoute", "Тек. короб (штрих-код)", barcode, currentFreightBoxRoute, 1);

        destinationCurrentFreightBoxRoute = addJProp(true, "destinationCurrentFreightBoxRoute", "Пункт назначения тек. короба (ИД)", destinationFreightBox, currentFreightBoxRoute, 1);
        nameDestinationCurrentFreightBoxRoute = addJProp("nameDestinationCurrentFreightBoxRoute", "Пункт назначения тек. короба", name, destinationCurrentFreightBoxRoute, 1);

        isCurrentFreightBox = addJProp(equals2, addJProp(true, currentFreightBoxRoute, routeCreationFreightBoxFreightBox, 1), 1, 1);
        isCurrentPallet = addJProp(equals2, addJProp(true, currentPalletRoute, routeCreationPalletPallet, 1), 1, 1);
        currentPalletFreightBox = addJProp(currentPalletRoute, routeCreationFreightBoxFreightBox, 1);
        isCurrentPalletFreightBox = addJProp(equals2, palletFreightBox, 1, currentPalletFreightBox, 1);
        isStoreFreightBoxSupplierBox = addJProp(equals2, destinationFreightBox, 1, destinationSupplierBox, 2);

        barcodeActionSeekPallet = addJProp(true, "Найти палету", isCurrentPallet, barcodeToObject, 1);
        barcodeActionCheckPallet = addJProp(true, "Проверка паллеты",
                                            addJProp(true, and(false, true),
                                                           addStopActionProp("Для маршрута выбранного короба не задана паллета", "Поиск по штрих-коду"),
                                                           is(freightBox), 1,
                                                           currentPalletFreightBox, 1), barcodeToObject, 1);
        barcodeActionSeekFreightBox = addJProp(true, "Найти короб для транспортировки", isCurrentFreightBox, barcodeToObject, 1);
        barcodeActionSetPallet = addJProp(true, "Установить паллету", isCurrentPalletFreightBox, barcodeToObject, 1);
        barcodeActionSetStore = addJProp(true, "Установить магазин", isStoreFreightBoxSupplierBox, barcodeToObject, 1, 2);

        addBoxShipmentDetailBoxShipmentSupplierBoxRouteBarcode = addJProp(true, "Добавить строку поставки",
                addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode, 1, 2, currentFreightBoxRoute, 3, 4);

        addSimpleShipmentDetailSimpleShipmentRouteBarcode = addJProp(true, "Добавить строку поставки",
                addSimpleShipmentSimpleShipmentDetailStockBarcode, 1, currentFreightBoxRoute, 2, 3);

        quantityRouteSku = addJProp(baseGroup, "quantityRouteSku", "Оприход. (МХ)", quantityStockSku, currentFreightBoxRoute, 1, 2);

        quantitySupplierBoxBoxShipmentRouteSku = addJProp(baseGroup, true,  "quantitySupplierBoxBoxShipmentRouteSku", "Кол-во оприход.",
                                                    quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4);
        quantitySimpleShipmentRouteSku = addJProp(baseGroup, true,  "quantitySimpleShipmentRouteSku", "Кол-во оприход.",
                                                    quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3);

        createFreightBox = addJProp(true, "Сгенерировать короба", addAAProp(freightBox, barcode, barcodePrefix, true), quantityCreationFreightBox, 1);
        createPallet = addJProp(true, "Сгенерировать паллеты", addAAProp(pallet, barcode, barcodePrefix, true), quantityCreationPallet, 1);

        barcodeActionCheckFreightBox = addJProp(true, "Проверка паллеты",
                                            addJProp(true, and(false, false, true),
                                                           addStopActionProp("Для выбранного маршрута не задан короб для транспортировки", "Поиск по штрих-коду"),
                                                           is(sku), 2,
                                                           is(route), 1,
                                                           currentFreightBoxRoute, 1), 1, barcodeToObject, 2);
        barcodeAction4 = addJProp(true, "Ввод штрих-кода 4",
                addCUProp(
                        addSCProp(addJProp(true, quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4))
                ), 1, 2, 3, barcodeToObject, 4);
        barcodeAction3 = addJProp(true, "Ввод штрих-кода 3",
                addCUProp(
                        addSCProp(addJProp(true, quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3))
                ), 1, 2, barcodeToObject, 3);
    }

    @Override
    protected void initTables() {
        tableFactory.include("customCategory", customCategory);
        tableFactory.include("customCategory4", customCategory4);
        tableFactory.include("customCategory6", customCategory6);
        tableFactory.include("customCategory9", customCategory9);
        tableFactory.include("customCategory10", customCategory10);
        tableFactory.include("customCategoryOrigin", customCategoryOrigin);
        tableFactory.include("customCategory10Origin", customCategory10, customCategoryOrigin);
        tableFactory.include("sku", sku);
        tableFactory.include("article", article);
        tableFactory.include("documentSku", document, sku);
        tableFactory.include("documentArticle", document, article);
        tableFactory.include("listSku", list, sku);
        tableFactory.include("listArticle", list, article);
    }

    @Override
    protected void initIndexes() {
    }

    public LP addDEAProp() {
        return addProperty(null, new LP<ClassPropertyInterface>(new DeclarationExportActionProperty("declarationExport", "Экспорт декларанта", RomanBusinessLogics.this, importer, freight)));
    }

    public InvoiceFromFormEntity invoiceFromFormEntity;

    @Override
    protected void initNavigators() throws JRException, FileNotFoundException {
        NavigatorElement classifier = new NavigatorElement(baseElement, "classifier", "Справочники");
        addFormEntity(new ColorSizeSupplierFormEntity(classifier, "сolorSizeSupplierForm", "Поставщики"));
        addFormEntity(new CustomCategoryFormEntity(classifier, "customCategoryForm", "ТН ВЭД (изменения)", false));
        addFormEntity(new CustomCategoryFormEntity(classifier, "customCategoryForm2", "ТН ВЭД (дерево)", true));
        classifier.add(category.getClassForm(this));
        classifier.add(currency.getClassForm(this));
        classifier.add(importer.getClassForm(this));
        classifier.add(exporter.getClassForm(this));        
        classifier.add(store.getClassForm(this));
        classifier.add(country.getClassForm(this));
        classifier.add(unitOfMeasure.getClassForm(this));
        classifier.add(freightType.getClassForm(this));

        createItemForm = addFormEntity(new CreateItemFormEntity(null, "createItemForm", "Ввод товара"));

        NavigatorElement printForms = new NavigatorElement(baseElement, "printForms", "Печатные формы");
        invoiceFromFormEntity = new InvoiceFromFormEntity(printForms, "invoiceFromForm", "Исходящие инвойсы");
        addFormEntity(invoiceFromFormEntity);
        addFormEntity(new PackingListFormEntity(printForms, "packingListForm", "Исходящие упаковочные листы"));
        addFormEntity(new PackingListBoxFormEntity(printForms, "packingListBoxForm", "Упаковочные листы коробов"));

        NavigatorElement purchase = new NavigatorElement(baseElement, "purchase", "Управление закупками");
        addFormEntity(new OrderFormEntity(purchase, "orderForm", "Заказы"));
        addFormEntity(new InvoiceFormEntity(purchase, "boxInvoiceForm", "Инвойсы по коробам", true));
        addFormEntity(new InvoiceFormEntity(purchase, "simpleInvoiceForm", "Инвойсы без коробов", false));
        addFormEntity(new ShipmentListFormEntity(purchase, "boxShipmentListForm", "Поставки по коробам", true));
        addFormEntity(new ShipmentListFormEntity(purchase, "simpleShipmentListForm", "Поставки без коробов", false));

        NavigatorElement shipment = new NavigatorElement(baseElement, "shipment", "Управление фрахтами");        
        addFormEntity(new FreightShipmentFormEntity(shipment, "freightShipmentForm", "Комплектация фрахта"));
        addFormEntity(new FreightInvoiceFormEntity(shipment, "freightInvoiceForm", "Расценка фрахта"));
        addFormEntity(new FreightChangeFormEntity(shipment, "freightChangeForm", "Обработка фрахта"));

        NavigatorElement distribution = new NavigatorElement(baseElement, "distribution", "Управление складом");
        FormEntity createPalletForm = addFormEntity(new CreatePalletFormEntity(distribution, "createPalletForm", "Сгенерировать паллеты", true));
        addFormEntity(new CreatePalletFormEntity(createPalletForm, "createPalletFormList", "Документы генерации паллет", false));
        FormEntity createFreightBoxForm = addFormEntity(new CreateFreightBoxFormEntity(distribution, "createFreightBoxForm", "Сгенерировать короба", true));
        addFormEntity(new CreateFreightBoxFormEntity(createFreightBoxForm, "createFreightBoxFormList", "Документы генерации коробов", false));
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

    private class PackingListBoxFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objBox;
        private ObjectEntity objArticle;
        
        private PackingListBoxFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objBox = addSingleGroupObject(1, "box", freightBox, "Короб", barcode, netWeightStock);
            objBox.groupTo.initClassView = ClassViewType.PANEL;

            objArticle = addSingleGroupObject(2, "article", article, "Артикул", sidArticle, nameBrandSupplierArticle, nameOriginCategoryArticle);

            addPropertyDraw(quantityStockArticle, objBox, objArticle);
            //addPropertyDraw(netWeightStockSku, objBox, objSku);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStockArticle, objBox, objArticle)));

            packingListFormFreightBox = addFAProp("Упаковочный лист", this, objBox);
            packingListFormRoute = addJProp(true, "packingListFormRoute", "Упаковочный лист", packingListFormFreightBox, currentFreightBoxRoute, 1);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objBox.groupTo).grid.constraints.fillVertical = 2;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 3;
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

            objOrder = addSingleGroupObject(order, "Заказ", date, sidDocument, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);
            addObjectActions(this, objOrder);

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, objOrder, objSIDArticleComposite));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(seekArticleSIDSupplier, objSIDArticleComposite, objSupplier));

            objSIDArticleSingle = addSingleGroupObject(StringClass.get(50), "Ввод простого артикула", objectValue);
            objSIDArticleSingle.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(addNEArticleSingleSIDSupplier, objSIDArticleSingle, objSupplier));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(incrementNumberListSID, objOrder, objSIDArticleSingle));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(seekArticleSIDSupplier, objSIDArticleSingle, objSupplier));

            objArticle = addSingleGroupObject(article, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, objOrder, objArticle);
            addPropertyDraw(objArticle, sidArticle, nameBrandSupplierArticle, originalNameArticle, nameCountrySupplierOfOriginArticle, nameCountryOfOriginArticle, barcode);
            addPropertyDraw(quantityListArticle, objOrder, objArticle);
            addPropertyDraw(priceDocumentArticle, objOrder, objArticle);
            addPropertyDraw(sumDocumentArticle, objOrder, objArticle);
            addPropertyDraw(invoicedOrderArticle, objOrder, objArticle);
            addPropertyDraw(delete, objArticle);

            objItem = addSingleGroupObject(item, "Товар", barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            addObjectActions(this, objItem);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", selection, sidSizeSupplier);
            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", selection, sidColorSupplier, name);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityDocumentArticleCompositeColorSize, objOrder, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.columnGroupObjects.add(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(sidSizeSupplier, objSizeSupplier);

            addPropertyDraw(quantityListSku, objOrder, objItem);
            addPropertyDraw(priceDocumentSku, objOrder, objItem);
            addPropertyDraw(invoicedOrderSku, objOrder, objItem);
            addPropertyDraw(quantityDocumentArticleCompositeColor, objOrder, objArticle, objColorSupplier);
            addPropertyDraw(quantityDocumentArticleCompositeSize, objOrder, objArticle, objSizeSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, objSupplier));
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

        private boolean box;

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
        private ObjectEntity objSku;

        private InvoiceFormEntity(NavigatorElement parent, String sID, String caption, boolean box) {
            super(parent, sID, caption);

            this.box = box;

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, nameCurrencySupplier, importInvoiceActionGroup, true);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс", date, objectClassName, sidDocument, nameCurrencyDocument, sumDocument, nameImporterInvoice, sidDestinationDestinationDocument, nameDestinationDestinationDocument, grossWeightDirectInvoice);
            addObjectActions(this, objInvoice);

            objOrder = addSingleGroupObject(order, "Заказ");
            objOrder.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inOrderInvoice, objOrder, objInvoice);
            addPropertyDraw(objOrder, date, sidDocument, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб", sidSupplierBox, barcode);
                objSupplierBox.groupTo.initClassView = ClassViewType.PANEL;
                addObjectActions(this, objSupplierBox);
            }

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleComposite));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(seekArticleSIDSupplier, objSIDArticleComposite, objSupplier));

            objSIDArticleSingle = addSingleGroupObject(StringClass.get(50), "Ввод простого артикула", objectValue);
            objSIDArticleSingle.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(addNEArticleSingleSIDSupplier, objSIDArticleSingle, objSupplier));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleSingle));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(seekArticleSIDSupplier, objSIDArticleSingle, objSupplier));

            objArticle = addSingleGroupObject(article, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(objArticle, sidArticle, nameBrandSupplierArticle, originalNameArticle, sidCustomCategoryOriginArticle,
                    nameCountrySupplierOfOriginArticle, netWeightArticle, mainCompositionOriginArticle, additionalCompositionOriginArticle, barcode);
            addPropertyDraw(quantityListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(priceDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(sumDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(orderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(priceOrderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(delete, objArticle);

            objItem = addSingleGroupObject(item, "Товар", barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            addObjectActions(this, objItem, objArticle, articleComposite);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", selection, sidSizeSupplier);
            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", selection, sidColorSupplier, name);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityDocumentArticleCompositeColorSize, objInvoice, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.columnGroupObjects.add(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(sidSizeSupplier, objSizeSupplier);

            addPropertyDraw(quantityListSku, (box ? objSupplierBox : objInvoice), objItem);
            addPropertyDraw(priceDocumentSku, objInvoice, objItem);
            addPropertyDraw(orderedInvoiceSku, objInvoice, objItem);
            addPropertyDraw(quantityDocumentArticleCompositeColor, objInvoice, objArticle, objColorSupplier);
            addPropertyDraw(quantityDocumentArticleCompositeSize, objInvoice, objArticle, objSizeSupplier);

            GroupObjectEntity gobjSpec = new GroupObjectEntity(genID());

            objSku = new ObjectEntity(genID(), sku, "SKU");
            gobjSpec.add(objSku);

            ObjectEntity objSupplierBoxSpec = null;
            if (box) {
                objSupplierBoxSpec = new ObjectEntity(genID(), supplierBox, "Короб поставщика");
                gobjSpec.add(objSupplierBoxSpec);
            }

            addGroup(gobjSpec);

            addPropertyDraw(numberListSku, (box ? objSupplierBoxSpec : objInvoice), objSku);
            if (box)
                addPropertyDraw(sidSupplierBox, objSupplierBoxSpec);
            addPropertyDraw(new LP[] {barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                                      nameBrandSupplierArticleSku, nameCountrySupplierOfOriginArticleSku , nameCountryOfOriginSku, netWeightSku,
                                      mainCompositionOriginSku, additionalCompositionOriginSku}, objSku);
            addPropertyDraw(quantityListSku, (box ? objSupplierBoxSpec : objInvoice), objSku);
            addPropertyDraw(priceDocumentSku, objInvoice, objSku);
            addPropertyDraw(sumDocumentSku, objInvoice, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            CompareFilterEntity orderSupplierFilter = new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier);
            addFixedFilter(orderSupplierFilter);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));
            if (box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(boxInvoiceSupplierBox, objSupplierBox), Compare.EQUALS, objInvoice));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleCompositeItem, objItem), Compare.EQUALS, objArticle));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle)));
            if (box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(boxInvoiceSupplierBox, objSupplierBoxSpec), Compare.EQUALS, objInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(numberListSku, (box ? objSupplierBoxSpec : objInvoice), objSku)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityListSku, (box ? objSupplierBoxSpec : objInvoice), objSku)));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(inOrderInvoice, objOrder, objInvoice), Compare.EQUALS, true));
            addPropertyDraw(
                    addSelectFromListAction(null, "Выбрать заказы", objOrder, new FilterEntity[]{orderSupplierFilter}, inOrderInvoice, true, order, invoice),
                    objOrder.groupTo,
                    objInvoice
            ).forceViewType = ClassViewType.PANEL;

            RegularFilterGroupEntity filterGroupSize = new RegularFilterGroupEntity(genID());
            filterGroupSize.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityDocumentArticleCompositeSize, objInvoice, objArticle, objSizeSupplier)),
                    "В инвойсе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupSize);

            RegularFilterGroupEntity filterGroupColor = new RegularFilterGroupEntity(genID());
            filterGroupColor.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityDocumentArticleCompositeColor, objInvoice, objArticle, objColorSupplier)),
                    "В инвойсе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroupColor);

            setReadOnly(objSupplier, true);
            setReadOnly(importInvoiceActionGroup, false, objSupplier.groupTo);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.defaultOrders.put(design.get(getPropertyDraw(numberListArticle)), true);
            design.defaultOrders.put(design.get(getPropertyDraw(numberListSku)), true);

            design.get(objOrder.groupTo).grid.constraints.fillVertical = 0.7;
            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 0.7;
            design.get(objItem.groupTo).grid.constraints.fillHorizontal = 1.5;

            design.get(getPropertyDraw(objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(objectValue, objSIDArticleSingle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

            ContainerView specContainer = design.createContainer("Ввод спецификации");
            specContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            if (box)
                specContainer.add(design.getGroupObjectContainer(objSupplierBox.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSIDArticleComposite.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSIDArticleSingle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSizeSupplier.groupTo));
            specContainer.add(design.getGroupObjectContainer(objItem.groupTo));
            specContainer.add(design.getGroupObjectContainer(objColorSupplier.groupTo));

            ContainerView detContainer = design.createContainer();
            design.getMainContainer().addAfter(detContainer, design.getGroupObjectContainer(objInvoice.groupTo));
            detContainer.add(design.getGroupObjectContainer(objOrder.groupTo));
            detContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            detContainer.add(specContainer);

            detContainer.tabbedPane = true;

            design.addIntersection(design.getGroupObjectContainer(objSIDArticleComposite.groupTo),
                                   design.getGroupObjectContainer(objSIDArticleSingle.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objItem.groupTo),
                                   design.getGroupObjectContainer(objSizeSupplier.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSizeSupplier.groupTo),
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

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", date, sidDocument, netWeightShipment, grossWeightShipment, quantityPalletShipment, quantityBoxShipment);
            addObjectActions(this, objShipment);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс");
            objInvoice.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inInvoiceShipment, objInvoice, objShipment);
            addPropertyDraw(objInvoice, date, sidDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

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

            objRoute = addSingleGroupObject(route, "Маршрут", name, barcodeCurrentPalletRoute, grossWeightCurrentPalletRoute, barcodeCurrentFreightBoxRoute, nameDestinationCurrentFreightBoxRoute);
            objRoute.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(packingListFormRoute, objRoute);

            objRoute.groupTo.tableRowsCount = 0;

            nameRoute = addPropertyDraw(name, objRoute);
            nameRoute.forceViewType = ClassViewType.PANEL;

            objSku = addSingleGroupObject(sku, "SKU", barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameBrandSupplierArticleSku, originalNameArticleSku, nameOriginCategoryArticleSku, nameOriginUnitOfMeasureArticleSku,
                    netWeightArticleSku, nameCountryOfOriginArticleSku, mainCompositionOriginArticleSku,
                    netWeightSku, nameCountryOfOriginSku, mainCompositionOriginSku, additionalCompositionOriginSku);
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

            objShipmentDetail = addSingleGroupObject((box ? boxShipmentDetail : simpleShipmentDetail),
                    selection, barcodeSkuShipmentDetail, sidArticleShipmentDetail, sidColorSupplierItemShipmentDetail, nameColorSupplierItemShipmentDetail, sidSizeSupplierItemShipmentDetail,
                    nameBrandSupplierArticleSkuShipmentDetail, originalNameArticleSkuShipmentDetail,
                    nameOriginCategoryArticleSkuShipmentDetail, nameOriginUnitOfMeasureArticleSkuShipmentDetail,
                    netWeightArticleSkuShipmentDetail,
                    nameCountryOfOriginArticleSkuShipmentDetail, mainCompositionOriginArticleSkuShipmentDetail,
                    netWeightSkuShipmentDetail, nameCountryOfOriginSkuShipmentDetail,
                    mainCompositionOriginSkuShipmentDetail, additionalCompositionOriginSkuShipmentDetail,
                    sidShipmentShipmentDetail,
                    sidSupplierBoxShipmentDetail, barcodeSupplierBoxShipmentDetail,
                    barcodeStockShipmentDetail, nameRouteFreightBoxShipmentDetail,
                    quantityShipmentDetail, nameUserShipmentDetail, timeShipmentDetail, delete);

            objShipmentDetail.groupTo.setSingleClassView(ClassViewType.GRID);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objShipmentDetail.groupTo);
            setForceViewType(supplierAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);
            setForceViewType(intraAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);

            PropertyObjectEntity oneArticleProperty = addPropertyObject(oneArticleSkuShipmentDetail, objShipmentDetail);
            PropertyObjectEntity oneSkuProperty = addPropertyObject(oneSkuShipmentDetail, objShipmentDetail);

            getPropertyDraw(nameOriginCategoryArticleSkuShipmentDetail).setPropertyHighlight(oneArticleProperty);
            getPropertyDraw(nameOriginUnitOfMeasureArticleSkuShipmentDetail).setPropertyHighlight(oneArticleProperty);
            getPropertyDraw(netWeightSkuShipmentDetail).setPropertyHighlight(oneSkuProperty);
            getPropertyDraw(nameCountryOfOriginSkuShipmentDetail).setPropertyHighlight(oneSkuProperty);
            getPropertyDraw(mainCompositionOriginSkuShipmentDetail).setPropertyHighlight(oneSkuProperty);
            getPropertyDraw(additionalCompositionOriginSkuShipmentDetail).setPropertyHighlight(oneSkuProperty);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(shipmentShipmentDetail, objShipmentDetail), Compare.EQUALS, objShipment));

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

            RegularFilterGroupEntity filterGroup2 = new RegularFilterGroupEntity(genID());
            filterGroup2.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(shipmentShipmentDetail, objShipmentDetail), Compare.EQUALS, objShipment),
                    "В текущей поставке",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroup2);

            if (box) {
                RegularFilterGroupEntity filterGroup3 = new RegularFilterGroupEntity(genID());
                filterGroup3.addFilter(new RegularFilterEntity(genID(),
                        new CompareFilterEntity(addPropertyObject(supplierBoxShipmentDetail, objShipmentDetail), Compare.EQUALS, objSupplierBox),
                        "В текущем коробе поставщика",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
                addRegularFilterGroup(filterGroup3);
            }

            RegularFilterGroupEntity filterGroup4 = new RegularFilterGroupEntity(genID());
            filterGroup4.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(userShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(currentUser)),
                    "Текущего пользователя",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroup4);

            RegularFilterGroupEntity filterGroup5 = new RegularFilterGroupEntity(genID());
            filterGroup5.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(stockShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(currentFreightBoxRoute, objRoute)),
                    "В текущем коробе для транспортировки",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroup5);

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSeekPallet, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionCheckPallet, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSeekFreightBox, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetPallet, objBarcode));
            if (box)
                addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetStore, objBarcode, objSupplierBox));

//            addActionsOnObjectChange(objBarcode, addPropertyObject(seekRouteToFillShipmentBarcode, objShipment, objBarcode));
//            if (box)
//                addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction4, objSupplierBox, objShipment, objRoute, objBarcode));
//            else
//                addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction3, objShipment, objRoute, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(seekBarcodeAction, objBarcode));

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionCheckFreightBox, objRoute, objBarcode));

            addActionsOnObjectChange(objBarcode,
                                     addPropertyObject(
                                             addJProp(true, andNot1,
                                                      addModalFormActionProp(
                                                              null,
                                                              "Ввод нового товара",
                                                              createItemForm,
                                                              new ObjectEntity[]{createItemForm.objSupplier, createItemForm.objBarcode},
                                                              createItemForm.addPropertyObject(addItemBarcode, createItemForm.objBarcode)
                                                      ), 1, 2,
                                                      barcodeToObject, 2
                                             ),
                                             objSupplier, objBarcode));

            addActionsOnObjectChange(objBarcode, addPropertyObject(
                    addJProp(true, addAProp(new SeekRouteActionProperty()),
                             1, barcodeToObject, 2, 3),
                    objShipment, objBarcode, objRoute));

            if (box) {
                addActionsOnObjectChange(objBarcode, addPropertyObject(addBoxShipmentDetailBoxShipmentSupplierBoxRouteBarcode, objShipment, objSupplierBox, objRoute, objBarcode));
            } else {
                addActionsOnObjectChange(objBarcode, addPropertyObject(addSimpleShipmentDetailSimpleShipmentRouteBarcode, objShipment, objRoute, objBarcode));
            }

//            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeNotFoundMessage, objBarcode));
            if (box)
                addActionsOnObjectChange(objSIDSupplierBox, addPropertyObject(seekSupplierBoxSIDSupplier, objSIDSupplierBox, objSupplier));

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

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objShipmentDetail.groupTo));
            specContainer.add(design.getGroupObjectContainer(objShipmentDetail.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            specContainer.tabbedPane = true;

            design.get(nameRoute).minimumCharWidth = 4;
            design.get(nameRoute).panelLabelAbove = true;
            design.get(nameRoute).design.font = new Font("Tahoma", Font.BOLD, 48);
            design.getGroupObjectContainer(objRoute.groupTo).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;

            design.setHighlightColor(new Color(255, 128, 128));

            return design;
        }
    }

    private class FreightShipmentFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objShipment;
        private ObjectEntity objFreight;
        private ObjectEntity objPallet;
        private ObjectEntity objDateFrom;
        private ObjectEntity objDateTo;
        private ObjectEntity objDirectInvoice;

        private FreightShipmentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", date, objectClassName, nameRouteFreight, nameFreightTypeFreight, tonnageFreight, palletCountFreight, volumeFreight, palletNumberFreight);
            addObjectActions(this, objFreight);

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

            objShipment = addSingleGroupObject(shipment, "Поставка", date, nameSupplierDocument, sidDocument, sumDocument, nameCurrencyDocument, netWeightShipment, grossWeightShipment, quantityPalletShipment, quantityBoxShipment);

            objPallet = addSingleGroupObject(pallet, "Паллета", barcode, grossWeightPallet, freightBoxNumberPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(equalsPalletFreight, objPallet, objFreight);

            objDirectInvoice = addSingleGroupObject(directInvoice, "Инвойс", date, sidDocument, nameCurrencyDocument, sumDocument, nameImporterInvoice, sidDestinationDestinationDocument, nameDestinationDestinationDocument);
            addPropertyDraw(equalsDirectInvoiceFreight, objDirectInvoice, objFreight);

            addPropertyDraw(quantityShipmentFreight, objShipment, objFreight);

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

            RegularFilterGroupEntity filterGroupInvoice = new RegularFilterGroupEntity(genID());
            filterGroupInvoice.addFilter(new RegularFilterEntity(genID(),
                                  new OrFilterEntity(new CompareFilterEntity(addPropertyObject(freightDirectInvoice, objDirectInvoice), Compare.EQUALS, objFreight),
                                                     new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(freightDirectInvoice, objDirectInvoice)))),
                                  "Не расписанные инвойсы или в текущем фрахте",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroupInvoice.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(freightDirectInvoice, objDirectInvoice), Compare.EQUALS, objFreight),
                    "В текущем фрахте",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroupInvoice.defaultFilter = 0;
            addRegularFilterGroup(filterGroupInvoice);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

//            design.get(objPallet.groupTo).grid.constraints.fillHorizontal = 0.4;
//
            design.addIntersection(design.getGroupObjectContainer(objPallet.groupTo),
                    design.getGroupObjectContainer(objDirectInvoice.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView shipContainer = design.createContainer("Поставки");
            shipContainer.add(design.getGroupObjectContainer(objDateFrom.groupTo));
            shipContainer.add(design.getGroupObjectContainer(objShipment.groupTo));

            ContainerView contContainer = design.createContainer("Комплектация");
            contContainer.add(design.getGroupObjectContainer(objPallet.groupTo));
            contContainer.add(design.getGroupObjectContainer(objDirectInvoice.groupTo));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objFreight.groupTo));
            specContainer.add(shipContainer);
            specContainer.add(contContainer);
            specContainer.tabbedPane = true;

            return design;
        }
    }

    private class CreateFreightBoxFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objFreightBox;

        private CreateFreightBoxFormEntity(NavigatorElement parent, String sID, String caption, boolean toAdd) {
            super(parent, sID, caption);

            objCreate = addSingleGroupObject(creationFreightBox, "Документ генерации коробов");
            if (!toAdd)
                addPropertyDraw(objCreate, objectValue);
            addPropertyDraw(objCreate, nameRouteCreationFreightBox, quantityCreationFreightBox);
            if (toAdd) {
                addPropertyDraw(createFreightBox, objCreate);
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);
                objCreate.addOnTransaction = true;
            }

            objFreightBox = addSingleGroupObject(freightBox, "Короба для транспортировки", barcode);
            setReadOnly(objFreightBox, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationFreightBoxFreightBox, objFreightBox), Compare.EQUALS, objCreate));
        }
    }

    private class CreatePalletFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objPallet;

        private CreatePalletFormEntity(NavigatorElement parent, String sID, String caption, boolean toAdd) {
            super(parent, sID, caption);

            objCreate = addSingleGroupObject(creationPallet, "Докунмент генерации паллет");
            if (!toAdd)
                addPropertyDraw(objCreate, objectValue);
            addPropertyDraw(objCreate, nameRouteCreationPallet, quantityCreationPallet);
            if (toAdd) {
                addPropertyDraw(createPallet, objCreate);
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);
                objCreate.addOnTransaction = true;
            }

            objPallet = addSingleGroupObject(pallet, "Паллеты для транспортировки", barcode);
            setReadOnly(objPallet, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationPalletPallet, objPallet), Compare.EQUALS, objCreate));
        }
    }

    private class CustomCategoryFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean tree = false;

        private ObjectEntity objCustomCategory4;
        private ObjectEntity objCustomCategory6;
        private ObjectEntity objCustomCategory4Origin;
        private ObjectEntity objCustomCategory6Origin;
        private ObjectEntity objCustomCategory9;
        private ObjectEntity objCustomCategory10;
        private ObjectEntity objCustomCategoryOrigin;

        private PropertyDrawEntity import1;
        private PropertyDrawEntity import2;

        private TreeGroupEntity treeCustomCategory;
        private TreeGroupEntity treeCustomCategoryOrigin;

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
            addObjectActions(this, objCustomCategory10);

            objCustomCategory4Origin = addSingleGroupObject(customCategory4, "Первый уровень", sidCustomCategory4, nameCustomCategory);
            if (!tree)
                addObjectActions(this, objCustomCategory4Origin);

            objCustomCategory6Origin = addSingleGroupObject(customCategory6, "Второй уровень", sidCustomCategory6, nameCustomCategory);
            if (!tree)
                addObjectActions(this, objCustomCategory6Origin);

            import1 = addPropertyDraw(importBelTnved);
            import2 = addPropertyDraw(importEuTnved);

            objCustomCategoryOrigin = addSingleGroupObject(customCategoryOrigin, "ЕС уровень", sidCustomCategoryOrigin, nameCustomCategory, sidCustomCategory10CustomCategoryOrigin);
            addObjectActions(this, objCustomCategoryOrigin);

            addPropertyDraw(relationCustomCategory10CustomCategoryOrigin, objCustomCategory10, objCustomCategoryOrigin);

            if (tree)
            {   treeCustomCategory = addTreeGroupObject(objCustomCategory4.groupTo, objCustomCategory6.groupTo, objCustomCategory9.groupTo);
                treeCustomCategoryOrigin = addTreeGroupObject(objCustomCategory4Origin.groupTo, objCustomCategory6Origin.groupTo);
            }

            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory4CustomCategory6, objCustomCategory6), Compare.EQUALS, objCustomCategory4));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory6CustomCategory9, objCustomCategory9), Compare.EQUALS, objCustomCategory6));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory9CustomCategory10, objCustomCategory10), Compare.EQUALS, objCustomCategory9));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory4CustomCategory6, objCustomCategory6Origin), Compare.EQUALS, objCustomCategory4Origin));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory6CustomCategoryOrigin, objCustomCategoryOrigin), Compare.EQUALS, objCustomCategory6Origin));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            if (tree)
            {
                design.addIntersection(design.getTreeContainer(treeCustomCategory), design.getGroupObjectContainer(objCustomCategory10.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getTreeContainer(treeCustomCategoryOrigin), design.getGroupObjectContainer(objCustomCategoryOrigin.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.get(treeCustomCategoryOrigin).constraints.fillHorizontal = 1;
                design.get(treeCustomCategory).constraints.fillHorizontal = 1;
                design.get(objCustomCategory10.groupTo).grid.constraints.fillHorizontal = 2;
                design.get(objCustomCategoryOrigin.groupTo).grid.constraints.fillHorizontal = 2;
            }
            else {

                design.addIntersection(design.getGroupObjectContainer(objCustomCategory4.groupTo), design.getGroupObjectContainer(objCustomCategory6.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objCustomCategory6.groupTo), design.getGroupObjectContainer(objCustomCategory9.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objCustomCategory9.groupTo), design.getGroupObjectContainer(objCustomCategory10.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

                design.addIntersection(design.getGroupObjectContainer(objCustomCategory4Origin.groupTo), design.getGroupObjectContainer(objCustomCategory6Origin.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objCustomCategory6Origin.groupTo), design.getGroupObjectContainer(objCustomCategoryOrigin.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

                design.get(objCustomCategory4.groupTo).grid.constraints.fillHorizontal = 2;
                design.get(objCustomCategory4Origin.groupTo).grid.constraints.fillHorizontal = 2;
                design.get(objCustomCategoryOrigin.groupTo).grid.constraints.fillHorizontal = 2;
            }
            design.addIntersection(design.get(import1), design.get(import2), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class FreightChangeFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objArticle;
        private ObjectEntity objSku;
        private ObjectEntity objSkuFreight;

        private FreightChangeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", date, objectClassName, nameRouteFreight, nameExporterFreight, nameFreightTypeFreight, grossWeightFreight, tonnageFreight, palletCountFreight, volumeFreight, palletNumberFreight);

            objImporter = addSingleGroupObject(importer, "Импортёр", name, addressSubject);

            addPropertyDraw(invoiceFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(packingListFormImporterFreight, objImporter, objFreight);

            objArticle = addSingleGroupObject(article, "Артикул", selection, sidArticle, nameBrandSupplierArticle, originalNameArticle, nameCategoryArticle, nameArticle,
                    sidCustomCategoryOriginArticle, nameCountryOfOriginArticle, mainCompositionOriginArticle, additionalCompositionOriginArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(quantityFreightArticle, objFreight, objArticle);

            objSku = addSingleGroupObject(sku, "SKU", selection, barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem, nameBrandSupplierArticleSku,
                     sidCustomCategoryOriginArticleSku, sidCustomCategory10Sku, nameCountrySku, netWeightSku,
                     mainCompositionOriginSku, translationMainCompositionSku, mainCompositionSku,
                     additionalCompositionOriginSku, translationAdditionalCompositionSku, additionalCompositionSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            objSkuFreight = addSingleGroupObject(sku, "Позиции фрахта", selection, barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem,
                    sidSizeSupplierItem, nameBrandSupplierArticleSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSkuFreight.groupTo);

            addPropertyDraw(quantityFreightSku, objFreight, objSku);
            addPropertyDraw(sidCustomCategory10FreightSku, objFreight, objSkuFreight);
            addPropertyDraw(nameCountryOfOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(netWeightFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(grossWeightFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(mainCompositionOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(mainCompositionFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(additionalCompositionOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(additionalCompositionFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(quantityImporterFreightSku, objImporter, objFreight, objSkuFreight);
            addPropertyDraw(priceInOutImporterFreightSku, objImporter, objFreight, objSkuFreight);
            addPropertyDraw(sumInOutImporterFreightSku, objImporter, objFreight, objSkuFreight);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightSku, objFreight, objSku)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSkuFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightArticle, objFreight, objArticle)));

            RegularFilterGroupEntity filterGroupCustomCategory10 = new RegularFilterGroupEntity(genID());
            filterGroupCustomCategory10.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(customCategory10Sku, objSku))),
                                  "Только без ТН ВЭД",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroupCustomCategory10);

            RegularFilterGroupEntity filterGroupCountry = new RegularFilterGroupEntity(genID());
            filterGroupCountry.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(countryOfOriginSku, objSku))),
                                  "Только без страны",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupCountry);

            RegularFilterGroupEntity filterGroupWeight = new RegularFilterGroupEntity(genID());
            filterGroupWeight.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(netWeightSku, objSku))),
                    "Только без веса",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroupWeight);

            RegularFilterGroupEntity filterGroupComposition = new RegularFilterGroupEntity(genID());
            filterGroupComposition.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(mainCompositionOriginSku, objSku))),
                                  "Только без состава",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroupComposition);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objFreight.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objFreight.groupTo).grid.constraints.fillVertical = 1;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSkuFreight.groupTo).grid.constraints.fillVertical = 4;

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo),
                                   design.getGroupObjectContainer(objImporter.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSkuFreight.groupTo));
            specContainer.tabbedPane = true;

            return design;
        }
    }

    public class InvoiceFromFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objArticle;
        public ObjectEntity objCategory;
        public ObjectEntity objComposition;
        public ObjectEntity objCountry;

        private GroupObjectEntity gobjFreightImporter;
        private GroupObjectEntity gobjArticleCompositionCountryCategory;

        private InvoiceFromFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freight, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, date, objectClassName, nameExporterFreight, addressExporterFreight, nameCurrencyFreight);
            addPropertyDraw(objImporter, name, addressSubject, contractImporter);
            addPropertyDraw(objImporter, objFreight, netWeightImporterFreight, grossWeightImporterFreight, sumImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            gobjArticleCompositionCountryCategory = new GroupObjectEntity(4, "gobjArticleCompositionCountryCategory");
            objArticle = new ObjectEntity(5, "article", article, "Артикул");
            objComposition = new ObjectEntity(6, "composition", COMPOSITION_CLASS, "Состав");
            objCountry = new ObjectEntity(7, "country", country, "Страна");
            objCategory = new ObjectEntity(8, "category", customCategory10, "ТН ВЭД");

            gobjArticleCompositionCountryCategory.add(objArticle);
            gobjArticleCompositionCountryCategory.add(objComposition);
            gobjArticleCompositionCountryCategory.add(objCountry);
            gobjArticleCompositionCountryCategory.add(objCategory);
            addGroup(gobjArticleCompositionCountryCategory);

            addPropertyDraw(objArticle, sidArticle, nameArticle);
            addPropertyDraw(objComposition, objectValue);
            addPropertyDraw(objCountry, sidCountry, name);
            addPropertyDraw(objCategory, sidCustomCategory10);                            
            addPropertyDraw(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(objArticle, nameUnitOfMeasureArticle);
            addPropertyDraw(netWeightImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(grossWeightImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(priceImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(sumImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
        
            addPropertyDraw(addDEAProp(), objImporter, objFreight);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

            invoiceFormImporterFreight = addFAProp("Инвойс", this, objImporter, objFreight);
        }        
    }
    private class PackingListFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objFreightUnit;
        private ObjectEntity objArticle;
        private ObjectEntity objSku;

        private GroupObjectEntity gobjFreightImporter;

        private PackingListFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freight, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, date, objectClassName, nameExporterFreight, addressExporterFreight, nameCurrencyFreight);
            addPropertyDraw(objImporter, name, addressSubject, contractImporter);
            addPropertyDraw(objImporter, objFreight, netWeightImporterFreight, grossWeightImporterFreight, sumInOutImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objFreightUnit = addSingleGroupObject(4, "freightUnit", freightUnit, "Короб", barcode);
            addPropertyDraw(netWeightImporterFreightUnit, objImporter, objFreightUnit);

            objArticle = addSingleGroupObject(5, "article", article, "Артикул", sidArticle, nameBrandSupplierArticle, nameArticle);
            addPropertyDraw(quantityImporterFreightUnitArticle, objImporter, objFreightUnit, objArticle);
            
            /*objSku = addSingleGroupObject(5, "sku", sku, "SKU", barcode, sidArticleSku, nameBrandSupplierArticleSku, nameOriginCategoryArticleSku);
            addPropertyDraw(mainCompositionFreightSku, objFreight, objSku);
            addPropertyDraw(additionalCompositionFreightSku, objFreight, objSku);
            addPropertyDraw(sidCustomCategory10FreightSku, objFreight, objSku);
            addPropertyDraw(quantityImporterFreightUnitSku, objImporter, objBox, objSku);
            addPropertyDraw(nameCountryOfOriginFreightSku, objFreight, objSku);
            addPropertyDraw(netWeightImporterFreightUnitSku, objImporter, objBox, objSku);
            addPropertyDraw(grossWeightImporterFreightUnitSku, objImporter, objBox, objSku);
            addPropertyDraw(sumImporterFreightUnitSku, objImporter, objBox, objSku); */

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightUnit, objFreightUnit), Compare.EQUALS, objFreight));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightUnitArticle, objImporter, objFreightUnit, objArticle)));

            packingListFormImporterFreight = addFAProp("Упаковочный лист", this, objImporter, objFreight);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objFreightUnit.groupTo).grid.constraints.fillVertical = 2;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 3;
            return design;
        }
    }

    private class ColorSizeSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objColor;
        private ObjectEntity objSize;
        private ObjectEntity objBrand;
        private ObjectEntity objCountry;

        private ColorSizeSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, nameBrandSupplierSupplier, nameCurrencySupplier, nameDictionaryComposition);
            addObjectActions(this, objSupplier);

            objColor = addSingleGroupObject(colorSupplier, "Цвет", sidColorSupplier, name);
            addObjectActions(this, objColor);

            objSize = addSingleGroupObject(sizeSupplier, "Размер", sidSizeSupplier);
            addObjectActions(this, objSize);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд", name);
            addObjectActions(this, objBrand);

            objCountry = addSingleGroupObject(countrySupplier, "Страна", name, nameCountryCountrySupplier);
            addObjectActions(this, objCountry);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColor), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSize), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierBrandSupplier, objBrand), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierCountrySupplier, objCountry), Compare.EQUALS, objSupplier));
        }
        
        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 0.5;
            design.get(objColor.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objSize.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objBrand.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objCountry.groupTo).grid.constraints.fillHorizontal = 2;

            design.addIntersection(design.getGroupObjectContainer(objColor.groupTo),
                                   design.getGroupObjectContainer(objSize.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            
            design.addIntersection(design.getGroupObjectContainer(objSize.groupTo),
                                   design.getGroupObjectContainer(objBrand.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            
            design.addIntersection(design.getGroupObjectContainer(objBrand.groupTo),
                                   design.getGroupObjectContainer(objCountry.groupTo),
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

            objSku = addSingleGroupObject(sku, "SKU", barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameCategoryArticleSku, sidCustomCategoryOriginArticleSku,
                    nameCountryOfOriginSku, netWeightSku, mainCompositionOriginSku,
                    additionalCompositionOriginSku);

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
        private ObjectEntity objBrandSupplier;
        private ObjectEntity objSku;
        
        private FreightInvoiceFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", date, objectClassName, nameRouteFreight, nameFreightTypeFreight, tonnageFreight, palletCountFreight, volumeFreight, nameCurrencyFreight, sumInFreight, sumMarkupInFreight, sumInOutFreight, palletNumberFreight);

            addPropertyDraw(sumInCurrentYear);
            addPropertyDraw(sumInOutCurrentYear);
            addPropertyDraw(balanceSumCurrentYear);

            objImporter = addSingleGroupObject(importer, "Импортер", name);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumMarkupInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInOutImporterFreight, objImporter, objFreight);

            objBrandSupplier = addSingleGroupObject(brandSupplier, "Бренд", name, nameSupplierBrandSupplier);

            addPropertyDraw(quantityImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);
            addPropertyDraw(markupPercentImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);

            objSku = addSingleGroupObject(sku, "SKU", barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameBrandSupplierArticleSku, nameCategoryArticleSku, sidCustomCategoryOriginArticleSku,
                    nameCountrySku, netWeightSku, mainCompositionOriginSku,
                    additionalCompositionOriginSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            addPropertyDraw(quantityImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(markupInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInOutImporterFreightSku, objImporter, objFreight, objSku);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSku)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                                          new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(priceInImporterFreightSku, objImporter, objFreight, objSku))),
                                                          "Только без цены",
                                                          KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView specContainer = design.createContainer("Итоги");
            specContainer.add(design.get(getPropertyDraw(sumInCurrentYear)));
            specContainer.add(design.get(getPropertyDraw(sumInOutCurrentYear)));
            specContainer.add(design.get(getPropertyDraw(balanceSumCurrentYear)));
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objFreight.groupTo));

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo), specContainer, DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objImporter.groupTo), design.getGroupObjectContainer(objBrandSupplier.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objSku.groupTo).grid.constraints.fillVertical = 3;

            return design;
        }
    }

    private class CreateItemFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objSupplier;
        ObjectEntity objBarcode;
        ObjectEntity objSIDArticleComposite;
        ObjectEntity objItem;

        public CreateItemFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", objectValue);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Артикул", objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            objItem = addSingleGroupObject(item, "Товар", sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            objItem.groupTo.setSingleClassView(ClassViewType.PANEL);

//            objItem.addOnTransaction = true;
//            addObjectActions(this, objItem);

            addActionsOnApply(addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
            addActionsOnApply(addPropertyObject(executeArticleCompositeItemSIDSupplier, objItem, objSIDArticleComposite, objSupplier));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.setEnabled(objSupplier, false);
            design.setEnabled(objBarcode, false);
            return design;
        }
    }

    public class SeekRouteActionProperty extends ActionProperty {

        private ClassPropertyInterface shipmentInterface;
        private ClassPropertyInterface skuInterface;
        private ClassPropertyInterface routeInterface;

        // route в интерфейсе нужен только, чтобы найти нужный ObjectInstance (не хочется бегать и искать его по массиву ObjectInstance)
        public SeekRouteActionProperty() {
            super(genSID(), "Поиск маршрута", new ValueClass[] {shipment, sku, route});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            shipmentInterface = i.next();
            skuInterface = i.next();
            routeInterface = i.next();
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            FormInstance<?> form = (FormInstance<?>)executeForm.form;
            DataSession session = form.session;

            DataObject objShipment = keys.get(shipmentInterface);
            DataObject objSku = keys.get(skuInterface);

            DataObject objRouteRB = route.getDataObject("rb");
            DataObject objRouteRF = route.getDataObject("rf");

            Double invoiced = (Double) invoicedShipmentSku.read(session, objShipment, objSku);

            DataObject objRouteResult = null;
            if (invoiced == null) {
                objRouteResult = objRouteRF;
            } else {

                Double invoicedRB = (Double)BaseUtils.nvl(invoicedShipmentRouteSku.read(session, objShipment, objRouteRB, objSku), 0.0);
                Double quantityRB = (Double)BaseUtils.nvl(quantityShipmentRouteSku.read(session, objShipment, objRouteRB, objSku), 0.0);

                Double invoicedRF = (Double)BaseUtils.nvl(invoicedShipmentRouteSku.read(session, objShipment, objRouteRF, objSku), 0.0);
                Double quantityRF = (Double)BaseUtils.nvl(quantityShipmentRouteSku.read(session, objShipment, objRouteRF, objSku), 0.0);

                if (quantityRB + 1E-9 < invoicedRB) {
                    objRouteResult = objRouteRB;
                } else
                    if (quantityRF + 1E-9 < invoicedRF) {
                        objRouteResult = objRouteRF;
                    } else
                        objRouteResult = objRouteRB;
            }

            form.seekObject((ObjectInstance)mapObjects.get(routeInterface), objRouteResult);
        }
    }

    public static class JennyferInvoiceImporter extends ExcelSheetImporter {
        private static final int LAST_COLUMN = P;

        public JennyferInvoiceImporter(jxl.Sheet sheet, Object... fields) {
            super(sheet, fields);
        }

        @Override
        protected boolean isCorrectRow(int rowNum) {
            return sheet.getCell(C, rowNum).getContents().trim().matches("^'(\\d{13}|\\d{12}|\\d{8})$");
        }

        @Override
        protected String getCellString(int row, int column) {
            if (column <= LAST_COLUMN) {
                return super.getCellString(row, column);
            } else if (column == LAST_COLUMN + 1) {
                return String.valueOf(currentRow + 1);
            } else if (column == LAST_COLUMN + 2) {
                String customCode = super.getCellString(row, K);
                return customCode.substring(0, Math.min(6, customCode.length()));
            } else {
                return "";
            }
        }

        @Override
        protected String transformValue(int row, int column, int part, String value) {
            value = value.trim();

            switch (column) {
                case C:
                    switch (part) {
                        case 0 : return value.substring(1); // barcode
                        case 1 : return value.substring(1, 7); // article
                    }
                case K: return value.substring(0, Math.min(10, value.length())); // customs code
                case N: case O: return value.replace(',', '.');
                case E:
                    switch (part) {
//                        case 0: return value.substring(0, value.indexOf(' ')); // sid
                        case 0: return value.substring(value.indexOf(' ') + 1, value.lastIndexOf(' ')).trim(); // color
                        case 1: return value.substring(value.lastIndexOf(' ') + 1); // size
                    }
                default: return value;
            }
        }
    }

    public static class TallyWeijlInvoiceImporter extends ExcelSheetImporter {
        private static final int LAST_COLUMN = AF;

        public TallyWeijlInvoiceImporter(jxl.Sheet sheet, Object... fields) {
            super(sheet, fields);
        }

        @Override
        protected boolean isCorrectRow(int rowNum) {
            return sheet.getCell(M, rowNum).getContents().trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
        }

        @Override
        protected String getCellString(int row, int column) {
            if (column == LAST_COLUMN + 1) {
                return String.valueOf(currentRow + 1);
            } else if (column == LAST_COLUMN + 2) {
                String customCode = super.getCellString(row, K);
                return customCode.substring(0, Math.min(6, customCode.length()));
            }
            return super.getCellString(row, column);
        }

        @Override
        protected String transformValue(int row, int column, int part, String value) {
            value = value.trim();

            switch (column) {
                case L: return value.substring(0, Math.min(10, value.length())); // customs code
                case X: case AD: return value.replace(',', '.');
                case Q:
                    int lastBackslashPos = value.lastIndexOf('\\');
                    return (lastBackslashPos == -1 ? value : value.substring(0, lastBackslashPos));
                case R:
                    switch (part) {
                        case 0: return value.substring(0, value.indexOf(',')).trim(); // original name
                        case 1: return value.substring(value.indexOf(',') + 1, value.lastIndexOf(',')).trim(); // color
                    }
                default: return value;
            }
        }
    }

    public class JennyferImportInvoiceActionProperty extends ImportInvoiceActionProperty {

        public JennyferImportInvoiceActionProperty() {
            super("Импортировать инвойс", jennyferSupplier);
        }

        @Override
        protected ExcelSheetImporter createExporter(Sheet sheet) {
            return new JennyferInvoiceImporter(sheet, new Object[] {invoiceSIDField, boxNumberField, new ImportField[] {barCodeField, sidField}, colorCodeField,
                    new ImportField[] {colorNameField, sizeField}, null, compositionField, null, null,
                    countryField, customCodeField, null, null, unitPriceField, unitQuantityField, null,
                    numberSkuField, customCode6Field, unitNetWeightField, originalNameField});
        }
    }

    public class JennyferImportArticleWeightActionProperty extends SupplierActionProperty {

        public JennyferImportArticleWeightActionProperty() {
            super("Импортировать вес", jennyferSupplier);
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            DataObject supplier = keys.get(supplierInterface);
            FormInstance remoteForm = executeForm.form;
            DataSession session = remoteForm.session;

            ImportField sidField = new ImportField(sidArticle);
            ImportField netWeightField = new ImportField(netWeightArticle);

            ImportTable table;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                Sheet sheet = Workbook.getWorkbook(inFile).getSheet(0);

                table = new ExcelSheetImporter(sheet, new Object[] {sidField, netWeightField}) {
                    @Override
                    protected boolean isCorrectRow(int rowNum) {
                        return sheet.getCell(A, rowNum).getContents().trim().matches("^(\\d{6})$");
                    }
                    @Override
                    protected String getCellString(int row, int column) {
                        if (column == B)
                            return ((Double)(Double.parseDouble(super.getCellString(row, K).replace(',', '.')) /
                                   Double.parseDouble(super.getCellString(row, H).replace(',', '.')))).toString();
                        return super.getCellString(row, column);
                    }
                }.getTable();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class TallyWeijlImportInvoiceActionProperty extends ImportInvoiceActionProperty {

        public TallyWeijlImportInvoiceActionProperty() {
            super("Импортировать инвойс", tallyWeijlSupplier);
        }

        @Override
        protected ExcelSheetImporter createExporter(Sheet sheet) {
            return new TallyWeijlInvoiceImporter(sheet, new Object[] {null, null, invoiceSIDField, null, null, null, null, null,
                    compositionField, countryField, boxNumberField, customCodeField, barCodeField, null,
                    sizeField, colorCodeField, sidField, new ImportField[] {originalNameField, colorNameField},
                    null, null, null, null, unitQuantityField, unitNetWeightField, null, null, null, null, null,
                    unitPriceField, null, null, numberSkuField, customCode6Field});
        }
    }

    public abstract class ImportInvoiceActionProperty extends SupplierActionProperty {

        protected ImportField invoiceSIDField, boxNumberField, barCodeField, colorCodeField, sidField,
        colorNameField, sizeField, compositionField, countryField, customCodeField, unitPriceField,
        unitQuantityField, unitNetWeightField, originalNameField, numberSkuField, customCode6Field;

        public ImportInvoiceActionProperty(String caption, ValueClass supplierClass) {
            super(caption, supplierClass);
        }

        protected abstract ExcelSheetImporter createExporter(Sheet sheet);

        private void initFields() {
            invoiceSIDField = new ImportField(sidDocument);
            boxNumberField = new ImportField(sidSupplierBox);
            barCodeField = new ImportField(barcode);
            colorCodeField = new ImportField(sidColorSupplier);
            sidField = new ImportField(sidArticle);
            colorNameField = new ImportField(name);
            sizeField = new ImportField(sidSizeSupplier);
            compositionField = new ImportField(mainCompositionOriginArticle);
            countryField = new ImportField(name);
            customCodeField = new ImportField(sidCustomCategoryOrigin);
            customCode6Field = new ImportField(sidCustomCategory6);
            unitPriceField = new ImportField(quantityDataListSku);
            unitQuantityField = new ImportField(priceDataDocumentItem);
            unitNetWeightField = new ImportField(netWeightArticle);
            originalNameField = new ImportField(originalNameArticle);
            numberSkuField = new ImportField(numberDataListSku);
        }


        public void execute(final Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            DataObject supplier = keys.get(supplierInterface);
            FormInstance remoteForm = executeForm.form;
            DataSession session = remoteForm.session;

            initFields();

            ImportTable table;
            try {
                ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
                Sheet sheet = Workbook.getWorkbook(inFile).getSheet(0);

                table = createExporter(sheet).getTable();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> invoiceKey = new ImportKey(boxInvoice, documentSIDSupplier.getMapping(invoiceSIDField, supplier));
            properties.add(new ImportProperty(invoiceSIDField, sidDocument.getMapping(invoiceKey)));
            properties.add(new ImportProperty(supplier, supplierDocument.getMapping(invoiceKey)));

            ImportKey<?> boxKey = new ImportKey(supplierBox, supplierBoxSIDSupplier.getMapping(boxNumberField, supplier));
            properties.add(new ImportProperty(invoiceSIDField, boxInvoiceSupplierBox.getMapping(boxKey), object(boxInvoice).getMapping(invoiceKey)));
            properties.add(new ImportProperty(boxNumberField, sidSupplierBox.getMapping(boxKey)));

            ImportKey<?> articleKey = new ImportKey(articleComposite, articleSIDSupplier.getMapping(sidField, supplier));
            properties.add(new ImportProperty(sidField, sidArticle.getMapping(articleKey)));
            properties.add(new ImportProperty(supplier, supplierArticle.getMapping(articleKey)));
            properties.add(new ImportProperty(compositionField, mainCompositionOriginArticle.getMapping(articleKey)));
            properties.add(new ImportProperty(unitNetWeightField, netWeightArticle.getMapping(articleKey)));
            properties.add(new ImportProperty(originalNameField, originalNameArticle.getMapping(articleKey)));

            ImportKey<?> itemKey = new ImportKey(item, barcodeToObject.getMapping(barCodeField));
            properties.add(new ImportProperty(barCodeField, barcode.getMapping(itemKey)));
            properties.add(new ImportProperty(sidField, articleCompositeItem.getMapping(itemKey), object(articleComposite).getMapping(articleKey)));

            ImportKey<?> countryKey = new ImportKey(countrySupplier, countryNameSupplier.getMapping(countryField, supplier));
            properties.add(new ImportProperty(countryField, name.getMapping(countryKey)));
            properties.add(new ImportProperty(supplier, supplierCountrySupplier.getMapping(countryKey)));
            properties.add(new ImportProperty(countryField, countrySupplierOfOriginArticle.getMapping(articleKey), object(countrySupplier).getMapping(countryKey)));

            ImportKey<?> customCategoryKey = new ImportKey(customCategoryOrigin, sidToCustomCategoryOrigin.getMapping(customCodeField));
            properties.add(new ImportProperty(customCodeField, sidCustomCategoryOrigin.getMapping(customCategoryKey)));
            properties.add(new ImportProperty(customCodeField, customCategoryOriginArticle.getMapping(articleKey), object(customCategoryOrigin).getMapping(customCategoryKey)));

            ImportKey<?> customCategory6Key = new ImportKey(customCategory6, sidToCustomCategory6.getMapping(customCode6Field));
            properties.add(new ImportProperty(customCode6Field, sidCustomCategory6.getMapping(customCategory6Key)));

            ImportKey<?> colorKey = new ImportKey(colorSupplier, colorSIDSupplier.getMapping(colorCodeField, supplier));
            properties.add(new ImportProperty(colorCodeField, sidColorSupplier.getMapping(colorKey)));
            properties.add(new ImportProperty(supplier, supplierColorSupplier.getMapping(colorKey)));
            properties.add(new ImportProperty(colorNameField, name.getMapping(colorKey)));
            properties.add(new ImportProperty(colorCodeField, colorSupplierItem.getMapping(itemKey), object(colorSupplier).getMapping(colorKey)));

            ImportKey<?> sizeKey = new ImportKey(sizeSupplier, sizeSIDSupplier.getMapping(sizeField, supplier));
            properties.add(new ImportProperty(sizeField, sidSizeSupplier.getMapping(sizeKey)));
            properties.add(new ImportProperty(supplier, supplierSizeSupplier.getMapping(sizeKey)));
            properties.add(new ImportProperty(sizeField, sizeSupplierItem.getMapping(itemKey), object(sizeSupplier).getMapping(sizeKey)));

            properties.add(new ImportProperty(numberSkuField, numberListArticle.getMapping(boxKey, articleKey)));
            properties.add(new ImportProperty(numberSkuField, numberDataListSku.getMapping(boxKey, itemKey)));
            properties.add(new ImportProperty(unitQuantityField, quantityDataListSku.getMapping(boxKey, itemKey)));
            properties.add(new ImportProperty(unitPriceField, priceDataDocumentItem.getMapping(invoiceKey, itemKey)));
            properties.add(new ImportProperty(unitPriceField, priceDocumentArticle.getMapping(invoiceKey, articleKey)));


            ImportKey<?>[] keysArray = {invoiceKey, boxKey, articleKey, itemKey, colorKey, sizeKey, countryKey, customCategoryKey, customCategory6Key};
            new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);

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

    public abstract class SupplierActionProperty extends ActionProperty {

        protected final ClassPropertyInterface supplierInterface;

        public SupplierActionProperty(String caption, ValueClass supplierClass) {
            super(genSID(), caption, new ValueClass[]{supplierClass});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            supplierInterface = i.next();
        }
    }
}
