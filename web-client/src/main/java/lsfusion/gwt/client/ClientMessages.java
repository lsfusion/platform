package lsfusion.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface ClientMessages extends Messages {
    String yes();
    String no();
    String ok();
    String close();
    String cancel();
    String more();
    String logout();

    String error();
    String internalServerError();
    String actionTimeoutError();
    String sessionTimeoutError();

    String choosingClass();

    String grid();
    String panel();
    String hide();

    String formRendererEmpty();
    String formRendererNotDefined();
    String formRendererRequired();

    String formGridExport();

    String formGridTableView();
    String formGridPivotView();
    String formGridMapView();

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
    String formQueriesFilter();
    String formQueriesFilterAddCondition();
    String formQueriesFilterRemoveCondition();
    String formQueriesFilterResetConditions();
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

    String formFilterConditionViewNot();
    String formFilterConditionViewAnd();
    String formFilterConditionViewOr();
    String formFilterDialogHeader();

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

    String expandFilterWindow();
    String hideFilterWindow();
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

    String propertyEmptyCaption();

    String typeDateTimeCaption();
    String typeZDateTimeCaption();
    String typeActionCaption();
    String typeColorCaption();
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
    String typeWordFileLinkCaption();
    String typeTextFileLinkCaption();
    String typeCSVFileLinkCaption();
    String typeHTMLFileLinkCaption();
    String typeJSONFileLinkCaption();
    String typeXMLFileLinkCaption();
    String typeTableFileLinkCaption();

    String filterCompareStartsWith();
    String filterCompareContains();
    String filterCompareEndsWith();
    String filterDataValue();
    String filterObjectValue();
    String filterPropertyValue();

    String fileEditorTitle();
    String fileEditorChooseFile();
    String fileEditorChooseFileSuffix();
    String fileEditorAddFiles();
    String fileEditorDropFiles();
    String fileEditorCancel();

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
    
    class Instance {
        private static final ClientMessages instance = GWT.create(ClientMessages.class);

        public static ClientMessages get() {
            return instance;
        }
    }
}
