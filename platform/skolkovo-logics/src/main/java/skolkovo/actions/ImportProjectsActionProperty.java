package skolkovo.actions;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.context.ThreadLocalContext;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.integration.*;
import platform.server.logics.ContactLogicsModule;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.UserActionProperty;
import platform.server.session.DataSession;
import platform.server.session.SessionTableUsage;
import skolkovo.SkolkovoLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class ImportProjectsActionProperty extends UserActionProperty {
    private static Logger logger = Logger.getLogger("import");

    private final SkolkovoLogicsModule LM;
    private final ContactLogicsModule contactLM;

    private final boolean onlyMessage;
    private final boolean onlyReplace;
    private final boolean fillSids;

    public ImportProjectsActionProperty(String caption, boolean onlyMessage, boolean onlyReplace, boolean fillSids, SkolkovoLogicsModule LM, ContactLogicsModule contactLM) {
        super(LM.genSID(), caption, new ValueClass[]{});
        this.onlyMessage = onlyMessage;
        this.onlyReplace = onlyReplace;
        this.fillSids = fillSids;
        this.LM = LM;
        this.contactLM = contactLM;
    }

    protected ImportField dateProjectField, dateStatusProjectField,
            projectIdField, nameNativeProjectField, nameForeignProjectField,
            nameNativeManagerProjectField, nameNativeGenitiveManagerProjectField, nameNativeDativusManagerProjectField, nameNativeAblateManagerProjectField,
            nameForeignManagerProjectField,
            nativeProblemProjectField, foreignProblemProjectField, nativeInnovativeProjectField, foreignInnovativeProjectField,
            projectTypeProjectField, projectActionProjectField,
            nativeSubstantiationProjectTypeField, foreignSubstantiationProjectTypeField, nativeSubstantiationProjectClusterField, foreignSubstantiationProjectClusterField,
            isOwnedEquipmentProjectField, isAvailableEquipmentProjectField,
            isTransferEquipmentProjectField, descriptionTransferEquipmentProjectField, ownerEquipmentProjectField,
            isPlanningEquipmentProjectField, specificationEquipmentProjectField,
            isSeekEquipmentProjectField, descriptionEquipmentProjectField,
            isOtherEquipmentProjectField, commentEquipmentProjectField,
            fillNativeProjectField, fillForeignProjectField,
            isConsultingCenterQuestionProjectField, isConsultingCenterCommentProjectField, consultingCenterCommentProjectField,

            isReturnInvestmentsProjectField, nameReturnInvestorProjectField, amountReturnFundsProjectField,
            isNonReturnInvestmentsProjectField, isCapitalInvestmentProjectField, isPropertyInvestmentProjectField, isGrantsProjectField, isOtherNonReturnInvestmentsProjectField,
            nameNonReturnInvestorProjectField, amountNonReturnFundsProjectField, commentOtherNonReturnInvestmentsProjectField,
            isOwnFundsProjectField, amountOwnFundsProjectField,
            isPlanningSearchSourceProjectField, amountFundsProjectField,
            isOtherSoursesProjectField, commentOtherSoursesProjectField, updateDateProjectField,

            nameNativeClusterField, inProjectClusterField, inClaimerProjectClusterField, numberCurrentClusterField,
            isOtherClusterProjectField, nativeSubstantiationOtherClusterProjectField, foreignSubstantiationOtherClusterProjectField,
            nameNativeClaimerField, nameForeignClaimerField,
            firmNameNativeClaimerField, firmNameForeignClaimerField, phoneClaimerField, addressClaimerField, siteClaimerField,
            emailClaimerField, emailFirmClaimerField, OGRNClaimerField, INNClaimerField,
            fileStatementClaimerField, fileConstituentClaimerField, fileExtractClaimerField,
            fileNativeSummaryProjectField, fileForeignSummaryProjectField,
            fileRoadMapProjectField, fileForeignRoadMapProjectField,
            nativeTypePatentField, foreignTypePatentField, nativeNumberPatentField, foreignNumberPatentField,
            datePatentField, isOwnedPatentField, ownerPatentField, ownerTypePatentField, isValuatedPatentField, valuatorPatentField,
            fileIntentionOwnerPatentField, fileActValuationPatentField, linksPatentField,
            fullNameAcademicField, institutionAcademicField, titleAcademicField,
            fileDocumentConfirmingAcademicField, fileDocumentEmploymentAcademicField,
            fullNameNonRussianSpecialistField, organizationNonRussianSpecialistField, titleNonRussianSpecialistField,
            fileForeignResumeNonRussianSpecialistField, fileNativeResumeNonRussianSpecialistField,
            filePassportNonRussianSpecialistField, fileStatementNonRussianSpecialistField,
            fileMinutesOfMeetingExpertCollegiumProjectField,  fileWrittenConsentClaimerProjectField,
            fileNativeTechnicalDescriptionProjectField, fileForeignTechnicalDescriptionProjectField,
            filePassportSpecialistField, fileStatementSpecialistField,

            projectMissionProjectField, nativeCommentMissionProjectField, foreignCommentMissionProjectField,
            projectScheduleProjectField,
            nativeResumeProjectField, foreignResumeProjectField, nameNativeContactProjectField, nameForeignContactProjectField,
            phoneContactProjectField, emailContactProjectField,
            nativeMarketTrendsProjectField, foreignMarketTrendsProjectField, nativeRelevanceProjectField,
            foreignRelevanceProjectField, nativeBasicTechnologyProjectField, foreignBasicTechnologyProjectField,
            linksMarketTrendsProjectField, linksAnalogProjectField,
            nativeCaseStudiesProjectField, foreignCaseStudiesProjectField,
            nativeCharacteristicsAnaloguesProjectField, foreignCharacteristicsAnaloguesProjectField,
            nativeCompaniesAnaloguesProjectField, foreignCompaniesAnaloguesProjectField,
            nativeMarketIntroductionProjectField, foreignMarketIntroductionProjectField, linksMarketIntroductionProjectField, linksForeignMarketIntroductionProjectField,
            nativeHistoryProjectField, foreignHistoryProjectField, nativeDynamicsProjectField,
            foreignDynamicsProjectField, nativeGrantsProjectField, foreignGrantsProjectField,
            nativeLaboratoryProjectField, foreignLaboratoryProjectField, nativeInvestmentProjectField,
            foreignInvestmentProjectField, nativeResultsProjectField, foreignResultsProjectField,
            nativeGeneralizedPlanProjectField, foreignGeneralizedPlanProjectField,

            nativeCommentResearchField, foreignCommentResearchField, dataResearchField,
            nativePublicationsField, foreignPublicationsField, nativeAuthorPublicationsField, foreignAuthorPublicationsField,
            nativeEditionPublicationsField, foreignEditionPublicationsField, datePublicationsField, nativeLinksPublicationsField,
            nativeProjectCommercializationField, foreignProjectCommercializationField,
            nativeCommentProjectCommercializationField, foreignCommentProjectCommercializationField,
            nativeProjectAnaloguesField, foreignProjectAnaloguesField, nativeDescriptionProjectAnaloguesField,
            foreignDescriptionProjectAnaloguesField, nativeCharacteristicsProjectAnaloguesField,
            foreignCharacteristicsProjectAnaloguesField,
            nameNativeSpecialistField, nameForeignSpecialistField, nativePostSpecialistField,
            foreignPostSpecialistField, nativeFunctionSpecialistField, foreignFunctionSpecialistField,
            nativeScopeSpecialistField, foreignScopeSpecialistField, nativeExperienceSpecialistField,
            foreignExperienceSpecialistField, nativeTitleSpecialistField, foreignTitleSpecialistField,
            nativeWorkSpecialistField, foreignWorkSpecialistField, nativePublicationsSpecialistField,
            foreignPublicationsSpecialistField, nativeCitationSpecialistField, foreignCitationSpecialistField,
            nativeIntellectualPropertySpecialistField, foreignIntellectualPropertySpecialistField,
            nativeMileStoneYearField, nativeMileStoneField, orderNumberMileStoneField, nativeResearchDescriptionTypeMileStoneMileStoneField,
            nativeProductCreationDescriptionTypeMileStoneMileStoneField, nativePlanOnHiringDescriptionTypeMileStoneMileStoneField,
            nativeLicensingDescriptionTypeMileStoneMileStoneField, nativePromotionDescriptionTypeMileStoneMileStoneField, 
            nativeSellingDescriptionTypeMileStoneMileStoneField, foreignResearchDescriptionTypeMileStoneMileStoneField, 
            foreignProductCreationDescriptionTypeMileStoneMileStoneField, foreignPlanOnHiringDescriptionTypeMileStoneMileStoneField,
            foreignLicensingDescriptionTypeMileStoneMileStoneField, foreignPromotionDescriptionTypeMileStoneMileStoneField,
            foreignSellingDescriptionTypeMileStoneMileStoneField,
            nativeProjectObjectivesField, foreignProjectObjectivesField;

    ImportKey<?> projectKey, projectTypeProjectKey, projectActionProjectKey, claimerKey, patentKey, ownerTypePatentKey,
            clusterKey, academicKey, nonRussianSpecialistKey, projectMissionProjectKey, projectScheduleProjectKey,
            researchKey, publicationsKey, commercializationKey, analoguesKey, specialistKey, mileStoneYearKey, mileStoneKey, objectivesKey;
    List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
    ImportProperty<?> propertyDate;
    ImportProperty<?> propertyStatusDate;
    ImportProperty<?> propertyOtherCluster;
    ImportProperty<?> propertyOtherClusterNative;
    ImportProperty<?> propertyOtherClusterForeign;
    List<ImportProperty<?>> propertiesNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesForeign = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesFullClaimer = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesFullClaimerNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesFullClaimerForeign = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesCluster = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesClusterNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesClusterForeign = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesPatent = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesPatentNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesPatentForeign = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesAcademic = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesNonRussianSpecialist = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesResearch = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesResearchNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesResearchForeign = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesPublications = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesPublicationsNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesPublicationsForeign = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesCommercialization = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesCommercializationNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesCommercializationForeign = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesAnalogues = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesAnaloguesNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesAnaloguesForeign = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesSpecialist = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesSpecialistNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesSpecialistForeign = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesMileStone = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesMileStoneNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesMileStoneForeign = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesObjectives = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesObjectivesNative = new ArrayList<ImportProperty<?>>();
    List<ImportProperty<?>> propertiesObjectivesForeign = new ArrayList<ImportProperty<?>>();

    private void initOldFieldsNProperties() {
        dateProjectField = new ImportField(LM.date);
        dateStatusProjectField = new ImportField(LM.statusDateProject);
        projectIdField = new ImportField(LM.sidProject);
        nameNativeProjectField = new ImportField(LM.nameNativeJoinProject);
        nameForeignProjectField = new ImportField(LM.nameForeignJoinProject);
        nameNativeManagerProjectField = new ImportField(LM.nameNativeManagerProject);
        nameNativeGenitiveManagerProjectField = new ImportField(LM.nameNativeGenitiveManagerProject);
        nameNativeDativusManagerProjectField = new ImportField(LM.nameNativeDativusManagerProject);
        nameNativeAblateManagerProjectField = new ImportField(LM.nameNativeAblateManagerProject);
        nameForeignManagerProjectField = new ImportField(LM.nameForeignManagerProject);
        nativeProblemProjectField = new ImportField(LM.nativeProblemProject);
        foreignProblemProjectField = new ImportField(LM.foreignProblemProject);
        nativeInnovativeProjectField = new ImportField(LM.nativeInnovativeProject);
        foreignInnovativeProjectField = new ImportField(LM.foreignInnovativeProject);
        projectTypeProjectField = new ImportField(LM.baseLM.staticID);
        projectActionProjectField = new ImportField(LM.baseLM.staticID);
        nativeSubstantiationProjectTypeField = new ImportField(LM.nativeSubstantiationProjectType);
        foreignSubstantiationProjectTypeField = new ImportField(LM.foreignSubstantiationProjectType);
        isOtherClusterProjectField = new ImportField(LM.isOtherClusterProject);
        nativeSubstantiationOtherClusterProjectField = new ImportField(LM.nativeSubstantiationOtherClusterProject);
        foreignSubstantiationOtherClusterProjectField = new ImportField(LM.foreignSubstantiationOtherClusterProject);
        isOwnedEquipmentProjectField = new ImportField(LM.isOwnedEquipmentProject);
        isAvailableEquipmentProjectField = new ImportField(LM.isAvailableEquipmentProject);
        isTransferEquipmentProjectField = new ImportField(LM.isTransferEquipmentProject);
        descriptionTransferEquipmentProjectField = new ImportField(LM.descriptionTransferEquipmentProject);
        ownerEquipmentProjectField = new ImportField(LM.ownerEquipmentProject);
        isPlanningEquipmentProjectField = new ImportField(LM.isPlanningEquipmentProject);
        specificationEquipmentProjectField = new ImportField(LM.specificationEquipmentProject);
        isSeekEquipmentProjectField = new ImportField(LM.isSeekEquipmentProject);
        descriptionEquipmentProjectField = new ImportField(LM.descriptionEquipmentProject);
        isOtherEquipmentProjectField = new ImportField(LM.isOtherEquipmentProject);
        commentEquipmentProjectField = new ImportField(LM.commentEquipmentProject);

        isReturnInvestmentsProjectField = new ImportField(LM.isReturnInvestmentsProject);
        nameReturnInvestorProjectField = new ImportField(LM.nameReturnInvestorProject);
        amountReturnFundsProjectField = new ImportField(LM.amountReturnFundsProject);
        isNonReturnInvestmentsProjectField = new ImportField(LM.isNonReturnInvestmentsProject);
        isCapitalInvestmentProjectField = new ImportField(LM.isCapitalInvestmentProject);
        isPropertyInvestmentProjectField = new ImportField(LM.isPropertyInvestmentProject);
        isGrantsProjectField = new ImportField(LM.isGrantsProject);
        isOtherNonReturnInvestmentsProjectField = new ImportField(LM.isOtherNonReturnInvestmentsProject);

        nameNonReturnInvestorProjectField = new ImportField(LM.nameNonReturnInvestorProject);
        amountNonReturnFundsProjectField = new ImportField(LM.amountNonReturnFundsProject);
        commentOtherNonReturnInvestmentsProjectField = new ImportField(LM.commentOtherNonReturnInvestmentsProject);
        isOwnFundsProjectField = new ImportField(LM.isOwnFundsProject);
        amountOwnFundsProjectField = new ImportField(LM.amountOwnFundsProject);
        isPlanningSearchSourceProjectField = new ImportField(LM.isPlanningSearchSourceProject);
        amountFundsProjectField = new ImportField(LM.amountFundsProject);
        isOtherSoursesProjectField = new ImportField(LM.isOtherSoursesProject);
        commentOtherSoursesProjectField = new ImportField(LM.commentOtherSoursesProject);
        fillNativeProjectField = new ImportField(LM.fillNativeProject);
        fillForeignProjectField = new ImportField(LM.fillForeignProject);
        updateDateProjectField = new ImportField(LM.updateDateProject);
        isConsultingCenterQuestionProjectField = new ImportField(LM.isConsultingCenterQuestionProject);
        isConsultingCenterCommentProjectField = new ImportField(LM.isConsultingCenterCommentProject);
        consultingCenterCommentProjectField = new ImportField(LM.consultingCenterCommentProject);

        nameNativeClusterField = new ImportField(LM.nameNativeCluster);
        inProjectClusterField = new ImportField(LM.inProjectCluster);
        numberCurrentClusterField = new ImportField(LM.numberCluster);
        nativeSubstantiationProjectClusterField = new ImportField(LM.nativeSubstantiationProjectCluster);
        foreignSubstantiationProjectClusterField = new ImportField(LM.foreignSubstantiationProjectCluster);

        nameNativeClaimerField = new ImportField(LM.nameNativeJoinClaimer);
        nameForeignClaimerField = new ImportField(LM.nameForeignJoinClaimer);
        firmNameNativeClaimerField = new ImportField(LM.firmNameNativeClaimer);
        firmNameForeignClaimerField = new ImportField(LM.firmNameForeignClaimer);
        phoneClaimerField = new ImportField(LM.phoneClaimer);
        addressClaimerField = new ImportField(LM.addressClaimer);
        siteClaimerField = new ImportField(LM.siteClaimer);
        emailClaimerField = new ImportField(LM.emailClaimer);
        emailFirmClaimerField = new ImportField(LM.emailFirmClaimer);
        OGRNClaimerField = new ImportField(LM.OGRNClaimer);
        INNClaimerField = new ImportField(LM.INNClaimer);
        fileStatementClaimerField = new ImportField(LM.statementClaimer);
        fileConstituentClaimerField = new ImportField(LM.constituentClaimer);
        fileExtractClaimerField = new ImportField(LM.extractClaimer);
        fileNativeSummaryProjectField = new ImportField(LM.fileNativeSummaryProject);
        fileForeignSummaryProjectField = new ImportField(LM.fileForeignSummaryProject);
        fileMinutesOfMeetingExpertCollegiumProjectField = new ImportField(LM.fileMinutesOfMeetingExpertCollegiumProject);
        fileNativeTechnicalDescriptionProjectField = new ImportField(LM.fileNativeTechnicalDescriptionProject);
        fileForeignTechnicalDescriptionProjectField = new ImportField(LM.fileForeignTechnicalDescriptionProject);
        fileRoadMapProjectField = new ImportField(LM.fileNativeRoadMapProject);
        fileForeignRoadMapProjectField = new ImportField(LM.fileForeignRoadMapProject);

        nativeTypePatentField = new ImportField(LM.nativeTypePatent);
        foreignTypePatentField = new ImportField(LM.foreignTypePatent);
        nativeNumberPatentField = new ImportField(LM.nativeNumberPatent);
        foreignNumberPatentField = new ImportField(LM.foreignNumberPatent);
        datePatentField = new ImportField(LM.priorityDatePatent);
        isOwnedPatentField = new ImportField(LM.isOwned);
        ownerPatentField = new ImportField(LM.ownerPatent);
        ownerTypePatentField = new ImportField(LM.baseLM.staticID);
        isValuatedPatentField = new ImportField(LM.isValuated);
        valuatorPatentField = new ImportField(LM.valuatorPatent);
        fileIntentionOwnerPatentField = new ImportField(LM.fileIntentionOwnerPatent);
        fileActValuationPatentField = new ImportField(LM.fileActValuationPatent);

        fullNameAcademicField = new ImportField(LM.fullNameAcademic);
        institutionAcademicField = new ImportField(LM.institutionAcademic);
        titleAcademicField = new ImportField(LM.titleAcademic);
        fileDocumentConfirmingAcademicField = new ImportField(LM.fileDocumentConfirmingAcademic);
        fileDocumentEmploymentAcademicField = new ImportField(LM.fileDocumentEmploymentAcademic);

        fullNameNonRussianSpecialistField = new ImportField(LM.fullNameNonRussianSpecialist);
        organizationNonRussianSpecialistField = new ImportField(LM.organizationNonRussianSpecialist);
        titleNonRussianSpecialistField = new ImportField(LM.titleNonRussianSpecialist);
        fileForeignResumeNonRussianSpecialistField = new ImportField(LM.fileForeignResumeNonRussianSpecialist);
        fileNativeResumeNonRussianSpecialistField = new ImportField(LM.fileNativeResumeNonRussianSpecialist);
        filePassportNonRussianSpecialistField = new ImportField(LM.filePassportNonRussianSpecialist);
        fileStatementNonRussianSpecialistField = new ImportField(LM.fileStatementNonRussianSpecialist);

        properties = new ArrayList<ImportProperty<?>>();
        propertiesNative = new ArrayList<ImportProperty<?>>();
        propertiesForeign = new ArrayList<ImportProperty<?>>();
        propertiesPatent = new ArrayList<ImportProperty<?>>();
        propertiesPatentNative = new ArrayList<ImportProperty<?>>();
        propertiesPatentForeign = new ArrayList<ImportProperty<?>>();
        propertiesCluster = new ArrayList<ImportProperty<?>>();
        propertiesClusterNative = new ArrayList<ImportProperty<?>>();
        propertiesClusterForeign = new ArrayList<ImportProperty<?>>();
        propertiesAcademic = new ArrayList<ImportProperty<?>>();
        propertiesNonRussianSpecialist = new ArrayList<ImportProperty<?>>();

        projectKey = new ImportKey(LM.project, LM.sidToProject.getMapping(projectIdField));
        properties.add(new ImportProperty(projectIdField, LM.sidProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isOwnedEquipmentProjectField, LM.isOwnedEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isAvailableEquipmentProjectField, LM.isAvailableEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isTransferEquipmentProjectField, LM.isTransferEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(descriptionTransferEquipmentProjectField, LM.descriptionTransferEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(ownerEquipmentProjectField, LM.ownerEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isPlanningEquipmentProjectField, LM.isPlanningEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(specificationEquipmentProjectField, LM.specificationEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isSeekEquipmentProjectField, LM.isSeekEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(descriptionEquipmentProjectField, LM.descriptionEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isOtherEquipmentProjectField, LM.isOtherEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(commentEquipmentProjectField, LM.commentEquipmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fillNativeProjectField, LM.fillNativeProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fillForeignProjectField, LM.fillForeignProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isConsultingCenterQuestionProjectField, LM.isConsultingCenterQuestionProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isConsultingCenterCommentProjectField, LM.isConsultingCenterCommentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(consultingCenterCommentProjectField, LM.consultingCenterCommentProject.getMapping(projectKey)));

        properties.add(new ImportProperty(isReturnInvestmentsProjectField, LM.isReturnInvestmentsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameReturnInvestorProjectField, LM.nameReturnInvestorProject.getMapping(projectKey)));
        properties.add(new ImportProperty(amountReturnFundsProjectField, LM.amountReturnFundsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isNonReturnInvestmentsProjectField, LM.isNonReturnInvestmentsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(nameNonReturnInvestorProjectField, LM.nameNonReturnInvestorProject.getMapping(projectKey)));
        properties.add(new ImportProperty(amountNonReturnFundsProjectField, LM.amountNonReturnFundsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(commentOtherNonReturnInvestmentsProjectField, LM.commentOtherNonReturnInvestmentsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isCapitalInvestmentProjectField, LM.isCapitalInvestmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isPropertyInvestmentProjectField, LM.isPropertyInvestmentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isGrantsProjectField, LM.isGrantsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isOtherNonReturnInvestmentsProjectField, LM.isOtherNonReturnInvestmentsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isOwnFundsProjectField, LM.isOwnFundsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(amountOwnFundsProjectField, LM.amountOwnFundsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isPlanningSearchSourceProjectField, LM.isPlanningSearchSourceProject.getMapping(projectKey)));
        properties.add(new ImportProperty(amountFundsProjectField, LM.amountFundsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isOtherSoursesProjectField, LM.isOtherSoursesProject.getMapping(projectKey)));
        properties.add(new ImportProperty(commentOtherSoursesProjectField, LM.commentOtherSoursesProject.getMapping(projectKey)));
        properties.add(new ImportProperty(updateDateProjectField, LM.updateDateProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fileMinutesOfMeetingExpertCollegiumProjectField, LM.fileMinutesOfMeetingExpertCollegiumProject.getMapping(projectKey)));

        projectTypeProjectKey = new ImportKey(LM.projectType, LM.projectTypeToSID.getMapping(projectTypeProjectField));
        properties.add(new ImportProperty(projectTypeProjectField, LM.projectTypeProject.getMapping(projectKey),
                LM.baseLM.object(LM.projectType).getMapping(projectTypeProjectKey)));

        projectActionProjectKey = new ImportKey(LM.projectAction, LM.projectActionToSID.getMapping(projectActionProjectField));
        properties.add(new ImportProperty(projectActionProjectField, LM.projectActionProject.getMapping(projectKey),
                LM.baseLM.object(LM.projectAction).getMapping(projectActionProjectKey)));

        claimerKey = new ImportKey(LM.claimer, contactLM.contactEmail.getMapping(emailClaimerField));
        properties.add(new ImportProperty(emailClaimerField, contactLM.emailContact.getMapping(claimerKey)));

        propertiesFullClaimer = new ArrayList<ImportProperty<?>>();
        propertiesFullClaimer.add(new ImportProperty(phoneClaimerField, LM.phoneClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(addressClaimerField, LM.addressClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(siteClaimerField, LM.siteClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(emailFirmClaimerField, LM.emailFirmClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(OGRNClaimerField, LM.OGRNClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(INNClaimerField, LM.INNClaimer.getMapping(claimerKey)));

        propertiesFullClaimer.add(new ImportProperty(fileStatementClaimerField, LM.statementClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(fileConstituentClaimerField, LM.constituentClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(fileExtractClaimerField, LM.extractClaimer.getMapping(claimerKey)));

        propertyDate = new ImportProperty(dateProjectField, LM.dateJoinProject.getMapping(projectKey));
        propertyStatusDate = new ImportProperty(dateStatusProjectField, LM.statusDateProject.getMapping(projectKey));

        propertiesNative.add(new ImportProperty(nameNativeProjectField, LM.nameNativeJoinProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeManagerProjectField, LM.nameNativeManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeGenitiveManagerProjectField, LM.nameNativeGenitiveManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeDativusManagerProjectField, LM.nameNativeDativusManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeAblateManagerProjectField, LM.nameNativeAblateManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeProblemProjectField, LM.nativeProblemProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeInnovativeProjectField, LM.nativeInnovativeProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeSubstantiationProjectTypeField, LM.nativeSubstantiationProjectType.getMapping(projectKey)));

        propertiesNative.add(new ImportProperty(fileNativeSummaryProjectField, LM.fileNativeSummaryProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(fileNativeTechnicalDescriptionProjectField, LM.fileNativeTechnicalDescriptionProject.getMapping(projectKey)));

        propertiesNative.add(new ImportProperty(fileRoadMapProjectField, LM.fileNativeRoadMapProject.getMapping(projectKey)));

        properties.add(new ImportProperty(emailClaimerField, LM.claimerProject.getMapping(projectKey),
                LM.baseLM.object(LM.claimer).getMapping(claimerKey)));

        propertiesFullClaimerNative = new ArrayList<ImportProperty<?>>();
        propertiesFullClaimerNative.add(new ImportProperty(nameNativeClaimerField, LM.nameNativeJoinClaimer.getMapping(claimerKey)));
        propertiesFullClaimerNative.add(new ImportProperty(firmNameNativeClaimerField, LM.firmNameNativeClaimer.getMapping(claimerKey)));

        propertiesForeign.add(new ImportProperty(nameForeignProjectField, LM.nameForeignJoinProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(nameForeignManagerProjectField, LM.nameForeignManagerProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignProblemProjectField, LM.foreignProblemProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignInnovativeProjectField, LM.foreignInnovativeProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignSubstantiationProjectTypeField, LM.foreignSubstantiationProjectType.getMapping(projectKey)));

        propertiesForeign.add(new ImportProperty(fileForeignSummaryProjectField, LM.fileForeignSummaryProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(fileForeignTechnicalDescriptionProjectField, LM.fileForeignTechnicalDescriptionProject.getMapping(projectKey)));

        propertiesForeign.add(new ImportProperty(fileForeignRoadMapProjectField, LM.fileForeignRoadMapProject.getMapping(projectKey)));

        propertiesFullClaimerForeign = new ArrayList<ImportProperty<?>>();
        propertiesFullClaimerForeign.add(new ImportProperty(nameForeignClaimerField, LM.nameForeignJoinClaimer.getMapping(claimerKey)));
        propertiesFullClaimerForeign.add(new ImportProperty(firmNameForeignClaimerField, LM.firmNameForeignClaimer.getMapping(claimerKey)));

        patentKey = new ImportKey(LM.patent, LM.nativeNumberSIDToPatent.getMapping(nativeNumberPatentField, projectIdField));
        propertiesPatent.add(new ImportProperty(nativeNumberPatentField, LM.nativeNumberPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(datePatentField, LM.priorityDatePatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(isOwnedPatentField, LM.isOwned.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(ownerPatentField, LM.ownerPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(isValuatedPatentField, LM.isValuated.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(valuatorPatentField, LM.valuatorPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(fileIntentionOwnerPatentField, LM.fileIntentionOwnerPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(fileActValuationPatentField, LM.fileActValuationPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(projectIdField, LM.projectPatent.getMapping(patentKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));

        ownerTypePatentKey = new ImportKey(LM.ownerType, LM.ownerTypeToSID.getMapping(ownerTypePatentField));
        propertiesPatent.add(new ImportProperty(ownerTypePatentField, LM.ownerTypePatent.getMapping(patentKey),
                LM.baseLM.object(LM.ownerType).getMapping(ownerTypePatentKey)));

        propertiesPatentNative.add(new ImportProperty(nativeTypePatentField, LM.nativeTypePatent.getMapping(patentKey)));
        propertiesPatentForeign.add(new ImportProperty(foreignTypePatentField, LM.foreignTypePatent.getMapping(patentKey)));
        propertiesPatentForeign.add(new ImportProperty(foreignNumberPatentField, LM.foreignNumberPatent.getMapping(patentKey)));

        clusterKey = new ImportKey(LM.cluster, LM.nameNativeToCluster.getMapping(nameNativeClusterField));
        propertiesCluster.add(new ImportProperty(inProjectClusterField, LM.inProjectCluster.getMapping(projectKey, clusterKey)));
        propertiesCluster.add(new ImportProperty(nameNativeClusterField, LM.nameNativeCluster.getMapping(clusterKey)));

        propertiesClusterNative.add(new ImportProperty(nativeSubstantiationProjectClusterField, LM.nativeSubstantiationProjectCluster.getMapping(projectKey, clusterKey)));
        propertiesClusterForeign.add(new ImportProperty(foreignSubstantiationProjectClusterField, LM.foreignSubstantiationProjectCluster.getMapping(projectKey, clusterKey)));

        propertyOtherCluster = new ImportProperty(isOtherClusterProjectField, LM.isOtherClusterProject.getMapping(projectKey));
        propertyOtherClusterNative = new ImportProperty(nativeSubstantiationOtherClusterProjectField, LM.nativeSubstantiationOtherClusterProject.getMapping(projectKey));
        propertyOtherClusterForeign = new ImportProperty(foreignSubstantiationOtherClusterProjectField, LM.foreignSubstantiationOtherClusterProject.getMapping(projectKey));


        academicKey = new ImportKey(LM.academic, LM.fullNameSIDToAcademic.getMapping(fullNameAcademicField, projectIdField));
        propertiesAcademic.add(new ImportProperty(fullNameAcademicField, LM.fullNameAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(institutionAcademicField, LM.institutionAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(titleAcademicField, LM.titleAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(fileDocumentConfirmingAcademicField, LM.fileDocumentConfirmingAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(fileDocumentEmploymentAcademicField, LM.fileDocumentEmploymentAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(projectIdField, LM.projectAcademic.getMapping(academicKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));

        nonRussianSpecialistKey = new ImportKey(LM.nonRussianSpecialist, LM.fullNameSIDToNonRussianSpecialist.getMapping(fullNameNonRussianSpecialistField, projectIdField));
        propertiesNonRussianSpecialist.add(new ImportProperty(fullNameNonRussianSpecialistField, LM.fullNameNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(organizationNonRussianSpecialistField, LM.organizationNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(titleNonRussianSpecialistField, LM.titleNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(fileForeignResumeNonRussianSpecialistField, LM.fileForeignResumeNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(fileNativeResumeNonRussianSpecialistField, LM.fileNativeResumeNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(filePassportNonRussianSpecialistField, LM.filePassportNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(fileStatementNonRussianSpecialistField, LM.fileStatementNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
        propertiesNonRussianSpecialist.add(new ImportProperty(projectIdField, LM.projectNonRussianSpecialist.getMapping(nonRussianSpecialistKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));
    }

    private void initNewFieldsNProperties(boolean fillNative) {
        dateProjectField = new ImportField(LM.date);
        dateStatusProjectField = new ImportField(LM.statusDateProject);
        projectIdField = new ImportField(LM.sidProject);
        nameNativeProjectField = new ImportField(LM.nameNativeJoinProject);
        nameForeignProjectField = new ImportField(LM.nameForeignJoinProject);
        nativeProblemProjectField = new ImportField(LM.nativeProblemProject);
        foreignProblemProjectField = new ImportField(LM.foreignProblemProject);
        nativeInnovativeProjectField = new ImportField(LM.nativeInnovativeProject);
        foreignInnovativeProjectField = new ImportField(LM.foreignInnovativeProject);

        nameNativeManagerProjectField = new ImportField(LM.nameNativeManagerProject);
        nameNativeGenitiveManagerProjectField = new ImportField(LM.nameNativeGenitiveManagerProject);
        nameNativeDativusManagerProjectField = new ImportField(LM.nameNativeDativusManagerProject);
        nameNativeAblateManagerProjectField = new ImportField(LM.nameNativeAblateManagerProject);
        nameForeignManagerProjectField = new ImportField(LM.nameForeignManagerProject);

        nativeCommentMissionProjectField = new ImportField(LM.nativeCommentMissionProject);
        foreignCommentMissionProjectField = new ImportField(LM.foreignCommentMissionProject);
        nativeResumeProjectField = new ImportField(LM.nativeResumeProject);
        foreignResumeProjectField = new ImportField(LM.foreignResumeProject);
        nameNativeContactProjectField = new ImportField(LM.nameNativeContactProject);
        nameForeignContactProjectField = new ImportField(LM.nameForeignContactProject);
        phoneContactProjectField = new ImportField(LM.phoneContactProject);
        emailContactProjectField = new ImportField(LM.emailContactProject);

        nativeMarketTrendsProjectField = new ImportField(LM.nativeMarketTrendsProject);
        foreignMarketTrendsProjectField = new ImportField(LM.foreignMarketTrendsProject);
        nativeRelevanceProjectField = new ImportField(LM.nativeRelevanceProject);
        foreignRelevanceProjectField = new ImportField(LM.foreignRelevanceProject);
        nativeBasicTechnologyProjectField = new ImportField(LM.nativeBasicTechnologyProject);
        foreignBasicTechnologyProjectField = new ImportField(LM.foreignBasicTechnologyProject);
        linksMarketTrendsProjectField = new ImportField(LM.linksMarketTrendsProject);
        linksAnalogProjectField = new ImportField(LM.linksAnalogProject);

        nativeCaseStudiesProjectField = new ImportField(LM.nativeCaseStudiesProject);
        foreignCaseStudiesProjectField = new ImportField(LM.foreignCaseStudiesProject);
        nativeCharacteristicsAnaloguesProjectField = new ImportField(LM.nativeCharacteristicsAnaloguesProject);
        foreignCharacteristicsAnaloguesProjectField = new ImportField(LM.foreignCharacteristicsAnaloguesProject);
        nativeCompaniesAnaloguesProjectField = new ImportField(LM.nativeCompaniesAnaloguesProject);
        foreignCompaniesAnaloguesProjectField = new ImportField(LM.foreignCompaniesAnaloguesProject);
        nativeMarketIntroductionProjectField = new ImportField(LM.nativeMarketIntroductionProject);
        foreignMarketIntroductionProjectField = new ImportField(LM.foreignMarketIntroductionProject);
        linksMarketIntroductionProjectField = new ImportField(LM.linksMarketIntroductionProject);
        linksForeignMarketIntroductionProjectField = new ImportField(LM.linksForeignMarketIntroductionProject);

        nameNativeSpecialistField = new ImportField(LM.nameNativeSpecialist);
        nameForeignSpecialistField = new ImportField(LM.nameForeignSpecialist);
        nativePostSpecialistField = new ImportField(LM.nativePostSpecialist);
        foreignPostSpecialistField = new ImportField(LM.foreignPostSpecialist);
        nativeFunctionSpecialistField = new ImportField(LM.nativeFunctionSpecialist);
        foreignFunctionSpecialistField = new ImportField(LM.foreignFunctionSpecialist);
        nativeScopeSpecialistField = new ImportField(LM.nativeScopeSpecialist);
        foreignScopeSpecialistField = new ImportField(LM.foreignScopeSpecialist);
        nativeExperienceSpecialistField = new ImportField(LM.nativeExperienceSpecialist);
        foreignExperienceSpecialistField = new ImportField(LM.foreignExperienceSpecialist);
        nativeTitleSpecialistField = new ImportField(LM.nativeTitleSpecialist);
        foreignTitleSpecialistField = new ImportField(LM.foreignTitleSpecialist);
        nativeWorkSpecialistField = new ImportField(LM.nativeWorkSpecialist);
        foreignWorkSpecialistField = new ImportField(LM.foreignWorkSpecialist);
        nativePublicationsSpecialistField = new ImportField(LM.nativePublicationsSpecialist);
        foreignPublicationsSpecialistField = new ImportField(LM.foreignPublicationsSpecialist);
        nativeCitationSpecialistField = new ImportField(LM.nativeCitationSpecialist);
        foreignCitationSpecialistField = new ImportField(LM.foreignCitationSpecialist);
        nativeIntellectualPropertySpecialistField = new ImportField(LM.nativeIntellectualPropertySpecialist);
        foreignIntellectualPropertySpecialistField = new ImportField(LM.foreignIntellectualPropertySpecialist);
        filePassportSpecialistField = new ImportField(LM.fileStatementSpecialist);
        fileStatementSpecialistField = new ImportField(LM.filePassportSpecialist);
        nativeMileStoneYearField = new ImportField(LM.nativeMileStoneYear);
        nativeMileStoneField = new ImportField(LM.nativeMileStone);
        orderNumberMileStoneField = new ImportField(LM.orderNumberMileStone);
        nativeResearchDescriptionTypeMileStoneMileStoneField = new ImportField(LM.nativeDescriptionTypeMileStoneMileStoneMileStoneYear);
        nativeProductCreationDescriptionTypeMileStoneMileStoneField = new ImportField(LM.nativeDescriptionTypeMileStoneMileStoneMileStoneYear);
        nativePlanOnHiringDescriptionTypeMileStoneMileStoneField = new ImportField(LM.nativeDescriptionTypeMileStoneMileStoneMileStoneYear);
        nativeLicensingDescriptionTypeMileStoneMileStoneField = new ImportField(LM.nativeDescriptionTypeMileStoneMileStoneMileStoneYear);
        nativePromotionDescriptionTypeMileStoneMileStoneField = new ImportField(LM.nativeDescriptionTypeMileStoneMileStoneMileStoneYear);
        nativeSellingDescriptionTypeMileStoneMileStoneField = new ImportField(LM.nativeDescriptionTypeMileStoneMileStoneMileStoneYear);
        foreignResearchDescriptionTypeMileStoneMileStoneField = new ImportField(LM.foreignDescriptionTypeMileStoneMileStoneMileStoneYear);
        foreignProductCreationDescriptionTypeMileStoneMileStoneField = new ImportField(LM.foreignDescriptionTypeMileStoneMileStoneMileStoneYear);
        foreignPlanOnHiringDescriptionTypeMileStoneMileStoneField = new ImportField(LM.foreignDescriptionTypeMileStoneMileStoneMileStoneYear);
        foreignLicensingDescriptionTypeMileStoneMileStoneField = new ImportField(LM.foreignDescriptionTypeMileStoneMileStoneMileStoneYear);
        foreignPromotionDescriptionTypeMileStoneMileStoneField = new ImportField(LM.foreignDescriptionTypeMileStoneMileStoneMileStoneYear);
        foreignSellingDescriptionTypeMileStoneMileStoneField = new ImportField(LM.foreignDescriptionTypeMileStoneMileStoneMileStoneYear);
        nativeHistoryProjectField = new ImportField(LM.nativeHistoryProject);
        foreignHistoryProjectField = new ImportField(LM.foreignHistoryProject);
        nativeDynamicsProjectField = new ImportField(LM.nativeDynamicsProject);
        foreignDynamicsProjectField = new ImportField(LM.foreignDynamicsProject);
        nativeGrantsProjectField = new ImportField(LM.nativeGrantsProject);
        foreignGrantsProjectField = new ImportField(LM.foreignGrantsProject);
        nativeLaboratoryProjectField = new ImportField(LM.nativeLaboratoryProject);
        foreignLaboratoryProjectField = new ImportField(LM.foreignLaboratoryProject);
        nativeInvestmentProjectField = new ImportField(LM.nativeInvestmentProject);
        foreignInvestmentProjectField = new ImportField(LM.foreignInvestmentProject);
        nativeResultsProjectField = new ImportField(LM.nativeResultsProject);
        foreignResultsProjectField = new ImportField(LM.foreignResultsProject);
        nativeGeneralizedPlanProjectField = new ImportField(LM.nativeGeneralizedPlanProject);
        foreignGeneralizedPlanProjectField = new ImportField(LM.foreignGeneralizedPlanProject);
        fileRoadMapProjectField = new ImportField(LM.fileNativeRoadMapProject);
        fileForeignRoadMapProjectField = new ImportField(LM.fileForeignRoadMapProject);
        fileNativeTechnicalDescriptionProjectField = new ImportField(LM.fileNativeTechnicalDescriptionProject);
        fileForeignTechnicalDescriptionProjectField = new ImportField(LM.fileForeignTechnicalDescriptionProject);
        fileMinutesOfMeetingExpertCollegiumProjectField = new ImportField(LM.fileMinutesOfMeetingExpertCollegiumProject);
        fileWrittenConsentClaimerProjectField = new ImportField(LM.fileWrittenConsentClaimerProject);

        updateDateProjectField = new ImportField(LM.updateDateProject);
        projectMissionProjectField = new ImportField(LM.baseLM.staticID);
        projectScheduleProjectField = new ImportField(LM.baseLM.staticID);
        projectActionProjectField = new ImportField(LM.baseLM.staticID);

        isOtherClusterProjectField = new ImportField(LM.isOtherClusterProject);
        nativeSubstantiationOtherClusterProjectField = new ImportField(LM.nativeSubstantiationOtherClusterProject);
        foreignSubstantiationOtherClusterProjectField = new ImportField(LM.foreignSubstantiationOtherClusterProject);

        fillNativeProjectField = new ImportField(LM.fillNativeProject);
        fillForeignProjectField = new ImportField(LM.fillForeignProject);
        isConsultingCenterQuestionProjectField = new ImportField(LM.isConsultingCenterQuestionProject);
        isConsultingCenterCommentProjectField = new ImportField(LM.isConsultingCenterCommentProject);
        consultingCenterCommentProjectField = new ImportField(LM.consultingCenterCommentProject);

        nameNativeClusterField = new ImportField(LM.nameNativeCluster);
        inProjectClusterField = new ImportField(LM.inProjectCluster);
        inClaimerProjectClusterField = new ImportField(LM.inClaimerProjectCluster);
        numberCurrentClusterField = new ImportField(LM.numberCluster);
        nativeSubstantiationProjectClusterField = new ImportField(LM.nativeSubstantiationProjectCluster);
        foreignSubstantiationProjectClusterField = new ImportField(LM.foreignSubstantiationProjectCluster);

        nameNativeClaimerField = new ImportField(LM.nameNativeJoinClaimer);
        nameForeignClaimerField = new ImportField(LM.nameForeignJoinClaimer);
        firmNameNativeClaimerField = new ImportField(LM.firmNameNativeClaimer);
        firmNameForeignClaimerField = new ImportField(LM.firmNameForeignClaimer);
        phoneClaimerField = new ImportField(LM.phoneClaimer);
        addressClaimerField = new ImportField(LM.addressClaimer);
        siteClaimerField = new ImportField(LM.siteClaimer);
        emailClaimerField = new ImportField(LM.emailClaimer);
        emailFirmClaimerField = new ImportField(LM.emailFirmClaimer);
        OGRNClaimerField = new ImportField(LM.OGRNClaimer);
        INNClaimerField = new ImportField(LM.INNClaimer);
        fileStatementClaimerField = new ImportField(LM.statementClaimer);
        fileConstituentClaimerField = new ImportField(LM.constituentClaimer);
        fileExtractClaimerField = new ImportField(LM.extractClaimer);

        nativeTypePatentField = new ImportField(LM.nativeTypePatent);
        foreignTypePatentField = new ImportField(LM.foreignTypePatent);
        nativeNumberPatentField = new ImportField(LM.nativeNumberPatent);
        linksPatentField = new ImportField(LM.linksPatent);

        nativeCommentResearchField = new ImportField(LM.nativeCommentResearch);
        foreignCommentResearchField = new ImportField(LM.foreignCommentResearch);
        dataResearchField = new ImportField(LM.dataResearch);

        nativePublicationsField = new ImportField(LM.nativePublications);
        foreignPublicationsField = new ImportField(LM.foreignPublications);
        nativeAuthorPublicationsField = new ImportField(LM.nativeAuthorPublications);
        foreignAuthorPublicationsField = new ImportField(LM.foreignAuthorPublications);
        nativeEditionPublicationsField = new ImportField(LM.nativeEditionPublications);
        foreignEditionPublicationsField = new ImportField(LM.foreignEditionPublications);
        datePublicationsField = new ImportField(LM.datePublications);
        nativeLinksPublicationsField = new ImportField(LM.nativeLinksPublications);

        nativeProjectCommercializationField = new ImportField(LM.nativeProjectCommercialization);
        foreignProjectCommercializationField = new ImportField(LM.foreignProjectCommercialization);
        nativeCommentProjectCommercializationField = new ImportField(LM.nativeCommentProjectCommercialization);
        foreignCommentProjectCommercializationField = new ImportField(LM.foreignCommentProjectCommercialization);

        nativeProjectAnaloguesField = new ImportField(LM.nativeProjectAnalogues);
        foreignProjectAnaloguesField = new ImportField(LM.foreignProjectAnalogues);
        nativeDescriptionProjectAnaloguesField = new ImportField(LM.nativeDescriptionProjectAnalogues);
        foreignDescriptionProjectAnaloguesField = new ImportField(LM.foreignDescriptionProjectAnalogues);
        nativeCharacteristicsProjectAnaloguesField = new ImportField(LM.nativeCharacteristicsProjectAnalogues);
        foreignCharacteristicsProjectAnaloguesField = new ImportField(LM.foreignCharacteristicsProjectAnalogues);

        nativeProjectObjectivesField = new ImportField(LM.nativeProjectObjectives);
        foreignProjectObjectivesField = new ImportField(LM.foreignProjectObjectives);

        properties = new ArrayList<ImportProperty<?>>();
        propertiesNative = new ArrayList<ImportProperty<?>>();
        propertiesForeign = new ArrayList<ImportProperty<?>>();
        propertiesPatent = new ArrayList<ImportProperty<?>>();
        propertiesPatentNative = new ArrayList<ImportProperty<?>>();
        propertiesPatentForeign = new ArrayList<ImportProperty<?>>();
        propertiesCluster = new ArrayList<ImportProperty<?>>();
        propertiesClusterNative = new ArrayList<ImportProperty<?>>();
        propertiesClusterForeign = new ArrayList<ImportProperty<?>>();
        propertiesResearch = new ArrayList<ImportProperty<?>>();
        propertiesResearchNative = new ArrayList<ImportProperty<?>>();
        propertiesResearchForeign = new ArrayList<ImportProperty<?>>();
        propertiesPublications = new ArrayList<ImportProperty<?>>();
        propertiesPublicationsNative = new ArrayList<ImportProperty<?>>();
        propertiesPublicationsForeign = new ArrayList<ImportProperty<?>>();
        propertiesCommercialization = new ArrayList<ImportProperty<?>>();
        propertiesCommercializationNative = new ArrayList<ImportProperty<?>>();
        propertiesCommercializationForeign = new ArrayList<ImportProperty<?>>();
        propertiesAnalogues = new ArrayList<ImportProperty<?>>();
        propertiesAnaloguesNative = new ArrayList<ImportProperty<?>>();
        propertiesAnaloguesForeign = new ArrayList<ImportProperty<?>>();
        propertiesSpecialist = new ArrayList<ImportProperty<?>>();
        propertiesSpecialistNative = new ArrayList<ImportProperty<?>>();
        propertiesSpecialistForeign = new ArrayList<ImportProperty<?>>();
        propertiesMileStone = new ArrayList<ImportProperty<?>>();
        propertiesMileStoneNative = new ArrayList<ImportProperty<?>>();
        propertiesMileStoneForeign = new ArrayList<ImportProperty<?>>();
        propertiesObjectives = new ArrayList<ImportProperty<?>>();
        propertiesObjectivesNative = new ArrayList<ImportProperty<?>>();
        propertiesObjectivesForeign = new ArrayList<ImportProperty<?>>();

        projectKey = new ImportKey(LM.project, LM.sidToProject.getMapping(projectIdField));
        properties.add(new ImportProperty(projectIdField, LM.sidProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fillNativeProjectField, LM.fillNativeProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fillForeignProjectField, LM.fillForeignProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isConsultingCenterQuestionProjectField, LM.isConsultingCenterQuestionProject.getMapping(projectKey)));
        properties.add(new ImportProperty(isConsultingCenterCommentProjectField, LM.isConsultingCenterCommentProject.getMapping(projectKey)));
        properties.add(new ImportProperty(consultingCenterCommentProjectField, LM.consultingCenterCommentProject.getMapping(projectKey)));

        properties.add(new ImportProperty(phoneContactProjectField, LM.phoneContactProject.getMapping(projectKey)));
        properties.add(new ImportProperty(emailContactProjectField, LM.emailContactProject.getMapping(projectKey)));
        properties.add(new ImportProperty(linksMarketTrendsProjectField, LM.linksMarketTrendsProject.getMapping(projectKey)));
        properties.add(new ImportProperty(linksAnalogProjectField, LM.linksAnalogProject.getMapping(projectKey)));
        properties.add(new ImportProperty(updateDateProjectField, LM.updateDateProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fileMinutesOfMeetingExpertCollegiumProjectField, LM.fileMinutesOfMeetingExpertCollegiumProject.getMapping(projectKey)));
        properties.add(new ImportProperty(fileWrittenConsentClaimerProjectField, LM.fileWrittenConsentClaimerProject.getMapping(projectKey)));

        projectMissionProjectKey = new ImportKey(LM.projectMission, LM.projectMissionToSID.getMapping(projectMissionProjectField));
        properties.add(new ImportProperty(projectMissionProjectField, LM.projectMissionProject.getMapping(projectKey),
                LM.baseLM.object(LM.projectMission).getMapping(projectMissionProjectKey)));

        projectScheduleProjectKey = new ImportKey(LM.projectSchedule, LM.projectScheduleToSID.getMapping(projectScheduleProjectField));
        properties.add(new ImportProperty(projectScheduleProjectField, LM.projectScheduleProject.getMapping(projectKey),
                LM.baseLM.object(LM.projectSchedule).getMapping(projectScheduleProjectKey)));

        projectActionProjectKey = new ImportKey(LM.projectAction, LM.projectActionToSID.getMapping(projectActionProjectField));
        properties.add(new ImportProperty(projectActionProjectField, LM.projectActionProject.getMapping(projectKey),
                LM.baseLM.object(LM.projectAction).getMapping(projectActionProjectKey)));

        claimerKey = new ImportKey(LM.claimer, contactLM.contactEmail.getMapping(emailClaimerField));
        properties.add(new ImportProperty(emailClaimerField, contactLM.emailContact.getMapping(claimerKey)));

        propertiesFullClaimer = new ArrayList<ImportProperty<?>>();
        propertiesFullClaimer.add(new ImportProperty(phoneClaimerField, LM.phoneClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(addressClaimerField, LM.addressClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(siteClaimerField, LM.siteClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(emailFirmClaimerField, LM.emailFirmClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(OGRNClaimerField, LM.OGRNClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(INNClaimerField, LM.INNClaimer.getMapping(claimerKey)));

        propertiesFullClaimer.add(new ImportProperty(fileStatementClaimerField, LM.statementClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(fileConstituentClaimerField, LM.constituentClaimer.getMapping(claimerKey)));
        propertiesFullClaimer.add(new ImportProperty(fileExtractClaimerField, LM.extractClaimer.getMapping(claimerKey)));

        propertyDate = new ImportProperty(dateProjectField, LM.dateJoinProject.getMapping(projectKey));
        propertyStatusDate = new ImportProperty(dateStatusProjectField, LM.statusDateProject.getMapping(projectKey));

        propertiesNative.add(new ImportProperty(nameNativeProjectField, LM.nameNativeJoinProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeManagerProjectField, LM.nameNativeManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeGenitiveManagerProjectField, LM.nameNativeGenitiveManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeDativusManagerProjectField, LM.nameNativeDativusManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeAblateManagerProjectField, LM.nameNativeAblateManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeProblemProjectField, LM.nativeProblemProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeInnovativeProjectField, LM.nativeInnovativeProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeCommentMissionProjectField, LM.nativeCommentMissionProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeResumeProjectField, LM.nativeResumeProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeContactProjectField, LM.nameNativeContactProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeMarketTrendsProjectField, LM.nativeMarketTrendsProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeRelevanceProjectField, LM.nativeRelevanceProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeBasicTechnologyProjectField, LM.nativeBasicTechnologyProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeCaseStudiesProjectField, LM.nativeCaseStudiesProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeCharacteristicsAnaloguesProjectField, LM.nativeCharacteristicsAnaloguesProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeCompaniesAnaloguesProjectField, LM.nativeCompaniesAnaloguesProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeMarketIntroductionProjectField, LM.nativeMarketIntroductionProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeHistoryProjectField, LM.nativeHistoryProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeDynamicsProjectField, LM.nativeDynamicsProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeGrantsProjectField, LM.nativeGrantsProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeLaboratoryProjectField, LM.nativeLaboratoryProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeInvestmentProjectField, LM.nativeInvestmentProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeResultsProjectField, LM.nativeResultsProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeGeneralizedPlanProjectField, LM.nativeGeneralizedPlanProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(linksMarketIntroductionProjectField, LM.linksMarketIntroductionProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(fileRoadMapProjectField, LM.fileNativeRoadMapProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(fileNativeTechnicalDescriptionProjectField, LM.fileNativeTechnicalDescriptionProject.getMapping(projectKey)));

        properties.add(new ImportProperty(emailClaimerField, LM.claimerProject.getMapping(projectKey),
                LM.baseLM.object(LM.claimer).getMapping(claimerKey)));

        propertiesFullClaimerNative = new ArrayList<ImportProperty<?>>();
        propertiesFullClaimerNative.add(new ImportProperty(nameNativeClaimerField, LM.nameNativeJoinClaimer.getMapping(claimerKey)));
        propertiesFullClaimerNative.add(new ImportProperty(firmNameNativeClaimerField, LM.firmNameNativeClaimer.getMapping(claimerKey)));

        propertiesForeign.add(new ImportProperty(nameForeignProjectField, LM.nameForeignJoinProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(nameForeignManagerProjectField, LM.nameForeignManagerProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignProblemProjectField, LM.foreignProblemProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignInnovativeProjectField, LM.foreignInnovativeProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignCommentMissionProjectField, LM.foreignCommentMissionProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignResumeProjectField, LM.foreignResumeProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(nameForeignContactProjectField, LM.nameForeignContactProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignMarketTrendsProjectField, LM.foreignMarketTrendsProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignRelevanceProjectField, LM.foreignRelevanceProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignBasicTechnologyProjectField, LM.foreignBasicTechnologyProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignCaseStudiesProjectField, LM.foreignCaseStudiesProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignCharacteristicsAnaloguesProjectField, LM.foreignCharacteristicsAnaloguesProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignCompaniesAnaloguesProjectField, LM.foreignCompaniesAnaloguesProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignMarketIntroductionProjectField, LM.foreignMarketIntroductionProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignHistoryProjectField, LM.foreignHistoryProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignDynamicsProjectField, LM.foreignDynamicsProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignGrantsProjectField, LM.foreignGrantsProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignLaboratoryProjectField, LM.foreignLaboratoryProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignInvestmentProjectField, LM.foreignInvestmentProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignResultsProjectField, LM.foreignResultsProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignGeneralizedPlanProjectField, LM.foreignGeneralizedPlanProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(linksForeignMarketIntroductionProjectField, LM.linksForeignMarketIntroductionProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(fileForeignRoadMapProjectField, LM.fileForeignRoadMapProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(fileForeignTechnicalDescriptionProjectField, LM.fileForeignTechnicalDescriptionProject.getMapping(projectKey)));

        propertiesFullClaimerForeign = new ArrayList<ImportProperty<?>>();
        propertiesFullClaimerForeign.add(new ImportProperty(nameForeignClaimerField, LM.nameForeignJoinClaimer.getMapping(claimerKey)));
        propertiesFullClaimerForeign.add(new ImportProperty(firmNameForeignClaimerField, LM.firmNameForeignClaimer.getMapping(claimerKey)));

        patentKey = new ImportKey(LM.patent, LM.nativeNumberSIDToPatent.getMapping(nativeNumberPatentField, projectIdField));
        propertiesPatent.add(new ImportProperty(nativeNumberPatentField, LM.nativeNumberPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(linksPatentField, LM.linksPatent.getMapping(patentKey)));
        propertiesPatent.add(new ImportProperty(projectIdField, LM.projectPatent.getMapping(patentKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));
        propertiesPatentNative.add(new ImportProperty(nativeTypePatentField, LM.nativeTypePatent.getMapping(patentKey)));
        propertiesPatentForeign.add(new ImportProperty(foreignTypePatentField, LM.foreignTypePatent.getMapping(patentKey)));

        clusterKey = new ImportKey(LM.cluster, LM.nameNativeToCluster.getMapping(nameNativeClusterField));
        propertiesCluster.add(new ImportProperty(inProjectClusterField, LM.inProjectCluster.getMapping(projectKey, clusterKey)));
        propertiesCluster.add(new ImportProperty(inClaimerProjectClusterField, LM.inClaimerProjectCluster.getMapping(projectKey, clusterKey)));
        propertiesCluster.add(new ImportProperty(nameNativeClusterField, LM.nameNativeCluster.getMapping(clusterKey)));

        propertiesClusterNative.add(new ImportProperty(nativeSubstantiationProjectClusterField, LM.nativeSubstantiationProjectCluster.getMapping(projectKey, clusterKey)));
        propertiesClusterForeign.add(new ImportProperty(foreignSubstantiationProjectClusterField, LM.foreignSubstantiationProjectCluster.getMapping(projectKey, clusterKey)));

        propertyOtherCluster = new ImportProperty(isOtherClusterProjectField, LM.isOtherClusterProject.getMapping(projectKey));
        propertyOtherClusterNative = new ImportProperty(nativeSubstantiationOtherClusterProjectField, LM.nativeSubstantiationOtherClusterProject.getMapping(projectKey));
        propertyOtherClusterForeign = new ImportProperty(foreignSubstantiationOtherClusterProjectField, LM.foreignSubstantiationOtherClusterProject.getMapping(projectKey));

        researchKey = new ImportKey(LM.research, (fillNative ? LM.nativeCommentSIDToResearch : LM.foreignCommentSIDToResearch).getMapping(fillNative ? nativeCommentResearchField : foreignCommentResearchField, projectIdField));
        propertiesResearch.add(new ImportProperty(dataResearchField, LM.dataResearch.getMapping(researchKey)));
        propertiesResearch.add(new ImportProperty(projectIdField, LM.projectResearch.getMapping(researchKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));
        propertiesResearchNative.add(new ImportProperty(nativeCommentResearchField, LM.nativeCommentResearch.getMapping(researchKey)));
        propertiesResearchForeign.add(new ImportProperty(foreignCommentResearchField, LM.foreignCommentResearch.getMapping(researchKey)));

        publicationsKey = new ImportKey(LM.publications, (fillNative ? LM.nativeSIDToPublications : LM.foreignSIDToPublications).getMapping(fillNative ? nativePublicationsField : foreignPublicationsField, projectIdField));
        propertiesPublications.add(new ImportProperty(datePublicationsField, LM.datePublications.getMapping(publicationsKey)));
        propertiesPublications.add(new ImportProperty(nativeLinksPublicationsField, LM.nativeLinksPublications.getMapping(publicationsKey)));
        propertiesPublications.add(new ImportProperty(projectIdField, LM.projectPublications.getMapping(publicationsKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));
        propertiesPublicationsNative.add(new ImportProperty(nativePublicationsField, LM.nativePublications.getMapping(publicationsKey)));
        propertiesPublicationsNative.add(new ImportProperty(nativeAuthorPublicationsField, LM.nativeAuthorPublications.getMapping(publicationsKey)));
        propertiesPublicationsNative.add(new ImportProperty(nativeEditionPublicationsField, LM.nativeEditionPublications.getMapping(publicationsKey)));
        propertiesPublicationsForeign.add(new ImportProperty(foreignPublicationsField, LM.foreignPublications.getMapping(publicationsKey)));
        propertiesPublicationsForeign.add(new ImportProperty(foreignAuthorPublicationsField, LM.foreignAuthorPublications.getMapping(publicationsKey)));
        propertiesPublicationsForeign.add(new ImportProperty(foreignEditionPublicationsField, LM.foreignEditionPublications.getMapping(publicationsKey)));


        commercializationKey = new ImportKey(LM.commercialization, (fillNative ? LM.nativeProjectSIDToCommercialization : LM.foreignProjectSIDToCommercialization).getMapping(fillNative ? nativeProjectCommercializationField : foreignProjectCommercializationField, projectIdField));
        propertiesCommercialization.add(new ImportProperty(projectIdField, LM.projectCommercialization.getMapping(commercializationKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));
        propertiesCommercializationNative.add(new ImportProperty(nativeProjectCommercializationField, LM.nativeProjectCommercialization.getMapping(commercializationKey)));
        propertiesCommercializationNative.add(new ImportProperty(nativeCommentProjectCommercializationField, LM.nativeCommentProjectCommercialization.getMapping(commercializationKey)));
        propertiesCommercializationForeign.add(new ImportProperty(foreignProjectCommercializationField, LM.foreignProjectCommercialization.getMapping(commercializationKey)));
        propertiesCommercializationForeign.add(new ImportProperty(foreignCommentProjectCommercializationField, LM.foreignCommentProjectCommercialization.getMapping(commercializationKey)));

        analoguesKey = new ImportKey(LM.analogues, (fillNative ? LM.nativeProjectSIDToAnalogues : LM.foreignProjectSIDToAnalogues).getMapping(fillNative ? nativeProjectAnaloguesField : foreignProjectAnaloguesField, projectIdField));
        propertiesAnalogues.add(new ImportProperty(projectIdField, LM.projectAnalogues.getMapping(analoguesKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));
        propertiesAnaloguesNative.add(new ImportProperty(nativeProjectAnaloguesField, LM.nativeProjectAnalogues.getMapping(analoguesKey)));
        propertiesAnaloguesNative.add(new ImportProperty(nativeDescriptionProjectAnaloguesField, LM.nativeDescriptionProjectAnalogues.getMapping(analoguesKey)));
        propertiesAnaloguesNative.add(new ImportProperty(nativeCharacteristicsProjectAnaloguesField, LM.nativeCharacteristicsProjectAnalogues.getMapping(analoguesKey)));
        propertiesAnaloguesForeign.add(new ImportProperty(foreignProjectAnaloguesField, LM.foreignProjectAnalogues.getMapping(analoguesKey)));
        propertiesAnaloguesForeign.add(new ImportProperty(foreignDescriptionProjectAnaloguesField, LM.foreignDescriptionProjectAnalogues.getMapping(analoguesKey)));
        propertiesAnaloguesForeign.add(new ImportProperty(foreignCharacteristicsProjectAnaloguesField, LM.foreignCharacteristicsProjectAnalogues.getMapping(analoguesKey)));


        specialistKey = new ImportKey(LM.specialist, (fillNative ? LM.nameNativeSIDToSpecialist : LM.nameForeignSIDToSpecialist).getMapping(fillNative ? nameNativeSpecialistField : nameForeignSpecialistField, projectIdField));
        propertiesSpecialist.add(new ImportProperty(projectIdField, LM.projectSpecialist.getMapping(specialistKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));
        propertiesSpecialist.add(new ImportProperty(filePassportSpecialistField, LM.filePassportSpecialist.getMapping(specialistKey)));
        propertiesSpecialist.add(new ImportProperty(fileStatementSpecialistField, LM.fileStatementSpecialist.getMapping(specialistKey)));
        propertiesSpecialistNative.add(new ImportProperty(nameNativeSpecialistField, LM.nameNativeSpecialist.getMapping(specialistKey)));
        propertiesSpecialistNative.add(new ImportProperty(nativePostSpecialistField, LM.nativePostSpecialist.getMapping(specialistKey)));
        propertiesSpecialistNative.add(new ImportProperty(nativeFunctionSpecialistField, LM.nativeFunctionSpecialist.getMapping(specialistKey)));
        propertiesSpecialistNative.add(new ImportProperty(nativeScopeSpecialistField, LM.nativeScopeSpecialist.getMapping(specialistKey)));
        propertiesSpecialistNative.add(new ImportProperty(nativeExperienceSpecialistField, LM.nativeExperienceSpecialist.getMapping(specialistKey)));
        propertiesSpecialistNative.add(new ImportProperty(nativeTitleSpecialistField, LM.nativeTitleSpecialist.getMapping(specialistKey)));
        propertiesSpecialistNative.add(new ImportProperty(nativeWorkSpecialistField, LM.nativeWorkSpecialist.getMapping(specialistKey)));
        propertiesSpecialistNative.add(new ImportProperty(nativePublicationsSpecialistField, LM.nativePublicationsSpecialist.getMapping(specialistKey)));
        propertiesSpecialistNative.add(new ImportProperty(nativeCitationSpecialistField, LM.nativeCitationSpecialist.getMapping(specialistKey)));
        propertiesSpecialistNative.add(new ImportProperty(nativeIntellectualPropertySpecialistField, LM.nativeIntellectualPropertySpecialist.getMapping(specialistKey)));
        propertiesSpecialistForeign.add(new ImportProperty(nameForeignSpecialistField, LM.nameForeignSpecialist.getMapping(specialistKey)));
        propertiesSpecialistForeign.add(new ImportProperty(foreignPostSpecialistField, LM.foreignPostSpecialist.getMapping(specialistKey)));
        propertiesSpecialistForeign.add(new ImportProperty(foreignFunctionSpecialistField, LM.foreignFunctionSpecialist.getMapping(specialistKey)));
        propertiesSpecialistForeign.add(new ImportProperty(foreignScopeSpecialistField, LM.foreignScopeSpecialist.getMapping(specialistKey)));
        propertiesSpecialistForeign.add(new ImportProperty(foreignExperienceSpecialistField, LM.foreignExperienceSpecialist.getMapping(specialistKey)));
        propertiesSpecialistForeign.add(new ImportProperty(foreignTitleSpecialistField, LM.foreignTitleSpecialist.getMapping(specialistKey)));
        propertiesSpecialistForeign.add(new ImportProperty(foreignWorkSpecialistField, LM.foreignWorkSpecialist.getMapping(specialistKey)));
        propertiesSpecialistForeign.add(new ImportProperty(foreignPublicationsSpecialistField, LM.foreignPublicationsSpecialist.getMapping(specialistKey)));
        propertiesSpecialistForeign.add(new ImportProperty(foreignCitationSpecialistField, LM.foreignCitationSpecialist.getMapping(specialistKey)));
        propertiesSpecialistForeign.add(new ImportProperty(foreignIntellectualPropertySpecialistField, LM.foreignIntellectualPropertySpecialist.getMapping(specialistKey)));

        mileStoneYearKey = new ImportKey(LM.mileStoneYear, LM.nativeSIDToMileStoneYear.getMapping(nativeMileStoneYearField, projectIdField));
        propertiesMileStone.add(new ImportProperty(projectIdField, LM.projectMileStoneYear.getMapping(mileStoneYearKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));

        mileStoneKey = new ImportKey(LM.mileStone, LM.nativeSIDToMileStone.getMapping(nativeMileStoneField, projectIdField));
        propertiesMileStone.add(new ImportProperty(projectIdField, LM.projectMileStone.getMapping(mileStoneKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));
        propertiesMileStone.add(new ImportProperty(nativeMileStoneYearField, LM.nativeMileStoneYear.getMapping(mileStoneYearKey)));
        propertiesMileStone.add(new ImportProperty(nativeMileStoneField, LM.nativeMileStone.getMapping(mileStoneKey)));
        propertiesMileStone.add(new ImportProperty(orderNumberMileStoneField, LM.orderNumberMileStone.getMapping(mileStoneKey)));
        propertiesMileStoneNative.add(new ImportProperty(nativeResearchDescriptionTypeMileStoneMileStoneField, LM.nativeResearchDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneNative.add(new ImportProperty(nativeProductCreationDescriptionTypeMileStoneMileStoneField, LM.nativeProductCreationDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneNative.add(new ImportProperty(nativePlanOnHiringDescriptionTypeMileStoneMileStoneField, LM.nativePlanOnHiringDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneNative.add(new ImportProperty(nativeLicensingDescriptionTypeMileStoneMileStoneField, LM.nativeLicensingDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneNative.add(new ImportProperty(nativePromotionDescriptionTypeMileStoneMileStoneField, LM.nativePromotionDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneNative.add(new ImportProperty(nativeSellingDescriptionTypeMileStoneMileStoneField, LM.nativeSellingDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneForeign.add(new ImportProperty(foreignResearchDescriptionTypeMileStoneMileStoneField, LM.foreignResearchDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneForeign.add(new ImportProperty(foreignProductCreationDescriptionTypeMileStoneMileStoneField, LM.foreignProductCreationDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneForeign.add(new ImportProperty(foreignPlanOnHiringDescriptionTypeMileStoneMileStoneField, LM.foreignPlanOnHiringDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneForeign.add(new ImportProperty(foreignLicensingDescriptionTypeMileStoneMileStoneField, LM.foreignLicensingDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneForeign.add(new ImportProperty(foreignPromotionDescriptionTypeMileStoneMileStoneField, LM.foreignPromotionDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));
        propertiesMileStoneForeign.add(new ImportProperty(foreignSellingDescriptionTypeMileStoneMileStoneField, LM.foreignSellingDescriptionTypeMileStoneMileStoneYear.getMapping(mileStoneKey, mileStoneYearKey)));

        objectivesKey = new ImportKey(LM.objectives, (fillNative ? LM.nativeProjectSIDToObjectives : LM.foreignProjectSIDToObjectives).getMapping(fillNative ? nativeProjectObjectivesField : foreignProjectObjectivesField, projectIdField));
        propertiesObjectives.add(new ImportProperty(projectIdField, LM.projectObjectives.getMapping(objectivesKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));
        propertiesObjectivesNative.add(new ImportProperty(nativeProjectObjectivesField, LM.nativeProjectObjectives.getMapping(objectivesKey)));
        propertiesObjectivesForeign.add(new ImportProperty(foreignProjectObjectivesField, LM.foreignProjectObjectives.getMapping(objectivesKey)));
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        PrivateInfo pInfo = new PrivateInfo();

        pInfo.toLog = "";

        pInfo.projectsImportLimit = (Integer) LM.projectsImportLimit.read(context);
        if (pInfo.projectsImportLimit == null) {
            pInfo.projectsImportLimit = 100;
        }

        try {
            pInfo.session = context.getSession();

            String host = getHost(pInfo);
            if (host == null) {
                context.delayUserInterfaction(new MessageClientAction("Не задан web хост", "Импорт", true));
                return;
            }
            Map<String, Timestamp> projects = importProjectsFromXML(pInfo, host);

            if (!onlyMessage && !fillSids) {
                for (String projectId : projects.keySet()) {
                    URL url = new URL(host + "&show=all&projectId=" + projectId);
                    URLConnection connection = url.openConnection();
                    connection.setDoOutput(false);
                    connection.setDoInput(true);
                    ThreadLocalContext.pushActionMessage("ИД проекта - " + projectId);
                    importProject(pInfo, connection.getInputStream(), projectId, projects.get(projectId), context);
                    ThreadLocalContext.popActionMessage();
                    System.gc();
                }
            }
            String message = "";
            if (projects == null || !projects.isEmpty()) {
                if (!onlyMessage) {
                    message = "Данные были успешно приняты\n";
                }
                message += pInfo.toLog;
            } else {
                if (pInfo.responseContents != null) {
                    message = new String(pInfo.responseContents);
                } else {
                    message = "Вся информация актуальна";
                }
            }
            context.delayUserInterfaction(new MessageClientAction(message, "Импорт", true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Timestamp> importProjectsFromXML(PrivateInfo pInfo, String host) throws IOException, SQLException {
        ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = null;
        List<Element> elementList = new ArrayList<Element>();
        try {
            URL url = new URL(host + "&show=projects&limit=2000");
            URLConnection connection = url.openConnection();

            connection.setDoOutput(false);
            connection.setDoInput(true);

            pInfo.responseContents = IOUtils.readBytesFromStream(connection.getInputStream());
            /*
            File file = new File("C://testNew.xml");
            InputStream is = new FileInputStream(file);
            long length = file.length();
            byte[] bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            is.close();
            pInfo.responseContents = bytes;
            */
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(new ByteArrayInputStream(pInfo.responseContents));
            pInfo.responseContents = null;

            LCP<PropertyInterface> isProject = (LCP<PropertyInterface>) LM.is(LM.project);
            ImRevMap<PropertyInterface, KeyExpr> keys = isProject.getMapKeys();
            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
            query.addProperty("id", LM.sidProject.getExpr(keys.singleValue()));
            query.addProperty("emailContact", LM.emailClaimerProject.getExpr(keys.singleValue()));
            query.addProperty("name", LM.nameNative.getExpr(keys.singleValue()));
            query.addProperty("date", LM.updateDateProject.getExpr(keys.singleValue()));
            if (fillSids) {
                query.and(isProject.property.getExpr(keys).getWhere());
            } else {
                query.and(LM.sidProject.getExpr(keys.singleValue()).getWhere());
            }
            result = query.execute(pInfo.session.sql);

            Element rootNode = document.getRootElement();
            elementList = new ArrayList<Element>(rootNode.getChildren("project"));
        } catch (JDOMParseException e) {
            logger.error(e.getCause() + " : " + new String(pInfo.responseContents), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Collections.sort(elementList, new Comparator<Element>() {
            @Override
            public int compare(Element o1, Element o2) {
                return o1.getChildText("emailProject").compareTo(o2.getChildText("emailProject"));
            }
        });

        if (fillSids) {
            fillSids(pInfo, result, elementList);
            logger.info("Imported sids:" + pInfo.toLog);
            return null;
        }

        return makeImportList(pInfo, result, elementList);
    }

    public String getHost(PrivateInfo pInfo) throws NoSuchAlgorithmException, SQLException {
        Calendar calTZ = new GregorianCalendar(TimeZone.getTimeZone("Europe/Moscow"));
        calTZ.setTimeInMillis(new java.util.Date().getTime());
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.DAY_OF_MONTH, calTZ.get(Calendar.DAY_OF_MONTH) - 2);
        cal.set(Calendar.MONTH, calTZ.get(Calendar.MONTH));
        cal.set(Calendar.YEAR, calTZ.get(Calendar.YEAR));
        String string = cal.getTimeInMillis() / 1000 + "_1q2w3e";

        MessageDigest m = MessageDigest.getInstance("MD5");
        String webHost = (String) LM.webHost.read(pInfo.session);
        webHost = webHost != null ? webHost.substring(0, webHost.indexOf("/")) : null;
        return webHost == null ? null : "http://" + webHost + "/xml.php?hash=" + Hex.encodeHexString(m.digest(string.getBytes()));
    }

    public Date getUpdateProjectDate(String year, String month, String day, String hour, String minute, String second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, Integer.valueOf(year));
        calendar.set(Calendar.MONTH, Integer.valueOf(month) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(day));
        if (hour != null)
            calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(hour));
        else
            calendar.set(Calendar.HOUR_OF_DAY, 0);
        if (minute != null)
            calendar.set(Calendar.MINUTE, Integer.valueOf(minute));
        else
            calendar.set(Calendar.MINUTE, 0);
        if (second != null)
            calendar.set(Calendar.SECOND, Integer.valueOf(second));
        else
            calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public Map<String, Timestamp> makeImportList(PrivateInfo pInfo, ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> data, List<Element> elementList) {
        Map<String, Timestamp> projectList = new LinkedHashMap<String, Timestamp>();
        if (elementList != null) {
            Timestamp currentProjectDate;
            int counter = 0;
            for (Element element : elementList) {
                String projectId = element.getChildText("projectId");
                currentProjectDate = new Timestamp(getUpdateProjectDate(element.getChildText("yearProject"), element.getChildText("monthProject"),
                        element.getChildText("dayProject"), element.getChildText("hourProject"),
                        element.getChildText("minuteProject"), element.getChildText("secondProject")).getTime());
                String name = element.getChildText("nameNativeProject");
                boolean ignore = false;
                Object regulation = element.getChildText("regulationProject");
                try {
                    if (LM.importOnlyR2Projects.read(pInfo.session) != null)
                        if (regulation != null) {
                            if ("b".equals(regulation)) {
                                ignore = false;
                            } else {
                                ignore = true;
                                break;
                            }
                        } else {
                            ignore = true;
                            break;
                        }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                for (ImMap<Object, Object> project : data.valueIt()) {
                    Timestamp date = (Timestamp) project.get("date");
                    if (project.get("id").toString().trim().equals(projectId)) {
                        if (date == null || currentProjectDate.after(date)) {
                            Object email = project.get("emailContact");
                            toLogString(pInfo, projectId, email == null ? null : email.toString().trim(), element.getChildText("emailProject"), name.toString().trim(), name, date, currentProjectDate);
                            projectList.put(projectId, currentProjectDate);
                            counter++;
                            ignore = true;
                            break;
                        } else {
                            ignore = true;
                            break;
                        }
                    }
                }
                if (!ignore && !onlyReplace) {
                    toLogString(pInfo, projectId, null, element.getChildText("emailProject"), null, name, null, currentProjectDate);
                    projectList.put(projectId, currentProjectDate);
                    counter++;
                }
                if (counter == pInfo.projectsImportLimit) {
                    break;
                }
            }
        }

        if (!onlyMessage) {
            logger.info("Projects to import (" + projectList.size() + "):" + pInfo.toLog);
        }
        return projectList;
    }

    public void toLogString(PrivateInfo pInfo, Object projectId, Object oldEmail, Object newEmail, Object oldName, Object newName, Object oldDate, Object newDate) {
        pInfo.toLog += "\nprojectId: " + projectId + "\n\temailContact: " + oldEmail + " <- " + newEmail + "\n\tname: " + oldName +
                " <- " + newName + "\n\tdate: " + oldDate + " <- " + newDate;
    }

    public void fillSids(PrivateInfo pInfo, ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> data, List<Element> elements) throws SQLException {
        for (ImMap<PropertyInterface, Object> key : data.keyIt()) {
            ImMap<Object, Object> project = data.get(key);
            if (project.get("id") == null) {
                Object email = project.get("emailContact");
                if (email != null && getEmailRepeatings(data, elements, email.toString().trim()) == 1) {
                    String projectId = getProjectId(elements, email.toString().trim());
                    if (projectId != null) {
                        LM.sidProject.change(projectId, pInfo.session, pInfo.session.getDataObject(LM.project, key.singleValue()));
                        pInfo.toLog += "\nprojectId: " + projectId + "\n\temail: " + email.toString().trim() + "\n\t" +
                                project.get("name").toString().trim() + " =? " + getProjectName(elements, email.toString().trim().toLowerCase());
                    }
                }
            }
        }
    }

    public String getProjectId(List<Element> elements, String email) {
        for (Element element : elements) {
            String formEmail = element.getChildText("emailProject");
            if (formEmail.trim().toLowerCase().equals(email.toLowerCase())) {
                if (!formEmail.trim().equals(email)) {
                    logger.info("case-differing emails: " + email + " : " + formEmail);
                }
                return element.getChildText("projectId");
            }
        }
        return null;
    }

    public String getProjectName(List<Element> elements, String email) {
        for (Element element : elements) {
            if (element.getChildText("emailProject").trim().toLowerCase().equals(email)) {
                return element.getChildText("nameNativeProject");
            }
        }
        return null;
    }

    public int getEmailRepeatings(ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> data, List<Element> elements, String email) {
        int repeatings = 0;
        for (ImMap<Object, Object> project : data.values()) {
            if (project.get("emailContact") != null && project.get("emailContact").toString().trim().toLowerCase().equals(email.toLowerCase())) {
                repeatings++;
            }
        }
        if (repeatings == 1) {
            repeatings = 0;
            for (Element element : elements) {
                if (element.getChildText("emailProject").trim().toLowerCase().equals(email.toLowerCase())) {
                    repeatings++;
                }
            }
            return repeatings;
        }
        return 0;
    }

    public byte[] buildFileByteArray(Element element) {
        if (element != null) {
            byte[] file = Base64.decodeBase64(BaseUtils.nevl(element.getText(), null));
            Attribute extAttr = element.getAttribute("ext");
            String extension = extAttr != null ? extAttr.getValue() : null;
            if (extension == null) {
                extension = "pdf";
            }
            return file == null ? null : BaseUtils.mergeFileAndExtension(file, extension.getBytes());
        }
        return null;
    }

    public void importProject(PrivateInfo pInfo, InputStream inputStream, String projectId, Timestamp currentProjectDate, ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<List<Object>> dataCluster = new ArrayList<List<Object>>();
        List<List<Object>> dataPatent = new ArrayList<List<Object>>();
        List<List<Object>> dataAcademic = new ArrayList<List<Object>>();
        List<List<Object>> dataNonRussianSpecialist = new ArrayList<List<Object>>();
        List<List<Object>> dataResearch = new ArrayList<List<Object>>();
        List<List<Object>> dataPublications = new ArrayList<List<Object>>();
        List<List<Object>> dataCommercialization = new ArrayList<List<Object>>();
        List<List<Object>> dataAnalogues = new ArrayList<List<Object>>();
        List<List<Object>> dataSpecialist = new ArrayList<List<Object>>();
        List<List<Object>> dataMileStone = new ArrayList<List<Object>>();
        List<List<Object>> dataObjectives = new ArrayList<List<Object>>();
        List<Object> row;

        SAXBuilder builder = new SAXBuilder();

        try {
            pInfo.responseContents = IOUtils.readBytesFromStream(inputStream);
            /*
            File file = new File("C://testNew.xml");
            InputStream is = new FileInputStream(file);
            long length = file.length();
            byte[] bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            is.close();
            pInfo.responseContents = bytes;
            */
            Document document = builder.build(new ByteArrayInputStream(pInfo.responseContents));
            pInfo.responseContents = null;
            Element rootNode = document.getRootElement();
            List list = rootNode.getChildren("project");

            for (int i = 0; i < list.size(); i++) {
                Element node = (Element) list.get(i);
                row = new ArrayList<Object>();
                String lng = BaseUtils.nevl(node.getChildText("languageProject"), "rus").toString();
                boolean fillNative = ("rus".equals(lng)) || "both".equals(lng);
                boolean fillForeign = ("eng".equals(lng)) || "both".equals(lng);
                boolean fillDate = (!"1970".equals(node.getChildText("yearProject")));
                boolean fillClaimer = ("status".equals(node.getChildText("actionProject")));

                boolean fillTwoDates = false;
                if (fillClaimer) {
                    ObjectValue projectObject = LM.sidToProject.readClasses(pInfo.session, new DataObject(projectId));
                    fillTwoDates = !(projectObject instanceof DataObject) || LM.quantityPreliminaryVoteProject.read(context, (DataObject) projectObject) == null;
                }

                if ("unknown".equals(lng)) {
                    String nameNativeManagerProject = node.getChildText("nameNativeManagerProject");
                    if (nameNativeManagerProject != null && !nameNativeManagerProject.equals("")) {
                        fillNative = true;
                    }
                    String nameForeignManagerProject = node.getChildText("nameForeignManagerProject");
                    if (nameForeignManagerProject != null && !nameForeignManagerProject.equals("")) {
                        fillForeign = true;
                    }
                }
                String regulationProject = BaseUtils.nevl(node.getChildText("regulationProject"), "a");

                if (!"b".equals(regulationProject)) {

                    initOldFieldsNProperties();

                    if (fillNative) row.add(true);
                    else row.add(null); //fillNativeProject
                    if (fillForeign) row.add(true);
                    else row.add(null); //fillForeignProject

                    String consultingCenterComment = node.getChildText("consultingCenterComment");
                    if (consultingCenterComment == null) {
                        row.add(null);
                        row.add(null);
                        row.add(null);
                    } else if ("".equals(consultingCenterComment)) {
                        row.add(true);
                        row.add(null);
                        row.add(null);
                    } else {
                        row.add(true);
                        row.add(true);
                        row.add(consultingCenterComment);
                    }

                    row.add(projectId);
                    row.add(BaseUtils.nullZero(node.getChildText("isOwnedEquipmentProject")));
                    row.add(BaseUtils.nullZero(node.getChildText("isAvailableEquipmentProject")));
                    row.add(BaseUtils.nullZero(node.getChildText("isTransferEquipmentProject")));
                    row.add(node.getChildText("descriptionTransferEquipmentProject"));
                    row.add(node.getChildText("ownerEquipmentProject"));
                    row.add(BaseUtils.nullZero(node.getChildText("isPlanningEquipmentProject")));
                    row.add(node.getChildText("specificationEquipmentProject"));
                    row.add(BaseUtils.nullZero(node.getChildText("isSeekEquipmentProject")));
                    row.add(node.getChildText("descriptionEquipmentProject"));
                    row.add(BaseUtils.nullZero(node.getChildText("isOtherEquipmentProject")));
                    row.add(node.getChildText("commentEquipmentProject"));
                    row.add(BaseUtils.nullZero(node.getChildText("isReturnInvestmentsProject")));
                    row.add(node.getChildText("nameReturnInvestorProject"));
                    row.add(node.getChildText("amountReturnFundsProject"));
                    row.add(BaseUtils.nullZero(node.getChildText("isNonReturnInvestmentsProject")));
                    row.add(node.getChildText("nameNonReturnInvestorProject"));
                    row.add(node.getChildText("amountNonReturnFundsProject"));
                    row.add(node.getChildText("commentOtherNonReturnInvestmentsProject"));

                    row.add(BaseUtils.nullZero(node.getChildText("isCapitalInvestmentProject")));
                    row.add(BaseUtils.nullZero(node.getChildText("isPropertyInvestmentProject")));
                    row.add(BaseUtils.nullZero(node.getChildText("isGrantsProject")));
                    row.add(BaseUtils.nullZero(node.getChildText("isOtherNonReturnInvestmentsProject")));
                    row.add(BaseUtils.nullZero(node.getChildText("isOwnFundsProject")));
                    row.add(node.getChildText("amountOwnFundsProject"));
                    row.add(BaseUtils.nullZero(node.getChildText("isPlanningSearchSourceProject")));
                    row.add(node.getChildText("amountFundsProject"));
                    row.add(BaseUtils.nullZero(node.getChildText("isOtherSoursesProject")));
                    row.add(node.getChildText("commentOtherSoursesProject"));
                    row.add(currentProjectDate);
                    row.add(buildFileByteArray(node.getChild("fileMinutesOfMeetingExpertCollegiumProject")));

                    row.add(node.getChildText("typeProject"));
                    row.add(node.getChildText("actionProject"));

                    row.add(node.getChildText("emailProject"));


                    LCP<PropertyInterface> isCluster = (LCP<PropertyInterface>) LM.is(LM.cluster);
                    ImRevMap<PropertyInterface, KeyExpr> keys = isCluster.getMapKeys();
                    QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
                    query.addProperty("name", LM.nameNative.getExpr(keys.singleValue()));
                    query.addProperty("nameNativeShort", LM.nameNativeShort.getExpr(keys.singleValue()));
                    query.and(isCluster.property.getExpr(keys).getWhere());
                    ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(pInfo.session.sql);

                    boolean fillOtherCluster = false;
                    Object otherClusterNativeSubstantiation = null, otherClusterForeignSubstantiation = null;

                    List<Element> listCluster = node.getChildren("cluster");
                    Map<String, ClusterInfo> clusterInfoList = new HashMap<String, ClusterInfo>();

                    for (ImMap<Object, Object> values : result.valueIt()) {
                        String nextItem = values.get("name").toString().trim();
                        Object nextNameNativeShort = values.get("nameNativeShort");
                        clusterInfoList.put(nextItem, new ClusterInfo(projectId, nextItem, nextNameNativeShort));
                    }

                    for (Element nodeCluster : listCluster) {
                        String nameCluster = nodeCluster.getChildText("nameNativeCluster");
                        if ("Нажмите здесь, если не относится".equals(nameCluster)) {
                            fillOtherCluster = true;
                            otherClusterNativeSubstantiation = nodeCluster.getChildText("nativeSubstantiationClusterProject");
                            otherClusterForeignSubstantiation = nodeCluster.getChildText("foreignSubstantiationClusterProject");
                        } else {
                            if (clusterInfoList.containsKey(nameCluster)) {
                                clusterInfoList.put(nameCluster, new ClusterInfo(projectId, true, nameCluster, null,
                                        nodeCluster.getChildText("nativeSubstantiationClusterProject"),
                                        nodeCluster.getChildText("foreignSubstantiationClusterProject")));
                            }
                        }
                    }

                    for (ClusterInfo nodeCluster : clusterInfoList.values()) {
                        List<Object> rowCluster = new ArrayList<Object>();
                        rowCluster.add(nodeCluster.projectID);
                        rowCluster.add(nodeCluster.inProjectCluster);
                        rowCluster.add(nodeCluster.nameNativeCluster);
                        if (fillNative)
                            rowCluster.add(nodeCluster.nativeSubstantiationProjectCluster);
                        if (fillForeign)
                            rowCluster.add(nodeCluster.foreignSubstantiationProjectCluster);
                        dataCluster.add(rowCluster);
                    }

                    if (fillClaimer) {
                        row.add(node.getChildText("phoneClaimer"));
                        row.add(node.getChildText("addressClaimer"));
                        row.add(node.getChildText("siteClaimer"));
                        row.add(node.getChildText("emailClaimer"));
                        row.add(node.getChildText("OGRNClaimer"));
                        row.add(node.getChildText("INNClaimer"));
                        row.add(buildFileByteArray(node.getChild("fileStatementClaimer")));
                        row.add(buildFileByteArray(node.getChild("fileConstituentClaimer")));
                        row.add(buildFileByteArray(node.getChild("fileExtractClaimer")));
                    }

                    ObjectValue expertObject = LM.emailToExpert.readClasses(pInfo.session, new DataObject(node.getChildText("emailProject")));
                    if (expertObject instanceof DataObject)
                        LM.baseLM.objectClass.change(LM.claimerExpert.getSingleClass().ID, context, (DataObject) expertObject);

                    Date date = new java.sql.Date(Integer.parseInt(node.getChildText("yearProject")) - 1900, Integer.parseInt(node.getChildText("monthProject")) - 1, Integer.parseInt(node.getChildText("dayProject")));
                    if (fillDate) {
                        row.add(date);
                        if (fillTwoDates) {
                            row.add(date);
                        }
                    }

                    row.add(BaseUtils.nullBoolean(fillOtherCluster));

                    if (fillNative)
                        row.add(otherClusterNativeSubstantiation);
                    if (fillForeign)
                        row.add(otherClusterForeignSubstantiation);


                    if (fillNative) {
                        row.add(node.getChildText("nameNativeProject"));
                        row.add(node.getChildText("nameNativeManagerProject"));
                        row.add(node.getChildText("nameNativeGenitiveManagerProject"));
                        row.add(node.getChildText("nameNativeDativusManagerProject"));
                        row.add(node.getChildText("nameNativeAblateManagerProject"));
                        row.add(node.getChildText("nativeProblemProject"));
                        row.add(node.getChildText("nativeInnovativeProject"));
                        row.add(node.getChildText("nativeSubstantiationProjectType"));
                        row.add(buildFileByteArray(node.getChild("fileNativeSummaryProject")));
                        row.add(buildFileByteArray(node.getChild("fileNativeTechnicalDescriptionProject")));
                        row.add(buildFileByteArray(node.getChild("fileRoadMapProject")));
                        if (fillClaimer) {
                            row.add(node.getChildText("nameNativeClaimer"));
                            row.add(node.getChildText("firmNameNativeClaimer"));
                        }
                    }

                    if (fillForeign) {
                        row.add(node.getChildText("nameForeignProject"));
                        row.add(node.getChildText("nameForeignManagerProject"));
                        row.add(node.getChildText("foreignProblemProject"));
                        row.add(node.getChildText("foreignInnovativeProject"));
                        row.add(node.getChildText("foreignSubstantiationProjectType"));
                        row.add(buildFileByteArray(node.getChild("fileForeignSummaryProject")));
                        row.add(buildFileByteArray(node.getChild("fileForeignTechnicalDescriptionProject")));
                        row.add(buildFileByteArray(node.getChild("fileForeignRoadMapProject")));
                        if (fillClaimer) {
                            row.add(node.getChildText("nameForeignClaimer"));
                            row.add(node.getChildText("firmNameForeignClaimer"));
                        }
                    }
                    data.add(row);

                    List listPatent = node.getChildren("patent");
                    for (int j = 0; j < listPatent.size(); j++) {
                        Element nodePatent = (Element) listPatent.get(j);
                        List<Object> rowPatent = new ArrayList<Object>();

                        rowPatent.add(projectId);
                        rowPatent.add(nodePatent.getChildText("nativeNumberPatent"));
                        try {
                            rowPatent.add(new java.sql.Date(new Date(Integer.parseInt(nodePatent.getChildText("yearPatent")) - 1900, Integer.parseInt(nodePatent.getChildText("monthPatent")), Integer.parseInt(nodePatent.getChildText("dayPatent")) - 1).getTime()));
                        } catch (NumberFormatException e) {
                            rowPatent.add(null);
                        }
                        rowPatent.add(BaseUtils.nullZero(node.getChildText("isOwnedPatent")));
                        rowPatent.add(BaseUtils.nullString(nodePatent.getChildText("ownerPatent")));
                        rowPatent.add(BaseUtils.nullString(nodePatent.getChildText("ownerTypePatent")));
                        rowPatent.add(BaseUtils.nullZero(node.getChildText("isValuatedPatent")));
                        rowPatent.add(BaseUtils.nullString(nodePatent.getChildText("valuatorPatent")));
                        rowPatent.add(buildFileByteArray(node.getChild("fileIntentionOwnerPatent")));
                        rowPatent.add(buildFileByteArray(node.getChild("fileActValuationPatent")));
                        if (fillNative) {
                            rowPatent.add(nodePatent.getChildText("nativeTypePatent"));
                        }
                        if (fillForeign) {
                            rowPatent.add(nodePatent.getChildText("foreignTypePatent"));
                            rowPatent.add(nodePatent.getChildText("foreignNumberPatent"));
                        }
                        dataPatent.add(rowPatent);
                    }

                    List listAcademic = node.getChildren("academic");
                    for (int j = 0; j < listAcademic.size(); j++) {
                        Element nodeAcademic = (Element) listAcademic.get(j);
                        List<Object> rowAcademic = new ArrayList<Object>();
                        rowAcademic.add(projectId);
                        rowAcademic.add(nodeAcademic.getChildText("fullNameAcademic"));
                        rowAcademic.add(nodeAcademic.getChildText("institutionAcademic"));
                        rowAcademic.add(nodeAcademic.getChildText("titleAcademic"));
                        rowAcademic.add(buildFileByteArray(nodeAcademic.getChild("fileDocumentConfirmingAcademic")));
                        rowAcademic.add(buildFileByteArray(nodeAcademic.getChild("fileDocumentEmploymentAcademic")));
                        dataAcademic.add(rowAcademic);
                    }

                    List listNonRussianSpecialist = node.getChildren("nonRussianSpecialist");
                    for (int j = 0; j < listNonRussianSpecialist.size(); j++) {
                        Element nodeNonRussianSpecialist = (Element) listNonRussianSpecialist.get(j);
                        List<Object> rowNonRussianSpecialist = new ArrayList<Object>();
                        rowNonRussianSpecialist.add(projectId);
                        rowNonRussianSpecialist.add(nodeNonRussianSpecialist.getChildText("fullNameNonRussianSpecialist"));
                        rowNonRussianSpecialist.add(nodeNonRussianSpecialist.getChildText("organizationNonRussianSpecialist"));
                        rowNonRussianSpecialist.add(nodeNonRussianSpecialist.getChildText("titleNonRussianSpecialist"));
                        rowNonRussianSpecialist.add(buildFileByteArray(nodeNonRussianSpecialist.getChild("fileNativeResumeNonRussianSpecialist")));
                        rowNonRussianSpecialist.add(buildFileByteArray(nodeNonRussianSpecialist.getChild("fileForeignResumeNonRussianSpecialist")));
                        rowNonRussianSpecialist.add(buildFileByteArray(nodeNonRussianSpecialist.getChild("filePassportNonRussianSpecialist")));
                        rowNonRussianSpecialist.add(buildFileByteArray(nodeNonRussianSpecialist.getChild("fileStatementNonRussianSpecialist")));
                        dataNonRussianSpecialist.add(rowNonRussianSpecialist);
                    }

                    // поскольку нам нужно менять properties
                    List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>(this.properties);
                    List<ImportProperty<?>> propertiesNative = new ArrayList<ImportProperty<?>>(this.propertiesNative);
                    List<ImportProperty<?>> propertiesForeign = new ArrayList<ImportProperty<?>>(this.propertiesForeign);
                    List<ImportProperty<?>> propertiesFullClaimer = new ArrayList<ImportProperty<?>>(this.propertiesFullClaimer);
                    List<ImportProperty<?>> propertiesFullClaimerNative = new ArrayList<ImportProperty<?>>(this.propertiesFullClaimerNative);
                    List<ImportProperty<?>> propertiesFullClaimerForeign = new ArrayList<ImportProperty<?>>(this.propertiesFullClaimerForeign);

                    List<ImportField> fieldsNative = BaseUtils.toList(
                            nameNativeProjectField, nameNativeManagerProjectField, nameNativeGenitiveManagerProjectField,
                            nameNativeDativusManagerProjectField, nameNativeAblateManagerProjectField, nativeProblemProjectField,
                            nativeInnovativeProjectField, nativeSubstantiationProjectTypeField, fileNativeSummaryProjectField, fileNativeTechnicalDescriptionProjectField, fileRoadMapProjectField);

                    List<ImportField> fieldsFullClaimerNative = BaseUtils.toList(nameNativeClaimerField, firmNameNativeClaimerField);

                    List<ImportField> fieldsForeign = BaseUtils.toList(
                            nameForeignProjectField, nameForeignManagerProjectField, foreignProblemProjectField,
                            foreignInnovativeProjectField, foreignSubstantiationProjectTypeField, fileForeignSummaryProjectField, fileForeignTechnicalDescriptionProjectField, fileForeignRoadMapProjectField);

                    List<ImportField> fieldsFullClaimerForeign = BaseUtils.toList(nameForeignClaimerField, firmNameForeignClaimerField);

                    List<ImportField> fieldsBoth = BaseUtils.toList(
                            fillNativeProjectField, fillForeignProjectField,
                            isConsultingCenterQuestionProjectField, isConsultingCenterCommentProjectField, consultingCenterCommentProjectField,
                            projectIdField,
                            isOwnedEquipmentProjectField, isAvailableEquipmentProjectField, isTransferEquipmentProjectField,
                            descriptionTransferEquipmentProjectField, ownerEquipmentProjectField, isPlanningEquipmentProjectField,
                            specificationEquipmentProjectField, isSeekEquipmentProjectField, descriptionEquipmentProjectField,
                            isOtherEquipmentProjectField, commentEquipmentProjectField, isReturnInvestmentsProjectField,
                            nameReturnInvestorProjectField, amountReturnFundsProjectField, isNonReturnInvestmentsProjectField,
                            nameNonReturnInvestorProjectField, amountNonReturnFundsProjectField, commentOtherNonReturnInvestmentsProjectField,
                            isCapitalInvestmentProjectField, isPropertyInvestmentProjectField, isGrantsProjectField,
                            isOtherNonReturnInvestmentsProjectField, isOwnFundsProjectField, amountOwnFundsProjectField,
                            isPlanningSearchSourceProjectField, amountFundsProjectField, isOtherSoursesProjectField,
                            commentOtherSoursesProjectField, updateDateProjectField, fileMinutesOfMeetingExpertCollegiumProjectField,
                            projectTypeProjectField, projectActionProjectField, emailClaimerField
                    );

                    List<ImportField> fieldsFullClaimerBoth = BaseUtils.toList(phoneClaimerField, addressClaimerField,
                            siteClaimerField, emailFirmClaimerField,
                            OGRNClaimerField, INNClaimerField, fileStatementClaimerField,
                            fileConstituentClaimerField, fileExtractClaimerField);

                    if (fillClaimer) {
                        fieldsNative.addAll(fieldsFullClaimerNative);
                        fieldsForeign.addAll(fieldsFullClaimerForeign);
                        fieldsBoth.addAll(fieldsFullClaimerBoth);
                        propertiesNative.addAll(propertiesFullClaimerNative);
                        propertiesForeign.addAll(propertiesFullClaimerForeign);
                        properties.addAll(propertiesFullClaimer);
                    }

                    if (fillDate) {
                        if (fillTwoDates) {
                            properties.add(propertyStatusDate);
                            fieldsBoth.add(dateStatusProjectField);

                            properties.add(propertyDate);
                            fieldsBoth.add(dateProjectField);

                        } else if (fillClaimer) {
                            properties.add(propertyStatusDate);
                            fieldsBoth.add(dateStatusProjectField);
                        } else {
                            properties.add(propertyDate);
                            fieldsBoth.add(dateProjectField);
                        }
                    }

                    properties.add(propertyOtherCluster);
                    fieldsBoth.add(isOtherClusterProjectField);
                    if (fillNative) {
                        properties.add(propertyOtherClusterNative);
                        fieldsBoth.add(nativeSubstantiationOtherClusterProjectField);
                    }
                    if (fillForeign) {
                        properties.add(propertyOtherClusterForeign);
                        fieldsBoth.add(foreignSubstantiationOtherClusterProjectField);
                    }

                    ImportKey<?>[] keysArray = new ImportKey<?>[]{projectKey, projectTypeProjectKey, projectActionProjectKey, claimerKey};
                    importMultilanguageData(pInfo,
                            fieldsBoth, fieldsNative, fieldsForeign,
                            properties, propertiesNative, propertiesForeign,
                            data, keysArray, null, fillNative, fillForeign);

                    List<ImportField> fieldsCurrentClusterBoth = BaseUtils.toList(
                            projectIdField, inProjectClusterField, nameNativeClusterField);
                    List<ImportField> fieldsCurrentClusterNative = BaseUtils.toList(
                            nativeSubstantiationProjectClusterField);
                    List<ImportField> fieldsCurrentClusterForeign = BaseUtils.toList(
                            foreignSubstantiationProjectClusterField);

                    keysArray = new ImportKey<?>[]{clusterKey, projectKey};
                    importMultilanguageData(pInfo,
                            fieldsCurrentClusterBoth, fieldsCurrentClusterNative, fieldsCurrentClusterForeign,
                            propertiesCluster, propertiesClusterNative, propertiesClusterForeign,
                            dataCluster, keysArray, null, fillNative, fillForeign);

                    List<ImportField> fieldsPatentBoth = BaseUtils.toList(
                            projectIdField, nativeNumberPatentField, datePatentField,
                            isOwnedPatentField, ownerPatentField, ownerTypePatentField,
                            isValuatedPatentField, valuatorPatentField, fileIntentionOwnerPatentField,
                            fileActValuationPatentField);
                    List<ImportField> fieldsPatentNative = BaseUtils.toList(nativeTypePatentField);
                    List<ImportField> fieldsPatentForeign = BaseUtils.toList(foreignTypePatentField, foreignNumberPatentField);
                    keysArray = new ImportKey<?>[]{projectKey, patentKey, ownerTypePatentKey};
                    importMultilanguageData(pInfo,
                            fieldsPatentBoth, fieldsPatentNative, fieldsPatentForeign,
                            propertiesPatent, propertiesPatentNative, propertiesPatentForeign,
                            dataPatent, keysArray, null, fillNative, fillForeign);

                    List<ImportField> fieldsAcademic = BaseUtils.toList(
                            projectIdField,
                            fullNameAcademicField, institutionAcademicField, titleAcademicField,
                            fileDocumentConfirmingAcademicField, fileDocumentEmploymentAcademicField
                    );
                    ImportTable table = new ImportTable(fieldsAcademic, dataAcademic);
                    keysArray = new ImportKey<?>[]{projectKey, academicKey};
                    new IntegrationService(pInfo.session, table, Arrays.asList(keysArray), propertiesAcademic).synchronize(true);

                    List<ImportField> fieldsNonRussianSpecialist = BaseUtils.toList(
                            projectIdField,
                            fullNameNonRussianSpecialistField, organizationNonRussianSpecialistField, titleNonRussianSpecialistField
                            , fileNativeResumeNonRussianSpecialistField, fileForeignResumeNonRussianSpecialistField,
                            filePassportNonRussianSpecialistField, fileStatementNonRussianSpecialistField
                    );
                    table = new ImportTable(fieldsNonRussianSpecialist, dataNonRussianSpecialist);
                    keysArray = new ImportKey<?>[]{projectKey, nonRussianSpecialistKey};
                    new IntegrationService(pInfo.session, table, Arrays.asList(keysArray), propertiesNonRussianSpecialist).synchronize(true);

                } else {

                    initNewFieldsNProperties(fillNative);

                    if (fillNative) row.add(true);
                    else row.add(null); //fillNativeProject
                    if (fillForeign) row.add(true);
                    else row.add(null); //fillForeignProject

                    String consultingCenterComment = node.getChildText("consultingCenterComment");
                    if (consultingCenterComment == null) {
                        row.add(null);
                        row.add(null);
                        row.add(null);
                    } else if ("".equals(consultingCenterComment)) {
                        row.add(true);
                        row.add(null);
                        row.add(null);
                    } else {
                        row.add(true);
                        row.add(true);
                        row.add(consultingCenterComment);
                    }

                    row.add(node.getChildText("phoneContactProject"));
                    row.add(node.getChildText("emailContactProject"));
                    row.add(node.getChildText("linksMarketTrendsProject"));
                    row.add(node.getChildText("linksAnalogProject"));

                    row.add(projectId);

                    row.add(currentProjectDate);
                    row.add(buildFileByteArray(node.getChild("fileMinutesOfMeetingExpertCollegiumProject")));
                    row.add(buildFileByteArray(node.getChild("fileWrittenConsentClaimerProject")));
                    row.add(node.getChildText("missionProject"));
                    row.add("R2"); //scheduleProject
                    row.add(node.getChildText("actionProject"));

                    row.add(node.getChildText("emailProject"));


                    LCP<PropertyInterface> isCluster = (LCP<PropertyInterface>) LM.is(LM.cluster);
                    ImRevMap<PropertyInterface, KeyExpr> keys = isCluster.getMapKeys();
                    QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
                    query.addProperty("name", LM.nameNative.getExpr(keys.singleValue()));
                    query.addProperty("nameNativeShort", LM.nameNativeShort.getExpr(keys.singleValue()));
                    query.and(isCluster.property.getExpr(keys).getWhere());
                    ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(pInfo.session.sql);

                    boolean fillOtherCluster = false;
                    Object otherClusterNativeSubstantiation = null, otherClusterForeignSubstantiation = null;

                    List<Element> listCluster = node.getChildren("cluster");
                    Map<String, ClusterInfo> clusterInfoList = new HashMap<String, ClusterInfo>();

                    for (ImMap<Object, Object> values : result.valueIt()) {
                        String nextItem = values.get("name").toString().trim();
                        Object nextNameNativeShort = values.get("nameNativeShort");
                        clusterInfoList.put(nextItem, new ClusterInfo(projectId, nextItem, nextNameNativeShort));
                    }

                    for (Element nodeCluster : listCluster) {
                        String nameCluster = nodeCluster.getChildText("nameNativeCluster");
                        if ("Нажмите здесь, если не относится".equals(nameCluster)) {
                            fillOtherCluster = true;
                            otherClusterNativeSubstantiation = nodeCluster.getChildText("nativeSubstantiationClusterProject");
                            otherClusterForeignSubstantiation = nodeCluster.getChildText("foreignSubstantiationClusterProject");
                        } else {
                            if (clusterInfoList.containsKey(nameCluster)) {
                                clusterInfoList.put(nameCluster, new ClusterInfo(projectId, true, nameCluster, null,
                                        nodeCluster.getChildText("nativeSubstantiationClusterProject"),
                                        nodeCluster.getChildText("foreignSubstantiationClusterProject")));
                            }
                        }
                    }

                    for (ClusterInfo nodeCluster : clusterInfoList.values()) {
                        List<Object> rowCluster = new ArrayList<Object>();
                        rowCluster.add(nodeCluster.projectID);
                        rowCluster.add(nodeCluster.inProjectCluster);
                        rowCluster.add(nodeCluster.inProjectCluster); //inClaimerProjectCluster
                        rowCluster.add(nodeCluster.nameNativeCluster);
                        if (fillNative)
                            rowCluster.add(nodeCluster.nativeSubstantiationProjectCluster);
                        if (fillForeign)
                            rowCluster.add(nodeCluster.foreignSubstantiationProjectCluster);
                        dataCluster.add(rowCluster);
                    }

                    if (fillClaimer) {
                        row.add(node.getChildText("phoneClaimer"));
                        row.add(node.getChildText("addressClaimer"));
                        row.add(node.getChildText("siteClaimer"));
                        row.add(node.getChildText("emailClaimer"));
                        row.add(node.getChildText("OGRNClaimer"));
                        row.add(node.getChildText("INNClaimer"));
                        row.add(buildFileByteArray(node.getChild("fileStatementClaimer")));
                        row.add(buildFileByteArray(node.getChild("fileConstituentClaimer")));
                        row.add(buildFileByteArray(node.getChild("fileExtractClaimer")));
                    }

                    ObjectValue expertObject = LM.emailToExpert.readClasses(pInfo.session, new DataObject(node.getChildText("emailProject")));
                    if (expertObject instanceof DataObject)
                        LM.baseLM.objectClass.change(LM.claimerExpert.getSingleClass().ID, context, (DataObject) expertObject);

                    Date date = new java.sql.Date(Integer.parseInt(node.getChildText("yearProject")) - 1900, Integer.parseInt(node.getChildText("monthProject")) - 1, Integer.parseInt(node.getChildText("dayProject")));
                    if (fillDate) {
                        row.add(date);
                        if (fillTwoDates) {
                            row.add(date);
                        }
                    }

                    row.add(BaseUtils.nullBoolean(fillOtherCluster));

                    if (fillNative)
                        row.add(otherClusterNativeSubstantiation);
                    if (fillForeign)
                        row.add(otherClusterForeignSubstantiation);

                    if (fillNative) {
                        row.add(node.getChildText("nameNativeProject"));
                        row.add(node.getChildText("nameNativeManagerProject"));
                        row.add(node.getChildText("nameNativeGenitiveManagerProject"));
                        row.add(node.getChildText("nameNativeDativusManagerProject"));
                        row.add(node.getChildText("nameNativeAblateManagerProject"));
                        row.add(node.getChildText("nativeProblemProject"));
                        row.add(node.getChildText("nativeInnovativeProject"));
                        row.add(node.getChildText("nativeCommentMissionProject"));
                        row.add(node.getChildText("nativeResumeProject"));
                        row.add(node.getChildText("nameNativeContactProject"));
                        row.add(node.getChildText("nativeMarketTrendsProject"));
                        row.add(node.getChildText("nativeRelevanceProject"));
                        row.add(node.getChildText("nativeBasicTechnologyProject"));
                        row.add(node.getChildText("nativeCaseStudiesProject"));
                        row.add(node.getChildText("nativeCharacteristicsAnaloguesProject"));
                        row.add(node.getChildText("nativeCompaniesAnaloguesProject"));
                        row.add(node.getChildText("nativeMarketIntroductionProject"));

                        row.add(node.getChildText("nativeHistoryProject"));
                        row.add(node.getChildText("nativeDynamicsProject"));
                        row.add(node.getChildText("nativeGrantsProject"));
                        row.add(node.getChildText("nativeLaboratoryProject"));
                        row.add(node.getChildText("nativeInvestmentProject"));
                        row.add(node.getChildText("nativeResultsProject"));
                        row.add(node.getChildText("nativeGeneralizedPlanProject"));
                        row.add(node.getChildText("linksMarketIntroductionProject"));
                        row.add(buildFileByteArray(node.getChild("fileRoadMapProject")));
                        row.add(buildFileByteArray(node.getChild("fileNativeTechnicalDescriptionProject")));

                        if (fillClaimer) {
                            row.add(node.getChildText("nameNativeClaimer"));
                            row.add(node.getChildText("firmNameNativeClaimer"));
                        }
                    }

                    if (fillForeign) {
                        row.add(node.getChildText("nameForeignProject"));
                        row.add(node.getChildText("nameForeignManagerProject"));
                        row.add(node.getChildText("foreignProblemProject"));
                        row.add(node.getChildText("foreignInnovativeProject"));
                        row.add(node.getChildText("foreignCommentMissionProject"));
                        row.add(node.getChildText("foreignResumeProject"));
                        row.add(node.getChildText("nameForeignContactProject"));
                        row.add(node.getChildText("foreignMarketTrendsProject"));
                        row.add(node.getChildText("foreignRelevanceProject"));
                        row.add(node.getChildText("foreignBasicTechnologyProject"));
                        row.add(node.getChildText("foreignCaseStudiesProject"));
                        row.add(node.getChildText("foreignCharacteristicsAnaloguesProject"));
                        row.add(node.getChildText("foreignCompaniesAnaloguesProject"));
                        row.add(node.getChildText("foreignMarketIntroductionProject"));

                        row.add(node.getChildText("foreignHistoryProject"));
                        row.add(node.getChildText("foreignDynamicsProject"));
                        row.add(node.getChildText("foreignGrantsProject"));
                        row.add(node.getChildText("foreignLaboratoryProject"));
                        row.add(node.getChildText("foreignInvestmentProject"));
                        row.add(node.getChildText("foreignResultsProject"));
                        row.add(node.getChildText("foreignGeneralizedPlanProject"));
                        row.add(node.getChildText("linksForeignMarketIntroductionProject"));
                        row.add(buildFileByteArray(node.getChild("fileForeignRoadMapProject")));
                        row.add(buildFileByteArray(node.getChild("fileForeignTechnicalDescriptionProject")));

                        if (fillClaimer) {
                            row.add(node.getChildText("nameForeignClaimer"));
                            row.add(node.getChildText("firmNameForeignClaimer"));
                        }
                    }
                    data.add(row);

                    List listPatent = node.getChildren("patent");
                    for (int j = 0; j < listPatent.size(); j++) {
                        Element nodePatent = (Element) listPatent.get(j);
                        List<Object> rowPatent = new ArrayList<Object>();

                        rowPatent.add(projectId);
                        rowPatent.add(nodePatent.getChildText("nativeNumberPatent"));
                        rowPatent.add(BaseUtils.nullString(nodePatent.getChildText("linksPatent")));
                        if (fillNative) {
                            rowPatent.add(nodePatent.getChildText("nativeTypePatent"));
                        }
                        if (fillForeign) {
                            rowPatent.add(nodePatent.getChildText("foreignTypePatent"));
                        }
                        dataPatent.add(rowPatent);
                    }

                    List listResearch = node.getChildren("research");
                    for (int j = 0; j < listResearch.size(); j++) {
                        Element nodeResearch = (Element) listResearch.get(j);
                        List<Object> rowResearch = new ArrayList<Object>();

                        rowResearch.add(projectId);
                        rowResearch.add(nodeResearch.getChildText("dataResearch"));
                        if (fillNative)
                            rowResearch.add(nodeResearch.getChildText("nativeCommentResearch"));
                        if (fillForeign)
                            rowResearch.add(nodeResearch.getChildText("foreignCommentResearch"));
                        dataResearch.add(rowResearch);
                    }

                    List listPublications = node.getChildren("publications");
                    for (int j = 0; j < listPublications.size(); j++) {
                        Element nodePublications = (Element) listPublications.get(j);
                        List<Object> rowPublications = new ArrayList<Object>();

                        rowPublications.add(projectId);
                        rowPublications.add(BaseUtils.nullParseInt(nodePublications.getChildText("datePublications")));
                        rowPublications.add(nodePublications.getChildText("nativeLinksPublications"));
                        if (fillNative) {
                            rowPublications.add(nodePublications.getChildText("nativePublications"));
                            rowPublications.add(nodePublications.getChildText("nativeAuthorPublications"));
                            rowPublications.add(nodePublications.getChildText("nativeEditionPublications"));
                        }
                        if (fillForeign) {
                            rowPublications.add(nodePublications.getChildText("foreignPublications"));
                            rowPublications.add(nodePublications.getChildText("foreignAuthorPublications"));
                            rowPublications.add(nodePublications.getChildText("foreignEditionPublications"));
                        }

                        dataPublications.add(rowPublications);
                    }

                    List listCommercialization = node.getChildren("commercialization");
                    for (int j = 0; j < listCommercialization.size(); j++) {
                        Element nodeCommercialization = (Element) listCommercialization.get(j);
                        List<Object> rowCommercialization = new ArrayList<Object>();

                        rowCommercialization.add(projectId);
                        if (fillNative) {
                            rowCommercialization.add(nodeCommercialization.getChildText("nativeProjectCommercialization"));
                            rowCommercialization.add(nodeCommercialization.getChildText("nativeCommentProjectCommercialization"));
                        }
                        if (fillForeign) {
                            rowCommercialization.add(nodeCommercialization.getChildText("foreignProjectCommercialization"));
                            rowCommercialization.add(nodeCommercialization.getChildText("foreignCommentProjectCommercialization"));
                        }
                        dataCommercialization.add(rowCommercialization);
                    }

                    List listAnalogues = node.getChildren("analogues");
                    for (int j = 0; j < listAnalogues.size(); j++) {
                        Element nodeAnalogues = (Element) listAnalogues.get(j);
                        List<Object> rowAnalogues = new ArrayList<Object>();

                        rowAnalogues.add(projectId);
                        if (fillNative) {
                            rowAnalogues.add(nodeAnalogues.getChildText("nativeProjectAnalogues"));
                            rowAnalogues.add(nodeAnalogues.getChildText("nativeDescriptionProjectAnalogues"));
                            rowAnalogues.add(nodeAnalogues.getChildText("nativeCharacteristicsProjectAnalogues"));
                        }
                        if (fillForeign) {
                            rowAnalogues.add(nodeAnalogues.getChildText("foreignProjectAnalogues"));
                            rowAnalogues.add(nodeAnalogues.getChildText("foreignDescriptionProjectAnalogues"));
                            rowAnalogues.add(nodeAnalogues.getChildText("foreignCharacteristicsProjectAnalogues"));
                        }
                        dataAnalogues.add(rowAnalogues);
                    }

                    List listSpecialist = node.getChildren("specialist");
                    for (int j = 0; j < listSpecialist.size(); j++) {
                        Element nodeSpecialist = (Element) listSpecialist.get(j);
                        List<Object> rowSpecialist = new ArrayList<Object>();

                        rowSpecialist.add(projectId);
                        rowSpecialist.add(buildFileByteArray(nodeSpecialist.getChild("filePassportSpecialist")));
                        rowSpecialist.add(buildFileByteArray(nodeSpecialist.getChild("fileStatementSpecialist")));
                        if (fillNative) {
                            rowSpecialist.add(nodeSpecialist.getChildText("nameNativeSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("nativePostSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("nativeFunctionSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("nativeScopeSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("nativeExperienceSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("nativeTitleSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("nativeWorkSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("nativePublicationsSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("nativeCitationSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("nativeIntellectualPropertySpecialist"));
                        }
                        if (fillForeign) {
                            rowSpecialist.add(nodeSpecialist.getChildText("nameForeignSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("foreignPostSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("foreignFunctionSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("foreignScopeSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("foreignExperienceSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("foreignTitleSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("foreignWorkSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("foreignPublicationsSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("foreignCitationSpecialist"));
                            rowSpecialist.add(nodeSpecialist.getChildText("foreignIntellectualPropertySpecialist"));
                        }
                        dataSpecialist.add(rowSpecialist);
                    }

                    List listMileStone = node.getChildren("mileStone");
                    for (int j = 0; j < listMileStone.size(); j++) {
                        Element nodeMileStone = (Element) listMileStone.get(j);
                        List<Object> rowMileStone = new ArrayList<Object>();

                        rowMileStone.add(projectId);
                        rowMileStone.add(nodeMileStone.getChildText("nativeMileStoneYear"));
                        rowMileStone.add(nodeMileStone.getChildText("nativeMileStone"));
                        rowMileStone.add(j+1); //orderNumberMileStone
                        if (fillNative) {
                            rowMileStone.add(nodeMileStone.getChildText("nativeResearch"));
                            rowMileStone.add(nodeMileStone.getChildText("nativeProductCreation"));
                            rowMileStone.add(nodeMileStone.getChildText("nativePlanOnHiring"));
                            rowMileStone.add(nodeMileStone.getChildText("nativeLicensing"));
                            rowMileStone.add(nodeMileStone.getChildText("nativePromotion"));
                            rowMileStone.add(nodeMileStone.getChildText("nativeSelling"));
                        }
                        if (fillForeign) {
                            rowMileStone.add(nodeMileStone.getChildText("foreignResearch"));
                            rowMileStone.add(nodeMileStone.getChildText("foreignProductCreation"));
                            rowMileStone.add(nodeMileStone.getChildText("foreignPlanOnHiring"));
                            rowMileStone.add(nodeMileStone.getChildText("foreignLicensing"));
                            rowMileStone.add(nodeMileStone.getChildText("foreignPromotion"));
                            rowMileStone.add(nodeMileStone.getChildText("foreignSelling"));
                        }
                        dataMileStone.add(rowMileStone);
                    }

                    List listObjectives = node.getChildren("objectives");
                    for (int j = 0; j < listObjectives.size(); j++) {
                        Element nodeObjectives = (Element) listObjectives.get(j);
                        List<Object> rowObjectives = new ArrayList<Object>();
                        rowObjectives.add(projectId);
                        if (fillNative)
                            rowObjectives.add(nodeObjectives.getChildText("nativeProjectObjectives"));
                        if (fillForeign)
                            rowObjectives.add(nodeObjectives.getChildText("foreignProjectObjectives"));
                        dataObjectives.add(rowObjectives);
                    }

                    // поскольку нам нужно менять properties
                    List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>(this.properties);
                    List<ImportProperty<?>> propertiesNative = new ArrayList<ImportProperty<?>>(this.propertiesNative);
                    List<ImportProperty<?>> propertiesForeign = new ArrayList<ImportProperty<?>>(this.propertiesForeign);
                    List<ImportProperty<?>> propertiesFullClaimer = new ArrayList<ImportProperty<?>>(this.propertiesFullClaimer);
                    List<ImportProperty<?>> propertiesFullClaimerNative = new ArrayList<ImportProperty<?>>(this.propertiesFullClaimerNative);
                    List<ImportProperty<?>> propertiesFullClaimerForeign = new ArrayList<ImportProperty<?>>(this.propertiesFullClaimerForeign);

                    List<ImportField> fieldsNative = BaseUtils.toList(
                            nameNativeProjectField, nameNativeManagerProjectField, nameNativeGenitiveManagerProjectField,
                            nameNativeDativusManagerProjectField, nameNativeAblateManagerProjectField,
                            nativeProblemProjectField, nativeInnovativeProjectField,
                            nativeCommentMissionProjectField, nativeResumeProjectField, nameNativeContactProjectField,
                            nativeMarketTrendsProjectField, nativeRelevanceProjectField, nativeBasicTechnologyProjectField,
                            nativeCaseStudiesProjectField, nativeCharacteristicsAnaloguesProjectField,
                            nativeCompaniesAnaloguesProjectField, nativeMarketIntroductionProjectField,
                            nativeHistoryProjectField, nativeDynamicsProjectField, nativeGrantsProjectField,
                            nativeLaboratoryProjectField, nativeInvestmentProjectField, nativeResultsProjectField,
                            nativeGeneralizedPlanProjectField, linksMarketIntroductionProjectField, fileRoadMapProjectField,
                            fileNativeTechnicalDescriptionProjectField
                    );

                    List<ImportField> fieldsFullClaimerNative = BaseUtils.toList(nameNativeClaimerField, firmNameNativeClaimerField);

                    List<ImportField> fieldsForeign = BaseUtils.toList(
                            nameForeignProjectField, nameForeignManagerProjectField, foreignProblemProjectField, foreignInnovativeProjectField,
                            foreignCommentMissionProjectField, foreignResumeProjectField, nameForeignContactProjectField,
                            foreignMarketTrendsProjectField, foreignRelevanceProjectField, foreignBasicTechnologyProjectField,
                            foreignCaseStudiesProjectField, foreignCharacteristicsAnaloguesProjectField,
                            foreignCompaniesAnaloguesProjectField, foreignMarketIntroductionProjectField,
                            foreignHistoryProjectField, foreignDynamicsProjectField, foreignGrantsProjectField,
                            foreignLaboratoryProjectField, foreignInvestmentProjectField, foreignResultsProjectField,
                            foreignGeneralizedPlanProjectField, linksForeignMarketIntroductionProjectField, fileForeignRoadMapProjectField,
                            fileForeignTechnicalDescriptionProjectField
                    );

                    List<ImportField> fieldsFullClaimerForeign = BaseUtils.toList(nameForeignClaimerField, firmNameForeignClaimerField);

                    List<ImportField> fieldsBoth = BaseUtils.toList(
                            fillNativeProjectField, fillForeignProjectField,
                            isConsultingCenterQuestionProjectField, isConsultingCenterCommentProjectField, consultingCenterCommentProjectField,
                            phoneContactProjectField,
                            emailContactProjectField, linksMarketTrendsProjectField, linksAnalogProjectField,
                            projectIdField, updateDateProjectField, fileMinutesOfMeetingExpertCollegiumProjectField,
                            fileWrittenConsentClaimerProjectField, projectMissionProjectField, projectScheduleProjectField,
                            projectActionProjectField, emailClaimerField
                    );

                   List<ImportField> fieldsFullClaimerBoth = BaseUtils.toList(phoneClaimerField, addressClaimerField,
                            siteClaimerField, emailFirmClaimerField,
                            OGRNClaimerField, INNClaimerField, fileStatementClaimerField,
                            fileConstituentClaimerField, fileExtractClaimerField);

                    if (fillClaimer) {
                        fieldsNative.addAll(fieldsFullClaimerNative);
                        fieldsForeign.addAll(fieldsFullClaimerForeign);
                        fieldsBoth.addAll(fieldsFullClaimerBoth);
                        propertiesNative.addAll(propertiesFullClaimerNative);
                        propertiesForeign.addAll(propertiesFullClaimerForeign);
                        properties.addAll(propertiesFullClaimer);
                    }

                    if (fillDate) {
                        if (fillTwoDates) {
                            properties.add(propertyStatusDate);
                            fieldsBoth.add(dateStatusProjectField);

                            properties.add(propertyDate);
                            fieldsBoth.add(dateProjectField);

                        } else if (fillClaimer) {
                            properties.add(propertyStatusDate);
                            fieldsBoth.add(dateStatusProjectField);
                        } else {
                            properties.add(propertyDate);
                            fieldsBoth.add(dateProjectField);
                        }
                    }

                    properties.add(propertyOtherCluster);
                    fieldsBoth.add(isOtherClusterProjectField);
                    if (fillNative) {
                        properties.add(propertyOtherClusterNative);
                        fieldsBoth.add(nativeSubstantiationOtherClusterProjectField);
                    }
                    if (fillForeign) {
                        properties.add(propertyOtherClusterForeign);
                        fieldsBoth.add(foreignSubstantiationOtherClusterProjectField);
                    }

                    ImportKey<?>[] keysArray = new ImportKey<?>[]{projectKey, projectMissionProjectKey, projectScheduleProjectKey, projectActionProjectKey, claimerKey};
                    SessionTableUsage<String, ImportField> projectTable = null;
                    if(1==1) throw new RuntimeException("not supported any more");
                    importMultilanguageData(pInfo,
                                                    fieldsBoth, fieldsNative, fieldsForeign,
                                                    properties, propertiesNative, propertiesForeign,
                                                    data, keysArray, null, fillNative, fillForeign);

                    List<ImportField> fieldsCurrentClusterBoth = BaseUtils.toList(
                            projectIdField, inProjectClusterField, inClaimerProjectClusterField, nameNativeClusterField);
                    List<ImportField> fieldsCurrentClusterNative = BaseUtils.toList(
                            nativeSubstantiationProjectClusterField);
                    List<ImportField> fieldsCurrentClusterForeign = BaseUtils.toList(
                            foreignSubstantiationProjectClusterField);

                    keysArray = new ImportKey<?>[]{clusterKey, projectKey};
                    importMultilanguageData(pInfo,
                            fieldsCurrentClusterBoth, fieldsCurrentClusterNative, fieldsCurrentClusterForeign,
                            propertiesCluster, propertiesClusterNative, propertiesClusterForeign,
                            dataCluster, keysArray, null, fillNative, fillForeign);

                    List<ImportField> fieldsPatentBoth = BaseUtils.toList(
                            projectIdField, nativeNumberPatentField, linksPatentField);
                    List<ImportField> fieldsPatentNative = BaseUtils.toList(nativeTypePatentField);
                    List<ImportField> fieldsPatentForeign = BaseUtils.toList(foreignTypePatentField);
                    keysArray = new ImportKey<?>[]{projectKey, patentKey};
                    importMultilanguageData(pInfo,
                            fieldsPatentBoth, fieldsPatentNative, fieldsPatentForeign,
                            propertiesPatent, propertiesPatentNative, propertiesPatentForeign,
                            dataPatent, keysArray,
                            Collections.singletonList(new ImportDelete(patentKey,
                                                                       LM.equalsPatentProject.getMapping(patentKey,
                                                                                                         new ImportKeyTable(projectKey, projectTable)),
                                                                       false)),
                            fillNative, fillForeign);


                    List<ImportField> fieldsResearchBoth = BaseUtils.toList(
                            projectIdField, dataResearchField);
                    List<ImportField> fieldsResearchNative = BaseUtils.toList(nativeCommentResearchField);
                    List<ImportField> fieldsResearchForeign = BaseUtils.toList(foreignCommentResearchField);
                    keysArray = new ImportKey<?>[]{projectKey, researchKey};
                    importMultilanguageData(pInfo,
                            fieldsResearchBoth, fieldsResearchNative, fieldsResearchForeign,
                            propertiesResearch, propertiesResearchNative, propertiesResearchForeign,
                            dataResearch, keysArray,
                            Collections.singletonList(new ImportDelete(researchKey,
                                                                       LM.equalsResearchProject.getMapping(researchKey,
                                                                                                           new ImportKeyTable(projectKey, projectTable)),
                                                                       false)),
                            fillNative, fillForeign);

                    List<ImportField> fieldsPublicationsBoth = BaseUtils.toList(
                            projectIdField, datePublicationsField, nativeLinksPublicationsField);
                    List<ImportField> fieldsPublicationsNative = BaseUtils.toList(nativePublicationsField, nativeAuthorPublicationsField, nativeEditionPublicationsField);
                    List<ImportField> fieldsPublicationsForeign = BaseUtils.toList(foreignPublicationsField, foreignAuthorPublicationsField, foreignEditionPublicationsField);
                    keysArray = new ImportKey<?>[]{projectKey, publicationsKey};
                    importMultilanguageData(pInfo,
                            fieldsPublicationsBoth, fieldsPublicationsNative, fieldsPublicationsForeign,
                            propertiesPublications, propertiesPublicationsNative, propertiesPublicationsForeign,
                            dataPublications, keysArray,
                            Collections.singletonList(new ImportDelete(publicationsKey,
                                                                       LM.equalsPublicationsProject.getMapping(publicationsKey,
                                                                                                               new ImportKeyTable(projectKey, projectTable)),
                                                                       false)),
                            fillNative, fillForeign);

                    List<ImportField> fieldsCommercializationBoth = BaseUtils.toList(
                            projectIdField);
                    List<ImportField> fieldsCommercializationNative = BaseUtils.toList(nativeProjectCommercializationField, nativeCommentProjectCommercializationField);
                    List<ImportField> fieldsCommercializationForeign = BaseUtils.toList(foreignProjectCommercializationField, foreignCommentProjectCommercializationField);
                    keysArray = new ImportKey<?>[]{projectKey, commercializationKey};
                    importMultilanguageData(pInfo,
                            fieldsCommercializationBoth, fieldsCommercializationNative, fieldsCommercializationForeign,
                            propertiesCommercialization, propertiesCommercializationNative, propertiesCommercializationForeign,
                            dataCommercialization, keysArray,
                            Collections.singletonList(new ImportDelete(commercializationKey,
                                                                       LM.equalsCommercializationProject.getMapping(commercializationKey,
                                                                                                                    new ImportKeyTable(projectKey, projectTable)),
                                                                       false)),
                            fillNative, fillForeign);

                    List<ImportField> fieldsAnaloguesBoth = BaseUtils.toList(
                            projectIdField);
                    List<ImportField> fieldsAnaloguesNative = BaseUtils.toList(nativeProjectAnaloguesField, nativeDescriptionProjectAnaloguesField, nativeCharacteristicsProjectAnaloguesField);
                    List<ImportField> fieldsAnaloguesForeign = BaseUtils.toList(foreignProjectAnaloguesField, foreignDescriptionProjectAnaloguesField, foreignCharacteristicsProjectAnaloguesField);
                    keysArray = new ImportKey<?>[]{projectKey, analoguesKey};
                    importMultilanguageData(pInfo,
                            fieldsAnaloguesBoth, fieldsAnaloguesNative, fieldsAnaloguesForeign,
                            propertiesAnalogues, propertiesAnaloguesNative, propertiesAnaloguesForeign,
                            dataAnalogues, keysArray,
                            Collections.singletonList(new ImportDelete(analoguesKey,
                                                                       LM.equalsAnaloguesProject.getMapping(analoguesKey,
                                                                                                            new ImportKeyTable(projectKey, projectTable)),
                                                                       false)),
                            fillNative, fillForeign);

                    List<ImportField> fieldsSpecialistBoth = BaseUtils.toList(
                            projectIdField, filePassportSpecialistField, fileStatementSpecialistField);
                    List<ImportField> fieldsSpecialistNative = BaseUtils.toList(
                            nameNativeSpecialistField, nativePostSpecialistField, nativeFunctionSpecialistField,
                            nativeScopeSpecialistField, nativeExperienceSpecialistField, nativeTitleSpecialistField,
                            nativeWorkSpecialistField, nativePublicationsSpecialistField, nativeCitationSpecialistField,
                            nativeIntellectualPropertySpecialistField
                    );
                    List<ImportField> fieldsSpecialistForeign = BaseUtils.toList(
                            nameForeignSpecialistField, foreignPostSpecialistField, foreignFunctionSpecialistField,
                            foreignScopeSpecialistField, foreignExperienceSpecialistField, foreignTitleSpecialistField,
                            foreignWorkSpecialistField, foreignPublicationsSpecialistField, foreignCitationSpecialistField,
                            foreignIntellectualPropertySpecialistField);
                    keysArray = new ImportKey<?>[]{projectKey, specialistKey};
                    importMultilanguageData(pInfo,
                            fieldsSpecialistBoth, fieldsSpecialistNative, fieldsSpecialistForeign,
                            propertiesSpecialist, propertiesSpecialistNative, propertiesSpecialistForeign,
                            dataSpecialist, keysArray,
                            Collections.singletonList(new ImportDelete(specialistKey,
                                                                       LM.equalsSpecialistProject.getMapping(specialistKey,
                                                                                                             new ImportKeyTable(projectKey, projectTable)),
                                                                       false)),
                            fillNative, fillForeign);

                    List<ImportField> fieldsMileStoneBoth = BaseUtils.toList(
                            projectIdField, nativeMileStoneYearField, nativeMileStoneField, orderNumberMileStoneField);
                    List<ImportField> fieldsMileStoneNative = BaseUtils.toList(nativeResearchDescriptionTypeMileStoneMileStoneField, nativeProductCreationDescriptionTypeMileStoneMileStoneField,
                            nativePlanOnHiringDescriptionTypeMileStoneMileStoneField, nativeLicensingDescriptionTypeMileStoneMileStoneField,
                            nativePromotionDescriptionTypeMileStoneMileStoneField, nativeSellingDescriptionTypeMileStoneMileStoneField);
                    List<ImportField> fieldsMileStoneForeign = BaseUtils.toList(foreignResearchDescriptionTypeMileStoneMileStoneField, foreignProductCreationDescriptionTypeMileStoneMileStoneField,
                            foreignPlanOnHiringDescriptionTypeMileStoneMileStoneField, foreignLicensingDescriptionTypeMileStoneMileStoneField,
                            foreignPromotionDescriptionTypeMileStoneMileStoneField, foreignSellingDescriptionTypeMileStoneMileStoneField);
                    keysArray = new ImportKey<?>[]{projectKey, mileStoneYearKey, mileStoneKey};
                    importMultilanguageData(pInfo,
                            fieldsMileStoneBoth, fieldsMileStoneNative, fieldsMileStoneForeign,
                            propertiesMileStone, propertiesMileStoneNative, propertiesMileStoneForeign,
                            dataMileStone, keysArray,
                            BaseUtils.toList(new ImportDelete(mileStoneYearKey,
                                                              LM.equalsMileStoneYearProject.getMapping(mileStoneYearKey,
                                                                                                       new ImportKeyTable(projectKey, projectTable)),
                                                              false),
                                             new ImportDelete(mileStoneKey,
                                                              LM.equalsMileStoneProject.getMapping(mileStoneKey,
                                                                                                   new ImportKeyTable(projectKey, projectTable)),
                                                              false)),
                            fillNative, fillForeign);

                    List<ImportField> fieldsObjectivesBoth = BaseUtils.toList(
                            projectIdField);
                    List<ImportField> fieldsObjectivesNative = BaseUtils.toList(nativeProjectObjectivesField);
                    List<ImportField> fieldsObjectivesForeign = BaseUtils.toList(foreignProjectObjectivesField);
                    keysArray = new ImportKey<?>[]{projectKey, objectivesKey};
                    importMultilanguageData(pInfo,
                            fieldsObjectivesBoth, fieldsObjectivesNative, fieldsObjectivesForeign,
                            propertiesObjectives, propertiesObjectivesNative, propertiesObjectivesForeign,
                            dataObjectives, keysArray,
                            Collections.singletonList(new ImportDelete(objectivesKey,
                                                                       LM.equalsObjectivesProject.getMapping(objectivesKey,
                                                                                                             new ImportKeyTable(projectKey, projectTable)),
                                                                       false)),
                            fillNative, fillForeign);
                }
            }
        } catch (JDOMParseException e) {
            String info = "failed to import project " + projectId + ". Reason: " + new String(pInfo.responseContents);
//            String info = e.getCause() + " : projectId=" + projectId + " : " + new String(responseContents);
            pInfo.toLog += info;
            logger.info(info);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String sessionApply = pInfo.session.applyMessage(context.getBL());
        if (sessionApply != null) {
            String info = "failed to import project " + projectId + ". Constraint: " + sessionApply;
            pInfo.toLog += info;
            logger.error(info);
            pInfo.session.cancel();
        } else {
            logger.info(projectId + " project was imported successfully");
        }
    }

    private void importMultilanguageData (
            PrivateInfo pInfo,
            List<ImportField> fieldsBoth, List<ImportField> fieldsNative, List<ImportField> fieldsForeign,
            List<ImportProperty<?>> propertiesBoth, List<ImportProperty<?>> propertiesNative, List<ImportProperty<?>> propertiesForeign,
            List<List<Object>> data, ImportKey<?>[] keysArray, Collection<ImportDelete> deletes, boolean fillNative, boolean fillForeign) throws SQLException {

        List<ImportField> fieldsMerged = new ArrayList<ImportField>(fieldsBoth);
        List<ImportProperty<?>> propertiesMerged = new ArrayList<ImportProperty<?>>(propertiesBoth);
        if (fillNative) {
            fieldsMerged.addAll(fieldsNative);
            propertiesMerged.addAll(propertiesNative);
        }
        if (fillForeign) {
            fieldsMerged.addAll(fieldsForeign);
            propertiesMerged.addAll(propertiesForeign);
        }

        try {
            new IntegrationService(pInfo.session,
                                          new ImportTable(fieldsMerged, data),
                                          Arrays.asList(keysArray),
                                          propertiesMerged,
                                          deletes).synchronize(true);
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при импорте данных", e);
        }
    }

    public class ClusterInfo {
        public String projectID;
        public Object inProjectCluster;
        public String nameNativeCluster;
        public Object shortNameNativeCluster;
        public String nativeSubstantiationProjectCluster;
        public String foreignSubstantiationProjectCluster;

        public ClusterInfo(String projectID, String nameNativeCluster, Object shortNameNativeCluster) {
            this(projectID, null, nameNativeCluster, shortNameNativeCluster, null, null);
        }

        public ClusterInfo(String projectID, Object inProjectCluster, String nameNativeCluster, Object shortNameNativeCluster,
                           String nativeSubstantiationProjectCluster, String foreignSubstantiationProjectCluster) {
            this.projectID = projectID;
            this.inProjectCluster = inProjectCluster;
            this.nameNativeCluster = nameNativeCluster;
            this.shortNameNativeCluster = shortNameNativeCluster;
            this.nativeSubstantiationProjectCluster = nativeSubstantiationProjectCluster;
            this.foreignSubstantiationProjectCluster = foreignSubstantiationProjectCluster;
        }

    }

    public class PrivateInfo {
        private DataSession session;
        private byte[] responseContents;
        private String toLog = "";
        private Integer projectsImportLimit;
    }


}
