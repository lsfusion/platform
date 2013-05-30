package platform.client.form.grid;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.client.ArrayListTransferHandler;
import platform.client.Main;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.form.ClientFormController;
import platform.client.form.queries.FilterView;
import platform.client.form.queries.ToolbarGridButton;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.form.ColumnUserPreferences;
import platform.interop.form.FormUserPreferences;
import platform.interop.form.GroupObjectUserPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static platform.client.ClientResourceBundle.getString;

public abstract class UserPreferencesButton extends ToolbarGridButton {
    private static final ImageIcon savedIcon = new ImageIcon(FilterView.class.getResource("/images/userPreferencesSaved.png"));

    private static final ImageIcon unsavedIcon = new ImageIcon(FilterView.class.getResource("/images/userPreferences.png"));

    public HideSettingsDialog dialog;

    public UserPreferencesButton(boolean hasUserPreferences) {
        super(hasUserPreferences ? savedIcon : unsavedIcon, getString("form.grid.user.preferences"));
    }

    public abstract void addListener();

    public class HideSettingsDialog extends JDialog {
        private GridTable initialTable;
        private ClientFormController form;
        private Map<String, Integer> orderMap = new HashMap<String, Integer>();
        DefaultListModel visibleListModel, invisibleListModel;
        JList visibleList, invisibleList;


        public HideSettingsDialog(Frame owner, final GridTable initialTable, ClientFormController form) throws IOException {
            super(owner, getString("form.grid.user.preferences"), true);
            this.initialTable = initialTable;
            this.form = form;

            setMinimumSize(new Dimension(500, 500));
            Rectangle bounds = new Rectangle();
            bounds.x = 100;
            bounds.y = 100;
            bounds.width = 500;
            bounds.height = 500;
            setBounds(bounds);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout());

            final JPanel allFieldsPanel = new JPanel();
            allFieldsPanel.setLayout(new BoxLayout(allFieldsPanel, BoxLayout.Y_AXIS));

            Map<ClientPropertyDraw, Integer> propertyOrderMap = new HashMap<ClientPropertyDraw, Integer>();
            List<ClientPropertyDraw> properties = initialTable.getProperties();
            for (int i = 0; i < properties.size(); i++) {
                if (properties.get(i).orderUser == null)
                    properties.get(i).orderUser = i;
                propertyOrderMap.put(properties.get(i), properties.get(i).orderUser);

            }
            ValueComparator valueComparator = new ValueComparator(propertyOrderMap);
            TreeMap<ClientPropertyDraw, Integer> propertyOrderTreeMap = new TreeMap(valueComparator);
            propertyOrderTreeMap.putAll(propertyOrderMap);

            ArrayListTransferHandler arrayListHandler = new ArrayListTransferHandler();

            visibleListModel = new DefaultListModel();
            invisibleListModel = new DefaultListModel();

            int i = 0;
            for (Map.Entry<ClientPropertyDraw, Integer> entry : propertyOrderTreeMap.entrySet()) {
                String caption = BaseUtils.nullTrim(entry.getKey().getCaption());
                orderMap.put(caption, i);

                Boolean needToHide = entry.getKey().hideUser == null ? entry.getKey().hide : entry.getKey().hideUser;
                if (!needToHide)
                    visibleListModel.addElement(caption);
                else
                    invisibleListModel.addElement(caption);
                i++;
            }

            visibleList = new JList(visibleListModel);
            visibleList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            visibleList.setTransferHandler(arrayListHandler);
            visibleList.setDragEnabled(true);
            JScrollPane visibleListView = new JScrollPane(visibleList);
            visibleListView.setPreferredSize(new Dimension(200, 100));
            TitledPanel visiblePanel = new TitledPanel(getString("form.grid.displayed.columns"));
            visiblePanel.setLayout(new BorderLayout());
            visiblePanel.add(visibleListView, BorderLayout.CENTER);

