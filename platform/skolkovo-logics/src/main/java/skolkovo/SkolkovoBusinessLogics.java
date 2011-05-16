package skolkovo;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.auth.PolicyManager;
import platform.server.auth.User;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.OrderType;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.mail.EmailActionProperty;
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
import java.io.InputStream;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

import static java.util.Arrays.asList;
import static platform.base.BaseUtils.nvl;

public class SkolkovoBusinessLogics extends BusinessLogics<SkolkovoBusinessLogics> implements SkolkovoRemoteInterface {

    private LP inExpertVoteDateFromDateTo;
    private LP quantityInExpertDateFromDateTo;

    public SkolkovoBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    AbstractCustomClass multiLanguageNamed;

    ConcreteCustomClass project;
    ConcreteCustomClass expert;
    ConcreteCustomClass cluster;
    ConcreteCustomClass claimer;
    ConcreteCustomClass documentTemplate;
    ConcreteCustomClass documentAbstract;
    ConcreteCustomClass documentTemplateDetail;
    ConcreteCustomClass document;

    ConcreteCustomClass nonRussianSpecialist;
    ConcreteCustomClass academic;
    ConcreteCustomClass patent;

    ConcreteCustomClass vote;

    StaticCustomClass projectType;
    StaticCustomClass language;
    StaticCustomClass documentType;
    StaticCustomClass ownerType;

    StaticCustomClass voteResult;
    StaticCustomClass projectStatus;

    AbstractGroup projectInformationGroup;
    AbstractGroup innovationGroup;
    AbstractGroup executiveSummaryGroup;
    AbstractGroup sourcesFundingGroup;
    AbstractGroup equipmentGroup;
    AbstractGroup projectDocumentsGroup;
    AbstractGroup projectStatusGroup;

    AbstractGroup voteResultGroup;
    AbstractGroup voteResultCheckGroup;
    AbstractGroup voteResultCommentGroup;

    AbstractGroup expertResultGroup;

