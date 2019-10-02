package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.grid.view.GGridTable;

public class GUserPreferencesButton extends GToolbarButton {
    private final ClientMessages messages = ClientMessages.Instance.get();

    private final boolean canBeSaved;
    private final GGridTable table;
    private final GGridController groupController;
    
    public GUserPreferencesButton(GGridTable table, GGridController groupController, boolean canBeSaved) {
        super(table.hasUserPreferences() ? PREFERENCES_SAVED_ICON : PREFERENCES_UNSAVED_ICON, "");
        this.table = table;
        this.groupController = groupController;
        this.canBeSaved = canBeSaved;
        showBackground(table.hasUserPreferences());
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
                        showBackground(table.generalPreferencesSaved() || table.userPreferencesSaved());
                        updateTooltip();
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
