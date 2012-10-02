package budget;

import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.expr.query.PartitionType;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.filter.*;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.group.AbstractGroup;

/**
 * User: DAle
 * Date: 26.05.11
 * Time: 15:42
 */

public class BudgetLogicsModule extends LogicsModule {

    public BudgetLogicsModule(BaseLogicsModule<BudgetBusinessLogics> baseLM) {
        super("BudgetLogicsModule");
        setBaseLogicsModule(baseLM);
    }

    AbstractGroup salaryGroup, dateTimeGroup, personGroup, extraGroup, outGroup, inOperationGroup, payerGroup, baseCurGroup;
    AbstractCustomClass operation, inAbsOperation, outAbsOperation, absOutPerson, absOutTime, departmentAbs, extraSection, payer, payerAbs;

    ConcreteCustomClass exOperation, section, inOperation, outOperation, extraCost, person, pay, absMonth, mission, misOperation, department, sectionOut, city, extraPersonSection, extraAdmSection, contractor, reimbursement, vacation;
    ConcreteCustomClass incomeCash;
    ConcreteCustomClass incomeNotCash;
    ConcreteCustomClass outcomeCost;
    AbstractCustomClass investor;
    ConcreteCustomClass employeeInvestor, nonEmployeeInvestor;
    AbstractCustomClass investment, investmentMoney;
    ConcreteCustomClass investmentCash, investmentNotCash;
    ConcreteCustomClass investmentNotMoney;
    ConcreteCustomClass transfer, incomeProfit;

    @Override
    public void initModule() {
    }

    @Override
    public void initClasses() {
        initBaseClassAliases();

        operation = addAbstractClass("operation", "Операции", baseLM.transaction);
        inAbsOperation = addAbstractClass("inAbsOperation", "Абс. приход", operation);
        outAbsOperation = addAbstractClass("outAbsOperation", "Абс. расход", operation);
        absOutPerson = addAbstractClass("absOutPerson", "Абс. расход сотруд.", outAbsOperation);
        absOutTime = addAbstractClass("absOutTime", "Абс. расход времен.", outAbsOperation);
        departmentAbs = addAbstractClass("departmentAbs", "Абс. отдел", baseClass);
        payerAbs = addAbstractClass("payerAbs", "Абс. плательщик", baseClass);

        extraCost = addConcreteClass("extraCost", "Дополнительные затраты", absOutTime, departmentAbs, payerAbs);
        pay = addConcreteClass("pay", "Выплата", absOutPerson, absOutTime, payerAbs);
        exOperation = addConcreteClass("exOperation", "Опер. конверсия", inAbsOperation, outAbsOperation, departmentAbs);
        inOperation = addConcreteClass("inOperation", "Опер. приход", inAbsOperation, departmentAbs);
        outOperation = addConcreteClass("outOperation", "Опер. расход", outAbsOperation, departmentAbs, payerAbs);
        misOperation = addConcreteClass("misOperation", "Опер. расход ком.", outAbsOperation, departmentAbs, payerAbs);
        payer = addAbstractClass("payer", "Плательщик", baseClass.named);

        transfer = addConcreteClass("transfer", "Перемещение", operation);

        absMonth = addConcreteClass("absMonth", "Месяц", baseClass.named);
        section = addConcreteClass("section", "Статья ком.", baseClass.named);
        person = addConcreteClass("person", "Сотрудник", payer);
        extraSection = addAbstractClass("extraSection", "Статья затрат", baseClass.named);
        mission = addConcreteClass("mission", "Командировка", baseClass);

        department = addConcreteClass("department", "Отдел", baseClass.named);
        sectionOut = addConcreteClass("sectionOut", "Статья расх.", baseClass.named);
        city = addConcreteClass("city", "Город", baseClass.named);
        extraPersonSection = addConcreteClass("extraPersonSection", "Статья затрат перс.", extraSection);
        extraAdmSection = addConcreteClass("extraAdmSection", "Статья затрат админ.", extraSection);
        contractor = addConcreteClass("contractor", "Контрагент", payer);
        reimbursement = addConcreteClass("reimbursement", "Возмещение", baseLM.transaction);
        vacation = addConcreteClass("vacation", "Отпуск", baseClass);

        incomeCash = addConcreteClass("incomeCash", "Приход по налу", inAbsOperation, departmentAbs);
        incomeNotCash = addConcreteClass("incomeNotCash", "Приход по безналу", inAbsOperation, departmentAbs);
        outcomeCost = addConcreteClass("outcomeCost", "Расходы на обороты", outAbsOperation, departmentAbs, payerAbs);

        incomeProfit = addConcreteClass("incomeProfit","Доход отдела",operation);

        investor = addAbstractClass("investor", "Инвестор", baseClass.named);

        employeeInvestor = addConcreteClass("employeeInvestor", "Инвестор (сотрудник)", person, investor);
        nonEmployeeInvestor = addConcreteClass("nonEmployeeInvestor", "Внешний инвестор", investor);

        investment = addAbstractClass("investment", "Инвестиция", baseClass);

        investmentMoney = addAbstractClass("investmentMoney", "Инвестиция (деньги)", investment);
        investmentCash = addConcreteClass("investmentCash", "Инвестиция (нал)", incomeCash, investmentMoney);
        investmentNotCash = addConcreteClass("investmentNotCash", "Инвестиция (безнал)", incomeNotCash, investmentMoney);

        investmentNotMoney = addConcreteClass("investmentNotMoney", "Инвестиция (немат.)", investment, baseLM.transaction);
    }

    @Override
    public void initTables() {
        addTable("salary", person, absMonth, YearClass.instance);
        addTable("currency", person, absMonth, YearClass.instance);
        addTable("hour", person, absMonth, YearClass.instance);
        addTable("extraSum", extraSection, absMonth, YearClass.instance, department);
        addTable("extraCurrency", extraSection, absMonth, YearClass.instance, department);
        addTable("isAddToSum", person, extraPersonSection, absMonth, YearClass.instance);
        addTable("workDays", absMonth, YearClass.instance);
        addTable("adminExtra", department, absMonth, YearClass.instance);
        addTable("exchangeRate", baseLM.currency, baseLM.currency, DateClass.instance);
    }

    @Override
    public void initGroups() {
        initBaseGroupAliases();

        salaryGroup = addAbstractGroup("salaryGroup", "Параметры запрлаты");
        dateTimeGroup = addAbstractGroup("dateTimeGroup", "Параметры даты и времени");
        personGroup = addAbstractGroup("personGroup", "Затраты по сотрудникам");
        extraGroup = addAbstractGroup("extraGroup", "Дополнительные затраты");
        outGroup = addAbstractGroup("outGroup", "Оперативный расход");
        inOperationGroup = addAbstractGroup("inOperationGroup", "Параметры прихода");
        payerGroup = addAbstractGroup("payerGroup", "Параметры плательщика");
        baseCurGroup = addAbstractGroup("baseCurGroup", "Сумма в базовой валюте");
    }

    LCP inSum, outSum, inCur, outCur, outPerson, outYear, outMonth;
    LCP operationDepartment, nameOperationDepartment;
    LCP personDepartment, reimbursementCurrencyIn, reimbursementPayer, vacationPerson;
    LCP balanceQuantity, salaryInMonth, missionOperation, roundSalary, dayInMonthOv, opDep, operationPayer, reimbursementDepartment, depBalanceQuantity, roundSalarySum, payTotalSum;

