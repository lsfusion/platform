package trushkin;

import net.sf.jasperreports.engine.JRException;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.*;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class TrushkinBusinessLogics extends BusinessLogics<TrushkinBusinessLogics> {

    private ConcreteCustomClass participant, administrator, expert, workGroup, subject;
    private StaticCustomClass workGroupStatus;

    private LP inExpertWorkGroup;
    private LP workGroupSubject;
    private LP nameWorkGroupSubject;
    private LP authorSubject;
    private LP nameAuthorSubject;
    private LP nameSubject;
    private LP descriptionSubject;
    private LP inCurrentExpertWorkGroup;
    private LP genDeadlineWorkGroup;
    private LP genEndWorkGroup;
    private LP statusWorkGroup;
    private LP nameStatusWorkGroup;
    private LP rateExpertSubjectSubject;
    private LP rateCurrentExpertSubjectSubject;
    private LP sumRowExpertSubject;
    private LP sumColumnExpertSubject;
    private LP sumRowCurrentExpertSubject;
    private LP sumColumnCurrentExpertSubject;
    private LP matrixEndWorkGroup;
    private LP oneRatedExpertSubjectSubject;
    private LP quantityRatedExpertWorkGroup;
    private LP quantitySubjectsWorkGroup;
    private LP squareSubjectsWorkGroup;
    private LP completedRatingExpertWorkGroup;
    private LP inParticipantWorkGroup;
    private LP inCurrentParticipantWorkGroup;
    private LP sumSubjectSubject;
    private LP countSubjectSubject;
    private LP avgSubjectSubject;
    private LP deltaExpertSubjectSubject;
    private LP sqrDltExpertSubjectSubject;
    private LP sumSqrDltSubjectSubject;
    private LP varianceSubjectSubject;
    private LP limitVariance;
    private LP overLimitExpertSubjectSubject;
    private LP countOverLimitSubjectSubject;
    private LP titleColumnExpertSubject;
    private LP titleColumnCurrentExpertSubject;

    public TrushkinBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void initGroups() {
    }

    @Override
    protected void initClasses() {

        participant = addConcreteClass("participant", "Участник", customUser);
        administrator = addConcreteClass("adminstrator", "Администратор системы", participant);
        expert = addConcreteClass("expert", "Оценщик", participant);

        workGroup = addConcreteClass("workGroup", "Рабочая группа", baseClass.named);
        workGroupStatus = addStaticClass("workGroupStatus", "Статус рабочей группы",
                new String[]{"generation", "matrix", "result"},
                new String[]{"Генерация", "Матричная обработка", "Выводы и коммуникации"});

        subject = addConcreteClass("subject", "Строка", baseClass);
    }

    @Override
    protected void initProperties() {

//        currentExpert = addJProp(idGroup, "currentExpert", "Текущий оценщик (ИД)", and1, currentUser, is(expert), 1);

        genDeadlineWorkGroup = addDProp(baseGroup, "genDeadlineWorkGroup", "Время перехода в режим матричной обработки", DateTimeClass.instance, workGroup);
        genEndWorkGroup = addJProp("genEndWorkGroup", "Генерация завершена", greater2, currentDateTime, genDeadlineWorkGroup, 1);

        matrixEndWorkGroup = addDProp("matrixEndWorkGroup", "Матричная обработка завершена", LogicalClass.instance, workGroup);

        inExpertWorkGroup  = addDProp(baseGroup, "inExpertWorkGroup", "Вкл", LogicalClass.instance, expert, workGroup);
        inParticipantWorkGroup = addCUProp(baseGroup, "inParticipantWorkGroup", "Вкл", addCProp(LogicalClass.instance, true, administrator, workGroup), inExpertWorkGroup);

        inCurrentExpertWorkGroup = addJProp("inCurrentExpertWorkGroup", "Вкл", inExpertWorkGroup, currentUser, 1);
        inCurrentParticipantWorkGroup = addJProp("inCurrentParticipantWorkGroup", "Вкл", inParticipantWorkGroup, currentUser, 1);

        nameSubject = addDProp(baseGroup, "nameSubject", "Наименование", StringClass.get(256), subject);
        nameSubject.setMinimumWidth(20);
        nameSubject.setPreferredWidth(60);

        descriptionSubject = addDProp(baseGroup, "descriptionSubject", "Описание", StringClass.get(1000), subject);
        descriptionSubject.setMinimumWidth(40);
        descriptionSubject.setPreferredWidth(120);

        workGroupSubject = addDProp(idGroup, "workGroupSubject", "Рабочая группа (ИД)", workGroup, subject);
        nameWorkGroupSubject = addJProp(baseGroup, "nameWorkGroupSubject", "Рабочая группа", name, workGroupSubject, 1);

        quantitySubjectsWorkGroup = addSGProp(baseGroup, "quantitySubjectsWorkGroup", "Кол-во строк", addCProp(IntegerClass.instance, 1, subject), workGroupSubject, 1);
        squareSubjectsWorkGroup = addJProp("squareSubjectsWorkGroup", "Кол-во полей матрицы", squareInteger, quantitySubjectsWorkGroup, 1);

        authorSubject = addDProp(idGroup, "authorSubject", "Автор (ИД)", expert, subject);
        nameAuthorSubject = addJProp(baseGroup, "nameAuthorSubject", "Автор", name, authorSubject, 1);

        rateExpertSubjectSubject = addDProp(baseGroup, "rateExpertSubjectSubject", "Оценка", IntegerClass.instance, expert, subject, subject);
        oneRatedExpertSubjectSubject = addJProp("oneRatedExpertSubjectSubject", "Оценен", and1, addCProp(IntegerClass.instance, 1), rateExpertSubjectSubject, 1, 2, 3);

        sumSubjectSubject = addSGProp(baseGroup, "sumSubjectSubject", "Сумма", rateExpertSubjectSubject, 2, 3);
        countSubjectSubject = addSGProp(baseGroup, "countSubjectSubject", "Сумма", oneRatedExpertSubjectSubject, 2, 3);
        avgSubjectSubject = addJProp(baseGroup, "avgSubjectSubject", "Матожидание", divideDouble2, sumSubjectSubject, 1, 2, countSubjectSubject, 1, 2);

        deltaExpertSubjectSubject = addJProp(baseGroup, "deltaExpertSubjectSubject", "Отклонение", deltaDouble2, rateExpertSubjectSubject, 1, 2, 3, avgSubjectSubject, 2, 3);
        sqrDltExpertSubjectSubject = addJProp(baseGroup, "sqrDltExpertSubjectSubject", "Квадратичное отклонение", squareDouble, deltaExpertSubjectSubject, 1, 2, 3);
        sumSqrDltSubjectSubject = addSGProp(baseGroup, "sumSqrDltSubjectSubject", "Квадратичное отклонение", sqrDltExpertSubjectSubject, 2, 3);
        varianceSubjectSubject = addJProp(baseGroup, "varianceSubjectSubject", "Дисперсия", sqrtDouble2, sumSqrDltSubjectSubject, 1, 2);

        limitVariance = addCProp("limitVariance", DoubleClass.instance, 1.5);
        overLimitExpertSubjectSubject = addJProp(baseGroup, "overLimitExpertSubjectSubject", "Превышен лимит", greater2, deltaExpertSubjectSubject, 1, 2, 3, limitVariance);
        countOverLimitSubjectSubject = addSGProp(baseGroup, "countOverLimitSubjectSubject", "Кол-во превышений",
                                                 addJProp(and1, addCProp(IntegerClass.instance, 1), overLimitExpertSubjectSubject, 1, 2, 3), 2, 3);

        sumRowExpertSubject = addSGProp(baseGroup, "sumRowExpertSubject", "Сумма ряда", rateExpertSubjectSubject, 1, 2);
        sumColumnExpertSubject = addSGProp(baseGroup, "sumColumnExpertSubject", "Сумма колонки", rateExpertSubjectSubject, 1, 3);

        titleColumnExpertSubject = addJProp("titleColumnExpertSubject", "Заголовок колонки", addSFProp("((prm1) || ' (' || CAST((prm2) as text) || ')')", TextClass.instance, 2), nameSubject, 2, sumColumnExpertSubject, 1, 2);

        // todo : здесь конечно не совсем правильно построено свойство, так как workGroupSubject может не совпадать у разных subject, но пока на уровне форм пользователь не сможет так ввести данные
        quantityRatedExpertWorkGroup = addSGProp(baseGroup, "quantityRatedExpertWorkGroup", "Кол-во оценок", oneRatedExpertSubjectSubject, 1, workGroupSubject, 2);

        completedRatingExpertWorkGroup = addJProp(baseGroup, "completedRatingExpertWorkGroup", "Закончил оценку", equals2, quantityRatedExpertWorkGroup, 1, 2, squareSubjectsWorkGroup, 2);

        rateCurrentExpertSubjectSubject = addJProp(true, "rateCurrentExpertSubjectSubject", "Оценка", rateExpertSubjectSubject, currentUser, 1, 2);
        sumRowCurrentExpertSubject = addJProp("sumRowCurrentExpertSubject", "Сумма", sumRowExpertSubject, currentUser, 1);
        sumColumnCurrentExpertSubject = addJProp("sumColumnCurrentExpertSubject", "Сумма", sumColumnExpertSubject, currentUser, 1);

        titleColumnCurrentExpertSubject = addJProp("titleColumnCurrentExpertSubject", "Заголовок колонки", titleColumnExpertSubject, currentUser, 1);

        statusWorkGroup = addIfElseUProp(idGroup, "statusWorkGroup", "Статус (ИД)",
                                  addCProp(workGroupStatus, "result", workGroup),
                                  addIfElseUProp(addCProp(workGroupStatus, "matrix", workGroup),
                                                 addCProp(workGroupStatus, "generation", workGroup),
                                                 genEndWorkGroup, 1),
                                  matrixEndWorkGroup, 1);
        nameStatusWorkGroup = addJProp(baseGroup, "nameStatusWorkGroup", "Статус", name, statusWorkGroup, 1);
    }

    @Override
    protected void initTables() {
    }

    @Override
    protected void initIndexes() {
    }

    @Override
    protected void initNavigators() throws JRException, FileNotFoundException {

        addFormEntity(new ExpertFormEntity(baseElement, "expertForm", "Реестр оценщиков"));
        addFormEntity(new WorkGroupFormEntity(baseElement, "workGroupForm", "Реестр рабочих групп"));

        addFormEntity(new GenerateSubjectFormEntity(baseElement, "generateSubjectForm", "Генерация строк"));

        addFormEntity(new RateMatrixFormEntity(baseElement, "rateMatrixForm", "Матричная обработка"));

        addFormEntity(new RateResultFormEntity(baseElement, "rateResultForm", "Выводы и коммуникации"));
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
        admin.addSecurityPolicy(permitAllPolicy);
    }

    private class ExpertFormEntity extends FormEntity<TrushkinBusinessLogics> {

        private ExpertFormEntity(NavigatorElement<TrushkinBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            ObjectEntity objExpert = addSingleGroupObject(expert, "Оценщик", userFirstName, userLastName, userLogin, userPassword, nameUserMainRole);
            addObjectActions(this, objExpert);

            setPageSize(0);
        }
    }

    private class WorkGroupFormEntity extends FormEntity<TrushkinBusinessLogics> {

        private ObjectEntity objWorkGroup;
        private ObjectEntity objExpert;
        private ObjectEntity objSubjectAll;
        private ObjectEntity objExpertMatrix;
        private PropertyDrawEntity matrixEnd;

        private WorkGroupFormEntity(NavigatorElement<TrushkinBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            objWorkGroup = addSingleGroupObject(workGroup, "Рабочая группа", objectValue, name, genDeadlineWorkGroup, nameStatusWorkGroup);
            addObjectActions(this, objWorkGroup);

            objExpert = addSingleGroupObject(expert, "Оценщики", userFirstName, userLastName, userLogin);

            addPropertyDraw(inExpertWorkGroup, objExpert, objWorkGroup);

            objSubjectAll = addSingleGroupObject(subject, "Генерация", nameSubject, descriptionSubject, nameAuthorSubject);

            objExpertMatrix = addSingleGroupObject(expert, "Матричная обработка", userFirstName, userLastName);

            addPropertyDraw(quantityRatedExpertWorkGroup, objExpertMatrix, objWorkGroup);
            addPropertyDraw(completedRatingExpertWorkGroup, objExpertMatrix, objWorkGroup);

            matrixEnd = addPropertyDraw(matrixEndWorkGroup, objWorkGroup);
            setForceViewType(matrixEnd, ClassViewType.PANEL);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objSubjectAll), Compare.EQUALS, objWorkGroup));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(inExpertWorkGroup, objExpert, objWorkGroup)),
                    "Только оценшики из рабочей группы"));
            addRegularFilterGroup(filterGroup);

            setPageSize(0);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView specContainer = design.createContainer();
            specContainer.tabbedPane = true;
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objWorkGroup.groupTo));

            specContainer.add(design.getGroupObjectContainer(objExpert.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSubjectAll.groupTo));
            specContainer.add(design.getGroupObjectContainer(objExpertMatrix.groupTo));

            design.getGroupObjectContainer(objExpertMatrix.groupTo).add(design.get(matrixEnd));

            return design;
        }
    }

    private class GenerateSubjectFormEntity extends FormEntity<TrushkinBusinessLogics> {

        private ObjectEntity objWorkGroup;
        private ObjectEntity objSubject;
        private ObjectEntity objSubjectAll;

        private GenerateSubjectFormEntity(NavigatorElement<TrushkinBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            objWorkGroup = addSingleGroupObject(workGroup, "Рабочая группа", name, genDeadlineWorkGroup, nameStatusWorkGroup);
            objWorkGroup.groupTo.setInitClassView(ClassViewType.PANEL);
            setReadOnly(objWorkGroup, true);

            objSubject = addSingleGroupObject(subject, "Мои строки", nameSubject, descriptionSubject);
            addObjectActions(this, objSubject);

            objSubjectAll = addSingleGroupObject(subject, "Все строки", nameSubject, descriptionSubject, nameAuthorSubject);
            setReadOnly(objSubjectAll, true);

            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(genEndWorkGroup, objWorkGroup))));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inCurrentExpertWorkGroup, objWorkGroup)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(authorSubject, objSubject), Compare.EQUALS, addPropertyObject(currentUser)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objSubject), Compare.EQUALS, objWorkGroup));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objSubjectAll), Compare.EQUALS, objWorkGroup));

            setPageSize(0);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView subjectContainer = design.createContainer();
            subjectContainer.tabbedPane = true;
            design.getMainContainer().addAfter(subjectContainer, design.getGroupObjectContainer(objWorkGroup.groupTo));

            subjectContainer.add(design.getGroupObjectContainer(objSubject.groupTo));
            subjectContainer.add(design.getGroupObjectContainer(objSubjectAll.groupTo));

            return design;
        }
    }

    private class RateMatrixFormEntity extends FormEntity<TrushkinBusinessLogics> {

        private ObjectEntity objWorkGroup;
        private ObjectEntity objColumn;
        private ObjectEntity objRow;
        private ObjectEntity objExpert;

        private RateMatrixFormEntity(NavigatorElement<TrushkinBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            objWorkGroup = addSingleGroupObject(workGroup, "Рабочая группа", name, genDeadlineWorkGroup, nameStatusWorkGroup);
            objWorkGroup.groupTo.setInitClassView(ClassViewType.PANEL);
            setReadOnly(objWorkGroup, true);

            objColumn = addSingleGroupObject(subject, "Выбор столбцов", selection, nameSubject, descriptionSubject, nameAuthorSubject, sumColumnCurrentExpertSubject);
            setReadOnly(objColumn, true);
            setReadOnly(selection, false);

            objRow = addSingleGroupObject(subject, "Матрица", nameSubject, descriptionSubject, nameAuthorSubject);
            setReadOnly(objRow, true);

            PropertyDrawEntity rate = addPropertyDraw(rateCurrentExpertSubjectSubject, objRow, objColumn);
            rate.columnGroupObjects.add(objColumn.groupTo);
            rate.setPropertyCaption(addPropertyObject(titleColumnCurrentExpertSubject, objColumn));

            addPropertyDraw(sumRowCurrentExpertSubject, objRow);

            objExpert = addSingleGroupObject(expert, "Оценщики", userFirstName, userLastName);
            setReadOnly(objExpert, true);

            addPropertyDraw(quantityRatedExpertWorkGroup, objExpert, objWorkGroup);
            addPropertyDraw(completedRatingExpertWorkGroup, objExpert, objWorkGroup);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(genEndWorkGroup, objWorkGroup)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objColumn), Compare.EQUALS, objWorkGroup));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objRow), Compare.EQUALS, objWorkGroup));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inCurrentExpertWorkGroup, objWorkGroup)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertWorkGroup, objExpert, objWorkGroup)));

            setPageSize(0);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView specContainer = design.createContainer();
            specContainer.tabbedPane = true;
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objWorkGroup.groupTo));

            specContainer.add(design.getGroupObjectContainer(objRow.groupTo));
            specContainer.add(design.getGroupObjectContainer(objColumn.groupTo));
            specContainer.add(design.getGroupObjectContainer(objExpert.groupTo));

            return design;
        }
    }

    private class RateResultFormEntity extends FormEntity<TrushkinBusinessLogics> {

        private ObjectEntity objWorkGroup;
        private ObjectEntity objColumn;
        private ObjectEntity objExpert;
        private ObjectEntity objRowExpert;
        private ObjectEntity objRowAvg;
        private ObjectEntity objRowVariance;
        private ObjectEntity objRowOver;
        private ObjectEntity objColumnOver;
        private ObjectEntity objExpertOver;
        private ObjectEntity objExpertRating;
        private ObjectEntity objSubjectRating;

        private RateResultFormEntity(NavigatorElement<TrushkinBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            objWorkGroup = addSingleGroupObject(workGroup, "Рабочая группа", name);
            objWorkGroup.groupTo.setInitClassView(ClassViewType.PANEL);

            GroupObjectEntity gobjSubjectOver = new GroupObjectEntity(genID());

            objRowOver = new ObjectEntity(genID(), subject, "Поле матрицы");
            objColumnOver = new ObjectEntity(genID(), subject, "Колонка");

            gobjSubjectOver.add(objRowOver);
            gobjSubjectOver.add(objColumnOver);
            addGroup(gobjSubjectOver);

            addPropertyDraw(new LP[] {nameSubject, descriptionSubject, nameAuthorSubject}, objRowOver);
            addPropertyDraw(new LP[] {nameSubject, descriptionSubject, nameAuthorSubject}, objColumnOver);

            addPropertyDraw(avgSubjectSubject, objRowOver, objColumnOver);
            addPropertyDraw(varianceSubjectSubject, objRowOver, objColumnOver);
            addPropertyDraw(countOverLimitSubjectSubject, objRowOver, objColumnOver);

            objExpertOver = addSingleGroupObject(expert, "Оценка", userFirstName, userLastName, userLogin);

            addPropertyDraw(rateExpertSubjectSubject, objExpertOver, objRowOver, objColumnOver);
            addPropertyDraw(deltaExpertSubjectSubject, objExpertOver, objRowOver, objColumnOver).setPropertyHighlight(
                    addPropertyObject(overLimitExpertSubjectSubject, objExpertOver, objRowOver, objColumnOver));

            objExpertRating = addSingleGroupObject(expert, "Оценщик", userFirstName, userLastName, userLogin);
            objSubjectRating = addSingleGroupObject(subject, "Строка", nameSubject, descriptionSubject, nameAuthorSubject);

            addPropertyDraw(sumRowExpertSubject, objExpertRating, objSubjectRating);
            addPropertyDraw(sumColumnExpertSubject, objExpertRating, objSubjectRating);

            objColumn = addSingleGroupObject(subject, "Выбор столбцов", selection, nameSubject, descriptionSubject, nameAuthorSubject);

            objExpert = addSingleGroupObject(expert, "Оценщик", userFirstName, userLastName, userLogin);
            objExpert.groupTo.setInitClassView(ClassViewType.PANEL);

            objRowExpert = addSingleGroupObject(subject, "Матрица", nameSubject, descriptionSubject, nameAuthorSubject);

            PropertyDrawEntity rate = addPropertyDraw(rateExpertSubjectSubject, objExpert, objRowExpert, objColumn);
            rate.columnGroupObjects.add(objColumn.groupTo);
            rate.setPropertyCaption(addPropertyObject(titleColumnExpertSubject, objExpert, objColumn));

            addPropertyDraw(sumRowExpertSubject, objExpert, objRowExpert);

            objRowAvg = addSingleGroupObject(subject, "Матожидание", nameSubject, descriptionSubject, nameAuthorSubject);

            PropertyDrawEntity avg = addPropertyDraw(avgSubjectSubject, objRowAvg, objColumn);
            avg.columnGroupObjects.add(objColumn.groupTo);
            avg.setPropertyCaption(addPropertyObject(nameSubject, objColumn));

            objRowVariance = addSingleGroupObject(subject, "Дисперсия", nameSubject, descriptionSubject, nameAuthorSubject);

            PropertyDrawEntity variance = addPropertyDraw(varianceSubjectSubject, objRowVariance, objColumn);
            variance.columnGroupObjects.add(objColumn.groupTo);
            variance.setPropertyCaption(addPropertyObject(nameSubject, objColumn));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inCurrentParticipantWorkGroup, objWorkGroup)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(matrixEndWorkGroup, objWorkGroup)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertWorkGroup, objExpert, objWorkGroup)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertWorkGroup, objExpertRating, objWorkGroup)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objColumn), Compare.EQUALS, objWorkGroup));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objRowExpert), Compare.EQUALS, objWorkGroup));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objRowAvg), Compare.EQUALS, objWorkGroup));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objRowVariance), Compare.EQUALS, objWorkGroup));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objRowOver), Compare.EQUALS, objWorkGroup));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objColumnOver), Compare.EQUALS, objWorkGroup));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(workGroupSubject, objSubjectRating), Compare.EQUALS, objWorkGroup));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(rateExpertSubjectSubject, objExpertOver, objRowOver, objColumnOver)));

            setReadOnly(true);
            setReadOnly(selection, false);

            setPageSize(0);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.defaultOrders.put(design.get(getPropertyDraw(countOverLimitSubjectSubject, objRowOver.groupTo)), false);
            design.defaultOrders.put(design.get(getPropertyDraw(varianceSubjectSubject, objRowOver.groupTo)), false);
            design.defaultOrders.put(design.get(getPropertyDraw(deltaExpertSubjectSubject, objExpertOver.groupTo)), false);

            design.defaultOrders.put(design.get(getPropertyDraw(sumRowExpertSubject, objSubjectRating.groupTo)), false);
            design.defaultOrders.put(design.get(getPropertyDraw(sumColumnExpertSubject, objSubjectRating.groupTo)), false);

            ContainerView specContainer = design.createContainer();
            specContainer.tabbedPane = true;
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objWorkGroup.groupTo));

            ContainerView rateContainer = design.createContainer("Оценки");
            rateContainer.add(design.getGroupObjectContainer(objExpert.groupTo));
            rateContainer.add(design.getGroupObjectContainer(objRowExpert.groupTo));

            ContainerView overContainer = design.createContainer("Поля матрицы");
            overContainer.add(design.getGroupObjectContainer(objRowOver.groupTo));
            overContainer.add(design.getGroupObjectContainer(objExpertOver.groupTo));

            ContainerView ratingContainer = design.createContainer("Рейтинг названий");
            ratingContainer.add(design.getGroupObjectContainer(objExpertRating.groupTo));
            ratingContainer.add(design.getGroupObjectContainer(objSubjectRating.groupTo));

            specContainer.add(overContainer);
            specContainer.add(ratingContainer);
            specContainer.add(rateContainer);
            specContainer.add(design.getGroupObjectContainer(objRowAvg.groupTo));
            specContainer.add(design.getGroupObjectContainer(objRowVariance.groupTo));
            specContainer.add(design.getGroupObjectContainer(objColumn.groupTo));

            return design;
        }
    }
}
