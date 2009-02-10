/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;


public class Main {

    Class[] ClassList;

    static RemoteNavigator Navigator;

    static Layout Layout;

    static DataAdapter Adapter;
    static DataSession Session = null;

    static boolean recreateDB = false;
    public static Integer ForceSeed = -1; //1199; //3581
    public static boolean DebugFlag = false;
    static boolean ActivateCaches = true;
    static boolean AllowNulls = false;

    static DataAdapter getDefault() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        return new PostgreDataAdapter("testplat","localhost");
//        return new MySQLDataAdapter("testplat","localhost");
//          return new MSSQLDataAdapter();
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

/*        Where a = new TestDataWhere("a");
        Where b = new TestDataWhere("b");
        Where c = new TestDataWhere("c");
        Where d = new TestDataWhere("d");
        Where x = new TestDataWhere("x");
        Where y = new TestDataWhere("y");
        System.out.println(a.and(b).and(a.not())); // a.b.a' = FALSE
        System.out.println(a.not().and(c.not()).or(d.and(a.or(c)))); // a'.c'+d.(a+c) = a'.c'+d
        System.out.println(a.or(b).and(a.or(b.not()))); // (a+b).(a+b') = a
        System.out.println(a.not().or(d.and(a))); // a'+d.a = a'+d
        System.out.println(a.not().or(d.or(a).and(b))); // a'+(b.(d+a)) = a'+b
        System.out.println(a.or(b.not().and(b.not()))); // a+b'.b' = a+b'
        System.out.println(a.and(b.not()).or(a.not().and(b)).and(a.not().and(b))); // (a.b'+a'b).(a'.b)
        System.out.println(a.and(b).or(a.not().and(b.not())).and(a.and(x).or(a.not().and(y)))); // (a.b+a'.b').(a.x+a'.y)
        System.out.println(a.and(b).followFalse(a.not().or(b.not()))); // a.b.a' = FALSE

        Where result = new AndWhere();
        Where wb = new TestDataWhere("b");
        for(int i=0;i<6;i++) {
            Where iteration = new OrWhere();
            iteration = iteration.or(wb);
            for(int j=0;j<4;j++)
                iteration = iteration.or(new TestDataWhere("w"+i+"_"+j));
            result = result.and(iteration);
        }
        System.out.println(result);

        WhereTester test = new WhereTester();
        test.test();
        if(1==1) return;*/
/*        new SourceTest();
        if(1==1) return;*/

        if(ForceSeed==null || ForceSeed!=-1) {
            while(true) {
                System.out.println("Opened");
                new TmcBusinessLogics(2);
//            System.out.println("Closed");
//            try {
//                new TestBusinessLogics(0);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            System.out.println("Suspicious");
//            new TmcBusinessLogics(-1);
            }
        }
/*        UnionQuery<KeyField,PropertyField> Union = new UnionQuery<KeyField,PropertyField>(Table1.Keys,1);

        JoinQuery<KeyField,PropertyField> Query = Union.newJoinQuery(1);

        Join<KeyField,PropertyField> TableJoin = new UniJoin<KeyField,PropertyField>(Table1,Query);
        Query.Wheres.add(new Where(TableJoin));

        Join<KeyField,PropertyField> Table2Join = new Join<KeyField,PropertyField>(Table2);
        Table2Join.Joins.put(Table2Key1,new JoinExpr<KeyField,PropertyField>(TableJoin,Table1Prop1,true));
//        Table2Join.Joins.put(Table2Key1,Query.MapKeys.get(Table1Key1));
        Query.properties.put(Table1Prop1,new JoinExpr<KeyField,PropertyField>(Table2Join,Table2Prop1,true));

        Join<KeyField,PropertyField> Table2Join2 = new Join<KeyField,PropertyField>(Table2);
        Table2Join2.Joins.put(Table2Key1,new JoinExpr<KeyField,PropertyField>(TableJoin,Table1Prop1,true));
        Query.properties.put(Table1Prop2,new JoinExpr<KeyField,PropertyField>(Table2Join2,Table2Prop2,true));

        FormulaSourceExpr Formula = new FormulaSourceExpr("prm1=3");
        Formula.Params.put("prm1",new JoinExpr<KeyField,PropertyField>(Table2Join2,Table2Prop2,true));
        Query.properties.put(Table1Prop3,new FormulaWhereSourceExpr(Formula,true));

        JoinQuery<KeyField,PropertyField> Query2 = Union.newJoinQuery(1);
        Join<KeyField,PropertyField> Q2TableJoin = new UniJoin<KeyField,PropertyField>(Table1,Query2);
        Query2.Wheres.add(new Where(Q2TableJoin));

        Query2.properties.put(Table1Prop1,new JoinExpr<KeyField,PropertyField>(Q2TableJoin,Table1Prop1,false));
  */
 //       List<String> GroupKeys = new ArrayList();
//        GroupKeys.add("value");
//        GroupQuery<String,String,String> GroupQuery = new GroupQuery<String,String,String>(GroupKeys,Union,"value2",0);

