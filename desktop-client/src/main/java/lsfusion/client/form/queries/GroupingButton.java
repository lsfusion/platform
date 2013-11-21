package lsfusion.client.form.queries;

import lsfusion.client.Main;
import lsfusion.client.form.grid.GridTable;
import lsfusion.interop.FormGrouping;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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
                try {
                    dialog = new GroupingDialog(Main.frame, grid, readGroupings()) {

                        @Override
                        protected void savePressed(FormGrouping grouping) {
                            GroupingButton.this.savePressed(grouping);
                        }

                        @Override
                        public void updatePressed() {
                            try {
                                Map<Integer, List<byte[]>> sumMap = dialog.getSelectedSumMap();
                                Map<Integer, List<byte[]>> maxMap = dialog.getSelectedMaxMap();
                                boolean onlyNotNull = dialog.onlyNotNull();

                                List<Map<List<Object>, List<Object>>> result = new ArrayList<Map<List<Object>, List<Object>>>();

                                for (Map<Integer, List<byte[]>> level : dialog.getSelectedGroupLevels()) {
                                    if (!level.isEmpty()) {
                                        result.add(groupData(level, sumMap, maxMap, onlyNotNull));
                                    }
                                }
                                dialog.update(result);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    };
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                dialog.updatePressed();
                dialog.setVisible(true);    
            }
        });
    }

    public abstract List<FormGrouping> readGroupings();
    public abstract Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap, Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws IOException;
    public abstract void savePressed(FormGrouping grouping);
}

