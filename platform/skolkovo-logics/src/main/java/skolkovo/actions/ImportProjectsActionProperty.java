package skolkovo.actions;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import skolkovo.SkolkovoLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.*;

public class ImportProjectsActionProperty extends ActionProperty {

    private SkolkovoLogicsModule LM;

    public ImportProjectsActionProperty(SkolkovoLogicsModule LM) {
        super(LM.genSID(), "Импортировать проекты", new ValueClass[]{});
        this.LM = LM;
    }

        protected FileActionClass valueClass = FileActionClass.getDefinedInstance(true, "Файлы c данными (*.xml)", "xml");

        protected ImportField dateProjectField,
                projectIdField, nameNativeProjectField, nameForeignProjectField,
                nameNativeManagerProjectField, nameNativeGenitiveManagerProjectField, nameNativeDativusManagerProjectField, nameNativeAblateManagerProjectField,
                nameForeignManagerProjectField,
                nativeProblemProjectField, foreignProblemProjectField, nativeInnovativeProjectField, foreignInnovativeProjectField,
                projectTypeProjectField,
                nativeSubstantiationProjectTypeField, foreignSubstantiationProjectTypeField, nativeSubstantiationClusterProjectField, foreignSubstantiationClusterProjectField,
                isOwnedEquipmentProjectField, isAvailableEquipmentProjectField,
                isTransferEquipmentProjectField, descriptionTransferEquipmentProjectField, ownerEquipmentProjectField,
                isPlanningEquipmentProjectField, specificationEquipmentProjectField,
                isSeekEquipmentProjectField, descriptionEquipmentProjectField,
                isOtherEquipmentProjectField, commentEquipmentProjectField,

                isReturnInvestmentsProjectField, nameReturnInvestorProjectField, amountReturnFundsProjectField,
                isNonReturnInvestmentsProjectField, isCapitalInvestmentProjectField, isPropertyInvestmentProjectField, isGrantsProjectField, isOtherNonReturnInvestmentsProjectField,
                nameNonReturnInvestorProjectField, amountNonReturnFundsProjectField, commentOtherNonReturnInvestmentsProjectField,
                isOwnFundsProjectField, amountOwnFundsProjectField,
                isPlanningSearchSourceProjectField, amountFundsProjectField,
                isOtherSoursesProjectField, commentOtherSoursesProjectField,

    nameNativeClusterField, inProjectClusterField, numberCurrentClusterField,
                nameNativeClaimerField, nameForeignClaimerField,
                firmNameNativeClaimerField, firmNameForeignClaimerField, phoneClaimerField, addressClaimerField, siteClaimerField,
                emailClaimerField, OGRNClaimerField, INNClaimerField,
                //fileStatementClaimerField, fileConstituentClaimerField, fileExtractClaimerField,
                //fileNativeSummaryProjectField, fileForeignSummaryProjectField, fileRoadMapProjectField, fileResolutionIPProjectField,
                nativeTypePatentField, foreignTypePatentField, nativeNumberPatentField, foreignNumberPatentField,
                datePatentField, isOwnedPatentField, ownerPatentField, ownerTypePatentField, isValuatedPatentField, valuatorPatentField,
                //fileIntentionOwnerPatentField, fileActValuationPatentField,
                fullNameAcademicField, institutionAcademicField, titleAcademicField,
                //fileDocumentConfirmingAcademicField, fileDocumentEmploymentAcademicField,
                fullNameNonRussianSpecialistField, organizationNonRussianSpecialistField, titleNonRussianSpecialistField
                //,fileForeignResumeNonRussianSpecialistField, fileNativeResumeNonRussianSpecialistField,
                // filePassportNonRussianSpecialistField, fileStatementNonRussianSpecialistField
                ;

        //protected final ClassPropertyInterface projectInterface;

        //public ImportProjectsActionProperty() {
        //    super(LM.genSID(), "Импортировать проекты", new ValueClass[]{});
        //
        //    Iterator<ClassPropertyInterface> i = interfaces.iterator();
        //    projectInterface = i.next();
        //}

