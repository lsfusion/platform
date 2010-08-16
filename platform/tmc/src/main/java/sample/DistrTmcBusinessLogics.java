package sample;

import net.sf.jasperreports.engine.JRException;
import platform.interop.Compare;
import platform.server.auth.User;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.form.navigator.*;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.OrFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.classes.CustomClass;
import platform.server.classes.NumericClass;

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
        article = addConcreteClass("Товар", namedObject);
        document = addConcreteClass("Документ", namedObject, transaction);
        inDocument = addConcreteClass("Приходный документ", document);
        outDocument = addConcreteClass("Расходный документ", document);
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

        new InDocumentArticleFormEntity(baseElement, 100, "Прих. документы");
        new OutDocumentArticleFormEntity(baseElement, 101, "Расх. документы");
        new SystemFormEntity(baseElement, 102, "Движение (документ*товар)");
    }

    private class InDocumentArticleFormEntity extends FormEntity {

        public InDocumentArticleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objDoc = addSingleGroupObject(inDocument, "Документ", properties,
                                                                        baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", properties,
                                                                        baseGroup, true);

            addPropertyDraw(objDoc, objArt, properties, baseGroup);
        }
    }

    private class OutDocumentArticleFormEntity extends FormEntity {

        public OutDocumentArticleFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectEntity objOutDoc = addSingleGroupObject(outDocument, "Расх. документ", properties,
                                                                        baseGroup);
            ObjectEntity objArt = addSingleGroupObject(article, "Товар", properties,
                                                                        baseGroup, true);
            ObjectEntity objInDoc = addSingleGroupObject(inDocument, "Прих. документ", properties,
                                                                        baseGroup);

            addPropertyDraw(objOutDoc, objArt, properties, baseGroup);
            addPropertyDraw(objArt, objInDoc, properties, baseGroup);
            addPropertyDraw(objOutDoc, objArt, objInDoc, properties, baseGroup);

            addFixedFilter(new OrFilterEntity(
                    new CompareFilterEntity(getPropertyObject(remains), Compare.NOT_EQUALS, 0),
                    new NotNullFilterEntity(getPropertyObject(outInQuantity))));

//            addHintsNoUpdate(remains.property);
        }
    }

    private class SystemFormEntity extends FormEntity {

        public SystemFormEntity(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            GroupObjectEntity group = new GroupObjectEntity(IDShift(1));

            ObjectEntity objDoc = new ObjectEntity(IDShift(1), document, "Документ");
            ObjectEntity objArt = new ObjectEntity(IDShift(1), article, "Товар");

            group.add(objDoc);
            group.add(objArt);
            addGroup(group);

            addPropertyDraw(objDoc, properties, baseGroup);
            addPropertyDraw(objArt, properties, baseGroup);
            addPropertyDraw(objDoc, objArt, properties, baseGroup);
        }
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
    }
}