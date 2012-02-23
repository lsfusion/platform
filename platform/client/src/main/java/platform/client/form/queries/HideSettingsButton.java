package platform.client.form.queries;

import platform.client.ClientResourceBundle;
import platform.client.Main;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.form.ClientFormController;
import platform.client.form.grid.GridTable;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.form.FormColumnUserPreferences;
import platform.interop.form.FormUserPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.Boolean;
import java.util.*;
import java.util.List;

public abstract class HideSettingsButton extends ToolbarGridButton {

    public HideSettingsDialog dialog;

    public HideSettingsButton() {
        super("/images/hideSettings.png", ClientResourceBundle.getString("form.grid.hidesettings"));
    }

    public abstract void addListener();

    public class HideSettingsDialog extends JDialog {
        private GridTable initialTable;
        private ClientFormController form;
        private List<JCheckBox> groupChecks = new ArrayList<JCheckBox>();


        public HideSettingsDialog(Frame owner, final GridTable initialTable, ClientFormController form) throws IOException {
            super(owner, ClientResourceBundle.getString("form.grid.hidesettings"), true);
            this.initialTable = initialTable;
            this.form = form;

            setMinimumSize(new Dimension(300, 500));
            Rectangle bounds = new Rectangle();
            bounds.x = 100;
            bounds.y = 100;
            bounds.width = 300;
            bounds.height = 500;
            setBounds(bounds);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout());

            TitledPanel groupByPanel = new TitledPanel(ClientResourceBundle.getString("form.grid.displayed.columns"));

            JPanel allFieldsPanel = new JPanel();
            allFieldsPanel.setLayout(new BoxLayout(allFieldsPanel, BoxLayout.Y_AXIS));

            final JButton applyButton = new JButton(ClientResourceBundle.getString("form.grid.hide.apply"));
            applyButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        applyButtonPressed(false);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (dialog != null) {
                        dialog.firePropertyChange("buttonPressed", null, null);
                    }
                    initialTable.updateTable();
                }
            });

            final JButton applyForAllButton = new JButton(ClientResourceBundle.getString("form.grid.hide.apply.for.all"));
            applyForAllButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        Boolean permission = Main.frame.remoteNavigator.getConfiguratorSecurityPolicy();
                        if ((permission != null) && (permission == true))
                            applyButtonPressed(true);
                        else
                            JOptionPane.showMessageDialog(null, ClientResourceBundle.getString("form.grid.hide.not.enough.rights"), ClientResourceBundle.getString("form.grid.hide.error"), JOptionPane.ERROR_MESSAGE);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (dialog != null) {
                        dialog.firePropertyChange("buttonPressed", null, null);
                    }
                    initialTable.updateTable();
                }
            });

            for (int i = 0; i < initialTable.getPropertyCount(); i++) {
                final JCheckBox checkBox = new JCheckBox(initialTable.getPropertyName(i).trim());
                ClientPropertyDraw property = initialTable.getProperty(i);
                Boolean needToHide = property.hideUser == null ? property.hide : property.hideUser;
                if (!needToHide) {
                    checkBox.setSelected(true);
                    if (property.hideUser == null)
                        checkBox.setForeground(Color.gray);
                }
                checkBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        checkBox.setForeground(Color.black);
                    }
                });
                groupChecks.add(checkBox);

                JPanel fieldPanel = new JPanel();
                fieldPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                fieldPanel.add(checkBox);
                fieldPanel.setPreferredSize(new Dimension(fieldPanel.getPreferredSize().width, checkBox.getPreferredSize().height + 3));
                allFieldsPanel.add(fieldPanel);
            }

            groupByPanel.setLayout(new BorderLayout());
            groupByPanel.add(allFieldsPanel, BorderLayout.NORTH);

            JScrollPane groupScrollPane = new JScrollPane();
            groupScrollPane.setViewportView(groupByPanel);

            JButton checkAllButton = new JButton(ClientResourceBundle.getString("form.queries.check.all"));
            checkAllButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < groupChecks.size(); i++) {
                        groupChecks.get(i).setSelected(true);
                    }
                }
            });

            JButton okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        okButtonPressed();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (dialog != null) {
                        dialog.firePropertyChange("buttonPressed", null, null);
                    }
                    initialTable.updateTable();
                }
            });

            JButton cancelButton = new JButton(ClientResourceBundle.getString("form.grid.hide.cancel"));
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.add(okButton);
            buttonsPanel.add(cancelButton);

            JPanel checkAllPanel = new JPanel();
            checkAllPanel.add(checkAllButton);

            TitledPanel applyButtonsPanel = new TitledPanel(ClientResourceBundle.getString("form.grid.hide.save.settings"));
            applyButtonsPanel.add(applyButton, BorderLayout.NORTH);
            applyButtonsPanel.add(applyForAllButton, BorderLayout.SOUTH);

            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BorderLayout());
            bottomPanel.add(applyButtonsPanel, BorderLayout.NORTH);
            bottomPanel.add(checkAllPanel, BorderLayout.WEST);
            bottomPanel.add(buttonsPanel, BorderLayout.EAST);

            add(bottomPanel, BorderLayout.SOUTH);
            add(groupScrollPane, BorderLayout.CENTER);
        }

        private void okButtonPressed() throws IOException {

            for (int i = 0; i < groupChecks.size(); i++) {
                if (groupChecks.get(i).getForeground().equals(Color.black)) {
                    if (groupChecks.get(i).isSelected()) {
                        initialTable.getProperty(i).hideUser = false;
                    } else {
                        initialTable.getProperty(i).hideUser = true;
                    }
                }
            }
            dialog.setVisible(false);
            dispose();
        }

        private void applyButtonPressed(Boolean forAllUsers) throws IOException {

            Map<String, FormColumnUserPreferences> preferences = new HashMap<String, FormColumnUserPreferences>();

            for (int i = 0; i < groupChecks.size(); i++) {
                Boolean needToHideSet = groupChecks.get(i).getForeground().equals(Color.black);
                Boolean isSelected = groupChecks.get(i).isSelected();
                preferences.put(initialTable.getProperty(i).getSID(),
                        new FormColumnUserPreferences(!needToHideSet ? null : isSelected, initialTable.getProperty(i).widthUser));
                if (needToHideSet)
                    if (isSelected) {
                        initialTable.getProperty(i).hideUser = false;
                    } else {
                        initialTable.getProperty(i).hideUser = true;
                    }
            }
            form.remoteForm.saveUserPreferences(new FormUserPreferences(preferences), forAllUsers);
            JOptionPane.showMessageDialog(this, ClientResourceBundle.getString("form.grid.hide.save.settings.successfully.complete"), ClientResourceBundle.getString("form.grid.hide.save.complete"), JOptionPane.INFORMATION_MESSAGE);
        }
    }
}

