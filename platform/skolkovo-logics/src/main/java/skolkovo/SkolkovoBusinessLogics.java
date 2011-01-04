package skolkovo;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.ClientAction;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.session.DataSession;
import skolkovo.api.remote.SkolkovoRemoteInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

import static platform.base.BaseUtils.nvl;

public class SkolkovoBusinessLogics extends BusinessLogics<SkolkovoBusinessLogics> implements SkolkovoRemoteInterface {

    public SkolkovoBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    AbstractCustomClass participant;

    ConcreteCustomClass project;
    ConcreteCustomClass expert;
    ConcreteCustomClass cluster;
    ConcreteCustomClass claimer;
    ConcreteCustomClass document;

    ConcreteCustomClass vote;

    StaticCustomClass voteResult;
    StaticCustomClass projectStatus;

    AbstractGroup voteResultGroup;
    AbstractGroup voteResultCheckGroup;
    AbstractGroup voteResultCommentGroup;

    protected void initGroups() {
        voteResultGroup = new AbstractGroup("Результаты голосования");
        publicGroup.add(voteResultGroup);

        voteResultCheckGroup = new AbstractGroup("Результаты голосования (выбор)");
        voteResultCheckGroup.createContainer = false;
        voteResultGroup.add(voteResultCheckGroup);

        voteResultCommentGroup = new AbstractGroup("Результаты голосования (комментарии)");
        voteResultCommentGroup.createContainer = false;
        voteResultGroup.add(voteResultCommentGroup);
    }

    protected void initClasses() {

        participant = addAbstractClass("participant", "Участник", baseClass);

        project = addConcreteClass("project", "Проект", baseClass.named, transaction);
        expert = addConcreteClass("expert", "Эксперт", customUser, participant);
        cluster = addConcreteClass("cluster", "Кластер", baseClass.named);
        claimer = addConcreteClass("claimer", "Заявитель", baseClass.named, participant);
        document = addConcreteClass("document", "Документ", baseClass.named);

        vote = addConcreteClass("vote", "Заседание", baseClass, transaction);

        voteResult = addStaticClass("voteResult", "Результат заседания", 
                                    new String[]{"refused", "connected", "voted"}, 
                                    new String[]{"Отказался", "Аффилирован", "Проголосовал"});
        
        projectStatus = addStaticClass("projectStatus", "Статус проекта",
                                       new String[]{"unknown", "needExtraVote", "inProgress", "succeeded", "valued"},
                                       new String[]{"Неизвестный статус", "Требуется заседание", "Идет заседание", "Достаточно голосов", "Произведена оценка"});
    }

    LP projectVote, nameProjectVote;
    LP clusterExpert, nameClusterExpert;
    LP clusterProject, nameClusterProject;
    LP clusterVote, nameClusterVote;
    LP claimerProject, nameClaimerProject;
    LP emailParticipant;

    LP claimerVote, nameClaimerVote;

    LP projectDocument, nameProjectDocument;
    LP fileDocument;

    LP inExpertVote;
    LP dateStartVote, dateEndVote;

    LP openedVote;
    LP voteInProgressProject;
    LP requiredPeriod;
    LP requiredQuantity;
    LP limitExperts;

    LP voteResultExpertVote, nameVoteResultExpertVote;
    LP doneExpertVote;
    LP expertVoteConnected;
    LP inClusterExpertVote;
    LP innovativeExpertVote;
    LP innovativeCommentExpertVote;
    LP foreignExpertVote;
    LP competentExpertVote;
    LP completeExpertVote;
    LP completeCommentExpertVote;

    LP projectExpertVote;
    LP quantityDoneVote;
    LP succeededVote;
    LP voteSucceededProject;
    LP noCurrentVoteProject;
    LP voteValuedProject;
    LP needExtraVoteProject;

    LP generateVote;

    LP expertLogin;

    LP statusProject, nameStatusProject;

