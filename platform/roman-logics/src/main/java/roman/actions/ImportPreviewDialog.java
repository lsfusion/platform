package roman.actions;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class ImportPreviewDialog extends JDialog {
    final JTableCheck table;
    private HashSet<String> result;
    private int booleanIndex = 5;

    public ImportPreviewDialog(ArrayList<InvoiceProperties> invoiceList, String title) {
        super(null, title, ModalityType.APPLICATION_MODAL);
        setMinimumSize(new Dimension(600, 250));

        setLocationRelativeTo(null);

        String[] columns = new String[]{"Номер инвойса", "Дата инвойса", "Сумма документа",
                "Общее количество в документе", "Общий вес", "Импортировать"};
        int rowCount = invoiceList.size();
        Object[][] data = new Object[rowCount][6];

        int i = 0;
        Collections.sort(invoiceList, COMPARATOR);
        for (InvoiceProperties entry : invoiceList) {
            data[i][0] = entry.sid == null ? "" : entry.sid;
            data[i][1] = entry.date == null ? new Date() : entry.date;
            data[i][2] = entry.sumDocument == null ? 0.0 : entry.sumDocument;
            data[i][3] = entry.quantityDocument == null ? 0.0 : entry.quantityDocument;
            data[i][4] = entry.netWeightDocument == null ? 0.0 : entry.netWeightDocument;
            data[i][booleanIndex] = Boolean.FALSE;
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

    private static Comparator<InvoiceProperties> COMPARATOR = new Comparator<InvoiceProperties>() {
        public int compare(InvoiceProperties i1, InvoiceProperties i2) {
            if (i1.date == null || i2.date == null)
                return 0;
            else {
                if (i1.date.getTime() > i2.date.getTime()) {
                    return 1;
                } else if (i1.date.getTime() < i2.date.getTime()) {
                    return -1;
                } else return 0;
            }
        }
    };

    private void checkAll() {
        for (int i = 0; i < table.getCheckTableModel().getRowCount(); i++) {
            table.getCheckTableModel().setValueAt(true, i, booleanIndex);
        }
        update(getGraphics());
    }

    private void onOk() {
        result = new HashSet<String>();
        for (Object[] row : table.getCheckTableModel().data) {
            if ((Boolean) row[booleanIndex])
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
        setPreferredSize(new Dimension(550, 150));

        checkTableModel = new CheckTableModel(columns, data);
        JTable table = new JTable(checkTableModel);
        table.setFillsViewportHeight(true);
        JScrollPane pane = new JScrollPane(table);
        add(pane, BorderLayout.CENTER);

        TableRowSorter<TableModel> sorter = new TableRowSorter(checkTableModel);
        table.setRowSorter(sorter);
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
        if (columnIndex == 5)
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