package lsfusion.client.form.object.table.grid.user.design;

import lsfusion.client.form.object.table.grid.controller.GridController;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.filter.user.FilterView;
import lsfusion.client.form.object.table.grid.user.toolbar.ToolbarGridButton;
import lsfusion.client.view.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static lsfusion.client.ClientResourceBundle.getString;

public class UserPreferencesButton extends ToolbarGridButton {
    public static final ImageIcon PREFERENCES_SAVED_ICON = new ImageIcon(FilterView.class.getResource("/images/userPreferencesSaved.png"));

    public static final ImageIcon PREFERENCES_UNSAVED_ICON = new ImageIcon(FilterView.class.getResource("/images/userPreferences.png"));
    private GridTable table;

    public UserPreferencesButton(final GridTable table, final GridController groupController) {
        super(table.hasUserPreferences() ? PREFERENCES_SAVED_ICON : PREFERENCES_UNSAVED_ICON, null);
        this.table = table;
        updateTooltip();

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UserPreferencesDialog dialog = new UserPreferencesDialog(MainFrame.instance, table, groupController, groupController.getFormController().hasCanonicalName()) {
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

