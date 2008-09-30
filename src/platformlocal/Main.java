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

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

/*        DataAdapter Syntax = DataAdapter.getDefault();

        Table Table1 = new Table("table1");
        KeyField Table1Key1 = new KeyField("key1","integer");
        Table1.Keys.add(Table1Key1);
        KeyField Table1Key2 = new KeyField("key2","integer");
        Table1.Keys.add(Table1Key2);
        PropertyField Table1Prop1 = new PropertyField("prop1","integer");
        Table1.Properties.add(Table1Prop1);
        PropertyField Table1Prop2 = new PropertyField("prop2","integer");
        Table1.Properties.add(Table1Prop2);
        PropertyField Table1Prop3 = new PropertyField("prop3","integer");
        Table1.Properties.add(Table1Prop3);
        PropertyField Table1Prop4 = new PropertyField("prop4","integer");
        Table1.Properties.add(Table1Prop4);

        Table Table2 = new Table("table2");
        KeyField Table2Key1 = new KeyField("key1","integer");
        Table2.Keys.add(Table2Key1);
        PropertyField Table2Prop1 = new PropertyField("prop1","integer");
        Table2.Properties.add(Table2Prop1);
        PropertyField Table2Prop2 = new PropertyField("prop2","integer");
        Table2.Properties.add(Table2Prop2);

        JoinQuery<KeyField,PropertyField> Query = new JoinQuery<KeyField,PropertyField>(Table1.Keys);
        Join<KeyField,PropertyField> TableJoin = new UniJoin<KeyField,PropertyField>(Table1,Query,true);
        Query.Properties.put(Table1Prop1,TableJoin.Exprs.get(Table1Prop1));

//        Join<KeyField,PropertyField> Table2Join = new UniJoin<KeyField,PropertyField>(Table1,Query,true);
//        Query.Properties.put(Table1Prop2,Table2Join.Exprs.get(Table1Prop2));
        Join<KeyField,PropertyField> Table2Join = new Join<KeyField,PropertyField>(Table1,false);
        Table2Join.Joins.put(Table1Key1,Query.MapKeys.get(Table1Key2));
        Table2Join.Joins.put(Table1Key2,Query.MapKeys.get(Table1Key1));
        Query.Properties.put(Table1Prop2,Table2Join.Exprs.get(Table1Prop2));

//        Join<KeyField,PropertyField> Table3Join = new Join<KeyField,PropertyField>(Table1,true);
//        Table3Join.Joins.put(Table1Key1,Query.MapKeys.get(Table1Key2));
//        Table3Join.Joins.put(Table1Key2,Query.MapKeys.get(Table1Key1));
//        Query.Properties.put(Table1Prop3,Table3Join.Exprs.get(Table1Prop3));

        Collection<String> SubKeys = new ArrayList();
        SubKeys.add("zkey1");
        SubKeys.add("zkey2");
        UnionQuery<String,String> SubUnion = new UnionQuery<String,String>(SubKeys,3);

        JoinQuery<String,String> SubQuery = SubUnion.newJoinQuery(1);
        Join<KeyField,PropertyField> Table2Join1 = new Join<KeyField,PropertyField>(Table2,true);
        Table2Join1.Joins.put(Table2Key1,SubQuery.MapKeys.get("zkey1"));
        SubQuery.Properties.put("zprop1",Table2Join1.Exprs.get(Table2Prop2));

        Join<KeyField,PropertyField> Table2Join2 = new Join<KeyField,PropertyField>(Table1,true);
        Table2Join2.Joins.put(Table1Key1,SubQuery.MapKeys.get("zkey1"));
        Table2Join2.Joins.put(Table1Key2,SubQuery.MapKeys.get("zkey2"));
        SubQuery.Properties.put("zprop2",Table2Join2.Exprs.get(Table1Prop3));
//        SubQuery.Wheres.add(new FieldExprCompareWhere(Table2Join1.Exprs.get(Table2Prop1),11,0));
//        SubQuery.Wheres.add(new FieldExprCompareWhere(SubQuery.MapKeys.get("zkey1"),11,0));

        SubQuery = SubUnion.newJoinQuery(1);
        Table2Join1 = new Join<KeyField,PropertyField>(Table2,true);
        Table2Join1.Joins.put(Table2Key1,SubQuery.MapKeys.get("zkey1"));
        SubQuery.Properties.put("zprop1",Table2Join1.Exprs.get(Table2Prop2));

        Table2Join2 = new Join<KeyField,PropertyField>(Table1,true);
        Table2Join2.Joins.put(Table1Key1,SubQuery.MapKeys.get("zkey1"));
        Table2Join2.Joins.put(Table1Key2,SubQuery.MapKeys.get("zkey2"));
        SubQuery.Properties.put("zprop2",Table2Join2.Exprs.get(Table1Prop2));
//        SubQuery.Wheres.add(new FieldExprCompareWhere(SubQuery.MapKeys.get("zkey1"),11,0));

        Join<String,String> SubJoin = new Join<String, String>(SubUnion,false);
//        SubJoin.Joins.put("zkey1",TableJoin.Exprs.get(Table1Prop4));
//        SubJoin.Joins.put("zkey2",TableJoin.Exprs.get(Table1Prop3));
        SubJoin.Joins.put("zkey1",Query.MapKeys.get(Table1Key1));
        SubJoin.Joins.put("zkey2",Query.MapKeys.get(Table1Key2));
        Query.Properties.put(Table1Prop4,SubJoin.Exprs.get("zprop2"));

        System.out.println((new ModifyQuery(Table1,Query)).getInsertSelect(Syntax));
//        System.out.println(Query.getSelect(new ArrayList(),new ArrayList(),Adapter));

        if(1==1) return;*/

