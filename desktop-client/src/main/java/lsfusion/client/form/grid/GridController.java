package lsfusion.client.form.grid;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.GroupObjectController;
import lsfusion.client.form.InternalEditEvent;
import lsfusion.client.form.RmiQueue;
import lsfusion.client.form.layout.ClientFormLayout;
import lsfusion.client.form.queries.*;
import lsfusion.client.logics.ClientGrid;
import lsfusion.client.logics.ClientGroupObject;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientIntegralClass;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.FontInfo;
import lsfusion.interop.FormGrouping;
import lsfusion.interop.Order;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.form.screen.ExternalScreenComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridController {

    private static final ImageIcon PRINT_XLS_ICON = new ImageIcon(FilterView.class.getResource("/images/excelbw.png"));

    private static final ImageIcon PRINT_GROUP_ICON = new ImageIcon(FilterView.class.getResource("/images/reportbw.png"));

    private static final ImageIcon GROUP_CHANGE_ICON = new ImageIcon(FilterView.class.getResource("/images/groupchange.png"));

    private final ClientGrid clientGrid;

    private final GridView view;

    public final GridTable table;

    private final ClientFormController form;

    private final GroupObjectController groupController;

    private boolean forceHidden = false;

    public GridController(GroupObjectController igroupController, ClientFormController iform, GridUserPreferences[] iuserPreferences) {
        groupController = igroupController;
        clientGrid = groupController.getGroupObject().grid;
        form = iform;

        view = new GridView(this, form, iuserPreferences, clientGrid.tabVertical, clientGrid.groupObject.needVerticalScroll);
        table = view.getTable();

        FontInfo userFont = table.getUserFont();
        if (groupController.getGroupObject() != null && userFont != null) {
            if (userFont.fontSize == 0)
                table.setFont(table.getFont().deriveFont(userFont.getStyle()));
            else
                table.setFont(table.getFont().deriveFont(userFont.getStyle(), userFont.fontSize));
        }
        Integer userPageSize = table.getUserPageSize();
        if(userPageSize != null)
            table.setPageSize(userPageSize);
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
        return new ToolbarGridButton(PRINT_XLS_ICON, ClientResourceBundle.getString("form.grid.export.to.xls")) {
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
        return new ToolbarGridButton(PRINT_GROUP_ICON, ClientResourceBundle.getString("form.grid.print.grid")) {
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
                                    throw new RuntimeException(ex);
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
                                    throw new RuntimeException(ex);
                                }
                            }
                        });
                    }
                });
            }
        };
    }

    public ToolbarGridButton createGroupChangeButton() {
        ToolbarGridButton groupChangeButton = new ToolbarGridButton(GROUP_CHANGE_ICON, ClientResourceBundle.getString("form.grid.group.groupchange") + " (F12)") {
            @Override
            public void addListener() {
                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final int rowIndex = table.getSelectedRow();
                        final int columnIndex = table.getSelectedColumn();
                        if (rowIndex == -1 || columnIndex == -1)
                            JOptionPane.showMessageDialog(form.getLayout(), "Не выбрано ни одной колонки", "Сообщение", 0);
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

    public void setRowKeysAndCurrentObject(List<ClientGroupObjectValue> gridObjects, ClientGroupObjectValue newCurrentObject) {
        table.setRowKeysAndCurrentObject(gridObjects, newCurrentObject);
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

    public void setForceHidden(boolean forceHidden) {
        this.forceHidden = forceHidden;
    }

    public GroupObjectController getGroupController() {
        return groupController;
    }

    public boolean isVisible() {
        return !forceHidden && groupController.classView == ClassViewType.GRID;
    }

    public void update() {
        table.updateTable();
        view.setVisible(isVisible());
    }
}
