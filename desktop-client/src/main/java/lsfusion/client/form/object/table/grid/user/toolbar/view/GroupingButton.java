package lsfusion.client.form.object.table.grid.user.toolbar.view;

import com.google.common.base.Throwables;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.object.table.grid.user.toolbar.FormGrouping;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class GroupingButton extends ToolbarGridButton {

    private static final String PIVOT_ICON_PATH = "pivot.png";

    public GroupingDialog dialog;

    public GroupingButton(final GridTable grid) {
        super(PIVOT_ICON_PATH, getString("form.queries.grouping"));

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        dialog = new GroupingDialog(MainFrame.instance, grid, readGroupings(), grid.getForm().hasCanonicalName()) {

                            @Override
                            protected void savePressed(FormGrouping grouping) {
                                GroupingButton.this.savePressed(grouping);
                            }

                            @Override
                            public void updatePressed() {
                                updateData(false);
                            }

                            @Override
                            public void pivotPressed() {
                                updateData(true);
                                try {
                                    dialog.exportToExcelPivot(false);
                                } catch (Exception e) {
                                    throw Throwables.propagate(e);
                                }
                            }

                            @Override
                            public void pivotXLSXPressed() {
                                updateData(true);
                                try {
                                    dialog.exportToExcelPivot(true);
                                } catch (Exception e) {
                                    throw Throwables.propagate(e);
                                }
                            }

                            private void updateData(boolean isPivot) {
                                Map<Integer, List<byte[]>> sumMap = dialog.getSelectedSumMap();
                                Map<Integer, List<byte[]>> maxMap = dialog.getSelectedMaxMap();
                                boolean onlyNotNull = dialog.onlyNotNull();

                                List<Map<List<Object>, List<Object>>> result = new ArrayList<>();

                                boolean replace = false;
                                for (Map<Integer, List<byte[]>> level : dialog.getSelectedGroupLevels()) {
                                    if (!level.isEmpty()) {
                                        Map<List<Object>, List<Object>> groupData = groupData(level, sumMap, maxMap, onlyNotNull);
                                        if (groupData != null) {
                                            if (isPivot && !result.isEmpty()) {
                                                if (replace ||result.get(0).size() < groupData.size()) {
                                                    result.set(0, groupData);
                                                    replace = true;
                                                }
                                            } else
                                                result.add(groupData);
                                        }
                                    }
                                }
                                dialog.update(result);
                            }
                        };

                        dialog.updatePressed();
                        dialog.setVisible(true);
                    }
                });
            }
        });
    }    

    public abstract List<FormGrouping> readGroupings();
    public abstract Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap, Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull);
    public abstract void savePressed(FormGrouping grouping);
}