/*        int a=1;
        while(a==1) {
            System.out.println("Opened");
            new TestBusinessLogics(1);
            System.out.println("Closed");
            new TestBusinessLogics(0);
        }
  */
/*        UnionQuery<KeyField,PropertyField> Union = new UnionQuery<KeyField,PropertyField>(Table1.Keys,1);

        JoinQuery<KeyField,PropertyField> Query = Union.newJoinQuery(1);

        Join<KeyField,PropertyField> TableJoin = new UniJoin<KeyField,PropertyField>(Table1,Query);
        Query.Wheres.add(new JoinWhere(TableJoin));

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
        Query2.Wheres.add(new JoinWhere(Q2TableJoin));

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

            DataAdapter Adapter = DataAdapter.getDefault();

            TestBusinessLogics BL = new TestBusinessLogics();

            DataSession Session = BL.createSession(Adapter);
            BL.FillDB(Session);
            Session.close();

            BL.fillData(Adapter);

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

        Map<Class,Integer> ClassQuantity = new HashMap();
/*        ClassQuantity.put(Article,1000);
        ClassQuantity.put(ArticleGroup,50);
        ClassQuantity.put(Store,5);
        ClassQuantity.put(PrihDocument,500);
        ClassQuantity.put(RashDocument,2000); */
        ClassQuantity.put(Article,10);
        ClassQuantity.put(ArticleGroup,2);
        ClassQuantity.put(Store,2);
        ClassQuantity.put(PrihDocument,10);
        ClassQuantity.put(RashDocument,20);

        Map<DataProperty,Integer> PropQuantity = new HashMap();
        Map<DataProperty,Set<DataPropertyInterface>> PropNotNulls = new HashMap();

        Name.putNotNulls(PropNotNulls,0);
        DocStore.putNotNulls(PropNotNulls,0);
        DocDate.putNotNulls(PropNotNulls,0);
        ArtToGroup.putNotNulls(PropNotNulls,0);
        PrihQuantity.putNotNulls(PropNotNulls,0);
        RashQuantity.putNotNulls(PropNotNulls,0);

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

    PropertyObjectImplement AddPropView(NavigatorForm fbv,LP ListProp,GroupObjectImplement gv,ObjectImplement... Params) {
        PropertyObjectImplement PropImpl = new PropertyObjectImplement((ObjectProperty)ListProp.Property);

        ListIterator<PropertyInterface> i = ListProp.ListInterfaces.listIterator();
        for(ObjectImplement Object : Params) {
            PropImpl.Mapping.put(i.next(),Object);
        }
        fbv.Properties.add(new PropertyView(fbv.IDShift(1),PropImpl,gv));
        return PropImpl;
    }

    Class ArticleGroup;
    Class Document;
    Class Article;
    Class Store;
    Class PrihDocument;
    Class RashDocument;

    void InitClasses() {

        Article = new ObjectClass(4, "Товар");
        Article.AddParent(objectClass);
        Store = new ObjectClass(5, "Склад");
        Store.AddParent(objectClass);
        Document = new ObjectClass(6, "Документ");
        Document.AddParent(objectClass);
        PrihDocument = new ObjectClass(7, "Приходный документ");
        PrihDocument.AddParent(Document);
        RashDocument = new ObjectClass(8, "Расходный документ");
        RashDocument.AddParent(Document);
        ArticleGroup = new ObjectClass(9, "Группа товаров");
        ArticleGroup.AddParent(objectClass);
    }

    LDP Name,DocStore,PrihQuantity,RashQuantity,ArtToGroup,
            DocDate,GrAddV,ArtAddV,BarCode,ExpireDate,Weight;
    LJP FilledProperty,Quantity,OstArtStore,MaxOpStore,OpValue;
    LGP GP,GSum,GAP,G2P,OstArt,MaxPrih,SumMaxArt,PrihArtStore,RashArtStore;

    LGP RashArtInt;

    void InitProperties() {

        PropertyGroup groupArticleG = new PropertyGroup("Группа");
        PropertyGroup groupArticleA = new PropertyGroup("Атрибуты");
        PropertyGroup groupArticleC = new PropertyGroup("Ценовые параметры");
        PropertyGroup groupArticleS = new PropertyGroup("Показатели");

        Name = AddDProp(stringClass, objectClass);
        Name.Property.caption = "имя";
        groupArticleA.add(Name.Property);

        DocStore = AddDProp(Store,Document);
        DocStore.Property.caption = "склад";

        PrihQuantity = AddDProp(quantityClass,PrihDocument,Article);
        PrihQuantity.Property.caption = "кол-во прих.";

        RashQuantity = AddDProp(quantityClass,RashDocument,Article);
        RashQuantity.Property.caption = "кол-во расх.";

        ArtToGroup = AddDProp(ArticleGroup,Article);
        ArtToGroup.Property.caption = "гр. тов";
        groupArticleG.add(ArtToGroup.Property);

        DocDate = AddDProp(dateClass,Document);
        DocDate.Property.caption = "дата док.";

        GrAddV = AddDProp(quantityClass,ArticleGroup);
        GrAddV.Property.caption = "нац. по гр.";

        ArtAddV = AddDProp(quantityClass,Article);
        ArtAddV.Property.caption = "нац. перегр.";
        groupArticleC.add(ArtAddV.Property);

        BarCode = AddDProp(quantityClass,Article);
        BarCode.Property.caption = "штрих-код";
        groupArticleA.add(BarCode.Property);

        ExpireDate = AddDProp(dateClass,Article);
        ExpireDate.Property.caption = "срок годн.";
        groupArticleA.add(ExpireDate.Property);

        Weight = AddDProp(bitClass,Article);
        Weight.Property.caption = "вес.";
        groupArticleA.add(Weight.Property);

        LDP AbsQuantity = AddCProp(null, quantityClass,Document,Article);
        AbsQuantity.Property.caption = "абст. кол-во";

        LDP IsGrmat = AddCProp(0, quantityClass,Article);
        IsGrmat.Property.caption = "признак товара";
        groupArticleA.add(IsGrmat.Property);

        FilledProperty = AddUProp(0,1,1,IsGrmat,1,1,ArtToGroup,1);
        FilledProperty.Property.caption = "заполнение гр. тов.";

        // сделаем Quantity перегрузкой
        Quantity = AddUProp(2,2,1,AbsQuantity,1,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);
        Quantity.Property.caption = "кол-во";

        LDP RashValue = AddCProp(-1, quantityClass,RashDocument);
        RashValue.Property.caption = "призн. расхода";

        LDP PrihValue = AddCProp(1, quantityClass,PrihDocument);
        PrihValue.Property.caption = "призн. прихода";

        OpValue = AddUProp(2,1,1,RashValue,1,1,PrihValue,1);
        OpValue.Property.caption = "общ. призн.";

        LGP RaznSValue = AddGProp(OpValue,true,DocStore,1);
        RaznSValue.Property.caption = "разн. пр-рас.";

        LJP RGrAddV = AddJProp(GrAddV,1,ArtToGroup,1);
        RGrAddV.Property.caption = "наценка по товару (гр.)";
        groupArticleG.add(RGrAddV.Property);

        LJP ArtActAddV = AddUProp(2,1,1,RGrAddV,1,1,ArtAddV,1);
        ArtActAddV.Property.caption = "наценка по товару";
        groupArticleC.add(ArtActAddV.Property);

//        LJP Quantity = AddUProp(2,2,1,PrihQuantity,1,2,1,RashQuantity,1,2);
//
        LSFP Dirihle = AddSFProp("prm1<prm2",true,2);
        LMFP Multiply = AddMFProp(2);

        LJP StoreName = AddJProp(Name,1,DocStore,1);
        StoreName.Property.caption = "имя склада";

        LJP ArtGroupName = AddJProp(Name,1,ArtToGroup,1);
        ArtGroupName.Property.caption = "имя гр. тов.";
        groupArticleG.add(ArtGroupName.Property);

        LDP ArtGName = AddDProp(stringClass,Article);
        ArtGName.Property.caption = "при доб. гр. тов.";
        SetDefProp(ArtGName,ArtGroupName,true);

        LJP DDep = AddJProp(Dirihle,2,DocDate,1,DocDate,2);
        DDep.Property.caption = "предш. док.";
        ((ObjectProperty)DDep.Property).XL = true;

        LJP QDep = AddJProp(Multiply,3,DDep,1,2,Quantity,1,3);
        QDep.Property.caption = "изм. баланса";
        ((ObjectProperty)QDep.Property).XL = true;

        GSum = AddGProp(QDep,true,2,3);
        GSum.Property.caption = "остаток до операции";

        GP = AddGProp(Quantity,true,DocStore,1,2);
        GP.Property.caption = "сумм кол-во док. тов.";
        GAP = AddGProp(GP,true,2);
        GAP.Property.caption = "сумм кол-во тов.";
        G2P = AddGProp(Quantity,true,DocStore,1,ArtToGroup,2);
        G2P.Property.caption = "скл-гр. тов";

        PrihArtStore = AddGProp(PrihQuantity,true,DocStore,1,2);
        PrihArtStore.Property.caption = "приход по складу";

        RashArtStore = AddGProp(RashQuantity,true,DocStore,1,2);
        RashArtStore.Property.caption = "расход по складу";

        OstArtStore = AddUProp(1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);
        OstArtStore.Property.caption = "остаток по складу";

        OstArt = AddGProp(OstArtStore,true,2);
        OstArt.Property.caption = "остаток по товару";

        MaxPrih = AddGProp(PrihQuantity,false,DocStore,1,ArtToGroup,2);
        MaxPrih.Property.caption = "макс. приход по гр. тов.";

        MaxOpStore = AddUProp(0,2,1,PrihArtStore,1,2,1,RashArtStore,1,2);
        MaxOpStore.Property.caption = "макс. операция";

        SumMaxArt = AddGProp(MaxOpStore,true,2);
        SumMaxArt.Property.caption = "макс. операция (сумма)";

        LSFP Between = AddSFProp("prm1>=prm2 AND prm1<=prm3",true,3);
        LJP DokBetweenDate = AddJProp(Between,3,DocDate,1,2,3);
        DokBetweenDate.Property.caption = "документ в интервале";

        LJP RashBetweenDate = AddJProp(Multiply,4,RashQuantity,2,1,DokBetweenDate,2,3,4);
        RashBetweenDate.Property.caption = "расх. кол-во в интервале";

        RashArtInt = AddGProp(RashBetweenDate,true,1,3,4);
        RashArtInt.Property.caption = "расход по товару"; 

    }

    void InitConstraints() {
/*
        Constraints.put((ObjectProperty)OstArtStore.Property,new PositiveConstraint());
        Constraints.put((ObjectProperty)FilledProperty.Property,new NotEmptyConstraint());
        Constraints.put((ObjectProperty)BarCode.Property,new UniqueConstraint());
*/
    }

    void InitPersistents() {

        Persistents.add((AggregateProperty)GP.Property);
        Persistents.add((AggregateProperty)GAP.Property);
        Persistents.add((AggregateProperty)G2P.Property);
        Persistents.add((AggregateProperty)GSum.Property);
        Persistents.add((AggregateProperty)OstArtStore.Property);
        Persistents.add((AggregateProperty)OstArt.Property);
        Persistents.add((AggregateProperty)MaxPrih.Property);
        Persistents.add((AggregateProperty)MaxOpStore.Property);
        Persistents.add((AggregateProperty)SumMaxArt.Property);
        Persistents.add((AggregateProperty)OpValue.Property);
    }

    void InitTables() {
        TableImplement Include;

/*        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Article));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Store));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(ArticleGroup));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Article));
        Include.add(new DataPropertyInterface(Document));
        TableFactory.IncludeIntoGraph(Include);
        Include = new TableImplement();
        Include.add(new DataPropertyInterface(Article));
        Include.add(new DataPropertyInterface(Store));
        TableFactory.IncludeIntoGraph(Include);*/
    }

    void InitIndexes() {
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

        GroupObjectImplement gv = new GroupObjectImplement();
        GroupObjectImplement gv2 = new GroupObjectImplement();
        GroupObjectImplement gv3 = new GroupObjectImplement();

        gv.addObject(obj1);
        gv2.addObject(obj2);
        gv3.addObject(obj3);
        addGroup(gv);
        addGroup(gv2);
        addGroup(gv3);
        gv.GID = 1;
        gv2.GID = 2;
        gv3.GID = 3;

        Set<String> Obj2Set = new HashSet();
        Obj2Set.add("гр. тов");
        Set<String> Obj3Set = new HashSet();
        Obj3Set.add("имя");
        Obj3Set.add("дата док.");

        BL.FillSingleViews(obj1,this,null);
        Map<String,PropertyObjectImplement> Obj2Props = BL.FillSingleViews(obj2,this,Obj2Set);
        Map<String,PropertyObjectImplement> Obj3Props = BL.FillSingleViews(obj3,this,Obj3Set);

        PropertyObjectImplement QImpl = BL.AddPropView(this,BL.Quantity,gv3,obj3,obj2);
        BL.AddPropView(this,BL.GP,gv3,obj3,obj2);
        BL.AddPropView(this,BL.PrihQuantity,gv3,obj3,obj2);
        BL.AddPropView(this,BL.RashQuantity,gv3,obj3,obj2);
        BL.AddPropView(this,BL.GSum,gv3,obj3,obj2);

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
        formView.get(gv).defaultViewType = true;
        formView.get(gv).singleViewType = true;

        richDesign = formView;


    }

}

