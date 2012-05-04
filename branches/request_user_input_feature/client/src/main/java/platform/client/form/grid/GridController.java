package platform.client.form.grid;

import platform.client.ClientResourceBundle;
import platform.client.Main;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectController;
import platform.client.form.classes.ClassContainer;
import platform.client.form.queries.*;
import platform.client.logics.ClientGrid;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientObject;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientIntegralClass;
import platform.interop.Order;
import platform.interop.form.screen.ExternalScreenComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class GridController {

    private final ClientGrid clientGrid;

    private final FilterController filterController;

    private final GridView view;

    private final GridTable table;

    private final ClientFormController form;

    private final GroupObjectController groupController;

    public GridController(GroupObjectController igroupController, ClientFormController iform) {

        groupController = igroupController;
        clientGrid = groupController.getGroupObject().grid;
        form = iform;

        view = new GridView(this, form, clientGrid.tabVertical, clientGrid.groupObject.needVerticalScroll);
        table = view.getTable();

        boolean hasClassChoosers = false;
        for (final ClientObject object : clientGrid.groupObject.objects) {
            if (object.classChooser.show) {
                ToolbarGridButton classButton = new ToolbarGridButton("/images/side_expand.png", ClientResourceBundle.getString("form.tree.show", object.caption)) {
                    public void addListener() {
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                ClassContainer container = groupController.getObjectsMap().get(object).classController.getClassContainer();
                                if (!container.isExpanded) {
                                    container.expandTree();
                                    setIcon(new ImageIcon(getClass().getResource("/images/side_hide.gif")));
                                    setToolTipText(ClientResourceBundle.getString("form.tree.hide", object.caption));
                                } else {
                                    container.collapseTree();
                                    setIcon(new ImageIcon(getClass().getResource("/images/side_expand.png")));
                                    setToolTipText(ClientResourceBundle.getString("form.tree.show", object.caption));
                                }
                            }
                        });
                    }
                };
                groupController.addToToolbar(classButton);
                hasClassChoosers = true;
            }
        }
        if (hasClassChoosers)
            groupController.addToToolbar(Box.createHorizontalStrut(5));

        if (clientGrid.showFind) {
            FindController findController = new FindController(groupController) {
                protected void queryChanged() {
                    form.changeFind(getConditions());
                    table.requestFocusInWindow();
                }

                public void conditionsUpdated() {}
            };
            groupController.addToToolbar(findController.getView());
            findController.getView().addActions(table);
        }

        if (clientGrid.showFilter) {
            filterController = new FilterController(groupController) {
                protected void queryChanged() {
                    try {
                        form.changeFilter(groupController.getGroupObject(), getConditions());
                    } catch (IOException e) {
                        throw new RuntimeException(ClientResourceBundle.getString("errors.error.applying.filter"), e);
                    }

                    table.requestFocusInWindow();
                }

                public void conditionsUpdated() {
                    groupController.moveComponent(getView(), getDestination());
                }
            };
            groupController.addToToolbar(filterController.getView());
            filterController.getView().addActions(table);

            groupController.addToToolbar(Box.createHorizontalStrut(5));
        } else {
            filterController = null;
        }

        if (clientGrid.showGroupChange) {
            groupController.addToToolbar(new ToolbarGridButton("/images/groupchange.gif", ClientResourceBundle.getString("form.grid.group.groupchange") +" (Ctrl+F12)") {
                @Override
                public void addListener() {
                    addActionListener(table.getActionMap().get(GridTable.GROUP_CORRECTION_ACTION));
                }
            });
        }

        if (clientGrid.showCountQuantity) {
            groupController.addToToolbar(new CountQuantityButton() {
                public void addListener() {
                    addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            try {
                                showPopupMenu(form.countRecords(groupController.getGroupObject().getID()));
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    });
                }
            });
        }

        if (clientGrid.showCalculateSum) {
            groupController.addToToolbar(new CalculateSumButton() {
                public void addListener() {
                    addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            try {
                                ClientPropertyDraw property = getCurrentProperty();
                                String caption = property.getCaption() != null ? property.getCaption() : table.getColumnName(table.getSelectedColumn()).trim();
                                if (property.baseType instanceof ClientIntegralClass) {
                                    ClientGroupObjectValue columnKey = table.getTableModel().getColumnKey(table.getSelectedColumn());
                                    Object sum = form.calculateSum(property.getID(), columnKey.serialize());
                                    showPopupMenu(caption, sum);
                                } else {
                                    showPopupMenu(caption, null);
                                }
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    });
                }
            });
        }

        if (clientGrid.showGroup) {
            groupController.addToToolbar(new GroupButton() {
                public void addListener() {
                    addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            try {
                                dialog = new GroupButton.GroupDialog(Main.frame, table);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                            dialog.addPropertyChangeListener(new PropertyChangeListener() {
                                public void propertyChange(PropertyChangeEvent evt) {
                                    try {
                                        Map<Integer, List<byte[]>> sumMap = dialog.getSelectedSumMap();
                                        Map<Integer, List<byte[]>> maxMap = dialog.getSelectedMaxMap();
                                        List<Map<Integer, List<byte[]>>> groupLevels = dialog.getSelectedGroupLevels();
                                        boolean onlyNotNull = dialog.onlyNotNull();

                                        List<Map<List<Object>, List<Object>>> result = new ArrayList<Map<List<Object>, List<Object>>>();

                                        for (Map<Integer, List<byte[]>> level : groupLevels) {
                                            if (!level.isEmpty()) {
                                                result.add(form.groupData(level, sumMap, maxMap, onlyNotNull));
                                            }
                                        }
                                        dialog.update(result);
                                    } catch (IOException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                }
                            });
                            dialog.setVisible(true);
                        }
                    });
                }
            });
        }

        groupController.addToToolbar(Box.createHorizontalStrut(5));

        if (clientGrid.showPrintGroupButton && Main.module.isFull()) { // todo [dale]: Можно ли избавиться от if'ов?
            groupController.addToToolbar(new ToolbarGridButton("/images/reportbw.gif", ClientResourceBundle.getString("form.grid.print.grid")) {
                @Override
                public void addListener() {
                    addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            form.runSingleGroupReport(groupController);
                        }
                    });
                }
            });
        }

        if (clientGrid.showPrintGroupXlsButton && Main.module.isFull()) {
            groupController.addToToolbar(new ToolbarGridButton("/images/excelbw.jpg", ClientResourceBundle.getString("form.grid.export.to.xls")) {
                @Override
                public void addListener() {
                    addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            form.runSingleGroupXlsExport(groupController);
                        }
                    });
                }
            });
            groupController.addToToolbar(Box.createHorizontalStrut(5));
        }

        if (clientGrid.showHideSettings) {
            groupController.addToToolbar(new HideSettingsButton() {
                public void addListener() {
                    addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            try {
                                dialog = new HideSettingsButton.HideSettingsDialog(Main.frame, table, form);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                            dialog.setVisible(true);
                            form.refresh();
                        }
                    });
                }
            });
            groupController.addToToolbar(Box.createHorizontalStrut(5));

            table.getTableHeader().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    for (int i = 0; i < table.getTableModel().getColumnCount(); ++i) {
                        table.getTableModel().getColumnProperty(i).widthUser = table.getColumnModel().getColumn(i).getWidth();
                    }
                }
            });
        }

        if (clientGrid.minRowCount > 0) { // вообще говоря, так делать неправильно, посколько и HeaderHeight и RowHeight могут изменяться во времени
            Dimension minSize = table.getMinimumSize();
            minSize.height = Math.max(minSize.height, (int) table.getTableHeader().getPreferredSize().getHeight() + clientGrid.minRowCount * table.getRowHeight());
            view.setMinimumSize(minSize);
        }
    }

    void needToBeShown() {
        if (!hidden && !view.isVisible()) {
            view.setVisible(true);
        }
    }

    void needToBeHidden() {
        view.setVisible(false);
    }

    public GridView getGridView() {
        return view;
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(clientGrid, view);
        for (Map.Entry<ClientPropertyDraw, ExternalScreenComponent> entry : extViews.entrySet()) {
            entry.getKey().externalScreen.add(form.getID(), entry.getValue(), entry.getKey().externalScreenConstraints);
        }
    }

    private Map<ClientPropertyDraw, ExternalScreenComponent> extViews = new HashMap<ClientPropertyDraw, ExternalScreenComponent>();

    private void addExternalScreenComponent(ClientPropertyDraw key) {
        if (!extViews.containsKey(key)) {
            ExternalScreenComponent extView = new ExternalScreenComponent();
            extViews.put(key, extView);
        }
    }

    public void addProperty(ClientPropertyDraw property) {
        table.addProperty(property);

        if (property.externalScreen != null) {
            addExternalScreenComponent(property);
        }
    }

    public void removeProperty(ClientPropertyDraw property) {
        table.removeProperty(property);
    }

    public void setGridObjects(List<ClientGroupObjectValue> gridObjects) {
        table.setRowKeys(gridObjects);
    }

    public void updateColumnKeys(ClientPropertyDraw drawProperty, List<ClientGroupObjectValue> groupColumnKeys) {
        table.updateColumnKeys(drawProperty, groupColumnKeys);
    }

    public void updatePropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions) {
        table.updateColumnCaptions(property, captions);
    }

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        table.updateCellBackgroundValues(property, cellBackgroundValues);
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        table.updateCellForegroundValues(property, cellForegroundValues);
    }

    public void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> rowBackground) {
        table.updateRowBackgroundValues(rowBackground);
    }

    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> rowForeground) {
        table.updateRowForegroundValues(rowForeground);
    }

    public void selectObject(ClientGroupObjectValue currentObject) {
        table.selectObject(currentObject);
    }

    public void updatePropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        table.setColumnValues(property, values);
        if (extViews.containsKey(property)) {
            Object value = getSelectedValue(property, null);
            extViews.get(property).setValue((value == null) ? "" : value.toString());
            property.externalScreen.invalidate();
        }
    }

    public void changeGridOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        table.changeGridOrder(property, modiType);
    }

    public ClientPropertyDraw getCurrentProperty() {
        return table.getCurrentProperty();
    }

    public Object getSelectedValue(ClientPropertyDraw cell, ClientGroupObjectValue columnKey) {
        return table.getSelectedValue(cell, columnKey);
    }

    public boolean requestFocusInWindow() {
        return table.requestFocusInWindow();
    }

    public void selectProperty(ClientPropertyDraw propertyDraw) {
        table.selectProperty(propertyDraw);
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, ClientPropertyDraw propertyDraw) {
        if (filterController != null) {
            filterController.quickEditFilter(initFilterKeyEvent, propertyDraw);
            table.selectProperty(propertyDraw);
        }
    }

    public boolean hasActiveFilter() {
        return filterController != null && filterController.hasActiveFilter();
    }

    boolean hidden = false;

    public void hideViews() {
        hidden = true;
        view.setVisible(false);
    }

    public void showViews() {
        hidden = false;
        view.setVisible(true);
    }

    public void update() {
        table.updateTable();
    }

    public GroupObjectController getGroupController() {
        return groupController;
    }
}
