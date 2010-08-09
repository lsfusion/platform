package budget;


import net.sf.jasperreports.engine.JRException;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.view.form.client.DefaultFormView;
import platform.server.view.navigator.NavigatorElement;
import platform.server.view.navigator.NavigatorForm;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.view.navigator.filter.CompareFilterNavigator;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import sample.SampleBusinessLogics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class BudgetBusinessLogics extends BusinessLogics<SampleBusinessLogics> {

    public BudgetBusinessLogics(DataAdapter iAdapter, int port) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException, FileNotFoundException {
        super(iAdapter, port);
    }

    AbstractGroup salaryGroup, dateTimeGroup, extraGroup, inOperationGroup, payerGroup;

    protected void initGroups() {
        salaryGroup = new AbstractGroup("Параметры запрлаты");
        dateTimeGroup = new AbstractGroup("Параметры даты и времени");
        extraGroup = new AbstractGroup("Параметры затрат");
        inOperationGroup = new AbstractGroup("Параметры прихода");
        payerGroup = new AbstractGroup("Параметры плательщика");

    }

    AbstractCustomClass operation, inAbsOperation, outAbsOperation, absOutPerson, absOutTime, departmentAbs, extraSection, payer, payerAbs;

    ConcreteCustomClass currency, exOperation, section, inOperation, outOperation, extraCost, person, pay, absMonth, mission, misOperation, department, sectionOut, city, extraPersonSection, extraAdmSection, contractor, reimbursement;

    protected void initClasses() {
        operation = addAbstractClass("Операции", transaction);
        inAbsOperation = addAbstractClass("Абс. приход", operation);
        outAbsOperation = addAbstractClass("Абс. расход", operation);
        absOutPerson = addAbstractClass("Абс. расход сотруд.", outAbsOperation);
        absOutTime = addAbstractClass("Абс. расход времен.", outAbsOperation);
        departmentAbs = addAbstractClass("Абс. отдел", baseClass);
        payerAbs = addAbstractClass("Абс. плательщик", baseClass);

        extraCost = addConcreteClass(1, "Дополнительные затраты", absOutTime, departmentAbs, payerAbs);
        pay = addConcreteClass(2, "Выплата", absOutPerson, absOutTime, payerAbs);
        exOperation = addConcreteClass(3, "Опер. конверсия", inAbsOperation, outAbsOperation, departmentAbs);
        inOperation = addConcreteClass(4, "Опер. приход", inAbsOperation, departmentAbs);
        outOperation = addConcreteClass(5, "Опер. расход", outAbsOperation, departmentAbs, payerAbs);
        misOperation = addConcreteClass(13, "Опер. расход ком.", outAbsOperation, departmentAbs, payerAbs);
        payer = addAbstractClass("Плательщик", namedObject);

        absMonth = addConcreteClass(6, "Месяц", namedObject);
        currency = addConcreteClass(8, "Валюта", namedObject);
        section = addConcreteClass(9, "Статья ком.", namedObject);
        person = addConcreteClass(10, "Сотрудник", payer);
        extraSection = addAbstractClass("Статья затрат", namedObject);
        mission = addConcreteClass(12, "Командировка", baseClass);

        department = addConcreteClass(14, "Отдел", namedObject);
        sectionOut = addConcreteClass(15, "Статья расх.", namedObject);
        city = addConcreteClass(16, "Город", namedObject);
        extraPersonSection = addConcreteClass(17, "Статья затрат перс.", extraSection);
        extraAdmSection = addConcreteClass(18, "Статья затрат админ.", extraSection);
        contractor = addConcreteClass(19, "Контрагент", payer);
        reimbursement = addConcreteClass(20, "Возмещение", transaction);
    }

    LP inSum, outSum, inCur, outCur, outPerson, outYear, outMonth, operationDepartment, personDepartment, reimbursementCurrencyIn, reimbursementPayer;
    LP balanceQuantity, salaryInMonth, missionOperation, roundSalary, dayInMonthOv, opDep, operationPayer, reimbursementDepartment, depBalanceQuantity;

    LP isWorkingMonthForPerson;

    LP personDepartSum, personDepartSumInBC, payMonthTotal, extraMonthTotal, totalOutOper, totalOutOperInBC, totalMisOper, totalMisOperInBC, monthNum, exchangeRate, nearestPredDate, nearestExchangeRate;
    LP baseCurrency, rateDate, userRateDay, dateByMY, rateDay;

    protected void initProperties() {

        operationDepartment = addDProp("operDepartment", "Отдел", department, departmentAbs);
        operationPayer = addDProp("operPayer", "Плательщик", payer, payerAbs);
        personDepartment = addDProp("personDepartment", "Отдел", department, person);

        baseCurrency = addDProp("baseCurrency", "Базовая валюта", currency);
        rateDate = addDProp("rateDate", "Дата курса", DoubleClass.instance);
        userRateDay = addDProp("userRateDay", "День месяца для курса", IntegerClass.instance, absMonth, IntegerClass.instance);
        rateDay = addSUProp(Union.OVERRIDE, addCProp(IntegerClass.instance, 1, absMonth, IntegerClass.instance), userRateDay);

        LP personStartWorkYear = addDProp(baseGroup, "personStartWorkYear", "Год начала раб.", IntegerClass.instance, person);
        LP personStartWorkMonth = addDProp("personStartWorkMonth", "Месяц начала раб.", absMonth, person);
        addJProp(baseGroup, "Месяц начала раб.", name, personStartWorkMonth, 1);
        LP personEndWorkYear = addDProp(baseGroup, "personEndWorkYear", "Год окончания раб.", IntegerClass.instance, person);
        LP personEndWorkMonth = addDProp("personEndWorkMonth", "Месяц окончания раб.", absMonth, person);
        addJProp(baseGroup, "Месяц окончания раб.", name, personEndWorkMonth, 1);

        addJProp(payerGroup, "Плательщик", name, operationPayer, 1);
        LP multiplyDouble2 = addMFProp(DoubleClass.instance, 2);
        LP divDouble = addSFProp("((prm1+0.0)/(prm2))", DoubleClass.instance, 2);
        LP calcCoef = addSFProp("((prm1+0.0)/((prm2)*8))", DoubleClass.instance, 2);
        LP calcExtraCoef = addSFProp("(round((0.0+(prm1)*(prm2))/(prm3)))", DoubleClass.instance, 3);
        LP roundMult = addSFProp("(round((prm1)*(prm2)))", IntegerClass.instance, 2);
        LP hourInDay = addSFProp("((prm1)*8)", IntegerClass.instance, 1);
        LP yearInDate = addSFProp("(extract(year from prm1))", IntegerClass.instance, 1);
        LP extMonthInDate = addSFProp("(extract(month from prm1))", IntegerClass.instance, 1);
        LP dateBy = addSFProp("(to_date(cast(prm1 as varchar) || ' ' || cast(prm2 as varchar) || ' ' || cast(prm3 as varchar) || ' ', 'DD MM YYYY'))", DateClass.instance, 3);

        monthNum = addOProp("Номер месяца", addJProp(and1, addCProp(IntegerClass.instance, 1), is(absMonth), 1), false, true, true, 0, 1);
        LP internalNum = addCGProp(null, "extNumToIntNum", "Внутренний номер", object(absMonth), monthNum, monthNum, 1);
        LP monthInDate = addJProp("Реальный месяц", internalNum, extMonthInDate, 1);
        dateByMY = addJProp(dateBy, rateDay, 1, 2, monthNum, 1, 2);

        LP transactionMonth = addJProp("Месяц", monthInDate, date, 1);
        LP transactionYear = addJProp("Год", yearInDate, date, 1);

        LP payRate = addDProp(baseGroup, "rateP", "Курс", DoubleClass.instance, pay);
        LP extraRate = addDProp(baseGroup, "rateExtra", "Курс", DoubleClass.instance, extraCost);
        LP extraCurrency = addDProp("curExtra", "Валюта затрат", currency, extraCost);
        addJProp(baseGroup, "Валюта затрат", name, extraCurrency, 1);
        
        exchangeRate = addDProp("exchangeRate", "Курс обмена", DoubleClass.instance, currency, currency, DateClass.instance);

        LP lessCmpDate = addJProp(and(false, true, false), object(DateClass.instance), 3, exchangeRate, 1, 2, 3, greater2, 3, 4, is(DateClass.instance), 4);
        nearestPredDate = addMGProp((AbstractGroup) null, "nearestPredDate", "Ближайшая меньшая дата", lessCmpDate, 1, 2, 4);
        nearestExchangeRate = addJProp("Ближайший курс обмена", exchangeRate, 1, 2, nearestPredDate, 1, 2, 3);

        outPerson = addDProp("personP", "Сотрудник", person, absOutPerson);
        outMonth = addDProp("outM", "Месяц", absMonth, absOutTime);
        outYear = addDProp("outY", "Год", IntegerClass.instance, absOutTime);

        reimbursementPayer = addDProp("reimbPayer", "Плательщик", payer, reimbursement);
        LP reimbursementSum = addDProp(baseGroup, "reimbSum", "Сумма", IntegerClass.instance, reimbursement);
        LP reimbursementRate = addDProp(baseGroup, "reimbRate", "Курс", DoubleClass.instance, reimbursement);
        reimbursementCurrencyIn = addDProp("reimbCurIn", "Валюта расх.", currency, reimbursement);
        LP reimbursementCurrencyOut = addDProp("reimbCurOut", "Валюта выплаты", currency, reimbursement);
        reimbursementDepartment = addDProp("reimbDep", "Отдел", department, reimbursement);
        addJProp(baseGroup, "Валюта", name, reimbursementCurrencyOut, 1);

        LP missionCity = addDProp("misCity", "Город", city, mission);
        LP departDate = addDProp(baseGroup, "depDate", "Отъезд", DateClass.instance, mission);
        LP arrivalDate = addDProp(baseGroup, "arrDate", "Приезд", DateClass.instance, mission);
        LP comment = addDProp(baseGroup, "misComm", "Коментарий", StringClass.get(50), mission);
        LP isPersonMission = addDProp(baseGroup, "isPM", "Наличие", LogicalClass.instance, person, mission);
        LP payOperationDepartment = addJProp(personDepartment, outPerson, 1);

        missionOperation = addDProp("misOp", "Командировка", mission, misOperation);

        inCur = addDProp("inCurrency", "Валюта пр.", currency, inAbsOperation);
        outCur = addDProp("outCurrency", "Валюта расх.", currency, outAbsOperation);

        LP inCurName = addJProp(baseGroup, "Валюта прих.", name, inCur, 1);
        addJProp(baseGroup, "Город", name, missionCity, 1);

        inSum = addDProp(baseGroup, "inSum", "Сумма прихода", DoubleClass.instance, inAbsOperation);
        outSum = addDProp(baseGroup, "outSum", "Сумма расхода", DoubleClass.instance, outAbsOperation);
        addJProp(baseGroup, "Курс", divDouble, inSum, 1, outSum, 1);
        LP outCurName = addJProp(baseGroup, "Валюта расх.", name, outCur, 1);


        LP misSection = addDProp("section", "Статья ком.", section, misOperation);
        LP misSectionName = addJProp(baseGroup, "Статья ком.", name, misSection, 1);

        LP outSection = addDProp("outSection", "Статья расх.", sectionOut, outOperation);
        LP outSectionName = addJProp(baseGroup, "Статья расх.", name, outSection, 1);

        LP decVal = addJProp("Расход", and1, outSum, 1, is(outAbsOperation), 1);

        LP outComment = addDProp(baseGroup, "comment", "Коментарий", StringClass.get(40), operation);
        //department
        opDep = addCUProp(operationDepartment, payOperationDepartment);
        LP incDepSum = addSGProp(baseGroup, "Приход по отделу", inSum, inCur, 1, opDep, 1);
        LP decDepSum = addSGProp(baseGroup, "Расход по отделу", decVal, outCur, 1, opDep, 1);
        depBalanceQuantity = addDUProp(baseGroup, "Ост. по отделу", incDepSum, decDepSum);
        //


        LP incSum = addSGProp("Приход по валюте", inSum, inCur, 1);
        LP decSum = addSGProp("Расход по валюте", decVal, outCur, 1);

        balanceQuantity = addDUProp("Ост. по валюте", incSum, decSum);

        //args: person, month, year
        isWorkingMonthForPerson = addJProp("isWorkingMonthForPerson", "Рабочий месяц", and(false, false, true, true),
                is(person), 1, is(absMonth), 2, is(IntegerClass.instance), 3,
                addJProp(less22, 3, 2, personStartWorkYear, 1, personStartWorkMonth, 1), 1, 2, 3,
                addJProp(greater22, 3, 2, personEndWorkYear, 1, personEndWorkMonth, 1), 1, 2, 3
        );

        addConstraint(addJProp("Время окончания работы меньше, чем время начала", greater22,
                personStartWorkYear, 1, personStartWorkMonth, 1,
                personEndWorkYear, 1, personEndWorkMonth, 1), false);

        LP extraSum = addJProp(and1, addJProp(multiplyDouble2, outSum, 1, extraRate, 1), 1, is(extraCost), 1);
        LP extraMonthTotal = addSGProp(extraGroup, "Затрачено", extraSum, outMonth, 1, outYear, 1, operationDepartment, 1, extraCurrency, 1);

        salaryInMonth = addDProp(salaryGroup, "salaryInM", "Зарплата", DoubleClass.instance, person, absMonth, IntegerClass.instance);
        LP currencyInMonth = addDProp("currencyInM", "Валюта (зарплата)", currency, person, absMonth, IntegerClass.instance);
        LP workDays = addDProp(dateTimeGroup, "workD", "Раб. дни", IntegerClass.instance, absMonth, IntegerClass.instance);
        LP dayInMonth = addDProp("dayWorkInM", "Дней отраб.", IntegerClass.instance, person, absMonth, IntegerClass.instance);
        dayInMonthOv = addSUProp(dateTimeGroup, "dayWorkInMOv", "Дней отраб.", Union.OVERRIDE, addJProp(and1, workDays, 2, 3, isWorkingMonthForPerson, 1, 2, 3), dayInMonth);
        LP hourInMonth = addDProp("hourInM", "Часов отраб.", DoubleClass.instance, person, absMonth, IntegerClass.instance);
        LP hourInMonthOv = addSUProp(dateTimeGroup, "dayWorkInMOv", "Часов отраб.",
                Union.OVERRIDE, addJProp(and1, addJProp(hourInDay, dayInMonthOv, 1, 2, 3), 1, 2, 3, isWorkingMonthForPerson, 1, 2, 3), hourInMonth);
        LP extraInMonth = addDProp(baseGroup, "extraInM", "Затраты", DoubleClass.instance, extraSection, absMonth, IntegerClass.instance, department);
        // LP extraAdminInMonth = addDProp(baseGroup, "extraAdminInM", "Админ. затраты",DoubleClass.instance, department, absMonth, IntegerClass.instance);
        LP currencyExtraInMonth = addDProp("currencyExtraInM", "Валюта (доп. затрат)", currency, extraSection, absMonth, IntegerClass.instance, department);

        addJProp(salaryGroup, "Валюта", name, currencyInMonth, 1, 2, 3);
        addJProp(baseGroup, "Валюта", name, currencyExtraInMonth, 1, 2, 3, 4);

        LP extraAdd = addDProp(baseGroup, "isAdd", "Не учитывать", LogicalClass.instance, person, extraPersonSection, absMonth, IntegerClass.instance);
        //LP isReimbursed = addDProp(payerGroup, "isReimbersed", "Возмещено", LogicalClass.instance, payerAbs);

        LP[] maxDateSal = addMGProp((AbstractGroup) null, false, new String[]{"maxYear", "maxMonth"}, new String[]{"год", "месяц"}, 1,
                addJProp(and(false, false, true, false), 4, is(absMonth), 1, is(IntegerClass.instance), 2, less22, 2, 1, 4, 3, salaryInMonth, 5, 3, 4), 3, 1, 2, 5);
        LP curSalary = addJProp(salaryGroup, "Тек. зарплата", salaryInMonth, 3, maxDateSal[1], 1, 2, 3, maxDateSal[0], 1, 2, 3);
        LP curCurrency = addJProp(currencyInMonth, 3, maxDateSal[1], 1, 2, 3, maxDateSal[0], 1, 2, 3);
        addJProp(salaryGroup, "Тек. валюта", name, curCurrency, 1, 2, 3);

        LP[] maxDateExtra = addMGProp((AbstractGroup) null, false, new String[]{"maxExtraYear", "maxExtraMonth"}, new String[]{"год", "месяц1"}, 1,
                addJProp(and(false, false, true, false), 4, is(absMonth), 1, is(IntegerClass.instance), 2, less22, 2, 1, 4, 3, extraInMonth, 5, 3, 4, 6), 3, 1, 2, 5, 6);
        LP curExtra = addJProp(baseGroup, "Тек. затраты", extraInMonth, 3, maxDateExtra[1], 1, 2, 3, 4, maxDateExtra[0], 1, 2, 3, 4, 4);
        // 3 - extraSection, 4 - department
        LP curExtraCurrency = addJProp(currencyExtraInMonth, 3, maxDateExtra[1], 1, 2, 3, 4, maxDateExtra[0], 1, 2, 3, 4, 4);
        addJProp(baseGroup, "Тек. валюта", name, curExtraCurrency, 1, 2, 3, 4);

        LP workCoeff = addJProp(calcCoef, hourInMonthOv, 1, 2, 3, addJProp(and1, workDays, 2, 3, is(person), 1), 1, 2, 3);
        roundSalary = addJProp(baseGroup, "К оплате", roundMult, workCoeff, 3, 1, 2, curSalary, 1, 2, 3);

        LP paySum = addJProp("Выплачено", multiplyDouble2, payRate, 1, outSum, 1);
        LP payTotal = addSGProp(baseGroup, "Всего заплачено", paySum, outPerson, 1, outMonth, 1, outYear, 1);

        //LP extraTotalPerson = addSGProp(addJProp(andNot1, addJProp(and(false,false), curExtra, 3, 4, 2, personDepartment, 1, is(person), 1, is(extraPersonSection), 2), 1, 2, 3, 4, extraAdd, 1, 2, 3, 4), 1, 3, 4);
        LP extraTotal = addJProp(andNot1, addJProp(and1, addJProp(curExtra, 3, 4, 2, personDepartment, 1), 1, 2, 3, 4, is(extraPersonSection), 2), 1, 2, 3, 4, extraAdd, 1, 2, 3, 4);
        LP extraTotalSum = addSGProp(extraTotal, 1, 3, 4, addJProp(curExtraCurrency, 3, 4, 2, personDepartment, 1), 1, 2, 3, 4);
        // 1 - person, 2- month, 3 - year, 4 - currency 
        LP roundExtraCur = addJProp(baseGroup, "Доп. затраты", calcExtraCoef, dayInMonthOv, 1, 2, 3, extraTotalSum, 1, 2, 3, 4, addJProp(and1, workDays, 2, 3, is(person), 1), 1, 2, 3);

        LP extraTotalPerson = addSGProp(addJProp(andNot1, addJProp(and1, addJProp(curExtra, 3, 4, 2, personDepartment, 1), 1, 2, 3, 4, is(extraPersonSection), 2), 1, 2, 3, 4, extraAdd, 1, 2, 3, 4), 1, 3, 4);
        LP roundExtraPerson = addJProp(calcExtraCoef, dayInMonthOv, 1, 2, 3, extraTotalPerson, 1, 2, 3, addJProp(and1, workDays, 2, 3, is(person), 1), 1, 2, 3);

        LP extraComPerson = addSGProp(roundExtraCur, personDepartment, 1, 2, 3, 4);
        LP extraComAdm = addSGProp(addJProp(and1, curExtra, 1, 2, 3, 4, is(extraAdmSection), 3), 4, 1, 2, curExtraCurrency, 1, 2, 3, 4);

        LP extraDepartmentTotal = addSUProp(extraGroup, "Всего затрат", Union.SUM, extraComPerson, extraComAdm);

        LP totalDebt = addSGProp(baseGroup, "Затрачено", outSum, operationPayer, 1, outCur, 1);
        LP totalReimbursement = addSGProp(baseGroup, "Возмещено", addJProp(multiplyDouble2, reimbursementSum, 1, reimbursementRate, 1), reimbursementPayer, 1, reimbursementCurrencyIn, 1);
        addDUProp(baseGroup, "Осталось", totalDebt, totalReimbursement);

        LP totalDebtDep = addSGProp(baseGroup, "Затрачено", outSum, operationPayer, 1, outCur, 1, opDep, 1);
        LP totalReimbursementDep = addSGProp(baseGroup, "Возмещено", addJProp(multiplyDouble2, reimbursementSum, 1, reimbursementRate, 1), reimbursementPayer, 1, reimbursementCurrencyIn, 1, reimbursementDepartment, 1);
        addDUProp(baseGroup, "Осталось", totalDebtDep, totalReimbursementDep);

        //LP rateBC = addJProp(nearestExchangeRate, curCurrency, 1, 2, 3, baseCurrency, addJProp(dateByMY, 1, 2), 1, 2);
        //LP salaryInBC = addJProp("К выплате в БВ", multiplyDouble2, roundSalary, 1, 2, 3, rateBC, 1, 2, 3);
        //personDepartSumInBC = addSGProp("Всего к выплате в БВ", salaryInBC, personDepartment, 3, 1, 2);

        personDepartSum = addSGProp(baseGroup, "Всего к выплате", roundSalary, personDepartment, 3, 1, 2, curCurrency, 1, 2, 3);
        payMonthTotal = addSGProp(baseGroup, "Всего выплачено", payTotal, personDepartment, 1, 2, 3, curCurrency, 2, 3, 1);

        extraMonthTotal = addSGProp(baseGroup, "Всего выплачено доп. затрат", roundExtraPerson, personDepartment, 1, 2, 3, curCurrency, 2, 3, 1);

        LP outOperVal = addJProp("Опер. расход", and1, outSum, 1, is(outOperation), 1);
        totalOutOper = addSGProp(baseGroup, "Всего по опер. расходу", outOperVal, opDep, 1, transactionMonth, 1, transactionYear, 1, outCur, 1);

        LP misOperVal = addJProp("Опер. расход ком.", and1, outSum, 1, is(misOperation), 1);
        totalMisOper = addSGProp(baseGroup, "Всего по опер. расходу ком.", misOperVal, opDep, 1, transactionMonth, 1, transactionYear, 1, outCur, 1);

        LP outRateBC = addJProp(nearestExchangeRate, outCur, 1, baseCurrency, addJProp(dateByMY, transactionMonth, 1, transactionYear, 1), 1);
        LP outOperValInBC = addJProp("Опер. расход в БВ", multiplyDouble2, outOperVal, 1, outRateBC, 1);
        totalOutOperInBC = addSGProp("Всего по опер. расходу в БВ", outOperValInBC, opDep, 1, transactionMonth, 1, transactionYear, 1);
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
        tableFactory.include("extraSum", extraSection, absMonth, IntegerClass.instance, department);
        tableFactory.include("extraCurrency", extraSection, absMonth, IntegerClass.instance, department);
        tableFactory.include("isAddToSum", person, extraPersonSection, absMonth, IntegerClass.instance);
        tableFactory.include("workDays", absMonth, IntegerClass.instance);
        tableFactory.include("adminExtra", department, absMonth, IntegerClass.instance);
        tableFactory.include("exchangeRate", currency, currency, DateClass.instance);
    }

    protected void initIndexes() {
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
    }

    NavigatorForm mainAccountForm, salesArticleStoreForm;

    protected void initNavigators() throws JRException, FileNotFoundException {

        NavigatorElement primaryData = new NavigatorElement(baseElement, 100, "Первичные данные");
        NavigatorForm specialRecordForm = addNavigatorForm(new SpecialRecordNavigatorForm(primaryData, 113, "Затраты по сотрудникам"));
        NavigatorForm recordForm = new RecordNavigatorForm(primaryData, 114, "Прочие операции");
        NavigatorForm salaryForm = addNavigatorForm(new ExtraNavigatorForm(primaryData, 115, "Дополнительные затраты"));
        NavigatorForm missionForm = new MissionNavigatorForm(primaryData, 116, "Командировка");
        NavigatorForm exchangeRatesForm = new ExchangeRatesForm(primaryData, 117, "Курсы валют");

        NavigatorElement aggregateData = new NavigatorElement(baseElement, 200, "Сводная информация");
        NavigatorForm departmentBalance = new DepartmentBalanceForm(aggregateData, 214, "Баланс по отделам");
        NavigatorForm employeeExtraSum = addNavigatorForm(new DepartmentRevenueForm(aggregateData, 218, "Обороты по отделам"));
        NavigatorForm reimbursement = addNavigatorForm(new ReimbursementForm(aggregateData, 215, "Компенсация"));
    }


    private class RecordNavigatorForm extends NavigatorForm {

        public RecordNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDepartment = addSingleGroupObjectImplement(department, "Отдел", properties, baseGroup);
            objDepartment.groupTo.initClassView = ClassViewType.PANEL;
            objDepartment.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            ObjectNavigator objInOp = addSingleGroupObjectImplement(inOperation, "Операция прихода", properties, baseGroup);
            ObjectNavigator objOutOp = addSingleGroupObjectImplement(outOperation, "Операция расхода", properties, baseGroup);
            ObjectNavigator objExOp = addSingleGroupObjectImplement(exOperation, "Операция конверсии", properties, baseGroup);

            addPropertyView(objExOp, properties, inOperationGroup);
            addPropertyView(objOutOp, properties, payerGroup);

            addObjectActions(this, objInOp);
            addObjectActions(this, objOutOp);
            addObjectActions(this, objExOp);


            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(opDep, objInOp), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(opDep, objOutOp), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(opDep, objExOp), Compare.EQUALS, objDepartment));

        }
    }

    private class ExtraNavigatorForm extends NavigatorForm {
        private ObjectNavigator objMonthOp;
        private ObjectNavigator objCur;

        public ExtraNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDepartment = addSingleGroupObjectImplement(department, "Отдел", properties, baseGroup);
            objDepartment.groupTo.initClassView = ClassViewType.PANEL;
            objDepartment.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            ObjectNavigator objExtraStateOp = addSingleGroupObjectImplement(extraSection, "Статья затрат", properties, baseGroup);
            ObjectNavigator objYearOp = addSingleGroupObjectImplement(YearClass.instance, "Год", properties, baseGroup);
            objYearOp.groupTo.initClassView = ClassViewType.PANEL;
            objYearOp.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            objMonthOp = addSingleGroupObjectImplement(absMonth, "Месяц", properties, baseGroup);
            objCur = addSingleGroupObjectImplement(currency, "Затраты", properties, baseGroup);
            ObjectNavigator objExtraOp = addSingleGroupObjectImplement(extraCost, "Доп. затраты", properties, baseGroup);

            addPropertyView(objExtraStateOp, objYearOp, objMonthOp, properties, baseGroup);
            addPropertyView(objDepartment, objMonthOp, objYearOp, objExtraStateOp, properties, baseGroup);
            addPropertyView(objDepartment, objMonthOp, objYearOp, properties, baseGroup);

            addObjectActions(this, objExtraStateOp);
            addObjectActions(this, objExtraOp);
            addPropertyView(objExtraOp, properties, payerGroup);
            //NotNullFilterNavigator documentFilter = new NotNullFilterNavigator(getPropertyImplement(salaryInMonth));
            //addFixedFilter(documentFilter);

            addPropertyView(objDepartment, objMonthOp, objYearOp, objCur, properties, extraGroup);

            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outYear, objExtraOp), Compare.EQUALS, objYearOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outMonth, objExtraOp), Compare.EQUALS, objMonthOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(operationDepartment, objExtraOp), Compare.EQUALS, objDepartment));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.get(objCur.groupTo).gridView.constraints.fillHorizontal /= 2;
            design.addIntersection(design.getGroupObjectContainer(objMonthOp.groupTo), design.getGroupObjectContainer(objCur.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            return design;
        }
    }

    private class SpecialRecordNavigatorForm extends NavigatorForm {

        private ObjectNavigator objExtraStateOp;
        private ObjectNavigator objPayOp;
        private ObjectNavigator objMonthOp;
        private ObjectNavigator objCur;

        public SpecialRecordNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);
            ObjectNavigator objDepartment = addSingleGroupObjectImplement(department, "Отдел", properties, baseGroup);
            objDepartment.groupTo.initClassView = ClassViewType.PANEL;
            objDepartment.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            ObjectNavigator objPersonOp = addSingleGroupObjectImplement(person, "Персонал", properties, baseGroup);
            ObjectNavigator objYearOp = addSingleGroupObjectImplement(YearClass.instance, "Год", properties, baseGroup);
            objYearOp.groupTo.initClassView = ClassViewType.PANEL;
            objYearOp.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            objMonthOp = addSingleGroupObjectImplement(absMonth, "Месяц", properties, baseGroup);
            objCur = addSingleGroupObjectImplement(currency, "Затраты", properties, baseGroup);
            objExtraStateOp = addSingleGroupObjectImplement(extraPersonSection, "Статья затрат", properties, baseGroup);

            objPayOp = addSingleGroupObjectImplement(pay, "Выплата", properties, baseGroup);

            addPropertyView(objPersonOp, objYearOp, objMonthOp, properties, salaryGroup);
            addPropertyView(objYearOp, objMonthOp, properties, dateTimeGroup);
            addPropertyView(objPersonOp, objYearOp, objMonthOp, properties, dateTimeGroup);
            addPropertyView(objPersonOp, objYearOp, objMonthOp, properties, baseGroup);
            addPropertyView(objPersonOp, objExtraStateOp, objYearOp, objMonthOp, properties, baseGroup);
            addPropertyView(objYearOp, objMonthOp, properties, baseGroup);
            addPropertyView(objPayOp, properties, payerGroup);

            addObjectActions(this, objPersonOp);
            addObjectActions(this, objPayOp);


            addPropertyView(objPersonOp, objYearOp, objMonthOp, objCur, properties, baseGroup);

            addFixedFilter(new NotNullFilterNavigator(addPropertyObjectImplement(isWorkingMonthForPerson, objPersonOp, objMonthOp, objYearOp)));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outPerson, objPayOp), Compare.EQUALS, objPersonOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outYear, objPayOp), Compare.EQUALS, objYearOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outMonth, objPayOp), Compare.EQUALS, objMonthOp));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(personDepartment, objPersonOp), Compare.EQUALS, objDepartment));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            design.get(objExtraStateOp.groupTo).gridView.constraints.fillHorizontal /= 3;
            design.addIntersection(design.getGroupObjectContainer(objExtraStateOp.groupTo), design.getGroupObjectContainer(objPayOp.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.get(objCur.groupTo).gridView.constraints.fillHorizontal /= 3;
            design.addIntersection(design.getGroupObjectContainer(objMonthOp.groupTo), design.getGroupObjectContainer(objCur.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }


    private class MissionNavigatorForm extends NavigatorForm {

        public MissionNavigatorForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);
            ObjectNavigator objDepartment = addSingleGroupObjectImplement(department, "Отдел", properties, baseGroup);
            objDepartment.groupTo.initClassView = ClassViewType.PANEL;
            objDepartment.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            ObjectNavigator objMission = addSingleGroupObjectImplement(mission, "Командировка", properties, baseGroup);
            ObjectNavigator objPerson = addSingleGroupObjectImplement(person, "Сотрудник", properties, baseGroup);
            ObjectNavigator objOutOp = addSingleGroupObjectImplement(misOperation, "Расх. команд.", properties, baseGroup);

            addPropertyView(objMission, objPerson, properties, baseGroup);
            addPropertyView(objOutOp, properties, payerGroup);

            addObjectActions(this, objMission);
            addObjectActions(this, objOutOp);

            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(missionOperation, objOutOp), Compare.EQUALS, objMission));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(operationDepartment, objOutOp), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(personDepartment, objPerson), Compare.EQUALS, objDepartment));
        }
    }

    private class DepartmentBalanceForm extends NavigatorForm {

        public DepartmentBalanceForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objDepartment = addSingleGroupObjectImplement(department, "Отдел", properties, baseGroup);
            objDepartment.groupTo.initClassView = ClassViewType.PANEL;
            objDepartment.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            ObjectNavigator objCur = addSingleGroupObjectImplement(currency, "Валюта", properties, baseGroup);
            ObjectNavigator objInOp = addSingleGroupObjectImplement(inAbsOperation, "Операция пр.", properties, baseGroup, true);
            ObjectNavigator objOutOp = addSingleGroupObjectImplement(outAbsOperation, "Операция расх.", properties, baseGroup, true);
            addPropertyView(properties, baseGroup, true, objCur, objInOp);
            addPropertyView(properties, baseGroup, true, objCur, objOutOp);
            addPropertyView(properties, baseGroup, false, objDepartment, objCur);

            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(inCur, objInOp), Compare.EQUALS, objCur));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outCur, objOutOp), Compare.EQUALS, objCur));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(opDep, objInOp), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(opDep, objOutOp), Compare.EQUALS, objDepartment));

        }
    }

    private class ReimbursementForm extends NavigatorForm {
        private ObjectNavigator objCurrency;
        private ObjectNavigator objDepartment;
        private ObjectNavigator objOutOp;
        private ObjectNavigator objReimbursement;

        public ReimbursementForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

            ObjectNavigator objPayer = addSingleGroupObjectImplement(payer, "Плательщик", properties, baseGroup);
            objCurrency = addSingleGroupObjectImplement(currency, "Валюта", properties, baseGroup);
            objDepartment = addSingleGroupObjectImplement(department, "Отдел", properties, baseGroup);
            objOutOp = addSingleGroupObjectImplement(payerAbs, "Оплаты", properties, baseGroup);
            objReimbursement = addSingleGroupObjectImplement(reimbursement, "Возмещение", properties, baseGroup);

            addObjectActions(this, objReimbursement);
            addPropertyView(objPayer, objCurrency, properties, baseGroup);
            addPropertyView(objPayer, objCurrency, objDepartment, properties, baseGroup);
            addPropertyView(depBalanceQuantity, objCurrency, objDepartment);
            //depBalanceQuantity
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(operationPayer, objOutOp), Compare.EQUALS, objPayer));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(reimbursementPayer, objReimbursement), Compare.EQUALS, objPayer));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(outCur, objOutOp), Compare.EQUALS, objCurrency));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(reimbursementCurrencyIn, objReimbursement), Compare.EQUALS, objCurrency));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(opDep, objOutOp), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterNavigator(addPropertyObjectImplement(reimbursementDepartment, objReimbursement), Compare.EQUALS, objDepartment));


        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = super.createDefaultRichDesign();

            //design.get(objCurrency.groupTo).gridView.constraints.fillHorizontal /= 2;
            design.addIntersection(design.getGroupObjectContainer(objCurrency.groupTo), design.getGroupObjectContainer(objDepartment.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objOutOp.groupTo), design.getGroupObjectContainer(objReimbursement.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }

    }

    private class DepartmentRevenueForm extends NavigatorForm {

        public DepartmentRevenueForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);
            ObjectNavigator objDepartment = addSingleGroupObjectImplement(department, "Отдел", properties, baseGroup);
            objDepartment.groupTo.initClassView = ClassViewType.PANEL;
            objDepartment.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            //ObjectNavigator objBaseCurrency = addSingleGroupObjectImplement(currency, "Базовая валюта", properties, baseGroup);
            //        objBaseCurrency.groupTo.initClassView = ClassViewType.PANEL;
            //        objBaseCurrency.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            addPropertyView(baseCurrency);

            ObjectNavigator objYearOp = addSingleGroupObjectImplement(YearClass.instance, "Год", properties, baseGroup);
            objYearOp.groupTo.initClassView = ClassViewType.PANEL;
            objYearOp.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
            ObjectNavigator objMonthOp = addSingleGroupObjectImplement(absMonth, "Месяц", properties, baseGroup);
            //addPropertyView(totalOutOperInBC, objDepartment, objMonthOp, objYearOp);
            //addPropertyView(personDepartSumInBC, objDepartment, objMonthOp, objYearOp);

            ObjectNavigator objCurrency = addSingleGroupObjectImplement(currency, "Валюта", properties, baseGroup);

            addPropertyView(properties, baseGroup, true, objDepartment, objMonthOp, objYearOp, objCurrency);
        }
    }

    private class ExchangeRatesForm extends NavigatorForm {

        public ExchangeRatesForm(NavigatorElement parent, int ID, String caption) {
            super(parent, ID, caption);

//            ObjectNavigator objYearOp = addSingleGroupObjectImplement(IntegerClass.instance, "Год", properties, baseGroup);
//                        objYearOp.groupTo.initClassView = ClassViewType.PANEL;
//                        objYearOp.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
//            ObjectNavigator objMonthOp = addSingleGroupObjectImplement(absMonth, "Месяц", properties, baseGroup);
//
//            addPropertyView(rateDay, objMonthOp, objYearOp);
//            addPropertyView(userRateDay, objMonthOp, objYearOp);
//            addPropertyView(dateByMY, objMonthOp, objYearOp);

            ObjectNavigator objSrcCurrency = addSingleGroupObjectImplement(currency, "Исходная валюта", properties, baseGroup);
            ObjectNavigator objDstCurrency = addSingleGroupObjectImplement(currency, "Целевая валюта", properties, baseGroup);

            ObjectNavigator objDate = addSingleGroupObjectImplement(DateClass.instance, "Дата", properties, baseGroup);
            objDate.groupTo.initClassView = ClassViewType.PANEL;
            objDate.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;

            addPropertyView(exchangeRate, objSrcCurrency, objDstCurrency, objDate);
            addPropertyView(nearestPredDate, objSrcCurrency, objDstCurrency, objDate);
            addPropertyView(nearestExchangeRate, objSrcCurrency, objDstCurrency, objDate);

            ObjectNavigator objDateRate = addSingleGroupObjectImplement(DateClass.instance, "Дата", properties, baseGroup);
            addPropertyView(exchangeRate, objSrcCurrency, objDstCurrency, objDateRate);

            addFixedFilter(new NotNullFilterNavigator(addPropertyObjectImplement(exchangeRate, objSrcCurrency, objDstCurrency, objDateRate)));
        }
    }
}
