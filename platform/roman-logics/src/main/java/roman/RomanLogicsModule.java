package roman;

import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.FormEventType;
import platform.interop.action.AudioClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.navigator.FormShowType;
import platform.server.Settings;
import platform.server.classes.*;
import platform.server.daemons.ScannerDaemonTask;
import platform.server.daemons.WeightDaemonTask;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.window.PanelNavigatorWindow;
import platform.server.form.window.ToolBarNavigatorWindow;
import platform.server.form.window.TreeNavigatorWindow;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.logics.property.actions.FormActionProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import static platform.server.logics.property.actions.ExecutePropertiesActionProperty.*;

/**
 * Created by IntelliJ IDEA.
 * User: DAle
 * Date: 24.05.11
 * Time: 15:14
 */


public class RomanLogicsModule extends LogicsModule {
    private final RomanBusinessLogics BL;
    private LP equalsColorItemSupplier;
    private LP equalsSizeItemSupplier;
    private LP equalsSeasonSupplierArticleSupplier;
    private LP equalsThemeItemSupplier;
    private LP boxInvoiceAddFA;
    private LP simpleInvoiceAddFA;
    private LP orderEditFA;
    private LP boxInvoiceEditFA;
    private LP simpleInvoiceEditFA;
    private LP freightCreateFA;
    private LP freightEditFA;
    private LP freightCompleteFA;
    private LP addNEColorSupplierSIDSupplier;
    private LP addNEColorSupplierSIDInvoice;
    private LP executeAddColorDocument;
    private LP seekColorSIDSupplier;
    private LP seekColorSIDInvoice;
    public LP itemArticleCompositeColorSize;
    private ConcreteCustomClass sizeGroupSupplier;
    private LP supplierSizeGroup;
    private LP groupSizeSupplier;
    private LP orderSizeSupplier;
    private LP equalsGroupSizeSupplier;
    private LP sizeGroupSupplierArticle;
    private LP nameSizeGroupSupplierArticle;
    private LP nameSupplierSizeGroup;
    private LP nameGroupSizeSupplier;
    private LP freightChangedFA;
    private LP freightPricedFA;
    private LP skuEditFA;
    private LP cloneItem;
    private LP addItemArticleCompositeColorSizeBarcode;
    private LP addItemSIDArticleSupplierColorSizeBarcode;
    private LP quantitySimpleInvoiceSimpleShipmentStockSku;
    private LP priceProxyImporterFreightSku;
    public LP contractInProxyImporterStockSku, sidContractInProxyImporterStockSku, dateContractInProxyImporterStockSku;
    private LP priceDirectImporterFreightSku;
    private LP RRPDirectImporterFreightSku;
    private LP RRPProxyImporterFreightSku;
    private LP RRPImporterFreightSku;


    public RomanLogicsModule(BaseLogicsModule<RomanBusinessLogics> baseLM, RomanBusinessLogics BL) {
        super("RomanLogicsModule");
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
    public LP itemSupplierArticleSIDColorSIDSizeSID;
    public LP round2;
    private LP articleSku;
    private ConcreteCustomClass order;
    private ConcreteCustomClass typeInvoice;
    private AbstractCustomClass invoice;
    protected AbstractCustomClass shipment;
    private ConcreteCustomClass boxShipment;
    private ConcreteCustomClass simpleShipment;
    private ConcreteCustomClass freight;
    private StaticCustomClass route;
    private StaticCustomClass typeTransit;
    private AbstractCustomClass seller;
    //private AbstractCustomClass buyer;
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
    private LP dateFromOrder;
    private LP dateToOrder;
    LP documentSIDSupplier;
    private LP sumSimpleInvoice;
    private LP sumInvoicedDocument;
    private LP sumDocument;
    ConcreteCustomClass commonSize;
    ConcreteCustomClass colorSupplier;
    ConcreteCustomClass sizeSupplier;
    ConcreteCustomClass gender;
    ConcreteCustomClass genderSupplier;
    private ConcreteCustomClass brandSupplier;
    public ConcreteCustomClass themeSupplier;
    public ConcreteCustomClass seasonSupplier;
    ConcreteCustomClass countrySupplier;
    LP supplierArticle;
    private LP nameSupplierArticle;
    private LP dateFromDataOrderArticle;
    private LP dateToDataOrderArticle;
    private LP dateFromOrderOrderArticle;
    private LP dateToOrderOrderArticle;
    private LP dateFromOrderArticle;
    private LP dateToOrderArticle;
    private LP dateFromOrderArticleSku;
    private LP dateToOrderArticleSku;
    LP priceDocumentArticle;
    LP RRPDocumentArticle;
    LP RRPRateDocumentSku;
    private LP sumDocumentArticle;
    private LP sumSimpleInvoiceArticle;
    LP colorSupplierItem;
    private LP nameColorSupplierItem;
    private LP inListArticleColorSupplier;
    LP sizeSupplierItem;
    public LP sidSizeSupplierItem;
    public LP commonSizeItem;
    public LP nameCommonSizeItem;
    LP supplierColorSupplier;
    LP genderSupplierArticle;
    LP sidGenderSupplierArticle;
    private LP quantityCreationStamp;
    private LP seriesOfCreationStamp;
    private LP dateOfCreationStamp;
    private LP nameSupplierColorSupplier;
    private LP nameSupplierThemeSupplier;
    private LP nameSupplierGenderSupplier;
    LP supplierSizeSupplier;
    private LP nameSupplierSizeSupplier;
    LP supplierGenderSupplier;
    private LP sidBrandSupplier;
    public LP sidTypeDuty;
    public LP nameTypeDuty;
    public LP sidToTypeDuty;
    public LP typeDutyDuty;
    private LP sidTypeDutyDuty;
    private LP nameTypeDutyDuty;
    private LP typeDutyNDS;
    private LP sidTypeDutyNDS;
    private LP nameTypeDutyNDS;
    private LP typeDutyRegistration;
    private LP sidTypeDutyRegistration;
    private LP nameTypeDutyRegistration;
    private LP supplierBrandSupplier;
    private LP nameSupplierBrandSupplier;
    private LP brandSupplierSupplier;
    private LP nameBrandSupplierSupplier;
    private LP brandSupplierArticle;
    private LP sidBrandSupplierArticle;
    private LP nameBrandSupplierArticle;
    private LP supplierBrandSupplierArticle;
    private LP countryBrandSupplier;
    private LP nameCountryBrandSupplier;
    private LP brandSupplierDataArticle;
    private LP brandSupplierSupplierArticle;
    private LP brandSupplierArticleSku;
    private LP sidBrandSupplierArticleSku;
    public LP nameBrandSupplierArticleSku;
    private LP nameBrandSupplierArticleSkuShipmentDetail;
    private LP supplierArticleSku;
    private LP nameSupplierArticleSku;
    public LP supplierThemeSupplier;
    public LP supplierSeasonSupplier;
    public LP sidDestinationSupplier;
    private LP nameSupplierSeasonSupplier;
    public LP seasonSupplierArticle;
    public LP sidSeasonSupplierArticle;
    public LP nameSeasonSupplierArticle;
    public LP themeSupplierArticle;
    public LP nameThemeSupplierArticle;
    public LP themeSupplierArticleSku;
    public LP sidThemeSupplierArticleSku;
    public LP nameThemeSupplierArticleSku;

    private ConcreteCustomClass currency;
    private ConcreteCustomClass typeExchange;
    public ConcreteCustomClass store;
    private ConcreteCustomClass unitOfMeasure;
    public LP relationStoreSupplier;
    private LP symbolCurrency;
    private LP currencyTypeExchange;
    private LP nameCurrencyTypeExchange;
    private LP rateExchange;
    private LP typeExchangeSTX;
    private LP nameTypeExchangeSTX;
    public LP typeExchangeCustom;
    public LP nameTypeExchangeCustom;
    public LP typeExchangePayCustom;
    public LP nameTypeExchangePayCustom;
    private LP currencyCustom;
    private LP nameCurrencyCustom;
    public LP currencyPayCustom;
    public LP nameCurrencyPayCustom;
    private LP NDSPercentCustom;
    private LP lessCmpDate;
    private LP nearestPredDate;
    public LP nearestRateExchange;
    public LP sidImporterFreightTypeInvoice;
    public LP sidImporterFreight;
    public LP sidDestination;
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
    private LP dateDepartureShipment;
    private LP dateArrivalShipment;
    private LP quantityPalletShipment;
    private LP grossWeightPallet;
    private LP grossWeightCurrentPalletRoute;
    private LP grossWeightFreight;
    private LP sumGrossWeightFreightSku;
    private LP grossWeightFreightSkuAggr;
    private LP netWeightShipment;
    private LP grossWeightShipment;
    LP sidColorSupplier;
    public LP sidColorSupplierItem;
    private LP sidThemeSupplierArticle;
    private LP quantityDocumentSku;
    private LP quantityDocumentBrandSupplier;
    private LP quantityAllDocumentsBrandSupplier;
    private LP quantitySimpleInvoiceArticle;
    private LP quantitySimpleInvoice;
    private LP quantityDocumentArticle;
    private LP quantityInvoicedDocumentArticle;
    private LP quantityListArticleCompositeColor;
    private LP quantityListArticleCompositeSize;
    private LP quantityListArticleCompositeColorSize;
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
    ConcreteCustomClass subCategory;
    ConcreteCustomClass typeDuty;

    private LP typeInvoiceCategory;
    private LP nameTypeInvoiceCategory;
    private LP categoryArticle;
    private LP unitOfMeasureDataArticle;
    private LP unitOfMeasureCategoryArticle;
    private LP nameOriginUnitOfMeasureArticleSku;
    private LP nameOriginCategoryArticle;
    private LP nameCategoryArticle;
    private LP categoryArticleSku;
    public LP nameCategoryArticleSku;
    private LP nameOriginCategoryArticleSku;
    private LP typeInvoiceCategoryArticle;
    private LP typeInvoiceCategoryArticleSku;
    LP sidCustomCategory4;
    LP sidCustomCategory6;
    LP sidCustomCategory9;
    LP sidCustomCategory10;
    LP certificatedCustomCategory10;
    LP sidCustomCategoryOrigin;
    LP numberIdCustomCategory10;
    LP numberIdCustomCategoryOrigin;
    LP sidToCustomCategory4;
    LP sidToCustomCategory6;
    LP sidToCustomCategory9;
    LP sidToCustomCategory10;
    LP sidToCustomCategoryOrigin;
    private LP importBelTnved;
    private LP importEuTnved;
    LP importTnvedCountryMinPrices;
    LP importTnvedDuty;
    LP customCategory4CustomCategory6;
    LP customCategory6CustomCategory9;
    LP customCategory9CustomCategory10;
    LP customCategory6CustomCategory10;
    LP customCategory4CustomCategory10;
    LP customCategory6CustomCategoryOrigin;
    LP customCategory4CustomCategoryOrigin;
    LP customCategory10CustomCategoryOrigin;
    LP sidCustomCategory10CustomCategoryOrigin;
    LP nameSubCategory;
    LP nameToSubCategory;
    LP relationCustomCategory10SubCategory;
    LP subCategoryCustomCategory10;
    LP countRelationCustomCategory10;
    LP diffCountRelationCustomCategory10Sku;
    LP diffCountRelationCustomCategory10FreightSku;
    LP minPriceCustomCategory10SubCategory;
    LP minPriceCustomCategory10SubCategoryCountry;
    public LP dutyPercentCustomCategory10TypeDuty;
    public LP dutySumCustomCategory10TypeDuty;
    private LP sidCustomCategory4CustomCategory6;
    private LP sidCustomCategory6CustomCategory9;
    private LP sidCustomCategory9CustomCategory10;
    public LP sidCustomCategory6CustomCategoryOrigin;
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
    private LP customCategory9Sku;
    private LP customCategory6FreightSku;
    private LP sidCustomCategory10Sku;
    private LP subCategoryDataSku;
    private LP subCategoryCustomCategory10Sku;
    private LP subCategorySku;
    private LP nameSubCategorySku;
    private LP nameSubCategoryDataSku;

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
    private LP countryOfOriginArticleColorSku;
    private LP countryOfOriginArticleSku;
    private LP nameCountryArticleColor;

    private LP genderGenderSupplier;
    private LP sidGenderGenderSupplier;
    private LP genderSupplierArticleSku;
    private LP sidGenderSupplierArticleSku;
    private LP countryOfOriginDataSku;
    private LP countryOfOriginSku;
    private LP nameCountryOfOriginSku;
    private LP nameCountrySupplierOfOriginArticle;
    private LP nameCountryOfOriginArticleSku;

    private LP genderBrandSupplier;
    private LP sidGenderBrandSupplier;
    private LP genderBrandSupplierArticle;
    private LP genderOriginArticle;
    private LP genderDataArticle;
    private LP genderArticle;
    private LP sidGenderArticle;
    private LP genderArticleSku;
    public LP sidGenderArticleSku;
    private LP quantitySizeSupplierGenderCategory;
    private LP commonSizeSku;
    public LP nameCommonSizeSku;
    public LP typeFabricArticle;
    public LP typeFabricArticleSku;
    public LP nameTypeFabricArticle;
    public LP nameTypeFabricArticleSku;
    LP articleSIDSupplier;
    private LP seekArticleSIDSupplier;
    private LP seekArticleSIDInvoice;
    LP numberListArticle;
    LP notZeroListArticle;
    private LP articleSIDList;
    private LP incrementNumberListSID;
    private LP addArticleSingleSIDSupplier;
    private LP addNEArticleSingleSIDSupplier;
     private LP addNEArticleSingleSIDInvoice;
    private LP addArticleCompositeSIDSupplier;
    private LP addNEArticleCompositeSIDSupplier;
    private LP addNEArticleCompositeSIDInvoice;
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
    public ConcreteCustomClass simpleInvoice;
    LP sidSupplierBox;
    private AbstractCustomClass list;
    LP quantityDataListSku;
    LP quantityDataList;
    LP boxInvoiceSupplierBox;
    private LP sidBoxInvoiceSupplierBox;
    private LP supplierSupplierBox;
    private LP supplierList;
    private LP orderedSupplierBoxSku;
    private LP quantityListArticle;
    private LP orderedSimpleInvoiceSku;
    LP priceDataDocumentItem;
    private LP priceArticleDocumentSku;
    private LP minPriceCustomCategoryFreightSku;
    private LP minPriceCustomCategoryCountryFreightSku;
    private LP minPriceRateCustomCategoryFreightSku;
    private LP minPriceRateFreightSku;
    private LP minPriceRateImporterFreightSku;
    private LP minPriceRateImporterFreightArticle;
    private LP minPriceRateWeightImporterFreightSku;
    private LP diffPriceMinPriceImporterFreightArticle;
    private LP diffPriceMinPriceImporterFreightSku;
    private LP greaterPriceMinPriceImporterFreightArticle;
    private LP greaterPriceMinPriceImporterFreightSku;
    private LP dutyNetWeightFreightSku;
    private LP dutyNetWeightImporterFreightSku;
    private LP dutyPercentImporterFreightSku;
    private LP dutyImporterFreightSku;
    private LP sumDutyImporterFreightSku;
    private LP sumDutyImporterFreight;
    private LP priceDutyImporterFreightSku;
    private LP NDSPercentOriginFreightSku;
    private LP NDSPercentCustomFreightSku;
    private LP NDSPercentFreightSku;
    private LP NDSImporterFreightSku;
    private LP sumNDSImporterFreightSku;
    private LP sumNDSImporterFreight;
    private LP sumRegistrationFreightSku;
    private LP sumRegistrationImporterFreightSku;
    private LP sumRegistrationImporterFreight;
    private LP sumCustomImporterFreight;
    private LP minPriceRateCustomCategoryCountryFreightSku;
    private LP priceImporterFreightSku;
    private LP priceDocumentSku;
    private LP priceRateDocumentArticle;
    private LP priceRateDocumentSku;
    private LP RRPRateDocumentArticle;
    private LP sumDocumentSku;
    private LP inOrderShipment;
    private LP inInvoiceShipment;
    private LP descriptionFreight;
    private LP tonnageFreight;
    private LP tonnageDataFreight;
    private LP palletCountFreight;
    private LP palletCountDataFreight;
    private LP volumeFreight;
    private LP volumeDataFreight;
    private LP currencyFreight;
    private LP nameCurrencyFreight;
    private LP symbolCurrencyFreight;
    private LP sumFreightFreight;
    private LP insuranceFreight;
    private LP insuranceFreightBrandSupplier;
    private LP insuranceFreightBrandSupplierArticle;
    private LP insuranceFreightBrandSupplierSku;
    private LP routeFreight;
    private LP nameRouteFreight;
    private LP dateArrivalFreight;
    private LP exporterFreight;
    LP nameOriginExporterFreight;
    LP nameExporterFreight;
    LP addressOriginExporterFreight;
    LP addressExporterFreight;
    private LP inInvoiceFreight;
    private LP netWeightInvoicedFreight;
    public LP contractImporterFreight;
    //public LP nameContractImporterFreight;
    public LP sidContractImporterFreight;
    public LP dateContractImporterFreight;
    private LP conditionShipmentContractImporterFreight;
    private LP conditionPaymentContractImporterFreight;
    public LP dateImporterFreightTypeInvoice;
    public LP dateImporterFreight;
    private LP dateShipmentImporterFreightTypeInvoice;
    private ConcreteCustomClass stock;
    private ConcreteCustomClass freightBox;
    private ConcreteCustomClass typeFabric;
    public LP sidArticleSku;
    public LP originalNameArticleSku;
    public LP coefficientArticle;
    public LP coefficientArticleSku;
    private LP inSupplierBoxShipment;
    private LP quantityArticle;
    private LP quantityShipSku;
    private LP quantitySupplierBoxBoxShipmentStockSku;
    private LP quantitySupplierBoxBoxShipmentSku;
    private LP quantitySimpleShipmentStockSku;
    private LP quantitySimpleShipmentStockItem;
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
    private LP invoicedBetweenDateSku;
    private LP invoicedBetweenDateBrandSupplier;
    private LP quantityShipmentedBetweenDateSku;
    private LP quantityShipmentedBetweenDateBrandSupplier;
    private LP quantityShipmentedSku;
    private LP emptyBarcodeShipment;
    private LP priceShipmentSku;
    private LP invoicedShipment;
    private LP invoicedShipmentRouteSku;
    private LP zeroInvoicedShipmentRouteSku;
    private LP zeroQuantityShipmentRouteSku;
    private LP diffShipmentRouteSku;
    private LP sumShipmentRouteSku;
    private LP sumShipmentRoute;
    private LP sumShipmentSku;
    private LP sumShipmentArticleColor;
    private LP sumShipment;
    private LP invoicedShipmentRoute;

    private LP documentList;
    private LP numberDocumentArticle;
    private ConcreteCustomClass shipDimension;
    private LP quantityShipDimensionShipmentStockSku;
    private LP quantityShipmentSku;
    private LP orderedOrderShipmentSku;
    private LP quantityOrderShipmentSku;
    private LP shipmentedOrderSku;
    private LP shipmentedAtTermOrderSku;
    private LP quantityShipment;
    private LP barcodeCurrentPalletRoute;
    private LP barcodeCurrentFreightBoxRoute;
    private LP equalsPalletFreight;
    private LP equalsPalletFreightBox;
    private ConcreteCustomClass freightType;
    private ConcreteCustomClass creationSku;
    private ConcreteCustomClass creationFreightBox;
    private ConcreteCustomClass creationPallet;
    private LP quantityCreationSku;
    private LP quantityCreationPallet;
    private LP routeCreationPallet;
    private LP nameRouteCreationPallet;
    private LP creationSkuSku;
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
    private LP freightBoxNumberFreight;
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
    private LP quantityShipmentArticleColorSize;
    private LP quantityShipmentSize;
    private LP oneShipmentArticleSize;
    private LP oneShipmentArticleColor;

    private LP oneShipmentSku;
    private LP quantityBoxShipment;
    private LP quantityShipmentStock;
    private LP quantityShipmentPallet;
    private LP quantityShipmentFreight;
    private LP quantityShipmentFreightSku;
    private LP quantityShipmentedFreightArticle;
    private LP quantityShipmentedFreightBrandSupplier;
    private LP importerShipmentFreight;
    private LP nameImporterShipmentFreight;
    private LP balanceStockSku;
    private LP quantityStockSku;
    private LP quantityFreightUnitSku;
    private LP quantityStock;
    private LP quantityStockArticle;
    private LP quantityStockBrandSupplier;
    private LP quantityFreightUnitBrandSupplier;
    private LP stockNumberFreightBrandSupplier;
    private LP quantityPalletSku;
    private LP quantityPalletBrandSupplier;
    private LP quantityAllPalletsBrandSupplier;
    private LP quantityBrandSupplier;
    private LP quantityRouteSku;
    private AbstractCustomClass destinationDocument;
    private LP destinationInvoiceSupplierBox;
    private LP destinationSupplierBox;
    private LP destinationFreightBox;
    private LP nameDestinationFreightBox;
    private LP nameDestinationInvoiceSupplierBox;
    private LP nameDestinationSupplierBox;
    public LP destinationDataSupplierBox;
    public LP sidDestinationDataSupplierBox;
    public LP nameDestinationDataSupplierBox;
    private LP destinationCurrentFreightBoxRoute;
    private LP nameDestinationCurrentFreightBoxRoute;
    private LP quantityShipDimensionStock;
    private LP isStoreFreightBoxSupplierBox;
    private LP barcodeActionSetStore;
    public AbstractCustomClass destination;
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
    private LP addBoxShipmentDetailBoxShipmentSupplierBoxStockSku;
    private LP addSimpleShipmentDetailSimpleShipmentStockSku;
    private LP addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode;
    private LP addBoxShipmentDetailBoxShipmentSupplierBoxRouteSku;
    private LP addSimpleShipmentDetailSimpleShipmentRouteSku;
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
    private LP nameCategoryArticleSkuShipmentDetail;
    private LP coefficientArticleSkuShipmentDetail;
    private LP customCategoryOriginArticleSkuShipmentDetail;
    private LP sidCustomCategoryOriginArticleSkuShipmentDetail;
    private LP netWeightArticleSkuShipmentDetail;
    private LP countryOfOriginArticleSkuShipmentDetail;
    private LP nameCountryOfOriginArticleSkuShipmentDetail;
    private LP countryOfOriginSkuShipmentDetail;
    private LP nameCountryOfOriginSkuShipmentDetail;
    private LP genderArticleSkuShipmentDetail;
    private LP sidGenderArticleSkuShipmentDetail;
    private LP typeFabricArticleSkuShipmentDetail;
    private LP nameTypeFabricArticleSkuShipmentDetail;
    private LP mainCompositionOriginArticleSkuShipmentDetail;
    private LP mainCompositionOriginSkuShipmentDetail;
    private LP additionalCompositionOriginSkuShipmentDetail;
    private LP unitOfMeasureArticleSkuShipmentDetail;
    private LP nameOriginUnitOfMeasureArticleSkuShipmentDetail;
    private LP nameUnitOfMeasureArticleSkuShipmentDetail;
    private LP userShipmentDetail;
    private LP nameUserShipmentDetail;
    private LP timeShipmentDetail;
    private LP createSku;
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
    LP customCategory6Article;
    private LP sidCustomCategory6Article;
    private LP customCategoryOriginArticleSku;
    private LP sidCustomCategoryOriginArticle;
    private LP sidCustomCategoryOriginArticleSku;
    private LP quantityBoxInvoiceBoxShipmentStockSku;
    private LP quantityInvoiceShipmentStockSku;
    private LP invoicedSimpleInvoiceSimpleShipmentStockSku;
    private LP invoicedSimpleInvoiceSimpleShipmentStockArticleComposite;
    private LP invoicedSimpleInvoiceSimpleShipmentStockItem;
    private LP quantityDataSimpleInvoiceSimpleShipmentStockSku;
    private LP quantitySimpleInvoiceSimpleShipmentStockItem;
    private LP quantitySkuSimpleInvoiceSimpleShipmentStockSku;
    private LP quantityArticleSimpleInvoiceSimpleShipmentStockItem;
    private LP quantityInvoiceStockSku;
    private LP quantityInvoiceSku;
    private LP quantityInvoice;
    private LP diffDocumentInvoiceSku;
    private LP quantitySupplierBoxSku;
    private LP quantityDirectSupplierBoxSku;
    private LP quantitySupplierBox;
    private LP zeroQuantityListSku;
    private LP zeroQuantityShipDimensionShipmentSku;
    private LP diffListSupplierBoxSku;
    private LP diffListSupplierBox;
    private LP zeroQuantityShipmentSku;
    private LP zeroInvoicedShipmentSku;
    private LP diffShipmentSku;
    private ConcreteCustomClass importer;
    private ConcreteCustomClass exporter;
    private LP sidContract;
    private LP dateContract;
    private LP conditionShipmentContract;
    private LP conditionPaymentContract;
    //private LP buyerContract;
    //private LP nameBuyerContract;
    private LP sellerContract;
    private LP nameSellerContract;
    private LP subjectContract;
    private LP nameSubjectContract;

    private LP currencyContract;
    private LP nameCurrencyContract;
    private LP contractImporter;
    public LP sidImporter;
    private LP exporterInvoice, exporterProxyInvoice;
    private LP nameExporterInvoice;
    private LP importerDirectInvoice;
    private LP nameImporterDirectInvoice;
    private LP contractInvoice;
    private LP sidContractInvoice;
    private LP freightFreightBox;
    private LP importerSupplierBox;
    private LP routeFreightFreightBox;
    private LP importerShipmentRoute;
    private LP nameImporterShipmentRoute;
    private LP importerShipmentFreightBox;
    public LP quantityImporterStockSku;
    private LP quantityDirectImporterFreightUnitSku;
    private LP quantityImporterStock;
    private LP quantityImporterStockArticle;
    private LP quantityImporterStockTypeInvoice;
    private LP quantityProxyImporterFreightSku;
    private LP quantityDirectImporterFreightSku;
    private LP quantityImporterFreightSku;

    private LP netWeightStockSku;
    private LP netWeightStockArticle;
    private LP netWeightStock;
    private LP netWeightImporterFreightUnitSku;
    private LP netWeightImporterFreightUnitArticle;
    private LP netWeightImporterFreightUnitTypeInvoice;
    private LP netWeightImporterFreightUnit;
    private LP grossWeightImporterFreightUnitSku;
    private LP grossWeightImporterFreightUnitArticle;
    private LP grossWeightImporterFreightUnitTypeInvoice;
    private LP grossWeightImporterFreightUnit;
    private LP priceInInvoiceStockSku;
    private LP RRPInInvoiceStockSku;
    private LP contractInInvoiceStockSku;
    private LP priceInInvoiceShipmentStockSku;
    private LP RRPInInvoiceShipmentStockSku;
    private LP contractInInvoiceShipmentStockSku;
    private LP priceInShipmentStockSku;
    private LP RRPInShipmentStockSku;
    private LP priceInShipmentDetail;
    private LP contractInShipmentStockSku;
    public LP priceInImporterFreightSku;
    public LP RRPInImporterFreightSku;
    private LP netWeightImporterFreightSku;
    private LP netWeightImporterFreightArticle;
    private LP netWeightProxyImporterFreightSku;
    private LP netWeightDirectImporterFreightSku;
    private LP typeInvoiceDataFreightArticle;
    private LP typeInvoiceCategoryFreightArticle;
    private LP typeInvoiceFreightArticle;
    private LP typeInvoiceFreightSku;
    private LP nameTypeInvoiceFreightArticleSku;
    private LP netWeightImporterFreightTypeInvoice;
    private LP grossWeightImporterFreightSku;
    private LP grossWeightProxyImporterFreightSku;
    private LP grossWeightDirectImporterFreightSku;
    private LP grossWeightImporterFreightTypeInvoice;
    private LP netWeightImporterFreightCustomCategory6;
    private LP netWeightImporterFreightCustomCategory6Category;
    private LP netWeightImporterFreightSupplierCustomCategory6;
    private LP netWeightImporterFreightSupplierCustomCategory6Category;
    private LP grossWeightImporterFreightCustomCategory6;
    private LP grossWeightImporterFreightCustomCategory6Category;
    private LP grossWeightImporterFreightSupplierCustomCategory6;
    private LP grossWeightImporterFreightSupplierCustomCategory6Category;
    private LP netWeightImporterFreight;
    private LP netWeightImporterFreightSupplier;
    private LP grossWeightImporterFreightSupplier;
    private LP grossWeightImporterFreight;
    private LP priceFreightImporterFreightSku;
    private LP priceInsuranceImporterFreightSku;
    private LP priceFullImporterFreightSku;
    private LP priceFullKgImporterFreightSku;
    private LP sumFullImporterFreightArticle;
    private LP priceFullKgImporterFreightArticle;
    private LP priceFullDutyImporterFreightSku;
    private LP oneArticleSkuShipmentDetail;
    private LP oneArticleColorShipmentDetail;
    private LP oneArticleSizeShipmentDetail;
    private LP oneSkuShipmentDetail;
    private LP quantityImporterFreight;
    private LP quantityFreight;
    private LP quantityProxyImporterFreight;
    private LP quantityImporterFreightTypeInvoice;
    private LP quantityImporterFreightSupplier;
    private LP quantityImporterFreightArticle;
    private LP quantityImporterFreightBrandSupplier;
    private LP markupPercentImporterFreightBrandSupplier;
    private LP markupPercentImporterFreightBrandSupplierArticle;
    private LP markupPercentImporterFreightDataArticle;
    private LP markupPercentImporterFreightArticle;
    private LP markupPercentImporterFreightArticleSku;
    private LP markupPercentImporterFreightDataSku;
    private LP markupPercentImporterFreightBrandSupplierSku;
    private LP markupInImporterFreightSku;
    private LP priceExpenseImporterFreightSku;
    private LP markupPercentImporterFreightSku;
    private LP sumPercentImporterFreightBrandSupplier;
    private LP averagePercentImporterFreightBrandSupplier;
    private LP markupImporterFreightSku;
    private LP priceMarkupInImporterFreightSku;
    private LP priceOutImporterFreightSku;
    private LP priceInOutImporterFreightSku;
    public LP sumInImporterStockSku, sumInImporterFreightSku;
    private LP sumInProxyImporterFreightSku;
    private LP sumMarkupImporterFreightSku;
    private LP sumOutImporterFreightSku;
    private LP sumInImporterFreight;
    private LP sumMarkupImporterFreight;
    private LP sumMarkupInImporterFreight;
    private LP sumMarkupInImporterFreightSku;
    private LP sumInOutImporterFreightSku;
    private LP sumInOutProxyImporterFreightSku;
    private LP sumInOutDirectImporterFreightSku;
    private LP sumInOutImporterFreightTypeInvoice;
    private LP sumImporterFreightUnitSku;
    private LP sumImporterFreightUnitArticle;
    private LP sumMarkupInFreight;
    private LP sumImporterFreight;
    private LP sumImporterFreightTypeInvoice;
    private LP sumSbivkaImporterFreight;
    private LP sumInImporterFreightArticle;
    private LP sumInImporterFreightBrandSupplier;
    private LP sumInOutImporterFreightArticle;
    private LP sumInOutImporterFreightBrandSupplier;
    private LP sumImporterFreightSupplier;
    private LP sumInFreightArticle;
    private LP sumInFreightBrandSupplier;
    private LP sumInOutFreightArticle;
    private LP sumInOutFreightBrandSupplier;
    private LP sumInOutFreightBrandSupplierArticle;
    private LP sumInOutFreightBrandSupplierSku;
    private LP sumOutImporterFreight;
    private LP sumInOutImporterFreight;
    private LP sumInOutFreight;
    private LP sumInFreight;
    private LP sumMarkupFreight;
    private LP sumOutFreight;
    private LP sumFreightImporterFreightSku;
    private LP insuranceImporterFreightSku;
    private LP quantityProxyImporterFreightCustomCategory6Category;
    private LP quantityProxyImporterFreightCustomCategory6;
    private LP quantityDirectImporterFreightSupplierCustomCategory6;
    private LP quantityDirectImporterFreightSupplierCustomCategory6Category;
    private LP quantityFreightArticle;
    private LP quantityDirectFreightSku;
    private LP quantityDirectImporterFreightSupplier;
    private LP quantityFreightBrandSupplier;
    private LP quantityFreightSupplier;
    private LP quantityFreightSku;
    private LP quantityFreightedBetweenDateSku;
    private LP quantityFreightedSku;
    private LP balanceSku;
    private LP quantityFreightCategory;
    private LP sumImporterFreightSku;
    private LP sumProxyImporterFreightSku;
    private LP sumDirectImporterFreightSku;
    private LP sumImporterFreightCustomCategory6;
    private LP sumImporterFreightCustomCategory6Category;
    private LP sumImporterFreightSupplierCustomCategory6;
    private LP sumImporterFreightSupplierCustomCategory6Category;

    private LP quantityFreightCategoryGenderCompositionTypeFabric;
    private LP customCategory10CategoryGenderCompositionTypeFabric;
    private LP sidCustomCategory10CategoryGenderCompositionTypeFabric;
    private LP customCategory10CategoryGenderCompositionTypeFabricSku;

    LP quantityImporterFreightArticleCompositionCountryCategory;
    LP compositionFreightArticleCompositionCountryCategory;
    LP netWeightImporterFreightArticleCompositionCountryCategory;
    LP grossWeightImporterFreightArticleCompositionCountryCategory;
    LP priceImporterFreightArticleCompositionCountryCategory;
    public LP priceInvoiceImporterFreightSku;
    LP markupInOutImporterFreightSku;
    public LP sumInvoiceImporterStockSku;
    LP sumImporterFreightArticleCompositionCountryCategory;
    LP sumProxyInvoiceImporterFreightSku;
    private ConcreteCustomClass freightComplete;
    private ConcreteCustomClass freightPriced;
    private ConcreteCustomClass freightChanged;
    private ConcreteCustomClass freightShipped;
    private ConcreteCustomClass freightArrived;
    private LP dictionaryComposition;
    private LP nameDictionaryComposition;
    private LP translationMainCompositionSku;
    private LP translationAdditionalCompositionSku;
    private LP translationAllMainComposition;
    private LP translationMainCompositionFreightSku;
    private LP translationAdditionalCompositionFreightSku;
    private LP sidShipmentShipmentDetail;
    private LP commonSizeSizeSupplier;
    private LP nameCommonSizeSizeSupplier;
    private LP commonSizeSizeSupplierGenderCategory;
    private LP nameCommonSizeSizeSupplierGenderCategory;
    LP colorSIDSupplier;
    LP sidSizeSupplier;
    LP sidThemeSupplier;
    LP sidGender;
    LP sidGenderSupplier;
    LP sizeSIDSupplier;
    LP themeSIDSupplier;
    LP sidSeasonSupplier;
    LP seasonSIDSupplier;
    LP seasonSupplierArticleSku;
    LP sidSeasonSupplierArticleSku;
    LP genderSIDSupplier;
    LP destinationSIDSupplier;
    LP brandSIDSupplier;
    LP countryNameSupplier;
    LP numberDataListSku;
    private LP numberArticleListSku;
    private LP grossWeightFreightSku;
    private LP netWeightFreightSku;
    private LP customCategory10FreightSku;
    private LP sidCustomCategory10FreightSku;
    private LP subCategoryFreightSku;
    private LP nameSubCategoryFreightSku;
    private LP mainCompositionOriginFreightSku;
    public LP mainCompositionFreightSku;
    private LP additionalCompositionOriginFreightSku;
    private LP additionalCompositionFreightSku;
    private LP countryOfOriginFreightSku;
    private LP sidCountryOfOriginFreightSku;
    public LP nameCountryOfOriginFreightSku;
    private LP equalsItemArticleComposite;
    private LP executeArticleCompositeItemSIDSupplier;
    private LP executeChangeFreightClass, executeChangeFreightClassApply, executeChangeFreightChangedClass, executeChangeFreightShippedClass;
    private CreateItemFormEntity createItemForm;
    private EditItemFormEntity editItemForm;
    private FindItemFormEntity findItemFormBox, findItemFormBoxBarcode;
    private FindItemFormEntity findItemFormSimple, findItemFormSimpleBarcode;
    private LogFormEntity logFreightForm;
    private LP formLogFreight;

    private LP addItemBarcode;
    private LP barcodeActionSeekFreightBox;
    private LP currentPalletFreightBox;
    private LP barcodeActionCheckPallet;
    private LP barcodeActionCheckFreightBox;
    private LP barcodeActionCheckChangedFreightBox;
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
    LP seekRouteShipmentSkuRoute;
    LP barcodeActionSeekPallet, barcodeActionSetPallet, barcodeActionSetPalletFreightBox, barcodeActionSetFreight, barcodeAction3;
    private LP invoiceOriginFormImporterFreight;
    private LP invoiceFormImporterFreight;
    private LP invoiceExportFormImporterFreight;
    private LP proformOriginFormImporterFreight;
    private LP proformFormImporterFreight;
    private LP annexInvoiceOriginFormImporterFreight;
    private LP annexInvoiceFormImporterFreight;
    private LP packingListOriginFormImporterFreight;
    private LP packingListFormImporterFreight;
    private LP sbivkaFormImporterFreight;
    private LP sbivkaFormImporterFreightSupplier;
    private LP listFreightUnitFormImporterFreight;

    private LP countrySupplierOfOriginArticleSku;
    private LP nameCountrySupplierOfOriginArticleSku;
    private AbstractCustomClass directInvoice;
    private ConcreteCustomClass directBoxInvoice;
    private LP freightDirectInvoice;
    private LP equalsDirectInvoiceFreight;
    private LP grossWeightDirectInvoice;
    private LP palletNumberDirectInvoice;
    private LP nameOriginCountry;
    public LP sidOrigin2Country;
    private LP sidOrigin3Country;
    private LP sid3Country;
    public  LP sidOrigin2ToCountry;
    private LP nameCountrySku;
    public LP countryBrandSupplierSku;
    public LP nameCountryBrandSupplierSku;
    private LP sumInCurrentYear;
    private LP sumInOutCurrentYear;
    private LP balanceSumCurrentYear;
    private AbstractCustomClass freightUnit;
    private LP quantityInvoiceFreightUnitSku;
    private LP freightSupplierBox;
    private LP freightFreightUnit;
    private LP priceRateSupplierBoxSku;
    private LP RRPRateSupplierBoxSku;
    private LP priceInInvoiceFreightUnitSku;
    ConcreteCustomClass boxSupplier;
    ConcreteCustomClass simpleSupplier;
    ConcreteCustomClass jennyferSupplier;
    ConcreteCustomClass dieselSupplier;
    ConcreteCustomClass steilmannSupplier;
    ConcreteCustomClass tallyWeijlSupplier;
    ConcreteCustomClass hugoBossSupplier;
    ConcreteCustomClass gerryWeberSupplier;
    ConcreteCustomClass topazSupplier;
    ConcreteCustomClass aprioriSupplier;
    ConcreteCustomClass mexxSupplier;
    ConcreteCustomClass bestsellerSupplier;
    ConcreteCustomClass sOliverSupplier;
    ConcreteCustomClass womenSecretSupplier;
    ConcreteCustomClass babyPhatSupplier;
    private LP steilmannImportInvoice;
    private LP dieselImportInvoice;
    private LP jennyferImportInvoice;
    private LP jennyferImportArticleWeightInvoice;
    private LP tallyWeijlImportInvoice;
    private LP hugoBossImportInvoice;
    private LP gerryWeberImportInvoice;
    public LP mexxImportInvoice;
    public LP mexxImportPricesInvoice;
    public LP mexxImportArticleInfoInvoice;
    public LP mexxImportColorInvoice;
    private LP mexxImportDelivery;
    private LP bestsellerImportInvoice;
    private LP hugoBossImportPricat;
    private LP gerryWeberImportPricat;
    private LP sOliverImportInvoice;
    private LP womenSecretImportInvoice;
    private LP topazImportInvoice;
    private LP aprioriImportInvoice;

    private AbstractGroup importInvoiceActionGroup;
    private LP skuPrintFA;
    private LP printCreateSkuForm;
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
    private LP quantityAllSku;
    private LP sumInInvoiceStockSku;
    private LP sumStockedSku;
    private LP sumDirectInvoicedSku;
    private LP sumSku;
    private LP netWeightDocumentSku;
    private LP barcode10;
    private LP steilmannSupplierArticle;
    private LP skuJennyferBarcode10;
    private LP jennyferSupplierArticle;
    private LP jennyferSupplierArticleSku;
    private LP substring10;
    private LP skuJennyferBarcode;
    private LP substring10s13;
    private LP skuBarcodeObject;

    private LP typeSupplier;
    private LP noBarcodeSupplier;
    private LP nameClassFreight;
    private LP logFreight;

    ConcreteCustomClass pricat;
    LP barcodePricat;
    LP articleNumberPricat;
    LP customCategoryOriginalPricat;
    LP themeCodePricat;
    LP themeNamePricat;
    LP colorCodePricat;
    LP colorNamePricat;
    LP sizePricat;
    LP seasonPricat;
    LP genderPricat;
    LP originalNamePricat;
    LP countryPricat;
    LP netWeightPricat;
    LP compositionPricat;
    LP pricePricat;
    LP rrpPricat;
    LP supplierPricat;
    LP barcodeToPricat;
    LP importPricatSupplier;
    LP destinationPricat;

    private ConcreteCustomClass stamp;
    private ConcreteCustomClass creationStamp;
    LP sidStamp;
    LP dateOfStamp;
    LP seriesOfStamp;
    LP stampShipmentDetail;
    LP sidStampShipmentDetail;
    LP seriesOfStampShipmentDetail;
    LP hideSidStampShipmentDetail;
    LP hideSeriesOfStampShipmentDetail;
    LP necessaryStampCategory;
    LP necessaryStampSkuShipmentDetail;
    LP shipmentDetailStamp;
    LP firstNumberCreationStamp;
    LP lastNumberCreationStamp;
    LP dateOfStampCreationStamp;
    LP seriesOfStampCreationStamp;
    LP createStamp;
    LP creationStampStamp;
    private ConcreteCustomClass transitDocument;
    private LP sidTransitDocument;
    private LP dateRepaymentTransitDocument;
    private LP dateClosingTransitDocument;
    private LP sellerTransitDocument;
    private LP nameSellerTransitDocument;
    private LP importerTransitDocument;
    private LP freightTransitDocument;
    private LP typeTransitTransitDocument;
    private LP nameTypeTransitTransitDocument;

    LP scalesSpeed;
    LP scalesComPort;
    LP scannerComPort;

    private LP declarationExport;
    private LP invoiceExportDbf;

    public AnnexInvoiceFormEntity invoiceFromFormEntity;
    public InvoiceExportFormEntity invoiceExportForm;

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

        pricat = addConcreteClass("pricat", "Прикат", baseClass);

        item = addConcreteClass("item", "Товар", sku);

        document = addAbstractClass("document", "Документ", baseLM.transaction);
        list = addAbstractClass("list", "Список", baseClass);

        contract = addConcreteClass("contract", "Договор", baseLM.transaction);

        priceDocument = addAbstractClass("priceDocument", "Документ с ценами", document);
        destinationDocument = addAbstractClass("destinationDocument", "Документ в пункт назначения", document);

        order = addConcreteClass("order", "Заказ", priceDocument, destinationDocument, list);

        typeInvoice = addConcreteClass("typeInvoice", "Тип инвойса", baseClass.named);

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
        //buyer = addAbstractClass("buyer", "Покупатель", baseClass.named);

        supplier = addConcreteClass("supplier", "Поставщик", baseClass.named, seller);

        boxSupplier = addConcreteClass("boxSupplier", "Поставщик по коробам", supplier);
        simpleSupplier = addConcreteClass("simpleSupplier", "Поставщик без коробов", supplier);

        jennyferSupplier = addConcreteClass("jennyferSupplier", "Jennyfer", boxSupplier);
        dieselSupplier = addConcreteClass("dieselSupplier", "Diesel", boxSupplier);
        steilmannSupplier = addConcreteClass("steilmannSupplier", "Steilmann", boxSupplier);
        tallyWeijlSupplier = addConcreteClass("tallyWeijlSupplier", "Tally Weijl", boxSupplier);
        hugoBossSupplier = addConcreteClass("hugoBossSupplier", "Hugo Boss", simpleSupplier);
        mexxSupplier = addConcreteClass("mexxSupplier", "Mexx", boxSupplier);
        bestsellerSupplier = addConcreteClass("bestsellerSupplier", "Bestseller", boxSupplier);
        sOliverSupplier = addConcreteClass("sOliverSupplier", "s.Oliver", simpleSupplier);
        womenSecretSupplier = addConcreteClass("womenSecretSupplier", "Women'Secret", boxSupplier);
        babyPhatSupplier = addConcreteClass("babyPhatSupplier", "Baby Phat", simpleSupplier);
        gerryWeberSupplier = addConcreteClass("gerryWeberSupplier", "Gerry Weber", simpleSupplier);
        topazSupplier = addConcreteClass("topazSupplier", "Topaz", simpleSupplier);
        aprioriSupplier = addConcreteClass("aprioriSupplier", "Apriori", simpleSupplier);

        secondNameClass = addAbstractClass("secondNameClass", "Класс со вторым именем", baseClass);

        subject = addAbstractClass("subject", "Субъект", baseClass.named, secondNameClass);
        importer = addConcreteClass("importer", "Импортер", subject);
        exporter = addConcreteClass("exporter", "Экспортер", subject, seller);

        commonSize = addConcreteClass("commonSize", "Размер", baseClass.named);

        colorSupplier = addConcreteClass("colorSupplier", "Цвет поставщика", baseClass.named);
        sizeSupplier = addConcreteClass("sizeSupplier", "Размер поставщика", baseClass);
        gender = addConcreteClass("gender", "Пол", baseClass);
        genderSupplier = addConcreteClass("genderSupplier", "Пол поставщика", baseClass);
        sizeGroupSupplier = addConcreteClass("sizeGroupSupplier", "Размерная сетка", baseClass.named);

        typeFabric = addConcreteClass("typeFabric", "Тип одежды", baseClass.named);

        freightBox = addConcreteClass("freightBox", "Короб для транспортировки", stock, freightUnit);

        freight = addConcreteClass("freight", "Фрахт", baseClass.named, baseLM.transaction);
        freightComplete = addConcreteClass("freightComplete", "Скомплектованный фрахт", freight);
        freightChanged = addConcreteClass("freightChanged", "Обработанный фрахт", freightComplete);
        freightPriced = addConcreteClass("freightPriced", "Расцененный фрахт", freightChanged);
        freightShipped = addConcreteClass("freightShipped", "Отгруженный фрахт", freightPriced);
        freightArrived = addConcreteClass("freightArrived", "Прибывший в РБ фрахт", freightShipped);

        freightType = addConcreteClass("freightType", "Тип машины", baseClass.named);

        pallet = addConcreteClass("pallet", "Паллета", baseLM.barcodeObject);

        category = addConcreteClass("category", "Номенклатурная группа", secondNameClass, baseClass.named);

        customCategory = addAbstractClass("customCategory", "Уровень ТН ВЭД", baseClass);

        customCategory4 = addConcreteClass("customCategory4", "Первый уровень", customCategory);
        customCategory6 = addConcreteClass("customCategory6", "Второй уровень", customCategory);
        customCategory9 = addConcreteClass("customCategory9", "Третий уровень", customCategory);
        customCategory10 = addConcreteClass("customCategory10", "Четвёртый уровень", customCategory);

        customCategoryOrigin = addConcreteClass("customCategoryOrigin", "ЕС уровень", customCategory);

        subCategory = addConcreteClass("subCategory", "Дополнительное деление", baseClass);

        typeDuty = addConcreteClass("typeDuty", "Тип пошлины", baseClass);

        creationSku = addConcreteClass("creationSku", "Операция создания товаров", baseLM.transaction);
        creationFreightBox = addConcreteClass("creationFreightBox", "Операция создания коробов", baseLM.transaction);
        creationPallet = addConcreteClass("creationPallet", "Операция создания паллет", baseLM.transaction);
        creationStamp = addConcreteClass("creationStamp", "Операция создания марок", baseLM.transaction);

        transfer = addConcreteClass("transfer", "Внутреннее перемещение", baseClass);

        unitOfMeasure = addConcreteClass("unitOfMeasure", "Единица измерения", secondNameClass, baseClass.named);

        brandSupplier = addConcreteClass("brandSupplier", "Бренд поставщика", baseClass.named);

        themeSupplier = addConcreteClass("themeSupplier", "Тема поставщика", baseClass.named);

        countrySupplier = addConcreteClass("countrySupplier", "Страна поставщика", baseClass.named);

        seasonSupplier = addConcreteClass("seasonSupplier", "Сезон", baseClass.named);

        route = addStaticClass("route", "Маршрут", new String[]{"rb", "rf"}, new String[]{"РБ", "РФ"});

        typeTransit = addStaticClass("typeTransit", "Тип транзита", new String[]{"ex", "t1"}, new String[]{"EX", "T1"});

        stamp = addConcreteClass("stamp", "Контрольная марка", baseClass);

        transitDocument = addConcreteClass("transitDocument", "Транзитный документ", baseClass);
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
        baseLM.tableFactory.include("customCategory10STypeDuty", customCategory10, typeDuty);
        baseLM.tableFactory.include("customCategory10SubCategory", customCategory10, subCategory);
        baseLM.tableFactory.include("customCategory10SubCategoryCountry", customCategory10, subCategory, baseLM.country);

        baseLM.tableFactory.include("colorSupplier", colorSupplier);
        baseLM.tableFactory.include("sizeSupplier", sizeSupplier);
        baseLM.tableFactory.include("country", baseLM.country);
        baseLM.tableFactory.include("article", article);
        baseLM.tableFactory.include("sku", sku);
        baseLM.tableFactory.include("documentArticle", document, article);
        baseLM.tableFactory.include("documentSku", document, sku);
        baseLM.tableFactory.include("shipmentSku", shipment, sku);
        baseLM.tableFactory.include("listSku", list, sku);
        baseLM.tableFactory.include("listArticle", list, article);

        baseLM.tableFactory.include("importerFreightUnitSku", importer, freightUnit, sku);
        baseLM.tableFactory.include("importerFreightSku", importer, freight, sku);

        baseLM.tableFactory.include("articleColorSupplier", article, colorSupplier);
        baseLM.tableFactory.include("articleSizeSupplier", article, sizeSupplier);

        baseLM.tableFactory.include("shipmentRoute", shipment, route);

        baseLM.tableFactory.include("stockSku", stock, sku);
        baseLM.tableFactory.include("stockArticle", stock, article);
        baseLM.tableFactory.include("importerStockSku", importer, stock, sku);
        baseLM.tableFactory.include("importerStockArticle", importer, stock, article);
        baseLM.tableFactory.include("importerFreightUnitArticle", importer, freightUnit, article);
        baseLM.tableFactory.include("importerFreightBrandSupplier", importer, freight, brandSupplier);
        baseLM.tableFactory.include("importerFreightArticle", importer, freight, article);
        baseLM.tableFactory.include("importerFreightCustomCategory6", importer, freight, customCategory6);
        baseLM.tableFactory.include("freightBrandSupplier", freight, brandSupplier);
        baseLM.tableFactory.include("freightArticle", freight, article);
        baseLM.tableFactory.include("freightCategory", freight, category);
        baseLM.tableFactory.include("shipmentFreight", shipment, freight);
        baseLM.tableFactory.include("invoiceShipment", invoice, shipment);
        baseLM.tableFactory.include("shipmentStockSku", shipment, stock, sku);
        baseLM.tableFactory.include("invoiceStockSku", invoice, stock, sku);
        baseLM.tableFactory.include("importerFreightSupplier", importer, freight, supplier);
        baseLM.tableFactory.include("importerFreightTypeInvoice", importer, freight, typeInvoice);
        baseLM.tableFactory.include("importerFreightSupplierCustomCategory6", importer, freight, supplier, customCategory6);

        baseLM.tableFactory.include("supplierBoxBoxShipmentStockSku", supplierBox, boxShipment, stock, sku);
        baseLM.tableFactory.include("boxInvoiceBoxShipmentStockSku", boxInvoice, boxShipment, stock, sku);

        baseLM.tableFactory.include("palletSku", pallet, sku);
        baseLM.tableFactory.include("palletBrandSupplier", pallet, brandSupplier);
        baseLM.tableFactory.include("stockBrandSupplier", stock, brandSupplier);
        baseLM.tableFactory.include("documentBrandSupplier", document, brandSupplier);

        baseLM.tableFactory.include("freightSku", freight, sku);
        baseLM.tableFactory.include("shipmentDetail", shipmentDetail);
        baseLM.tableFactory.include("pallet", pallet);
        baseLM.tableFactory.include("freight", freight);
        baseLM.tableFactory.include("freightUnit", freightUnit);
        baseLM.tableFactory.include("barcodeObject", baseLM.barcodeObject);

        baseLM.tableFactory.include("categoryGenderCompositionTypeFabric", category, gender, COMPOSITION_CLASS, typeFabric);
        baseLM.tableFactory.include("freightCategoryGenderCompositionTypeFabric", freight, category, gender, COMPOSITION_CLASS, typeFabric);

        baseLM.tableFactory.include("sizeSupplierGenderCategory", sizeSupplier, gender, category);

        baseLM.tableFactory.include("rateExchange", typeExchange, currency, DateClass.instance);
        baseLM.tableFactory.include("pricat", pricat);
        baseLM.tableFactory.include("strings", StringClass.get(10));

        baseLM.tableFactory.include("subCategory", subCategory);
        baseLM.tableFactory.include("stamp", stamp);
        baseLM.tableFactory.include("secondNameClass", secondNameClass);

        baseLM.tableFactory.include("importerFreight", importer, freight);

        baseLM.tableFactory.include("simpleInvoiceSimpleShipmentStockSku", simpleInvoice, simpleShipment, stock, sku);
    }

    @Override
    public void initGroups() {
        initBaseGroupAliases();
        Settings.instance.setDisableSumGroupNotZero(true);

        skuAttributeGroup = addAbstractGroup("skuAttributeGroup", "Атрибуты SKU", baseGroup);
        itemAttributeGroup = addAbstractGroup("itemAttributeGroup", "Атрибуты товара", baseGroup);
        supplierAttributeGroup = addAbstractGroup ("supplierAttributeGroup", "Атрибуты поставщика", publicGroup);
        intraAttributeGroup = addAbstractGroup("intraAttributeGroup", "Внутренние атрибуты", publicGroup);
        importInvoiceActionGroup = addAbstractGroup("importInvoiceActionGroup","Импорт инвойсов", actionGroup, false);
    }

    @Override
    public void initProperties() {
        idGroup.add(baseLM.objectValue);
        baseLM.delete.property.askConfirm = true;

        round2 = addSFProp("round2", "round(CAST((prm1) as numeric), 2)", DoubleClass.instance, 1);

        typeSupplier = is(simpleSupplier);
        //typeSupplier = addCUProp("typeSupplier", addCProp(LogicalClass.instance, true, hugoBossSupplier), addCProp(LogicalClass.instance, true, gerryWeberSupplier), addCProp(LogicalClass.instance, true, sOliverSupplier), addCProp(LogicalClass.instance, true, babyPhatSupplier));
        noBarcodeSupplier = addCUProp("noBarcodeSupplier", addCProp(LogicalClass.instance, true, babyPhatSupplier));

        nameClassFreight = addJProp(baseGroup, "nameClassFreight", "Класс фрахта", baseLM.and1, baseLM.objectClassName, 1, is(freight), 1);
        logFreight = addLProp(nameClassFreight);

        symbolCurrency = addDProp(baseGroup, "symbolCurrency", "Символ", StringClass.get(5), currency);

        // rate
        currencyTypeExchange = addDProp(idGroup, "currencyTypeExchange", "Валюта типа обмена (ИД)", currency, typeExchange);
        nameCurrencyTypeExchange = addJProp(baseGroup, "nameCurrencyTypeExchange", "Валюта типа обмена (наим.)", baseLM.name, currencyTypeExchange, 1);
        rateExchange = addDProp(baseGroup, "rateExchange", "Курс обмена", DoubleClass.instance, typeExchange, currency, DateClass.instance);
        typeExchangeSTX = addDProp(idGroup, "typeExchangeSTX", "Тип обмена валют для STX (ИД)", typeExchange);
        nameTypeExchangeSTX = addJProp(baseGroup, "nameTypeExchangeSTX", "Тип обмена валют для STX", baseLM.name, typeExchangeSTX);
        typeExchangeCustom = addDProp(idGroup, "typeExchangeCustom", "Тип обмена валют для мин.цен (ИД)", typeExchange);
        nameTypeExchangeCustom = addJProp(baseGroup, "nameTypeExchangeCustom", "Тип обмена валют для мин.цен", baseLM.name, typeExchangeCustom);
        typeExchangePayCustom = addDProp(idGroup, "typeExchangePayCustom", "Тип обмена валют для платежей (ИД)", typeExchange);
        nameTypeExchangePayCustom = addJProp(baseGroup, "nameTypeExchangePayCustom", "Тип обмена валют для платежей", baseLM.name, typeExchangePayCustom);
        currencyCustom = addDProp(idGroup, "currencyCustom", "Валюта мин.цен (ИД)", currency);
        nameCurrencyCustom = addJProp(baseGroup, "nameCurrencyCustom", "Валюта мин.цен", baseLM.name, currencyCustom);
        currencyPayCustom = addDProp(idGroup, "currencyPayCustom", "Валюта для платежей (ИД)", currency);
        nameCurrencyPayCustom = addJProp(baseGroup, "nameCurrencyPayCustom", "Валюта для платежей", baseLM.name, currencyPayCustom);
        NDSPercentCustom = addDProp(baseGroup, "NDSPercentCustom", "НДС", DoubleClass.instance);

        //lessCmpDate = addJProp(and(false, true, false), object(DateClass.instance), 3, rateExchange, 1, 2, 3, greater2, 3, 4, is(DateClass.instance), 4);
        lessCmpDate = addJProp(and(false, true, false), object(DateClass.instance), 3, rateExchange, 1, 2, 3, addJProp(baseLM.greater2, 3, baseLM.date, 4), 1, 2, 3, 4, is(baseLM.transaction), 4);
        nearestPredDate = addMGProp((AbstractGroup) null, "nearestPredDate", "Ближайшая меньшая дата", lessCmpDate, 1, 2, 4);
        nearestRateExchange = addJProp("nearestRateExchange", "Ближайший курс обмена", rateExchange, 1, 2, nearestPredDate, 1, 2, 3);

        // GENERAL
        nameOrigin = addDProp(baseGroup, "nameOrigin", "Наименование (ориг.)", InsensitiveStringClass.get(50), secondNameClass);
        nameOriginCountry = addDProp(baseGroup, "nameOriginCountry", "Наименование (ориг.)", InsensitiveStringClass.get(50), baseLM.country);

        sidOrigin2Country = addDProp(baseGroup, "sidOrigin2Country", "Код 2 знака (ориг.)", StringClass.get(2), baseLM.country);
        sidOrigin3Country = addDProp(baseGroup, "sidOrigin3Country", "Код 3 знака (ориг.)", StringClass.get(3), baseLM.country);
        sid3Country = addDProp(baseGroup, "sid3Country", "Код 3 знака", StringClass.get(3), baseLM.country);

        sidOrigin2ToCountry = addAGProp("sidOrigin2ToCountry", "Страна", sidOrigin2Country);

        dictionaryComposition = addDProp(idGroup, "dictionaryComposition", "Словарь для составов (ИД)", baseLM.dictionary);
        nameDictionaryComposition = addJProp(baseGroup, "nameDictionaryComposition", "Словарь для составов", baseLM.name, dictionaryComposition);

        sidImporterFreightTypeInvoice = addDProp(baseGroup, "sidImporterFreightTypeInvoice", "Номер инвойса", StringClass.get(50), importer, freight, typeInvoice);
        sidImporterFreight = addMGProp(baseGroup, "sidImporterFreight", "Номер инвойса", sidImporterFreightTypeInvoice, 1, 2);

        sidDestination = addDProp(baseGroup, "sidDestination", "Код", StringClass.get(50), destination);

        destinationSID = addAGProp(idGroup, "destinationSID", "Магазин (ИД)", sidDestination);

        relationStoreSupplier = addDProp(baseGroup, "relationStoreSupplier", "Связь магазинов и поставщиков", LogicalClass.instance, store, supplier);

        sidBrandSupplier = addDProp(baseGroup, "sidBrandSupplier", "Код", StringClass.get(50), brandSupplier);  //???

        sidTransitDocument = addDProp(baseGroup, "sidTransitDocument", "Номер документа", StringClass.get(50), transitDocument);
        dateRepaymentTransitDocument = addDProp(baseGroup, "dateRepaymentTransitDocument", "Срок погашения", DateClass.instance, transitDocument);
        dateClosingTransitDocument = addDProp(baseGroup, "dateClosingTransitDocument", "Дата закрытия", DateClass.instance, transitDocument);

        sellerTransitDocument = addDProp(idGroup, "sellerTransitDocument", "Отправитель документа (ИД)", seller, transitDocument);
        nameSellerTransitDocument = addJProp(baseGroup, "nameSellerTransitDocument", "Отправитель документа", baseLM.name, sellerTransitDocument, 1);
        nameSellerTransitDocument.property.preferredCharWidth = 15;

        importerTransitDocument = addDProp(baseGroup, "importerTransitDocument", "Получатель документа", importer, transitDocument);

        freightTransitDocument =  addDProp(baseGroup, "freightTransitDocument", "Фрахт документа", freight, transitDocument);
        typeTransitTransitDocument = addDProp(idGroup, "typeTransitTransitDocument", "Тип транзита (ИД)", typeTransit, transitDocument);
        nameTypeTransitTransitDocument = addJProp(baseGroup, "nameTypeTransitTransitDocument", "Тип транзита", baseLM.name, typeTransitTransitDocument, 1);

        // Duty
        sidTypeDuty = addDProp(baseGroup, "sidTypeDuty", "Код", StringClass.get(10), typeDuty);
        sidToTypeDuty = addAGProp("sidToTypeDuty", "Тип пошлины", sidTypeDuty);

        nameTypeDuty = addDProp(baseGroup, "nameTypeDuty", "Наименование", StringClass.get(50), typeDuty);

        typeDutyDuty = addDProp(idGroup, "typeDutyDuty", "Для пошлин (ИД)", typeDuty);
        sidTypeDutyDuty = addJProp(baseGroup, "sidTypeDutyDuty", "Для пошлин (код)", sidTypeDuty, typeDutyDuty);
        nameTypeDutyDuty = addJProp(baseGroup, "nameTypeDutyDuty", "Для пошлин", nameTypeDuty, typeDutyDuty);

        typeDutyNDS = addDProp(idGroup, "typeDutyNDS", "Для НДС (ИД)", typeDuty);
        sidTypeDutyNDS = addJProp(baseGroup, "sidTypeDutyNDS", "Для НДС (код)", sidTypeDuty, typeDutyNDS);
        nameTypeDutyNDS = addJProp(baseGroup, "nameTypeDutyNDS", "Для НДС", nameTypeDuty, typeDutyNDS);

        typeDutyRegistration = addDProp(idGroup, "typeDutyRegistration", "Для оформления (ИД)", typeDuty);
        sidTypeDutyRegistration = addJProp(baseGroup, "sidTypeDutyRegistration", "Для оформления (код)", sidTypeDuty, typeDutyRegistration);
        nameTypeDutyRegistration = addJProp(baseGroup, "nameTypeDutyRegistration", "Для оформления", nameTypeDuty, typeDutyRegistration);

        // Contract
        sidContract = addDProp(baseGroup, "sidContract", "Номер договора", StringClass.get(50), contract);
        dateContract = addDProp(baseGroup, "dateContract", "Дата договора", DateClass.instance, contract);
        conditionShipmentContract = addDProp(baseGroup, "conditionShipmentContract", "Условие поставки", StringClass.get(200), contract);
        conditionPaymentContract = addDProp(baseGroup, "conditionPaymentContract", "Условие оплаты", StringClass.get(200), contract);

        //buyerContract = addDProp(idGroup, "buyerContract", "Покупатель (ИД)", buyer, contract);
        //nameBuyerContract = addJProp(baseGroup, "nameBuyerContract", "Покупатель", baseLM.name, buyerContract, 1);

        subjectContract = addDProp(idGroup, "subjectContract", "Покупатель (ИД)", subject, contract);
        nameSubjectContract = addJProp(baseGroup, "nameSubjectContract", "Покупатель", baseLM.name, subjectContract, 1);

        sellerContract = addDProp(idGroup, "sellerContract", "Продавец (ИД)", seller, contract);
        nameSellerContract = addJProp(baseGroup, "nameSellerContract", "Продавец", baseLM.name, sellerContract, 1);

        currencyContract = addDProp(idGroup, "currencyContract", "Валюта (ИД)", currency, contract);
        nameCurrencyContract = addJProp(baseGroup, "nameCurrencyContract", "Валюта", baseLM.name, currencyContract, 1);

        // Subject
        addressOriginSubject = addDProp(baseGroup, "addressOriginSubject", "Address", StringClass.get(200), subject);
        addressSubject = addDProp(baseGroup, "addressSubject", "Адрес", StringClass.get(200), subject);

        contractImporter = addDProp(baseGroup, "contractImporter", "Номер договора", StringClass.get(50), importer);
        sidImporter = addDProp(baseGroup, "sidImporter", "Номер клиента", StringClass.get(50), importer);

        // CustomCategory
        sidCustomCategory4 = addDProp(baseGroup, "sidCustomCategory4", "Код(4)", StringClass.get(4), customCategory4);
        sidCustomCategory4.setFixedCharWidth(4);

        sidCustomCategory6 = addDProp(baseGroup, "sidCustomCategory6", "Код(6)", StringClass.get(6), customCategory6);
        sidCustomCategory6.setFixedCharWidth(6);

        sidCustomCategory9 = addDProp(baseGroup, "sidCustomCategory9", "Код(9)", StringClass.get(9), customCategory9);
        sidCustomCategory9.setFixedCharWidth(9);

        numberIdCustomCategory10 = addDProp(baseGroup, "numberIdCustomCategory10", "Номер", IntegerClass.instance, customCategory10);
        numberIdCustomCategoryOrigin = addDProp(baseGroup, "numberIdCustomCategoryOrigin", "Номер", IntegerClass.instance, customCategoryOrigin);
                
        sidCustomCategory10 = addDProp(baseGroup, "sidCustomCategory10", "Код(10)", StringClass.get(10), customCategory10);
        sidCustomCategory10.setFixedCharWidth(10);

        certificatedCustomCategory10 = addDProp(baseGroup, "certificatedCustomCategory10", "Необходимость сертификации", LogicalClass.instance, customCategory10);

        sidCustomCategoryOrigin = addDProp(baseGroup, "sidCustomCategoryOrigin", "Код ЕС(10)", StringClass.get(10), customCategoryOrigin);
        sidCustomCategoryOrigin.setFixedCharWidth(10);

        nameCustomCategory = addDProp(baseGroup, "nameCustomCategory", "Наименование", StringClass.get(500), customCategory);
        nameCustomCategory.property.preferredCharWidth = 50;
        nameCustomCategory.property.minimumCharWidth = 20;

        sidToCustomCategory4 = addAGProp("sidToCustomCategory4", "Код(4)", sidCustomCategory4);
        sidToCustomCategory6 = addAGProp("sidToCustomCategory6", "Код(6)", sidCustomCategory6);
        sidToCustomCategory9 = addAGProp("sidToCustomCategory9", "Код(9)", sidCustomCategory9);
        sidToCustomCategory10 = addAGProp("sidToCustomCategory10", "Код(10)", sidCustomCategory10);
        sidToCustomCategoryOrigin = addAGProp("sidToCustomCategoryOrigin", "Код ЕС (10)", sidCustomCategoryOrigin);

        importBelTnved = addAProp(new TNVEDImportActionProperty(genSID(), "Импортировать (РБ)", this, TNVEDImportActionProperty.CLASSIFIER_IMPORT, "belarusian"));
        importEuTnved = addAProp(new TNVEDImportActionProperty(genSID(), "Импортировать (ЕС)", this, TNVEDImportActionProperty.CLASSIFIER_IMPORT, "origin"));
        importTnvedCountryMinPrices = addAProp(new TNVEDImportActionProperty(genSID(), "Импортировать мин. цены", this, TNVEDImportActionProperty.MIN_PRICES_IMPORT));
        importTnvedDuty = addAProp(new TNVEDImportActionProperty(genSID(), "Импортировать платежи", this, TNVEDImportActionProperty.DUTIES_IMPORT));
        dieselImportInvoice = addAProp(importInvoiceActionGroup, new DieselImportInvoiceActionProperty(this));
        jennyferImportInvoice = addAProp(importInvoiceActionGroup, new JennyferImportInvoiceActionProperty(this));
        steilmannImportInvoice = addAProp(importInvoiceActionGroup, new SteilmannImportInvoiceActionProperty(BL));
        tallyWeijlImportInvoice = addAProp(importInvoiceActionGroup, new TallyWeijlImportInvoiceActionProperty(this));
        hugoBossImportInvoice = addAProp(importInvoiceActionGroup, new HugoBossImportInvoiceActionProperty(BL));
        gerryWeberImportInvoice = addAProp(importInvoiceActionGroup, new GerryWeberImportInvoiceActionProperty(BL));
        mexxImportInvoice = addAProp(new MexxImportInvoiceActionProperty(this));
        mexxImportPricesInvoice = addAProp(new MexxImportPricesInvoiceActionProperty(this));
        mexxImportArticleInfoInvoice = addAProp(new MexxImportArticleInfoInvoiceActionProperty(this));
        mexxImportColorInvoice = addAProp(new MexxImportColorInvoiceActionProperty(this));
        mexxImportDelivery = addAProp(importInvoiceActionGroup, new MexxImportDeliveryActionProperty(this));
        bestsellerImportInvoice = addAProp(importInvoiceActionGroup, new BestsellerImportInvoiceActionProperty(BL));
        sOliverImportInvoice = addAProp(importInvoiceActionGroup, new SOliverImportInvoiceActionProperty(BL));
        womenSecretImportInvoice = addAProp(importInvoiceActionGroup, new WomenSecretImportInvoiceActionProperty(this));
        topazImportInvoice = addAProp(importInvoiceActionGroup, new TopazImportInvoiceActionProperty(BL));
        aprioriImportInvoice = addAProp(importInvoiceActionGroup, new AprioriImportInvoiceActionProperty(this));

        customCategory4CustomCategory6 = addDProp(idGroup, "customCategory4CustomCategory6", "Код(4)", customCategory4, customCategory6);
        customCategory6CustomCategory9 = addDProp(idGroup, "customCategory6CustomCategory9", "Код(6)", customCategory6, customCategory9);
        customCategory9CustomCategory10 = addDProp(idGroup, "customCategory9CustomCategory10", "Код(9)", customCategory9, customCategory10);
        customCategory6CustomCategory10 = addJProp(idGroup, "customCategory6CustomCategory10", "Код(6)", customCategory6CustomCategory9, customCategory9CustomCategory10, 1);
        customCategory4CustomCategory10 = addJProp(idGroup, "customCategory4CustomCategory10", "Код(4)", customCategory4CustomCategory6, customCategory6CustomCategory10, 1);

        nameSubCategory = addDProp(baseGroup, "nameSubCategory", "Наименование", StringClass.get(200), subCategory);
        nameSubCategory.property.preferredCharWidth = 30;
        nameSubCategory.property.minimumCharWidth = 10;
        nameToSubCategory = addAGProp("nameToSubCategory", "Наименование", nameSubCategory);

        relationCustomCategory10SubCategory = addDProp(baseGroup, "relationCustomCategory10SubCategory", "Связь ТН ВЭД", LogicalClass.instance, customCategory10, subCategory);

        subCategoryCustomCategory10 = addMGProp(baseGroup, "subCategoryCustomCategory10", "По умолчанию", addJProp(baseLM.and1, 2, relationCustomCategory10SubCategory, 1, 2), 1);

        countRelationCustomCategory10 = addSGProp("countRelationCustomCategory10", true, "Кол-во групп", addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), relationCustomCategory10SubCategory, 1, 2), 1);

        minPriceCustomCategory10SubCategory = addDProp(baseGroup, "minPriceCustomCategory10SubCategory", "Минимальная цена ($)", DoubleClass.instance, customCategory10, subCategory);
        minPriceCustomCategory10SubCategoryCountry = addDProp("minPriceCustomCategory10SubCategoryCountry", "Минимальная цена для страны ($)", DoubleClass.instance, customCategory10, subCategory, baseLM.country);
        dutyPercentCustomCategory10TypeDuty = addDProp("dutyPercentCustomCategory10TypeDuty", "в %", DoubleClass.instance, customCategory10, typeDuty);
        dutySumCustomCategory10TypeDuty = addDProp("dutySumCustomCategory10TypeDuty", "в евро", DoubleClass.instance, customCategory10, typeDuty);

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
        nameCustomCategory9CustomCategory10 = addJProp("nameCustomCategory9CustomCategory10", "Наименование(9)", nameCustomCategory, customCategory9CustomCategory10, 1);
        nameCustomCategory6CustomCategory10 = addJProp("nameCustomCategory6CustomCategory10", "Наименование(6)", nameCustomCategory, customCategory6CustomCategory10, 1);
        nameCustomCategory4CustomCategory10 = addJProp(baseGroup, "nameCustomCategory4CustomCategory10", "Наименование(4)", nameCustomCategory, customCategory4CustomCategory10, 1);

        nameCustomCategory6CustomCategoryOrigin = addJProp(baseGroup, "nameCustomCategory6CustomCategoryOrigin", "Наименование(6)", nameCustomCategory, customCategory6CustomCategoryOrigin, 1);
        nameCustomCategory4CustomCategoryOrigin = addJProp(baseGroup, "nameCustomCategory4CustomCategoryOrigin", "Наименование(4)", nameCustomCategory, customCategory4CustomCategoryOrigin, 1);

        relationCustomCategory10CustomCategoryOrigin = addDProp(baseGroup, "relationCustomCategory10CustomCategoryOrigin", "Связь ТН ВЭД", LogicalClass.instance, customCategory10, customCategoryOrigin);

//         addConstraint(addJProp("По умолчанию должен быть среди связанных", and(true, false),
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
        sidThemeSupplier = addDProp(baseGroup, "sidThemeSupplier", "Код", StringClass.get(50), themeSupplier);
        sidSeasonSupplier = addDProp(itemAttributeGroup, "sidSeasonSupplier", "Код", StringClass.get(50), seasonSupplier);
        sidGender = addDProp(baseGroup, "sidGender", "Код", StringClass.get(50), gender);
        sidGender.setPreferredCharWidth(2);
        sidGenderSupplier = addDProp(baseGroup, "sidGenderSupplier", "Код", StringClass.get(10), genderSupplier);

        commonSizeSizeSupplierGenderCategory = addDProp(idGroup, "commonSizeSizeSupplierGenderCategory", "Унифицированный размер (ИД)", commonSize, sizeSupplier, gender, category);
        nameCommonSizeSizeSupplierGenderCategory = addJProp(baseGroup, "nameCommonSizeSizeSupplierGenderCategory", "Унифицированный размер", baseLM.name, commonSizeSizeSupplierGenderCategory, 1, 2, 3);

        commonSizeSizeSupplier = addDProp(idGroup, "commonSizeSizeSupplier", "Унифицированный размер (ИД)", commonSize, sizeSupplier);
        nameCommonSizeSizeSupplier = addJProp(baseGroup, "nameCommonSizeSizeSupplier", "Унифицированный размер", baseLM.name, commonSizeSizeSupplier, 1);

        supplierSizeSupplier = addDProp(idGroup, "supplierSizeSupplier", "Поставщик (ИД)", supplier, sizeSupplier);
        nameSupplierSizeSupplier = addJProp(baseGroup, "nameSupplierSizeSupplier", "Поставщик", baseLM.name, supplierSizeSupplier, 1);

        supplierGenderSupplier = addDProp(idGroup, "supplierGenderSupplier", "Поставщик (ИД)", supplier, genderSupplier);
        nameSupplierGenderSupplier = addJProp(baseGroup, "nameSupplierGenderSupplier", "Поставщик", baseLM.name, supplierGenderSupplier, 1);

        supplierSizeGroup = addDProp(idGroup, "supplierSizeGroup", "Поставщик (ИД)", supplier, sizeGroupSupplier);
        nameSupplierSizeGroup = addJProp(baseGroup, "nameSupplierSizeGroup", "Поставщик", baseLM.name, supplierSizeGroup, 1);
        groupSizeSupplier = addDProp(idGroup, "groupSizeSupplier", "Размерная сетка (ИД)", sizeGroupSupplier, sizeSupplier);
        nameGroupSizeSupplier = addJProp(baseGroup, "nameGroupSizeSupplier", "Размерная сетка", baseLM.name, groupSizeSupplier, 1);
        orderSizeSupplier = addDProp(baseGroup, "orderSizeSupplier", "Порядок", IntegerClass.instance, sizeSupplier);

        equalsGroupSizeSupplier = addJProp("equalsGroupSizeSupplier", "Вкл", baseLM.equals2, groupSizeSupplier, 1, 2);

        LP supplierGroupSizeSupplier = addJProp(supplierSizeGroup, groupSizeSupplier, 1);
        addConstraint(addJProp("Поставщик размерной сетки должен соответствовать поставщику размера", baseLM.diff2,
                supplierSizeSupplier, 1,
                supplierGroupSizeSupplier, 1), true);

        supplierThemeSupplier = addDProp(idGroup, "supplierThemeSupplier", "Поставщик (ИД)", supplier, themeSupplier);
        supplierSeasonSupplier = addDProp(idGroup, "supplierSeasonSupplier", "Поставщик (ИД)", supplier, seasonSupplier);
        nameSupplierSeasonSupplier = addJProp(baseGroup, "nameSupplierSeasonSupplier", "Поставщик", baseLM.name, supplierSeasonSupplier, 1);
        nameSupplierThemeSupplier = addJProp(baseGroup, "nameSupplierThemeSupplier", "Поставщик", baseLM.name, supplierThemeSupplier, 1);
        sidDestinationSupplier = addDProp(idGroup, "sidDestinationSupplier", "Идентификатор магазина у поставщика", InsensitiveStringClass.get(50),  destination, supplier);

        sizeSIDSupplier = addAGProp(idGroup, "sizeSIDSupplier", "Размер поставщика (ИД)", sidSizeSupplier, supplierSizeSupplier);
        themeSIDSupplier = addAGProp(idGroup, "themeSIDSupplier", "Тема поставщика (ИД)", sidThemeSupplier, supplierThemeSupplier);
        seasonSIDSupplier = addAGProp(idGroup, "seasonSIDSupplier", "Сезон поставщика (ИД)", sidSeasonSupplier, supplierSeasonSupplier);
        genderSIDSupplier = addAGProp(idGroup, "genderSIDSupplier", "Пол поставщика (ИД)", sidGenderSupplier, supplierGenderSupplier);
        destinationSIDSupplier = addAGProp(idGroup, "destinationSIDSupplier", "Магазин поставщика (ИД)",
                addJProp(baseLM.and1, is(destination), 1, is(supplier), 2), 1, sidDestinationSupplier, 1, 2);

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

        countryBrandSupplier = addDProp(idGroup, "countryBrandSupplier", "Страна бренда (ИД)", baseLM.country, brandSupplier);
        nameCountryBrandSupplier = addJProp(baseGroup, "nameCountryBrandSupplier", "Страна бренда", baseLM.name, countryBrandSupplier, 1);

        // Document
        supplierDocument = addDProp(idGroup, "supplierDocument", "Поставщик (ИД)", supplier, document);
        supplierPriceDocument = addJProp(idGroup, "supplierPricedDocument", "Поставщик(ИД)", baseLM.and1, supplierDocument, 1, is(priceDocument), 1);
        nameSupplierDocument = addJProp(baseGroup, "nameSupplierDocument", "Поставщик", baseLM.name, supplierDocument, 1);

        addConstraint(addJProp("Для инвойса по коробам поставщик должен быть с коробами", baseLM.and1, is(boxInvoice), 1, addJProp(typeSupplier, supplierDocument, 1), 1), true);
        addConstraint(addJProp("Для инвойса без коробов поставщик должен быть без коробов", baseLM.andNot1, is(simpleInvoice), 1, addJProp(typeSupplier, supplierDocument, 1), 1), true);

        currencyDocument = addDCProp(idGroup, "currencyDocument", "Валюта (ИД)", currencySupplier, supplierPriceDocument, 1);
        nameCurrencyDocument = addJProp(baseGroup, "nameCurrencyDocument", "Валюта", baseLM.name, currencyDocument, 1);
        nameCurrencyDocument.property.preferredCharWidth = 50;
        nameCurrencyDocument.property.minimumCharWidth = 10;

        addConstraint(addJProp("Для инвойса должна быть задана валюта", baseLM.andNot1, is(invoice), 1, currencyDocument, 1), false);

        destinationDestinationDocument = addDProp(idGroup, "destinationDestinationDocument", "Пункт назначения (ИД)", destination, destinationDocument);
        nameDestinationDestinationDocument = addJProp(baseGroup, "nameDestinationDestinationDocument", "Пункт назначения (наим.)", baseLM.name, destinationDestinationDocument, 1);
        nameDestinationDestinationDocument.property.preferredCharWidth = 50;
        nameDestinationDestinationDocument.property.minimumCharWidth = 30;
        sidDestinationDestinationDocument = addJProp(baseGroup, "sidDestinationDestinationDocument", "Пункт назначения", sidDestination, destinationDestinationDocument, 1);
        setNotNull(destinationDestinationDocument);

        addConstraint(addJProp("Магазин для документа должен быть связан с поставщиком документа", and(false, false, true), addCProp(LogicalClass.instance, true, destinationDocument), 1,
                   destinationDestinationDocument, 1,
                   supplierDocument, 1,
                   addJProp(relationStoreSupplier, destinationDestinationDocument, 1, supplierDocument, 1), 1), true);

        // Invoice
        exporterInvoice = addDProp(idGroup, "exporterInvoice", "Экспортер (ИД)", exporter, invoice);
        nameExporterInvoice = addJProp(baseGroup, "nameExporterInvoice", "Экспортер", baseLM.name, exporterInvoice, 1);
        nameExporterInvoice.property.preferredCharWidth = 50;

        exporterProxyInvoice = addJProp(idGroup, "exporterProxyInvoice", "Экспортер (ИД)", baseLM.andNot1, exporterInvoice, 1, is(directInvoice), 1);

        addConstraint(addJProp("Для инвойса должен быть задан экспортёр", and(true, true), is(invoice), 1, exporterInvoice, 1, is(directInvoice), 1), false);

        importerDirectInvoice = addDProp(idGroup, "importerDirectInvoice", "Импортер (ИД)", importer, directInvoice);
        nameImporterDirectInvoice = addJProp(baseGroup, "nameImporterDirectInvoice", "Импортер", baseLM.name, importerDirectInvoice, 1);
        nameImporterDirectInvoice.property.preferredCharWidth = 50;
        nameImporterDirectInvoice.property.minimumCharWidth = 30;

        addConstraint(addJProp("Для инвойса должен быть задан импортёр", baseLM.andNot1, is(directInvoice), 1, importerDirectInvoice, 1), false);

        contractInvoice = addDProp(idGroup, "contractInvoice", "Договор (ИД)", contract, invoice);
        sidContractInvoice = addJProp(baseGroup, "sidContractInvoice", "Договор", sidContract, contractInvoice, 1);

        addConstraint(addJProp("Экспортер договора должен соответствовать экспортеру инвойса", baseLM.diff2,
                exporterProxyInvoice, 1, addJProp(subjectContract, contractInvoice, 1), 1), true);

        addConstraint(addJProp("Импортер договора должен соответствовать импортеру инвойса", baseLM.diff2,
                importerDirectInvoice, 1, addJProp(subjectContract, contractInvoice, 1), 1), true);

        addConstraint(addJProp("Поставщик договора должен соответствовать поставщику инвойса", baseLM.diff2,
                supplierDocument, 1, addJProp(sellerContract, contractInvoice, 1), 1), true);

        // Shipment
        dateDepartureShipment = addDProp(baseGroup, "dateDepartureShipment", "Дата отгрузки", DateClass.instance, shipment);
        dateArrivalShipment = addDProp(baseGroup, "dateArrivalShipment", "Дата прихода на STX", DateClass.instance, shipment);
        quantityPalletShipment = addDProp(baseGroup, "quantityPalletShipment", "Кол-во паллет", IntegerClass.instance, shipment);
        netWeightShipment = addDProp(baseGroup, "netWeightShipment", "Вес нетто", DoubleClass.instance, shipment);
        grossWeightShipment = addDProp(baseGroup, "grossWeightShipment", "Вес брутто", DoubleClass.instance, shipment);

        grossWeightPallet = addDProp(baseGroup, "grossWeightPallet", "Вес брутто", DoubleClass.instance, pallet);
        quantityBoxShipment = addDProp(baseGroup, "quantityBoxShipment", "Кол-во коробов", DoubleClass.instance, shipment);

        // Item
        articleCompositeItem = addDProp(idGroup, "articleCompositeItem", "Артикул (ИД)", articleComposite, item);
        equalsItemArticleComposite = addJProp(baseGroup, "equalsItemArticleComposite", "Вкл.", baseLM.equals2, articleCompositeItem, 1, 2);

        articleSku = addCUProp(idGroup, "articleSku", true, "Артикул (ИД)", object(articleSingle), articleCompositeItem);
//        setNotNull(articleSku);
//        addConstraint(addJProp("Для товара должен быть задан артикул", baseLM.andNot1, is(sku), 1, articleSku, 1), false);

        addItemBarcode = addJProp(true, "Ввод товара по штрих-коду", addAAProp(item, baseLM.barcode), 1);

        // Article
        sidArticle = addDProp(baseGroup, "sidArticle", "Артикул", StringClass.get(50), article);
        sidArticle.setMinimumCharWidth(15);
        sidArticleSku = addJProp(supplierAttributeGroup, "sidArticleSku", "Артикул", sidArticle, articleSku, 1);

        originalNameArticle = addDProp(supplierAttributeGroup, "originalNameArticle", "Наименование (ориг.)", InsensitiveStringClass.get(50), article);
        originalNameArticleSku = addJProp(supplierAttributeGroup, "originalNameArticleSku", "Наименование (ориг.)", originalNameArticle, articleSku, 1);

        coefficientArticle = addDProp(intraAttributeGroup, "coefficientArticle", "Кол-во в комплекте", IntegerClass.instance, article);
        coefficientArticleSku = addJProp(intraAttributeGroup, true, "coefficientArticleSku", "Кол-во в комплекте", coefficientArticle, articleSku, 1);

        //Category
        typeInvoiceCategory = addDProp(idGroup, "typeInvoiceCategory", "Тип инвойса номенклатурной группы (ИД)", typeInvoice, category);
        nameTypeInvoiceCategory = addJProp(baseGroup, "nameTypeInvoiceCategory", "Тип инвойса номенклатурной группы", baseLM.name, typeInvoiceCategory, 1);
        setNotNull(typeInvoiceCategory);
        
        categoryArticle = addDProp(idGroup, "categoryArticle", "Номенклатурная группа товара (ИД)", category, article);
        nameOriginCategoryArticle = addJProp(intraAttributeGroup, "nameOriginCategoryArticle", "Номенклатурная группа товара (ориг.)", nameOrigin, categoryArticle, 1);
        nameCategoryArticle = addJProp(intraAttributeGroup, "nameCategoryArticle", "Номенклатурная группа товара", baseLM.name, categoryArticle, 1);
        categoryArticleSku = addJProp(idGroup, true, "categoryArticleSku", true, "Номенклатурная группа товара (ИД)", categoryArticle, articleSku, 1);
        nameCategoryArticleSku = addJProp(intraAttributeGroup, "nameCategoryArticleSku", "Номенклатурная группа товара", baseLM.name, categoryArticleSku, 1);
        nameCategoryArticleSku.property.preferredCharWidth = 50;
        nameCategoryArticleSku.property.minimumCharWidth = 15;
        nameOriginCategoryArticleSku = addJProp(intraAttributeGroup, "nameOriginCategoryArticleSku", "Номенклатурная группа товара", nameOrigin, categoryArticleSku, 1);

        typeInvoiceCategoryArticle = addJProp(idGroup, "typeInvoiceCategoryArticle", "Тип инвойса артикула (ИД)", typeInvoiceCategory, categoryArticle, 1);
        typeInvoiceCategoryArticleSku = addJProp(idGroup, "typeInvoiceCategoryArticleSku", "Тип инвойса товара (ИД)", typeInvoiceCategory, categoryArticleSku, 1);

        typeInvoiceDataFreightArticle = addDProp(idGroup, "typeInvoiceDataFreightArticle", "Тип инвойса (ИД)", typeInvoice, freight, article);
        typeInvoiceCategoryFreightArticle = addJProp(idGroup, "typeInvoiceCategoryFreightArticle", "Тип инвойса (ИД)", baseLM.and1, typeInvoiceCategoryArticle, 2, is(freight), 1);
        typeInvoiceFreightArticle = addSUProp(idGroup, "typeInvoiceFreightArticle", "Тип инвойса (ИД)", Union.OVERRIDE, typeInvoiceCategoryFreightArticle, typeInvoiceDataFreightArticle);
        typeInvoiceFreightSku = addJProp(idGroup, true, "typeInvoiceFreightSku", "Тип инвойса (ИД)", typeInvoiceFreightArticle, 1, articleSku, 2);
        nameTypeInvoiceFreightArticleSku = addJProp(baseGroup, "nameTypeInvoiceFreightArticleSku", "Тип инвойса", baseLM.name, typeInvoiceFreightSku, 1, 2);
        nameTypeInvoiceFreightArticleSku.property.preferredCharWidth = 50;
        nameTypeInvoiceFreightArticleSku.property.minimumCharWidth = 15;

        nameArticle = addSUProp(baseGroup, "nameArticle", "Наименование", Union.OVERRIDE, originalNameArticle, nameOriginCategoryArticle);
        nameArticleSku = addJProp(intraAttributeGroup, "nameArticleSku", "Наименование", nameArticle, articleSku, 1);

        colorSupplierItem = addDProp(idGroup, "colorSupplierItem", "Цвет поставщика (ИД)", colorSupplier, item);
        sidColorSupplierItem = addJProp(itemAttributeGroup, "sidColorSupplierItem", "Код цвета", sidColorSupplier, colorSupplierItem, 1);
        nameColorSupplierItem = addJProp(itemAttributeGroup, "nameColorSupplierItem", "Цвет поставщика", baseLM.name, colorSupplierItem, 1);

        inListArticleColorSupplier = addDProp(baseGroup, "inListArticleColorSupplier", "Наличие", LogicalClass.instance, list, article, colorSupplier);

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
        setNotNull(supplierArticle);
        nameSupplierArticle = addJProp(baseGroup, "nameSupplierArticle", "Поставщик", baseLM.name, supplierArticle, 1);

        jennyferSupplierArticle = addJProp("jennyferSupplierArticle", "Поставщик Jennyfer (ИД)", baseLM.and1, supplierArticle, 1, addJProp(is(jennyferSupplier), supplierArticle, 1), 1);
        steilmannSupplierArticle = addJProp("steilmannSupplierArticle", "Поставщик Steilmann (ИД)", baseLM.and1, supplierArticle, 1, addJProp(is(steilmannSupplier), supplierArticle, 1), 1);

        sizeGroupSupplierArticle = addDProp(idGroup, "sizeGroupSupplierArticle", "Размерная сетка (ИД)", sizeGroupSupplier, article);
        nameSizeGroupSupplierArticle = addJProp(baseGroup, "nameSizeGroupSupplierArticle", "Размерная сетка", baseLM.name, sizeGroupSupplierArticle, 1);
        nameSizeGroupSupplierArticle.setMinimumCharWidth(6); nameSizeGroupSupplierArticle.setPreferredCharWidth(10);
        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику размерной сетки", baseLM.diff2,
                supplierArticle, 1,
                addJProp(supplierSizeGroup, sizeGroupSupplierArticle, 1), 1), true);

        brandSupplierDataArticle = addDProp(idGroup, "brandSupplierDataArticle", "Бренд (ИД)", brandSupplier, article);
        brandSupplierSupplierArticle = addJProp(idGroup, "brandSupplierSupplierArticle", "Бренд (ИД)", brandSupplierSupplier, supplierArticle, 1);
        brandSupplierArticle = addSUProp(idGroup, "brandSupplierArticle", "Бренд (ИД)", Union.OVERRIDE, brandSupplierSupplierArticle, brandSupplierDataArticle);
        nameBrandSupplierArticle = addJProp(baseGroup, "nameBrandSupplierArticle", "Бренд", baseLM.name, brandSupplierArticle, 1);
        nameBrandSupplierArticle.property.preferredCharWidth = 30;
        nameBrandSupplierArticle.property.minimumCharWidth = 15;
        sidBrandSupplierArticle = addJProp(supplierAttributeGroup, "sidBrandSupplierArticle", "Бренд (ИД)", sidBrandSupplier, brandSupplierArticle, 1);
        sidBrandSupplierArticle.property.preferredCharWidth = 20;
        sidBrandSupplierArticle.property.minimumCharWidth = 10;

        supplierBrandSupplierArticle = addJProp(idGroup, "supplierBrandSupplierArticle", "Поставщик", supplierBrandSupplier, brandSupplierArticle, 1);
        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику бренда артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierBrandSupplier, brandSupplierArticle, 1), 1), true);

        brandSupplierArticleSku = addJProp(idGroup, true, "brandSupplierArticleSku", "Бренд (ИД)", brandSupplierArticle, articleSku, 1);
        nameBrandSupplierArticleSku = addJProp(supplierAttributeGroup, "nameBrandSupplierArticleSku", "Бренд", baseLM.name, brandSupplierArticleSku, 1);
        nameBrandSupplierArticleSku.property.preferredCharWidth = 30;
        nameBrandSupplierArticleSku.property.minimumCharWidth = 15;
        sidBrandSupplierArticleSku = addJProp(supplierAttributeGroup, "sidBrandSupplierArticleSku", "Бренд(ИД)", sidBrandSupplier, brandSupplierArticleSku, 1);
        sidBrandSupplierArticleSku.property.preferredCharWidth = 20;
        sidBrandSupplierArticleSku.property.minimumCharWidth = 10;

        themeSupplierArticle = addDProp(idGroup, "themeSupplierArticle", "Тема поставщика(ИД)", themeSupplier, article);
        nameThemeSupplierArticle = addJProp(supplierAttributeGroup, "nameThemeSupplierArticle", "Тема", baseLM.name, themeSupplierArticle, 1);
        nameThemeSupplierArticle.property.preferredCharWidth = 30;
        nameThemeSupplierArticle.property.minimumCharWidth = 15;

        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику темы артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierThemeSupplier, themeSupplierArticle, 1), 1), true);

        themeSupplierArticleSku = addJProp(idGroup, "themeSupplierArticleSku", "Тема (ИД)", themeSupplierArticle, articleSku, 1);
        nameThemeSupplierArticleSku = addJProp(supplierAttributeGroup, "nameThemeSupplierArticleSku", "Тема", baseLM.name, themeSupplierArticleSku, 1);
        nameThemeSupplierArticleSku.property.preferredCharWidth = 30;
        nameThemeSupplierArticleSku.property.minimumCharWidth = 15;
        sidThemeSupplierArticleSku = addJProp(baseGroup, "sidThemeSupplierArticleSku", "Код темы", sidThemeSupplier, themeSupplierArticleSku, 1);

        seasonSupplierArticle = addDProp(itemAttributeGroup, "seasonSupplierArticle", "Сезон (ИД)", seasonSupplier, article);
        nameSeasonSupplierArticle = addJProp(itemAttributeGroup, "nameSeasonSupplierArticle", "Сезон", baseLM.name, seasonSupplierArticle, 1);
        nameSeasonSupplierArticle.property.preferredCharWidth = 30;
        nameSeasonSupplierArticle.property.minimumCharWidth = 15;
        sidSeasonSupplierArticle = addJProp(itemAttributeGroup, "sidSeasonSupplierArticle", "Сезон поставщика", sidSeasonSupplier, seasonSupplierArticle, 1);

        seasonSupplierArticleSku = addJProp(idGroup, "seasonArticleSku", "Сезон", seasonSupplierArticle, articleSku, 1);
        sidSeasonSupplierArticleSku = addJProp(baseGroup, "sidSeasonSupplierArticleSku", "Сезон", sidSeasonSupplier, seasonSupplierArticleSku, 1);

        articleSIDSupplier = addAGProp(idGroup, "articleSIDSupplier", "Артикул (ИД)", sidArticle, supplierArticle);

        seekArticleSIDSupplier = addJProp(true, "Поиск артикула", addSAProp(null), articleSIDSupplier, 1, 2);
        seekArticleSIDInvoice = addJProp(true, "Поиск артикула", seekArticleSIDSupplier, 1, supplierDocument, 2);

        //???
        addArticleSingleSIDSupplier = addJProp(true, "Ввод простого артикула", addAAProp(articleSingle, sidArticle, supplierArticle), 1, 2);
        addNEArticleSingleSIDSupplier = addJProp(true, "Ввод простого артикула (НС)", baseLM.andNot1, addArticleSingleSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);
        addNEArticleSingleSIDInvoice = addJProp(true, "Ввод простого артикула (НС)", addNEArticleSingleSIDSupplier, 1, supplierDocument, 2);

        addArticleCompositeSIDSupplier = addJProp(true, "Ввод составного артикула", addAAProp(articleComposite, sidArticle, supplierArticle), 1, 2);
        addNEArticleCompositeSIDSupplier = addJProp(true, "Ввод составного артикула (НС)", baseLM.andNot1, addArticleCompositeSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);
        addNEArticleCompositeSIDInvoice = addJProp(true, "Ввод составного артикула (НС)", addNEArticleCompositeSIDSupplier, 1, supplierDocument, 2);

        addNEColorSupplierSIDSupplier = addJProp(true, "Ввод цвета (НС)", baseLM.andNot1, addAAProp(colorSupplier, sidColorSupplier, supplierColorSupplier), 1, 2, colorSIDSupplier, 1, 2);
        addNEColorSupplierSIDInvoice = addJProp(true, "Ввод цвета (НС)", addNEColorSupplierSIDSupplier, 1, supplierDocument, 2);

        executeAddColorDocument = addJProp(true, "Наличие цвета", addEPAProp(EPA_INTERFACE, inListArticleColorSupplier), 1, 2, 3, baseLM.vtrue);

        seekColorSIDSupplier = addJProp(true, "Поиск цвета", addSAProp(null), colorSIDSupplier, 1, 2);
        seekColorSIDInvoice = addJProp(true, "Поиск цвета", seekColorSIDSupplier, 1, supplierDocument, 2);

        executeArticleCompositeItemSIDSupplier = addJProp(true, "Замена артикула", addEPAProp(articleCompositeItem), 1, articleSIDSupplier, 2, 3);

        executeChangeFreightClass = addJProp(true, "Изменить класс фрахта", baseLM.and1, addEPAProp(baseLM.objectClass), 1, 2, is(freight), 1);

        executeChangeFreightClassApply = addEPAProp(EPA_DEFAULT, executeChangeFreightClass, 1, 2, baseLM.apply, baseLM.cancel);

        executeChangeFreightChangedClass = addJProp(true, "Пометить как обработанный", and(false, true),
                                                addJProp(executeChangeFreightClassApply, 1, addCProp(baseClass.objectClass, "freightChanged")), 1,
                                                is(freightComplete), 1,
                                                is(freightChanged), 1);
        executeChangeFreightChangedClass.property.askConfirm = true;
        executeChangeFreightChangedClass.setImage("sign_tick.png");

        executeChangeFreightShippedClass = addJProp(true, "Пометить как отгруженный", and(false, true),
                                                addJProp(executeChangeFreightClassApply, 1, addCProp(baseClass.objectClass, "freightShipped")), 1,
                                                is(freightPriced), 1,
                                                is(freightShipped), 1);
        executeChangeFreightShippedClass.property.askConfirm = true;
        executeChangeFreightShippedClass.setImage("sign_tick.png");

        supplierArticleSku = addJProp(idGroup, "supplierArticleSku", "Поставщик (ИД)", supplierArticle, articleSku, 1);
        nameSupplierArticleSku = addJProp(baseGroup, "nameSupplierArticleSku", "Поставщик", baseLM.name, supplierArticleSku, 1);

        jennyferSupplierArticleSku = addJProp("jennyferSupplierArticleSku", "Поставщик Jennyfer (ИД)", jennyferSupplierArticle, articleSku, 1);

        sizeSupplierItem = addDProp(itemAttributeGroup, "sizeSupplierItem", "Размер поставщика (ИД)", sizeSupplier, item);
        sidSizeSupplierItem = addJProp(itemAttributeGroup, "sidSizeSupplierItem", "Размер поставщика", sidSizeSupplier, sizeSupplierItem, 1);

        commonSizeItem = addJProp(idGroup, "commonSizeItem", "Размер (ИД)", commonSizeSizeSupplier, sizeSupplierItem, 1);
        nameCommonSizeItem = addJProp("nameCommonSizeItem", "Размер", baseLM.name, commonSizeItem, 1);

        genderGenderSupplier = addDProp(idGroup, "genderGenderSupplier", "Унифицированный пол", gender, genderSupplier);
        sidGenderGenderSupplier = addJProp(baseGroup, "sidGenderGenderSupplier", "Пол", sidGender, genderGenderSupplier, 1);

        genderSupplierArticle = addDProp(itemAttributeGroup, "genderSupplierArticle", "Пол поставщика (ИД)", genderSupplier, article);
        sidGenderSupplierArticle = addJProp(itemAttributeGroup, "sidGenderSupplierArticle", "Пол поставщика", sidGenderSupplier, genderSupplierArticle, 1);

        genderOriginArticle = addJProp(itemAttributeGroup, "genderOriginArticle", "Пол (ИД)", genderGenderSupplier, genderSupplierArticle, 1);

        LP supplierColorItem = addJProp(supplierColorSupplier, colorSupplierItem, 1);
        addConstraint(addJProp("Поставщик товара должен соответствовать цвету поставщика", baseLM.diff2,
                supplierArticleSku, 1,
                supplierColorItem, 1), true);

        LP supplierSizeItem = addJProp(supplierSizeSupplier, sizeSupplierItem, 1);
        addConstraint(addJProp("Поставщик товара должен соответствовать размеру поставщика", baseLM.diff2,
                supplierArticleSku, 1,
                supplierSizeItem, 1), true);

        equalsColorItemSupplier = addJProp(baseLM.equals2, supplierColorItem, 1, 2); // временное решение
        equalsSizeItemSupplier = addJProp(baseLM.equals2, supplierSizeItem, 1, 2); // временное решение

        LP supplierSeasonSupplierArticle = addJProp(supplierSeasonSupplier, seasonSupplierArticle, 1);
        addConstraint(addJProp("Поставщик товара должен соответствовать сезону поставщика", baseLM.diff2,
                supplierArticle, 1,
                supplierSeasonSupplierArticle, 1), true);

        LP supplierThemeArticle = addJProp(supplierThemeSupplier, themeSupplierArticle, 1);
        addConstraint(addJProp("Поставщик товара должен соответствовать теме поставщика", baseLM.diff2,
                supplierArticle, 1,
                supplierThemeArticle, 1), true);

        equalsSeasonSupplierArticleSupplier = addJProp(baseLM.equals2, supplierSeasonSupplierArticle, 1, 2); // временное решение
        equalsThemeItemSupplier = addJProp(baseLM.equals2, supplierThemeArticle, 1, 2); // временное решение

        addItemArticleCompositeColorSizeBarcode = addJProp(true, "Ввод товара", addAAProp(item, articleCompositeItem, colorSupplierItem, sizeSupplierItem, baseLM.barcode), 1, 2, 3, 4);
        addItemSIDArticleSupplierColorSizeBarcode = addJProp(true, "Ввод товара", addItemArticleCompositeColorSizeBarcode, articleSIDSupplier, 1, 2, 3, 4, 5);

        // Weight
        netWeightArticle = addDProp(supplierAttributeGroup, "netWeightArticle", "Вес нетто (ориг.)", DoubleClass.instance, article);
        netWeightArticleSku = addJProp(intraAttributeGroup, "netWeightArticleSku", "Вес нетто (ориг.)", netWeightArticle, articleSku, 1);
        netWeightArticleSize = addDProp(intraAttributeGroup, "netWeightArticleSize", "Вес нетто размера", DoubleClass.instance, article, sizeSupplier);

        netWeightDataSku = addDProp(intraAttributeGroup, "netWeightDataSku", "Вес нетто", DoubleClass.instance, sku);
        netWeightArticleSizeSku = addJProp(intraAttributeGroup, true, "netWeightArticleSizeSku", "Вес нетто", netWeightArticleSize, articleSku, 1, sizeSupplierItem, 1);
        netWeightSku = addSUProp(intraAttributeGroup, "netWeightSku", "Вес нетто (ед.)", Union.OVERRIDE, netWeightArticleSku, netWeightArticleSizeSku);

        // Gender
        genderSupplierArticleSku = addJProp(idGroup, "genderSupplierArticleSku", "Пол (ИД)", genderSupplierArticle, articleSku, 1);
        sidGenderSupplierArticleSku = addJProp(baseGroup, "sidGenderSupplierArticleSku", "Пол", sidGenderSupplier, genderSupplierArticleSku, 1);

        genderBrandSupplier = addDProp(idGroup, "genderBrandSupplier", "Пол (ИД)", gender, brandSupplier);
        sidGenderBrandSupplier = addJProp(baseGroup, "sidGenderBrandSupplier", "Пол", sidGender, genderBrandSupplier, 1);

        genderBrandSupplierArticle = addJProp(idGroup, "genderBrandSupplierArticle", "Пол (ИД)", genderBrandSupplier, brandSupplierArticle, 1);

        genderDataArticle = addDProp(idGroup, "genderDataArticle", "Пол (ИД)", gender, article);
        genderArticle = addSUProp(idGroup, "genderArticle", "Пол (ИД)", Union.OVERRIDE, genderBrandSupplierArticle, genderOriginArticle, genderDataArticle);
        sidGenderArticle = addJProp(baseGroup, "sidGenderArticle", "Пол", sidGender, genderArticle, 1);
        genderArticleSku = addJProp(idGroup, true, "genderArticleSku", true, "Пол (ИД)", genderArticle, articleSku, 1);
        sidGenderArticleSku = addJProp(baseGroup, "sidGenderArticleSku", "Пол", sidGender, genderArticleSku, 1);

        commonSizeSku = addJProp(idGroup, "commonSizeSku", "Унифицированный размер (ИД)", commonSizeSizeSupplierGenderCategory, sizeSupplierItem, 1, genderArticleSku, 1, categoryArticleSku, 1);
        nameCommonSizeSku = addJProp(baseGroup, "nameCommonSizeSku", "Унифицированный размер", baseLM.name, commonSizeSku, 1);

        // Type fabric
        typeFabricArticle = addDProp(idGroup, "typeFabricArticle", "Тип одежды (ИД)", typeFabric, article);
        nameTypeFabricArticle = addJProp(baseGroup, "nameTypeFabricArticle", "Тип одежды", baseLM.name, typeFabricArticle, 1);
        typeFabricArticleSku = addJProp(idGroup, true, "typeFabricArticleSku", true, "Тип одежды (ИД)", typeFabricArticle, articleSku, 1);
        nameTypeFabricArticleSku = addJProp(baseGroup, "nameTypeFabricArticleSku", "Тип одежды", baseLM.name, typeFabricArticleSku, 1);
        nameTypeFabricArticleSku.property.preferredCharWidth = 10;
        nameTypeFabricArticleSku.property.minimumCharWidth = 5;

        // Country
        countrySupplierOfOriginArticle = addDProp(idGroup, "countrySupplierOfOriginArticle", "Страна происхождения (ИД)", countrySupplier, article);
        nameCountrySupplierOfOriginArticle = addJProp(supplierAttributeGroup, "nameCountrySupplierOfOriginArticle", "Страна происхождения (ориг.)", baseLM.name, countrySupplierOfOriginArticle, 1);
        nameCountrySupplierOfOriginArticle.property.preferredCharWidth = 30;
        nameCountrySupplierOfOriginArticle.property.minimumCharWidth = 15;

        countrySupplierOfOriginArticleSku = addJProp(idGroup, "countrySupplierOfOriginArticleSku", "Страна происхождения (ИД)", countrySupplierOfOriginArticle, articleSku, 1);
        nameCountrySupplierOfOriginArticleSku = addJProp(supplierAttributeGroup, "nameCountrySupplierOfOriginArticleSku", "Страна происхождения (ориг.)", baseLM.name, countrySupplierOfOriginArticleSku, 1);
        nameCountrySupplierOfOriginArticleSku.property.preferredCharWidth = 30;
        nameCountrySupplierOfOriginArticleSku.property.minimumCharWidth = 15;

        countryOfOriginArticle = addJProp(idGroup, "countryOfOriginArticle", "Страна происхождения (ИД)", countryCountrySupplier, countrySupplierOfOriginArticle, 1);
        nameCountryOfOriginArticle = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticle", "Страна происхождения", nameOriginCountry, countryOfOriginArticle, 1);

        countryOfOriginArticleSku = addJProp(idGroup, "countryOfOriginArticleSku", "Страна происхождения (ИД)", countryOfOriginArticle, articleSku, 1);
        nameCountryOfOriginArticleSku = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSku", "Страна происхождения", nameOriginCountry, countryOfOriginArticleSku, 1);

        countryOfOriginArticleColor = addDProp(idGroup, "countryOfOriginArticleColor", "Страна происхождения (ИД)", baseLM.country, article, colorSupplier);
        countryOfOriginArticleColorSku = addJProp(idGroup, true, "countryOfOriginArticleColorSku", "Страна происхождения (ИД)", countryOfOriginArticleColor, articleSku, 1, colorSupplierItem, 1);
        nameCountryArticleColor = addJProp(baseGroup, "nameCountryArticleColor", "Страна происхождения", baseLM.name, countryOfOriginArticleColor, 1, 2);
        nameCountryArticleColor.property.preferredCharWidth = 50;
        nameCountryArticleColor.property.minimumCharWidth = 15;

        countryOfOriginDataSku = addDProp(idGroup, "countryOfOriginDataSku", "Страна происхождения (ИД) (первичное)", baseLM.country, sku);

        countryOfOriginSku = addSUProp(idGroup, "countryOfOriginSku", true, "Страна происхождения (ИД)", Union.OVERRIDE, countryOfOriginArticleSku, countryOfOriginArticleColorSku);
        nameCountryOfOriginSku = addJProp(intraAttributeGroup, "nameCountryOfOriginSku", "Страна происхождения", nameOriginCountry, countryOfOriginSku, 1);
        nameCountrySku = addJProp(intraAttributeGroup, "nameCountrySku", "Страна происхождения", baseLM.name, countryOfOriginSku, 1);
        nameCountrySku.property.preferredCharWidth = 50;
        nameCountrySku.property.minimumCharWidth = 15;

        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику страны артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierCountrySupplier, countrySupplierOfOriginArticle, 1), 1), true);

        countryBrandSupplierSku = addJProp(idGroup, "countryBrandSupplierSku", "Страна поставки (ИД)", countryBrandSupplier, brandSupplierArticleSku, 1);
        nameCountryBrandSupplierSku = addJProp(baseGroup, "nameCountryBrandSupplierSku", "Страна поставки", baseLM.name, countryBrandSupplierSku, 1);


        // Composition
        mainCompositionOriginArticle = addDProp(supplierAttributeGroup, "mainCompositionOriginArticle", "Состав", COMPOSITION_CLASS, article);
        mainCompositionOriginArticle.property.preferredCharWidth = 80;
        mainCompositionOriginArticle.property.minimumCharWidth = 40;
        additionalCompositionOriginArticle = addDProp(supplierAttributeGroup, "additionalCompositionOriginArticle", "Доп. состав", COMPOSITION_CLASS, article);
        additionalCompositionOriginArticle.property.preferredCharWidth = 40;
        additionalCompositionOriginArticle.property.minimumCharWidth = 20;

        mainCompositionOriginArticleSku = addJProp(supplierAttributeGroup, "mainCompositionOriginArticleSku", "Состав", mainCompositionOriginArticle, articleSku, 1);
        additionalCompositionOriginArticleSku = addJProp(supplierAttributeGroup, "additionalCompositionOriginArticleSku", "Доп. состав", additionalCompositionOriginArticle, articleSku, 1);

        mainCompositionOriginArticleColor = addDProp(supplierAttributeGroup, "mainCompositionOriginArticleColor", "Состав", COMPOSITION_CLASS, article, colorSupplier);
        additionalCompositionOriginArticleColor = addDProp(supplierAttributeGroup, "additionalCompositionOriginArticleColor", "Доп. состав", COMPOSITION_CLASS, article, colorSupplier);

        mainCompositionOriginArticleColorSku = addJProp(supplierAttributeGroup, true, "mainCompositionOriginArticleColorSku", "Состав", mainCompositionOriginArticleColor, articleSku, 1, colorSupplierItem, 1);
        additionalCompositionOriginArticleColorSku = addJProp(supplierAttributeGroup, true, "additionalCompositionOriginArticleColorSku", "Доп. состав", additionalCompositionOriginArticleColor, articleSku, 1, colorSupplierItem, 1);

        mainCompositionOriginDataSku = addDProp(intraAttributeGroup, "mainCompositionOriginDataSku", "Состав", COMPOSITION_CLASS, sku);
        additionalCompositionOriginDataSku = addDProp(intraAttributeGroup, "additionalCompositionOriginDataSku", "Доп. состав", COMPOSITION_CLASS, sku);

        mainCompositionOriginSku = addSUProp(intraAttributeGroup, "mainCompositionOriginSku", true, "Состав", Union. OVERRIDE, mainCompositionOriginArticleSku, mainCompositionOriginArticleColorSku);
        mainCompositionOriginSku.setPreferredCharWidth(80);

        additionalCompositionOriginSku = addSUProp(intraAttributeGroup, "additionalCompositionOriginSku", "Доп. состав", Union.OVERRIDE, additionalCompositionOriginArticleSku, additionalCompositionOriginArticleColorSku);
        additionalCompositionOriginSku.property.preferredCharWidth = 40;
        additionalCompositionOriginSku.property.minimumCharWidth = 20;

        mainCompositionArticle = addDProp(intraAttributeGroup, "mainCompositionArticle", "Состав (перевод)", COMPOSITION_CLASS, article);
        additionalCompositionArticle = addDProp(intraAttributeGroup, "additionalCompositionArticle", "Доп. состав (перевод)", COMPOSITION_CLASS, article);

        mainCompositionSku = addDProp(intraAttributeGroup, "mainCompositionSku", "Состав (перевод)", COMPOSITION_CLASS, sku);
        additionalCompositionSku = addDProp(intraAttributeGroup, "additionalCompositionSku", "Доп. состав (перевод)", COMPOSITION_CLASS, sku);
        additionalCompositionSku.property.preferredCharWidth = 40;
        additionalCompositionSku.property.minimumCharWidth = 20;

        // CustomCategory
        customCategoryOriginArticle = addDProp(idGroup, "customCategoryOriginArticle", "ТН ВЭД (ориг.) (ИД)", customCategoryOrigin, article);
        sidCustomCategoryOriginArticle = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticle", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategory6Article = addDProp(idGroup, "customCategory6Article", "ТН ВЭД (6) (ИД)", customCategory6, article);
        sidCustomCategory6Article = addJProp(supplierAttributeGroup, "sidCustomCategory6Article", "Код ТН ВЭД (6)", sidCustomCategory6, customCategory6Article, 1);
        customCategoryOriginArticleSku = addJProp(idGroup, true, "customCategoryOriginArticleSku", "ТН ВЭД (ориг.) (ИД)", customCategoryOriginArticle, articleSku, 1);
        sidCustomCategoryOriginArticleSku = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticleSku", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticleSku, 1);

        customCategory10CategoryGenderCompositionTypeFabric = addDProp(idGroup, "customCategory10FreightCategoryGenderCompositionTypeFabric", "ТН ВЭД (ИД)", customCategory10, category, gender, COMPOSITION_CLASS, typeFabric);
        sidCustomCategory10CategoryGenderCompositionTypeFabric = addJProp(baseGroup, "sidCustomCategory10CategoryGenderCompositionTypeFabric", "ТН ВЭД", sidCustomCategory10, customCategory10CategoryGenderCompositionTypeFabric, 1, 2, 3, 4);

        customCategory10CategoryGenderCompositionTypeFabricSku = addJProp(idGroup, "customCategory10CategoryGenderCompositionTypeFabricSku", "ТН ВЭД (ИД)", customCategory10CategoryGenderCompositionTypeFabric,
                                categoryArticleSku, 1, genderArticleSku, 1, mainCompositionOriginSku, 1, typeFabricArticleSku, 1);

        customCategory10DataSku = addDProp(idGroup, "customCategory10DataSku", "ТН ВЭД (ИД)", customCategory10, sku);
        customCategory10CustomCategoryOriginArticle = addJProp(idGroup, "customCategory10CustomCategoryOriginArticle", "ТН ВЭД (ИД)", customCategory10CustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategory10CustomCategoryOriginArticleSku = addJProp(idGroup, "customCategory10CustomCategoryOriginArticleSku", "ТН ВЭД (ИД)", customCategory10CustomCategoryOriginArticle, articleSku, 1);
        customCategory10Sku = addSUProp(idGroup, "customCategory10Sku", true, "ТН ВЭД (ИД)", Union.OVERRIDE, customCategory10CustomCategoryOriginArticleSku, customCategory10DataSku, customCategory10CategoryGenderCompositionTypeFabricSku);
        customCategory9Sku = addJProp(baseGroup, "customCategory9Sku", "ТН ВЭД", customCategory9CustomCategory10, customCategory10Sku, 1);
        sidCustomCategory10Sku = addJProp(baseGroup, "sidCustomCategory10Sku", "ТН ВЭД", sidCustomCategory10, customCategory10Sku, 1);

        diffCountRelationCustomCategory10Sku = addJProp("diffCountRelationCustomCategory10Sku", baseLM.greater2, addJProp(countRelationCustomCategory10, customCategory10Sku, 1), 1, addCProp("1", DoubleClass.instance, 1));
        diffCountRelationCustomCategory10FreightSku = addJProp("diffCountRelationCustomCategory10FreightSku", baseLM.and1, diffCountRelationCustomCategory10Sku, 2, is(freight), 1);

        subCategoryDataSku = addDProp(idGroup, "subCategoryDataSku", "Дополнительное деление (ИД)", subCategory, sku);
        nameSubCategoryDataSku = addJProp(baseGroup, "nameSubCategoryDataSku", "Дополнительное деление", nameSubCategory, subCategoryDataSku, 1);
        subCategoryCustomCategory10Sku = addJProp(idGroup, "subCategoryCustomCategory10Sku", "Дополнительное деление (ИД)", subCategoryCustomCategory10, customCategory10Sku, 1);

        subCategorySku = addSUProp(idGroup, "subCategorySku", "Дополнительное деление (ИД)", Union.OVERRIDE, subCategoryCustomCategory10Sku, subCategoryDataSku);
        nameSubCategorySku = addJProp(baseGroup, "nameSubCategorySku", "Дополнительное деление", nameSubCategory, subCategorySku, 1);

        addConstraint(addJProp("Выбранный для товара ТН ВЭД должен иметь верхний элемент", baseLM.andNot1, customCategory10Sku, 1, customCategory9Sku, 1), false);

        addConstraint(addJProp("Категория минимальных предельных цен запрещена для выбранного кода ТНВЭД", and(false, false, true), addCProp(LogicalClass.instance, true, sku), 1,
                   customCategory10Sku, 1,
                   subCategoryDataSku, 1,
                   addJProp(relationCustomCategory10SubCategory, customCategory10Sku, 1, subCategorySku, 1), 1), true);

        // barcode Jennyfer
        substring10 = addSFProp("substring10", "substring(prm1,1,10)", StringClass.get(10), 1);
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
        sidSupplierBox.property.preferredCharWidth = 50;
        sidSupplierBox.property.minimumCharWidth = 20;

        boxInvoiceSupplierBox = addDProp(idGroup, "boxInvoiceSupplierBox", "Документ по коробам (ИД)", boxInvoice, supplierBox);
        setNotNull(boxInvoiceSupplierBox, PropertyFollows.RESOLVE_FALSE);

        sidBoxInvoiceSupplierBox = addJProp(baseGroup, "sidBoxInvoiceSupplierBox", "Документ по коробам", sidDocument, boxInvoiceSupplierBox, 1);

        destinationInvoiceSupplierBox = addJProp(idGroup, "destinationInvoiceSupplierBox", "Пункт назначения (ИД)", destinationDestinationDocument, boxInvoiceSupplierBox, 1);
        nameDestinationInvoiceSupplierBox = addJProp(baseGroup, "nameDestinationInvoiceSupplierBox", "Пункт назначения", baseLM.name, destinationInvoiceSupplierBox, 1);
        nameDestinationInvoiceSupplierBox.property.preferredCharWidth = 50;
        nameDestinationInvoiceSupplierBox.property.minimumCharWidth = 20;

        supplierSupplierBox = addJProp(idGroup, "supplierSupplierBox", "Поставщик (ИД)", supplierDocument, boxInvoiceSupplierBox, 1);

        supplierBoxSIDSupplier = addAGProp(idGroup, "supplierBoxSIDSupplier", "Короб поставщика (ИД)", sidSupplierBox, supplierSupplierBox);

        seekSupplierBoxSIDSupplier = addJProp(true, "Поиск короба поставщика", addSAProp(null), supplierBoxSIDSupplier, 1, 2);

        // заказ по артикулам
        documentList = addCUProp(idGroup, "documentList", "Документ (ИД)", object(order), object(simpleInvoice), boxInvoiceSupplierBox);
        supplierList = addJProp(idGroup, "supplierList", "Поставщик (ИД)", supplierDocument, documentList, 1);

        articleSIDList = addJProp(idGroup, "articleSIDList", "Артикул (ИД)", articleSIDSupplier, 1, supplierList, 2);

        numberListArticle = addDProp(baseGroup, "numberListArticle", "Номер", IntegerClass.instance, list, article);
        numberListSIDArticle = addJProp(numberListArticle, 1, articleSIDList, 2, 1);

        //notZeroListArticle = addJProp(baseLM.andNot1, );

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
        themeCodePricat = addDProp(baseGroup, "themeCodePricat", "Код темы", StringClass.get(20), pricat);
        themeNamePricat = addDProp(baseGroup, "themeNamePricat", "Тема", StringClass.get(50), pricat);
        sizePricat = addDProp(baseGroup, "sizePricat", "Размер", StringClass.get(5), pricat);
        seasonPricat = addDProp(baseGroup, "seasonPricat", "Сезон", StringClass.get(10), pricat);
        genderPricat = addDProp(baseGroup, "genderPricat", "Пол", StringClass.get(10), pricat);
        originalNamePricat = addDProp(baseGroup, "originalNamePricat", "Наименование (ориг.)", StringClass.get(50), pricat);
        countryPricat = addDProp(baseGroup, "countryPricat", "Страна происхождения", StringClass.get(20), pricat);
        netWeightPricat = addDProp(baseGroup, "netWeightPricat", "Вес нетто", DoubleClass.instance, pricat);
        compositionPricat = addDProp(baseGroup, "compositionPricat", "Состав", StringClass.get(50), pricat);
        pricePricat = addDProp(baseGroup, "pricePricat", "Цена", DoubleClass.instance, pricat);
        rrpPricat = addDProp(baseGroup, "RRP", "Рекомендованная цена", DoubleClass.instance, pricat);
        destinationPricat = addDProp("destinationPricat", "Пункт назначения", destination, pricat);
        supplierPricat = addDProp("supplierPricat", "Поставщик", supplier, pricat);
        barcodeToPricat = addAGProp("barcodeToPricat", "штрих-код", barcodePricat);
        importPricatSupplier = addProperty(null, new LP<ClassPropertyInterface>(new PricatEDIImportActionProperty(genSID(), this, supplier)));
        hugoBossImportPricat = addProperty(null, new LP<ClassPropertyInterface>(new HugoBossPricatCSVImportActionProperty(genSID(), this, hugoBossSupplier)));
        gerryWeberImportPricat = addProperty(null, new LP<ClassPropertyInterface>(new GerryWeberPricatCSVImportActionProperty(genSID(), this, gerryWeberSupplier)));

        // кол-во заказа
        quantityDataListSku = addDProp("quantityDataListSku", "Кол-во (первичное)", DoubleClass.instance, list, sku);
        quantityListSku = quantityDataListSku; //addJProp(baseGroup, "quantityListSku", true, "Кол-во", baseLM.and1, quantityDataListSku, 1, 2, numberListSku, 1, 2);
        quantityDataList = addSGProp(baseGroup, "quantityDataList", "Кол-во", quantityDataListSku, 1);

        quantitySimpleInvoiceArticle = addDProp(baseGroup, "quantitySimpleInvoiceArticle", "Кол-во", IntegerClass.instance, simpleInvoice, articleComposite);
        quantitySimpleInvoice = addSGProp(baseGroup, "quantitySimpleInvoice", true, true, "Кол-во в документе", quantityListSku, documentList, 1, 2);

        quantityDocumentSku = addSGProp(baseGroup, "quantityDocumentSku", true, true, "Кол-во в документе", quantityListSku, documentList, 1, 2);
        quantityInvoicedDocumentArticle = addSGProp(baseGroup, "quantityInvoicedDocumentArticle", "Кол-во артикула в документе", quantityDocumentSku, 1, articleSku, 2);
        quantityDocumentArticle = addSUProp(baseGroup, "quantityDocumentArticle", "Кол-во артикула в документе", Union.SUM, quantityInvoicedDocumentArticle, quantitySimpleInvoiceArticle);
        quantityDocument = addSGProp(baseGroup, "quantityDocument", "Общее кол-во в документе", quantityDocumentArticle, 1);

        quantitySizeSupplierGenderCategory = addSGProp(baseGroup, "quantitySizeSupplierGenderCategory", true, "Кол-во", addCProp(IntegerClass.instance, 1, sku), sizeSupplierItem, 1, genderArticleSku, 1, categoryArticleSku, 1);

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
        quantityOrderInvoiceSku = addPGProp(baseGroup, "quantityOrderInvoiceSku", true, 0, true, "Кол-во по заказу/инвойсу (расч.)",
                orderedOrderInvoiceSku,
                quantityDocumentSku, 2, 3);

        invoicedOrderSku = addSGProp(baseGroup, "invoicedOrderSku", "Выставлено инвойсов", quantityOrderInvoiceSku, 1, 3);

        // todo : не работает на инвойсе/простом товаре
        quantityListArticle = addDGProp(baseGroup, "quantityListArticle", "Кол-во (итог)",
                1, false, // кол-во объектов для порядка и ascending/descending
                quantityListSku, 1, articleSku, 2,
                addCUProp(addCProp(DoubleClass.instance, Double.MAX_VALUE, list, articleSingle),
                        addCProp(DoubleClass.instance, Double.MAX_VALUE, order, item),
                        addJProp(baseLM.and1, orderedSimpleInvoiceSku, 1, 2, is(item), 2), // если не артикул (простой), то пропорционально заказано
                        addJProp(baseLM.and1, orderedSupplierBoxSku, 1, 2, is(item), 2)), 1, 2, // ограничение (максимально-возможное число)
                2);

        quantityListArticleCompositeColor = addSGProp(baseGroup, "quantityListArticleCompositeColor", "Кол-во", quantityListSku, 1, articleCompositeItem, 2, colorSupplierItem, 2);
        quantityListArticleCompositeSize = addSGProp(baseGroup, "quantityListArticleCompositeSize", "Кол-во", quantityListSku, 1, articleCompositeItem, 2, sizeSupplierItem, 2);

        quantityListArticleCompositeColorSize = addDGProp(baseGroup, "quantityListArticleCompositeColorSize", "Кол-во",
                1, false,
                quantityListSku, 1, articleCompositeItem, 2, colorSupplierItem, 2, sizeSupplierItem, 2,
                addCProp(DoubleClass.instance, Double.MAX_VALUE, list, sku), 1, 2,
                2);
        quantityListArticleCompositeColorSize.property.setFixedCharWidth(2);

        itemArticleCompositeColorSize = addAGProp("itemArticleCompositeColorSize", "Item", true, articleCompositeItem, colorSupplierItem, sizeSupplierItem);

        orderedOrderInvoiceArticle = addJProp(baseLM.and1, quantityListArticle, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceArticle = addSGProp(baseGroup, "orderedInvoiceArticle", "Кол-во заказано", orderedOrderInvoiceArticle, 2, 3);
        // todo : сделать, чтобы работало автоматическое проставление
//        quantityListArticle.setDerivedForcedChange(orderedInvoiceArticle, 1, 2, numberListArticle, 1, 2);

        invoicedOrderArticle = addSGProp(baseGroup, "invoicedOrderArticle", "Выставлено инвойсов", invoicedOrderSku, 1, articleSku, 2);

        itemSupplierArticleSIDColorSIDSizeSID = addJProp("itemSupplierArticleSIDColorSIDSizeSID", "item", itemArticleCompositeColorSize, articleSIDSupplier, 2, 1, colorSIDSupplier, 2, 3, sizeSIDSupplier, 2, 4);

        // сроки заказа
        dateFromOrder = addDProp(baseGroup, "dateFromOrder", "Дата с", DateClass.instance, order);
        dateToOrder = addDProp(baseGroup, "dateToOrder", "Дата по", DateClass.instance, order);

        dateFromDataOrderArticle = addDProp(baseGroup, "dateFromDataOrderArticle", "Дата с", DateClass.instance, order, article);
        dateToDataOrderArticle = addDProp(baseGroup, "dateToDataOrderArticle", "Дата по", DateClass.instance, order, article);

        dateFromOrderOrderArticle = addJProp(baseGroup, "dateFromOrderOrderArticle", "Дата с", baseLM.and1, dateFromOrder, 1, is(article), 2);
        dateToOrderOrderArticle = addJProp(baseGroup, "dateToOrderOrderArticle", "Дата по", baseLM.and1, dateToOrder, 1, is(article), 2);

        dateFromOrderArticle = addSUProp(baseGroup, "dateFromOrderArticle", "Дата с", Union.OVERRIDE, dateFromOrderOrderArticle, dateFromDataOrderArticle);
        dateToOrderArticle = addSUProp(baseGroup, "dateToOrderArticle", "Дата по", Union.OVERRIDE, dateToOrderOrderArticle, dateToDataOrderArticle);

        dateFromOrderArticleSku = addJProp(baseGroup, "dateFromOrderArticleSku", "Дата с", dateFromOrderArticle, 1, articleSku, 2);
        dateToOrderArticleSku = addJProp(baseGroup, "dateToOrderArticleSku", "Дата по", dateToOrderArticle, 1, articleSku, 2);

        // цены
        priceDocumentArticle = addDProp(baseGroup, "priceDocumentArticle", "Цена", DoubleClass.instance, priceDocument, article);
        //priceRateDocumentArticle = addJProp(baseGroup, "priceRateDocumentArticle", true, "Цена (конверт.)", round2, addJProp(baseLM.multiplyDouble2, priceDocumentArticle, 1, 2, addJProp(nearestRateExchange, typeExchangeSTX, currencyDocument, 1, 1), 1), 1, 2);

        priceDataDocumentItem = addDProp(baseGroup, "priceDataDocumentItem", "Цена по товару", DoubleClass.instance, priceDocument, item);
        priceArticleDocumentSku = addJProp(baseGroup, "priceArticleDocumentItem", "Цена по артикулу", priceDocumentArticle, 1, articleSku, 2);
        priceDocumentSku = addSUProp(baseGroup, "priceDocumentSku", true, "Цена", Union.OVERRIDE, priceArticleDocumentSku, priceDataDocumentItem);

        priceRateDocumentSku = addJProp(baseGroup, "priceRateDocumentSku", true, "Цена (конверт.)", round2, addJProp(baseLM.multiplyDouble2, priceDocumentSku, 1, 2, addJProp(nearestRateExchange, typeExchangeSTX, currencyDocument, 1, 1), 1), 1, 2);

        RRPDocumentArticle = addDProp(baseGroup, "RRPDocumentArticle", "Рекомендованная цена", DoubleClass.instance, priceDocument, article);
        RRPRateDocumentArticle = addJProp(baseGroup, "RRPRateDocumentArticle", true, "Рекомендованная цена (конверт.)", round2, addJProp(baseLM.multiplyDouble2, RRPDocumentArticle, 1, 2, addJProp(nearestRateExchange, typeExchangeSTX, currencyDocument, 1, 1), 1), 1, 2);
        RRPRateDocumentSku = addJProp(baseGroup, "RRPRateDocumentSku", "Рекомендованная цена (конверт.)", RRPRateDocumentArticle, 1, articleSku, 2);

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
        sumSimpleInvoiceArticle = addJProp(baseGroup, "sumSimpleInvoiceArticle", "Сумма по артикулу", baseLM.multiplyDouble2, priceDocumentArticle, 1, 2, quantitySimpleInvoiceArticle, 1, 2);

        sumSimpleInvoice = addSGProp(baseGroup, "sumSimpleInvoice", "Сумма", sumSimpleInvoiceArticle, 1);
        sumInvoicedDocument = addSGProp(baseGroup, "sumInvoicedDocument", "Сумма", sumDocumentSku, 1);

        sumDocument = addSUProp(baseGroup, "sumDocument", "Сумма документа", Union.SUM, sumInvoicedDocument, sumSimpleInvoice);

        // route
        percentShipmentRoute = addDProp(baseGroup, "percentShipmentRoute", "Процент", DoubleClass.instance, shipment, route);

        percentShipmentRouteSku = addJProp(baseGroup, "percentShipmentRouteSku", "Процент", baseLM.and1, percentShipmentRoute, 1, 2, is(sku), 3);

        // creation
        quantityCreationSku = addDProp(baseGroup, "quantityCreationSku", "Количество", IntegerClass.instance, creationSku);

        creationSkuSku = addDProp(idGroup, "creationSkuSku", "Операция (ИД)", creationSku, sku);

        quantityCreationFreightBox = addDProp(baseGroup, "quantityCreationFreightBox", "Количество", IntegerClass.instance, creationFreightBox);
        routeCreationFreightBox = addDProp(idGroup, "routeCreationFreightBox", "Маршрут (ИД)", route, creationFreightBox);
        nameRouteCreationFreightBox = addJProp(baseGroup, "nameRouteCreationFreightBox", "Маршрут", baseLM.name, routeCreationFreightBox, 1);

        quantityCreationPallet = addDProp(baseGroup, "quantityCreationPallet", "Количество", IntegerClass.instance, creationPallet);
        routeCreationPallet = addDProp(idGroup, "routeCreationPallet", "Маршрут (ИД)", route, creationPallet);
        nameRouteCreationPallet = addJProp(baseGroup, "nameRouteCreationPallet", "Маршрут", baseLM.name, routeCreationPallet, 1);

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

        freightShippedDirectInvoice = addJProp(baseGroup, "freightShippedDirectInvoice", "Инвойс отгружен", is(freightShipped), freightDirectInvoice, 1);

        sumDirectInvoicedSku = addSGProp(baseGroup, "sumDirectInvoicedSku", true, "Сумма по инвойсам напрямую", addJProp(and(false, true), sumDocumentSku, 1, 2, is(directInvoice), 1, freightShippedDirectInvoice, 1), 2);
        quantityDirectInvoicedSku = addSGProp(baseGroup, "quantityDirectInvoicedSku", true, "Кол-во по инвойсам напрямую", addJProp(and(false, true), quantityDocumentSku, 1, 2, is(directInvoice), 1, freightShippedDirectInvoice, 1), 2);
        quantityDocumentBrandSupplier = addSGProp(baseGroup, "quantityDocumentBrandSupplier", true, "Кол-во по бренду в документе", addJProp(and(false, true), quantityDocumentSku, 1, 2, is(directInvoice), 1, freightShippedDirectInvoice, 1), 1, brandSupplierArticleSku, 2);
        quantityAllDocumentsBrandSupplier = addSGProp(baseGroup, "quantityAllDocumentsBrandSupplier", true, "Кол-во по бренду в документах", quantityDocumentBrandSupplier, 2);

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

        destinationDataSupplierBox = addDProp(idGroup, "destinationDataSupplierBox", "Пункт назначения (ИД)", destination, supplierBox);
        sidDestinationDataSupplierBox = addJProp(baseGroup, "sidDestinationDataSupplierBox", "Пункт назначения (код)", sidDestination, destinationDataSupplierBox, 1);
        nameDestinationDataSupplierBox = addJProp(baseGroup, "nameDestinationDataSupplierBox", "Пункт назначения", baseLM.name, destinationDataSupplierBox, 1);

        destinationSupplierBox = addSUProp(idGroup, "destinationSupplierBox", "Пункт назначения (ИД)", Union.OVERRIDE, destinationInvoiceSupplierBox, destinationDataSupplierBox);
        nameDestinationSupplierBox = addJProp(baseGroup, "nameDestinationSupplierBox", "Пункт назначения", baseLM.name, destinationSupplierBox, 1);

        // поставка на склад
        inOrderShipment = addDProp(baseGroup, "inOrderShipment", "Вкл", LogicalClass.instance, order, shipment);

        inInvoiceShipment = addDProp(baseGroup, "inInvoiceShipment", "Вкл", LogicalClass.instance, invoice, shipment);

        inSupplierBoxShipment = addJProp(baseGroup, "inSupplierBoxShipment", "Вкл", inInvoiceShipment, boxInvoiceSupplierBox, 1, 2);

        invoicedShipmentSku = addSGProp(baseGroup, "invoicedShipmentSku", true, true, "Ожид. (пост.)",
                addJProp(baseLM.and1, quantityDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        invoicedBetweenDateSku = addSGProp(baseGroup, "invoicedBetweenDateSku", "Заявленное кол-во на период", addJProp(baseLM.and1, invoicedShipmentSku, 1, 2, addJProp(baseLM.betweenDates, dateArrivalShipment, 1, object(DateClass.instance), 3, object(DateClass.instance), 4), 1, 2, 3, 4), 2, 3, 4);
        invoicedBetweenDateBrandSupplier = addSGProp(baseGroup, "invoicedBetweenDateBrandSupplier", "Заявленное кол-во на период", invoicedBetweenDateSku, brandSupplierArticleSku, 1, 2, 3);

        emptyBarcodeShipment = addSGProp(privateGroup, "emptyBarcodeShipment", true, true, "Кол-во позиций без штрих-кода",
                addJProp(and(false, true), addCProp(IntegerClass.instance, 1, shipment), 1, invoicedShipmentSku, 1, 2, baseLM.barcode, 2),
                1);

        priceShipmentSku = addMGProp(baseGroup, "priceShipmentSku", true, "Цена (пост.)",
                addJProp(baseLM.and1, priceDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        invoicedShipment = addSGProp(baseGroup, "invoicedShipment", true, "Всего ожидается (пост.)", invoicedShipmentSku, 1);

        //sku shipment detail
        skuShipmentDetail = addDProp(idGroup, "skuShipmentDetail", "SKU (ИД)", sku, shipmentDetail);
        barcodeSkuShipmentDetail = addJProp(baseGroup, "barcodeSkuShipmentDetail", "Штрих-код SKU", baseLM.barcode, skuShipmentDetail, 1);

        articleShipmentDetail = addJProp(idGroup, "articleShipmentDetail", "Артикул (ИД)", articleSku, skuShipmentDetail, 1);
        sidArticleShipmentDetail = addJProp(baseGroup, "sidArticleShipmentDetail", "Артикул", sidArticle, articleShipmentDetail, 1);
        
        colorSupplierItemShipmentDetail = addJProp(idGroup, "colorSupplierItemShipmentDetail", "Цвет поставщика (ИД)", colorSupplierItem, skuShipmentDetail, 1);
        sidColorSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "sidColorSupplierItemShipmentDetail", "Код цвета", sidColorSupplier, colorSupplierItemShipmentDetail, 1);
        nameColorSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "nameColorSupplierItemShipmentDetail", "Цвет поставщика", baseLM.name, colorSupplierItemShipmentDetail, 1);

        sizeSupplierItemShipmentDetail = addJProp(idGroup, "sizeSupplierItemShipmentDetail", "Размер поставщика (ИД)", sizeSupplierItem, skuShipmentDetail, 1);
        sidSizeSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "sidSizeSupplierItemShipmentDetail", "Размер поставщика", sidSizeSupplier, sizeSupplierItemShipmentDetail, 1);

        nameBrandSupplierArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "nameBrandSupplierArticleSkuShipmentDetail", "Бренд", nameBrandSupplierArticleSku, skuShipmentDetail, 1);
        originalNameArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "originalNameArticleSkuShipmentDetail", "Наименование (ориг.)", originalNameArticleSku, skuShipmentDetail, 1);

        genderArticleSkuShipmentDetail = addJProp(idGroup, true, "genderArticleSkuShipmentDetail", "Пол товара (ИД)", genderArticleSku, skuShipmentDetail, 1);
        sidGenderArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "sidGenderArticleSkuShipmentDetail", "Пол товара", sidGender, genderArticleSkuShipmentDetail, 1);
        sidGenderArticleSkuShipmentDetail.property.preferredCharWidth = 10;
        sidGenderArticleSkuShipmentDetail.property.minimumCharWidth = 5;

        typeFabricArticleSkuShipmentDetail = addJProp(idGroup, true, "typeFabricArticleSkuShipmentDetail", "Тип одежды (ИД)", typeFabricArticleSku, skuShipmentDetail, 1);
        nameTypeFabricArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameTypeFabricArticleSkuShipmentDetail", "Тип одежды", baseLM.name, typeFabricArticleSkuShipmentDetail, 1);
        nameTypeFabricArticleSkuShipmentDetail.property.preferredCharWidth = 10;
        nameTypeFabricArticleSkuShipmentDetail.property.minimumCharWidth = 5;

        categoryArticleSkuShipmentDetail = addJProp(idGroup, true, "categoryArticleSkuShipmentDetail", "Номенклатурная группа товара (ИД)", categoryArticleSku, skuShipmentDetail, 1);
        nameOriginCategoryArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameOriginCategoryArticleSkuShipmentDetail", "Номенклатурная группа товара", nameOrigin, categoryArticleSkuShipmentDetail, 1);
        nameCategoryArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameCategoryArticleSkuShipmentDetail", "Номенклатурная группа товара", baseLM.name, categoryArticleSkuShipmentDetail, 1);

        coefficientArticleSkuShipmentDetail = addJProp(intraAttributeGroup, true, "coefficientArticleSkuShipmentDetail", "Кол-во в комплекте", coefficientArticleSku, skuShipmentDetail, 1);

        customCategoryOriginArticleSkuShipmentDetail = addJProp(idGroup, true, "customCategoryOriginArticleSkuShipmentDetail", "ТН ВЭД (ИД)", customCategoryOriginArticleSku, skuShipmentDetail, 1);
        sidCustomCategoryOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticleSkuShipmentDetail", "Код ТН ВЭД", sidCustomCategoryOrigin, customCategoryOriginArticleSkuShipmentDetail, 1);

        netWeightArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "netWeightArticleSkuShipmentDetail", "Вес нетто (ориг.)", netWeightArticleSku, skuShipmentDetail, 1);
        netWeightSkuShipmentDetail = addJProp(intraAttributeGroup, true, "netWeightSkuShipmentDetail", "Вес нетто (ед.)", netWeightSku, skuShipmentDetail, 1);

        countryOfOriginArticleSkuShipmentDetail = addJProp(idGroup, "countryOfOriginArticleSkuShipmentDetail", "Страна происхождения (ориг.) (ИД)", countryOfOriginArticleSku, skuShipmentDetail, 1);
        nameCountryOfOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSkuShipmentDetail", "Страна происхождения", nameOriginCountry, countryOfOriginArticleSkuShipmentDetail, 1);

        countryOfOriginSkuShipmentDetail = addJProp(idGroup, true, "countryOfOriginSkuShipmentDetail", "Страна происхождения (ИД)", countryOfOriginSku, skuShipmentDetail, 1);
        nameCountryOfOriginSkuShipmentDetail = addJProp(intraAttributeGroup, "nameCountryOfOriginSkuShipmentDetail", "Страна происхождения", nameOriginCountry, countryOfOriginSkuShipmentDetail, 1);

        mainCompositionOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "mainCompositionOriginArticleSkuShipmentDetail", "Состав", mainCompositionOriginArticleSku, skuShipmentDetail, 1);
        mainCompositionOriginSkuShipmentDetail = addJProp(intraAttributeGroup, true, "mainCompositionOriginSkuShipmentDetail", "Состав", mainCompositionOriginSku, skuShipmentDetail, 1);

        additionalCompositionOriginSkuShipmentDetail = addJProp(intraAttributeGroup, true, "additionalCompositionOriginSkuShipmentDetail", "Дополнительный состав", additionalCompositionOriginSku, skuShipmentDetail, 1);

        unitOfMeasureArticleSkuShipmentDetail = addJProp(idGroup, true, "unitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара (ИД)", unitOfMeasureArticleSku, skuShipmentDetail, 1);
        nameOriginUnitOfMeasureArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара", nameOrigin, unitOfMeasureArticleSkuShipmentDetail, 1);
        nameUnitOfMeasureArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара", baseLM.name, unitOfMeasureArticleSkuShipmentDetail, 1);

        // stock shipment detail
        stockShipmentDetail = addDProp(idGroup, "stockShipmentDetail", "Место хранения (ИД)", stock, shipmentDetail);
        barcodeStockShipmentDetail = addJProp(baseGroup, "barcodeStockShipmentDetail", "Штрих-код короба для транспортировки", baseLM.barcode, stockShipmentDetail, 1);

        routeFreightBoxShipmentDetail = addJProp(idGroup, "routeFreightBoxShipmentDetail", "Маршрут (ИД)", routeCreationFreightBoxFreightBox, stockShipmentDetail, 1);
        nameRouteFreightBoxShipmentDetail = addJProp(baseGroup, "nameRouteFreightBoxShipmentDetail", "Маршрут", baseLM.name, routeFreightBoxShipmentDetail, 1);
        nameRouteFreightBoxShipmentDetail.setFixedCharWidth(3);

        boxShipmentBoxShipmentDetail = addDProp(idGroup, "boxShipmentBoxShipmentDetail", "Поставка (ИД)", boxShipment, boxShipmentDetail);
        simpleShipmentSimpleShipmentDetail = addDProp(idGroup, "simpleShipmentSimpleShipmentDetail", "Поставка (ИД)", simpleShipment, simpleShipmentDetail);
        shipmentShipmentDetail = addCUProp(idGroup, "shipmentShipmentDetail", "Поставка (ИД)", boxShipmentBoxShipmentDetail, simpleShipmentSimpleShipmentDetail);
        sidShipmentShipmentDetail = addJProp(baseGroup, "sidShipmentShipmentDetail", "Поставка", sidDocument, shipmentShipmentDetail, 1);

        // supplier box shipmentDetail
        supplierBoxShipmentDetail = addDProp(idGroup, "supplierBoxShipmentDetail", "Короб поставщика (ИД)", supplierBox, boxShipmentDetail);
        setNotNull(supplierBoxShipmentDetail);
        sidSupplierBoxShipmentDetail = addJProp(baseGroup, "sidSupplierBoxShipmentDetail", "Номер короба поставщика", sidSupplierBox, supplierBoxShipmentDetail, 1);
        barcodeSupplierBoxShipmentDetail = addJProp(baseGroup, "barcodeSupplierBoxShipmentDetail", "Штрих-код короба поставщика", baseLM.barcode, supplierBoxShipmentDetail, 1);

        quantityShipmentDetail = addDProp(baseGroup, "quantityShipmentDetail", "Кол-во", DoubleClass.instance, shipmentDetail);

        userShipmentDetail = addDCProp(idGroup, "userShipmentDetail", "Пользователь (ИД)", baseLM.currentUser, true, is(shipmentDetail), 1);
        nameUserShipmentDetail = addJProp(baseGroup, "nameUserShipmentDetail", "Пользователь", baseLM.name, userShipmentDetail, 1);

        timeShipmentDetail = addTCProp(Time.DATETIME, "timeShipmentDetail", true, "Время ввода", quantityShipmentDetail);

        addBoxShipmentDetailBoxShipmentSupplierBoxStockSku = addJProp(true, "Добавить строку поставки",
                addAAProp(boxShipmentDetail, boxShipmentBoxShipmentDetail, supplierBoxShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail),
                1, 2, 3, 4, addCProp(DoubleClass.instance, 1));

        addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode = addJProp(true, "Добавить строку поставки",
                addBoxShipmentDetailBoxShipmentSupplierBoxStockSku,
                1, 2, 3, skuBarcodeObject, 4);

        addSimpleShipmentDetailSimpleShipmentStockSku = addJProp(true, "Добавить строку поставки",
                addAAProp(simpleShipmentDetail, simpleShipmentSimpleShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail),
                1, 2, 3, addCProp(DoubleClass.instance, 1));

        addSimpleShipmentSimpleShipmentDetailStockBarcode = addJProp(true, "Добавить строку поставки",
                addSimpleShipmentDetailSimpleShipmentStockSku,
                1, 2, skuBarcodeObject, 3);

        quantityArticle = addSGProp(baseGroup, "quantityArticle", true, "Оприходовано", quantityShipmentDetail, articleShipmentDetail, 1);
        quantityShipSku = addSGProp(baseGroup, "quantityShipSku", true, "Оприходовано", quantityShipmentDetail, skuShipmentDetail, 1);

        addConstraint(addJProp("Для артикула должна быть задана номенклатурная группа", baseLM.andNot1, quantityArticle, 1, categoryArticle, 1), false);
        addConstraint(addJProp("Для артикула должен быть задан пол", baseLM.andNot1, quantityArticle, 1, genderArticle, 1), false);
        addConstraint(addJProp("Для артикула должен быть задан тип одежды", baseLM.andNot1, quantityArticle, 1, typeFabricArticle, 1), false);

        addConstraint(addJProp("Для товара должен быть задан вес", baseLM.andNot1, quantityShipSku, 1, netWeightSku, 1), false);
        addConstraint(addJProp("Для товара должен быть задан страна", baseLM.andNot1, quantityShipSku, 1, countryOfOriginSku, 1), false);
        addConstraint(addJProp("Для товара должен быть задан состав", baseLM.andNot1, quantityShipSku, 1, mainCompositionOriginSku, 1), false);

        quantitySupplierBoxBoxShipmentStockSku = addSGProp(baseGroup, "quantitySupplierBoxBoxShipmentStockSku", true, "Кол-во оприход.", quantityShipmentDetail,
                supplierBoxShipmentDetail, 1, boxShipmentBoxShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        quantitySupplierBoxBoxShipmentSku = addSGProp(baseGroup, "quantitySupplierBoxBoxShipmentSku", "Кол-во оприход.", quantitySupplierBoxBoxShipmentStockSku,
                1, 2, 4);

        quantitySupplierBoxSku = addSGProp(baseGroup, "quantitySupplierBoxSku", "Кол-во оприход.", quantitySupplierBoxBoxShipmentStockSku, 1, 4);

        quantityDirectSupplierBoxSku = addJProp(baseGroup, "quantityDirectSupplierBoxSku", "Кол-во", baseLM.and1, quantityListSku, 1, 2, addJProp(is(directInvoice), boxInvoiceSupplierBox, 1), 1);

        quantitySupplierBox = addSGProp(baseGroup, "quantitySupplierBox", "Кол-во оприход.(короб)", quantitySupplierBoxSku, 1);

        diffListSupplierBoxSku = addJProp(baseLM.equals2, quantityDataListSku, 1, 2, quantitySupplierBoxSku, 1, 2);

        diffListSupplierBox = addJProp(baseLM.less2, quantityDataList, 1, quantitySupplierBox, 1);

        quantitySimpleShipmentStockSku = addSGProp(baseGroup, "quantitySimpleShipmentStockSku", true, "Кол-во оприход.", quantityShipmentDetail,
                simpleShipmentSimpleShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        quantityShipDimensionShipmentStockSku = addCUProp(baseGroup, "quantityShipDimensionShipmentStockSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                addJProp(baseLM.and1, quantitySimpleShipmentStockSku, 2, 3, 4, baseLM.equals2, 1, 2));

        quantityBoxInvoiceBoxShipmentStockSku = addSGProp(baseGroup, "quantityBoxInvoiceBoxShipmentStockSku", true, "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                boxInvoiceSupplierBox, 1, 2, 3, 4);

        invoicedSimpleInvoiceSimpleShipmentStockSku = addJProp("invoicedSimpleInvoiceSimpleShipmentStockSku", "Кол-во оприх.", and(false, false, false, false), quantityDocumentSku, 1, 4, inInvoiceShipment, 1, 2, is(simpleInvoice), 1, is(simpleShipment), 2, is(stock), 3);
        invoicedSimpleInvoiceSimpleShipmentStockArticleComposite = addJProp("invoicedSimpleInvoiceSimpleShipmentStockArticleComposite", "Кол-во оприх.", and(false, false, false), quantitySimpleInvoiceArticle, 1, 4, inInvoiceShipment, 1, 2, is(simpleShipment), 2, is(stock), 3);
        invoicedSimpleInvoiceSimpleShipmentStockItem = addJProp("invoicedSimpleInvoiceSimpleShipmentStockItem", "Кол-во оприх.", invoicedSimpleInvoiceSimpleShipmentStockArticleComposite, 1, 2, 3, articleCompositeItem, 4);

        quantitySkuSimpleInvoiceSimpleShipmentStockSku = addPGProp(baseGroup, "quantityDataSimpleInvoiceSimpleShipmentStockSku", true, 0, true, "Кол-во оприход.",
                invoicedSimpleInvoiceSimpleShipmentStockSku,
                quantitySimpleShipmentStockSku, 2, 3, 4);

        quantitySimpleShipmentStockItem = addJProp("quantitySimpleShipmentStockItem", baseLM.and1, quantitySimpleShipmentStockSku, 1, 2, 3, is(item), 3);

        quantityArticleSimpleInvoiceSimpleShipmentStockItem = addPGProp(baseGroup, "quantitySimpleInvoiceSimpleShipmentStockItem", true, 0, true, "Кол-во оприход.",
                invoicedSimpleInvoiceSimpleShipmentStockItem,
                quantitySimpleShipmentStockItem, 2, 3, 4);

        quantitySimpleInvoiceSimpleShipmentStockSku = addSUProp(baseGroup, "quantitySkuSimpleInvoiceSimpleShipmentStockSku", "Кол-во оприход.", Union.SUM, quantityArticleSimpleInvoiceSimpleShipmentStockItem, quantitySkuSimpleInvoiceSimpleShipmentStockSku);

        quantityInvoiceShipmentStockSku = addCUProp(baseGroup, "quantityInvoiceShipmentStockSku", "Кол-во оприход.",
                quantityBoxInvoiceBoxShipmentStockSku, quantitySimpleInvoiceSimpleShipmentStockSku);

        quantityInvoiceStockSku = addSGProp(baseGroup, "quantityInvoiceStockSku", true, "Кол-во оприход.", quantityInvoiceShipmentStockSku, 1, 3, 4);

        quantityInvoiceSku = addSGProp(baseGroup, "quantityInvoiceSku", true, true, "Кол-во оприход.", quantityInvoiceStockSku, 1, 3);

        quantityInvoice = addSGProp(baseGroup, "quantityInvoice", true, true, "Кол-во оприход.", quantityInvoiceSku, 1);

        diffDocumentInvoiceSku = addJProp(baseLM.equals2, quantityDocumentSku, 1, 2, quantityInvoiceSku, 1, 2);

        priceInInvoiceStockSku = addJProp(baseGroup, "priceInInvoiceStockSku", false, "Цена входная", baseLM.and1,
                priceRateDocumentSku, 1, 3, quantityInvoiceStockSku, 1, 2, 3);
        RRPInInvoiceStockSku = addJProp(baseGroup, "RRPInInvoiceStockSku", false, "Цена рекомендованная", baseLM.and1,
                RRPRateDocumentSku, 1, 3, quantityInvoiceStockSku, 1, 2, 3);
        contractInInvoiceStockSku = addJProp(baseGroup, "contractInInvoiceStockSku", false, "Контракт (ИД)", baseLM.and1,
                contractInvoice, 1, quantityInvoiceStockSku, 1, 2, 3);

        priceInInvoiceShipmentStockSku = addJProp("priceInInvoiceShipmentStockSku", "Цена входная", baseLM.and1, priceInInvoiceStockSku, 1, 3, 4, inInvoiceShipment, 1, 2);
        RRPInInvoiceShipmentStockSku = addJProp("RRPInInvoiceShipmentStockSku", "Цена рекомендованная", baseLM.and1, RRPInInvoiceStockSku, 1, 3, 4, inInvoiceShipment, 1, 2);
        contractInInvoiceShipmentStockSku = addJProp("contractInInvoiceShipmentStockSku", "Контракт (ИД)", baseLM.and1, contractInInvoiceStockSku, 1, 3, 4, inInvoiceShipment, 1, 2);

        priceInShipmentStockSku = addMGProp(baseGroup, "priceShipmentStockSku", true, "Цена входная", priceInInvoiceShipmentStockSku, 2, 3, 4);
        RRPInShipmentStockSku = addMGProp(baseGroup, "RRPShipmentStockSku", true, "Цена рекомендованная", RRPInInvoiceShipmentStockSku, 2, 3, 4);
        priceInShipmentDetail = addJProp(baseGroup, "priceInShipmentDetail", "Цена входная", priceInShipmentStockSku, shipmentShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        contractInShipmentStockSku = addMGProp(baseGroup, "contractInShipmentStockSku", true, "Контракт (ИД)", contractInInvoiceShipmentStockSku, 2, 3, 4);

        quantityShipDimensionStock = addSGProp(baseGroup, "quantityShipDimensionStock", "Всего оприход.", quantityShipDimensionShipmentStockSku, 1, 3);

        quantityShipDimensionShipmentSku = addSGProp(baseGroup, "quantityShipDimensionShipmentSku", "Оприход. (короб)", quantityShipDimensionShipmentStockSku, 1, 2, 4);

        zeroQuantityListSku = addSUProp(baseGroup, "zeroQuantityListSku", "кол-во", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, list, sku), quantityListSku);
        zeroQuantityShipDimensionShipmentSku = addSUProp(baseGroup, "zeroQuantityShipDimensionShipmentSku", "кол-во", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipDimension, shipment, sku), quantityShipDimensionShipmentSku);

        diffListShipSku = addJProp(baseLM.diff2, zeroQuantityListSku, 1, 3, zeroQuantityShipDimensionShipmentSku, 1, 2, 3);

        quantityShipmentStockSku = addSGProp(baseGroup, "quantityShipmentStockSku", true, "Кол-во оприход.", quantityShipDimensionShipmentStockSku, 2, 3, 4);

        quantityShipmentStock = addSGProp(baseGroup, "quantityShipmentStock", "Всего оприход.", quantityShipmentStockSku, 1, 2);

        quantityShipmentSku = addSGProp(baseGroup, "quantityShipmentSku", "Оприход. (пост.)", quantityShipmentStockSku, 1, 3);

        quantityShipmentedBetweenDateSku = addSGProp(baseGroup, "quantityShipmentedBetweenDateSku", "Фактический приход за период", addJProp(baseLM.and1, quantityShipmentSku, 1, 2, addJProp(baseLM.betweenDates, dateArrivalShipment, 1, object(DateClass.instance), 3, object(DateClass.instance), 4), 1, 2, 3, 4), 2, 3, 4);
        quantityShipmentedBetweenDateBrandSupplier = addSGProp(baseGroup, "quantityShipmentedBetweenDateBrandSupplier", "Фактический приход за период", quantityShipmentedBetweenDateSku, brandSupplierArticleSku, 1, 2, 3);

        quantityShipmentedSku = addSGProp(baseGroup, "quantityShipmentedSku", "Фактический приход", quantityShipmentSku, 2);

        // выполнение заказа
        orderedOrderShipmentSku = addJProp(baseLM.and1, quantityDocumentSku, 1, 3, inOrderShipment, 1, 2);

        quantityOrderShipmentSku = addPGProp(baseGroup, "quantityOrderShipmentSku", true, 0, true, "Кол-во по заказу/поставке (расч.)",
                orderedOrderShipmentSku,
                quantityShipmentSku, 2, 3);

        shipmentedOrderSku = addSGProp(baseGroup, "shipmentedOrderSku", "Прислано по заказу", quantityOrderShipmentSku, 1, 3);

        shipmentedAtTermOrderSku = addSGProp(baseGroup, "shipmentedAtTermOrderSku", "Прислано в срок", addJProp(baseLM.and1, quantityOrderShipmentSku, 1, 2, 3, addJProp(baseLM.betweenDates, dateArrivalShipment, 2, dateFromOrderArticleSku, 1, 3, dateToOrderArticleSku, 1, 3), 1, 2, 3), 1, 3);

        quantityShipment = addSGProp(baseGroup, "quantityShipment", true, "Оприходовано", quantityShipmentSku, 1);

        zeroQuantityShipmentSku = addSUProp(baseGroup, "zeroQuantityShipmentSku", "кол-во", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipment, sku), quantityShipmentSku);
        zeroInvoicedShipmentSku = addSUProp(baseGroup, "zeroInvoicedShipmentSku", "кол-во", Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipment, sku), invoicedShipmentSku);
        diffShipmentSku = addJProp(baseLM.diff2, zeroQuantityShipmentSku, 1, 2, zeroInvoicedShipmentSku, 1, 2);

        quantityStockSku = addSGProp(baseGroup, "quantityStockSku", true, true, "Оприход. в короб для транспортировки", quantityShipmentStockSku, 2, 3);

        quantityFreightUnitSku = addCUProp(baseGroup, "quantityFreightUnitSku", "Кол-во в коробе", quantityDirectSupplierBoxSku, quantityStockSku);

        quantityStock = addSGProp(baseGroup, "quantityStock", "Кол-во оприход.", quantityStockSku, 1);

        quantityStockArticle = addSGProp(baseGroup, "quantityStockArticle", true, "Кол-во по артикулу", quantityStockSku, 1, articleSku, 2);

        freightShippedFreightBox = addJProp(baseGroup, "freightShippedFreightBox", is(freightShipped), freightFreightBox, 1);

        sumInInvoiceStockSku = addJProp(baseGroup, "sumInInvoiceStockSku", "Сумма в коробе", baseLM.multiplyDouble2, addJProp(baseLM.andNot1, quantityInvoiceStockSku, 1, 2, 3, freightShippedFreightBox, 2), 1, 2, 3, priceInInvoiceStockSku, 1, 2, 3);

        sumStockedSku = addSGProp(baseGroup, "sumStockedSku", true, "Сумма на приемке", sumInInvoiceStockSku, 3);
        quantityAllSku = addSGProp(baseGroup, "quantityAllSku", true, "Кол-во на приемке", quantityStockSku, 2);
        quantityStockedSku = addSGProp(baseGroup, "quantityStockedSku", true, "Кол-во на приемке", addJProp(baseLM.andNot1, quantityStockSku, 1, 2, freightShippedFreightBox, 1), 2);

        quantitySku = addSUProp(baseGroup, "quantitySku", "Кол-во", Union.SUM, quantityStockedSku, quantityDirectInvoicedSku);
        sumSku = addSUProp(baseGroup, "sumSku", "Сумма", Union.SUM, sumStockedSku, sumDirectInvoicedSku);

        quantityStockBrandSupplier = addSGProp(baseGroup, "quantityStockBrandSupplier", true, "Кол-во по бренду",
                addJProp(baseLM.andNot1, quantityStockArticle, 1, 2, freightShippedFreightBox, 1), 1, brandSupplierArticle, 2);

        quantityFreightUnitBrandSupplier = addSGProp(baseGroup, "quantityFreightUnitBrandSupplier", "Кол-во по бренду", quantityFreightUnitSku, 1, brandSupplierArticleSku, 2);

        quantityPalletSku = addSGProp(baseGroup, "quantityPalletSku", true, "Оприход. (пал.)", quantityStockSku, palletFreightBox, 1, 2);

        quantityPalletBrandSupplier = addSGProp(baseGroup, "quantityPalletBrandSupplier", true, "Кол-во по бренду", quantityStockBrandSupplier, palletFreightBox, 1, 2);
        quantityAllPalletsBrandSupplier = addSGProp(baseGroup, "quantityAllPalletBrandSupplier", true, "Кол-во по бренду", quantityPalletBrandSupplier, 2);

        quantityBrandSupplier = addSUProp(baseGroup, "quantityBrandSupplier", "Кол-во по бренду", Union.SUM, quantityAllDocumentsBrandSupplier, quantityAllPalletsBrandSupplier);

        quantityShipmentPallet = addSGProp(baseGroup, "quantityShipmentPallet", "Всего оприход. (паллета)", quantityShipmentStock, 1, palletFreightBox, 2);

        quantityShipmentFreight = addSGProp(baseGroup, "quantityShipmentFreight", true, true, "Всего оприход. (фрахт)", quantityShipmentPallet, 1, freightPallet, 2);

        quantityShipmentFreightSku = addJProp(baseGroup, "quantityShipmentFreightSku", "Кол-во по данным поставщика", baseLM.and1, invoicedShipmentSku, 1, 3, quantityShipmentFreight, 1, 2);
        quantityShipmentedFreightArticle = addSGProp(baseGroup, "quantityShipmentedFreightArticle", "Кол-во по данным поставщика", quantityShipmentFreightSku, 2, articleSku, 3);
        quantityShipmentedFreightBrandSupplier = addSGProp(baseGroup, "quantityShipmentedFreightBrandSupplier", "Кол-во по данным поставщика", quantityShipmentedFreightArticle, 1, brandSupplierArticle, 2);

        quantityShipmentArticle = addSGProp(baseGroup, "quantityShipmentArticle", "Всего оприход. (артикул)", quantityShipmentSku, 1, articleSku, 2);
        quantityShipmentArticleSize = addSGProp(baseGroup, "quantityShipmentArticleSize", "Всего оприход. (артикул-размер)", quantityShipmentSku, 1, articleSku, 2, sizeSupplierItem, 2);
        quantityShipmentArticleColor = addSGProp(baseGroup, "quantityShipmentArticleColor", "Всего оприход. (артикул-цвет)", quantityShipmentSku, 1, articleSku, 2, colorSupplierItem, 2);
        quantityShipmentArticleColorSize = addSGProp(baseGroup, "quantityShipmentArticleColorSize", "Всего оприход. (артикул-цвет-размер)", quantityShipmentSku, 1, articleSku, 2, colorSupplierItem, 2, sizeSupplierItem, 2);
        quantityShipmentSize = addSGProp(baseGroup, "quantityShipmentSize", "Всего оприход. (размер)", quantityShipmentArticleSize, 1, 3);

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
        quantityCreationStamp = addDProp(baseGroup, "quantityCreationStamp", "Количество", IntegerClass.instance, creationStamp);
        seriesOfCreationStamp = addDProp(baseGroup, "seriesOfCreationStamp", "Серия марки", StringClass.get(2), creationStamp);
        firstNumberCreationStamp = addDProp(baseGroup, "firstNumberCreationStamp", "Номер с", StringClass.get(8), creationStamp);
        lastNumberCreationStamp = addDProp(baseGroup, "lastNumberCreationStamp", "Номер по", StringClass.get(8), creationStamp);
        dateOfCreationStamp = addDProp(baseGroup, "dateOfCreationStamp", "Дата", DateClass.instance, creationStamp);

        creationStampStamp = addDProp(idGroup, "creationStampStamp", "Операция (ИД)", creationStamp, stamp);
        sidStamp = addDProp(baseGroup, "sidStamp", "Контрольная марка", StringClass.get(100), stamp);
        seriesOfStamp = addJProp(baseGroup, "seriesOfStamp", "Серия марки", seriesOfCreationStamp, creationStampStamp, 1);
        dateOfStamp = addJProp(baseGroup, "dateOfStamp", "Дата марки", dateOfCreationStamp, creationStampStamp, 1);

        stampShipmentDetail = addDProp("stampSkuShipmentDetail", "Контрольная марка", stamp, shipmentDetail);
        necessaryStampCategory = addDProp(baseGroup, "necessaryStampCategory", "Нужна марка", LogicalClass.instance, category);
        necessaryStampSkuShipmentDetail = addJProp("necessaryStampSkuShipmentDetail", necessaryStampCategory, categoryArticleSkuShipmentDetail, 1);
        sidStampShipmentDetail = addJProp(intraAttributeGroup, "sidStampShipmentDetail", "Контрольная марка",  sidStamp, stampShipmentDetail, 1);
        seriesOfStampShipmentDetail = addJProp(intraAttributeGroup, "seriesOfStampShipmentDetail", "Серия контрольной марки",  seriesOfStamp, stampShipmentDetail, 1);
        hideSidStampShipmentDetail = addHideCaptionProp(privateGroup, "Контрольная марка", sidStampShipmentDetail, necessaryStampSkuShipmentDetail);
        hideSeriesOfStampShipmentDetail = addHideCaptionProp(privateGroup, "Серия контрольной марки", seriesOfStampShipmentDetail, necessaryStampSkuShipmentDetail);
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

        sumShipmentRouteSku = addJProp("sumShipmentRouteSku", "Сумма", baseLM.multiplyDouble2, invoicedShipmentRouteSku, 1, 2, 3, priceShipmentSku, 1, 3);
        sumShipmentRoute = addSGProp("sumShipmentRoute", "Сумма (ожид.)", sumShipmentRouteSku, 1, 2);

        sumShipmentSku = addJProp("sumShipmentSku", "Сумма (оприход.)", baseLM.multiplyDouble2, priceShipmentSku, 1, 2, quantityShipmentSku, 1, 2);

        sumShipmentArticleColor = addSGProp("sumShipmentArticleColor", "Сумма (оприход.)", sumShipmentSku, 1, articleSku, 2, colorSupplierItem, 2);
        sumShipment = addSGProp("sumShipment", "Сумма (ожид.)", sumShipmentRoute, 1);

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

        descriptionFreight = addDProp(baseGroup, "descriptionFreight", "Описание машины", StringClass.get(50), freight);

        freightTypeFreight = addDProp(idGroup, "freightTypeFreight", "Тип машины (ИД)", freightType, freight);
        nameFreightTypeFreight = addJProp(baseGroup, "nameFreightTypeFreight", "Тип машины", baseLM.name, freightTypeFreight, 1);

        tonnageFreight = addJProp(baseGroup, "tonnageFreight", "Тоннаж (кг)", tonnageFreightType, freightTypeFreight, 1);
        tonnageDataFreight = addDProp(baseGroup, "tonnageDataFreight", "Тоннаж (кг)", DoubleClass.instance, freight);
        palletCountFreight = addJProp(baseGroup, "palletCountFreight", "Кол-во паллет", palletCountFreightType, freightTypeFreight, 1);
        palletCountDataFreight = addDProp(baseGroup, "palletCountDataFreight", "Кол-во паллет", DoubleClass.instance, freight);
        volumeFreight = addJProp(baseGroup, "volumeFreight", "Объём", volumeFreightType, freightTypeFreight, 1);
        volumeDataFreight = addDProp(baseGroup, "volumeDataFreight", "Объем груза", DoubleClass.instance, freight);

        currencyFreight = addDProp(idGroup, "currencyFreight", "Валюта (ИД)", currency, freight);
        nameCurrencyFreight = addJProp(baseGroup, "nameCurrencyFreight", "Валюта", baseLM.name, currencyFreight, 1);
        nameCurrencyFreight.setFixedCharWidth(10);
        symbolCurrencyFreight = addJProp(baseGroup, "symbolCurrencyFreight", "Валюта", symbolCurrency, currencyFreight, 1);

        sumFreightFreight = addDProp(baseGroup, "sumFreightFreight", "Стоимость", DoubleClass.instance, freight);
        insuranceFreight = addDProp(baseGroup, "insuranceFreight", "Страховка", DoubleClass.instance, freight);
        insuranceFreightBrandSupplier = addDProp(baseGroup, "insuranceFreightBrandSupplier", "Страховка за бренд", DoubleClass.instance, freight, brandSupplier);
        insuranceFreightBrandSupplierArticle = addJProp(baseGroup, "insuranceFreightBrandSupplierArticle", "Страховка за бренд", insuranceFreightBrandSupplier, 1, brandSupplierArticle, 2);
        insuranceFreightBrandSupplierSku = addJProp(baseGroup, "insuranceFreightBrandSupplierSku", "Страховка за бренд", insuranceFreightBrandSupplier, 1, brandSupplierArticleSku, 2);

        routeFreight = addDProp(idGroup, "routeFreight", "Маршрут (ИД)", route, freight);
        nameRouteFreight = addJProp(baseGroup, "nameRouteFreight", "Маршрут", baseLM.name, routeFreight, 1);
        nameRouteFreight.setFixedCharWidth(8);

        exporterFreight = addDProp(idGroup, "exporterFreight", "Экспортер (ИД)", exporter, freight);
        setNotNull(exporterFreight);
        nameOriginExporterFreight = addJProp(baseGroup, "nameOriginExporterFreight", "Экспортер", nameOrigin, exporterFreight, 1);
        nameExporterFreight = addJProp(baseGroup, "nameExporterFreight", "Экспортер", baseLM.name, exporterFreight, 1);
        addressOriginExporterFreight = addJProp(baseGroup, "addressOriginExporterFreight", "Адрес", addressOriginSubject, exporterFreight, 1);
        addressExporterFreight = addJProp(baseGroup, "addressExporterFreight", "Адрес", addressSubject, exporterFreight, 1);

        inInvoiceFreight = addDProp(baseGroup, "inInvoiceFreight", "Вкл.", LogicalClass.instance, invoice, freight);
        netWeightInvoicedFreight = addSGProp(baseGroup, "netWeightInvoicedFreight", "Вес инвойсов", addJProp(baseLM.and1, netWeightDocument, 1, inInvoiceFreight, 1, 2), 2);

        dateArrivalFreight = addDProp(baseGroup, "dateArrivalFreight", "Дата поступления на СВХ", DateClass.instance, freight);

        dateImporterFreightTypeInvoice = addDProp(baseGroup, "dateImporterFreightTypeInvoice", "Дата инвойса", DateClass.instance, importer, freight, typeInvoice);
        dateImporterFreight = addMGProp(baseGroup, "dateImporterFreight", "Дата инвойса", dateImporterFreightTypeInvoice, 1, 2);

        dateShipmentImporterFreightTypeInvoice = addDProp(baseGroup, "dateShipmentImporterFreightTypeInvoice", "Дата поставки", DateClass.instance, importer, freight, typeInvoice);
        contractImporterFreight = addDProp(idGroup, "contractImporterFreight", "Договор (ИД)", contract, importer, freight);
        //nameContractImporterFreight = addJProp(baseGroup, "nameContractImporterFreight", "Договор", baseLM.name, contractImporterFreight, 1, 2);

        addConstraint(addJProp("Импортер договора должен соответствовать импортеру исходящего инвойса", baseLM.diff2, 1, addJProp(subjectContract, contractImporterFreight, 1, 2), 1, 2), true);
        addConstraint(addJProp("Продавец по договору должен соответствовать экспортеру исходящего инвойса", baseLM.diff2, exporterFreight, 2, addJProp(sellerContract, contractImporterFreight, 1, 2), 1, 2), true);

        sidContractImporterFreight = addJProp(baseGroup, "sidContractImporterFreight", "Договор", sidContract, contractImporterFreight, 1, 2);
        dateContractImporterFreight = addJProp(baseGroup, "dateContractImporterFreight", "Дата договора", dateContract, contractImporterFreight, 1, 2);
        conditionShipmentContractImporterFreight = addJProp(baseGroup, "conditionShipmentContractImporterFreight", "Условие поставки", conditionShipmentContract, contractImporterFreight, 1, 2);
        conditionPaymentContractImporterFreight = addJProp(baseGroup, "conditionPaymentContractImporterFreight", "Условие оплаты", conditionPaymentContract, contractImporterFreight, 1, 2);

        quantityPalletShipmentBetweenDate = addSGProp(baseGroup, "quantityPalletShipmentBetweenDate", "Кол-во паллет по поставкам за интервал",
                addJProp(baseLM.and1, quantityPalletShipment, 1, addJProp(baseLM.between, baseLM.date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);
        quantityPalletFreightBetweenDate = addSGProp(baseGroup, "quantityPalletFreightBetweenDate", "Кол-во паллет по фрахтам за интервал",
                addJProp(baseLM.and1, palletCountFreight, 1, addJProp(baseLM.between, baseLM.date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);

        freightBoxNumberPallet = addSGProp(baseGroup, "freightBoxNumberPallet", "Кол-во коробов", addCProp(IntegerClass.instance, 1, freightBox), palletFreightBox, 1);

        addConstraint(addJProp("Для паллеты должен быть задан вес бруто", baseLM.andNot1, freightBoxNumberPallet, 1, grossWeightPallet, 1), false);

        addConstraint(addJProp("Маршрут паллеты должен совпадать с маршрутом фрахта", baseLM.diff2,
                routeCreationPalletPallet, 1, addJProp(routeFreight, freightPallet, 1), 1), true);

        //addConstraint(addJProp("Маршрут короба должен совпадать с маршрутом паллеты", baseLM.diff2,
        //        routeCreationFreightBoxFreightBox, 1, addJProp(routeCreationPalletPallet, palletFreightBox, 1), 1), true);

        palletNumberFreight = addSUProp(baseGroup, "palletNumberFreight", true, "Кол-во присоединённых паллет", Union.SUM,
                addSGProp(addCProp(IntegerClass.instance, 1, pallet), freightPallet, 1),
                addSGProp(palletNumberDirectInvoice, freightDirectInvoice, 1));

        freightBoxNumberFreight = addSGProp(baseGroup, "freightBoxNumberFreight", "Кол-во присоединённых коробов", freightBoxNumberPallet, freightPallet, 1);

        diffPalletFreight = addJProp(baseLM.greater2, palletNumberFreight, 1, palletCountDataFreight, 1);

        freightSupplierBox = addJProp(baseGroup, "freightSupplierBox", "Фрахт (ИД)", freightDirectInvoice, boxInvoiceSupplierBox, 1);
        freightFreightUnit = addCUProp(idGroup, "freightFreightUnit", true, "Фрахт (ИД)", freightFreightBox, freightSupplierBox);

        stockNumberFreightBrandSupplier = addSGProp(baseGroup, "stockNumberFreightBrandSupplier", "Кол-во коробов по бренду", addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), quantityFreightUnitBrandSupplier, 1, 2), freightFreightUnit, 1, 2);

        importerSupplierBox = addJProp(baseGroup, "importerSupplierBox", "Импортер (ИД)", importerDirectInvoice, boxInvoiceSupplierBox, 1);

        // Кол-во для импортеров
        // здесь не соблюдается policy, что входы совпадают с именем
        quantityInvoiceFreightUnitSku = addCUProp(baseGroup, "quantityInvoiceFreightUnitSku", "Кол-во",
                quantityInvoiceStockSku,
                addJProp(baseLM.and1, quantityListSku, 2, 3, addJProp(baseLM.equals2, 1, boxInvoiceSupplierBox, 2), 1, 2));

        priceRateSupplierBoxSku = addJProp("priceRateSupplierBoxSku", "Цена входная", priceRateDocumentSku, boxInvoiceSupplierBox, 1, 2);
        RRPRateSupplierBoxSku = addJProp("RRPRateSupplierBoxSku", "Цена рекомендованная", RRPRateDocumentSku, boxInvoiceSupplierBox, 1, 2);

        priceInInvoiceFreightUnitSku = addCUProp(baseGroup, "priceInInvoiceFreightUnitSku", "Цена входная",
                priceInInvoiceStockSku,
                addJProp(baseLM.and1, priceRateDocumentSku, 1, 3, addJProp(baseLM.equals2, 1, boxInvoiceSupplierBox, 2), 1, 2));

        routeFreightFreightBox = addJProp(idGroup, "routeFreightFreightBox", "Маршрут (ИД)", routeFreight, freightFreightBox, 1);
        importerShipmentRoute = addDProp("importerShipmentRoute", "Импортер (ИД)", importer, shipment, route);

        addConstraint(addJProp("Для маршрута поставки должен быть задан импортер", baseLM.andNot1, percentShipmentRoute, 1, 2, importerShipmentRoute, 1, 2), false);

        nameImporterShipmentRoute = addJProp("nameImporterShipmentRoute", "Импортер", baseLM.name, importerShipmentRoute, 1, 2);
        importerShipmentFreightBox = addJProp("importerShipmentFreightBox", "Импортер (ИД)", importerShipmentRoute, 1, routeFreightFreightBox, 2);

        importerShipmentFreight = addJProp(idGroup, true, "importerShipmentFreight", "Импортёр (ИД)", importerShipmentRoute, 1, routeFreight, 2);
        nameImporterShipmentFreight = addJProp(baseGroup, "nameImporterShipmentFreight", "Импортёр", baseLM.name, importerShipmentFreight, 1, 2);

        quantityImporterStockSku = addSGProp(baseGroup, "quantityImporterStockSku", true, "Кол-во", quantityShipmentStockSku, importerShipmentFreightBox, 1, 2, 2, 3);
        // quantityImporterStockSku = addSGProp(baseGroup, "quantityImporterStockSku", true, "Кол-во", quantityInvoiceStockSku, importerInvoice, 1, 2, 3);
        quantityImporterStockArticle = addSGProp(baseGroup, "quantityImporterStockArticle", true, true, "Кол-во", quantityImporterStockSku, 1, 2, articleSku, 3);

        quantityImporterStockTypeInvoice = addSGProp(baseGroup, "quantityImporterStockTypeInvoice", "Кол-во", quantityImporterStockArticle, 1, 2, addJProp(typeInvoiceFreightArticle, freightFreightUnit, 2, 3), 1, 2, 3);
        quantityImporterStock = addSGProp(baseGroup, "quantityImporterStock", "Кол-во", quantityImporterStockSku, 1, 2);

        quantityProxyImporterFreightSku = addSGProp(baseGroup, "quantityProxyImporterFreightSku", true, true, "Кол-во (из приёмки)", quantityImporterStockSku, 1, freightFreightUnit, 2, 3);
        quantityDirectImporterFreightSku = addSGProp(baseGroup, "quantityDirectImporterFreightSku", true, true, "Кол-во (напрямую)", quantityListSku, importerSupplierBox, 1, freightFreightUnit, 1, 2);
        quantityImporterFreightSku = addSUProp(baseGroup, "quantityImporterFreightSku", true, "Кол-во", Union.SUM, quantityProxyImporterFreightSku, quantityDirectImporterFreightSku);

        quantityFreightArticle = addSGProp(baseGroup, "quantityFreightArticle", "Кол-во отгруженное с STX", quantityImporterFreightSku, 2, articleSku, 3);
        quantityFreightBrandSupplier = addSGProp(baseGroup, "quantityFreightBrandSupplier", "Кол-во отгруженное с STX", quantityImporterFreightSku, 2, brandSupplierArticleSku, 3);
        quantityFreightSupplier = addSGProp(baseGroup, "quantityFreightSupplier", true, "Кол-во", quantityFreightBrandSupplier, 1, supplierBrandSupplier, 2);

        quantityFreightSku = addSGProp(baseGroup, "quantityFreightSku", true, true, "Кол-во", quantityImporterFreightSku, 2, 3);
        quantityDirectFreightSku = addSGProp(baseGroup, "quantityDirectFreightSku", true, true, "Кол-во (напрямую)", quantityDirectImporterFreightSku, 2, 3);
        quantityDirectImporterFreightSupplier = addSGProp(baseGroup, "quantityDirectImporterFreightSupplier", true, true, "Кол-во (напрямую)", quantityDirectImporterFreightSku, 1, 2, supplierArticleSku, 3);

        quantityFreightedBetweenDateSku = addSGProp(baseGroup, "quantityFreightedBetweenDateSku", "Кол-во отгруженное за период", addJProp(and(false, false), quantityFreightSku, 1, 2, is(freightShipped), 1, addJProp(baseLM.betweenDates, baseLM.date, 1, object(DateClass.instance), 3, object(DateClass.instance), 4), 1, 2, 3, 4), 2, 3, 4);
        quantityFreightedSku = addSGProp(baseGroup, "quantityFreightedSku", "Кол-во отгруженное", quantityFreightSku, 2);

        balanceSku = addDUProp(baseGroup, "balanceSku", "Остатки на складе", quantityShipmentedSku, quantityFreightedSku);

        quantityFreightCategory = addSGProp(baseGroup, "quantityFreightCategory", true, true, "Кол-во", quantityFreightSku, 1, categoryArticleSku, 2);

        quantityFreightCategoryGenderCompositionTypeFabric = addSGProp(baseGroup, "quantityFreightCategoryGenderCompositionTypeFabric", "Кол-во", quantityFreightSku, 1, categoryArticleSku, 2, genderArticleSku, 2, mainCompositionOriginSku, 2, typeFabricArticleSku, 2);

        customCategory10FreightSku = addDProp(idGroup, "customCategory10FreightSku", "ТН ВЭД (ИД)", customCategory10, freight, sku);
        customCategory10FreightSku.setDerivedForcedChange(true, addJProp(baseLM.and1, customCategory10Sku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        sidCustomCategory10FreightSku = addJProp(baseGroup, "sidCustomCategory10FreightSku", "ТН ВЭД", sidCustomCategory10, customCategory10FreightSku, 1, 2);
        addConstraint(addJProp("Для SKU должен быть задан ТН ВЭД", and(true, false), is(freightChanged), 1, customCategory10FreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        subCategoryFreightSku = addDProp(idGroup, "subCategoryFreightSku", "Дополнительное деление (ИД)", subCategory, freight, sku);
        subCategoryFreightSku.setDerivedForcedChange(true, addJProp(baseLM.and1, subCategorySku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        nameSubCategoryFreightSku = addJProp(baseGroup, "nameSubCategoryFreightSku", "Дополнительное деление", nameSubCategory, subCategoryFreightSku, 1, 2);
        addConstraint(addJProp("Для SKU должно быть задано дополнительное деление", and(true, false, false), is(freightChanged), 1, subCategoryFreightSku, 1, 2, diffCountRelationCustomCategory10FreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        customCategory6FreightSku = addJProp(idGroup, "customCategory6FreightSku", "ТН ВЭД", customCategory6CustomCategory10, customCategory10FreightSku, 1, 2);

        quantityProxyImporterFreightCustomCategory6Category = addSGProp(baseGroup, "quantityProxyImporterFreightCustomCategory6Category", "Кол-во", quantityProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        quantityProxyImporterFreightCustomCategory6 = addSGProp(baseGroup, "quantityProxyImporterFreightCustomCategory6", "Кол-во", quantityProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);
        quantityDirectImporterFreightSupplierCustomCategory6Category = addSGProp(baseGroup, "quantityDirectImporterFreightSupplierCustomCategory6Category", "Кол-во", quantityDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        quantityDirectImporterFreightSupplierCustomCategory6 = addSGProp(baseGroup, "quantityDirectImporterFreightSupplierCustomCategory6", "Кол-во", quantityDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3);

        mainCompositionOriginFreightSku = addDProp(baseGroup, "mainCompositionOriginFreightSku", "Состав", COMPOSITION_CLASS, freight, sku);
        mainCompositionOriginFreightSku.setDerivedForcedChange(true, addJProp(baseLM.and1, mainCompositionOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        additionalCompositionOriginFreightSku = addDProp(baseGroup, "additionalCompositionOriginFreightSku", "Доп. состав", COMPOSITION_CLASS, freight, sku);
        additionalCompositionOriginFreightSku.setDerivedForcedChange(true, addJProp(baseLM.and1, additionalCompositionOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        additionalCompositionOriginFreightSku.property.preferredCharWidth = 40;
        additionalCompositionOriginFreightSku.property.minimumCharWidth = 20;

        translationMainCompositionSku = addJProp(actionGroup, true, "translationMainCompositionSku", "Перевод состава", addTAProp(mainCompositionOriginSku, mainCompositionSku), dictionaryComposition, 1);
        translationAdditionalCompositionSku = addJProp(actionGroup, true, "translationAdditionalCompositionSku", "Перевод доп. состава", addTAProp(additionalCompositionOriginSku, additionalCompositionSku), dictionaryComposition, 1);

        mainCompositionFreightSku = addDProp(baseGroup, "mainCompositionFreightSku", "Состав (перевод)", COMPOSITION_CLASS, freight, sku);
        mainCompositionFreightSku.setDerivedForcedChange(true, addJProp(baseLM.and1, mainCompositionSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        addConstraint(addJProp("Для SKU должен быть задан состав", and(true, false), is(freightChanged), 1, mainCompositionFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        additionalCompositionFreightSku = addDProp(baseGroup, "additionalCompositionFreightSku", "Доп. состав (перевод)", COMPOSITION_CLASS, freight, sku);
        additionalCompositionFreightSku.setDerivedForcedChange(true, addJProp(baseLM.and1, additionalCompositionSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        additionalCompositionFreightSku.property.preferredCharWidth = 40;
        additionalCompositionFreightSku.property.minimumCharWidth = 20;

        translationMainCompositionFreightSku = addJProp(actionGroup, true, "translationMainCompositionFreightSku", "Перевод состава", addTAProp(mainCompositionOriginFreightSku, mainCompositionFreightSku), dictionaryComposition, 1, 2);
        translationAdditionalCompositionFreightSku = addJProp(actionGroup, true, "translationAdditionalCompositionFreightSku", "Перевод доп. состава", addTAProp(additionalCompositionOriginFreightSku, additionalCompositionFreightSku), dictionaryComposition, 1, 2);

        countryOfOriginFreightSku = addDProp(idGroup, "countryOfOriginFreightSku", "Страна (ИД)", baseLM.country, freight, sku);
        countryOfOriginFreightSku.setDerivedForcedChange(true, addJProp(baseLM.and1, countryOfOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        addConstraint(addJProp("Для SKU должна быть задана страна", and(true, false), is(freightChanged), 1, countryOfOriginFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        sidCountryOfOriginFreightSku = addJProp(baseGroup, "sidCountryOfOriginFreightSku", "Код страны", baseLM.sidCountry, countryOfOriginFreightSku, 1, 2);
        nameCountryOfOriginFreightSku = addJProp(baseGroup, "nameCountryOfOriginFreightSku", "Страна", baseLM.name, countryOfOriginFreightSku, 1, 2);
        nameCountryOfOriginFreightSku.property.preferredCharWidth = 50;
        nameCountryOfOriginFreightSku.property.minimumCharWidth = 15;

        quantityImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "quantityImporterFreightArticleCompositionCountryCategory", "Кол-во",
                quantityProxyImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        compositionFreightArticleCompositionCountryCategory = addMGProp(baseGroup, "compositionFreightArticleCompositionCountryCategory", "Состав",
                mainCompositionFreightSku, 1, articleSku, 2, mainCompositionOriginFreightSku, 1, 2, countryOfOriginFreightSku, 1, 2, customCategory10FreightSku, 1, 2);

        netWeightStockSku = addJProp(baseGroup, "netWeightStockSku", "Вес нетто", baseLM.multiplyDouble2, quantityStockSku, 1, 2, netWeightSku, 2);
        netWeightStockArticle = addSGProp(baseGroup, "netWeightStockArticle", "Вес нетто", netWeightStockSku, 1, articleSku, 2);
        netWeightStock = addSGProp(baseGroup, "netWeightStock", "Вес нетто короба", netWeightStockSku, 1);

        netWeightFreightSku = addDProp(baseGroup, "netWeightFreightSku", "Вес нетто (ед.)", DoubleClass.instance, freight, sku);
        netWeightFreightSku.setDerivedForcedChange(true, addJProp(baseLM.and1, netWeightSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        addConstraint(addJProp("Для SKU должен быть задан вес нетто", and(true, false), is(freightChanged), 1, netWeightFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        netWeightImporterFreightUnitSku = addJProp(baseGroup, "netWeightImporterFreightUnitSku", "Вес нетто", baseLM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, addJProp(netWeightFreightSku, freightFreightUnit, 1, 2), 2, 3);
        netWeightImporterFreightUnitArticle = addSGProp(baseGroup, "netWeightImporterFreightUnitArticle", true, "Вес нетто", netWeightImporterFreightUnitSku, 1, 2, articleSku, 3);

        netWeightImporterFreightUnitTypeInvoice = addSGProp(baseGroup, "netWeightImporterFreightUnitTypeInvoice", "Вес нетто", netWeightImporterFreightUnitArticle, 1, 2, addJProp(typeInvoiceFreightArticle, freightFreightUnit, 2, 3), 1, 2, 3);

        netWeightImporterFreightUnit = addSGProp(baseGroup, "netWeightImporterFreightUnit", "Вес нетто", netWeightImporterFreightUnitSku, 1, 2);

        netWeightImporterFreightSku = addJProp(baseGroup, "netWeightImporterFreightSku", "Вес нетто", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);
        netWeightProxyImporterFreightSku = addJProp(baseGroup, "netWeightProxyImporterFreightSku", "Вес нетто", baseLM.multiplyDouble2, quantityProxyImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);
        netWeightDirectImporterFreightSku = addJProp(baseGroup, "netWeightDirectImporterFreightSku", "Вес нетто", baseLM.multiplyDouble2, quantityDirectImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);

        netWeightImporterFreightArticle = addSGProp(baseGroup, "netWeightImporterFreightArticle", true, "Вес нетто", netWeightImporterFreightSku, 1, 2, articleSku, 3);

        netWeightImporterFreightTypeInvoice = addSGProp(baseGroup, "netWeightImporterFreightTypeInvoice", "Вес нетто", netWeightProxyImporterFreightSku, 1, 2, typeInvoiceFreightSku, 2, 3);
        netWeightImporterFreightCustomCategory6 = addSGProp(baseGroup, "netWeightImporterFreightCustomCategory6", "Вес нетто", netWeightProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);
        netWeightImporterFreightCustomCategory6Category = addSGProp(baseGroup, "netWeightImporterFreightCustomCategory6Category", "Вес нетто", netWeightProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        netWeightImporterFreightSupplierCustomCategory6Category = addSGProp(baseGroup, "netWeightImporterFreightSupplierCustomCategory6Category", "Вес нетто", netWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        netWeightImporterFreightSupplierCustomCategory6 = addSGProp(baseGroup, "netWeightImporterFreightSupplierCustomCategory6", "Вес нетто", netWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3);

        netWeightImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "netWeightImporterFreightArticleCompositionCountryCategory", "Вес нетто",
                netWeightProxyImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        netWeightImporterFreight = addSGProp(baseGroup, "netWeightImporterFreight", true, "Вес нетто", netWeightProxyImporterFreightSku, 1, 2);
        netWeightImporterFreightSupplier = addSGProp(baseGroup, "netWeightImporterFreightSupplier", true, "Вес нетто", netWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3);

        quantityImporterFreightArticle = addSGProp(baseGroup, "quantityImporterFreightArticle", true, "Кол-во (импортер)", quantityImporterFreightSku, 1, 2, articleSku, 3);
        quantityImporterFreightBrandSupplier = addSGProp(baseGroup, "quantityImporterFreightBrandSupplier", true, "Кол-во (импортер)", quantityImporterFreightSku, 1, 2, brandSupplierArticleSku, 3);

        quantityImporterFreight = addSGProp(baseGroup, "quantityImporterFreight", true, "Кол-во позиций", quantityImporterFreightSku, 1, 2);
        quantityProxyImporterFreight = addSGProp(baseGroup, "quantityProxyImporterFreight", true, "Кол-во позиций", quantityProxyImporterFreightSku, 1, 2);
        quantityImporterFreightTypeInvoice = addSGProp(baseGroup, "quantityImporterFreightTypeInvoice", true, "Кол-во позиций", quantityProxyImporterFreightSku, 1, 2, typeInvoiceFreightSku, 2, 3);
        quantityImporterFreightSupplier = addSGProp(baseGroup, "quantityImporterFreightSupplier", true, "Кол-во позиций", quantityDirectImporterFreightSku, 1, 2, supplierArticleSku, 3);

        quantityFreight = addSGProp(baseGroup, "quantityFreight", true, "Кол-во единиц", quantityImporterFreight, 2);

        // Текущие палеты/коробки для приема
        currentPalletRouteUser = addDProp("currentPalletRouteUser", "Тек. паллета (ИД)", pallet, route, baseLM.user);

        currentPalletRoute = addJProp(true, "currentPalletRoute", "Тек. паллета (ИД)", currentPalletRouteUser, 1, baseLM.currentUser);
        barcodeCurrentPalletRoute = addJProp("barcodeCurrentPalletRoute", "Тек. паллета (штрих-код)", baseLM.barcode, currentPalletRoute, 1);

        sumNetWeightFreightSku = addJProp(baseGroup, "sumNetWeightFreightSku", "Вес нетто (всего)", baseLM.multiplyDouble2, quantityFreightSku, 1, 2, netWeightSku, 2);

        grossWeightCurrentPalletRoute = addJProp(true, "grossWeightCurrentPalletRoute", "Вес брутто", grossWeightPallet, currentPalletRoute, 1);
        grossWeightFreight = addSUProp(baseGroup, "freightGrossWeight", true, "Вес брутто (фрахт)", Union.SUM,
                addSGProp(grossWeightPallet, freightPallet, 1),
                addSGProp(grossWeightDirectInvoice, freightDirectInvoice, 1));

        sumGrossWeightFreightSku = addPGProp(baseGroup, "sumGrossWeightFreightSku", false, 10, false, "Вес брутто",
                sumNetWeightFreightSku,
                grossWeightFreight, 1);

        grossWeightFreightSkuAggr = addJProp(baseGroup, "grossWeightFreightSkuAggr", "Вес брутто", baseLM.divideDouble3, sumGrossWeightFreightSku, 1, 2, quantityFreightSku, 1, 2);
        grossWeightFreightSku = addDProp(baseGroup, "grossWeightFreightSku", "Вес брутто", DoubleClass.instance, freight, sku);
        grossWeightFreightSku.setDerivedForcedChange(true, addJProp(baseLM.and1, grossWeightFreightSkuAggr, 1, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        addConstraint(addJProp("Для SKU должен быть задан вес брутто", and(true, false), is(freightChanged), 1, grossWeightFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        grossWeightImporterFreightSku = addJProp(baseGroup, "grossWeightImporterFreightSku", "Вес брутто", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, grossWeightFreightSku, 2, 3);
        grossWeightProxyImporterFreightSku = addJProp(baseGroup, "grossWeightProxyImporterFreightSku", "Вес брутто", baseLM.multiplyDouble2, quantityProxyImporterFreightSku, 1, 2, 3, grossWeightFreightSku, 2, 3);
        grossWeightDirectImporterFreightSku = addJProp(baseGroup, "grossWeightDirectImporterFreightSku", "Вес брутто", baseLM.multiplyDouble2, quantityDirectImporterFreightSku, 1, 2, 3, grossWeightFreightSku, 2, 3);

        grossWeightImporterFreightTypeInvoice = addSGProp(baseGroup, "grossWeightImporterFreightTypeInvoice", "Вес брутто", grossWeightProxyImporterFreightSku, 1, 2, typeInvoiceFreightSku, 2, 3);
        grossWeightImporterFreightCustomCategory6 = addSGProp(baseGroup, "grossWeightImporterFreightCustomCategory6", "Вес брутто", grossWeightProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);
        grossWeightImporterFreightCustomCategory6Category = addSGProp(baseGroup, "grossWeightImporterFreightCustomCategory6Category", "Вес брутто", grossWeightProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        grossWeightImporterFreightSupplierCustomCategory6Category = addSGProp(baseGroup, "grossWeightImporterFreightSupplierCustomCategory6Category", "Вес брутто", grossWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        grossWeightImporterFreightSupplierCustomCategory6 = addSGProp(baseGroup, "grossWeightImporterFreightSupplierCustomCategory6", "Вес брутто", grossWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3);

        grossWeightImporterFreightSupplier = addSGProp(baseGroup, "grossWeightImporterFreightSupplier", true, "Вес брутто", grossWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3);

        grossWeightImporterFreight = addSGProp(baseGroup, "grossWeightImporterFreight", "Вес брутто", grossWeightProxyImporterFreightSku, 1, 2);
        grossWeightImporterFreightUnitSku = addJProp(baseGroup, "grossWeightImporterFreightUnitSku", "Вес брутто", baseLM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, addJProp(grossWeightFreightSku, freightFreightUnit, 2, 3), 1, 2, 3);
        grossWeightImporterFreightUnitArticle = addSGProp(baseGroup, "grossWeightImporterFreightUnitArticle", "Вес брутто", grossWeightImporterFreightUnitSku, 1, 2, articleSku, 3);

        grossWeightImporterFreightUnitTypeInvoice = addSGProp(baseGroup, "grossWeightImporterFreightUnitTypeInvoice", "Вес брутто", grossWeightImporterFreightUnitArticle, 1, 2, addJProp(typeInvoiceFreightArticle, freightFreightUnit, 2, 3), 1, 2, 3);
        grossWeightImporterFreightUnit = addSGProp(baseGroup, "grossWeightImporterFreightUnit", "Вес брутто", grossWeightImporterFreightUnitSku, 1, 2);

        grossWeightImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "grossWeightImporterFreightArticleCompositionCountryCategory", "Вес брутто",
                grossWeightProxyImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        // цены, надбавки, пошлины, налоги
        priceImporterFreightSku = addDProp(baseGroup, "priceImporterFreightSku", "Цена входная", DoubleClass.instance, importer, freight, sku);
//        priceMaxImporterFreightSku = addMGProp(baseGroup, "priceMaxImporterFreightSku", true, "Цена входная", priceInInvoiceFreightUnitSku, importerDirectInvoice, 1, freightFreightUnit, 2, 3);
        priceProxyImporterFreightSku = addMGProp(baseGroup, "priceProxyImporterFreightSku", true, "Цена входная", priceInShipmentStockSku, importerShipmentFreightBox, 1, 2, freightFreightUnit, 2, 3);
        priceDirectImporterFreightSku = addMGProp(baseGroup, "priceDirectImporterFreightSku", true, "Цена входная", priceRateSupplierBoxSku, importerSupplierBox, 1, freightFreightUnit, 1, 2);
        priceInImporterFreightSku = addSUProp(baseGroup, "priceInImporterFreightSku", "Цена входная", Union.OVERRIDE, priceDirectImporterFreightSku, priceProxyImporterFreightSku, priceImporterFreightSku);

        RRPImporterFreightSku = addDProp(baseGroup, "RRPImporterFreightSku", "Цена рекомендованная", DoubleClass.instance, importer, freight, sku);
        RRPProxyImporterFreightSku = addMGProp(baseGroup, "RRPProxyImporterFreightSku", true, "Цена рекомендованная", RRPInShipmentStockSku, importerShipmentFreightBox, 1, 2, freightFreightUnit, 2, 3);
        RRPDirectImporterFreightSku = addMGProp(baseGroup, "RRPDirectImporterFreightSku", true, "Цена рекомендованная", RRPRateSupplierBoxSku, importerSupplierBox, 1, freightFreightUnit, 1, 2);
        RRPInImporterFreightSku = addSUProp(baseGroup, "RRPInImporterFreightSku", "Цена рекомендованная", Union.OVERRIDE, RRPDirectImporterFreightSku, RRPProxyImporterFreightSku, RRPImporterFreightSku);

        addConstraint(addJProp("Для SKU должна быть задана входная цена", and(true, false), is(freightPriced), 2, priceInImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3), false);

        sumInImporterStockSku = addJProp(baseGroup, "sumInImporterStockSku", "Сумма входная", baseLM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, addJProp(priceInImporterFreightSku, 1, freightFreightBox, 2, 3), 1, 2, 3);
        sumInImporterFreightSku = addJProp(baseGroup, "sumInImporterFreightSku", "Сумма входная", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceInImporterFreightSku, 1, 2, 3);

        contractInProxyImporterStockSku = addMGProp(baseGroup, "contractInProxyImporterStockSku", true, "Договор (ИД)", contractInShipmentStockSku, importerShipmentFreightBox, 1, 2, 2, 3);
        sidContractInProxyImporterStockSku = addJProp(baseGroup, "sidContractInProxyImporterStockSku", "Номер договора", sidContract, contractInProxyImporterStockSku, 1, 2, 3);
        dateContractInProxyImporterStockSku = addJProp(baseGroup, "dateContractInProxyImporterStockSku", "Дата договора", dateContract, contractInProxyImporterStockSku, 1, 2, 3);

        sumFreightImporterFreightSku = addPGProp(baseGroup, "sumFreightImporterFreightSku", false, 10, false, "Сумма фрахта",
                grossWeightImporterFreightSku,
                sumFreightFreight, 2);

        priceFreightImporterFreightSku = addJProp(baseGroup, "priceFreightImporterFreightSku", true, "Цена за фрахт", baseLM.divideDouble2, sumFreightImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        //priceExpenseImporterFreightSku = addJProp(baseGroup, "priceExpenseImporterFreightSku", "Цена затр.", baseLM.sumDouble2, priceInImporterFreightSku, 1, 2, 3, priceFreightImporterFreightSku, 1, 2, 3);

        markupPercentImporterFreightBrandSupplier = addDProp(baseGroup, "markupPercentImporterFreightBrandSupplier", "Надбавка (%)", DoubleClass.instance, importer, freight, brandSupplier);

        markupPercentImporterFreightBrandSupplierArticle = addJProp(baseGroup, "markupPercentImporterFreightBrandSupplierArticle", "Надбавка (%)", baseLM.and1, addJProp(markupPercentImporterFreightBrandSupplier, 1, 2, brandSupplierArticle, 3), 1, 2, 3, quantityImporterFreightArticle, 1, 2, 3);
        markupPercentImporterFreightDataArticle = addDProp(baseGroup, "markupPercentImporterFreightDataArticle", "Надбавка (%)", DoubleClass.instance, importer, freight, article);
        markupPercentImporterFreightArticle = addSUProp(baseGroup, "markupPercentImporterFreightArticle", true, "Надбавка (%)", Union.OVERRIDE, markupPercentImporterFreightBrandSupplierArticle, markupPercentImporterFreightDataArticle);
        markupPercentImporterFreightArticleSku = addJProp(baseGroup, "markupPercentImporterFreightArticleSku", "Надбавка (%)", markupPercentImporterFreightArticle, 1, 2, articleSku, 3);

        markupPercentImporterFreightDataSku = addDProp(baseGroup, "markupPercentImporterFreightDataSku", "Надбавка (%)", DoubleClass.instance, importer, freight, sku);
        markupPercentImporterFreightBrandSupplierSku = addJProp(baseGroup, "markupPercentImporterFreightBrandSupplierSku", true, "Надбавка (%)", baseLM.and1, addJProp(markupPercentImporterFreightBrandSupplier, 1, 2, brandSupplierArticleSku, 3), 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        markupPercentImporterFreightSku = addSUProp(baseGroup, "markupPercentImporterFreightSku", true, "Надбавка (%)", Union.OVERRIDE, markupPercentImporterFreightArticleSku, markupPercentImporterFreightDataSku);

        sumPercentImporterFreightBrandSupplier = addSGProp(baseGroup, "sumPercentImporterFreightBrandSupplier", true, "Сумма процентов надбавок", addJProp(baseLM.multiplyDouble2, markupPercentImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3), 1, 2, brandSupplierArticleSku, 3);
        averagePercentImporterFreightBrandSupplier = addJProp(baseGroup, "averagePercentImporterFreightBrandSupplier", "Средний процент надбавки", baseLM.round0, addJProp(baseLM.divideDouble2, sumPercentImporterFreightBrandSupplier, 1, 2, 3, quantityImporterFreightBrandSupplier, 1, 2, 3), 1, 2, 3);

        minPriceCustomCategoryFreightSku = addJProp(baseGroup, "minPriceCustomCategoryFreightSku", "Минимальная цена ($)", minPriceCustomCategory10SubCategory, customCategory10FreightSku, 1, 2, subCategoryFreightSku, 1, 2);
        minPriceCustomCategoryCountryFreightSku = addJProp(baseGroup, "minPriceCustomCategoryCountryFreightSku", "Минимальная цена для страны ($)", minPriceCustomCategory10SubCategoryCountry, customCategory10FreightSku, 1, 2, subCategoryFreightSku, 1, 2, countryOfOriginFreightSku, 1, 2);

        minPriceRateCustomCategoryFreightSku = addJProp(baseGroup, "minPriceRateCustomCategoryFreightSku", true, "Минимальная цена (евро)", round2, addJProp(baseLM.multiplyDouble2, minPriceCustomCategoryFreightSku, 1, 2, addJProp(nearestRateExchange, typeExchangeCustom, currencyCustom, 1), 1), 1, 2);
        minPriceRateCustomCategoryCountryFreightSku = addJProp(baseGroup, "minPriceRateCustomCategoryCountryFreightSku", true, "Минимальная цена (евро)", round2, addJProp(baseLM.multiplyDouble2, minPriceCustomCategoryCountryFreightSku, 1, 2, addJProp(nearestRateExchange, typeExchangeCustom, currencyCustom, 1), 1), 1, 2);

        minPriceRateFreightSku = addSUProp(baseGroup, "minPriceRateFreightSku", "Минимальная цена (евро)", Union.OVERRIDE, minPriceRateCustomCategoryFreightSku, minPriceRateCustomCategoryCountryFreightSku);
        minPriceRateImporterFreightSku = addJProp(baseGroup, "minPriceImporterFreightSku", "Минимальная цена (евро)", baseLM.and1, minPriceRateFreightSku, 2, 3, is(importer), 1);
        minPriceRateImporterFreightArticle = addMGProp(baseGroup, "minPriceRateImporterFreightArticle", true, "Минимальная для артикула за кг", minPriceRateImporterFreightSku, 1, 2, articleSku, 3);

        minPriceRateWeightImporterFreightSku = addJProp(baseGroup, "minPriceRateWeightImporterFreightSku", "Минимальная для веса", round2, addJProp(baseLM.multiplyDouble2, minPriceRateImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3), 1, 2, 3);

        // sku
        markupInImporterFreightSku = addJProp(baseGroup, "markupInImporterFreightSku", "Надбавка", baseLM.percent2, priceInImporterFreightSku, 1, 2, 3, markupPercentImporterFreightSku, 1, 2, 3);
        priceMarkupInImporterFreightSku = addJProp(baseGroup, "priceMarkupInImporterFreightSku", "Цена выходная", baseLM.sumDouble2, priceInImporterFreightSku, 1, 2, 3, markupInImporterFreightSku, 1, 2, 3);

        priceInOutImporterFreightSku = addDProp(baseGroup, "priceInOutImporterFreightSku", "Цена выходная", DoubleClass.instance, importer, freightPriced, sku);
        priceInOutImporterFreightSku.setDerivedChange(true, addJProp(baseLM.and1, priceMarkupInImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3), 1, 2, 3, is(freightPriced), 2, markupPercentImporterFreightSku, 1, 2, 3);

        priceImporterFreightArticleCompositionCountryCategory = addMGProp(baseGroup, "priceImporterFreightArticleCompositionCountryCategory", false, "Цена", true, priceInOutImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        priceInvoiceImporterFreightSku = addJProp(baseGroup, "priceInvoiceImporterFreightSku", true, "Цена в инвойсе",
                priceImporterFreightArticleCompositionCountryCategory, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        markupInOutImporterFreightSku = addDUProp(baseGroup, "markupInOutImporterFreightSku", "Надбавка", priceInvoiceImporterFreightSku, priceInImporterFreightSku);

        sumInvoiceImporterStockSku = addJProp(baseGroup, "sumInvoiceImporterStockSku", "Сумма", baseLM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, addJProp(priceInvoiceImporterFreightSku, 1, freightFreightBox, 2, 3), 1, 2, 3);

        sumImporterFreightSku = addJProp(baseGroup, "sumImporterFreightSku", "Сумма", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);
        sumProxyImporterFreightSku = addJProp(baseGroup, "sumProxyImporterFreightSku", "Сумма", baseLM.multiplyDouble2, quantityProxyImporterFreightSku, 1, 2, 3, priceInvoiceImporterFreightSku, 1, 2, 3);
        sumDirectImporterFreightSku = addJProp(baseGroup, "sumDirectImporterFreightSku", "Сумма", baseLM.multiplyDouble2, quantityDirectImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);
        sumImporterFreightCustomCategory6 = addSGProp(baseGroup, "sumImporterFreightCustomCategory6", true, "Сумма", sumProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);
        sumImporterFreightCustomCategory6Category = addSGProp(baseGroup, "sumImporterFreightCustomCategory6Category", "Сумма", sumProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        sumImporterFreightSupplierCustomCategory6Category = addSGProp(baseGroup, "sumImporterFreightSupplierCustomCategory6Category", "Сумма", sumDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        sumImporterFreightSupplierCustomCategory6 = addSGProp(baseGroup, "sumImporterFreightSupplierCustomCategory6", true, "Сумма", sumDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3);

        sumImporterFreightUnitSku = addJProp(baseGroup, "sumImporterFreightUnitSku", "Сумма", baseLM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, addJProp(priceInOutImporterFreightSku, 1, freightFreightUnit, 2, 3), 1, 2, 3);

        sumImporterFreightUnitArticle = addSGProp(baseGroup, "sumImporterFreightUnitArticle", "Сумма", sumImporterFreightUnitSku, 1, 2, articleSku, 3);

        sumImporterFreightArticleCompositionCountryCategory = addJProp(baseGroup, "sumImporterFreightArticleCompositionCountryCategory", "Сумма", baseLM.multiplyDouble2,
                quantityImporterFreightArticleCompositionCountryCategory, 1, 2, 3, 4, 5, 6,
                priceImporterFreightArticleCompositionCountryCategory, 1, 2, 3, 4, 5, 6);

        sumProxyInvoiceImporterFreightSku = addJProp(baseGroup, "sumProxyInvoiceImporterFreightSku", "Сумма в инвойсе", baseLM.multiplyDouble2,
                quantityProxyImporterFreightSku, 1, 2, 3,
                priceInvoiceImporterFreightSku, 1, 2, 3);

        ////sumImporterFreight = addSGProp(baseGroup, "sumImporterFreight", true, "Сумма выходная", sumImporterFreightArticleCompositionCountryCategory, 1, 2);
        sumImporterFreight = addSGProp(baseGroup, "sumImporterFreight", true, "Сумма выходная", sumImporterFreightSku, 1, 2);

        sumImporterFreightTypeInvoice = addSGProp(baseGroup, "sumImporterFreightTypeInvoice", "Сумма выходная", sumProxyImporterFreightSku, 1, 2, typeInvoiceFreightSku, 2, 3);

        sumSbivkaImporterFreight = addSGProp(baseGroup, "sumSbivkaImporterFreight", "Сумма выходная", sumProxyImporterFreightSku, 1, 2);
        sumImporterFreightSupplier = addSGProp(baseGroup, "sumImporterFreightSupplier", "Сумма выходная", sumDirectImporterFreightSku, 1, 2, supplierArticleSku, 3);

        sumMarkupInImporterFreightSku = addJProp(baseGroup, "sumMarkupInImporterFreightSku", "Сумма надбавки", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, markupInOutImporterFreightSku, 1, 2, 3);
        sumInOutProxyImporterFreightSku = addJProp(baseGroup, "sumInOutProxyImporterFreightSku", "Сумма выходная", baseLM.multiplyDouble2, quantityProxyImporterFreightSku, 1, 2, 3, priceInvoiceImporterFreightSku, 1, 2, 3);
        sumInOutDirectImporterFreightSku = addJProp(baseGroup, "sumInOutDirectImporterFreightSku", "Сумма выходная", baseLM.multiplyDouble2, quantityDirectImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);
        sumInOutImporterFreightSku = addSUProp(baseGroup, "sumInOutImporterFreightSku", true, "Сумма выходная", Union.SUM, sumInOutProxyImporterFreightSku, sumInOutDirectImporterFreightSku);

        sumInImporterFreightArticle = addSGProp(baseGroup, "sumInImporterFreightArticle", true, "Итого по ценам поставщика (импортер)", sumInImporterFreightSku, 1, 2, articleSku, 3);

        sumInImporterFreightBrandSupplier = addSGProp(baseGroup, "sumInImporterFreightBrandSupplier", true, "Итого по ценам поставщика (импортер)", sumInImporterFreightSku, 1, 2, brandSupplierArticleSku, 3);

        sumInOutImporterFreightArticle = addSGProp(baseGroup, "sumInOutImporterFreightArticle", true, "Итого по отгрузочным ценам (импортер)", sumInOutImporterFreightSku, 1, 2, articleSku, 3);
        sumInOutImporterFreightBrandSupplier = addSGProp(baseGroup, "sumInOutImporterFreightBrandSupplier", true, "Итого по отгрузочным ценам (импортер)", sumInOutImporterFreightSku, 1, 2, brandSupplierArticleSku, 3);

        sumInFreightArticle = addSGProp(baseGroup, "sumInFreightArticle", true, "Итого по ценам поставщика", sumInImporterFreightSku, 2, articleSku, 3);
        sumInFreightBrandSupplier = addSGProp(baseGroup, "sumInFreightBrandSupplier", true, "Итого по ценам поставщика", sumInImporterFreightSku, 2, brandSupplierArticleSku, 3);

        sumInOutFreightArticle = addSGProp(baseGroup, "sumInOutFreightArticle", true, "Итого по отгрузочным ценам", sumInOutImporterFreightSku, 2, articleSku, 3);
        sumInOutFreightBrandSupplier = addSGProp(baseGroup, "sumInOutFreightBrandSupplier", true, "Итого по отгрузочным ценам", sumInOutImporterFreightSku, 2, brandSupplierArticleSku, 3);
        sumInOutFreightBrandSupplierArticle = addJProp(baseGroup, "sumInOutFreightBrandSupplierArticle", "Сумма по бренду", sumInOutFreightBrandSupplier, 1, brandSupplierArticle, 2);
        sumInOutFreightBrandSupplierSku = addJProp(baseGroup, "sumInOutFreightBrandSupplierSku", "Сумма по бренду", sumInOutFreightBrandSupplier, 1, brandSupplierArticleSku, 2);

        sumInOutImporterFreight = addSGProp(baseGroup, "sumInOutImporterFreight", true, "Сумма выходная", sumInOutImporterFreightSku, 1, 2);
        sumInOutFreight = addSGProp(baseGroup, "sumInOutFreight", true, true, "Сумма выходная", sumInOutImporterFreight, 2);
        // временно так пока система сама не научится либо обнаруживать равные свойства, либо решать проблему с инкрементностью по другому
        insuranceImporterFreightSku = addJProp("insuranceImporterFreightSku", true, "Сумма за страховку", addSFProp("ROUND(CAST((prm1*prm2/prm3) as NUMERIC(15,3))," + 10 + ")", DoubleClass.instance, 3), sumInOutImporterFreightSku, 1, 2, 3, insuranceFreightBrandSupplierSku, 2, 3, sumInOutFreightBrandSupplierSku, 2, 3);
        //addPGProp(baseGroup, "insuranceImporterFreightSku", false, 2, false, "Сумма страховки",
        //        sumInOutImporterFreightSku,
        //        insuranceFreight, 2);

        priceInsuranceImporterFreightSku = addJProp(baseGroup, "priceInsuranceImporterFreightSku", "Цена за страховку", baseLM.divideDouble2, insuranceImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);

        priceFullImporterFreightSku = addJProp(baseGroup, "priceFullImporterFreightSku", true, "Цена с расходами", baseLM.and1, addSUProp(Union.SUM, priceInvoiceImporterFreightSku, priceFreightImporterFreightSku, priceInsuranceImporterFreightSku), 1, 2, 3, is(freightPriced), 2);
        priceFullKgImporterFreightSku = addJProp(baseGroup, "priceFullKgImporterFreightSku", "Цена за кг", baseLM.divideDouble2, priceFullImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);

        sumFullImporterFreightArticle = addSGProp(baseGroup, "sumFullImporterFreightArticle", true, "Сумма", addJProp(baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceFullImporterFreightSku, 1, 2, 3), 1, 2, articleSku, 3);

        priceFullKgImporterFreightArticle = addJProp(baseGroup, "priceFullKgImporterFreightArticle", "Цена за кг", baseLM.divideDouble2, sumFullImporterFreightArticle, 1, 2, 3, netWeightImporterFreightArticle, 1, 2, 3);

        diffPriceMinPriceImporterFreightArticle = addDUProp(baseGroup, "diffPriceMinPriceImporterFreightArticle", "Разница цен", minPriceRateImporterFreightArticle, priceFullKgImporterFreightArticle);
        greaterPriceMinPriceImporterFreightArticle = addJProp(baseGroup, "greaterPriceMinPriceImporterFreightArticle", "Недостаточность цены", baseLM.greater2, diffPriceMinPriceImporterFreightArticle, 1, 2, 3, baseLM.vzero);

        diffPriceMinPriceImporterFreightSku = addDUProp(baseGroup, "diffPriceMinPriceImporterFreightSku", "Разница цен", minPriceRateImporterFreightSku, priceFullKgImporterFreightSku);
        greaterPriceMinPriceImporterFreightSku = addJProp(baseGroup, "greaterPriceMinPriceImporterFreightSku", "Недостаточность цены", baseLM.greater2, diffPriceMinPriceImporterFreightSku, 1, 2, 3, baseLM.vzero);

        dutyNetWeightFreightSku = addJProp(baseGroup, "dutyNetWeightFreightSku", "Пошлина по весу нетто", baseLM.multiplyDouble2, netWeightFreightSku, 1, 2, addJProp(dutySumCustomCategory10TypeDuty, customCategory10FreightSku, 1, 2, typeDutyDuty), 1, 2);
        dutyNetWeightImporterFreightSku = addJProp(baseGroup, "dutyNetWeightImpoterFreightSku", "Пошлина по весу нетто", baseLM.and1, dutyNetWeightFreightSku, 2, 3, is(importer), 1);

        dutyPercentImporterFreightSku = addJProp(baseGroup, "dutyPercentImporterFreightSku", "Пошлина по цене", baseLM.percent2, priceFullImporterFreightSku, 1, 2, 3, addJProp(dutyPercentCustomCategory10TypeDuty, customCategory10FreightSku, 2, 3, typeDutyDuty), 1, 2, 3);

        dutyImporterFreightSku = addJProp(baseGroup, "dutyImporterFreightSku", true, "Пошлина", and(false, false), addSUProp(Union.MAX, dutyNetWeightImporterFreightSku, dutyPercentImporterFreightSku), 1, 2, 3, is(freightPriced), 2, quantityImporterFreightSku, 1, 2, 3);
        priceDutyImporterFreightSku = addJProp(baseGroup, "priceDutyImporterFreightSku", "Сумма пошлины", baseLM.sumDouble2, dutyImporterFreightSku, 1, 2, 3, priceInvoiceImporterFreightSku, 1, 2, 3);

        priceFullDutyImporterFreightSku = addSUProp(baseGroup, "priceFullDutyImporterFreightSku", "Цена с пошлиной", Union.SUM, priceFullImporterFreightSku, dutyImporterFreightSku);

        sumDutyImporterFreightSku = addJProp(baseGroup, "sumDutyImporterFreightSku", "Сумма пошлины", baseLM.multiplyDouble2, dutyImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        sumDutyImporterFreight = addSGProp(baseGroup, "sumDutyImporterFreight", true, "Сумма пошлины", sumDutyImporterFreightSku, 1, 2);

        NDSPercentOriginFreightSku = addJProp(baseGroup, "NDSPercentOriginFreightSku", "НДС (%)", dutyPercentCustomCategory10TypeDuty, customCategory10FreightSku, 1, 2, typeDutyNDS);
        NDSPercentCustomFreightSku = addJProp(baseGroup, "NDSPercentCustomFreightSku", "НДС (%)", and(false, false), NDSPercentCustom, is(freight), 1, is(sku), 2);
        NDSPercentFreightSku = addSUProp(baseGroup, "NDSPercentFreightSku", "НДС (%)", Union.OVERRIDE, NDSPercentCustomFreightSku, NDSPercentOriginFreightSku);
        NDSImporterFreightSku = addJProp(baseGroup, "NDSImporterFreightSku", "НДС (евро)", baseLM.percent2, priceFullDutyImporterFreightSku, 1, 2, 3, NDSPercentFreightSku, 2, 3);

        sumNDSImporterFreightSku = addJProp(baseGroup, "sumNDSImporterFreightSku", "Сумма НДС", baseLM.multiplyDouble2, NDSImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        sumNDSImporterFreight = addSGProp(baseGroup, "sumNDSImporterFreight", true, "Сумма НДС", sumNDSImporterFreightSku, 1, 2);

        sumRegistrationFreightSku = addJProp(baseGroup, "sumRegistrationFreightSku", "За оформление", dutyPercentCustomCategory10TypeDuty, customCategory10FreightSku, 1, 2, typeDutyRegistration);
        sumRegistrationImporterFreightSku = addJProp(baseGroup, "sumRegistrationImporterFreightSku", "За оформление", baseLM.and1, sumRegistrationFreightSku, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        sumRegistrationImporterFreight = addMGProp(baseGroup, "sumRegistrationImporterFreight", true, "За оформление", sumRegistrationImporterFreightSku, 1, 2);

        sumCustomImporterFreight = addSUProp(baseGroup, "sumCustomImporterFreight", "Итого по таможенным платежам", Union.SUM, sumDutyImporterFreight, sumNDSImporterFreight, sumRegistrationImporterFreight);
        sumCustomImporterFreight.property.preferredCharWidth = 35;

        sumMarkupInImporterFreight = addSGProp(baseGroup, "sumMarkupInImporterFreight", true, "Сумма надбавки", sumMarkupInImporterFreightSku, 1, 2);

        sumInOutImporterFreightTypeInvoice = addSGProp(baseGroup, "sumInOutImporterFreightTypeInvoice", true, "Сумма выходная", sumInOutProxyImporterFreightSku, 1, 2, typeInvoiceFreightSku, 2, 3);

        sumMarkupInFreight = addSGProp(baseGroup, "sumMarkupInFreight", true, "Сумма надбавки", sumMarkupInImporterFreight, 2);

        //markupImporterFreightSku = addJProp(baseGroup, "markupImporterFreightSku", "Надбавка", baseLM.percent2, priceExpenseImporterFreightSku, 1, 2, 3, markupPercentImporterFreightSku, 1, 2, 3);
        //sumMarkupImporterFreightSku = addJProp(baseGroup, "sumMarkupImporterFreightSku", "Сумма надбавки", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, markupInOutImporterFreightSku, 1, 2, 3);

        //priceOutImporterFreightSku = addJProp(baseGroup, "priceOutImporterFreightSku", "Цена выходная", baseLM.sumDouble2, priceExpenseImporterFreightSku, 1, 2, 3, markupImporterFreightSku, 1, 2, 3);
        //sumOutImporterFreightSku = addJProp(baseGroup, "sumOutImporterFreightSku", "Сумма выходная", baseLM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceOutImporterFreightSku, 1, 2, 3);

        sumInImporterFreight = addSGProp(baseGroup, "sumInImporterFreight", true, "Сумма входная", sumInImporterFreightSku, 1, 2);
        //sumMarkupImporterFreight = addSGProp(baseGroup, "sumMarkupImporterFreight", true, "Сумма надбавки", sumMarkupImporterFreightSku, 1, 2);
        //sumOutImporterFreight = addSGProp(baseGroup, "sumOutImporterFreight", true, "Сумма выходная", sumOutImporterFreightSku, 1, 2);

        sumInFreight = addSGProp(baseGroup, "sumInFreight", true, "Сумма входная", sumInImporterFreight, 2);
        //sumMarkupFreight = addSGProp(baseGroup, "sumMarkupFreight", true, "Сумма надбавки", sumMarkupImporterFreight, 2);
        //sumOutFreight = addSGProp(baseGroup, "sumOutFreight", true, "Сумма выходная", sumOutImporterFreight, 2);

        // итоги с начала года
        sumInCurrentYear = addSGProp(baseGroup, "sumInCurrentYear", "Итого вход", addJProp(baseLM.and1, sumInFreight, 1, addJProp(baseLM.equals2, addJProp(baseLM.yearInDate, baseLM.currentDate), addJProp(baseLM.yearInDate, baseLM.date, 1), 1), 1));
        sumInOutCurrentYear = addSGProp(baseGroup, "sumInOutCurrentYear", "Итого выход", addJProp(baseLM.and1, sumInOutFreight, 1, addJProp(baseLM.equals2, addJProp(baseLM.yearInDate, baseLM.currentDate), addJProp(baseLM.yearInDate, baseLM.date, 1), 1), 1));
        balanceSumCurrentYear = addDUProp(baseGroup, "balanceSumCurrentYear", "Сальдо", sumInOutCurrentYear, sumInCurrentYear);

        currentFreightBoxRouteUser = addDProp("currentFreightBoxRouteUser", "Тек. короб (ИД)", freightBox, route, baseLM.user);

        currentFreightBoxRoute = addJProp(true, "currentFreightBoxRoute", "Тек. короб (ИД)", currentFreightBoxRouteUser, 1, baseLM.currentUser);
        barcodeCurrentFreightBoxRoute = addJProp("barcodeCurrentFreightBoxRoute", "Тек. короб (штрих-код)", baseLM.barcode, currentFreightBoxRoute, 1);

        destinationCurrentFreightBoxRoute = addJProp(true, "destinationCurrentFreightBoxRoute", "Пункт назначения тек. короба (ИД)", destinationFreightBox, currentFreightBoxRoute, 1);
        nameDestinationCurrentFreightBoxRoute = addJProp("nameDestinationCurrentFreightBoxRoute", "Пункт назначения тек. короба", baseLM.name, destinationCurrentFreightBoxRoute, 1);

        isCurrentFreightBox = addJProp(baseLM.equals2, addJProp(true, currentFreightBoxRoute, routeCreationFreightBoxFreightBox, 1), 1, 1);
        isCurrentPallet = addJProp(baseLM.equals2, addJProp(true, currentPalletRoute, routeCreationPalletPallet, 1), 1, 1);
        currentPalletFreightBox = addJProp(currentPalletRoute, routeCreationFreightBoxFreightBox, 1);
        isCurrentPalletFreightBox = addJProp(true, baseLM.and1, addJProp(baseLM.equals2, palletFreightBox, 1, currentPalletFreightBox, 1), 1, currentPalletFreightBox, 1);
        isStoreFreightBoxSupplierBox = addJProp(baseLM.equals2, destinationFreightBox, 1, destinationSupplierBox, 2);

        seekRouteShipmentSkuRoute = addAProp(new SeekRouteActionProperty());

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

        addBoxShipmentDetailBoxShipmentSupplierBoxRouteSku = addJProp(true, "Добавить строку поставки",
                addBoxShipmentDetailBoxShipmentSupplierBoxStockSku, 1, 2, currentFreightBoxRoute, 3, 4);

        addSimpleShipmentDetailSimpleShipmentRouteSku = addJProp(true, "Добавить строку поставки",
                addSimpleShipmentDetailSimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3);

        addBoxShipmentDetailBoxShipmentSupplierBoxRouteBarcode = addJProp(true, "Добавить строку поставки",
                addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode, 1, 2, currentFreightBoxRoute, 3, 4);

        addSimpleShipmentDetailSimpleShipmentRouteBarcode = addJProp(true, "Добавить строку поставки",
                addSimpleShipmentSimpleShipmentDetailStockBarcode, 1, currentFreightBoxRoute, 2, 3);

        quantityRouteSku = addJProp(baseGroup, "quantityRouteSku", "Оприход. в короб для транспортировки", quantityStockSku, currentFreightBoxRoute, 1, 2);

        quantitySupplierBoxBoxShipmentRouteSku = addJProp(baseGroup, true, "quantitySupplierBoxBoxShipmentRouteSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4);
        quantitySimpleShipmentRouteSku = addJProp(baseGroup, true, "quantitySimpleShipmentRouteSku", "Кол-во оприход.",
                quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3);

        createSku = addJProp(true, "Сгенерировать товары", addAAProp(sku, baseLM.barcode, baseLM.barcodePrefix, true), quantityCreationSku, 1);
        createFreightBox = addJProp(true, "Сгенерировать короба", addAAProp(freightBox, baseLM.barcode, baseLM.barcodePrefix, true), quantityCreationFreightBox, 1);
        createPallet = addJProp(true, "Сгенерировать паллеты", addAAProp(pallet, baseLM.barcode, baseLM.barcodePrefix, true), quantityCreationPallet, 1);
        createStamp = addAProp(actionGroup, new CreateStampActionProperty());

        barcodeActionCheckFreightBox = addJProp(true, "Проверка короба для транспортировки",
                addJProp(true, and(false, false, true),
                        addStopActionProp("Для выбранного маршрута не задан короб для транспортировки", "Поиск по штрих-коду"),
                        is(sku), 2,
                        is(route), 1,
                        currentFreightBoxRoute, 1), 1, baseLM.barcodeToObject, 2);

        barcodeActionCheckChangedFreightBox = addJProp(true, "Проверка короба для транспортировки (скомплектован)",
                addJProp(true, and(false, false, false),
                        addStopActionProp("Текущей короб находится в скомплектованном фрахте", "Поиск по штрих-коду"),
                        is(sku), 2,
                        is(route), 1,
                        addJProp(freightFreightBox, currentFreightBoxRoute, 1), 1), 1, baseLM.barcodeToObject, 2);

        cloneItem = addAProp(new CloneItemActionProperty());

        barcodeAction4 = addJProp(true, "Ввод штрих-кода 4",
                addCUProp(
                        addSCProp(addJProp(true, quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4))
                ), 1, 2, 3, baseLM.barcodeToObject, 4);
        barcodeAction3 = addJProp(true, "Ввод штрих-кода 3",
                addCUProp(
                        addSCProp(addJProp(true, quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3))
                ), 1, 2, baseLM.barcodeToObject, 3);
        declarationExport = addDEAProp("declarationExport");
        invoiceExportDbf = addProperty(null, new LP<ClassPropertyInterface>(new InvoiceExportDbfActionProperty("invoiceExportDbf", "Экспорт в dbf", BL, importer, freight, typeInvoice)));
        scalesComPort = addDProp(baseGroup, "scalesComPort", "COM-порт весов", IntegerClass.instance, baseLM.computer);
        scalesSpeed = addDProp(baseGroup, "scalesSpeed", "Скорость весов", IntegerClass.instance, baseLM.computer);
        scannerComPort = addDProp(baseGroup, "scannerComPort", "COM-порт сканера", IntegerClass.instance, baseLM.computer);

        initNavigators();
    }

    public LP addDEAProp(String sID) {
        return addProperty(null, new LP<ClassPropertyInterface>(new DeclarationExportActionProperty(sID, "Экспорт декларанта", BL, importer, freight)));
    }

    @Override
    public void initIndexes() {
    }

    private void initNavigators() {

        ToolBarNavigatorWindow mainToolbar = new ToolBarNavigatorWindow(JToolBar.HORIZONTAL, "mainToolbar", "Навигатор");
        mainToolbar.titleShown = false;
        mainToolbar.drawScrollBars = false;

        baseLM.navigatorWindow.y = 10;
        baseLM.navigatorWindow.height -= 10;

        PanelNavigatorWindow generateToolbar = new PanelNavigatorWindow(SwingConstants.HORIZONTAL, "generateToolbar", "Генерация");
        generateToolbar.titleShown = false;
        generateToolbar.drawRoot = true;
        generateToolbar.drawScrollBars = false;

        ToolBarNavigatorWindow leftToolbar = new ToolBarNavigatorWindow(JToolBar.VERTICAL, "leftToolbar", "Список");

        baseLM.baseElement.window = mainToolbar;
        baseLM.adminElement.window = leftToolbar;

        TreeNavigatorWindow objectsWindow = new TreeNavigatorWindow("objectsWindow", "Объекты");
        objectsWindow.drawRoot = true;
        baseLM.objectElement.window = objectsWindow;

        mainToolbar.setDockPosition(0, 0, 100, 6);
        generateToolbar.setDockPosition(20, 6, 80, 4);
        leftToolbar.setDockPosition(0, 6, 20, 64);
        objectsWindow.setDockPosition(0, 6, 20, 64);
        baseLM.formsWindow.setDockPosition(20, 10, 80, 89);

        NavigatorElement classifier = addNavigatorElement(baseLM.baseElement, "classifier", "Справочники");
        classifier.window = leftToolbar;
        //NavigatorElement classifierItem = addNavigatorElement(classifier, "classifierItem", "Для описания товаров");
        addFormEntity(new NomenclatureFormEntity(classifier, "nomenclatureForm", "Номенклатура"));

        CategoryFormEntity categoryForm = addFormEntity(new CategoryFormEntity(classifier, "categoryForm", "Номенклатурные группы"));
        category.setDialogForm(categoryForm, categoryForm.objCategory);

        addFormEntity(new ColorSizeSupplierFormEntity(classifier, "сolorSizeSupplierForm", "Поставщики"));
        addFormEntity(new CustomCategoryFormEntity(classifier, "customCategoryForm", "ТН ВЭД (изменения)", false));
        addFormEntity(new CustomCategoryFormEntity(classifier, "customCategoryForm2", "ТН ВЭД (дерево)", true));

        CustomCategory10FormEntity customCategory10Form = addFormEntity(new CustomCategory10FormEntity(classifier, "customCategory10Form", "ТН ВЭД"));
        customCategory10.setDialogForm(customCategory10Form, customCategory10Form.objCustomCategory10);

        classifier.add(baseLM.country.getListForm(baseLM).form);
        classifier.add(unitOfMeasure.getListForm(baseLM).form);
        classifier.add(typeFabric.getListForm(baseLM).form);
        classifier.add(commonSize.getListForm(baseLM).form);
        addFormEntity(new CommonSizeEditFormEntity(classifier, "commonEditSizeForm", "Белорусские размеры"));
        addFormEntity(new CommonSizeImportFormEntity(classifier, "commonImportSizeForm", "Белорусские размеры (таблицей)"));
        classifier.add(seasonSupplier.getListForm(baseLM).form);
        classifier.add(importer.getListForm(baseLM).form);
        classifier.add(exporter.getListForm(baseLM).form);
        addFormEntity(new ContractFormEntity(classifier, "contractForm", "Договора"));
        addFormEntity(new StoreFormEntity(classifier, "storeForm", "Магазины"));
        //classifier.add(store.getListForm(baseLM));
        classifier.add(freightType.getListForm(baseLM).form);
        classifier.add(baseLM.dictionaryForm);

        ArticleCompositeEditFormEntity articleCompositeEditForm = new ArticleCompositeEditFormEntity(null, "articleCompositeEditForm", "Артикул (составной)");
        articleComposite.setEditForm(articleCompositeEditForm, articleCompositeEditForm.objArticleComposite);

        ArticleCompositeFormEntity articleCompositeForm = new ArticleCompositeFormEntity(null, "articleCompositeForm", "Артикул (составной)");
        articleComposite.setDialogForm(articleCompositeForm, articleCompositeForm.objArticleComposite);

        ColorSupplierFormEntity colorSupplierForm = new ColorSupplierFormEntity(null, "colorSupplierForm", "Цвет поставщика");
        colorSupplier.setDialogForm(colorSupplierForm, colorSupplierForm.objColor);

        SizeSupplierFormEntity sizeSupplierForm = new SizeSupplierFormEntity(null, "sizeSupplierForm", "Размер поставщика");
        sizeSupplier.setDialogForm(sizeSupplierForm, sizeSupplierForm.objSize);

        SeasonSupplierFormEntity seasonSupplierForm = new SeasonSupplierFormEntity(null, "seasonSupplierForm", "Сезон поставщика");
        seasonSupplier.setDialogForm(seasonSupplierForm, seasonSupplierForm.objSeason);

        ThemeSupplierFormEntity themeSupplierForm = new ThemeSupplierFormEntity(null, "themeSupplierForm", "Тема поставщика");
        themeSupplier.setDialogForm(themeSupplierForm, themeSupplierForm.objTheme);

        GenderSupplierFormEntity genderSupplierForm = new GenderSupplierFormEntity(null, "genderSupplierForm", "Пол поставщика");
        genderSupplier.setDialogForm(genderSupplierForm, genderSupplierForm.objGender);

        CommonSizeFormEntity commonSizeForm = new CommonSizeFormEntity(null, "commonSizeForm", "Унифицированный размер");
        commonSize.setDialogForm(commonSizeForm, commonSizeForm.objCommonSize);

        createItemForm = addFormEntity(new CreateItemFormEntity(null, "createItemForm", "Ввод товара"));
        editItemForm = addFormEntity(new EditItemFormEntity(null, "editItemForm", "Редактирование товара"));
        findItemFormBox = addFormEntity(new FindItemFormEntity(null, "findItemFormBox", "Поиск товара (с коробами)", true, false));
        findItemFormSimple = addFormEntity(new FindItemFormEntity(null, "findItemFormSimple", "Поиск товара", false, false));
        findItemFormBoxBarcode = addFormEntity(new FindItemFormEntity(null, "findItemFormBoxBarcode", "Поиск товара (с коробами и выбором штрих-кода)", true, true));
        findItemFormSimpleBarcode = addFormEntity(new FindItemFormEntity(null, "findItemFormSimpleBarcode", "Поиск товара (с выбором штрих-кода)", false, true));

        NavigatorElement printForms = addNavigatorElement(baseLM.baseElement, "printForms", "Печатные формы");
        printForms.window = leftToolbar;
        addFormEntity(new AnnexInvoiceFormEntity(printForms, "annexInvoiceForm", "Приложение к инвойсу", false));
        invoiceFromFormEntity = addFormEntity(new AnnexInvoiceFormEntity(printForms, "annexInvoiceForm2", "Приложение к инвойсу (перевод)", true));
        addFormEntity(new InvoiceFromFormEntity(printForms, "invoiceFromForm", "Исходящие инвойсы", false));
        addFormEntity(new InvoiceFromFormEntity(printForms, "invoiceFromForm2", "Исходящие инвойсы (перевод)", true));
        addFormEntity(new DeclarantFormEntity(printForms, "declarantForm", "Экспорт в деларант"));
        addFormEntity(new ProformInvoiceFormEntity(printForms, "proformInvoiceForm", "Исходящие инвойсы-проформы", false));
        addFormEntity(new ProformInvoiceFormEntity(printForms, "proformInvoiceForm2", "Исходящие инвойсы-проформы (перевод)", true));
        addFormEntity(new SbivkaFormEntity(printForms, "sbivkaForm", "Сбивка товаров"));
        addFormEntity(new SbivkaSupplierFormEntity(printForms, "sbivkaSupplierForm", "Сбивка товаров поставщика"));
        addFormEntity(new PackingListFormEntity(printForms, "packingListForm", "Исходящие упаковочные листы", false));
        addFormEntity(new PackingListFormEntity(printForms, "packingListForm2", "Исходящие упаковочные листы (перевод)", true));
        addFormEntity(new PackingListBoxFormEntity(printForms, "packingListBoxForm", "Упаковочные листы коробов"));
        addFormEntity(new ListFreightUnitFreightFormEntity(printForms, "listFreightUnitFreightForm", "Список коробов"));
        addFormEntity(new PrintSkuFormEntity(printForms, "printSkuForm", "Товар"));

        FormEntity createSkuForm = addFormEntity(new CreateSkuFormEntity(printForms, "createSkuForm", "Штрих-коды товаров", FormType.PRINT));
        FormEntity createPalletForm = addFormEntity(new CreatePalletFormEntity(printForms, "createPalletForm", "Штрих-коды паллет", FormType.PRINT));
        FormEntity createFreightBoxForm = addFormEntity(new CreateFreightBoxFormEntity(printForms, "createFreightBoxForm", "Штрих-коды коробов", FormType.PRINT));

        invoiceExportForm = addFormEntity(new InvoiceExportFormEntity(printForms, "invoiceExportForm", "Экспорт инвойсов"));

        NavigatorElement purchase = addNavigatorElement(baseLM.baseElement, "purchase", "Управление закупками");
        purchase.window = leftToolbar;
        addFormEntity(new PricatFormEntity(purchase, "pricatForm", "Прикаты"));


        NavigatorElement purchaseCreate = addNavigatorElement(purchase, "purchaseCreate", "Создать");
        addFormEntity(new OrderEditFormEntity(purchaseCreate, "orderAddForm", "Заказ", false)).showType = FormShowType.MODAL_FULLSCREEN;
        addFormEntity(new InvoiceEditFormEntity(purchaseCreate, "boxInvoiceAddForm", "Инвойс по коробам", true, false)).showType = FormShowType.MODAL_FULLSCREEN;
        addFormEntity(new InvoiceEditFormEntity(purchaseCreate, "simpleInvoiceAddForm", "Инвойс без коробов", false, false)).showType = FormShowType.MODAL_FULLSCREEN;
        purchaseCreate.window = generateToolbar;

        addFormEntity(new OrderEditFormEntity(null, "orderEditForm", "Редактировать заказ", true));
        addFormEntity(new OrderFormEntity(purchase, "orderForm", "Заказы"));

        addFormEntity(new InvoiceEditFormEntity(null, "boxInvoiceEditForm", "Редактировать инвойс по коробам", true, true));
        addFormEntity(new InvoiceEditFormEntity(null, "simpleInvoiceEditForm", "Редактировать инвойс без коробов", false, true));
        addFormEntity(new InvoiceFormEntity(purchase, "boxInvoiceForm", "Инвойсы по коробам", true));
        addFormEntity(new InvoiceFormEntity(purchase, "simpleInvoiceForm", "Инвойсы без коробов", false));
        addFormEntity(new ShipmentListFormEntity(purchase, "boxShipmentListForm", "Поставки по коробам", true));
        addFormEntity(new ShipmentListFormEntity(purchase, "simpleShipmentListForm", "Поставки без коробов", false));
        addFormEntity(new ShipmentExportFormEntity(purchase, "shipmentExportForm", "Экспорт поставки"));
        addFormEntity(new InvoiceShipmentFormEntity(purchase, "invoiceShipmentForm", "Сравнение по инвойсам"));

        NavigatorElement distribution = addNavigatorElement(baseLM.baseElement, "distribution", "Управление складом");
        distribution.window = leftToolbar;

        NavigatorElement generation = addNavigatorElement(distribution, "generation", "Генерация");
        generation.window = generateToolbar;

        FormEntity createPalletFormCreate = addFormEntity(new CreatePalletFormEntity(generation, "createPalletFormAdd", "Сгенерировать паллеты", FormType.ADD));
        createPalletFormCreate.showType = FormShowType.MODAL;
        FormEntity createFreightBoxFormAdd = addFormEntity(new CreateFreightBoxFormEntity(generation, "createFreightBoxFormAdd", "Сгенерировать короба", FormType.ADD));
        createFreightBoxFormAdd.showType = FormShowType.MODAL;
        FormEntity createSkuFormAdd = addFormEntity(new CreateSkuFormEntity(generation, "createSkuFormAdd", "Сгенерировать товары", FormType.ADD));
        createSkuFormAdd.showType = FormShowType.MODAL;
        FormEntity createStampFormAdd = addFormEntity(new CreateStampFormEntity(generation, "createStampFormAdd", "Сгенерировать марки", FormType.ADD));
        createStampFormAdd.showType = FormShowType.MODAL;

        NavigatorElement preparation = addNavigatorElement(distribution, "preparation", "Подготовка к приемке");

        addFormEntity(new CreatePalletFormEntity(preparation, "createPalletFormList", "Паллеты", FormType.LIST));
        addFormEntity(new CreateFreightBoxFormEntity(preparation, "createFreightBoxFormList", "Короба", FormType.LIST));
        addFormEntity(new CreateSkuFormEntity(preparation, "createSkuFormList", "Товары", FormType.LIST));
        addFormEntity(new CreateStampFormEntity(preparation, "createStampFormList", "Марки", FormType.LIST));

        NavigatorElement acceptance = addNavigatorElement(distribution, "acceptance", "Приемка");
        addFormEntity(new ShipmentSpecFormEntity(acceptance, "boxShipmentSpecForm", "Прием товара по коробам", true));
        addFormEntity(new ShipmentSpecFormEntity(acceptance, "simpleShipmentSpecForm", "Прием товара без коробов", false));

        NavigatorElement placing = addNavigatorElement(distribution, "placing", "Распределение");
        addFormEntity(new BoxPalletStoreFormEntity(placing, "boxPalletStoreForm", "Распределение коробов по паллетам"));
        addFormEntity(new FreightShipmentStoreFormEntity(placing, "freightShipmentStoreForm", "Распределение паллет по фрахтам"));

        NavigatorElement balance = addNavigatorElement(distribution, "balance", "Остатки по товарам");
        addFormEntity(new BalanceBrandWarehouseFormEntity(balance, "balanceBrandWarehouseForm", "Остатки на складе (по брендам)"));
        addFormEntity(new BalanceWarehouseFormEntity(balance, "balanceWarehouseForm", "Остатки на складе"));
        addFormEntity(new BalanceWarehousePeriodFormEntity(balance, "balanceWarehousePeriodForm", "Движение товара за период"));

        NavigatorElement shipment = addNavigatorElement(baseLM.baseElement, "shipment", "Управление фрахтами");
        shipment.window = leftToolbar;

        addFormEntity(new FreightContentFormEntity(shipment, "freightContentForm", "Содержимое фрахта"));
        addFormEntity(new FreightReportFormEntity(shipment, "freightReporttForm", "Отчёт по фрахту"));
        addFormEntity(new FreightBoxContentFormEntity(shipment, "freightBoxContentForm", "Содержимое короба"));
        NavigatorElement actionFreight = addNavigatorElement(shipment, "actionFreight", "Действия");
        addFormEntity(new FreightCreateFormEntity(actionFreight, "freightCreateForm", "Редактирование фрахта"));
        addFormEntity(new FreightShipmentFormEntity(actionFreight, "freightShipmentForm", "Комплектация фрахта"));
        addFormEntity(new FreightChangeFormEntity(actionFreight, "freightChangeForm", "Обработка фрахта"));
        addFormEntity(new FreightInvoiceFormEntity(actionFreight, "freightInvoiceForm", "Расценка фрахта"));

        logFreightForm = new LogFormEntity("logFreightForm", "История фрахта", nameClassFreight, logFreight, baseLM, false);
        formLogFreight = addMFAProp("История фрахта", logFreightForm, logFreightForm.params);
        formLogFreight.setImage("history.png");

        addFormEntity(new FreightListFormEntity(shipment, "freightListForm", "Список фрахтов"));
        addFormEntity(new PrintDocumentFormEntity(shipment, "printDocumentForm", "Печать документов"));

        shipment.add(actionFreight);

        NavigatorElement customs = addNavigatorElement(baseLM.baseElement, "customs", "Для таможни");
        customs.window = leftToolbar;

        NavigatorElement settings = addNavigatorElement(baseLM.baseElement, "settings", "Настройки");
        settings.window = leftToolbar;
        addFormEntity(new GlobalParamFormEntity(settings, "globalParamForm", "Общие параметры"));
        NavigatorElement classifierCurrency = addNavigatorElement(settings, "classifierCurrency", "Валюты и курсы");
        classifierCurrency.add(currency.getListForm(baseLM).form);
        classifierCurrency.add(typeExchange.getListForm(baseLM).form);
        addFormEntity(new RateCurrencyFormEntity(classifierCurrency, "rateCurrencyForm", "Курсы валют"));

        // пока не поддерживается из-за того, что пока нет расчета себестоимости для внутреннего перемещения
//        addFormEntity(new StockTransferFormEntity(distribution, "stockTransferForm", "Внутреннее перемещение"));

        baseLM.baseElement.add(printForms);
        baseLM.baseElement.add(baseLM.adminElement);
    }

    @Override
    public String getNamePrefix() {
        return null;
    }


    private class
            BarcodeFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objBarcode;

        protected Font getDefaultFont() {
            return null;
        }

        private BarcodeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseLM.objectValue, baseLM.barcodeObjectName);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

//            objBarcode.resetOnApply = true;

            getPropertyDraw(baseLM.objectValue, objBarcode).eventSID = ScannerDaemonTask.SCANNER_SID;

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

    private class CategoryFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCategory;

        private CategoryFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objCategory = addSingleGroupObject(category, "Номенклатурная группа", baseGroup);
            setReadOnly(true);

            addFormActions(this, objCategory);

            addDefaultOrder(baseLM.name, true);
        }
    }

    private class ArticleCompositeEditFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objArticleComposite;

        public ArticleCompositeEditFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objArticleComposite = addSingleGroupObject(articleComposite, "Артикул", sidArticle, nameSupplierArticle, nameBrandSupplierArticle);
            objArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            addDefaultOrder(sidArticle, true);
        }
    }


    private class ArticleCompositeFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objArticleComposite;

        public ArticleCompositeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objArticleComposite = addSingleGroupObject(articleComposite, "Артикул", sidArticle, nameSupplierArticle, nameBrandSupplierArticle, nameCategoryArticle, mainCompositionOriginArticle);
            setReadOnly(true);

            addFormActions(this, objArticleComposite);

            addDefaultOrder(sidArticle, true);
        }
    }


    private class ColorSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objColor;

        public ColorSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objColor = addSingleGroupObject(colorSupplier, "Цвет поставщика", sidColorSupplier, baseLM.name, nameSupplierColorSupplier);
            setReadOnly(true);

            addFormActions(this, objColor);

            addDefaultOrder(sidColorSupplier, true);
        }
    }

    private class SeasonSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSeason;

        public SeasonSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSeason = addSingleGroupObject(seasonSupplier, "Сезон поставщика", sidSeasonSupplier, nameSupplierSeasonSupplier);
            setReadOnly(true);

            addFormActions(this, objSeason);

            addDefaultOrder(sidSeasonSupplier, true);
        }
    }

    private class GenderSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objGender;

        public GenderSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objGender = addSingleGroupObject(genderSupplier, "Пол поставщика", sidGenderSupplier, nameSupplierGenderSupplier/*, nameGenderSupplierSku*/);
            setReadOnly(true);

            addFormActions(this, objGender);

            addDefaultOrder(sidGenderSupplier, true);
        }
    }

     private class ThemeSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objTheme;

        public ThemeSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objTheme = addSingleGroupObject(themeSupplier, "Тема поставщика", sidThemeSupplier, baseLM.name, nameSupplierThemeSupplier);
            setReadOnly(true);

            addFormActions(this, objTheme);

            addDefaultOrder(sidThemeSupplier, true);
        }
    }

    private class SizeSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSize;

        public SizeSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSize = addSingleGroupObject(sizeSupplier, "Размер поставщика", sidSizeSupplier, nameSupplierSizeSupplier, orderSizeSupplier);
            setReadOnly(true);

            addFormActions(this, objSize);

            addDefaultOrder(orderSizeSupplier, true);
        }
    }

    private class CustomCategory10FormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCustomCategory10;

        private CustomCategory10FormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objCustomCategory10 = addSingleGroupObject(customCategory10, "ТН ВЭД", sidCustomCategory10, nameCustomCategory, nameCustomCategory4CustomCategory10, numberIdCustomCategory10);
            objCustomCategory10.groupTo.initClassView = ClassViewType.GRID;

            addDefaultOrder(numberIdCustomCategory10, true);
        }
    }

    private class CommonSizeFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCommonSize;

        private CommonSizeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objCommonSize = addSingleGroupObject(commonSize, "Унифицированный размер", baseLM.name);
            objCommonSize.groupTo.initClassView = ClassViewType.GRID;

            addDefaultOrder(baseLM.name, true);
        }
    }

    private class GlobalParamFormEntity extends FormEntity<RomanBusinessLogics> {

        private GlobalParamFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            addPropertyDraw(nameDictionaryComposition);
            addPropertyDraw(nameTypeExchangeSTX);
            addPropertyDraw(nameTypeExchangeCustom);
            addPropertyDraw(nameTypeExchangePayCustom);
            addPropertyDraw(nameCurrencyCustom);
            addPropertyDraw(nameCurrencyPayCustom);
            addPropertyDraw(NDSPercentCustom);
            addPropertyDraw(sidTypeDutyDuty);
            addPropertyDraw(nameTypeDutyDuty);
            addPropertyDraw(sidTypeDutyNDS);
            addPropertyDraw(nameTypeDutyNDS);
            addPropertyDraw(sidTypeDutyRegistration);
            addPropertyDraw(nameTypeDutyRegistration);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

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
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objCurrency.groupTo).grid.constraints.fillVertical = 1;
            design.get(objDateRate.groupTo).grid.constraints.fillVertical = 3;

            return design;
        }
    }

    private class StoreFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objStore;
        private ObjectEntity objSupplier;


        private StoreFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objStore = addSingleGroupObject(store, "Магазин", baseLM.name, sidDestination);
            addObjectActions(this, objStore);

            objStore.groupTo.initClassView = ClassViewType.GRID;

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);
            objSupplier.groupTo.initClassView = ClassViewType.GRID;

            addPropertyDraw(relationStoreSupplier, objStore, objSupplier);
            addPropertyDraw(sidDestinationSupplier, objStore, objSupplier);
            setReadOnly(objSupplier, true);
            setReadOnly(relationStoreSupplier, false);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objStore.groupTo).grid.constraints.fillVertical = 3;
            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 2;

            return design;
        }
    }


    private class PackingListBoxFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objBox;
        private ObjectEntity objArticle;

        private PackingListBoxFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objBox = addSingleGroupObject(1, "box", freightBox, "Короб", nameDestinationFreightBox, baseLM.barcode, netWeightStock, quantityStock);
            objBox.groupTo.initClassView = ClassViewType.PANEL;

            objArticle = addSingleGroupObject(2, "article", article, "Артикул", sidArticle, nameBrandSupplierArticle, nameCategoryArticle);

            addPropertyDraw(quantityStockArticle, objBox, objArticle);
            addPropertyDraw(netWeightStockArticle, objBox, objArticle);
            objArticle.groupTo.initClassView = ClassViewType.GRID;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStockArticle, objBox, objArticle)));

            packingListFormFreightBox = addFAProp("Упаковочный лист", this, objBox);
            packingListFormRoute = addJProp(true, "packingListFormRoute", "Упаковочный лист", packingListFormFreightBox, currentFreightBoxRoute, 1);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objBox.groupTo).grid.constraints.fillVertical = 2;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 3;
            return design;
        }
    }


    private class OrderEditFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean edit;

        private ObjectEntity objOrder;
        private ObjectEntity objSIDArticleComposite;
        private ObjectEntity objSIDColorSupplier;
        private ObjectEntity objGroupSizeSupplier;
        private ObjectEntity objArticle;
        private ObjectEntity objItem;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objColorSupplier;
        //private ObjectEntity objSku;

        private OrderEditFormEntity(NavigatorElement parent, String sID, String caption, boolean edit) {
            super(parent, sID, caption);

            this.edit = edit;

            objOrder = addSingleGroupObject(order, "Заказ");
            if (!edit) {
                addPropertyDraw(nameSupplierDocument, objOrder);
                objOrder.setAddOnEvent(FormEventType.INIT);
            }

            addPropertyDraw(objOrder, baseLM.date, sidDocument, nameDestinationDestinationDocument, dateFromOrder, dateToOrder, nameCurrencyDocument, sumDocument, quantityDocument, netWeightDocument);
            objOrder.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", baseLM.objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            objArticle = addSingleGroupObject(articleComposite, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, objOrder, objArticle);
            addPropertyDraw(objArticle, nameSizeGroupSupplierArticle, sidArticle, nameBrandSupplierArticle, nameSeasonSupplierArticle, nameThemeSupplierArticle,
                    nameCategoryArticle, originalNameArticle, sidCustomCategoryOriginArticle,
                    nameCountrySupplierOfOriginArticle, netWeightArticle, mainCompositionOriginArticle, baseLM.barcode);
            addPropertyDraw(quantityListArticle, objOrder, objArticle);
            addPropertyDraw(dateFromOrderArticle, objOrder, objArticle);
            addPropertyDraw(dateToOrderArticle, objOrder, objArticle);
            addPropertyDraw(priceDocumentArticle, objOrder, objArticle);
            addPropertyDraw(RRPDocumentArticle, objOrder, objArticle);
            addPropertyDraw(sumDocumentArticle, objOrder, objArticle);
            addPropertyDraw(invoicedOrderArticle, objOrder, objArticle);
            //addPropertyDraw(priceOrderedInvoiceArticle, objInvoice, objArticle);

            objSIDColorSupplier = addSingleGroupObject(StringClass.get(50), "Ввод цвета", baseLM.objectValue);
            objSIDColorSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(addNEColorSupplierSIDInvoice, objSIDColorSupplier, objOrder));
            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(seekColorSIDInvoice, objSIDColorSupplier, objOrder));

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер"); // baseLM.selection, sidSizeSupplier
            addPropertyDraw(orderSizeSupplier, objSizeSupplier).forceViewType = ClassViewType.HIDE;

            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", baseLM.selection, sidColorSupplier, baseLM.name);
            setReadOnly(sidColorSupplier, true, objColorSupplier.groupTo);

            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(executeAddColorDocument, objOrder, objArticle, objColorSupplier));

            objItem = addSingleGroupObject(item, "Товар", baseLM.barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityListArticleCompositeColorSize, objOrder, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.columnGroupObjects.add(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(sidSizeSupplier, objSizeSupplier);

            addPropertyDraw(addGCAProp(actionGroup, "nullOrderListArticleCompositeColor" + (edit ? "Edit" : ""), "Сбросить", objSizeSupplier.groupTo, quantityListArticleCompositeColorSize, 1, 2, 3, 4, baseLM.vzero, 4),
                    objOrder, objArticle, objColorSupplier, objSizeSupplier);

            addPropertyDraw(quantityListSku, objOrder, objItem);
            addPropertyDraw(priceDocumentSku, objOrder, objItem);
            addPropertyDraw(priceRateDocumentSku, objOrder, objItem);
            addPropertyDraw(invoicedOrderSku, objOrder, objItem);
            addPropertyDraw(quantityListArticleCompositeColor, objOrder, objArticle, objColorSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, addPropertyObject(supplierDocument, objOrder)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objOrder)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objOrder)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleCompositeItem, objItem), Compare.EQUALS, objArticle));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(numberListArticle, objOrder, objArticle)));


//            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeGroup, objGroupSizeSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objInvoice)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(groupSizeSupplier, objSizeSupplier), Compare.EQUALS, addPropertyObject(sizeGroupSupplierArticle, objArticle)));

            RegularFilterGroupEntity filterGroupColor = new RegularFilterGroupEntity(genID());
            filterGroupColor.addFilter(new RegularFilterEntity(genID(), new OrFilterEntity(
                    new NotNullFilterEntity(addPropertyObject(quantityListArticleCompositeColor, objOrder, objArticle, objColorSupplier)),
                    new CompareFilterEntity(addPropertyObject(inListArticleColorSupplier, objOrder, objArticle, objColorSupplier), Compare.EQUALS, true)),
                    "В заказе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(filterGroupColor);

            RegularFilterGroupEntity filterItemOrder = new RegularFilterGroupEntity(genID());
            filterItemOrder.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityListSku, objOrder, objItem)),
                    "В заказе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(filterItemOrder);

            if(edit) {
               orderEditFA = addMFAProp(actionGroup, "Редактировать заказ", this, new ObjectEntity[] {objOrder}, true);
               orderEditFA.setImage("edit.png");
            }

            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addAProp(new AddNewArticleActionProperty(objArticle)), objSIDArticleComposite, objOrder));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, objOrder, objSIDArticleComposite));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(seekArticleSIDInvoice, objSIDArticleComposite, objOrder));

            addDefaultOrder(numberListArticle, true);
            addDefaultOrder(orderSizeSupplier, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidDocument, objOrder)).caption = "Номер заказа";
            design.get(getPropertyDraw(baseLM.date, objOrder)).caption = "Дата заказа";

            design.get(objOrder.groupTo).grid.constraints.fillVertical = 0.2;

            design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(baseLM.objectValue, objSIDColorSupplier)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);

            design.addIntersection(design.getGroupObjectContainer(objColorSupplier.groupTo),
                    design.getGroupObjectContainer(objItem.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objItem.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objColorSupplier.groupTo).grid.constraints.fillHorizontal = 3;

//            design.get(getPropertyDraw(cloneItem, objItem)).drawToToolbar = true;
            return design;
        }

        @Override
        public boolean isActionOnChange(Property property) {
            return property.equals(quantityListArticleCompositeColorSize.property);
        }
        @Override
        public <P extends PropertyInterface> void onChange(Property<P> property, PropertyChange<P> change, DataSession session, Modifier modifier) throws SQLException {
            if(property.equals(quantityListArticleCompositeColorSize.property)) { // если изменяем quantityListArticle
                // смотрим изменения
                itemArticleCompositeColorSize.property.setJoinNotNull(
                        BaseUtils.join(BaseUtils.buildMap(itemArticleCompositeColorSize.listInterfaces, quantityListArticleCompositeColorSize.listInterfaces.subList(1, 4)), change.mapKeys),
                        change.expr.getWhere().and(change.where), session, modifier);
            }
        }
    }

    private class OrderFormEntity extends FormEntity<RomanBusinessLogics> {
        private ObjectEntity objSupplier;
        private ObjectEntity objOrder;
        private ObjectEntity objInvoice;
        private ObjectEntity objSku;

        private OrderFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name, nameCurrencySupplier);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objOrder = addSingleGroupObject(order, "Заказ", baseLM.date, sidDocument, dateFromOrder, dateToOrder, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);
            addObjectActions(this, objOrder);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier));
            addPropertyDraw(orderEditFA, objOrder).forceViewType = ClassViewType.GRID;

            objInvoice = addSingleGroupObject(invoice, "Инвойс");
            objInvoice.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inOrderInvoice, objOrder, objInvoice);
            addPropertyDraw(objInvoice, baseLM.date, sidDocument, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            objSku = addSingleGroupObject(sku, "SKU");
            addPropertyDraw(new LP[]{baseLM.barcode, sidArticleSku, sidSeasonSupplierArticleSku, sidGenderSupplierArticleSku, sidThemeSupplierArticleSku, nameThemeSupplierArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    sidBrandSupplierArticleSku, nameBrandSupplierArticleSku, originalNameArticleSku,
                    nameCountrySupplierOfOriginArticleSku, nameCountryOfOriginSku, netWeightSku,
                    mainCompositionOriginSku, additionalCompositionOriginSku, baseLM.delete}, objSku);

            setReadOnly(sidArticleSku, true, objSku.groupTo);

            addPropertyDraw(priceDocumentSku, objOrder, objSku);
            addPropertyDraw(quantityDocumentSku, objOrder, objSku);
            addPropertyDraw(invoicedOrderSku, objOrder, objSku);
            addPropertyDraw(shipmentedOrderSku, objOrder, objSku);
            addPropertyDraw(shipmentedAtTermOrderSku, objOrder, objSku);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDocumentSku, objOrder, objSku)));

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            //addFixedFilter(new CompareFilterEntity(addPropertyObject(inOrderInvoice, objOrder, objInvoice), Compare.EQUALS, true));

            CompareFilterEntity invoiceSupplierFilter = new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier);
            addFixedFilter(invoiceSupplierFilter);

            setReadOnly(objSupplier, true);

            //addDefaultOrder(numberListArticle, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidDocument, objOrder)).caption = "Номер заказа";
            design.get(getPropertyDraw(baseLM.date, objOrder)).caption = "Дата заказа";

            ContainerView detContainer = design.createContainer();
            design.getMainContainer().addAfter(detContainer, design.getGroupObjectContainer(objOrder.groupTo));
            detContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            detContainer.add(design.getGroupObjectContainer(objInvoice.groupTo));
            detContainer.tabbedPane = true;

            return design;
        }
    }


    private class InvoiceEditFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean box;
        private boolean edit;

        private ObjectEntity objInvoice;
        private ObjectEntity objSupplierBox;
        private ObjectEntity objSIDArticleComposite;
        private ObjectEntity objSIDArticleSingle;
        private ObjectEntity objSIDColorSupplier;
        private ObjectEntity objGroupSizeSupplier;
        private ObjectEntity objArticle;
        private ObjectEntity objItem;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objColorSupplier;
        private PropertyDrawEntity nullArticleColor;
        private PropertyDrawEntity nullArticle;
        //private ObjectEntity objSku;

        private InvoiceEditFormEntity(NavigatorElement parent, String sID, String caption, boolean box, boolean edit) {
            super(parent, sID, caption);

            this.box = box;
            this.edit = edit;

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс");
            if (!edit) {
                addPropertyDraw(nameSupplierDocument, objInvoice);
                objInvoice.setAddOnEvent(FormEventType.INIT);
            }

            addPropertyDraw(objInvoice, baseLM.date, baseLM.objectClassName, sidDocument, nameCurrencyDocument, sumDocument, quantityDocument, netWeightDocument, nameExporterInvoice, nameImporterDirectInvoice, sidContractInvoice, sidDestinationDestinationDocument, nameDestinationDestinationDocument);
            objInvoice.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objList;
            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб", sidSupplierBox, baseLM.barcode, nameDestinationDataSupplierBox);
                objSupplierBox.groupTo.initClassView = ClassViewType.GRID;
                addObjectActions(this, objSupplierBox);
                objList = objSupplierBox;
            } else
                objList = objInvoice;

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", baseLM.objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            //objSIDArticleSingle = addSingleGroupObject(StringClass.get(50), "Ввод простого артикула", baseLM.objectValue);
            //objSIDArticleSingle.groupTo.setSingleClassView(ClassViewType.PANEL);

            //addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(addNEArticleSingleSIDInvoice, objSIDArticleSingle, objInvoice));
            //addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleSingle));
            //addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(seekArticleSIDInvoice, objSIDArticleSingle, objInvoice));

            objArticle = addSingleGroupObject(articleComposite, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(objArticle, nameSizeGroupSupplierArticle, sidArticle, nameBrandSupplierArticle, nameSeasonSupplierArticle, nameThemeSupplierArticle, nameCategoryArticle, originalNameArticle, sidCustomCategoryOriginArticle,
                    nameCountrySupplierOfOriginArticle, netWeightArticle, mainCompositionOriginArticle, baseLM.barcode);
            addPropertyDraw(quantityListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(quantitySimpleInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(priceDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(RRPDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(sumDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(sumSimpleInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(orderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(priceOrderedInvoiceArticle, objInvoice, objArticle);

//            getPropertyDraw(sizeGroupSupplierArticle).forceViewType = ClassViewType.PANEL;

            objSIDColorSupplier = addSingleGroupObject(StringClass.get(50), "Ввод цвета", baseLM.objectValue);
            objSIDColorSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(addNEColorSupplierSIDInvoice, objSIDColorSupplier, objInvoice));
            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(seekColorSIDInvoice, objSIDColorSupplier, objInvoice));

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер"); // baseLM.selection, sidSizeSupplier
            addPropertyDraw(orderSizeSupplier, objSizeSupplier).forceViewType = ClassViewType.HIDE;

            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", baseLM.selection, sidColorSupplier, baseLM.name);
            setReadOnly(sidColorSupplier, true, objColorSupplier.groupTo);

            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(executeAddColorDocument, objList, objArticle, objColorSupplier));

            objItem = addSingleGroupObject(item, "Товар", baseLM.barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityListArticleCompositeColorSize, objList, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.columnGroupObjects.add(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(sidSizeSupplier, objSizeSupplier);

            String formPostfix = (box ? "Box" : "") + (edit ? "Edit" : "");

            nullArticle = addPropertyDraw(addEPAProp("nullListArticle" + formPostfix, "Сбросить", EPA_NULL,
                    quantityListArticle,
                    numberListArticle),
                    objList, objArticle);
            nullArticleColor = addPropertyDraw(addEPAProp("nullInvoiceListArticleCompositeColor" + formPostfix, "Сбросить", EPA_NULL,
                                                addGCAProp(actionGroup, "nullGCAInvoiceListArticleCompositeColor" + formPostfix, "Сбросить (количество)", objSizeSupplier.groupTo, quantityListArticleCompositeColorSize, 1, 2, 3, 4, baseLM.vnull, 4), 1, 2, 3, 4,
                                                inListArticleColorSupplier, 1, 2, 3),
                                                objList, objArticle, objColorSupplier, objSizeSupplier);

            addPropertyDraw(quantityListSku, (box ? objSupplierBox : objInvoice), objItem);
            addPropertyDraw(priceDocumentSku, objInvoice, objItem);
            addPropertyDraw(priceRateDocumentSku, objInvoice, objItem);
            addPropertyDraw(orderedInvoiceSku, objInvoice, objItem);
            addPropertyDraw(quantityListArticleCompositeColor, objList, objArticle, objColorSupplier);
//            addPropertyDraw(quantityListArticleCompositeSize, objInvoice, objArticle, objSizeSupplier);

            if (box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(boxInvoiceSupplierBox, objSupplierBox), Compare.EQUALS, objInvoice));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, addPropertyObject(supplierDocument, objInvoice)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objInvoice)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objInvoice)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleCompositeItem, objItem), Compare.EQUALS, objArticle));
            //addFixedFilter(new NotNullFilterEntity(addPropertyObject(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle), Compare.GREATER, addPropertyObject(baseLM.vzero)));

//            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeGroup, objGroupSizeSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objInvoice)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(groupSizeSupplier, objSizeSupplier), Compare.EQUALS, addPropertyObject(sizeGroupSupplierArticle, objArticle)));

/*            RegularFilterGroupEntity filterGroupSize = new RegularFilterGroupEntity(genID());
            filterGroupSize.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityListArticleCompositeSize, objInvoice, objArticle, objSizeSupplier)),
                    "В инвойсе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupSize);*/

            //addPropertyObject(sidColorSupplier, objColorSupplier), Compare.EQUALS, objSIDColorSupplier)

            RegularFilterGroupEntity filterGroupColor = new RegularFilterGroupEntity(genID());
            filterGroupColor.addFilter(new RegularFilterEntity(genID(), new OrFilterEntity(
                    new NotNullFilterEntity(addPropertyObject(quantityListArticleCompositeColor, objList, objArticle, objColorSupplier)),
                    new CompareFilterEntity(addPropertyObject(inListArticleColorSupplier, objList, objArticle, objColorSupplier), Compare.EQUALS, true)),
                    "В инвойсе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(filterGroupColor);

            RegularFilterGroupEntity filterItemInvoice = new RegularFilterGroupEntity(genID());
            filterItemInvoice.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityListSku, objList, objItem)),
                    "В инвойсе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(filterItemInvoice);

            if(edit){
               if(box) {
                  boxInvoiceEditFA = addMFAProp(actionGroup, "Редактировать инвойс", this, new ObjectEntity[] {objInvoice}, true);
                  boxInvoiceEditFA.setImage("edit.png");
               }
               else {
                  simpleInvoiceEditFA = addMFAProp(actionGroup, "Редактировать инвойс", this, new ObjectEntity[] {objInvoice}, true);
                  simpleInvoiceEditFA.setImage("edit.png");
               }
            }

//            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addNEArticleCompositeSIDInvoice, objSIDArticleComposite, objInvoice));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addAProp(new AddNewArticleActionProperty(objArticle)), objSIDArticleComposite, objInvoice));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleComposite));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(seekArticleSIDInvoice, objSIDArticleComposite, objInvoice));

            addDefaultOrder(numberListArticle, true);
            addDefaultOrder(orderSizeSupplier, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.objectClassName, objInvoice)).caption = "Тип инвойса";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(baseLM.date, objInvoice)).caption = "Дата инвойса";

            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 0.2;

            if (box) {
                design.addIntersection(design.getGroupObjectContainer(objInvoice.groupTo),
                    design.getGroupObjectContainer(objSupplierBox.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objSIDArticleComposite.groupTo),
                    design.getGroupObjectContainer(objSupplierBox.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.get(objSupplierBox.groupTo).grid.constraints.fillVertical= 0.4;
            }

            design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(baseLM.objectValue, objSIDColorSupplier)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);
            /*design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleSingle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

            design.addIntersection(design.getGroupObjectContainer(objSIDArticleComposite.groupTo),
                    design.getGroupObjectContainer(objSIDArticleSingle.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);*/

            design.addIntersection(design.getGroupObjectContainer(objColorSupplier.groupTo),
                    design.getGroupObjectContainer(objItem.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

//            design.addIntersection(design.getGroupObjectContainer(objSizeSupplier.groupTo),
//                    design.getGroupObjectContainer(objColorSupplier.groupTo),
//                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objItem.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objColorSupplier.groupTo).grid.constraints.fillHorizontal = 3;

            design.get(nullArticle).design.setIconPath("delete.png");
            design.get(nullArticleColor).design.setIconPath("delete.png");
//            design.get(getPropertyDraw(cloneItem, objItem)).drawToToolbar = true;
            return design;
        }

        @Override
        public boolean isActionOnChange(Property property) {
            return property.equals(quantityListArticleCompositeColorSize.property);
        }
        @Override
        public <P extends PropertyInterface> void onChange(Property<P> property, PropertyChange<P> change, DataSession session, Modifier modifier) throws SQLException {
            if(property.equals(quantityListArticleCompositeColorSize.property)) { // если изменяем quantityListArticle
                // смотрим изменения
                Map mapKeys = BaseUtils.join(BaseUtils.buildMap(itemArticleCompositeColorSize.listInterfaces, quantityListArticleCompositeColorSize.listInterfaces.subList(1, 4)), change.mapKeys);
                itemArticleCompositeColorSize.property.setJoinNotNull(
                        mapKeys, change.expr.getWhere().and(change.where), session, modifier);
            }
        }
    }

    private class InvoiceFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean box;

        private ObjectEntity objSupplier;
        private ObjectEntity objOrder;
        private ObjectEntity objInvoice;
        private ObjectEntity objSku;

        private InvoiceFormEntity(NavigatorElement parent, String sID, String caption, boolean box) {
            super(parent, sID, caption);

            this.box = box;
            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name, nameCurrencySupplier, importInvoiceActionGroup, true);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс", baseLM.date, baseLM.objectClassName, sidDocument, nameCurrencyDocument, sumDocument,
                    quantityDocument, netWeightDocument, nameExporterInvoice, nameImporterDirectInvoice, sidContractInvoice, sidDestinationDestinationDocument, nameDestinationDestinationDocument, baseLM.delete);
            //addObjectActions(this, objInvoice);

            if (box) {
                addPropertyDraw(boxInvoiceEditFA, objInvoice).forceViewType = ClassViewType.GRID;
            }
            else {
                addPropertyDraw(simpleInvoiceEditFA, objInvoice).forceViewType = ClassViewType.GRID;
            }

            objOrder = addSingleGroupObject(order, "Заказ");
            objOrder.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inOrderInvoice, objOrder, objInvoice);
            addPropertyDraw(objOrder, baseLM.date, sidDocument, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            objSku = addSingleGroupObject(sku, "SKU");
            addPropertyDraw(new LP[]{baseLM.barcode, sidArticleSku, sidSeasonSupplierArticleSku, sidGenderSupplierArticleSku, sidThemeSupplierArticleSku, nameThemeSupplierArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    sidBrandSupplierArticleSku, nameBrandSupplierArticleSku, originalNameArticleSku,
                    nameCountrySupplierOfOriginArticleSku, nameCountryOfOriginSku, netWeightSku,
                    mainCompositionOriginSku, additionalCompositionOriginSku, baseLM.delete}, objSku);

            setReadOnly(sidArticleSku, true, objSku.groupTo);

            addPropertyDraw(priceDocumentSku, objInvoice, objSku);
            addPropertyDraw(quantityDocumentSku, objInvoice, objSku);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDocumentSku, objInvoice, objSku)));

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            if (!box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue)));

            if (box)
                addFixedFilter(new NotFilterEntity(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue))));

            CompareFilterEntity orderSupplierFilter = new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier);
            addFixedFilter(orderSupplierFilter);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));

            /*addFixedFilter(new CompareFilterEntity(addPropertyObject(inOrderInvoice, objOrder, objInvoice), Compare.EQUALS, true));
            addPropertyDraw(
                    addSelectFromListAction(null, "Выбрать заказы", objOrder, new FilterEntity[]{orderSupplierFilter}, inOrderInvoice, true, order, invoice),
                    objOrder.groupTo,
                    objInvoice
            ).forceViewType = ClassViewType.PANEL;*/

            setReadOnly(objSupplier, true);
            setReadOnly(importInvoiceActionGroup, false, objSupplier.groupTo);

            //addDefaultOrder(numberListArticle, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.objectClassName, objInvoice)).caption = "Тип инвойса";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(baseLM.date, objInvoice)).caption = "Дата инвойса";
            design.get(getPropertyDraw(sidDocument, objOrder)).caption = "Номер заказа";
            design.get(getPropertyDraw(baseLM.date, objOrder)).caption = "Дата заказа";

            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 1;
            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 3;
            design.get(objOrder.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 4;

            ContainerView detContainer = design.createContainer();
            design.getMainContainer().addAfter(detContainer, design.getGroupObjectContainer(objInvoice.groupTo));
            detContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            detContainer.add(design.getGroupObjectContainer(objOrder.groupTo));
            detContainer.tabbedPane = true;

            return design;
        }
    }

    private class ShipmentListFormEntity extends FormEntity {
        private boolean box;

        private ObjectEntity objSupplier;
        private ObjectEntity objShipment;
        private ObjectEntity objInvoice;
        private ObjectEntity objOrder;
        private ObjectEntity objRoute;

        private ShipmentListFormEntity(NavigatorElement parent, String sID, String caption, boolean box) {
            super(parent, sID, caption);

            this.box = box;

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", baseLM.date, sidDocument, dateDepartureShipment, dateArrivalShipment, netWeightShipment, grossWeightShipment, quantityPalletShipment, quantityBoxShipment);//, invoicedShipment, sumShipment
            addObjectActions(this, objShipment);

            objOrder = addSingleGroupObject(order, "Заказ", sidDocument, dateFromOrder, dateToOrder);
            addPropertyDraw(inOrderShipment, objOrder, objShipment);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс");
            objInvoice.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objInvoice, true);

            addPropertyDraw(inInvoiceShipment, objInvoice, objShipment);
            addPropertyDraw(objInvoice, baseLM.date, sidDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            objRoute = addSingleGroupObject(route, "Маршрут", baseLM.name);
            addPropertyDraw(nameImporterShipmentRoute, objShipment, objRoute);
            addPropertyDraw(percentShipmentRoute, objShipment, objRoute);
            addPropertyDraw(invoicedShipmentRoute, objShipment, objRoute);
            addPropertyDraw(sumShipmentRoute, objShipment, objRoute);

            if (!box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue)));

            if (box) {
                addFixedFilter(new NotFilterEntity(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue))));
                addFixedFilter(new NotFilterEntity(new CompareFilterEntity(addPropertyObject(is(directInvoice), objInvoice), Compare.EQUALS, addPropertyObject(baseLM.vtrue))));
            }

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objShipment), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier));

            setReadOnly(objSupplier, true);

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidDocument, objShipment)).caption = "Номер поставки";
            design.get(getPropertyDraw(baseLM.date, objShipment)).caption = "Дата поставки";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(baseLM.date, objInvoice)).caption = "Дата инвойса";

            design.get(objRoute.groupTo).grid.constraints.fillHorizontal = 0.6;

            design.addIntersection(design.getGroupObjectContainer(objShipment.groupTo),
                    design.getGroupObjectContainer(objOrder.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objInvoice.groupTo),
                    design.getGroupObjectContainer(objRoute.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class PrintSkuFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSku;

        private PrintSkuFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objSku = addSingleGroupObject(1, "sku", sku, "Товар", baseLM.barcode);
            objSku.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objSku, true);

            skuPrintFA = addFAProp("Печать штрих-кода", this, objSku);
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

        private PropertyDrawEntity findItemBox;
        private PropertyDrawEntity findItemSimple;

        private PropertyDrawEntity nameRoute;

        private ShipmentSpecFormEntity(NavigatorElement parent, String sID, String caption, boolean box) {
            super(parent, sID, caption);

            isSynchronizedApply = true;

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
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб поставщика", sidSupplierBox, baseLM.barcode, nameDestinationSupplierBox, quantityDataList, quantitySupplierBox);
                objSupplierBox.groupTo.initClassView = ClassViewType.PANEL;
            }

            objRoute = addSingleGroupObject(route, "Маршрут", baseLM.name, barcodeCurrentPalletRoute, grossWeightCurrentPalletRoute, barcodeCurrentFreightBoxRoute, nameDestinationCurrentFreightBoxRoute);
            objRoute.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(packingListFormRoute, objRoute);
            addPropertyDraw(changePallet, objRoute);

            nameRoute = addPropertyDraw(baseLM.name, objRoute);
            nameRoute.forceViewType = ClassViewType.PANEL;

            objSku = addSingleGroupObject(sku, "SKU", baseLM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameBrandSupplierArticleSku, originalNameArticleSku, nameCategoryArticleSku, nameUnitOfMeasureArticleSku,
                    netWeightArticleSku, sidCustomCategoryOriginArticleSku, nameCountryOfOriginArticleSku, mainCompositionOriginArticleSku,
                    netWeightSku, nameCountryOfOriginSku, mainCompositionOriginSku, additionalCompositionOriginSku);

            objSku.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(skuEditFA, objSku).forceViewType = ClassViewType.GRID;
            addPropertyDraw(skuPrintFA, objSku).forceViewType = ClassViewType.GRID;

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

                PropertyObjectEntity diffListSupplierBoxProperty = addPropertyObject(diffListSupplierBox, objSupplierBox);
                getPropertyDraw(quantityDataList).setPropertyHighlight(diffListSupplierBoxProperty);
                getPropertyDraw(quantitySupplierBox).setPropertyHighlight(diffListSupplierBoxProperty);

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
                    baseLM.selection, barcodeSkuShipmentDetail, nameBrandSupplierArticleSkuShipmentDetail, sidArticleShipmentDetail, sidColorSupplierItemShipmentDetail, nameColorSupplierItemShipmentDetail, sidSizeSupplierItemShipmentDetail,
                    nameBrandSupplierArticleSkuShipmentDetail, sidCustomCategoryOriginArticleSkuShipmentDetail, originalNameArticleSkuShipmentDetail,
                    nameCategoryArticleSkuShipmentDetail, nameUnitOfMeasureArticleSkuShipmentDetail, sidGenderArticleSkuShipmentDetail, nameTypeFabricArticleSkuShipmentDetail,
                    coefficientArticleSkuShipmentDetail,  netWeightArticleSkuShipmentDetail,
                    nameCountryOfOriginArticleSkuShipmentDetail, mainCompositionOriginArticleSkuShipmentDetail,
                    netWeightSkuShipmentDetail, nameCountryOfOriginSkuShipmentDetail,
                    mainCompositionOriginSkuShipmentDetail, additionalCompositionOriginSkuShipmentDetail,
                    sidShipmentShipmentDetail, priceInShipmentDetail,
                    sidSupplierBoxShipmentDetail, barcodeSupplierBoxShipmentDetail,
                    barcodeStockShipmentDetail, nameRouteFreightBoxShipmentDetail,
                    quantityShipmentDetail, nameUserShipmentDetail, sidStampShipmentDetail, seriesOfStampShipmentDetail, timeShipmentDetail, baseLM.delete);

            objShipmentDetail.groupTo.setSingleClassView(ClassViewType.GRID);

            LP skuShipmentDetailEditFA = addJProp(true, "Редактировать", skuEditFA, skuShipmentDetail, 1);
            skuShipmentDetailEditFA.setImage("edit.png");
            addPropertyDraw(skuShipmentDetailEditFA, objShipmentDetail);

            getPropertyDraw(sidStampShipmentDetail).propertyCaption = addPropertyObject(hideSidStampShipmentDetail, objShipmentDetail);
            getPropertyDraw(seriesOfStampShipmentDetail).propertyCaption = addPropertyObject(hideSeriesOfStampShipmentDetail, objShipmentDetail);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objShipmentDetail.groupTo);
            setForceViewType(supplierAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);
            setForceViewType(intraAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);

            getPropertyDraw(nameBrandSupplierArticleSkuShipmentDetail, 0).forceViewType = ClassViewType.GRID;

            //getPropertyDraw(nameOriginCategoryArticleSkuShipmentDetail).forceViewType = ClassViewType.GRID;
            //getPropertyDraw(netWeightArticleSkuShipmentDetail).forceViewType = ClassViewType.GRID;

            PropertyObjectEntity oneArticleProperty = addPropertyObject(oneArticleSkuShipmentDetail, objShipmentDetail);
            PropertyObjectEntity oneSkuProperty = addPropertyObject(oneSkuShipmentDetail, objShipmentDetail);
            PropertyObjectEntity oneArticleColorProperty = addPropertyObject(oneArticleColorShipmentDetail, objShipmentDetail);
            PropertyObjectEntity oneArticleSizeProperty = addPropertyObject(oneArticleSizeShipmentDetail, objShipmentDetail);

            getPropertyDraw(nameCategoryArticleSkuShipmentDetail).setPropertyHighlight(oneArticleProperty);
            getPropertyDraw(nameUnitOfMeasureArticleSkuShipmentDetail).setPropertyHighlight(oneArticleProperty);
            getPropertyDraw(netWeightSkuShipmentDetail).setPropertyHighlight(oneArticleSizeProperty);
            getPropertyDraw(netWeightSkuShipmentDetail).eventSID = WeightDaemonTask.SCALES_SID;
            getPropertyDraw(nameCountryOfOriginSkuShipmentDetail).setPropertyHighlight(oneArticleColorProperty);
            getPropertyDraw(mainCompositionOriginSkuShipmentDetail).setPropertyHighlight(oneArticleColorProperty);
            getPropertyDraw(additionalCompositionOriginSkuShipmentDetail).setPropertyHighlight(oneArticleColorProperty);

            if (!box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue)));

            if (box)
                addFixedFilter(new NotFilterEntity(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue))));

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
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        new NotNullFilterEntity(addPropertyObject(invoicedShipmentSku, objShipment, objSku)),
                        "Ожидается в поставке"));
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
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        new NotNullFilterEntity(addPropertyObject(invoicedShipmentSku, objShipment, objSku)),
                        "Ожидается в поставке"));
            }
            filterGroup.defaultFilter = 0;
            addRegularFilterGroup(filterGroup);

            RegularFilterGroupEntity filterGroup2 = new RegularFilterGroupEntity(genID());
            filterGroup2.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(shipmentShipmentDetail, objShipmentDetail), Compare.EQUALS, objShipment),
                    "В поставке",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroup2.defaultFilter = 0;
            addRegularFilterGroup(filterGroup2);

            RegularFilterGroupEntity filterGroup4 = new RegularFilterGroupEntity(genID());
            filterGroup4.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(userShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(baseLM.currentUser)),
                    "Пользователя",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroup4);

            RegularFilterGroupEntity filterGroup5 = new RegularFilterGroupEntity(genID());
            filterGroup5.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(stockShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(currentFreightBoxRoute, objRoute)),
                    "В коробе для трансп.",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroup5);

            RegularFilterGroupEntity filterGroupDiffShipment = new RegularFilterGroupEntity(genID());
            filterGroupDiffShipment.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(diffShipmentSku, objShipment, objSku)),
                    "Отличающиеся в поставке",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroupDiffShipment);

            if (box) {
                RegularFilterGroupEntity filterGroup3 = new RegularFilterGroupEntity(genID());
                filterGroup3.addFilter(new RegularFilterEntity(genID(),
                        new CompareFilterEntity(addPropertyObject(supplierBoxShipmentDetail, objShipmentDetail), Compare.EQUALS, objSupplierBox),
                        "В коробе поставщика",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
                addRegularFilterGroup(filterGroup3);

                RegularFilterGroupEntity filterGroupDiffBox = new RegularFilterGroupEntity(genID());
                filterGroupDiffBox.addFilter(new RegularFilterEntity(genID(),
                        new NotNullFilterEntity(addPropertyObject(diffListShipSku, objSupplierBox, objShipment, objSku)),
                        "Отличающиеся в коробе",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
                addRegularFilterGroup(filterGroupDiffBox);
            }

            addActionsOnObjectChange(objBarcode, addPropertyObject(baseLM.apply));

            if (box)
                addActionsOnObjectChange(objBarcode, addPropertyObject(
                        addJProp(and(true, false),
                                    addMFAProp(null, "Поиск по артикулу",
                                    findItemFormBoxBarcode,
                                    new ObjectEntity[]{findItemFormBoxBarcode.objShipment, findItemFormBoxBarcode.objBarcode, findItemFormBoxBarcode.objSupplierBox},
                                    false), 1, 2, 3,
                                    skuBarcodeObject, 2, emptyBarcodeShipment, 1),
                                objShipment, objBarcode, objSupplierBox));
            else
                addActionsOnObjectChange(objBarcode, addPropertyObject(
                        addJProp(and(true, false),
                                    addMFAProp(null, "Поиск по артикулу",
                                    findItemFormSimpleBarcode,
                                    new ObjectEntity[]{findItemFormSimpleBarcode.objShipment, findItemFormSimpleBarcode.objBarcode},
                                    false), 1, 2,
                                    skuBarcodeObject, 2, emptyBarcodeShipment, 1),
                                objShipment, objBarcode));

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSeekPallet, objBarcode));
            //addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionCheckPallet, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSeekFreightBox, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetPallet, objBarcode));

            if (box)
                addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetStore, objBarcode, objSupplierBox));

//            addActionsOnObjectChange(objBarcode, addPropertyObject(seekRouteToFillShipmentBarcode, objShipment, objBarcode));
//            if (box)
//                addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction4, objSupplierBox, objShipment, objRoute, objBarcode));
//            else
//                addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction3, objShipment, objRoute, objBarcode));

//
//            addActionsOnObjectChange(objBarcode, addPropertyObject(
//                    addJProp(true, baseLM.and1, addJProp(true, baseLM.equalsObjectBarcode, skuShipmentDetail, 1, 2), 1, 2, noBarcodeSupplier, 3),
//                    objShipmentDetail, objBarcode, objSupplier
//            ));

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
                                            false),
                                    1, 2,
                                    skuBarcodeObject, 2
                            ),
                            objSupplier, objBarcode));

            addActionsOnObjectChange(objBarcode, addPropertyObject(
                    addJProp(true, seekRouteShipmentSkuRoute,
                            1, skuBarcodeObject, 2, 3),
                    objShipment, objBarcode, objRoute));

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionCheckFreightBox, objRoute, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionCheckChangedFreightBox, objRoute, objBarcode));

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

            if (box) {
                setReadOnly(objSupplierBox, true);

                findItemBox = addPropertyDraw(addMFAProp(null, "Поиск по артикулу",
                            findItemFormBox,
                                                 new ObjectEntity[]{findItemFormBox.objShipment, findItemFormBox.objSupplierBox, findItemFormBox.objSku},
                                                 false),
                                                 objShipment, objSupplierBox, objSku);
                ((FormActionProperty)findItemBox.propertyObject.property).seekOnOk.add(findItemFormBox.objSku);
                ((FormActionProperty)findItemBox.propertyObject.property).seekOnOk.add(findItemFormBox.objShipmentDetail);
                findItemBox.forceViewType = ClassViewType.PANEL;
            } else {
                findItemSimple = addPropertyDraw(addMFAProp(null, "Поиск по артикулу",
                            findItemFormSimple,
                                                 new ObjectEntity[]{findItemFormSimple.objShipment, findItemFormSimple.objSku},
                                                 false),
                                                 objShipment, objSku);
                ((FormActionProperty)findItemSimple.propertyObject.property).seekOnOk.add(findItemFormSimple.objSku);
                ((FormActionProperty)findItemSimple.propertyObject.property).seekOnOk.add(findItemFormSimple.objShipmentDetail);
                findItemSimple.forceViewType = ClassViewType.PANEL;
            }

            addHintsIncrementTable(priceInShipmentStockSku, priceInInvoiceShipmentStockSku, priceInInvoiceStockSku, quantityInvoiceStockSku, quantityInvoiceShipmentStockSku);
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

            ContainerView supplierRow1 = design.createContainer();
            supplierRow1.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            supplierRow1.add(design.get(getPropertyDraw(originalNameArticleSkuShipmentDetail)));
            supplierRow1.add(design.get(getPropertyDraw(sidCustomCategoryOriginArticleSkuShipmentDetail)));
            supplierRow1.add(design.get(getPropertyDraw(nameCountryOfOriginArticleSkuShipmentDetail)));

            ContainerView supplierRow2 = design.createContainer();
            supplierRow2.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            supplierRow2.add(design.get(getPropertyDraw(nameBrandSupplierArticleSkuShipmentDetail, 1)));
            supplierRow2.add(design.get(getPropertyDraw(netWeightArticleSkuShipmentDetail)));
            supplierRow2.add(design.get(getPropertyDraw(mainCompositionOriginArticleSkuShipmentDetail)));

            ContainerView supplierContainer = design.getGroupPropertyContainer(objShipmentDetail.groupTo, supplierAttributeGroup);
            supplierContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            supplierContainer.add(supplierRow1);
            supplierContainer.add(supplierRow2);

            ContainerView intraRow1 = design.createContainer();
            intraRow1.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            intraRow1.add(design.get(getPropertyDraw(sidGenderArticleSkuShipmentDetail)));
            intraRow1.add(design.get(getPropertyDraw(nameCategoryArticleSkuShipmentDetail)));
            intraRow1.add(design.get(getPropertyDraw(nameTypeFabricArticleSkuShipmentDetail)));
            intraRow1.add(design.get(getPropertyDraw(nameUnitOfMeasureArticleSkuShipmentDetail)));
            intraRow1.add(design.get(getPropertyDraw(netWeightSkuShipmentDetail)));
            intraRow1.add(design.get(getPropertyDraw(nameCountryOfOriginSkuShipmentDetail)));

            ContainerView intraRow2 = design.createContainer();
            intraRow2.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            intraRow2.add(design.get(getPropertyDraw(mainCompositionOriginSkuShipmentDetail)));
            intraRow2.add(design.get(getPropertyDraw(additionalCompositionOriginSkuShipmentDetail)));
            intraRow2.add(design.get(getPropertyDraw(coefficientArticleSkuShipmentDetail)));

            ContainerView intraRow3 = design.createContainer();
            intraRow3.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            intraRow3.add(design.get(getPropertyDraw(sidStampShipmentDetail)));
            intraRow3.add(design.get(getPropertyDraw(seriesOfStampShipmentDetail)));

            ContainerView intraContainer = design.getGroupPropertyContainer(objShipmentDetail.groupTo, intraAttributeGroup);
            intraContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            intraContainer.add(intraRow1);
            intraContainer.add(intraRow2);
            intraContainer.add(intraRow3);

            design.setHighlightColor(new Color(255, 128, 128));

            if (box)
                design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(this.findItemBox));
            else
                design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(this.findItemSimple));

            return design;
        }
    }

    /************/

    private class ShipmentExportFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objShipment;
        private ObjectEntity objArticle;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objColorSupplier;

        private ShipmentExportFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);

            objShipment = addSingleGroupObject(shipment, "Поставка", baseLM.date, sidDocument);

            //objArticle = addSingleGroupObject(article, "Артикул", sidArticle);

            //objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", baseLM.selection, sidColorSupplier, baseLM.name);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", sidSizeSupplier);

            GroupObjectEntity gobjArticleColor= new GroupObjectEntity(genID());
            objArticle = new ObjectEntity(genID(), articleComposite, "Артикул");
            objColorSupplier = new ObjectEntity(genID(), colorSupplier, "Цвет");
            gobjArticleColor.add(objArticle);
            gobjArticleColor.add(objColorSupplier);
            addGroup(gobjArticleColor);

            addPropertyDraw(objArticle, sidArticle);
            addPropertyDraw(objColorSupplier, sidColorSupplier, baseLM.name);
            addPropertyDraw(objArticle, objColorSupplier, mainCompositionOriginArticleColor, additionalCompositionOriginArticleColor);
            addPropertyDraw(objShipment, objArticle, objColorSupplier, sumShipmentArticleColor);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityShipmentArticleColorSize, objShipment, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.columnGroupObjects.add(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(sidSizeSupplier, objSizeSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objShipment), Compare.EQUALS, objSupplier));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityShipmentArticleColor, objShipment, objArticle, objColorSupplier)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityShipmentSize, objShipment, objSizeSupplier)));

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            //design.get(getPropertyDraw(sidDocument, objDirectInvoice)).caption = "Номер инвойса";
            //design.get(getPropertyDraw(baseLM.date, objDirectInvoice)).caption = "Дата инвойса";

            //design.get(objShipment.groupTo).grid.constraints.fillVertical = 1;
            //design.get(objSku.groupTo).grid.constraints.fillVertical = 4;

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                    design.getGroupObjectContainer(objShipment.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }


    private class FreightShipmentStoreFormEntity extends BarcodeFormEntity {

        private ObjectEntity objFreight;
        private ObjectEntity objPallet;
        private ObjectEntity objFreightBox;

        private FreightShipmentStoreFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.objectValue, baseLM.date, baseLM.objectClassName, nameRouteFreight, nameExporterFreight, descriptionFreight, tonnageDataFreight, grossWeightFreight, volumeDataFreight, palletCountDataFreight, palletNumberFreight, freightBoxNumberFreight, nameCurrencyFreight, sumFreightFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objFreight, true);

            PropertyObjectEntity diffPalletFreightProperty = addPropertyObject(diffPalletFreight, objFreight);
            getPropertyDraw(palletCountDataFreight).setPropertyHighlight(diffPalletFreightProperty);
            getPropertyDraw(palletNumberFreight).setPropertyHighlight(diffPalletFreightProperty);

            objPallet = addSingleGroupObject(pallet, "Паллета", baseLM.barcode, grossWeightPallet, freightBoxNumberPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objPallet, true);
            setReadOnly(grossWeightPallet, false);

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetFreight, objBarcode, objFreight));

            addPropertyDraw(equalsPalletFreight, objPallet, objFreight);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightPallet, objPallet), Compare.EQUALS, objFreight));

            objFreightBox = addSingleGroupObject(freightBox, "Короб", baseLM.barcode, netWeightStock);
            objFreightBox.groupTo.setSingleClassView(ClassViewType.GRID);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(palletFreightBox, objFreightBox), Compare.EQUALS, objPallet));

        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.blockedScreen.put("changePropertyDraw", getPropertyDraw(baseLM.objectValue, objBarcode).getID() + "");

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.get(objFreight.groupTo).grid.constraints.fillVertical = 1;
            design.get(objPallet.groupTo).grid.constraints.fillVertical = 1;
            design.get(objFreightBox.groupTo).grid.constraints.fillVertical = 2;

            design.setHighlightColor(new Color(128, 255, 128));

            return design;
        }
    }

    private class BoxPalletStoreFormEntity extends BarcodeFormEntity {

        private ObjectEntity objBox;
        private ObjectEntity objPallet;
        private ObjectEntity objShipment;
        private ObjectEntity objSku;

        private BoxPalletStoreFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objPallet = addSingleGroupObject(pallet, "Паллета", baseLM.barcode, nameRouteCreationPalletPallet, grossWeightPallet, freightBoxNumberPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objPallet, true);
            setReadOnly(grossWeightPallet, false);

            objBox = addSingleGroupObject(freightBox, "Короб для транспортировки", baseLM.barcode, nameRouteCreationFreightBoxFreightBox, quantityStock, netWeightStock);
            objBox.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objBox, true);

            addPropertyDraw(equalsPalletFreightBox, objBox, objPallet);

            GroupObjectEntity gobjShipmentSku = new GroupObjectEntity(genID());
            objShipment = new ObjectEntity(genID(), shipment, "Поставка");
            objSku = new ObjectEntity(genID(), sku, "SKU");
            gobjShipmentSku.add(objShipment);
            gobjShipmentSku.add(objSku);
            addGroup(gobjShipmentSku);

            addPropertyDraw(objShipment, baseLM.date, sidDocument);
            addPropertyDraw(objSku, sidArticleSku, nameArticleSku, nameBrandSupplierArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            addPropertyDraw(quantityShipmentStockSku, objShipment, objBox, objSku);
            setReadOnly(objSku, true);

            addActionsOnObjectChange(objBarcode, addPropertyObject(baseLM.seekBarcodeAction, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetPalletFreightBox, objBarcode, objPallet));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new OrFilterEntity(new CompareFilterEntity(addPropertyObject(palletFreightBox, objBox), Compare.EQUALS, objPallet),
                            new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(palletFreightBox, objBox)))),
                    "Не расписанные короба или в текущей паллете",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(palletFreightBox, objBox), Compare.EQUALS, objPallet),
                    "В текущей паллете",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.defaultFilter = 0;
            addRegularFilterGroup(filterGroup);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStock, objBox)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityShipmentStockSku, objShipment, objBox, objSku)));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            return design;
        }
    }

    private class FreightShipmentFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objPallet;
        private ObjectEntity objShipment;
        private ObjectEntity objDirectInvoice;
        private ObjectEntity objSku;

        private FreightShipmentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.date, baseLM.objectClassName, nameRouteFreight, nameExporterFreight, descriptionFreight, volumeDataFreight, tonnageDataFreight, grossWeightFreight, volumeDataFreight, palletCountDataFreight, palletNumberFreight, freightBoxNumberFreight, nameCurrencyFreight, sumFreightFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objFreight, true);
//            addObjectActions(this, objFreight);

            PropertyObjectEntity diffPalletFreightProperty = addPropertyObject(diffPalletFreight, objFreight);
            getPropertyDraw(palletCountDataFreight).setPropertyHighlight(diffPalletFreightProperty);
            getPropertyDraw(palletNumberFreight).setPropertyHighlight(diffPalletFreightProperty);

            objShipment = addSingleGroupObject(shipment, "Поставка", baseLM.date, sidDocument, nameSupplierDocument);

            addPropertyDraw(objShipment, baseLM.date, sidDocument, nameSupplierDocument);
            addPropertyDraw(objShipment, objFreight, nameImporterShipmentFreight);

            objPallet = addSingleGroupObject(pallet, "Паллета", baseLM.barcode, grossWeightPallet, freightBoxNumberPallet, nameRouteCreationPalletPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objPallet, true);

            addPropertyDraw(equalsPalletFreight, objPallet, objFreight);

            objSku = addSingleGroupObject(sku, "SKU", sidArticleSku, nameArticleSku, nameBrandSupplierArticleSku, sidColorSupplierItem, sidSizeSupplierItem);
            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            addPropertyDraw(quantityPalletSku, objPallet, objSku);
            setReadOnly(objSku, true);

            objDirectInvoice = addSingleGroupObject(directInvoice, "Инвойс напрямую", baseLM.date, sidDocument, nameImporterDirectInvoice, nameDestinationDestinationDocument, grossWeightDirectInvoice, palletNumberDirectInvoice);
            setReadOnly(objDirectInvoice, true);
            setReadOnly(grossWeightDirectInvoice, false);
            setReadOnly(palletNumberDirectInvoice, false);

            addPropertyDraw(equalsDirectInvoiceFreight, objDirectInvoice, objFreight);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(routeCreationPalletPallet, objPallet), Compare.EQUALS, addPropertyObject(routeFreight, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(freightBoxNumberPallet, objPallet)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityShipmentFreight, objShipment, objFreight)));
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

            freightCompleteFA = addMFAProp(actionGroup, "Скомплектовать", this, new ObjectEntity[] {objFreight}, true,
                    addPropertyObject(addJProp(true, executeChangeFreightClass, 1, addCProp(baseClass.objectClass, "freightComplete")), objFreight));
            freightCompleteFA.setImage("arrow_right.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidDocument, objDirectInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(baseLM.date, objDirectInvoice)).caption = "Дата инвойса";
            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.get(objShipment.groupTo).grid.constraints.fillVertical = 1;
            design.get(objPallet.groupTo).grid.constraints.fillVertical = 1;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 2;

            design.addIntersection(design.getGroupObjectContainer(objShipment.groupTo),
                    design.getGroupObjectContainer(objDirectInvoice.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objPallet.groupTo),
                    design.getGroupObjectContainer(objDirectInvoice.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSku.groupTo),
                    design.getGroupObjectContainer(objDirectInvoice.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);



            design.setHighlightColor(new Color(128, 255, 128));

            return design;
        }
    }

    private class CreateFreightBoxFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objFreightBox;

        private CreateFreightBoxFormEntity(NavigatorElement parent, String sID, String caption, FormType type) {
            super(parent, sID, caption, type.equals(FormType.PRINT));

            objCreate = addSingleGroupObject(1, "creationFreightBox", creationFreightBox, "Документ генерации коробов");

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
                objCreate.setAddOnTransaction();

            objFreightBox = addSingleGroupObject(2, "freightBox", freightBox, "Короба для транспортировки", baseLM.barcode);
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

            objCreate = addSingleGroupObject(1, "creationPallet", creationPallet, "Документ генерации паллет");
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
                objCreate.setAddOnTransaction();

            objPallet = addSingleGroupObject(2, "pallet", pallet, "Паллеты для транспортировки", baseLM.barcode);
            setReadOnly(objPallet, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationPalletPallet, objPallet), Compare.EQUALS, objCreate));

            if (type.equals(FormType.PRINT))
                printCreatePalletForm = addFAProp("Печать штрих-кодов", this, objCreate);
        }
    }


    private class CreateSkuFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objSku;

        private CreateSkuFormEntity(NavigatorElement parent, String sID, String caption, FormType type) {
            super(parent, sID, caption, type.equals(FormType.PRINT));

            objCreate = addSingleGroupObject(creationSku, "Документ генерации товаров");
            if (!type.equals(FormType.ADD))
                addPropertyDraw(objCreate, baseLM.objectValue);

            addPropertyDraw(objCreate, quantityCreationSku);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createSku, objCreate);

            if (!type.equals(FormType.PRINT))
                addPropertyDraw(objCreate, printCreateSkuForm);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.ADD))
                objCreate.setAddOnTransaction();

            objSku = addSingleGroupObject(sku, "Товары", baseLM.barcode);
            setReadOnly(objSku, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationSkuSku, objSku), Compare.EQUALS, objCreate));

            if (type.equals(FormType.PRINT))
                printCreateSkuForm = addFAProp("Печать штрих-кодов", this, objCreate);
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

            addPropertyDraw(objCreate, seriesOfCreationStamp, firstNumberCreationStamp, lastNumberCreationStamp, dateOfCreationStamp);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createStamp, objCreate);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.LIST))
                addPropertyDraw(objCreate, baseLM.delete);

            if (type.equals(FormType.ADD))
                objCreate.setAddOnTransaction();

            objStamp = addSingleGroupObject(stamp, "Таможенные марки", sidStamp, baseLM.delete);
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
        private ObjectEntity objSubCategory;
        private ObjectEntity objCountry;
        private ObjectEntity objTypeDuty; 

        private PropertyDrawEntity import1;
        private PropertyDrawEntity import2;
        private PropertyDrawEntity importMinPrices;
        private PropertyDrawEntity importDuties;

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

            objCustomCategory10 = addSingleGroupObject(customCategory10, "Четвёртый уровень", sidCustomCategory10, nameCustomCategory, certificatedCustomCategory10);
            addObjectActions(this, objCustomCategory10);

            objSubCategory = addSingleGroupObject(subCategory, "Дополнительное деление", nameSubCategory);
            addObjectActions(this, objSubCategory);

            objCountry = addSingleGroupObject(baseLM.country, "Страна производства", baseLM.name);

            objTypeDuty = addSingleGroupObject(typeDuty, "Пошлины, НДС, платежи", sidTypeDuty, nameTypeDuty);
            addObjectActions(this, objTypeDuty);

            objCustomCategory4Origin = addSingleGroupObject(customCategory4, "Первый уровень", sidCustomCategory4, nameCustomCategory);
            if (tree) {
                addPropertyDraw(baseLM.dumb1, objCustomCategory4Origin);
                addPropertyDraw(baseLM.dumb2, objCustomCategory10, objCustomCategory4Origin);
            }
            else
                addObjectActions(this, objCustomCategory4Origin);

            objCustomCategory6Origin = addSingleGroupObject(customCategory6, "Второй уровень", sidCustomCategory6, nameCustomCategory);
            if (tree) {
                addPropertyDraw(baseLM.dumb1, objCustomCategory6Origin);
                addPropertyDraw(baseLM.dumb2, objCustomCategory10, objCustomCategory6Origin);
            }
            else
                addObjectActions(this, objCustomCategory6Origin);

            import1 = addPropertyDraw(importBelTnved);
            import2 = addPropertyDraw(importEuTnved);
            importMinPrices = addPropertyDraw(importTnvedCountryMinPrices);
            importDuties = addPropertyDraw(importTnvedDuty);

            objCustomCategoryOrigin = addSingleGroupObject(customCategoryOrigin, "ЕС уровень", sidCustomCategoryOrigin, nameCustomCategory, sidCustomCategory10CustomCategoryOrigin);
            addObjectActions(this, objCustomCategoryOrigin, objCustomCategory6Origin, customCategory6);

            addPropertyDraw(relationCustomCategory10SubCategory, objCustomCategory10, objSubCategory);
            addPropertyDraw(minPriceCustomCategory10SubCategory, objCustomCategory10, objSubCategory);
            addPropertyDraw(minPriceCustomCategory10SubCategoryCountry, objCustomCategory10, objSubCategory, objCountry);

            addPropertyDraw(dutyPercentCustomCategory10TypeDuty, objCustomCategory10, objTypeDuty);
            addPropertyDraw(dutySumCustomCategory10TypeDuty, objCustomCategory10, objTypeDuty);

            addPropertyDraw(relationCustomCategory10CustomCategoryOrigin, objCustomCategory10, objCustomCategoryOrigin);

            if (tree) {
                treeCustomCategory = addTreeGroupObject(objCustomCategory4.groupTo, objCustomCategory6.groupTo, objCustomCategory9.groupTo);
                treeCustomCategoryOrigin = addTreeGroupObject(objCustomCategory4Origin.groupTo, objCustomCategory6Origin.groupTo);

                treeCustomCategory.plainTreeMode = true;
            }

            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory4CustomCategory6, objCustomCategory6), Compare.EQUALS, objCustomCategory4));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory6CustomCategory9, objCustomCategory9), Compare.EQUALS, objCustomCategory6));
            //addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory9CustomCategory10, objCustomCategory10), Compare.EQUALS, objCustomCategory9));
            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(customCategory9CustomCategory10, objCustomCategory10), Compare.EQUALS, objCustomCategory9),
                    "от 3 уровня",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(customCategory6CustomCategory10, objCustomCategory10), Compare.EQUALS, objCustomCategory6),
                    "от 2 уровня",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(customCategory4CustomCategory10, objCustomCategory10), Compare.EQUALS, objCustomCategory4),
                    "от 1 уровня",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.defaultFilter = 0;
            addRegularFilterGroup(filterGroup);


            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory4CustomCategory6, objCustomCategory6Origin), Compare.EQUALS, objCustomCategory4Origin));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(customCategory6CustomCategoryOrigin, objCustomCategoryOrigin), Compare.EQUALS, objCustomCategory6Origin));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView categoryContainer = design.createContainer("Классификаторы");

            if (tree)
            {
                design.getTreeContainer(treeCustomCategory).setTitle("Белорусский классификатор");
                design.getTreeContainer(treeCustomCategoryOrigin).setTitle("Европейский классификатор");

                categoryContainer.add(design.getTreeContainer(treeCustomCategory));
                categoryContainer.add(design.getGroupObjectContainer(objCustomCategory10.groupTo));
                categoryContainer.add(design.getTreeContainer(treeCustomCategoryOrigin));
                categoryContainer.add(design.getGroupObjectContainer(objCustomCategoryOrigin.groupTo));

                design.addIntersection(design.getTreeContainer(treeCustomCategory), design.getGroupObjectContainer(objCustomCategory10.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getTreeContainer(treeCustomCategory), design.getGroupObjectContainer(objCustomCategoryOrigin.groupTo), DoNotIntersectSimplexConstraint.TOTHE_BOTTOM);
                design.addIntersection(design.getTreeContainer(treeCustomCategoryOrigin), design.getGroupObjectContainer(objCustomCategoryOrigin.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                                
                design.get(treeCustomCategoryOrigin).constraints.fillHorizontal = 1;
                design.get(treeCustomCategory).constraints.fillHorizontal = 1;
                design.get(objCustomCategory10.groupTo).grid.constraints.fillHorizontal = 2;
                design.get(objCustomCategoryOrigin.groupTo).grid.constraints.fillHorizontal = 2;
            } else {

                design.addIntersection(design.getGroupObjectContainer(objCustomCategory4.groupTo), design.getGroupObjectContainer(objCustomCategory6.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objCustomCategory6.groupTo), design.getGroupObjectContainer(objCustomCategory9.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objCustomCategory9.groupTo), design.getGroupObjectContainer(objCustomCategory10.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

                design.addIntersection(design.getGroupObjectContainer(objCustomCategory4Origin.groupTo), design.getGroupObjectContainer(objCustomCategory6Origin.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objCustomCategory6Origin.groupTo), design.getGroupObjectContainer(objCustomCategoryOrigin.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

                categoryContainer.add(design.getGroupObjectContainer(objCustomCategory4.groupTo));
                categoryContainer.add(design.getGroupObjectContainer(objCustomCategory6.groupTo));
                categoryContainer.add(design.getGroupObjectContainer(objCustomCategory9.groupTo));
                categoryContainer.add(design.getGroupObjectContainer(objCustomCategory10.groupTo));
                categoryContainer.add(design.getGroupObjectContainer(objCustomCategory4Origin.groupTo));
                categoryContainer.add(design.getGroupObjectContainer(objCustomCategory6Origin.groupTo));
                categoryContainer.add(design.getGroupObjectContainer(objCustomCategoryOrigin.groupTo));

                design.get(objCustomCategory4Origin.groupTo).grid.constraints.fillHorizontal = 2;
                design.get(objCustomCategoryOrigin.groupTo).grid.constraints.fillHorizontal = 2;
            }

            design.addIntersection(design.get(import1), design.get(import2), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.get(import2), design.get(importMinPrices), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.get(importMinPrices), design.get(importDuties), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSubCategory.groupTo), design.getGroupObjectContainer(objCountry.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objCountry.groupTo), design.getGroupObjectContainer(objTypeDuty.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView customContainer = design.createContainer("Таможенные пошлины, акцизы, минимальные цены");
            customContainer.add(design.getGroupObjectContainer(objSubCategory.groupTo));
            customContainer.add(design.getGroupObjectContainer(objCountry.groupTo));
            customContainer.add(design.getGroupObjectContainer(objTypeDuty.groupTo));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addBefore(specContainer, design.get(import1));
            specContainer.add(categoryContainer);
            specContainer.add(customContainer);
            specContainer.tabbedPane = true;              
                                                         
            return design;
        }
    }

    private class BalanceWarehouseFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSku;

        private BalanceWarehouseFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.selection, baseLM.barcode, nameSupplierArticleSku, nameBrandSupplierArticleSku, nameThemeSupplierArticleSku, sidSeasonSupplierArticleSku,
                    nameCategoryArticleSku, sidArticleSku, nameArticleSku, sidCustomCategory10Sku,
                    sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameCountrySku, netWeightSku,
                    mainCompositionSku, additionalCompositionSku, quantityDirectInvoicedSku, quantityStockedSku, quantitySku, sumSku);
            addObjectActions(this, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            setReadOnly(objSku, true);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(quantitySku, objSku), Compare.GREATER, addPropertyObject(baseLM.vzero)),
                    "Ненулевые остатки",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(quantitySku, objSku), Compare.EQUALS,addPropertyObject( baseLM.vzero)),
                    "Нулевые остатки",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.defaultFilter = 0;
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            return design;
        }
    }


    private class BalanceWarehousePeriodFormEntity extends DateIntervalFormEntity<RomanBusinessLogics> {

        private ObjectEntity objBrand;
        private ObjectEntity objSku;

        private BalanceWarehousePeriodFormEntity(NavigatorElement parent, String sID, String caption) {
            super(baseLM, parent, sID, caption);

            objBrand = addSingleGroupObject(4, brandSupplier, "Бренд", baseLM.name);

            addPropertyDraw(invoicedBetweenDateBrandSupplier, objBrand, objDateFrom, objDateTo);
            addPropertyDraw(quantityShipmentedBetweenDateBrandSupplier, objBrand, objDateFrom, objDateTo);

            objSku = addSingleGroupObject(5, sku, "SKU", baseLM.selection, baseLM.barcode, nameCategoryArticleSku, sidArticleSku,
                    sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            //addObjectActions(this, objSku);

            addPropertyDraw(invoicedBetweenDateSku, objSku, objDateFrom, objDateTo);
            addPropertyDraw(quantityShipmentedBetweenDateSku, objSku, objDateFrom, objDateTo);
            addPropertyDraw(quantityFreightedBetweenDateSku, objSku, objDateFrom, objDateTo);

            addPropertyDraw(balanceSku, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            setReadOnly(objSku, true);

            addFixedFilter(new OrFilterEntity(
                               new CompareFilterEntity(addPropertyObject(invoicedBetweenDateBrandSupplier, objBrand, objDateFrom, objDateTo), Compare.GREATER, addPropertyObject(baseLM.vzero)),
                               new CompareFilterEntity(addPropertyObject(quantityShipmentedBetweenDateBrandSupplier, objBrand, objDateFrom, objDateTo), Compare.GREATER, addPropertyObject(baseLM.vzero))));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(brandSupplierArticleSku, objSku), Compare.EQUALS, objBrand));

            addFixedFilter(new OrFilterEntity(
                               new CompareFilterEntity(addPropertyObject(invoicedBetweenDateSku, objSku, objDateFrom, objDateTo), Compare.GREATER, addPropertyObject(baseLM.vzero)),
                               new CompareFilterEntity(addPropertyObject(quantityShipmentedBetweenDateSku, objSku, objDateFrom, objDateTo), Compare.GREATER, addPropertyObject(baseLM.vzero))));

            /*RegularFilterGroupEntity filterGroupBrand = new RegularFilterGroupEntity(genID());
            filterGroupBrand.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(brandSupplierArticleSku, objSku), Compare.EQUALS, objBrand),
                    "Текущего бренда",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroupBrand.defaultFilter = 0;
            addRegularFilterGroup(filterGroupBrand);*/

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.addIntersection(design.getGroupObjectContainer(objBrand.groupTo),
                    design.getGroupObjectContainer(objSku.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objBrand.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objSku.groupTo).grid.constraints.fillHorizontal = 3;

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

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle, nameSupplierArticle, nameBrandSupplierArticle, nameThemeSupplierArticle, sidGenderArticle, nameCategoryArticle, nameTypeFabricArticle, nameArticle);
            addObjectActions(this, objArticle);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.selection, baseLM.barcode, nameSupplierArticleSku, nameBrandSupplierArticleSku, nameThemeSupplierArticleSku, sidGenderArticleSku, sidSeasonSupplierArticleSku,
                    nameCategoryArticleSku, nameTypeFabricArticleSku, sidArticleSku, nameArticleSku,
                    sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem, nameCommonSizeSku,
                    nameCountrySku, netWeightSku,
                    mainCompositionSku, additionalCompositionSku, quantityAllSku);
            addObjectActions(this, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            RegularFilterGroupEntity filterGroupSupplierSku = new RegularFilterGroupEntity(genID());
            filterGroupSupplierSku.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(supplierArticleSku, objSku), Compare.EQUALS, objSupplier),
                    "Текущего поставщика",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroupSupplierSku.defaultFilter = 0;
            addRegularFilterGroup(filterGroupSupplierSku);

            RegularFilterGroupEntity filterGroupCategorySku = new RegularFilterGroupEntity(genID());
            filterGroupCategorySku.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(categoryArticleSku, objSku), Compare.EQUALS, objCategory),
                    "Текущей номенклатурной группы",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroupCategorySku.defaultFilter = 0;
            addRegularFilterGroup(filterGroupCategorySku);

            RegularFilterGroupEntity filterGroupSupplierArticle = new RegularFilterGroupEntity(genID());
            filterGroupSupplierArticle.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, objSupplier),
                    "Текущего поставщика",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroupSupplierArticle.defaultFilter = 0;
            addRegularFilterGroup(filterGroupSupplierArticle);

            RegularFilterGroupEntity filterGroupCategoryArticle = new RegularFilterGroupEntity(genID());
            filterGroupCategoryArticle.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(categoryArticle, objArticle), Compare.EQUALS, objCategory),
                    "Текущей номенклатурной группы",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroupCategoryArticle.defaultFilter = 0;
            addRegularFilterGroup(filterGroupCategoryArticle);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

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

    private class CommonSizeEditFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objCategory;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objGender;
        private ObjectEntity objCommonSize;

        private GroupObjectEntity gobjGenderSizeSupplier;

        private CommonSizeEditFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objCategory = addSingleGroupObject(category, "Номенклатурная группа", baseLM.name);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);

            gobjGenderSizeSupplier = new GroupObjectEntity(genID());
            objGender = new ObjectEntity(genID(), gender, "Пол");
            objSizeSupplier = new ObjectEntity(genID(), sizeSupplier, "Размер");
            gobjGenderSizeSupplier.add(objGender);
            gobjGenderSizeSupplier.add(objSizeSupplier);
            addGroup(gobjGenderSizeSupplier);

            addPropertyDraw(objGender, sidGender);
            addPropertyDraw(objSizeSupplier, sidSizeSupplier, nameSupplierSizeSupplier);
            addPropertyDraw(objSizeSupplier, objGender, objCategory, nameCommonSizeSizeSupplierGenderCategory);

            objCommonSize = addSingleGroupObject(commonSize, "Унифицированный размер", baseLM.name);
            addObjectActions(this, objCommonSize);

            RegularFilterGroupEntity filterGroupSupplierSize = new RegularFilterGroupEntity(genID());
            filterGroupSupplierSize.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, objSupplier),
                    "Текущего поставщика",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroupSupplierSize.defaultFilter = 0;
            addRegularFilterGroup(filterGroupSupplierSize);

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidGender, objGender)).caption = "Пол";
            design.get(getPropertyDraw(sidSizeSupplier, objSizeSupplier)).caption = "Размер";

            design.get(gobjGenderSizeSupplier).grid.constraints.fillVertical = 2;

            design.addIntersection(design.getGroupObjectContainer(objCategory.groupTo),
                    design.getGroupObjectContainer(objSupplier.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(gobjGenderSizeSupplier),
                    design.getGroupObjectContainer(objCommonSize.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class CommonSizeImportFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCategory;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objGender;

        private GroupObjectEntity gobjCategoryGenderSizeSupplier;

        private CommonSizeImportFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            gobjCategoryGenderSizeSupplier = new GroupObjectEntity(genID());
            objCategory = new ObjectEntity(genID(), category, "Номенклатурная группа");
            objGender = new ObjectEntity(genID(), gender, "Пол");
            objSizeSupplier = new ObjectEntity(genID(), sizeSupplier, "Размер поставщика");
            gobjCategoryGenderSizeSupplier.add(objCategory);
            gobjCategoryGenderSizeSupplier.add(objGender);
            gobjCategoryGenderSizeSupplier.add(objSizeSupplier);
            addGroup(gobjCategoryGenderSizeSupplier);

            addPropertyDraw(objCategory, baseLM.name);
            addPropertyDraw(objGender, sidGender);
            addPropertyDraw(objSizeSupplier, sidSizeSupplier, nameSupplierSizeSupplier);
            addPropertyDraw(objSizeSupplier, objGender, objCategory, nameCommonSizeSizeSupplierGenderCategory);
            addPropertyDraw(objSizeSupplier, objGender, objCategory, quantitySizeSupplierGenderCategory);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantitySizeSupplierGenderCategory, objSizeSupplier, objGender, objCategory), Compare.GREATER, addPropertyObject(baseLM.vzero)));

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            return design;
        }
    }

    private class ContractFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSeller;
        private ObjectEntity objContract;

        private ContractFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSeller = addSingleGroupObject(seller, "Продавец", baseLM.name, baseLM.objectClassName);

            objContract = addSingleGroupObject(contract, "Договор", sidContract, dateContract, baseLM.date, nameSubjectContract, nameCurrencyContract, conditionShipmentContract, conditionPaymentContract);
            addObjectActions(this, objContract);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(sellerContract, objContract), Compare.EQUALS, objSeller));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name, baseLM.dumb1, baseLM.dumb1, baseLM.dumb1);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд", baseLM.name, quantityBrandSupplier);

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
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

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
        private ObjectEntity objSku;
        private ObjectEntity objSku2;

        private InvoiceShipmentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);

            objInvoice = addSingleGroupObject(invoice, "Инвойс", baseLM.date, sidDocument, baseLM.objectClassName, quantityDocument, quantityInvoice);

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
                    "По отличающимся",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupDiffInvoice);

            RegularFilterGroupEntity filterGroupDiffBox = new RegularFilterGroupEntity(genID());
            filterGroupDiffBox.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(diffListSupplierBoxSku, objBox, objSku2))),
                    "По отличающимся",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroupDiffBox);

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

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
        private ObjectEntity objCategory2;
        private ObjectEntity objGender;
        private ObjectEntity objComposition;
        private ObjectEntity objTypeFabric;
        private ObjectEntity objSku;
        private ObjectEntity objSkuFreight;
        GroupObjectEntity gobjCategoryGenderCompositionTypeFabric;

        private RegularFilterGroupEntity filterGroupCategory;
        private RegularFilterGroupEntity filterGroupCustomCategory10;
        private RegularFilterGroupEntity filterGroupCountry;

        private FreightChangeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freightComplete, "Фрахт", baseLM.date, baseLM.objectClassName, nameRouteFreight, nameExporterFreight, nameFreightTypeFreight, grossWeightFreight, nameCurrencyFreight, sumFreightFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objFreight, true);

            objImporter = addSingleGroupObject(importer, "Импортёр", baseLM.name, addressSubject);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);

            objArticle = addSingleGroupObject(article, "Артикул", baseLM.selection, sidArticle, nameBrandSupplierArticle, originalNameArticle, nameCategoryArticle, nameArticle,
                    sidCustomCategoryOriginArticle, nameCountryOfOriginArticle, mainCompositionOriginArticle, additionalCompositionOriginArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(quantityFreightArticle, objFreight, objArticle);

            objCategory2 = addSingleGroupObject(category, "Номенклатурная группа", baseLM.name);

            gobjCategoryGenderCompositionTypeFabric = new GroupObjectEntity(genID(), "Группировка");
            objGender = new ObjectEntity(genID(), gender, "Пол");
            objComposition = new ObjectEntity(genID(), COMPOSITION_CLASS, "Состав");
            objTypeFabric = new ObjectEntity(genID(), typeFabric, "Тип одежды");

            gobjCategoryGenderCompositionTypeFabric.add(objGender);
            gobjCategoryGenderCompositionTypeFabric.add(objComposition);
            gobjCategoryGenderCompositionTypeFabric.add(objTypeFabric);
            addGroup(gobjCategoryGenderCompositionTypeFabric);

            addPropertyDraw(objGender, sidGender);
            addPropertyDraw(objComposition, baseLM.objectValue);
            addPropertyDraw(objTypeFabric, baseLM.name);
            addPropertyDraw(sidCustomCategory10CategoryGenderCompositionTypeFabric, objCategory2, objGender, objComposition, objTypeFabric);
            addPropertyDraw(quantityFreightCategoryGenderCompositionTypeFabric, objFreight, objCategory2, objGender, objComposition, objTypeFabric);

            objCategory = addSingleGroupObject(category, "Номенклатурная группа", baseLM.name);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.selection, baseLM.barcode, sidArticleSku,
                     nameBrandSupplierArticleSku, nameCategoryArticleSku, sidGenderArticleSku, nameTypeFabricArticleSku,
                     sidCustomCategoryOriginArticleSku, sidCustomCategory10Sku, nameSubCategoryDataSku, nameCountrySku, netWeightSku,
                     mainCompositionOriginSku, translationMainCompositionSku, mainCompositionSku,
                     additionalCompositionOriginSku, translationAdditionalCompositionSku, additionalCompositionSku);

            PropertyObjectEntity diffCountRelationCustomCategory10SkuProperty = addPropertyObject(diffCountRelationCustomCategory10Sku, objSku);
            getPropertyDraw(nameSubCategoryDataSku).setPropertyHighlight(diffCountRelationCustomCategory10SkuProperty);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            addPropertyDraw(addGCAProp(actionGroup, "translationAllMainComposition", "Перевод составов", objSku.groupTo, translationMainCompositionSku, baseLM.actionTrue), objSku).forceViewType = ClassViewType.PANEL;
            addPropertyDraw(addGCAProp(actionGroup, "translationAllAdditionalComposition", "Перевод доп. составов", objSku.groupTo, translationAdditionalCompositionSku, baseLM.actionTrue), objSku).forceViewType = ClassViewType.PANEL;

            setReadOnly(baseGroup, true, objSku.groupTo);
            setReadOnly(publicGroup, true, objSku.groupTo);
            setReadOnly(sidGenderArticleSku, false, objSku.groupTo);
            setReadOnly(nameTypeFabricArticleSku, false, objSku.groupTo);
            setReadOnly(sidCustomCategory10Sku, false, objSku.groupTo);
            setReadOnly(nameSubCategoryDataSku, false, objSku.groupTo);
            setReadOnly(nameCountrySku, false, objSku.groupTo);
            setReadOnly(netWeightSku, false, objSku.groupTo);
            setReadOnly(mainCompositionOriginSku, false, objSku.groupTo);
            setReadOnly(additionalCompositionOriginSku, false, objSku.groupTo);
            setReadOnly(mainCompositionSku, false, objSku.groupTo);
            setReadOnly(additionalCompositionSku, false, objSku.groupTo);

            objSkuFreight = addSingleGroupObject(sku, "Позиции фрахта", baseLM.selection, baseLM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem,
                    sidSizeSupplierItem, nameBrandSupplierArticleSku, nameArticleSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSkuFreight.groupTo);

            addPropertyDraw(quantityFreightSku, objFreight, objSku);
            addPropertyDraw(quantityDirectFreightSku, objFreight, objSku);
            addPropertyDraw(sidCustomCategory10FreightSku, objFreight, objSkuFreight);
            addPropertyDraw(nameSubCategoryFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(nameCountryOfOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(netWeightFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(grossWeightFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(mainCompositionOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(translationMainCompositionFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(mainCompositionFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(additionalCompositionOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(translationAdditionalCompositionFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(additionalCompositionFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(quantityImporterFreightSku, objImporter, objFreight, objSkuFreight);
            addPropertyDraw(quantityProxyImporterFreightSku, objImporter, objFreight, objSkuFreight);
            addPropertyDraw(quantityDirectImporterFreightSku, objImporter, objFreight, objSkuFreight);

            addPropertyDraw(addGCAProp(actionGroup, "translationAllFreightMainComposition", "Перевод составов (для фрахта)", objSkuFreight.groupTo, translationMainCompositionFreightSku, baseLM.actionTrue), objFreight, objSkuFreight).forceViewType = ClassViewType.PANEL;
            addPropertyDraw(addGCAProp(actionGroup, "translationAllFreightAdditionalComposition", "Перевод доп. составов (для фрахта)", objSkuFreight.groupTo, translationAdditionalCompositionFreightSku, baseLM.actionTrue), objFreight, objSkuFreight).forceViewType = ClassViewType.PANEL;
            
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightCategoryGenderCompositionTypeFabric, objFreight, objCategory2, objGender, objComposition, objTypeFabric)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightCategory, objFreight, objCategory)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightCategory, objFreight, objCategory2)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightSku, objFreight, objSku)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSkuFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightArticle, objFreight, objArticle)));

            filterGroupCategory = new RegularFilterGroupEntity(genID());
            filterGroupCategory.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(categoryArticleSku, objSku), Compare.EQUALS, objCategory),
                    "По номенклатурной группе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            filterGroupCategory.defaultFilter = 0;
            addRegularFilterGroup(filterGroupCategory);

            filterGroupCustomCategory10 = new RegularFilterGroupEntity(genID());
            filterGroupCustomCategory10.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(customCategory10Sku, objSku))),
                    "Без ТН ВЭД",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroupCustomCategory10);

            filterGroupCountry = new RegularFilterGroupEntity(genID());
            filterGroupCountry.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(countryOfOriginSku, objSku))),
                    "Без страны",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupCountry);

            RegularFilterGroupEntity filterGroupWeight = new RegularFilterGroupEntity(genID());
            filterGroupWeight.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(netWeightSku, objSku))),
                    "Без веса",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroupWeight);

            RegularFilterGroupEntity filterGroupComposition = new RegularFilterGroupEntity(genID());
            filterGroupComposition.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(mainCompositionOriginSku, objSku))),
                    "Без состава",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroupComposition);

            setPageSize(0);

            freightChangedFA = addMFAProp(actionGroup, "Обработать", this,
                    new ObjectEntity[]{objFreight}, true);
            freightChangedFA.setImage("arrow_right.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";
            design.get(getPropertyDraw(baseLM.name, objGender)).caption = "Пол";
            design.get(getPropertyDraw(baseLM.name, objComposition)).caption = "Состав";
            design.get(getPropertyDraw(baseLM.name, objTypeFabric)).caption = "Тип одежды";

            design.get(objFreight.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objFreight.groupTo).grid.constraints.fillVertical = 1;
            design.get(gobjCategoryGenderCompositionTypeFabric).grid.constraints.fillVertical = 4;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 4;
            design.get(objCategory.groupTo).grid.constraints.fillHorizontal = 0.1;
            //design.get(objSku.groupTo).grid.constraints.fillHorizontal = 6;
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

            ContainerView categoryContainer = design.createContainer("Группы");
            categoryContainer.add(design.getGroupObjectContainer(objCategory2.groupTo));
            categoryContainer.add(design.getGroupObjectContainer(gobjCategoryGenderCompositionTypeFabric));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(categoryContainer);
            specContainer.add(skuContainer);
            specContainer.add(design.getGroupObjectContainer(objSkuFreight.groupTo));
            specContainer.tabbedPane = true;

            //design.get(filterGroupCategory).drawToToolbar = false;
            //design.get(filterGroupCustomCategory10).drawToToolbar = false;
            //design.get(filterGroupCountry).drawToToolbar = false;

            design.setHighlightColor(new Color(128, 255, 255));

            return design;
        }
    }

    public class AnnexInvoiceFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean translate;

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objTypeInvoice;
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

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objTypeInvoice = new ObjectEntity(9, "typeInvoice", typeInvoice, "Тип инвойса");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            gobjFreightImporter.add(objTypeInvoice);
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
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, netWeightImporterFreightTypeInvoice, grossWeightImporterFreightTypeInvoice, sumImporterFreightTypeInvoice);

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

            addPropertyDraw(sidImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(declarationExport, objImporter, objFreight);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightArticle, objFreight, objArticle), Compare.EQUALS, objTypeInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

            if (!translate)
                annexInvoiceOriginFormImporterFreight = addFAProp("Приложение", this, objImporter, objFreight, objTypeInvoice);

            if (translate)
                annexInvoiceFormImporterFreight = addFAProp("Приложение (перевод)", this, objImporter, objFreight, objTypeInvoice);
        }
    }

    public class InvoiceExportFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objTypeInvoice;
        public ObjectEntity objFreightBox;
        public ObjectEntity objSku;

        private GroupObjectEntity gobjFreightImporterTypeInvoice;
        private GroupObjectEntity gobjFreightBoxSku;

        private InvoiceExportFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporterTypeInvoice = new GroupObjectEntity(1, "freightImporterTypeInvoice");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objTypeInvoice = new ObjectEntity(9, "typeInvoice", typeInvoice, "Тип инвойса");

            gobjFreightImporterTypeInvoice.add(objFreight);
            gobjFreightImporterTypeInvoice.add(objImporter);
            gobjFreightImporterTypeInvoice.add(objTypeInvoice);
            addGroup(gobjFreightImporterTypeInvoice);

            addPropertyDraw(objFreight, baseLM.date, baseLM.objectClassName);
            addPropertyDraw(objImporter, baseLM.name);
            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, dateContractImporterFreight);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice, dateShipmentImporterFreightTypeInvoice);
            addPropertyDraw(objTypeInvoice, baseLM.name);

            gobjFreightImporterTypeInvoice.initClassView = ClassViewType.PANEL;

            gobjFreightBoxSku = new GroupObjectEntity(4, "freightBoxSku");
            objFreightBox = new ObjectEntity(5, "freightBox", freightBox, "Короб");
            objSku = new ObjectEntity(6, "sku", sku, "SKU");

            gobjFreightBoxSku.add(objFreightBox);
            gobjFreightBoxSku.add(objSku);
            addGroup(gobjFreightBoxSku);

            addPropertyDraw(objFreightBox, baseLM.barcode);
            addPropertyDraw(objSku, sidArticleSku, originalNameArticleSku, sidColorSupplierItem, sidSizeSupplierItem, nameCommonSizeSku);
            addPropertyDraw(objFreight, objSku, mainCompositionFreightSku, nameCountryOfOriginFreightSku);
            addPropertyDraw(objSku, baseLM.barcode, nameBrandSupplierArticleSku, nameCountryBrandSupplierSku, sidGenderArticleSku, nameThemeSupplierArticleSku, sidSeasonSupplierArticleSku, nameCategoryArticleSku, nameSupplierArticleSku);
            //setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
//            getPropertyDraw(sidImporterFreightTypeInvoice).toDraw = objSku.groupTo;
//            getPropertyDraw(dateImporterFreightTypeInvoice).toDraw = objSku.groupTo;
            addPropertyDraw(sidContractInProxyImporterStockSku, objImporter, objFreightBox, objSku);
            addPropertyDraw(dateContractInProxyImporterStockSku, objImporter, objFreightBox, objSku);

            addPropertyDraw(quantityImporterStockSku, objImporter, objFreightBox, objSku);
            addPropertyDraw(priceInvoiceImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(sumInvoiceImporterStockSku, objImporter, objFreightBox, objSku);
            addPropertyDraw(priceInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(RRPInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(sumInImporterStockSku, objImporter, objFreightBox, objSku);

            gobjFreightBoxSku.initClassView = ClassViewType.GRID;

            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightSku, objFreight, objSku), Compare.EQUALS, objTypeInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterStockSku, objImporter, objFreightBox, objSku)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSku)));

            invoiceExportFormImporterFreight = addFAProp("Экспорт", this, objImporter, objFreight, objTypeInvoice);

            setReadOnly(true);
        }
    }

    public class InvoiceFromFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean translate;

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objTypeInvoice;
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

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objTypeInvoice = new ObjectEntity(9, "typeInvoice", typeInvoice, "Тип инвойса");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            gobjFreightImporter.add(objTypeInvoice);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, baseLM.date, baseLM.objectClassName, nameCurrencyFreight, symbolCurrencyFreight);

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

            addPropertyDraw(objImporter, sidImporter);
            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, dateContractImporterFreight, conditionShipmentContractImporterFreight, conditionPaymentContractImporterFreight);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice, dateShipmentImporterFreightTypeInvoice, quantityImporterFreightTypeInvoice, netWeightImporterFreightTypeInvoice, grossWeightImporterFreightTypeInvoice, sumImporterFreightTypeInvoice);

            addPropertyDraw(objTypeInvoice, baseLM.name);

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
                addPropertyDraw(objArticle, nameArticle, nameOriginCategoryArticle);
                addPropertyDraw(objComposition, baseLM.objectValue);
            }

            addPropertyDraw(objArticle, coefficientArticle);

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

            addPropertyDraw(declarationExport, objImporter, objFreight);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightArticle, objFreight, objArticle), Compare.EQUALS, objTypeInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

            if (!translate)
                invoiceOriginFormImporterFreight = addFAProp("Инвойс", this, objImporter, objFreight, objTypeInvoice);

            if (translate)
                invoiceFormImporterFreight = addFAProp("Инвойс (перевод)", this, objImporter, objFreight, objTypeInvoice);
        }
    }


    public class DeclarantFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objTypeInvoice;
        public ObjectEntity objArticle;
        public ObjectEntity objCategory;
        public ObjectEntity objComposition;
        public ObjectEntity objCountry;

        private GroupObjectEntity gobjFreightImporter;
        private GroupObjectEntity gobjArticleCompositionCountryCategory;

        private DeclarantFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objTypeInvoice = new ObjectEntity(9, "typeInvoice", typeInvoice, "Тип инвойса");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            gobjFreightImporter.add(objTypeInvoice);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, baseLM.date, baseLM.objectClassName, nameCurrencyFreight, symbolCurrencyFreight);

            addPropertyDraw(objImporter, sidImporter);
            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, conditionShipmentContractImporterFreight, conditionPaymentContractImporterFreight);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice, dateShipmentImporterFreightTypeInvoice, quantityImporterFreightTypeInvoice, netWeightImporterFreightTypeInvoice, grossWeightImporterFreightTypeInvoice, sumImporterFreightTypeInvoice);

            addPropertyDraw(objTypeInvoice, baseLM.name);

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

            addPropertyDraw(objArticle, nameCategoryArticle);
            addPropertyDraw(compositionFreightArticleCompositionCountryCategory, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(objCountry, baseLM.name);
            addPropertyDraw(objCategory, sidCustomCategory10);
            addPropertyDraw(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(objArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(priceImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(sumImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(declarationExport, objImporter, objFreight);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightArticle, objFreight, objArticle), Compare.EQUALS, objTypeInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

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

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
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
            addPropertyDraw(declarationExport, objImporter, objFreight);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

            if (!translate)
                proformOriginFormImporterFreight = addFAProp("Инвойс-проформа", this, objImporter, objFreight);

            if (translate)
                proformFormImporterFreight = addFAProp("Инвойс-проформа (перевод)", this, objImporter, objFreight);
        }
    }



    private class ListFreightUnitFreightFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objFreightUnit;
        private ObjectEntity objBrand;

        private GroupObjectEntity gobjFreightImporter;

        private ListFreightUnitFreightFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, baseLM.date);
            addPropertyDraw(objImporter, baseLM.name);
            addPropertyDraw(objImporter, objFreight, dateImporterFreight, sidImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objBrand = addSingleGroupObject(4, "brand", brandSupplier, "Бренд", baseLM.name);

            objFreightUnit = addSingleGroupObject(5, "freightBox", freightUnit, "Короб", baseLM.barcode);

            addPropertyDraw(quantityFreightUnitBrandSupplier, objFreightUnit, objBrand);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightUnit, objFreightUnit), Compare.EQUALS, objFreight));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightUnitBrandSupplier, objFreightUnit, objBrand)));

            listFreightUnitFormImporterFreight = addFAProp("Короба фрахта", this, objImporter, objFreight);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            return design;
        }
    }

    private class PackingListFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean translate;

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objFreightBox;
        private ObjectEntity objArticle;
        private ObjectEntity objTypeInvoice;
        private ObjectEntity objSku;

        private GroupObjectEntity gobjFreightImporter;

        private PackingListFormEntity(NavigatorElement parent, String sID, String caption, boolean translate) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objTypeInvoice = new ObjectEntity(6, "typeInvoice", typeInvoice, "Тип инвойса");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            gobjFreightImporter.add(objTypeInvoice);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, baseLM.date, baseLM.objectClassName, nameCurrencyFreight, symbolCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight, addressExporterFreight);
                addPropertyDraw(objImporter, baseLM.name, addressSubject);
            }

            if (!translate) {
                addPropertyDraw(objFreight, nameOriginExporterFreight, addressOriginExporterFreight);
                addPropertyDraw(objImporter, nameOrigin, addressOriginSubject);
            }

            addPropertyDraw(objImporter, sidImporter);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice, quantityImporterFreightTypeInvoice, netWeightImporterFreightTypeInvoice, grossWeightImporterFreightTypeInvoice, sumInOutImporterFreightTypeInvoice);
            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, dateContractImporterFreight, conditionShipmentContractImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objFreightBox = addSingleGroupObject(4, "freightBox", freightBox, "Короб", baseLM.barcode);
            addPropertyDraw(objImporter, objFreightBox, objTypeInvoice, netWeightImporterFreightUnitTypeInvoice, grossWeightImporterFreightUnitTypeInvoice, quantityImporterStockTypeInvoice);

            objArticle = addSingleGroupObject(5, "article", article, "Артикул", sidArticle, nameBrandSupplierArticle);

            if (translate)
                addPropertyDraw(objArticle, nameCategoryArticle);               

            if (!translate)
                addPropertyDraw(objArticle, nameArticle);

            addPropertyDraw(quantityImporterStockArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(netWeightImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(grossWeightImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(sumImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightArticle, objFreight, objArticle), Compare.EQUALS, objTypeInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterStockTypeInvoice, objImporter, objFreightBox, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterStockArticle, objImporter, objFreightBox, objArticle)));

            if (!translate)
                packingListOriginFormImporterFreight = addFAProp("Упаковочный лист", this, objImporter, objFreight, objTypeInvoice);

            if (translate)
                packingListFormImporterFreight = addFAProp("Упаковочный лист (перевод)", this, objImporter, objFreight, objTypeInvoice);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objFreightBox.groupTo).grid.constraints.fillVertical = 2;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 3;
            return design;
        }
    }

    public class SbivkaFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objCustomCategory6;
        public ObjectEntity objCategory;

        private GroupObjectEntity gobjFreightImporter;

        private SbivkaFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroup(gobjFreightImporter);

            addPropertyDraw(objFreight, baseLM.date, nameExporterFreight, addressExporterFreight, nameCurrencyFreight);
            addPropertyDraw(objImporter, baseLM.name, addressSubject, contractImporter);
            addPropertyDraw(objImporter, objFreight, quantityProxyImporterFreight, netWeightImporterFreight, grossWeightImporterFreight, sumSbivkaImporterFreight, sidImporterFreight, dateImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objCustomCategory6 = addSingleGroupObject(4, "customCategory6", customCategory6, "ТН ВЭД");

            addPropertyDraw(objCustomCategory6, sidCustomCategory6);
            addPropertyDraw(objImporter, objFreight, objCustomCategory6, quantityProxyImporterFreightCustomCategory6, netWeightImporterFreightCustomCategory6, grossWeightImporterFreightCustomCategory6, sumImporterFreightCustomCategory6);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityProxyImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityProxyImporterFreightCustomCategory6, objImporter, objFreight, objCustomCategory6)));

            objCategory = addSingleGroupObject(5, "category", category, "Номенклатурная группа", baseLM.name, nameUnitOfMeasureCategory);
            addPropertyDraw(objImporter, objFreight, objCustomCategory6, objCategory, quantityProxyImporterFreightCustomCategory6Category, netWeightImporterFreightCustomCategory6Category, grossWeightImporterFreightCustomCategory6Category, sumImporterFreightCustomCategory6Category);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityProxyImporterFreightCustomCategory6Category, objImporter, objFreight, objCustomCategory6, objCategory)));

            sbivkaFormImporterFreight = addFAProp("Сбивка", this, objImporter, objFreight);
        }
    }

    public class SbivkaSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objFreight;
        public ObjectEntity objSupplier;
        public ObjectEntity objImporter;
        public ObjectEntity objCustomCategory6;
        public ObjectEntity objCategory;

        private GroupObjectEntity gobjFreightImporterSupplier;

        private SbivkaSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporterSupplier = new GroupObjectEntity(1, "freightImporterSupplier");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objSupplier = new ObjectEntity(4, "supplier", supplier, "Поставщик");

            gobjFreightImporterSupplier.add(objFreight);
            gobjFreightImporterSupplier.add(objImporter);
            gobjFreightImporterSupplier.add(objSupplier);
            addGroup(gobjFreightImporterSupplier);

            addPropertyDraw(objFreight, baseLM.date, nameCurrencyFreight);
            addPropertyDraw(objImporter, baseLM.name, addressSubject, contractImporter);
            addPropertyDraw(objSupplier, baseLM.name);

            gobjFreightImporterSupplier.initClassView = ClassViewType.PANEL;

            objCustomCategory6 = addSingleGroupObject(5, "customCategory6", customCategory6, "ТН ВЭД");

            addPropertyDraw(objCustomCategory6, sidCustomCategory6, nameCustomCategory);
            addPropertyDraw(objImporter, objFreight, objSupplier, objCustomCategory6, quantityDirectImporterFreightSupplierCustomCategory6, netWeightImporterFreightSupplierCustomCategory6, grossWeightImporterFreightSupplierCustomCategory6, sumImporterFreightSupplierCustomCategory6);
            addPropertyDraw(objImporter, objFreight, objSupplier, quantityImporterFreightSupplier, netWeightImporterFreightSupplier, grossWeightImporterFreightSupplier, sumImporterFreightSupplier);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSupplier, objImporter, objFreight, objSupplier)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDirectImporterFreightSupplierCustomCategory6, objImporter, objFreight, objSupplier, objCustomCategory6)));

            objCategory = addSingleGroupObject(6, "category", category, "Номенклатурная группа", baseLM.name);
            addPropertyDraw(quantityDirectImporterFreightSupplierCustomCategory6Category, objImporter, objFreight, objSupplier, objCustomCategory6, objCategory);
            addPropertyDraw(netWeightImporterFreightSupplierCustomCategory6Category, objImporter, objFreight, objSupplier, objCustomCategory6, objCategory);
            addPropertyDraw(grossWeightImporterFreightSupplierCustomCategory6Category, objImporter, objFreight, objSupplier, objCustomCategory6, objCategory);
            addPropertyDraw(sumImporterFreightSupplierCustomCategory6Category, objImporter, objFreight, objSupplier, objCustomCategory6, objCategory);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDirectImporterFreightSupplierCustomCategory6Category, objImporter, objFreight, objSupplier, objCustomCategory6, objCategory)));

            sbivkaFormImporterFreightSupplier = addFAProp("Сбивка по поставщику", this, objImporter, objFreight, objSupplier);
        }
    }

    private class ColorSizeSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objColor;
        private ObjectEntity objSize;
        private ObjectEntity objBrand;
        private ObjectEntity objCountry;
        private ObjectEntity objTheme;
        private ObjectEntity objSeason;
        private ObjectEntity objGroupSize;
        private ObjectEntity objGenderSupplier;

        private ColorSizeSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name, nameBrandSupplierSupplier, nameCurrencySupplier);
            addObjectActions(this, objSupplier);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд", sidBrandSupplier, baseLM.name, nameCountryBrandSupplier, sidGenderBrandSupplier);
            addObjectActions(this, objBrand);

            objColor = addSingleGroupObject(colorSupplier, "Цвет", sidColorSupplier, baseLM.name);
            addObjectActions(this, objColor);

            objGroupSize = addSingleGroupObject(sizeGroupSupplier, "Размерная сетка", baseLM.name);
            addObjectActions(this, objGroupSize);

            objSize = addSingleGroupObject(sizeSupplier, "Размер", sidSizeSupplier, nameCommonSizeSizeSupplier, nameGroupSizeSupplier, orderSizeSupplier);
            addObjectActions(this, objSize);

            addPropertyDraw(equalsGroupSizeSupplier, objSize, objGroupSize);

            objTheme = addSingleGroupObject(themeSupplier, "Тема", baseLM.name);
            addObjectActions(this, objTheme);

            objSeason = addSingleGroupObject(seasonSupplier, "Сезон", baseLM.name, sidSeasonSupplier);
            addObjectActions(this, objSeason);

            objCountry = addSingleGroupObject(countrySupplier, "Страна", baseLM.name, nameCountryCountrySupplier);
            addObjectActions(this, objCountry);

            objGenderSupplier = addSingleGroupObject(genderSupplier, "Пол", baseLM.name, sidGenderSupplier, sidGenderGenderSupplier);
            addObjectActions(this, objGenderSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColor), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSize), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierBrandSupplier, objBrand), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierThemeSupplier, objTheme), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSeasonSupplier, objSeason), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierCountrySupplier, objCountry), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeGroup, objGroupSize), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierGenderSupplier, objGenderSupplier), Compare.EQUALS, objSupplier));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(groupSizeSupplier, objSize), Compare.EQUALS, objGroupSize),
                    "В размерной сетке",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                                   design.getGroupObjectContainer(objBrand.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objColor.groupTo),
                    design.getGroupObjectContainer(objSize.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSize.groupTo),
                                   design.getGroupObjectContainer(objTheme.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objTheme.groupTo),
                    design.getGroupObjectContainer(objSeason.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSeason.groupTo),
                    design.getGroupObjectContainer(objCountry.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objCountry.groupTo),
                    design.getGroupObjectContainer(objGenderSupplier.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView sizeContainer = design.createContainer("Размеры");
            sizeContainer.add(design.getGroupObjectContainer(objGroupSize.groupTo));
            sizeContainer.add(design.getGroupObjectContainer(objSize.groupTo));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objColor.groupTo));
            specContainer.add(design.getGroupObjectContainer(objColor.groupTo));
            specContainer.add(sizeContainer);
            specContainer.add(design.getGroupObjectContainer(objTheme.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSeason.groupTo));
            specContainer.add(design.getGroupObjectContainer(objCountry.groupTo));
            specContainer.add(design.getGroupObjectContainer(objGenderSupplier.groupTo));
            specContainer.tabbedPane = true;

            design.get(objSupplier.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objBrand.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 1;
            design.get(objBrand.groupTo).grid.constraints.fillVertical = 1;            

            design.get(objColor.groupTo).grid.constraints.fillVertical = 2;
            design.get(objSize.groupTo).grid.constraints.fillVertical = 2;
            design.get(objTheme.groupTo).grid.constraints.fillVertical = 2;
            design.get(objSeason.groupTo).grid.constraints.fillVertical = 2;
            design.get(objCountry.groupTo).grid.constraints.fillVertical = 2;
            design.get(objGenderSupplier.groupTo).grid.constraints.fillVertical = 2;

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
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objTransfer.groupTo).grid.constraints.fillVertical = 0.4;

            return design;
        }
    }

    private class FreightInvoiceFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objBrandSupplier;
        private ObjectEntity objSku;
        private ObjectEntity objArticle;

        private FreightInvoiceFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freightChanged, "Фрахт", baseLM.date, baseLM.objectClassName, nameRouteFreight, nameFreightTypeFreight, nameCurrencyFreight, sumFreightFreight, sumInFreight, sumMarkupInFreight, sumInOutFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objFreight, true);
            setReadOnly(nameCurrencyFreight, false);
            setReadOnly(sumFreightFreight, false);

            addPropertyDraw(sumInCurrentYear);
            addPropertyDraw(sumInOutCurrentYear);
            addPropertyDraw(balanceSumCurrentYear);

            objImporter = addSingleGroupObject(importer, "Импортер", baseLM.name);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumMarkupInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInOutImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumDutyImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumNDSImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumRegistrationImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumCustomImporterFreight, objImporter, objFreight);

            objBrandSupplier = addSingleGroupObject(brandSupplier, "Бренд", baseLM.name, nameSupplierBrandSupplier);

            addPropertyDraw(quantityImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);
            addPropertyDraw(markupPercentImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);
            addPropertyDraw(averagePercentImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);
            addPropertyDraw(insuranceFreightBrandSupplier, objFreight, objBrandSupplier);

            /*gobjArticleNetWeight = new GroupObjectEntity(genID(), "Группировка");
            objArticle = new ObjectEntity(genID(), article, "Артикул");
            objWeight = new ObjectEntity(genID(), DoubleClass.instance, "Вес нетто");
            addPropertyDraw(quantityImporterFreightArticleNetWeight, objImporter, objFreight, objArticle, objWeight);
            gobjArticleNetWeight.add(objArticle);
            gobjArticleNetWeight.add(objWeight);
            addGroup(gobjArticleNetWeight);
            addPropertyDraw(objArticle, sidArticle, nameCategoryArticle, nameBrandSupplierArticle);
            addPropertyDraw(objWeight, baseLM.objectValue); */

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle, nameBrandSupplierArticle, nameCategoryArticle);

            addPropertyDraw(objImporter, objFreight, objArticle, quantityImporterFreightArticle, markupPercentImporterFreightArticle, priceFullKgImporterFreightArticle, minPriceRateImporterFreightArticle);

            PropertyObjectEntity greaterPriceMinPriceImporterFreightArticleProperty = addPropertyObject(greaterPriceMinPriceImporterFreightArticle, objImporter, objFreight, objArticle);
            getPropertyDraw(minPriceRateImporterFreightArticle).setPropertyHighlight(greaterPriceMinPriceImporterFreightArticleProperty);
            getPropertyDraw(priceFullKgImporterFreightArticle).setPropertyHighlight(greaterPriceMinPriceImporterFreightArticleProperty);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.barcode, sidArticleSku, nameBrandSupplierArticleSku, nameCategoryArticleSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            setReadOnly(baseGroup, true, objSku.groupTo);
            setReadOnly(publicGroup, true, objSku.groupTo);

            addPropertyDraw(nameCountryOfOriginFreightSku, objFreight, objSku);
            addPropertyDraw(netWeightFreightSku, objFreight, objSku);
            addPropertyDraw(sidCustomCategory10FreightSku, objFreight, objSku);
            //addPropertyDraw(nameSubCategoryFreightSku, objFreight, objSku);
            //addPropertyDraw(nameTypeInvoiceFreightArticleSku, objFreight, objSku);
            addPropertyDraw(quantityImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(RRPInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(markupPercentImporterFreightSku, objImporter, objFreight, objSku);
            //addPropertyDraw(markupInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInOutImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInvoiceImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(markupInOutImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceFreightImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInsuranceImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceFullImporterFreightSku, objImporter, objFreight, objSku);
            //addPropertyDraw(minPriceRateWeightImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceFullKgImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(minPriceRateFreightSku, objFreight, objSku);
            //addPropertyDraw(dutyNetWeightFreightSku, objFreight, objSku);
            //addPropertyDraw(dutyPercentImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(dutyImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(NDSPercentFreightSku, objFreight, objSku);
            addPropertyDraw(NDSImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(sumRegistrationImporterFreightSku, objImporter, objFreight, objSku);

            PropertyObjectEntity greaterPriceMinPriceImporterFreightSkuProperty = addPropertyObject(greaterPriceMinPriceImporterFreightSku, objImporter, objFreight, objSku);
            getPropertyDraw(minPriceRateFreightSku).setPropertyHighlight(greaterPriceMinPriceImporterFreightSkuProperty);
            getPropertyDraw(priceFullKgImporterFreightSku).setPropertyHighlight(greaterPriceMinPriceImporterFreightSkuProperty);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticle, objImporter, objFreight, objArticle)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSku)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(priceInImporterFreightSku, objImporter, objFreight, objSku))),
                    "Без цены",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroup);

            RegularFilterGroupEntity filterPriceGroup = new RegularFilterGroupEntity(genID());
            filterPriceGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(priceInImporterFreightSku, objImporter, objFreight, objSku), Compare.EQUALS, addPropertyObject(baseLM.vzero)),
                    "С нулевой ценой",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterPriceGroup);

            addHintsIncrementTable(priceFullKgImporterFreightArticle, minPriceRateImporterFreightArticle,
                    sumInOutImporterFreightArticle, sumInOutImporterFreightBrandSupplier, sumPercentImporterFreightBrandSupplier, sumInOutFreightArticle,
                    markupPercentImporterFreightSku, priceInOutImporterFreightSku, priceInvoiceImporterFreightSku, priceInsuranceImporterFreightSku, priceFullImporterFreightSku,
                    sumInOutFreightBrandSupplier, sumInOutImporterFreightSku, dutyImporterFreightSku, priceMarkupInImporterFreightSku, sumMarkupInImporterFreight,
                    sumInImporterFreight, sumMarkupInImporterFreight, sumInOutImporterFreight,
                    sumInFreight, sumMarkupInFreight, sumInOutFreight);
//            addHintsNoUpdate(objImporter.groupTo);
//            addHintsNoUpdate(dutyImporterFreightSku);
//            addHintsNoUpdate(NDSImporterFreightSku);
            //addHintsNoUpdate(sumInImporterFreight);

            setPageSize(0);

            freightPricedFA = addMFAProp(actionGroup, "Расценить", this, new ObjectEntity[] {objFreight}, true,
                    addPropertyObject(addJProp(true, executeChangeFreightClass, 1, addCProp(baseClass.objectClass, "freightPriced")), objFreight));
            freightPricedFA.setImage("arrow_right.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objBrandSupplier.groupTo).grid.constraints.fillHorizontal = 1;

            ContainerView specContainer = design.createContainer("Итоги по текущему году");
            specContainer.add(design.get(getPropertyDraw(sumInCurrentYear)));
            specContainer.add(design.get(getPropertyDraw(sumInOutCurrentYear)));
            specContainer.add(design.get(getPropertyDraw(balanceSumCurrentYear)));
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objFreight.groupTo));

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo), specContainer, DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objImporter.groupTo), design.getGroupObjectContainer(objBrandSupplier.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView priceContainer = design.createContainer();
            design.getMainContainer().addAfter(priceContainer, design.getGroupObjectContainer(objSku.groupTo));
            priceContainer.add(design.getGroupObjectContainer(objArticle.groupTo));
            priceContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            priceContainer.tabbedPane = true;

            design.get(objArticle.groupTo).grid.constraints.fillVertical = 3;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 3;

            design.setHighlightColor(new Color(255, 128, 128));

            return design;
        }
    }

    private class FreightContentFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objBrand;
        private ObjectEntity objArticle;
        private ObjectEntity objFreightBox;

        private PropertyDrawEntity nameBrand;

        private FreightContentFormEntity(NavigatorElement<RomanBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.date, baseLM.objectClassName, nameRouteFreight, nameExporterFreight, nameFreightTypeFreight, nameCurrencyFreight, sumFreightFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.GRID);

            objImporter = addSingleGroupObject(importer, "Импортер", baseLM.name);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд");

            nameBrand = addPropertyDraw(baseLM.name, objBrand);

            addPropertyDraw(objFreight, objBrand, stockNumberFreightBrandSupplier, quantityShipmentedFreightBrandSupplier, quantityFreightBrandSupplier, sumInFreightBrandSupplier, sumInOutFreightBrandSupplier);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityFreightBrandSupplier, objFreight, objBrand), Compare.GREATER, addPropertyObject(baseLM.vzero)));

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle, nameCategoryArticle);

            addPropertyDraw(objFreight, objArticle, quantityFreightArticle, sumInFreightArticle, sumInOutFreightArticle);
            //addPropertyDraw(objImporter, objFreight, objArticle, quantityImporterFreightArticle, sumInImporterFreightArticle, sumInOutImporterFreightArticle);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(brandSupplierArticle, objArticle), Compare.EQUALS, objBrand));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityFreightArticle, objFreight, objArticle), Compare.GREATER, addPropertyObject(baseLM.vzero)));

            objFreightBox = addSingleGroupObject(freightBox, "Короб", baseLM.barcode);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityStockArticle, objFreightBox, objArticle), Compare.GREATER, addPropertyObject(baseLM.vzero)));

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo), design.getGroupObjectContainer(objImporter.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(nameBrand).setPreferredCharWidth(15);
            design.get(getPropertyDraw(nameCategoryArticle, objArticle)).setPreferredCharWidth(15);
            design.get(getPropertyDraw(sidArticle, objArticle)).setPreferredCharWidth(15);

            design.get(objFreight.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 1;

            design.addIntersection(design.getGroupObjectContainer(objArticle.groupTo), design.getGroupObjectContainer(objFreightBox.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.get(objArticle.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objFreightBox.groupTo).grid.constraints.fillHorizontal = 1;

            design.get(objFreight.groupTo).grid.constraints.fillVertical = 1;
            design.get(objBrand.groupTo).grid.constraints.fillVertical = 1;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 2;

            return design;
        }
    }

    private class FreightReportFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objTypeInvoice;
        private ObjectEntity objTransitDocument;
        private ObjectEntity objBrand;

        private GroupObjectEntity gobjFreightImporterTypeInvoice;

        private PropertyDrawEntity nameBrand;

        private FreightReportFormEntity(NavigatorElement<RomanBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            gobjFreightImporterTypeInvoice = new GroupObjectEntity(genID(), "Группировка");
            objFreight = new ObjectEntity(genID(), freight, "Фрахт");
            objImporter = new ObjectEntity(genID(), importer, "Импортёр");
            objTypeInvoice = new ObjectEntity(genID(), typeInvoice, "Тип инвойса");

            gobjFreightImporterTypeInvoice.add(objFreight);
            gobjFreightImporterTypeInvoice.add(objImporter);
            gobjFreightImporterTypeInvoice.add(objTypeInvoice);
            addGroup(gobjFreightImporterTypeInvoice);

            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice);

            addPropertyDraw(objFreight, baseLM.date, dateArrivalFreight, nameRouteFreight);
            addPropertyDraw(objImporter, baseLM.name);
            addPropertyDraw(objFreight, nameCurrencyFreight, sumFreightFreight, freightBoxNumberFreight, quantityFreight, sumInFreight, sumInOutFreight);
            setForceViewType(sumInOutFreight, ClassViewType.GRID);

            addPropertyDraw(objImporter, objFreight, sumCustomImporterFreight);

            objTransitDocument = addSingleGroupObject(transitDocument, "Транзитный документ", sidTransitDocument, nameSellerTransitDocument, nameTypeTransitTransitDocument, dateRepaymentTransitDocument, dateClosingTransitDocument);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд");
            nameBrand = addPropertyDraw(baseLM.name, objBrand);

            addPropertyDraw(objFreight, objBrand, stockNumberFreightBrandSupplier, quantityFreightBrandSupplier, sumInFreightBrandSupplier, sumInOutFreightBrandSupplier);
            addPropertyDraw(objImporter, objFreight, objBrand, averagePercentImporterFreightBrandSupplier);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(sidImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(importerTransitDocument, objTransitDocument), Compare.EQUALS, objImporter));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightTransitDocument, objTransitDocument), Compare.EQUALS, objFreight));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight), Compare.GREATER, addPropertyObject(baseLM.vzero)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityFreightBrandSupplier, objFreight, objBrand), Compare.GREATER, addPropertyObject(baseLM.vzero)));

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.name, objImporter)).caption = "Импортёр";
            design.get(getPropertyDraw(sumInFreight, objFreight)).caption = "Итого в ценах поставщика";
            design.get(getPropertyDraw(sumInOutFreight, objFreight)).caption = "Итого в отгрузочных ценах";

            design.get(getPropertyDraw(sidImporterFreightTypeInvoice)).setPreferredCharWidth(15);
            design.get(getPropertyDraw(dateArrivalFreight, objFreight)).setPreferredCharWidth(20);
            design.get(getPropertyDraw(freightBoxNumberFreight, objFreight)).setPreferredCharWidth(20);
            design.get(getPropertyDraw(sumInFreight, objFreight)).setPreferredCharWidth(20);
            design.get(getPropertyDraw(sumInOutFreight, objFreight)).setPreferredCharWidth(20);
            design.get(getPropertyDraw(baseLM.name, objImporter)).setPreferredCharWidth(20);
            design.get(getPropertyDraw(baseLM.name, objBrand)).setPreferredCharWidth(20);

            design.addIntersection(design.getGroupObjectContainer(objTransitDocument.groupTo), design.getGroupObjectContainer(objBrand.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(gobjFreightImporterTypeInvoice).grid.constraints.fillVertical = 2;
            design.get(objTransitDocument.groupTo).grid.constraints.fillVertical = 1;
            design.get(objBrand.groupTo).grid.constraints.fillVertical = 1;

            design.get(objTransitDocument.groupTo).grid.constraints.fillHorizontal= 2;
            design.get(objBrand.groupTo).grid.constraints.fillHorizontal = 3;

            return design;
        }
    }

    private class FreightBoxContentFormEntity extends BarcodeFormEntity {

        private ObjectEntity objFreightBox;
        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objTypeInvoice;
        private ObjectEntity objArticle;

        private GroupObjectEntity gobjFreightImporterTypeInvoice;
        private PropertyDrawEntity importerName;
        private PropertyDrawEntity typeInvoiceName;

        private FreightBoxContentFormEntity(NavigatorElement<RomanBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreightBox = addSingleGroupObject(freightBox, "Короб", baseLM.barcode, netWeightStock, nameDestinationFreightBox);
            objFreightBox.groupTo.setSingleClassView(ClassViewType.GRID);

            addActionsOnObjectChange(objBarcode, addPropertyObject(baseLM.seekBarcodeAction, objBarcode));

            gobjFreightImporterTypeInvoice = new GroupObjectEntity(genID(), "Группировка");
            objFreight = new ObjectEntity(genID(), freight, "Фрахт");
            objImporter = new ObjectEntity(genID(), importer, "Импортёр");
            objTypeInvoice = new ObjectEntity(genID(), typeInvoice, "Тип инвойса");

            gobjFreightImporterTypeInvoice.add(objFreight);
            gobjFreightImporterTypeInvoice.add(objImporter);
            gobjFreightImporterTypeInvoice.add(objTypeInvoice);
            addGroup(gobjFreightImporterTypeInvoice);

            addPropertyDraw(objFreight, baseLM.date, dateArrivalFreight, nameExporterFreight);
            importerName = addPropertyDraw(baseLM.name, objImporter);
            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, dateContractImporterFreight);

            typeInvoiceName = addPropertyDraw(baseLM.name, objTypeInvoice);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityImporterStock, objImporter, objFreightBox), Compare.GREATER, addPropertyObject(baseLM.vzero)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityImporterStockTypeInvoice, objImporter, objFreightBox, objTypeInvoice), Compare.GREATER, addPropertyObject(baseLM.vzero)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(freightFreightBox, objFreightBox)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle, nameCategoryArticle);

            addPropertyDraw(objFreightBox, objArticle, quantityStockArticle);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityStockArticle, objFreightBox, objArticle), Compare.GREATER, addPropertyObject(baseLM.vzero)));

        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(importerName).caption = "Импортер";
            design.get(typeInvoiceName).caption = "Тип";

            //design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo), design.getGroupObjectContainer(objImporter.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objImporter.groupTo), design.getGroupObjectContainer(objTypeInvoice.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            //design.get(objFreight.groupTo).grid.constraints.fillHorizontal = 2;
            //design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 1;
            //design.get(objTypeInvoice.groupTo).grid.constraints.fillHorizontal = 2;            /
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 2;

            return design;
        }
    }

    private class FreightCreateFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;

        private FreightCreateFormEntity(NavigatorElement<RomanBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.date, baseLM.objectClassName, dateArrivalFreight, nameRouteFreight, nameExporterFreight, descriptionFreight, tonnageDataFreight, volumeDataFreight, palletCountDataFreight, nameCurrencyFreight, sumFreightFreight);
//            addObjectActions(this, objFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);

            freightCreateFA = addMFAProp(actionGroup, "Создать фрахт", this, new ObjectEntity[] {},
                    new PropertyObjectEntity[] {addPropertyObject(getAddObjectAction(freight))},
                    new OrderEntity[] {addPropertyObject(addCProp(baseClass.objectClass, "freight"))}, true);
            ((FormActionProperty)freightCreateFA.property).seekOnOk.add(objFreight);
            freightEditFA = addMFAProp(actionGroup, "Редактировать фрахт", this, new ObjectEntity[] {objFreight}, true);
            freightEditFA.setImage("edit.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            return design;
        }
    }

    private class FreightListFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objDateTo;
        private ObjectEntity objDateFrom;
        private ObjectEntity objShipment;
        private ObjectEntity objInvoice;

        private PropertyDrawEntity createFreight;

        private FreightListFormEntity(NavigatorElement<RomanBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.date, baseLM.objectClassName, dateArrivalFreight, formLogFreight, nameRouteFreight, nameExporterFreight, descriptionFreight, tonnageDataFreight, netWeightInvoicedFreight, grossWeightFreight, volumeDataFreight, palletCountDataFreight, palletNumberFreight, freightBoxNumberFreight, nameCurrencyFreight, sumFreightFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objFreight, true);
            setReadOnly(formLogFreight, false);
            //addFixedFilter(new NotFilterEntity(new CompareFilterEntity(addPropertyObject(is(freightShipped), objFreight), Compare.EQUALS, addPropertyObject(baseLM.vtrue))));

            createFreight = addPropertyDraw(freightCreateFA, objFreight);
            createFreight.forceViewType = ClassViewType.PANEL;

            addPropertyDraw(freightEditFA, objFreight).forceViewType = ClassViewType.GRID;
            addPropertyDraw(freightCompleteFA, objFreight).forceViewType = ClassViewType.GRID;
            addPropertyDraw(freightChangedFA, objFreight).forceViewType = ClassViewType.GRID;

            addPropertyDraw(executeChangeFreightChangedClass, objFreight).forceViewType = ClassViewType.GRID;
            addPropertyDraw(freightPricedFA, objFreight).forceViewType = ClassViewType.GRID;
            addPropertyDraw(executeChangeFreightShippedClass, objFreight).forceViewType = ClassViewType.GRID;
            addPropertyDraw(baseLM.delete, objFreight).forceViewType = ClassViewType.GRID;

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
            addPropertyDraw(quantityShipmentFreight, objShipment, objFreight);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(baseLM.date, objShipment), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(baseLM.date, objShipment), Compare.LESS_EQUALS, objDateTo));

            objInvoice = addSingleGroupObject(invoice, "Инвойс");
            objInvoice.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objInvoice, true);
            addPropertyDraw(inInvoiceFreight, objInvoice, objFreight);
            addPropertyDraw(objInvoice, baseLM.date, nameSupplierDocument, sidDocument, nameDestinationDestinationDocument, sumDocument, nameCurrencyDocument, netWeightDocument, quantityDocument);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(baseLM.date, objInvoice), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(baseLM.date, objInvoice), Compare.LESS_EQUALS, objDateTo));

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.getMainContainer().addBefore(design.get(createFreight), design.getGroupObjectContainer(objFreight.groupTo));

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(baseLM.date, objInvoice)).caption = "Дата инвойса";
            design.get(getPropertyDraw(sidDocument, objShipment)).caption = "Номер поставки";
            design.get(getPropertyDraw(baseLM.date, objShipment)).caption = "Дата поставки";
            return design;
        }
    }


    private class PrintDocumentFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objSeller;
        private ObjectEntity objTransitDocument;
        private ObjectEntity objImporter;
        private ObjectEntity objTypeInvoice;
        private ObjectEntity objSku;

        private PrintDocumentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.date, baseLM.objectClassName, dateArrivalFreight, nameRouteFreight, nameExporterFreight, nameFreightTypeFreight, nameCurrencyFreight, symbolCurrencyFreight, sumFreightFreight, sumInFreight, sumMarkupInFreight, sumInOutFreight);
            setForceViewType(sumInOutFreight, ClassViewType.GRID);

            objImporter = addSingleGroupObject(importer, "Импортер", baseLM.name, sidImporter);

            objTypeInvoice = addSingleGroupObject(typeInvoice, "Тип инвойса", baseLM.name);

            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, dateContractImporterFreight, sidImporterFreight, dateImporterFreight); //, conditionShipmentContractImporterFreight, conditionPaymentContractImporterFreight);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumMarkupInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInOutImporterFreight, objImporter, objFreight);

            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice, dateShipmentImporterFreightTypeInvoice);

            objSeller = addSingleGroupObject(seller, "Поставщик", baseLM.name);

            addPropertyDraw(invoiceOriginFormImporterFreight, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(invoiceFormImporterFreight, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(sbivkaFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(listFreightUnitFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(packingListOriginFormImporterFreight, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(packingListFormImporterFreight, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(sbivkaFormImporterFreightSupplier, objImporter, objFreight, objSeller);
            addPropertyDraw(invoiceExportFormImporterFreight, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(invoiceExportDbf, objImporter, objFreight, objTypeInvoice);

            objTransitDocument = addSingleGroupObject(transitDocument, "Транзитный документ", sidTransitDocument, nameTypeTransitTransitDocument, dateRepaymentTransitDocument, dateClosingTransitDocument);
            addObjectActions(this, objTransitDocument);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.barcode, sidArticleSku, nameBrandSupplierArticleSku, nameCategoryArticleSku, sidGenderArticleSku, nameUnitOfMeasureArticleSku,
                      sidSizeSupplierItem, nameCommonSizeSku, sidColorSupplierItem, nameColorSupplierItem);

            setForceViewType(baseGroup, ClassViewType.GRID, objSku.groupTo);

            setReadOnly(baseGroup, true, objSku.groupTo);
            setReadOnly(publicGroup, true, objSku.groupTo);

            addPropertyDraw(objFreight, objSku, sidCustomCategory10FreightSku, nameCountryOfOriginFreightSku, mainCompositionFreightSku, additionalCompositionFreightSku);
            addPropertyDraw(objImporter, objFreight, objSku, netWeightImporterFreightSku, grossWeightImporterFreightSku);

            addPropertyDraw(objImporter, objFreight, objSku, quantityImporterFreightSku, priceInOutImporterFreightSku, sumInOutImporterFreightSku);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightTransitDocument, objTransitDocument), Compare.EQUALS, objFreight));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(sellerTransitDocument, objTransitDocument), Compare.EQUALS, objSeller));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(importerTransitDocument, objTransitDocument), Compare.EQUALS, objImporter));

            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSupplier, objImporter, objFreight, objSeller)),
                                              new CompareFilterEntity(addPropertyObject(exporterFreight, objFreight), Compare.EQUALS, objSeller)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSku)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightSku, objFreight, objSku), Compare.EQUALS, objTypeInvoice));

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo),
                                   design.getGroupObjectContainer(objImporter.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objTypeInvoice.groupTo),
                                   design.getGroupObjectContainer(objSeller.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSeller.groupTo),
                                   design.getGroupObjectContainer(objTransitDocument.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            /*design.addIntersection(design.getGroupObjectContainer(objImporter.groupTo),
                                   design.getGroupObjectContainer(objTypeInvoice.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT); */

            //design.get(objSupplier.groupTo).grid.constraints.fillVertical = 3;
            //design.get(objImporter.groupTo).grid.constraints.fillVertical = 2;
            //design.get(objTypeInvoice.groupTo).grid.constraints.fillVertical = 2;
            design.get(objFreight.groupTo).grid.constraints.fillHorizontal = 4;
            design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objTypeInvoice.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objSeller.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objTransitDocument.groupTo).grid.constraints.fillHorizontal = 2;
            //design.get(objFreight.groupTo).grid.constraints.fillVertical = 3;
            //design.get(objSku.groupTo).grid.constraints.fillVertical = 2;

            return design;
        }
    }

    private class CreateItemFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objSupplier;
        ObjectEntity objBarcode;
        ObjectEntity objArticleComposite;
        ObjectEntity objSIDArticleComposite;
        ObjectEntity objColorSupplier;
        ObjectEntity objSizeSupplier;
        ObjectEntity objSeasonSupplier;
        ObjectEntity objThemeSupplier;
        ObjectEntity objGenderSupplier;

        public CreateItemFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", baseLM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseLM.objectValue);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

            objArticleComposite = addSingleGroupObject(articleComposite, "Артикул", sidArticle);
            objArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objArticleComposite, true);

//            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Артикул (новый)", baseLM.objectValue);
//            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет поставщика", sidColorSupplier, baseLM.name);
            objColorSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objColorSupplier, true);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер поставщика", sidSizeSupplier);
            objSizeSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objSizeSupplier, true);

            objSeasonSupplier = addSingleGroupObject(seasonSupplier, "Сезон поставщика", sidSeasonSupplier, baseLM.name);
            objSeasonSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objSeasonSupplier, true);

            objThemeSupplier = addSingleGroupObject(themeSupplier, "Тема поставщика", sidThemeSupplier, baseLM.name);
            objThemeSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objThemeSupplier, true);

            objGenderSupplier = addSingleGroupObject(genderSupplier, "Пол поставщика", sidGenderSupplier, baseLM.name);
            objGenderSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objGenderSupplier, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticleComposite), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSeasonSupplier, objSeasonSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierThemeSupplier, objThemeSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierGenderSupplier, objGenderSupplier), Compare.EQUALS, objSupplier));

//            addActionsOnOk(addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
//            addActionsOnOk(addPropertyObject(addItemSIDArticleSupplierColorSizeBarcode, objSIDArticleComposite, objSupplier, objColorSupplier, objSizeSupplier, objBarcode));

            addActionsOnOk(addPropertyObject(addItemArticleCompositeColorSizeBarcode, objArticleComposite, objColorSupplier, objSizeSupplier, objBarcode));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.setEnabled(objSupplier, false);
            design.setEnabled(objBarcode, false);
            return design;
        }
    }

    private class EditItemFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objSku;

        public EditItemFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSku = addSingleGroupObject(sku, "Товар", nameSupplierArticleSku, baseLM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            objSku.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objSku, true);
            setReadOnly(baseLM.barcode, false, objSku.groupTo);

            skuEditFA = addMFAProp(actionGroup, "Редактировать товар", this, new ObjectEntity[] {objSku}, true);
            skuEditFA.setImage("edit.png");

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.setEnabled(nameSupplierArticleSku, false);

            return design;
        }
    }


    private class FindItemFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean box;
        private boolean barcode;

        ObjectEntity objBarcode;
        ObjectEntity objShipment;
        ObjectEntity objSupplierBox;
        ObjectEntity objRoute;
        ObjectEntity objSku;
        ObjectEntity objShipmentDetail;

        public FindItemFormEntity(NavigatorElement parent, String sID, String caption, boolean box, boolean barcode) {
            super(parent, sID, caption);

            this.box = box;
            this.barcode = barcode;

            if (barcode) {
                objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseLM.objectValue);
                objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);
            }

            if (box)
                objShipment = addSingleGroupObject(boxShipment, "Поставка", baseLM.objectValue, sidDocument, baseLM.date);
            else
                objShipment = addSingleGroupObject(simpleShipment, "Поставка", baseLM.objectValue, sidDocument, baseLM.date);

            objShipment.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб", sidSupplierBox, baseLM.barcode);
                objSupplierBox.groupTo.setSingleClassView(ClassViewType.PANEL);
            }

            objRoute = addSingleGroupObject(route, "Маршрут", baseLM.name);
            objRoute.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSku = addSingleGroupObject(sku, "Товар", sidArticleSku, baseLM.barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            setForceViewType(baseGroup, ClassViewType.GRID, objSku.groupTo);

            addPropertyDraw(invoicedShipmentSku, objShipment, objSku);
            if (box)
                addPropertyDraw(quantityListSku, objSupplierBox, objSku);
            setReadOnly(objSku, true);

            objShipmentDetail = addSingleGroupObject(box ? boxShipmentDetail : simpleShipmentDetail, "Строка поставки");

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(invoicedShipmentSku, objShipment, objSku)));
            if (barcode) {
                RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(baseLM.barcode, objSku))),
                    "Без штрих-кода",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
                filterGroup.defaultFilter = 0;
                addRegularFilterGroup(filterGroup);
            }

            if (box) {
                RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityListSku, objSupplierBox, objSku)),
                    "В текущем коробе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
//                filterGroup.defaultFilter = 0;
                addRegularFilterGroup(filterGroup);
            }

            if (barcode) {
                addActionsOnOk(addPropertyObject(baseLM.equalsObjectBarcode, objSku, objBarcode));
            } else {
                addActionsOnOk(addPropertyObject(seekRouteShipmentSkuRoute, objShipment, objSku, objRoute));
                if (box)
                    addActionsOnOk(addPropertyObject(addBoxShipmentDetailBoxShipmentSupplierBoxRouteSku, objShipment, objSupplierBox, objRoute, objSku));
                else
                    addActionsOnOk(addPropertyObject(addSimpleShipmentDetailSimpleShipmentRouteSku, objShipment, objRoute, objSku));
            }

            setReadOnly(true);

            addDefaultOrder(sidArticleSku, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            if (box) {
                design.addIntersection(design.getGroupObjectContainer(objShipment.groupTo),
                                       design.getGroupObjectContainer(objSupplierBox.groupTo),
                                       DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }

            design.get(objShipment.groupTo).grid.constraints.fillVertical = 1;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 3;

            if (barcode)
                design.setEnabled(objBarcode, false);

            design.setEnabled(objShipment, false);
            if (box)
                design.setEnabled(objSupplierBox, false);
            design.setEnabled(objRoute, false);
            return design;
        }
    }

    public class PricatFormEntity extends FormEntity {
        ObjectEntity objSupplier;

        public PricatFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            objSupplier = addSingleGroupObject(supplier, baseLM.name, importPricatSupplier, gerryWeberImportPricat, hugoBossImportPricat);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objPricat = addSingleGroupObject(pricat);
            addPropertyDraw(objPricat, baseGroup);
            addObjectActions(this, objPricat);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierPricat, objPricat), Compare.EQUALS, objSupplier));
            setReadOnly(objSupplier, true);
            setReadOnly(importPricatSupplier, false, objSupplier.groupTo);
            setReadOnly(hugoBossImportPricat, false, objSupplier.groupTo);
            setReadOnly(gerryWeberImportPricat, false, objSupplier.groupTo);
        }
    }

    public class AddNewArticleActionProperty extends CustomActionProperty {

        ObjectEntity objArticle;
        private final ClassPropertyInterface sidInterface;
        private final ClassPropertyInterface docInterface;

        public AddNewArticleActionProperty(ObjectEntity objArticle) {
            super(genSID(), StringClass.get(50), document);

            this.objArticle = objArticle;

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            sidInterface = i.next();
            docInterface = i.next();
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            context.emitExceptionIfNotInFormSession();

            DataObject sID = context.getKeyValue(sidInterface);
            DataObject document = context.getKeyValue(docInterface);

            ObjectValue supplier = supplierDocument.readClasses(context, document);
            if (supplier.isNull()) {
                context.addAction(new MessageClientAction("Не выбран поставщик", "Ввод товара"));
                return;
            }

            ObjectValue articlePrev = articleSIDSupplier.readClasses(context, sID, (DataObject)supplier);
            if (articlePrev.isNull()) {
                ObjectValue oldArticle = context.getObjectInstance(objArticle).getObjectValue();
                DataObject article = context.addObject(articleComposite);
                sidArticle.execute(sID.getValue(), context, article);
                supplierArticle.execute(supplier.getValue(), context, article);
                if (!oldArticle.isNull())
                    sizeGroupSupplierArticle.execute(sizeGroupSupplierArticle.read(context, (DataObject)oldArticle), context, article);
            }
        }
    }

    public class CreateStampActionProperty extends CustomActionProperty {
        private ClassPropertyInterface createStampInterface;

        public CreateStampActionProperty() {
            super(genSID(), "Сгенерировать марки", new ValueClass[]{creationStamp});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            createStampInterface = i.next();
        }

        public void execute(ExecutionContext context) throws SQLException {
            DataObject objCreateStamp = context.getKeyValue(createStampInterface);
            if ((firstNumberCreationStamp.read(context, objCreateStamp) == null) || (lastNumberCreationStamp.read(context, objCreateStamp) == null)) {
                context.addAction(new MessageClientAction("Необходимо задать диапазон", "Ошибка"));
                return;
            }

            String stringStart = (String) firstNumberCreationStamp.read(context, objCreateStamp);
            String stringFinish = (String) lastNumberCreationStamp.read(context, objCreateStamp);

            if (stringStart.length() != stringFinish.length()) {
                context.addAction(new MessageClientAction("Количество символов у границ диапазонов должно совпадать", "Ошибка"));
                return;
            }

            Integer start = Integer.parseInt(stringStart);
            Integer finish = Integer.parseInt(stringFinish);

            if ((finish - start) > 3000) {
                context.addAction(new MessageClientAction("Слишком большой диапазон (больше 3000)", "Ошибка"));
                return;
            }

            for (int i = start; i <= finish; i++) {
                DataObject stampObject = context.addObject(stamp);
                creationStampStamp.execute(objCreateStamp.getValue(), context, stampObject);
                sidStamp.execute(BaseUtils.padl(((Integer)i).toString(), stringStart.length(), '0'), context, stampObject);
            }
        }
    }

    public class CloneItemActionProperty extends CustomActionProperty {
        private ClassPropertyInterface itemInterface;

        public CloneItemActionProperty() {
            super(genSID(), "Копировать", new ValueClass[]{item});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            itemInterface = i.next();
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            DataObject cloneObject = context.getKeyValue(itemInterface);
            DataObject newObject = context.addObject(item);

            for(LP lp : new LP[]{colorSupplierItem, sizeSupplierItem})
                lp.execute(lp.read(context, cloneObject), context, newObject);
        }
    }

    public class SeekRouteActionProperty extends CustomActionProperty {

        private ClassPropertyInterface shipmentInterface;
        private ClassPropertyInterface skuInterface;
        private ClassPropertyInterface routeInterface;

        // route в интерфейсе нужен только, чтобы найти нужный ObjectInstance (не хочется бегать и искать его по массиву ObjectInstance)
        public SeekRouteActionProperty() {
            super(genSID(), "Поиск маршрута", new ValueClass[]{shipment, sku, route});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            shipmentInterface = i.next();
            skuInterface = i.next();
            routeInterface = i.next();
        }

        @Override
        public void execute(ExecutionContext context) throws SQLException {
            context.emitExceptionIfNotInFormSession();

            DataObject objShipment = context.getKeyValue(shipmentInterface);
            DataObject objSku = context.getKeyValue(skuInterface);

            DataObject objRouteRB = route.getDataObject("rb");
            DataObject objRouteRF = route.getDataObject("rf");

            Double invoiced = (Double) invoicedShipmentSku.read(context, objShipment, objSku);

            DataObject objRouteResult;
            if (invoiced == null) {
                Double percentRF = (Double) percentShipmentRouteSku.read(context, objShipment, objRouteRF, objSku);
                objRouteResult = (percentRF != null && percentRF > 1E-9) ? objRouteRF : objRouteRB;
            } else {

                Double invoicedRB = (Double) BaseUtils.nvl(invoicedShipmentRouteSku.read(context, objShipment, objRouteRB, objSku), 0.0);
                Double quantityRB = (Double) BaseUtils.nvl(quantityShipmentRouteSku.read(context, objShipment, objRouteRB, objSku), 0.0);

                Double invoicedRF = (Double) BaseUtils.nvl(invoicedShipmentRouteSku.read(context, objShipment, objRouteRF, objSku), 0.0);
                Double quantityRF = (Double) BaseUtils.nvl(quantityShipmentRouteSku.read(context, objShipment, objRouteRF, objSku), 0.0);

                if (quantityRB + 1E-9 < invoicedRB) {
                    objRouteResult = objRouteRB;
                } else if (quantityRF + 1E-9 < invoicedRF) {
                    objRouteResult = objRouteRF;
                } else {
                    Double percentRB = (Double) percentShipmentRouteSku.read(context, objShipment, objRouteRB, objSku);
                    objRouteResult = (percentRB != null && percentRB > 1E-9) ? objRouteRB : objRouteRF;
                }
            }

            ObjectInstance objectInstance = (ObjectInstance) context.getObjectInstance(routeInterface);
            if (!objRouteResult.equals(objectInstance.getObjectValue())) {
                try {
                    context.addAction(new AudioClientAction(getClass().getResourceAsStream(
                            objRouteResult.equals(objRouteRB) ? "/audio/rb.wav" : "/audio/rf.wav"
                    )));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            context.getFormInstance().seekObject(objectInstance, objRouteResult);
        }
    }
}
