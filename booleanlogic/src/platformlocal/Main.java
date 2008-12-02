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

    static boolean recreateDB = true;
    public static Integer ForceSeed = 2513;
    public static boolean DebugFlag = false;
    static boolean ActivateCaches = true;

    static DataAdapter getDefault() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        return new PostgreDataAdapter("testplat","localhost");
//          return new MSSQLDataAdapter();
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

/*        DataAdapter Syntax = getDefault();

        Table Table1 = new Table("table1");
        KeyField Table1Key1 = new KeyField("key1",Type.Integer);
        Table1.Keys.add(Table1Key1);
        KeyField Table1Key2 = new KeyField("key2",Type.Integer);
        Table1.Keys.add(Table1Key2);
        PropertyField Table1Prop1 = new PropertyField("prop1",Type.Integer);
        Table1.Properties.add(Table1Prop1);
        PropertyField Table1Prop2 = new PropertyField("prop2",Type.Integer);
        Table1.Properties.add(Table1Prop2);
        PropertyField Table1Prop3 = new PropertyField("prop3",Type.Integer);
        Table1.Properties.add(Table1Prop3);
        PropertyField Table1Prop4 = new PropertyField("prop4",Type.Integer);
        Table1.Properties.add(Table1Prop4);

        Table Table2 = new Table("table2");
        KeyField Table2Key1 = new KeyField("key1",Type.Integer);
        Table2.Keys.add(Table2Key1);
        PropertyField Table2Prop1 = new PropertyField("prop1",Type.Integer);
        Table2.Properties.add(Table2Prop1);
        PropertyField Table2Prop2 = new PropertyField("prop2",Type.Integer);
        Table2.Properties.add(Table2Prop2);

        Table Table3 = new Table("table3");
        KeyField Table3Key1 = new KeyField("key1",Type.Integer);
        Table3.Keys.add(Table3Key1);
        KeyField Table3Key2 = new KeyField("key2",Type.Integer);
        Table3.Keys.add(Table3Key2);
        PropertyField Table3Prop1 = new PropertyField("prop1",Type.Integer);
        Table3.Properties.add(Table3Prop1);

        Map<KeyField,KeyField> Map3To1 = new HashMap<KeyField, KeyField>();
        Map3To1.put(Table3Key1,Table1Key1);
        Map3To1.put(Table3Key2,Table1Key2);
        
        UnionQuery<KeyField,PropertyField> UnionQ = new UnionQuery<KeyField,PropertyField>(Table1.Keys,1);

        // 1-й запрос
        JoinQuery<KeyField,PropertyField> JoinQuery = new JoinQuery<KeyField,PropertyField>(Table1.Keys);
        Join<KeyField,PropertyField> TableJoin = new Join<KeyField,PropertyField>(Table2,true);
        TableJoin.Joins.put(Table2Key1,JoinQuery.MapKeys.get(Table1Key1));
        Map<KeyField, Integer> DumbMap = Collections.singletonMap(Table1Key2,5);
        JoinQuery.putKeyWhere(DumbMap);
        JoinQuery.add(Table2Prop1,TableJoin.Exprs.get(Table2Prop1));
        JoinQuery.add(new FieldExprCompareWhere(TableJoin.Exprs.get(Table2Prop2),1,FieldExprCompareWhere.EQUALS));
        UnionQ.add(JoinQuery,1);

        UnionQ.add(Table1,1);

        JoinQuery<KeyField,PropertyField> Join1Q = new JoinQuery<KeyField,PropertyField>(Table1.Keys);
        Join1Q.addAll((new UniJoin<KeyField,PropertyField>(UnionQ,Join1Q,true)).Exprs);
        Join1Q.addAll((new MapJoin<KeyField,PropertyField,KeyField>(Table3,Map3To1,Join1Q,true)).Exprs);

        JoinQuery<KeyField,PropertyField> Join2Q = new JoinQuery<KeyField,PropertyField>(Table1.Keys);
        Join2Q.addAll((new UniJoin<KeyField,PropertyField>(Table1,Join2Q,true)).Exprs);
        Join2Q.addAll((new MapJoin<KeyField,PropertyField,KeyField>(Table3,Map3To1,Join2Q,true)).Exprs);

        UnionQuery<KeyField,PropertyField> ResultQ = new UnionQuery<KeyField,PropertyField>(Table1.Keys,1);
        ResultQ.add(Join1Q,1);
        ResultQ.add(Join2Q,1);
  */