class SimpleNavigatorForm extends NavigatorForm<TestBusinessLogics> {

    SimpleNavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),BL.Article);
        obj1.caption = "товар";

        GroupObjectImplement gv = new GroupObjectImplement();

        gv.addObject(obj1);
        addGroup(gv);
        gv.GID = 4;

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

        GroupObjectImplement gv = new GroupObjectImplement();
        GroupObjectImplement gv2 = new GroupObjectImplement();

        gv.addObject(obj1);
        gv2.addObject(obj2);
        addGroup(gv);
        addGroup(gv2);
        gv.GID = 1;
        gv2.GID = 2;

        BL.FillSingleViews(obj1,this,null);
        BL.FillSingleViews(obj2,this,null);

        PropertyObjectImplement QImpl = BL.AddPropView(this,BL.Quantity,gv2,obj1,obj2);
        BL.AddPropView(this,BL.GP,gv2,obj1,obj2);
        BL.AddPropView(this,BL.PrihQuantity,gv2,obj1,obj2);
        BL.AddPropView(this,BL.RashQuantity,gv2,obj1,obj2);

        addFixedFilter(new Filter(QImpl, 5, new UserValueLink(0)));
//        BL.AddPropView(this,BL.GSum,gv2,obj1,obj2);

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

        ObjectImplement obj1 = new ObjectImplement(IDShift(1),BL.dateClass);
        obj1.caption = "дата 1";

        ObjectImplement obj2 = new ObjectImplement(IDShift(1),BL.dateClass);
        obj2.caption = "дата 2";

        ObjectImplement obj3 = new ObjectImplement(IDShift(1),BL.Article);
        obj3.caption = "товар";

        GroupObjectImplement gv = new GroupObjectImplement();
        GroupObjectImplement gv2 = new GroupObjectImplement();

        gv.addObject(obj1);
        gv.addObject(obj2);
        addGroup(gv);
        gv.GID = 334;

        gv2.addObject(obj3);
        addGroup(gv2);
        gv2.GID = 335;

        BL.FillSingleViews(obj3,this,null);

        BL.AddPropView(this,BL.RashArtInt,gv2,obj3,obj1,obj2);

        DefaultClientFormView formView = new DefaultClientFormView(this);
        formView.get(gv).defaultViewType = false;
        formView.get(gv).singleViewType = true;

        richDesign = formView;

    }

}

