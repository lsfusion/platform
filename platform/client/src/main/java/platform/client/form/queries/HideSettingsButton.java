package platform.client.form.queries;

import jxl.CellView;
import jxl.Workbook;
import jxl.write.*;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.*;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.client.ClientResourceBundle;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.form.grid.GridTable;
import platform.client.form.grid.GridTableModel;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientIntegralClass;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Number;
import java.util.*;
import java.util.List;

public abstract class HideSettingsButton extends ToolbarGridButton {

    public HideSettingsDialog dialog;

    public HideSettingsButton() {
        super("/images/hideSettings.png", ClientResourceBundle.getString("Form.grid.hidesettings"));
    }

    public abstract void addListener();

    public class HideSettingsDialog extends JDialog {
        private GridTable initialTable;
        private List<JCheckBox> groupChecks = new ArrayList<JCheckBox>();


        public HideSettingsDialog(Frame owner, final GridTable initialTable) throws IOException {
            super(owner, ClientResourceBundle.getString("Form.grid.hidesettings"), true);
            this.initialTable = initialTable;

            setMinimumSize(new Dimension(300, 400));
            Rectangle bounds = new Rectangle();
            bounds.x = 100;
            bounds.y = 100;
            bounds.width = 300;
            bounds.height = 500;
            setBounds(bounds);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout());

            TitledPanel groupByPanel = new TitledPanel("Отображаемые колонки");

            JPanel allFieldsPanel = new JPanel();
            allFieldsPanel.setLayout(new BoxLayout(allFieldsPanel, BoxLayout.Y_AXIS));

            for (int i = 0; i < initialTable.getPropertyCount(); i++) {
                final JCheckBox checkBox = new JCheckBox(initialTable.getPropertyName(i).trim());
                if (!initialTable.getProperty(i).hide) {
                    checkBox.setSelected(true);
                }
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

            JButton executeButton = new JButton("OK");
            executeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        buttonPressed();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (dialog != null) {
                        dialog.firePropertyChange("buttonPressed", null, null);
                    }
                    initialTable.updateTable();
                }
            });

            JButton cancelButton = new JButton("Отмена");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.add(checkAllButton);
            buttonsPanel.add(executeButton);
            buttonsPanel.add(cancelButton);

            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BorderLayout());
            bottomPanel.add(buttonsPanel, BorderLayout.WEST);

            add(bottomPanel, BorderLayout.SOUTH);
            add(groupScrollPane, BorderLayout.CENTER);
        }

        private void buttonPressed() throws IOException {

            for (int i = 0; i < groupChecks.size(); i++) {
                if (groupChecks.get(i).isSelected()) {
                    initialTable.getProperty(i).hide = false;
                } else {
                    initialTable.getProperty(i).hide = true;
                }
            }
            dialog.setVisible(false);
            dispose();
        }
    }
}