/*        JoinQuery<KeyField,Object> JoinGroupQuery = new JoinQuery<KeyField,Object>(Table1.Keys);
        Join<KeyField,PropertyField> TableJoin = new UniJoin<KeyField,PropertyField>(Table1,JoinGroupQuery,true);
        JoinGroupQuery.add(Table1Prop1,TableJoin.Exprs.get(Table1Prop1));
        JoinGroupQuery.add(Table1Key1,JoinGroupQuery.MapKeys.get(Table1Key1));
        GroupQuery<Object,KeyField,PropertyField> GroupQuery = new GroupQuery<Object,KeyField,PropertyField>(UnionKeys,JoinGroupQuery,Table1Prop1,1);
        UnionQ.add(GroupQuery,2);

        // 2-й запрс

        JoinQuery<KeyField, PropertyField> JoinUnion = new JoinQuery<KeyField, PropertyField>(UnionKeys);

        JoinQuery<KeyField,Object> JoinGroupQuery2 = new JoinQuery<KeyField,Object>(Table1.Keys);
        Join<KeyField,PropertyField> TableJoin2 = new UniJoin<KeyField,PropertyField>(Table1,JoinGroupQuery2,true);
        JoinGroupQuery2.add(Table1Prop2,TableJoin2.Exprs.get(Table1Prop2));
        JoinGroupQuery2.add(Table1Key1,JoinGroupQuery2.MapKeys.get(Table1Key1));
        List<KeyField> GroupKeys2 = new ArrayList();
        GroupKeys2.add(Table1Key1);
        GroupQuery<Object,KeyField,PropertyField> GroupQuery2 = new GroupQuery<Object,KeyField,PropertyField>(GroupKeys2,JoinGroupQuery2,Table1Prop2,1);

        Join<KeyField,PropertyField> GroupJoin = new UniJoin<KeyField,PropertyField>(GroupQuery2,JoinUnion,true);
        JoinUnion.add(Table1Prop1,GroupJoin.Exprs.get(Table1Prop2));

        Join<KeyField,PropertyField> Table2Join = new Join<KeyField,PropertyField>(Table2,false);
        Table2Join.Joins.put(Table2Key1,JoinUnion.MapKeys.get(Table1Key1));
        JoinUnion.add(Table2Prop2,Table2Join.Exprs.get(Table2Prop2));

        UnionQ.add(JoinUnion,-5);*/
/*
//        Join<KeyField,PropertyField> Table2Join = new UniJoin<KeyField,PropertyField>(Table1,Query,true);
//        Query.Properties.put(Table1Prop2,Table2Join.Wheres.get(Table1Prop2));
        Join<KeyField,PropertyField> Table2Join = new Join<KeyField,PropertyField>(Table1,false);
        Table2Join.Joins.put(Table1Key1,Query.MapKeys.get(Table1Key2));
        Table2Join.Joins.put(Table1Key2,Query.MapKeys.get(Table1Key1));
        Query.Properties.put(Table1Prop2,Table2Join.Wheres.get(Table1Prop2));

//        Join<KeyField,PropertyField> Table3Join = new Join<KeyField,PropertyField>(Table1,true);
//        Table3Join.Joins.put(Table1Key1,Query.MapKeys.get(Table1Key2));
//        Table3Join.Joins.put(Table1Key2,Query.MapKeys.get(Table1Key1));
//        Query.Properties.put(Table1Prop3,Table3Join.Wheres.get(Table1Prop3));

        Collection<String> SubKeys = new ArrayList();
        SubKeys.add("zkey1");
        SubKeys.add("zkey2");
        UnionQuery<String,String> SubUnion = new UnionQuery<String,String>(SubKeys,3);

        JoinQuery<String,String> SubQuery = SubUnion.newJoinQuery(1);
        Join<KeyField,PropertyField> Table2Join1 = new Join<KeyField,PropertyField>(Table2,true);
        Table2Join1.Joins.put(Table2Key1,SubQuery.MapKeys.get("zkey1"));
        SubQuery.Properties.put("zprop1",Table2Join1.Wheres.get(Table2Prop2));

        Join<KeyField,PropertyField> Table2Join2 = new Join<KeyField,PropertyField>(Table1,true);
        Table2Join2.Joins.put(Table1Key1,SubQuery.MapKeys.get("zkey1"));
        Table2Join2.Joins.put(Table1Key2,SubQuery.MapKeys.get("zkey2"));
        SubQuery.Properties.put("zprop2",Table2Join2.Wheres.get(Table1Prop3));
//        SubQuery.Wheres.add(new FieldExprCompareWhere(Table2Join1.Wheres.get(Table2Prop1),11,0));
//        SubQuery.Wheres.add(new FieldExprCompareWhere(SubQuery.MapKeys.get("zkey1"),11,0));

        SubQuery = SubUnion.newJoinQuery(1);
        Table2Join1 = new Join<KeyField,PropertyField>(Table2,true);
        Table2Join1.Joins.put(Table2Key1,SubQuery.MapKeys.get("zkey1"));
        SubQuery.Properties.put("zprop1",Table2Join1.Wheres.get(Table2Prop2));

        Table2Join2 = new Join<KeyField,PropertyField>(Table1,true);
        Table2Join2.Joins.put(Table1Key1,SubQuery.MapKeys.get("zkey1"));
        Table2Join2.Joins.put(Table1Key2,SubQuery.MapKeys.get("zkey2"));
        SubQuery.Properties.put("zprop2",Table2Join2.Wheres.get(Table1Prop2));
//        SubQuery.Wheres.add(new FieldExprCompareWhere(SubQuery.MapKeys.get("zkey1"),11,0));

        Join<String,String> SubJoin = new Join<String, String>(SubUnion,false);
//        SubJoin.Joins.put("zkey1",TableJoin.Wheres.get(Table1Prop4));
//        SubJoin.Joins.put("zkey2",TableJoin.Wheres.get(Table1Prop3));
        SubJoin.Joins.put("zkey1",Query.MapKeys.get(Table1Key1));
        SubJoin.Joins.put("zkey2",Query.MapKeys.get(Table1Key2));
        Query.Properties.put(Table1Prop4,SubJoin.Wheres.get("zprop2"));*/

