package budget;


import platform.server.data.sql.DataAdapter;
import platform.server.data.Union;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.linear.LP;
import platform.server.logics.BusinessLogics;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.view.navigator.filter.CompareFilterNavigator;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.interop.Compare;
import platform.interop.ClassViewType;

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

    AbstractCustomClass operation, inAbsOperation, outAbsOperation, absOutPerson, absOutTime;

    ConcreteCustomClass  currency, exOperation, section, inOperation, outOperation, month, salary, extraCost, person, year, pay, absMonth, testSample;

    protected void initClasses() {
        operation = addAbstractClass("Операции", namedObject, transaction);
        inAbsOperation = addAbstractClass("Абс. приход", operation);
        outAbsOperation = addAbstractClass("Абс. расход", operation);
        absOutPerson = addAbstractClass("Абс. расход сотруд.", outAbsOperation);
        absOutTime = addAbstractClass("Абс. расход времен.", outAbsOperation);

        extraCost = addConcreteClass("Дополнительные затраты", absOutPerson, absOutTime);
        pay = addConcreteClass("Выплата", absOutPerson, absOutTime);
        exOperation = addConcreteClass("Опер. конверсия", inAbsOperation, outAbsOperation);
        inOperation = addConcreteClass("Опер. приход", inAbsOperation);
        outOperation = addConcreteClass("Опер. расход", outAbsOperation);

        absMonth = addConcreteClass("Месяц", namedObject);
        salary = addConcreteClass("Зарплата", namedObject);
        currency = addConcreteClass("Валюта", namedObject);
        section = addConcreteClass("Статья", namedObject);
        month = addConcreteClass("Месяц года", baseClass);
        year = addConcreteClass("Год", baseClass);
        person = addConcreteClass("Сотрудник", namedObject);
        testSample = addConcreteClass("Пример", baseClass);
    }

    LP groupBalanceQuantity, inSum, outSum, outComment, outSection, inCur, outCur, salaryExtraCost, salaryPay;
    LP balanceQuantity, incQuantity, balanceGroupQuantity, curBalance, exRate, outPerson, salaryInMonth;

    protected void initProperties() {
        LP multiplyDouble2 = addMFProp(DoubleClass.instance, 2);

        LP monthYear = addDProp("year", "Год", year, month);
        LP mYear = addDProp("monthYear", "Месяц", absMonth, month);

        LP yearNumber = addDProp(baseGroup, "yearNum", "Год", IntegerClass.instance, year);
        LP dayCount = addDProp(baseGroup, "dayCount", "Количество дней", IntegerClass.instance, absMonth);
        LP monthNumber = addDProp(baseGroup, "monthNum", "Номер месяца", IntegerClass.instance, absMonth);

        LP salaryDays = addDProp(baseGroup, "salaryD", "Кол-во дней", IntegerClass.instance, salary);
        LP salaryCount = addDProp(baseGroup, "countS", "Размер", DoubleClass.instance, salary);
        LP currencySalary = addDProp("salaryCurrency", "Валюта", currency, salary);
        outPerson = addDProp("outP", "Сотрудник", person, salary);

        LP monthName = addJProp(baseGroup, "Название месяца", name, mYear, 1);
        salaryExtraCost = addDProp(baseGroup, "extraCostS", "Зарплата",salary, extraCost);

        LP payRate = addDProp(baseGroup, "rateP", "Курс", DoubleClass.instance, pay);
        salaryPay = addDProp(baseGroup, "salsryPay", "Выплата", salary, pay);
        

        LP salaryCurName = addJProp(baseGroup, "Назв. валюты", name, currencySalary, 1);
                
        LP outTime = addDProp("outM", "Месяц", month, absOutTime);

        LP outPersonName = addJProp(baseGroup, "Сотрудник", name, outPerson, 1);
        LP outMonthName = addJProp(baseGroup, "Месяц", name, outTime, 1);

        addConstraint(addJProp("Слишком много дней", greater2, salaryDays, 1, dayCount, 2), false);

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

        LP paySum = addJProp("Выплачено", multiplyDouble2, payRate, 1, outSum, 1);
        LP payTotal = addSGProp(baseGroup, "Всего заплачено", paySum, salaryPay, 1);
        LP extraCostTotal = addSGProp(baseGroup, "Всего затрат", outSum, salaryExtraCost, 1);

        LP salaryRest = addDUProp(baseGroup, "Осталось", salaryCount, payTotal);
        
        addConstraint(addJProp("Много выплачено", greater2, vzero, salaryRest, 1), false);

        salaryInMonth = addDProp(baseGroup, "salaryInM", "Зарплата", DoubleClass.instance, person, month);

        LP monthNum = addJProp(baseGroup, "Номер месяца", monthNumber, mYear, 1);
        LP monthYearName = addJProp(baseGroup, "Год", yearNumber, monthYear, 1);
        LP monthInY = addSFProp("prm1*12", IntegerClass.instance, 1);
        LP mYN = addJProp("Месяцев прошло", monthInY, monthYearName, 1);
        LP totalMonth = addSUProp("Всего",Union.SUM, mYN, monthNum);

        LP monthCompare = addJProp(groeq2, totalMonth, 1, totalMonth, 2);
        LP lastMonthNum = addMGProp(baseGroup, "max" , "Текущая", addJProp(and(false, false), totalMonth, 2, monthCompare, 1, 2, salaryInMonth,  3, 2), 1, 3);
        LP numToMonth = addCGProp(null , "maxToObject", "Ближайший месяц", object(month), totalMonth, totalMonth, 1);

        LP curMonth = addJProp(baseGroup, "Месяц зарплаты", numToMonth, lastMonthNum, 1, 2);
//        LP curSalary = addJProp(baseGroup, "Текущая зарплата", salaryInMonth, 1, curMonth, 2);

       LP testNumber = addDProp(baseGroup, "Num", "Число", IntegerClass.instance, testSample);
       LP testMonth = addJProp(baseGroup, "Месяц зарплаты", numToMonth, testNumber, 1);

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
           NavigatorForm specialRecordForm = new SpecialRecordNavigatorForm(primaryData, 113, "Список расходов");
           NavigatorForm recordForm = new RecordNavigatorForm(primaryData, 114, "Список операций");
           NavigatorForm salaryForm = new SalaryNavigatorForm(primaryData, 115, "Зарплаты");

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

     private class SalaryNavigatorForm extends NavigatorForm {

        public SalaryNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objInOp = addSingleGroupObjectImplement(person, "Работник", properties, baseGroup);
            ObjectNavigator objMonth = addSingleGroupObjectImplement(month, "Обновление", properties, baseGroup);
            objMonth.groupTo.initClassView = ClassViewType.PANEL;
            objMonth.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            ObjectNavigator objExOp = addSingleGroupObjectImplement(month, "Срок", properties, baseGroup);
            ObjectNavigator objTestOp = addSingleGroupObjectImplement(testSample, "Test", properties, baseGroup);

            addObjectActions(this, objInOp);
            addObjectActions(this, objMonth);
            addObjectActions(this, objExOp);
            addObjectActions(this, objTestOp);

            addPropertyView(objInOp, objMonth, properties, baseGroup);
            addPropertyView(objInOp, objExOp, properties, baseGroup);

            //NotNullFilterNavigator documentFilter = new NotNullFilterNavigator(getPropertyImplement(salaryInMonth));
            //addFixedFilter(documentFilter);

        }
    }

     private class SpecialRecordNavigatorForm extends NavigatorForm {

        public SpecialRecordNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);
            ObjectNavigator objPersonOp = addSingleGroupObjectImplement(person, "Персонал", properties, baseGroup);
            ObjectNavigator objSalOp = addSingleGroupObjectImplement(salary, "Операция зарплата", properties, baseGroup);
            ObjectNavigator objExtraOp = addSingleGroupObjectImplement(extraCost, "Доп. затраты", properties, baseGroup);
            ObjectNavigator objPayOp = addSingleGroupObjectImplement(pay, "Выплата", properties, baseGroup);

            addPropertyView(objSalOp, objExtraOp, properties, baseGroup);
            addPropertyView(objPayOp, objSalOp, properties, baseGroup);
            addObjectActions(this, objPersonOp);
            addObjectActions(this, objSalOp);
            addObjectActions(this, objExtraOp);
            addObjectActions(this, objPayOp);

            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outPerson, objSalOp), Compare.EQUALS, objPersonOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(salaryExtraCost, objExtraOp), Compare.EQUALS, objSalOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(salaryPay, objPayOp), Compare.EQUALS, objSalOp));
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
