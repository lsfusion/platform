package lsfusion.client.form.grid;

import lsfusion.client.Main;
import lsfusion.client.form.GroupObjectController;
import lsfusion.client.form.queries.FilterView;
import lsfusion.client.form.queries.ToolbarGridButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static lsfusion.client.ClientResourceBundle.getString;

public class UserPreferencesButton extends ToolbarGridButton {
    public static final ImageIcon PREFERENCES_SAVED_ICON = new ImageIcon(FilterView.class.getResource("/images/userPreferencesSaved.png"));

    public static final ImageIcon PREFERENCES_UNSAVED_ICON = new ImageIcon(FilterView.class.getResource("/images/userPreferences.png"));
    private GridTable table;

    public UserPreferencesButton(final GridTable table, final GroupObjectController groupController) {
        super(table.hasUserPreferences() ? PREFERENCES_SAVED_ICON : PREFERENCES_UNSAVED_ICON, null);
        this.table = table;
        updateTooltip();

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    UserPreferencesDialog dialog = new UserPreferencesDialog(Main.frame, table, groupController, groupController.getForm().hasCanonicalName()) {
                        @Override
                        public void preferencesChanged() {
                            setIcon(table.generalPreferencesSaved() || table.userPreferencesSaved() ? PREFERENCES_SAVED_ICON : PREFERENCES_UNSAVED_ICON);
                            updateTooltip();
                        }
                    };
                    dialog.setVisible(true);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void updateTooltip() {
        String tooltip = getString("form.grid.preferences") + " (";
        if (table.userPreferencesSaved()) {
            tooltip += getString("form.grid.preferences.saved.for.current.users");
        } else if (table.generalPreferencesSaved()) {
            tooltip += getString("form.grid.preferences.saved.for.all.users");
        } else {
            tooltip += getString("form.grid.preferences.not.saved");
        }

        setToolTipText(tooltip + ")");
    }
}

