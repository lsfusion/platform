package lsfusion.client.form.object.table.grid.controller;

import com.google.common.base.Throwables;
import lsfusion.client.classes.data.ClientIntegralClass;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.filter.user.FilterView;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.grid.ClientGrid;
import lsfusion.client.form.object.table.grid.user.design.GridUserPreferences;
import lsfusion.client.form.object.table.grid.user.design.view.UserPreferencesButton;
import lsfusion.client.form.object.table.grid.user.toolbar.view.CalculateSumButton;
import lsfusion.client.form.object.table.grid.user.toolbar.view.CountQuantityButton;
import lsfusion.client.form.object.table.grid.user.toolbar.view.GroupingButton;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.object.table.grid.view.GridView;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.table.view.InternalEditEvent;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.object.table.grid.user.toolbar.FormGrouping;
import lsfusion.interop.form.order.user.Order;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static lsfusion.client.ClientResourceBundle.getString;

public class GridTableController {

    private static final ImageIcon PRINT_XLS_ICON = new ImageIcon(FilterView.class.getResource("/images/excelbw.png"));

    private static final ImageIcon PRINT_GROUP_ICON = new ImageIcon(FilterView.class.getResource("/images/reportbw.png"));

    private static final ImageIcon GROUP_CHANGE_ICON = new ImageIcon(FilterView.class.getResource("/images/groupchange.png"));

    private final ClientGrid clientGrid;

    private final GridView view;

    public final GridTable table;

    private final ClientFormController form;

    private final GridController groupController;

    private boolean forceHidden = false;

    public GridTableController(GridController igroupController, ClientFormController iform, GridUserPreferences[] iuserPreferences) {
        groupController = igroupController;
        clientGrid = groupController.getGroupObject().grid;
        form = iform;

        view = new GridView(this, form, iuserPreferences, clientGrid.tabVertical, clientGrid.groupObject.needVerticalScroll);
        table = view.getTable();
    }

    public boolean containsProperty(ClientPropertyDraw property) {
        return table.containsProperty(property);
    }

    public ToolbarGridButton createGridSettingsButton() {
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                for (int i = 0; i < table.getTableModel().getColumnCount(); ++i) {
                    table.setUserWidth(table.getTableModel().getColumnProperty(i), table.getColumnModel().getColumn(i).getWidth());
                }
            }
        });

        return new UserPreferencesButton(table, groupController);
    }

    public ToolbarGridButton createPrintGroupXlsButton() {
        return new ToolbarGridButton(PRINT_XLS_ICON, getString("form.grid.export.to.xlsx")) {
            @Override
            public void addListener() {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                form.runSingleGroupXlsExport(groupController);
                            }
                        });
                    }
                });
            }
        };
    }

    public ToolbarGridButton createPrintGroupButton() {
        return new ToolbarGridButton(PRINT_GROUP_ICON, getString("form.grid.print.grid")) {
            @Override
            public void addListener() {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                form.runSingleGroupReport(groupController);
                            }
                        });
                    }
                });
            }
        };
    }

    public GroupingButton createGroupingButton() {
        return new GroupingButton(table) {
            @Override
            public List<FormGrouping> readGroupings() {
                return form.readGroupings(groupController.getGroupObject().getSID());
            }

            @Override
            public Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer, 
                    List<byte[]>> sumMap, Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) {
               return form.groupData(groupMap, sumMap, maxMap, onlyNotNull); 
            }

            @Override
            public void savePressed(FormGrouping grouping) {
                form.saveGrouping(grouping);
            }
        };
    }

    public CalculateSumButton createCalculateSumButton() {
        return new CalculateSumButton() {
            public void addListener() {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ClientPropertyDraw property = getCurrentProperty();
                                    String caption = property.getCaption();
                                    if (property.baseType instanceof ClientIntegralClass) {
                                        ClientGroupObjectValue columnKey = table.getTableModel().getColumnKey(Math.max(table.getSelectedColumn(), 0));
                                        Object sum = form.calculateSum(property.getID(), columnKey.serialize());
                                        showPopupMenu(caption, sum);
                                    } else {
                                        showPopupMenu(caption, null);
                                    }
                                } catch (Exception ex) {
                                    throw Throwables.propagate(ex);
                                }
                            }
                        });
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
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    showPopupMenu(form.countRecords(groupController.getGroupObject().getID()));
                                } catch (Exception ex) {
                                    throw Throwables.propagate(ex);
                                }
                            }
                        });
                    }
                });
            }
        };
    }

    public ToolbarGridButton createGroupChangeButton() {
        ToolbarGridButton groupChangeButton = new ToolbarGridButton(GROUP_CHANGE_ICON, getString("form.grid.group.groupchange") + " (F12)") {
            @Override
            public void addListener() {
                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final int rowIndex = table.getSelectedRow();
                        final int columnIndex = table.getSelectedColumn();
                        if (rowIndex == -1 || columnIndex == -1)
                            JOptionPane.showMessageDialog(form.getLayout(), getString("form.grid.group.groupchange.no.column.selected"), 
                                    getString("errors.error"), JOptionPane.ERROR_MESSAGE);
                        else
                            RmiQueue.runAction(new Runnable() {
                                @Override
                                public void run() {
                                    table.editCellAt(rowIndex, columnIndex, new InternalEditEvent(table, ServerResponse.GROUP_CHANGE));
                                }
                            });
                    }
                });

                table.addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        setEnabled(true);
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        setEnabled(false);
                    }
                });
            }
        };
        groupChangeButton.setEnabled(false);
        return groupChangeButton;
    }

    public GridView getGridView() {
        return view;
    }

    public Font getFont() {
        return clientGrid.design.getFont(table);
    }
    
    public boolean getAutoSize() {
        return clientGrid.autoSize;
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(clientGrid, view);
    }

    public void addProperty(ClientPropertyDraw property) {
        table.addProperty(property);
    }

    public void removeProperty(ClientPropertyDraw property) {
        table.removeProperty(property);
    }

    public void setRowKeysAndCurrentObject(List<ClientGroupObjectValue> gridObjects, ClientGroupObjectValue newCurrentObject) {
        table.setRowKeysAndCurrentObject(gridObjects, newCurrentObject);
    }

    public void modifyGridObject(ClientGroupObjectValue gridObject, boolean add, int position) {
        table.modifyGroupObject(gridObject, add, position);
    }

    public void updateColumnKeys(ClientPropertyDraw drawProperty, List<ClientGroupObjectValue> groupColumnKeys) {
        table.updateColumnKeys(drawProperty, groupColumnKeys);
    }

    public void updatePropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions) {
        table.updateColumnCaptions(property, captions);
    }

    public void updateShowIfs(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> showIfs) {
        table.updateShowIfs(property, showIfs);
    }

    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> readOnlyValues) {
        table.updateReadOnlyValues(property, readOnlyValues);
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

    public ClientGroupObjectValue getCurrentObject() {
        return table.getCurrentObject();
    }

    public void updatePropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update) {
        table.setColumnValues(property, values, update);
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
    public ClientGroupObjectValue getCurrentColumn() {
        return table.getCurrentColumn();
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

    public void setForceHidden(boolean forceHidden) {
        this.forceHidden = forceHidden;
    }

    public GridController getGroupController() {
        return groupController;
    }

    public boolean isVisible() {
        return !forceHidden && groupController.isGrid();
    }

    public void update() {
        table.updateTable();
        view.setVisible(isVisible());
    }
}