    protected void initProperties() {

        // глобальные свойства
        requiredPeriod = addDProp(baseGroup, "votePeriod", "Срок заседания", IntegerClass.instance);
        requiredQuantity = addDProp(baseGroup, "voteRequiredQuantity", "Кол-во экспертов", IntegerClass.instance);
        limitExperts = addDProp(baseGroup, "limitExperts", "Кол-во прогол. экспертов", IntegerClass.instance);

        projectVote = addDProp(idGroup, "projectVote", "Проект (ИД)", project, vote);
        nameProjectVote = addJProp(baseGroup, "nameProjectVote", "Проект", name, projectVote, 1);

        clusterExpert = addDProp(idGroup, "clusterExpert", "Кластер (ИД)", cluster, expert);
        nameClusterExpert = addJProp(baseGroup, "nameClusterExpert", "Кластер", name, clusterExpert, 1);

        clusterProject = addDProp(idGroup, "clusterProject", "Кластер (ИД)", cluster, project);
        nameClusterProject = addJProp(baseGroup, "nameClusterProject", "Кластер", name, clusterProject, 1);

        clusterVote = addJProp(idGroup, "clusterVote", "Кластер (ИД)", clusterProject, projectVote, 1);
        nameClusterVote = addJProp(baseGroup, "nameClusterVote", "Кластер", name, clusterVote, 1);

        claimerProject = addDProp(idGroup, "claimerProject", "Заявитель (ИД)", claimer, project);
        nameClaimerProject = addJProp(baseGroup, "nameClaimerProject", "Заявитель", name, claimerProject, 1);

        claimerVote = addJProp(idGroup, "claimerVote", "Заявитель (ИД)", claimerProject, projectVote, 1);
        nameClaimerVote = addJProp(idGroup, "nameClaimerVote", "Заявитель", name, claimerVote, 1);

        emailParticipant = addDProp(baseGroup, "emailParticipant", "E-mail", StringClass.get(50), participant);

        projectDocument = addDProp(idGroup, "projectDocument", "Проект (ИД)", project, document);
        nameProjectDocument = addJProp(baseGroup, "nameProjectDocument", "Проект", name, projectDocument, 1);

        fileDocument = addDProp(baseGroup, "fileDocument", "Файл", WordClass.instance, document);

        inExpertVote = addDProp(baseGroup, "inExpertVote", "Вкл", LogicalClass.instance, expert, vote); // !!! нужно отослать письмо с документами и т.д

        dateStartVote = addJProp(baseGroup, "dateStartVote", "Дата начала", and1, date, 1, is(vote), 1);
        dateEndVote = addJProp(baseGroup, "dateEndVote", "Дата окончания", addDate2, dateStartVote, 1, requiredPeriod);

        openedVote = addJProp(baseGroup, "openedVote", "Открыто", greater2, dateEndVote, 1, currentDate);

        voteInProgressProject = addCGProp(idGroup, false, "voteInProgressProject", "Тек. заседание (ИД)",
                                       addJProp(and1, 1, openedVote, 1), openedVote,
                                       projectVote, 1); // активно только одно заседание

        // результаты голосования
        voteResultExpertVote = addDProp(idGroup, "voteResultExpertVote", "Результат (ИД)", voteResult, expert, vote);

        doneExpertVote = addJProp(baseGroup, "doneExpertVote", "Проголосовал", equals2,
                                  voteResultExpertVote, 1, 2, addCProp(voteResult, "voted"));

        nameVoteResultExpertVote = addJProp(voteResultCheckGroup, "nameVoteResultExpertVote", "Результат", name, voteResultExpertVote, 1, 2);
        inClusterExpertVote = addDProp(voteResultCheckGroup, "inClusterExpertVote", "Соотв-ие кластеру", LogicalClass.instance, expert, vote);
        innovativeExpertVote = addDProp(voteResultCheckGroup, "innovativeExpertVote", "Инновац.", LogicalClass.instance, expert, vote);
        innovativeCommentExpertVote = addDProp(voteResultCommentGroup, "innovativeCommentExpertVote", "Инновационность (комм.)", TextClass.instance, expert, vote);
        foreignExpertVote = addDProp(voteResultCheckGroup, "foreignExpertVote", "Иностр. специалист", LogicalClass.instance, expert, vote);
        competentExpertVote = addDProp(voteResultCheckGroup, "competentExpertVote", "Компет.", IntegerClass.instance, expert, vote);
        completeExpertVote = addDProp(voteResultCheckGroup, "completeExpertVote", "Полнота информ.", IntegerClass.instance, expert, vote);
        completeCommentExpertVote = addDProp(voteResultCommentGroup, "completeCommentExpertVote", "Полнота информации (комм.)", TextClass.instance, expert, vote);

        quantityDoneVote = addSGProp(baseGroup, "quantityDoneVote", "Проголосовало", addJProp(and1, addCProp(IntegerClass.instance, 1), doneExpertVote, 1, 2), 2); // сколько экспертов высказалось
        succeededVote = addJProp(baseGroup, "succeededVote", "Состоялось", groeq2, quantityDoneVote, 1, limitExperts); // достаточно экспертов

        voteSucceededProject = addCGProp(idGroup, false, "voteSucceededProject", "Успешное заседание (ИД)",
                                         addJProp(and1, 1, succeededVote, 1), succeededVote,
                                         projectVote, 1);

        noCurrentVoteProject = addJProp("noCurrentVoteProject", "Нет текущих заседаний", andNot1, is(project), 1, voteInProgressProject, 1); // нету текущих заседаний

        voteValuedProject = addJProp(idGroup, "voteValuedProject", "Оцененнное заседание (ИД)", and1, voteSucceededProject, 1, noCurrentVoteProject, 1); // нет открытого заседания и есть состояшееся заседания

        needExtraVoteProject = addJProp("needExtraVoteProject", "Треб. заседание", and(true, true),
                                        is(project), 1,
                voteInProgressProject, 1,
                                        voteSucceededProject, 1); // есть открытое заседания и есть состояшееся заседания !!! нужно создать новое заседание

        addConstraint(addJProp("Эксперт не соответствует необходимому кластеру", diff2,
                               clusterExpert, 1, addJProp(and1, clusterVote, 2, inExpertVote, 1, 2), 1, 2), false);

        addConstraint(addJProp("Количество экспертов не соответствует требуемому", andNot1, is(vote), 1, addJProp(equals2, requiredQuantity,
                                                                                                                  addSGProp(addJProp(and1, addCProp(IntegerClass.instance, 1), inExpertVote, 2, 1), 1), 1), 1), false);

        generateVote = addAProp(actionGroup, new GenerateVoteActionProperty());
        generateVote.setDerivedChange(addCProp(ActionClass.instance, true), needExtraVoteProject, 1);

        expertLogin = addCGProp(baseGroup, "expertLogin", "Эксперт (ИД)", object(expert), userLogin, userLogin, 1);

        addCUProp("userRole", true, "Роль пользователя", addCProp(StringClass.get(30), "expert", expert));

        statusProject = addIfElseUProp(idGroup, "statusProject", "Статус (ИД)",
                                  addCProp(projectStatus, "valued", project),
                                  addIfElseUProp(addCProp(projectStatus, "succeeded", project),
                                          addIfElseUProp(addCProp(projectStatus, "inProgress", project),
                                                  addIfElseUProp(addCProp(projectStatus, "needExtraVote", project),
                                                                 addCProp(projectStatus, "unknown", project),
                                                                 needExtraVoteProject, 1),
                                                         voteInProgressProject, 1),
                                                 voteSucceededProject, 1),
                                  voteValuedProject, 1);
        nameStatusProject = addJProp(baseGroup, "nameStatusProject", "Статус", name, statusProject, 1);
    }

