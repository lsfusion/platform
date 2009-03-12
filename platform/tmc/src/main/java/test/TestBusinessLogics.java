package test;

import platform.server.logics.BusinessLogics;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.ObjectClass;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.logics.properties.AggregateProperty;
import platform.server.logics.properties.groups.AbstractGroup;
import platform.server.logics.properties.linear.*;
import platform.server.data.Union;
import platform.server.data.sql.DataAdapter;
import platform.server.view.navigator.NavigatorElement;
import platform.server.view.navigator.NavigatorForm;
import platform.interop.Compare;

import java.sql.SQLException;
import java.util.*;
import java.io.FileNotFoundException;
import java.io.IOException;

import net.sf.jasperreports.engine.JRException;

public class TestBusinessLogics extends BusinessLogics<TestBusinessLogics> {

    TestBusinessLogics(DataAdapter iDataAdapter) throws ClassNotFoundException, IOException, SQLException, InstantiationException, IllegalAccessException, JRException, FileNotFoundException {
        super(iDataAdapter);
    }

    TestBusinessLogics(DataAdapter iDataAdapter,int testType,Integer seed,int iterations) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException {
        super(iDataAdapter,testType,seed,iterations);
    }

    // заполняет тестовую базу
    public void fillData() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        Map<DataProperty,Integer> PropQuantity = new HashMap();
        Map<DataProperty, Set<DataPropertyInterface>> PropNotNulls = new HashMap();

        Name.putNotNulls(PropNotNulls,0);
        DocStore.putNotNulls(PropNotNulls,0);
        DocDate.putNotNulls(PropNotNulls,0);
        ArtToGroup.putNotNulls(PropNotNulls,0);
        PrihQuantity.putNotNulls(PropNotNulls,0);
        RashQuantity.putNotNulls(PropNotNulls,0);

        Map<RemoteClass,Integer> ClassQuantity = new HashMap();
/*        ClassQuantity.put(Article,1000);
        ClassQuantity.put(ArticleGroup,50);
        ClassQuantity.put(Store,5);
        ClassQuantity.put(PrihDocument,500);
        ClassQuantity.put(RashDocument,2000);*/
/*        ClassQuantity.put(Article,100);
        ClassQuantity.put(ArticleGroup,5);
        ClassQuantity.put(Store,4);
        ClassQuantity.put(PrihDocument,50);
        ClassQuantity.put(RashDocument,200);*/
        ClassQuantity.put(article,20);
        ClassQuantity.put(articleGroup,3);
        ClassQuantity.put(store,3);
        ClassQuantity.put(PrihDocument,30);
        ClassQuantity.put(RashDocument,50);

        PropQuantity.put((DataProperty)PrihQuantity.property,10);
        PropQuantity.put((DataProperty)RashQuantity.property,3);

