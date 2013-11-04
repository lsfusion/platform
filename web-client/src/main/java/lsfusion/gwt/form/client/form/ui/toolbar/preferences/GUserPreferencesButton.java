package lsfusion.gwt.form.client.form.ui.toolbar.preferences;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.form.client.form.ui.GGridTable;
import lsfusion.gwt.form.client.form.ui.GGroupObjectController;
import lsfusion.gwt.form.client.form.ui.toolbar.GToolbarButton;

public class GUserPreferencesButton extends GToolbarButton {
    private static final String PREFERENCES_SAVED_ICON = "userPreferencesSaved.png";
    private static final String PREFERENCES_UNSAVED_ICON = "userPreferences.png";
    
    public GUserPreferencesDialog dialog;
    private GGridTable table;
    private GGroupObjectController groupController;

    public GUserPreferencesButton(GGridTable table, GGroupObjectController groupController) {
        super(table.hasUserPreferences() ? PREFERENCES_SAVED_ICON : PREFERENCES_UNSAVED_ICON, "");
        this.table = table;
        this.groupController = groupController;
        updateTooltip();
    }

    @Override
    public void addListener() {
        addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                GUserPreferencesDialog dialog = new GUserPreferencesDialog(table, groupController) {
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
        String tooltip = "Настройки таблицы (";
        if (table.userPreferencesSaved()) {
            tooltip += "Сохранены для текущего пользователя";
        } else if (table.generalPreferencesSaved()) {
            tooltip += "Сохранены для всех пользователей";
        } else {
            tooltip += "Не сохранены";
        }

        setTitle(tooltip + ")");
    }
}