class LP {
    LP(Property iProperty) {
        Property=iProperty;
        ListInterfaces = new ArrayList<PropertyInterface>();
    }
    Property Property;
    List<PropertyInterface> ListInterfaces;
}

class LDP extends LP {

    LDP(Property iProperty) {super(iProperty);}

    void AddInterface(Class InClass) {
        DataPropertyInterface Interface = new DataPropertyInterface(InClass);
        ListInterfaces.add(Interface);
        Property.Interfaces.add(Interface);
    }

    void ChangeProperty(DataSession Session, Object Value, Integer... iParams) throws SQLException {
        Map<PropertyInterface,ObjectValue> Keys = new HashMap();
        Integer IntNum = 0;
        for(int i : iParams) {
            DataPropertyInterface Interface = (DataPropertyInterface)ListInterfaces.get(IntNum);
            Keys.put(Interface,new ObjectValue(i,Interface.Class));
            IntNum++;
        }

        ((DataProperty)Property).ChangeProperty(Keys, Value, Session);
    }

    void putNotNulls(Map<DataProperty,Set<DataPropertyInterface>> PropNotNulls,Integer... iParams) {
        Set<DataPropertyInterface> InterfaceNotNulls = new HashSet();
        for(Integer Interface : iParams)
            InterfaceNotNulls.add((DataPropertyInterface)ListInterfaces.get(Interface));

        PropNotNulls.put((DataProperty)Property,InterfaceNotNulls);
    }
}

