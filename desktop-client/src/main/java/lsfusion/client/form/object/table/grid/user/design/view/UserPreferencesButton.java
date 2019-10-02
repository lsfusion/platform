package lsfusion.client.form.object.table.grid.user.design.view;

import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.filter.user.FilterView;
import lsfusion.client.form.object.table.grid.controller.GridController;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.view.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static lsfusion.client.ClientResourceBundle.getString;

public class UserPreferencesButton extends ToolbarGridButton {
    public static final ImageIcon userPreferencesIcon = new ImageIcon(FilterView.class.getResource("/images/userPreferences.png"));
    private GridTable table;

    public UserPreferencesButton(final GridTable table, final GridController groupController) {
        super(userPreferencesIcon, null);
        showBackground(table.hasUserPreferences());
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
                                showBackground((table.generalPreferencesSaved() || table.userPreferencesSaved()));
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