    LCP isWorkingMonthForPerson;

    LCP personDepartSum, personDepartSumInBC, payMonthTotal, extraMonthTotal, totalOutOper, departmentOutOperInBC, totalMisOper, totalMisOperInBC, monthNum, exchangeRate, nearestPredDate, nearestExchangeRate, exchangeBaseRateCurrencyDate, nearestExchangeRateOp;
    LCP exchangeRateCurrencyTransaction;
    LCP baseCurrency, baseCurrencyName;
    LCP rateDate, userRateDay, dateByMY, rateDay, incomeOutcome;

    LCP investorInvestment, nameInvestorInvestment;

    LCP sumInvestmentCash, sumInvestmentNotCash, sumInvestmentMoney;
    LCP curInvestmentCash, curInvestmentNotCash, curInvestmentMoney;

    LCP sumInvestment, curInvestment, nameCurInvestment;

    LCP sumInvestmentNotMoney;
    LCP curInvestmentNotMoney;
    LCP exchangeBaseRateInvestment;
    LCP sumBaseInvestment;

    LCP sumInvestmentInvestor;
    LCP investmentTotal;
    LCP shareInvestor;
    
    LCP inactiveAbsOutPerson;

    @Override
    public void initProperties() {

        operationDepartment = addDProp(idGroup, "operDepartment", "Отдел (ИД)", department, departmentAbs);
        nameOperationDepartment = addJProp(baseGroup, "nameOperationDepartment", "Отдел", baseLM.name, operationDepartment, 1);
        operationPayer = addDProp("operPayer", "Плательщик", payer, payerAbs);
        personDepartment = addDProp("personDepartment", "Отдел", department, person);

        baseCurrency = addDProp("baseCurrency", "Базовая валюта", baseLM.currency);
        baseCurrencyName = addJProp(baseGroup, "Базовая валюта", baseLM.name, baseCurrency);

        rateDate = addDProp("rateDate", "Дата курса", DoubleClass.instance);
        userRateDay = addDProp("userRateDay", "День месяца для курса", IntegerClass.instance, absMonth, YearClass.instance);
        rateDay = addSUProp(Union.OVERRIDE, addCProp(IntegerClass.instance, 1, absMonth, YearClass.instance), userRateDay);

        LCP daysInMonth = addDProp(baseGroup, "daysInMonth", "Дней в месяце", IntegerClass.instance, absMonth);

        LCP personStartWorkYear = addDProp(baseGroup, "personStartWorkYear", "Год начала раб.", YearClass.instance, person);
        LCP personStartWorkMonth = addDProp("personStartWorkMonth", "Месяц начала раб.", absMonth, person);
        addJProp(baseGroup, "Месяц начала раб.", baseLM.name, personStartWorkMonth, 1);
        LCP personStartWorkDay = addDProp(baseGroup, "personStartWorkDay", "День начала раб.", IntegerClass.instance, person);
        LCP personEndWorkYear = addDProp(baseGroup, "personEndWorkYear", "Год окончания раб.", YearClass.instance, person);
        LCP personEndWorkMonth = addDProp("personEndWorkMonth", "Месяц окончания раб.", absMonth, person);
        addJProp(baseGroup, "Месяц окончания раб.", baseLM.name, personEndWorkMonth, 1);
        LCP personEndWorkDay = addDProp(baseGroup, "personEndWorkDay", "День окончания раб.", IntegerClass.instance, person);

        addJProp(payerGroup, "Плательщик", baseLM.name, operationPayer, 1);
        LCP multiplyDouble2 = addMFProp(2);
        LCP divDouble = addSFProp("((prm1+0.0)/(prm2))", DoubleClass.instance, 2);
        LCP calcCoef = addSFProp("((prm1+0.0)/((prm2)*8))", DoubleClass.instance, 2);
        LCP calcExtraCoef = addSFProp("(round((0.0+(prm1)*(prm2))/(prm3)))", DoubleClass.instance, 3);
        LCP roundMult = addSFProp("(round((prm1)*(prm2)))", IntegerClass.instance, 2);
        LCP hourInDay = addSFProp("((prm1)*8)", IntegerClass.instance, 1);
        LCP dayInYear = addSFProp("(extract(day from prm1))", IntegerClass.instance, 1);
        LCP yearInDate = addSFProp("(extract(year from prm1))", IntegerClass.instance, 1);
        LCP extMonthInDate = addSFProp("(extract(month from prm1))", IntegerClass.instance, 1);
        LCP dateBy = addSFProp("(to_date(cast(prm1 as varchar) || ' ' || cast(prm2 as varchar) || ' ' || cast(prm3 as varchar) || ' ', 'DD MM YYYY'))", DateClass.instance, 3);
        LCP daysBetweenDates = addSFProp("(prm1-prm2)", IntegerClass.instance, 2);
        LCP sumByRate = addSFProp("(prm1*prm2)", DoubleClass.instance, 2);
        LCP calcPerCent = addSFProp("(prm1*prm2/100)", DoubleClass.instance, 2);

        monthNum = addOProp("Номер месяца", PartitionType.SUM, addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), is(absMonth), 1), true, true, 0, 1);
        LCP internalNum = addAGProp("extNumToIntNum", "Внутренний номер", monthNum);
        LCP monthInDate = addJProp("Реальный месяц", internalNum, extMonthInDate, 1);
        dateByMY = addJProp("Дата курса для года/месяца", dateBy, rateDay, 1, 2, monthNum, 1, 2);

        LCP personEndDate = addJProp(dateBy, personEndWorkDay, 1, addJProp(monthNum, personEndWorkMonth, 1), 1, personEndWorkYear, 1);
        LCP totalDaysWorkedTillDate = addJProp(baseGroup, "daysWorkedTillDate", "Дней отработано", daysBetweenDates,
                addSUProp(Union.OVERRIDE, addJProp(baseLM.and1, baseLM.currentDate, is(person), 1), personEndDate), 1,
                addJProp(dateBy, personStartWorkDay, 1, addJProp(monthNum, personStartWorkMonth, 1), 1, personStartWorkYear, 1), 1);

        vacationPerson = addDProp("vacationPerson", "Сотрудник", person, vacation);
        LCP vacationPersonName = addJProp(baseGroup, "vacationPersonName", "Имя сотрудника", baseLM.name, vacationPerson, 1);
        LCP vacationStartDate = addDProp(baseGroup, "vacationStartDate", "Начало отпуска", DateClass.instance, vacation);
        LCP vacationEndDate = addDProp(baseGroup, "vacationEndDate", "Окончание отпуска", DateClass.instance, vacation);
        LCP vacationDaysQuantity = addJProp(baseGroup, "vacationDaysQuantity", "Длина отпуска, дней", daysBetweenDates, vacationEndDate, 1, vacationStartDate, 1);
        LCP vacationWorkDays = addDProp(baseGroup, "vacationWorkDays", "Рабочих дней за период отпуска", IntegerClass.instance, vacation);
        LCP totalVacationDays = addSGProp(baseGroup, "totalVacationDays", "Отпуск, календарных дней", vacationDaysQuantity, vacationPerson, 1);
        LCP totalVacationWorkDays = addSGProp(baseGroup, "totalVacationWorkDays", "Отпуск, рабочих дней", vacationWorkDays, vacationPerson, 1);

        LCP transactionMonth = addJProp("Месяц", monthInDate, baseLM.date, 1);
        LCP transactionYear = addJProp("Год", yearInDate, baseLM.date, 1);

        LCP payRate = addDProp(baseGroup, "rateP", "Курс", DoubleClass.instance, pay);
        LCP extraRate = addDProp(baseGroup, "rateExtra", "Курс", DoubleClass.instance, extraCost);
        LCP extraCurrency = addDProp("curExtra", "Валюта затрат", baseLM.currency, extraCost);
        addJProp(baseGroup, "Валюта затрат", baseLM.name, extraCurrency, 1);

        exchangeRate = addDProp("exchangeRate", "Курс обмена", DoubleClass.instance, baseLM.currency, baseLM.currency, DateClass.instance);

        LCP lessCmpDate = addJProp(and(false, true, false), object(DateClass.instance), 3, exchangeRate, 1, 2, 3, baseLM.greater2, 3, 4, is(DateClass.instance), 4);
        nearestPredDate = addMGProp((AbstractGroup) null, "nearestPredDate", "Ближайшая меньшая дата", lessCmpDate, 1, 2, 4);
        nearestExchangeRate = addJProp("Ближайший курс обмена", exchangeRate, 1, 2, nearestPredDate, 1, 2, 3);
        exchangeBaseRateCurrencyDate = addJProp("Ближайший курс обмена к БВ", nearestExchangeRate, 1, 2, baseCurrency);

        // чисто проталкивание ключей внутрь

        LCP lessCmpDateOp = addJProp(and(false, true, false), object(DateClass.instance), 3, exchangeRate, 1, 2, 3, addJProp(baseLM.greater2, 1, baseLM.date, 2), 3, 4, baseLM.date, 4);
        LCP nearestPredDateOp = addMGProp((AbstractGroup) null, "nearestPredDateOp", "Ближайшая меньшая дата операции", lessCmpDateOp, 1, 2, 4);
        nearestExchangeRateOp = addJProp("nearestExchangeRateOp","Ближайший курс обмена операции", exchangeRate, 1, 2, nearestPredDateOp, 1, 2, 3);

        exchangeRateCurrencyTransaction = addJProp("exchangeRateCurrencyTransaction", "Ближайший курс", nearestExchangeRateOp, 1, baseCurrency, 2);

        LCP lessCmpDateMY = addJProp(and(false, true, false, false), object(DateClass.instance), 3, exchangeRate, 1, 2, 3, addJProp(baseLM.greater2, 1, dateByMY, 2, 3), 3, 4, 5, is(absMonth), 4, is(YearClass.instance), 5);
        LCP nearestPredDateMY = addMGProp((AbstractGroup) null, "nearestPredDateMY", "Ближайшая меньшая дата курса месяца/года", lessCmpDateMY, 1, 2, 4, 5);
        LCP nearestExchangeRateMY = addJProp("Ближайший курс обмена месяца/года", exchangeRate, 1, 2, nearestPredDateMY, 1, 2, 3, 4);

        outPerson = addDProp("personP", "Сотрудник", person, absOutPerson);
        outMonth = addDProp("outM", "Месяц", absMonth, absOutTime);
        outYear = addDProp("outY", "Год", YearClass.instance, absOutTime);

        inactiveAbsOutPerson = addJProp(baseLM.greater2, baseLM.currentDate, personEndDate, 1);

        reimbursementPayer = addDProp("reimbPayer", "Плательщик", payer, reimbursement);
        LCP reimbursementSum = addDProp(baseGroup, "reimbSum", "Сумма", IntegerClass.instance, reimbursement);
        LCP reimbursementRate = addDProp(baseGroup, "reimbRate", "Курс", DoubleClass.instance, reimbursement);
        reimbursementCurrencyIn = addDProp("reimbCurIn", "Валюта расх.", baseLM.currency, reimbursement);
        LCP reimbursementCurrencyOut = addDProp("reimbCurOut", "Валюта выплаты", baseLM.currency, reimbursement);
        reimbursementDepartment = addDProp("reimbDep", "Отдел", department, reimbursement);
        addJProp(baseGroup, "Валюта", baseLM.name, reimbursementCurrencyOut, 1);

        LCP missionCity = addDProp("misCity", "Город", city, mission);
        LCP departDate = addDProp(baseGroup, "depDate", "Отъезд", DateClass.instance, mission);
        LCP arrivalDate = addDProp(baseGroup, "arrDate", "Приезд", DateClass.instance, mission);
        LCP comment = addDProp(baseGroup, "misComm", "Коментарий", StringClass.get(50), mission);
        LCP isPersonMission = addDProp(baseGroup, "isPM", "Наличие", LogicalClass.instance, person, mission);
        LCP payOperationDepartment = addJProp(personDepartment, outPerson, 1);

        missionOperation = addDProp("misOp", "Командировка", mission, misOperation);

        inCur = addDProp("inCurrency", "Валюта пр.", baseLM.currency, inAbsOperation);
        outCur = addDProp("outCurrency", "Валюта расх.", baseLM.currency, outAbsOperation);

        LCP inCurName = addJProp(baseGroup, "Валюта прих.", baseLM.name, inCur, 1);
        addJProp(baseGroup, "Город", baseLM.name, missionCity, 1);

        inSum = addDProp(baseGroup, "inSum", "Сумма прихода", DoubleClass.instance, inAbsOperation);
        outSum = addDProp(baseGroup, "outSum", "Сумма расхода", DoubleClass.instance, outAbsOperation);
        addJProp(baseGroup, "Курс", divDouble, inSum, 1, outSum, 1);
        LCP outCurName = addJProp(baseGroup, "Валюта расх.", baseLM.name, outCur, 1);


        LCP misSection = addDProp("section", "Статья ком.", section, misOperation);
        LCP misSectionName = addJProp(baseGroup, "Статья ком.", baseLM.name, misSection, 1);

        LCP outSection = addDProp("outSection", "Статья расх.", sectionOut, outOperation);
        LCP outSectionName = addJProp(baseGroup, "Статья расх.", baseLM.name, outSection, 1);

        LCP decVal = addJProp("Расход", baseLM.and1, outSum, 1, is(outAbsOperation), 1);

        //department


        opDep = addCUProp(operationDepartment, payOperationDepartment);

        LCP currencyTransfer = addDProp("currencyTransfer", "Валюта перем.", baseLM.currency, transfer);
        LCP nameCurrencyTransfer = addJProp(baseGroup, "nameCurrencyTransfer", "Валюта перемещения", baseLM.name, currencyTransfer, 1);
        LCP depFrom = addDProp("depFrom", "Отдел отправитель", department, transfer);
        LCP depFromName = addJProp(baseGroup, "depFromName", "Отдел отправитель", baseLM.name, depFrom, 1);
        LCP depTo = addDProp("depTo", "Отдель получатель", department, transfer);
        LCP depToName = addJProp(baseGroup, "depToName", "Отдел получатель", baseLM.name, depTo, 1);
        LCP valueTransfer = addDProp(baseGroup, "valueTransfer", "Сумма перемещения", DoubleClass.instance, transfer);
        LCP incSumTransfer = addSGProp("incSumTransfer", "Перемещение в отдел", valueTransfer, currencyTransfer, 1, depTo, 1);
        LCP outSumTransfer = addSGProp("outSumTransfer", "Перемещение от отдела", valueTransfer, currencyTransfer, 1, depFrom, 1);

        LCP currencyIncomeProfit = addDProp("currencyIncomeProfit", "Валюта выр.", baseLM.currency, incomeProfit);
        LCP nameCurrencyIncomeProfit = addJProp(baseGroup, "nameCurrencyIncomeProfit", "Валюта выручки", baseLM.name, currencyIncomeProfit, 1);
        LCP depProfit = addDProp("depProfit", "Отдел", department, incomeProfit);
        LCP nameDepProfit = addJProp(baseGroup, "nameDepProfit", "Отдел." ,baseLM.name, depProfit, 1);
        LCP valueProfit = addDProp(baseGroup, "valueProfit","Доход", DoubleClass.instance, incomeProfit);
        LCP sumProfit =addSGProp("sumProfit", "Сумма дохода", valueProfit, currencyIncomeProfit, 1, depProfit, 1);

        LCP incDepSumOut = addSGProp("Приход по отделу внешний", inSum, inCur, 1, opDep, 1);
        LCP decDepSumOut = addSGProp("Расход по отделу внешний", decVal, outCur, 1, opDep, 1);

        LCP incDepSumTr = addSUProp("incDepSumTr", "Приход по отделу c перемещением", Union.SUM,incDepSumOut, incSumTransfer);
        LCP decDepSum = addSUProp(baseGroup, "decDepSum", "Расход по отделу", Union.SUM, decDepSumOut, outSumTransfer);

        LCP incDepSum = addSUProp(baseGroup, "incDepSum", "Приход по отделу", Union.SUM,incDepSumTr, sumProfit);

        depBalanceQuantity = addDUProp(baseGroup, "Ост. по отделу", incDepSum, decDepSum);

        LCP perCent = addDProp(baseGroup, "perCent", "Процент", DoubleClass.instance, department);

        incomeOutcome = addDProp("incomeOutcome", "Расход по приходу", incomeNotCash, outcomeCost);

        LCP inCost = addJProp(baseGroup, "inCost", "Стоимость оборотов", calcPerCent, addJProp(baseLM.and1, inSum, 1, is(incomeNotCash), 1), 1, addJProp(perCent, operationDepartment, 1), 1);
        LCP payRevenueRate = addDProp(baseGroup, "payRevenueRate", "Курс", DoubleClass.instance, outcomeCost);
        LCP paySumInCur = addJProp("paySumInCur", "Приведенная сумма расхода", sumByRate, outSum, 1, payRevenueRate, 1);
        LCP totalOutcome = addSGProp(baseGroup, "totalOutcome", "Всего выплачено", paySumInCur, incomeOutcome, 1);

        LCP outComment = addDProp(baseGroup, "comment", "Коментарий", StringClass.get(40), operation);

        LCP incSum = addSGProp("Приход по валюте", inSum, inCur, 1);
        LCP decSum = addSGProp("Расход по валюте", decVal, outCur, 1);

        balanceQuantity = addDUProp("Ост. по валюте", incSum, decSum);

        //args: person, month, year
        isWorkingMonthForPerson = addJProp("isWorkingMonthForPerson", "Рабочий месяц", and(false, false, true, true),
                is(person), 1, is(absMonth), 2, is(YearClass.instance), 3,
                addJProp(baseLM.less22, 3, 2, personStartWorkYear, 1, personStartWorkMonth, 1), 1, 2, 3,
                addJProp(baseLM.greater22, 3, 2, personEndWorkYear, 1, personEndWorkMonth, 1), 1, 2, 3
        );

        /*addConstraint(addJProp("Время окончания работы меньше, чем время начала", greater22,
                personStartWorkYear, 1, personStartWorkMonth, 1,
                personEndWorkYear, 1, personEndWorkMonth, 1), false);*/

        addConstraint(addJProp("Неверный день окончания работы", baseLM.greater2,
                personEndWorkDay, 1,
                addJProp(daysInMonth, personEndWorkMonth, 1), 1), false);
        addConstraint(addJProp("Неверный день начала работы", baseLM.greater2,
                personStartWorkDay, 1,
                addJProp(daysInMonth, personStartWorkMonth, 1), 1), false);

        addConstraint(addJProp("Дата окончания работы меньше даты начала работы", baseLM.greater2,
                addJProp(dateBy, personStartWorkDay, 1, addJProp(monthNum, personStartWorkMonth, 1), 1, personStartWorkYear, 1), 1,
                addJProp(dateBy, personEndWorkDay, 1, addJProp(monthNum, personEndWorkMonth, 1), 1, personEndWorkYear, 1), 1), false);

        addConstraint(addJProp("Дата окончания отпуска меньше даты начала отпуска", baseLM.greater2,
                baseLM.vzero, addJProp(daysBetweenDates, vacationEndDate, 1, vacationStartDate, 1), 1), false);

        addConstraint(addJProp("Количество рабочих дней превышает длину отпуска", baseLM.greater2,
                vacationWorkDays, 1, vacationDaysQuantity, 1), false);

        addConstraint(addJProp("Расходы превышают доходы", baseLM.greater2, totalOutcome, 1, inCost, 1), false);


        LCP extraSum = addJProp(baseLM.and1, addJProp(multiplyDouble2, outSum, 1, extraRate, 1), 1, is(extraCost), 1);

        salaryInMonth = addDProp(salaryGroup, "salaryInM", "Зарплата", DoubleClass.instance, person, absMonth, YearClass.instance);
        LCP currencyInMonth = addDProp("currencyInM", "Валюта (зарплата)", baseLM.currency, person, absMonth, YearClass.instance);
        LCP workDays = addDProp(dateTimeGroup, "workD", "Раб. дни", IntegerClass.instance, absMonth, YearClass.instance);
        LCP dayInMonth = addDProp("dayWorkInM", "Дней отраб.", IntegerClass.instance, person, absMonth, YearClass.instance);
        dayInMonthOv = addSUProp(dateTimeGroup, "dayWorkInMOv", "Дней отраб.", Union.OVERRIDE, addJProp(baseLM.and1, workDays, 2, 3, isWorkingMonthForPerson, 1, 2, 3), dayInMonth);
        LCP hourInMonth = addDProp("hourInM", "Часов отраб.", DoubleClass.instance, person, absMonth, YearClass.instance);
        LCP hourInMonthOv = addSUProp(dateTimeGroup, "hourInMonthOv", "Часов отраб.",
                Union.OVERRIDE, addJProp(baseLM.and1, addJProp(hourInDay, dayInMonthOv, 1, 2, 3), 1, 2, 3, isWorkingMonthForPerson, 1, 2, 3), hourInMonth);
        LCP extraInMonth = addDProp(baseGroup, "extraInM", "Затраты", DoubleClass.instance, extraSection, absMonth, YearClass.instance, department);
        // LCP extraAdminInMonth = addDProp(baseGroup, "extraAdminInM", "Админ. затраты",DoubleClass.instance, department, absMonth, YearClass.instance);
        LCP currencyExtraInMonth = addDProp("currencyExtraInM", "Валюта (доп. затрат)", baseLM.currency, extraSection, absMonth, YearClass.instance, department);

        addJProp(salaryGroup, "Валюта", baseLM.name, currencyInMonth, 1, 2, 3);
        addJProp(baseGroup, "Валюта", baseLM.name, currencyExtraInMonth, 1, 2, 3, 4);

        LCP extraAdd = addDProp(baseGroup, "isAdd", "Не учитывать", LogicalClass.instance, person, extraPersonSection, absMonth, YearClass.instance);
        //LCP isReimbursed = addDProp(payerGroup, "isReimbersed", "Возмещено", LogicalClass.instance, payerAbs);

        LCP[] maxDateSal = addMGProp((AbstractGroup) null, false, new String[]{"maxYear", "maxMonth"}, new String[]{"год", "месяц"}, 1,
                addJProp(and(false, false, true, false), 4, is(absMonth), 1, is(YearClass.instance), 2, baseLM.less22, 2, 1, 4, 3, salaryInMonth, 5, 3, 4), 3, 1, 2, 5);
        LCP curSalary = addJProp(salaryGroup, "Тек. зарплата", salaryInMonth, 3, maxDateSal[1], 1, 2, 3, maxDateSal[0], 1, 2, 3);
        LCP curCurrency = addJProp(currencyInMonth, 3, maxDateSal[1], 1, 2, 3, maxDateSal[0], 1, 2, 3);
        addJProp(salaryGroup, "Тек. валюта", baseLM.name, curCurrency, 1, 2, 3);

        LCP[] maxDateExtra = addMGProp((AbstractGroup) null, false, new String[]{"maxExtraYear", "maxExtraMonth"}, new String[]{"год", "месяц1"}, 1,
                addJProp(and(false, false, true, false), 4, is(absMonth), 1, is(YearClass.instance), 2, baseLM.less22, 2, 1, 4, 3, extraInMonth, 5, 3, 4, 6), 3, 1, 2, 5, 6);
        LCP curExtra = addJProp(baseGroup, "Тек. затраты", extraInMonth, 3, maxDateExtra[1], 1, 2, 3, 4, maxDateExtra[0], 1, 2, 3, 4, 4);
        // 3 - extraSection, 4 - department
        LCP curExtraCurrency = addJProp(currencyExtraInMonth, 3, maxDateExtra[1], 1, 2, 3, 4, maxDateExtra[0], 1, 2, 3, 4, 4);
        addJProp(baseGroup, "Тек. валюта", baseLM.name, curExtraCurrency, 1, 2, 3, 4);

        LCP workCoeff = addJProp(calcCoef, hourInMonthOv, 1, 2, 3, addJProp(baseLM.and1, workDays, 2, 3, is(person), 1), 1, 2, 3);
        roundSalary = addJProp(baseGroup, "К оплате", roundMult, workCoeff, 3, 1, 2, curSalary, 1, 2, 3);

        LCP paySum = addJProp("Выплачено", roundMult, payRate, 1, outSum, 1);
        LCP payTotal = addSGProp(baseGroup, "Всего заплачено", paySum, outPerson, 1, outMonth, 1, outYear, 1);
        
        roundSalarySum = addSGProp("roundSalarySum", "Сумарно к выплате", roundSalary, 3, curCurrency, 1, 2, 3);
        payTotalSum = addSGProp("payTotalSum", "Сумарно выплачено", payTotal, 1, curCurrency, 2, 3, 1);



        //LCP extraTotalPerson = addSGProp(addJProp(andNot1, addJProp(and(false,false), curExtra, 3, 4, 2, personDepartment, 1, is(person), 1, is(extraPersonSection), 2), 1, 2, 3, 4, extraAdd, 1, 2, 3, 4), 1, 3, 4);
        LCP extraTotal = addJProp(baseLM.andNot1, addJProp(baseLM.and1, addJProp(curExtra, 3, 4, 2, personDepartment, 1), 1, 2, 3, 4, is(extraPersonSection), 2), 1, 2, 3, 4, extraAdd, 1, 2, 3, 4);
        LCP extraTotalSum = addSGProp(extraTotal, 1, 3, 4, addJProp(curExtraCurrency, 3, 4, 2, personDepartment, 1), 1, 2, 3, 4);
        // 1 - person, 2- month, 3 - year, 4 - currency
        LCP roundExtraCur = addJProp(baseGroup, "Доп. затраты", calcExtraCoef, dayInMonthOv, 1, 2, 3, extraTotalSum, 1, 2, 3, 4, addJProp(baseLM.and1, workDays, 2, 3, is(person), 1), 1, 2, 3);

        LCP extraTotalPerson = addSGProp(addJProp(baseLM.andNot1, addJProp(baseLM.and1, addJProp(curExtra, 3, 4, 2, personDepartment, 1), 1, 2, 3, 4, is(extraPersonSection), 2), 1, 2, 3, 4, extraAdd, 1, 2, 3, 4), 1, 3, 4);
        LCP roundExtraPerson = addJProp(calcExtraCoef, dayInMonthOv, 1, 2, 3, extraTotalPerson, 1, 2, 3, addJProp(baseLM.and1, workDays, 2, 3, is(person), 1), 1, 2, 3);

        LCP extraComPerson = addSGProp(roundExtraCur, personDepartment, 1, 2, 3, 4);
        LCP extraComAdm = addSGProp(addJProp(baseLM.and1, curExtra, 1, 2, 3, 4, is(extraAdmSection), 3), 4, 1, 2, curExtraCurrency, 1, 2, 3, 4);

        // компенсации

        LCP totalDebt = addSGProp(baseGroup, "Затрачено", outSum, operationPayer, 1, outCur, 1);
        LCP totalReimbursement = addSGProp(baseGroup, "Возмещено", addJProp(multiplyDouble2, reimbursementSum, 1, reimbursementRate, 1), reimbursementPayer, 1, reimbursementCurrencyIn, 1);
        addDUProp(baseGroup, "Осталось", totalDebt, totalReimbursement);

        LCP totalDebtDep = addSGProp(baseGroup, "Затрачено", outSum, operationPayer, 1, outCur, 1, opDep, 1);
        LCP totalReimbursementDep = addSGProp(baseGroup, "Возмещено", addJProp(multiplyDouble2, reimbursementSum, 1, reimbursementRate, 1), reimbursementPayer, 1, reimbursementCurrencyIn, 1, reimbursementDepartment, 1);
        addDUProp(baseGroup, "Осталось", totalDebtDep, totalReimbursementDep);

        // обороты

        personDepartSum = addSGProp(personGroup, "Всего к выплате", roundSalary, personDepartment, 3, 1, 2, curCurrency, 1, 2, 3);
        payMonthTotal = addSGProp(personGroup, "Всего выплачено", payTotal, personDepartment, 1, 2, 3, curCurrency, 2, 3, 1);

        LCP extraDepartmentSum = addSUProp(extraGroup, "Всего доп. затрат", Union.SUM, extraComPerson, extraComAdm);
        LCP extraMonthTotal = addSGProp(extraGroup, "Выплачено доп. затрат", extraSum, outMonth, 1, outYear, 1, operationDepartment, 1, extraCurrency, 1);

        LCP outOperVal = addJProp("Опер. расход", baseLM.and1, outSum, 1, is(outOperation), 1);
        totalOutOper = addSGProp(outGroup, "Всего по опер. расходу", outOperVal, opDep, 1, transactionMonth, 1, transactionYear, 1, outCur, 1);

        LCP misOperVal = addJProp("Опер. расход ком.", baseLM.and1, outSum, 1, is(misOperation), 1);
        totalMisOper = addSGProp(outGroup, "Всего по опер. расходу ком.", misOperVal, opDep, 1, transactionMonth, 1, transactionYear, 1, outCur, 1);

        //LCP rateBC = addJProp(nearestExchangeRate, curCurrency, 1, 2, 3, baseCurrency, addJProp(dateByMY, 1, 2), 1, 2);
        //LCP salaryInBC = addJProp("К выплате в БВ", multiply, roundSalary, 1, 2, 3, rateBC, 1, 2, 3);
        //personDepartSumInBC = addSGProp("Всего к выплате в БВ", salaryInBC, personDepartment, 3, 1, 2);

        // Обороты в базовой валюте

        LCP personDepartSumInBC = addJProp("К выплате в БВ", multiplyDouble2, personDepartSum, 1, 2, 3, 4, addJProp("Курс выплаты к БВ", nearestExchangeRateMY, 1, baseCurrency, 2, 3), 4, 2, 3);
        LCP totalPersonDepartSumInBC = addSGProp(baseCurGroup, "Всего к выплате в БВ", personDepartSumInBC, 1, 2, 3);

        LCP extraDepartmentSumInBC = addJProp("Доп. затрат в БВ", multiplyDouble2, extraDepartmentSum, 1, 2, 3, 4, addJProp("Курс выплаты к БВ", nearestExchangeRateMY, 1, baseCurrency, 2, 3), 4, 2, 3);
        LCP totalExtraDepartmentSumInBC = addSGProp(baseCurGroup, "Всего доп. затрат в БВ", extraDepartmentSumInBC, 1, 2, 3);

        LCP outRateBC = addJProp(nearestExchangeRateOp, outCur, 1, baseCurrency, 1);
        LCP outOperValInBC = addJProp("Опер. расход в БВ", multiplyDouble2, outOperVal, 1, outRateBC, 1);
        departmentOutOperInBC = addSGProp(baseCurGroup, "Всего по опер. расходу в БВ", outOperValInBC, opDep, 1, transactionMonth, 1, transactionYear, 1);

        LCP misOperValInBC = addJProp("Опер. расход ком. в БВ", multiplyDouble2, misOperVal, 1, outRateBC, 1);
        LCP departmentMisOperInBC = addSGProp(baseCurGroup, "Всего по опер. расходу ком. в БВ", misOperValInBC, opDep, 1, transactionMonth, 1, transactionYear, 1);

        investorInvestment = addDProp(idGroup, "investorInvestment", "Инвестор (ИД)", investor, investment);
        nameInvestorInvestment = addJProp(baseGroup, "nameInvestorInvestment", "Инвестор", baseLM.name, investorInvestment, 1);

        sumInvestmentCash = addJProp("sumInvestmentCash", "Сумма", baseLM.and1, inSum, 1, is(investmentCash), 1);
        sumInvestmentNotCash = addJProp("sumInvestmentNotCash", "Сумма", baseLM.and1, inSum, 1, is(investmentNotCash), 1);

        curInvestmentCash = addJProp(idGroup, "curInvestmentCash", "Валюта (ИД)", baseLM.and1, inCur, 1, is(investmentCash), 1);
        curInvestmentNotCash = addJProp(idGroup, "curInvestmentNotCash", "Валюта (ИД)", baseLM.and1, inCur, 1, is(investmentNotCash), 1);

        sumInvestmentMoney = addCUProp("sumInvestmentMoney", "Сумма", sumInvestmentCash, sumInvestmentNotCash);
        curInvestmentMoney = addCUProp(idGroup, "curInvestmentMoney", "Валюта (ИД)", curInvestmentCash, curInvestmentNotCash);

        sumInvestmentNotMoney = addDProp(baseGroup, "sumInvestmentNotMoney", "Сумма", DoubleClass.instance, investmentNotMoney);
        curInvestmentNotMoney = addDProp(idGroup, "curInvestmentNotMoney", "Валюта (ИД)", baseLM.currency, investmentNotMoney);

        sumInvestment = addCUProp("sumInvestment", "Сумма", sumInvestmentMoney, sumInvestmentNotMoney);
        curInvestment = addCUProp(idGroup, "curInvestment", "Валюта (ИД)", curInvestmentMoney, curInvestmentNotMoney);
        nameCurInvestment = addJProp(baseGroup, "nameCurInvestment", "Валюта", baseLM.name, curInvestment, 1);

        exchangeBaseRateInvestment = addJProp("exchangeBaseRateInvestment", "Курс", exchangeRateCurrencyTransaction, curInvestment, 1, 1);
        sumBaseInvestment = addJProp("sumBaseInvestment", "Сумма (БВ)", addJProp(baseLM.round, 1, addCProp(IntegerClass.instance, 0)), addJProp(multiplyDouble2, sumInvestment, 1, exchangeBaseRateInvestment, 1), 1);

        sumInvestmentInvestor = addSGProp("sumInvestmentInvestor", "Проинвестировано", sumBaseInvestment, investorInvestment, 1);
        investmentTotal = addSGProp("investmentTotal", "Всего проинвестировано", sumBaseInvestment);

        shareInvestor = addJProp("shareInvestor", "Доля (%)", baseLM.share, sumInvestmentInvestor, 1, investmentTotal);

        initNavigators();
    }

    

    @Override
    public void initIndexes() {
    }

    FormEntity mainAccountForm, salesArticleStoreForm;

    private void initNavigators() {
        NavigatorElement primaryData = addNavigatorElement(baseLM.baseElement, "primaryData", "Первичные данные");
        FormEntity incomeForm = addFormEntity(new IncomeFormEntity(primaryData, "incomeForm", "Приход"));
        FormEntity investmentNotMoney = addFormEntity(new InvestmentFormEntity(primaryData, "investment", "Инвестиции"));
        FormEntity specialRecordForm = addFormEntity(new SpecialRecordFormEntity(primaryData, "specialRecordForm", "Затраты по сотрудникам"));
        FormEntity salaryForm = addFormEntity(new ExtraFormEntity(primaryData, "salaryForm", "Дополнительные затраты"));
        FormEntity recordForm = addFormEntity(new RecordFormEntity(primaryData, "recordForm", "Прочие операции"));
        FormEntity missionForm = new MissionFormEntity(primaryData, "missionForm", "Командировка");
        FormEntity vacationForm = addFormEntity(new VacationFormEntity(primaryData, "vacationForm", "Отпуск сотрудников"));
        FormEntity exchangeRatesForm = new ExchangeRatesFormEntity(primaryData, "exchangeRatesForm", "Курсы валют");
        FormEntity depTransfer = new depTransfer(primaryData, "depTransfer", "Перемещение");


        NavigatorElement aggregateData = addNavigatorElement(baseLM.baseElement, "aggregateData", "Сводная информация");
        FormEntity departmentBalance = new DepartmentBalanceFormEntity(aggregateData, "departmentBalance", "Баланс по отделам");
        FormEntity employeeExtraSum = addFormEntity(new DepartmentRevenueFormEntity(aggregateData, "employeeExtraSum", "Обороты по отделам"));
        FormEntity reimbursement = addFormEntity(new ReimbursementFormEntity(aggregateData, "reimbursement", "Компенсация"));
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    private class RecordFormEntity extends FormEntity {

        public RecordFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objDepartment = addSingleGroupObject(department, baseGroup);
            objDepartment.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objOutOp = addSingleGroupObject(outOperation, baseGroup);
            ObjectEntity objExOp = addSingleGroupObject(exOperation, baseGroup);

            addPropertyDraw(objExOp, inOperationGroup);
            addPropertyDraw(objOutOp, payerGroup);

            addObjectActions(this, objOutOp);
            addObjectActions(this, objExOp);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(opDep, objOutOp), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(opDep, objExOp), Compare.EQUALS, objDepartment));
        }
    }

    private class IncomeFormEntity extends FormEntity {
        private ObjectEntity objIncNotCash;
        private ObjectEntity objOutcome;

        public IncomeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objDepartment = addSingleGroupObject(department, baseGroup);
            objDepartment.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objIncCash = addSingleGroupObject(incomeCash, baseGroup);
            addPropertyDraw(objIncCash, nameInvestorInvestment);

            objIncNotCash = addSingleGroupObject(incomeNotCash, baseGroup);
            addPropertyDraw(objIncNotCash, nameInvestorInvestment);

            setForceViewType(nameInvestorInvestment, ClassViewType.GRID);

            objOutcome = addSingleGroupObject(outcomeCost, baseGroup);

            addPropertyDraw(objIncCash, objIncNotCash, objOutcome, baseGroup);

            ObjectEntity objTransfer = addSingleGroupObject(transfer, baseGroup);
            addObjectActions(this, objTransfer);
            addPropertyDraw(objDepartment, objTransfer, baseGroup);

            ObjectEntity objProfit = addSingleGroupObject(incomeProfit, baseGroup);
            addObjectActions(this, objProfit);

            addObjectActions(this, objIncCash);
            addObjectActions(this, objIncNotCash);
            addObjectActions(this, objOutcome);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(opDep, objIncCash), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(opDep, objIncNotCash), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(incomeOutcome, objOutcome), Compare.EQUALS, objIncNotCash));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(opDep, objOutcome), Compare.EQUALS, objDepartment));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.addIntersection(design.getGroupObjectContainer(objIncNotCash.groupTo), design.getGroupObjectContainer(objOutcome.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            return design;
        }
    }

    private class InvestmentFormEntity extends FormEntity {

        private InvestmentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objInvestment = addSingleGroupObject(investment, baseLM.date, baseLM.objectClassName, nameCurInvestment, sumInvestment, exchangeBaseRateInvestment, sumBaseInvestment, nameInvestorInvestment, nameOperationDepartment);
            addObjectActions(this, objInvestment);

            ObjectEntity objInvestor = addSingleGroupObject(investor, baseGroup, sumInvestmentInvestor, shareInvestor);

            addPropertyDraw(investmentTotal);

            addDefaultOrder(baseLM.date, true);
        }
    }

    private class ExtraFormEntity extends FormEntity {
        private ObjectEntity objMonthOp;
        private ObjectEntity objCur;

        public ExtraFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objDepartment = addSingleGroupObject(department, baseGroup);
            objDepartment.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objExtraStateOp = addSingleGroupObject(extraSection, baseGroup);
            ObjectEntity objYearOp = addSingleGroupObject(YearClass.instance, "Год", baseGroup);
            objYearOp.groupTo.setSingleClassView(ClassViewType.PANEL);

            objMonthOp = addSingleGroupObject(absMonth, baseGroup);
            objCur = addSingleGroupObject(baseLM.currency, baseGroup);
            ObjectEntity objExtraOp = addSingleGroupObject(extraCost, baseGroup);

            addPropertyDraw(objExtraStateOp, objYearOp, objMonthOp, baseGroup);
            addPropertyDraw(objDepartment, objMonthOp, objYearOp, objExtraStateOp, baseGroup);
            addPropertyDraw(objDepartment, objMonthOp, objYearOp, baseGroup);

            addObjectActions(this, objExtraStateOp);
            addObjectActions(this, objExtraOp);
            addPropertyDraw(objExtraOp, payerGroup);
            //NotNullFilterEntity documentFilter = new NotNullFilterEntity(getPropertyObject(salaryInMonth));
            //addFixedFilter(documentFilter);

            addPropertyDraw(objDepartment, objMonthOp, objYearOp, objCur, extraGroup);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(outYear, objExtraOp), Compare.EQUALS, objYearOp));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outMonth, objExtraOp), Compare.EQUALS, objMonthOp));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(operationDepartment, objExtraOp), Compare.EQUALS, objDepartment));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objCur.groupTo).grid.constraints.fillHorizontal /= 2;
            design.addIntersection(design.getGroupObjectContainer(objMonthOp.groupTo), design.getGroupObjectContainer(objCur.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            return design;
        }
    }

    private class SpecialRecordFormEntity extends FormEntity {

        private ObjectEntity objExtraStateOp;
        private ObjectEntity objPayOp;
        private ObjectEntity objMonthOp;
        private ObjectEntity objCur;

        public SpecialRecordFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            ObjectEntity objDepartment = addSingleGroupObject(department, baseGroup);
            objDepartment.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objPersonOp = addSingleGroupObject(person, baseGroup);
            ObjectEntity objYearOp = addSingleGroupObject(YearClass.instance, "Год", baseGroup);
            objYearOp.groupTo.setSingleClassView(ClassViewType.PANEL);

            objMonthOp = addSingleGroupObject(absMonth, baseGroup);
            objCur = addSingleGroupObject(baseLM.currency, baseGroup);
            objExtraStateOp = addSingleGroupObject(extraPersonSection, baseGroup);

            objPayOp = addSingleGroupObject(pay, baseGroup);

            addPropertyDraw(objPersonOp, objYearOp, objMonthOp, salaryGroup);
            addPropertyDraw(objYearOp, objMonthOp, dateTimeGroup);
            addPropertyDraw(objPersonOp, objYearOp, objMonthOp, dateTimeGroup);
            addPropertyDraw(objPersonOp, objYearOp, objMonthOp, baseGroup);
            addPropertyDraw(objPersonOp, objExtraStateOp, objYearOp, objMonthOp, baseGroup);
            addPropertyDraw(objYearOp, objMonthOp, baseGroup);
            addPropertyDraw(objPayOp, payerGroup);
            addPropertyDraw(objPersonOp, objCur, roundSalarySum, payTotalSum);

            addObjectActions(this, objPersonOp);
            addObjectActions(this, objPayOp);


            addPropertyDraw(objPersonOp, objYearOp, objMonthOp, objCur, baseGroup);
            
            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(inactiveAbsOutPerson, objPersonOp))),
                    "Только активные"), true);
            addRegularFilterGroup(filterGroup);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(isWorkingMonthForPerson, objPersonOp, objMonthOp, objYearOp)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outPerson, objPayOp), Compare.EQUALS, objPersonOp));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outYear, objPayOp), Compare.EQUALS, objYearOp));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outMonth, objPayOp), Compare.EQUALS, objMonthOp));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(personDepartment, objPersonOp), Compare.EQUALS, objDepartment));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objExtraStateOp.groupTo).grid.constraints.fillHorizontal /= 3;
            design.addIntersection(design.getGroupObjectContainer(objExtraStateOp.groupTo), design.getGroupObjectContainer(objPayOp.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.get(objCur.groupTo).grid.constraints.fillHorizontal /= 3;
            design.addIntersection(design.getGroupObjectContainer(objMonthOp.groupTo), design.getGroupObjectContainer(objCur.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }


    private class MissionFormEntity extends FormEntity {

        public MissionFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            ObjectEntity objDepartment = addSingleGroupObject(department, baseGroup);
            objDepartment.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objMission = addSingleGroupObject(mission, baseGroup);
            ObjectEntity objPerson = addSingleGroupObject(person, baseGroup);
            ObjectEntity objOutOp = addSingleGroupObject(misOperation, baseGroup);

            addPropertyDraw(objMission, objPerson, baseGroup);
            addPropertyDraw(objOutOp, payerGroup);

            addObjectActions(this, objMission);
            addObjectActions(this, objOutOp);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(missionOperation, objOutOp), Compare.EQUALS, objMission));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(operationDepartment, objOutOp), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(personDepartment, objPerson), Compare.EQUALS, objDepartment));
        }
    }

    private class VacationFormEntity extends FormEntity {

        public VacationFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            ObjectEntity objDepartment = addSingleGroupObject(department, baseGroup);
            objDepartment.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objPerson = addSingleGroupObject(person, baseGroup);
            ObjectEntity objVacation = addSingleGroupObject(vacation, baseGroup);

            addPropertyDraw(objVacation, objPerson, baseGroup);

            addObjectActions(this, objVacation);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(personDepartment, objPerson), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(vacationPerson, objVacation), Compare.EQUALS, objPerson));
        }
    }

    private class depTransfer extends FormEntity {

        public depTransfer(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objDepartment = addSingleGroupObject(department, baseGroup);
            objDepartment.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objCur = addSingleGroupObject(baseLM.currency, baseGroup);
            addPropertyDraw(baseGroup, false, false, objDepartment, objCur);

            ObjectEntity objTransfer = addSingleGroupObject(transfer, baseGroup);
            addObjectActions(this, objTransfer);
            addPropertyDraw(objDepartment, objTransfer, baseGroup);
        }
    }

    private class DepartmentBalanceFormEntity extends FormEntity {

        public DepartmentBalanceFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objDepartment = addSingleGroupObject(department, baseGroup);
            objDepartment.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objCur = addSingleGroupObject(baseLM.currency, baseGroup);
            ObjectEntity objInOp = addSingleGroupObject(inAbsOperation, baseGroup, true);
            ObjectEntity objOutOp = addSingleGroupObject(outAbsOperation, baseGroup, true);
            addPropertyDraw(baseGroup, true, false, objCur, objInOp);
            addPropertyDraw(baseGroup, true, false, objCur, objOutOp);
            addPropertyDraw(baseGroup, false, false, objDepartment, objCur);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(inCur, objInOp), Compare.EQUALS, objCur));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outCur, objOutOp), Compare.EQUALS, objCur));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(opDep, objInOp), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(opDep, objOutOp), Compare.EQUALS, objDepartment));

        }
    }

    private class ReimbursementFormEntity extends FormEntity {
        private ObjectEntity objCurrency;
        private ObjectEntity objDepartment;
        private ObjectEntity objOutOp;
        private ObjectEntity objReimbursement;

        public ReimbursementFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objPayer = addSingleGroupObject(payer, baseGroup);
            objCurrency = addSingleGroupObject(baseLM.currency, baseGroup);
            objDepartment = addSingleGroupObject(department, baseGroup);
            objOutOp = addSingleGroupObject(payerAbs, baseGroup);
            objReimbursement = addSingleGroupObject(reimbursement, baseGroup);

            addObjectActions(this, objReimbursement);
            addPropertyDraw(objPayer, objCurrency, baseGroup);
            addPropertyDraw(objPayer, objCurrency, objDepartment, baseGroup);
            addPropertyDraw(depBalanceQuantity, objCurrency, objDepartment);
            //depBalanceQuantity
            addFixedFilter(new CompareFilterEntity(addPropertyObject(operationPayer, objOutOp), Compare.EQUALS, objPayer));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(reimbursementPayer, objReimbursement), Compare.EQUALS, objPayer));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(outCur, objOutOp), Compare.EQUALS, objCurrency));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(reimbursementCurrencyIn, objReimbursement), Compare.EQUALS, objCurrency));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(opDep, objOutOp), Compare.EQUALS, objDepartment));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(reimbursementDepartment, objReimbursement), Compare.EQUALS, objDepartment));


        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            //design.get(objCurrency.groupTo).grid.constraints.fillHorizontal /= 2;
            design.addIntersection(design.getGroupObjectContainer(objCurrency.groupTo), design.getGroupObjectContainer(objDepartment.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objOutOp.groupTo), design.getGroupObjectContainer(objReimbursement.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }

    }

    private class DepartmentRevenueFormEntity extends FormEntity {

        public DepartmentRevenueFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            ObjectEntity objDepartment = addSingleGroupObject(department, baseGroup);
            objDepartment.groupTo.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(baseCurrencyName);

            ObjectEntity objYearOp = addSingleGroupObject(YearClass.instance, "Год", baseGroup);
            objYearOp.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objMonthOp = addSingleGroupObject(absMonth, baseGroup);

            addPropertyDraw(baseCurGroup, true, objDepartment, objMonthOp, objYearOp);

            ObjectEntity objCurrency = addSingleGroupObject(baseLM.currency, baseGroup);

            addPropertyDraw(personGroup, true, objDepartment, objMonthOp, objYearOp, objCurrency);
            addPropertyDraw(extraGroup, true, objDepartment, objMonthOp, objYearOp, objCurrency);
            addPropertyDraw(outGroup, true, objDepartment, objMonthOp, objYearOp, objCurrency);
        }
    }

    private class ExchangeRatesFormEntity extends FormEntity {

        public ExchangeRatesFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

//            ObjectEntity objYearOp = addSingleGroupObject(IntegerClass.instance, "Год", properties, baseGroup);
//                        objYearOp.groupTo.initClassView = ClassViewType.PANEL;
//                        objYearOp.groupTo.banClassView = ClassViewType.GRID | ClassViewType.HIDE;
//            ObjectEntity objMonthOp = addSingleGroupObject(absMonth, "Месяц", properties, baseGroup);
//
//            addPropertyDraw(rateDay, objMonthOp, objYearOp);
//            addPropertyDraw(userRateDay, objMonthOp, objYearOp);
//            addPropertyDraw(dateByMY, objMonthOp, objYearOp);

            ObjectEntity objSrcCurrency = addSingleGroupObject(baseLM.currency, "Исходная валюта", baseGroup);
            ObjectEntity objDstCurrency = addSingleGroupObject(baseLM.currency, "Целевая валюта", baseGroup);

            ObjectEntity objDate = addSingleGroupObject(DateClass.instance, "Дата", baseGroup);
            objDate.groupTo.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(exchangeRate, objSrcCurrency, objDstCurrency, objDate);
            addPropertyDraw(nearestPredDate, objSrcCurrency, objDstCurrency, objDate);
            addPropertyDraw(nearestExchangeRate, objSrcCurrency, objDstCurrency, objDate);

            ObjectEntity objDateRate = addSingleGroupObject(DateClass.instance, "Дата", baseGroup);
            addPropertyDraw(exchangeRate, objSrcCurrency, objDstCurrency, objDateRate);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(exchangeRate, objSrcCurrency, objDstCurrency, objDateRate)));
        }
    }

}
