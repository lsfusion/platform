package skolkovo;

//import com.smartgwt.client.docs.Files;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfCopyFields;
import com.lowagie.text.pdf.PdfReader;
import jasperapi.PdfUtils;
import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.FormEventType;
import platform.interop.PropertyEditType;
import platform.interop.action.MessageClientAction;
import platform.interop.action.OpenFileClientAction;
import platform.interop.form.layout.ContainerType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.Settings;
import platform.server.classes.*;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.PartitionType;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.window.ToolBarNavigatorWindow;
import platform.server.form.window.TreeNavigatorWindow;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.CustomReadValueActionProperty;
import platform.server.logics.property.actions.UserActionProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.mail.AttachmentFormat;
import platform.server.session.DataSession;
import skolkovo.actions.CopyProjectActionProperty;
import skolkovo.actions.ExportExpertsActionProperty;
import skolkovo.actions.ExportProjectDocumentsActionProperty;
import skolkovo.actions.ImportProjectsActionProperty;

import javax.mail.internet.MimeMessage;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static platform.base.BaseUtils.nvl;

/**
 * User: DAle
 * Date: 24.05.11
 * Time: 18:21
 */


public class SkolkovoLogicsModule extends LogicsModule {
    private final SkolkovoBusinessLogics BL;
    private LCP incrementVote;

    public SkolkovoLogicsModule(BaseLogicsModule<SkolkovoBusinessLogics> baseLM, SkolkovoBusinessLogics BL) {
        super("SkolkovoLogicsModule");
        setBaseLogicsModule(baseLM);
        this.BL = BL;
    }

    private LCP dateResultForesightCheckProject;
    private LCP userResultForesightCheckProject;
    private LCP nameResultForesightCheckProject;
    private LCP sidResultForesightCheckProject;
    private LCP nameUserResultForesightCheckProject;
    private LCP useAllClusterExpertsProject, useAllClusterExperts, useAllClusterExpertsUnionProject;
    private LCP inExpertVoteDateFromDateTo;
    private LCP quantityInExpertDateFromDateTo;
    private LCP doneClusterExpertVoteDateFromDateTo;
    private LCP quantityDoneClusterExpertDateFromDateTo;
    private LCP inClusterExpertVoteDateFromDateTo;
    private LCP quantityInClusterExpertDateFromDateTo;

    private LCP emailClaimerAcceptedHeaderVote;
    private LCP emailClaimerNameVote;
    private LCP emailClaimerRejectedHeaderVote;
    private LCP emailClaimerAcceptedHeaderProject;
    private LAP emailAcceptedProjectEA;
    private LAP emailAcceptedProject;
    public LCP sidProject;
    public LCP sidToProject;
    public LCP nameNativeToCluster;
    public LCP nameNativeCluster;
    public LCP dateProject;
    public LCP dateJoinProject;
    public LCP dateDataProject;
    public LCP dateStatusProject;
    public LCP statusDateProject;
    public LCP dateStatusDataProject;
    public LCP updateDateProject;
    public LCP nativeNumberToPatent;
    public LCP nativeNumberSIDToPatent;

    private LCP resultConferenceExpert, nameResultConferenceExpert;
    public LCP confirmedConferenceExpert;
    public LCP rejectedConferenceExpert;
    private LCP confirmedConference;
    private LCP rejectedConference;
    private LCP totalExperts;
    private LCP textConference;
    private LAP emailConferenceExpertEA;
    private LAP emailConferenceExpert;
    private LCP inConferenceExpert;
    private LCP nameNativeShortAggregateClusterExpert;

    private LAP setCurrentDateDecisionNoticedVote;

    private LAP emailNoticeRejectedVoteEA;
    private LAP emailNoticeRejectedVote;

    private LAP emailNoticeAcceptedStatusVoteEA;
    private LAP emailNoticeAcceptedStatusVote;

    private LAP emailNoticeAcceptedPreliminaryVoteEA;
    private LAP emailNoticeAcceptedPreliminaryVote;

    public ConcreteCustomClass project;
    public ConcreteCustomClass expert;
    public ConcreteCustomClass cluster;
    public ConcreteCustomClass clusterUser;
    public ConcreteCustomClass foresight;
    public ConcreteCustomClass claimer;
    public ConcreteCustomClass claimerExpert;
    ConcreteCustomClass documentTemplate;
    ConcreteCustomClass documentAbstract;
    ConcreteCustomClass documentTemplateDetail;
    ConcreteCustomClass document;

    public ConcreteCustomClass nonRussianSpecialist;
    public ConcreteCustomClass academic;
    public ConcreteCustomClass patent;

    public ConcreteCustomClass specialist;
    public ConcreteCustomClass research;
    public ConcreteCustomClass publications;
    public ConcreteCustomClass analogues;
    public ConcreteCustomClass commercialization;
    public ConcreteCustomClass objectives;
    public ConcreteCustomClass mileStone;
    public ConcreteCustomClass mileStoneYear;

    private ConcreteCustomClass expertConference;
    private StaticClass expertConferenceResult;

    AbstractCustomClass vote;

    ConcreteCustomClass voteR1;
    ConcreteCustomClass voteR2;

    public StaticCustomClass projectType;
    public StaticCustomClass projectAction;
    StaticCustomClass language;
    StaticCustomClass documentType;
    public StaticCustomClass ownerType;
    public StaticCustomClass typeMileStone;

    StaticCustomClass voteResult;
    StaticCustomClass projectStatus;
    StaticCustomClass typeProjectStatus;

    StaticCustomClass formalControlResult;
    StaticCustomClass foresightCheckResult;
    ConcreteCustomClass formalControl;
    StaticCustomClass legalCheckResult;
    StaticCustomClass originalDocsCheckResult;
    public StaticCustomClass projectSchedule;
    ConcreteCustomClass legalCheck;
    ConcreteCustomClass originalDocsCheck;

    AbstractCustomClass application;
    ConcreteCustomClass applicationPreliminary;
    ConcreteCustomClass applicationStatus;

    AbstractGroup projectInformationGroup;
    AbstractGroup additionalInformationGroup;
    AbstractGroup innovationGroup;
    AbstractGroup sourcesFundingGroup;
    AbstractGroup nonReturnFundingGroup;
    AbstractGroup equipmentGroup;
    AbstractGroup projectDocumentsGroup;
    AbstractGroup executiveSummaryGroup, applicationFormGroup, techDescrGroup, roadMapGroup, resolutionIPGroup;
    AbstractGroup minutesOfMettingGroup, writtenConsentGroup;
    AbstractGroup projectStatusGroup;
    AbstractGroup projectOptionsGroup;
    AbstractGroup translateActionGroup;
    AbstractGroup translationGroup;
    AbstractGroup withdrawnGroup;
    AbstractGroup projectTranslationsGroup;
    AbstractGroup projectOtherClusterGroup;
    AbstractGroup consultingCenterGroup;
    AbstractGroup consultingCenterStatGroup;

    AbstractGroup voteResultGroup;
    AbstractGroup voteResultCheckGroup;
    AbstractGroup voteResultCommentGroup;
    AbstractGroup voteResultLengthGroup;
    AbstractGroup formalControlResultGroup;
    AbstractGroup legalCheckResultGroup;
    AbstractGroup registerGroup;
    AbstractGroup originalDoscCheckGroup;

    AbstractGroup commentExpertiseGroup;
    AbstractGroup changeLegalCheckGroup;

    AbstractGroup contactGroup;
    AbstractGroup documentGroup;
    AbstractGroup legalDataGroup;
    AbstractGroup claimerInformationGroup;
    AbstractGroup teamGroup;
    AbstractGroup problemGroup;
    AbstractGroup commercializationGroup;
    AbstractGroup analoguesGroup;
    AbstractGroup historyGroup;
    AbstractGroup projectmissionGroup;
    AbstractGroup publicationsGroup;
    AbstractGroup descGroup;

    AbstractGroup expertResultGroup;
    AbstractGroup importGroup;
    public StaticCustomClass projectMission;

    @Override
    public void initModuleDependencies() {
        setRequiredModules(Arrays.asList("System", "Country"));
    }

    @Override
    public void initModule() {
    }

    @Override
    public void initClasses() {
        initBaseClassAliases();

        projectType = addStaticClass("projectType", "Тип проекта",
                new String[]{"comparable", "surpasses", "russianbenchmark", "certainadvantages", "significantlyoutperforms", "nobenchmarks"},
                new String[]{"сопоставим с существующими российскими аналогами или уступает им", "превосходит российские аналоги, но уступает лучшим зарубежным аналогам",
                        "Является российским аналогом востребованного зарубежного продукта/технологии", "Обладает отдельными преимуществами над лучшими мировыми аналогами, но в целом сопоставим с ними",
                        "Существенно превосходит все существующие мировые аналоги", "Не имеет аналогов, удовлетворяет ранее не удовлетворенную потребность и создает новый рынок"});

        projectAction = addStaticClass("projectAction", "Тип заявки",
                new String[]{"preliminary", "status"},
                new String[]{"На предварительную экспертизу", "На статус участника"});

        projectMission = addStaticClass("projectMission", "Цель проекта",
                       new String[]{"development", "investigation"},
                       new String[]{"Коммерциализация продукта", "Осуществление исследований"});
        typeMileStone = addStaticClass("typeMileStone", "Раздел дорожной карты",
                       new String[]{"research and development", "product Creation and Production", "plan on hiring", "licensing", "promotion", "selling"},
                       new String[]{"Разработка", "Создание продукта", "План по найму", "Лицензирование", "Продвижение", "Продажи"});

        ownerType = addStaticClass("ownerType", "Тип правообладателя",
                new String[]{"employee", "participant", "thirdparty"},
                new String[]{"Работником организации", "Участником организации", "Третьим лицом"});

        patent = addConcreteClass("patent", "Патент", baseClass);
        academic = addConcreteClass("academic", "Научные кадры", baseClass);
        nonRussianSpecialist = addConcreteClass("nonRussianSpecialist", "Иностранный специалист", baseClass);

        specialist = addConcreteClass("specialist", "Член команды", baseClass);
        research = addConcreteClass("research", "Исследования", baseClass);
        publications = addConcreteClass("publications", "Научные публикации", baseClass);
        commercialization = addConcreteClass("commercialization", "Основные направления коммерциализации", baseClass);
        analogues = addConcreteClass("analogues", "Аналоги проекта", baseClass);
        objectives = addConcreteClass("objectives", "Цели проекта", baseClass);

        project = addConcreteClass("project", "Проект", baseLM.multiLanguageNamed, baseLM.transaction);
        expert = addConcreteClass("expert", "Эксперт", baseLM.customUser);
        cluster = addConcreteClass("cluster", "Кластер", baseLM.multiLanguageNamed, baseLM.emailObject);
        foresight = addConcreteClass("foresight", "Форсайт", baseLM.multiLanguageNamed);

        clusterUser = addConcreteClass("clusterUser", "Сотрудник кластера", baseLM.customUser);

        claimer = addConcreteClass("claimer", "Заявитель", baseLM.multiLanguageNamed, baseLM.emailObject);
        claimer.dialogReadOnly = false;

        claimerExpert = addConcreteClass("claimerExpert", "Заявитель/эксперт", claimer, expert);

        documentTemplate = addConcreteClass("documentTemplate", "Шаблон документов", baseClass.named);

        documentAbstract = addConcreteClass("documentAbstract", "Документ (абстр.)", baseClass);

        documentTemplateDetail = addConcreteClass("documentTemplateDetail", "Документ (прототип)", documentAbstract);

        document = addConcreteClass("document", "Документ", documentAbstract);

        vote = addAbstractClass("vote", "Заседание", baseClass, baseLM.transaction);

        voteR1 = addConcreteClass("voteR1", "Заседание (регл. 1)", vote);
        voteR2 = addConcreteClass("voteR2", "Заседание (регл. 2)", vote);

        language = addStaticClass("language", "Язык",
                new String[]{"russian", "english"},
                new String[]{"Русский", "Английский"});

        voteResult = addStaticClass("voteResult", "Результат заседания",
                new String[]{"refused", "connected", "voted"},
                new String[]{"Отказался", "Аффилирован", "Проголосовал"});

        projectStatus = addStaticClass("projectStatus", "Статус проекта",
                new String[]{"unknown", "needTranslation", "needExtraVote", "inProgress", "accepted", "rejected",
                     "notEnoughDocsForPreliminary", "notEnoughDocsForStatus", "noExperts", "noCluster", "positiveFCResult", "negativeLCStatusResult", "negativeLCPreliminaryResult","positiveLCResult",
                     "registered", "repeated", "withdrawn", "overdueFC", "overdueLC", "positiveFSResult",
                     "issuedVoteDocs", "applyStatus", "sentRejected", "sentPreliminaryAccepted", "sentStatusAccepted", "inProgressRepeat",
                     "haveStatus", "notEnoughOriginalDocs", "overdueOriginalDocs", "appliedOriginalDocs", "sentForSignature", "signed", "sentToFinDep",
                     "submittedToRegister", "preparedCertificate", "certified", "companyHasLostStatus"},
                new String[]{"Неизвестный статус", "Направлена на перевод", "Требуется заседание (повторное)", "Идет заседание", "Оценен положительно", "Оценен отрицательно",
                     "Неполный перечень документов (на экспертизу)","Неполный перечень документов (на статус)",  "Отсутствует перечень экспертов", "Не соответствует направлению", "Направлена на юридическую проверку", "Не прошла юридическую проверку (на статус)", "Не прошла юридическую проверку (на предв.экспертизу)", "Прошла юридическую проверку",
                     "Зарегистрирована", "Подана повторно", "Отозвана заявителем", "Не исправлена в срок (ФЭ)", "Не исправлена в срок (ЮП)", "Прошла проверку на соответствие форсайту",
                     "Оформление документов по заседанию", "Подана заявка на статус", "Отправлено отрицательное решение", "Отправлено положительное решение предв.экспертизы", "Отправлено положительное решение экспертизы на статус", "Идет заседание (повторное)",
                     "Оставлена без рассмотрения", "Неполный пакет оригиналов документов", "Пакет оригиналов документов не пополнен в срок", "Предоставлены документы в бумажном виде", "Решение передано на подпись", "Решение подписано", "Документы переданы в Финансовый департамент",
                     "Внесен в реестр участников", "Подготовлено свидетельство участника", "Выдано свидетельство участника", "Компания утратила статус участника"});

        typeProjectStatus = addStaticClass("typeProjectStatus", "Тип статуса проекта",
                new String[]{"inConsideration", "requestMaterials", "positiveResult", "negativeResult"},
                new String[]{"На рассмотрении в Фонде", "Запрос материалов у Заявителя", "Положительный результат", "Отрицательный результат"});

        documentType = addStaticClass("documentType", "Тип документа",
                new String[]{"application", "resume", "techdesc", "forres", "ipres", "roadmap", "passport", "statement"},
                new String[]{"Анкета", "Резюме", "Техническое описание", "Резюме иностранного специалиста", "Заявление IP", "Дорожная карта", "Документ, удостоверяющий личность участников Команды проекта", "Заявление участника Команды проекта"});

        formalControlResult = addStaticClass("formalControlResult", "Решение формальной экспертизы",
                new String[]{"notEnoughDocuments", "noListOfExperts", "notSuitableCluster", "repeatedFC", "positiveFormalResult"},
                new String[]{"Неполный перечень документов", "Отсутствует перечень экспертов", "Не соответствует направлению", "Подана повторно", "Прошла формальную экспертизу"});

        foresightCheckResult = addStaticClass("foresightCheckResult", "Решение проверки на форсайты",
                new String[]{"negativeForesightCheckResult", "positiveForesightCheckResult"},
                new String[]{"Не прошла проверку на форсайты", "Прошла проверку на форсайты"});

        legalCheckResult = addStaticClass("legalCheckResult", "Решение юридической проверки",
                new String[]{"negativeLegalCheckResult", "positiveLegalCheckResult"},
                new String[]{"Не прошла юридическую проверку", "Прошла юридическую проверку"});

        originalDocsCheckResult = addStaticClass("originalDocsCheckResult", "Проверка оригиналов документов",
                new String[]{"notCompleteOriginalDocsPacket", "completeOriginalDocsPacket"},
                new String[]{"Подан неполный пакет документов", "Подан полный пакет документов"});

        formalControl = addConcreteClass("formalControl", "Формальная экспертиза", baseClass);
        legalCheck = addConcreteClass("legalCheck", "Юридическая проверка", baseClass);
        originalDocsCheck = addConcreteClass("originalDocsCheck", "Проверка оригиналов документов", baseLM.transaction);

        mileStone = addConcreteClass("mileStone", "Квартал", baseClass);
        mileStoneYear = addConcreteClass("mileStoneYear", "Год", baseClass);

        application = addAbstractClass("application", "Заявка", baseClass);
        applicationPreliminary = addConcreteClass("applicationPreliminary", "Заявка на предварительную экспертизу", application);
        applicationStatus = addConcreteClass("applicationStatus", "Заявка на статус участника", application);

        projectSchedule = addStaticClass("projectSchedule", "Регламент проекта",
                new String[]{"R1", "R2"},
                new String[]{"R1", "R2"});

        expertConference = addConcreteClass("expertConference", "Конференция экспертов", baseLM.transaction, baseClass.named);
        expertConferenceResult = addStaticClass("expertConferenceResult", "Участие в конференции",
                new String[]{"confirmedConference", "rejectedConference"},
                new String[]{"Подтвердил участие", "Отказался от участия"});
    }

    @Override
    public void initTables() {
        addTable("property", baseLM.property);
        addTable("multiLanguageNamed", baseLM.multiLanguageNamed);
        addTable("project", project);
        addTable("expert", expert);
        addTable("cluster", cluster);
        addTable("claimer", claimer);
        addTable("vote", vote);
        addTable("patent", patent);
        addTable("academic", academic);
        addTable("nonRussianSpecialist", nonRussianSpecialist);
        addTable("documentAbstract", documentAbstract);
        addTable("document", document);
        addTable("expertVote", expert, vote);
        addTable("formalControl", formalControl);
        addTable("legalCheck", legalCheck);
        addTable("originalDocsCheck", originalDocsCheck);
        addTable("application", application);
        addTable("foresight", foresight);
        addTable("specialist", specialist);
        addTable("commercialization", commercialization);
        addTable("analogues", analogues);
        addTable("publications", publications);
        addTable("research", research);
        addTable("objectives", objectives);
        addTable("mileStone", mileStone);
        addTable("mileStoneYear", mileStoneYear);
        addTable("projectForesight", project, foresight);
        addTable("projectCluster", project, cluster);
        addTable("clusterExpert", cluster, expert);
        addTable("expertForesight", expert, foresight);
        addTable("expertConferenceExpert", expertConference, expert);
        addTable("typeMileStoneMileStoneMileStoneYear", typeMileStone, mileStone, mileStoneYear);
        addTable("projectLanguageDocumentType", project, language, documentType);
        addTable("languageDocumentType", language, documentType);

    }

    @Override
    public void initGroups() {
        initBaseGroupAliases();

        Settings.instance.setLimitExclusiveCount(5);
//        Settings.instance.setEditLogicalOnSingleClick(true);
//        Settings.instance.setEditActionOnSingleClick(true);

        contactGroup = addAbstractGroup("contactGroup", "Контакты организации", publicGroup);

        documentGroup = addAbstractGroup("documentGroup", "Юридические документы", publicGroup);

        legalDataGroup = addAbstractGroup("legalDataGroup", "Юридические данные", publicGroup);

        claimerInformationGroup = addAbstractGroup("claimerInformationGroup", "Информация о заявителе", publicGroup);


        projectInformationGroup = addAbstractGroup("projectInformationGroup", "Информация по проекту", baseGroup);

        additionalInformationGroup = addAbstractGroup("additionalInformationGroup", "Доп. информация по проекту", publicGroup);

        innovationGroup = addAbstractGroup("innovationGroup", "Инновация", baseGroup);

        sourcesFundingGroup = addAbstractGroup("sourcesFundingGroup", "Источники финансирования", baseGroup);
        nonReturnFundingGroup = addAbstractGroup("nonReturnFundingGroup", "Безвозвратные", sourcesFundingGroup);

        equipmentGroup = addAbstractGroup("equipmentGroup", "Оборудование", baseGroup);

        descGroup = addAbstractGroup("descGroup", "Описание", baseGroup);
        teamGroup = addAbstractGroup("teamGroup", "Команда", baseGroup);
        problemGroup = addAbstractGroup("problemGroup", "Проблемы", baseGroup);
        commercializationGroup = addAbstractGroup("commercializationGroup", "Коммерциализация", baseGroup);
        analoguesGroup = addAbstractGroup("analoguesGroup", "Конкурренты", baseGroup);
        historyGroup  = addAbstractGroup("historyGroup", "История проекта", baseGroup);
        projectmissionGroup = addAbstractGroup("projectmissionGroup", "Цели и задачи проекта", baseGroup);
        publicationsGroup = addAbstractGroup("publicationsGroup", "Публикации членов", baseGroup);


        projectDocumentsGroup = addAbstractGroup("projectDocumentsGroup", "Документы", baseGroup);
        applicationFormGroup = addAbstractGroup("applicationFormGroup", "Анкета", projectDocumentsGroup);
        executiveSummaryGroup = addAbstractGroup("executiveSummaryGroup", "Резюме проекта", projectDocumentsGroup);
        techDescrGroup = addAbstractGroup("techDescrGroup", "Техническое описание", projectDocumentsGroup);
        roadMapGroup = addAbstractGroup("roadMapGroup", "Дорожная карта", projectDocumentsGroup);
        resolutionIPGroup = addAbstractGroup("resolutionIPGroup", "Заявление IP", projectDocumentsGroup);
        minutesOfMettingGroup = addAbstractGroup("minutesOfMettingGroup", "Протокол заседания", baseGroup);
        writtenConsentGroup  = addAbstractGroup("writtenConsentGroup", "Письменое согласие заявителя", baseGroup);

        projectStatusGroup = addAbstractGroup("projectStatusGroup", "Текущий статус проекта", baseGroup);

        projectOptionsGroup = addAbstractGroup("projectOptionsGroup", "Параметры проекта", baseGroup);

        translateActionGroup = addAbstractGroup("translateActionGroup", "Перевод", baseGroup);

        translationGroup = addAbstractGroup("translationGroup", "Перевод материалов", baseGroup);
        withdrawnGroup = addAbstractGroup("withdrawnGroup", "Отзыв заявки", baseGroup);

        projectTranslationsGroup = addAbstractGroup("projectTranslationsGroup", "Переведено", baseGroup);

        projectOtherClusterGroup = addAbstractGroup("projectOtherClusterGroup", "Иной кластер", baseGroup);

        consultingCenterGroup = addAbstractGroup("consultingCenterGroup", "Консультативный центр", baseGroup);

        consultingCenterStatGroup = addAbstractGroup("consultingCenterStatGroup", "Консультативный центр", baseGroup);

        voteResultGroup = addAbstractGroup("voteResultGroup", "Результаты голосования", publicGroup, false);

        expertResultGroup = addAbstractGroup("expertResultGroup", "Статистика по экспертам", publicGroup);

        importGroup = addAbstractGroup("importProjectsGroup", "Импорт", actionGroup);

        voteResultCheckGroup = addAbstractGroup("voteResultCheckGroup", "Результаты голосования (выбор)", voteResultGroup, true);
        voteResultCommentGroup = addAbstractGroup("voteResultCommentGroup", "Результаты голосования (комментарии)", voteResultGroup, true);
        voteResultLengthGroup = addAbstractGroup("voteResultLengthGroup", "Результаты голосования (длина)", publicGroup, true);

        formalControlResultGroup = addAbstractGroup("formalControlResultGroup", "Решения формальной экспертизы", publicGroup);
        legalCheckResultGroup = addAbstractGroup("legalCheckResultGroup", "Решения юридической проверки", publicGroup);
        registerGroup = addAbstractGroup("registerGroup", "Оформление свидетельства участника", publicGroup);
        originalDoscCheckGroup = addAbstractGroup("originalDoscCheckGroup", "Проверка оригиналов документов", publicGroup);

        commentExpertiseGroup = addAbstractGroup("commentExpertiseGroup", "Опыт в областях", publicGroup);

        changeLegalCheckGroup = addAbstractGroup("changeLegalCheckGroup", "Изменение типа заявки", publicGroup); 
    }

    public LCP negative;
    public LCP nameNative;
    public LCP nameForeign;
    public LCP nameNativeShort;
    public LCP nameNativeShortAggregateClusterProject;
    public LCP firmNameNativeClaimer;
    public LCP firmNameForeignClaimer;
    public LCP phoneClaimer;
    public LCP addressClaimer;
    public LCP postAddressClaimer;
    public LCP siteClaimer;
    public LCP emailClaimer;
    public LCP emailFirmClaimer;
    public LCP emailClaimerProject;
    public LCP statementClaimer;
    public LCP constituentClaimerProject;
    public LCP extractClaimerProject;
    public LCP statementClaimerProject;
    LAP loadStatementClaimer;
    LAP openStatementClaimer;
    public LCP constituentClaimer;
    LAP loadConstituentClaimer;
    LAP openConstituentClaimer;
    public LCP extractClaimer;
    LAP loadExtractClaimer;
    LAP openExtractClaimer;
    public LCP OGRNClaimer;
    public LCP INNClaimer;
    public LCP projectScheduleProject;
    LCP regulationsProject;
    public LCP nameRegulationsProject;
    LCP isR2Project, isR1Project;
    LCP revisionVote;
    LCP projectVote, claimerVote, nameNativeProjectVote, nameForeignProjectVote;
    LCP quantityVoteProject;
    LCP quantityDefaultVoteProject;
    LCP quantitySubDefaultVoteProject;
    LCP quantityVoteProjectCluster;
    LCP quantityClusterVotedProject;
    public LCP dataDocumentNameExpert, documentNameExpert;
    public LCP emailExpert;
    public LAP generateSIDExpert;
    public LCP clusterClusterUser, nameClusterClusterUser, clusterCurrentUser;
    public LCP inClusterCurrentUserProject;
    LCP clusterExpert, nameNativeClusterExpert, nameForeignClusterExpert, nameNativeShortClusterExpert;
    public LCP inExpertForesight, commentExpertForesight, quantityInForesightExpert, quantityInExpertForesight;
    public LCP inProjectForesightExpert, quantityForesightProjectExpert;
    public LCP clusterInExpertForesight;
    public LCP isScientificExpert, isTechnicalExpert, isBusinessExpert;
    public LCP commentScientificExpert, commentTechnicalExpert, commentBusinessExpert;
    public LCP expertiseExpert, grantExpert;
    LCP profileUpdateDateExpert;
    LCP profileBlockedExpert;
    public LCP primClusterExpert, extraClusterExpert, inClusterExpert;
    LCP quantityInClusterExpert;
    LCP clusterInExpertVote;
    public LCP inProjectCluster;
    public LCP inClaimerProjectCluster;
    public LCP inProjectForesight;
    public LCP isRootForesight;
    public LCP isRootInProjectForesight;
    LCP isInClusterProjectForesight;
    LCP clusterVote, nameNativeClusterVote, nameForeignClusterVote;
    LCP projectCluster;
    LCP inTestCluster;
    LCP quantityClusterProject;
    public LCP clusterForesight, sidForesight;
    LCP foresightSID;
    LCP nameNativeClusterForesight, nameForeignClusterForesight, nameNativeShortClusterForesight;
    LCP quantityForesightProject;
    LCP maxForesightProjectCluster, nameMaxForesightVote, maxForesightVote, nMaxForesightVote;
    LCP maxForesightProject, nameMaxForesightLegalChek, maxForesightLegalChek;
    LCP isPrevVoteVote;
    LCP countPrevVote;
    public LCP claimerProject;
    LCP nameNativeUnionManagerProject, nameForeignUnionManagerProject;
    LCP nameNativeJoinClaimerProject, nameForeignJoinClaimerProject;
    LCP nameNativeCorrectManagerProject, nameForeignCorrectManagerProject;
    LCP nameNativeCorrectHighlightClaimerProject, nameForeignCorrectHighlightClaimerProject;
    //LCP nameNativeJoinClaimerProject;
    LCP nameNativeCorrectClaimer, nameForeignCorrectClaimer;
    LCP nameNativeClaimer;
    LCP nameNativeCorrectClaimerProject, nameForeignCorrectClaimerProject;
    public LCP nameNativeClaimerProject, nameForeignClaimerProject;
    //LCP nameForeignClaimerProject;
    //LCP nameForeignJoinClaimerProject;
    LCP emailDocuments, emailIO, emailExperts, emailPresident;
    LCP isR1ProjectVote;
    LCP isR2ProjectVote;
    LCP emailFondFC;
    LCP emailForesightLC;
    LCP emailFondTransferred;
    public LCP emailToExpert;
    public LCP sidExpert, sidCluster, sidToExpert;

    LCP nameNativeJoinClaimerVote, nameForeignJoinClaimerVote;
    LCP nameNativeClaimerVote, nameForeignClaimerVote;
    public LCP nameNativeGenitiveManagerProject;
    LCP nameGenitiveManagerProject;
    public LCP nameNativeDativusManagerProject;
    LCP nameDativusManagerProject;
    public LCP nameNativeAblateManagerProject;
    LCP nameAblateManagerProject;

    public LCP nameNativeJoinClaimer, nameForeignJoinClaimer;
    public LCP nameForeignClaimer;
    LCP nameGenitiveClaimerProject;
    LCP nameDativusClaimerProject;
    LCP nameAblateClaimerProject;
    LCP nameGenitiveClaimerVote;
    LCP nameDativusClaimerVote;
    LCP nameAblateClaimerVote;
    LCP firmNameNativeClaimerProject, firmNameForeignClaimerProject, phoneClaimerProject, OGRNClaimerProject;
    LCP addressClaimerProject, postAddressClaimerProject, siteClaimerProject, emailFirmClaimerProject, INNClaimerProject;

    LCP documentTemplateDocumentTemplateDetail;

    LCP projectDocument, nameNativeProjectDocument;
    LCP fileDocument;
    LAP loadFileDocument;
    LAP openFileDocument;

    LCP fileDecisionVote;
    LAP attachProtocolDecisionVote;
    LAP loadFileDecisionVote;
    LAP openFileDecisionVote;

    LCP fileDecisionProject;
    LCP acceptedDecisionProject, rejectedDecisionProject;

    private LCP decisionNoticedVote, dateDecisionNoticedVote;
    private LCP decisionNoticedProject, dateDecisionNoticedProject;
    private LCP acceptedNoticedProject;
    private LCP rejectedNoticedProject;
    private LCP acceptedNoticedStatusProject;
    private LCP acceptedNoticedPreliminaryProject;

    private LCP prevOriginalDocsCheck;
    private LCP datePrevOriginalDocsCheck;

    LCP add2Strings;
    LCP overdueOriginalDocsCheckProject;
    LCP projectOriginalDocsCheck;
    LCP resultOriginalDocsCheck;
    LCP commentOriginalDocsCheck;
    LCP nameResultOriginalDocsCheck;
    LCP dateSubmitOriginalDocsCheck;
    LCP dateTimeOriginalDocsCheck;
    LCP dateFirstSubmitOriginalDocsProject;
    LCP dateOriginalDocsCheck;
    LCP overdueDateOriginalDocsCheck;
    LCP [] maxOriginalDocsCheckProjectProps;
    LCP currentOriginalDocsCheckProject;
    LCP executeOriginalDocsCheckProject;
    LCP dateExecuteOriginalDocsCheckProject;
    LCP resultExecuteOriginalDocsCheckProject;
    LAP setNegativeResultOriginalDocsCheck;
    LAP setPositiveResultOriginalDocsCheck;
    LCP negativeOriginalDocsCheckProject;
    LCP positiveOriginalDocsCheckProject;
    LCP sentForSignatureProject;
    LCP signedProject;
    LCP sentToFinDepProject;
    LCP submittedToRegisterProject;
    LCP preparedCertificateProject;
    LCP certifiedProject;
    LCP dateAppliedOriginalDocsProject;
    LCP dateSentForSignatureProject;
    LCP dateSignedProject;
    LCP dateSentToFinDepProject;
    LCP dateSubmittedToRegisterProject;
    LCP datePreparedCertificateProject;
    LCP dateCertifiedProject;
    LCP withdrawnProject;
    LCP dateWithdrawnProject;

    LCP inDefaultDocumentLanguage;
    LCP inDefaultDocumentExpert;
    LCP inDocumentLanguage;
    LCP inDocumentExpert;

    LCP inExpertVote, oldExpertVote, inNewExpertVote, inOldExpertVote;
    LCP exclExpertVote;
    LCP businessExpertVote;
    LCP dateStartVote, dateEndVote;
    LCP aggrDateEndVote;
    public LCP quantityPreliminaryVoteProject;

    LCP weekStartVote, quantityNewExpertWeek;
    LCP quantityNewWeek;

    LCP openedVote;
    LCP closedVote;
    LCP voteInProgressProject;
    LCP voteInProgressRepeatProject;
    LCP requiredPeriod;
    LCP overduePeriod;
    LCP requiredQuantity;
    LCP requiredBusinessQuantity;
    LCP requiredQuantityVote;
    LCP limitExperts;
    public LCP projectsImportLimit;
    public LCP importOnlyR2Projects;
    LAP voteStartFormVote;
    LAP voteProtocolFormVote;

    LCP dateExpertVote;
    LCP voteResultExpertVote, voteResultCorExpertVote, nameVoteResultExpertVote;
    LCP voteResultNewExpertVote;
    LCP inProjectExpert;
    LCP voteProjectExpert;
    LCP clusterVotedProjectExpert;
    LCP voteResultProjectExpert;
    LCP doneProjectExpert;
    LCP doneExpertVote, doneCorExpertVote, doneNewExpertVote, doneOldExpertVote;
    LCP refusedExpertVote;
    LCP connectedExpertVote;
    LCP expertVoteConnected;
    LCP ipExpertVote;

    LCP inClusterExpertVote, inClusterNewExpertVote;
    LCP innovativeExpertVote, innovativeNewExpertVote;
    LCP innovativeCommentExpertVote;
    LCP foreignExpertVote, foreignNewExpertVote;
    LCP competentExpertVote;
    LCP completeExpertVote;
    LCP completeCommentExpertVote;

    private LCP percentNeeded;
    private LCP percentNeededVote;
    private LCP quantityNeededVote;

    public LCP competitiveAdvantagesExpertVote, competitiveAdvantagesCorExpertVote;
    public LCP commercePotentialExpertVote, commercePotentialCorExpertVote;
    public LCP canBeImplementedExpertVote, canBeImplementedCorExpertVote;
    public LCP haveExpertiseExpertVote, haveExpertiseCorExpertVote;
    public LCP internationalExperienceExpertVote, internationalExperienceCorExpertVote;
    public LCP enoughDocumentsExpertVote, enoughDocumentsCorExpertVote;
    public LCP commentCompetitiveAdvantagesExpertVote, lengthCommentCompetitiveAdvantagesExpertVote;
    public LCP commentCommercePotentialExpertVote, lengthCommentCommercePotentialExpertVote;
    public LCP commentCanBeImplementedExpertVote, lengthCommentCanBeImplementedExpertVote;
    public LCP commentHaveExpertiseExpertVote, lengthCommentHaveExpertiseExpertVote;
    public LCP commentInternationalExperienceExpertVote, lengthCommentInternationalExperienceExpertVote;
    public LCP commentEnoughDocumentsExpertVote, lengthCommentEnoughDocumentsExpertVote;

    LCP quantityInVote;
    LCP quantityInOldVote;
    LCP quantityRepliedVote;
    LCP quantityDoneVote;
    LCP quantityExclVote;
    LCP quantityDoneNewVote;
    LCP quantityDoneOldVote;
    LCP quantityRefusedVote;
    LCP quantityConnectedVote;
    LCP quantityInClusterVote;
    LCP quantityInnovativeVote;
    LCP quantityForeignVote;
    LCP quantityCompetitiveAdvantagesVote;
    LCP quantityCommercePotentialVote;
    LCP quantityCanBeImplementedVote;
    LCP quantityHaveExpertiseVote;
    LCP quantityInternationalExperienceVote;
    LCP quantityEnoughDocumentsVote;
    LCP acceptedInClusterVote;
    LCP acceptedInnovativeVote;
    LCP acceptedForeignVote;
    LCP acceptedCompetitiveAdvantagesVote;
    LCP acceptedCommercePotentialVote;
    LCP acceptedCanBeImplementedVote;
    LCP acceptedHaveExpertiseVote;
    LCP acceptedInternationalExperienceVote;
    LCP acceptedEnoughDocumentsVote;

    LCP acceptedVote;
    LCP acceptedVoteR1;
    LCP acceptedVoteR2;
    LCP succeededVote;
    LCP openedSucceededVote, closedSucceededVote;
    LCP closedAcceptedVote;
    LCP closedRejectedVote;
    LCP closedAcceptedStatusVote, closedAcceptedPreliminaryVote;
    LCP doneExpertVoteDateFromDateTo;
    LCP doneExpertVoteMonthYear;
    LCP quantityDoneExpertDateFromDateTo;
    LCP quantityDoneExpertMonthYear;
    LCP voteSucceededProjectCluster;
    LCP voteOpenedSucceededProject;
    LCP noCurrentVoteProject;
    LCP valuedProjectCluster;
    LCP voteValuedProjectCluster;
    LCP voteLastProject;
    LCP dateStartVoteLastProject, dateEndVoteLastProject;
    LCP acceptedProjectCluster;
    LCP rejectedProjectCluster;
    LCP clusterAcceptedProject;
    LCP acceptedProject;
    LCP rejectedProject;
    LCP valuedProject;
    LCP voteRejectedProject;
    LCP needExtraVoteProject;
    LCP needExtraVoteRepeatProject;

    LCP nameNativeShortFinalClusterApplication;
    LAP emailLetterExpertVoteEA;
    LAP emailLetterExpertVote;
    LAP allowedEmailLetterExpertVote;

    LCP emailClaimerFromAddress;
    LAP emailClaimerVoteEA;
    LCP claimerEmailVote;
    LCP emailClaimerHeaderVote;
    LAP emailClaimerVote;
    LAP emailFondVoteEA;
    LCP emailFondHeaderVote;
    LAP emailFondVote;
    LCP emailFondStartVote;

    LAP emailStartVoteEA;
    LCP emailStartHeaderVote;
    LAP emailStartVote;
    LAP emailProtocolVoteEA;
    LCP emailProtocolHeaderVote;
    LAP emailProtocolVote;
    LAP emailClosedVoteEA;
    LCP emailClosedHeaderVote;
    LAP emailClosedVote;
    LAP emailForesightCheckProjectEA;
    LAP emailNotificationProjectEA;
    LCP emailNotificationHeaderProject;
    LAP emailNotificationProject;
    LAP emailAuthExpertEA;
    LAP emailAuthExpert;
    LAP emailAuthProfileExpertEA;
    LAP emailAuthProfileExpert;
    LAP emailReminderProfileExpertEA;
    LAP emailReminderProfileExpert;
    LCP reminderProfileExpertSubjectLanguage;
    LCP authExpertSubjectLanguage, letterExpertSubjectLanguage;
    LCP authProfileExpertSubjectLanguage;
    LAP emailClaimerFormalControlEA;
    LCP claimerFormalControl, claimerEmailFormalControl, nameNativeJoinClaimerFormalControl, nameForeignJoinClaimerFormalControl, nameNativeClaimerFormalControl, nameNativeUnionManagerFormalControl;
    LCP needEmailClaimerFormalControl;
    LCP resultNoticedFormalControl;
    LCP dateResultNoticedFormalControl;
    LAP setCurrentDateResultNoticedFormalControl;
    LCP emailClaimerHeaderFormalControl;
    LAP emailClaimerFormalControl;
    LAP emailClaimerLegalCheckEA;
    LCP claimerLegalCheck, claimerEmailLegalCheck, nameNativeJoinClaimerLegalCheck, nameForeignJoinClaimerLegalCheck, nameNativeClaimerLegalCheck;
    LCP resultNoticedLegalCheck;
    LCP dateResultNoticedLegalCheck;
    LAP setCurrentDateResultNoticedLegalCheck;
    LCP emailClaimerHeaderLegalCheck;
    LAP emailClaimerLegalCheck;
    LAP emailClaimerLegalCheckProject;
    LCP needNoticeNegativeLegalCheck, needNoticeChangeLegalCheck, needNoticeLegalCheck;
    LAP emailFondFormalControlEA;
    LCP emailFondHeaderFormalControl;
    LAP emailFondFormalControl;
    LAP emailTransferredProjectEA;
    LCP emailTransferredHeaderProject;
    LAP emailTransferredProject;
    LAP emailForesightClaimerProjectEA;
    LCP emailForesightClaimerHeaderProject;
    LAP emailForesightClaimerProject;
    LCP emailBureauTranslation;
    LAP emailBureauTrProjectEA;
    LAP emailBureauTrProject;
    LAP emailNeedVoteProjectEA, emailNeedVoteProject;
    LCP resultNeedVoteProject, dateResultNeedVoteProject, dateOverdueResultNeedVoteProject;
    LAP setCurrentDateResultNeedVoteProject;
    LAP emailClaimerChangeLegalCheckEA;
    LAP setCurrentDateChangeLegalCheck;
    LAP emailClaimerChangeLegalCheck;

    LAP generateDocumentsProjectDocumentType;
    LAP includeDocumentsProject;
    LCP hideIncludeDocumentsProject;
    LAP importProjectSidsAction;
    LAP showProjectsToImportAction;
    LAP showProjectsReplaceToImportAction;
    LAP importProjectsAction;
    LAP openApplicationProjectAction;
    LAP openCompleteApplicationProjectAction;
    LAP exportExpertsAction;
    LAP exportExpertsEscapeAction;
    LAP exportProjectDocumentsAction;
    LAP copyProjectAction;
    LAP generateVoteProject;
    LCP needNameExtraVoteProject;
    LCP hideGenerateVoteProject;
    LAP copyResultsVote;
    LAP includeProjectClusterForesight;
    LAP importIPsExpertVoteAction;

    LCP expertLogin;

    LCP projectStatusProject, nameProjectStatusProject;
    LCP logStatusProject, logNameStatusProject;

    LCP valuedStatusProject;
    LCP certifiedStatusProject;
    LCP foresightCheckStatusProject;
    LCP legalCheckStatusProject;
    LCP formalCheckStatusProject;
    LCP voteStatusProject;
    LCP statusProject;
    public LCP nameStatusProject;
    LCP oficialNameProjectStatus;
    LCP numberProjectStatus;
    LCP statusDataProject;
    LCP statusProjectVote, nameStatusProjectVote;

    LCP projectSucceededClaimer;

    LCP quantityTotalExpert;
    LCP quantityDoneExpert;
    LCP percentDoneExpert;
    LCP percentInClusterExpert;
    LCP percentInnovativeExpert;
    LCP percentForeignExpert;
    LCP percentCompetitiveAdvantagesExpert;
    LCP percentCommercePotentialExpert;
    LCP percentCanBeImplementedExpert;
    LCP percentHaveExpertiseExpert;
    LCP percentInternationalExperienceExpert;
    LCP percentEnoughDocumentsExpert;

    LCP prevDateStartVote, prevDateVote;
    LCP prevClusterVote, nameNativePrevClusterVote;
    LCP dateProjectVote;
    LCP numberNewExpertVote;
    LCP numberOldExpertVote;

    public LCP numberCluster;
    LCP clusterNumber;
    LCP currentClusterProject, firstClusterProject, lastClusterProject, finalClusterProject;
    LCP lastClusterProjectVote, isLastClusterVote;
    public LCP nameNativeFinalClusterProject, nameForeignFinalClusterProject, nameNativeShortFinalClusterProject, emailFinalClusterProject;

    LCP finalClusterProjectVote, nameNativeFinalClusterProjectVote;

    LCP nativeSubstantiationFinalClusterProject, foreignSubstantiationFinalClusterProject;

    LCP languageExpert;
    public LCP nameLanguageExpert;
    LCP languageDocument;
    LCP nameLanguageDocument;
    LCP englishDocument;
    LCP defaultEnglishDocumentType;
    LCP defaultEnglishDocument;
    LCP typeDocument;
    LCP nameTypeDocument;
    LCP postfixDocument;
    LCP hidePostfixDocument;

    LCP localeLanguage;

    LCP quantityMinLanguageDocumentType;
    LCP quantityMaxLanguageDocumentType;
    LCP quantityProjectLanguageDocumentType;
    LCP translateLanguageDocumentType;
    LCP notEnoughProject;
    public LCP inactiveProject;
    public LCP autoGenerateProject;

    LCP nameDocument;

    public LCP nameNativeProject, nameNativeDataProject, nameNativeJoinProject;
    public LCP nameForeignProject, nameForeignDataProject, nameForeignJoinProject;
    public LCP nameNativeManagerProject;
    public LCP nameForeignManagerProject;
    public LCP nativeProblemProject;
    public LCP foreignProblemProject;
    public LCP nativeInnovativeProject;
    public LCP foreignInnovativeProject;
    public LCP nameForeignProjectType;
    public LCP projectTypeProject;
    LCP nameNativeProjectTypeProject;
    LCP nameForeignProjectTypeProject;
    public LCP projectActionProject;
    public LCP nameProjectActionProject;
    LCP projectActionVote;
    LCP nameProjectActionVote;
    LCP isStatusVote;
    LCP isPreliminaryVote;
    LCP isStatusProject;
    LCP isPreliminaryProject;
    public LCP nativeSubstantiationProjectType;
    public LCP foreignSubstantiationProjectType;
    public LCP nativeSubstantiationProjectCluster;
    public LCP foreignSubstantiationProjectCluster;
    public LCP isOtherClusterProject;
    LCP hideIsOtherClusterProject;
    public LCP nativeSubstantiationOtherClusterProject;
    LCP hideNativeSubstantiationOtherClusterProject;
    public LCP foreignSubstantiationOtherClusterProject;
    public LCP fileNativeSummaryProject;
    LCP hideForeignSubstantiationOtherClusterProject;
    LCP currentCluster;
    LCP nameNativeShortCurrentCluster;
    LCP inProjectCurrentCluster;
    LAP loadFileNativeSummaryProject;
    LAP openFileNativeSummaryProject;
    public LCP fileForeignSummaryProject;
    LAP loadFileForeignSummaryProject;
    LAP openFileForeignSummaryProject;
    public LCP fileNativeApplicationFormProject;
    LAP loadFileNativeApplicationFormProject;
    LAP openFileNativeApplicationFormProject;
    public LCP fileForeignApplicationFormProject;
    LAP loadFileForeignApplicationFormProject;
    LAP openFileForeignApplicationFormProject;
    public LCP fileNativeRoadMapProject;
    LAP loadNativeFileRoadMapProject;
    LAP openNativeFileRoadMapProject;
    public LCP fileForeignRoadMapProject;
    LAP loadForeignFileRoadMapProject;
    LAP openForeignFileRoadMapProject;
    public LCP fileResolutionIPProject;
    LAP loadFileResolutionIPProject;
    LCP hideLoadFileResolutionIPProject;
    LAP openFileResolutionIPProject;
    public LCP fileNativeTechnicalDescriptionProject;
    public LAP loadFileNativeTechnicalDescriptionProject;
    LAP openFileNativeTechnicalDescriptionProject;
    public LCP fileForeignTechnicalDescriptionProject;
    LAP loadFileForeignTechnicalDescriptionProject;
    LAP openFileForeignTechnicalDescriptionProject;

    public LCP isReturnInvestmentsProject;
    public LCP nameReturnInvestorProject;
    public LCP amountReturnFundsProject;
    public LCP isNonReturnInvestmentsProject;
    public LCP nameNonReturnInvestorProject;
    public LCP amountNonReturnFundsProject;
    public LCP isCapitalInvestmentProject;
    public LCP isPropertyInvestmentProject;
    public LCP isGrantsProject;
    public LCP isOtherNonReturnInvestmentsProject;
    public LCP commentOtherNonReturnInvestmentsProject;
    public LCP isOwnFundsProject;
    public LCP amountOwnFundsProject;
    public LCP isPlanningSearchSourceProject;
    public LCP amountFundsProject;
    public LCP isOtherSoursesProject;
    public LCP commentOtherSoursesProject;
    public LCP needsToBeTranslatedToRussianProject;
    public LCP needsToBeTranslatedToEnglishProject;
    public LCP translatedToRussianProject;
    public LCP hideTranslatedToRussianProject;
    public LCP translatedToEnglishProject;
    public LCP hideTranslatedToEnglishProject;
    public LCP fillNativeProject;
    public LCP fillForeignProject;
    public LCP langProject;
    public LCP isConsultingCenterQuestionProject;
    public LCP isConsultingCenterCommentProject;
    public LCP consultingCenterCommentProject;
    public LCP sumPositiveConsultingCenterCommentProject;
    public LCP sumNegativeConsultingCenterCommentProject;
    public LCP sumTotalConsultingCenterCommentProject;
    public LCP betweenDateProject;

    LCP hideNameReturnInvestorProject;
    LCP hideAmountReturnFundsProject;
    LCP hideNameNonReturnInvestorProject;
    LCP hideAmountNonReturnFundsProject;
    LCP hideCommentOtherNonReturnInvestmentsProject;
    LCP hideAmountOwnFundsProject;
    LCP hideAmountFundsProject;
    LCP hideCommentOtherSoursesProject;
    LCP hideIsCapitalInvestmentProject;
    LCP hideIsPropertyInvestmentProject;
    LCP hideIsGrantsProject;
    LCP hideIsOtherNonReturnInvestmentsProject;

    public LCP isOwnedEquipmentProject;
    public LCP isAvailableEquipmentProject;
    public LCP isTransferEquipmentProject;
    public LCP descriptionTransferEquipmentProject;
    public LCP ownerEquipmentProject;
    public LCP isPlanningEquipmentProject;
    public LCP specificationEquipmentProject;
    public LCP isSeekEquipmentProject;
    public LCP descriptionEquipmentProject;
    public LCP isOtherEquipmentProject;
    public LCP commentEquipmentProject;
    LCP hideDescriptionTransferEquipmentProject;
    LCP hideOwnerEquipmentProject;
    LCP hideSpecificationEquipmentProject;
    LCP hideDescriptionEquipmentProject;
    LCP hideCommentEquipmentProject;

    public LCP projectPatent, equalsPatentProject;
    public LCP sidProjectPatent;
    public LCP nativeTypePatent;
    public LCP foreignTypePatent;
    public LCP nativeNumberPatent;
    public LCP foreignNumberPatent;
    public LCP priorityDatePatent;
    public LCP isOwned;
    public LCP ownerPatent;
    public LCP nameForeignOwnerType;
    public LCP ownerTypePatent;
    LCP ownerTypeToPatent;
    LCP nameNativeOwnerTypePatent;
    LCP nameForeignOwnerTypePatent;
    public LCP ownerTypeToSID;
    public LCP projectTypeToSID;
    public LCP projectActionToSID;
    public LCP projectMissionToSID;
    public LCP projectScheduleToSID;
    public LCP fileIntentionOwnerPatent;
    LAP loadFileIntentionOwnerPatent;
    LAP openFileIntentionOwnerPatent;
    public LCP isValuated;
    public LCP valuatorPatent;
    public LCP fileActValuationPatent;
    LAP loadFileActValuationPatent;
    LAP openFileActValuationPatent;
    LCP hideOwnerPatent;
    LCP hideFileIntentionOwnerPatent;
    LCP hideLoadFileIntentionOwnerPatent;
    LCP hideOpenFileIntentionOwnerPatent;
    LCP hideValuatorPatent;
    LCP hideFileActValuationPatent;
    LCP hideLoadFileActValuationPatent;
    LCP hideOpenFileActValuationPatent;
    LCP hideNameNativeOwnerTypePatent;
    LCP hideNameForeignOwnerTypePatent;

    public LCP projectAcademic;
    public LCP sidProjectAcademic;
    public LCP fullNameAcademic;
    public LCP fullNameToAcademic;
    public LCP fullNameSIDToAcademic;
    public LCP institutionAcademic;
    public LCP titleAcademic;
    public LCP fileDocumentConfirmingAcademic;
    LAP loadFileDocumentConfirmingAcademic;
    LAP openFileDocumentConfirmingAcademic;
    public LCP fileDocumentEmploymentAcademic;
    LAP loadFileDocumentEmploymentAcademic;
    LAP openFileDocumentEmploymentAcademic;

    public LCP projectNonRussianSpecialist;
    public LCP sidProjectNonRussianSpecialist;
    public LCP fullNameNonRussianSpecialist;
    public LCP fullNameToNonRussianSpecialist;
    public LCP fullNameSIDToNonRussianSpecialist;
    public LCP organizationNonRussianSpecialist;
    public LCP titleNonRussianSpecialist;
    public LCP fileNativeResumeNonRussianSpecialist;
    LAP loadFileForeignResumeNonRussianSpecialist;
    LAP openFileForeignResumeNonRussianSpecialist;
    public LCP fileForeignResumeNonRussianSpecialist;
    LAP loadFileNativeResumeNonRussianSpecialist;
    LAP openFileNativeResumeNonRussianSpecialist;
    public LCP filePassportNonRussianSpecialist;
    LAP loadFilePassportNonRussianSpecialist;
    LAP openFilePassportNonRussianSpecialist;
    public LCP fileStatementNonRussianSpecialist;
    LAP loadFileStatementNonRussianSpecialist;
    LAP openFileStatementNonRussianSpecialist;

    public LCP fileMinutesOfMeetingExpertCollegiumProject;
    LAP loadFileMinutesOfMeetingExpertCollegiumProject;
    LAP openFileMinutesOfMeetingExpertCollegiumProject;
    public LCP fileWrittenConsentClaimerProject;
    LAP loadFileWrittenConsentClaimerProject;
    LAP openFileWrittenConsentClaimerProject;


    LCP isForeignExpert;
    LCP localeExpert;
    public LCP disableExpert;

    LAP editClaimer;
    public LAP addProject;
    LAP editR1Project;
    LAP editClaimerProject;
    LAP translateToRussianProject;
    LAP translateToEnglishProject;
    LCP hideTranslateToRussianProject, hideTranslateToEnglishProject;
    LCP needTranslationProject, needTranslationLegalChek, needsToBeTranslatedToRussianLegalChek;
    LCP needNeedTranslationProject;

    LCP projectFormalControl, dateProjectFormalControl, clusterProjectFormalControl, nameNativeClusterProjectFormalControl;
    LCP resultFormalControl;
    LCP sidResultFormalControl, nameResultFormalControl;
    LAP addNotEnoughDocumentsFCResult;
    LAP addNoListOfExpertsFCResult;
    LAP addNotSuitableClusterFCResult;
    LAP addRepeatedFCResult;
    LAP addPositiveFCResult;

    LCP updateDateFormalControl;
    LCP dateTimeSubmitFormalControl, dateSubmitFormalControl;
    LCP dateTimeFormalControl, dateFormalControl;

    LCP prevFormalControl, datePrevFormalControl;

    LCP overdueDateFormalControl;
    LCP overdueFormalControlProject;

    LCP projectActionLegalCheck;
    LCP nameProjectActionLegalCheck;
    LCP LCMinDateProjectActionProject;
    LCP minDateLegalCheck;
    LCP isR1LegalCheck;

    LCP isPreliminaryLegalCheck, isStatusLegalCheck;
    LCP prevLegalCheck, datePrevLegalCheck;

    LCP commentFormalControl;
    LCP[] maxFormalControlProjectProps;
    LCP currentFormalControlProject;
    LCP executeFormalControlProject;
    LCP dateExecuteFormalControlProject;
    LCP resultExecuteFormalControlProject;
    LCP nameResultExecuteFormalControlProject;
    LCP notEnoughDocumentsProject;
    LCP isPreliminaryNotEnoughDocumentProject;
    LCP isStatusNotEnoughDocumentProject;
    LCP noListOfExpertsProject;
    LCP notSuitableClusterProject;
    LCP repeatedProject;
    LCP positiveFormalResultProject;
    LCP negativeFormalResultProject;

    LCP projectActionFormalControl;
    LCP nameProjectActionFormalControl;

    LCP isPreliminaryFormalControl, isStatusFormalControl;
    LCP datePreliminaryProjectFormalControl, dateStatusProjectFormalControl;
    LCP updateDateLegalCheck;
    LCP dateTimeSubmitLegalCheck, dateSubmitLegalCheck;

    LCP dateTimeLegalCheck, dateLegalCheck;
    LCP overdueDateLegalCheck;

    LCP userLegalCheck, nameUserLegalCheck;

    LCP overdueLegalCheckProject;
    LCP projectLegalCheck;
    LCP resultForesightCheckProject, positiveResultForesightCheckProject, negativeResultForesightCheckProject;
    LAP setNegativeResultForesightCheckProject;
    LAP setPositiveResultForesightCheckProject;
    LCP commentForesightCheckProject;
    LAP setNegativeResultForesightCheckProjectApply;
    LAP applyForesightCheckProjectCluster;
    LAP rejectForesightCheckProject;
    LAP applyForesightCheckProject;
    LCP needForesightCheckProject;
    LCP resultLegalCheck, positiveResultLegalCheck, negativeResultLegalCheck;
    LCP nameResultLegalCheck;
    LCP dateProjectLegalCheck, sidResultLegalCheck, clusterProjectLegalCheck, nameNativeClusterProjectLegalCheck, isPreliminaryAndStatusProjectLegalCheck;
    LCP statusProjectFormalControl, sidStatusProjectFormalControl;
    LCP statusProjectLegalCheck, sidStatusProjectLegalCheck;
    LAP addNegativeLCResult;
    LAP addPositiveLCResult;
    LCP commentLegalCheck;
    LCP[] maxLegalCheckProjectProps;
    LCP currentLegalCheckProject;
    LCP executeLegalCheckProject;
    LCP dateExecuteLegalCheckProject;
    LCP resultExecuteLegalCheckProject;
    LCP resultNoticedLegalCheckProject;
    LCP negativeLegalResultProject;
    LCP negativeLegalResultStatusProject;
    LCP negativeLegalResultPreliminaryProject;
    LCP positiveLegalResultProject;
    LCP needNoticeLegalResultProject;
    LCP sentForTranslationProject, dateSentForTranslationProject, dateToSentForTranslationProject;
    LCP positiveStatusLegalCheckProject, datePositiveStatusLegalCheckProject;
    LCP transferredProject, dateTransferredProject;
    LCP needVoteProject;
    LCP changeLegalCheck, noticedChangeLegalCheck, dateChangeLegalCheck;

    public LCP dateAgreementExpert;
    LCP vone;
    LCP claimerProjectVote;
    LCP nameNativeJoinClaimerProjectVote;
    LCP countryExpert;
    public LCP nameCountryExpert;
    LCP caseCountry;
    LCP caseCountryExpert;
    LCP currencyExpert;
    public LCP nameCurrencyExpert;
    LCP residency;
    LCP residencyCountryExpert;
    LCP rateExpert;
    LCP executiveLD, phoneExecutiveLD, mobileExecutiveLD;
    LCP executiveIS;
    LCP emailForCertificates;
    LCP readInformation;
    LCP moneyQuantityDoneExpertMonthYear;
    LCP baseCurrency;
    LCP baseCurrencyExpert;
    LCP englCountry;
    LCP englCountryExpert;
    LCP englCurrency;
    LCP englCurrencyExpert;
    LCP pluralCurrency;
    LCP pluralCurrencyExpert;
    LAP emailLetterExpertMonthYearEA;
    LAP emailLetterCertificatesExpertMonthYear;
    LCP previousDate;
    LCP monthInPreviousDate, yearInPreviousDate;
    LCP isNewMonth;
    LCP quantityIsR1DoneExpert;
    LCP quantityIsR2DoneExpert;

    LCP hasPreliminaryVoteProject;
    LCP isPreliminaryStatusProject;
    LCP isPreliminaryAndStatusProject, isPreliminaryAndStatusProjectFormalControl;

    LCP projectActionApplication, nameProjectActionApplication;

    LCP projectApplication;
    LCP projectApplicationPreliminary, projectApplicationStatus;
    LCP isPreliminaryApplication, isStatusApplication;
    LCP isPreliminaryAndStatusApplication;
    LCP preliminaryApplicationProject;
    LCP statusApplicationProject;
    LCP inActTestApplication;
    LCP inActTestApplicationDate;
    LCP inApplicationCluster;
    LCP nativeSubstantiationApplicationCluster;
    LCP foreignSubstantiationApplicationCluster;
    LCP inApplicationForesight;
    LCP isR2Application, isR1Application;
    LAP openApplicationProjectActionApplication;
    LAP openCompleteApplicationProjectActionApplication;
    LAP exportProjectDocumentsActionApplication;
    LCP nameRegulationsApplication;

    LCP inactiveApplication;

    LCP dateApplicationPreliminary, dateApplicationStatus;
    LCP dateApplication;

    LCP nameNativeProjectApplication;
    LCP nameNativeClaimerApplicationPreliminary, nameNativeClaimerApplicationStatus;
    LCP nameNativeClaimerApplication;

    private LCP emailClaimerApplication;

    private LCP langApplication;
    private LCP nameNativeShortAggregateClusterApplication;

    private LCP finalClusterApplication, nameFinalClusterApplication;

    LCP quantityClusterApplication;
    LCP nonClusterApplication;
    LCP nonClusterApplicationsSubmitDateDate;
    LCP nonClusterApplicationsIsSatusSubmitDateDate;

    LCP statusJoinApplication;
    LCP isPreliminaryAfterStatusApplication;
    LCP statusApplication;
    LCP nameStatusApplication;
    LCP officialNameStatusApplication;
    LCP needFormalCheckStatusProject;
    LCP needLegalCheckStatusProject;
    LCP hideAddPositiveLCResult;
    LCP hideAddNegativeLCResult;
    public LCP projectMissionProject;
    LCP nameProjectMissionProject;
    public LCP nativeResumeProject;
    public LCP foreignResumeProject;
    public LCP nameNativeContactProject;
    public LCP nameForeignContactProject;
    public LCP phoneContactProject;
    public LCP emailContactProject;
    public LCP nativeMarketTrendsProject;
    public LCP foreignMarketTrendsProject;
    public LCP linksMarketTrendsProject;
    public LCP linksAnalogProject;
    public LCP nativeRelevanceProject;
    public LCP foreignRelevanceProject;
    public LCP nativeBasicTechnologyProject;
    public LCP foreignBasicTechnologyProject;
    public LCP nativeCaseStudiesProject;
    public LCP foreignCaseStudiesProject;
    public LCP nativeCharacteristicsAnaloguesProject;
    public LCP foreignCharacteristicsAnaloguesProject;
    public LCP nativeCompaniesAnaloguesProject;
    public LCP foreignCompaniesAnaloguesProject;
    public LCP nativeMarketIntroductionProject;
    public LCP foreignMarketIntroductionProject;
    public LCP linksMarketIntroductionProject;
    public LCP linksForeignMarketIntroductionProject;
    public LCP nativeHistoryProject;
    public LCP foreignHistoryProject;
    public LCP nativeDynamicsProject;
    public LCP foreignDynamicsProject;
    public LCP nativeGrantsProject;
    public LCP foreignGrantsProject;
    public LCP nativeLaboratoryProject;
    public LCP foreignLaboratoryProject;
    public LCP nativeInvestmentProject;
    public LCP foreignInvestmentProject;
    public LCP nativeResultsProject;
    public LCP foreignResultsProject;
    public LCP nativeGeneralizedPlanProject;
    public LCP foreignGeneralizedPlanProject;
    public LCP projectSpecialist, equalsSpecialistProject;
    public LCP nativePostSpecialist;
    public LCP foreignPostSpecialist;
    public LCP nativeFunctionSpecialist;
    public LCP foreignFunctionSpecialist;
    public LCP nativeScopeSpecialist;
    public LCP foreignScopeSpecialist;
    public LCP nativeExperienceSpecialist;
    public LCP foreignExperienceSpecialist;
    public LCP nativeTitleSpecialist;
    public LCP foreignTitleSpecialist;
    public LCP nativeWorkSpecialist;
    public LCP foreignWorkSpecialist;
    public LCP nativePublicationsSpecialist;
    public LCP foreignPublicationsSpecialist;
    public LCP nativeCitationSpecialist;
    public LCP foreignCitationSpecialist;
    public LCP nativeIntellectualPropertySpecialist;
    public LCP foreignIntellectualPropertySpecialist;
    public LCP fileStatementSpecialist;
    public LAP loadFileStatementSpecialist;
    public LAP openFileStatementSpecialist;
    public LCP filePassportSpecialist;
    public LAP loadFilePassportSpecialist;
    public LAP openFilePassportSpecialist;
    public LCP projectResearch;
    public LCP equalsResearchProject;
    public LCP foreignCommentResearch;
    public LCP nativeCommentResearch;
    public LCP dataResearch;
    public LCP nativeCommentToResearch;
    public LCP nativeCommentSIDToResearch;
    public LCP foreignCommentToResearch;
    public LCP foreignCommentSIDToResearch;
    public LCP linksPatent;
    public LCP nativeCommentMissionProject;
    public LCP foreignCommentMissionProject;
    public LCP projectPublications, equalsPublicationsProject;
    public LCP nativeToPublications, nativeSIDToPublications;
    public LCP foreignToPublications, foreignSIDToPublications;
    public LCP foreignPublications;
    public LCP nativeAuthorPublications;
    public LCP foreignAuthorPublications;
    public LCP datePublications;
    public LCP nativeLinksPublications;
    public LCP projectCommercialization, equalsCommercializationProject;
    public LCP nativeProjectCommercialization;
    public LCP nativeProjectToCommercialization, nativeProjectSIDToCommercialization;
    public LCP foreignProjectToCommercialization, foreignProjectSIDToCommercialization;
    public LCP foreignProjectCommercialization;
    public LCP nativeCommentProjectCommercialization;
    public LCP foreignCommentProjectCommercialization;
    public LCP projectAnalogues, equalsAnaloguesProject;
    public LCP nativeProjectAnalogues;
    public LCP nativeProjectToAnalogues, nativeProjectSIDToAnalogues;
    public LCP foreignProjectToAnalogues, foreignProjectSIDToAnalogues;
    public LCP foreignProjectAnalogues;
    public LCP nativeDescriptionProjectAnalogues;
    public LCP foreignDescriptionProjectAnalogues;
    public LCP nativeCharacteristicsProjectAnalogues;
    public LCP foreignCharacteristicsProjectAnalogues;
    public LCP projectObjectives, equalsObjectivesProject;
    public LCP nativeProjectObjectives;
    public LCP nativeProjectToObjectives, nativeProjectSIDToObjectives;
    public LCP foreignProjectToObjectives, foreignProjectSIDToObjectives;
    public LCP foreignProjectObjectives;
    public LCP nameNativeSpecialist;
    public LCP nameNativeToSpecialist, nameNativeSIDToSpecialist;
    public LCP nameForeignToSpecialist, nameForeignSIDToSpecialist;
    public LCP nameForeignSpecialist;
    public LCP nativePublications;
    public LCP nativeEditionPublications;
    public LCP foreignEditionPublications;
    LAP editR2Project;
    LCP foreignProjectMissionProject;
    LCP descriptionProjectMission;

    private LCP daysSubmitApplicationDate;
    private LCP weekSubmitApplicationDate;
    private LCP applicationsSubmitDateWeek;
    private LCP oneApplicationDateDate;
    private LCP applicationsSubmitDateDate;
    private LCP applicationsSubmitProjectActionClusterDateDate;
    private LCP averageWeekApplSubmitDateDate;

    private LCP daysClaimerFirstOriginalDocsProject, isClaimerFirstOriginalDocsProjectDate;
    private LCP daysClaimerExtraOriginalDocsProject, isClaimerExtraOriginalDocsProjectDate;
    private LCP daysClaimerOriginalDocsProject, isClaimerOriginalDocsProjectDate;

    private LCP daysClaimerPreliminaryFormalControlProject, isClaimerPreliminaryFormalControlProjectDate;
    private LCP daysClaimerStatusFormalControlProject, isClaimerStatusFormalControlProjectDate;

    private LCP daysClaimerPreliminaryLegalCheckProject, isClaimerPreliminaryLegalCheckProjectDate;
    private LCP daysClaimerStatusLegalCheckProject, isClaimerStatusLegalCheckProjectDate;

    private LCP daysClaimerPreliminaryProject, isClaimerPreliminaryProjectDate;
    private LCP daysClaimerStatusProject, isClaimerStatusProjectDate;

    private LCP daysClaimerApplicationPreliminary, daysClaimerApplicationStatus, daysClaimerApplication;
    private LCP isClaimerApplicationPreliminaryDate, isClaimerApplicationStatusDate, isClaimerApplicationDate;

    LCP nonClusterApplicationsProjectActionSubmitDateDate;
    LCP applicationsSubmitClusterDateDate;
    LCP applicationsSubmitProjectActionDateDate;
    LCP applicationsSubmitStatusApplicationClusterDateDate;
    LCP applicationsSubmitStatusApplicationDateDate;
    LCP nonClusterApplicationsStatusAplicationSubmitDateDate;
    LCP oneApplications;
    LCP sumApplicationsStatusApplication;
    LCP sumApplicationsStatusApplicationCluster;
    LCP typeProjectStatusApplication;
    LCP sumApplicationsTypeProjectStatus;
    LCP sumApplicationsTypeProjectStatusCluster;
    LCP sumSubmitApplications;
    LCP nonClusterApplicationsStatusApplicationCluster;
    LCP nonClusterApplicationsStatusApplication;
    LCP nonClusterApplicationsTypeProjectStatusCluster;
    LCP nonClusterApplicationsTypeProjectStatus;
    LCP nonClusterApplicationsSubmit;
    LCP sumApplicationsCluster;
    LCP percentSumApplicationsTypeProjectStatusCluster;
    LCP percentNonClusterApplicationsTypeProjectStatus;
    LCP percentApplicationsTypeProjectStatus;
    LCP inTestApplication;
    public LCP projectMileStone, equalsMileStoneProject;
    public LCP nativeMileStone, nativeToMileStone;;
    public LCP nativeSIDToMileStone;
    public LCP orderNumberMileStone;
    public LCP projectMileStoneYear, equalsMileStoneYearProject;
    public LCP nativeMileStoneYear;
    public LCP nativeToMileStoneYear, nativeSIDToMileStoneYear;
    public LCP nativeTypeMileStone, foreignTypeMileStone;
    public LCP nativeDescriptionTypeMileStoneMileStoneMileStoneYear;
    public LCP foreignDescriptionTypeMileStoneMileStoneMileStoneYear;
    public LCP nativeResearchDescriptionTypeMileStoneMileStoneYear;
    public LCP nativeProductCreationDescriptionTypeMileStoneMileStoneYear;
    public LCP nativePlanOnHiringDescriptionTypeMileStoneMileStoneYear;
    public LCP nativeLicensingDescriptionTypeMileStoneMileStoneYear;
    public LCP nativePromotionDescriptionTypeMileStoneMileStoneYear;
    public LCP nativeSellingDescriptionTypeMileStoneMileStoneYear;
    public LCP foreignResearchDescriptionTypeMileStoneMileStoneYear;
    public LCP foreignProductCreationDescriptionTypeMileStoneMileStoneYear;
    public LCP foreignPlanOnHiringDescriptionTypeMileStoneMileStoneYear;
    public LCP foreignLicensingDescriptionTypeMileStoneMileStoneYear;
    public LCP foreignPromotionDescriptionTypeMileStoneMileStoneYear;
    public LCP foreignSellingDescriptionTypeMileStoneMileStoneYear;

    LCP dateRegisteredStatusProject;
    LCP dateNoClusterStatusProject;
    LCP dateNoExpertsStatusProject;
    LCP dateNotEnoughDocsForStatusStatusProject;
    LCP dateNotEnoughDocsForPreliminaryStatusProject;
    LCP dateRepeatedStatusProject;
    LCP datePositiveFCResultStatusProject;
    LCP dateOverdueFCStatusProject;
    LCP dateNegativeLCPreliminaryResultStatusProject;
    LCP dateNegativeLCStatusResultStatusProject;
    LCP datePositiveLCResultStatusProject;
    LCP dateOverdueLCStatusProject;
    LCP datePositiveFSResultProject;
    LCP dateNeedTranslationStatusProject;
    LCP dateInProgressStatusProject, dateInProgressRepeatStatusProject, dateIssuedVoteDocsStatusProject, dateNeedExtraVoteStatusProject, dateRejectedStatusProject, dateAcceptedStatusProject;
    LCP dateAppliedOriginalDocsStatusProject, dateNotEnoughOriginalDocsStatusProject, dateOverdueOriginalDocsStatusProject;
    LCP dateApplyStatusStatusProject;
    LCP dateSentPreliminaryAcceptedStatusProject, dateSentStatusAcceptedStatusProject, dateSentRejectedStatusProject, dateWithdrawnStatusProject;
    LCP dateSentForSignatureStatusProject, dateSignedStatusProject, dateSentToFinDepStatusProject, dateSubmittedToRegisterStatusProject, datePreparedCertificateStatusProject, dateCertifiedStatusProject;
    LCP dateInStatusProject;
    LCP overdueDateStatusProject, normalPeriodStatus, normalPeriodStatusProject, normalPeriodStatusApplication, isWorkDaysNormalPeriodStatus, quantityDaysToOverdueDateStatusProject;
    LCP isFinalProjectStatus;
    LCP typeProjectStatusProjectStatus, nameTypeProjectStatusProjectStatus;
    LCP dateInStatusApplication, overdueDateStatusApplication, quantityDaysToOverdueDateStatusApplication;
    LCP dateSubmittedToRegisterApplication;
    LCP quantityStatusVoteProject;
    LCP quantityStatusVoteApplication;
    LCP daysCommonApplication;
    LCP daysStatusApplication;
    LCP registerApplicationDateTo;
    LCP daysRegisterApplicationDateTo;
    LCP oneRegisterApplicationDateTo;
    LCP submitRegisterApplicationsDateTo;
    LCP submitDaysRegisterApplicationDateTo;
    LCP averageDaysRegisterApplicationsDateTo;
    LCP qSubmitRegisterApplicationsDateTo;
    LCP daysSubmitRegisterApplicationDate;
    LCP weekSubmitRegisterApplicationDate;
    LCP registerApplicationDate;
    LCP submitDaysRegisterApplicationDateWeek;
    LCP submitRegisterApplicationsDateWeek;
    LCP risingDaysRegisterApplicationDateWeek;
    LCP risingRegisterApplicationsDateWeek;
    LCP averageDaysRegisterApplicationsDateWeek;
    LCP registerApplication;
    LCP oneStatusApplicationDateDate;
    LCP statusApplicationsSubmitDateDate;
    LCP averageDaysRegisterApplicationsDateWeekWeek;
    LCP averageDaysStatusApplicationsSubmitDateDate;
    LCP daysStatusApplicationsSubmitDateDate;
    LCP statusApplicationDateDate;
    LCP notificationPeriodProject;
    LCP dateNotificationPeriodProject;
    LCP datePositiveLegalResultProject;
    LCP isFileNativeTechnicalDescriptionProject;
    LCP isFileForeignTechnicalDescriptionProject;
    LCP nameNativeProjectFormalControl;
    LCP commentLegalCheckProject;
    LCP setNegativeLegalResultProject;
    LCP setNegativeLegalResultProjectApply;
    LAP acceptPreliminaryLegalCheckProject;
    LAP rejectLegalCheckProject;
    LCP setPositiveLegalResultProject, setPositiveLegalResultProjectApply, applyLegalCheckProject;
    LAP addPositiveLCResultProject;
    LAP setPositiveLCResultApplyProject;

    @Override
    public void initProperties() {
        initProperties0();
        initProperties1();
    }

    public void initProperties0() {

        idGroup.add(baseLM.objectValue);

        negative = addJProp(baseLM.less2, 1, baseLM.vzero);

        previousDate = addJProp("previousDate", "Вчерашняя дата", baseLM.subtractDate, baseLM.currentDate, addCProp("1", IntegerClass.instance, 1));
        monthInPreviousDate = addJProp("monthInPreviousDate", "Вчерашний месяц", baseLM.numberMonthInDate, previousDate);
        yearInPreviousDate = addJProp("yearInPreviousDate", "Вчерашний год", baseLM.yearInDate, previousDate);

        // monthInYeasterdayDate = addJProp("monthInYeasterdayDate", "Вчерашний месяц", baseLM.numberMonthInDate, addJProp(baseLM.sumDate, baseLM.currentDate, addCProp("1", IntegerClass.instance, 1)));
        isNewMonth = addJProp("isNewMonth", "Начало месяца", baseLM.diff2, baseLM.currentMonth, monthInPreviousDate);

        nameNative = addDProp(recognizeGroup, "nameNative", "Имя", InsensitiveStringClass.get(2000), baseLM.multiLanguageNamed);
        ((CalcProperty)nameNative.property).aggProp = true;
        nameNative.setMinimumWidth(10);
        nameNative.setPreferredWidth(50);
        nameForeign = addDProp(recognizeGroup, "nameForeign", "Имя (иностр.)", InsensitiveStringClass.get(2000), baseLM.multiLanguageNamed);
        ((CalcProperty)nameForeign.property).aggProp = true;
        nameForeign.setMinimumWidth(10);
        nameForeign.setPreferredWidth(50);

        nameNativeShort = addDProp(baseGroup, "nameNativeShort", "Имя (сокр.)", InsensitiveStringClass.get(4), cluster);
        nameNativeShort.setFixedCharWidth(5);

        baseGroup.add(baseLM.email.property); // сделано, чтобы email был не самой первой колонкой в диалогах

        // глобальные свойства
        requiredPeriod = addDProp(baseGroup, "votePeriod", "Срок заседания", IntegerClass.instance);
        overduePeriod = addDProp(baseGroup, "overduePeriod", "Срок просрочки для формального контроля", IntegerClass.instance);

        requiredQuantity = addDProp(baseGroup, "voteRequiredQuantity", "Кол-во экспертов", IntegerClass.instance);
        requiredBusinessQuantity = addDProp(baseGroup, "voteRequiredBusinessQuantity", "Кол-во экспертов (бизнес)", IntegerClass.instance);
        requiredQuantityVote = addDProp(baseGroup, "requiredQuantityVote", "Кол-во экспертов", IntegerClass.instance, vote);

        limitExperts = addDProp(baseGroup, "limitExperts", "Кол-во прогол. экспертов", IntegerClass.instance);
        projectsImportLimit = addDProp(baseGroup, "projectsImportLimit", "Максимальное кол-во импортируемых проектов", IntegerClass.instance);
        importOnlyR2Projects = addDProp(baseGroup, "importOnlyR2Projects", "Импортировать проекты только по новому регламенту", LogicalClass.instance);
        rateExpert = addDProp(baseGroup, "rateExpert", "Ставка эксперта (долларов)", DoubleClass.instance);
        emailForCertificates = addDProp(baseGroup, "emailForCertificates", "E-mail для актов", StringClass.get(50));
        readInformation = addDProp(baseGroup, "readInformation", "Включить в отчетах персонифицированную информацию по экспертам", LogicalClass.instance);

        //свойства заявителя
        nameNativeJoinClaimer = addJProp(claimerInformationGroup, "nameNativeJoinClaimer", "Заявитель", baseLM.and1, nameNative, 1, is(claimer), 1);
        nameNativeJoinClaimer.setMinimumWidth(10);
        nameNativeJoinClaimer.setPreferredWidth(50);
        nameForeignJoinClaimer = addJProp(claimerInformationGroup, "nameForeignJoinClaimer", "Claimer", baseLM.and1, nameForeign, 1, is(claimer), 1);
        nameForeignJoinClaimer.setMinimumWidth(10);
        nameForeignJoinClaimer.setPreferredWidth(50);
        firmNameNativeClaimer = addDProp(claimerInformationGroup, "firmNameNativeClaimer", "Фирменное название", InsensitiveStringClass.get(2000), claimer);
        firmNameNativeClaimer.setMinimumWidth(10);
        firmNameNativeClaimer.setPreferredWidth(50);
        firmNameForeignClaimer = addDProp(claimerInformationGroup, "firmNameForeignClaimer", "Brand name", InsensitiveStringClass.get(2000), claimer);
        firmNameForeignClaimer.setMinimumWidth(10);
        firmNameForeignClaimer.setPreferredWidth(50);

        phoneClaimer = addDProp(contactGroup, "phoneClaimer", "Телефон", StringClass.get(100), claimer);

        addressClaimer = addDProp(contactGroup, "addressClaimer", "Адрес", StringClass.get(2000), claimer);
        addressClaimer.setMinimumWidth(10);
        addressClaimer.setPreferredWidth(50);

        postAddressClaimer = addDProp(contactGroup, "postAddressClaimer", "Почтовый адрес", StringClass.get(2000), claimer);
        postAddressClaimer.setMinimumWidth(10);
        postAddressClaimer.setPreferredWidth(50);

        siteClaimer = addDProp(contactGroup, "siteClaimer", "Сайт", StringClass.get(100), claimer);
        emailClaimer = addJProp(contactGroup, "emailClaimer", "E-mail", baseLM.and1, baseLM.email, 1, is(claimer), 1);
        emailFirmClaimer = addDProp(contactGroup, "emailFirmClaimer", "E-mail организации", StringClass.get(50), claimer);
        emailExpert = addJProp("emailExpert", "E-mail", baseLM.and1, baseLM.email, 1, is(expert), 1);

        clusterClusterUser = addDProp(idGroup, "clusterClusterUser", "Кластер (ИД)", cluster, clusterUser);
        nameClusterClusterUser = addJProp(baseGroup, "nameClusterClusterUser", "Кластер", nameNativeShort, clusterClusterUser, 1);
        clusterCurrentUser = addJProp(idGroup, "clusterCurrentUser", "Кластер текущего пользователя", clusterClusterUser, baseLM.currentUser);
        sidCluster = addDProp("sidCluster", "префикс кластера", StringClass.get(1), cluster);
        sidCluster.setFixedCharWidth(3);

        withdrawnProject = addDProp(withdrawnGroup, "withdrawnProject", "Отозвана заявителем", LogicalClass.instance, project);
        dateWithdrawnProject = addDCProp(withdrawnGroup, "dateWithdrawnProject", "Дата отзыва заявителем", true, baseLM.currentDate, withdrawnProject, 1);

        statementClaimer = addDProp("statementClaimer", "Заявление", DynamicFormatFileClass.instance, claimer);
        loadStatementClaimer = addLFAProp(documentGroup, "Загрузить заявление", statementClaimer);
        openStatementClaimer = addOFAProp(documentGroup, "Открыть заявление", statementClaimer);

        constituentClaimer = addDProp("constituentClaimer", "Учредительные документы", DynamicFormatFileClass.instance, claimer);
        loadConstituentClaimer = addLFAProp(documentGroup, "Загрузить учредительные документы", constituentClaimer);
        openConstituentClaimer = addOFAProp(documentGroup, "Открыть учредительные документы", constituentClaimer);

        extractClaimer = addDProp("extractClaimer", "Выписка из реестра", DynamicFormatFileClass.instance, claimer);
        loadExtractClaimer = addLFAProp(documentGroup, "Загрузить выписку из реестра", extractClaimer);
        openExtractClaimer = addOFAProp(documentGroup, "Открыть Выписку из реестра", extractClaimer);

        OGRNClaimer = addDProp(legalDataGroup, "OGRNClaimer", "ОГРН", StringClass.get(13), claimer);
        INNClaimer = addDProp(legalDataGroup, "INNClaimer", "ИНН", StringClass.get(12), claimer);

        projectScheduleProject = addDProp(idGroup, "projectScheduleProject", "Регламент проекта (ИД)", projectSchedule, project);
        regulationsProject = addSUProp("regulationsProject", "regulationsProject", Union.OVERRIDE, addCProp(projectSchedule, "R1", project), projectScheduleProject);
        nameRegulationsProject = addJProp(baseGroup, "nameRegulationsProject", "Регламент проекта", baseLM.name, regulationsProject, 1);
        nameRegulationsProject.setFixedCharWidth(2);
      //  isR2Project = addDProp(baseGroup, "isR2Project", "Новый регламент", LogicalClass.instance, project);
      //  isR1Project = addJProp(baseGroup, "isR1Project", "Старый регламент", baseLM.andNot1, addCProp(LogicalClass.instance, true, project), 1, isR2Project, 1);
        isR2Project = addJProp(baseGroup, "isR2Project", "Регламент 2", baseLM.equals2, regulationsProject, 1, addCProp(projectSchedule, "R2"));
        isR1Project = addJProp(baseGroup, "isR1Project", "Регламент 1", baseLM.equals2, regulationsProject, 1, addCProp(projectSchedule, "R1"));

        useAllClusterExpertsProject = addDProp("useAllClusterExpertsProject", "Все эксп.", LogicalClass.instance, project);
        useAllClusterExperts = addDProp(baseGroup, "useAllClusterExperts", "Все эксперты", LogicalClass.instance);
        useAllClusterExpertsUnionProject = addSUProp(baseGroup, "useAllClusterExpertsUnionProject", "Все эксп.", Union.OVERRIDE, addJProp(baseLM.and1, useAllClusterExperts, is(project), 1), useAllClusterExpertsProject);

        revisionVote = addCUProp(baseGroup, "revisionVote", "Регламент", addCProp(StringClass.get(3), "R1", voteR1), addCProp(StringClass.get(3), "R2", voteR2));

        projectVote = addDProp(idGroup, "projectVote", "Проект (ИД)", project, vote);
        setNotNull(projectVote);

        isR1ProjectVote = addJProp(baseGroup, "isR1ProjectVote", "Проект 1", isR1Project, projectVote, 1);
        isR2ProjectVote = addJProp(baseGroup, "isR2ProjectVote", "Проект 2", isR2Project, projectVote, 1);

        addConstraint(addJProp("Не соответствует регламенту", baseLM.andNot1,
                is(voteR1), 1, isR1ProjectVote, 1), false);

        addConstraint(addJProp("Не соответствует регламенту", baseLM.andNot1,
                is(voteR2), 1, isR2ProjectVote, 1), false);

        inTestCluster = addDProp(baseGroup, "inTestCluster", "Ненужный", LogicalClass.instance, cluster);

        inProjectCluster = addDProp(baseGroup, "inProjectCluster", "Вкл", LogicalClass.instance, project, cluster);
        inClaimerProjectCluster = addDProp(baseGroup, "inClaimerProjectCluster", "Вкл (заявитель)", LogicalClass.instance, project, cluster);

        inClusterCurrentUserProject = addJProp("inClusterCurrentUserProject", "Вкл", inProjectCluster, 1, clusterCurrentUser);

        quantityClusterProject = addSGProp(baseGroup, "quantityClusterProject", true, "Кол-во кластеров",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1, cluster), 2,
                        addJProp(baseLM.equals2, baseLM.vtrue, inProjectCluster, 1, 2), 1, 2), 1);
        quantityClusterProject.setFixedCharWidth(1);

        sidForesight = addDProp(baseGroup, "sidForesight", "Код форсайта", StringClass.get(10), foresight);
        sidForesight.setFixedCharWidth(10);

        foresightSID = addAGProp("foresightSID", "Форсайт (ИД)", sidForesight);

        clusterForesight = addDProp(idGroup, "clusterForesight", "Кластер (ИД)", cluster, foresight);
        nameNativeClusterForesight = addJProp(baseGroup, "nameNativeClusterForesight", "Кластер", nameNative, clusterForesight, 1);
        nameForeignClusterForesight = addJProp(baseGroup, "nameForeignClusterForesight", "Кластер (иностр.)", nameForeign, clusterForesight, 1);
        nameNativeShortClusterForesight = addJProp(baseGroup, "nameNativeShortClusterForesight", "Кластер (сокр.)", nameNativeShort, clusterForesight, 1);

        inProjectForesight = addDProp(baseGroup, "inProjectForesight", "Вкл", LogicalClass.instance, project, foresight);
        isRootForesight = addDProp(baseGroup, "isRootForesight", "Корневой", LogicalClass.instance, foresight);
        isRootInProjectForesight = addJProp("isRootInProjectForesight", "Вкл", baseLM.andNot1, inProjectForesight, 1, 2, isRootForesight, 2);
        isInClusterProjectForesight = addJProp(true, "isInClusterProjectForesight", "Форсайт в кластере проекта", inProjectCluster, 1, clusterForesight, 2);
        quantityForesightProject = addSGProp(baseGroup, "quantityForesightProject", true, "Кол-во форсайтов",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1, foresight), 2,
                        inProjectForesight, 1, 2), 1);

        followed(isInClusterProjectForesight, inProjectForesight);

        quantityVoteProject = addSGProp(baseGroup, "quantityVoteProject", true, "Кол-во заседаний", addCProp(IntegerClass.instance, 1, vote), projectVote, 1);
        quantityVoteProject.setFixedCharWidth(2);
        quantityDefaultVoteProject = addDProp(baseGroup, "quantityDefaultVoteProject", "По-умолчанию, 1", IntegerClass.instance, project);
        quantityDefaultVoteProject.setFixedCharWidth(2);

        nameNativeJoinProject = addJProp(baseLM.and1, nameNative, 1, is(project), 1);
        nameNativeDataProject = addDProp("nameNativeDataProject", "Название проекта", InsensitiveStringClass.get(2000), project);
        nameNativeProject = addSUProp(projectInformationGroup, "nameNativeProject", "Название проекта", Union.OVERRIDE, nameNativeJoinProject, nameNativeDataProject);
        nameNativeProject.setMinimumWidth(10);
        nameNativeProject.setPreferredWidth(120);

        nameForeignJoinProject = addJProp(baseLM.and1, nameForeign, 1, is(project), 1);

        nameForeignDataProject = addDProp("nameForeignDataProject", "Name of the project", InsensitiveStringClass.get(2000), project);

        nameForeignProject = addSUProp(projectInformationGroup, "nameForeignProject", "Name of the project", Union.OVERRIDE, nameForeignJoinProject, nameForeignDataProject);
        nameForeignProject.setMinimumWidth(10);
        nameForeignProject.setPreferredWidth(120);

        nameNativeProjectVote = addJProp(baseGroup, "nameNativeProjectVote", "Проект", nameNativeProject, projectVote, 1);
        nameForeignProjectVote = addJProp(baseGroup, "nameForeignProjectVote", "Проект (иностр.)", nameForeignProject, projectVote, 1);

        dataDocumentNameExpert = addDProp(baseGroup, "dataDocumentNameExpert", "Имя для документов", StringClass.get(70), expert);
        documentNameExpert = addSUProp("documentNameExpert", "Имя для документов", Union.OVERRIDE, addJProp(baseLM.and1, addJProp(baseLM.istring2SP, baseLM.userLastName, 1, baseLM.userFirstName, 1), 1, is(expert), 1), dataDocumentNameExpert);
        documentNameExpert.setMinimumCharWidth(20);

        isScientificExpert = addDProp("isScientificExpert", "Научный эксперт", LogicalClass.instance, expert);
        isTechnicalExpert = addDProp("isTechnicalExpert", "Технический эксперт", LogicalClass.instance, expert);
        isBusinessExpert = addDProp("isBusinessExpert", "Бизнес-эксперт", LogicalClass.instance, expert);
        sidExpert = addDProp(baseGroup, "sidExpert", "Код эксперта", StringClass.get(5), expert);
        sidExpert.setFixedCharWidth(5);
        sidToExpert = addAGProp("sidToExpert", "SID эксперта", sidExpert);

        commentScientificExpert = addDProp(commentExpertiseGroup, "commentScientificExpert", "Опыт в научной сфере", TextClass.instance, expert);
        commentTechnicalExpert = addDProp(commentExpertiseGroup, "commentTechnicalExpert", "Опыт в технической сфере", TextClass.instance, expert);
        commentBusinessExpert = addDProp(commentExpertiseGroup, "commentBusinessExpert", "Опыт в бизнес сфере", TextClass.instance, expert);

        expertiseExpert = addDProp("expertiseExpert", "Экспертиза по существу", LogicalClass.instance, expert);
        grantExpert = addDProp("grantExpert", "Гранты", LogicalClass.instance, expert);
        profileBlockedExpert = addDProp("profileBlockedExpert", "Профайл заблокирован", LogicalClass.instance, expert);
        profileUpdateDateExpert = addDProp("profileUpdateDateExpert", "Время обновления", DateTimeClass.instance, expert);

        clusterExpert = addDProp(idGroup, "clusterExpert", "Кластер (ИД)", cluster, expert);
        nameNativeClusterExpert = addJProp(baseGroup, "nameNativeClusterExpert", "Кластер", nameNative, clusterExpert, 1);
        nameForeignClusterExpert = addJProp("nameForeignClusterExpert", "Кластер (иностр.)", nameForeign, clusterExpert, 1);
        nameNativeShortClusterExpert = addJProp(baseGroup, "nameNativeShortClusterExpert", "Кластер (сокр.)", nameNativeShort, clusterExpert, 1);

        primClusterExpert = addJProp("primClusterExpert", "Вкл (осн.)", baseLM.equals2, 1, clusterExpert, 2);
        extraClusterExpert = addDProp(baseGroup, "extraClusterExpert", "Вкл (доп.)", LogicalClass.instance, cluster, expert);
        inClusterExpert = addSUProp(baseGroup, "inClusterExpert", true, "Вкл", Union.OVERRIDE, extraClusterExpert, addJProp(baseLM.equals2, 1, clusterExpert, 2));

        quantityInClusterExpert = addSGProp("quantityInClusterExpert", true, "Кол-во кластеров", addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1, cluster, expert), 1, 2, inClusterExpert, 1, 2), 2);
        quantityInClusterExpert.setFixedCharWidth(2);

        clusterInExpertForesight = addJProp(baseGroup, "clusterInExpertForesight", "Форсайт кластера эксперта", inClusterExpert, clusterForesight, 2, 1);
        inExpertForesight = addDProp(baseGroup, "inExpertForesight", "Вкл.", LogicalClass.instance, expert, foresight);
        commentExpertForesight = addDProp(baseGroup, "commentExpertForesight", "Комментарий", TextClass.instance, expert, foresight);

        addConstraint(addJProp("Вначале выберите для эксперта кластер", baseLM.andNot1,
                inExpertForesight, 1, 2, clusterInExpertForesight, 1, 2), false);

        quantityInExpertForesight = addSGProp(baseGroup, "quantityInExpertForesight", true, "Количество экспертов по форсайту",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1, expert), 1, inExpertForesight, 1, 2), 2);
        quantityInExpertForesight.setFixedCharWidth(3);

        quantityInForesightExpert = addSGProp(baseGroup, "quantityInForesightExpert", true, "Количество форсайтов",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1, expert), 1, inExpertForesight, 1, 2), 1);
        quantityInForesightExpert.setFixedCharWidth(3);

        inProjectForesightExpert = addJProp(baseGroup, "inProjectForesightExpert", "Вкл.", baseLM.and1, inProjectForesight, 1, 2, inExpertForesight, 3, 2);
        quantityForesightProjectExpert = addSGProp(baseGroup, "quantityForesightProjectExpert", "Кол-во форсайтов",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inProjectForesightExpert, 1, 2, 3), 1, 3);

        generateSIDExpert = addAProp(actionGroup, new GenerateExpertSIDsActionProperty());

        // project
        claimerProject = addDProp(idGroup, "claimerProject", "Заявитель (ИД)", claimer, project);
        emailClaimerProject = addJProp(claimerInformationGroup, "emailClaimerProject", "E-mail заявителя", emailClaimer, claimerProject, 1);
        statementClaimerProject = addJProp("statementClaimerProject", "Заявление", statementClaimer, claimerProject, 1);
        constituentClaimerProject = addJProp("constituentClaimerProject", "Учредительные документы", constituentClaimer, claimerProject, 1);
        extractClaimerProject = addJProp("extractClaimerProject", "Выписка", extractClaimer, claimerProject, 1);

        firmNameNativeClaimerProject = addJProp(claimerInformationGroup, "firmNameNativeClaimerProject", "Фирменное название", firmNameNativeClaimer, claimerProject, 1);
        firmNameForeignClaimerProject = addJProp(claimerInformationGroup, "firmNameForeignClaimerProject", "Brand name", firmNameForeignClaimer, claimerProject, 1);
        phoneClaimerProject = addJProp(claimerInformationGroup, "phoneClaimerProject", "Телефон", phoneClaimer, claimerProject, 1);
        addressClaimerProject  = addJProp(claimerInformationGroup, "addressClaimerProject", "Адрес", addressClaimer, claimerProject, 1);
        postAddressClaimerProject = addJProp(claimerInformationGroup, "postAddressClaimerProject", "Почтовый дрес", postAddressClaimer, claimerProject, 1);
        siteClaimerProject = addJProp(claimerInformationGroup, "siteClaimerProject", "Сайт заявителя", siteClaimer, claimerProject, 1);
        emailFirmClaimerProject = addJProp(claimerInformationGroup, "emailFirmClaimerProject", "E-mail организации", emailFirmClaimer, claimerProject, 1);
        OGRNClaimerProject = addJProp(claimerInformationGroup, "OGRNClaimerProject", "ОГРН заявителя", OGRNClaimer, claimerProject, 1);
        INNClaimerProject = addJProp(claimerInformationGroup, "INNClaimerProject", "ИНН заявителя", INNClaimer, claimerProject, 1);

        claimerVote = addJProp(idGroup, "claimerVote", "Заявитель (ИД)", claimerProject, projectVote, 1);

        sidProject = addDProp(projectInformationGroup, "sidProject", "Внешний идентификатор проекта", StringClass.get(10), project);
        sidToProject = addAGProp("sidToProject", "SID проекта", sidProject);

        nameNativeManagerProject = addDProp(projectInformationGroup, "nameNativeManagerProject", "ФИО руководителя проекта", InsensitiveStringClass.get(2000), project);
        nameNativeManagerProject.setMinimumWidth(10);
        nameNativeManagerProject.setPreferredWidth(50);

        nameForeignManagerProject = addDProp(projectInformationGroup, "nameForeignManagerProject", "Full name manager project", InsensitiveStringClass.get(2000), project);
        nameForeignManagerProject.setMinimumWidth(10);
        nameForeignManagerProject.setPreferredWidth(50);

        nameNativeCorrectManagerProject = addDProp(baseGroup, "nameNativeCorrectManagerProject", "ФИО руководителя проекта (испр.)", InsensitiveStringClass.get(2000), project);
        nameForeignCorrectManagerProject = addDProp(baseGroup, "nameForeignCorrectManagerProject", "Full name manager project (corr.)", InsensitiveStringClass.get(2000), project);

        nameNativeUnionManagerProject = addSUProp(baseGroup, "nameNativeUnionManagerProject", "ФИО руководителя проекта", Union.OVERRIDE, nameNativeManagerProject, nameNativeCorrectManagerProject);
        nameNativeUnionManagerProject.setMinimumWidth(10);
        nameNativeUnionManagerProject.setPreferredWidth(50);

        nameForeignUnionManagerProject = addSUProp(baseGroup, "nameForeignUnionManagerProject", "Full name project manager", Union.OVERRIDE, nameForeignManagerProject, nameForeignCorrectManagerProject);
        nameForeignUnionManagerProject.setMinimumWidth(10);
        nameForeignUnionManagerProject.setPreferredWidth(50);

        nameNativeGenitiveManagerProject = addDProp(projectOptionsGroup, "nameNativeGenitiveManagerProject", "ФИО руководителя проекта (Кого)", InsensitiveStringClass.get(2000), project);
        nameNativeGenitiveManagerProject.setMinimumWidth(10);
        nameNativeGenitiveManagerProject.setPreferredWidth(50);

        nameGenitiveManagerProject = addSUProp("nameGenitiveManagerProject", "Заявитель (Кого)", Union.OVERRIDE, nameNativeUnionManagerProject, nameNativeGenitiveManagerProject);
        nameGenitiveManagerProject.setMinimumWidth(10);
        nameGenitiveManagerProject.setPreferredWidth(50);

        nameNativeDativusManagerProject = addDProp(projectOptionsGroup, "nameNativeDativusManagerProject", "ФИО руководителя проекта (Кому)", InsensitiveStringClass.get(2000), project);
        nameNativeDativusManagerProject.setMinimumWidth(10);
        nameNativeDativusManagerProject.setPreferredWidth(50);

        nameDativusManagerProject = addSUProp("nameDativusManagerProject", "Заявитель (Кому)", Union.OVERRIDE, nameNativeUnionManagerProject, nameNativeDativusManagerProject);
        nameDativusManagerProject.setMinimumWidth(10);
        nameDativusManagerProject.setPreferredWidth(50);

        nameNativeAblateManagerProject = addDProp(projectOptionsGroup, "nameNativeAblateManagerProject", "ФИО руководителя проекта (Кем)", InsensitiveStringClass.get(2000), project);
        nameNativeAblateManagerProject.setMinimumWidth(10);
        nameNativeAblateManagerProject.setPreferredWidth(50);
        //makeLoggable(nameNativeAblateManagerProject);

        nameAblateManagerProject = addSUProp("nameAblateManagerProject", "Заявитель (Кем)", Union.OVERRIDE, nameNativeUnionManagerProject, nameNativeAblateManagerProject);
        nameAblateManagerProject.setMinimumWidth(10);
        nameAblateManagerProject.setPreferredWidth(50);

        projectActionProject = addDProp(idGroup, "projectActionProject", "Тип заявки (ИД)", projectAction, project);
        nameProjectActionProject = addJProp(projectInformationGroup, "nameProjectActionProject", "Тип заявки", baseLM.name, projectActionProject, 1);
        nameProjectActionProject.setPreferredCharWidth(10);
                                  // Анкета
        projectMissionProject = addDProp("projectMissionProject", "Цель заявки (ИД)", projectMission, project);
        nameProjectMissionProject = addJProp(descGroup, "nameProjectMissionProject", "Цель заявки", baseLM.name, projectMissionProject, 1);
        nameProjectMissionProject.setPreferredCharWidth(10);

        descriptionProjectMission = addDProp(descGroup, "descriptionProjectMission", "Description", InsensitiveStringClass.get(2000), projectMission);
        foreignProjectMissionProject = addJProp(descGroup, "foreignProjectMissionProject", "Objectives of the project", descriptionProjectMission, projectMissionProject, 1);
        foreignProjectMissionProject.setMinimumWidth(10);
        foreignProjectMissionProject.setPreferredWidth(50);

        nativeCommentMissionProject = addDProp(descGroup, "nativeCommentMissionProject", "Комментарий к цели", InsensitiveStringClass.get(2000), project);
        nativeCommentMissionProject.setMinimumWidth(10);
        nativeCommentMissionProject.setPreferredWidth(50);

        foreignCommentMissionProject = addDProp(descGroup, "foreignCommentMissionProject", "Comment for mission", InsensitiveStringClass.get(2000), project);
        foreignCommentMissionProject.setMinimumWidth(10);
        foreignCommentMissionProject.setPreferredWidth(50);

        nativeResumeProject = addDProp(descGroup, "nativeResumeProject", "Резюме проекта", InsensitiveStringClass.get(500), project);
        nativeResumeProject.setMinimumWidth(10);
        nativeResumeProject.setPreferredWidth(50);

        foreignResumeProject = addDProp(descGroup, "foreignResumeProject", "Resume of the project", InsensitiveStringClass.get(500), project);
        foreignResumeProject.setMinimumWidth(10);
        foreignResumeProject.setPreferredWidth(50);

        nameNativeContactProject = addDProp(descGroup, "nameNativeContactProject", "ФИО контактного лица по проекту", InsensitiveStringClass.get(2000), project);
        nameNativeContactProject.setMinimumWidth(10);
        nameNativeContactProject.setPreferredWidth(50);

        nameForeignContactProject = addDProp(descGroup, "nameForeignContactProject", "Contact the project", InsensitiveStringClass.get(2000), project);
        nameForeignContactProject.setMinimumWidth(10);
        nameForeignContactProject.setPreferredWidth(50);

        phoneContactProject = addDProp(descGroup, "phoneContactProject", "Телефон контактного лица", StringClass.get(50), project);
        emailContactProject = addDProp(descGroup, "emailContactProject", "e-mail контактного лица", StringClass.get(50), project);

        projectResearch = addDProp("projectResearch", "Проект Исследования (ИД)", project, research);
        equalsResearchProject = addJProp("equalsResearchProject", "Вкл", baseLM.equals2, projectResearch, 1, 2);

        nativeCommentResearch = addDProp(problemGroup, "nativeCommentResearch", "Комментарий, навзвание публикации", InsensitiveStringClass.get(2000), research);
        nativeCommentResearch.setMinimumWidth(10);
        nativeCommentResearch.setPreferredWidth(50);

        nativeCommentToResearch = addAGProp("nativeCommentToResearch", "Исследование (номер комментария, внутренний ID проекта)", nativeCommentResearch, projectResearch);
        nativeCommentSIDToResearch = addJProp("nativeCommentSIDToResearch", "Исследование (номер комментария, внешний ID проекта)", nativeCommentToResearch, 1, sidToProject, 2);

        foreignCommentResearch = addDProp(problemGroup, "foreignCommentResearch", "Comment research", InsensitiveStringClass.get(2000), research);
        foreignCommentResearch.setMinimumWidth(10);
        foreignCommentResearch.setPreferredWidth(50);

        foreignCommentToResearch = addAGProp("foreignCommentToResearch", "Исследование (номер комментария, внутренний ID проекта) (иностр.)", foreignCommentResearch, projectResearch);
        foreignCommentSIDToResearch = addJProp("foreignCommentSIDToResearch", "Исследование (номер комментария, внешний ID проекта) (иностр.)", foreignCommentToResearch, 1, sidToProject, 2);

        dataResearch = addDProp(problemGroup, "dataResearch", "Ссылка", InsensitiveStringClass.get(2000), research);
        dataResearch.setMinimumWidth(10);
        dataResearch.setPreferredWidth(50);

        projectPublications = addDProp("projectPublications", "Проект публикации (ИД)", project, publications);
        equalsPublicationsProject = addJProp("equalsPublicationsProject", "Вкл", baseLM.equals2, projectPublications, 1, 2);

        nativePublications = addDProp(publicationsGroup, "nativePublications", "Название", InsensitiveStringClass.get(2000), publications);
        nativePublications.setMinimumWidth(10);
        nativePublications.setPreferredWidth(50);

        nativeToPublications = addAGProp("nativeToPublications", "Публикация", nativePublications, projectPublications);
        nativeSIDToPublications = addJProp("nativeSIDToPublications", "Публикация", nativeToPublications, 1, sidToProject, 2);

        foreignPublications = addDProp(publicationsGroup, "foreignPublications", "Publications", InsensitiveStringClass.get(2000), publications);
        foreignPublications.setMinimumWidth(10);
        foreignPublications.setPreferredWidth(50);

        foreignToPublications = addAGProp("foreignToPublications", "Публикация (иностр.)", foreignPublications, projectPublications);
        foreignSIDToPublications = addJProp("foreignSIDToPublications", "Публикация (иностр.)", foreignToPublications, 1, sidToProject, 2);

        nativeAuthorPublications = addDProp(publicationsGroup, "nativeAuthorPublications", "Автор", InsensitiveStringClass.get(2000), publications);
        nativeAuthorPublications.setMinimumWidth(10);
        nativeAuthorPublications.setPreferredWidth(50);

        foreignAuthorPublications = addDProp(publicationsGroup, "foreignAuthorPublications", "Author", InsensitiveStringClass.get(2000), publications);
        foreignAuthorPublications.setMinimumWidth(10);
        foreignAuthorPublications.setPreferredWidth(50);

        nativeEditionPublications = addDProp(publicationsGroup, "nativeEditionPublications", "Издание", InsensitiveStringClass.get(2000), publications);
        nativeEditionPublications.setMinimumWidth(10);
        nativeEditionPublications.setPreferredWidth(50);

        foreignEditionPublications = addDProp(publicationsGroup, "foreignEditionPublications", "Edition", InsensitiveStringClass.get(2000), publications);
        foreignEditionPublications.setMinimumWidth(10);
        foreignEditionPublications.setPreferredWidth(50);

        datePublications = addDProp(publicationsGroup, "datePublications", "Год", IntegerClass.instance, publications);
        nativeLinksPublications = addDProp(publicationsGroup, "nativeLinksPublications", "Ссылка на эл.версию", InsensitiveStringClass.get(2000), publications);
        nativeLinksPublications.setMinimumWidth(10);
        nativeLinksPublications.setPreferredWidth(50);

        projectCommercialization = addDProp("projectCommercialization", "Проект коммерциализации (ИД)", project, commercialization);
        equalsCommercializationProject = addJProp("equalsCommercializationProject", "Вкл", baseLM.equals2, projectCommercialization, 1, 2);

        nativeProjectCommercialization = addDProp(commercializationGroup, "nativeProjectCommercialization", "Основные направления коммерциализации проекта", InsensitiveStringClass.get(2000), commercialization);
        nativeProjectCommercialization.setMinimumWidth(10);
        nativeProjectCommercialization.setPreferredWidth(50);

        nativeProjectToCommercialization = addAGProp("nativeProjectToCommercialization", "Коммерциализация", nativeProjectCommercialization, projectCommercialization);
        nativeProjectSIDToCommercialization = addJProp("nativeProjectSIDToCommercialization", "Коммерциализация", nativeProjectToCommercialization, 1, sidToProject, 2);

        foreignProjectCommercialization = addDProp(commercializationGroup, "foreignProjectCommercialization", "Main areas of commercialization of the project", InsensitiveStringClass.get(2000), commercialization);
        foreignProjectCommercialization.setMinimumWidth(10);
        foreignProjectCommercialization.setPreferredWidth(50);

        foreignProjectToCommercialization = addAGProp("foreignProjectToCommercialization", "Коммерциализация (иностр.)", foreignProjectCommercialization, projectCommercialization);
        foreignProjectSIDToCommercialization = addJProp("foreignProjectSIDToCommercialization", "Коммерциализация (иностр.)", foreignProjectToCommercialization, 1, sidToProject, 2);

        nativeCommentProjectCommercialization = addDProp(commercializationGroup, "nativeCommentProjectCommercialization", "Комментарий", InsensitiveStringClass.get(2000), commercialization);
        nativeCommentProjectCommercialization.setMinimumWidth(10);
        nativeCommentProjectCommercialization.setPreferredWidth(50);

        foreignCommentProjectCommercialization = addDProp(commercializationGroup, "foreignCommentProjectCommercialization", "Comment", InsensitiveStringClass.get(2000), commercialization);
        foreignCommentProjectCommercialization.setMinimumWidth(10);
        foreignCommentProjectCommercialization.setPreferredWidth(50);

        nativeMarketTrendsProject = addDProp(problemGroup, "nativeMarketTrendsProject", "Описание рыночных трендов", InsensitiveStringClass.get(4000), project);
        nativeMarketTrendsProject.setMinimumWidth(10);
        nativeMarketTrendsProject.setPreferredWidth(50);

        foreignMarketTrendsProject = addDProp(problemGroup, "foreignMarketTrendsProject", "Description of market trends", InsensitiveStringClass.get(4000), project);
        foreignMarketTrendsProject.setMinimumWidth(10);
        foreignMarketTrendsProject.setPreferredWidth(50);

        linksMarketTrendsProject = addDProp(problemGroup, "linksMarketTrendsProject", "Ссылки на рыночные тренды", InsensitiveStringClass.get(2000), project);
        linksMarketTrendsProject.setMinimumWidth(10);
        linksMarketTrendsProject.setPreferredWidth(50);

        linksAnalogProject = addDProp(problemGroup, "linksAnalogProject", "Ссылки на аналоги", InsensitiveStringClass.get(2000), project);
        linksAnalogProject.setMinimumWidth(10);
        linksAnalogProject.setPreferredWidth(50);

        nativeRelevanceProject = addDProp(problemGroup, "nativeRelevanceProject", "Обоснование актуальности проекта", InsensitiveStringClass.get(4000), project);
        nativeRelevanceProject.setMinimumWidth(10);
        nativeRelevanceProject.setPreferredWidth(50);

        foreignRelevanceProject = addDProp(problemGroup, "foreignRelevanceProject", "Study the relevance of the project", InsensitiveStringClass.get(4000), project);
        foreignRelevanceProject.setMinimumWidth(10);
        foreignRelevanceProject.setPreferredWidth(50);

        nativeBasicTechnologyProject = addDProp(problemGroup, "nativeBasicTechnologyProject", "Описание базовой технологии проекта", InsensitiveStringClass.get(20000), project);
        nativeBasicTechnologyProject.setMinimumWidth(10);
        nativeBasicTechnologyProject.setPreferredWidth(50);

        foreignBasicTechnologyProject = addDProp(problemGroup, "foreignBasicTechnologyProject", "Description of the underlying technology of the project", InsensitiveStringClass.get(20000), project);
        foreignBasicTechnologyProject.setMinimumWidth(10);
        foreignBasicTechnologyProject.setPreferredWidth(50);

        nativeCaseStudiesProject = addDProp(commercializationGroup, "nativeCaseStudiesProject", "Примеры внедрения аналогичных разработок", InsensitiveStringClass.get(2000), project);
        nativeCaseStudiesProject.setMinimumWidth(10);
        nativeCaseStudiesProject.setPreferredWidth(50);

        foreignCaseStudiesProject = addDProp(commercializationGroup, "foreignCaseStudiesProject", "Case studies of similar developments", InsensitiveStringClass.get(2000), project);
        foreignCaseStudiesProject.setMinimumWidth(10);
        foreignCaseStudiesProject.setPreferredWidth(50);

        projectAnalogues = addDProp("projectAnalogues", "Проект аналога (ИД)", project, analogues);
        equalsAnaloguesProject = addJProp("equalsAnaloguesProject", "Вкл", baseLM.equals2, projectAnalogues, 1, 2);

        nativeProjectAnalogues = addDProp(analoguesGroup, "nativeProjectAnalogues", "Аналоги проекта и его преимущество", InsensitiveStringClass.get(2000), analogues);
        nativeProjectAnalogues.setMinimumWidth(10);
        nativeProjectAnalogues.setPreferredWidth(50);

        nativeProjectToAnalogues = addAGProp("nativeProjectToAnalogues", "Аналоги", nativeProjectAnalogues, projectAnalogues);
        nativeProjectSIDToAnalogues = addJProp("nativeProjectSIDToAnalogues", "Аналоги", nativeProjectToAnalogues, 1, sidToProject, 2);

        foreignProjectAnalogues = addDProp(analoguesGroup, "foreignProjectAnalogues", "Analogues of the project and its benefit", InsensitiveStringClass.get(2000), analogues);
        foreignProjectAnalogues.setMinimumWidth(10);
        foreignProjectAnalogues.setPreferredWidth(50);

        foreignProjectToAnalogues = addAGProp("foreignProjectToAnalogues", "Аналоги (иностр.)", foreignProjectAnalogues, projectAnalogues);
        foreignProjectSIDToAnalogues = addJProp("foreignProjectSIDToAnalogues", "Аналоги (иностр.)", foreignProjectToAnalogues, 1, sidToProject, 2);

        nativeDescriptionProjectAnalogues = addDProp(analoguesGroup, "nativeDescriptionProjectAnalogues", "Описание", InsensitiveStringClass.get(2000), analogues);
        nativeDescriptionProjectAnalogues.setMinimumWidth(10);
        nativeDescriptionProjectAnalogues.setPreferredWidth(50);

        foreignDescriptionProjectAnalogues = addDProp(analoguesGroup, "foreignDescriptionProjectAnalogues", "Description", InsensitiveStringClass.get(2000), analogues);
        foreignDescriptionProjectAnalogues.setMinimumWidth(10);
        foreignDescriptionProjectAnalogues.setPreferredWidth(50);

        nativeCharacteristicsProjectAnalogues = addDProp(analoguesGroup, "nativeCharacteristicsProjectAnalogues", "Характеристики", InsensitiveStringClass.get(2000), analogues);
        nativeCharacteristicsProjectAnalogues.setMinimumWidth(10);
        nativeCharacteristicsProjectAnalogues.setPreferredWidth(50);

        foreignCharacteristicsProjectAnalogues = addDProp(analoguesGroup, "foreignCharacteristicsProjectAnalogues", "Characteristics", InsensitiveStringClass.get(2000), analogues);
        foreignCharacteristicsProjectAnalogues.setMinimumWidth(10);
        foreignCharacteristicsProjectAnalogues.setPreferredWidth(50);

        nativeCharacteristicsAnaloguesProject = addDProp(analoguesGroup, "nativeCharacteristicsAnaloguesProject", "Характеристики рынка аналогичных решений", InsensitiveStringClass.get(2000), project);
        nativeCharacteristicsAnaloguesProject.setMinimumWidth(10);
        nativeCharacteristicsAnaloguesProject.setPreferredWidth(50);

        foreignCharacteristicsAnaloguesProject = addDProp(analoguesGroup, "foreignCharacteristicsAnaloguesProject", "Market characteristics of these similar solutions", InsensitiveStringClass.get(2000), project);
        foreignCharacteristicsAnaloguesProject.setMinimumWidth(10);
        foreignCharacteristicsAnaloguesProject.setPreferredWidth(50);

        nativeCompaniesAnaloguesProject = addDProp(analoguesGroup, "nativeCompaniesAnaloguesProject", "Компании, ведущие аналогичные или близкие разработки", InsensitiveStringClass.get(2000), project);
        nativeCompaniesAnaloguesProject.setMinimumWidth(10);
        nativeCompaniesAnaloguesProject.setPreferredWidth(50);

        foreignCompaniesAnaloguesProject = addDProp(analoguesGroup, "foreignCompaniesAnaloguesProject", "Companies engaged in similar or closely related development", InsensitiveStringClass.get(2000), project);
        foreignCompaniesAnaloguesProject.setMinimumWidth(10);
        foreignCompaniesAnaloguesProject.setPreferredWidth(50);

        nativeMarketIntroductionProject = addDProp(analoguesGroup, "nativeMarketIntroductionProject", "Рынки для реализации данного проекта", InsensitiveStringClass.get(2000), project);
        nativeMarketIntroductionProject.setMinimumWidth(10);
        nativeMarketIntroductionProject.setPreferredWidth(50);

        foreignMarketIntroductionProject = addDProp(analoguesGroup, "foreignMarketIntroductionProject", "Markets for this project", InsensitiveStringClass.get(2000), project);
        foreignMarketIntroductionProject.setMinimumWidth(10);
        foreignMarketIntroductionProject.setPreferredWidth(50);

        linksMarketIntroductionProject = addDProp(analoguesGroup, "linksMarketIntroductionProject", "Ссылки на исследования рынка сбыта", InsensitiveStringClass.get(2000), project);
        linksMarketIntroductionProject.setMinimumWidth(10);
        linksMarketIntroductionProject.setPreferredWidth(50);

        linksForeignMarketIntroductionProject = addDProp(analoguesGroup, "linksForeignMarketIntroductionProject", "Links to research market", InsensitiveStringClass.get(2000), project);
        linksForeignMarketIntroductionProject.setMinimumWidth(10);
        linksForeignMarketIntroductionProject.setPreferredWidth(50);

        nativeHistoryProject = addDProp(historyGroup, "nativeHistoryProject", "История возникновения проекта", InsensitiveStringClass.get(2000), project);
        nativeHistoryProject.setMinimumWidth(10);
        nativeHistoryProject.setPreferredWidth(50);

        foreignHistoryProject = addDProp(historyGroup, "foreignHistoryProject", "The history of the project", InsensitiveStringClass.get(2000), project);
        foreignHistoryProject.setMinimumWidth(10);
        foreignHistoryProject.setPreferredWidth(50);

        nativeDynamicsProject = addDProp(historyGroup, "nativeDynamicsProject", "Динамика развития проекта", InsensitiveStringClass.get(2000), project);
        nativeDynamicsProject.setMinimumWidth(10);
        nativeDynamicsProject.setPreferredWidth(50);

        foreignDynamicsProject = addDProp(historyGroup, "foreignDynamicsProject", "The dynamics of the development project", InsensitiveStringClass.get(2000), project);
        foreignDynamicsProject.setMinimumWidth(10);
        foreignDynamicsProject.setPreferredWidth(50);

        nativeGrantsProject = addDProp(historyGroup, "nativeGrantsProject", "Получали ли члены команды проекта гранты", InsensitiveStringClass.get(2000), project);
        nativeGrantsProject.setMinimumWidth(10);
        nativeGrantsProject.setPreferredWidth(50);

        foreignGrantsProject = addDProp(historyGroup, "foreignGrantsProject", "Have the project team grants", InsensitiveStringClass.get(2000), project);
        foreignGrantsProject.setMinimumWidth(10);
        foreignGrantsProject.setPreferredWidth(50);

        nativeLaboratoryProject = addDProp(historyGroup, "nativeLaboratoryProject", "Лаборатория, на которой планируется реализация проекта", InsensitiveStringClass.get(2000), project);
        nativeLaboratoryProject.setMinimumWidth(10);
        nativeLaboratoryProject.setPreferredWidth(50);

        foreignLaboratoryProject = addDProp(historyGroup, "foreignLaboratoryProject", "Laboratory, which is planned to implement the project", InsensitiveStringClass.get(2000), project);
        foreignLaboratoryProject.setMinimumWidth(10);
        foreignLaboratoryProject.setPreferredWidth(50);

        nativeInvestmentProject = addDProp(historyGroup, "nativeInvestmentProject", "Привлекалось ли венчурное и (или) иное финансирование", InsensitiveStringClass.get(2000), project);
        nativeInvestmentProject.setMinimumWidth(10);
        nativeInvestmentProject.setPreferredWidth(50);

        foreignInvestmentProject = addDProp(historyGroup, "foreignInvestmentProject", "Attention was whether the venture capital and (or) other fundingt", InsensitiveStringClass.get(2000), project);
        foreignInvestmentProject.setMinimumWidth(10);
        foreignInvestmentProject.setPreferredWidth(50);

        nativeResultsProject = addDProp(baseGroup, "nativeResultsProject", "Какие результаты уже достигнуты", InsensitiveStringClass.get(2000), project);
        nativeResultsProject.setMinimumWidth(10);
        nativeResultsProject.setPreferredWidth(50);

        foreignResultsProject = addDProp(baseGroup, "foreignResultsProject", "What results have been achieved", InsensitiveStringClass.get(2000), project);
        foreignResultsProject.setMinimumWidth(10);
        foreignResultsProject.setPreferredWidth(50);

        projectObjectives = addDProp("projectObjectives", "Проект цели (ИД)", project, objectives);
        equalsObjectivesProject = addJProp("equalsObjectivesProject", "Вкл", baseLM.equals2, projectObjectives, 1, 2);

        nativeProjectObjectives = addDProp(projectmissionGroup, "nativeProjectObjectives", "Ключевые цели проекта", InsensitiveStringClass.get(200), objectives);
        nativeProjectObjectives.setMinimumWidth(10);
        nativeProjectObjectives.setPreferredWidth(50);

        nativeProjectToObjectives = addAGProp("nameNativeToObjectives", "Цели проекта", nativeProjectObjectives, projectObjectives);
        nativeProjectSIDToObjectives = addJProp("nativeProjectSIDToObjectives", "Цели проекта", nativeProjectToObjectives, 1, sidToProject, 2);

        foreignProjectObjectives = addDProp(projectmissionGroup, "foreignProjectObjectives", "Describe the key objectives of the project", InsensitiveStringClass.get(200), objectives);
        foreignProjectObjectives.setMinimumWidth(10);
        foreignProjectObjectives.setPreferredWidth(50);

        foreignProjectToObjectives = addAGProp("foreignProjectToObjectives", "Цели проекта (иностр.)", foreignProjectObjectives, projectObjectives);
        foreignProjectSIDToObjectives = addJProp("foreignProjectSIDToObjectives", "Цели проекта (иностр.)", foreignProjectToObjectives, 1, sidToProject, 2);

        nativeGeneralizedPlanProject = addDProp(projectmissionGroup, "nativeGeneralizedPlanProject", "Обобщенный план последующего развития", InsensitiveStringClass.get(2000), project);
        nativeGeneralizedPlanProject.setMinimumWidth(10);
        nativeGeneralizedPlanProject.setPreferredWidth(50);

        foreignGeneralizedPlanProject = addDProp(projectmissionGroup, "foreignGeneralizedPlanProject", "A generalized plan of future development", InsensitiveStringClass.get(2000), project);
        foreignGeneralizedPlanProject.setMinimumWidth(10);
        foreignGeneralizedPlanProject.setPreferredWidth(50);

                          // команда
        projectSpecialist = addDProp("projectSpecialist", "Проект члена команды (ИД)", project, specialist);
        equalsSpecialistProject = addJProp("equalsSpecialistProject", "Вкл", baseLM.equals2, projectSpecialist, 1, 2);

        nameNativeSpecialist = addDProp(teamGroup, "nameNativeSpecialist", "ФИО", InsensitiveStringClass.get(2000), specialist);
        nameNativeSpecialist.setMinimumWidth(10);
        nameNativeSpecialist.setPreferredWidth(50);

        nameNativeToSpecialist = addAGProp("nameNativeToSpecialist", "Специалист", nameNativeSpecialist, projectSpecialist);
        nameNativeSIDToSpecialist = addJProp("nameNativeSIDToSpecialist", "Специалист", nameNativeToSpecialist, 1, sidToProject, 2);

        nameForeignSpecialist = addDProp(teamGroup, "nameForeignSpecialist", "The name of the specialist", InsensitiveStringClass.get(2000), specialist);
        nameForeignSpecialist.setMinimumWidth(10);
        nameForeignSpecialist.setPreferredWidth(50);

        nameForeignToSpecialist = addAGProp("nameForeignToSpecialist", "Специалист (иностр.)", nameForeignSpecialist, projectSpecialist);
        nameForeignSIDToSpecialist = addJProp("nameForeignSIDToSpecialist", "Специалист (иностр.)", nameForeignToSpecialist, 1, sidToProject, 2);

        nativePostSpecialist = addDProp(teamGroup, "nativePostSpecialist", "Роль в прокте, должность в компании", InsensitiveStringClass.get(170), specialist);
        nativePostSpecialist.setMinimumWidth(10);
        nativePostSpecialist.setPreferredWidth(50);

        foreignPostSpecialist = addDProp(teamGroup, "foreignPostSpecialist", "The role of proctitis, position in the company", InsensitiveStringClass.get(170), specialist);
        foreignPostSpecialist.setMinimumWidth(10);
        foreignPostSpecialist.setPreferredWidth(50);

        nativeFunctionSpecialist = addDProp(teamGroup, "nativeFunctionSpecialist", "Описание функций и задач в рамках проекта", InsensitiveStringClass.get(2000), specialist);
        nativeFunctionSpecialist.setMinimumWidth(10);
        nativeFunctionSpecialist.setPreferredWidth(50);

        foreignFunctionSpecialist = addDProp(teamGroup, "foreignFunctionSpecialist", "Description of the functions and tasks within the project", InsensitiveStringClass.get(2000), specialist);
        foreignFunctionSpecialist.setMinimumWidth(10);
        foreignFunctionSpecialist.setPreferredWidth(50);

        nativeScopeSpecialist = addDProp(teamGroup, "nativeScopeSpecialist", "Сфера деятельности и профессиональные достижения", InsensitiveStringClass.get(2000), specialist);
        nativeScopeSpecialist.setMinimumWidth(10);
        nativeScopeSpecialist.setPreferredWidth(50);

        foreignScopeSpecialist = addDProp(teamGroup, "foreignScopeSpecialist", "Scope and professional achievements", InsensitiveStringClass.get(2000), specialist);
        foreignScopeSpecialist.setMinimumWidth(10);
        foreignScopeSpecialist.setPreferredWidth(50);

        nativeExperienceSpecialist = addDProp(teamGroup, "nativeExperienceSpecialist", "Ключевой опыт, имеющий отношение к области данного проекта", InsensitiveStringClass.get(2000), specialist);
        nativeExperienceSpecialist.setMinimumWidth(10);
        nativeExperienceSpecialist.setPreferredWidth(50);

        foreignExperienceSpecialist = addDProp(teamGroup, "foreignExperienceSpecialist", "A key experience relevant to the area of the project", InsensitiveStringClass.get(2000), specialist);
        foreignExperienceSpecialist.setMinimumWidth(10);
        foreignExperienceSpecialist.setPreferredWidth(50);

        nativeTitleSpecialist = addDProp(teamGroup, "nativeTitleSpecialist", "Образование (ВУЗ, специальность и т.д.), ученая степень, звание", InsensitiveStringClass.get(2000), specialist);
        nativeTitleSpecialist.setMinimumWidth(10);
        nativeTitleSpecialist.setPreferredWidth(50);

        foreignTitleSpecialist = addDProp(teamGroup, "foreignTitleSpecialist", "Education (college, profession, etc.), academic degree, rank", InsensitiveStringClass.get(2000), specialist);
        foreignTitleSpecialist.setMinimumWidth(10);
        foreignTitleSpecialist.setPreferredWidth(50);

        nativeWorkSpecialist = addDProp(teamGroup, "nativeWorkSpecialist", "Места работы, должности за последние 5 лет", InsensitiveStringClass.get(2000), specialist);
        nativeWorkSpecialist.setMinimumWidth(10);
        nativeWorkSpecialist.setPreferredWidth(50);

        foreignWorkSpecialist = addDProp(teamGroup, "foreignWorkSpecialist", "Jobs, positions for the last 5 years", InsensitiveStringClass.get(2000), specialist);
        foreignWorkSpecialist.setMinimumWidth(10);
        foreignWorkSpecialist.setPreferredWidth(50);

        nativePublicationsSpecialist = addDProp(teamGroup, "nativePublicationsSpecialist", "Научные публикации", InsensitiveStringClass.get(2000), specialist);
        nativePublicationsSpecialist.setMinimumWidth(10);
        nativePublicationsSpecialist.setPreferredWidth(50);

        foreignPublicationsSpecialist = addDProp(teamGroup, "foreignPublicationsSpecialist", "Scientific publications", InsensitiveStringClass.get(2000), specialist);
        foreignPublicationsSpecialist.setMinimumWidth(10);
        foreignPublicationsSpecialist.setPreferredWidth(50);

        nativeCitationSpecialist = addDProp(teamGroup, "nativeCitationSpecialist", "Цитируемость, доклады на международных научных конференциях", InsensitiveStringClass.get(200), specialist);
        nativeCitationSpecialist.setMinimumWidth(10);
        nativeCitationSpecialist.setPreferredWidth(50);

        foreignCitationSpecialist = addDProp(teamGroup, "foreignCitationSpecialist", "Citations, papers at international scientific conferences", InsensitiveStringClass.get(200), specialist);
        foreignCitationSpecialist.setMinimumWidth(10);
        foreignCitationSpecialist.setPreferredWidth(50);

        nativeIntellectualPropertySpecialist = addDProp(teamGroup, "nativeIntellectualPropertySpecialist", "Сведения об объектах интеллектуальной собственности", InsensitiveStringClass.get(2000), specialist);
        nativeIntellectualPropertySpecialist.setMinimumWidth(10);
        nativeIntellectualPropertySpecialist.setPreferredWidth(50);

        foreignIntellectualPropertySpecialist = addDProp(teamGroup, "foreignIntellectualPropertySpecialist", "Information about the objects of intellectual property", InsensitiveStringClass.get(2000), specialist);
        foreignIntellectualPropertySpecialist.setMinimumWidth(10);
        foreignIntellectualPropertySpecialist.setPreferredWidth(50);
//                                               Новая дорожная карта
        projectMileStone = addDProp("projectMileStone", "Проект квартала (ИД)", project, mileStone);
        equalsMileStoneProject = addJProp("equalsMileStoneProject", "Вкл", baseLM.equals2, projectMileStone, 1, 2);

        projectMileStoneYear = addDProp("projectMileStoneYear", "Проект года (ИД)", project, mileStoneYear);
        equalsMileStoneYearProject = addJProp("equalsMileStoneYearProject", "Вкл", baseLM.equals2, projectMileStoneYear, 1, 2);

        nativeMileStoneYear = addDProp("nativeMileStoneYear", "Год", StringClass.get(4), mileStoneYear);
        nativeToMileStoneYear = addAGProp("nativeToMileStoneYear", "Название", nativeMileStoneYear, projectMileStoneYear);
        nativeSIDToMileStoneYear = addJProp("nativeSIDToMileStoneYear", "Дорожная карта", nativeToMileStoneYear, 1, sidToProject, 2);

        nativeMileStone = addDProp("nativeMileStone", "Название", InsensitiveStringClass.get(1), mileStone);
        nativeToMileStone = addAGProp("nativeToMileStone", "Название", nativeMileStone, projectMileStone);
        nativeSIDToMileStone = addJProp("nativeSIDToMileStone", "Дорожная карта", nativeToMileStone, 1, sidToProject, 2);

        nativeTypeMileStone = addDProp("nativeTypeMileStone", "Название раздела на русск.", InsensitiveStringClass.get(2000), typeMileStone);
        nativeTypeMileStone.setMinimumWidth(10);
        nativeTypeMileStone.setPreferredWidth(50);

        orderNumberMileStone = addDProp("orderNumberMileStone", "Порядок", IntegerClass.instance, mileStone);
        orderNumberMileStone.setFixedCharWidth(2);

        nativeDescriptionTypeMileStoneMileStoneMileStoneYear = addDProp("nativeDescriptionTypeMileStoneMileStoneMileStoneYear", "Описание дорожной карты", InsensitiveStringClass.get(1000), typeMileStone, mileStone, mileStoneYear);
        nativeDescriptionTypeMileStoneMileStoneMileStoneYear.setMinimumWidth(10);
        nativeDescriptionTypeMileStoneMileStoneMileStoneYear.setPreferredWidth(50);

        nativeResearchDescriptionTypeMileStoneMileStoneYear = addJProp(true, "nativeResearchDescriptionTypeMileStoneMileStoneYear", "Описание разработки", nativeDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "research and development"), 1, 2);
        nativeProductCreationDescriptionTypeMileStoneMileStoneYear = addJProp(true, "nativeProductCreationDescriptionTypeMileStoneMileStoneYear", "Описание создания продукта", nativeDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "product Creation and Production"), 1, 2);
        nativePlanOnHiringDescriptionTypeMileStoneMileStoneYear = addJProp(true, "nativePlanOnHiringDescriptionTypeMileStoneMileStoneYear", "Описание плана по найму", nativeDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "plan on hiring"), 1, 2);
        nativeLicensingDescriptionTypeMileStoneMileStoneYear = addJProp(true, "nativeLicensingDescriptionTypeMileStoneMileStoneYear", "Описание лицензирования", nativeDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "licensing"), 1, 2);
        nativePromotionDescriptionTypeMileStoneMileStoneYear = addJProp(true, "nativePromotionDescriptionTypeMileStoneMileStoneYear", "Описание продвижения", nativeDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "promotion"), 1, 2);
        nativeSellingDescriptionTypeMileStoneMileStoneYear = addJProp(true, "nativeSellingDescriptionTypeMileStoneMileStoneYear", "Описание продаж", nativeDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "selling"), 1, 2);

        foreignDescriptionTypeMileStoneMileStoneMileStoneYear = addDProp("foreignDescriptionTypeMileStoneMileStoneMileStoneYear", "Description of the road map", InsensitiveStringClass.get(2000), typeMileStone, mileStone, mileStoneYear);
        foreignDescriptionTypeMileStoneMileStoneMileStoneYear.setMinimumWidth(10);
        foreignDescriptionTypeMileStoneMileStoneMileStoneYear.setPreferredWidth(50);

        foreignResearchDescriptionTypeMileStoneMileStoneYear = addJProp(true, "foreignResearchDescriptionTypeMileStoneMileStoneYear", "Описание разработки", foreignDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "research and development"), 1, 2);
        foreignProductCreationDescriptionTypeMileStoneMileStoneYear = addJProp(true, "foreignProductCreationDescriptionTypeMileStoneMileStoneYear", "Описание создания продукта", foreignDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "product Creation and Production"), 1, 2);
        foreignPlanOnHiringDescriptionTypeMileStoneMileStoneYear = addJProp(true, "foreignPlanOnHiringDescriptionTypeMileStoneMileStoneYear", "Описание плана по найму", foreignDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "plan on hiring"), 1, 2);
        foreignLicensingDescriptionTypeMileStoneMileStoneYear = addJProp(true, "foreignLicensingDescriptionTypeMileStoneMileStoneYear", "Описание лицензирования", foreignDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "licensing"), 1, 2);
        foreignPromotionDescriptionTypeMileStoneMileStoneYear = addJProp(true, "foreignPromotionDescriptionTypeMileStoneMileStoneYear", "Описание продвижения", foreignDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "promotion"), 1, 2);
        foreignSellingDescriptionTypeMileStoneMileStoneYear = addJProp(true, "foreignSellingDescriptionTypeMileStoneMileStoneYear", "Описание продаж", foreignDescriptionTypeMileStoneMileStoneMileStoneYear, addCProp(typeMileStone, "selling"), 1, 2);

        foreignTypeMileStone = addDProp("foreignTypeMileStone", "Section title", InsensitiveStringClass.get(2000), typeMileStone);
        foreignTypeMileStone.setMinimumWidth(10);
        foreignTypeMileStone.setPreferredWidth(50);

        fileStatementSpecialist = addDProp("fileStatementSpecialist", "Файл заявления", DynamicFormatFileClass.instance, specialist);
        loadFileStatementSpecialist = addLFAProp(teamGroup, "Загрузить файл заявления", fileStatementSpecialist);
        openFileStatementSpecialist = addOFAProp(teamGroup, "Открыть файл заявления", fileStatementSpecialist);

        filePassportSpecialist = addDProp("filePassportSpecialist", "Файл документа, удостоверяющего личность", DynamicFormatFileClass.instance, specialist);
        loadFilePassportSpecialist = addLFAProp(teamGroup, "Загрузить файл документа, удостоверяющего личность", filePassportSpecialist);
        openFilePassportSpecialist = addOFAProp(teamGroup, "Открыть файл документа, удостоверяющего личность", filePassportSpecialist);

        isStatusProject = addJProp("isStatusProject", "На статус участника", baseLM.equals2, projectActionProject, 1, addCProp(projectAction, "status", project), 1);
        isPreliminaryProject = addJProp("isPreliminaryProject", "На предварительную экспертизу", baseLM.equals2, projectActionProject, 1, addCProp(projectAction, "preliminary", project), 1);

        nameNativeCorrectClaimer = addDProp("nameNativeCorrectClaimer", "Полное наименование организации", InsensitiveStringClass.get(2000), claimer);

        nameNativeClaimer = addSUProp("nameNativeClaimer", "Заявитель", Union.OVERRIDE, nameNativeJoinClaimer, nameNativeCorrectClaimer);
        nameNativeClaimer.setMinimumWidth(10);
        nameNativeClaimer.setPreferredWidth(50);

        nameNativeJoinClaimerProject = addJProp(baseGroup, true, "nameNativeJoinClaimerProject", "Заявитель", nameNativeClaimer, claimerProject, 1);

        nameNativeClaimerProject = addIfElseUProp(projectInformationGroup, "nameNativeClaimerProject", "Заявитель", nameNativeUnionManagerProject, nameNativeJoinClaimerProject, isPreliminaryProject, 1);
        nameNativeClaimerProject.setMinimumWidth(10);
        nameNativeClaimerProject.setPreferredWidth(120);

        nameNativeCorrectClaimerProject = addJProp(baseGroup, true, "nameNativeCorrectClaimerProject", "Полное наименование организации", nameNativeCorrectClaimer, claimerProject, 1);
        nameNativeCorrectHighlightClaimerProject = addIfElseUProp(baseGroup, "nameNativeCorrectHighlightClaimerProject", "Заявитель", nameNativeCorrectManagerProject, nameNativeCorrectClaimerProject, isPreliminaryProject, 1);

        nameForeignCorrectClaimer = addDProp("nameForeignCorrectClaimer", "Full name of the Organisation", InsensitiveStringClass.get(2000), claimer);

        nameForeignClaimer = addSUProp("nameForeignClaimer", "Claimer", Union.OVERRIDE, nameForeignJoinClaimer, nameForeignCorrectClaimer);
        nameForeignClaimer.setMinimumWidth(10);
        nameForeignClaimer.setPreferredWidth(50);

        nameForeignJoinClaimerProject = addJProp(baseGroup, true, "nameForeignJoinClaimerProject", "Claimer", nameForeignClaimer, claimerProject, 1);

        nameForeignClaimerProject = addIfElseUProp(projectInformationGroup, "nameForeignClaimerProject", "Claimer", nameForeignUnionManagerProject, nameForeignJoinClaimerProject, isPreliminaryProject, 1);
        nameForeignClaimerProject.setMinimumWidth(10);
        nameForeignClaimerProject.setPreferredWidth(120);

        nameForeignCorrectClaimerProject = addJProp(baseGroup, true, "nameForeignCorrectClaimerProject", "Full name of the Organisation", nameForeignCorrectClaimer, claimerProject, 1);
        nameForeignCorrectHighlightClaimerProject = addIfElseUProp(baseGroup, "nameForeignCorrectHighlightClaimerProject", "Claimer", nameForeignCorrectManagerProject, nameForeignCorrectClaimerProject, isPreliminaryProject, 1);

        nameGenitiveClaimerProject = addIfElseUProp(baseGroup, "nameGenitiveClaimerProject", "Заявитель (кого)", nameGenitiveManagerProject, nameNativeJoinClaimerProject, isPreliminaryProject, 1);
        nameGenitiveClaimerProject.setMinimumWidth(10);
        nameGenitiveClaimerProject.setPreferredWidth(50);
        nameDativusClaimerProject = addIfElseUProp(baseGroup, "nameDativusClaimerProject", "Заявитель (кому)", nameDativusManagerProject, nameNativeJoinClaimerProject, isPreliminaryProject, 1);
        nameDativusClaimerProject.setMinimumWidth(10);
        nameDativusClaimerProject.setPreferredWidth(50);
        nameAblateClaimerProject = addIfElseUProp(baseGroup, "nameAblateClaimerProject", "Заявитель (кем)", nameAblateManagerProject, nameNativeJoinClaimerProject, isPreliminaryProject, 1);
        nameAblateClaimerProject.setMinimumWidth(10);
        nameAblateClaimerProject.setPreferredWidth(50);


        nativeProblemProject = addDProp(innovationGroup, "nativeProblemProject", "Проблема, на решение которой направлен проект", InsensitiveStringClass.get(4000), project);
        nativeProblemProject.setMinimumWidth(10);
        nativeProblemProject.setPreferredWidth(50);
        foreignProblemProject = addDProp(innovationGroup, "foreignProblemProject", "The problem that the project will solve", InsensitiveStringClass.get(4000), project);
        foreignProblemProject.setMinimumWidth(10);
        foreignProblemProject.setPreferredWidth(50);

        nativeInnovativeProject = addDProp(innovationGroup, "nativeInnovativeProject", "Суть инновации", InsensitiveStringClass.get(6000), project);
        nativeInnovativeProject.setMinimumWidth(10);
        nativeInnovativeProject.setPreferredWidth(50);
        foreignInnovativeProject = addDProp(innovationGroup, "foreignInnovativeProject", "Description of the innovation", InsensitiveStringClass.get(6000), project);
        foreignInnovativeProject.setMinimumWidth(10);
        foreignInnovativeProject.setPreferredWidth(50);

        nameForeignProjectType = addDProp(baseGroup, "nameForeignProjectType", "Имя (иностр.)", InsensitiveStringClass.get(110), projectType);

        projectTypeProject = addDProp(idGroup, "projectTypeProject", "Тип проекта (ИД)", projectType, project);
        nameNativeProjectTypeProject = addJProp(innovationGroup, "nameNativeProjectTypeProject", "Тип проекта", baseLM.name, projectTypeProject, 1);
        nameForeignProjectTypeProject = addJProp(innovationGroup, "nameForeignProjectTypeProject", "Тип проекта (иностр.)", nameForeignProjectType, projectTypeProject, 1);

        projectActionVote = addDProp(idGroup, "projectActionVote", "Тип заявки (ИД)", projectAction, vote);
        nameProjectActionVote = addJProp(baseGroup, "nameProjectActionVote", "Тип заявки", baseLM.name, projectActionVote, 1);
        nameProjectActionVote.setPreferredCharWidth(20);

        isStatusVote = addJProp("isStatusVote", "На статус участника", baseLM.equals2, projectActionVote, 1, addCProp(projectAction, "status", vote), 1);
        isPreliminaryVote = addJProp("isPreliminaryVote", "На предварительную экспертизу", baseLM.equals2, projectActionVote, 1, addCProp(projectAction, "preliminary", vote), 1);

        nativeSubstantiationProjectType = addDProp(innovationGroup, "nativeSubstantiationProjectType", "Обоснование выбора", InsensitiveStringClass.get(2000), project);
        nativeSubstantiationProjectType.setMinimumWidth(10);
        nativeSubstantiationProjectType.setPreferredWidth(50);

        foreignSubstantiationProjectType = addDProp(innovationGroup, "foreignSubstantiationProjectType", "Description of choice", InsensitiveStringClass.get(2000), project);
        foreignSubstantiationProjectType.setMinimumWidth(10);
        foreignSubstantiationProjectType.setPreferredWidth(50);

        nameNativeCluster = addJProp("nameNativeCluster", "Кластер", baseLM.and1, nameNative, 1, is(cluster), 1);
        nameNativeToCluster = addAGProp(idGroup, "nameNativeToCluster", "Кластер", nameNativeCluster);
        nativeSubstantiationProjectCluster = addDProp(baseGroup, "nativeSubstantiationProjectCluster", "Обоснование выбора", InsensitiveStringClass.get(2000), project, cluster);
        nativeSubstantiationProjectCluster.setMinimumWidth(10);
        nativeSubstantiationProjectCluster.setPreferredWidth(50);
        foreignSubstantiationProjectCluster = addDProp(baseGroup, "foreignSubstantiationProjectCluster", "Description of choice", InsensitiveStringClass.get(2000), project, cluster);
        foreignSubstantiationProjectCluster.setMinimumWidth(10);
        foreignSubstantiationProjectCluster.setPreferredWidth(50);
        isOtherClusterProject = addDProp(projectOtherClusterGroup, "isOtherClusterProject", "Иной кластер", LogicalClass.instance, project);
        hideIsOtherClusterProject = addHideCaptionProp(privateGroup, "Иной кластер", isOtherClusterProject);
        nativeSubstantiationOtherClusterProject = addDProp(projectOtherClusterGroup, "nativeSubstantiationOtherClusterProject", "Обоснование выбора", InsensitiveStringClass.get(2000), project);
        nativeSubstantiationOtherClusterProject.setMinimumWidth(10);
        nativeSubstantiationOtherClusterProject.setPreferredWidth(50);
        hideNativeSubstantiationOtherClusterProject = addHideCaptionProp(privateGroup, "Укажите", isOtherClusterProject);
        foreignSubstantiationOtherClusterProject = addDProp(projectOtherClusterGroup, "foreignSubstantiationOtherClusterProject", "Description of choice", InsensitiveStringClass.get(2000), project);
        foreignSubstantiationOtherClusterProject.setMinimumWidth(10);
        foreignSubstantiationOtherClusterProject.setPreferredWidth(50);
        hideForeignSubstantiationOtherClusterProject = addHideCaptionProp(privateGroup, "Укажите", isOtherClusterProject);
        fileNativeSummaryProject = addDProp("fileNativeSummaryProject", "Файл резюме проекта", DynamicFormatFileClass.instance, project);
        loadFileNativeSummaryProject = addLFAProp(executiveSummaryGroup, "Загрузить файл резюме проекта", fileNativeSummaryProject);
        openFileNativeSummaryProject = addOFAProp(executiveSummaryGroup, "Открыть файл резюме проекта", fileNativeSummaryProject);

        fileForeignSummaryProject = addDProp("fileForeignSummaryProject", "Файл резюме проекта (иностр.)", DynamicFormatFileClass.instance, project);
        loadFileForeignSummaryProject = addLFAProp(executiveSummaryGroup, "Загрузить файл резюме проекта (иностр.)", fileForeignSummaryProject);
        openFileForeignSummaryProject = addOFAProp(executiveSummaryGroup, "Открыть файл резюме проекта (иностр.)", fileForeignSummaryProject);

        fileNativeApplicationFormProject = addDProp("fileNativeApplicationFormProject", "Анкета на русском", DynamicFormatFileClass.instance, project);
        loadFileNativeApplicationFormProject = addLFAProp(applicationFormGroup, "Загрузить файл анкеты", fileNativeApplicationFormProject);
        openFileNativeApplicationFormProject = addOFAProp(applicationFormGroup, "Открыть файл анкеты", fileNativeApplicationFormProject);
        fileForeignApplicationFormProject = addDProp("fileForeignApplicationFormProject", "Анкета на английском", DynamicFormatFileClass.instance, project);
        loadFileForeignApplicationFormProject = addLFAProp(applicationFormGroup, "Загрузить файл анкеты (иностр.)", fileForeignApplicationFormProject);
        openFileForeignApplicationFormProject = addOFAProp(applicationFormGroup, "Открыть файл анкеты (иностр.)", fileForeignApplicationFormProject);

        // источники финансирования
        isReturnInvestmentsProject = addDProp(sourcesFundingGroup, "isReturnInvestmentsProject", "Средства третьих лиц, привлекаемые на возвратной основе (заемные средства и т.п.)", LogicalClass.instance, project);
        nameReturnInvestorProject = addDProp(sourcesFundingGroup, "nameReturnInvestorProject", "Третьи лица для возврата средств", InsensitiveStringClass.get(2000), project);
        nameReturnInvestorProject.setMinimumWidth(10);
        nameReturnInvestorProject.setPreferredWidth(50);
        amountReturnFundsProject = addDProp(sourcesFundingGroup, "amountReturnFundsProject", "Объем средств на возвратной основе (тыс. руб.)", StringClass.get(30), project);
        hideNameReturnInvestorProject = addHideCaptionProp(privateGroup, "укажите данных лиц и их контактную информацию", isReturnInvestmentsProject);
        hideAmountReturnFundsProject = addHideCaptionProp(privateGroup, "укажите объем привлекаемых средств (тыс. руб.)", isReturnInvestmentsProject);

        isNonReturnInvestmentsProject = addDProp(sourcesFundingGroup, "isNonReturnInvestmentsProject", "Средства третьих лиц, привлекаемые на безвозвратной основе (гранты и т.п.)", LogicalClass.instance, project);

        isCapitalInvestmentProject = addDProp(nonReturnFundingGroup, "isCapitalInvestmentProject", "Вклады в уставный капитал", LogicalClass.instance, project);
        isPropertyInvestmentProject = addDProp(nonReturnFundingGroup, "isPropertyInvestmentProject", "Вклады в имущество", LogicalClass.instance, project);
        isGrantsProject = addDProp(nonReturnFundingGroup, "isGrantsProject", "Гранты", LogicalClass.instance, project);

        isOtherNonReturnInvestmentsProject = addDProp(nonReturnFundingGroup, "isOtherNonReturnInvestmentsProject", "Иное", LogicalClass.instance, project);

        nameNonReturnInvestorProject = addDProp(nonReturnFundingGroup, "nameNonReturnInvestorProject", "Третьи лица для возврата средств", InsensitiveStringClass.get(2000), project);
        nameNonReturnInvestorProject.setMinimumWidth(10);
        nameNonReturnInvestorProject.setPreferredWidth(50);
        amountNonReturnFundsProject = addDProp(nonReturnFundingGroup, "amountNonReturnFundsProject", "Объем средств на безвозвратной основе (тыс. руб.)", StringClass.get(30), project);
        hideNameNonReturnInvestorProject = addHideCaptionProp(privateGroup, "укажите данных лиц и их контактную информацию", isNonReturnInvestmentsProject);
        hideAmountNonReturnFundsProject = addHideCaptionProp(privateGroup, "укажите объем привлекаемых средств (тыс. руб.)", isNonReturnInvestmentsProject);

        commentOtherNonReturnInvestmentsProject = addDProp(nonReturnFundingGroup, "commentOtherNonReturnInvestmentsProject", "Комментарий", InsensitiveStringClass.get(2000), project);
        commentOtherNonReturnInvestmentsProject.setMinimumWidth(10);
        commentOtherNonReturnInvestmentsProject.setPreferredWidth(50);

        hideIsCapitalInvestmentProject = addHideCaptionProp(privateGroup, "Укажите", isNonReturnInvestmentsProject);
        hideIsPropertyInvestmentProject = addHideCaptionProp(privateGroup, "Укажите", isNonReturnInvestmentsProject);
        hideIsGrantsProject = addHideCaptionProp(privateGroup, "Укажите", isNonReturnInvestmentsProject);
        hideIsOtherNonReturnInvestmentsProject = addHideCaptionProp(privateGroup, "Укажите", isNonReturnInvestmentsProject);

        hideCommentOtherNonReturnInvestmentsProject = addHideCaptionProp(privateGroup, "Укажите", isOtherNonReturnInvestmentsProject);

        isOwnFundsProject = addDProp(sourcesFundingGroup, "isOwnFundsProject", "Собственные средства организации", LogicalClass.instance, project);
        amountOwnFundsProject = addDProp(sourcesFundingGroup, "amountOwnFundsProject", "Объем собственных средств (тыс. руб.)", StringClass.get(30), project);
        hideAmountOwnFundsProject = addHideCaptionProp(privateGroup, "Укажите", isOwnFundsProject);

        isPlanningSearchSourceProject = addDProp(sourcesFundingGroup, "isPlanningSearchSourceProject", "Планируется поиск источника финансирования проекта", LogicalClass.instance, project);
        amountFundsProject = addDProp(sourcesFundingGroup, "amountFundsProject", "Требуемый объем средств (тыс. руб.)", StringClass.get(30), project);
        hideAmountFundsProject = addHideCaptionProp(privateGroup, "Укажите", isPlanningSearchSourceProject);

        isOtherSoursesProject = addDProp(sourcesFundingGroup, "isOtherSoursesProject", "Иное", LogicalClass.instance, project);
        commentOtherSoursesProject = addDProp(sourcesFundingGroup, "commentOtherSoursesProject", "Комментарий", InsensitiveStringClass.get(2000), project);
        commentOtherSoursesProject.setMinimumWidth(10);
        commentOtherSoursesProject.setPreferredWidth(50);
        hideCommentOtherSoursesProject = addHideCaptionProp(privateGroup, "Укажите", isOtherSoursesProject);

        // оборудование
        isOwnedEquipmentProject = addDProp(equipmentGroup, "isOwnedEquipmentProject", "Оборудование имеется в собственности и/или в пользовании Вашей организации", LogicalClass.instance, project);

        isAvailableEquipmentProject = addDProp(equipmentGroup, "isAvailableEquipmentProject", "Оборудование имеется в открытом доступе на рынке, и Ваша организация планирует приобрести его в собственность и/или в пользование", LogicalClass.instance, project);

        isTransferEquipmentProject = addDProp(equipmentGroup, "isTransferEquipmentProject", "Оборудование отсутствует в открытом доступе на рынке, но достигнута договоренность с собственником оборудования о передаче данного оборудования в собственность и/или в пользование для реализации проекта", LogicalClass.instance, project);
        descriptionTransferEquipmentProject = addDProp(equipmentGroup, "descriptionTransferEquipmentProject", "Опишите данное оборудование", InsensitiveStringClass.get(2000), project);
        descriptionTransferEquipmentProject.setMinimumWidth(10);
        descriptionTransferEquipmentProject.setPreferredWidth(50);
        ownerEquipmentProject = addDProp(equipmentGroup, "ownerEquipmentProject", "Укажите собственника оборудования и его контактную информацию", InsensitiveStringClass.get(2000), project);
        ownerEquipmentProject.setMinimumWidth(10);
        ownerEquipmentProject.setPreferredWidth(50);
        hideDescriptionTransferEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", isTransferEquipmentProject);
        hideOwnerEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", isTransferEquipmentProject);

        isPlanningEquipmentProject = addDProp(equipmentGroup, "isPlanningEquipmentProject", "Ваша организация планирует использовать для реализации проекта оборудование, которое имеется в собственности или в пользовании Фонда «Сколково» (учрежденных им юридических лиц)", LogicalClass.instance, project);
        specificationEquipmentProject = addDProp(equipmentGroup, "specificationEquipmentProject", "Укажите данное оборудование", InsensitiveStringClass.get(2000), project);
        specificationEquipmentProject.setMinimumWidth(10);
        specificationEquipmentProject.setPreferredWidth(50);
        hideSpecificationEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", isPlanningEquipmentProject);

        isSeekEquipmentProject = addDProp(equipmentGroup, "isSeekEquipmentProject", "Оборудование имеется на рынке, но Ваша организация не имеет возможности приобрести его в собственность или в пользование и ищет возможность получить доступ к такому оборудованию", LogicalClass.instance, project);
        descriptionEquipmentProject = addDProp(equipmentGroup, "descriptionEquipmentProject", "Опишите данное оборудование", InsensitiveStringClass.get(2000), project);
        descriptionEquipmentProject.setMinimumWidth(10);
        descriptionEquipmentProject.setPreferredWidth(50);
        hideDescriptionEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", isSeekEquipmentProject);

        isOtherEquipmentProject = addDProp(equipmentGroup, "isOtherEquipmentProject", "Иное", LogicalClass.instance, project);
        commentEquipmentProject = addDProp(equipmentGroup, "commentEquipmentProject", "Комментарий", InsensitiveStringClass.get(2000), project);
        commentEquipmentProject.setMinimumWidth(10);
        commentEquipmentProject.setPreferredWidth(50);
        hideCommentEquipmentProject = addHideCaptionProp(privateGroup, "Укажите", isOtherEquipmentProject);

        // документы
        fileNativeRoadMapProject = addDProp("fileNativeRoadMapProject", "Файл дорожной карты", DynamicFormatFileClass.instance, project);
        loadNativeFileRoadMapProject = addLFAProp(roadMapGroup, "Загрузить файл дорожной карты", fileNativeRoadMapProject);
        openNativeFileRoadMapProject = addOFAProp(roadMapGroup, "Открыть файл дорожной карты", fileNativeRoadMapProject);

        fileForeignRoadMapProject = addDProp("fileForeignRoadMapProject", "Файл дорожной карты (иностр.)", DynamicFormatFileClass.instance, project);
        loadForeignFileRoadMapProject = addLFAProp(roadMapGroup, "Загрузить файл дорожной карты (иностр.)", fileForeignRoadMapProject);
        openForeignFileRoadMapProject = addOFAProp(roadMapGroup, "Открыть файл дорожной карты (иностр.)", fileForeignRoadMapProject);

        fileResolutionIPProject = addDProp("fileResolutionIPProject", "Заявление IP", DynamicFormatFileClass.instance, project);
        loadFileResolutionIPProject = addLFAProp(resolutionIPGroup, "Загрузить файл заявление IP", fileResolutionIPProject);
        openFileResolutionIPProject = addOFAProp(resolutionIPGroup, "Открыть файл заявление IP", fileResolutionIPProject);

        fileNativeTechnicalDescriptionProject = addDProp("fileNativeTechnicalDescriptionProject", "Файл технического описания", DynamicFormatFileClass.instance, project);
        loadFileNativeTechnicalDescriptionProject = addLFAProp(techDescrGroup, "Загрузить файл технического описания", fileNativeTechnicalDescriptionProject);
        openFileNativeTechnicalDescriptionProject = addOFAProp(techDescrGroup, "Открыть файл технического описания", fileNativeTechnicalDescriptionProject);
        isFileNativeTechnicalDescriptionProject = addJProp(techDescrGroup, "isFileNativeTechnicalDescriptionProject", "Файл", baseLM.and1, addCProp(LogicalClass.instance, true, project), 1, fileNativeTechnicalDescriptionProject, 1);

        fileForeignTechnicalDescriptionProject = addDProp("fileForeignTechnicalDescriptionProject", "Файл технического описания (иностр.)", DynamicFormatFileClass.instance, project);
        loadFileForeignTechnicalDescriptionProject = addLFAProp(techDescrGroup, "Загрузить файл технического описания (иностр.)", fileForeignTechnicalDescriptionProject);
        openFileForeignTechnicalDescriptionProject = addOFAProp(techDescrGroup, "Открыть файл технического описания (иностр.)", fileForeignTechnicalDescriptionProject);
        isFileForeignTechnicalDescriptionProject = addJProp(techDescrGroup, "isFileForeignTechnicalDescriptionProject", "Файл (ин.)", baseLM.and1, addCProp(LogicalClass.instance, true, project), 1, fileForeignTechnicalDescriptionProject, 1);

        // патенты
        projectPatent = addDProp(idGroup, "projectPatent", "Проект патента", project, patent);
        equalsPatentProject = addJProp("equalsPatentProject", "Вкл", baseLM.equals2, projectPatent, 1, 2);

        nativeTypePatent = addDProp(baseGroup, "nativeTypePatent", "Тип заявки/патента", InsensitiveStringClass.get(2000), patent);
        nativeTypePatent.setMinimumWidth(10);
        nativeTypePatent.setPreferredWidth(50);
        foreignTypePatent = addDProp(baseGroup, "foreignTypePatent", "Type of application/patent", InsensitiveStringClass.get(2000), patent);
        foreignTypePatent.setMinimumWidth(10);
        foreignTypePatent.setPreferredWidth(50);
        nativeNumberPatent = addDProp(baseGroup, "nativeNumberPatent", "Номер", InsensitiveStringClass.get(2000), patent);
        nativeNumberPatent.setMinimumWidth(10);
        nativeNumberPatent.setPreferredWidth(50);
        sidProjectPatent = addJProp("sidProjectPatent", sidProject, projectPatent, 1);
        nativeNumberToPatent = addAGProp("nativeNumberToPatent", "Патент (номер патента, внутренний ID проекта)", nativeNumberPatent, projectPatent);
        nativeNumberSIDToPatent = addJProp("nativeNumberSIDToPatent", "Патент (номер патента, внешний ID проекта)", nativeNumberToPatent, 1, sidToProject, 2);
        linksPatent = addDProp("linksPatent", "Ссылки на патенты и алгоритмы", InsensitiveStringClass.get(500), patent);
        linksPatent.setMinimumWidth(10);
        linksPatent.setPreferredWidth(50);

        foreignNumberPatent = addDProp(baseGroup, "foreignNumberPatent", "Reference number", InsensitiveStringClass.get(2000), patent);
        foreignNumberPatent.setMinimumWidth(10);
        foreignNumberPatent.setPreferredWidth(50);
        priorityDatePatent = addDProp(baseGroup, "priorityDatePatent", "Дата приоритета", DateClass.instance, patent);

        isOwned = addDProp(baseGroup, "isOwned", "Организация не обладает исключительными правами на указанные результаты интеллектуальной деятельности", LogicalClass.instance, patent);
        ownerPatent = addDProp(baseGroup, "ownerPatent", "Укажите правообладателя и его контактную информацию", InsensitiveStringClass.get(2000), patent);
        ownerPatent.setMinimumWidth(10);
        ownerPatent.setPreferredWidth(50);

        nameForeignOwnerType = addDProp(baseGroup, "nameForeignOwnerType", "Имя (иностр.)", InsensitiveStringClass.get(110), ownerType);

        ownerTypePatent = addDProp(idGroup, "ownerTypePatent", "Кем является правообладатель (ИД)", ownerType, patent);
        nameNativeOwnerTypePatent = addJProp(baseGroup, "nameNativeOwnerTypePatent", "Кем является правообладатель", baseLM.name, ownerTypePatent, 1);
        nameForeignOwnerTypePatent = addJProp(baseGroup, "nameForeignOwnerTypePatent", "Кем является правообладатель (иностр.)", nameForeignOwnerType, ownerTypePatent, 1);

        ownerTypeToSID = addAGProp("ownerTypeToSID", "SID типа правообладателя", addJProp(baseLM.and1, baseLM.classSID, 1, is(ownerType), 1));
        projectTypeToSID = addAGProp("projectTypeToSID", "SID типа проекта", addJProp(baseLM.and1, baseLM.classSID, 1, is(projectType), 1));
        projectActionToSID = addAGProp("projectActionToSID", "SID текущего статуса", addJProp(baseLM.and1, baseLM.classSID, 1, is(projectAction), 1));
        projectMissionToSID = addAGProp("projectMissionToSID", "SID назначения", addJProp(baseLM.and1, baseLM.classSID, 1, is(projectMission), 1));
        projectScheduleToSID = addAGProp("projectScheduleToSID", "SID регламента", addJProp(baseLM.and1, baseLM.classSID, 1, is(projectSchedule), 1));
        fileIntentionOwnerPatent = addDProp("fileIntentionOwnerPatent", "Файл документа о передаче права", DynamicFormatFileClass.instance, patent);
        loadFileIntentionOwnerPatent = addLFAProp(baseGroup, "Загрузить файл документа о передаче права", fileIntentionOwnerPatent);
        openFileIntentionOwnerPatent = addOFAProp(baseGroup, "Открыть файл документа о передаче права", fileIntentionOwnerPatent);

        hideOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", isOwned);
        hideNameNativeOwnerTypePatent = addHideCaptionProp(privateGroup, "Укажите", isOwned);
        hideNameForeignOwnerTypePatent = addHideCaptionProp(privateGroup, "Укажите", isOwned);
        hideFileIntentionOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", isOwned);
        hideLoadFileIntentionOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", isOwned);
        hideOpenFileIntentionOwnerPatent = addHideCaptionProp(privateGroup, "Укажите", isOwned);

        isValuated = addDProp(baseGroup, "isValuated", "Проводилась ли оценка указанных результатов интеллектальной деятельности", LogicalClass.instance, patent);
        valuatorPatent = addDProp(baseGroup, "valuatorPatent", "Укажите оценщика и его контактную информацию", InsensitiveStringClass.get(2000), patent);
        valuatorPatent.setMinimumWidth(10);
        valuatorPatent.setPreferredWidth(50);
        fileActValuationPatent = addDProp("fileActValuationPatent", "Файл акта оценки", DynamicFormatFileClass.instance, patent);
        loadFileActValuationPatent = addLFAProp(baseGroup, "Загрузить файл акта оценки", fileActValuationPatent);
        openFileActValuationPatent = addOFAProp(baseGroup, "Открыть файл акта оценки", fileActValuationPatent);
        hideValuatorPatent = addHideCaptionProp(privateGroup, "Укажите", isValuated);
        hideFileActValuationPatent = addHideCaptionProp(privateGroup, "Укажите", isValuated);
        hideLoadFileActValuationPatent = addHideCaptionProp(privateGroup, "Укажите", isValuated);
        hideOpenFileActValuationPatent = addHideCaptionProp(privateGroup, "Укажите", isValuated);

        // учёные
        projectAcademic = addDProp(idGroup, "projectAcademic", "Проект ученого", project, academic);
        sidProjectAcademic = addJProp("sidProjectAcademic", sidProject, projectAcademic, 1);
        fullNameAcademic = addDProp(baseGroup, "fullNameAcademic", "ФИО", InsensitiveStringClass.get(2000), academic);
        fullNameAcademic.setMinimumWidth(10);
        fullNameAcademic.setPreferredWidth(50);
        fullNameToAcademic = addAGProp("fullNameToAcademic", "ФИО учёного (фамилия, внутренний ИД проекта)", fullNameAcademic, projectAcademic);
        fullNameSIDToAcademic = addJProp("fullNameSIDToAcademic", "Учёный (фамилия, внешний ИД проекта)", fullNameToAcademic, 1, sidToProject, 2);
        institutionAcademic = addDProp(baseGroup, "institutionAcademic", "Учреждение, в котором данный специалист осуществляет научную и (или) преподавательскую деятельность", InsensitiveStringClass.get(2000), academic);
        institutionAcademic.setMinimumWidth(10);
        institutionAcademic.setPreferredWidth(50);
        titleAcademic = addDProp(baseGroup, "titleAcademic", "Ученая степень, звание, должность и др.", InsensitiveStringClass.get(2000), academic);
        titleAcademic.setMinimumWidth(10);
        titleAcademic.setPreferredWidth(50);

        fileDocumentConfirmingAcademic = addDProp("fileDocumentConfirmingAcademic", "Файл трудового договора", DynamicFormatFileClass.instance, academic);
        loadFileDocumentConfirmingAcademic = addLFAProp(baseGroup, "Загрузить файл трудового договора", fileDocumentConfirmingAcademic);
        openFileDocumentConfirmingAcademic = addOFAProp(baseGroup, "Открыть файл трудового договора", fileDocumentConfirmingAcademic);

        fileDocumentEmploymentAcademic = addDProp("fileDocumentEmploymentAcademic", "Файл заявления специалиста", DynamicFormatFileClass.instance, academic);
        loadFileDocumentEmploymentAcademic = addLFAProp(baseGroup, "Загрузить файл заявления", fileDocumentEmploymentAcademic);
        openFileDocumentEmploymentAcademic = addOFAProp(baseGroup, "Открыть файл заявления", fileDocumentEmploymentAcademic);

        // иностранные специалисты
        projectNonRussianSpecialist = addDProp(idGroup, "projectNonRussianSpecialist", "Проект иностранного специалиста", project, nonRussianSpecialist);
        sidProjectNonRussianSpecialist = addJProp("sidProjectNonRussianSpecialist", sidProject, projectNonRussianSpecialist, 1);

        fullNameNonRussianSpecialist = addDProp(baseGroup, "fullNameNonRussianSpecialist", "ФИО", InsensitiveStringClass.get(2000), nonRussianSpecialist);
        fullNameNonRussianSpecialist.setMinimumWidth(10);
        fullNameNonRussianSpecialist.setPreferredWidth(50);
        fullNameToNonRussianSpecialist = addAGProp("fullNameToNonRussianSpecialist", "Иностранный специалист (фамилия, внутренний ИД проекта)", fullNameNonRussianSpecialist, projectNonRussianSpecialist);
        fullNameSIDToNonRussianSpecialist = addJProp("fullNameSIDToNonRussianSpecialist", "Иностранный специалист (фамилия, внешний ИД проекта)", fullNameToNonRussianSpecialist, 1, sidToProject, 2);
        organizationNonRussianSpecialist = addDProp(baseGroup, "organizationNonRussianSpecialist", "Место работы", InsensitiveStringClass.get(2000), nonRussianSpecialist);
        organizationNonRussianSpecialist.setMinimumWidth(10);
        organizationNonRussianSpecialist.setPreferredWidth(50);
        titleNonRussianSpecialist = addDProp(baseGroup, "titleNonRussianSpecialist", "Должность, если есть - ученая степень, звание и др.", InsensitiveStringClass.get(2000), nonRussianSpecialist);
        titleNonRussianSpecialist.setMinimumWidth(10);
        titleNonRussianSpecialist.setPreferredWidth(50);

        fileForeignResumeNonRussianSpecialist = addDProp("fileForeignResumeNonRussianSpecialist", "File Curriculum Vitae", DynamicFormatFileClass.instance, nonRussianSpecialist);
        loadFileForeignResumeNonRussianSpecialist = addLFAProp(baseGroup, "Load file Curriculum Vitae", fileForeignResumeNonRussianSpecialist);
        openFileForeignResumeNonRussianSpecialist = addOFAProp(baseGroup, "Open file Curriculum Vitae", fileForeignResumeNonRussianSpecialist);

        fileNativeResumeNonRussianSpecialist = addDProp("fileNativeResumeNonRussianSpecialist", "Файл резюме специалиста", DynamicFormatFileClass.instance, nonRussianSpecialist);
        loadFileNativeResumeNonRussianSpecialist = addLFAProp(baseGroup, "Загрузить файл резюме", fileNativeResumeNonRussianSpecialist);
        openFileNativeResumeNonRussianSpecialist = addOFAProp(baseGroup, "Открыть файл резюме", fileNativeResumeNonRussianSpecialist);

        filePassportNonRussianSpecialist = addDProp("filePassportNonRussianSpecialist", "Файл паспорта", DynamicFormatFileClass.instance, nonRussianSpecialist);
        loadFilePassportNonRussianSpecialist = addLFAProp(baseGroup, "Загрузить файл паспорта", filePassportNonRussianSpecialist);
        openFilePassportNonRussianSpecialist = addOFAProp(baseGroup, "Открыть файл паспорта", filePassportNonRussianSpecialist);

        fileStatementNonRussianSpecialist = addDProp("fileStatementNonRussianSpecialist", "Файл заявления", DynamicFormatFileClass.instance, nonRussianSpecialist);
        loadFileStatementNonRussianSpecialist = addLFAProp(baseGroup, "Загрузить файл заявления", fileStatementNonRussianSpecialist);
        openFileStatementNonRussianSpecialist = addOFAProp(baseGroup, "Открыть файл заявления", fileStatementNonRussianSpecialist);

        fileMinutesOfMeetingExpertCollegiumProject = addDProp("fileMinutesOfMeetingExpertCollegiumProject", "Файл протокола заседания Экспертной коллегии", DynamicFormatFileClass.instance, project);
        loadFileMinutesOfMeetingExpertCollegiumProject = addLFAProp(minutesOfMettingGroup, "Загрузить файл протокола заседания Экспертной коллегии", fileMinutesOfMeetingExpertCollegiumProject);
        openFileMinutesOfMeetingExpertCollegiumProject = addOFAProp(minutesOfMettingGroup, "Открыть Файл протокола заседания Экспертной коллегии", fileMinutesOfMeetingExpertCollegiumProject);

        fileWrittenConsentClaimerProject = addDProp("fileWrittenConsentClaimerProject", "Файл письменного согласия заявителя", DynamicFormatFileClass.instance, project);
        loadFileWrittenConsentClaimerProject = addLFAProp(writtenConsentGroup, "Загрузить файл письменного согласия заявителя", fileWrittenConsentClaimerProject);
        openFileWrittenConsentClaimerProject = addOFAProp(writtenConsentGroup, "Открыть Файл письменного согласия заявителя", fileWrittenConsentClaimerProject);

        nameNativeJoinClaimerVote = addJProp(baseGroup, "nameNativeJoinClaimerVote", nameNativeClaimer, claimerVote, 1);
        nameForeignJoinClaimerVote = addJProp(baseGroup, "nameForeignJoinClaimerVote", nameForeign, claimerVote, 1);

        nameNativeClaimerVote = addIfElseUProp(baseGroup, "nameNativeClaimerVote", "Заявитель", addJProp(nameNativeUnionManagerProject, projectVote, 1), nameNativeJoinClaimerVote, isPreliminaryVote, 1);
        nameNativeClaimerVote.setMinimumWidth(10);
        nameNativeClaimerVote.setPreferredWidth(50);
        nameForeignClaimerVote = addIfElseUProp(baseGroup, "nameForeignClaimerVote", "Заявитель (иностр.)", addJProp(nameForeignUnionManagerProject, projectVote, 1), nameForeignJoinClaimerVote, isPreliminaryVote, 1);
        nameForeignClaimerVote.setMinimumWidth(10);
        nameForeignClaimerVote.setPreferredWidth(50);
        nameGenitiveClaimerVote = addIfElseUProp(baseGroup, "nameGenitiveClaimerVote", "Заявитель (кого)", addJProp(nameGenitiveManagerProject, projectVote, 1), nameNativeJoinClaimerVote, isPreliminaryVote, 1);
        nameGenitiveClaimerVote.setMinimumWidth(10);
        nameGenitiveClaimerVote.setPreferredWidth(50);
        nameDativusClaimerVote = addIfElseUProp(baseGroup, "nameDativusClaimerVote", "Заявитель (кому)", addJProp(nameDativusManagerProject, projectVote, 1), nameNativeJoinClaimerVote, isPreliminaryVote, 1);
        nameDativusClaimerVote.setMinimumWidth(10);
        nameDativusClaimerVote.setPreferredWidth(50);
        nameAblateClaimerVote = addIfElseUProp(baseGroup, "nameAblateClaimerVote", "Заявитель (кем)", addJProp(nameAblateManagerProject, projectVote, 1), nameNativeJoinClaimerVote, isPreliminaryVote, 1);
        nameAblateClaimerVote.setMinimumWidth(10);
        nameAblateClaimerVote.setPreferredWidth(50);

        documentTemplateDocumentTemplateDetail = addDProp(idGroup, "documentTemplateDocumentTemplateDetail", "Шаблон (ИД)", documentTemplate, documentTemplateDetail);

        projectDocument = addDProp(idGroup, "projectDocument", "Проект (ИД)", project, document);
        nameNativeProjectDocument = addJProp(baseGroup, "nameNativeProjectDocument", "Проект", nameNativeProject, projectDocument, 1);

        quantityMinLanguageDocumentType = addDProp(baseGroup, "quantityMinLanguageDocumentType", "Мин. док.", IntegerClass.instance, language, documentType);
        quantityMaxLanguageDocumentType = addDProp(baseGroup, "quantityMaxLanguageDocumentType", "Макс. док.", IntegerClass.instance, language, documentType);
        LCP singleLanguageDocumentType = addJProp("Один док.", baseLM.equals2, quantityMaxLanguageDocumentType, 1, 2, addCProp(IntegerClass.instance, 1));
        LCP multipleLanguageDocumentType = addJProp(baseLM.andNot1, addCProp(LogicalClass.instance, true, language, documentType), 1, 2, singleLanguageDocumentType, 1, 2);
        translateLanguageDocumentType = addDProp(baseGroup, "translateLanguageDocumentType", "Перевод", StringClass.get(50), language, documentType);

        languageExpert = addDProp(idGroup, "languageExpert", "Язык (ИД)", language, expert);
        nameLanguageExpert = addJProp(baseGroup, "nameLanguageExpert", "Язык", baseLM.name, languageExpert, 1);
        nameLanguageExpert.setFixedCharWidth(10);

        languageDocument = addDProp(idGroup, "languageDocument", "Язык (ИД)", language, documentAbstract);
        nameLanguageDocument = addJProp(baseGroup, "nameLanguageDocument", "Язык", baseLM.name, languageDocument, 1);
        englishDocument = addJProp("englishDocument", "Иностр.", baseLM.equals2, languageDocument, 1, addCProp(language, "english"));

        defaultEnglishDocumentType = addDProp(baseGroup, "defaultEnglishDocumentType", "Англ.", LogicalClass.instance, documentType);

        typeDocument = addDProp(idGroup, "typeDocument", "Тип (ИД)", documentType, documentAbstract);
        nameTypeDocument = addJProp(baseGroup, "nameTypeDocument", "Тип", baseLM.name, typeDocument, 1);
        defaultEnglishDocument = addJProp("defaultEnglishDocument", "Англ.", defaultEnglishDocumentType, typeDocument, 1);

        localeLanguage = addDProp(baseGroup, "localeLanguage", "Locale", StringClass.get(5), language);
        authExpertSubjectLanguage = addDProp(baseGroup, "authExpertSubjectLanguage", "Заголовок аутентификации эксперта", StringClass.get(100), language);
        authProfileExpertSubjectLanguage = addDProp(baseGroup, "authProfileExpertSubjectLanguage", "Заголовок уведомления о заполнении профиля", StringClass.get(100), language);
        letterExpertSubjectLanguage = addDProp(baseGroup, "letterExpertSubjectLanguage", "Заголовок письма о заседании", StringClass.get(100), language);
        reminderProfileExpertSubjectLanguage = addDProp(baseGroup, "reminderProfileExpertSubjectLanguage", "Заголовок напоминания о заполнении профиля", StringClass.get(100), language);

        LCP multipleDocument = addJProp(multipleLanguageDocumentType, languageDocument, 1, typeDocument, 1);
        postfixDocument = addJProp(baseLM.and1, addDProp("postfixDocument", "Доп. описание", StringClass.get(15), document), 1, multipleDocument, 1);
        hidePostfixDocument = addJProp(baseLM.and1, addCProp(StringClass.get(40), "Постфикс", document), 1, multipleDocument, 1);

        LCP translateNameDocument = addJProp("Перевод", translateLanguageDocumentType, languageDocument, 1, typeDocument, 1);
        nameDocument = addJProp("nameDocument", "Заголовок", baseLM.string2SP, translateNameDocument, 1, addSUProp(Union.OVERRIDE, addCProp(StringClass.get(15), "", document), postfixDocument), 1);

        quantityProjectLanguageDocumentType = addSGProp("projectLanguageDocumentType", true, "Кол-во док.", addCProp(IntegerClass.instance, 1, document), projectDocument, 1, languageDocument, 1, typeDocument, 1); // сколько экспертов высказалось
        LCP notEnoughProjectLanguageDocumentType = addSUProp(Union.OVERRIDE, addJProp(baseLM.greater2, quantityProjectLanguageDocumentType, 1, 2, 3, quantityMaxLanguageDocumentType, 2, 3),
                addJProp(baseLM.less2, addSUProp(Union.OVERRIDE, addCProp(IntegerClass.instance, 0, project, language, documentType), quantityProjectLanguageDocumentType), 1, 2, 3, quantityMinLanguageDocumentType, 2, 3));
        notEnoughProject = addMGProp(projectStatusGroup, "notEnoughProject", true, "Недостаточно док.", notEnoughProjectLanguageDocumentType, 1);

        autoGenerateProject = addDProp(projectStatusGroup, "autoGenerateProject", "Авт. зас.", LogicalClass.instance, project);
        inactiveProject = addDProp(projectStatusGroup, "inactiveProject", "Не акт.", LogicalClass.instance, project);

        fileDocument = addDProp(baseGroup, "fileDocument", "Файл", PDFClass.instance, document);
        loadFileDocument = addLFAProp(baseGroup, "Загрузить", fileDocument);
        openFileDocument = addOFAProp(baseGroup, "Открыть", fileDocument);

        inDefaultDocumentLanguage = addJProp("inDefaultDocumentLanguage", "Вкл. (по умолчанию)", and(false, false, true),
                englishDocument, 1, // если документ на английском
                defaultEnglishDocument, 1, // если для типа документа можно только на английском
                is(language), 2, // второй параметр - язык
                addJProp(addMGProp(object(document), projectDocument, 1, typeDocument, 1, postfixDocument, 1, languageDocument, 1), projectDocument, 1, typeDocument, 1, postfixDocument, 1, 2), 1, 2); // нету документа на русском
        inDefaultDocumentExpert = addJProp("inDefaultDocumentExpert", "Вкл.", inDefaultDocumentLanguage, 1, languageExpert, 2);

        inDocumentLanguage = addJProp("inDocumentLanguage", "Вкл.", baseLM.equals2, languageDocument, 1, 2);
        inDocumentExpert = addJProp("inDocumentExpert", "Вкл.", inDocumentLanguage, 1, languageExpert, 2);

        inExpertVote = addDProp(baseGroup, "inExpertVote", "Вкл", LogicalClass.instance, expert, vote); // !!! нужно отослать письмо с документами и т.д
        oldExpertVote = addDProp(baseGroup, "oldExpertVote", "Из предыдущего заседания", LogicalClass.instance, expert, vote); // !!! нужно отослать письмо с документами и т.д
        exclExpertVote = addDProp(baseGroup, "exclExpertVote", "Аннулирован", LogicalClass.instance, expert, vote);

        inNewExpertVote = addJProp(baseGroup, "inNewExpertVote", "Вкл (нов.)", baseLM.andNot1, inExpertVote, 1, 2, oldExpertVote, 1, 2);
        inOldExpertVote = addJProp(baseGroup, "inOldExpertVote", "Вкл (стар.)", baseLM.and1, inExpertVote, 1, 2, oldExpertVote, 1, 2);

        businessExpertVote = addDProp(baseGroup, "businessExpertVote", "Выбран как бизнес", LogicalClass.instance, expert, voteR2);

        dateStartVote = addJProp(baseGroup, "dateStartVote", true, "Дата начала", baseLM.and1, baseLM.date, 1, is(vote), 1);
//        dateEndVote = addJProp(baseGroup, "dateEndVote", "Дата окончания", sumDate, dateStartVote, 1, requiredPeriod);
        aggrDateEndVote = addJProp(baseGroup, "aggrDateEndVote", "Дата окончания (агр.)", baseLM.sumDate, dateStartVote, 1, requiredPeriod);
        dateEndVote = addDProp(baseGroup, "dateEndVote", "Дата окончания", DateClass.instance, vote);
        dateEndVote.setEventChangeNewSet(aggrDateEndVote, 1, dateStartVote, 1);

        weekStartVote = addJProp("weekStartVote", true, "Неделя начала", baseLM.weekInDate, dateStartVote, 1);
        quantityNewExpertWeek = addSGProp("quantityNewExpertWeek", "Кол-во заседаний", addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inNewExpertVote, 1, 2), 1, weekStartVote, 2);
        quantityNewExpertWeek.setFixedCharWidth(2);
        quantityNewWeek = addSGProp("quantityNewWeek", "Кол-во заседаний", quantityNewExpertWeek, 2);

        quantityPreliminaryVoteProject = addSGProp("quantityPreliminaryVoteProject", true, "Количество заседаний",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1, vote), 1,
                        isPreliminaryVote, 1), projectVote, 1);

        quantityStatusVoteProject = addSGProp("quantityStatusVoteProject", true, "Количество заседаний",
                 addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1, vote), 1,
                         isStatusVote, 1), projectVote, 1);

        openedVote = addJProp(baseGroup, "openedVote", "Открыто", baseLM.groeq2, dateEndVote, 1, baseLM.currentDate);
        closedVote = addJProp(baseGroup, "closedVote", "Закрыто", baseLM.andNot1, is(vote), 1, openedVote, 1);

        voteInProgressProject = addAGProp(idGroup, "voteInProgressProject", true, "Тек. заседание (ИД)",
                openedVote, 1, projectVote, 1); // активно только одно заседание

        voteInProgressRepeatProject = addJProp("voteInProgressRepeatProject", true, "Тек. заседание (ИД) (повт.)", baseLM.and1,
                voteInProgressProject, 1,
                addJProp(baseLM.greater2, quantityVoteProject, 1, addCProp(IntegerClass.instance, 1)), 1);

        // результаты голосования
        voteResultExpertVote = addDProp(idGroup, "voteResultExpertVote", "Результат (ИД)", voteResult, expert, vote);
        voteResultCorExpertVote = addJProp(idGroup, "voteResultCorExpertVote", true, "Результат (ИД)", baseLM.andNot1, voteResultExpertVote, 1, 2, exclExpertVote, 1, 2);
        voteResultNewExpertVote = addJProp(baseGroup, "voteResultNewExpertVote", "Результат (ИД) (новый)", baseLM.and1,
                voteResultCorExpertVote, 1, 2, inNewExpertVote, 1, 2);

        dateExpertVote = addDProp(voteResultCheckGroup, "dateExpertVote", "Дата рез.", DateClass.instance, expert, vote);

        doneExpertVote = addJProp(baseGroup, "doneExpertVote", "Проголосовал", baseLM.equals2,
                voteResultExpertVote, 1, 2, addCProp(voteResult, "voted"));
        doneCorExpertVote = addJProp(baseGroup, "doneCorExpertVote", "Проголосовал", baseLM.equals2,
                voteResultCorExpertVote, 1, 2, addCProp(voteResult, "voted"));
        doneNewExpertVote = addJProp(baseGroup, "doneNewExpertVote", "Проголосовал (новый)", baseLM.and1,
                doneCorExpertVote, 1, 2, inNewExpertVote, 1, 2);
        doneOldExpertVote = addJProp(baseGroup, "doneOldExpertVote", "Проголосовал (старый)", baseLM.and1,
                doneCorExpertVote, 1, 2, inOldExpertVote, 1, 2);

        refusedExpertVote = addJProp(baseGroup, "refusedExpertVote", "Отказался", baseLM.equals2,
                voteResultCorExpertVote, 1, 2, addCProp(voteResult, "refused"));

        connectedExpertVote = addJProp(baseGroup, "connectedExpertVote", "Аффилирован", baseLM.equals2,
                voteResultCorExpertVote, 1, 2, addCProp(voteResult, "connected"));

        ipExpertVote = addDProp(voteResultCheckGroup, "ipExpertVote", "IP эксперта", StringClass.get(150), expert, vote);

        nameVoteResultExpertVote = addJProp(voteResultCheckGroup, "nameVoteResultExpertVote", "Результат", baseLM.name, voteResultExpertVote, 1, 2);

        incrementVote = addJProp(baseLM.greater2, dateEndVote, 1, addCProp(DateClass.instance, new java.sql.Date(2011 - 1900, 4 - 1, 26)));
        inProjectExpert = addMGProp(baseGroup, "inProjectExpert", "Вкл. в заседания", inExpertVote, projectVote, 2, 1);
        voteProjectExpert = addAGProp(baseGroup, "voteProjectExpert", "Результ. заседание", addJProp(baseLM.and1, voteResultNewExpertVote, 1, 2, incrementVote, 2), 2, projectVote, 2);
        voteResultProjectExpert = addJProp(baseGroup, "voteResultProjectExpert", "Результ. заседания", voteResultCorExpertVote, 2, voteProjectExpert, 1, 2);
        doneProjectExpert = addJProp(baseGroup, "doneProjectExpert", "Проголосовал", baseLM.equals2, voteResultProjectExpert, 1, 2, addCProp(voteResult, "voted"));
        LCP doneProject = addSGProp(baseGroup, "doneProject", "Проголосовало", addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneProjectExpert, 1, 2), 2); // сколько экспертов высказалось

        // vote Revision 1
        inClusterExpertVote = addDProp(voteResultCheckGroup, "inClusterExpertVote", "Соотв-ие кластеру", LogicalClass.instance, expert, voteR1);
        inClusterNewExpertVote = addJProp(baseGroup, "inClusterNewExpertVote", "Соотв-ие кластеру (новый)", baseLM.and1,
                inClusterExpertVote, 1, 2, inNewExpertVote, 1, 2);

        innovativeExpertVote = addDProp(voteResultCheckGroup, "innovativeExpertVote", "Инновац.", LogicalClass.instance, expert, voteR1);
        innovativeNewExpertVote = addJProp(baseGroup, "innovativeNewExpertVote", "Инновац. (новый)", baseLM.and1,
                innovativeExpertVote, 1, 2, inNewExpertVote, 1, 2);

        foreignExpertVote = addDProp(voteResultCheckGroup, "foreignExpertVote", "Иностр. специалист", LogicalClass.instance, expert, voteR1);
        foreignNewExpertVote = addJProp(baseGroup, "foreignNewExpertVote", "Иностр. специалист (новый)", baseLM.and1,
                foreignExpertVote, 1, 2, inNewExpertVote, 1, 2);

        innovativeCommentExpertVote = addDProp(voteResultCommentGroup, "innovativeCommentExpertVote", "Инновационность (комм.)", TextClass.instance, expert, voteR1);
        competentExpertVote = addDProp(voteResultCheckGroup, "competentExpertVote", "Компет.", IntegerClass.instance, expert, voteR1);
        completeExpertVote = addDProp(voteResultCheckGroup, "completeExpertVote", "Полнота информ.", IntegerClass.instance, expert, voteR1);
        completeCommentExpertVote = addDProp(voteResultCommentGroup, "completeCommentExpertVote", "Полнота информации (комм.)", TextClass.instance, expert, voteR1);

        followed(doneExpertVote, inClusterExpertVote, innovativeExpertVote, foreignExpertVote, innovativeCommentExpertVote, competentExpertVote, completeExpertVote, completeCommentExpertVote);
        followed(voteResultExpertVote, dateExpertVote);

        // vote Revision 2


        competitiveAdvantagesExpertVote = addDProp(voteResultCheckGroup, "competitiveAdvantagesExpertVote", "Конкур. преим.", LogicalClass.instance, expert, voteR2);
        commercePotentialExpertVote = addDProp(voteResultCheckGroup, "commercePotentialExpertVote", "Потенциал коммерц.", LogicalClass.instance, expert, voteR2);
        canBeImplementedExpertVote = addDProp(voteResultCheckGroup, "canBeImplementedExpertVote", "Теоретически реализуем", LogicalClass.instance, expert, voteR2);
        haveExpertiseExpertVote = addDProp(voteResultCheckGroup, "haveExpertiseExpertVote", "Наличие экспертизы", LogicalClass.instance, expert, voteR2);
        internationalExperienceExpertVote = addDProp(voteResultCheckGroup, "internationalExperienceExpertVote", "Международный опыт", LogicalClass.instance, expert, voteR2);
        enoughDocumentsExpertVote = addDProp(voteResultCheckGroup, "enoughDocumentsExpertVote", "Достаточно документов", LogicalClass.instance, expert, voteR2);

        competitiveAdvantagesCorExpertVote = addJProp("competitiveAdvantagesCorExpertVote", "Конкур. преим. (не анулл.)", and(true), competitiveAdvantagesExpertVote, 1, 2, exclExpertVote, 1, 2);
        commercePotentialCorExpertVote = addJProp("commercePotentialCorExpertVote", "Потенциал коммерц. (не анулл.)", and(true), commercePotentialExpertVote, 1, 2, exclExpertVote, 1, 2);
        canBeImplementedCorExpertVote = addJProp("canBeImplementedCorExpertVote", "Теоретически реализуем (не анулл.)", and(true), canBeImplementedExpertVote, 1, 2, exclExpertVote, 1, 2);
        haveExpertiseCorExpertVote = addJProp("haveExpertiseCorExpertVote", "Наличие экспертизы (не анулл.)", and(true), haveExpertiseExpertVote, 1, 2, exclExpertVote, 1, 2);
        internationalExperienceCorExpertVote = addJProp("internationalExperienceCorExpertVote", "Международный опыт (не анулл.)", and(true), internationalExperienceExpertVote, 1, 2, exclExpertVote, 1, 2);
        enoughDocumentsCorExpertVote = addJProp("enoughDocumentsCorExpertVote", "Достаточно документов (не анулл.)", and(true), enoughDocumentsExpertVote, 1, 2, exclExpertVote, 1, 2);

        commentCompetitiveAdvantagesExpertVote = addDProp(voteResultCommentGroup, "commentCompetitiveAdvantagesExpertVote", "Конкур. преим. (обоснование)", TextClass.instance, expert, voteR2);
        commentCommercePotentialExpertVote = addDProp(voteResultCommentGroup, "commentCommercePotentialExpertVote", "Потенциал коммерц. (обоснование)", TextClass.instance, expert, voteR2);
        commentCanBeImplementedExpertVote = addDProp(voteResultCommentGroup, "commentCanBeImplementedExpertVote", "Теоретически реализуем (обоснование)", TextClass.instance, expert, voteR2);
        commentHaveExpertiseExpertVote = addDProp(voteResultCommentGroup, "commentHaveExpertiseExpertVote", "Наличие экспертизы (обоснование)", TextClass.instance, expert, voteR2);
        commentInternationalExperienceExpertVote = addDProp(voteResultCommentGroup, "commentInternationalExperienceExpertVote", "Международный опыт (обоснование)", TextClass.instance, expert, voteR2);
        commentEnoughDocumentsExpertVote = addDProp(voteResultCommentGroup, "commentEnoughDocumentsExpertVote", "Достаточно документов (обоснование)", TextClass.instance, expert, voteR2);

        lengthCommentCompetitiveAdvantagesExpertVote = addJProp(voteResultLengthGroup, "lengthCommentCompetitiveAdvantagesExpertVote", "Длина комментария (Конкур. преим.)", baseLM.charLength, commentCompetitiveAdvantagesExpertVote, 1, 2);
        lengthCommentCommercePotentialExpertVote = addJProp(voteResultLengthGroup, "lengthCommentCommercePotentialExpertVote", "Длина комментария (Потенциал коммерц.)", baseLM.charLength, commentCommercePotentialExpertVote, 1, 2);
        lengthCommentCanBeImplementedExpertVote = addJProp(voteResultLengthGroup, "lengthCommentCanBeImplementedExpertVote", "Длина комментария (Теоретически реализуем)", baseLM.charLength, commentCanBeImplementedExpertVote, 1, 2);
        lengthCommentHaveExpertiseExpertVote = addJProp(voteResultLengthGroup, "lengthCommentHaveExpertiseExpertVote", "Длина комментария (Наличие экспертизы)", baseLM.charLength, commentHaveExpertiseExpertVote, 1, 2);
        lengthCommentInternationalExperienceExpertVote = addJProp(voteResultLengthGroup, "lengthCommentInternationalExperienceExpertVote", "Длина комментария (Международный опыт)", baseLM.charLength, commentInternationalExperienceExpertVote, 1, 2);
        lengthCommentEnoughDocumentsExpertVote = addJProp(voteResultLengthGroup, "lengthCommentEnoughDocumentsExpertVote", "Длина комментария (Достаточно документов)", baseLM.charLength, commentEnoughDocumentsExpertVote, 1, 2);

        followed(doneExpertVote, competitiveAdvantagesExpertVote, commercePotentialExpertVote, canBeImplementedExpertVote, haveExpertiseExpertVote, internationalExperienceExpertVote, enoughDocumentsExpertVote,
                commentCompetitiveAdvantagesExpertVote, commentCommercePotentialExpertVote, commentCanBeImplementedExpertVote, commentHaveExpertiseExpertVote, commentInternationalExperienceExpertVote, commentEnoughDocumentsExpertVote);
        followed(voteResultExpertVote, dateExpertVote);

        quantityInVote = addSGProp(voteResultGroup, "quantityInVote", true, "Участвовало",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inExpertVote, 1, 2), 2); // сколько экспертов учавстовало

        addConstraint(addJProp("Количество экспертов в заседании не соответствует требуемому.", baseLM.diff2, requiredQuantityVote, 1, quantityInVote, 1), false);

        quantityInOldVote = addSGProp(voteResultGroup, "quantityInOldVote", true, "Участвовало",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), oldExpertVote, 1, 2), 2); // сколько экспертов учавстовало

        quantityRepliedVote = addSGProp(voteResultGroup, "quantityRepliedVote", true, "Ответило",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), voteResultCorExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityDoneVote = addSGProp(voteResultGroup, "quantityDoneVote", true, "Проголосовало",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneCorExpertVote, 1, 2), 2); // сколько экспертов высказалось
        quantityDoneVote.setFixedCharWidth(3);

        quantityExclVote = addSGProp(voteResultGroup, "quantityExclVote", true, "Аннулировано бюллетеней",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), exclExpertVote, 1, 2), 2); // сколько экспертов анулировано
        quantityExclVote.setFixedCharWidth(3);

        percentNeeded = addDProp(baseGroup, "percentNeeded", "Процент для положительного решения", DoubleClass.instance);
        percentNeededVote = addDProp(baseGroup, "percentNeededVote", "Процент голосования", DoubleClass.instance, voteR2);

        quantityNeededVote = addJProp(baseGroup, "quantityNeededVote", "Треб. количество голосов", baseLM.percent, quantityDoneVote, 1, percentNeededVote, 1);

        quantityDoneNewVote = addSGProp(voteResultGroup, "quantityDoneNewVote", true, "Проголосовало (нов.)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneNewExpertVote, 1, 2), 2); // сколько новых экспертов высказалось

        quantityDoneOldVote = addSGProp(voteResultGroup, "quantityDoneOldVote", true, "Проголосовало (пред.)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneOldExpertVote, 1, 2), 2); // сколько старых экспертов высказалось

        quantityRefusedVote = addSGProp(voteResultGroup, "quantityRefusedVote", true, "Отказалось",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), refusedExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityConnectedVote = addSGProp(voteResultGroup, "quantityConnectedVote", true, "Аффилировано",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), connectedExpertVote, 1, 2), 2); // сколько экспертов высказалось

        quantityInClusterVote = addSGProp(voteResultGroup, "quantityInClusterVote", true, "Соотв-ие кластеру (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inClusterExpertVote, 1, 2), 2); // сколько экспертов высказалось
        quantityInClusterVote.setFixedCharWidth(3);

        quantityInnovativeVote = addSGProp(voteResultGroup, "quantityInnovativeVote", true, "Инновац. (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), innovativeExpertVote, 1, 2), 2); // сколько экспертов высказалось
        quantityInnovativeVote.setFixedCharWidth(3);

        quantityForeignVote = addSGProp(voteResultGroup, "quantityForeignVote", true, "Иностр. специалист (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), foreignExpertVote, 1, 2), 2); // сколько экспертов высказалось
        quantityForeignVote.setFixedCharWidth(3);

        quantityCompetitiveAdvantagesVote = addSGProp(voteResultGroup, "quantityCompetitiveAdvantagesVote", true, "Конкур. преим. (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), competitiveAdvantagesCorExpertVote, 1, 2), 2); // сколько экспертов высказалось
        quantityCompetitiveAdvantagesVote.setFixedCharWidth(3);

        quantityCommercePotentialVote = addSGProp(voteResultGroup, "quantityCommercePotentialVote", true, "Потенциал коммерц. (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), commercePotentialCorExpertVote, 1, 2), 2); // сколько экспертов высказалось
        quantityCommercePotentialVote.setFixedCharWidth(3);

        quantityCanBeImplementedVote = addSGProp(voteResultGroup, "quantityCanBeImplementedVote", true, "Теоретически реализуем (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), canBeImplementedCorExpertVote, 1, 2), 2); // сколько экспертов высказалось
        quantityCanBeImplementedVote.setFixedCharWidth(3);

        quantityHaveExpertiseVote = addSGProp(voteResultGroup, "quantityHaveExpertiseVote", true, "Наличие экспертизы (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), haveExpertiseCorExpertVote, 1, 2), 2); // сколько экспертов высказалось
        quantityHaveExpertiseVote.setFixedCharWidth(3);

        quantityInternationalExperienceVote = addSGProp(voteResultGroup, "quantityInternationalExperienceVote", true, "Международный опыт (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), internationalExperienceCorExpertVote, 1, 2), 2); // сколько экспертов высказалось
        quantityInternationalExperienceVote.setFixedCharWidth(3);

        quantityEnoughDocumentsVote = addSGProp(voteResultGroup, "quantityEnoughDocumentsVote", true, "Достаточно документов (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), enoughDocumentsCorExpertVote, 1, 2), 2); // сколько экспертов высказалось
        quantityEnoughDocumentsVote.setFixedCharWidth(3);

        LCP multiplyBy2 = addSFProp("((prm1)*2)", 1);
        acceptedInClusterVote = addJProp(voteResultGroup, "acceptedInClusterVote", "Соотв-ие кластеру", baseLM.greater2,
                addJProp(multiplyBy2, quantityInClusterVote, 1), 1,
                quantityDoneVote, 1);

        acceptedInnovativeVote = addJProp(voteResultGroup, "acceptedInnovativeVote", "Инновац.", baseLM.greater2,
                addJProp(multiplyBy2, quantityInnovativeVote, 1), 1,
                quantityDoneVote, 1);

        acceptedForeignVote = addJProp(voteResultGroup, "acceptedForeignVote", "Иностр. специалист", baseLM.greater2,
                addJProp(multiplyBy2, quantityForeignVote, 1), 1,
                quantityDoneVote, 1);

        acceptedCompetitiveAdvantagesVote = addJProp(voteResultGroup, "acceptedCompetitiveAdvantagesVote", "Конкур. преим.", baseLM.groeq2,
                quantityCompetitiveAdvantagesVote, 1, quantityNeededVote, 1);

        acceptedCommercePotentialVote = addJProp(voteResultGroup, "acceptedCommercePotentialVote", "Потенциал коммерц.", baseLM.groeq2,
                quantityCommercePotentialVote, 1, quantityNeededVote, 1);

        acceptedCanBeImplementedVote = addJProp(voteResultGroup, "acceptedCanBeImplementedVote", "Теоретически реализуем", baseLM.groeq2,
                quantityCanBeImplementedVote, 1, quantityNeededVote, 1);

        acceptedHaveExpertiseVote = addJProp(voteResultGroup, "acceptedHaveExpertiseVote", "Наличие экспертизы", baseLM.groeq2,
                quantityHaveExpertiseVote, 1, quantityNeededVote, 1);

        acceptedInternationalExperienceVote = addJProp(voteResultGroup, "acceptedInternationalExperienceVote", "Международный опыт", baseLM.groeq2,
                quantityInternationalExperienceVote, 1, quantityNeededVote, 1);

        acceptedEnoughDocumentsVote = addJProp(voteResultGroup, "acceptedEnoughDocumentsVote", "Достаточно голосов", baseLM.groeq2,
                addJProp(multiplyBy2, quantityEnoughDocumentsVote, 1), 1,
                quantityDoneVote, 1);

        acceptedVoteR1 = addJProp("acceptedVoteR1", true, "Положительно", and(false, false),
                acceptedInClusterVote, 1, acceptedInnovativeVote, 1, acceptedForeignVote, 1);

        acceptedVoteR2 = addJProp("acceptedVoteR2", true, "Положительно", and(false, false, false, false, false),
                acceptedCompetitiveAdvantagesVote, 1, acceptedCommercePotentialVote, 1, acceptedCanBeImplementedVote, 1,
                acceptedHaveExpertiseVote, 1, acceptedInternationalExperienceVote, 1, acceptedEnoughDocumentsVote, 1);

        acceptedVote = addCUProp(voteResultGroup, "acceptedVote", "Положительно", acceptedVoteR1, acceptedVoteR2);

        succeededVote = addJProp(voteResultGroup, "succeededVote", true, "Состоялось", baseLM.groeq2, quantityDoneVote, 1, limitExperts); // достаточно экспертов
        openedSucceededVote = addJProp("openedSucceededVote", "Открыто и состоялось", baseLM.and1, succeededVote, 1, openedVote, 1);
        closedSucceededVote = addJProp("closedSucceededVote", "Закрыто и состоялось", baseLM.andNot1, succeededVote, 1, openedVote, 1);

        closedAcceptedVote = addJProp("closedAcceptedVote", "Закрыто и положительно", baseLM.and1, closedSucceededVote, 1, acceptedVote, 1);
        closedRejectedVote = addJProp("closedRejectedVote", "Закрыто и отрицательно", baseLM.andNot1, closedSucceededVote, 1, acceptedVote, 1);

        closedAcceptedStatusVote = addJProp("closedAcceptedStatusVote", "Закрыто и положительно (статус участника)", baseLM.and1, closedAcceptedVote, 1, isStatusVote, 1);
        closedAcceptedPreliminaryVote = addJProp("closedAcceptedPreliminaryVote", "Закрыто и положительно (предварительная экспертиза)", baseLM.and1, closedAcceptedVote, 1, isPreliminaryVote, 1);
//        succeededClusterVote = addJProp("succeededClusterVote", "Состоялось в тек. кластере", baseLM.and1, succeededVote, 1, equalsClusterProjectVote, 1);
    }

    public void initProperties1() {

        LCP betweenExpertVoteDateFromDateTo = addJProp(baseLM.betweenDates, dateExpertVote, 1, 2, 3, 4);
        doneExpertVoteDateFromDateTo = addJProp(baseLM.and1, doneNewExpertVote, 1, 2, betweenExpertVoteDateFromDateTo, 1, 2, 3, 4);
        quantityDoneExpertDateFromDateTo = addSGProp("quantityDoneExpertDateFromDateTo", "Кол-во голосов.",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneExpertVoteDateFromDateTo, 1, 2, 3, 4), 1, 3, 4); // в скольки заседаниях поучавствовал
        inExpertVoteDateFromDateTo = addJProp(baseLM.and1, inNewExpertVote, 1, 2, betweenExpertVoteDateFromDateTo, 1, 2, 3, 4);
        quantityInExpertDateFromDateTo = addSGProp("quantityInExpertDateFromDateTo", "Кол-во участ.",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inExpertVoteDateFromDateTo, 1, 2, 3, 4), 1, 3, 4); // в скольки заседаниях поучавствовал

        LCP expertVoteMonthYear = addJProp(baseLM.and1, addJProp(baseLM.equals2, 3, addJProp(baseLM.numberMonthInDate, dateExpertVote, 1, 2), 1, 2), 1, 2, 3,
                addJProp(baseLM.equals2, 3, addJProp(baseLM.yearInDate, dateExpertVote, 1, 2), 1, 2), 1, 2, 4);
        doneExpertVoteMonthYear = addJProp("doneExpertVoteMonthYear", "Проголосовал в текущем месяце", baseLM.and1, doneNewExpertVote, 1, 2, expertVoteMonthYear, 1, 2, 3, 4);
        quantityDoneExpertMonthYear = addSGProp("quantityDoneExpertMonthYear", "Кол-во голосов.",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneExpertVoteMonthYear, 1, 2, 3, 4), 1, 3, 4); // в скольки заседаниях поучавствовал за месяц

        clusterVote = addDProp(idGroup, "clusterVote", "Кластер (ИД)", cluster, vote);
        nameNativeClusterVote = addJProp(baseGroup, "nameNativeClusterVote", "Кластер", nameNative, clusterVote, 1);
        nameForeignClusterVote = addJProp("nameForeignClusterVote", "Кластер (иностр.)", nameForeign, clusterVote, 1);

        maxForesightProjectCluster = addMGProp("maxForesightProjectCluster", "Форсайт", addJProp(baseLM.and1, object(foresight), 2, inProjectForesight, 1, 2), 1, clusterForesight, 2);
        nameMaxForesightVote = addJProp("nameMaxForesightVote", "Форсайт", nameNative, addJProp(maxForesightProjectCluster, projectVote, 1, clusterVote, 1), 1);
//        maxForesightVote = addMGProp("maxForesightVote", "Форсайт", addJProp(and(false, false, false), 2, inProjectForesight, 1, 2, addJProp(baseLM.equals2, object(project), 1, projectVote, 2), 1, 3, addJProp(baseLM.equals2, clusterForesight, 1, clusterVote, 2),  2, 3), 3);
//        nMaxForesightVote = addJProp("nMaxForesightVote", "Форсайт", nameNative, maxForesightVote, 1);

        doneClusterExpertVoteDateFromDateTo = addJProp(and(false, false), doneNewExpertVote, 2, 3, betweenExpertVoteDateFromDateTo, 2, 3, 4, 5, addJProp(baseLM.equals2, 1, clusterVote, 2), 1, 3);
        quantityDoneClusterExpertDateFromDateTo = addSGProp("quantityDoneClusterExpertDateFromDateTo", "Кол-во голосов.",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneClusterExpertVoteDateFromDateTo, 1, 2, 3, 4, 5), 1, 2, 4, 5); // в скольки заседаниях голосовал
        inClusterExpertVoteDateFromDateTo = addJProp(and(false, false), inNewExpertVote, 2, 3, betweenExpertVoteDateFromDateTo, 2, 3, 4, 5, addJProp(baseLM.equals2, 1, clusterVote, 2), 1, 3);
        quantityInClusterExpertDateFromDateTo = addSGProp("quantityInClusterExpertDateFromDateTo", "Кол-во участ.",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inClusterExpertVoteDateFromDateTo, 1, 2, 3, 4, 5), 1, 2, 4, 5);    // в скольки заседаниях участвовал

        clusterVotedProjectExpert = addJProp(baseGroup, "clusterVotedProjectExpert", "Кластер результ. заседания", clusterVote, voteProjectExpert, 1, 2);

        quantityVoteProjectCluster = addSGProp(baseGroup, "quantityVoteProjectCluster", true, "Кол-во заседаний", addCProp(IntegerClass.instance, 1, vote), projectVote, 1, clusterVote, 1);
        quantityClusterVotedProject = addSGProp(baseGroup, "quantityClusterVotedProject", true, "Кол-во кластеров (засед.)", addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), quantityVoteProjectCluster, 1, 2), 1);
        quantityClusterVotedProject.setFixedCharWidth(1);

        isPrevVoteVote = addJProp("isPrevVoteVote", "Пред.", and(false, false, false), addJProp(baseLM.equals2, projectVote, 1, projectVote, 2), 1, 2,
                addJProp(baseLM.equals2, clusterVote, 1, clusterVote, 2), 1, 2,
                addJProp(baseLM.less2, dateStartVote, 1, dateStartVote, 2), 1, 2,
                incrementVote, 1);
        countPrevVote = addSGProp("countPrevVote", "Кол-во пред. заседания", addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1, vote), 2, isPrevVoteVote, 1, 2), 2);

        clusterInExpertVote = addJProp("clusterInExpertVote", "Вкл", inClusterExpert, clusterVote, 2, 1);

        voteSucceededProjectCluster = addAGProp(idGroup, "voteSucceededProjectCluster", true, "Успешное заседание (ИД)", succeededVote, 1, projectVote, 1, clusterVote, 1);
        voteOpenedSucceededProject = addAGProp(idGroup, "voteOpenedSucceededProject", true, "Успешное открытое заседание (ИД)", openedSucceededVote, 1, projectVote, 1);

        noCurrentVoteProject = addJProp(projectStatusGroup, "noCurrentVoteProject", "Нет текущих заседаний", baseLM.andNot1, is(project), 1, voteInProgressProject, 1); // нету текущих заседаний

        valuedProjectCluster = addJProp(idGroup, "valuedProjectCluster", "Заседание состоялось", closedVote, voteSucceededProjectCluster, 1, 2);
        voteValuedProjectCluster = addJProp(idGroup, "voteValuedProjectCluster", true, "Оцененное заседание (ИД)", baseLM.and1, voteSucceededProjectCluster, 1, 2, valuedProjectCluster, 1, 2);
//        voteValuedProject = addJProp(idGroup, "voteValuedProject", true, "Оцененнное заседание (ИД)", baseLM.and1, voteSucceededProject, 1, noCurrentVoteProject, 1); // нет открытого заседания и есть состояшееся заседания

        voteLastProject = addMGProp(idGroup, "voteLastProject", true, "Посл. заседание (ИД)", object(vote), projectVote, 1);

        dateStartVoteLastProject = addJProp("dateStartVoteLastProject", true, "Дата начала посл. заседания", dateStartVote, voteLastProject, 1);
        dateEndVoteLastProject = addJProp("dateEndVoteLastProject", true, "Дата окончания посл. заседания", dateEndVote, voteLastProject, 1);

        acceptedProjectCluster = addJProp(baseGroup, "acceptedProjectCluster", true, "Оценен положительно", acceptedVote, voteValuedProjectCluster, 1, 2);
        rejectedProjectCluster = addJProp(baseGroup, "rejectedProjectCluster", true, "Оценен отрицательно", baseLM.andNot1, voteValuedProjectCluster, 1, 2, acceptedProjectCluster, 1, 2);

        clusterAcceptedProject = addAGProp(idGroup, "clusterAcceptedProject", true, "Кластер (ИД)", acceptedProjectCluster, 2);
        acceptedProject = addJProp(baseGroup, "acceptedProject", true, "Оценен положительно", baseLM.and1, addCProp(LogicalClass.instance, true, project), 1, clusterAcceptedProject, 1);

        currentCluster = addSDProp("currentCluster", "Текущий кластер", cluster);

        nameNativeShortCurrentCluster = addJProp("nameNativeShortCurrentCluster", "Кластер", nameNativeShort, currentCluster);

        numberCluster = addDProp(baseGroup, "numberCluster", "Приоритет", IntegerClass.instance, cluster);
        clusterNumber = addAGProp("clusterName", "Кластер (ИД)", numberCluster);
        nameNativeShortAggregateClusterProject = addOGProp(baseGroup, "nameNativeShortAggregateClusterProject", false, "Кластеры", GroupType.STRING_AGG, 1, Settings.instance.isDefaultOrdersNotNull(), true, 2, addJProp(baseLM.and1, nameNativeShort, 2, inProjectCluster, 1, 2), 1, 2, addCProp(StringClass.get(5), ";"), numberCluster, 2, 1);

        currentClusterProject = addJProp("currentClusterProject", true, "Рабочий кластер (ИД)", clusterNumber,
                addMGProp(addJProp(and(false, true), numberCluster, 2, inProjectCluster, 1, 2, rejectedProjectCluster, 1, 2), 1), 1);

        firstClusterProject = addJProp("firstClusterProject", true, "Первый кластер (ИД)", clusterNumber,
                addMGProp(addJProp(and(false), numberCluster, 2, inProjectCluster, 1, 2), 1), 1);

        lastClusterProject = addJProp("lastClusterProject", true, "Последний кластер (ИД)", clusterNumber,
                addJProp(baseLM.minus,
                        addMGProp(addJProp(and(false), addJProp(baseLM.minus, numberCluster, 1), 2, inProjectCluster, 1, 2), 1), 1), 1);

        finalClusterProject = addSUProp("finalClusterProject", true, "Тек. кластер (ИД)", Union.OVERRIDE, lastClusterProject, currentClusterProject, clusterAcceptedProject);
        nameNativeFinalClusterProject = addJProp(projectInformationGroup, "nameNativeFinalClusterProject", "Тек. кластер", nameNative, finalClusterProject, 1);
        nameForeignFinalClusterProject = addJProp(projectInformationGroup, "nameForeignFinalClusterProject", "Тек. кластер (иностр.)", nameForeign, finalClusterProject, 1);
        nameNativeShortFinalClusterProject = addJProp(projectInformationGroup, "nameShortFinalClusterProject", "Тек. кластер (сокр.)", nameNativeShort, finalClusterProject, 1);
        emailFinalClusterProject = addJProp("emailFinalClusterProject", "E-mail тек. кластера", baseLM.email, finalClusterProject, 1);
        inProjectCurrentCluster = addJProp(baseGroup, "inProjectCurrentCluster", "Вкл", inProjectCluster, 1, currentCluster);

        finalClusterProjectVote = addJProp("finalClusterProjectVote", "Тек. кластер (ИД)", finalClusterProject, projectVote, 1);
        nameNativeFinalClusterProjectVote = addJProp("nameNativeFinalClusterProjectVote", "Тек. кластер", nameNative, finalClusterProjectVote, 1);

        nativeSubstantiationFinalClusterProject = addJProp(projectInformationGroup, "nativeSubstantiationFinalClusterProject", "Обоснование выбора (тек. кластер)", nativeSubstantiationProjectCluster, 1, finalClusterProject, 1);
        foreignSubstantiationFinalClusterProject = addJProp(projectInformationGroup, "foreignSubstantiationFinalClusterProject", "Обоснование выбора (тек. кластер) (иностр.)", foreignSubstantiationProjectCluster, 1, finalClusterProject, 1);

        lastClusterProjectVote = addJProp("lastClusterProjectVote", "Посл. кластер (ИД)", lastClusterProject, projectVote, 1);
        isLastClusterVote = addJProp("isLastClusterVote", "Посл. кластер", baseLM.equals2, lastClusterProjectVote, 1, clusterVote, 1);

        rejectedProject = addJProp("rejectedProject", true, "Оценен отрицательно", baseLM.andNot1, addCProp(LogicalClass.instance, true, project), 1, currentClusterProject, 1);

        valuedProject = addJProp("valuedProject", true, "Оценен", baseLM.and1, addSUProp(Union.OVERRIDE, acceptedProject, rejectedProject), 1, quantityClusterProject, 1);

        needExtraVoteProject = addJProp("needExtraVoteProject", true, "Треб. заседание", and(true, true, true, true, false),
                is(project), 1,
                withdrawnProject, 1,
                notEnoughProject, 1,
                voteInProgressProject, 1,
                clusterAcceptedProject, 1,
                currentClusterProject, 1); // есть открытое заседания и есть состояшееся заседания !!! нужно создать новое заседание

        needExtraVoteRepeatProject = addJProp("needExtraVoteRepeatProject", true, "Треб. заседание (повторно)", baseLM.and1, needExtraVoteProject, 1, quantityVoteProject, 1);

//        clusterVote.setDerivedForcedChange(false, currentClusterProject, 1, is(vote), 1);
//        clusterVote = addDCProp(idGroup, "clusterVote", "Кластер (ИД)", true, currentClusterProject, true, projectVote, 1);

        addConstraint(addJProp("Эксперт не соответствует необходимому кластеру", baseLM.andNot1,
                inExpertVote, 1, 2, clusterInExpertVote, 1, 2), false);
//        addConstraint(addJProp("Эксперт не соответствует необходимому кластеру", baseLM.diff2,
//                clusterExpert, 1, addJProp(baseLM.and1, clusterVote, 2, inCorExpertVote, 1, 2), 1, 2), false);

//        addConstraint(addJProp("Количество экспертов не соответствует требуемому", baseLM.andNot1, is(vote), 1, addJProp(baseLM.equals2, requiredQuantity,
//                addSGProp(addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inCorExpertVote, 2, 1), 1), 1), 1), false);

        generateDocumentsProjectDocumentType = addAProp(actionGroup, new GenerateDocumentsActionProperty());
        includeDocumentsProject = addAProp(actionGroup, new IncludeDocumentsActionProperty());
        importProjectSidsAction = addAProp(importGroup, new ImportProjectsActionProperty("Импортировать идентификаторы проектов", this, BL, false, false, true));
        showProjectsToImportAction = addAProp(importGroup, new ImportProjectsActionProperty("Посмотреть импортируемые проекты", this, BL, true, false, false));
        showProjectsReplaceToImportAction = addAProp(importGroup, new ImportProjectsActionProperty("Посмотреть замещаемые проекты", this, BL, true, true, false));
        importProjectsAction = addAProp(importGroup, new ImportProjectsActionProperty("Импортировать", this, BL, false, false, false));
        copyProjectAction = addAProp(actionGroup, new CopyProjectActionProperty("Копировать", this, project));
        openApplicationProjectAction = addAProp(actionGroup, new OpenApplicationProjectActionProperty(false));
        openCompleteApplicationProjectAction = addAProp(actionGroup, new OpenApplicationProjectActionProperty(true));
        exportExpertsAction = addAProp(actionGroup, new ExportExpertsActionProperty(this, false));
        exportExpertsEscapeAction = addAProp(actionGroup, new ExportExpertsActionProperty(this, true));
        exportProjectDocumentsAction = addAProp(actionGroup, new ExportProjectDocumentsActionProperty("Экспортировать документы", this, project));
        importIPsExpertVoteAction = addAProp(actionGroup, new ImportIPsExpertVoteActionProperty());
        importProjectSidsAction = addAProp(importGroup, new ImportProjectsActionProperty("Импортировать идентификаторы проектов", this, BL, false, false, true));

        generateVoteProject = addAProp(actionGroup, new GenerateVoteActionProperty());
        copyResultsVote = addAProp(actionGroup, new CopyResultsActionProperty());
        needNameExtraVoteProject = addJProp(and(false, false, false, false), needExtraVoteProject, 1, nameNativeProject, 1, nameForeignProject, 1, nameNativeClaimerProject, 1, nameForeignClaimerProject, 1);
        hideGenerateVoteProject = addHideCaptionProp(privateGroup, "Сгенерировать заседание", needNameExtraVoteProject);
//        generateVoteProject.setDerivedForcedChange(addCProp(ActionClass.instance, true), needExtraVoteProject, 1, autoGenerateProject, 1);

        includeProjectClusterForesight = addAProp(actionGroup, new IncludeProjectClusterForesightActionProperty(project, cluster, foresight));
        includeProjectClusterForesight.setImage("include.png");

        baseLM.generateLoginPassword.setEventSetAction(this, is(expert), 1);

        expertLogin = addAGProp(baseGroup, "expertLogin", "Эксперт (ИД)", baseLM.userLogin);
        disableExpert = addDProp(baseGroup, "disableExpert", "Не акт.", LogicalClass.instance, expert);

//        LCP userRole = addCUProp("userRole", true, "Роль пользователя", baseLM.customUserSIDMainRole);
        LCP userRole = addSUProp("userRole", true, "Роль пользователя", Union.OVERRIDE, baseLM.customUserSIDMainRole, addCProp(StringClass.get(30), "expert", expert));

//        voteValuedProject, 1, addIfElseUProp(addCProp(projectStatus, "accepted", project), addCProp(projectStatus, "rejected", project), acceptedProject, 1), 1,
//        voteSucceededProject, 1, addCProp(projectStatus, "succeeded", project), 1,
//        voteInProgressProject, 1, addCProp(projectStatus, "inProgress", project), 1,

        isConsultingCenterQuestionProject = addDProp(consultingCenterGroup, "isConsultingCenterQuestionProject", "Был ли задан вопрос о консультативном центре", LogicalClass.instance, project);
        isConsultingCenterCommentProject = addDProp(consultingCenterGroup, "isConsultingCenterCommentProject", "Утвердительный ответ", LogicalClass.instance, project);
        consultingCenterCommentProject = addDProp(consultingCenterGroup, "consultingCenterCommentProject", "Комментарий о консультативном центре", InsensitiveStringClass.get(2000), project);
        consultingCenterCommentProject.setMinimumWidth(50);
        consultingCenterCommentProject.setPreferredWidth(50);

        //quantitySupplierArticleBetweenDates = addSGProp("quantitySupplierArticleBetweenDates", "Проданное кол-во",
        //        addJProp(and(false,false), innerQuantity, 1, 2, 3, is(orderSale), 1, betweenDate2, 1, 4, 5), contragentOrder, 3, 2, 4, 5);

        fillNativeProject = addDProp(projectOptionsGroup, "fillNativeProject", "Анкета на русском", LogicalClass.instance, project);
        fillForeignProject = addDProp(projectOptionsGroup, "fillForeignProject", "Анкета на английском", LogicalClass.instance, project);
        langProject = addSFUProp(projectOptionsGroup, "langProject", "Язык", "/",
                addJProp(baseLM.and1, addCProp(StringClass.get(10), "Рус"), fillNativeProject, 1),
                addJProp(baseLM.and1, addCProp(StringClass.get(10), "Англ"), fillForeignProject, 1));

        transferredProject = addDProp(translationGroup, "transferredProject", "Переведена", LogicalClass.instance, project);
        dateTransferredProject = addDCProp(translationGroup, "dateTransferredProject", "Дата перевода", true, baseLM.currentDate, transferredProject, 1);

        translatedToRussianProject = transferredProject; // addDProp(projectTranslationsGroup, "translatedToRussianProject", "Переведено на русский", LogicalClass.instance, project);
        translatedToEnglishProject = transferredProject; // addDProp(projectTranslationsGroup, "translatedToEnglishProject", "Переведено на английский", LogicalClass.instance, project);

        needsToBeTranslatedToRussianProject = addJProp(and(true, true), addCProp(LogicalClass.instance, true, project), 1, fillNativeProject, 1, translatedToRussianProject, 1);
        needsToBeTranslatedToEnglishProject = addJProp(and(true, true), addCProp(LogicalClass.instance, true, project), 1, fillForeignProject, 1, translatedToEnglishProject, 1);

//        hideTranslatedToRussianProject = addHideCaptionProp(privateGroup, "Переведено", translatedToRussianProject, fillForeignProject);
//        hideTranslatedToEnglishProject = addHideCaptionProp(privateGroup, "Переведено", translatedToEnglishProject, fillNativeProject);

        dateJoinProject = addJProp(baseLM.and1, baseLM.date, 1, is(project), 1);
        dateDataProject = addDProp("dateDataProject", "Дата", DateClass.instance, project);
        dateProject = addSUProp("dateProject", "Дата проекта", Union.OVERRIDE, dateJoinProject, dateDataProject);
        updateDateProject = addDProp(projectInformationGroup, "updateDateProject", "Дата изменения проекта", DateTimeClass.instance, project);
        statusDateProject = addDProp(baseGroup, "statusDateProject", "Дата подачи на статус участника", DateClass.instance, project);
        dateStatusDataProject = addDProp("dateStatusDataProject", "Дата", DateClass.instance, project);
        dateStatusProject = addSUProp(projectInformationGroup, "dateStatusProject", "Дата подачи на статус участника", Union.OVERRIDE, statusDateProject, dateStatusDataProject);

        betweenDateProject = addJProp(baseLM.between, dateProject, 1, object(DateClass.instance), 2, object(DateClass.instance), 3);

        sumPositiveConsultingCenterCommentProject = addSGProp(consultingCenterStatGroup, "sumPositiveConsultingCenterCommentProject", "Сумма положительных комментариев",
                addJProp(and(false, false, false, true), addCProp(IntegerClass.instance, 1), isConsultingCenterQuestionProject, 1, isConsultingCenterCommentProject, 1, betweenDateProject, 1, 2, 3, inactiveProject, 1),
                2, 3);
        sumNegativeConsultingCenterCommentProject = addSGProp(consultingCenterStatGroup, "sumNegativeConsultingCenterCommentProject", "Сумма отрицательных комментариев",
                addJProp(and(false, true, false, true), addCProp(IntegerClass.instance, 1), isConsultingCenterQuestionProject, 1, isConsultingCenterCommentProject, 1, betweenDateProject, 1, 2, 3, inactiveProject, 1),
                2, 3);
        sumTotalConsultingCenterCommentProject = addSGProp(consultingCenterStatGroup, "sumTotalConsultingCenterCommentProject", "Всего комментариев",
                addJProp(and(false, false, true), addCProp(IntegerClass.instance, 1), isConsultingCenterQuestionProject, 1, betweenDateProject, 1, 2, 3, inactiveProject, 1),
                2, 3);

        //формальная экспертиза и юридическая проверка

        projectFormalControl = addDProp("projectFormalControl", "Проект (ИД)", project, formalControl);
//        dateProjectFormalControl = addJProp("dateProjectFormalControl", "Дата проекта", dateProject, projectFormalControl, 1);
        clusterProjectFormalControl = addJProp("clusterProjectFormalControl", "Кластер (ИД)", finalClusterProject, projectFormalControl, 1);
        nameNativeClusterProjectFormalControl = addJProp("nameNativeClusterProjectFormalControl", "Кластер", nameNative, clusterProjectFormalControl, 1);
        resultFormalControl = addDProp("resultFormalControl", "Решение формальной экспертизы", formalControlResult, formalControl);
        sidResultFormalControl = addJProp("sidResultFormalControl", "Решение формальной экспертизы (ИД)", baseLM.classSID, resultFormalControl, 1);

                                     //кнопки
        nameResultFormalControl = addJProp("nameResultFormalControl", "Решение формальной экспертизы", baseLM.name, resultFormalControl, 1);
        commentFormalControl = addDProp("commentFormalControl", "Комментарий", TextClass.instance, formalControl);
        // notAvailableForStatusProjectStatus = addDProp(baseGroup, "notAvailableForStatusProjectStatus", "Не действует для заявки на статус", LogicalClass.instance, projectStatus);
        // notAvailableForPreliminaryProjectStatus = addDProp(baseGroup, "notAvailableForPreliminaryProjectStatus", "Не действует для заявки на предварительную экспертизу", LogicalClass.instance, projectStatus);

        updateDateFormalControl = addJProp("updateDateFormalControl", "Дата изменения проекта", updateDateProject, projectFormalControl, 1);

        dateTimeSubmitFormalControl = addDProp("dateTimeSubmitFormalControl", "Дата/время отправки проекта", DateTimeClass.instance, formalControl);
        dateTimeSubmitFormalControl.setEventChangeNewSet(updateDateFormalControl, 1, is(formalControl), 1);
        dateSubmitFormalControl = addJProp("dateSubmitFormalControl", "Дата отправки проекта", baseLM.toDate, dateTimeSubmitFormalControl, 1);

        dateTimeFormalControl = addTCProp(Time.DATETIME, "dateTimeFormalControl", true, "Дата/время экспертизы", resultFormalControl);
        dateFormalControl = addJProp("dateFormalControl", "Дата экспертизы", baseLM.toDate, dateTimeFormalControl, 1);
        resultNoticedFormalControl = addDProp("resultNoticedFormalControl", "Отослано уведомление", LogicalClass.instance, formalControl);
        dateResultNoticedFormalControl = addDProp("dateResultNoticedFormalControl", "Дата отсылки уведомления", DateClass.instance, formalControl);

        LCP defaultCountry = BL.getModule("Country").getLCPByName("defaultCountry");
        overdueDateFormalControl = addJProp("overdueDateFormalControl", "Дата просрочки формальной экспертизы", baseLM.sumDate, addJProp(baseLM.jumpWorkdays, defaultCountry, dateResultNoticedFormalControl, 1, addCProp(IntegerClass.instance, 1)), 1, addJProp(baseLM.subtractDate, overduePeriod, addCProp(IntegerClass.instance, 1)));

        addNotEnoughDocumentsFCResult = addJoinAProp(formalControlResultGroup, "addNotEnoughDocumentsFCResult", "Неполный перечень документов", addAAProp(formalControl, resultFormalControl), addCProp(formalControlResult, "notEnoughDocuments"));
        addNoListOfExpertsFCResult = addJoinAProp(formalControlResultGroup, "Отсутствует перечень экспертов", addAAProp(formalControl, resultFormalControl), addCProp(formalControlResult, "noListOfExperts"));
        addNotSuitableClusterFCResult = addJoinAProp(formalControlResultGroup, "Не соответствует направлению", addAAProp(formalControl, resultFormalControl), addCProp(formalControlResult, "notSuitableCluster"));
        addRepeatedFCResult = addJoinAProp(formalControlResultGroup, "Подана повторно", addAAProp(formalControl, resultFormalControl), addCProp(formalControlResult, "repeatedFC"));
        addPositiveFCResult = addJoinAProp(formalControlResultGroup, "Прошла формальную экспертизу", addAAProp(formalControl, resultFormalControl), addCProp(formalControlResult, "positiveFormalResult"));

        projectLegalCheck = addDProp("projectLegalCheck", "Проект (ИД)", project, legalCheck);

        resultLegalCheck = addDProp("resultLegalCheck", "Решение юридической проверки", legalCheckResult, legalCheck);
        positiveResultLegalCheck = addJProp("positiveResultLegalCheck", "Положительное решение юридической проверки", baseLM.equals2, resultLegalCheck, 1, addCProp(legalCheckResult, "positiveLegalCheckResult"));
        negativeResultLegalCheck = addJProp("negativeResultLegalCheck", "Отрицательное решение юридической проверки", baseLM.equals2, resultLegalCheck, 1, addCProp(legalCheckResult, "negativeLegalCheckResult"));

        addNegativeLCResult = addJoinAProp(legalCheckResultGroup, "Не прошла юридическую проверку", addAAProp(legalCheck, resultLegalCheck), addCProp(legalCheckResult, "negativeLegalCheckResult"));
        addPositiveLCResult = addJoinAProp(legalCheckResultGroup, "Прошла юридическую проверку", addAAProp(legalCheck, resultLegalCheck), addCProp(legalCheckResult, "positiveLegalCheckResult"));
        commentLegalCheck = addDProp("commentLegalCheck", "Комментарий", TextClass.instance, legalCheck);
        nameResultLegalCheck = addJProp("nameResultLegalCheck", "Решение юридической проверки", baseLM.name, resultLegalCheck, 1);

        dateProjectLegalCheck = addJProp("dateProjectLegalCheck", "Дата проекта", dateProject, projectLegalCheck, 1);
        sidResultLegalCheck = addJProp("sidResultLegalCheck", "Решение юридической проверки (ИД)", baseLM.classSID, resultLegalCheck, 1);
        clusterProjectLegalCheck = addJProp("clusterProjectLegalCheck", "Кластер (ИД)", finalClusterProject, projectLegalCheck, 1);
        nameNativeClusterProjectLegalCheck = addJProp("nameNativeClusterProjectLegalCheck", "Кластер", nameNative, clusterProjectLegalCheck, 1);

        updateDateLegalCheck = addJProp("updateDateLegalCheck", "Дата изменения проекта", updateDateProject, projectLegalCheck, 1);

        dateTimeSubmitLegalCheck = addDProp("dateTimeSubmitLegalCheck", "Дата/время отправки проекта", DateTimeClass.instance, legalCheck);
        dateTimeSubmitLegalCheck.setEventChangeNewSet(updateDateLegalCheck, 1, is(legalCheck), 1);
        dateSubmitLegalCheck = addJProp("dateSubmitLegalCheck", "Дата отправки проекта", baseLM.toDate, dateTimeSubmitLegalCheck, 1);

        resultNoticedLegalCheck = addDProp("resultNoticedLegalCheck", "Отослано уведомление", LogicalClass.instance, legalCheck);
        dateResultNoticedLegalCheck = addDProp("dateResultNoticedLegalCheck", "Дата отсылки уведомления", DateClass.instance, legalCheck);

        dateTimeLegalCheck = addTCProp(Time.DATETIME, "dateTimeLegalCheck", true, "Дата проверки", resultLegalCheck);
        dateLegalCheck = addJProp("dateLegalCheck", "Дата отправки уведомления", baseLM.toDate, dateTimeLegalCheck, 1);

        isR1LegalCheck = addJProp("isR1LegalCheck", "ЮП по R1", isR1Project, projectLegalCheck, 1);
        projectActionLegalCheck = addDProp(idGroup, "projectActionLegalCheck", "Тип заявки (ИД)", projectAction, legalCheck);
        nameProjectActionLegalCheck = addJProp(baseGroup, "nameProjectActionLegalCheck", "Тип заявки", baseLM.name, projectActionLegalCheck, 1);
        nameProjectActionLegalCheck.setPreferredCharWidth(20);
        projectActionLegalCheck.setEventChangeNew(addJProp(projectActionProject, projectLegalCheck, 1), 1, is(legalCheck), 1);

        maxForesightLegalChek = addJProp("maxForesightLegalChek", maxForesightProjectCluster, projectLegalCheck, 1, addJProp(firstClusterProject, projectLegalCheck, 1), 1);
        nameMaxForesightLegalChek = addJProp("nameMaxForesightLegalChek", "Форсайт", nameNative, maxForesightLegalChek, 1);

        LCMinDateProjectActionProject = addMGProp("LCMinDateProjectActionProject", true, "Дата первой отсылки ЮП", true, dateResultNoticedLegalCheck, projectActionLegalCheck, 1, projectLegalCheck, 1);
        minDateLegalCheck = addJProp("minDateLegalCheck", "Дата отсылки", LCMinDateProjectActionProject, projectActionLegalCheck, 1, projectLegalCheck, 1);
        overdueDateLegalCheck = addJProp("overdueDateLegalCheck", "Дата просрочки юридической проверки", baseLM.sumDate,
                addJProp(baseLM.jumpWorkdays, defaultCountry, minDateLegalCheck, 1, addCProp(IntegerClass.instance, 1)), 1,
                addJProp(baseLM.subtractDate, overduePeriod, addCProp(IntegerClass.instance, 1)));

        userLegalCheck = addDCProp("userLegalCheck", "Пользователь ЮП (ИД)", true, baseLM.currentUser, resultLegalCheck, 1);
        nameUserLegalCheck = addJProp("nameUserLegalCheck", "Пользователь ЮП", baseLM.name, userLegalCheck, 1);

        changeLegalCheck = addDProp("changeLegalCheck", "Изменен тип заявки", LogicalClass.instance, legalCheck);
        noticedChangeLegalCheck = addDProp(changeLegalCheckGroup, "noticedChangeLegalCheck", "Отослано уведомление", LogicalClass.instance, legalCheck);
        dateChangeLegalCheck = addDProp(changeLegalCheckGroup, "dateChangeLegalCheck", "Дата отсылки уведомления типа заявки", DateClass.instance, legalCheck);
//        maxFormalControlProjectProps = addMGProp((AbstractGroup) null, new String[]{"maxDateFormalControlProject", "currentFCProject"}, new String[]{"Дата посл. формальной экспертизы.", "Посл. формальная экспертиза"}, 1,
//                dateTimeFormalControl, 1, projectFormalControl, 1);
//        LCP currentDateFormalControlProject = maxFormalControlProjectProps[0];
        currentFormalControlProject = addMGProp("currentFormalControlProject", "Последняя формальная проверка", object(formalControl), projectFormalControl, 1);

        needNoticeNegativeLegalCheck = addJProp("needNoticeNegativeLegalCheck", true, "Требуется отсылка уведомления", baseLM.andNot1, negativeResultLegalCheck, 1, resultNoticedLegalCheck, 1);
        needNoticeChangeLegalCheck = addJProp("needNoticeChangeLegalCheck", true, "Требуется отсылка уведомления (изменение типа заявки)", baseLM.andNot1, changeLegalCheck, 1, noticedChangeLegalCheck, 1);
        needNoticeLegalCheck = addSUProp("needNoticeLegalCheck", true, "Требуется отсылка уведомления", Union.OVERRIDE, needNoticeNegativeLegalCheck, needNoticeChangeLegalCheck);

        projectActionFormalControl = addDProp(idGroup, "projectActionFormalControl", "Тип заявки (ИД)", projectAction, formalControl);
        nameProjectActionFormalControl = addJProp(baseGroup, "nameProjectActionFormalControl", "Тип заявки", baseLM.name, projectActionFormalControl, 1);
        nameProjectActionFormalControl.setPreferredCharWidth(20);
        projectActionFormalControl.setEventChangeNew(addJProp(projectActionProject, projectFormalControl, 1), 1, is(formalControl), 1);

        executeFormalControlProject = addJProp("executeFormalControlProject", true, "Действующая", and(false, false),
                currentFormalControlProject, 1,
                addJProp(baseLM.greater2, addJProp(dateTimeFormalControl, currentFormalControlProject, 1), 1, updateDateProject, 1), 1,
                addJProp(baseLM.equals2, addJProp(projectActionFormalControl, currentFormalControlProject, 1), 1, projectActionProject, 1), 1);

        dateExecuteFormalControlProject = addJProp("dateExecuteFormalControlProject", true, "Дата действующего решения формальной экспертизы", dateFormalControl, executeFormalControlProject, 1);
        resultExecuteFormalControlProject = addJProp("resultExecuteFormalControlProject", true, "Решение", resultFormalControl, executeFormalControlProject, 1);
        nameResultExecuteFormalControlProject = addJProp("nameResultExecuteFormalControlProject", "Решение", baseLM.name, resultExecuteFormalControlProject, 1);

        notEnoughDocumentsProject = addJProp("notEnoughDocumentsProject", true, "Неполный перечень документов", baseLM.equals2, resultExecuteFormalControlProject, 1, addCProp(formalControlResult, "notEnoughDocuments"));
        isPreliminaryNotEnoughDocumentProject = addJProp("isPreliminaryNotEnoughDocumentProject", true, "Неполный перечень документов (на экспертизу)", baseLM.and1, notEnoughDocumentsProject, 1, isPreliminaryProject, 1);
        isStatusNotEnoughDocumentProject = addJProp("isStatusNotEnoughDocumentProject", true, "Неполный перечень документов (на статус)", baseLM.and1, notEnoughDocumentsProject, 1, isStatusProject, 1);
        noListOfExpertsProject = addJProp("noListOfExpertsProject", true, "Отсутствует перечень экспертов", baseLM.equals2, resultExecuteFormalControlProject, 1, addCProp(formalControlResult, "noListOfExperts"));
        notSuitableClusterProject = addJProp("notSuitableClusterProject", true, "Не соответствует направлению", baseLM.equals2, resultExecuteFormalControlProject, 1, addCProp(formalControlResult, "notSuitableCluster"));
        repeatedProject = addJProp("repeatedProject", true, "Подана повторно", baseLM.equals2, resultExecuteFormalControlProject, 1, addCProp(formalControlResult, "repeatedFC"));
        positiveFormalResultProject = addJProp("positiveFormalResultProject", true, "Прошла формальную экспертизу", baseLM.equals2, resultExecuteFormalControlProject, 1, addCProp(formalControlResult, "positiveFormalResult"));
        negativeFormalResultProject = addJProp("negativeFormalResultProject", true, "Не прошла формальную экспертизу", baseLM.andNot1, resultExecuteFormalControlProject, 1, positiveFormalResultProject, 1);

        isPreliminaryFormalControl = addJProp("isPreliminaryFormalControl", "На предв. экспертизу", baseLM.equals2, projectActionFormalControl, 1, addCProp(projectAction, "preliminary"));
        isStatusFormalControl = addJProp("isStatusFormalControl", "На статус", baseLM.equals2, projectActionFormalControl, 1, addCProp(projectAction, "status"));
        datePreliminaryProjectFormalControl = addJProp("datePreliminaryProjectFormalControl", "Дата подачи на предварительную экспертизу", dateProject, projectFormalControl, 1);
        dateStatusProjectFormalControl = addJProp("dateStatusProjectFormalControl", "Дата подачи на статус", dateStatusProject, projectFormalControl, 1);
        dateProjectFormalControl = addIfElseUProp(privateGroup, "dateProjectFormalControl", "Дата проекта",
                datePreliminaryProjectFormalControl,
                dateStatusProjectFormalControl,
                isPreliminaryFormalControl, 1);

        prevFormalControl = addOProp("prevFormalControl", "Пред. формальная экспертиза", PartitionType.PREVIOUS, object(formalControl), true, true, 2, projectFormalControl, 1, projectActionFormalControl, 1, object(formalControl), 1);
        datePrevFormalControl = addJProp("datePrevFormalControl", "Дата пред. формальной эскпертизы", dateFormalControl, prevFormalControl, 1);

        overdueFormalControlProject = addJProp("overdueFormalControlProject", true, "Просрочена формальная экспертиза", baseLM.and1,
                addJProp(baseLM.greater2, baseLM.currentDate, addJProp(overdueDateFormalControl, executeFormalControlProject, 1), 1), 1,
                notEnoughDocumentsProject, 1);

        // последняя юридическая проверка
//        maxLegalCheckProjectProps = addMGProp((AbstractGroup) null, new String[]{"maxDateLegalCheckProject", "currentLCProject"}, new String[]{"Дата посл. юр. проверки", "Посл. юр. проверка"}, 1,
//                dateTimeLegalCheck, 1, projectLegalCheck, 1);
//        LCP currentDateLegalCheckProject = maxLegalCheckProjectProps[0];
        currentLegalCheckProject = addMGProp("currentLegalCheckProject", "Последняя юридическая проверка", object(legalCheck), projectLegalCheck, 1);

        
        executeLegalCheckProject = addJProp("executeLegalCheckProject", true, "Действующая", and(false, false),
                currentLegalCheckProject, 1,
                addJProp(baseLM.greater2, addJProp(dateTimeLegalCheck, currentLegalCheckProject, 1), 1, updateDateProject, 1), 1,
                addJProp(baseLM.equals2, addJProp(projectActionLegalCheck, currentLegalCheckProject, 1), 1, projectActionProject, 1), 1);

        dateExecuteLegalCheckProject = addJProp("dateExecuteLegalCheckProject", true, "Дата действующей юридической проверки", dateLegalCheck, executeLegalCheckProject, 1);
        resultExecuteLegalCheckProject = addJProp("resultExecuteLegalCheckProject", true, "Решение", resultLegalCheck, executeLegalCheckProject, 1);
        resultNoticedLegalCheckProject = addJProp("resultNoticedLegalCheckProject", true, "Отослано уведомление", resultNoticedLegalCheck, executeLegalCheckProject, 1);

        negativeLegalResultProject = addJProp("negativeLegalResultProject", true, "Не прошла юридическую проверку", negativeResultLegalCheck, executeLegalCheckProject, 1);
        negativeLegalResultStatusProject = addJProp("negativeLegalResultStatusProject", true, "Не прошла юридическую проверку (на статус)", baseLM.and1, negativeLegalResultProject, 1, addJProp(baseLM.equals2, projectActionProject,  1, addCProp(projectAction, "status")), 1);
        negativeLegalResultPreliminaryProject = addJProp("negativeLegalResultPreliminaryProject", true, "Не прошла юридическую проверку (на предв.экспертизу)", baseLM.and1, negativeLegalResultProject, 1, addJProp(baseLM.equals2, projectActionProject,  1, addCProp(projectAction, "preliminary")), 1);
        positiveLegalResultProject = addJProp("positiveLegalResultProject", true, "Прошла юридическую проверку", positiveResultLegalCheck, executeLegalCheckProject, 1);
        datePositiveLegalResultProject = addJProp("datePositiveLegalResultProject", "Дата прохождения юридической экспертизы", baseLM.and1, dateExecuteLegalCheckProject, 1, positiveLegalResultProject, 1);

        needNoticeLegalResultProject = addJProp("needNoticeLegalResultProject", true, "Требуется отсылка уведомления", needNoticeLegalCheck, executeLegalCheckProject, 1);

        // последняя юридическая проверка (на статус)

        isPreliminaryLegalCheck = addJProp("isPreliminaryLegalCheck", "На предв. экспертизу", baseLM.equals2, projectActionLegalCheck, 1, addCProp(projectAction, "preliminary"));
        isStatusLegalCheck = addJProp("isStatusLegalCheck", "На статус", baseLM.equals2, projectActionLegalCheck, 1, addCProp(projectAction, "status"));

        prevLegalCheck = addOProp("prevLegalCheck", "Пред. юридическая проверка", PartitionType.PREVIOUS, object(legalCheck), true, true, 2, projectLegalCheck, 1, projectActionLegalCheck, 1, object(legalCheck), 1);
        datePrevLegalCheck = addJProp("datePrevLegalCheck", "Дата пред. юридической проверки", dateLegalCheck, prevLegalCheck, 1);

        positiveStatusLegalCheckProject = addMGProp("executeStatusLegalCheckProject", true, "Положительная (статус)", false,
                addJProp(baseLM.and1, object(legalCheck), 1, positiveResultLegalCheck, 1), projectLegalCheck, 1);

        datePositiveStatusLegalCheckProject = addJProp("datePositiveStatusLegalCheckProject", "Дата прохождения юридической экспертизы (статус)", dateLegalCheck, positiveStatusLegalCheckProject, 1);

        overdueLegalCheckProject = addJProp("overdueLegalCheckProject", true, "Просрочена юридическая проверка", baseLM.and1,
                addJProp(baseLM.greater2, baseLM.currentDate, addJProp(overdueDateLegalCheck, executeLegalCheckProject, 1), 1), 1,
                negativeLegalResultProject, 1);

        resultForesightCheckProject = addDProp("resultForesightCheckProject", "Решение проверки на форсайты (ИД)", foresightCheckResult, project);
        nameResultForesightCheckProject = addJProp("nameResultForesightCheckProject", "Решение проверки на форсайты", baseLM.name, resultForesightCheckProject, 1);
        sidResultForesightCheckProject = addJProp("sidResultForesightCheckProject", "Результат проверки", baseLM.classSID, resultForesightCheckProject, 1);
        dateResultForesightCheckProject = addDCProp("dateResultForesightCheckProject", "Дата проверки на форсайты", true, baseLM.currentDate, resultForesightCheckProject, 1);
        userResultForesightCheckProject = addDCProp("userResultForesightCheckProject", "Пользователь проверки на форсайты (ИД)", true, baseLM.currentUser, resultForesightCheckProject, 1);
        nameUserResultForesightCheckProject = addJProp("nameUserResultForesightCheckProject", "Пользователь проверки на форсайты", baseLM.name, userResultForesightCheckProject, 1);
        positiveResultForesightCheckProject = addJProp("positiveResultForesightCheckProject", "Положительное решение проверки на форсайты", baseLM.equals2, resultForesightCheckProject, 1, addCProp(foresightCheckResult, "positiveForesightCheckResult"));
        negativeResultForesightCheckProject = addJProp("negativeResultForesightCheckProject", "Отрицательное решение проверки на форсайты", baseLM.equals2, resultForesightCheckProject, 1, addCProp(foresightCheckResult, "negativeForesightCheckResult"));

        setNegativeResultForesightCheckProject = addSetPropertyAProp("setNegativeResultForesightCheckProject", "Не прошла проверку на соответствие форсайту", resultForesightCheckProject, 1, addCProp(foresightCheckResult, "negativeForesightCheckResult"), 1);
        setNegativeResultForesightCheckProjectApply = addListAProp("setNegativeResultForesightCheckProjectApply", "Не прошла проверку на соответствие форсайту", setNegativeResultForesightCheckProject, 1, baseLM.apply, baseLM.cancel);
        setNegativeResultForesightCheckProjectApply.property.askConfirm = true;
        setNegativeResultForesightCheckProjectApply.setImage("delete.png");

        setPositiveResultForesightCheckProject = addSetPropertyAProp("Прошла проверку на соответствие форсайты", resultForesightCheckProject, 1, addCProp(foresightCheckResult, "positiveForesightCheckResult"));

        commentForesightCheckProject = addDProp("commentForesightCheckProject", "Комментарий проверки на форсайты", TextClass.instance, project);

        addConstraint(addJProp("Вы не выбрали ни одного инновационного приоритета ( форсайта )\n" +
                " \n" +
                "Согласно п.17 новой редакции положения, ответственный сотрудник кластера должен проверить заявку на соответствие направлению деятельности Фонда, а так же определеить инновационные приоритеты (форсайты) для каждой заявки.\n" +
                "Пожалуйста выберите один или несколько инновационных приоритетов.\n" +
                "Обращаем Ваше внимание, что согласно п.5 Положения об экспертных коллегиях, под инновационным приоритетом понимается пункт списка, не включающий в себя подпунктов.", baseLM.andNot1,
                positiveResultForesightCheckProject, 1, quantityForesightProject, 1), false);

        addConstraint(addJProp("Вы выбрали более одного инновационного приоритета ( форсайта ) - временно это запрещено \n", baseLM.and1,
                positiveResultForesightCheckProject, 1, addJProp(baseLM.greater2, quantityForesightProject, 1, addCProp(IntegerClass.instance, 1)), 1), false);

//        addConstraint(addJProp("При отрицательном результате проверки на соответствие направлению деятельности Фонда необходимо указать комментарий.", baseLM.andNot1,
//                negativeResultForesightCheckProject, 1, commentForesightCheckProject, 1), false);

        needForesightCheckProject = addJProp("needForesightCheckProject", "Требуется проверка на форсайты", and(false, true),
                isR2Project, 1,
                positiveLegalResultProject, 1,
                withdrawnProject, 1);

        needTranslationProject = addJProp("needTranslationProject", true, "Требуется перевод", and(false, true),
                addSUProp(Union.OVERRIDE, needsToBeTranslatedToRussianProject, needsToBeTranslatedToEnglishProject), 1,
                positiveResultForesightCheckProject, 1,
                withdrawnProject, 1);

        needTranslationLegalChek = addJProp("needTranslationLegalChek", "Требуется перевод", needTranslationProject, projectLegalCheck, 1);
        needsToBeTranslatedToRussianLegalChek = addJProp("needsToBeTranslatedToRussianLegalChek", needsToBeTranslatedToRussianProject, projectLegalCheck, 1);
        
        needVoteProject = addJProp("needVoteProject", true, "Треб. заседание", and(false, true, true, false, true),
                is(project), 1,
                positiveResultForesightCheckProject, 1,
                voteInProgressProject, 1,
                clusterAcceptedProject, 1,
                currentClusterProject, 1,
                needTranslationProject, 1);

        sentForTranslationProject = addDProp(translationGroup, "sentForTranslationProject", "Направлена на перевод", LogicalClass.instance, project);
        sentForTranslationProject.setEventChangeNewSet(addCProp(LogicalClass.instance, true), needTranslationProject, 1);

        dateSentForTranslationProject = addDCProp(translationGroup, "dateSentForTranslationProject", "Дата направления на перевод", true, baseLM.currentDate, sentForTranslationProject, 1);
        dateToSentForTranslationProject = addDCProp(translationGroup, "dateToSentForTranslationProject", "Дата до которой д.б. переведен", baseLM.jumpWorkdays, true, defaultCountry, dateSentForTranslationProject, 1, addCProp(IntegerClass.instance, 5));

        oficialNameProjectStatus = addDProp(baseGroup, "oficialNameProjectStatus", "Наименование из регламента", StringClass.get(200), projectStatus);
        oficialNameProjectStatus.setMinimumWidth(10);
        oficialNameProjectStatus.setPreferredWidth(50);
        numberProjectStatus = addDProp(baseGroup, "numberProjectStatus", "Номер", StringClass.get(10), projectStatus);

        fileDecisionVote = addDProp("fileDecisionVote", "Решение по проекту", PDFClass.instance, vote);

        attachProtocolDecisionVote = addIfAProp(actionGroup, "attachProtocolDecisionVote", "Сгенерировать протокол", closedSucceededVote, 1, addAProp(new AttachProtocolDecisionVoteActionProperty()), 1);
        loadFileDecisionVote = addIfAProp(actionGroup, "loadFileDecisionVote", "Загрузить решение", closedSucceededVote, 1, addLFAProp(fileDecisionVote), 1);
        openFileDecisionVote = addIfAProp(actionGroup, "openFileDecisionVote", "Открыть решение", closedSucceededVote, 1, addOFAProp(fileDecisionVote), 1);

        fileDecisionProject = addJProp("fileDecisionProject", "Решение по проекту", fileDecisionVote, voteLastProject, 1);

        acceptedDecisionProject = addJProp("acceptedDecisionProject", true, "Есть решение о соответствии", baseLM.and1, acceptedProject, 1, fileDecisionProject, 1);
        rejectedDecisionProject = addJProp("rejectedDecisionProject", true, "Есть решение о несоответствии", baseLM.and1, rejectedProject, 1, fileDecisionProject, 1);

        decisionNoticedVote = addDProp("decisionNoticedVote", "Отослано уведомление", LogicalClass.instance, vote);
        dateDecisionNoticedVote = addDProp("dateDecisionNoticedVote", "Дата отсылки уведомления", DateClass.instance, vote);

        decisionNoticedProject = addJProp("decisionNoticedProject", "Отослано уведомление", decisionNoticedVote, voteLastProject, 1);
        dateDecisionNoticedProject = addJProp("dateDecisionNoticedProject", true, "Дата отсылки уведомления", dateDecisionNoticedVote, voteLastProject, 1);

        acceptedNoticedProject = addJProp("acceptedNoticedProject", true, "Отослано уведомление о соответствии", baseLM.and1, acceptedProject, 1, decisionNoticedProject, 1);
        rejectedNoticedProject = addJProp("rejectedNoticedProject", true, "Отослано уведомление о несоответствии", baseLM.and1, rejectedProject, 1, decisionNoticedProject, 1);

        acceptedNoticedStatusProject = addJProp("acceptedNoticedStatusProject", true, "Отослано уведомление о соответствии (предв. экспертиза)", baseLM.and1, acceptedNoticedProject, 1, isStatusProject, 1);
        acceptedNoticedPreliminaryProject = addJProp("acceptedNoticedPreliminaryProject", true, "Отослано уведомление о соответствии (статус)", baseLM.andNot1, acceptedNoticedProject, 1, isStatusProject, 1);

        projectOriginalDocsCheck = addDProp("projectOriginalDocsCheck", "Проект (ИД)", project, originalDocsCheck);
        resultOriginalDocsCheck = addDProp("resultOriginalDocsCheck", "Проверка оригиналов документов", originalDocsCheckResult, originalDocsCheck);

        dateSubmitOriginalDocsCheck = addJProp(baseGroup, "dateSubmitOriginalDocsCheck", "Дата подачи документов", baseLM.and1, baseLM.date, 1, is(originalDocsCheck), 1);
        dateTimeOriginalDocsCheck = addTCProp(Time.DATETIME, "dateTimeOriginalDocsCheck", true, "Дата/время проверки документов", resultOriginalDocsCheck);

        commentOriginalDocsCheck = addDProp("commentOriginalDocsCheck", "Комментарий", TextClass.instance, originalDocsCheck);
        nameResultOriginalDocsCheck = addJProp("nameResultOriginalDocsCheck", "Проверка оригиналов документов", baseLM.name, resultOriginalDocsCheck, 1);

        dateOriginalDocsCheck = addJProp("dateOriginalDocsCheck", "Дата отправки уведомления", baseLM.toDate, dateTimeOriginalDocsCheck, 1);
        overdueDateOriginalDocsCheck = addJProp("overdueDateOriginalDocsCheck", "Дата просрочки подачи оригиналов документов", baseLM.sumDate, addJProp(baseLM.jumpWorkdays, defaultCountry, dateOriginalDocsCheck, 1, addCProp(IntegerClass.instance, 1)), 1, addJProp(baseLM.subtractDate, overduePeriod, addCProp(IntegerClass.instance, 1)));

        dateFirstSubmitOriginalDocsProject = addMGProp("dateFirstSubmitOriginalDocsProject", true, "Дата первой подачи документов", true, baseLM.date, projectOriginalDocsCheck, 1);

        maxOriginalDocsCheckProjectProps = addMGProp((AbstractGroup) null, new String[]{"maxDateOriginalDocsCheckProject", "currentOCProject"}, new String[]{"Дата посл. проверки документов", "Посл. проверка документов"}, 1,
                dateTimeOriginalDocsCheck, 1, projectOriginalDocsCheck, 1);
        LCP currentDateOriginalDocsCheckProject = maxOriginalDocsCheckProjectProps[0];
        executeOriginalDocsCheckProject = maxOriginalDocsCheckProjectProps[1];

        dateExecuteOriginalDocsCheckProject = addJProp("dateExecuteOriginalDocsCheckProject", true, "Дата действующей проверки оригиналов документов", dateOriginalDocsCheck, executeOriginalDocsCheckProject, 1);
        resultExecuteOriginalDocsCheckProject = addJProp("resultExecuteOriginalDocsCheckProject", true, "Проверка", resultOriginalDocsCheck, executeOriginalDocsCheckProject, 1);

//        setNegativeResultOriginalDocsCheck = addJProp(originalDoscCheckGroup, true, "Подан неполный пакет документов", addAAProp(originalDocsCheck, resultOriginalDocsCheck), addCProp(originalDocsCheckResult, "notCompleteOriginalDocsPacket"));
//        setPositiveResultOriginalDocsCheck = addJProp(originalDoscCheckGroup, true, "Подан полный пакет документов", addAAProp(originalDocsCheck, resultOriginalDocsCheck), addCProp(originalDocsCheckResult, "completeOriginalDocsPacket"));
        setNegativeResultOriginalDocsCheck = addSetPropertyAProp(originalDoscCheckGroup, "Подан неполный пакет документов", resultOriginalDocsCheck, 1, addCProp(originalDocsCheckResult, "notCompleteOriginalDocsPacket"));
        setPositiveResultOriginalDocsCheck = addSetPropertyAProp(originalDoscCheckGroup, "Подан полный пакет документов", resultOriginalDocsCheck, 1, addCProp(originalDocsCheckResult, "completeOriginalDocsPacket"));

        negativeOriginalDocsCheckProject = addJProp("negativeOriginalDocsCheckProject", true, "Не полный пакет документов", baseLM.equals2, resultExecuteOriginalDocsCheckProject, 1, addCProp(originalDocsCheckResult, "notCompleteOriginalDocsPacket"));
        positiveOriginalDocsCheckProject = addJProp("positiveOriginalDocsCheckProject", true, "Полный пакет документов", baseLM.equals2, resultExecuteOriginalDocsCheckProject, 1, addCProp(originalDocsCheckResult, "completeOriginalDocsPacket"));
        overdueOriginalDocsCheckProject = addJProp("overdueOriginalDocsCheckProject", true, "Пакет оригиналов документов не пополнен в срок", baseLM.greater2, baseLM.currentDate, addJProp(overdueDateOriginalDocsCheck, executeOriginalDocsCheckProject, 1), 1);

        prevOriginalDocsCheck = addOProp("prevOriginalDocsCheck", "Пред. проверка", PartitionType.PREVIOUS, object(originalDocsCheck), true, true, 1, projectOriginalDocsCheck, 1, object(originalDocsCheck), 1);
        datePrevOriginalDocsCheck = addJProp("datePrevOriginalDocsCheck", "Дата пред. проверки", dateOriginalDocsCheck, prevOriginalDocsCheck, 1);

        sentForSignatureProject = addDProp(registerGroup, "sentForSignatureProject", "Решение передано на подпись", LogicalClass.instance, project);
        dateSentForSignatureProject = addDCProp(registerGroup, "dateSentForSignatureProject", "Дата передачи на подпись", true, baseLM.currentDate, sentForSignatureProject, 1);

        signedProject = addDProp(registerGroup, "signedProject", "Решение подписано", LogicalClass.instance, project);
        dateSignedProject = addDCProp(registerGroup, "dateSignedProject", "Дата подписания", true, baseLM.currentDate, signedProject, 1);

        sentToFinDepProject = addDProp(registerGroup, "sentToFinDepProject", "Документы переданы в Финансовый департамент", LogicalClass.instance, project);
        dateSentToFinDepProject = addDCProp(registerGroup, "dateSentToFinDepProject", "Дата передачи в Финансовый департамент", true, baseLM.currentDate, sentToFinDepProject, 1);

        submittedToRegisterProject = addDProp(registerGroup, "submittedToRegisterProject", "Внесен в реестр участников", LogicalClass.instance, project);
        dateSubmittedToRegisterProject = addDCProp(registerGroup, "dateSubmittedToRegisterProject", "Дата внесения в реестр участников", true, baseLM.currentDate, submittedToRegisterProject, 1);

        preparedCertificateProject = addDProp(registerGroup, "preparedCertificateProject", "Подготовлено свидетельство участника", LogicalClass.instance, project);
        datePreparedCertificateProject = addDCProp(registerGroup, "datePreparedCertificateProject", "Дата подготовки свидетельства участника", true, baseLM.currentDate, preparedCertificateProject, 1);

        certifiedProject = addDProp(registerGroup, "certifiedProject", "Выдано свидетельство участника", LogicalClass.instance, project);
        dateCertifiedProject = addDCProp(registerGroup, "dateCertifiedProject", "Дата выдачи свидетельства участника", true, baseLM.currentDate, certifiedProject, 1);
        quantitySubDefaultVoteProject = addSUProp("quantitySubDefaultVoteProject", true, "Вместо заседаний", Union.OVERRIDE, quantityDefaultVoteProject, quantityVoteProject);

        hasPreliminaryVoteProject = addJProp("hasPreliminaryVoteProject", "Подавался на предв. экспертизу", baseLM.and1, is(project), 1, quantityPreliminaryVoteProject, 1);
        isPreliminaryStatusProject = addSUProp("isPreliminaryStatusProject", "На предв. экспертизу", Union.OVERRIDE, hasPreliminaryVoteProject, isPreliminaryProject);

        isPreliminaryAndStatusProject = addJProp("isPreliminaryAndStatusProject", "Прошла предварительную экспертизу", baseLM.and1, isStatusProject, 1, hasPreliminaryVoteProject, 1);

        legalCheckStatusProject = addCaseUProp(idGroup, "legalCheckStatusProject", true, "Статус (юридич. пров.) (ИД)",
                positiveLegalResultProject, 1, addCProp(projectStatus, "positiveLCResult", project), 1,
                overdueLegalCheckProject, 1, addCProp(projectStatus, "overdueLC", project), 1,
                negativeLegalResultStatusProject, 1, addCProp(projectStatus, "negativeLCStatusResult", project), 1,
                negativeLegalResultPreliminaryProject, 1, addCProp(projectStatus, "negativeLCPreliminaryResult", project), 1,
                addCProp(projectStatus, "unknown", project), 1);

        formalCheckStatusProject = addCaseUProp(idGroup, "formalCheckStatusProject", true, "Статус (предв. эксп.) (ИД)",
                overdueFormalControlProject, 1, addCProp(projectStatus, "overdueFC", project), 1,
                isPreliminaryNotEnoughDocumentProject, 1, addCProp(projectStatus, "notEnoughDocsForPreliminary", project), 1,
                isStatusNotEnoughDocumentProject, 1, addCProp(projectStatus, "notEnoughDocsForStatus", project), 1,
                noListOfExpertsProject, 1, addCProp(projectStatus, "noExperts", project), 1,
                notSuitableClusterProject, 1, addCProp(projectStatus, "noCluster", project), 1,
                repeatedProject, 1, addCProp(projectStatus, "repeated", project), 1,
                positiveFormalResultProject, 1, addCProp(projectStatus, "positiveFCResult", project), 1,
                addCProp(projectStatus, "unknown", project), 1);

        certifiedStatusProject = addCaseUProp(idGroup, "certifiedStatusProject", true, "Статус (оформлен) (ИД)",
                certifiedProject, 1, addCProp(projectStatus, "certified", project), 1,
                preparedCertificateProject, 1, addCProp(projectStatus, "preparedCertificate", project), 1,
                submittedToRegisterProject, 1, addCProp(projectStatus, "submittedToRegister", project), 1,
                sentToFinDepProject, 1, addCProp(projectStatus, "sentToFinDep", project), 1,
                signedProject, 1, addCProp(projectStatus, "signed", project), 1,
                sentForSignatureProject, 1, addCProp(projectStatus, "sentForSignature", project), 1,
                positiveOriginalDocsCheckProject, 1, addCProp(projectStatus, "appliedOriginalDocs", project), 1,
                overdueOriginalDocsCheckProject, 1, addCProp(projectStatus, "overdueOriginalDocs", project), 1,
                negativeOriginalDocsCheckProject, 1, addCProp(projectStatus, "notEnoughOriginalDocs", project), 1,
                resultExecuteLegalCheckProject, 1, legalCheckStatusProject, 1,
                resultExecuteFormalControlProject, 1, formalCheckStatusProject, 1,
                isPreliminaryAndStatusProject, 1, addCProp(projectStatus, "registered", project), 1,
                addCProp(projectStatus, "sentStatusAccepted", project), 1);

        valuedStatusProject = addCaseUProp(idGroup, "valuedStatusProject", true, "Статус (оценен) (ИД)",
                acceptedNoticedStatusProject, 1, addCProp(projectStatus, "sentStatusAccepted", project), 1,
                acceptedNoticedPreliminaryProject, 1, addCProp(projectStatus, "sentPreliminaryAccepted", project), 1,
                rejectedNoticedProject, 1, addCProp(projectStatus, "sentRejected", project), 1,
                acceptedDecisionProject, 1, addCProp(projectStatus, "accepted", project), 1,
                rejectedDecisionProject, 1, addCProp(projectStatus, "rejected", project), 1,
                addCProp(projectStatus, "issuedVoteDocs", project), 1);

        foresightCheckStatusProject = addCaseUProp(idGroup, "foresightCheckProject", true, "Статус (проверка на форсайты) (ИД)",
                positiveResultForesightCheckProject, 1, addCProp(projectStatus, "positiveFSResult", project), 1,
                negativeResultForesightCheckProject, 1, addCProp(projectStatus, "noCluster", project), 1,
                addCProp(projectStatus, "unknown", project), 1);

        voteStatusProject = addCaseUProp(idGroup, "voteStatusProject", true, "Статус (заседание) (ИД)",
                quantityDefaultVoteProject, 1, certifiedStatusProject, 1,
                acceptedNoticedStatusProject, 1, certifiedStatusProject, 1,
                valuedProject, 1, valuedStatusProject, 1,
                voteInProgressRepeatProject, 1, addCProp(projectStatus, "inProgressRepeat", project), 1,
                voteInProgressProject, 1, addCProp(projectStatus, "inProgress", project), 1,
                needExtraVoteRepeatProject, 1, addCProp(projectStatus, "needExtraVote", project), 1,
                addCProp(projectStatus, "unknown", project), 1);

        statusProject = addCaseUProp(idGroup, "statusProject", true, "Статус (ИД)",
                withdrawnProject, 1, addCProp(projectStatus, "withdrawn", project), 1,
                quantitySubDefaultVoteProject, 1, voteStatusProject, 1,
                sentForTranslationProject, 1, addCProp(projectStatus, "needTranslation", project), 1,
                resultForesightCheckProject, 1, foresightCheckStatusProject, 1,
                resultExecuteLegalCheckProject, 1, legalCheckStatusProject, 1,
                resultExecuteFormalControlProject, 1, formalCheckStatusProject, 1,
                addCProp(projectStatus, "registered", project), 1);

        statusDataProject = addDProp("statusDataProject", "Статус", projectStatus, project);
        projectStatusProject = addSUProp(idGroup, "projectStatusProject", true, "Статус", Union.OVERRIDE, statusProject, statusDataProject);
        // пока логи не работают, поскольку лог записывается каждый раз без проверки на изменилось ли на самом деле или нет
//        logStatusProject = addLProp(projectStatusProject);
//        logNameStatusProject = addJProp(baseGroup, "logNameStatusProject", "Статус", baseLM.name, logStatusProject, 1, 2);
//
        nameStatusProject = addJProp(projectInformationGroup, "nameStatusProject", "Статус", baseLM.name, projectStatusProject, 1);

        dateProjectVote = addJProp("dateProjectVote", "Дата проекта", dateProject, projectVote, 1);
        statusProjectVote = addJProp(idGroup, "statusProjectVote", "Статус проекта (ИД)", projectStatusProject, projectVote, 1);
        nameStatusProjectVote = addJProp(baseGroup, "nameStatusProjectVote", "Статус проекта", baseLM.name, statusProjectVote, 1);

//        сейчас в сколково реально есть заявители с двумя успешными проектами
//        projectSucceededClaimer = addAGProp(idGroup, "projectSucceededClaimer", true, "Успешный проект (ИД)", acceptedProject, 1, claimerProject, 1);


        // статистика по экспертам
        quantityTotalExpert = addSGProp(expertResultGroup, "quantityTotalExpert", "Всего заседаний",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inNewExpertVote, 1, 2), 1);

        quantityDoneExpert = addSGProp(expertResultGroup, "quantityDoneExpert", "Проголосовал",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), doneNewExpertVote, 1, 2), 1);
        quantityIsR1DoneExpert = addSGProp("quantityIsR1DoneExpert", "Проголосовал R1",
                        addJProp(and(false, false), addCProp(IntegerClass.instance, 1), doneNewExpertVote, 1, 2, isR1ProjectVote, 2), 1);

        quantityIsR2DoneExpert = addSGProp("quantityIsR2DoneExpert", "Проголосовал R2",
                                addJProp(and(false, false), addCProp(IntegerClass.instance, 1), doneNewExpertVote, 1, 2, isR2ProjectVote, 2), 1);

        percentDoneExpert = addJProp(expertResultGroup, "percentDoneExpert", "Проголосовал (%)", baseLM.share, quantityDoneExpert, 1, quantityTotalExpert, 1);

        LCP quantityClusterExpert = addSGProp("quantityClusterExpert", "Соотв-ие кластеру (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inClusterNewExpertVote, 1, 2), 1);
        percentInClusterExpert = addJProp(expertResultGroup, "percentInClusterExpert", "Соотв-ие кластеру (%)", baseLM.share, quantityClusterExpert, 1, quantityIsR1DoneExpert, 1);

        LCP quantityInnovativeExpert = addSGProp("quantityInnovativeExpert", "Инновац. (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), innovativeNewExpertVote, 1, 2), 1);
        percentInnovativeExpert = addJProp(expertResultGroup, "percentInnovativeExpert", "Инновац. (%)", baseLM.share, quantityInnovativeExpert, 1, quantityIsR1DoneExpert, 1);

        LCP quantityForeignExpert = addSGProp("quantityForeignExpert", "Иностр. специалист (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), foreignNewExpertVote, 1, 2), 1);
        percentForeignExpert = addJProp(expertResultGroup, "percentForeignExpert", "Иностр. специалист (%)", baseLM.share, quantityForeignExpert, 1, quantityIsR1DoneExpert, 1);

        LCP quantityCompetitiveAdvantagesExpert = addSGProp("quantityCompetitiveAdvantagesExpert", "Конкур. преим. (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), competitiveAdvantagesCorExpertVote, 1, 2), 1);
        percentCompetitiveAdvantagesExpert = addJProp(expertResultGroup, "percentCompetitiveAdvantagesExpert", "Конкур. преим. (%)", baseLM.share, quantityCompetitiveAdvantagesExpert, 1, quantityIsR2DoneExpert, 1);

        LCP quantityCommercePotentialExpert = addSGProp("quantityCommercePotentialExpert", "Потенциал коммерц. (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), commercePotentialCorExpertVote, 1, 2), 1);
        percentCommercePotentialExpert = addJProp(expertResultGroup, "percentCommercePotentialExpert", "Потенциал коммерц. (%)", baseLM.share, quantityCommercePotentialExpert, 1, quantityIsR2DoneExpert, 1);

        LCP quantityCanBeImplementedExpert = addSGProp("quantityCanBeImplementedExpert", "Теоретически реализуем (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), canBeImplementedCorExpertVote, 1, 2), 1);
        percentCanBeImplementedExpert = addJProp(expertResultGroup, "percentCanBeImplementedExpert", "Теоретически реализуем (%)", baseLM.share, quantityCanBeImplementedExpert, 1, quantityIsR2DoneExpert, 1);

        LCP quantityHaveExpertiseExpert = addSGProp("quantityHaveExpertiseExpert", "Наличие экспертизы (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), haveExpertiseCorExpertVote, 1, 2), 1);
        percentHaveExpertiseExpert = addJProp(expertResultGroup, "percentHaveExpertiseExpert", "Наличие экспертизы (%)", baseLM.share, quantityHaveExpertiseExpert, 1, quantityIsR2DoneExpert, 1);

        LCP quantityInternationalExperienceExpert = addSGProp("quantityInternationalExperienceExpert", "Международный опыт (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), internationalExperienceCorExpertVote, 1, 2), 1);
        percentInternationalExperienceExpert = addJProp(expertResultGroup, "percentInternationalExperienceExpert", "Международный опыт (%)", baseLM.share, quantityInternationalExperienceExpert, 1, quantityIsR2DoneExpert, 1);

        LCP quantityEnoughDocumentsExpert = addSGProp("quantityEnoughDocumentsExpert", "Достаточно голосов (голоса)",
                addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), enoughDocumentsCorExpertVote, 1, 2), 1);
        percentEnoughDocumentsExpert = addJProp(expertResultGroup, "percentEnoughDocumentsExpert", "Достаточно голосов (%)", baseLM.share, quantityEnoughDocumentsExpert, 1, quantityIsR2DoneExpert, 1);

        prevDateStartVote = addOProp("prevDateStartVote", "Пред. засед. (старт)", PartitionType.PREVIOUS, dateStartVote, true, true, 2, projectVote, 1, clusterVote, 1, baseLM.date, 1);
        prevDateVote = addOProp("prevDateVote", "Пред. засед. (окончание)", PartitionType.PREVIOUS, dateEndVote, true, true, 2, projectVote, 1, clusterVote, 1, baseLM.date, 1);

        prevClusterVote = addOProp(idGroup, "prevClusterVote", "Пред. кластер. (ИД)", PartitionType.PREVIOUS, clusterVote, true, true, 1, projectVote, 1, baseLM.date, 1);
        nameNativePrevClusterVote = addJProp("nameNativePrevClusterVote", "Пред. кластер", nameNative, prevClusterVote, 1);

        numberNewExpertVote = addOProp("numberNewExpertVote", "Номер (нов.)", PartitionType.SUM, addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inNewExpertVote, 1, 2), true, true, 1, 2, 1);
        numberOldExpertVote = addOProp("numberOldExpertVote", "Номер (стар.)", PartitionType.SUM, addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), inOldExpertVote, 1, 2), true, true, 1, 2, 1);

        emailDocuments = addDProp(baseGroup, "emailDocuments", "E-mail для документов", StringClass.get(100));
        emailIO = addDProp(baseGroup, "emailIO", "E-mail инвестиционного отдела", StringClass.get(100));
        emailExperts = addDProp(baseGroup, "emailExperts", "E-mail экспертного отдела", StringClass.get(100));
        emailPresident = addDProp(baseGroup, "emailPresident", "E-mail аппарата президента Фонда", StringClass.get(100));
        emailBureauTranslation = addDProp(baseGroup, "emailBureauTranslation", "E-mail для отправкм в бюро переводов", StringClass.get(100));

        emailFondFC = addDProp(baseGroup, "emailFondFC", "E-mail для формальной проверки", StringClass.get(100));      // внести записи kzosin@corp.i-gorod.com; Esinyatkina@corp.i-gorod.com; lkiselev@corp.i-gorod.com;
        emailForesightLC = addDProp(baseGroup, "emailForesightLC", "E-mail для решения проверки на соот. форсайту", StringClass.get(100));    // внести записи EChapysheva@corp.i-gorod.com; Lmerchenko@corp.i-gorod.com; Sizotova@corp.i-gorod.com;
        emailFondTransferred = addDProp(baseGroup, "emailFondTransferred", "E-mail для решения о переводе", StringClass.get(100));          // внести записи ddudina@corp.i-gorod.com;
        emailFondStartVote = addDProp(baseGroup, "emailFondStartVote", "E-mail для уведомления о начале заседания", StringClass.get(100));  // внести записи kzosin@corp.i-gorod.com; esinyatkina@corp.i-gorod.com;

        emailLetterExpertVoteEA = addEAProp(expert, vote);
        addEARecipients(emailLetterExpertVoteEA, baseLM.email, 1);
        addEARecipients(emailLetterExpertVoteEA, MimeMessage.RecipientType.BCC, emailExperts);

        emailLetterExpertVote = addJoinAProp(baseGroup, "emailLetterExpertVote", "Письмо о заседании (e-mail)",
                emailLetterExpertVoteEA, 1, 2, addJProp(letterExpertSubjectLanguage, languageExpert, 1), 1);
        emailLetterExpertVote.setImage("email.png");
        emailLetterExpertVote.property.askConfirm = true;
        emailLetterExpertVote.setEventSetAction(this, inNewExpertVote, 1, 2);

        allowedEmailLetterExpertVote = addIfAProp(baseGroup, "Письмо о заседании (e-mail)", "Письмо о заседании", true, voteResultExpertVote, 1, 2, emailLetterExpertVote, 1, 2);
        allowedEmailLetterExpertVote.property.askConfirm = true;

        emailClaimerFromAddress = addDProp("emailClaimerFromAddress", "Адрес отправителя (для заявителей)", StringClass.get(50));
        emailClaimerVoteEA = addEAProp(emailClaimerFromAddress, emailClaimerFromAddress, vote);

        claimerEmailVote = addJProp("claimerEmailVote", "E-mail (заявителя)", baseLM.email, claimerVote, 1);
        addEARecipients(emailClaimerVoteEA, claimerEmailVote, 1);

        emailClaimerHeaderVote = addJProp("emailClaimerHeaderVote", "Заголовок уведомления заявителю", baseLM.string2SP, addCProp(StringClass.get(2000), "Уведомление."), nameNativeClaimerVote, 1);
        emailClaimerVote = addJoinAProp(actionGroup, "emailClaimerVote", "Письмо о рассмотрении", emailClaimerVoteEA, 1, emailClaimerHeaderVote, 1);
        emailClaimerVote.property.askConfirm = true;

        emailClaimerVote.setEventSetAction(this, openedVote, 1);

        emailFondVoteEA = addEAProp(emailIO, vote);
        addEARecipients(emailFondVoteEA, emailFondStartVote);
        emailFondHeaderVote = addJProp("emailFondHeaderVote", "Заголовок уведомления в фонд", baseLM.string2SP, addCProp(StringClass.get(2000), "Уведомление."), nameNativeClaimerVote, 1);
        emailFondVote = addJoinAProp(actionGroup, "emailFondVote", "Письмо о рассмотрении", emailFondVoteEA, 1, emailFondHeaderVote, 1);
        emailFondVote.property.askConfirm = true;
        emailFondVote.setEventSetAction(this, openedVote, 1);

        emailNoticeRejectedVoteEA = addEAProp(emailClaimerFromAddress, emailClaimerFromAddress, vote);
        addEARecipients(emailNoticeRejectedVoteEA, claimerEmailVote, 1);

        setCurrentDateDecisionNoticedVote = addSetPropertyAProp(actionGroup, "setCurrentDateDecisionNoticedVote", "Установить текущую дату уведомления",
                dateDecisionNoticedVote, 1, baseLM.currentDate);

        emailNoticeRejectedVote = addIfAProp(actionGroup, "emailNoticeRejectedVote", "Письмо о несоответствии", closedRejectedVote, 1,
                addListAProp(
                        addJoinAProp(emailNoticeRejectedVoteEA, 1, emailClaimerHeaderVote, 1), 1, // отсылаем письмо
                        addSetPropertyAProp(decisionNoticedVote, 1, baseLM.vtrue), 1, // пишем, что отослано
                        setCurrentDateDecisionNoticedVote, 1 // записываем дату отсылки
                ), 1);
        emailNoticeRejectedVote.setImage("email.png");
        emailNoticeRejectedVote.property.askConfirm = true;

        emailNoticeAcceptedStatusVoteEA = addEAProp(emailClaimerFromAddress, emailClaimerFromAddress, vote);
        addEARecipients(emailNoticeAcceptedStatusVoteEA, claimerEmailVote, 1);

        emailNoticeAcceptedStatusVote = addIfAProp(actionGroup, "emailNoticeAcceptedStatusVote", "Письмо о соответствии (статус участника)", closedAcceptedStatusVote, 1,
                addListAProp(
                        addJoinAProp(emailNoticeAcceptedStatusVoteEA, 1, emailClaimerHeaderVote, 1), 1,
                        addSetPropertyAProp(decisionNoticedVote, 1, baseLM.vtrue), 1,
                        setCurrentDateDecisionNoticedVote, 1
                ), 1);
        emailNoticeAcceptedStatusVote.setImage("email.png");
        emailNoticeAcceptedStatusVote.property.askConfirm = true;

        emailNoticeAcceptedPreliminaryVoteEA = addEAProp(emailClaimerFromAddress, emailClaimerFromAddress, vote);
        addEARecipients(emailNoticeAcceptedPreliminaryVoteEA, claimerEmailVote, 1);

//        emailNoticeAcceptedPreliminaryVote = addJProp(actionGroup, true, "emailNoticeAcceptedPreliminaryVote", "Письмо о соответствии (предварительная экспертиза)", and(false, false), addJProp(emailNoticeAcceptedPreliminaryVoteEA, 1, emailClaimerHeaderVote, 1), 1,);

        emailNoticeAcceptedPreliminaryVote = addIfAProp(actionGroup, "emailNoticeAcceptedPreliminaryVote", "Письмо о соответствии (предварительная экспертиза)",
                addJProp(baseLM.and1, closedAcceptedPreliminaryVote, 1, fileDecisionVote, 1), 1,
                addListAProp(
                        addJoinAProp(emailNoticeAcceptedPreliminaryVoteEA, 1, emailClaimerHeaderVote, 1), 1,
                        addSetPropertyAProp(decisionNoticedVote, 1, baseLM.vtrue), 1,
                        setCurrentDateDecisionNoticedVote, 1
                ), 1);
        emailNoticeAcceptedPreliminaryVote.setImage("email.png");
        emailNoticeAcceptedPreliminaryVote.property.askConfirm = true;

        emailStartVoteEA = addEAProp(vote);
        addEARecipients(emailStartVoteEA, emailDocuments);

        add2Strings = addSFProp("(CAST(prm1 as text))||(CAST(prm2 as text))", StringClass.get(2000), 2);
        emailClaimerNameVote = addJProp(add2Strings, 2, nameNativeClaimerVote, 1);

        emailStartHeaderVote = addJProp("emailStartHeaderVote", "Заголовок созыва заседания", emailClaimerNameVote, 1, addCProp(StringClass.get(2000), "Созыв заседания - "));
        emailStartVote = addJoinAProp(baseGroup, "emailStartVote", "Созыв заседания (e-mail)", emailStartVoteEA, 1, emailStartHeaderVote, 1);
        emailStartVote.property.askConfirm = true;
//        emailStartVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), openedVote, 1);

        emailProtocolVoteEA = addEAProp(vote);
        addEARecipients(emailProtocolVoteEA, emailDocuments);

        emailProtocolHeaderVote = addJProp("emailProtocolHeaderVote", "Заголовок протокола заседания", emailClaimerNameVote, 1, addCProp(StringClass.get(2000), "Протокол заседания - "));
        emailProtocolVote = addJoinAProp(baseGroup, "emailProtocolVote", "Протокол заседания (e-mail)", emailProtocolVoteEA, 1, emailProtocolHeaderVote, 1);
        emailProtocolVote.property.askConfirm = true;
//        emailProtocolVote.setDerivedForcedChange(addCProp(ActionClass.instance, true), closedVote, 1);

        emailClosedVoteEA = addEAProp(vote);
        addEARecipients(emailClosedVoteEA, emailDocuments);

        emailClosedHeaderVote = addJProp(emailClaimerNameVote, 1, addCProp(StringClass.get(2000), "Результаты заседания - "));
        emailClosedVote = addJoinAProp(baseGroup, "emailClosedVote", "Результаты заседания (e-mail)", emailClosedVoteEA, 1, emailClosedHeaderVote, 1);
        emailClosedVote.setImage("email.png");
        emailClosedVote.property.askConfirm = true;
        emailClosedVote.setEventSetAction(this, closedVote, 1);

        emailForesightCheckProjectEA = addEAProp("Решение проверки о соответствии форсайту", project);
        addEARecipients(emailForesightCheckProjectEA, emailIO);
        addEARecipients(emailForesightCheckProjectEA, MimeMessage.RecipientType.CC, emailForesightLC);
        emailForesightCheckProjectEA.setEventSetAction(this, resultForesightCheckProject, 1);

        emailNotificationProjectEA = addEAProp(emailIO, project);
        addEARecipients(emailNotificationProjectEA, emailFinalClusterProject, 1);
        addEARecipients(emailNotificationProjectEA, MimeMessage.RecipientType.CC, emailIO);
        emailNotificationHeaderProject = addJProp(add2Strings, addCProp(StringClass.get(2000), "Проверка проекта - "), nameNativeProject, 1);
        emailNotificationProject = addJoinAProp(baseGroup, "emailNotificationProject", "Проверка на соответствие направлению деятельности (e-mail)", emailNotificationProjectEA, 1, emailNotificationHeaderProject, 1);
        emailNotificationProject.setImage("email.png");
        emailNotificationProject.property.askConfirm = true;
        emailNotificationProject.setEventSetAction(this, needForesightCheckProject, 1);

        emailForesightClaimerProjectEA = addEAProp(emailIO, project);
        addEARecipients(emailForesightClaimerProjectEA, emailClaimerProject, 1);
        addEARecipients(emailForesightClaimerProjectEA, MimeMessage.RecipientType.CC, emailIO);
        emailForesightClaimerHeaderProject = addJProp("emailForesightClaimerHeaderProject", "Заголовок уведомления" ,add2Strings, addCProp(StringClass.get(2000), "Уведомление. "), nameNativeClaimerProject, 1);
        emailForesightClaimerProject = addJoinAProp(baseGroup, "emailForesightClaimerProject", "Уведомление о результатах проверки на форсайты (e-mail)", emailForesightClaimerProjectEA, 1, emailForesightClaimerHeaderProject, 1);
        emailForesightClaimerProject.setImage("email.png");
        emailForesightClaimerProject.property.askConfirm = true;
//        emailForesightClaimerProject.setDerivedForcedChange(addCProp(ActionClass.instance, true), negativeResultForesightCheckProject, 1);

        emailBureauTrProjectEA = addEAProp(emailIO, project);
        addEARecipients(emailBureauTrProjectEA, emailBureauTranslation);
        addEARecipients(emailBureauTrProjectEA, MimeMessage.RecipientType.CC, emailIO);
        emailBureauTrProject = addJoinAProp(baseGroup, "emailBureauTrProject", "Письмо в бюро переводов (e-mail)", emailBureauTrProjectEA, 1, addCProp(StringClass.get(2000), "Заявка на перевод. "));
        emailBureauTrProject.setImage("email.png");
        emailBureauTrProject.property.askConfirm = true;
        emailBureauTrProject.setEventSetAction(this, sentForTranslationProject, 1);

        resultNeedVoteProject = addDProp("resultNeedVoteProject", "Отослано уведомление", LogicalClass.instance, project);
        dateResultNeedVoteProject = addDProp("dateResultNeedVoteProject", "Дата отсылки уведомления", DateClass.instance, project);
        setCurrentDateResultNeedVoteProject = addSetPropertyAProp(actionGroup, "setCurrentDateResultNeedVoteProject", "Установить текущую дату уведомления", dateResultNeedVoteProject, 1, baseLM.currentDate);
        emailNeedVoteProjectEA = addEAProp(emailIO, project);
        addEARecipients(emailNeedVoteProjectEA, emailPresident);
        addEARecipients(emailNeedVoteProjectEA, MimeMessage.RecipientType.CC, emailIO);
//        emailNeedVoteProject = addJProp(baseGroup, true, "emailNeedVoteProject", "Письмо в фонд о необходимости заседания (e-mail)", emailNeedVoteProjectEA, 1, addCProp(StringClass.get(2000), "Созвать заседание!"));
        emailNeedVoteProject = addIfAProp(actionGroup, "emailNeedVoteProject", "Письмо в фонд о необходимости заседания (e-mail)", needVoteProject, 1,
                        addListAProp(
                                addSetPropertyAProp(resultNeedVoteProject, 1, baseLM.vtrue), 1, // пишем, что отослано
                                setCurrentDateResultNeedVoteProject, 1, // записываем дату отсылки
                                addJoinAProp(emailNeedVoteProjectEA, 1, addJProp(add2Strings, addCProp(StringClass.get(2000), "Запустить заседание по -  "), nameNativeClaimerProject, 1), 1), 1 // отсылаем письмо
                        ), 1);
        emailNeedVoteProject.setImage("email.png");
        emailNeedVoteProject.property.askConfirm = true;
        emailNeedVoteProject.setEventSetAction(this, needVoteProject, 1);
        dateOverdueResultNeedVoteProject = addJProp("dateOverdueResultNeedVoteProject", "Дата до которой д.б. созвано заседание", baseLM.jumpWorkdays, defaultCountry, dateResultNeedVoteProject, 1, addCProp(IntegerClass.instance, 1));

        emailTransferredProjectEA = addEAProp(emailIO, project);
        addEARecipients(emailTransferredProjectEA, emailIO);
        addEARecipients(emailTransferredProjectEA, MimeMessage.RecipientType.CC, emailFondTransferred);
        emailTransferredHeaderProject = addJProp("emailTransferredHeaderProject", "Заголовок уведомления" ,add2Strings, addCProp(StringClass.get(2000), "Уведомление. "), nameNativeClaimerProject, 1);
        emailTransferredProject = addJoinAProp(baseGroup, "emailTransferredProject", "Уведомление о прохождении перевода (e-mail)", emailTransferredProjectEA, 1, emailTransferredHeaderProject, 1);
        emailTransferredProject.setImage("email.png");
        emailTransferredProject.property.askConfirm = true;
        emailTransferredProject.setEventSetAction(this, transferredProject, 1);

        emailClaimerFormalControlEA = addEAProp(emailClaimerFromAddress, emailClaimerFromAddress, formalControl);

        claimerFormalControl = addJProp(idGroup, "claimerFormalControl", "Заявитель (ИД)", claimerProject, projectFormalControl, 1);
        claimerEmailFormalControl = addJProp("claimerEmailFormalControl", "E-mail (заявителя)", baseLM.email, claimerFormalControl, 1);

        nameNativeJoinClaimerFormalControl = addJProp(baseGroup, "nameNativeJoinClaimerFormalControl", nameNativeClaimer, claimerFormalControl, 1);
        nameForeignJoinClaimerFormalControl = addJProp(baseGroup, "nameForeignJoinClaimerFormalControl", nameForeign, claimerFormalControl, 1);

        nameNativeUnionManagerFormalControl = addJProp(baseGroup, "nameNativeUnionManagerFormalControl", "Заявитель", nameNativeUnionManagerProject, projectFormalControl, 1);
        
        nameNativeClaimerFormalControl = addIfElseUProp(baseGroup, "nameNativeClaimerFormalControl", "Заявитель", addJProp(nameNativeUnionManagerProject, projectFormalControl, 1), nameNativeJoinClaimerFormalControl, isPreliminaryFormalControl, 1);
        nameNativeClaimerFormalControl.setMinimumWidth(10);
        nameNativeClaimerFormalControl.setPreferredWidth(50);


        setCurrentDateResultNoticedFormalControl = addSetPropertyAProp(actionGroup, "setCurrentDateResultNoticedFormalControl", "Установить текущую дату уведомления",
                dateResultNoticedFormalControl, 1, baseLM.currentDate);

        addEARecipients(emailClaimerFormalControlEA, claimerEmailFormalControl, 1);

        emailClaimerHeaderFormalControl = addJProp("emailClaimerHeaderFormalControl", "Заголовок уведомления заявителю", baseLM.string2SP, addCProp(StringClass.get(2000), "Уведомление."), nameNativeClaimerFormalControl, 1);
  //      emailClaimerFormalControl = addJProp(actionGroup, true, "emailClaimerFormalControl", "Письмо о формальной экспертизе", emailClaimerFormalControlEA, 1, emailClaimerHeaderFormalControl, 1);

        needEmailClaimerFormalControl = addSUProp("needEmailClaimerFormalControl", "Требуется уведомление", Union.OVERRIDE, addJProp(baseLM.equals2, resultFormalControl, 1, addCProp(formalControlResult, "notEnoughDocuments")), addJProp(baseLM.equals2, resultFormalControl, 1, addCProp(formalControlResult, "repeatedFC")));
        emailClaimerFormalControl = addIfAProp(actionGroup, "emailClaimerFormalControl", "Письмо о формальной экспертизе", needEmailClaimerFormalControl, 1,
                addListAProp(setCurrentDateResultNoticedFormalControl, 1, // записываем дату отсылки
                        addJoinAProp(emailClaimerFormalControlEA, 1, emailClaimerHeaderFormalControl, 1), 1, // отсылаем письмо
                        addSetPropertyAProp(resultNoticedFormalControl, 1, baseLM.vtrue), 1 // пишем, что отослано
                ), 1);

        emailClaimerFormalControl.setImage("email.png");
        emailClaimerFormalControl.property.askConfirm = true;

        emailFondFormalControlEA = addEAProp(emailIO, formalControl);
        addEARecipients(emailFondFormalControlEA, emailIO);
        addEARecipients(emailFondFormalControlEA, MimeMessage.RecipientType.CC, emailFondFC);
        emailFondHeaderFormalControl = addJProp("emailFondHeaderFormalControl", "Заголовок уведомления", baseLM.string2SP, addCProp(StringClass.get(2000), "Уведомление."), nameNativeClaimerFormalControl, 1);

        emailFondFormalControl = addJoinAProp(actionGroup, "emailFondFormalControl", "Письмо о формальной экспертизе", emailFondFormalControlEA, 1, emailFondHeaderFormalControl, 1);
        emailFondFormalControl.setImage("email.png");
        emailFondFormalControl.property.askConfirm = true;
        emailFondFormalControl.setEventSetAction(this, resultFormalControl, 1);
        nameNativeProjectFormalControl = addJProp(baseGroup, "nameNativeProjectFormalControl", "Проект", nameNativeProject, projectFormalControl, 1);

        // для отсылки по юридической проверке
        emailClaimerLegalCheckEA = addEAProp(emailClaimerFromAddress, emailClaimerFromAddress, legalCheck);

        claimerLegalCheck = addJProp(idGroup, "claimerLegalCheck", "Заявитель (ИД)", claimerProject, projectLegalCheck, 1);
        claimerEmailLegalCheck = addJProp("claimerEmailLegalCheck", "E-mail (заявителя)", baseLM.email, claimerLegalCheck, 1);

        nameNativeJoinClaimerLegalCheck = addJProp(baseGroup, "nameNativeJoinClaimerLegalCheck", nameNativeClaimer, claimerLegalCheck, 1);
        nameForeignJoinClaimerLegalCheck = addJProp(baseGroup, "nameForeignJoinClaimerLegalCheck", nameForeign, claimerLegalCheck, 1);

        nameNativeClaimerLegalCheck = addIfElseUProp(baseGroup, "nameNativeClaimerLegalCheck", "Заявитель", addJProp(nameNativeUnionManagerProject, projectLegalCheck, 1), nameNativeJoinClaimerLegalCheck, isPreliminaryLegalCheck, 1);
        nameNativeClaimerLegalCheck.setMinimumWidth(10);
        nameNativeClaimerLegalCheck.setPreferredWidth(50);

        setCurrentDateResultNoticedLegalCheck = addSetPropertyAProp(actionGroup, "setCurrentDateResultNoticedLegalCheck", "Установить текущую дату уведомления",
                dateResultNoticedLegalCheck, 1, baseLM.currentDate);

        addEARecipients(emailClaimerLegalCheckEA, claimerEmailLegalCheck, 1);
        addEARecipients(emailClaimerLegalCheckEA, MimeMessage.RecipientType.BCC, emailIO);
//        addEARecipients(emailClaimerLegalCheckEA, MimeMessage.RecipientType.BCC, emailFinalClusterLegalCheck, 1);

        emailClaimerHeaderLegalCheck = addJProp("emailClaimerHeaderLegalCheck", "Заголовок уведомления заявителю", baseLM.string2SP, addCProp(StringClass.get(2000), "Уведомление."), nameNativeClaimerLegalCheck, 1);

        emailClaimerLegalCheck = addIfAProp(actionGroup, "emailClaimerLegalCheck", "Письмо о формальной экспертизе", object(legalCheck), 1,
                addListAProp(
                        setCurrentDateResultNoticedLegalCheck, 1, // записываем дату отсылки
                        addJoinAProp(emailClaimerLegalCheckEA, 1, emailClaimerHeaderLegalCheck, 1), 1, // отсылаем письмо
                        addSetPropertyAProp(resultNoticedLegalCheck, 1, baseLM.vtrue), 1 // пишем, что отослано
                ), 1);
        emailClaimerLegalCheck.setImage("email.png");
        emailClaimerLegalCheck.property.askConfirm = true;

        emailClaimerChangeLegalCheckEA = addEAProp(emailClaimerFromAddress, emailClaimerFromAddress, legalCheck);
        setCurrentDateChangeLegalCheck = addSetPropertyAProp(actionGroup, "setCurrentDateChangeLegalCheck", "Установить текущую дату уведомления",
                dateChangeLegalCheck, 1, baseLM.currentDate);

        addEARecipients(emailClaimerChangeLegalCheckEA, claimerEmailLegalCheck, 1);
        addEARecipients(emailClaimerChangeLegalCheckEA, MimeMessage.RecipientType.BCC, emailIO);

        emailClaimerChangeLegalCheck = addIfAProp(changeLegalCheckGroup, "emailClaimerChangeLegalCheck", "Письмо об изменении типа заявки", changeLegalCheck, 1,
                addListAProp(
                        setCurrentDateChangeLegalCheck, 1, // записываем дату отсылки
                        addJoinAProp(emailClaimerChangeLegalCheckEA, 1, emailClaimerHeaderLegalCheck, 1), 1, // отсылаем письмо
                        addSetPropertyAProp(noticedChangeLegalCheck, 1, baseLM.vtrue), 1 // пишем, что отослано
                ), 1);
        emailClaimerChangeLegalCheck.setImage("email.png");
        emailClaimerChangeLegalCheck.property.askConfirm = true;

        emailClaimerLegalCheckProject = addJoinAProp(actionGroup, "emailClaimerLegalCheckProject", "Письмо о формальной экспертизе", emailClaimerLegalCheck, executeLegalCheckProject, 1);
        emailClaimerLegalCheckProject.setEventSetAction(this, positiveResultForesightCheckProject, 1);

        isForeignExpert = addJProp("isForeignExpert", "Иностр.", baseLM.equals2, languageExpert, 1, addCProp(language, "english"));
        localeExpert = addJProp("localeExpert", "Locale", localeLanguage, languageExpert, 1);

        emailAuthExpertEA = addEAProp(expert);
        addEARecipients(emailAuthExpertEA, baseLM.email, 1);
        addEARecipients(emailAuthExpertEA, MimeMessage.RecipientType.BCC, emailExperts);

        emailAuthExpert = addJoinAProp(baseGroup, "emailAuthExpert", "Аутентификация эксперта (e-mail)",
                emailAuthExpertEA, 1, addJProp(authExpertSubjectLanguage, languageExpert, 1), 1);
        emailAuthExpert.setImage("email.png");
        emailAuthExpert.property.askConfirm = true;

        emailAuthProfileExpertEA = addEAProp(expert);
        addEARecipients(emailAuthProfileExpertEA, baseLM.email, 1);
        addEARecipients(emailAuthProfileExpertEA, MimeMessage.RecipientType.BCC, emailExperts);

        emailAuthProfileExpert = addJoinAProp(baseGroup, "emailAuthProfileExpert", "Уведомление о заполнении профиля (e-mail)",
                emailAuthProfileExpertEA, 1, addJProp(authProfileExpertSubjectLanguage, languageExpert, 1), 1);
        emailAuthProfileExpert.setImage("email.png");
        emailAuthProfileExpert.property.askConfirm = true;

        emailReminderProfileExpertEA = addEAProp(expert);
        addEARecipients(emailReminderProfileExpertEA, baseLM.email, 1);
        addEARecipients(emailReminderProfileExpertEA, MimeMessage.RecipientType.BCC, emailExperts);

        emailReminderProfileExpert = addJoinAProp(baseGroup, "emailReminderProfileExpert", "Напоминание о заполнении профиля (e-mail)",
                emailReminderProfileExpertEA, 1, addJProp(reminderProfileExpertSubjectLanguage, languageExpert, 1), 1);
        emailReminderProfileExpert.setImage("email.png");
        emailReminderProfileExpert.property.askConfirm = true;

//        emailAuthExpert.setDerivedChange(addCProp(ActionClass.instance, true), baseLM.userLogin, 1, baseLM.userPassword, 1);

        emailClaimerAcceptedHeaderVote = addJProp(emailClaimerNameVote, 1, addCProp(StringClass.get(2000), "Решение о соответствии - "));
        emailClaimerRejectedHeaderVote = addJProp(emailClaimerNameVote, 1, addCProp(StringClass.get(2000), "Решение о несоответствии - "));

        emailAcceptedProjectEA = addEAProp(project);
        addEARecipients(emailAcceptedProjectEA, emailDocuments);
        emailClaimerAcceptedHeaderProject = addJProp(addSFProp("(CAST(prm1 as text))||(CAST(prm2 as text))", StringClass.get(2000), 2), addCProp(StringClass.get(2000), "Решение о присвоении статуса участника - "), nameNativeClaimerProject, 1);
        emailAcceptedProject = addJoinAProp(baseGroup, "emailAcceptedProject", "Решение о присвоении статуса участника (e-mail)", emailAcceptedProjectEA, 1, emailClaimerAcceptedHeaderProject, 1);
        emailAcceptedProject.setImage("email.png");
        emailAcceptedProject.property.askConfirm = true;
        emailAcceptedProject.setEventSetAction(this, acceptedProject, 1);

        emailToExpert = addJProp("emailToExpert", "Эксперт по e-mail", addJProp(baseLM.and1, 1, is(expert), 1), baseLM.emailToObject, 1);

        ConcreteCustomClass country = (ConcreteCustomClass) BL.getModule("Country").getClassByName("country");

        dateAgreementExpert = addDProp("dateAgreementExpert", "Дата соглашения с экспертом", DateClass.instance, expert);
        vone = addCProp("1", IntegerClass.instance, 1);
        claimerProjectVote = addJProp("claimerProjectVote", "claimerProjectVote", claimerProject, projectVote, 1);
        nameNativeJoinClaimerProjectVote = addJProp("nameNativeJoinClaimerProjectVote", "Имя заявителя", nameNativeJoinClaimerProject, projectVote, 1);
        nameNativeJoinClaimerProjectVote.setMinimumWidth(10);
        nameNativeJoinClaimerProjectVote.setPreferredWidth(120);
        countryExpert = addDProp("countryExpert", "Страна эксперта", country, expert);
        nameCountryExpert = addJProp("nameCountryExpert", "Страна эксперта", baseLM.name, countryExpert, 1);
        nameCountryExpert.setMinimumWidth(10);
        nameCountryExpert.setPreferredWidth(20);

        caseCountry = addDProp(baseGroup, "caseCountry", "Страна в Предложном падеже", StringClass.get(40), country);
        caseCountryExpert = addJProp("caseCountryExpert", "Страна эксперта П.П.", caseCountry, countryExpert, 1);

        currencyExpert = addDProp("currencyExpert", "Валюта (ИД)", getCurrencyClass(), expert);
        nameCurrencyExpert = addJProp("nameCurrencyExpert", "Валюта договора", baseLM.name, currencyExpert, 1);
        nameCurrencyExpert.setMinimumWidth(10);
        nameCurrencyExpert.setPreferredWidth(20);

        executiveLD = addDProp("executiveLD", "Ответственный исполнитель ПД", StringClass.get(100));
        phoneExecutiveLD = addDProp("phoneExecutiveLD", "Телефон ответственного исполнителя ПД", StringClass.get(100));
        mobileExecutiveLD = addDProp("mobileExecutiveLD", "Телефон (моб.) ответственного исполнителя ПД", StringClass.get(100));
        executiveIS = addDProp("executiveIS", "Ответственный исполнитель ИС", StringClass.get(100));

        residency = addDProp(baseGroup, "residency", "Признак резидентства", LogicalClass.instance, country);
        residencyCountryExpert = addJProp("residencyCountryExpert", "Резидент", residency, countryExpert, 1);
        moneyQuantityDoneExpertMonthYear = addJProp("moneyQuantityDoneExpertMonthYear", "ЗП эксперта за мес.", addJProp(baseLM.round, 1, addCProp(IntegerClass.instance, 0)), addJProp(baseLM.multiply, quantityDoneExpertMonthYear, 1, 2, 3, rateExpert), 1, 2, 3);

        baseCurrency = addDProp(baseGroup, "baseCurrency", "Базовая валюта", LogicalClass.instance, getCurrencyClass());
        baseCurrencyExpert = addJProp("baseCurrencyExpert", "Базовая валюта", baseCurrency, currencyExpert, 1);

        englCountry = addDProp(baseGroup, "englCountry", "Страна на английском", StringClass.get(40), country);
        englCountryExpert = addJProp("englCountryExpert", "Страна эксперта англ", englCountry, countryExpert, 1);
        englCurrency = addDProp(baseGroup, "englCurrency", "Валюта на английском", StringClass.get(40), getCurrencyClass());
        englCurrencyExpert = addJProp("englCurrencyExpert", "Валюта эксперта англ", englCurrency, currencyExpert, 1);
        pluralCurrency = addDProp(baseGroup, "pluralCurrency", "Валюта множ.числ", StringClass.get(40), getCurrencyClass());
        pluralCurrencyExpert = addJProp("pluralCurrencyExpert", "Валюта эксперта мн.ч.", pluralCurrency, currencyExpert, 1);

        emailLetterExpertMonthYearEA = addEAProp("Акт выполненных работ", IntegerClass.instance, IntegerClass.instance);
        addEARecipients(emailLetterExpertMonthYearEA, emailForCertificates);

        emailLetterCertificatesExpertMonthYear = addJoinAProp("emailLetterCertificatesExpertMonthYear", "Отправка актов", emailLetterExpertMonthYearEA, monthInPreviousDate, yearInPreviousDate);
        emailLetterCertificatesExpertMonthYear.setImage("email.png");
        emailLetterCertificatesExpertMonthYear.property.askConfirm = true;

        // так конечно не совсем правильно, если поменяется дата с 01 числа одного месяца на 01 число другого месяца
        emailLetterCertificatesExpertMonthYear.setEventSetAction(this, isNewMonth);

        isPreliminaryAndStatusProjectFormalControl = addJProp(true, "isPreliminaryAndStatusProjectFormalControl", "На предв. экспертизу и статус", isPreliminaryAndStatusProject, projectFormalControl, 1);
        isPreliminaryAndStatusProjectLegalCheck = addJProp(true, "isPreliminaryAndStatusProjectLegalCheck", "На предв. экспертизу и статус", isPreliminaryAndStatusProject, projectLegalCheck, 1);

        statusProjectFormalControl = addJProp("statusProjectFormalControl", "Статус (ИД)", statusProject, projectFormalControl, 1);
        sidStatusProjectFormalControl = addJProp("sidStatusProjectFormalControl", "Статус", baseLM.classSID, statusProjectFormalControl, 1);

        statusProjectLegalCheck = addJProp("statusProjectLegalCheck", "Статус (ИД)", statusProject, projectLegalCheck, 1);
        sidStatusProjectLegalCheck = addJProp("sidStatusProjectLegalCheck", "Статус", baseLM.classSID, statusProjectLegalCheck, 1);

        // заявки

        projectActionApplication = addCUProp(idGroup, "projectActionApplication", true, "Тип заявки (ИД)",
                addCProp(projectAction, "preliminary", applicationPreliminary),
                addCProp(projectAction, "status", applicationStatus));
        nameProjectActionApplication = addJProp(baseGroup, "nameProjectActionApplication", "Тип заявки", baseLM.name, projectActionApplication, 1);

        projectApplication = addDProp(idGroup, "projectApplication", "Проект (ИД)", project, application);

        quantityStatusVoteApplication = addJProp("quantityStatusVoteApplication", true, "Количество заседаний", quantityStatusVoteProject, projectApplication, 1);

        projectApplicationPreliminary = addJProp(idGroup, "projectApplicationPreliminary", "Проект (ИД)", baseLM.and1, projectApplication, 1, is(applicationPreliminary), 1);
        projectApplicationStatus = addJProp(idGroup, "projectApplicationStatus", "Проект (ИД)", baseLM.and1, projectApplication, 1, is(applicationStatus), 1);

        isPreliminaryApplication = addJProp(isPreliminaryStatusProject, projectApplication, 1);
        isStatusApplication = addJProp(isStatusProject, projectApplication, 1);

        isPreliminaryAndStatusApplication = addJProp("isPreliminaryAndStatusApplication", "Заявка была и на статус, и на предв", isPreliminaryAndStatusProject, projectApplication, 1);

        preliminaryApplicationProject = addAGProp(idGroup, false, "preliminaryApplicationProject", false, "Заяка на предварительную экспертизу", applicationPreliminary, projectApplication);
        follows(isPreliminaryStatusProject, preliminaryApplicationProject, 1);
        follows(is(applicationPreliminary), isPreliminaryApplication, 1);

        statusApplicationProject = addAGProp(idGroup, false, "statusApplicationProject", false, "Заяка на статус участника", applicationStatus, projectApplication);
        follows(isStatusProject, statusApplicationProject, 1);
        follows(is(applicationStatus), isStatusApplication, 1);

        dateApplicationPreliminary = addJProp("dateApplicationPreliminary", "Дата (предв. экспертиза)", dateProject, projectApplicationPreliminary, 1);
        dateApplicationStatus = addJProp("dateApplicationStatus", "Дата (статус участника)", dateStatusProject, projectApplicationStatus, 1);

        inactiveApplication = addJProp(baseGroup, "inactiveApplication", "Не акт.", inactiveProject, projectApplication, 1);

        dateApplication = addCUProp(baseGroup, "dateApplication", true, "Дата", dateApplicationPreliminary, dateApplicationStatus);

        nameNativeProjectApplication = addJProp(baseGroup, "nameNativeProjectApplication", "Проект", nameNativeProject, projectApplication, 1);

        nameNativeClaimerApplicationPreliminary = addJProp("nameNativeClaimerApplicationPreliminary", "Заявитель (предв. экспертиза)", nameNativeUnionManagerProject, projectApplicationPreliminary, 1);
        nameNativeClaimerApplicationStatus = addJProp("nameNativeClaimerApplicationStatus", "Заявитель (статус участника)", nameNativeJoinClaimerProject, projectApplicationStatus, 1);

        nameNativeClaimerApplication = addCUProp(baseGroup, "nameNativeClaimerApplication", "Заявитель", nameNativeClaimerApplicationPreliminary, nameNativeClaimerApplicationStatus);
        nameNativeClaimerApplication.setMinimumWidth(10);
        nameNativeClaimerApplication.setPreferredWidth(50);

        emailClaimerApplication = addJProp("emailClaimerApplication", "E-mail заявителя", emailClaimerProject, projectApplication, 1);

        statusJoinApplication = addJProp(idGroup, "statusJoinApplication", "Статус заявки (ИД)", projectStatusProject, projectApplication, 1);
        isPreliminaryAfterStatusApplication = addJProp("isPreliminaryAfterStatusApplication", "Заявка на предв. экспертизу (после подачи на статус)", baseLM.and1, is(applicationPreliminary), 1, isPreliminaryAndStatusApplication, 1);

        statusApplication = addIfElseUProp(idGroup, "statusApplication", "Статус заявки (ИД)", addCProp(projectStatus, "applyStatus", application), statusJoinApplication, isPreliminaryAfterStatusApplication, 1);
        nameStatusApplication = addJProp(baseGroup, "nameStatusApplication", "Статус заявки", baseLM.name, statusApplication, 1);
        officialNameStatusApplication = addJProp(baseGroup, "officialNameStatusApplication", "Статус заявки (по регламенту)", oficialNameProjectStatus, statusApplication, 1);

        langApplication = addJProp(baseGroup, "langApplication", "Язык", langProject, projectApplication, 1);
        nameNativeShortAggregateClusterApplication = addJProp(baseGroup, "nameNativeShortAggregateClusterApplication", "Кластеры", nameNativeShortAggregateClusterProject, projectApplication, 1);

        finalClusterApplication = addJProp(idGroup, "finalClusterApplication", true, "Последний кластер (ИД)", finalClusterProject, projectApplication, 1);
        nameFinalClusterApplication = addJProp(baseGroup, "nameFinalClusterApplication", "Последний кластер", baseLM.name, finalClusterApplication, 1);
        nameNativeShortFinalClusterApplication = addJProp(baseGroup, "nameNativeShortFinalClusterApplication", "Тек. кластер (сокр.)", nameNativeShort, finalClusterApplication, 1);

        inTestApplication = addJProp("inTestApplication", "Ненужный", inTestCluster, finalClusterApplication, 1);
        inActTestApplication =  addJProp("inActTestApplication", "Активные", and(true, true), object(application), 1, inTestApplication, 1,  inactiveApplication, 1);
        inActTestApplicationDate =  addJProp("inActTestApplicationDate", baseLM.and1, inActTestApplication, 1, is(DateClass.instance), 2);
        needFormalCheckStatusProject = addJProp("needFormalCheckStatusProject", and(true, true, true), addCProp(LogicalClass.instance, true, project), 1, positiveFormalResultProject, 1, overdueFormalControlProject, 1, withdrawnProject, 1);

        inApplicationCluster = addJProp("inApplicationCluster", "Вкл.", inProjectCluster, projectApplication, 1, 2);
        nativeSubstantiationApplicationCluster = addJProp("nativeSubstantiationApplicationCluster", "Обоснование выбора кластера", nativeSubstantiationProjectCluster, projectApplication, 1, 2);
        foreignSubstantiationApplicationCluster = addJProp("foreignSubstantiationApplicationCluster", "Обоснование выбора кластера", foreignSubstantiationProjectCluster, projectApplication, 1, 2);
        inApplicationForesight = addJProp("inApplicationForesight", "Вкл.", inProjectForesight, projectApplication, 1, 2);
        isR2Application = addJProp("isR2Application", "Заявка R2", isR2Project, projectApplication, 1);
        isR1Application = addJProp("isR1Application", "Заявка R2", isR1Project, projectApplication, 1);
        nameRegulationsApplication = addJProp(baseGroup, "nameRegulationsApplication", "Регламент заявки", nameRegulationsProject, projectApplication, 1);
        nameRegulationsApplication.setFixedCharWidth(2);

        openApplicationProjectActionApplication = addJoinAProp("openApplicationProjectActionApplication", "Открыть анкету", openApplicationProjectAction, projectApplication, 1);
        openCompleteApplicationProjectActionApplication = addJoinAProp("openCompleteApplicationProjectActionApplication", "Открыть анкету (полную)", openCompleteApplicationProjectAction, projectApplication, 1);
        exportProjectDocumentsActionApplication = addJoinAProp("exportProjectDocumentsActionApplication", "Экспортировать документы", exportProjectDocumentsAction, projectApplication, 1);

        needLegalCheckStatusProject = addJProp("needLegalCheckStatusProject", and(true, true, true, true),
                addCProp(LogicalClass.instance, true, project), 1,
                negativeFormalResultProject, 1,
                resultExecuteLegalCheckProject, 1,
                overdueLegalCheckProject, 1,
                withdrawnProject, 1);

        addPositiveLCResultProject = addJoinAProp(legalCheckResultGroup, "Прошла юридическую проверку",
                addAAProp(legalCheck, projectLegalCheck, resultLegalCheck), 1, addCProp(legalCheckResult, "positiveLegalCheckResult"));
        setPositiveLCResultApplyProject = addIfAProp(actionGroup, "setPositiveLCResultApplyProject", "Прошла юридическую проверку", needLegalCheckStatusProject, 1,
                addListAProp(addPositiveLCResultProject, 1, baseLM.apply, baseLM.cancel), 1);
        setPositiveLCResultApplyProject.property.askConfirm = true;
        setPositiveLCResultApplyProject.setImage("sign_tick.png");

        // статистика заявок

        daysSubmitApplicationDate = addJProp("daysSubmitApplicationDate", "Кол-во дней заявки", baseLM.subtractInteger, dateApplication, 1, object(DateClass.instance), 2);

        weekSubmitApplicationDate = addJProp("weekSubmitApplicationDate", "Неделя заявки", baseLM.divideIntegerNeg, daysSubmitApplicationDate, 1, 2, addCProp(IntegerClass.instance, 7));

        oneApplicationDateDate = addJProp(and(false, false, false), addCProp(IntegerClass.instance, 1, application, DateClass.instance, DateClass.instance), 1, 2, 3,
                                            addJProp(baseLM.groeq2, dateApplication, 1, 2), 1, 2,
                                            addJProp(baseLM.lsoeq2, dateApplication, 1, 2), 1, 3,
                                            inActTestApplication, 1);

        applicationsSubmitDateDate = addSGProp("applicationsSubmitDateDate", "Всего поступивших заявок", oneApplicationDateDate, 2, 3);

        applicationsSubmitProjectActionClusterDateDate = addSGProp("applicationsSubmitProjectActionClusterDateDate", "Всего поступивших заявок",
                oneApplicationDateDate, projectActionApplication, 1, finalClusterApplication, 1, 2, 3);

        applicationsSubmitClusterDateDate = addSGProp("applicationsSubmitClusterDateDate", "Всего поступивших заявок",
                oneApplicationDateDate, finalClusterApplication, 1, 2, 3);

        applicationsSubmitProjectActionDateDate = addSGProp("applicationsSubmitProjectActionDateDate", "Всего поступивших заявок",
                oneApplicationDateDate, projectActionApplication, 1, 2, 3);

        quantityClusterApplication = addJProp("quantityClusterApplication", "К-во кластеров", quantityClusterProject, projectApplication, 1);
        nonClusterApplication = addJProp("nonClusterApplication", "Заявка без кластера", baseLM.andNot1, is(application), 1, quantityClusterApplication, 1);
        nonClusterApplicationsSubmitDateDate = addSGProp("nonClusterApplicationsSubmitDateDate", "Итого не указано", addJProp(baseLM.andNot1, oneApplicationDateDate, 1, 2, 3, quantityClusterApplication, 1), 2, 3);
        nonClusterApplicationsProjectActionSubmitDateDate = addSGProp("nonClusterApplicationsProjectActionSubmitDateDate", "Итого не указано", addJProp(baseLM.andNot1, oneApplicationDateDate, 1, 2, 3, quantityClusterApplication, 1), projectActionApplication, 1, 2, 3);

        applicationsSubmitStatusApplicationDateDate = addSGProp("applicationsSubmitStatusApplicationDateDate", "Всего поступивших заявок",
                oneApplicationDateDate, statusApplication, 1, 2, 3);
        applicationsSubmitStatusApplicationClusterDateDate = addSGProp("applicationsSubmitStatusApplicationClusterDateDate", "Всего поступивших заявок",
                oneApplicationDateDate, statusApplication, 1, finalClusterApplication,1, 2, 3);

        nonClusterApplicationsStatusAplicationSubmitDateDate = addSGProp("nonClusterApplicationsStatusAplicationSubmitDateDate", "Итого не указано", addJProp(baseLM.and1, oneApplicationDateDate, 1, 2, 3, nonClusterApplication, 1), statusApplication, 1, 2, 3);

        averageWeekApplSubmitDateDate = addJProp("averageApplSubmitDateDate", "Среднее кол-во заявок в неделю",
                baseLM.divideIntegerRnd, applicationsSubmitDateDate, 1, 2, baseLM.weeksNullInclBetweenDates, 1, 2);

        applicationsSubmitDateWeek = addSGProp("applicationsSubmitDateWeek", "Кол-во поступивших заявок",              // с даты
                addJProp(and(false, false), addCProp(IntegerClass.instance, 1, application, DateClass.instance), 1, 2,
                        addJProp(baseLM.groeq2, dateApplication, 1, 2), 1, 2,
                        inActTestApplication, 1),
                        2, weekSubmitApplicationDate, 1, 2);

        // подсчет дат нахождения заявки на стороне заявителя
        daysClaimerFirstOriginalDocsProject = addJProp("daysClaimerFirstOriginalDocumentsProject", "Кол-во дней подачи документов в бумажном виде (первый раз)", baseLM.and1,
                addIfElseUProp(addJProp(baseLM.subtractInteger, dateFirstSubmitOriginalDocsProject, 1, datePositiveStatusLegalCheckProject, 1),
                        addJProp(baseLM.subtractInteger, dateFirstSubmitOriginalDocsProject, 1, dateDecisionNoticedProject, 1),
                        hasPreliminaryVoteProject, 1), 1, isStatusProject, 1);

        isClaimerFirstOriginalDocsProjectDate = addJProp("isClaimerFirstOriginalDocsProjectDate", "На стороне заявителя подача документов в бумажном виде (первый раз)", baseLM.and1,
                addIfElseUProp(addJProp(baseLM.betweenDates, 2, datePositiveStatusLegalCheckProject, 1, dateFirstSubmitOriginalDocsProject, 1),
                        addJProp(baseLM.betweenDates, 2, dateDecisionNoticedProject, 1, dateFirstSubmitOriginalDocsProject, 1),
                        hasPreliminaryVoteProject, 1), 1, 2, isStatusProject, 1);

        daysClaimerExtraOriginalDocsProject = addSGProp("daysClaimerExtraOriginalDocsProject", true, "Кол-во дней подачи документов в бумажном виде (повторно)",
                addJProp(baseLM.subtractInteger, dateSubmitOriginalDocsCheck, 1, datePrevOriginalDocsCheck, 1), projectOriginalDocsCheck, 1);

        isClaimerExtraOriginalDocsProjectDate = addMGProp("isClaimerExtraOriginalDocsProjectDate", "На стороне заявителя подача документов в бумажном виде (повторно)",
                addJProp(baseLM.betweenDates, 2, datePrevOriginalDocsCheck, 1, dateSubmitOriginalDocsCheck, 1), projectOriginalDocsCheck, 1, 2);

        daysClaimerOriginalDocsProject = addSUProp("daysClaimerOriginalDocsProject", "Кол-во дней подачи документов в бумажном виде", Union.SUM, daysClaimerFirstOriginalDocsProject, daysClaimerExtraOriginalDocsProject);
        isClaimerOriginalDocsProjectDate = addSUProp("isClaimerOriginalDocsProjectDate", "На стороне заявителя подача документов в бумажном виде", Union.OVERRIDE, isClaimerFirstOriginalDocsProjectDate, isClaimerExtraOriginalDocsProjectDate);

        daysClaimerPreliminaryFormalControlProject = addSGProp("daysClaimerPreliminaryFormalControlProject", true, "Кол-во дней формальной экспертизы (предв. экспертиза)",
                addJProp(baseLM.and1, addJProp(baseLM.subtractInteger, dateSubmitFormalControl, 1, datePrevFormalControl, 1), 1, isPreliminaryFormalControl, 1),
                        projectFormalControl, 1);

        isClaimerPreliminaryFormalControlProjectDate = addMGProp("isClaimerPreliminaryFormalControlProjectDate", "На стороне заявителя формальной экспертизы (предв. экспертиза)",
                addJProp(baseLM.and1, addJProp(baseLM.betweenDates, 2, datePrevFormalControl, 1, dateSubmitFormalControl, 1), 1, 2, isPreliminaryFormalControl, 1),
                        projectFormalControl, 1, 2);

        daysClaimerStatusFormalControlProject = addSGProp("daysClaimerStatusFormalControlProject", true, "Кол-во дней формальной экспертизы (статус)",
                addJProp(baseLM.and1, addJProp(baseLM.subtractInteger, dateSubmitFormalControl, 1, datePrevFormalControl, 1), 1, isStatusFormalControl, 1),
                        projectFormalControl, 1);

        isClaimerStatusFormalControlProjectDate = addMGProp("isClaimerStatusFormalControlProjectDate", "На стороне заявителя формальной экспертизы (статус)",
                addJProp(baseLM.and1, addJProp(baseLM.betweenDates, 2, datePrevFormalControl, 1, dateSubmitFormalControl, 1), 1, 2, isStatusFormalControl, 1),
                        projectFormalControl, 1, 2);

        daysClaimerPreliminaryLegalCheckProject = addSGProp("daysClaimerPreliminaryLegalCheckProject", true, "Кол-во дней юридической проверки (предв. экспертиза)",
                addJProp(baseLM.and1, addJProp(baseLM.subtractInteger, dateSubmitLegalCheck, 1, datePrevLegalCheck, 1), 1, isPreliminaryLegalCheck, 1),
                        projectLegalCheck, 1);

        isClaimerPreliminaryLegalCheckProjectDate = addMGProp("isClaimerPreliminaryLegalCheckProjectDate", "На стороне заявителя юридической проверки (предв. экспертиза)",
                addJProp(baseLM.and1, addJProp(baseLM.betweenDates, 2, datePrevLegalCheck, 1, dateSubmitLegalCheck, 1), 1, 2, isPreliminaryLegalCheck, 1),
                        projectLegalCheck, 1, 2);

        daysClaimerStatusLegalCheckProject = addSGProp("daysClaimerStatusLegalCheckProject", true, "Кол-во дней юридической проверки (статус)",
                addJProp(baseLM.and1, addJProp(baseLM.subtractInteger, dateSubmitLegalCheck, 1, datePrevLegalCheck, 1), 1, isStatusLegalCheck, 1),
                        projectLegalCheck, 1);

        isClaimerStatusLegalCheckProjectDate = addMGProp("isClaimerStatusLegalCheckProjectDate", "На стороне заявителя юридической проверки (статус)",
                addJProp(baseLM.and1, addJProp(baseLM.betweenDates, 2, datePrevLegalCheck, 1, dateSubmitLegalCheck, 1), 1, 2, isStatusLegalCheck, 1),
                        projectLegalCheck, 1, 2);

        daysClaimerPreliminaryProject = addSUProp("daysClaimerPreliminaryProject", "Кол-во дней на стороне заявителя (предв. экспертиза)", Union.SUM,
                daysClaimerPreliminaryFormalControlProject, daysClaimerPreliminaryLegalCheckProject);

        isClaimerPreliminaryProjectDate = addSUProp("isClaimerPreliminaryProjectDate", "На стороне заявителя (предв. экспертиза)", Union.OVERRIDE,
                isClaimerPreliminaryFormalControlProjectDate, isClaimerPreliminaryLegalCheckProjectDate);

        daysClaimerStatusProject = addSUProp("daysClaimerStatusProject", "Кол-во дней на стороне заявителя (статус)", Union.SUM,
                daysClaimerStatusFormalControlProject, daysClaimerStatusLegalCheckProject, daysClaimerOriginalDocsProject);

        isClaimerStatusProjectDate = addSUProp("isClaimerStatusProjectDate", "На стороне заявителя (статус)", Union.OVERRIDE,
                isClaimerStatusFormalControlProjectDate, isClaimerStatusLegalCheckProjectDate, isClaimerOriginalDocsProjectDate);

        daysClaimerApplicationPreliminary = addJProp("daysClaimerApplicationPreliminary", "Кол-во дней на стороне заявителя", daysClaimerPreliminaryProject, projectApplicationPreliminary, 1);
        daysClaimerApplicationStatus = addJProp("daysClaimerApplicationStatus", "Кол-во дней на стороне заявителя", daysClaimerStatusProject, projectApplicationStatus, 1);

        isClaimerApplicationPreliminaryDate = addJProp("isClaimerApplicationPreliminaryDate", "На стороне заявителя", isClaimerPreliminaryProjectDate, projectApplicationPreliminary, 1, 2);
        isClaimerApplicationStatusDate = addJProp("isClaimerApplicationStatusDate", "На стороне заявителя", isClaimerStatusProjectDate, projectApplicationStatus, 1, 2);

        daysClaimerApplication = addCUProp("daysClaimerApplication", "Кол-во дней на стороне заявителя", daysClaimerApplicationPreliminary, daysClaimerApplicationStatus);
        isClaimerApplicationDate = addCUProp("isClaimerApplicationDate", "На стороне заявителя", isClaimerApplicationPreliminaryDate, isClaimerApplicationStatusDate);

        dateSubmittedToRegisterApplication = addJProp("dateSubmittedToRegisterApplication", "дата внесения в реестр участников", dateSubmittedToRegisterProject, projectApplicationStatus, 1);

        daysCommonApplication =  addJProp("daysCommonApplication", "Общее к-во дней заявки", baseLM.subtractInteger, dateSubmittedToRegisterApplication, 1, dateApplicationStatus, 1);
        daysStatusApplication =  addDUProp("daysStatusApplication", "Кол-во дней рассмотрения заявки на статус", daysCommonApplication, daysClaimerApplication);

        // даты для статусов-проектов
        dateWithdrawnStatusProject = addJProp("dateWithdrawnStatusProject", true, "Дата статуса", baseLM.and1, dateWithdrawnProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "withdrawn")), 1);
        dateRegisteredStatusProject = addJProp("dateRegisteredStatusProject", true, "Дата статуса", baseLM.and1, dateProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "registered")), 1);
        dateNoClusterStatusProject = addJProp("dateNoClusterStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteFormalControlProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "noCluster")), 1);
        dateNoExpertsStatusProject = addJProp("dateNoExpertsStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteFormalControlProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "noExperts")), 1);
        dateNotEnoughDocsForStatusStatusProject = addJProp("dateNotEnoughDocsForStatusStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteFormalControlProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "notEnoughDocsForStatus")), 1);
        dateNotEnoughDocsForPreliminaryStatusProject = addJProp("dateNotEnoughDocsForPreliminaryStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteFormalControlProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "notEnoughDocsForPreliminary")), 1);
        dateRepeatedStatusProject = addJProp("dateRepeatedStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteFormalControlProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "repeated")), 1);
        datePositiveFCResultStatusProject = addJProp("datePositiveFCResultStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteFormalControlProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "positiveFCResult")), 1);
        dateOverdueFCStatusProject = addJProp("dateOverdueFCStatusProject", true, "Дата статуса", baseLM.and1, addJProp(overdueDateFormalControl, executeFormalControlProject, 1), 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "overdueFC")), 1);

        dateNegativeLCPreliminaryResultStatusProject = addJProp("dateNegativeLCPreliminaryResultStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteLegalCheckProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "negativeLCPreliminaryResult")), 1);
        dateNegativeLCStatusResultStatusProject = addJProp("dateNegativeLCStatusResultStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteLegalCheckProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "negativeLCStatusResult")), 1);
        datePositiveLCResultStatusProject = addJProp("datePositiveLCResultStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteLegalCheckProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "positiveLCResult")), 1);
        dateOverdueLCStatusProject = addJProp("dateOverdueLCStatusProject", true, "Дата статуса", baseLM.and1, addJProp(overdueDateLegalCheck, executeLegalCheckProject, 1), 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "overdueLC")), 1);

        datePositiveFSResultProject = addJProp("datePositiveFSResultProject", true, "Дата статуса", baseLM.and1, dateResultForesightCheckProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "positiveFSResult")), 1);

        dateNotificationPeriodProject = addJProp("dateNotificationPeriodProject", "Дата до которой д.б. проверен", baseLM.jumpWorkdays, defaultCountry, datePositiveLegalResultProject, 1, addCProp(IntegerClass.instance, 2));

        dateNeedTranslationStatusProject = addJProp("dateNeedTranslationStatusProject", true, "Дата статуса", baseLM.and1, dateSentForTranslationProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "needTranslation")), 1);

        dateInProgressStatusProject = addJProp("dateInProgressStatusProject", true, "Дата статуса", baseLM.and1, dateStartVoteLastProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "inProgress")), 1);
        dateInProgressRepeatStatusProject = addJProp("dateInProgressRepeatStatusProject", true, "Дата статуса", baseLM.and1, dateStartVoteLastProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "inProgressRepeat")), 1);
        dateIssuedVoteDocsStatusProject = addJProp("dateIssuedVoteDocsStatusProject", true, "Дата статуса", baseLM.and1, dateEndVoteLastProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "issuedVoteDocs")), 1);
        dateNeedExtraVoteStatusProject = addJProp("dateNeedExtraVoteStatusProject", true, "Дата статуса", baseLM.and1, dateEndVoteLastProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "needExtraVote")), 1);
        dateRejectedStatusProject = addJProp("dateRejectedStatusProject", true, "Дата статуса", baseLM.and1, dateEndVoteLastProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "rejected")), 1);
        dateAcceptedStatusProject = addJProp("dateAcceptedStatusProject", true, "Дата статуса", baseLM.and1, dateEndVoteLastProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "accepted")), 1);

        dateSentPreliminaryAcceptedStatusProject = addJProp("dateSentPreliminaryAcceptedStatusProject", true, "Дата статуса", baseLM.and1, dateDecisionNoticedProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "sentPreliminaryAccepted")), 1);
        dateSentStatusAcceptedStatusProject = addJProp("dateSentStatusAcceptedStatusProject", true, "Дата статуса", baseLM.and1, dateDecisionNoticedProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "sentStatusAccepted")), 1);
        dateSentRejectedStatusProject = addJProp("dateSentRejectedStatusProject", true, "Дата статуса", baseLM.and1, dateDecisionNoticedProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "sentRejected")), 1);

        dateAppliedOriginalDocsStatusProject = addJProp("dateAppliedOriginalDocsStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteOriginalDocsCheckProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "appliedOriginalDocs")), 1);
        dateNotEnoughOriginalDocsStatusProject = addJProp("dateNotEnoughOriginalDocsStatusProject", true, "Дата статуса", baseLM.and1, dateExecuteOriginalDocsCheckProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "notEnoughOriginalDocs")), 1);
        dateOverdueOriginalDocsStatusProject = addJProp("dateOverdueOriginalDocsStatusProject", true, "Дата статуса", baseLM.and1, addJProp(overdueDateOriginalDocsCheck, executeOriginalDocsCheckProject, 1), 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "overdueOriginalDocs")), 1);

        dateApplyStatusStatusProject = addJProp("datApplyStatusStatusProject", true, "Дата статуса", baseLM.and1, dateStatusProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "applyStatus")), 1);

        dateSentForSignatureStatusProject = addJProp("dateSentForSignatureStatusProject", true, "Дата статуса", baseLM.and1, dateSentForSignatureProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "sentForSignature")), 1);
        dateSignedStatusProject = addJProp("dateSignedStatusProject", true, "Дата статуса", baseLM.and1, dateSignedProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "signed")), 1);
        dateSentToFinDepStatusProject = addJProp("dateSentToFinDepStatusProject", true, "Дата статуса", baseLM.and1, dateSentToFinDepProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "sentToFinDep")), 1);
        dateSubmittedToRegisterStatusProject = addJProp("dateSubmittedToRegisterStatusProject", true, "Дата статуса", baseLM.and1, dateSubmittedToRegisterProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "submittedToRegister")), 1);
        datePreparedCertificateStatusProject = addJProp("datePreparedCertificateStatusProject", true, "Дата статуса", baseLM.and1, datePreparedCertificateProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "preparedCertificate")), 1);
        dateCertifiedStatusProject = addJProp("dateCertifiedStatusProject", true, "Дата статуса", baseLM.and1, dateCertifiedProject, 1, addJProp(baseLM.equals2, statusProject, 1, addCProp(projectStatus, "certified")), 1);

        dateInStatusProject = addXSUProp(baseGroup,"dateInStatusProject", true, "Дата статуса", dateRegisteredStatusProject, dateNoClusterStatusProject, dateNoExpertsStatusProject, dateNotEnoughDocsForStatusStatusProject,
                dateNotEnoughDocsForPreliminaryStatusProject, dateRepeatedStatusProject, datePositiveFCResultStatusProject, dateOverdueFCStatusProject, dateNegativeLCPreliminaryResultStatusProject,
                dateNegativeLCStatusResultStatusProject, datePositiveLCResultStatusProject, dateOverdueLCStatusProject, datePositiveFSResultProject, dateNeedTranslationStatusProject, dateInProgressStatusProject,
                dateInProgressRepeatStatusProject, dateIssuedVoteDocsStatusProject, dateNeedExtraVoteStatusProject, dateRejectedStatusProject, dateAcceptedStatusProject,
                dateSentPreliminaryAcceptedStatusProject, dateSentStatusAcceptedStatusProject, dateSentRejectedStatusProject,
                dateAppliedOriginalDocsStatusProject, dateNotEnoughOriginalDocsStatusProject, dateOverdueOriginalDocsStatusProject, dateApplyStatusStatusProject,
                dateSentForSignatureStatusProject, dateSignedStatusProject, dateSentToFinDepStatusProject, dateSubmittedToRegisterStatusProject, datePreparedCertificateStatusProject, dateCertifiedStatusProject, dateWithdrawnStatusProject);

        normalPeriodStatus = addDProp(baseGroup, "normalPeriodStatus", "Нормативный срок в статусе", IntegerClass.instance, projectStatus);
        normalPeriodStatusProject = addJProp(baseGroup, true, "normalPeriodStatusProject", "Нормативный срок в статусе", normalPeriodStatus, statusProject, 1);
        normalPeriodStatusProject.setFixedCharWidth(2);

        isFinalProjectStatus = addDProp(baseGroup, "isFinalProjectStatus", "Завершающий статус", LogicalClass.instance, projectStatus);

        typeProjectStatusProjectStatus = addDProp(idGroup, "typeProjectStatusProjectStatus", "Тип статуса (ИД)", typeProjectStatus, projectStatus);
        nameTypeProjectStatusProjectStatus = addJProp(baseGroup, "nameTypeProjectStatusProjectStatus", "Тип статуса", baseLM.name, typeProjectStatusProjectStatus, 1);
        typeProjectStatusApplication = addJProp("typeProjectStatusApplication", "Тип статуса заявки (ИД)", typeProjectStatusProjectStatus, statusApplication, 1);

        isWorkDaysNormalPeriodStatus = addDProp(baseGroup, "isWorkDaysNormalPeriodStatus", "В рабочих днях", LogicalClass.instance, projectStatus);
        overdueDateStatusProject = addIfElseUProp(baseGroup, "overdueDateStatusProject", true, "Дата просрочки статуса",
                addJProp(baseLM.jumpWorkdays, defaultCountry, dateInStatusProject, 1, normalPeriodStatusProject, 1),
                addJProp(baseLM.sumDate, dateInStatusProject, 1, normalPeriodStatusProject, 1), addJProp(isWorkDaysNormalPeriodStatus, statusProject, 1), 1);
        quantityDaysToOverdueDateStatusProject = addJProp("quantityDaysToOverdueDateStatusProject", true, "Количество дней до просрочки", baseLM.subtractInteger, overdueDateStatusProject, 1, baseLM.currentDate);
        quantityDaysToOverdueDateStatusProject.setFixedCharWidth(4);

        dateInStatusApplication = addIfElseUProp(idGroup, "dateInStatusApplication", "Дата статуса", addJProp(dateDecisionNoticedProject, projectApplication, 1), addJProp(dateInStatusProject, projectApplication, 1), isPreliminaryAfterStatusApplication, 1);
        normalPeriodStatusApplication = addJProp(baseGroup, "normalPeriodStatusApplication", "Нормативный срок в статусе", normalPeriodStatus, statusApplication, 1);
        normalPeriodStatusApplication.setFixedCharWidth(2);
        overdueDateStatusApplication = addIfElseUProp(baseGroup, "overdueDateStatusApplication", "Дата просрочки статуса",
                addJProp(baseLM.jumpWorkdays, defaultCountry, dateInStatusApplication, 1, normalPeriodStatusApplication, 1),
                addJProp(baseLM.sumDate, dateInStatusApplication, 1, normalPeriodStatusApplication, 1), addJProp(isWorkDaysNormalPeriodStatus, statusApplication, 1), 1);
        quantityDaysToOverdueDateStatusApplication = addJProp("quantityDaysToOverdueDateStatusApplication", "Количество дней до просрочки", baseLM.subtractInteger, overdueDateStatusApplication, 1, baseLM.currentDate);
        quantityDaysToOverdueDateStatusApplication.setFixedCharWidth(4);

        oneApplications = addJProp("oneApplications", "К-во заявок" ,and(false,true, true),  addCProp(IntegerClass.instance, 1), is(application), 1, inactiveApplication, 1, inTestApplication, 1);
        sumSubmitApplications = addSGProp("sumSubmitApplications", "К-во заявок" , oneApplications);
        sumApplicationsStatusApplication = addSGProp("sumApplicationsStatusApplication", "Всего поступивших заявок",
                oneApplications, statusApplication, 1);
        sumApplicationsStatusApplicationCluster = addSGProp("sumApplicationsStatusApplicationCluster", "Всего поступивших заявок",
                oneApplications, statusApplication, 1, finalClusterApplication, 1);
        nonClusterApplicationsStatusApplicationCluster = addSGProp("nonClusterApplicationsStatusApplicationCluster", "Итого не указано", addJProp(baseLM.andNot1, oneApplications, 1, quantityClusterApplication, 1), statusApplication, 1, finalClusterApplication, 1);
        nonClusterApplicationsStatusApplication = addSGProp("nonClusterApplicationsStatusApplication", "Итого не указано", addJProp(baseLM.andNot1, oneApplications, 1, quantityClusterApplication, 1), statusApplication, 1);

        nonClusterApplicationsSubmit = addSGProp("nonClusterApplicationsSubmit", "Итого не указано", addJProp(baseLM.andNot1, oneApplications, 1, quantityClusterApplication, 1));
        sumApplicationsCluster = addSGProp("sumApplicationsCluster", "Всего поступивших заявок",
                oneApplications, finalClusterApplication, 1);
        sumApplicationsTypeProjectStatus = addSGProp("sumApplicationsTypeProjectStatus", "Всего поступивших заявок",
                oneApplications, typeProjectStatusApplication, 1);
        sumApplicationsTypeProjectStatusCluster = addSGProp("sumApplicationsTypeProjectStatusCluster", "Всего поступивших заявок",
                oneApplications, typeProjectStatusApplication, 1, finalClusterApplication,1);
        nonClusterApplicationsTypeProjectStatusCluster = addSGProp("nonClusterApplicationsTypeProjectStatusCluster", "Не указано", addJProp(baseLM.andNot1, oneApplications, 1, quantityClusterApplication, 1), typeProjectStatusApplication, 1, finalClusterApplication, 1);
        nonClusterApplicationsTypeProjectStatus = addSGProp("nonClusterApplicationsTypeProjectStatus", "Не указано", addJProp(baseLM.andNot1, oneApplications, 1, quantityClusterApplication, 1), typeProjectStatusApplication, 1);

        percentSumApplicationsTypeProjectStatusCluster = addJProp("percentSumApplicationsTypeProjectStatusCluster", "(%) по кластеру", baseLM.share, sumApplicationsTypeProjectStatusCluster, 1, 2, sumApplicationsCluster, 2);
        percentNonClusterApplicationsTypeProjectStatus = addJProp("percentNonClusterApplicationsTypeProjectStatus", "(%) не указано", baseLM.share, nonClusterApplicationsTypeProjectStatus, 1, nonClusterApplicationsSubmit);
        percentApplicationsTypeProjectStatus = addJProp("percentApplicationsTypeProjectStatus", "Итого (%)", baseLM.share, sumApplicationsTypeProjectStatus, 1, sumSubmitApplications);

        registerApplicationDateTo = addJProp(and(false, false, false), object(application), 1,
                 addJProp(baseLM.lsoeq2, dateSubmittedToRegisterApplication, 1, 2), 1, 2,     // заявки по  дату
                 object(DateClass.instance), 2,
                 inActTestApplication, 1);
        daysRegisterApplicationDateTo = addJProp("daysRegisterApplicationDateTo", "Дней на статус", daysStatusApplication, registerApplicationDateTo, 1, 2);
        submitDaysRegisterApplicationDateTo = addSGProp("submitDaysRegisterApplicationDateTo",  "Дней на статус", daysRegisterApplicationDateTo, 2);

        oneRegisterApplicationDateTo = addJProp("oneRegisterApplicationDateTo", addCProp(IntegerClass.instance, 1, application), registerApplicationDateTo, 1, 2);
        submitRegisterApplicationsDateTo = addSGProp("submitRegisterApplicationsDateTo", "К-во заявок всего",  oneRegisterApplicationDateTo, 2);

        averageDaysRegisterApplicationsDateTo = addJProp("averageDaysRegisterApplicationsDateTo", "Средний срок рассмотрения зявки на статус",
                baseLM.divideIntegerRnd, submitDaysRegisterApplicationDateTo, 1, submitRegisterApplicationsDateTo, 1);

        statusApplicationDateDate = addJProp(and(false, false, false, false), inActTestApplication, 1,
                                            addJProp(baseLM.groeq2, dateSubmittedToRegisterApplication, 1, 2), 1, 2,
                                            addJProp(baseLM.lsoeq2, dateSubmittedToRegisterApplication, 1, 2), 1, 3,
                                            object(DateClass.instance), 2,
                                            object(DateClass.instance), 3);

        statusApplicationsSubmitDateDate =  addSGProp("statusApplicationsSubmitDateDate", "К-во заявок", addJProp(addCProp(IntegerClass.instance, 1, application), statusApplicationDateDate, 1, 2, 3), 2, 3);
        daysStatusApplicationsSubmitDateDate = addSGProp("daysStatusApplicationsSubmitDateDate", "Дней на статус", addJProp(daysStatusApplication, statusApplicationDateDate, 1, 2, 3), 2, 3);
        averageDaysStatusApplicationsSubmitDateDate = addJProp("averageDaysStatusApplicationsSubmitDateDate", "Средний срок, на статус",baseLM.divideIntegerRnd, daysStatusApplicationsSubmitDateDate, 1, 2, statusApplicationsSubmitDateDate, 1, 2);

        daysSubmitRegisterApplicationDate = addJProp("daysSubmitRegisterApplicationDate", "Кол-во дней заявки", baseLM.subtractInteger, dateSubmittedToRegisterApplication, 1, object(DateClass.instance), 2);
        weekSubmitRegisterApplicationDate = addJProp("weekSubmitRegisterApplicationDate", "Неделя заявки", baseLM.divideIntegerNeg, daysSubmitRegisterApplicationDate, 1, 2, addCProp(IntegerClass.instance, 7));

        submitDaysRegisterApplicationDateWeek = addSGProp("submitDaysRegisterApplicationDateWeek", "Дней на статус", addJProp(daysStatusApplication, inActTestApplicationDate, 1, 2), 2, weekSubmitRegisterApplicationDate, 1, 2);
        submitRegisterApplicationsDateWeek = addSGProp("submitRegisterApplicationsDateWeek", "К-во заявок за неделю", addJProp(addCProp(IntegerClass.instance, 1, application), inActTestApplicationDate, 1, 2), 2, weekSubmitRegisterApplicationDate, 1, 2);
        risingDaysRegisterApplicationDateWeek = addOProp("risingDaysRegisterApplicationDateWeek", "Дней на статус, нарастающий", PartitionType.SUM, submitDaysRegisterApplicationDateWeek, true, true, 1, 1, 2);
        risingRegisterApplicationsDateWeek = addOProp("risingRegisterApplicationsDateWeek", "К-во заявок, нарастающий", PartitionType.SUM, submitRegisterApplicationsDateWeek, true, true, 1, 1, 2);

        averageDaysRegisterApplicationsDateWeekWeek = addJProp("averageDaysRegisterApplicationsDateWeekWeek", "Ср.срок на статус, за нед.",
                baseLM.divideIntegerRnd, submitDaysRegisterApplicationDateWeek, 1, 2, submitRegisterApplicationsDateWeek, 1, 2);
        averageDaysRegisterApplicationsDateWeek = addJProp("averageDaysRegisterApplicationsDateWeek", "Ср.срок на статус, нарастающий",
                baseLM.divideIntegerRnd, risingDaysRegisterApplicationDateWeek, 1, 2, risingRegisterApplicationsDateWeek, 1, 2);

        textConference = addDProp("textConference", "Текст", TextClass.instance, expertConference);
        inConferenceExpert = addDProp("inConferenceExpert", "Вкл", LogicalClass.instance, expertConference, expert);
        resultConferenceExpert = addDProp("resultConferenceExpert", "Результат (ИД)", expertConferenceResult, expertConference, expert);
        nameResultConferenceExpert = addJProp("Результат", baseLM.name, resultConferenceExpert, 1, 2);
        follows(resultConferenceExpert, inConferenceExpert, 1, 2);

        confirmedConferenceExpert = addJProp(baseLM.equals2, resultConferenceExpert, 1, 2, addCProp(expertConferenceResult, "confirmedConference"));
        rejectedConferenceExpert = addJProp(baseLM.equals2, resultConferenceExpert, 1, 2, addCProp(expertConferenceResult, "rejectedConference"));
        confirmedConference = addSGProp("Согласилось", addJProp(baseLM.and1, vone, confirmedConferenceExpert, 1, 2), 1);
        rejectedConference = addSGProp("Отказалось", addJProp(baseLM.and1, vone, rejectedConferenceExpert, 1, 2), 1);
        totalExperts = addSGProp("Всего", addJProp(baseLM.and1, vone, is(expert), 1));

        nameNativeShortAggregateClusterExpert = addOGProp(baseGroup, "nameNativeShortAggregateClusterExpert", false, "Кластеры", GroupType.STRING_AGG, 1, true, addJProp(baseLM.and1, nameNativeShort, 1, inClusterExpert, 1, 2), addCProp(StringClass.get(5), ";"), numberCluster, 1, 2);

        emailConferenceExpertEA = addEAProp(expertConference, expert);
        addEARecipients(emailConferenceExpertEA, baseLM.email, 2);

        emailConferenceExpert = addIfAProp(baseGroup, "emailConferenceExpert", "Участие в конф. (e-mail)", addJProp(baseLM.andNot1, inConferenceExpert, 1, 2, resultConferenceExpert, 1, 2), 1, 2, addJoinAProp(emailConferenceExpertEA, 1, 2, addCProp(StringClass.get(50), "Участие в конференции")), 1, 2);
        emailConferenceExpert.setImage("email.png");
        emailConferenceExpert.property.askConfirm = true;

        initNavigators();
    }

    @Override
    public void initIndexes() {
        addIndex(projectVote);
        addIndex(projectDocument);
    }


    FormEntity languageDocumentTypeForm;
    FormEntity globalForm;
    public ProjectFullFormEntity projectFullNative;
    public ProjectFullFormEntity projectFullForeign;
    public ProjectFullFormEntity projectFullBoth;
    public ClaimerFullFormEntity claimerFull;
    public ProjectFullR2FormEntity projectFullR2Native;
    public ProjectFullR2FormEntity projectFullR2Foreign;
    public ProjectFullR2FormEntity projectCompleteR2Native;
    public ProjectFullR2FormEntity projectCompleteR2Foreign;

    private StatusLogFormEntity logNameStatusForm;
    private LCP formLogNameStatusProject;

    private VoteProtocolFormEntity voteProtocolSimple;

    private void initNavigators() {

        StatusFormEntity statusForm = new StatusFormEntity(null, "StatusForm");
        projectStatus.setDialogForm(statusForm, statusForm.objStatus);

        ToolBarNavigatorWindow mainToolbar = new ToolBarNavigatorWindow(JToolBar.VERTICAL, "mainToolbar", "Навигатор", BorderLayout.WEST);
        mainToolbar.titleShown = false;
        mainToolbar.verticalTextPosition = SwingConstants.BOTTOM;
        mainToolbar.horizontalTextPosition = SwingConstants.CENTER;
        mainToolbar.alignmentX = JToolBar.CENTER_ALIGNMENT;

        ToolBarNavigatorWindow leftToolbar = new ToolBarNavigatorWindow(JToolBar.VERTICAL, "leftToolbar", "Список", 0, 0, 20, 60);

        baseLM.baseElement.window = mainToolbar;
        baseLM.adminElement.window = leftToolbar;

        TreeNavigatorWindow objectsWindow = new TreeNavigatorWindow("objectsWindow", "Объекты", 0, 30, 20, 40);
        objectsWindow.drawRoot = true;
        baseLM.objectElement.window = objectsWindow;

        baseLM.relevantFormsWindow.visible = false;
        baseLM.relevantClassFormsWindow.visible = false;
        baseLM.logWindow.visible = false;

//        logNameStatusForm = new StatusLogFormEntity(null, "logNameStatusForm");
//        formLogNameStatusProject = addMFAProp("История", logNameStatusForm, logNameStatusForm.objProject);
//        formLogNameStatusProject.setImage("history.png");

        projectFullNative = addFormEntity(new ProjectFullFormEntity(baseLM.objectElement, "projectFullNative", "Резюме проекта для эксперта", "rus"));
        project.setEditForm(projectFullNative, projectFullNative.objProject);
        projectFullBoth = addFormEntity(new ProjectFullFormEntity(baseLM.objectElement, "projectFullBoth", "Резюме проекта для эксперта", "both"));
        projectFullForeign = addFormEntity(new ProjectFullFormEntity(baseLM.objectElement, "projectFullForeign", "Resume project for expert", "eng"));

        projectFullR2Native = addFormEntity(new ProjectFullR2FormEntity(baseLM.objectElement, "projectFullR2Native", "Резюме проекта R2"));
        projectFullR2Foreign = addFormEntity(new ProjectFullR2FormEntity(baseLM.objectElement, "projectFullR2Foreign", "Resume project R2"));

        projectCompleteR2Native = addFormEntity(new ProjectFullR2FormEntity(baseLM.objectElement, "projectCompleteR2Native", "Полная анкета R2"));
        projectCompleteR2Foreign = addFormEntity(new ProjectFullR2FormEntity(baseLM.objectElement, "projectCompleteR2Foreign", "Full resume project R2"));

        claimerFull = addFormEntity(new ClaimerFullFormEntity(baseLM.objectElement, "claimerFull"));
        claimer.setEditForm(claimerFull, claimerFull.objClaimer);

        NavigatorElement print = addNavigatorElement(baseLM.baseElement, "print", "Печатные формы");
        print.window = leftToolbar;

        addFormEntity(new VoteStartFormEntity(print, "voteStart"));
        addFormEntity(new ExpertLetterFormEntity(print, "expertLetter"));
        addFormEntity(new VoteProtocolFormEntity(print, "voteProtocol", false));
        voteProtocolSimple = addFormEntity(new VoteProtocolFormEntity(print, "voteProtocolSimple", true));
        addFormEntity(new ExpertProtocolFormEntity(print, "expertProtocol"));
        addFormEntity(new ExpertProtocolClusterFormEntity(print, "expertProtocolCluster"));
        addFormEntity(new ExpertAuthFormEntity(print, "expertAuth"));
        addFormEntity(new ExpertAuthProfileFormEntity(print, "expertAuthProfile"));
        addFormEntity(new ExpertReminderProfileFormEntity(print, "expertReminderProfile"));
        addFormEntity(new ClusterExpertProfileFormEntity(print, "clusterExpertProfile"));
        addFormEntity(new ConferenceExpertLetterFormEntity(print, "conferenceExpertLetter"));
        addFormEntity(new ClaimerAcceptedFormEntity(print, "claimerAccepted"));
        addFormEntity(new ClaimerRejectedFormEntity(print, "claimerRejected"));
        addFormEntity(new ClaimerStatusFormEntity(print, "claimerStatus"));
        addFormEntity(new FormalControlFondFormEntity(print, "formalControlFond", "Оповещение о формальной экспертизе, фонд"));
        addFormEntity(new FormalControlClaimerFormEntity(print, "formalControlClaimer", "Уведомление о формальной экспертизе"));
        addFormEntity(new NotificationProjectFormEntity(print, "notificationProject", "Оповещение о соответствии направлению деятельности, фонд"));
        addFormEntity(new LegalCheckClaimerFormEntity(print, "legalCheckClaimer", "Уведомление о юридической проверке"));
        addFormEntity(new LegalCheckClaimerChangeFormEntity(print, "legalCheckClaimerChange", "Уведомление о юридической проверке, с изменением типа заявки"));
        addFormEntity(new ForesightAdviceFormEntity(print, "foresightAdvice", "Оповещение по форсайту, фонд"));
        addFormEntity(new ForesightClaimerProjectFormEntity(print, "foresightClaimerProject", "Уведомление об отрицательном результате проверки на форсайты"));  // непрохождении проверки
        addFormEntity(new BureauTrProjectFormEntity(print, "bureauTrProject", "Письмо в бюро о необходимости перевода"));
        addFormEntity(new TransferredProjectFormEntity(print, "transferredProject", "Оповещение о прохождении перевода, фонд"));
        addFormEntity(new NeedVoteFondProjectFormEntity(print, "needVoteFondProject", "Оповещение о необходимости заседания, фонд"));
        addFormEntity(new VoteFondFormEntity(print, "voteFond", "Оповещение о рассмотрении, фонд"));
        addFormEntity(new VoteClaimerFormEntity(print, "voteClaimer", "Уведомление о рассмотрении"));
        addFormEntity(new NoticeRejectedFormEntity(print, "noticeRejected", "Уведомление о несоответствии"));
        addFormEntity(new NoticeAcceptedStatusFormEntity(print, "noticeAcceptedStatus", "Уведомление о соответствии (статус участника)"));
        addFormEntity(new NoticeAcceptedPreliminaryFormEntity(print, "noticeAcceptedPreliminary", "Уведомление о соответствии (предварительная экспертиза)"));
        addFormEntity(new AcceptanceCertificateFormEntity(print, "acceptanceCertificate", "Акт оказанных услуг (резидент)", true));
        addFormEntity(new AcceptanceCertificateFormEntity(print, "acceptanceCertificateNonResident", "Акт оказанных услуг (нерезидент)", false));

        NavigatorElement report = addNavigatorElement(baseLM.baseElement, "report", "Отчеты");
        report.window = leftToolbar;

        addFormEntity(new ApplicationsSubmittedFormEntity(report, "applicationsSubmitted"));
        addFormEntity(new ApplicationsDynamicsFormEntity(report, "applicationsDynamics"));
        addFormEntity(new ApplicationsStatusWeekFormEntity(report, "applicationsStatusWeek"));
        addFormEntity(new ApplicationsStatusTimeFormEntity(report, "applicationsStatusTime"));
        addFormEntity(new RegisterApplicationWeekFormEntity(report, "registerApplicationWeekFormEntity"));
        addFormEntity(new ApplicationsListFormEntity(report, "applicationsList"));
        addFormEntity(new ExpertStatsFormEntity(report, "expertStats"));
        addFormEntity(new ConsultingCenterFormEntity(report, "consultingCenter"));

        addFormEntity(new ProjectFormEntity(baseLM.baseElement, "project"));
        addFormEntity(new ApplicationFormEntity(baseLM.baseElement, "application"));
        addFormEntity(new ClaimerFormEntity(baseLM.baseElement, "claimer"));
        addFormEntity(new VoteFormEntity(baseLM.baseElement, "vote", false));
        addFormEntity(new ExpertFormEntity(baseLM.baseElement, "expert"));
        addFormEntity(new VoteExpertFormEntity(baseLM.baseElement, "voteExpert", false));
        addFormEntity(new VoteExpertFormEntity(baseLM.baseElement, "voteExpertRestricted", true));
        addFormEntity(new VoteFormEntity(baseLM.baseElement, "voterestricted", true));
        addFormEntity(new LegalCheckAcceptPreliminaryFormEntity(baseLM.objectElement, "legalCheckAcceptPreliminary"));
        addFormEntity(new LegalCheckRejectFormEntity(baseLM.objectElement, "legalCheckReject"));
        addFormEntity(new LegalCheckExpertiseFormEntity(baseLM.baseElement, "legalCheckExpertise", "Юридическая проверка"));
        addFormEntity(new ForesightExpertiseApplyFormEntity(baseLM.objectElement, "foresightExpertiseApply"));
        addFormEntity(new ForesightExpertiseRejectFormEntity(baseLM.objectElement, "foresightExpertiseReject"));
        addFormEntity(new ForesightExpertiseListFormEntity(baseLM.baseElement, "foresightExpertiseList", "Соответствие кластеру", 2));
        addFormEntity(new ForesightExpertiseListFormEntity(baseLM.baseElement, "foresightExpertiseList2", "Проверка форсайтов", 1));
//        addFormEntity(new ProjectDocumentsFormEntity(baseLM.baseElement, "projectdocs"));
        addFormEntity(new ConferenceFormEntity(baseLM.baseElement, "conferences"));

        baseLM.baseElement.add(print);
        baseLM.baseElement.add(report);

        NavigatorElement summaryTables = addNavigatorElement(baseLM.baseElement, "summaryTables", "Сводные таблицы");
        summaryTables.window = leftToolbar;

        addFormEntity(new ProjectClusterFormEntity(summaryTables, "projectCluster"));
        baseLM.baseElement.add(summaryTables);

        addFormEntity(new ExpertForesightFormEntity(summaryTables, "expertForesight"));

        NavigatorElement options = addNavigatorElement(baseLM.baseElement, "options", "Настройки");
        options.window = leftToolbar;

        languageDocumentTypeForm = addFormEntity(new LanguageDocumentTypeFormEntity(options, "languageDocumentType"));
        addFormEntity(new DocumentTemplateFormEntity(options, "documentTemplate"));
        globalForm = addFormEntity(new GlobalFormEntity(options, "global"));
        addFormEntity(new ClusterUserFormEntity(options, "clusterUser"));

        addFormEntity(new ClusterForesightFormEntity(options, "clusterForesight"));
        baseLM.baseElement.add(options);
        baseLM.baseElement.add(baseLM.adminElement); // перемещаем adminElement в конец
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    public ConcreteCustomClass getCurrencyClass() {
        return (ConcreteCustomClass) BL.getModule("Currency").getClassByName("currency");
    }

    private void addDefaultHintsIncrementTable(FormEntity form) {
/*        form.addHintsIncrementTable(quantityDoneVote, quantityProjectLanguageDocumentType, notEnoughProject, acceptedVote, succeededVote, voteSucceededProjectCluster,
                voteInProgressRepeatProject, voteInProgressProject, needExtraVoteRepeatProject,
                voteValuedProjectCluster, rejectedProjectCluster, clusterAcceptedProject, currentClusterProject, finalClusterProject,
                executeFormalControlProject, dateExecuteFormalControlProject, resultExecuteFormalControlProject, overdueFormalControlProject,
                executeLegalCheckProject, dateExecuteLegalCheckProject, resultExecuteLegalCheckProject, overdueLegalCheckProject,
                needExtraVoteProject, isPreliminaryNotEnoughDocumentProject, isStatusNotEnoughDocumentProject,
                voteLastProject, dateStartVoteLastProject, dateEndVoteLastProject,
                acceptedProject, rejectedProject, acceptedDecisionProject, rejectedDecisionProject, valuedProject,
                dateDecisionNoticedProject, rejectedNoticedProject, acceptedNoticedPreliminaryProject, acceptedNoticedStatusProject,
                formalCheckStatusProject, legalCheckStatusProject, foresightCheckStatusProject, positiveResultForesightCheckProject, certifiedStatusProject, valuedStatusProject,
                executeOriginalDocsCheckProject, dateExecuteOriginalDocsCheckProject, resultExecuteOriginalDocsCheckProject,
                positiveOriginalDocsCheckProject, overdueOriginalDocsCheckProject, negativeOriginalDocsCheckProject,
                certifiedProject, preparedCertificateProject, submittedToRegisterProject, sentToFinDepProject, signedProject,
                sentForSignatureProject, certifiedStatusProject,
                quantityVoteProject, quantitySubDefaultVoteProject, voteStatusProject, statusProject, projectStatusProject,
                acceptedCompetitiveAdvantagesVote, acceptedCommercePotentialVote, acceptedCanBeImplementedVote,
                acceptedHaveExpertiseVote, acceptedInternationalExperienceVote, acceptedEnoughDocumentsVote);*/
    }

    private class ConsultingCenterFormEntity extends DateIntervalFormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objProject;

        private ConsultingCenterFormEntity(NavigatorElement parent, String sID) {
            super(baseLM, parent, sID, "Консультативный центр");

            objProject = addSingleGroupObject(4, "project", project, dateProject, nameNativeProject, nameNativeClaimerProject, isConsultingCenterCommentProject, consultingCenterCommentProject);

            addPropertyDraw(sumPositiveConsultingCenterCommentProject, objDateFrom, objDateTo);
            setForceViewType(sumPositiveConsultingCenterCommentProject, ClassViewType.PANEL);
            addPropertyDraw(sumNegativeConsultingCenterCommentProject, objDateFrom, objDateTo);
            setForceViewType(sumNegativeConsultingCenterCommentProject, ClassViewType.PANEL);
            addPropertyDraw(sumTotalConsultingCenterCommentProject, objDateFrom, objDateTo);
            setForceViewType(sumTotalConsultingCenterCommentProject, ClassViewType.PANEL);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateProject, objProject), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateProject, objProject), Compare.LESS_EQUALS, objDateTo));
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(inactiveProject, objProject))));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(isConsultingCenterQuestionProject, objProject)));

            addDefaultOrder(dateProject, true);
        }
    }

    private class ProjectFullFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private String lng;

        private ObjectEntity objProject;
        private ObjectEntity objCluster;
        private ObjectEntity objPatent;
        private ObjectEntity objAcademic;
        private ObjectEntity objNonRussianSpecialist;

        private ProjectFullFormEntity(NavigatorElement parent, String sID, String caption, String lng) {
            super(parent, sID, caption);

            this.lng = lng;

            objProject = addSingleGroupObject(1, "project", project, "Проект", projectInformationGroup, innovationGroup, projectDocumentsGroup, minutesOfMettingGroup, sourcesFundingGroup, consultingCenterGroup, equipmentGroup, projectOptionsGroup, projectStatusGroup);

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

//            addPropertyDraw(objProject, translatedToRussianProject, translatedToEnglishProject);
//
//            if ("eng".equals(lng)) {
//                getPropertyDraw(translatedToRussianProject).propertyCaption = addPropertyObject(hideTranslatedToRussianProject, objProject);
//            }
//            if ("rus".equals(lng)) {
//                getPropertyDraw(translatedToEnglishProject).propertyCaption = addPropertyObject(hideTranslatedToEnglishProject, objProject);
//            }

            objCluster = addSingleGroupObject(5, "cluster", cluster, "Кластер");
            addPropertyDraw(inProjectCluster, objProject, objCluster);
            addPropertyDraw(objCluster, nameNative, nameForeign);
            addPropertyDraw(new LCP[]{nativeSubstantiationProjectCluster, foreignSubstantiationProjectCluster}, objProject, objCluster);
            addPropertyDraw(numberCluster, objCluster);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inProjectCluster, objProject, objCluster)));

            objPatent = addSingleGroupObject(2, "patent", patent, "Патент", baseGroup);
            getPropertyDraw(ownerPatent).propertyCaption = addPropertyObject(hideOwnerPatent, objPatent);
            getPropertyDraw(nameNativeOwnerTypePatent).propertyCaption = addPropertyObject(hideNameNativeOwnerTypePatent, objPatent);
            getPropertyDraw(nameForeignOwnerTypePatent).propertyCaption = addPropertyObject(hideNameForeignOwnerTypePatent, objPatent);
            getPropertyDraw(loadFileIntentionOwnerPatent).propertyCaption = addPropertyObject(hideLoadFileIntentionOwnerPatent, objPatent);
            getPropertyDraw(openFileIntentionOwnerPatent).propertyCaption = addPropertyObject(hideOpenFileIntentionOwnerPatent, objPatent);

            getPropertyDraw(valuatorPatent).propertyCaption = addPropertyObject(hideValuatorPatent, objPatent);
            getPropertyDraw(loadFileActValuationPatent).propertyCaption = addPropertyObject(hideLoadFileActValuationPatent, objPatent);
            getPropertyDraw(openFileActValuationPatent).propertyCaption = addPropertyObject(hideOpenFileActValuationPatent, objPatent);
            addObjectActions(this, objPatent);

            objAcademic = addSingleGroupObject(3, "academic", academic, "Учёный", baseGroup);
            addObjectActions(this, objAcademic);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectAcademic, objAcademic), Compare.EQUALS, objProject));

            objNonRussianSpecialist = addSingleGroupObject(4, "nonRussianSpecialist", nonRussianSpecialist, "Иностранный специалист", baseGroup);
            addObjectActions(this, objNonRussianSpecialist);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectNonRussianSpecialist, objNonRussianSpecialist), Compare.EQUALS, objProject));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectPatent, objPatent), Compare.EQUALS, objProject));
            addProject = addMFAProp(actionGroup, "Добавить", this, new ObjectEntity[]{}, true, addPropertyObject(getFormAddObjectAction(objProject)));

            if (lng.equals("both")) {
                editR1Project = addIfAProp((AbstractGroup)null, "editR1Project", "Редактировать проект", isR1Project, 1,
                        addMFAProp("Редактировать проект", this, new ObjectEntity[]{objProject}), 1);
                editR1Project.setImage("edit.png");
            }

            if (lng.equals("rus")) {
                translateToRussianProject = addIfAProp(translateActionGroup, "Перевести на русский", needsToBeTranslatedToRussianProject, 1,
                        addMFAProp("Требуется перевод на русский", this, new ObjectEntity[]{objProject}), 1);
                translateToRussianProject.setImage("edit.png");
            }

            if (lng.equals("eng")) {
                translateToEnglishProject = addIfAProp(translateActionGroup, "Перевести на английский", needsToBeTranslatedToEnglishProject, 1,
                        addMFAProp("Требуется перевод на английский", this, new ObjectEntity[]{objProject}), 1);
                translateToEnglishProject.setImage("edit.png");
            }

            addDefaultHintsIncrementTable(this);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView row112 = design.createContainer();
            row112.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            row112.add(design.getGroupPropertyContainer(objProject.groupTo, projectOptionsGroup));
//            row112.add(design.getGroupPropertyContainer(objProject.groupTo, projectTranslationsGroup));
            row112.add(design.getGroupPropertyContainer(objProject.groupTo, projectStatusGroup));

            ContainerView col11 = design.createContainer();
            col11.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, innovationGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            col11.add(design.getGroupPropertyContainer(objProject.groupTo, innovationGroup));
            col11.add(row112);

            ContainerView row1 = design.createContainer();
            row1.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            row1.add(col11);
            design.getGroupPropertyContainer(objProject.groupTo, sourcesFundingGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, nonReturnFundingGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            row1.add(design.getGroupPropertyContainer(objProject.groupTo, sourcesFundingGroup));

            design.getGroupPropertyContainer(objProject.groupTo, sourcesFundingGroup).addAfter(
                    design.getGroupPropertyContainer(objProject.groupTo, nonReturnFundingGroup), design.get(getPropertyDraw(isNonReturnInvestmentsProject)));

            design.getGroupPropertyContainer(objProject.groupTo, projectDocumentsGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            design.getGroupPropertyContainer(objProject.groupTo, applicationFormGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, executiveSummaryGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, techDescrGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, minutesOfMettingGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, roadMapGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, resolutionIPGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;

            ContainerView descrContainer = design.createContainer("Описание проекта");
            descrContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            descrContainer.add(row1);
            descrContainer.add(design.getGroupPropertyContainer(objProject.groupTo, consultingCenterGroup));
            descrContainer.add(design.getGroupPropertyContainer(objProject.groupTo, equipmentGroup));
            descrContainer.add(design.getGroupPropertyContainer(objProject.groupTo, projectDocumentsGroup));
            descrContainer.add(design.getGroupPropertyContainer(objProject.groupTo, minutesOfMettingGroup));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objProject.groupTo));
            specContainer.add(descrContainer);
            specContainer.add(design.getGroupObjectContainer(objCluster.groupTo));
            specContainer.add(design.getGroupObjectContainer(objPatent.groupTo));
            specContainer.add(design.getGroupObjectContainer(objAcademic.groupTo));
            specContainer.add(design.getGroupObjectContainer(objNonRussianSpecialist.groupTo));
            specContainer.type = ContainerType.TABBED_PANE;

            design.getMainContainer().addBefore(design.getGroupPropertyContainer(objProject.groupTo, projectInformationGroup), specContainer);

//            if (!("both".equals(lng))) {
//                design.getMainContainer().addAfter(design.getGroupPropertyContainer(objProject.groupTo, projectTranslationsGroup), specContainer);
//            }
            design.setShowTableFirstLogical(true);

            CalcPropertyObjectEntity sidProjectProperty = addPropertyObject(addJProp(baseLM.and1, addCProp(ColorClass.instance, new Color(255, 250, 205)), sidProject, 1), objProject);
            if ("rus".equals(lng)) {
                getPropertyDraw(nameNativeManagerProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nameNativeClaimerProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nameNativeProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nameNativeFinalClusterProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nativeProblemProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nativeInnovativeProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nativeSubstantiationProjectType).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(loadFileNativeTechnicalDescriptionProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(openFileNativeTechnicalDescriptionProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(loadFileNativeSummaryProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(openFileNativeSummaryProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nativeNumberPatent).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nativeTypePatent).setPropertyBackground(sidProjectProperty);
            }

            if ("eng".equals(lng)) {
                getPropertyDraw(nameForeignManagerProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nameForeignClaimerProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nameForeignProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(nameForeignFinalClusterProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(foreignProblemProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(foreignInnovativeProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(foreignSubstantiationProjectType).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(loadFileForeignTechnicalDescriptionProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(openFileForeignTechnicalDescriptionProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(loadFileForeignSummaryProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(openFileForeignSummaryProject).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(foreignNumberPatent).setPropertyBackground(sidProjectProperty);
                getPropertyDraw(foreignTypePatent).setPropertyBackground(sidProjectProperty);
            }
            return design;
        }
    }

    private class ProjectFullR2FormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;
        private ObjectEntity objCluster;
        private ObjectEntity objResearch;
        private ObjectEntity objPublications;
        private ObjectEntity objPatent;
        private ObjectEntity objCommercialization;
        private ObjectEntity objAnalogues;
        private ObjectEntity objSpecialist;
        private ObjectEntity objObjectives;
        private ObjectEntity objMileStoneYear;
        private ObjectEntity objMileStone;
        private ObjectEntity objTypeMileStone;

        private ProjectFullR2FormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objProject = addSingleGroupObject(1, "project", project, "Проект", baseLM.objectValue, nameNativeProject, nameForeignProject, sidProject, nameNativeClaimerProject, nameForeignClaimerProject, nameNativeFinalClusterProject,
                    nameForeignFinalClusterProject, nameProjectActionProject, updateDateProject, nameStatusProject, dateStatusProject, isStatusProject, nameNativeJoinClaimerProject, nameForeignJoinClaimerProject,
                    nativeProblemProject, foreignProblemProject, nativeInnovativeProject, foreignInnovativeProject, descGroup, techDescrGroup, minutesOfMettingGroup, writtenConsentGroup, claimerInformationGroup);

            addPropertyDraw(objProject, problemGroup, analoguesGroup, commercializationGroup, historyGroup, projectmissionGroup, nativeResultsProject, foreignResultsProject);
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);
            addPropertyDraw(baseLM.webHost, objProject.groupTo);
            getPropertyDraw(baseLM.objectValue, objProject).setSID("project");

            objResearch = addSingleGroupObject(2, "research", research, "Исследования");
            addPropertyDraw(objResearch, problemGroup);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectResearch, objResearch), Compare.EQUALS, objProject));
            addObjectActions(this, objResearch);

            objPublications = addSingleGroupObject(3, "publications", publications, "Публикации кадров");
            addPropertyDraw(objPublications, publicationsGroup);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectPublications, objPublications), Compare.EQUALS, objProject));
            addObjectActions(this, objPublications);

            objCluster = addSingleGroupObject(4, "cluster", cluster, "Кластер");
            addPropertyDraw(inClaimerProjectCluster, objProject, objCluster);
            addPropertyDraw(objCluster, nameNative, nameForeign);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inClaimerProjectCluster, objProject, objCluster)));

            objPatent = addSingleGroupObject(5, "patent", patent, "Патент");
            addPropertyDraw(objPatent, nativeTypePatent, foreignTypePatent, nativeNumberPatent, linksPatent);
            addObjectActions(this, objPatent);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectPatent, objPatent), Compare.EQUALS, objProject));

            objCommercialization = addSingleGroupObject(6, "commercialization", commercialization, "Основные направления коммерциализации");
            addPropertyDraw(objCommercialization, commercializationGroup);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectCommercialization, objCommercialization), Compare.EQUALS, objProject));
            addObjectActions(this, objCommercialization);

            objAnalogues = addSingleGroupObject(7, "analogues", analogues, "Аналоги проекта");
            addPropertyDraw(objAnalogues, analoguesGroup);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectAnalogues, objAnalogues), Compare.EQUALS, objProject));
            addObjectActions(this, objAnalogues);

            objSpecialist = addSingleGroupObject(8, "specialist", specialist, "Члены команды", baseLM.objectValue);
            addPropertyDraw(objSpecialist, teamGroup);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectSpecialist, objSpecialist), Compare.EQUALS, objProject));
            getPropertyDraw(baseLM.objectValue, objSpecialist).setSID("specialist");
//            addPropertyDraw(baseLM.webHost, objSpecialist.groupTo);

            addObjectActions(this, objSpecialist);

            objObjectives = addSingleGroupObject(9, "objectives", objectives, "Цели проекта");
            addPropertyDraw(objObjectives, projectmissionGroup);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectObjectives, objObjectives), Compare.EQUALS, objProject));
            addObjectActions(this, objObjectives);

            objMileStoneYear = addSingleGroupObject(10, "mileStoneYear", mileStoneYear, "Год", nativeMileStoneYear);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectMileStoneYear, objMileStoneYear), Compare.EQUALS, objProject));
            addObjectActions(this, objMileStoneYear);
            addDefaultOrder(getPropertyDraw(nativeMileStoneYear, objMileStoneYear), true);

            objMileStone = addSingleGroupObject(11, "mileStone", mileStone, "Квартал", nativeMileStone, orderNumberMileStone);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectMileStone, objMileStone), Compare.EQUALS, objProject));

            addObjectActions(this, objMileStone);
            addDefaultOrder(getPropertyDraw(nativeMileStone, objMileStone), true);

            objTypeMileStone = addSingleGroupObject(12, "typeMileStone", typeMileStone, "Раздел дорожной карты", baseLM.name, nativeTypeMileStone, foreignTypeMileStone);
            PropertyDrawEntity count = addPropertyDraw(nativeDescriptionTypeMileStoneMileStoneMileStoneYear, objTypeMileStone, objMileStone, objMileStoneYear);
            count.columnGroupObjects.add(objMileStone.groupTo);
            count.propertyCaption = addPropertyObject(nativeMileStone, objMileStone);

            PropertyDrawEntity count1 = addPropertyDraw(foreignDescriptionTypeMileStoneMileStoneMileStoneYear, objTypeMileStone, objMileStone, objMileStoneYear);
            count1.columnGroupObjects.add(objMileStone.groupTo);
            count1.propertyCaption = addPropertyObject(nativeMileStone, objMileStone);

            addObjectActions(this, objTypeMileStone);
//            addPropertyDraw(objTypeMileStone, objMileStone, nativeDescriptionTypeMileStoneMileStoneMileStoneYear, foreignDescriptionTypeMileStoneMileStoneMileStoneYear);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(isR2Project, objProject)));
            if (sID.equals("projectFullR2Foreign") || sID.equals("projectCompleteR2Foreign"))
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(fillForeignProject, objProject)));
            else
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(fillNativeProject, objProject)));

            if (editR2Project == null) {
                editR2Project = addIfAProp((AbstractGroup)null, "editR2Project", "Редактировать проект", isR2Project, 1,
                        addMFAProp("Редактировать проект", this, new ObjectEntity[]{objProject}), 1);
                editR2Project.setImage("edit.png");
            }

            addDefaultHintsIncrementTable(this);

            if (sID.equals("projectFullR2Native") || sID.equals("projectFullR2Foreign"))
                addAttachEAFormNameFullyMapped(emailBureauTrProjectEA, this, AttachmentFormat.PDF,
                                               sID.equals("projectFullR2Native") ? nameNativeClaimerProject : nameForeignClaimerProject,
                                               objProject, 1);
        }

        @Override

        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView max = design.createContainer();
            max.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            max.add(design.getGroupObjectContainer(objResearch.groupTo));

            ContainerView author = design.createContainer();
            author.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            author.add(design.getGroupObjectContainer(objPublications.groupTo));

            ContainerView prob1 = design.createContainer();
            prob1.add(design.get(getPropertyDraw(nativeProblemProject, objProject)));
            prob1.add(design.get(getPropertyDraw(foreignProblemProject, objProject)));

            ContainerView problemMainInformationContainer = design.createContainer();              // проблема, решение, технология и прикладные исследования
            problemMainInformationContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            problemMainInformationContainer.add(prob1);
            problemMainInformationContainer.add(max);
            problemMainInformationContainer.add(design.getGroupPropertyContainer(objProject.groupTo, innovationGroup));
            problemMainInformationContainer.add(design.getGroupPropertyContainer(objProject.groupTo, problemGroup));
            problemMainInformationContainer.add(design.getGroupPropertyContainer(objProject.groupTo, techDescrGroup));

            ContainerView additionalDocuments = design.createContainer();

            additionalDocuments.add(design.getGroupPropertyContainer(objProject.groupTo, minutesOfMettingGroup));
            additionalDocuments.add(design.getGroupPropertyContainer(objProject.groupTo, writtenConsentGroup));
            problemMainInformationContainer.add(additionalDocuments);

            problemMainInformationContainer.add(author);

            ContainerView commercContainer = design.createContainer("Схема коммерциализации, конкурирующие решения, параметры рынка");              // схема коммерциализации и конкурирующие решения и параметры рынка
            commercContainer.add(design.getGroupObjectContainer(objCommercialization.groupTo));
            commercContainer.add(design.getGroupPropertyContainer(objProject.groupTo, commercializationGroup));
            commercContainer.add(design.getGroupObjectContainer(objAnalogues.groupTo));
            commercContainer.add(design.getGroupPropertyContainer(objProject.groupTo, analoguesGroup));


            ContainerView resul1 = design.createContainer();
            resul1.add(design.get(getPropertyDraw(nativeResultsProject, objProject)));
            resul1.add(design.get(getPropertyDraw(foreignResultsProject, objProject)));

            ContainerView exchequerContainer = design.createContainer("История возникновения, цели и задачи проект");              // ресурсы
            exchequerContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            exchequerContainer.add(design.getGroupPropertyContainer(objProject.groupTo, historyGroup));
            exchequerContainer.add(resul1);
            exchequerContainer.add(design.getGroupObjectContainer(objObjectives.groupTo));

            ContainerView descrContainer = design.createContainer("Описание проекта");
            descrContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            descrContainer.add(problemMainInformationContainer);

            ContainerView mapContainer = design.createContainer("Дорожная карта");
            mapContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            mapContainer.add(design.getGroupObjectContainer(objMileStoneYear.groupTo));
            mapContainer.add(design.getGroupObjectContainer(objMileStone.groupTo));
            mapContainer.add(design.getGroupObjectContainer(objTypeMileStone.groupTo));
            mapContainer.add(design.getGroupPropertyContainer(objProject.groupTo, projectmissionGroup));

            ContainerView dopContainer = design.createContainer("Дополнительные сведения");
            dopContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            dopContainer.add(design.getGroupPropertyContainer(objProject.groupTo, claimerInformationGroup));
            dopContainer.add(design.get(getPropertyDraw(isStatusProject)));
            dopContainer.add(design.get(getPropertyDraw(nameNativeJoinClaimerProject)));
            dopContainer.add(design.get(getPropertyDraw(nameForeignJoinClaimerProject)));

            ContainerView specContainer = design.createContainer();

            design.getGroupObjectContainer(objProject.groupTo).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            design.getGroupPropertyContainer(objProject.groupTo, projectInformationGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            design.getGroupPropertyContainer(objProject.groupTo, descGroup).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;

            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objProject.groupTo));
            specContainer.add(descrContainer);
            specContainer.add(commercContainer);
            specContainer.add(design.getGroupObjectContainer(objPatent.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSpecialist.groupTo));
            specContainer.add(design.getGroupObjectContainer(objCluster.groupTo));
            specContainer.add(exchequerContainer);
            specContainer.add(mapContainer);
            specContainer.add(dopContainer);

            specContainer.type = ContainerType.TABBED_PANE;
            return design;

        }
    }

    public class ClusterUserFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objClusterUser;

        public ClusterUserFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Сотрудники кластеров");

            objClusterUser = addSingleGroupObject(clusterUser, "Сотрудник", baseLM.userFirstName, baseLM.userLastName, baseLM.userLogin, baseLM.userPassword, baseLM.email, baseLM.nameUserMainRole, nameClusterClusterUser);
            addObjectActions(this, objClusterUser);
        }
    }

    public class ForesightExpertiseApplyFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;
        private ObjectEntity objCluster;
        private ObjectEntity objForesight;

        public ForesightExpertiseApplyFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Проверка на соответствие форсайту (положительная)");

            objProject = addSingleGroupObject(1, "project", project, "Проект", dateProject, nameNativeProject, nameForeignProject, nameNativeClaimerProject, nameForeignClaimerProject,
                    openApplicationProjectAction, exportProjectDocumentsAction);
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);

            objCluster = addSingleGroupObject(cluster);
            addPropertyDraw(inProjectCluster, objProject, objCluster);
            addPropertyDraw(objCluster, nameNative, nameForeign, nameNativeShort);

            objForesight = addSingleGroupObject(foresight);
//            addPropertyDraw(includeProjectClusterForesight, objProject, objCluster, objForesight);
            addPropertyDraw(objProject, objForesight, isRootInProjectForesight);
            addPropertyDraw(objForesight, sidForesight, nameNative, nameForeign, nameNativeShortClusterForesight, quantityInExpertForesight);

            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(inTestCluster, objCluster))));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(clusterForesight, objForesight), Compare.EQUALS, objCluster));

            setEditType(PropertyEditType.READONLY);

            setEditType(inProjectCluster, PropertyEditType.EDITABLE);
//            setEditType(includeProjectClusterForesight, PropertyEditType.EDITABLE);
            setEditType(isRootInProjectForesight, PropertyEditType.EDITABLE);
            setEditType(openApplicationProjectAction, PropertyEditType.EDITABLE);
            setEditType(exportProjectDocumentsAction, PropertyEditType.EDITABLE);

            addActionsOnEvent(FormEventType.OK, addPropertyObject(setPositiveResultForesightCheckProject, objProject));

            setPageSize(0);

            applyForesightCheckProjectCluster = addMFAProp(actionGroup, "Прошла проверку на соответствие форсайту", this, new ObjectEntity[] {objProject, objCluster}, true);
            applyForesightCheckProjectCluster.property.askConfirm = true;
            applyForesightCheckProjectCluster.setImage("sign_tick.png");

            applyForesightCheckProject = addIfAProp(actionGroup, "applyForesightCheckProject", "Прошла проверку на соответствие форсайту", true, resultForesightCheckProject, 1,
                    addJoinAProp(applyForesightCheckProjectCluster, 1, finalClusterProject, 1), 1);
            applyForesightCheckProject.property.askConfirm = true;
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.getPrintButton().removeFromParent();
            design.getXlsButton().removeFromParent();
            design.getApplyButton().removeFromParent();
            design.getCancelButton().removeFromParent();
            return design;
        }
    }

    public class ForesightExpertiseRejectFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;
        private ObjectEntity objCluster;
        private ObjectEntity objForesight;

        public ForesightExpertiseRejectFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Проверка на соответствие форсайту (отрицательная)");

            objProject = addSingleGroupObject(1, "project", project, "Проект", commentForesightCheckProject);
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnEvent(FormEventType.OK, addPropertyObject(setNegativeResultForesightCheckProject, objProject));

            rejectForesightCheckProject = addIfAProp(actionGroup, "rejectForesightCheckProject", "Не прошла проверку на соответствие форсайту", true, resultForesightCheckProject, 1,
                    addMFAProp(actionGroup, "Не прошла проверку на соответствие форсайту", this, new ObjectEntity[]{objProject}, true), 1);
            rejectForesightCheckProject.property.askConfirm = true;
            rejectForesightCheckProject.setImage("delete.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.getPrintButton().removeFromParent();
            design.getXlsButton().removeFromParent();
            design.getApplyButton().removeFromParent();
            design.getCancelButton().removeFromParent();

            design.get(getPropertyDraw(commentForesightCheckProject)).panelLabelAbove = true;
            design.get(getPropertyDraw(commentForesightCheckProject)).constraints.fillHorizontal = 1;
            design.get(getPropertyDraw(commentForesightCheckProject)).constraints.fillVertical = 1;
            return design;
        }
    }

    public class ForesightAdviceFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;


        public ForesightAdviceFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objProject = addSingleGroupObject(1, "project", project, "Проект", dateProject, nameNativeProject, nameForeignProject, nameNativeClaimerProject, nameForeignClaimerProject, emailClaimerProject, positiveResultForesightCheckProject, negativeResultForesightCheckProject, sidResultForesightCheckProject, langProject);
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(sidResultForesightCheckProject, objProject)));
            setEditType(PropertyEditType.READONLY);

            addInlineEAForm(emailForesightCheckProjectEA, this, objProject, 1);
        }
    }

    public class NotificationProjectFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;


        public NotificationProjectFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objProject = addSingleGroupObject(1, "project", project, "Проект", positiveLegalResultProject, dateProject, nameNativeProject, nameForeignProject, nameNativeClaimerProject, emailClaimerProject, datePositiveLegalResultProject, dateNotificationPeriodProject);      //    уточнить дату
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(positiveLegalResultProject, objProject)));
            setEditType(PropertyEditType.READONLY);

            addInlineEAForm(emailNotificationProjectEA, this, objProject, 1);
        }
    }

     public class ForesightClaimerProjectFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;

        public ForesightClaimerProjectFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objProject = addSingleGroupObject(1, "project", project, "Проект", nameResultForesightCheckProject, dateResultForesightCheckProject, positiveResultForesightCheckProject, negativeResultForesightCheckProject, dateProject, nameNativeProject, nameForeignProject, nameNativeClaimerProject, emailClaimerProject);
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(positiveLegalResultProject, objProject)));   // или        positiveResultForesightCheckProject
            setEditType(PropertyEditType.READONLY);

            addInlineEAForm(emailForesightClaimerProjectEA, this, objProject, 1);
        }
    }

     public class BureauTrProjectFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;

        public BureauTrProjectFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objProject = addSingleGroupObject(1, "project", project, "Проект",
                    nameResultForesightCheckProject, dateResultForesightCheckProject, positiveResultForesightCheckProject,
                    negativeResultForesightCheckProject, dateProject, nameNativeProject, nameForeignProject,
                    nameNativeClaimerProject, emailClaimerProject, dateSentForTranslationProject, dateToSentForTranslationProject);
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(positiveResultForesightCheckProject, objProject)));   // или             positiveLegalResultProject
            setEditType(PropertyEditType.READONLY);

            addInlineEAForm(emailBureauTrProjectEA, this, objProject, 1);
        }
    }

     public class NeedVoteFondProjectFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;


        public NeedVoteFondProjectFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objProject = addSingleGroupObject(1, "project", project, "Проект", positiveLegalResultProject, dateProject, nameNativeProject, nameForeignProject, nameNativeClaimerProject, emailClaimerProject, datePositiveLegalResultProject, resultNeedVoteProject, dateResultNeedVoteProject, dateOverdueResultNeedVoteProject);
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(positiveLegalResultProject, objProject)));
            setEditType(PropertyEditType.READONLY);

            addInlineEAForm(emailNeedVoteProjectEA, this, objProject, 1);

            addDefaultHintsIncrementTable(this);
        }
    }

    public class TransferredProjectFormEntity extends FormEntity<SkolkovoBusinessLogics> {

            private ObjectEntity objProject;


            public TransferredProjectFormEntity(NavigatorElement parent, String sID, String caption) {
                super(parent, sID, caption, true);

                objProject = addSingleGroupObject(1, "project", project, "Проект", dateProject, nameNativeProject, nameForeignProject, nameNativeClaimerProject, emailClaimerProject);
                addPropertyDraw(objProject, translationGroup);
                objProject.groupTo.setSingleClassView(ClassViewType.PANEL);
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(transferredProject, objProject)));
                setEditType(PropertyEditType.READONLY);

                addInlineEAForm(emailTransferredProjectEA, this, objProject, 1);
            }
        }

    public class ForesightExpertiseListFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;
        private ObjectEntity objForesight;
        private RegularFilterGroupEntity projectFilterGroup;

        public ForesightExpertiseListFormEntity(NavigatorElement parent, String sID, String caption, int mode) {
            super(parent, sID, caption);

            if (mode == 1)
                objProject = addSingleGroupObject(project, "Проект", dateProject, nameNativeProject, nameForeignProject, nameNativeShortFinalClusterProject, nameNativeClaimerProject, nameForeignClaimerProject,
                        emailClaimerProject, nameResultForesightCheckProject, dateResultForesightCheckProject, dateNotificationPeriodProject, nameUserResultForesightCheckProject, openApplicationProjectAction, exportProjectDocumentsAction, applyForesightCheckProject, rejectForesightCheckProject);
            else if (mode == 2) {
                objProject = addSingleGroupObject(project, "Проект", dateProject, nameNativeProject, nameForeignProject, nameNativeClaimerProject, nameForeignClaimerProject,
                        emailClaimerProject, nameResultForesightCheckProject, dateResultForesightCheckProject, dateNotificationPeriodProject, nameUserResultForesightCheckProject, openApplicationProjectAction, exportProjectDocumentsAction, applyForesightCheckProject, rejectForesightCheckProject);
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(inClusterCurrentUserProject, objProject)));
            }

            objForesight = addSingleGroupObject(foresight, sidForesight, nameNative, nameForeign, nameNativeShortClusterForesight, quantityInExpertForesight);

            projectFilterGroup = new RegularFilterGroupEntity(genID());
            projectFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(nameResultForesightCheckProject, objProject))),
                    "Только непроверенные проекты",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(projectFilterGroup);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inProjectForesight, objProject, objForesight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(needForesightCheckProject, objProject)));
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(withdrawnProject, objProject))));

            setEditType(PropertyEditType.READONLY, objProject.groupTo);
            setEditType(actionGroup, PropertyEditType.EDITABLE);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.getGroupObjectContainer(objProject.groupTo).constraints.fillVertical = 5;
            return design;
        }
    }

    public class LegalCheckAcceptPreliminaryFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;
        private ObjectEntity objComment;

        public LegalCheckAcceptPreliminaryFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Юридическая проверка (положительная на предварительную)");

            objProject = addSingleGroupObject(1, "project", project, "Проект");
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);

            objComment = addSingleGroupObject(TextClass.instance, "Комментарий", baseLM.objectValue);
            objComment.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnEvent(FormEventType.OK, addPropertyObject(addSetPropertyAProp(projectActionProject, 1, addCProp(projectAction, "preliminary")), objProject));

            addActionsOnEvent(FormEventType.OK, addPropertyObject(addJoinAProp(
                        addAAProp(legalCheck, projectLegalCheck, commentLegalCheck, changeLegalCheck, resultLegalCheck),
                        1, 2, addCProp(LogicalClass.instance, true), addCProp(legalCheckResult, "positiveLegalCheckResult")
                        ), objProject, objComment));

            acceptPreliminaryLegalCheckProject = addIfAProp(actionGroup, "acceptPreliminaryLegalCheckProject", "Прошла юридическую проверку (на предварительную экспертизу)",
                    addJProp(and(false, true), needLegalCheckStatusProject, 1, isStatusProject, 1, isPreliminaryAndStatusProject, 1), 1,
                    addMFAProp(actionGroup, "Не прошла юридическую проверку", this, new ObjectEntity[]{objProject}, true), 1);
            acceptPreliminaryLegalCheckProject.property.askConfirm = true;
            acceptPreliminaryLegalCheckProject.setImage("sign_tick.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.getPrintButton().removeFromParent();
            design.getXlsButton().removeFromParent();
            design.getApplyButton().removeFromParent();
            design.getCancelButton().removeFromParent();

            design.get(getPropertyDraw(baseLM.objectValue, objComment)).panelLabelAbove = true;
            design.get(getPropertyDraw(baseLM.objectValue, objComment)).constraints.fillHorizontal = 1;
            design.get(getPropertyDraw(baseLM.objectValue, objComment)).constraints.fillVertical = 1;
            return design;
        }
    }

    public class LegalCheckRejectFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;
        private ObjectEntity objComment;

        public LegalCheckRejectFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Юридическая проверка (отрицательная)");

            objProject = addSingleGroupObject(1, "project", project, "Проект");
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);

            objComment = addSingleGroupObject(TextClass.instance, "Комментарий", baseLM.objectValue);
            objComment.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnEvent(FormEventType.OK, addPropertyObject(addJoinAProp(
                        addAAProp(legalCheck, projectLegalCheck, commentLegalCheck, resultLegalCheck),
                        1, 2, addCProp(legalCheckResult, "negativeLegalCheckResult"))
                                                 , objProject, objComment));

            rejectLegalCheckProject = addIfAProp(actionGroup, "rejectLegalCheckProject", "Не прошла юридическую проверку", needLegalCheckStatusProject, 1,
                    addMFAProp(actionGroup, "Не прошла юридическую проверку", this, new ObjectEntity[]{objProject}, true), 1);
            rejectLegalCheckProject.property.askConfirm = true;
            rejectLegalCheckProject.setImage("delete.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.getPrintButton().removeFromParent();
            design.getXlsButton().removeFromParent();
            design.getApplyButton().removeFromParent();
            design.getCancelButton().removeFromParent();

            design.get(getPropertyDraw(baseLM.objectValue, objComment)).panelLabelAbove = true;
            design.get(getPropertyDraw(baseLM.objectValue, objComment)).constraints.fillHorizontal = 1;
            design.get(getPropertyDraw(baseLM.objectValue, objComment)).constraints.fillVertical = 1;
            return design;
        }
    }

    public class LegalCheckExpertiseFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;
        private ObjectEntity objLegalCheck;

        private RegularFilterGroupEntity projectFilterGroup;

        public LegalCheckExpertiseFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objProject = addSingleGroupObject(1, "project", project, "Проект", dateProject, dateStatusProject, nameNativeProject, nameForeignProject,
                nameNativeShortFinalClusterProject, nameNativeClaimerProject, nameForeignClaimerProject, emailClaimerProject,
                nameStatusProject, isPreliminaryAndStatusProject, nameProjectActionProject, updateDateProject, nameRegulationsProject,
                dateExecuteLegalCheckProject, openCompleteApplicationProjectAction, exportProjectDocumentsAction,
                setPositiveLCResultApplyProject, acceptPreliminaryLegalCheckProject, rejectLegalCheckProject, needNoticeLegalResultProject);

//                showIf(this, setPositiveLCResultApplyProject, needLegalCheckStatusProject, objProject);

            objLegalCheck = addSingleGroupObject(legalCheck);
            addPropertyDraw(objLegalCheck, dateTimeSubmitLegalCheck, nameResultLegalCheck, changeLegalCheck, nameProjectActionLegalCheck,
                    dateTimeLegalCheck, nameUserLegalCheck, emailClaimerLegalCheck, resultNoticedLegalCheck,
                    dateResultNoticedLegalCheck, overdueDateLegalCheck, baseLM.delete);
            addPropertyDraw(commentLegalCheck, objLegalCheck).forceViewType = ClassViewType.PANEL;
            
            addPropertyDraw(objLegalCheck, changeLegalCheckGroup);
            setForceViewType(changeLegalCheckGroup, ClassViewType.PANEL);
            
            showIf(this, new LP[] {emailClaimerChangeLegalCheck, noticedChangeLegalCheck, dateChangeLegalCheck}, changeLegalCheck, objLegalCheck);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectLegalCheck, objLegalCheck), Compare.EQUALS, objProject));
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(inactiveProject, objProject))));

            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(withdrawnProject, objProject))));
//            addFixedFilter(new NotNullFilterEntity(addPropertyObject(positiveFormalResultProject, objProject)));

            projectFilterGroup = new RegularFilterGroupEntity(genID());
            projectFilterGroup.addFilter(new RegularFilterEntity(genID(),
                new NotNullFilterEntity(addPropertyObject(needLegalCheckStatusProject, objProject)),
                "Только непроверенные проекты",
                KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(projectFilterGroup);

            setEditType(PropertyEditType.READONLY, objProject.groupTo);
            setEditType(nameUserLegalCheck, PropertyEditType.READONLY);
            setEditType(actionGroup, PropertyEditType.EDITABLE);

            addDefaultOrder(updateDateProject, false);

            addDefaultHintsIncrementTable(this);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.getGroupObjectContainer(objLegalCheck.groupTo).setFixedSize(new Dimension(-1, 300));

            design.get(getPropertyDraw(commentLegalCheck)).constraints.fillHorizontal = 1;
            design.get(getPropertyDraw(commentLegalCheck)).constraints.fillVertical = 1;
            design.get(getPropertyDraw(commentLegalCheck)).panelLabelAbove = true;

            return design;
        }
    }

    public class FillLangProjectActionProperty extends UserActionProperty {

        private final ClassPropertyInterface projectInterface;

        public FillLangProjectActionProperty(SkolkovoLogicsModule LM, ValueClass project) {
            super(LM.genSID(), "Заполнить язык", new ValueClass[]{project});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            projectInterface = i.next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

            DataObject projectObject = context.getKeyValue(projectInterface);

            Object fillNative = fillNativeProject.read(context, projectObject);
            Object fillForeign = fillForeignProject.read(context, projectObject);

            String lang = "";
            if ((fillNative != null) && (fillForeign != null)) {
                lang = "Рус/Англ";
            } else if (fillNative != null) {
                lang = "Рус";
            } else if (fillForeign != null) {
                lang = "Англ";
            }
            langProject.change(lang, context, projectObject);
        }
    }

    public class IncludeProjectClusterForesightActionProperty extends UserActionProperty {

        private final ClassPropertyInterface projectInterface;
        private final ClassPropertyInterface clusterInterface;
        private final ClassPropertyInterface foresightInterface;

        public IncludeProjectClusterForesightActionProperty(ValueClass project, ValueClass cluster, ValueClass foresight) {
            super("IncludeProjectClusterForesight", "Подключить", new ValueClass[]{project, cluster, foresight});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            projectInterface = i.next();
            clusterInterface = i.next();
            foresightInterface = i.next();
        }

        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataObject projectObject = context.getKeyValue(projectInterface);
            DataObject clusterObject = context.getKeyValue(clusterInterface);
            DataObject foresightObject = context.getKeyValue(foresightInterface);

            Object valueInProjectForesight = null;
            if (inProjectForesight.read(context, projectObject, foresightObject) == null)
                valueInProjectForesight = true;
            inProjectForesight.change(valueInProjectForesight, context, projectObject, foresightObject);

            String codeForesight = "";
            Object codeForesightObject = sidForesight.read(context, foresightObject);
            if (codeForesightObject != null)
                codeForesight = codeForesightObject.toString().trim();

            LCP<PropertyInterface> isForesight = (LCP<PropertyInterface>) is(foresight);
            Map<PropertyInterface, KeyExpr> keys = isForesight.getMapKeys();
            KeyExpr key = BaseUtils.singleValue(keys);
            Query<PropertyInterface, Object> query = new Query<PropertyInterface, Object>(keys);
            query.properties.put("sidForesight", sidForesight.getExpr(BaseUtils.singleValue(keys)));
            query.and(isForesight.property.getExpr(keys).getWhere());
            query.and(clusterForesight.getExpr(context.getModifier(), key).compare(clusterObject.getExpr(), Compare.EQUALS));
            OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> result = query.execute(context.getSession().sql);

            for (Map.Entry<Map<PropertyInterface, Object>, Map<Object, Object>> rows : result.entrySet()) {
                DataObject foresightNewObject = new DataObject(rows.getKey().get(((OrderedMap) keys).getKey(0)), foresight);
                if (rows.getValue().get("sidForesight").toString().startsWith(codeForesight) && (!"".equals(codeForesight)))
                    inProjectForesight.change(valueInProjectForesight, context, projectObject, foresightNewObject);
            }
        }
    }

    private class ProjectFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objProject;
        private ObjectEntity objCluster;
        private ObjectEntity objForesight;
        private ObjectEntity objVote;
        private ObjectEntity objDocument;
        private ObjectEntity objExpert;
        private ObjectEntity objDocumentTemplate;
        private ObjectEntity objFormalControl;
        private ObjectEntity objLegalCheck;
        private ObjectEntity objOriginalDocsCheck;
        private ObjectEntity objNonRussianSpecialist;
        private RegularFilterGroupEntity projectFilterGroup;
        private RegularFilterGroupEntity activeProjectFilterGroup;
        private PropertyDrawEntity nameNativeEntity;
        private PropertyDrawEntity nameForeignEntity;
        private PropertyDrawEntity nameNativeClaimerEntity;
        private PropertyDrawEntity nameForeignClaimerEntity;
        private PropertyDrawEntity nameNativeTranslationEntity;
        private PropertyDrawEntity nameForeignTranslationEntity;
        private PropertyDrawEntity nameNativeClaimerTranslationEntity;
        private PropertyDrawEntity nameForeignClaimerTranslationEntity;

        private ProjectFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр проектов");

            objProject = addSingleGroupObject(project, dateProject, dateStatusProject, nameNativeProject, nameForeignProject,
                    nameNativeShortFinalClusterProject, nameNativeClaimerProject, nameForeignClaimerProject, emailClaimerProject,
                    nameStatusProject, dateInStatusProject, normalPeriodStatusProject, quantityDaysToOverdueDateStatusProject, formLogNameStatusProject, nameProjectActionProject, updateDateProject, autoGenerateProject,
                    inactiveProject, quantityClusterProject, quantityClusterVotedProject, quantityVoteProject, generateVoteProject,
                    nameRegulationsProject, useAllClusterExpertsUnionProject);

            addPropertyDraw(objProject, isOtherClusterProject, nativeSubstantiationOtherClusterProject, foreignSubstantiationOtherClusterProject);
            addPropertyDraw(objProject, registerGroup);
            setForceViewType(registerGroup, ClassViewType.PANEL);
            //   setForceViewType(dateNotEnoughOriginalDocsProject, ClassViewType.PANEL);
            getPropertyDraw(isOtherClusterProject).propertyCaption = addPropertyObject(hideIsOtherClusterProject, objProject);
            getPropertyDraw(nativeSubstantiationOtherClusterProject).propertyCaption = addPropertyObject(hideNativeSubstantiationOtherClusterProject, objProject);
            getPropertyDraw(foreignSubstantiationOtherClusterProject).propertyCaption = addPropertyObject(hideForeignSubstantiationOtherClusterProject, objProject);

            addPropertyDraw(nameNativeShortCurrentCluster).toDraw = objProject.groupTo;
            setForceViewType(nameNativeShortCurrentCluster, ClassViewType.PANEL);
            addPropertyDraw(quantityDefaultVoteProject, objProject).forceViewType = ClassViewType.PANEL;

//            addPropertyDraw(objProject, translateToRussianProject, translateToEnglishProject);
//            setForceViewType(translateToRussianProject, ClassViewType.PANEL);
//            setForceViewType(translateToEnglishProject, ClassViewType.PANEL);

            nameNativeEntity = addPropertyDraw(nameNativeProject, objProject);
            nameForeignEntity = addPropertyDraw(nameForeignProject, objProject);
            nameNativeClaimerEntity = addPropertyDraw(nameNativeClaimerProject, objProject);
            nameForeignClaimerEntity = addPropertyDraw(nameForeignClaimerProject, objProject);

            setForceViewType(nameNativeEntity, ClassViewType.PANEL);
            setForceViewType(nameForeignEntity, ClassViewType.PANEL);
            setForceViewType(nameNativeClaimerEntity, ClassViewType.PANEL);
            setForceViewType(nameForeignClaimerEntity, ClassViewType.PANEL);

            nameNativeTranslationEntity = addPropertyDraw(nameNativeProject, objProject);
            nameForeignTranslationEntity = addPropertyDraw(nameForeignProject, objProject);
            nameNativeClaimerTranslationEntity = addPropertyDraw(nameNativeClaimerProject, objProject);
            nameForeignClaimerTranslationEntity = addPropertyDraw(nameForeignClaimerProject, objProject);

            setForceViewType(nameNativeTranslationEntity, ClassViewType.PANEL);
            setForceViewType(nameForeignTranslationEntity, ClassViewType.PANEL);
            setForceViewType(nameNativeClaimerTranslationEntity, ClassViewType.PANEL);
            setForceViewType(nameForeignClaimerTranslationEntity, ClassViewType.PANEL);

            addPropertyDraw(objProject, translationGroup, projectDocumentsGroup, withdrawnGroup);
            setForceViewType(translationGroup, ClassViewType.PANEL);
            setForceViewType(projectDocumentsGroup, ClassViewType.PANEL);
            setForceViewType(withdrawnGroup, ClassViewType.PANEL);

            addPropertyDraw(importProjectsAction).toDraw = objProject.groupTo;
            setForceViewType(importProjectsAction, ClassViewType.PANEL);

            addPropertyDraw(copyProjectAction, objProject).toDraw = objProject.groupTo;
            setForceViewType(copyProjectAction, ClassViewType.PANEL);

            //addPropertyDraw(openApplicationProjectAction, objProject).toDraw = objProject.groupTo;
            //setForceViewType(openApplicationProjectAction, ClassViewType.PANEL);

//            hideTranslateToRussianProject = addHideCaptionProp(privateGroup, "Перевести", translateToRussianProject, needsToBeTranslatedToRussianProject);
//            getPropertyDraw(translateToRussianProject).propertyCaption = addPropertyObject(hideTranslateToRussianProject, objProject);
//
//            hideTranslateToEnglishProject = addHideCaptionProp(privateGroup, "Перевести", translateToEnglishProject, needsToBeTranslatedToEnglishProject);
//            getPropertyDraw(translateToEnglishProject).propertyCaption = addPropertyObject(hideTranslateToEnglishProject, objProject);

//            hideLoadFileResolutionIPProject = addHideCaptionProp(privateGroup, "Загрузить", loadFileResolutionIPProject, addJProp(baseLM.andNot1, addCProp(LogicalClass.instance, true, project), 1, openFileResolutionIPProject, 1));
//            getPropertyDraw(loadFileResolutionIPProject).propertyCaption = addPropertyObject(hideLoadFileResolutionIPProject, objProject);

            showIf(this, loadFileResolutionIPProject, addJProp(baseLM.andNot1, isR1Project, 1, fileResolutionIPProject, 1), objProject);

            addObjectActions(this, objProject, false, false);

            addPropertyDraw(objProject, editR1Project, editR2Project, editClaimerProject);

            setForceViewType(editR1Project, ClassViewType.PANEL);
            getPropertyDraw(editR1Project).propertyCaption = addPropertyObject(addHideCaptionProp(isR1Project), objProject);

            setForceViewType(editR2Project, ClassViewType.PANEL);
            getPropertyDraw(editR2Project).propertyCaption = addPropertyObject(addHideCaptionProp(isR2Project), objProject);

            setForceViewType(editClaimerProject, ClassViewType.PANEL);

            LCP highlightColor = addCProp(ColorClass.instance, new Color(223, 255, 223));
            CalcPropertyObjectEntity statusProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, statusDataProject, 1), objProject);
            getPropertyDraw(nameStatusProject).setPropertyBackground(statusProperty);

            CalcPropertyObjectEntity dateProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, dateDataProject, 1), objProject);
            getPropertyDraw(dateProject).setPropertyBackground(dateProperty);

            CalcPropertyObjectEntity dateStatusProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, dateStatusDataProject, 1), objProject);
            getPropertyDraw(dateStatusProject).setPropertyBackground(dateStatusProperty);

            CalcPropertyObjectEntity nameNativeProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, nameNativeDataProject, 1), objProject);
            getPropertyDraw(nameNativeProject).setPropertyBackground(nameNativeProperty);

            CalcPropertyObjectEntity nameForeignProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, nameForeignDataProject, 1), objProject);
            getPropertyDraw(nameForeignProject).setPropertyBackground(nameForeignProperty);

            CalcPropertyObjectEntity nameNativeCorrectHighlightClaimerProjectProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, nameNativeCorrectHighlightClaimerProject, 1), objProject);
            getPropertyDraw(nameNativeClaimerProject).setPropertyBackground(nameNativeCorrectHighlightClaimerProjectProperty);

            CalcPropertyObjectEntity nameForeignCorrectHighlightClaimerProjectProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, nameForeignCorrectHighlightClaimerProject, 1), objProject);
            getPropertyDraw(nameForeignClaimerProject).setPropertyBackground(nameForeignCorrectHighlightClaimerProjectProperty);

//            addPropertyDraw(addProject).toDraw = objProject.groupTo;
//            getPropertyDraw(addProject).forceViewType = ClassViewType.PANEL;

            objCluster = addSingleGroupObject(cluster);
            addPropertyDraw(inProjectCluster, objProject, objCluster);
            addPropertyDraw(inClaimerProjectCluster, objProject, objCluster);
            addPropertyDraw(objCluster, nameNative, nameForeign, nameNativeShort, sidCluster);
            addPropertyDraw(new LCP[]{nativeSubstantiationProjectCluster, foreignSubstantiationProjectCluster}, objProject, objCluster);
            addPropertyDraw(numberCluster, objCluster);

            getPropertyDraw(generateVoteProject).forceViewType = ClassViewType.PANEL;
            getPropertyDraw(generateVoteProject).propertyCaption = addPropertyObject(hideGenerateVoteProject, objProject);

            objForesight = addSingleGroupObject(foresight);
            addPropertyDraw(includeProjectClusterForesight, objProject, objCluster, objForesight);
            addPropertyDraw(inProjectForesight, objProject, objForesight);
            addPropertyDraw(objForesight, sidForesight, nameNative, nameForeign, nameNativeShortClusterForesight, quantityInExpertForesight);

            objVote = addSingleGroupObject(vote, dateStartVote, dateEndVote, nameNativeClusterVote, nameProjectActionVote, requiredQuantityVote, percentNeededVote, openedVote, succeededVote, acceptedVote,
                    quantityDoneVote,
                    quantityInClusterVote, quantityInnovativeVote, quantityForeignVote,
                    quantityCompetitiveAdvantagesVote, quantityCommercePotentialVote, quantityCanBeImplementedVote,
                    quantityHaveExpertiseVote, quantityInternationalExperienceVote, quantityEnoughDocumentsVote,
                    attachProtocolDecisionVote, loadFileDecisionVote, openFileDecisionVote,
                    emailClaimerVote, emailNoticeRejectedVote, emailNoticeAcceptedStatusVote, emailNoticeAcceptedPreliminaryVote,
                    decisionNoticedVote, dateDecisionNoticedVote, baseLM.delete);
            objVote.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.HIDE));

            getPropertyDraw(percentNeededVote).forceViewType = ClassViewType.GRID;
            showIf(this, percentNeededVote, isR2Project, objProject);
            
            getPropertyDraw(attachProtocolDecisionVote).forceViewType = ClassViewType.GRID;
            showIf(this, attachProtocolDecisionVote, isR2Project, objProject);

            setForceViewType(voteResultGroup, ClassViewType.GRID, objVote.groupTo);

            showIf(this, new LCP[] {quantityInClusterVote, quantityInnovativeVote, quantityForeignVote},
                    isR1Project, objProject);

            showIf(this, new LCP[] {quantityCompetitiveAdvantagesVote, quantityCommercePotentialVote, quantityCanBeImplementedVote, quantityHaveExpertiseVote, quantityInternationalExperienceVote, quantityEnoughDocumentsVote},
                    isR2Project, objProject);

//            getPropertyDraw(copyResultsVote).forceViewType = ClassViewType.PANEL;

            objDocumentTemplate = addSingleGroupObject(documentTemplate, "Шаблон документов", baseLM.name);
            objDocumentTemplate.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objDocumentTemplate, PropertyEditType.READONLY);
            addPropertyDraw(generateDocumentsProjectDocumentType, objProject, objDocumentTemplate);

            objDocument = addSingleGroupObject(document, nameTypeDocument, nameLanguageDocument, postfixDocument, loadFileDocument, openFileDocument);
            addObjectActions(this, objDocument);
            getPropertyDraw(postfixDocument).forceViewType = ClassViewType.PANEL;
            getPropertyDraw(postfixDocument).propertyCaption = addPropertyObject(hidePostfixDocument, objDocument);

            addPropertyDraw(exportProjectDocumentsAction, objProject).toDraw = objDocument.groupTo;
            setForceViewType(exportProjectDocumentsAction, ClassViewType.PANEL);

            addPropertyDraw(nameResultForesightCheckProject, objProject).toDraw = objDocument.groupTo;
            setForceViewType(nameResultForesightCheckProject, ClassViewType.PANEL);
            addPropertyDraw(dateResultForesightCheckProject, objProject).toDraw = objDocument.groupTo;
            setForceViewType(dateResultForesightCheckProject, ClassViewType.PANEL);
            addPropertyDraw(nameUserResultForesightCheckProject, objProject).toDraw = objDocument.groupTo;
            setForceViewType(nameUserResultForesightCheckProject, ClassViewType.PANEL);
            addPropertyDraw(commentForesightCheckProject, objProject).toDraw = objDocument.groupTo;
            setForceViewType(commentForesightCheckProject, ClassViewType.PANEL);

            addPropertyDraw(includeDocumentsProject, objProject).toDraw = objDocument.groupTo;
            setForceViewType(includeDocumentsProject, ClassViewType.PANEL);

            //hideIncludeDocumentsProject = addHideCaptionProp(privateGroup, "Подключить", includeDocumentsProject, addJProp(baseLM.andNot1, openFileResolutionIPProject, 1, needTranslationProject, 1));
            //getPropertyDraw(includeDocumentsProject).propertyCaption = addPropertyObject(hideIncludeDocumentsProject, objProject);
//            hideIncludeDocumentsProject = addHideCaptionProp(privateGroup, "Подключить", includeDocumentsProject, openFileResolutionIPProject);
//            getPropertyDraw(includeDocumentsProject).propertyCaption = addPropertyObject(hideIncludeDocumentsProject, objProject);
            showIf(this, includeDocumentsProject, addJProp(baseLM.and1, addSUProp(Union.OVERRIDE, isR2Project, addJProp(baseLM.and1, isR1Project, 1, fileResolutionIPProject, 1)), 1, positiveLegalResultProject, 1), objProject);

            objExpert = addSingleGroupObject(expert);
            addPropertyDraw(objExpert, objVote, inExpertVote, oldExpertVote, businessExpertVote, exclExpertVote);
            addPropertyDraw(objExpert, baseLM.name, documentNameExpert, sidExpert, baseLM.email);
            addPropertyDraw(voteResultGroup, true, objExpert, objVote);

            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);
            setForceViewType(projectOtherClusterGroup, ClassViewType.PANEL);

            objFormalControl = addSingleGroupObject(formalControl);
            addPropertyDraw(new LP[]{addNoListOfExpertsFCResult, addNotEnoughDocumentsFCResult, addNotSuitableClusterFCResult, addRepeatedFCResult, addPositiveFCResult});
            addPropertyDraw(objFormalControl, dateTimeSubmitFormalControl, nameResultFormalControl, nameProjectActionFormalControl, dateTimeFormalControl, emailClaimerFormalControl, resultNoticedFormalControl, dateResultNoticedFormalControl, baseLM.delete);
            addPropertyDraw(commentFormalControl, objFormalControl).forceViewType = ClassViewType.PANEL;

            showIf(this, new LP[] {addPositiveFCResult, addNotEnoughDocumentsFCResult, addNoListOfExpertsFCResult, addNotSuitableClusterFCResult, addRepeatedFCResult},
                    needFormalCheckStatusProject, objProject);

            objLegalCheck = addSingleGroupObject(legalCheck);
            addPropertyDraw(objLegalCheck, dateTimeSubmitLegalCheck, nameResultLegalCheck, changeLegalCheck, nameProjectActionLegalCheck, dateTimeLegalCheck, nameUserLegalCheck, emailClaimerLegalCheck, resultNoticedLegalCheck, dateResultNoticedLegalCheck, baseLM.delete);
            addPropertyDraw(new LP[]{addNegativeLCResult, addPositiveLCResult});
            addPropertyDraw(commentLegalCheck, objLegalCheck).forceViewType = ClassViewType.PANEL;

            showIf(this, new LP[] {addPositiveLCResult, addNegativeLCResult},
                    needLegalCheckStatusProject, objProject);

            objOriginalDocsCheck = addSingleGroupObject(originalDocsCheck);
            addPropertyDraw(objOriginalDocsCheck, dateSubmitOriginalDocsCheck, nameResultOriginalDocsCheck, dateTimeOriginalDocsCheck, setNegativeResultOriginalDocsCheck, setPositiveResultOriginalDocsCheck);
            addPropertyDraw(commentOriginalDocsCheck, objOriginalDocsCheck).forceViewType = ClassViewType.PANEL;
            addObjectActions(this, objOriginalDocsCheck);

            objNonRussianSpecialist = addSingleGroupObject(4, "nonRussianSpecialist", nonRussianSpecialist, "Иностранный специалист", baseGroup);
            addObjectActions(this, objNonRussianSpecialist);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectNonRussianSpecialist, objNonRussianSpecialist), Compare.EQUALS, objProject));

            getPropertyDraw(importProjectsAction).setDrawToToolbar(true);
            getPropertyDraw(copyProjectAction).setDrawToToolbar(true);
            //getPropertyDraw(openApplicationProjectAction).drawToToolbar = true;
            getPropertyDraw(editR1Project).setDrawToToolbar(true);
            getPropertyDraw(editR2Project).setDrawToToolbar(true);
            getPropertyDraw(editClaimerProject).setDrawToToolbar(true);
            getPropertyDraw(nameNativeShortCurrentCluster).setDrawToToolbar(true);

//            addFixedFilter(new CompareFilterEntity(addPropertyObject(clusterForesight, objForesight), Compare.EQUALS, objCluster));
     //       addFixedFilter(new NotNullFilterEntity(addPropertyObject(isInClusterProjectForesight, objProject, objForesight)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectVote, objVote), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectDocument, objDocument), Compare.EQUALS, objProject));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(clusterInExpertVote, objExpert, objVote)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectFormalControl, objFormalControl), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectLegalCheck, objLegalCheck), Compare.EQUALS, objProject));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectOriginalDocsCheck, objOriginalDocsCheck), Compare.EQUALS, objProject));
            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(inProjectCurrentCluster, objProject)),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(currentCluster)))));

            RegularFilterGroupEntity foresightFilterGroup = new RegularFilterGroupEntity(genID());
            foresightFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(inProjectForesight, objProject, objForesight)),
                    "В проекте",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(foresightFilterGroup);

            RegularFilterGroupEntity expertFilterGroup = new RegularFilterGroupEntity(genID());
            expertFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)),
                    "В заседании",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(expertFilterGroup);

            projectFilterGroup = new RegularFilterGroupEntity(genID());
            projectFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(valuedProject, objProject)),
                    "Оценен",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(projectFilterGroup);

            activeProjectFilterGroup = new RegularFilterGroupEntity(genID());
            activeProjectFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(inactiveProject, objProject))),
                    "Активный",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), true);
            addRegularFilterGroup(activeProjectFilterGroup);

            addDefaultHintsIncrementTable(this);
            addHintsNoUpdate(dateInStatusProject, quantityDaysToOverdueDateStatusProject);

//            setPageSize(0);

            setEditType(PropertyEditType.READONLY, objCluster.groupTo);
            setEditType(inProjectCluster, PropertyEditType.EDITABLE);
            setEditType(inClaimerProjectCluster, PropertyEditType.EDITABLE);

            addDefaultOrder(getPropertyDraw(dateProject, objProject), true);
            addDefaultOrder(numberCluster, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

//            design.get(getPropertyDraw(addProject)).setPanelLocation(new ToolbarPanelLocation());

//            design.addIntersection(design.getGroupPropertyContainer(objProject.groupTo, projectInformationGroup),
//                    design.getGroupPropertyContainer(objProject.groupTo, translateActionGroup),
//                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

//            design.getPanelContainer(objProject.groupTo).add(design.getGroupPropertyContainer((GroupObjectEntity)null, importGroup));

            ContainerView specContainer = design.createContainer();
            specContainer.type = ContainerType.TABBED_PANE;

            ContainerView projectMainInformationContainer = design.createContainer();
            projectMainInformationContainer.add(design.get(nameNativeEntity));
            projectMainInformationContainer.add(design.get(nameForeignEntity));
            projectMainInformationContainer.add(design.get(nameNativeClaimerEntity));
            projectMainInformationContainer.add(design.get(nameForeignClaimerEntity));

            ContainerView projectTranslationInformationContainer = design.createContainer();
            projectTranslationInformationContainer.add(design.get(nameNativeTranslationEntity));
            projectTranslationInformationContainer.add(design.get(nameForeignTranslationEntity));
            projectTranslationInformationContainer.add(design.get(nameNativeClaimerTranslationEntity));
            projectTranslationInformationContainer.add(design.get(nameForeignClaimerTranslationEntity));

            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objProject.groupTo));

            ContainerView infoContainer = design.createContainer("Информация");
            infoContainer.add(projectMainInformationContainer);
            infoContainer.add(design.getGroupObjectContainer(objCluster.groupTo));
            infoContainer.add(design.getGroupPropertyContainer(objProject.groupTo, projectOtherClusterGroup));

//            ContainerView withHeaderContainer = design.createContainer();
//            withHeaderContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHTBOTTOM;
//            withHeaderContainer.add(design.getGroupPropertyContainer(objProject.groupTo, withdrawnGroup));

            ContainerView formalControlContainer = design.createContainer("Формальная экспертиза");
            formalControlContainer.add(design.get(getPropertyDraw(exportProjectDocumentsAction)));
            formalControlContainer.add(design.getGroupObjectContainer(objFormalControl.groupTo));
            formalControlContainer.add(design.getGroupPropertyContainer(objProject.groupTo, withdrawnGroup));
            formalControlContainer.add(design.getGroupPropertyContainer((GroupObjectEntity) null, formalControlResultGroup));
            formalControlContainer.add(design.get(getPropertyDraw(nameResultForesightCheckProject)));
            formalControlContainer.add(design.get(getPropertyDraw(dateResultForesightCheckProject)));
            formalControlContainer.add(design.get(getPropertyDraw(nameUserResultForesightCheckProject)));
//            formalControlContainer.add(withHeaderContainer);


            PropertyDrawView commentFormalView = design.get(getPropertyDraw(commentFormalControl, objFormalControl));
            commentFormalView.constraints.fillHorizontal = 1.0;
            commentFormalView.preferredSize = new Dimension(-1, 200);
            commentFormalView.panelLabelAbove = true;

            ContainerView legalCheckContainer = design.createContainer("Юридическая проверка");
            legalCheckContainer.add(design.getGroupObjectContainer(objLegalCheck.groupTo));
            legalCheckContainer.add(design.getGroupPropertyContainer((GroupObjectEntity) null, legalCheckResultGroup));
            PropertyDrawView commentLegalView = design.get(getPropertyDraw(commentLegalCheck, objLegalCheck));
            commentLegalView.constraints.fillHorizontal = 1.0;
            commentLegalView.preferredSize = new Dimension(-1, 200);
            commentLegalView.panelLabelAbove = true;

            ContainerView foresightCheckContainer = design.createContainer("Соответствие форсайту");
            foresightCheckContainer.add(design.get(getPropertyDraw(nameResultForesightCheckProject)));
            foresightCheckContainer.add(design.get(getPropertyDraw(dateResultForesightCheckProject)));
            foresightCheckContainer.add(design.get(getPropertyDraw(nameUserResultForesightCheckProject)));
            foresightCheckContainer.add(design.get(getPropertyDraw(commentForesightCheckProject)));
            foresightCheckContainer.add(design.getGroupObjectContainer(objForesight.groupTo));

            design.get(getPropertyDraw(commentForesightCheckProject)).panelLabelAbove = true;
            design.get(getPropertyDraw(commentForesightCheckProject)).constraints.fillHorizontal = 1;

            ContainerView projectDocumentsContainer = design.getGroupPropertyContainer(objProject.groupTo, projectDocumentsGroup);
            projectDocumentsContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;

            ContainerView translationHeaderContainer = design.createContainer();
            translationHeaderContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            translationHeaderContainer.add(design.getGroupPropertyContainer(objProject.groupTo, translationGroup));
            translationHeaderContainer.add(projectTranslationInformationContainer);

            ContainerView translationContainer = design.createContainer("Перевод");
            translationContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            translationContainer.constraints.fillHorizontal = 1.0;
            translationContainer.constraints.fillVertical = 1.0;
            translationContainer.add(translationHeaderContainer);
            translationContainer.add(projectDocumentsContainer);
            translationContainer.add(design.getGroupObjectContainer(objNonRussianSpecialist.groupTo));
//            translationContainer.add(design.getGroupPropertyContainer(objProject.groupTo, translateActionGroup));

            ContainerView docContainer = design.createContainer("Документы");
            docContainer.add(design.getGroupObjectContainer(objDocumentTemplate.groupTo));
            docContainer.add(design.getGroupObjectContainer(objDocument.groupTo));
            docContainer.add(design.getGroupPropertyContainer(objProject.groupTo, resolutionIPGroup));

            ContainerView expertContainer = design.createContainer("Экспертиза по существу");
            expertContainer.add(design.getGroupObjectContainer(objVote.groupTo));
            expertContainer.add(design.getGroupObjectContainer(objExpert.groupTo));
            expertContainer.add(design.get(getPropertyDraw(quantityDefaultVoteProject)));

            ContainerView originalDocsContainer = design.createContainer("Проверка оригиналов документов");
            originalDocsContainer.add(design.getGroupObjectContainer(objOriginalDocsCheck.groupTo));
//            originalDocsContainer.add(design.getGroupPropertyContainer((GroupObjectEntity) null, originalDoscCheckGroup));
            PropertyDrawView commentOriginalDocsView = design.get(getPropertyDraw(commentOriginalDocsCheck, objOriginalDocsCheck));
            commentOriginalDocsView.constraints.fillHorizontal = 1.0;
            commentOriginalDocsView.preferredSize = new Dimension(-1, 200);
            commentOriginalDocsView.panelLabelAbove = true;

            ContainerView registerContainer = design.createContainer("Оформление свидетельства");
            registerContainer.constraints.fillHorizontal = 1.0;
            registerContainer.constraints.fillVertical = 1.0;
            registerContainer.add(design.getGroupPropertyContainer(objProject.groupTo, registerGroup));

            specContainer.add(infoContainer);
            specContainer.add(formalControlContainer);
            specContainer.add(legalCheckContainer);
            specContainer.add(foresightCheckContainer);
            specContainer.add(translationContainer);
            specContainer.add(docContainer);
            specContainer.add(expertContainer);
            specContainer.add(originalDocsContainer);
            specContainer.add(registerContainer);

//            design.get(objVoteHeader.groupTo).grid.constraints.fillHorizontal = 1.5;

            design.getPanelContainer(objVote.groupTo).add(design.get(getPropertyDraw(generateVoteProject)));

            design.get(objProject.groupTo).grid.constraints.fillVertical = 1.5;
            design.get(objExpert.groupTo).grid.constraints.fillVertical = 1.5;

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 0.3333);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, -1));

            design.get(getPropertyDraw(nameNative, objForesight)).setPreferredCharWidth(500);

            design.get(objVote.groupTo).toolbar.visible = false;

            design.addIntersection(design.get(getPropertyDraw(innovativeCommentExpertVote)), design.get(getPropertyDraw(completeCommentExpertVote)), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objProject.groupTo).grid.getContainer().setFixedSize(new Dimension(-1, 200));

            design.get(objFormalControl.groupTo).grid.constraints.fillHorizontal = 1;

            return design;
        }
    }

    private class StatusLogFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        public ObjectEntity objProject;
        private ObjectEntity objSession;

        private StatusLogFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "История изменений статуса");

            objProject = addSingleGroupObject(project, nameNativeProject, nameNativeClaimerProject);
            objProject.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSession = addSingleGroupObject(baseLM.session, baseLM.baseGroup);

            addPropertyDraw(logNameStatusProject, objProject, objSession);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(logStatusProject, objProject, objSession)));

            setEditType(PropertyEditType.READONLY);
        }
    }


    private class ApplicationFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objApplication;
        private ObjectEntity objCluster;
        private ObjectEntity objForesight;
        private ObjectEntity objVote;

        private ApplicationFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр заявок");

            objApplication = addSingleGroupObject(application, dateApplication, nameNativeClaimerApplication, nameProjectActionApplication, nameNativeProjectApplication,
                    officialNameStatusApplication, dateInStatusApplication, normalPeriodStatusApplication, quantityDaysToOverdueDateStatusApplication, langApplication, nameNativeShortAggregateClusterApplication, nameNativeShortFinalClusterApplication, emailClaimerApplication, daysClaimerApplication, nameRegulationsApplication, openCompleteApplicationProjectActionApplication, exportProjectDocumentsActionApplication);
            objCluster = addSingleGroupObject(2,"cluster", cluster, "Кластер");
                        addPropertyDraw(inApplicationCluster, objApplication, objCluster);
                        addPropertyDraw(objCluster, nameNative, nameForeign, nameNativeShort);
                        addPropertyDraw(nativeSubstantiationApplicationCluster, objApplication, objCluster);
                        addPropertyDraw(foreignSubstantiationApplicationCluster, objApplication, objCluster);
                        addPropertyDraw(numberCluster, objCluster);

            objForesight = addSingleGroupObject(3,"foresight", foresight, "Форсайт");
                        addPropertyDraw(inApplicationForesight, objApplication, objForesight);
                        addPropertyDraw(objForesight, sidForesight, nameNative, nameForeign, nameNativeShortClusterForesight, quantityInExpertForesight);

            objVote = addSingleGroupObject(4, "vote", vote, "Заседание", dateStartVote, dateEndVote, nameNativeClusterVote, nameProjectActionVote, requiredQuantityVote, percentNeededVote, openedVote, succeededVote, acceptedVote,
                                quantityDoneVote,
                                quantityInClusterVote, quantityInnovativeVote, quantityForeignVote,
                                quantityCompetitiveAdvantagesVote, quantityCommercePotentialVote, quantityCanBeImplementedVote,
                                quantityHaveExpertiseVote, quantityInternationalExperienceVote, quantityEnoughDocumentsVote,
                                openFileDecisionVote, decisionNoticedVote, dateDecisionNoticedVote);
                        objVote.groupTo.banClassView.addAll(BaseUtils.toList(ClassViewType.PANEL, ClassViewType.HIDE));

                        getPropertyDraw(percentNeededVote).forceViewType = ClassViewType.GRID;
                        showIf(this, percentNeededVote, isR2Application, objApplication);

                        setForceViewType(voteResultGroup, ClassViewType.GRID, objVote.groupTo);

                        showIf(this, new LCP[] {quantityInClusterVote, quantityInnovativeVote, quantityForeignVote},
                                isR1Application, objApplication);

                        showIf(this, new LCP[] {quantityCompetitiveAdvantagesVote, quantityCommercePotentialVote, quantityCanBeImplementedVote, quantityHaveExpertiseVote, quantityInternationalExperienceVote, quantityEnoughDocumentsVote},
                                isR2Application, objApplication);


            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inApplicationCluster, objApplication, objCluster)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inApplicationForesight, objApplication, objForesight)));
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(inactiveApplication, objApplication))));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(clusterForesight, objForesight), Compare.EQUALS, objCluster));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectApplication, objApplication), Compare.EQUALS, addPropertyObject(projectVote, objVote)));

            setEditType(PropertyEditType.READONLY);
            setEditType(openCompleteApplicationProjectActionApplication, PropertyEditType.EDITABLE);
            setEditType(exportProjectDocumentsActionApplication, PropertyEditType.EDITABLE);
            setEditType(openFileDecisionVote, PropertyEditType.EDITABLE);

            addDefaultOrder(dateApplication, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView foresightContainer = design.createContainer("Кластеры и форсайты");
            foresightContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            foresightContainer.add(design.getGroupObjectContainer(objCluster.groupTo));
            foresightContainer.add(design.getGroupObjectContainer(objForesight.groupTo));

            ContainerView expertizContainer = design.createContainer("Экспертиза по существу");
            expertizContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            expertizContainer.add(design.getGroupObjectContainer(objVote.groupTo));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objApplication.groupTo));
            specContainer.add(foresightContainer);
            specContainer.add(expertizContainer);
            specContainer.type = ContainerType.TABBED_PANE;

            design.get(objApplication.groupTo).grid.getContainer().setFixedSize(new Dimension(-1, 400));

            return design;
        }
    }

    private class ProjectClusterFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objProject;
        private ObjectEntity objCluster;

        private ProjectClusterFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Проект/Кластер");


            objProject = new ObjectEntity(genID(), project, "Проект");
            addPropertyDraw(objProject, dateProject, nameNativeProject, nameForeignProject, nameNativeShortFinalClusterProject, nameNativeClaimerProject, nameForeignClaimerProject, emailClaimerProject,
                    nameStatusProject, nameProjectActionProject, updateDateProject, autoGenerateProject, inactiveProject);

            objCluster = new ObjectEntity(genID(), cluster, "Кластер");
            addPropertyDraw(objCluster, nameNative, nameForeign);

            addPropertyDraw(objProject, objCluster, nativeSubstantiationProjectCluster, foreignSubstantiationProjectCluster);

            GroupObjectEntity gobjProjectCluster = new GroupObjectEntity(genID());
            gobjProjectCluster.add(objProject);
            gobjProjectCluster.add(objCluster);
            addGroupObject(gobjProjectCluster);
            gobjProjectCluster.setSingleClassView(ClassViewType.GRID);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inProjectCluster, objProject, objCluster)));

            setEditType(PropertyEditType.READONLY);
        }
    }

    private class ExpertForesightFormEntity extends FormEntity<SkolkovoBusinessLogics> {
            private ObjectEntity objExpert;
            private ObjectEntity objForesight;

        private ExpertForesightFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Эксперт/Форсайт");


            objExpert = new ObjectEntity(genID(), expert, "Эксперт");
            addPropertyDraw(objExpert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert);        // baseLM.selection,
            addPropertyDraw(objExpert, baseLM.email, disableExpert, nameLanguageExpert, dateAgreementExpert);

            objForesight = new ObjectEntity(genID(), foresight, "Форсайты");
            addPropertyDraw(objForesight, sidForesight, nameNative, nameNativeShortClusterForesight);
            addPropertyDraw(objExpert, objForesight, commentExpertForesight, inExpertForesight);

            GroupObjectEntity gobjExpertForesight = new GroupObjectEntity(genID());
            gobjExpertForesight.add(objExpert);
            gobjExpertForesight.add(objForesight);
            addGroupObject(gobjExpertForesight);
            gobjExpertForesight.setSingleClassView(ClassViewType.GRID);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertForesight, objExpert, objForesight)));
            RegularFilterGroupEntity inactiveFilterGroup = new RegularFilterGroupEntity(genID());
            inactiveFilterGroup.addFilter(new RegularFilterEntity(genID(),
                                new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(disableExpert, objExpert))),
                                "Только активные"), true);
                        addRegularFilterGroup(inactiveFilterGroup);

            setEditType(PropertyEditType.READONLY);
//            setEditType(baseLM.selection, PropertyEditType.EDITABLE);
        }
    }

    private class GlobalFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private GlobalFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Глобальные параметры");

            addPropertyDraw(new LP[]{baseLM.currentDate, requiredPeriod, overduePeriod,
                    requiredQuantity, requiredBusinessQuantity, useAllClusterExperts,
                    limitExperts, percentNeeded,
                    emailDocuments, emailPresident, emailClaimerFromAddress, emailForCertificates, emailIO, emailExperts, emailFondFC, emailForesightLC, emailFondTransferred, emailFondStartVote, emailBureauTranslation,
                    projectsImportLimit, importOnlyR2Projects, readInformation, importProjectSidsAction, showProjectsToImportAction, showProjectsReplaceToImportAction, importProjectsAction, importIPsExpertVoteAction,
                    rateExpert, emailLetterCertificatesExpertMonthYear, executiveLD, phoneExecutiveLD, mobileExecutiveLD, executiveIS});
        }
    }

    private class VoteFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objVote;
        private ObjectEntity objExpert;

        private VoteFormEntity(NavigatorElement parent, String sID, boolean restricted) {
            super(parent, sID, (!restricted) ? "Реестр заседаний" : "Результаты заседаний");

            objVote = addSingleGroupObject(vote, baseLM.objectClassName, nameNativeProjectVote, nameNativeClaimerVote, claimerEmailVote,
                    nameProjectActionVote, nameNativeClusterVote, dateStartVote, dateEndVote,
                    openedVote, succeededVote, acceptedVote, quantityDoneVote, quantityExclVote,
                    quantityInClusterVote, quantityInnovativeVote, quantityForeignVote,
                    quantityCompetitiveAdvantagesVote, quantityCommercePotentialVote, quantityCanBeImplementedVote,
                    quantityHaveExpertiseVote, quantityInternationalExperienceVote, quantityEnoughDocumentsVote);

            if (!restricted)
                addPropertyDraw(objVote, emailClosedVote, baseLM.delete);

            objExpert = addSingleGroupObject(expert);
            if (!restricted)
                addPropertyDraw(objExpert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert, baseLM.userLogin, baseLM.userPassword, baseLM.email);

            addPropertyDraw(objExpert, objVote, oldExpertVote, exclExpertVote);
            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            if (!restricted)
                addPropertyDraw(objExpert, objVote, allowedEmailLetterExpertVote);
            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            if (!restricted) {
                addPropertyDraw(objVote, voteStartFormVote);
                addPropertyDraw(objVote, voteProtocolFormVote);
                setForceViewType(voteStartFormVote, ClassViewType.PANEL);
                setForceViewType(voteProtocolFormVote, ClassViewType.PANEL);

            }

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertVote, objExpert, objVote)));

            RegularFilterGroupEntity voteFilterGroup = new RegularFilterGroupEntity(genID());
            voteFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(openedVote, objVote)),
                    "Открыто",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), !restricted);
            voteFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(succeededVote, objVote)),
                    "Состоялось",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), false);
            voteFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(acceptedVote, objVote)),
                    "Положительно",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), false);
            addRegularFilterGroup(voteFilterGroup);

            if (restricted) {
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(voteResultExpertVote, objExpert, objVote)));
                setEditType(PropertyEditType.READONLY);
            }
//            setEditType(PropertyEditType.READONLY, objVote.groupTo);
//            setEditType(PropertyEditType.READONLY, objExpert.groupTo);
//            setEditType(allowedEmailLetterExpertVote, PropertyEditType.EDITABLE);
//            setEditType(emailClosedVote, PropertyEditType.EDITABLE);

            setPageSize(0);

            addDefaultHintsIncrementTable(this);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 0.3333);

            PropertyDrawView className = design.get(getPropertyDraw(baseLM.objectClassName, objVote.groupTo));
            className.setPreferredCharWidth(20);
            className.setMaximumCharWidth(20);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, -1));

            design.get(objVote.groupTo).grid.getContainer().setFixedSize(new Dimension(-1, 300));

            return design;
        }
    }

    private class ExpertFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objExpert;
        private ObjectEntity objVote;
        private ObjectEntity objExtraCluster;
        private ObjectEntity objForesight;

        private ExpertFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр экспертов");

            objExpert = addSingleGroupObject(expert, baseLM.selection, baseLM.userFirstName, baseLM.userLastName, documentNameExpert,
                    baseLM.userLogin, baseLM.userPassword, baseLM.email, disableExpert,
                    nameNativeShortClusterExpert, nameLanguageExpert,
                    dateAgreementExpert, nameCountryExpert, nameCurrencyExpert,
                    isScientificExpert, isTechnicalExpert, isBusinessExpert, expertiseExpert, grantExpert, profileBlockedExpert, profileUpdateDateExpert,
                    quantityInClusterExpert, quantityInForesightExpert,
                    expertResultGroup, sidExpert, generateSIDExpert, baseLM.generateLoginPassword, emailAuthExpert, emailAuthProfileExpert, emailReminderProfileExpert);
            addObjectActions(this, objExpert);

            addPropertyDraw(objExpert, commentExpertiseGroup);
            setForceViewType(commentExpertiseGroup, ClassViewType.PANEL);

            objVote = addSingleGroupObject(vote, nameNativeProjectVote, dateStartVote, dateEndVote, openedVote, succeededVote, quantityDoneVote, revisionVote);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            addPropertyDraw(objExpert, objVote, allowedEmailLetterExpertVote);
            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            objExtraCluster = addSingleGroupObject(cluster, "Дополнительные кластеры");
            addPropertyDraw(extraClusterExpert, objExtraCluster, objExpert);
            addPropertyDraw(objExtraCluster, nameNative, nameForeign, sidCluster);

            objForesight = addSingleGroupObject(foresight, "Форсайты");
            addPropertyDraw(objForesight, sidForesight, nameNative, nameForeign, nameNativeShortClusterForesight, quantityInExpertForesight);
            addPropertyDraw(objExpert, objForesight, inExpertForesight, commentExpertForesight);

            getPropertyDraw(commentExpertForesight).forceViewType = ClassViewType.PANEL;

            RegularFilterGroupEntity inactiveFilterGroup = new RegularFilterGroupEntity(genID());
            inactiveFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(disableExpert, objExpert))),
                    "Только активные"), true);
            addRegularFilterGroup(inactiveFilterGroup);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));

            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(primClusterExpert, objExtraCluster, objExpert))));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(clusterInExpertForesight, objExpert, objForesight)));
//            setEditType(PropertyEditType.READONLY, objVote.groupTo);
//            setEditType(allowedEmailLetterExpertVote, PropertyEditType.EDITABLE);

//            setPageSize(0);

            addPropertyDraw(new LP[] {exportExpertsAction, exportExpertsEscapeAction});
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.setPanelLabelAbove(voteResultCommentGroup, true);
            design.setConstraintsFillHorizontal(voteResultCommentGroup, 0.333);

            design.setPreferredSize(voteResultCheckGroup, new Dimension(60, -1));

            design.get(objExpert.groupTo).grid.getContainer().setFixedSize(new Dimension(-1, 300));

            design.setPanelLabelAbove(commentExpertiseGroup, true);
            design.setConstraintsFillHorizontal(commentExpertiseGroup, 0.333);

            ContainerView foresightContainer = design.createContainer("Области специализации");
            foresightContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            foresightContainer.add(design.getGroupPropertyContainer(objExpert.groupTo, commentExpertiseGroup));
            foresightContainer.add(design.getGroupObjectContainer(objForesight.groupTo));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objExpert.groupTo));
            specContainer.add(design.getGroupObjectContainer(objVote.groupTo));
            specContainer.add(design.getGroupObjectContainer(objExtraCluster.groupTo));
            specContainer.add(foresightContainer);
            specContainer.type = ContainerType.TABBED_PANE;

            design.setPanelLabelAbove(design.get(getPropertyDraw(commentExpertForesight)), true);

            design.get(getPropertyDraw(commentExpertForesight)).constraints.fillHorizontal = 1;

            return design;
        }
    }

    private class ExpertStatsFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objExpert;
        private ObjectEntity objWeek;

        private ExpertStatsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Статистика экспертов");

            objWeek = addSingleGroupObject(1, "week", IntegerClass.instance, "Неделя");

            objExpert = addSingleGroupObject(2, "expert", expert, baseLM.selection, disableExpert, nameNativeShortClusterExpert, baseLM.userFirstName, baseLM.userLastName, sidExpert, baseLM.email);

            PropertyDrawEntity quantity = addPropertyDraw(quantityNewExpertWeek, objExpert, objWeek);
            quantity.columnGroupObjects.add(objWeek.groupTo);
            quantity.propertyCaption = addPropertyObject(baseLM.objectValue.getLP(IntegerClass.instance), objWeek);

            addPropertyDraw(objExpert, quantityTotalExpert);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityNewWeek, objWeek)));

            RegularFilterGroupEntity inactiveFilterGroup = new RegularFilterGroupEntity(genID());
            inactiveFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(disableExpert, objExpert))),
                    "Только активные"));
            addRegularFilterGroup(inactiveFilterGroup);

            addDefaultOrder(nameNativeShortClusterExpert, true);
            addDefaultOrder(baseLM.userLastName, true);

            setEditType(PropertyEditType.READONLY);
            setPageSize(0);
        }
    }

    private class VoteExpertFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objExpert;
        private ObjectEntity objVote;

        private VoteExpertFormEntity(NavigatorElement parent, String sID, boolean restricted) {
            super(parent, sID, (!restricted) ? "Реестр голосований" : "Результаты голосований");

            objVote = new ObjectEntity(genID(), vote, "Заседание");
            addPropertyDraw(objVote, nameNativeProjectVote, nameNativeClaimerVote, dateStartVote, dateEndVote, openedVote, succeededVote, acceptedVote, revisionVote);

            objExpert = new ObjectEntity(genID(), expert, "Эксперт");
            addPropertyDraw(objExpert, nameNativeClusterExpert, nameLanguageExpert);

            if (!restricted)
                addPropertyDraw(objExpert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert, baseLM.email);

            if (!restricted)
                addPropertyDraw(objExpert, objVote, allowedEmailLetterExpertVote);

            addPropertyDraw(objExpert, objVote, exclExpertVote);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            if (!restricted)
                addPropertyDraw(expertResultGroup, true, objExpert);
            setForceViewType(voteResultCommentGroup, ClassViewType.PANEL);

            addPropertyDraw(voteResultLengthGroup, true, objExpert, objVote);

            GroupObjectEntity gobjVoteExpert = new GroupObjectEntity(genID());
            gobjVoteExpert.add(objVote);
            gobjVoteExpert.add(objExpert);
            addGroupObject(gobjVoteExpert);
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
                    new CompareFilterEntity(addPropertyObject(doneCorExpertVote, objExpert, objVote), Compare.EQUALS, addPropertyObject(baseLM.vtrue)),
                    "Только проголосовавшие",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupDoneExpertVote);

            RegularFilterGroupEntity filterGroupExpertVote = new RegularFilterGroupEntity(genID());
            filterGroupExpertVote.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(voteResultExpertVote, objExpert, objVote))),
                    "Только не принявшие участие",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroupExpertVote);

            addDefaultHintsIncrementTable(this);
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

            ObjectEntity objDocumentTemplate = addSingleGroupObject(documentTemplate, "Шаблон", baseLM.name);
            addObjectActions(this, objDocumentTemplate);

            ObjectEntity objDocumentTemplateDetail = addSingleGroupObject(documentTemplateDetail, nameTypeDocument, nameLanguageDocument);
            addObjectActions(this, objDocumentTemplateDetail);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(documentTemplateDocumentTemplateDetail, objDocumentTemplateDetail), Compare.EQUALS, objDocumentTemplate));
        }
    }

    private class ClaimerFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        public ObjectEntity objClaimer;

        private ClaimerFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Реестр заявителей");

            objClaimer = addSingleGroupObject(claimer, "Заявитель", claimerInformationGroup, contactGroup, editClaimer);
        }
    }


    private class ClaimerFullFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        public ObjectEntity objClaimer;

        private ClaimerFullFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Заявители");

            objClaimer = addSingleGroupObject(claimer, "Заявитель", claimerInformationGroup, contactGroup, documentGroup, legalDataGroup);
            objClaimer.groupTo.setSingleClassView(ClassViewType.PANEL);

            editClaimer = addMFAProp(actionGroup, "Редактировать", this, new ObjectEntity[]{objClaimer}, false);
            editClaimer.setImage("form.png");
            editClaimerProject = addJoinAProp(actionGroup, "Юр.лицо", editClaimer, claimerProject, 1);
        }
    }

    private class ClusterForesightFormEntity extends FormEntity<SkolkovoBusinessLogics>{
        private ObjectEntity objCluster;
        private ObjectEntity objForesight;
        private ObjectEntity objExpert;

        private ClusterForesightFormEntity(NavigatorElement parent, String sID){
            super(parent, sID, "Кластеры");

            objCluster = addSingleGroupObject(1, "Кластер", cluster);
            addPropertyDraw(objCluster, nameNative, nameForeign, baseLM.email, sidCluster, inTestCluster);
            addObjectActions(this, objCluster);

            objForesight = addSingleGroupObject(2, "Форсайт", foresight);
            addPropertyDraw(objForesight, sidForesight, isRootForesight, nameNative, nameForeign, nameNativeShortClusterForesight, quantityInExpertForesight);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(clusterForesight, objForesight), Compare.EQUALS, objCluster));
            addObjectActions(this, objForesight);

            objExpert = addSingleGroupObject(3, "Эксперт", expert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert, baseLM.email);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertForesight, objExpert, objForesight)));

            includeProjectClusterForesight = addJoinAProp(actionGroup, "Подключить", editClaimer, claimerProject, 1);

            setPageSize(0);
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
            addGroupObject(gobjLanguageDocumentType);

            addPropertyDraw(objLanguage, baseLM.name);
            addPropertyDraw(objDocumentType, baseLM.name);
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
            addGroupObject(gobjExpertVote);
            gobjExpertVote.initClassView = ClassViewType.PANEL;

            addPropertyDraw(baseLM.webHost, gobjExpertVote);
            addPropertyDraw(requiredPeriod, gobjExpertVote);
            addPropertyDraw(objExpert, baseLM.name, documentNameExpert, sidExpert, isForeignExpert, localeExpert);
            addPropertyDraw(objVote, nameNativeClaimerVote, nameForeignClaimerVote, nameNativeProjectVote, nameForeignProjectVote, isR1ProjectVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));

            //!!!
            //dateStartVote = addJProp(baseGroup, "dateStartVote", "Дата начала", baseLM.and1, date, 1, is(vote), 1);
            //LCP isDocumentUnique = addJProp(baseGroup,"isDocumentUnique", "уникальность док-та", baseLM.and1, languageDocument, is(), objDocument);

            objDocument = addSingleGroupObject(8, "document", document, fileDocument);
            addPropertyDraw(nameDocument, objDocument).setSID("docName");
            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectDocument, objDocument), Compare.EQUALS, addPropertyObject(projectVote, objVote)));
            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(inDocumentExpert, objDocument, objExpert)),
                    new NotNullFilterEntity(addPropertyObject(inDefaultDocumentExpert, objDocument, objExpert))));

            addInlineEAForm(emailLetterExpertVoteEA, this, objExpert, 1, objVote, 2);

            setEditType(PropertyEditType.READONLY);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(gobjExpertVote);
        }
    }

    private class ExpertAuthFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту о логине
        private ObjectEntity objExpert;

        private ExpertAuthFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Аутентификация эксперта", true);

            objExpert = addSingleGroupObject(1, "expert", expert, baseLM.userLogin, baseLM.userPassword, baseLM.name, documentNameExpert, sidExpert, isForeignExpert);
            objExpert.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailAuthExpertEA, this, objExpert, 1);

            setEditType(PropertyEditType.READONLY);
        }
    }

    private class ExpertAuthProfileFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту о логине
        private ObjectEntity objExpert;

        private ExpertAuthProfileFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Уведомление о заполнении профиля", true);

            objExpert = addSingleGroupObject(1, "expert", expert, baseLM.userLogin, baseLM.userPassword, baseLM.name, documentNameExpert, sidExpert, isForeignExpert, localeExpert);
            objExpert.groupTo.initClassView = ClassViewType.PANEL;

            addPropertyDraw(baseLM.webHost, objExpert.groupTo);

            addInlineEAForm(emailAuthProfileExpertEA, this, objExpert, 1);

            setEditType(PropertyEditType.READONLY);
        }
    }

    private class ExpertReminderProfileFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо-напоминание эксперту о заполнении профиля
        private ObjectEntity objExpert;

        private ExpertReminderProfileFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Напоминание о заполнении профиля", true);

            objExpert = addSingleGroupObject(1, "expert", expert, baseLM.userLogin, baseLM.userPassword, baseLM.name, documentNameExpert, sidExpert, isForeignExpert, localeExpert);
            objExpert.groupTo.initClassView = ClassViewType.PANEL;

            addPropertyDraw(baseLM.webHost, objExpert.groupTo);

            addInlineEAForm(emailReminderProfileExpertEA, this, objExpert, 1);

            setEditType(PropertyEditType.READONLY);
        }
    }

     private class ClusterExpertProfileFormEntity extends FormEntity<SkolkovoBusinessLogics>{
        private ObjectEntity objCluster;
        private ObjectEntity objExpert;
        private ObjectEntity objForesight;

        private ClusterExpertProfileFormEntity(NavigatorElement parent, String sID){
            super(parent, sID, "Информация о заполненном профайле");

            objCluster = addSingleGroupObject(1, "cluster", cluster, "Кластер");
            addPropertyDraw(objCluster, nameNative, nameForeign);
            objCluster.groupTo.initClassView = ClassViewType.PANEL;


            objExpert = addSingleGroupObject(2, "expert", expert, "Эксперт", baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert, baseLM.email, baseLM.userPassword, nameNativeClusterExpert);
            addPropertyDraw(objExpert, commentExpertiseGroup);
            addPropertyDraw(objExpert, isScientificExpert, isTechnicalExpert, isBusinessExpert, expertiseExpert, grantExpert, profileBlockedExpert, profileUpdateDateExpert);

            objForesight = addSingleGroupObject(3, "foresight", foresight, "Области специализации");
            addPropertyDraw(objForesight, sidForesight, isRootForesight, nameNative, nameForeign, nameNativeShortClusterForesight);
            addPropertyDraw(objExpert, objForesight, commentExpertForesight, inExpertForesight);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(clusterExpert, objExpert), Compare.EQUALS, objCluster));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(clusterForesight, objForesight), Compare.EQUALS, objCluster));
            addObjectActions(this, objForesight);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inExpertForesight, objExpert, objForesight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(profileUpdateDateExpert, objExpert)));
//            includeProjectClusterForesight = addJProp(actionGroup, true, "Подключить", editClaimer, claimerProject, 1);

            setPageSize(0);
        }
    }


    private class ConferenceExpertLetterFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private GroupObjectEntity gobjConferenceExpert;
        private ObjectEntity objConference;
        private ObjectEntity objExpert;

        private ConferenceExpertLetterFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Письмо о конференции", true);

            gobjConferenceExpert = new GroupObjectEntity(2, "conferenceExpert");
            objExpert = new ObjectEntity(2, "expert", expert, "Эксперт");
            objConference = new ObjectEntity(3, "conference", expertConference, "Конференция");
            gobjConferenceExpert.add(objExpert);
            gobjConferenceExpert.add(objConference);
            addGroupObject(gobjConferenceExpert);
            gobjConferenceExpert.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objConference, textConference);
            addPropertyDraw(objExpert, baseLM.name, sidExpert);

            addPropertyDraw(baseLM.webHost, gobjConferenceExpert);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inConferenceExpert, objConference, objExpert)));

            addInlineEAForm(emailConferenceExpertEA, this, objConference, 1, objExpert, 2);

            setEditType(PropertyEditType.READONLY);
        }
    }

    private class VoteStartFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objVote;

        private ObjectEntity objExpert;
        private ObjectEntity objOldExpert;
        private ObjectEntity objPrevVote;

        private VoteStartFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Созыв заседания", true);

            objVote = addSingleGroupObject(1, "vote", vote, baseLM.date, dateProjectVote, nameNativeClaimerVote, nameNativeProjectVote, nameAblateClaimerVote, prevDateStartVote, prevDateVote, quantityInVote, quantityInOldVote, countPrevVote, isStatusVote, isR1ProjectVote, nameNativeClusterVote, nameMaxForesightVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            objExpert = addSingleGroupObject(2, "expert", expert, baseLM.userLastName, baseLM.userFirstName, documentNameExpert, sidExpert);
            addPropertyDraw(numberNewExpertVote, objExpert, objVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));

            objOldExpert = addSingleGroupObject(3, "oldexpert", expert, baseLM.userLastName, baseLM.userFirstName, documentNameExpert, sidExpert);
            addPropertyDraw(numberOldExpertVote, objOldExpert, objVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inOldExpertVote, objOldExpert, objVote)));

            objPrevVote = addSingleGroupObject(4, "prevVote", vote, dateEndVote, isR1ProjectVote, isStatusVote);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(isPrevVoteVote, objPrevVote, objVote)));

            addAttachEAForm(emailStartVoteEA, this, AttachmentFormat.PDF, objVote, 1);
            addAttachEAForm(emailStartVoteEA, this, AttachmentFormat.DOCX, objVote, 1);
            addAttachEAFormNameFullyMapped(emailClosedVoteEA, this, AttachmentFormat.PDF, emailStartHeaderVote, objVote, 1);
            addAttachEAFormNameFullyMapped(emailClosedVoteEA, this, AttachmentFormat.DOCX, emailStartHeaderVote, objVote, 1);

            addDefaultHintsIncrementTable(this);

            voteStartFormVote = addFAProp("Созыв заседания", this, objVote);
            addPropertyDraw(readInformation).toDraw = objVote.groupTo;

            setEditType(PropertyEditType.READONLY);
//            setEditType(readInformation, PropertyEditType.EDITABLE);
        }
        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(objVote.groupTo);
        }
    }

    private class VoteProtocolFormEntity extends FormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objVote;
        private ObjectEntity objPrevVote;
        private ObjectEntity objExpert;

        private VoteProtocolFormEntity(NavigatorElement parent, String sID, boolean simple) {
            super(parent, sID, simple ? "Протокол заседания (без бюллетеней)" : "Протокол заседания", true);

            objVote = addSingleGroupObject(1, "vote", vote, dateProjectVote, baseLM.date, dateEndVote, nameNativeProjectVote, nameNativeClaimerVote, nameNativeClusterVote,
                    quantityInVote, quantityRepliedVote, quantityDoneVote, quantityDoneNewVote, quantityDoneOldVote, quantityRefusedVote, quantityConnectedVote, succeededVote, acceptedVote,
                    quantityInClusterVote, acceptedInClusterVote, quantityInnovativeVote, acceptedInnovativeVote, quantityForeignVote,
                    quantityCompetitiveAdvantagesVote, acceptedCompetitiveAdvantagesVote, quantityCommercePotentialVote, acceptedCommercePotentialVote, quantityCanBeImplementedVote, acceptedCanBeImplementedVote,
                    quantityHaveExpertiseVote, acceptedHaveExpertiseVote, quantityInternationalExperienceVote, acceptedInternationalExperienceVote, quantityEnoughDocumentsVote, acceptedEnoughDocumentsVote,
                    acceptedForeignVote, prevDateStartVote, prevDateVote, countPrevVote, revisionVote, isStatusVote, isR1ProjectVote, nameMaxForesightVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            objPrevVote = addSingleGroupObject(5, "prevVote", vote, dateStartVote, isStatusVote, isR1ProjectVote);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(closedVote, objVote)));

            if (!simple) {
                objExpert = addSingleGroupObject(12, "expert", expert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert);

                addPropertyDraw(voteResultGroup, true, objExpert, objVote);

                addPropertyDraw(objExpert, objVote, connectedExpertVote, competitiveAdvantagesExpertVote, commercePotentialExpertVote, canBeImplementedExpertVote, haveExpertiseExpertVote, internationalExperienceExpertVote, enoughDocumentsExpertVote, commentCompetitiveAdvantagesExpertVote, commentCommercePotentialExpertVote, commentCanBeImplementedExpertVote, commentHaveExpertiseExpertVote, commentInternationalExperienceExpertVote, commentEnoughDocumentsExpertVote);
                addPropertyDraw(objExpert, objVote, quantityCompetitiveAdvantagesVote, quantityCommercePotentialVote, quantityCanBeImplementedVote, quantityHaveExpertiseVote, quantityInternationalExperienceVote, quantityEnoughDocumentsVote, acceptedCompetitiveAdvantagesVote, acceptedCommercePotentialVote, acceptedCanBeImplementedVote, acceptedHaveExpertiseVote, acceptedInternationalExperienceVote, acceptedEnoughDocumentsVote);

                addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(doneCorExpertVote, objExpert, objVote)),
                        new NotNullFilterEntity(addPropertyObject(connectedExpertVote, objExpert, objVote))));
            }

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(isPrevVoteVote, objPrevVote, objVote)));

            addAttachEAForm(emailProtocolVoteEA, this, AttachmentFormat.PDF, objVote, 1);
            addAttachEAForm(emailProtocolVoteEA, this, AttachmentFormat.DOCX, objVote, 1);
            addAttachEAFormNameFullyMapped(emailClosedVoteEA, this, AttachmentFormat.PDF, emailProtocolHeaderVote, objVote, 1);
            addAttachEAFormNameFullyMapped(emailClosedVoteEA, this, AttachmentFormat.DOCX, emailProtocolHeaderVote, objVote, 1);

            addDefaultHintsIncrementTable(this);

            voteProtocolFormVote = addFAProp("Протокол заседания", this, objVote);
            addPropertyDraw(readInformation).toDraw = objVote.groupTo;
            setEditType(PropertyEditType.READONLY);
//            setEditType(readInformation, PropertyEditType.EDITABLE);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(objVote.groupTo);
        }
    }

    private class ExpertProtocolFormEntity extends DateIntervalFormEntity<SkolkovoBusinessLogics> { // письмо эксперту

        private ObjectEntity objExpert;

        private ObjectEntity objVoteHeader;
        private ObjectEntity objVote;

        private ExpertProtocolFormEntity(NavigatorElement parent, String sID) {
            super(baseLM, parent, sID, "Протокол голосования экспертов", true);

            getPropertyDraw(baseLM.objectValue, objDateFrom).setSID("dateFrom");

            // так делать неправильно в общем случае, поскольку getPropertyDraw ищет по groupObject, а не object

//            addPropertyDraw(objDateTo, baseLM.objectValue);
            getPropertyDraw(baseLM.objectValue, objDateTo).setSID("dateTo");

            objExpert = addSingleGroupObject(4, "expert", expert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert, nameNativeClusterExpert);
            objExpert.groupTo.initClassView = ClassViewType.PANEL;

            addPropertyDraw(objExpert, objDateFrom, objDateTo, quantityDoneExpertDateFromDateTo, quantityInExpertDateFromDateTo);

            objVoteHeader = addSingleGroupObject(5, "voteHeader", vote);

            objVote = addSingleGroupObject(6, "vote", vote, dateProjectVote, baseLM.date, dateEndVote, nameNativeProjectVote, nameNativeClaimerVote, nameNativeClusterVote, revisionVote);

            addPropertyDraw(nameNativeClaimerVote, objVoteHeader).setSID("nameNativeClaimerVoteHeader");
            addPropertyDraw(nameNativeProjectVote, objVoteHeader);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            addPropertyDraw(objExpert, objVote, competitiveAdvantagesExpertVote, commercePotentialExpertVote, canBeImplementedExpertVote, haveExpertiseExpertVote, internationalExperienceExpertVote, enoughDocumentsExpertVote, commentCompetitiveAdvantagesExpertVote, commentCommercePotentialExpertVote, commentCanBeImplementedExpertVote, commentHaveExpertiseExpertVote, commentInternationalExperienceExpertVote, commentEnoughDocumentsExpertVote);

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

            setEditType(PropertyEditType.READONLY);
            setEditType(PropertyEditType.EDITABLE, objDateFrom.groupTo);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.setPanelLabelAbove(voteResultCommentGroup, true);
            return design;
        }
    }

    private class ExpertProtocolClusterFormEntity extends DateIntervalFormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objCluster;
        private ObjectEntity objExpert;

        private ObjectEntity objVoteHeader;
        private ObjectEntity objVote;

        private ExpertProtocolClusterFormEntity(NavigatorElement parent, String sID) {
            super(baseLM, parent, sID, "Протокол голосования экспертов по кластеру", true);

            getPropertyDraw(baseLM.objectValue, objDateFrom).setSID("dateFrom");

            // так делать неправильно в общем случае, поскольку getPropertyDraw ищет по groupObject, а не object

//            addPropertyDraw(objDateTo, baseLM.objectValue);
            getPropertyDraw(baseLM.objectValue, objDateTo).setSID("dateTo");

            objCluster = addSingleGroupObject(4, "Кластер", cluster);
            addPropertyDraw(objCluster, nameNative, nameForeign);
            objCluster.groupTo.initClassView = ClassViewType.PANEL;

            objExpert = addSingleGroupObject(5, "expert", expert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert, nameNativeClusterExpert);
//            objExpert.groupTo.initClassView = ClassViewType.PANEL;

//            addPropertyDraw(objExpert, objDateFrom, objDateTo, quantityDoneExpertDateFromDateTo, quantityInExpertDateFromDateTo);
            addPropertyDraw(objCluster, objExpert, objDateFrom, objDateTo, quantityDoneClusterExpertDateFromDateTo, quantityInClusterExpertDateFromDateTo);

            objVoteHeader = addSingleGroupObject(6, "voteHeader", vote);

            objVote = addSingleGroupObject(7, "vote", vote, dateProjectVote, baseLM.date, dateEndVote, nameNativeProjectVote, nameNativeClaimerVote, nameNativeClusterVote, revisionVote);

            addPropertyDraw(nameNativeClaimerVote, objVoteHeader).setSID("nameNativeClaimerVoteHeader");
            addPropertyDraw(nameNativeProjectVote, objVoteHeader);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            addPropertyDraw(objExpert, objVote, competitiveAdvantagesExpertVote, commercePotentialExpertVote, canBeImplementedExpertVote, haveExpertiseExpertVote, internationalExperienceExpertVote, enoughDocumentsExpertVote, commentCompetitiveAdvantagesExpertVote, commentCommercePotentialExpertVote, commentCanBeImplementedExpertVote, commentHaveExpertiseExpertVote, commentInternationalExperienceExpertVote, commentEnoughDocumentsExpertVote);

            addPropertyDraw(connectedExpertVote, objExpert, objVote);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inClusterExpert, objCluster, objExpert)));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVoteHeader), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVoteHeader), Compare.LESS_EQUALS, objDateTo));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(doneNewExpertVote, objExpert, objVoteHeader)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(clusterVote, objVoteHeader), Compare.EQUALS, objCluster));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVote), Compare.GREATER_EQUALS, objDateFrom));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateExpertVote, objExpert, objVote), Compare.LESS_EQUALS, objDateTo));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(doneNewExpertVote, objExpert, objVote)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(clusterVote, objVote), Compare.EQUALS, objCluster));


            RegularFilterGroupEntity filterGroupExpertVote = new RegularFilterGroupEntity(genID());
            filterGroupExpertVote.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityDoneClusterExpertDateFromDateTo, objCluster, objExpert, objDateFrom, objDateTo)),
                    "Голосовавшие",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)), true);
            filterGroupExpertVote.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityInClusterExpertDateFromDateTo, objCluster, objExpert, objDateFrom, objDateTo)),
                    "Учавствовавшие",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroupExpertVote);

            setEditType(PropertyEditType.READONLY);
            setEditType(PropertyEditType.EDITABLE, objDateFrom.groupTo);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.setPanelLabelAbove(voteResultCommentGroup, true);
            return design;
        }
    }

    private class VoteClaimerFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private VoteClaimerFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objVote = addSingleGroupObject(1, "vote", vote, "Заседание", nameNativeClusterVote, prevDateVote, nameNativePrevClusterVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailClaimerVoteEA, this, objVote, 1);
        }
    }
     private class VoteFondFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private VoteFondFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objVote = addSingleGroupObject(1, "vote", vote, "Заседание", openedVote, nameNativeProjectVote, nameNativeClaimerVote, claimerEmailVote, nameNativeClusterVote, prevDateVote, nameNativePrevClusterVote);
            objVote.groupTo.setSingleClassView(ClassViewType.PANEL);

            addInlineEAForm(emailFondVoteEA, this, objVote, 1);
        }
    }


     private class FormalControlClaimerFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objFormalControl;

        private FormalControlClaimerFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objFormalControl = addSingleGroupObject(1, "formalControl", formalControl, "Формальная экспертиза", dateProjectFormalControl, sidResultFormalControl, sidStatusProjectFormalControl, nameResultFormalControl, nameNativeClusterProjectFormalControl, nameProjectActionFormalControl, commentFormalControl,
                    dateTimeFormalControl, overdueDateFormalControl, dateResultNoticedFormalControl, isPreliminaryAndStatusProjectFormalControl);
            objFormalControl.groupTo.initClassView = ClassViewType.PANEL;
            addPropertyDraw(executiveLD, objFormalControl).toDraw = objFormalControl.groupTo;
            addPropertyDraw(phoneExecutiveLD, objFormalControl).toDraw = objFormalControl.groupTo;
            addPropertyDraw(mobileExecutiveLD, objFormalControl).toDraw = objFormalControl.groupTo;
            addInlineEAForm(emailClaimerFormalControlEA, this, objFormalControl, 1);
        }
    }

    private class FormalControlFondFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objFormalControl;

        private FormalControlFondFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objFormalControl = addSingleGroupObject(1, "formalControl", formalControl, "Формальная экспертиза", nameNativeClaimerFormalControl, nameNativeProjectFormalControl, claimerEmailFormalControl, dateProjectFormalControl, sidResultFormalControl, sidStatusProjectFormalControl, nameResultFormalControl, nameNativeClusterProjectFormalControl, nameProjectActionFormalControl, commentFormalControl,
                    dateTimeFormalControl, overdueDateFormalControl, dateResultNoticedFormalControl, isStatusFormalControl, isPreliminaryAndStatusProjectFormalControl, nameNativeUnionManagerFormalControl);
            objFormalControl.groupTo.initClassView = ClassViewType.PANEL;
            addPropertyDraw(executiveLD, objFormalControl).toDraw = objFormalControl.groupTo;
            addPropertyDraw(phoneExecutiveLD, objFormalControl).toDraw = objFormalControl.groupTo;
            addPropertyDraw(mobileExecutiveLD, objFormalControl).toDraw = objFormalControl.groupTo;
            addPropertyDraw(executiveIS, objFormalControl).toDraw = objFormalControl.groupTo;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(sidResultFormalControl, objFormalControl)));
//            setEditType(PropertyEditType.READONLY);
            addInlineEAForm(emailFondFormalControlEA, this, objFormalControl, 1);
            addInlineEAForm(emailFondFormalControlEA, projectCompleteR2Native, projectCompleteR2Native.objProject, projectFormalControl, 1);
            addInlineEAForm(emailFondFormalControlEA, projectCompleteR2Foreign, projectCompleteR2Foreign.objProject, projectFormalControl, 1);
        }
    }

     private class LegalCheckClaimerFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objLegalCheck;

        private LegalCheckClaimerFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objLegalCheck = addSingleGroupObject(1, "legalCheck", legalCheck, "Юридическая проверка", dateProjectLegalCheck, sidResultLegalCheck, sidStatusProjectLegalCheck, nameResultLegalCheck, nameNativeClusterProjectLegalCheck, nameProjectActionLegalCheck, commentLegalCheck,
                    overdueDateLegalCheck, dateTimeLegalCheck, dateResultNoticedLegalCheck, isPreliminaryAndStatusProjectLegalCheck, isR1LegalCheck, changeLegalCheck, noticedChangeLegalCheck, dateChangeLegalCheck, needTranslationLegalChek, needsToBeTranslatedToRussianLegalChek, nameMaxForesightLegalChek);
            addPropertyDraw(executiveLD, objLegalCheck).toDraw = objLegalCheck.groupTo;
            addPropertyDraw(phoneExecutiveLD, objLegalCheck).toDraw = objLegalCheck.groupTo;
            addPropertyDraw(mobileExecutiveLD, objLegalCheck).toDraw = objLegalCheck.groupTo;
            objLegalCheck.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailClaimerLegalCheckEA, this, objLegalCheck, 1);

            addDefaultHintsIncrementTable(this);
        }
    }

    private class LegalCheckClaimerChangeFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objLegalCheck;

        private LegalCheckClaimerChangeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objLegalCheck = addSingleGroupObject(1, "legalCheck", legalCheck, "Юридическая проверка", dateProjectLegalCheck, sidResultLegalCheck, sidStatusProjectLegalCheck, nameResultLegalCheck, nameNativeClusterProjectLegalCheck, nameProjectActionLegalCheck, commentLegalCheck,
                    overdueDateLegalCheck, dateTimeLegalCheck, dateResultNoticedLegalCheck, isPreliminaryAndStatusProjectLegalCheck, isR1LegalCheck, changeLegalCheck, noticedChangeLegalCheck, dateChangeLegalCheck);
            addPropertyDraw(executiveLD, objLegalCheck).toDraw = objLegalCheck.groupTo;
            addPropertyDraw(phoneExecutiveLD, objLegalCheck).toDraw = objLegalCheck.groupTo;
            addPropertyDraw(mobileExecutiveLD, objLegalCheck).toDraw = objLegalCheck.groupTo;
            objLegalCheck.groupTo.initClassView = ClassViewType.PANEL;

            addInlineEAForm(emailClaimerChangeLegalCheckEA, this, objLegalCheck, 1);

            addDefaultHintsIncrementTable(this);
        }
    }

    private class ConferenceFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objConference;
        private ObjectEntity objExpert;

        private ConferenceFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Конференции экспертов");

            objConference = addSingleGroupObject(expertConference, textConference, baseLM.date, baseLM.name, confirmedConference, rejectedConference);
            addObjectActions(this, objConference);

            objExpert = addSingleGroupObject(expert);
            addPropertyDraw(objConference, objExpert, inConferenceExpert);
            addPropertyDraw(objExpert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert, baseLM.userLogin, baseLM.userPassword, baseLM.email,
                    nameLanguageExpert, nameCountryExpert, disableExpert, nameNativeShortClusterExpert, nameNativeShortAggregateClusterExpert);
            addPropertyDraw(objConference, objExpert, nameResultConferenceExpert, emailConferenceExpert);
            setForceViewType(textConference, ClassViewType.PANEL);

            RegularFilterGroupEntity inFilterGroup = new RegularFilterGroupEntity(genID());
            inFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(inConferenceExpert, objConference, objExpert)),
                    "В конференции",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(inFilterGroup);

            RegularFilterGroupEntity inactiveFilterGroup = new RegularFilterGroupEntity(genID());
            inactiveFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(disableExpert, objExpert))),
                    "Акт.",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), true);
            inactiveFilterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(disableExpert, objExpert)),
                    "Не акт.",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));

            addRegularFilterGroup(inactiveFilterGroup);

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            ContainerView textContainer = design.createContainer("Содержание");
            textContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            textContainer.add(design.get(getPropertyDraw(textConference, objConference)));
            textContainer.constraints.fillHorizontal = 1.0;
            textContainer.constraints.fillVertical = 1.0;

            PropertyDrawView textView = design.get(getPropertyDraw(textConference, objConference));
            textView.constraints.fillHorizontal = 1.0;
            textView.preferredSize = new Dimension(-1, 300);
            textView.panelLabelAbove = true;

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objConference.groupTo));
            specContainer.add(design.getGroupObjectContainer(objExpert.groupTo));
            specContainer.add(textContainer);
            specContainer.type = ContainerType.TABBED_PANE;

            return design;
        }

    }

    private class NoticeRejectedFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;
        private ObjectEntity objExpert;

        private NoticeRejectedFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objVote = addSingleGroupObject(1, "vote", vote, "Заседание", nameNativeClusterVote, prevDateVote, nameNativePrevClusterVote, nameNativeFinalClusterProjectVote, isLastClusterVote, dateProjectVote, nameNativeProjectVote,
                    quantityDoneVote, quantityInClusterVote, acceptedInClusterVote, quantityInnovativeVote, acceptedInnovativeVote, quantityForeignVote, acceptedForeignVote, isR1ProjectVote, isStatusVote,
                    quantityCompetitiveAdvantagesVote, acceptedCompetitiveAdvantagesVote, quantityCommercePotentialVote, acceptedCommercePotentialVote, quantityCanBeImplementedVote, acceptedCanBeImplementedVote,
                    quantityHaveExpertiseVote, acceptedHaveExpertiseVote, quantityInternationalExperienceVote, acceptedInternationalExperienceVote, quantityEnoughDocumentsVote, acceptedEnoughDocumentsVote, nameMaxForesightVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            objExpert = addSingleGroupObject(2, "expert", expert, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert);

            addPropertyDraw(voteResultGroup, true, objExpert, objVote);
//            addPropertyDraw(objExpert, objVote, connectedExpertVote, competitiveAdvantagesExpertVote, commercePotentialExpertVote, canBeImplementedExpertVote, haveExpertiseExpertVote, internationalExperienceExpertVote, enoughDocumentsExpertVote, commentCompetitiveAdvantagesExpertVote, commentCommercePotentialExpertVote, commentCanBeImplementedExpertVote, commentHaveExpertiseExpertVote, commentInternationalExperienceExpertVote, commentEnoughDocumentsExpertVote);
//            addPropertyDraw(objExpert, objVote, quantityCompetitiveAdvantagesVote, quantityCommercePotentialVote, quantityCanBeImplementedVote, quantityHaveExpertiseVote, quantityInternationalExperienceVote, quantityEnoughDocumentsVote, acceptedCompetitiveAdvantagesVote, acceptedCommercePotentialVote, acceptedCanBeImplementedVote, acceptedHaveExpertiseVote, acceptedInternationalExperienceVote, acceptedEnoughDocumentsVote);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(doneCorExpertVote, objExpert, objVote)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(closedRejectedVote, objVote)));
            
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(enoughDocumentsCorExpertVote, objExpert, objVote))));

            addInlineEAForm(emailNoticeRejectedVoteEA, this, objVote, 1);
        }

        @Override
        public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
            groupHierarchy.markGroupAsNonJoinable(objVote.groupTo);
        }

    }

    private class NoticeAcceptedStatusFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private NoticeAcceptedStatusFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objVote = addSingleGroupObject(1, "vote", vote, "Заседание", nameNativeProjectVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(closedAcceptedStatusVote, objVote)));

            addInlineEAForm(emailNoticeAcceptedStatusVoteEA, this, objVote, 1);
        }
    }

    private class NoticeAcceptedPreliminaryFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private NoticeAcceptedPreliminaryFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objVote = addSingleGroupObject(1, "vote", vote, "Заседание", nameNativeProjectVote, fileDecisionVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(closedAcceptedPreliminaryVote, objVote)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(fileDecisionVote, objVote)));

            addInlineEAForm(emailNoticeAcceptedPreliminaryVoteEA, this, objVote, 1);
        }
    }

    private class AcceptanceCertificateFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objExpert;
        private ObjectEntity objVote;
        private ObjectEntity objVoteBallot;
        private ObjectEntity objYear;
        private ObjectEntity objMonth;


        private AcceptanceCertificateFormEntity(NavigatorElement parent, String sID, String caption, boolean resident) {
            super(parent, sID, caption);

            objYear = addSingleGroupObject(1, "year", IntegerClass.instance, "Год");
            objYear.groupTo.setSingleClassView(ClassViewType.PANEL);
            addPropertyDraw(objYear, baseLM.objectValue);
            getPropertyDraw(baseLM.objectValue, objYear).setSID("year");

            objMonth = addSingleGroupObject(2, "month", IntegerClass.instance, "Месяц");
            objMonth.groupTo.setSingleClassView(ClassViewType.PANEL);
            addPropertyDraw(objMonth, baseLM.objectValue);
            getPropertyDraw(baseLM.objectValue, objMonth).setSID("month");

            objExpert = addSingleGroupObject(3, "expert", expert, baseLM.selection, baseLM.userFirstName, baseLM.userLastName, documentNameExpert, sidExpert, dateAgreementExpert, nameCountryExpert, caseCountryExpert, englCountryExpert, nameCurrencyExpert, baseCurrencyExpert, englCurrencyExpert, pluralCurrencyExpert, nameNativeClusterExpert, nameLanguageExpert, residencyCountryExpert);
            //   objExpert.groupTo.initClassView = ClassViewType.PANEL;

            objVoteBallot = addSingleGroupObject(4, "voteBallot", vote, dateProjectVote, baseLM.date, dateEndVote, nameNativeProjectVote, nameNativeClaimerVote, nameNativeClusterVote, revisionVote);

            addPropertyDraw(voteResultGroup, true, objExpert, objVoteBallot);

            objVote = addSingleGroupObject(5, "vote", vote, nameNativeClaimerVote, nameForeignClaimerVote, openedVote, succeededVote, quantityDoneVote);
            addPropertyDraw(inNewExpertVote, objExpert, objVote);
            addPropertyDraw(objExpert, objVoteBallot, competitiveAdvantagesExpertVote, commercePotentialExpertVote, canBeImplementedExpertVote, haveExpertiseExpertVote, internationalExperienceExpertVote, enoughDocumentsExpertVote, commentCompetitiveAdvantagesExpertVote, commentCommercePotentialExpertVote, commentCanBeImplementedExpertVote, commentHaveExpertiseExpertVote, commentInternationalExperienceExpertVote, commentEnoughDocumentsExpertVote);
            addPropertyDraw(objExpert, objVote, objMonth, objYear, doneExpertVoteMonthYear);

            //  addPropertyDraw(voteResultGroup, true, objExpert, objVote);
            addPropertyDraw(objExpert, objMonth, objYear, quantityDoneExpertMonthYear, moneyQuantityDoneExpertMonthYear);
            if (resident)
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(residencyCountryExpert, objExpert)));

            if (!resident)
                addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(residencyCountryExpert, objExpert))));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDoneExpertMonthYear, objExpert, objMonth, objYear)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(doneExpertVoteMonthYear, objExpert, objVote, objMonth, objYear)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVote)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(doneExpertVoteMonthYear, objExpert, objVoteBallot, objMonth, objYear)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(inNewExpertVote, objExpert, objVoteBallot)));

            addDefaultOrder(nameNativeClusterExpert, true);
            addDefaultOrder(documentNameExpert, true);

            addAttachEAForm(emailLetterExpertMonthYearEA, this, AttachmentFormat.PDF, objMonth, 1, objYear, 2);
            addAttachEAForm(emailLetterExpertMonthYearEA, this, AttachmentFormat.DOCX, objMonth, 1, objYear, 2);
            //     setPageSize(0);
            addPropertyDraw(readInformation).toDraw = objYear.groupTo;
            setEditType(readInformation, PropertyEditType.READONLY);
        }
    }

    private class ClaimerAcceptedFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private ClaimerAcceptedFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Решение о соответствии", true);

            objVote = addSingleGroupObject(genID(), "vote", vote, "Заседание", dateEndVote, nameNativeProjectVote, dateProjectVote, nameNativeClaimerVote, nameAblateClaimerVote, nameNativeClusterVote, isStatusVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(succeededVote, objVote)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(acceptedVote, objVote)));

            addDefaultHintsIncrementTable(this);

            addAttachEAFormNameFullyMapped(emailClosedVoteEA, this, AttachmentFormat.PDF, emailClaimerAcceptedHeaderVote, objVote, 1);
            addAttachEAFormNameFullyMapped(emailClosedVoteEA, this, AttachmentFormat.DOCX, emailClaimerAcceptedHeaderVote, objVote, 1);
        }
    }

    private class ClaimerRejectedFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objVote;

        private ClaimerRejectedFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Решение о несоответствии", true);

            objVote = addSingleGroupObject(genID(), "vote", vote, "Заседание", dateEndVote, nameNativeProjectVote, dateProjectVote, nameNativeClaimerVote, nameAblateClaimerVote, nameDativusClaimerVote, nameGenitiveClaimerVote, isLastClusterVote, nameNativeClusterVote, isStatusVote);
            objVote.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(succeededVote, objVote)));
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(acceptedVote, objVote))));

            addDefaultHintsIncrementTable(this);

            addAttachEAFormNameFullyMapped(emailClosedVoteEA, this, AttachmentFormat.PDF, emailClaimerRejectedHeaderVote, objVote, 1);
            addAttachEAFormNameFullyMapped(emailClosedVoteEA, this, AttachmentFormat.DOCX, emailClaimerRejectedHeaderVote, objVote, 1);
        }
    }

    private class ClaimerStatusFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objProject;

        private ClaimerStatusFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Решение о присвоении статуса участника", true);

            objProject = addSingleGroupObject(genID(), "project", project, "Проект", dateProject, nameNativeProject, nameNativeClaimerProject, nameAblateClaimerProject, nameDativusClaimerProject, nameGenitiveClaimerProject, isR1Project);
            objProject.groupTo.initClassView = ClassViewType.PANEL;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(acceptedProject, objProject)));

            addAttachEAFormNameFullyMapped(emailAcceptedProjectEA, this, AttachmentFormat.PDF, emailClaimerAcceptedHeaderProject, objProject, 1);
            addAttachEAFormNameFullyMapped(emailAcceptedProjectEA, this, AttachmentFormat.DOCX, emailClaimerAcceptedHeaderProject, objProject, 1);
        }
    }

    private class StatusFormEntity extends FormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objStatus;

        private StatusFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Статус", true);
            objStatus = addSingleGroupObject(genID(), "Status", projectStatus, "Статус", numberProjectStatus, baseLM.name, oficialNameProjectStatus);

            setEditType(PropertyEditType.READONLY);

            addDefaultOrder(getPropertyDraw(numberProjectStatus, objStatus), true);
        }
    }

    private class ProjectDocumentsFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objProject;
        private ObjectEntity objDocumentTemplate;
        private ObjectEntity objDocument;
        private PropertyDrawEntity nameNativeEntity;
        private PropertyDrawEntity nameForeignEntity;
        private PropertyDrawEntity nameNativeClaimerEntity;
        private PropertyDrawEntity nameForeignClaimerEntity;
        private PropertyDrawEntity nameNativeTranslationEntity;
        private PropertyDrawEntity nameForeignTranslationEntity;
        private PropertyDrawEntity nameNativeClaimerTranslationEntity;
        private PropertyDrawEntity nameForeignClaimerTranslationEntity;

        private ProjectDocumentsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Документы по проектам");

            objProject = addSingleGroupObject(project, dateProject, dateStatusProject, nameNativeProject, nameForeignProject,
                    nameNativeShortFinalClusterProject, nameNativeClaimerProject, nameForeignClaimerProject, emailClaimerProject,
                    nameStatusProject, dateInStatusProject, normalPeriodStatusProject, quantityDaysToOverdueDateStatusProject, formLogNameStatusProject, nameProjectActionProject, updateDateProject,
                    inactiveProject, quantityClusterProject, nameRegulationsProject);

            addPropertyDraw(nameNativeShortCurrentCluster).toDraw = objProject.groupTo;
            setForceViewType(nameNativeShortCurrentCluster, ClassViewType.PANEL);

            addPropertyDraw(objProject, resolutionIPGroup);
            hideLoadFileResolutionIPProject = addHideCaptionProp(privateGroup, "Загрузить", addJProp(baseLM.andNot1, addCProp(LogicalClass.instance, true, project), 1, fileResolutionIPProject, 1));
            getPropertyDraw(loadFileResolutionIPProject).propertyCaption = addPropertyObject(hideLoadFileResolutionIPProject, objProject);
            getPropertyDraw(loadFileResolutionIPProject).forceViewType = ClassViewType.PANEL;

            LCP highlightColor = addCProp(ColorClass.instance, new Color(223, 255, 223));
            CalcPropertyObjectEntity statusProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, statusDataProject, 1), objProject);
            getPropertyDraw(nameStatusProject).setPropertyBackground(statusProperty);

            CalcPropertyObjectEntity dateProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, dateDataProject, 1), objProject);
            getPropertyDraw(dateProject).setPropertyBackground(dateProperty);

            CalcPropertyObjectEntity dateStatusProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, dateStatusDataProject, 1), objProject);
            getPropertyDraw(dateStatusProject).setPropertyBackground(dateStatusProperty);

            CalcPropertyObjectEntity nameNativeProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, nameNativeDataProject, 1), objProject);
            getPropertyDraw(nameNativeProject).setPropertyBackground(nameNativeProperty);

            CalcPropertyObjectEntity nameForeignProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, nameForeignDataProject, 1), objProject);
            getPropertyDraw(nameForeignProject).setPropertyBackground(nameForeignProperty);

            CalcPropertyObjectEntity nameNativeCorrectHighlightClaimerProjectProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, nameNativeCorrectHighlightClaimerProject, 1), objProject);
            getPropertyDraw(nameNativeClaimerProject).setPropertyBackground(nameNativeCorrectHighlightClaimerProjectProperty);

            CalcPropertyObjectEntity nameForeignCorrectHighlightClaimerProjectProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, nameForeignCorrectHighlightClaimerProject, 1), objProject);
            getPropertyDraw(nameForeignClaimerProject).setPropertyBackground(nameForeignCorrectHighlightClaimerProjectProperty);

            objDocument = addSingleGroupObject(document, nameTypeDocument, nameLanguageDocument, postfixDocument, openFileDocument);
            getPropertyDraw(postfixDocument).forceViewType = ClassViewType.PANEL;
            getPropertyDraw(postfixDocument).propertyCaption = addPropertyObject(hidePostfixDocument, objDocument);

            addPropertyDraw(exportProjectDocumentsAction, objProject).toDraw = objDocument.groupTo;
            setForceViewType(exportProjectDocumentsAction, ClassViewType.PANEL);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(projectDocument, objDocument), Compare.EQUALS, objProject));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objProject.groupTo).grid.constraints.fillVertical = 1.5;
            design.get(objProject.groupTo).grid.getContainer().setFixedSize(new Dimension(-1, 200));
            design.getMainContainer().addAfter(design.getGroupPropertyContainer(objProject.groupTo, projectDocumentsGroup), design.getGroupObjectContainer(objDocument.groupTo));

            return design;
        }
    }

    private class ApplicationsSubmittedFormEntity extends DateIntervalFormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objCluster;
        private ObjectEntity objProjectAction;
        private ObjectEntity objProjectStatus;

        public ApplicationsSubmittedFormEntity(NavigatorElement parent, String sID) {
            super(baseLM, parent, sID, "Статистика заявок, поступивших в отчетный период");

            objCluster = addSingleGroupObject(4, "cluster", cluster, "Кластер", nameNativeShort, nameNative, nameForeign);
            objProjectAction = addSingleGroupObject(5, "projectAction", projectAction, "Тип заявки", baseLM.name);

            PropertyDrawEntity count = addPropertyDraw(applicationsSubmitProjectActionClusterDateDate, objProjectAction, objCluster, objDateFrom, objDateTo);
            count.columnGroupObjects.add(objCluster.groupTo);
            count.propertyCaption = addPropertyObject(nameNativeShort, objCluster);
            count.propertyFooter = addPropertyObject(applicationsSubmitClusterDateDate, objCluster, objDateFrom, objDateTo);

            addPropertyDraw(objDateFrom, objDateTo, nonClusterApplicationsSubmitDateDate, applicationsSubmitDateDate);
            addPropertyDraw(objCluster, objDateFrom, objDateTo, applicationsSubmitClusterDateDate);
            addPropertyDraw(objProjectAction, objDateFrom, objDateTo, nonClusterApplicationsProjectActionSubmitDateDate, applicationsSubmitProjectActionDateDate);

            objProjectStatus = addSingleGroupObject(6, "projectStatus", projectStatus, "Статус заявки", baseLM.name, oficialNameProjectStatus);

            PropertyDrawEntity count1 = addPropertyDraw(applicationsSubmitStatusApplicationClusterDateDate, objProjectStatus, objCluster, objDateFrom, objDateTo);
            count1.columnGroupObjects.add(objCluster.groupTo);
            count1.propertyCaption = addPropertyObject(nameNativeShort, objCluster);
            count1.propertyFooter = addPropertyObject(applicationsSubmitClusterDateDate, objCluster, objDateFrom, objDateTo);

            addPropertyDraw(objProjectStatus, objDateFrom, objDateTo, nonClusterApplicationsStatusAplicationSubmitDateDate, applicationsSubmitStatusApplicationDateDate);

            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(inTestCluster, objCluster))));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(applicationsSubmitStatusApplicationDateDate, objProjectStatus, objDateFrom, objDateTo)));

        }
    }

     private class ApplicationsDynamicsFormEntity extends FormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objCluster;
        private ObjectEntity objTypeProjectStatus;
        private ObjectEntity objProjectStatus;

        public ApplicationsDynamicsFormEntity(NavigatorElement parent, String sID) {
            super(parent, sID, "Статистика заявок с разбивкой");

            objCluster = addSingleGroupObject(1, "cluster", cluster, "Кластер", nameNativeShort, nameNative, nameForeign, sumApplicationsCluster);

            objTypeProjectStatus = addSingleGroupObject(3, "typeProjectStatus", typeProjectStatus, "Тип статуса проекта", baseLM.name);

            PropertyDrawEntity count = addPropertyDraw(sumApplicationsTypeProjectStatusCluster, objTypeProjectStatus, objCluster);
            count.columnGroupObjects.add(objCluster.groupTo);
            count.propertyCaption = addPropertyObject(nameNativeShort, objCluster);
            count.propertyFooter = addPropertyObject(sumApplicationsCluster, objCluster);

            addPropertyDraw(objTypeProjectStatus, nonClusterApplicationsTypeProjectStatus, sumApplicationsTypeProjectStatus);
            addPropertyDraw(objTypeProjectStatus, percentNonClusterApplicationsTypeProjectStatus);
            addPropertyDraw(objTypeProjectStatus, percentApplicationsTypeProjectStatus);

            PropertyDrawEntity count2 = addPropertyDraw(percentSumApplicationsTypeProjectStatusCluster, objTypeProjectStatus, objCluster);
            count2.columnGroupObjects.add(objCluster.groupTo);
            count2.propertyCaption = addPropertyObject(nameNativeShort, objCluster);

            objProjectStatus = addSingleGroupObject(2, "projectStatus", projectStatus, "Статус заявки", baseLM.name, oficialNameProjectStatus);

            PropertyDrawEntity count1 = addPropertyDraw(sumApplicationsStatusApplicationCluster, objProjectStatus, objCluster);
            count1.columnGroupObjects.add(objCluster.groupTo);
            count1.propertyCaption = addPropertyObject(nameNativeShort, objCluster);
            addPropertyDraw(objProjectStatus, nonClusterApplicationsStatusApplication, sumApplicationsStatusApplication);


            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(inTestCluster, objCluster))));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(sumApplicationsStatusApplication, objProjectStatus)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeProjectStatusProjectStatus, objProjectStatus), Compare.EQUALS, objTypeProjectStatus));

            addPropertyDraw(sumSubmitApplications).toDraw = objProjectStatus.groupTo;
            addPropertyDraw(nonClusterApplicationsSubmit).toDraw = objProjectStatus.groupTo;

        }
    }

    private class ApplicationsStatusWeekFormEntity extends DateIntervalFormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objWeek;

        public ApplicationsStatusWeekFormEntity(NavigatorElement parent, String sID) {
            super(baseLM, parent, sID, "Общая статистика. Динамика изменения");

            addPropertyDraw(objDateFrom, objDateTo, applicationsSubmitDateDate, averageWeekApplSubmitDateDate);

            objWeek = addSingleGroupObject(4, "week", IntegerClass.instance, "Неделя", baseLM.objectValue);
            addPropertyDraw(objDateFrom, objWeek, baseLM.sumDateWeekFrom, baseLM.sumDateWeekTo, applicationsSubmitDateWeek);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(applicationsSubmitDateWeek, objDateFrom, objWeek)));
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(negative, objWeek))));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(baseLM.sumDateWeekTo, objDateFrom, objWeek), Compare.LESS_EQUALS, objDateTo));
        }
    }

    private class ApplicationsStatusTimeFormEntity extends DateIntervalFormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objApplicationStatus;

        public ApplicationsStatusTimeFormEntity(NavigatorElement parent, String sID) {
            super(baseLM, parent, sID, "Средний срок рассмотрения заявки на статус участника");

            objApplicationStatus = addSingleGroupObject(4, "applicationStatus", applicationStatus, "Заявка", nameProjectActionApplication, nameNativeClaimerApplication, dateApplicationStatus, daysClaimerApplication, quantityStatusVoteApplication, dateSubmittedToRegisterApplication, daysStatusApplication, isPreliminaryAndStatusApplication);
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(inactiveApplication, objApplicationStatus))));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateSubmittedToRegisterApplication, objApplicationStatus), Compare.LESS_EQUALS, objDateTo));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateSubmittedToRegisterApplication, objApplicationStatus), Compare.GREATER_EQUALS, objDateFrom));
        }
    }

    private class RegisterApplicationWeekFormEntity extends DateIntervalFormEntity<SkolkovoBusinessLogics> {
        private ObjectEntity objWeek;

        public RegisterApplicationWeekFormEntity(NavigatorElement parent, String sID) {
            super(baseLM, parent, sID, "Динамика среднего срока рассмотрения заявки на статус участника");

            objWeek = addSingleGroupObject(4, "week", IntegerClass.instance, "Неделя", baseLM.objectValue);
            addPropertyDraw(objDateFrom, objDateTo, statusApplicationsSubmitDateDate, averageDaysStatusApplicationsSubmitDateDate);
            addPropertyDraw(objDateFrom, objWeek, baseLM.sumDateWeekFrom, baseLM.sumDateWeekTo, submitRegisterApplicationsDateWeek, risingDaysRegisterApplicationDateWeek, risingRegisterApplicationsDateWeek, averageDaysRegisterApplicationsDateWeekWeek, averageDaysRegisterApplicationsDateWeek);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(submitRegisterApplicationsDateWeek, objDateFrom, objWeek)));
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(negative, objWeek))));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(baseLM.sumDateWeekTo, objDateFrom, objWeek), Compare.LESS_EQUALS, objDateTo));

        }
    }


    private class ApplicationsListFormEntity extends DateIntervalFormEntity<SkolkovoBusinessLogics> {

        private ObjectEntity objApplication;

         public ApplicationsListFormEntity(NavigatorElement parent, String sID) {
            super(baseLM, parent, sID, "Заявки, поступившие в отчетный период");

            objApplication = addSingleGroupObject(4, "application", application, "Заявка");
            addPropertyDraw(objApplication, dateApplication, nameNativeClaimerApplication, nameProjectActionApplication, nameNativeProjectApplication, officialNameStatusApplication, nameNativeShortAggregateClusterApplication);

            addPropertyDraw(objDateFrom, objDateTo, applicationsSubmitDateDate);
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(inactiveApplication, objApplication))));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateApplication, objApplication), Compare.LESS_EQUALS, objDateTo));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(dateApplication, objApplication), Compare.GREATER_EQUALS, objDateFrom));

             addDefaultOrder(getPropertyDraw(dateApplication, objApplication), true);
         }
    }

    public class GenerateDocumentsActionProperty extends UserActionProperty {

        private final ClassPropertyInterface projectInterface;
        private final ClassPropertyInterface documentTemplateInterface;

        public GenerateDocumentsActionProperty() {
            super(genSID(), "Сгенерировать документы", new ValueClass[]{project, documentTemplate});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            projectInterface = i.next();
            documentTemplateInterface = i.next();
        }

        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataObject projectObject = context.getKeyValue(projectInterface);
            DataObject documentTemplateObject = context.getKeyValue(documentTemplateInterface);

            Query<String, String> query = new Query<String, String>(Collections.singleton("key"));
            query.and(documentTemplateDocumentTemplateDetail.getExpr(context.getModifier(), query.mapKeys.get("key")).compare(documentTemplateObject.getExpr(), Compare.EQUALS));
            query.properties.put("documentType", typeDocument.getExpr(context.getModifier(), query.mapKeys.get("key")));
            query.properties.put("languageDocument", languageDocument.getExpr(context.getModifier(), query.mapKeys.get("key")));

            for (Map<String, Object> row : query.execute(context).values()) {
                DataObject documentObject = context.addObject(document);
                projectDocument.change(projectObject.getValue(), context, documentObject);
                typeDocument.change(row.get("documentType"), context, documentObject);
                languageDocument.change(row.get("languageDocument"), context, documentObject);
            }
        }
    }

    public class OpenApplicationProjectActionProperty extends UserActionProperty {

        private final ClassPropertyInterface projectInterface;

        boolean complete = false;

        public OpenApplicationProjectActionProperty(boolean complete) {
            super(complete ? "openCompleteApplicationProjectAction" : "openApplicationProjectAction",
                    "Открыть анкету" + (complete ? " (полную)" : ""), new ValueClass[]{project});

            this.complete = complete;

            projectInterface = interfaces.iterator().next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            try {
                DataObject projectObject = context.getKeyValue(projectInterface);

                byte[] file = (byte[]) fileNativeApplicationFormProject.read(context, projectObject);

                boolean newRegulation = false;
                Object regulation = nameRegulationsProject.read(context, projectObject);
                if (regulation != null) {
                    if ("R2".equals(regulation.toString().trim())) {
                        newRegulation = true;
                    } else newRegulation = false;
                }

                if (file == null)
                    file = generateApplicationFile(context, projectObject, false, newRegulation, complete);
                context.delayUserInterfaction(new OpenFileClientAction(file, "pdf"));

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (JRException e) {
                e.printStackTrace();
            }
        }
    }

    public class AttachProtocolDecisionVoteActionProperty extends UserActionProperty {

        private final ClassPropertyInterface voteInterface;

        public AttachProtocolDecisionVoteActionProperty() {
            super(genSID(), "Генерация протокола", new ValueClass[]{voteR2});

            voteInterface = interfaces.iterator().next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            try {
                DataObject voteObject = context.getKeyValue(voteInterface);

                File tempFile = context.generateFileFromForm(BL, voteProtocolSimple, voteProtocolSimple.objVote, voteObject);
                fileDecisionVote.change(IOUtils.getFileBytes(tempFile), context, voteObject);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class GenerateExpertSIDsActionProperty extends UserActionProperty {

        private final ClassPropertyInterface expertInterface;

        public GenerateExpertSIDsActionProperty() {
            super(genSID(), "Сгенерировать SID экспертов", new ValueClass[]{expert});

            expertInterface = interfaces.iterator().next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

            DataObject expertObject = context.getKeyValue(expertInterface);

            List<String> expertSIDList = new ArrayList<String>();

            LCP isExpert = is(expert);
            Map<Object, KeyExpr> keys = isExpert.getMapKeys();
            KeyExpr key = BaseUtils.singleValue(keys);
            Query<Object, Object> query = new Query<Object, Object>(keys);
            query.properties.put("sidExpert", sidExpert.getExpr(context.getModifier(), key));
            query.and(isExpert.getExpr(key).getWhere());
            OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(context.getSession().sql);

            for (Map<Object, Object> values : result.values()) {
                if (values.get("sidExpert") != null)
                    expertSIDList.add(values.get("sidExpert").toString());
            }

            String prefix = "";

            KeyExpr expertExpr = new KeyExpr("expert");
            KeyExpr clusterExpr = new KeyExpr("cluster");
            Map<Object, KeyExpr> newKeys = new HashMap<Object, KeyExpr>();
            newKeys.put("expert", expertExpr);
            newKeys.put("cluster", clusterExpr);

            query = new Query<Object, Object>(newKeys);
            query.properties.put(sidCluster, sidCluster.getExpr(context.getModifier(), clusterExpr));
            query.and(inClusterExpert.getExpr(context.getModifier(), clusterExpr, expertExpr).getWhere());
            query.and(expertExpr.compare(expertObject, Compare.EQUALS));
            result = query.execute(context.getSession().sql);

            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> rows : result.entrySet()) {
                DataObject clusterObject = new DataObject(rows.getKey().get("cluster"), cluster);
                Object clusterSID = sidCluster.read(context, clusterObject);
                if (clusterSID != null)
                    prefix = clusterSID.toString();
            }

            Random rand = new Random();

            String generatedSid = generateExpertSID(prefix, rand);
            while (expertSIDList.contains(generatedSid))
                generatedSid = generateExpertSID(prefix, rand);

            sidExpert.change(generatedSid, context, expertObject);
        }
    }

    private String generateExpertSID(String prefix, Random rand) {
        int nextRand = rand.nextInt(10000);
        if (nextRand < 1000) prefix += "0";
        if (nextRand < 100) prefix += "0";
        if (nextRand < 10) prefix += "0";
        return prefix + nextRand;
    }

    public class IncludeDocumentsActionProperty extends UserActionProperty {

        private final ClassPropertyInterface projectInterface;

        public IncludeDocumentsActionProperty() {
            super(genSID(), "Подключить документы", new ValueClass[]{project});

            projectInterface = interfaces.iterator().next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

            try {

                DataObject projectObject = context.getKeyValue(projectInterface);
                DataObject documentObject;

                Object regulation = nameRegulationsProject.read(context, projectObject);

                if(regulation!=null) {
                    if("R1".equals(regulation.toString().trim())) {

                Object file = fileNativeApplicationFormProject.read(context, projectObject);
                documentObject = context.addObject(document);
                projectDocument.change(projectObject.getValue(), context, documentObject);
                typeDocument.change(documentType.getID("application"), context, documentObject);
                languageDocument.change(language.getID("russian"), context, documentObject);
                if (file != null)
                    fileDocument.change(file, context, documentObject);
                else if (needsToBeTranslatedToRussianProject.read(context, projectObject) == null)
                    fileDocument.change(generateApplicationFile(context, projectObject, false, false, false), context, documentObject);

                file = fileForeignApplicationFormProject.read(context, projectObject);
                documentObject = context.addObject(document);
                projectDocument.change(projectObject.getValue(), context, documentObject);
                typeDocument.change(documentType.getID("application"), context, documentObject);
                languageDocument.change(language.getID("english"), context, documentObject);
                if (file != null)
                    fileDocument.change(file, context, documentObject);
                else if (needsToBeTranslatedToEnglishProject.read(context, projectObject) == null)
                    fileDocument.change(generateApplicationFile(context, projectObject, true, false, false), context, documentObject);

                file = fileNativeSummaryProject.read(context, projectObject);
                if (file != null) {
                    documentObject = context.addObject(document);
                    fileDocument.change(file, context, documentObject);
                    projectDocument.change(projectObject.getValue(), context, documentObject);
                    typeDocument.change(documentType.getID("resume"), context, documentObject);
                    languageDocument.change(language.getID("russian"), context, documentObject);
                }

                file = fileForeignSummaryProject.read(context, projectObject);
                if (file != null) {
                    documentObject = context.addObject(document);
                    fileDocument.change(file, context, documentObject);
                    projectDocument.change(projectObject.getValue(), context, documentObject);
                    typeDocument.change(documentType.getID("resume"), context, documentObject);
                    languageDocument.change(language.getID("english"), context, documentObject);
                }

                Query<String, String> query = new Query<String, String>(Collections.singleton("nonRussianSpecialist"));
                query.and(projectNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("nonRussianSpecialist")).compare(projectObject.getExpr(), Compare.EQUALS));
                query.properties.put("fullNameNonRussianSpecialist", projectNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("nonRussianSpecialist")));
                query.properties.put("fileNativeResumeNonRussianSpecialist", fileNativeResumeNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("nonRussianSpecialist")));
                query.properties.put("fileForeignResumeNonRussianSpecialist", fileForeignResumeNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("nonRussianSpecialist")));
                int countForeign = 1;
                int countNative = 1;
                int size = query.executeClasses(context).entrySet().size();
                for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(context).entrySet()) {
                    row.getKey().get("nonRussianSpecialist");
                    row.getValue().get("fullNameNonRussianSpecialist");
                    row.getValue().get("fileForeignResumeNonRussianSpecialist");
                    row.getValue().get("fileNativeResumeNonRussianSpecialist");

                    file = row.getValue().get("fileForeignResumeNonRussianSpecialist").getValue();
                    if (file != null) {
                        documentObject = context.addObject(document);
                        projectDocument.change(projectObject.getValue(), context, documentObject);
                        typeDocument.change(documentType.getID("forres"), context, documentObject);
                        languageDocument.change(language.getID("english"), context, documentObject);
                        if (size > 1)
                            postfixDocument.change(String.valueOf(countForeign), context, documentObject);
                        fileDocument.change(file, context, documentObject);
                        countForeign++;
                    }

                    file = row.getValue().get("fileNativeResumeNonRussianSpecialist").getValue();
                    if (file != null) {
                        documentObject = context.addObject(document);
                        projectDocument.change(projectObject.getValue(), context, documentObject);
                        typeDocument.change(documentType.getID("forres"), context, documentObject);
                        languageDocument.change(language.getID("russian"), context, documentObject);
                        if (size > 1)
                            postfixDocument.change(String.valueOf(countNative), context, documentObject);
                        fileDocument.change(file, context, documentObject);
                        countNative++;
                    }
                }

                file = fileNativeTechnicalDescriptionProject.read(context, projectObject);
                if (file != null) {
                    documentObject = context.addObject(document);
                    projectDocument.change(projectObject.getValue(), context, documentObject);
                    typeDocument.change(documentType.getID("techdesc"), context, documentObject);
                    languageDocument.change(language.getID("russian"), context, documentObject);
                    fileDocument.change(file, context, documentObject);
                }

                file = fileForeignTechnicalDescriptionProject.read(context, projectObject);
                if (file != null) {
                    documentObject = context.addObject(document);
                    projectDocument.change(projectObject.getValue(), context, documentObject);
                    typeDocument.change(documentType.getID("techdesc"), context, documentObject);
                    languageDocument.change(language.getID("english"), context, documentObject);
                    fileDocument.change(file, context, documentObject);
                }

                file = fileNativeRoadMapProject.read(context, projectObject);
                if (file != null) {
                    documentObject = context.addObject(document);
                    projectDocument.change(projectObject.getValue(), context, documentObject);
                    typeDocument.change(documentType.getID("roadmap"), context, documentObject);
                    languageDocument.change(language.getID("russian"), context, documentObject);
                    fileDocument.change(file, context, documentObject);
                }

                file = fileForeignRoadMapProject.read(context, projectObject);
                if (file != null) {
                    documentObject = context.addObject(document);
                    projectDocument.change(projectObject.getValue(), context, documentObject);
                    typeDocument.change(documentType.getID("roadmap"), context, documentObject);
                    languageDocument.change(language.getID("english"), context, documentObject);
                    fileDocument.change(file, context, documentObject);
                }

                file = fileResolutionIPProject.read(context, projectObject);
                if (file != null) {
                    documentObject = context.addObject(document);
                    projectDocument.change(projectObject.getValue(), context, documentObject);
                    typeDocument.change(documentType.getID("ipres"), context, documentObject);
                    languageDocument.change(language.getID("english"), context, documentObject);
                    fileDocument.change(file, context, documentObject);
                }
                    } else {

                        Object file = fileNativeRoadMapProject.read(context, projectObject);
                        if (file != null) {
                            documentObject = context.addObject(document);
                            projectDocument.change(projectObject.getValue(), context, documentObject);
                            typeDocument.change(documentType.getID("roadmap"), context, documentObject);
                            languageDocument.change(language.getID("russian"), context, documentObject);
                            fileDocument.change(file, context, documentObject);
                        }

                        file = fileForeignRoadMapProject.read(context, projectObject);
                        if (file != null) {
                            documentObject = context.addObject(document);
                            projectDocument.change(projectObject.getValue(), context, documentObject);
                            typeDocument.change(documentType.getID("roadmap"), context, documentObject);
                            languageDocument.change(language.getID("english"), context, documentObject);
                            fileDocument.change(file, context, documentObject);
                        }

                        file = fileNativeApplicationFormProject.read(context, projectObject);
                        documentObject = context.addObject(document);
                        projectDocument.change(projectObject.getValue(), context, documentObject);
                        typeDocument.change(documentType.getID("application"), context, documentObject);
                        languageDocument.change(language.getID("russian"), context, documentObject);
                        if (file != null)
                            fileDocument.change(file, context, documentObject);
                        else if (needsToBeTranslatedToRussianProject.read(context, projectObject) == null)
                            fileDocument.change(generateApplicationFile(context, projectObject, false, true, false), context, documentObject);

                        file = fileForeignApplicationFormProject.read(context, projectObject);
                        documentObject = context.addObject(document);
                        projectDocument.change(projectObject.getValue(), context, documentObject);
                        typeDocument.change(documentType.getID("application"), context, documentObject);
                        languageDocument.change(language.getID("english"), context, documentObject);
                        if (file != null)
                            fileDocument.change(file, context, documentObject);
                        else if (needsToBeTranslatedToEnglishProject.read(context, projectObject) == null)
                            fileDocument.change(generateApplicationFile(context, projectObject, true, true, false), context, documentObject);
/*
                        Query<String, String> query = new Query<String, String>(Collections.singleton("specialist"));
                        query.and(projectSpecialist.getExpr(context.getModifier(), query.mapKeys.get("specialist")).compare(projectObject.getExpr(), Compare.EQUALS));
                        query.properties.put("nameNativeSpecialist", projectNonRussianSpecialist.getExpr(context.getModifier(), query.mapKeys.get("specialist")));
                        query.properties.put("filePassportSpecialist", filePassportSpecialist.getExpr(context.getModifier(), query.mapKeys.get("specialist")));
                        query.properties.put("fileStatementSpecialist", fileStatementSpecialist.getExpr(context.getModifier(), query.mapKeys.get("specialist")));
                        int countPassport = 1;
                        int countStatement = 1;
                        int size = query.executeClasses(context.getSession(), baseClass).entrySet().size();
                        for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(context.getSession(), baseClass).entrySet()) {
                            row.getKey().get("specialist");
                            row.getValue().get("nameNativeSpecialist");
                            row.getValue().get("filePassportSpecialist");
                            row.getValue().get("fileStatementSpecialist");

                            file = row.getValue().get("filePassportSpecialist").getValue();
                            if (file != null) {
                                documentObject = context.addObject(document);
                                projectDocument.execute(projectObject.getValue(), context, documentObject);
                                typeDocument.execute(documentType.getID("passport"), context, documentObject);
                                languageDocument.execute(language.getID("russian"), context, documentObject);
                                if (size > 1)
                                    postfixDocument.execute(String.valueOf(countPassport), context, documentObject);
                                fileDocument.execute(file, context, documentObject);
                                countPassport++;
                            }

                            file = row.getValue().get("fileStatementSpecialist").getValue();
                            if (file != null) {
                                documentObject = context.addObject(document);
                                projectDocument.execute(projectObject.getValue(), context, documentObject);
                                typeDocument.execute(documentType.getID("statement"), context, documentObject);
                                languageDocument.execute(language.getID("russian"), context, documentObject);
                                if (size > 1)
                                    postfixDocument.execute(String.valueOf(countStatement), context, documentObject);
                                fileDocument.execute(file, context, documentObject);
                                countStatement++;
                            }
                        } */
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (JRException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }


    public class ImportIPsExpertVoteActionProperty extends CustomReadValueActionProperty {

        protected CustomStaticFormatFileClass valueClass;

        public ImportIPsExpertVoteActionProperty() {
            super(genSID(), "Импортировать IP экспертов", new ValueClass[]{});
            valueClass = CustomStaticFormatFileClass.getDefinedInstance(true, "Файлы c данными", "*");
        }

        protected DataClass getReadType() {
            return DoubleClass.instance;
        }

        protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
            try {
                List<byte[]> fileList = valueClass.getFiles(userValue);

                DataSession session = context.getSession();

                for(byte[] file : fileList) {

                File tempFile = File.createTempFile("temp", "tempFile");
                FileOutputStream fileOuputStream = new FileOutputStream(tempFile);
	            fileOuputStream.write(file);
	            fileOuputStream.close();
                Scanner rowScanner = new Scanner(tempFile);

                    while (rowScanner.hasNextLine()) {
                        String str = rowScanner.nextLine();
                        Pattern p = Pattern.compile("(.*)(\\s-\\s-)(.*)(/res/expert\\.html\\?voteId=)([a-zA-Z0-9-_]*)(.*)");
                        Matcher m = p.matcher(str);
                        if (m.find()) {
                            Integer[] ids = BaseUtils.decode(2, m.group(5)); //voteID - expertID
                            DataObject voteObj = session.getDataObject(ids[0], ObjectType.instance);
                            DataObject expertObj = session.getDataObject(ids[1], ObjectType.instance);
                            if (ipExpertVote.read(session, expertObj, voteObj) == null)
                                ipExpertVote.change(m.group(1), context, expertObj, voteObj);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    public byte[] generateApplicationFile(ExecutionContext context, DataObject project, boolean foreign, boolean newRegulation, boolean complete) throws IOException, ClassNotFoundException, JRException, SQLException {

        File tempFile;
        if (newRegulation) {
            ProjectFullR2FormEntity applicationForm = foreign ? (complete ? projectCompleteR2Foreign : projectFullR2Foreign) : (complete ? projectCompleteR2Native : projectFullR2Native);
            tempFile = context.generateFileFromForm(BL, applicationForm, applicationForm.objProject, project);
        } else {
            ProjectFullFormEntity applicationForm = foreign ? projectFullForeign : projectFullNative;
            tempFile = context.generateFileFromForm(BL, applicationForm, applicationForm.objProject, project);
        }
        if (newRegulation) {
            byte[] descriptionFile;
            if (foreign)
                descriptionFile = (byte[]) fileForeignTechnicalDescriptionProject.read(context, project);
            else
                descriptionFile = (byte[]) fileNativeTechnicalDescriptionProject.read(context, project);
            if (descriptionFile != null) {
                PdfReader reader1 = new PdfReader(new FileInputStream(tempFile));
                PdfReader reader2 = null;
                try {
                    reader2 = new PdfReader(new FileInputStream(PdfUtils.makeStamp(new ByteArrayInputStream(descriptionFile),
                            "Приложение 1 к пункту «Технология и(или) направление прикладных исследований»")));
                } catch (DocumentException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    return IOUtils.getFileBytes(tempFile);
                }
                File outputFile = File.createTempFile("merged", ".pdf");
                PdfCopyFields copy = null;
                try {
                    copy = new PdfCopyFields(new FileOutputStream(outputFile));
                    copy.addDocument(reader1);
                    copy.addDocument(reader2);
                    copy.close();
                    return IOUtils.getFileBytes(outputFile);
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
            }
        }
        return IOUtils.getFileBytes(tempFile);
    }

    public class GenerateVoteActionProperty extends UserActionProperty {

        private final ClassPropertyInterface projectInterface;

        public GenerateVoteActionProperty() {
            super("generateVote", "Сгенерировать заседание", new ValueClass[]{project});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            projectInterface = i.next();
        }

        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataObject projectObject = context.getKeyValue(projectInterface);

            boolean r2 = isR2Project.read(context, projectObject) != null;
            boolean allExperts = useAllClusterExpertsUnionProject.read(context, projectObject) != null;

            boolean r2Foresight = r2 && !allExperts;

            // считываем всех экспертов, которые уже голосовали по проекту
            Query<String, String> query = new Query<String, String>(Collections.singleton("key"));
            query.and(doneProjectExpert.getExpr(context.getModifier(), projectObject.getExpr(), query.mapKeys.get("key")).getWhere());
            query.and(currentClusterProject.getExpr(context.getModifier(), projectObject.getExpr()).compare( // смотрим чтобы голосование было из того же кластера
                      clusterVotedProjectExpert.getExpr(context.getModifier(), projectObject.getExpr(), query.mapKeys.get("key")), Compare.EQUALS));
            query.and(inClusterExpert.getExpr(context.getModifier(), currentClusterProject.getExpr(context.getModifier(), projectObject.getExpr()), query.mapKeys.get("key")).getWhere());
            query.properties.put("vote", voteProjectExpert.getExpr(context.getModifier(), projectObject.getExpr(), query.mapKeys.get("key")));
            query.properties.put("business", isBusinessExpert.getExpr(context.getModifier(), query.mapKeys.get("key")));
//            query.properties.put("technical", isTechnicalExpert.getExpr(context.getModifier(), query.mapKeys.get("key")));

            int previousBusiness = 0;
//            int previousTechnical = 0;
            Map<DataObject, DataObject> previousResults = new HashMap<DataObject, DataObject>();
            for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(context).entrySet()) {
                if (!row.getValue().get("business").isNull())
                    previousBusiness++;
                previousResults.put(row.getKey().get("key"), (DataObject) row.getValue().get("vote"));
            }

            // считываем всех неголосовавших экспертов из этого кластера
            query = new Query<String, String>(Collections.singleton("key"));
            query.and(inClusterExpert.getExpr(context.getModifier(), currentClusterProject.getExpr(context.getModifier(), projectObject.getExpr()), query.mapKeys.get("key")).getWhere());
            query.and(disableExpert.getExpr(context.getModifier(), query.mapKeys.get("key")).getWhere().not());
            query.and(voteResultProjectExpert.getExpr(context.getModifier(), projectObject.getExpr(), query.mapKeys.get("key")).getWhere().not());
            query.properties.put("technical", isTechnicalExpert.getExpr(context.getModifier(), query.mapKeys.get("key")));
            query.properties.put("business", isBusinessExpert.getExpr(context.getModifier(), query.mapKeys.get("key")));

            if (r2Foresight) {
                query.and(quantityForesightProjectExpert.getExpr(context.getModifier(), projectObject.getExpr(), query.mapKeys.get("key")).getWhere());
            }

            query.properties.put("in", inProjectExpert.getExpr(context.getModifier(), projectObject.getExpr(), query.mapKeys.get("key")));

            // получаем два списка - один, которые уже назначались на проект, другой - которые нет
            java.util.List<DataObject> expertNew = new ArrayList<DataObject>();
            java.util.List<DataObject> expertVoted = new ArrayList<DataObject>();

            java.util.List<DataObject> expertNewBusiness = new ArrayList<DataObject>();
            java.util.List<DataObject> expertNewTechnical = new ArrayList<DataObject>();

            java.util.List<DataObject> expertVotedBusiness = new ArrayList<DataObject>();
            java.util.List<DataObject> expertVotedTechnical = new ArrayList<DataObject>();

            for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(context).entrySet()) {
                DataObject objExpert = row.getKey().get("key");

                boolean business = row.getValue().get("business").getValue() != null;
                boolean technical = row.getValue().get("technical").getValue() != null;

                if (row.getValue().get("in").getValue() != null) {// эксперт уже голосовал
                    expertVoted.add(objExpert);
                    if (r2) {
                        if (business)
                            expertVotedBusiness.add(objExpert);
                        if (technical)
                            expertVotedTechnical.add(objExpert);
                    }
                } else {
                    expertNew.add(objExpert);
                    if (r2) {
                        if (business)
                            expertNewBusiness.add(objExpert);
                        if (technical)
                            expertNewTechnical.add(objExpert);
                    }
                }
            }

            Integer requiredTotal = nvl((Integer) requiredQuantity.read(context), 0);
            Integer required = requiredTotal - previousResults.size();
            Integer requiredBusiness = Math.max(nvl((Integer) requiredBusinessQuantity.read(context), 0) - previousBusiness, 0);
            if (required > expertVoted.size() + expertNew.size()) {
                context.delayUserInterfaction(new MessageClientAction("Недостаточно экспертов по кластеру/форсайту", "Генерация заседания"));
                return;
            }

            if (r2Foresight && (requiredBusiness > expertNewBusiness.size() + expertVotedBusiness.size())) {
                context.delayUserInterfaction(new MessageClientAction("Недостаточно бизнес-экспертов по форсайту", "Генерация заседания"));
                return;
            }

            // создаем новое заседание
            DataObject voteObject = context.addObject(r2 ? voteR2 : voteR1);
            projectVote.change(projectObject.object, context, voteObject);
            clusterVote.change(currentClusterProject.read(context, projectObject), context, voteObject);
            projectActionVote.change(projectActionProject.read(context, projectObject), context, voteObject);
            if (r2)
                percentNeededVote.change(percentNeeded.read(context), context, voteObject);
            requiredQuantityVote.change(requiredTotal, context, voteObject);

            // копируем результаты старых заседаний
            for (Map.Entry<DataObject, DataObject> row : previousResults.entrySet()) {
                inExpertVote.change(true, context, row.getKey(), voteObject);
                oldExpertVote.change(true, context, row.getKey(), voteObject);
                ArrayList<LCP> copyProperties = new ArrayList<LCP>(Arrays.asList(dateExpertVote, voteResultExpertVote));
                if (r2)
                    copyProperties.addAll(Arrays.asList(competitiveAdvantagesExpertVote, commercePotentialExpertVote, canBeImplementedExpertVote,
                        haveExpertiseExpertVote, internationalExperienceExpertVote, enoughDocumentsExpertVote,
                        commentCompetitiveAdvantagesExpertVote, commentCommercePotentialExpertVote, commentCanBeImplementedExpertVote,
                        commentHaveExpertiseExpertVote, commentInternationalExperienceExpertVote, commentEnoughDocumentsExpertVote,
                        businessExpertVote));
                else
                    copyProperties.addAll(Arrays.asList(inClusterExpertVote,
                        innovativeExpertVote, foreignExpertVote, innovativeCommentExpertVote,
                        competentExpertVote, completeExpertVote, completeCommentExpertVote));
                for (LCP property : copyProperties) {
                    property.change(property.read(context, row.getKey(), row.getValue()), context, row.getKey(), voteObject);
                }
            }

            // назначаем новых экспертов - сначала, которые не голосовали еще, а затем остальных
            Random rand = new Random();
            while (required > 0) {
                if (r2Foresight) {
                    if (requiredBusiness > 0) { // сначала берем бизнес-экспертов
                        if (!expertNewBusiness.isEmpty()) {
                            includeExpertInVote(rand, context, voteObject, expertNewBusiness, expertNewTechnical, true);
                        } else {
                            includeExpertInVote(rand, context, voteObject, expertVotedBusiness, expertVotedTechnical, true);
                        }
                        requiredBusiness--;
                    } else { // потом технических
                        if (!expertNewTechnical.isEmpty()) {
                            includeExpertInVote(rand, context, voteObject, expertNewTechnical, expertNewBusiness, false);
                        } else {
                            if (expertVotedTechnical.isEmpty()) {
                                context.delayUserInterfaction(new MessageClientAction("Недостаточно технических экспертов по форсайту", "Генерация заседания"));
                                return;
                            }
                            includeExpertInVote(rand, context, voteObject, expertVotedTechnical, expertVotedBusiness, false);
                        }
                    }
                } else
                    if (!expertNew.isEmpty())
                        inExpertVote.change(true, context, expertNew.remove(rand.nextInt(expertNew.size())), voteObject);
                    else
                        inExpertVote.change(true, context, expertVoted.remove(rand.nextInt(expertVoted.size())), voteObject);
                required--;
            }
        }

        private void includeExpertInVote(Random rand, ExecutionContext context, DataObject voteObject, List<DataObject> experts, List<DataObject> expertsExtra, boolean isBusiness) throws SQLException {
            DataObject objExpert = experts.get(rand.nextInt(experts.size()));
            inExpertVote.change(true, context, objExpert, voteObject);
            if (isBusiness)
                businessExpertVote.change(true, context, objExpert, voteObject);
            experts.remove(objExpert);
            expertsExtra.remove(objExpert);
        }

        @Override
        public PropsNewSession aspectChangeExtProps() {
            return getChangeProps((CalcProperty)projectVote.property, (CalcProperty)inExpertVote.property);
        }
    }

    public class CopyResultsActionProperty extends UserActionProperty {

        private final ClassPropertyInterface voteInterface;

        public CopyResultsActionProperty() {
            super(genSID(), "Скопировать результаты из предыдущего заседания", new ValueClass[]{vote});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            voteInterface = i.next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataObject voteObject = context.getKeyValue(voteInterface);
            java.sql.Date dateStart = (java.sql.Date) dateStartVote.read(context, voteObject);

            DataObject projectObject = new DataObject(projectVote.read(context, voteObject), project);
            Query<String, String> voteQuery = new Query<String, String>(Collections.singleton("vote"));
            voteQuery.and(projectVote.getExpr(context.getModifier(), voteQuery.mapKeys.get("vote")).compare(projectObject.getExpr(), Compare.EQUALS));
            voteQuery.properties.put("dateStartVote", dateStartVote.getExpr(context.getModifier(), voteQuery.mapKeys.get("vote")));

            java.sql.Date datePrev = null;
            DataObject votePrevObject = null;

            for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : voteQuery.executeClasses(context).entrySet()) {
                java.sql.Date dateCur = (java.sql.Date) row.getValue().get("dateStartVote").getValue();
                if (dateCur != null && dateCur.getTime() < dateStart.getTime() && (datePrev == null || dateCur.getTime() > datePrev.getTime())) {
                    datePrev = dateCur;
                    votePrevObject = row.getKey().get("vote");
                }
            }
            if (votePrevObject == null) return;

            // считываем всех экспертов, которые уже голосовали по проекту
            Query<String, String> query = new Query<String, String>(Collections.singleton("key"));
            query.and(doneCorExpertVote.getExpr(context.getModifier(), query.mapKeys.get("key"), votePrevObject.getExpr()).getWhere());
//            query.properties.put("expert", object(expert).getExpr(session.modifier, query.mapKeys.get("key")));

            Set<DataObject> experts = new HashSet<DataObject>();
            for (Map.Entry<Map<String, DataObject>, Map<String, ObjectValue>> row : query.executeClasses(context).entrySet()) {
                experts.add(row.getKey().get("key"));
            }

            // копируем результаты старых заседаний
            for (DataObject expert : experts) {
                inExpertVote.change(true, context, expert, voteObject);
                oldExpertVote.change(true, context, expert, voteObject);
                LCP[] copyProperties = new LCP[]{dateExpertVote, voteResultExpertVote, inClusterExpertVote,
                        innovativeExpertVote, foreignExpertVote, innovativeCommentExpertVote,
                        competentExpertVote, completeExpertVote, completeCommentExpertVote};
                for (LCP property : copyProperties) {
                    property.change(property.read(context, expert, votePrevObject), context, expert, voteObject);
                }
            }
        }
    }
}
