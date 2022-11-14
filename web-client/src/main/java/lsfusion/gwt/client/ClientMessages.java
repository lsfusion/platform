package lsfusion.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface ClientMessages extends Messages {
    String yes();
    String no();
    String ok();
    String close();
    String cancel();
    String reset();
    String more();
    String logout();
    String closeAllTabs();

    String error();
    String internalServerError();
    String actionTimeoutError();
    String sessionTimeoutError();

    String choosingClass();

    String grid();
    String panel();
    String hide();

    String loading();
    
    String navigator();

    String formRendererEmpty();
    String formRendererNotDefined();
    String formRendererRequired();

    String formGridExport();

    String formGridTableView();
    String formGridPivotView();
    String formGridMapView();
    String formGridCustomView();
    String formGridCalendarView();

    String formGridManualUpdate();
    String formGridUpdate();

    String formTree();
    String formTreeExpand();
    String formTreeExpandCurrent();
    String formTreeCollapse();
    String formTreeCollapseCurrent();

    String formQueriesCalculateSum();
    String formQueriesSumResult();
    String formQueriesUnableToCalculateSum();
    String formQueriesNumberOfEntries();

    String formGridPreferences();
    String formGridPreferencesSaving();
    String formGridPreferencesResetting();
    String formGridPreferencesSave();
    String formGridPreferencesReset();
    String formGridPreferencesSaveSuccess();
    String formGridPreferencesResetSuccess();
    String formGridPreferencesNotSaved();
    String formGridPreferencesSavedForCurrentUser();
    String formGridPreferencesSavedForAllUsers();
    String formGridPreferencesSureToSave();
    String formGridPreferencesSureToReset();
    String formGridPreferencesForCurrentUser();
    String formGridPreferencesForAllUsers();
    String formGridPreferencesForAllUsersComplete();
    String formGridPreferencesDisplayedColumns();
    String formGridPreferencesPropertyInPanel();
    String formGridPreferencesPropertyNotShown();
    String formGridPreferencesHiddenColumns();
    String formGridPreferencesSelectedColumnSettings();
    String formGridPreferencesColumnCaption();
    String formGridPreferencesColumnPattern();
    String formGridPreferencesGridSettings();
    String formGridPreferencesHeaderHeight();
    String formGridPreferencesPageSize();
    String formGridPreferencesFont();
    String formGridPreferencesFontSize();
    String formGridPreferencesFontStyleBold();
    String formGridPreferencesFontStyleItalic();

    String formFilterCompareGreater();
    String formFilterCompareGreaterEquals();
    String formFilterCompareEquals();
    String formFilterCompareInArray();
    String formFilterCompareLess();
    String formFilterCompareLessEquals();
    String formFilterCompareContains();
    String formFilterCompareNot();
    String formFilterCompareNotEquals();
    String formFilterCompareSearch();
    String formFilterConditionViewAnd();
    String formFilterConditionViewOr();
    String formFilterConditionAllowNull();
    String formFilterAddCondition();
    String formFilterApply();
    String formFilterRemoveCondition();
    String formFilterResetConditions();
    String formFilterShowControls();
    String formFilterHideControls();

    String tooltipPath();

    String busyDialogBreak();
    String busyDialogCancelTransaction();
    String busyDialogCancelTransactionConfirm();
    String busyDialogCopyToClipboard();
    String busyDialogExit();
    String busyDialogInterruptTransaction();
    String busyDialogInterruptTransactionConfirm();
    String busyDialogLoading();
    String busyDialogReconnect();

    String uncaughtGWTException();
    String formGridSureToPasteMultivalue();

    String rmiConnectionLost();
    String rmiConnectionLostAuth();
    String rmiConnectionLostFatal();
    String rmiConnectionLostNonfatal();
    String rmiConnectionLostMessage(int attempt);
    String rmiConnectionLostMessageUnavailable(int attempt);
    String rmiConnectionLostExit();
    String rmiConnectionLostReconnect();

    String formGridPageSizeHit(int pagesize);
    String formGridPageSizeShowAll();

    String propertyTooltipCanonicalName();
    String propertyTooltipTable();
    String propertyTooltipObjects();
    String propertyTooltipSignature();
    String propertyTooltipScript();
    String propertyTooltipPath();
    String propertyTooltipFormPropertyName();
    String propertyTooltipFormPropertyDeclaration();
    String propertyTooltipHotkey();
    String showInEditor();
    String enterPath();
    String absolutePathToLsfusionDir();

    String propertyEmptyCaption();

    String typeDateTimeCaption();
    String typeZDateTimeCaption();
    String typeActionCaption();
    String typeColorCaption();
    String typeNamedFileCaption();
    String typeJSONCaption();
    String typeCustomDynamicFormatFileCaption();
    String typeCustomStaticFormatFileCaption();
    String typeDateCaption();
    String typeDoubleCaption();
    String typeIntegerCaption();
    String typeLogicalCaption();
    String typeLongCaption();
    String typeNumericCaption();
    String typeStringCaption();
    String typeStringCaptionRegister();
    String typeStringCaptionPadding();
    String typeTextCaption();
    String typeObjectCaption();
    String typeTimeCaption();
    String typeExcelFileCaption();
    String typeImageCaption();
    String typePDFFileCaption();
    String typeDBFFileCaption();
    String typeWordFileCaption();
    String typeTextFileCaption();
    String typeCSVFileCaption();
    String typeHTMLFileCaption();
    String typeJSONFileCaption();
    String typeXMLFileCaption();
    String typeTableFileCaption();
    String typeStaticFormatLinkCaption();
    String typeDynamicFormatLinkCaption();
    String typeExcelFileLinkCaption();
    String typeImageLinkCaption();
    String typePDFFileLinkCaption();
    String typeDBFFileLinkCaption();
    String typeWordFileLinkCaption();
    String typeTextFileLinkCaption();
    String typeCSVFileLinkCaption();
    String typeHTMLFileLinkCaption();
    String typeJSONFileLinkCaption();
    String typeXMLFileLinkCaption();
    String typeTableFileLinkCaption();

    String filterDataValue();

    String multipleFilterComponentAll();

    String pivotTotalsLabel();
    
    String pivotAggregatorSum();
    String pivotAggregatorMin();
    String pivotAggregatorMax();
    
    String pivotColumnAttribute();

    String pivotTableRenderer();
    String pivotTableBarchartRenderer();
    String pivotTableHeatmapRenderer();
    String pivotTableRowHeatmapRenderer();
    String pivotTableColHeatmapRenderer();
    String pivotBarchartRenderer();
    String pivotStackedBarchartRenderer();
    String pivotLinechartRenderer();
    String pivotAreachartRenderer();
    String pivotScatterchartRenderer();
    String pivotMultiplePiechartRenderer();
    String pivotHorizontalBarchartRenderer();
    String pivotHorizontalStackedBarchartRenderer();
    String pivotTreemapRenderer();

    String fullScreenModeEnable();
    String fullScreenModeDisable();

//    dateRangePicker
    String applyLabel();
    String cancelLabel();
    String customRangeLabel();
    String daysOfWeekSU();
    String daysOfWeekMO();
    String daysOfWeekTU();
    String daysOfWeekWE();
    String daysOfWeekTH();
    String daysOfWeekFR();
    String daysOfWeekSA();
    String monthJanuary();
    String monthFebruary();
    String monthMarch();
    String monthApril();
    String monthMay();
    String monthJune();
    String monthJuly();
    String monthAugust();
    String monthSeptember();
    String monthOctober();
    String monthNovember();
    String monthDecember();
    String today();
    String yesterday();
    String last7Days();
    String sevenDaysAgo();
    String last30Days();
    String thirtyDaysAgo();
    String thisMonth();
    String monthStart();
    String monthEnd();
    String toMonthEnd();
    String previousMonth();
    String previousMonthStart();
    String previousMonthEnd();
    String monthStartToCurrentDate();
    String thisYear();
    String thisYearStart();
    String thisYearEnd();
    String toYearEnd();
    String clear();

    String noResults();

    String doYouReallyWantToCloseForm();

    String suggestBoxMatchTip(String separator);
    String suggestBoxContainsTip();

    class Instance {
        private static final ClientMessages instance = GWT.create(ClientMessages.class);

        public static ClientMessages get() {
            return instance;
        }
    }
}
