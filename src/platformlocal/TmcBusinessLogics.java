package platformlocal;

import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class TmcBusinessLogics extends BusinessLogics<TmcBusinessLogics>{

    Class article;
    Class articleGroup;

    Class store;

    Class document;
    Class quantityDocument;
    Class incomeDocument;
    Class outcomeDocument;

    Class extIncomeDocument;
    Class intraDocument;
    Class extOutcomeDocument;

    void InitClasses() {

        article = new ObjectClass(4, "Товар", objectClass);
        articleGroup = new ObjectClass(5, "Группа товаров", objectClass);

        store = new ObjectClass(6, "Склад", objectClass);

        document = new ObjectClass(7, "Документ", objectClass);
        quantityDocument = new ObjectClass(8, "Товарный документ", document);
        incomeDocument = new ObjectClass(9, "Приходный документ", quantityDocument);
        outcomeDocument = new ObjectClass(10, "Расходный документ", quantityDocument);

        extIncomeDocument = new ObjectClass(11, "Внешний приход", incomeDocument);
        intraDocument = new ObjectClass(12, "Внутреннее перемещение", incomeDocument, outcomeDocument);
        extOutcomeDocument = new ObjectClass(13, "Внешний расход", outcomeDocument);
    }

    PropertyGroup baseGroup;

    LDP name;
    LDP artGroup;
    LDP docDate, docStore;

    LDP extIncQuantity;
    LDP intraQuantity, intraStore;
    LDP extOutQuantity;
    LJP incQuantity, outQuantity;
    LGP incStoreQuantity, outStoreQuantity;
    LJP dltStoreQuantity;

    void InitProperties() {

        // -------------------------- Group Properties --------------------- //

        baseGroup = new PropertyGroup("Атрибуты");

        // -------------------------- Data Properties ---------------------- //

        name = AddDProp("Имя", Class.stringClass, objectClass);

        artGroup = AddDProp("Гр. тов.", articleGroup, article);

        docDate = AddDProp("Дата", Class.dateClass, document);
        docStore = AddDProp("Склад", store, document);

        // -------------------------- Движение товара по количествам ---------------------- //

        extIncQuantity = AddDProp("Кол-во прих.", Class.doubleClass, extIncomeDocument, article);

        intraQuantity = AddDProp("Кол-во внутр.", Class.doubleClass, intraDocument, article);
        intraStore = AddDProp("Склад назн.", store, intraDocument);

        extOutQuantity = AddDProp("Кол-во расх.", Class.doubleClass, extOutcomeDocument, article);

        LP docIncQuantity = AddCProp("абст. кол-во",null,Class.doubleClass, incomeDocument,article);
        incQuantity = AddUProp("Кол-во прих.", 2, 2, 1, docIncQuantity, 1, 2, 1, extIncQuantity, 1, 2, 1, intraQuantity, 1, 2);
        LP docOutQuantity = AddCProp("абст. кол-во",null,Class.doubleClass, outcomeDocument,article);
        outQuantity = AddUProp("Кол-во расх.", 2, 2, 1, docOutQuantity, 1, 2, 1, extOutQuantity, 1, 2, 1, intraQuantity, 1, 2);

        incStoreQuantity = AddGProp("Прих. на скл.", incQuantity, true, docStore, 1, 2);
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

        NavigatorForm extIncForm = new ExtIncNavigatorForm(1, "Внешний приход");
        baseElement.addChild(extIncForm);

        NavigatorForm intraForm = new IntraNavigatorForm(2, "Внутреннее перемещение");
        baseElement.addChild(intraForm);

        NavigatorForm extOutForm = new ExtOutNavigatorForm(3, "Внешний расход");
        baseElement.addChild(extOutForm);

        NavigatorForm storeArticleForm = new StoreArticleNavigatorForm(4, "Товары по складам");
        baseElement.addChild(storeArticleForm);
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

            addPropertyView(this, objDoc);
            addPropertyView(this, objArt);
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

            addPropertyView(this, objDoc);
            addPropertyView(this, objArt);
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

            addPropertyView(this, objDoc);
            addPropertyView(this, objArt);
            addPropertyView(this, extOutQuantity, objDoc, objArt);
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

            addPropertyView(this, objStore);
            addPropertyView(this, objArt);
            addPropertyView(this, objStore, objArt);
        }
    }

    // ------------------------------------- Временные методы --------------------------- //

    void fillData(DataAdapter Adapter) throws SQLException {

        Map<Class,Integer> ClassQuantity = new HashMap();
        ClassQuantity.put(article,20);
        ClassQuantity.put(articleGroup,3);
        ClassQuantity.put(store,3);
        ClassQuantity.put(extIncomeDocument,30);
        ClassQuantity.put(intraDocument,30);
        ClassQuantity.put(extOutcomeDocument,50);

        Map<DataProperty, Set<DataPropertyInterface>> PropNotNulls = new HashMap();
        name.putNotNulls(PropNotNulls,0);
        artGroup.putNotNulls(PropNotNulls,0);
        docDate.putNotNulls(PropNotNulls,0);
        docStore.putNotNulls(PropNotNulls,0);
        intraStore.putNotNulls(PropNotNulls,0);

        Map<DataProperty,Integer> PropQuantity = new HashMap();

        autoFillDB(Adapter,ClassQuantity,PropQuantity,PropNotNulls);
    }

}