    protected void initTables() {
    }

    protected void initIndexes() {
    }

    protected void initNavigators() throws JRException, FileNotFoundException {
        ExpertLetterFormEntity letterForm = new ExpertLetterFormEntity(baseElement, 30);
        addFormEntity(letterForm);

        addFormEntity(new ProjectFormEntity(baseElement, 10));
        addFormEntity(new ExpertFormEntity(baseElement, 15));
        addFormEntity(new GlobalFormEntity(baseElement, 20));
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
        admin.addSecurityPolicy(permitAllPolicy);
    }

    private class ProjectFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objProject;
        private ObjectEntity objVote;
        private ObjectEntity objDocument;
        private ObjectEntity objExpert;

        private ProjectFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Реестр проектов");

            objProject = addSingleGroupObject(project, objectValue, date, name, nameClusterProject, nameClaimerProject, nameStatusProject, generateVote);
            addObjectActions(this, objProject);

            objVote = addSingleGroupObject(vote, objectValue, dateStartVote, dateEndVote, openedVote, succeededVote, quantityDoneVote, delete);
            objVote.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.HIDE));

            objDocument = addSingleGroupObject(document, objectValue, name, fileDocument);
            addObjectActions(this, objDocument);

            objExpert = addSingleGroupObject(expert);
            addPropertyDraw(objExpert, objVote, inExpertVote);
            addPropertyDraw(objExpert, objectValue, name, emailParticipant);
            addPropertyDraw(voteResultGroup, true, objExpert, objVote);

            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectVote, objVote), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectDocument, objDocument), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(clusterExpert, objExpert), Compare.EQUALS, addPropertyObject(clusterProject, objProject)));

            RegularFilterGroupEntity expertFilterGroup = new RegularFilterGroupEntity(genID());
            expertFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)),
                                                                "В заседании",
                                                                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(expertFilterGroup);

            RegularFilterGroupEntity projectFilterGroup = new RegularFilterGroupEntity(genID());
            projectFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                 new NotNullFilterEntity(addPropertyObject(voteValuedProject, objProject)),
                                                                 "Оценен",
                                                                 KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(projectFilterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.addIntersection(design.getGroupObjectContainer(objVote.groupTo),
                                   design.getGroupObjectContainer(objDocument.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objProject.groupTo).grid.constraints.fillVertical = 1.5;
            design.get(objExpert.groupTo).grid.constraints.fillVertical = 1.5;

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 1);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, 1));

            return design;
        }
    }

    private class GlobalFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private GlobalFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Глобальные параметры");

            addPropertyDraw(new LP[]{currentDate, requiredPeriod, requiredQuantity, limitExperts});
        }
    }

    private class ExpertFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objExpert;
        private ObjectEntity objVote;

        private ExpertFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Реестр экспертов");

            objExpert = addSingleGroupObject(expert, selection, objectValue, userFirstName, userLastName, userLogin, userPassword, emailParticipant, nameClusterExpert);
            addObjectActions(this, objExpert);

            objVote = addSingleGroupObject(vote, objectValue, nameProjectVote, dateStartVote, dateEndVote, openedVote, succeededVote, quantityDoneVote, delete);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 0.5);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, 1));

            return design;
        }
    }

    private class ExpertLetterFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту
        private ObjectEntity objExpert;
        private ObjectEntity objVote;

        private ObjectEntity objDocument;

        private ExpertLetterFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Письмо о заседании");

            GroupObjectEntity gobjExpertVote = new GroupObjectEntity(genID());
            objExpert = new ObjectEntity(genID(), expert, "Эксперт");
            objVote = new ObjectEntity(genID(), vote, "Заседание");
            gobjExpertVote.add(objExpert);
            gobjExpertVote.add(objVote);
            addGroup(gobjExpertVote);
            gobjExpertVote.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objExpert, name);
            addPropertyDraw(objVote, nameClaimerVote, nameProjectVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)));

            objDocument = addSingleGroupObject(document, name, fileDocument);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectDocument, objDocument), Compare.EQUALS, addPropertyObject(projectVote, objVote)));

            addEAProp(baseGroup, "Отослать письмо", "Тест письма", Collections.<FormEntity>singletonList(this), Collections.singletonList(Arrays.asList(objExpert, objVote)));
        }
    }

    public VoteInfo getVoteInfo(String login, int voteId) throws RemoteException {

        try {

            DataSession session = createSession();
            VoteObjects vo = new VoteObjects(session, login, voteId);

            VoteInfo voteInfo = new VoteInfo();
            voteInfo.expertName = (String) name.read(session, vo.expertObj);
            voteInfo.projectName = (String) name.read(session, vo.projectObj);
            voteInfo.projectClaimer = (String) nameClaimerProject.read(session, vo.projectObj);
            voteInfo.projectCluster = (String) nameClusterProject.read(session, vo.projectObj);

            voteInfo.inCluster = nvl((Boolean) inClusterExpertVote.read(session, vo.expertObj, vo.voteObj), false);
            voteInfo.innovative = nvl((Boolean) innovativeExpertVote.read(session, vo.expertObj, vo.voteObj), false);
            voteInfo.innovativeComment = (String) innovativeCommentExpertVote.read(session, vo.expertObj, vo.voteObj);
            voteInfo.foreign = nvl((Boolean) foreignExpertVote.read(session, vo.expertObj, vo.voteObj), false);
            voteInfo.competent = nvl((Integer) competentExpertVote.read(session, vo.expertObj, vo.voteObj), 1);
            voteInfo.complete = nvl((Integer) completeExpertVote.read(session, vo.expertObj, vo.voteObj), 1);
            voteInfo.completeComment = (String) completeCommentExpertVote.read(session, vo.expertObj, vo.voteObj);
            voteInfo.voteDone = nvl((Boolean) doneExpertVote.read(session, vo.expertObj, vo.voteObj), false);

            return voteInfo;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о проекте", e);
        }
    }

    public void setVoteInfo(String login, int voteId, VoteInfo voteInfo) throws RemoteException {

        try {

            DataSession session = createSession();
            VoteObjects vo = new VoteObjects(session, login, voteId);

            voteResultExpertVote.execute(voteResult.getID(voteInfo.voteResult), session, vo.expertObj, vo.voteObj);
            inClusterExpertVote.execute(voteInfo.inCluster, session, vo.expertObj, vo.voteObj);
            innovativeExpertVote.execute(voteInfo.innovative, session, vo.expertObj, vo.voteObj);
            innovativeCommentExpertVote.execute(voteInfo.innovativeComment, session, vo.expertObj, vo.voteObj);
            foreignExpertVote.execute(voteInfo.foreign, session, vo.expertObj, vo.voteObj);
            competentExpertVote.execute(voteInfo.competent, session, vo.expertObj, vo.voteObj);
            completeExpertVote.execute(voteInfo.complete, session, vo.expertObj, vo.voteObj);
            completeCommentExpertVote.execute(voteInfo.completeComment, session, vo.expertObj, vo.voteObj);

            String result = session.apply(this);
            if (result != null) {
                throw new RuntimeException("Не удалось сохранить информацию о голосовании : " + result);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при записи информации о голосовании", e);
        }
    }

    private class VoteObjects {

        DataObject expertObj;
        DataObject voteObj;
        DataObject projectObj;

        private VoteObjects(DataSession session, String login, int voteId) throws SQLException {

            Integer expertId = (Integer) expertLogin.read(session, new DataObject(login, StringClass.get(30)));
            if (expertId == null) {
                throw new RuntimeException("Не удалось найти пользователя с логином " + login);
            }

            voteObj = new DataObject(voteId, vote);

            Integer projectId = (Integer) projectVote.read(session, voteObj);
            if (projectId == null) {
                throw new RuntimeException("Не удалось найти проект для заседания с идентификатором " + voteId);
            }

            expertObj = new DataObject(expertId, expert);

            Boolean inVote = (Boolean) inExpertVote.read(session, expertObj, voteObj);
            if (inVote == null || !inVote) {
                throw new RuntimeException("Эксперт с логином " + login + " не назначен на заседание с идентификатором " + voteId);
            }

            projectObj = new DataObject(projectId, project);
        }
    }

    public class GenerateVoteActionProperty extends ActionProperty {

        private final ClassPropertyInterface projectInterface;

        public GenerateVoteActionProperty() {
            super(genSID(), "Сгенерировать заседание", new ValueClass[]{project});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            projectInterface = i.next();
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            throw new RuntimeException("no need");
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            DataObject projectObject = keys.get(projectInterface);

            // нужно выбрать случайных экспертов из того же кластера
            Query<String, Object> query = new Query<String, Object>(Collections.singleton("key"));
            query.and(clusterExpert.getExpr(session.modifier, query.mapKeys.get("key")).compare(clusterProject.getExpr(session.modifier, projectObject.getExpr()), Compare.EQUALS));

            List<DataObject> experts = new ArrayList<DataObject>();
            for (Map<String, DataObject> row : query.executeClasses(session.sql, session.env, baseClass).keySet()) {
                experts.add(row.get("key"));
            }

            Integer required = nvl((Integer) requiredQuantity.read(session), 0);
            if (required > experts.size()) {
                logger.info("not enough experts");
                return;
            }

            DataObject voteObject = session.addObject(vote, session.modifier);
            projectVote.execute(projectObject.object, session, session.modifier, voteObject);

            Random rand = new Random();
            for (int i = 0; i < required; i++) {
                inExpertVote.execute(true, session, session.modifier, experts.remove(rand.nextInt(experts.size())), voteObject);
            }
        }
    }

}
