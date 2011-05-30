package roman;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.AudioClientAction;
import platform.interop.action.ClientAction;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.Settings;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
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
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: DAle
 * Date: 24.05.11
 * Time: 15:14
 */


public class RomanLogicsModule extends LogicsModule {
    private final RomanBusinessLogics BL;

    public RomanLogicsModule(BaseLogicsModule<RomanBusinessLogics> baseLM, RomanBusinessLogics BL) {
        setBaseLogicsModule(baseLM);
        this.BL = BL;
    }

    private static StringClass COMPOSITION_CLASS = StringClass.get(200);

    private AbstractCustomClass article;
    ConcreteCustomClass articleComposite;
    private ConcreteCustomClass articleSingle;
    ConcreteCustomClass item;
    protected AbstractCustomClass sku;
    private ConcreteCustomClass pallet;
    LP sidArticle;
    LP articleCompositeItem;
    private LP round2;
    private LP articleSku;
    private ConcreteCustomClass order;
    private AbstractCustomClass invoice;
    protected AbstractCustomClass shipment;
    private ConcreteCustomClass boxShipment;
    private ConcreteCustomClass simpleShipment;
    private ConcreteCustomClass freight;
    private StaticCustomClass route;
    private AbstractCustomClass seller;
    private ConcreteCustomClass supplier;
    private ConcreteCustomClass contract;
    private AbstractCustomClass document;
    private AbstractCustomClass priceDocument;
    private AbstractCustomClass subject;
    LP addressOriginSubject;
    LP addressSubject;
    LP supplierDocument;
    private LP nameSupplierDocument;
    LP sidDocument;
    LP documentSIDSupplier;
    private LP sumDocument;
    ConcreteCustomClass commonSize;
    ConcreteCustomClass colorSupplier;
    ConcreteCustomClass sizeSupplier;
    private ConcreteCustomClass brandSupplier;
    private ConcreteCustomClass themeSupplier;
    private ConcreteCustomClass season;
    ConcreteCustomClass countrySupplier;
    LP supplierArticle;
    private LP nameSupplierArticle;
    LP priceDocumentArticle;
    LP RRPDocumentArticle;
    private LP sumDocumentArticle;
    LP colorSupplierItem;
    private LP nameColorSupplierItem;
    LP sizeSupplierItem;
    LP sidSizeSupplierItem;
    LP supplierColorSupplier;
    private LP nameSupplierColorSupplier;
    LP supplierSizeSupplier;
    private LP nameSupplierSizeSupplier;
    private LP sidBrandSupplier;
    private LP supplierBrandSupplier;
    private LP nameSupplierBrandSupplier;
    private LP brandSupplierSupplier;
    private LP nameBrandSupplierSupplier;
    private LP brandSupplierArticle;
    private LP sidBrandSupplierArticle;
    private LP nameBrandSupplierArticle;
    private LP supplierBrandSupplierArticle;
    private LP brandSupplierDataArticle;
    private LP brandSupplierSupplierArticle;
    private LP brandSupplierArticleSku;
    private LP sidBrandSupplierArticleSku;
    private LP nameBrandSupplierArticleSku;
    private LP nameBrandSupplierArticleSkuShipmentDetail;
    private LP supplierArticleSku;
    private LP nameSupplierArticleSku;
    private LP supplierThemeSupplier;
    private LP themeSupplierArticle;
    private LP nameThemeSupplierArticle;
    private LP themeSupplierArticleSku;
    private LP nameThemeSupplierArticleSku;
    private LP seasonArticle;
    private LP nameSeasonArticle;

    private ConcreteCustomClass currency;
    private ConcreteCustomClass typeExchange;
    private ConcreteCustomClass store;
    private ConcreteCustomClass unitOfMeasure;
    private LP currencyTypeExchange;
    private LP nameCurrencyTypeExchange;
    private LP rateExchange;
    private LP typeExchangeSTX;
    private LP nameTypeExchangeSTX;
    private LP lessCmpDate;
    private LP nearestPredDate;
    private LP nearestRateExchange;
    private LP sidDestination;
    private LP destinationSID;
    private LP unitOfMeasureCategory;
    private LP nameUnitOfMeasureCategory;
    private LP unitOfMeasureArticle;
    private LP nameOriginUnitOfMeasureArticle;
    LP nameUnitOfMeasureArticle;
    private LP unitOfMeasureArticleSku;
    private LP nameUnitOfMeasureArticleSku;
    LP supplierCountrySupplier;
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
    LP sidColorSupplier;
    private LP sidColorSupplierItem;
    private LP quantityDocumentSku;
    private LP quantityDocumentBrandSupplier;
    private LP quantityDocumentArticle;
    private LP quantityDocumentArticleCompositeColor;
    private LP quantityDocumentArticleCompositeSize;
    private LP quantityDocumentArticleCompositeColorSize;
    private LP quantityDocument;
    private LP netWeightDocumentArticle;
    private LP netWeightDocument;
    LP originalNameArticle;
    private LP nameArticle;
    private LP netWeightArticleSku;
    LP netWeightDataSku;
    private LP netWeightSku;
    private LP sumNetWeightFreightSku;
    LP netWeightArticle;
    private LP netWeightArticleSize;
    private LP netWeightArticleSizeSku;
    private LP netWeightSkuShipmentDetail;
    private LP mainCompositionOriginDataSku;
    private LP additionalCompositionOriginDataSku;
    private LP mainCompositionOriginSku;
    private LP additionalCompositionOriginSku;
    AbstractCustomClass secondNameClass;
    private LP nameOrigin;
    private ConcreteCustomClass category;
    ConcreteCustomClass customCategory4;
    ConcreteCustomClass customCategory6;
    ConcreteCustomClass customCategory9;
    ConcreteCustomClass customCategory10;
    ConcreteCustomClass customCategoryOrigin;
    private LP categoryArticle;
    private LP unitOfMeasureDataArticle;
    private LP unitOfMeasureCategoryArticle;
    private LP nameOriginUnitOfMeasureArticleSku;
    private LP nameOriginCategoryArticle;
    private LP nameCategoryArticle;
    private LP categoryArticleSku;
    private LP nameCategoryArticleSku;
    private LP nameOriginCategoryArticleSku;
    LP sidCustomCategory4;
    LP sidCustomCategory6;
    LP sidCustomCategory9;
    LP sidCustomCategory10;
    LP sidCustomCategoryOrigin;

    LP sidToCustomCategory4;
    LP sidToCustomCategory6;
    LP sidToCustomCategory9;
    LP sidToCustomCategory10;
    LP sidToCustomCategoryOrigin;
    private LP importBelTnved;
    private LP importEuTnved;

    LP customCategory4CustomCategory6;
    LP customCategory6CustomCategory9;
    LP customCategory9CustomCategory10;
    LP customCategory6CustomCategory10;
    LP customCategory4CustomCategory10;
    LP customCategory6CustomCategoryOrigin;
    LP customCategory4CustomCategoryOrigin;
    LP customCategory10CustomCategoryOrigin;
    LP sidCustomCategory10CustomCategoryOrigin;
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
    LP relationCustomCategory10CustomCategoryOrigin;
    private LP customCategory10DataSku;
    private LP customCategory10Sku;
    private LP customCategory6FreightSku;
    private LP sidCustomCategory10Sku;
    private LP customCategory10CustomCategoryOriginArticle;
    private LP customCategory10CustomCategoryOriginArticleSku;
    private LP mainCompositionArticle;
    private LP additionalCompositionArticle;
    LP mainCompositionOriginArticle;
    private LP additionalCompositionOriginArticle;
    private LP mainCompositionOriginArticleColor;
    private LP additionalCompositionOriginArticleColor;
    private LP mainCompositionOriginArticleSku;
    private LP additionalCompositionOriginArticleSku;
    private LP mainCompositionOriginArticleColorSku;
    private LP additionalCompositionOriginArticleColorSku;
    private LP mainCompositionSku;
    private LP additionalCompositionSku;
    LP countrySupplierOfOriginArticle;
    private LP countryOfOriginArticle;
    private LP nameCountryOfOriginArticle;
    private LP countryOfOriginArticleColor;
    private LP nameCountryOfOriginArticleColor;
    private LP countryOfOriginArticleColorSku;
    private LP countryOfOriginArticleSku;
    private LP countryOfOriginDataSku;
    private LP countryOfOriginSku;
    private LP nameCountryOfOriginSku;
    private LP nameCountrySupplierOfOriginArticle;
    private LP nameCountryOfOriginArticleSku;
    private LP countrySupplierOfOriginSku;
    LP articleSIDSupplier;
    private LP seekArticleSIDSupplier;
    LP numberListArticle;
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
    ConcreteCustomClass supplierBox;
    ConcreteCustomClass boxInvoice;
    private ConcreteCustomClass simpleInvoice;
    LP sidSupplierBox;
    private AbstractCustomClass list;
    LP quantityDataListSku;
    LP boxInvoiceSupplierBox;
    private LP sidBoxInvoiceSupplierBox;
    private LP supplierSupplierBox;
    private LP supplierList;
    private LP orderedSupplierBoxSku;
    private LP quantityListArticle;
    private LP orderedSimpleInvoiceSku;
    LP priceDataDocumentItem;
    private LP priceArticleDocumentSku;
    private LP priceImporterFreightSku;
    private LP priceMaxImporterFreightSku;
    private LP priceDocumentSku;
    private LP priceRateDocumentSku;
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
    LP nameOriginExporterFreight;
    LP nameExporterFreight;
    LP addressOriginExporterFreight;
     LP addressExporterFreight;
    private ConcreteCustomClass stock;
    private ConcreteCustomClass freightBox;
    private LP sidArticleSku;
    private LP originalNameArticleSku;
    private LP inSupplierBoxShipment;
    private LP quantitySupplierBoxBoxShipmentStockSku;
    private LP quantitySupplierBoxBoxShipmentSku;
    private LP quantitySimpleShipmentStockSku;
    private LP barcodeAction4;
    LP supplierBoxSIDSupplier;
    private LP seekSupplierBoxSIDSupplier;
    private LP quantityPalletShipmentBetweenDate;
    private LP quantityPalletFreightBetweenDate;
    private LP quantityShipmentStockSku;
    private LP quantityShipmentRouteSku;
    private LP quantityShipDimensionShipmentSku;
    private LP diffListShipSku;
    private LP supplierPriceDocument;
    private LP percentShipmentRoute;
    private LP percentShipmentRouteSku;
    private LP invoicedShipmentSku;
    private LP priceShipmentSku;
    private LP invoicedShipment;
    private LP invoicedShipmentRouteSku;
    private LP zeroInvoicedShipmentRouteSku;
    private LP zeroQuantityShipmentRouteSku;
    private LP diffShipmentRouteSku;
    private LP sumShipmentRouteSku;
    private LP sumShipmentRoute;
    private LP sumShipment;
    private LP invoicedShipmentRoute;

    private LP documentList;
    private LP numberDocumentArticle;
    private ConcreteCustomClass shipDimension;
    private LP quantityShipDimensionShipmentStockSku;
    private LP quantityShipmentSku;
    private LP quantityShipment;
    private LP barcodeCurrentPalletRoute;
    private LP barcodeCurrentFreightBoxRoute;
    private LP equalsPalletFreight;
    private LP equalsPalletFreightBox;
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
    private LP diffPalletFreight;
    private LP barcodePalletFreightBox;
    private LP freightBoxNumberPallet;
    private LP notFilledShipmentRouteSku;
    private LP routeToFillShipmentSku;
    private LP seekRouteToFillShipmentBarcode;
    private LP quantityShipmentArticle;
    private LP oneShipmentArticle;
    private LP oneShipmentArticleSku;
    private LP oneShipmentArticleColorSku;
    private LP oneShipmentArticleSizeSku;
    private LP quantityShipmentArticleSize;
    private LP quantityShipmentArticleColor;
    private LP oneShipmentArticleSize;
    private LP oneShipmentArticleColor;

    private LP oneShipmentSku;
    private LP quantityBoxShipment;
    private LP quantityShipmentStock;
    private LP quantityShipmentPallet;
    private LP quantityShipmentFreight;
    private LP balanceStockSku;
    private LP quantityStockSku;
    private LP quantityStockArticle;
    private LP quantityStockBrandSupplier;
    private LP quantityPalletSku;
    private LP quantityPalletBrandSupplier;
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
    LP nameCustomCategory;
    LP customCategoryOriginArticle;
    private LP customCategoryOriginArticleSku;
    private LP sidCustomCategoryOriginArticle;
    private LP sidCustomCategoryOriginArticleSku;
    private LP quantityBoxInvoiceBoxShipmentStockSku;
    private LP quantityInvoiceShipmentStockSku;
    private LP invoicedSimpleInvoiceSimpleShipmentStockSku;
    private LP quantitySimpleInvoiceSimpleShipmentStockSku;
    private LP quantityInvoiceStockSku;
    private LP quantityInvoiceSku;
    private LP diffDocumentInvoiceSku;
    private LP quantitySupplierBoxSku;
    private LP zeroQuantityListSku;
    private LP zeroQuantityShipDimensionShipmentSku;
    private LP diffListSupplierBoxSku;
    private LP zeroQuantityShipmentSku;
    private LP zeroInvoicedShipmentSku;
    private LP diffShipmentSku;
    private ConcreteCustomClass importer;
    private ConcreteCustomClass exporter;
    private LP sidContract;
    private LP importerContract;
    private LP nameImporterContract;
    private LP sellerContract;
    private LP nameSellerContract;
    private LP currencyContract;
    private LP nameCurrencyContract;
    private LP contractImporter;
    private LP importerInvoice;
    private LP nameImporterInvoice;
    private LP contractInvoice;
    private LP sidContractInvoice;
    private LP freightFreightBox;
    private LP importerSupplierBox;
    private LP quantityImporterStockSku;
    private LP quantityDirectImporterFreightUnitSku;
    private LP quantityImporterSku;
    private LP quantityImporterStock;
    private LP quantityImporterStockArticle;
    private LP quantityImporterFreightBoxSku;
    private LP quantityImporterFreightBoxArticle;
    private LP quantityProxyImporterFreightSku;
    private LP quantityDirectImporterFreightSku;
    private LP quantityImporterFreightSku;

    private LP netWeightStockSku;
    private LP netWeightStockArticle;
    private LP netWeightStock;
    private LP netWeightImporterFreightUnitSku;
    private LP netWeightImporterFreightUnitArticle;
    private LP netWeightImporterFreightUnit;
    private LP grossWeightImporterFreightUnitSku;
    private LP grossWeightImporterFreightUnitArticle;
    private LP grossWeightImporterFreightUnit;
    private LP priceInInvoiceStockSku;
    private LP priceInImporterFreightSku;
    private LP netWeightImporterFreightSku;
    private LP grossWeightImporterFreightSku;
    private LP netWeightImporterFreightCustomCategory6;
    private LP grossWeightImporterFreightCustomCategory6;

    private LP netWeightImporterFreight;
    private LP grossWeightImporterFreight;
    private LP priceFreightImporterFreightSku;
    private LP oneArticleSkuShipmentDetail;
    private LP oneArticleColorShipmentDetail;
    private LP oneArticleSizeShipmentDetail;
    private LP oneSkuShipmentDetail;
    private LP quantityImporterFreight;
    private LP quantitySbivkaImporterFreight;
    private LP quantityImporterFreightBrandSupplier;
    private LP markupPercentImporterFreightBrandSupplier;
    private LP markupPercentImporterFreightDataSku;
    private LP markupPercentImporterFreightBrandSupplierSku;
    private LP markupInImporterFreightSku;
    private LP priceExpenseImporterFreightSku;
    private LP markupPercentImporterFreightSku;
    private LP markupImporterFreightSku;
    private LP priceMarkupInImporterFreightSku;
    private LP priceOutImporterFreightSku;
    private LP priceInOutImporterFreightSku;
    private LP sumInImporterFreightSku;
    private LP sumInProxyImporterFreightSku;
    private LP sumMarkupImporterFreightSku;
    private LP sumOutImporterFreightSku;
    private LP sumInImporterFreight;
    private LP sumMarkupImporterFreight;
    private LP sumMarkupInImporterFreight;
    private LP sumMarkupInImporterFreightSku;
    private LP sumInOutImporterFreightSku;
    private LP sumImporterFreightUnitSku;
    private LP sumImporterFreightUnitArticle;
    private LP sumMarkupInFreight;
    private LP sumImporterFreight;
    private LP sumSbivkaImporterFreight;
    private LP sumOutImporterFreight;
    private LP sumInOutImporterFreight;
    private LP sumInOutFreight;
    private LP sumInFreight;
    private LP sumMarkupFreight;
    private LP sumOutFreight;
    private LP sumFreightImporterFreightSku;
    private LP quantityProxyFreightArticle;
    private LP quantityDirectFreightArticle;
    private LP quantityImporterFreightCustomCategory6;
    private LP quantityFreightArticle;
    private LP quantityProxyFreightSku;
    private LP quantityDirectFreightSku;
    private LP quantityFreightSku;
    private LP quantityFreightCategory;
    private LP sumImporterFreightSku;
    private LP sumImporterFreightCustomCategory6;
    LP quantityImporterFreightArticleCompositionCountryCategory;
    LP compositionFreightArticleCompositionCountryCategory;
    LP netWeightImporterFreightArticleCompositionCountryCategory;
    LP grossWeightImporterFreightArticleCompositionCountryCategory;
    LP priceImporterFreightArticleCompositionCountryCategory;
    LP sumImporterFreightArticleCompositionCountryCategory;
    private ConcreteCustomClass freightComplete;
    private ConcreteCustomClass freightPriced;
    private ConcreteCustomClass freightChanged;
    private ConcreteCustomClass freightShipped;
    private LP dictionaryComposition;
    private LP nameDictionaryComposition;
    private LP translationMainCompositionSku;
    private LP translationAdditionalCompositionSku;
    private LP translationAllMainComposition;
    private LP sidShipmentShipmentDetail;
    private LP commonSizeSizeSupplier;
    private LP nameCommonSizeSizeSupplier;
    LP colorSIDSupplier;
    LP sidSizeSupplier;
    LP sizeSIDSupplier;
    LP brandSIDSupplier;
    LP countryNameSupplier;
    LP numberDataListSku;
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
    private LP executeChangeFreightClass;
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
    private LP currentPalletRouteUser;
    LP currentPalletRoute;
    private LP currentFreightBoxRouteUser;
    LP currentFreightBoxRoute;
    LP isCurrentFreightBox, isCurrentPalletFreightBox;
    LP isCurrentPallet;
    private LP changePallet;
    LP barcodeActionSeekPallet, barcodeActionSetPallet, barcodeActionSetPalletFreightBox, barcodeActionSetFreight, barcodeAction3;
    private LP invoiceOriginFormImporterFreight;
    private LP invoiceFormImporterFreight;
    private LP proformOriginFormImporterFreight;
    private LP proformFormImporterFreight;
    private LP annexInvoiceOriginFormImporterFreight;
    private LP annexInvoiceFormImporterFreight;
    private LP packingListFormImporterFreight;
    private LP sbivkaFormImporterFreight;

    private LP countrySupplierOfOriginArticleSku;
    private LP nameCountrySupplierOfOriginArticleSku;
    private AbstractCustomClass directInvoice;
    private ConcreteCustomClass directBoxInvoice;
    private LP freightDirectInvoice;
    private LP equalsDirectInvoiceFreight;
    private LP grossWeightDirectInvoice;
    private LP palletNumberDirectInvoice;
    private LP nameOriginCountry;
    private LP sidOrigin2Country;
    private LP sidOrigin3Country;
    private LP sid3Country;
    private LP nameCountrySku;
    private LP sumInCurrentYear;
    private LP sumInOutCurrentYear;
    private LP balanceSumCurrentYear;
    private AbstractCustomClass freightUnit;
    private LP quantityInvoiceFreightUnitSku;
    private LP freightSupplierBox;
    private LP freightFreightUnit;
    private LP priceInInvoiceFreightUnitSku;
    ConcreteCustomClass jennyferSupplier;
    ConcreteCustomClass tallyWeijlSupplier;
    ConcreteCustomClass hugoBossSupplier;
    ConcreteCustomClass mexxSupplier;
    ConcreteCustomClass bestsellerSupplier;
    ConcreteCustomClass sOliverSupplier;
    private LP jennyferImportInvoice;
    private LP jennyferImportArticleWeightInvoice;
    private LP tallyWeijlImportInvoice;
    private LP hugoBossImportInvoice;
    private LP mexxImportInvoice;
    private LP mexxImportPricesInvoice;
    private LP mexxImportArticleInfoInvoice;
    private LP mexxImportColorInvoice;
    private LP bestsellerImportInvoice;
    private LP hugoBossImportPricat;
    private LP sOliverImportInvoice;
    private AbstractGroup importInvoiceActionGroup;
    private LP printCreatePalletForm;
    private LP printCreateFreightBoxForm;
    private LP priceSupplierBoxSku;
    private LP sumSupplierBoxSku;
    private LP nameArticleSku;
    private LP freightShippedFreightBox;
    private LP freightShippedDirectInvoice;
    private LP quantityDirectInvoicedSku;
    private LP quantityStockedSku;
    private LP quantitySku;
    private LP sumInInvoiceStockSku;
    private LP sumStockedSku;
    private LP sumDirectInvoicedSku;
    private LP sumSku;
    private LP netWeightDocumentSku;
    private LP barcode10;
    private LP skuJennyferBarcode10;
    private LP jennyferSupplierArticle;
    private LP jennyferSupplierArticleSku;
    private LP substring10;
    private LP skuJennyferBarcode;
    private LP substring10s13;
    private LP skuBarcodeObject;

    ConcreteCustomClass pricat;
    LP barcodePricat;
    LP articleNumberPricat;
    LP customCategoryOriginalPricat;
    LP colorCodePricat;
    LP colorNamePricat;
    LP sizePricat;
    LP originalNamePricat;
    LP countryPricat;
    LP netWeightPricat;
    LP compositionPricat;
    LP pricePricat;
    LP rrpPricat;
    LP supplierPricat;
    LP barcodeToPricat;
    LP importPricatSupplier;

    private ConcreteCustomClass stamp;
    private ConcreteCustomClass creationStamp;
    LP sidStamp;
    LP dateOfStamp;
    LP seriesOfStamp;
    LP stampShipmentDetail;
    LP sidStampShipmentDetail;
    LP hideSidStampShipmentDetail;
    LP necessaryStampCategory;
    LP necessaryStampSkuShipmentDetail;
    LP shipmentDetailStamp;
    LP firstNumberCreationStamp;
    LP lastNumberCreationStamp;
    LP dateOfStampCreationStamp;
    LP seriesOfStampCreationStamp;
    LP createStamp;
    LP creationStampStamp;


    public AnnexInvoiceFormEntity invoiceFromFormEntity;

    @Override
    
    public void initClasses() {
        initBaseClassAliases();

        currency = addConcreteClass("currency", "Валюта", baseClass.named);

        typeExchange = addConcreteClass("typeExchange", "Тип обмена", baseClass.named);

        destination = addAbstractClass("destination", "Пункт назначения", baseClass);

        store = addConcreteClass("store", "Магазин", destination, baseClass.named);

        sku = addAbstractClass("sku", "SKU", baseLM.barcodeObject);

        article = addAbstractClass("article", "Артикул", baseClass);
        articleComposite = addConcreteClass("articleComposite", "Артикул (составной)", article);
        articleSingle = addConcreteClass("articleSingle", "Артикул (простой)", sku, article);

        pricat = addConcreteClass("pricat", "Прайс", baseClass);

        item = addConcreteClass("item", "Товар", sku);

        document = addAbstractClass("document", "Документ", baseLM.transaction);
        list = addAbstractClass("list", "Список", baseClass);

        contract = addConcreteClass("contract", "Договор", baseLM.transaction);

        priceDocument = addAbstractClass("priceDocument", "Документ с ценами", document);
        destinationDocument = addAbstractClass("destinationDocument", "Документ в пункт назначения", document);

        order = addConcreteClass("order", "Заказ", priceDocument, destinationDocument, list);

        invoice = addAbstractClass("invoice", "Инвойс", priceDocument, destinationDocument);
        boxInvoice = addConcreteClass("boxInvoice", "Инвойс по коробам", invoice);

        directInvoice = addAbstractClass("directInvoice", "Инвойс (напрямую)", invoice);
        directBoxInvoice = addConcreteClass("directBoxInvoice", "Инвойс по коробам (напрямую)", boxInvoice, directInvoice);

        simpleInvoice = addConcreteClass("simpleInvoice", "Инвойс без коробов", invoice, list);

        shipDimension = addConcreteClass("shipDimension", "Разрез поставки", baseClass);

        stock = addConcreteClass("stock", "Место хранения", baseLM.barcodeObject);

        freightUnit = addAbstractClass("freightUnit", "Машиноместо", baseClass);

        supplierBox = addConcreteClass("supplierBox", "Короб поставщика", list, shipDimension, baseLM.barcodeObject, freightUnit);

        shipment = addAbstractClass("shipment", "Поставка", document);
        boxShipment = addConcreteClass("boxShipment", "Поставка по коробам", shipment);
        simpleShipment = addConcreteClass("simpleShipment", "Поставка без коробов", shipment, shipDimension);

        shipmentDetail = addAbstractClass("shipmentDetail", "Строка поставки", baseClass);
        boxShipmentDetail = addConcreteClass("boxShipmentDetail", "Строка поставки по коробам", shipmentDetail);
        simpleShipmentDetail = addConcreteClass("simpleShipmentDetail", "Строка поставки без коробов", shipmentDetail);

        seller = addAbstractClass("seller", "Продавец", baseClass);

        supplier = addConcreteClass("supplier", "Поставщик", baseClass.named, seller);

        jennyferSupplier = addConcreteClass("jennyferSupplier", "Jennyfer", supplier);
        tallyWeijlSupplier = addConcreteClass("tallyWeijlSupplier", "Tally Weijl", supplier);
        hugoBossSupplier = addConcreteClass("hugoBossSupplier", "Hugo Boss", supplier);
        mexxSupplier = addConcreteClass("mexxSupplier", "Mexx", supplier);
        bestsellerSupplier = addConcreteClass("bestsellerSupplier", "Bestseller", supplier);
        sOliverSupplier = addConcreteClass("sOliverSupplier", "s.Oliver", supplier);

        secondNameClass = addAbstractClass("secondNameClass", "Класс со вторым именем", baseClass);

        subject = addAbstractClass("subject", "Субъект", baseClass.named, secondNameClass);
        importer = addConcreteClass("importer", "Импортер", subject);
        exporter = addConcreteClass("exporter", "Экспортер", subject, seller);

        commonSize = addConcreteClass("commonSize", "Унифицированный размер", baseClass.named);

        colorSupplier = addConcreteClass("colorSupplier", "Цвет поставщика", baseClass.named);
        sizeSupplier = addConcreteClass("sizeSupplier", "Размер поставщика", baseClass);

        freightBox = addConcreteClass("freightBox", "Короб для транспортировки", stock, freightUnit);

        freight = addConcreteClass("freight", "Фрахт", baseClass.named, baseLM.transaction);
        freightComplete = addConcreteClass("freightComplete", "Скомплектованный фрахт", freight);
        freightPriced = addConcreteClass("freightPriced", "Расцененный фрахт", freightComplete);
        freightChanged = addConcreteClass("freightChanged", "Обработанный фрахт", freightPriced);
        freightShipped = addConcreteClass("freightShipped", "Отгруженный фрахт", freightChanged);

        freightType = addConcreteClass("freightType", "Тип машины", baseClass.named);

        pallet = addConcreteClass("pallet", "Паллета", baseLM.barcodeObject);

        category = addConcreteClass("category", "Номенклатурная группа", secondNameClass, baseClass.named);

        customCategory = addAbstractClass("customCategory", "Уровень ТН ВЭД", baseClass);

        customCategory4 = addConcreteClass("customCategory4", "Первый уровень", customCategory);
        customCategory6 = addConcreteClass("customCategory6", "Второй уровень", customCategory);
        customCategory9 = addConcreteClass("customCategory9", "Третий уровень", customCategory);
        customCategory10 = addConcreteClass("customCategory10", "Четвёртый уровень", customCategory);

        customCategoryOrigin = addConcreteClass("customCategoryOrigin", "ЕС уровень", customCategory);

        creationFreightBox = addConcreteClass("creationFreightBox", "Операция создания коробов", baseLM.transaction);
        creationPallet = addConcreteClass("creationPallet", "Операция создания паллет", baseLM.transaction);
        creationStamp = addConcreteClass("creationStamp", "Операция создания марок", baseLM.transaction);

        transfer = addConcreteClass("transfer", "Внутреннее перемещение", baseClass);

        unitOfMeasure = addConcreteClass("unitOfMeasure", "Единица измерения", secondNameClass, baseClass.named);

        brandSupplier = addConcreteClass("brandSupplier", "Бренд поставщика", baseClass.named);

        themeSupplier = addConcreteClass("themeSupplier", "Тема поставщика", baseClass.named);

        countrySupplier = addConcreteClass("countrySupplier", "Страна поставщика", baseClass.named);

        season = addConcreteClass("season", "Сезон", baseClass.named);

        route = addStaticClass("route", "Маршрут", new String[]{"rb", "rf"}, new String[]{"РБ", "РФ"});

        stamp = addConcreteClass("stamp", "Контрольная марка", baseClass);
    }