//        System.out.println((new ModifyQuery(Table1,ResultQ)).getInsertSelect(Syntax));
//        System.out.println(Query.getSelect(new ArrayList(),new ArrayList(),Adapter));

//        if(1==1) return;

        if(ForceSeed==null || ForceSeed!=-1) {
            int a=1;
            while(a==1) {
                System.out.println("Opened");
                new TmcBusinessLogics(1);
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
        Query.Properties.put(Table1Prop1,new JoinExpr<KeyField,PropertyField>(Table2Join,Table2Prop1,true));

        Join<KeyField,PropertyField> Table2Join2 = new Join<KeyField,PropertyField>(Table2);
        Table2Join2.Joins.put(Table2Key1,new JoinExpr<KeyField,PropertyField>(TableJoin,Table1Prop1,true));
        Query.Properties.put(Table1Prop2,new JoinExpr<KeyField,PropertyField>(Table2Join2,Table2Prop2,true));

        FormulaSourceExpr Formula = new FormulaSourceExpr("prm1=3");
        Formula.Params.put("prm1",new JoinExpr<KeyField,PropertyField>(Table2Join2,Table2Prop2,true));
        Query.Properties.put(Table1Prop3,new FormulaWhereSourceExpr(Formula,true));

        JoinQuery<KeyField,PropertyField> Query2 = Union.newJoinQuery(1);
        Join<KeyField,PropertyField> Q2TableJoin = new UniJoin<KeyField,PropertyField>(Table1,Query2);
        Query2.Wheres.add(new Where(Q2TableJoin));

        Query2.Properties.put(Table1Prop1,new JoinExpr<KeyField,PropertyField>(Q2TableJoin,Table1Prop1,false));
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
            RemoteNavigator<TestBusinessLogics> Navigator =  new RemoteNavigator(Adapter,BL);

/*            JFrame frame = new JFrame("Hello");
            frame.add(new ClientForm(Navigator.CreateForm(1)));
            frame.setVisible(true);*/
            Layout = new Layout(Navigator);

/*            if(!Layout.Loaded) {
                Layout.DefaultStation.drop(new ClientFormDockable(((NavigatorForm)Navigator.GetElements(null).get(0)).ID,Navigator));
                Layout.DefaultStation.drop(new ClientFormDockable(((NavigatorForm)Navigator.GetElements(null).get(1)).ID,Navigator));
            }*/
//            Frontend.add(new DefaultDockable((new ClientForm(Form)).getContentPane(),"Form 2"),"Forn 2");
//            Rectangle Bounds = new Rectangle(300,400);
//            ScreenStation.addDockable(new DefaultDockable((new ClientForm(Form)).getContentPane(),"Form 2"),Bounds);

            Layout.setVisible(true);
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

        PropQuantity.put((DataProperty)PrihQuantity.Property,10);
        PropQuantity.put((DataProperty)RashQuantity.Property,3);

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

    void InitGroups() {

    }

    Class ArticleGroup;
    Class Document;
    Class Article;
    Class Store;
    Class PrihDocument;
    Class RashDocument;

    void InitClasses() {

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

    void InitProperties() {

        AbstractGroup groupArticleG = new AbstractGroup("Группа");
        AbstractGroup groupArticleA = new AbstractGroup("Атрибуты");
        AbstractGroup groupArticleC = new AbstractGroup("Ценовые параметры");
        AbstractGroup groupArticleS = new AbstractGroup("Показатели");

        Name = AddDProp("имя", Class.string, objectClass);
        groupArticleA.add(Name.Property);

        DocStore = AddDProp("склад", Store, Document);

        PrihQuantity = AddDProp("кол-во прих.", Class.integer, PrihDocument, Article);
        PrihQuantity.Property.caption = "кол-во прих.";

        RashQuantity = AddDProp("кол-во расх.", Class.integer, RashDocument, Article);

        ArtToGroup = AddDProp("гр. тов.", ArticleGroup, Article);
        groupArticleG.add(ArtToGroup.Property);

        DocDate = AddDProp("дата док.", Class.date, Document);

        GrAddV = AddDProp("нац. по гр.", Class.integer, ArticleGroup);

        ArtAddV = AddDProp("нац. перегр.", Class.integer, Article);
        groupArticleC.add(ArtAddV.Property);

        BarCode = AddDProp("штрих-код", Class.doubleClass, Article);
        groupArticleA.add(BarCode.Property);

        Price = AddDProp("цена", Class.longClass, Article);
        groupArticleA.add(Price.Property);

        ExpireDate = AddDProp("срок годн.", Class.date, Article);
        groupArticleA.add(ExpireDate.Property);

        Weight = AddDProp("вес.", Class.bit, Article);
        groupArticleA.add(Weight.Property);

        LCP AbsQuantity = AddCProp("абст. кол-во",null,Class.integer,Document,Article);

        LCP IsGrmat = AddCProp("признак товара",0,Class.integer,Article);
        groupArticleA.add(IsGrmat.Property);

        FilledProperty = AddUProp("заполнение гр. тов.", 0,1,1,IsGrmat,1,1,ArtToGroup,1);

        // сделаем Quantity перегрузкой
        Quantity = AddUProp("кол-во",2,2,1,AbsQuantity,1,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);

        LCP RashValue = AddCProp("призн. расхода",-1,Class.integer,RashDocument);

        LCP PrihValue = AddCProp("призн. прихода",1,Class.integer,PrihDocument);

        OpValue = AddUProp("общ. призн.", 2,1,1,RashValue,1,1,PrihValue,1);

        LGP RaznSValue = AddGProp("разн. пр-рас.", OpValue,true,DocStore,1);

        LJP RGrAddV = AddJProp(groupArticleG, "наценка по товару (гр.)", GrAddV,1,ArtToGroup,1);

        LUP ArtActAddV = AddUProp("наценка по товару",2,1,1,RGrAddV,1,1,ArtAddV,1);
        groupArticleC.add(ArtActAddV.Property);

//        LJP Quantity = AddUProp(2,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);
//
        LSFP NotZero = AddWSFProp("prm1<>0",1);
        LSFP Less = AddWSFProp("prm1<prm2",2);
        LMFP Multiply = AddMFProp(Class.integer,2);
        LMFP Multiply3 = AddMFProp(Class.integer,3);

        LJP StoreName = AddJProp("имя склада", Name,1,DocStore,1);

        LJP ArtGroupName = AddJProp(groupArticleG, "имя гр. тов.", Name,1,ArtToGroup,1);

        LDP ArtGName = AddDProp("при доб. гр. тов.", Class.string, Article);
        setDefProp(ArtGName,ArtGroupName,true);

        LJP InDoc = AddJProp("товар в док.", NotZero,2,Quantity,1,2);

        LJP DDep = AddJProp("предш. док.", Less,2,DocDate,1,DocDate,2);
        DDep.Property.XL = true;

        LJP QDep = AddJProp("изм. баланса", Multiply3,3,DDep,1,2,Quantity,1,3,InDoc,2,3);
        QDep.Property.XL = true;

        GSum = AddGProp("остаток до операции", QDep,true,2,3);

        GP = AddGProp("сумм кол-во док. тов.", Quantity,true,DocStore,1,2);
        GAP = AddGProp("сумм кол-во тов.", GP,true,2);
        G2P = AddGProp("скл-гр. тов", Quantity,true,DocStore,1,ArtToGroup,2);

        PrihArtStore = AddGProp("приход по складу", PrihQuantity,true,DocStore,1,2);
        RashArtStore = AddGProp("расход по складу", RashQuantity,true,DocStore,1,2);
        OstArtStore = AddUProp("остаток по складу",1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);

        OstArt = AddGProp("остаток по товару", OstArtStore,true,2);

        MaxPrih = AddGProp("макс. приход по гр. тов.", PrihQuantity,false,DocStore,1,ArtToGroup,2);

        MaxOpStore = AddUProp("макс. операция", 0,2,1,PrihArtStore,1,2,1,RashArtStore,1,2);

        SumMaxArt = AddGProp("макс. операция (сумма)", MaxOpStore,true,2);

        LGP ArtMaxQty = AddGProp("макс. кол-во", Quantity,false,2);

        LSFP BetweenDate = AddWSFProp("prm1>=prm2 AND prm1<=prm3",3);
        LJP DokBetweenDate = AddJProp("документ в интервале", BetweenDate,3,DocDate,1,2,3);

        LJP RashBetweenDate = AddJProp("расх. кол-во в интервале", Multiply,4,RashQuantity,2,1,DokBetweenDate,2,3,4);

        RashArtInt = AddGProp("расход по товару", RashBetweenDate,true,1,3,4);

        ArtDateRash = AddGProp("реал. за день", RashQuantity, true, DocDate, 1, 2);

    }

    void InitConstraints() {
/*
        Constraints.put((Property)OstArtStore.Property,new PositiveConstraint());
        Constraints.put((Property)FilledProperty.Property,new NotEmptyConstraint());
        Constraints.put((Property)BarCode.Property,new UniqueConstraint());
*/
    }

    void InitPersistents() {

        Persistents.add((AggregateProperty)GP.Property);
        Persistents.add((AggregateProperty)GAP.Property);
        Persistents.add((AggregateProperty)G2P.Property);
        Persistents.add((AggregateProperty)GSum.Property);
//        Persistents.add((AggregateProperty)Quantity.Property);
        Persistents.add((AggregateProperty)OstArtStore.Property);
        Persistents.add((AggregateProperty)OstArt.Property);
        Persistents.add((AggregateProperty)MaxPrih.Property);
        Persistents.add((AggregateProperty)MaxOpStore.Property);
        Persistents.add((AggregateProperty)SumMaxArt.Property);
        Persistents.add((AggregateProperty)OpValue.Property);
    }

    void InitTables() {
        TableImplement Include;

        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,Article));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,Store));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,ArticleGroup));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,Article));
        Include.add(new DataPropertyInterface(0,Document));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(0,Article));
        Include.add(new DataPropertyInterface(0,Store));
        TableFactory.IncludeIntoGraph(Include);
    }

    void InitIndexes() {

        List indexBarCode = new ArrayList();
        indexBarCode.add(BarCode.Property);
        Indexes.add(indexBarCode);

        List indexOstArtStore = new ArrayList();
        indexOstArtStore.add(OstArtStore.Property);
        Indexes.add(indexOstArtStore);

    }

    void InitNavigators() {

        createDefaultClassForms(objectClass, baseElement);

        NavigatorElement group1 = new NavigatorElement<TestBusinessLogics>(1,"Group 1");
        NavigatorElement group2 = new NavigatorElement<TestBusinessLogics>(2,"Group 2");

        baseElement.addChild(group1);
        baseElement.addChild(group2);

        NavigatorForm testForm = new TestNavigatorForm(3,"Test Form 1", this);
        group1.addChild(testForm);

        NavigatorForm simpleForm = new SimpleNavigatorForm(4,"Test Form 2", this);
        testForm.addChild(simpleForm);
        simpleForm.isPrintForm = true;

        NavigatorForm test2Form = new Test2NavigatorForm(5,"Test Form 3", this);
        group2.addChild(test2Form);

        NavigatorForm integralForm = new IntegralNavigatorForm(6, "Integral Form", this);
        group2.addChild(integralForm);

        NavigatorForm articleDateForm = new ArticleDateNavigatorForm(7, "Article/Date", this);
        group2.addChild(articleDateForm);

        testForm.addRelevantElement(simpleForm);
        testForm.addRelevantElement(test2Form);

        simpleForm.addRelevantElement(testForm);

        PrihDocument.addRelevantElement(testForm);
        RashDocument.addRelevantElement(simpleForm);

        Article.addRelevantElement(test2Form);
        ArticleGroup.addRelevantElement(testForm);

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