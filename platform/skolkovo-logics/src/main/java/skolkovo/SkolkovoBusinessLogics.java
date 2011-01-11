package skolkovo;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.data.expr.query.OrderType;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.GroupObjectHierarchy;
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
import platform.server.logics.EmailActionProperty;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
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

    StaticClass language;
    StaticClass documentType;

    StaticCustomClass voteResult;
    StaticCustomClass projectStatus;

    AbstractGroup voteResultGroup;
    AbstractGroup voteResultCheckGroup;
    AbstractGroup voteResultCommentGroup;

    AbstractGroup expertResultGroup;

    protected void initGroups() {
        voteResultGroup = new AbstractGroup("Результаты голосования");
        publicGroup.add(voteResultGroup);

        expertResultGroup = new AbstractGroup("Статистика по экспертам");
        publicGroup.add(expertResultGroup);

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
        document = addConcreteClass("document", "Документ", baseClass);

        vote = addConcreteClass("vote", "Заседание", baseClass, transaction);

        language = addStaticClass("language", "Язык",
                new String[]{"russian", "english"},
                new String[]{"Русский", "Английский"});

        voteResult = addStaticClass("voteResult", "Результат заседания", 
                                    new String[]{"refused", "connected", "voted"}, 
                                    new String[]{"Отказался", "Аффилирован", "Проголосовал"});
        
        projectStatus = addStaticClass("projectStatus", "Статус проекта",
                                       new String[]{"unknown", "needDocuments", "needExtraVote", "inProgress", "succeeded", "accepted", "rejected"},
                                       new String[]{"Неизвестный статус", "Не соответствуют документы", "Требуется заседание", "Идет заседание", "Достаточно голосов", "Оценен положительно", "Оценен отрицательно"});

        documentType = addStaticClass("documentType", "Тип документа",
                                    new String[]{"application", "resume", "techdesc", "forres"},
                                    new String[]{"Анкета", "Резюме", "Техническое описание", "Резюме иностранного специалиста "});
    }

    LP projectVote, nameProjectVote;
    LP clusterExpert, nameClusterExpert;
    LP clusterProject, nameClusterProject;
    LP clusterVote, nameClusterVote;
    LP claimerProject, nameClaimerProject;
    LP emailParticipant;
    LP emailDocuments;

    LP claimerVote, nameClaimerVote;

    LP projectDocument, nameProjectDocument;
    LP fileDocument;

    LP inExpertVote;
    LP dateStartVote, dateEndVote;

    LP openedVote;
    LP closedVote;
    LP voteInProgressProject;
    LP requiredPeriod;
    LP requiredQuantity;
    LP limitExperts;

    LP dateExpertVote;
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

    LP quantityRepliedVote;
    LP quantityDoneVote;
    LP quantityInClusterVote;
    LP quantityInnovativeVote;
    LP quantityForeignVote;
    LP acceptedInClusterVote;
    LP acceptedInnovativeVote;
    LP acceptedForeignVote;

    LP acceptedVote;
    LP succeededVote;
    LP voteSucceededProject;
    LP noCurrentVoteProject;
    LP voteValuedProject;
    LP acceptedProject;
    LP voteRejectedProject;
    LP needExtraVoteProject;

    LP emailLetterExpertVote;
    LP allowedEmailLetterExpertVote;
    LP emailStartVote;
    LP emailProtocolVote;
    LP emailAuthExpert;

    LP generateVoteProject, hideGenerateVoteProject;
    LP generateLoginPasswordExpert;

    LP expertLogin;

    LP statusProject, nameStatusProject;

    LP quantityTotalExpert;
    LP quantityDoneExpert;
    LP percentDoneExpert;
    LP percentInClusterExpert;
    LP percentInnovativeExpert;
    LP percentForeignExpert;

    LP datePrevVote;
    LP dateProjectVote;
    LP numberExpertVote;

    LP languageExpert;
    LP nameLanguageExpert;
    LP languageDocument;
    LP nameLanguageDocument;
    LP typeDocument;
    LP nameTypeDocument;
    LP postfixDocument;
    LP hidePostfixDocument;

    LP quantityMinLanguageDocumentType;
    LP quantityMaxLanguageDocumentType;
    LP quantityProjectLanguageDocumentType;
    LP translateLanguageDocumentType;
    LP notEnoughProject;
    LP autoGenerateProject;

    LP nameDocument;

    LP isForeignExpert;

    protected void initProperties() {

        LP percent = addSFProp("(prm1*100/prm2)", DoubleClass.instance, 2);

        // глобальные свойства
        requiredPeriod = addDProp(baseGroup, "votePeriod", "Срок заседания", IntegerClass.instance);
        requiredQuantity = addDProp(baseGroup, "voteRequiredQuantity", "Кол-во экспертов", IntegerClass.instance);
        limitExperts = addDProp(baseGroup, "limitExperts", "Кол-во прогол. экспертов", IntegerClass.instance);

        projectVote = addDProp(idGroup, "projectVote", "Проект (ИД)", project, vote);
        nameProjectVote = addJProp(baseGroup, "nameProjectVote", "Проект", name, projectVote, 1);

        dateProjectVote = addJProp("dateProjectVote", "Дата проекта", date, projectVote, 1);

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

        quantityMinLanguageDocumentType = addDProp(baseGroup, "quantityMinLanguageDocumentType", "Мин. док.", IntegerClass.instance, language, documentType);
        quantityMaxLanguageDocumentType = addDProp(baseGroup, "quantityMaxLanguageDocumentType", "Макс. док.", IntegerClass.instance, language, documentType);
        LP singleLanguageDocumentType = addJProp("Один док.", equals2, quantityMaxLanguageDocumentType, 1, 2, addCProp(IntegerClass.instance, 1));
        LP multipleLanguageDocumentType = addJProp(andNot1, addCProp(LogicalClass.instance, true, language, documentType), 1, 2, singleLanguageDocumentType, 1, 2);
        translateLanguageDocumentType = addDProp(baseGroup, "translateLanguageDocumentType", "Перевод", StringClass.get(50), language, documentType);

        languageExpert = addDProp(idGroup, "languageExpert", "Язык (ИД)", language, expert);
        nameLanguageExpert = addJProp(baseGroup, "nameLanguageExpert", "Язык", name, languageExpert, 1);
        languageDocument = addDProp(idGroup, "languageDocument", "Язык (ИД)", language, document);
        nameLanguageDocument = addJProp(baseGroup, "nameLanguageDocument", "Язык", name, languageDocument, 1);
        typeDocument = addDProp(idGroup, "typeDocument", "Тип (ИД)", documentType, document);
        nameTypeDocument = addJProp(baseGroup, "nameTypeDocument", "Тип", name, typeDocument, 1);

        LP multipleDocument = addJProp(multipleLanguageDocumentType, languageDocument, 1, typeDocument, 1);
        postfixDocument = addJProp(and1, addDProp("postfixDocument", "Доп. описание", StringClass.get(15), document), 1, multipleDocument, 1);
        hidePostfixDocument = addJProp(and1, addCProp(StringClass.get(40), "Постфикс", document), 1, multipleDocument, 1);

        LP translateNameDocument = addJProp("Перевод", translateLanguageDocumentType, languageDocument, 1, typeDocument, 1);
        nameDocument = addJProp(string2, translateNameDocument, 1, addSUProp(Union.OVERRIDE, addCProp(StringClass.get(15),"", document), postfixDocument), 1);

        quantityProjectLanguageDocumentType = addSGProp("projectLanguageDocumentType", "Кол-во док.", addCProp(IntegerClass.instance, 1, document), projectDocument, 1, languageDocument, 1, typeDocument, 1); // сколько экспертов высказалось
        LP notEnoughProjectLanguageDocumentType = addSUProp(Union.OVERRIDE, addJProp(greater2, quantityProjectLanguageDocumentType, 1, 2, 3, quantityMaxLanguageDocumentType, 2, 3),
                addJProp(less2, addSUProp(Union.OVERRIDE, addCProp(IntegerClass.instance, 0, project, language, documentType), quantityProjectLanguageDocumentType), 1, 2, 3, quantityMinLanguageDocumentType, 2, 3));
        notEnoughProject = addMGProp(baseGroup, "notEnoughProject", "Недостаточно док.", notEnoughProjectLanguageDocumentType, 1);

        autoGenerateProject = addDProp(baseGroup, "autoGenerateProject", "Авт. зас.", LogicalClass.instance, project);

        fileDocument = addDProp(baseGroup, "fileDocument", "Файл", PDFClass.instance, document);

        inExpertVote = addDProp(baseGroup, "inExpertVote", "Вкл", LogicalClass.instance, expert, vote); // !!! нужно отослать письмо с документами и т.д

        dateStartVote = addJProp(baseGroup, "dateStartVote", "Дата начала", and1, date, 1, is(vote), 1);
        dateEndVote = addJProp(baseGroup, "dateEndVote", "Дата окончания", addDate2, dateStartVote, 1, requiredPeriod);

        openedVote = addJProp(baseGroup, "openedVote", "Открыто", greater2, dateEndVote, 1, currentDate);
        closedVote = addJProp(baseGroup, "closedVote", "Закрыто", andNot1, is(vote), 1, openedVote, 1);

        voteInProgressProject = addCGProp(idGroup, false, "voteInProgressProject", "Тек. заседание (ИД)",
                                       addJProp(and1, 1, openedVote, 1), openedVote,
                                       projectVote, 1); // активно только одно заседание

        // результаты голосования
        voteResultExpertVote = addDProp(idGroup, "voteResultExpertVote", "Результат (ИД)", voteResult, expert, vote);
        dateExpertVote = addDProp(voteResultCheckGroup, "dateExpertVote", "Дата рез.", DateClass.instance, expert, vote);

        doneExpertVote = addJProp(baseGroup, "doneExpertVote", "Проголосовал", equals2,
                                  voteResultExpertVote, 1, 2, addCProp(voteResult, "voted"));

        nameVoteResultExpertVote = addJProp(voteResultCheckGroup, "nameVoteResultExpertVote", "Результат", name, voteResultExpertVote, 1, 2);

        inClusterExpertVote = addDProp(voteResultCheckGroup, "inClusterExpertVote", "Соотв-ие кластеру", LogicalClass.instance, expert, vote);
        innovativeExpertVote = addDProp(voteResultCheckGroup, "innovativeExpertVote", "Инновац.", LogicalClass.instance, expert, vote);
        foreignExpertVote = addDProp(voteResultCheckGroup, "foreignExpertVote", "Иностр. специалист", LogicalClass.instance, expert, vote);

        innovativeCommentExpertVote = addDProp(voteResultCommentGroup, "innovativeCommentExpertVote", "Инновационность (комм.)", TextClass.instance, expert, vote);
        competentExpertVote = addDProp(voteResultCheckGroup, "competentExpertVote", "Компет.", IntegerClass.instance, expert, vote);
        completeExpertVote = addDProp(voteResultCheckGroup, "completeExpertVote", "Полнота информ.", IntegerClass.instance, expert, vote);
        completeCommentExpertVote = addDProp(voteResultCommentGroup, "completeCommentExpertVote", "Полнота информации (комм.)", TextClass.instance, expert, vote);


        quantityRepliedVote = addSGProp(voteResultGroup, "quantityRepliedVote", "Ответило",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), voteResultExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityDoneVote = addSGProp(voteResultGroup, "quantityDoneVote", "Проголосовало",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), doneExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityInClusterVote = addSGProp(voteResultGroup, "quantityInClusterVote", "Соотв-ие кластеру (голоса)",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), inClusterExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityInnovativeVote = addSGProp(voteResultGroup, "quantityInnovativeVote", "Инновац. (голоса)",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), innovativeExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityForeignVote = addSGProp(voteResultGroup, "quantityForeignVote", "Иностр. специалист (голоса)",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), foreignExpertVote, 1, 2), 2); // сколько экспертов высказалось

        acceptedInClusterVote = addJProp(voteResultGroup, "acceptedInClusterVote", "Соотв-ие кластеру", greater2,
                                          addJProp(multiplyIntegerBy2, quantityInClusterVote, 1), 1,
                                          quantityDoneVote, 1);

        acceptedInnovativeVote = addJProp(voteResultGroup, "acceptedInnovativeVote", "Инновац.", greater2,
                                           addJProp(multiplyIntegerBy2, quantityInnovativeVote, 1), 1,
                                           quantityDoneVote, 1);

        acceptedForeignVote = addJProp(voteResultGroup, "acceptedForeignVote", "Иностр. специалист", greater2,
                                        addJProp(multiplyIntegerBy2, quantityForeignVote, 1), 1,
                                        quantityDoneVote, 1);

        acceptedVote = addJProp(voteResultGroup, "acceptedVote", "Положительно", and(false, false),
                                acceptedInClusterVote, 1, acceptedInnovativeVote, 1, acceptedForeignVote, 1);

        succeededVote = addJProp(voteResultGroup, "succeededVote", "Состоялось", groeq2, quantityDoneVote, 1, limitExperts); // достаточно экспертов

        voteSucceededProject = addCGProp(idGroup, false, "voteSucceededProject", "Успешное заседание (ИД)",
                addJProp(and1, 1, succeededVote, 1), succeededVote,
                projectVote, 1);

        noCurrentVoteProject = addJProp(baseGroup, "noCurrentVoteProject", "Нет текущих заседаний", andNot1, is(project), 1, voteInProgressProject, 1); // нету текущих заседаний

        voteValuedProject = addJProp(idGroup, "voteValuedProject", "Оцененнное заседание (ИД)", and1, voteSucceededProject, 1, noCurrentVoteProject, 1); // нет открытого заседания и есть состояшееся заседания

        acceptedProject = addJProp(baseGroup, "acceptedProject", "Оценен положительно", acceptedVote, voteValuedProject, 1);
        voteRejectedProject = addJProp(baseGroup, "rejectedProject", "Оценен отрицательно", andNot1, voteValuedProject, 1, acceptedVote, 1);

        needExtraVoteProject = addJProp("needExtraVoteProject", "Треб. заседание", and(true, true, true),
                                        is(project), 1,
                                        notEnoughProject, 1,
                                        voteInProgressProject, 1,
                                        voteSucceededProject, 1); // есть открытое заседания и есть состояшееся заседания !!! нужно создать новое заседание

        addConstraint(addJProp("Эксперт не соответствует необходимому кластеру", diff2,
                               clusterExpert, 1, addJProp(and1, clusterVote, 2, inExpertVote, 1, 2), 1, 2), false);

        addConstraint(addJProp("Количество экспертов не соответствует требуемому", andNot1, is(vote), 1, addJProp(equals2, requiredQuantity,
                                                                                                                  addSGProp(addJProp(and1, addCProp(IntegerClass.instance, 1), inExpertVote, 2, 1), 1), 1), 1), false);

        generateVoteProject = addAProp(actionGroup, new GenerateVoteActionProperty());
        hideGenerateVoteProject = addHideCaptionProp(privateGroup, "Сгенерировать заседание", generateVoteProject, needExtraVoteProject);
        generateVoteProject.setDerivedForcedChange(addCProp(ActionClass.instance, true), needExtraVoteProject, 1, autoGenerateProject, 1);

        generateLoginPasswordExpert = addAProp(actionGroup, new GenerateLoginPasswordActionProperty());
        generateLoginPasswordExpert.setDerivedForcedChange(addCProp(ActionClass.instance, true), is(expert), 1);

        expertLogin = addCGProp(baseGroup, "expertLogin", "Эксперт (ИД)", object(expert), userLogin, userLogin, 1);

        addCUProp("userRole", true, "Роль пользователя", addCProp(StringClass.get(30), "expert", expert));

        statusProject = addIfElseUProp(idGroup, "statusProject", "Статус (ИД)",
                                  addCProp(projectStatus, "accepted", project),
                                  addIfElseUProp(addCProp(projectStatus, "rejected", project),
                                                 addIfElseUProp(addCProp(projectStatus, "succeeded", project),
                                                                addIfElseUProp(addCProp(projectStatus, "inProgress", project),
                                                                               addIfElseUProp(addCProp(projectStatus, "needExtraVote", project),
                                                                                           addIfElseUProp(addCProp(projectStatus, "needDocuments", project),
                                                                                                 addCProp(projectStatus, "unknown", project),
                                                                                                 notEnoughProject, 1),
                                                                                           needExtraVoteProject, 1),
                                                                               voteInProgressProject, 1),
                                                                voteSucceededProject, 1),
                                                 voteRejectedProject, 1),
                                  acceptedProject, 1);
        nameStatusProject = addJProp(baseGroup, "nameStatusProject", "Статус", name, statusProject, 1);

        // статистика по экспертам
        quantityTotalExpert = addSGProp(expertResultGroup, "quantityTotalExpert", "Всего заседаний",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), inExpertVote, 1, 2), 1);

        quantityDoneExpert = addSGProp(expertResultGroup, "quantityDoneExpert", "Проголосовал",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), doneExpertVote, 1, 2), 1);
        percentDoneExpert = addJProp(expertResultGroup, "percentDoneExpert", "Проголосовал (%)", percent, quantityDoneExpert, 1, quantityTotalExpert, 1);

        LP quantityInClusterExpert = addSGProp("quantityInClusterExpert", "Соотв-ие кластеру (голоса)",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), inClusterExpertVote, 1, 2), 1);
        percentInClusterExpert = addJProp(expertResultGroup, "percentInClusterExpert", "Соотв-ие кластеру (%)", percent, quantityInClusterExpert, 1, quantityDoneExpert, 1);

        LP quantityInnovativeExpert = addSGProp("quantityInnovativeExpert", "Инновац. (голоса)",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), innovativeExpertVote, 1, 2), 1);
        percentInnovativeExpert = addJProp(expertResultGroup, "percentInnovativeExpert", "Инновац. (%)", percent, quantityInnovativeExpert, 1, quantityDoneExpert, 1);

        LP quantityForeignExpert = addSGProp("quantityForeignExpert", "Иностр. специалист (голоса)",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), foreignExpertVote, 1, 2), 1);
        percentForeignExpert = addJProp(expertResultGroup, "percentForeignExpert", "Иностр. специалист (%)", percent, quantityForeignExpert, 1, quantityDoneExpert, 1);

        datePrevVote = addOProp("prevDateVote", "Пред. засед.", OrderType.PREVIOUS, dateEndVote, true, true, 1, projectVote, 1, date, 1);
        numberExpertVote = addOProp("numberExpertVote", "Номер", OrderType.SUM, addJProp(and1, addCProp(IntegerClass.instance, 1), inExpertVote, 1, 2), true, true, 1, 2, 1);

        emailDocuments = addDProp(baseGroup, "emailDocuments", "E-mail для документов", StringClass.get(50));

        emailLetterExpertVote = addEAProp(privateGroup, "Письмо о заседании (e-mail)", "Письмо о заседании", expert, vote);
        addEARecepient(emailLetterExpertVote, emailParticipant, 1);
        emailLetterExpertVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), inExpertVote, 1, 2);

        allowedEmailLetterExpertVote = addJProp(baseGroup, "Письмо о заседании (e-mail)", "Письмо о заседании", andNot1, emailLetterExpertVote, 1, 2, voteResultExpertVote, 1, 2);
        allowedEmailLetterExpertVote.property.askConfirm = true;

        emailStartVote = addEAProp(baseGroup, "Созыв заседания (e-mail)", "Созыв заседания", vote);
        addEARecepient(emailStartVote, emailDocuments);
        emailStartVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), openedVote, 1);

        emailProtocolVote = addEAProp(baseGroup, "Протокол заседания (e-mail)", "Протокол заседания", vote);
        addEARecepient(emailProtocolVote, emailDocuments);
        emailProtocolVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), closedVote, 1);

        isForeignExpert = addJProp(equals2, languageExpert, 1, addCProp(language, "english"));
        emailAuthExpert = addEAProp(baseGroup, "Аутентификация эксперта (e-mail)", "Аутентификация эксперта", expert);
        addEARecepient(emailAuthExpert, emailParticipant, 1);
        emailAuthExpert.setDerivedChange(addCProp(ActionClass.instance, true), userLogin, 1, userPassword, 1);
    }

    protected void initTables() {
    }

    protected void initIndexes() {
    }

    protected void initNavigators() throws JRException, FileNotFoundException {
        addFormEntity(new ProjectFormEntity(baseElement, 10));
        addFormEntity(new VoteFormEntity(baseElement, 15));
        addFormEntity(new ExpertFormEntity(baseElement, 18));
        addFormEntity(new LanguageDocumentTypeFormEntity(baseElement, 25));
        addFormEntity(new GlobalFormEntity(baseElement, 20));

        NavigatorElement print = new NavigatorElement(baseElement, 60, "Печатные формы");
        addFormEntity(new VoteStartFormEntity(print, 40));
        addFormEntity(new ExpertLetterFormEntity(print, 30));
        addFormEntity(new VoteProtocolFormEntity(print, 35));
        addFormEntity(new ExpertAuthFormEntity(print, 45));
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

            objProject = addSingleGroupObject(project, objectValue, date, name, nameClusterProject, nameClaimerProject, nameStatusProject, autoGenerateProject, generateVoteProject);
            addObjectActions(this, objProject);

            getPropertyDraw(generateVoteProject).forceViewType = ClassViewType.PANEL;
            getPropertyDraw(generateVoteProject).propertyCaption = addPropertyObject(hideGenerateVoteProject, objProject);

            objVote = addSingleGroupObject(vote, objectValue, dateStartVote, dateEndVote, openedVote, succeededVote, acceptedVote, quantityDoneVote, quantityInClusterVote, quantityInnovativeVote, quantityForeignVote, delete);
            objVote.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.HIDE));

            objDocument = addSingleGroupObject(document, objectValue, nameTypeDocument, nameLanguageDocument, postfixDocument, fileDocument);
            addObjectActions(this, objDocument);
            getPropertyDraw(postfixDocument).forceViewType = ClassViewType.PANEL;
            getPropertyDraw(postfixDocument).propertyCaption = addPropertyObject(hidePostfixDocument, objDocument);

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

            design.get(objVote.groupTo).grid.constraints.fillHorizontal = 1.5;

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

            addPropertyDraw(new LP[]{currentDate, requiredPeriod, requiredQuantity, limitExperts, emailDocuments});
        }
    }

    private class VoteFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objVote;
        private ObjectEntity objExpert;

        private VoteFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Реестр заседаний");

            objVote = addSingleGroupObject(vote, objectValue, nameProjectVote, dateStartVote, dateEndVote, openedVote, succeededVote, quantityDoneVote, delete);

            objExpert = addSingleGroupObject(expert, selection, objectValue, userFirstName, userLastName, userLogin, userPassword, emailParticipant, nameClusterExpert);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            addPropertyDraw(objExpert, objVote, allowedEmailLetterExpertVote);
            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)));
            
            RegularFilterGroupEntity voteFilterGroup = new RegularFilterGroupEntity(genID());
            voteFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                new NotNullFilterEntity(addPropertyObject(openedVote, objVote)),
                                                                "Открыто",
                                                                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            voteFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                new NotNullFilterEntity(addPropertyObject(succeededVote, objVote)),
                                                                "Состоялось",
                                                                KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), false);
            voteFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                new NotNullFilterEntity(addPropertyObject(acceptedVote, objVote)),
                                                                "Положительно",
                                                                KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), false);
            addRegularFilterGroup(voteFilterGroup);
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

    private class ExpertFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objExpert;
        private ObjectEntity objVote;

        private ExpertFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Реестр экспертов");

            objExpert = addSingleGroupObject(expert, selection, objectValue, userFirstName, userLastName, userLogin, userPassword, emailParticipant, nameClusterExpert, expertResultGroup, generateLoginPasswordExpert, emailAuthExpert);
            addObjectActions(this, objExpert);

            objVote = addSingleGroupObject(vote, objectValue, nameProjectVote, dateStartVote, dateEndVote, openedVote, succeededVote, quantityDoneVote, delete);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            addPropertyDraw(objExpert, objVote, allowedEmailLetterExpertVote);
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

    private class LanguageDocumentTypeFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту
        private ObjectEntity objLanguage;
        private ObjectEntity objDocumentType;

        private GroupObjectEntity gobjLanguageDocumentType;

        private LanguageDocumentTypeFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Обязательные документы");

            gobjLanguageDocumentType = new GroupObjectEntity(genID());
            objLanguage = new ObjectEntity(genID(), language, "Язык");
            objDocumentType = new ObjectEntity(genID(), documentType, "Тип документа");
            gobjLanguageDocumentType.add(objLanguage);
            gobjLanguageDocumentType.add(objDocumentType);
            addGroup(gobjLanguageDocumentType);

            addPropertyDraw(objLanguage, name);
            addPropertyDraw(objDocumentType, name);
            addPropertyDraw(objLanguage, objDocumentType, quantityMinLanguageDocumentType, quantityMaxLanguageDocumentType, translateLanguageDocumentType);
        }
    }

    private class ExpertLetterFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту
        private ObjectEntity objExpert;
        private ObjectEntity objVote;

        private ObjectEntity objDocument;

        private GroupObjectEntity gobjExpertVote;

        private ExpertLetterFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Письмо о заседании", true);

            gobjExpertVote = new GroupObjectEntity(2);
            objExpert = new ObjectEntity(2, expert, "Эксперт");
            objVote = new ObjectEntity(3, vote, "Заседание");
            gobjExpertVote.add(objExpert);
            gobjExpertVote.add(objVote);
            addGroup(gobjExpertVote);
            gobjExpertVote.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objExpert, name);
            addPropertyDraw(objVote, nameClaimerVote, nameProjectVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)));

            objDocument = addSingleGroupObject(8, document, fileDocument);
            addPropertyDraw(nameDocument, objDocument).setSID("docName");
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectDocument, objDocument), Compare.EQUALS, addPropertyObject(projectVote, objVote)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(languageDocument, objDocument), Compare.EQUALS, addPropertyObject(languageExpert, objExpert)));

            addEAForm(emailLetterExpertVote, EmailActionProperty.Format.HTML, this, objExpert, 1, objVote, 2);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(gobjExpertVote);
        }
    }

    private class ExpertAuthFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту о логине
        private ObjectEntity objExpert;

        private ExpertAuthFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Аутентификация эксперта", true);

            objExpert = addSingleGroupObject(1, expert, userLogin, userPassword, name, isForeignExpert);
            objExpert.groupTo.initClassView = ClassViewType.PANEL;

            addEAForm(emailAuthExpert, EmailActionProperty.Format.HTML, this, objExpert, 1);
        }
    }

    private class VoteStartFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objVote;

        private ObjectEntity objExpert;

        private VoteStartFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Созыв заседания", true);

            objVote = addSingleGroupObject(1, vote, date, dateProjectVote, nameClaimerVote, datePrevVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            objExpert = addSingleGroupObject(2, expert, name);
            addPropertyDraw(numberExpertVote, objExpert, objVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)));

            addEAForm(emailStartVote, EmailActionProperty.Format.PDF, this, objVote, 1);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(objVote.groupTo);
        }
    }

    private class VoteProtocolFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objVote;
        
        private ObjectEntity objExpert;

        private VoteProtocolFormEntity(NavigatorElement parent, int iID) {
            super(parent, iID, "Протокол заседания", true);

            objVote = addSingleGroupObject(1, vote, date, nameClaimerVote, quantityRepliedVote, quantityDoneVote, succeededVote, quantityInClusterVote, acceptedInClusterVote, quantityInnovativeVote, acceptedInnovativeVote, quantityForeignVote, acceptedForeignVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            objExpert = addSingleGroupObject(12, expert, userFirstName, userLastName);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(doneExpertVote, objExpert, objVote)));

            addEAForm(emailProtocolVote, EmailActionProperty.Format.PDF, this, objVote, 1);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(objVote.groupTo);
        }
    }

    public VoteInfo getVoteInfo(String login, int voteId) throws RemoteException {

        try {
            DataSession session = createSession();
            try {
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

                Integer vResult = (Integer) voteResultExpertVote.read(session, vo.expertObj, vo.voteObj);
                if (vResult != null) {
                    voteInfo.voteResult = voteResult.getSID(vResult);
                }

                voteInfo.voteDone = nvl((Boolean) doneExpertVote.read(session, vo.expertObj, vo.voteObj), false);
                voteInfo.date = DateConverter.sqlToDate((java.sql.Date)dateExpertVote.read(session, vo.expertObj, vo.voteObj));

                return voteInfo;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о проекте", e);
        }
    }

    public void setVoteInfo(String login, int voteId, VoteInfo voteInfo) throws RemoteException {

        try {
            DataSession session = createSession();
            try {
                VoteObjects vo = new VoteObjects(session, login, voteId);

                dateExpertVote.execute(DateConverter.dateToSql(voteInfo.date), session, vo.expertObj, vo.voteObj);
                voteResultExpertVote.execute(voteResult.getID(voteInfo.voteResult), session, vo.expertObj, vo.voteObj);
                if (voteInfo.voteResult.equals("voted")) {
                    inClusterExpertVote.execute(voteInfo.inCluster, session, vo.expertObj, vo.voteObj);
                    innovativeExpertVote.execute(voteInfo.innovative, session, vo.expertObj, vo.voteObj);
                    innovativeCommentExpertVote.execute(voteInfo.innovativeComment, session, vo.expertObj, vo.voteObj);
                    foreignExpertVote.execute(voteInfo.foreign, session, vo.expertObj, vo.voteObj);
                    competentExpertVote.execute(voteInfo.competent, session, vo.expertObj, vo.voteObj);
                    completeExpertVote.execute(voteInfo.complete, session, vo.expertObj, vo.voteObj);
                    completeCommentExpertVote.execute(voteInfo.completeComment, session, vo.expertObj, vo.voteObj);
                }

                String result = session.apply(this);
                if (result != null) {
                    throw new RuntimeException("Не удалось сохранить информацию о голосовании : " + result);
                }
            } finally {
                session.close();
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
                actions.add(new MessageClientAction("Недостаточно экспертов по кластеру", "Генерация заседания"));
                return;
            }

            DataObject voteObject = session.addObject(vote, session.modifier);
            projectVote.execute(projectObject.object, session, session.modifier, voteObject);

            Random rand = new Random();
            for (int i = 0; i < required; i++) {
                inExpertVote.execute(true, session, session.modifier, experts.remove(rand.nextInt(experts.size())), voteObject);
            }
        }

        @Override
        public Set<Property> getChangeProps() {
            return BaseUtils.toSet(projectVote.property, inExpertVote.property);
        }
    }

    public class GenerateLoginPasswordActionProperty extends ActionProperty {

        private final ClassPropertyInterface expertInterface;

        public GenerateLoginPasswordActionProperty() {
            super(genSID(), "Сгенерировать логин и пароль", new ValueClass[]{expert});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            expertInterface = i.next();
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            throw new RuntimeException("no need");
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            DataObject expertObject = keys.get(expertInterface);

            String currentEmail = (String) emailParticipant.read(session, expertObject);

            String login;
            int indexMail = currentEmail.indexOf("@");
            if(indexMail>=0)
                login = currentEmail.substring(0, indexMail);
            else
                login = "login" + expertObject.object;

            Random rand = new Random();
            String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
            String password = "";
            for(int i=0;i<8;i++)
                password += chars.charAt(rand.nextInt(chars.length()));

            userLogin.execute(login, session, expertObject);
            userPassword.execute(password, session, expertObject);
        }
    }
}