        private void initFields() {
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
            nativeSubstantiationProjectTypeField = new ImportField(LM.nativeSubstantiationProjectType);
            foreignSubstantiationProjectTypeField = new ImportField(LM.foreignSubstantiationProjectType);
            nativeSubstantiationClusterProjectField = new ImportField(LM.nativeSubstantiationClusterProject);
            foreignSubstantiationClusterProjectField = new ImportField(LM.foreignSubstantiationClusterProject);
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

            nameNativeClusterField = new ImportField(LM.nameNativeCluster);
            inProjectClusterField =  new ImportField(LM.inProjectCluster);
            numberCurrentClusterField = new ImportField(LM.numberCluster);

            nameNativeClaimerField = new ImportField(LM.nameNativeClaimer);
            nameForeignClaimerField = new ImportField(LM.nameForeignClaimer);
            firmNameNativeClaimerField = new ImportField(LM.firmNameNativeClaimer);
            firmNameForeignClaimerField = new ImportField(LM.firmNameForeignClaimer);
            phoneClaimerField = new ImportField(LM.phoneClaimer);
            addressClaimerField = new ImportField(LM.addressClaimer);
            siteClaimerField = new ImportField(LM.siteClaimer);
            emailClaimerField = new ImportField(LM.emailClaimer);
            OGRNClaimerField = new ImportField(LM.OGRNClaimer);
            INNClaimerField = new ImportField(LM.INNClaimer);
            //fileStatementClaimerField = new ImportField(statementClaimer);
            //fileConstituentClaimerField = new ImportField(constituentClaimer);
            //fileExtractClaimerField = new ImportField(extractClaimer);
            //fileNativeSummaryProjectField = new ImportField(fileNativeSummaryProject);
            //fileForeignSummaryProjectField = new ImportField(fileForeignSummaryProject);
            //fileRoadMapProjectField = new ImportField(fileRoadMapProject);
            //fileResolutionIPProjectField = new ImportField(fileResolutionIPProject);
            //fileNativeTechnicalDescriptionProjectField = new ImportField(fileNativeTechnicalDescriptionProject);
            //fileForeignTechnicalDescriptionProjectField = new ImportField(fileForeignTechnicalDescriptionProject);

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

            //fileIntentionOwnerPatentField = new ImportField(fileIntentionOwnerPatent);
            //fileActValuationPatentField = new ImportField(fileActValuationPatent);

            fullNameAcademicField = new ImportField(LM.fullNameAcademic);
            institutionAcademicField = new ImportField(LM.institutionAcademic);
            titleAcademicField = new ImportField(LM.titleAcademic);
            //fileDocumentConfirmingAcademicField = new ImportField(fileDocumentConfirmingAcademic);
            //fileDocumentEmploymentAcademicField = new ImportField(fileDocumentEmploymentAcademic);


            fullNameNonRussianSpecialistField = new ImportField(LM.fullNameNonRussianSpecialist);
            organizationNonRussianSpecialistField = new ImportField(LM.organizationNonRussianSpecialist);
            titleNonRussianSpecialistField = new ImportField(LM.titleNonRussianSpecialist);
            //fileForeignResumeNonRussianSpecialistField = new ImportField(fileForeignResumeNonRussianSpecialist);
            //fileNativeResumeNonRussianSpecialistField = new ImportField(fileNativeResumeNonRussianSpecialist);
            //filePassportNonRussianSpecialistField = new ImportField(filePassportNonRussianSpecialist);
            //fileStatementNonRussianSpecialistField = new ImportField(fileStatementNonRussianSpecialist);
        }

        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
            throw new RuntimeException("no need");
        }

        @Override
        public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
            initFields();

            List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> propertiesCluster = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> propertiesPatent = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> propertiesAcademic = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> propertiesNonRussianSpecialist = new ArrayList<ImportProperty<?>>();

            ImportKey<?> projectKey = new ImportKey(LM.project, LM.sidToProject.getMapping(projectIdField));
            properties.add(new ImportProperty(dateProjectField, LM.dateProject.getMapping(projectKey)));
            properties.add(new ImportProperty(projectIdField, LM.sidProject.getMapping(projectKey)));
            properties.add(new ImportProperty(nameNativeProjectField, LM.nameNativeProject.getMapping(projectKey)));
            properties.add(new ImportProperty(nameForeignProjectField, LM.nameForeignProject.getMapping(projectKey)));
            properties.add(new ImportProperty(nameNativeManagerProjectField, LM.nameNativeManagerProject.getMapping(projectKey)));
            properties.add(new ImportProperty(nameNativeGenitiveManagerProjectField, LM.nameNativeGenitiveManagerProject.getMapping(projectKey)));
            properties.add(new ImportProperty(nameNativeDativusManagerProjectField, LM.nameNativeDativusManagerProject.getMapping(projectKey)));
            properties.add(new ImportProperty(nameNativeAblateManagerProjectField, LM.nameNativeAblateManagerProject.getMapping(projectKey)));
            properties.add(new ImportProperty(nameForeignManagerProjectField, LM.nameForeignManagerProject.getMapping(projectKey)));
            properties.add(new ImportProperty(nativeProblemProjectField, LM.nativeProblemProject.getMapping(projectKey)));
            properties.add(new ImportProperty(foreignProblemProjectField, LM.foreignProblemProject.getMapping(projectKey)));
            properties.add(new ImportProperty(nativeInnovativeProjectField, LM.nativeInnovativeProject.getMapping(projectKey)));
            properties.add(new ImportProperty(foreignInnovativeProjectField, LM.foreignInnovativeProject.getMapping(projectKey)));
            properties.add(new ImportProperty(nativeSubstantiationProjectTypeField, LM.nativeSubstantiationProjectType.getMapping(projectKey)));
            properties.add(new ImportProperty(foreignSubstantiationProjectTypeField, LM.foreignSubstantiationProjectType.getMapping(projectKey)));
            properties.add(new ImportProperty(nativeSubstantiationClusterProjectField, LM.nativeSubstantiationClusterProject.getMapping(projectKey)));
            properties.add(new ImportProperty(foreignSubstantiationClusterProjectField, LM.foreignSubstantiationClusterProject.getMapping(projectKey)));

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

            ImportKey<?> projectTypeProjectKey = new ImportKey(LM.projectType, LM.projectTypeToSID.getMapping(projectTypeProjectField));
            properties.add(new ImportProperty(projectTypeProjectField, LM.projectTypeProject.getMapping(projectKey),
                    LM.baseLM.object(LM.projectType).getMapping(projectTypeProjectKey)));

            ImportKey<?> claimerKey = new ImportKey(LM.claimer, LM.nameNativeToClaimer.getMapping(nameNativeClaimerField));
            properties.add(new ImportProperty(nameNativeClaimerField, LM.claimerProject.getMapping(projectKey),
                    LM.baseLM.object(LM.claimer).getMapping(claimerKey)));
            properties.add(new ImportProperty(nameNativeClaimerField, LM.nameNativeClaimer.getMapping(claimerKey)));
            properties.add(new ImportProperty(nameForeignClaimerField, LM.nameForeignClaimer.getMapping(claimerKey)));
            properties.add(new ImportProperty(firmNameNativeClaimerField, LM.firmNameNativeClaimer.getMapping(claimerKey)));
            properties.add(new ImportProperty(firmNameForeignClaimerField, LM.firmNameForeignClaimer.getMapping(claimerKey)));
            properties.add(new ImportProperty(phoneClaimerField, LM.phoneClaimer.getMapping(claimerKey)));
            properties.add(new ImportProperty(addressClaimerField, LM.addressClaimer.getMapping(claimerKey)));
            properties.add(new ImportProperty(siteClaimerField, LM.siteClaimer.getMapping(claimerKey)));
            properties.add(new ImportProperty(emailClaimerField, LM.emailClaimer.getMapping(claimerKey)));
            properties.add(new ImportProperty(OGRNClaimerField, LM.OGRNClaimer.getMapping(claimerKey)));
            properties.add(new ImportProperty(INNClaimerField, LM.INNClaimer.getMapping(claimerKey)));

            //properties.add(new ImportProperty(fileStatementClaimerField, statementClaimer.getMapping(claimerKey)));
            //properties.add(new ImportProperty(fileConstituentClaimerField, constituentClaimer.getMapping(claimerKey)));
            //properties.add(new ImportProperty(fileExtractClaimerField, extractClaimer.getMapping(claimerKey)));
            //properties.add(new ImportProperty(fileNativeSummaryProjectField, fileNativeSummaryProject.getMapping(projectKey)));
            //properties.add(new ImportProperty(fileForeignSummaryProjectField, fileForeignSummaryProject.getMapping(projectKey)));
            //properties.add(new ImportProperty(fileRoadMapProjectField, fileRoadMapProject.getMapping(projectKey)));
            //properties.add(new ImportProperty(fileResolutionIPProjectField, fileResolutionIPProject.getMapping(projectKey)));
            //properties.add(new ImportProperty(fileNativeTechnicalDescriptionProjectField, fileNativeTechnicalDescriptionProject.getMapping(projectKey)));
            //properties.add(new ImportProperty(fileForeignTechnicalDescriptionProjectField, fileForeignTechnicalDescriptionProject.getMapping(projectKey)));



            ImportKey<?> patentKey = new ImportKey(LM.patent, LM.nativeNumberToPatent.getMapping(nativeNumberPatentField));
            propertiesPatent.add(new ImportProperty(nativeTypePatentField, LM.nativeTypePatent.getMapping(patentKey)));
            propertiesPatent.add(new ImportProperty(foreignTypePatentField, LM.foreignTypePatent.getMapping(patentKey)));
            propertiesPatent.add(new ImportProperty(nativeNumberPatentField, LM.nativeNumberPatent.getMapping(patentKey)));
            propertiesPatent.add(new ImportProperty(foreignNumberPatentField, LM.foreignNumberPatent.getMapping(patentKey)));
            propertiesPatent.add(new ImportProperty(datePatentField, LM.priorityDatePatent.getMapping(patentKey)));
            propertiesPatent.add(new ImportProperty(isOwnedPatentField, LM.isOwned.getMapping(patentKey)));
            propertiesPatent.add(new ImportProperty(ownerPatentField, LM.ownerPatent.getMapping(patentKey)));
            propertiesPatent.add(new ImportProperty(isValuatedPatentField, LM.isValuated.getMapping(patentKey)));
            propertiesPatent.add(new ImportProperty(valuatorPatentField, LM.valuatorPatent.getMapping(patentKey)));
            //propertiesPatent.add(new ImportProperty(fileIntentionOwnerPatentField, fileIntentionOwnerPatent.getMapping(patentKey)));
            //propertiesPatent.add(new ImportProperty(fileActValuationPatentField, fileActValuationPatent.getMapping(patentKey)));

            ImportKey<?> projectPatentKey = new ImportKey(LM.patent, LM.sidToProject.getMapping(projectIdField));
            propertiesPatent.add(new ImportProperty(projectIdField, LM.projectPatent.getMapping(patentKey),
                    LM.baseLM.object(LM.project).getMapping(projectPatentKey)));

            ImportKey<?> ownerTypePatentKey = new ImportKey(LM.ownerType, LM.ownerTypeToSID.getMapping(ownerTypePatentField));
            propertiesPatent.add(new ImportProperty(ownerTypePatentField, LM.ownerTypePatent.getMapping(patentKey),
                    LM.baseLM.object(LM.ownerType).getMapping(ownerTypePatentKey)));

            ImportKey<?> clusterKey = new ImportKey(LM.cluster, LM.nameNativeToCluster.getMapping(nameNativeClusterField));
            propertiesCluster.add(new ImportProperty(inProjectClusterField, LM.inProjectCluster.getMapping(projectKey , clusterKey)));
            propertiesCluster.add(new ImportProperty(nameNativeClusterField, LM.nameNativeCluster.getMapping(clusterKey)));

            ImportKey<?> academicKey = new ImportKey(LM.academic, LM.fullNameToAcademic.getMapping(fullNameAcademicField));
            propertiesAcademic.add(new ImportProperty(fullNameAcademicField, LM.fullNameAcademic.getMapping(academicKey)));
            propertiesAcademic.add(new ImportProperty(institutionAcademicField, LM.institutionAcademic.getMapping(academicKey)));
            propertiesAcademic.add(new ImportProperty(titleAcademicField, LM.titleAcademic.getMapping(academicKey)));
            //propertiesAcademic.add(new ImportProperty(fileDocumentConfirmingAcademicField, fileDocumentConfirmingAcademic.getMapping(academicKey)));
            //propertiesAcademic.add(new ImportProperty(fileDocumentEmploymentAcademicField, fileDocumentEmploymentAcademic.getMapping(academicKey)));

            ImportKey<?> projectAcademicKey = new ImportKey(LM.academic, LM.sidToProject.getMapping(projectIdField));
            propertiesAcademic.add(new ImportProperty(projectIdField, LM.projectAcademic.getMapping(academicKey),
                    LM.baseLM.object(LM.project).getMapping(projectAcademicKey)));

            ImportKey<?> nonRussianSpecialistKey = new ImportKey(LM.nonRussianSpecialist, LM.fullNameToNonRussianSpecialist.getMapping(fullNameNonRussianSpecialistField));
            propertiesNonRussianSpecialist.add(new ImportProperty(fullNameNonRussianSpecialistField, LM.fullNameNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
            propertiesNonRussianSpecialist.add(new ImportProperty(organizationNonRussianSpecialistField, LM.organizationNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
            propertiesNonRussianSpecialist.add(new ImportProperty(titleNonRussianSpecialistField, LM.titleNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
            //propertiesNonRussianSpecialist.add(new ImportProperty(fileForeignResumeNonRussianSpecialistField, fileForeignResumeNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
            //propertiesNonRussianSpecialist.add(new ImportProperty(fileNativeResumeNonRussianSpecialistField, fileNativeResumeNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
            //propertiesNonRussianSpecialist.add(new ImportProperty(filePassportNonRussianSpecialistField, filePassportNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));
            //propertiesNonRussianSpecialist.add(new ImportProperty(fileStatementNonRussianSpecialistField, fileStatementNonRussianSpecialist.getMapping(nonRussianSpecialistKey)));

            ImportKey<?> projectNonRussianSpecialistKey = new ImportKey(LM.nonRussianSpecialist, LM.sidToProject.getMapping(projectIdField));
            propertiesNonRussianSpecialist.add(new ImportProperty(projectIdField, LM.projectNonRussianSpecialist.getMapping(nonRussianSpecialistKey),
                    LM.baseLM.object(LM.project).getMapping(projectNonRussianSpecialistKey)));

            List<List<Object>> data = new ArrayList<List<Object>>();
            List<List<Object>> dataCluster = new ArrayList<List<Object>>();
            List<List<Object>> dataPatent = new ArrayList<List<Object>>();
            List<List<Object>> dataAcademic = new ArrayList<List<Object>>();
            List<List<Object>> dataNonRussianSpecialist = new ArrayList<List<Object>>();
            List<Object> row;

            List<byte[]> fileList = valueClass.getFiles(value.getValue());

            for (byte[] file : fileList) {

                ByteArrayInputStream xmlFile = new ByteArrayInputStream(file);
                SAXBuilder builder = new SAXBuilder();

                try {
                    Document document = (Document) builder.build(xmlFile);
                    Element rootNode = document.getRootElement();
                    List list = rootNode.getChildren("project");

                    for (int i = 0; i < list.size(); i++) {
                        Element node = (Element) list.get(i);
                        row = new ArrayList<Object>();
                        row.add(new Date(Integer.parseInt(node.getChildText("yearProject")) - 1900, Integer.parseInt(node.getChildText("monthProject")), Integer.parseInt(node.getChildText("dayProject")) - 1));
                        row.add(node.getChildText("projectId"));
                        row.add(node.getChildText("nameNativeProject"));
                        row.add(node.getChildText("nameForeignProject"));
                        row.add(node.getChildText("nameNativeManagerProject"));
                        row.add(node.getChildText("nameNativeGenitiveManagerProject"));
                        row.add(node.getChildText("nameNativeDativusManagerProject"));
                        row.add(node.getChildText("nameNativeAblateManagerProject"));
                        row.add(node.getChildText("nameForeignManagerProject"));
                        row.add(node.getChildText("nativeProblemProject"));
                        row.add(node.getChildText("foreignProblemProject"));
                        row.add(node.getChildText("nativeInnovativeProject"));
                        row.add(node.getChildText("foreignInnovativeProject"));
                        row.add(node.getChildText("nativeSubstantiationProjectType"));
                        row.add(node.getChildText("foreignSubstantiationProjectType"));
                        row.add(node.getChildText("nativeSubstantiationClusterProject"));
                        row.add(node.getChildText("foreignSubstantiationClusterProject"));

                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isOwnedEquipmentProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isAvailableEquipmentProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isTransferEquipmentProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        row.add(node.getChildText("descriptionTransferEquipmentProject"));
                        row.add(node.getChildText("ownerEquipmentProject"));
                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isPlanningEquipmentProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        row.add(node.getChildText("specificationEquipmentProject"));
                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isSeekEquipmentProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        row.add(node.getChildText("descriptionEquipmentProject"));
                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isOtherEquipmentProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        row.add(node.getChildText("commentEquipmentProject"));

                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isReturnInvestmentsProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        row.add(node.getChildText("nameReturnInvestorProject"));
                        row.add(Double.parseDouble(BaseUtils.nvl(node.getChildText("amountReturnFundsProject"), "0")));

                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isNonReturnInvestmentsProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        row.add(node.getChildText("nameNonReturnInvestorProject"));
                        row.add(Double.parseDouble(BaseUtils.nvl(node.getChildText("amountNonReturnFundsProject"), "0")));
                        row.add(node.getChildText("commentOtherNonReturnInvestmentsProject"));

                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isCapitalInvestmentProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isPropertyInvestmentProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isGrantsProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isOtherNonReturnInvestmentsProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);

                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isOwnFundsProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        row.add(Double.parseDouble(BaseUtils.nvl(node.getChildText("amountOwnFundsProject"), "0")));
                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isPlanningSearchSourceProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        row.add(Double.parseDouble(BaseUtils.nvl(node.getChildText("amountFundsProject"), "0")));
                        if (Integer.parseInt(BaseUtils.nvl(node.getChildText("isOtherSoursesProject"), "0")) == 1)
                            row.add(true);
                        else row.add(null);
                        row.add(node.getChildText("commentOtherSoursesProject"));

                        row.add(node.getChildText("typeProject"));
                        row.add(node.getChildText("nameNativeClaimer"));
                        row.add(node.getChildText("nameForeignClaimer"));

                        row.add(node.getChildText("firmNameNativeClaimer"));
                        row.add(node.getChildText("firmNameForeignClaimer"));
                        row.add(node.getChildText("phoneClaimer"));
                        row.add(node.getChildText("addressClaimer"));
                        row.add(node.getChildText("siteClaimer"));
                        row.add(node.getChildText("emailClaimer"));
                        row.add(node.getChildText("OGRNClaimer"));
                        row.add(node.getChildText("INNClaimer"));

                        ////byte[] fileNormal = Base64.decodeBase64(node.getChildText("fileStatementClaimerField"));
                        ////FileWriter fw = new FileWriter("C:\\test\\resume.pdf");
                        ////for (byte b : fileNormal) {
                        ////    fw.write(b);
                        ////}
                        ////fw.close();
                        ////FileInputStream file = new FileInputStream("C:\\test\\resume.pdf");
                        ////byte[] fileBytes = IOUtils.readBytesFromStream(file);
                        ////row.add(fileBytes);

                        //byte[] fileBytes = Base64.decodeBase64(node.getChildText("fileStatementClaimerField"));
                        //row.add(fileBytes);
                        //fileBytes = Base64.decodeBase64(node.getChildText("fileConstituentClaimerField"));
                        //row.add(fileBytes);
                        //fileBytes = Base64.decodeBase64(node.getChildText("fileExtractClaimerField"));
                        //row.add(fileBytes);
                        //fileBytes = Base64.decodeBase64(node.getChildText("fileNativeSummaryProject"));
                        //row.add(fileBytes);
                        //fileBytes = Base64.decodeBase64(node.getChildText("fileForeignSummaryProject"));
                        //row.add(fileBytes);
                        //fileBytes = Base64.decodeBase64(node.getChildText("fileRoadMapProject"));
                        //row.add(fileBytes);
                        //fileBytes = Base64.decodeBase64(node.getChildText("fileResolutionIPProject"));
                        //row.add(fileBytes);
                        //fileBytes = Base64.decodeBase64(node.getChildText("fileNativeTechnicalDescriptionProject"));
                        //row.add(fileBytes);
                        //fileBytes = Base64.decodeBase64(node.getChildText("fileForeignTechnicalDescriptionProject"));
                        //row.add(fileBytes);

                        data.add(row);

                        //List listCluster = node.getChildren("cluster");
                        //for (int z = 0; z < listCluster.size(); z++) {
                        //    Element nodeCluster = (Element) listCluster.get(z);
                        //    List<Object> rowCluster = new ArrayList<Object>();
                        //
                        //
                        //    if (Integer.parseInt(BaseUtils.nvl(nodeCluster.getChildText("inProjectCluster"), "0")) == 1)
                        //        rowCluster.add(true);
                        //    else rowCluster.add(null);
                        //    rowCluster.add(nodeCluster.getChildText("nameNativeCurrentCluster"));
                        //    rowCluster.add(Integer.parseInt(nodeCluster.getChildText("numberCluster")));
                        //    rowCluster.add(node.getChildText("projectId"));
                        //    dataCluster.add(rowCluster);
                        //}

                        List listCluster = node.getChildren("cluster");
                        for (int z = 0; z < listCluster.size(); z++) {
                            Element nodeCluster = (Element) listCluster.get(z);
                            List<Object> rowCluster = new ArrayList<Object>();

                            rowCluster.add(true);

                            rowCluster.add(nodeCluster.getText());
                            rowCluster.add(node.getChildText("projectId"));
                            dataCluster.add(rowCluster);
                        }


                        int j;

                        List listPatent = node.getChildren("patent");
                        for (j = 0; j < listPatent.size(); j++) {
                            Element nodePatent = (Element) listPatent.get(j);
                            List<Object> rowPatent = new ArrayList<Object>();

                            rowPatent.add(node.getChildText("projectId"));
                            rowPatent.add(nodePatent.getChildText("nativeTypePatent"));
                            rowPatent.add(nodePatent.getChildText("foreignTypePatent"));
                            rowPatent.add(nodePatent.getChildText("nativeNumberPatent"));
                            rowPatent.add(nodePatent.getChildText("foreignNumberPatent"));
                            rowPatent.add(new Date(Integer.parseInt(nodePatent.getChildText("yearPatent")) - 1900, Integer.parseInt(nodePatent.getChildText("monthPatent")), Integer.parseInt(nodePatent.getChildText("dayPatent")) - 1));
                            if (Integer.parseInt(BaseUtils.nvl(nodePatent.getChildText("isOwnedPatent"), "0")) == 1)
                                rowPatent.add(true);
                            else rowPatent.add(null);
                            rowPatent.add(nodePatent.getChildText("ownerPatent"));
                            rowPatent.add(nodePatent.getChildText("ownerTypePatent"));
                            if (Integer.parseInt(BaseUtils.nvl(nodePatent.getChildText("isValuatedPatent"), "0")) == 1)
                                rowPatent.add(true);
                            else rowPatent.add(null);
                            rowPatent.add(nodePatent.getChildText("valuatorPatent"));
                            //byte[] fileBytes = Base64.decodeBase64(node.getChildText("fileIntentionOwnerPatent"));
                            //row.add(fileBytes);
                            //fileBytes = Base64.decodeBase64(node.getChildText("fileActValuationPatent"));
                            //row.add(fileBytes);

                            dataPatent.add(rowPatent);
                        }

                        List listAcademic = node.getChildren("academic");
                        for (j = 0; j < listAcademic.size(); j++) {
                            Element nodeAcademic = (Element) listAcademic.get(j);
                            List<Object> rowAcademic = new ArrayList<Object>();
                            rowAcademic.add(node.getChildText("projectId"));
                            rowAcademic.add(nodeAcademic.getChildText("fullNameAcademic"));
                            rowAcademic.add(nodeAcademic.getChildText("institutionAcademic"));
                            rowAcademic.add(nodeAcademic.getChildText("titleAcademic"));
                            //byte[] fileBytes = Base64.decodeBase64(node.getChildText("fileDocumentConfirmingAcademic"));
                            //row.add(fileBytes);
                            //fileBytes = Base64.decodeBase64(node.getChildText("fileDocumentEmploymentAcademic"));
                            //row.add(fileBytes);
                            dataAcademic.add(rowAcademic);
                        }

                        List listNonRussianSpecialist = node.getChildren("nonRussianSpecialist");
                        for (j = 0; j < listNonRussianSpecialist.size(); j++) {
                            Element nodeNonRussianSpecialist = (Element) listNonRussianSpecialist.get(j);
                            List<Object> rowNonRussianSpecialist = new ArrayList<Object>();
                            rowNonRussianSpecialist.add(node.getChildText("projectId"));
                            rowNonRussianSpecialist.add(nodeNonRussianSpecialist.getChildText("fullNameNonRussianSpecialist"));
                            rowNonRussianSpecialist.add(nodeNonRussianSpecialist.getChildText("organizationNonRussianSpecialist"));
                            rowNonRussianSpecialist.add(nodeNonRussianSpecialist.getChildText("titleNonRussianSpecialist"));
                            //byte[] fileBytes = Base64.decodeBase64(node.getChildText("fileForeignResumeNonRussianSpecialist"));
                            //row.add(fileBytes);
                            //fileBytes = Base64.decodeBase64(node.getChildText("fileNativeResumeNonRussianSpecialist"));
                            //row.add(fileBytes);
                            //fileBytes = Base64.decodeBase64(node.getChildText("filePassportNonRussianSpecialist"));
                            //row.add(fileBytes);
                            //fileBytes = Base64.decodeBase64(node.getChildText("fileStatementNonRussianSpecialist"));
                            //row.add(fileBytes);
                            dataNonRussianSpecialist.add(rowNonRussianSpecialist);
                        }

                        //creating test binary code
                        //FileInputStream inpdf = new FileInputStream("C:\\test\\test.pdf");
                        //byte[] fileBytes = IOUtils.readBytesFromStream(inpdf);
                        //byte[] file64 = Base64.encodeBase64(fileBytes);
                        //FileWriter fw = new FileWriter("C:\\test\\test.txt");
                        //for (byte b : file64) {
                        //    fw.write(b);
                        //}
                        //fw.close();

                    }
                } catch (IOException io) {
                    System.out.println(io.getMessage());
                } catch (JDOMException jdomex) {
                    System.out.println(jdomex.getMessage());
                }

                List<ImportField> fields = BaseUtils.toList(
                        dateProjectField, projectIdField, nameNativeProjectField, nameForeignProjectField,
                        nameNativeManagerProjectField, nameNativeGenitiveManagerProjectField, nameNativeDativusManagerProjectField, nameNativeAblateManagerProjectField,
                        nameForeignManagerProjectField,
                        nativeProblemProjectField, foreignProblemProjectField,
                        nativeInnovativeProjectField, foreignInnovativeProjectField,
                        nativeSubstantiationProjectTypeField, foreignSubstantiationProjectTypeField, nativeSubstantiationClusterProjectField, foreignSubstantiationClusterProjectField,
                        isOwnedEquipmentProjectField, isAvailableEquipmentProjectField,
                        isTransferEquipmentProjectField, descriptionTransferEquipmentProjectField, ownerEquipmentProjectField,
                        isPlanningEquipmentProjectField, specificationEquipmentProjectField,
                        isSeekEquipmentProjectField, descriptionEquipmentProjectField,
                        isOtherEquipmentProjectField, commentEquipmentProjectField,
                        isReturnInvestmentsProjectField, nameReturnInvestorProjectField, amountReturnFundsProjectField,
                        isNonReturnInvestmentsProjectField, nameNonReturnInvestorProjectField, amountNonReturnFundsProjectField, commentOtherNonReturnInvestmentsProjectField,
                        isCapitalInvestmentProjectField, isPropertyInvestmentProjectField, isGrantsProjectField, isOtherNonReturnInvestmentsProjectField,
                        isOwnFundsProjectField, amountOwnFundsProjectField,
                        isPlanningSearchSourceProjectField, amountFundsProjectField,
                        isOtherSoursesProjectField, commentOtherSoursesProjectField,
                        projectTypeProjectField,
                        nameNativeClaimerField, nameForeignClaimerField,
                        firmNameNativeClaimerField, firmNameForeignClaimerField, phoneClaimerField, addressClaimerField,
                        siteClaimerField, emailClaimerField, OGRNClaimerField, INNClaimerField
                        //, fileStatementClaimerField, fileConstituentClaimerField, fileExtractClaimerField,
                        // fileNativeSummaryProjectField, fileForeignSummaryProjectField, fileRoadMapProjectField, fileResolutionIPProjectField,
                        // fileNativeTechnicalDescriptionProjectField, fileForeignTechnicalDescriptionProjectField,
                );
                ImportTable table = new ImportTable(fields, data);

                ImportKey<?>[] keysArray;
                keysArray = new ImportKey<?>[]{projectKey, projectTypeProjectKey, claimerKey};
                new IntegrationService(session, table, Arrays.asList(keysArray), properties).synchronize(true, true, false);

                List<ImportField> fieldsCurrentCluster = BaseUtils.toList(

                        inProjectClusterField, nameNativeClusterField, projectIdField);
                table = new ImportTable(fieldsCurrentCluster, dataCluster);
                keysArray = new ImportKey<?>[]{clusterKey, projectKey };
                new IntegrationService(session, table, Arrays.asList(keysArray), propertiesCluster).synchronize(true, true, false);

                List<ImportField> fieldsPatent = BaseUtils.toList(
                        projectIdField,
                        nativeTypePatentField, foreignTypePatentField, nativeNumberPatentField, foreignNumberPatentField,
                        datePatentField, isOwnedPatentField, ownerPatentField, ownerTypePatentField,
                        isValuatedPatentField, valuatorPatentField
                        //,fileIntentionOwnerPatentField, fileActValuationPatentField
                );
                table = new ImportTable(fieldsPatent, dataPatent);
                keysArray = new ImportKey<?>[]{projectPatentKey, patentKey, ownerTypePatentKey};
                new IntegrationService(session, table, Arrays.asList(keysArray), propertiesPatent).synchronize(true, true, false);

                List<ImportField> fieldsAcademic = BaseUtils.toList(
                        projectIdField,
                        fullNameAcademicField, institutionAcademicField, titleAcademicField
                        //, fileDocumentConfirmingAcademicField, fileDocumentEmploymentAcademicField
                );
                table = new ImportTable(fieldsAcademic, dataAcademic);
                keysArray = new ImportKey<?>[]{projectAcademicKey, academicKey};
                new IntegrationService(session, table, Arrays.asList(keysArray), propertiesAcademic).synchronize(true, true, false);

                List<ImportField> fieldsNonRussianSpecialist = BaseUtils.toList(
                        projectIdField,
                        fullNameNonRussianSpecialistField, organizationNonRussianSpecialistField, titleNonRussianSpecialistField
                        //, fileForeignResumeNonRussianSpecialistField, fileNativeResumeNonRussianSpecialistField,
                        //filePassportNonRussianSpecialistField, fileStatementNonRussianSpecialistField
                );
                table = new ImportTable(fieldsNonRussianSpecialist, dataNonRussianSpecialist);
                keysArray = new ImportKey<?>[]{projectNonRussianSpecialistKey, nonRussianSpecialistKey};
                new IntegrationService(session, table, Arrays.asList(keysArray), propertiesNonRussianSpecialist).synchronize(true, true, false);
            }

            actions.add(new MessageClientAction("Данные были успешно приняты", "Импорт"));

        }

        @Override
        public DataClass getValueClass() {
            return valueClass;
        }
}