    protected void initGroups() {

        idGroup.add(objectValue);

        projectInformationGroup = new AbstractGroup("Информация по проекту");
        baseGroup.add(projectInformationGroup);

        innovationGroup = new AbstractGroup("Инновация");
        baseGroup.add(innovationGroup);

        executiveSummaryGroup = new AbstractGroup("Резюме проекта");
        baseGroup.add(executiveSummaryGroup);

        sourcesFundingGroup = new AbstractGroup("Источники финансирования");
        baseGroup.add(sourcesFundingGroup);

        equipmentGroup = new AbstractGroup("Оборудование");
        baseGroup.add(equipmentGroup);

        projectDocumentsGroup = new AbstractGroup("Документы");
        baseGroup.add(projectDocumentsGroup);

        projectStatusGroup = new AbstractGroup("Текущий статус проекта");
        baseGroup.add(projectStatusGroup);


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

        multiLanguageNamed = addAbstractClass("multiLanguageNamed", "Многоязычный объект", baseClass);

        projectType = addStaticClass("projectType", "Тип проекта",
                                     new String[]{"comparable3", "surpasses3", "russianbenchmark3", "certainadvantages3", "significantlyoutperforms3", "nobenchmarks3"},
                                     new String[]{"сопоставим с существующими российскими аналогами или уступает им", "превосходит российские аналоги, но уступает лучшим зарубежным аналогам",
                                                  "Является российским аналогом востребованного зарубежного продукта/технологии", "Обладает отдельными преимуществами над лучшими мировыми аналогами, но в целом сопоставим с ними",
                                                  "Существенно превосходит все существующие мировые аналоги", "Не имеет аналогов, удовлетворяет ранее не удовлетворенную потребность и создает новый рынок"});

        ownerType = addStaticClass("ownerType", "Тип правообладателя",
                                               new String[]{"employee", "participant", "thirdparty"},
                                               new String[]{"Работником организации", "Участником организации", "Третьим лицом"});  

        patent = addConcreteClass("patent", "Патент", baseClass);
        academic = addConcreteClass("academic", "Научные кадры", baseClass);        
        nonRussianSpecialist = addConcreteClass("nonRussianSpecialist", "Иностранный специалист", baseClass);

        project = addConcreteClass("project", "Проект", multiLanguageNamed, transaction);
        expert = addConcreteClass("expert", "Эксперт", customUser);
        cluster = addConcreteClass("cluster", "Кластер", multiLanguageNamed);

        claimer = addConcreteClass("claimer", "Заявитель", multiLanguageNamed, emailObject);
        claimer.dialogReadOnly = false;

        documentTemplate = addConcreteClass("documentTemplate", "Шаблон документов", baseClass.named);

        documentAbstract = addConcreteClass("documentAbstract", "Документ (абстр.)", baseClass);

        documentTemplateDetail = addConcreteClass("documentTemplateDetail", "Документ (прототип)", documentAbstract);

        document = addConcreteClass("document", "Документ", documentAbstract);

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

    LP nameNative, nameForeign;

    LP projectVote, nameNativeProjectVote, nameForeignProjectVote;
    LP clusterExpert, nameNativeClusterExpert;
    LP clusterProject, nameNativeClusterProject, nameForeignClusterProject;
    LP clusterVote, nameNativeClusterVote;
    LP clusterProjectVote, equalsClusterProjectVote;
    LP claimerProject, nameNativeClaimerProject, nameForeignClaimerProject;
    LP emailDocuments;

    LP emailToExpert;

    LP claimerVote, nameNativeClaimerVote, nameForeignClaimerVote;
    LP nameNativeAblateClaimer;
    LP nameNativeDativusClaimer;
    LP nameAblateClaimer;
    LP nameDativusClaimer;
    LP nameNativeClaimer;
    LP nameAblateClaimerProject;
    LP nameDativusClaimerProject;
    LP nameAblateClaimerVote;
    LP nameDativusClaimerVote;

    LP documentTemplateDocumentTemplateDetail;

    LP projectDocument, nameNativeProjectDocument;
    LP fileDocument;
    LP loadFileDocument;
    LP openFileDocument;

    LP inExpertVote, oldExpertVote, inNewExpertVote, inOldExpertVote;
    LP dateStartVote, dateEndVote;
    LP aggrDateEndVote;

    LP openedVote;
    LP closedVote;
    LP voteInProgressProject;
    LP requiredPeriod;
    LP requiredQuantity;
    LP limitExperts;

    LP dateExpertVote;
    LP voteResultExpertVote, nameVoteResultExpertVote;
    LP voteResultNewExpertVote;
    LP inProjectExpert;
    LP voteProjectExpert;
    LP voteResultProjectExpert;
    LP doneProjectExpert;
    LP doneExpertVote, doneNewExpertVote, doneOldExpertVote;
    LP refusedExpertVote;
    LP connectedExpertVote;
    LP expertVoteConnected;
    LP inClusterExpertVote, inClusterNewExpertVote;
    LP innovativeExpertVote, innovativeNewExpertVote;
    LP innovativeCommentExpertVote;
    LP foreignExpertVote, foreignNewExpertVote;
    LP competentExpertVote;
    LP completeExpertVote;
    LP completeCommentExpertVote;

    LP quantityRepliedVote;
    LP quantityDoneVote;
    LP quantityDoneOldVote;
    LP quantityRefusedVote;
    LP quantityConnectedVote;
    LP quantityInClusterVote;
    LP quantityInnovativeVote;
    LP quantityForeignVote;
    LP acceptedInClusterVote;
    LP acceptedInnovativeVote;
    LP acceptedForeignVote;

    LP acceptedVote;
    LP succeededVote, succeededClusterVote;
    LP doneExpertVoteDateFromDateTo;
    LP quantityDoneExpertDateFromDateTo;
    LP voteSucceededProject;
    LP noCurrentVoteProject;
    LP voteValuedProject;
    LP acceptedProject;
    LP voteRejectedProject;
    LP needExtraVoteProject;

    LP emailLetterExpertVoteEA, emailLetterExpertVote;
    LP allowedEmailLetterExpertVote;
    LP emailStartVoteEA, emailStartHeaderVote, emailStartVote;
    LP emailProtocolVoteEA, emailProtocolHeaderVote, emailProtocolVote;
    LP emailClosedVoteEA, emailClosedHeaderVote, emailClosedVote;
    LP emailAuthExpertEA, emailAuthExpert;
    LP authExpertSubjectLanguage, letterExpertSubjectLanguage;

    LP generateDocumentsProjectDocumentType;
    LP generateVoteProject, hideGenerateVoteProject;

    LP expertLogin;

    LP statusProject, nameStatusProject;
    LP statusProjectVote, nameStatusProjectVote;

    LP projectSucceededClaimer;

    LP quantityTotalExpert;
    LP quantityDoneExpert;
    LP percentDoneExpert;
    LP percentInClusterExpert;
    LP percentInnovativeExpert;
    LP percentForeignExpert;

    LP prevDateStartVote, prevDateVote;
    LP dateProjectVote;
    LP numberNewExpertVote;
    LP numberOldExpertVote;

    LP languageExpert;
    LP nameLanguageExpert;
    LP languageDocument;
    LP nameLanguageDocument;
    LP typeDocument;
    LP nameTypeDocument;
    LP postfixDocument;
    LP hidePostfixDocument;

    LP localeLanguage;

    LP quantityMinLanguageDocumentType;
    LP quantityMaxLanguageDocumentType;
    LP quantityProjectLanguageDocumentType;
    LP translateLanguageDocumentType;
    LP notEnoughProject;
    LP autoGenerateProject;

    LP nameDocument;

    LP nameNativeProject;
    LP nameForeignProject;
    LP nameNativeManagerProject;
    LP nameForeignManagerProject;
    LP nativeProblemProject;
    LP foreignProblemProject;
    LP nativeInnovativeProject;
    LP foreignInnovativeProject;
    LP projectTypeProject;
    LP nameProjectTypeProject;
    LP nativeSubstantiationProjectType;
    LP foreignSubstantiationProjectType;
    LP nativeSubstantiationClusterProject;
    LP foreignSubstantiationClusterProject;
    LP fileNativeSummaryProject;
    LP loadFileNativeSummaryProject;
    LP openFileNativeSummaryProject;
    LP fileForeignSummaryProject;
    LP loadFileForeignSummaryProject;
    LP openFileForeignSummaryProject;
    LP fileRoadMapProject;
    LP loadFileRoadMapProject;
    LP openFileRoadMapProject;
    LP fileNativeTechnicalDescriptionProject;
    LP loadFileNativeTechnicalDescriptionProject;
    LP openFileNativeTechnicalDescriptionProject;
    LP fileForeignTechnicalDescriptionProject;
    LP loadFileForeignTechnicalDescriptionProject;
    LP openFileForeignTechnicalDescriptionProject;

    LP isReturnInvestmentsProject;
    LP nameReturnInvestorProject;
    LP amountReturnFundsProject;
    LP isNonReturnInvestmentsProject;
    LP nameNonReturnInvestorProject;
    LP amountNonReturnFundsProject;
    LP isCapitalInvestmentProject;
    LP isPropertyInvestmentProject;
    LP isGrantsProject;
    LP isOtherNonReturnInvestmentsProject;
    LP commentOtherNonReturnInvestmentsProject;
    LP isOwnFundsProject;
    LP amountOwnFundsProject;
    LP isPlanningSearchSourceProject;
    LP amountFundsProject;    
    LP isOtherSoursesProject;
    LP commentOtherSoursesProject;
    LP hideNameReturnInvestorProject;
    LP hideAmountReturnFundsProject;
    LP hideNameNonReturnInvestorProject;
    LP hideAmountNonReturnFundsProject;
    LP hideCommentOtherNonReturnInvestmentsProject;
    LP hideAmountOwnFundsProject;
    LP hideAmountFundsProject;
    LP hideCommentOtherSoursesProject;
    LP hideIsCapitalInvestmentProject;
    LP hideIsPropertyInvestmentProject;
    LP hideIsGrantsProject;
    LP hideIsOtherNonReturnInvestmentsProject;

    LP isOwnedEquipmentProject;
    LP isAvailableEquipmentProject;
    LP isTransferEquipmentProject;
    LP descriptionTransferEquipmentProject;
    LP ownerEquipmentProject;
    LP isPlanningEquipmentProject;
    LP specificationEquipmentProject;
    LP isSeekEquipmentProject;
    LP descriptionEquipmentProject;
    LP isOtherEquipmentProject;
    LP commentEquipmentProject;
    LP hideDescriptionTransferEquipmentProject;
    LP hideOwnerEquipmentProject;
    LP hideSpecificationEquipmentProject;
    LP hideDescriptionEquipmentProject;
    LP hideCommentEquipmentProject;

    LP projectPatent;
    LP nativeTypePatent;
    LP foreignTypePatent;
    LP nativeNumberPatent;
    LP foreignNumberPatent;
    LP priorityDatePatent;
    LP isOwned;
    LP ownerPatent;
    LP ownerTypePatent;
    LP nameOwnerTypePatent;
    LP fileIntentionOwnerPatent;
    LP loadFileIntentionOwnerPatent;
    LP openFileIntentionOwnerPatent;
    LP isValuated;   
    LP valuatorPatent;
    LP fileActValuationPatent;
    LP loadFileActValuationPatent;
    LP openFileActValuationPatent;
    LP hideOwnerPatent;
    LP hideFileIntentionOwnerPatent;
    LP hideLoadFileIntentionOwnerPatent;
    LP hideOpenFileIntentionOwnerPatent;
    LP hideValuatorPatent;
    LP hideFileActValuationPatent;
    LP hideLoadFileActValuationPatent;
    LP hideOpenFileActValuationPatent;
    LP hideNameOwnerTypePatent;

    LP projectAcademic;
    LP fullNameAcademic;
    LP institutionAcademic;
    LP titleAcademic;
    LP fileDocumentConfirmingAcademic;
    LP loadFileDocumentConfirmingAcademic;
    LP openFileDocumentConfirmingAcademic;
    LP fileDocumentEmploymentAcademic;
    LP loadFileDocumentEmploymentAcademic;
    LP openFileDocumentEmploymentAcademic;

    LP projectNonRussianSpecialist;
    LP fullNameNonRussianSpecialist;
    LP organizationNonRussianSpecialist;
    LP titleNonRussianSpecialist;
    LP fileNativeResumeNonRussianSpecialist;
    LP loadFileForeignResumeNonRussianSpecialist;
    LP openFileForeignResumeNonRussianSpecialist;
    LP fileForeignResumeNonRussianSpecialist;
    LP loadFileNativeResumeNonRussianSpecialist;
    LP openFileNativeResumeNonRussianSpecialist;
    LP filePassportNonRussianSpecialist;
    LP loadFilePassportNonRussianSpecialist;
    LP openFilePassportNonRussianSpecialist;
    LP fileStatementNonRussianSpecialist;
    LP loadFileStatementNonRussianSpecialist;
    LP openFileStatementNonRussianSpecialist;

    LP isForeignExpert;
    LP localeExpert;
    LP disableExpert;

    LP addProject, editProject;

    protected void initProperties() {

        nameNative = addDProp(baseGroup, "nameNative", "Имя", InsensitiveStringClass.get(2000), multiLanguageNamed);
        nameNative.setMinimumWidth(10); nameNative.setPreferredWidth(50);
        nameForeign = addDProp(baseGroup, "nameForeign", "Имя (иностр.)", InsensitiveStringClass.get(2000), multiLanguageNamed);
        nameForeign.setMinimumWidth(10); nameForeign.setPreferredWidth(50);

        nameNativeDativusClaimer = addDProp(baseGroup, "nameNativeDativusClaimer", "Наименование (кому)", InsensitiveStringClass.get(2000), claimer);
        nameNativeDativusClaimer.setMinimumWidth(10); nameNativeDativusClaimer.setPreferredWidth(50);

        nameNativeAblateClaimer = addDProp(baseGroup, "nameNativeAblateClaimer", "Наименование (кем)", InsensitiveStringClass.get(2000), claimer);
        nameNativeAblateClaimer.setMinimumWidth(10); nameNativeAblateClaimer.setPreferredWidth(50);

        nameDativusClaimer = addSUProp("nameDativusClaimer", "Наименование (кому)", Union.OVERRIDE, nameNative,  nameNativeDativusClaimer);
        nameDativusClaimer.setMinimumWidth(10); nameDativusClaimer.setPreferredWidth(50);

        nameAblateClaimer = addSUProp("nameAblateClaimer", "Наименование (кем)", Union.OVERRIDE, nameNative,  nameNativeAblateClaimer);
        nameAblateClaimer.setMinimumWidth(10); nameAblateClaimer.setPreferredWidth(50);

        baseGroup.add(email.property); // сделано, чтобы email был не самой первой колонкой в диалогах

        LP percent = addSFProp("(prm1*100/prm2)", DoubleClass.instance, 2);

        // глобальные свойства
        requiredPeriod = addDProp(baseGroup, "votePeriod", "Срок заседания", IntegerClass.instance);
        requiredQuantity = addDProp(baseGroup, "voteRequiredQuantity", "Кол-во экспертов", IntegerClass.instance);
        limitExperts = addDProp(baseGroup, "limitExperts", "Кол-во прогол. экспертов", IntegerClass.instance);

        projectVote = addDProp(idGroup, "projectVote", "Проект (ИД)", project, vote);
        setNotNull(projectVote);

        nameNativeProjectVote = addJProp(baseGroup, "nameNativeProjectVote", "Проект", nameNative, projectVote, 1);
        nameForeignProjectVote = addJProp(baseGroup, "nameForeignProjectVote", "Проект (иностр.)", nameForeign, projectVote, 1);

        dateProjectVote = addJProp("dateProjectVote", "Дата проекта", date, projectVote, 1);

        clusterExpert = addDProp(idGroup, "clusterExpert", "Кластер (ИД)", cluster, expert);
        nameNativeClusterExpert = addJProp(baseGroup, "nameNativeClusterExpert", "Кластер", nameNative, clusterExpert, 1);

        // project
        claimerProject = addDProp(idGroup, "claimerProject", "Заявитель (ИД)", claimer, project);
        nameNativeClaimerProject = addJProp(projectInformationGroup, "nameNativeClaimerProject", "Заявитель", nameNative, claimerProject, 1);
        nameForeignClaimerProject = addJProp(projectInformationGroup, "nameForeignClaimerProject", "Заявитель (иностр.)", nameForeign, claimerProject, 1);
        nameDativusClaimerProject = addJProp("nameDativusClaimerProject", "Заявитель", nameDativusClaimer, claimerProject, 1);
        nameAblateClaimerProject = addJProp("nameAblateClaimerProject", "Заявитель", nameAblateClaimer, claimerProject, 1);

        nameNativeProject = addJProp(projectInformationGroup, "nameNativeProject", "Название проекта", and1, nameNative, 1, is(project), 1);
        nameNativeProject.setMinimumWidth(10); nameNativeProject.setPreferredWidth(50);
        nameForeignProject = addJProp(projectInformationGroup, "nameForeignProject", "Name of the project", and1,  nameForeign, 1, is(project), 1);
        nameForeignProject.setMinimumWidth(10); nameForeignProject.setPreferredWidth(50);

        nameNativeManagerProject = addDProp(projectInformationGroup, "nameNativeManagerProject", "ФИО руководителя проекта", InsensitiveStringClass.get(2000), project);
        nameNativeManagerProject.setMinimumWidth(10); nameNativeManagerProject.setPreferredWidth(50);
        nameForeignManagerProject = addDProp(projectInformationGroup, "nameForeignManagerProject", "Full name project manager", InsensitiveStringClass.get(2000), project);
        nameForeignManagerProject.setMinimumWidth(10); nameForeignManagerProject.setPreferredWidth(50);

        nativeProblemProject = addDProp(innovationGroup, "nativeProblemProject", "Проблема, на решение которой направлен проект", InsensitiveStringClass.get(2000), project);
        nativeProblemProject.setMinimumWidth(10); nativeProblemProject.setPreferredWidth(50);
        foreignProblemProject = addDProp(innovationGroup, "foreignProblemProject", "The problem that the project will solve", InsensitiveStringClass.get(2000), project);
        foreignProblemProject.setMinimumWidth(10); foreignProblemProject.setPreferredWidth(50);

        nativeInnovativeProject = addDProp(innovationGroup, "nativeInnovativeProject", "Суть инновации", InsensitiveStringClass.get(2000), project);
        nativeInnovativeProject.setMinimumWidth(10); nativeInnovativeProject.setPreferredWidth(50);
        foreignInnovativeProject = addDProp(innovationGroup, "foreignInnovativeProject", "Description of the innovation", InsensitiveStringClass.get(2000), project);
        foreignInnovativeProject.setMinimumWidth(10); foreignInnovativeProject.setPreferredWidth(50);

        projectTypeProject = addDProp(idGroup, "projectTypeProject", "Тип проекта (ИД)", projectType, project);
        nameProjectTypeProject = addJProp(innovationGroup, "nameProjectTypeProject", "Тип проекта", name, projectTypeProject, 1);
        nativeSubstantiationProjectType = addDProp(innovationGroup, "nativeSubstantiationProjectType", "Обоснование выбора", InsensitiveStringClass.get(2000), project);
        nativeSubstantiationProjectType.setMinimumWidth(10); nativeSubstantiationProjectType.setPreferredWidth(50);
        foreignSubstantiationProjectType = addDProp(innovationGroup, "foreignSubstantiationProjectType", "Description of choice", InsensitiveStringClass.get(2000), project);
        foreignSubstantiationProjectType.setMinimumWidth(10); foreignSubstantiationProjectType.setPreferredWidth(50);

        clusterProject = addDProp(idGroup, "clusterProject", "Кластер (ИД)", cluster, project);
        nameNativeClusterProject = addJProp(innovationGroup, "nameNativeClusterProject", "Кластер", nameNative, clusterProject, 1);
        nameForeignClusterProject = addJProp(innovationGroup, "nameForeignClusterProject", "Кластер (иностр.)", nameForeign, clusterProject, 1);
        nativeSubstantiationClusterProject = addDProp(innovationGroup, "nativeSubstantiationClusterProject", "Обоснование выбора", InsensitiveStringClass.get(2000), project);
        nativeSubstantiationClusterProject.setMinimumWidth(10); nativeSubstantiationClusterProject.setPreferredWidth(50);
        foreignSubstantiationClusterProject = addDProp(innovationGroup, "foreignSubstantiationClusterProject", "Description of choice", InsensitiveStringClass.get(2000), project);
        foreignSubstantiationClusterProject.setMinimumWidth(10); foreignSubstantiationClusterProject.setPreferredWidth(50);

        fileNativeSummaryProject = addDProp("fileNativeSummaryProject", "Файл резюме проекта", PDFClass.instance, project);
        loadFileNativeSummaryProject = addLFAProp(executiveSummaryGroup, "Загрузить файл резюме проекта", fileNativeSummaryProject);
        openFileNativeSummaryProject = addOFAProp(executiveSummaryGroup, "Открыть файл резюме проекта", fileNativeSummaryProject);

        fileForeignSummaryProject = addDProp("fileForeignSummaryProject", "Файл резюме проекта (иностр.)", PDFClass.instance, project);
        loadFileForeignSummaryProject = addLFAProp(executiveSummaryGroup, "Загрузить файл резюме проекта (иностр.)", fileForeignSummaryProject);
        openFileForeignSummaryProject = addOFAProp(executiveSummaryGroup, "Открыть файл резюме проекта (иностр.)", fileForeignSummaryProject);

        // источники финансирования
        isReturnInvestmentsProject = addDProp(sourcesFundingGroup, "isReturnInvestmentsProject", "Средства третьих лиц, привлекаемые на возвратной основе (заемные средства и т.п.)", LogicalClass.instance, project);
        nameReturnInvestorProject = addDProp(sourcesFundingGroup, "nameReturnInvestorProject", "Третьи лица для возврата средств", InsensitiveStringClass.get(2000), project);
        nameReturnInvestorProject.setMinimumWidth(10); nameReturnInvestorProject.setPreferredWidth(50);
        amountReturnFundsProject = addDProp(sourcesFundingGroup, "amountReturnFundsProject", "Объем средств на возвратной основе (тыс. руб.)", DoubleClass.instance, project);
        hideNameReturnInvestorProject = addHideCaptionProp(privateGroup, "укажите данных лиц и их контактную информацию", nameReturnInvestorProject, isReturnInvestmentsProject);
        hideAmountReturnFundsProject = addHideCaptionProp(privateGroup, "укажите объем привлекаемых средств (тыс. руб.)", amountReturnFundsProject, isReturnInvestmentsProject);

        isNonReturnInvestmentsProject = addDProp(sourcesFundingGroup, "isNonReturnInvestmentsProject", "Средства третьих лиц, привлекаемые на безвозвратной основе (гранты и т.п.)", LogicalClass.instance, project);

        isCapitalInvestmentProject = addDProp(sourcesFundingGroup, "isCapitalInvestmentProject", "Вклады в уставный капитал", LogicalClass.instance, project);
        isPropertyInvestmentProject = addDProp(sourcesFundingGroup, "isPropertyInvestmentProject", "Вклады в имущество", LogicalClass.instance, project);
        isGrantsProject = addDProp(sourcesFundingGroup, "isGrantsProject", "Гранты", LogicalClass.instance, project);

        isOtherNonReturnInvestmentsProject = addDProp(sourcesFundingGroup, "isOtherNonReturnInvestmentsProject", "Иное", LogicalClass.instance, project);

        nameNonReturnInvestorProject = addDProp(sourcesFundingGroup, "nameNonReturnInvestorProject", "Третьи лица для возврата средств", InsensitiveStringClass.get(2000), project);
        nameNonReturnInvestorProject.setMinimumWidth(10); nameNonReturnInvestorProject.setPreferredWidth(50);
        amountNonReturnFundsProject = addDProp(sourcesFundingGroup, "amountNonReturnFundsProject", "Объем средств на безвозвратной основе (тыс. руб.)", DoubleClass.instance, project);
        hideNameNonReturnInvestorProject = addHideCaptionProp(privateGroup, "укажите данных лиц и их контактную информацию", nameReturnInvestorProject, isNonReturnInvestmentsProject);
        hideAmountNonReturnFundsProject = addHideCaptionProp(privateGroup, "укажите объем привлекаемых средств (тыс. руб.)", amountReturnFundsProject, isNonReturnInvestmentsProject);

        commentOtherNonReturnInvestmentsProject = addDProp(sourcesFundingGroup, "commentOtherNonReturnInvestmentsProject", "Комментарий", InsensitiveStringClass.get(2000), project);
        commentOtherNonReturnInvestmentsProject.setMinimumWidth(10); commentOtherNonReturnInvestmentsProject.setPreferredWidth(50);

        hideIsCapitalInvestmentProject = addHideCaptionProp(privateGroup, "Укажите", isCapitalInvestmentProject, isNonReturnInvestmentsProject);
        hideIsPropertyInvestmentProject = addHideCaptionProp(privateGroup, "Укажите", isPropertyInvestmentProject, isNonReturnInvestmentsProject);
        hideIsGrantsProject = addHideCaptionProp(privateGroup, "Укажите", isGrantsProject, isNonReturnInvestmentsProject);
        hideIsOtherNonReturnInvestmentsProject = addHideCaptionProp(privateGroup, "Укажите", isOtherNonReturnInvestmentsProject, isNonReturnInvestmentsProject);

        hideCommentOtherNonReturnInvestmentsProject = addHideCaptionProp(privateGroup, "Укажите", commentOtherNonReturnInvestmentsProject, isOtherNonReturnInvestmentsProject);

        isOwnFundsProject = addDProp(sourcesFundingGroup, "isOwnFundsProject", "Собственные средства организации", LogicalClass.instance, project);
        amountOwnFundsProject = addDProp(sourcesFundingGroup, "amountOwnFundsProject", "Объем собственных средств (тыс. руб.)", DoubleClass.instance, project);
        hideAmountOwnFundsProject = addHideCaptionProp(privateGroup, "Укажите", amountOwnFundsProject, isOwnFundsProject);

        isPlanningSearchSourceProject = addDProp(sourcesFundingGroup, "isPlanningSearchSourceProject", "Планируется поиск источника финансирования проекта", LogicalClass.instance, project);
        amountFundsProject = addDProp(sourcesFundingGroup, "amountFundsProject", "Требуемый объем средств (тыс. руб.)", DoubleClass.instance, project);
        hideAmountFundsProject = addHideCaptionProp(privateGroup, "Укажите", amountFundsProject, isPlanningSearchSourceProject);

        isOtherSoursesProject = addDProp(sourcesFundingGroup, "isOtherSoursesProject", "Иное", LogicalClass.instance, project);
        commentOtherSoursesProject = addDProp(sourcesFundingGroup, "commentOtherSoursesProject", "Комментарий", InsensitiveStringClass.get(2000), project);
        commentOtherSoursesProject.setMinimumWidth(10); commentOtherSoursesProject.setPreferredWidth(50);
        hideCommentOtherSoursesProject = addHideCaptionProp(privateGroup, "Укажите", commentOtherSoursesProject, isOtherSoursesProject);

        // оборудование
        isOwnedEquipmentProject = addDProp(equipmentGroup, "isOwnedEquipmentProject", "Оборудование имеется в собственности и/или в пользовании Вашей организации", LogicalClass.instance, project);

        isAvailableEquipmentProject = addDProp(equipmentGroup, "isAvailableEquipmentProject", "Оборудование имеется в открытом доступе на рынке, и Ваша организация планирует приобрести его в собственность и/или в пользование", LogicalClass.instance, project);

        isTransferEquipmentProject = addDProp(equipmentGroup, "isTransferEquipmentProject", "Оборудование отсутствует в открытом доступе на рынке, но достигнута договоренность с собственником оборудования о передаче данного оборудования в собственность и/или в пользование для реализации проекта", LogicalClass.instance, project);
        descriptionTransferEquipmentProject = addDProp(equipmentGroup, "descriptionTransferEquipmentProject", "Опишите данное оборудование", InsensitiveStringClass.get(2000), project);
        descriptionTransferEquipmentProject.setMinimumWidth(10); descriptionTransferEquipmentProject.setPreferredWidth(50);
        ownerEquipmentProject = addDProp(equipmentGroup, "ownerEquipmentProject", "Укажите собственника оборудования и его контактную информацию", InsensitiveStringClass.get(2000), project);
        ownerEquipmentProject.setMinimumWidth(10); ownerEquipmentProject.setPreferredWidth(50);
        hideDescriptionTransferEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", descriptionTransferEquipmentProject, isTransferEquipmentProject);
        hideOwnerEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", ownerEquipmentProject, isTransferEquipmentProject);

        isPlanningEquipmentProject = addDProp(equipmentGroup, "isPlanningEquipmentProject", "Ваша организация планирует использовать для реализации проекта оборудование, которое имеется в собственности или в пользовании Фонда «Сколково» (учрежденных им юридических лиц)", LogicalClass.instance, project);
        specificationEquipmentProject = addDProp(equipmentGroup, "specificationEquipmentProject", "Укажите данное оборудование", InsensitiveStringClass.get(2000), project);
        specificationEquipmentProject.setMinimumWidth(10); specificationEquipmentProject.setPreferredWidth(50);
        hideSpecificationEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", specificationEquipmentProject, isPlanningEquipmentProject);

        isSeekEquipmentProject = addDProp(equipmentGroup, "isSeekEquipmentProject", "Оборудование имеется на рынке, но Ваша организация не имеет возможности приобрести его в собственность или в пользование и ищет возможность получить доступ к такому оборудованию", LogicalClass.instance, project);
        descriptionEquipmentProject = addDProp(equipmentGroup, "descriptionEquipmentProject", "Опишите данное оборудование", InsensitiveStringClass.get(2000), project);
        descriptionEquipmentProject.setMinimumWidth(10); descriptionEquipmentProject.setPreferredWidth(50);
        hideDescriptionEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", descriptionEquipmentProject, isSeekEquipmentProject);

        isOtherEquipmentProject = addDProp(equipmentGroup, "isOtherEquipmentProject", "Иное", LogicalClass.instance, project);
        commentEquipmentProject = addDProp(equipmentGroup, "commentEquipmentProject", "Комментарий", InsensitiveStringClass.get(2000), project);
        commentEquipmentProject.setMinimumWidth(10); commentEquipmentProject.setPreferredWidth(50);
        hideCommentEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", commentEquipmentProject, isOtherEquipmentProject);

        // документы
        fileRoadMapProject = addDProp("fileRoadMapProject", "Файл дорожной карты", PDFClass.instance, project);
        loadFileRoadMapProject = addLFAProp(projectDocumentsGroup, "Загрузить файл дорожной карты", fileRoadMapProject);
        openFileRoadMapProject = addOFAProp(projectDocumentsGroup, "Открыть файл дорожной карты", fileRoadMapProject);

        fileNativeTechnicalDescriptionProject = addDProp("fileNativeTechnicalDescriptionProject", "Файл технического описания", PDFClass.instance, project);
        loadFileNativeTechnicalDescriptionProject = addLFAProp(projectDocumentsGroup, "Загрузить файл технического описания", fileNativeTechnicalDescriptionProject);
        openFileNativeTechnicalDescriptionProject = addOFAProp(projectDocumentsGroup, "Открыть файл технического описания", fileNativeTechnicalDescriptionProject);

        fileForeignTechnicalDescriptionProject = addDProp("fileForeignTechnicalDescriptionProject", "Файл технического описания (иностр.)", PDFClass.instance, project);
        loadFileForeignTechnicalDescriptionProject = addLFAProp(projectDocumentsGroup, "Загрузить файл технического описания (иностр.)", fileForeignTechnicalDescriptionProject);
        openFileForeignTechnicalDescriptionProject = addOFAProp(projectDocumentsGroup, "Открыть файл технического описания (иностр.)", fileForeignTechnicalDescriptionProject);

        // патенты
        projectPatent = addDProp(idGroup, "projectPatent", "Проект патента", project, patent);

        nativeTypePatent = addDProp(baseGroup, "nativeTypePatent", "Тип заявки/патента", InsensitiveStringClass.get(2000), patent);
        nativeTypePatent.setMinimumWidth(10); nativeTypePatent.setPreferredWidth(50);
        foreignTypePatent = addDProp(baseGroup, "foreignTypePatent", "Type of application/patent", InsensitiveStringClass.get(2000), patent);
        foreignTypePatent.setMinimumWidth(10); foreignTypePatent.setPreferredWidth(50);
        nativeNumberPatent = addDProp(baseGroup, "nativeNumberPatent", "Номер", InsensitiveStringClass.get(2000), patent);
        nativeNumberPatent.setMinimumWidth(10); nativeNumberPatent.setPreferredWidth(50);
        foreignNumberPatent = addDProp(baseGroup, "foreignNumberPatent", "Reference number", InsensitiveStringClass.get(2000), patent);
        foreignNumberPatent.setMinimumWidth(10); foreignNumberPatent.setPreferredWidth(50);
        priorityDatePatent = addDProp(baseGroup, "priorityDatePatent", "Дата приоритета", DateClass.instance, patent);

        isOwned = addDProp(baseGroup, "isOwned", "Организация не обладает исключительными правами на указанные результаты интеллектуальной деятельности", LogicalClass.instance, patent);
        ownerPatent = addDProp(baseGroup, "ownerPatent", "Укажите правообладателя и его контактную информацию", InsensitiveStringClass.get(2000), patent);
        ownerPatent.setMinimumWidth(10); ownerPatent.setPreferredWidth(50);
        ownerTypePatent = addDProp(idGroup, "ownerTypePatent", "Кем является правообладатель (ИД)", ownerType, patent);
        nameOwnerTypePatent = addJProp(baseGroup, "nameOwnerTypePatent", "Кем является правообладатель", name, ownerTypePatent, 1);
        fileIntentionOwnerPatent = addDProp("fileIntentionOwnerPatent", "Файл документа о передаче права", PDFClass.instance, patent);
        loadFileIntentionOwnerPatent = addLFAProp(baseGroup, "Загрузить файл документа о передаче права", fileIntentionOwnerPatent);
        openFileIntentionOwnerPatent = addOFAProp(baseGroup, "Открыть файл документа о передаче права", fileIntentionOwnerPatent);

        hideOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", ownerPatent, isOwned);
        hideNameOwnerTypePatent = addHideCaptionProp(privateGroup, "Укажите", nameOwnerTypePatent, isOwned);
        hideFileIntentionOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", fileIntentionOwnerPatent, isOwned);
        hideLoadFileIntentionOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", loadFileIntentionOwnerPatent, isOwned);
        hideOpenFileIntentionOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", openFileIntentionOwnerPatent, isOwned);

        isValuated = addDProp(baseGroup, "isValuated", "Проводилась ли оценка указанных результатов интеллектальной деятельности", LogicalClass.instance, patent);
        valuatorPatent = addDProp(baseGroup, "valuatorPatent", "Укажите оценщика и его контактную информацию", InsensitiveStringClass.get(2000), patent);
        valuatorPatent.setMinimumWidth(10); valuatorPatent.setPreferredWidth(50);
        fileActValuationPatent = addDProp("fileActValuationPatent", "Файл акта оценки", PDFClass.instance, patent);
        loadFileActValuationPatent = addLFAProp(baseGroup, "Загрузить файл акта оценки", fileActValuationPatent);
        openFileActValuationPatent = addOFAProp(baseGroup, "Открыть файл акта оценки", fileActValuationPatent);
        hideValuatorPatent = addHideCaptionProp(privateGroup, "Укажите", valuatorPatent, isValuated);
        hideFileActValuationPatent = addHideCaptionProp(privateGroup, "Укажите", fileActValuationPatent, isValuated);
        hideLoadFileActValuationPatent = addHideCaptionProp(privateGroup, "Укажите", loadFileActValuationPatent, isValuated);
        hideOpenFileActValuationPatent = addHideCaptionProp(privateGroup, "Укажите", openFileActValuationPatent, isValuated);

        // учёные
        projectAcademic = addDProp(idGroup, "projectAcademic", "Проект ученого", project, academic);

        fullNameAcademic = addDProp(baseGroup, "fullNameAcademic", "ФИО", InsensitiveStringClass.get(2000), academic);
        fullNameAcademic.setMinimumWidth(10); fullNameAcademic.setPreferredWidth(50);
        institutionAcademic = addDProp(baseGroup, "institutionAcademic", "Учреждение, в котором данный специалист осуществляет научную и (или) преподавательскую деятельность", InsensitiveStringClass.get(2000), academic);
        institutionAcademic.setMinimumWidth(10); institutionAcademic.setPreferredWidth(50);
        titleAcademic = addDProp(baseGroup, "titleAcademic", "Ученая степень, звание, должность и др.", InsensitiveStringClass.get(2000), academic);
        titleAcademic.setMinimumWidth(10); titleAcademic.setPreferredWidth(50);

        fileDocumentConfirmingAcademic = addDProp("fileDocumentConfirmingAcademic", "Файл трудового договора", PDFClass.instance, academic);
        loadFileDocumentConfirmingAcademic = addLFAProp(baseGroup, "Загрузить файл трудового договора", fileDocumentConfirmingAcademic);
        openFileDocumentConfirmingAcademic = addOFAProp(baseGroup, "Открыть файл трудового договора", fileDocumentConfirmingAcademic);

        fileDocumentEmploymentAcademic = addDProp("fileDocumentEmploymentAcademic", "Файл заявления специалиста", PDFClass.instance, academic);
        loadFileDocumentEmploymentAcademic = addLFAProp(baseGroup, "Загрузить файл заявления", fileDocumentEmploymentAcademic);
        openFileDocumentEmploymentAcademic = addOFAProp(baseGroup, "Открыть файл заявления", fileDocumentEmploymentAcademic);

        // иностранные специалисты
        projectNonRussianSpecialist = addDProp(idGroup, "projectNonRussianSpecialist", "Проект иностранного специалиста", project, nonRussianSpecialist);

        fullNameNonRussianSpecialist = addDProp(baseGroup, "fullNameNonRussianSpecialist", "ФИО", InsensitiveStringClass.get(2000), nonRussianSpecialist);
        fullNameNonRussianSpecialist.setMinimumWidth(10); fullNameNonRussianSpecialist.setPreferredWidth(50);
        organizationNonRussianSpecialist = addDProp(baseGroup, "organizationNonRussianSpecialist", "Место работы", InsensitiveStringClass.get(2000), nonRussianSpecialist);
        organizationNonRussianSpecialist.setMinimumWidth(10); organizationNonRussianSpecialist.setPreferredWidth(50);
        titleNonRussianSpecialist = addDProp(baseGroup, "titleNonRussianSpecialist", "Должность, если есть - ученая степень, звание и др.", InsensitiveStringClass.get(2000), nonRussianSpecialist);
        titleNonRussianSpecialist.setMinimumWidth(10); titleNonRussianSpecialist.setPreferredWidth(50);

        fileForeignResumeNonRussianSpecialist = addDProp("fileForeignResumeNonRussianSpecialist", "File Curriculum Vitae", PDFClass.instance, nonRussianSpecialist);
        loadFileForeignResumeNonRussianSpecialist = addLFAProp(baseGroup, "Load file Curriculum Vitae", fileForeignResumeNonRussianSpecialist);
        openFileForeignResumeNonRussianSpecialist = addOFAProp(baseGroup, "Open file Curriculum Vitae", fileForeignResumeNonRussianSpecialist);

        fileNativeResumeNonRussianSpecialist = addDProp("fileNativeResumeNonRussianSpecialist", "Файл резюме специалиста", PDFClass.instance, nonRussianSpecialist);
        loadFileNativeResumeNonRussianSpecialist = addLFAProp(baseGroup, "Загрузить файл резюме", fileNativeResumeNonRussianSpecialist);
        openFileNativeResumeNonRussianSpecialist = addOFAProp(baseGroup, "Открыть файл резюме", fileNativeResumeNonRussianSpecialist);

        filePassportNonRussianSpecialist = addDProp("filePassportNonRussianSpecialist", "Файл паспорта", PDFClass.instance, nonRussianSpecialist);
        loadFilePassportNonRussianSpecialist = addLFAProp(baseGroup, "Загрузить файл паспорта", filePassportNonRussianSpecialist);
        openFilePassportNonRussianSpecialist = addOFAProp(baseGroup, "Открыть файл паспорта", filePassportNonRussianSpecialist);

        fileStatementNonRussianSpecialist = addDProp("fileStatementNonRussianSpecialist", "Файл заявления", PDFClass.instance, nonRussianSpecialist);
        loadFileStatementNonRussianSpecialist = addLFAProp(baseGroup, "Загрузить файл заявления", fileStatementNonRussianSpecialist);
        openFileStatementNonRussianSpecialist = addOFAProp(baseGroup, "Открыть файл заявления", fileStatementNonRussianSpecialist);


        clusterVote = addDCProp(idGroup, "clusterVote", "Кластер (ИД)", true, clusterProject, true, projectVote, 1);
        clusterProjectVote = addJProp(idGroup, "clusterProjectVote", "Кластер проекта (ИД)", clusterProject, projectVote, 1);
        equalsClusterProjectVote = addJProp(baseGroup, true, "equalsClusterProjectVote", "Тек. кластер", equals2, clusterVote, 1, clusterProjectVote, 1);
        nameNativeClusterVote = addJProp(baseGroup, "nameNativeClusterVote", "Кластер", nameNative, clusterVote, 1);

        claimerVote = addJProp(idGroup, "claimerVote", "Заявитель (ИД)", claimerProject, projectVote, 1);
        nameNativeClaimerVote = addJProp(baseGroup, "nameNativeClaimerVote", "Заявитель", nameNative, claimerVote, 1);
        nameForeignClaimerVote = addJProp(baseGroup, "nameForeignClaimerVote", "Заявитель (иностр.)", nameForeign, claimerVote, 1);
        nameDativusClaimerVote = addJProp(baseGroup, "nameDativusClaimerVote", "Заявитель", nameDativusClaimer, claimerVote, 1);
        nameAblateClaimerVote = addJProp(baseGroup, "nameAblateClaimerVote", "Заявитель", nameAblateClaimer, claimerVote, 1);

        documentTemplateDocumentTemplateDetail = addDProp(idGroup, "documentTemplateDocumentTemplateDetail", "Шаблон (ИД)", documentTemplate, documentTemplateDetail);

        projectDocument = addDProp(idGroup, "projectDocument", "Проект (ИД)", project, document);
        nameNativeProjectDocument = addJProp(baseGroup, "nameNativeProjectDocument", "Проект", nameNative, projectDocument, 1);

        quantityMinLanguageDocumentType = addDProp(baseGroup, "quantityMinLanguageDocumentType", "Мин. док.", IntegerClass.instance, language, documentType);
        quantityMaxLanguageDocumentType = addDProp(baseGroup, "quantityMaxLanguageDocumentType", "Макс. док.", IntegerClass.instance, language, documentType);
        LP singleLanguageDocumentType = addJProp("Один док.", equals2, quantityMaxLanguageDocumentType, 1, 2, addCProp(IntegerClass.instance, 1));
        LP multipleLanguageDocumentType = addJProp(andNot1, addCProp(LogicalClass.instance, true, language, documentType), 1, 2, singleLanguageDocumentType, 1, 2);
        translateLanguageDocumentType = addDProp(baseGroup, "translateLanguageDocumentType", "Перевод", StringClass.get(50), language, documentType);

        languageExpert = addDProp(idGroup, "languageExpert", "Язык (ИД)", language, expert);
        nameLanguageExpert = addJProp(baseGroup, "nameLanguageExpert", "Язык", name, languageExpert, 1);
        languageDocument = addDProp(idGroup, "languageDocument", "Язык (ИД)", language, documentAbstract);
        nameLanguageDocument = addJProp(baseGroup, "nameLanguageDocument", "Язык", name, languageDocument, 1);
        typeDocument = addDProp(idGroup, "typeDocument", "Тип (ИД)", documentType, documentAbstract);
        nameTypeDocument = addJProp(baseGroup, "nameTypeDocument", "Тип", name, typeDocument, 1);

        localeLanguage = addDProp(baseGroup, "localeLanguage", "Locale", StringClass.get(5), language);
        authExpertSubjectLanguage = addDProp(baseGroup, "authExpertSubjectLanguage", "Заголовок аутентификации эксперта", StringClass.get(100), language);
        letterExpertSubjectLanguage = addDProp(baseGroup, "letterExpertSubjectLanguage", "Заголовок письма о заседании", StringClass.get(100), language);

        LP multipleDocument = addJProp(multipleLanguageDocumentType, languageDocument, 1, typeDocument, 1);
        postfixDocument = addJProp(and1, addDProp("postfixDocument", "Доп. описание", StringClass.get(15), document), 1, multipleDocument, 1);
        hidePostfixDocument = addJProp(and1, addCProp(StringClass.get(40), "Постфикс", document), 1, multipleDocument, 1);

        LP translateNameDocument = addJProp("Перевод", translateLanguageDocumentType, languageDocument, 1, typeDocument, 1);
        nameDocument = addJProp("nameDocument", "Заголовок", string2, translateNameDocument, 1, addSUProp(Union.OVERRIDE, addCProp(StringClass.get(15),"", document), postfixDocument), 1);

        quantityProjectLanguageDocumentType = addSGProp("projectLanguageDocumentType", "Кол-во док.", addCProp(IntegerClass.instance, 1, document), projectDocument, 1, languageDocument, 1, typeDocument, 1); // сколько экспертов высказалось
        LP notEnoughProjectLanguageDocumentType = addSUProp(Union.OVERRIDE, addJProp(greater2, quantityProjectLanguageDocumentType, 1, 2, 3, quantityMaxLanguageDocumentType, 2, 3),
                addJProp(less2, addSUProp(Union.OVERRIDE, addCProp(IntegerClass.instance, 0, project, language, documentType), quantityProjectLanguageDocumentType), 1, 2, 3, quantityMinLanguageDocumentType, 2, 3));
        notEnoughProject = addMGProp(projectStatusGroup, "notEnoughProject", true, "Недостаточно док.", notEnoughProjectLanguageDocumentType, 1);

        autoGenerateProject = addDProp(projectStatusGroup, "autoGenerateProject", "Авт. зас.", LogicalClass.instance, project);

        fileDocument = addDProp(baseGroup, "fileDocument", "Файл", PDFClass.instance, document);
        loadFileDocument = addLFAProp(baseGroup, "Загрузить", fileDocument);
        openFileDocument = addOFAProp(baseGroup, "Открыть", fileDocument);

        inExpertVote = addDProp(baseGroup, "inExpertVote", "Вкл", LogicalClass.instance, expert, vote); // !!! нужно отослать письмо с документами и т.д
        oldExpertVote = addDProp(baseGroup, "oldExpertVote", "Пред.", LogicalClass.instance, expert, vote); // !!! нужно отослать письмо с документами и т.д
        inNewExpertVote = addJProp(baseGroup, "inNewExpertVote", "Вкл (нов.)", andNot1, inExpertVote, 1, 2, oldExpertVote, 1, 2);
        inOldExpertVote = addJProp(baseGroup, "inOldExpertVote", "Вкл (стар.)", and1, inExpertVote, 1, 2, oldExpertVote, 1, 2);

        dateStartVote = addJProp(baseGroup, "dateStartVote", true, "Дата начала", and1, date, 1, is(vote), 1);
//        dateEndVote = addJProp(baseGroup, "dateEndVote", "Дата окончания", addDate2, dateStartVote, 1, requiredPeriod);
        aggrDateEndVote = addJProp(baseGroup, "aggrDateEndVote", "Дата окончания (агр.)", addDate2, dateStartVote, 1, requiredPeriod);
        dateEndVote = addDProp(baseGroup, "dateEndVote", "Дата окончания", DateClass.instance, vote);
        dateEndVote.setDerivedForcedChange(true, aggrDateEndVote, 1, dateStartVote, 1);

        openedVote = addJProp(baseGroup, "openedVote", "Открыто", groeq2, dateEndVote, 1, currentDate);
        closedVote = addJProp(baseGroup, "closedVote", "Закрыто", andNot1, is(vote), 1, openedVote, 1);

        voteInProgressProject = addAGProp(idGroup, "voteInProgressProject", true, "Тек. заседание (ИД)",
                                       openedVote, 1, projectVote, 1); // активно только одно заседание

        // результаты голосования
        voteResultExpertVote = addDProp(idGroup, "voteResultExpertVote", "Результат (ИД)", voteResult, expert, vote);
        voteResultNewExpertVote = addJProp(baseGroup, "voteResultNewExpertVote", "Результат (ИД) (новый)", and1,
                voteResultExpertVote, 1, 2, inNewExpertVote, 1, 2);

        dateExpertVote = addDProp(voteResultCheckGroup, "dateExpertVote", "Дата рез.", DateClass.instance, expert, vote);

        doneExpertVote = addJProp(baseGroup, "doneExpertVote", "Проголосовал", equals2,
                                  voteResultExpertVote, 1, 2, addCProp(voteResult, "voted"));
        doneNewExpertVote = addJProp(baseGroup, "doneNewExpertVote", "Проголосовал (новый)", and1,
                                  doneExpertVote, 1, 2, inNewExpertVote, 1, 2);
        doneOldExpertVote = addJProp(baseGroup, "doneOldExpertVote", "Проголосовал (старый)", and1,
                                  doneExpertVote, 1, 2, inOldExpertVote, 1, 2);

        refusedExpertVote = addJProp(baseGroup, "refusedExpertVote", "Проголосовал", equals2,
                                  voteResultExpertVote, 1, 2, addCProp(voteResult, "refused"));

        connectedExpertVote = addJProp(baseGroup, "connectedExpertVote", "Аффилирован", equals2,
                                  voteResultExpertVote, 1, 2, addCProp(voteResult, "connected"));

        nameVoteResultExpertVote = addJProp(voteResultCheckGroup, "nameVoteResultExpertVote", "Результат", name, voteResultExpertVote, 1, 2);

        LP incrementVote = addJProp(greater2, dateEndVote, 1, addCProp(DateClass.instance, new java.sql.Date(2011-1900, 4-1, 29)));
        inProjectExpert = addMGProp(baseGroup, "inProjectExpert", "Вкл. в заседания", inExpertVote, projectVote, 2, 1);
        voteProjectExpert = addAGProp(baseGroup, "voteProjectExpert", "Результ. заседание", addJProp(and1, voteResultNewExpertVote, 1, 2, incrementVote, 2), 2, projectVote, 2);
        voteResultProjectExpert = addJProp(baseGroup, "voteResultProjectExpert", "Результ. заседания", voteResultExpertVote, 2, voteProjectExpert, 1, 2);
        doneProjectExpert = addJProp(baseGroup, "doneProjectExpert", "Проголосовал", equals2, voteResultProjectExpert, 1, 2, addCProp(voteResult, "voted"));
        LP doneProject = addSGProp(baseGroup, "doneProject", "Проголосовало", addJProp(and1, addCProp(IntegerClass.instance, 1), doneProjectExpert, 1, 2), 2); // сколько экспертов высказалось

        inClusterExpertVote = addDProp(voteResultCheckGroup, "inClusterExpertVote", "Соотв-ие кластеру", LogicalClass.instance, expert, vote);
        inClusterNewExpertVote = addJProp(baseGroup, "inClusterNewExpertVote", "Соотв-ие кластеру (новый)", and1,
                inClusterExpertVote, 1, 2, inNewExpertVote, 1, 2);

        innovativeExpertVote = addDProp(voteResultCheckGroup, "innovativeExpertVote", "Инновац.", LogicalClass.instance, expert, vote);
        innovativeNewExpertVote = addJProp(baseGroup, "innovativeNewExpertVote", "Инновац. (новый)", and1,
                innovativeExpertVote, 1, 2, inNewExpertVote, 1, 2);

        foreignExpertVote = addDProp(voteResultCheckGroup, "foreignExpertVote", "Иностр. специалист", LogicalClass.instance, expert, vote);
        foreignNewExpertVote = addJProp(baseGroup, "foreignNewExpertVote", "Иностр. специалист (новый)", and1,
                foreignExpertVote, 1, 2, inNewExpertVote, 1, 2);

        innovativeCommentExpertVote = addDProp(voteResultCommentGroup, "innovativeCommentExpertVote", "Инновационность (комм.)", TextClass.instance, expert, vote);
        competentExpertVote = addDProp(voteResultCheckGroup, "competentExpertVote", "Компет.", IntegerClass.instance, expert, vote);
        completeExpertVote = addDProp(voteResultCheckGroup, "completeExpertVote", "Полнота информ.", IntegerClass.instance, expert, vote);
        completeCommentExpertVote = addDProp(voteResultCommentGroup, "completeCommentExpertVote", "Полнота информации (комм.)", TextClass.instance, expert, vote);

        followed(doneExpertVote, inClusterExpertVote, innovativeExpertVote, foreignExpertVote, innovativeCommentExpertVote, competentExpertVote, completeExpertVote, completeCommentExpertVote);
        followed(voteResultExpertVote, dateExpertVote);

        quantityRepliedVote = addSGProp(voteResultGroup, "quantityRepliedVote", "Ответило",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), voteResultExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityDoneVote = addSGProp(voteResultGroup, "quantityDoneVote", "Проголосовало",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), doneExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityDoneOldVote = addSGProp(voteResultGroup, "quantityDoneOldVote", "Проголосовало (пред.)",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), doneOldExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityRefusedVote = addSGProp(voteResultGroup, "quantityRefusedVote", "Отказалось",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), refusedExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityConnectedVote = addSGProp(voteResultGroup, "quantityConnectedVote", "Аффилировано",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), connectedExpertVote, 1, 2), 2); // сколько экспертов высказалось

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

