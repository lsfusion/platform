package budget;


import platform.server.data.sql.DataAdapter;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.linear.LP;
import platform.server.logics.BusinessLogics;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.view.navigator.filter.CompareFilterNavigator;
import platform.server.auth.User;
import platform.server.classes.AbstractCustomClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.StringClass;
import platform.interop.Compare;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import net.sf.jasperreports.engine.JRException;

import javax.swing.*;

import sample.SampleBusinessLogics;

public class BudgetBusinessLogics extends BusinessLogics<SampleBusinessLogics> {

    public BudgetBusinessLogics(DataAdapter iAdapter,int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(iAdapter,port);
    }

    AbstractGroup documentGroup, fixedGroup, currentGroup, lastDocumentGroup;

    protected void initGroups() {
    }

    AbstractCustomClass document, operation, inAbsOperation, outAbsOperation;

    ConcreteCustomClass article, store, incomeDocument, outcomeDocument, articleGroup, currency, exOperation, section, inOperation, outOperation;

    protected void initClasses() {
        operation = addAbstractClass("Операции", namedObject, transaction);
        inAbsOperation = addAbstractClass("Абс. приход", operation);
        outAbsOperation = addAbstractClass("Абс. расход", operation);
        exOperation = addConcreteClass("Опер. конверсия", inAbsOperation, outAbsOperation);
        inOperation = addConcreteClass("Опер. приход", inAbsOperation);
        outOperation = addConcreteClass("Опер. расход", outAbsOperation);

        currency = addConcreteClass("Валюта", namedObject);
        section = addConcreteClass("Статья", namedObject);

    }

    LP quantity, documentStore, artGroup, groupBalanceQuantity, inSum, outSum, outComment, outSection, inCur, outCur;
    LP balanceQuantity, incQuantity, balanceGroupQuantity, curBalance, exRate;

    protected void initProperties() {
        LP multiplyDouble2 = addMFProp(DoubleClass.instance, 2);
        inCur = addDProp("inCurrency", "Валюта пр.", currency, inAbsOperation);
        outCur = addDProp("outCurrency", "Валюта расх.", currency, outAbsOperation);

        LP inCurName = addJProp(baseGroup, "Назв. валюты", name, inCur, 1);
        LP outCurName = addJProp(baseGroup, "Назв. валюты", name, outCur, 1);

        inSum = addDProp(baseGroup, "inSum", "Сумма прихода", DoubleClass.instance, inOperation);

        outSum = addDProp(baseGroup, "outSum", "Сумма расхода", DoubleClass.instance, outAbsOperation);

        outComment = addDProp(baseGroup, "comment", "Коментарий записи", StringClass.get(20), outOperation);
        outSection = addDProp(baseGroup, "section", "Статья расхода", section, outOperation);

        exRate = addDProp(baseGroup, "rate", "Курс операции", DoubleClass.instance, exOperation);

        LP incOpVal =  addJProp("Кол-во прихода", and1, inSum, 1, is(inOperation), 1);
        LP decVal =  addJProp("Расход", and1, outSum, 1, is(outAbsOperation), 1);
        LP incExCalc = addJProp("Приход обмена", multiplyDouble2, outSum, 1, exRate, 1);
        LP incExVal = addJProp("Приход обмена", and1, incExCalc, 1, is(exOperation), 1);
        LP incVal = addCUProp(baseGroup, "Приход",  incOpVal, incExVal);

        LP incSum = addSGProp(baseGroup, "Приход по валюте", incVal, inCur, 1);
        LP decSum = addSGProp(baseGroup, "Расход по валюте", decVal, outCur, 1);

        balanceQuantity = addDUProp(baseGroup, "Ост. по валюте", incSum, decSum);
    }

    protected void initConstraints() {
//        balanceQuantity.property.constraint = new PositiveConstraint();
    }

    protected void initPersistents() {
    }

    protected void initTables() {
    }

    protected void initIndexes() {
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User user1 = addUser("user1");
    }

    NavigatorForm mainAccountForm, salesArticleStoreForm;

    protected void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement primaryData = new NavigatorElement(baseElement, 100, "Первичные данные");
           NavigatorForm recordForm = new RecordNavigatorForm(primaryData, 114, "Список");

        NavigatorElement aggregateData = new NavigatorElement(baseElement, 200, "Сводная информация");
           NavigatorForm systemCurrency = new SystemCurrencyForm(aggregateData, 213, "Остаток по валютам");

    }


       private class RecordNavigatorForm extends NavigatorForm {

        public RecordNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objInOp = addSingleGroupObjectImplement(inOperation, "Операция прихода", properties, baseGroup);
            ObjectNavigator objOutOp = addSingleGroupObjectImplement(outOperation, "Операция расхода", properties, baseGroup);
            ObjectNavigator objExOp = addSingleGroupObjectImplement(exOperation, "Операция конверсии", properties, baseGroup);

            addObjectActions(this, objInOp);
            addObjectActions(this, objOutOp);
            addObjectActions(this, objExOp);
        }
    }

     private class SystemCurrencyForm extends NavigatorForm {

        public SystemCurrencyForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objCur = addSingleGroupObjectImplement(currency, "Валюта", properties, baseGroup);
            ObjectNavigator objInOp = addSingleGroupObjectImplement(inAbsOperation, "Операция пр.", properties, baseGroup);
            ObjectNavigator objOutOp = addSingleGroupObjectImplement(outAbsOperation, "Операция расх.", properties, baseGroup);
            addPropertyView(objCur, objInOp, properties, baseGroup);
            addPropertyView(objCur, objOutOp, properties, baseGroup);

            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(inCur, objInOp), Compare.EQUALS, objCur));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outCur, objOutOp), Compare.EQUALS, objCur));
        }
    }


}