    @Override
    public void initTables() {
        baseLM.tableFactory.include("customCategory4", customCategory4);
        baseLM.tableFactory.include("customCategory6", customCategory6);
        baseLM.tableFactory.include("customCategory9", customCategory9);
        baseLM.tableFactory.include("customCategory10", customCategory10);
        baseLM.tableFactory.include("customCategoryOrigin", customCategoryOrigin);
        baseLM.tableFactory.include("customCategory10Origin", customCategory10, customCategoryOrigin);
        baseLM.tableFactory.include("customCategory", customCategory);

        baseLM.tableFactory.include("article", article);
        baseLM.tableFactory.include("sku", sku);
        baseLM.tableFactory.include("documentArticle", document, article);
        baseLM.tableFactory.include("documentSku", document, sku);
        baseLM.tableFactory.include("listSku", list, sku);
        baseLM.tableFactory.include("listArticle", list, article);
        baseLM.tableFactory.include("importerFreightSku", importer, freight, sku);
        baseLM.tableFactory.include("freightSku", freight, sku);
        baseLM.tableFactory.include("shipmentDetail", shipmentDetail);
        baseLM.tableFactory.include("pallet", pallet);
        baseLM.tableFactory.include("freight", freight);
        baseLM.tableFactory.include("freightUnit", freightUnit);
        baseLM.tableFactory.include("barcodeObject", baseLM.barcodeObject);
        baseLM.tableFactory.include("rateExchange", typeExchange, currency, DateClass.instance);

        baseLM.tableFactory.include("pricat", pricat);
        baseLM.tableFactory.include("strings", StringClass.get(10));
    }

    @Override
    public void initGroups() {
        initBaseGroupAliases();
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
    public void initProperties() {
        idGroup.add(baseLM.objectValue);

        round2 = addSFProp("round(CAST((prm1) as numeric), 2)", DoubleClass.instance, 1);

        // rate
        currencyTypeExchange = addDProp(idGroup, "currencyTypeExchange", "Валюта типа обмена (ИД)", currency, typeExchange);
        nameCurrencyTypeExchange = addJProp(baseGroup, "nameCurrencyTypeExchange", "Валюта типа обмена (наим.)", baseLM.name, currencyTypeExchange, 1);
        rateExchange = addDProp(baseGroup, "rateExchange", "Курс обмена", DoubleClass.instance, typeExchange, currency, DateClass.instance);
        typeExchangeSTX = addDProp(idGroup, "typeExchangeSTX", "Тип обмена валют для STX (ИД)", typeExchange);
        nameTypeExchangeSTX = addJProp(baseGroup, "nameTypeExchangeSTX", "Тип обмена валют для STX", baseLM.name, typeExchangeSTX);

        //lessCmpDate = addJProp(and(false, true, false), object(DateClass.instance), 3, rateExchange, 1, 2, 3, greater2, 3, 4, is(DateClass.instance), 4);
        lessCmpDate = addJProp(and(false, true, false), object(DateClass.instance), 3, rateExchange, 1, 2, 3, addJProp(baseLM.greater2, 3, baseLM.date, 4), 1, 2, 3, 4, is(document), 4);
        nearestPredDate = addMGProp((AbstractGroup) null, "nearestPredDate", "Ближайшая меньшая дата", lessCmpDate, 1, 2, 4);
        nearestRateExchange = addJProp("Ближайший курс обмена", rateExchange, 1, 2, nearestPredDate, 1, 2, 3);

        nameOrigin = addDProp(baseGroup, "nameOrigin", "Наименование (ориг.)", InsensitiveStringClass.get(50), secondNameClass);
        nameOriginCountry = addDProp(baseGroup, "nameOriginCountry", "Наименование (ориг.)", InsensitiveStringClass.get(50), baseLM.country);

        sidOrigin2Country = addDProp(baseGroup, "sidOrigin2Country", "Код 2 знака (ориг.)", StringClass.get(2), baseLM.country);
        sidOrigin3Country = addDProp(baseGroup, "sidOrigin3Country", "Код 3 знака (ориг.)", StringClass.get(3), baseLM.country);
        sid3Country = addDProp(baseGroup, "sid3Country", "Код 3 знака", StringClass.get(3), baseLM.country);

        dictionaryComposition = addDProp(idGroup, "dictionaryComposition", "Словарь для составов (ИД)", baseLM.dictionary);
        nameDictionaryComposition = addJProp(baseGroup, "nameDictionaryComposition", "Словарь для составов", baseLM.name, dictionaryComposition);

        sidDestination = addDProp(baseGroup, "sidDestination", "Код", StringClass.get(50), destination);

        destinationSID = addAGProp(idGroup, "destinationSID", "Магазин (ИД)", sidDestination);

        sidBrandSupplier = addDProp(baseGroup, "sidBrandSupplier", "Код", StringClass.get(50), brandSupplier);

        // Contract
        sidContract = addDProp(baseGroup, "sidContract", "Номер договора", StringClass.get(50), contract);

        importerContract = addDProp(idGroup, "importerContract", "Импортер (ИД)", importer, contract);
        nameImporterContract = addJProp(baseGroup, "nameImporterContract", "Импортер", baseLM.name, importerContract, 1);

        sellerContract = addDProp(idGroup, "sellerContract", "Продавец (ИД)", seller, contract);
        nameSellerContract = addJProp(baseGroup, "nameSellerContract", "Продавец", baseLM.name, sellerContract, 1);

        currencyContract = addDProp(idGroup, "currencyContract", "Валюта (ИД)", currency, contract);
        nameCurrencyContract = addJProp(baseGroup, "nameCurrencyContract", "Валюта", baseLM.name, currencyContract, 1);

        // Subject
        addressOriginSubject = addDProp(baseGroup, "addressOriginSubject", "Address", StringClass.get(200), subject);
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

        sidToCustomCategory4 = addAGProp("sidToCustomCategory4", "Код(4)", sidCustomCategory4);
        sidToCustomCategory6 = addAGProp("sidToCustomCategory6", "Код(6)", sidCustomCategory6);
        sidToCustomCategory9 = addAGProp("sidToCustomCategory9", "Код(9)", sidCustomCategory9);
        sidToCustomCategory10 = addAGProp("sidToCustomCategory10", "Код(10)", sidCustomCategory10);
        sidToCustomCategoryOrigin = addAGProp("sidToCustomCategoryOrigin", "Код ЕС (10)", sidCustomCategoryOrigin);

        importBelTnved = addAProp(new ClassifierTNVEDImportActionProperty(genSID(), "Импортировать (РБ)", this, "belarusian"));
        importEuTnved = addAProp(new ClassifierTNVEDImportActionProperty(genSID(), "Импортировать (ЕС)", this, "origin"));
        jennyferImportInvoice = addAProp(importInvoiceActionGroup, new JennyferImportInvoiceActionProperty(this));
        tallyWeijlImportInvoice = addAProp(importInvoiceActionGroup, new TallyWeijlImportInvoiceActionProperty(this));
        hugoBossImportInvoice = addAProp(importInvoiceActionGroup, new HugoBossImportInvoiceActionProperty(BL));
        mexxImportInvoice = addAProp(importInvoiceActionGroup, new MexxImportInvoiceActionProperty(this));
        mexxImportPricesInvoice = addAProp(importInvoiceActionGroup, new MexxImportPricesInvoiceActionProperty(this));
        mexxImportArticleInfoInvoice = addAProp(importInvoiceActionGroup, new MexxImportArticleInfoInvoiceActionProperty(this));
        mexxImportColorInvoice = addAProp(importInvoiceActionGroup, new MexxImportColorInvoiceActionProperty(this));
        bestsellerImportInvoice = addAProp(importInvoiceActionGroup, new BestsellerImportInvoiceActionProperty(BL));
        sOliverImportInvoice = addAProp(importInvoiceActionGroup, new SOliverImportInvoiceActionProperty(BL));

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
        nameCurrencySupplier = addJProp(baseGroup, "nameCurrencySupplier", "Валюта", baseLM.name, currencySupplier, 1);

        sidColorSupplier = addDProp(baseGroup, "sidColorSupplier", "Код", StringClass.get(50), colorSupplier);

        supplierColorSupplier = addDProp(idGroup, "supplierColorSupplier", "Поставщик (ИД)", supplier, colorSupplier);
        nameSupplierColorSupplier = addJProp(baseGroup, "nameSupplierColorSupplier", "Поставщик", baseLM.name, supplierColorSupplier, 1);

        colorSIDSupplier = addAGProp(idGroup, "colorSIDSupplier", "Цвет поставщика (ИД)", sidColorSupplier, supplierColorSupplier);

        sidSizeSupplier = addDProp(baseGroup, "sidSizeSupplier", "Код", StringClass.get(50), sizeSupplier);

        commonSizeSizeSupplier = addDProp(idGroup, "commonSizeSizeSupplier", "Унифицированный размер (ИД)", commonSize, sizeSupplier);
        nameCommonSizeSizeSupplier = addJProp(baseGroup, "nameCommonSizeSizeSupplier", "Унифицированный размер", baseLM.name, commonSizeSizeSupplier, 1);

        supplierSizeSupplier = addDProp(idGroup, "supplierSizeSupplier", "Поставщик (ИД)", supplier, sizeSupplier);
        nameSupplierSizeSupplier = addJProp(baseGroup, "nameSupplierSizeSupplier", "Поставщик", baseLM.name, supplierSizeSupplier, 1);

        sizeSIDSupplier = addAGProp(idGroup, "sizeSIDSupplier", "Размер поставщика (ИД)", sidSizeSupplier, supplierSizeSupplier);

        // Country
        supplierCountrySupplier = addDProp(idGroup, "supplierCountrySupplier", "Поставщик (ИД)", supplier, countrySupplier);
        nameSupplierCountrySupplier = addJProp(baseGroup, "nameSupplierCountrySupplier", "Поставщик", baseLM.name, supplierCountrySupplier, 1);

        countryCountrySupplier = addDProp(idGroup, "countryCountrySupplier", "Страна (ИД)", baseLM.country, countrySupplier);
        nameCountryCountrySupplier = addJProp(baseGroup, "nameCountryCountrySupplier", "Страна", baseLM.name, countryCountrySupplier, 1);

        countryNameSupplier = addAGProp(idGroup, "countryNameSupplier", "Страна поставщика", baseLM.name, supplierCountrySupplier);

        // Brand
        supplierBrandSupplier = addDProp(idGroup, "supplierBrandSupplier", "Поставщик (ИД)", supplier, brandSupplier);
        nameSupplierBrandSupplier = addJProp(baseGroup, "nameSupplierBrandSupplier", "Поставщик", baseLM.name, supplierBrandSupplier, 1);

        brandSIDSupplier = addAGProp(idGroup, "brandSIDSupplier", "Бренд поставщика (ИД)", sidBrandSupplier, supplierBrandSupplier);

        brandSupplierSupplier = addDProp(idGroup, "brandSupplierSupplier", "Бренд (ИД)", brandSupplier, supplier);
        nameBrandSupplierSupplier = addJProp(baseGroup, "nameBrandSupplierSupplier", "Бренд по умолчанию", baseLM.name, brandSupplierSupplier, 1);

        addConstraint(addJProp("Бренд по умолчанию для поставщика должен соответствовать брендам поставщика", baseLM.diff2, 1, addJProp(supplierBrandSupplier, brandSupplierSupplier, 1), 1), true);

        supplierThemeSupplier = addDProp(idGroup, "supplierThemeSupplier", "Поставщик (ИД)", supplier, themeSupplier);

        supplierDocument = addDProp(idGroup, "supplierDocument", "Поставщик (ИД)", supplier, document);
        supplierPriceDocument = addJProp(idGroup, "supplierPricedDocument", "Поставщик(ИД)", baseLM.and1, supplierDocument, 1, is(priceDocument), 1);
        nameSupplierDocument = addJProp(baseGroup, "nameSupplierDocument", "Поставщик", baseLM.name, supplierDocument, 1);

        currencyDocument = addDCProp(idGroup, "currencyDocument", "Валюта (ИД)", currencySupplier, supplierPriceDocument, 1);
        nameCurrencyDocument = addJProp(baseGroup, "nameCurrencyDocument", "Валюта", baseLM.name, currencyDocument, 1);

        // Order
        destinationDestinationDocument = addDProp(idGroup, "destinationDestinationDocument", "Пункт назначения (ИД)", destination, destinationDocument);
        nameDestinationDestinationDocument = addJProp(baseGroup, "nameDestinationDestinationDocument", "Пункт назначения (наим.)", baseLM.name, destinationDestinationDocument, 1);
        sidDestinationDestinationDocument = addJProp(baseGroup, "sidDestinationDestinationDocument", "Пункт назначения", sidDestination, destinationDestinationDocument, 1);

        // Invoice
        importerInvoice = addDProp(idGroup, "importerDocument", "Импортер (ИД)", importer, invoice);
        nameImporterInvoice = addJProp(baseGroup, "nameImporterInvoice", "Импортер", baseLM.name, importerInvoice, 1);

        contractInvoice = addDProp(idGroup, "contractInvoice", "Договор (ИД)", contract, invoice);
        sidContractInvoice = addJProp(baseGroup, "sidContractInvoice", "Договор", sidContract, contractInvoice, 1);

        addConstraint(addJProp("Импортер договора должен соответствовать импортеру инвойса", baseLM.diff2,
                importerInvoice, 1, addJProp(importerContract, contractInvoice, 1), 1), true);

        addConstraint(addJProp("Поставщик договора должен соответствовать поставщику инвойса", baseLM.diff2,
                supplierDocument, 1, addJProp(sellerContract, contractInvoice, 1), 1), true);


        // Shipment
        quantityPalletShipment = addDProp(baseGroup, "quantityPalletShipment", "Кол-во паллет", IntegerClass.instance, shipment);
        netWeightShipment = addDProp(baseGroup, "netWeightShipment", "Вес нетто", DoubleClass.instance, shipment);
        grossWeightShipment = addDProp(baseGroup, "grossWeightShipment", "Вес брутто", DoubleClass.instance, shipment);

        grossWeightPallet = addDProp(baseGroup, "grossWeightPallet", "Вес брутто", DoubleClass.instance, pallet);
        quantityBoxShipment = addDProp(baseGroup, "quantityBoxShipment", "Кол-во коробов", DoubleClass.instance, shipment);

        // Item
        articleCompositeItem = addDProp(idGroup, "articleCompositeItem", "Артикул (ИД)", articleComposite, item);
        equalsItemArticleComposite = addJProp(baseGroup, "equalsItemArticleComposite", "Вкл.", baseLM.equals2, articleCompositeItem, 1, 2);

        articleSku = addCUProp(idGroup, "articleSku", "Артикул (ИД)", object(articleSingle), articleCompositeItem);

        addItemBarcode = addJProp(true, "Ввод товара по штрих-коду", addAAProp(item, baseLM.barcode), 1);

        // Article
        sidArticle = addDProp(baseGroup, "sidArticle", "Артикул", StringClass.get(50), article);
        sidArticleSku = addJProp(supplierAttributeGroup, "sidArticleSku", "Артикул", sidArticle, articleSku, 1);

        originalNameArticle = addDProp(supplierAttributeGroup, "originalNameArticle", "Наименование (ориг.)", InsensitiveStringClass.get(50), article);
        originalNameArticleSku = addJProp(supplierAttributeGroup, "originalNameArticleSku", "Наименование (ориг.)", originalNameArticle, articleSku, 1);

        //Category
        categoryArticle = addDProp(idGroup, "categoryArticle", "Номенклатурная группа товара (ИД)", category, article);
        nameOriginCategoryArticle = addJProp(intraAttributeGroup, "nameOriginCategoryArticle", "Номенклатурная группа товара (ориг.)", nameOrigin, categoryArticle, 1);
        nameCategoryArticle = addJProp(intraAttributeGroup, "nameCategoryArticle", "Номенклатурная группа товара", baseLM.name, categoryArticle, 1);
        categoryArticleSku = addJProp(idGroup, true, "categoryArticleSku", "Номенклатурная группа товара (ИД)", categoryArticle, articleSku, 1);
        nameCategoryArticleSku = addJProp(intraAttributeGroup, "nameCategoryArticleSku", "Номенклатурная группа товара", baseLM.name, categoryArticleSku, 1);
        nameOriginCategoryArticleSku = addJProp(intraAttributeGroup, "nameOriginCategoryArticleSku", "Номенклатурная группа товара", nameOrigin, categoryArticleSku, 1);

        nameArticle = addSUProp(baseGroup, "nameArticle", "Наименование", Union.OVERRIDE, originalNameArticle, nameOriginCategoryArticle);
        nameArticleSku = addJProp(intraAttributeGroup, "nameArticleSku", "Наименование", nameArticle, articleSku, 1);

        customCategoryOriginArticle = addDProp(idGroup, "customCategoryOriginArticle", "ТН ВЭД (ориг.) (ИД)", customCategoryOrigin, article);
        sidCustomCategoryOriginArticle = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticle", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategoryOriginArticleSku = addJProp(idGroup, true, "customCategoryOriginArticleSku", "ТН ВЭД (ориг.) (ИД)", customCategoryOriginArticle, articleSku, 1);
        sidCustomCategoryOriginArticleSku = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticleSku", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticleSku, 1);

        customCategory10DataSku = addDProp(idGroup, "customCategory10DataSku", "ТН ВЭД (ИД)", customCategory10, sku);
        customCategory10CustomCategoryOriginArticle = addJProp(idGroup, "customCategory10CustomCategoryOriginArticle", "ТН ВЭД (ИД)", customCategory10CustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategory10CustomCategoryOriginArticleSku = addJProp(idGroup, "customCategory10CustomCategoryOriginArticleSku", "ТН ВЭД (ИД)", customCategory10CustomCategoryOriginArticle, articleSku, 1);
        customCategory10Sku = addSUProp(idGroup, "customCategory10Sku", true, "ТН ВЭД (ИД)", Union.OVERRIDE, customCategory10CustomCategoryOriginArticleSku, customCategory10DataSku);
        sidCustomCategory10Sku = addJProp(baseGroup, "sidCustomCategory10Sku", "ТН ВЭД", sidCustomCategory10, customCategory10Sku, 1);
        /*addConstraint(addJProp("Выбранный должен быть среди связанных кодов", andNot1, addCProp(LogicalClass.instance, true, article), 1,
                   addJProp(relationCustomCategory10CustomCategoryOrigin, customCategory10Article, 1, customCategoryOriginArticle, 1), 1), true);*/

        // unitOfMeasure
        unitOfMeasureCategory = addDProp(idGroup, "unitOfMeasureCategory", "Единица измерения (ИД)", unitOfMeasure, category);
        nameUnitOfMeasureCategory = addJProp(baseGroup, "nameUnitOfMeasureCategory", "Единица измерения", baseLM.name, unitOfMeasureCategory, 1);
        unitOfMeasureCategoryArticle = addJProp(idGroup, "unitOfMeasureCategoryArticle", "Единица измерения (ИД)", unitOfMeasureCategory, categoryArticle, 1);
        unitOfMeasureDataArticle = addDProp(idGroup, "unitOfMeasureDataArticle", "Единица измерения (ИД)", unitOfMeasure, article);
        unitOfMeasureArticle = addSUProp(idGroup, "unitOfMeasureArticle", "Единица измерения", Union.OVERRIDE, unitOfMeasureCategoryArticle, unitOfMeasureDataArticle);

        nameOriginUnitOfMeasureArticle = addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticle", "Единица измерения (ориг.)", nameOrigin, unitOfMeasureArticle, 1);
        nameUnitOfMeasureArticle = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticle", "Единица измерения", baseLM.name, unitOfMeasureArticle, 1);
        unitOfMeasureArticleSku = addJProp(idGroup, true, "unitOfMeasureArticleSku", "Ед. изм. товара (ИД)", unitOfMeasureArticle, articleSku, 1);
        nameUnitOfMeasureArticleSku = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticleSku", "Ед. изм. товара", baseLM.name, unitOfMeasureArticleSku, 1);
        nameOriginUnitOfMeasureArticleSku = addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticleSku", "Ед. изм. товара", nameOrigin, unitOfMeasureArticleSku, 1);

        // Supplier
        supplierArticle = addDProp(idGroup, "supplierArticle", "Поставщик (ИД)", supplier, article);
        nameSupplierArticle = addJProp(baseGroup, "nameSupplierArticle", "Поставщик", baseLM.name, supplierArticle, 1);

        jennyferSupplierArticle = addJProp("jennyferSupplierArticle", "Поставщик Jennyfer (ИД)", baseLM.and1, supplierArticle, 1, addJProp(is(jennyferSupplier), supplierArticle, 1), 1);

        brandSupplierDataArticle = addDProp(idGroup, "brandSupplierDataArticle", "Бренд (ИД)", brandSupplier, article);
        brandSupplierSupplierArticle = addJProp(idGroup, "brandSupplierSupplierArticle", "Бренд (ИД)", brandSupplierSupplier, supplierArticle, 1);
        brandSupplierArticle = addSUProp(idGroup, "brandSupplierArticle", "Бренд (ИД)", Union.OVERRIDE, brandSupplierSupplierArticle, brandSupplierDataArticle);
        nameBrandSupplierArticle = addJProp(supplierAttributeGroup, "nameBrandSupplierArticle", "Бренд", baseLM.name, brandSupplierArticle, 1);
        sidBrandSupplierArticle = addJProp(supplierAttributeGroup, "sidBrandSupplierArticle", "Бренд (ИД)", sidBrandSupplier, brandSupplierArticle, 1);

        supplierBrandSupplierArticle = addJProp(idGroup, "supplierBrandSupplierArticle", "Поставщик", supplierBrandSupplier, brandSupplierArticle, 1);
        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику бренда артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierBrandSupplier, brandSupplierArticle, 1), 1), true);

        brandSupplierArticleSku = addJProp(idGroup, "brandSupplierArticleSku", "Бренд (ИД)", brandSupplierArticle, articleSku, 1);
        nameBrandSupplierArticleSku = addJProp(supplierAttributeGroup, "nameBrandSupplierArticleSku", "Бренд", baseLM.name, brandSupplierArticleSku, 1);
        sidBrandSupplierArticleSku = addJProp(supplierAttributeGroup, "sidBrandSupplierArticleSku", "Бренд(ИД)", sidBrandSupplier, brandSupplierArticleSku, 1);