        autoFillDB(ClassQuantity,PropQuantity,PropNotNulls);
    }

    protected void initGroups() {

    }

    RemoteClass articleGroup;
    RemoteClass document;
    RemoteClass article;
    RemoteClass store;
    RemoteClass PrihDocument;
    RemoteClass RashDocument;

    protected void initClasses() {

        article = new ObjectClass(4, "Товар", objectClass);
        store = new ObjectClass(5, "Склад", objectClass);
        document = new ObjectClass(6, "Документ", objectClass);
        PrihDocument = new ObjectClass(7, "Приходный документ", document);
        RashDocument = new ObjectClass(8, "Расходный документ", document);
        articleGroup = new ObjectClass(9, "Группа товаров", objectClass);
    }

    LDP Name,DocStore,PrihQuantity,RashQuantity,ArtToGroup,
            DocDate,GrAddV,ArtAddV,BarCode,Price,ExpireDate,Weight;
    LUP FilledProperty;
    LUP Quantity;
    LUP OstArtStore;
    LUP MaxOpStore;
    LUP OpValue;
    LGP GP,GSum,GAP,G2P,OstArt,MaxPrih,SumMaxArt,PrihArtStore,RashArtStore,ArtDateRash;

    LGP RashArtInt;

    protected void initProperties() {

        AbstractGroup groupArticleG = new AbstractGroup("Группа");
        AbstractGroup groupArticleA = new AbstractGroup("Атрибуты");
        AbstractGroup groupArticleC = new AbstractGroup("Ценовые параметры");
        AbstractGroup groupArticleS = new AbstractGroup("Показатели");

        Name = addDProp("имя", RemoteClass.string(50), objectClass);
        groupArticleA.add(Name.property);

        DocStore = addDProp("склад", store, document);

        PrihQuantity = addDProp("кол-во прих.", RemoteClass.integer, PrihDocument, article);
        PrihQuantity.property.caption = "кол-во прих.";

        RashQuantity = addDProp("кол-во расх.", RemoteClass.integer, RashDocument, article);

        ArtToGroup = addDProp("гр. тов.", articleGroup, article);
        groupArticleG.add(ArtToGroup.property);

        DocDate = addDProp("дата док.", RemoteClass.date, document);

        GrAddV = addDProp("нац. по гр.", RemoteClass.integer, articleGroup);

        ArtAddV = addDProp("нац. перегр.", RemoteClass.integer, article);
        groupArticleC.add(ArtAddV.property);

        BarCode = addDProp("штрих-код", RemoteClass.doubleClass, article);
        groupArticleA.add(BarCode.property);

        Price = addDProp("цена", RemoteClass.longClass, article);
        groupArticleA.add(Price.property);

        ExpireDate = addDProp("срок годн.", RemoteClass.date, article);
        groupArticleA.add(ExpireDate.property);

        Weight = addDProp("вес.", RemoteClass.bit, article);
        groupArticleA.add(Weight.property);

        LCP AbsQuantity = addCProp("абст. кол-во", RemoteClass.integer, null, document, article);

        LCP IsGrmat = addCProp("признак товара", RemoteClass.integer, 0, article);
        groupArticleA.add(IsGrmat.property);

        FilledProperty = addUProp("заполнение гр. тов.", Union.MAX,1,1,IsGrmat,1,1,ArtToGroup,1);

        // сделаем Quantity перегрузкой
        Quantity = addUProp("кол-во",Union.OVERRIDE,2,1,AbsQuantity,1,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);

        LCP RashValue = addCProp("призн. расхода", RemoteClass.integer, -1, RashDocument);

        LCP PrihValue = addCProp("призн. прихода", RemoteClass.integer, 1, PrihDocument);

        OpValue = addUProp("общ. призн.",Union.OVERRIDE,1,1,RashValue,1,1,PrihValue,1);

        LGP RaznSValue = addGProp("разн. пр-рас.", OpValue,true,DocStore,1);

        LJP RGrAddV = addJProp(groupArticleG, "наценка по товару (гр.)", GrAddV,1,ArtToGroup,1);

        LUP ArtActAddV = addUProp("наценка по товару",Union.OVERRIDE,1,1,RGrAddV,1,1,ArtAddV,1);
        groupArticleC.add(ArtActAddV.property);

//        LJP Quantity = addUProp(2,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);
//
        LNFP NotZero = addNFProp();
        LCFP Less = addCFProp(Compare.LESS);
        LMFP Multiply = addMFProp(RemoteClass.integer,2);
        LMFP Multiply3 = addMFProp(RemoteClass.integer,3);

        LJP StoreName = addJProp("имя склада", Name,1,DocStore,1);

        LJP ArtGroupName = addJProp(groupArticleG, "имя гр. тов.", Name,1,ArtToGroup,1);

        LDP ArtGName = addDProp("при доб. гр. тов.", RemoteClass.string(50), article);
        setDefProp(ArtGName,ArtGroupName,true);

        LJP InDoc = addJProp("товар в док.", NotZero,2,Quantity,1,2);

        LJP DDep = addJProp("предш. док.", Less,2,DocDate,1,DocDate,2);
        DDep.property.XL = true;

        LJP QDep = addJProp("изм. баланса", Multiply3,3,DDep,1,2,Quantity,1,3,InDoc,2,3);
        QDep.property.XL = true;

        GSum = addGProp("остаток до операции", QDep,true,2,3);

        GP = addGProp("сумм кол-во док. тов.", Quantity,true,DocStore,1,2);
        GAP = addGProp("сумм кол-во тов.", GP,true,2);
        G2P = addGProp("скл-гр. тов", Quantity,true,DocStore,1,ArtToGroup,2);

        PrihArtStore = addGProp("приход по складу", PrihQuantity,true,DocStore,1,2);
        RashArtStore = addGProp("расход по складу", RashQuantity,true,DocStore,1,2);
        OstArtStore = addUProp("остаток по складу", Union.MAX,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);

        OstArt = addGProp("остаток по товару", OstArtStore,true,2);

        MaxPrih = addGProp("макс. приход по гр. тов.", PrihQuantity,false,DocStore,1,ArtToGroup,2);

        MaxOpStore = addUProp("макс. операция",Union.MAX,2,1,PrihArtStore,1,2,1,RashArtStore,1,2);

        SumMaxArt = addGProp("макс. операция (сумма)", MaxOpStore,true,2);

        LGP ArtMaxQty = addGProp("макс. кол-во", Quantity,false,2);

/*        LSFP BetweenDate = addWSFProp("prm1>=prm2 AND prm1<=prm3",3);
        LJP DokBetweenDate = addJProp("документ в интервале", BetweenDate,3,DocDate,1,2,3);

        LJP RashBetweenDate = addJProp("расх. кол-во в интервале", Multiply,4,RashQuantity,2,1,DokBetweenDate,2,3,4);

        RashArtInt = addGProp("расход по товару", RashBetweenDate,true,1,3,4);

        ArtDateRash = addGProp("реал. за день", RashQuantity, true, DocDate, 1, 2);
  */
    }

    protected void initConstraints() {
/*
        Constraints.put((Property)OstArtStore.Property,new PositiveConstraint());
        Constraints.put((Property)FilledProperty.Property,new NotEmptyConstraint());
        Constraints.put((Property)BarCode.Property,new UniqueConstraint());
*/
    }

    protected void initPersistents() {

        persistents.add((AggregateProperty)GP.property);
        persistents.add((AggregateProperty)GAP.property);
        persistents.add((AggregateProperty)G2P.property);
        persistents.add((AggregateProperty)GSum.property);
//        Persistents.add((AggregateProperty)Quantity.Property);
        persistents.add((AggregateProperty)OstArtStore.property);
        persistents.add((AggregateProperty)OstArt.property);
        persistents.add((AggregateProperty)MaxPrih.property);
        persistents.add((AggregateProperty)MaxOpStore.property);
        persistents.add((AggregateProperty)SumMaxArt.property);
        persistents.add((AggregateProperty)OpValue.property);
    }

    protected void initTables() {

        tableFactory.include(article);
        tableFactory.include(store);
        tableFactory.include(articleGroup);
        tableFactory.include(article,document);
        tableFactory.include(article,store);
    }

    protected void initIndexes() {

        List indexBarCode = new ArrayList();
        indexBarCode.add(BarCode.property);
        indexes.add(indexBarCode);

        List indexOstArtStore = new ArrayList();
        indexOstArtStore.add(OstArtStore.property);
        indexes.add(indexOstArtStore);

    }

    protected void initNavigators() {

        createDefaultClassForms(objectClass, baseElement);

        NavigatorElement group1 = new NavigatorElement<TestBusinessLogics>(1,"Group 1");
        NavigatorElement group2 = new NavigatorElement<TestBusinessLogics>(2,"Group 2");

        baseElement.add(group1);
        baseElement.add(group2);

        NavigatorForm testForm = new TestNavigatorForm(3,"Test Form 1", this);
        group1.add(testForm);

        NavigatorForm simpleForm = new SimpleNavigatorForm(4,"Test Form 2", this);
        testForm.add(simpleForm);
        simpleForm.isPrintForm = true;

        NavigatorForm test2Form = new Test2NavigatorForm(5,"Test Form 3", this);
        group2.add(test2Form);

        NavigatorForm integralForm = new IntegralNavigatorForm(6, "Integral Form", this);
        group2.add(integralForm);

        NavigatorForm articleDateForm = new ArticleDateNavigatorForm(7, "Article/Date", this);
        group2.add(articleDateForm);

        testForm.addRelevantElement(simpleForm);
        testForm.addRelevantElement(test2Form);

        simpleForm.addRelevantElement(testForm);

        PrihDocument.addRelevantElement(testForm);
        RashDocument.addRelevantElement(simpleForm);

        article.addRelevantElement(test2Form);
        articleGroup.addRelevantElement(testForm);

    }

    protected void initAuthentication() {
    }
}