        // сначала закинем KeyField'ы и прогоним Select
/*        Map<KeyField,String> KeyNames = new HashMap();
        Map<String,String> PropertyNames = new HashMap();
        Query.fillSelectNames(KeyNames,PropertyNames);
        System.out.println(Union.getSelect(KeyNames,PropertyNames));*/

//        System.out.println((new ModifyQuery(Table1,Query)).getInsertLeftKeys());

/*        Map<String,String> KeyNames = new HashMap();
        Map<String,String> PropertyNames = new HashMap();
        GroupQuery.fillSelectNames(KeyNames,PropertyNames);
        System.out.println(GroupQuery.getSelect(KeyNames,PropertyNames));
*/
//        if(1==1) return;

        try {
            UIManager.setLookAndFeel(UIManager.getInstalledLookAndFeels()[2].getClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        try {

            Adapter = Main.getDefault();

            BusinessLogics BL = new TmcBusinessLogics();

            if (recreateDB) {

                Adapter.createDB();

                DataSession Session = BL.createSession(Adapter);
                BL.FillDB(Session, true);
                Session.close();

                BL.fillData(Adapter);
            } else
                BL.FillDB(null, false);

            // базовый навигатор
            RemoteNavigator<TestBusinessLogics> remoteNavigator =  new RemoteNavigator(Adapter,BL);

            remoteNavigator.changeCurrentUser("user1", "user1");

//            LoginDialog loginDialog = new LoginDialog(remoteNavigator);
//            if (loginDialog.login()) {
                Layout = new Layout(remoteNavigator);
                Layout.setVisible(true);
//            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (JRException ex) {
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

//          java.lang.Class.forName("net.sourceforge.jtds.jdbc.Driver");
//        java.lang.Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
//          Connection cn = DriverManager.getConnection("jdbc:jtds:sqlserver://mycomp:1433;namedPipe=true;User=sa;Password=");
//        Connection cn = DriverManager.getConnection("jdbc:sqlserver://server:1433;User=sa;Password=");

//        BusinessLogics t = new BusinessLogics();
//        t.FullDBTest();

//        Test t = new Test();
//        t.SimpleTest(null);
    }
}

class TestBusinessLogics extends BusinessLogics<TestBusinessLogics> {

    TestBusinessLogics() {
        super();
    }

    TestBusinessLogics(int TestType) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(TestType);
    }

    // заполняет тестовую базу
    void fillData(DataAdapter Adapter) throws SQLException {

        Map<DataProperty,Integer> PropQuantity = new HashMap();
        Map<DataProperty,Set<DataPropertyInterface>> PropNotNulls = new HashMap();

        Name.putNotNulls(PropNotNulls,0);
        DocStore.putNotNulls(PropNotNulls,0);
        DocDate.putNotNulls(PropNotNulls,0);
        ArtToGroup.putNotNulls(PropNotNulls,0);
        PrihQuantity.putNotNulls(PropNotNulls,0);
        RashQuantity.putNotNulls(PropNotNulls,0);

        Map<Class,Integer> ClassQuantity = new HashMap();
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
        for(i=0;i<Articles.length;i++) Articles[i] = AddObject(Session, Article);

        Integer[] Stores = new Integer[2];
        for(i=0;i<Stores.length;i++) Stores[i] = AddObject(Session, Store);

        Integer[] PrihDocuments = new Integer[6];
        for(i=0;i<PrihDocuments.length;i++) {
            PrihDocuments[i] = AddObject(Session, PrihDocument);
            Name.ChangeProperty(Session, "ПР ДОК "+i.toString(), PrihDocuments[i]);
        }

        Integer[] RashDocuments = new Integer[6];
        for(i=0;i<RashDocuments.length;i++) {
            RashDocuments[i] = AddObject(Session, RashDocument);
            Name.ChangeProperty(Session, "РАСХ ДОК "+i.toString(), RashDocuments[i]);
        }

        Integer[] ArticleGroups = new Integer[2];
        for(i=0;i<ArticleGroups.length;i++) ArticleGroups[i] = AddObject(Session, ArticleGroup);

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

        Apply(Session);

//        ChangeDBTest(ad,30,new Random());

        Session.close();

/*        PrihArtStore.Property.Out(ad);
        RashArtStore.Property.Out(ad);
        OstArtStore.Property.Out(ad);
        OstArt.Property.Out(ad);

        throw new RuntimeException();
  */
    }

    void initGroups() {

    }

    Class ArticleGroup;
    Class Document;
    Class Article;
    Class Store;
    Class PrihDocument;
    Class RashDocument;

    void initClasses() {

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

    void initProperties() {

        AbstractGroup groupArticleG = new AbstractGroup("Группа");
        AbstractGroup groupArticleA = new AbstractGroup("Атрибуты");
        AbstractGroup groupArticleC = new AbstractGroup("Ценовые параметры");
        AbstractGroup groupArticleS = new AbstractGroup("Показатели");

        Name = addDProp("имя", Class.string, objectClass);
        groupArticleA.add(Name.property);

        DocStore = addDProp("склад", Store, Document);

        PrihQuantity = addDProp("кол-во прих.", Class.integer, PrihDocument, Article);
        PrihQuantity.property.caption = "кол-во прих.";

        RashQuantity = addDProp("кол-во расх.", Class.integer, RashDocument, Article);

        ArtToGroup = addDProp("гр. тов.", ArticleGroup, Article);
        groupArticleG.add(ArtToGroup.property);

        DocDate = addDProp("дата док.", Class.date, Document);

        GrAddV = addDProp("нац. по гр.", Class.integer, ArticleGroup);

        ArtAddV = addDProp("нац. перегр.", Class.integer, Article);
        groupArticleC.add(ArtAddV.property);

        BarCode = addDProp("штрих-код", Class.doubleClass, Article);
        groupArticleA.add(BarCode.property);

        Price = addDProp("цена", Class.longClass, Article);
        groupArticleA.add(Price.property);

        ExpireDate = addDProp("срок годн.", Class.date, Article);
        groupArticleA.add(ExpireDate.property);

        Weight = addDProp("вес.", Class.bit, Article);
        groupArticleA.add(Weight.property);

        LCP AbsQuantity = addCProp("абст. кол-во",null,Class.integer,Document,Article);

        LCP IsGrmat = addCProp("признак товара",0,Class.integer,Article);
        groupArticleA.add(IsGrmat.property);

        FilledProperty = addUProp("заполнение гр. тов.", 0,1,1,IsGrmat,1,1,ArtToGroup,1);

        // сделаем Quantity перегрузкой
        Quantity = addUProp("кол-во",2,2,1,AbsQuantity,1,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);

        LCP RashValue = addCProp("призн. расхода",-1,Class.integer,RashDocument);

        LCP PrihValue = addCProp("призн. прихода",1,Class.integer,PrihDocument);

        OpValue = addUProp("общ. призн.", 2,1,1,RashValue,1,1,PrihValue,1);

        LGP RaznSValue = addGProp("разн. пр-рас.", OpValue,true,DocStore,1);

        LJP RGrAddV = addJProp(groupArticleG, "наценка по товару (гр.)", GrAddV,1,ArtToGroup,1);

        LUP ArtActAddV = addUProp("наценка по товару",2,1,1,RGrAddV,1,1,ArtAddV,1);
        groupArticleC.add(ArtActAddV.property);

//        LJP Quantity = addUProp(2,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);
//
        LNFP NotZero = addNFProp();
        LCFP Less = addCFProp(CompareWhere.LESS);
        LMFP Multiply = addMFProp(Class.integer,2);
        LMFP Multiply3 = addMFProp(Class.integer,3);

        LJP StoreName = addJProp("имя склада", Name,1,DocStore,1);

        LJP ArtGroupName = addJProp(groupArticleG, "имя гр. тов.", Name,1,ArtToGroup,1);

        LDP ArtGName = addDProp("при доб. гр. тов.", Class.string, Article);
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
        OstArtStore = addUProp("остаток по складу",1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);

        OstArt = addGProp("остаток по товару", OstArtStore,true,2);

        MaxPrih = addGProp("макс. приход по гр. тов.", PrihQuantity,false,DocStore,1,ArtToGroup,2);

        MaxOpStore = addUProp("макс. операция", 0,2,1,PrihArtStore,1,2,1,RashArtStore,1,2);

        SumMaxArt = addGProp("макс. операция (сумма)", MaxOpStore,true,2);

        LGP ArtMaxQty = addGProp("макс. кол-во", Quantity,false,2);

/*        LSFP BetweenDate = addWSFProp("prm1>=prm2 AND prm1<=prm3",3);
        LJP DokBetweenDate = addJProp("документ в интервале", BetweenDate,3,DocDate,1,2,3);

        LJP RashBetweenDate = addJProp("расх. кол-во в интервале", Multiply,4,RashQuantity,2,1,DokBetweenDate,2,3,4);

        RashArtInt = addGProp("расход по товару", RashBetweenDate,true,1,3,4);

        ArtDateRash = addGProp("реал. за день", RashQuantity, true, DocDate, 1, 2);
  */
    }

    void initConstraints() {
/*
        Constraints.put((Property)OstArtStore.Property,new PositiveConstraint());
        Constraints.put((Property)FilledProperty.Property,new NotEmptyConstraint());
        Constraints.put((Property)BarCode.Property,new UniqueConstraint());
*/
    }

    void initPersistents() {

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

    void initTables() {
        TableImplement Include;

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,Article));
        tableFactory.includeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,Store));
        tableFactory.includeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,ArticleGroup));
        tableFactory.includeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,Article));
        Include.add(new DataPropertyInterface(0,Document));
        tableFactory.includeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,Article));
        Include.add(new DataPropertyInterface(0,Store));
        tableFactory.includeIntoGraph(Include);
    }

    void initIndexes() {

        List indexBarCode = new ArrayList();
        indexBarCode.add(BarCode.property);
        indexes.add(indexBarCode);

        List indexOstArtStore = new ArrayList();
        indexOstArtStore.add(OstArtStore.property);
        indexes.add(indexOstArtStore);

    }

    void InitNavigators() {

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

    void InitAuthentication() {
    }
}