        themeSupplierArticle = addDProp(idGroup, "themeSupplierDataArticle", "Тема (ИД)", themeSupplier, article);
        nameThemeSupplierArticle = addJProp(supplierAttributeGroup, "nameThmeSupplierArticle", "Тема", baseLM.name, themeSupplierArticle, 1);

        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику темы артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierThemeSupplier, themeSupplierArticle, 1), 1), true);

        themeSupplierArticleSku = addJProp(idGroup, "themeSupplierArticleSku", "Тема (ИД)", themeSupplierArticle, articleSku, 1);
        nameThemeSupplierArticleSku = addJProp(supplierAttributeGroup, "nameThemeSupplierArticleSku", "Тема", baseLM.name, themeSupplierArticleSku, 1);

        seasonArticle = addDProp(idGroup, "seasonArticle", "Сезон (ИД)", season, article);
        nameSeasonArticle = addJProp(supplierAttributeGroup, "nameSeasonArticle", "Сезон", baseLM.name, seasonArticle, 1);

        articleSIDSupplier = addAGProp(idGroup, "articleSIDSupplier", "Артикул (ИД)", sidArticle, supplierArticle);

        seekArticleSIDSupplier = addJProp(true, "Поиск артикула", addSAProp(null), articleSIDSupplier, 1, 2);

        addArticleSingleSIDSupplier = addJProp(true, "Ввод простого артикула", addAAProp(articleSingle, sidArticle, supplierArticle), 1, 2);
        addNEArticleSingleSIDSupplier = addJProp(true, "Ввод простого артикула (НС)", baseLM.andNot1, addArticleSingleSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);

        addArticleCompositeSIDSupplier = addJProp(true, "Ввод составного артикула", addAAProp(articleComposite, sidArticle, supplierArticle), 1, 2);
        addNEArticleCompositeSIDSupplier = addJProp(true, "Ввод составного артикула (НС)", baseLM.andNot1, addArticleCompositeSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);

        executeArticleCompositeItemSIDSupplier = addJProp(true, "Замена артикула", addEPAProp(articleCompositeItem), 1, articleSIDSupplier, 2, 3);

        executeChangeFreightClass = addJProp(true, "Изменить класс фрахта", baseLM.and1, addEPAProp(baseLM.objectClass), 1, 2, is(freight), 1);

        supplierArticleSku = addJProp(idGroup, "supplierArticleSku", "Поставщик (ИД)", supplierArticle, articleSku, 1);
        nameSupplierArticleSku = addJProp(baseGroup, "nameSupplierArticleSku", "Поставщик", baseLM.name, supplierArticleSku, 1);

        jennyferSupplierArticleSku = addJProp("jennyferSupplierArticleSku", "Поставщик Jennyfer (ИД)", jennyferSupplierArticle, articleSku, 1);

        colorSupplierItem = addDProp(idGroup, "colorSupplierItem", "Цвет поставщика (ИД)", colorSupplier, item);
        sidColorSupplierItem = addJProp(itemAttributeGroup, "sidColorSupplierItem", "Код цвета", sidColorSupplier, colorSupplierItem, 1);
        nameColorSupplierItem = addJProp(itemAttributeGroup, "nameColorSupplierItem", "Цвет поставщика", baseLM.name, colorSupplierItem, 1);

        sizeSupplierItem = addDProp(itemAttributeGroup, "sizeSupplierItem", "Размер поставщика (ИД)", sizeSupplier, item);
        sidSizeSupplierItem = addJProp(itemAttributeGroup, "sidSizeSupplierItem", "Размер поставщика", sidSizeSupplier, sizeSupplierItem, 1);

        addConstraint(addJProp("Поставщик товара должен соответствовать цвету поставщика", baseLM.diff2,
                supplierArticleSku, 1,
                addJProp(supplierColorSupplier, colorSupplierItem, 1), 1), true);

        addConstraint(addJProp("Поставщик товара должен соответствовать размеру поставщика", baseLM.diff2,
                supplierArticleSku, 1,
                addJProp(supplierSizeSupplier, sizeSupplierItem, 1), 1), true);

        // Weight
        netWeightArticle = addDProp(supplierAttributeGroup, "netWeightArticle", "Вес нетто (ориг.)", DoubleClass.instance, article);
        netWeightArticleSku = addJProp(intraAttributeGroup, "netWeightArticleSku", "Вес нетто (ориг.)", netWeightArticle, articleSku, 1);
        netWeightArticleSize = addDProp(intraAttributeGroup, "netWeightArticleSize", "Вес нетто размера", DoubleClass.instance, article, sizeSupplier);

        netWeightDataSku = addDProp(intraAttributeGroup, "netWeightDataSku", "Вес нетто", DoubleClass.instance, sku);
        netWeightArticleSizeSku = addJProp(intraAttributeGroup, true, "netWeightArticleSizeSku", "Вес нетто", netWeightArticleSize, articleSku, 1, sizeSupplierItem, 1);
        netWeightSku = addSUProp(intraAttributeGroup, "netWeightSku", "Вес нетто единицы товара", Union.OVERRIDE, netWeightArticleSku, netWeightArticleSizeSku);

        // Country
        countrySupplierOfOriginArticle = addDProp(idGroup, "countrySupplierOfOriginArticle", "Страна происхождения (ИД)", countrySupplier, article);
        nameCountrySupplierOfOriginArticle = addJProp(supplierAttributeGroup, "nameCountrySupplierOfOriginArticle", "Страна происхождения (ориг.)", baseLM.name, countrySupplierOfOriginArticle, 1);

        countrySupplierOfOriginArticleSku = addJProp(idGroup, "countrySupplierOfOriginArticleSku", "Страна происхождения (ИД)", countrySupplierOfOriginArticle, articleSku, 1);
        nameCountrySupplierOfOriginArticleSku = addJProp(supplierAttributeGroup, "nameCountrySupplierOfOriginArticleSku", "Страна происхождения (ориг.)", baseLM.name, countrySupplierOfOriginArticleSku, 1);

        countryOfOriginArticle = addJProp(idGroup, "countryOfOriginArticle", "Страна происхождения (ИД)", countryCountrySupplier, countrySupplierOfOriginArticle, 1);
        nameCountryOfOriginArticle = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticle", "Страна происхождения", nameOriginCountry, countryOfOriginArticle, 1);

        countryOfOriginArticleSku = addJProp(idGroup, "countryOfOriginArticleSku", "Страна происхождения (ИД)", countryOfOriginArticle, articleSku, 1);
        nameCountryOfOriginArticleSku = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSku", "Страна происхождения", nameOriginCountry, countryOfOriginArticleSku, 1);

        countryOfOriginArticleColor = addDProp(idGroup, "countryOfOriginArticleColor", "Страна происхождения (ИД)", baseLM.country, article, colorSupplier);
        countryOfOriginArticleColorSku = addJProp(idGroup, true, "countryOfOriginArticleColorSku", "Страна происхождения (ИД)", countryOfOriginArticleColor, articleSku, 1, colorSupplierItem, 1);

        countryOfOriginDataSku = addDProp(idGroup, "countryOfOriginDataSku", "Страна происхождения (ИД) (первичное)", baseLM.country, sku);

        countryOfOriginSku = addSUProp(idGroup, "countryOfOriginSku", true, "Страна происхождения (ИД)", Union.OVERRIDE, countryOfOriginArticleSku, countryOfOriginArticleColorSku);

        nameCountryOfOriginSku = addJProp(intraAttributeGroup, "nameCountryOfOriginSku", "Страна происхождения", nameOriginCountry, countryOfOriginSku, 1);
        nameCountrySku = addJProp(intraAttributeGroup, "nameCountrySku", "Страна происхождения", baseLM.name, countryOfOriginSku, 1);

        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику страны артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierCountrySupplier, countrySupplierOfOriginArticle, 1), 1), true);

        // Composition
        mainCompositionOriginArticle = addDProp(supplierAttributeGroup, "mainCompositionOriginArticle", "Состав", COMPOSITION_CLASS, article);
        additionalCompositionOriginArticle = addDProp(supplierAttributeGroup, "additionalCompositionOriginArticle", "Доп. состав", COMPOSITION_CLASS, article);

        mainCompositionOriginArticleSku = addJProp(supplierAttributeGroup, "mainCompositionOriginArticleSku", "Состав", mainCompositionOriginArticle, articleSku, 1);
        additionalCompositionOriginArticleSku = addJProp(supplierAttributeGroup, "additionalCompositionOriginArticleSku", "Доп. состав", additionalCompositionOriginArticle, articleSku, 1);

        mainCompositionOriginArticleColor = addDProp(supplierAttributeGroup, "mainCompositionOriginArticleColor", "Состав", COMPOSITION_CLASS, article, colorSupplier);
        additionalCompositionOriginArticleColor = addDProp(supplierAttributeGroup, "additionalCompositionOriginArticleColor", "Доп. состав", COMPOSITION_CLASS, article, colorSupplier);

        mainCompositionOriginArticleColorSku = addJProp(supplierAttributeGroup, true, "mainCompositionOriginArticleColorSku", "Состав", mainCompositionOriginArticleColor, articleSku, 1, colorSupplierItem, 1);
        additionalCompositionOriginArticleColorSku = addJProp(supplierAttributeGroup, true, "additionalCompositionOriginArticleColorSku", "Доп. состав", additionalCompositionOriginArticleColor, articleSku, 1, colorSupplierItem, 1);

        mainCompositionOriginDataSku = addDProp(intraAttributeGroup, "mainCompositionOriginDataSku", "Состав", COMPOSITION_CLASS, sku);
        additionalCompositionOriginDataSku = addDProp(intraAttributeGroup, "additionalCompositionOriginDataSku", "Доп. состав", COMPOSITION_CLASS, sku);

        mainCompositionOriginSku = addSUProp(intraAttributeGroup, "mainCompositionOriginSku", true, "Состав", Union.OVERRIDE, mainCompositionOriginArticleSku, mainCompositionOriginArticleColorSku);
        additionalCompositionOriginSku = addSUProp(intraAttributeGroup, "additionalCompositionOriginSku", "Доп. состав", Union.OVERRIDE, additionalCompositionOriginArticleSku, additionalCompositionOriginArticleColorSku);

        mainCompositionArticle = addDProp(intraAttributeGroup, "mainCompositionArticle", "Состав (перевод)", COMPOSITION_CLASS, article);
        additionalCompositionArticle = addDProp(intraAttributeGroup, "additionalCompositionArticle", "Доп. состав (перевод)", COMPOSITION_CLASS, article);

        mainCompositionSku = addDProp(intraAttributeGroup, "mainCompositionSku", "Состав (перевод)", COMPOSITION_CLASS, sku);
        additionalCompositionSku = addDProp(intraAttributeGroup, "additionalCompositionSku", "Доп. состав (перевод)", COMPOSITION_CLASS, sku);

        substring10 = addSFProp("substring(prm1,1,10)", StringClass.get(10), 1);
        substring10s13 = addJProp(baseLM.and1, substring10, 1, is(StringClass.get(13)), 1);

        barcode10 = addJProp("barcode10", "Штрих-код(10)", substring10, baseLM.barcode, 1);
        skuJennyferBarcode10 = addMGProp("skuJennyferBarcode10", "Товар (ИД)", addJProp(baseLM.and1, object(sku), 1, addJProp(is(jennyferSupplier), supplierArticleSku, 1), 1),
                barcode10, 1);
        skuJennyferBarcode = addJProp("skuJennyferBarcode", "Товар (ИД)", skuJennyferBarcode10, substring10s13, 1);

        skuBarcodeObject = addSUProp(Union.OVERRIDE, baseLM.barcodeToObject, skuJennyferBarcode);

        sidDocument = addDProp(baseGroup, "sidDocument", "Код документа", StringClass.get(50), document);
        documentSIDSupplier = addAGProp(idGroup, "documentSIDSupplier", "Документ поставщика (ИД)", sidDocument, supplierDocument);

        // коробки
        sidSupplierBox = addDProp(baseGroup, "sidSupplierBox", "Номер короба", StringClass.get(50), supplierBox);

        boxInvoiceSupplierBox = addDProp(idGroup, "boxInvoiceSupplierBox", "Документ по коробам (ИД)", boxInvoice, supplierBox);
        setNotNull(boxInvoiceSupplierBox);

        sidBoxInvoiceSupplierBox = addJProp(baseGroup, "sidBoxInvoiceSupplierBox", "Документ по коробам", sidDocument, boxInvoiceSupplierBox, 1);

        destinationSupplierBox = addJProp(idGroup, "destinationSupplierBox", "Пункт назначения (ИД)", destinationDestinationDocument, boxInvoiceSupplierBox, 1);
        nameDestinationSupplierBox = addJProp(baseGroup, "nameDestinationSupplierBox", "Пункт назначения", baseLM.name, destinationSupplierBox, 1);

        supplierSupplierBox = addJProp(idGroup, "supplierSupplierBox", "Поставщик (ИД)", supplierDocument, boxInvoiceSupplierBox, 1);

        supplierBoxSIDSupplier = addAGProp(idGroup, "supplierBoxSIDSupplier", "Короб поставщика (ИД)", sidSupplierBox, supplierSupplierBox);

        seekSupplierBoxSIDSupplier = addJProp(true, "Поиск короба поставщика", addSAProp(null), supplierBoxSIDSupplier, 1, 2);

        // заказ по артикулам
        documentList = addCUProp(idGroup, "documentList", "Документ (ИД)", object(order), object(simpleInvoice), boxInvoiceSupplierBox);
        supplierList = addJProp(idGroup, "supplierList", "Поставщик (ИД)", supplierDocument, documentList, 1);

        articleSIDList = addJProp(idGroup, "articleSIDList", "Артикул (ИД)", articleSIDSupplier, 1, supplierList, 2);

        numberListArticle = addDProp(baseGroup, "numberListArticle", "Номер", IntegerClass.instance, list, article);
        numberListSIDArticle = addJProp(numberListArticle, 1, articleSIDList, 2, 1);

        numberDataListSku = addDProp(baseGroup, "numberDataListSku", "Номер", IntegerClass.instance, list, sku);
        numberArticleListSku = addJProp(baseGroup, "numberArticleListSku", "Номер (артикула)", numberListArticle, 1, articleSku, 2);

        numberListSku = addSUProp("numberListSku", "Номер", Union.OVERRIDE, numberArticleListSku, numberDataListSku);

        numberDocumentArticle = addSGProp(baseGroup, "inDocumentArticle", numberListArticle, documentList, 1, 2);

        incrementNumberListSID = addJProp(true, "Добавить строку", baseLM.andNot1,
                addJProp(true, addIAProp(numberListArticle, 1),
                        1, articleSIDList, 2, 1), 1, 2,
                numberListSIDArticle, 1, 2); // если еще не было добавлено такой строки

        //price and catalog (pricat)
        barcodePricat = addDProp(baseGroup, "barcodePricat", "Штрих-код", StringClass.get(13), pricat);
        articleNumberPricat = addDProp(baseGroup, "articleNumberPricat", "Артикул", StringClass.get(20), pricat);
        customCategoryOriginalPricat = addDProp(baseGroup, "customCategoryOriginalPricat", "Код ЕС (10)", StringClass.get(10), pricat);
        colorCodePricat = addDProp(baseGroup, "colorCodePricat", "Код цвета", StringClass.get(20), pricat);
        colorNamePricat = addDProp(baseGroup, "colorNamePricat", "Цвет", StringClass.get(50), pricat);
        sizePricat = addDProp(baseGroup, "sizePricat", "Размер", StringClass.get(5), pricat);
        originalNamePricat = addDProp(baseGroup, "originalNamePricat", "Наименование (ориг.)", StringClass.get(50), pricat);
        countryPricat = addDProp(baseGroup, "countryPricat", "Страна происхождения", StringClass.get(20), pricat);
        netWeightPricat = addDProp(baseGroup, "netWeightPricat", "Вес нетто", DoubleClass.instance, pricat);
        compositionPricat = addDProp(baseGroup, "compositionPricat", "Состав", StringClass.get(50), pricat);
        pricePricat = addDProp(baseGroup, "pricePricat", "Цена", DoubleClass.instance, pricat);
        rrpPricat = addDProp(baseGroup, "RRP", "Рекомендованная цена", DoubleClass.instance, pricat);
        supplierPricat = addDProp("supplierPricat", "Поставщик", supplier, pricat);
        barcodeToPricat = addAGProp("barcodeToPricat", "штрих-код", barcodePricat);
        importPricatSupplier = addProperty(null, new LP<ClassPropertyInterface>(new PricatEDIImportActionProperty(genSID(), this, supplier)));
        hugoBossImportPricat = addProperty(null, new LP<ClassPropertyInterface>(new HugoBossPricatCSVImportActionProperty(genSID(), this, hugoBossSupplier)));

        // кол-во заказа
        quantityDataListSku = addDProp("quantityDataListSku", "Кол-во (первичное)", DoubleClass.instance, list, sku);
        quantityListSku = quantityDataListSku; //addJProp(baseGroup, "quantityListSku", true, "Кол-во", baseLM.and1, quantityDataListSku, 1, 2, numberListSku, 1, 2);

        quantityDocumentSku = addSGProp(baseGroup, "quantityDocumentSku", true, "Кол-во в документе", quantityListSku, documentList, 1, 2);
        quantityDocumentArticle = addSGProp(baseGroup, "quantityDocumentArticle", "Кол-во артикула в документе", quantityDocumentSku, 1, articleSku, 2);
        quantityDocument = addSGProp(baseGroup, "quantityDocument", "Общее кол-во в документе", quantityDocumentSku, 1);

        // связь инвойсов и заказов
        inOrderInvoice = addDProp(baseGroup, "inOrderInvoice", "Вкл", LogicalClass.instance, order, invoice);

        addConstraint(addJProp("Магазин инвойса должен совпадать с магазином заказа", baseLM.and1,
                addJProp(baseLM.diff2, destinationDestinationDocument, 1, destinationDestinationDocument, 2), 1, 2,
                inOrderInvoice, 1, 2), true);

        orderedOrderInvoiceSku = addJProp(baseLM.and1, quantityDocumentSku, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceSku = addSGProp(baseGroup, "orderedInvoiceSku", "Кол-во заказано", orderedOrderInvoiceSku, 2, 3);
        orderedSimpleInvoiceSku = addJProp(baseGroup, "orderedSimpleInvoiceSku", "Кол-во заказано", baseLM.and1, orderedInvoiceSku, 1, 2, is(simpleInvoice), 1);
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
                        addJProp(baseLM.and1, orderedSimpleInvoiceSku, 1, 2, is(item), 2), // если не артикул (простой), то пропорционально заказано
                        addJProp(baseLM.and1, orderedSupplierBoxSku, 1, 2, is(item), 2)), 1, 2, // ограничение (максимально-возможное число)
                2);

        quantityDocumentArticleCompositeColor = addSGProp(baseGroup, "quantityDocumentArticleCompositeColor", "Кол-во", quantityDocumentSku, 1, articleCompositeItem, 2, colorSupplierItem, 2);
        quantityDocumentArticleCompositeSize = addSGProp(baseGroup, "quantityDocumentArticleCompositeSize", "Кол-во", quantityDocumentSku, 1, articleCompositeItem, 2, sizeSupplierItem, 2);

        quantityDocumentArticleCompositeColorSize = addDGProp(baseGroup, "quantityDocumentArticleCompositeColorSize", "Кол-во",
                1, false,
                quantityDocumentSku, 1, articleCompositeItem, 2, colorSupplierItem, 2, sizeSupplierItem, 2,
                addCProp(DoubleClass.instance, Double.MAX_VALUE, document, sku), 1, 2,
                2);
        quantityDocumentArticleCompositeColorSize.property.setFixedCharWidth(2);

        orderedOrderInvoiceArticle = addJProp(baseLM.and1, quantityListArticle, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceArticle = addSGProp(baseGroup, "orderedInvoiceArticle", "Кол-во заказано", orderedOrderInvoiceArticle, 2, 3);
        // todo : сделать, чтобы работало автоматическое проставление
//        quantityListArticle.setDerivedForcedChange(orderedInvoiceArticle, 1, 2, numberListArticle, 1, 2);

        invoicedOrderArticle = addSGProp(baseGroup, "invoicedOrderArticle", "Выставлено инвойсов", invoicedOrderSku, 1, articleSku, 2);

        // цены
        priceDocumentArticle = addDProp(baseGroup, "priceDocumentArticle", "Цена", DoubleClass.instance, priceDocument, article);
        priceDataDocumentItem = addDProp(baseGroup, "priceDataDocumentItem", "Цена по товару", DoubleClass.instance, priceDocument, item);
        priceArticleDocumentSku = addJProp(baseGroup, "priceArticleDocumentItem", "Цена по артикулу", priceDocumentArticle, 1, articleSku, 2);
        priceDocumentSku = addSUProp(baseGroup, "priceDocumentSku", true, "Цена", Union.OVERRIDE, priceArticleDocumentSku, priceDataDocumentItem);

        priceRateDocumentSku = addJProp(baseGroup, "priceRateDocumentSku", true, "Цена (конверт.)", round2, addJProp(baseLM.multiplyDouble2, priceDocumentSku, 1, 2, addJProp(nearestRateExchange, typeExchangeSTX, currencyDocument, 1, 1), 1), 1, 2);

        RRPDocumentArticle = addDProp(baseGroup, "RRPDocumentArticle", "Рекомендованная цена", DoubleClass.instance, priceDocument, article);

        priceSupplierBoxSku = addJProp(baseGroup, "priceSupplierBoxSku", "Цена", priceDocumentSku, boxInvoiceSupplierBox, 1, 2);

        priceOrderInvoiceArticle = addJProp(baseLM.and1, priceDocumentArticle, 1, 3, inOrderInvoice, 1, 2);
        priceOrderedInvoiceArticle = addMGProp(baseGroup, "priceOrderedInvoiceArticle", "Цена в заказе", priceOrderInvoiceArticle, 2, 3);
        // todo : не работает
        priceDocumentArticle.setDerivedForcedChange(priceOrderedInvoiceArticle, 1, 2, numberDocumentArticle, 1, 2);

        sumSupplierBoxSku = addJProp(baseGroup, "sumSupplierBoxSku", "Сумма", baseLM.multiplyDouble2, quantityListSku, 1, 2, priceSupplierBoxSku, 1, 2);
        sumDocumentSku = addJProp(baseGroup, "sumDocumentSku", "Сумма", baseLM.multiplyDouble2, quantityDocumentSku, 1, 2, priceDocumentSku, 1, 2);

        netWeightDocumentArticle = addJProp(baseGroup, "netWeightDocumentArticle", "Общий вес по артикулу", baseLM.multiplyDouble2, quantityDocumentArticle, 1, 2, netWeightArticle, 2);
        netWeightDocumentSku = addJProp(baseGroup, "netWeightDocumentSku", "Общий вес по sku", baseLM.multiplyDouble2, quantityDocumentSku, 1, 2, netWeightSku, 2);
        netWeightDocument = addSGProp(baseGroup, "netWeightDocument", "Общий вес", netWeightDocumentSku, 1);

        sumDocumentArticle = addSGProp(baseGroup, "sumDocumentArticle", "Сумма", sumDocumentSku, 1, articleSku, 2);
        sumDocument = addSGProp(baseGroup, "sumDocument", "Сумма документа", sumDocumentSku, 1);

        // route
        percentShipmentRoute = addDProp(baseGroup, "percentShipmentRoute", "Процент", DoubleClass.instance, shipment, route);

        percentShipmentRouteSku = addJProp(baseGroup, "percentShipmentRouteSku", "Процент", baseLM.and1, percentShipmentRoute, 1, 2, is(sku), 3);

        // creation
        quantityCreationFreightBox = addDProp(baseGroup, "quantityCreationFreightBox", "Количество", IntegerClass.instance, creationFreightBox);
        routeCreationFreightBox = addDProp(idGroup, "routeCreationFreightBox", "Маршрут (ИД)", route, creationFreightBox);
        nameRouteCreationFreightBox = addJProp(baseGroup, "nameRouteCreationFreightBox", "Маршрут", baseLM.name, routeCreationFreightBox, 1);

        quantityCreationPallet = addDProp(baseGroup, "quantityCreationPallet", "Количество", IntegerClass.instance, creationPallet);
        routeCreationPallet = addDProp(idGroup, "routeCreationPallet", "Маршрут (ИД)", route, creationPallet);
        nameRouteCreationPallet = addJProp(baseGroup, "nameRouteCreationPallet", "Маршрут", baseLM.name, routeCreationPallet, 1);

        firstNumberCreationStamp = addDProp(baseGroup, "firstNumberCreationStamp", "Номер с", IntegerClass.instance, creationStamp);
        lastNumberCreationStamp = addDProp(baseGroup, "lastNumberCreationStamp", "Номер по", IntegerClass.instance, creationStamp);
        seriesOfStampCreationStamp = addDProp(baseGroup, "seriesOfStampCreationStamp", "Серия", DateClass.instance, creationStamp);
        dateOfStampCreationStamp = addDProp(baseGroup, "dateOfStampCreationStamp", "Дата", DateClass.instance, creationStamp);
        creationStampStamp = addDProp(idGroup, "creationStampStamp", "Операция (ИД)", creationStamp, stamp);

        // паллеты
        creationPalletPallet = addDProp(idGroup, "creationPalletPallet", "Операция (ИД)", creationPallet, pallet);
        routeCreationPalletPallet = addJProp(idGroup, "routeCreationPalletPallet", true, "Маршрут (ИД)", routeCreationPallet, creationPalletPallet, 1);
        nameRouteCreationPalletPallet = addJProp(baseGroup, "nameRouteCreationPalletPallet", "Маршрут", baseLM.name, routeCreationPalletPallet, 1);

        freightPallet = addDProp(baseGroup, "freightPallet", "Фрахт (ИД)", freight, pallet);
        equalsPalletFreight = addJProp(baseGroup, "equalsPalletFreight", "Вкл.", baseLM.equals2, freightPallet, 1, 2);

        // инвойсы напрямую во фрахты
        freightDirectInvoice = addDProp(baseGroup, "freightDirectInvoice", "Фрахт (ИД)", freight, directInvoice);
        equalsDirectInvoiceFreight = addJProp(baseGroup, "equalsDirectInvoiceFreight", "Вкл.", baseLM.equals2, freightDirectInvoice, 1, 2);

        grossWeightDirectInvoice = addDProp(baseGroup, "grossWeightDirectInvoice", "Вес брутто", DoubleClass.instance, directInvoice);
        palletNumberDirectInvoice = addDProp(baseGroup, "palletNumberDirectInvoice", "Кол-во паллет", IntegerClass.instance, directInvoice);

        freightShippedDirectInvoice = addJProp(baseGroup, "freightShippedDirectInvoice", is(freightShipped), freightDirectInvoice, 1);

        sumDirectInvoicedSku = addSGProp(baseGroup, "sumDirectInvoicedSku", "Сумма по инвойсам напрямую", addJProp(and(false, true), sumDocumentSku, 1, 2, is(directInvoice), 1, freightShippedDirectInvoice, 1), 2);
        quantityDirectInvoicedSku = addSGProp(baseGroup, "quantityDirectInvoicedSku", "Кол-во по инвойсам напрямую", addJProp(and(false, true), quantityDocumentSku, 1, 2, is(directInvoice), 1, freightShippedDirectInvoice, 1), 2);
        quantityDocumentBrandSupplier = addSGProp(baseGroup, "quantityDocumentBrandSupplier", "Кол-во по бренду в документе", addJProp(baseLM.andNot1, quantityDocumentSku, 1, 2, freightShippedDirectInvoice, 1), 1, brandSupplierArticleSku, 2);

        // freight box
        creationFreightBoxFreightBox = addDProp(idGroup, "creationFreightBoxFreightBox", "Операция (ИД)", creationFreightBox, freightBox);

        palletFreightBox = addDProp(idGroup, "palletFreightBox", "Паллета (ИД)", pallet, freightBox);
        equalsPalletFreightBox = addJProp(baseGroup, "equalsPalletFreightBox", "Вкл.", baseLM.equals2, palletFreightBox, 1, 2);
        barcodePalletFreightBox = addJProp(baseGroup, "barcodePalletFreightBox", "Паллета (штрих-код)", baseLM.barcode, palletFreightBox, 1);

        routeCreationFreightBoxFreightBox = addJProp(idGroup, "routeCreationFreightBoxFreightBox", true, "Маршрут (ИД)", routeCreationFreightBox, creationFreightBoxFreightBox, 1);
        nameRouteCreationFreightBoxFreightBox = addJProp(baseGroup, "nameRouteCreationFreightBoxFreightBox", "Маршрут", baseLM.name, routeCreationFreightBoxFreightBox, 1);

        freightFreightBox = addJProp(idGroup, "freightFreightBox", "Фрахт короба транспортировки", freightPallet, palletFreightBox, 1);

        destinationFreightBox = addDProp(idGroup, "destinationFreightBox", "Пункт назначения (ИД)", destination, freightBox);
        nameDestinationFreightBox = addJProp(baseGroup, "nameDestinationFreightBox", "Пункт назначения", baseLM.name, destinationFreightBox, 1);

        // поставка на склад
        inInvoiceShipment = addDProp(baseGroup, "inInvoiceShipment", "Вкл", LogicalClass.instance, invoice, shipment);

        inSupplierBoxShipment = addJProp(baseGroup, "inSupplierBoxShipment", "Вкл", inInvoiceShipment, boxInvoiceSupplierBox, 1, 2);

        invoicedShipmentSku = addSGProp(baseGroup, "invoicedShipmentSku", true, "Ожид. (пост.)",
                addJProp(baseLM.and1, quantityDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        priceShipmentSku = addMGProp(baseGroup, "priceShipmentSku", true, "Цена (пост.)",
                addJProp(baseLM.and1, priceDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        invoicedShipment = addSGProp(baseGroup, "invoicedShipment", true, "Всего ожидается (пост.)", invoicedShipmentSku, 1);

        //sku shipment detail
        skuShipmentDetail = addDProp(idGroup, "skuShipmentDetail", "SKU (ИД)", sku, shipmentDetail);
        barcodeSkuShipmentDetail = addJProp(baseGroup, "barcodeSkuShipmentDetail", "Штрих-код SKU", baseLM.barcode, skuShipmentDetail, 1);
        setNotNull(skuShipmentDetail);

        articleShipmentDetail = addJProp(idGroup, "articleShipmentDetail", "Артикул (ИД)", articleSku, skuShipmentDetail, 1);
        sidArticleShipmentDetail = addJProp(baseGroup, "sidArticleShipmentDetail", "Артикул", sidArticle, articleShipmentDetail, 1);
        setNotNull(articleShipmentDetail);

        colorSupplierItemShipmentDetail = addJProp(idGroup, "colorSupplierItemShipmentDetail", "Цвет поставщика (ИД)", colorSupplierItem, skuShipmentDetail, 1);
        sidColorSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "sidColorSupplierItemShipmentDetail", "Код цвета", sidColorSupplier, colorSupplierItemShipmentDetail, 1);
        nameColorSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "nameColorSupplierItemShipmentDetail", "Цвет поставщика", baseLM.name, colorSupplierItemShipmentDetail, 1);

        sizeSupplierItemShipmentDetail = addJProp(idGroup, "sizeSupplierItemShipmentDetail", "Размер поставщика (ИД)", sizeSupplierItem, skuShipmentDetail, 1);
        sidSizeSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "sidSizeSupplierItemShipmentDetail", "Размер поставщика", sidSizeSupplier, sizeSupplierItemShipmentDetail, 1);

        nameBrandSupplierArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "nameBrandSupplierArticleSkuShipmentDetail", "Бренд", nameBrandSupplierArticleSku, skuShipmentDetail, 1);
        originalNameArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "originalNameArticleSkuShipmentDetail", "Наименование (ориг.)", originalNameArticleSku, skuShipmentDetail, 1);

        categoryArticleSkuShipmentDetail = addJProp(idGroup, true, "categoryArticleSkuShipmentDetail", "Номенклатурная группа товара (ИД)", categoryArticleSku, skuShipmentDetail, 1);
        nameOriginCategoryArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameOriginCategoryArticleSkuShipmentDetail", "Номенклатурная группа товара", nameOrigin, categoryArticleSkuShipmentDetail, 1);

        customCategoryOriginArticleSkuShipmentDetail = addJProp(idGroup, true, "customCategoryOriginArticleSkuShipmentDetail", "ТН ВЭД (ИД)", customCategoryOriginArticleSku, skuShipmentDetail, 1);
        sidCustomCategoryOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticleSkuShipmentDetail", "Код ТН ВЭД", sidCustomCategoryOrigin, customCategoryOriginArticleSkuShipmentDetail, 1);

        netWeightArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "netWeightArticleSkuShipmentDetail", "Вес нетто единицы товара (ориг.)", netWeightArticleSku, skuShipmentDetail, 1);
        netWeightSkuShipmentDetail = addJProp(intraAttributeGroup, true, "netWeightSkuShipmentDetail", "Вес нетто единицы товара", netWeightSku, skuShipmentDetail, 1);

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
        barcodeStockShipmentDetail = addJProp(baseGroup, "barcodeStockShipmentDetail", "Штрих-код короба для транспортировки", baseLM.barcode, stockShipmentDetail, 1);

        routeFreightBoxShipmentDetail = addJProp(idGroup, "routeFreightBoxShipmentDetail", "Маршрут (ИД)", routeCreationFreightBoxFreightBox, stockShipmentDetail, 1);
        nameRouteFreightBoxShipmentDetail = addJProp(baseGroup, "nameRouteFreightBoxShipmentDetail", "Маршрут", baseLM.name, routeFreightBoxShipmentDetail, 1);

        boxShipmentBoxShipmentDetail = addDProp(idGroup, "boxShipmentBoxShipmentDetail", "Поставка (ИД)", boxShipment, boxShipmentDetail);
        simpleShipmentSimpleShipmentDetail = addDProp(idGroup, "simpleShipmentSimpleShipmentDetail", "Поставка (ИД)", simpleShipment, simpleShipmentDetail);
        shipmentShipmentDetail = addCUProp(idGroup, "shipmentShipmentDetail", "Поставка (ИД)", boxShipmentBoxShipmentDetail, simpleShipmentSimpleShipmentDetail);
        sidShipmentShipmentDetail = addJProp(baseGroup, "sidShipmentShipmentDetail", "Поставка", sidDocument, shipmentShipmentDetail, 1);

        // supplier box shipmentDetail
        supplierBoxShipmentDetail = addDProp(idGroup, "supplierBoxShipmentDetail", "Короб поставщика (ИД)", supplierBox, boxShipmentDetail);
        sidSupplierBoxShipmentDetail = addJProp(baseGroup, "sidSupplierBoxShipmentDetail", "Номер короба поставщика", sidSupplierBox, supplierBoxShipmentDetail, 1);
        barcodeSupplierBoxShipmentDetail = addJProp(baseGroup, "barcodeSupplierBoxShipmentDetail", "Штрих-код короба поставщика", baseLM.barcode, supplierBoxShipmentDetail, 1);

        quantityShipmentDetail = addDProp(baseGroup, "quantityShipmentDetail", "Кол-во", DoubleClass.instance, shipmentDetail);

        userShipmentDetail = addDCProp(idGroup, "userShipmentDetail", "Пользователь (ИД)", baseLM.currentUser, true, is(shipmentDetail), 1);
        nameUserShipmentDetail = addJProp(baseGroup, "nameUserShipmentDetail", "Пользователь", baseLM.name, userShipmentDetail, 1);

        timeShipmentDetail = addTCProp(Time.DATETIME, "timeShipmentDetail", true, "Время ввода", quantityShipmentDetail);

        addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode = addJProp(true, "Добавить строку поставки",
                addAAProp(boxShipmentDetail, boxShipmentBoxShipmentDetail, supplierBoxShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail),
                1, 2, 3, skuBarcodeObject, 4, addCProp(DoubleClass.instance, 1));

        addSimpleShipmentSimpleShipmentDetailStockBarcode = addJProp(true, "Добавить строку поставки",
                addAAProp(simpleShipmentDetail, simpleShipmentSimpleShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail),
                1, 2, baseLM.barcodeToObject, 3, addCProp(DoubleClass.instance, 1));

        quantitySupplierBoxBoxShipmentStockSku = addSGProp(baseGroup, "quantitySupplierBoxBoxShipmentStockSku", "Кол-во оприход.", quantityShipmentDetail,
                supplierBoxShipmentDetail, 1, boxShipmentBoxShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        quantitySupplierBoxBoxShipmentSku = addSGProp(baseGroup, "quantitySupplierBoxBoxShipmentSku", "Кол-во оприход.", quantitySupplierBoxBoxShipmentStockSku,
                1, 2, 4);

        quantitySupplierBoxSku = addSGProp(baseGroup, "quantitySupplierBoxSku", "Кол-во оприход.", quantitySupplierBoxBoxShipmentStockSku, 1, 4);

        diffListSupplierBoxSku =addJProp(baseLM.diff2, quantityListSku, 1, 2, quantitySupplierBoxSku, 1, 2);

        quantitySimpleShipmentStockSku = addSGProp(baseGroup, "quantitySimpleShipmentStockSku", "Кол-во оприход.", quantityShipmentDetail,
                simpleShipmentSimpleShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        quantityShipDimensionShipmentStockSku = addCUProp(baseGroup, "quantityShipDimensionShipmentStockSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                addJProp(baseLM.and1, quantitySimpleShipmentStockSku, 2, 3, 4, baseLM.equals2, 1, 2));

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

        quantityInvoiceSku = addSGProp(baseGroup, "quantityInvoiceSku", true, "Кол-во оприход.", quantityInvoiceStockSku, 1, 3);

        diffDocumentInvoiceSku = addJProp(baseLM.equals2, quantityDocumentSku, 1, 2, quantityInvoiceSku, 1, 2);

        priceInInvoiceStockSku = addJProp(baseGroup, "priceInInvoiceStockSku", false, "Цена входная", baseLM.and1,
                priceRateDocumentSku, 1, 3, quantityInvoiceStockSku, 1, 2, 3);

        quantityShipDimensionStock = addSGProp(baseGroup, "quantityShipDimensionStock", "Всего оприход.", quantityShipDimensionShipmentStockSku, 1, 3);

        quantityShipDimensionShipmentSku = addSGProp(baseGroup, "quantityShipDimensionShipmentSku", "Оприход. (короб)", quantityShipDimensionShipmentStockSku, 1, 2, 4);

        zeroQuantityListSku = addSUProp(baseGroup, "zeroQuantityListSku", "кол-во", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, list, sku), quantityListSku);
        zeroQuantityShipDimensionShipmentSku = addSUProp(baseGroup, "zeroQuantityShipDimensionShipmentSku", "кол-во", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipDimension, shipment, sku), quantityShipDimensionShipmentSku);

        diffListShipSku = addJProp(baseLM.diff2, zeroQuantityListSku, 1, 3, zeroQuantityShipDimensionShipmentSku, 1, 2, 3);

        quantityShipmentStockSku = addSGProp(baseGroup, "quantityShipmentStockSku", true, "Кол-во оприход.", quantityShipDimensionShipmentStockSku, 2, 3, 4);

        quantityShipmentStock = addSGProp(baseGroup, "quantityShipmentStock", "Всего оприход.", quantityShipmentStockSku, 1, 2);

        quantityShipmentSku = addSGProp(baseGroup, "quantityShipmentSku", "Оприход. (пост.)", quantityShipmentStockSku, 1, 3);

        quantityShipment = addSGProp(baseGroup, "quantityShipment", "Оприходовано", quantityShipmentSku, 1);
        
        zeroQuantityShipmentSku = addSUProp(baseGroup, "zeroQuantityShipmentSku", "кол-во", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipment, sku), quantityShipmentSku);
        zeroInvoicedShipmentSku = addSUProp(baseGroup, "zeroInvoicedShipmentSku", "кол-во", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipment, sku), invoicedShipmentSku);
        diffShipmentSku = addJProp(baseLM.diff2, zeroQuantityShipmentSku, 1, 2, zeroInvoicedShipmentSku, 1, 2);

        quantityStockSku = addSGProp(baseGroup, "quantityStockSku", true, true, "Оприход. в короб для транспортировки", quantityShipmentStockSku, 2, 3);

        quantityStockArticle = addSGProp(baseGroup, "quantityStockArticle", "Кол-во по артикулу", quantityStockSku, 1, articleSku, 2);

        freightShippedFreightBox = addJProp(baseGroup, "freightShippedFreightBox", is(freightShipped), freightFreightBox, 1);

        sumInInvoiceStockSku = addJProp(baseGroup, "sumInInvoiceStockSku", "Сумма в коробе", baseLM.multiplyDouble2, addJProp(baseLM.andNot1, quantityInvoiceStockSku, 1, 2, 3, freightShippedFreightBox, 2), 1, 2, 3, priceInInvoiceStockSku, 1, 2, 3);

        sumStockedSku = addSGProp(baseGroup, "sumStockedSku", "Сумма на приемке", sumInInvoiceStockSku, 3);
        quantityStockedSku = addSGProp(baseGroup, "quantityStockedSku", "Кол-во на приемке", addJProp(baseLM.andNot1, quantityStockSku, 1, 2, freightShippedFreightBox, 1), 2);

        quantitySku = addSUProp(baseGroup, "quantitySku", "Кол-во", Union.SUM, quantityStockedSku, quantityDirectInvoicedSku);
        sumSku = addSUProp(baseGroup, "sumSku", "Сумма", Union.SUM, sumStockedSku, sumDirectInvoicedSku);

        quantityStockBrandSupplier = addSGProp(baseGroup, "quantityStockBrandSupplier", "Кол-во по бренду",
                addJProp(baseLM.andNot1, quantityStockArticle, 1, 2, freightShippedFreightBox, 1), 1, brandSupplierArticle, 2);

        quantityPalletSku = addSGProp(baseGroup, "quantityPalletSku", "Оприход. (пал.)", quantityStockSku, palletFreightBox, 1, 2);

        quantityPalletBrandSupplier = addSGProp(baseGroup, "quantityPalletBrandSupplier", "Кол-во по бренду", quantityStockBrandSupplier, palletFreightBox, 1, 2);

        quantityShipmentPallet = addSGProp(baseGroup, "quantityShipmentPallet", "Всего оприход. (паллета)", quantityShipmentStock, 1, palletFreightBox, 2);

        quantityShipmentFreight = addSGProp(baseGroup, "quantityShipmentFreight", "Всего оприход. (фрахт)", quantityShipmentPallet, 1, freightPallet, 2);

        quantityShipmentArticle = addSGProp(baseGroup, "quantityShipmentArticle", "Всего оприход. (артикул)", quantityShipmentSku, 1, articleSku, 2);
        quantityShipmentArticleSize = addSGProp(baseGroup, "quantityShipmentArticleSize", "Всего оприход. (артикул-размер)", quantityShipmentSku, 1, articleSku, 2, sizeSupplierItem, 2);
        quantityShipmentArticleColor = addSGProp(baseGroup, "quantityShipmentArticleColor", "Всего оприход. (артикул-цвет)", quantityShipmentSku, 1, articleSku, 2, colorSupplierItem, 2);

        oneShipmentArticle = addJProp(baseGroup, "oneShipmentArticle", "Первый артикул", baseLM.equals2, quantityShipmentArticle, 1, 2, addCProp(DoubleClass.instance, 1));
        oneShipmentArticleColor = addJProp(baseGroup, "oneShipmentArticleColor", "Первый артикул-цвет", baseLM.equals2, quantityShipmentArticleColor, 1, 2, 3, addCProp(DoubleClass.instance, 1));
        oneShipmentArticleSize = addJProp(baseGroup, "oneShipmentArticleSize", "Первый артикул-размер", baseLM.equals2, quantityShipmentArticleSize, 1, 2, 3, addCProp(DoubleClass.instance, 1));

        oneShipmentArticleSku = addJProp(baseGroup, "oneShipmentArticleSku", "Первый артикул", oneShipmentArticle, 1, articleSku, 2);
        oneShipmentArticleColorSku = addJProp(baseGroup, "oneShipmentArticleColorSku", "Первый артикул-цвет", oneShipmentArticleColor, 1, articleSku, 2, colorSupplierItem, 2);
        oneShipmentArticleSizeSku = addJProp(baseGroup, "oneShipmentArticleSizeSku", "Первый артикул-размер", oneShipmentArticleSize, 1, articleSku, 2, sizeSupplierItem, 2);

        oneShipmentSku = addJProp(baseGroup, "oneShipmentSku", "Первый SKU", baseLM.equals2, quantityShipmentSku, 1, 2, addCProp(DoubleClass.instance, 1));

        oneArticleSkuShipmentDetail = addJProp(baseGroup, "oneArticleSkuShipmentDetail", "Первый артикул", oneShipmentArticleSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);
        oneArticleColorShipmentDetail = addJProp(baseGroup, "oneArticleColorShipmentDetail", "Первый артикул-цвет", oneShipmentArticleColorSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);
        oneArticleSizeShipmentDetail = addJProp(baseGroup, "oneArticleSizeShipmentDetail", "Первый артикул-размер", oneShipmentArticleSizeSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);
        oneSkuShipmentDetail = addJProp(baseGroup, "oneSkuShipmentDetail", "Первый SKU", oneShipmentSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);

        // Stamp
        sidStamp = addDProp(baseGroup, "sidStamp", "Контрольная марка", StringClass.get(100), stamp);
        seriesOfStamp = addDProp(baseGroup, "seriesOfStamp", "Серия марки", StringClass.get(2), stamp);
        dateOfStamp = addDProp(baseGroup, "dateOfStamp", "Дата марки", DateClass.instance, stamp);
        stampShipmentDetail = addDProp("stampSkuShipmentDetail", "Контрольная марка", stamp, shipmentDetail);
        necessaryStampCategory = addDProp(baseGroup, "necessaryStampCategory", "Нужна марка",  LogicalClass.instance, category);
        necessaryStampSkuShipmentDetail = addJProp("necessaryStampSkuShipmentDetail",necessaryStampCategory, categoryArticleSkuShipmentDetail, 1);
        sidStampShipmentDetail = addJProp(intraAttributeGroup, "sidStampShipmentDetail", "Контрольная марка",  sidStamp, stampShipmentDetail, 1);
        hideSidStampShipmentDetail = addHideCaptionProp(privateGroup, "Контрольная марка", sidStampShipmentDetail, necessaryStampSkuShipmentDetail);
        shipmentDetailStamp = addAGProp(idGroup, "shipmentDetailStamp", "Контрольная марка (ИД)", shipmentDetail, stampShipmentDetail);



        // Transfer
        stockFromTransfer = addDProp(idGroup, "stockFromTransfer", "Место хранения (с) (ИД)", stock, transfer);
        barcodeStockFromTransfer = addJProp(baseGroup, "barcodeStockFromTransfer", "Штрих-код МХ (с)", baseLM.barcode, stockFromTransfer, 1);

        stockToTransfer = addDProp(idGroup, "stockToTransfer", "Место хранения (на) (ИД)", stock, transfer);
        barcodeStockToTransfer = addJProp(baseGroup, "barcodeStockToTransfer", "Штрих-код МХ (на)", baseLM.barcode, stockToTransfer, 1);

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

        zeroQuantityShipmentRouteSku = addSUProp(baseGroup, "zeroQuantityShipmentRouteSku", "кол-во", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipment, route, sku), quantityShipmentRouteSku);
        zeroInvoicedShipmentRouteSku = addSUProp(baseGroup, "zeroInvoicedShipmentRouteSku", "кол-во", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipment, route, sku), invoicedShipmentRouteSku);

        diffShipmentRouteSku = addJProp(baseLM.greater2, zeroQuantityShipmentRouteSku, 1, 2, 3, zeroInvoicedShipmentRouteSku, 1, 2, 3);

        sumShipmentRouteSku = addJProp(baseGroup, "sumShipmentRouteSku", "Сумма", baseLM.multiplyDouble2, invoicedShipmentRouteSku, 1, 2, 3, priceShipmentSku, 1, 3);
        sumShipmentRoute = addSGProp(baseGroup, "sumShipmentRoute", "Сумма (ожид.)", sumShipmentRouteSku, 1, 2);
        sumShipment = addSGProp(baseGroup, "sumShipment", "Сумма (ожид.)", sumShipmentRoute, 1);

        invoicedShipmentRoute = addSGProp(baseGroup, "invoicedShipmentRoute", "Кол-во", invoicedShipmentRouteSku, 1, 2);