class LSFP extends LP {

    LSFP(Property iProperty,IntegralClass iClass,int Objects) {
        super(iProperty);
        for(int i=0;i<Objects;i++) {
            StringFormulaPropertyInterface Interface = new StringFormulaPropertyInterface(iClass,"prm"+(i+1));
            ListInterfaces.add(Interface);
            Property.Interfaces.add(Interface);
        }
    }
}

class LMFP extends LP {

    LMFP(Property iProperty,IntegralClass iClass,int Objects) {
        super(iProperty);
        for(int i=0;i<Objects;i++) {
            FormulaPropertyInterface Interface = new FormulaPropertyInterface(iClass);
            ListInterfaces.add(Interface);
            Property.Interfaces.add(Interface);
        }
    }
}


class LJP extends LP {

    LJP(Property iProperty,int Objects) {
        super(iProperty);
        for(int i=0;i<Objects;i++) {
            PropertyInterface Interface = new PropertyInterface();
            ListInterfaces.add(Interface);
            Property.Interfaces.add(Interface);
        }
    }
}

class LGP extends LP {

    LP GroupProperty;
    LGP(Property iProperty,LP iGroupProperty) {
        super(iProperty);
        GroupProperty = iGroupProperty;
    }

    void AddInterface(PropertyInterfaceImplement Implement) {
        GroupPropertyInterface Interface = new GroupPropertyInterface(Implement);
        ListInterfaces.add(Interface);
        Property.Interfaces.add(Interface);
    }
}