        acceptedVote = addJProp(voteResultGroup, "acceptedVote", true, "Положительно", and(false, false),
                                acceptedInClusterVote, 1, acceptedInnovativeVote, 1, acceptedForeignVote, 1);

        succeededVote = addJProp(voteResultGroup, "succeededVote", true, "Состоялось", groeq2, quantityDoneVote, 1, limitExperts); // достаточно экспертов
        succeededClusterVote = addJProp("succeededClusterVote", "Состоялось в тек. кластере", and1, succeededVote, 1, equalsClusterProjectVote, 1);

        LP betweenExpertVoteDateFromDateTo = addJProp(betweenDates, dateExpertVote, 1, 2, 3, 4);
        doneExpertVoteDateFromDateTo = addJProp(and1, doneNewExpertVote, 1, 2, betweenExpertVoteDateFromDateTo, 1, 2, 3, 4);
        quantityDoneExpertDateFromDateTo = addSGProp("quantityDoneExpertDateFromDateTo", "Кол-во голосов.",
                addJProp(and1, addCProp(IntegerClass.instance, 1), doneExpertVoteDateFromDateTo, 1, 2, 3, 4), 1, 3, 4); // в скольки заседаниях поучавствовал
        inExpertVoteDateFromDateTo = addJProp(and1, inNewExpertVote, 1, 2, betweenExpertVoteDateFromDateTo, 1, 2, 3, 4);
        quantityInExpertDateFromDateTo = addSGProp("quantityInExpertDateFromDateTo", "Кол-во участ.",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), inExpertVoteDateFromDateTo, 1, 2, 3, 4), 1, 3, 4); // в скольки заседаниях поучавствовал

        voteSucceededProject = addAGProp(idGroup, "voteSucceededProject", true, "Успешное заседание (ИД)", succeededClusterVote, 1, projectVote, 1);

        noCurrentVoteProject = addJProp(projectStatusGroup, "noCurrentVoteProject", "Нет текущих заседаний", andNot1, is(project), 1, voteInProgressProject, 1); // нету текущих заседаний

        voteValuedProject = addJProp(idGroup, "voteValuedProject", true, "Оцененнное заседание (ИД)", and1, voteSucceededProject, 1, noCurrentVoteProject, 1); // нет открытого заседания и есть состояшееся заседания

        acceptedProject = addJProp(projectStatusGroup, "acceptedProject", "Оценен положительно", acceptedVote, voteValuedProject, 1);
        voteRejectedProject = addJProp(projectStatusGroup, "rejectedProject", "Оценен отрицательно", andNot1, voteValuedProject, 1, acceptedVote, 1);

        needExtraVoteProject = addJProp("needExtraVoteProject", true, "Треб. заседание", and(true, true, true),
                                        is(project), 1,
                                        notEnoughProject, 1,
                                        voteInProgressProject, 1,
                                        voteSucceededProject, 1); // есть открытое заседания и есть состояшееся заседания !!! нужно создать новое заседание

        addConstraint(addJProp("Эксперт не соответствует необходимому кластеру", diff2,
                               clusterExpert, 1, addJProp(and1, clusterVote, 2, inExpertVote, 1, 2), 1, 2), false);

        addConstraint(addJProp("Количество экспертов не соответствует требуемому", andNot1, is(vote), 1, addJProp(equals2, requiredQuantity,
                                                                                                                  addSGProp(addJProp(and1, addCProp(IntegerClass.instance, 1), inExpertVote, 2, 1), 1), 1), 1), false);

        generateDocumentsProjectDocumentType = addAProp(actionGroup, new GenerateDocumentsActionProperty());

        generateVoteProject = addAProp(actionGroup, new GenerateVoteActionProperty());
        hideGenerateVoteProject = addHideCaptionProp(privateGroup, "Сгенерировать заседание", generateVoteProject, needExtraVoteProject);
