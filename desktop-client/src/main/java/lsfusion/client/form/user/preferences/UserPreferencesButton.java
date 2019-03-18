package lsfusion.client.form.user.preferences;

import lsfusion.client.Main;
import lsfusion.client.form.object.table.GroupObjectController;
import lsfusion.client.base.RmiQueue;
import lsfusion.client.form.object.table.grid.GridTable;
import lsfusion.client.form.user.queries.FilterView;
import lsfusion.client.form.user.queries.ToolbarGridButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
                UserPreferencesDialog dialog = new UserPreferencesDialog(Main.frame, table, groupController, groupController.getFormController().hasCanonicalName()) {
                    @Override
                    public void preferencesChanged() {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                setIcon(table.generalPreferencesSaved() || table.userPreferencesSaved() ? PREFERENCES_SAVED_ICON : PREFERENCES_UNSAVED_ICON);
                                updateTooltip();
                            }
                        });
                    }
                };
                dialog.setVisible(true);
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

