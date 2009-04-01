package sample;

import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.data.Union;
import platform.server.logics.BusinessLogics;
import platform.server.logics.properties.linear.*;
import platform.server.logics.classes.RemoteClass;
import platform.server.view.navigator.NavigatorElement;
import platform.server.view.navigator.NavigatorForm;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.Filter;
import platform.server.view.form.UserValueLink;
import platform.server.auth.User;
import platform.interop.Compare;
import platform.interop.UserInfo;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.net.MalformedURLException;
import java.rmi.registry.LocateRegistry;

import net.sf.jasperreports.engine.JRException;

public class SampleBusinessLogics extends BusinessLogics<SampleBusinessLogics> {

    public SampleBusinessLogics(DataAdapter iAdapter,int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(iAdapter,port);
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, FileNotFoundException, JRException, MalformedURLException {
        System.out.println("Server is starting...");
        DataAdapter adapter = new PostgreDataAdapter("sample3","localhost");
        SampleBusinessLogics BL = new SampleBusinessLogics(adapter,7652);
//        BL.fillData();
        LocateRegistry.createRegistry(7652).rebind("BusinessLogics", BL);
//        Naming.rebind("rmi://127.0.0.1:1099/TmcBusinessLogics",new TmcBusinessLogics());
        System.out.println("Server has successfully started");

    }

    protected void initGroups() {
    }

    RemoteClass article,document,inDocument,outDocument;
    protected void initClasses() {
        article = addObjectClass("Товар", objectClass);
        document = addObjectClass("Документ", objectClass);
        inDocument = addObjectClass("Приходный документ", document);
        outDocument = addObjectClass("Расходный документ", document);
    }


    LDP outInQuantity;
    LP name,date,quantity,inQuantity,sumOutQuantity;
    LP outSumInQuantity,isInDocument;
    LP multiply2;
    LP remains;
    
    protected void initProperties() {

        RemoteClass quantityClass = RemoteClass.numeric(15,3);

        LP lessEquals = addCFProp(Compare.LESS_EQUALS);
        LP less = addCFProp(Compare.LESS);
        multiply2 = addMFProp(quantityClass,2);
        LP multiplyBit2 = addMFProp(RemoteClass.bit,2);

        name = addDProp(baseGroup, "name", "Имя", RemoteClass.string(50), objectClass);
        date = addDProp(baseGroup, "date", "Дата", RemoteClass.date, document);
        quantity = addDProp(baseGroup, "quantity", "Кол-во", quantityClass, document, article);
        outInQuantity = addDProp(baseGroup, "outInQuantity", "Кол-во расх. док.", quantityClass, outDocument, article, inDocument);

        isInDocument = addCProp("Приход",RemoteClass.bit,true,inDocument);
        inQuantity = addJProp("Кол-во прихода",multiply2,2,quantity,1,2,isInDocument,1);

        LP isOutDocument = addCProp("Расход",RemoteClass.bit,true,outDocument);
        LP outQuantity = addJProp(baseGroup, "Кол-во расхода",multiply2,2,quantity,1,2,isOutDocument,1);

        outSumInQuantity = addGProp(baseGroup, "Кол-во расх.", outInQuantity, true, 1, 2);
        sumOutQuantity = addGProp(baseGroup, "Всего расх.", outInQuantity,true, 3, 2);

        remains = addUProp(baseGroup,"Ост. по парт.",Union.SUM,2,1,inQuantity,1,2,-1,sumOutQuantity,1,2);

        LP remainPrev = addGProp(baseGroup, "Всего до", addJProp(baseGroup, "Остаток до",multiply2,3,
                        addJProp("Документ до",multiplyBit2,2,
                                addJProp("Дата до",lessEquals,2,date,1,date,2),1,2,
                                lessEquals,1,2),1,2,
                        remains,1,3),true,2,3);

        LP calcQuantity = addJProp(baseGroup, "Расч. кол-во",addSFProp("prm1+LEAST(prm3,prm2)-prm3", quantityClass, 4),3,
            remains,3,2,outQuantity,1,2, remainPrev,3,2,
            addJProp(baseGroup, "Есть ост.",less,3,
                addJProp(baseGroup, "Всего пред",addSFProp("prm1-prm2", quantityClass, 2),2,
                        remainPrev,1,2,remains,1,2),3,2,
                outQuantity,1,2),1,2,3);
//        setDefProp(outInQuantity,calcQuantity,true);
    }

    protected void initConstraints() {
    }

    protected void initPersistents() {
    }

    protected void initTables() {
    }

    protected void initIndexes() {
    }

    protected void initNavigators() throws JRException, FileNotFoundException {

        createDefaultClassForms(objectClass, baseElement);
        
        new InDocumentArticleNavigatorForm(baseElement, 100, "Прих. документы");
        new OutDocumentArticleNavigatorForm(baseElement, 101, "Расх. документы");
    }

    private class InDocumentArticleNavigatorForm extends NavigatorForm {

        public InDocumentArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objDoc = addSingleGroupObjectImplement(inDocument, "Документ", properties,
                                                                        baseGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyView(objDoc, objArt, properties, baseGroup);
        }
    }

    private class OutDocumentArticleNavigatorForm extends NavigatorForm {

        public OutDocumentArticleNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectImplement objOutDoc = addSingleGroupObjectImplement(outDocument, "Расх. документ", properties,
                                                                        baseGroup);
            ObjectImplement objArt = addSingleGroupObjectImplement(article, "Товар", properties,
                                                                        baseGroup, true);
            ObjectImplement objInDoc = addSingleGroupObjectImplement(inDocument, "Прих. документ", properties,
                                                                        baseGroup);

            addPropertyView(objOutDoc, objArt, properties, baseGroup);
            addPropertyView(objArt, objInDoc, properties, baseGroup);
            addPropertyView(objOutDoc, objArt, objInDoc, properties, baseGroup);

            addFixedFilter(new Filter(getPropertyView(remains.property).view, Filter.NOT_EQUALS, new UserValueLink(0)));

            addHintsNoUpdate(remains.property);
        }
    }

    protected void initAuthentication() {

        User user1 = authPolicy.addUser("user1", "user1", new UserInfo("Петр", "Петров"));
    }
}