//        generateVoteProject.setDerivedForcedChange(addCProp(ActionClass.instance, true), needExtraVoteProject, 1, autoGenerateProject, 1);

        generateLoginPassword.setDerivedForcedChange(addCProp(ActionClass.instance, true), is(expert), 1);

        expertLogin = addAGProp(baseGroup, "expertLogin", "Эксперт (ИД)", userLogin);
        disableExpert = addDProp(baseGroup, "disableExpert", "Не акт.", LogicalClass.instance, expert);

        addCUProp("userRole", true, "Роль пользователя", addCProp(StringClass.get(30), "expert", expert));

        statusProject = addCaseUProp(idGroup, "statusProject", true, "Статус (ИД)",
                    acceptedProject, 1, addCProp(projectStatus, "accepted", project), 1,
                    voteRejectedProject, 1, addCProp(projectStatus, "rejected", project), 1,
                    voteSucceededProject, 1, addCProp(projectStatus, "succeeded", project), 1,
                    voteInProgressProject, 1, addCProp(projectStatus, "inProgress", project), 1,
                    needExtraVoteProject, 1, addCProp(projectStatus, "needExtraVote", project), 1,
                    notEnoughProject, 1, addCProp(projectStatus, "needDocuments", project), 1);
/*        statusProject = addIfElseUProp(idGroup, "statusProject", true, "Статус (ИД)",
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
                                  acceptedProject, 1);*/
        nameStatusProject = addJProp(projectInformationGroup, "nameStatusProject", "Статус", name, statusProject, 1);

        statusProjectVote = addJProp(idGroup, "statusProjectVote", "Статус проекта (ИД)", statusProject, projectVote, 1);
        nameStatusProjectVote = addJProp(baseGroup, "nameStatusProjectVote", "Статус проекта", name, statusProjectVote, 1);

        projectSucceededClaimer = addAGProp(idGroup, "projectSucceededClaimer", true, "Успешный проект (ИД)", acceptedProject, 1, claimerProject, 1);


        // статистика по экспертам
        quantityTotalExpert = addSGProp(expertResultGroup, "quantityTotalExpert", "Всего заседаний",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), inNewExpertVote, 1, 2), 1);

        quantityDoneExpert = addSGProp(expertResultGroup, "quantityDoneExpert", "Проголосовал",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), doneNewExpertVote, 1, 2), 1);
        percentDoneExpert = addJProp(expertResultGroup, "percentDoneExpert", "Проголосовал (%)", percent, quantityDoneExpert, 1, quantityTotalExpert, 1);

        LP quantityInClusterExpert = addSGProp("quantityInClusterExpert", "Соотв-ие кластеру (голоса)",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), inClusterNewExpertVote, 1, 2), 1);
        percentInClusterExpert = addJProp(expertResultGroup, "percentInClusterExpert", "Соотв-ие кластеру (%)", percent, quantityInClusterExpert, 1, quantityDoneExpert, 1);

        LP quantityInnovativeExpert = addSGProp("quantityInnovativeExpert", "Инновац. (голоса)",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), innovativeNewExpertVote, 1, 2), 1);
        percentInnovativeExpert = addJProp(expertResultGroup, "percentInnovativeExpert", "Инновац. (%)", percent, quantityInnovativeExpert, 1, quantityDoneExpert, 1);

        LP quantityForeignExpert = addSGProp("quantityForeignExpert", "Иностр. специалист (голоса)",
                                     addJProp(and1, addCProp(IntegerClass.instance, 1), foreignNewExpertVote, 1, 2), 1);
        percentForeignExpert = addJProp(expertResultGroup, "percentForeignExpert", "Иностр. специалист (%)", percent, quantityForeignExpert, 1, quantityDoneExpert, 1);

        prevDateStartVote = addOProp("prevDateStartVote", "Пред. засед. (старт)", OrderType.PREVIOUS, dateStartVote, true, true, 1, projectVote, 1, date, 1);
        prevDateVote = addOProp("prevDateVote", "Пред. засед. (окончание)", OrderType.PREVIOUS, dateEndVote, true, true, 1, projectVote, 1, date, 1);
        numberNewExpertVote = addOProp("numberNewExpertVote", "Номер (нов.)", OrderType.SUM, addJProp(and1, addCProp(IntegerClass.instance, 1), inNewExpertVote, 1, 2), true, true, 1, 2, 1);
        numberOldExpertVote = addOProp("numberOldExpertVote", "Номер (стар.)", OrderType.SUM, addJProp(and1, addCProp(IntegerClass.instance, 1), inOldExpertVote, 1, 2), true, true, 1, 2, 1);

        emailDocuments = addDProp(baseGroup, "emailDocuments", "E-mail для документов", StringClass.get(50));

        emailLetterExpertVoteEA = addEAProp(expert, vote);
        addEARecepient(emailLetterExpertVoteEA, email, 1);

        emailLetterExpertVote = addJProp(baseGroup, true, "emailLetterExpertVote", "Письмо о заседании (e-mail)",
                emailLetterExpertVoteEA, 1, 2, addJProp(letterExpertSubjectLanguage, languageExpert, 1), 1);
        emailLetterExpertVote.setImage("/images/email.png");
        emailLetterExpertVote.property.askConfirm = true;
        emailLetterExpertVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), inNewExpertVote, 1, 2);

        allowedEmailLetterExpertVote = addJProp(baseGroup, "Письмо о заседании (e-mail)", "Письмо о заседании", andNot1, emailLetterExpertVote, 1, 2, voteResultExpertVote, 1, 2);
        allowedEmailLetterExpertVote.property.askConfirm = true;

        emailStartVoteEA = addEAProp(vote);
        addEARecepient(emailStartVoteEA, emailDocuments);

        emailStartHeaderVote = addJProp("emailStartHeaderVote", "Заголовок созыва заседания", addSFProp("(CAST(prm1 as text))||(CAST(prm2 as text))", StringClass.get(2000), 2), addCProp(StringClass.get(2000), "Созыв заседания - "), nameNativeClaimerVote, 1);
        emailStartVote = addJProp(baseGroup, true, "emailStartVote", "Созыв заседания (e-mail)", emailStartVoteEA, 1, emailStartHeaderVote, 1);
        emailStartVote.property.askConfirm = true;
