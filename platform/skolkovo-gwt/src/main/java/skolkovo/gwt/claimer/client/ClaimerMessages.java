package skolkovo.gwt.claimer.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface ClaimerMessages extends Messages {

    String title();

    String projectInformation();

    String projectName();

    String maximumValueLength(int count);

    String projectManagerName();

    String projectManagerHint();

    String justification();

    String innovation();

    String innovationProblem();

    String innovationProblemHint();

    String innovationDescription();

    String innovationDescriptionHint();

    String projectTechnology();

    String projectArea();

    String projectTechComparable();

    String projectTechSurpass();

    String projectTechDemanded();

    String projectTechAdvantages();

    String projectTechOutperforms();

    String projectTechNoBenchmarks();

    String projectTechDescription();

    String projectAreaEnergy();

    String projectAreaNuclear();

    String projectAreaSpace();

    String projectAreaMedical();

    String projectAreaIT();

    String projectAreaNone();

    String executiveSummary();

    String executiveSummaryFile();

    String executiveSummaryHint();

    String availableFileFormatsPDF();

    String maximumFileSize4Mb();

    String fundingSources();

    String fundingReturns();

    String fundingInvestorName();

    String fundingAmount();

    String fundingNonReturns();

    String fundingCapital();

    String fundingProperty();

    String fundingGrants();

    String fundingOwn();

    String fundingOwnAmount();

    String fundingSearch();

    String fundingSearchAmount();

    String fundingOther();

    String comment();

    String equipment();

    String equipmentHint();

    String equipmentOwned();

    String equipmentBuy();

    String equipmentAgreement();

    String equipmentOwnerName();

    String equipmentDescribe();

    String equipmentSpecify();

    String equipmentSkolkovo();

    String equipmentSeek();

    String equipmentOther();

    String patents();

    String patentsHint();

    String patentsType();

    String patentsExample();

    String patentsNumber();

    String patentsPriorityDate();

    String patentsPriorityDateHint();

    String patentsHasnotRights();

    String patentsValuated();

    String documents();

    String documentsRoadmap();

    String documentsRoadmapHint(String link);

    String documentsTemplate();

    String documentsDescription();

    String documentsPreliminarySertificate();

    String documentsPreliminaryHint();

    public static class Instance {
        private static final ClaimerMessages instance = (ClaimerMessages) GWT.create(ClaimerMessages.class);

        public static ClaimerMessages get() {
            return instance;
        }
    }
}