class TestNavigatorForm extends NavigatorForm<TestBusinessLogics> {

    TestNavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),BL.ArticleGroup);
        obj1.caption = "группа товаров";
        ObjectImplement obj2 = new ObjectImplement(IDShift(1),BL.Article);
        obj2.caption = "товар";
        ObjectImplement obj3 = new ObjectImplement(IDShift(1),BL.Document);
        obj3.caption = "документ";

        GroupObjectImplement gv = new GroupObjectImplement(IDShift(1));
        GroupObjectImplement gv2 = new GroupObjectImplement(IDShift(1));
        GroupObjectImplement gv3 = new GroupObjectImplement(IDShift(1));

        gv.addObject(obj1);
        gv2.addObject(obj2);
        gv3.addObject(obj3);
        addGroup(gv);
        addGroup(gv2);
        addGroup(gv3);

        Set<String> Obj2Set = new HashSet();
        Obj2Set.add("гр. тов");
        Set<String> Obj3Set = new HashSet();
        Obj3Set.add("имя");
        Obj3Set.add("дата док.");

        BL.FillSingleViews(obj1,this,null);
        Map<String,PropertyObjectImplement> Obj2Props = BL.FillSingleViews(obj2,this,Obj2Set);
        Map<String,PropertyObjectImplement> Obj3Props = BL.FillSingleViews(obj3,this,Obj3Set);

        PropertyObjectImplement QImpl = BL.addPropertyView(this,BL.Quantity,gv3,obj3,obj2);
        BL.addPropertyView(this,BL.GP,gv3,obj3,obj2);
        BL.addPropertyView(this,BL.PrihQuantity,gv3,obj3,obj2);
        BL.addPropertyView(this,BL.RashQuantity,gv3,obj3,obj2);
        BL.addPropertyView(this,BL.GSum,gv3,obj3,obj2);