//        emailStartVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), openedVote, 1);

        emailProtocolVoteEA = addEAProp(vote);
        addEARecepient(emailProtocolVoteEA, emailDocuments);

        emailProtocolHeaderVote = addJProp("emailProtocolHeaderVote", "Заголовок протокола заседания", addSFProp("(CAST(prm1 as text))||(CAST(prm2 as text))", StringClass.get(2000), 2), addCProp(StringClass.get(2000), "Протокол заседания - "), nameNativeClaimerVote, 1);
        emailProtocolVote = addJProp(baseGroup, true, "emailProtocolVote", "Протокол заседания (e-mail)", emailProtocolVoteEA, 1, emailProtocolHeaderVote, 1);
        emailProtocolVote.property.askConfirm = true;
//        emailProtocolVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), closedVote, 1);

        emailClosedVoteEA = addEAProp(vote);
        addEARecepient(emailClosedVoteEA, emailDocuments);

        emailClosedHeaderVote = addJProp(addSFProp("(CAST(prm1 as text))||(CAST(prm2 as text))", StringClass.get(2000), 2), addCProp(StringClass.get(2000), "Результаты заседания - "), nameNativeClaimerVote, 1);
        emailClosedVote = addJProp(baseGroup, true, "emailClosedVote", "Результаты заседания (e-mail)", emailClosedVoteEA, 1, emailClosedHeaderVote, 1);
        emailClosedVote.setImage("/images/email.png");
        emailClosedVote.property.askConfirm = true;
        emailClosedVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), closedVote, 1);

        isForeignExpert = addJProp("isForeignExpert", "Иностр.", equals2, languageExpert, 1, addCProp(language, "english"));
        localeExpert = addJProp("localeExpert", "Locale", localeLanguage, languageExpert, 1);

        emailAuthExpertEA = addEAProp(expert);
        addEARecepient(emailAuthExpertEA, email, 1);

        emailAuthExpert = addJProp(baseGroup, true, "emailAuthExpert", "Аутентификация эксперта (e-mail)",
                emailAuthExpertEA, 1, addJProp(authExpertSubjectLanguage, languageExpert, 1), 1);
        emailAuthExpert.setImage("/images/email.png");
        emailAuthExpert.property.askConfirm = true;
