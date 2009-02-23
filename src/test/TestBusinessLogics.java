package test;

import platform.server.logics.BusinessLogics;
import platform.server.logics.data.TableImplement;
import platform.server.logics.session.DataSession;
import platform.server.logics.classes.DataClass;
import platform.server.logics.classes.ObjectClass;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.logics.properties.AggregateProperty;
import platform.server.logics.properties.groups.AbstractGroup;
import platform.server.logics.properties.linear.*;
import platform.server.data.sql.DataAdapter;
import platform.server.data.query.Union;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.view.navigator.NavigatorElement;
import platform.server.view.navigator.NavigatorForm;

import java.sql.SQLException;
import java.util.*;

public class TestBusinessLogics extends BusinessLogics<TestBusinessLogics> {

    TestBusinessLogics() {
        super();
    }

    TestBusinessLogics(int TestType) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(TestType);
    }

    // заполняет тестовую базу
    public void fillData(DataAdapter Adapter) throws SQLException {

        Map<DataProperty,Integer> PropQuantity = new HashMap();
        Map<DataProperty, Set<DataPropertyInterface>> PropNotNulls = new HashMap();

        Name.putNotNulls(PropNotNulls,0);
        DocStore.putNotNulls(PropNotNulls,0);
        DocDate.putNotNulls(PropNotNulls,0);
        ArtToGroup.putNotNulls(PropNotNulls,0);
        PrihQuantity.putNotNulls(PropNotNulls,0);
        RashQuantity.putNotNulls(PropNotNulls,0);

        Map<DataClass,Integer> ClassQuantity = new HashMap();
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
        ClassQuantity.put(Article,20);
        ClassQuantity.put(ArticleGroup,3);
        ClassQuantity.put(Store,3);
        ClassQuantity.put(PrihDocument,30);
        ClassQuantity.put(RashDocument,50);

        PropQuantity.put((DataProperty)PrihQuantity.property,10);
        PropQuantity.put((DataProperty)RashQuantity.property,3);

        autoFillDB(Adapter,ClassQuantity,PropQuantity,PropNotNulls);

        if(1==1) return;

        DataSession Session = createSession(Adapter);

        Integer i;
        Integer[] Articles = new Integer[6];
        for(i=0;i<Articles.length;i++) Articles[i] = addObject(Session, Article);

        Integer[] Stores = new Integer[2];
        for(i=0;i<Stores.length;i++) Stores[i] = addObject(Session, Store);

        Integer[] PrihDocuments = new Integer[6];
        for(i=0;i<PrihDocuments.length;i++) {
            PrihDocuments[i] = addObject(Session, PrihDocument);
            Name.ChangeProperty(Session, "ПР ДОК "+i.toString(), PrihDocuments[i]);
        }

        Integer[] RashDocuments = new Integer[6];
        for(i=0;i<RashDocuments.length;i++) {
            RashDocuments[i] = addObject(Session, RashDocument);
            Name.ChangeProperty(Session, "РАСХ ДОК "+i.toString(), RashDocuments[i]);
        }

        Integer[] ArticleGroups = new Integer[2];
        for(i=0;i<ArticleGroups.length;i++) ArticleGroups[i] = addObject(Session, ArticleGroup);

        Name.ChangeProperty(Session, "КОЛБАСА", Articles[0]);
        Name.ChangeProperty(Session, "ТВОРОГ", Articles[1]);
        Name.ChangeProperty(Session, "МОЛОКО", Articles[2]);
        Name.ChangeProperty(Session, "ОБУВЬ", Articles[3]);
        Name.ChangeProperty(Session, "ДЖЕМПЕР", Articles[4]);
        Name.ChangeProperty(Session, "МАЙКА", Articles[5]);

        Name.ChangeProperty(Session, "СКЛАД", Stores[0]);
        Name.ChangeProperty(Session, "ТЗАЛ", Stores[1]);

        Name.ChangeProperty(Session, "ПРОДУКТЫ", ArticleGroups[0]);
        Name.ChangeProperty(Session, "ОДЕЖДА", ArticleGroups[1]);

        DocStore.ChangeProperty(Session, Stores[0],PrihDocuments[0]);
        DocStore.ChangeProperty(Session, Stores[0],PrihDocuments[1]);
        DocStore.ChangeProperty(Session, Stores[1],PrihDocuments[2]);
        DocStore.ChangeProperty(Session, Stores[0],PrihDocuments[3]);
        DocStore.ChangeProperty(Session, Stores[1],PrihDocuments[4]);

        DocStore.ChangeProperty(Session, Stores[1],RashDocuments[0]);
        DocStore.ChangeProperty(Session, Stores[1],RashDocuments[1]);
        DocStore.ChangeProperty(Session, Stores[0],RashDocuments[2]);
        DocStore.ChangeProperty(Session, Stores[0],RashDocuments[3]);
        DocStore.ChangeProperty(Session, Stores[1],RashDocuments[4]);

//        DocStore.ChangeProperty(ad,Stores[1],Documents[5]);

        DocDate.ChangeProperty(Session, 1001,PrihDocuments[0]);
        DocDate.ChangeProperty(Session, 1001,RashDocuments[0]);
        DocDate.ChangeProperty(Session, 1008,PrihDocuments[1]);
        DocDate.ChangeProperty(Session, 1009,RashDocuments[1]);
        DocDate.ChangeProperty(Session, 1010,RashDocuments[2]);
        DocDate.ChangeProperty(Session, 1011,RashDocuments[3]);
        DocDate.ChangeProperty(Session, 1012,PrihDocuments[2]);
        DocDate.ChangeProperty(Session, 1014,PrihDocuments[3]);
        DocDate.ChangeProperty(Session, 1016,RashDocuments[4]);
        DocDate.ChangeProperty(Session, 1018,PrihDocuments[4]);

        ArtToGroup.ChangeProperty(Session, ArticleGroups[0],Articles[0]);
        ArtToGroup.ChangeProperty(Session, ArticleGroups[0],Articles[1]);
        ArtToGroup.ChangeProperty(Session, ArticleGroups[0],Articles[2]);
        ArtToGroup.ChangeProperty(Session, ArticleGroups[1],Articles[3]);
        ArtToGroup.ChangeProperty(Session, ArticleGroups[1],Articles[4]);
        ArtToGroup.ChangeProperty(Session, ArticleGroups[1],Articles[5]);

        // Quantity
        PrihQuantity.ChangeProperty(Session, 10,PrihDocuments[0],Articles[0]);
        PrihQuantity.ChangeProperty(Session, 8,PrihDocuments[2],Articles[0]);
        RashQuantity.ChangeProperty(Session, 5,RashDocuments[0],Articles[0]);
        RashQuantity.ChangeProperty(Session, 3,RashDocuments[1],Articles[0]);

        PrihQuantity.ChangeProperty(Session, 8,PrihDocuments[0],Articles[1]);
        PrihQuantity.ChangeProperty(Session, 2,PrihDocuments[1],Articles[1]);
        PrihQuantity.ChangeProperty(Session, 10,PrihDocuments[3],Articles[1]);
        RashQuantity.ChangeProperty(Session, 14,RashDocuments[2],Articles[1]);

        PrihQuantity.ChangeProperty(Session, 32,PrihDocuments[2],Articles[2]);
        PrihQuantity.ChangeProperty(Session, 18,PrihDocuments[3],Articles[2]);
        RashQuantity.ChangeProperty(Session, 2,RashDocuments[1],Articles[2]);
        RashQuantity.ChangeProperty(Session, 10,RashDocuments[3],Articles[2]);
        PrihQuantity.ChangeProperty(Session, 4,PrihDocuments[4],Articles[2]);

        PrihQuantity.ChangeProperty(Session, 4,PrihDocuments[3],Articles[3]);

        PrihQuantity.ChangeProperty(Session, 8,PrihDocuments[0],Articles[4]);
        RashQuantity.ChangeProperty(Session, 4,RashDocuments[2],Articles[4]);
        RashQuantity.ChangeProperty(Session, 4,RashDocuments[3],Articles[4]);

        PrihQuantity.ChangeProperty(Session, 10,PrihDocuments[3],Articles[5]);

        apply(Session);

//        ChangeDBTest(ad,30,new Random());

        Session.close();

/*        PrihArtStore.Property.Out(ad);
        RashArtStore.Property.Out(ad);
        OstArtStore.Property.Out(ad);
        OstArt.Property.Out(ad);

        throw new RuntimeException();
  */
    }

    protected void initGroups() {

    }

    DataClass ArticleGroup;
    DataClass Document;
    DataClass Article;
    DataClass Store;
    DataClass PrihDocument;
    DataClass RashDocument;

    protected void initClasses() {

        Article = new ObjectClass(4, "Товар", objectClass);
        Store = new ObjectClass(5, "Склад", objectClass);
        Document = new ObjectClass(6, "Документ", objectClass);
        PrihDocument = new ObjectClass(7, "Приходный документ", Document);
        RashDocument = new ObjectClass(8, "Расходный документ", Document);
        ArticleGroup = new ObjectClass(9, "Группа товаров", objectClass);
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

        Name = addDProp("имя", DataClass.string(50), objectClass);
        groupArticleA.add(Name.property);

        DocStore = addDProp("склад", Store, Document);

        PrihQuantity = addDProp("кол-во прих.", DataClass.integer, PrihDocument, Article);
        PrihQuantity.property.caption = "кол-во прих.";

        RashQuantity = addDProp("кол-во расх.", DataClass.integer, RashDocument, Article);

        ArtToGroup = addDProp("гр. тов.", ArticleGroup, Article);
        groupArticleG.add(ArtToGroup.property);

        DocDate = addDProp("дата док.", DataClass.date, Document);

        GrAddV = addDProp("нац. по гр.", DataClass.integer, ArticleGroup);

        ArtAddV = addDProp("нац. перегр.", DataClass.integer, Article);
        groupArticleC.add(ArtAddV.property);

        BarCode = addDProp("штрих-код", DataClass.doubleClass, Article);
        groupArticleA.add(BarCode.property);

        Price = addDProp("цена", DataClass.longClass, Article);
        groupArticleA.add(Price.property);

        ExpireDate = addDProp("срок годн.", DataClass.date, Article);
        groupArticleA.add(ExpireDate.property);

        Weight = addDProp("вес.", DataClass.bit, Article);
        groupArticleA.add(Weight.property);

        LCP AbsQuantity = addCProp("абст. кол-во",null, DataClass.integer,Document,Article);

        LCP IsGrmat = addCProp("признак товара",0, DataClass.integer,Article);
        groupArticleA.add(IsGrmat.property);

        FilledProperty = addUProp("заполнение гр. тов.", Union.MAX,1,1,IsGrmat,1,1,ArtToGroup,1);

        // сделаем Quantity перегрузкой
        Quantity = addUProp("кол-во",Union.OVERRIDE,2,1,AbsQuantity,1,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);

        LCP RashValue = addCProp("призн. расхода",-1, DataClass.integer,RashDocument);

        LCP PrihValue = addCProp("призн. прихода",1, DataClass.integer,PrihDocument);

        OpValue = addUProp("общ. призн.",Union.OVERRIDE,1,1,RashValue,1,1,PrihValue,1);

        LGP RaznSValue = addGProp("разн. пр-рас.", OpValue,true,DocStore,1);

        LJP RGrAddV = addJProp(groupArticleG, "наценка по товару (гр.)", GrAddV,1,ArtToGroup,1);

        LUP ArtActAddV = addUProp("наценка по товару",Union.OVERRIDE,1,1,RGrAddV,1,1,ArtAddV,1);
        groupArticleC.add(ArtActAddV.property);

//        LJP Quantity = addUProp(2,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);
//
        LNFP NotZero = addNFProp();
        LCFP Less = addCFProp(CompareWhere.LESS);
        LMFP Multiply = addMFProp(DataClass.integer,2);
        LMFP Multiply3 = addMFProp(DataClass.integer,3);

        LJP StoreName = addJProp("имя склада", Name,1,DocStore,1);

        LJP ArtGroupName = addJProp(groupArticleG, "имя гр. тов.", Name,1,ArtToGroup,1);

        LDP ArtGName = addDProp("при доб. гр. тов.", DataClass.string(50), Article);
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
        TableImplement include;

        include = new TableImplement();
        include.add(new DataPropertyInterface(0,Article));
        tableFactory.includeIntoGraph(include);
        include = new TableImplement();
        include.add(new DataPropertyInterface(0,Store));
        tableFactory.includeIntoGraph(include);
        include = new TableImplement();
        include.add(new DataPropertyInterface(0,ArticleGroup));
        tableFactory.includeIntoGraph(include);
        include = new TableImplement();
        include.add(new DataPropertyInterface(0,Article));
        include.add(new DataPropertyInterface(0,Document));
        tableFactory.includeIntoGraph(include);
        include = new TableImplement();
        include.add(new DataPropertyInterface(0,Article));
        include.add(new DataPropertyInterface(0,Store));
        tableFactory.includeIntoGraph(include);
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

        Article.addRelevantElement(test2Form);
        ArticleGroup.addRelevantElement(testForm);

    }

    protected void initAuthentication() {
    }
}