//        fbv.AddObjectSeek(obj3,13);
//        fbv.AddPropertySeek(Obj3Props.get("имя"),"ПРОДУКТЫ");

//        AddFilter(new NotNullFilter(QImpl));
//        addFilter(new CompareFilter(Obj2Props.get("гр. тов"),0,new ObjectValueLink(obj1)));

//        fbv.AddObjectSeek(obj3,13);
//        fbv.AddPropertySeek(Obj3Props.get("имя"),"ПРОДУКТЫ");

//        fbv.AddOrder(Obj3Props.get("имя"));
//        fbv.AddOrder(Obj3Props.get("дата док."));

//        richDesign.getGroupObject()

        DefaultClientFormView formView = new DefaultClientFormView(this);
//        formView.get(gv).defaultViewType = true;
//        formView.get(gv).singleViewType = true;
//        formView.defaultOrders.put(formView.get(BL.getPropertyView(this,QImpl)), true);

        richDesign = formView;


    }

}

class SimpleNavigatorForm extends NavigatorForm<TestBusinessLogics> {

    SimpleNavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),BL.Article);
        obj1.caption = "товар";

        GroupObjectImplement gv = new GroupObjectImplement(IDShift(1));

        gv.addObject(obj1);
        addGroup(gv);

        BL.FillSingleViews(obj1,this,null);
    }

}