//        emailAuthExpert.setDerivedChange(addCProp(ActionClass.instance, true), userLogin, 1, userPassword, 1);

        emailToExpert = addJProp("emailToExpert", "Эксперт по e-mail", addJProp(and1, 1, is(expert), 1), emailToObject, 1);
    }

    protected void initTables() {
    }

    protected void initIndexes() {
    }

    FormEntity languageDocumentTypeForm;
    FormEntity globalForm;

    protected void initNavigators() throws JRException, FileNotFoundException {
        addFormEntity(new ProjectFullFormEntity(objectElement, "projectFull"));

        addFormEntity(new ProjectFormEntity(baseElement, "project"));
        addFormEntity(new VoteFormEntity(baseElement, "vote"));
        addFormEntity(new ExpertFormEntity(baseElement, "expert"));
        addFormEntity(new VoteExpertFormEntity(baseElement, "voteExpert"));
        languageDocumentTypeForm = addFormEntity(new LanguageDocumentTypeFormEntity(baseElement, "languageDocumentType"));
        addFormEntity(new DocumentTemplateFormEntity(baseElement, "documentTemplate"));
        globalForm = addFormEntity(new GlobalFormEntity(baseElement, "global"));

        NavigatorElement print = new NavigatorElement(baseElement, "print", "Печатные формы");
        addFormEntity(new VoteStartFormEntity(print, "voteStart"));
        addFormEntity(new ExpertLetterFormEntity(print, "expertLetter"));
        addFormEntity(new VoteProtocolFormEntity(print, "voteProtocol"));
        addFormEntity(new ExpertProtocolFormEntity(print, "expertProtocol"));
        addFormEntity(new ExpertAuthFormEntity(print, "expertAuth"));
        addFormEntity(new ClaimerAcceptedFormEntity(print, "claimerAccepted"));
        addFormEntity(new ClaimerRejectedFormEntity(print, "claimerRejected"));
        addFormEntity(new ClaimerStatusFormEntity(print, "claimerStatus"));
    }

    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        PolicyManager.defaultSecurityPolicy.navigator.deny(adminElement, objectElement, languageDocumentTypeForm, globalForm);

        PolicyManager.defaultSecurityPolicy.property.view.deny(userPassword);

        PolicyManager.defaultSecurityPolicy.property.change.deny(dateStartVote, dateEndVote, inExpertVote, oldExpertVote, voteResultExpertVote, doneExpertVote);

        for (Property property : voteResultGroup.getProperties())
            PolicyManager.defaultSecurityPolicy.property.change.deny(property);

        User admin = addUser("admin", "fusion");
        admin.addSecurityPolicy(permitAllPolicy);
    }

    private class ProjectFullFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;
        private ObjectEntity objPatent;
        private ObjectEntity objAcademic;
        private ObjectEntity objNonRussianSpecialist;

        private ProjectFullFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Все данные по проекту");

            objProject = addSingleGroupObject(project, "Описание проекта", projectInformationGroup, innovationGroup, executiveSummaryGroup, sourcesFundingGroup, equipmentGroup, projectDocumentsGroup, projectStatusGroup);

            getPropertyDraw(nameReturnInvestorProject).propertyCaption = addPropertyObject(hideNameReturnInvestorProject, objProject);
            getPropertyDraw(amountReturnFundsProject).propertyCaption = addPropertyObject(hideAmountReturnFundsProject, objProject);
            getPropertyDraw(nameNonReturnInvestorProject).propertyCaption = addPropertyObject(hideNameNonReturnInvestorProject, objProject);
            getPropertyDraw(amountNonReturnFundsProject).propertyCaption = addPropertyObject(hideAmountNonReturnFundsProject, objProject);

            getPropertyDraw(isCapitalInvestmentProject).propertyCaption = addPropertyObject(hideIsCapitalInvestmentProject, objProject);
            getPropertyDraw(isPropertyInvestmentProject).propertyCaption = addPropertyObject(hideIsPropertyInvestmentProject, objProject);
            getPropertyDraw(isGrantsProject).propertyCaption = addPropertyObject(hideIsGrantsProject, objProject);
            getPropertyDraw(isOtherNonReturnInvestmentsProject).propertyCaption = addPropertyObject(hideIsOtherNonReturnInvestmentsProject, objProject);

            getPropertyDraw(commentOtherNonReturnInvestmentsProject).propertyCaption = addPropertyObject(hideCommentOtherNonReturnInvestmentsProject, objProject);
            getPropertyDraw(amountOwnFundsProject).propertyCaption = addPropertyObject(hideAmountOwnFundsProject, objProject);
            getPropertyDraw(amountFundsProject).propertyCaption = addPropertyObject(hideAmountFundsProject, objProject);
            getPropertyDraw(commentOtherSoursesProject).propertyCaption = addPropertyObject(hideCommentOtherSoursesProject, objProject);

            getPropertyDraw(descriptionTransferEquipmentProject).propertyCaption = addPropertyObject(hideDescriptionTransferEquipmentProject, objProject);
            getPropertyDraw(ownerEquipmentProject).propertyCaption = addPropertyObject(hideOwnerEquipmentProject, objProject);
            getPropertyDraw(specificationEquipmentProject).propertyCaption = addPropertyObject(hideSpecificationEquipmentProject, objProject);
            getPropertyDraw(descriptionEquipmentProject).propertyCaption = addPropertyObject(hideDescriptionEquipmentProject, objProject);
            getPropertyDraw(commentEquipmentProject).propertyCaption = addPropertyObject(hideCommentEquipmentProject, objProject);

            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);

            objPatent = addSingleGroupObject(patent, baseGroup);
            addObjectActions(this, objPatent);

            getPropertyDraw(ownerPatent).propertyCaption = addPropertyObject(hideOwnerPatent, objPatent);
            getPropertyDraw(nameOwnerTypePatent).propertyCaption = addPropertyObject(hideNameOwnerTypePatent, objPatent);
            getPropertyDraw(loadFileIntentionOwnerPatent).propertyCaption = addPropertyObject(hideLoadFileIntentionOwnerPatent, objPatent);
            getPropertyDraw(openFileIntentionOwnerPatent).propertyCaption = addPropertyObject(hideOpenFileIntentionOwnerPatent, objPatent);

            getPropertyDraw(valuatorPatent).propertyCaption = addPropertyObject(hideValuatorPatent, objPatent);
            getPropertyDraw(loadFileActValuationPatent).propertyCaption = addPropertyObject(hideLoadFileActValuationPatent, objPatent);
            getPropertyDraw(openFileActValuationPatent).propertyCaption = addPropertyObject(hideOpenFileActValuationPatent, objPatent);

            objAcademic = addSingleGroupObject(academic, baseGroup);
            addObjectActions(this, objAcademic);

            objNonRussianSpecialist = addSingleGroupObject(nonRussianSpecialist, baseGroup);
            addObjectActions(this, objNonRussianSpecialist);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectPatent, objPatent), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectAcademic, objAcademic), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectNonRussianSpecialist, objNonRussianSpecialist), Compare.EQUALS, objProject));

            addProject = addMFAProp(actionGroup, "Добавить", this, new ObjectEntity[] {}, true, addPropertyObject(getAddObjectAction(project)));
            editProject = addMFAProp(actionGroup, "Редактировать", this, new ObjectEntity[] {objProject}).setImage("/images/edit.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.getGroupPropertyContainer(objProject.groupTo, innovationGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, executiveSummaryGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, sourcesFundingGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, equipmentGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, projectDocumentsGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, projectStatusGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;

            design.addIntersection(design.getGroupPropertyContainer(objProject.groupTo, innovationGroup),
                                   design.getGroupPropertyContainer(objProject.groupTo, sourcesFundingGroup), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupPropertyContainer(objProject.groupTo, executiveSummaryGroup),
                                   design.getGroupPropertyContainer(objProject.groupTo, sourcesFundingGroup), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupPropertyContainer(objProject.groupTo, projectDocumentsGroup),
                                   design.getGroupPropertyContainer(objProject.groupTo, projectStatusGroup), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objProject.groupTo));
            specContainer.add(design.getGroupObjectContainer(objProject.groupTo));
            specContainer.add(design.getGroupObjectContainer(objPatent.groupTo));
            specContainer.add(design.getGroupObjectContainer(objAcademic.groupTo));
            specContainer.add(design.getGroupObjectContainer(objNonRussianSpecialist.groupTo));
            specContainer.tabbedPane = true;

            design.getMainContainer().addBefore(design.getGroupPropertyContainer(objProject.groupTo, projectInformationGroup), specContainer);

            design.setShowTableFirstLogical(true);

            return design;
        }
    }

    private class ProjectFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objProject;
        private ObjectEntity objVote;
        private ObjectEntity objDocument;
        private ObjectEntity objExpert;
        private ObjectEntity objDocumentTemplate;
        private RegularFilterGroupEntity projectFilterGroup;

        private ProjectFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр проектов");

            objProject = addSingleGroupObject(project, date, nameNative, nameForeign,  nameNativeClusterProject, nameNativeClaimerProject, nameStatusProject, autoGenerateProject, generateVoteProject, editProject);
            addObjectActions(this, objProject);

//            addPropertyDraw(addProject).toDraw = objProject.groupTo;
//            getPropertyDraw(addProject).forceViewType = ClassViewType.PANEL;

            getPropertyDraw(generateVoteProject).forceViewType = ClassViewType.PANEL;
            getPropertyDraw(generateVoteProject).propertyCaption = addPropertyObject(hideGenerateVoteProject, objProject);

            objVote = addSingleGroupObject(vote, dateStartVote, dateEndVote, nameNativeClusterVote, equalsClusterProjectVote, openedVote, succeededVote, acceptedVote, quantityDoneVote, quantityInClusterVote, quantityInnovativeVote, quantityForeignVote, delete);
            objVote.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.HIDE));

            objDocumentTemplate = addSingleGroupObject(documentTemplate, "Шаблон документов", name);
            objDocumentTemplate.groupTo.setSingleClassView(ClassViewType.PANEL);
            setReadOnly(objDocumentTemplate, true);
            addPropertyDraw(generateDocumentsProjectDocumentType, objProject, objDocumentTemplate);

            objDocument = addSingleGroupObject(document, nameTypeDocument, nameLanguageDocument, postfixDocument, loadFileDocument, openFileDocument);
            addObjectActions(this, objDocument);
            getPropertyDraw(postfixDocument).forceViewType = ClassViewType.PANEL;
            getPropertyDraw(postfixDocument).propertyCaption = addPropertyObject(hidePostfixDocument, objDocument);

            objExpert = addSingleGroupObject(expert);
            addPropertyDraw(objExpert, objVote, inExpertVote, oldExpertVote);
            addPropertyDraw(objExpert, name, email);
            addPropertyDraw(voteResultGroup, true, objExpert, objVote);

            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectVote, objVote), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectDocument, objDocument), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(clusterExpert, objExpert), Compare.EQUALS, addPropertyObject(clusterVote, objVote)));

            RegularFilterGroupEntity expertFilterGroup = new RegularFilterGroupEntity(genID());
            expertFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)),
                                                                "В заседании",
                                                                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(expertFilterGroup);

            projectFilterGroup = new RegularFilterGroupEntity(genID());
            projectFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                                                 new NotNullFilterEntity(addPropertyObject(voteValuedProject, objProject)),
                                                                 "Оценен",
                                                                 KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(projectFilterGroup);

            setPageSize(0);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

//            design.get(getPropertyDraw(addProject)).drawToToolbar = true;

            ContainerView specContainer = design.createContainer();
            specContainer.tabbedPane = true;

            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objProject.groupTo));

            ContainerView expertContainer = design.createContainer("Экспертиза по существу");
            expertContainer.add(design.getGroupObjectContainer(objVote.groupTo));
            expertContainer.add(design.getGroupObjectContainer(objExpert.groupTo));

            ContainerView docContainer = design.createContainer("Документы");
            docContainer.add(design.getGroupObjectContainer(objDocumentTemplate.groupTo));
            docContainer.add(design.getGroupObjectContainer(objDocument.groupTo));

            specContainer.add(docContainer);
            specContainer.add(expertContainer);
//

