package lsfusion.gwt.form.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface MainFrameMessages extends Messages {
    String title();

    String choseClass();

    String ok();

    String close();

    String cancel();

    String grid();
    String panel();
    String hide();

    String formRendererNotDefined();
    String formRendererRequired();

    String formGridExportToXls();
    String formGridPrintGrid();
    String formGridGroupGroupChange();

    String formQueriesCalculateSum();
    String formQueriesSumResult();
    String formQueriesUnableToCalculateSum();
    String formQueriesFilter();
    String formQueriesFilterApply();
    String formQueriesFilterAddCondition();
    String formQueriesFilterRemoveCondition();
    String formQueriesNumberOfEntries();

    String formGridPreferences();
    String formGridPreferencesCancel();
    String formGridPreferencesChange();
    String formGridPreferencesColumnCaption();
    String formGridPreferencesColumnPattern();
    String formGridPreferencesCompleteReset();
    String formGridPreferencesCompleteResetHeader();
    String formGridPreferencesDisplayedColumns();
    String formGridPreferencesFontSettings();
    String formGridPreferencesFontSize();
    String formGridPreferencesFontStyleBold();
    String formGridPreferencesFontStyleItalic();
    String formGridPreferencesForAllUsers();
    String formGridPreferencesForUser();
    String formGridPreferencesGridSettings();
    String formGridPreferencesHeaderHeight();
    String formGridPreferencesHiddenColumns();
    String formGridPreferencesNotSaved();
    String formGridPreferencesPageSize();
    String formGridPreferencesPropertyInPanel();
    String formGridPreferencesPropertyNotShown();
    String formGridPreferencesResetSettings();
    String formGridPreferencesResetSettingsSuccessfullyComplete();
    String formGridPreferencesSavedForCurrentUser();
    String formGridPreferencesSavedForAllUsers();
    String formGridPreferencesSaveSettings();
    String formGridPreferencesSaveSettingsSuccessfullyComplete();
    String formGridPreferencesSelectedColumnSettings();

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
    String rmiConnectionLostFatal();
    String rmiConnectionLostNonfatal();
    String rmiConnectionLostExit();
    String rmiConnectionLostRelogin();
    String rmiConnectionLostReconnect();


    class Instance {
        private static final MainFrameMessages instance = (MainFrameMessages) GWT.create(MainFrameMessages.class);

        public static MainFrameMessages get() {
            return instance;
        }
    }
}
