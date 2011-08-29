package skolkovo.actions;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OrderedMap;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;
import skolkovo.SkolkovoBusinessLogics;
import skolkovo.SkolkovoLogicsModule;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class ImportProjectsActionProperty extends ActionProperty {

    private SkolkovoLogicsModule LM;
    private SkolkovoBusinessLogics BL;
    private DataSession session;
    private static Logger logger = Logger.getLogger("import");
    private byte[] responseContents;
    private String toLog = "";
    private boolean onlyMessage;
    private boolean onlyReplace;
    private boolean fillSids;
    private Integer projectsImportLimit;

    public ImportProjectsActionProperty(String caption, SkolkovoLogicsModule LM, SkolkovoBusinessLogics BL, boolean onlyMessage, boolean onlyReplace, boolean fillSids) {
        super(LM.genSID(), caption, new ValueClass[]{});
        this.LM = LM;
        this.BL = BL;
        this.onlyMessage = onlyMessage;
        this.onlyReplace = onlyReplace;
        this.fillSids = fillSids;
    }

    protected ImportField dateProjectField,
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

            isReturnInvestmentsProjectField, nameReturnInvestorProjectField, amountReturnFundsProjectField,
            isNonReturnInvestmentsProjectField, isCapitalInvestmentProjectField, isPropertyInvestmentProjectField, isGrantsProjectField, isOtherNonReturnInvestmentsProjectField,
            nameNonReturnInvestorProjectField, amountNonReturnFundsProjectField, commentOtherNonReturnInvestmentsProjectField,
            isOwnFundsProjectField, amountOwnFundsProjectField,
            isPlanningSearchSourceProjectField, amountFundsProjectField,
            isOtherSoursesProjectField, commentOtherSoursesProjectField, updateDateProjectField,

            nameNativeClusterField, inProjectClusterField, numberCurrentClusterField,
            isOtherClusterProjectField, nativeSubstantiationOtherClusterProjectField, foreignSubstantiationOtherClusterProjectField,
            nameNativeClaimerField, nameForeignClaimerField,
            firmNameNativeClaimerField, firmNameForeignClaimerField, phoneClaimerField, addressClaimerField, siteClaimerField,
            emailClaimerField, emailFirmClaimerField, OGRNClaimerField, INNClaimerField,
            fileStatementClaimerField, fileConstituentClaimerField, fileExtractClaimerField,
            fileNativeSummaryProjectField, fileForeignSummaryProjectField,
            fileRoadMapProjectField,
            nativeTypePatentField, foreignTypePatentField, nativeNumberPatentField, foreignNumberPatentField,
            datePatentField, isOwnedPatentField, ownerPatentField, ownerTypePatentField, isValuatedPatentField, valuatorPatentField,
            fileIntentionOwnerPatentField, fileActValuationPatentField,
            fullNameAcademicField, institutionAcademicField, titleAcademicField,
            fileDocumentConfirmingAcademicField, fileDocumentEmploymentAcademicField,
            fullNameNonRussianSpecialistField, organizationNonRussianSpecialistField, titleNonRussianSpecialistField,
            fileForeignResumeNonRussianSpecialistField, fileNativeResumeNonRussianSpecialistField,
            filePassportNonRussianSpecialistField, fileStatementNonRussianSpecialistField,
            fileNativeTechnicalDescriptionProjectField, fileForeignTechnicalDescriptionProjectField;

    ImportKey<?> projectKey, projectTypeProjectKey, projectActionProjectKey, claimerKey, patentKey, ownerTypePatentKey, clusterKey, academicKey, nonRussianSpecialistKey;
    List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
    ImportProperty<?> propertyDate;
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

    private void initFieldsNProperties() {
        dateProjectField = new ImportField(LM.baseLM.date);
        projectIdField = new ImportField(LM.sidProject);
        nameNativeProjectField = new ImportField(LM.nameNative);
        nameForeignProjectField = new ImportField(LM.nameForeign);
        nameNativeManagerProjectField = new ImportField(LM.nameNativeManagerProject);
        nameNativeGenitiveManagerProjectField = new ImportField(LM.nameNativeGenitiveManagerProject);
        nameNativeDativusManagerProjectField = new ImportField(LM.nameNativeDativusManagerProject);
        nameNativeAblateManagerProjectField = new ImportField(LM.nameNativeAblateManagerProject);
        nameForeignManagerProjectField = new ImportField(LM.nameForeignManagerProject);
        nativeProblemProjectField = new ImportField(LM.nativeProblemProject);
        foreignProblemProjectField = new ImportField(LM.foreignProblemProject);
        nativeInnovativeProjectField = new ImportField(LM.nativeInnovativeProject);
        foreignInnovativeProjectField = new ImportField(LM.foreignInnovativeProject);
        projectTypeProjectField = new ImportField(LM.baseLM.classSID);
        projectActionProjectField = new ImportField(LM.baseLM.classSID);
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

        nameNativeClusterField = new ImportField(LM.nameNativeCluster);
        inProjectClusterField = new ImportField(LM.inProjectCluster);
        numberCurrentClusterField = new ImportField(LM.numberCluster);
        nativeSubstantiationProjectClusterField = new ImportField(LM.nativeSubstantiationProjectCluster);
        foreignSubstantiationProjectClusterField = new ImportField(LM.foreignSubstantiationProjectCluster);

        nameNativeClaimerField = new ImportField(LM.nameNativeClaimer);
        nameForeignClaimerField = new ImportField(LM.nameForeignClaimer);
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
        fileRoadMapProjectField = new ImportField(LM.fileRoadMapProject);
        fileNativeTechnicalDescriptionProjectField = new ImportField(LM.fileNativeTechnicalDescriptionProject);
        fileForeignTechnicalDescriptionProjectField = new ImportField(LM.fileForeignTechnicalDescriptionProject);

        nativeTypePatentField = new ImportField(LM.nativeTypePatent);
        foreignTypePatentField = new ImportField(LM.foreignTypePatent);
        nativeNumberPatentField = new ImportField(LM.nativeNumberPatent);
        foreignNumberPatentField = new ImportField(LM.foreignNumberPatent);
        datePatentField = new ImportField(LM.priorityDatePatent);
        isOwnedPatentField = new ImportField(LM.isOwned);
        ownerPatentField = new ImportField(LM.ownerPatent);
        ownerTypePatentField = new ImportField(LM.baseLM.classSID);
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

        projectTypeProjectKey = new ImportKey(LM.projectType, LM.projectTypeToSID.getMapping(projectTypeProjectField));
        properties.add(new ImportProperty(projectTypeProjectField, LM.projectTypeProject.getMapping(projectKey),
                LM.baseLM.object(LM.projectType).getMapping(projectTypeProjectKey)));

        projectActionProjectKey = new ImportKey(LM.projectAction, LM.projectActionToSID.getMapping(projectActionProjectField));
        properties.add(new ImportProperty(projectActionProjectField, LM.projectActionProject.getMapping(projectKey),
                LM.baseLM.object(LM.projectAction).getMapping(projectActionProjectKey)));

        claimerKey = new ImportKey(LM.claimer, LM.baseLM.emailToObject.getMapping(emailClaimerField));
        properties.add(new ImportProperty(emailClaimerField, LM.baseLM.email.getMapping(claimerKey)));

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
        propertiesFullClaimer.add(new ImportProperty(fileRoadMapProjectField, LM.fileRoadMapProject.getMapping(projectKey)));

        propertyDate = new ImportProperty(dateProjectField, LM.dateJoinProject.getMapping(projectKey));

        propertiesNative.add(new ImportProperty(nameNativeProjectField, LM.nameNativeProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeManagerProjectField, LM.nameNativeManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeGenitiveManagerProjectField, LM.nameNativeGenitiveManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeDativusManagerProjectField, LM.nameNativeDativusManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nameNativeAblateManagerProjectField, LM.nameNativeAblateManagerProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeProblemProjectField, LM.nativeProblemProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeInnovativeProjectField, LM.nativeInnovativeProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(nativeSubstantiationProjectTypeField, LM.nativeSubstantiationProjectType.getMapping(projectKey)));

        propertiesNative.add(new ImportProperty(fileNativeSummaryProjectField, LM.fileNativeSummaryProject.getMapping(projectKey)));
        propertiesNative.add(new ImportProperty(fileNativeTechnicalDescriptionProjectField, LM.fileNativeTechnicalDescriptionProject.getMapping(projectKey)));

        properties.add(new ImportProperty(emailClaimerField, LM.claimerProject.getMapping(projectKey),
                LM.baseLM.object(LM.claimer).getMapping(claimerKey)));

        propertiesFullClaimerNative = new ArrayList<ImportProperty<?>>();
        propertiesFullClaimerNative.add(new ImportProperty(nameNativeClaimerField, LM.nameNativeClaimer.getMapping(claimerKey)));
        propertiesFullClaimerNative.add(new ImportProperty(firmNameNativeClaimerField, LM.firmNameNativeClaimer.getMapping(claimerKey)));

        propertiesForeign.add(new ImportProperty(nameForeignProjectField, LM.nameForeignProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(nameForeignManagerProjectField, LM.nameForeignManagerProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignProblemProjectField, LM.foreignProblemProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignInnovativeProjectField, LM.foreignInnovativeProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(foreignSubstantiationProjectTypeField, LM.foreignSubstantiationProjectType.getMapping(projectKey)));

        propertiesForeign.add(new ImportProperty(fileForeignSummaryProjectField, LM.fileForeignSummaryProject.getMapping(projectKey)));
        propertiesForeign.add(new ImportProperty(fileForeignTechnicalDescriptionProjectField, LM.fileForeignTechnicalDescriptionProject.getMapping(projectKey)));

        propertiesFullClaimerForeign = new ArrayList<ImportProperty<?>>();
        propertiesFullClaimerForeign.add(new ImportProperty(nameForeignClaimerField, LM.nameForeignClaimer.getMapping(claimerKey)));
        propertiesFullClaimerForeign.add(new ImportProperty(firmNameForeignClaimerField, LM.firmNameForeignClaimer.getMapping(claimerKey)));

        patentKey = new ImportKey(LM.patent, LM.nativeNumberToPatent.getMapping(nativeNumberPatentField));
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

        academicKey = new ImportKey(LM.academic, LM.fullNameToAcademic.getMapping(fullNameAcademicField));
        propertiesAcademic.add(new ImportProperty(fullNameAcademicField, LM.fullNameAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(institutionAcademicField, LM.institutionAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(titleAcademicField, LM.titleAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(fileDocumentConfirmingAcademicField, LM.fileDocumentConfirmingAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(fileDocumentEmploymentAcademicField, LM.fileDocumentEmploymentAcademic.getMapping(academicKey)));
        propertiesAcademic.add(new ImportProperty(projectIdField, LM.projectAcademic.getMapping(academicKey),
                LM.baseLM.object(LM.project).getMapping(projectKey)));

        nonRussianSpecialistKey = new ImportKey(LM.nonRussianSpecialist, LM.fullNameToNonRussianSpecialist.getMapping(fullNameNonRussianSpecialistField));
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

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        throw new RuntimeException("no need");
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        this.session = context.getSession();
        toLog = "";

        projectsImportLimit = (Integer) LM.projectsImportLimit.read(context);
        if (projectsImportLimit == null) {
            projectsImportLimit = 100;
        }

        try {
            String host = getHost();
            Map<String, Timestamp> projects = importProjectsFromXML(host);

            if (!onlyMessage && !fillSids) {
                initFieldsNProperties();
                for (String projectId : projects.keySet()) {
                    URL url = new URL(host + "&show=all&projectId=" + projectId);
                    URLConnection connection = url.openConnection();
                    connection.setDoOutput(false);
                    connection.setDoInput(true);
                    importProject(connection.getInputStream(), projectId, projects.get(projectId));
                    System.gc();
                }
            }
            String message = "";
            if (projects == null || !projects.isEmpty()) {
                if (!onlyMessage) {
                    message = "Данные были успешно приняты\n";
                }
                message += toLog;
            } else {
                if (responseContents != null) {
                    message = new String(responseContents);
                } else {
                    message = "Вся информация актуальна";
                }
            }
            context.addAction(new MessageClientAction(message, "Импорт", true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Timestamp> importProjectsFromXML(String host) throws IOException, SQLException {
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = null;
        List<Element> elementList = null;
        try {
            URL url = new URL(host + "&show=projects&limit=1000");
            URLConnection connection = url.openConnection();

            connection.setDoOutput(false);
            connection.setDoInput(true);

            responseContents = IOUtils.readBytesFromStream(connection.getInputStream());

            //File file = new File("C://test.xml");
            //InputStream is = new FileInputStream(file);
            //long length = file.length();
            //byte[] bytes = new byte[(int) length];
            //int offset = 0;
            //int numRead = 0;
            //while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            //    offset += numRead;
            //}
            //is.close();
            //responseContents = bytes;

            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(new ByteArrayInputStream(responseContents));
            responseContents = null;

            LP isProject = LM.is(LM.project);
            Map<Object, KeyExpr> keys = isProject.getMapKeys();
            Query<Object, Object> query = new Query<Object, Object>(keys);
            query.properties.put("id", LM.sidProject.getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("email", LM.emailClaimerProject.getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("name", LM.nameNative.getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("date", LM.updateDateProject.getExpr(BaseUtils.singleValue(keys)));
            if (fillSids) {
                query.and(isProject.property.getExpr(keys).getWhere());
            } else {
                query.and(LM.sidProject.getExpr(BaseUtils.singleValue(keys)).getWhere());
            }
            result = query.execute(session.sql);

            Element rootNode = document.getRootElement();
            elementList = new ArrayList<Element>(rootNode.getChildren("project"));
        } catch (JDOMParseException e) {
            logger.error(e.getCause() + " : " + new String(responseContents));
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
            fillSids(result, elementList);
            logger.info("Imported sids:" + toLog);
            return null;
        }

        return makeImportList(result, elementList);
    }

    public String getHost() throws NoSuchAlgorithmException {
        Calendar calTZ = new GregorianCalendar(TimeZone.getTimeZone("EST"));
        calTZ.setTimeInMillis(new java.util.Date().getTime());
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 15);
        cal.set(Calendar.DAY_OF_MONTH, calTZ.get(Calendar.DAY_OF_MONTH) - 3);
        cal.set(Calendar.MONTH, calTZ.get(Calendar.MONTH));
        cal.set(Calendar.YEAR, calTZ.get(Calendar.YEAR));
        String string = cal.getTimeInMillis() / 1000 + "_1q2w3e";

        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(string.getBytes(), 0, string.length());
        return "http://app.i-gorod.com/xml.php?hash=" + new BigInteger(1, m.digest()).toString(16);
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

    public Map<String, Timestamp> makeImportList(OrderedMap<Map<Object, Object>, Map<Object, Object>> data, List<Element> elementList) {
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
                for (Map<Object, Object> project : data.values()) {
                    Timestamp date = (Timestamp) project.get("date");
                    if (project.get("id").toString().trim().equals(projectId)) {
                        if (date == null || currentProjectDate.after(date)) {
                            Object email = project.get("email");
                            toLogString(projectId, email == null ? null : email.toString().trim(), element.getChildText("emailProject"), name.toString().trim(), name, date, currentProjectDate);
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
                    toLogString(projectId, null, element.getChildText("emailProject"), null, name, null, currentProjectDate);
                    projectList.put(projectId, currentProjectDate);
                    counter++;
                }
                if (counter == projectsImportLimit) {
                    break;
                }
            }
        }

        if (!onlyMessage) {
            logger.info("Projects to import (" + projectList.size() + "):" + toLog);
        }
        return projectList;
    }

    public void toLogString(Object projectId, Object oldEmail, Object newEmail, Object oldName, Object newName, Object oldDate, Object newDate) {
        toLog += "\nprojectId: " + projectId + "\n\temail: " + oldEmail + " <- " + newEmail + "\n\tname: " + oldName +
                " <- " + newName + "\n\tdate: " + oldDate + " <- " + newDate;
    }

    public void fillSids(OrderedMap<Map<Object, Object>, Map<Object, Object>> data, List<Element> elements) throws SQLException {
        for (Map<Object, Object> key : data.keySet()) {
            Map<Object, Object> project = data.get(key);
            if (project.get("id") == null) {
                Object email = project.get("email");
                if (email != null && getEmailRepeatings(data, elements, email.toString().trim()) == 1) {
                    String projectId = getProjectId(elements, email.toString().trim());
                    if (projectId != null) {
                        LM.sidProject.execute(projectId, session, session.getDataObject(BaseUtils.singleValue(key), LM.project.getType()));
                        toLog += "\nprojectId: " + projectId + "\n\temail: " + email.toString().trim() + "\n\t" +
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

    public int getEmailRepeatings(OrderedMap<Map<Object, Object>, Map<Object, Object>> data, List<Element> elements, String email) {
        int repeatings = 0;
        for (Map<Object, Object> project : data.values()) {
            if (project.get("email") != null && project.get("email").toString().trim().toLowerCase().equals(email.toLowerCase())) {
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

    public void importProject(InputStream inputStream, String projectId, Timestamp currentProjectDate) throws SQLException {
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<List<Object>> dataCluster = new ArrayList<List<Object>>();
        List<List<Object>> dataPatent = new ArrayList<List<Object>>();
        List<List<Object>> dataAcademic = new ArrayList<List<Object>>();
        List<List<Object>> dataNonRussianSpecialist = new ArrayList<List<Object>>();
        List<Object> row;

        SAXBuilder builder = new SAXBuilder();

        try {
            responseContents = IOUtils.readBytesFromStream(inputStream);

            //File file = new File("C://test.xml");
            //InputStream is = new FileInputStream(file);
            //long length = file.length();
            //byte[] bytes = new byte[(int) length];
            //int offset = 0;
            //int numRead = 0;
            //while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            //    offset += numRead;
            //}
            //is.close();
            //responseContents = bytes;

            Document document = builder.build(new ByteArrayInputStream(responseContents));
            responseContents = null;
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

                if (fillNative) row.add(true);
                else row.add(null); //fillNativeProject
                if (fillForeign) row.add(true);
                else row.add(null); //fillForeignProject

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

                row.add(node.getChildText("typeProject"));
                row.add(node.getChildText("actionProject"));

                row.add(node.getChildText("emailProject"));
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
                    row.add(buildFileByteArray(node.getChild("fileRoadMapProject")));
                }
                if (fillDate) {
                    row.add(new java.sql.Date(Integer.parseInt(node.getChildText("yearProject")) - 1900, Integer.parseInt(node.getChildText("monthProject")) - 1, Integer.parseInt(node.getChildText("dayProject"))));
                }

                LP isCluster = LM.is(LM.cluster);
                Map<Object, KeyExpr> keys = isCluster.getMapKeys();
                Query<Object, Object> query = new Query<Object, Object>(keys);
                query.properties.put("name", LM.nameNative.getExpr(BaseUtils.singleValue(keys)));
                query.and(isCluster.property.getExpr(keys).getWhere());
                OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);

                boolean fillOtherCluster = false;
                Object otherClusterNativeSubstantiation = null, otherClusterForeignSubstantiation = null;

                List<Element> listCluster = node.getChildren("cluster");
                Map<String, ClusterInfo> clusterInfoList = new HashMap<String, ClusterInfo>();

                for (Map<Object, Object> values : result.values()) {
                    String nextItem = values.get("name").toString().trim();
                    clusterInfoList.put(nextItem, new ClusterInfo(nextItem));
                }

                for (Element nodeCluster : listCluster) {
                    String nameCluster = nodeCluster.getChildText("nameNativeCluster");
                    if ("Нажмите здесь, если не относится".equals(nameCluster)) {
                        fillOtherCluster = true;
                        otherClusterNativeSubstantiation = nodeCluster.getChildText("nativeSubstantiationClusterProject");
                        otherClusterForeignSubstantiation = nodeCluster.getChildText("foreignSubstantiationClusterProject");
                    } else {
                        if (clusterInfoList.containsKey(nameCluster)) {
                            clusterInfoList.put(nameCluster, new ClusterInfo(projectId, true, nameCluster,
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
                        rowCluster.add(nodeCluster.nativeSubstantiationProjectCluster);
                    dataCluster.add(rowCluster);
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
                    rowPatent.add(nodePatent.getChildText("ownerPatent"));
                    rowPatent.add(nodePatent.getChildText("ownerTypePatent"));
                    rowPatent.add(BaseUtils.nullZero(node.getChildText("isValuatedPatent")));
                    rowPatent.add(nodePatent.getChildText("valuatorPatent"));
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
                        nativeInnovativeProjectField, nativeSubstantiationProjectTypeField, fileNativeSummaryProjectField, fileNativeTechnicalDescriptionProjectField);

                List<ImportField> fieldsFullClaimerNative = BaseUtils.toList(nameNativeClaimerField, firmNameNativeClaimerField);

                List<ImportField> fieldsForeign = BaseUtils.toList(
                        nameForeignProjectField, nameForeignManagerProjectField, foreignProblemProjectField,
                        foreignInnovativeProjectField, foreignSubstantiationProjectTypeField, fileForeignSummaryProjectField, fileForeignTechnicalDescriptionProjectField);

                List<ImportField> fieldsFullClaimerForeign = BaseUtils.toList(nameForeignClaimerField, firmNameForeignClaimerField);

                List<ImportField> fieldsBoth = BaseUtils.toList(
                        fillNativeProjectField, fillForeignProjectField, projectIdField,
                        isOwnedEquipmentProjectField, isAvailableEquipmentProjectField, isTransferEquipmentProjectField,
                        descriptionTransferEquipmentProjectField, ownerEquipmentProjectField, isPlanningEquipmentProjectField,
                        specificationEquipmentProjectField, isSeekEquipmentProjectField, descriptionEquipmentProjectField,
                        isOtherEquipmentProjectField, commentEquipmentProjectField, isReturnInvestmentsProjectField,
                        nameReturnInvestorProjectField, amountReturnFundsProjectField, isNonReturnInvestmentsProjectField,
                        nameNonReturnInvestorProjectField, amountNonReturnFundsProjectField, commentOtherNonReturnInvestmentsProjectField,
                        isCapitalInvestmentProjectField, isPropertyInvestmentProjectField, isGrantsProjectField,
                        isOtherNonReturnInvestmentsProjectField, isOwnFundsProjectField, amountOwnFundsProjectField,
                        isPlanningSearchSourceProjectField, amountFundsProjectField, isOtherSoursesProjectField,
                        commentOtherSoursesProjectField, updateDateProjectField, projectTypeProjectField,
                        projectActionProjectField, emailClaimerField);

                List<ImportField> fieldsFullClaimerBoth = BaseUtils.toList(phoneClaimerField, addressClaimerField,
                        siteClaimerField, emailFirmClaimerField,
                        OGRNClaimerField, INNClaimerField, fileStatementClaimerField,
                        fileConstituentClaimerField, fileExtractClaimerField, fileRoadMapProjectField);

                if (fillClaimer) {
                    fieldsNative.addAll(fieldsFullClaimerNative);
                    fieldsForeign.addAll(fieldsFullClaimerForeign);
                    fieldsBoth.addAll(fieldsFullClaimerBoth);
                    propertiesNative.addAll(propertiesFullClaimerNative);
                    propertiesForeign.addAll(propertiesFullClaimerForeign);
                    properties.addAll(propertiesFullClaimer);
                }

                if (fillDate) {
                    properties.add(propertyDate);
                    fieldsBoth.add(dateProjectField);
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
                importMultilanguageData(
                        fieldsBoth, fieldsNative, fieldsForeign,
                        properties, propertiesNative, propertiesForeign,
                        data, keysArray, fillNative, fillForeign);

                List<ImportField> fieldsCurrentClusterBoth = BaseUtils.toList(
                        projectIdField, inProjectClusterField, nameNativeClusterField);
                List<ImportField> fieldsCurrentClusterNative = BaseUtils.toList(
                        nativeSubstantiationProjectClusterField);
                List<ImportField> fieldsCurrentClusterForeign = BaseUtils.toList(
                        foreignSubstantiationProjectClusterField);

                keysArray = new ImportKey<?>[]{clusterKey, projectKey};
                importMultilanguageData(
                        fieldsCurrentClusterBoth, fieldsCurrentClusterNative, fieldsCurrentClusterForeign,
                        propertiesCluster, propertiesClusterNative, propertiesClusterForeign,
                        dataCluster, keysArray, fillNative, fillForeign);

                List<ImportField> fieldsPatentBoth = BaseUtils.toList(
                        projectIdField, nativeNumberPatentField, datePatentField,
                        isOwnedPatentField, ownerPatentField, ownerTypePatentField,
                        isValuatedPatentField, valuatorPatentField, fileIntentionOwnerPatentField,
                        fileActValuationPatentField);
                List<ImportField> fieldsPatentNative = BaseUtils.toList(nativeTypePatentField);
                List<ImportField> fieldsPatentForeign = BaseUtils.toList(foreignTypePatentField, foreignNumberPatentField);
                keysArray = new ImportKey<?>[]{projectKey, patentKey, ownerTypePatentKey};
                importMultilanguageData(
                        fieldsPatentBoth, fieldsPatentNative, fieldsPatentForeign,
                        propertiesPatent, propertiesPatentNative, propertiesPatentForeign,
                        dataPatent, keysArray, fillNative, fillForeign);

                List<ImportField> fieldsAcademic = BaseUtils.toList(
                        projectIdField,
                        fullNameAcademicField, institutionAcademicField, titleAcademicField,
                        fileDocumentConfirmingAcademicField, fileDocumentEmploymentAcademicField
                );
                ImportTable table = new ImportTable(fieldsAcademic, dataAcademic);
                keysArray = new ImportKey<?>[]{projectKey, academicKey};
                new IntegrationService(session, table, Arrays.asList(keysArray), propertiesAcademic).synchronize(true, true, false, true);

                List<ImportField> fieldsNonRussianSpecialist = BaseUtils.toList(
                        projectIdField,
                        fullNameNonRussianSpecialistField, organizationNonRussianSpecialistField, titleNonRussianSpecialistField
                        , fileNativeResumeNonRussianSpecialistField, fileForeignResumeNonRussianSpecialistField,
                        filePassportNonRussianSpecialistField, fileStatementNonRussianSpecialistField
                );
                table = new ImportTable(fieldsNonRussianSpecialist, dataNonRussianSpecialist);
                keysArray = new ImportKey<?>[]{projectKey, nonRussianSpecialistKey};
                new IntegrationService(session, table, Arrays.asList(keysArray), propertiesNonRussianSpecialist).synchronize(true, true, false, true);
            }
        } catch (JDOMParseException e) {
            String info = "failed to import project " + projectId + ". Reason: " + new String(responseContents);
//            String info = e.getCause() + " : projectId=" + projectId + " : " + new String(responseContents);
            toLog += info;
            logger.info(info);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String sessionApply = session.apply(BL);
        if (sessionApply != null) {
            String info = "failed to import project " + projectId + ". Constraint: " + sessionApply;
            toLog += info;
            logger.error(info);
            session.restart(true);
        } else
            logger.info(projectId + " project was imported successfully");
    }

    private void importMultilanguageData(
            List<ImportField> fieldsBoth, List<ImportField> fieldsNative, List<ImportField> fieldsForeign,
            List<ImportProperty<?>> propertiesBoth, List<ImportProperty<?>> propertiesNative, List<ImportProperty<?>> propertiesForeign,
            List<List<Object>> data, ImportKey<?>[] keysArray, boolean fillNative, boolean fillForeign) throws SQLException {

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
            new IntegrationService(session, new ImportTable(fieldsMerged, data), Arrays.asList(keysArray), propertiesMerged).synchronize(true, true, false, true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public class ClusterInfo {
        public String projectID;
        public Object inProjectCluster;
        public String nameNativeCluster;
        public String nativeSubstantiationProjectCluster;
        public String foreignSubstantiationProjectCluster;

        public ClusterInfo(String nameNativeCluster) {
            this(null, null, nameNativeCluster, null, null);
        }

        public ClusterInfo(String projectID, Object inProjectCluster, String nameNativeCluster,
                           String nativeSubstantiationProjectCluster, String foreignSubstantiationProjectCluster) {
            this.projectID = projectID;
            this.inProjectCluster = inProjectCluster;
            this.nameNativeCluster = nameNativeCluster;
            this.nativeSubstantiationProjectCluster = nativeSubstantiationProjectCluster;
            this.foreignSubstantiationProjectCluster = foreignSubstantiationProjectCluster;
        }

    }
}
