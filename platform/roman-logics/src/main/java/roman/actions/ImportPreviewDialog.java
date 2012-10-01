package roman.actions;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class ImportPreviewDialog extends JDialog {
    final JTableCheck table;
    private HashSet<String> result;

    public ImportPreviewDialog(Map<String, Date> invoiceList, String title) {
        super(null, title, ModalityType.APPLICATION_MODAL);
        setMinimumSize(new Dimension(500, 250));
        String[] columns = new String[]{"Номер инвойса", "Дата инвойса", "Импортировать"};
        int rowCount = invoiceList.size();
        Object[][] data = new Object[rowCount][3];

        int i = 0;
        for (Map.Entry<String, Date> entry : invoiceList.entrySet()) {
            data[i][0] = entry.getKey() == null ? "" : entry.getKey();
            data[i][1] = entry.getValue() == null ? new Date() : entry.getValue();
            data[i][2] = Boolean.TRUE;
            i++;
        }

        table = new JTableCheck(columns, data);
        JScrollPane tablePane = new JScrollPane(table);

        JButton checkAllButton = new JButton("Отметить все");
        checkAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkAll();

            }
        });

        JButton OKButton = new JButton("OK");
        OKButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();

            }
        });

        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(tablePane, BorderLayout.CENTER);
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(checkAllButton, BorderLayout.EAST);
        buttonsPanel.add(OKButton, BorderLayout.EAST);
        buttonsPanel.add(cancelButton, BorderLayout.EAST);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

    }

    private void checkAll() {
        for (int i=0; i<table.getCheckTableModel().getRowCount(); i++) {
            table.getCheckTableModel().setValueAt(true, i, 2);
        }
        update(getGraphics());
    }

    private void onOk() {
        result = new HashSet<String>();
        for (Object[] row : table.getCheckTableModel().data) {
            if ((Boolean) row[2])
                result.add((String) row[0]);
        }
        this.dispose();
    }

    private void onCancel() {
        result = new HashSet<String>();
        this.dispose();
    }

    public HashSet<String> execute() {
        this.setVisible(true);
        return this.result;
    }
}


class JTableCheck extends JPanel {
    private CheckTableModel checkTableModel;

    public JTableCheck(String[] columns, Object[][] data) {
        initializeUI(columns, data);
    }

    private void initializeUI(String[] columns, Object[][] data) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(450, 150));

        checkTableModel = new CheckTableModel(columns, data);
        JTable table = new JTable(checkTableModel);
        table.setFillsViewportHeight(true);
        JScrollPane pane = new JScrollPane(table);
        add(pane, BorderLayout.CENTER);
    }

    public CheckTableModel getCheckTableModel() {
        return checkTableModel;
    }
}

class CheckTableModel extends AbstractTableModel {
    String[] columns;
    Object[][] data;

    public CheckTableModel(String[] columns, Object[][] data) {
        this.columns = columns;
        this.data = data;
    }

    public int getRowCount() {
        return data.length;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return data[rowIndex][columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 2)
            return true;
        else return false;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        data[rowIndex][columnIndex] = aValue;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return data[0][columnIndex].getClass();
    }
}