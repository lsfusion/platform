package platform.client.form.grid;

import platform.client.ClientResourceBundle;
import platform.client.Main;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectController;
import platform.client.form.InternalEditEvent;
import platform.client.form.queries.*;
import platform.client.logics.ClientGrid;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientIntegralClass;
import platform.interop.Order;
import platform.interop.form.ServerResponse;
import platform.interop.form.screen.ExternalScreenComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridController {

    private static final ImageIcon printXlsIcon = new ImageIcon(FilterView.class.getResource("/images/excelbw.png"));

    private static final ImageIcon printGroupIcon = new ImageIcon(FilterView.class.getResource("/images/reportbw.png"));

    private static final ImageIcon groupChangeIcon = new ImageIcon(FilterView.class.getResource("/images/groupchange.png"));

    private final ClientGrid clientGrid;

    private final GridView view;

    public final GridTable table;

    private final ClientFormController form;

    private final GroupObjectController groupController;

    public GridController(GroupObjectController igroupController, ClientFormController iform) {
        groupController = igroupController;
        clientGrid = groupController.getGroupObject().grid;
        form = iform;

        view = new GridView(this, form, clientGrid.tabVertical, clientGrid.groupObject.needVerticalScroll);
        table = view.getTable();

        if (clientGrid.minRowCount > 0) { // вообще говоря, так делать неправильно, посколько и HeaderHeight и RowHeight могут изменяться во времени
            Dimension minSize = table.getMinimumSize();
            minSize.height = Math.max(minSize.height, (int) table.getTableHeader().getPreferredSize().getHeight() + clientGrid.minRowCount * table.getRowHeight());
            view.setMinimumSize(minSize);
        }
    }

    public UserPreferencesButton createHideSettingsButton() {
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                for (int i = 0; i < table.getTableModel().getColumnCount(); ++i) {
                    table.getTableModel().getColumnProperty(i).widthUser = table.getColumnModel().getColumn(i).getWidth();
                }
            }
        });

        return new UserPreferencesButton(groupController.getGroupObject().hasUserPreferences) {
                    public void addListener() {
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    dialog = new HideSettingsDialog(Main.frame, table, form);
                                    dialog.setVisible(true);
                                    form.processRemoteChanges(false);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        });
                    }
                };
    }

    public ToolbarGridButton createPrintGroupXlsButton() {
        return new ToolbarGridButton(printXlsIcon, ClientResourceBundle.getString("form.grid.export.to.xls")) {
                    @Override
                    public void addListener() {
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                form.runSingleGroupXlsExport(groupController);
                            }
                        });
                    }
                };
    }

    public ToolbarGridButton createPrintGroupButton() {
        return new ToolbarGridButton(printGroupIcon, ClientResourceBundle.getString("form.grid.print.grid")) {
                    @Override
                    public void addListener() {
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                form.runSingleGroupReport(groupController);
                            }
                        });
                    }
                };
    }

    public GroupButton createGroupButton() {
        return new GroupButton() {
                    public void addListener() {
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    dialog = new GroupDialog(Main.frame, table);
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
                };
    }

    public CalculateSumButton craeteCalculateSumButton() {
        return new CalculateSumButton() {
                    public void addListener() {
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    ClientPropertyDraw property = getCurrentProperty();
                                    String caption = property.getCaption();
                                    if (property.baseType instanceof ClientIntegralClass) {
                                        ClientGroupObjectValue columnKey = table.getTableModel().getColumnKey(table.getSelectedColumn());
                                        Object sum = form.calculateSum(property.getID(), columnKey.serialize());
                                        showPopupMenu(caption, sum);
                                    } else {
                                        showPopupMenu(caption, null);
                                    }
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        });
                    }
                };
    }

    public CountQuantityButton createCountQuantityButton() {
        return new CountQuantityButton() {
                    public void addListener() {
                        addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    showPopupMenu(form.countRecords(groupController.getGroupObject().getID()));
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        });
                    }
                };
    }

    public ToolbarGridButton createGroupChangeButton() {
        return new ToolbarGridButton(groupChangeIcon, ClientResourceBundle.getString("form.grid.group.groupchange") + " (Ctrl+F12)") {
                    @Override
                    public void addListener() {
                        addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                int rowIndex = table.getSelectedRow();
                                int columnIndex = table.getSelectedColumn();
                                table.editCellAt(rowIndex, columnIndex, new InternalEditEvent(table, ServerResponse.GROUP_CHANGE));
                            }
                        });
                    }
                };
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

    public void modifyGridObject(ClientGroupObjectValue gridObject, boolean add) {
        table.modifyGroupObject(gridObject, add);
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

    public void setCurrentObject(ClientGroupObjectValue currentObject) {
        table.setCurrentObject(currentObject);
    }

    public ClientGroupObjectValue getCurrentObject() {
        return table.getCurrentObject();
    }

    public void updatePropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update) {
        table.setColumnValues(property, values, update);
        if (extViews.containsKey(property)) {
            Object value = getSelectedValue(property, null);
            extViews.get(property).setValue((value == null) ? "" : value.toString());
            property.externalScreen.invalidate();
        }
    }

    public void changeGridOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        table.changeGridOrder(property, modiType);
    }

    public void clearGridOrders(ClientGroupObject groupObject) throws IOException {
        table.clearGridOrders(groupObject);
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