            visibleList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JList list = (JList) e.getSource();
                    if (e.getClickCount() == 2) {
                        int index = list.locationToIndex(e.getPoint());
                        invisibleListModel.addElement(visibleListModel.get(index));
                        visibleListModel.remove(index);
                    }
                }
            });

            invisibleList = new JList(invisibleListModel);
            invisibleList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            invisibleList.setTransferHandler(arrayListHandler);
            invisibleList.setDragEnabled(true);
            JScrollPane invisibleListView = new JScrollPane(invisibleList);
            invisibleListView.setPreferredSize(new Dimension(200, 100));
            TitledPanel invisiblePanel = new TitledPanel(getString("form.grid.hidden.columns"));
            invisiblePanel.setLayout(new BorderLayout());
            invisiblePanel.add(invisibleListView, BorderLayout.CENTER);

            invisibleList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JList list = (JList) e.getSource();
                    if (e.getClickCount() == 2) {
                        Rectangle r = list.getCellBounds(0, list.getLastVisibleIndex());
                        if (r != null && r.contains(e.getPoint())) {
                            int index = list.locationToIndex(e.getPoint());
                            visibleListModel.addElement(invisibleListModel.get(index));
                            invisibleListModel.remove(index);
                        }
                    }
                }
            });

            final JButton applyButton = new JButton(getString("form.grid.hide.apply"));
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

            final JButton applyForAllButton = new JButton(getString("form.grid.hide.apply.for.all"));
            applyForAllButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (Main.configurationAccessAllowed) {
                            applyButtonPressed(true);
                        } else {
                            JOptionPane.showMessageDialog(null, getString("form.grid.hide.not.enough.rights"), getString("form.grid.hide.error"), JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    if (dialog != null) {
                        dialog.firePropertyChange("buttonPressed", null, null);
                    }
                    initialTable.updateTable();
                }
            });

            final JButton resetButton = new JButton(getString("form.grid.hide.reset"));
            resetButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        resetButtonPressed(false);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    if (dialog != null) {
                        dialog.firePropertyChange("buttonPressed", null, null);
                    }
                    initialTable.updateTable();
                }
            });

            final JButton resetForAllButton = new JButton(getString("form.grid.hide.reset.for.all"));
            resetForAllButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (Main.configurationAccessAllowed) {
                            resetButtonPressed(true);
                        } else {
                            JOptionPane.showMessageDialog(null, getString("form.grid.hide.not.enough.rights"), getString("form.grid.hide.error"), JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    if (dialog != null) {
                        dialog.firePropertyChange("buttonPressed", null, null);
                    }
                    initialTable.updateTable();
                }
            });

            JButton showAllButton = new JButton(new ImageIcon(Main.class.getResource("/images/arrowLeft.png")));
            showAllButton.setBorder(null);
            showAllButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < invisibleListModel.getSize(); i++) {
                        visibleListModel.addElement(invisibleListModel.get(i));
                    }
                    invisibleListModel.clear();
                }
            });

            JButton hideAllButton = new JButton(new ImageIcon(Main.class.getResource("/images/arrowRight.png")));
            hideAllButton.setBorder(null);
            hideAllButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < visibleListModel.getSize(); i++) {
                        invisibleListModel.addElement(visibleListModel.get(i));
                    }
                    visibleListModel.clear();
                }
            });


            JPanel arrowsPanel = new JPanel();
            arrowsPanel.setLayout(new BoxLayout(arrowsPanel, BoxLayout.Y_AXIS));
            arrowsPanel.add(hideAllButton);
            arrowsPanel.add(showAllButton);

            JPanel columnsPanel = new JPanel();
            columnsPanel.setLayout(new BoxLayout(columnsPanel, BoxLayout.X_AXIS));
            columnsPanel.add(visiblePanel);
            columnsPanel.add(arrowsPanel, BoxLayout.Y_AXIS);
            columnsPanel.add(invisiblePanel);

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

            JButton cancelButton = new JButton(getString("form.grid.hide.cancel"));
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.add(okButton);
            buttonsPanel.add(cancelButton);


            TitledPanel applyButtonsPanel = new TitledPanel(getString("form.grid.hide.save.settings"));
            applyButtonsPanel.add(applyButton, BorderLayout.NORTH);
            applyButtonsPanel.add(applyForAllButton, BorderLayout.SOUTH);

            TitledPanel resetButtonsPanel = new TitledPanel(getString("form.grid.hide.reset.settings"));
            resetButtonsPanel.add(resetButton, BorderLayout.NORTH);
            resetButtonsPanel.add(resetForAllButton, BorderLayout.SOUTH);

            JPanel applyResetButtonsPanel = new JPanel();
            applyResetButtonsPanel.add(applyButtonsPanel, BorderLayout.WEST);
            applyResetButtonsPanel.add(resetButtonsPanel, BorderLayout.EAST);

            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BorderLayout());
            bottomPanel.add(applyResetButtonsPanel, BorderLayout.NORTH);
            bottomPanel.add(buttonsPanel, BorderLayout.EAST);

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            mainPanel.add(columnsPanel, BorderLayout.CENTER);
            mainPanel.add(bottomPanel, BorderLayout.SOUTH);

            setLayout(new BorderLayout());
            add(mainPanel, BorderLayout.CENTER);


            add(bottomPanel, BorderLayout.SOUTH);
        }

        private void okButtonPressed() throws IOException {

            for (int i = 0; i < visibleListModel.getSize(); i++) {
                int index = orderMap.get(visibleListModel.get(i));
                initialTable.getProperties().get(index).orderUser = i;
                if (initialTable.getColumnModel().getColumnCount() > index)
                    initialTable.getProperties().get(index).widthUser = initialTable.getColumnModel().getColumn(index).getPreferredWidth();
                else
                    initialTable.getProperties().get(index).widthUser = null;
                initialTable.getProperties().get(index).hideUser = false;
            }
            for (int i = 0; i < invisibleListModel.getSize(); i++) {
                int index = orderMap.get(invisibleListModel.get(i));
                initialTable.getProperties().get(index).orderUser = visibleListModel.getSize() + i;
                initialTable.getProperties().get(index).hideUser = true;
            }

            dialog.setVisible(false);
            dispose();
        }

        private void applyButtonPressed(Boolean forAllUsers) throws IOException {
            Map<Pair<ClientPropertyDraw,ClientGroupObjectValue>, Boolean> orderDirections = initialTable.getOrderDirections();
            Map<ClientPropertyDraw, Pair<Boolean, Integer>> sortDirections = new HashMap<ClientPropertyDraw, Pair<Boolean, Integer>>();
            int j = 1;
            for(Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Boolean> entry : orderDirections.entrySet()){
                sortDirections.put(entry.getKey().first, new Pair<Boolean, Integer>(entry.getValue(), j));
                j++;
            }

            Map<String, ColumnUserPreferences> preferences = new HashMap<String, ColumnUserPreferences>();
            for (int i = 0; i < visibleListModel.getSize(); i++) {
                int index = orderMap.get(visibleListModel.get(i));
                ClientPropertyDraw property = initialTable.getProperties().get(index);
                Boolean sortDirection = sortDirections.containsKey(property) ? sortDirections.get(property).first : null;
                Integer sortIndex = sortDirections.containsKey(property) ? sortDirections.get(property).second : null;
                preferences.put(property.getSID(),
                        new ColumnUserPreferences(false, initialTable.getProperties().get(index).widthUser, i, sortDirection != null ? sortIndex : null, sortDirection));
                initialTable.getProperties().get(index).hideUser = false;
                initialTable.getProperties().get(index).orderUser = i;
                orderMap.put(initialTable.getProperties().get(index).caption, i);
            }

            for (int i = 0; i < invisibleListModel.getSize(); i++) {
                int index = orderMap.get(invisibleListModel.get(i));
                preferences.put(initialTable.getProperties().get(index).getSID(),
                        new ColumnUserPreferences(true, initialTable.getProperties().get(index).widthUser, i + visibleListModel.getSize(), 0, null));
                initialTable.getProperties().get(index).hideUser = true;
                initialTable.getProperties().get(index).orderUser = visibleListModel.getSize() + i;
                orderMap.put(initialTable.getProperties().get(index).caption, visibleListModel.getSize() + i);
            }
            if (initialTable.getProperties().size() != 0) {
                List<GroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<GroupObjectUserPreferences>();
                groupObjectUserPreferencesList.add(new GroupObjectUserPreferences(preferences, initialTable.getProperties().get(0).groupObject.getSID(), true));
                form.saveUserPreferences(new FormUserPreferences(groupObjectUserPreferencesList), forAllUsers);
            }
            JOptionPane.showMessageDialog(this, getString("form.grid.hide.save.settings.successfully.complete"), getString("form.grid.hide.save.complete"), JOptionPane.INFORMATION_MESSAGE);
        }

        private void resetButtonPressed(Boolean forAllUsers) throws IOException {
            Map<String, ColumnUserPreferences> preferences = new HashMap<String, ColumnUserPreferences>();
            initialTable.setOrResetPreferredColumnWidths();
            for (int i = 0; i < visibleListModel.getSize(); i++) {
                int index = orderMap.get(visibleListModel.get(i));
                String propertySID = initialTable.getProperties().get(index).getSID();
                preferences.put(propertySID,
                        new ColumnUserPreferences(null, null, null, null, null));
                initialTable.getProperties().get(index).hideUser = null;
                if (initialTable.getColumnModel().getColumnCount() > index)
                    initialTable.getProperties().get(index).widthUser = initialTable.getColumnModel().getColumn(index).getPreferredWidth();
                else
                    initialTable.getProperties().get(index).widthUser = null;
                initialTable.getProperties().get(index).sortUser = null;
                initialTable.getProperties().get(index).ascendingSortUser = null;
                initialTable.getProperties().get(index).orderUser = initialTable.getProperties().get(index).getID();
            }

            for (int i = 0; i < invisibleListModel.getSize(); i++) {
                int index = orderMap.get(invisibleListModel.get(i));
                preferences.put(initialTable.getProperties().get(index).getSID(),
                        new ColumnUserPreferences(null, null, null, null, null));
                initialTable.getProperties().get(index).hideUser = null;
                if (initialTable.getColumnModel().getColumnCount() > index)
                    initialTable.getProperties().get(index).widthUser = initialTable.getColumnModel().getColumn(index).getPreferredWidth();
                else
                    initialTable.getProperties().get(index).widthUser = null;
                initialTable.getProperties().get(index).sortUser = null;
                initialTable.getProperties().get(index).ascendingSortUser = null;
                initialTable.getProperties().get(index).orderUser = initialTable.getProperties().get(index).getID();
            }
            if (initialTable.getProperties().size() != 0) {
                List<GroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<GroupObjectUserPreferences>();
                groupObjectUserPreferencesList.add(new GroupObjectUserPreferences(preferences, initialTable.getProperties().get(0).groupObject.getSID(), false));
                form.saveUserPreferences(new FormUserPreferences(groupObjectUserPreferencesList), forAllUsers);
            }
            initialTable.updateTable();
            visibleListModel.clear();
            invisibleListModel.clear();
            for (int i = 0; i < initialTable.getProperties().size(); i++) {
                ClientPropertyDraw property = initialTable.getProperties().get(i);
                String caption = BaseUtils.nullTrim(property.getCaption());
                orderMap.put(caption, i);
                if (!property.hide)
                    visibleListModel.addElement(caption);
                else
                    invisibleListModel.addElement(caption);
            }

            JOptionPane.showMessageDialog(this, getString("form.grid.hide.reset.settings.successfully.complete"), getString("form.grid.hide.reset.complete"), JOptionPane.INFORMATION_MESSAGE);
        }

        class ValueComparator implements Comparator {
            Map base;

            public ValueComparator(Map base) {
                this.base = base;
            }

            public int compare(Object a, Object b) {

                if ((Integer) base.get(a) < (Integer) base.get(b))
                    return -1;
                else
                    return 1;
            }
        }
    }
}