class Test2NavigatorForm extends NavigatorForm<TestBusinessLogics> {

    Test2NavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),BL.Document);
        obj1.caption = "документ";
        ObjectImplement obj2 = new ObjectImplement(IDShift(1),BL.Article);
        obj2.caption = "товар";

        GroupObjectImplement gv = new GroupObjectImplement(IDShift(1));
        GroupObjectImplement gv2 = new GroupObjectImplement(IDShift(1));

        gv.addObject(obj1);
        gv2.addObject(obj2);
        addGroup(gv);
        addGroup(gv2);

        BL.FillSingleViews(obj1,this,null);
        BL.FillSingleViews(obj2,this,null);

        PropertyObjectImplement QImpl = BL.addPropertyView(this,BL.Quantity,gv2,obj1,obj2);
        BL.addPropertyView(this,BL.GP,gv2,obj1,obj2);
        BL.addPropertyView(this,BL.PrihQuantity,gv2,obj1,obj2);
        BL.addPropertyView(this,BL.RashQuantity,gv2,obj1,obj2);

        addFixedFilter(new Filter(QImpl, 5, new UserValueLink(0)));
//        BL.addPropertyView(this,BL.GSum,gv2,obj1,obj2);

//        fbv.AddObjectSeek(obj3,13);
//        fbv.AddPropertySeek(Obj3Props.get("имя"),"ПРОДУКТЫ");

//        AddFilter(new NotNullFilter(QImpl));
//        addFilter(new CompareFilter(Obj2Props.get("гр. тов"),0,new ObjectValueLink(obj1)));

//        fbv.AddObjectSeek(obj3,13);
//        fbv.AddPropertySeek(Obj3Props.get("имя"),"ПРОДУКТЫ");