//            design.get(objVoteHeader.groupTo).grid.constraints.fillHorizontal = 1.5;

            design.getPanelContainer(objVote.groupTo).add(design.get(getPropertyDraw(generateVoteProject)));

            design.get(objProject.groupTo).grid.constraints.fillVertical = 1.5;
            design.get(objExpert.groupTo).grid.constraints.fillVertical = 1.5;

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 1);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, -1));

            design.get(objVote.groupTo).grid.hideToolbarItems();

            design.addIntersection(design.get(getPropertyDraw(innovativeCommentExpertVote)), design.get(getPropertyDraw(completeCommentExpertVote)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class GlobalFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private GlobalFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Глобальные параметры");

            addPropertyDraw(new LP[]{currentDate, requiredPeriod, requiredQuantity, limitExperts, emailDocuments});
        }
    }

    private class VoteFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objVote;
        private ObjectEntity objExpert;

        private VoteFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр заседаний");

            objVote = addSingleGroupObject(vote, nameNativeProjectVote, nameNativeClusterVote, equalsClusterProjectVote, dateStartVote, dateEndVote, openedVote, succeededVote, quantityDoneVote, emailClosedVote, delete);

            objExpert = addSingleGroupObject(expert, userFirstName, userLastName, userLogin, userPassword, email, nameNativeClusterExpert);

            addPropertyDraw(objExpert, objVote, oldExpertVote);
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

//            setReadOnly(true, objVote.groupTo);
//            setReadOnly(true, objExpert.groupTo);
//            setReadOnly(allowedEmailLetterExpertVote, false);
//            setReadOnly(emailClosedVote, false);

            setPageSize(0);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 0.5);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, -1));

            return design;
        }
    }

    private class ExpertFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objExpert;
        private ObjectEntity objVote;

        private ExpertFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр экспертов");

            objExpert = addSingleGroupObject(expert, selection, userFirstName, userLastName, userLogin, userPassword, email, disableExpert, nameNativeClusterExpert, nameLanguageExpert, expertResultGroup, generateLoginPassword, emailAuthExpert);
            addObjectActions(this, objExpert);

            objVote = addSingleGroupObject(vote, nameNativeProjectVote, dateStartVote, dateEndVote, openedVote, succeededVote, quantityDoneVote);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            addPropertyDraw(objExpert, objVote, allowedEmailLetterExpertVote);
            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));

