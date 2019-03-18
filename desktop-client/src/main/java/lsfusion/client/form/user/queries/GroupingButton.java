package lsfusion.client.form.user.queries;

import lsfusion.client.Main;
import lsfusion.client.base.RmiQueue;
import lsfusion.client.form.object.grid.GridTable;
import lsfusion.interop.form.user.FormGrouping;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class GroupingButton extends ToolbarGridButton {

    private static final ImageIcon GROUP_ICON = new ImageIcon(FilterView.class.getResource("/images/group.png"));

    public GroupingDialog dialog;

    public GroupingButton(final GridTable grid) {
        super(GROUP_ICON, getString("form.queries.grouping"));

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        dialog = new GroupingDialog(Main.frame, grid, readGroupings(), grid.getForm().hasCanonicalName()) {

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
                                    throw new RuntimeException(e);
                                }
                            }

                            @Override
                            public void pivotXLSXPressed() {
                                updateData(true);
                                try {
                                    dialog.exportToExcelPivot(true);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
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

