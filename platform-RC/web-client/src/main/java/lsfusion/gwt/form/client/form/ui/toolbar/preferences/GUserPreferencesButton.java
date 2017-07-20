package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.client.form.ui.GGridTable;
import lsfusion.gwt.form.client.form.ui.GGroupObjectController;
import lsfusion.gwt.form.client.form.ui.toolbar.GToolbarButton;

public class GUserPreferencesButton extends GToolbarButton {
    private final MainFrameMessages messages = MainFrameMessages.Instance.get();
    private static final String PREFERENCES_SAVED_ICON = "userPreferencesSaved.png";
    private static final String PREFERENCES_UNSAVED_ICON = "userPreferences.png";

    private final boolean canBeSaved;
    private final GGridTable table;
    private final GGroupObjectController groupController;
    
    public GUserPreferencesButton(GGridTable table, GGroupObjectController groupController, boolean canBeSaved) {
        super(table.hasUserPreferences() ? PREFERENCES_SAVED_ICON : PREFERENCES_UNSAVED_ICON, "");
        this.table = table;
        this.groupController = groupController;
        this.canBeSaved = canBeSaved;
        updateTooltip();
    }

    @Override
    public void addListener() {
        addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                GUserPreferencesDialog dialog = new GUserPreferencesDialog(table, groupController, canBeSaved) {
                    @Override
                    public void preferencesChanged() {
                        updateTooltip();
                        setModuleImagePath(table.generalPreferencesSaved() || table.userPreferencesSaved() ? PREFERENCES_SAVED_ICON : PREFERENCES_UNSAVED_ICON);
                    }
                };
                dialog.showDialog();
            }
        });
    }

    private void updateTooltip() {
        String tooltip = messages.formGridPreferences() + " (";
        if (table.userPreferencesSaved()) {
            tooltip += messages.formGridPreferencesSavedForCurrentUser();
        } else if (table.generalPreferencesSaved()) {
            tooltip += messages.formGridPreferencesSavedForAllUsers();
        } else {
            tooltip += messages.formGridPreferencesNotSaved();
        }

        setTitle(tooltip + ")");
    }
}
