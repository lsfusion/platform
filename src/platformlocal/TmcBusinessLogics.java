package platformlocal;

import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class TmcBusinessLogics extends BusinessLogics<TmcBusinessLogics>{

    public TmcBusinessLogics() {
        super();
    }

    public TmcBusinessLogics(int TestType) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(TestType);
    }

    Class article;
    Class articleGroup;

    Class store;

    Class document;
    Class quantityDocument;
    Class incomeDocument;
    Class outcomeDocument;

    Class extIncomeDocument;
    Class extIncomeDetail;

    Class intraDocument;
    Class extOutcomeDocument;
    Class exchangeDocument;

    void InitClasses() {

        article = new ObjectClass(4, "Товар", objectClass);
        articleGroup = new ObjectClass(5, "Группа товаров", objectClass);

        store = new ObjectClass(6, "Склад", objectClass);

        document = new ObjectClass(7, "Документ", objectClass);
        quantityDocument = new ObjectClass(8, "Товарный документ", document);
        incomeDocument = new ObjectClass(9, "Приходный документ", quantityDocument);
        outcomeDocument = new ObjectClass(10, "Расходный документ", quantityDocument);

        extIncomeDocument = new ObjectClass(11, "Внешний приход", incomeDocument);
        extIncomeDetail = new ObjectClass(12, "Внешний приход (строки)", objectClass);

        intraDocument = new ObjectClass(13, "Внутреннее перемещение", incomeDocument, outcomeDocument);
        extOutcomeDocument = new ObjectClass(14, "Внешний расход", outcomeDocument);
        exchangeDocument = new ObjectClass(15, "Пересорт", incomeDocument, outcomeDocument);


    }

    PropertyGroup baseGroup, artclGroup, artgrGroup, storeGroup, quantGroup;

    LDP name;
    LDP artGroup;
    LDP docDate, docStore;

    LJP artGroupName;
    LJP docStoreName;
    LJP intraStoreName;
    LJP extIncDetailArticleName;

    LDP extIncDetailDocument, extIncDetailArticle, extIncDetailQuantity;
    LDP extIncQuantity;
    LDP intraQuantity, intraStore;
    LDP extOutQuantity;
    LDP exchangeQuantity;
    LGP exchIncQuantity, exchOutQuantity;

    LJP incQuantity, outQuantity;
    LJP incStore;
    LGP incStoreQuantity, outStoreQuantity;
    LJP dltStoreQuantity;

    void InitProperties() {

        // -------------------------- Group Properties --------------------- //

        baseGroup = new PropertyGroup("Атрибуты");
        artclGroup = new PropertyGroup("Товар");
        artgrGroup = new PropertyGroup("Группа товаров");
        storeGroup = new PropertyGroup("Склад");
        quantGroup = new PropertyGroup("Количество");

        // -------------------------- Data Properties ---------------------- //

        name = AddDProp(baseGroup, "Имя", Class.stringClass, objectClass);

        artGroup = AddDProp(artgrGroup, "Гр. тов.", articleGroup, article);

        docDate = AddDProp(baseGroup, "Дата", Class.dateClass, document);
        docStore = AddDProp(storeGroup, "Склад", store, document);

        intraStore = AddDProp(storeGroup, "Склад назн.", store, intraDocument);

        extIncDetailDocument = AddDProp(null, "Документ", extIncomeDocument, extIncomeDetail);
        extIncDetailArticle = AddDProp(artclGroup, "Товар", article, extIncomeDetail);
        
        // -------------------------- Relation Properties ------------------ //

        artGroupName = AddJProp(artgrGroup, "Имя гр. тов.", name, 1, artGroup, 1);
        docStoreName = AddJProp(storeGroup, "Имя склада", name, 1, docStore, 1);
        intraStoreName = AddJProp(storeGroup, "Имя склада (назн.)", name, 1, intraStore, 1);

        extIncDetailArticleName = AddJProp(artclGroup, "Имя товара", name, 1, extIncDetailArticle, 1);

        // -------------------------- Движение товара по количествам ---------------------- //

        extIncDetailQuantity = AddDProp(quantGroup, "Кол-во", Class.doubleClass, extIncomeDetail);

        extIncQuantity = AddDProp(quantGroup, "Кол-во прих.", Class.doubleClass, extIncomeDocument, article);

        intraQuantity = AddDProp(quantGroup, "Кол-во внутр.", Class.doubleClass, intraDocument, article);

        extOutQuantity = AddDProp(quantGroup, "Кол-во расх.", Class.doubleClass, extOutcomeDocument, article);

        exchangeQuantity = AddDProp(quantGroup, "Кол-во перес.", Class.doubleClass, exchangeDocument, article, article);

        exchIncQuantity = AddGProp("Прих. перес.", exchangeQuantity, true, 1, 3);
        exchOutQuantity = AddGProp("Расх. перес.", exchangeQuantity, true, 1, 2); 

        LP docIncQuantity = AddCProp("абст. кол-во",null,Class.doubleClass, incomeDocument,article);
        incQuantity = AddUProp("Кол-во прих.", 2, 2, 1, docIncQuantity, 1, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchIncQuantity, 1, 2);
        LP docOutQuantity = AddCProp("абст. кол-во",null,Class.doubleClass, outcomeDocument,article);
        outQuantity = AddUProp("Кол-во расх.", 2, 2, 1, docOutQuantity, 1, 2, 1, extOutQuantity, 1, 2, 1, intraQuantity, 1, 2, 1, exchOutQuantity, 1, 2);

        incStore = AddUProp("Склад прих.", 2, 1, 1, docStore, 1, 1, intraStore, 1);

        incStoreQuantity = AddGProp("Прих. на скл.", incQuantity, true, incStore, 1, 2);
        outStoreQuantity = AddGProp("Расх. со скл.", outQuantity, true, docStore, 1, 2);

        dltStoreQuantity = AddUProp("Ост. на скл.", 1, 2, 1, incStoreQuantity, 1, 2, -1, outStoreQuantity, 1, 2);
//        OstArtStore = AddUProp("остаток по складу",1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);

    }

    void InitConstraints() {
    }

    void InitPersistents() {
    }

    void InitTables() {

        TableImplement Include;

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,article));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,store));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,articleGroup));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,article));
        Include.add(new DataPropertyInterface(0,document));
        TableFactory.IncludeIntoGraph(Include);

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,article));
        Include.add(new DataPropertyInterface(0,store));
        TableFactory.IncludeIntoGraph(Include);

    }

    void InitIndexes() {
    }

    void InitNavigators() {

        createDefaultClassForms(objectClass, baseElement);

        NavigatorForm extIncDetailForm = new ExtIncDetailNavigatorForm(10, "Внешний приход");
        baseElement.addChild(extIncDetailForm);

        NavigatorForm extIncForm = new ExtIncNavigatorForm(15, "Внешний приход по товарам");
        extIncDetailForm.addChild(extIncForm);

        NavigatorForm intraForm = new IntraNavigatorForm(20, "Внутреннее перемещение");
        baseElement.addChild(intraForm);

        NavigatorForm extOutForm = new ExtOutNavigatorForm(30, "Внешний расход");
        baseElement.addChild(extOutForm);

        NavigatorForm exchangeForm = new ExchangeNavigatorForm(40, "Пересорт");
        baseElement.addChild(exchangeForm);

        NavigatorForm storeArticleForm = new StoreArticleNavigatorForm(50, "Товары по складам");
        baseElement.addChild(storeArticleForm);
    }

    private class ExtIncDetailNavigatorForm extends NavigatorForm {

        public ExtIncDetailNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjDetail = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extIncomeDocument, "Документ", gobjDoc);
            ObjectImplement objDetail = new ObjectImplement(IDShift(1), extIncomeDetail, "Строка", gobjDetail);

            addGroup(gobjDoc);
            addGroup(gobjDetail);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, artclGroup, objDetail);
            addPropertyView(this, quantGroup, objDetail);

            PropertyObjectImplement detDocument = addPropertyObjectImplement(extIncDetailDocument, objDetail);
            addFixedFilter(new Filter(detDocument, FieldExprCompareWhere.EQUALS, new ObjectValueLink(objDoc)));
        }
    }

    private class ExtIncNavigatorForm extends NavigatorForm {

        public ExtIncNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extIncomeDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, extIncQuantity, objDoc, objArt);
        }
    }

    private class IntraNavigatorForm extends NavigatorForm {

        public IntraNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), intraDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, intraQuantity, objDoc, objArt);
        }
    }

    private class ExtOutNavigatorForm extends NavigatorForm {

        public ExtOutNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), extOutcomeDocument, "Документ", gobjDoc);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjDoc);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArt);
            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, extOutQuantity, objDoc, objArt);
        }
    }

    private class ExchangeNavigatorForm extends NavigatorForm {

        public ExchangeNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjDoc = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArtFrom = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArtTo = new GroupObjectImplement(IDShift(1));

            ObjectImplement objDoc = new ObjectImplement(IDShift(1), exchangeDocument, "Документ", gobjDoc);
            ObjectImplement objArtFrom = new ObjectImplement(IDShift(1), article, "Товар (с)", gobjArtFrom);
            ObjectImplement objArtTo = new ObjectImplement(IDShift(1), article, "Товар (на)", gobjArtTo);

            addGroup(gobjDoc);
            addGroup(gobjArtFrom);
            addGroup(gobjArtTo);

            addPropertyView(this, baseGroup, objDoc);
            addPropertyView(this, storeGroup, objDoc);
            addPropertyView(this, baseGroup, objArtFrom);
            addPropertyView(this, artgrGroup, objArtFrom);
            addPropertyView(this, baseGroup, objArtTo);
            addPropertyView(this, artgrGroup, objArtTo);
            addPropertyView(this, exchIncQuantity, objDoc, objArtFrom);
            addPropertyView(this, exchOutQuantity, objDoc, objArtFrom);
            addPropertyView(this, exchangeQuantity, objDoc, objArtFrom, objArtTo);
        }
    }

    private class StoreArticleNavigatorForm extends NavigatorForm {

        public StoreArticleNavigatorForm(int ID, String caption) {
            super(ID, caption);

            GroupObjectImplement gobjStore = new GroupObjectImplement(IDShift(1));
            GroupObjectImplement gobjArt = new GroupObjectImplement(IDShift(1));

            ObjectImplement objStore = new ObjectImplement(IDShift(1), store, "Склад", gobjStore);
            ObjectImplement objArt = new ObjectImplement(IDShift(1), article, "Товар", gobjArt);

            addGroup(gobjStore);
            addGroup(gobjArt);

            addPropertyView(this, baseGroup, objStore);
            addPropertyView(this, baseGroup, objArt);
            addPropertyView(this, artgrGroup, objArt);
            addPropertyView(this, objStore, objArt);
        }
    }

    // ------------------------------------- Временные методы --------------------------- //

    void fillData(DataAdapter Adapter) throws SQLException {

        Map<Class,Integer> ClassQuantity = new HashMap();
        ClassQuantity.put(article,20);
        ClassQuantity.put(articleGroup,3);
        ClassQuantity.put(store,3);
        ClassQuantity.put(extIncomeDocument,20);
        ClassQuantity.put(extIncomeDetail,50);
        ClassQuantity.put(intraDocument,15);
        ClassQuantity.put(extOutcomeDocument,50);
        ClassQuantity.put(exchangeDocument,10);

        Map<DataProperty, Set<DataPropertyInterface>> PropNotNulls = new HashMap();
        name.putNotNulls(PropNotNulls,0);
        artGroup.putNotNulls(PropNotNulls,0);
        docDate.putNotNulls(PropNotNulls,0);
        docStore.putNotNulls(PropNotNulls,0);
        intraStore.putNotNulls(PropNotNulls,0);
        extIncDetailDocument.putNotNulls(PropNotNulls,0);
        extIncDetailArticle.putNotNulls(PropNotNulls,0);
        extIncDetailQuantity.putNotNulls(PropNotNulls,0);

        Map<DataProperty,Integer> PropQuantity = new HashMap();

        PropQuantity.put((DataProperty)extIncQuantity.Property,10);
        PropQuantity.put((DataProperty)intraQuantity.Property,15);
        PropQuantity.put((DataProperty)extOutQuantity.Property,5);
        PropQuantity.put((DataProperty)exchangeQuantity.Property,14);

        autoFillDB(Adapter,ClassQuantity,PropQuantity,PropNotNulls);
    }

}


