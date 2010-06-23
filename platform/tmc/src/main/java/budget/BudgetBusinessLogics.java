package budget;


import platform.server.data.sql.DataAdapter;
import platform.server.data.Union;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.AggregateProperty;
import platform.server.logics.linear.LP;
import platform.server.logics.BusinessLogics;
import platform.server.view.navigator.*;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.view.navigator.filter.CompareFilterNavigator;
import platform.server.view.form.client.DefaultFormView;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.interop.Compare;
import platform.interop.ClassViewType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;

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

    AbstractGroup salaryGroup, dateTimeGroup;

    protected void initGroups() {
        salaryGroup = new AbstractGroup("Параметры запрлаты");
        dateTimeGroup = new AbstractGroup("Параметры даты и времени");
        

    }

    AbstractCustomClass operation, inAbsOperation, outAbsOperation, absOutPerson, absOutTime;

    ConcreteCustomClass  currency, exOperation, section, inOperation, outOperation, salary, extraCost, person, pay, absMonth, extraSection, mission, misOperation;

    protected void initClasses() {
        operation = addAbstractClass("Операции", namedObject, transaction);
        inAbsOperation = addAbstractClass("Абс. приход", operation);
        outAbsOperation = addAbstractClass("Абс. расход", operation);
        absOutPerson = addAbstractClass("Абс. расход сотруд.", outAbsOperation);
        absOutTime = addAbstractClass("Абс. расход времен.", outAbsOperation);

        extraCost = addConcreteClass(1, "Дополнительные затраты", absOutTime);
        pay = addConcreteClass(2, "Выплата", absOutPerson, absOutTime);
        exOperation = addConcreteClass(3, "Опер. конверсия", inAbsOperation, outAbsOperation);
        inOperation = addConcreteClass(4, "Опер. приход", inAbsOperation);
        outOperation = addConcreteClass(5, "Опер. расход", outAbsOperation);
        misOperation = addConcreteClass(13, "Опер. расход ком.", outAbsOperation);

        absMonth = addConcreteClass(6, "Месяц", namedObject);
        currency = addConcreteClass(8, "Валюта", namedObject);
        section = addConcreteClass(9, "Статья", namedObject);
        person = addConcreteClass(10, "Сотрудник", namedObject);
        extraSection = addConcreteClass(11, "Статья затрат", namedObject);
        mission = addConcreteClass(12, "Командировка", baseClass);
    }

    LP groupBalanceQuantity, inSum, outSum, outComment, outSection, inCur, outCur, salaryExtraCost, outPerson, outYear, outMonth;
    LP balanceQuantity, incQuantity, balanceGroupQuantity, curBalance, exRate, salaryPerson, salaryInMonth, missionOperation;

    protected void initProperties() {
        LP multiplyDouble2 = addMFProp(DoubleClass.instance, 2);
        LP calcCoef = addSFProp("((prm1)/((prm2)*8))", DoubleClass.instance, 2);
        LP calcExtraCoef = addSFProp("(round((0.0+(prm1)*(prm2))/(prm3)))", DoubleClass.instance, 3);
        LP roundMult = addSFProp("(round((prm1)*(prm2)))", IntegerClass.instance, 2);
        LP hourInDay = addSFProp("((prm1)*8)", IntegerClass.instance, 1);
        LP dayCount = addDProp("dayCount", IntegerClass.instance, absMonth);

        LP payRate = addDProp(baseGroup, "rateP", "Курс", DoubleClass.instance, pay);
        LP extraRate = addDProp(baseGroup, "rateExtra", "Курс", DoubleClass.instance, extraCost);

        outPerson = addDProp("personP", "Сотрудник", person, absOutPerson);
        outMonth = addDProp("outM", "Месяц", absMonth, absOutTime);
        outYear = addDProp("outY", "Год", IntegerClass.instance, absOutTime);

        LP missionCity = addDProp(baseGroup, "misCity", "Город", StringClass.get(15), mission);
        LP departDate = addDProp(baseGroup, "depDate", "Отъезд", DateClass.instance, mission);
        LP arrivalDate = addDProp(baseGroup, "arrDate", "Приезд", DateClass.instance, mission);
        LP comment = addDProp(baseGroup, "misComm", "Коментарий", StringClass.get(50), mission);
        LP isPersonMission = addDProp(baseGroup, "isPM", "Наличие", LogicalClass.instance, person, mission);

        missionOperation = addDProp("misOp", "Валюта пр.", mission, misOperation);

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


        LP extraSum =  addJProp(and1, addJProp(multiplyDouble2, outSum, 1, extraRate, 1), 1, is(extraCost), 1);
        LP extraMonthTotal = addSGProp(baseGroup, "Затрачено", extraSum, outMonth, 1, outYear, 1);

        salaryInMonth = addDProp(salaryGroup, "salaryInM", "Зарплата", DoubleClass.instance, person, absMonth, IntegerClass.instance);
        LP currencyInMonth = addDProp("currencyInM", currency, person, absMonth, IntegerClass.instance);
        LP workDays = addDProp(dateTimeGroup, "workD", "Раб. дни", IntegerClass.instance, absMonth, IntegerClass.instance);
        LP dayInMonth = addDProp("dayWorkInM", "Дней отраб.", IntegerClass.instance, person, absMonth, IntegerClass.instance);
        LP dayInMonthOv = addSUProp(dateTimeGroup, "dayWorkInMOv", "Дней отраб.", Union.OVERRIDE, addJProp(and1, workDays, 2, 3, is(person), 1), dayInMonth);
        LP hourInMonth = addDProp("hourInM", "Часов отраб.", DoubleClass.instance, person, absMonth, IntegerClass.instance);
        LP hourInMonthOv = addSUProp(dateTimeGroup, "dayWorkInMOv", "Часов отраб.", Union.OVERRIDE, addJProp(hourInDay, dayInMonthOv, 1, 2, 3), hourInMonth);
        LP extraInMonth = addDProp(baseGroup, "extraInM", "Затраты",DoubleClass.instance, extraSection, absMonth, IntegerClass.instance);
        LP currencyExtraInMonth = addDProp("currencyExtraInM", currency, extraSection, absMonth, IntegerClass.instance);

        addJProp(salaryGroup, "Валюта", name, currencyInMonth, 1, 2 ,3);
        addJProp(baseGroup, "Валюта", name, currencyExtraInMonth, 1, 2 ,3);

        LP extraAdd = addDProp(baseGroup, "isAdd", "Не учитывать", LogicalClass.instance, person, extraSection, absMonth, IntegerClass.instance);

        LP yearGr = addJProp(and(false, false, false, false), greater2, 2, 4, is(absMonth), 1, is(IntegerClass.instance), 2, is(absMonth), 3, is(IntegerClass.instance), 4);
        LP dateCmp = addJProp(and(false, false, false, false, false), equals2, 2, 4, groeq2,  1, 3, is(absMonth), 1, is(IntegerClass.instance), 2, is(absMonth), 3, is(IntegerClass.instance), 4);
        LP dateCompare = addSUProp(Union.OVERRIDE, yearGr, dateCmp);
       
        LP[] maxDateSal = addMGProp((AbstractGroup)null, false, new String[]{"maxYear", "maxMonth"}, new String[]{"год", "месяц"}, 1,
                addJProp(and(false, false), 4, dateCompare, 1, 2, 3, 4, salaryInMonth,  5, 3, 4), 3, 1, 2, 5);
        LP curSalary = addJProp(salaryGroup, "Тек. зарплата", salaryInMonth, 3, maxDateSal[1], 1, 2, 3, maxDateSal[0], 1, 2, 3);
        LP curCurrency = addJProp(currencyInMonth, 3, maxDateSal[1], 1, 2, 3, maxDateSal[0], 1, 2, 3);
        addJProp(salaryGroup, "Тек. валюта", name, curCurrency, 1, 2, 3);

        LP[] maxDateExtra = addMGProp((AbstractGroup)null, false, new String[]{"maxExtraYear", "maxExtraMonth"}, new String[]{"год", "месяц"}, 1,
                addJProp(and(false, false), 4, dateCompare, 1, 2, 3, 4, extraInMonth,  5, 3, 4), 3, 1, 2, 5);
        LP curExtra = addJProp(baseGroup, "Тек. затраты", extraInMonth, 3, maxDateExtra[1], 1, 2, 3, maxDateExtra[0], 1, 2, 3);
        LP curExtraCurrency = addJProp(currencyExtraInMonth, 3, maxDateExtra[1], 1, 2, 3, maxDateExtra[0], 1, 2, 3);
        addJProp(baseGroup, "Тек. валюта", name, curExtraCurrency, 1, 2, 3);

        LP workCoeff = addJProp(calcCoef, hourInMonthOv, 1, 2, 3, addJProp(and1, workDays, 2, 3, is(person), 1), 1, 2, 3);
        LP roundSalary = addJProp(baseGroup, "К оплате", roundMult, workCoeff, 3, 1, 2, curSalary, 1, 2, 3);

        LP paySum = addJProp("Выплачено", multiplyDouble2, payRate, 1, outSum, 1);
        LP payTotal = addSGProp(baseGroup, "Всего заплачено", paySum, outPerson, 1, outMonth, 1, outYear, 1);
                  
        LP extraTotal = addSGProp(addJProp(andNot1, addJProp(and1, curExtra, 3, 4, 2, is(person), 1), 1, 2, 3, 4, extraAdd, 1, 2, 3, 4), 1, 3, 4);
        LP roundExtra = addJProp(baseGroup, "Макс. затрат", calcExtraCoef, dayInMonthOv, 1, 2, 3, extraTotal, 1, 2, 3, addJProp(and1, workDays, 2, 3, is(person), 1), 1, 2, 3);

        //LP extraComSum =  addJProp(and1, addJProp(multiplyDouble2, outSum, 1, extraRate, 1), 1, is(extraCost), 1);
        //LP extraComTotal = addSGProp(baseGroup, "Общ. затр.", roundExtra, 2, 3);
        //addConstraint(addJProp("Много затрат", greater2, extraTotal, 1, 2, 3, maxExtraTotal, 2, 3, 1), false);

        /*
        LP monthNum = addJProp(baseGroup, "Номер месяца", monthNumber, mYear, 1);
        LP monthYearName = addJProp(baseGroup, "Год", yearNumber, monthYear, 1);
        LP monthInY = addSFProp("prm1*12", IntegerClass.instance, 1);
        LP mYN = addJProp("Месяцев прошло", monthInY, monthYearName, 1);
        totalMonth = addSUProp("total", "Всего",Union.SUM, mYN, monthNum);

        LP monthCompare = addJProp(groeq2, totalMonth, 1, totalMonth, 2);
        lastMonthNum = addMGProp(addJProp(and(false, false), totalMonth, 2, monthCompare, 1, 2, salaryInMonth,  3, 2), 1, 3);
        LP numToMonth = addCGProp(null , false, "maxToObject", "Ближайший месяц", object(month), totalMonth, totalMonth, 1);

        LP curMonth = addJProp("Месяц зарплаты", numToMonth, lastMonthNum, 1, 2);
        LP curSalary = addJProp(baseGroup, "Текущая зарплата", salaryInMonth, 1, curMonth, 2, 1);
        LP curCurrency = addJProp(baseGroup, "Текущая валюта", currencyInMonth, 1, curMonth, 2, 1);

        LP workerCurSalary = addJProp(baseGroup, "Зарплата", curSalary, outPerson, 1, salaryMonth, 1);
        LP salaryRest = addDUProp(baseGroup, "Осталось", workerCurSalary, payTotal);
        addConstraint(addJProp("Много выплачено", greater2, vzero, salaryRest, 1), false);
        LP workDays = addSUProp(baseGroup, "total", "Дней",Union.OVERRIDE, addJProp( dayCount, addJProp(mYear, salaryMonth, 1), 1), salaryDays);

        LP workerCurCurrency = addJProp("Валюта", curCurrency, outPerson, 1, salaryMonth, 1);
        LP salaryCurName = addJProp(baseGroup, "Назв. валюты", name, workerCurCurrency, 1);

        LP salaryMonthName = addJProp(baseGroup, "Месяц", name, addJProp( mYear, salaryMonth, 1), 1);
        LP salaryYearName = addJProp(baseGroup, "Год", yearNumber, addJProp( monthYear, salaryMonth, 1), 1);
        */
        //
        //LP payCur = addSUProp(Union.OVERRIDE, addJProp(workerCurCurrency, salaryPay, 1), outCur);
       // LP payCurName = addJProp(baseGroup, "Валюта зарп.", name, payCur, 1);
       // LP timeCur = addSUProp(baseGroup, "timeCur", "Валюта зарп.",Union.OVERRIDE, addJProp(salaryCurName, salaryPay, 1), outCurName);
    }


    protected void initConstraints() {
//        balanceQuantity.property.constraint = new PositiveConstraint();
    }

    protected void initPersistents() {
//        persistents.add((AggregateProperty) totalMonth.property);
//        persistents.add((AggregateProperty) lastMonthNum.property);
    }

    protected void initTables() {
        tableFactory.include("salary", person, absMonth, IntegerClass.instance);
        tableFactory.include("currency", person, absMonth, IntegerClass.instance);
        tableFactory.include("hour", person, absMonth, IntegerClass.instance);
        tableFactory.include("extraSum", extraSection, absMonth, IntegerClass.instance);
        tableFactory.include("extraCurrency", extraSection, absMonth, IntegerClass.instance);
        tableFactory.include("isAddToSum", person, extraSection, absMonth, IntegerClass.instance);
        tableFactory.include("workDays", absMonth, IntegerClass.instance);
    }

    protected void initIndexes() {
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User user1 = addUser("user1");
    }

    NavigatorForm mainAccountForm, salesArticleStoreForm;

    protected void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement primaryData = new NavigatorElement(baseElement, 100, "Первичные данные");
           NavigatorForm specialRecordForm = addNavigatorForm(new SpecialRecordNavigatorForm(primaryData, 113, "Список расходов"));
           NavigatorForm recordForm = new RecordNavigatorForm(primaryData, 114, "Список операций");
           NavigatorForm salaryForm = new ExtraNavigatorForm(primaryData, 115, "Затраты");
           NavigatorForm missionForm = new MissionNavigatorForm(primaryData, 116, "Командировка");

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

     private class ExtraNavigatorForm extends NavigatorForm {

        public ExtraNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objExtraStateOp = addSingleGroupObjectImplement(extraSection, "Статья затрат", properties, baseGroup);
            ObjectNavigator objYearOp = addSingleGroupObjectImplement(IntegerClass.instance, "Год", properties, baseGroup);
                        objYearOp.groupTo.initClassView = ClassViewType.PANEL;
                        objYearOp.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            ObjectNavigator objMonthOp = addSingleGroupObjectImplement(absMonth, "Месяц", properties, baseGroup);
            ObjectNavigator objExtraOp = addSingleGroupObjectImplement(extraCost, "Доп. затраты", properties, baseGroup);

            addPropertyView(objExtraStateOp, objYearOp, objMonthOp, properties, baseGroup);
            addPropertyView(objYearOp, objMonthOp, properties, baseGroup);
           
            addObjectActions(this, objExtraStateOp);
            addObjectActions(this, objMonthOp);
            addObjectActions(this, objExtraOp);
            //NotNullFilterNavigator documentFilter = new NotNullFilterNavigator(getPropertyImplement(salaryInMonth));
            //addFixedFilter(documentFilter);

            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outYear, objExtraOp), Compare.EQUALS, objYearOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outMonth, objExtraOp), Compare.EQUALS, objMonthOp));


        }
    }

     private class SpecialRecordNavigatorForm extends NavigatorForm {

         private ObjectNavigator objExtraStateOp;
         private ObjectNavigator objPayOp;

         public SpecialRecordNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);
            ObjectNavigator objPersonOp = addSingleGroupObjectImplement(person, "Персонал", properties, baseGroup);
            ObjectNavigator objYearOp = addSingleGroupObjectImplement(IntegerClass.instance, "Год", properties, baseGroup);
                        objYearOp.groupTo.initClassView = ClassViewType.PANEL;
                        objYearOp.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            ObjectNavigator objMonthOp = addSingleGroupObjectImplement(absMonth, "Операция зарплата", properties, baseGroup);
            objExtraStateOp = addSingleGroupObjectImplement(extraSection, "Статья затрат", properties, baseGroup);

            objPayOp = addSingleGroupObjectImplement(pay, "Выплата", properties, baseGroup);

            addPropertyView(objPersonOp, objYearOp, objMonthOp, properties, salaryGroup);
            addPropertyView(objYearOp, objMonthOp, properties, dateTimeGroup);
            addPropertyView(objPersonOp, objYearOp, objMonthOp, properties, dateTimeGroup);
            addPropertyView(objPersonOp, objYearOp, objMonthOp, properties, baseGroup);
            addPropertyView(objPersonOp, objExtraStateOp, objYearOp, objMonthOp, properties, baseGroup);
            addPropertyView(objYearOp, objMonthOp, properties, baseGroup);
            
            addObjectActions(this, objPersonOp);
            addObjectActions(this, objMonthOp);

            addObjectActions(this, objPayOp);

            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outPerson, objPayOp), Compare.EQUALS, objPersonOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outYear, objPayOp), Compare.EQUALS, objYearOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outMonth, objPayOp), Compare.EQUALS, objMonthOp));
            /*
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outPerson, objExtraOp), Compare.EQUALS, objPersonOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outYear, objExtraOp), Compare.EQUALS, objYearOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outMonth, objExtraOp), Compare.EQUALS, objMonthOp));
            */
        }

         @Override
         public DefaultFormView createDefaultRichDesign() {

             DefaultFormView design = super.createDefaultRichDesign();

             design.get(objExtraStateOp.groupTo).gridView.constraints.fillHorizontal /= 2;
             design.addIntersection(design.getGroupObjectContainer(objExtraStateOp.groupTo), design.getGroupObjectContainer(objPayOp.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

             return design;
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

     private class MissionNavigatorForm extends NavigatorForm {

        public MissionNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objMission = addSingleGroupObjectImplement(mission, "Командировка", properties, baseGroup);
            ObjectNavigator objPerson = addSingleGroupObjectImplement(person, "Сотрудник", properties, baseGroup);
            ObjectNavigator objOutOp = addSingleGroupObjectImplement(misOperation, "Расх. команд.", properties, baseGroup);

            addPropertyView(objMission, objPerson, properties, baseGroup);

            addObjectActions(this, objPerson);
            addObjectActions(this, objMission);
            addObjectActions(this, objOutOp);

            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(missionOperation, objOutOp), Compare.EQUALS, objMission));
            //addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outCur, objOutOp), Compare.EQUALS, objCur));
        }
    }

}