//            setReadOnly(true, objVote.groupTo);
//            setReadOnly(allowedEmailLetterExpertVote, false);

            setPageSize(0);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 0.5);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, -1));

            return design;
        }
    }

    private class VoteExpertFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objExpert;
        private ObjectEntity objVote;

        private VoteExpertFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр голосований");

            objVote = addSingleGroupObject(vote, nameNativeProjectVote, dateStartVote, dateEndVote, openedVote, succeededVote);

            objExpert = addSingleGroupObject(expert, userFirstName, userLastName, email, nameNativeClusterExpert, nameLanguageExpert);

            addPropertyDraw(objExpert, objVote, allowedEmailLetterExpertVote);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            addPropertyDraw(expertResultGroup, true, objExpert);            
            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            GroupObjectEntity gobjVoteExpert = new GroupObjectEntity(genID());
            gobjVoteExpert.add(objVote);
            gobjVoteExpert.add(objExpert);
            addGroup(gobjVoteExpert);
            gobjVoteExpert.setSingleClassView(ClassViewType.GRID);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));
            
            RegularFilterGroupEntity filterGroupOpenedVote = new RegularFilterGroupEntity(genID());
            filterGroupOpenedVote.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(openedVote, objVote)),
                                  "Только открытые заседания",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroupOpenedVote);

            RegularFilterGroupEntity filterGroupDoneExpertVote = new RegularFilterGroupEntity(genID());
            filterGroupDoneExpertVote.addFilter(new RegularFilterEntity(genID(),
                                  new CompareFilterEntity(addPropertyObject(doneExpertVote, objExpert, objVote), Compare.EQUALS, addPropertyObject(vtrue)),
                                  "Только проголосовавшие",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupDoneExpertVote);

            RegularFilterGroupEntity filterGroupExpertVote = new RegularFilterGroupEntity(genID());
            filterGroupExpertVote.addFilter(new RegularFilterEntity(genID(),
                                  new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(voteResultExpertVote, objExpert, objVote))),
                                  "Только не принявшие участие",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroupExpertVote);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 0.5);

            return design;
        }
    }

    private class DocumentTemplateFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private DocumentTemplateFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Шаблоны документов");

            ObjectEntity objDocumentTemplate = addSingleGroupObject(documentTemplate, "Шаблон", name);
            addObjectActions(this, objDocumentTemplate);

            ObjectEntity objDocumentTemplateDetail = addSingleGroupObject(documentTemplateDetail, nameTypeDocument, nameLanguageDocument);
            addObjectActions(this, objDocumentTemplateDetail);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(documentTemplateDocumentTemplateDetail, objDocumentTemplateDetail), Compare.EQUALS, objDocumentTemplate));
        }
    }

    private class LanguageDocumentTypeFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту
        private ObjectEntity objLanguage;
        private ObjectEntity objDocumentType;

        private GroupObjectEntity gobjLanguageDocumentType;

        private LanguageDocumentTypeFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Обязательные документы");

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

        private ExpertLetterFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Письмо о заседании", true);

            gobjExpertVote = new GroupObjectEntity(2, "expertVote");
            objExpert = new ObjectEntity(2, "expert", expert, "Эксперт");
            objVote = new ObjectEntity(3, "vote", vote, "Заседание");
            gobjExpertVote.add(objExpert);
            gobjExpertVote.add(objVote);
            addGroup(gobjExpertVote);
            gobjExpertVote.initClassView = ClassViewType.PANEL;

            addPropertyDraw(webHost, gobjExpertVote);
            addPropertyDraw(requiredPeriod, gobjExpertVote);
            addPropertyDraw(objExpert, name, isForeignExpert, localeExpert);
            addPropertyDraw(objVote, nameNativeClaimerVote, nameForeignClaimerVote, nameNativeProjectVote, nameForeignProjectVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));

            objDocument = addSingleGroupObject(8, "document", document, fileDocument);
            addPropertyDraw(nameDocument, objDocument).setSID("docName");
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectDocument, objDocument), Compare.EQUALS, addPropertyObject(projectVote, objVote)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(languageDocument, objDocument), Compare.EQUALS, addPropertyObject(languageExpert, objExpert)));

            addInlineEAForm(emailLetterExpertVoteEA, this, objExpert, 1, objVote, 2);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(gobjExpertVote);
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }
    }

    private class ExpertAuthFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту о логине
        private ObjectEntity objExpert;

        private ExpertAuthFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Аутентификация эксперта", true);

            objExpert = addSingleGroupObject(1, "expert", expert, userLogin, userPassword, name, isForeignExpert);
            objExpert.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailAuthExpertEA, this, objExpert, 1);
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }
    }

    private class VoteStartFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objVote;

        private ObjectEntity objExpert;
        private ObjectEntity objOldExpert;

        private VoteStartFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Созыв заседания", true);

            objVote = addSingleGroupObject(1, "vote", vote, date, dateProjectVote, nameNativeClaimerVote, nameNativeProjectVote, nameAblateClaimerVote, prevDateStartVote, prevDateVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            objExpert = addSingleGroupObject(2, "expert", expert, userLastName, userFirstName);
            addPropertyDraw(numberNewExpertVote, objExpert, objVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));

            objOldExpert = addSingleGroupObject(3, "oldexpert", expert, userLastName, userFirstName);
            addPropertyDraw(numberOldExpertVote, objOldExpert, objVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inOldExpertVote, objOldExpert, objVote)));

            addAttachEAForm(emailStartVoteEA, this, EmailActionProperty.Format.PDF, objVote, 1);
            addAttachEAForm(emailClosedVoteEA, this, EmailActionProperty.Format.PDF, emailStartHeaderVote, objVote, 1);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(objVote.groupTo);
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }
    }

    private class VoteProtocolFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objVote;
        
        private ObjectEntity objExpert;

        private VoteProtocolFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Протокол заседания", true);

            objVote = addSingleGroupObject(1, "vote", vote, dateProjectVote, date, dateEndVote, nameNativeProjectVote, nameNativeClaimerVote, nameNativeClusterVote, quantityRepliedVote, quantityDoneVote, quantityRefusedVote, quantityConnectedVote, succeededVote, quantityInClusterVote, acceptedInClusterVote, quantityInnovativeVote, acceptedInnovativeVote, quantityForeignVote, acceptedForeignVote, nameStatusProjectVote, prevDateStartVote, prevDateVote, quantityDoneOldVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(closedVote, objVote)));

            objExpert = addSingleGroupObject(12, "expert", expert, userFirstName, userLastName);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);

            addPropertyDraw(connectedExpertVote, objExpert, objVote);

            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(doneExpertVote, objExpert, objVote)),
                                              new NotNullFilterEntity(addPropertyObject(connectedExpertVote, objExpert, objVote))));

            addAttachEAForm(emailProtocolVoteEA, this, EmailActionProperty.Format.PDF, objVote, 1);
            addAttachEAForm(emailClosedVoteEA, this, EmailActionProperty.Format.PDF, emailProtocolHeaderVote, objVote, 1);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(objVote.groupTo);
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }
    }

    private class ExpertProtocolFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objDateFrom;
        private ObjectEntity objDateTo;

        private ObjectEntity objExpert;

        private ObjectEntity objVoteHeader;
        private ObjectEntity objVote;

        private ExpertProtocolFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Протокол голосования экспертов", true);

            GroupObjectEntity gobjDates = new GroupObjectEntity(1, "date");
            objDateFrom = new ObjectEntity(2, DateClass.instance, "Дата (с)");
            objDateTo = new ObjectEntity(3, DateClass.instance, "Дата (по)");
            gobjDates.add(objDateFrom);
            gobjDates.add(objDateTo);

            addGroup(gobjDates);
            gobjDates.setSingleClassView(ClassViewType.PANEL);

            addPropertyDraw(objDateFrom, objectValue);
            getPropertyDraw(objectValue, objDateFrom).setSID("dateFrom");

            // так делать неправильно в общем случае, поскольку getPropertyDraw ищет по groupObject, а не object

            addPropertyDraw(objDateTo, objectValue);
            getPropertyDraw(objectValue, objDateTo).setSID("dateTo");

            objExpert = addSingleGroupObject(4, "expert", expert, userFirstName, userLastName, nameNativeClusterExpert);
            objExpert.groupTo.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objExpert, objDateFrom, objDateTo, quantityDoneExpertDateFromDateTo, quantityInExpertDateFromDateTo);

            objVoteHeader = addSingleGroupObject(5, "voteHeader", vote);

            objVote = addSingleGroupObject(6, "vote", vote, dateProjectVote, date, dateEndVote, nameNativeProjectVote, nameNativeClaimerVote, nameNativeClusterVote);

            addPropertyDraw(nameNativeClaimerVote, objVoteHeader).setSID("nameNativeClaimerVoteHeader");
            addPropertyDraw(nameNativeProjectVote, objVoteHeader);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);

            addPropertyDraw(connectedExpertVote, objExpert, objVote);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVoteHeader), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVoteHeader), Compare.LESS_EQUALS, objDateTo));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(doneNewExpertVote, objExpert, objVoteHeader)));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVote), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVote), Compare.LESS_EQUALS, objDateTo));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(doneNewExpertVote, objExpert, objVote)));

            RegularFilterGroupEntity filterGroupExpertVote = new RegularFilterGroupEntity(genID());
            filterGroupExpertVote.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(quantityDoneExpertDateFromDateTo, objExpert, objDateFrom, objDateTo)),
                                  "Голосовавшие",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), true);
            filterGroupExpertVote.addFilter(new RegularFilterEntity(genID(),
                                  new NotNullFilterEntity(addPropertyObject(quantityInExpertDateFromDateTo, objExpert, objDateFrom, objDateTo)),
                                  "Учавствовавшие",
                                  KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroupExpertVote);

            setReadOnly(true);
            setReadOnly(false, objDateFrom.groupTo);
        }
    }

     private class ClaimerAcceptedFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private ClaimerAcceptedFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Решение о соответствии", true);

            objVote = addSingleGroupObject(vote, "Заседание", dateEndVote, nameNativeProjectVote, dateProjectVote, nameNativeClaimerVote, nameAblateClaimerVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

        }
    }

    private class ClaimerRejectedFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private ClaimerRejectedFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Решение о несоответствии", true);

            objVote = addSingleGroupObject(vote, "Заседание", dateEndVote, nameNativeProjectVote, dateProjectVote, nameNativeClaimerVote, nameAblateClaimerVote, nameDativusClaimerVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

        }
    }

     private class ClaimerStatusFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;

        private ClaimerStatusFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Решение о присвоении статуса участника", true);

            objProject = addSingleGroupObject(project, "Проект", date, nameNativeProject, nameNativeClaimerProject, nameAblateClaimerProject, nameDativusClaimerProject);
            objProject.groupTo.initClassView = ClassViewType.PANEL;

        }
    }
    

    public VoteInfo getVoteInfo(String voteId) throws RemoteException {

        try {
            DataSession session = createSession();
            try {
                VoteObjects vo = new VoteObjects(session, voteId);

                VoteInfo voteInfo = new VoteInfo();
                voteInfo.expertName = (String) name.read(session, vo.expertObj);

                Boolean isForeign = (Boolean) isForeignExpert.read(session, vo.expertObj);
                if (isForeign == null) {
                    voteInfo.projectName = (String) nameNative.read(session, vo.projectObj);
                    voteInfo.projectClaimer = (String) nameNativeClaimerProject.read(session, vo.projectObj);
                    voteInfo.projectCluster = (String) nameNativeClusterProject.read(session, vo.projectObj);
                } else {
                    voteInfo.projectName = (String) nameForeign.read(session, vo.projectObj);
                    voteInfo.projectClaimer = (String) nameForeignClaimerProject.read(session, vo.projectObj);
                    voteInfo.projectCluster = (String) nameForeignClusterProject.read(session, vo.projectObj);
                }

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

                voteInfo.voteDone = voteInfo.voteResult != null
                                    || !nvl((Boolean) openedVote.read(session, vo.voteObj), false);

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

    public void setVoteInfo(String voteId, VoteInfo voteInfo) throws RemoteException {

        try {
            DataSession session = createSession();
            try {
                VoteObjects vo = new VoteObjects(session, voteId);
                Boolean opVote = (Boolean) openedVote.read(session, vo.voteObj);
                if (opVote == null || !opVote) {
                    throw new RuntimeException("Голосование по заседанию с идентификатором " + vo.voteObj.object + " завершено");
                }

                Integer vResult = (Integer) voteResultExpertVote.read(session, vo.expertObj, vo.voteObj);
                if (vResult != null) {
                    throw new RuntimeException("Эксперт уже голосовал по этому заседанию.");
                }

                dateExpertVote.execute(DateConverter.dateToSql(new Date()), session, vo.expertObj, vo.voteObj);
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

    public ProfileInfo getProfileInfo(String expertLogin) throws RemoteException {
        assert expertLogin != null;
        try {
            DataSession session = createSession();
            try {
                Integer expertId = (Integer) loginToUser.read(session, new DataObject(expertLogin));
                if (expertId == null) {
                    throw new RuntimeException("Не удалось найти пользователя с логином " + expertLogin);
                }
                DataObject expertObj = new DataObject(expertId,  expert);

                ProfileInfo profileInfo = new ProfileInfo();
                profileInfo.expertName = (String) name.read(session, expertObj);
                profileInfo.expertEmail = (String) email.read(session, expertObj);
                boolean isForeign = isForeignExpert.read(session, expertObj) != null;

                Map<String, KeyExpr> keys = KeyExpr.getMapKeys(asList("exp", "vote"));
                Expr expExpr = keys.get("exp");
                Expr voteExpr = keys.get("vote");
                Expr projExpr = projectVote.getExpr(session.modifier, voteExpr);

                Query<String, String> q = new Query<String, String>(keys);
                q.and(inExpertVote.getExpr(session.modifier, expExpr, voteExpr).getWhere());
                q.and(userLogin.getExpr(session.modifier, expExpr).compare(new DataObject(expertLogin), Compare.EQUALS));

                q.properties.put("projectId", projExpr);
                q.properties.put("projectName", (isForeign ? nameForeign : nameNative).getExpr(session.modifier, projExpr));
                q.properties.put("projectClaimer", (isForeign ? nameForeignClaimerProject : nameNativeClaimerProject).getExpr(session.modifier, projExpr));
                q.properties.put("projectCluster", (isForeign ? nameForeignClusterProject : nameNativeClusterProject).getExpr(session.modifier, projExpr));

                q.properties.put("inCluster", inClusterExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("innovative", innovativeExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("innovativeComment", innovativeCommentExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("foreign", foreignExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("competent", competentExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("complete", completeExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("completeComment", completeCommentExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("vResult", voteResultExpertVote.getExpr(session.modifier, expExpr, voteExpr));
                q.properties.put("openedVote", openedVote.getExpr(session.modifier, voteExpr));
                q.properties.put("date", dateExpertVote.getExpr(session.modifier, expExpr, voteExpr));

                OrderedMap<Map<String, Object>, Map<String, Object>> values = q.execute(session.sql);
                profileInfo.voteInfos = new VoteInfo[values.size()];

                int i = 0;
                for (Map.Entry<Map<String, Object>, Map<String, Object>> entry : values.entrySet()) {
                    Map<String, Object> propValues = entry.getValue();

                    VoteInfo voteInfo = new VoteInfo();

                    int voteId = (Integer)entry.getKey().get("vote");
                    voteInfo.voteId = voteId;
                    voteInfo.linkHash = BaseUtils.encode(voteId, expertId);
                    voteInfo.projectName = (String) propValues.get("projectName");
                    voteInfo.projectClaimer = (String) propValues.get("projectClaimer");
                    voteInfo.projectCluster = (String) propValues.get("projectCluster");
                    voteInfo.inCluster = nvl((Boolean) propValues.get("inCluster"), false);
                    voteInfo.innovative = nvl((Boolean) propValues.get("innovative"), false);
                    voteInfo.innovativeComment = (String) propValues.get("innovativeComment");
                    voteInfo.foreign = nvl((Boolean) propValues.get("foreign"), false);
                    voteInfo.competent = nvl((Integer) propValues.get("competent"), 1);
                    voteInfo.complete = nvl((Integer) propValues.get("complete"), 1);
                    voteInfo.completeComment = (String) propValues.get("completeComment");

                    Integer vResult = (Integer) propValues.get("vResult");
                    if (vResult != null) {
                        voteInfo.voteResult = voteResult.getSID(vResult);
                    }

                    voteInfo.voteDone = voteInfo.voteResult != null
                                        || !nvl((Boolean) propValues.get("openedVote"), false);
                    voteInfo.date = DateConverter.sqlToDate((java.sql.Date)propValues.get("date"));

                    profileInfo.voteInfos[i++] = voteInfo;
                }

                return profileInfo;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при считывании информации о профиле эксперта", e);
        }
    }

    public void sentVoteDocuments(String expertLogin, int voteId) throws RemoteException {
        assert expertLogin != null;
        try {
            DataSession session = createSession();
            try {
                Integer expertId = (Integer) loginToUser.read(session, new DataObject(expertLogin));
                if (expertId == null) {
                    throw new RuntimeException("Не удалось найти пользователя с логином " + expertLogin);
                }

                allowedEmailLetterExpertVote.execute(true, session, new DataObject(expertId,  expert), new DataObject(voteId, vote));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при попытке выслать документы.", e);
        }
    }

    @Override
    public void remindPassword(String email) throws RemoteException {
        assert email != null;
        try {
            DataSession session = createSession();
            try {
                Integer expertId = (Integer) emailToExpert.read(session, new DataObject(email));
                if (expertId == null) {
                    throw new RuntimeException("Не удалось найти пользователя с e-mail: " + email);
                }

                emailUserPassUser.execute(true, session, new DataObject(expertId,  customUser));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Ошибка при попытке выслать напомиание пароля.", e);
        }
    }

    private class VoteObjects {

        DataObject expertObj;
        DataObject voteObj;
        DataObject projectObj;

        private VoteObjects(DataSession session, String svoteId) throws SQLException {

            Integer[] ids = BaseUtils.decode(2, svoteId);

            Integer voteId = ids[0], expertId = ids[1];
//            Integer expertId = (Integer) expertLogin.read(session, new DataObject(login, StringClass.get(30)));
//            if (expertId == null) {
//                throw new RuntimeException("Не удалось найти пользователя с логином " + login);
//            }

            voteObj = new DataObject(voteId, vote);

            Integer projectId = (Integer) projectVote.read(session, voteObj);
            if (projectId == null) {
                throw new RuntimeException("Не удалось найти проект для заседания с идентификатором " + voteId);
            }

            expertObj = new DataObject(expertId, expert);

            Boolean inVote = (Boolean) inNewExpertVote.read(session, expertObj, voteObj);
            if (inVote == null || !inVote) {
                throw new RuntimeException("Эксперт с идентификатором " + expertId + " не назначен на заседание с идентификатором " + voteId);
            }

            projectObj = new DataObject(projectId, project);
        }
    }

    public class GenerateDocumentsActionProperty extends ActionProperty {

        private final ClassPropertyInterface projectInterface;
        private final ClassPropertyInterface documentTemplateInterface;

        public GenerateDocumentsActionProperty() {
            super(genSID(), "Сгенерировать документы", new ValueClass[]{project, documentTemplate});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            projectInterface = i.next();
            documentTemplateInterface = i.next();
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            throw new RuntimeException("no need");
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject projectObject = keys.get(projectInterface);
            DataObject documentTemplateObject = keys.get(documentTemplateInterface);

            Query<String, String> query = new Query<String, String>(Collections.singleton("key"));
            query.and(documentTemplateDocumentTemplateDetail.getExpr(session.modifier, query.mapKeys.get("key")).compare(documentTemplateObject.getExpr(), Compare.EQUALS));
            query.properties.put("documentType", typeDocument.getExpr(session.modifier, query.mapKeys.get("key")));
            query.properties.put("languageDocument", languageDocument.getExpr(session.modifier, query.mapKeys.get("key")));

            for (Map<String, Object> row : query.execute(session.sql, session.env).values()) {
                DataObject documentObject = session.addObject(document, session.modifier);
                projectDocument.execute(projectObject.getValue(), session, documentObject);
                typeDocument.execute(row.get("documentType"), session, documentObject);
                languageDocument.execute(row.get("languageDocument"), session, documentObject);
            }
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
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            DataObject projectObject = keys.get(projectInterface);

            // считываем всех экспертов, которые уже голосовали по проекту
            Query<String, String> query = new Query<String, String>(Collections.singleton("key"));
            query.and(doneProjectExpert.getExpr(session.modifier, projectObject.getExpr(), query.mapKeys.get("key")).getWhere());
            query.properties.put("vote", voteProjectExpert.getExpr(session.modifier, projectObject.getExpr(), query.mapKeys.get("key")));

            Map<DataObject, DataObject> previousResults = new HashMap<DataObject, DataObject>();
            for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(session.sql, session.env, baseClass).entrySet()) {
                previousResults.put(row.getKey().get("key"), (DataObject) row.getValue().get("vote"));
            }

            // считываем всех неголосовавших экспертов из этого кластера
            query = new Query<String, String>(Collections.singleton("key"));
            query.and(clusterExpert.getExpr(session.modifier, query.mapKeys.get("key")).compare(clusterProject.getExpr(session.modifier, projectObject.getExpr()), Compare.EQUALS));
            query.and(disableExpert.getExpr(session.modifier, query.mapKeys.get("key")).getWhere().not());
            query.and(voteResultProjectExpert.getExpr(session.modifier, projectObject.getExpr(), query.mapKeys.get("key")).getWhere().not());

            query.properties.put("in", inProjectExpert.getExpr(session.modifier, projectObject.getExpr(), query.mapKeys.get("key")));

            // получаем два списка - один, которые уже назначались на проект, другой - которые нет
            List<DataObject> expertNew = new ArrayList<DataObject>();
            List<DataObject> expertVoted = new ArrayList<DataObject>();
            for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(session.sql, session.env, baseClass).entrySet()) {
                if (row.getValue().get("in").getValue() != null) // эксперт уже голосовал
                    expertVoted.add(row.getKey().get("key"));
                else
                    expertNew.add(row.getKey().get("key"));
            }

            Integer required = nvl((Integer) requiredQuantity.read(session), 0) - previousResults.size();
            if (required > expertVoted.size() + expertNew.size()) {
                actions.add(new MessageClientAction("Недостаточно экспертов по кластеру", "Генерация заседания"));
                return;
            }

            // создаем новое заседание
            DataObject voteObject;
            if (form == null)
                voteObject = session.addObject(vote, session.modifier);
            else
                voteObject = executeForm.form.addObject(vote);
            projectVote.execute(projectObject.object, session, session.modifier, voteObject);

            // копируем результаты старых заседаний
            for (Map.Entry<DataObject, DataObject> row : previousResults.entrySet()) {
                inExpertVote.execute(true, session, row.getKey(), voteObject);
                oldExpertVote.execute(true, session, row.getKey(), voteObject);
                LP[] copyProperties = new LP[] {dateExpertVote, voteResultExpertVote, inClusterExpertVote,
                                                innovativeExpertVote, foreignExpertVote, innovativeCommentExpertVote,
                                                competentExpertVote, completeExpertVote, completeCommentExpertVote};
                for (LP property : copyProperties) {
                    property.execute(property.read(session, row.getKey(), row.getValue()), session, row.getKey(), voteObject);
                }
            }

            // назначаем новых экспертов - сначала, которые не голосовали еще, а затем остальных
            Random rand = new Random();
            while (required > 0) {
                if (!expertNew.isEmpty())
                    inExpertVote.execute(true, session, expertNew.remove(rand.nextInt(expertNew.size())), voteObject);
                else
                    inExpertVote.execute(true, session, expertVoted.remove(rand.nextInt(expertVoted.size())), voteObject);
                required--;
            }
        }

        @Override
        public Set<Property> getChangeProps() {
            return BaseUtils.toSet(projectVote.property, inExpertVote.property);
        }
    }

    public String getDisplayName() throws RemoteException {
        return "Skolkovo";
    }

    @Override
    public byte[] getMainIcon() throws RemoteException {
        InputStream in = SkolkovoBusinessLogics.class.getResourceAsStream("/images/sk_icon.jpg");
        try {
            try {
                return IOUtils.readBytesFromStream(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            logger.error("Не могу прочитать icon-файл.", e);
            return null;
        }
    }

    @Override
    public byte[] getLogo() throws RemoteException {
        InputStream in = SkolkovoBusinessLogics.class.getResourceAsStream("/images/sk_logo.jpg");
        try {
            try {
                return IOUtils.readBytesFromStream(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            logger.error("Не могу прочитать splash-картинку.", e);
            return null;
        }
    }
}
