package roman;

import net.sf.jasperreports.engine.JRException;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.*;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class RomanBusinessLogics extends BusinessLogics<RomanBusinessLogics> {
    private AbstractCustomClass article;
    private ConcreteCustomClass articleComposite;
    private ConcreteCustomClass articleSingle;
    private ConcreteCustomClass item;
    private ConcreteCustomClass sku;
    private LP multiplyDouble2;
    private LP sidArticle;
    private LP articleItem;
    private LP sidArticleItem;
    private ConcreteCustomClass order;
    private ConcreteCustomClass invoice;
    private ConcreteCustomClass shipment;
    private ConcreteCustomClass supplier;
    private AbstractCustomClass document;
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
    private LP currencySupplier;  //
    private LP nameCurrencySupplier;
    private LP currencyOrder;
    private LP nameCurrencyOrder;
    private LP shopOrder;
    private LP nameShopOrder;
    private LP sidColorSupplier;
    private LP sidColorSupplierItem;
    private LP quantityDocumentSku;
    private LP quantityDocumentArticleComposite;
    private LP quantityDocumentArticleCompositeColor;
    private LP quantityDocumentArticleCompositeSize;
    private LP quantityDocumentArticleCompositeColorSize;
    private LP originalNameArticle;
    private ConcreteCustomClass country;
    private LP countryOfOriginArticle;
    private LP nameCountryOfOriginArticle;
    private LP articleSIDSupplier;
    private LP seekArticleSIDSupplier;
    private LP addArticleCompositeSIDSupplier;
    private LP numberDocumentArticle;
    private LP incrementNumberDocumentArticle;
    private LP articleSIDDocument;
    private LP incrementNumberDocumentSID;
    private LP addNEArticleCompositeSIDSupplier;
    private LP numberDocumentSIDArticle;

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

        sku = addConcreteClass("sku", "SKU", barcodeObject);

        article = addAbstractClass("article", "Артикул", baseClass);
        articleComposite = addConcreteClass("articleComposite", "Артикул (составной)", article);
        articleSingle = addConcreteClass("articleSingle", "Артикул (простой)", sku, article);

        item = addConcreteClass("item", "Товар", sku);

        document = addAbstractClass("document", "Документ", transaction);

        order = addConcreteClass("order", "Заказ", document);

        invoice = addConcreteClass("invoice", "Инвойс", document);
        shipment = addConcreteClass("shipment", "Поставка", document);

        supplier = addConcreteClass("supplier", "Поставщик", baseClass.named);

        colorSupplier = addConcreteClass("colorSupplier", "Цвет поставщика", baseClass.named); 
        sizeSupplier = addConcreteClass("sizeSupplier", "Размер поставщика", baseClass.named);
    }

    @Override
    protected void initProperties() {

        multiplyDouble2 = addMFProp(DoubleClass.instance, 2);

        currencySupplier = addDProp(idGroup, "currencySupplier", "Валюта (ИД)", currency, supplier);
        nameCurrencySupplier = addJProp(baseGroup, "nameCurrencySupplier", "Валюта", name, currencySupplier, 1);

        sidColorSupplier = addDProp(baseGroup, "sidColorSupplier", "Код", StringClass.get(50), colorSupplier);

        supplierColorSupplier = addDProp(idGroup, "supplierColorSupplier", "Поставщик (ИД)", supplier, colorSupplier);
        nameSupplierColorSupplier = addJProp(baseGroup, "nameSupplierColorSupplier", "Поставщик", name, supplierColorSupplier, 1);

        supplierSizeSupplier = addDProp(idGroup, "supplierSizeSupplier", "Поставщик (ИД)", supplier, sizeSupplier);
        nameSupplierSizeSupplier = addJProp(baseGroup, "nameSupplierSizeSupplier", "Поставщик", name, supplierSizeSupplier, 1);

        supplierDocument = addDProp(idGroup, "supplierDocument", "Поставщик (ИД)", supplier, document);
        nameSupplierDocument = addJProp(baseGroup, "nameSupplierDocument", "Поставщик", name, supplierDocument, 1);

        // Order
        currencyOrder = addDProp(idGroup, "currencyOrder", "Валюта (ИД)", currency, order);
        nameCurrencyOrder = addJProp(baseGroup, "nameCurrencyOrder", "Валюта", name, currencyOrder, 1);

        shopOrder = addDProp(idGroup, "shopOrder", "Магазин (ИД)", store, order);
        nameShopOrder = addJProp(baseGroup, "nameShopOrder", "Магазин", name, shopOrder, 1);

        // Article

        sidArticle = addDProp(baseGroup, "sidArticle", "Код", StringClass.get(50), article);
        originalNameArticle = addDProp(baseGroup, "originalNameArticle", "Имя производителя", StringClass.get(50), article);

        countryOfOriginArticle = addDProp(idGroup, "countryOfOriginArticle", "Страна происхождения (ИД)", country, article);
        nameCountryOfOriginArticle = addJProp(baseGroup, "nameCountryOfOriginArticle", "Страна происхождения", name, countryOfOriginArticle, 1);

        supplierArticle = addDProp(idGroup, "supplierArticle", "Поставщик (ИД)", supplier, article);
        nameSupplierArticle = addJProp(baseGroup, "nameSupplierArticle", "Поставщик", name, supplierArticle, 1);

        articleSIDSupplier = addCGProp(idGroup, "articleSIDSupplier", "Артикул (ИД)", object(article), sidArticle, sidArticle, 1, supplierArticle, 1);

        seekArticleSIDSupplier = addJProp(true, "Поиск артикула", addSAProp(null), articleSIDSupplier, 1, 2);
        addArticleCompositeSIDSupplier = addJProp(true, "Ввод артикула", addAAProp(articleComposite, sidArticle, supplierArticle), 1, 2);
        addNEArticleCompositeSIDSupplier = addJProp(true, "Ввод артикула (НС)", andNot1, addArticleCompositeSIDSupplier, 1, 2, articleSIDSupplier, 1, 2);

        // Item

        articleItem = addDProp(idGroup, "articleItem", "Артикул (ИД)", articleComposite, item);
        sidArticleItem = addJProp(baseGroup, "sidArticleItem", "Артикул", sidArticle, articleItem, 1);

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

        sidDocument = addDProp(baseGroup, "sidDocument", "Код", StringClass.get(50), document);

        // кол-во заказа
        quantityDocumentSku = addDProp(baseGroup, "quantityDocumentSku", "Кол-во", DoubleClass.instance, document, sku);

        // заказ по артикулам

        articleSIDDocument = addJProp(idGroup, "articleSIDDocument", "Артикул (ИД)", articleSIDSupplier, 1, supplierDocument, 2);

        numberDocumentArticle = addDProp(baseGroup, "numberDocumentArticle", "Номер", IntegerClass.instance, document, article);
        numberDocumentSIDArticle = addJProp(numberDocumentArticle, 1, articleSIDDocument, 2, 1);

        incrementNumberDocumentSID = addJProp(true, "Добавить строку", andNot1,
                                                  addJProp(true, addIAProp(numberDocumentArticle, 1),
                                                  1, articleSIDDocument, 2, 1), 1, 2,
                                                  numberDocumentSIDArticle, 1, 2);

        quantityDocumentArticleComposite = addDGProp(baseGroup, "quantityDocumentArticleComposite", "Кол-во",
                1, false,
                quantityDocumentSku, 1, articleItem, 2,
                addCProp(DoubleClass.instance, Double.MAX_VALUE, document, sku), 1, 2,
                2);

        priceDocumentArticle = addDProp(baseGroup, "priceDocumentArticle", "Цена", DoubleClass.instance, document, article);

        sumDocumentArticle = addJProp(baseGroup, "Сумма", multiplyDouble2, quantityDocumentArticleComposite, 1, 2, priceDocumentArticle, 1, 2);
        sumDocument = addSGProp(baseGroup, "sumDocument", "Сумма документа", sumDocumentArticle, 1);

        quantityDocumentArticleCompositeColor = addSGProp(baseGroup, "quantityDocumentArticleCompositeColor", "Кол-во", quantityDocumentSku, 1, articleItem, 2, colorSupplierItem, 2);
        quantityDocumentArticleCompositeSize = addSGProp(baseGroup, "quantityDocumentArticleCompositeSize", "Кол-во", quantityDocumentSku, 1, articleItem, 2, sizeSupplierItem, 2);

        quantityDocumentArticleCompositeColorSize = addDGProp(baseGroup, "quantityDocumentArticleCompositeColorSize", "Кол-во",
                1, false, // кол-во объектов для порядка и ascending/descending
                quantityDocumentSku, 1, articleItem, 2, colorSupplierItem, 2, sizeSupplierItem, 2,
                addCProp(DoubleClass.instance, Double.MAX_VALUE, document, sku), 1, 2, // ограничение (максимально-возможное число)
                2); // порядок
    }

    @Override
    protected void initTables() {
    }

    @Override
    protected void initIndexes() {
    }

    @Override
    protected void initNavigators() throws JRException, FileNotFoundException {
        addFormEntity(new OrderFormEntity(baseElement, 10, "Заказы"));
        addFormEntity(new InvoiceFormEntity(baseElement, 20, "Инвойсы"));
        addFormEntity(new ShipmentFormEntity(baseElement, 30, "Поставки"));
    }


    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
        admin.addSecurityPolicy(permitAllPolicy);
    }

    private class OrderFormEntity extends FormEntity<RomanBusinessLogics> {
        private ObjectEntity objSIDArticle;
        private ObjectEntity objArticleComposite;
        private ObjectEntity objItem;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objColorSupplier;
        private ObjectEntity objOrder;
        private ObjectEntity objSupplier;

        private OrderFormEntity(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, nameCurrencySupplier);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objOrder = addSingleGroupObject(order, "Заказ", date, sidDocument, nameCurrencyOrder, sumDocument, nameShopOrder);
            addObjectActions(this, objOrder);

            objSIDArticle = addSingleGroupObject(StringClass.get(50), "Ввод артикула", objectValue);
            objSIDArticle.groupTo.setSingleClassView(ClassViewType.PANEL);

            addAutoAction(objSIDArticle, addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticle, objSupplier));
            addAutoAction(objSIDArticle, addPropertyObject(incrementNumberDocumentSID, objOrder, objSIDArticle));
            addAutoAction(objSIDArticle, addPropertyObject(seekArticleSIDSupplier, objSIDArticle, objSupplier));

            objArticleComposite = addSingleGroupObject(articleComposite, "Артикул");
            addPropertyDraw(numberDocumentArticle, objOrder, objArticleComposite);
            addPropertyDraw(objArticleComposite, sidArticle, originalNameArticle, nameCountryOfOriginArticle);
            addPropertyDraw(quantityDocumentArticleComposite, objOrder, objArticleComposite);
            addPropertyDraw(priceDocumentArticle, objOrder, objArticleComposite);
            addPropertyDraw(sumDocumentArticle, objOrder, objArticleComposite);
            addObjectActions(this, objArticleComposite);

            objItem = addSingleGroupObject(item, "Товар", barcode, sidColorSupplierItem, nameColorSupplierItem, nameSizeSupplierItem);
            addObjectActions(this, objItem);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", selection, name);
            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", sidColorSupplier, name);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityDocumentArticleCompositeColorSize, objOrder, objArticleComposite, objColorSupplier, objSizeSupplier);
            quantityColumn.columnGroupObjects.add(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(name, objSizeSupplier);

            addPropertyDraw(quantityDocumentSku, objOrder, objItem);
            addPropertyDraw(quantityDocumentArticleCompositeColor, objOrder, objArticleComposite, objColorSupplier);
            addPropertyDraw(quantityDocumentArticleCompositeSize, objOrder, objArticleComposite, objSizeSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticleComposite), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleItem, objItem), Compare.EQUALS, objArticleComposite));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(numberDocumentArticle, objOrder, objArticleComposite)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(quantityDocumentArticleCompositeColor, objOrder, objArticleComposite, objColorSupplier)),
                                  "Заказано",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            design.setReadOnly(objSupplier, true);

            design.defaultOrders.put(design.get(getPropertyDraw(numberDocumentArticle)), true);

            design.get(getPropertyDraw(objectValue, objSIDArticle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);

            design.get(objOrder.groupTo).grid.constraints.fillVertical = 0.5;
            design.get(objArticleComposite.groupTo).grid.constraints.fillHorizontal = 4;
            design.get(objItem.groupTo).grid.constraints.fillHorizontal = 3;

            design.addIntersection(design.getGroupObjectContainer(objArticleComposite.groupTo),
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
        private InvoiceFormEntity(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);
        }
    }

    private class ShipmentFormEntity extends FormEntity<RomanBusinessLogics> {
        private ShipmentFormEntity(NavigatorElement parent, int iID, String caption) {
            super(parent, iID, caption);
        }
    }
}
