package sample;

import net.sf.jasperreports.engine.JRException;
import platform.interop.Compare;
import platform.interop.UserInfo;
import platform.server.auth.User;
import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.linear.LDP;
import platform.server.logics.property.linear.LP;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.CompareFilterNavigator;
import platform.server.classes.CustomClass;
import platform.server.classes.NumericClass;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.registry.LocateRegistry;
import java.sql.SQLException;

public class DistrTmcBusinessLogics extends BusinessLogics<DistrTmcBusinessLogics> {

    public DistrTmcBusinessLogics(DataAdapter iAdapter,int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(iAdapter,port);
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, FileNotFoundException, JRException, MalformedURLException {
        System.out.println("Server is starting...");
        DataAdapter adapter = new PostgreDataAdapter("sample5","localhost","postgres","11111");
        DistrTmcBusinessLogics BL = new DistrTmcBusinessLogics(adapter,7652);
        BL.fillData();
        LocateRegistry.createRegistry(7652).rebind("BusinessLogics", BL);
//        Naming.rebind("rmi://127.0.0.1:1099/TmcBusinessLogics",new TmcBusinessLogics());
        System.out.println("Server has successfully started");

    }

    protected void initGroups() {
    }

    CustomClass article,document,inDocument,outDocument;
    protected void initClasses() {
        article = addConcreteClass("Товар", namedObject);
        document = addConcreteClass("Документ", namedObject, transaction);
        inDocument = addConcreteClass("Приходный документ", document);
        outDocument = addConcreteClass("Расходный документ", document);
    }


    LDP outInQuantity;
    LP quantity,inQuantity,sumOutQuantity;
    LP outSumInQuantity,isInDocument;
    LP multiply2;
    LP remains;

    protected void initProperties() {

        NumericClass quantityClass = NumericClass.get(15, 3);

        LP lessEquals = addCFProp(Compare.LESS_EQUALS);
        LP less = addCFProp(Compare.LESS);
        multiply2 = addMFProp(quantityClass,2);

        quantity = addDProp(baseGroup, "quantity", "Кол-во", quantityClass, document, article);
        outInQuantity = addDProp(baseGroup, "outInQuantity", "Кол-во расх. док.", quantityClass, outDocument, article, inDocument);

        inQuantity = addJProp("Кол-во прихода",and1, quantity, 1, 2, is(inDocument), 1);

        outSumInQuantity = addSGProp(baseGroup, "Кол-во расх.", outInQuantity, 1, 2);
        sumOutQuantity = addSGProp(baseGroup, "Всего расх.", outInQuantity, 3, 2);

        remains = addDUProp(baseGroup,"Ост. по парт.",inQuantity,sumOutQuantity);

//        LP outQuantity = addJProp(baseGroup, "Кол-во расхода",and1, quantity, 1, 2, is(outDocument), 1);

//        addUGProp(baseGroup, "Расч. кол-во", 1, remains, outQuantity, 3, 2);

//        setDefProp(outInQuantity,calcQuantity,true);
    }

    protected void initConstraints() {
    }

    protected void initPersistents() {
    }

    protected void initTables() {
        tableFactory.include("article",article);
        tableFactory.include("document", transaction);
        tableFactory.include("articledocument",article, transaction);
    }

    protected void initIndexes() {
    }

    protected void initNavigators() throws JRException, FileNotFoundException {

        createDefaultClassForms(baseClass, baseElement);

        new InDocumentArticleNavigatorForm(baseElement, 100, "Прих. документы");
        new OutDocumentArticleNavigatorForm(baseElement, 101, "Расх. документы");
        new SystemNavigatorForm(baseElement, 102, "Движение (документ*товар)");
    }

    private class InDocumentArticleNavigatorForm extends NavigatorForm {

        public InDocumentArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDoc = addSingleGroupObjectImplement(inDocument, "Документ", properties,
                                                                        baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup);
        }
    }

    private class OutDocumentArticleNavigatorForm extends NavigatorForm {

        public OutDocumentArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objOutDoc = addSingleGroupObjectImplement(outDocument, "Расх. документ", properties,
                                                                        baseGroup);
            ObjectNavigator objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);
            ObjectNavigator objInDoc = addSingleGroupObjectImplement(inDocument, "Прих. документ", properties,
                                                                        baseGroup);

            addPropertyView(objOutDoc, objArt, properties, baseGroup);
            addPropertyView(objArt, objInDoc, properties, baseGroup);
            addPropertyView(objOutDoc, objArt, objInDoc, properties, baseGroup);

            addFixedFilter(new CompareFilterNavigator(getPropertyView(remains.property).view, Compare.NOT_EQUALS, 0));

            addHintsNoUpdate(remains.property);
        }
    }

    private class SystemNavigatorForm extends NavigatorForm {

        public SystemNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            GroupObjectNavigator group = new GroupObjectNavigator(IDShift(1));

            ObjectNavigator objDoc = new ObjectNavigator(IDShift(1), document, "Документ");
            ObjectNavigator objArt = new ObjectNavigator(IDShift(1), article, "Товар");

            group.add(objDoc);
            group.add(objArt);
            addGroup(group);

            addPropertyView(objDoc, properties, baseGroup);
            addPropertyView(objArt, properties, baseGroup);
            addPropertyView(objDoc, objArt, properties, baseGroup);
        }
    }

    protected void initAuthentication() {

        User user1 = authPolicy.addUser("user1", "user1", new UserInfo("Петр", "Петров"));
    }
}