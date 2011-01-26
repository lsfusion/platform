package sample;

import net.sf.jasperreports.engine.JRException;
import platform.interop.Compare;
import platform.server.auth.User;
import platform.server.classes.CustomClass;
import platform.server.classes.NumericClass;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.OrFilterEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class DistrTmcBusinessLogics extends BusinessLogics<DistrTmcBusinessLogics> {

    public DistrTmcBusinessLogics(DataAdapter iAdapter,int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(iAdapter,port);
    }

    protected void initGroups() {
    }

    CustomClass article,document,inDocument,outDocument;
    protected void initClasses() {
        article = addConcreteClass("article", "Товар", baseClass.named);
        document = addConcreteClass("document", "Документ", baseClass.named, transaction);
        inDocument = addConcreteClass("inDocument", "Приходный документ", document);
        outDocument = addConcreteClass("outDocument", "Расходный документ", document);
    }


    LP outInQuantity;
    LP inQuantity,sumOutQuantity;
    LP outSumInQuantity,isInDocument;
    LP multiply2;
    LP remains;

    protected void initProperties() {

        NumericClass quantityClass = NumericClass.get(15, 3);

        LP lessEquals = addCFProp(Compare.LESS_EQUALS);
        LP less = addCFProp(Compare.LESS);
        multiply2 = addMFProp(quantityClass,2);

        inQuantity = addDProp(baseGroup, "quantity", "Кол-во прих.", quantityClass, inDocument, article);
        outInQuantity = addDProp(baseGroup, "outInQuantity", "Кол-во расх. док.", quantityClass, outDocument, article, inDocument);

        sumOutQuantity = addSGProp(baseGroup, "Всего расх.", outInQuantity, 3, 2);

        remains = addDUProp(baseGroup,"Ост. по парт.",inQuantity,sumOutQuantity);

        outSumInQuantity = addDGProp(baseGroup, "outSumInQuantity", "Кол-во расх.", 2, false, outInQuantity, 1, 2,
                addJProp(and1, remains, 1, 2, is(outDocument), 3), 3, 2, 1, date, 3, 3);

//        LF outQuantity = addJProp(baseGroup, "Кол-во расхода",and1, quantity, 1, 2, is(outDocument), 1);

//        addUGProp(baseGroup, "Расч. кол-во", 1, remains, outQuantity, 3, 2);

//        setDefProp(outInQuantity,calcQuantity,true);
    }

    protected void initTables() {
        tableFactory.include("article",article);
        tableFactory.include("document", transaction);
        tableFactory.include("articledocument",article, transaction);
    }

    protected void initIndexes() {
    }

    protected void initNavigators() throws JRException, FileNotFoundException {

        new InDocumentArticleFormEntity(baseElement, "inDocArticleForm", "Прих. документы");
        new OutDocumentArticleFormEntity(baseElement, "outDocArticleForm", "Расх. документы");
        new SystemFormEntity(baseElement, "systemForm", "Движение (документ*товар)");
    }

    private class InDocumentArticleFormEntity extends FormEntity {

        public InDocumentArticleFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objDoc = addSingleGroupObject(inDocument, "Документ",
                    baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup, true);

            addPropertyDraw(objDoc, objArt, baseGroup);
        }
    }

    private class OutDocumentArticleFormEntity extends FormEntity {

        public OutDocumentArticleFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objOutDoc = addSingleGroupObject(outDocument, "Расх. документ",
                    baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар",
                    baseGroup, true);
            ObjectEntity objInDoc = addSingleGroupObject(inDocument, "Прих. документ",
                    baseGroup);

            addPropertyDraw(objOutDoc, objArt, baseGroup);
            addPropertyDraw(objArt, objInDoc, baseGroup);
            addPropertyDraw(objOutDoc, objArt, objInDoc, baseGroup);

            addFixedFilter(new OrFilterEntity(
                    new CompareFilterEntity(getPropertyObject(remains), Compare.NOT_EQUALS, 0),
                    new NotNullFilterEntity(getPropertyObject(outInQuantity))));

//            addHintsNoUpdate(remains.property);
        }
    }

    private class SystemFormEntity extends FormEntity {

        public SystemFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            GroupObjectEntity group = new GroupObjectEntity(genID());

            ObjectEntity objDoc = new ObjectEntity(genID(), document, "Документ");
            ObjectEntity objArt = new ObjectEntity(genID(), article, "Товар");

            group.add(objDoc);
            group.add(objArt);
            addGroup(group);

            addPropertyDraw(objDoc, baseGroup);
            addPropertyDraw(objArt, baseGroup);
            addPropertyDraw(objDoc, objArt, baseGroup);
        }
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
    }
}