//        fbv.AddOrder(Obj3Props.get("имя"));
//        fbv.AddOrder(Obj3Props.get("дата док."));
    }

}

class IntegralNavigatorForm extends NavigatorForm<TestBusinessLogics> {

    IntegralNavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),Class.date);
        obj1.caption = "дата 1";

        ObjectImplement obj2 = new ObjectImplement(IDShift(1),Class.date);
        obj2.caption = "дата 2";

        ObjectImplement obj3 = new ObjectImplement(IDShift(1),BL.Article);
        obj3.caption = "товар";

        GroupObjectImplement gv = new GroupObjectImplement(IDShift(1));
        GroupObjectImplement gv2 = new GroupObjectImplement(IDShift(1));

        gv.addObject(obj1);
        gv.addObject(obj2);
        addGroup(gv);
        gv.gridClassView = false;

        gv2.addObject(obj3);
        addGroup(gv2);

        BL.FillSingleViews(obj3,this,null);

        BL.addPropertyView(this,BL.RashArtInt,gv2,obj3,obj1,obj2);

        DefaultClientFormView formView = new DefaultClientFormView(this);
        formView.get(gv).singleViewType = true;

        richDesign = formView;

    }

}

class ArticleDateNavigatorForm extends NavigatorForm<TestBusinessLogics> {

    ArticleDateNavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),BL.Article);
        obj1.caption = "товар";

        ObjectImplement obj2 = new ObjectImplement(IDShift(1),Class.date);
        obj2.caption = "дата";

        GroupObjectImplement gv = new GroupObjectImplement(IDShift(1));
        GroupObjectImplement gv2 = new GroupObjectImplement(IDShift(1));

        gv.addObject(obj1);
        addGroup(gv);

        gv2.addObject(obj2);
        addGroup(gv2);

        BL.FillSingleViews(obj1,this,null);

        PropertyObjectImplement QImpl = BL.addPropertyView(this, BL.ArtDateRash, gv2, obj2, obj1);

        addFixedFilter(new Filter(QImpl, 5, new UserValueLink(0)));

        DefaultClientFormView formView = new DefaultClientFormView(this);
//        formView.get(gv).defaultViewType = false;
//        formView.get(gv).singleViewType = true;

        richDesign = formView;
    }
}

// тестирование Source'ов
class SourceTest {

    DataAdapter Syntax;

    Table Table1;
    KeyField Table1Key1;
    KeyField Table1Key2;
    PropertyField Table1Prop1;
    PropertyField Table1Prop2;
    PropertyField Table1Prop3;
    PropertyField Table1Prop4;

    Table Table2;
    KeyField Table2Key1;
    PropertyField Table2Prop1;
    PropertyField Table2Prop2;

    Table Table3;
    KeyField Table3Key1;
    KeyField Table3Key2;
    PropertyField Table3Prop1;
    PropertyField Table3Prop2;

    Map<KeyField,KeyField> Map3To1;
    Map<KeyField,KeyField> MapBack3To1;

    SourceTest() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Syntax = Main.getDefault();

        // Table 1
        Table1 = new Table("table1");
        Table1Key1 = new KeyField("key1",Type.Integer);
        Table1.keys.add(Table1Key1);
        Table1Key2 = new KeyField("key2",Type.Integer);
        Table1.keys.add(Table1Key2);
        Table1Prop1 = new PropertyField("prop1",Type.Integer);
        Table1.Properties.add(Table1Prop1);
        Table1Prop2 = new PropertyField("prop2",Type.Integer);
        Table1.Properties.add(Table1Prop2);
        Table1Prop3 = new PropertyField("prop3",Type.Integer);
        Table1.Properties.add(Table1Prop3);
        Table1Prop4 = new PropertyField("prop4",Type.Integer);
        Table1.Properties.add(Table1Prop4);

        // Table 2
        Table2 = new Table("table2");
        Table2Key1 = new KeyField("key1",Type.Integer);
        Table2.keys.add(Table2Key1);
        Table2Prop1 = new PropertyField("prop1",Type.Integer);
        Table2.Properties.add(Table2Prop1);
        Table2Prop2 = new PropertyField("prop2",Type.Integer);
        Table2.Properties.add(Table2Prop2);

