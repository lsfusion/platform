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
    public final BaseLogicsModule<RomanBusinessLogics> LM;
    private final RomanBusinessLogics BL;

    public RomanLogicsModule(BaseLogicsModule<RomanBusinessLogics> LM, RomanBusinessLogics BL) {
        this.LM = LM;
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
    LP barcodeActionSeekPallet, barcodeActionSetPallet, barcodeActionSetFreight, barcodeAction3;
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

    public AnnexInvoiceFormEntity invoiceFromFormEntity;

    @Override
    public void initClasses() {
        currency = LM.addConcreteClass("currency", "Валюта", LM.baseClass.named);

        typeExchange = LM.addConcreteClass("typeExchange", "Тип обмена", LM.baseClass.named);

        destination = LM.addAbstractClass("destination", "Пункт назначения", LM.baseClass);

        store = LM.addConcreteClass("store", "Магазин", destination, LM.baseClass.named);

        sku = LM.addAbstractClass("sku", "SKU", LM.barcodeObject);

        article = LM.addAbstractClass("article", "Артикул", LM.baseClass);
        articleComposite = LM.addConcreteClass("articleComposite", "Артикул (составной)", article);
        articleSingle = LM.addConcreteClass("articleSingle", "Артикул (простой)", sku, article);

        pricat = LM.addConcreteClass("pricat", "Прайс", LM.baseClass);

        item = LM.addConcreteClass("item", "Товар", sku);

        document = LM.addAbstractClass("document", "Документ", LM.transaction);
        list = LM.addAbstractClass("list", "Список", LM.baseClass);

        contract = LM.addConcreteClass("contract", "Договор", LM.transaction);

        priceDocument = LM.addAbstractClass("priceDocument", "Документ с ценами", document);
        destinationDocument = LM.addAbstractClass("destinationDocument", "Документ в пункт назначения", document);

        order = LM.addConcreteClass("order", "Заказ", priceDocument, destinationDocument, list);

        invoice = LM.addAbstractClass("invoice", "Инвойс", priceDocument, destinationDocument);
        boxInvoice = LM.addConcreteClass("boxInvoice", "Инвойс по коробам", invoice);

        directInvoice = LM.addAbstractClass("directInvoice", "Инвойс (напрямую)", invoice);
        directBoxInvoice = LM.addConcreteClass("directBoxInvoice", "Инвойс по коробам (напрямую)", boxInvoice, directInvoice);

        simpleInvoice = LM.addConcreteClass("simpleInvoice", "Инвойс без коробов", invoice, list);

        shipDimension = LM.addConcreteClass("shipDimension", "Разрез поставки", LM.baseClass);

        stock = LM.addConcreteClass("stock", "Место хранения", LM.barcodeObject);

        freightUnit = LM.addAbstractClass("freightUnit", "Машиноместо", LM.baseClass);

        supplierBox = LM.addConcreteClass("supplierBox", "Короб поставщика", list, shipDimension, LM.barcodeObject, freightUnit);

        shipment = LM.addAbstractClass("shipment", "Поставка", document);
        boxShipment = LM.addConcreteClass("boxShipment", "Поставка по коробам", shipment);
        simpleShipment = LM.addConcreteClass("simpleShipment", "Поставка без коробов", shipment, shipDimension);

        shipmentDetail = LM.addAbstractClass("shipmentDetail", "Строка поставки", LM.baseClass);
        boxShipmentDetail = LM.addConcreteClass("boxShipmentDetail", "Строка поставки по коробам", shipmentDetail);
        simpleShipmentDetail = LM.addConcreteClass("simpleShipmentDetail", "Строка поставки без коробов", shipmentDetail);

        seller = LM.addAbstractClass("seller", "Продавец", LM.baseClass);

        supplier = LM.addConcreteClass("supplier", "Поставщик", LM.baseClass.named, seller);

        jennyferSupplier = LM.addConcreteClass("jennyferSupplier", "Jennyfer", supplier);
        tallyWeijlSupplier = LM.addConcreteClass("tallyWeijlSupplier", "Tally Weijl", supplier);
        hugoBossSupplier = LM.addConcreteClass("hugoBossSupplier", "Hugo Boss", supplier);
        mexxSupplier = LM.addConcreteClass("mexxSupplier", "Mexx", supplier);
        bestsellerSupplier = LM.addConcreteClass("bestsellerSupplier", "Bestseller", supplier);
        sOliverSupplier = LM.addConcreteClass("sOliverSupplier", "s.Oliver", supplier);

        secondNameClass = LM.addAbstractClass("secondNameClass", "Класс со вторым именем", LM.baseClass);

        subject = LM.addAbstractClass("subject", "Субъект", LM.baseClass.named, secondNameClass);
        importer = LM.addConcreteClass("importer", "Импортер", subject);
        exporter = LM.addConcreteClass("exporter", "Экспортер", subject, seller);

        commonSize = LM.addConcreteClass("commonSize", "Унифицированный размер", LM.baseClass.named);

        colorSupplier = LM.addConcreteClass("colorSupplier", "Цвет поставщика", LM.baseClass.named);
        sizeSupplier = LM.addConcreteClass("sizeSupplier", "Размер поставщика", LM.baseClass);

        freightBox = LM.addConcreteClass("freightBox", "Короб для транспортировки", stock, freightUnit);

        freight = LM.addConcreteClass("freight", "Фрахт", LM.baseClass.named, LM.transaction);
        freightComplete = LM.addConcreteClass("freightComplete", "Скомплектованный фрахт", freight);
        freightPriced = LM.addConcreteClass("freightPriced", "Расцененный фрахт", freightComplete);
        freightChanged = LM.addConcreteClass("freightChanged", "Обработанный фрахт", freightPriced);
        freightShipped = LM.addConcreteClass("freightShipped", "Отгруженный фрахт", freightChanged);

        freightType = LM.addConcreteClass("freightType", "Тип машины", LM.baseClass.named);

        pallet = LM.addConcreteClass("pallet", "Паллета", LM.barcodeObject);

        category = LM.addConcreteClass("category", "Номенклатурная группа", secondNameClass, LM.baseClass.named);

        customCategory = LM.addAbstractClass("customCategory", "Уровень ТН ВЭД", LM.baseClass);

        customCategory4 = LM.addConcreteClass("customCategory4", "Первый уровень", customCategory);
        customCategory6 = LM.addConcreteClass("customCategory6", "Второй уровень", customCategory);
        customCategory9 = LM.addConcreteClass("customCategory9", "Третий уровень", customCategory);
        customCategory10 = LM.addConcreteClass("customCategory10", "Четвёртый уровень", customCategory);

        customCategoryOrigin = LM.addConcreteClass("customCategoryOrigin", "ЕС уровень", customCategory);

        creationFreightBox = LM.addConcreteClass("creationFreightBox", "Операция создания коробов", LM.transaction);
        creationPallet = LM.addConcreteClass("creationPallet", "Операция создания паллет", LM.transaction);

        transfer = LM.addConcreteClass("transfer", "Внутреннее перемещение", LM.baseClass);

        unitOfMeasure = LM.addConcreteClass("unitOfMeasure", "Единица измерения", secondNameClass, LM.baseClass.named);

        brandSupplier = LM.addConcreteClass("brandSupplier", "Бренд поставщика", LM.baseClass.named);

        themeSupplier = LM.addConcreteClass("themeSupplier", "Тема поставщика", LM.baseClass.named);

        countrySupplier = LM.addConcreteClass("countrySupplier", "Страна поставщика", LM.baseClass.named);

        season = LM.addConcreteClass("season", "Сезон", LM.baseClass.named);

        route = LM.addStaticClass("route", "Маршрут", new String[]{"rb", "rf"}, new String[]{"РБ", "РФ"});
    }

    @Override
    public void initTables() {
        LM.tableFactory.include("customCategory4", customCategory4);
        LM.tableFactory.include("customCategory6", customCategory6);
        LM.tableFactory.include("customCategory9", customCategory9);
        LM.tableFactory.include("customCategory10", customCategory10);
        LM.tableFactory.include("customCategoryOrigin", customCategoryOrigin);
        LM.tableFactory.include("customCategory10Origin", customCategory10, customCategoryOrigin);
        LM.tableFactory.include("customCategory", customCategory);

        LM.tableFactory.include("article", article);
        LM.tableFactory.include("sku", sku);
        LM.tableFactory.include("documentArticle", document, article);
        LM.tableFactory.include("documentSku", document, sku);
        LM.tableFactory.include("listSku", list, sku);
        LM.tableFactory.include("listArticle", list, article);
        LM.tableFactory.include("importerFreightSku", importer, freight, sku);
        LM.tableFactory.include("freightSku", freight, sku);
        LM.tableFactory.include("shipmentDetail", shipmentDetail);
        LM.tableFactory.include("pallet", pallet);
        LM.tableFactory.include("freight", freight);
        LM.tableFactory.include("freightUnit", freightUnit);
        LM.tableFactory.include("barcodeObject", LM.barcodeObject);
        LM.tableFactory.include("rateExchange", typeExchange, currency, DateClass.instance);

        LM.tableFactory.include("pricat", pricat);
        LM.tableFactory.include("strings", StringClass.get(10));
    }

    @Override
    public void initGroups() {
        Settings.instance.setDisableSumGroupNotZero(true);

        skuAttributeGroup = new AbstractGroup("Атрибуты SKU");
        LM.baseGroup.add(skuAttributeGroup);

        itemAttributeGroup = new AbstractGroup("Атрибуты товара");
        LM.baseGroup.add(itemAttributeGroup);

        supplierAttributeGroup = new AbstractGroup ("Атрибуты поставщика");
        LM.publicGroup.add(supplierAttributeGroup);

        intraAttributeGroup = new AbstractGroup("Внутренние атрибуты");
        LM.publicGroup.add(intraAttributeGroup);

        importInvoiceActionGroup = new AbstractGroup("Импорт инвойсов");
        importInvoiceActionGroup.createContainer = false;
        LM.actionGroup.add(importInvoiceActionGroup);
    }

    @Override
    public void initProperties() {
        LM.idGroup.add(LM.objectValue);

        round2 = LM.addSFProp("round(CAST((prm1) as numeric), 2)", DoubleClass.instance, 1);

        // rate
        currencyTypeExchange = LM.addDProp(LM.idGroup, "currencyTypeExchange", "Валюта типа обмена (ИД)", currency, typeExchange);
        nameCurrencyTypeExchange = LM.addJProp(LM.baseGroup, "nameCurrencyTypeExchange", "Валюта типа обмена (наим.)", LM.name, currencyTypeExchange, 1);
        rateExchange = LM.addDProp(LM.baseGroup, "rateExchange", "Курс обмена", DoubleClass.instance, typeExchange, currency, DateClass.instance);
        typeExchangeSTX = LM.addDProp(LM.idGroup, "typeExchangeSTX", "Тип обмена для STX (ИД)", typeExchange);
        nameTypeExchangeSTX = LM.addJProp(LM.baseGroup, "nameTypeExchangeSTX", "Тип обмена для STX", LM.name, typeExchangeSTX);

        //lessCmpDate = LM.addJProp(LM.and(false, true, false), LM.object(DateClass.instance), 3, rateExchange, 1, 2, 3, greater2, 3, 4, is(DateClass.instance), 4);
        lessCmpDate = LM.addJProp(LM.and(false, true, false), LM.object(DateClass.instance), 3, rateExchange, 1, 2, 3, LM.addJProp(LM.greater2, 3, LM.date, 4), 1, 2, 3, 4, LM.is(document), 4);
        nearestPredDate = LM.addMGProp((AbstractGroup) null, "nearestPredDate", "Ближайшая меньшая дата", lessCmpDate, 1, 2, 4);
        nearestRateExchange = LM.addJProp("Ближайший курс обмена", rateExchange, 1, 2, nearestPredDate, 1, 2, 3);

        nameOrigin = LM.addDProp(LM.baseGroup, "nameOrigin", "Наименование (ориг.)", InsensitiveStringClass.get(50), secondNameClass);
        nameOriginCountry = LM.addDProp(LM.baseGroup, "nameOriginCountry", "Наименование (ориг.)", InsensitiveStringClass.get(50), LM.country);

        dictionaryComposition = LM.addDProp(LM.idGroup, "dictionaryComposition", "Словарь для составов (ИД)", LM.dictionary);
        nameDictionaryComposition = LM.addJProp(LM.baseGroup, "nameDictionaryComposition", "Словарь для составов", LM.name, dictionaryComposition);

        sidDestination = LM.addDProp(LM.baseGroup, "sidDestination", "Код", StringClass.get(50), destination);

        destinationSID = LM.addAGProp(LM.idGroup, "destinationSID", "Магазин (ИД)", sidDestination);

        sidBrandSupplier = LM.addDProp(LM.baseGroup, "sidBrandSupplier", "Код", StringClass.get(50), brandSupplier);

        // Contract
        sidContract = LM.addDProp(LM.baseGroup, "sidContract", "Номер договора", StringClass.get(50), contract);

        importerContract = LM.addDProp(LM.idGroup, "importerContract", "Импортер (ИД)", importer, contract);
        nameImporterContract = LM.addJProp(LM.baseGroup, "nameImporterContract", "Импортер", LM.name, importerContract, 1);

        sellerContract = LM.addDProp(LM.idGroup, "sellerContract", "Продавец (ИД)", seller, contract);
        nameSellerContract = LM.addJProp(LM.baseGroup, "nameSellerContract", "Продавец", LM.name, sellerContract, 1);

        currencyContract = LM.addDProp(LM.idGroup, "currencyContract", "Валюта (ИД)", currency, contract);
        nameCurrencyContract = LM.addJProp(LM.baseGroup, "nameCurrencyContract", "Валюта", LM.name, currencyContract, 1);

        // Subject
        addressOriginSubject = LM.addDProp(LM.baseGroup, "addressOriginSubject", "Address", StringClass.get(200), subject);
        addressSubject = LM.addDProp(LM.baseGroup, "addressSubject", "Адрес", StringClass.get(200), subject);

        contractImporter = LM.addDProp(LM.baseGroup, "contractImporter", "Номер договора", StringClass.get(50), importer);

        // CustomCategory
        nameCustomCategory = LM.addDProp(LM.baseGroup, "nameCustomCategory", "Наименование", StringClass.get(500), customCategory);
        nameCustomCategory.property.preferredCharWidth = 50;
        nameCustomCategory.property.minimumCharWidth = 20;

        sidCustomCategory4 = LM.addDProp(LM.baseGroup, "sidCustomCategory4", "Код(4)", StringClass.get(4), customCategory4);
        sidCustomCategory4.setFixedCharWidth(4);

        sidCustomCategory6 = LM.addDProp(LM.baseGroup, "sidCustomCategory6", "Код(6)", StringClass.get(6), customCategory6);
        sidCustomCategory6.setFixedCharWidth(6);

        sidCustomCategory9 = LM.addDProp(LM.baseGroup, "sidCustomCategory9", "Код(9)", StringClass.get(9), customCategory9);
        sidCustomCategory9.setFixedCharWidth(9);

        sidCustomCategory10 = LM.addDProp(LM.baseGroup, "sidCustomCategory10", "Код(10)", StringClass.get(10), customCategory10);
        sidCustomCategory10.setFixedCharWidth(10);

        sidCustomCategoryOrigin = LM.addDProp(LM.baseGroup, "sidCustomCategoryOrigin", "Код ЕС(10)", StringClass.get(10), customCategoryOrigin);
        sidCustomCategoryOrigin.setFixedCharWidth(10);

        sidToCustomCategory4 = LM.addAGProp("sidToCustomCategory4", "Код(4)", sidCustomCategory4);
        sidToCustomCategory6 = LM.addAGProp("sidToCustomCategory6", "Код(6)", sidCustomCategory6);
        sidToCustomCategory9 = LM.addAGProp("sidToCustomCategory9", "Код(9)", sidCustomCategory9);
        sidToCustomCategory10 = LM.addAGProp("sidToCustomCategory10", "Код(10)", sidCustomCategory10);
        sidToCustomCategoryOrigin = LM.addAGProp("sidToCustomCategoryOrigin", "Код ЕС (10)", sidCustomCategoryOrigin);

        importBelTnved = LM.addAProp(new ClassifierTNVEDImportActionProperty(LM.genSID(), "Импортировать (РБ)", this, "belarusian"));
        importEuTnved = LM.addAProp(new ClassifierTNVEDImportActionProperty(LM.genSID(), "Импортировать (ЕС)", this, "origin"));
        jennyferImportInvoice = LM.addAProp(importInvoiceActionGroup, new JennyferImportInvoiceActionProperty(this));
        tallyWeijlImportInvoice = LM.addAProp(importInvoiceActionGroup, new TallyWeijlImportInvoiceActionProperty(this));
        hugoBossImportInvoice = LM.addAProp(importInvoiceActionGroup, new HugoBossImportInvoiceActionProperty(BL));
        mexxImportInvoice = LM.addAProp(importInvoiceActionGroup, new MexxImportInvoiceActionProperty(this));
        mexxImportPricesInvoice = LM.addAProp(importInvoiceActionGroup, new MexxImportPricesInvoiceActionProperty(this));
        mexxImportArticleInfoInvoice = LM.addAProp(importInvoiceActionGroup, new MexxImportArticleInfoInvoiceActionProperty(this));
        mexxImportColorInvoice = LM.addAProp(importInvoiceActionGroup, new MexxImportColorInvoiceActionProperty(this));
        bestsellerImportInvoice = LM.addAProp(importInvoiceActionGroup, new BestsellerImportInvoiceActionProperty(BL));
        sOliverImportInvoice = LM.addAProp(importInvoiceActionGroup, new SOliverImportInvoiceActionProperty(BL));

        customCategory4CustomCategory6 = LM.addDProp(LM.idGroup, "customCategory4CustomCategory6", "Код(4)", customCategory4, customCategory6);
        customCategory6CustomCategory9 = LM.addDProp(LM.idGroup, "customCategory6CustomCategory9", "Код(6)", customCategory6, customCategory9);
        customCategory9CustomCategory10 = LM.addDProp(LM.idGroup, "customCategory9CustomCategory10", "Код(9)", customCategory9, customCategory10);
        customCategory6CustomCategory10 = LM.addJProp(LM.idGroup, "customCategory6CustomCategory10", "Код(6)", customCategory6CustomCategory9, customCategory9CustomCategory10, 1);
        customCategory4CustomCategory10 = LM.addJProp(LM.idGroup, "customCategory4CustomCategory10", "Код(4)", customCategory4CustomCategory6, customCategory6CustomCategory10, 1);

        customCategory6CustomCategoryOrigin = LM.addDProp(LM.idGroup, "customCategory6CustomCategoryOrigin", "Код(6)", customCategory6, customCategoryOrigin);
        customCategory4CustomCategoryOrigin = LM.addJProp(LM.idGroup, "customCategory4CustomCategoryOrigin", "Код(4)", customCategory4CustomCategory6, customCategory6CustomCategoryOrigin, 1);

        customCategory10CustomCategoryOrigin = LM.addDProp(LM.idGroup, "customCategory10CustomCategoryOrigin", "Код по умолчанию(ИД)", customCategory10, customCategoryOrigin);
        sidCustomCategory10CustomCategoryOrigin = LM.addJProp(LM.baseGroup, "sidCustomCategory10CustomCategoryOrigin", "Код по умолчанию", sidCustomCategory10, customCategory10CustomCategoryOrigin, 1);
        sidCustomCategory10CustomCategoryOrigin.property.preferredCharWidth = 10;
        sidCustomCategory10CustomCategoryOrigin.property.minimumCharWidth = 10;

        sidCustomCategory4CustomCategory6 = LM.addJProp(LM.baseGroup, "sidCustomCategory4CustomCategory6", "Код(4)", sidCustomCategory4, customCategory4CustomCategory6, 1);
        sidCustomCategory6CustomCategory9 = LM.addJProp(LM.baseGroup, "sidCustomCategory6CustomCategory9", "Код(6)", sidCustomCategory6, customCategory6CustomCategory9, 1);
        sidCustomCategory9CustomCategory10 = LM.addJProp(LM.idGroup, "sidCustomCategory9CustomCategory10", "Код(9)", sidCustomCategory9, customCategory9CustomCategory10, 1);
        sidCustomCategory6CustomCategoryOrigin = LM.addJProp(LM.idGroup, "sidCustomCategory6CustomCategoryOrigin", "Код(6)", sidCustomCategory6, customCategory6CustomCategoryOrigin, 1);

        nameCustomCategory4CustomCategory6 = LM.addJProp(LM.baseGroup, "nameCustomCategory4CustomCategory6", "Наименование(4)", nameCustomCategory, customCategory4CustomCategory6, 1);
        nameCustomCategory6CustomCategory9 = LM.addJProp(LM.baseGroup, "nameCustomCategory6CustomCategory9", "Наименование(6)", nameCustomCategory, customCategory6CustomCategory9, 1);
        nameCustomCategory9CustomCategory10 = LM.addJProp(LM.baseGroup, "nameCustomCategory9CustomCategory10", "Наименование(9)", nameCustomCategory, customCategory9CustomCategory10, 1);
        nameCustomCategory6CustomCategory10 = LM.addJProp(LM.baseGroup, "nameCustomCategory6CustomCategory10", "Наименование(6)", nameCustomCategory, customCategory6CustomCategory10, 1);
        nameCustomCategory4CustomCategory10 = LM.addJProp(LM.baseGroup, "nameCustomCategory4CustomCategory10", "Наименование(4)", nameCustomCategory, customCategory4CustomCategory10, 1);

        nameCustomCategory6CustomCategoryOrigin = LM.addJProp(LM.baseGroup, "nameCustomCategory6CustomCategoryOrigin", "Наименование(6)", nameCustomCategory, customCategory6CustomCategoryOrigin, 1);
        nameCustomCategory4CustomCategoryOrigin = LM.addJProp(LM.baseGroup, "nameCustomCategory4CustomCategoryOrigin", "Наименование(4)", nameCustomCategory, customCategory4CustomCategoryOrigin, 1);

        relationCustomCategory10CustomCategoryOrigin = LM.addDProp(LM.baseGroup, "relationCustomCategory10CustomCategoryOrigin", "Связь ТН ВЭД", LogicalClass.instance, customCategory10, customCategoryOrigin);

//        addConstraint(LM.addJProp("По умолчанию должен быть среди связанных", LM.and(true, false),
//                addCProp(LogicalClass.instance, true, customCategoryOrigin), 1,
//                LM.addJProp(relationCustomCategory10CustomCategoryOrigin, customCategory10CustomCategoryOrigin, 1, 1), 1,
//                LM.addJProp(LM.is(customCategory10), customCategory10CustomCategoryOrigin, 1), 1), true);

        // Supplier
        currencySupplier = LM.addDProp(LM.idGroup, "currencySupplier", "Валюта (ИД)", currency, supplier);
        nameCurrencySupplier = LM.addJProp(LM.baseGroup, "nameCurrencySupplier", "Валюта", LM.name, currencySupplier, 1);

        sidColorSupplier = LM.addDProp(LM.baseGroup, "sidColorSupplier", "Код", StringClass.get(50), colorSupplier);

        supplierColorSupplier = LM.addDProp(LM.idGroup, "supplierColorSupplier", "Поставщик (ИД)", supplier, colorSupplier);
        nameSupplierColorSupplier = LM.addJProp(LM.baseGroup, "nameSupplierColorSupplier", "Поставщик", LM.name, supplierColorSupplier, 1);

        colorSIDSupplier = LM.addAGProp(LM.idGroup, "colorSIDSupplier", "Цвет поставщика (ИД)", sidColorSupplier, supplierColorSupplier);

        sidSizeSupplier = LM.addDProp(LM.baseGroup, "sidSizeSupplier", "Код", StringClass.get(50), sizeSupplier);

        commonSizeSizeSupplier = LM.addDProp(LM.idGroup, "commonSizeSizeSupplier", "Унифицированный размер (ИД)", commonSize, sizeSupplier);
        nameCommonSizeSizeSupplier = LM.addJProp(LM.baseGroup, "nameCommonSizeSizeSupplier", "Унифицированный размер", LM.name, commonSizeSizeSupplier, 1);

        supplierSizeSupplier = LM.addDProp(LM.idGroup, "supplierSizeSupplier", "Поставщик (ИД)", supplier, sizeSupplier);
        nameSupplierSizeSupplier = LM.addJProp(LM.baseGroup, "nameSupplierSizeSupplier", "Поставщик", LM.name, supplierSizeSupplier, 1);

        sizeSIDSupplier = LM.addAGProp(LM.idGroup, "sizeSIDSupplier", "Размер поставщика (ИД)", sidSizeSupplier, supplierSizeSupplier);

        // Country
        supplierCountrySupplier = LM.addDProp(LM.idGroup, "supplierCountrySupplier", "Поставщик (ИД)", supplier, countrySupplier);
        nameSupplierCountrySupplier = LM.addJProp(LM.baseGroup, "nameSupplierCountrySupplier", "Поставщик", LM.name, supplierCountrySupplier, 1);

        countryCountrySupplier = LM.addDProp(LM.idGroup, "countryCountrySupplier", "Страна (ИД)", LM.country, countrySupplier);
        nameCountryCountrySupplier = LM.addJProp(LM.baseGroup, "nameCountryCountrySupplier", "Страна", LM.name, countryCountrySupplier, 1);

        countryNameSupplier = LM.addAGProp(LM.idGroup, "countryNameSupplier", "Страна поставщика", LM.name, supplierCountrySupplier);

        // Brand
        supplierBrandSupplier = LM.addDProp(LM.idGroup, "supplierBrandSupplier", "Поставщик (ИД)", supplier, brandSupplier);
        nameSupplierBrandSupplier = LM.addJProp(LM.baseGroup, "nameSupplierBrandSupplier", "Поставщик", LM.name, supplierBrandSupplier, 1);

        brandSIDSupplier = LM.addAGProp(LM.idGroup, "brandSIDSupplier", "Бренд поставщика (ИД)", sidBrandSupplier, supplierBrandSupplier);

        brandSupplierSupplier = LM.addDProp(LM.idGroup, "brandSupplierSupplier", "Бренд (ИД)", brandSupplier, supplier);
        nameBrandSupplierSupplier = LM.addJProp(LM.baseGroup, "nameBrandSupplierSupplier", "Бренд по умолчанию", LM.name, brandSupplierSupplier, 1);

        LM.addConstraint(LM.addJProp("Бренд по умолчанию для поставщика должен соответствовать брендам поставщика", LM.diff2, 1, LM.addJProp(supplierBrandSupplier, brandSupplierSupplier, 1), 1), true);

        supplierThemeSupplier = LM.addDProp(LM.idGroup, "supplierThemeSupplier", "Поставщик (ИД)", supplier, themeSupplier);

        supplierDocument = LM.addDProp(LM.idGroup, "supplierDocument", "Поставщик (ИД)", supplier, document);
        supplierPriceDocument = LM.addJProp(LM.idGroup, "supplierPricedDocument", "Поставщик(ИД)", LM.and1, supplierDocument, 1, LM.is(priceDocument), 1);
        nameSupplierDocument = LM.addJProp(LM.baseGroup, "nameSupplierDocument", "Поставщик", LM.name, supplierDocument, 1);

        currencyDocument = LM.addDCProp(LM.idGroup, "currencyDocument", "Валюта (ИД)", currencySupplier, supplierPriceDocument, 1);
        nameCurrencyDocument = LM.addJProp(LM.baseGroup, "nameCurrencyDocument", "Валюта", LM.name, currencyDocument, 1);

        // Order
        destinationDestinationDocument = LM.addDProp(LM.idGroup, "destinationDestinationDocument", "Пункт назначения (ИД)", destination, destinationDocument);
        nameDestinationDestinationDocument = LM.addJProp(LM.baseGroup, "nameDestinationDestinationDocument", "Пункт назначения (наим.)", LM.name, destinationDestinationDocument, 1);
        sidDestinationDestinationDocument = LM.addJProp(LM.baseGroup, "sidDestinationDestinationDocument", "Пункт назначения", sidDestination, destinationDestinationDocument, 1);

        // Invoice
        importerInvoice = LM.addDProp(LM.idGroup, "importerDocument", "Импортер (ИД)", importer, invoice);
        nameImporterInvoice = LM.addJProp(LM.baseGroup, "nameImporterInvoice", "Импортер", LM.name, importerInvoice, 1);

        contractInvoice = LM.addDProp(LM.idGroup, "contractInvoice", "Договор (ИД)", contract, invoice);
        sidContractInvoice = LM.addJProp(LM.baseGroup, "sidContractInvoice", "Договор", sidContract, contractInvoice, 1);

        LM.addConstraint(LM.addJProp("Импортер договора должен соответствовать импортеру инвойса", LM.diff2,
                importerInvoice, 1, LM.addJProp(importerContract, contractInvoice, 1), 1), true);

        LM.addConstraint(LM.addJProp("Поставщик договора должен соответствовать поставщику инвойса", LM.diff2,
                supplierDocument, 1, LM.addJProp(sellerContract, contractInvoice, 1), 1), true);


        // Shipment
        quantityPalletShipment = LM.addDProp(LM.baseGroup, "quantityPalletShipment", "Кол-во паллет", IntegerClass.instance, shipment);
        netWeightShipment = LM.addDProp(LM.baseGroup, "netWeightShipment", "Вес нетто", DoubleClass.instance, shipment);
        grossWeightShipment = LM.addDProp(LM.baseGroup, "grossWeightShipment", "Вес брутто", DoubleClass.instance, shipment);

        grossWeightPallet = LM.addDProp(LM.baseGroup, "grossWeightPallet", "Вес брутто", DoubleClass.instance, pallet);
        quantityBoxShipment = LM.addDProp(LM.baseGroup, "quantityBoxShipment", "Кол-во коробов", DoubleClass.instance, shipment);

        // Item
        articleCompositeItem = LM.addDProp(LM.idGroup, "articleCompositeItem", "Артикул (ИД)", articleComposite, item);
        equalsItemArticleComposite = LM.addJProp(LM.baseGroup, "equalsItemArticleComposite", "Вкл.", LM.equals2, articleCompositeItem, 1, 2);

        articleSku = LM.addCUProp(LM.idGroup, "articleSku", "Артикул (ИД)", LM.object(articleSingle), articleCompositeItem);

        addItemBarcode = LM.addJProp(true, "Ввод товара по штрих-коду", LM.addAAProp(item, LM.barcode), 1);

        // Article
        sidArticle = LM.addDProp(LM.baseGroup, "sidArticle", "Артикул", StringClass.get(50), article);
        sidArticleSku = LM.addJProp(LM.baseGroup, "sidArticleSku", "Артикул", sidArticle, articleSku, 1);

        originalNameArticle = LM.addDProp(supplierAttributeGroup, "originalNameArticle", "Наименование (ориг.)", InsensitiveStringClass.get(50), article);
        originalNameArticleSku = LM.addJProp(supplierAttributeGroup, "originalNameArticleSku", "Наименование (ориг.)", originalNameArticle, articleSku, 1);

        //Category
        categoryArticle = LM.addDProp(LM.idGroup, "categoryArticle", "Номенклатурная группа товара (ИД)", category, article);
        nameOriginCategoryArticle = LM.addJProp(intraAttributeGroup, "nameOriginCategoryArticle", "Номенклатурная группа товара (ориг.)", nameOrigin, categoryArticle, 1);
        nameCategoryArticle = LM.addJProp(intraAttributeGroup, "nameCategoryArticle", "Номенклатурная группа товара", LM.name, categoryArticle, 1);
        categoryArticleSku = LM.addJProp(LM.idGroup, true, "categoryArticleSku", "Номенклатурная группа товара (ИД)", categoryArticle, articleSku, 1);
        nameCategoryArticleSku = LM.addJProp(intraAttributeGroup, "nameCategoryArticleSku", "Номенклатурная группа товара", LM.name, categoryArticleSku, 1);
        nameOriginCategoryArticleSku = LM.addJProp(intraAttributeGroup, "nameOriginCategoryArticleSku", "Номенклатурная группа товара", nameOrigin, categoryArticleSku, 1);

        nameArticle = LM.addSUProp(LM.baseGroup, "nameArticle", "Наименование", Union.OVERRIDE, originalNameArticle, nameOriginCategoryArticle);
        nameArticleSku = LM.addJProp(LM.baseGroup, "nameArticleSku", "Наименование", nameArticle, articleSku, 1);

        customCategoryOriginArticle = LM.addDProp(LM.idGroup, "customCategoryOriginArticle", "ТН ВЭД (ориг.) (ИД)", customCategoryOrigin, article);
        sidCustomCategoryOriginArticle = LM.addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticle", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategoryOriginArticleSku = LM.addJProp(LM.idGroup, true, "customCategoryOriginArticleSku", "ТН ВЭД (ориг.) (ИД)", customCategoryOriginArticle, articleSku, 1);
        sidCustomCategoryOriginArticleSku = LM.addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticleSku", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticleSku, 1);

        customCategory10DataSku = LM.addDProp(LM.idGroup, "customCategory10DataSku", "ТН ВЭД (ИД)", customCategory10, sku);
        customCategory10CustomCategoryOriginArticle = LM.addJProp(LM.idGroup, "customCategory10CustomCategoryOriginArticle", "ТН ВЭД (ИД)", customCategory10CustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategory10CustomCategoryOriginArticleSku = LM.addJProp(LM.idGroup, "customCategory10CustomCategoryOriginArticleSku", "ТН ВЭД (ИД)", customCategory10CustomCategoryOriginArticle, articleSku, 1);
        customCategory10Sku = LM.addSUProp(LM.idGroup, "customCategory10Sku", true, "ТН ВЭД (ИД)", Union.OVERRIDE, customCategory10CustomCategoryOriginArticleSku, customCategory10DataSku);
        sidCustomCategory10Sku = LM.addJProp(LM.baseGroup, "sidCustomCategory10Sku", "ТН ВЭД", sidCustomCategory10, customCategory10Sku, 1);
        /*addConstraint(LM.addJProp("Выбранный должен быть среди связанных кодов", andNot1, addCProp(LogicalClass.instance, true, article), 1,
                   LM.addJProp(relationCustomCategory10CustomCategoryOrigin, customCategory10Article, 1, customCategoryOriginArticle, 1), 1), true);*/

        // unitOfMeasure
        unitOfMeasureCategory = LM.addDProp(LM.idGroup, "unitOfMeasureCategory", "Единица измерения (ИД)", unitOfMeasure, category);
        nameUnitOfMeasureCategory = LM.addJProp(LM.baseGroup, "nameUnitOfMeasureCategory", "Единица измерения", LM.name, unitOfMeasureCategory, 1);
        unitOfMeasureCategoryArticle = LM.addJProp(LM.idGroup, "unitOfMeasureCategoryArticle", "Единица измерения (ИД)", unitOfMeasureCategory, categoryArticle, 1);
        unitOfMeasureDataArticle = LM.addDProp(LM.idGroup, "unitOfMeasureDataArticle", "Единица измерения (ИД)", unitOfMeasure, article);
        unitOfMeasureArticle = LM.addSUProp(LM.idGroup, "unitOfMeasureArticle", "Единица измерения", Union.OVERRIDE, unitOfMeasureCategoryArticle, unitOfMeasureDataArticle);

        nameOriginUnitOfMeasureArticle = LM.addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticle", "Единица измерения (ориг.)", nameOrigin, unitOfMeasureArticle, 1);
        nameUnitOfMeasureArticle = LM.addJProp(intraAttributeGroup, "nameUnitOfMeasureArticle", "Единица измерения", LM.name, unitOfMeasureArticle, 1);
        unitOfMeasureArticleSku = LM.addJProp(LM.idGroup, true, "unitOfMeasureArticleSku", "Ед. изм. товара (ИД)", unitOfMeasureArticle, articleSku, 1);
        nameUnitOfMeasureArticleSku = LM.addJProp(intraAttributeGroup, "nameUnitOfMeasureArticleSku", "Ед. изм. товара", LM.name, unitOfMeasureArticleSku, 1);
        nameOriginUnitOfMeasureArticleSku = LM.addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticleSku", "Ед. изм. товара", nameOrigin, unitOfMeasureArticleSku, 1);

        // Supplier
        supplierArticle = LM.addDProp(LM.idGroup, "supplierArticle", "Поставщик (ИД)", supplier, article);
        nameSupplierArticle = LM.addJProp(LM.baseGroup, "nameSupplierArticle", "Поставщик", LM.name, supplierArticle, 1);

        jennyferSupplierArticle = LM.addJProp("jennyferSupplierArticle", "Поставщик Jennyfer (ИД)", LM.and1, supplierArticle, 1, LM.addJProp(LM.is(jennyferSupplier), supplierArticle, 1), 1);

        brandSupplierDataArticle = LM.addDProp(LM.idGroup, "brandSupplierDataArticle", "Бренд (ИД)", brandSupplier, article);
        brandSupplierSupplierArticle = LM.addJProp(LM.idGroup, "brandSupplierSupplierArticle", "Бренд (ИД)", brandSupplierSupplier, supplierArticle, 1);
        brandSupplierArticle = LM.addSUProp(LM.idGroup, "brandSupplierArticle", "Бренд (ИД)", Union.OVERRIDE, brandSupplierSupplierArticle, brandSupplierDataArticle);
        nameBrandSupplierArticle = LM.addJProp(supplierAttributeGroup, "nameBrandSupplierArticle", "Бренд", LM.name, brandSupplierArticle, 1);
        sidBrandSupplierArticle = LM.addJProp(supplierAttributeGroup, "sidBrandSupplierArticle", "Бренд (ИД)", sidBrandSupplier, brandSupplierArticle, 1);

        supplierBrandSupplierArticle = LM.addJProp(LM.idGroup, "supplierBrandSupplierArticle", "Поставщик", supplierBrandSupplier, brandSupplierArticle, 1);
        LM.addConstraint(LM.addJProp("Поставщик артикула должен соответствовать поставщику бренда артикула", LM.diff2,
                supplierArticle, 1, LM.addJProp(supplierBrandSupplier, brandSupplierArticle, 1), 1), true);

        brandSupplierArticleSku = LM.addJProp(LM.idGroup, "brandSupplierArticleSku", "Бренд (ИД)", brandSupplierArticle, articleSku, 1);
        nameBrandSupplierArticleSku = LM.addJProp(supplierAttributeGroup, "nameBrandSupplierArticleSku", "Бренд", LM.name, brandSupplierArticleSku, 1);
        sidBrandSupplierArticleSku = LM.addJProp(supplierAttributeGroup, "sidBrandSupplierArticleSku", "Бренд(ИД)", sidBrandSupplier, brandSupplierArticleSku, 1);

        themeSupplierArticle = LM.addDProp(LM.idGroup, "themeSupplierDataArticle", "Тема (ИД)", themeSupplier, article);
        nameThemeSupplierArticle = LM.addJProp(supplierAttributeGroup, "nameThmeSupplierArticle", "Тема", LM.name, themeSupplierArticle, 1);

        LM.addConstraint(LM.addJProp("Поставщик артикула должен соответствовать поставщику темы артикула", LM.diff2,
                supplierArticle, 1, LM.addJProp(supplierThemeSupplier, themeSupplierArticle, 1), 1), true);

        themeSupplierArticleSku = LM.addJProp(LM.idGroup, "themeSupplierArticleSku", "Тема (ИД)", themeSupplierArticle, articleSku, 1);
        nameThemeSupplierArticleSku = LM.addJProp(supplierAttributeGroup, "nameThemeSupplierArticleSku", "Тема", LM.name, themeSupplierArticleSku, 1);

        seasonArticle = LM.addDProp(LM.idGroup, "seasonArticle", "Сезон (ИД)", season, article);
        nameSeasonArticle = LM.addJProp(supplierAttributeGroup, "nameSeasonArticle", "Сезон", LM.name, seasonArticle, 1);

        articleSIDSupplier = LM.addAGProp(LM.idGroup, "articleSIDSupplier", "Артикул (ИД)", sidArticle, supplierArticle);

        seekArticleSIDSupplier = LM.addJProp(true, "Поиск артикула", LM.addSAProp(null), articleSIDSupplier, 1, 2);

        addArticleSingleSIDSupplier = LM.addJProp(true, "Ввод простого артикула", LM.addAAProp(articleSingle, sidArticle, supplierArticle), 1, 2);
        addNEArticleSingleSIDSupplier = LM.addJProp(true, "Ввод простого артикула (НС)", LM.andNot1, addArticleSingleSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);

        addArticleCompositeSIDSupplier = LM.addJProp(true, "Ввод составного артикула", LM.addAAProp(articleComposite, sidArticle, supplierArticle), 1, 2);
        addNEArticleCompositeSIDSupplier = LM.addJProp(true, "Ввод составного артикула (НС)", LM.andNot1, addArticleCompositeSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);

        executeArticleCompositeItemSIDSupplier = LM.addJProp(true, "Замена артикула", LM.addEPAProp(articleCompositeItem), 1, articleSIDSupplier, 2, 3);

        executeChangeFreightClass = LM.addJProp(true, "Изменить класс фрахта", LM.and1, LM.addEPAProp(LM.objectClass), 1, 2, LM.is(freight), 1);

        supplierArticleSku = LM.addJProp(LM.idGroup, "supplierArticleSku", "Поставщик (ИД)", supplierArticle, articleSku, 1);
        nameSupplierArticleSku = LM.addJProp(LM.baseGroup, "nameSupplierArticleSku", "Поставщик", LM.name, supplierArticleSku, 1);

        jennyferSupplierArticleSku = LM.addJProp("jennyferSupplierArticleSku", "Поставщик Jennyfer (ИД)", jennyferSupplierArticle, articleSku, 1);

        colorSupplierItem = LM.addDProp(LM.idGroup, "colorSupplierItem", "Цвет поставщика (ИД)", colorSupplier, item);
        sidColorSupplierItem = LM.addJProp(itemAttributeGroup, "sidColorSupplierItem", "Код цвета", sidColorSupplier, colorSupplierItem, 1);
        nameColorSupplierItem = LM.addJProp(itemAttributeGroup, "nameColorSupplierItem", "Цвет поставщика", LM.name, colorSupplierItem, 1);

        sizeSupplierItem = LM.addDProp(itemAttributeGroup, "sizeSupplierItem", "Размер поставщика (ИД)", sizeSupplier, item);
        sidSizeSupplierItem = LM.addJProp(itemAttributeGroup, "sidSizeSupplierItem", "Размер поставщика", sidSizeSupplier, sizeSupplierItem, 1);

        LM.addConstraint(LM.addJProp("Поставщик товара должен соответствовать цвету поставщика", LM.diff2,
                supplierArticleSku, 1,
                LM.addJProp(supplierColorSupplier, colorSupplierItem, 1), 1), true);

        LM.addConstraint(LM.addJProp("Поставщик товара должен соответствовать размеру поставщика", LM.diff2,
                supplierArticleSku, 1,
                LM.addJProp(supplierSizeSupplier, sizeSupplierItem, 1), 1), true);

        // Weight
        netWeightArticle = LM.addDProp(supplierAttributeGroup, "netWeightArticle", "Вес нетто (ориг.)", DoubleClass.instance, article);
        netWeightArticleSku = LM.addJProp(intraAttributeGroup, "netWeightArticleSku", "Вес нетто (ориг.)", netWeightArticle, articleSku, 1);
        netWeightArticleSize = LM.addDProp(intraAttributeGroup, "netWeightArticleSize", "Вес нетто размера", DoubleClass.instance, article, sizeSupplier);

        netWeightDataSku = LM.addDProp(intraAttributeGroup, "netWeightDataSku", "Вес нетто", DoubleClass.instance, sku);
        netWeightArticleSizeSku = LM.addJProp(intraAttributeGroup, true, "netWeightArticleSizeSku", "Вес нетто", netWeightArticleSize, articleSku, 1, sizeSupplierItem, 1);
        netWeightSku = LM.addSUProp(intraAttributeGroup, "netWeightSku", "Вес нетто", Union.OVERRIDE, netWeightArticleSku, netWeightArticleSizeSku);

        // Country
        countrySupplierOfOriginArticle = LM.addDProp(LM.idGroup, "countrySupplierOfOriginArticle", "Страна происхождения (ИД)", countrySupplier, article);
        nameCountrySupplierOfOriginArticle = LM.addJProp(supplierAttributeGroup, "nameCountrySupplierOfOriginArticle", "Страна происхождения (ориг.)", LM.name, countrySupplierOfOriginArticle, 1);

        countrySupplierOfOriginArticleSku = LM.addJProp(LM.idGroup, "countrySupplierOfOriginArticleSku", "Страна происхождения (ИД)", countrySupplierOfOriginArticle, articleSku, 1);
        nameCountrySupplierOfOriginArticleSku = LM.addJProp(supplierAttributeGroup, "nameCountrySupplierOfOriginArticleSku", "Страна происхождения (ориг.)", LM.name, countrySupplierOfOriginArticleSku, 1);

        countryOfOriginArticle = LM.addJProp(LM.idGroup, "countryOfOriginArticle", "Страна происхождения (ИД)", countryCountrySupplier, countrySupplierOfOriginArticle, 1);
        nameCountryOfOriginArticle = LM.addJProp(supplierAttributeGroup, "nameCountryOfOriginArticle", "Страна происхождения", nameOriginCountry, countryOfOriginArticle, 1);

        countryOfOriginArticleSku = LM.addJProp(LM.idGroup, "countryOfOriginArticleSku", "Страна происхождения (ИД)", countryOfOriginArticle, articleSku, 1);
        nameCountryOfOriginArticleSku = LM.addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSku", "Страна происхождения", nameOriginCountry, countryOfOriginArticleSku, 1);

        countryOfOriginArticleColor = LM.addDProp(LM.idGroup, "countryOfOriginArticleColor", "Страна происхождения (ИД)", LM.country, article, colorSupplier);
        countryOfOriginArticleColorSku = LM.addJProp(LM.idGroup, true, "countryOfOriginArticleColorSku", "Страна происхождения (ИД)", countryOfOriginArticleColor, articleSku, 1, colorSupplierItem, 1);

        countryOfOriginDataSku = LM.addDProp(LM.idGroup, "countryOfOriginDataSku", "Страна происхождения (ИД) (первичное)", LM.country, sku);

        countryOfOriginSku = LM.addSUProp(LM.idGroup, "countryOfOriginSku", true, "Страна происхождения (ИД)", Union.OVERRIDE, countryOfOriginArticleSku, countryOfOriginArticleColorSku);

        nameCountryOfOriginSku = LM.addJProp(intraAttributeGroup, "nameCountryOfOriginSku", "Страна происхождения", nameOriginCountry, countryOfOriginSku, 1);
        nameCountrySku = LM.addJProp(intraAttributeGroup, "nameCountrySku", "Страна происхождения", LM.name, countryOfOriginSku, 1);

        LM.addConstraint(LM.addJProp("Поставщик артикула должен соответствовать поставщику страны артикула", LM.diff2,
                supplierArticle, 1, LM.addJProp(supplierCountrySupplier, countrySupplierOfOriginArticle, 1), 1), true);

        // Composition
        mainCompositionOriginArticle = LM.addDProp(supplierAttributeGroup, "mainCompositionOriginArticle", "Состав", COMPOSITION_CLASS, article);
        additionalCompositionOriginArticle = LM.addDProp(supplierAttributeGroup, "additionalCompositionOriginArticle", "Доп. состав", COMPOSITION_CLASS, article);

        mainCompositionOriginArticleSku = LM.addJProp(supplierAttributeGroup, "mainCompositionOriginArticleSku", "Состав", mainCompositionOriginArticle, articleSku, 1);
        additionalCompositionOriginArticleSku = LM.addJProp(supplierAttributeGroup, "additionalCompositionOriginArticleSku", "Доп. состав", additionalCompositionOriginArticle, articleSku, 1);

        mainCompositionOriginArticleColor = LM.addDProp(supplierAttributeGroup, "mainCompositionOriginArticleColor", "Состав", COMPOSITION_CLASS, article, colorSupplier);
        additionalCompositionOriginArticleColor = LM.addDProp(supplierAttributeGroup, "additionalCompositionOriginArticleColor", "Доп. состав", COMPOSITION_CLASS, article, colorSupplier);

        mainCompositionOriginArticleColorSku = LM.addJProp(supplierAttributeGroup, true, "mainCompositionOriginArticleColorSku", "Состав", mainCompositionOriginArticleColor, articleSku, 1, colorSupplierItem, 1);
        additionalCompositionOriginArticleColorSku = LM.addJProp(supplierAttributeGroup, true, "additionalCompositionOriginArticleColorSku", "Доп. состав", additionalCompositionOriginArticleColor, articleSku, 1, colorSupplierItem, 1);

        mainCompositionOriginDataSku = LM.addDProp(intraAttributeGroup, "mainCompositionOriginDataSku", "Состав", COMPOSITION_CLASS, sku);
        additionalCompositionOriginDataSku = LM.addDProp(intraAttributeGroup, "additionalCompositionOriginDataSku", "Доп. состав", COMPOSITION_CLASS, sku);

        mainCompositionOriginSku = LM.addSUProp(intraAttributeGroup, "mainCompositionOriginSku", true, "Состав", Union.OVERRIDE, mainCompositionOriginArticleSku, mainCompositionOriginArticleColorSku);
        additionalCompositionOriginSku = LM.addSUProp(intraAttributeGroup, "additionalCompositionOriginSku", "Доп. состав", Union.OVERRIDE, additionalCompositionOriginArticleSku, additionalCompositionOriginArticleColorSku);

        mainCompositionArticle = LM.addDProp(intraAttributeGroup, "mainCompositionArticle", "Состав (перевод)", COMPOSITION_CLASS, article);
        additionalCompositionArticle = LM.addDProp(intraAttributeGroup, "additionalCompositionArticle", "Доп. состав (перевод)", COMPOSITION_CLASS, article);

        mainCompositionSku = LM.addDProp(intraAttributeGroup, "mainCompositionSku", "Состав (перевод)", COMPOSITION_CLASS, sku);
        additionalCompositionSku = LM.addDProp(intraAttributeGroup, "additionalCompositionSku", "Доп. состав (перевод)", COMPOSITION_CLASS, sku);

        substring10 = LM.addSFProp("substring(prm1,1,10)", StringClass.get(10), 1);
        substring10s13 = LM.addJProp(LM.and1, substring10, 1, LM.is(StringClass.get(13)), 1);

        barcode10 = LM.addJProp("barcode10", "Штрих-код(10)", substring10, LM.barcode, 1);
        skuJennyferBarcode10 = LM.addMGProp("skuJennyferBarcode10", "Товар (ИД)", LM.addJProp(LM.and1, LM.object(sku), 1, LM.addJProp(LM.is(jennyferSupplier), supplierArticleSku, 1), 1),
                barcode10, 1);
        skuJennyferBarcode = LM.addJProp("skuJennyferBarcode", "Товар (ИД)", skuJennyferBarcode10, substring10s13, 1);

        skuBarcodeObject = LM.addSUProp(Union.OVERRIDE, LM.barcodeToObject, skuJennyferBarcode);

        sidDocument = LM.addDProp(LM.baseGroup, "sidDocument", "Код документа", StringClass.get(50), document);
        documentSIDSupplier = LM.addAGProp(LM.idGroup, "documentSIDSupplier", "Документ поставщика (ИД)", sidDocument, supplierDocument);

        // коробки
        sidSupplierBox = LM.addDProp(LM.baseGroup, "sidSupplierBox", "Номер короба", StringClass.get(50), supplierBox);

        boxInvoiceSupplierBox = LM.addDProp(LM.idGroup, "boxInvoiceSupplierBox", "Документ по коробам (ИД)", boxInvoice, supplierBox);
        LM.setNotNull(boxInvoiceSupplierBox);

        sidBoxInvoiceSupplierBox = LM.addJProp(LM.baseGroup, "sidBoxInvoiceSupplierBox", "Документ по коробам", sidDocument, boxInvoiceSupplierBox, 1);

        destinationSupplierBox = LM.addJProp(LM.idGroup, "destinationSupplierBox", "Пункт назначения (ИД)", destinationDestinationDocument, boxInvoiceSupplierBox, 1);
        nameDestinationSupplierBox = LM.addJProp(LM.baseGroup, "nameDestinationSupplierBox", "Пункт назначения", LM.name, destinationSupplierBox, 1);

        supplierSupplierBox = LM.addJProp(LM.idGroup, "supplierSupplierBox", "Поставщик (ИД)", supplierDocument, boxInvoiceSupplierBox, 1);

        supplierBoxSIDSupplier = LM.addAGProp(LM.idGroup, "supplierBoxSIDSupplier", "Короб поставщика (ИД)", sidSupplierBox, supplierSupplierBox);

        seekSupplierBoxSIDSupplier = LM.addJProp(true, "Поиск короба поставщика", LM.addSAProp(null), supplierBoxSIDSupplier, 1, 2);

        // заказ по артикулам
        documentList = LM.addCUProp(LM.idGroup, "documentList", "Документ (ИД)", LM.object(order), LM.object(simpleInvoice), boxInvoiceSupplierBox);
        supplierList = LM.addJProp(LM.idGroup, "supplierList", "Поставщик (ИД)", supplierDocument, documentList, 1);

        articleSIDList = LM.addJProp(LM.idGroup, "articleSIDList", "Артикул (ИД)", articleSIDSupplier, 1, supplierList, 2);

        numberListArticle = LM.addDProp(LM.baseGroup, "numberListArticle", "Номер", IntegerClass.instance, list, article);
        numberListSIDArticle = LM.addJProp(numberListArticle, 1, articleSIDList, 2, 1);

        numberDataListSku = LM.addDProp(LM.baseGroup, "numberDataListSku", "Номер", IntegerClass.instance, list, sku);
        numberArticleListSku = LM.addJProp(LM.baseGroup, "numberArticleListSku", "Номер (артикула)", numberListArticle, 1, articleSku, 2);

        numberListSku = LM.addSUProp("numberListSku", "Номер", Union.OVERRIDE, numberArticleListSku, numberDataListSku);

        numberDocumentArticle = LM.addSGProp(LM.baseGroup, "inDocumentArticle", numberListArticle, documentList, 1, 2);

        incrementNumberListSID = LM.addJProp(true, "Добавить строку", LM.andNot1,
                LM.addJProp(true, LM.addIAProp(numberListArticle, 1),
                        1, articleSIDList, 2, 1), 1, 2,
                numberListSIDArticle, 1, 2); // если еще не было добавлено такой строки

        //price and catalog (pricat)
        barcodePricat = LM.addDProp(LM.baseGroup, "barcodePricat", "Штрих-код", StringClass.get(13), pricat);
        articleNumberPricat = LM.addDProp(LM.baseGroup, "articleNumberPricat", "Артикул", StringClass.get(20), pricat);
        customCategoryOriginalPricat = LM.addDProp(LM.baseGroup, "customCategoryOriginalPricat", "Код ЕС (10)", StringClass.get(10), pricat);
        colorCodePricat = LM.addDProp(LM.baseGroup, "colorCodePricat", "Код цвета", StringClass.get(20), pricat);
        colorNamePricat = LM.addDProp(LM.baseGroup, "colorNamePricat", "Цвет", StringClass.get(50), pricat);
        sizePricat = LM.addDProp(LM.baseGroup, "sizePricat", "Размер", StringClass.get(5), pricat);
        originalNamePricat = LM.addDProp(LM.baseGroup, "originalNamePricat", "Наименование (ориг.)", StringClass.get(50), pricat);
        countryPricat = LM.addDProp(LM.baseGroup, "countryPricat", "Страна происхождения", StringClass.get(20), pricat);
        netWeightPricat = LM.addDProp(LM.baseGroup, "netWeightPricat", "Вес нетто", DoubleClass.instance, pricat);
        compositionPricat = LM.addDProp(LM.baseGroup, "compositionPricat", "Состав", StringClass.get(50), pricat);
        pricePricat = LM.addDProp(LM.baseGroup, "pricePricat", "Цена", DoubleClass.instance, pricat);
        rrpPricat = LM.addDProp(LM.baseGroup, "RRP", "Рекомендованная цена", DoubleClass.instance, pricat);
        supplierPricat = LM.addDProp("supplierPricat", "Поставщик", supplier, pricat);
        barcodeToPricat = LM.addAGProp("barcodeToPricat", "штрих-код", barcodePricat);
        importPricatSupplier = LM.addProperty(null, new LP<ClassPropertyInterface>(new PricatEDIImportActionProperty(LM.genSID(), this, supplier)));
        hugoBossImportPricat = LM.addProperty(null, new LP<ClassPropertyInterface>(new HugoBossPricatCSVImportActionProperty(LM.genSID(), this, hugoBossSupplier)));

        // кол-во заказа
        quantityDataListSku = LM.addDProp("quantityDataListSku", "Кол-во (первичное)", DoubleClass.instance, list, sku);
        quantityListSku = quantityDataListSku; //LM.addJProp(LM.baseGroup, "quantityListSku", true, "Кол-во", LM.and1, quantityDataListSku, 1, 2, numberListSku, 1, 2);

        quantityDocumentSku = LM.addSGProp(LM.baseGroup, "quantityDocumentSku", true, "Кол-во в документе", quantityListSku, documentList, 1, 2);
        quantityDocumentArticle = LM.addSGProp(LM.baseGroup, "quantityDocumentArticle", "Кол-во артикула в документе", quantityDocumentSku, 1, articleSku, 2);
        quantityDocument = LM.addSGProp(LM.baseGroup, "quantityDocument", "Общее кол-во в документе", quantityDocumentSku, 1);

        // связь инвойсов и заказов
        inOrderInvoice = LM.addDProp(LM.baseGroup, "inOrderInvoice", "Вкл", LogicalClass.instance, order, invoice);

        LM.addConstraint(LM.addJProp("Магазин инвойса должен совпадать с магазином заказа", LM.and1,
                LM.addJProp(LM.diff2, destinationDestinationDocument, 1, destinationDestinationDocument, 2), 1, 2,
                inOrderInvoice, 1, 2), true);

        orderedOrderInvoiceSku = LM.addJProp(LM.and1, quantityDocumentSku, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceSku = LM.addSGProp(LM.baseGroup, "orderedInvoiceSku", "Кол-во заказано", orderedOrderInvoiceSku, 2, 3);
        orderedSimpleInvoiceSku = LM.addJProp(LM.baseGroup, "orderedSimpleInvoiceSku", "Кол-во заказано", LM.and1, orderedInvoiceSku, 1, 2, LM.is(simpleInvoice), 1);
        // здесь на самом деле есть ограничение, что supplierBox ссылается именно на invoice
        orderedSupplierBoxSku = LM.addJProp("orderedSupplierBoxSku", "Кол-во заказано", orderedInvoiceSku, boxInvoiceSupplierBox, 1, 2);


        // todo : переделать на PGProp, здесь надо derive'ить, иначе могут быть проблемы с расписыванием
        // если включаешь, то начинает тормозить изменение количества в заказах
//        quantityOrderInvoiceSku = addPGProp(LM.baseGroup, "quantityOrderInvoiceSku", true, 0, "Кол-во по заказу/инвойсу (расч.)",
//                orderedOrderInvoiceSku,
//                quantityDocumentSku, 2, 3);

        quantityOrderInvoiceSku = LM.addDProp(LM.baseGroup, "quantityOrderInvoiceSku", "Кол-во по заказу/инвойсу (расч.)", DoubleClass.instance,
                order, invoice, sku);

        invoicedOrderSku = LM.addSGProp(LM.baseGroup, "invoicedOrderSku", "Выставлено инвойсов", quantityOrderInvoiceSku, 1, 3);

        // todo : не работает на инвойсе/простом товаре
        quantityListArticle = LM.addDGProp(LM.baseGroup, "quantityListArticle", "Кол-во",
                1, false, // кол-во объектов для порядка и ascending/descending
                quantityListSku, 1, articleSku, 2,
                LM.addCUProp(LM.addCProp(DoubleClass.instance, Double.MAX_VALUE, list, articleSingle),
                        LM.addCProp(DoubleClass.instance, Double.MAX_VALUE, order, item),
                        LM.addJProp(LM.and1, orderedSimpleInvoiceSku, 1, 2, LM.is(item), 2), // если не артикул (простой), то пропорционально заказано
                        LM.addJProp(LM.and1, orderedSupplierBoxSku, 1, 2, LM.is(item), 2)), 1, 2, // ограничение (максимально-возможное число)
                2);

        quantityDocumentArticleCompositeColor = LM.addSGProp(LM.baseGroup, "quantityDocumentArticleCompositeColor", "Кол-во", quantityDocumentSku, 1, articleCompositeItem, 2, colorSupplierItem, 2);
        quantityDocumentArticleCompositeSize = LM.addSGProp(LM.baseGroup, "quantityDocumentArticleCompositeSize", "Кол-во", quantityDocumentSku, 1, articleCompositeItem, 2, sizeSupplierItem, 2);

        quantityDocumentArticleCompositeColorSize = LM.addDGProp(LM.baseGroup, "quantityDocumentArticleCompositeColorSize", "Кол-во",
                1, false,
                quantityDocumentSku, 1, articleCompositeItem, 2, colorSupplierItem, 2, sizeSupplierItem, 2,
                LM.addCProp(DoubleClass.instance, Double.MAX_VALUE, document, sku), 1, 2,
                2);
        quantityDocumentArticleCompositeColorSize.property.setFixedCharWidth(2);

        orderedOrderInvoiceArticle = LM.addJProp(LM.and1, quantityListArticle, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceArticle = LM.addSGProp(LM.baseGroup, "orderedInvoiceArticle", "Кол-во заказано", orderedOrderInvoiceArticle, 2, 3);
        // todo : сделать, чтобы работало автоматическое проставление
//        quantityListArticle.setDerivedForcedChange(orderedInvoiceArticle, 1, 2, numberListArticle, 1, 2);

        invoicedOrderArticle = LM.addSGProp(LM.baseGroup, "invoicedOrderArticle", "Выставлено инвойсов", invoicedOrderSku, 1, articleSku, 2);

        // цены
        priceDocumentArticle = LM.addDProp(LM.baseGroup, "priceDocumentArticle", "Цена", DoubleClass.instance, priceDocument, article);
        priceDataDocumentItem = LM.addDProp(LM.baseGroup, "priceDataDocumentItem", "Цена по товару", DoubleClass.instance, priceDocument, item);
        priceArticleDocumentSku = LM.addJProp(LM.baseGroup, "priceArticleDocumentItem", "Цена по артикулу", priceDocumentArticle, 1, articleSku, 2);
        priceDocumentSku = LM.addSUProp(LM.baseGroup, "priceDocumentSku", true, "Цена", Union.OVERRIDE, priceArticleDocumentSku, priceDataDocumentItem);

        priceRateDocumentSku = LM.addJProp(LM.baseGroup, "priceRateDocumentSku", true, "Цена (конверт.)", round2, LM.addJProp(LM.multiplyDouble2, priceDocumentSku, 1, 2, LM.addJProp(nearestRateExchange, typeExchangeSTX, currencyDocument, 1, 1), 1), 1, 2);

        RRPDocumentArticle = LM.addDProp(LM.baseGroup, "RRPDocumentArticle", "Рекомендованная цена", DoubleClass.instance, priceDocument, article);

        priceSupplierBoxSku = LM.addJProp(LM.baseGroup, "priceSupplierBoxSku", "Цена", priceDocumentSku, boxInvoiceSupplierBox, 1, 2);

        priceOrderInvoiceArticle = LM.addJProp(LM.and1, priceDocumentArticle, 1, 3, inOrderInvoice, 1, 2);
        priceOrderedInvoiceArticle = LM.addMGProp(LM.baseGroup, "priceOrderedInvoiceArticle", "Цена в заказе", priceOrderInvoiceArticle, 2, 3);
        // todo : не работает
        priceDocumentArticle.setDerivedForcedChange(priceOrderedInvoiceArticle, 1, 2, numberDocumentArticle, 1, 2);

        sumSupplierBoxSku = LM.addJProp(LM.baseGroup, "sumSupplierBoxSku", "Сумма", LM.multiplyDouble2, quantityListSku, 1, 2, priceSupplierBoxSku, 1, 2);
        sumDocumentSku = LM.addJProp(LM.baseGroup, "sumDocumentSku", "Сумма", LM.multiplyDouble2, quantityDocumentSku, 1, 2, priceDocumentSku, 1, 2);

        netWeightDocumentArticle = LM.addJProp(LM.baseGroup, "netWeightDocumentArticle", "Общий вес по артикулу", LM.multiplyDouble2, quantityDocumentArticle, 1, 2, netWeightArticle, 2);
        netWeightDocumentSku = LM.addJProp(LM.baseGroup, "netWeightDocumentSku", "Общий вес по sku", LM.multiplyDouble2, quantityDocumentSku, 1, 2, netWeightSku, 2);
        netWeightDocument = LM.addSGProp(LM.baseGroup, "netWeightDocument", "Общий вес", netWeightDocumentSku, 1);

        sumDocumentArticle = LM.addSGProp(LM.baseGroup, "sumDocumentArticle", "Сумма", sumDocumentSku, 1, articleSku, 2);
        sumDocument = LM.addSGProp(LM.baseGroup, "sumDocument", "Сумма документа", sumDocumentSku, 1);

        // route
        percentShipmentRoute = LM.addDProp(LM.baseGroup, "percentShipmentRoute", "Процент", DoubleClass.instance, shipment, route);

        percentShipmentRouteSku = LM.addJProp(LM.baseGroup, "percentShipmentRouteSku", "Процент", LM.and1, percentShipmentRoute, 1, 2, LM.is(sku), 3);

        // creation
        quantityCreationFreightBox = LM.addDProp(LM.baseGroup, "quantityCreationFreightBox", "Количество", IntegerClass.instance, creationFreightBox);
        routeCreationFreightBox = LM.addDProp(LM.idGroup, "routeCreationFreightBox", "Маршрут (ИД)", route, creationFreightBox);
        nameRouteCreationFreightBox = LM.addJProp(LM.baseGroup, "nameRouteCreationFreightBox", "Маршрут", LM.name, routeCreationFreightBox, 1);

        quantityCreationPallet = LM.addDProp(LM.baseGroup, "quantityCreationPallet", "Количество", IntegerClass.instance, creationPallet);
        routeCreationPallet = LM.addDProp(LM.idGroup, "routeCreationPallet", "Маршрут (ИД)", route, creationPallet);
        nameRouteCreationPallet = LM.addJProp(LM.baseGroup, "nameRouteCreationPallet", "Маршрут", LM.name, routeCreationPallet, 1);

        // паллеты
        creationPalletPallet = LM.addDProp(LM.idGroup, "creationPalletPallet", "Операция (ИД)", creationPallet, pallet);
        routeCreationPalletPallet = LM.addJProp(LM.idGroup, "routeCreationPalletPallet", true, "Маршрут (ИД)", routeCreationPallet, creationPalletPallet, 1);
        nameRouteCreationPalletPallet = LM.addJProp(LM.baseGroup, "nameRouteCreationPalletPallet", "Маршрут", LM.name, routeCreationPalletPallet, 1);

        freightPallet = LM.addDProp(LM.baseGroup, "freightPallet", "Фрахт (ИД)", freight, pallet);
        equalsPalletFreight = LM.addJProp(LM.baseGroup, "equalsPalletFreight", "Вкл.", LM.equals2, freightPallet, 1, 2);

        // инвойсы напрямую во фрахты
        freightDirectInvoice = LM.addDProp(LM.baseGroup, "freightDirectInvoice", "Фрахт (ИД)", freight, directInvoice);
        equalsDirectInvoiceFreight = LM.addJProp(LM.baseGroup, "equalsDirectInvoiceFreight", "Вкл.", LM.equals2, freightDirectInvoice, 1, 2);

        grossWeightDirectInvoice = LM.addDProp(LM.baseGroup, "grossWeightDirectInvoice", "Вес брутто", DoubleClass.instance, directInvoice);
        palletNumberDirectInvoice = LM.addDProp(LM.baseGroup, "palletNumberDirectInvoice", "Кол-во паллет", IntegerClass.instance, directInvoice);

        freightShippedDirectInvoice = LM.addJProp(LM.baseGroup, "freightShippedDirectInvoice", LM.is(freightShipped), freightDirectInvoice, 1);

        sumDirectInvoicedSku = LM.addSGProp(LM.baseGroup, "sumDirectInvoicedSku", "Сумма по инвойсам напрямую", LM.addJProp(LM.and(false, true), sumDocumentSku, 1, 2, LM.is(directInvoice), 1, freightShippedDirectInvoice, 1), 2);
        quantityDirectInvoicedSku = LM.addSGProp(LM.baseGroup, "quantityDirectInvoicedSku", "Кол-во по инвойсам напрямую", LM.addJProp(LM.and(false, true), quantityDocumentSku, 1, 2, LM.is(directInvoice), 1, freightShippedDirectInvoice, 1), 2);
        quantityDocumentBrandSupplier = LM.addSGProp(LM.baseGroup, "quantityDocumentBrandSupplier", "Кол-во по бренду в документе", LM.addJProp(LM.andNot1, quantityDocumentSku, 1, 2, freightShippedDirectInvoice, 1), 1, brandSupplierArticleSku, 2);

        // freight box
        creationFreightBoxFreightBox = LM.addDProp(LM.idGroup, "creationFreightBoxFreightBox", "Операция (ИД)", creationFreightBox, freightBox);

        palletFreightBox = LM.addDProp(LM.idGroup, "palletFreightBox", "Паллета (ИД)", pallet, freightBox);
        barcodePalletFreightBox = LM.addJProp(LM.baseGroup, "barcodePalletFreightBox", "Паллета (штрих-код)", LM.barcode, palletFreightBox, 1);

        routeCreationFreightBoxFreightBox = LM.addJProp(LM.idGroup, "routeCreationFreightBoxFreightBox", true, "Маршрут (ИД)", routeCreationFreightBox, creationFreightBoxFreightBox, 1);
        nameRouteCreationFreightBoxFreightBox = LM.addJProp(LM.baseGroup, "nameRouteCreationFreightBoxFreightBox", "Маршрут", LM.name, routeCreationFreightBoxFreightBox, 1);

        freightFreightBox = LM.addJProp(LM.idGroup, "freightFreightBox", "Фрахт короба транспортировки", freightPallet, palletFreightBox, 1);

        destinationFreightBox = LM.addDProp(LM.idGroup, "destinationFreightBox", "Пункт назначения (ИД)", destination, freightBox);
        nameDestinationFreightBox = LM.addJProp(LM.baseGroup, "nameDestinationFreightBox", "Пункт назначения", LM.name, destinationFreightBox, 1);

        // поставка на склад
        inInvoiceShipment = LM.addDProp(LM.baseGroup, "inInvoiceShipment", "Вкл", LogicalClass.instance, invoice, shipment);

        inSupplierBoxShipment = LM.addJProp(LM.baseGroup, "inSupplierBoxShipment", "Вкл", inInvoiceShipment, boxInvoiceSupplierBox, 1, 2);

        invoicedShipmentSku = LM.addSGProp(LM.baseGroup, "invoicedShipmentSku", true, "Ожид. (пост.)",
                LM.addJProp(LM.and1, quantityDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        priceShipmentSku = LM.addMGProp(LM.baseGroup, "priceShipmentSku", true, "Цена (пост.)",
                LM.addJProp(LM.and1, priceDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        invoicedShipment = LM.addSGProp(LM.baseGroup, "invoicedShipment", true, "Всего ожидается (пост.)", invoicedShipmentSku, 1);

        //sku shipment detail
        skuShipmentDetail = LM.addDProp(LM.idGroup, "skuShipmentDetail", "SKU (ИД)", sku, shipmentDetail);
        barcodeSkuShipmentDetail = LM.addJProp(LM.baseGroup, "barcodeSkuShipmentDetail", "Штрих-код SKU", LM.barcode, skuShipmentDetail, 1);

        articleShipmentDetail = LM.addJProp(LM.idGroup, "articleShipmentDetail", "Артикул (ИД)", articleSku, skuShipmentDetail, 1);
        sidArticleShipmentDetail = LM.addJProp(LM.baseGroup, "sidArticleShipmentDetail", "Артикул", sidArticle, articleShipmentDetail, 1);

        colorSupplierItemShipmentDetail = LM.addJProp(LM.idGroup, "colorSupplierItemShipmentDetail", "Цвет поставщика (ИД)", colorSupplierItem, skuShipmentDetail, 1);
        sidColorSupplierItemShipmentDetail = LM.addJProp(itemAttributeGroup, "sidColorSupplierItemShipmentDetail", "Код цвета", sidColorSupplier, colorSupplierItemShipmentDetail, 1);
        nameColorSupplierItemShipmentDetail = LM.addJProp(itemAttributeGroup, "nameColorSupplierItemShipmentDetail", "Цвет поставщика", LM.name, colorSupplierItemShipmentDetail, 1);

        sizeSupplierItemShipmentDetail = LM.addJProp(LM.idGroup, "sizeSupplierItemShipmentDetail", "Размер поставщика (ИД)", sizeSupplierItem, skuShipmentDetail, 1);
        sidSizeSupplierItemShipmentDetail = LM.addJProp(itemAttributeGroup, "sidSizeSupplierItemShipmentDetail", "Размер поставщика", sidSizeSupplier, sizeSupplierItemShipmentDetail, 1);

        nameBrandSupplierArticleSkuShipmentDetail = LM.addJProp(supplierAttributeGroup, true, "nameBrandSupplierArticleSkuShipmentDetail", "Бренд", nameBrandSupplierArticleSku, skuShipmentDetail, 1);
        originalNameArticleSkuShipmentDetail = LM.addJProp(supplierAttributeGroup, true, "originalNameArticleSkuShipmentDetail", "Наименование (ориг.)", originalNameArticleSku, skuShipmentDetail, 1);

        categoryArticleSkuShipmentDetail = LM.addJProp(LM.idGroup, true, "categoryArticleSkuShipmentDetail", "Номенклатурная группа товара (ИД)", categoryArticleSku, skuShipmentDetail, 1);
        nameOriginCategoryArticleSkuShipmentDetail = LM.addJProp(intraAttributeGroup, "nameOriginCategoryArticleSkuShipmentDetail", "Номенклатурная группа товара", nameOrigin, categoryArticleSkuShipmentDetail, 1);

        customCategoryOriginArticleSkuShipmentDetail = LM.addJProp(LM.idGroup, true, "customCategoryOriginArticleSkuShipmentDetail", "ТН ВЭД (ИД)", customCategoryOriginArticleSku, skuShipmentDetail, 1);
        sidCustomCategoryOriginArticleSkuShipmentDetail = LM.addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticleSkuShipmentDetail", "Код ТН ВЭД", sidCustomCategoryOrigin, customCategoryOriginArticleSkuShipmentDetail, 1);

        netWeightArticleSkuShipmentDetail = LM.addJProp(supplierAttributeGroup, true, "netWeightArticleSkuShipmentDetail", "Вес нетто (ориг.)", netWeightArticleSku, skuShipmentDetail, 1);
        netWeightSkuShipmentDetail = LM.addJProp(intraAttributeGroup, true, "netWeightSkuShipmentDetail", "Вес нетто", netWeightSku, skuShipmentDetail, 1);

        countryOfOriginArticleSkuShipmentDetail = LM.addJProp(LM.idGroup, true, "countryOfOriginArticleSkuShipmentDetail", "Страна происхождения (ориг.) (ИД)", countryOfOriginArticleSku, skuShipmentDetail, 1);
        nameCountryOfOriginArticleSkuShipmentDetail = LM.addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSkuShipmentDetail", "Страна происхождения", nameOriginCountry, countryOfOriginArticleSkuShipmentDetail, 1);

        countryOfOriginSkuShipmentDetail = LM.addJProp(LM.idGroup, true, "countryOfOriginSkuShipmentDetail", "Страна происхождения (ИД)", countryOfOriginSku, skuShipmentDetail, 1);
        nameCountryOfOriginSkuShipmentDetail = LM.addJProp(intraAttributeGroup, "nameCountryOfOriginSkuShipmentDetail", "Страна происхождения", nameOriginCountry, countryOfOriginSkuShipmentDetail, 1);

        mainCompositionOriginArticleSkuShipmentDetail = LM.addJProp(supplierAttributeGroup, true, "mainCompositionOriginArticleSkuShipmentDetail", "Состав", mainCompositionOriginArticleSku, skuShipmentDetail, 1);
        mainCompositionOriginSkuShipmentDetail = LM.addJProp(intraAttributeGroup, true, "mainCompositionOriginSkuShipmentDetail", "Состав", mainCompositionOriginSku, skuShipmentDetail, 1);

        additionalCompositionOriginSkuShipmentDetail = LM.addJProp(intraAttributeGroup, true, "additionalCompositionOriginSkuShipmentDetail", "Дополнительный состав", additionalCompositionOriginSku, skuShipmentDetail, 1);

        unitOfMeasureArticleSkuShipmentDetail = LM.addJProp(LM.idGroup, true, "unitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара (ИД)", unitOfMeasureArticleSku, skuShipmentDetail, 1);
        nameOriginUnitOfMeasureArticleSkuShipmentDetail = LM.addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара", nameOrigin, unitOfMeasureArticleSkuShipmentDetail, 1);

        // stock shipment detail
        stockShipmentDetail = LM.addDProp(LM.idGroup, "stockShipmentDetail", "Место хранения (ИД)", stock, shipmentDetail);
        barcodeStockShipmentDetail = LM.addJProp(LM.baseGroup, "barcodeStockShipmentDetail", "Штрих-код МХ", LM.barcode, stockShipmentDetail, 1);

        routeFreightBoxShipmentDetail = LM.addJProp(LM.idGroup, "routeFreightBoxShipmentDetail", "Маршрут (ИД)", routeCreationFreightBoxFreightBox, stockShipmentDetail, 1);
        nameRouteFreightBoxShipmentDetail = LM.addJProp(LM.baseGroup, "nameRouteFreightBoxShipmentDetail", "Маршрут", LM.name, routeFreightBoxShipmentDetail, 1);

        boxShipmentBoxShipmentDetail = LM.addDProp(LM.idGroup, "boxShipmentBoxShipmentDetail", "Поставка (ИД)", boxShipment, boxShipmentDetail);
        simpleShipmentSimpleShipmentDetail = LM.addDProp(LM.idGroup, "simpleShipmentSimpleShipmentDetail", "Поставка (ИД)", simpleShipment, simpleShipmentDetail);
        shipmentShipmentDetail = LM.addCUProp(LM.idGroup, "shipmentShipmentDetail", "Поставка (ИД)", boxShipmentBoxShipmentDetail, simpleShipmentSimpleShipmentDetail);
        sidShipmentShipmentDetail = LM.addJProp(LM.baseGroup, "sidShipmentShipmentDetail", "Поставка", sidDocument, shipmentShipmentDetail, 1);

        // supplier box shipmentDetail
        supplierBoxShipmentDetail = LM.addDProp(LM.idGroup, "supplierBoxShipmentDetail", "Короб поставщика (ИД)", supplierBox, boxShipmentDetail);
        sidSupplierBoxShipmentDetail = LM.addJProp(LM.baseGroup, "sidSupplierBoxShipmentDetail", "Номер короба поставщика", sidSupplierBox, supplierBoxShipmentDetail, 1);
        barcodeSupplierBoxShipmentDetail = LM.addJProp(LM.baseGroup, "barcodeSupplierBoxShipmentDetail", "Штрих-код короба поставщика", LM.barcode, supplierBoxShipmentDetail, 1);

        quantityShipmentDetail = LM.addDProp(LM.baseGroup, "quantityShipmentDetail", "Кол-во", DoubleClass.instance, shipmentDetail);

        userShipmentDetail = LM.addDCProp(LM.idGroup, "userShipmentDetail", "Пользователь (ИД)", LM.currentUser, true, LM.is(shipmentDetail), 1);
        nameUserShipmentDetail = LM.addJProp(LM.baseGroup, "nameUserShipmentDetail", "Пользователь", LM.name, userShipmentDetail, 1);

        timeShipmentDetail = LM.addTCProp(Time.DATETIME, "timeShipmentDetail", true, "Время ввода", quantityShipmentDetail);

        addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode = LM.addJProp(true, "Добавить строку поставки",
                LM.addAAProp(boxShipmentDetail, boxShipmentBoxShipmentDetail, supplierBoxShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail),
                1, 2, 3, skuBarcodeObject, 4, LM.addCProp(DoubleClass.instance, 1));

        addSimpleShipmentSimpleShipmentDetailStockBarcode = LM.addJProp(true, "Добавить строку поставки",
                LM.addAAProp(simpleShipmentDetail, simpleShipmentSimpleShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail),
                1, 2, LM.barcodeToObject, 3, LM.addCProp(DoubleClass.instance, 1));

        quantitySupplierBoxBoxShipmentStockSku = LM.addSGProp(LM.baseGroup, "quantitySupplierBoxBoxShipmentStockSku", "Кол-во оприход.", quantityShipmentDetail,
                supplierBoxShipmentDetail, 1, boxShipmentBoxShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        quantitySupplierBoxBoxShipmentSku = LM.addSGProp(LM.baseGroup, "quantitySupplierBoxBoxShipmentSku", "Кол-во оприход.", quantitySupplierBoxBoxShipmentStockSku,
                1, 2, 4);

        quantitySupplierBoxSku = LM.addSGProp(LM.baseGroup, "quantitySupplierBoxSku", "Кол-во оприход.", quantitySupplierBoxBoxShipmentStockSku, 1, 4);

        diffListSupplierBoxSku =LM.addJProp(LM.diff2, quantityListSku, 1, 2, quantitySupplierBoxSku, 1, 2);

        quantitySimpleShipmentStockSku = LM.addSGProp(LM.baseGroup, "quantitySimpleShipmentStockSku", "Кол-во оприход.", quantityShipmentDetail,
                simpleShipmentSimpleShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        quantityShipDimensionShipmentStockSku = LM.addCUProp(LM.baseGroup, "quantityShipDimensionShipmentStockSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                LM.addJProp(LM.and1, quantitySimpleShipmentStockSku, 2, 3, 4, LM.equals2, 1, 2));

        quantityBoxInvoiceBoxShipmentStockSku = LM.addSGProp(LM.baseGroup, "quantityBoxInvoiceBoxShipmentStockSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                boxInvoiceSupplierBox, 1, 2, 3, 4);

        invoicedSimpleInvoiceSimpleShipmentStockSku = LM.addJProp(LM.and(false, false, false, false), quantityDocumentSku, 1, 4, inInvoiceShipment, 1, 2, LM.is(simpleInvoice), 1, LM.is(simpleShipment), 2, LM.is(stock), 3);

        quantitySimpleInvoiceSimpleShipmentStockSku = LM.addPGProp(LM.baseGroup, "quantitySimpleInvoiceSimpleShipmentStockSku", true, 0, true, "Кол-во оприход.",
                invoicedSimpleInvoiceSimpleShipmentStockSku,
                quantitySimpleShipmentStockSku, 2, 3, 4);

        quantityInvoiceShipmentStockSku = LM.addCUProp(LM.baseGroup, "quantityInvoiceShipmentStockSku", "Кол-во оприход.",
                quantityBoxInvoiceBoxShipmentStockSku, quantitySimpleInvoiceSimpleShipmentStockSku);

        quantityInvoiceStockSku = LM.addSGProp(LM.baseGroup, "quantityInvoiceStockSku", true, "Кол-во оприход.", quantityInvoiceShipmentStockSku, 1, 3, 4);

        quantityInvoiceSku = LM.addSGProp(LM.baseGroup, "quantityInvoiceSku", true, "Кол-во оприход.", quantityInvoiceStockSku, 1, 3);

        diffDocumentInvoiceSku = LM.addJProp(LM.equals2, quantityDocumentSku, 1, 2, quantityInvoiceSku, 1, 2);

        priceInInvoiceStockSku = LM.addJProp(LM.baseGroup, "priceInInvoiceStockSku", false, "Цена входная", LM.and1,
                priceRateDocumentSku, 1, 3, quantityInvoiceStockSku, 1, 2, 3);

        quantityShipDimensionStock = LM.addSGProp(LM.baseGroup, "quantityShipDimensionStock", "Всего оприход.", quantityShipDimensionShipmentStockSku, 1, 3);

        quantityShipDimensionShipmentSku = LM.addSGProp(LM.baseGroup, "quantityShipDimensionShipmentSku", "Оприход. (короб)", quantityShipDimensionShipmentStockSku, 1, 2, 4);

        zeroQuantityListSku = LM.addSUProp(LM.baseGroup, "zeroQuantityListSku", "кол-во", Union.OVERRIDE, LM.addCProp(DoubleClass.instance, 0, list, sku), quantityListSku);
        zeroQuantityShipDimensionShipmentSku = LM.addSUProp(LM.baseGroup, "zeroQuantityShipDimensionShipmentSku", "кол-во", Union.OVERRIDE, LM.addCProp(DoubleClass.instance, 0, shipDimension, shipment, sku), quantityShipDimensionShipmentSku);

        diffListShipSku = LM.addJProp(LM.diff2, zeroQuantityListSku, 1, 3, zeroQuantityShipDimensionShipmentSku, 1, 2, 3);

        quantityShipmentStockSku = LM.addSGProp(LM.baseGroup, "quantityShipmentStockSku", true, "Кол-во оприход.", quantityShipDimensionShipmentStockSku, 2, 3, 4);

        quantityShipmentStock = LM.addSGProp(LM.baseGroup, "quantityShipmentStock", "Всего оприход.", quantityShipmentStockSku, 1, 2);

        quantityShipmentSku = LM.addSGProp(LM.baseGroup, "quantityShipmentSku", "Оприход. (пост.)", quantityShipmentStockSku, 1, 3);

        zeroQuantityShipmentSku = LM.addSUProp(LM.baseGroup, "zeroQuantityShipmentSku", "кол-во", Union.OVERRIDE, LM.addCProp(DoubleClass.instance, 0, shipment, sku), quantityShipmentSku);
        zeroInvoicedShipmentSku = LM.addSUProp(LM.baseGroup, "zeroInvoicedShipmentSku", "кол-во", Union.OVERRIDE, LM.addCProp(DoubleClass.instance, 0, shipment, sku), invoicedShipmentSku);
        diffShipmentSku = LM.addJProp(LM.diff2, zeroQuantityShipmentSku, 1, 2, zeroInvoicedShipmentSku, 1, 2);

        quantityStockSku = LM.addSGProp(LM.baseGroup, "quantityStockSku", true, true, "Оприход. (МХ)", quantityShipmentStockSku, 2, 3);

        quantityStockArticle = LM.addSGProp(LM.baseGroup, "quantityStockArticle", "Кол-во по артикулу", quantityStockSku, 1, articleSku, 2);

        freightShippedFreightBox = LM.addJProp(LM.baseGroup, "freightShippedFreightBox", LM.is(freightShipped), freightFreightBox, 1);

        sumInInvoiceStockSku = LM.addJProp(LM.baseGroup, "sumInInvoiceStockSku", "Сумма в коробе", LM.multiplyDouble2, LM.addJProp(LM.andNot1, quantityInvoiceStockSku, 1, 2, 3, freightShippedFreightBox, 2), 1, 2, 3, priceInInvoiceStockSku, 1, 2, 3);

        sumStockedSku = LM.addSGProp(LM.baseGroup, "sumStockedSku", "Сумма на приемке", sumInInvoiceStockSku, 3);
        quantityStockedSku = LM.addSGProp(LM.baseGroup, "quantityStockedSku", "Кол-во на приемке", LM.addJProp(LM.andNot1, quantityStockSku, 1, 2, freightShippedFreightBox, 1), 2);

        quantitySku = LM.addSUProp(LM.baseGroup, "quantitySku", "Кол-во", Union.SUM, quantityStockedSku, quantityDirectInvoicedSku);
        sumSku = LM.addSUProp(LM.baseGroup, "sumSku", "Сумма", Union.SUM, sumStockedSku, sumDirectInvoicedSku);

        quantityStockBrandSupplier = LM.addSGProp(LM.baseGroup, "quantityStockBrandSupplier", "Кол-во по бренду",
                LM.addJProp(LM.andNot1, quantityStockArticle, 1, 2, freightShippedFreightBox, 1), 1, brandSupplierArticle, 2);

        quantityPalletSku = LM.addSGProp(LM.baseGroup, "quantityPalletSku", "Оприход. (пал.)", quantityStockSku, palletFreightBox, 1, 2);

        quantityPalletBrandSupplier = LM.addSGProp(LM.baseGroup, "quantityPalletBrandSupplier", "Кол-во по бренду", quantityStockBrandSupplier, palletFreightBox, 1, 2);

        quantityShipmentPallet = LM.addSGProp(LM.baseGroup, "quantityShipmentPallet", "Всего оприход. (паллета)", quantityShipmentStock, 1, palletFreightBox, 2);

        quantityShipmentFreight = LM.addSGProp(LM.baseGroup, "quantityShipmentFreight", "Всего оприход. (фрахт)", quantityShipmentPallet, 1, freightPallet, 2);

        quantityShipmentArticle = LM.addSGProp(LM.baseGroup, "quantityShipmentArticle", "Всего оприход. (артикул)", quantityShipmentSku, 1, articleSku, 2);
        quantityShipmentArticleSize = LM.addSGProp(LM.baseGroup, "quantityShipmentArticleSize", "Всего оприход. (артикул-размер)", quantityShipmentSku, 1, articleSku, 2, sizeSupplierItem, 2);
        quantityShipmentArticleColor = LM.addSGProp(LM.baseGroup, "quantityShipmentArticleColor", "Всего оприход. (артикул-цвет)", quantityShipmentSku, 1, articleSku, 2, colorSupplierItem, 2);

        oneShipmentArticle = LM.addJProp(LM.baseGroup, "oneShipmentArticle", "Первый артикул", LM.equals2, quantityShipmentArticle, 1, 2, LM.addCProp(DoubleClass.instance, 1));
        oneShipmentArticleColor = LM.addJProp(LM.baseGroup, "oneShipmentArticleColor", "Первый артикул-цвет", LM.equals2, quantityShipmentArticleColor, 1, 2, 3, LM.addCProp(DoubleClass.instance, 1));
        oneShipmentArticleSize = LM.addJProp(LM.baseGroup, "oneShipmentArticleSize", "Первый артикул-размер", LM.equals2, quantityShipmentArticleSize, 1, 2, 3, LM.addCProp(DoubleClass.instance, 1));

        oneShipmentArticleSku = LM.addJProp(LM.baseGroup, "oneShipmentArticleSku", "Первый артикул", oneShipmentArticle, 1, articleSku, 2);
        oneShipmentArticleColorSku = LM.addJProp(LM.baseGroup, "oneShipmentArticleColorSku", "Первый артикул-цвет", oneShipmentArticleColor, 1, articleSku, 2, colorSupplierItem, 2);
        oneShipmentArticleSizeSku = LM.addJProp(LM.baseGroup, "oneShipmentArticleSizeSku", "Первый артикул-размер", oneShipmentArticleSize, 1, articleSku, 2, sizeSupplierItem, 2);

        oneShipmentSku = LM.addJProp(LM.baseGroup, "oneShipmentSku", "Первый SKU", LM.equals2, quantityShipmentSku, 1, 2, LM.addCProp(DoubleClass.instance, 1));

        oneArticleSkuShipmentDetail = LM.addJProp(LM.baseGroup, "oneArticleSkuShipmentDetail", "Первый артикул", oneShipmentArticleSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);
        oneArticleColorShipmentDetail = LM.addJProp(LM.baseGroup, "oneArticleColorShipmentDetail", "Первый артикул-цвет", oneShipmentArticleColorSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);
        oneArticleSizeShipmentDetail = LM.addJProp(LM.baseGroup, "oneArticleSizeShipmentDetail", "Первый артикул-размер", oneShipmentArticleSizeSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);
        oneSkuShipmentDetail = LM.addJProp(LM.baseGroup, "oneSkuShipmentDetail", "Первый SKU", oneShipmentSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);

        // Transfer
        stockFromTransfer = LM.addDProp(LM.idGroup, "stockFromTransfer", "Место хранения (с) (ИД)", stock, transfer);
        barcodeStockFromTransfer = LM.addJProp(LM.baseGroup, "barcodeStockFromTransfer", "Штрих-код МХ (с)", LM.barcode, stockFromTransfer, 1);

        stockToTransfer = LM.addDProp(LM.idGroup, "stockToTransfer", "Место хранения (на) (ИД)", stock, transfer);
        barcodeStockToTransfer = LM.addJProp(LM.baseGroup, "barcodeStockToTransfer", "Штрих-код МХ (на)", LM.barcode, stockToTransfer, 1);

        quantityTransferSku = LM.addDProp(LM.baseGroup, "quantityTransferStockSku", "Кол-во перемещения", DoubleClass.instance, transfer, sku);

        outcomeTransferStockSku = LM.addSGProp(LM.baseGroup, "outcomeTransferStockSku", "Расход по ВП", quantityTransferSku, stockFromTransfer, 1, 2);
        incomeTransferStockSku = LM.addSGProp(LM.baseGroup, "incomeTransferStockSku", "Приход по ВП", quantityTransferSku, stockToTransfer, 1, 2);

        incomeStockSku = LM.addSUProp(LM.baseGroup, "incomeStockSku", "Приход", Union.SUM, quantityStockSku, incomeTransferStockSku);
        outcomeStockSku = outcomeTransferStockSku;

        balanceStockSku = LM.addDUProp(LM.baseGroup, "balanceStockSku", "Тек. остаток", incomeStockSku, outcomeStockSku);

        balanceStockFromTransferSku = LM.addJProp(LM.baseGroup, "balanceStockFromTransferSku", "Тек. остаток на МХ (с)", balanceStockSku, stockFromTransfer, 1, 2);
        balanceStockToTransferSku = LM.addJProp(LM.baseGroup, "balanceStockToTransferSku", "Тек. остаток на МХ (на)", balanceStockSku, stockToTransfer, 1, 2);

        // Расписывание по route'ам количеств в инвойсе
        quantityShipmentRouteSku = LM.addSGProp(LM.baseGroup, "quantityShipmentRouteSku", "Кол-во оприход.", quantityShipmentStockSku, 1, routeCreationFreightBoxFreightBox, 2, 3);
        invoicedShipmentRouteSku = LM.addPGProp(LM.baseGroup, "invoicedShipmentRouteSku", false, 0, true, "Кол-во ожид.",
                percentShipmentRouteSku,
                invoicedShipmentSku, 1, 3);

        zeroQuantityShipmentRouteSku = LM.addSUProp(LM.baseGroup, "zeroQuantityShipmentRouteSku", "кол-во", Union.OVERRIDE, LM.addCProp(DoubleClass.instance, 0, shipment, route, sku), quantityShipmentRouteSku);
        zeroInvoicedShipmentRouteSku = LM.addSUProp(LM.baseGroup, "zeroInvoicedShipmentRouteSku", "кол-во", Union.OVERRIDE, LM.addCProp(DoubleClass.instance, 0, shipment, route, sku), invoicedShipmentRouteSku);

        diffShipmentRouteSku = LM.addJProp(LM.greater2, zeroQuantityShipmentRouteSku, 1, 2, 3, zeroInvoicedShipmentRouteSku, 1, 2, 3);

        sumShipmentRouteSku = LM.addJProp(LM.baseGroup, "sumShipmentRouteSku", "Сумма", LM.multiplyDouble2, invoicedShipmentRouteSku, 1, 2, 3, priceShipmentSku, 1, 3);
        sumShipmentRoute = LM.addSGProp(LM.baseGroup, "sumShipmentRoute", "Сумма (ожид.)", sumShipmentRouteSku, 1, 2);
        sumShipment = LM.addSGProp(LM.baseGroup, "sumShipment", "Сумма (ожид.)", sumShipmentRoute, 1);

        invoicedShipmentRoute = LM.addSGProp(LM.baseGroup, "invoicedShipmentRoute", "Кол-во", invoicedShipmentRouteSku, 1, 2);

//        notFilledShipmentRouteSku = LM.addJProp(LM.baseGroup, "notFilledShipmentRouteSku", "Не заполнен", LM.greater2, invoicedShipmentRouteSku, 1, 2, 3,
//                addSUProp(Union.OVERRIDE, LM.addCProp(DoubleClass.instance, 0, shipment, route, sku), quantityShipmentRouteSku), 1, 2, 3);
//
//        routeToFillShipmentSku = LM.addMGProp(LM.idGroup, "routeToFillShipmentSku", "Маршрут (ИД)",
//                LM.addJProp(LM.and1, LM.object(route), 2, notFilledShipmentRouteSku, 1, 2, 3), 1, 3);
//
//        LP routeToFillShipmentBarcode = LM.addJProp(routeToFillShipmentSku, 1, barcodeToObject, 2);
//        seekRouteToFillShipmentBarcode = LM.addJProp(actionGroup, true, "seekRouteToFillShipmentSku", "Поиск маршрута", addSAProp(null),
//                routeToFillShipmentBarcode, 1, 2);

        LM.addConstraint(LM.addJProp("Магазин короба для транспортировки должен совпадать с магазином короба поставщика", LM.and1,
                LM.addJProp(LM.diff2, destinationSupplierBox, 1, destinationFreightBox, 2), 1, 2,
                quantityShipDimensionStock, 1, 2), true);

        // Freight
        tonnageFreightType = LM.addDProp(LM.baseGroup, "tonnageFreightType", "Тоннаж (кг)", DoubleClass.instance, freightType);
        palletCountFreightType = LM.addDProp(LM.baseGroup, "palletCountFreightType", "Кол-во паллет", IntegerClass.instance, freightType);
        volumeFreightType = LM.addDProp(LM.baseGroup, "volumeFreightType", "Объем", DoubleClass.instance, freightType);

        freightTypeFreight = LM.addDProp(LM.idGroup, "freightTypeFreight", "Тип машины (ИД)", freightType, freight);
        nameFreightTypeFreight = LM.addJProp(LM.baseGroup, "nameFreightTypeFreight", "Тип машины", LM.name, freightTypeFreight, 1);

        tonnageFreight = LM.addJProp(LM.baseGroup, "tonnageFreight", "Тоннаж (кг)", tonnageFreightType, freightTypeFreight, 1);
        palletCountFreight = LM.addJProp(LM.baseGroup, "palletCountFreight", "Кол-во паллет", palletCountFreightType, freightTypeFreight, 1);
        volumeFreight = LM.addJProp(LM.baseGroup, "volumeFreight", "Объём", volumeFreightType, freightTypeFreight, 1);

        currencyFreight = LM.addDProp(LM.idGroup, "currencyFreight", "Валюта (ИД)", currency, freight);
        nameCurrencyFreight = LM.addJProp(LM.baseGroup, "nameCurrencyFreight", "Валюта", LM.name, currencyFreight, 1);
        sumFreightFreight = LM.addDProp(LM.baseGroup, "sumFreightFreight", "Стоимость", DoubleClass.instance, freight);

        routeFreight = LM.addDProp(LM.idGroup, "routeFreight", "Маршрут (ИД)", route, freight);
        nameRouteFreight = LM.addJProp(LM.baseGroup, "nameRouteFreight", "Маршрут", LM.name, routeFreight, 1);

        exporterFreight = LM.addDProp(LM.idGroup, "exporterFreight", "Экспортер (ИД)", exporter, freight);
        nameOriginExporterFreight = LM.addJProp(LM.baseGroup, "nameOriginExporterFreight", "Экспортер", nameOrigin, exporterFreight, 1);
        nameExporterFreight = LM.addJProp(LM.baseGroup, "nameExporterFreight", "Экспортер", LM.name, exporterFreight, 1);
        addressOriginExporterFreight = LM.addJProp(LM.baseGroup, "addressOriginExporterFreight", "Адрес", addressOriginSubject, exporterFreight, 1);
        addressExporterFreight = LM.addJProp(LM.baseGroup, "addressExporterFreight", "Адрес", addressSubject, exporterFreight, 1);

        quantityPalletShipmentBetweenDate = LM.addSGProp(LM.baseGroup, "quantityPalletShipmentBetweenDate", "Кол-во паллет по поставкам за интервал",
                LM.addJProp(LM.and1, quantityPalletShipment, 1, LM.addJProp(LM.between, LM.date, 1, LM.object(DateClass.instance), 2, LM.object(DateClass.instance), 3), 1, 2, 3), 2, 3);
        quantityPalletFreightBetweenDate = LM.addSGProp(LM.baseGroup, "quantityPalletFreightBetweenDate", "Кол-во паллет по фрахтам за интервал",
                LM.addJProp(LM.and1, palletCountFreight, 1, LM.addJProp(LM.between, LM.date, 1, LM.object(DateClass.instance), 2, LM.object(DateClass.instance), 3), 1, 2, 3), 2, 3);

        freightBoxNumberPallet = LM.addSGProp(LM.baseGroup, "freightBoxNumberPallet", "Кол-во коробов", LM.addCProp(IntegerClass.instance, 1, freightBox), palletFreightBox, 1);

        LM.addConstraint(LM.addJProp("Маршрут паллеты должен совпадать с маршрутом фрахта", LM.diff2,
                routeCreationPalletPallet, 1, LM.addJProp(routeFreight, freightPallet, 1), 1), true);

        palletNumberFreight = LM.addSUProp(LM.baseGroup, "palletNumberFreight", "Кол-во присоединённых паллет", Union.SUM,
                LM.addSGProp(LM.addCProp(IntegerClass.instance, 1, pallet), freightPallet, 1),
                LM.addSGProp(palletNumberDirectInvoice, freightDirectInvoice, 1));

        diffPalletFreight = LM.addJProp(LM.greater2, palletNumberFreight, 1, palletCountFreight, 1);
        // freight для supplierBox
        freightSupplierBox = LM.addJProp(LM.baseGroup, "freightSupplierBox", "Фрахт (ИД)", freightDirectInvoice, boxInvoiceSupplierBox, 1);
        freightFreightUnit = LM.addCUProp(LM.idGroup, "freightFreightUnit", "Фрахт (ИД)", freightFreightBox, freightSupplierBox);

        importerSupplierBox = LM.addJProp(LM.baseGroup, "importerSupplierBox", "Импортер (ИД)", importerInvoice, boxInvoiceSupplierBox, 1);

        // Кол-во для импортеров
        // здесь не соблюдается policy, что входы совпадают с именем
        quantityInvoiceFreightUnitSku = LM.addCUProp(LM.baseGroup, "quantityInvoiceFreightUnitSku", "Кол-во",
                quantityInvoiceStockSku,
                LM.addJProp(LM.and1, quantityListSku, 2, 3, LM.addJProp(LM.equals2, 1, boxInvoiceSupplierBox, 2), 1, 2));

        priceInInvoiceFreightUnitSku = LM.addCUProp(LM.baseGroup, "priceInInvoiceFreightUnitSku", "Цена входная",
                priceInInvoiceStockSku,
                LM.addJProp(LM.and1, priceRateDocumentSku, 1, 3, LM.addJProp(LM.equals2, 1, boxInvoiceSupplierBox, 2), 1, 2));

        quantityImporterStockSku = LM.addSGProp(LM.baseGroup, "quantityImporterStockSku", "Кол-во", quantityInvoiceStockSku, importerInvoice, 1, 2, 3);
        quantityImporterStockArticle = LM.addSGProp(LM.baseGroup, "quantityImporterStockArticle", "Кол-во", quantityImporterStockSku, 1, 2, articleSku, 3);
        quantityImporterStock = LM.addSGProp(LM.baseGroup, "quantityImporterStock", "Кол-во", quantityImporterStockSku, 1, 2);

        quantityProxyImporterFreightSku = LM.addSGProp(LM.baseGroup, "quantityProxyImporterFreightSku", true, true, "Кол-во (из приёмки)", quantityImporterStockSku, 1, freightFreightUnit, 2, 3);
        quantityDirectImporterFreightSku = LM.addSGProp(LM.baseGroup, "quantityDirectImporterFreightSku", true, true, "Кол-во (напрямую)", quantityListSku, importerSupplierBox, 1, freightFreightUnit, 1, 2);
        quantityImporterFreightSku = LM.addSUProp(LM.baseGroup, "quantityImporterFreightSku", "Кол-во", Union.SUM, quantityProxyImporterFreightSku, quantityDirectImporterFreightSku);

        quantityFreightArticle = LM.addSGProp(LM.baseGroup, "quantityFreightArticle", "Кол-во", quantityImporterFreightSku, 2, articleSku, 3);

        quantityFreightSku = LM.addSGProp(LM.baseGroup, "quantityFreightSku", true, true, "Кол-во", quantityImporterFreightSku, 2, 3);
        quantityDirectFreightSku = LM.addSGProp(LM.baseGroup, "quantityDirectFreightSku", true, true, "Кол-во (напрямую)", quantityDirectImporterFreightSku, 2, 3);

        customCategory10FreightSku = LM.addDProp(LM.idGroup, "customCategory10FreightSku", "ТН ВЭД (ИД)", customCategory10, freight, sku);
        customCategory10FreightSku.setDerivedForcedChange(LM.addJProp(LM.and1, customCategory10Sku, 2, quantityFreightSku, 1, 2), 1, 2, LM.is(freightChanged), 1);
        sidCustomCategory10FreightSku = LM.addJProp(LM.baseGroup, "sidCustomCategory10FreightSku", "ТН ВЭД", sidCustomCategory10, customCategory10FreightSku, 1, 2);

        LM.addConstraint(LM.addJProp("Для SKU должен быть задан ТН ВЭД", LM.and(true, false), LM.is(freightChanged), 1, customCategory10FreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        customCategory6FreightSku = LM.addJProp(LM.idGroup, "customCategory6FreightSku", "ТН ВЭД", customCategory6CustomCategory10, customCategory10FreightSku, 1, 2);

        quantityImporterFreightCustomCategory6 = LM.addSGProp(LM.baseGroup, "quantityImporterFreightCustomCategory6", "Кол-во", quantityImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);

        mainCompositionOriginFreightSku = LM.addDProp(LM.baseGroup, "mainCompositionOriginFreightSku", "Состав", COMPOSITION_CLASS, freight, sku);
        mainCompositionOriginFreightSku.setDerivedForcedChange(LM.addJProp(LM.and1, mainCompositionOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, LM.is(freightChanged), 1);

        additionalCompositionOriginFreightSku = LM.addDProp(LM.baseGroup, "additionalCompositionOriginFreightSku", "Доп. состав", COMPOSITION_CLASS, freight, sku);
        additionalCompositionOriginFreightSku.setDerivedForcedChange(LM.addJProp(LM.and1, additionalCompositionOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, LM.is(freightChanged), 1);

        translationMainCompositionSku = LM.addJProp(LM.actionGroup, true, "translationMainCompositionSku", "Перевод состава", LM.addTAProp(mainCompositionOriginSku, mainCompositionSku), dictionaryComposition, 1);
        translationAdditionalCompositionSku = LM.addJProp(LM.actionGroup, true, "translationAdditionalCompositionSku", "Перевод доп. состава", LM.addTAProp(additionalCompositionOriginSku, additionalCompositionSku), dictionaryComposition, 1);

        mainCompositionFreightSku = LM.addDProp(LM.baseGroup, "mainCompositionFreightSku", "Состав (перевод)", COMPOSITION_CLASS, freight, sku);
        mainCompositionFreightSku.setDerivedForcedChange(LM.addJProp(LM.and1, mainCompositionSku, 2, quantityFreightSku, 1, 2), 1, 2, LM.is(freightChanged), 1);

        LM.addConstraint(LM.addJProp("Для SKU должен быть задан состав", LM.and(true, false), LM.is(freightChanged), 1, mainCompositionFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        additionalCompositionFreightSku = LM.addDProp(LM.baseGroup, "additionalCompositionFreightSku", "Доп. состав (перевод)", COMPOSITION_CLASS, freight, sku);
        additionalCompositionFreightSku.setDerivedForcedChange(LM.addJProp(LM.and1, additionalCompositionSku, 2, quantityFreightSku, 1, 2), 1, 2, LM.is(freightChanged), 1);

        countryOfOriginFreightSku = LM.addDProp(LM.idGroup, "countryOfOriginFreightSku", "Страна (ИД)", LM.country, freight, sku);
        countryOfOriginFreightSku.setDerivedForcedChange(LM.addJProp(LM.and1, countryOfOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, LM.is(freightChanged), 1);

        LM.addConstraint(LM.addJProp("Для SKU должна быть задана страна", LM.and(true, false), LM.is(freightChanged), 1, countryOfOriginFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        sidCountryOfOriginFreightSku = LM.addJProp(LM.baseGroup, "sidCountryOfOriginFreightSku", "Код страны", LM.sidCountry, countryOfOriginFreightSku, 1, 2);
        nameCountryOfOriginFreightSku = LM.addJProp(LM.baseGroup, "nameCountryOfOriginFreightSku", "Страна", LM.name, countryOfOriginFreightSku, 1, 2);

        quantityImporterFreightArticleCompositionCountryCategory = LM.addSGProp(LM.baseGroup, "quantityImporterFreightArticleCompositionCountryCategory", "Кол-во",
                quantityProxyImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        compositionFreightArticleCompositionCountryCategory = LM.addMGProp(LM.baseGroup, "compositionFreightArticleCompositionCountryCategory", "Состав",
                mainCompositionFreightSku, 1, articleSku, 2, mainCompositionOriginFreightSku, 1, 2, countryOfOriginFreightSku, 1, 2, customCategory10FreightSku, 1, 2);

        netWeightStockSku = LM.addJProp(LM.baseGroup, "netWeightStockSku", "Вес нетто", LM.multiplyDouble2, quantityStockSku, 1, 2, netWeightSku, 2);
        netWeightStockArticle = LM.addSGProp(LM.baseGroup, "netWeightStockArticle", "Вес нетто", netWeightStockSku, 1, articleSku, 2);
        netWeightStock = LM.addSGProp(LM.baseGroup, "netWeightStock", "Вес нетто короба", netWeightStockSku, 1);

        netWeightFreightSku = LM.addDProp(LM.baseGroup, "netWeightFreightSku", "Вес нетто", DoubleClass.instance, freight, sku);
        netWeightFreightSku.setDerivedForcedChange(LM.addJProp(LM.and1, netWeightSku, 2, quantityFreightSku, 1, 2), 1, 2, LM.is(freightChanged), 1);

        LM.addConstraint(LM.addJProp("Для SKU должен быть задан вес нетто", LM.and(true, false), LM.is(freightChanged), 1, netWeightFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        netWeightImporterFreightUnitSku = LM.addJProp(LM.baseGroup, "netWeightImporterFreightUnitSku", "Вес нетто", LM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, LM.addJProp(netWeightFreightSku, freightFreightUnit, 1, 2), 2, 3);
        netWeightImporterFreightUnitArticle = LM.addSGProp(LM.baseGroup, "netWeightImporterFreightUnitArticle", "Вес нетто", netWeightImporterFreightUnitSku, 1, 2, articleSku, 3);

        netWeightImporterFreightUnit = LM.addSGProp(LM.baseGroup, "netWeightImporterFreightUnit", "Вес нетто", netWeightImporterFreightUnitSku, 1, 2);

        netWeightImporterFreightSku = LM.addJProp(LM.baseGroup, "netWeightImporterFreightSku", "Вес нетто", LM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);

        netWeightImporterFreightCustomCategory6 = LM.addSGProp(LM.baseGroup, "netWeightImporterFreightCustomCategory6", "Вес нетто", netWeightImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);

        netWeightImporterFreightArticleCompositionCountryCategory = LM.addSGProp(LM.baseGroup, "netWeightImporterFreightArticleCompositionCountryCategory", "Вес нетто",
                netWeightImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        netWeightImporterFreight = LM.addSGProp(LM.baseGroup, "netWeightImporterFreight", "Вес нетто", netWeightImporterFreightSku, 1, 2);
        //netWeightImporterFreightBox = LM.addSGProp(LM.baseGroup, "netWeightImporterFreight", "Вес нетто", netWeightImporterFreightSku, 1, 2);

        priceImporterFreightSku = LM.addDProp(LM.baseGroup, "priceImporterFreightSku", "Цена входная", DoubleClass.instance, importer, freight, sku);
        priceMaxImporterFreightSku = LM.addMGProp(LM.baseGroup, "priceMaxImporterFreightSku", false, "Цена входная", priceInInvoiceFreightUnitSku, importerInvoice, 1, freightFreightUnit, 2, 3);
        priceInImporterFreightSku = LM.addSUProp(LM.baseGroup, "priceInImporterFreightSku", "Цена входная", Union.OVERRIDE, priceMaxImporterFreightSku, priceImporterFreightSku);

        sumInImporterFreightSku = LM.addJProp(LM.baseGroup, "sumInImporterFreightSku", "Сумма входная", LM.multiplyDouble2, quantityProxyImporterFreightSku, 1, 2, 3, priceInImporterFreightSku, 1, 2, 3);

        sumFreightImporterFreightSku = LM.addPGProp(LM.baseGroup, "sumFreightImporterFreightSku", false, 2, false, "Сумма фрахта",
                netWeightImporterFreightSku,
                sumFreightFreight, 2);

        LP priceAggrFreightImporterFreightSku = LM.addJProp(LM.divideDouble2, sumFreightImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);

//        priceFreightImporterFreightSku = LM.addJProp(LM.baseGroup, "priceFreightImporterFreightSku", "Цена за фрахт", divideDouble2, sumFreightImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        priceFreightImporterFreightSku = LM.addDProp(LM.baseGroup, "priceFreightImporterFreightSku", "Цена за фрахт", DoubleClass.instance, importer, freight, sku);
        priceFreightImporterFreightSku.setDerivedForcedChange(priceAggrFreightImporterFreightSku, 1, 2, 3, LM.is(freightPriced), 2, sumFreightFreight, 2);

        priceExpenseImporterFreightSku = LM.addJProp(LM.baseGroup, "priceExpenseImporterFreightSku", "Цена затр.", LM.sumDouble2, priceInImporterFreightSku, 1, 2, 3, priceFreightImporterFreightSku, 1, 2, 3);

        markupPercentImporterFreightBrandSupplier = LM.addDProp(LM.baseGroup, "markupPercentImporterFreightBrandSupplier", "Надбавка (%)", DoubleClass.instance, importer, freight, brandSupplier);
        markupPercentImporterFreightDataSku = LM.addDProp(LM.baseGroup, "markupPercentImporterFreightDataSku", "Надбавка (%)", DoubleClass.instance, importer, freight, sku);
        markupPercentImporterFreightBrandSupplierSku = LM.addJProp(LM.baseGroup, "markupPercentImporterFreightBrandSupplierSku", true, "Надбавка (%)", markupPercentImporterFreightBrandSupplier, 1, 2, brandSupplierArticleSku, 3);
        markupPercentImporterFreightSku = LM.addSUProp(LM.baseGroup, "markupPercentImporterFreightSku", true, "Надбавка (%)", Union.OVERRIDE, markupPercentImporterFreightBrandSupplierSku, markupPercentImporterFreightDataSku);

        // надбавка на цену без учёта стоимости фрахта
        markupInImporterFreightSku = LM.addJProp(LM.baseGroup, "markupInImporterFreightSku", "Надбавка", LM.percent2, priceInImporterFreightSku, 1, 2, 3, markupPercentImporterFreightSku, 1, 2, 3);

        priceMarkupInImporterFreightSku = LM.addJProp(LM.baseGroup, "priceMarkupInImporterFreightSku", "Цена выходная", LM.sumDouble2, priceInImporterFreightSku, 1, 2, 3, markupInImporterFreightSku, 1, 2, 3);

        priceInOutImporterFreightSku = LM.addDProp(LM.baseGroup, "priceInOutImporterFreightSku", "Цена выходная", DoubleClass.instance, importer, freight, sku);
        priceInOutImporterFreightSku.setDerivedForcedChange(true, LM.addJProp(LM.and1, priceMarkupInImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3), 1, 2, 3, LM.is(freightPriced), 2);

        priceImporterFreightArticleCompositionCountryCategory = LM.addMGProp(LM.baseGroup, "priceImporterFreightArticleCompositionCountryCategory", "Цена",
                priceInOutImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        sumImporterFreightSku = LM.addJProp(LM.baseGroup, "sumImporterFreightSku", "Сумма", LM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);
        sumImporterFreightCustomCategory6 = LM.addSGProp(LM.baseGroup, "sumImporterFreightCustomCategory6", "Сумма", sumImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);

        sumImporterFreightUnitSku = LM.addJProp(LM.baseGroup, "sumImporterFreightUnitSku", "Сумма", LM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, LM.addJProp(priceInOutImporterFreightSku, 1, freightFreightUnit, 2, 3), 1, 2, 3);

        sumImporterFreightUnitArticle = LM.addSGProp(LM.baseGroup, "sumImporterFreightUnitArticle", "Сумма", sumImporterFreightUnitSku, 1, 2, articleSku, 3);

        sumImporterFreightArticleCompositionCountryCategory = LM.addJProp(LM.baseGroup, "sumImporterFreightArticleCompositionCountryCategory", "Сумма", LM.multiplyDouble2,
                quantityImporterFreightArticleCompositionCountryCategory, 1, 2, 3, 4, 5, 6,
                priceImporterFreightArticleCompositionCountryCategory, 1, 2, 3, 4, 5, 6);

        sumImporterFreight = LM.addSGProp(LM.baseGroup, "sumImporterFreight", "Сумма выходная", sumImporterFreightArticleCompositionCountryCategory, 1, 2);

        sumSbivkaImporterFreight = LM.addSGProp(LM.baseGroup, "sumSbivkaImporterFreight", "Сумма выходная", sumImporterFreightSku, 1, 2);

        sumMarkupInImporterFreightSku = LM.addJProp(LM.baseGroup, "sumMarkupInImporterFreightSku", "Сумма надбавки", LM.multiplyDouble2, quantityProxyImporterFreightSku, 1, 2, 3, markupInImporterFreightSku, 1, 2, 3);
        sumInOutImporterFreightSku = LM.addJProp(LM.baseGroup, "sumInOutImporterFreightSku", "Сумма выходная", LM.multiplyDouble2, quantityProxyImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);

        sumMarkupInImporterFreight = LM.addSGProp(LM.baseGroup, "sumMarkupInImporterFreight", "Сумма надбавки", sumMarkupInImporterFreightSku, 1, 2);
        sumInOutImporterFreight = LM.addSGProp(LM.baseGroup, "sumInOutImporterFreight", "Сумма выходная", sumInOutImporterFreightSku, 1, 2);

        sumMarkupInFreight = LM.addSGProp(LM.baseGroup, "sumMarkupInFreight", "Сумма надбавки", sumMarkupInImporterFreight, 2);
        sumInOutFreight = LM.addSGProp(LM.baseGroup, "sumInOutFreight", "Сумма выходная", sumInOutImporterFreight, 2);

        // надбавка на цену с учётом стоимости фрахта
        markupImporterFreightSku = LM.addJProp(LM.baseGroup, "markupImporterFreightSku", "Надбавка", LM.percent2, priceExpenseImporterFreightSku, 1, 2, 3, markupPercentImporterFreightSku, 1, 2, 3);
        sumMarkupImporterFreightSku = LM.addJProp(LM.baseGroup, "sumMarkupImporterFreightSku", "Сумма надбавки", LM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, markupImporterFreightSku, 1, 2, 3);

        priceOutImporterFreightSku = LM.addJProp(LM.baseGroup, "priceOutImporterFreightSku", "Цена выходная", LM.sumDouble2, priceExpenseImporterFreightSku, 1, 2, 3, markupImporterFreightSku, 1, 2, 3);
        sumOutImporterFreightSku = LM.addJProp(LM.baseGroup, "sumOutImporterFreightSku", "Сумма выходная", LM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, priceOutImporterFreightSku, 1, 2, 3);

        sumInImporterFreight = LM.addSGProp(LM.baseGroup, "sumInImporterFreight", "Сумма входная", sumInImporterFreightSku, 1, 2);
        sumMarkupImporterFreight = LM.addSGProp(LM.baseGroup, "sumMarkupImporterFreight", "Сумма надбавки", sumMarkupImporterFreightSku, 1, 2);

        sumOutImporterFreight = LM.addSGProp(LM.baseGroup, "sumOutImporterFreight", "Сумма выходная", sumOutImporterFreightSku, 1, 2);

        sumInFreight = LM.addSGProp(LM.baseGroup, "sumInFreight", "Сумма входная", sumInImporterFreight, 2);
        sumMarkupFreight = LM.addSGProp(LM.baseGroup, "sumMarkupFreight", "Сумма надбавки", sumMarkupImporterFreight, 2);
        sumOutFreight = LM.addSGProp(LM.baseGroup, "sumOutFreight", "Сумма выходная", sumOutImporterFreight, 2);

        // итоги с начала года
        sumInCurrentYear = LM.addSGProp(LM.baseGroup, "sumInCurrentYear", "Итого вход", LM.addJProp(LM.and1, sumInFreight, 1, LM.addJProp(LM.equals2, LM.addJProp(LM.yearInDate, LM.currentDate), LM.addJProp(LM.yearInDate, LM.date, 1), 1), 1));
        sumInOutCurrentYear = LM.addSGProp(LM.baseGroup, "sumInOutCurrentYear", "Итого выход", LM.addJProp(LM.and1, sumInOutFreight, 1, LM.addJProp(LM.equals2, LM.addJProp(LM.yearInDate, LM.currentDate), LM.addJProp(LM.yearInDate, LM.date, 1), 1), 1));
        balanceSumCurrentYear = LM.addDUProp(LM.baseGroup, "balanceSumCurrentYear", "Сальдо", sumInOutCurrentYear, sumInCurrentYear);

        quantityImporterFreightBrandSupplier = LM.addSGProp(LM.baseGroup, "quantityImporterFreightBrandSupplier", "Кол-во позиций", quantityImporterFreightSku, 1, 2, brandSupplierArticleSku, 3);

        quantityImporterFreight = LM.addSGProp(LM.baseGroup, "quantityImporterFreight", "Кол-во позиций", quantityProxyImporterFreightSku, 1, 2);
        quantitySbivkaImporterFreight = LM.addSGProp(LM.baseGroup, "quantitySbivkaImporterFreight", "Кол-во позиций", quantityImporterFreightSku, 1, 2);

        // Текущие палеты/коробки для приема
        currentPalletRouteUser = LM.addDProp("currentPalletRouteUser", "Тек. паллета (ИД)", pallet, route, LM.user);

        currentPalletRoute = LM.addJProp(true, "currentPalletRoute", "Тек. паллета (ИД)", currentPalletRouteUser, 1, LM.currentUser);
        barcodeCurrentPalletRoute = LM.addJProp("barcodeCurrentPalletRoute", "Тек. паллета (штрих-код)", LM.barcode, currentPalletRoute, 1);

        sumNetWeightFreightSku = LM.addJProp(LM.baseGroup, "sumNetWeightFreightSku", "Вес нетто (всего)", LM.multiplyDouble2, quantityFreightSku, 1, 2, netWeightSku, 2);

        grossWeightCurrentPalletRoute = LM.addJProp(true, "grossWeightCurrentPalletRoute", "Вес брутто", grossWeightPallet, currentPalletRoute, 1);
        grossWeightFreight = LM.addSUProp(LM.baseGroup, "freightGrossWeight", "Вес брутто (фрахт)", Union.SUM,
                LM.addSGProp(grossWeightPallet, freightPallet, 1),
                LM.addSGProp(grossWeightDirectInvoice, freightDirectInvoice, 1));

        sumGrossWeightFreightSku = LM.addPGProp(LM.baseGroup, "sumGrossWeightFreightSku", false, 1, false, "Вес брутто",
                sumNetWeightFreightSku,
                grossWeightFreight, 1);

        grossWeightFreightSkuAggr = LM.addJProp(LM.baseGroup, "grossWeightFreightSkuAggr", "Вес брутто", LM.divideDouble2, sumGrossWeightFreightSku, 1, 2, quantityFreightSku, 1, 2);
        grossWeightFreightSku = LM.addDProp(LM.baseGroup, "grossWeightFreightSku", "Вес брутто", DoubleClass.instance, freight, sku);
        grossWeightFreightSku.setDerivedForcedChange(LM.addJProp(LM.and1, grossWeightFreightSkuAggr, 1, 2, quantityFreightSku, 1, 2), 1, 2, LM.is(freightChanged), 1);

        grossWeightImporterFreightSku = LM.addJProp(LM.baseGroup, "grossWeightImporterFreightSku", "Вес брутто", LM.multiplyDouble2, quantityImporterFreightSku, 1, 2, 3, grossWeightFreightSku, 2, 3);

        grossWeightImporterFreightCustomCategory6 = LM.addSGProp(LM.baseGroup, "grossWeightImporterFreightCustomCategory6", "Вес брутто", grossWeightImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);

        grossWeightImporterFreight = LM.addSGProp(LM.baseGroup, "grossWeightImporterFreight", "Вес брутто", grossWeightImporterFreightSku, 1, 2);
        grossWeightImporterFreightUnitSku = LM.addJProp(LM.baseGroup, "grossWeightImporterFreightUnitSku", "Вес брутто", LM.multiplyDouble2, quantityImporterStockSku, 1, 2, 3, LM.addJProp(grossWeightFreightSku, freightFreightUnit, 2, 3), 1, 2, 3);
        grossWeightImporterFreightUnitArticle = LM.addSGProp(LM.baseGroup, "grossWeightImporterFreightUnitArticle", "Вес брутто", grossWeightImporterFreightUnitSku, 1, 2, articleSku, 3);
        grossWeightImporterFreightUnit = LM.addSGProp(LM.baseGroup, "grossWeightImporterFreightUnit", "Вес брутто", grossWeightImporterFreightUnitSku, 1, 2);

        grossWeightImporterFreightArticleCompositionCountryCategory = LM.addSGProp(LM.baseGroup, "grossWeightImporterFreightArticleCompositionCountryCategory", "Вес брутто",
                grossWeightImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        currentFreightBoxRouteUser = LM.addDProp("currentFreightBoxRouteUser", "Тек. короб (ИД)", freightBox, route, LM.user);

        currentFreightBoxRoute = LM.addJProp(true, "currentFreightBoxRoute", "Тек. короб (ИД)", currentFreightBoxRouteUser, 1, LM.currentUser);
        barcodeCurrentFreightBoxRoute = LM.addJProp("barcodeCurrentFreightBoxRoute", "Тек. короб (штрих-код)", LM.barcode, currentFreightBoxRoute, 1);

        destinationCurrentFreightBoxRoute = LM.addJProp(true, "destinationCurrentFreightBoxRoute", "Пункт назначения тек. короба (ИД)", destinationFreightBox, currentFreightBoxRoute, 1);
        nameDestinationCurrentFreightBoxRoute = LM.addJProp("nameDestinationCurrentFreightBoxRoute", "Пункт назначения тек. короба", LM.name, destinationCurrentFreightBoxRoute, 1);

        isCurrentFreightBox = LM.addJProp(LM.equals2, LM.addJProp(true, currentFreightBoxRoute, routeCreationFreightBoxFreightBox, 1), 1, 1);
        isCurrentPallet = LM.addJProp(LM.equals2, LM.addJProp(true, currentPalletRoute, routeCreationPalletPallet, 1), 1, 1);
        currentPalletFreightBox = LM.addJProp(currentPalletRoute, routeCreationFreightBoxFreightBox, 1);
        isCurrentPalletFreightBox = LM.addJProp(LM.equals2, palletFreightBox, 1, currentPalletFreightBox, 1);
        isStoreFreightBoxSupplierBox = LM.addJProp(LM.equals2, destinationFreightBox, 1, destinationSupplierBox, 2);

        barcodeActionSeekPallet = LM.addJProp(true, "Найти палету", isCurrentPallet, LM.barcodeToObject, 1);
        barcodeActionCheckPallet = LM.addJProp(true, "Проверка паллеты",
                LM.addJProp(true, LM.and(false, true),
                        LM.addStopActionProp("Для маршрута выбранного короба не задана паллета", "Поиск по штрих-коду"),
                        LM.is(freightBox), 1,
                        currentPalletFreightBox, 1), LM.barcodeToObject, 1);
        barcodeActionSeekFreightBox = LM.addJProp(true, "Найти короб для транспортировки", isCurrentFreightBox, LM.barcodeToObject, 1);
        barcodeActionSetPallet = LM.addJProp(true, "Установить паллету", isCurrentPalletFreightBox, LM.barcodeToObject, 1);
        barcodeActionSetStore = LM.addJProp(true, "Установить магазин", isStoreFreightBoxSupplierBox, LM.barcodeToObject, 1, 2);

        changePallet = LM.addJProp(true, "Изменить паллету", isCurrentPalletFreightBox, currentFreightBoxRoute, 1);

        barcodeActionSetFreight = LM.addJProp(true, "Установить фрахт", equalsPalletFreight, LM.barcodeToObject, 1, 2);

        addBoxShipmentDetailBoxShipmentSupplierBoxRouteBarcode = LM.addJProp(true, "Добавить строку поставки",
                addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode, 1, 2, currentFreightBoxRoute, 3, 4);

        addSimpleShipmentDetailSimpleShipmentRouteBarcode = LM.addJProp(true, "Добавить строку поставки",
                addSimpleShipmentSimpleShipmentDetailStockBarcode, 1, currentFreightBoxRoute, 2, 3);

        quantityRouteSku = LM.addJProp(LM.baseGroup, "quantityRouteSku", "Оприход. (МХ)", quantityStockSku, currentFreightBoxRoute, 1, 2);

        quantitySupplierBoxBoxShipmentRouteSku = LM.addJProp(LM.baseGroup, true, "quantitySupplierBoxBoxShipmentRouteSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4);
        quantitySimpleShipmentRouteSku = LM.addJProp(LM.baseGroup, true, "quantitySimpleShipmentRouteSku", "Кол-во оприход.",
                quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3);

        createFreightBox = LM.addJProp(true, "Сгенерировать короба", LM.addAAProp(freightBox, LM.barcode, LM.barcodePrefix, true), quantityCreationFreightBox, 1);
        createPallet = LM.addJProp(true, "Сгенерировать паллеты", LM.addAAProp(pallet, LM.barcode, LM.barcodePrefix, true), quantityCreationPallet, 1);

        barcodeActionCheckFreightBox = LM.addJProp(true, "Проверка короба для транспортировки",
                LM.addJProp(true, LM.and(false, false, true),
                        LM.addStopActionProp("Для выбранного маршрута не задан короб для транспортировки", "Поиск по штрих-коду"),
                        LM.is(sku), 2,
                        LM.is(route), 1,
                        currentFreightBoxRoute, 1), 1, LM.barcodeToObject, 2);
        barcodeAction4 = LM.addJProp(true, "Ввод штрих-кода 4",
                LM.addCUProp(
                        LM.addSCProp(LM.addJProp(true, quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4))
                ), 1, 2, 3, LM.barcodeToObject, 4);
        barcodeAction3 = LM.addJProp(true, "Ввод штрих-кода 3",
                LM.addCUProp(
                        LM.addSCProp(LM.addJProp(true, quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3))
                ), 1, 2, LM.barcodeToObject, 3);
    }

    public LP addDEAProp() {
        return LM.addProperty(null, new LP<ClassPropertyInterface>(new DeclarationExportActionProperty("declarationExport", "Экспорт декларанта", BL, importer, freight)));
    }

    @Override
    public void initIndexes() {
    }

    @Override
    public void initNavigators() throws JRException, FileNotFoundException {
        NavigatorElement classifier = new NavigatorElement(LM.baseElement, "classifier", "Справочники");
        NavigatorElement classifierCurrency = new NavigatorElement(classifier, "classifierCurrency", "Валюты и курсы");
        classifierCurrency.add(currency.getClassForm(BL));
        classifierCurrency.add(typeExchange.getClassForm(BL));
        LM.addFormEntity(new RateCurrencyFormEntity(classifierCurrency, "rateCurrencyForm", "Курсы валют"));

        NavigatorElement classifierItem = new NavigatorElement(classifier, "classifierItem", "Для описания товаров");
        LM.addFormEntity(new CustomCategoryFormEntity(classifierItem, "customCategoryForm", "ТН ВЭД (изменения)", false));
        LM.addFormEntity(new CustomCategoryFormEntity(classifierItem, "customCategoryForm2", "ТН ВЭД (дерево)", true));
        classifierItem.add(category.getClassForm(BL));
        classifierItem.add(commonSize.getClassForm(BL));
        classifierItem.add(season.getClassForm(BL));
        classifierItem.add(LM.country.getClassForm(BL));
        classifierItem.add(unitOfMeasure.getClassForm(BL));

        LM.addFormEntity(new GlobalParamFormEntity(classifier, "globalParamForm", "Общие параметры"));
        LM.addFormEntity(new ColorSizeSupplierFormEntity(classifier, "сolorSizeSupplierForm", "Поставщики"));
        classifier.add(importer.getClassForm(BL));
        classifier.add(exporter.getClassForm(BL));
        classifier.add(store.getClassForm(BL));
        LM.addFormEntity(new ContractFormEntity(classifier, "contractForm", "Договора"));
        LM.addFormEntity(new NomenclatureFormEntity(classifier, "nomenclatureForm", "Номенклатура"));
        classifier.add(freightType.getClassForm(BL));

        createItemForm = LM.addFormEntity(new CreateItemFormEntity(null, "createItemForm", "Ввод товара"));

        NavigatorElement printForms = new NavigatorElement(LM.baseElement, "printForms", "Печатные формы");

        LM.addFormEntity(new AnnexInvoiceFormEntity(printForms, "annexInvoiceForm", "Приложение к инвойсу", false));
        invoiceFromFormEntity = LM.addFormEntity(new AnnexInvoiceFormEntity(printForms, "annexInvoiceForm2", "Приложение к инвойсу (перевод)", true));
        LM.addFormEntity(new InvoiceFromFormEntity(printForms, "invoiceFromForm", "Исходящие инвойсы", false));
        LM.addFormEntity(new InvoiceFromFormEntity(printForms, "invoiceFromForm2", "Исходящие инвойсы (перевод)", true));

        LM.addFormEntity(new ProformInvoiceFormEntity(printForms, "proformInvoiceForm", "Исходящие инвойсы-проформы", false));
        LM.addFormEntity(new ProformInvoiceFormEntity(printForms, "proformInvoiceForm2", "Исходящие инвойсы-проформы (перевод)", true));

        LM.addFormEntity(new SbivkaFormEntity(printForms, "sbivkaForm", "Сбивка товаров"));
        LM.addFormEntity(new PackingListFormEntity(printForms, "packingListForm", "Исходящие упаковочные листы"));
        LM.addFormEntity(new PackingListBoxFormEntity(printForms, "packingListBoxForm", "Упаковочные листы коробов"));
        FormEntity createPalletForm = LM.addFormEntity(new CreatePalletFormEntity(printForms, "createPalletForm", "Штрих-коды паллет", FormType.PRINT));
        FormEntity createFreightBoxForm = LM.addFormEntity(new CreateFreightBoxFormEntity(printForms, "createFreightBoxForm", "Штрих-коды коробов", FormType.PRINT));

        NavigatorElement purchase = new NavigatorElement(LM.baseElement, "purchase", "Управление закупками");
        LM.addFormEntity(new OrderFormEntity(purchase, "orderForm", "Заказы"));
        LM.addFormEntity(new InvoiceFormEntity(purchase, "boxInvoiceForm", "Инвойсы по коробам", true));
        //LM.addFormEntity(new InvoiceFormEntity(purchase, "simpleInvoiceForm", "Инвойсы без коробов", false));
        LM.addFormEntity(new ShipmentListFormEntity(purchase, "boxShipmentListForm", "Поставки по коробам", true));
        //LM.addFormEntity(new ShipmentListFormEntity(purchase, "simpleShipmentListForm", "Поставки без коробов", false));
        LM.addFormEntity(new PricatFormEntity(purchase, "pricatForm", "Прайсы"));

        NavigatorElement shipment = new NavigatorElement(LM.baseElement, "shipment", "Управление фрахтами");
        LM.addFormEntity(new FreightShipmentFormEntity(shipment, "freightShipmentForm", "Комплектация фрахта"));
        LM.addFormEntity(new FreightInvoiceFormEntity(shipment, "freightInvoiceForm", "Расценка фрахта"));
        LM.addFormEntity(new FreightChangeFormEntity(shipment, "freightChangeForm", "Обработка фрахта"));

        NavigatorElement distribution = new NavigatorElement(LM.baseElement, "distribution", "Управление складом");
        FormEntity createPalletFormCreate = LM.addFormEntity(new CreatePalletFormEntity(distribution, "createPalletFormAdd", "Сгенерировать паллеты", FormType.ADD));
        LM.addFormEntity(new CreatePalletFormEntity(createPalletFormCreate, "createPalletFormList", "Документы генерации паллет", FormType.LIST));
        FormEntity createFreightBoxFormAdd = LM.addFormEntity(new CreateFreightBoxFormEntity(distribution, "createFreightBoxFormAdd", "Сгенерировать короба", FormType.ADD));
        LM.addFormEntity(new CreateFreightBoxFormEntity(createFreightBoxFormAdd, "createFreightBoxFormList", "Документы генерации коробов", FormType.LIST));
        LM.addFormEntity(new ShipmentSpecFormEntity(distribution, "boxShipmentSpecForm", "Прием товара по коробам", true));
        LM.addFormEntity(new ShipmentSpecFormEntity(distribution, "simpleShipmentSpecForm", "Прием товара без коробов", false));
        LM.addFormEntity(new FreightShipmentStoreFormEntity(distribution, "freightShipmentStoreForm", "Комплектация фрахта (на складе)"));
        LM.addFormEntity(new BalanceBrandWarehouseFormEntity(distribution, "balanceBrandWarehouseForm", "Остатки на складе (по брендам)"));
        LM.addFormEntity(new BalanceWarehouseFormEntity(distribution, "balanceWarehouseForm", "Остатки на складе"));
        LM.addFormEntity(new InvoiceShipmentFormEntity(distribution, "invoiceShipmentForm", "Сравнение по инвойсам"));

        // пока не поддерживается из-за того, что пока нет расчета себестоимости для внутреннего перемещения
//        LM.addFormEntity(new StockTransferFormEntity(distribution, "stockTransferForm", "Внутреннее перемещение"));
    }

    private class BarcodeFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objBarcode;

        protected Font getDefaultFont() {
            return null;
        }

        private BarcodeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", LM.objectValue, LM.barcodeObjectName);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

            objBarcode.resetOnApply = true;

            addPropertyDraw(LM.reverseBarcode);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            if (getDefaultFont() != null)
                design.setFont(getDefaultFont());

            PropertyDrawView barcodeView = design.get(getPropertyDraw(LM.objectValue, objBarcode));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(LM.reverseBarcode)));
//            design.getPanelContainer(design.get(objBarcode.groupTo)).constraints.maxVariables = 0;

            design.setBackground(LM.barcodeObjectName, new Color(240, 240, 240));

            design.setEditKey(barcodeView, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
            design.setEditKey(LM.reverseBarcode, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

            design.setFocusable(LM.reverseBarcode, false);
            design.setFocusable(false, objBarcode.groupTo);

            return design;
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

            objTypeExchange = addSingleGroupObject(typeExchange, "Тип обмена", LM.objectValue, LM.name, nameCurrencyTypeExchange);
            objTypeExchange.groupTo.initClassView = ClassViewType.PANEL;

            objCurrency = addSingleGroupObject(currency, "Валюта", LM.name);
            objCurrency.groupTo.initClassView = ClassViewType.GRID;

            objDate = addSingleGroupObject(DateClass.instance, "Дата", LM.objectValue);
            objDate.groupTo.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(rateExchange, objTypeExchange, objCurrency, objDate);

            objDateRate = addSingleGroupObject(DateClass.instance, "Дата", LM.objectValue);

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

            objBox = addSingleGroupObject(1, "box", freightBox, "Короб", LM.barcode, netWeightStock);
            objBox.groupTo.initClassView = ClassViewType.PANEL;

            objArticle = addSingleGroupObject(2, "article", article, "Артикул", sidArticle, nameBrandSupplierArticle, nameArticle);

            addPropertyDraw(quantityStockArticle, objBox, objArticle);
            addPropertyDraw(netWeightStockArticle, objBox, objArticle);
            objArticle.groupTo.initClassView = ClassViewType.GRID;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStockArticle, objBox, objArticle)));

            packingListFormFreightBox = LM.addFAProp("Упаковочный лист", this, objBox);
            packingListFormRoute = LM.addJProp(true, "packingListFormRoute", "Упаковочный лист", packingListFormFreightBox, currentFreightBoxRoute, 1);
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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", LM.name, nameCurrencySupplier);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objOrder = addSingleGroupObject(order, "Заказ", LM.date, sidDocument, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);
            LM.addObjectActions(this, objOrder);

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", LM.objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, objOrder, objSIDArticleComposite));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(seekArticleSIDSupplier, objSIDArticleComposite, objSupplier));

            objSIDArticleSingle = addSingleGroupObject(StringClass.get(50), "Ввод простого артикула", LM.objectValue);
            objSIDArticleSingle.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(addNEArticleSingleSIDSupplier, objSIDArticleSingle, objSupplier));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(incrementNumberListSID, objOrder, objSIDArticleSingle));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(seekArticleSIDSupplier, objSIDArticleSingle, objSupplier));

            objArticle = addSingleGroupObject(article, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, objOrder, objArticle);
            addPropertyDraw(objArticle, sidArticle, sidBrandSupplierArticle, nameBrandSupplierArticle, nameSeasonArticle, nameThemeSupplierArticle, originalNameArticle, nameCountrySupplierOfOriginArticle, nameCountryOfOriginArticle, LM.barcode);
            addPropertyDraw(quantityListArticle, objOrder, objArticle);
            addPropertyDraw(priceDocumentArticle, objOrder, objArticle);
            addPropertyDraw(RRPDocumentArticle, objOrder, objArticle);
            addPropertyDraw(sumDocumentArticle, objOrder, objArticle);
            addPropertyDraw(invoicedOrderArticle, objOrder, objArticle);
            addPropertyDraw(LM.delete, objArticle);

            objItem = addSingleGroupObject(item, "Товар", LM.barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            LM.addObjectActions(this, objItem);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", LM.selection, sidSizeSupplier);
            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", LM.selection, sidColorSupplier, LM.name);

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
            design.get(getPropertyDraw(LM.date, objOrder)).caption = "Дата заказа";

            design.defaultOrders.put(design.get(getPropertyDraw(numberListArticle)), true);

            design.get(getPropertyDraw(LM.objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(LM.objectValue, objSIDArticleSingle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", LM.name, nameCurrencySupplier, importInvoiceActionGroup, true);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс", LM.date, LM.objectClassName, sidDocument, nameCurrencyDocument, sumDocument, quantityDocument, netWeightDocument, nameImporterInvoice, sidContractInvoice, sidDestinationDestinationDocument, nameDestinationDestinationDocument);
            LM.addObjectActions(this, objInvoice);

            objOrder = addSingleGroupObject(order, "Заказ");
            objOrder.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inOrderInvoice, objOrder, objInvoice);
            addPropertyDraw(objOrder, LM.date, sidDocument, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб", sidSupplierBox, LM.barcode);
                objSupplierBox.groupTo.initClassView = ClassViewType.PANEL;
                LM.addObjectActions(this, objSupplierBox);
            }

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", LM.objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleComposite));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(seekArticleSIDSupplier, objSIDArticleComposite, objSupplier));

            objSIDArticleSingle = addSingleGroupObject(StringClass.get(50), "Ввод простого артикула", LM.objectValue);
            objSIDArticleSingle.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(addNEArticleSingleSIDSupplier, objSIDArticleSingle, objSupplier));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleSingle));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(seekArticleSIDSupplier, objSIDArticleSingle, objSupplier));

            objArticle = addSingleGroupObject(article, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(objArticle, sidArticle, sidBrandSupplierArticle, nameBrandSupplierArticle, nameSeasonArticle, nameThemeSupplierArticle, originalNameArticle, sidCustomCategoryOriginArticle,
                    nameCountrySupplierOfOriginArticle, netWeightArticle, mainCompositionOriginArticle, additionalCompositionOriginArticle, LM.barcode);
            addPropertyDraw(quantityListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(priceDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(RRPDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(sumDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(orderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(priceOrderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(LM.delete, objArticle);

            objItem = addSingleGroupObject(item, "Товар", LM.barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            LM.addObjectActions(this, objItem, objArticle, articleComposite);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", LM.selection, sidSizeSupplier);
            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", LM.selection, sidColorSupplier, LM.name);

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
            addPropertyDraw(new LP[] {LM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                                      sidBrandSupplierArticleSku, nameBrandSupplierArticleSku, originalNameArticleSku,
                                      nameCountrySupplierOfOriginArticleSku , nameCountryOfOriginSku, netWeightSku,
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
                    LM.addSelectFromListAction(null, "Выбрать заказы", objOrder, new FilterEntity[]{orderSupplierFilter}, inOrderInvoice, true, order, invoice),
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

            design.get(getPropertyDraw(LM.objectClassName, objInvoice)).caption = "Тип инвойса";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(LM.date, objInvoice)).caption = "Дата инвойса";

            design.get(getPropertyDraw(sidDocument, objOrder)).caption = "Номер заказа";
            design.get(getPropertyDraw(LM.date, objOrder)).caption = "Дата заказа";

            design.defaultOrders.put(design.get(getPropertyDraw(numberListArticle)), true);
            design.defaultOrders.put(design.get(getPropertyDraw(numberListSku)), true);

            design.get(objOrder.groupTo).grid.constraints.fillVertical = 0.7;
            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 0.7;
            design.get(objItem.groupTo).grid.constraints.fillHorizontal = 1.5;

            design.get(getPropertyDraw(LM.objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(LM.objectValue, objSIDArticleSingle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", LM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", LM.date, sidDocument, netWeightShipment, grossWeightShipment, quantityPalletShipment, quantityBoxShipment);//, invoicedShipment, sumShipment
            LM.addObjectActions(this, objShipment);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс");
            objInvoice.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objInvoice, true);

            addPropertyDraw(inInvoiceShipment, objInvoice, objShipment);
            addPropertyDraw(objInvoice, LM.date, sidDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            objRoute = addSingleGroupObject(route, "Маршрут", LM.name);
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
            design.get(getPropertyDraw(LM.date, objShipment)).caption = "Дата поставки";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(LM.date, objInvoice)).caption = "Дата инвойса";

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
                objSIDSupplierBox = addSingleGroupObject(StringClass.get(50), "Номер короба", LM.objectValue);
                objSIDSupplierBox.groupTo.setSingleClassView(ClassViewType.PANEL);
            }

            objSupplier = addSingleGroupObject(supplier, "Поставщик", LM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", LM.date, sidDocument);
            objShipment.groupTo.initClassView = ClassViewType.PANEL;

            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб поставщика", sidSupplierBox, LM.barcode, nameDestinationSupplierBox);
                objSupplierBox.groupTo.initClassView = ClassViewType.PANEL;
            }

            objRoute = addSingleGroupObject(route, "Маршрут", LM.name, barcodeCurrentPalletRoute, grossWeightCurrentPalletRoute, barcodeCurrentFreightBoxRoute, nameDestinationCurrentFreightBoxRoute);
            objRoute.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(packingListFormRoute, objRoute);
            addPropertyDraw(changePallet, objRoute);

            nameRoute = addPropertyDraw(LM.name, objRoute);
            nameRoute.forceViewType = ClassViewType.PANEL;

            objSku = addSingleGroupObject(sku, "SKU", LM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameBrandSupplierArticleSku, originalNameArticleSku, nameOriginCategoryArticleSku, nameOriginUnitOfMeasureArticleSku,
                    netWeightArticleSku, sidCustomCategoryOriginArticleSku, nameCountryOfOriginArticleSku, mainCompositionOriginArticleSku,
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

                PropertyObjectEntity diffListShipSkuProperty = addPropertyObject(diffListShipSku, objSupplierBox, objShipment, objSku);
                getPropertyDraw(quantityDataListSku).setPropertyHighlight(diffListShipSkuProperty);
                getPropertyDraw(quantityShipDimensionShipmentSku).setPropertyHighlight(diffListShipSkuProperty);

            } else {
                quantityColumn = addPropertyDraw(quantitySimpleShipmentRouteSku, objShipment, objRoute, objSku);
            }

            quantityColumn.columnGroupObjects.add(objRoute.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(LM.name, objRoute);

            addPropertyDraw(quantityRouteSku, objRoute, objSku);

            addPropertyDraw(percentShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);
            addPropertyDraw(invoicedShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);
            addPropertyDraw(quantityShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);

            PropertyObjectEntity diffShipmentRouteSkuProperty = addPropertyObject(diffShipmentRouteSku, objShipment, objRoute, objSku);
            getPropertyDraw(invoicedShipmentRouteSku).setPropertyHighlight(diffShipmentRouteSkuProperty);
            getPropertyDraw(quantityShipmentRouteSku).setPropertyHighlight(diffShipmentRouteSkuProperty);

            objShipmentDetail = addSingleGroupObject((box ? boxShipmentDetail : simpleShipmentDetail),
                    LM.selection, barcodeSkuShipmentDetail, sidArticleShipmentDetail, sidColorSupplierItemShipmentDetail, nameColorSupplierItemShipmentDetail, sidSizeSupplierItemShipmentDetail,
                    nameBrandSupplierArticleSkuShipmentDetail, sidCustomCategoryOriginArticleSkuShipmentDetail, originalNameArticleSkuShipmentDetail,
                    nameOriginCategoryArticleSkuShipmentDetail, nameOriginUnitOfMeasureArticleSkuShipmentDetail,
                    netWeightArticleSkuShipmentDetail,
                    nameCountryOfOriginArticleSkuShipmentDetail, mainCompositionOriginArticleSkuShipmentDetail,
                    netWeightSkuShipmentDetail, nameCountryOfOriginSkuShipmentDetail,
                    mainCompositionOriginSkuShipmentDetail, additionalCompositionOriginSkuShipmentDetail,
                    sidShipmentShipmentDetail,
                    sidSupplierBoxShipmentDetail, barcodeSupplierBoxShipmentDetail,
                    barcodeStockShipmentDetail, nameRouteFreightBoxShipmentDetail,
                    quantityShipmentDetail, nameUserShipmentDetail, timeShipmentDetail, LM.delete);

            objShipmentDetail.groupTo.setSingleClassView(ClassViewType.GRID);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objShipmentDetail.groupTo);
            setForceViewType(supplierAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);
            setForceViewType(intraAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);

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
                    new CompareFilterEntity(addPropertyObject(userShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(LM.currentUser)),
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
                                                    LM.addJProp(true, LM.addSAProp(null), skuBarcodeObject, 1),
                                                            objBarcode));

            addActionsOnObjectChange(objBarcode,
                                     addPropertyObject(
                                             LM.addJProp(true, LM.andNot1,
                                                     LM.addMFAProp(
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
                    LM.addJProp(true, LM.addAProp(new SeekRouteActionProperty()),
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

            setReadOnly(LM.name, true, objRoute.groupTo);
            setReadOnly(percentShipmentRouteSku, true, objRoute.groupTo);
            setReadOnly(itemAttributeGroup, true, objSku.groupTo);
            setReadOnly(sidArticleSku, true, objSku.groupTo);

            setReadOnly(LM.baseGroup, true, objShipmentDetail.groupTo);
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

            design.blockedScreen.put("changePropertyDraw", getPropertyDraw(LM.objectValue, objBarcode).getID() + "");

            design.get(getPropertyDraw(sidDocument, objShipment)).caption = "Номер поставки";
            design.get(getPropertyDraw(LM.date, objShipment)).caption = "Дата поставки";

            if (box)
                design.setEditKey(design.get(getPropertyDraw(LM.objectValue, objSIDSupplierBox)), KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));

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

            objFreight = addSingleGroupObject(freight, "Фрахт", LM.objectValue, LM.date, LM.objectClassName, nameRouteFreight, nameFreightTypeFreight, tonnageFreight, grossWeightFreight, volumeFreight, palletCountFreight, palletNumberFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objFreight, true);

            PropertyObjectEntity diffPalletFreightProperty = addPropertyObject(diffPalletFreight, objFreight);
            getPropertyDraw(palletCountFreight).setPropertyHighlight(diffPalletFreightProperty);
            getPropertyDraw(palletNumberFreight).setPropertyHighlight(diffPalletFreightProperty);

            objPallet = addSingleGroupObject(pallet, "Паллета", LM.barcode, grossWeightPallet, freightBoxNumberPallet);
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

            design.blockedScreen.put("changePropertyDraw", getPropertyDraw(LM.objectValue, objBarcode).getID() + "");

            design.get(getPropertyDraw(LM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(LM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.get(objFreight.groupTo).grid.constraints.fillVertical = 1;
            design.get(objPallet.groupTo).grid.constraints.fillVertical = 2;

            design.setHighlightColor(new Color(128, 255, 128));

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

            objFreight = addSingleGroupObject(freight, "Фрахт", LM.date, LM.objectClassName, nameRouteFreight, nameFreightTypeFreight, tonnageFreight, grossWeightFreight, volumeFreight, palletCountFreight, palletNumberFreight);
            LM.addObjectActions(this, objFreight);

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

            addPropertyDraw(objDateFrom, LM.objectValue);
            addPropertyDraw(objDateTo, LM.objectValue);
            addPropertyDraw(quantityPalletShipmentBetweenDate, objDateFrom, objDateTo);
            addPropertyDraw(quantityPalletFreightBetweenDate, objDateFrom, objDateTo);

            objShipment = addSingleGroupObject(shipment, "Поставка", LM.date, nameSupplierDocument, sidDocument, sumDocument, nameCurrencyDocument, netWeightShipment, grossWeightShipment, quantityPalletShipment, quantityBoxShipment);
            setReadOnly(objShipment, true);

            objPallet = addSingleGroupObject(pallet, "Паллета", LM.barcode, grossWeightPallet, freightBoxNumberPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.GRID);
            setReadOnly(objPallet, true);

            addPropertyDraw(equalsPalletFreight, objPallet, objFreight);

            objSku = addSingleGroupObject(sku, "SKU", sidArticleSku, nameArticleSku, nameBrandSupplierArticleSku, nameColorSupplierItem, sidSizeSupplierItem);
            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            addPropertyDraw(quantityPalletSku, objPallet, objSku);

            setReadOnly(objSku, true);

            objDirectInvoice = addSingleGroupObject(directInvoice, "Инвойс напрямую", LM.date, sidDocument, sumDocument, nameImporterInvoice, sidContractInvoice, nameDestinationDestinationDocument, grossWeightDirectInvoice, palletNumberDirectInvoice);
            setReadOnly(objDirectInvoice, true);

            addPropertyDraw(equalsDirectInvoiceFreight, objDirectInvoice, objFreight);

            addPropertyDraw(quantityShipmentFreight, objShipment, objFreight);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.date, objShipment), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(LM.date, objShipment), Compare.LESS_EQUALS, objDateTo));

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
            design.get(getPropertyDraw(LM.date, objDirectInvoice)).caption = "Дата инвойса";
            design.get(getPropertyDraw(sidDocument, objShipment)).caption = "Номер поставки";
            design.get(getPropertyDraw(LM.date, objShipment)).caption = "Дата поставки";
            design.get(getPropertyDraw(LM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(LM.objectClassName, objFreight)).caption = "Статус фрахта";

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
                addPropertyDraw(objCreate, LM.objectValue);

            addPropertyDraw(objCreate, nameRouteCreationFreightBox, quantityCreationFreightBox);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createFreightBox, objCreate);

            if (!type.equals(FormType.PRINT))
                addPropertyDraw(objCreate, printCreateFreightBoxForm);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.ADD))
                objCreate.addOnTransaction = true;

            objFreightBox = addSingleGroupObject(freightBox, "Короба для транспортировки", LM.barcode);
            setReadOnly(objFreightBox, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationFreightBoxFreightBox, objFreightBox), Compare.EQUALS, objCreate));

            if (type.equals(FormType.PRINT))
                printCreateFreightBoxForm = LM.addFAProp("Печать штрих-кодов", this, objCreate);
        }
    }

    private class CreatePalletFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objPallet;

        private CreatePalletFormEntity(NavigatorElement parent, String sID, String caption, FormType type) {
            super(parent, sID, caption, type.equals(FormType.PRINT));

            objCreate = addSingleGroupObject(creationPallet, "Документ генерации паллет");
            if (!type.equals(FormType.ADD))
                addPropertyDraw(objCreate, LM.objectValue);

            addPropertyDraw(objCreate, nameRouteCreationPallet, quantityCreationPallet);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createPallet, objCreate);

            if (!type.equals(FormType.PRINT))
                addPropertyDraw(objCreate, printCreatePalletForm);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.ADD))
                objCreate.addOnTransaction = true;

            objPallet = addSingleGroupObject(pallet, "Паллеты для транспортировки", LM.barcode);
            setReadOnly(objPallet, true);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationPalletPallet, objPallet), Compare.EQUALS, objCreate));

            if (type.equals(FormType.PRINT))
                printCreatePalletForm = LM.addFAProp("Печать штрих-кодов", this, objCreate);
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
                LM.addObjectActions(this, objCustomCategory4);

            objCustomCategory6 = addSingleGroupObject(customCategory6, "Второй уровень", sidCustomCategory6, nameCustomCategory);
            if (!tree)
                LM.addObjectActions(this, objCustomCategory6);

            objCustomCategory9 = addSingleGroupObject(customCategory9, "Третий уровень", sidCustomCategory9, nameCustomCategory);
            if (!tree)
                LM.addObjectActions(this, objCustomCategory9);

            objCustomCategory10 = addSingleGroupObject(customCategory10, "Четвёртый уровень", sidCustomCategory10, nameCustomCategory);
            LM.addObjectActions(this, objCustomCategory10);

            objCustomCategory4Origin = addSingleGroupObject(customCategory4, "Первый уровень", sidCustomCategory4, nameCustomCategory);
            if (tree) {
                addPropertyDraw(LM.dumb1, objCustomCategory4Origin);
                addPropertyDraw(LM.dumb2, objCustomCategory10, objCustomCategory4Origin);
            }
            LM.addObjectActions(this, objCustomCategory4Origin);

            objCustomCategory6Origin = addSingleGroupObject(customCategory6, "Второй уровень", sidCustomCategory6, nameCustomCategory);
            if (tree) {
                addPropertyDraw(LM.dumb1, objCustomCategory6Origin);
                addPropertyDraw(LM.dumb2, objCustomCategory10, objCustomCategory6Origin);
            }
            LM.addObjectActions(this, objCustomCategory6Origin);

            import1 = addPropertyDraw(importBelTnved);
            import2 = addPropertyDraw(importEuTnved);

            objCustomCategoryOrigin = addSingleGroupObject(customCategoryOrigin, "ЕС уровень", sidCustomCategoryOrigin, nameCustomCategory, sidCustomCategory10CustomCategoryOrigin);
            LM.addObjectActions(this, objCustomCategoryOrigin, objCustomCategory6Origin, customCategory6);

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

            objSku = addSingleGroupObject(sku, "SKU", LM.selection, LM.barcode, nameSupplierArticleSku, nameBrandSupplierArticleSku, nameThemeSupplierArticleSku,
                     nameCategoryArticleSku, sidArticleSku, nameArticleSku, sidCustomCategory10Sku,
                     sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                     nameCountrySku, netWeightSku,
                     mainCompositionSku, additionalCompositionSku, quantityDirectInvoicedSku, quantityStockedSku, quantitySku, sumSku);
            LM.addObjectActions(this, objSku);

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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", LM.name);

            objCategory = addSingleGroupObject(category, "Номенклатурная группа", LM.name);

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle, nameSupplierArticle, nameBrandSupplierArticle, nameThemeSupplierArticle, nameCategoryArticle, nameArticle);
            LM.addObjectActions(this, objArticle);

            objSku = addSingleGroupObject(sku, "SKU", LM.selection, LM.barcode, nameSupplierArticleSku, nameBrandSupplierArticleSku, nameThemeSupplierArticleSku,
                     nameCategoryArticleSku, sidArticleSku, nameArticleSku,
                     sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                     nameCountrySku, netWeightSku,
                     mainCompositionSku, additionalCompositionSku);
            LM.addObjectActions(this, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            RegularFilterGroupEntity filterGroupSupplierSku = new RegularFilterGroupEntity(genID());
            filterGroupSupplierSku.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(supplierArticleSku, objSku), Compare.EQUALS, objSupplier),
                                  "Только текущего поставщика",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroupSupplierSku);

            RegularFilterGroupEntity filterGroupCategorySku = new RegularFilterGroupEntity(genID());
            filterGroupCategorySku.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(categoryArticleSku, objSku), Compare.EQUALS, objCategory),
                                  "Только текущей номенклатурной группы",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupCategorySku);

            RegularFilterGroupEntity filterGroupSupplierArticle = new RegularFilterGroupEntity(genID());
            filterGroupSupplierArticle.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, objSupplier),
                                  "Только текущего поставщика",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroupSupplierArticle);

            RegularFilterGroupEntity filterGroupCategoryArticle = new RegularFilterGroupEntity(genID());
            filterGroupCategoryArticle.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(categoryArticle, objArticle), Compare.EQUALS, objCategory),
                                  "Только текущей номенклатурной группы",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
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

            objSeller = addSingleGroupObject(seller, "Продавец", LM.name, LM.objectClassName);

            objContract = addSingleGroupObject(contract, "Договор", sidContract, LM.date, nameImporterContract, nameCurrencyContract);
            LM.addObjectActions(this, objContract);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(sellerContract, objContract), Compare.EQUALS, objSeller));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.get(getPropertyDraw(LM.objectClassName, objSeller)).caption = "Тип продавца";
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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", LM.name);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд", LM.name);

            treeSupplierBrand = addTreeGroupObject(objSupplier.groupTo, objBrand.groupTo);

            objPallet = addSingleGroupObject(pallet, "Паллета", LM.barcode);
            addPropertyDraw(quantityPalletBrandSupplier, objPallet, objBrand);

            objInvoice = addSingleGroupObject(directInvoice, "Инвойс (напрямую)", sidDocument);
            addPropertyDraw(quantityDocumentBrandSupplier, objInvoice, objBrand);
            addPropertyDraw(objInvoice, LM.date);

            objBox = addSingleGroupObject(freightBox, "Короб", LM.barcode);
            addPropertyDraw(quantityStockBrandSupplier, objBox, objBrand);

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle);
            addPropertyDraw(quantityStockArticle, objBox, objArticle);
            addPropertyDraw(objArticle, nameArticleSku, nameThemeSupplierArticle, nameCategoryArticleSku);

            objArticle2 = addSingleGroupObject(article, "Артикул", sidArticle);
            addPropertyDraw(quantityDocumentArticle, objInvoice, objArticle2);
            addPropertyDraw(objArticle2, LM.dumb1, nameArticleSku, nameThemeSupplierArticle, nameCategoryArticleSku);

            objSku = addSingleGroupObject(sku, "SKU", LM.barcode);
            addPropertyDraw(quantityStockSku, objBox, objSku);
            addPropertyDraw(objSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                     nameCountrySku, netWeightSku, mainCompositionSku, additionalCompositionSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            objSku2 = addSingleGroupObject(sku, "SKU", LM.barcode);
            addPropertyDraw(quantityDocumentSku, objInvoice, objSku2);
            addPropertyDraw(objSku2, LM.dumb1, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", LM.name);

            objInvoice = addSingleGroupObject(invoice, "Инвойс", LM.date, sidDocument, LM.objectClassName);

            objBox = addSingleGroupObject(supplierBox, "Короб из инвойса", sidSupplierBox);

            objSku = addSingleGroupObject(sku, "Товар в инвойсе", LM.barcode, sidArticleSku, nameBrandSupplierArticleSku, nameCategoryArticleSku,
                     sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);

            addPropertyDraw(quantityDocumentSku, objInvoice, objSku);
            addPropertyDraw(quantityInvoiceSku, objInvoice, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            objSku2 = addSingleGroupObject(sku, "Товар в коробе", LM.barcode, sidArticleSku, nameBrandSupplierArticleSku, nameCategoryArticleSku,
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

            design.get(getPropertyDraw(LM.objectClassName, objInvoice)).caption = "Тип инвойса";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";

            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 1;
            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 1;
            design.get(objBox.groupTo).grid.constraints.fillVertical = 1;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 3;
            design.get(objSku2.groupTo).grid.constraints.fillVertical = 3;

            design.addIntersection(design.getGroupObjectContainer(objInvoice.groupTo),
                                   design.getGroupObjectContainer(objBox.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSku.groupTo),
                                   design.getGroupObjectContainer(objSku2.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

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
            this(parent, sID, caption, false);
        }

        private FreightChangeFormEntity(NavigatorElement parent, String sID, String caption, boolean edit) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", LM.date, LM.objectClassName, nameRouteFreight, nameExporterFreight, nameFreightTypeFreight, grossWeightFreight, tonnageFreight, palletCountFreight, volumeFreight, palletNumberFreight);

            if (edit) {
                objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
                addActionsOnClose(addPropertyObject(executeChangeFreightClass, objFreight, (DataObject)freightChanged.getClassObject()));
            } else {
                FreightChangeFormEntity editFreightForm = new FreightChangeFormEntity(null, "freightChangeForm_edit", "Обработка фрахта", true);
                addPropertyDraw(
                        LM.addJProp("Обработать фрахт", LM.and(false, true),
                                LM.addMFAProp(null,
                                        "Обработать фрахт",
                                        editFreightForm,
                                        new ObjectEntity[]{editFreightForm.objFreight},
                                        new PropertyObjectEntity[0],
                                        new PropertyObjectEntity[0],
                                        false), 1,
                                LM.is(freightPriced), 1,
                                LM.is(freightChanged), 1),
                        objFreight
                ).forceViewType = ClassViewType.GRID;

                addPropertyDraw(
                        LM.addJProp("Отгрузить фрахт", LM.and(false, true),
                                executeChangeFreightClass, 1, 2,
                                LM.is(freightChanged), 1,
                                LM.is(freightShipped), 1),
                        objFreight,
                        (DataObject)freightShipped.getClassObject()
                ).forceViewType = ClassViewType.GRID;
            }

            objImporter = addSingleGroupObject(importer, "Импортёр", LM.name, addressSubject);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(invoiceOriginFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(invoiceFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(proformOriginFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(proformFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(annexInvoiceOriginFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(annexInvoiceFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(sbivkaFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(packingListFormImporterFreight, objImporter, objFreight);

            objArticle = addSingleGroupObject(article, "Артикул", LM.selection, sidArticle, nameBrandSupplierArticle, originalNameArticle, nameCategoryArticle, nameArticle,
                    sidCustomCategoryOriginArticle, nameCountryOfOriginArticle, mainCompositionOriginArticle, additionalCompositionOriginArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(quantityFreightArticle, objFreight, objArticle);

            objSku = addSingleGroupObject(sku, "SKU", LM.selection, LM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                     nameBrandSupplierArticleSku, nameArticleSku,
                     sidCustomCategoryOriginArticleSku, sidCustomCategory10Sku, nameCountrySku, netWeightSku,
                     mainCompositionOriginSku, translationMainCompositionSku, mainCompositionSku,
                     additionalCompositionOriginSku, translationAdditionalCompositionSku, additionalCompositionSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            addPropertyDraw(LM.addGCAProp(LM.actionGroup, "translationAllMainComposition", "Перевод составов", objSku.groupTo, translationMainCompositionSku, LM.actionTrue), objSku).forceViewType = ClassViewType.PANEL;
            addPropertyDraw(LM.addGCAProp(LM.actionGroup, "translationAllAdditionalComposition", "Перевод доп. составов", objSku.groupTo, translationAdditionalCompositionSku, LM.actionTrue), objSku).forceViewType = ClassViewType.PANEL;

            objSkuFreight = addSingleGroupObject(sku, "Позиции фрахта", LM.selection, LM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem,
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

            design.get(getPropertyDraw(LM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(LM.objectClassName, objFreight)).caption = "Статус фрахта";

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

            addPropertyDraw(objFreight, LM.date, LM.objectClassName, nameCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight);
                addPropertyDraw(objFreight, addressExporterFreight);
                addPropertyDraw(objImporter, LM.name);
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
            objCountry = new ObjectEntity(7, "country", LM.country, "Страна");
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
                addPropertyDraw(objComposition, LM.objectValue);
            }

            addPropertyDraw(objCountry, LM.sidCountry);
            if (!translate)
                addPropertyDraw(objCountry, nameOriginCountry);

            if (translate)
                addPropertyDraw(objCountry, LM.name);

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
                annexInvoiceOriginFormImporterFreight = LM.addFAProp("Приложение", this, objImporter, objFreight);

            if (translate)
                annexInvoiceFormImporterFreight = LM.addFAProp("Приложение (перевод)", this, objImporter, objFreight);
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

            addPropertyDraw(objFreight, LM.date, LM.objectClassName, nameCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight);
                addPropertyDraw(objFreight, addressExporterFreight);
                addPropertyDraw(objImporter, LM.name);
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
            objCountry = new ObjectEntity(7, "country", LM.country, "Страна");
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
                addPropertyDraw(objComposition, LM.objectValue);
            }

            addPropertyDraw(objCountry, LM.sidCountry);
            if (!translate)
                addPropertyDraw(objCountry, nameOriginCountry);

            if (translate)
                addPropertyDraw(objCountry, LM.name);

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
                invoiceOriginFormImporterFreight = LM.addFAProp("Инвойс", this, objImporter, objFreight);

            if (translate)
                invoiceFormImporterFreight = LM.addFAProp("Инвойс (перевод)", this, objImporter, objFreight);
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

            addPropertyDraw(objFreight, LM.date, LM.objectClassName, nameCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight);
                addPropertyDraw(objFreight, addressExporterFreight);
                addPropertyDraw(objImporter, LM.name);
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
            objCountry = new ObjectEntity(7, "country", LM.country, "Страна");
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
                addPropertyDraw(objComposition, LM.objectValue);
            }

            addPropertyDraw(objCountry, LM.sidCountry);
            if (!translate)
                addPropertyDraw(objCountry, nameOriginCountry);

            if (translate)
                addPropertyDraw(objCountry, LM.name);

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
                proformOriginFormImporterFreight = LM.addFAProp("Инвойс-проформа", this, objImporter, objFreight);

            if (translate)
                proformFormImporterFreight = LM.addFAProp("Инвойс-проформа (перевод)", this, objImporter, objFreight);
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

            addPropertyDraw(objFreight, LM.date, LM.objectClassName, nameOriginExporterFreight, addressOriginExporterFreight, nameCurrencyFreight);
            addPropertyDraw(objImporter, nameOrigin, addressOriginSubject, contractImporter);
            addPropertyDraw(objImporter, objFreight, quantityImporterFreight, netWeightImporterFreight, grossWeightImporterFreight, sumInOutImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objFreightBox = addSingleGroupObject(4, "freightBox", freightBox, "Короб", LM.barcode);
            addPropertyDraw(objImporter, objFreightBox, netWeightImporterFreightUnit, grossWeightImporterFreightUnit, quantityImporterStock);

            objArticle = addSingleGroupObject(5, "article", article, "Артикул", sidArticle, nameBrandSupplierArticle, nameArticle);
            addPropertyDraw(quantityImporterStockArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(netWeightImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(grossWeightImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(sumImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterStockArticle, objImporter, objFreightBox, objArticle)));

            packingListFormImporterFreight = LM.addFAProp("Упаковочный лист", this, objImporter, objFreight);
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

            addPropertyDraw(objFreight, LM.date, nameCurrencyFreight);
            addPropertyDraw(objImporter, contractImporter);
            addPropertyDraw(objImporter, objFreight, netWeightImporterFreight, grossWeightImporterFreight, sumSbivkaImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objCategory = addSingleGroupObject(4, "category", customCategory6, "ТН ВЭД");

            addPropertyDraw(objCategory, sidCustomCategory6, nameCustomCategory);
            addPropertyDraw(objImporter, objFreight, objCategory, quantityImporterFreightCustomCategory6, netWeightImporterFreightCustomCategory6, grossWeightImporterFreightCustomCategory6, sumImporterFreightCustomCategory6);
            addPropertyDraw(quantitySbivkaImporterFreight, objImporter, objFreight);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantitySbivkaImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightCustomCategory6, objImporter, objFreight, objCategory)));

            sbivkaFormImporterFreight = LM.addFAProp("Сбивка", this, objImporter, objFreight);
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
            objSupplier = addSingleGroupObject(supplier, "Поставщик", LM.name, nameBrandSupplierSupplier, nameCurrencySupplier);
            LM.addObjectActions(this, objSupplier);

            objColor = addSingleGroupObject(colorSupplier, "Цвет", sidColorSupplier, LM.name);
            LM.addObjectActions(this, objColor);

            objSize = addSingleGroupObject(sizeSupplier, "Размер", sidSizeSupplier, nameCommonSizeSizeSupplier);
            LM.addObjectActions(this, objSize);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд", sidBrandSupplier, LM.name);
            LM.addObjectActions(this, objBrand);

            objTheme = addSingleGroupObject(themeSupplier, "Тема", LM.name);
            LM.addObjectActions(this, objTheme);

            objCountry = addSingleGroupObject(countrySupplier, "Страна", LM.name, nameCountryCountrySupplier);
            LM.addObjectActions(this, objCountry);

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

            objTransfer = addSingleGroupObject(transfer, "Внутреннее перемещение", LM.objectValue, barcodeStockFromTransfer, barcodeStockToTransfer);
            LM.addObjectActions(this, objTransfer);

            objSku = addSingleGroupObject(sku, "SKU", LM.barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
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

            objFreight = addSingleGroupObject(freight, "Фрахт", LM.date, LM.objectClassName, nameRouteFreight, nameFreightTypeFreight, tonnageFreight, palletCountFreight, volumeFreight, nameCurrencyFreight, sumInFreight, sumMarkupInFreight, sumInOutFreight, palletNumberFreight);

            addPropertyDraw(sumInCurrentYear);
            addPropertyDraw(sumInOutCurrentYear);
            addPropertyDraw(balanceSumCurrentYear);

            if (edit) {
                objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
                addActionsOnClose(addPropertyObject(executeChangeFreightClass, objFreight, (DataObject)freightPriced.getClassObject()));
            } else {
                FreightInvoiceFormEntity editFreightForm = new FreightInvoiceFormEntity(null, "freightInvoiceForm_edit", "Расценка фрахта", true);
                addPropertyDraw(
                        LM.addJProp("Расценить фрахт", LM.and(false, true),
                                LM.addMFAProp(null,
                                        "Расценить фрахт",
                                        editFreightForm,
                                        new ObjectEntity[]{editFreightForm.objFreight},
                                        new PropertyObjectEntity[0],
                                        new PropertyObjectEntity[0],
                                        false), 1,
                                LM.is(freightComplete), 1,
                                LM.is(freightPriced), 1),
                        objFreight
                ).forceViewType = ClassViewType.GRID;
            }

            objImporter = addSingleGroupObject(importer, "Импортер", LM.name);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumMarkupInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInOutImporterFreight, objImporter, objFreight);

            objBrandSupplier = addSingleGroupObject(brandSupplier, "Бренд", LM.name, nameSupplierBrandSupplier);

            addPropertyDraw(quantityImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);
            addPropertyDraw(markupPercentImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);

            objSku = addSingleGroupObject(sku, "SKU", LM.barcode, sidArticleSku, sidSizeSupplierItem,
                    nameBrandSupplierArticleSku, nameCategoryArticleSku, sidCustomCategoryOriginArticleSku,
                    nameCountrySku, netWeightSku, mainCompositionOriginSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

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

            design.get(getPropertyDraw(LM.date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(LM.objectClassName, objFreight)).caption = "Статус фрахта";

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

            objSupplier = addSingleGroupObject(supplier, "Поставщик", LM.name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", LM.objectValue);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Артикул", LM.objectValue);
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

    public class PricatFormEntity extends FormEntity {
        ObjectEntity objSupplier;

        public PricatFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            objSupplier = addSingleGroupObject(supplier, LM.name, importPricatSupplier, hugoBossImportPricat);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objPricat = addSingleGroupObject(pricat);
            addPropertyDraw(objPricat, LM.baseGroup);
            LM.addObjectActions(this, objPricat);

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
            super(LM.genSID(), "Поиск маршрута", new ValueClass[] {shipment, sku, route});

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
