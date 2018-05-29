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

    String formGridExportToXlsx();
    String formGridPrintGrid();
    String formGridGroupGroupChange();
    
    String formTree();

    String formQueriesCalculateSum();
    String formQueriesSumResult();
    String formQueriesUnableToCalculateSum();
    String formQueriesFilter();
    String formQueriesFilterApply();
    String formQueriesFilterAddCondition();
    String formQueriesFilterRemoveCondition();
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
    String rmiConnectionLostWaitReconnect(int attempt);
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