//        notFilledShipmentRouteSku = addJProp(baseGroup, "notFilledShipmentRouteSku", "Не заполнен", greater2, invoicedShipmentRouteSku, 1, 2, 3,
//                addSUProp(Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipment, route, sku), quantityShipmentRouteSku), 1, 2, 3);
//
//        routeToFillShipmentSku = addMGProp(idGroup, "routeToFillShipmentSku", "Маршрут (ИД)",
//                addJProp(baseLM.and1, object(route), 2, notFilledShipmentRouteSku, 1, 2, 3), 1, 3);
//
//        LP routeToFillShipmentBarcode = addJProp(routeToFillShipmentSku, 1, baseLM.barcodeToObject, 2);
//        seekRouteToFillShipmentBarcode = addJProp(actionGroup, true, "seekRouteToFillShipmentSku", "Поиск маршрута", addSAProp(null),
//                routeToFillShipmentBarcode, 1, 2);

        addConstraint(addJProp("Магазин короба для транспортировки должен совпадать с магазином короба поставщика", baseLM.and1,
                addJProp(baseLM.diff2, destinationSupplierBox, 1, destinationFreightBox, 2), 1, 2,
                quantityShipDimensionStock, 1, 2), true);

        // Freight
        tonnageFreightType = addDProp(baseGroup, "tonnageFreightType", "Тоннаж (кг)", DoubleClass.instance, freightType);
        palletCountFreightType = addDProp(baseGroup, "palletCountFreightType", "Кол-во паллет", IntegerClass.instance, freightType);
        volumeFreightType = addDProp(baseGroup, "volumeFreightType", "Объем", DoubleClass.instance, freightType);

        freightTypeFreight = addDProp(idGroup, "freightTypeFreight", "Тип машины (ИД)", freightType, freight);
        nameFreightTypeFreight = addJProp(baseGroup, "nameFreightTypeFreight", "Тип машины", baseLM.name, freightTypeFreight, 1);

        tonnageFreight = addJProp(baseGroup, "tonnageFreight", "Тоннаж (кг)", tonnageFreightType, freightTypeFreight, 1);
        palletCountFreight = addJProp(baseGroup, "palletCountFreight", "Кол-во паллет", palletCountFreightType, freightTypeFreight, 1);
        volumeFreight = addJProp(baseGroup, "volumeFreight", "Объём", volumeFreightType, freightTypeFreight, 1);

        currencyFreight = addDProp(idGroup, "currencyFreight", "Валюта (ИД)", currency, freight);
        nameCurrencyFreight = addJProp(baseGroup, "nameCurrencyFreight", "Валюта", baseLM.name, currencyFreight, 1);
        sumFreightFreight = addDProp(baseGroup, "sumFreightFreight", "Стоимость", DoubleClass.instance, freight);

        routeFreight = addDProp(idGroup, "routeFreight", "Маршрут (ИД)", route, freight);
        nameRouteFreight = addJProp(baseGroup, "nameRouteFreight", "Маршрут", baseLM.name, routeFreight, 1);

        exporterFreight = addDProp(idGroup, "exporterFreight", "Экспортер (ИД)", exporter, freight);
        nameOriginExporterFreight = addJProp(baseGroup, "nameOriginExporterFreight", "Экспортер", nameOrigin, exporterFreight, 1);
        nameExporterFreight = addJProp(baseGroup, "nameExporterFreight", "Экспортер", baseLM.name, exporterFreight, 1);
        addressOriginExporterFreight = addJProp(baseGroup, "addressOriginExporterFreight", "Адрес", addressOriginSubject, exporterFreight, 1);
        addressExporterFreight = addJProp(baseGroup, "addressExporterFreight", "Адрес", addressSubject, exporterFreight, 1);

        quantityPalletShipmentBetweenDate = addSGProp(baseGroup, "quantityPalletShipmentBetweenDate", "Кол-во паллет по поставкам за интервал",
                addJProp(baseLM.and1, quantityPalletShipment, 1, addJProp(baseLM.between, baseLM.date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);
        quantityPalletFreightBetweenDate = addSGProp(baseGroup, "quantityPalletFreightBetweenDate", "Кол-во паллет по фрахтам за интервал",
                addJProp(baseLM.and1, palletCountFreight, 1, addJProp(baseLM.between, baseLM.date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);

        freightBoxNumberPallet = addSGProp(baseGroup, "freightBoxNumberPallet", "Кол-во коробов", addCProp(IntegerClass.instance, 1, freightBox), palletFreightBox, 1);

        addConstraint(addJProp("Маршрут паллеты должен совпадать с маршрутом фрахта", baseLM.diff2,
                routeCreationPalletPallet, 1, addJProp(routeFreight, freightPallet, 1), 1), true);

        addConstraint(addJProp("Маршрут короба должен совпадать с маршрутом паллеты", baseLM.diff2,
                routeCreationFreightBoxFreightBox, 1, addJProp(routeCreationPalletPallet, palletFreightBox, 1), 1), true);

        palletNumberFreight = addSUProp(baseGroup, "palletNumberFreight", "Кол-во присоединённых паллет", Union.SUM,
                addSGProp(addCProp(IntegerClass.instance, 1, pallet), freightPallet, 1),
                addSGProp(palletNumberDirectInvoice, freightDirectInvoice, 1));

        diffPalletFreight = addJProp(baseLM.greater2, palletNumberFreight, 1, palletCountFreight, 1);
        // freight для supplierBox
        freightSupplierBox = addJProp(baseGroup, "freightSupplierBox", "Фрахт (ИД)", freightDirectInvoice, boxInvoiceSupplierBox, 1);
        freightFreightUnit = addCUProp(idGroup, "freightFreightUnit", "Фрахт (ИД)", freightFreightBox, freightSupplierBox);

        importerSupplierBox = addJProp(baseGroup, "importerSupplierBox", "Импортер (ИД)", importerInvoice, boxInvoiceSupplierBox, 1);

        // Кол-во для импортеров
        // здесь не соблюдается policy, что входы совпадают с именем
        quantityInvoiceFreightUnitSku = addCUProp(baseGroup, "quantityInvoiceFreightUnitSku", "Кол-во",
                quantityInvoiceStockSku,
                addJProp(baseLM.and1, quantityListSku, 2, 3, addJProp(baseLM.equals2, 1, boxInvoiceSupplierBox, 2), 1, 2));

        priceInInvoiceFreightUnitSku = addCUProp(baseGroup, "priceInInvoiceFreightUnitSku", "Цена входная",
                priceInInvoiceStockSku,
                addJProp(baseLM.and1, priceRateDocumentSku, 1, 3, addJProp(baseLM.equals2, 1, boxInvoiceSupplierBox, 2), 1, 2));

        quantityImporterStockSku = addSGProp(baseGroup, "quantityImporterStockSku", "Кол-во", quantityInvoiceStockSku, importerInvoice, 1, 2, 3);
        quantityImporterStockArticle = addSGProp(baseGroup, "quantityImporterStockArticle", "Кол-во", quantityImporterStockSku, 1, 2, articleSku, 3);
        quantityImporterStock = addSGProp(baseGroup, "quantityImporterStock", "Кол-во", quantityImporterStockSku, 1, 2);

        quantityProxyImporterFreightSku = addSGProp(baseGroup, "quantityProxyImporterFreightSku", true, true, "Кол-во (из приёмки)", quantityImporterStockSku, 1, freightFreightUnit, 2, 3);
        quantityDirectImporterFreightSku = addSGProp(baseGroup, "quantityDirectImporterFreightSku", true, true, "Кол-во (напрямую)", quantityListSku, importerSupplierBox, 1, freightFreightUnit, 1, 2);
        quantityImporterFreightSku = addSUProp(baseGroup, "quantityImporterFreightSku", "Кол-во", Union.SUM, quantityProxyImporterFreightSku, quantityDirectImporterFreightSku);

        quantityFreightArticle = addSGProp(baseGroup, "quantityFreightArticle", "Кол-во", quantityImporterFreightSku, 2, articleSku, 3);

        quantityFreightSku = addSGProp(baseGroup, "quantityFreightSku", true, true, "Кол-во", quantityImporterFreightSku, 2, 3);
        quantityDirectFreightSku = addSGProp(baseGroup, "quantityDirectFreightSku", true, true, "Кол-во (напрямую)", quantityDirectImporterFreightSku, 2, 3);

        quantityFreightCategory = addSGProp(baseGroup, "quantityFreightCategory", true, true, "Кол-во", quantityFreightSku, 1, categoryArticleSku, 2);

        customCategory10FreightSku = addDProp(idGroup, "customCategory10FreightSku", "ТН ВЭД (ИД)", customCategory10, freight, sku);
        customCategory10FreightSku.setDerivedForcedChange(addJProp(baseLM.and1, customCategory10Sku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        sidCustomCategory10FreightSku = addJProp(baseGroup, "sidCustomCategory10FreightSku", "ТН ВЭД", sidCustomCategory10, customCategory10FreightSku, 1, 2);

        addConstraint(addJProp("Для SKU должен быть задан ТН ВЭД", and(true, false), is(freightChanged), 1, customCategory10FreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        customCategory6FreightSku = addJProp(idGroup, "customCategory6FreightSku", "ТН ВЭД", customCategory6CustomCategory10, customCategory10FreightSku, 1, 2);

        quantityImporterFreightCustomCategory6 = addSGProp(baseGroup, "quantityImporterFreightCustomCategory6", "Кол-во", quantityImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);

        mainCompositionOriginFreightSku = addDProp(baseGroup, "mainCompositionOriginFreightSku", "Состав", COMPOSITION_CLASS, freight, sku);
        mainCompositionOriginFreightSku.setDerivedForcedChange(addJProp(baseLM.and1, mainCompositionOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        additionalCompositionOriginFreightSku = addDProp(baseGroup, "additionalCompositionOriginFreightSku", "Доп. состав", COMPOSITION_CLASS, freight, sku);
        additionalCompositionOriginFreightSku.setDerivedForcedChange(addJProp(baseLM.and1, additionalCompositionOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        translationMainCompositionSku = addJProp(actionGroup, true, "translationMainCompositionSku", "Перевод состава", addTAProp(mainCompositionOriginSku, mainCompositionSku), dictionaryComposition, 1);
        translationAdditionalCompositionSku = addJProp(actionGroup, true, "translationAdditionalCompositionSku", "Перевод доп. состава", addTAProp(additionalCompositionOriginSku, additionalCompositionSku), dictionaryComposition, 1);

        mainCompositionFreightSku = addDProp(baseGroup, "mainCompositionFreightSku", "Состав (перевод)", COMPOSITION_CLASS, freight, sku);
        mainCompositionFreightSku.setDerivedForcedChange(addJProp(baseLM.and1, mainCompositionSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        addConstraint(addJProp("Для SKU должен быть задан состав", and(true, false), is(freightChanged), 1, mainCompositionFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        additionalCompositionFreightSku = addDProp(baseGroup, "additionalCompositionFreightSku", "Доп. состав (перевод)", COMPOSITION_CLASS, freight, sku);
        additionalCompositionFreightSku.setDerivedForcedChange(addJProp(baseLM.and1, additionalCompositionSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        countryOfOriginFreightSku = addDProp(idGroup, "countryOfOriginFreightSku", "Страна (ИД)", baseLM.country, freight, sku);
        countryOfOriginFreightSku.setDerivedForcedChange(addJProp(baseLM.and1, countryOfOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        addConstraint(addJProp("Для SKU должна быть задана страна", and(true, false), is(freightChanged), 1, countryOfOriginFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        sidCountryOfOriginFreightSku = addJProp(baseGroup, "sidCountryOfOriginFreightSku", "Код страны", baseLM.sidCountry, countryOfOriginFreightSku, 1, 2);
        nameCountryOfOriginFreightSku = addJProp(baseGroup, "nameCountryOfOriginFreightSku", "Страна", baseLM.name, countryOfOriginFreightSku, 1, 2);

        quantityImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "quantityImporterFreightArticleCompositionCountryCategory", "Кол-во",
                quantityProxyImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        compositionFreightArticleCompositionCountryCategory = addMGProp(baseGroup, "compositionFreightArticleCompositionCountryCategory", "Состав",
                mainCompositionFreightSku, 1, articleSku, 2, mainCompositionOriginFreightSku, 1, 2, countryOfOriginFreightSku, 1, 2, customCategory10FreightSku, 1, 2);

        netWeightStockSku = addJProp(baseGroup, "netWeightStockSku", "Вес нетто", baseLM.multiplyDouble2, quantityStockSku, 1, 2, netWeightSku, 2);
        netWeightStockArticle = addSGProp(baseGroup, "netWeightStockArticle", "Вес нетто", netWeightStockSku, 1, articleSku, 2);
        netWeightStock = addSGProp(baseGroup, "netWeightStock", "Вес нетто короба", netWeightStockSku, 1);

        netWeightFreightSku = addDProp(baseGroup, "netWeightFreightSku", "Вес нетто единицы товара", DoubleClass.instance, freight, sku);
        netWeightFreightSku.setDerivedForcedChange(addJProp(baseLM.and1, netWeightSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        addConstraint(addJProp("Для SKU должен быть задан вес нетто", and(true, false), is(freightChanged), 1, netWeightFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        netWeightImporterFreightUnitSku = addJProp(baseGroup, "netWeightImporterFreightUnitSku", "Вес нетто", baseLM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, addJProp(netWeightFreightSku, freightFreightUnit, 1, 2), 2, 3);
        netWeightImporterFreightUnitArticle = addSGProp(baseGroup, "netWeightImporterFreightUnitArticle", "Вес нетто", netWeightImporterFreightUnitSku, 1, 2, articleSku, 3);

        netWeightImporterFreightUnit = addSGProp(baseGroup, "netWeightImporterFreightUnit", "Вес нетто", netWeightImporterFreightUnitSku, 1, 2);

        netWeightImporterFreightSku = addJProp(baseGroup, "netWeightImporterFreightSku", "Вес нетто", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);

        netWeightImporterFreightCustomCategory6 = addSGProp(baseGroup, "netWeightImporterFreightCustomCategory6", "Вес нетто", netWeightImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);

        netWeightImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "netWeightImporterFreightArticleCompositionCountryCategory", "Вес нетто",
                netWeightImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        netWeightImporterFreight = addSGProp(baseGroup, "netWeightImporterFreight", "Вес нетто", netWeightImporterFreightSku, 1, 2);
        //netWeightImporterFreightBox = addSGProp(baseGroup, "netWeightImporterFreight", "Вес нетто", netWeightImporterFreightSku, 1, 2);

        priceImporterFreightSku = addDProp(baseGroup, "priceImporterFreightSku", "Цена входная", DoubleClass.instance, importer, freight, sku);
        priceMaxImporterFreightSku = addMGProp(baseGroup, "priceMaxImporterFreightSku", false, "Цена входная", priceInInvoiceFreightUnitSku, importerInvoice, 1, freightFreightUnit, 2, 3);
        priceInImporterFreightSku = addSUProp(baseGroup, "priceInImporterFreightSku", "Цена входная", Union.OVERRIDE, priceMaxImporterFreightSku, priceImporterFreightSku);

        sumInImporterFreightSku = addJProp(baseGroup, "sumInImporterFreightSku", "Сумма входная", baseLM.multiplyDouble2, quantityProxyImporterFreightSku, 1, 2, 3, priceInImporterFreightSku, 1, 2, 3);

        sumFreightImporterFreightSku = addPGProp(baseGroup, "sumFreightImporterFreightSku", false, 2, false, "Сумма фрахта",
                netWeightImporterFreightSku,
                sumFreightFreight, 2);

        LP priceAggrFreightImporterFreightSku = addJProp(baseLM.divideDouble2, sumFreightImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);

//        priceFreightImporterFreightSku = addJProp(baseGroup, "priceFreightImporterFreightSku", "Цена за фрахт", divideDouble2, sumFreightImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        priceFreightImporterFreightSku = addDProp(baseGroup, "priceFreightImporterFreightSku", "Цена за фрахт", DoubleClass.instance, importer, freight, sku);
        priceFreightImporterFreightSku.setDerivedForcedChange(priceAggrFreightImporterFreightSku, 1, 2, 3, is(freightPriced), 2, sumFreightFreight, 2);

        priceExpenseImporterFreightSku = addJProp(baseGroup, "priceExpenseImporterFreightSku", "Цена затр.", baseLM.sumDouble2, priceInImporterFreightSku, 1, 2, 3, priceFreightImporterFreightSku, 1, 2, 3);

        markupPercentImporterFreightBrandSupplier = addDProp(baseGroup, "markupPercentImporterFreightBrandSupplier", "Надбавка (%)", DoubleClass.instance, importer, freight, brandSupplier);
        markupPercentImporterFreightDataSku = addDProp(baseGroup, "markupPercentImporterFreightDataSku", "Надбавка (%)", DoubleClass.instance, importer, freight, sku);
        markupPercentImporterFreightBrandSupplierSku = addJProp(baseGroup, "markupPercentImporterFreightBrandSupplierSku", true, "Надбавка (%)", markupPercentImporterFreightBrandSupplier, 1, 2, brandSupplierArticleSku, 3);
        markupPercentImporterFreightSku = addSUProp(baseGroup, "markupPercentImporterFreightSku", true, "Надбавка (%)", Union.OVERRIDE, markupPercentImporterFreightBrandSupplierSku, markupPercentImporterFreightDataSku);

        // надбавка на цену без учёта стоимости фрахта
        markupInImporterFreightSku = addJProp(baseGroup, "markupInImporterFreightSku", "Надбавка", baseLM.percent2, priceInImporterFreightSku, 1, 2, 3, markupPercentImporterFreightSku, 1, 2, 3);

        priceMarkupInImporterFreightSku = addJProp(baseGroup, "priceMarkupInImporterFreightSku", "Цена выходная", baseLM.sumDouble2, priceInImporterFreightSku, 1, 2, 3, markupInImporterFreightSku, 1, 2, 3);

        priceInOutImporterFreightSku = addDProp(baseGroup, "priceInOutImporterFreightSku", "Цена выходная", DoubleClass.instance, importer, freight, sku);
        priceInOutImporterFreightSku.setDerivedForcedChange(true, addJProp(baseLM.and1, priceMarkupInImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3), 1, 2, 3, is(freightPriced), 2);

        priceImporterFreightArticleCompositionCountryCategory = addMGProp(baseGroup, "priceImporterFreightArticleCompositionCountryCategory", "Цена",
                priceInOutImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        sumImporterFreightSku = addJProp(baseGroup, "sumImporterFreightSku", "Сумма", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);
        sumImporterFreightCustomCategory6 = addSGProp(baseGroup, "sumImporterFreightCustomCategory6", "Сумма", sumImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);

        sumImporterFreightUnitSku = addJProp(baseGroup, "sumImporterFreightUnitSku", "Сумма", baseLM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, addJProp(priceInOutImporterFreightSku, 1, freightFreightUnit, 2, 3), 1, 2, 3);

        sumImporterFreightUnitArticle = addSGProp(baseGroup, "sumImporterFreightUnitArticle", "Сумма", sumImporterFreightUnitSku, 1, 2, articleSku, 3);

        sumImporterFreightArticleCompositionCountryCategory = addJProp(baseGroup, "sumImporterFreightArticleCompositionCountryCategory", "Сумма", baseLM.multiplyDouble2,
                quantityImporterFreightArticleCompositionCountryCategory, 1, 2, 3, 4, 5, 6,
                priceImporterFreightArticleCompositionCountryCategory, 1, 2, 3, 4, 5, 6);

        sumImporterFreight = addSGProp(baseGroup, "sumImporterFreight", "Сумма выходная", sumImporterFreightArticleCompositionCountryCategory, 1, 2);

        sumSbivkaImporterFreight = addSGProp(baseGroup, "sumSbivkaImporterFreight", "Сумма выходная", sumImporterFreightSku, 1, 2);

        sumMarkupInImporterFreightSku = addJProp(baseGroup, "sumMarkupInImporterFreightSku", "Сумма надбавки", baseLM.multiplyDouble2, quantityProxyImporterFreightSku, 1, 2, 3, markupInImporterFreightSku, 1, 2, 3);
        sumInOutImporterFreightSku = addJProp(baseGroup, "sumInOutImporterFreightSku", "Сумма выходная", baseLM.multiplyDouble2, quantityProxyImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);

        sumMarkupInImporterFreight = addSGProp(baseGroup, "sumMarkupInImporterFreight", "Сумма надбавки", sumMarkupInImporterFreightSku, 1, 2);
        sumInOutImporterFreight = addSGProp(baseGroup, "sumInOutImporterFreight", "Сумма выходная", sumInOutImporterFreightSku, 1, 2);

        sumMarkupInFreight = addSGProp(baseGroup, "sumMarkupInFreight", "Сумма надбавки", sumMarkupInImporterFreight, 2);
        sumInOutFreight = addSGProp(baseGroup, "sumInOutFreight", "Сумма выходная", sumInOutImporterFreight, 2);

        // надбавка на цену с учётом стоимости фрахта
        markupImporterFreightSku = addJProp(baseGroup, "markupImporterFreightSku", "Надбавка", baseLM.percent2, priceExpenseImporterFreightSku, 1, 2, 3, markupPercentImporterFreightSku, 1, 2, 3);
        sumMarkupImporterFreightSku = addJProp(baseGroup, "sumMarkupImporterFreightSku", "Сумма надбавки", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, markupImporterFreightSku, 1, 2, 3);

        priceOutImporterFreightSku = addJProp(baseGroup, "priceOutImporterFreightSku", "Цена выходная", baseLM.sumDouble2, priceExpenseImporterFreightSku, 1, 2, 3, markupImporterFreightSku, 1, 2, 3);
        sumOutImporterFreightSku = addJProp(baseGroup, "sumOutImporterFreightSku", "Сумма выходная", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceOutImporterFreightSku, 1, 2, 3);

        sumInImporterFreight = addSGProp(baseGroup, "sumInImporterFreight", "Сумма входная", sumInImporterFreightSku, 1, 2);
        sumMarkupImporterFreight = addSGProp(baseGroup, "sumMarkupImporterFreight", "Сумма надбавки", sumMarkupImporterFreightSku, 1, 2);

        sumOutImporterFreight = addSGProp(baseGroup, "sumOutImporterFreight", "Сумма выходная", sumOutImporterFreightSku, 1, 2);

        sumInFreight = addSGProp(baseGroup, "sumInFreight", "Сумма входная", sumInImporterFreight, 2);
        sumMarkupFreight = addSGProp(baseGroup, "sumMarkupFreight", "Сумма надбавки", sumMarkupImporterFreight, 2);
        sumOutFreight = addSGProp(baseGroup, "sumOutFreight", "Сумма выходная", sumOutImporterFreight, 2);

        // итоги с начала года
        sumInCurrentYear = addSGProp(baseGroup, "sumInCurrentYear", "Итого вход", addJProp(baseLM.and1, sumInFreight, 1, addJProp(baseLM.equals2, addJProp(baseLM.yearInDate, baseLM.currentDate), addJProp(baseLM.yearInDate, baseLM.date, 1), 1), 1));
        sumInOutCurrentYear = addSGProp(baseGroup, "sumInOutCurrentYear", "Итого выход", addJProp(baseLM.and1, sumInOutFreight, 1, addJProp(baseLM.equals2, addJProp(baseLM.yearInDate, baseLM.currentDate), addJProp(baseLM.yearInDate, baseLM.date, 1), 1), 1));
        balanceSumCurrentYear = addDUProp(baseGroup, "balanceSumCurrentYear", "Сальдо", sumInOutCurrentYear, sumInCurrentYear);

        quantityImporterFreightBrandSupplier = addSGProp(baseGroup, "quantityImporterFreightBrandSupplier", "Кол-во позиций", quantityImporterFreightSku, 1, 2, brandSupplierArticleSku, 3);

        quantityImporterFreight = addSGProp(baseGroup, "quantityImporterFreight", "Кол-во позиций", quantityProxyImporterFreightSku, 1, 2);
        quantitySbivkaImporterFreight = addSGProp(baseGroup, "quantitySbivkaImporterFreight", "Кол-во позиций", quantityImporterFreightSku, 1, 2);

        // Текущие палеты/коробки для приема
        currentPalletRouteUser = addDProp("currentPalletRouteUser", "Тек. паллета (ИД)", pallet, route, baseLM.user);

        currentPalletRoute = addJProp(true, "currentPalletRoute", "Тек. паллета (ИД)", currentPalletRouteUser, 1, baseLM.currentUser);
        barcodeCurrentPalletRoute = addJProp("barcodeCurrentPalletRoute", "Тек. паллета (штрих-код)", baseLM.barcode, currentPalletRoute, 1);

        sumNetWeightFreightSku = addJProp(baseGroup, "sumNetWeightFreightSku", "Вес нетто (всего)", baseLM.multiplyDouble2, quantityFreightSku, 1, 2, netWeightSku, 2);

        grossWeightCurrentPalletRoute = addJProp(true, "grossWeightCurrentPalletRoute", "Вес брутто", grossWeightPallet, currentPalletRoute, 1);
        grossWeightFreight = addSUProp(baseGroup, "freightGrossWeight", "Вес брутто (фрахт)", Union.SUM,
                addSGProp(grossWeightPallet, freightPallet, 1),
                addSGProp(grossWeightDirectInvoice, freightDirectInvoice, 1));

        sumGrossWeightFreightSku = addPGProp(baseGroup, "sumGrossWeightFreightSku", false, 1, false, "Вес брутто",
                sumNetWeightFreightSku,
                grossWeightFreight, 1);

        grossWeightFreightSkuAggr = addJProp(baseGroup, "grossWeightFreightSkuAggr", "Вес брутто", baseLM.divideDouble2, sumGrossWeightFreightSku, 1, 2, quantityFreightSku, 1, 2);
        grossWeightFreightSku = addDProp(baseGroup, "grossWeightFreightSku", "Вес брутто", DoubleClass.instance, freight, sku);
        grossWeightFreightSku.setDerivedForcedChange(addJProp(baseLM.and1, grossWeightFreightSkuAggr, 1, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        grossWeightImporterFreightSku = addJProp(baseGroup, "grossWeightImporterFreightSku", "Вес брутто", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, grossWeightFreightSku, 2, 3);

        grossWeightImporterFreightCustomCategory6 = addSGProp(baseGroup, "grossWeightImporterFreightCustomCategory6", "Вес брутто", grossWeightImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);

        grossWeightImporterFreight = addSGProp(baseGroup, "grossWeightImporterFreight", "Вес брутто", grossWeightImporterFreightSku, 1, 2);
        grossWeightImporterFreightUnitSku = addJProp(baseGroup, "grossWeightImporterFreightUnitSku", "Вес брутто", baseLM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, addJProp(grossWeightFreightSku, freightFreightUnit, 2, 3), 1, 2, 3);
        grossWeightImporterFreightUnitArticle = addSGProp(baseGroup, "grossWeightImporterFreightUnitArticle", "Вес брутто", grossWeightImporterFreightUnitSku, 1, 2, articleSku, 3);
        grossWeightImporterFreightUnit = addSGProp(baseGroup, "grossWeightImporterFreightUnit", "Вес брутто", grossWeightImporterFreightUnitSku, 1, 2);

        grossWeightImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "grossWeightImporterFreightArticleCompositionCountryCategory", "Вес брутто",
                grossWeightImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        currentFreightBoxRouteUser = addDProp("currentFreightBoxRouteUser", "Тек. короб (ИД)", freightBox, route, baseLM.user);

        currentFreightBoxRoute = addJProp(true, "currentFreightBoxRoute", "Тек. короб (ИД)", currentFreightBoxRouteUser, 1, baseLM.currentUser);
        barcodeCurrentFreightBoxRoute = addJProp("barcodeCurrentFreightBoxRoute", "Тек. короб (штрих-код)", baseLM.barcode, currentFreightBoxRoute, 1);

        destinationCurrentFreightBoxRoute = addJProp(true, "destinationCurrentFreightBoxRoute", "Пункт назначения тек. короба (ИД)", destinationFreightBox, currentFreightBoxRoute, 1);
        nameDestinationCurrentFreightBoxRoute = addJProp("nameDestinationCurrentFreightBoxRoute", "Пункт назначения тек. короба", baseLM.name, destinationCurrentFreightBoxRoute, 1);

        isCurrentFreightBox = addJProp(baseLM.equals2, addJProp(true, currentFreightBoxRoute, routeCreationFreightBoxFreightBox, 1), 1, 1);
        isCurrentPallet = addJProp(baseLM.equals2, addJProp(true, currentPalletRoute, routeCreationPalletPallet, 1), 1, 1);
        currentPalletFreightBox = addJProp(currentPalletRoute, routeCreationFreightBoxFreightBox, 1);
        isCurrentPalletFreightBox = addJProp(baseLM.equals2, palletFreightBox, 1, currentPalletFreightBox, 1);
        isStoreFreightBoxSupplierBox = addJProp(baseLM.equals2, destinationFreightBox, 1, destinationSupplierBox, 2);

        barcodeActionSeekPallet = addJProp(true, "Найти палету", isCurrentPallet, baseLM.barcodeToObject, 1);
        barcodeActionCheckPallet = addJProp(true, "Проверка паллеты",
                addJProp(true, and(false, true),
                        addStopActionProp("Для маршрута выбранного короба не задана паллета", "Поиск по штрих-коду"),
                        is(freightBox), 1,
                        currentPalletFreightBox, 1), baseLM.barcodeToObject, 1);
        barcodeActionSeekFreightBox = addJProp(true, "Найти короб для транспортировки", isCurrentFreightBox, baseLM.barcodeToObject, 1);
        barcodeActionSetPallet = addJProp(true, "Установить паллету", isCurrentPalletFreightBox, baseLM.barcodeToObject, 1);
        barcodeActionSetStore = addJProp(true, "Установить магазин", isStoreFreightBoxSupplierBox, baseLM.barcodeToObject, 1, 2);

        changePallet = addJProp(true, "Изменить паллету", isCurrentPalletFreightBox, currentFreightBoxRoute, 1);

        barcodeActionSetPalletFreightBox = addJProp(true, "Установить паллету", equalsPalletFreightBox, baseLM.barcodeToObject, 1, 2);

        barcodeActionSetFreight = addJProp(true, "Установить фрахт", equalsPalletFreight, baseLM.barcodeToObject, 1, 2);

        addBoxShipmentDetailBoxShipmentSupplierBoxRouteBarcode = addJProp(true, "Добавить строку поставки",
                addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode, 1, 2, currentFreightBoxRoute, 3, 4);

        addSimpleShipmentDetailSimpleShipmentRouteBarcode = addJProp(true, "Добавить строку поставки",
                addSimpleShipmentSimpleShipmentDetailStockBarcode, 1, currentFreightBoxRoute, 2, 3);

        quantityRouteSku = addJProp(baseGroup, "quantityRouteSku", "Оприход. в короб для транспортировки", quantityStockSku, currentFreightBoxRoute, 1, 2);

        quantitySupplierBoxBoxShipmentRouteSku = addJProp(baseGroup, true, "quantitySupplierBoxBoxShipmentRouteSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4);
        quantitySimpleShipmentRouteSku = addJProp(baseGroup, true, "quantitySimpleShipmentRouteSku", "Кол-во оприход.",
                quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3);

        createFreightBox = addJProp(true, "Сгенерировать короба", addAAProp(freightBox, baseLM.barcode, baseLM.barcodePrefix, true), quantityCreationFreightBox, 1);
        createPallet = addJProp(true, "Сгенерировать паллеты", addAAProp(pallet, baseLM.barcode, baseLM.barcodePrefix, true), quantityCreationPallet, 1);
        createStamp = addJProp(true, "Сгенерировать марки", addAAProp(stamp, baseLM.barcode, baseLM.barcodePrefix, true), firstNumberCreationStamp, 1);

        barcodeActionCheckFreightBox = addJProp(true, "Проверка короба для транспортировки",
                addJProp(true, and(false, false, true),
                        addStopActionProp("Для выбранного маршрута не задан короб для транспортировки", "Поиск по штрих-коду"),
                        is(sku), 2,
                        is(route), 1,
                        currentFreightBoxRoute, 1), 1, baseLM.barcodeToObject, 2);
        barcodeAction4 = addJProp(true, "Ввод штрих-кода 4",
                addCUProp(
                        addSCProp(addJProp(true, quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4))
                ), 1, 2, 3, baseLM.barcodeToObject, 4);
        barcodeAction3 = addJProp(true, "Ввод штрих-кода 3",
                addCUProp(
                        addSCProp(addJProp(true, quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3))
                ), 1, 2, baseLM.barcodeToObject, 3);
    }

    public LP addDEAProp() {
        return addProperty(null, new LP<ClassPropertyInterface>(new DeclarationExportActionProperty("declarationExport", "Экспорт декларанта", BL, importer, freight)));
    }

    @Override
    public void initIndexes() {
    }

    @Override
    public void initNavigators() throws JRException, FileNotFoundException {
        NavigatorElement classifier = new NavigatorElement(baseLM.baseElement, "classifier", "Справочники");
        NavigatorElement classifierCurrency = new NavigatorElement(classifier, "classifierCurrency", "Валюты и курсы");
        classifierCurrency.add(currency.getClassForm(baseLM));
        classifierCurrency.add(typeExchange.getClassForm(baseLM));
        addFormEntity(new RateCurrencyFormEntity(classifierCurrency, "rateCurrencyForm", "Курсы валют"));

        NavigatorElement classifierItem = new NavigatorElement(classifier, "classifierItem", "Для описания товаров");
        addFormEntity(new CustomCategoryFormEntity(classifierItem, "customCategoryForm", "ТН ВЭД (изменения)", false));
        addFormEntity(new CustomCategoryFormEntity(classifierItem, "customCategoryForm2", "ТН ВЭД (дерево)", true));
        //classifierItem.add(category.getClassForm(BL));

        AbstractClassFormEntity categoryForm = new CategoryFormEntity("categoryForm", "Номенклатурные группы");
        classifierItem.add(categoryForm);
        addFormEntity(categoryForm);

        classifierItem.add(commonSize.getClassForm(baseLM));
        classifierItem.add(season.getClassForm(baseLM));
        classifierItem.add(baseLM.country.getClassForm(baseLM));
        classifierItem.add(unitOfMeasure.getClassForm(baseLM));

        addFormEntity(new GlobalParamFormEntity(classifier, "globalParamForm", "Общие параметры"));
        addFormEntity(new ColorSizeSupplierFormEntity(classifier, "сolorSizeSupplierForm", "Поставщики"));
        classifier.add(importer.getClassForm(baseLM));
        classifier.add(exporter.getClassForm(baseLM));
        classifier.add(store.getClassForm(baseLM));
        addFormEntity(new ContractFormEntity(classifier, "contractForm", "Договора"));
        addFormEntity(new NomenclatureFormEntity(classifier, "nomenclatureForm", "Номенклатура"));
        classifier.add(freightType.getClassForm(baseLM));

        createItemForm = addFormEntity(new CreateItemFormEntity(null, "createItemForm", "Ввод товара"));

        NavigatorElement printForms = new NavigatorElement(baseLM.baseElement, "printForms", "Печатные формы");

        addFormEntity(new AnnexInvoiceFormEntity(printForms, "annexInvoiceForm", "Приложение к инвойсу", false));
        invoiceFromFormEntity = addFormEntity(new AnnexInvoiceFormEntity(printForms, "annexInvoiceForm2", "Приложение к инвойсу (перевод)", true));
        addFormEntity(new InvoiceFromFormEntity(printForms, "invoiceFromForm", "Исходящие инвойсы", false));
        addFormEntity(new InvoiceFromFormEntity(printForms, "invoiceFromForm2", "Исходящие инвойсы (перевод)", true));

        addFormEntity(new ProformInvoiceFormEntity(printForms, "proformInvoiceForm", "Исходящие инвойсы-проформы", false));
        addFormEntity(new ProformInvoiceFormEntity(printForms, "proformInvoiceForm2", "Исходящие инвойсы-проформы (перевод)", true));

        addFormEntity(new SbivkaFormEntity(printForms, "sbivkaForm", "Сбивка товаров"));
        addFormEntity(new PackingListFormEntity(printForms, "packingListForm", "Исходящие упаковочные листы"));
        addFormEntity(new PackingListBoxFormEntity(printForms, "packingListBoxForm", "Упаковочные листы коробов"));
        FormEntity createPalletForm = addFormEntity(new CreatePalletFormEntity(printForms, "createPalletForm", "Штрих-коды паллет", FormType.PRINT));
        FormEntity createFreightBoxForm = addFormEntity(new CreateFreightBoxFormEntity(printForms, "createFreightBoxForm", "Штрих-коды коробов", FormType.PRINT));

        NavigatorElement purchase = new NavigatorElement(baseLM.baseElement, "purchase", "Управление закупками");
        addFormEntity(new OrderFormEntity(purchase, "orderForm", "Заказы"));
        addFormEntity(new InvoiceFormEntity(purchase, "boxInvoiceForm", "Инвойсы по коробам", true));
        //addFormEntity(new InvoiceFormEntity(purchase, "simpleInvoiceForm", "Инвойсы без коробов", false));
        addFormEntity(new ShipmentListFormEntity(purchase, "boxShipmentListForm", "Поставки по коробам", true));
        //addFormEntity(new ShipmentListFormEntity(purchase, "simpleShipmentListForm", "Поставки без коробов", false));
        addFormEntity(new PricatFormEntity(purchase, "pricatForm", "Прайсы"));

        NavigatorElement shipment = new NavigatorElement(baseLM.baseElement, "shipment", "Управление фрахтами");
        addFormEntity(new FreightShipmentFormEntity(shipment, "freightShipmentForm", "Комплектация фрахта"));
        addFormEntity(new FreightInvoiceFormEntity(shipment, "freightInvoiceForm", "Расценка фрахта"));
        addFormEntity(new FreightChangeFormEntity(shipment, "freightChangeForm", "Обработка фрахта"));

        NavigatorElement distribution = new NavigatorElement(baseLM.baseElement, "distribution", "Управление складом");
        FormEntity createPalletFormCreate = addFormEntity(new CreatePalletFormEntity(distribution, "createPalletFormAdd", "Сгенерировать паллеты", FormType.ADD));
        addFormEntity(new CreatePalletFormEntity(createPalletFormCreate, "createPalletFormList", "Документы генерации паллет", FormType.LIST));
        FormEntity createFreightBoxFormAdd = addFormEntity(new CreateFreightBoxFormEntity(distribution, "createFreightBoxFormAdd", "Сгенерировать короба", FormType.ADD));
        addFormEntity(new CreateFreightBoxFormEntity(createFreightBoxFormAdd, "createFreightBoxFormList", "Документы генерации коробов", FormType.LIST));
        FormEntity createStampFormAdd = addFormEntity(new CreateStampFormEntity(distribution, "createStampFormAdd", "Сгенерировать марки", FormType.ADD));
        addFormEntity(new CreateStampFormEntity(createStampFormAdd, "createStampFormList", "Документы генерации марок", FormType.LIST));
        addFormEntity(new ShipmentSpecFormEntity(distribution, "boxShipmentSpecForm", "Прием товара по коробам", true));
        addFormEntity(new ShipmentSpecFormEntity(distribution, "simpleShipmentSpecForm", "Прием товара без коробов", false));

        addFormEntity(new BoxPalletStoreFormEntity(distribution, "boxPalletStoreForm", "Расстановка коробов по паллетам"));
        addFormEntity(new FreightShipmentStoreFormEntity(distribution, "freightShipmentStoreForm", "Комплектация фрахта (на складе)"));
        addFormEntity(new BalanceBrandWarehouseFormEntity(distribution, "balanceBrandWarehouseForm", "Остатки на складе (по брендам)"));
        addFormEntity(new BalanceWarehouseFormEntity(distribution, "balanceWarehouseForm", "Остатки на складе"));
        addFormEntity(new InvoiceShipmentFormEntity(distribution, "invoiceShipmentForm", "Сравнение по инвойсам"));

        category.setDialogForm(categoryForm);
        // пока не поддерживается из-за того, что пока нет расчета себестоимости для внутреннего перемещения
//        addFormEntity(new StockTransferFormEntity(distribution, "stockTransferForm", "Внутреннее перемещение"));
    }

    private class BarcodeFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objBarcode;

        protected Font getDefaultFont() {
            return null;
        }

        private BarcodeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseLM.objectValue, baseLM.barcodeObjectName);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

            objBarcode.resetOnApply = true;

            addPropertyDraw(baseLM.reverseBarcode);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            if (getDefaultFont() != null)
                design.setFont(getDefaultFont());

            PropertyDrawView barcodeView = design.get(getPropertyDraw(baseLM.objectValue, objBarcode));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(baseLM.reverseBarcode)));
//            design.getPanelContainer(design.get(objBarcode.groupTo)).constraints.maxVariables = 0;

            design.setBackground(baseLM.barcodeObjectName, new Color(240, 240, 240));

            design.setEditKey(barcodeView, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
            design.setEditKey(baseLM.reverseBarcode, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

            design.setFocusable(baseLM.reverseBarcode, false);
            design.setFocusable(false, objBarcode.groupTo);

            return design;
        }
    }

    private class CategoryFormEntity extends AbstractClassFormEntity<RomanBusinessLogics> {

        private ObjectEntity objCategory;

        private CategoryFormEntity(String sID, String caption) {
            super(sID, caption);

            objCategory = addSingleGroupObject(category, "Номенклатурная группа", baseGroup);
            objCategory.groupTo.initClassView = ClassViewType.GRID;
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.defaultOrders.put(design.get(getPropertyDraw(baseLM.name)), true);

            return design;
        }

        @Override
        public ObjectEntity getObject() {
            return objCategory;
        }

        @Override
        protected AbstractClassFormEntity copy() {
            return new CategoryFormEntity(getSID() + "_copy" + copies++, caption);
        }
    }



    private class GlobalParamFormEntity extends FormEntity<RomanBusinessLogics> {

        private GlobalParamFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            addPropertyDraw(nameDictionaryComposition);
            addPropertyDraw(nameTypeExchangeSTX);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            return design;
        }
    }

    private class RateCurrencyFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objTypeExchange;
        private ObjectEntity objCurrency;
        private ObjectEntity objDate;
        private ObjectEntity objDateRate;

        private RateCurrencyFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objTypeExchange = addSingleGroupObject(typeExchange, "Тип обмена", baseLM.objectValue, baseLM.name, nameCurrencyTypeExchange);
            objTypeExchange.groupTo.initClassView = ClassViewType.PANEL;

            objCurrency = addSingleGroupObject(currency, "Валюта", baseLM.name);
            objCurrency.groupTo.initClassView = ClassViewType.GRID;

            objDate = addSingleGroupObject(DateClass.instance, "Дата", baseLM.objectValue);
            objDate.groupTo.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(rateExchange, objTypeExchange, objCurrency, objDate);

            objDateRate = addSingleGroupObject(DateClass.instance, "Дата", baseLM.objectValue);

            addPropertyDraw(rateExchange, objTypeExchange, objCurrency, objDateRate);
            setReadOnly(rateExchange, true, objDateRate.groupTo);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(rateExchange, objTypeExchange, objCurrency, objDateRate)));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objCurrency.groupTo).grid.constraints.fillVertical = 1;
            design.get(objDateRate.groupTo).grid.constraints.fillVertical = 3;

            return design;
        }
    }

    private class PackingListBoxFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objBox;
        private ObjectEntity objArticle;

        private PackingListBoxFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objBox = addSingleGroupObject(1, "box", freightBox, "Короб", baseLM.barcode, netWeightStock);
            objBox.groupTo.initClassView = ClassViewType.PANEL;

            objArticle = addSingleGroupObject(2, "article", article, "Артикул", sidArticle, nameBrandSupplierArticle, nameArticle);

            addPropertyDraw(quantityStockArticle, objBox, objArticle);
            addPropertyDraw(netWeightStockArticle, objBox, objArticle);
            objArticle.groupTo.initClassView = ClassViewType.GRID;

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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name, nameCurrencySupplier);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objOrder = addSingleGroupObject(order, "Заказ", baseLM.date, sidDocument, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);
            addObjectActions(this, objOrder);

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", baseLM.objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, objOrder, objSIDArticleComposite));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(seekArticleSIDSupplier, objSIDArticleComposite, objSupplier));

            objSIDArticleSingle = addSingleGroupObject(StringClass.get(50), "Ввод простого артикула", baseLM.objectValue);
            objSIDArticleSingle.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(addNEArticleSingleSIDSupplier, objSIDArticleSingle, objSupplier));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(incrementNumberListSID, objOrder, objSIDArticleSingle));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(seekArticleSIDSupplier, objSIDArticleSingle, objSupplier));

            objArticle = addSingleGroupObject(article, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, objOrder, objArticle);
            addPropertyDraw(objArticle, sidArticle, sidBrandSupplierArticle, nameBrandSupplierArticle, nameSeasonArticle, nameThemeSupplierArticle, originalNameArticle, nameCountrySupplierOfOriginArticle, nameCountryOfOriginArticle, baseLM.barcode);
            addPropertyDraw(quantityListArticle, objOrder, objArticle);
            addPropertyDraw(priceDocumentArticle, objOrder, objArticle);
            addPropertyDraw(RRPDocumentArticle, objOrder, objArticle);
            addPropertyDraw(sumDocumentArticle, objOrder, objArticle);
            addPropertyDraw(invoicedOrderArticle, objOrder, objArticle);
            addPropertyDraw(baseLM.delete, objArticle);

            objItem = addSingleGroupObject(item, "Товар", baseLM.barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            addObjectActions(this, objItem);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", baseLM.selection, sidSizeSupplier);
            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", baseLM.selection, sidColorSupplier, baseLM.name);

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

            design.get(getPropertyDraw(sidDocument, objOrder)).caption = "Номер заказа";
            design.get(getPropertyDraw(baseLM.date, objOrder)).caption = "Дата заказа";

            design.defaultOrders.put(design.get(getPropertyDraw(numberListArticle)), true);

            design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleSingle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name, nameCurrencySupplier, importInvoiceActionGroup, true);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс", baseLM.date, baseLM.objectClassName, sidDocument, nameCurrencyDocument, sumDocument, quantityDocument, netWeightDocument, nameImporterInvoice, sidContractInvoice, sidDestinationDestinationDocument, nameDestinationDestinationDocument);
            addObjectActions(this, objInvoice);

            objOrder = addSingleGroupObject(order, "Заказ");
            objOrder.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inOrderInvoice, objOrder, objInvoice);
            addPropertyDraw(objOrder, baseLM.date, sidDocument, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб", sidSupplierBox, baseLM.barcode);
                objSupplierBox.groupTo.initClassView = ClassViewType.PANEL;
                addObjectActions(this, objSupplierBox);
            }

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", baseLM.objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleComposite));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(seekArticleSIDSupplier, objSIDArticleComposite, objSupplier));

            objSIDArticleSingle = addSingleGroupObject(StringClass.get(50), "Ввод простого артикула", baseLM.objectValue);
            objSIDArticleSingle.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(addNEArticleSingleSIDSupplier, objSIDArticleSingle, objSupplier));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleSingle));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(seekArticleSIDSupplier, objSIDArticleSingle, objSupplier));

            objArticle = addSingleGroupObject(article, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(objArticle, sidArticle, sidBrandSupplierArticle, nameBrandSupplierArticle, nameSeasonArticle, nameThemeSupplierArticle, originalNameArticle, sidCustomCategoryOriginArticle,
                    nameCountrySupplierOfOriginArticle, netWeightArticle, mainCompositionOriginArticle, additionalCompositionOriginArticle, baseLM.barcode);
            addPropertyDraw(quantityListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(priceDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(RRPDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(sumDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(orderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(priceOrderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(baseLM.delete, objArticle);

            objItem = addSingleGroupObject(item, "Товар", baseLM.barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            addObjectActions(this, objItem, objArticle, articleComposite);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", baseLM.selection, sidSizeSupplier);
            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", baseLM.selection, sidColorSupplier, baseLM.name);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityDocumentArticleCompositeColorSize, objInvoice, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.columnGroupObjects.add(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(sidSizeSupplier, objSizeSupplier);

            addPropertyDraw(quantityListSku, (box ? objSupplierBox : objInvoice), objItem);
            addPropertyDraw(priceDocumentSku, objInvoice, objItem);
            addPropertyDraw(priceRateDocumentSku, objInvoice, objItem);
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
            addPropertyDraw(new LP[] {baseLM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                                      sidBrandSupplierArticleSku, nameBrandSupplierArticleSku, originalNameArticleSku,
                                      nameCountrySupplierOfOriginArticleSku, nameCountryOfOriginSku, netWeightSku,
                                      mainCompositionOriginSku, additionalCompositionOriginSku}, objSku);
            addPropertyDraw(quantityListSku, (box ? objSupplierBoxSpec : objInvoice), objSku);
            addPropertyDraw(priceDocumentSku, objInvoice, objSku);
            if (box)
                addPropertyDraw(sumSupplierBoxSku, objSupplierBoxSpec, objSku);
            else
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

            design.get(getPropertyDraw(baseLM.objectClassName, objInvoice)).caption = "Тип инвойса";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(baseLM.date, objInvoice)).caption = "Дата инвойса";

            design.get(getPropertyDraw(sidDocument, objOrder)).caption = "Номер заказа";
            design.get(getPropertyDraw(baseLM.date, objOrder)).caption = "Дата заказа";

            design.defaultOrders.put(design.get(getPropertyDraw(numberListArticle)), true);
            design.defaultOrders.put(design.get(getPropertyDraw(numberListSku)), true);

            design.get(objOrder.groupTo).grid.constraints.fillVertical = 0.7;
            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 0.7;
            design.get(objItem.groupTo).grid.constraints.fillHorizontal = 1.5;

            design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleSingle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", baseLM.date, sidDocument, netWeightShipment, grossWeightShipment, quantityPalletShipment, quantityBoxShipment);//, invoicedShipment, sumShipment
            addObjectActions(this, objShipment);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс");
            objInvoice.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objInvoice, true);

            addPropertyDraw(inInvoiceShipment, objInvoice, objShipment);
            addPropertyDraw(objInvoice, baseLM.date, sidDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            objRoute = addSingleGroupObject(route, "Маршрут", baseLM.name);
            addPropertyDraw(percentShipmentRoute, objShipment, objRoute);
            addPropertyDraw(invoicedShipmentRoute, objShipment, objRoute);
            addPropertyDraw(sumShipmentRoute, objShipment, objRoute);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objShipment), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));

            setReadOnly(objSupplier, true);

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidDocument, objShipment)).caption = "Номер поставки";
            design.get(getPropertyDraw(baseLM.date, objShipment)).caption = "Дата поставки";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(baseLM.date, objInvoice)).caption = "Дата инвойса";

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
                objSIDSupplierBox = addSingleGroupObject(StringClass.get(50), "Номер короба", baseLM.objectValue);
                objSIDSupplierBox.groupTo.setSingleClassView(ClassViewType.PANEL);
            }

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", baseLM.date, sidDocument, invoicedShipment, quantityShipment);
            objShipment.groupTo.initClassView = ClassViewType.PANEL;

            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб поставщика", sidSupplierBox, baseLM.barcode, nameDestinationSupplierBox);
                objSupplierBox.groupTo.initClassView = ClassViewType.PANEL;
            }

            objRoute = addSingleGroupObject(route, "Маршрут", baseLM.name, barcodeCurrentPalletRoute, grossWeightCurrentPalletRoute, barcodeCurrentFreightBoxRoute, nameDestinationCurrentFreightBoxRoute);
            objRoute.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(packingListFormRoute, objRoute);
            addPropertyDraw(changePallet, objRoute);

            nameRoute = addPropertyDraw(baseLM.name, objRoute);
            nameRoute.forceViewType = ClassViewType.PANEL;

            objSku = addSingleGroupObject(sku, "SKU", baseLM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameBrandSupplierArticleSku, originalNameArticleSku, nameOriginCategoryArticleSku, nameOriginUnitOfMeasureArticleSku,
                    netWeightArticleSku, sidCustomCategoryOriginArticleSku, nameCountryOfOriginArticleSku, mainCompositionOriginArticleSku,
                    netWeightSku, nameCountryOfOriginSku, mainCompositionOriginSku, additionalCompositionOriginSku);

            objSku.groupTo.setSingleClassView(ClassViewType.GRID);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            setForceViewType(intraAttributeGroup, ClassViewType.PANEL, objSku.groupTo);
            //getPropertyDraw(nameOriginCategoryArticleSku).forceViewType = ClassViewType.GRID;
            //getPropertyDraw(netWeightArticleSku).forceViewType = ClassViewType.GRID;            

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

                PropertyObjectEntity diffListShipSkuProperty = addPropertyObject(diffListShipSku, objSupplierBox, objShipment, objSku);
                getPropertyDraw(quantityDataListSku).setPropertyHighlight(diffListShipSkuProperty);
                getPropertyDraw(quantityShipDimensionShipmentSku).setPropertyHighlight(diffListShipSkuProperty);

            } else {
                quantityColumn = addPropertyDraw(quantitySimpleShipmentRouteSku, objShipment, objRoute, objSku);
            }

            quantityColumn.columnGroupObjects.add(objRoute.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(baseLM.name, objRoute);

            addPropertyDraw(quantityRouteSku, objRoute, objSku);

            addPropertyDraw(percentShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);
            addPropertyDraw(invoicedShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);
            addPropertyDraw(quantityShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);

            PropertyObjectEntity diffShipmentRouteSkuProperty = addPropertyObject(diffShipmentRouteSku, objShipment, objRoute, objSku);
            getPropertyDraw(invoicedShipmentRouteSku).setPropertyHighlight(diffShipmentRouteSkuProperty);
            getPropertyDraw(quantityShipmentRouteSku).setPropertyHighlight(diffShipmentRouteSkuProperty);

            objShipmentDetail = addSingleGroupObject((box ? boxShipmentDetail : simpleShipmentDetail),
                    baseLM.selection, barcodeSkuShipmentDetail, sidArticleShipmentDetail, sidColorSupplierItemShipmentDetail, nameColorSupplierItemShipmentDetail, sidSizeSupplierItemShipmentDetail,
                    nameBrandSupplierArticleSkuShipmentDetail, sidCustomCategoryOriginArticleSkuShipmentDetail, originalNameArticleSkuShipmentDetail,
                    nameOriginCategoryArticleSkuShipmentDetail, nameOriginUnitOfMeasureArticleSkuShipmentDetail,
                    netWeightArticleSkuShipmentDetail,
                    nameCountryOfOriginArticleSkuShipmentDetail, mainCompositionOriginArticleSkuShipmentDetail,
                    netWeightSkuShipmentDetail, nameCountryOfOriginSkuShipmentDetail,
                    mainCompositionOriginSkuShipmentDetail, additionalCompositionOriginSkuShipmentDetail,
                    sidShipmentShipmentDetail,
                    sidSupplierBoxShipmentDetail, barcodeSupplierBoxShipmentDetail,
                    barcodeStockShipmentDetail, nameRouteFreightBoxShipmentDetail,
                    quantityShipmentDetail, nameUserShipmentDetail, sidStampShipmentDetail, timeShipmentDetail, baseLM.delete);

            objShipmentDetail.groupTo.setSingleClassView(ClassViewType.GRID);

            getPropertyDraw(sidStampShipmentDetail).propertyCaption = addPropertyObject(hideSidStampShipmentDetail, objShipmentDetail);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objShipmentDetail.groupTo);
            setForceViewType(supplierAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);
            setForceViewType(intraAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);

            //getPropertyDraw(nameOriginCategoryArticleSkuShipmentDetail).forceViewType = ClassViewType.GRID;
            //getPropertyDraw(netWeightArticleSkuShipmentDetail).forceViewType = ClassViewType.GRID;

            PropertyObjectEntity oneArticleProperty = addPropertyObject(oneArticleSkuShipmentDetail, objShipmentDetail);
            PropertyObjectEntity oneSkuProperty = addPropertyObject(oneSkuShipmentDetail, objShipmentDetail);
            PropertyObjectEntity oneArticleColorProperty = addPropertyObject(oneArticleColorShipmentDetail, objShipmentDetail);
            PropertyObjectEntity oneArticleSizeProperty = addPropertyObject(oneArticleSizeShipmentDetail, objShipmentDetail);

            getPropertyDraw(nameOriginCategoryArticleSkuShipmentDetail).setPropertyHighlight(oneArticleProperty);
            getPropertyDraw(nameOriginUnitOfMeasureArticleSkuShipmentDetail).setPropertyHighlight(oneArticleProperty);
            getPropertyDraw(netWeightSkuShipmentDetail).setPropertyHighlight(oneArticleSizeProperty);
            getPropertyDraw(nameCountryOfOriginSkuShipmentDetail).setPropertyHighlight(oneArticleColorProperty);
            getPropertyDraw(mainCompositionOriginSkuShipmentDetail).setPropertyHighlight(oneArticleColorProperty);
            getPropertyDraw(additionalCompositionOriginSkuShipmentDetail).setPropertyHighlight(oneArticleColorProperty);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objShipment), Compare.EQUALS, objSupplier));

            if (box)
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(inSupplierBoxShipment, objSupplierBox, objShipment)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            if (box) {
                FilterEntity inSupplierBox = new NotNullFilterEntity(addPropertyObject(quantityListSku, objSupplierBox, objSku));
                FilterEntity inSupplierBoxShipmentStock = new NotNullFilterEntity(addPropertyObject(quantitySupplierBoxBoxShipmentRouteSku, objSupplierBox, objShipment, objRoute, objSku));
                FilterEntity inSupplierBoxShipmentSku = new NotNullFilterEntity(addPropertyObject(quantitySupplierBoxBoxShipmentSku, objSupplierBox, objShipment, objSku));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      new OrFilterEntity(inSupplierBox, inSupplierBoxShipmentSku),
                                      "В коробе поставщика или оприходовано",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      inSupplierBox,
                                      "В коробе поставщика"));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                                      inSupplierBoxShipmentStock,
                                      "Оприходовано в тек. короб",
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
                                      inInvoiceShipmentStock,
                                      "Оприходовано в тек. короб",
                                      KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            }
            filterGroup.defaultFilter = 0;
            addRegularFilterGroup(filterGroup);

            RegularFilterGroupEntity filterGroup2 = new RegularFilterGroupEntity(genID());
            filterGroup2.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(shipmentShipmentDetail, objShipmentDetail), Compare.EQUALS, objShipment),
                    "В текущей поставке",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroup2.defaultFilter = 0;
            addRegularFilterGroup(filterGroup2);

            RegularFilterGroupEntity filterGroup4 = new RegularFilterGroupEntity(genID());
            filterGroup4.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(userShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(baseLM.currentUser)),
                    "Текущего пользователя",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroup4);

            RegularFilterGroupEntity filterGroup5 = new RegularFilterGroupEntity(genID());
            filterGroup5.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(stockShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(currentFreightBoxRoute, objRoute)),
                    "В текущем коробе для транспортировки",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroup5);

            RegularFilterGroupEntity filterGroupDiffShipment = new RegularFilterGroupEntity(genID());
            filterGroupDiffShipment.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(diffShipmentSku, objShipment, objSku)),
                                  "Только по отличающимся (в поставке)",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroupDiffShipment);

            if (box) {
                RegularFilterGroupEntity filterGroup3 = new RegularFilterGroupEntity(genID());
                filterGroup3.addFilter(new RegularFilterEntity(genID(),
                        new CompareFilterEntity(addPropertyObject(supplierBoxShipmentDetail, objShipmentDetail), Compare.EQUALS, objSupplierBox),
                        "В текущем коробе поставщика",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
                addRegularFilterGroup(filterGroup3);
            }

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

            addActionsOnObjectChange(objBarcode, addPropertyObject(
                                                    addJProp(true, addSAProp(null), skuBarcodeObject, 1),
                                                            objBarcode));

            addActionsOnObjectChange(objBarcode,
                                     addPropertyObject(
                                             addJProp(true, baseLM.andNot1,
                                                     addMFAProp(
                                                             null,
                                                             "Ввод нового товара",
                                                             createItemForm,
                                                             new ObjectEntity[]{createItemForm.objSupplier, createItemForm.objBarcode},
                                                             true,
                                                             createItemForm.addPropertyObject(addItemBarcode, createItemForm.objBarcode)
                                                     ), 1, 2,
                                                     skuBarcodeObject, 2
                                             ),
                                             objSupplier, objBarcode));

            addActionsOnObjectChange(objBarcode, addPropertyObject(
                    addJProp(true, addAProp(new SeekRouteActionProperty()),
                            1, skuBarcodeObject, 2, 3),
                    objShipment, objBarcode, objRoute));

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionCheckFreightBox, objRoute, objBarcode));

            if (box) {
                addActionsOnObjectChange(objBarcode, addPropertyObject(addBoxShipmentDetailBoxShipmentSupplierBoxRouteBarcode, objShipment, objSupplierBox, objRoute, objBarcode));
            } else {
                addActionsOnObjectChange(objBarcode, addPropertyObject(addSimpleShipmentDetailSimpleShipmentRouteBarcode, objShipment, objRoute, objBarcode));
            }

