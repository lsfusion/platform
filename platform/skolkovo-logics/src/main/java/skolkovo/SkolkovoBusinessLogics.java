package skolkovo;

import net.sf.jasperreports.engine.JRException;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.ClientAction;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.linear.LP;
import platform.server.session.DataSession;
import platform.base.BaseUtils;
import skolkovo.api.remote.SkolkovoRemoteInterface;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

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

    protected void initGroups() {
    }

    protected void initClasses() {

        participant = addAbstractClass("participant", "Участник", customUser);

        project = addConcreteClass("project", "Проект", baseClass.named, transaction);
        expert = addConcreteClass("expert", "Эксперт", participant);
        cluster = addConcreteClass("cluster", "Кластер", baseClass.named);
        claimer = addConcreteClass("claimer", "Заявитель", participant);
        document = addConcreteClass("document", "Документ", baseClass.named);

        vote = addConcreteClass("vote", "Заседание", baseClass, transaction);

        voteResult = addStaticClass("voteResult", "Результат заседания", new String[]{"refused","connected","voted"}, new String[]{"Отказался", "Аффилирован", "Проголосовал"});
    }

    LP voteProject;
    LP expertCluster;
    LP projectCluster, projectClusterName;
    LP projectClaimer, projectClaimerName;
    LP participantEmail;

    LP documentProject;
    LP documentFile;

    LP expertInVote;
    LP voteEndDate;

    LP voteOpened;
    LP projectVote;
    LP requiredQuantity;

    LP expertVoteResult;
    LP expertVoteDone;
    LP expertVoteConnected;
    LP expertVoteInCluster;
    LP expertVoteInnovative;
    LP expertVoteInnovativeComment;
    LP expertVoteForeign;
    LP expertVoteCompetent;
    LP expertVoteComplete;
    LP expertVoteCompleteComment;

    LP projectExpertVote;
    LP voteDoneExperts;
    LP voteSuccessed;
    LP projectValuedVote;
    LP projectNeedExtraVote;

    LP generateVote;

    LP expertLogin;

    protected void initProperties() {

        LP addDays = addSFProp("prm1+prm2", DateClass.instance, 2);
        LP requiredPeriod = addDProp(baseGroup, "votePeriod", "Срок заседания", IntegerClass.instance);
        requiredQuantity = addDProp(baseGroup, "voteRequiredQuantity", "Кол-во экспертов", IntegerClass.instance);
        LP limitExperts = addDProp(baseGroup, "limitExperts", "Кол-во прогол. экспертов", IntegerClass.instance);

        voteProject = addDProp("voteProject", "Проект (ИД)", project, vote); addJProp(baseGroup, "Проект", name, voteProject, 1);
        expertCluster = addDProp("expertCluster", "Кластер (ИД)", cluster, expert); addJProp(baseGroup, "Кластер", name, expertCluster, 1);

        projectCluster = addDProp("projectCluster", "Кластер (ИД)", cluster, project); projectClusterName = addJProp(baseGroup, "Кластер", name, projectCluster, 1);
        LP voteCluster = addJProp("Кластер (ИД)", projectCluster, voteProject, 1); addJProp(baseGroup, "Кластер", name, voteCluster, 1);

        projectClaimer = addDProp("projectClaimer", "Заявитель (ИД)", claimer, project); projectClaimerName = addJProp(baseGroup, "Заявитель", name, projectClaimer, 1);
        participantEmail = addDProp(baseGroup, "participantEmail", "E-mail", StringClass.get(50), participant);

        documentProject = addDProp("documentProject", "Проект (ИД)", project, document); addJProp(baseGroup, "Проект", name, documentProject, 1);
        documentFile = addDProp("documentFile", "Файл", WordClass.instance, document);

        expertInVote = addDProp(baseGroup, "expertInVote", "Вкл", LogicalClass.instance, expert, vote); // !!! нужно отослать письмо с документами и т.д

        voteEndDate = addJProp("voteEndDate", "Дата окончания", addDays, addJProp(and1, date, 1, is(vote), 1), 1, requiredPeriod);

        voteOpened = addJProp(baseGroup, "voteOpened", "Открыто", greater2, voteEndDate, 1, currentDate);
        projectVote = addCGProp(baseGroup, false, "projectVote", "Тек. заседание", addJProp(and1, 1, voteOpened, 1), voteOpened, voteProject, 1); // активно только одно заседание

//        projectExpertVote = addCGProp(baseGroup, false, "projectExpertVote", "Заседание", addJProp(and1, 1, expertInVote, 2, 1), expertInVote, voteProject, 1, 2); // только один раз может голосовать

        // результаты голосования
        expertVoteResult = addDProp("expertVoteResult", "Результат (ИД)", voteResult, expert, vote);
        addJProp(baseGroup, "Результат", name, expertVoteResult, 1, 2);
        expertVoteDone = addJProp("Проголосовал", equals2, expertVoteResult, 1, 2, addCProp(voteResult, "voted"));

        expertVoteInCluster = addDProp(baseGroup, "expertVoteInCluster", "Соот-ет кластеру", LogicalClass.instance, expert, vote);
        expertVoteInnovative = addDProp(baseGroup, "expertVoteInnovative", "Подходит", LogicalClass.instance, expert, vote);
        expertVoteInnovativeComment = addDProp(baseGroup, "expertVoteInnovativeComment", "Подходит (комм.)", TextClass.instance, expert, vote);
        expertVoteForeign = addDProp(baseGroup, "expertVoteForeign", "Инн. спец.", LogicalClass.instance, expert, vote);
        expertVoteCompetent = addDProp(baseGroup, "expertVoteCompetent", "Компет.", IntegerClass.instance, expert, vote);
        expertVoteComplete = addDProp(baseGroup, "expertVoteComplete", "Полная инф.", IntegerClass.instance, expert, vote);
        expertVoteCompleteComment = addDProp(baseGroup, "expertVoteCompleteComment", "Полная инф. (комм.)", TextClass.instance, expert, vote);

        voteDoneExperts = addSGProp(baseGroup, "Проголосовало", addJProp(and1, addCProp(IntegerClass.instance, 1), expertVoteDone, 1, 2), 2); // сколько экспертов высказалось
        voteSuccessed = addJProp(baseGroup, "Состоялся", groeq2, voteDoneExperts, 1, limitExperts); // достаточно экспертов

        LP projectIsSuccessedVote = addCGProp(baseGroup, false, "projectIsSuccessedVote", "Достаточно оценок", addJProp(and1, 1, voteSuccessed, 1), voteSuccessed, voteProject, 1); // если есть состоявшееся заседание
        LP projectClosedVotes = addJProp(andNot1, is(project), 1, projectVote, 1); // нету заседаний
        projectValuedVote = addJProp(baseGroup, "projectValuedVote", "Оценен", and1, projectIsSuccessedVote, 1, projectClosedVotes, 1); // нет открытого заседания и есть состояшееся заседания
        projectNeedExtraVote = addJProp(baseGroup, "Треб. заседание", and(true, true), is(project), 1, projectVote, 1, projectIsSuccessedVote, 1); // есть открытое заседания и есть состояшееся заседания !!! нужно создать новое заседание

        addConstraint(addJProp("Эксперт не соответствует необходимому кластеру", diff2,
                        expertCluster, 1, addJProp(and1, voteCluster, 2, expertInVote, 1, 2), 1, 2), false);

        addConstraint(addJProp("Количество экспертов не соответствует требуемому", andNot1, is(vote), 1, addJProp(equals2, requiredQuantity,
                addSGProp(addJProp(and1, addCProp(IntegerClass.instance, 1), expertInVote, 2, 1), 1), 1), 1), false);

        generateVote = addAProp(baseGroup, new GenerateVoteActionProperty());
        generateVote.setDerivedChange(addCProp(ActionClass.instance,true), projectNeedExtraVote, 1);

        expertLogin = addCGProp(baseGroup, "expertLogin", "Эксперт (ИД)", object(expert), userLogin, userLogin, 1);
    }

    protected void initTables() {
    }

    protected void initIndexes() {
    }

    protected void initNavigators() throws JRException, FileNotFoundException {
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

            objProject = addSingleGroupObject(project, baseGroup);
            addObjectActions(this, objProject);

            objVote = addSingleGroupObject(vote, baseGroup);
            addObjectActions(this, objVote);

            objDocument = addSingleGroupObject(document, baseGroup);
            addObjectActions(this, objDocument);

            objExpert = addSingleGroupObject(expert, baseGroup);

            addPropertyDraw(objVote, objExpert, baseGroup);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(voteProject, objVote), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(documentProject, objDocument), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(expertCluster, objExpert), Compare.EQUALS, addPropertyObject(projectCluster, objProject)));

            RegularFilterGroupEntity expertFilterGroup = new RegularFilterGroupEntity(genID());
            expertFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(expertInVote, objExpert, objVote)),
                                  "В заседании",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(expertFilterGroup);

            RegularFilterGroupEntity projectFilterGroup = new RegularFilterGroupEntity(genID());
            projectFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(projectValuedVote, objProject)),
                                  "Оценен",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(projectFilterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView)super.createDefaultRichDesign();

            return design;
        }
    }

    private class GlobalFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private GlobalFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Глобальные параметры");

            addPropertyDraw(baseGroup, true);
        }
    }

    private class ExpertFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objExpert;
        private ObjectEntity objVote;

        private ExpertFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Статистика по экспертам");

            objExpert = addSingleGroupObject(expert, baseGroup);
            addObjectActions(this, objExpert);

            objVote = addSingleGroupObject(vote, baseGroup);

            addPropertyDraw(objVote, objExpert, baseGroup);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(expertInVote, objExpert, objVote)));
        }
    }

    public VoteInfo getVoteInfo(String login, int voteId) throws RemoteException {

        try {

            DataSession session = createSession();
            VoteObjects vo = new VoteObjects(session, login, voteId);

            VoteInfo voteInfo = new VoteInfo();
            voteInfo.expertName = (String)name.read(session, vo.expertObj);
            voteInfo.projectName = (String)name.read(session, vo.projectObj);
            voteInfo.projectClaimer = (String)projectClaimerName.read(session, vo.projectObj);
            voteInfo.projectCluster = (String)projectClusterName.read(session, vo.projectObj);

            voteInfo.connected = (Boolean)expertVoteConnected.read(session, vo.expertObj, vo.voteObj);
            voteInfo.inCluster = (Boolean)expertVoteInCluster.read(session, vo.expertObj, vo.voteObj);
            voteInfo.innovative = (Boolean)expertVoteInnovative.read(session, vo.expertObj, vo.voteObj);
            voteInfo.innovativeComment = (String)expertVoteInnovativeComment.read(session, vo.expertObj, vo.voteObj);
            voteInfo.foreign = (Boolean)expertVoteForeign.read(session, vo.expertObj, vo.voteObj);
            voteInfo.competent = (Integer)expertVoteCompetent.read(session, vo.expertObj, vo.voteObj);
            voteInfo.complete = (Integer)expertVoteComplete.read(session, vo.expertObj, vo.voteObj);
            voteInfo.completeComment = (String)expertVoteCompleteComment.read(session, vo.expertObj, vo.voteObj);
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

            expertVoteDone.execute(true, session, vo.expertObj, vo.voteObj);
            expertVoteConnected.execute(voteInfo.connected, session, vo.expertObj, vo.voteObj);
            expertVoteInCluster.execute(voteInfo.inCluster, session, vo.expertObj, vo.voteObj);
            expertVoteInnovative.execute(voteInfo.innovative, session, vo.expertObj, vo.voteObj);
            expertVoteInnovativeComment.execute(voteInfo.innovativeComment, session, vo.expertObj, vo.voteObj);
            expertVoteForeign.execute(voteInfo.foreign, session, vo.expertObj, vo.voteObj);
            expertVoteCompetent.execute(voteInfo.competent, session, vo.expertObj, vo.voteObj);
            expertVoteComplete.execute(voteInfo.complete, session, vo.expertObj, vo.voteObj);
            expertVoteCompleteComment.execute(voteInfo.completeComment, session, vo.expertObj, vo.voteObj);

            String result = session.apply(this);
            if (result != null)
                throw new RuntimeException("Не удалось сохранить информацию о голосовании : " + result);

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

            Integer expertId = (Integer)expertLogin.read(session, new DataObject(login, StringClass.get(30)));
            if (expertId == null) {
                throw new RuntimeException("Не удалось найти пользователя с логином " + login);
            }

            voteObj = new DataObject(voteId, vote);

            Integer projectId = (Integer)voteProject.read(session, voteObj);
            if (projectId == null) {
                throw new RuntimeException("Не удалось найти проект для заседания с идентификатором " + voteId);
            }

            expertObj = new DataObject(expertId, expert);

            Boolean inVote = (Boolean)expertInVote.read(session, expertObj, voteObj);
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
            query.and(expertCluster.getExpr(session.modifier,query.mapKeys.get("key")).compare(projectCluster.getExpr(session.modifier, projectObject.getExpr()), Compare.EQUALS));

            List<DataObject> experts = new ArrayList<DataObject>();
            for(Map<String, DataObject> row : query.executeClasses(session.sql, session.env, baseClass).keySet())
                experts.add(row.get("key"));

            Integer required = BaseUtils.nvl((Integer) requiredQuantity.read(session), 0);
            if(required > experts.size()) {
                logger.info("not enough experts");
                return;
            }

            DataObject voteObject = session.addObject(vote, session.modifier);
            voteProject.execute(projectObject.object, session, session.modifier, voteObject);

            Random rand = new Random();
            for(int i=0;i<required;i++)
                expertInVote.execute(true, session, session.modifier, experts.remove(rand.nextInt(experts.size())), voteObject);
        }
    }

}