/*
    List<PropertyView> GetPropViews(RemoteForm fbv, Property prop) {

        List<PropertyView> result = new ArrayList();

        for (PropertyView propview : fbv.Properties)
            if (propview.View.Property == prop) result.add(propview);

        return result;
    }

    // "СЂРёСЃСѓРµС‚" РєР»Р°СЃСЃ, СЃРѕ РІСЃРµРјРё СЃРІ-РІР°РјРё
    void DisplayClasses(DataAdapter Adapter, DataPropertyInterface[] ToDraw) throws SQLException {

        Map<DataPropertyInterface,SourceExpr> JoinSources = new HashMap<DataPropertyInterface,SourceExpr>();
        SelectQuery SimpleSelect = new SelectQuery(null);
        FromTable PrevSelect = null;
        for(int ic=0;ic<ToDraw.length;ic++) {
            FromTable Select = TableFactory.ObjectTable.ClassSelect(ToDraw[ic].Class);
            Select.JoinType = "FULL";
            if(PrevSelect==null)
                SimpleSelect.From = Select;
            else
                PrevSelect.Joins.add(Select);

            PrevSelect = Select;
            JoinSources.put(ToDraw[ic],new FieldSourceExpr(Select,TableFactory.ObjectTable.Key.Name));
        }

        JoinList Joins=new JoinList();

        Integer SelFields = 0;
        Iterator<Property> i = Properties.iterator();
        while(i.hasNext()) {
            Property Prop = i.next();

            MapBuilder<PropertyInterface,DataPropertyInterface> mb= new MapBuilder<PropertyInterface,DataPropertyInterface>();
            List<Map<PropertyInterface,DataPropertyInterface>> Maps = mb.BuildMap((PropertyInterface[])Prop.Interfaces.toArray(new PropertyInterface[0]), ToDraw);
            // РїРѕРїСЂРѕР±СѓРµРј РІСЃРµ РІР°СЂРёР°РЅС‚С‹ РѕС‚РѕР±СЂР°Р¶РµРЅРёСЏ
            Iterator<Map<PropertyInterface,DataPropertyInterface>> im = Maps.iterator();
            while(im.hasNext()) {
                Map<PropertyInterface,DataPropertyInterface> Impl = im.next();
                Map<PropertyInterface,SourceExpr> JoinImplement = new HashMap();

                InterfaceClass ClassImplement = new InterfaceClass();
                Iterator<PropertyInterface> ip = Prop.Interfaces.iterator();
                while(ip.hasNext()) {
                    PropertyInterface Interface = ip.next();
                    DataPropertyInterface MapInterface = Impl.get(Interface);
                    ClassImplement.put(Interface,MapInterface.Class);
                    JoinImplement.put(Interface,JoinSources.get(MapInterface));
                }

                if(Prop.GetValueClass(ClassImplement)!=null) {
                    // С‚Рѕ РµСЃС‚СЊ Р°РєС‚СѓР°Р»СЊРЅРѕРµ СЃРІ-РІРѕ
                    SimpleSelect.Expressions.put("test"+(SelFields++).toString(),Prop.JoinSelect(Joins,JoinImplement,false));
                }
            }
        }

        Iterator<From> ij = Joins.iterator();
        while(ij.hasNext()) {
            From Join = ij.next();
            Join.JoinType = "LEFT";
            PrevSelect.Joins.add(Join);
        }

        Adapter.OutSelect(SimpleSelect);
    }
*/