        // Table 3
        Table3 = new Table("table3");
        Table3Key1 = new KeyField("key1",Type.Integer);
        Table3.keys.add(Table3Key1);
        Table3Key2 = new KeyField("key2",Type.Integer);
        Table3.keys.add(Table3Key2);
        Table3Prop1 = new PropertyField("prop1",Type.Integer);
        Table3.Properties.add(Table3Prop1);
        Table3Prop2 = new PropertyField("prop2",Type.Integer);
        Table3.Properties.add(Table3Prop2);

        Map3To1 = new HashMap<KeyField, KeyField>();
        Map3To1.put(Table3Key1,Table1Key1);
        Map3To1.put(Table3Key2,Table1Key2);

        MapBack3To1 = new HashMap<KeyField, KeyField>();
        MapBack3To1.put(Table3Key1,Table1Key2);
        MapBack3To1.put(Table3Key2,Table1Key1);

        System.out.println(test2().getInsertSelect(Syntax));
        System.out.println(test1().getInsertSelect(Syntax));
        System.out.println(test3().getInsertSelect(Syntax));
        System.out.println(test4().getInsertSelect(Syntax));
    }

    // просто Union 1-й и 3-й таблицы
    ModifyQuery test1() {
        JoinQuery<KeyField,PropertyField> Join1Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table11Q = new Join<KeyField, PropertyField>(Table1, Join1Q);
        Join1Q.properties.put(Table1Prop1, Table11Q.exprs.get(Table1Prop1));
        Join1Q.and(Table11Q.inJoin);

        JoinQuery<KeyField,PropertyField> Join2Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table32Q = new Join<KeyField, PropertyField>(Table3, Map3To1, Join2Q);
        Join2Q.properties.put(Table1Prop1, Table32Q.exprs.get(Table3Prop1));
        Join2Q.and(Table32Q.inJoin);

        JoinQuery<KeyField,PropertyField> Join3Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table33Q = new Join<KeyField, PropertyField>(Table3, MapBack3To1, Join3Q);
        Join3Q.properties.put(Table1Prop1, Table33Q.exprs.get(Table3Prop2));
        Join3Q.and(Table33Q.inJoin);

        UnionQuery<KeyField,PropertyField> ResultQ = new UnionQuery<KeyField,PropertyField>(Table1.keys,1);
        ResultQ.add(Join1Q,1);
        ResultQ.add(Join2Q,1);
        ResultQ.add(Join3Q,1);

        return new ModifyQuery(Table1,ResultQ);
    }

    // просто 2-ю с первой и поле второй не null
    ModifyQuery test3() {
        JoinQuery<KeyField,PropertyField> Result = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table1J = new Join<KeyField, PropertyField>(Table1, Result);

        Join<KeyField, PropertyField> Table2J = new Join<KeyField, PropertyField>(Table2);
        Table2J.joins.put(Table2Key1, Table1J.exprs.get(Table1Prop1));
        Result.properties.put(Table1Prop1, Table2J.exprs.get(Table2Prop2));
        Result.and(Table2J.exprs.get(Table2Prop2).getWhere());

        return new ModifyQuery(Table1,Result);
    }

    // 2 U(J(U(таблицы2 с prop2=1 и 2-м ключом таблицы1=5,таблица1),Table3),J(Table1,Table3))
    // последний Join должен уйти
    ModifyQuery test2() {
        UnionQuery<KeyField,PropertyField> UnionQ = new UnionQuery<KeyField,PropertyField>(Table1.keys,2);

        // 1-й запрос
        JoinQuery<KeyField,PropertyField> JoinQuery = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField,PropertyField> TableJoin = new Join<KeyField,PropertyField>(Table2);
        TableJoin.joins.put(Table2Key1,JoinQuery.mapKeys.get(Table1Key1));
        JoinQuery.putKeyWhere(Collections.singletonMap(Table1Key2,5));
        JoinQuery.properties.put(Table2Prop1, TableJoin.exprs.get(Table2Prop1));
        JoinQuery.and(new CompareWhere(TableJoin.exprs.get(Table2Prop2),new ValueExpr(1,Type.Integer),CompareWhere.EQUALS));
        UnionQ.add(JoinQuery,1);

        UnionQ.add(Table1,1);

        JoinQuery<KeyField,PropertyField> Join1Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Union1Q = new Join<KeyField, PropertyField>(UnionQ, Join1Q);
        Join1Q.properties.putAll(Union1Q.exprs);
        Join1Q.and(Union1Q.inJoin);
        Join<KeyField, PropertyField> Table31Q = new Join<KeyField, PropertyField>(Table3, Map3To1, Join1Q);
        Join1Q.properties.putAll(Table31Q.exprs);

/*        JoinQuery<KeyField,PropertyField> Join2Q = new JoinQuery<KeyField,PropertyField>(Table1.Keys);
        Join<KeyField, PropertyField> Table12Q = new Join<KeyField, PropertyField>(Table1, Join2Q);
        Join2Q.addAll(Table12Q.Exprs);
        Join2Q.add(Table12Q.InJoin);
        Join<KeyField, PropertyField> Table32Q = new Join<KeyField, PropertyField>(Table3, Map3To1, Join2Q);
        Join2Q.addAll(Table32Q.Exprs);
        Join2Q.add(Table32Q.InJoin);

        UnionQuery<KeyField,PropertyField> ResultQ = new UnionQuery<KeyField,PropertyField>(Table1.Keys,1);
        ResultQ.add(Join1Q,1);
        ResultQ.add(Join2Q,1);
  */
        return new ModifyQuery(Table1,Join1Q);
    }

    // первую группируем по полю 1, 2-й по полю 2
    ModifyQuery test4() {
        JoinQuery<KeyField,PropertyField> Join1Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table11Q = new Join<KeyField, PropertyField>(Table1, Join1Q);
        Join1Q.properties.put(Table1Prop1, Table11Q.exprs.get(Table1Prop1));
        Join1Q.properties.put(Table1Prop2, Table11Q.exprs.get(Table1Prop2));
//        Join1Q.and(Table11Q.inJoin);

        GroupQuery<PropertyField,PropertyField,PropertyField,KeyField> Group1Q = new GroupQuery<PropertyField, PropertyField, PropertyField, KeyField>(Collections.singleton(Table1Prop1),
            Join1Q,Table1Prop2,1);

        JoinQuery<KeyField,PropertyField> Join2Q = new JoinQuery<KeyField,PropertyField>(Table1.keys);
        Join<KeyField, PropertyField> Table12Q = new Join<KeyField, PropertyField>(Table1, Join2Q);
        Join2Q.properties.put(Table1Prop1, Table12Q.exprs.get(Table1Prop1));
        Join2Q.properties.put(Table1Prop3, Table12Q.exprs.get(Table1Prop3));
//        Join2Q.and(Table12Q.inJoin);

        GroupQuery<PropertyField,PropertyField,PropertyField,KeyField> Group2Q = new GroupQuery<PropertyField, PropertyField, PropertyField, KeyField>(Collections.singleton(Table1Prop1),
            Join2Q,Table1Prop3,1);

        UnionQuery<PropertyField,PropertyField> UnionQ = new UnionQuery<PropertyField,PropertyField>(Collections.singleton(Table1Prop1),2);
        UnionQ.add(Group1Q,1);
        UnionQ.add(Group2Q,1);

        JoinQuery<KeyField,PropertyField> ModiQ = new JoinQuery<KeyField,PropertyField>(Table2.keys);
        Join<PropertyField,PropertyField> JoinUnion = new Join<PropertyField,PropertyField>(UnionQ);
        JoinUnion.joins.put(Table1Prop1,ModiQ.mapKeys.get(Table2Key1));
        ModiQ.properties.putAll(JoinUnion.exprs);
        ModiQ.and(JoinUnion.inJoin);

        return new ModifyQuery(Table2,ModiQ);
    }
}