//            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeNotFoundMessage, objBarcode));
            if (box)
                addActionsOnObjectChange(objSIDSupplierBox, addPropertyObject(seekSupplierBoxSIDSupplier, objSIDSupplierBox, objSupplier));

            setReadOnly(baseLM.name, true, objRoute.groupTo);
            setReadOnly(percentShipmentRouteSku, true, objRoute.groupTo);
            setReadOnly(itemAttributeGroup, true, objSku.groupTo);
            setReadOnly(sidArticleSku, true, objSku.groupTo);

            setReadOnly(baseGroup, true, objShipmentDetail.groupTo);
            setReadOnly(supplierAttributeGroup, true, objShipmentDetail.groupTo);
            setReadOnly(sidSupplierBoxShipmentDetail, false, objShipmentDetail.groupTo);
            setReadOnly(barcodeSupplierBoxShipmentDetail, false, objShipmentDetail.groupTo);
            setReadOnly(barcodeStockShipmentDetail, false, objShipmentDetail.groupTo);

            setReadOnly(objSupplier, true);
            setReadOnly(objShipment, true);
            if (box)
                setReadOnly(objSupplierBox, true);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.blockedScreen.put("changePropertyDraw", getPropertyDraw(baseLM.objectValue, objBarcode).getID() + "");

            design.get(getPropertyDraw(sidDocument, objShipment)).caption = "Номер поставки";
            design.get(getPropertyDraw(baseLM.date, objShipment)).caption = "Дата поставки";

            if (box)
                design.setEditKey(design.get(getPropertyDraw(baseLM.objectValue, objSIDSupplierBox)), KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));

            design.get(objRoute.groupTo).grid.hideToolbarItems();
            design.get(objRoute.groupTo).setTableRowsCount(0);

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

            design.get(nameRoute).setMinimumCharWidth(4);
            design.get(nameRoute).panelLabelAbove = true;
            design.get(nameRoute).design.font = new Font("Tahoma", Font.BOLD, 48);
            design.getGroupObjectContainer(objRoute.groupTo).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;

            design.setHighlightColor(new Color(255, 128, 128));

            return design;
        }
    }

    private class FreightShipmentStoreFormEntity extends BarcodeFormEntity {

        private ObjectEntity objFreight;
        private ObjectEntity objPallet;

        private FreightShipmentStoreFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.objectValue, baseLM.date, baseLM.objectClassName, nameRouteFreight, nameFreightTypeFreight, tonnageFreight, grossWeightFreight, volumeFreight, palletCountFreight, palletNumberFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objFreight, true);

            PropertyObjectEntity diffPalletFreightProperty = addPropertyObject(diffPalletFreight, objFreight);
            getPropertyDraw(palletCountFreight).setPropertyHighlight(diffPalletFreightProperty);
            getPropertyDraw(palletNumberFreight).setPropertyHighlight(diffPalletFreightProperty);

            objPallet = addSingleGroupObject(pallet, "Паллета", baseLM.barcode, grossWeightPallet, freightBoxNumberPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objPallet, true);

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetFreight, objBarcode, objFreight));

            addPropertyDraw(equalsPalletFreight, objPallet, objFreight);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightPallet, objPallet), Compare.EQUALS, objFreight));
            /*addFixedFilter(new CompareFilterEntity(addPropertyObject(routeCreationPalletPallet, objPallet), Compare.EQUALS, addPropertyObject(routeFreight, objFreight)));

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
            addRegularFilterGroup(filterGroup);*/

        }

        @Override
         public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.blockedScreen.put("changePropertyDraw", getPropertyDraw(baseLM.objectValue, objBarcode).getID() + "");

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.get(objFreight.groupTo).grid.constraints.fillVertical = 1;
            design.get(objPallet.groupTo).grid.constraints.fillVertical = 2;

            design.setHighlightColor(new Color(128, 255, 128));

            return design;
        }
    }

    private class BoxPalletStoreFormEntity extends BarcodeFormEntity {

        private ObjectEntity objBox;
        private ObjectEntity objPallet;

        private BoxPalletStoreFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objPallet = addSingleGroupObject(pallet, "Паллета", baseLM.barcode, nameRouteCreationPalletPallet, grossWeightPallet, freightBoxNumberPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objPallet, true);

            objBox = addSingleGroupObject(freightBox, "Короб для транспортировки", baseLM.barcode, nameRouteCreationFreightBoxFreightBox, netWeightStock);
            objBox.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objBox, true);

            addActionsOnObjectChange(objBarcode, addPropertyObject(baseLM.seekBarcodeAction, objBarcode));                        
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetPalletFreightBox, objBarcode, objPallet));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(palletFreightBox, objBox), Compare.EQUALS, objPallet));

        }

        @Override
         public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();                        
                        
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
        private ObjectEntity objSku;

        private FreightShipmentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.date, baseLM.objectClassName, nameRouteFreight, nameFreightTypeFreight, tonnageFreight, grossWeightFreight, volumeFreight, palletCountFreight, palletNumberFreight);
            addObjectActions(this, objFreight);

            PropertyObjectEntity diffPalletFreightProperty = addPropertyObject(diffPalletFreight, objFreight);
            getPropertyDraw(palletCountFreight).setPropertyHighlight(diffPalletFreightProperty);
            getPropertyDraw(palletNumberFreight).setPropertyHighlight(diffPalletFreightProperty);

            GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
            objDateFrom = new ObjectEntity(genID(), DateClass.instance, "Дата (с)");
            objDateTo = new ObjectEntity(genID(), DateClass.instance, "Дата (по)");
            gobjDates.add(objDateFrom);
            gobjDates.add(objDateTo);

            addGroup(gobjDates);
            gobjDates.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(objDateFrom, baseLM.objectValue);
            addPropertyDraw(objDateTo, baseLM.objectValue);
            addPropertyDraw(quantityPalletShipmentBetweenDate, objDateFrom, objDateTo);
            addPropertyDraw(quantityPalletFreightBetweenDate, objDateFrom, objDateTo);

            objShipment = addSingleGroupObject(shipment, "Поставка", baseLM.date, nameSupplierDocument, sidDocument, sumDocument, nameCurrencyDocument, netWeightShipment, grossWeightShipment, quantityPalletShipment, quantityBoxShipment);
            setReadOnly(objShipment, true);

            objPallet = addSingleGroupObject(pallet, "Паллета", baseLM.barcode, grossWeightPallet, freightBoxNumberPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objPallet, true);

            addPropertyDraw(equalsPalletFreight, objPallet, objFreight);

            objSku = addSingleGroupObject(sku, "SKU", sidArticleSku, nameArticleSku, nameBrandSupplierArticleSku, nameColorSupplierItem, sidSizeSupplierItem);
            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            addPropertyDraw(quantityPalletSku, objPallet, objSku);

            setReadOnly(objSku, true);

            objDirectInvoice = addSingleGroupObject(directInvoice, "Инвойс напрямую", baseLM.date, sidDocument, sumDocument, nameImporterInvoice, sidContractInvoice, nameDestinationDestinationDocument, grossWeightDirectInvoice, palletNumberDirectInvoice);
            setReadOnly(objDirectInvoice, true);

            addPropertyDraw(equalsDirectInvoiceFreight, objDirectInvoice, objFreight);

            addPropertyDraw(quantityShipmentFreight, objShipment, objFreight);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(baseLM.date, objShipment), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(baseLM.date, objShipment), Compare.LESS_EQUALS, objDateTo));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(routeCreationPalletPallet, objPallet), Compare.EQUALS, addPropertyObject(routeFreight, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityPalletSku, objPallet, objSku)));

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

            design.get(getPropertyDraw(sidDocument, objDirectInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(baseLM.date, objDirectInvoice)).caption = "Дата инвойса";
            design.get(getPropertyDraw(sidDocument, objShipment)).caption = "Номер поставки";
            design.get(getPropertyDraw(baseLM.date, objShipment)).caption = "Дата поставки";
            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.get(objDirectInvoice.groupTo).grid.constraints.fillHorizontal = 4;
            design.get(objPallet.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objSku.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objPallet.groupTo).grid.constraints.fillVertical = 1;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 2;

            design.addIntersection(design.getGroupObjectContainer(objPallet.groupTo),
                    design.getGroupObjectContainer(objDirectInvoice.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSku.groupTo),
                    design.getGroupObjectContainer(objDirectInvoice.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView shipContainer = design.createContainer("Поставки");
            shipContainer.add(design.getGroupObjectContainer(objDateFrom.groupTo));
            shipContainer.add(design.getGroupObjectContainer(objShipment.groupTo));

            ContainerView contContainer = design.createContainer("Комплектация");
            contContainer.add(design.getGroupObjectContainer(objPallet.groupTo));
            contContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            contContainer.add(design.getGroupObjectContainer(objDirectInvoice.groupTo));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objFreight.groupTo));
            specContainer.add(shipContainer);
            specContainer.add(contContainer);
            specContainer.tabbedPane = true;

            design.setHighlightColor(new Color(128, 255, 128));

            return design;
        }
    }

    private class CreateFreightBoxFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objFreightBox;

        private CreateFreightBoxFormEntity(NavigatorElement parent, String sID, String caption, FormType type) {
            super(parent, sID, caption, type.equals(FormType.PRINT));

            objCreate = addSingleGroupObject(creationFreightBox, "Документ генерации коробов");

            if (!type.equals(FormType.ADD))
                addPropertyDraw(objCreate, baseLM.objectValue);

            addPropertyDraw(objCreate, nameRouteCreationFreightBox, quantityCreationFreightBox);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createFreightBox, objCreate);

            if (!type.equals(FormType.PRINT))
                addPropertyDraw(objCreate, printCreateFreightBoxForm);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.ADD))
                objCreate.addOnTransaction = true;

            objFreightBox = addSingleGroupObject(freightBox, "Короба для транспортировки", baseLM.barcode);
            setReadOnly(objFreightBox, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationFreightBoxFreightBox, objFreightBox), Compare.EQUALS, objCreate));

            if (type.equals(FormType.PRINT))
                printCreateFreightBoxForm = addFAProp("Печать штрих-кодов", this, objCreate);
        }
    }

    private class CreatePalletFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objPallet;

        private CreatePalletFormEntity(NavigatorElement parent, String sID, String caption, FormType type) {
            super(parent, sID, caption, type.equals(FormType.PRINT));

            objCreate = addSingleGroupObject(creationPallet, "Документ генерации паллет");
            if (!type.equals(FormType.ADD))
                addPropertyDraw(objCreate, baseLM.objectValue);

            addPropertyDraw(objCreate, nameRouteCreationPallet, quantityCreationPallet);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createPallet, objCreate);

            if (!type.equals(FormType.PRINT))
                addPropertyDraw(objCreate, printCreatePalletForm);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.ADD))
                objCreate.addOnTransaction = true;

            objPallet = addSingleGroupObject(pallet, "Паллеты для транспортировки", baseLM.barcode);
            setReadOnly(objPallet, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationPalletPallet, objPallet), Compare.EQUALS, objCreate));

            if (type.equals(FormType.PRINT))
                printCreatePalletForm = addFAProp("Печать штрих-кодов", this, objCreate);
        }
    }

       private class CreateStampFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objStamp;

        private CreateStampFormEntity(NavigatorElement parent, String sID, String caption, FormType type) {
            super(parent, sID, caption, type.equals(FormType.PRINT));

            objCreate = addSingleGroupObject(creationStamp, "Документ генерации марок");
            if (!type.equals(FormType.ADD))
                addPropertyDraw(objCreate, baseLM.objectValue);

            addPropertyDraw(objCreate, firstNumberCreationStamp,lastNumberCreationStamp, dateOfStampCreationStamp, seriesOfStampCreationStamp);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createStamp, objCreate);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.ADD))
                objCreate.addOnTransaction = true;

            objStamp = addSingleGroupObject(stamp, "Контрольные марки", sidStamp, dateOfStamp);
            setReadOnly(objStamp, true);

             addFixedFilter(new CompareFilterEntity(addPropertyObject(creationStampStamp, objStamp), Compare.EQUALS, objCreate));

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
            if (tree) {
                addPropertyDraw(baseLM.dumb1, objCustomCategory4Origin);
                addPropertyDraw(baseLM.dumb2, objCustomCategory10, objCustomCategory4Origin);
            }
            addObjectActions(this, objCustomCategory4Origin);

            objCustomCategory6Origin = addSingleGroupObject(customCategory6, "Второй уровень", sidCustomCategory6, nameCustomCategory);
            if (tree) {
                addPropertyDraw(baseLM.dumb1, objCustomCategory6Origin);
                addPropertyDraw(baseLM.dumb2, objCustomCategory10, objCustomCategory6Origin);
            }
            addObjectActions(this, objCustomCategory6Origin);

            import1 = addPropertyDraw(importBelTnved);
            import2 = addPropertyDraw(importEuTnved);

            objCustomCategoryOrigin = addSingleGroupObject(customCategoryOrigin, "ЕС уровень", sidCustomCategoryOrigin, nameCustomCategory, sidCustomCategory10CustomCategoryOrigin);
            addObjectActions(this, objCustomCategoryOrigin, objCustomCategory6Origin, customCategory6);

            addPropertyDraw(relationCustomCategory10CustomCategoryOrigin, objCustomCategory10, objCustomCategoryOrigin);

            if (tree) {
                treeCustomCategory = addTreeGroupObject(objCustomCategory4.groupTo, objCustomCategory6.groupTo, objCustomCategory9.groupTo);
                treeCustomCategoryOrigin = addTreeGroupObject(objCustomCategory4Origin.groupTo, objCustomCategory6Origin.groupTo, objCustomCategoryOrigin.groupTo);

                treeCustomCategory.plainTreeMode = true;
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
                design.getTreeContainer(treeCustomCategory).setTitle("Белорусский классификатор");
                design.getTreeContainer(treeCustomCategoryOrigin).setTitle("Европейский классификатор");

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

    private class BalanceWarehouseFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSku;

        private BalanceWarehouseFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.selection, baseLM.barcode, nameSupplierArticleSku, nameBrandSupplierArticleSku, nameThemeSupplierArticleSku,
                     nameCategoryArticleSku, sidArticleSku, nameArticleSku, sidCustomCategory10Sku,
                     sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                     nameCountrySku, netWeightSku,
                     mainCompositionSku, additionalCompositionSku, quantityDirectInvoicedSku, quantityStockedSku, quantitySku, sumSku);
            addObjectActions(this, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            setReadOnly(objSku, true);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(quantitySku, objSku)),
                                  "Только ненулевые остатки",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(quantitySku, objSku))),
                                  "Только нулевые остатки",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.defaultFilter = 0;
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            return design;
        }
    }

    private class NomenclatureFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objCategory;
        private ObjectEntity objArticle;
        private ObjectEntity objSku;

        private NomenclatureFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);

            objCategory = addSingleGroupObject(category, "Номенклатурная группа", baseLM.name);

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle, nameSupplierArticle, nameBrandSupplierArticle, nameThemeSupplierArticle, nameCategoryArticle, nameArticle);
            addObjectActions(this, objArticle);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.selection, baseLM.barcode, nameSupplierArticleSku, nameBrandSupplierArticleSku, nameThemeSupplierArticleSku,
                     nameCategoryArticleSku, sidArticleSku, nameArticleSku,
                     sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                     nameCountrySku, netWeightSku,
                     mainCompositionSku, additionalCompositionSku);
            addObjectActions(this, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            RegularFilterGroupEntity filterGroupSupplierSku = new RegularFilterGroupEntity(genID());
            filterGroupSupplierSku.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(supplierArticleSku, objSku), Compare.EQUALS, objSupplier),
                                  "Только текущего поставщика",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroupSupplierSku.defaultFilter = 0;
            addRegularFilterGroup(filterGroupSupplierSku);

            RegularFilterGroupEntity filterGroupCategorySku = new RegularFilterGroupEntity(genID());
            filterGroupCategorySku.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(categoryArticleSku, objSku), Compare.EQUALS, objCategory),
                                  "Только текущей номенклатурной группы",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroupCategorySku.defaultFilter = 0;
            addRegularFilterGroup(filterGroupCategorySku);

            RegularFilterGroupEntity filterGroupSupplierArticle = new RegularFilterGroupEntity(genID());
            filterGroupSupplierArticle.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, objSupplier),
                                  "Только текущего поставщика",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroupSupplierArticle.defaultFilter = 0;
            addRegularFilterGroup(filterGroupSupplierArticle);

            RegularFilterGroupEntity filterGroupCategoryArticle = new RegularFilterGroupEntity(genID());
            filterGroupCategoryArticle.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(categoryArticle, objArticle), Compare.EQUALS, objCategory),
                                  "Только текущей номенклатурной группы",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroupCategoryArticle.defaultFilter = 0;
            addRegularFilterGroup(filterGroupCategoryArticle);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 1;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 4;

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            specContainer.tabbedPane = true;

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                                   design.getGroupObjectContainer(objCategory.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);


            return design;
        }
    }

    private class ContractFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSeller;
        private ObjectEntity objContract;

        private ContractFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSeller = addSingleGroupObject(seller, "Продавец", baseLM.name, baseLM.objectClassName);

            objContract = addSingleGroupObject(contract, "Договор", sidContract, baseLM.date, nameImporterContract, nameCurrencyContract);
            addObjectActions(this, objContract);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(sellerContract, objContract), Compare.EQUALS, objSeller));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.objectClassName, objSeller)).caption = "Тип продавца";
            design.get(objSeller.groupTo).grid.constraints.fillVertical = 1;
            design.get(objContract.groupTo).grid.constraints.fillVertical = 3;

            return design;
        }
    }

    private class BalanceBrandWarehouseFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objBrand;
        private ObjectEntity objPallet;
        private ObjectEntity objInvoice;
        private ObjectEntity objBox;
        private ObjectEntity objArticle;
        private ObjectEntity objArticle2;
        private ObjectEntity objSku;
        private ObjectEntity objSku2;

        private TreeGroupEntity treeSupplierBrand;
        private TreeGroupEntity treePalletBoxArticleSku;
        private TreeGroupEntity treeInvoiceArticleSku;

        private BalanceBrandWarehouseFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд", baseLM.name);

            treeSupplierBrand = addTreeGroupObject(objSupplier.groupTo, objBrand.groupTo);

            objPallet = addSingleGroupObject(pallet, "Паллета", baseLM.barcode);
            addPropertyDraw(quantityPalletBrandSupplier, objPallet, objBrand);

            objInvoice = addSingleGroupObject(directInvoice, "Инвойс (напрямую)", sidDocument);
            addPropertyDraw(quantityDocumentBrandSupplier, objInvoice, objBrand);
            addPropertyDraw(objInvoice, baseLM.date);

            objBox = addSingleGroupObject(freightBox, "Короб", baseLM.barcode);
            addPropertyDraw(quantityStockBrandSupplier, objBox, objBrand);

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle);
            addPropertyDraw(quantityStockArticle, objBox, objArticle);
            addPropertyDraw(objArticle, nameArticleSku, nameThemeSupplierArticle, nameCategoryArticleSku);

            objArticle2 = addSingleGroupObject(article, "Артикул", sidArticle);
            addPropertyDraw(quantityDocumentArticle, objInvoice, objArticle2);
            addPropertyDraw(objArticle2, baseLM.dumb1, nameArticleSku, nameThemeSupplierArticle, nameCategoryArticleSku);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.barcode);
            addPropertyDraw(quantityStockSku, objBox, objSku);
            addPropertyDraw(objSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                     nameCountrySku, netWeightSku, mainCompositionSku, additionalCompositionSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            objSku2 = addSingleGroupObject(sku, "SKU", baseLM.barcode);
            addPropertyDraw(quantityDocumentSku, objInvoice, objSku2);
            addPropertyDraw(objSku2, baseLM.dumb1, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                     nameCountrySku, netWeightSku, mainCompositionSku, additionalCompositionSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku2.groupTo);

            setReadOnly(objSupplier, true);
            setReadOnly(objBrand, true);
            setReadOnly(objInvoice, true);
            setReadOnly(objPallet, true);
            setReadOnly(objBox, true);
            setReadOnly(objArticle, true);
            setReadOnly(objArticle2, true);
            setReadOnly(objSku, true);
            setReadOnly(objSku2, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierBrandSupplier, objBrand), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(palletFreightBox, objBox), Compare.EQUALS, objPallet));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(brandSupplierArticle, objArticle), Compare.EQUALS, objBrand));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(brandSupplierArticle, objArticle2), Compare.EQUALS, objBrand));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleSku, objSku), Compare.EQUALS, objArticle));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleSku, objSku2), Compare.EQUALS, objArticle2));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDocumentBrandSupplier, objInvoice, objBrand)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityPalletBrandSupplier, objPallet, objBrand)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStockBrandSupplier, objBox, objBrand)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStockArticle, objBox, objArticle)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDocumentArticle, objInvoice, objArticle2)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStockSku, objBox, objSku)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDocumentSku, objInvoice, objSku2)));

            treePalletBoxArticleSku = addTreeGroupObject(objPallet.groupTo, objBox.groupTo, objArticle.groupTo, objSku.groupTo);
            treeInvoiceArticleSku = addTreeGroupObject(objInvoice.groupTo, objArticle2.groupTo, objSku2.groupTo);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.getTreeContainer(treeSupplierBrand).setTitle("Поставщики и их бренды");
            design.getTreeContainer(treePalletBoxArticleSku).setTitle("В паллетах и коробах");
            design.getTreeContainer(treeInvoiceArticleSku).setTitle("В инвойсах напрямую");

            design.get(treeSupplierBrand).constraints.fillVertical = 2;
            design.get(treePalletBoxArticleSku).constraints.fillVertical = 5;
            design.get(treeInvoiceArticleSku).constraints.fillVertical = 5;

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.get(treePalletBoxArticleSku));
            specContainer.add(design.getTreeContainer(treePalletBoxArticleSku));
            specContainer.add(design.getTreeContainer(treeInvoiceArticleSku));
            specContainer.tabbedPane = true;

            return design;
        }
    }

    private class InvoiceShipmentFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objInvoice;
        private ObjectEntity objBox;
        //private ObjectEntity objArticle;
        private ObjectEntity objSku;
        private ObjectEntity objSku2;

        private InvoiceShipmentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);

            objInvoice = addSingleGroupObject(invoice, "Инвойс", baseLM.date, sidDocument, baseLM.objectClassName);

            objBox = addSingleGroupObject(supplierBox, "Короб из инвойса", sidSupplierBox);

            objSku = addSingleGroupObject(sku, "Товар в инвойсе", baseLM.barcode, sidArticleSku, nameBrandSupplierArticleSku, nameCategoryArticleSku,
                     sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);

            addPropertyDraw(quantityDocumentSku, objInvoice, objSku);
            addPropertyDraw(quantityInvoiceSku, objInvoice, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            objSku2 = addSingleGroupObject(sku, "Товар в коробе", baseLM.barcode, sidArticleSku, nameBrandSupplierArticleSku, nameCategoryArticleSku,
                     sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);

            addPropertyDraw(quantityListSku, objBox, objSku2);
            addPropertyDraw(quantitySupplierBoxSku, objBox, objSku2);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku2.groupTo);

            setReadOnly(objSupplier, true);
            setReadOnly(objInvoice, true);
            setReadOnly(objBox, true);
            setReadOnly(objSku, true);
            setReadOnly(objSku2, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(boxInvoiceSupplierBox, objBox), Compare.EQUALS, objInvoice));

            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(quantityDocumentSku, objInvoice, objSku)),
                                              new NotNullFilterEntity(addPropertyObject(quantityInvoiceSku, objInvoice, objSku))));

            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(quantityListSku, objBox, objSku2)),
                                              new NotNullFilterEntity(addPropertyObject(quantitySupplierBoxSku, objBox, objSku2))));

            RegularFilterGroupEntity filterGroupDiffInvoice = new RegularFilterGroupEntity(genID());
            filterGroupDiffInvoice.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(diffDocumentInvoiceSku, objInvoice, objSku))),
                                  "Только по отличающимся",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupDiffInvoice);

            RegularFilterGroupEntity filterGroupDiffBox = new RegularFilterGroupEntity(genID());
            filterGroupDiffBox.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(diffListSupplierBoxSku, objBox, objSku2))),
                                  "Только по отличающимся",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroupDiffBox);

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.objectClassName, objInvoice)).caption = "Тип инвойса";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";

            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 1;
            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 1;
            design.get(objSupplier.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objInvoice.groupTo).grid.constraints.fillHorizontal = 5;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 4;
            design.get(objBox.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSku2.groupTo).grid.constraints.fillVertical = 4;
            design.get(objBox.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objSku2.groupTo).grid.constraints.fillHorizontal = 5;

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                                   design.getGroupObjectContainer(objInvoice.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView boxContainer = design.createContainer("По коробам");
            boxContainer.add(design.getGroupObjectContainer(objBox.groupTo));
            boxContainer.add(design.getGroupObjectContainer(objSku2.groupTo));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objSku.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSku.groupTo));            
            specContainer.add(boxContainer);
            specContainer.tabbedPane = true;                        

            return design;
        }
    }

    private class FreightChangeFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objArticle;
        private ObjectEntity objCategory;
        private ObjectEntity objSku;
        private ObjectEntity objSkuFreight;

        private FreightChangeFormEntity(NavigatorElement parent, String sID, String caption) {
            this(parent, sID, caption, false);
        }

        private FreightChangeFormEntity(NavigatorElement parent, String sID, String caption, boolean edit) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.date, baseLM.objectClassName, nameRouteFreight, nameExporterFreight, nameFreightTypeFreight, grossWeightFreight, palletNumberFreight);

            if (edit) {
                objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
                addActionsOnClose(addPropertyObject(executeChangeFreightClass, objFreight, (DataObject)freightChanged.getClassObject()));
            } else {
                FreightChangeFormEntity editFreightForm = new FreightChangeFormEntity(null, "freightChangeForm_edit", "Обработка фрахта", true);
                addPropertyDraw(
                        addJProp("Обработать фрахт", and(false, true),
                                addMFAProp(null,
                                        "Обработать фрахт",
                                        editFreightForm,
                                        new ObjectEntity[]{editFreightForm.objFreight},
                                        new PropertyObjectEntity[0],
                                        new PropertyObjectEntity[0],
                                        false), 1,
                                is(freightPriced), 1,
                                is(freightChanged), 1),
                        objFreight
                ).forceViewType = ClassViewType.GRID;

                addPropertyDraw(
                        addJProp("Отгрузить фрахт", and(false, true),
                                executeChangeFreightClass, 1, 2,
                                is(freightChanged), 1,
                                is(freightShipped), 1),
                        objFreight,
                        (DataObject)freightShipped.getClassObject()
                ).forceViewType = ClassViewType.GRID;
            }

            objImporter = addSingleGroupObject(importer, "Импортёр", baseLM.name, addressSubject);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(invoiceOriginFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(invoiceFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(proformOriginFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(proformFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(annexInvoiceOriginFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(annexInvoiceFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(sbivkaFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(packingListFormImporterFreight, objImporter, objFreight);

            objArticle = addSingleGroupObject(article, "Артикул", baseLM.selection, sidArticle, nameBrandSupplierArticle, originalNameArticle, nameCategoryArticle, nameArticle,
                    sidCustomCategoryOriginArticle, nameCountryOfOriginArticle, mainCompositionOriginArticle, additionalCompositionOriginArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(quantityFreightArticle, objFreight, objArticle);

            objCategory = addSingleGroupObject(category, "Номенклатурная группа", baseLM.name);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.selection, baseLM.barcode, sidArticleSku,
                     nameBrandSupplierArticleSku, nameArticleSku, nameCategoryArticleSku,
                     sidCustomCategoryOriginArticleSku, sidCustomCategory10Sku, nameCountrySku, netWeightSku,
                     mainCompositionOriginSku, translationMainCompositionSku, mainCompositionSku,
                     additionalCompositionOriginSku, translationAdditionalCompositionSku, additionalCompositionSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            addPropertyDraw(addGCAProp(actionGroup, "translationAllMainComposition", "Перевод составов", objSku.groupTo, translationMainCompositionSku, baseLM.actionTrue), objSku).forceViewType = ClassViewType.PANEL;
            addPropertyDraw(addGCAProp(actionGroup, "translationAllAdditionalComposition", "Перевод доп. составов", objSku.groupTo, translationAdditionalCompositionSku, baseLM.actionTrue), objSku).forceViewType = ClassViewType.PANEL;

            setReadOnly(baseGroup, true, objSku.groupTo);
            setReadOnly(publicGroup, true, objSku.groupTo);            
            setReadOnly(sidCustomCategory10Sku, false, objSku.groupTo);

            objSkuFreight = addSingleGroupObject(sku, "Позиции фрахта", baseLM.selection, baseLM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem,
                    sidSizeSupplierItem, nameBrandSupplierArticleSku, nameArticleSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSkuFreight.groupTo);

            addPropertyDraw(quantityFreightSku, objFreight, objSku);
            addPropertyDraw(quantityDirectFreightSku, objFreight, objSku);
            addPropertyDraw(sidCustomCategory10FreightSku, objFreight, objSkuFreight);
            addPropertyDraw(nameCountryOfOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(netWeightFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(grossWeightFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(mainCompositionOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(mainCompositionFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(additionalCompositionOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(additionalCompositionFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(quantityImporterFreightSku, objImporter, objFreight, objSkuFreight);
            addPropertyDraw(quantityProxyImporterFreightSku, objImporter, objFreight, objSkuFreight);
            addPropertyDraw(quantityDirectImporterFreightSku, objImporter, objFreight, objSkuFreight);
            addPropertyDraw(priceInOutImporterFreightSku, objImporter, objFreight, objSkuFreight);
            addPropertyDraw(sumInOutImporterFreightSku, objImporter, objFreight, objSkuFreight);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightCategory, objFreight, objCategory)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightSku, objFreight, objSku)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSkuFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightArticle, objFreight, objArticle)));

            RegularFilterGroupEntity filterGroupCategory = new RegularFilterGroupEntity(genID());
            filterGroupCategory.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(categoryArticleSku, objSku), Compare.EQUALS, objCategory),
                                  "Только по номенклатурной группе",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            filterGroupCategory.defaultFilter = 0;
            addRegularFilterGroup(filterGroupCategory);

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

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.get(objFreight.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objFreight.groupTo).grid.constraints.fillVertical = 1;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 4;
            design.get(objCategory.groupTo).grid.constraints.fillHorizontal = 0.2;
            design.get(objSku.groupTo).grid.constraints.fillHorizontal = 6;
            design.get(objCategory.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSkuFreight.groupTo).grid.constraints.fillVertical = 4;

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo),
                                   design.getGroupObjectContainer(objImporter.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

             design.addIntersection(design.getGroupObjectContainer(objCategory.groupTo),
                                   design.getGroupObjectContainer(objSku.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView skuContainer = design.createContainer("SKU");
            skuContainer.add(design.getGroupObjectContainer(objCategory.groupTo));
            skuContainer.add(design.getGroupObjectContainer(objSku.groupTo));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(skuContainer);
            specContainer.add(design.getGroupObjectContainer(objSkuFreight.groupTo));
            specContainer.tabbedPane = true;

            return design;
        }
    }

    public class AnnexInvoiceFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean translate;

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objArticle;
        public ObjectEntity objCategory;
        public ObjectEntity objComposition;
        public ObjectEntity objCountry;

        private GroupObjectEntity gobjFreightImporter;
        private GroupObjectEntity gobjArticleCompositionCountryCategory;

        private AnnexInvoiceFormEntity(NavigatorElement parent, String sID, String caption, boolean translate) {
            super(parent, sID, caption, true);

            this.translate = translate;

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightChanged, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, baseLM.date, baseLM.objectClassName, nameCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight);
                addPropertyDraw(objFreight, addressExporterFreight);
                addPropertyDraw(objImporter, baseLM.name);
                addPropertyDraw(objImporter, addressSubject);
            }

            if (!translate) {
                addPropertyDraw(objFreight, nameOriginExporterFreight);
                addPropertyDraw(objFreight, addressOriginExporterFreight);
                addPropertyDraw(objImporter, nameOrigin);
                addPropertyDraw(objImporter, addressOriginSubject);
            }

            addPropertyDraw(objImporter, contractImporter);
            addPropertyDraw(objImporter, objFreight, netWeightImporterFreight, grossWeightImporterFreight, sumImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            gobjArticleCompositionCountryCategory = new GroupObjectEntity(4, "gobjArticleCompositionCountryCategory");
            objArticle = new ObjectEntity(5, "article", article, "Артикул");
            objComposition = new ObjectEntity(6, "composition", COMPOSITION_CLASS, "Состав");
            objCountry = new ObjectEntity(7, "country", baseLM.country, "Страна");
            objCategory = new ObjectEntity(8, "category", customCategory10, "ТН ВЭД");

            gobjArticleCompositionCountryCategory.add(objArticle);
            gobjArticleCompositionCountryCategory.add(objComposition);
            gobjArticleCompositionCountryCategory.add(objCountry);
            gobjArticleCompositionCountryCategory.add(objCategory);
            addGroup(gobjArticleCompositionCountryCategory);

            addPropertyDraw(objArticle, sidArticle);
            addPropertyDraw(objArticle, nameBrandSupplierArticle);

            if (translate) {
                addPropertyDraw(objArticle, nameCategoryArticle);
                addPropertyDraw(compositionFreightArticleCompositionCountryCategory, objFreight, objArticle, objComposition, objCountry, objCategory);
            }

            if (!translate) {
                addPropertyDraw(objArticle, nameArticle);
                addPropertyDraw(objComposition, baseLM.objectValue);
            }

            addPropertyDraw(objCountry, baseLM.sidCountry);
            if (!translate)
                addPropertyDraw(objCountry, nameOriginCountry);

            if (translate)
                addPropertyDraw(objCountry, baseLM.name);

            addPropertyDraw(objCategory, sidCustomCategory10);
            addPropertyDraw(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            if (!translate)
                addPropertyDraw(objArticle, nameOriginUnitOfMeasureArticle);

            if (translate)
                addPropertyDraw(objArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(netWeightImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(grossWeightImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(priceImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(sumImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(addDEAProp(), objImporter, objFreight);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

            if (!translate)
                annexInvoiceOriginFormImporterFreight = addFAProp("Приложение", this, objImporter, objFreight);

            if (translate)
                annexInvoiceFormImporterFreight = addFAProp("Приложение (перевод)", this, objImporter, objFreight);
        }
    }

    public class InvoiceFromFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean translate;

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objArticle;
        public ObjectEntity objCategory;
        public ObjectEntity objComposition;
        public ObjectEntity objCountry;

        private GroupObjectEntity gobjFreightImporter;
        private GroupObjectEntity gobjArticleCompositionCountryCategory;

        private InvoiceFromFormEntity(NavigatorElement parent, String sID, String caption, boolean translate) {
            super(parent, sID, caption, true);

            this.translate = translate;

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightChanged, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, baseLM.date, baseLM.objectClassName, nameCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight);
                addPropertyDraw(objFreight, addressExporterFreight);
                addPropertyDraw(objImporter, baseLM.name);
                addPropertyDraw(objImporter, addressSubject);
            }

            if (!translate) {
                addPropertyDraw(objFreight, nameOriginExporterFreight);
                addPropertyDraw(objFreight, addressOriginExporterFreight);
                addPropertyDraw(objImporter, nameOrigin);
                addPropertyDraw(objImporter, addressOriginSubject);
            }

            addPropertyDraw(objImporter, contractImporter);
            addPropertyDraw(objImporter, objFreight, netWeightImporterFreight, grossWeightImporterFreight, sumImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            gobjArticleCompositionCountryCategory = new GroupObjectEntity(4, "gobjArticleCompositionCountryCategory");
            objArticle = new ObjectEntity(5, "article", article, "Артикул");
            objComposition = new ObjectEntity(6, "composition", COMPOSITION_CLASS, "Состав");
            objCountry = new ObjectEntity(7, "country", baseLM.country, "Страна");
            objCategory = new ObjectEntity(8, "category", customCategory10, "ТН ВЭД");

            gobjArticleCompositionCountryCategory.add(objArticle);
            gobjArticleCompositionCountryCategory.add(objComposition);
            gobjArticleCompositionCountryCategory.add(objCountry);
            gobjArticleCompositionCountryCategory.add(objCategory);
            addGroup(gobjArticleCompositionCountryCategory);

            addPropertyDraw(objArticle, sidArticle);
            addPropertyDraw(objArticle, nameBrandSupplierArticle);

            if (translate) {
                addPropertyDraw(objArticle, nameCategoryArticle);
                addPropertyDraw(compositionFreightArticleCompositionCountryCategory, objFreight, objArticle, objComposition, objCountry, objCategory);
            }

            if (!translate) {
                addPropertyDraw(objArticle, nameArticle);
                addPropertyDraw(objComposition, baseLM.objectValue);
            }

            addPropertyDraw(objCountry, baseLM.sidCountry);
            if (!translate)
                addPropertyDraw(objCountry, nameOriginCountry);

            if (translate)
                addPropertyDraw(objCountry, baseLM.name);

            addPropertyDraw(objCategory, sidCustomCategory10);
            addPropertyDraw(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            if (!translate)
                addPropertyDraw(objArticle, nameOriginUnitOfMeasureArticle);

            if (translate)
                addPropertyDraw(objArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(priceImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(sumImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(addDEAProp(), objImporter, objFreight);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

            if (!translate)
                invoiceOriginFormImporterFreight = addFAProp("Инвойс", this, objImporter, objFreight);

            if (translate)
                invoiceFormImporterFreight = addFAProp("Инвойс (перевод)", this, objImporter, objFreight);
        }
    }

    public class ProformInvoiceFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean translate;

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objArticle;
        public ObjectEntity objCategory;
        public ObjectEntity objComposition;
        public ObjectEntity objCountry;

        private GroupObjectEntity gobjFreightImporter;
        private GroupObjectEntity gobjArticleCompositionCountryCategory;

        private ProformInvoiceFormEntity(NavigatorElement parent, String sID, String caption, boolean translate) {
            super(parent, sID, caption, true);

            this.translate = translate;

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightChanged, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, baseLM.date, baseLM.objectClassName, nameCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight);
                addPropertyDraw(objFreight, addressExporterFreight);
                addPropertyDraw(objImporter, baseLM.name);
                addPropertyDraw(objImporter, addressSubject);
            }

            if (!translate) {
                addPropertyDraw(objFreight, nameOriginExporterFreight);
                addPropertyDraw(objFreight, addressOriginExporterFreight);
                addPropertyDraw(objImporter, nameOrigin);
                addPropertyDraw(objImporter, addressOriginSubject);
            }

            addPropertyDraw(objImporter, contractImporter);
            addPropertyDraw(objImporter, objFreight, netWeightImporterFreight, grossWeightImporterFreight, sumImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            gobjArticleCompositionCountryCategory = new GroupObjectEntity(4, "gobjArticleCompositionCountryCategory");
            objArticle = new ObjectEntity(5, "article", article, "Артикул");
            objComposition = new ObjectEntity(6, "composition", COMPOSITION_CLASS, "Состав");
            objCountry = new ObjectEntity(7, "country", baseLM.country, "Страна");
            objCategory = new ObjectEntity(8, "category", customCategory10, "ТН ВЭД");

            gobjArticleCompositionCountryCategory.add(objArticle);
            gobjArticleCompositionCountryCategory.add(objComposition);
            gobjArticleCompositionCountryCategory.add(objCountry);
            gobjArticleCompositionCountryCategory.add(objCategory);
            addGroup(gobjArticleCompositionCountryCategory);

            addPropertyDraw(objArticle, sidArticle);
            addPropertyDraw(objArticle, nameBrandSupplierArticle);

            if (translate) {
                addPropertyDraw(objArticle, nameCategoryArticle);
                addPropertyDraw(compositionFreightArticleCompositionCountryCategory, objFreight, objArticle, objComposition, objCountry, objCategory);
            }

            if (!translate) {
                addPropertyDraw(objArticle, nameArticle);
                addPropertyDraw(objComposition, baseLM.objectValue);
            }

            addPropertyDraw(objCountry, baseLM.sidCountry);
            if (!translate)
                addPropertyDraw(objCountry, nameOriginCountry);

            if (translate)
                addPropertyDraw(objCountry, baseLM.name);

            addPropertyDraw(objCategory, sidCustomCategory10);
            addPropertyDraw(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            if (!translate)
                addPropertyDraw(objArticle, nameOriginUnitOfMeasureArticle);

            if (translate)
                addPropertyDraw(objArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(priceImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(sumImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(addDEAProp(), objImporter, objFreight);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

            if (!translate)
                proformOriginFormImporterFreight = addFAProp("Инвойс-проформа", this, objImporter, objFreight);

            if (translate)
                proformFormImporterFreight = addFAProp("Инвойс-проформа (перевод)", this, objImporter, objFreight);
        }
    }

    private class PackingListFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objFreightBox;
        private ObjectEntity objArticle;
        private ObjectEntity objSku;

        private GroupObjectEntity gobjFreightImporter;

        private PackingListFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightChanged, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, baseLM.date, baseLM.objectClassName, nameOriginExporterFreight, addressOriginExporterFreight, nameCurrencyFreight);
            addPropertyDraw(objImporter, nameOrigin, addressOriginSubject, contractImporter);
            addPropertyDraw(objImporter, objFreight, quantityImporterFreight, netWeightImporterFreight, grossWeightImporterFreight, sumInOutImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objFreightBox = addSingleGroupObject(4, "freightBox", freightBox, "Короб", baseLM.barcode);
            addPropertyDraw(objImporter, objFreightBox, netWeightImporterFreightUnit, grossWeightImporterFreightUnit, quantityImporterStock);

            objArticle = addSingleGroupObject(5, "article", article, "Артикул", sidArticle, nameBrandSupplierArticle, nameArticle);
            addPropertyDraw(quantityImporterStockArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(netWeightImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(grossWeightImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(sumImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterStockArticle, objImporter, objFreightBox, objArticle)));

            packingListFormImporterFreight = addFAProp("Упаковочный лист", this, objImporter, objFreight);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objFreightBox.groupTo).grid.constraints.fillVertical = 2;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 3;
            return design;
        }
    }

    public class SbivkaFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objCategory;

        private GroupObjectEntity gobjFreightImporter;

        private SbivkaFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightChanged, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, baseLM.date, nameCurrencyFreight);
            addPropertyDraw(objImporter, contractImporter);
            addPropertyDraw(objImporter, objFreight, netWeightImporterFreight, grossWeightImporterFreight, sumSbivkaImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objCategory = addSingleGroupObject(4, "category", customCategory6, "ТН ВЭД");

            addPropertyDraw(objCategory, sidCustomCategory6, nameCustomCategory);
            addPropertyDraw(objImporter, objFreight, objCategory, quantityImporterFreightCustomCategory6, netWeightImporterFreightCustomCategory6, grossWeightImporterFreightCustomCategory6, sumImporterFreightCustomCategory6);
            addPropertyDraw(quantitySbivkaImporterFreight, objImporter, objFreight);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantitySbivkaImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightCustomCategory6, objImporter, objFreight, objCategory)));

            sbivkaFormImporterFreight = addFAProp("Сбивка", this, objImporter, objFreight);
        }
    }


    private class ColorSizeSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objColor;
        private ObjectEntity objSize;
        private ObjectEntity objBrand;
        private ObjectEntity objCountry;
        private ObjectEntity objTheme;


        private ColorSizeSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name, nameBrandSupplierSupplier, nameCurrencySupplier);
            addObjectActions(this, objSupplier);

            objColor = addSingleGroupObject(colorSupplier, "Цвет", sidColorSupplier, baseLM.name);
            addObjectActions(this, objColor);

            objSize = addSingleGroupObject(sizeSupplier, "Размер", sidSizeSupplier, nameCommonSizeSizeSupplier);
            addObjectActions(this, objSize);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд", sidBrandSupplier, baseLM.name);
            addObjectActions(this, objBrand);

            objTheme = addSingleGroupObject(themeSupplier, "Тема", baseLM.name);
            addObjectActions(this, objTheme);

            objCountry = addSingleGroupObject(countrySupplier, "Страна", baseLM.name, nameCountryCountrySupplier);
            addObjectActions(this, objCountry);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColor), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSize), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierBrandSupplier, objBrand), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierThemeSupplier, objTheme), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierCountrySupplier, objCountry), Compare.EQUALS, objSupplier));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 0.5;
            design.get(objColor.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objSize.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objBrand.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objTheme.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objCountry.groupTo).grid.constraints.fillHorizontal = 2;

            design.addIntersection(design.getGroupObjectContainer(objColor.groupTo),
                                   design.getGroupObjectContainer(objSize.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSize.groupTo),
                                   design.getGroupObjectContainer(objBrand.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objBrand.groupTo),
                                   design.getGroupObjectContainer(objTheme.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objTheme.groupTo),
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

            objTransfer = addSingleGroupObject(transfer, "Внутреннее перемещение", baseLM.objectValue, barcodeStockFromTransfer, barcodeStockToTransfer);
            addObjectActions(this, objTransfer);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
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
            this(parent, sID, caption, false);
        }

        private FreightInvoiceFormEntity(NavigatorElement parent, String sID, String caption, boolean edit) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.date, baseLM.objectClassName, nameRouteFreight, nameFreightTypeFreight, nameCurrencyFreight, sumInFreight, sumMarkupInFreight, sumInOutFreight, palletNumberFreight);

            addPropertyDraw(sumInCurrentYear);
            addPropertyDraw(sumInOutCurrentYear);
            addPropertyDraw(balanceSumCurrentYear);

            if (edit) {
                objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
                addActionsOnClose(addPropertyObject(executeChangeFreightClass, objFreight, (DataObject)freightPriced.getClassObject()));
            } else {
                FreightInvoiceFormEntity editFreightForm = new FreightInvoiceFormEntity(null, "freightInvoiceForm_edit", "Расценка фрахта", true);
                addPropertyDraw(
                        addJProp("Расценить фрахт", and(false, true),
                                addMFAProp(null,
                                        "Расценить фрахт",
                                        editFreightForm,
                                        new ObjectEntity[]{editFreightForm.objFreight},
                                        new PropertyObjectEntity[0],
                                        new PropertyObjectEntity[0],
                                        false), 1,
                                is(freightComplete), 1,
                                is(freightPriced), 1),
                        objFreight
                ).forceViewType = ClassViewType.GRID;
            }

            objImporter = addSingleGroupObject(importer, "Импортер", baseLM.name);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumMarkupInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInOutImporterFreight, objImporter, objFreight);

            objBrandSupplier = addSingleGroupObject(brandSupplier, "Бренд", baseLM.name, nameSupplierBrandSupplier);

            addPropertyDraw(quantityImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);
            addPropertyDraw(markupPercentImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.barcode, sidArticleSku, sidSizeSupplierItem,
                    nameBrandSupplierArticleSku, nameCategoryArticleSku, sidCustomCategoryOriginArticleSku,
                    nameCountrySku, netWeightSku, mainCompositionOriginSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            setReadOnly(baseGroup, true, objSku.groupTo);
            setReadOnly(publicGroup, true, objSku.groupTo);
            
            addPropertyDraw(quantityImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(markupPercentImporterFreightSku, objImporter, objFreight, objSku);
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

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            ContainerView specContainer = design.createContainer("Итоги по текущему году");
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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseLM.objectValue);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Артикул", baseLM.objectValue);
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


    /*private class CreateItemFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objSupplier;
        ObjectEntity objArticle;
        ObjectEntity objSku;

        public CreateItemFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle);
            objArticle.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSku = addSingleGroupObject(item, "Товар", sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            objItem.groupTo.setSingleClassView(ClassViewType.PANEL);


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
    }*/

    public class PricatFormEntity extends FormEntity {
        ObjectEntity objSupplier;

        public PricatFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            objSupplier = addSingleGroupObject(supplier, baseLM.name, importPricatSupplier, hugoBossImportPricat);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objPricat = addSingleGroupObject(pricat);
            addPropertyDraw(objPricat, baseGroup);
            addObjectActions(this, objPricat);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierPricat, objPricat), Compare.EQUALS, objSupplier));
            setReadOnly(objSupplier, true);
            setReadOnly(importPricatSupplier, false, objSupplier.groupTo);
            setReadOnly(hugoBossImportPricat, false, objSupplier.groupTo);
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
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            FormInstance<?> form = (FormInstance<?>)executeForm.form;

            DataObject objShipment = keys.get(shipmentInterface);
            DataObject objSku = keys.get(skuInterface);

            DataObject objRouteRB = route.getDataObject("rb");
            DataObject objRouteRF = route.getDataObject("rf");

            Double invoiced = (Double) invoicedShipmentSku.read(session, modifier, objShipment, objSku);

            DataObject objRouteResult;
            if (invoiced == null) {
                Double percentRF = (Double)percentShipmentRouteSku.read(session, modifier, objShipment, objRouteRF, objSku);
                objRouteResult = (percentRF != null && percentRF > 1E-9) ? objRouteRF : objRouteRB;
            } else {

                Double invoicedRB = (Double) BaseUtils.nvl(invoicedShipmentRouteSku.read(session, modifier, objShipment, objRouteRB, objSku), 0.0);
                Double quantityRB = (Double)BaseUtils.nvl(quantityShipmentRouteSku.read(session, modifier, objShipment, objRouteRB, objSku), 0.0);

                Double invoicedRF = (Double)BaseUtils.nvl(invoicedShipmentRouteSku.read(session, modifier, objShipment, objRouteRF, objSku), 0.0);
                Double quantityRF = (Double)BaseUtils.nvl(quantityShipmentRouteSku.read(session, modifier, objShipment, objRouteRF, objSku), 0.0);

                if (quantityRB + 1E-9 < invoicedRB) {
                    objRouteResult = objRouteRB;
                } else
                    if (quantityRF + 1E-9 < invoicedRF) {
                        objRouteResult = objRouteRF;
                    } else
                        objRouteResult = objRouteRB;
            }

            ObjectInstance objectInstance = (ObjectInstance)mapObjects.get(routeInterface);
            if (!objRouteResult.equals(objectInstance.getObjectValue())) {
                try {
                    actions.add(new AudioClientAction(getClass().getResourceAsStream(
                        objRouteResult.equals(objRouteRB) ? "/audio/rb.wav" : "/audio/rf.wav"
                    )));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            form.seekObject(objectInstance, objRouteResult);
        }
    }

}
