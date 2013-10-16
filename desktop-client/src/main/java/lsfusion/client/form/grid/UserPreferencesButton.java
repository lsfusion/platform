package lsfusion.client.form.grid;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.ArrayListTransferHandler;
import lsfusion.client.Main;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.queries.FilterView;
import lsfusion.client.form.queries.ToolbarGridButton;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.FontInfo;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class UserPreferencesButton extends ToolbarGridButton {
    private static final ImageIcon savedIcon = new ImageIcon(FilterView.class.getResource("/images/userPreferencesSaved.png"));

    private static final ImageIcon unsavedIcon = new ImageIcon(FilterView.class.getResource("/images/userPreferences.png"));

    public HideSettingsDialog dialog;

    boolean hasUserPreferences;

    public int fontSize;
    public boolean isFontBold;
    public boolean isFontItalic;

    public UserPreferencesButton(boolean hasUserPreferences, FontInfo fontInfo) {
        super(hasUserPreferences ? savedIcon : unsavedIcon, getString("form.grid.user.preferences"));
        this.hasUserPreferences = hasUserPreferences;
        this.fontSize = fontInfo == null ? 0 : fontInfo.getFontSize();
        this.isFontBold = fontInfo != null && fontInfo.isBold();
        this.isFontItalic = fontInfo != null && fontInfo.isItalic();
    }

    public abstract void addListener();

    public class HideSettingsDialog extends JDialog {
        private GridTable initialTable;
        private ClientFormController form;
        private Map<String, Integer> orderMap = new HashMap<String, Integer>();
        DefaultListModel visibleListModel, invisibleListModel;
        JList visibleList, invisibleList;
        JLabel fontSizeLabel;
        JTextField fontSizeField;
        JCheckBox isFontBoldCheckBox;
        JCheckBox isFontItalicCheckBox;


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
                if (properties.get(i).orderUser == null) {
                    if (hasUserPreferences)
                        properties.get(i).hideUser = true;
                    properties.get(i).orderUser = hasUserPreferences ? (Integer.MAX_VALUE - i) : i;
                }
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

            fontSize = fontSize == 0 ? initialTable.getFont().getSize() : fontSize;
            fontSizeLabel = new JLabel(getString("descriptor.editor.font.size") + ": ");
            fontSizeField = new JTextField(String.valueOf(fontSize), 2);
            isFontBold = isFontBold ? initialTable.getFont().isBold() : isFontBold;
            isFontBoldCheckBox = new JCheckBox(getString("descriptor.editor.font.style.bold"), isFontBold);
            isFontItalic = isFontItalic ? initialTable.getFont().isItalic() : isFontItalic;
            isFontItalicCheckBox = new JCheckBox(getString("descriptor.editor.font.style.italic"), isFontItalic);
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

            final JButton applyButton = new JButton(getString("form.grid.hide.save.settings"));
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

            final JButton resetButton = new JButton(getString("form.grid.hide.reset.settings"));
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


            TitledPanel currentUserPanel = new TitledPanel(getString("form.grid.hide.for.user"));
            currentUserPanel.add(applyButton, BorderLayout.NORTH);
            currentUserPanel.add(resetButton, BorderLayout.SOUTH);

            Box fontAndApplyResetPanel = new Box(BoxLayout.Y_AXIS);

            TitledPanel fontPanel = new TitledPanel(getString("form.grid.hide.font.settings"));
            fontPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            fontPanel.add(fontSizeLabel);
            fontPanel.add(fontSizeField);
            fontPanel.add(isFontBoldCheckBox);
            fontPanel.add(isFontItalicCheckBox);
            fontAndApplyResetPanel.add(fontPanel);

            JPanel applyResetButtonsPanel = new JPanel();
            applyResetButtonsPanel.add(currentUserPanel, BorderLayout.WEST);

            if (Main.configurationAccessAllowed) {
                final JButton applyForAllButton = new JButton(getString("form.grid.hide.save.settings"));
                applyForAllButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            applyButtonPressed(true);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        if (dialog != null) {
                            dialog.firePropertyChange("buttonPressed", null, null);
                        }
                        initialTable.updateTable();
                    }
                });

                final JButton resetForAllButton = new JButton(getString("form.grid.hide.reset.settings"));
                resetForAllButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            resetButtonPressed(true);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        if (dialog != null) {
                            dialog.firePropertyChange("buttonPressed", null, null);
                        }
                        initialTable.updateTable();
                    }
                });

                TitledPanel allUsersPanelPanel = new TitledPanel(getString("form.grid.hide.for.all.users"));
                allUsersPanelPanel.add(applyForAllButton, BorderLayout.NORTH);
                allUsersPanelPanel.add(resetForAllButton, BorderLayout.SOUTH);
                applyResetButtonsPanel.add(allUsersPanelPanel, BorderLayout.EAST);
            }

            fontAndApplyResetPanel.add(applyResetButtonsPanel);

            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BorderLayout());
            bottomPanel.add(fontAndApplyResetPanel, BorderLayout.NORTH);
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
                initialTable.getProperties().get(index).hideUser = false;
            }
            for (int i = 0; i < invisibleListModel.getSize(); i++) {
                int index = orderMap.get(invisibleListModel.get(i));
                initialTable.getProperties().get(index).orderUser = visibleListModel.getSize() + i;
                initialTable.getProperties().get(index).hideUser = true;
            }
            int newFontSize = getFontSize(initialTable.getFont().getSize());
            if (newFontSize == 0)
                initialTable.setFont(initialTable.getFont().deriveFont(getFontStyle()));
            else
                initialTable.setFont(initialTable.getFont().deriveFont(getFontStyle(), newFontSize));
            fontSize = initialTable.getFont().getSize();
            isFontBold = initialTable.getFont().isBold();
            isFontItalic = initialTable.getFont().isItalic();

            dialog.setVisible(false);
            dispose();
        }

        private void applyButtonPressed(Boolean forAllUsers) throws IOException {
            Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Boolean> orderDirections = initialTable.getOrderDirections();
            Map<ClientPropertyDraw, Pair<Boolean, Integer>> sortDirections = new HashMap<ClientPropertyDraw, Pair<Boolean, Integer>>();
            int j = 1;
            for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Boolean> entry : orderDirections.entrySet()) {
                sortDirections.put(entry.getKey().first, new Pair<Boolean, Integer>(entry.getValue(), j));
                j++;
            }

            Map<String, ColumnUserPreferences> preferences = new HashMap<String, ColumnUserPreferences>();
            for (int i = 0; i < visibleListModel.getSize(); i++) {
                int index = orderMap.get(visibleListModel.get(i));
                ClientPropertyDraw property = initialTable.getProperties().get(index);
                preferences.put(property.getSID(), refreshPropertyUserPreferences(property, false, i, sortDirections));
            }

            for (int i = 0; i < invisibleListModel.getSize(); i++) {
                int index = orderMap.get(invisibleListModel.get(i));
                ClientPropertyDraw property = initialTable.getProperties().get(index);
                int propertyOrder = visibleListModel.getSize() + i;
                preferences.put(property.getSID(), refreshPropertyUserPreferences(property, true, propertyOrder, sortDirections));
            }
            if (initialTable.getProperties().size() != 0) {
                List<GroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<GroupObjectUserPreferences>();
                FontInfo fontInfo = getFontInfo(false);
                if (fontInfo.fontSize == 0) {
                    fontInfo = fontInfo.derive(initialTable.getFont().getSize());
                    fontSizeField.setText(String.valueOf(initialTable.getFont().getSize()));
                }
                groupObjectUserPreferencesList.add(new GroupObjectUserPreferences(preferences, initialTable.getProperties().get(0).groupObject.getSID(), fontInfo, true));
                form.saveUserPreferences(new FormUserPreferences(groupObjectUserPreferencesList), forAllUsers);
            }
            JOptionPane.showMessageDialog(this, getString("form.grid.hide.save.settings.successfully.complete"), getString("form.grid.hide.save.complete"), JOptionPane.INFORMATION_MESSAGE);
            setIcon(savedIcon);
        }

        private ColumnUserPreferences refreshPropertyUserPreferences(ClientPropertyDraw property, boolean hide, int propertyOrder,
                                                                     Map<ClientPropertyDraw, Pair<Boolean, Integer>> sortDirections) {
            Boolean sortDirection = sortDirections.containsKey(property) ? sortDirections.get(property).first : null;
            Integer sortIndex = sortDirections.containsKey(property) ? sortDirections.get(property).second : null;
            property.hideUser = hide;
            property.orderUser = propertyOrder;
            orderMap.put(property.caption, propertyOrder);
            return new ColumnUserPreferences(hide, property.widthUser, propertyOrder, sortDirection != null ? sortIndex : null, sortDirection);
        }

        private void resetButtonPressed(Boolean forAllUsers) throws IOException {
            Map<String, ColumnUserPreferences> preferences = new HashMap<String, ColumnUserPreferences>();
            for (int i = 0; i < visibleListModel.getSize(); i++) {
                int index = orderMap.get(visibleListModel.get(i));
                ClientPropertyDraw property = initialTable.getProperties().get(index);
                preferences.put(property.getSID(), resetPropertyUserPreferences(property));
            }

            for (int i = 0; i < invisibleListModel.getSize(); i++) {
                int index = orderMap.get(invisibleListModel.get(i));
                ClientPropertyDraw property = initialTable.getProperties().get(index);
                preferences.put(property.getSID(), resetPropertyUserPreferences(property));
            }
            if (initialTable.getProperties().size() != 0) {
                List<GroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<GroupObjectUserPreferences>();
                groupObjectUserPreferencesList.add(new GroupObjectUserPreferences(preferences, initialTable.getProperties().get(0).groupObject.getSID(), getFontInfo(true), false));
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
            initialTable.setFont(initialTable.getFont().deriveFont(Font.PLAIN, 11));
            fontSizeField.setText("11");
            isFontBoldCheckBox.setSelected(false);
            isFontItalicCheckBox.setSelected(false);

            JOptionPane.showMessageDialog(this, getString("form.grid.hide.reset.settings.successfully.complete"), getString("form.grid.hide.reset.complete"), JOptionPane.INFORMATION_MESSAGE);
            if (forAllUsers) {
                setIcon(unsavedIcon);
            }
        }

        private FontInfo getFontInfo(boolean reset) {
            Font font = initialTable.getFont();
            return new FontInfo(font.getFamily(), reset ? font.getSize() : getFontSize(font.getSize()),
                    reset ? font.isBold() : isFontBoldCheckBox.isSelected(),
                    reset ? font.isItalic() : isFontItalicCheckBox.isSelected());
        }

        private int getFontStyle() {
            return (isFontBoldCheckBox.isSelected() ? Font.BOLD : Font.PLAIN) | (isFontItalicCheckBox.isSelected() ? Font.ITALIC : Font.PLAIN);
        }

        private int getFontSize(int oldSize) {
            int fontSize;
            try {
                fontSize = Integer.parseInt(fontSizeField.getText());
            } catch (Exception e) {
                return oldSize;
            }
            return fontSize;
        }

        private ColumnUserPreferences resetPropertyUserPreferences(ClientPropertyDraw property) {
            property.hideUser = null;
            property.widthUser = null;
            property.sortUser = null;
            property.ascendingSortUser = null;
            property.orderUser = property.getID();
            return new ColumnUserPreferences(null, null, null, null, null);
        }

        class ValueComparator implements Comparator {
            Map base;

            public ValueComparator(Map base) {
                this.base = base;
            }

            public int compare(Object a, Object b) {
                if (base.get(a) == null)
                    return base.get(b) == null ? 0 : -1;
                else
                    return base.get(b) == null ? 1 : ((Integer) base.get(a) - (Integer) base.get(b));
            }
        }
    }
}

